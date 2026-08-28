package org.us_ignite.thingworx.operator.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
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
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;
import org.us_ignite.thingworx.operator.api.DatabaseSpec;
import org.us_ignite.thingworx.operator.api.ImageSet;
import org.us_ignite.thingworx.operator.api.IngressSpec;
import org.us_ignite.thingworx.operator.api.SecretKeyReference;
import org.us_ignite.thingworx.operator.api.StorageSpec;
import org.us_ignite.thingworx.operator.api.ThingWorxCluster;
import org.us_ignite.thingworx.operator.api.ThingWorxClusterSpec;
import org.us_ignite.thingworx.operator.reconcile.ThingWorxClusterReconciler;

/**
 * Single-container Kubernetes integration test.
 *
 * <p>Spins up a {@code rancher/k3s} distribution via Testcontainers ({@code K3sContainer}). k3s is
 * the best fit for a single-container control plane: it is a certified Kubernetes distribution that
 * runs the API server, scheduler, controller-manager and an embedded etcd/sqlite inside one Docker
 * container, without requiring a separate VM (minikube) or extra nodes. Compared to {@code kind} or
 * {@code k0s}, the Testcontainers {@code k3s} module is stable, small (~150&nbsp;MiB), and already
 * exposes a ready-to-use {@code kubeconfig} that Fabric8 7.8 can consume. {@code kind} also works
 * but needs Docker-in-Docker and a second container for the worker, while {@code minikube} is
 * heavier and slower for CI.
 *
 * <p>The test verifies the full install path: 1) k3s becomes ready, 2) CRDs from {@code
 * charts/thingworx-operator/crds} are applied and become established, 3) the operator is started
 * in-process (JOSDK {@code Operator}) against that cluster, 4) a minimal single-node {@code
 * ThingWorxCluster} is created and the reconciler creates the expected precheck resources
 * (ConfigMap, PVC, Service). This mirrors {@code devSetup} but without Helm or an external image
 * registry.
 */
@Tag("integration")
@Testcontainers
class K3sOperatorIT {

    // k3s is the lightest certified distribution that fits the "single container" requirement.
    // Testcontainers' K3sContainer already waits for the API to be ready and writes a kubeconfig.
    // v1.31 satisfies Fabric8 7.8's supported skew while remaining small.
    @Container
    static final K3sContainer K3S =
            new K3sContainer(DockerImageName.parse("rancher/k3s:v1.31.4-k3s1"));

    private static KubernetesClient client;
    private static Operator operator;

