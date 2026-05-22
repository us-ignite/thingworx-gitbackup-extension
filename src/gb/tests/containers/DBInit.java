package gb.tests.containers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import gb.tests.util.TestingCredentials;

public class DBInit extends GenericContainer<DBInit> {
    public DBInit(String dbInitImage,
            GenericContainer<?> postgres, Network network, TestingCredentials credentials) {
        super(dbInitImage);
        withNetwork(network);
        dependsOn(postgres);
        withEnv("DATABASE_ADMIN_USERNAME", credentials.dbAdminUser);
        withEnv("DATABASE_ADMIN_PASSWORD", credentials.dbAdminPass);
        withEnv("DATABASE_ADMIN_SCHEMA", credentials.dbAdminSchema);
        withEnv("DATABASE_HOST", "postgresql");
        withEnv("DATABASE_PORT", "5432");
        withEnv("TWX_DATABASE_USERNAME", credentials.twxDatabaseUser);
        withEnv("TWX_DATABASE_SCHEMA", credentials.twxDatabaseSchema);
        withEnv("TWX_DATABASE_PASSWORD", credentials.twxDatabasePass);
        withEnv("TABLESPACE_LOCATION", "/var/lib/postgresql/data");
        withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("bash", "-c",
                "/usr/local/bin/db-setup.sh && echo 'DB_INIT_DONE' && sleep infinity"));
        waitingFor(Wait.forLogMessage(".*DB_INIT_DONE.*", 1));
    }
}
