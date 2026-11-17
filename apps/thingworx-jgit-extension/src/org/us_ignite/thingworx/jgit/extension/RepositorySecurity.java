package org.us_ignite.thingworx.jgit.extension;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.eclipse.jgit.lib.StoredConfig;

/**
 * Applies security-relevant Git configuration defaults that must win over anything written into a
 * repository's {@code .git/config}.
 *
 * <p>Most of these settings are applied to the in-memory {@link StoredConfig}. Scalar values are
 * set locally, which shadows values from the system or global Git configuration; list values are
 * reset with JGit's empty-list convention. The repository is never re-written just to open it.
 */
final class RepositorySecurity {
    /** Rejects configuration that cannot be made safe by an in-memory override. */
    static void harden(StoredConfig config) {
        // Remote HTTP must verify TLS and must not follow redirects to an unapproved host. JGit
        // reads these from the config on every transport use.
        setHttpBoolean(config, null, "sslVerify", true);
        config.setString("http", null, "followRedirects", "false");
        resetHttpList(config, null, "extraHeader");
        config.setString("http", null, "cookieFile", "");
        config.setBoolean("http", null, "saveCookies", false);

        // URL-specific http.* sections override the generic section.
        for (String subsection : config.getSubsections("http")) {
            if (subsection == null || subsection.isBlank()) continue;
            setHttpBoolean(config, subsection, "sslVerify", true);
            config.setString("http", subsection, "followRedirects", "false");
            resetHttpList(config, subsection, "extraHeader");
            config.setString("http", subsection, "cookieFile", "");
            config.setBoolean("http", subsection, "saveCookies", false);
        }

        // A checked-out symlink can point outside the FileRepository and make a later import read
        // or overwrite files the repository owner cannot otherwise access.
        config.setBoolean("core", null, "symlinks", false);
        config.unset("core", null, "sshCommand");
        config.unset("core", null, "fsmonitor");

        // Signing is chosen explicitly by the commit code, never inherited from a shared config.
        config.setBoolean("commit", null, "gpgSign", false);
        config.setBoolean("tag", null, "gpgSign", false);

        // URL rewriting can change the host that receives credentials after validation.
        for (String subsection : config.getSubsections("url")) {
            config.unset("url", subsection, "insteadOf");
            config.unset("url", subsection, "pushInsteadOf");
        }

        // External clean/smudge filters are never needed by this extension and may execute
        // programs during checkout.
        for (String subsection : config.getSubsections("filter")) {
            config.unsetSection("filter", subsection);
        }
    }

    /**
     * Verifies the effective configuration cannot carry credentials somewhere else, rewrite a
     * remote URL, or receive commands from a remote helper.
     */
    static void validate(StoredConfig config) throws IOException {
        if (!config.getBoolean("http", null, "sslVerify", true)) {
            throw new IOException("Refusing repository with TLS verification disabled.");
        }
        for (String subsection : config.getSubsections("http")) {
            if (subsection == null || subsection.isBlank()) continue;
            if (!config.getBoolean("http", subsection, "sslVerify", true)) {
                throw new IOException("Refusing repository with TLS verification disabled.");
            }
        }
        for (String subsection : config.getSubsections("url")) {
            if (config.getStringList("url", subsection, "insteadOf").length > 0
                    || config.getStringList("url", subsection, "pushInsteadOf").length > 0) {
                throw new IOException(
                        "Refusing repository with url.*.insteadOf rewriting configured.");
            }
        }
        Remotes.validateConfig(config);
    }

    private static void setHttpBoolean(
            StoredConfig config, String subsection, String name, boolean value) {
        config.setBoolean("http", subsection, name, value);
    }

    private static void resetHttpList(StoredConfig config, String subsection, String name) {
        config.setStringList("http", subsection, name, Collections.singletonList(null));
    }

    static List<String> emptyList() {
        return Collections.emptyList();
    }

    private RepositorySecurity() {}
}
