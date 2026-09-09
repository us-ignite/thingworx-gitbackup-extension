package org.us_ignite.thingworx.operator.test.reconcile;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatusBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobConditionBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServerExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.us_ignite.thingworx.operator.api.ClusterReference;
import org.us_ignite.thingworx.operator.api.DatabaseSpec;
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
import org.us_ignite.thingworx.operator.reconcile.ThingWorxExtensionReconciler;

@ExtendWith(KubernetesMockServerExtension.class)
@EnableKubernetesMockClient(crud = true)
class ThingWorxExtensionReconcilerMockServerTest {
    KubernetesClient client;

    @Test
    void createsDigestNamedInstallerJobForReadyCluster() {
        var cluster = readyCluster();
        client.resource(cluster).create();
        var extension = extension();
        var reconciler = new ThingWorxExtensionReconciler(client);

        var result = reconciler.reconcile(extension, null);

        var status = result.getResource().orElseThrow().getStatus();
        assertThat(status.getPhase()).isEqualTo("Importing");
        assertThat(status.getInstallerJob()).matches("entities-[0-9a-f]{12}");
        assertThat(status.getLastAttemptedDigest()).startsWith("sha256:0123456789ab");
        assertThat(status.getConditions())
                .anySatisfy(
                        condition -> {
                            assertThat(condition.getType()).isEqualTo("Progressing");
                            assertThat(condition.getStatus()).isEqualTo("True");
                            assertThat(condition.getReason()).isEqualTo("InstallerJobCreated");
                        });
        var job =
                client.batch()
                        .v1()
                        .jobs()
                        .inNamespace("thingworx")
                        .withName(status.getInstallerJob())
                        .get();
        assertThat(job.getSpec().getTemplate().getSpec().getContainers().getFirst().getImage())
                .isEqualTo("registry.example/installer@sha256:installer");
        assertThat(job.getSpec().getTemplate().getSpec().getContainers().getFirst().getEnv())
                .extracting(env -> env.getName())
                .contains(
                        "INSTALL_MODE",
                        "INSTALL_FINGERPRINT",
                        "OCI_ARTIFACT",
                        "OCI_DIGEST",
                        "THINGWORX_EXTENSION_APP_KEY");
        assertThat(
                        valueOf(
                                job.getSpec()
                                        .getTemplate()
                                        .getSpec()
                                        .getContainers()
                                        .getFirst()
                                        .getEnv(),
                                "OCI_ARTIFACT"))
                .isEqualTo("registry.example/extensions/entities");
        assertThat(
                        valueOf(
                                job.getSpec()
                                        .getTemplate()
                                        .getSpec()
                                        .getContainers()
                                        .getFirst()
                                        .getEnv(),
                                "OCI_DIGEST"))
                .isEqualTo(
                        "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        assertThat(
                        valueOf(
                                job.getSpec()
                                        .getTemplate()
                                        .getSpec()
                                        .getContainers()
                                        .getFirst()
                                        .getEnv(),
                                "THINGWORX_URL"))
                .isEqualTo("http://production-platform:8080/Thingworx");
    }

    @Test
    void mountsArtifactRegistryCredentialsForTheInstallerProcess() {
        var cluster = readyCluster();
        client.resource(cluster).create();
        createSecret("artifact-registry", "thingworx");
        var extension = extension();
        extension.getSpec().setArtifactPullSecret("artifact-registry");

        var result =
                new ThingWorxExtensionReconciler(client)
                        .reconcile(extension, null)
                        .getResource()
                        .orElseThrow();
        var job =
                client.batch()
                        .v1()
                        .jobs()
                        .inNamespace("thingworx")
                        .withName(result.getStatus().getInstallerJob())
                        .get();
        var pod = job.getSpec().getTemplate().getSpec();
        assertThat(valueOf(pod.getContainers().getFirst().getEnv(), "DOCKER_CONFIG"))
                .isEqualTo("/registry-auth");
        assertThat(pod.getVolumes())
                .anySatisfy(
                        volume -> {
                            assertThat(volume.getName()).isEqualTo("artifact-registry-auth");
                            assertThat(volume.getSecret().getSecretName())
                                    .isEqualTo("artifact-registry");
                            assertThat(volume.getSecret().getItems().getFirst().getKey())
                                    .isEqualTo(".dockerconfigjson");
                            assertThat(volume.getSecret().getItems().getFirst().getPath())
                                    .isEqualTo("config.json");
                        });
    }

