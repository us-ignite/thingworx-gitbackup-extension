package org.us_ignite.thingworx.jgit.tests.containers;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;

public class GiteaRepo extends GenericContainer<GiteaRepo> {
    public GiteaRepo(Network network, TestingCredentials credentials) throws Exception {
        super("gitea/gitea:1.20.4");
        withNetwork(network);
        withNetworkAliases("gitea");
        withEnv("GITEA__security__INSTALL_LOCK", "true");
        withEnv("GITEA__server__DOMAIN", "localhost");
        withEnv("GITEA__server__HTTP_PORT", "3000");
        withEnv("GITEA__server__ROOT_URL", "http://localhost:3000/");
        withEnv("GITEA__database__DB_TYPE", "sqlite3");
        withExposedPorts(3000);
        waitingFor(Wait.forHttp("/").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(3)));
    }
}
