package containers;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.time.Duration;
import java.util.Base64;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import util.TestingCredentials;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

public class ThingWorxContainer extends GenericContainer<ThingWorxContainer> {

    private String externalUrl;

    private static Map<String, String> loadEnvFile() {
        try {
            var envPath = Path.of(System.getProperty("user.dir"), ".env");
            if (Files.exists(envPath)) {
                return Files.lines(envPath)
                    .filter(line -> line.contains("="))
                    .collect(Collectors.toMap(
                        line -> line.split("=")[0].trim(),
                        line -> line.substring(line.indexOf("=") + 1).trim()
                    ));
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load .env file: " + e.getMessage());
        }
        return Map.of();
    }

    private static final Map<String, String> ENV = loadEnvFile();

    private String authHeader;

    public ThingWorxContainer(String platformImage,
                GenericContainer<?> dbInit, GenericContainer<?> postgres, Network network, TestingCredentials credentials) {
        super(platformImage);
        withNetwork(network);
        withNetworkAliases("thingworx");
        dependsOn(dbInit, postgres);
        withEnv("DATABASE_HOST", "postgresql");
        withEnv("DATABASE_PORT", "5432");
        withEnv("TWX_DATABASE_USERNAME", credentials.twxDatabaseUser);
        withEnv("TWX_DATABASE_SCHEMA", credentials.twxDatabaseSchema);
        withEnv("TWX_DATABASE_PASSWORD", credentials.twxDatabasePass);
        withEnv("THINGWORX_INITIAL_ADMIN_PASSWORD", credentials.thingworxAdminPass);
        withEnv("THINGWORX_INITIAL_METRICS_USER_PASSWORD", "MetricsP@ssw0rd!");
        withEnv("ENABLE_CONSOLE_OUTPUT", "true");
        withEnv("CATALINA_OPTS", "-Xms1g -Xmx2g");
        withEnv("THINGWORX_PLATFORM_SCRIPTTIMEOUT", "30");
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
        withExposedPorts(8080);
        waitingFor(Wait.forHttp("/Thingworx/health")
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(15)));

        authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (credentials.thingworxAdminUser + ":" + credentials.thingworxAdminPass).getBytes());
    }

    @Override
    public void start() {
        super.start();
        String host = getHost();
        int port = getMappedPort(8080);
        if (host.equals("localhost") || host.equals("127.0.0.1") || host.contains("docker.internal") || host.contains("desktop")) {
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
        var uri = URI.create(baseUrl + "/Thingworx/Things/"
                + thingName
                + "/Services/"
                + serviceName);
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
