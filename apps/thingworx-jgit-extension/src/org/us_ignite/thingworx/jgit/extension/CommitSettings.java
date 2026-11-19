package org.us_ignite.thingworx.jgit.extension;

import java.io.IOException;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.UnsupportedSigningFormatException;
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.lib.GpgSignature;
import org.eclipse.jgit.lib.ObjectBuilder;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Signer;
import org.eclipse.jgit.lib.Signers;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.CredentialsProvider;

/**
 * Gives the commits JGit creates inside a command, such as the merge commit of a Pull or Merge and
 * the commits a Rebase rewrites, the same committer identity and signing key that Commit uses.
 *
 * <p>Those commands build their own {@code CommitCommand}, which takes the committer from {@code
 * user.name}/{@code user.email}, decides whether to sign from {@code commit.gpgSign} and {@code
 * gpg.format}, and looks the signer up in JGit's JVM-wide {@link Signers} registry. While a scope
 * is open, those settings are set in the repository's in-memory configuration (never saved), and
 * the registry entry for the format hands signing to the signer registered for the current thread,
 * so concurrent operations for different users never sign with each other's keys. Closing the scope
 * restores the configuration.
 */
final class CommitSettings implements AutoCloseable {
    private static final ThreadLocal<Signer> CURRENT = new ThreadLocal<>();
    private static final String[][] KEYS = {
        {"user", "name"},
        {"user", "email"},
        {"user", "signingKey"},
        {"commit", "gpgSign"},
        {"gpg", "format"}
    };
    private static boolean installed;

    private final StoredConfig config;
    private final String[] previous = new String[KEYS.length];
    private final Signer previousSigner;

    private CommitSettings(Repository repository, String name, String email, Signer signer) {
        this.config = repository.getConfig();
        for (int i = 0; i < KEYS.length; i++) {
            previous[i] = config.getString(KEYS[i][0], null, KEYS[i][1]);
        }
        if (name != null && !name.isBlank()) config.setString("user", null, "name", name);
        if (email != null && !email.isBlank()) config.setString("user", null, "email", email);
        // The signer holds exactly one key, so it needs no key selector.
        config.unset("user", null, "signingKey");
        config.setBoolean("commit", null, "gpgSign", signer != null);
        if (signer != null) {
            config.setString("gpg", null, "format", format(signer).toConfigValue());
        }
        previousSigner = CURRENT.get();
        CURRENT.set(signer);
    }

    /**
     * Opens a scope in which commits JGit creates in {@code repository} on this thread use the
     * given identity and signer. A blank name or email leaves that part of the configuration
     * unchanged; a {@code null} signer makes those commits unsigned.
     */
    static CommitSettings apply(Repository repository, String name, String email, Signer signer) {
        install();
        return new CommitSettings(repository, name, email, signer);
    }

    /** The signature format a signer from {@code GitRepositoryShape#resolveSigner} produces. */
    static GpgConfig.GpgFormat format(Signer signer) {
        return signer instanceof StoredKeySshSigner
                ? GpgConfig.GpgFormat.SSH
                : GpgConfig.GpgFormat.OPENPGP;
    }

    @Override
    public void close() {
        for (int i = 0; i < KEYS.length; i++) {
            if (previous[i] == null) config.unset(KEYS[i][0], null, KEYS[i][1]);
            else config.setString(KEYS[i][0], null, KEYS[i][1], previous[i]);
        }
        if (previousSigner == null) CURRENT.remove();
        else CURRENT.set(previousSigner);
    }

    private static synchronized void install() {
        if (installed) return;
        for (GpgConfig.GpgFormat format :
                new GpgConfig.GpgFormat[] {GpgConfig.GpgFormat.OPENPGP, GpgConfig.GpgFormat.SSH}) {
            Signer existing = Signers.get(format);
            if (existing instanceof ThreadSigner) continue;
            Signers.set(format, new ThreadSigner(existing));
        }
        installed = true;
    }

    /**
     * Signs with the signer of the current thread's scope, or with JGit's default signer for the
     * format when no scope is open.
     */
    private static final class ThreadSigner implements Signer {
        private final Signer fallback;

        ThreadSigner(Signer fallback) {
            this.fallback = fallback;
        }

        private Signer target() throws CanceledException {
            Signer signer = CURRENT.get();
            if (signer == null) signer = fallback;
            if (signer == null) throw new CanceledException("No signing key is available.");
            return signer;
        }

        @Override
        public void signObject(
                Repository repository,
                GpgConfig config,
                ObjectBuilder object,
                PersonIdent committer,
                String signingKey,
                CredentialsProvider credentialsProvider)
                throws CanceledException, IOException, UnsupportedSigningFormatException {
            target().signObject(
                            repository, config, object, committer, signingKey, credentialsProvider);
        }

        @Override
        public GpgSignature sign(
                Repository repository,
                GpgConfig config,
                byte[] data,
                PersonIdent committer,
                String signingKey,
                CredentialsProvider credentialsProvider)
                throws CanceledException, IOException, UnsupportedSigningFormatException {
            return target().sign(
                            repository, config, data, committer, signingKey, credentialsProvider);
        }

        @Override
        public boolean canLocateSigningKey(
                Repository repository,
                GpgConfig config,
                PersonIdent committer,
                String signingKey,
                CredentialsProvider credentialsProvider)
                throws CanceledException {
            return target().canLocateSigningKey(
                            repository, config, committer, signingKey, credentialsProvider);
        }
    }
}
