package org.us_ignite.thingworx.testcontainers;

import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class Postgres extends PostgreSQLContainer<Postgres> {
    public Postgres(Network network, TestingCredentials credentials) {
        this(network, credentials, "postgresql");
    }

    public Postgres(Network network, TestingCredentials credentials, String networkAlias) {
        super(
                DockerImageName.parse(TestImages.postgresImage())
                        .asCompatibleSubstituteFor("postgres"));
        withNetwork(network);
        withNetworkAliases(networkAlias);
        withUsername(credentials.dbAdminUser);
        withPassword(credentials.dbAdminPass);
        withDatabaseName(credentials.dbAdminSchema);
        // Prod-faithful SCRAM auth: postgres 15+ defaults to scram-sha-256.
        // For ThingWorx 9.6 which may not support SCRAM, override via
        // -Dtest.postgresImage=postgres:15 -Dtest.postgresHostAuth=md5
        String hostAuth = System.getProperty("test.postgresHostAuth", "scram-sha-256");
        String passwordEncryption =
                System.getProperty("test.postgresPasswordEncryption", "scram-sha-256");
        withEnv("POSTGRES_HOST_AUTH_METHOD", hostAuth);
        withCommand("postgres", "-c", "password_encryption=" + passwordEncryption);
    }
}
