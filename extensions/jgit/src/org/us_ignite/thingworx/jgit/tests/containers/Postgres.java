package org.us_ignite.thingworx.jgit.tests.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;

public class Postgres extends GenericContainer<Postgres> {
    public Postgres(Network network, TestingCredentials credentials) {
        this(network, credentials, "postgresql");
    }

    public Postgres(Network network, TestingCredentials credentials, String networkAlias) {
        super("postgres:latest");
        withNetwork(network);
        withNetworkAliases(networkAlias);
        withEnv("POSTGRES_USER", credentials.dbAdminUser);
        withEnv("POSTGRES_PASSWORD", credentials.dbAdminPass);
        withEnv("POSTGRES_DB", credentials.dbAdminSchema);
        waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));
    }
}
