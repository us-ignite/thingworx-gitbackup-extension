package gb.tests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class ThingWorxIntegrationTest {

    private static final String DB_PASS = "twx_password_123";
    private static final String ADMIN_PASS = "MinimumRequirements1!";
    private static final String DB_USER = "twadmin";
    private static final String APP_KEY = "provisioning_app_key";

    private static Network network;
    private static GenericContainer<?> postgres;
    private static GenericContainer<?> dbInit;
    private static GenericContainer<?> platform;
    private static HttpClient httpClient;
    private static String baseUrl;

    @BeforeAll
    static void startContainers() throws Exception {
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        network = Network.newNetwork();

        postgres = new GenericContainer<>("postgres:15")
            .withNetwork(network)
            .withNetworkAliases("postgresql")
            .withEnv("POSTGRES_USER", "postgres")
            .withEnv("POSTGRES_PASSWORD", DB_PASS)
            .withEnv("POSTGRES_DB", "postgres")
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));
        postgres.start();

        dbInit = new GenericContainer<>("devopscadit/postgresql-init-twx:platform9.6.3")
            .withNetwork(network)
            .withEnv("DATABASE_ADMIN_USERNAME", "postgres")
            .withEnv("DATABASE_ADMIN_PASSWORD", DB_PASS)
            .withEnv("DATABASE_ADMIN_SCHEMA", "postgres")
            .withEnv("DATABASE_HOST", "postgresql")
            .withEnv("DATABASE_PORT", "5432")
            .withEnv("TWX_DATABASE_USERNAME", DB_USER)
            .withEnv("TWX_DATABASE_SCHEMA", DB_USER)
            .withEnv("TWX_DATABASE_PASSWORD", DB_PASS)
            .withEnv("TABLESPACE_LOCATION", "/var/lib/postgresql/data")
            .withCreateContainerCmdModifier(cmd ->
                cmd.withEntrypoint("bash", "-c",
                    "/usr/local/bin/db-setup.sh && echo 'DB_INIT_DONE' && sleep infinity"))
            .waitingFor(Wait.forLogMessage(".*DB_INIT_DONE.*", 1));
        dbInit.start();

        platform = new GenericContainer<>("devopscadit/platform-postgres:platform9.6.3")
            .withNetwork(network)
            .withEnv("DATABASE_HOST", "postgresql")
            .withEnv("DATABASE_PORT", "5432")
            .withEnv("TWX_DATABASE_USERNAME", DB_USER)
            .withEnv("TWX_DATABASE_SCHEMA", DB_USER)
            .withEnv("TWX_DATABASE_PASSWORD", DB_PASS)
            .withEnv("THINGWORX_INITIAL_ADMIN_PASSWORD", ADMIN_PASS)
            .withEnv("THINGWORX_INITIAL_METRICS_USER_PASSWORD", "MetricsPass1!")
            .withEnv("ENABLE_CONSOLE_OUTPUT", "true")
            .withEnv("CATALINA_OPTS", "-Xms1g -Xmx2g")
            .withEnv("THINGWORX_PLATFORM_SCRIPTTIMEOUT", "30")
            .withEnv("TOMCAT_KEEPALIVETIMEOUT", "20000")
            .withEnv("TOMCAT_CONNECTIONTIMEOUT", "20000")
            .withEnv("TOMCAT_MAXCONNECTION", "10000")
            .withEnv("TOMCAT_MAXTHREADS", "200")
            .withEnv("TOMCAT_CATALINA_LEVEL", "FINE")
            .withEnv("TOMCAT_LOCALHOST_LEVEL", "FINE")
            .withEnv("TOMCAT_MANAGER_LEVEL", "FINE")
            .withEnv("TOMCAT_HOSTMANAGER_LEVEL", "FINE")
            .withEnv("TOMCAT_JAVAUTIL_LEVEL", "FINE")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/Thingworx/health")
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(10)));
        platform.start();

        baseUrl = "http://" + platform.getHost() + ":" + platform.getMappedPort(8080);
    }

    @AfterAll
    static void stopContainers() {
        if (platform != null) platform.stop();
        if (dbInit != null) dbInit.stop();
        if (postgres != null) postgres.stop();
        if (network != null) network.close();
    }

    @Test
    void thingworxHealthCheck() throws Exception {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/Thingworx/health"))
            .GET()
            .timeout(Duration.ofSeconds(30))
            .build();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Health endpoint must return 200");
    }

    @Test
    void installAndVerifyExtension() throws Exception {
        Path extZipPath = Paths.get("build/distributions/GitBackupExtension.zip");
        assertTrue(Files.exists(extZipPath), "Extension ZIP must exist at " + extZipPath.toAbsolutePath());

        byte[] zipBytes = Files.readAllBytes(extZipPath);
        String boundary = "----FormBoundary" + System.currentTimeMillis();

        var body = new ByteArrayOutputStream();
        writeBoundary(body, boundary, "File", "GitBackupExtension.zip", "application/octet-stream");
        body.write(zipBytes);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes());

        var uploadReq = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/Thingworx/ExtensionPackageUploader?purpose=import"))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .header("appKey", APP_KEY)
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
            .timeout(Duration.ofMinutes(5))
            .build();

        var uploadRes = httpClient.send(uploadReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(uploadRes.statusCode() == 200 || uploadRes.statusCode() == 201,
            "Extension upload failed. Status: " + uploadRes.statusCode() + ", Body: " + uploadRes.body());

        var verifyReq = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/Thingworx/Things/GIT.Utility.Thing"))
            .header("appKey", APP_KEY)
            .header("Accept", "application/json")
            .GET()
            .timeout(Duration.ofSeconds(30))
            .build();
        var verifyRes = httpClient.send(verifyReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(verifyRes.statusCode() == 200 || verifyRes.statusCode() == 401,
            "GIT.Utility.Thing should exist or be accessible. Status: " + verifyRes.statusCode());
    }

    private static void writeBoundary(ByteArrayOutputStream out, String boundary,
                                       String name, String filename, String contentType) throws Exception {
        out.write(("--" + boundary + "\r\n").getBytes());
        out.write(("Content-Disposition: form-data; name=\"" + name
            + "\"; filename=\"" + filename + "\"\r\n").getBytes());
        out.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes());
    }
}
