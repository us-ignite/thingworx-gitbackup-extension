package org.us_ignite.thingworx.operator.reconcile;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import org.junit.jupiter.api.Test;
import org.us_ignite.thingworx.operator.api.DatabaseSpec;
import org.us_ignite.thingworx.operator.api.ImageSet;
import org.us_ignite.thingworx.operator.api.SecretKeyReference;
import org.us_ignite.thingworx.operator.api.StorageSpec;
import org.us_ignite.thingworx.operator.api.ThingWorxCluster;
import org.us_ignite.thingworx.operator.api.ThingWorxClusterSpec;

class ThingWorxResourcesTest {
    @Test
    void disabledOptionalServicesProduceNoResources() {
        var cluster = new ThingWorxCluster();
        cluster.setMetadata(
                new ObjectMetaBuilder().withName("demo").withNamespace("thingworx").build());
        cluster.setSpec(new ThingWorxClusterSpec());

        assertThat(ThingWorxResources.optional(cluster)).isEmpty();
    }

    @Test
    void sharedStorageIsReadWriteManyForThePlatformCluster() {
        var cluster = cluster();
        cluster.getSpec().setEnableHA(true);

        var shared =
                (PersistentVolumeClaim)
                        ThingWorxResources.precheck(cluster).stream()
                                .filter(
                                        resource ->
                                                "demo-shared"
                                                        .equals(resource.getMetadata().getName()))
                                .findFirst()
                                .orElseThrow();

        assertThat(shared.getSpec().getAccessModes()).containsExactly("ReadWriteMany");
        assertThat(shared.getSpec().getStorageClassName()).isEqualTo("rwx");
    }

    @Test
    void componentStorageRemainsReadWriteOnce() {
        var cluster = cluster();
        cluster.getSpec().setEnableHA(true);

        var zookeeper =
                (StatefulSet)
                        ThingWorxResources.coordination(cluster).stream()
                                .filter(resource -> resource instanceof StatefulSet)
                                .filter(
                                        resource ->
                                                "demo-zookeeper"
                                                        .equals(resource.getMetadata().getName()))
                                .findFirst()
                                .orElseThrow();

        var zookeeperData = zookeeper.getSpec().getVolumeClaimTemplates().getFirst();

        assertThat(zookeeperData.getSpec().getAccessModes()).containsExactly("ReadWriteOnce");
    }

    @Test
    void internalDatabaseCreatesPostgresStatefulSetAndService() {
        var cluster = cluster();
        var database = cluster.getSpec().getDatabase();
        database.setInternal(true);
        database.setImage("postgres:16");
        var admin = new SecretKeyReference();
        admin.setName("database-admin");
        admin.setKey("password");
        database.setAdminCredentials(admin);

        var resources = ThingWorxResources.precheck(cluster);
        var postgres =
                (StatefulSet)
                        resources.stream()
                                .filter(resource -> resource instanceof StatefulSet)
                                .findFirst()
                                .orElseThrow();
        var service =
                (Service)
                        resources.stream()
                                .filter(resource -> resource instanceof Service)
                                .filter(
                                        resource ->
                                                "demo-postgres"
                                                        .equals(resource.getMetadata().getName()))
                                .findFirst()
                                .orElseThrow();

        assertThat(postgres.getMetadata().getName()).isEqualTo("demo-postgres");
        assertThat(postgres.getSpec().getReplicas()).isEqualTo(1);
        assertThat(postgres.getSpec().getTemplate().getSpec().getContainers().getFirst().getImage())
                .isEqualTo("postgres:16");
        assertThat(service.getSpec().getPorts().getFirst().getPort()).isEqualTo(5432);
    }

    @Test
    void everyPlatformReplicaUsesTheSharedClaim() {
        var cluster = cluster();

        var platform = (StatefulSet) ThingWorxResources.platform(cluster, 2).getFirst();
        var pod = platform.getSpec().getTemplate().getSpec();

        assertThat(platform.getSpec().getReplicas()).isEqualTo(2);
        assertThat(pod.getVolumes())
                .anySatisfy(
                        volume ->
                                assertThat(volume.getPersistentVolumeClaim().getClaimName())
                                        .isEqualTo("demo-shared"));
        assertThat(pod.getContainers().getFirst().getVolumeMounts())
                .anySatisfy(
                        volumeMount ->
                                assertThat(volumeMount.getMountPath())
                                        .isEqualTo("/ThingworxStorage"));
    }

