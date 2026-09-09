package org.us_ignite.thingworx.jgit.test.containers;

import java.nio.file.Paths;
import org.us_ignite.thingworx.testcontainers.TestImages;
import org.us_ignite.thingworx.testcontainers.TestingCredentials;
import org.us_ignite.thingworx.testcontainers.ThingWorxTestStack;

public class JGitExtensionTestStack extends ThingWorxTestStack {
    public final GiteaRepo gitea;
    public final GiteaInit giteaInit;
    public final JGitExtensionInstaller installer;

    public JGitExtensionTestStack(TestingCredentials credentials, boolean enableGitea)
            throws Exception {
        this(TestImages.dbInitImage(), TestImages.platformImage(), credentials, enableGitea);
    }

    public JGitExtensionTestStack(TestingCredentials credentials) throws Exception {
        this(credentials, true);
    }

    public JGitExtensionTestStack(
            String dbInitImage,
            String platformImage,
            TestingCredentials credentials,
            boolean enableGitea)
            throws Exception {
        super(dbInitImage, platformImage, credentials, false);
        gitea = enableGitea ? new GiteaRepo(network, credentials) : null;
        giteaInit = enableGitea ? new GiteaInit(gitea, network, credentials) : null;
        var zipPath =
                System.getProperty(
                        "test.extensionZip",
                        "apps/thingworx-jgit-extension/build/distributions/JGitExtension.zip");
        installer = new JGitExtensionInstaller(Paths.get(zipPath), thingworx, network, credentials);
        this.start();
    }

    public JGitExtensionTestStack(
            String dbInitImage, String platformImage, TestingCredentials credentials)
            throws Exception {
        this(dbInitImage, platformImage, credentials, true);
    }

    public void start() throws Exception {
        super.start();
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
        if (giteaInit != null) giteaInit.close();
        if (gitea != null) gitea.close();
        super.close();
    }
}
