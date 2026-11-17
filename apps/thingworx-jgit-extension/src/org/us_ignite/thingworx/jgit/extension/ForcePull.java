package org.us_ignite.thingworx.jgit.extension;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Constants;

/** Fetches a remote before replacing a local branch with its remote-tracking branch. */
final class ForcePull {
    /**
     * @param transport credentials for the fetch; {@code null} fetches anonymously
     * @param remote remote name; blank selects the branch upstream remote or {@code origin}
     */
    static String resetToRemote(Git git, GitTransport transport, String remote) throws Exception {
        var repository = git.getRepository();
        String fullBranch = repository.getFullBranch();
        if (fullBranch == null || !fullBranch.startsWith(Constants.R_HEADS)) {
            throw new IllegalStateException("Force pull requires a checked-out local branch.");
        }
        String branch = fullBranch.substring(Constants.R_HEADS.length());
        String resolved = Remotes.resolve(repository, remote, branch);
        String upstream = repository.getConfig().getString("branch", branch, "remote");
        String merge = repository.getConfig().getString("branch", branch, "merge");
        if (merge != null && !merge.startsWith(Constants.R_HEADS)) {
            throw new IllegalStateException("Current branch has an invalid upstream branch.");
        }
        // The upstream branch name only applies to the remote the branch tracks.
        String remoteBranch =
                merge == null || !resolved.equals(upstream)
                        ? branch
                        : merge.substring(Constants.R_HEADS.length());
        var fetch = git.fetch().setRemote(resolved);
        (transport == null ? fetch : transport.configure(fetch, resolved)).call();
        String target = Constants.R_REMOTES + resolved + "/" + remoteBranch;
        if (repository.resolve(target) == null) {
            throw new IllegalStateException("Remote branch not found: " + target);
        }
        git.reset().setMode(ResetType.HARD).setRef(target).call();
        return target;
    }

    private ForcePull() {}
}
