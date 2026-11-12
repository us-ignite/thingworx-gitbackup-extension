package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ForcePullTest {
    @TempDir Path directory;

    @Test
    void fetchesBeforeResettingAndPreservesUntrackedFiles() throws Exception {
        Path remote = directory.resolve("remote.git");
        try (Git bare =
                        Git.init()
                                .setBare(true)
                                .setDirectory(remote.toFile())
                                .setInitialBranch("main")
                                .call();
                Git source =
                        Git.init()
                                .setInitialBranch("main")
                                .setDirectory(directory.resolve("source").toFile())
                                .call()) {
            source.getRepository()
                    .getConfig()
                    .setString("remote", "origin", "url", remote.toUri().toString());
            source.getRepository().getConfig().save();
            Path sourceFile = directory.resolve("source/file.txt");
            Files.writeString(sourceFile, "first");
            source.add().addFilepattern("file.txt").call();
            source.commit().setMessage("first").setAuthor("Test", "test@example.com").call();
            source.push().setRemote("origin").call();
            try (Git local =
                    Git.cloneRepository()
                            .setURI(remote.toUri().toString())
                            .setDirectory(directory.resolve("local").toFile())
                            .call()) {
                Path localFile = directory.resolve("local/file.txt");
                Files.writeString(localFile, "local commit");
                local.add().addFilepattern("file.txt").call();
                local.commit().setMessage("local").setAuthor("Test", "test@example.com").call();
                Files.writeString(localFile, "dirty");
                Files.writeString(directory.resolve("local/untracked.txt"), "keep");
                Files.writeString(sourceFile, "remote update");
                source.add().addFilepattern("file.txt").call();
                var remoteCommit =
                        source.commit()
                                .setMessage("remote")
                                .setAuthor("Test", "test@example.com")
                                .call();
                source.push().setRemote("origin").call();
                ForcePull.resetToOrigin(local, null);
                assertEquals(remoteCommit.getId(), local.getRepository().resolve("HEAD"));
                assertEquals("remote update", Files.readString(localFile));
                assertEquals("keep", Files.readString(directory.resolve("local/untracked.txt")));
            }
        }
    }

    @Test
    void missingRemoteBranchDoesNotResetLocalChanges() throws Exception {
        Path remote = directory.resolve("empty.git");
        try (Git bare = Git.init().setBare(true).setDirectory(remote.toFile()).call();
                Git local =
                        Git.init()
                                .setDirectory(directory.resolve("local").toFile())
                                .setInitialBranch("main")
                                .call()) {
            Path file = directory.resolve("local/file.txt");
            Files.writeString(file, "committed");
            local.add().addFilepattern("file.txt").call();
            var commit =
                    local.commit().setMessage("local").setAuthor("Test", "test@example.com").call();
            Files.writeString(file, "dirty");
            var config = local.getRepository().getConfig();
            config.setString("remote", "origin", "url", remote.toUri().toString());
            config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
            config.save();
            assertThrows(IllegalStateException.class, () -> ForcePull.resetToOrigin(local, null));
            assertEquals(commit.getId(), local.getRepository().resolve("HEAD"));
            assertEquals("dirty", Files.readString(file));
        }
    }
}
