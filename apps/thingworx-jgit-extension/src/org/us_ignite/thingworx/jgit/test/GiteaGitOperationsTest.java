package org.us_ignite.thingworx.jgit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.us_ignite.thingworx.jgit.test.util.ServiceResultAssertions.assertServiceSuccess;
import static org.us_ignite.thingworx.jgit.test.util.ServiceResultAssertions.assertSuccess;
import static org.us_ignite.thingworx.jgit.test.util.ServiceResultAssertions.responseRows;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.us_ignite.thingworx.jgit.test.containers.JGitExtensionTestStack;
import org.us_ignite.thingworx.jgit.test.util.GPGGenerator;
import org.us_ignite.thingworx.testcontainers.TestingCredentials;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class GiteaGitOperationsTest {

    private static final String GIT_THING_NAME = "ITGiteaTestThing";
    private static final String GIT_THING_PATH = "/" + GIT_THING_NAME;
    private static final String TEST_FILE = "hello-jgit.txt";
    private TestingCredentials credentials;
    private String giteaRepoUrl;

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
        assertServiceSuccess(createRes, "CreateBranch");

        JsonObject checkoutBody = new JsonObject();
        checkoutBody.addProperty("BranchName", branchName);
        var checkoutReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "BranchSwitch", checkoutBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var checkoutRes = stack.httpClient.send(checkoutReq, HttpResponse.BodyHandlers.ofString());
        assertServiceSuccess(checkoutRes, "Checkout to new branch");

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
        assertServiceSuccess(pushRes, "Push after branch creation");

        JsonObject checkoutMainBody = new JsonObject();
        checkoutMainBody.addProperty("BranchName", "main");
        var checkoutMainReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "BranchSwitch", checkoutMainBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var checkoutMainRes =
                stack.httpClient.send(checkoutMainReq, HttpResponse.BodyHandlers.ofString());
        assertServiceSuccess(checkoutMainRes, "Checkout back to main");
    }

    @BeforeAll
    public void beforeAll() throws Exception {
        credentials = new TestingCredentials();
        giteaRepoUrl =
                "http://gitea:3000/" + credentials.giteaUser + "/" + credentials.repoName + ".git";
        stack = new JGitExtensionTestStack(credentials);
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
        // FileRepository's inherited SaveText service returns an empty JSON object on
        // success,
        // rather than the extension's one-row ServiceResult envelope.
        assertTrue(
                res.statusCode() == 200 || res.statusCode() == 201,
                "SaveText failed: " + res.statusCode() + " " + res.body());
    }

    private void stageFile(String path) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("File", repoRelativePath(path));
        body.addProperty("All", false);
        var req =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Add", body.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Add " + path + " failed: " + res.body());
        assertSuccess(res.body());
    }

    private String giteaExternalBaseUrl() {
        return "http://" + stack.gitea.getHost() + ":" + stack.gitea.getMappedPort(3000);
    }

    private HttpResponse<String> callService(String service, JsonObject body) throws Exception {
        var req =
                stack.thingworx
                        .serviceRequest(
                                GIT_THING_NAME, service, body == null ? null : body.toString())
                        .build();
        return stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private JsonArray getStatusRows() throws Exception {
        var res = callService("Status", null);
        assertEquals(200, res.statusCode(), "Status failed: " + res.body());
        return responseRows(res.body());
    }

    private JsonObject findWorkingTreeRow(JsonArray rows, String file, String area) {
        for (int i = 0; i < rows.size(); i++) {
            JsonObject row = rows.get(i).getAsJsonObject();
            if (file.equals(row.get("File").getAsString())
                    && area.equals(row.get("Area").getAsString())) {
                return row;
            }
        }
        return null;
    }

    private void applyFileAction(String action, String... files) throws Exception {
        JsonObject statusRequest = new JsonObject();
        statusRequest.addProperty("ShowUnchangedFiles", true);
        var status = callService("Status", statusRequest);
        assertEquals(200, status.statusCode(), "Status failed: " + status.body());
        JsonObject payload =
                JsonParser.parseString(status.body())
                        .getAsJsonObject()
                        .getAsJsonArray("rows")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonObject("Result");
        JsonObject changes = new JsonObject();
        changes.add("dataShape", payload.getAsJsonObject("dataShape").deepCopy());
        JsonArray rows = new JsonArray();
        for (String file : files) {
            String relativePath = repoRelativePath(file);
            JsonObject row = null;
            for (var candidate : payload.getAsJsonArray("rows")) {
                JsonObject current = candidate.getAsJsonObject();
                if (relativePath.equals(current.get("File").getAsString())) {
                    row = current;
                    break;
                }
            }
            assertNotNull(row, "File missing from working-tree status: " + relativePath);
            rows.add(row.deepCopy());
        }
        changes.add("rows", rows);

        JsonObject body = new JsonObject();
        body.add("Changes", changes);
        body.addProperty("Action", action);
        var res = callService("ApplyFileAction", body);
        assertEquals(200, res.statusCode(), "ApplyFileAction failed: " + res.body());
        assertSuccess(res.body());
    }

    private String repoRelativePath(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private void assertServiceFailure(HttpResponse<String> response, String operation) {
        assertEquals(
                200,
                response.statusCode(),
                operation + " transport failed: " + response.statusCode() + " " + response.body());
        JsonObject table = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(table.has("rows"), operation + " must return an InfoTable: " + response.body());
        JsonObject row = table.getAsJsonArray("rows").get(0).getAsJsonObject();
        assertTrue(
                row.get("Error").getAsBoolean(),
                operation + " should report Error=true: " + response.body());
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

    /** Builds a GIT.TrackedProject.DataShape InfoTable payload for RepositoryCreate. */
    private static JsonObject trackedProjects(String... projects) {
        JsonObject field = new JsonObject();
        field.addProperty("name", "ProjectName");
        field.addProperty("baseType", "STRING");
        JsonObject fields = new JsonObject();
        fields.add("ProjectName", field);
        JsonObject shape = new JsonObject();
        shape.add("fieldDefinitions", fields);
        com.google.gson.JsonArray rows = new com.google.gson.JsonArray();
        for (String project : projects) {
            JsonObject row = new JsonObject();
            row.addProperty("ProjectName", project);
            rows.add(row);
        }
        JsonObject table = new JsonObject();
        table.add("dataShape", shape);
        table.add("rows", rows);
        return table;
    }

    @Test
    @Order(1)
    void testCreateGitThing() throws Exception {
        // RepositoryCreate tracks only existing projects.
        JsonObject project = new JsonObject();
        project.addProperty("name", "GiteaGitOperationsProject");
        assertEquals(200, callEntityService("CreateProject", project).statusCode());
        JsonObject body = new JsonObject();
        body.addProperty("RepoName", GIT_THING_NAME);
        body.addProperty("GitRepoURL", giteaRepoUrl);
        body.addProperty("RepoPath", GIT_THING_PATH);
        body.addProperty("BranchName", "main");
        body.add("TrackedProjects", trackedProjects("GiteaGitOperationsProject"));
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
        assertServiceSuccess(createRes, "RepositoryCreate");
        Thread.sleep(5000);
        var verifyReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "GetCurrentBranch", null).build();
        var verifyRes = stack.httpClient.send(verifyReq, HttpResponse.BodyHandlers.ofString());
        assertServiceSuccess(verifyRes, "GetCurrentBranch after RepositoryCreate");

        var implementingReq =
                stack.thingworx
                        .templateServiceRequest(
                                "GIT.Repository.ThingTemplate", "GetImplementingThings", null)
                        .build();
        var implementingRes =
                stack.httpClient.send(implementingReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                implementingRes.statusCode(),
                "GetImplementingThings failed: " + implementingRes.body());
        assertTrue(
                implementingRes.body().contains(GIT_THING_NAME),
                "Created repo Thing should derive from GIT.Repository.ThingTemplate for mashup "
                        + "dynamic services, but it was not listed as implementing Thing. Got: "
                        + implementingRes.body());
    }

    @Test
    @Order(3)
    void testPush() throws Exception {
        editFileInRepoViaThingworxAPI(
                GIT_THING_NAME,
                GIT_THING_PATH + "/" + TEST_FILE,
                "Hello from ThingWorx JGit extension integration test!");
        stageFile(GIT_THING_PATH + "/" + TEST_FILE);

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
        editFileInRepoViaThingworxAPI(GIT_THING_NAME, "/README.md", readmeContent);

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
        assertSuccess(addRes.body());

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
        assertFalse(
                statusRes.body().contains("Unchanged"),
                "Unchanged files should be hidden by default: " + statusRes.body());

        JsonObject showUnchanged = new JsonObject();
        showUnchanged.addProperty("ShowUnchangedFiles", true);
        var allStatusReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Status", showUnchanged.toString())
                        .build();
        var allStatusRes =
                stack.httpClient.send(allStatusReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                allStatusRes.statusCode(),
                "Status with unchanged files failed: " + allStatusRes.body());
        assertTrue(
                allStatusRes.body().contains("README.md")
                        && allStatusRes.body().contains("Unchanged"),
                "A committed README should be listed as unchanged: " + allStatusRes.body());
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
        JsonArray rows = responseRows(res.body());
        assertTrue(
                rows.size() > 0 && rows.get(0).getAsJsonObject().has("BranchName"),
                "Result should wrap the current branch payload: " + res.body());
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
        stageFile(GIT_THING_PATH + "/feature-file.txt");

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
        var currentBranchRows = responseRows(currentBranchRes.body());
        assertTrue(
                anyRowContains(currentBranchRows, "it-test-feature"),
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
        JsonArray rows = responseRows(res.body());
        assertTrue(anyRowContains(rows, "main"), "Should contain main branch: " + res.body());
        assertTrue(
                anyRowContains(rows, "it-test-feature"),
                "Should contain feature branch: " + res.body());
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
                anyRowContains(responseRows(branchListRes.body()), "refs/heads/it-temp-branch"),
                "Local branch it-temp-branch should have been deleted: " + branchListRes.body());
    }

    @Test
    @Order(10)
    void testCommitInfo() throws Exception {
        var commitListReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "GetLog", "{\"Ref\":\"main\"}")
                        .build();
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
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "GetLog", "{\"Ref\":\"main\"}")
                        .build();
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
        verifyKeyBody.addProperty("GpgPrivateKey", testKey);
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
        stageFile(GIT_THING_PATH + "/signed-test.txt");

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
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "GetLog", "{\"Ref\":\"main\"}")
                        .build();
        var commitListRes =
                stack.httpClient.send(commitListReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, commitListRes.statusCode(), "GetLog failed: " + commitListRes.body());

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
        JsonArray rows = responseRows(res.body());
        assertTrue(
                anyRowContains(rows, "test-v1.0"),
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
                anyRowContains(responseRows(verifyRes.body()), "test-v1.0"),
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
        stageFile(GIT_THING_PATH + "/merge-test.txt");

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
        stageFile(GIT_THING_PATH + "/rebase-main.txt");
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
    @Order(31)
    void testWorkingTreeChangesFlow() throws Exception {
        String fileA = GIT_THING_PATH + "/changes-flow-a.txt";
        String fileB = GIT_THING_PATH + "/changes-flow-b.txt";
        editFileInRepoViaThingworxAPI(GIT_THING_NAME, fileA, "Changes flow A");
        editFileInRepoViaThingworxAPI(GIT_THING_NAME, fileB, "Changes flow B");
        editFileInRepoViaThingworxAPI(
                GIT_THING_NAME, GIT_THING_PATH + "/.gitignore", "ignored-status.txt\n");
        editFileInRepoViaThingworxAPI(
                GIT_THING_NAME, GIT_THING_PATH + "/ignored-status.txt", "ignored");

        String relA = repoRelativePath(fileA);
        String relB = repoRelativePath(fileB);

        JsonArray rows = getStatusRows();
        assertNotNull(
                findWorkingTreeRow(rows, relA, "Changes"),
                "Unstaged file A should be listed: " + rows);
        assertEquals(
                "Untracked", findWorkingTreeRow(rows, relA, "Changes").get("Status").getAsString());
        assertNotNull(
                findWorkingTreeRow(rows, relB, "Changes"),
                "Unstaged file B should be listed: " + rows);
        String ignoredPath = repoRelativePath(GIT_THING_PATH + "/ignored-status.txt");
        JsonObject ignored = findWorkingTreeRow(rows, ignoredPath, "Ignored");
        assertNotNull(ignored, "Legacy status should include ignored paths");
        assertEquals("Ignored", ignored.get("Status").getAsString());

        JsonObject filters = new JsonObject();
        filters.addProperty("ShowUnchangedFiles", true);
        filters.addProperty("HideUntracked", true);
        filters.addProperty("HideUnchanged", true);
        filters.addProperty("HideIgnored", true);
        var filtered = callService("Status", filters);
        assertEquals(200, filtered.statusCode(), filtered.body());
        for (var item : responseRows(filtered.body())) {
            String changeType = item.getAsJsonObject().get("ChangeType").getAsString();
            assertFalse("Untracked".equals(changeType));
            assertFalse("Unchanged".equals(changeType));
            assertFalse("Ignored".equals(changeType));
        }
        assertNull(findWorkingTreeRow(responseRows(filtered.body()), relA, "Changes"));

        applyFileAction("stage", fileA);
        rows = getStatusRows();
        JsonObject stagedA = findWorkingTreeRow(rows, relA, "Staged Changes");
        assertNotNull(stagedA, "Staged file A should be listed: " + rows);
        assertEquals("Added", stagedA.get("Status").getAsString());
        assertTrue(stagedA.get("Staged").getAsBoolean(), "File A should be staged: " + stagedA);
        assertNotNull(
                findWorkingTreeRow(rows, relB, "Changes"),
                "File B should remain unstaged: " + rows);

        applyFileAction("unstage", fileA);
        rows = getStatusRows();
        assertNotNull(
                findWorkingTreeRow(rows, relA, "Changes"),
                "File A should return to unstaged changes: " + rows);

        applyFileAction("stage", fileA, fileB);
        rows = getStatusRows();
        assertNotNull(
                findWorkingTreeRow(rows, relA, "Staged Changes"),
                "File A should be staged again: " + rows);
        assertNotNull(
                findWorkingTreeRow(rows, relB, "Staged Changes"),
                "File B should be staged: " + rows);

        applyFileAction("untrack", fileB);
        rows = getStatusRows();
        JsonObject untrackedB = findWorkingTreeRow(rows, relB, "Changes");
        assertNotNull(untrackedB, "Untracked file B should remain on disk: " + rows);
        assertEquals(
                "Untracked",
                untrackedB.get("ChangeType").getAsString(),
                "File B should be untracked after untrack: " + untrackedB);
        assertFalse(
                untrackedB.get("Tracked").getAsBoolean(),
                "File B should report Tracked=false: " + untrackedB);

        applyFileAction("stage", fileB);
        JsonObject commitBody = new JsonObject();
        commitBody.addProperty("Message", "Integration test: working tree changes flow");
        var commitRes = callService("CommitStaged", commitBody);
        assertEquals(200, commitRes.statusCode(), "CommitStaged failed: " + commitRes.body());
        assertSuccess(commitRes.body());

        rows = getStatusRows();
        assertNull(
                findWorkingTreeRow(rows, relA, "Staged Changes"),
                "File A should be committed: " + rows);
        assertNull(
                findWorkingTreeRow(rows, relB, "Changes"), "File B should be committed: " + rows);

        applyFileAction("untrack", fileB);
        rows = getStatusRows();
        JsonObject removedB = findWorkingTreeRow(rows, relB, "Staged Changes");
        assertNotNull(removedB, "Untracking a committed file stages its removal: " + rows);
        assertEquals("Removed", removedB.get("Status").getAsString());
        assertFalse(
                removedB.get("Tracked").getAsBoolean(), "Removed file is no longer in the index");
        untrackedB = findWorkingTreeRow(rows, relB, "Changes");
        assertNotNull(untrackedB, "Untracked file should remain on disk: " + rows);
        assertFalse(untrackedB.get("Tracked").getAsBoolean());
        applyFileAction("stage", fileB);

        String fileC = GIT_THING_PATH + "/changes-flow-c.txt";
        String relC = repoRelativePath(fileC);
        editFileInRepoViaThingworxAPI(GIT_THING_NAME, fileC, "Changes flow C");
        applyFileAction("delete", fileC);
        rows = getStatusRows();
        assertNull(
                findWorkingTreeRow(rows, relC, "Changes"),
                "Deleting an untracked file removes it from disk: " + rows);

        applyFileAction("delete", fileB);
        rows = getStatusRows();
        JsonObject deletedB = findWorkingTreeRow(rows, relB, "Staged Changes");
        assertNotNull(deletedB, "Deleting a tracked file stages its removal: " + rows);
        assertEquals("Removed", deletedB.get("Status").getAsString());
        assertNull(
                findWorkingTreeRow(rows, relB, "Changes"),
                "Deleted tracked file should not remain on disk: " + rows);
        commitBody = new JsonObject();
        commitBody.addProperty("Message", "Integration test: remove source file");
        commitRes = callService("CommitStaged", commitBody);
        assertEquals(200, commitRes.statusCode(), "CommitStaged failed: " + commitRes.body());
        assertSuccess(commitRes.body());
    }

    @Test
    @Order(32)
    void testCommitRequiresStagedChanges() throws Exception {
        JsonObject unstageAll = new JsonObject();
        unstageAll.addProperty("All", true);
        var unstageRes = callService("Unstage", unstageAll);
        assertEquals(200, unstageRes.statusCode(), "Unstage all failed: " + unstageRes.body());
        assertSuccess(unstageRes.body());

        JsonObject body = new JsonObject();
        body.addProperty("Message", "Integration test: no staged changes");
        assertServiceFailure(callService("CommitStaged", body), "CommitStaged");
        assertServiceFailure(callService("Commit", body), "Commit");
    }

    @Test
    @Order(33)
    void testRemoveEverywhereRemovesExportWhenEntityIsAlreadyAbsent() throws Exception {
        String file = GIT_THING_PATH + "/orphaned-thing.xml";
        String relative = repoRelativePath(file);
        editFileInRepoViaThingworxAPI(
                GIT_THING_NAME,
                file,
                "<Entities><Things><Thing name=\"RemoveEverywhere.AlreadyAbsent\"/></Things></Entities>");
        stageFile(file);
        JsonObject commit = new JsonObject();
        commit.addProperty("Message", "Add orphaned export fixture");
        assertSuccess(callService("CommitStaged", commit).body());

        JsonObject statusRequest = new JsonObject();
        statusRequest.addProperty("ShowUnchangedFiles", true);
        JsonObject payload =
                JsonParser.parseString(callService("Status", statusRequest).body())
                        .getAsJsonObject()
                        .getAsJsonArray("rows")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonObject("Result");
        JsonObject selected =
                findWorkingTreeRow(payload.getAsJsonArray("rows"), relative, "Unchanged");
        assertNotNull(selected, "Expected orphaned export in status");
        JsonObject changes = new JsonObject();
        changes.add("dataShape", payload.getAsJsonObject("dataShape").deepCopy());
        JsonArray rows = new JsonArray();
        rows.add(selected.deepCopy());
        changes.add("rows", rows);
        JsonObject body = new JsonObject();
        body.add("Changes", changes);
        assertSuccess(callService("RemoveEverywhere", body).body());

        JsonObject removed = findWorkingTreeRow(getStatusRows(), relative, "Staged Changes");
        assertNotNull(removed, "Tracked orphaned export should be staged as deleted");
        assertEquals("Removed", removed.get("Status").getAsString());
    }

    @Test
    @Order(34)
    void testStatusSynchronizesDeletedThingWithoutStagingIt() throws Exception {
        String thing = "StatusSync.DeletedThing";
        JsonObject create = new JsonObject();
        create.addProperty("name", thing);
        create.addProperty("thingTemplateName", "GenericThing");
        create.addProperty("projectName", "GiteaGitOperationsProject");
        assertEquals(200, callEntityService("CreateThing", create).statusCode());

        var firstStatus = callService("Status", null);
        assertSuccess(firstStatus.body());
        String file = GIT_THING_PATH + "/GiteaGitOperationsProject/Things/" + thing + ".xml";
        String relative = repoRelativePath(file);
        stageFile(file);
        JsonObject commit = new JsonObject();
        commit.addProperty("Message", "Add status sync fixture");
        assertSuccess(callService("CommitStaged", commit).body());

        JsonObject delete = new JsonObject();
        delete.addProperty("name", thing);
        assertEquals(200, callEntityService("DeleteThing", delete).statusCode());
        var refreshed = callService("Status", null);
        assertSuccess(refreshed.body());
        boolean foundMissing = false;
        for (var value : responseRows(refreshed.body())) {
            JsonObject row = value.getAsJsonObject();
            if (relative.equals(row.get("File").getAsString())) {
                foundMissing =
                        "Missing".equals(row.get("Status").getAsString())
                                && !row.get("Staged").getAsBoolean();
            }
        }
        assertTrue(
                foundMissing,
                "Status should report the tracked entity as an unstaged deletion: "
                        + refreshed.body());
        JsonObject row = findWorkingTreeRow(getStatusRows(), relative, "Changes");
        assertNotNull(row, "Changes status should retain the deletion");
        assertEquals("Deleted", row.get("ChangeType").getAsString());
        assertFalse(row.get("Staged").getAsBoolean());
    }

    private HttpResponse<String> callEntityService(String service, JsonObject body)
            throws Exception {
        String encoded =
                Base64.getEncoder()
                        .encodeToString(
                                (credentials.thingworxAdminUser
                                                + ":"
                                                + credentials.thingworxAdminPass)
                                        .getBytes(StandardCharsets.UTF_8));
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        stack.thingworx.getExternalUrl()
                                                + "/Thingworx/Resources/EntityServices/Services/"
                                                + service))
                        .header("Authorization", "Basic " + encoded)
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();
        return stack.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @Order(33)
    void testExportDoesNotStageProjectFiles() throws Exception {
        String file = GIT_THING_PATH + "/GiteaGitOperationsProject/export-not-staged-test.txt";
        editFileInRepoViaThingworxAPI(GIT_THING_NAME, file, "Export must not stage this file");

        String rel = repoRelativePath(file);
        JsonArray rows = getStatusRows();
        JsonObject row = findWorkingTreeRow(rows, rel, "Changes");
        assertNotNull(row, "Exported project file should be listed as an unstaged change: " + rows);
        assertFalse(
                row.get("Staged").getAsBoolean(),
                "Project export must not stage files automatically: " + row);

        applyFileAction("stage", file);
        JsonObject body = new JsonObject();
        body.addProperty("Message", "Integration test: export does not auto-stage");
        var commitRes = callService("CommitStaged", body);
        assertEquals(200, commitRes.statusCode(), "CommitStaged failed: " + commitRes.body());
        assertSuccess(commitRes.body());
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
    @Order(16)
    void testHistoryTreeAndTruncatedContent() throws Exception {
        String fixtureName = "history-content-fixture.txt";
        String fixturePath = GIT_THING_PATH + "/" + fixtureName;
        String relativePath = repoRelativePath(fixturePath);
        editFileInRepoViaThingworxAPI(GIT_THING_NAME, fixturePath, "History content fixture\n");
        stageFile(fixturePath);
        JsonObject commitRequest = new JsonObject();
        commitRequest.addProperty("Message", "Integration test: history fixture");
        assertServiceSuccess(callService("CommitStaged", commitRequest), "Commit history fixture");

        var logResponse = callService("GetLog", new JsonObject());
        assertServiceSuccess(logResponse, "GetLog without Ref");
        String commitId = extractFirstCommitId(logResponse.body());
        assertNotNull(commitId, "Expected a commit on the current branch");

        JsonObject treeRequest = new JsonObject();
        treeRequest.addProperty("CommitID", commitId);
        var treeResponse = callService("GetCommitTree", treeRequest);
        assertServiceSuccess(treeResponse, "GetCommitTree");
        boolean found = false;
        for (var entry : responseRows(treeResponse.body())) {
            var row = entry.getAsJsonObject();
            if (relativePath.equals(row.get("Path").getAsString())) {
                found = true;
                assertEquals(fixtureName, row.get("Name").getAsString());
                assertTrue(row.get("Size").getAsLong() > 1);
            }
        }
        assertTrue(found, "Expected the committed test file in the tree");

        JsonObject contentRequest = new JsonObject();
        contentRequest.addProperty("CommitID", commitId);
        contentRequest.addProperty("Path", relativePath);
        contentRequest.addProperty("MaxBytes", 1);
        var contentResponse = callService("GetFileContent", contentRequest);
        assertServiceSuccess(contentResponse, "GetFileContent");
        var content = responseRows(contentResponse.body()).get(0).getAsJsonObject();
        assertFalse(content.get("Binary").getAsBoolean());
        assertTrue(content.get("Truncated").getAsBoolean());
        assertEquals(1, content.get("Content").getAsString().length());
    }

    @Test
    @Order(17)
    void testConcurrentHistoryReads() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            var log =
                    stack.httpClient.sendAsync(
                            stack.thingworx.serviceRequest(GIT_THING_NAME, "GetLog", "{}").build(),
                            HttpResponse.BodyHandlers.ofString());
            var branches =
                    stack.httpClient.sendAsync(
                            stack.thingworx
                                    .serviceRequest(GIT_THING_NAME, "GetBranchList", "{}")
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            var tags =
                    stack.httpClient.sendAsync(
                            stack.thingworx
                                    .serviceRequest(GIT_THING_NAME, "GetTagList", "{}")
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertServiceSuccess(log.get(), "GetLog");
            assertServiceSuccess(branches.get(), "GetBranchList");
            assertServiceSuccess(tags.get(), "GetTagList");
        }
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
    @Order(23)
    void testGetHistoryRefsListsHeadFirstAndFiltersReflogs() throws Exception {
        JsonArray all = responseRows(callService("GetHistoryRefs", new JsonObject()).body());
        assertFalse(all.isEmpty(), "GetHistoryRefs should return at least HEAD");
        JsonObject head = all.get(0).getAsJsonObject();
        assertEquals("HEAD", head.get("RefName").getAsString());
        assertEquals("HEAD", head.get("DisplayName").getAsString());
        assertEquals("HEAD", head.get("RefType").getAsString());
        assertTrue(head.get("IsHead").getAsBoolean());
        boolean currentLocalBranch = false;
        for (var entry : all) {
            JsonObject row = entry.getAsJsonObject();
            String name = row.get("RefName").getAsString();
            assertFalse(
                    name.startsWith("refs/remotes/") && name.endsWith("/HEAD"),
                    "Symbolic remote HEAD refs should be omitted: " + name);
            if ("LOCAL".equals(row.get("RefType").getAsString())
                    && row.get("IsHead").getAsBoolean()) {
                currentLocalBranch = true;
                assertEquals(
                        name.substring("refs/heads/".length()),
                        row.get("DisplayName").getAsString());
            }
        }
        assertTrue(currentLocalBranch, "Expected the checked-out branch to be marked IsHead");

        JsonObject reflogOnly = new JsonObject();
        reflogOnly.addProperty("ReflogOnly", true);
        JsonArray withReflogs = responseRows(callService("GetHistoryRefs", reflogOnly).body());
        assertFalse(withReflogs.isEmpty(), "HEAD should have reflog entries after commits");
        for (var entry : withReflogs) {
            assertTrue(
                    entry.getAsJsonObject().get("HasReflog").getAsBoolean(),
                    "ReflogOnly should drop refs without reflogs: " + entry);
        }
    }

    @Test
    @Order(23)
    void testHistoryServicesResolveDefaultsAndRejectUnknownRefs() throws Exception {
        assertServiceSuccess(callService("GetReflog", new JsonObject()), "GetReflog without Ref");

        JsonObject shortName = new JsonObject();
        shortName.addProperty("Ref", "main");
        for (var entry : responseRows(callService("GetReflog", shortName).body())) {
            assertEquals("refs/heads/main", entry.getAsJsonObject().get("RefName").getAsString());
        }

        JsonObject unknown = new JsonObject();
        unknown.addProperty("Ref", "no-such-ref-for-history-test");
        assertServiceFailure(callService("GetReflog", unknown), "GetReflog unknown ref");
        assertServiceFailure(callService("GetLog", unknown), "GetLog unknown ref");
    }

    @Test
    @Order(23)
    void testGetExtensionLogReturnsExtensionErrors() throws Exception {
        String marker = "extension-log-test-" + System.nanoTime();
        JsonObject unknown = new JsonObject();
        unknown.addProperty("Ref", marker);
        assertServiceFailure(callService("GetLog", unknown), "GetLog unknown ref");

        JsonObject query = new JsonObject();
        query.addProperty("MaxEntries", 50);
        query.addProperty("MinimumLevel", "ERROR");
        query.addProperty("SearchText", marker);
        var response =
                stack.httpClient.send(
                        stack.thingworx
                                .serviceRequest(
                                        "GIT.Utility.Thing", "GetExtensionLog", query.toString())
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        JsonArray rows = responseRows(response.body());
        assertEquals(1, rows.size(), "Expected only the GetLog failure: " + response.body());
        JsonObject row = rows.get(0).getAsJsonObject();
        assertEquals("ERROR", row.get("Level").getAsString());
        assertTrue(row.get("Source").getAsString().contains("DapResults\n"), row.toString());
        assertTrue(
                row.get("Source").getAsString().contains("GitRepositoryShape.GetLog"),
                row.toString());
        assertTrue(row.get("Message").getAsString().contains(marker), row.toString());
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

        for (String remote : new String[] {null, "", "   "}) {
            JsonObject defaultBody = new JsonObject();
            if (remote != null) defaultBody.addProperty("Remote", remote);
            var defaultRes = callService("Fetch", defaultBody);
            assertServiceSuccess(defaultRes, "Fetch with default remote");
            assertTrue(defaultRes.body().contains("origin"), defaultRes.body());
        }

        JsonObject unknownBody = new JsonObject();
        unknownBody.addProperty("Remote", "nonexistent-remote");
        assertServiceFailure(callService("Fetch", unknownBody), "Fetch");
    }

    private String extractFirstCommitId(String json) {
        var rows = responseRows(json);
        if (rows.isEmpty()) return null;
        var row = rows.get(0).getAsJsonObject();
        return row.has("CommitID") ? row.get("CommitID").getAsString() : null;
    }

    private boolean anyRowContains(JsonArray rows, String value) {
        for (var element : rows) {
            String rendered = element.toString();
            if (rendered.contains(value)) return true;
        }
        return false;
    }
}
