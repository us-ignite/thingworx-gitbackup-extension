package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConflictWorkflowTest {
    private static final PersonIdent AUTHOR = new PersonIdent("Test", "test@example.com");

    @TempDir Path directory;
    private Git git;

    @BeforeEach
    void createRepository() throws Exception {
        git = Git.init().setInitialBranch("main").setDirectory(directory.toFile()).call();
        var config = git.getRepository().getConfig();
        config.setString("user", null, "name", "Test");
        config.setString("user", null, "email", "test@example.com");
        config.setBoolean("commit", null, "gpgsign", false);
        config.save();
        write("entity.xml", "one\ntwo\nthree\n");
        write("other.xml", "other\n");
        commit("base");
    }

    @Test
    void mergeConflictIsResolvedAndCommittedAndLocalChangesReturn() throws Exception {
        divergeOnFeature();
        write("other.xml", "local edit\n");

        var outcome =
                ConflictWorkflow.run(
                        git,
                        "Merge",
                        () ->
                                ConflictWorkflow.outcome(
                                        git.merge()
                                                .include(git.getRepository().resolve("feature"))
                                                .call()));

        assertEquals(ConflictWorkflow.Outcome.CONFLICTS, outcome);
        var state = ConflictWorkflow.state(git);
        assertEquals(ConflictWorkflow.Phase.MERGE, state.phase());
        assertEquals(List.of("entity.xml"), state.conflicts());
        assertEquals("other\n", read("other.xml"), "local changes wait until the merge completes");
        assertThrows(
                IllegalStateException.class,
                () -> ConflictWorkflow.requireNotInProgress(git),
                "other operations are refused while a conflict waits");

        var versions = ConflictWorkflow.versions(git, "entity.xml");
        assertTrue(versions.conflicted());
        assertEquals("one\nmain\nthree\n", text(versions.current()));
        assertEquals("one\nfeature\nthree\n", text(versions.incoming()));
        assertEquals("one\ntwo\nthree\n", text(versions.base()));
        assertTrue(text(versions.working()).contains("<<<<<<<"));
        assertThrows(
                IllegalStateException.class,
                () -> ConflictWorkflow.continueOperation(git, "", null),
                "unresolved files block completion");

        ConflictWorkflow.resolve(git, "entity.xml", "content", "one\nmain\nfeature\nthree\n");
        assertEquals(
                ConflictWorkflow.Outcome.COMPLETED,
                ConflictWorkflow.continueOperation(git, "", commit -> commit.setCommitter(AUTHOR)));

        RevCommit head = git.log().setMaxCount(1).call().iterator().next();
        assertEquals(2, head.getParentCount());
        assertEquals("one\nmain\nfeature\nthree\n", read("entity.xml"));
        assertEquals("local edit\n", read("other.xml"));
        assertFalse(ConflictWorkflow.inProgress(git));
        assertFalse(git.stashList().call().iterator().hasNext());
    }

    @Test
    void cancellingMergeRestoresCommitAndLocalChanges() throws Exception {
        ObjectId before = divergeOnFeature();
        write("other.xml", "local edit\n");
        ConflictWorkflow.run(
                git,
                "Merge",
                () ->
                        ConflictWorkflow.outcome(
                                git.merge()
                                        .include(git.getRepository().resolve("feature"))
                                        .call()));

        ConflictWorkflow.abort(git);

        assertEquals(before, git.getRepository().resolve(Constants.HEAD));
        assertEquals(RepositoryState.SAFE, git.getRepository().getRepositoryState());
        assertEquals("one\nmain\nthree\n", read("entity.xml"));
        assertEquals("local edit\n", read("other.xml"));
        assertFalse(ConflictWorkflow.inProgress(git));
        assertFalse(git.stashList().call().iterator().hasNext());
    }

    @Test
    void checkoutCarriesConflictingLocalChangesForResolution() throws Exception {
        git.branchCreate().setName("feature").call();
        git.checkout().setName("feature").call();
        write("entity.xml", "one\nfeature\nthree\n");
        commit("feature");
        git.checkout().setName("main").call();
        write("entity.xml", "one\nlocal\nthree\n");

        var outcome =
                ConflictWorkflow.run(
                        git,
                        "Branch switch",
                        () -> {
                            git.checkout().setName("feature").call();
                            return ConflictWorkflow.Outcome.COMPLETED;
                        });

        assertEquals(ConflictWorkflow.Outcome.CONFLICTS, outcome);
        assertEquals("feature", git.getRepository().getBranch());
        var state = ConflictWorkflow.state(git);
        assertEquals(ConflictWorkflow.Phase.LOCAL_CHANGES, state.phase());
        assertEquals("Your uncommitted changes", state.incomingLabel());
        var versions = ConflictWorkflow.versions(git, "entity.xml");
        assertEquals("one\nfeature\nthree\n", text(versions.current()));
        assertEquals("one\nlocal\nthree\n", text(versions.incoming()));

        ConflictWorkflow.resolve(git, "entity.xml", "incoming", null);
        assertEquals(
                ConflictWorkflow.Outcome.COMPLETED,
                ConflictWorkflow.continueOperation(git, "", null));
        assertEquals("feature", git.getRepository().getBranch());
        assertEquals("one\nlocal\nthree\n", read("entity.xml"));
        assertFalse(ConflictWorkflow.inProgress(git));
        assertFalse(git.stashList().call().iterator().hasNext());
    }

    @Test
    void cancellingCarriedChangesReturnsToOriginalBranch() throws Exception {
        git.branchCreate().setName("feature").call();
        git.checkout().setName("feature").call();
        write("entity.xml", "one\nfeature\nthree\n");
        commit("feature");
        git.checkout().setName("main").call();
        write("entity.xml", "one\nlocal\nthree\n");
        ConflictWorkflow.run(
                git,
                "Branch switch",
                () -> {
                    git.checkout().setName("feature").call();
                    return ConflictWorkflow.Outcome.COMPLETED;
                });

        ConflictWorkflow.abort(git);

        assertEquals("main", git.getRepository().getBranch());
        assertEquals("one\nlocal\nthree\n", read("entity.xml"));
        assertFalse(ConflictWorkflow.inProgress(git));
    }

    @Test
    void cleanLocalChangesAreReappliedWithoutConflict() throws Exception {
        divergeOnFeatureWithoutConflict();
        write("other.xml", "local edit\n");

        var outcome =
                ConflictWorkflow.run(
                        git,
                        "Merge",
                        () ->
                                ConflictWorkflow.outcome(
                                        git.merge()
                                                .include(git.getRepository().resolve("feature"))
                                                .call()));

        assertEquals(ConflictWorkflow.Outcome.COMPLETED, outcome);
        assertEquals("local edit\n", read("other.xml"));
        assertFalse(ConflictWorkflow.inProgress(git));
        assertFalse(git.stashList().call().iterator().hasNext());
    }

    @Test
    void failedOperationRestoresLocalChanges() throws Exception {
        ObjectId before = git.getRepository().resolve(Constants.HEAD);
        write("entity.xml", "dirty\n");

        assertThrows(
                IllegalStateException.class,
                () ->
                        ConflictWorkflow.run(
                                git,
                                "Pull",
                                () -> {
                                    throw new IllegalStateException("network down");
                                }));

        assertEquals(before, git.getRepository().resolve(Constants.HEAD));
        assertEquals("dirty\n", read("entity.xml"));
        assertFalse(ConflictWorkflow.inProgress(git));
    }

    @Test
    void rebaseConflictContinuesToLinearHistory() throws Exception {
        divergeOnFeature();
        git.checkout().setName("feature").call();

        var outcome =
                ConflictWorkflow.run(
                        git,
                        "Rebase",
                        () -> ConflictWorkflow.outcome(git.rebase().setUpstream("main").call()));

        assertEquals(ConflictWorkflow.Outcome.CONFLICTS, outcome);
        var state = ConflictWorkflow.state(git);
        assertEquals(ConflictWorkflow.Phase.REBASE, state.phase());
        assertTrue(state.canSkip());
        ConflictWorkflow.resolve(git, "entity.xml", "content", "one\nboth\nthree\n");

        assertEquals(
                ConflictWorkflow.Outcome.COMPLETED,
                ConflictWorkflow.continueOperation(git, "", null));
        assertEquals("feature", git.getRepository().getBranch());
        assertEquals("one\nboth\nthree\n", read("entity.xml"));
        RevCommit head = git.log().setMaxCount(1).call().iterator().next();
        assertEquals(1, head.getParentCount());
        assertEquals(git.getRepository().resolve("main"), head.getParent(0).getId());
        assertFalse(ConflictWorkflow.inProgress(git));
    }

    @Test
    void modifyDeleteConflictCanBeResolvedByDeleting() throws Exception {
        git.branchCreate().setName("feature").call();
        git.checkout().setName("feature").call();
        git.rm().addFilepattern("entity.xml").call();
        commit("delete");
        git.checkout().setName("main").call();
        write("entity.xml", "changed\n");
        commit("change");

        ConflictWorkflow.run(
                git,
                "Merge",
                () ->
                        ConflictWorkflow.outcome(
                                git.merge()
                                        .include(git.getRepository().resolve("feature"))
                                        .call()));

        var versions = ConflictWorkflow.versions(git, "entity.xml");
        assertTrue(versions.conflicted());
        assertNull(versions.incoming());
        assertArrayEquals("changed\n".getBytes(StandardCharsets.UTF_8), versions.current());

        ConflictWorkflow.resolve(git, "entity.xml", "delete", null);
        ConflictWorkflow.continueOperation(git, "", commit -> commit.setCommitter(AUTHOR));
        assertFalse(Files.exists(directory.resolve("entity.xml")));
        assertFalse(ConflictWorkflow.inProgress(git));
    }

    @Test
    void pathsOutsideTheRepositoryAreRejected() throws Exception {
        divergeOnFeature();
        ConflictWorkflow.run(
                git,
                "Merge",
                () ->
                        ConflictWorkflow.outcome(
                                git.merge()
                                        .include(git.getRepository().resolve("feature"))
                                        .call()));

        assertThrows(
                Exception.class,
                () -> ConflictWorkflow.resolve(git, "../escape.xml", "content", "x"));
        assertThrows(
                Exception.class,
                () -> ConflictWorkflow.resolve(git, ".git/config", "content", "x"));
    }

    /** Creates conflicting edits to entity.xml on main and feature; returns main's tip. */
    private ObjectId divergeOnFeature() throws Exception {
        git.branchCreate().setName("feature").call();
        git.checkout().setName("feature").call();
        write("entity.xml", "one\nfeature\nthree\n");
        commit("feature");
        git.checkout().setName("main").call();
        write("entity.xml", "one\nmain\nthree\n");
        return commit("main").getId();
    }

    private void divergeOnFeatureWithoutConflict() throws Exception {
        git.branchCreate().setName("feature").call();
        git.checkout().setName("feature").call();
        write("entity.xml", "one\ntwo\nthree\nfeature\n");
        commit("feature");
        git.checkout().setName("main").call();
        write("entity.xml", "main\none\ntwo\nthree\n");
        commit("main");
    }

    private RevCommit commit(String message) throws Exception {
        git.add().addFilepattern(".").call();
        return git.commit()
                .setMessage(message)
                .setAuthor(AUTHOR)
                .setCommitter(AUTHOR)
                .setSign(false)
                .call();
    }

    private void write(String name, String content) throws Exception {
        Files.writeString(directory.resolve(name), content);
    }

    private String read(String name) throws Exception {
        return Files.readString(directory.resolve(name));
    }

    private static String text(byte[] content) {
        return new String(content, StandardCharsets.UTF_8);
    }
}
