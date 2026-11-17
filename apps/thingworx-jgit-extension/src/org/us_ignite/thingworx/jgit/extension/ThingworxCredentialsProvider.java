package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.types.collections.ValueCollection;
import java.util.Arrays;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

/**
 * JGit credentials backed by the current ThingWorx user's sign-in for one remote, or the
 * repository-wide sign-in when the remote has none of its own.
 *
 * <p>The password is stored as a ThingWorx PASSWORD value, which the platform keeps encrypted at
 * rest. This provider holds no secret itself: JGit only calls {@link #get} when a server asks for
 * authentication, and the password is decrypted at that moment, copied into JGit's credential item
 * and cleared from the provider. Anonymous remotes never touch the stored secret.
 *
 * <p>A credential row records the remote origins it was approved for. JGit receives the secret only
 * when the URI being contacted is one of those origins, so changing a remote URL cannot redirect
 * another user's stored token to an attacker. Credentials are never released over plain HTTP,
 * except in test deployments that explicitly set a test-only override.
 *
 * <p>Instances are created per service call and are never installed as JGit's JVM-wide default,
 * which every ThingWorx user would share.
 */
final class ThingworxCredentialsProvider extends CredentialsProvider {
    private final GitUserContextManager context;
    private final String repositoryThingName;
    private String remote;
    private boolean missing;
    private URIish server;
    private String refusedServer;
    private String unapprovedOrigin;
    private boolean insecureTransport;

    ThingworxCredentialsProvider(GitUserContextManager context, String repositoryThingName) {
        this.context = context;
        this.repositoryThingName = repositoryThingName;
    }

    /** Selects the remote whose saved sign-in is used. */
    void useRemote(String remote) {
        this.remote = remote;
    }

    /** True when a server asked for credentials and the user had none stored. */
    boolean credentialsMissing() {
        return missing;
    }

    /**
     * Limits this provider to the server of {@code remote}. A request for any other origin, which
     * happens when the remote redirects elsewhere, is declined.
     */
    void scopeTo(URIish remote) {
        this.server = remote;
    }

    /** The other server that asked for credentials after a redirect, or {@code null}. */
    String refusedServer() {
        return refusedServer;
    }

    /** The origin the user has not approved for their stored credentials, or {@code null}. */
    String unapprovedOrigin() {
        return unapprovedOrigin;
    }

    /** True when a remote asked for credentials over plain HTTP. */
    boolean insecureTransport() {
        return insecureTransport;
    }

    private static boolean sameServer(URIish a, URIish b) {
        return Remotes.sameServer(a, b);
    }

    @Override
    public boolean isInteractive() {
        return false;
    }

    @Override
    public boolean supports(CredentialItem... items) {
        for (CredentialItem item : items) {
            if (!(item instanceof CredentialItem.Username)
                    && !(item instanceof CredentialItem.Password)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
        if (uri == null || server == null || !sameServer(server, uri)) {
            refusedServer = uri == null ? "an unknown server" : String.valueOf(uri.getHost());
            return false;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
                && !NetworkPolicy.allowInsecureCredentials()) {
            insecureTransport = true;
            return false;
        }
        ValueCollection row;
        try {
            row = context.credentialsFor(repositoryThingName, remote);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read stored Git credentials.", e);
        }
        String approvedOrigins = valueOf(row, Const.ApprovedRemoteOrigins);
        if (!Remotes.credentialApproved(approvedOrigins, uri)) {
            unapprovedOrigin = Remotes.credentialScope(uri);
            return false;
        }
        String user = valueOf(row, Const.GitCommitterUser);
        String password = valueOf(row, Const.GitCommitterPassword);
        if (user.isBlank() || password.isEmpty()) {
            missing = true;
            return false;
        }
        char[] secret = password.toCharArray();
        try {
            for (CredentialItem item : items) {
                if (item instanceof CredentialItem.Username) {
                    ((CredentialItem.Username) item).setValue(user);
                } else if (item instanceof CredentialItem.Password) {
                    // setValue copies the array, so the local copy can be cleared below.
                    ((CredentialItem.Password) item).setValue(secret);
                } else {
                    throw new UnsupportedCredentialItem(uri, item.getPromptText());
                }
            }
            return true;
        } finally {
            Arrays.fill(secret, '\0');
        }
    }

    private static String valueOf(ValueCollection row, String field) {
        try {
            String value = row.getStringValue(field);
            return value == null ? "" : value;
        } catch (Exception e) {
            return "";
        }
    }
}
