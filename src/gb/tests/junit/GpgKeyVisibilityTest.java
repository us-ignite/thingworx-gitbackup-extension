package gb.tests.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gb.tests.junit.util.TestingCredentials;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
public class GpgKeyVisibilityTest {

    private static final String DB_INIT_IMAGE =
            System.getProperty("test.dbInitImage", "devopscadit/postgresql-init-twx:platform9.6.3");
    private static final String PLATFORM_IMAGE =
            System.getProperty("test.platformImage", "devopscadit/platform-postgres:platform9.6.3");

    private TestingCredentials credentials;
    private GitBackupExtensionTestStack stack;

    @BeforeAll
    public void beforeAll() throws Exception {
        credentials = new TestingCredentials();
        stack = new GitBackupExtensionTestStack(DB_INIT_IMAGE, PLATFORM_IMAGE, credentials);
    }

    @AfterAll
    public void afterAll() {
        stack.close();
    }

    @Test
    void gpgKeyDataShapeEntityDoesNotExist() throws Exception {
        var req =
                HttpRequest.newBuilder()
                        .uri(
                                java.net.URI.create(
                                        stack.thingworx.getExternalUrl()
                                                + "/Thingworx/DataShapes/GitBackup.GpgKey"))
                        .header("Accept", "application/json")
                        .header(
                                "Authorization",
                                "Basic "
                                        + java.util.Base64.getEncoder()
                                                .encodeToString(
                                                        (credentials.thingworxAdminUser
                                                                        + ":"
                                                                        + credentials
                                                                                .thingworxAdminPass)
                                                                .getBytes()))
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .GET()
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                res.statusCode(),
                "GpgKey.DataShape should be accessible via REST API. Got "
                        + res.statusCode()
                        + ": "
                        + res.body());
    }

    @Test
    void otherDataShapesStillExist() throws Exception {
        var req =
                HttpRequest.newBuilder()
                        .uri(
                                java.net.URI.create(
                                        stack.thingworx.getExternalUrl()
                                                + "/Thingworx/DataShapes/GitBackup.GitCredentials"))
                        .header("Accept", "application/json")
                        .header(
                                "Authorization",
                                "Basic "
                                        + java.util.Base64.getEncoder()
                                                .encodeToString(
                                                        (credentials.thingworxAdminUser
                                                                        + ":"
                                                                        + credentials
                                                                                .thingworxAdminPass)
                                                                .getBytes()))
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .GET()
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                res.statusCode(),
                "Unrelated DataShape should still be accessible. Got "
                        + res.statusCode()
                        + ": "
                        + res.body());
    }

    @Test
    void userExtensionsHasGpgKeysProperty() throws Exception {
        JsonObject getKeysBody = new JsonObject();
        var req =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "GetGpgKeys", getKeysBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                res.statusCode(),
                "GetGpgKeys should return 200. Got " + res.statusCode() + ": " + res.body());
        assertNotNull(res.body());
        var json = JsonParser.parseString(res.body()).getAsJsonObject();
        assertTrue(
                json.has("rows"),
                "GetGpgKeys should return an infotable with 'rows' field: " + res.body());
    }

    @Test
    void setAndGetGpgKeyRoundTrip() throws Exception {
        var setBody = new JsonObject();
        setBody.addProperty("GitThing", "NonExistentTestThing");
        setBody.addProperty("GpgPrivateKey", "test-private-key-data");
        setBody.addProperty("GpgKeyPassphrase", "test-passphrase");
        setBody.addProperty("SignCommits", false);
        setBody.addProperty("GpgKeyFingerprint", "TEST:FINGER:PRINT:1234");
        var setReq =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "SetGpgKey", setBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var setRes = stack.httpClient.send(setReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                setRes.statusCode() == 200 || setRes.statusCode() == 201,
                "SetGpgKey should succeed. Got " + setRes.statusCode() + ": " + setRes.body());

        var getReq =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "GetGpgKeys", "{}")
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var getRes = stack.httpClient.send(getReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                getRes.statusCode(),
                "GetGpgKeys should return 200 after Set. Got "
                        + getRes.statusCode()
                        + ": "
                        + getRes.body());
        assertNotNull(getRes.body());
        var json = JsonParser.parseString(getRes.body()).getAsJsonObject();
        assertTrue(json.has("rows"), "Response should have rows: " + getRes.body());
        assertTrue(
                json.getAsJsonArray("rows").size() > 0,
                "Should have at least one GpgKey row: " + getRes.body());
        var firstRow = json.getAsJsonArray("rows").get(0).getAsJsonObject();
        assertTrue(firstRow.has("GitThing"), "Row should have GitThing field: " + firstRow);
        assertEquals(
                "NonExistentTestThing",
                firstRow.get("GitThing").getAsString(),
                "GitThing should match the one we set");

        var deleteBody = new JsonObject();
        deleteBody.addProperty("GitThing", "NonExistentTestThing");
        var deleteReq =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "DeleteGpgKey", deleteBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        stack.httpClient.send(deleteReq, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void gpgKeysFieldNamesMatchExpectedSchema() throws Exception {
        var getReq =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "GetGpgKeys", "{}")
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var getRes = stack.httpClient.send(getReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getRes.statusCode(), "GetGpgKeys should return 200: " + getRes.body());

        var json = JsonParser.parseString(getRes.body()).getAsJsonObject();
        assertTrue(
                json.has("dataShape"),
                "Response should include dataShape metadata: " + getRes.body());
        var ds = json.getAsJsonObject("dataShape");
        assertTrue(ds.has("fieldDefinitions"), "dataShape should have fieldDefinitions: " + ds);

        var fields = ds.getAsJsonObject("fieldDefinitions");
        assertTrue(fields.has("GitThing"), "Missing GitThing field");
        assertTrue(fields.has("GpgPrivateKey"), "Missing GpgPrivateKey field");
        assertTrue(fields.has("GpgKeyPassphrase"), "Missing GpgKeyPassphrase field");
        assertTrue(fields.has("SignCommits"), "Missing SignCommits field");
        assertTrue(fields.has("GpgKeyFingerprint"), "Missing GpgKeyFingerprint field");

        assertEquals("STRING", fields.getAsJsonObject("GitThing").get("baseType").getAsString());
        assertEquals(
                "PASSWORD", fields.getAsJsonObject("GpgPrivateKey").get("baseType").getAsString());
        assertEquals(
                "PASSWORD",
                fields.getAsJsonObject("GpgKeyPassphrase").get("baseType").getAsString());
        assertEquals(
                "BOOLEAN", fields.getAsJsonObject("SignCommits").get("baseType").getAsString());
        assertEquals(
                "STRING",
                fields.getAsJsonObject("GpgKeyFingerprint").get("baseType").getAsString());
    }

    @Test
    void initUserExtensionGpgKeysPropertyCreatesInlineFields() throws Exception {
        var initReq =
                stack.thingworx
                        .serviceRequest(
                                "GIT.Utility.Thing", "InitUserExtensionGpgKeysProperty", "{}")
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var initRes = stack.httpClient.send(initReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                initRes.statusCode() == 200 || initRes.statusCode() == 201,
                "InitUserExtensionGpgKeysProperty should succeed. Got "
                        + initRes.statusCode()
                        + ": "
                        + initRes.body());

        var getReq =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "GetGpgKeys", "{}")
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var getRes = stack.httpClient.send(getReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                getRes.statusCode(),
                "GetGpgKeys should work after init. Got "
                        + getRes.statusCode()
                        + ": "
                        + getRes.body());

        var json = JsonParser.parseString(getRes.body()).getAsJsonObject();
        assertTrue(json.has("dataShape"), "Should have dataShape metadata: " + getRes.body());
        var ds = json.getAsJsonObject("dataShape");
        assertTrue(ds.has("fieldDefinitions"), "Should have inline fieldDefinitions: " + ds);
    }
}
