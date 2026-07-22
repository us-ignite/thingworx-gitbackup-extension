package gb.tests.junit;

import static org.junit.jupiter.api.Assertions.*;

import gb.tests.junit.containers.DBInit;
import gb.tests.junit.containers.Postgres;
import gb.tests.junit.containers.ThingWorxContainer;
import gb.tests.junit.util.TestingCredentials;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ThingWorxIntegrationTest {

    public static final HttpClient httpClient = HttpClient.newBuilder().build();

    private static final String DB_INIT_IMAGE =
            System.getProperty("test.dbInitImage", "devopscadit/postgresql-init-twx:platform9.6.3");
    private static final String PLATFORM_IMAGE =
            System.getProperty("test.platformImage", "devopscadit/platform-postgres:platform9.6.3");

    private TestingCredentials credentials = new TestingCredentials();

    void postgresIsRunning() {
        var postgres = new Postgres(null, credentials);
        postgres.start();
        assertTrue(postgres.isRunning(), "PostgreSQL container must be running");
        postgres.close();
    }

    @Test
    void dbInitCompleted() {
        var network = Network.newNetwork();
        var postgres = new Postgres(network, credentials);
        var dbInit = new DBInit(DB_INIT_IMAGE, postgres, network, credentials);
        dbInit.start();
        assertTrue(dbInit.isRunning(), "DB init container must be running");
        dbInit.close();
    }

    @Test
    void thingworxTablesExist() throws Exception {
        var network = Network.newNetwork();
        var postgres = new Postgres(network, credentials);
        var dbInit = new DBInit(DB_INIT_IMAGE, postgres, network, credentials);
        dbInit.start();

        var tables =
                postgres.execInContainer(
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

        dbInit.close();
    }

    @Test
    void platformIsRunning() {
        var network = Network.newNetwork();
        var postgres = new Postgres(network, credentials);
        var dbInit = new DBInit(DB_INIT_IMAGE, postgres, network, credentials);
        var thingworx =
                new ThingWorxContainer(PLATFORM_IMAGE, dbInit, postgres, network, credentials);
        thingworx.start();
        assertTrue(thingworx.isRunning(), "Platform container must be running");
        thingworx.close();
    }

    @Test
    void thingworxHealthCheck() throws Exception {
        var network = Network.newNetwork();
        var postgres = new Postgres(network, credentials);
        var dbInit = new DBInit(DB_INIT_IMAGE, postgres, network, credentials);
        var thingworx =
                new ThingWorxContainer(PLATFORM_IMAGE, dbInit, postgres, network, credentials);
        thingworx.start();
        var req = thingworx.healthCheckRequest();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Health endpoint must return 200");

        thingworx.close();
    }

    @Test
    void installAndVerifyExtension() throws Exception {
        var stack =
                new GitBackupExtensionTestStack(DB_INIT_IMAGE, PLATFORM_IMAGE, credentials, false);
        try {
            stack.installer.start();

            var req =
                    stack.thingworx
                            .serviceRequest("GitBackup.Tests.Thing", "IsExtensionInstalled", "{}")
                            .timeout(Duration.ofMinutes(2))
                            .build();
            var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            assertEquals(
                    200, res.statusCode(), "IsExtensionInstalled HTTP status. Body: " + res.body());
            assertNotNull(res.body(), "IsExtensionInstalled response body must not be null");
        } finally {
            if (stack != null) {
                stack.close();
            }
        }
    }
}
