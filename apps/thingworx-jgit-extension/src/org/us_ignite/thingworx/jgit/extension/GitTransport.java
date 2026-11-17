package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.types.collections.ValueCollection;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.HttpTransport;
import org.eclipse.jgit.transport.SshConfigStore;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.sshd.JGitKeyCache;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;

/**
 * The single place where network Git commands are given credentials.
 *
 * <p>Every fetch, pull, push and ls-remote goes through {@link #configure}, which attaches:
 *
 * <ul>
 *   <li>a {@link ThingworxCredentialsProvider} for HTTPS remotes, and
 *   <li>for SSH remotes, a session factory that authenticates with the SSH key the user selected
 *       for that remote and only connects to host keys that were explicitly approved.
 * </ul>
 *
 * <p>Nothing here is installed JVM-wide and the Tomcat user's {@code ~/.ssh} is never read. Create
 * one instance per service call and close it when the call ends.
 */
final class GitTransport implements AutoCloseable {
    /** Folder inside the repository's {@code .git} directory that holds SSH state. */
    static final String SSH_DIRECTORY = "ssh";

    static final String KNOWN_HOSTS = "known_hosts";

    private final GitUserContextManager context;
    private final String repositoryThingName;
    private final ThingworxCredentialsProvider credentials;
    private SshdSessionFactory sshFactory;
    private String remote;
    private boolean sshKeyMissing;
    private boolean sshUsed;

    private GitTransport(GitUserContextManager context, String repositoryThingName) {
        this.context = context;
        this.repositoryThingName = repositoryThingName;
        this.credentials = new ThingworxCredentialsProvider(context, repositoryThingName);
    }

    /** Resolves the current ThingWorx user; call this on the service thread. */
    static GitTransport forRepository(String repositoryThingName) throws Exception {
        return new GitTransport(new GitUserContextManager(), repositoryThingName);
    }

    /**
     * Gives {@code command} the current user's sign-in for {@code remote}, the remote name the
     * command contacts. Each remote has its own username, password and SSH key.
     */
    <C extends TransportCommand<C, ?>> C configure(C command, String remote) {
        if (remote == null || remote.isBlank()) {
            throw new IllegalArgumentException("A remote name is required.");
        }
        if (!remote.equals(this.remote)) sshFactory = null;
        this.remote = remote;
        credentials.useRemote(remote);
        Repository repository = command.getRepository();
        command.setTimeout(NetworkPolicy.networkTimeoutSeconds());
        return command.setCredentialsProvider(credentials)
                .setTransportConfigCallback(
                        transport -> {
                            // Checked on the URL actually being contacted, after any
                            // url.*.insteadOf rewriting in .git/config. Local paths, git://,
                            // ftp:// and bundle files are refused: a local remote can name a
                            // command to run, and the others have no server authentication.
                            URIish uri = transport.getURI();
                            Remotes.requireNetworkUri(uri);
                            NetworkPolicy.requireAllowed(uri);
                            if (transport instanceof HttpTransport) {
                                credentials.scopeTo(uri);
                            } else if (!(transport instanceof SshTransport)) {
                                throw new IllegalArgumentException(
                                        "Only https://, http:// and ssh:// remote URLs are supported.");
                            }
                            if (transport instanceof SshTransport) {
                                ((SshTransport) transport)
                                        .setSshSessionFactory(sshFactory(repository));
                                // sshd calls the provider from its own threads. SSH here uses
                                // keys only and host keys are checked strictly, so decline every
                                // prompt instead of trusting a key from a background thread.
                                transport.setCredentialsProvider(DECLINE_ALL);
                            }
                        });
    }

    /** Answers no to every SSH prompt, so a changed host key is always refused. */
    private static final CredentialsProvider DECLINE_ALL =
            new CredentialsProvider() {
                @Override
                public boolean isInteractive() {
                    return false;
                }

                @Override
                public boolean supports(CredentialItem... items) {
                    return true;
                }

                @Override
                public boolean get(URIish uri, CredentialItem... items) {
                    return false;
                }
            };

    /** Deletes the trusted SSH host keys, so the next connection requires approval again. */
    static boolean resetKnownHosts(Repository repository) throws Exception {
        return Files.deleteIfExists(knownHostsFile(repository));
    }

    static Path knownHostsFile(Repository repository) {
        return sshDirectory(repository).toPath().resolve(KNOWN_HOSTS);
    }

    static File sshStateDirectory(Repository repository) {
        return sshDirectory(repository);
    }

    private static File sshDirectory(Repository repository) {
        return new File(repository.getDirectory(), SSH_DIRECTORY);
    }

    private SshdSessionFactory sshFactory(Repository repository) {
        if (sshFactory != null) return sshFactory;
        sshUsed = true;
        File sshDir = sshDirectory(repository);
        if (!sshDir.isDirectory() && !sshDir.mkdirs()) {
            throw new IllegalStateException("Could not create " + sshDir);
        }
        Path knownHosts = knownHostsFile(repository);
        // JGit asks before creating a missing known_hosts file, and this provider cannot answer,
        // so a missing file would leave every host trusted for one connection only. Create it.
        try {
            if (!Files.exists(knownHosts)) Files.createFile(knownHosts);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not create " + knownHosts, e);
        }
        // Read the key now, on the service thread: sshd may ask for keys from its own I/O
        // threads, where the ThingWorx user context is not available.
        Iterable<KeyPair> keys = selectedKey();
        sshFactory =
                new SshdSessionFactoryBuilder()
                        .setHomeDirectory(sshDir)
                        .setSshDirectory(sshDir)
                        .setPreferredAuthentications("publickey")
                        .setDefaultIdentities(dir -> Collections.emptyList())
                        .setDefaultKeysProvider(dir -> keys)
                        .setDefaultKnownHostsFiles(dir -> List.of(knownHosts))
                        .setConfigStoreFactory(
                                (home, configFile, localUser) ->
                                        new StrictHostKeyConfig(knownHosts))
                        .build(new JGitKeyCache());
        return sshFactory;
    }

