package org.us_ignite.thingworx.jgit.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;

/**
 * Builds the result messages of Pull, Merge, Rebase, Push and the branch services from JGit's
 * result fields, instead of the results' {@code toString()} output, and the messages of Sync to and
 * from ThingWorx. It needs no running ThingWorx, so tests can call it directly.
 */
final class ResultMessages {
    private static final Pattern ANSI = Pattern.compile("\u001B\\[[0-9;]*[A-Za-z]");
    private static final Pattern BANNER = Pattern.compile("^[\\s=*#\\-_~+]*$");

    private ResultMessages() {}

    /** A 7-character commit ID, or an empty string. */
    static String shortId(AnyObjectId id) {
        return id == null || ObjectId.zeroId().equals(id) ? "" : id.abbreviate(7).name();
    }

    /** A branch name without its {@code refs/heads/} prefix. */
    static String branchName(String ref) {
        if (ref == null) return "";
        return ref.startsWith(Constants.R_HEADS) ? ref.substring(Constants.R_HEADS.length()) : ref;
    }

    /**
     * What a merge did to {@code branch}, such as {@code fast-forward to 3b25ae7} or {@code merged
     * 423e954 into main (merge commit 7f2c0c5)}.
     */
    static String merge(MergeResult result, String branch) {
        ObjectId[] mergedCommits = result.getMergedCommits();
        String incoming =
                mergedCommits != null && mergedCommits.length > 1
                        ? shortId(mergedCommits[mergedCommits.length - 1])
                        : "";
        String head = shortId(result.getNewHead());
        return switch (result.getMergeStatus()) {
            case ALREADY_UP_TO_DATE -> "already up to date";
            case FAST_FORWARD, FAST_FORWARD_SQUASHED -> "fast-forward to " + head;
            case MERGED ->
                    "merged " + incoming + " into " + branch + " (merge commit " + head + ")";
            case MERGED_NOT_COMMITTED, MERGED_SQUASHED, MERGED_SQUASHED_NOT_COMMITTED ->
                    "merged " + incoming + " into " + branch + " without committing";
            default -> status(result.getMergeStatus().name());
        };
    }

    /** What a rebase did to {@code branch}, such as {@code rebased main onto 3b25ae7}. */
    static String rebase(RebaseResult result, String branch, AnyObjectId onto) {
        return switch (result.getStatus()) {
            case UP_TO_DATE -> "already up to date";
            case FAST_FORWARD -> "fast-forward to " + shortId(onto);
            case OK -> "rebased " + branch + " onto " + shortId(onto);
            default -> status(result.getStatus().name());
        };
    }

    /**
     * The result of a pull, such as {@code Pulled origin/main: fast-forward to 3b25ae7}.
     *
     * @param remoteBranch the branch pulled from {@code remote}
     * @param branch the local branch
     */
    static String pull(PullResult result, String remote, String remoteBranch, String branch) {
        String source = remote + "/" + branchName(remoteBranch);
        String outcome;
        if (result == null) outcome = "done";
        else if (result.getMergeResult() != null) outcome = merge(result.getMergeResult(), branch);
        else if (result.getRebaseResult() != null) {
            RebaseResult rebase = result.getRebaseResult();
            outcome =
                    rebase.getStatus() == RebaseResult.Status.OK
                            ? "rebased " + branch + " onto " + source
                            : rebase(rebase, branch, null);
        } else outcome = "fetched";
        return "Pulled " + source + ": " + outcome;
    }

    /**
     * What a successful push did to one remote ref, such as {@code Pushed main to origin (new
     * branch, 7a0d318)}.
     */
    static String pushed(PushResult result, RemoteRefUpdate update, String source, String remote) {
        String destination = branchName(update.getRemoteName());
        String target = destination.equals(source) ? remote : remote + " as " + destination;
        String id = shortId(update.getNewObjectId());
        if (update.getStatus() == RemoteRefUpdate.Status.UP_TO_DATE)
            return source + " is already up to date on " + target;
        if (update.isDelete()) return "Deleted " + destination + " on " + remote;
        if (result.getAdvertisedRef(update.getRemoteName()) == null)
            return "Pushed " + source + " to " + target + " (new branch, " + id + ")";
        if (!update.isFastForward())
            return "Pushed " + source + " to " + target + " (forced update to " + id + ")";
        return "Pushed " + source + " to " + target + " (fast-forward to " + id + ")";
    }

    /** Whether the remote accepted the update. */
    static boolean accepted(RemoteRefUpdate update) {
        return update.getStatus() == RemoteRefUpdate.Status.OK
                || update.getStatus() == RemoteRefUpdate.Status.UP_TO_DATE;
    }

