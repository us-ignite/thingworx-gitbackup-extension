package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverwriteWarningTest {
    @Test
    void noChangesNeedNoConfirmation() {
        assertEquals("", OverwriteWarning.forChanges(List.of()));
    }

    @Test
    void namesEachChangedEntity() {
        String warning = OverwriteWarning.forChanges(List.of("Thing Pump"));
        assertTrue(
                warning.startsWith(
                        "1 entity has changed in ThingWorx since the last sync: Thing Pump."));
        assertTrue(warning.contains("Use Sync from ThingWorx first to keep those changes."));
    }

    @Test
    void summarizesLongLists() {
        List<String> changed = new ArrayList<>();
        for (int i = 1; i <= 13; i++) changed.add("Thing T" + i);
        String warning = OverwriteWarning.forChanges(changed);
        assertTrue(warning.startsWith("13 entities have changed"));
        assertTrue(warning.contains("Thing T10 and 3 more."));
        assertTrue(!warning.contains("Thing T11"));
    }
}
