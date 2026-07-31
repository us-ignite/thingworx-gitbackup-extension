package org.us_ignite.thingworx.jgit.tests;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import org.us_ignite.thingworx.jgit.tests.containers.JGitExtensionTestStack;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class EntitySyncTest {

    private static final String DB_INIT_IMAGE =
            System.getProperty("test.dbInitImage", "devopscadit/postgresql-init-twx:platform9.6.3");
    private static final String PLATFORM_IMAGE =
            System.getProperty("test.platformImage", "devopscadit/platform-postgres:platform9.6.3");

    private static final String GIT_THING_NAME = "ITEntitySyncThing";
    private static final String TEST_PROJECT = "TestProject";
    private static final String TEST_THING = "IT.ESync.TestThing";
    private static final String TEST_THING_SHAPE = "IT.ESync.TestShape";
    private static final String ORIGINAL_DESC = "Original description for entity sync test";
    private TestingCredentials credentials;
    private String giteaRepoUrl;
    private JGitExtensionTestStack stack;
    private String authHeader;

    @BeforeAll
    public void beforeAll() throws Exception {
        credentials = new TestingCredentials();
        giteaRepoUrl =
                "http://gitea:3000/" + credentials.giteaUser + "/" + credentials.repoName + ".git";
        stack = new JGitExtensionTestStack(DB_INIT_IMAGE, PLATFORM_IMAGE, credentials);
        authHeader =
                "Basic "
                        + Base64.getEncoder()
                                .encodeToString(
                                        (credentials.thingworxAdminUser
                                                        + ":"
                                                        + credentials.thingworxAdminPass)
                                                .getBytes());
    }

    @AfterAll
    public void afterAll() {
        stack.close();
    }

    private HttpRequest.Builder resourceRequest(
            String resourceName, String serviceName, String body) {
        var uri =
                URI.create(
                        stack.thingworx.getExternalUrl()
                                + "/Thingworx/Resources/"
                                + resourceName
                                + "/Services/"
                                + serviceName);
        return HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                .header("X-Requested-By", "ThingWorx")
                .POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
    }

    @Test
    @Order(1)
    void createProject() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("name", TEST_PROJECT);
        body.addProperty("description", "Entity sync test project");
        var req = resourceRequest("EntityServices", "CreateProject", body.toString()).build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                res.statusCode() == 200 || res.statusCode() == 201,
                "CreateProject failed: " + res.statusCode() + " " + res.body());
    }

    @Test
    @Order(2)
    void createTestThing() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("name", TEST_THING);
        body.addProperty("description", ORIGINAL_DESC);
        body.addProperty("thingTemplateName", "GenericThing");
        body.addProperty("projectName", TEST_PROJECT);
        var req = resourceRequest("EntityServices", "CreateThing", body.toString()).build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                res.statusCode() == 200 || res.statusCode() == 201,
                "CreateThing failed: " + res.statusCode() + " " + res.body());

        var enableReq =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        stack.thingworx.getExternalUrl()
                                                + "/Thingworx/Things/"
                                                + TEST_THING
                                                + "/Services/EnableThing"))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", authHeader)
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build();
        stack.httpClient.send(enableReq, HttpResponse.BodyHandlers.ofString());

        var restartReq =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        stack.thingworx.getExternalUrl()
                                                + "/Thingworx/Things/"
                                                + TEST_THING
                                                + "/Services/RestartThing"))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", authHeader)
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build();
        stack.httpClient.send(restartReq, HttpResponse.BodyHandlers.ofString());

        Thread.sleep(2000);
    }

    @Test
    @Order(3)
    void createTestThingShape() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("name", TEST_THING_SHAPE);
        body.addProperty("description", "Test thing shape for entity sync round-trip");
        body.addProperty("projectName", TEST_PROJECT);
        var req = resourceRequest("EntityServices", "CreateThingShape", body.toString()).build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                res.statusCode() == 200 || res.statusCode() == 201,
                "CreateThingShape failed: " + res.statusCode() + " " + res.body());
    }

    @Test
    @Order(4)
    void createGitThingWithProject() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("RepoName", GIT_THING_NAME);
        body.addProperty("GitRepoURL", giteaRepoUrl);
        body.addProperty("RepoPath", "/" + GIT_THING_NAME);
        body.addProperty("FileRepo", "GitRepository");
        body.addProperty("User", credentials.giteaUser);
        body.addProperty("Password", credentials.giteaPass);
        body.addProperty("CommitUser", "Entity Sync Test");
        body.addProperty("CommitEmail", "entity-sync@test.com");
        body.addProperty("InitialBranch", "main");
        body.addProperty("ProxyURL", "");
        body.addProperty("ProxyPort", 0);
        body.addProperty("UseProxy", false);
        body.addProperty("LocalizationTokensPrefix", "");
        body.addProperty("ProjectName", TEST_PROJECT);

        var req =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "AddNewRepo", body.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                res.statusCode() == 200 || res.statusCode() == 201,
                "AddNewRepo failed: " + res.statusCode() + " " + res.body());

        Thread.sleep(5000);
        var verifyReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "GetCurrentBranch", null).build();
        var verifyRes = stack.httpClient.send(verifyReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                verifyRes.statusCode() == 200 || verifyRes.statusCode() == 201,
                "GitThing not created: " + verifyRes.statusCode() + " " + verifyRes.body());
    }

    @Test
    @Order(5)
    void testPullFirst() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("Force", false);
        var req =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Pull", body.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Pull failed: " + res.body());
        assertFalse(res.body().contains("Error"), "Pull returned error: " + res.body());
    }

    @Test
    @Order(6)
    void syncProjectToRepository() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("GitThingName", GIT_THING_NAME);
        var req =
                stack.thingworx
                        .serviceRequest(
                                "GIT.Utility.Thing", "SyncProjectToRepository", body.toString())
                        .timeout(Duration.ofSeconds(60))
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "SyncProjectToRepository failed: " + res.body());
        assertFalse(res.body().contains("Error"), "Sync returned error: " + res.body());
    }

    @Test
    @Order(7)
    void verifyExportedFilesExist() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("path", "/" + GIT_THING_NAME + "/" + TEST_PROJECT + "/Things");
        var req =
                stack.thingworx
                        .serviceRequest("GitRepository", "ListFiles", body.toString())
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                res.statusCode(),
                "ListFiles for Things failed: " + res.statusCode() + " " + res.body());
        assertTrue(
                res.body().contains(TEST_THING),
                "Exported Thing file not found in "
                        + body.get("path").getAsString()
                        + ": "
                        + res.body());

        JsonObject shapeBody = new JsonObject();
        shapeBody.addProperty("path", "/" + GIT_THING_NAME + "/" + TEST_PROJECT + "/ThingShapes");
        var shapeReq =
                stack.thingworx
                        .serviceRequest("GitRepository", "ListFiles", shapeBody.toString())
                        .build();
        var shapeRes = stack.httpClient.send(shapeReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, shapeRes.statusCode(), "ListFiles for ThingShapes failed: " + shapeRes.body());
        assertTrue(
                shapeRes.body().contains(TEST_THING_SHAPE),
                "Exported ThingShape file not found: " + shapeRes.body());
    }

    @Test
    @Order(8)
    void testCommitService() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("Message", "Initial export of TestProject entities");
        var req =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Commit", body.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, res.statusCode(), "Commit failed: " + res.statusCode() + " " + res.body());
        assertFalse(res.body().contains("Error"), "Commit returned error: " + res.body());
        assertTrue(
                res.body().contains("Commit succeeded"),
                "Commit response should indicate success: " + res.body());

        var commitListReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "GetCommitList", null).build();
        var commitListRes =
                stack.httpClient.send(commitListReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, commitListRes.statusCode());
        var json = JsonParser.parseString(commitListRes.body()).getAsJsonObject();
        assertTrue(json.has("rows"), "Commit list should have rows: " + commitListRes.body());
        var rows = json.getAsJsonArray("rows");
        assertTrue(rows.size() >= 1, "Should have at least 1 commit: " + commitListRes.body());
        assertEquals(
                "Initial export of TestProject entities",
                rows.get(0).getAsJsonObject().get("CommitName").getAsString(),
                "Latest commit message mismatch");
    }

    static final String SECOND_THING = "IT.ESync.SecondThing";

    @Test
    @Order(9)
    void createSecondThing() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("name", SECOND_THING);
        body.addProperty("description", "Second thing for entity sync diff test");
        body.addProperty("thingTemplateName", "GenericThing");
        body.addProperty("projectName", TEST_PROJECT);
        var req = resourceRequest("EntityServices", "CreateThing", body.toString()).build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                res.statusCode() == 200 || res.statusCode() == 201,
                "Create second thing failed: " + res.statusCode() + " " + res.body());

        var enableReq =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        stack.thingworx.getExternalUrl()
                                                + "/Thingworx/Things/"
                                                + SECOND_THING
                                                + "/Services/EnableThing"))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", authHeader)
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build();
        stack.httpClient.send(enableReq, HttpResponse.BodyHandlers.ofString());

        var restartReq =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        stack.thingworx.getExternalUrl()
                                                + "/Thingworx/Things/"
                                                + SECOND_THING
                                                + "/Services/RestartThing"))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", authHeader)
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build();
        stack.httpClient.send(restartReq, HttpResponse.BodyHandlers.ofString());
        Thread.sleep(2000);
    }

    @Test
    @Order(10)
    void exportSecondEntity() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("GitThingName", GIT_THING_NAME);
        var req =
                stack.thingworx
                        .serviceRequest(
                                "GIT.Utility.Thing", "SyncProjectToRepository", body.toString())
                        .timeout(Duration.ofSeconds(60))
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Export second entity failed: " + res.body());
        assertFalse(
                res.body().contains("Error"), "Export second entity returned error: " + res.body());
    }

    @Test
    @Order(11)
    void testGetDiffPerFileShowsNewEntity() throws Exception {
        JsonObject diffBody = new JsonObject();
        diffBody.addProperty("File", "TestProject/Things/" + SECOND_THING + ".xml");
        var diffReq =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "GetDiffPerFile", diffBody.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var diffRes = stack.httpClient.send(diffReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                diffRes.statusCode(),
                "GetDiffPerFile for new entity failed: "
                        + diffRes.statusCode()
                        + " "
                        + diffRes.body());
        String diff = diffRes.body();
        assertFalse(
                diff.isEmpty(),
                "GetDiffPerFile should show the new entity file as added. Got: " + diff);
    }

    @Test
    @Order(12)
    void pushSecondEntity() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("Message", "Add IT.ESync.SecondThing entity");
        var commitReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "Commit", body.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var commitRes = stack.httpClient.send(commitReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, commitRes.statusCode(), "Commit failed: " + commitRes.body());
        var req =
                stack.thingworx
                        .serviceRequest(GIT_THING_NAME, "Push", null)
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Push failed: " + res.statusCode() + " " + res.body());
        assertFalse(res.body().contains("Error"), "Push returned error: " + res.body());
        assertFalse(res.body().contains("Exception"), "Push threw exception: " + res.body());
    }

    @Test
    @Order(13)
    void verifyGitHistoryShowsSecondEntity() throws Exception {
        var commitListReq =
                stack.thingworx.serviceRequest(GIT_THING_NAME, "GetCommitList", null).build();
        var commitListRes =
                stack.httpClient.send(commitListReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                commitListRes.statusCode(),
                "GetCommitList failed: " + commitListRes.statusCode() + " " + commitListRes.body());
        String body = commitListRes.body();
        assertTrue(body.contains("rows"), "Commit list should have rows: " + body);

        var json = JsonParser.parseString(body).getAsJsonObject();
        var rows = json.getAsJsonArray("rows");
        assertTrue(rows.size() >= 2, "Should have at least 2 commits: " + body);

        String latestMessage = rows.get(0).getAsJsonObject().get("CommitName").getAsString();
        assertEquals(
                "Add IT.ESync.SecondThing entity", latestMessage, "Latest commit message mismatch");

        String firstCommitId = rows.get(0).getAsJsonObject().get("CommitID").getAsString();
        JsonObject diffBody = new JsonObject();
        diffBody.addProperty("File", "TestProject/Things/" + SECOND_THING + ".xml");
        diffBody.addProperty("FromCommitID", firstCommitId);
        var diffReq =
                stack.thingworx
                        .serviceRequest(
                                GIT_THING_NAME, "GetDiffPerFileBetweenCommits", diffBody.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var diffRes = stack.httpClient.send(diffReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                diffRes.statusCode(),
                "GetDiffPerFileBetweenCommits failed: "
                        + diffRes.statusCode()
                        + " "
                        + diffRes.body());
        String diffContent = diffRes.body();
        assertFalse(
                diffContent.isEmpty(),
                "GetDiffPerFileBetweenCommits should show the new entity was added. Got: "
                        + diffContent);
        assertTrue(
                diffContent.contains(SECOND_THING),
                "Diff should reference the new entity. Got: " + diffContent);
    }
}
