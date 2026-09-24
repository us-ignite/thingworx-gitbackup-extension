package org.us_ignite.thingworx.operator.test.integration.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatusBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatusBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.Operator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.awaitility.Durations;
import org.awaitility.core.ThrowingRunnable;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;
import org.us_ignite.thingworx.operator.api.ClusterReference;
import org.us_ignite.thingworx.operator.api.DatabaseSpec;
import org.us_ignite.thingworx.operator.api.ExtensionContentPolicy;
import org.us_ignite.thingworx.operator.api.ImageSet;
import org.us_ignite.thingworx.operator.api.IngressSpec;
import org.us_ignite.thingworx.operator.api.OciArtifactReference;
import org.us_ignite.thingworx.operator.api.SecretKeyReference;
import org.us_ignite.thingworx.operator.api.StorageSpec;
import org.us_ignite.thingworx.operator.api.ThingWorxCluster;
import org.us_ignite.thingworx.operator.api.ThingWorxClusterSpec;
import org.us_ignite.thingworx.operator.api.ThingWorxClusterStatus;
import org.us_ignite.thingworx.operator.api.ThingWorxExtension;
import org.us_ignite.thingworx.operator.api.ThingWorxExtensionSpec;

/**
 * Shared K3s integration harness.
 *
 * <p>Centralises boilerplate previously duplicated in {@code K3sOperatorIT}: K3sContainer
 * definition, kubeconfig → Fabric8 client creation, API readiness await, idempotent CRD install
 * with {@code CRD_INSTALL_LOCK}, {@code resolveCrdsDir} walking candidates, namespace/secret
 * helpers, minimal/extension-ready cluster factories, patch-to-READY, platform/postgres stubs,
 * job success marker, delete-if-exists, operator stop, and generic await helpers.
 *
 * <p>All ITs remain {@code @Tag("integration")} + {@code @Testcontainers} in their own class;
 * they delegate to this utility for hermetic, reusable setup.
 */
public final class K3sSupport {

    private K3sSupport() {}

    // -------------------------------------------------------------------------
    // K3s container
    // -------------------------------------------------------------------------

    public static final DockerImageName K3S_IMAGE =
            DockerImageName.parse("rancher/k3s:v1.31.4-k3s1");

    /** Shared definition – tests declare {@code @Container static final K3sContainer K3S = K3sSupport.newK3sContainer()} or reuse {@code K3S_IMAGE}. */
    public static final K3sContainer K3S = new K3sContainer(K3S_IMAGE);

    /** Factory for a fresh container instance per test class (recommended for hermetic forks). */
    public static K3sContainer newK3sContainer() {
        return new K3sContainer(K3S_IMAGE);
    }

    /**
     * Creates a Fabric8 {@link KubernetesClient} from a running {@link K3sContainer}.
     * Writes kubeconfig to a temp file (for file-reference resolution) and waits for the API
     * to be ready by listing namespaces.
     */
    public static KubernetesClient createClient(K3sContainer k3s) throws IOException {
        String kubeConfigYaml = k3s.getKubeConfigYaml();
        Path tmp = Files.createTempFile("k3s-kubeconfig", ".yaml");
        Files.writeString(tmp, kubeConfigYaml);
        Config config = Config.fromKubeconfig(kubeConfigYaml);
        KubernetesClient client = new KubernetesClientBuilder().withConfig(config).build();
        awaitApiReady(client);
        return client;
    }

    /** Waits for the API to be ready – list namespaces is a cheap readiness probe. */
    public static void awaitApiReady(KubernetesClient client) {
        await().atMost(Durations.TWO_MINUTES)
                .pollInterval(Duration.ofSeconds(2))
                .until(
                        () -> {
                            try {
                                return client.namespaces().list().getItems() != null;
                            } catch (Exception ignored) {
                                return false;
                            }
                        });
    }

    // -------------------------------------------------------------------------
    // CRD handling
    // -------------------------------------------------------------------------

