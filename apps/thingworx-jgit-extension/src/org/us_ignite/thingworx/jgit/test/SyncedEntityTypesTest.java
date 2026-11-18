package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SyncedEntityTypesTest {
    @TempDir Path directory;

    @Test
    void normalizesToSelectableOrderWithoutRepeats() {
        assertEquals("Mashups,Things", SyncedEntityTypes.normalize(" things, Mashups things "));
        assertEquals("", SyncedEntityTypes.normalize(""));
        assertEquals("", SyncedEntityTypes.normalize(null));
        assertEquals("", SyncedEntityTypes.normalize(" , "));
    }

    @Test
    void everySelectableTypeMeansAll() {
        assertEquals(
                "", SyncedEntityTypes.normalize(String.join(",", SyncedEntityTypes.SELECTABLE)));
    }

    @Test
    void rejectsUnknownTypes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SyncedEntityTypes.normalize("Things,Widgets"));
        assertThrows(IllegalArgumentException.class, () -> SyncedEntityTypes.normalize("Projects"));
    }

    @Test
    void syncsSelectedCollectionsAndAlwaysProjects() {
        assertTrue(SyncedEntityTypes.syncs("", "Anything"));
        assertTrue(SyncedEntityTypes.syncs("", null));
        assertTrue(SyncedEntityTypes.syncs("Things", "Things"));
        assertTrue(SyncedEntityTypes.syncs("Things", "Projects"));
        assertFalse(SyncedEntityTypes.syncs("Things", "Mashups"));
        assertFalse(SyncedEntityTypes.syncs("Things", null));
    }

    @Test
    void mapsEntityTypesToTheirExportFolders() {
        assertEquals("Things", SyncedEntityTypes.collectionOf(ThingworxRelationshipTypes.Thing));
        assertEquals(
                "MediaEntities",
                SyncedEntityTypes.collectionOf(ThingworxRelationshipTypes.MediaEntity));
        assertEquals(
                "DataTags",
                SyncedEntityTypes.collectionOf(ThingworxRelationshipTypes.DataTagVocabulary));
        assertEquals("Users", SyncedEntityTypes.collectionOf(ThingworxRelationshipTypes.User));
    }

    @Test
    void describesSelections() {
        assertEquals("All", SyncedEntityTypes.describe(""));
        assertEquals(
                "Thing Templates, Things", SyncedEntityTypes.describe("ThingTemplates,Things"));
    }

    @Test
    void trackedProjectsKeepTheirEntityTypesThroughNormalization() {
        var projects =
                FileRepositoryManager.normalizeTrackedProjects(
                        List.of(new FileRepositoryManager.TrackedProject("A", "", "things")));
        assertEquals("Things", projects.get(0).entityTypes());
        assertEquals("", new FileRepositoryManager.TrackedProject("B", "").entityTypes());
    }

    @Test
    void exportKeepsOnlySyncedCollectionsAndTheProject() throws Exception {
        Path snapshot = directory.resolve("snapshot");
        for (String folder : List.of("Things", "Mashups", "Projects")) {
            Files.createDirectories(snapshot.resolve(folder));
            Files.writeString(snapshot.resolve(folder).resolve("A.xml"), "<Entities/>");
        }
        Files.writeString(snapshot.resolve("loose.xml"), "<Entities/>");
        FileRepositoryManager.removeUnsyncedCollections(
                snapshot, new FileRepositoryManager.TrackedProject("A", "", "Things"));
        assertTrue(Files.exists(snapshot.resolve("Things/A.xml")));
        assertTrue(Files.exists(snapshot.resolve("Projects/A.xml")));
        assertFalse(Files.exists(snapshot.resolve("Mashups")));
        assertFalse(Files.exists(snapshot.resolve("loose.xml")));
    }

    @Test
    void exportKeepsEverythingWhenAllTypesAreSynced() throws Exception {
        Path snapshot = directory.resolve("snapshot");
        Files.createDirectories(snapshot.resolve("Mashups"));
        Files.writeString(snapshot.resolve("loose.xml"), "<Entities/>");
        FileRepositoryManager.removeUnsyncedCollections(
                snapshot, new FileRepositoryManager.TrackedProject("A", ""));
        assertTrue(Files.exists(snapshot.resolve("Mashups")));
        assertTrue(Files.exists(snapshot.resolve("loose.xml")));
    }

    @Test
    void collectionOfPathIsTheFirstSegment() {
        assertEquals("Things", FileRepositoryManager.collectionOfPath("Things/A.xml"));
        assertEquals("Things", FileRepositoryManager.collectionOfPath("Things\\A.xml"));
        assertEquals(null, FileRepositoryManager.collectionOfPath("A.xml"));
    }

    @Test
    void reconcileNeverDeletesFilesOfUnsyncedTypes() throws Exception {
        Path root = directory.resolve("repository");
        String thing = "<Entities><Things><Thing name=\"Gone\"/></Things></Entities>";
        String mashup = "<Entities><Mashups><Mashup name=\"Gone\"/></Mashups></Entities>";
        Files.createDirectories(root.resolve("App/Things"));
        Files.createDirectories(root.resolve("App/Mashups"));
        Files.writeString(root.resolve("App/Things/Gone.xml"), thing);
        Files.writeString(root.resolve("App/Mashups/Gone.xml"), mashup);
        Path snapshot = directory.resolve("snapshot");
        Files.createDirectories(snapshot);
        try (Git git = Git.init().setDirectory(root.toFile()).call()) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("base").setAuthor("Test", "test@example.com").call();
            StatusExportReconciler.reconcileFolder(
                    git,
                    snapshot,
                    "App",
                    "App",
                    (type, name, project) -> false,
                    collection -> collection.equals("Things"));
        }
        assertFalse(Files.exists(root.resolve("App/Things/Gone.xml")));
        assertTrue(Files.exists(root.resolve("App/Mashups/Gone.xml")));
    }
}
