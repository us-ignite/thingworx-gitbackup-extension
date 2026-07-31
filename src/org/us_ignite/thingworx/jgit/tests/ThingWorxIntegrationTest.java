package org.us_ignite.thingworx.jgit.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
public class ThingWorxIntegrationTest {

    public static final HttpClient httpClient = HttpClient.newBuilder().build();

    private static final String DB_INIT_IMAGE =
            System.getProperty("test.dbInitImage", "devopscadit/postgresql-init-twx:platform9.6.3");
    private static final String PLATFORM_IMAGE =
            System.getProperty("test.platformImage", "devopscadit/platform-postgres:platform9.6.3");

    private TestingCredentials credentials;
    private JGitExtensionTestStack stack;

    @BeforeAll
    public void beforeAll() throws Exception {
        credentials = new TestingCredentials();
        stack = new JGitExtensionTestStack(DB_INIT_IMAGE, PLATFORM_IMAGE, credentials, false);
    }

    @AfterAll
    public void afterAll() {
        if (stack != null) stack.close();
    }

    @Test
    void postgresIsRunning() {
        assertTrue(stack.postgres.isRunning(), "PostgreSQL container must be running");
    }

    @Test
    void dbInitCompleted() {
        assertTrue(stack.dbInit.isRunning(), "DB init container must be running");
    }

    @Test
    void thingworxTablesExist() throws Exception {
        var tables =
                stack.postgres.execInContainer(
                        "psql",
                        "-U",
                        "postgres",
                        "-d",
                        credentials.twxDatabaseSchema,
                        "-c",
                        "SELECT table_schema, table_name FROM information_schema.tables WHERE table_type = 'BASE TABLE' AND table_schema NOT IN ('pg_catalog', 'information_schema') ORDER BY table_schema, table_name;");
        assertEquals(0, tables.getExitCode(), "psql query failed: " + tables.getStderr());
        assertFalse(
                tables.getStdout().isBlank(),
                "No tables found in " + credentials.twxDatabaseSchema + " database");
        assertTrue(
                tables.getStdout().contains("thing_model"),
                "Expected ThingWorx tables in "
                        + credentials.twxDatabaseSchema
                        + " database. Got:\n"
                        + tables.getStdout());
    }

    @Test
    void platformIsRunning() {
        assertTrue(stack.thingworx.isRunning(), "Platform container must be running");
    }

    @Test
    void thingworxHealthCheck() throws Exception {
        var req = stack.thingworx.healthCheckRequest();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Health endpoint must return 200");
    }

    @Test
    void installAndVerifyExtension() throws Exception {
        var baseUrl =
                stack.thingworx.getExternalUrl() != null
                        ? stack.thingworx.getExternalUrl()
                        : "http://thingworx:8080";
        var req =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/Thingworx/Things/GIT.Utility.Thing"))
                        .header("Accept", "application/json")
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
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
                        .GET()
                        .timeout(Duration.ofMinutes(2))
                        .build();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "GIT.Utility.Thing GET status. Body: " + res.body());
        assertNotNull(res.body(), "GIT.Utility.Thing response body must not be null");
    }
}
