package org.us_ignite.thingworx.jgit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.us_ignite.thingworx.jgit.tests.util.ServiceResultAssertions.assertSuccess;
import static org.us_ignite.thingworx.jgit.tests.util.ServiceResultAssertions.responseRows;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import org.us_ignite.thingworx.jgit.tests.containers.JGitExtensionTestStack;
import org.us_ignite.thingworx.jgit.tests.util.GPGGenerator;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class GiteaGitOperationsTest {

    private static final String GIT_THING_NAME = "ITGiteaTestThing";
    private static final String GIT_THING_PATH = "/" + GIT_THING_NAME;
    private static final String TEST_FILE = "hello-jgit.txt";
    private TestingCredentials credentials;
    private String giteaRepoUrl;

    private static final String DB_INIT_IMAGE =
            System.getProperty("test.dbInitImage", "devopscadit/postgresql-init-twx:platform9.6.3");
    private static final String PLATFORM_IMAGE =
            System.getProperty("test.platformImage", "devopscadit/platform-postgres:platform9.6.3");

    private JGitExtensionTestStack stack;

    private void createBranchOnGiteaViaGiteaAPI(String branchName) throws Exception {
        Thread.sleep(3000);
        JsonObject createBody = new JsonObject();
        createBody.addProperty("BranchName", branchName);
        createBody.addProperty("StartPoint", "main");
        var createReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "BranchCreate", createBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var createRes = stack.httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                createRes.statusCode() == 200 || createRes.statusCode() == 201,
                "CreateBranch failed: " + createRes.statusCode() + " " + createRes.body());

        JsonObject checkoutBody = new JsonObject();
        checkoutBody.addProperty("BranchName", branchName);
        var checkoutReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "BranchSwitch", checkoutBody.toString())
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
        pushBody.addProperty("BranchName", branchName);
        pushBody.addProperty("RemoteBranchName", branchName);
        pushBody.addProperty("SetUpstream", true);
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
        checkoutMainBody.addProperty("BranchName", "main");
        var checkoutMainReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "BranchSwitch", checkoutMainBody.toString())
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
        stack = new JGitExtensionTestStack(DB_INIT_IMAGE, PLATFORM_IMAGE, credentials);
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

    private String giteaExternalBaseUrl() {
        return "http://" + stack.gitea.getHost() + ":" + stack.gitea.getMappedPort(3000);
    }

    private void registerGpgKeyWithGitea(String privateKey) throws Exception {
        JsonObject keyBody = new JsonObject();
        keyBody.addProperty("armored_public_key", GPGGenerator.publicKeyFromPrivateKey(privateKey));
        var request =
                java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(giteaExternalBaseUrl() + "/api/v1/user/gpg_keys"))
                        .header("Content-Type", "application/json")
                        .header(
                                "Authorization",
                                "Basic "
                                        + java.util.Base64.getEncoder()
                                                .encodeToString(
                                                        (credentials.giteaUser
                                                                        + ":"
                                                                        + credentials.giteaPass)
                                                                .getBytes()))
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(keyBody.toString()))
                        .build();
        var response = stack.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                201,
                response.statusCode(),
                "Registering GPG key with Gitea failed: "
                        + response.statusCode()
                        + " "
                        + response.body());
    }

    @Test
    @Order(1)
    void testCreateGitThing() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("RepoName", GIT_THING_NAME);
        body.addProperty("GitRepoURL", giteaRepoUrl);
        body.addProperty("RepoPathName", GIT_THING_PATH);
        body.addProperty("BranchName", "main");
        body.addProperty("ProjectName", "GiteaGitOperationsProject");
        body.addProperty("GitCommitterUser", credentials.giteaUser);
        body.addProperty("GitCommitterPassword", credentials.giteaPass);
        body.addProperty("GitCommitterEmail", "test@example.com");
        body.addProperty("GitCommitterFullName", "Test User");
        var createRes =
                stack.httpClient.send(
                        stack.thingworx
                                .serviceRequest(
                                        "GIT.Utility.Thing", "RepositoryCreate", body.toString())
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertTrue(
                createRes.statusCode() == 200 || createRes.statusCode() == 201,
                "Repository was not created/configured: " + createRes.body());
        Thread.sleep(5000);
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
                GIT_THING_NAME,
                GIT_THING_PATH + "/" + TEST_FILE,
                "Hello from ThingWorx JGit extension integration test!");

        JsonObject body = new JsonObject();
        body.addProperty("Message", "Integration test: initial commit");
        var commitReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "Commit", body.toString()).build();
        var commitRes = stack.httpClient.send(commitReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, commitRes.statusCode(), "Commit failed: " + commitRes.body());
        var pushReq = stack.thingworx.serviceRequest(GIT_THING_NAME, "Push", null).build();
        var pushRes = stack.httpClient.send(pushReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pushRes.statusCode(), "Push failed: " + pushRes.body());
        assertNotNull(pushRes.body());
        String bodyStr = pushRes.body();
        assertSuccess(bodyStr);
    }

    @Test
    @Order(3)
    void testStageCommitAndPushRootReadme() throws Exception {
        String readmeContent = "README staged through the explicit Git index API.";
        editFileInRepoViaThingworxAPI(
                GIT_THING_NAME, "/README.md", readmeContent);

        JsonObject addBody = new JsonObject();
        addBody.addProperty("File", "README.md");
        addBody.addProperty("All", false);
        var addReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Add", addBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var addRes = stack.httpClient.send(addReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, addRes.statusCode(), "Add README.md failed: " + addRes.body());
        assertFalse(addRes.body().contains("Error"), "Add returned error: " + addRes.body());

        var statusReq = stack.thingworx.serviceRequest(GIT_THING_NAME, "Status", null).build();
        var statusRes = stack.httpClient.send(statusReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, statusRes.statusCode(), "Status after Add failed: " + statusRes.body());
        assertTrue(
                statusRes.body().contains("README.md") && statusRes.body().contains("Staged"),
                "README.md should be reported after staging: " + statusRes.body());

        JsonObject commitBody = new JsonObject();
        commitBody.addProperty("Message", "Integration test: stage root README");
        var commitReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Commit", commitBody.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var commitRes = stack.httpClient.send(commitReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, commitRes.statusCode(), "README commit failed: " + commitRes.body());
        assertSuccess(commitRes.body());

        var pushReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Push", null)
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var pushRes = stack.httpClient.send(pushReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pushRes.statusCode(), "README push failed: " + pushRes.body());
        assertSuccess(pushRes.body());

        var remoteReadmeReq =
                java.net.http.HttpRequest.newBuilder()
                        .uri(
                                java.net.URI.create(
                                        giteaExternalBaseUrl()
                                                + "/api/v1/repos/"
                                                + credentials.giteaUser
                                                + "/"
                                                + credentials.repoName
                                                + "/contents/README.md?ref=main"))
                        .header(
                                "Authorization",
                                "Basic "
                                        + java.util.Base64.getEncoder()
                                                .encodeToString(
                                                        (credentials.giteaUser
                                                                        + ":"
                                                                        + credentials.giteaPass)
                                                                .getBytes()))
                        .GET()
                        .build();
        var remoteReadmeRes =
                stack.httpClient.send(remoteReadmeReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                remoteReadmeRes.statusCode(),
                "Gitea README.md lookup failed: " + remoteReadmeRes.body());
        assertEquals(
                "README.md",
                JsonParser.parseString(remoteReadmeRes.body())
                        .getAsJsonObject()
                        .get("name")
                        .getAsString(),
                "Pushed README.md was not found in the remote repository: "
                        + remoteReadmeRes.body());
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
        assertSuccess(res.body());
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
                GIT_THING_NAME, GIT_THING_PATH + "/feature-file.txt", "Feature branch content");

        var commitBody = new JsonObject();
        commitBody.addProperty("Message", "Feature branch commit");
        var commitReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Commit", commitBody.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var commitRes = stack.httpClient.send(commitReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                commitRes.statusCode(),
                "Commit on feature branch failed: " + commitRes.body());
        var pushReq = stack.thingworx.serviceRequest(GIT_THING_NAME, "Push", null).build();
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
        checkoutBody.addProperty("BranchName", "it-temp-branch");
        var checkoutReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "BranchSwitch", checkoutBody.toString())
                        .build();
        var checkoutRes = stack.httpClient.send(checkoutReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                checkoutRes.statusCode(),
                "Checkout to temp branch failed: " + checkoutRes.body());

        JsonObject switchBackBody = new JsonObject();
        switchBackBody.addProperty("BranchName", "main");
        var switchBackReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "BranchSwitch", switchBackBody.toString())
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
                        .serviceRequest(GIT_THING_NAME, "BranchDelete", deleteBody.toString())
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
        assertSuccess(pullRes.body());
    }

    @Test
    @Order(16)
    void testSignedPush() throws Exception {
        String testKey = GPGGenerator.generateTestGpgPrivateKey();
        registerGpgKeyWithGitea(testKey);

        JsonObject setKeyBody = new JsonObject();
        setKeyBody.addProperty("GpgPrivateKey", testKey);
        setKeyBody.addProperty("GpgKeyPassphrase", "");
        setKeyBody.addProperty("GpgKeyFingerprint", "");
        var setKeyReq =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "GpgKeyCreate", setKeyBody.toString())
                        .build();
        var setKeyRes = stack.httpClient.send(setKeyReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                setKeyRes.statusCode() == 200 || setKeyRes.statusCode() == 201,
                "SetGpgKey failed: " + setKeyRes.statusCode() + " " + setKeyRes.body());

        JsonObject verifyKeyBody = new JsonObject();
        verifyKeyBody.addProperty("GpgPrivateKey", "");
        verifyKeyBody.addProperty("GpgKeyPassphrase", "");
        var verifyKeyReq =
                stack.thingworx
                        .serviceRequest(
                                "GIT.Utility.Thing", "VerifyGpgKey", verifyKeyBody.toString())
                        .build();
        var verifyKeyRes =
                stack.httpClient.send(verifyKeyReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, verifyKeyRes.statusCode(), "VerifyGpgKey failed: " + verifyKeyRes.body());
        assertTrue(
                verifyKeyRes
                        .body()
                        .matches("(?s).*\\\"GpgKeyFingerprint\\\"\\s*:\\s*\\\"[0-9a-fA-F]+\\\".*"),
                "Stored GPG key fingerprint could not be located: " + verifyKeyRes.body());
        String signingFingerprint =
                responseRows(verifyKeyRes.body())
                        .get(0)
                        .getAsJsonObject()
                        .get("GpgKeyFingerprint")
                        .getAsString();
        JsonObject signingBody = new JsonObject();
        signingBody.addProperty("GpgKeyFingerprint", signingFingerprint);
        var signingReq =
                stack.thingworx
                        .serviceRequest(
                                GIT_THING_NAME, "SetGPGKeyForSigning", signingBody.toString())
                        .build();
        var signingRes = stack.httpClient.send(signingReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, signingRes.statusCode(), "SetGPGKeyForSigning failed: " + signingRes.body());

        editFileInRepoViaThingworxAPI(
                GIT_THING_NAME, GIT_THING_PATH + "/signed-test.txt", "Signed commit test content");

        JsonObject commitBody = new JsonObject();
        commitBody.addProperty("Message", "Integration test: signed commit");
        var commitReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Commit", commitBody.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var commitRes = stack.httpClient.send(commitReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, commitRes.statusCode(), "Signed commit failed: " + commitRes.body());
        var pushReq = stack.thingworx.serviceRequest(GIT_THING_NAME, "Push", null).build();
        var pushRes = stack.httpClient.send(pushReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pushRes.statusCode(), "Signed Push failed: " + pushRes.body());
        assertSuccess(pushRes.body());

        var commitListReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "GetCommitList", null).build();
        var commitListRes =
                stack.httpClient.send(commitListReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, commitListRes.statusCode(), "GetCommitList failed: " + commitListRes.body());
        String signedCommitId = extractFirstCommitId(commitListRes.body());
        assertNotNull(
                signedCommitId, "Signed push did not create a commit: " + commitListRes.body());

        JsonObject commitInfoBody = new JsonObject();
        commitInfoBody.addProperty("CommitID", signedCommitId);
        var commitInfoReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "GetCommitInfo", commitInfoBody.toString())
                        .build();
        var commitInfoRes =
                stack.httpClient.send(commitInfoReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, commitInfoRes.statusCode(), "GetCommitInfo failed: " + commitInfoRes.body());

        var rows = responseRows(commitInfoRes.body());
        assertNotNull(rows, "Commit info did not return rows: " + commitInfoRes.body());
        assertEquals(1, rows.size(), "Expected one commit info row: " + commitInfoRes.body());
        assertEquals(
                "SIGNED",
                rows.get(0).getAsJsonObject().get("SignatureVerification").getAsString(),
                "The pushed commit does not contain a GPG signature: " + commitInfoRes.body());

        String publicKey = GPGGenerator.publicKeyFromPrivateKey(testKey);
        String publicKeyBase64 =
                java.util.Base64.getEncoder().encodeToString(publicKey.getBytes("UTF-8"));
        var giteaVerification =
                stack.gitea.execInContainer(
                        "sh",
                        "-c",
                        "set -eu; "
                                + "gnupg_home=$(mktemp -d); chmod 700 \"$gnupg_home\"; "
                                + "echo '"
                                + publicKeyBase64
                                + "' | base64 -d | gpg --batch --homedir \"$gnupg_home\" --import; "
                                + "repo=$(find /data/git/repositories -type d -name '*.git' | head -n 1); "
                                + "GNUPGHOME=\"$gnupg_home\" git --git-dir=\"$repo\" verify-commit --raw "
                                + signedCommitId);
        assertEquals(
                0,
                giteaVerification.getExitCode(),
                "Gitea git verify-commit failed: "
                        + giteaVerification.getStdout()
                        + giteaVerification.getStderr());
        assertTrue(
                (giteaVerification.getStdout() + giteaVerification.getStderr()).contains("GOODSIG"),
                "Gitea did not report a valid signature: "
                        + giteaVerification.getStdout()
                        + giteaVerification.getStderr());
    }

    @Test
    @Order(26)
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
                res.body().contains("created"), "CreateTag should confirm creation: " + res.body());
    }

    @Test
    @Order(27)
    void testGetTagList() throws Exception {
        var req = stack.thingworx.serviceRequest(GIT_THING_NAME, "GetTagList", null).build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "GetTagList failed: " + res.body());
        assertTrue(
                res.body().contains("test-v1.0"),
                "Tag list should contain test-v1.0: " + res.body());
    }

    @Test
    @Order(28)
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
    @Order(29)
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
                GIT_THING_NAME, GIT_THING_PATH + "/merge-test.txt", "Merge branch content");

        JsonObject pushBody = new JsonObject();
        pushBody.addProperty("Message", "Commit on merge branch");
        var commitReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Commit", pushBody.toString())
                        .build();
        var commitRes = stack.httpClient.send(commitReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, commitRes.statusCode(), "Commit on merge branch failed: " + commitRes.body());
        var pushReq = stack.thingworx.serviceRequest(GIT_THING_NAME, "Push", null).build();
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
        assertSuccess(body);
    }

    @Test
    @Order(30)
    void testRebase() throws Exception {
        createBranchOnGiteaViaGiteaAPI("test-rebase-branch");

        Thread.sleep(2000);

        editFileInRepoViaThingworxAPI(
                GIT_THING_NAME, GIT_THING_PATH + "/rebase-main.txt", "Main branch advancement");
        JsonObject pushBody = new JsonObject();
        pushBody.addProperty("Message", "Advance main for rebase test");
        var commitReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Commit", pushBody.toString())
                        .build();
        var commitRes = stack.httpClient.send(commitReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, commitRes.statusCode(), "Commit on main failed: " + commitRes.body());
        var pushReq = stack.thingworx.serviceRequest(GIT_THING_NAME, "Push", null).build();
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
        assertSuccess(body);
    }

    @Test
    @Order(15)
    void testGetLog() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("Ref", "main");
        body.addProperty("MaxEntries", 10);
        var req = stack.thingworx.serviceRequest(GIT_THING_NAME, "GetLog", body.toString()).build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "GetLog failed: " + res.body());
        assertNotNull(res.body());
        assertTrue(
                res.body().contains("rows"),
                "GetLog should return infotable JSON with rows: " + res.body());
    }

    @Test
    @Order(23)
    void testGetReflog() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("Ref", "HEAD");
        body.addProperty("MaxEntries", 10);
        var req =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "GetReflog", body.toString())
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "GetReflog failed: " + res.body());
        assertNotNull(res.body());
        assertTrue(
                res.body().contains("rows"),
                "GetReflog should return infotable JSON with rows: " + res.body());
    }

    @Test
    @Order(24)
    void testFetch() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("Remote", "origin");
        var req = stack.thingworx.serviceRequest(GIT_THING_NAME, "Fetch", body.toString()).build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Fetch failed: " + res.body());
        assertNotNull(res.body());
        assertSuccess(res.body());
    }

    @Test
    @Order(25)
    void testDeleteLocalRepoContent() throws Exception {
        var statusReq = stack.thingworx.serviceRequest(GIT_THING_NAME, "Status", null).build();
        var statusRes = stack.httpClient.send(statusReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, statusRes.statusCode(), "DeleteLocalRepoContent failed: " + statusRes.body());
        var pullReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "Pull", "{\"Force\":false}").build();
        var pullRes = stack.httpClient.send(pullReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pullRes.statusCode(), "Pull after delete failed: " + pullRes.body());
        assertSuccess(pullRes.body());
    }

    private String extractFirstCommitId(String json) {
        var rows = responseRows(json);
        if (rows.isEmpty()) return null;
        var row = rows.get(0).getAsJsonObject();
        return row.has("CommitID") ? row.get("CommitID").getAsString() : null;
    }
}