    private static final Object CRD_INSTALL_LOCK = new Object();
    private static volatile boolean crdsInstalled = false;

    public static File resolveCrdsDir() {
        for (String candidate :
                List.of(
                        "charts/thingworx-operator/crds",
                        "../../charts/thingworx-operator/crds",
                        "../charts/thingworx-operator/crds",
                        "thingworx-jgit-extension/charts/thingworx-operator/crds")) {
            File f = new File(candidate);
            if (f.isDirectory()) return f.getAbsoluteFile();
        }
        Path cur = Path.of("").toAbsolutePath();
        for (int i = 0; i < 5; i++) {
            Path p = cur.resolve("charts/thingworx-operator/crds");
            if (Files.isDirectory(p)) return p.toFile();
            cur = cur.getParent();
            if (cur == null) break;
        }
        throw new IllegalStateException(
                "CRD directory not found; tried relative paths from " + Path.of("").toAbsolutePath());
    }

    public static void awaitCrdEstablished(KubernetesClient client, String name) {
        await().atMost(Duration.ofMinutes(1))
                .untilAsserted(
                        () -> {
                            CustomResourceDefinition crd =
                                    client.apiextensions()
                                            .v1()
                                            .customResourceDefinitions()
                                            .withName(name)
                                            .get();
                            assertThat(crd).isNotNull();
                            assertThat(crd.getStatus()).isNotNull();
                            assertThat(crd.getStatus().getConditions()).isNotEmpty();
                        });
    }

    /**
     * Idempotent CRD installer guarded by {@link #CRD_INSTALL_LOCK} so parallel forks
     * ({@code forkCount=1C}) do not race on global CRD creation. Also re-uses an already-established
     * CRD when running ordered methods within the same JVM (PER_CLASS).
     */
    public static void installCrdsIdempotently(KubernetesClient client) throws Exception {
        synchronized (CRD_INSTALL_LOCK) {
            File crdDir = resolveCrdsDir();
            assertThat(crdDir).isDirectory();
            for (File crd :
                    List.of(
                            new File(crdDir, "thingworx.us-ignite.org_thingworxclusters.yaml"),
                            new File(crdDir, "thingworx.us-ignite.org_thingworxextensions.yaml"))) {
                assertThat(crd).exists();
                try (var is = new FileInputStream(crd)) {
                    List<HasMetadata> items = client.load(is).items();
                    for (HasMetadata item : items) {
                        if (item instanceof CustomResourceDefinition) {
                            CustomResourceDefinition existing =
                                    client.apiextensions()
                                            .v1()
                                            .customResourceDefinitions()
                                            .withName(item.getMetadata().getName())
                                            .get();
                            if (existing == null) {
                                client.resource(item).create();
                            } else {
                                client.resource(item).update();
                            }
                        } else {
                            client.resource(item).serverSideApply();
                        }
                    }
                }
            }
            if (!crdsInstalled) {
                await().atMost(Duration.ofMinutes(1))
                        .untilAsserted(
                                () -> {
                                    for (String name :
                                            List.of(
                                                    "thingworxclusters.thingworx.us-ignite.org",
                                                    "thingworxextensions.thingworx.us-ignite.org")) {
                                        CustomResourceDefinition crd =
                                                client.apiextensions()
                                                        .v1()
                                                        .customResourceDefinitions()
                                                        .withName(name)
                                                        .get();
                                        assertThat(crd).isNotNull();
                                        assertThat(crd.getStatus()).isNotNull();
                                        assertThat(crd.getStatus().getConditions()).isNotEmpty();
                                    }
                                });
                crdsInstalled = true;
            } else {
                await().atMost(Duration.ofSeconds(10))
                        .untilAsserted(
                                () ->
                                        assertThat(
                                                        client.genericKubernetesResources(
                                                                        "thingworx.us-ignite.org/v1alpha1",
                                                                        "ThingWorxCluster")
                                                                .inNamespace("thingworx")
                                                                .list()
                                                                .getItems())
                                                .isNotNull());
            }
            assertThat(
                            client.genericKubernetesResources(
                                            "thingworx.us-ignite.org/v1alpha1", "ThingWorxCluster")
                                    .inNamespace("thingworx")
                                    .list()
                                    .getItems())
                    .isNotNull();
        }
    }

