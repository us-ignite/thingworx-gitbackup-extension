package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SshHostKeysTest {
    @TempDir Path tempDir;

    @Test
    void refusesToReplaceAnAlreadyApprovedHostKey() throws Exception {
        try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
            Path knownHosts = GitTransport.knownHostsFile(git.getRepository());
            SshHostKeys.record(
                    git.getRepository(),
                    new SshHostKeys.HostKey(
                            "git.example.com", 22, "SHA256:first", "ssh-ed25519 AAAA first"));
            SshHostKeys.record(
                    git.getRepository(),
                    new SshHostKeys.HostKey(
                            "git.example.com", 22, "SHA256:first", "ssh-ed25519 AAAA first"));
            assertEquals(
                    1,
                    Files.readAllLines(knownHosts, StandardCharsets.UTF_8).size(),
                    "Approving the same key twice must not duplicate the entry");

            IllegalStateException error =
                    assertThrows(
                            IllegalStateException.class,
                            () ->
                                    SshHostKeys.record(
                                            git.getRepository(),
                                            new SshHostKeys.HostKey(
                                                    "git.example.com",
                                                    22,
                                                    "SHA256:second",
                                                    "ssh-ed25519 AAAA second")));
            assertTrue(
                    error.getMessage().contains("Reset the trusted host keys"), error.getMessage());
            assertTrue(
                    Files.readString(knownHosts, StandardCharsets.UTF_8).contains("AAAA first"),
                    "The original host key must be kept");
        }
    }

    @Test
    void recordsNonDefaultPortHostPattern() throws Exception {
        try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
            SshHostKeys.record(
                    git.getRepository(),
                    new SshHostKeys.HostKey(
                            "git.example.com", 2222, "SHA256:port", "ssh-rsa AAAA port"));
            String knownHosts =
                    Files.readString(
                            GitTransport.knownHostsFile(git.getRepository()),
                            StandardCharsets.UTF_8);
            assertTrue(knownHosts.startsWith("git.example.com:2222 ssh-rsa AAAA port"), knownHosts);
        }
    }
}
