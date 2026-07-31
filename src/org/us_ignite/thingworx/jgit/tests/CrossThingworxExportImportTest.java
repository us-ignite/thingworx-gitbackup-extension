package org.us_ignite.thingworx.jgit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
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
public class CrossThingworxExportImportTest {

    private static final String DB_INIT_IMAGE =
            System.getProperty("test.dbInitImage", "devopscadit/postgresql-init-twx:platform9.6.3");
    private static final String PLATFORM_IMAGE =
            System.getProperty("test.platformImage", "devopscadit/platform-postgres:platform9.6.3");

    private static final String GIT_THING_A = "CrossTwxSourceThing";
    private static final String GIT_THING_B = "CrossTwxTargetThing";
    private static final String TEST_FILE = "cross-instance-test.txt";
    private static final String TEST_CONTENT = "Hello from TWX-A!";

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

        // Init UserExtension properties on both instances
        initUserExtensions(twxA);
        initUserExtensions(twxB);
    }

    private void installExtension(ThingWorxContainer twx, String hostname) throws Exception {
        Path extZip =
                Path.of(
                        System.getProperty(
                                "test.extensionZip", "build/distributions/JGitExtension.zip"));
        @SuppressWarnings("resource")
        var installer =
                new JGitExtensionInstaller(
                        extZip, twx, network, credentials, "http://" + hostname + ":8080");
        installer.start();
        System.out.println("Extension installed on " + hostname);
        installer.close();
    }

    private void initUserExtensions(ThingWorxContainer twx) throws Exception {
        var req1 =
                twx.serviceRequest("GIT.Utility.Thing", "InitUserExtensionProperties", null)
                        .build();
        var req2 =
                twx.serviceRequest("GIT.Utility.Thing", "InitUserExtensionGpgKeysProperty", null)
                        .build();
        httpClient.send(req1, HttpResponse.BodyHandlers.ofString());
        httpClient.send(req2, HttpResponse.BodyHandlers.ofString());
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
        JsonObject body = new JsonObject();
        body.addProperty("RepoName", GIT_THING_A);
        body.addProperty("GitRepoURL", giteaRepoUrl);
        body.addProperty("RepoPath", "/" + GIT_THING_A);
        body.addProperty("FileRepo", "GitRepository");
        body.addProperty("User", credentials.giteaUser);
        body.addProperty("Password", credentials.giteaPass);
        body.addProperty("CommitUser", "TWX-A User");
        body.addProperty("CommitEmail", "twxa@example.com");
        body.addProperty("InitialBranch", "main");
        body.addProperty("ProxyURL", "");
        body.addProperty("ProxyPort", 0);
        body.addProperty("UseProxy", false);
        body.addProperty("LocalizationTokensPrefix", "");

        var createReq =
                twxA.serviceRequest("GIT.Utility.Thing", "AddNewRepo", body.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var createRes = httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                createRes.statusCode() == 200 || createRes.statusCode() == 201,
                "AddNewRepo on source failed: " + createRes.statusCode() + " " + createRes.body());

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
        var json = new JsonObject();
        json.addProperty("path", "/" + GIT_THING_A + "/" + TEST_FILE);
        json.addProperty("content", TEST_CONTENT);
        var saveReq =
                twxA.serviceRequest("GitRepository", "SaveText", json.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var saveRes = httpClient.send(saveReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                saveRes.statusCode() == 200 || saveRes.statusCode() == 201,
                "SaveText failed: " + saveRes.statusCode() + " " + saveRes.body());

        JsonObject pushBody = new JsonObject();
        pushBody.addProperty("Message", "Cross-instance test push from TWX-A");
        var pushReq =
                twxA.serviceRequest(GIT_THING_A, "Push", pushBody.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var pushRes = httpClient.send(pushReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pushRes.statusCode(), "Push on source failed: " + pushRes.body());
        assertFalse(pushRes.body().contains("Error"), "Push returned error: " + pushRes.body());
    }

    @Test
    @Order(3)
    void createGitThingOnTargetInstance() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("RepoName", GIT_THING_B);
        body.addProperty("GitRepoURL", giteaRepoUrl);
        body.addProperty("RepoPath", "/" + GIT_THING_B);
        body.addProperty("FileRepo", "GitRepository");
        body.addProperty("User", credentials.giteaUser);
        body.addProperty("Password", credentials.giteaPass);
        body.addProperty("CommitUser", "TWX-B User");
        body.addProperty("CommitEmail", "twxb@example.com");
        body.addProperty("InitialBranch", "main");
        body.addProperty("ProxyURL", "");
        body.addProperty("ProxyPort", 0);
        body.addProperty("UseProxy", false);
        body.addProperty("LocalizationTokensPrefix", "");

        var createReq =
                twxB.serviceRequest("GIT.Utility.Thing", "AddNewRepo", body.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var createRes = httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                createRes.statusCode() == 200 || createRes.statusCode() == 201,
                "AddNewRepo on target failed: " + createRes.statusCode() + " " + createRes.body());

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
        assertFalse(pullRes.body().contains("Error"), "Pull returned error: " + pullRes.body());
    }

    @Test
    @Order(5)
    void verifyEntitiesExistOnTarget() throws Exception {
        var statusReq =
                twxB.serviceRequest(GIT_THING_B, "Status", null)
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var statusRes = httpClient.send(statusReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, statusRes.statusCode(), "Status on target failed: " + statusRes.body());
        String body = statusRes.body();
        assertNotNull(body);
        assertTrue(body.contains("rows"), "Status should return infotable JSON: " + body);
    }

    @Test
    @Order(6)
    void verifyCommitHistoryOnTarget() throws Exception {
        var commitReq =
                twxB.serviceRequest(GIT_THING_B, "GetCommitList", null)
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var commitRes = httpClient.send(commitReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200, commitRes.statusCode(), "GetCommitList on target failed: " + commitRes.body());
        String body = commitRes.body();
        assertTrue(body.contains("rows"), "Commit list should have rows: " + body);
    }

    @Test
    @Order(7)
    void pushBackAndVerifyOnSource() throws Exception {
        var json = new JsonObject();
        json.addProperty("path", "/" + GIT_THING_B + "/returned-data.txt");
        json.addProperty("content", "Round-trip from TWX-B");
        var saveReq =
                twxB.serviceRequest("GitRepository", "SaveText", json.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var saveRes = httpClient.send(saveReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                saveRes.statusCode() == 200 || saveRes.statusCode() == 201,
                "SaveText on target failed: " + saveRes.statusCode());

        JsonObject pushBody = new JsonObject();
        pushBody.addProperty("Message", "Cross-instance return push from TWX-B");
        var pushReq =
                twxB.serviceRequest(GIT_THING_B, "Push", pushBody.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var pushRes = httpClient.send(pushReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pushRes.statusCode(), "Push on target failed: " + pushRes.body());

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
                twxA.serviceRequest(GIT_THING_A, "GetCommitList", null)
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var commitRes = httpClient.send(commitReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                commitRes.body().contains("Cross-instance return push"),
                "Source should see the return commit: " + commitRes.body());
    }
}
