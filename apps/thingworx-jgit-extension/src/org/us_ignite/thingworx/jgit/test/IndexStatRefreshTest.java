package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IndexStatRefreshTest {
    @TempDir Path dir;

    private static long indexModified(Git git, String path) throws Exception {
        return git.getRepository()
                .readDirCache()
                .getEntry(path)
                .getLastModifiedInstant()
                .toEpochMilli();
    }

    private static ObjectId indexId(Git git, String path) throws Exception {
        return git.getRepository().readDirCache().getEntry(path).getObjectId();
    }

    @Test
    void refreshesStatsOfRewrittenFilesWithoutStagingChanges() throws Exception {
        try (Git git = Git.init().setDirectory(dir.toFile()).call()) {
            Path same = dir.resolve("P/Things/Same.xml");
            Path edited = dir.resolve("P/Things/Edited.xml");
            Path staged = dir.resolve("P/Things/Staged.xml");
            Files.createDirectories(same.getParent());
            Files.writeString(same, "<same/>\n");
            Files.writeString(edited, "<edited/>\n");
            Files.writeString(staged, "<staged/>\n");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("init").setSign(false).call();

            // Rewrite with identical content but a new timestamp, as an export would.
            FileTime later = FileTime.fromMillis(System.currentTimeMillis() + 60_000);
            Files.writeString(same, "<same/>\n");
            Files.setLastModifiedTime(same, later);
            Files.writeString(staged, "<staged v2/>\n");
            git.add().addFilepattern("P/Things/Staged.xml").call();
            Files.writeString(edited, "<edited v2/>\n");
            ObjectId editedBefore = indexId(git, "P/Things/Edited.xml");
            ObjectId stagedBefore = indexId(git, "P/Things/Staged.xml");
            Path untracked = dir.resolve("P/Things/New.xml");
            Files.writeString(untracked, "<new/>\n");

            IndexStatRefresh.refresh(git);

            assertEquals(later.toMillis(), indexModified(git, "P/Things/Same.xml"));
            var status = git.status().call();
            assertEquals(Set.of("P/Things/Edited.xml"), status.getModified());
            assertEquals(Set.of("P/Things/Staged.xml"), status.getChanged());
            assertEquals(Set.of("P/Things/New.xml"), status.getUntracked());
            assertTrue(status.getAdded().isEmpty());
            assertEquals(editedBefore, indexId(git, "P/Things/Edited.xml"));
            assertEquals(stagedBefore, indexId(git, "P/Things/Staged.xml"));
        }
    }
}
