package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SquashCommitsTest {
    @TempDir Path directory;

    private static final PersonIdent ME = new PersonIdent("Me", "me@example.com");

    private RevCommit commit(Git git, String file, String content, String message)
            throws Exception {
        Files.writeString(directory.resolve(file), content);
        git.add().addFilepattern(file).call();
        return git.commit()
                .setMessage(message)
                .setAuthor("Author " + message, "a@example.com")
                .setCommitter("Author", "a@example.com")
                .setSign(false)
                .call();
    }

    private static List<RevCommit> log(Git git) throws Exception {
        List<RevCommit> out = new ArrayList<>();
        git.log().call().forEach(out::add);
        return out;
    }

    private static StoredKeySshSigner sshSigner() throws Exception {
        KeyPair pair = KeyUtils.generateKeyPair("ecdsa-sha2-nistp256", 256);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OpenSSHKeyPairResourceWriter.INSTANCE.writePrivateKey(pair, "test", null, out);
        return new StoredKeySshSigner(out.toString(StandardCharsets.UTF_8), "");
    }

    @Test
    void squashesMiddleRunAndSignsEveryRewrittenCommit() throws Exception {
        try (Git git =
                Git.init().setInitialBranch("main").setDirectory(directory.toFile()).call()) {
            RevCommit base = commit(git, "a.txt", "1", "base");
            RevCommit c1 = commit(git, "a.txt", "2", "one");
            RevCommit c2 = commit(git, "b.txt", "x", "two");
            RevCommit c3 = commit(git, "a.txt", "3", "three");
            RevCommit top = commit(git, "c.txt", "y", "top");

            StoredKeySshSigner signer = sshSigner();
            SquashCommits.Result result =
                    SquashCommits.squash(
                            git.getRepository(),
                            List.of(c3.name(), c1.name().substring(0, 10), c2.name()),
                            "",
                            ME,
                            signer);

            assertEquals(3, result.squashed());
            assertEquals(1, result.rewritten());
            assertFalse(result.published());
            List<RevCommit> history = log(git);
            assertEquals(3, history.size());
            RevCommit newTop = history.get(0);
            RevCommit squashed = history.get(1);
            assertEquals(base, history.get(2));
            assertEquals(top.getTree(), newTop.getTree());
            assertEquals("top", newTop.getFullMessage());
            assertEquals(top.getAuthorIdent().getName(), newTop.getAuthorIdent().getName());
            assertEquals(c3.getTree(), squashed.getTree());
            assertEquals("one\n\ntwo\n\nthree", squashed.getFullMessage());
            assertEquals("Author one", squashed.getAuthorIdent().getName());
            assertEquals("Me", squashed.getCommitterIdent().getName());
            assertNotNull(squashed.getRawGpgSignature());
            assertNotNull(newTop.getRawGpgSignature());
            assertEquals(result.newHead(), git.getRepository().resolve("HEAD"));
            assertTrue(git.status().call().isClean());
        }
    }

    @Test
    void squashesUpToHeadWithMessageAndNoSigner() throws Exception {
        try (Git git =
                Git.init().setInitialBranch("main").setDirectory(directory.toFile()).call()) {
            RevCommit first = commit(git, "a.txt", "1", "first");
            RevCommit second = commit(git, "a.txt", "2", "second");
            SquashCommits.Result result =
                    SquashCommits.squash(
                            git.getRepository(),
                            List.of(first.name(), second.name()),
                            "combined",
                            ME,
                            null);
            List<RevCommit> history = log(git);
            assertEquals(1, history.size());
            assertEquals(0, history.get(0).getParentCount());
            assertEquals("combined", history.get(0).getFullMessage());
            assertEquals(second.getTree(), history.get(0).getTree());
            assertNull(history.get(0).getRawGpgSignature());
            assertEquals(0, result.rewritten());
        }
    }

    @Test
    void rejectsGapsSingleCommitsAndDetachedHead() throws Exception {
        try (Git git =
                Git.init().setInitialBranch("main").setDirectory(directory.toFile()).call()) {
            RevCommit c1 = commit(git, "a.txt", "1", "one");
            commit(git, "a.txt", "2", "two");
            RevCommit c3 = commit(git, "a.txt", "3", "three");
            ObjectId head = git.getRepository().resolve("HEAD");

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            SquashCommits.squash(
                                    git.getRepository(),
                                    List.of(c1.name(), c3.name()),
                                    "",
                                    ME,
                                    null));
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            SquashCommits.squash(
                                    git.getRepository(), List.of(c3.name()), "", ME, null));
            assertEquals(head, git.getRepository().resolve("HEAD"));

            git.checkout().setName(c3.name()).call();
            assertThrows(
                    IllegalStateException.class,
                    () ->
                            SquashCommits.squash(
                                    git.getRepository(),
                                    List.of(c1.name(), c3.name()),
                                    "",
                                    ME,
                                    null));
        }
    }

    @Test
    void reportsAlreadyPushedCommits() throws Exception {
        Path remote = directory.resolve("remote.git");
        Path work = directory.resolve("work");
        try (Git bare = Git.init().setBare(true).setDirectory(remote.toFile()).call();
                Git git = Git.init().setInitialBranch("main").setDirectory(work.toFile()).call()) {
            git.getRepository()
                    .getConfig()
                    .setString("remote", "origin", "url", remote.toUri().toString());
            git.getRepository().getConfig().save();
            directory = work;
            RevCommit c1 = commit(git, "a.txt", "1", "one");
            RevCommit c2 = commit(git, "a.txt", "2", "two");
            git.push().setRemote("origin").add("main").call();
            git.fetch()
                    .setRemote("origin")
                    .setRefSpecs("+refs/heads/*:refs/remotes/origin/*")
                    .call();
            SquashCommits.Result result =
                    SquashCommits.squash(
                            git.getRepository(), List.of(c1.name(), c2.name()), "", ME, null);
            assertTrue(result.published());
        }
    }
}
