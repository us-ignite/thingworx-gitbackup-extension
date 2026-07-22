package gb.tests.junit;

import gb.tests.junit.containers.DBInit;
import gb.tests.junit.containers.GitBackupExtensionInstaller;
import gb.tests.junit.containers.GiteaRepo;
import gb.tests.junit.containers.Postgres;
import gb.tests.junit.containers.ThingWorxContainer;
import gb.tests.junit.util.TestingCredentials;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.Base64;
import org.testcontainers.containers.Network;

public class GitBackupExtensionTestStack implements AutoCloseable {
    public final Network network;
    public final Postgres postgres;
    public final DBInit dbInit;
    public final GiteaRepo gitea;
    public final ThingWorxContainer thingworx;
    public final GitBackupExtensionInstaller installer;
    public final HttpClient httpClient = HttpClient.newBuilder().build();
    private final TestingCredentials credentials;

    public GitBackupExtensionTestStack(
            String dbInitImage,
            String platformImage,
            TestingCredentials credentials,
            boolean enableGitea)
            throws Exception {
        this.credentials = credentials;
        network = Network.newNetwork();
        postgres = new Postgres(network, credentials);
        dbInit = new DBInit(dbInitImage, postgres, network, credentials);
        gitea = enableGitea ? new GiteaRepo(network, credentials) : null;
        thingworx = new ThingWorxContainer(platformImage, dbInit, postgres, network, credentials);
        var zipPath =
                System.getProperty(
                        "test.extensionZip", "build/distributions/GitBackupExtension.zip");
        installer =
                new GitBackupExtensionInstaller(
                        Paths.get(zipPath), thingworx, network, credentials);
        this.start();
    }

    public GitBackupExtensionTestStack(
            String dbInitImage, String platformImage, TestingCredentials credentials)
            throws Exception {
        this(dbInitImage, platformImage, credentials, true);
    }

    public void start() throws Exception {
        postgres.start();
        dbInit.start();
        thingworx.start();
        installer.start();
        if (gitea != null) {
            gitea.start();
            // Create admin user via CLI (works fine)
            for (int i = 0; i < 10; i++) {
                try {
                    var userResult =
                            gitea.execInContainer(
                                    "sh",
                                    "-c",
                                    "su git -c '/usr/local/bin/gitea admin user create --username "
                                            + credentials.giteaUser
                                            + " --password "
                                            + credentials.giteaPass
                                            + " --email admin@example.com --admin --must-change-password=false'");
                    System.out.println(
                            "GITEA_DEBUG user attempt "
                                    + i
                                    + " exit="
                                    + userResult.getExitCode()
                                    + " stdout="
                                    + userResult.getStdout()
                                    + " stderr="
                                    + userResult.getStderr());
                    if (userResult.getExitCode() == 0) break;
                } catch (Exception e) {
                    System.out.println(
                            "GITEA_DEBUG user attempt " + i + " exception: " + e.getMessage());
                }
                Thread.sleep(2000);
            }
            // Create repo via Gitea REST API (more reliable than CLI)
            for (int i = 0; i < 30; i++) {
                try {
                    var repoBody = new java.util.HashMap<String, Object>();
                    repoBody.put("name", credentials.repoName);
                    repoBody.put("private", false);
                    repoBody.put("auto_init", true);
                    repoBody.put("default_branch", "main");
                    var request =
                            HttpRequest.newBuilder()
                                    .uri(
                                            URI.create(
                                                    "http://"
                                                            + gitea.getHost()
                                                            + ":"
                                                            + gitea.getMappedPort(3000)
                                                            + "/api/v1/user/repos"))
                                    .header("Content-Type", "application/json")
                                    .header(
                                            "Authorization",
                                            "Basic "
                                                    + Base64.getEncoder()
                                                            .encodeToString(
                                                                    (credentials.giteaUser
                                                                                    + ":"
                                                                                    + credentials
                                                                                            .giteaPass)
                                                                            .getBytes()))
                                    .POST(
                                            HttpRequest.BodyPublishers.ofString(
                                                    new com.google.gson.Gson().toJson(repoBody)))
                                    .build();
                    var repoRes = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println(
                            "GITEA_DEBUG repo attempt "
                                    + i
                                    + " status="
                                    + repoRes.statusCode()
                                    + " body="
                                    + repoRes.body());
                    if (repoRes.statusCode() == 201 || repoRes.statusCode() == 200) break;
                } catch (Exception e) {
                    System.out.println(
                            "GITEA_DEBUG repo attempt " + i + " exception: " + e.getMessage());
                }
                Thread.sleep(2000);
            }
        }

        var req1 =
                thingworx
                        .serviceRequest("GIT.Utility.Thing", "InitUserExtensionProperties", null)
                        .build();
        var req2 =
                thingworx
                        .serviceRequest(
                                "GIT.Utility.Thing", "InitUserExtensionGpgKeysProperty", null)
                        .build();

        httpClient.send(req1, HttpResponse.BodyHandlers.ofString());
        httpClient.send(req2, HttpResponse.BodyHandlers.ofString());
    }

    public void close() {
        if (installer != null) installer.close();
        if (thingworx != null) thingworx.close();
        if (dbInit != null) dbInit.close();
        if (postgres != null) postgres.close();
        if (gitea != null) gitea.close();
        if (network != null) network.close();
    }
}