    /** Visible for tests – resets the JVM-wide installed flag (rarely needed). */
    public static void resetCrdsInstalledForTests() {
        synchronized (CRD_INSTALL_LOCK) {
            crdsInstalled = false;
        }
    }

    // -------------------------------------------------------------------------
    // Namespace / Secret helpers
    // -------------------------------------------------------------------------

    public static void createNamespace(KubernetesClient client, String name) {
        if (client.namespaces().withName(name).get() != null) return;
        client.namespaces()
                .resource(
                        new io.fabric8.kubernetes.api.model.NamespaceBuilder()
                                .withNewMetadata()
                                .withName(name)
                                .endMetadata()
                                .build())
                .create();
    }

    public static void createSecret(
            KubernetesClient client, String namespace, String name, String key, String value) {
        var existing = client.secrets().inNamespace(namespace).withName(name).get();
        if (existing != null) return;
        client.secrets()
                .inNamespace(namespace)
                .resource(
                        new SecretBuilder()
                                .withNewMetadata()
                                .withName(name)
                                .withNamespace(namespace)
                                .endMetadata()
                                .withType("Opaque")
                                .addToStringData(key, value)
                                .build())
                .create();
    }

    // -------------------------------------------------------------------------
    // Cluster / Extension factories (verbatim from K3sOperatorIT)
    // -------------------------------------------------------------------------

    public static ThingWorxCluster minimalCluster(String name) {
        var images = new ImageSet();
        images.setPlatform("ghcr.io/us-ignite/thingworx/platform-postgres:10.1.2");
        images.setDatabaseInit("ghcr.io/us-ignite/thingworx/postgres-init:10.1.2");
        images.setSecurityCli("ghcr.io/us-ignite/thingworx/security-tool:10.1.2");

        var database = new DatabaseSpec();
        database.setInternal(true);
        database.setImage("postgres:16");
        database.setDatabase("thingworx");
        database.setSchema("thingworx");
        database.setUsername("thingworx");
        database.setAdminUsername("postgres");
        var creds = new SecretKeyReference();
        creds.setName("thingworx-database");
        creds.setKey("password");
        database.setCredentials(creds);
        var adminCreds = new SecretKeyReference();
        adminCreds.setName("thingworx-database-admin");
        adminCreds.setKey("password");
        database.setAdminCredentials(adminCreds);

        var storage = new StorageSpec();
        storage.setStorageClassName("local-path");
        storage.setSharedStorageSize("1Gi");
        storage.setComponentStorageSize("1Gi");

        var admin = new SecretKeyReference();
        admin.setName("thingworx-platform-admin");
        admin.setKey("password");
        var keystore = new SecretKeyReference();
        keystore.setName("thingworx-keystore");
        keystore.setKey("password");

        var spec = new ThingWorxClusterSpec();
        spec.setImages(images);
        spec.setDatabase(database);
        spec.setStorage(storage);
        spec.setIngress(new IngressSpec());
        spec.setPlatformAdminPassword(admin);
        spec.setKeystorePassword(keystore);
        spec.setEnableHA(false);

        var cluster = new ThingWorxCluster();
        cluster.setMetadata(new ObjectMetaBuilder().withName(name).withNamespace("thingworx").build());
        cluster.setSpec(spec);
        return cluster;
    }

    public static ThingWorxCluster extensionReadyCluster(String name) {
        ThingWorxCluster base = minimalCluster(name);
        base.getSpec().getImages().setExtensionInstaller("ghcr.io/us-ignite/thingworx/extension-installer:10.1.2");
        var importKey = new SecretKeyReference();
        importKey.setName("extension-import-key");
        importKey.setKey("appKey");
        base.getSpec().setExtensionImportAppKey(importKey);
        base.getSpec().setExtensionImportPolicy(new ExtensionContentPolicy());
        return base;
    }

