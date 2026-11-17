package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Remote resolution, URL rules, migration, and the refusal to run commands from Git config. */
class RemotesTest {
    @TempDir Path directory;

    private Git init(String name) throws GitAPIException {
        return Git.init()
                .setFs(HardenedFs.create())
                .setDirectory(directory.resolve(name).toFile())
                .setInitialBranch("main")
                .call();
    }

    @Test
    void onlyNetworkUrlsWithoutPasswordsAreAccepted() {
        for (String url :
                new String[] {
                    "https://git.example.com/team/repo.git",
                    "http://gitea:3000/team/repo.git",
                    "ssh://git@git.example.com:2222/team/repo.git",
                    "git@git.example.com:team/repo.git"
                }) {
            Remotes.parseUrl(url);
        }
        for (String url :
                new String[] {
                    "",
                    "/srv/git/repo.git",
                    "../other",
                    "file:///srv/git/repo.git",
                    "git://git.example.com/repo.git",
                    "ftp://git.example.com/repo.git",
                    "sftp://git.example.com/repo.git",
                    "amazon-s3://bucket/repo.git",
                    "https://user:token@git.example.com/repo.git"
                }) {
            assertThrows(IllegalArgumentException.class, () -> Remotes.parseUrl(url), url);
        }
    }

    @Test
    void resolvesExplicitThenUpstreamThenOrigin() throws Exception {
        try (Git git = init("resolve")) {
            var repository = git.getRepository();
            assertThrows(
                    IllegalArgumentException.class, () -> Remotes.resolve(repository, "", "main"));
            Remotes.add(git, "origin", "https://git.example.com/a.git");
            Remotes.add(git, "upstream", "https://git.example.com/b.git");
            assertEquals("origin", Remotes.resolve(repository, null, "main"));
            assertEquals("upstream", Remotes.resolve(repository, " upstream ", "main"));
            StoredConfig config = repository.getConfig();
            config.setString("branch", "main", "remote", "upstream");
            config.setString("branch", "main", "merge", "refs/heads/main");
            config.save();
            assertEquals("upstream", Remotes.resolve(repository, "", "main"));
            assertEquals("origin", Remotes.resolve(repository, "origin", "main"));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Remotes.resolve(repository, "missing", "main"));
        }
    }

    @Test
    void removeAlsoDropsTrackingBranchesAndUpstreams() throws Exception {
        try (Git source = init("source");
                Git git = init("remove")) {
            Files.writeString(directory.resolve("source/file.txt"), "one");
            source.add().addFilepattern("file.txt").call();
            source.commit().setMessage("one").setAuthor("Test", "test@example.com").call();
            Remotes.add(git, "origin", "https://git.example.com/a.git");
            // Fetch from a local repository to create a real remote-tracking branch.
            StoredConfig config = git.getRepository().getConfig();
            config.setString(
                    "remote", "origin", "url", directory.resolve("source").toUri().toString());
            config.setString("branch", "main", "remote", "origin");
            config.setString("branch", "main", "merge", "refs/heads/main");
            config.save();
            git.fetch().setRemote("origin").call();
            assertEquals("origin/main", Remotes.trackingStartPoint(git.getRepository(), "main"));

            Remotes.remove(git, "origin");

            assertEquals(Set.of(), Remotes.names(git.getRepository()));
            assertNull(git.getRepository().findRef("refs/remotes/origin/main"));
            assertNull(Remotes.trackingStartPoint(git.getRepository(), "main"));
            assertNull(git.getRepository().getConfig().getString("branch", "main", "remote"));
            assertThrows(IllegalArgumentException.class, () -> Remotes.remove(git, "origin"));
        }
    }

    @Test
    void legacyUrlBecomesOriginOnceAndNeverOverwritesIt() throws Exception {
        try (Git git = init("migrate")) {
            StoredConfig config = git.getRepository().getConfig();
            // Earlier versions wrote an origin without a URL for repositories with no remote.
            config.setString("remote", "origin", "url", "");
            config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
            assertTrue(Remotes.migrateLegacyUrl(config, ""));
            assertFalse(config.getSubsections("remote").contains("origin"));

            assertTrue(Remotes.migrateLegacyUrl(config, "https://git.example.com/a.git"));
            assertEquals(
                    "https://git.example.com/a.git", config.getString("remote", "origin", "url"));
            assertFalse(Remotes.migrateLegacyUrl(config, "https://git.example.com/other.git"));
            assertEquals(
                    "https://git.example.com/a.git", config.getString("remote", "origin", "url"));
        }
    }

    @Test
    void credentialScopeBindsSchemeHostPortAndPath() throws Exception {
        assertEquals(
                "https://git.example.com:443/team/repo.git",
                Remotes.credentialScope(
                        new org.eclipse.jgit.transport.URIish(
                                "https://Git.Example.com/team/repo.git")));
        assertEquals(
                "ssh://git.example.com:22/team/repo.git",
                Remotes.credentialScope(
                        new org.eclipse.jgit.transport.URIish(
                                "git@git.example.com:team/repo.git")));
        String approved =
                "https://git.example.com:443/team/repo.git\n"
                        + "ssh://git.example.com:22/team/repo.git";
        assertTrue(
                Remotes.credentialApproved(
                        approved,
                        new org.eclipse.jgit.transport.URIish(
                                "https://git.example.com/team/repo.git")));
        assertTrue(
                Remotes.credentialApproved(
                        approved,
                        new org.eclipse.jgit.transport.URIish(
                                "https://git.example.com/team/repo.git/info/refs")));
        assertFalse(
                Remotes.credentialApproved(
                        approved,
                        new org.eclipse.jgit.transport.URIish(
                                "https://git.example.com/team/repo-evil.git")));
        assertFalse(
                Remotes.credentialApproved(
                        approved,
                        new org.eclipse.jgit.transport.URIish(
                                "https://other.example.com/team/repo.git")));
        assertFalse(
                Remotes.credentialApproved(
                        "", new org.eclipse.jgit.transport.URIish("https://git.example.com/r")));
    }

    @Test
    void legacyUrlMigrationStripsEmbeddedCredentialsAndRejectsUnsafeUrls() throws Exception {
        try (Git git = init("legacy-sanitize")) {
            StoredConfig config = git.getRepository().getConfig();
            assertTrue(
                    Remotes.migrateLegacyUrl(
                            config, "https://user:token@git.example.com/team/repo.git"));
            String url = config.getString("remote", "origin", "url");
            assertEquals("https://git.example.com/team/repo.git", url);
        }
        try (Git git = init("legacy-unsafe")) {
            StoredConfig config = git.getRepository().getConfig();
            assertFalse(Remotes.migrateLegacyUrl(config, "file:///srv/git/repo.git"));
            assertNull(config.getString("remote", "origin", "url"));
        }
    }

    @Test
    void approvedOriginsCoverFetchAndPushUrlsWithoutSecrets() throws Exception {
        try (Git git = init("origins")) {
            Remotes.add(git, "origin", "https://git.example.com/team/repo.git");
            git.remoteSetUrl()
                    .setRemoteName("origin")
                    .setRemoteUri(
                            new org.eclipse.jgit.transport.URIish(
                                    "ssh://git@git.example.com/team/repo.git"))
                    .setUriType(org.eclipse.jgit.api.RemoteSetUrlCommand.UriType.PUSH)
                    .call();
            String origins = Remotes.approvedOrigins(git.getRepository());
            assertTrue(origins.contains("https://git.example.com:443/team/repo.git"), origins);
            assertTrue(origins.contains("ssh://git.example.com:22/team/repo.git"), origins);
            assertFalse(origins.contains("@"), origins);
        }
    }

    @Test
    void hooksAndFilterCommandsFromGitConfigNeverRun() throws Exception {
        Path proof = directory.resolve("proof");
        Path work = directory.resolve("hardened");
        try (Git git = init("hardened")) {
            Path hook = work.resolve(".git/hooks/post-commit");
            Files.createDirectories(hook.getParent());
            Files.writeString(hook, "#!/bin/sh\ntouch '" + proof + "-hook'\n");
            Files.setPosixFilePermissions(hook, PosixFilePermissions.fromString("rwxr-xr-x"));
            StoredConfig config = git.getRepository().getConfig();
            config.setString("filter", "run", "clean", "touch '" + proof + "-filter'; cat");
            config.save();
            Files.writeString(work.resolve(".gitattributes"), "*.txt filter=run\n");
            Files.writeString(work.resolve("file.txt"), "content");
        }
        // Reopen the way FileRepositoryManager does, so the settings are read from disk.
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        builder.setFS(HardenedFs.create());
        builder.setWorkTree(work.toFile());
        try (Git git = new Git(builder.build())) {
            try {
                git.add().addFilepattern(".").call();
            } catch (Exception refused) {
                // A refused filter command may fail the add; it must not have run.
            }
            git.commit()
                    .setAllowEmpty(true)
                    .setMessage("commit")
                    .setAuthor("Test", "test@example.com")
                    .setCommitter("Test", "test@example.com")
                    .call();
        }
        assertFalse(Files.exists(Path.of(proof + "-hook")), "The post-commit hook ran.");
        assertFalse(Files.exists(Path.of(proof + "-filter")), "The filter command ran.");
    }
}
