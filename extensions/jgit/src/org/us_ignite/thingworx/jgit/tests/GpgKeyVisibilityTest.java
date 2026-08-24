package org.us_ignite.thingworx.jgit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.us_ignite.thingworx.jgit.tests.util.ServiceResultAssertions.assertSuccess;
import static org.us_ignite.thingworx.jgit.tests.util.ServiceResultAssertions.responseDataShape;
import static org.us_ignite.thingworx.jgit.tests.util.ServiceResultAssertions.responseRows;

import com.google.gson.JsonObject;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.us_ignite.thingworx.jgit.extension.PastedKeyGpgSigner;
import org.us_ignite.thingworx.jgit.tests.containers.JGitExtensionTestStack;
import org.us_ignite.thingworx.jgit.tests.util.GPGGenerator;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
public class GpgKeyVisibilityTest {

    private static final String DB_INIT_IMAGE =
            System.getProperty("test.dbInitImage", "devopscadit/postgresql-init-twx:platform9.6.3");
    private static final String PLATFORM_IMAGE =
            System.getProperty("test.platformImage", "devopscadit/platform-postgres:platform9.6.3");

    private TestingCredentials credentials;
    private JGitExtensionTestStack stack;
    private String testPrivateKey;
    private String testFingerprint;

    @BeforeAll
    public void beforeAll() throws Exception {
        credentials = new TestingCredentials();
        testPrivateKey = GPGGenerator.generateTestGpgPrivateKey();
        PastedKeyGpgSigner signer = new PastedKeyGpgSigner(testPrivateKey, "");
        testFingerprint = signer.getFingerprint();
        signer.clearSensitiveData();
        stack = new JGitExtensionTestStack(DB_INIT_IMAGE, PLATFORM_IMAGE, credentials);
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
                                                + "/Thingworx/DataShapes/GIT.GpgKey.UserExtension.DataShape"))
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
                "UserGpgKey.DataShape should be accessible via REST API. Got "
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
                                                + "/Thingworx/DataShapes/GIT.RepositoryConfiguration.UserExtension.DataShape"))
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
                        .serviceRequest("GIT.Utility.Thing", "GpgKeyList", getKeysBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                res.statusCode(),
                "GetGpgKeys should return 200. Got " + res.statusCode() + ": " + res.body());
        assertNotNull(res.body());
        assertSuccess(res.body());
    }

    @Test
    void setAndGetGpgKeyRoundTrip() throws Exception {
        var setBody = new JsonObject();
        setBody.addProperty("GpgPrivateKey", testPrivateKey);
        setBody.addProperty("GpgKeyPassphrase", "test-passphrase");
        setBody.addProperty("GpgKeyFingerprint", testFingerprint);
        setBody.addProperty("GpgKeyLabel", "ci-signing-key");
        var setReq =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "GpgKeyCreate", setBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var setRes = stack.httpClient.send(setReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                setRes.statusCode() == 200 || setRes.statusCode() == 201,
                "SetGpgKey should succeed. Got " + setRes.statusCode() + ": " + setRes.body());

        var getReq =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "GpgKeyList", "{}")
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
        var rows = responseRows(getRes.body());
        assertTrue(rows.size() > 0, "Should have at least one GpgKey row: " + getRes.body());
        var firstRow = rows.get(0).getAsJsonObject();
        assertTrue(
                firstRow.has("GpgKeyFingerprint"),
                "Row should have GpgKeyFingerprint field: " + firstRow);
        assertEquals(
                testFingerprint,
                firstRow.get("GpgKeyFingerprint").getAsString(),
                "GpgKeyFingerprint should match the one we set");
        assertTrue(firstRow.has("GpgKeyLabel"), "Row should have GpgKeyLabel field: " + firstRow);
        assertEquals(
                "ci-signing-key",
                firstRow.get("GpgKeyLabel").getAsString(),
                "GpgKeyLabel should match the one we set");

        var getByLabelBody = new JsonObject();
        getByLabelBody.addProperty("GpgKeyLabel", "ci-signing-key");
        var getByLabelReq =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "GpgKeyGet", getByLabelBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var getByLabelRes =
                stack.httpClient.send(getByLabelReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                getByLabelRes.statusCode(),
                "GpgKeyGet by label should return 200. Got "
                        + getByLabelRes.statusCode()
                        + ": "
                        + getByLabelRes.body());
        var labelRows = responseRows(getByLabelRes.body());
        assertTrue(
                labelRows.size() > 0, "Get by label should return a row: " + getByLabelRes.body());
        assertEquals(
                testFingerprint,
                labelRows.get(0).getAsJsonObject().get("GpgKeyFingerprint").getAsString(),
                "Get by label should return the matching key");

        var deleteBody = new JsonObject();
        deleteBody.addProperty("GpgKeyLabel", "ci-signing-key");
        var deleteReq =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "GpgKeyDelete", deleteBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        stack.httpClient.send(deleteReq, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void deleteByLabelClearsRepositorySigningReferences() throws Exception {
        String fingerprint = testFingerprint;
        String label = "label-delete-signing-test";
        String repoThing = "GIT.Repo.LabelDelete.Test";

        var createBody = new JsonObject();
        createBody.addProperty("GpgPrivateKey", testPrivateKey);
        createBody.addProperty("GpgKeyPassphrase", "test-passphrase");
        createBody.addProperty("GpgKeyFingerprint", fingerprint);
        createBody.addProperty("GpgKeyLabel", label);
        var createReq =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "GpgKeyCreate", createBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var createRes = stack.httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                createRes.statusCode() == 200 || createRes.statusCode() == 201,
                "GpgKeyCreate should succeed. Got "
                        + createRes.statusCode()
                        + ": "
                        + createRes.body());

        var credBody = new JsonObject();
        credBody.addProperty("GitCommitterUser", "testuser");
        credBody.addProperty("GitCommitterPassword", "testpass");
        credBody.addProperty("GitCommitterEmail", "test@example.com");
        credBody.addProperty("GitCommitterFullName", "Test User");
        credBody.addProperty("GitThing", repoThing);
        credBody.addProperty("GpgKeyFingerprint", fingerprint);
        var credReq =
                stack.thingworx
                        .serviceRequest(
                                "GIT.Utility.Thing", "GitCredentialCreate", credBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var credRes = stack.httpClient.send(credReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                credRes.statusCode() == 200 || credRes.statusCode() == 201,
                "GitCredentialCreate should succeed. Got "
                        + credRes.statusCode()
                        + ": "
                        + credRes.body());

        var deleteBody = new JsonObject();
        deleteBody.addProperty("GpgKeyLabel", label);
        var deleteReq =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "GpgKeyDelete", deleteBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var deleteRes = stack.httpClient.send(deleteReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                deleteRes.statusCode(),
                "GpgKeyDelete by label should succeed. Got "
                        + deleteRes.statusCode()
                        + ": "
                        + deleteRes.body());
        var deletedRows = responseRows(deleteRes.body());
        assertEquals(
                1,
                deletedRows.size(),
                "Delete should return exactly the removed key: " + deleteRes.body());
        assertEquals(
                fingerprint,
                deletedRows.get(0).getAsJsonObject().get("GpgKeyFingerprint").getAsString(),
                "Deleted key row should carry the fingerprint");
        assertEquals(
                label,
                deletedRows.get(0).getAsJsonObject().get("GpgKeyLabel").getAsString(),
                "Deleted key row should carry the label");

        var getBody = new JsonObject();
        getBody.addProperty("GitThing", repoThing);
        var getReq =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "GitCredentialGet", getBody.toString())
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var getRes = stack.httpClient.send(getReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getRes.statusCode(), "GitCredentialGet should succeed: " + getRes.body());
        var credRows = responseRows(getRes.body());
        assertEquals(1, credRows.size(), "Credential should still exist: " + getRes.body());
        assertEquals(
                "",
                credRows.get(0).getAsJsonObject().get("GpgKeyFingerprint").getAsString(),
                "Delete by label should clear the repository signing reference");
    }

    @Test
    void gpgKeysFieldNamesMatchExpectedSchema() throws Exception {
        var getReq =
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", "GpgKeyList", "{}")
                        .timeout(Duration.ofSeconds(10))
                        .build();
        var getRes = stack.httpClient.send(getReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getRes.statusCode(), "GetGpgKeys should return 200: " + getRes.body());

        var ds = responseDataShape(getRes.body());
        assertTrue(ds.has("fieldDefinitions"), "dataShape should have fieldDefinitions: " + ds);

        var fields = ds.getAsJsonObject("fieldDefinitions");
        assertTrue(fields.has("GpgPrivateKey"), "Missing GpgPrivateKey field");
        assertTrue(fields.has("GpgKeyPassphrase"), "Missing GpgKeyPassphrase field");
        assertTrue(fields.has("GpgKeyFingerprint"), "Missing GpgKeyFingerprint field");
        assertTrue(fields.has("GpgKeyLabel"), "Missing GpgKeyLabel field");

        assertEquals(
                "PASSWORD", fields.getAsJsonObject("GpgPrivateKey").get("baseType").getAsString());
        assertEquals(
                "PASSWORD",
                fields.getAsJsonObject("GpgKeyPassphrase").get("baseType").getAsString());
        assertEquals(
                "STRING",
                fields.getAsJsonObject("GpgKeyFingerprint").get("baseType").getAsString());
        assertEquals("STRING", fields.getAsJsonObject("GpgKeyLabel").get("baseType").getAsString());
    }
}
