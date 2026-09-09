package org.us_ignite.thingworx.operator.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.k3s.K3sContainer;
import org.us_ignite.thingworx.operator.api.ThingWorxCluster;
import org.us_ignite.thingworx.operator.api.ThingWorxExtension;
import org.us_ignite.thingworx.operator.test.integration.support.K3sSupport;
import org.us_ignite.thingworx.operator.reconcile.ThingWorxClusterReconciler;
import org.us_ignite.thingworx.operator.reconcile.ThingWorxExtensionReconciler;

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
 *
 * <p>Delegates all harness boilerplate to {@link K3sSupport}.
 */
@Tag("integration")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class K3sOperatorIT {

    @Container
    static final K3sContainer K3S = K3sSupport.newK3sContainer();

    private static KubernetesClient client;
    private static Operator operator;

    @BeforeAll
    static void startClient() throws IOException {
        client = K3sSupport.createClient(K3S);
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
    @Order(1)
    void k3sStartsAndCrdCanBeInstalled() throws Exception {
        assertThat(K3S.isRunning()).isTrue();
        assertThat(client.namespaces().list().getItems()).isNotEmpty();

        installCrdsIdempotently();
    }

    @Test
    @Order(2)
    void operatorReconcilesSingleNodeClusterInK3s() throws Exception {
        k3sStartsAndCrdCanBeInstalled();

        createNamespace("thingworx");
        createNamespace("thingworx-operator");

        createSecret("thingworx", "thingworx-platform-admin", "password", "test-admin-pass");
        createSecret("thingworx", "thingworx-keystore", "password", "test-keystore-pass");
        createSecret("thingworx", "thingworx-database", "password", "test-db-pass");
        createSecret("thingworx", "thingworx-database-admin", "password", "test-admin-db-pass");

        ensureOperatorStopped();
        operator = new Operator(over -> over.withKubernetesClient(client));
        operator.register(new ThingWorxClusterReconciler(client));
        operator.start();
        Thread.sleep(3000);

        ThingWorxCluster cluster = minimalCluster("k3s-demo");

        client.resource(cluster).create();

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
                            assertThat(pvc.getSpec().getStorageClassName()).isEqualTo("local-path");

                            var svc =
                                    client.services()
                                            .inNamespace("thingworx")
                                            .withName("k3s-demo-platform")
                                            .get();
                            assertThat(svc).isNotNull();
                        });
    }

    @Test
    @Order(3)
    void operatorReconcilesExtensionInK3s() throws Exception {
        k3sStartsAndCrdCanBeInstalled();
        createNamespace("thingworx");
        createNamespace("thingworx-operator");

        createSecret("thingworx", "thingworx-platform-admin", "password", "test-admin-pass");
        createSecret("thingworx", "thingworx-keystore", "password", "test-keystore-pass");
        createSecret("thingworx", "thingworx-database", "password", "test-db-pass");
        createSecret("thingworx", "thingworx-database-admin", "password", "test-admin-db-pass");
        createSecret("thingworx", "extension-import-key", "appKey", "test-app-key");

        ensureOperatorStopped();
        operator = new Operator(over -> over.withKubernetesClient(client));
        operator.register(new ThingWorxClusterReconciler(client));
        operator.register(new ThingWorxExtensionReconciler(client));
        operator.start();
        Thread.sleep(3000);

        deleteIfExists(ThingWorxExtension.class, "thingworx", "beta");
        deleteIfExists(ThingWorxExtension.class, "thingworx", "alpha");
        deleteIfExists(ThingWorxCluster.class, "thingworx", "ext-demo");

        ThingWorxCluster cluster = extensionReadyCluster("ext-demo");
        client.resource(cluster).create();

        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(
                        () -> {
                            var fetched =
                                    client.resources(ThingWorxCluster.class)
                                            .inNamespace("thingworx")
                                            .withName("ext-demo")
                                            .get();
                            assertThat(fetched).isNotNull();
                        });

        patchClusterToReady("ext-demo");
        ensurePlatformAndPostgresStubsReady("ext-demo");

        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(
                        () -> {
                            var observed =
                                    client.resources(ThingWorxCluster.class)
                                            .inNamespace("thingworx")
                                            .withName("ext-demo")
                                            .get();
                            assertThat(observed).isNotNull();
                            assertThat(observed.getStatus()).isNotNull();
                            if (!"READY".equals(observed.getStatus().getPhase())) {
                                patchClusterToReady("ext-demo");
                            }
                            var rechecked =
                                    client.resources(ThingWorxCluster.class)
                                            .inNamespace("thingworx")
                                            .withName("ext-demo")
                                            .get();
                            assertThat(rechecked.getStatus().getPhase()).isEqualTo("READY");
                        });

        ThingWorxExtension alpha = extension("alpha", "ext-demo", List.of());
        ThingWorxExtension beta = extension("beta", "ext-demo", List.of("alpha"));
        client.resource(alpha).create();
        client.resource(beta).create();

        await().atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(
                        () -> {
                            var a =
                                    client.resources(ThingWorxExtension.class)
                                            .inNamespace("thingworx")
                                            .withName("alpha")
                                            .get();
                            assertThat(a).isNotNull();
                            assertThat(a.getStatus()).isNotNull();
                            assertThat(a.getStatus().getPhase()).isIn("Importing", "Updating");
                            assertThat(a.getStatus().getInstallerJob()).isNotNull();
                            assertThat(a.getStatus().getLastAttemptedFingerprint()).isNotNull();
                            var jobAlpha =
                                    client.batch()
                                            .v1()
                                            .jobs()
                                            .inNamespace("thingworx")
                                            .withName(a.getStatus().getInstallerJob())
                                            .get();
                            assertThat(jobAlpha).isNotNull();
                            assertThat(jobAlpha.getMetadata().getLabels())
                                    .containsEntry("thingworx.us-ignite.org/extension", "alpha");
                            assertThat(jobAlpha.getMetadata().getLabels())
                                    .containsEntry("app.kubernetes.io/managed-by", "thingworx-operator");
                            assertThat(
                                            jobAlpha
                                                    .getSpec()
                                                    .getTemplate()
                                                    .getSpec()
                                                    .getContainers()
                                                    .getFirst()
                                                    .getImage())
                                    .isEqualTo("ghcr.io/us-ignite/thingworx/extension-installer:10.1.2");
                            var env =
                                    jobAlpha
                                            .getSpec()
                                            .getTemplate()
                                            .getSpec()
                                            .getContainers()
                                            .getFirst()
                                            .getEnv();
                            assertThat(env).extracting(e -> e.getName())
                                    .contains("OCI_ARTIFACT", "OCI_DIGEST", "INSTALL_FINGERPRINT", "THINGWORX_EXTENSION_APP_KEY");
                            var b =
                                    client.resources(ThingWorxExtension.class)
                                            .inNamespace("thingworx")
                                            .withName("beta")
                                            .get();
                            assertThat(b).isNotNull();
                            assertThat(b.getStatus()).isNotNull();
                            assertThat(b.getStatus().getPhase())
                                    .isEqualTo("WaitingForDependencies");
                            assertThat(b.getStatus().getMessage()).contains("prerequisite");
                        });

        {
            var a =
                    client.resources(ThingWorxExtension.class)
                            .inNamespace("thingworx")
                            .withName("alpha")
                            .get();
            String jobName = a.getStatus().getInstallerJob();
            markJobSucceeded("thingworx", jobName);
        }

        await().atMost(Duration.ofMinutes(1))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(
                        () -> {
                            var cl =
                                    client.resources(ThingWorxCluster.class)
                                            .inNamespace("thingworx")
                                            .withName("ext-demo")
                                            .get();
                            if (cl != null
                                    && cl.getStatus() != null
                                    && !"READY".equals(cl.getStatus().getPhase())) {
                                patchClusterToReady("ext-demo");
                            }
                            var a =
                                    client.resources(ThingWorxExtension.class)
                                            .inNamespace("thingworx")
                                            .withName("alpha")
                                            .get();
                            assertThat(a).isNotNull();
                            assertThat(a.getStatus()).isNotNull();
                            assertThat(a.getStatus().getPhase()).isEqualTo("Ready");
                            assertThat(a.getStatus().getObservedFingerprint())
                                    .isEqualTo(a.getStatus().getLastAttemptedFingerprint());
                            assertThat(a.getStatus().getObservedDigest())
                                    .isNotNull()
                                    .startsWith("sha256:");
                            assertThat(a.getStatus().getConditions())
                                    .anySatisfy(
                                            c -> {
                                                assertThat(c.getType()).isEqualTo("Ready");
                                                assertThat(c.getStatus()).isEqualTo("True");
                                            });
                        });

        await().atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(
                        () -> {
                            var b =
                                    client.resources(ThingWorxExtension.class)
                                            .inNamespace("thingworx")
                                            .withName("beta")
                                            .get();
                            assertThat(b).isNotNull();
                            assertThat(b.getStatus()).isNotNull();
                            assertThat(b.getStatus().getPhase()).isIn("Importing", "Updating");
                            assertThat(b.getStatus().getInstallerJob()).isNotNull();
                            var jobBeta =
                                    client.batch()
                                            .v1()
                                            .jobs()
                                            .inNamespace("thingworx")
                                            .withName(b.getStatus().getInstallerJob())
                                            .get();
                            assertThat(jobBeta).isNotNull();
                            assertThat(jobBeta.getMetadata().getLabels())
                                    .containsEntry("thingworx.us-ignite.org/extension", "beta");
                            var a =
                                    client.resources(ThingWorxExtension.class)
                                            .inNamespace("thingworx")
                                            .withName("alpha")
                                            .get();
                            assertThat(a.getStatus().getInstallerJob())
                                    .isNotEqualTo(b.getStatus().getInstallerJob());
                        });

        {
            var b =
                    client.resources(ThingWorxExtension.class)
                            .inNamespace("thingworx")
                            .withName("beta")
                            .get();
            markJobSucceeded("thingworx", b.getStatus().getInstallerJob());
        }

        await().atMost(Duration.ofMinutes(1))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(
                        () -> {
                            var b =
                                    client.resources(ThingWorxExtension.class)
                                            .inNamespace("thingworx")
                                            .withName("beta")
                                            .get();
                            assertThat(b).isNotNull();
                            assertThat(b.getStatus()).isNotNull();
                            assertThat(b.getStatus().getPhase()).isEqualTo("Ready");
                            assertThat(b.getStatus().getObservedDigest()).startsWith("sha256:");
                            assertThat(b.getStatus().getConditions())
                                    .anySatisfy(
                                            c -> {
                                                assertThat(c.getType()).isEqualTo("Ready");
                                                assertThat(c.getStatus()).isEqualTo("True");
                                            });
                        });

        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(
                        () -> {
                            var a =
                                    client.resources(ThingWorxExtension.class)
                                            .inNamespace("thingworx")
                                            .withName("alpha")
                                            .get();
                            var b =
                                    client.resources(ThingWorxExtension.class)
                                            .inNamespace("thingworx")
                                            .withName("beta")
                                            .get();
                            assertThat(a.getStatus().getPhase()).isEqualTo("Ready");
                            assertThat(b.getStatus().getPhase()).isEqualTo("Ready");
                            var jobs =
                                    client.batch()
                                            .v1()
                                            .jobs()
                                            .inNamespace("thingworx")
                                            .withLabel("thingworx.us-ignite.org/extension", "alpha")
                                            .list()
                                            .getItems();
                            assertThat(jobs).isNotEmpty();
                        });
    }

    // ---------------------------------------------------------------------
    // Delegating helpers – keep behavior, no duplication (logic in K3sSupport)
    // ---------------------------------------------------------------------

    private void createNamespace(String name) {
        K3sSupport.createNamespace(client, name);
    }

    private void createSecret(String namespace, String name, String key, String value) {
        K3sSupport.createSecret(client, namespace, name, key, value);
    }

    private ThingWorxCluster minimalCluster(String name) {
        return K3sSupport.minimalCluster(name);
    }

    private ThingWorxCluster extensionReadyCluster(String name) {
        return K3sSupport.extensionReadyCluster(name);
    }

    private ThingWorxExtension extension(String name, String clusterName, List<String> dependsOn) {
        return K3sSupport.extension(name, clusterName, dependsOn);
    }

    private void patchClusterToReady(String clusterName) {
        K3sSupport.patchClusterToReady(client, clusterName);
    }

    private void ensurePlatformAndPostgresStubsReady(String clusterName) {
        K3sSupport.ensurePlatformAndPostgresStubsReady(client, clusterName);
    }

    private void markJobSucceeded(String namespace, String jobName) {
        K3sSupport.markJobSucceeded(client, namespace, jobName);
    }

    private <T extends HasMetadata> void deleteIfExists(Class<T> type, String namespace, String name) {
        K3sSupport.deleteIfExists(client, type, namespace, name);
    }

    private void ensureOperatorStopped() throws InterruptedException {
        K3sSupport.ensureOperatorStopped(operator);
        operator = null;
    }

    private void installCrdsIdempotently() throws Exception {
        K3sSupport.installCrdsIdempotently(client);
    }

    @SuppressWarnings("unused")
    private File resolveCrdsDir() {
        return K3sSupport.resolveCrdsDir();
    }

    @SuppressWarnings("unused")
    private void awaitCrdEstablished(String name) {
        K3sSupport.awaitCrdEstablished(client, name);
    }
}
