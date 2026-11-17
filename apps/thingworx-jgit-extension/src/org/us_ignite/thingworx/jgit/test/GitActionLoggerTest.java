package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class GitActionLoggerTest {
    @Test
    void untrustedFieldsCannotAddRecordsOrSeparators() {
        String escaped = GitActionLogger.escapeField("repo | forged\nnext\r\t\\u007c\u2028");
        assertEquals("repo \\u007c forged\\u000anext\\u000d\\u0009\\\\u007c\\u2028", escaped);
        assertFalse(escaped.contains(" | "));
        assertFalse(escaped.contains("\n"));
        assertEquals("", GitActionLogger.escapeField(null));
    }

    @Test
    void escapedFieldsRoundTripThroughStructuredParser() {
        String action = "Commit | Push";
        String repo = "repo\nGIT.ACTION | forged";
        String message = "failed: a | b\r\nThingWorx-User: mallory";
        String user = "alice";
        String line =
                GitActionLogger.ACTION_PREFIX
                        + " | "
                        + GitActionLogger.escapeField(action)
                        + " | "
                        + GitActionLogger.escapeField(repo)
                        + " | "
                        + GitActionLogger.escapeField(message)
                        + " | "
                        + GitActionLogger.escapeField(user);
        GitActionLogger.Action parsed = GitActionLogger.parse(line);
        assertEquals(action, parsed.action());
        assertEquals(repo, parsed.repoName());
        assertEquals(message, parsed.message());
        assertEquals(user, parsed.user());
        assertNull(GitActionLogger.parse("GIT.ACTION | only | three | fields"));
        assertNull(GitActionLogger.parse("not an action"));
        assertEquals("a\\b|", GitActionLogger.unescapeField(GitActionLogger.escapeField("a\\b|")));
    }
}
