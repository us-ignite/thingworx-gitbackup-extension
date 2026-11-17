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

    private static final class LoggerHolder {
        static final Logger INSTANCE =
                LogUtilities.getInstance().getApplicationLogger(GitActionLogger.class);
    }

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
            LoggerHolder.INSTANCE.warn(
                    ACTION_PREFIX
                            + FIELD_SEPARATOR
                            + escapeField(action)
                            + FIELD_SEPARATOR
                            + escapeField(repoName)
                            + FIELD_SEPARATOR
                            + escapeField(message)
                            + FIELD_SEPARATOR
                            + escapeField(UserUtilities.getCurrentUser()));
        } catch (Exception e) {
            // never allow action logging to break the Git operation itself
        }
    }

    /** One parsed {@link #ACTION_PREFIX} record. */
    record Action(String action, String repoName, String message, String user) {}

    /**
     * Parses a log line written by {@link #logAction}. Returns {@code null} for any line that does
     * not have exactly four escaped fields, so malformed or forged lines are ignored.
     */
    static Action parse(String content) {
        if (content == null || !content.startsWith(ACTION_PREFIX)) return null;
        String rest = content.substring(ACTION_PREFIX.length());
        if (!rest.startsWith(FIELD_SEPARATOR)) return null;
        rest = rest.substring(FIELD_SEPARATOR.length());
        String[] parts = rest.split(java.util.regex.Pattern.quote(FIELD_SEPARATOR), -1);
        if (parts.length != 4) return null;
        return new Action(
                unescapeField(parts[0]),
                unescapeField(parts[1]),
                unescapeField(parts[2]),
                unescapeField(parts[3]));
    }

    /** Reverses {@link #escapeField}, preserving unknown escape sequences. */
    static String unescapeField(String value) {
        if (value == null) return "";
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                result.append(c);
                continue;
            }
            char next = value.charAt(i + 1);
            if (next == '\\') {
                result.append('\\');
                i++;
            } else if (next == 'u' && i + 5 < value.length()) {
                try {
                    result.append((char) Integer.parseInt(value.substring(i + 2, i + 6), 16));
                    i += 5;
                } catch (NumberFormatException e) {
                    result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /** Prevent untrusted field contents from creating log records or field separators. */
    static String escapeField(String value) {
        if (value == null) return "";
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') escaped.append("\\\\");
            else if (c == '|') escaped.append("\\u007c");
            else if (Character.isISOControl(c) || c == '\u2028' || c == '\u2029')
                escaped.append(String.format("\\u%04x", (int) c));
            else escaped.append(c);
        }
        return escaped.toString();
    }
}
