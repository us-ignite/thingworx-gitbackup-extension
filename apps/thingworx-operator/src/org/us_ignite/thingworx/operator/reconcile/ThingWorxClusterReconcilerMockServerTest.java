package org.us_ignite.thingworx.operator.reconcile;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatusBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatusBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.us_ignite.thingworx.operator.api.DatabaseSpec;
import org.us_ignite.thingworx.operator.api.ImageSet;
import org.us_ignite.thingworx.operator.api.IngressSpec;
import org.us_ignite.thingworx.operator.api.SecretKeyReference;
import org.us_ignite.thingworx.operator.api.StorageSpec;
import org.us_ignite.thingworx.operator.api.ThingWorxCluster;
import org.us_ignite.thingworx.operator.api.ThingWorxClusterSpec;
import org.us_ignite.thingworx.operator.api.ThingWorxClusterStatus;

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
        assertThat(
                        client.persistentVolumeClaims()
                                .inNamespace("thingworx")
                                .withName("demo-shared")
                                .get()
                                .getSpec()
                                .getAccessModes())
                .containsExactly("ReadWriteOnce");
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
        return cluster;
    }
}
