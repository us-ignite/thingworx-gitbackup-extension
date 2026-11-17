package org.us_ignite.thingworx.jgit.extension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remotes live only in the repository's {@code .git/config}. This class resolves which remote a
 * command should use and validates the URLs that services write there.
 */
final class Remotes {
    private static final Logger LOG = LoggerFactory.getLogger(Remotes.class);

    static final String DEFAULT_REMOTE = Constants.DEFAULT_REMOTE_NAME;

    /** URL schemes a remote may use. Local paths, git://, ftp and bundles are refused. */
    private static final Set<String> SCHEMES = Set.of("https", "http", "ssh");

    /**
     * Returns the remote a network command should use: {@code explicit} when given, otherwise the
     * upstream remote of {@code branch}, otherwise {@code origin}.
     *
     * @throws IllegalArgumentException when that remote is not configured
     */
    static String resolve(Repository repository, String explicit, String branch) {
        String remote = explicit == null ? "" : explicit.trim();
        if (remote.isEmpty() && branch != null && !branch.isBlank()) {
            String upstream = new BranchConfig(repository.getConfig(), branch).getRemote();
            // "." means the branch tracks another local branch, which is not a remote.
            if (upstream != null && !upstream.isBlank() && !".".equals(upstream)) {
                remote = upstream;
            }
        }
        if (remote.isEmpty()) remote = DEFAULT_REMOTE;
        if (!names(repository).contains(remote)) {
            throw new IllegalArgumentException(
                    Const.ERR_PREFIX_CONFIG
                            + "Remote '"
                            + remote
                            + "' is not configured for this repository. Add it in Repository Settings.");
        }
        return remote;
    }

    static Set<String> names(Repository repository) {
        return repository.getRemoteNames();
    }

    /**
     * Returns {@code <remote>/<branch>} for the remote-tracking branch a new local branch should
     * start from: the one on {@code origin} when it exists, otherwise the only remote that has that
     * branch. Returns {@code null} when no remote, or more than one, has it.
     */
    static String trackingStartPoint(Repository repository, String branch) throws Exception {
        if (repository.findRef(Constants.R_REMOTES + DEFAULT_REMOTE + "/" + branch) != null) {
            return DEFAULT_REMOTE + "/" + branch;
        }
        String found = null;
        for (String remote : names(repository)) {
            Ref ref = repository.exactRef(Constants.R_REMOTES + remote + "/" + branch);
            if (ref == null) continue;
            if (found != null) return null;
            found = remote + "/" + branch;
        }
        return found;
    }

