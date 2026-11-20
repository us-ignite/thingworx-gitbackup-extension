package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.us_ignite.thingworx.jgit.test.util.GPGGenerator;

/**
 * Commits JGit creates inside Pull, Merge and Rebase take the identity and signing key of the open
 * {@link CommitSettings} scope (issues #15 and #16).
 */
class CommitSettingsTest {
    private static final PersonIdent OTHER = new PersonIdent("Other", "other@example.com");
    private static final String NAME = "Configured Committer";
    private static final String EMAIL = "committer@example.com";

    @TempDir Path directory;
    private Git git;
    private PastedKeyGpgSigner signer;
    private String fingerprint;

    @BeforeEach
    void createRepository() throws Exception {
        git =
                Git.init()
                        .setInitialBranch("main")
                        .setDirectory(directory.resolve("work").toFile())
                        .call();
        StoredConfig config = git.getRepository().getConfig();
        // As RepositorySecurity.harden leaves it: no inherited identity or signing.
        config.setBoolean("commit", null, "gpgSign", false);
        config.save();
        commit(git, "a.txt", "base\n", "base");
        signer = new PastedKeyGpgSigner(GPGGenerator.generateTestGpgPrivateKey(), "");
        fingerprint = signer.getFingerprint();
    }

    @AfterEach
    void closeRepository() {
        if (git != null) git.close();
    }

    private RevCommit commit(Git repo, String file, String content, String message)
            throws Exception {
        Path path = repo.getRepository().getWorkTree().toPath().resolve(file);
        Files.writeString(path, content);
        repo.add().addFilepattern(file).call();
        return repo.commit()
                .setMessage(message)
                .setAuthor(OTHER)
                .setCommitter(OTHER)
                .setSign(false)
                .call();
    }

    /** Diverges main from a new {@code feature} branch, with non-conflicting edits. */
    private ObjectId diverge() throws Exception {
        git.branchCreate().setName("feature").call();
        git.checkout().setName("feature").call();
        commit(git, "feature.txt", "feature\n", "feature work");
        ObjectId feature = git.getRepository().resolve("feature");
        git.checkout().setName("main").call();
        commit(git, "main.txt", "main\n", "main work");
        return feature;
    }

    private void assertSignedByScope(RevCommit commit) {
        assertNotNull(commit.getRawGpgSignature(), "unsigned: " + commit.getShortMessage());
        CommitSignatures.Description description =
                CommitSignatures.describe(commit.getRawGpgSignature());
        assertEquals("openpgp", description.format());
        assertEquals(fingerprint, description.key());
        assertEquals(NAME, commit.getCommitterIdent().getName());
        assertEquals(EMAIL, commit.getCommitterIdent().getEmailAddress());
    }

