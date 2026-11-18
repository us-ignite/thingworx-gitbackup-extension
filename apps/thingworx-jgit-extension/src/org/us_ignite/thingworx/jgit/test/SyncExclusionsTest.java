package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SyncExclusionsTest {
    @TempDir Path directory;

    private static final String USER =
            "<Entities><Users><User name=\"alice\" description=\"x\"/></Users></Entities>";
    private static final String THING =
            "<Entities><Things><Thing name=\"T\" description=\"x\"/></Things></Entities>";
    private static final String MIXED =
            "<Entities><Users><User name=\"bob\"/></Users>"
                    + "<Things><Thing name=\"M\"/></Things></Entities>";
    private static final String KEYS =
            "<Entities><ApplicationKeys>"
                    + "<ApplicationKey name=\"Broken\" keyId=\"\"/>"
                    + "<ApplicationKey name=\"Healthy\" keyId=\"\"/>"
                    + "</ApplicationKeys></Entities>";

    private static final SyncExclusions.Rule USERS_ONLY =
            (collection, name) -> SyncExclusions.USERS.equals(collection);

    /** Users always, plus the ApplicationKey named Broken, standing in for an undecodable key. */
    private static final SyncExclusions.Rule IMPORT_WITH_BROKEN_KEY =
            (collection, name) ->
                    SyncExclusions.USERS.equals(collection)
                            || (SyncExclusions.APPLICATION_KEYS.equals(collection)
                                    && name.equals("Broken"));

    private static void write(Path path, String contents) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents);
    }

    private static List<String> names(List<SyncExclusions.Excluded> excluded) {
        return excluded.stream().map(SyncExclusions.Excluded::toString).sorted().toList();
    }

    @Test
    void stagedImportCopyLeavesUsersAndBrokenKeysOut() throws Exception {
        Path source = directory.resolve("source");
        write(source.resolve("Users/alice.xml"), USER);
        write(source.resolve("Things/T.xml"), THING);
        write(source.resolve("all.xml"), MIXED);
        write(source.resolve("ApplicationKeys/keys.xml"), KEYS);
        write(source.resolve("MediaEntities/logo.png"), "png");
        assertEquals(
                List.of("ApplicationKeys/Broken", "Users/alice", "Users/bob"),
                names(SyncExclusions.find(source, IMPORT_WITH_BROKEN_KEY)));

        Path target = directory.resolve("target");
        List<SyncExclusions.Excluded> removed =
                SyncExclusions.copyExcluding(source, target, IMPORT_WITH_BROKEN_KEY);

        assertEquals(List.of("ApplicationKeys/Broken", "Users/alice", "Users/bob"), names(removed));
        assertFalse(Files.exists(target.resolve("Users/alice.xml")));
        assertEquals(THING, Files.readString(target.resolve("Things/T.xml")));
        assertEquals("png", Files.readString(target.resolve("MediaEntities/logo.png")));
        String mixed = Files.readString(target.resolve("all.xml"));
        assertFalse(mixed.contains("<Users"));
        assertTrue(mixed.contains("<Thing name=\"M\"/>"));
        String keys = Files.readString(target.resolve("ApplicationKeys/keys.xml"));
        assertFalse(keys.contains("Broken"));
        assertTrue(keys.contains("name=\"Healthy\""));
        assertTrue(SyncExclusions.find(target, IMPORT_WITH_BROKEN_KEY).isEmpty());
        // The source folder is never changed.
        assertEquals(USER, Files.readString(source.resolve("Users/alice.xml")));
        assertEquals(KEYS, Files.readString(source.resolve("ApplicationKeys/keys.xml")));
    }

    @Test
    void healthyApplicationKeysNeedNoStaging() throws Exception {
        Path source = directory.resolve("source");
        write(source.resolve("ApplicationKeys/keys.xml"), KEYS);
        write(source.resolve("Things/T.xml"), THING);
        assertTrue(SyncExclusions.find(source, USERS_ONLY).isEmpty());
    }

    @Test
    void textWithoutFilteredCollectionsIsUnchanged() throws Exception {
        assertFalse(SyncExclusions.mayContainFiltered(THING));
        assertFalse(SyncExclusions.mayContainFiltered("<Entities><UsersX/></Entities>"));
        assertEquals(
                "<NotEntities><Users/></NotEntities>",
                SyncExclusions.without(
                        "<NotEntities><Users/></NotEntities>", USERS_ONLY, new ArrayList<>()));
    }

    @Test
    void reconcilerKeepsUserFilesOfProjectMembersAndDeletesTheRest() throws Exception {
        Path root = directory.resolve("repository");
        Path users = root.resolve("Example/Users");
        write(users.resolve("alice.xml"), USER);
        try (Git git = Git.init().setDirectory(root.toFile()).call()) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("base").setAuthor("Test", "test@example.com").call();
            Path snapshot = directory.resolve("snapshot");
            write(snapshot.resolve("Things/T.xml"), THING);

            StatusExportReconciler.reconcileFolder(
                    git, snapshot, "Example", "Example", (type, name, project) -> true);
            assertEquals(USER, Files.readString(users.resolve("alice.xml")));

            StatusExportReconciler.reconcileFolder(
                    git, snapshot, "Example", "Example", (type, name, project) -> false);
            assertFalse(Files.exists(users.resolve("alice.xml")));
        }
    }
}
