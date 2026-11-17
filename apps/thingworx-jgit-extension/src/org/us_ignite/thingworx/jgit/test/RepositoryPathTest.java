package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositoryPathTest {
    @TempDir Path temp;

    @Test
    void newProjectNamesAreSafeFolderNamesAndNotTheExtensionProject() {
        assertEquals("My.App_2-x", FileRepositoryManager.validateNewProjectName("  My.App_2-x "));
        for (String value :
                new String[] {
                    "",
                    "   ",
                    ".hidden",
                    "-x",
                    "a/b",
                    "a\\b",
                    "has space",
                    "..",
                    "GIT",
                    "git",
                    "GIT.Repositories",
                    "Git.Mine"
                }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> FileRepositoryManager.validateNewProjectName(value),
                    value);
        }
    }

    @Test
    void validatesThingworxRepositoryPaths() {
        assertEquals("", FileRepositoryManager.validateRepositoryPath(null));
        assertEquals("", FileRepositoryManager.validateRepositoryPath("/"));
        assertEquals(
                "shared/nested", FileRepositoryManager.validateRepositoryPath("/shared/nested/"));
        for (String value :
                new String[] {
                    "../outside",
                    "a/../../outside",
                    "a/../b",
                    ".",
                    "a/./b",
                    "//server/share",
                    "C:/outside",
                    "C:\\outside",
                    "a\\..\\outside",
                    "a//b",
                    ".git/config",
                    "a/.GIT",
                    "a\0b"
                }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> FileRepositoryManager.validateRepositoryPath(value),
                    value);
        }
    }

    @Test
    void rejectsSymlinkAndFileComponentsIncludingProjectFolder() throws Exception {
        Path root = Files.createDirectory(temp.resolve("root"));
        Path outside = Files.createDirectory(temp.resolve("outside"));
        Files.createDirectory(root.resolve("safe"));
        Files.createSymbolicLink(root.resolve("escape"), outside);
        Files.createSymbolicLink(root.resolve("safe").resolve("Project"), outside);
        Files.writeString(root.resolve("not-a-directory"), "file");
        assertEquals(
                root.resolve("safe").resolve("new"),
                FileRepositoryManager.resolveRepositoryPath(root, "/safe/new"));
        for (String value :
                new String[] {"escape/child", "safe/Project", "not-a-directory/child"}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> FileRepositoryManager.resolveRepositoryPath(root, value),
                    value);
        }
    }
}