    public static ThingWorxExtension extension(
            String name, String clusterName, List<String> dependsOn) {
        String digest =
                "alpha".equals(name)
                        ? "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        : "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        if ("beta".equals(name) && dependsOn.isEmpty()) {
            digest = "sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
        }
        var artifact = new OciArtifactReference();
        artifact.setRepository("ghcr.io/example/extensions/" + name);
        artifact.setDigest(digest);
        artifact.setExpectedName(name);
        artifact.setExpectedVersion("1.0.0");
        var ref = new ClusterReference();
        ref.setName(clusterName);
        var spec = new ThingWorxExtensionSpec();
        spec.setClusterRef(ref);
        spec.setArtifact(artifact);
        spec.setDependsOn(dependsOn == null ? List.of() : dependsOn);
        var ext = new ThingWorxExtension();
        ext.setMetadata(new ObjectMetaBuilder().withName(name).withNamespace("thingworx").build());
        ext.setSpec(spec);
        return ext;
    }

    // -------------------------------------------------------------------------
    // Patch / stub helpers
    // -------------------------------------------------------------------------

    public static void patchClusterToReady(KubernetesClient client, String clusterName) {
        patchClusterToReady(client, "thingworx", clusterName);
    }

    public static void patchClusterToReady(
            KubernetesClient client, String namespace, String clusterName) {
        var fetched =
                client.resources(ThingWorxCluster.class).inNamespace(namespace).withName(clusterName).get();
        if (fetched == null) return;
        var status = fetched.getStatus();
        if (status == null) status = new ThingWorxClusterStatus();
        status.setPhase("READY");
        status.setMessage("Manually patched to READY for extension IT (local-path stub).");
        Long genTmp = fetched.getMetadata().getGeneration();
        if (genTmp == null) genTmp = 1L;
        final Long gen = genTmp;
        status.setObservedGeneration(gen);
        status.setCurrentVersion(fetched.getSpec().getImages().getPlatform());
        status.setMode(fetched.getSpec().isEnableHA() ? "HA" : "SINGLE_NODE");
        fetched.setStatus(status);
        try {
            client.resource(fetched).updateStatus();
        } catch (Exception e) {
            try {
                client.resource(fetched).update();
            } catch (Exception nested) {
                client.resources(ThingWorxCluster.class)
                        .inNamespace(namespace)
                        .withName(clusterName)
                        .editStatus(
                                c -> {
                                    if (c.getStatus() == null) c.setStatus(new ThingWorxClusterStatus());
                                    c.getStatus().setPhase("READY");
                                    c.getStatus().setObservedGeneration(gen);
                                    return c;
                                });
            }
        }
    }

    public static void ensurePlatformAndPostgresStubsReady(
            KubernetesClient client, String clusterName) {
        ensurePlatformAndPostgresStubsReady(client, "thingworx", clusterName);
    }

