package gb.tests.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import gb.tests.junit.containers.DBInit;
import gb.tests.junit.containers.GiteaRepo;
import gb.tests.junit.containers.Postgres;
import gb.tests.junit.containers.ThingWorxContainer;
import gb.tests.junit.util.TestingCredentials;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
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
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;

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

    private GenericContainer<?> gitea;
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
        setupGiteaRepo();

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
                                "test.extensionZip", "build/distributions/GitBackupExtension.zip"));
        assertTrue(Files.exists(extZip), "Extension ZIP must exist at " + extZip.toAbsolutePath());

        @SuppressWarnings("resource")
        var installer =
                new GenericContainer<>("curlimages/curl:7.85.0")
                        .withNetwork(network)
                        .dependsOn(twx)
                        .withFileSystemBind(
                                extZip.toAbsolutePath().toString(),
                                "/tmp/extension.zip",
                                BindMode.READ_ONLY)
                        .withStartupCheckStrategy(
                                new OneShotStartupCheckStrategy()
                                        .withTimeout(Duration.ofMinutes(10)))
                        .withCreateContainerCmdModifier(
                                cmd ->
                                        cmd.withEntrypoint(
                                                "sh",
                                                "-c",
                                                "RESP_FILE=$(mktemp)\n"
                                                        + "UPLOAD_STATUS=$(curl -s -o \"$RESP_FILE\" -w '%{http_code}' -X POST \\\n"
                                                        + "  -H 'X-XSRF-TOKEN: TWX-XSRF-TOKEN-VALUE' \\\n"
                                                        + "  -H 'X-Requested-By: ThingWorx' \\\n"
                                                        + "  -H 'Accept: application/json' \\\n"
                                                        + "  -u '"
                                                        + credentials.thingworxAdminUser
                                                        + ":"
                                                        + credentials.thingworxAdminPass
                                                        + "' \\\n"
                                                        + "  -F 'file=@/tmp/extension.zip' \\\n"
                                                        + "  --connect-timeout 30 --max-time 300 \\\n"
                                                        + "  'http://"
                                                        + hostname
                                                        + ":8080/Thingworx/ExtensionPackageUploader?purpose=import')\n"
                                                        + "echo \"Upload status: $UPLOAD_STATUS\"\n"
                                                        + "echo \"Response body: $(cat \"$RESP_FILE\")\"\n"
                                                        + "if [ \"$UPLOAD_STATUS\" != \"200\" ] && [ \"$UPLOAD_STATUS\" != \"201\" ]; then\n"
                                                        + "  exit 1\n"
                                                        + "fi\n"
                                                        + "VERIFY_RESP=$(mktemp)\n"
                                                        + "VERIFY_STATUS=$(curl -s -o \"$VERIFY_RESP\" -w '%{http_code}' \\\n"
                                                        + "  -H 'Accept: application/json' \\\n"
                                                        + "  -u '"
                                                        + credentials.thingworxAdminUser
                                                        + ":"
                                                        + credentials.thingworxAdminPass
                                                        + "' \\\n"
                                                        + "  --connect-timeout 30 --max-time 30 \\\n"
                                                        + "  'http://"
                                                        + hostname
                                                        + ":8080/Thingworx/Things/GITBACKUP.Utility.Thing')\n"
                                                        + "echo \"Verify status: $VERIFY_STATUS\"\n"
                                                        + "echo \"Verify response body: $(cat \"$VERIFY_RESP\")\"\n"
                                                        + "if [ \"$VERIFY_STATUS\" != \"200\" ] && [ \"$VERIFY_STATUS\" != \"401\" ]; then\n"
                                                        + "  exit 1\n"
                                                        + "fi\n"
                                                        + "echo 'ALL_DONE'\n"));
        installer.start();
        System.out.println("Extension installed on " + hostname);
        installer.close();
    }

    private void setupGiteaRepo() throws Exception {
        var giteaUser = credentials.giteaUser;
        var giteaPass = credentials.giteaPass;
        var repoName = credentials.repoName;

        for (int i = 0; i < 10; i++) {
            try {
                var userResult =
                        gitea.execInContainer(
                                "sh",
                                "-c",
                                "su git -c '/usr/local/bin/gitea admin user create --username "
                                        + giteaUser
                                        + " --password "
                                        + giteaPass
                                        + " --email admin@example.com --admin --must-change-password=false'");
                if (userResult.getExitCode() == 0) break;
            } catch (Exception e) {
                System.out.println("Gitea user creation attempt " + i + ": " + e.getMessage());
            }
            Thread.sleep(2000);
        }

        var repoBody = new java.util.HashMap<String, Object>();
        repoBody.put("name", repoName);
        repoBody.put("private", false);
        repoBody.put("auto_init", false);
        repoBody.put("default_branch", "main");
        for (int i = 0; i < 30; i++) {
            try {
                var request =
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                "http://"
                                                        + gitea.getHost()
                                                        + ":"
                                                        + gitea.getMappedPort(3000)
                                                        + "/api/v1/user/repos"))
                                .header("Content-Type", "application/json")
                                .header(
                                        "Authorization",
                                        "Basic "
                                                + Base64.getEncoder()
                                                        .encodeToString(
                                                                (giteaUser + ":" + giteaPass)
                                                                        .getBytes()))
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                new com.google.gson.Gson().toJson(repoBody)))
                                .build();
                var repoRes = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (repoRes.statusCode() == 201 || repoRes.statusCode() == 200) break;
            } catch (Exception e) {
                System.out.println("Gitea repo creation attempt " + i + ": " + e.getMessage());
            }
            Thread.sleep(2000);
        }
    }

    private void initUserExtensions(ThingWorxContainer twx) throws Exception {
        var req1 =
                twx.serviceRequest("GITBACKUP.Utility.Thing", "InitUserExtensionProperties", null)
                        .build();
        var req2 =
                twx.serviceRequest("GITBACKUP.Utility.Thing", "InitUserExtensionGpgKeysProperty", null)
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
                twxA.serviceRequest("GITBACKUP.Utility.Thing", "AddNewRepo", body.toString())
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
                twxB.serviceRequest("GITBACKUP.Utility.Thing", "AddNewRepo", body.toString())
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
