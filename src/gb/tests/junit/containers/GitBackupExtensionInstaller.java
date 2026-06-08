package gb.tests.junit.containers;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import gb.tests.junit.util.TestingCredentials;

public class GitBackupExtensionInstaller extends GenericContainer<GitBackupExtensionInstaller> {
        public GitBackupExtensionInstaller(Path extensionZip, ThingWorxContainer platform, Network network, TestingCredentials credentials) {
                super("curlimages/curl:7.85.0");
                dependsOn(platform);
                withNetwork(network);
                platform.withNetworkAliases("thingworx");

                assertTrue(Files.exists(extensionZip), "Extension ZIP must exist at " + extensionZip.toAbsolutePath());

                withFileSystemBind(extensionZip.toAbsolutePath().toString(), "/tmp/extension.zip", BindMode.READ_ONLY);

                String createScript = String.format(
                        "UPLOAD_STATUS=$(curl -s -o /dev/null -w '%%{http_code}' -X POST \\\n" +
                        "  -H 'X-XSRF-TOKEN: TWX-XSRF-TOKEN-VALUE' \\\n" +
                        "  -H 'X-Requested-By: ThingWorx' \\\n" +
                        "  -H 'Accept: application/json' \\\n" +
                        "  -u '%s:%s' \\\n" +
                        "  -F 'file=@/tmp/extension.zip' \\\n" +
                        "  --connect-timeout 30 --max-time 300 \\\n" +
                        "  'http://thingworx:8080/Thingworx/ExtensionPackageUploader?purpose=import')\n" +
                        "echo \"Upload status: $UPLOAD_STATUS\"\n" +
                        "if [ \"$UPLOAD_STATUS\" != \"200\" ] && [ \"$UPLOAD_STATUS\" != \"201\" ]; then\n" +
                        "  exit 1\n" +
                        "fi\n\n", credentials.thingworxAdminUser, credentials.thingworxAdminPass);

                String verifyScript = String.format(
                        "VERIFY_STATUS=$(curl -s -o /dev/null -w '%%{http_code}' \\\n" +
                        "  -H 'Accept: application/json' \\\n" +
                        "  -u '%s:%s' \\\n" +
                        "  --connect-timeout 30 --max-time 30 \\\n" +
                        "  'http://thingworx:8080/Thingworx/Things/GIT.Utility.Thing')\n" +
                        "echo \"Verify status: $VERIFY_STATUS\"\n" +
                        "if [ \"$VERIFY_STATUS\" != \"200\" ] && [ \"$VERIFY_STATUS\" != \"401\" ]; then\n" +
                        "  exit 1\n" +
                        "fi\n\n" +
                        "echo 'ALL_DONE'\n",
                        credentials.thingworxAdminUser, credentials.thingworxAdminPass);

                withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("sh", "-c", createScript + verifyScript));
                waitingFor(Wait.forLogMessage(".*ALL_DONE.*", 1));
        }
}
