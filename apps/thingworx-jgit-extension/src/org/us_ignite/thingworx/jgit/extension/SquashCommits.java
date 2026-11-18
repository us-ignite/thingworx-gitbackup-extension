package org.us_ignite.thingworx.jgit.extension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Signer;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * Squashes consecutive commits on the current branch into one commit.
 *
 * <p>The squashed commit keeps the tree of the newest selected commit, so the commits after it keep
 * their own trees too: they are rebuilt on the new parent without any merge, and the working tree
 * and index are not touched. Every commit created here, the squashed one and the rebuilt later
 * ones, is signed with {@code signer} when one is given, so squashing also replaces unsigned
 * commits above the selection with signed ones.
 */
final class SquashCommits {

    /** Commits walked from HEAD looking for the selection before giving up. */
    static final int MAX_WALK = 10_000;

    /**
     * The outcome of a squash.
     *
     * @param newHead the branch tip after the squash
     * @param squashedId the commit that replaces the selection
     * @param squashed number of commits folded into {@code squashedId}
     * @param rewritten number of later commits rebuilt on top of it
     * @param published whether a remote-tracking branch already holds a selected commit, so the
     *     next push must be forced
     */
    record Result(
            ObjectId newHead,
            ObjectId squashedId,
            int squashed,
            int rewritten,
            boolean published) {}

    private SquashCommits() {}

