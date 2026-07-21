package gb.tests.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import gb.tests.junit.util.TestingCredentials;
import gb.tests.junit.util.ThingWorxVersion;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
public class GitBackupValidationIntegrationTest {

    private static final ThingWorxVersion TEST_VERSION =
            new ThingWorxVersion(
                    "9.7.5",
                    "devopscadit/postgresql-init-twx:platform9.7.5",
                    "devopscadit/platform-postgres:platform9.7.5");

    private TestingCredentials credentials;
    private GitBackupExtensionTestStack stack;

    @BeforeAll
    public void beforeAll() throws Exception {
        credentials = new TestingCredentials();
        stack = new GitBackupExtensionTestStack(TEST_VERSION, credentials);
    }

    @AfterAll
    public void afterAll() {
        stack.close();
    }

    private HttpRequest.Builder resourceRequest(
            String resourceName, String serviceName, String body) {
        var baseUrl = stack.thingworx.getExternalUrl();
        var uri =
                URI.create(
                        baseUrl
                                + "/Thingworx/Resources/"
                                + resourceName
                                + "/Services/"
                                + serviceName);
        return HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json;charset=UTF-8")
                .header("Accept", "application/json")
                .header(
                        "Authorization",
                        "Basic "
                                + Base64.getEncoder()
                                        .encodeToString(
                                                (credentials.thingworxAdminUser
                                                                + ":"
                                                                + credentials.thingworxAdminPass)
                                                        .getBytes()))
                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                .header("X-Requested-By", "ThingWorx")
                .POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
    }

    @Test
    void gitBackupValidationResourceExists() throws Exception {
        var req =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        stack.thingworx.getExternalUrl()
                                                + "/Thingworx/Resources/GitBackupValidation"))
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
                                                                .getBytes()))
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .GET()
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                res.statusCode(),
                "GitBackupValidation Resource should be accessible: "
                        + res.statusCode()
                        + " "
                        + res.body());
    }

    @Test
    void checkConfigurationWithValidRepository() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("FileRepository", "GitRepository");
        var req =
                resourceRequest("GitBackupValidation", "CheckConfiguration", body.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                res.statusCode(),
                "CheckConfiguration with valid repo should return 200. Got "
                        + res.statusCode()
                        + ": "
                        + res.body());
        String bodyStr = res.body();
        assertTrue(
                bodyStr.contains("Success"),
                "Should indicate success for valid FileRepository: " + bodyStr);
    }

    @Test
    void checkConfigurationWithInvalidRepository() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("FileRepository", "__non_existent_repo__");
        var req =
                resourceRequest("GitBackupValidation", "CheckConfiguration", body.toString())
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                res.statusCode() != 200 || res.body().contains("Failed"),
                "Invalid FileRepository should fail. Status="
                        + res.statusCode()
                        + " body="
                        + res.body());
    }

    @Test
    void userExtensionPropertiesExistAfterMigration() throws Exception {
        var getInfoReq =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        stack.thingworx.getExternalUrl()
                                                + "/Thingworx/ThingShapes/UserExtensions"))
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
                                                                .getBytes()))
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .GET()
                        .build();
        var getInfoRes = stack.httpClient.send(getInfoReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                getInfoRes.statusCode(),
                "UserExtensions ThingShape should be accessible: " + getInfoRes.statusCode());
        String body = getInfoRes.body();
        assertTrue(
                body.contains("GitCredentials") || body.contains("GpgKeys"),
                "UserExtensions should have GitCredentials or GpgKeys property: " + body);
    }
}