    @Test
    void installerJobHasTtlSecondsAfterFinished() {
        var cluster = readyCluster();
        client.resource(cluster).create();
        var extension = extension();
        var reconciler = new ThingWorxExtensionReconciler(client);

        var result = reconciler.reconcile(extension, null).getResource().orElseThrow();

        var job =
                client.batch()
                        .v1()
                        .jobs()
                        .inNamespace("thingworx")
                        .withName(result.getStatus().getInstallerJob())
                        .get();
        assertThat(job.getSpec().getTtlSecondsAfterFinished()).isEqualTo(3600);
    }

    @Test
    void installerJobRespectsClusterTtlConfiguration() {
        var cluster = readyCluster();
        cluster.getSpec().setJobTtlSecondsAfterFinished(7200);
        client.resource(cluster).create();
        var extension = extension();
        var reconciler = new ThingWorxExtensionReconciler(client);

        var result = reconciler.reconcile(extension, null).getResource().orElseThrow();
        var job =
                client.batch()
                        .v1()
                        .jobs()
                        .inNamespace("thingworx")
                        .withName(result.getStatus().getInstallerJob())
                        .get();
        assertThat(job.getSpec().getTtlSecondsAfterFinished()).isEqualTo(7200);
    }

    @Test
    void cleanupDeletesExtensionJobs() {
        var cluster = readyCluster();
        client.resource(cluster).create();
        var extension = extension();
        extension.getMetadata().setUid("test-uid-cleanup");
        var reconciler = new ThingWorxExtensionReconciler(client);

        var importing = reconciler.reconcile(extension, null).getResource().orElseThrow();
        var jobName = importing.getStatus().getInstallerJob();
        assertThat(client.batch().v1().jobs().inNamespace("thingworx").withName(jobName).get())
                .isNotNull();
        // Ensure job has TTL before cleanup
        assertThat(
                        client.batch()
                                .v1()
                                .jobs()
                                .inNamespace("thingworx")
                                .withName(jobName)
                                .get()
                                .getSpec()
                                .getTtlSecondsAfterFinished())
                .isEqualTo(3600);

        reconciler.cleanup(extension, null);

        assertThat(
                        client.batch()
                                .v1()
                                .jobs()
                                .inNamespace("thingworx")
                                .withLabel("thingworx.us-ignite.org/extension", "entities")
                                .list()
                                .getItems())
                .isEmpty();
    }

    @Test
    void failedInstallerJobSetsFailureStatusAndPreservesJobReason() {
        client.resource(readyCluster()).create();
        var extension = extension();
        var reconciler = new ThingWorxExtensionReconciler(client);
        var importing = reconciler.reconcile(extension, null).getResource().orElseThrow();

        var job =
                client.batch()
                        .v1()
                        .jobs()
                        .inNamespace("thingworx")
                        .withName(importing.getStatus().getInstallerJob())
                        .get();
        job.setStatus(
                new JobStatusBuilder()
                        .withFailed(1)
                        .withConditions(
                                new JobConditionBuilder()
                                        .withType("Failed")
                                        .withReason("ImagePullBackOff")
                                        .withMessage("installer image unavailable")
                                        .build())
                        .build());
        client.resource(job).update();

        var result = reconciler.reconcile(extension, null).getResource().orElseThrow();
        assertThat(result.getStatus().getPhase()).isEqualTo("Failed");
        assertThat(result.getStatus().getMessage())
                .contains("ImagePullBackOff: installer image unavailable");
    }

    @Test
    void completedRestartRequiredImportRequestsAPlatformRestart() {
        client.resource(readyCluster()).create();
        var extension = extension();
        extension.getSpec().getImportPolicy().setJarResources(true);
        var reconciler = new ThingWorxExtensionReconciler(client);
        var importing = reconciler.reconcile(extension, null).getResource().orElseThrow();
        var job =
                client.batch()
                        .v1()
                        .jobs()
                        .inNamespace("thingworx")
                        .withName(importing.getStatus().getInstallerJob())
                        .get();
        job.setStatus(new JobStatusBuilder().withSucceeded(1).build());
        client.resource(job).update();

        var result = reconciler.reconcile(importing, null).getResource().orElseThrow();

        assertThat(result.getStatus().getPhase()).isEqualTo("Restarting");
        assertThat(result.getStatus().getRequestedRestart())
                .isEqualTo(importing.getStatus().getLastAttemptedFingerprint());
    }