    @BeforeAll
    static void startClient() throws IOException {
        // K3sContainer exposes the kubeconfig as a String; Fabric8 can parse it directly.
        String kubeConfigYaml = K3S.getKubeConfigYaml();
        // Write to a temp file so Config.fromKubeconfig can also resolve file references if needed.
        Path tmp = Files.createTempFile("k3s-kubeconfig", ".yaml");
        Files.writeString(tmp, kubeConfigYaml);
        // Fabric8 7.8: Config.fromKubeconfig(String) parses YAML, but also supports file.
        Config config = Config.fromKubeconfig(kubeConfigYaml);
        // K3s inside Testcontainers is reachable via the mapped port, not the internal 6443.
        // getKubeConfigYaml already contains the correct external host/port, so no override needed.
        client = new KubernetesClientBuilder().withConfig(config).build();
        // Wait for the API to be ready – list namespaces is a cheap readiness probe.
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

    @AfterAll
    static void stopOperator() {
        if (operator != null) {
            operator.stop();
        }
        if (client != null) {
            client.close();
        }
    }

    @Test
    void k3sStartsAndCrdCanBeInstalled() throws Exception {
        assertThat(K3S.isRunning()).isTrue();
        // k3s exposes its own namespace list – sanity check we have a real API server.
        assertThat(client.namespaces().list().getItems()).isNotEmpty();

        // Install CRDs from the Helm chart – these are the release contracts.
        // Resolve charts directory relative to the project root (works both from IDE and Gradle).
        File crdDir = resolveCrdsDir();
        assertThat(crdDir).isDirectory();
        for (File crd :
                List.of(
                        new File(crdDir, "thingworx.us-ignite.org_thingworxclusters.yaml"),
                        new File(crdDir, "thingworx.us-ignite.org_thingworxextensions.yaml"))) {
            assertThat(crd).exists();
            // Fabric8 load will create the CRD; serverSideApply is idempotent, fall back to create.
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

        // Wait for CRDs to be Established – the API server sets the Established condition.
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

        // Verify we can list the new custom resource type (empty list is fine).
        assertThat(
                        client.genericKubernetesResources(
                                        "thingworx.us-ignite.org/v1alpha1", "ThingWorxCluster")
                                .inNamespace("thingworx")
                                .list()
                                .getItems())
                .isEmpty();
    }

    @Test
    void operatorReconcilesSingleNodeClusterInK3s() throws Exception {
        // Ensure CRDs are present – reuse the same helper.
        k3sStartsAndCrdCanBeInstalled();

        // Create namespaces the operator expects.
        createNamespace("thingworx");
        createNamespace("thingworx-operator");

        // Create required Secrets for the test cluster (the reconciler validates their presence).
        createSecret("thingworx", "thingworx-platform-admin", "password", "test-admin-pass");
        createSecret("thingworx", "thingworx-keystore", "password", "test-keystore-pass");
        createSecret("thingworx", "thingworx-database", "password", "test-db-pass");
        createSecret("thingworx", "thingworx-database-admin", "password", "test-admin-db-pass");

        // Start the operator in-process against the k3s cluster. This is the same code path as
        // OperatorApplication.main but with an explicit client that points at K3sContainer.
        operator = new Operator(over -> over.withKubernetesClient(client));
        operator.register(new ThingWorxClusterReconciler(client));
        // ThingWorxExtensionReconciler is not needed for this smoke test.
        operator.start();
        // Give the informers a moment to start.
        Thread.sleep(3000);

        // Create a minimal valid single-node cluster (external DB false => uses internal postgres,
        // but we satisfy adminCredentials; storage uses k3s' default StorageClass "local-path").
        ThingWorxCluster cluster = minimalCluster("k3s-demo");

        client.resource(cluster).create();

        // The reconciler is async – wait for the expected precheck resources to appear.
        // Phase should move from null → DATABASE after precheck, then the operator will create
        // ConfigMap, PVC (local-path), and Service.
        await().atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(
                        () -> {
                            ThingWorxCluster observed =
                                    client.resources(ThingWorxCluster.class)
                                            .inNamespace("thingworx")
                                            .withName("k3s-demo")
                                            .get();
                            assertThat(observed).isNotNull();
                            assertThat(observed.getStatus()).isNotNull();
                            assertThat(observed.getStatus().getPhase())
                                    .isIn(
                                            "DATABASE",
                                            "READY",
                                            "PLATFORM_PRIMARY",
                                            "PLATFORM_CLUSTER");
                        });

        // Verify precheck side-effects: ConfigMap and PVC exist with expected names.
        // These are the first resources ThingWorxResources.precheck creates and are independent of
        // platform images, so they are the best stable smoke signal.
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(
                        () -> {
                            var cm =
                                    client.configMaps()
                                            .inNamespace("thingworx")
                                            .withName("k3s-demo-runtime")
                                            .get();
                            assertThat(cm).isNotNull();
                            assertThat(cm.getData())
                                    .containsEntry("database-host", "k3s-demo-postgres");

                            var pvc =
                                    client.persistentVolumeClaims()
                                            .inNamespace("thingworx")
                                            .withName("k3s-demo-shared")
                                            .get();
                            assertThat(pvc).isNotNull();
                            // k3s local-path is the default storage class; the spec uses
                            // "local-path" in this test.
                            assertThat(pvc.getSpec().getStorageClassName()).isEqualTo("local-path");

                            var svc =
                                    client.services()
                                            .inNamespace("thingworx")
                                            .withName("k3s-demo-platform")
                                            .get();
                            assertThat(svc).isNotNull();
                        });
    }

    private void createNamespace(String name) {
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

    private void createSecret(String namespace, String name, String key, String value) {
        var existing = client.secrets().inNamespace(namespace).withName(name).get();
        if (existing != null) return;
        client.secrets()
                .inNamespace(namespace)
                .resource(
                        new io.fabric8.kubernetes.api.model.SecretBuilder()
                                .withNewMetadata()
                                .withName(name)
                                .withNamespace(namespace)
                                .endMetadata()
                                .withType("Opaque")
                                .addToStringData(key, value)
                                .build())
                .create();
    }

    private ThingWorxCluster minimalCluster(String name) {
        var images = new ImageSet();
        images.setPlatform("ghcr.io/us-ignite/thingworx/platform-postgres:10.1.2");
        images.setDatabaseInit("ghcr.io/us-ignite/thingworx/postgres-init:10.1.2");
        images.setSecurityCli("ghcr.io/us-ignite/thingworx/security-tool:10.1.2");
        // Defaults for ignite/cx/zookeeper/haProxy are already valid in ImageSet.

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
        // k3s ships with local-path provisioner; use it for the smoke test.
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
        cluster.setMetadata(
                new ObjectMetaBuilder().withName(name).withNamespace("thingworx").build());
        cluster.setSpec(spec);
        return cluster;
    }

    private File resolveCrdsDir() {
        // Gradle runs with rootProject dir as working dir when invoked as
        // :apps:thingworx-operator:integrationTest.
        // Try a few locations to be IDE-friendly.
        for (String candidate :
                List.of(
                        "charts/thingworx-operator/crds",
                        "../../charts/thingworx-operator/crds",
                        "../charts/thingworx-operator/crds",
                        "thingworx-jgit-extension/charts/thingworx-operator/crds")) {
            File f = new File(candidate);
            if (f.isDirectory()) return f.getAbsoluteFile();
        }
        // Fallback: walk up from current dir
        Path cur = Path.of("").toAbsolutePath();
        for (int i = 0; i < 5; i++) {
            Path p = cur.resolve("charts/thingworx-operator/crds");
            if (Files.isDirectory(p)) return p.toFile();
            cur = cur.getParent();
            if (cur == null) break;
        }
        throw new IllegalStateException(
                "CRD directory not found; tried relative paths from "
                        + Path.of("").toAbsolutePath());
    }
}
