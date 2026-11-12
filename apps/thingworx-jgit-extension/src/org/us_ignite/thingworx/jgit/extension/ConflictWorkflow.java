package org.us_ignite.thingworx.jgit.extension;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.Consumer;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.StashApplyFailureException;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Runs Git operations that can conflict so that their conflicts can be resolved from the UI, and so
 * that cancelling restores the repository to where it was before the operation.
 *
 * <p>Before an operation, uncommitted tracked changes are stashed and the original branch and
 * commit are recorded in {@code .git/twx-conflict-state}. After the operation succeeds, the stash
 * is re-applied. If the operation itself or the re-applied changes conflict, the repository is left
 * with unmerged index entries and the record is kept until the conflict is completed or cancelled.
 * Callers must not export ThingWorx entities over the working tree, or import them from it, while
 * {@link #inProgress(Git)} is true.
 */
final class ConflictWorkflow {
    static final String STATE_FILE = "twx-conflict-state";

    /** What kind of conflict the repository is waiting on. */
    enum Phase {
        NONE,
        MERGE,
        REBASE,
        LOCAL_CHANGES,
        OTHER
    }

    /** Whether an operation finished or stopped for conflict resolution. */
    enum Outcome {
        COMPLETED,
        CONFLICTS
    }

    /** A Git operation that throws when it fails for a reason other than a content conflict. */
    @FunctionalInterface
    interface GitOperation {
        Outcome run() throws Exception;
    }

    /** The pre-operation state that cancelling restores. */
    record Snapshot(
            String operation,
            String originalBranch,
            ObjectId originalHead,
            ObjectId stash,
            boolean stashApplied) {
        Snapshot withStashApplied() {
            return new Snapshot(operation, originalBranch, originalHead, stash, true);
        }
    }

    /** A summary of the conflict the repository is waiting on. */
    record State(
            Phase phase,
            String operation,
            String repositoryState,
            String currentLabel,
            String incomingLabel,
            String description,
            List<String> conflicts,
            boolean canSkip) {
        boolean inProgress() {
            return phase != Phase.NONE;
        }
    }

    /** The versions of one conflicted file. A missing version is {@code null}. */
    record Versions(
            String path,
            byte[] base,
            byte[] current,
            byte[] incoming,
            byte[] working,
            boolean conflicted) {
        boolean binary() {
            for (byte[] content : new byte[][] {base, current, incoming, working}) {
                if (content != null && RawText.isBinary(content)) return true;
            }
            return false;
        }
    }

    private ConflictWorkflow() {}

    // ---------------------------------------------------------------- operations

    /**
     * Runs {@code operation} with uncommitted changes stashed. On {@link Outcome#COMPLETED} the
     * changes are re-applied; when that conflicts, {@link Outcome#CONFLICTS} is returned. If the
     * operation throws, the repository is restored and the exception is rethrown.
     */
    static Outcome run(Git git, String operationName, GitOperation operation) throws Exception {
        requireNotInProgress(git);
        Repository repository = git.getRepository();
        ObjectId head = repository.resolve(Constants.HEAD);
        if (head == null) return operation.run();
        ObjectId stash = null;
        if (git.status().call().hasUncommittedChanges()) {
            RevCommit created =
                    git.stashCreate()
                            .setIndexMessage("ThingWorx Git: index before " + operationName)
                            .setWorkingDirectoryMessage(
                                    "ThingWorx Git: local changes before " + operationName)
                            .call();
            if (created != null) stash = created.getId();
        }
        Snapshot snapshot =
                new Snapshot(operationName, repository.getFullBranch(), head, stash, false);
        write(repository, snapshot);
        Outcome outcome;
        try {
            outcome = operation.run();
        } catch (Exception e) {
            try {
                restore(git, snapshot);
            } catch (Exception restoreFailure) {
                e.addSuppressed(restoreFailure);
            }
            throw e;
        }
        if (outcome == Outcome.CONFLICTS) {
            if (unmerged(git).isEmpty()) {
                // A stopped merge or rebase without unmerged paths cannot be resolved here.
                restore(git, snapshot);
                throw new IllegalStateException(
                        operationName + " stopped without reporting conflicted files.");
            }
            return Outcome.CONFLICTS;
        }
        return reapplyLocalChanges(git, snapshot);
    }

    /** Maps a pull result to an outcome, throwing for failures other than conflicts. */
    static Outcome outcome(PullResult result) {
        if (result.isSuccessful()) return Outcome.COMPLETED;
        MergeResult merge = result.getMergeResult();
        if (merge != null && merge.getMergeStatus() == MergeResult.MergeStatus.CONFLICTING)
            return Outcome.CONFLICTS;
        RebaseResult rebase = result.getRebaseResult();
        if (rebase != null && rebase.getStatus() == RebaseResult.Status.STOPPED)
            return Outcome.CONFLICTS;
        throw new IllegalStateException("Git pull was unsuccessful: " + result);
    }

    /** Maps a merge result to an outcome, throwing for failures other than conflicts. */
    static Outcome outcome(MergeResult result) {
        if (result.getMergeStatus().isSuccessful()) return Outcome.COMPLETED;
        if (result.getMergeStatus() == MergeResult.MergeStatus.CONFLICTING)
            return Outcome.CONFLICTS;
        throw new IllegalStateException("Merge was unsuccessful: " + result);
    }

    /** Maps a rebase result to an outcome, throwing for failures other than conflicts. */
    static Outcome outcome(RebaseResult result) {
        if (result.getStatus() == RebaseResult.Status.STOPPED) return Outcome.CONFLICTS;
        if (result.getStatus().isSuccessful()) return Outcome.COMPLETED;
        throw new IllegalStateException("Rebase was unsuccessful: " + result.getStatus());
    }

    /**
     * Completes the conflicted operation after every file is resolved. A merge is committed with
     * {@code commitSetup} applied to the commit command; a rebase continues with its next commit.
     *
     * @return {@link Outcome#CONFLICTS} when the next rebase step or the re-applied local changes
     *     conflict again
     */
    static Outcome continueOperation(Git git, String message, Consumer<CommitCommand> commitSetup)
            throws Exception {
        Repository repository = git.getRepository();
        List<String> remaining = unmerged(git);
        if (!remaining.isEmpty()) {
            throw new IllegalStateException(
                    remaining.size()
                            + " file(s) still have conflicts: "
                            + String.join(", ", remaining));
        }
        Snapshot snapshot = read(repository);
        RepositoryState state = repository.getRepositoryState();
        if (state == RepositoryState.MERGING || state == RepositoryState.MERGING_RESOLVED) {
            String text = message == null || message.isBlank() ? null : message;
            if (text == null) text = repository.readMergeCommitMsg();
            if (text == null || text.isBlank()) text = "Merge";
            CommitCommand commit = git.commit().setMessage(text);
            if (commitSetup != null) commitSetup.accept(commit);
            commit.call();
        } else if (state.isRebasing()) {
            Outcome outcome =
                    outcome(git.rebase().setOperation(RebaseCommand.Operation.CONTINUE).call());
            if (outcome == Outcome.CONFLICTS) return outcome;
        } else if (snapshot == null) {
            throw new IllegalStateException("No conflicted operation is in progress.");
        } else if (state != RepositoryState.SAFE) {
            throw new IllegalStateException(
                    "Unsupported repository state: " + state.getDescription());
        }
        if (snapshot == null) return Outcome.COMPLETED;
        if (snapshot.stashApplied()) {
            dropStash(git, snapshot.stash());
            clear(repository);
            return Outcome.COMPLETED;
        }
        return reapplyLocalChanges(git, snapshot);
    }

    /** Skips the commit a rebase stopped on. */
    static Outcome skip(Git git) throws Exception {
        Repository repository = git.getRepository();
        if (!repository.getRepositoryState().isRebasing())
            throw new IllegalStateException("Only a paused rebase can skip a commit.");
        Outcome outcome = outcome(git.rebase().setOperation(RebaseCommand.Operation.SKIP).call());
        if (outcome == Outcome.CONFLICTS) return outcome;
        Snapshot snapshot = read(repository);
        return snapshot == null ? Outcome.COMPLETED : reapplyLocalChanges(git, snapshot);
    }

    /**
     * Cancels the conflicted operation. With a recorded snapshot, the original branch, commit, and
     * uncommitted changes are restored; otherwise the merge or rebase is aborted to {@code HEAD}.
     */
    static void abort(Git git) throws Exception {
        Repository repository = git.getRepository();
        Snapshot snapshot = read(repository);
        if (snapshot != null) {
            restore(git, snapshot);
            return;
        }
        RepositoryState state = repository.getRepositoryState();
        if (state.isRebasing()) {
            git.rebase().setOperation(RebaseCommand.Operation.ABORT).call();
            return;
        }
        if (state == RepositoryState.SAFE && unmerged(git).isEmpty())
            throw new IllegalStateException("No conflicted operation is in progress.");
        clearOperationState(repository);
        git.reset().setMode(ResetType.HARD).setRef(Constants.HEAD).call();
    }

    // ---------------------------------------------------------------- state

    static boolean inProgress(Git git) throws Exception {
        Repository repository = git.getRepository();
        return stateFile(repository).toFile().isFile()
                || repository.getRepositoryState() != RepositoryState.SAFE
                || !unmerged(git).isEmpty();
    }

    /** Throws a {@link Const#ERR_PREFIX_CONFLICT} error when a conflict is waiting. */
    static void requireNotInProgress(Git git) throws Exception {
        if (!inProgress(git)) return;
        String operation = state(git).operation();
        throw new IllegalStateException(
                Const.ERR_PREFIX_CONFLICT
                        + String.format(
                                Const.ERR_CONFLICT_IN_PROGRESS,
                                operation,
                                operation.toLowerCase(java.util.Locale.ROOT)));
    }

    static State state(Git git) throws Exception {
        Repository repository = git.getRepository();
        Snapshot snapshot = read(repository);
        RepositoryState repositoryState = repository.getRepositoryState();
        List<String> conflicts = unmerged(git);
        String branch = repository.getBranch();
        String operation = snapshot == null ? null : snapshot.operation();
        Phase phase;
        String current;
        String incoming;
        String description;
        if (repositoryState.isRebasing()) {
            phase = Phase.REBASE;
            String onto = rebaseValue(repository, "onto_name");
            if (onto == null) onto = abbreviate(rebaseValue(repository, "onto"));
            String commitMessage = firstLine(rebaseValue(repository, "message"));
            current = "Upstream" + (onto == null ? "" : " (" + onto + ")");
            incoming =
                    "Your commit"
                            + (commitMessage == null || commitMessage.isBlank()
                                    ? ""
                                    : ": " + commitMessage);
            description =
                    "Your commits are being replayed onto the upstream branch. One of them changes"
                            + " the same lines as the upstream branch.";
            if (operation == null) operation = "Rebase";
        } else if (repositoryState == RepositoryState.MERGING
                || repositoryState == RepositoryState.MERGING_RESOLVED) {
            phase = Phase.MERGE;
            current = "Current branch (" + branch + ")";
            String mergeMessage = firstLine(repository.readMergeCommitMsg());
            incoming = mergeMessage == null || mergeMessage.isBlank() ? "Incoming" : mergeMessage;
            incoming = "Incoming: " + incoming.replaceFirst("^Merge ", "");
            description =
                    "The incoming commits change the same lines as commits on "
                            + branch
                            + ". Your uncommitted changes, if any, are restored after the merge.";
            if (operation == null) operation = "Merge";
        } else if (snapshot != null && snapshot.stashApplied()) {
            phase = Phase.LOCAL_CHANGES;
            current = branch + " after " + operation;
            incoming = "Your uncommitted changes";
            description =
                    operation
                            + " finished, but your uncommitted changes conflict with it. Choose how"
                            + " to combine them.";
        } else if (!conflicts.isEmpty() || snapshot != null) {
            phase = Phase.OTHER;
            current = "Current";
            incoming = "Incoming";
            description = "The repository has conflicted files.";
            if (operation == null) operation = repositoryState.getDescription();
        } else {
            phase = Phase.NONE;
            current = "";
            incoming = "";
            description = "";
            operation = "";
        }
        return new State(
                phase,
                operation,
                repositoryState.name(),
                current,
                incoming,
                description,
                conflicts,
                phase == Phase.REBASE);
    }

    static List<String> unmerged(Git git) throws Exception {
        return new ArrayList<>(new TreeSet<>(git.status().call().getConflicting()));
    }

    // ---------------------------------------------------------------- files

    static Versions versions(Git git, String path) throws Exception {
        Repository repository = git.getRepository();
        String file = checkPath(repository, path);
        byte[][] stages = new byte[4][];
        DirCache index = repository.readDirCache();
        boolean conflicted = false;
        for (int i = index.findEntry(file); i >= 0 && i < index.getEntryCount(); i++) {
            DirCacheEntry entry = index.getEntry(i);
            if (!entry.getPathString().equals(file)) break;
            int stage = entry.getStage();
            if (stage > 0) conflicted = true;
            stages[stage] = repository.open(entry.getObjectId()).getCachedBytes(Integer.MAX_VALUE);
        }
        Path working = repository.getWorkTree().toPath().resolve(file);
        byte[] workingContent =
                Files.isRegularFile(working, LinkOption.NOFOLLOW_LINKS)
                        ? Files.readAllBytes(working)
                        : null;
        return new Versions(
                file,
                stages[1],
                conflicted ? stages[2] : stages[0],
                stages[3],
                workingContent,
                conflicted);
    }

    /**
     * Resolves one file and stages the result.
     *
     * @param resolution {@code current}, {@code incoming}, {@code content}, or {@code delete}
     * @param content resolved text when {@code resolution} is {@code content}
     */
    static String resolve(Git git, String path, String resolution, String content)
            throws Exception {
        Repository repository = git.getRepository();
        if (!inProgress(git))
            throw new IllegalStateException("No conflicted operation is in progress.");
        String file = checkPath(repository, path);
        Versions versions = versions(git, file);
        String choice = resolution == null ? "" : resolution.trim().toLowerCase();
        byte[] result;
        switch (choice) {
            case "current", "ours" -> result = versions.current();
            case "incoming", "theirs" -> result = versions.incoming();
            case "delete" -> result = null;
            case "content" -> {
                if (content == null) throw new IllegalArgumentException("Content is required.");
                result = withLineEndingsOf(content, versions).getBytes(StandardCharsets.UTF_8);
            }
            default ->
                    throw new IllegalArgumentException(
                            "Resolution must be current, incoming, content, or delete.");
        }
        Path target = repository.getWorkTree().toPath().resolve(file);
        if (result == null) {
            Files.deleteIfExists(target);
            git.rm().addFilepattern(file).setCached(true).call();
            return "Resolved " + file + " by deleting it.";
        }
        if (Files.isSymbolicLink(target)) Files.delete(target);
        Files.createDirectories(target.getParent());
        Files.write(target, result);
        git.add().addFilepattern(file).call();
        return "Resolved " + file + ".";
    }

    private static String withLineEndingsOf(String content, Versions versions) {
        String normalized = content.replace("\r\n", "\n");
        for (byte[] sample : new byte[][] {versions.current(), versions.incoming()}) {
            if (sample != null && new String(sample, StandardCharsets.UTF_8).contains("\r\n")) {
                return normalized.replace("\n", "\r\n");
            }
        }
        return normalized;
    }

    private static String checkPath(Repository repository, String path) throws IOException {
        if (path == null || path.isBlank()) throw new IllegalArgumentException("File is required.");
        String file = path.replace('\\', '/').replaceAll("^/+", "");
        Path root = repository.getWorkTree().toPath().toAbsolutePath().normalize();
        Path resolved = root.resolve(file).normalize();
        if (!resolved.startsWith(root)
                || resolved.equals(root)
                || resolved.startsWith(root.resolve(Constants.DOT_GIT)))
            throw new IOException("File is outside the repository: " + path);
        return root.relativize(resolved).toString().replace('\\', '/');
    }

    // ---------------------------------------------------------------- internals

    private static Outcome reapplyLocalChanges(Git git, Snapshot snapshot) throws Exception {
        Repository repository = git.getRepository();
        if (snapshot.stash() != null) {
            try {
                git.stashApply().setStashRef(snapshot.stash().name()).call();
            } catch (StashApplyFailureException e) {
                if (!unmerged(git).isEmpty()) {
                    write(repository, snapshot.withStashApplied());
                    return Outcome.CONFLICTS;
                }
                // Nothing to resolve: put everything back the way it was before the operation.
                restore(git, snapshot);
                throw new IllegalStateException(
                        snapshot.operation()
                                + " was undone because your uncommitted changes could not be"
                                + " re-applied: "
                                + e.getMessage(),
                        e);
            }
            dropStash(git, snapshot.stash());
        }
        clear(repository);
        return Outcome.COMPLETED;
    }

    private static void restore(Git git, Snapshot snapshot) throws Exception {
        Repository repository = git.getRepository();
        if (repository.getRepositoryState().isRebasing()) {
            try {
                git.rebase().setOperation(RebaseCommand.Operation.ABORT).call();
            } catch (Exception ignored) {
                // A hard reset to the original commit below still restores the tree.
            }
        }
        clearOperationState(repository);
        git.reset().setMode(ResetType.HARD).setRef(Constants.HEAD).call();
        String original = snapshot.originalBranch();
        if (original != null && original.startsWith(Constants.R_HEADS)) {
            if (!original.equals(repository.getFullBranch())) {
                git.checkout()
                        .setName(original.substring(Constants.R_HEADS.length()))
                        .setForced(true)
                        .call();
            }
        } else {
            git.checkout().setName(snapshot.originalHead().name()).setForced(true).call();
        }
        git.reset().setMode(ResetType.HARD).setRef(snapshot.originalHead().name()).call();
        if (snapshot.stash() != null) {
            git.stashApply().setStashRef(snapshot.stash().name()).call();
            dropStash(git, snapshot.stash());
        }
        clear(repository);
    }

    private static void clearOperationState(Repository repository) throws IOException {
        repository.writeMergeHeads(null);
        repository.writeMergeCommitMsg(null);
        repository.writeCherryPickHead(null);
        repository.writeRevertHead(null);
    }

    private static void dropStash(Git git, ObjectId stash) throws Exception {
        if (stash == null) return;
        Collection<RevCommit> entries = git.stashList().call();
        int index = 0;
        for (RevCommit entry : entries) {
            if (entry.getId().equals(stash)) {
                git.stashDrop().setStashRef(index).call();
                return;
            }
            index++;
        }
    }

    private static String rebaseValue(Repository repository, String name) {
        for (String directory : new String[] {"rebase-merge", "rebase-apply"}) {
            Path file = repository.getDirectory().toPath().resolve(directory).resolve(name);
            try {
                if (Files.isRegularFile(file))
                    return Files.readString(file, StandardCharsets.UTF_8).trim();
            } catch (IOException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String abbreviate(String id) {
        return id == null || id.length() < 8 ? id : id.substring(0, 8);
    }

    private static String firstLine(String text) {
        if (text == null) return null;
        int end = text.indexOf('\n');
        return (end < 0 ? text : text.substring(0, end)).trim();
    }

    private static Path stateFile(Repository repository) {
        return repository.getDirectory().toPath().resolve(STATE_FILE);
    }

    static Snapshot read(Repository repository) throws IOException {
        Path file = stateFile(repository);
        if (!Files.isRegularFile(file)) return null;
        Properties values = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            values.load(input);
        }
        String head = values.getProperty("originalHead", "");
        String stash = values.getProperty("stash", "");
        return new Snapshot(
                values.getProperty("operation", "Operation"),
                values.getProperty("originalBranch", ""),
                head.isEmpty() ? null : ObjectId.fromString(head),
                stash.isEmpty() ? null : ObjectId.fromString(stash),
                Boolean.parseBoolean(values.getProperty("stashApplied", "false")));
    }

    private static void write(Repository repository, Snapshot snapshot) throws IOException {
        Properties values = new Properties();
        values.setProperty("operation", snapshot.operation());
        values.setProperty(
                "originalBranch",
                snapshot.originalBranch() == null ? "" : snapshot.originalBranch());
        values.setProperty("originalHead", snapshot.originalHead().name());
        values.setProperty("stash", snapshot.stash() == null ? "" : snapshot.stash().name());
        values.setProperty("stashApplied", String.valueOf(snapshot.stashApplied()));
        Path file = stateFile(repository);
        Path temporary = file.resolveSibling(STATE_FILE + ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) {
            values.store(output, "ThingWorx Git conflict resolution state");
        }
        Files.move(
                temporary,
                file,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    private static void clear(Repository repository) throws IOException {
        Files.deleteIfExists(stateFile(repository));
    }
}