    @Test
    void doesNotReportReadyWhenReadyReplicasAreStillFromTheOldRevision() {
        var cluster = readyCluster();
        client.resource(cluster).create();
        var extension = restartingExtension();
        client.resource(
                        platform(
                                "sha256:restart",
                                new StatefulSetStatusBuilder()
                                        .withReplicas(1)
                                        .withReadyReplicas(1)
                                        .withAvailableReplicas(1)
                                        .withCurrentRevision("old")
                                        .withUpdateRevision("new")
                                        .build()))
                .create();

        var result = new ThingWorxExtensionReconciler(client).reconcile(extension, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase())
                .isEqualTo("Restarting");
    }

    @Test
    void reportsReadyOnlyAfterRequestedRevisionIsHealthy() {
        var cluster = readyCluster();
        client.resource(cluster).create();
        var extension = restartingExtension();
        client.resource(
                        platform(
                                "sha256:restart",
                                new StatefulSetStatusBuilder()
                                        .withReplicas(1)
                                        .withUpdatedReplicas(1)
                                        .withReadyReplicas(1)
                                        .withAvailableReplicas(1)
                                        .withCurrentRevision("new")
                                        .withUpdateRevision("new")
                                        .build()))
                .create();

        var result = new ThingWorxExtensionReconciler(client).reconcile(extension, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("Ready");
    }

    @Test
    void keepsRestartingWhenRolloutIsNotHealthy() {
        var cluster = readyCluster();
        client.resource(cluster).create();
        var extension = restartingExtension();
        client.resource(
                        platform(
                                "sha256:restart",
                                new StatefulSetStatusBuilder()
                                        .withReplicas(1)
                                        .withUpdatedReplicas(1)
                                        .withReadyReplicas(0)
                                        .withAvailableReplicas(0)
                                        .withCurrentRevision("new")
                                        .withUpdateRevision("new")
                                        .build()))
                .create();

        var result = new ThingWorxExtensionReconciler(client).reconcile(extension, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase())
                .isEqualTo("Restarting");
    }

    @Test
    void rejectsDependencyAssignedToAnotherCluster() {
        client.resource(readyCluster()).create();
        client.resource(extension("shared", "other-cluster")).create();
        var extension = extension("alpha", "production", "shared");

        var result = new ThingWorxExtensionReconciler(client).reconcile(extension, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("Failed");
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .contains("shared", "different");
    }

    @Test
    void reportsMissingDependencyWithItsName() {
        client.resource(readyCluster()).create();
        var extension = extension("alpha", "production", "missing");

        var result = new ThingWorxExtensionReconciler(client).reconcile(extension, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase())
                .isEqualTo("WaitingForDependencies");
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .contains("missing", "does not exist");
    }

    @Test
    void rejectsSelfAndDuplicateDependencies() {
        client.resource(readyCluster()).create();
        var self = extension("alpha", "production", "alpha");
        var duplicate = extension("beta", "production", "ready", "ready");

        var selfResult = new ThingWorxExtensionReconciler(client).reconcile(self, null);
        var duplicateResult = new ThingWorxExtensionReconciler(client).reconcile(duplicate, null);

        assertThat(selfResult.getResource().orElseThrow().getStatus().getMessage())
                .contains("cannot depend on itself");
        assertThat(duplicateResult.getResource().orElseThrow().getStatus().getMessage())
                .contains("duplicate");
    }

    @Test
    void detectsTwoNodeDependencyCycle() {
        client.resource(readyCluster()).create();
        client.resource(extension("alpha", "production", "beta")).create();
        var beta = extension("beta", "production", "alpha");
        client.resource(beta).create();

        var result =
                new ThingWorxExtensionReconciler(client)
                        .reconcile(extension("alpha", "production", "beta"), null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("Failed");
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .contains("alpha", "beta");
    }

    @Test
    void detectsThreeNodeDependencyCycle() {
        client.resource(readyCluster()).create();
        client.resource(extension("alpha", "production", "beta")).create();
        client.resource(extension("beta", "production", "gamma")).create();
        client.resource(extension("gamma", "production", "alpha")).create();

        var result =
                new ThingWorxExtensionReconciler(client)
                        .reconcile(extension("alpha", "production", "beta"), null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("Failed");
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .contains("alpha", "beta", "gamma");
    }

    @Test
    void ordersImportByDependenciesBeforeName() {
        client.resource(readyCluster()).create();
        var beta = extension("beta", "production");
        client.resource(beta).create();
        var alpha = extension("alpha", "production", "beta");
        var reconciler = new ThingWorxExtensionReconciler(client);

        var alphaWaiting = reconciler.reconcile(alpha, null);
        assertThat(alphaWaiting.getResource().orElseThrow().getStatus().getPhase())
                .isEqualTo("WaitingForDependencies");

        var betaImporting = reconciler.reconcile(beta, null).getResource().orElseThrow();
        assertThat(betaImporting.getStatus().getPhase()).isEqualTo("Importing");
        assertThat(
                        client.batch()
                                .v1()
                                .jobs()
                                .inNamespace("thingworx")
                                .withName(betaImporting.getStatus().getInstallerJob())
                                .get())
                .isNotNull();

        var betaReady = betaImporting;
        var readyStatus = new org.us_ignite.thingworx.operator.api.ThingWorxExtensionStatus();
        readyStatus.setPhase("Ready");
        readyStatus.setObservedDigest(betaReady.getSpec().getArtifact().getDigest());
        readyStatus.setObservedFingerprint(betaReady.getStatus().getLastAttemptedFingerprint());
        betaReady.setStatus(readyStatus);
        client.resource(betaReady).update();

        var alphaImporting = reconciler.reconcile(alpha, null);
        assertThat(alphaImporting.getResource().orElseThrow().getStatus().getPhase())
                .isEqualTo("Importing");
        assertThat(
                        client.batch()
                                .v1()
                                .jobs()
                                .inNamespace("thingworx")
                                .withName(
                                        alphaImporting
                                                .getResource()
                                                .orElseThrow()
                                                .getStatus()
                                                .getInstallerJob())
                                .get())
                .isNotNull();
    }

    private String valueOf(
            java.util.List<io.fabric8.kubernetes.api.model.EnvVar> environment, String name) {
        return environment.stream()
                .filter(env -> name.equals(env.getName()))
                .findFirst()
                .orElseThrow()
                .getValue();
    }

    private ThingWorxExtension extension() {
        return extension("entities", "production");
    }

    private ThingWorxExtension extension(String name, String cluster, String... dependencies) {
        var artifact = new OciArtifactReference();
        artifact.setRepository("registry.example/extensions/" + name);
        artifact.setDigest(
                "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        var spec = new ThingWorxExtensionSpec();
        var ref = new ClusterReference();
        ref.setName(cluster);
        spec.setClusterRef(ref);
        spec.setArtifact(artifact);
        spec.setDependsOn(List.of(dependencies));
        var extension = new ThingWorxExtension();
        extension.setMetadata(
                new ObjectMetaBuilder()
                        .withName(name)
                        .withNamespace("thingworx")
                        .withGeneration(1L)
                        .build());
        extension.setSpec(spec);
        return extension;
    }

    private ThingWorxExtension restartingExtension() {
        var extension = extension();
        var policy = extension.getSpec().getImportPolicy();
        policy.setJarResources(true);
        var status = new org.us_ignite.thingworx.operator.api.ThingWorxExtensionStatus();
        status.setPhase("Restarting");
        status.setRequestedRestart("sha256:restart");
        extension.setStatus(status);
        return extension;
    }

    private io.fabric8.kubernetes.api.model.apps.StatefulSet platform(
            String restart, io.fabric8.kubernetes.api.model.apps.StatefulSetStatus status) {
        var statefulSet =
                new StatefulSetBuilder()
                        .withMetadata(
                                new ObjectMetaBuilder()
                                        .withName("production-platform")
                                        .withNamespace("thingworx")
                                        .build())
                        .withNewSpec()
                        .withReplicas(1)
                        .withNewTemplate()
                        .withNewMetadata()
                        .withAnnotations(
                                Map.of("thingworx.us-ignite.org/extension-restart", restart))
                        .endMetadata()
                        .endTemplate()
                        .endSpec()
                        .build();
        statefulSet.setStatus(status);
        return statefulSet;
    }

    private void createSecret(String name, String namespace) {
        if (name == null || name.isBlank() || namespace == null || namespace.isBlank()) return;
        if (client.secrets().inNamespace(namespace).withName(name).get() != null) return;
        client.resource(
                        new SecretBuilder()
                                .withNewMetadata()
                                .withName(name)
                                .withNamespace(namespace)
                                .endMetadata()
                                .withStringData(Map.of("password", "test", "appKey", "test", "key", "test"))
                                .build())
                .create();
    }

    private void ensureExtensionSecrets(ThingWorxCluster cluster) {
        var ns = cluster.getMetadata().getNamespace();
        if (cluster.getSpec().getExtensionImportAppKey() != null) createSecret(cluster.getSpec().getExtensionImportAppKey().getName(), ns);
        if (cluster.getSpec().getPlatformAdminPassword() != null) createSecret(cluster.getSpec().getPlatformAdminPassword().getName(), ns);
        if (cluster.getSpec().getImagePullSecret() != null && !cluster.getSpec().getImagePullSecret().isBlank())
            createSecret(cluster.getSpec().getImagePullSecret(), ns);
    }

    private ThingWorxCluster readyCluster() {
        var images = new ImageSet();
        images.setPlatform("registry.example/platform");
        images.setDatabaseInit("registry.example/db-init");
        images.setSecurityCli("registry.example/security");
        images.setIgnite("registry.example/ignite");
        images.setCxServer("registry.example/cx");
        images.setExtensionInstaller("registry.example/installer@sha256:installer");
        var database = new DatabaseSpec();
        database.setHost("postgres");
        database.setDatabase("thingworx");
        database.setSchema("thingworx");
        database.setUsername("thingworx");
        database.setAdminUsername("postgres");
        var storage = new StorageSpec();
        storage.setStorageClassName("rwx");
        var key = new SecretKeyReference();
        key.setName("extension-key");
        key.setKey("appKey");
        var password = new SecretKeyReference();
        password.setName("thingworx-platform-admin");
        password.setKey("password");
        var spec = new ThingWorxClusterSpec();
        spec.setImages(images);
        spec.setDatabase(database);
        spec.setStorage(storage);
        spec.setIngress(new IngressSpec());
        spec.setExtensionImportAppKey(key);
        spec.setPlatformAdminPassword(password);
        var status = new ThingWorxClusterStatus();
        status.setPhase("READY");
        var cluster = new ThingWorxCluster();
        cluster.setMetadata(
                new ObjectMetaBuilder().withName("production").withNamespace("thingworx").build());
        cluster.setSpec(spec);
        cluster.setStatus(status);
        ensureExtensionSecrets(cluster);
        return cluster;
    }

    @Test
    void missingExtensionImportAppKeySecretIsRejected() {
        var cluster = readyCluster();
        // readyCluster creates the secret, delete to simulate missing
        client.secrets().inNamespace("thingworx").withName("extension-key").delete();
        client.resource(cluster).create();
        var extension = extension();
        var result = new ThingWorxExtensionReconciler(client).reconcile(extension, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("Failed");
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .isEqualTo("Secret 'extension-key' not found in namespace 'thingworx'");
        assertThat(client.batch().v1().jobs().inNamespace("thingworx").list().getItems()).isEmpty();
    }

    @Test
    void missingImagePullSecretIsRejectedForExtension() {
        var cluster = readyCluster();
        cluster.getSpec().setImagePullSecret("missing-pull");
        client.resource(cluster).create();
        var extension = extension();
        var result = new ThingWorxExtensionReconciler(client).reconcile(extension, null);

        assertThat(result.getResource().orElseThrow().getStatus().getPhase()).isEqualTo("Failed");
        assertThat(result.getResource().orElseThrow().getStatus().getMessage())
                .contains("missing-pull");
        assertThat(client.batch().v1().jobs().inNamespace("thingworx").list().getItems()).isEmpty();
    }
}