    private RevCommit head() throws Exception {
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            return walk.parseCommit(git.getRepository().resolve("HEAD"));
        }
    }

    @Test
    void mergeCommitIsSignedWithConfiguredIdentity() throws Exception {
        ObjectId feature = diverge();
        try (CommitSettings ignored =
                CommitSettings.apply(git.getRepository(), NAME, EMAIL, signer)) {
            MergeResult result = git.merge().include(feature).call();
            assertEquals(MergeResult.MergeStatus.MERGED, result.getMergeStatus());
        }
        RevCommit merge = head();
        assertEquals(2, merge.getParentCount());
        assertSignedByScope(merge);
        assertEquals(NAME, merge.getAuthorIdent().getName());
    }

    @Test
    void pullMergeCommitIsSigned() throws Exception {
        Path upstreamDir = directory.resolve("upstream");
        try (Git upstream =
                Git.cloneRepository()
                        .setURI(directory.resolve("work").toUri().toString())
                        .setDirectory(upstreamDir.toFile())
                        .call()) {
            commit(upstream, "upstream.txt", "upstream\n", "upstream work");
        }
        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", upstreamDir.toUri().toString());
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.setString("branch", "main", "remote", "origin");
        config.setString("branch", "main", "merge", "refs/heads/main");
        config.save();
        commit(git, "local.txt", "local\n", "local work");

        try (CommitSettings ignored =
                CommitSettings.apply(git.getRepository(), NAME, EMAIL, signer)) {
            PullResult result = git.pull().call();
            assertTrue(result.isSuccessful());
        }
        RevCommit merge = head();
        assertEquals(2, merge.getParentCount());
        assertSignedByScope(merge);
    }

    @Test
    void rebasedCommitsAreSignedAndKeepTheirAuthor() throws Exception {
        ObjectId feature = diverge();
        git.checkout().setName("feature").call();
        commit(git, "feature2.txt", "more\n", "more feature work");
        ObjectId main = git.getRepository().resolve("main");
        try (CommitSettings ignored =
                CommitSettings.apply(git.getRepository(), NAME, EMAIL, signer)) {
            RebaseResult result = git.rebase().setUpstream(main).call();
            assertTrue(result.getStatus().isSuccessful(), result.getStatus().toString());
        }
        List<RevCommit> rewritten = new ArrayList<>();
        git.log()
                .addRange(main, git.getRepository().resolve("HEAD"))
                .call()
                .forEach(rewritten::add);
        assertEquals(2, rewritten.size());
        for (RevCommit commit : rewritten) {
            assertSignedByScope(commit);
            assertEquals(OTHER.getName(), commit.getAuthorIdent().getName());
        }
        assertFalse(rewritten.stream().anyMatch(c -> c.getId().equals(feature)));
    }

    @Test
    void continuedRebaseSignsTheRemainingCommits() throws Exception {
        git.branchCreate().setName("feature").call();
        git.checkout().setName("feature").call();
        commit(git, "a.txt", "feature\n", "conflicting work");
        commit(git, "b.txt", "b\n", "later work");
        git.checkout().setName("main").call();
        commit(git, "a.txt", "main\n", "main work");
        ObjectId main = git.getRepository().resolve("main");
        git.checkout().setName("feature").call();

        try (CommitSettings ignored =
                CommitSettings.apply(git.getRepository(), NAME, EMAIL, signer)) {
            assertEquals(
                    ConflictWorkflow.Outcome.CONFLICTS,
                    ConflictWorkflow.run(
                            git,
                            "Rebase",
                            () -> ConflictWorkflow.outcome(git.rebase().setUpstream(main).call())));
        }
        Files.writeString(directory.resolve("work/a.txt"), "resolved\n");
        git.add().addFilepattern("a.txt").call();
        try (CommitSettings ignored =
                CommitSettings.apply(git.getRepository(), NAME, EMAIL, signer)) {
            assertEquals(
                    ConflictWorkflow.Outcome.COMPLETED,
                    ConflictWorkflow.continueOperation(git, null, null));
        }
        List<RevCommit> rewritten = new ArrayList<>();
        git.log()
                .addRange(main, git.getRepository().resolve("HEAD"))
                .call()
                .forEach(rewritten::add);
        assertEquals(2, rewritten.size());
        rewritten.forEach(this::assertSignedByScope);
    }

    @Test
    void closingRestoresConfigurationAndScopesAreThreadLocal() throws Exception {
        ObjectId feature = diverge();
        StoredConfig config = git.getRepository().getConfig();
        String nameBefore = config.getString("user", null, "name");
        AtomicReference<Throwable> otherThread = new AtomicReference<>();
        try (CommitSettings ignored =
                CommitSettings.apply(git.getRepository(), NAME, EMAIL, signer)) {
            // Another thread with no scope must not sign with this thread's key.
            Thread thread =
                    new Thread(
                            () -> {
                                try (Git other =
                                        Git.init()
                                                .setDirectory(directory.resolve("other").toFile())
                                                .call()) {
                                    StoredConfig c = other.getRepository().getConfig();
                                    c.setBoolean("commit", null, "gpgSign", true);
                                    c.setString("gpg", null, "format", "openpgp");
                                    Files.writeString(directory.resolve("other/x.txt"), "x");
                                    other.add().addFilepattern("x.txt").call();
                                    other.commit()
                                            .setMessage("x")
                                            .setAuthor(OTHER)
                                            .setCommitter(OTHER)
                                            .call();
                                    otherThread.set(new AssertionError("signed without a scope"));
                                } catch (Throwable expected) {
                                    // No signer is registered for this thread.
                                }
                            });
            thread.start();
            thread.join();
            git.merge().include(feature).call();
        }
        assertNull(otherThread.get());
        assertFalse(config.getBoolean("commit", null, "gpgSign", true));
        assertEquals(nameBefore, config.getString("user", null, "name"));
        assertNull(config.getString("gpg", null, "format"));
        assertSignedByScope(head());
    }
}
