package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MoveProjectFolderTest {
    @TempDir Path root;

    private static void write(Path path, String contents) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents);
    }

    private Git committed() throws Exception {
        Git git = Git.init().setDirectory(root.toFile()).call();
        write(root.resolve("App/Things/A.xml"), "<a/>");
        write(root.resolve("App/Things/B.xml"), "<b/>");
        write(root.resolve("Other/C.xml"), "<c/>");
        git.add().addFilepattern(".").call();
        git.commit().setMessage("base").setAuthor("Test", "test@example.com").call();
        return git;
    }

    @Test
    void movesTrackedFilesAsRenamesKeepingStagedAndUnstagedChanges() throws Exception {
        try (Git git = committed()) {
            write(root.resolve("App/Things/A.xml"), "<a staged/>");
            git.add().addFilepattern("App/Things/A.xml").call();
            write(root.resolve("App/Things/A.xml"), "<a unstaged/>");
            write(root.resolve("App/Things/New.xml"), "<new/>");

            assertEquals(3, FileRepositoryManager.moveFolder(git, "App", "src/twx/app"));

            assertFalse(Files.exists(root.resolve("App")));
            assertEquals(3, FileRepositoryManager.moveFolder(git, "src/twx/app", "App"));
            // Empty parents of the source are removed as well.
            assertFalse(Files.exists(root.resolve("src")));
            assertEquals(3, FileRepositoryManager.moveFolder(git, "App", "src/twx/app"));
            assertEquals(
                    "<a unstaged/>", Files.readString(root.resolve("src/twx/app/Things/A.xml")));
            assertEquals("<new/>", Files.readString(root.resolve("src/twx/app/Things/New.xml")));
            var status = git.status().call();
            assertEquals(Set.of("src/twx/app/Things/New.xml"), status.getUntracked());
            assertEquals(Set.of("src/twx/app/Things/A.xml"), status.getModified());
            assertEquals(
                    Set.of("src/twx/app/Things/A.xml", "src/twx/app/Things/B.xml"),
                    status.getAdded());
            assertEquals(Set.of("App/Things/A.xml", "App/Things/B.xml"), status.getRemoved());
            // The staged content moved with the index entry.
            var entry = git.getRepository().readDirCache().getEntry("src/twx/app/Things/A.xml");
            assertEquals(
                    "<a staged/>",
                    new String(git.getRepository().open(entry.getObjectId()).getBytes()));
            // Git sees the unchanged file as a rename.
            try (TreeWalk walk = new TreeWalk(git.getRepository())) {
                walk.addTree(
                        git.getRepository()
                                .parseCommit(git.getRepository().resolve("HEAD"))
                                .getTree());
                walk.addTree(new DirCacheIterator(git.getRepository().readDirCache()));
                walk.setRecursive(true);
                walk.setFilter(TreeFilter.ANY_DIFF);
                RenameDetector detector = new RenameDetector(git.getRepository());
                detector.addAll(DiffEntry.scan(walk));
                assertTrue(
                        detector.compute().stream()
                                .anyMatch(
                                        d ->
                                                d.getChangeType() == DiffEntry.ChangeType.RENAME
                                                        && d.getOldPath()
                                                                .equals("App/Things/B.xml")));
            }
            assertTrue(Files.exists(root.resolve("Other/C.xml")));
        }
    }

    @Test
    void refusesExistingDestinationsWithoutChangingAnything() throws Exception {
        try (Git git = committed()) {
            write(root.resolve("Dest/Things/B.xml"), "<existing/>");
            assertThrows(
                    IllegalArgumentException.class,
                    () -> FileRepositoryManager.moveFolder(git, "App", "Dest"));
            assertTrue(Files.exists(root.resolve("App/Things/A.xml")));
            assertFalse(Files.exists(root.resolve("Dest/Things/A.xml")));
            assertFalse(git.status().call().isClean());
            assertTrue(git.status().call().getRemoved().isEmpty());
        }
    }

    @Test
    void refusesNestedFoldersAndTheRoot() throws Exception {
        try (Git git = committed()) {
            for (String[] move :
                    new String[][] {{"App", "App/sub"}, {"App/Things", "App"}, {"App", ""}}) {
                assertThrows(
                        IllegalArgumentException.class,
                        () -> FileRepositoryManager.moveFolder(git, move[0], move[1]),
                        move[0] + " -> " + move[1]);
            }
            assertTrue(git.status().call().isClean());
        }
    }

    @Test
    void refusesSymbolicLinksInTheSourceFolder() throws Exception {
        try (Git git = committed()) {
            Files.createSymbolicLink(root.resolve("App/link"), root.resolve("Other"));
            assertThrows(Exception.class, () -> FileRepositoryManager.moveFolder(git, "App", "X"));
            assertTrue(Files.exists(root.resolve("App/Things/A.xml")));
            assertFalse(Files.exists(root.resolve("X")));
        }
    }

    @Test
    void missingSourceFolderMovesNothing() throws Exception {
        try (Git git = committed()) {
            assertEquals(0, FileRepositoryManager.moveFolder(git, "Missing", "Elsewhere"));
            assertTrue(git.status().call().isClean());
        }
    }
}
