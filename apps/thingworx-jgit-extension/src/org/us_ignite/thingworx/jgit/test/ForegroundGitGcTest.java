package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Guards {@link org.us_ignite.thingworx.jgit.test.util.ForegroundGitGc}: without it, a merge leaves
 * {@code gc.log.lock} behind on a worker thread and {@code @TempDir} cleanup fails at random.
 */
class ForegroundGitGcTest {
    @TempDir Path directory;

    @Test
    void repositoriesRunAutoGcInTheForeground() throws Exception {
        try (Git git =
                Git.init().setInitialBranch("main").setDirectory(directory.toFile()).call()) {
            Config config = git.getRepository().getConfig();
            assertFalse(config.getBoolean("gc", "autoDetach", true));
            assertEquals(0, config.getInt("gc", "auto", -1));
        }
    }

    @Test
    void mergeLeavesNoGcLock() throws Exception {
        try (Git git =
                Git.init().setInitialBranch("main").setDirectory(directory.toFile()).call()) {
            commit(git, "a.txt", "base");
            git.branchCreate().setName("feature").call();
            git.checkout().setName("feature").call();
            commit(git, "b.txt", "feature");
            git.checkout().setName("main").call();
            commit(git, "c.txt", "main");

            MergeResult result = git.merge().include(git.getRepository().resolve("feature")).call();

            assertEquals(MergeResult.MergeStatus.MERGED, result.getMergeStatus());
            assertFalse(Files.exists(directory.resolve(".git/gc.log.lock")));
        }
    }

    private static void commit(Git git, String file, String content) throws Exception {
        Files.writeString(git.getRepository().getWorkTree().toPath().resolve(file), content);
        git.add().addFilepattern(file).call();
        git.commit().setMessage(file).setSign(false).call();
    }
}
