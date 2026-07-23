package gb.tests.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import gb.tests.junit.util.TestingCredentials;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class GiteaGitOperationsTest {

    private static final String GIT_THING_NAME = "ITGiteaTestThing";
    private static final String GIT_THING_PATH = "/" + GIT_THING_NAME;
    private static final String TEST_FILE = "hello-gitbackup.txt";
    private TestingCredentials credentials;
    private String giteaRepoUrl;

    private static final String DB_INIT_IMAGE =
            System.getProperty("test.dbInitImage", "devopscadit/postgresql-init-twx:platform9.6.3");
    private static final String PLATFORM_IMAGE =
            System.getProperty("test.platformImage", "devopscadit/platform-postgres:platform9.6.3");

    private GitBackupExtensionTestStack stack;

    private void createBranchOnGiteaViaGiteaAPI(String branchName) throws Exception {
        Thread.sleep(3000);
        JsonObject createBody = new JsonObject();
        createBody.addProperty("BranchName", branchName);
        createBody.addProperty("StartPoint", "main");
        var createReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "CreateBranch", createBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var createRes = stack.httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                createRes.statusCode() == 200 || createRes.statusCode() == 201,
                "CreateBranch failed: " + createRes.statusCode() + " " + createRes.body());

        JsonObject checkoutBody = new JsonObject();
        checkoutBody.addProperty("BranchNameOrCommit", branchName);
        var checkoutReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Checkout", checkoutBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var checkoutRes = stack.httpClient.send(checkoutReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                checkoutRes.statusCode() == 200 || checkoutRes.statusCode() == 201,
                "Checkout to new branch failed: "
                        + checkoutRes.statusCode()
                        + " "
                        + checkoutRes.body());

        JsonObject pushBody = new JsonObject();
        pushBody.addProperty("Message", "Create branch " + branchName + " via test");
        var pushReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Push", pushBody.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var pushRes = stack.httpClient.send(pushReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                pushRes.statusCode() == 200 || pushRes.statusCode() == 201,
                "Push after branch creation failed: "
                        + pushRes.statusCode()
                        + " "
                        + pushRes.body());

        JsonObject checkoutMainBody = new JsonObject();
        checkoutMainBody.addProperty("BranchNameOrCommit", "main");
        var checkoutMainReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Checkout", checkoutMainBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var checkoutMainRes =
                stack.httpClient.send(checkoutMainReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                checkoutMainRes.statusCode() == 200 || checkoutMainRes.statusCode() == 201,
                "Checkout back to main failed: "
                        + checkoutMainRes.statusCode()
                        + " "
                        + checkoutMainRes.body());
    }

    @BeforeAll
    public void beforeAll() throws Exception {
        credentials = new TestingCredentials();
        giteaRepoUrl =
                "http://gitea:3000/" + credentials.giteaUser + "/" + credentials.repoName + ".git";
        stack = new GitBackupExtensionTestStack(DB_INIT_IMAGE, PLATFORM_IMAGE, credentials);
    }

    @AfterAll
    public void afterAll() {
        stack.close();
    }

    private void editFileInRepoViaThingworxAPI(String repoName, String path, String content)
            throws Exception {
        var json = new JsonObject();
        json.addProperty("path", path);
        json.addProperty("content", content);
        var req = stack.thingworx.serviceRequest(repoName, "SaveText", json.toString()).build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                res.statusCode() == 200 || res.statusCode() == 201,
                "SaveText failed: " + res.statusCode() + " " + res.body());
    }

    @Test
    @Order(1)
    void testCreateGitThing() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("RepoName", GIT_THING_NAME);
        body.addProperty("GitRepoURL", giteaRepoUrl);
        body.addProperty("RepoPath", GIT_THING_PATH);
        body.addProperty("FileRepo", "GitRepository");
        body.addProperty("User", credentials.giteaUser);
        body.addProperty("Password", credentials.giteaPass);
        body.addProperty("CommitUser", "Test User");
        body.addProperty("CommitEmail", "test@example.com");
        body.addProperty("InitialBranch", "main");
        body.addProperty("ProxyURL", "");
        body.addProperty("ProxyPort", 0);
        body.addProperty("UseProxy", false);
        body.addProperty("LocalizationTokensPrefix", "");

        var createReq =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "AddNewRepo", body.toString())
                        .build();
        var createRes = stack.httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                createRes.statusCode() == 200 || createRes.statusCode() == 201,
                "AddNewRepo failed: " + createRes.statusCode() + " " + createRes.body());

        Thread.sleep(5000);
        JsonObject configurationRequest = new JsonObject();
        configurationRequest.addProperty("GitThingName", GIT_THING_NAME);
        var configurationRes =
                stack.httpClient.send(
                        stack.thingworx
                                .serviceRequest(
                                        "GIT.Utility.Thing",
                                        "GetConfiguration",
                                        configurationRequest.toString())
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertTrue(
                configurationRes.body().contains(giteaRepoUrl),
                "Git repository URL was not persisted: " + configurationRes.body());
        var verifyReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "GetCurrentBranch", null).build();
        var verifyRes = stack.httpClient.send(verifyReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                verifyRes.statusCode() == 200 || verifyRes.statusCode() == 201,
                "Git thing was not created successfully: "
                        + verifyRes.statusCode()
                        + " "
                        + verifyRes.body());
    }

    @Test
    @Order(3)
    void testPush() throws Exception {
        editFileInRepoViaThingworxAPI(
                "GitRepository",
                GIT_THING_PATH + "/" + TEST_FILE,
                "Hello from ThingWorx GitBackup integration test!");

        JsonObject body = new JsonObject();
        body.addProperty("Message", "Integration test: initial commit");
        var pushReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "Push", body.toString()).build();
        var pushRes = stack.httpClient.send(pushReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pushRes.statusCode(), "Push failed: " + pushRes.body());
        assertNotNull(pushRes.body());
        String bodyStr = pushRes.body();
        assertFalse(bodyStr.contains("Error"), "Push returned error: " + bodyStr);
        assertFalse(bodyStr.contains("Exception"), "Push threw exception: " + bodyStr);
    }

    @Test
    @Order(4)
    void testStatus() throws Exception {
        var statusReq = stack.thingworx.serviceRequest(GIT_THING_NAME, "Status", null).build();
        var statusRes = stack.httpClient.send(statusReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, statusRes.statusCode(), "Status failed: " + statusRes.body());
        assertNotNull(statusRes.body());
        assertTrue(
                statusRes.body().contains("rows"),
                "Status should return infotable JSON: " + statusRes.body());
    }

    @Test
    @Order(2)
    void testPull() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("Force", false);
        var req = stack.thingworx.serviceRequest(GIT_THING_NAME, "Pull", body.toString()).build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Pull failed: " + res.body());
        assertNotNull(res.body());
        assertFalse(res.body().contains("Error"), "Pull returned error: " + res.body());
    }

    @Test
    @Order(5)
    void testCheckoutMain() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("BranchNameOrCommit", "main");
        var req =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "Checkout", body.toString()).build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Checkout to main failed: " + res.body());
    }

    @Test
    @Order(6)
    void testGetCurrentBranch() throws Exception {
        var req = stack.thingworx.serviceRequest(GIT_THING_NAME, "GetCurrentBranch", null).build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "GetCurrentBranch failed: " + res.body());
        assertNotNull(res.body());
        assertTrue(
                res.body().contains("BranchName") || res.body().contains("main"),
                "Response should indicate current branch: " + res.body());
    }

    @Test
    @Order(7)
    void testCheckoutRemoteBranch() throws Exception {
        createBranchOnGiteaViaGiteaAPI("it-test-feature");

        Thread.sleep(2000);

        var pullBody = new JsonObject();
        pullBody.addProperty("Force", false);
        var pullReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "Pull", pullBody.toString()).build();
        var pullRes = stack.httpClient.send(pullReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pullRes.statusCode(), "Pull before checkout failed: " + pullRes.body());

        var checkoutBody = new JsonObject();
        checkoutBody.addProperty("BranchNameOrCommit", "it-test-feature");
        var checkoutReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Checkout", checkoutBody.toString())
                        .build();
        var checkoutRes = stack.httpClient.send(checkoutReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                checkoutRes.statusCode(),
                "Checkout to remote tracking branch failed: " + checkoutRes.body());

        editFileInRepoViaThingworxAPI(
                "GitRepository", GIT_THING_PATH + "/feature-file.txt", "Feature branch content");

        var pushBody = new JsonObject();
        pushBody.addProperty("Message", "Feature branch commit");
        var pushReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "Push", pushBody.toString()).build();
        var pushRes = stack.httpClient.send(pushReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pushRes.statusCode(), "Push on feature branch failed: " + pushRes.body());

        var currentBranchReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "GetCurrentBranch", null).build();
        var currentBranchRes =
                stack.httpClient.send(currentBranchReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                currentBranchRes.body().contains("it-test-feature"),
                "Should be on feature branch: " + currentBranchRes.body());

        var checkoutMainBody = new JsonObject();
        checkoutMainBody.addProperty("BranchNameOrCommit", "main");
        var checkoutMainReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Checkout", checkoutMainBody.toString())
                        .build();
        var checkoutMainRes =
                stack.httpClient.send(checkoutMainReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                checkoutMainRes.statusCode(),
                "Checkout back to main failed: " + checkoutMainRes.body());
    }

    @Test
    @Order(8)
    void testBranchList() throws Exception {
        var req = stack.thingworx.serviceRequest(GIT_THING_NAME, "GetBranchList", null).build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "GetBranchList failed: " + res.body());
        assertNotNull(res.body());
        String body = res.body();
        assertTrue(body.contains("main"), "Should contain main branch: " + body);
        assertTrue(body.contains("it-test-feature"), "Should contain feature branch: " + body);
    }

    @Test
    @Order(9)
    void testDeleteLocalBranch() throws Exception {
        createBranchOnGiteaViaGiteaAPI("it-temp-branch");

        Thread.sleep(2000);

        var pullBody = new JsonObject();
        pullBody.addProperty("Force", false);
        var pullReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "Pull", pullBody.toString()).build();
        var pullRes = stack.httpClient.send(pullReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pullRes.statusCode(), "Pull before checkout failed: " + pullRes.body());

        JsonObject checkoutBody = new JsonObject();
        checkoutBody.addProperty("BranchNameOrCommit", "it-temp-branch");
        var checkoutReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Checkout", checkoutBody.toString())
                        .build();
        var checkoutRes = stack.httpClient.send(checkoutReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                checkoutRes.statusCode(),
                "Checkout to temp branch failed: " + checkoutRes.body());

        JsonObject switchBackBody = new JsonObject();
        switchBackBody.addProperty("BranchNameOrCommit", "main");
        var switchBackReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Checkout", switchBackBody.toString())
                        .build();
        var switchBackRes =
                stack.httpClient.send(switchBackReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                switchBackRes.statusCode(),
                "Switch back to main failed: " + switchBackRes.body());

        JsonObject deleteBody = new JsonObject();
        deleteBody.addProperty("BranchName", "it-temp-branch");
        var deleteReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "DeleteLocalBranch", deleteBody.toString())
                        .build();
        var deleteRes = stack.httpClient.send(deleteReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, deleteRes.statusCode(), "DeleteLocalBranch failed: " + deleteRes.body());

        Thread.sleep(2000);

        var branchListReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "GetBranchList", null).build();
        var branchListRes =
                stack.httpClient.send(branchListReq, HttpResponse.BodyHandlers.ofString());
        assertFalse(
                branchListRes.body().contains("refs/heads/it-temp-branch"),
                "Local branch it-temp-branch should have been deleted: " + branchListRes.body());
    }

    @Test
    @Order(10)
    void testCommitList() throws Exception {
        var req = stack.thingworx.serviceRequest(GIT_THING_NAME, "GetCommitList", null).build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "GetCommitList failed: " + res.body());
        assertNotNull(res.body());
        assertTrue(res.body().contains("rows"), "Should contain rows: " + res.body());
    }

    @Test
    @Order(11)
    void testCommitInfo() throws Exception {
        var commitListReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "GetCommitList", null).build();
        var commitListRes =
                stack.httpClient.send(commitListReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, commitListRes.statusCode());

        var firstCommitId = extractFirstCommitId(commitListRes.body());
        assertNotNull(
                firstCommitId,
                "No commits found - cannot test CommitInfo. Response: " + commitListRes.body());

        JsonObject body = new JsonObject();
        body.addProperty("CommitID", firstCommitId);
        var commitInfoReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "GetCommitInfo", body.toString())
                        .build();
        var commitInfoRes =
                stack.httpClient.send(commitInfoReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, commitInfoRes.statusCode(), "GetCommitInfo failed: " + commitInfoRes.body());
        assertNotNull(commitInfoRes.body());
        assertTrue(
                commitInfoRes.body().contains(firstCommitId)
                        || commitInfoRes.body().contains("CommitID"),
                "Commit info should reference the commit: " + commitInfoRes.body());
    }

    @Test
    @Order(12)
    void testDiffPerFileBetweenCommits() throws Exception {
        var commitListReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "GetCommitList", null).build();
        var commitListRes =
                stack.httpClient.send(commitListReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, commitListRes.statusCode());

        var firstCommitId = extractFirstCommitId(commitListRes.body());
        assertNotNull(
                firstCommitId,
                "No commits found - cannot test diff. Response: " + commitListRes.body());

        JsonObject body = new JsonObject();
        body.addProperty("File", TEST_FILE);
        body.addProperty("FromCommitID", firstCommitId);
        var diffReq =
                stack.thingworx
                        .serviceRequest(
                                GIT_THING_NAME, "GetDiffPerFileBetweenCommits", body.toString())
                        .build();
        var diffRes = stack.httpClient.send(diffReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                diffRes.statusCode(),
                "GetDiffPerFileBetweenCommits failed: " + diffRes.body());
        assertNotNull(diffRes.body());
    }

    @Test
    @Order(13)
    void testDiffPerFile() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("File", TEST_FILE);
        var diffReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "GetDiffPerFile", body.toString())
                        .build();
        var diffRes = stack.httpClient.send(diffReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, diffRes.statusCode(), "GetDiffPerFile failed: " + diffRes.body());
        assertNotNull(diffRes.body());
    }

    @Test
    @Order(14)
    void testForcePull() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("Force", true);
        var pullReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "Pull", body.toString()).build();
        var pullRes = stack.httpClient.send(pullReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pullRes.statusCode(), "Force pull failed: " + pullRes.body());
        assertFalse(
                pullRes.body().contains("Error"), "Force pull returned error: " + pullRes.body());
    }

    //     @Test
    //     @Order(15)
    //     void testVerifyGpgKey() throws Exception {
    //         String testKey = GPGGenerator.generateTestGpgPrivateKey();
    //         JsonObject setKeyBody = new JsonObject();
    //         setKeyBody.addProperty("GitThing", GIT_THING_NAME);
    //         setKeyBody.addProperty("GpgPrivateKey", testKey);
    //         setKeyBody.addProperty("SignCommits", false);
    //         setKeyBody.addProperty("GpgKeyFingerprint", "");
    //         var setKeyReq = stack.thingworx.serviceRequest("GIT.Utility.Thing", "SetGpgKey",
    // setKeyBody.toString()).build();
    //         var setKeyRes = stack.httpClient.send(setKeyReq,
    // HttpResponse.BodyHandlers.ofString());
    //         assertTrue(setKeyRes.statusCode() == 200 || setKeyRes.statusCode() == 201,
    //                 "SetGpgKey failed: " + setKeyRes.statusCode() + " " + setKeyRes.body());
    //
    //         Thread.sleep(2000);
    //
    //         JsonObject verifyBody = new JsonObject();
    //         verifyBody.addProperty("GpgPrivateKey", "");
    //         verifyBody.addProperty("GpgKeyPassphrase", "");
    //         var verifyReq = stack.thingworx.serviceRequest(GIT_THING_NAME, "VerifyGpgKey",
    // verifyBody.toString()).build();
    //         var verifyRes = stack.httpClient.send(verifyReq,
    // HttpResponse.BodyHandlers.ofString());
    //         assertEquals(200, verifyRes.statusCode(), "VerifyGpgKey failed: " +
    // verifyRes.body());
    //         assertTrue(verifyRes.body().contains("GpgKeyFingerprint"),
    //                 "VerifyGpgKey should return fingerprint: " + verifyRes.body());
    //     }
    //
    //     @Test
    //     @Order(16)
    //     void testSignedPush() throws Exception {
    //         editFileInRepoViaThingworxAPI("GitRepository", GIT_THING_PATH + "/signed-test.txt",
    //                 "Signed commit test content");
    //
    //         var testKey = GPGGenerator.generateTestGpgPrivateKey();
    //
    //         var setKeyBody = new JsonObject();
    //         setKeyBody.addProperty("GitThing", GIT_THING_NAME);
    //         setKeyBody.addProperty("GpgPrivateKey", testKey);
    //         setKeyBody.addProperty("SignCommits", true);
    //         setKeyBody.addProperty("GpgKeyFingerprint", "");
    //         var setKeyReq = stack.thingworx.serviceRequest("GIT.Utility.Thing", "SetGpgKey",
    // setKeyBody.toString()).build();
    //         var setKeyRes = stack.httpClient.send(setKeyReq,
    // HttpResponse.BodyHandlers.ofString());
    //         assertTrue(setKeyRes.statusCode() == 200 || setKeyRes.statusCode() == 201,
    //                 "SetGpgKey failed: " + setKeyRes.statusCode() + " " + setKeyRes.body());
    //
    //         Thread.sleep(2000);
    //
    //         var pushBody = new JsonObject();
    //         pushBody.addProperty("Message", "Integration test: signed commit");
    //         var pushReq = stack.thingworx.serviceRequest(GIT_THING_NAME, "Push",
    // pushBody.toString()).build();
    //         var pushRes = stack.httpClient.send(pushReq, HttpResponse.BodyHandlers.ofString());
    //         assertEquals(200, pushRes.statusCode(), "Signed Push failed: " + pushRes.body());
    //         var body = pushRes.body();
    //         assertFalse(body.contains("Error"), "Signed Push returned error: " + body);
    //         assertFalse(body.contains("Exception"), "Signed Push threw exception: " + body);
    //         var commitListReq = stack.thingworx.serviceRequest(GIT_THING_NAME, "GetCommitList",
    // null).build();
    //         var commitListRes = stack.httpClient.send(commitListReq,
    // HttpResponse.BodyHandlers.ofString());
    //         assertTrue(commitListRes.body().contains("rows"),
    //                 "Commit list should have entries: " + commitListRes.body());
    //     }

    @Test
    @Order(18)
    void testCreateTag() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("TagName", "test-v1.0");
        body.addProperty("Message", "Test release v1.0");
        var req =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "CreateTag", body.toString())
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "CreateTag failed: " + res.body());
        assertTrue(
                res.body().contains("created"),
                "CreateTag should confirm creation: " + res.body());
    }

    @Test
    @Order(19)
    void testGetTagList() throws Exception {
        var req = stack.thingworx.serviceRequest(GIT_THING_NAME, "GetTagList", null).build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "GetTagList failed: " + res.body());
        assertTrue(
                res.body().contains("test-v1.0"),
                "Tag list should contain test-v1.0: " + res.body());
    }

    @Test
    @Order(20)
    void testDeleteTag() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("TagName", "test-v1.0");
        var req =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "DeleteTag", body.toString())
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "DeleteTag failed: " + res.body());

        var verifyReq = stack.thingworx.serviceRequest(GIT_THING_NAME, "GetTagList", null).build();
        var verifyRes = stack.httpClient.send(verifyReq, HttpResponse.BodyHandlers.ofString());
        assertFalse(
                verifyRes.body().contains("test-v1.0"),
                "Tag list should not contain test-v1.0 after deletion: " + verifyRes.body());
    }

    @Test
    @Order(21)
    void testMerge() throws Exception {
        createBranchOnGiteaViaGiteaAPI("test-merge-branch");

        Thread.sleep(2000);

        JsonObject checkoutBody = new JsonObject();
        checkoutBody.addProperty("BranchNameOrCommit", "test-merge-branch");
        var checkoutReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Checkout", checkoutBody.toString())
                        .build();
        var checkoutRes = stack.httpClient.send(checkoutReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                checkoutRes.statusCode(),
                "Checkout to merge branch failed: " + checkoutRes.body());

        editFileInRepoViaThingworxAPI(
                "GitRepository", GIT_THING_PATH + "/merge-test.txt", "Merge branch content");

        JsonObject pushBody = new JsonObject();
        pushBody.addProperty("Message", "Commit on merge branch");
        var pushReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "Push", pushBody.toString()).build();
        var pushRes = stack.httpClient.send(pushReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pushRes.statusCode(), "Push on merge branch failed: " + pushRes.body());

        JsonObject checkoutMainBody = new JsonObject();
        checkoutMainBody.addProperty("BranchNameOrCommit", "main");
        var checkoutMainReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Checkout", checkoutMainBody.toString())
                        .build();
        var checkoutMainRes =
                stack.httpClient.send(checkoutMainReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                checkoutMainRes.statusCode(),
                "Checkout to main failed: " + checkoutMainRes.body());

        JsonObject mergeBody = new JsonObject();
        mergeBody.addProperty("BranchName", "test-merge-branch");
        var mergeReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Merge", mergeBody.toString())
                        .build();
        var mergeRes = stack.httpClient.send(mergeReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, mergeRes.statusCode(), "Merge failed: " + mergeRes.body());
        String body = mergeRes.body();
        assertFalse(body.contains("Error"), "Merge returned error: " + body);
        assertFalse(body.contains("Exception"), "Merge threw exception: " + body);
    }

    @Test
    @Order(22)
    void testRebase() throws Exception {
        createBranchOnGiteaViaGiteaAPI("test-rebase-branch");

        Thread.sleep(2000);

        editFileInRepoViaThingworxAPI(
                "GitRepository", GIT_THING_PATH + "/rebase-main.txt", "Main branch advancement");
        JsonObject pushBody = new JsonObject();
        pushBody.addProperty("Message", "Advance main for rebase test");
        var pushReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "Push", pushBody.toString()).build();
        var pushRes = stack.httpClient.send(pushReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pushRes.statusCode(), "Push on main failed: " + pushRes.body());

        JsonObject checkoutBody = new JsonObject();
        checkoutBody.addProperty("BranchNameOrCommit", "test-rebase-branch");
        var checkoutReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Checkout", checkoutBody.toString())
                        .build();
        var checkoutRes = stack.httpClient.send(checkoutReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                checkoutRes.statusCode(),
                "Checkout to rebase branch failed: " + checkoutRes.body());

        JsonObject rebaseBody = new JsonObject();
        rebaseBody.addProperty("UpstreamBranch", "main");
        var rebaseReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Rebase", rebaseBody.toString())
                        .build();
        var rebaseRes = stack.httpClient.send(rebaseReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, rebaseRes.statusCode(), "Rebase failed: " + rebaseRes.body());
        String body = rebaseRes.body();
        assertFalse(body.contains("Error"), "Rebase returned error: " + body);
        assertFalse(body.contains("Exception"), "Rebase threw exception: " + body);
    }

    @Test
    @Order(17)
    void testDeleteLocalRepoContent() throws Exception {
        var statusReq = stack.thingworx.serviceRequest(GIT_THING_NAME, "Status", null).build();
        var statusRes = stack.httpClient.send(statusReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, statusRes.statusCode(), "DeleteLocalRepoContent failed: " + statusRes.body());
        var pullReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "Pull", "{\"Force\":false}").build();
        var pullRes = stack.httpClient.send(pullReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pullRes.statusCode(), "Pull after delete failed: " + pullRes.body());
        assertFalse(
                pullRes.body().contains("Error"),
                "Pull after content deletion returned error: " + pullRes.body());
    }

    private String extractFirstCommitId(String json) {
        int rowsIdx = json.indexOf("\"rows\"");
        if (rowsIdx == -1) return null;
        int commitIdIdx = json.indexOf("\"CommitID\"", rowsIdx);
        if (commitIdIdx == -1) return null;
        int colonIdx = json.indexOf(":", commitIdIdx);
        if (colonIdx == -1) return null;
        int quote1 = json.indexOf("\"", colonIdx);
        if (quote1 == -1) return null;
        int quote2 = json.indexOf("\"", quote1 + 1);
        if (quote2 == -1) return null;
        return json.substring(quote1 + 1, quote2);
    }
}
