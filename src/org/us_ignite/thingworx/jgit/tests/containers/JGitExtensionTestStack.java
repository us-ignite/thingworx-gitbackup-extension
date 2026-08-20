package org.us_ignite.thingworx.jgit.tests.containers;

import java.net.http.HttpClient;
import java.nio.file.Paths;
import org.testcontainers.containers.Network;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;

public class JGitExtensionTestStack implements AutoCloseable {
    public final Network network;
    public final Postgres postgres;
    public final DBInit dbInit;
    public final GiteaRepo gitea;
    public final GiteaInit giteaInit;
    public final ThingWorxContainer thingworx;
    public final JGitExtensionInstaller installer;
    public final HttpClient httpClient = HttpClient.newBuilder().build();

    public JGitExtensionTestStack(
            String dbInitImage,
            String platformImage,
            TestingCredentials credentials,
            boolean enableGitea)
            throws Exception {
        network = Network.newNetwork();
        postgres = new Postgres(network, credentials);
        dbInit = new DBInit(dbInitImage, postgres, network, credentials);
        gitea = enableGitea ? new GiteaRepo(network, credentials) : null;
        giteaInit = enableGitea ? new GiteaInit(gitea, network, credentials) : null;
        thingworx = new ThingWorxContainer(platformImage, dbInit, postgres, network, credentials);
        var zipPath =
                System.getProperty("test.extensionZip", "build/distributions/JGitExtension.zip");
        installer = new JGitExtensionInstaller(Paths.get(zipPath), thingworx, network, credentials);
        this.start();
    }

    public JGitExtensionTestStack(
            String dbInitImage, String platformImage, TestingCredentials credentials)
            throws Exception {
        this(dbInitImage, platformImage, credentials, true);
    }

    public void start() throws Exception {
        postgres.start();
        dbInit.start();
        thingworx.start();
        installer.start();

        // Restart ThingWorx to ensure the extension is loaded and initialized
        // DockerClient dockerClient = thingworx.getDockerClient();

        // String containerId = thingworx.getContainerId();

        // dockerClient.stopContainerCmd(containerId).exec();
        // dockerClient.startContainerCmd(containerId).exec();

        if (gitea != null) {
            gitea.start();
            giteaInit.start();
        }
    }

    public void close() {
        if (installer != null) installer.close();
        if (thingworx != null) thingworx.close();
        if (dbInit != null) dbInit.close();
        if (postgres != null) postgres.close();
        if (giteaInit != null) giteaInit.close();
        if (gitea != null) gitea.close();
        if (network != null) network.close();
    }
}
