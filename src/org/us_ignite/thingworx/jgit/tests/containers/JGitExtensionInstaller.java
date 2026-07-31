package org.us_ignite.thingworx.jgit.tests.containers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class JGitExtensionInstaller extends GenericContainer<JGitExtensionInstaller> {
    public JGitExtensionInstaller(
            Path extensionZip,
            ThingWorxContainer platform,
            Network network,
            TestingCredentials credentials) {
        super("curlimages/curl:7.85.0");
        dependsOn(platform);
        withNetwork(network);
        platform.withNetworkAliases("thingworx");

        assertTrue(
                Files.exists(extensionZip),
                "Extension ZIP must exist at " + extensionZip.toAbsolutePath());

        Path installScript = Path.of("scripts", "install-extension.sh").toAbsolutePath();
        assertTrue(
                Files.exists(installScript),
                "Install script must exist at " + installScript.toAbsolutePath());

        withFileSystemBind(
                extensionZip.toAbsolutePath().toString(), "/tmp/extension.zip", BindMode.READ_ONLY);
        withFileSystemBind(
                installScript.toString(), "/scripts/install-extension.sh", BindMode.READ_ONLY);

        withEnv("TWX_URL", "http://thingworx:8080");
        withEnv("TWX_USERNAME", credentials.thingworxAdminUser);
        withEnv("TWX_PASSWORD", credentials.thingworxAdminPass);
        withEnv("EXTENSION_ZIP", "/tmp/extension.zip");

        withLogConsumer(
                outputFrame -> System.out.print("[INSTALLER] " + outputFrame.getUtf8String()));
        withCreateContainerCmdModifier(
                cmd -> cmd.withEntrypoint("sh", "/scripts/install-extension.sh"));
        waitingFor(Wait.forLogMessage(".*UPLOAD_DONE.*", 1));
        withStartupTimeout(java.time.Duration.ofMinutes(10));
    }
}
