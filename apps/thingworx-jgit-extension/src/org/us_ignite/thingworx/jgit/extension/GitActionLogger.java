package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.entities.utils.UserUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes JGit extension Git action entries to the ScriptLog.
 *
 * <p>Entries use the {@link #ACTION_PREFIX} marker so {@code GetGitLog} can filter them with the
 * ScriptLog {@code QueryLogEntries} service. This is the interim logging surface for the Git Log
 * MashUp; the Git action log stream (GitLab #476) will replace the storage while keeping this call
 * sites and row contract.
 */
public final class GitActionLogger {

    /** Marker written before every Git action entry so log queries can filter entries. */
    public static final String ACTION_PREFIX = "GIT.ACTION";

    private static final String FIELD_SEPARATOR = " | ";

    private static final Logger scriptLogger = LoggerFactory.getLogger("ScriptLog");

    private GitActionLogger() {}

    /**
     * Writes a Git action entry to the ScriptLog.
     *
     * @param action the Git service that was executed (e.g. {@code Commit})
     * @param repoName the repository Thing the action was executed against; may be null
     * @param message human-readable result of the action; may be null
     */
    public static void logAction(String action, String repoName, String message) {
        try {
            scriptLogger.warn(
                    ACTION_PREFIX
                            + FIELD_SEPARATOR
                            + action
                            + FIELD_SEPARATOR
                            + (repoName == null ? "" : repoName)
                            + FIELD_SEPARATOR
                            + (message == null ? "" : message)
                            + FIELD_SEPARATOR
                            + UserUtilities.getCurrentUser());
        } catch (Exception e) {
            // never allow action logging to break the Git operation itself
        }
    }
}
