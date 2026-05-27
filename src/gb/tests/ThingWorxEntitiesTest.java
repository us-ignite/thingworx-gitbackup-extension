package gb.tests;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Testcontainers;

import gb.tests.util.TestingCredentials;
import gb.tests.util.ThingWorxVersion;

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
 * {@link #entitiesTestsForVersion} can pass, edit that file and fill in the
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


    @ParameterizedTest(name = "entitiesTestsForVersion [{0}]")
    @MethodSource("thingworxVersions")
    void entitiesTestsForVersion(ThingWorxVersion version) throws Exception {

        var stack = new GitBackupExtensionTestStack(version, credentials);

        var createReq = stack.thingworx.serviceRequest(
                "GitBackup.Tests.Thing",
                "CreateTestData",
                "{}").timeout(
                        Duration.ofMinutes(5))
                .build();
        var createRes = httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, createRes.statusCode(), "CreateTestData HTTP status. Body: " + createRes.body());
        assertNotNull(createRes.body());
        assertTrue(
                createRes.body().contains("tests"),
                "CreateTestData should return a JSON with 'tests' array. Body: " + createRes.body());

        var runReq = stack.thingworx.serviceRequest(
                "GitBackup.Tests.Thing",
                "RunAllTests",
                "{}")
                .timeout(
                        Duration.ofMinutes(10))
                .build();
        var runRes = httpClient.send(runReq, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, runRes.statusCode(),
                "RunAllTests HTTP status. Body: " + runRes.body());

        assertNotNull(runRes.body(), "RunAllTests response body must not be null");
        assertFalse(
                runRes.body().contains("\"passed\":false"),
                "All Entities tests should pass. Response: " + runRes.body());

        stack.close();
    }

}
