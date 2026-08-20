package org.us_ignite.thingworx.jgit.tests.containers;

import java.nio.file.Files;
import java.nio.file.Path;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;

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
        withEnv("DB_INIT_KEEP_ALIVE", "true");

        Path initScript = Path.of("scripts", "db-init-wrapper.sh").toAbsolutePath();
        if (!Files.exists(initScript)) {
            throw new IllegalStateException(
                    "db-init-wrapper.sh must exist at " + initScript.toAbsolutePath());
        }
        withFileSystemBind(
                initScript.toString(), "/scripts/db-init-wrapper.sh", BindMode.READ_ONLY);

        withCreateContainerCmdModifier(
                cmd -> cmd.withEntrypoint("bash", "/scripts/db-init-wrapper.sh"));
        waitingFor(Wait.forLogMessage(".*DB_INIT_DONE.*", 1));
    }
}
