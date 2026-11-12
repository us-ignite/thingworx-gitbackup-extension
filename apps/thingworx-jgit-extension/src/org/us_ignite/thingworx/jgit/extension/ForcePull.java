package org.us_ignite.thingworx.jgit.extension;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.CredentialsProvider;

/** Fetches origin before replacing a local branch with its remote tracking branch. */
final class ForcePull {
    static String resetToOrigin(Git git, CredentialsProvider credentials) throws Exception {
        var repository = git.getRepository();
        String fullBranch = repository.getFullBranch();
        if (fullBranch == null || !fullBranch.startsWith(Constants.R_HEADS)) {
            throw new IllegalStateException("Force pull requires a checked-out local branch.");
        }
        String branch = fullBranch.substring(Constants.R_HEADS.length());
        String remote = repository.getConfig().getString("branch", branch, "remote");
        String merge = repository.getConfig().getString("branch", branch, "merge");
        if (remote != null && !remote.equals("origin")) {
            throw new IllegalStateException("Current branch does not track origin.");
        }
        if (merge != null && !merge.startsWith(Constants.R_HEADS)) {
            throw new IllegalStateException("Current branch has an invalid upstream branch.");
        }
        String remoteBranch = merge == null ? branch : merge.substring(Constants.R_HEADS.length());
        git.fetch().setRemote("origin").setCredentialsProvider(credentials).call();
        String target = Constants.R_REMOTES + "origin/" + remoteBranch;
        if (repository.resolve(target) == null) {
            throw new IllegalStateException("Remote branch not found: " + target);
        }
        git.reset().setMode(ResetType.HARD).setRef(target).call();
        return target;
    }

    private ForcePull() {}
}
