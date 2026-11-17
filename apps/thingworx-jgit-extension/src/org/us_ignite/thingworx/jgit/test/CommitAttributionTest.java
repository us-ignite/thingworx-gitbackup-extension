package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CommitAttributionTest {
    @Test
    void appendsAuthenticatedUserTrailer() {
        assertEquals(
                "Fix the build\n\nThingWorx-User: alice",
                CommitAttribution.withUser("Fix the build", "alice"));
    }

    @Test
    void sanitizesLineBreaksInUserName() {
        assertEquals(
                "Fix the build\n\nThingWorx-User: ali ce",
                CommitAttribution.withUser("Fix the build", "ali\r\nce"));
    }
}
