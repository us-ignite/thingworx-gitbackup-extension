package org.us_ignite.thingworx.jgit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.us_ignite.thingworx.jgit.tests.util.ServiceResultAssertions.assertSuccess;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.us_ignite.thingworx.jgit.tests.containers.DBInit;
import org.us_ignite.thingworx.jgit.tests.containers.GiteaInit;
import org.us_ignite.thingworx.jgit.tests.containers.GiteaRepo;
import org.us_ignite.thingworx.jgit.tests.containers.JGitExtensionInstaller;
import org.us_ignite.thingworx.jgit.tests.containers.Postgres;
import org.us_ignite.thingworx.jgit.tests.containers.ThingWorxContainer;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EntitySyncTest {

    private static final String DB_INIT_IMAGE =
            System.getProperty("test.dbInitImage", "devopscadit/postgresql-init-twx:platform9.6.3");
    private static final String PLATFORM_IMAGE =
            System.getProperty("test.platformImage", "devopscadit/platform-postgres:platform9.6.3");

    private static final String GIT_THING_A = "CrossTwxSourceThing";
    private static final String GIT_THING_B = "CrossTwxTargetThing";
    private static final String SHARED_REPO_PATH = "/shared";
    private static final String TEST_PROJECT = "CrossSyncProject";
    private static final String SOURCE_THING = "CrossSync.SourceThing";
    private static final String UPDATED_DESCRIPTION = "Updated on ThingWorx B";

    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private TestingCredentials credentials;

    Network network = Network.newNetwork();

    private GiteaRepo gitea;
    private GiteaInit giteaInit;
    private Postgres postgresA;
    private DBInit dbInitA;
    private ThingWorxContainer twxA;
    private Postgres postgresB;
    private DBInit dbInitB;
    private ThingWorxContainer twxB;

    private String giteaRepoUrl;

    @BeforeAll
    void setup() throws Exception {
        credentials = new TestingCredentials();
        giteaRepoUrl =
                "http://gitea:3000/" + credentials.giteaUser + "/" + credentials.repoName + ".git";

        gitea = new GiteaRepo(network, credentials);
        gitea.start();
        giteaInit = new GiteaInit(gitea, network, credentials, false);
        giteaInit.start();

        postgresA = new Postgres(network, credentials, "postgresql-a");
        postgresA.start();
        dbInitA = new DBInit(DB_INIT_IMAGE, postgresA, network, credentials, "postgresql-a");
        dbInitA.start();
        twxA =
                new ThingWorxContainer(
                        PLATFORM_IMAGE,
                        dbInitA,
                        postgresA,
                        network,
                        credentials,
                        "postgresql-a",
                        "thingworx-a");
        twxA.start();
        installExtension(twxA, "thingworx-a");

        postgresB = new Postgres(network, credentials, "postgresql-b");
        postgresB.start();
        dbInitB = new DBInit(DB_INIT_IMAGE, postgresB, network, credentials, "postgresql-b");
        dbInitB.start();
        twxB =
                new ThingWorxContainer(
                        PLATFORM_IMAGE,
                        dbInitB,
                        postgresB,
                        network,
                        credentials,
                        "postgresql-b",
                        "thingworx-b");
        twxB.start();
        installExtension(twxB, "thingworx-b");

        // Both ThingWorx instances share this single Gitea container through the
        // common Testcontainers network. Their PostgreSQL and FileRepository
        // storage remain isolated from one another.
    }

    private void installExtension(ThingWorxContainer twx, String hostname) throws Exception {
        Path extZip =
                Path.of(
                        System.getProperty(
                                "test.extensionZip", "build/distributions/JGitExtension.zip"));
        var installer =
                new JGitExtensionInstaller(
                        extZip, twx, network, credentials, "http://" + hostname + ":8080");
        installer.start();
        System.out.println("Extension installed on " + hostname);
        installer.close();
    }

    private void configureRepository(
            ThingWorxContainer twx, String thingName, String fullName, String email)
            throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("RepoName", thingName);
        body.addProperty("GitRepoURL", giteaRepoUrl);
        body.addProperty("RepoPathName", SHARED_REPO_PATH);
        body.addProperty("BranchName", "main");
        body.addProperty("ProjectName", TEST_PROJECT);
        body.addProperty("GitCommitterUser", credentials.giteaUser);
        body.addProperty("GitCommitterPassword", credentials.giteaPass);
        body.addProperty("GitCommitterEmail", email);
        body.addProperty("GitCommitterFullName", fullName);
        var createRes =
                httpClient.send(
                        twx.serviceRequest("GIT.Utility.Thing", "RepositoryCreate", body.toString())
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertTrue(
                createRes.statusCode() == 200 || createRes.statusCode() == 201,
                "Repository was not created/configured: " + createRes.body());
    }

    @AfterAll
    void tearDown() {
        if (twxB != null) twxB.close();
        if (dbInitB != null) dbInitB.close();
        if (postgresB != null) postgresB.close();
        if (twxA != null) twxA.close();
        if (dbInitA != null) dbInitA.close();
        if (postgresA != null) postgresA.close();
        if (giteaInit != null) giteaInit.close();
        if (gitea != null) gitea.close();
        if (network != null) network.close();
    }

    @Test
    @Order(1)
    void createGitThingOnSourceInstance() throws Exception {
        configureRepository(twxA, GIT_THING_A, "TWX-A User", "twxa@example.com");

        Thread.sleep(5000);
        var verifyReq =
                twxA.serviceRequest(GIT_THING_A, "GetCurrentBranch", null)
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var verifyRes = httpClient.send(verifyReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                verifyRes.statusCode() == 200 || verifyRes.statusCode() == 201,
                "GitThing A was not created: " + verifyRes.statusCode() + " " + verifyRes.body());
    }

    @Test
    @Order(2)
    void pushEntitiesFromSource() throws Exception {
        JsonObject thingBody = new JsonObject();
        thingBody.addProperty("name", SOURCE_THING);
        thingBody.addProperty("description", "Entity created for cross-instance sync");
        thingBody.addProperty("thingTemplateName", "GenericThing");
        thingBody.addProperty("projectName", TEST_PROJECT);
        var thingRes =
                httpClient.send(
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                twxA.getExternalUrl()
                                                        + "/Thingworx/Resources/EntityServices/Services/CreateThing"))
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .header(
                                        "Authorization",
                                        "Basic "
                                                + Base64.getEncoder()
                                                        .encodeToString(
                                                                (credentials.thingworxAdminUser
                                                                                + ":"
                                                                                + credentials
                                                                                        .thingworxAdminPass)
                                                                        .getBytes(
                                                                                StandardCharsets
                                                                                        .UTF_8)))
                                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                                .header("X-Requested-By", "ThingWorx")
                                .POST(HttpRequest.BodyPublishers.ofString(thingBody.toString()))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertTrue(
                thingRes.statusCode() == 200 || thingRes.statusCode() == 201,
                "CreateThing failed: " + thingRes.statusCode() + " " + thingRes.body());

        var thingVerifyRes =
                httpClient.send(
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                twxA.getExternalUrl()
                                                        + "/Thingworx/Things/"
                                                        + SOURCE_THING))
                                .header("Accept", "application/json")
                                .header(
                                        "Authorization",
                                        "Basic "
                                                + Base64.getEncoder()
                                                        .encodeToString(
                                                                (credentials.thingworxAdminUser
                                                                                + ":"
                                                                                + credentials
                                                                                        .thingworxAdminPass)
                                                                        .getBytes(
                                                                                StandardCharsets
                                                                                        .UTF_8)))
                                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                                .header("X-Requested-By", "ThingWorx")
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                thingVerifyRes.statusCode(),
                "Source Thing was not created: " + thingVerifyRes.body());

        JsonObject pushBody = new JsonObject();
        pushBody.addProperty("Message", "Cross-instance entity push from TWX-A");
        var commitReq =
                twxA.serviceRequest(GIT_THING_A, "Commit", pushBody.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var commitRes = httpClient.send(commitReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("[DIAG] commitRes.body() => " + commitRes.body());
        assertEquals(200, commitRes.statusCode(), "Commit on source failed: " + commitRes.body());
        var pushReq =
                twxA.serviceRequest(GIT_THING_A, "Push", null)
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var pushRes = httpClient.send(pushReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("[DIAG] pushRes.body() => " + pushRes.body());
        assertEquals(200, pushRes.statusCode(), "Push on source failed: " + pushRes.body());
        assertSuccess(pushRes.body());
        System.out.println("[DIAG] push success ok");
        var statusAfterCommitReq =
                twxA.serviceRequest(GIT_THING_A, "Status", null)
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var statusAfterCommitRes =
                httpClient.send(statusAfterCommitReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("[DIAG] source Status => " + statusAfterCommitRes.body());
        String sourcePath = "shared/" + TEST_PROJECT + "/Things/" + SOURCE_THING + ".xml";
        var giteaRes =
                httpClient.send(
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                "http://"
                                                        + gitea.getHost()
                                                        + ":"
                                                        + gitea.getMappedPort(3000)
                                                        + "/api/v1/repos/"
                                                        + credentials.giteaUser
                                                        + "/"
                                                        + credentials.repoName
                                                        + "/contents/"
                                                        + sourcePath
                                                        + "?ref=main"))
                                .header(
                                        "Authorization",
                                        "Basic "
                                                + Base64.getEncoder()
                                                        .encodeToString(
                                                                (credentials.giteaUser
                                                                                + ":"
                                                                                + credentials
                                                                                        .giteaPass)
                                                                        .getBytes(
                                                                                StandardCharsets
                                                                                        .UTF_8)))
                                .header("Accept", "application/json")
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, giteaRes.statusCode(), "Entity was not pushed to Gitea: " + giteaRes.body());
        System.out.println("[DIAG] pushRes.body() => " + pushRes.body());
        String[] diagPaths = {
            "", "shared", "shared/CrossSyncProject", "shared/CrossSyncProject/Things", sourcePath
        };
        for (String p : diagPaths) {
            var dreq =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            "http://"
                                                    + gitea.getHost()
                                                    + ":"
                                                    + gitea.getMappedPort(3000)
                                                    + "/api/v1/repos/"
                                                    + credentials.giteaUser
                                                    + "/"
                                                    + credentials.repoName
                                                    + "/contents/"
                                                    + p
                                                    + "?ref=main"))
                            .header(
                                    "Authorization",
                                    "Basic "
                                            + Base64.getEncoder()
                                                    .encodeToString(
                                                            (credentials.giteaUser
                                                                            + ":"
                                                                            + credentials.giteaPass)
                                                                    .getBytes(
                                                                            StandardCharsets
                                                                                    .UTF_8)))
                            .header("Accept", "application/json")
                            .GET()
                            .build();
            var dres = httpClient.send(dreq, HttpResponse.BodyHandlers.ofString());
            System.out.println(
                    "[DIAG] contents["
                            + (p.isEmpty() ? "/" : p)
                            + "] -> "
                            + dres.statusCode()
                            + " "
                            + dres.body().substring(0, Math.min(dres.body().length(), 400)));
        }
        var srcFilesReq =
                twxA.serviceRequest(
                                GIT_THING_A,
                                "ListFiles",
                                "{\"path\":\""
                                        + SHARED_REPO_PATH
                                        + "/"
                                        + TEST_PROJECT
                                        + "/Things\"}")
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var srcFilesRes = httpClient.send(srcFilesReq, HttpResponse.BodyHandlers.ofString());
        System.out.println(
                "[DIAG] source ListFiles -> "
                        + srcFilesRes.statusCode()
                        + " "
                        + srcFilesRes.body());
        var treeReq =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        "http://"
                                                + gitea.getHost()
                                                + ":"
                                                + gitea.getMappedPort(3000)
                                                + "/api/v1/repos/"
                                                + credentials.giteaUser
                                                + "/"
                                                + credentials.repoName
                                                + "/git/trees/main?recursive=1"))
                        .header(
                                "Authorization",
                                "Basic "
                                        + Base64.getEncoder()
                                                .encodeToString(
                                                        (credentials.giteaUser
                                                                        + ":"
                                                                        + credentials.giteaPass)
                                                                .getBytes(StandardCharsets.UTF_8)))
                        .header("Accept", "application/json")
                        .GET()
                        .build();
        var treeRes = httpClient.send(treeReq, HttpResponse.BodyHandlers.ofString());
        System.out.println(
                "[DIAG] gitea tree(main) -> " + treeRes.statusCode() + " " + treeRes.body());
        assertTrue(
                treeRes.body().contains(SOURCE_THING + ".xml"),
                "Gitea returned the wrong file: " + treeRes.body());
    }

    @Test
    @Order(3)
    void createGitThingOnTargetInstance() throws Exception {
        configureRepository(twxB, GIT_THING_B, "TWX-B User", "twxb@example.com");

        Thread.sleep(5000);
        var verifyReq =
                twxB.serviceRequest(GIT_THING_B, "GetCurrentBranch", null)
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var verifyRes = httpClient.send(verifyReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                verifyRes.statusCode() == 200 || verifyRes.statusCode() == 201,
                "GitThing B was not created: " + verifyRes.statusCode() + " " + verifyRes.body());
    }

    @Test
    @Order(4)
    void pullEntitiesOnTargetInstance() throws Exception {
        JsonObject pullBody = new JsonObject();
        pullBody.addProperty("Force", true);
        var pullReq =
                twxB.serviceRequest(GIT_THING_B, "Pull", pullBody.toString())
                        .timeout(Duration.ofSeconds(60))
                        .build();
        var pullRes = httpClient.send(pullReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pullRes.statusCode(), "Pull on target failed: " + pullRes.body());
        assertSuccess(pullRes.body());
    }

    @Test
    @Order(5)
    void verifyEntitiesExistOnTarget() throws Exception {
        var thingReq =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        twxB.getExternalUrl()
                                                + "/Thingworx/Things/"
                                                + SOURCE_THING))
                        .header("Accept", "application/json")
                        .header(
                                "Authorization",
                                "Basic "
                                        + Base64.getEncoder()
                                                .encodeToString(
                                                        (credentials.thingworxAdminUser
                                                                        + ":"
                                                                        + credentials
                                                                                .thingworxAdminPass)
                                                                .getBytes(StandardCharsets.UTF_8)))
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .GET()
                        .build();
        var thingRes = httpClient.send(thingReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, thingRes.statusCode(), "Target Thing was not loaded: " + thingRes.body());

        var filesReq =
                twxB.serviceRequest(
                                GIT_THING_B,
                                "ListFiles",
                                "{\"path\":\""
                                        + SHARED_REPO_PATH
                                        + "/"
                                        + TEST_PROJECT
                                        + "/Things\"}")
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var filesRes = httpClient.send(filesReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, filesRes.statusCode(), "FileRepository listing failed: " + filesRes.body());
        assertTrue(
                filesRes.body().contains(SOURCE_THING),
                "Imported entity XML missing: " + filesRes.body());
    }

    @Test
    @Order(6)
    void verifyCommitHistoryOnTarget() throws Exception {
        var commitReq =
                twxB.serviceRequest(GIT_THING_B, "GetLog", "{\"Ref\":\"main\"}")
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var commitRes = httpClient.send(commitReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, commitRes.statusCode(), "GetLog on target failed: " + commitRes.body());
        String body = commitRes.body();
        assertTrue(body.contains("rows"), "Commit list should have rows: " + body);
    }

    @Test
    @Order(7)
    void pushBackAndVerifyOnSource() throws Exception {
        var updateReq =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        twxB.getExternalUrl()
                                                + "/Thingworx/Things/"
                                                + SOURCE_THING))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header(
                                "Authorization",
                                "Basic "
                                        + Base64.getEncoder()
                                                .encodeToString(
                                                        (credentials.thingworxAdminUser
                                                                        + ":"
                                                                        + credentials
                                                                                .thingworxAdminPass)
                                                                .getBytes(StandardCharsets.UTF_8)))
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .PUT(
                                HttpRequest.BodyPublishers.ofString(
                                        "{\"name\":\""
                                                + SOURCE_THING
                                                + "\",\"description\":\""
                                                + UPDATED_DESCRIPTION
                                                + "\",\"thingTemplate\":\"GenericThing\","
                                                + "\"projectName\":\""
                                                + TEST_PROJECT
                                                + "\"}"))
                        .build();
        var updateRes = httpClient.send(updateReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                updateRes.statusCode() == 200 || updateRes.statusCode() == 201,
                "Thing update failed: " + updateRes.statusCode() + " " + updateRes.body());
        var updatedThingRes =
                httpClient.send(
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                twxB.getExternalUrl()
                                                        + "/Thingworx/Things/"
                                                        + SOURCE_THING))
                                .header("Accept", "application/json")
                                .header(
                                        "Authorization",
                                        "Basic "
                                                + Base64.getEncoder()
                                                        .encodeToString(
                                                                (credentials.thingworxAdminUser
                                                                                + ":"
                                                                                + credentials
                                                                                        .thingworxAdminPass)
                                                                        .getBytes(
                                                                                StandardCharsets
                                                                                        .UTF_8)))
                                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                                .header("X-Requested-By", "ThingWorx")
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                updatedThingRes.statusCode(),
                "Updated Thing was not found: " + updatedThingRes.body());
        assertTrue(
                updatedThingRes.body().contains(UPDATED_DESCRIPTION),
                "Thing update was not applied: " + updatedThingRes.body());

        JsonObject pushBody = new JsonObject();
        pushBody.addProperty("Message", "Cross-instance entity return push from TWX-B");
        var targetCommitReq =
                twxB.serviceRequest(GIT_THING_B, "Commit", pushBody.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var targetCommitRes =
                httpClient.send(targetCommitReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                targetCommitRes.statusCode(),
                "Commit on target failed: " + targetCommitRes.body());
        var pushReq =
                twxB.serviceRequest(GIT_THING_B, "Push", null)
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var pushRes = httpClient.send(pushReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pushRes.statusCode(), "Push on target failed: " + pushRes.body());
        String updatedPath = "shared/" + TEST_PROJECT + "/Things/" + SOURCE_THING + ".xml";
        var updatedGiteaRes =
                httpClient.send(
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                "http://"
                                                        + gitea.getHost()
                                                        + ":"
                                                        + gitea.getMappedPort(3000)
                                                        + "/api/v1/repos/"
                                                        + credentials.giteaUser
                                                        + "/"
                                                        + credentials.repoName
                                                        + "/contents/"
                                                        + updatedPath
                                                        + "?ref=main"))
                                .header(
                                        "Authorization",
                                        "Basic "
                                                + Base64.getEncoder()
                                                        .encodeToString(
                                                                (credentials.giteaUser
                                                                                + ":"
                                                                                + credentials
                                                                                        .giteaPass)
                                                                        .getBytes(
                                                                                StandardCharsets
                                                                                        .UTF_8)))
                                .header("Accept", "application/json")
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                updatedGiteaRes.statusCode(),
                "Updated entity was not pushed to Gitea: " + updatedGiteaRes.body());
        String updatedXml =
                new String(
                        Base64.getMimeDecoder()
                                .decode(
                                        JsonParser.parseString(updatedGiteaRes.body())
                                                .getAsJsonObject()
                                                .get("content")
                                                .getAsString()),
                        StandardCharsets.UTF_8);
        assertTrue(
                updatedXml.contains(UPDATED_DESCRIPTION),
                "Gitea contained stale entity XML: " + updatedXml);

        var pullBody = new JsonObject();
        pullBody.addProperty("Force", false);
        var pullReq =
                twxA.serviceRequest(GIT_THING_A, "Pull", pullBody.toString())
                        .timeout(Duration.ofSeconds(60))
                        .build();
        var pullRes = httpClient.send(pullReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                pullRes.statusCode(),
                "Pull on source after round-trip failed: " + pullRes.body());

        var commitReq =
                twxA.serviceRequest(GIT_THING_A, "GetLog", "{\"Ref\":\"main\"}")
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var commitRes = httpClient.send(commitReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                commitRes.body().contains("Cross-instance entity return push from TWX-B"),
                "Source should see the return commit: " + commitRes.body());
        var sourceThingRes =
                httpClient.send(
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                twxA.getExternalUrl()
                                                        + "/Thingworx/Things/"
                                                        + SOURCE_THING))
                                .header("Accept", "application/json")
                                .header(
                                        "Authorization",
                                        "Basic "
                                                + Base64.getEncoder()
                                                        .encodeToString(
                                                                (credentials.thingworxAdminUser
                                                                                + ":"
                                                                                + credentials
                                                                                        .thingworxAdminPass)
                                                                        .getBytes(
                                                                                StandardCharsets
                                                                                        .UTF_8)))
                                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                                .header("X-Requested-By", "ThingWorx")
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                sourceThingRes.statusCode(),
                "Source Thing was not reloaded: " + sourceThingRes.body());
        assertTrue(
                sourceThingRes.body().contains(UPDATED_DESCRIPTION),
                "Source Thing has stale state: " + sourceThingRes.body());
        var filesReq =
                twxA.serviceRequest(
                                GIT_THING_A,
                                "ListFiles",
                                "{\"path\":\""
                                        + SHARED_REPO_PATH
                                        + "/"
                                        + TEST_PROJECT
                                        + "/Things\"}")
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var filesRes = httpClient.send(filesReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                filesRes.statusCode(),
                "Source FileRepository listing failed: " + filesRes.body());
        assertTrue(
                filesRes.body().contains(SOURCE_THING),
                "Returned entity XML missing: " + filesRes.body());
    }

    @Test
    @Order(8)
    void fastForwardMergeLoadsRealThingFromRepository() throws Exception {
        String thingName = "CrossSync.FastForwardThing";
        String branchName = "cross-fast-forward";

        JsonObject branchBody = new JsonObject();
        branchBody.addProperty("BranchName", branchName);
        branchBody.addProperty("StartPoint", "main");
        var branchRes =
                httpClient.send(
                        twxA.serviceRequest(GIT_THING_A, "BranchCreate", branchBody.toString())
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(200, branchRes.statusCode(), "CreateBranch failed: " + branchRes.body());

        JsonObject checkoutBody = new JsonObject();
        checkoutBody.addProperty("BranchNameOrCommit", branchName);
        var checkoutRes =
                httpClient.send(
                        twxA.serviceRequest(GIT_THING_A, "Checkout", checkoutBody.toString())
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, checkoutRes.statusCode(), "Checkout branch failed: " + checkoutRes.body());

        JsonObject thingBody = new JsonObject();
        thingBody.addProperty("name", thingName);
        thingBody.addProperty("description", "created on the fast-forward branch");
        thingBody.addProperty("thingTemplateName", "GenericThing");
        thingBody.addProperty("projectName", TEST_PROJECT);
        var thingRes =
                httpClient.send(
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                twxA.getExternalUrl()
                                                        + "/Thingworx/Resources/EntityServices/Services/CreateThing"))
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .header(
                                        "Authorization",
                                        "Basic "
                                                + Base64.getEncoder()
                                                        .encodeToString(
                                                                (credentials.thingworxAdminUser
                                                                                + ":"
                                                                                + credentials
                                                                                        .thingworxAdminPass)
                                                                        .getBytes(
                                                                                StandardCharsets
                                                                                        .UTF_8)))
                                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                                .header("X-Requested-By", "ThingWorx")
                                .POST(HttpRequest.BodyPublishers.ofString(thingBody.toString()))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertTrue(
                thingRes.statusCode() == 200 || thingRes.statusCode() == 201,
                "CreateThing failed: " + thingRes.statusCode() + " " + thingRes.body());

        JsonObject commitBody = new JsonObject();
        commitBody.addProperty("Message", "Commit fast-forward Thing");
        var commitRes =
                httpClient.send(
                        twxA.serviceRequest(GIT_THING_A, "Commit", commitBody.toString()).build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, commitRes.statusCode(), "Commit branch Thing failed: " + commitRes.body());
        var pushRes =
                httpClient.send(
                        twxA.serviceRequest(GIT_THING_A, "Push", null).build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pushRes.statusCode(), "Push branch Thing failed: " + pushRes.body());

        var checkoutMainRes =
                httpClient.send(
                        twxA.serviceRequest(
                                        GIT_THING_A,
                                        "Checkout",
                                        "{\"BranchNameOrCommit\":\"main\"}")
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                checkoutMainRes.statusCode(),
                "Checkout main failed: " + checkoutMainRes.body());

        JsonObject mergeBody = new JsonObject();
        mergeBody.addProperty("BranchName", branchName);
        var mergeRes =
                httpClient.send(
                        twxA.serviceRequest(GIT_THING_A, "Merge", mergeBody.toString()).build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(200, mergeRes.statusCode(), "Fast-forward merge failed: " + mergeRes.body());
        assertTrue(
                mergeRes.body().contains("FAST_FORWARD")
                        || mergeRes.body().contains("Fast-forward")
                        || mergeRes.body().contains("Already-up-to-date"),
                "Expected fast-forward merge result: " + mergeRes.body());

        var thingVerifyRes =
                httpClient.send(
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                twxA.getExternalUrl()
                                                        + "/Thingworx/Things/"
                                                        + thingName))
                                .header("Accept", "application/json")
                                .header(
                                        "Authorization",
                                        "Basic "
                                                + Base64.getEncoder()
                                                        .encodeToString(
                                                                (credentials.thingworxAdminUser
                                                                                + ":"
                                                                                + credentials
                                                                                        .thingworxAdminPass)
                                                                        .getBytes(
                                                                                StandardCharsets
                                                                                        .UTF_8)))
                                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                                .header("X-Requested-By", "ThingWorx")
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                thingVerifyRes.statusCode(),
                "Merged Thing was not loaded: " + thingVerifyRes.body());
        assertTrue(
                thingVerifyRes.body().contains("created on the fast-forward branch"),
                "Merged Thing has the wrong state: " + thingVerifyRes.body());
    }

    @Test
    @Order(9)
    void rebaseConflictCanBeAbortedWithRealThingState() throws Exception {
        String branchName = "cross-rebase-conflict";

        JsonObject branchBody = new JsonObject();
        branchBody.addProperty("BranchName", branchName);
        branchBody.addProperty("StartPoint", "main");
        var branchRes =
                httpClient.send(
                        twxA.serviceRequest(GIT_THING_A, "BranchCreate", branchBody.toString())
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(200, branchRes.statusCode(), "CreateBranch failed: " + branchRes.body());

        var checkoutBranchRes =
                httpClient.send(
                        twxA.serviceRequest(
                                        GIT_THING_A,
                                        "Checkout",
                                        "{\"BranchNameOrCommit\":\"" + branchName + "\"}")
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                checkoutBranchRes.statusCode(),
                "Checkout branch failed: " + checkoutBranchRes.body());

        var branchPutRes =
                httpClient.send(
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                twxA.getExternalUrl()
                                                        + "/Thingworx/Things/"
                                                        + SOURCE_THING))
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .header(
                                        "Authorization",
                                        "Basic "
                                                + Base64.getEncoder()
                                                        .encodeToString(
                                                                (credentials.thingworxAdminUser
                                                                                + ":"
                                                                                + credentials
                                                                                        .thingworxAdminPass)
                                                                        .getBytes(
                                                                                StandardCharsets
                                                                                        .UTF_8)))
                                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                                .header("X-Requested-By", "ThingWorx")
                                .PUT(
                                        HttpRequest.BodyPublishers.ofString(
                                                "{\"name\":\""
                                                        + SOURCE_THING
                                                        + "\",\"description\":\"rebase branch state\","
                                                        + "\"thingTemplate\":\"GenericThing\","
                                                        + "\"projectName\":\""
                                                        + TEST_PROJECT
                                                        + "\"}"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertTrue(
                branchPutRes.statusCode() == 200 || branchPutRes.statusCode() == 201,
                "Branch PUT failed: " + branchPutRes.body());
        var branchCommitRes =
                httpClient.send(
                        twxA.serviceRequest(
                                        GIT_THING_A,
                                        "Commit",
                                        "{\"Message\":\"Rebase branch state\"}")
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                branchCommitRes.statusCode(),
                "Branch commit failed: " + branchCommitRes.body());
        var branchPushRes =
                httpClient.send(
                        twxA.serviceRequest(GIT_THING_A, "Push", null).build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, branchPushRes.statusCode(), "Branch push failed: " + branchPushRes.body());

        var checkoutMainRes =
                httpClient.send(
                        twxA.serviceRequest(
                                        GIT_THING_A,
                                        "Checkout",
                                        "{\"BranchNameOrCommit\":\"main\"}")
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                checkoutMainRes.statusCode(),
                "Checkout main failed: " + checkoutMainRes.body());
        var mainPutRes =
                httpClient.send(
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                twxA.getExternalUrl()
                                                        + "/Thingworx/Things/"
                                                        + SOURCE_THING))
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .header(
                                        "Authorization",
                                        "Basic "
                                                + Base64.getEncoder()
                                                        .encodeToString(
                                                                (credentials.thingworxAdminUser
                                                                                + ":"
                                                                                + credentials
                                                                                        .thingworxAdminPass)
                                                                        .getBytes(
                                                                                StandardCharsets
                                                                                        .UTF_8)))
                                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                                .header("X-Requested-By", "ThingWorx")
                                .PUT(
                                        HttpRequest.BodyPublishers.ofString(
                                                "{\"name\":\""
                                                        + SOURCE_THING
                                                        + "\",\"description\":\"rebase main state\","
                                                        + "\"thingTemplate\":\"GenericThing\","
                                                        + "\"projectName\":\""
                                                        + TEST_PROJECT
                                                        + "\"}"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertTrue(
                mainPutRes.statusCode() == 200 || mainPutRes.statusCode() == 201,
                "Main PUT failed: " + mainPutRes.body());
        var mainCommitRes =
                httpClient.send(
                        twxA.serviceRequest(
                                        GIT_THING_A,
                                        "Commit",
                                        "{\"Message\":\"Rebase main state\"}")
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, mainCommitRes.statusCode(), "Main commit failed: " + mainCommitRes.body());
        var mainPushRes =
                httpClient.send(
                        twxA.serviceRequest(GIT_THING_A, "Push", null).build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(200, mainPushRes.statusCode(), "Main push failed: " + mainPushRes.body());

        var checkoutBranchAgainRes =
                httpClient.send(
                        twxA.serviceRequest(
                                        GIT_THING_A,
                                        "Checkout",
                                        "{\"BranchNameOrCommit\":\"" + branchName + "\"}")
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                checkoutBranchAgainRes.statusCode(),
                "Checkout branch again failed: " + checkoutBranchAgainRes.body());
        var rebaseRes =
                httpClient.send(
                        twxA.serviceRequest(GIT_THING_A, "Rebase", "{\"UpstreamBranch\":\"main\"}")
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(200, rebaseRes.statusCode(), "Rebase request failed: " + rebaseRes.body());
        assertTrue(
                rebaseRes.body().contains("STOPPED") || rebaseRes.body().contains("conflict"),
                "Expected rebase conflict: " + rebaseRes.body());

        var abortRes =
                httpClient.send(
                        twxA.serviceRequest(GIT_THING_A, "RebaseAbort", null).build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(200, abortRes.statusCode(), "RebaseAbort failed: " + abortRes.body());

        var verifyRes =
                httpClient.send(
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                twxA.getExternalUrl()
                                                        + "/Thingworx/Things/"
                                                        + SOURCE_THING))
                                .header("Accept", "application/json")
                                .header(
                                        "Authorization",
                                        "Basic "
                                                + Base64.getEncoder()
                                                        .encodeToString(
                                                                (credentials.thingworxAdminUser
                                                                                + ":"
                                                                                + credentials
                                                                                        .thingworxAdminPass)
                                                                        .getBytes(
                                                                                StandardCharsets
                                                                                        .UTF_8)))
                                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                                .header("X-Requested-By", "ThingWorx")
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                verifyRes.statusCode(),
                "Thing missing after rebase abort: " + verifyRes.body());
        assertTrue(
                verifyRes.body().contains("rebase branch state"),
                "Rebase abort did not restore branch state: " + verifyRes.body());
    }
}
