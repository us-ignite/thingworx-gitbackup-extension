package org.us_ignite.thingworx.testcontainers;

import java.net.http.HttpClient;
import org.testcontainers.containers.Network;

/** Reusable Postgres, DB-init, and ThingWorx Testcontainers stack. */
public class ThingWorxTestStack implements AutoCloseable {
    public final Network network;
    public final Postgres postgres;
    public final DBInit dbInit;
    public final ThingWorxContainer thingworx;
    private final java.nio.file.Path sharedStorage;
    private final org.testcontainers.containers.GenericContainer<?> securityInit;
    public final HttpClient httpClient = HttpClient.newBuilder().build();

    public ThingWorxTestStack(TestingCredentials credentials) throws Exception {
        this(TestImages.dbInitImage(), TestImages.platformImage(), credentials, true);
    }

    public ThingWorxTestStack(
            String dbInitImage, String platformImage, TestingCredentials credentials)
            throws Exception {
        this(dbInitImage, platformImage, credentials, true);
    }

    protected ThingWorxTestStack(
            String dbInitImage,
            String platformImage,
            TestingCredentials credentials,
            boolean autoStart)
            throws Exception {
        network = Network.newNetwork();
        postgres = new Postgres(network, credentials);
        dbInit = new DBInit(dbInitImage, postgres, network, credentials);
        boolean needsSecurity = platformImage != null && platformImage.contains("ghcr.io/us-ignite/thingworx/platform-postgres");
        if (needsSecurity) {
            sharedStorage = java.nio.file.Files.createTempDirectory("twx-storage");
            try {
                var chmodProcess =
                    new ProcessBuilder("chmod", "777", sharedStorage.toAbsolutePath().toString())
                        .start();
                try {
                    chmodProcess.waitFor();
                } finally {
                    chmodProcess.destroy();
                }
            } catch (Exception ignored) {
            }
            sharedStorage.toFile().deleteOnExit();
            String securityImage = TestImages.securityImageForPlatform(platformImage);
                var configuredSecurityInit =
                    new org.testcontainers.containers.GenericContainer<>(securityImage);
                configuredSecurityInit.withNetwork(network);
                configuredSecurityInit.withEnv("KEYSTORE", "true");
                configuredSecurityInit.withEnv("KEYSTORE_PASSWORD", "thingworx-keystore-password");
                configuredSecurityInit.withEnv("KEYSTORE_PASSWORD_FILE_PATH", "/ThingworxStorage");
                configuredSecurityInit.withEnv("KEYSTORE_FILE_PATH", "/ThingworxStorage");
                configuredSecurityInit.withEnv(
                    "CUSTOM_SECRET_LIST", "encrypt.db.password:TWX_DATABASE_PASSWORD");
                configuredSecurityInit.withEnv("TWX_DATABASE_PASSWORD", credentials.twxDatabasePass);
                configuredSecurityInit.withEnv(
                    "SECRET_PROVISIONING_APP_KEY", "test-provisioning-key-12345");
                configuredSecurityInit.withFileSystemBind(
                    sharedStorage.toAbsolutePath().toString(),
                    "/ThingworxStorage",
                    org.testcontainers.containers.BindMode.READ_WRITE);
                configuredSecurityInit.waitingFor(
                    org.testcontainers.containers.wait.strategy.Wait.forLogMessage(
                            ".*encrypt.db.password stored.*", 1)
                        .withStartupTimeout(java.time.Duration.ofMinutes(2)));
                securityInit = configuredSecurityInit;
            thingworx =
                    new ThingWorxContainer(
                            platformImage, dbInit, postgres, network, credentials, "postgresql", "thingworx", sharedStorage);
            thingworx.dependsOn(securityInit);
        } else {
            sharedStorage = null;
            securityInit = null;
            thingworx = new ThingWorxContainer(platformImage, dbInit, postgres, network, credentials);
        }
        if (autoStart) start();
    }

    public void start() throws Exception {
        try {
            postgres.start();
            dbInit.start();
            if (securityInit != null) securityInit.start();
            thingworx.start();
        } catch (Exception e) {
            try {
                close();
            } catch (Exception closeEx) {
                e.addSuppressed(closeEx);
            }
            throw e;
        }
    }

    @Override
    public void close() {
        if (thingworx != null) thingworx.close();
        if (securityInit != null) securityInit.close();
        if (dbInit != null) dbInit.close();
        if (postgres != null) postgres.close();
        if (network != null) network.close();
        if (sharedStorage != null) {
            try {
                try (var walk = java.nio.file.Files.walk(sharedStorage)) {
                    walk.sorted(java.util.Comparator.reverseOrder())
                            .forEach(
                                    p -> {
                                        try {
                                            java.nio.file.Files.deleteIfExists(p);
                                        } catch (Exception ignored) {
                                        }
                                    });
                }
            } catch (Exception ignored) {
            }
        }
    }
}
