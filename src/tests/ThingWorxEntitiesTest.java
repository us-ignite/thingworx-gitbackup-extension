import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Testcontainers;

import util.TestingCredentials;
import util.ThingWorxVersion;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Invokes the Entity-defined JavaScript tests (GitBackup.Tests.Thing) via the
 * ThingWorx REST API, using the same test container infrastructure as
 * ThingWorxIntegrationTest and GiteaGitOperationsTest.
 *
 * The Entities tests run inside the ThingWorx platform. They are called through
 * the ThingWorx REST API via the Services on GitBackup.Tests.Thing.
 *
 * === Configuration Required ===
 * The {@code CreateTestData} service in
 * {@code Entities/Things_GitBackup.Tests.Thing.xml} has "UPDATE" placeholders
 * for GitHub credentials (URL, user, token, etc.). Before
 * {@link #invokeEntitiesTestService} can pass, edit that file and fill in the
 * actual values:
 * <ul>
 * <li>GITTestRepoURL - your GitHub test repo URL</li>
 * <li>GitUser - your GitHub username</li>
 * <li>GitPassword - a GitHub Personal Access Token</li>
 * <li>GitCommitterName / GitComitterEmail - commit author info</li>
 * <li>GITTestRepoURLCommits - the GitHub API commits URL</li>
 * </ul>
 *
 * Alternatively, manually invoke individual test services via REST as shown in
 * {@link GiteaGitOperationsTest}.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ThingWorxEntitiesTest {

    private static final HttpClient httpClient = HttpClient.newBuilder().build();

    public static Stream<ThingWorxVersion> thingworxVersions() {
        return ThingWorxVersion.thingworxVersionsTestMatrix();
    }

    private TestingCredentials credentials = new TestingCredentials();
    private GitBackupExtensionTestStack stack;

    @BeforeAll
    public void beforeAll() throws Exception {
        stack = new GitBackupExtensionTestStack(new ThingWorxVersion("9.7.5",
                "devopscadit/postgresql-init-twx:platform9.7.5",
                "devopscadit/platform-postgres:platform9.7.5"), credentials);
    }

    @AfterAll
    public void afterAll() {
        stack.close();
    }

    @ParameterizedTest(name = "createTestDataReturnsConfig [{0}]")
    @MethodSource("thingworxVersions")
    @Order(1)
    void createTestDataReturnsConfig(ThingWorxVersion version) throws Exception {

        var req = stack.thingworx.serviceRequest(
                "GitBackup.Tests.Thing",
                "CreateTestData",
                "{}").timeout(
                        Duration.ofMinutes(5))
                .build();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "CreateTestData HTTP status. Body: " + res.body());
        assertNotNull(res.body());
        assertTrue(
                res.body().contains("tests"),
                "CreateTestData should return a JSON with 'tests' array. Body: " + res.body());
    }

    @ParameterizedTest(name = "invokeEntitiesTestService [{0}]")
    @MethodSource("thingworxVersions")
    @Order(2)
    void invokeEntitiesTestService(ThingWorxVersion version) throws Exception {

        var reqest = stack.thingworx.serviceRequest(
                "GitBackup.Tests.Thing",
                "RunAllTests",
                "{}")
                .timeout(
                        Duration.ofMinutes(10))
                .build();
        var res = httpClient.send(reqest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, res.statusCode(),
                "RunAllTests HTTP status. Body: " + res.body());

        assertNotNull(res.body(), "RunAllTests response body must not be null");
        assertFalse(
                res.body().contains("\"passed\":false"),
                "All Entities tests should pass. Response: " + res.body());
    }

}
