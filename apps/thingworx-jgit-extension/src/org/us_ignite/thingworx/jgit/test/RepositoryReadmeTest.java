package org.us_ignite.thingworx.jgit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.us_ignite.thingworx.jgit.extension.RepositoryReadme;
import org.us_ignite.thingworx.jgit.extension.RepositoryReadme.Readme;

class RepositoryReadmeTest {
    @TempDir Path dir;

    private void commit(Git git, String path, String content) throws Exception {
        Path file = dir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        git.add().addFilepattern(path).call();
        git.commit().setMessage("add " + path).setSign(false).call();
    }

    @Test
    void emptyRepositoryHasNoReadme() throws Exception {
        try (Git git = Git.init().setDirectory(dir.toFile()).call()) {
            assertTrue(RepositoryReadme.read(git.getRepository(), 1000).isEmpty());
        }
    }

    @Test
    void readsRootReadmeIgnoringCaseAndPrefersMarkdown() throws Exception {
        try (Git git = Git.init().setDirectory(dir.toFile()).call()) {
            commit(git, "docs/README.md", "# nested");
            commit(git, "README.txt", "plain");
            assertEquals(
                    "README.txt", RepositoryReadme.read(git.getRepository(), 1000).get().path());
            commit(git, "Readme.md", "# Demo\n");
            Readme readme = RepositoryReadme.read(git.getRepository(), 1000).get();
            assertEquals("Readme.md", readme.path());
            assertEquals("# Demo\n", readme.content());
            assertFalse(readme.binary());
            assertFalse(readme.truncated());
        }
    }

    @Test
    void uncommittedEditsAreNotShownAndLongFilesAreTruncated() throws Exception {
        try (Git git = Git.init().setDirectory(dir.toFile()).call()) {
            commit(git, "README.md", "0123456789");
            Files.writeString(dir.resolve("README.md"), "changed");
            Readme readme = RepositoryReadme.read(git.getRepository(), 4).get();
            assertEquals("0123", readme.content());
            assertEquals(10, readme.size());
            assertTrue(readme.truncated());
        }
    }
}