    private Iterable<KeyPair> selectedKey() {
        try {
            ValueCollection settings = context.credentialsFor(repositoryThingName, remote);
            String fingerprint = settings.getStringValue(Const.SshKeyFingerprint);
            ValueCollection key = context.sshKey(fingerprint);
            if (key == null) {
                sshKeyMissing = true;
                return Collections.emptyList();
            }
            String passphrase = key.getStringValue(Const.SshKeyPassphrase);
            return List.of(
                    SshKeys.parse(
                                    key.getStringValue(Const.SshPrivateKey),
                                    passphrase == null ? "" : passphrase,
                                    null)
                            .keyPair());
        } catch (Exception e) {
            throw new IllegalStateException("Could not load the selected SSH key.", e);
        }
    }

    /**
     * Returns a prefixed, user-facing message when {@code error} is an authentication or host-key
     * failure, or {@code null} when it is something else.
     */
    String authenticationFailure(Throwable error) {
        if (credentials.refusedServer() != null) {
            return Const.ERR_PREFIX_AUTH
                    + String.format(
                            Const.ERR_CREDENTIALS_OTHER_SERVER, credentials.refusedServer());
        }
        if (credentials.insecureTransport()) {
            return Const.ERR_PREFIX_AUTH + Const.ERR_CREDENTIALS_INSECURE;
        }
        if (credentials.unapprovedOrigin() != null) {
            return Const.ERR_PREFIX_AUTH + Const.ERR_CREDENTIALS_NOT_APPROVED;
        }
        if (credentials.credentialsMissing()) {
            return Const.ERR_PREFIX_AUTH + Const.ERR_NO_CREDENTIALS;
        }
        if (sshKeyMissing) {
            return Const.ERR_PREFIX_AUTH + Const.ERR_NO_SSH_KEY;
        }
        boolean transportError = false;
        boolean hostKey = false;
        boolean auth = false;
        for (Throwable t = error; t != null; t = t.getCause()) {
            transportError |=
                    t instanceof TransportException
                            || t instanceof org.eclipse.jgit.errors.TransportException;
            String message = t.getMessage();
            if (message == null) continue;
            String lower = message.toLowerCase(Locale.ROOT);
            hostKey |= lower.contains("host key") || lower.contains("server key");
            auth |=
                    lower.contains("not authorized")
                            || lower.contains("authentication")
                            || lower.contains("auth fail")
                            || lower.contains("cannot log in");
        }
        String detail = error == null ? "" : " " + error.getMessage();
        if (sshUsed && hostKey) {
            return Const.ERR_PREFIX_AUTH + Const.ERR_SSH_HOST_KEY_CHANGED + detail;
        }
        if (transportError && auth) {
            return Const.ERR_PREFIX_AUTH
                    + (sshUsed ? Const.ERR_SSH_AUTH_FAILED : Const.ERR_AUTH_FAILED_GENERIC)
                    + detail;
        }
        return null;
    }

    @Override
    public void close() {
        if (sshFactory != null) {
            sshFactory.close();
            sshFactory = null;
        }
    }

    /**
     * SSH client settings for every host: only connect when the server's host key is already in
     * this repository's {@code known_hosts} file ({@code StrictHostKeyChecking yes}).
     */
    private static final class StrictHostKeyConfig implements SshConfigStore {
        private final HostConfig config;

        StrictHostKeyConfig(Path knownHosts) {
            Map<String, String> single = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            single.put("StrictHostKeyChecking", "yes");
            single.put("HashKnownHosts", "no");
            Map<String, List<String>> multi = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            multi.put("UserKnownHostsFile", List.of(knownHosts.toString()));
            multi.put("GlobalKnownHostsFile", List.of(knownHosts.toString()));
            this.config =
                    new HostConfig() {
                        @Override
                        public String getValue(String key) {
                            String value = single.get(key);
                            if (value != null) return value;
                            List<String> values = multi.get(key);
                            return values == null || values.isEmpty() ? null : values.get(0);
                        }

                        @Override
                        public List<String> getValues(String key) {
                            List<String> values = multi.get(key);
                            if (values != null) return values;
                            String value = single.get(key);
                            return value == null ? Collections.emptyList() : List.of(value);
                        }

                        @Override
                        public Map<String, String> getOptions() {
                            return Collections.unmodifiableMap(single);
                        }

                        @Override
                        public Map<String, List<String>> getMultiValuedOptions() {
                            return Collections.unmodifiableMap(multi);
                        }
                    };
        }

        @Override
        public HostConfig lookup(String hostName, int port, String userName) {
            return config;
        }

        @Override
        public HostConfig lookupDefault(String hostName, int port, String userName) {
            return config;
        }
    }
}
