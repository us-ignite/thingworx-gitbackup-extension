package org.us_ignite.thingworx.jgit.extension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;

/**
 * Probes and records SSH server host keys.
 *
 * <p>Connections use {@code StrictHostKeyChecking yes}; a host is usable only after an operator or
 * repository owner has compared the server's SHA-256 fingerprint out of band and approved it. The
 * probe accepts any key solely to read it; the approval step writes it to this repository's {@code
 * known_hosts} file.
 */
final class SshHostKeys {
    record HostKey(String host, int port, String fingerprint, String publicKey) {}

    static HostKey probe(URIish uri) throws Exception {
        if (uri == null || uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("An SSH remote URL is required.");
        }
        String host = uri.getHost();
        int port = uri.getPort() < 0 ? 22 : uri.getPort();
        String user = uri.getUser() == null || uri.getUser().isBlank() ? "git" : uri.getUser();
        AtomicReference<PublicKey> captured = new AtomicReference<>();
        SshClient client = SshClient.setUpDefaultClient();
        client.setServerKeyVerifier(
                (session, remoteAddress, serverKey) -> {
                    captured.set(serverKey);
                    return true;
                });
        client.start();
        ConnectFuture future = null;
        ClientSession session = null;
        Duration timeout = Duration.ofSeconds(NetworkPolicy.networkTimeoutSeconds());
        try {
            future = client.connect(user, host, port, null, null);
            try {
                session = future.verify(timeout).getSession();
                // The connect future completes once the session exists; the key exchange may
                // still be in flight. Starting authentication forces it to finish. The probe
                // intentionally has no credentials, so authentication itself normally fails.
                session.auth().verify(timeout);
            } catch (Exception e) {
                // Authentication normally fails because the probe supplies no key. The host key
                // is exchanged first, so it is still available when the connection got that far.
                if (captured.get() == null) throw e;
            }
            PublicKey key = captured.get();
            if (key == null) {
                throw new IllegalStateException("The SSH server did not present a host key.");
            }
            return new HostKey(
                    host, port, KeyUtils.getFingerPrint(key), PublicKeyEntry.toString(key));
        } finally {
            try {
                if (session != null) session.close(true);
            } catch (Exception ignored) {
            }
            client.stop();
        }
    }

    static void approve(Repository repository, URIish uri, String expectedFingerprint)
            throws Exception {
        HostKey key = probe(uri);
        if (expectedFingerprint == null
                || !key.fingerprint().equalsIgnoreCase(expectedFingerprint.trim())) {
            throw new IllegalArgumentException(
                    "The server presented "
                            + key.fingerprint()
                            + ", which does not match the approved fingerprint.");
        }
        record(repository, key);
    }

    static void record(Repository repository, HostKey key) throws Exception {
        Path knownHosts = GitTransport.knownHostsFile(repository);
        Files.createDirectories(knownHosts.getParent());
        if (!Files.exists(knownHosts)) Files.createFile(knownHosts);

        String pattern = hostPattern(key.host(), key.port());
        String line = pattern + " " + key.publicKey();
        boolean different = false;
        for (String existing : Files.readAllLines(knownHosts, StandardCharsets.UTF_8)) {
            String trimmed = existing.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            String[] fields = trimmed.split("\\s+");
            if (fields.length < 2 || !matchesHost(fields[0], pattern)) continue;
            if (trimmed.endsWith(key.publicKey())) return;
            different = true;
        }
        if (different) {
            throw new IllegalStateException(
                    "A different host key is already approved for this server. Reset the trusted host keys before approving a changed key.");
        }
        String prefix = "";
        try {
            if (Files.size(knownHosts) > 0) {
                byte[] bytes = Files.readAllBytes(knownHosts);
                if (bytes[bytes.length - 1] != '\n') prefix = "\n";
            }
        } catch (Exception ignored) {
        }
        Files.writeString(
                knownHosts,
                prefix + line + "\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);
    }

    /**
     * Returns whether this repository has approved a host key for the server in an SSH URL. Only
     * the recorded entry is checked; nothing is contacted, so a key the server has since changed
     * still counts as approved until a connection is refused.
     */
    static boolean isApproved(Repository repository, URIish uri) throws Exception {
        if (uri == null || uri.getHost() == null || uri.getHost().isBlank()) return false;
        Path knownHosts = GitTransport.knownHostsFile(repository);
        if (!Files.isRegularFile(knownHosts)) return false;
        String pattern = hostPattern(uri.getHost(), uri.getPort() < 0 ? 22 : uri.getPort());
        for (String existing : Files.readAllLines(knownHosts, StandardCharsets.UTF_8)) {
            String trimmed = existing.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            String[] fields = trimmed.split("\\s+");
            if (fields.length >= 2 && matchesHost(fields[0], pattern)) return true;
        }
        return false;
    }

    private static boolean matchesHost(String field, String pattern) {
        for (String candidate : field.split(",")) {
            if (candidate
                    .trim()
                    .toLowerCase(Locale.ROOT)
                    .equals(pattern.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String hostPattern(String host, int port) {
        if (host.indexOf(':') >= 0) return "[" + host + "]:" + port;
        return port == 22 ? host : host + ":" + port;
    }

    private SshHostKeys() {}
}
