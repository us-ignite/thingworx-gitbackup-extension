package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RetireGitDirectoryTest {
    @TempDir Path storage;

    private File workTree(String name) throws Exception {
        Path tree = storage.resolve("repository").resolve(name);
        Files.createDirectories(tree);
        return tree.toFile();
    }

    @Test
    void recreatingADeletedRepositoryStartsWithoutTheOldOrigin() throws Exception {
        File tree = workTree("legacy-demo");
        File gitDir = FileRepositoryManager.gitDirectoryFor(tree);
        try (Git git = FileRepositoryManager.openRepository(tree, gitDir, "main")) {
            Remotes.add(git, "origin", "https://example.com/old.git");
            Files.writeString(tree.toPath().resolve("a.txt"), "a");
            git.add().addFilepattern("a.txt").call();
            git.commit().setMessage("old").setAuthor("T", "t@example.com").call();
        }
        assertTrue(FileRepositoryManager.hasPreviousGitDirectory(tree));

        Path retired = FileRepositoryManager.retireGitDirectory(tree);

        assertNotNull(retired);
        assertFalse(gitDir.exists());
        assertEquals(storage.resolve("jgit/deleted"), retired.getParent());
        assertTrue(retired.getFileName().toString().startsWith("legacy-demo-"));
        try (var old = new FileRepositoryBuilder().setGitDir(retired.toFile()).build()) {
            assertTrue(old.getRemoteNames().isEmpty(), "retired copy keeps no remotes");
            assertNotNull(old.resolve("refs/heads/main"), "history is kept for recovery");
        }
        // Recreating the repository: a fresh Git directory, so adding origin works again.
        try (Git git = FileRepositoryManager.openRepository(tree, gitDir, "main")) {
            assertTrue(git.getRepository().getRemoteNames().isEmpty());
            Remotes.add(git, "origin", "https://example.com/new.git");
        }
    }

    @Test
    void aFreshlyInitialisedGitDirectoryIsNotAPreviousRepository() throws Exception {
        File tree = workTree("fresh");
        assertFalse(FileRepositoryManager.hasPreviousGitDirectory(tree));
        try (Git git =
                FileRepositoryManager.openRepository(
                        tree, FileRepositoryManager.gitDirectoryFor(tree), "main")) {
            assertNotNull(git);
        }
        assertFalse(FileRepositoryManager.hasPreviousGitDirectory(tree));
        assertNull(FileRepositoryManager.retireGitDirectory(workTree("missing")));
    }
}
