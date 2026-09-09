package org.us_ignite.thingworx.operator;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderCallbacks;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.config.LeaderElectionConfiguration;
import io.javaoperatorsdk.operator.api.config.LeaderElectionConfigurationBuilder;
import io.javaoperatorsdk.operator.processing.retry.GenericRetry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.us_ignite.thingworx.operator.metrics.OperatorMetrics;
import org.us_ignite.thingworx.operator.reconcile.ThingWorxClusterReconciler;
import org.us_ignite.thingworx.operator.reconcile.ThingWorxExtensionReconciler;

/** Starts the ThingWorx controller manager with leader election, retry and graceful shutdown. */
public final class OperatorApplication {
    private static final Logger LOG = LoggerFactory.getLogger(OperatorApplication.class);

    static final String ENV_LEADER_ELECTION_ENABLED = "OPERATOR_LEADER_ELECTION_ENABLED";
    static final String ENV_LEADER_ELECTION_LEASE_NAME = "OPERATOR_LEADER_ELECTION_LEASE_NAME";
    static final String ENV_LEADER_ELECTION_LEASE_NAMESPACE = "OPERATOR_LEADER_ELECTION_LEASE_NAMESPACE";
    static final String ENV_LEADER_ELECTION_LEASE_DURATION = "OPERATOR_LEADER_ELECTION_LEASE_DURATION_SECONDS";
    static final String ENV_LEADER_ELECTION_RENEW_DEADLINE = "OPERATOR_LEADER_ELECTION_RENEW_DEADLINE_SECONDS";
    static final String ENV_LEADER_ELECTION_RETRY_PERIOD = "OPERATOR_LEADER_ELECTION_RETRY_PERIOD_SECONDS";
    static final String ENV_LEADER_ELECTION_IDENTITY = "OPERATOR_LEADER_ELECTION_IDENTITY";
    static final String ENV_LEADER_ELECTION_EXIT_ON_STOP = "OPERATOR_LEADER_ELECTION_EXIT_ON_STOP_LEADING";

    static final String ENV_RETRY_MAX_ATTEMPTS = "OPERATOR_RETRY_MAX_ATTEMPTS";
    static final String ENV_RETRY_INITIAL_INTERVAL = "OPERATOR_RETRY_INITIAL_INTERVAL_MS";
    static final String ENV_RETRY_MULTIPLIER = "OPERATOR_RETRY_INTERVAL_MULTIPLIER";
    /** Alias for {@link #ENV_RETRY_MULTIPLIER}. */
    static final String ENV_RETRY_MULTIPLIER_ALT = "OPERATOR_RETRY_MULTIPLIER";
    static final String ENV_RETRY_MAX_INTERVAL = "OPERATOR_RETRY_MAX_INTERVAL_MS";

    static final String ENV_WATCHED_NAMESPACES = "OPERATOR_WATCHED_NAMESPACES";
    static final String ENV_NAMESPACE = "OPERATOR_NAMESPACE";
    static final String ENV_NAMESPACE_ALT = "WATCHED_NAMESPACE";

    static final String ENV_HEALTH_PORT = "OPERATOR_HEALTH_PORT";
    private static final int DEFAULT_HEALTH_PORT = 8080;

    private static final String DEFAULT_LEASE_NAME = "thingworx-operator";
    private static final Duration DEFAULT_LEASE_DURATION = Duration.ofSeconds(15);
    private static final Duration DEFAULT_RENEW_DEADLINE = Duration.ofSeconds(10);
    private static final Duration DEFAULT_RETRY_PERIOD = Duration.ofSeconds(2);

    private static volatile HttpServer healthServer;

    private OperatorApplication() {}

