package org.us_ignite.thingworx.jgit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.Network;
import org.us_ignite.thingworx.jgit.tests.containers.GiteaInit;
import org.us_ignite.thingworx.jgit.tests.containers.GiteaRepo;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GiteaInitSmokeTest {
    private final TestingCredentials credentials = new TestingCredentials();
    private Network network;
    private GiteaRepo gitea;
    private GiteaInit giteaInit;

    @BeforeAll
    void setup() throws Exception {
        network = Network.newNetwork();
        gitea = new GiteaRepo(network, credentials);
        gitea.start();
        giteaInit = new GiteaInit(gitea, network, credentials);
        giteaInit.start();
    }

    @AfterAll
    void tearDown() {
        if (giteaInit != null) giteaInit.close();
        if (gitea != null) gitea.close();
        if (network != null) network.close();
    }

    @Test
    void userAndRepoExist() throws Exception {
        String auth =
                "Basic "
                        + Base64.getEncoder()
                                .encodeToString(
                                        (credentials.giteaUser + ":" + credentials.giteaPass)
                                                .getBytes());
        HttpClient client = HttpClient.newHttpClient();

        var userReq =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        "http://"
                                                + gitea.getHost()
                                                + ":"
                                                + gitea.getMappedPort(3000)
                                                + "/api/v1/user"))
                        .header("Authorization", auth)
                        .GET()
                        .build();
        var userRes = client.send(userReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, userRes.statusCode(), "admin user should exist: " + userRes.body());

        var repoReq =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        "http://"
                                                + gitea.getHost()
                                                + ":"
                                                + gitea.getMappedPort(3000)
                                                + "/api/v1/repos/"
                                                + credentials.giteaUser
                                                + "/"
                                                + credentials.repoName))
                        .header("Authorization", auth)
                        .GET()
                        .build();
        var repoRes = client.send(repoReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(
                200,
                repoRes.statusCode(),
                "test repo should exist and be readable: " + repoRes.body());
    }
}
