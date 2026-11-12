package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TrackedProjectNameTest {
    @Test
    void trimsValidProjectNames() {
        assertEquals(
                "Demo.Project", FileRepositoryManager.validateProjectFolderName("  Demo.Project "));
    }

    @Test
    void rejectsNamesThatAreNotASingleFolder() {
        for (String name : new String[] {null, "", "  ", ".", "..", "a/b", "a\\b", "a\0b"}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> FileRepositoryManager.validateProjectFolderName(name),
                    String.valueOf(name));
        }
    }
}