    @Test
    void platformWaitsForTheVendorHealthEndpointBeforeReceivingTraffic() {
        var cluster = cluster();

        var platform = (StatefulSet) ThingWorxResources.platform(cluster, 2).getFirst();
        var container = platform.getSpec().getTemplate().getSpec().getContainers().getFirst();

        assertThat(container.getStartupProbe().getHttpGet().getPath())
                .isEqualTo("/Thingworx/health");
        assertThat(container.getStartupProbe().getFailureThreshold()).isEqualTo(40);
        assertThat(container.getReadinessProbe().getHttpGet().getPath())
                .isEqualTo("/Thingworx/health");
        assertThat(container.getReadinessProbe().getInitialDelaySeconds()).isEqualTo(120);
        assertThat(container.getReadinessProbe().getPeriodSeconds()).isEqualTo(15);
    }

    private ThingWorxCluster cluster() {
        var images = new ImageSet();
        images.setPlatform("thingworx/platform-postgres:10.1.1");
        images.setZookeeper("zookeeper:3.8.4");
        images.setIgnite("apacheignite/ignite:2.16.0");
        var database = new DatabaseSpec();
        database.setInternal(false);
        database.setHost("postgres");
        database.setDatabase("thingworx");
        database.setSchema("thingworx");
        database.setUsername("thingworx");
        database.setAdminUsername("postgres");
        var storage = new StorageSpec();
        storage.setStorageClassName("rwx");
        var spec = new ThingWorxClusterSpec();
        spec.setImages(images);
        spec.setDatabase(database);
        var password = new SecretKeyReference();
        password.setName("thingworx-platform-admin");
        password.setKey("password");
        spec.setPlatformAdminPassword(password);
        var keystorePassword = new SecretKeyReference();
        keystorePassword.setName("thingworx-keystore");
        keystorePassword.setKey("password");
        spec.setKeystorePassword(keystorePassword);
        spec.setStorage(storage);
        var cluster = new ThingWorxCluster();
        cluster.setMetadata(
                new ObjectMetaBuilder().withName("demo").withNamespace("thingworx").build());
        cluster.setSpec(spec);
        return cluster;
    }

    @Test
    void platformReadsInitialAdminPasswordFromTheReferencedSecret() {
        var cluster = cluster();

        var platform = (StatefulSet) ThingWorxResources.platform(cluster, 1).getFirst();
        var adminPassword =
                platform
                        .getSpec()
                        .getTemplate()
                        .getSpec()
                        .getContainers()
                        .getFirst()
                        .getEnv()
                        .stream()
                        .filter(value -> value.getName().equals("THINGWORX_INITIAL_ADMIN_PASSWORD"))
                        .findFirst()
                        .orElseThrow();

        assertThat(adminPassword.getValue()).isNull();
        assertThat(adminPassword.getValueFrom().getSecretKeyRef().getName())
                .isEqualTo("thingworx-platform-admin");
        assertThat(adminPassword.getValueFrom().getSecretKeyRef().getKey()).isEqualTo("password");
    }

    @Test
    void initializationJobNamesAreStableForUnchangedInputs() {
        var cluster = cluster();

        var first = ((Job) ThingWorxResources.database(cluster).getFirst()).getMetadata().getName();
        var second =
                ((Job) ThingWorxResources.database(cluster).getFirst()).getMetadata().getName();

        assertThat(first).isEqualTo(second).startsWith("demo-database-init-");
    }

    @Test
    void initializationJobNamesChangeWhenAnImageOrDatabaseInputChanges() {
        var cluster = cluster();
        var originalDatabaseJob =
                ((Job) ThingWorxResources.database(cluster).getFirst()).getMetadata().getName();
        var originalSecurityJob =
                ((Job) ThingWorxResources.database(cluster).get(1)).getMetadata().getName();

        cluster.getSpec().getImages().setDatabaseInit("registry.example/db-init@sha256:changed");
        var changedImageJob =
                ((Job) ThingWorxResources.database(cluster).getFirst()).getMetadata().getName();
        cluster.getSpec().getDatabase().setSchema("changed_schema");
        var changedDatabaseJob =
                ((Job) ThingWorxResources.database(cluster).getFirst()).getMetadata().getName();
        var unchangedSecurityJob =
                ((Job) ThingWorxResources.database(cluster).get(1)).getMetadata().getName();

        assertThat(changedImageJob).isNotEqualTo(originalDatabaseJob);
        assertThat(changedDatabaseJob).isNotEqualTo(changedImageJob);
        assertThat(unchangedSecurityJob).isEqualTo(originalSecurityJob);
    }
}