    public static void main(String[] args) {
        LOG.info(
                "operator.startup phase=initializing version={} javaVersion={}",
                readVersion(),
                System.getProperty("java.version"));

        var client = new KubernetesClientBuilder().build();
        var retry = buildRetry();
        var leaderElectionEnabled = isLeaderElectionEnabled();
        var watchedNamespaces = parseWatchedNamespaces();

        LOG.info(
                "operator.config phase=configured leaderElectionEnabled={} leaseName={} leaseNamespace={} watchedNamespaces={} retryMaxAttempts={} retryInitialIntervalMs={} retryMultiplier={} retryMaxIntervalMs={}",
                leaderElectionEnabled,
                envOrDefault(ENV_LEADER_ELECTION_LEASE_NAME, DEFAULT_LEASE_NAME),
                envOrDefault(ENV_LEADER_ELECTION_LEASE_NAMESPACE, "<cluster>"),
                watchedNamespaces == null ? "<all>" : String.join(",", watchedNamespaces),
                retry.getMaxAttempts(),
                retry.getInitialInterval(),
                retry.getIntervalMultiplier(),
                retry.getMaxInterval());

        var operator = buildOperator(client, leaderElectionEnabled, retry);

        startHealthServer();

        var latch = new CountDownLatch(1);
        var shuttingDown = new AtomicBoolean(false);

        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    if (!shuttingDown.compareAndSet(false, true)) {
                                        return;
                                    }
                                    LOG.info("operator.shutdown phase=shutdownHookTriggered signal=SIGTERM");
                                    try {
                                        LOG.info("operator.shutdown phase=stoppingOperator");
                                        operator.stop();
                                        LOG.info("operator.shutdown phase=operatorStopped");
                                    } catch (Exception exception) {
                                        LOG.error(
                                                "operator.shutdown phase=operatorStopFailed error={}",
                                                exception.toString(),
                                                exception);
                                    }
                                    try {
                                        LOG.info("operator.shutdown phase=closingClient");
                                        client.close();
                                        LOG.info("operator.shutdown phase=clientClosed");
                                    } catch (Exception exception) {
                                        LOG.error(
                                                "operator.shutdown phase=clientCloseFailed error={}",
                                                exception.toString(),
                                                exception);
                                    }
                                    stopHealthServer();
                                    LOG.info("operator.shutdown phase=completed");
                                    latch.countDown();
                                },
                                "operator-shutdown-hook"));

        try {
            LOG.info("operator.startup phase=startingOperator");
            operator.register(
                    new ThingWorxClusterReconciler(client),
                    config -> configureController(config, retry, watchedNamespaces, "ThingWorxCluster"));
            operator.register(
                    new ThingWorxExtensionReconciler(client),
                    config -> configureController(config, retry, watchedNamespaces, "ThingWorxExtension"));
            operator.start();
            LOG.info(
                    "operator.startup phase=started leaderElectionEnabled={} watchedNamespaces={}",
                    leaderElectionEnabled,
                    watchedNamespaces == null ? "<all>" : String.join(",", watchedNamespaces));
            latch.await();
            LOG.info("operator.shutdown phase=mainThreadExiting");
            stopHealthServer();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOG.warn(
                    "operator.interrupted phase=interrupted error={}",
                    exception.toString(),
                    exception);
            if (shuttingDown.compareAndSet(false, true)) {
                try {
                    LOG.info("operator.shutdown phase=stoppingOperator reason=interrupted");
                    operator.stop();
                } catch (Exception nested) {
                    LOG.warn(
                            "operator.shutdown phase=operatorStopFailed error={}",
                            nested.toString(),
                            nested);
                }
                try {
                    client.close();
                } catch (Exception nested) {
                    LOG.warn(
                            "operator.shutdown phase=clientCloseFailed error={}",
                            nested.toString(),
                            nested);
                }
                stopHealthServer();
                latch.countDown();
            }
        } catch (Exception exception) {
            LOG.error(
                    "operator.startup phase=failed error={}",
                    exception.toString(),
                    exception);
            // Ensure client is closed on startup failure
            try {
                client.close();
            } catch (Exception nested) {
                LOG.warn(
                        "operator.shutdown phase=clientCloseFailed error={}",
                        nested.toString(),
                        nested);
            }
            stopHealthServer();
            throw exception instanceof RuntimeException
                    ? (RuntimeException) exception
                    : new RuntimeException(exception);
        }
    }

    static Operator buildOperator(
            KubernetesClient client, boolean leaderElectionEnabled, GenericRetry retry) {
        if (!leaderElectionEnabled) {
            LOG.info("operator.leaderElection phase=disabled");
            return new Operator(
                    overrider ->
                            overrider
                                    .withKubernetesClient(client)
                                    .withCloseClientOnStop(false));
        }
        var leaderElectionConfiguration = buildLeaderElectionConfiguration();
        LOG.info(
                "operator.leaderElection phase=enabled leaseName={} leaseNamespace={} leaseDurationSeconds={} renewDeadlineSeconds={} retryPeriodSeconds={} identity={} exitOnStopLeading={}",
                leaderElectionConfiguration.getLeaseName(),
                leaderElectionConfiguration.getLeaseNamespace().orElse("<auto>"),
                leaderElectionConfiguration.getLeaseDuration().toSeconds(),
                leaderElectionConfiguration.getRenewDeadline().toSeconds(),
                leaderElectionConfiguration.getRetryPeriod().toSeconds(),
                leaderElectionConfiguration.getIdentity().orElse("<auto>"),
                leaderElectionConfiguration.isExitOnStopLeading());
        return new Operator(
                overrider ->
                        overrider
                                .withKubernetesClient(client)
                                .withCloseClientOnStop(false)
                                .withLeaderElectionConfiguration(leaderElectionConfiguration));
    }

    @SuppressWarnings("deprecation")
    static LeaderElectionConfiguration buildLeaderElectionConfiguration() {
        var leaseName = envOrDefault(ENV_LEADER_ELECTION_LEASE_NAME, DEFAULT_LEASE_NAME);
        var leaseNamespace = envTrim(ENV_LEADER_ELECTION_LEASE_NAMESPACE);
        var leaseDuration = parseDurationSeconds(ENV_LEADER_ELECTION_LEASE_DURATION, DEFAULT_LEASE_DURATION);
        var renewDeadline = parseDurationSeconds(ENV_LEADER_ELECTION_RENEW_DEADLINE, DEFAULT_RENEW_DEADLINE);
        var retryPeriod = parseDurationSeconds(ENV_LEADER_ELECTION_RETRY_PERIOD, DEFAULT_RETRY_PERIOD);
        var identity = envTrim(ENV_LEADER_ELECTION_IDENTITY);
        var exitOnStopLeading = parseBoolean(ENV_LEADER_ELECTION_EXIT_ON_STOP, false);

        var builder = LeaderElectionConfigurationBuilder.aLeaderElectionConfiguration(leaseName)
                .withLeaseDuration(leaseDuration)
                .withRenewDeadline(renewDeadline)
                .withRetryPeriod(retryPeriod)
                .withExitOnStopLeading(exitOnStopLeading);

        if (leaseNamespace != null && !leaseNamespace.isBlank()) {
            builder.withLeaseNamespace(leaseNamespace);
        }
        if (identity != null && !identity.isBlank()) {
            builder.withIdentity(identity);
        }
        // Structured logging for lease callbacks
        var callbacks =
                new LeaderCallbacks(
                        () -> LOG.info(
                                "operator.leaderElection event=startLeading leaseName={} identity={}",
                                leaseName,
                                identity == null ? "<auto>" : identity),
                        () -> LOG.info(
                                "operator.leaderElection event=stopLeading leaseName={} identity={}",
                                leaseName,
                                identity == null ? "<auto>" : identity),
                        newLeader -> LOG.info(
                                "operator.leaderElection event=newLeader leaseName={} leader={}",
                                leaseName,
                                newLeader));
        builder.withLeaderCallbacks(callbacks);

        return builder.build();
    }

    static GenericRetry buildRetry() {
        var retry = GenericRetry.defaultLimitedExponentialRetry();
        var maxAttempts = parseInt(ENV_RETRY_MAX_ATTEMPTS, retry.getMaxAttempts());
        var initialInterval = parseLong(ENV_RETRY_INITIAL_INTERVAL, retry.getInitialInterval());
        var multiplier = parseDoubleMulti(
                ENV_RETRY_MULTIPLIER, ENV_RETRY_MULTIPLIER_ALT, retry.getIntervalMultiplier());
        var maxIntervalRaw = envTrim(ENV_RETRY_MAX_INTERVAL);

        retry.setMaxAttempts(maxAttempts);
        retry.setInitialInterval(initialInterval);
        retry.setIntervalMultiplier(multiplier);
        if (maxIntervalRaw != null && !maxIntervalRaw.isBlank()) {
            try {
                var maxInterval = Long.parseLong(maxIntervalRaw.trim());
                if (maxInterval <= 0) {
                    retry.withoutMaxInterval();
                    LOG.info(
                            "operator.retry phase=configured maxInterval=unlimited (disabled via env {})",
                            ENV_RETRY_MAX_INTERVAL);
                } else {
                    retry.setMaxInterval(maxInterval);
                }
            } catch (NumberFormatException exception) {
                LOG.warn(
                        "operator.retry phase=invalidMaxInterval value={} error={} usingDefault={}",
                        maxIntervalRaw,
                        exception.toString(),
                        retry.getMaxInterval());
            }
        }
        LOG.info(
                "operator.retry phase=configured maxAttempts={} initialIntervalMs={} multiplier={} maxIntervalMs={}",
                retry.getMaxAttempts(),
                retry.getInitialInterval(),
                retry.getIntervalMultiplier(),
                retry.getMaxInterval());
        return retry;
    }

    private static <T extends io.fabric8.kubernetes.api.model.HasMetadata> void configureController(
            io.javaoperatorsdk.operator.api.config.ControllerConfigurationOverrider<T> overrider,
            GenericRetry retry,
            Set<String> watchedNamespaces,
            String controllerName) {
        overrider.withRetry(retry);
        if (watchedNamespaces != null) {
            if (watchedNamespaces.isEmpty()) {
                overrider.watchingAllNamespaces();
            } else if (watchedNamespaces.size() == 1) {
                var ns = watchedNamespaces.iterator().next();
                overrider.settingNamespace(ns);
            } else {
                overrider.settingNamespaces(watchedNamespaces);
            }
            LOG.info(
                    "operator.controller phase=configuring controller={} watchedNamespaces={}",
                    controllerName,
                    String.join(",", watchedNamespaces));
        } else {
            LOG.info(
                    "operator.controller phase=configuring controller={} watchedNamespaces=<all>",
                    controllerName);
        }
    }

    static boolean isLeaderElectionEnabled() {
        var raw = envTrim(ENV_LEADER_ELECTION_ENABLED);
        if (raw == null) {
            return true;
        }
        return parseBoolean(ENV_LEADER_ELECTION_ENABLED, true);
    }

    static Set<String> parseWatchedNamespaces() {
        var raw = envTrim(ENV_WATCHED_NAMESPACES);
        if (raw == null) {
            raw = envTrim(ENV_NAMESPACE);
        }
        if (raw == null) {
            raw = envTrim(ENV_NAMESPACE_ALT);
        }
        if (raw == null || raw.isBlank()) {
            return null; // null means watch all namespaces (default)
        }
        var namespaces =
                Arrays.stream(raw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toSet());
        if (namespaces.isEmpty()) {
            return null;
        }
        return namespaces;
    }

    private static String envOrDefault(String key, String defaultValue) {
        var value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static String envTrim(String key) {
        var value = System.getenv(key);
        if (value == null) {
            return null;
        }
        var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean parseBoolean(String key, boolean defaultValue) {
        var raw = System.getenv(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        var normalized = raw.trim().toLowerCase();
        if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)) {
            return false;
        }
        LOG.warn(
                "operator.config phase=invalidBoolean key={} value={} usingDefault={}",
                key,
                raw,
                defaultValue);
        return defaultValue;
    }

    private static int parseInt(String key, int defaultValue) {
        var raw = System.getenv(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            LOG.warn(
                    "operator.config phase=invalidInteger key={} value={} error={} usingDefault={}",
                    key,
                    raw,
                    exception.toString(),
                    defaultValue);
            return defaultValue;
        }
    }

    private static long parseLong(String key, long defaultValue) {
        var raw = System.getenv(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException exception) {
            LOG.warn(
                    "operator.config phase=invalidLong key={} value={} error={} usingDefault={}",
                    key,
                    raw,
                    exception.toString(),
                    defaultValue);
            return defaultValue;
        }
    }

    private static double parseDoubleMulti(String key1, String key2, double defaultValue) {
        var raw = System.getenv(key1);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(key2);
        }
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException exception) {
            LOG.warn(
                    "operator.config phase=invalidDouble keys={},{} value={} error={} usingDefault={}",
                    key1,
                    key2,
                    raw,
                    exception.toString(),
                    defaultValue);
            return defaultValue;
        }
    }

    private static Duration parseDurationSeconds(String key, Duration defaultValue) {
        var raw = System.getenv(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            var seconds = Long.parseLong(raw.trim());
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException exception) {
            LOG.warn(
                    "operator.config phase=invalidDuration key={} value={} error={} usingDefault={}s",
                    key,
                    raw,
                    exception.toString(),
                    defaultValue.toSeconds());
            return defaultValue;
        }
    }

    static synchronized void startHealthServer() {
        int port = parseHealthPort();
        try {
            var server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/health", OperatorApplication::handleHealth);
            server.createContext("/healthz", OperatorApplication::handleHealth);
            server.createContext("/ready", OperatorApplication::handleHealth);
            server.createContext("/readyz", OperatorApplication::handleHealth);
            server.createContext("/metrics", OperatorApplication::handleMetrics);
            server.setExecutor(null);
            server.start();
            healthServer = server;
            LOG.info("operator.health phase=started port={} path=/health,/metrics", port);
        } catch (IOException exception) {
            LOG.warn(
                    "operator.health phase=failedToStart port={} error={}",
                    port,
                    exception.toString(),
                    exception);
        }
    }

    static synchronized void stopHealthServer() {
        var server = healthServer;
        if (server != null) {
            try {
                server.stop(0);
                LOG.info("operator.health phase=stopped");
            } catch (Exception exception) {
                LOG.warn(
                        "operator.health phase=stopFailed error={}", exception.toString(), exception);
            } finally {
                healthServer = null;
            }
        }
    }

    private static void handleMetrics(HttpExchange exchange) throws IOException {
        String response = OperatorMetrics.prometheusExposition();
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        String response = "{\"status\":\"UP\"}";
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static int parseHealthPort() {
        var raw = System.getenv(ENV_HEALTH_PORT);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_HEALTH_PORT;
        }
        try {
            int port = Integer.parseInt(raw.trim());
            if (port <= 0 || port > 65535) {
                LOG.warn(
                        "operator.health phase=invalidPort value={} usingDefault={}",
                        raw,
                        DEFAULT_HEALTH_PORT);
                return DEFAULT_HEALTH_PORT;
            }
            return port;
        } catch (NumberFormatException exception) {
            LOG.warn(
                    "operator.health phase=invalidPort value={} error={} usingDefault={}",
                    raw,
                    exception.toString(),
                    DEFAULT_HEALTH_PORT);
            return DEFAULT_HEALTH_PORT;
        }
    }

    private static String readVersion() {
        try (var stream =
                OperatorApplication.class.getClassLoader().getResourceAsStream("version.txt")) {
            if (stream != null) {
                return new String(stream.readAllBytes()).trim();
            }
        } catch (Exception ignored) {
        }
        try (var stream =
                OperatorApplication.class.getClassLoader().getResourceAsStream("operator.version")) {
            if (stream != null) {
                return new String(stream.readAllBytes()).trim();
            }
        } catch (Exception ignored) {
        }
        return System.getenv().getOrDefault("OPERATOR_VERSION", "0.2.0");
    }
}
