package org.us_ignite.thingworx.jgit.tests.containers;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Base64;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;

public class GiteaRepo extends GenericContainer<GiteaRepo> {
    TestingCredentials credentials;

    public GiteaRepo(Network network, TestingCredentials credentials) throws Exception {
        super("gitea/gitea:1.20.4");
        this.credentials = credentials;
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

    public void setupAdminAndRepo(String giteaUser, String giteaPass, String repoName)
            throws Exception {
        execInContainer(
                "gitea",
                "admin",
                "user",
                "create",
                "--username",
                giteaUser,
                "--password",
                giteaPass,
                "--email",
                "admin@example.com",
                "--admin",
                "--must-change-password=false");
        execInContainer(
                "gitea",
                "admin",
                "repo",
                "create",
                "--name",
                repoName,
                "--owner",
                giteaUser,
                "--auto-init=false",
                "--private=false");
    }

    public HttpRequest.Builder apiRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://gitea:3000" + path))
                .header("Content-Type", "application/json")
                .header(
                        "Authorization",
                        "Basic "
                                + Base64.getEncoder()
                                        .encodeToString(
                                                (credentials.giteaUser
                                                                + ":"
                                                                + credentials.giteaPass)
                                                        .getBytes()));
    }
}
