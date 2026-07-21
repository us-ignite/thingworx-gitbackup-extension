package gb.tests.junit.containers;

import gb.tests.junit.util.TestingCredentials;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class DBInit extends GenericContainer<DBInit> {
    public DBInit(
            String dbInitImage,
            GenericContainer<?> postgres,
            Network network,
            TestingCredentials credentials) {
        this(dbInitImage, postgres, network, credentials, "postgresql");
    }

    public DBInit(
            String dbInitImage,
            GenericContainer<?> postgres,
            Network network,
            TestingCredentials credentials,
            String dbHostAlias) {
        super(dbInitImage);
        withNetwork(network);
        dependsOn(postgres);
        withEnv("DATABASE_ADMIN_USERNAME", credentials.dbAdminUser);
        withEnv("DATABASE_ADMIN_PASSWORD", credentials.dbAdminPass);
        withEnv("DATABASE_ADMIN_SCHEMA", credentials.dbAdminSchema);
        withEnv("DATABASE_HOST", dbHostAlias);
        withEnv("DATABASE_PORT", "5432");
        withEnv("TWX_DATABASE_USERNAME", credentials.twxDatabaseUser);
        withEnv("TWX_DATABASE_SCHEMA", credentials.twxDatabaseSchema);
        withEnv("TWX_DATABASE_PASSWORD", credentials.twxDatabasePass);
        withEnv("TABLESPACE_LOCATION", "/var/lib/postgresql/data");
        withCreateContainerCmdModifier(
                cmd ->
                        cmd.withEntrypoint(
                                "bash",
                                "-c",
                                "/usr/local/bin/db-setup.sh && echo 'DB_INIT_DONE' && sleep infinity"));
        waitingFor(Wait.forLogMessage(".*DB_INIT_DONE.*", 1));
    }
}
