package org.us_ignite.thingworx.jgit.tests.containers;

import com.github.dockerjava.api.model.VolumesFrom;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;

public class GiteaInit extends GenericContainer<GiteaInit> {
    public GiteaInit(GiteaRepo gitea, Network network, TestingCredentials credentials)
            throws Exception {
        this(gitea, network, credentials, true);
    }

    public GiteaInit(
            GiteaRepo gitea, Network network, TestingCredentials credentials, boolean autoInit)
            throws Exception {
        super("gitea/gitea:1.20.4");
        dependsOn(gitea);
        withNetwork(network);

        Path initScript = Path.of("scripts", "gitea-init.sh").toAbsolutePath();
        if (!Files.exists(initScript)) {
            throw new IllegalStateException(
                    "gitea-init.sh must exist at " + initScript.toAbsolutePath());
        }
        withFileSystemBind(initScript.toString(), "/scripts/gitea-init.sh", BindMode.READ_ONLY);

        // Share the Gitea server's /data volume so the CLI writes to the same instance.
        withCreateContainerCmdModifier(
                cmd -> {
                    var hostConfig = cmd.getHostConfig();
                    if (hostConfig != null) {
                        hostConfig.withVolumesFrom(new VolumesFrom(gitea.getContainerId()));
                    }
                    cmd.withEntrypoint("sh", "/scripts/gitea-init.sh");
                });

        withEnv("GITEA_URL", "http://gitea:3000");
        withEnv("GITEA_USERNAME", credentials.giteaUser);
        withEnv("GITEA_PASSWORD", credentials.giteaPass);
        withEnv("GITEA_ADMIN_EMAIL", "test@example.com");
        withEnv("GITEA_REPO_NAME", credentials.repoName);
        withEnv("GITEA_REPO_AUTO_INIT", Boolean.toString(autoInit));
        withEnv("GITEA_DEFAULT_BRANCH", "main");

        withLogConsumer(
                outputFrame -> System.out.print("[GITEA-INIT] " + outputFrame.getUtf8String()));
        withStartupCheckStrategy(
                new OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(5)));
    }
}
