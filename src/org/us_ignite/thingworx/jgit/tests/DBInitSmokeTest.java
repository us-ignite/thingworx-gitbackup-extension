package org.us_ignite.thingworx.jgit.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.Network;
import org.us_ignite.thingworx.jgit.tests.containers.DBInit;
import org.us_ignite.thingworx.jgit.tests.containers.Postgres;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DBInitSmokeTest {
    private final TestingCredentials credentials = new TestingCredentials();
    private Network network;
    private Postgres postgres;
    private DBInit dbInit;

    @BeforeAll
    void setup() throws Exception {
        network = Network.newNetwork();
        postgres = new Postgres(network, credentials);
        postgres.start();
        dbInit =
                new DBInit(
                        "devopscadit/postgresql-init-twx:platform9.6.3",
                        postgres,
                        network,
                        credentials);
        dbInit.start();
    }

    @AfterAll
    void tearDown() {
        if (dbInit != null) dbInit.close();
        if (postgres != null) postgres.close();
        if (network != null) network.close();
    }

    @Test
    void dbInitRunsWrapperAndStaysAlive() throws Exception {
        assertTrue(dbInit.isRunning(), "DB init container must keep running (KEEP_ALIVE)");
        var dbCheck =
                postgres.execInContainer(
                        "psql",
                        "-U",
                        credentials.dbAdminUser,
                        "-c",
                        "SELECT 1 FROM pg_database WHERE datname='twx'");
        assertTrue(dbCheck.getStdout().contains("1"), "twx database should exist");
    }
}
