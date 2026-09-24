package org.us_ignite.thingworx.operator.test.reconcile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatusBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatusBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatusBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.storage.StorageClassBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Map;
import org.us_ignite.thingworx.operator.api.DatabaseSpec;
import org.us_ignite.thingworx.operator.api.ImageSet;
import org.us_ignite.thingworx.operator.api.IngressSpec;
import org.us_ignite.thingworx.operator.api.SecretKeyReference;
import org.us_ignite.thingworx.operator.api.StorageSpec;
import org.us_ignite.thingworx.operator.api.ThingWorxCluster;
import org.us_ignite.thingworx.operator.api.ThingWorxClusterSpec;
import org.us_ignite.thingworx.operator.api.ThingWorxClusterStatus;
import org.us_ignite.thingworx.operator.reconcile.ThingWorxClusterReconciler;
import org.us_ignite.thingworx.operator.reconcile.ThingWorxResources;

/** Verifies reconciler API calls without requiring a Kubernetes cluster. */
@ExtendWith(KubernetesMockServerExtension.class)
@EnableKubernetesMockClient(crud = true)
class ThingWorxClusterReconcilerMockServerTest {
    KubernetesClient client;
    KubernetesMockServer server;

    @Test
    void creatingAClusterAppliesPrecheckResourcesAndAdvancesToDatabasePhase() {
        var cluster = cluster();
        var reconciler = new ThingWorxClusterReconciler(client);

        var result = reconciler.reconcile(cluster, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("DATABASE");
        assertThat(
                        client.configMaps()
                                .inNamespace("thingworx")
                                .withName("demo-runtime")
                                .get()
                                .getData())
                .containsEntry("database-host", "postgres.example.test");
        var sharedPvc =
                client.persistentVolumeClaims()
                        .inNamespace("thingworx")
                        .withName("demo-shared")
                        .get();
        assertThat(sharedPvc.getSpec().getAccessModes())
                .containsExactly("ReadWriteOnce");
        assertThat(sharedPvc.getMetadata().getOwnerReferences()).isNullOrEmpty();
        assertThat(
                        client.services()
                                .inNamespace("thingworx")
                                .withName("demo-platform")
                                .get()
                                .getSpec()
                                .getPorts()
                                .getFirst()
                                .getPort())
                .isEqualTo(8080);
    }

    @Test
    void missingPlatformAdminPasswordIsRejectedBeforeResourcesAreApplied() {
        var cluster = cluster();
        cluster.getSpec().setPlatformAdminPassword(null);

        var result = new ThingWorxClusterReconciler(client).reconcile(cluster, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("INVALID");
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .isEqualTo("spec.platformAdminPassword must reference a Secret name and key.");
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-runtime").get())
                .isNull();
    }

    @Test
    void validSpecificationRecoversFromInvalidPhase() {
        var cluster = cluster();
        var status = new ThingWorxClusterStatus();
        status.setPhase("INVALID");
        status.setMessage("previous validation failure");
        cluster.setStatus(status);

        var result = new ThingWorxClusterReconciler(client).reconcile(cluster, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("DATABASE");
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-runtime").get())
                .isNotNull();
    }

    @Test
    void readyClusterWithNewGenerationRestartsReconciliationWithoutClaimingItIsObserved() {
        var cluster = cluster();
        cluster.getMetadata().setGeneration(2L);
        var status = new ThingWorxClusterStatus();
        status.setPhase("READY");
        status.setObservedGeneration(1L);
        cluster.setStatus(status);
        var reconciler = new ThingWorxClusterReconciler(client);

        var result = reconciler.reconcile(cluster, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("DATABASE");
        assertThat(result.getResource().orElseThrow().getStatus().getObservedGeneration())
                .isEqualTo(1L);
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .contains("restarting the ordered reconciliation workflow");
    }

    @Test
    void restartedReconciliationAppliesTheNewPlatformImageInItsPlatformPhase() {
        var cluster = cluster();
        cluster.getMetadata().setGeneration(2L);
        cluster.getSpec().getImages().setPlatform("registry.example/platform@sha256:updated");
        var status = new ThingWorxClusterStatus();
        status.setPhase("PLATFORM_CLUSTER");
        status.setObservedGeneration(1L);
        cluster.setStatus(status);

        new ThingWorxClusterReconciler(client).reconcile(cluster, null);

        assertThat(
                        client.apps()
                                .statefulSets()
                                .inNamespace("thingworx")
                                .withName("demo-platform")
                                .get()
                                .getSpec()
                                .getTemplate()
                                .getSpec()
                                .getContainers()
                                .getFirst()
                                .getImage())
                .isEqualTo("registry.example/platform@sha256:updated");
        assertThat(cluster.getStatus().getObservedGeneration()).isEqualTo(1L);
    }

    @Test
    void unchangedInitializationInputsReuseTheExistingJobs() {
        var cluster = cluster();
        var reconciler = new ThingWorxClusterReconciler(client);

        reconciler.reconcile(cluster, null);
        reconciler.reconcile(cluster, null);

        assertThat(client.batch().v1().jobs().inNamespace("thingworx").list().getItems())
                .hasSize(2);
    }

    @Test
    void changedInitializationImageCreatesAHashSuffixedJobAndDoesNotAcceptTheOldSuccess() {
        var cluster = cluster();
        var reconciler = new ThingWorxClusterReconciler(client);

        reconciler.reconcile(cluster, null);
        reconciler.reconcile(cluster, null);
        var oldName = ThingWorxResources.initializationJobName(cluster, "database-init");
        var oldJob =
                client.batch().v1().jobs().inNamespace("thingworx").list().getItems().stream()
                        .filter(job -> oldName.equals(job.getMetadata().getName()))
                        .findFirst()
                        .orElseThrow();
        oldJob.setStatus(new JobStatusBuilder().withSucceeded(1).build());
        client.resource(oldJob).update();

        cluster.getSpec().getImages().setDatabaseInit("registry.example/db-init@sha256:changed");
        var result = reconciler.reconcile(cluster, null).getResource().orElseThrow();
        var newName = ThingWorxResources.initializationJobName(cluster, "database-init");

        assertThat(newName).isNotEqualTo(oldName);
        assertThat(client.batch().v1().jobs().inNamespace("thingworx").withName(oldName).get())
                .isNotNull();
        assertThat(client.batch().v1().jobs().inNamespace("thingworx").withName(newName).get())
                .isNotNull();
        assertThat(result.getStatus().getPhase()).isEqualTo("DATABASE");
        assertThat(client.batch().v1().jobs().inNamespace("thingworx").list().getItems())
                .hasSize(3);
    }

    @Test
    void optionalServicesRemainInTheirPhaseUntilEveryEnabledServiceIsReady() {
        var cluster = cluster();
        cluster.getSpec().setKafkaEnabled(true);
        cluster.getSpec().getImages().setKafka("registry.example/kafka@sha256:kafka");
        cluster.getSpec().setOtelEnabled(true);
        cluster.getSpec().getImages().setOtelCollector("registry.example/otel@sha256:otel");
        var status = new org.us_ignite.thingworx.operator.api.ThingWorxClusterStatus();
        status.setPhase("OPTIONAL_SERVICES");
        cluster.setStatus(status);
        var reconciler = new ThingWorxClusterReconciler(client);

        var waiting = reconciler.reconcile(cluster, null).getResource().orElseThrow();

        assertThat(waiting.getStatus().getPhase()).isEqualTo("OPTIONAL_SERVICES");
        assertThat(waiting.getStatus().getMessage()).contains("Kafka", "OpenTelemetry");
        assertThat(
                        client.apps()
                                .statefulSets()
                                .inNamespace("thingworx")
                                .withName("demo-kafka")
                                .get())
                .isNotNull();
        assertThat(client.apps().deployments().inNamespace("thingworx").withName("demo-otel").get())
                .isNotNull();

        var kafka =
                client.apps().statefulSets().inNamespace("thingworx").withName("demo-kafka").get();
        kafka.setStatus(new StatefulSetStatusBuilder().withReadyReplicas(1).build());
        client.resource(kafka).update();
        var otel = client.apps().deployments().inNamespace("thingworx").withName("demo-otel").get();
        otel.setStatus(new DeploymentStatusBuilder().withAvailableReplicas(1).build());
        client.resource(otel).update();

        var ready = reconciler.reconcile(waiting, null).getResource().orElseThrow();

        assertThat(ready.getStatus().getPhase()).isEqualTo("READY");
    }

    @Test
    void failedInitializationJobMovesTheClusterToInvalid() {
        var cluster = cluster();
        var status = new ThingWorxClusterStatus();
        status.setPhase("DATABASE");
        cluster.setStatus(status);
        client.resource(
                        failedJob(
                                ThingWorxResources.initializationJobName(cluster, "database-init")))
                .create();
        client.resource(
                        pendingJob(
                                ThingWorxResources.initializationJobName(cluster, "security-init")))
                .create();

        var result = new ThingWorxClusterReconciler(client).reconcile(cluster, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("INVALID");
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .contains("required initialization Job failed");
    }

    @Test
    void deploymentPhaseWaitsUntilTheDeploymentHasAvailableReplicas() {
        var cluster = cluster();
        var status = new ThingWorxClusterStatus();
        status.setPhase("CONNECTION_SERVERS");
        cluster.setStatus(status);
        var reconciler = new ThingWorxClusterReconciler(client);

        var initial = reconciler.reconcile(cluster, null).getResource().orElseThrow();
        assertThat(initial.getStatus().getPhase()).isEqualTo("CONNECTION_SERVERS");

        var deployment =
                client.apps()
                        .deployments()
                        .inNamespace("thingworx")
                        .withName("demo-cxserver")
                        .get();
        deployment.setStatus(new DeploymentStatusBuilder().withAvailableReplicas(2).build());
        client.resource(deployment).update();

        var ready = reconciler.reconcile(initial, null).getResource().orElseThrow();
        assertThat(ready.getStatus().getPhase()).isEqualTo("EDGE");
    }

    @Test
    void pvcExpansionPatchesRequestsStorageWhenDesiredLarger() {
        var cluster = cluster();
        // Allow expansion on the StorageClass used by the test cluster.
        client.resource(
                        new StorageClassBuilder()
                                .withNewMetadata()
                                .withName("rwx")
                                .endMetadata()
                                .withProvisioner("test.provisioner")
                                .withAllowVolumeExpansion(true)
                                .build())
                .create();
        var reconciler = new ThingWorxClusterReconciler(client);

        reconciler.reconcile(cluster, null);
        var initialQty =
                client.persistentVolumeClaims()
                        .inNamespace("thingworx")
                        .withName("demo-shared")
                        .get()
                        .getSpec()
                        .getResources()
                        .getRequests()
                        .get("storage");
        assertThat(Quantity.getAmountInBytes(initialQty).toString()).isEqualTo("21474836480"); // 20Gi

        cluster.getSpec().getStorage().setSharedStorageSize("40Gi");
        reconciler.reconcile(cluster, null);

        var expandedQty =
                client.persistentVolumeClaims()
                        .inNamespace("thingworx")
                        .withName("demo-shared")
                        .get()
                        .getSpec()
                        .getResources()
                        .getRequests()
                        .get("storage");
        // Desired > existing triggers patch.
        assertThat(expandedQty.compareTo(initialQty)).isGreaterThan(0);
        assertThat(expandedQty.toString()).contains("40Gi");
    }

    @Test
    void pvcExpansionIsBlockedWhenStorageClassDisallows() {
        var cluster = cluster();
        client.resource(
                        new StorageClassBuilder()
                                .withNewMetadata()
                                .withName("rwx")
                                .endMetadata()
                                .withProvisioner("test.provisioner")
                                .withAllowVolumeExpansion(false)
                                .build())
                .create();
        var reconciler = new ThingWorxClusterReconciler(client);
        reconciler.reconcile(cluster, null);

        cluster.getSpec().getStorage().setSharedStorageSize("40Gi");
        assertThatThrownBy(() -> reconciler.reconcile(cluster, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not allow volume expansion");

        // PVC remains at original size; expansion was rejected and Event emitted.
        var qty =
                client.persistentVolumeClaims()
                        .inNamespace("thingworx")
                        .withName("demo-shared")
                        .get()
                        .getSpec()
                        .getResources()
                        .getRequests()
                        .get("storage");
        assertThat(qty.toString()).contains("20Gi");
    }

    @Test
    void pvcShrinkIsRejectedAndPreservesExistingSize() {
        var cluster = cluster();
        client.resource(
                        new StorageClassBuilder()
                                .withNewMetadata()
                                .withName("rwx")
                                .endMetadata()
                                .withProvisioner("test.provisioner")
                                .withAllowVolumeExpansion(true)
                                .build())
                .create();
        var reconciler = new ThingWorxClusterReconciler(client);
        reconciler.reconcile(cluster, null);

        // First expand to 40Gi
        cluster.getSpec().getStorage().setSharedStorageSize("40Gi");
        reconciler.reconcile(cluster, null);
        var expandedQty =
                client.persistentVolumeClaims()
                        .inNamespace("thingworx")
                        .withName("demo-shared")
                        .get()
                        .getSpec()
                        .getResources()
                        .getRequests()
                        .get("storage");
        assertThat(expandedQty.toString()).contains("40Gi");

        // Now attempt to shrink to 10Gi — must be rejected, size preserved.
        cluster.getSpec().getStorage().setSharedStorageSize("10Gi");
        assertThatThrownBy(() -> reconciler.reconcile(cluster, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shrinking PVC storage is not supported");

        var afterShrinkQty =
                client.persistentVolumeClaims()
                        .inNamespace("thingworx")
                        .withName("demo-shared")
                        .get()
                        .getSpec()
                        .getResources()
                        .getRequests()
                        .get("storage");
        assertThat(afterShrinkQty.toString()).contains("40Gi");
    }

    @Test
    void databaseJobsRetainCompletionStatusUntilTheNextUpgrade() {
        var cluster = cluster();
        var reconciler = new ThingWorxClusterReconciler(client);

        reconciler.reconcile(cluster, null);
        reconciler.reconcile(cluster, null);

        var jobs = client.batch().v1().jobs().inNamespace("thingworx").list().getItems();
        assertThat(jobs).hasSize(2);
        for (var job : jobs) {
            assertThat(job.getSpec().getTtlSecondsAfterFinished()).isNull();
        }
    }

    @Test
    void configuredJobTtlDoesNotExpireInitializationJobs() {
        var cluster = cluster();
        cluster.getSpec().setJobTtlSecondsAfterFinished(7200);
        var reconciler = new ThingWorxClusterReconciler(client);

        reconciler.reconcile(cluster, null);
        reconciler.reconcile(cluster, null);

        var jobs = client.batch().v1().jobs().inNamespace("thingworx").list().getItems();
        assertThat(jobs).hasSize(2);
        for (var job : jobs) {
            assertThat(job.getSpec().getTtlSecondsAfterFinished()).isNull();
        }
    }

    @Test
    void upgradeCleanupDeletesSupersededInitializationJobs() {
        var cluster = cluster();
        var reconciler = new ThingWorxClusterReconciler(client);

        // Initial reconciles create two hash Jobs (PRECHECK -> DATABASE)
        reconciler.reconcile(cluster, null);
        reconciler.reconcile(cluster, null);
        var initialJobs = client.batch().v1().jobs().inNamespace("thingworx").list().getItems();
        assertThat(initialJobs).hasSize(2);
        var oldHashName = ThingWorxResources.initializationJobName(cluster, "database-init");

        // Change image -> new hash Job will be created on next reconcile (DATABASE phase)
        cluster.getSpec().getImages().setDatabaseInit("registry.example/db-init@sha256:changed");
        reconciler.reconcile(cluster, null);
        var newHashName = ThingWorxResources.initializationJobName(cluster, "database-init");
        assertThat(newHashName).isNotEqualTo(oldHashName);
        assertThat(client.batch().v1().jobs().inNamespace("thingworx").withName(oldHashName).get())
                .isNotNull();
        assertThat(client.batch().v1().jobs().inNamespace("thingworx").withName(newHashName).get())
                .isNotNull();
        assertThat(client.batch().v1().jobs().inNamespace("thingworx").list().getItems()).hasSize(3);

        // Manually create an additional orphan Job with instance label but not in desired set
        var orphan =
                new JobBuilder()
                        .withMetadata(
                                new io.fabric8.kubernetes.api.model.ObjectMetaBuilder()
                                        .withName("demo-database-init-orphan1234")
                                        .withNamespace("thingworx")
                                        .withLabels(
                                                Map.of(
                                                        "app.kubernetes.io/instance",
                                                        "demo",
                                                        "app.kubernetes.io/component",
                                                        "database-init",
                                                        "app.kubernetes.io/managed-by",
                                                        "thingworx-operator"))
                                        .build())
                        .withNewSpec()
                        .withTtlSecondsAfterFinished(3600)
                        .withNewTemplate()
                        .withNewSpec()
                        .withRestartPolicy("OnFailure")
                        .addNewContainer()
                        .withName("database-init")
                        .withImage("registry.example/db-init@sha256:old")
                        .endContainer()
                        .endSpec()
                        .endTemplate()
                        .endSpec()
                        .build();
        client.resource(orphan).create();
        assertThat(client.batch().v1().jobs().inNamespace("thingworx").list().getItems()).hasSize(4);

        // Start a new rollout: superseded Jobs are pruned before its DATABASE phase begins.
        var status = new ThingWorxClusterStatus();
        status.setPhase("READY");
        status.setObservedGeneration(cluster.getMetadata().getGeneration());
        cluster.setStatus(status);
        cluster.getMetadata().setGeneration(cluster.getMetadata().getGeneration() + 1);
        var platform = new io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withName("demo-platform")
                                .withNamespace("thingworx")
                                .build())
                .withNewSpec()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(Map.of("app.kubernetes.io/instance", "demo"))
                .endMetadata()
                .endTemplate()
                .endSpec()
                .withStatus(
                        new io.fabric8.kubernetes.api.model.apps.StatefulSetStatusBuilder()
                                .withReplicas(1)
                                .withReadyReplicas(1)
                                .build())
                .build();
        client.resource(platform).create();

        var result = reconciler.reconcile(cluster, null);
        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("DATABASE");

        // Orphan and old hash should be pruned; only the two current desired Jobs remain
        var remaining = client.batch().v1().jobs().inNamespace("thingworx").list().getItems();
        assertThat(remaining)
                .extracting(job -> job.getMetadata().getName())
                .doesNotContain("demo-database-init-orphan1234", oldHashName)
                .contains(newHashName);
        // New hash + security-init = 2 jobs
        assertThat(remaining).hasSize(2);
        // Current Jobs remain as lifecycle completion records until the next upgrade.
        for (var job : remaining) {
            assertThat(job.getSpec().getTtlSecondsAfterFinished()).isNull();
        }
    }

    @Test
    void pruneRemovesOrphanConfigMapServiceIngressDeploymentAndStatefulSet() {
        var cluster = cluster();
        // Keep single-node (enableHA=false), no optional, no ingress — so HA/optional resources are orphans
        var reconciler = new ThingWorxClusterReconciler(client);

        // Pre-create HA-like orphans with correct managed-by + instance labels
        createOrphanConfigMap("demo-ignite-config");
        createOrphanConfigMap("demo-haproxy-config");
        createOrphanService("demo-zookeeper");
        createOrphanService("demo-ignite");
        createOrphanService("demo-cxserver");
        createOrphanService("demo-haproxy");
        createOrphanService("demo-kafka-extra");
        createOrphanIngress("demo-ingress");
        createOrphanDeployment("demo-cxserver");
        createOrphanDeployment("demo-haproxy");
        createOrphanDeployment("demo-otel");
        createOrphanStatefulSet("demo-zookeeper");
        createOrphanStatefulSet("demo-ignite");
        createOrphanStatefulSet("demo-kafka");

        // Create an unmanaged ConfigMap (wrong managed-by) — should NOT be pruned
        client.resource(
                        new ConfigMapBuilder()
                                .withMetadata(
                                        new ObjectMetaBuilder()
                                                .withName("demo-unmanaged")
                                                .withNamespace("thingworx")
                                                .withLabels(
                                                        Map.of(
                                                                "app.kubernetes.io/instance",
                                                                "demo",
                                                                "app.kubernetes.io/managed-by",
                                                                "other-operator"))
                                                .build())
                                .withData(Map.of("key", "value"))
                                .build())
                .create();

        // Create a different instance label — should NOT be pruned (use distinct name to avoid 409)
        createOrphanWithInstance("demo-foreign-config", "other-instance");
        createOrphanWithInstanceService("demo-foreign-service", "other-instance");

        makeReady(cluster, reconciler);

        var result = reconciler.reconcile(cluster, null);
        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("READY");

        // Orphans with correct labels should be gone
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-ignite-config").get())
                .isNull();
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-haproxy-config").get())
                .isNull();
        assertThat(client.services().inNamespace("thingworx").withName("demo-zookeeper").get()).isNull();
        assertThat(client.services().inNamespace("thingworx").withName("demo-ignite").get()).isNull();
        assertThat(client.services().inNamespace("thingworx").withName("demo-cxserver").get()).isNull();
        assertThat(client.services().inNamespace("thingworx").withName("demo-haproxy").get()).isNull();
        assertThat(client.network().v1().ingresses().inNamespace("thingworx").withName("demo-ingress").get())
                .isNull();
        assertThat(client.apps().deployments().inNamespace("thingworx").withName("demo-cxserver").get())
                .isNull();
        assertThat(client.apps().deployments().inNamespace("thingworx").withName("demo-haproxy").get())
                .isNull();
        assertThat(client.apps().deployments().inNamespace("thingworx").withName("demo-otel").get()).isNull();
        assertThat(client.apps().statefulSets().inNamespace("thingworx").withName("demo-zookeeper").get())
                .isNull();
        assertThat(client.apps().statefulSets().inNamespace("thingworx").withName("demo-ignite").get()).isNull();
        assertThat(client.apps().statefulSets().inNamespace("thingworx").withName("demo-kafka").get()).isNull();

        // Unmanaged or foreign-instance resources must remain
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-unmanaged").get()).isNotNull();
        // Verify foreign instance resources still exist under different instance label listing
        var foreign =
                client.configMaps()
                        .inNamespace("thingworx")
                        .withLabel("app.kubernetes.io/instance", "other-instance")
                        .list()
                        .getItems();
        assertThat(foreign).extracting(cm -> cm.getMetadata().getName()).contains("demo-foreign-config");
        var foreignSvc =
                client.services()
                        .inNamespace("thingworx")
                        .withLabel("app.kubernetes.io/instance", "other-instance")
                        .list()
                        .getItems();
        assertThat(foreignSvc).extracting(svc -> svc.getMetadata().getName()).contains("demo-foreign-service");

        // Desired resources must remain (demo-runtime ConfigMap, demo-platform Service, demo-shared PVC, demo-platform StatefulSet)
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-runtime").get()).isNotNull();
        assertThat(client.services().inNamespace("thingworx").withName("demo-platform").get()).isNotNull();
        assertThat(client.persistentVolumeClaims().inNamespace("thingworx").withName("demo-shared").get())
                .isNotNull();
    }

    @Test
    void pruneDoesNotDeletePvcWithoutPruneAnnotation() {
        var cluster = cluster();
        var reconciler = new ThingWorxClusterReconciler(client);

        // Create orphan PVCs without annotation — should NOT be deleted
        createOrphanPvc("demo-orphan-pvc-1", null);
        createOrphanPvc("demo-orphan-pvc-2", "false");
        createOrphanPvc("demo-kafka-data", null);

        makeReady(cluster, reconciler);
        var result = reconciler.reconcile(cluster, null);
        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("READY");

        assertThat(client.persistentVolumeClaims().inNamespace("thingworx").withName("demo-orphan-pvc-1").get())
                .isNotNull();
        assertThat(client.persistentVolumeClaims().inNamespace("thingworx").withName("demo-orphan-pvc-2").get())
                .isNotNull();
        assertThat(client.persistentVolumeClaims().inNamespace("thingworx").withName("demo-kafka-data").get())
                .isNotNull();
    }

    @Test
    void pruneDeletesPvcWithTrueAnnotation() {
        var cluster = cluster();
        var reconciler = new ThingWorxClusterReconciler(client);

        createOrphanPvc("demo-orphan-pvc-annotated", "true");
        createOrphanPvc("demo-kafka-data", "true");

        makeReady(cluster, reconciler);
        var result = reconciler.reconcile(cluster, null);
        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("READY");

        assertThat(
                        client.persistentVolumeClaims()
                                .inNamespace("thingworx")
                                .withName("demo-orphan-pvc-annotated")
                                .get())
                .isNull();
        assertThat(client.persistentVolumeClaims().inNamespace("thingworx").withName("demo-kafka-data").get())
                .isNull();
        // Desired PVCs must remain even if annotated? shared is desired, so not pruned regardless of annotation
        assertThat(client.persistentVolumeClaims().inNamespace("thingworx").withName("demo-shared").get())
                .isNotNull();
    }

    @Test
    void haToSingleNodeToggleSimulationPrunesHaAndOptionalOrphans() {
        var cluster = cluster();
        // Start as HA with kafka, otel, ingress
        cluster.getSpec().setEnableHA(true);
        cluster.getSpec().setKafkaEnabled(true);
        cluster.getSpec().setOtelEnabled(true);
        cluster.getSpec().getImages().setKafka("registry.example/kafka@sha256:kafka");
        cluster.getSpec().getImages().setOtelCollector("registry.example/otel@sha256:otel");
        cluster.getSpec().getImages().setZookeeper("registry.example/zookeeper@sha256:zk");
        cluster.getSpec().getImages().setIgnite("registry.example/ignite@sha256:ignite2");
        cluster.getSpec().getImages().setCxServer("registry.example/cx@sha256:cx2");
        cluster.getSpec().getImages().setHaProxy("registry.example/haproxy@sha256:haproxy");
        cluster.getSpec().getIngress().setHost("thingworx.example.test");
        cluster.getSpec().getIngress().setClassName("nginx");
        var cxKey = new SecretKeyReference();
        cxKey.setName("cx-key");
        cxKey.setKey("key");
        cluster.getSpec().setCxServerAppKey(cxKey);
        // external DB so no postgres PVC
        cluster.getSpec().getDatabase().setInternal(false);
        createSecret("cx-key", "thingworx");

        var reconciler = new ThingWorxClusterReconciler(client);

        // Simulate that HA reconciliation already created all HA/optional resources (via direct client creates)
        // Use ThingWorxResources to create them as if reconciliation had run, then we toggle off
        // First, advance to READY with HA spec to populate desired resources
        // To avoid waiting for readiness, manually create platform and mark ready
        makeReady(cluster, reconciler);
        // Manually apply HA desired resources so they exist as orphans after toggle
        // Instead of relying on reconciler to create, we directly create what would be HA orphans
        createOrphanConfigMap("demo-ignite-config");
        createOrphanStatefulSet("demo-zookeeper");
        createOrphanStatefulSet("demo-ignite");
        createOrphanService("demo-zookeeper");
        createOrphanService("demo-ignite");
        createOrphanService("demo-cxserver");
        createOrphanService("demo-haproxy");
        createOrphanDeployment("demo-cxserver");
        createOrphanDeployment("demo-haproxy");
        createOrphanConfigMap("demo-haproxy-config");
        createOrphanIngress("demo-ingress");
        createOrphanStatefulSet("demo-kafka");
        createOrphanPvc("demo-kafka-data", "true");
        createOrphanDeployment("demo-otel");

        // Now toggle to single-node, disable optional, remove ingress
        cluster.getSpec().setEnableHA(false);
        cluster.getSpec().setKafkaEnabled(false);
        cluster.getSpec().setOtelEnabled(false);
        cluster.getSpec().getIngress().setHost(null);
        cluster.getSpec().getIngress().setClassName(null);

        // Make READY again with single-node spec (recreate platform ready)
        // Need to reset status to READY to trigger prune
        var status = new ThingWorxClusterStatus();
        status.setPhase("READY");
        status.setObservedGeneration(cluster.getMetadata().getGeneration());
        cluster.setStatus(status);
        var platform =
                new StatefulSetBuilder()
                        .withMetadata(
                                new ObjectMetaBuilder()
                                        .withName("demo-platform")
                                        .withNamespace("thingworx")
                                        .build())
                        .withNewSpec()
                        .withReplicas(1)
                        .withNewTemplate()
                        .withNewMetadata()
                        .withLabels(Map.of("app.kubernetes.io/instance", "demo"))
                        .endMetadata()
                        .endTemplate()
                        .endSpec()
                        .withStatus(
                                new StatefulSetStatusBuilder().withReplicas(1).withReadyReplicas(1).build())
                        .build();
        if (client.apps().statefulSets().inNamespace("thingworx").withName("demo-platform").get() == null) {
            client.resource(platform).create();
        } else {
            client.resource(platform).update();
        }

        var result = reconciler.reconcile(cluster, null);
        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("READY");

        // All HA-specific resources should be pruned
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-ignite-config").get()).isNull();
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-haproxy-config").get()).isNull();
        assertThat(client.apps().statefulSets().inNamespace("thingworx").withName("demo-zookeeper").get())
                .isNull();
        assertThat(client.apps().statefulSets().inNamespace("thingworx").withName("demo-ignite").get()).isNull();
        assertThat(client.apps().statefulSets().inNamespace("thingworx").withName("demo-kafka").get()).isNull();
        assertThat(client.services().inNamespace("thingworx").withName("demo-zookeeper").get()).isNull();
        assertThat(client.services().inNamespace("thingworx").withName("demo-ignite").get()).isNull();
        assertThat(client.services().inNamespace("thingworx").withName("demo-cxserver").get()).isNull();
        assertThat(client.services().inNamespace("thingworx").withName("demo-haproxy").get()).isNull();
        assertThat(client.apps().deployments().inNamespace("thingworx").withName("demo-cxserver").get()).isNull();
        assertThat(client.apps().deployments().inNamespace("thingworx").withName("demo-haproxy").get()).isNull();
        assertThat(client.apps().deployments().inNamespace("thingworx").withName("demo-otel").get()).isNull();
        assertThat(client.network().v1().ingresses().inNamespace("thingworx").withName("demo-ingress").get())
                .isNull();
        // kafka PVC with true annotation should be pruned
        assertThat(client.persistentVolumeClaims().inNamespace("thingworx").withName("demo-kafka-data").get())
                .isNull();
        // Desired single-node resources remain
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-runtime").get()).isNotNull();
        assertThat(client.services().inNamespace("thingworx").withName("demo-platform").get()).isNotNull();
        assertThat(client.apps().statefulSets().inNamespace("thingworx").withName("demo-platform").get())
                .isNotNull();
    }

    private void makeReady(ThingWorxCluster cluster, ThingWorxClusterReconciler reconciler) {
        // Advance through precheck to create desired single-node resources, then mark platform ready and set status READY
        reconciler.reconcile(cluster, null);
        // DATABASE phase needs Jobs, but we can bypass by directly setting READY after platform ready
        var status = new ThingWorxClusterStatus();
        status.setPhase("READY");
        status.setObservedGeneration(cluster.getMetadata().getGeneration());
        cluster.setStatus(status);
        var platform =
                new StatefulSetBuilder()
                        .withMetadata(
                                new ObjectMetaBuilder()
                                        .withName("demo-platform")
                                        .withNamespace("thingworx")
                                        .build())
                        .withNewSpec()
                        .withReplicas(1)
                        .withNewTemplate()
                        .withNewMetadata()
                        .withLabels(Map.of("app.kubernetes.io/instance", "demo"))
                        .endMetadata()
                        .endTemplate()
                        .endSpec()
                        .withStatus(
                                new StatefulSetStatusBuilder().withReplicas(1).withReadyReplicas(1).build())
                        .build();
        // Only create if not exists
        if (client.apps().statefulSets().inNamespace("thingworx").withName("demo-platform").get() == null) {
            client.resource(platform).create();
        } else {
            client.resource(platform).update();
        }
    }

    private void createOrphanConfigMap(String name) {
        if (client.configMaps().inNamespace("thingworx").withName(name).get() != null) return;
        client.resource(
                        new ConfigMapBuilder()
                                .withMetadata(
                                        new ObjectMetaBuilder()
                                                .withName(name)
                                                .withNamespace("thingworx")
                                                .withLabels(
                                                        Map.of(
                                                                "app.kubernetes.io/instance",
                                                                "demo",
                                                                "app.kubernetes.io/managed-by",
                                                                "thingworx-operator",
                                                                "app.kubernetes.io/component",
                                                                "test"))
                                                .build())
                                .withData(Map.of("key", "value"))
                                .build())
                .create();
    }

    private void createOrphanWithInstance(String name, String instance) {
        client.resource(
                        new ConfigMapBuilder()
                                .withMetadata(
                                        new ObjectMetaBuilder()
                                                .withName(name)
                                                .withNamespace("thingworx")
                                                .withLabels(
                                                        Map.of(
                                                                "app.kubernetes.io/instance",
                                                                instance,
                                                                "app.kubernetes.io/managed-by",
                                                                "thingworx-operator"))
                                                .build())
                                .withData(Map.of("key", "value"))
                                .build())
                .create();
    }

    private void createOrphanWithInstanceService(String name, String instance) {
        client.resource(
                        new ServiceBuilder()
                                .withMetadata(
                                        new ObjectMetaBuilder()
                                                .withName(name)
                                                .withNamespace("thingworx")
                                                .withLabels(
                                                        Map.of(
                                                                "app.kubernetes.io/instance",
                                                                instance,
                                                                "app.kubernetes.io/managed-by",
                                                                "thingworx-operator"))
                                                .build())
                                .withNewSpec()
                                .withSelector(Map.of("app.kubernetes.io/instance", instance))
                                .addNewPort()
                                .withPort(8080)
                                .endPort()
                                .endSpec()
                                .build())
                .create();
    }

    private void createOrphanService(String name) {
        if (client.services().inNamespace("thingworx").withName(name).get() != null) return;
        client.resource(
                        new ServiceBuilder()
                                .withMetadata(
                                        new ObjectMetaBuilder()
                                                .withName(name)
                                                .withNamespace("thingworx")
                                                .withLabels(
                                                        Map.of(
                                                                "app.kubernetes.io/instance",
                                                                "demo",
                                                                "app.kubernetes.io/managed-by",
                                                                "thingworx-operator",
                                                                "app.kubernetes.io/component",
                                                                "test"))
                                                .build())
                                .withNewSpec()
                                .withSelector(Map.of("app.kubernetes.io/instance", "demo"))
                                .addNewPort()
                                .withPort(8080)
                                .endPort()
                                .endSpec()
                                .build())
                .create();
    }

    private void createOrphanIngress(String name) {
        if (client.network().v1().ingresses().inNamespace("thingworx").withName(name).get() != null) return;
        client.resource(
                        new IngressBuilder()
                                .withMetadata(
                                        new ObjectMetaBuilder()
                                                .withName(name)
                                                .withNamespace("thingworx")
                                                .withLabels(
                                                        Map.of(
                                                                "app.kubernetes.io/instance",
                                                                "demo",
                                                                "app.kubernetes.io/managed-by",
                                                                "thingworx-operator"))
                                                .build())
                                .withNewSpec()
                                .withIngressClassName("nginx")
                                .addNewRule()
                                .withHost("example.test")
                                .withNewHttp()
                                .addNewPath()
                                .withPath("/")
                                .withPathType("Prefix")
                                .withNewBackend()
                                .withNewService()
                                .withName("demo-platform")
                                .withNewPort()
                                .withNumber(8080)
                                .endPort()
                                .endService()
                                .endBackend()
                                .endPath()
                                .endHttp()
                                .endRule()
                                .endSpec()
                                .build())
                .create();
    }

    private void createOrphanPvc(String name, String pruneAnnotationValue) {
        if (client.persistentVolumeClaims().inNamespace("thingworx").withName(name).get() != null) return;
        var meta = new ObjectMetaBuilder()
                .withName(name)
                .withNamespace("thingworx")
                .withLabels(
                        Map.of(
                                "app.kubernetes.io/instance",
                                "demo",
                                "app.kubernetes.io/managed-by",
                                "thingworx-operator"))
                .build();
        if (pruneAnnotationValue != null) {
            meta.setAnnotations(Map.of("thingworx.us-ignite.org/prune-pvc", pruneAnnotationValue));
        }
        client.resource(
                        new PersistentVolumeClaimBuilder()
                                .withMetadata(meta)
                                .withNewSpec()
                                .withAccessModes("ReadWriteOnce")
                                .withStorageClassName("rwx")
                                .withResources(
                                        new io.fabric8.kubernetes.api.model.VolumeResourceRequirementsBuilder()
                                                .addToRequests("storage", new Quantity("10Gi"))
                                                .build())
                                .endSpec()
                                .build())
                .create();
    }

    private void createOrphanDeployment(String name) {
        if (client.apps().deployments().inNamespace("thingworx").withName(name).get() != null) return;
        client.resource(
                        new DeploymentBuilder()
                                .withMetadata(
                                        new ObjectMetaBuilder()
                                                .withName(name)
                                                .withNamespace("thingworx")
                                                .withLabels(
                                                        Map.of(
                                                                "app.kubernetes.io/instance",
                                                                "demo",
                                                                "app.kubernetes.io/managed-by",
                                                                "thingworx-operator",
                                                                "app.kubernetes.io/component",
                                                                "test"))
                                                .build())
                                .withNewSpec()
                                .withReplicas(1)
                                .withSelector(
                                        new io.fabric8.kubernetes.api.model.LabelSelectorBuilder()
                                                .withMatchLabels(
                                                        Map.of(
                                                                "app.kubernetes.io/instance",
                                                                "demo",
                                                                "app.kubernetes.io/component",
                                                                "test"))
                                                .build())
                                .withNewTemplate()
                                .withNewMetadata()
                                .withLabels(
                                        Map.of(
                                                "app.kubernetes.io/instance",
                                                "demo",
                                                "app.kubernetes.io/component",
                                                "test"))
                                .endMetadata()
                                .withNewSpec()
                                .addNewContainer()
                                .withName("test")
                                .withImage("registry.example/test@sha256:test")
                                .endContainer()
                                .endSpec()
                                .endTemplate()
                                .endSpec()
                                .build())
                .create();
    }

    private void createOrphanStatefulSet(String name) {
        if (client.apps().statefulSets().inNamespace("thingworx").withName(name).get() != null) return;
        client.resource(
                        new StatefulSetBuilder()
                                .withMetadata(
                                        new ObjectMetaBuilder()
                                                .withName(name)
                                                .withNamespace("thingworx")
                                                .withLabels(
                                                        Map.of(
                                                                "app.kubernetes.io/instance",
                                                                "demo",
                                                                "app.kubernetes.io/managed-by",
                                                                "thingworx-operator",
                                                                "app.kubernetes.io/component",
                                                                "test"))
                                                .build())
                                .withNewSpec()
                                .withServiceName(name)
                                .withReplicas(1)
                                .withSelector(
                                        new io.fabric8.kubernetes.api.model.LabelSelectorBuilder()
                                                .withMatchLabels(
                                                        Map.of(
                                                                "app.kubernetes.io/instance",
                                                                "demo",
                                                                "app.kubernetes.io/component",
                                                                "test"))
                                                .build())
                                .withNewTemplate()
                                .withNewMetadata()
                                .withLabels(
                                        Map.of(
                                                "app.kubernetes.io/instance",
                                                "demo",
                                                "app.kubernetes.io/component",
                                                "test"))
                                .endMetadata()
                                .withNewSpec()
                                .addNewContainer()
                                .withName("test")
                                .withImage("registry.example/test@sha256:test")
                                .endContainer()
                                .endSpec()
                                .endTemplate()
                                .endSpec()
                                .build())
                .create();
    }

    private io.fabric8.kubernetes.api.model.batch.v1.Job pendingJob(String name) {
        return new JobBuilder()
                .withMetadata(
                        new ObjectMetaBuilder().withName(name).withNamespace("thingworx").build())
                .build();
    }

    private io.fabric8.kubernetes.api.model.batch.v1.Job failedJob(String name) {
        var job = pendingJob(name);
        job.setStatus(new JobStatusBuilder().withFailed(1).build());
        return job;
    }

    private void createSecret(String name, String namespace) {
        if (blank(name) || blank(namespace)) return;
        if (client.secrets().inNamespace(namespace).withName(name).get() != null) return;
        client.resource(
                        new SecretBuilder()
                                .withNewMetadata()
                                .withName(name)
                                .withNamespace(namespace)
                                .endMetadata()
                                .withStringData(Map.of("password", "test", "key", "test"))
                                .build())
                .create();
    }

    private void createConfigMap(String name, String namespace) {
        if (blank(name) || blank(namespace)) return;
        if (client.configMaps().inNamespace(namespace).withName(name).get() != null) return;
        client.resource(
                        new ConfigMapBuilder()
                                .withNewMetadata()
                                .withName(name)
                                .withNamespace(namespace)
                                .endMetadata()
                                .withData(Map.of("dummy", "value"))
                                .build())
                .create();
    }

    private void createRequiredSecrets(ThingWorxCluster cluster) {
        var namespace = cluster.getMetadata().getNamespace();
        var spec = cluster.getSpec();
        if (spec.getPlatformAdminPassword() != null) createSecret(spec.getPlatformAdminPassword().getName(), namespace);
        if (spec.getKeystorePassword() != null) createSecret(spec.getKeystorePassword().getName(), namespace);
        if (spec.getProvisioningAppKey() != null) createSecret(spec.getProvisioningAppKey().getName(), namespace);
        if (spec.getCxServerAppKey() != null) createSecret(spec.getCxServerAppKey().getName(), namespace);
        if (spec.getExtensionImportAppKey() != null) createSecret(spec.getExtensionImportAppKey().getName(), namespace);
        if (spec.getDatabase() != null) {
            if (spec.getDatabase().getCredentials() != null) createSecret(spec.getDatabase().getCredentials().getName(), namespace);
            if (spec.getDatabase().getAdminCredentials() != null) createSecret(spec.getDatabase().getAdminCredentials().getName(), namespace);
        }
        if (spec.getLicenseSecret() != null && !blank(spec.getLicenseSecret())) createSecret(spec.getLicenseSecret(), namespace);
        if (spec.getImagePullSecret() != null && !blank(spec.getImagePullSecret())) createSecret(spec.getImagePullSecret(), namespace);
        if (spec.getSettingsConfigMap() != null && !blank(spec.getSettingsConfigMap())) createConfigMap(spec.getSettingsConfigMap(), namespace);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private ThingWorxCluster cluster() {
        var images = new ImageSet();
        images.setPlatform("registry.example/platform@sha256:platform");
        images.setDatabaseInit("registry.example/db-init@sha256:init");
        images.setSecurityCli("registry.example/security@sha256:security");
        images.setIgnite("registry.example/ignite@sha256:ignite");
        images.setCxServer("registry.example/cx@sha256:cx");
        var database = new DatabaseSpec();
        database.setHost("postgres.example.test");
        database.setDatabase("thingworx");
        database.setSchema("thingworx");
        database.setUsername("thingworx");
        database.setAdminUsername("postgres");
        var storage = new StorageSpec();
        storage.setStorageClassName("rwx");
        var password = new SecretKeyReference();
        password.setName("thingworx-platform-admin");
        password.setKey("password");
        var keystorePassword = new SecretKeyReference();
        keystorePassword.setName("thingworx-keystore");
        keystorePassword.setKey("password");
        // Tests run as single-node with external DB by default; internal PostgreSQL is
        // exercised explicitly in its own test. Use external to avoid requiring
        // adminCredentials for the common path.
        database.setInternal(false);
        var spec = new ThingWorxClusterSpec();
        spec.setImages(images);
        spec.setDatabase(database);
        spec.setStorage(storage);
        spec.setIngress(new IngressSpec());
        spec.setPlatformAdminPassword(password);
        spec.setKeystorePassword(keystorePassword);
        var cluster = new ThingWorxCluster();
        cluster.setMetadata(
                new ObjectMetaBuilder()
                        .withName("demo")
                        .withNamespace("thingworx")
                        .withGeneration(1L)
                        .build());
        cluster.setSpec(spec);
        createRequiredSecrets(cluster);
        return cluster;
    }

    @Test
    void missingLicenseSecretIsRejectedWithoutSideEffects() {
        var cluster = cluster();
        cluster.getSpec().setLicenseSecret("missing-license");
        // do not create the secret

        var result = new ThingWorxClusterReconciler(client).reconcile(cluster, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("INVALID");
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .isEqualTo("Secret 'missing-license' not found in namespace 'thingworx'");
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-runtime").get()).isNull();
    }

    @Test
    void missingSettingsConfigMapIsRejectedWithoutSideEffects() {
        var cluster = cluster();
        cluster.getSpec().setSettingsConfigMap("missing-settings");

        var result = new ThingWorxClusterReconciler(client).reconcile(cluster, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("INVALID");
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .isEqualTo("ConfigMap 'missing-settings' not found in namespace 'thingworx'");
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-runtime").get()).isNull();
    }

    @Test
    void missingImagePullSecretIsRejectedWithoutSideEffects() {
        var cluster = cluster();
        cluster.getSpec().setImagePullSecret("missing-pull");

        var result = new ThingWorxClusterReconciler(client).reconcile(cluster, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("INVALID");
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .isEqualTo("Secret 'missing-pull' not found in namespace 'thingworx'");
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-runtime").get()).isNull();
    }

    @Test
    void missingPlatformAdminPasswordSecretIsRejectedWithoutSideEffects() {
        var cluster = cluster();
        // cluster() already created the secret, delete it to simulate missing
        client.secrets().inNamespace("thingworx").withName("thingworx-platform-admin").delete();
        var result = new ThingWorxClusterReconciler(client).reconcile(cluster, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("INVALID");
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .isEqualTo("Secret 'thingworx-platform-admin' not found in namespace 'thingworx'");
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-runtime").get()).isNull();
    }

    @Test
    void missingDatabaseCredentialsSecretIsRejectedWithoutSideEffects() {
        var cluster = cluster();
        var cred = new SecretKeyReference();
        cred.setName("db-cred");
        cred.setKey("password");
        cluster.getSpec().getDatabase().setCredentials(cred);
        // do not create secret

        var result = new ThingWorxClusterReconciler(client).reconcile(cluster, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("INVALID");
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .isEqualTo("Secret 'db-cred' not found in namespace 'thingworx'");
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-runtime").get()).isNull();
    }

    @Test
    void missingDatabaseAdminCredentialsSecretIsRejectedWithoutSideEffects() {
        var cluster = cluster();
        cluster.getSpec().getDatabase().setInternal(true);
        cluster.getSpec().getDatabase().setImage("postgres:16");
        var adminCred = new SecretKeyReference();
        adminCred.setName("db-admin-cred");
        adminCred.setKey("password");
        cluster.getSpec().getDatabase().setAdminCredentials(adminCred);
        // do not create secret

        var result = new ThingWorxClusterReconciler(client).reconcile(cluster, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("INVALID");
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .isEqualTo("Secret 'db-admin-cred' not found in namespace 'thingworx'");
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-runtime").get()).isNull();
    }

    @Test
    void missingExtensionImportAppKeySecretIsRejectedWithoutSideEffects() {
        var cluster = cluster();
        var key = new SecretKeyReference();
        key.setName("missing-ext-key");
        key.setKey("appKey");
        cluster.getSpec().setExtensionImportAppKey(key);
        // do not create secret

        var result = new ThingWorxClusterReconciler(client).reconcile(cluster, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("INVALID");
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .isEqualTo("Secret 'missing-ext-key' not found in namespace 'thingworx'");
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-runtime").get()).isNull();
    }

    @Test
    void existingReferencedSecretsAllowsReconciliation() {
        var cluster = cluster();
        cluster.getSpec().setLicenseSecret("thingworx-license");
        createSecret("thingworx-license", "thingworx");
        cluster.getSpec().setSettingsConfigMap("thingworx-settings");
        createConfigMap("thingworx-settings", "thingworx");
        cluster.getSpec().setImagePullSecret("thingworx-pull");
        createSecret("thingworx-pull", "thingworx");

        var result = new ThingWorxClusterReconciler(client).reconcile(cluster, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("DATABASE");
        assertThat(client.configMaps().inNamespace("thingworx").withName("demo-runtime").get()).isNotNull();
    }
}