    /**
     * Parses a remote URL and checks that it is an HTTPS, HTTP or SSH URL without an embedded
     * password.
     */
    static URIish parseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("A remote URL is required.");
        }
        URIish uri;
        try {
            uri = new URIish(url.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("The remote URL is not valid: " + e.getMessage());
        }
        requireNetworkUri(uri);
        if (uri.getPass() != null) {
            throw new IllegalArgumentException(
                    "Do not put a password or token in the remote URL. Save it in Repository Settings instead.");
        }
        return uri;
    }

    /** Refuses every URL that is not HTTPS, HTTP or SSH on a named host. */
    static void requireNetworkUri(URIish uri) {
        String scheme = uri.getScheme();
        boolean scpStyleSsh = scheme == null && uri.getHost() != null;
        boolean allowed =
                scpStyleSsh
                        || (scheme != null
                                && SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))
                                && uri.getHost() != null);
        if (!allowed) {
            throw new IllegalArgumentException(
                    "Only https://, http:// and ssh:// remote URLs are supported.");
        }
    }

    static void requireName(String name) {
        if (name == null
                || !name.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")
                || name.endsWith(".")) {
            throw new IllegalArgumentException(
                    "A remote name must start with a letter or digit and contain only letters, digits, '.', '_' and '-'.");
        }
    }

    /** Adds a remote as {@code git remote add} does, and prunes deleted branches on fetch. */
    static void add(Git git, String name, String url) throws Exception {
        requireName(name);
        URIish uri = parseUrl(url);
        if (names(git.getRepository()).contains(name)) {
            throw new IllegalArgumentException("Remote '" + name + "' already exists.");
        }
        git.remoteAdd().setName(name).setUri(uri).call();
        StoredConfig config = git.getRepository().getConfig();
        config.setBoolean("remote", name, "prune", true);
        config.save();
    }

    /**
     * Removes a remote's separate push URLs, so pushes use its fetch URL again.
     *
     * @return true when the remote had a push URL
     */
    static boolean clearPushUrls(Git git, String name) throws Exception {
        Repository repository = git.getRepository();
        if (!names(repository).contains(name)) {
            throw new IllegalArgumentException("Remote '" + name + "' does not exist.");
        }
        StoredConfig config = repository.getConfig();
        if (config.getStringList("remote", name, "pushurl").length == 0) return false;
        config.unset("remote", name, "pushurl");
        config.save();
        return true;
    }

    /**
     * Removes a remote as {@code git remote remove} does: its configuration, its remote-tracking
     * branches, and the upstream settings of local branches that tracked it.
     */
    static void remove(Git git, String name) throws Exception {
        Repository repository = git.getRepository();
        if (!names(repository).contains(name)) {
            throw new IllegalArgumentException("Remote '" + name + "' does not exist.");
        }
        git.remoteRemove().setRemoteName(name).call();
        StoredConfig config = repository.getConfig();
        for (String branch : config.getSubsections("branch")) {
            if (name.equals(config.getString("branch", branch, "remote"))) {
                config.unset("branch", branch, "remote");
                config.unset("branch", branch, "merge");
            }
        }
        config.save();
        List<Ref> tracking =
                repository.getRefDatabase().getRefsByPrefix(Constants.R_REMOTES + name + "/");
        for (Ref ref : tracking) {
            var update = repository.updateRef(ref.getName());
            update.setForceUpdate(true);
            update.delete();
        }
    }

    /**
     * Moves the URL of the removed {@code GitRepoURL} Thing property into {@code .git/config} as
     * {@code origin}, unless the repository already has an origin. Also drops the URL-less origin
     * that earlier versions wrote for repositories without a remote.
     *
     * @return true when the configuration was changed and must be saved
     */
    static boolean migrateLegacyUrl(StoredConfig config, String legacyUrl) {
        boolean hasOrigin = config.getSubsections("remote").contains(DEFAULT_REMOTE);
        String url = config.getString("remote", DEFAULT_REMOTE, "url");
        boolean changed = false;
        if (hasOrigin && (url == null || url.isBlank())) {
            config.unsetSection("remote", DEFAULT_REMOTE);
            hasOrigin = false;
            changed = true;
        }
        if (!hasOrigin && legacyUrl != null && !legacyUrl.isBlank()) {
            URIish sanitized = sanitizeLegacyUrl(legacyUrl);
            if (sanitized != null) {
                config.setString("remote", DEFAULT_REMOTE, "url", sanitized.toString());
                config.setString(
                        "remote",
                        DEFAULT_REMOTE,
                        "fetch",
                        "+refs/heads/*:refs/remotes/" + DEFAULT_REMOTE + "/*");
                config.setBoolean("remote", DEFAULT_REMOTE, "prune", true);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Returns a credential scope for a remote URL: scheme, host, effective port and repository
     * path, without the user name or any password. Two URLs with the same scope may receive the
     * same stored credentials.
     */
    static String credentialScope(URIish uri) {
        requireNetworkUri(uri);
        String scheme = uri.getScheme();
        if (scheme == null) scheme = "ssh";
        scheme = scheme.toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        if (port < 0) port = defaultPort(scheme);
        String path = uri.getPath();
        if (path == null) path = "";
        if (!path.startsWith("/")) path = "/" + path;
        return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + ":" + port + path;
    }

    private static int defaultPort(String scheme) {
        if ("https".equals(scheme)) return 443;
        if ("http".equals(scheme)) return 80;
        return 22;
    }

    static boolean sameServer(URIish a, URIish b) {
        if (a == null || b == null) return false;
        String aScheme = a.getScheme() == null ? "ssh" : a.getScheme();
        String bScheme = b.getScheme() == null ? "ssh" : b.getScheme();
        if (!aScheme.equalsIgnoreCase(bScheme)) return false;
        if (!same(a.getHost(), b.getHost())) return false;
        int aPort = a.getPort() < 0 ? defaultPort(aScheme.toLowerCase(Locale.ROOT)) : a.getPort();
        int bPort = b.getPort() < 0 ? defaultPort(bScheme.toLowerCase(Locale.ROOT)) : b.getPort();
        return aPort == bPort;
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equalsIgnoreCase(b);
    }

    /** True when {@code uri} is one of the newline-separated approved remote origins. */
    static boolean credentialApproved(String approvedOrigins, URIish uri) {
        if (approvedOrigins == null || approvedOrigins.isBlank() || uri == null) return false;
        String actual;
        try {
            actual = credentialScope(uri);
        } catch (IllegalArgumentException e) {
            return false;
        }
        for (String approved : approvedOrigins.split("\\R")) {
            String candidate = approved.trim();
            if (candidate.isEmpty()) continue;
            if (sameRepository(candidate, actual)) return true;
        }
        return false;
    }

    private static boolean sameRepository(String approved, String actual) {
        if (approved.equalsIgnoreCase(actual)) return true;
        // JGit may add a service path such as /info/refs below the repository path. Compare with
        // a path boundary so /team/repo cannot approve /team/repo-evil.
        String prefix = approved.endsWith("/") ? approved : approved + "/";
        return actual.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Returns every network remote origin configured for this repository, sorted and separated by
     * newlines. Used to bind a user's credentials to the URLs they were saved for.
     */
    static String approvedOrigins(Repository repository) {
        TreeSet<String> origins = new TreeSet<>();
        try {
            for (RemoteConfig remote : RemoteConfig.getAllRemoteConfigs(repository.getConfig())) {
                for (URIish uri : remote.getURIs()) addOrigin(origins, uri);
                for (URIish uri : remote.getPushURIs()) addOrigin(origins, uri);
            }
        } catch (URISyntaxException | RuntimeException e) {
            // A malformed remote contributes no approved origin.
        }
        return String.join("\n", origins);
    }

    /** Returns the origins of one remote's fetch and push URLs, sorted and newline-separated. */
    static String approvedOrigins(Repository repository, String remote) {
        TreeSet<String> origins = new TreeSet<>();
        try {
            RemoteConfig config = new RemoteConfig(repository.getConfig(), remote);
            for (URIish uri : config.getURIs()) addOrigin(origins, uri);
            for (URIish uri : config.getPushURIs()) addOrigin(origins, uri);
        } catch (URISyntaxException | RuntimeException e) {
            // A malformed remote contributes no approved origin.
        }
        return String.join("\n", origins);
    }

    private static void addOrigin(TreeSet<String> origins, URIish uri) {
        try {
            if (uri != null && uri.getHost() != null) origins.add(credentialScope(uri));
        } catch (RuntimeException ignored) {
            // Local paths and unsupported schemes are never credential destinations.
        }
    }

    /**
     * Rejects repository settings that can redirect a fetch or execute a remote helper: mirrors,
     * fetch refspecs outside the remote-tracking namespace, and custom upload/receive commands.
     */
    static void validateConfig(StoredConfig config) throws IOException {
        try {
            for (RemoteConfig remote : RemoteConfig.getAllRemoteConfigs(config)) {
                String name = remote.getName();
                if (config.getBoolean("remote", name, "mirror", false)) {
                    throw new IOException("Refusing repository with mirror remote '" + name + "'.");
                }
                for (String option : new String[] {"uploadpack", "receivepack", "vcs"}) {
                    String value = config.getString("remote", name, option);
                    if (value != null && !value.isBlank()) {
                        throw new IOException(
                                "Refusing remote '" + name + "' with a custom " + option + ".");
                    }
                }
                for (RefSpec spec : remote.getFetchRefSpecs()) {
                    String source = spec.getSource();
                    String destination = spec.getDestination();
                    if (source == null
                            || destination == null
                            || !(source.startsWith("refs/heads/")
                                    || source.startsWith("refs/tags/"))
                            || !(destination.startsWith("refs/remotes/" + name + "/")
                                    || destination.startsWith("refs/tags/"))) {
                        throw new IOException(
                                "Refusing unsafe fetch refspec for remote '" + name + "'.");
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new IOException("Refusing repository with an invalid remote URL.", e);
        }
    }

    /** Sanitizes a legacy URL for migration; returns {@code null} when it must not be used. */
    static URIish sanitizeLegacyUrl(String legacyUrl) {
        if (legacyUrl == null || legacyUrl.isBlank()) return null;
        try {
            URIish parsed = new URIish(legacyUrl.trim());
            requireNetworkUri(parsed);
            if (parsed.getPass() != null) {
                LOG.warn(
                        "A legacy Git remote URL contained embedded credentials. The URL was migrated without them; save the credential in Repository Settings.");
            }
            URIish sanitized = parsed.setPass(null);
            String scheme = sanitized.getScheme();
            if (scheme != null
                    && ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) {
                sanitized = sanitized.setUser(null);
            }
            return sanitized;
        } catch (Exception e) {
            LOG.warn("A legacy Git remote URL is invalid or unsupported and was not migrated.");
            return null;
        }
    }

    /** Describes a remote's URLs without any embedded password. */
    static String describe(List<URIish> uris) {
        StringBuilder text = new StringBuilder();
        for (URIish uri : uris) {
            if (text.length() > 0) text.append('\n');
            text.append(uri.setPass(null).toString());
        }
        return text.toString();
    }

    static List<RemoteConfig> list(Git git) throws Exception {
        return git.remoteList().call();
    }

    private Remotes() {}
}
