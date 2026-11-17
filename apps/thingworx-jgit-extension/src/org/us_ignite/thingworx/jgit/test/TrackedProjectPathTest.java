package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.us_ignite.thingworx.jgit.extension.FileRepositoryManager.TrackedProject;

class TrackedProjectPathTest {
    @Test
    void normalizesCustomPathsAndAllowsBlank() {
        assertEquals("", FileRepositoryManager.validateProjectPath(null));
        assertEquals("", FileRepositoryManager.validateProjectPath("  "));
        assertEquals("", FileRepositoryManager.validateProjectPath("/"));
        assertEquals("src/twx", FileRepositoryManager.validateProjectPath(" /src/twx/ "));
    }

    @Test
    void rejectsPathsThatLeaveTheRepositoryOrReachGitMetadata() {
        for (String path :
                new String[] {
                    "..",
                    "a/../b",
                    "./a",
                    "a//b",
                    "a\\b",
                    "C:/x",
                    "//host/x",
                    ".git",
                    "a/.GIT/b",
                    "a\0b"
                }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> FileRepositoryManager.validateProjectPath(path),
                    path);
        }
    }

    @Test
    void defaultFolderIsTheProjectNameAtTheRoot() {
        assertEquals("App", FileRepositoryManager.projectFolder(new TrackedProject("App", "")));
        assertEquals(
                "twx/app",
                FileRepositoryManager.projectFolder(new TrackedProject("App", "/twx/app/")));
    }

    @Test
    void refusesOverlappingProjectFolders() {
        for (List<TrackedProject> projects :
                List.of(
                        List.of(new TrackedProject("A", "x"), new TrackedProject("B", "x")),
                        List.of(new TrackedProject("A", "x"), new TrackedProject("B", "x/y")),
                        // A custom path equal to, or containing, another project's default folder.
                        List.of(new TrackedProject("A", ""), new TrackedProject("B", "A")),
                        List.of(new TrackedProject("A", "a/A"), new TrackedProject("B", "a")))) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> FileRepositoryManager.requireDistinctProjectFolders(projects),
                    projects.toString());
        }
        assertDoesNotThrow(
                () ->
                        FileRepositoryManager.requireDistinctProjectFolders(
                                List.of(
                                        new TrackedProject("A", ""),
                                        new TrackedProject("B", "A2"),
                                        new TrackedProject("C", "x/C"),
                                        new TrackedProject("D", "x/CD"))));
    }

    @Test
    void retiredProjectNameAndRepoPathBecomeOneTrackedProject() {
        assertEquals(
                List.of(new TrackedProject("Example", "examples/export/Example")),
                FileRepositoryManager.convertLegacyProjectSettings(
                        List.of(), " Example ", "/examples/export/"));
        // Without a RepoPath the project stays at the root, where it was.
        assertEquals(
                List.of(new TrackedProject("Example", "")),
                FileRepositoryManager.convertLegacyProjectSettings(List.of(), "Example", ""));
        // Neither set: nothing to track.
        assertEquals(
                List.of(), FileRepositoryManager.convertLegacyProjectSettings(List.of(), "", ""));
    }

    @Test
    void retiredRepoPathMovesIntoExistingRowsWithoutAPath() {
        assertEquals(
                List.of(new TrackedProject("A", "shared/A"), new TrackedProject("B", "custom/b")),
                FileRepositoryManager.convertLegacyProjectSettings(
                        List.of(new TrackedProject("A", ""), new TrackedProject("B", "custom/b")),
                        // The table superseded the single ProjectName, which is dropped.
                        "Stale",
                        "shared"));
    }

    @Test
    void refusesInvalidOrOverlappingRetiredSettings() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FileRepositoryManager.convertLegacyProjectSettings(List.of(), "A", "../x"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        FileRepositoryManager.convertLegacyProjectSettings(
                                List.of(
                                        new TrackedProject("A", ""),
                                        new TrackedProject("B", "s/A")),
                                "",
                                "s"));
    }

    @Test
    void normalizeKeepsTheFirstRowForARepeatedName() {
        List<TrackedProject> projects =
                FileRepositoryManager.normalizeTrackedProjects(
                        List.of(
                                new TrackedProject(" A ", "/one/"),
                                new TrackedProject("A", "two")));
        assertEquals(List.of(new TrackedProject("A", "one")), projects);
    }
}