    /**
     * Why the remote refused an update, with the remote's own explanation, such as a pre-receive
     * hook's {@code Commit 7f2c0c5 is not signed.} It does not mention credentials: a remote that
     * rejects the sign-in fails the connection instead.
     */
    static String pushRejected(PushResult result, RemoteRefUpdate update, String remote) {
        String branch = branchName(update.getRemoteName());
        String reason =
                switch (update.getStatus()) {
                    case REJECTED_NONFASTFORWARD -> Const.ERR_PUSH_NON_FAST_FORWARD;
                    case REJECTED_NODELETE -> "The remote does not allow deleting " + branch + ".";
                    case REJECTED_REMOTE_CHANGED ->
                            branch + " changed on the remote during the push. Try again.";
                    case NON_EXISTING -> branch + " does not exist on the remote.";
                    case AWAITING_REPORT, NOT_ATTEMPTED ->
                            "The remote did not report a result for " + branch + ".";
                    default -> sentence(update.getMessage());
                };
        String output = remoteOutput(result == null ? null : result.getMessages());
        StringBuilder message = new StringBuilder("Push rejected by ").append(remote);
        if (reason.isEmpty() && output.isEmpty())
            return message.append(": ").append(branch).append(" was rejected.").toString();
        if (reason.isEmpty()) return message.append(": ").append(output).toString();
        message.append(": ").append(reason);
        if (!output.isEmpty()) message.append('\n').append(output);
        return message.toString();
    }

    /**
     * The lines the remote printed during the push, without blank lines, banner lines made only of
     * punctuation, terminal colour codes, and the {@code remote:} prefix.
     */
    static String remoteOutput(String messages) {
        if (messages == null || messages.isBlank()) return "";
        List<String> lines = new ArrayList<>();
        for (String raw : messages.split("\\r?\\n|\\r")) {
            String line = ANSI.matcher(raw).replaceAll("").strip();
            if (line.regionMatches(true, 0, "remote:", 0, 7)) line = line.substring(7).strip();
            if (line.isEmpty() || BANNER.matcher(line).matches()) continue;
            lines.add(line);
        }
        return String.join("\n", lines);
    }

    private static String sentence(String text) {
        if (text == null || text.isBlank()) return "";
        String trimmed = text.strip();
        if (trimmed.isEmpty()) return "";
        trimmed = Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
        return trimmed.endsWith(".") ? trimmed : trimmed + ".";
    }

    private static String status(String name) {
        return name.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    /** How many untracked files a Sync to ThingWorx result names before summarizing. */
    private static final int UNTRACKED_SHOWN = 10;

    /**
     * Names the Users created disabled, or returns blank. Users that could not be imported are
     * reported with every other failure of the sync.
     */
    static String usersNote(List<String> created) {
        if (created.isEmpty()) return "";
        return " "
                + (created.size() == 1 ? "User " : "Users ")
                + String.join(", ", created)
                + (created.size() == 1 ? " was" : " were")
                + " created disabled with a random password; set a password and"
                + " enable "
                + (created.size() == 1 ? "it." : "them.");
    }

    /** Names the untracked files a Sync to ThingWorx left out, or returns blank. */
    static String untrackedNote(List<String> untracked) {
        if (untracked.isEmpty()) return "";
        List<String> shown = untracked.subList(0, Math.min(untracked.size(), UNTRACKED_SHOWN));
        String names = String.join(", ", shown);
        if (untracked.size() > shown.size()) {
            names += " and " + (untracked.size() - shown.size()) + " more";
        }
        return " "
                + untracked.size()
                + (untracked.size() == 1 ? " untracked file was" : " untracked files were")
                + " not imported: "
                + names
                + ". Add, commit and push a file to include it.";
    }

    /** The message of a Sync from ThingWorx, naming what failed. */
    static String exportMessage(String projects, FileRepositoryManager.ExportResult result) {
        SyncFailures failures = result.failures();
        if (failures.isEmpty()) return "Exported " + projects + " from ThingWorx.";
        String message =
                result.exported()
                        ? "Partially exported " + projects + " from ThingWorx."
                        : "Nothing was exported from ThingWorx.";
        return message
                + failures.note("not exported")
                + " Their files were left as they were. The last sync time was not updated, so"
                + " Sync to ThingWorx still warns before overwriting ThingWorx changes.";
    }

    /** The message of a Sync to ThingWorx, naming what was left out and what failed. */
    static String importMessage(FileRepositoryManager.ImportResult result) {
        SyncFailures failures = result.failures();
        String message;
        if (failures.isEmpty()) {
            message =
                    result.imported()
                            ? "Repository entities imported into ThingWorx."
                            : "No tracked XML entities found to import.";
        } else {
            message =
                    result.imported()
                            ? "Repository entities partially imported into ThingWorx."
                            : "Nothing was imported into ThingWorx.";
        }
        message += untrackedNote(result.untracked());
        message += usersNote(result.createdUsers());
        if (!result.dataTables().isEmpty()) {
            message += " Data Table rows imported: " + String.join(", ", result.dataTables()) + ".";
        }
        if (!result.dataTableNotes().isEmpty()) {
            message +=
                    " Data Tables left alone: " + String.join(", ", result.dataTableNotes()) + ".";
        }
        if (!failures.isEmpty()) {
            message +=
                    failures.note("not imported")
                            + " They keep their ThingWorx copy; fix them in Git and sync again.";
        }
        return message;
    }
}
