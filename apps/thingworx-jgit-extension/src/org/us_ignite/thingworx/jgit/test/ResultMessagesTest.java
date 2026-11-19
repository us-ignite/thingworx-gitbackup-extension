package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.TestProtocol;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Service messages are built from result fields, not toString() (issues #17 and #20). */
class ResultMessagesTest {
    private static final PersonIdent ME = new PersonIdent("Me", "me@example.com");

    @TempDir Path directory;
    private Git git;
    private Repository remote;
    private TestProtocol<Object> protocol;
    private boolean rejectAll;

    @BeforeEach
    void createRepositories() throws Exception {
        git =
                Git.init()
                        .setInitialBranch("main")
                        .setDirectory(directory.resolve("work").toFile())
                        .call();
        commit("a.txt", "base\n", "base");
        remote =
                Git.init()
                        .setBare(true)
                        .setDirectory(directory.resolve("remote.git").toFile())
                        .call()
                        .getRepository();
        protocol =
                new TestProtocol<>(
                        null,
                        (request, db) -> {
                            ReceivePack pack = new ReceivePack(db);
                            pack.setPreReceiveHook(
                                    (rp, commands) -> {
                                        if (!rejectAll) return;
                                        rp.sendMessage("=========================");
                                        rp.sendMessage("Error: Commit 7f2c0c5 is not signed.");
                                        rp.sendMessage("See https://example.com/signing");
                                        for (ReceiveCommand command : commands)
                                            command.setResult(
                                                    ReceiveCommand.Result.REJECTED_OTHER_REASON,
                                                    "pre-receive hook declined");
                                    });
                            return pack;
                        });
        Transport.register(protocol);
    }

    @AfterEach
    void close() {
        Transport.unregister(protocol);
        remote.close();
        git.close();
    }

    private RevCommit commit(String file, String content, String message) throws Exception {
        Files.writeString(directory.resolve("work").resolve(file), content);
        git.add().addFilepattern(file).call();
        return git.commit()
                .setMessage(message)
                .setAuthor(ME)
                .setCommitter(ME)
                .setSign(false)
                .call();
    }

    private PushResult push(String spec) throws Exception {
        URIish uri = protocol.register(new Object(), remote);
        return git.push()
                .setRemote(uri.toString())
                .setRefSpecs(new RefSpec(spec))
                .call()
                .iterator()
                .next();
    }

    private static RemoteRefUpdate only(PushResult result) {
        return result.getRemoteUpdates().iterator().next();
    }

    @Test
    void mergeMessagesNameTheCommits() throws Exception {
        git.branchCreate().setName("feature").call();
        git.checkout().setName("feature").call();
        RevCommit feature = commit("f.txt", "f\n", "feature");
        git.checkout().setName("main").call();

        MergeResult ff = git.merge().include(feature).call();
        assertEquals(
                "fast-forward to " + feature.abbreviate(7).name(),
                ResultMessages.merge(ff, "main"));
        assertEquals(
                "already up to date",
                ResultMessages.merge(git.merge().include(feature).call(), "main"));

        git.checkout().setName("feature").call();
        RevCommit more = commit("g.txt", "g\n", "more");
        git.checkout().setName("main").call();
        commit("h.txt", "h\n", "main work");
        MergeResult merged = git.merge().include(more).call();
        String message = ResultMessages.merge(merged, "main");
        assertEquals(
                "merged "
                        + more.abbreviate(7).name()
                        + " into main (merge commit "
                        + merged.getNewHead().abbreviate(7).name()
                        + ")",
                message);
        assertFalse(message.contains("0000000"), message);
    }

    @Test
    void pushMessagesDescribeTheUpdate() throws Exception {
        PushResult created = push("refs/heads/main:refs/heads/main");
        ObjectId head = git.getRepository().resolve("HEAD");
        assertEquals(
                "Pushed main to origin (new branch, " + head.abbreviate(7).name() + ")",
                ResultMessages.pushed(created, only(created), "main", "origin"));

        RevCommit next = commit("b.txt", "b\n", "next");
        PushResult forward = push("refs/heads/main:refs/heads/main");
        assertEquals(
                "Pushed main to origin (fast-forward to " + next.abbreviate(7).name() + ")",
                ResultMessages.pushed(forward, only(forward), "main", "origin"));

        PushResult same = push("refs/heads/main:refs/heads/main");
        assertEquals(
                "main is already up to date on origin",
                ResultMessages.pushed(same, only(same), "main", "origin"));
    }

    @Test
    void rejectionIncludesTheRemoteExplanation() throws Exception {
        rejectAll = true;
        PushResult result = push("refs/heads/main:refs/heads/main");
        RemoteRefUpdate update = only(result);
        assertFalse(ResultMessages.accepted(update));
        String message = ResultMessages.pushRejected(result, update, "origin");
        assertTrue(message.startsWith("Push rejected by origin: "), message);
        assertTrue(message.contains("is not signed"), message);
        assertTrue(message.contains("See https://example.com/signing"), message);
        assertFalse(message.contains("====="), message);
        assertFalse(message.toLowerCase().contains("credentials"), message);
    }

    @Test
    void remoteOutputDropsBannersColoursAndPrefixes() {
        assertEquals(
                "Error: Commit 7f2c0c5 is not signed.\nSee docs",
                ResultMessages.remoteOutput(
                        "remote: \u001B[31m=====\u001B[0m\nremote: Error: Commit 7f2c0c5 is"
                                + " not signed.\n\nremote: See docs\n"));
        assertEquals("", ResultMessages.remoteOutput(null));
    }
}
