package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.entities.utils.UserUtilities;
import com.thingworx.logging.LogUtilities;
import org.slf4j.Logger;

/**
 * Writes JGit extension Git action entries to the extension's ApplicationLog logger.
 *
 * <p>Entries use the {@link #ACTION_PREFIX} marker and are read back by {@code GetGitLog} and
 * {@code GetExtensionLog} through {@link ExtensionLog}. The Git action log stream (GitLab #476) may
 * replace the storage while keeping these call sites and row contract.
 */
public final class GitActionLogger {

    /** Marker written before every Git action entry so log queries can filter entries. */
    public static final String ACTION_PREFIX = "GIT.ACTION";

    private static final String FIELD_SEPARATOR = " | ";

    private static final Logger logger =
            LogUtilities.getInstance().getApplicationLogger(GitActionLogger.class);

    private GitActionLogger() {}

    /**
     * Writes a Git action entry to the ApplicationLog.
     *
     * @param action the Git service that was executed (e.g. {@code Commit})
     * @param repoName the repository Thing the action was executed against; may be null
     * @param message human-readable result of the action; may be null
     */
    public static void logAction(String action, String repoName, String message) {
        try {
            logger.warn(
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
