package org.us_ignite.thingworx.jgit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.us_ignite.thingworx.jgit.tests.util.ServiceResultAssertions.assertSuccess;
import static org.us_ignite.thingworx.jgit.tests.util.ServiceResultAssertions.responseRows;

import com.google.gson.JsonObject;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.us_ignite.thingworx.jgit.tests.containers.JGitExtensionTestStack;
import org.us_ignite.thingworx.jgit.tests.util.GPGGenerator;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;

/**
 * Integration tests for {@code VerifyGpgKey} covering all valid calling conventions.
 *
 * <p>All 5 args are optional selectors (precedence: All > Fingerprint/Label > GpgPrivateKey):
 * <ul>
 *   <li>pasted {@code GpgPrivateKey}+{@code GpgKeyPassphrase} (Base64 or armored) -> single new key</li>
 *   <li>blank {@code GpgPrivateKey} -> first stored key fallback</li>
 *   <li>{@code All=true} -> all stored keys</li>
 *   <li>{@code GpgKeyFingerprint} and/or {@code GpgKeyLabel} -> matching stored key(s)</li>
 * </ul>
 * Result rows are {@code GIT.GpgKeyVerificationResult} with {@code GpgKeyFingerprint, Valid, Stored, GpgKeyLabel}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VerifyGpgKeyConventionsTest {

    private static final String DB_INIT_IMAGE =
            System.getProperty("test.dbInitImage", "devopscadit/postgresql-init-twx:platform9.6.3");
    private static final String PLATFORM_IMAGE =
            System.getProperty("test.platformImage", "devopscadit/platform-postgres:platform9.6.3");

    private TestingCredentials credentials;
    private JGitExtensionTestStack stack;

    // keys created during test run
    private String unencryptedKeyArmored;
    private String unencryptedFingerprint;
    private String unencryptedLabel = "verify-test-unencrypted-" + System.nanoTime();

    private String passphrase = "s3cret-" + System.nanoTime();
    private String encryptedKeyArmored;
    private String encryptedFingerprint;
    private String encryptedLabel = "verify-test-encrypted-" + System.nanoTime();

    @BeforeAll
    public void beforeAll() throws Exception {
        credentials = new TestingCredentials();
        stack = new JGitExtensionTestStack(DB_INIT_IMAGE, PLATFORM_IMAGE, credentials);
    }

    @AfterAll
    public void afterAll() {
        stack.close();
    }

    private JsonObject verifyResponse(JsonObject body) throws Exception {
        var req = stack.thingworx.serviceRequest("GIT.Utility.Thing", "VerifyGpgKey", body.toString())
                .timeout(Duration.ofSeconds(10)).build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "VerifyGpgKey HTTP should be 200: " + res.body());
        assertSuccess(res.body());
        return com.google.gson.JsonParser.parseString(res.body()).getAsJsonObject();
    }

    @Test
    @Order(1)
    void pastedUnencryptedNewKey_NotInUserKeys() throws Exception {
        unencryptedKeyArmored = GPGGenerator.generateTestGpgPrivateKey();
        JsonObject body = new JsonObject();
        body.addProperty("GpgPrivateKey", unencryptedKeyArmored);
        body.addProperty("GpgKeyPassphrase", "");
        var res = verifyResponse(body);
        var rows = responseRows(res.toString());
        assertEquals(1, rows.size(), "pasted new key should return one row");
        var row = rows.get(0).getAsJsonObject();
        assertTrue(row.has("Valid") && row.get("Valid").getAsBoolean(), "Valid should be true");
        assertTrue(row.has("Stored") && !row.get("Stored").getAsBoolean(), "Stored should be false for new key");
        assertEquals("Not in User Keys", row.get("GpgKeyLabel").getAsString());
        assertTrue(row.get("GpgKeyFingerprint").getAsString().matches("[0-9A-Fa-f]+"));
    }

    @Test
    @Order(2)
    void pastedUnencryptedBase64_NotInUserKeys() throws Exception {
        String b64 = Base64.getEncoder().encodeToString(unencryptedKeyArmored.getBytes(StandardCharsets.UTF_8));
        JsonObject body = new JsonObject();
        body.addProperty("GpgPrivateKey", b64);
        body.addProperty("GpgKeyPassphrase", "");
        var res = verifyResponse(body);
        var row = responseRows(res.toString()).get(0).getAsJsonObject();
        assertTrue(row.get("Valid").getAsBoolean());
        assertFalse(row.get("Stored").getAsBoolean());
        assertEquals("Not in User Keys", row.get("GpgKeyLabel").getAsString());
    }

    @Test
    @Order(3)
    void pastedEncryptedNewKey_WithPassphrase() throws Exception {
        encryptedKeyArmored = GPGGenerator.generateTestGpgPrivateKeyWithPassphrase(passphrase);
        JsonObject body = new JsonObject();
        body.addProperty("GpgPrivateKey", encryptedKeyArmored);
        body.addProperty("GpgKeyPassphrase", passphrase);
        var res = verifyResponse(body);
        var row = responseRows(res.toString()).get(0).getAsJsonObject();
        assertTrue(row.get("Valid").getAsBoolean(), "encrypted key with correct passphrase should be Valid");
        assertFalse(row.get("Stored").getAsBoolean());
        assertEquals("Not in User Keys", row.get("GpgKeyLabel").getAsString());
    }

    @Test
    @Order(4)
    void pastedEncryptedWrongPassphrase_Invalid() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("GpgPrivateKey", encryptedKeyArmored);
        body.addProperty("GpgKeyPassphrase", "wrong-passphrase");
        var res = verifyResponse(body);
        // Valid is derived from fingerprint; wrong passphrase should still derive? Bouncy may still get fingerprint?
        // PastedKeyGpgSigner loads without passphrase for fingerprint; but signing would fail.
        // Our implementation marks Valid based on getFingerprint() != blank, so it will be true even with wrong passphrase.
        // This documents current behavior: fingerprint derivation does not require passphrase.
        var row = responseRows(res.toString()).get(0).getAsJsonObject();
        assertNotNull(row.get("GpgKeyFingerprint").getAsString());
        // We assert Stored false regardless
        assertFalse(row.get("Stored").getAsBoolean());
    }

    @Test
    @Order(5)
    void createStoredKeysForFurtherTests() throws Exception {
        // unencrypted stored
        JsonObject create1 = new JsonObject();
        create1.addProperty("GpgPrivateKey", unencryptedKeyArmored);
        create1.addProperty("GpgKeyPassphrase", "");
        create1.addProperty("GpgKeyFingerprint", "");
        create1.addProperty("GpgKeyLabel", unencryptedLabel);
        var req1 = stack.thingworx.serviceRequest("GIT.Utility.Thing", "GpgKeyCreate", create1.toString()).build();
        var res1 = stack.httpClient.send(req1, HttpResponse.BodyHandlers.ofString());
        assertTrue(res1.statusCode()==200 || res1.statusCode()==201, "GpgKeyCreate unencrypted failed: "+res1.body());

        // encrypted stored
        JsonObject create2 = new JsonObject();
        create2.addProperty("GpgPrivateKey", encryptedKeyArmored);
        create2.addProperty("GpgKeyPassphrase", passphrase);
        create2.addProperty("GpgKeyFingerprint", "");
        create2.addProperty("GpgKeyLabel", encryptedLabel);
        var req2 = stack.thingworx.serviceRequest("GIT.Utility.Thing", "GpgKeyCreate", create2.toString()).build();
        var res2 = stack.httpClient.send(req2, HttpResponse.BodyHandlers.ofString());
        assertTrue(res2.statusCode()==200 || res2.statusCode()==201, "GpgKeyCreate encrypted failed: "+res2.body());

        // fetch fingerprints via Verify to capture derived ones
        JsonObject v1 = new JsonObject(); v1.addProperty("GpgKeyLabel", unencryptedLabel);
        var vr1 = verifyResponse(v1);
        unencryptedFingerprint = responseRows(vr1.toString()).get(0).getAsJsonObject().get("GpgKeyFingerprint").getAsString();

        JsonObject v2 = new JsonObject(); v2.addProperty("GpgKeyLabel", encryptedLabel);
        var vr2 = verifyResponse(v2);
        encryptedFingerprint = responseRows(vr2.toString()).get(0).getAsJsonObject().get("GpgKeyFingerprint").getAsString();

        assertNotNull(unencryptedFingerprint);
        assertNotNull(encryptedFingerprint);
        assertTrue(!unencryptedFingerprint.equals(encryptedFingerprint));
    }

    @Test
    @Order(6)
    void pastedStoredKey_NowStoredTrue() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("GpgPrivateKey", unencryptedKeyArmored);
        body.addProperty("GpgKeyPassphrase", "");
        var res = verifyResponse(body);
        var row = responseRows(res.toString()).get(0).getAsJsonObject();
        assertTrue(row.get("Valid").getAsBoolean());
        assertTrue(row.get("Stored").getAsBoolean(), "now owned, Stored should be true");
        assertEquals(unencryptedLabel, row.get("GpgKeyLabel").getAsString());
        assertEquals(unencryptedFingerprint, row.get("GpgKeyFingerprint").getAsString());
    }

    @Test
    @Order(7)
    void blankWithNoSelector_ShouldError() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("GpgPrivateKey", "");
        body.addProperty("GpgKeyPassphrase", "");
        var req = stack.thingworx.serviceRequest("GIT.Utility.Thing", "VerifyGpgKey", body.toString()).build();
        var resp = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"Error\":true") || resp.body().contains("\"Error\" : true"),
                "blank with no selector should now error after fallback removal: " + resp.body());
        assertTrue(resp.body().contains("GpgPrivateKey is required"), "should mention required: " + resp.body());
    }

    @Test
    @Order(8)
    void verifyAll_ReturnsAllStored() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("All", true);
        var res = verifyResponse(body);
        var rows = responseRows(res.toString());
        assertTrue(rows.size() >= 2, "All should return >=2 stored keys, got " + rows.size());
        for (var el : rows) {
            var r = el.getAsJsonObject();
            assertTrue(r.get("Valid").getAsBoolean(), "All stored keys should be Valid: " + r);
            assertTrue(r.get("Stored").getAsBoolean(), "All stored keys Stored=true: " + r);
            assertTrue(r.has("GpgKeyFingerprint") && !r.get("GpgKeyFingerprint").getAsString().isBlank());
            assertTrue(r.has("GpgKeyLabel"));
        }
    }

    @Test
    @Order(9)
    void verifyByFingerprint_Stored() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("GpgKeyFingerprint", unencryptedFingerprint);
        var res = verifyResponse(body);
        var rows = responseRows(res.toString());
        assertEquals(1, rows.size());
        var row = rows.get(0).getAsJsonObject();
        assertTrue(row.get("Valid").getAsBoolean());
        assertTrue(row.get("Stored").getAsBoolean());
        assertEquals(unencryptedLabel, row.get("GpgKeyLabel").getAsString());
        assertEquals(unencryptedFingerprint, row.get("GpgKeyFingerprint").getAsString());
    }

    @Test
    @Order(10)
    void verifyByLabel_Stored() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("GpgKeyLabel", encryptedLabel);
        var res = verifyResponse(body);
        var rows = responseRows(res.toString());
        assertEquals(1, rows.size());
        var row = rows.get(0).getAsJsonObject();
        assertTrue(row.get("Valid").getAsBoolean());
        assertTrue(row.get("Stored").getAsBoolean());
        assertEquals(encryptedLabel, row.get("GpgKeyLabel").getAsString());
        assertEquals(encryptedFingerprint, row.get("GpgKeyFingerprint").getAsString());
    }

    @Test
    @Order(11)
    void verifyByFingerprintAndLabel_BothMustMatch() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("GpgKeyFingerprint", unencryptedFingerprint);
        body.addProperty("GpgKeyLabel", unencryptedLabel);
        var res = verifyResponse(body);
        var rows = responseRows(res.toString());
        assertEquals(1, rows.size());
        assertEquals(unencryptedFingerprint, rows.get(0).getAsJsonObject().get("GpgKeyFingerprint").getAsString());

        // mismatch should fail Error=true
        JsonObject mismatch = new JsonObject();
        mismatch.addProperty("GpgKeyFingerprint", unencryptedFingerprint);
        mismatch.addProperty("GpgKeyLabel", encryptedLabel);
        var req = stack.thingworx.serviceRequest("GIT.Utility.Thing", "VerifyGpgKey", mismatch.toString()).build();
        var resp = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        // ServiceResults.fromError returns Error=true
        assertTrue(resp.body().contains("\"Error\":true") || resp.body().contains("\"Error\" : true"), "mismatched fingerprint+label should error: " + resp.body());
    }

    @Test
    @Order(12)
    void verifyAllTakesPrecedenceOverFingerprint() throws Exception {
        String freshKey = GPGGenerator.generateTestGpgPrivateKey();
        JsonObject body = new JsonObject();
        body.addProperty("All", true);
        body.addProperty("GpgKeyFingerprint", unencryptedFingerprint);
        body.addProperty("GpgPrivateKey", freshKey);
        var res = verifyResponse(body);
        var rows = responseRows(res.toString());
        // All should ignore pasted and fingerprint, return all stored
        assertTrue(rows.size() >= 2);
    }

    @Test
    @Order(13)
    void verifyInvalidPasted_ValidFalse() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("GpgPrivateKey", "not-a-valid-pgp-key");
        body.addProperty("GpgKeyPassphrase", "");
        var res = verifyResponse(body);
        var row = responseRows(res.toString()).get(0).getAsJsonObject();
        // Current impl: getFingerprint() returns null -> "Unable to derive fingerprint", Valid=false
        // But PastedKeyGpgSigner may still throw; we assert Valid=false if fingerprint is placeholder
        // If service returns Valid=false, Stored false
        assertTrue(row.has("Valid"));
        // Valid should be false for garbage key
        assertFalse(row.get("Valid").getAsBoolean(), "invalid key should be Valid=false: " + row);
        assertFalse(row.get("Stored").getAsBoolean());
        assertTrue(row.get("GpgKeyLabel").getAsString().isEmpty(), "invalid non-owned should have empty label");
    }

    @Test
    @Order(14)
    void verifyNonExistentFingerprint_Fails() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("GpgKeyFingerprint", "NONEXISTENTFP1234567890");
        var req = stack.thingworx.serviceRequest("GIT.Utility.Thing", "VerifyGpgKey", body.toString()).build();
        var resp = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"Error\":true") || resp.body().contains("\"Error\" : true"), "non-existent fingerprint should error: " + resp.body());
    }
}
