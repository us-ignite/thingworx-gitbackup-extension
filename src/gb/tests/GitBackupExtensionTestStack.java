package gb.tests;

import org.testcontainers.containers.Network;

import gb.tests.containers.DBInit;
import gb.tests.containers.GiteaRepo;
import gb.tests.containers.GitBackupExtensionInstaller;
import gb.tests.containers.Postgres;
import gb.tests.containers.ThingWorxContainer;
import gb.tests.util.TestingCredentials;
import gb.tests.util.ThingWorxVersion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.Base64;

public class GitBackupExtensionTestStack implements AutoCloseable {
    public final Network network;
    public final Postgres postgres;
    public final DBInit dbInit;
    public final GiteaRepo gitea;
    public final ThingWorxContainer thingworx;
    public final GitBackupExtensionInstaller installer;
    public final HttpClient httpClient = HttpClient.newBuilder().build();
    private final TestingCredentials credentials;



    public GitBackupExtensionTestStack(ThingWorxVersion thingworxVersion, TestingCredentials credentials, boolean enableGitea) throws Exception {
        network = Network.newNetwork();
        postgres = new Postgres(network, credentials);
        dbInit = new DBInit(thingworxVersion.dbInitImage, postgres, network, credentials);
        gitea = enableGitea ? new GiteaRepo(network, credentials) : null;
        thingworx = new ThingWorxContainer(thingworxVersion.platformImage, dbInit, postgres, network, credentials);
        installer = new GitBackupExtensionInstaller(Paths.get("build/distributions/GitBackupExtension.zip"), thingworx, network, credentials);
        this.credentials = credentials;
        this.start();
    }

    public GitBackupExtensionTestStack(ThingWorxVersion thingworxVersion) throws Exception {
        this(thingworxVersion, new TestingCredentials(), true);
    }

    public GitBackupExtensionTestStack(ThingWorxVersion thingworxVersion, boolean enableGitea) throws Exception {
        this(thingworxVersion, new TestingCredentials(), enableGitea);
    }

    public GitBackupExtensionTestStack(ThingWorxVersion thingworxVersion, TestingCredentials credentials) throws Exception {
        this(thingworxVersion, credentials, true);
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
                    var userResult = gitea.execInContainer("sh", "-c",
                            "su git -c '/usr/local/bin/gitea admin user create --username " + credentials.giteaUser
                            + " --password " + credentials.giteaPass
                            + " --email admin@example.com --admin --must-change-password=false'");
                    System.out.println("GITEA_DEBUG user attempt " + i + " exit=" + userResult.getExitCode()
                        + " stdout=" + userResult.getStdout() + " stderr=" + userResult.getStderr());
                    if (userResult.getExitCode() == 0) break;
                } catch (Exception e) {
                    System.out.println("GITEA_DEBUG user attempt " + i + " exception: " + e.getMessage());
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
                    var request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + gitea.getHost() + ":" + gitea.getMappedPort(3000) + "/api/v1/user/repos"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                            (credentials.giteaUser + ":" + credentials.giteaPass).getBytes()))
                        .POST(HttpRequest.BodyPublishers.ofString(
                            new com.google.gson.Gson().toJson(repoBody)))
                        .build();
                    var repoRes = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("GITEA_DEBUG repo attempt " + i + " status=" + repoRes.statusCode()
                        + " body=" + repoRes.body());
                    if (repoRes.statusCode() == 201 || repoRes.statusCode() == 200) break;
                } catch (Exception e) {
                    System.out.println("GITEA_DEBUG repo attempt " + i + " exception: " + e.getMessage());
                }
                Thread.sleep(2000);
            }
        }

        var req1 = thingworx.serviceRequest("GIT.Utility.Thing", "InitUserExtensionProperties", null).build();
        var req2 = thingworx.serviceRequest("GIT.Utility.Thing", "InitUserExtensionGpgKeysProperty", null).build();

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
