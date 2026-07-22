package gb.tests.junit.containers;

import gb.tests.junit.util.TestingCredentials;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class ThingWorxContainer extends GenericContainer<ThingWorxContainer> {

    private String externalUrl;

    private static final Path JDK21_DIR = prepareCachedJdk21();

    private static Path prepareCachedJdk21() {
        Path cacheDir = Path.of(System.getProperty("user.dir"), ".cache", "jdk21");
        Path javaBin = cacheDir.resolve("bin/java");
        if (Files.exists(javaBin)) {
            return cacheDir;
        }
        throw new RuntimeException(
                "JDK 21 not found at .cache/jdk21. Run 'gradle fetchJdk21' first.");
    }

    private static Map<String, String> loadEnvFile() {
        try {
            var envPath = Path.of(System.getProperty("user.dir"), ".env");
            if (Files.exists(envPath)) {
                return Files.lines(envPath)
                        .filter(line -> line.contains("="))
                        .collect(
                                Collectors.toMap(
                                        line -> line.split("=")[0].trim(),
                                        line ->
                                                parseEnvValue(
                                                        line.substring(line.indexOf("=") + 1))));
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load .env file: " + e.getMessage());
        }
        return Map.of();
    }

    private static String parseEnvValue(String rawValue) {
        var value = rawValue.trim();
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static final Map<String, String> ENV = loadEnvFile();

    private String authHeader;

    public ThingWorxContainer(
            String platformImage,
            GenericContainer<?> dbInit,
            GenericContainer<?> postgres,
            Network network,
            TestingCredentials credentials) {
        this(platformImage, dbInit, postgres, network, credentials, "postgresql", "thingworx");
    }

    public ThingWorxContainer(
            String platformImage,
            GenericContainer<?> dbInit,
            GenericContainer<?> postgres,
            Network network,
            TestingCredentials credentials,
            String dbHostAlias,
            String thingworxAlias) {
        super(platformImage);
        withNetwork(network);
        withNetworkAliases(thingworxAlias);
        dependsOn(dbInit, postgres);
        withEnv("DATABASE_HOST", dbHostAlias);
        withEnv("DATABASE_PORT", "5432");
        withEnv("TWX_DATABASE_USERNAME", credentials.twxDatabaseUser);
        withEnv("TWX_DATABASE_SCHEMA", credentials.twxDatabaseSchema);
        withEnv("TWX_DATABASE_PASSWORD", credentials.twxDatabasePass);
        withEnv("THINGWORX_INITIAL_ADMIN_PASSWORD", credentials.thingworxAdminPass);
        withEnv("THINGWORX_INITIAL_METRICS_USER_PASSWORD", "MetricsP@ssw0rd!");
        withEnv("ENABLE_CONSOLE_OUTPUT", "true");
        withEnv("THINGWORX_PLATFORM_SCRIPTTIMEOUT", "30");
        withEnv("CATALINA_OPTS", "-Xms1g -Xmx2g --add-opens=java.base/java.net=ALL-UNNAMED");
        withEnv("TOMCAT_KEEPALIVETIMEOUT", "20000");
        withEnv("TOMCAT_CONNECTIONTIMEOUT", "20000");
        withEnv("TOMCAT_MAXCONNECTION", "10000");
        withEnv("TOMCAT_MAXTHREADS", "200");
        withEnv("TOMCAT_CATALINA_LEVEL", "FINE");
        withEnv("TOMCAT_LOCALHOST_LEVEL", "FINE");
        withEnv("TOMCAT_MANAGER_LEVEL", "FINE");
        withEnv("EXTPKG_IMPORT_POLICY_ENABLED", "true");
        withEnv("EXTPKG_IMPORT_POLICY_ALLOW_ENTITIES", "true");
        withEnv("EXTPKG_IMPORT_POLICY_ALLOW_EXTENTITIES", "true");
        withEnv("EXTPKG_IMPORT_POLICY_ALLOW_JARRES", "true");
        withEnv("EXTPKG_IMPORT_POLICY_ALLOW_JSRES", "true");
        withEnv("EXTPKG_IMPORT_POLICY_ALLOW_CSSRES", "true");
        withEnv("EXTPKG_IMPORT_POLICY_ALLOW_JSONRES", "true");
        withEnv("EXTPKG_IMPORT_POLICY_ALLOW_WEBAPPRES", "true");
        withEnv("TOMCAT_HOSTMANAGER_LEVEL", "FINE");
        // get from test ENV
        withEnv("LS_USERNAME", ENV.getOrDefault("LS_USERNAME", ""));
        withEnv("LS_PASSWORD", ENV.getOrDefault("LS_PASSWORD", ""));
        withEnv("USE_TRIAL_LICENSE", "true");

        var licenseFile = Path.of(System.getProperty("user.dir"), "twx-lib", "license.bin");
        if (Files.exists(licenseFile)) {
            withFileSystemBind(
                    licenseFile.toAbsolutePath().toString(),
                    "/opt/trial.bin",
                    BindMode.READ_ONLY);
        }

        if (JDK21_DIR != null) {
            withFileSystemBind(
                    JDK21_DIR.toAbsolutePath().toString(), "/mnt/jdk21", BindMode.READ_ONLY);
            withCreateContainerCmdModifier(
                    cmd ->
                            cmd.withEntrypoint(
                                    "sh",
                                    "-c",
                                    "rm -rf /opt/jdk && ln -sf /mnt/jdk21 /opt/jdk && exec /docker-entrypoint.sh run"));
        }

        withExposedPorts(8080);
        waitingFor(
                Wait.forHttp("/Thingworx/health")
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(15)));

        authHeader =
                "Basic "
                        + Base64.getEncoder()
                                .encodeToString(
                                        (credentials.thingworxAdminUser
                                                        + ":"
                                                        + credentials.thingworxAdminPass)
                                                .getBytes());
    }

    @Override
    public void start() {
        super.start();
        String host = getHost();
        int port = getMappedPort(8080);
        if (host.equals("localhost")
                || host.equals("127.0.0.1")
                || host.contains("docker.internal")
                || host.contains("desktop")) {
            externalUrl = "http://" + host + ":" + port;
        } else {
            externalUrl = "http://localhost:" + port;
        }
        System.out.println("[ThingWorxContainer] External URL: " + externalUrl);
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public Builder serviceRequest(String thingName, String serviceName, String body) {
        var baseUrl = externalUrl != null ? externalUrl : "http://thingworx:8080";
        var uri =
                URI.create(baseUrl + "/Thingworx/Things/" + thingName + "/Services/" + serviceName);
        return HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json;charset=UTF-8")
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                .header("X-Requested-By", "ThingWorx")
                .POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
    }

    public HttpRequest healthCheckRequest() {
        var baseUrl = externalUrl != null ? externalUrl : "http://thingworx:8080";
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/Thingworx/health"))
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
    }
}
