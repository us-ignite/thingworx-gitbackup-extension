package gb.tests.junit.containers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import gb.tests.junit.util.TestingCredentials;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class GitBackupExtensionInstaller extends GenericContainer<GitBackupExtensionInstaller> {
    public GitBackupExtensionInstaller(
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

        withFileSystemBind(
                extensionZip.toAbsolutePath().toString(), "/tmp/extension.zip", BindMode.READ_ONLY);

        String createScript =
                String.format(
                        "echo '=== Starting extension install ==='\n"
                                + "UPLOAD_OK=false\n"
                                + "for purpose in import upgrade; do\n"
                                + "  UPLOAD_RESP=$(mktemp)\n"
                                + "  for attempt in 1 2 3; do\n"
                                + "    UPLOAD_STATUS=$(curl -s -o \"$UPLOAD_RESP\" -w '%%{http_code}' -X POST \\\n"
                                + "      -H 'X-XSRF-TOKEN: TWX-XSRF-TOKEN-VALUE' \\\n"
                                + "      -H 'X-Requested-By: ThingWorx' \\\n"
                                + "      -u '%s:%s' \\\n"
                                + "      -F 'file=@/tmp/extension.zip;filename=GitBackupExtension.zip;type=application/octet-stream' \\\n"
                                + "      --connect-timeout 30 --max-time 300 \\\n"
                                + "      'http://thingworx:8080/Thingworx/ExtensionPackageUploader?purpose='\"$purpose\")\n"
                                + "    echo \"Attempt $attempt ($purpose): upload status=$UPLOAD_STATUS\"\n"
                                + "    echo \"Response body: $(cat \"$UPLOAD_RESP\")\"\n"
                                + "    if [ \"$UPLOAD_STATUS\" = \"200\" ] || [ \"$UPLOAD_STATUS\" = \"201\" ]; then\n"
                                + "      echo 'Upload succeeded'\n"
                                + "      UPLOAD_OK=true\n"
                                + "      break 2\n"
                                + "    fi\n"
                                + "    if [ \"$UPLOAD_STATUS\" = \"406\" ] && grep -q 'queued for installation' \"$UPLOAD_RESP\"; then\n"
                                + "      echo 'Upload queued for installation; restart required'\n"
                                + "      UPLOAD_OK=true\n"
                                + "      break 2\n"
                                + "    fi\n"
                                + "    if [ \"$UPLOAD_STATUS\" = \"406\" ] && [ \"$purpose\" = \"import\" ]; then\n"
                                + "      echo 'Got 406 on import, trying upgrade...'\n"
                                + "      break\n"
                                + "    fi\n"
                                + "    echo \"Retrying...\"\n"
                                + "    sleep 10\n"
                                + "  done\n"
                                + "done\n"
                                + "if [ \"$UPLOAD_OK\" != \"true\" ]; then\n"
                                + "  echo 'UPLOAD_FAILED'\n"
                                + "  exit 1\n"
                                + "fi\n",
                        credentials.thingworxAdminUser, credentials.thingworxAdminPass);

        String completionScript = "echo 'UPLOAD_DONE'\n";

        withLogConsumer(
                outputFrame -> System.out.print("[INSTALLER] " + outputFrame.getUtf8String()));
        withCreateContainerCmdModifier(
                cmd -> cmd.withEntrypoint("sh", "-c", createScript + completionScript));
        waitingFor(Wait.forLogMessage(".*UPLOAD_DONE.*", 1));
        withStartupTimeout(java.time.Duration.ofMinutes(10));
    }
}
