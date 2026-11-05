package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StatusExportReconcilerTest {
    @TempDir Path directory;

    private static String entity(String name, String contents) {
        return "<Entities><Things><Thing name=\""
                + name
                + "\" description=\""
                + contents
                + "\"/></Things></Entities>";
    }

    private static void write(Path path, String contents) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents);
    }

    @Test
    void refreshesTrackedExportsAndRemovesOnlyProvenStaleEntities() throws Exception {
        Path root = directory.resolve("repository");
        Files.createDirectories(root);
        Path snapshot = directory.resolve("snapshot");
        Path base = root.resolve("source/Example/Things");
        write(base.resolve("Live.xml"), entity("Live", "old"));
        write(base.resolve("Deleted.xml"), entity("Deleted", "old"));
        write(base.resolve("Moved.xml"), entity("Moved", "old"));
        write(base.resolve("NotVisible.xml"), entity("NotVisible", "old"));
        write(base.resolve("Custom.xml"), "<not-an-entity/>");
        write(
                base.resolve("WrongType.xml"),
                "<Entities><Things><ThingShape name=\"WrongType\"/></Things></Entities>");
        write(root.resolve("source/Example/notes.txt"), "keep");
        try (Git git = Git.init().setDirectory(root.toFile()).call()) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("base").setAuthor("Test", "test@example.com").call();
            write(base.resolve("Live.xml"), entity("Live", "staged"));
            git.add().addFilepattern("source/Example/Things/Live.xml").call();
            String stagedBefore =
                    git.getRepository()
                            .readDirCache()
                            .getEntry("source/Example/Things/Live.xml")
                            .getObjectId()
                            .name();
            write(base.resolve("Untracked.xml"), "keep my untracked file");
            write(snapshot.resolve("Things/Live.xml"), entity("Live", "current"));
            write(snapshot.resolve("Things/Untracked.xml"), entity("Untracked", "current"));
            write(snapshot.resolve("Things/New.xml"), entity("New", "current"));

            StatusExportReconciler.reconcile(
                    git,
                    snapshot,
                    "/source",
                    "Example",
                    (type, name, project) -> name.equals("NotVisible"));

            assertEquals(entity("Live", "current"), Files.readString(base.resolve("Live.xml")));
            assertFalse(Files.exists(base.resolve("Deleted.xml")));
            assertFalse(Files.exists(base.resolve("Moved.xml")));
            assertTrue(Files.exists(base.resolve("NotVisible.xml")));
            assertEquals("<not-an-entity/>", Files.readString(base.resolve("Custom.xml")));
            assertTrue(Files.exists(base.resolve("WrongType.xml")));
            assertEquals("keep", Files.readString(root.resolve("source/Example/notes.txt")));
            assertEquals("keep my untracked file", Files.readString(base.resolve("Untracked.xml")));
            assertEquals(entity("New", "current"), Files.readString(base.resolve("New.xml")));
            assertEquals(
                    stagedBefore,
                    git.getRepository()
                            .readDirCache()
                            .getEntry("source/Example/Things/Live.xml")
                            .getObjectId()
                            .name());
            assertTrue(
                    git.status().call().getMissing().contains("source/Example/Things/Deleted.xml"));
            assertTrue(git.status().call().getChanged().contains("source/Example/Things/Live.xml"));
            assertTrue(
                    git.status().call().getModified().contains("source/Example/Things/Live.xml"));
        }
    }

    @Test
    void refusesSymlinkTargetsBeforeChangingFiles() throws Exception {
        Path root = directory.resolve("repo");
        Path outside = directory.resolve("outside");
        Path snapshot = directory.resolve("snapshot2");
        Files.createDirectories(root.resolve("Example"));
        Files.createDirectories(outside);
        Files.createSymbolicLink(root.resolve("Example/Things"), outside);
        write(snapshot.resolve("Things/Thing.xml"), entity("Thing", "new"));
        try (Git git = Git.init().setDirectory(root.toFile()).call()) {
            assertThrows(
                    Exception.class,
                    () ->
                            StatusExportReconciler.reconcile(
                                    git, snapshot, "", "Example", (t, n, p) -> false));
            assertFalse(Files.exists(outside.resolve("Thing.xml")));
        }
    }
}
