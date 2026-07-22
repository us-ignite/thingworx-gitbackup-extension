package gb.tests.junit;

import static org.junit.jupiter.api.Assertions.*;

import gb.tests.junit.util.TestingCredentials;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ThingWorxEntitiesTest {

    private static final HttpClient httpClient = HttpClient.newBuilder().build();

    private static final String DB_INIT_IMAGE = System.getProperty("test.dbInitImage",
            "devopscadit/postgresql-init-twx:platform9.6.3");
    private static final String PLATFORM_IMAGE = System.getProperty("test.platformImage",
            "devopscadit/platform-postgres:platform9.6.3");

    private TestingCredentials credentials = new TestingCredentials();

    @Test
    void entitiesTestsForVersion() throws Exception {

        var stack = new GitBackupExtensionTestStack(DB_INIT_IMAGE, PLATFORM_IMAGE, credentials);

        var createReq =
                stack.thingworx
                        .serviceRequest("GitBackup.Tests.Thing", "CreateTestData", "{}")
                        .timeout(Duration.ofMinutes(5))
                        .build();
        var createRes = httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                createRes.statusCode(),
                "CreateTestData HTTP status. Body: " + createRes.body());
        assertNotNull(createRes.body());
        assertTrue(
                createRes.body().contains("tests"),
                "CreateTestData should return a JSON with 'tests' array. Body: "
                        + createRes.body());

        var runReq =
                stack.thingworx
                        .serviceRequest("GitBackup.Tests.Thing", "RunAllTests", "{}")
                        .timeout(Duration.ofMinutes(10))
                        .build();
        var runRes = httpClient.send(runReq, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, runRes.statusCode(), "RunAllTests HTTP status. Body: " + runRes.body());

        assertNotNull(runRes.body(), "RunAllTests response body must not be null");
        assertFalse(
                runRes.body().contains("\"passed\":false"),
                "All Entities tests should pass. Response: " + runRes.body());

        stack.close();
    }
}
