package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.entities.utils.UserUtilities;

/** Adds the authenticated ThingWorx user to commits made through shared repository Things. */
final class CommitAttribution {
    static final String TRAILER = "ThingWorx-User";

    static String withAuthenticatedUser(String message) {
        String user;
        try {
            user = UserUtilities.getCurrentUser();
        } catch (Exception e) {
            user = null;
        }
        return withUser(message, user);
    }

    static String withUser(String message, String user) {
        if (user == null || user.isBlank()) return message == null ? "" : message;
        String safeUser = user.replaceAll("\\s+", " ").trim();
        String base = message == null ? "" : message;
        if (base.isBlank()) return TRAILER + ": " + safeUser;
        return base + "\n\n" + TRAILER + ": " + safeUser;
    }

    private CommitAttribution() {}
}