    public static void ensurePlatformAndPostgresStubsReady(
            KubernetesClient client, String namespace, String clusterName) {
        for (String component : List.of("platform", "postgres")) {
            String stsName = clusterName + "-" + component;
            var existing = client.apps().statefulSets().inNamespace(namespace).withName(stsName).get();
            if (existing != null) continue;
            var sts =
                    new StatefulSetBuilder()
                            .withNewMetadata()
                            .withName(stsName)
                            .withNamespace(namespace)
                            .withLabels(
                                    Map.of(
                                            "app.kubernetes.io/instance",
                                            clusterName,
                                            "app.kubernetes.io/component",
                                            component,
                                            "app.kubernetes.io/managed-by",
                                            "thingworx-operator"))
                            .endMetadata()
                            .withNewSpec()
                            .withReplicas(1)
                            .withServiceName(stsName)
                            .withSelector(
                                    new io.fabric8.kubernetes.api.model.LabelSelectorBuilder()
                                            .withMatchLabels(
                                                    Map.of(
                                                            "app.kubernetes.io/instance",
                                                            clusterName,
                                                            "app.kubernetes.io/component",
                                                            component))
                                            .build())
                            .withNewTemplate()
                            .withNewMetadata()
                            .withLabels(
                                    Map.of(
                                            "app.kubernetes.io/instance",
                                            clusterName,
                                            "app.kubernetes.io/component",
                                            component))
                            .endMetadata()
                            .withNewSpec()
                            .addNewContainer()
                            .withName(component)
                            .withImage("busybox:latest")
                            .endContainer()
                            .endSpec()
                            .endTemplate()
                            .endSpec()
                            .build();
            sts.setStatus(
                    new StatefulSetStatusBuilder()
                            .withReplicas(1)
                            .withReadyReplicas(1)
                            .withAvailableReplicas(1)
                            .withCurrentRevision("stub")
                            .withUpdateRevision("stub")
                            .build());
            var created = client.resource(sts).create();
            try {
                created.setStatus(sts.getStatus());
                client.resource(created).updateStatus();
            } catch (Exception ignored) {
                try {
                    client.resource(sts).update();
                } catch (Exception ignored2) {
                }
            }
        }
    }

    public static void markJobSucceeded(
            KubernetesClient client, String namespace, String jobName) {
        var job = client.batch().v1().jobs().inNamespace(namespace).withName(jobName).get();
        if (job == null) return;
        job.setStatus(new JobStatusBuilder().withSucceeded(1).build());
        try {
            client.resource(job).updateStatus();
        } catch (Exception e) {
            try {
                client.resource(job).update();
            } catch (Exception nested) {
                client.batch()
                        .v1()
                        .jobs()
                        .inNamespace(namespace)
                        .withName(jobName)
                        .editStatus(
                                j -> {
                                    j.setStatus(new JobStatusBuilder().withSucceeded(1).build());
                                    return j;
                                });
            }
        }
    }

    public static <T extends HasMetadata> void deleteIfExists(
            KubernetesClient client, Class<T> type, String namespace, String name) {
        try {
            var res = client.resources(type).inNamespace(namespace).withName(name).get();
            if (res != null) {
                client.resource(res).delete();
                await().atMost(Duration.ofSeconds(10))
                        .pollInterval(Duration.ofMillis(500))
                        .until(
                                () -> client.resources(type).inNamespace(namespace).withName(name).get() == null);
            }
        } catch (Exception ignored) {
        }
        try {
            client.batch()
                    .v1()
                    .jobs()
                    .inNamespace(namespace)
                    .withLabel("thingworx.us-ignite.org/extension", name)
                    .delete();
        } catch (Exception ignored) {
        }
    }

    public static void ensureOperatorStopped(Operator operator) throws InterruptedException {
        if (operator != null) {
            try {
                operator.stop();
            } catch (Exception ignored) {
            }
            Thread.sleep(2000);
        }
    }

    // -------------------------------------------------------------------------
    // Await helpers with sensible defaults
    // -------------------------------------------------------------------------

    /** Default poll interval for integration awaits. */
    public static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(2);

    /** Default timeout for most integration awaits. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(2);

    public static void awaitAsserted(ThrowingRunnable assertion) {
        await().atMost(DEFAULT_TIMEOUT).pollInterval(DEFAULT_POLL_INTERVAL).untilAsserted(assertion);
    }

    public static void awaitAsserted(Duration atMost, ThrowingRunnable assertion) {
        await().atMost(atMost).pollInterval(DEFAULT_POLL_INTERVAL).untilAsserted(assertion);
    }

    public static void awaitAsserted(Duration atMost, Duration pollInterval, ThrowingRunnable assertion) {
        await().atMost(atMost).pollInterval(pollInterval).untilAsserted(assertion);
    }

    public static void awaitCondition(Duration atMost, Duration pollInterval, java.util.concurrent.Callable<Boolean> condition) {
        await().atMost(atMost).pollInterval(pollInterval).until(condition);
    }
}
