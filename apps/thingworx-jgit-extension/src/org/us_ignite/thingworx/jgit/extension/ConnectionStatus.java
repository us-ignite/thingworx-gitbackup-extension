package org.us_ignite.thingworx.jgit.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.eclipse.jgit.transport.URIish;

/**
 * Works out which setup steps a remote still needs before the current user can pull from and push
 * to it. Nothing here connects to the server: the result reflects the saved settings only, so a
 * connection test is still the final check.
 *
 * <p>Each step is reported as {@link #OK}, {@link #MISSING} or {@link #NOT_NEEDED}. A remote has
 * one fetch URL and may have a separate push URL; a step is done only when it is done for every URL
 * that needs it.
 */
final class ConnectionStatus {
    static final String OK = "ok";
    static final String MISSING = "missing";
    static final String NOT_NEEDED = "n/a";

    /** What is known about one remote and the current user's sign-in for it. */
    record Inputs(
            String remote,
            List<URIish> uris,
            boolean isDefault,
            String username,
            boolean hasPassword,
            String approvedOrigins,
            String sshKeyFingerprint,
            boolean sshKeyAvailable,
            List<URIish> approvedHostKeys,
            List<String> networkProblems,
            boolean allowInsecureCredentials) {}

    /** One row of the setup checklist. */
    record Status(
            String remote,
            String url,
            String transport,
            boolean isDefault,
            String network,
            String signIn,
            String credentialOrigin,
            String hostKey,
            boolean ready,
            String nextStep) {}

    private ConnectionStatus() {}

    static String transport(URIish uri) {
        String scheme = uri.getScheme();
        return scheme == null ? "ssh" : scheme.toLowerCase(Locale.ROOT);
    }

    static Status evaluate(Inputs in) {
        List<URIish> uris = in.uris() == null ? List.of() : in.uris();
        if (uris.isEmpty()) {
            return new Status(
                    in.remote(),
                    "",
                    "",
                    in.isDefault(),
                    MISSING,
                    MISSING,
                    MISSING,
                    MISSING,
                    false,
                    "Remote '" + in.remote() + "' has no URL. Set one under Remotes.");
        }
        boolean anySsh = false;
        boolean anyHttps = false;
        boolean anyPlainHttp = false;
        List<String> transports = new ArrayList<>();
        for (URIish uri : uris) {
            String transport = transport(uri);
            if (!transports.contains(transport)) transports.add(transport);
            if ("ssh".equals(transport)) anySsh = true;
            else if ("https".equals(transport)) anyHttps = true;
            else if ("http".equals(transport)) anyPlainHttp = true;
        }
        boolean passwordUsable = anyHttps || (anyPlainHttp && in.allowInsecureCredentials());
        List<String> problems = in.networkProblems() == null ? List.of() : in.networkProblems();

        String network = problems.isEmpty() ? OK : MISSING;

        String signIn;
        boolean httpSignInDone = !blank(in.username()) && in.hasPassword();
        boolean sshSignInDone = !blank(in.sshKeyFingerprint()) && in.sshKeyAvailable();
        if (anySsh && passwordUsable) signIn = httpSignInDone && sshSignInDone ? OK : MISSING;
        else if (anySsh) signIn = sshSignInDone ? OK : MISSING;
        else if (passwordUsable) signIn = httpSignInDone ? OK : MISSING;
        else signIn = MISSING; // plain HTTP without the test-only override: no way to sign in

        String origin = NOT_NEEDED;
        if (passwordUsable) {
            origin = OK;
            for (URIish uri : uris) {
                if ("ssh".equals(transport(uri))) continue;
                if (!Remotes.credentialApproved(in.approvedOrigins(), uri)) origin = MISSING;
            }
        }

        String hostKey = NOT_NEEDED;
        if (anySsh) {
            hostKey = OK;
            List<URIish> approved =
                    in.approvedHostKeys() == null ? List.of() : in.approvedHostKeys();
            for (URIish uri : uris) {
                if ("ssh".equals(transport(uri)) && !approved.contains(uri)) hostKey = MISSING;
            }
        }

        String next;
        if (!problems.isEmpty()) {
            next =
                    problems.get(0)
                            + " Ask a ThingWorx administrator to add the host to THINGWORX_JGIT_NETWORK_ALLOWLIST if it is an internal Git server.";
        } else if (MISSING.equals(signIn)) {
            if (anyPlainHttp && !passwordUsable && !anySsh) {
                next =
                        "Plain http:// remotes can only be used without signing in, because saved passwords are never sent over HTTP. Use an https:// or ssh:// URL to push.";
            } else if (anySsh && blank(in.sshKeyFingerprint())) {
                next =
                        "Select this remote under Remotes and sign-in, choose its SSH key and save. Add the key's public key to your account on the Git server.";
            } else if (anySsh && !in.sshKeyAvailable()) {
                next =
                        "The SSH key selected for this remote no longer exists. Select the remote under Remotes and sign-in, choose another key and save.";
            } else {
                next =
                        "Select this remote under Remotes and sign-in and save a Git username and a password or access token.";
            }
        } else if (MISSING.equals(origin)) {
            next =
                    "This remote's URL changed since you saved its sign-in. Select the remote under Remotes and sign-in and save again to allow the password to be sent to the new server.";
        } else if (MISSING.equals(hostKey)) {
            next =
                    "Select this remote under Remotes and sign-in, check the SSH server's host key, compare its fingerprint with the Git server's operator, and approve it.";
        } else {
            next = "Ready. Use Test connection to confirm the server accepts your sign-in.";
        }

        boolean ready =
                OK.equals(network)
                        && OK.equals(signIn)
                        && !MISSING.equals(origin)
                        && !MISSING.equals(hostKey);
        return new Status(
                in.remote(),
                Remotes.describe(uris),
                String.join(", ", transports),
                in.isDefault(),
                network,
                signIn,
                origin,
                hostKey,
                ready,
                next);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
