package gb.tests.junit;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;

import gb.tests.junit.containers.DBInit;
import gb.tests.junit.containers.Postgres;
import gb.tests.junit.containers.ThingWorxContainer;
import gb.tests.junit.util.TestingCredentials;
import gb.tests.junit.util.ThingWorxVersion;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class ThingWorxIntegrationTest {

    public static final HttpClient httpClient = HttpClient.newBuilder().build();

    private TestingCredentials credentials = new TestingCredentials();

    public static Stream<ThingWorxVersion> thingworxVersions() {
        return ThingWorxVersion.thingworxVersionsTestMatrix();
    }

    void postgresIsRunning(ThingWorxVersion version) {
        var postgres = new Postgres(null, credentials);
        postgres.start();
        assertTrue(postgres.isRunning(), "PostgreSQL container must be running");
        postgres.close();
    }

    @ParameterizedTest(name = "dbInitCompleted [{0}]")
    @MethodSource("thingworxVersions")
    void dbInitCompleted(ThingWorxVersion version) {
        var network = Network.newNetwork();
        var postgres = new Postgres(network, credentials);
        var dbInit = new DBInit(version.dbInitImage, postgres, network, credentials);
        dbInit.start();
        assertTrue(dbInit.isRunning(), "DB init container must be running");
        dbInit.close();
    }

    @ParameterizedTest(name = "thingworxTablesExist [{0}]")
    @MethodSource("thingworxVersions")
    void thingworxTablesExist(ThingWorxVersion version) throws Exception {
        var network = Network.newNetwork();
        var postgres = new Postgres(network, credentials);
        var dbInit = new DBInit(version.dbInitImage, postgres, network, credentials);
        dbInit.start();

        var tables = postgres.execInContainer("psql", "-U", "postgres", "-d", credentials.twxDatabaseSchema,
                "-c",
                "SELECT table_schema, table_name FROM information_schema.tables WHERE table_type = 'BASE TABLE' AND table_schema NOT IN ('pg_catalog', 'information_schema') ORDER BY table_schema, table_name;");
        assertEquals(0, tables.getExitCode(), "psql query failed: " + tables.getStderr());
        assertFalse(tables.getStdout().isBlank(), "No tables found in " + credentials.twxDatabaseSchema + " database");
        assertTrue(tables.getStdout().contains("thing_model"),
                "Expected ThingWorx tables in " + credentials.twxDatabaseSchema + " database. Got:\n" + tables.getStdout());

        dbInit.close();
    }

    @ParameterizedTest(name = "platformIsRunning [{0}]")
    @MethodSource("thingworxVersions")
    void platformIsRunning(ThingWorxVersion version) {
        var network = Network.newNetwork();
        var postgres = new Postgres(network, credentials);
        var dbInit = new DBInit(version.dbInitImage, postgres, network, credentials);
        var thingworx = new ThingWorxContainer(version.platformImage, dbInit, postgres, network, credentials);
        thingworx.start();
        assertTrue(thingworx.isRunning(), "Platform container must be running");
        thingworx.close();
    }

    @ParameterizedTest(name = "thingworxHealthCheck [{0}]")
    @MethodSource("thingworxVersions")
    void thingworxHealthCheck(ThingWorxVersion version) throws Exception {
        var network = Network.newNetwork();
        var postgres = new Postgres(network, credentials);
        var dbInit = new DBInit(version.dbInitImage, postgres, network, credentials);
        var thingworx = new ThingWorxContainer(version.platformImage, dbInit, postgres, network, credentials);
        thingworx.start();
        var req = thingworx.healthCheckRequest();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Health endpoint must return 200");
        
        thingworx.close();
    }

    @ParameterizedTest(name = "installAndVerifyExtension [{0}]")
    @MethodSource("thingworxVersions")
    void installAndVerifyExtension(ThingWorxVersion version) throws Exception {
        var stack = new GitBackupExtensionTestStack(version, credentials, false);
        try {
            stack.installer.start();

            var req = stack.thingworx.serviceRequest("GitBackup.Tests.Thing", "IsExtensionInstalled", "{}")
                    .timeout(Duration.ofMinutes(2)).build();
            var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, res.statusCode(), "IsExtensionInstalled HTTP status. Body: " + res.body());
            assertNotNull(res.body(), "IsExtensionInstalled response body must not be null");
        } finally {
            if (stack != null) {
                stack.close();
            }
        }
    }
}
