package org.us_ignite.thingworx.jgit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.us_ignite.thingworx.jgit.test.util.ServiceResultAssertions.assertServiceSuccess;
import static org.us_ignite.thingworx.jgit.test.util.ServiceResultAssertions.assertSuccess;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Comparator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.us_ignite.thingworx.jgit.test.containers.GiteaInit;
import org.us_ignite.thingworx.jgit.test.containers.GiteaRepo;
import org.us_ignite.thingworx.jgit.test.containers.JGitExtensionInstaller;
import org.us_ignite.thingworx.testcontainers.DBInit;
import org.us_ignite.thingworx.testcontainers.Postgres;
import org.us_ignite.thingworx.testcontainers.TestImages;
import org.us_ignite.thingworx.testcontainers.TestingCredentials;
import org.us_ignite.thingworx.testcontainers.ThingWorxContainer;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EntitySyncTest {

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
        dbInitA =
                new DBInit(
                        TestImages.dbInitImage(), postgresA, network, credentials, "postgresql-a");
        dbInitA.start();
        twxA =
                new ThingWorxContainer(
                        TestImages.platformImage(),
                        dbInitA,
                        postgresA,
                        network,
                        credentials,
                        "postgresql-a",
                        "thingworx-a");
        twxA.withEnv("THINGWORX_JGIT_NETWORK_ALLOWLIST", "gitea");
        twxA.withEnv("THINGWORX_JGIT_ALLOW_INSECURE_CREDENTIALS", "true");
        twxA.start();
        installExtension(twxA, "thingworx-a");

        postgresB = new Postgres(network, credentials, "postgresql-b");
        postgresB.start();
        dbInitB =
                new DBInit(
                        TestImages.dbInitImage(), postgresB, network, credentials, "postgresql-b");
        dbInitB.start();
        twxB =
                new ThingWorxContainer(
                        TestImages.platformImage(),
                        dbInitB,
                        postgresB,
                        network,
                        credentials,
                        "postgresql-b",
                        "thingworx-b");
        twxB.withEnv("THINGWORX_JGIT_NETWORK_ALLOWLIST", "gitea");
        twxB.withEnv("THINGWORX_JGIT_ALLOW_INSECURE_CREDENTIALS", "true");
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
                                "test.extensionZip",
                                "apps/thingworx-jgit-extension/build/distributions/JGitExtension.zip"));
        var installer =
                new JGitExtensionInstaller(
                        extZip, twx, network, credentials, "http://" + hostname + ":8080");
        installer.start();
        System.out.println("Extension installed on " + hostname);
        installer.close();
    }

    /**
     * Exports the tracked project via Status and stages every change. Commit only records staged
     * changes, and neither export nor Commit stages files automatically.
     */
    /** Calls a sync service, which Git operations no longer do themselves, and requires success. */
    private void sync(ThingWorxContainer twx, String gitThing, String service, String body)
            throws Exception {
        var res =
                httpClient.send(
                        twx.serviceRequest(gitThing, service, body)
                                .timeout(Duration.ofSeconds(60))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), service + " failed: " + res.body());
        assertSuccess(res.body());
    }

    private void syncTo(ThingWorxContainer twx, String gitThing) throws Exception {
        sync(twx, gitThing, "SyncToThingWorx", "{}");
    }

    private void stageAll(ThingWorxContainer twx, String gitThing) throws Exception {
        sync(twx, gitThing, "SyncFromThingWorx", null);
        var statusRes =
                httpClient.send(
                        twx.serviceRequest(gitThing, "Status", null)
                                .timeout(Duration.ofSeconds(30))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(200, statusRes.statusCode(), "Status failed: " + statusRes.body());
        assertSuccess(statusRes.body());
        var addRes =
                httpClient.send(
                        twx.serviceRequest(gitThing, "Add", "{\"All\":true}")
                                .timeout(Duration.ofSeconds(30))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(200, addRes.statusCode(), "Add failed: " + addRes.body());
        assertSuccess(addRes.body());
    }

    /**
     * Builds a GIT.TrackedProject.DataShape InfoTable payload for RepositoryCreate, placing each
     * project in {@code <parent>/<ProjectName>}.
     */
    private static JsonObject trackedProjects(String parent, String... projects) {
        JsonObject fields = new JsonObject();
        for (String name : new String[] {"ProjectName", "Path"}) {
            JsonObject field = new JsonObject();
            field.addProperty("name", name);
            field.addProperty("baseType", "STRING");
            fields.add(name, field);
        }
        JsonObject shape = new JsonObject();
        shape.add("fieldDefinitions", fields);
        com.google.gson.JsonArray rows = new com.google.gson.JsonArray();
        for (String project : projects) {
            JsonObject row = new JsonObject();
            row.addProperty("ProjectName", project);
            row.addProperty("Path", parent.replaceAll("^/+|/+$", "") + "/" + project);
            rows.add(row);
        }
        JsonObject table = new JsonObject();
        table.add("dataShape", shape);
        table.add("rows", rows);
        return table;
    }

    private void configureRepository(
            ThingWorxContainer twx, String thingName, String fullName, String email)
            throws Exception {
        // RepositoryCreate tracks only existing projects, so create it first on this instance.
        JsonObject project = new JsonObject();
        project.addProperty("name", TEST_PROJECT);
        httpClient.send(
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        twx.getExternalUrl()
                                                + "/Thingworx/Resources/EntityServices/Services/CreateProject"))
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
                        .POST(HttpRequest.BodyPublishers.ofString(project.toString()))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        JsonObject body = new JsonObject();
        body.addProperty("RepoName", thingName);
        body.addProperty("GitRepoURL", giteaRepoUrl);
        body.addProperty("BranchName", "main");
        body.add("TrackedProjects", trackedProjects(SHARED_REPO_PATH, TEST_PROJECT));
        body.addProperty("GitCommitterUser", credentials.giteaUser);
        body.addProperty("GitCommitterPassword", credentials.giteaPass);
        body.addProperty("GitCommitterEmail", email);
        body.addProperty("GitCommitterFullName", fullName);
        var createRes =
                httpClient.send(
                        twx.serviceRequest("GIT.Utility.Thing", "RepositoryCreate", body.toString())
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertServiceSuccess(createRes, "RepositoryCreate for " + thingName);
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
        assertServiceSuccess(verifyRes, "GetCurrentBranch for source repository");
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

        stageAll(twxA, GIT_THING_A);
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
        assertServiceSuccess(verifyRes, "GetCurrentBranch for target repository");
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
        syncTo(twxB, GIT_THING_B);
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

        stageAll(twxB, GIT_THING_B);
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
        assertSuccess(pullRes.body());
        syncTo(twxA, GIT_THING_A);

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

        stageAll(twxA, GIT_THING_A);
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
                mergeRes.body().contains("fast-forward to")
                        || mergeRes.body().contains("already up to date"),
                "Expected fast-forward merge result: " + mergeRes.body());
        syncTo(twxA, GIT_THING_A);

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
        stageAll(twxA, GIT_THING_A);
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
        stageAll(twxA, GIT_THING_A);
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
        assertSuccess(abortRes.body());
        syncTo(twxA, GIT_THING_A);

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

    /**
     * One entity that cannot be imported no longer fails the whole sync: the rest of the project is
     * imported and the entity is returned as a failure row (issue #648).
     */
    @Test
    @Order(10)
    void syncToThingWorxWithMalformedEntityImportsTheRestAndReportsIt() throws Exception {
        String malformedThing = "CrossSync.MalformedThing";
        String commitMessage = "Commit malformed entity for failure test";

        // Create a Thing via ThingWorx API with an invalid template (via DAP export
        // path)
        // This Thing will be exported to Git and then cause import to fail on the other
        // side
        JsonObject thingBody = new JsonObject();
        thingBody.addProperty("name", malformedThing);
        thingBody.addProperty("description", "malformed for failure test");
        thingBody.addProperty("thingTemplateName", "NonExistentTemplateThatWillFailImport");
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
        // CreateThing may fail for non-existent template (expected for malformed test)
        // If it fails, we directly create the XML file via the FileRepository and
        // commit
        if (thingRes.statusCode() != 200 && thingRes.statusCode() != 201) {
            // Fallback: directly create the Thing XML via DAP-style file in the repo
            // Use JGit to create the file in the shared repo
            Path tempDir = Files.createTempDirectory("jgit-malformed-dap");
            try {
                String giteaHost = gitea.getHost();
                int giteaPort = gitea.getMappedPort(3000);
                String cloneUrl =
                        "http://"
                                + giteaHost
                                + ":"
                                + giteaPort
                                + "/"
                                + credentials.giteaUser
                                + "/"
                                + credentials.repoName
                                + ".git";
                var cp =
                        new UsernamePasswordCredentialsProvider(
                                credentials.giteaUser, credentials.giteaPass);
                Git cloned =
                        Git.cloneRepository()
                                .setURI(cloneUrl)
                                .setDirectory(tempDir.toFile())
                                .setBranch("main")
                                .setCredentialsProvider(cp)
                                .call();
                String malformedPath =
                        "shared/" + TEST_PROJECT + "/Things/" + malformedThing + ".xml";
                Path filePath = tempDir.resolve(malformedPath);
                Files.createDirectories(filePath.getParent());
                String malformedXml =
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                + "<Entities>\n"
                                + "  <Things>\n"
                                + "    <Thing name=\""
                                + malformedThing
                                + "\" description=\"malformed\">\n"
                                + "      <ThingTemplate>NonExistentTemplateThatWillFailImport</ThingTemplate>\n"
                                + "    </Thing>\n"
                                + "  </Things>\n"
                                + "</Entities>";
                Files.writeString(filePath, malformedXml, StandardCharsets.UTF_8);
                cloned.add().addFilepattern(malformedPath).call();
                cloned.commit()
                        .setMessage(commitMessage)
                        .setAuthor("Test User", "test@example.com")
                        .setCommitter("Test User", "test@example.com")
                        .call();
                cloned.push().setCredentialsProvider(cp).call();
                cloned.close();
            } finally {
                try (var walk = Files.walk(tempDir)) {
                    walk.sorted(Comparator.reverseOrder())
                            .forEach(
                                    p -> {
                                        try {
                                            Files.deleteIfExists(p);
                                        } catch (Exception ignored) {
                                        }
                                    });
                } catch (Exception ignored) {
                }
            }
        } else {
            // Created via API, now commit and push via Git thing
            stageAll(twxA, GIT_THING_A);
            JsonObject commitBody = new JsonObject();
            commitBody.addProperty("Message", commitMessage);
            var commitRes =
                    httpClient.send(
                            twxA.serviceRequest(GIT_THING_A, "Commit", commitBody.toString())
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertEquals(
                    200,
                    commitRes.statusCode(),
                    "Commit malformed Thing failed: " + commitRes.body());
            var pushRes =
                    httpClient.send(
                            twxA.serviceRequest(GIT_THING_A, "Push", null).build(),
                            HttpResponse.BodyHandlers.ofString());
            assertEquals(
                    200, pushRes.statusCode(), "Push malformed Thing failed: " + pushRes.body());
        }

        JsonObject pullBody = new JsonObject();
        pullBody.addProperty("Force", true);
        var pullReq =
                twxB.serviceRequest(GIT_THING_B, "Pull", pullBody.toString())
                        .timeout(Duration.ofSeconds(60))
                        .build();
        var pullRes = httpClient.send(pullReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                pullRes.statusCode(),
                "Pull with malformed entity transport failed: " + pullRes.body());
        assertSuccess(pullRes.body());

        var syncRes =
                httpClient.send(
                        twxB.serviceRequest(GIT_THING_B, "SyncToThingWorx", "{\"Force\":true}")
                                .timeout(Duration.ofSeconds(60))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, syncRes.statusCode(), "SyncToThingWorx transport failed: " + syncRes.body());
        JsonObject table = JsonParser.parseString(syncRes.body()).getAsJsonObject();
        assertTrue(table.has("rows"), "Sync result must be InfoTable: " + syncRes.body());
        var rows = table.getAsJsonArray("rows");
        assertEquals(1, rows.size(), "Sync result must have one row: " + syncRes.body());
        var row = rows.get(0).getAsJsonObject();
        assertFalse(
                row.get("Error").getAsBoolean(),
                "The rest of the project should still import, so the sync succeeds partially: "
                        + syncRes.body());
        String message = row.get("Message").getAsString();
        assertTrue(
                message.contains("partially imported"),
                "Message must say the import was partial: " + message);
        assertTrue(
                message.contains(TEST_PROJECT + "/Things/" + malformedThing),
                "Message must name the entity that failed: " + message);
        var failures = row.getAsJsonObject("Result").getAsJsonArray("rows");
        boolean reported = false;
        for (var failure : failures) {
            JsonObject item = failure.getAsJsonObject();
            if (TEST_PROJECT.equals(item.get("ProjectName").getAsString())
                    && "Things".equals(item.get("Collection").getAsString())
                    && malformedThing.equals(item.get("EntityName").getAsString())) {
                assertFalse(
                        item.get("Reason").getAsString().isBlank(),
                        "The failure must carry the reason: " + item);
                reported = true;
            }
        }
        assertTrue(reported, "The malformed Thing must be a failure row: " + syncRes.body());

        var logReq =
                twxB.serviceRequest(GIT_THING_B, "GetLog", "{\"Ref\":\"main\"}")
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var logRes = httpClient.send(logReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, logRes.statusCode(), "GetLog after malformed import failed: " + logRes.body());
        assertServiceSuccess(logRes, "GetLog after malformed pull");
        assertTrue(
                logRes.body().contains(commitMessage),
                "Git should have received malformed commit despite import failure: "
                        + logRes.body());

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
                                                        + "/contents/shared/"
                                                        + TEST_PROJECT
                                                        + "/Things/"
                                                        + malformedThing
                                                        + ".xml?ref=main"))
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
                giteaRes.statusCode(),
                "Malformed file should be in Gitea: " + giteaRes.body());
    }
}
