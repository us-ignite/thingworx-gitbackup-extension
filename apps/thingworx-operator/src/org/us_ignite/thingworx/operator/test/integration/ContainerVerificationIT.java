package org.us_ignite.thingworx.operator.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Docker-backed smoke tests for runtime prerequisites; no licence is added to an image layer. */
@Tag("integration")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerVerificationIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                    .withDatabaseName("thingworx")
                    .withUsername("thingworx")
                    .withPassword("test-only-password");

    @Test
    @Order(1)
    void externalPostgresDependencyStartsAndAcceptsConnections() throws Exception {
        assertThat(POSTGRES.isRunning()).isTrue();
        try (var connection = POSTGRES.createConnection("")) {
            assertThat(connection.isValid(5)).isTrue();
        }
    }

    @Test
    @Order(2)
    void platformImageReceivesLicenceOnlyAsARuntimeReadOnlyMount() throws Exception {
        var licence = Path.of("local", "license.bin").toAbsolutePath();
        Assumptions.assumeTrue(Files.isReadable(licence), "local runtime licence is unavailable");
        try (var platform =
                new GenericContainer<>(DockerImageName.parse("thingworx/platform-postgres:latest"))
                        .withFileSystemBind(
                                licence.toString(),
                                "/ThingworxPlatform/license.bin",
                                BindMode.READ_ONLY)
                        .withCreateContainerCmdModifier(
                                command -> command.withEntrypoint("/bin/sh"))
                        .withCommand("-c", "sleep 30")) {
            platform.start();
            var result =
                    platform.execInContainer(
                            "/bin/sh", "-c", "test -s /ThingworxPlatform/license.bin");
            assertThat(result.getExitCode()).isZero();
        }
    }

    @Test
    @Order(3)
    void ptcInitializerBootstrapsAnEmptyPostgresDatabase() throws Exception {
        try (var network = Network.newNetwork();
                var database =
                        new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                                .withNetwork(network)
                                .withNetworkAliases("postgres")
                                .withDatabaseName("postgres")
                                .withUsername("postgres")
                                .withPassword("admin-test-password");
                var initializer =
                        new GenericContainer<>(
                                        DockerImageName.parse(
                                                "thingworx/postgresql-init-twx:latest"))
                                .withNetwork(network)
                                .withEnv("DATABASE_HOST", "postgres")
                                .withEnv("DATABASE_PORT", "5432")
                                .withEnv("DATABASE_ADMIN_USERNAME", "postgres")
                                .withEnv("DATABASE_ADMIN_PASSWORD", "admin-test-password")
                                .withEnv("DATABASE_ADMIN_DBNAME", "postgres")
                                .withEnv("TWX_DATABASE_USERNAME", "thingworx")
                                .withEnv("TWX_DATABASE_PASSWORD", "thingworx-test-password")
                                .withEnv("TWX_DATABASE_DBNAME", "thingworx")
                                .withEnv("TWX_DATABASE_SCHEMA", "thingworx")
                                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())) {
            database.start();
            initializer.start();
            try (var connection =
                            java.sql.DriverManager.getConnection(
                                    "jdbc:postgresql://"
                                            + database.getHost()
                                            + ":"
                                            + database.getMappedPort(5432)
                                            + "/thingworx",
                                    "thingworx",
                                    "thingworx-test-password");
                    var statement = connection.createStatement();
                    var result =
                            statement.executeQuery(
                                    "select count(*) from information_schema.schemata where schema_name = 'thingworx'")) {
                result.next();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
        }
    }
}