    /**
     * Replaces the selected commits with one commit and rebuilds the commits after them.
     *
     * @param commitIds IDs of at least two consecutive commits on the current branch's first-parent
     *     line, in any order; abbreviated IDs are accepted
     * @param message message for the squashed commit; blank joins the selected messages, oldest
     *     first
     * @param committer committer for every created commit
     * @param signer signs every created commit, or {@code null} for unsigned commits
     * @throws IllegalArgumentException when the selection is not a consecutive run of at least two
     *     commits on the current branch, or includes a merge commit
     * @throws IllegalStateException when HEAD is detached or the branch moved during the squash
     */
    static Result squash(
            Repository repo,
            Collection<String> commitIds,
            String message,
            PersonIdent committer,
            Signer signer)
            throws Exception {
        String branchRef = repo.getFullBranch();
        if (branchRef == null || !branchRef.startsWith(Constants.R_HEADS)) {
            throw new IllegalStateException(
                    Const.ERR_PREFIX_GIT
                            + "HEAD is detached. Check out a branch before squashing commits.");
        }
        ObjectId head = repo.resolve(Constants.HEAD);
        if (head == null) {
            throw new IllegalStateException(
                    Const.ERR_PREFIX_GIT + "The current branch has no commits.");
        }

        try (RevWalk walk = new RevWalk(repo);
                ObjectInserter inserter = repo.newObjectInserter()) {
            Set<ObjectId> selected = new HashSet<>();
            for (String id : commitIds) {
                if (id == null || id.isBlank()) continue;
                ObjectId resolved = repo.resolve(id.trim() + "^{commit}");
                if (resolved == null) {
                    throw new IllegalArgumentException(
                            Const.ERR_PREFIX_GIT + "Commit " + id.trim() + " was not found.");
                }
                selected.add(resolved);
            }
            if (selected.size() < 2) {
                throw new IllegalArgumentException(
                        Const.ERR_PREFIX_CONFIG + "Select at least two commits to squash.");
            }

            // Newest first, from HEAD down to the oldest selected commit.
            List<RevCommit> chain = new ArrayList<>();
            int found = 0;
            int newestIndex = -1;
            RevCommit current = walk.parseCommit(head);
            while (current != null && found < selected.size() && chain.size() < MAX_WALK) {
                if (current.getParentCount() > 1) {
                    throw new IllegalArgumentException(
                            Const.ERR_PREFIX_GIT
                                    + "Commit "
                                    + current.abbreviate(8).name()
                                    + " is a merge commit. Only commits after the last merge can"
                                    + " be squashed.");
                }
                if (selected.contains(current)) {
                    if (newestIndex < 0) newestIndex = chain.size();
                    found++;
                } else if (newestIndex >= 0) {
                    throw new IllegalArgumentException(
                            Const.ERR_PREFIX_CONFIG
                                    + "The selected commits must be next to each other. Commit "
                                    + current.abbreviate(8).name()
                                    + " sits between them; select it too, or squash fewer"
                                    + " commits.");
                }
                chain.add(current);
                current =
                        current.getParentCount() == 0
                                ? null
                                : walk.parseCommit(current.getParent(0));
            }
            if (found < selected.size()) {
                throw new IllegalArgumentException(
                        Const.ERR_PREFIX_CONFIG
                                + "Some selected commits are not on the current branch. Squash"
                                + " works on the checked-out branch only.");
            }

            int oldestIndex = chain.size() - 1;
            RevCommit oldest = chain.get(oldestIndex);
            RevCommit newest = chain.get(newestIndex);
            boolean published = isPublished(repo, walk, oldest);

            String squashMessage = message;
            if (squashMessage == null || squashMessage.isBlank()) {
                StringBuilder joined = new StringBuilder();
                for (int i = oldestIndex; i >= newestIndex; i--) {
                    if (joined.length() > 0) joined.append("\n\n");
                    joined.append(chain.get(i).getFullMessage().strip());
                }
                squashMessage = joined.toString();
            }

            GpgConfig gpgConfig = new GpgConfig(repo.getConfig());
            ObjectId parent = oldest.getParentCount() == 0 ? null : oldest.getParent(0);
            ObjectId squashedId =
                    insert(
                            repo,
                            inserter,
                            gpgConfig,
                            signer,
                            newest.getTree(),
                            parent,
                            oldest.getAuthorIdent(),
                            committer,
                            squashMessage);
            ObjectId tip = squashedId;
            for (int i = newestIndex - 1; i >= 0; i--) {
                RevCommit later = chain.get(i);
                tip =
                        insert(
                                repo,
                                inserter,
                                gpgConfig,
                                signer,
                                later.getTree(),
                                tip,
                                later.getAuthorIdent(),
                                committer,
                                later.getFullMessage());
            }
            inserter.flush();

            RefUpdate update = repo.updateRef(branchRef);
            update.setExpectedOldObjectId(head);
            update.setNewObjectId(tip);
            update.setForceUpdate(true);
            update.setRefLogIdent(committer);
            update.setRefLogMessage("squash: " + selected.size() + " commits", false);
            RefUpdate.Result result = update.update(walk);
            if (result != RefUpdate.Result.FORCED
                    && result != RefUpdate.Result.FAST_FORWARD
                    && result != RefUpdate.Result.NEW) {
                throw new IllegalStateException(
                        Const.ERR_PREFIX_GIT
                                + "Could not update "
                                + Repository.shortenRefName(branchRef)
                                + " ("
                                + result
                                + "). Refresh History and try again.");
            }
            return new Result(tip, squashedId, selected.size(), newestIndex, published);
        }
    }

    private static ObjectId insert(
            Repository repo,
            ObjectInserter inserter,
            GpgConfig gpgConfig,
            Signer signer,
            ObjectId tree,
            ObjectId parent,
            PersonIdent author,
            PersonIdent committer,
            String message)
            throws Exception {
        CommitBuilder commit = new CommitBuilder();
        commit.setTreeId(tree);
        if (parent != null) commit.setParentId(parent);
        commit.setAuthor(author);
        commit.setCommitter(committer);
        commit.setMessage(message);
        if (signer != null) {
            signer.signObject(repo, gpgConfig, commit, committer, null, null);
        }
        return inserter.insert(commit);
    }

    /** Whether any remote-tracking branch already contains {@code commit}. */
    private static boolean isPublished(Repository repo, RevWalk walk, RevCommit commit)
            throws IOException {
        for (Ref ref : repo.getRefDatabase().getRefsByPrefix(Constants.R_REMOTES)) {
            if (ref.getObjectId() == null || ref.isSymbolic()) continue;
            RevCommit tip;
            try {
                tip = walk.parseCommit(ref.getObjectId());
            } catch (IOException notACommit) {
                continue;
            }
            if (walk.isMergedInto(commit, tip)) return true;
        }
        return false;
    }
}
