package org.us_ignite.thingworx.operator.test.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.us_ignite.thingworx.operator.api.DatabaseSpec;
import org.us_ignite.thingworx.operator.api.ImageSet;
import org.us_ignite.thingworx.operator.api.IngressSpec;
import org.us_ignite.thingworx.operator.api.SecretKeyReference;
import org.us_ignite.thingworx.operator.api.StorageSpec;
import org.us_ignite.thingworx.operator.api.ThingWorxCluster;
import org.us_ignite.thingworx.operator.api.ThingWorxClusterSpec;

/**
 * CEL validation coverage for {@code charts/thingworx-operator/crds/thingworxclusters.yaml}.
 *
 * <p>CRD CEL rules are server-side; {@code kubernetes-server-mock} does not enforce them, so this
 * test replicates each CEL rule in a Java validator and asserts:
 * <ul>
 *   <li>the CRD YAML still contains the expected CEL expressions (prevents accidental removal),</li>
 *   <li>the Java mirror of each rule rejects invalid specs with the same message.</li>
 * </ul>
 * This gives helm-unittest/kubeconform-adjacent coverage without requiring a real API server.
 */
public class ThingWorxClusterCelValidationTest {

    // Mirror of CEL rules from charts/thingworx-operator/crds/thingworxclusters.yaml

    public static void validateForCreate(ThingWorxCluster cluster) {
        validate(cluster, null);
    }

    public static void validateForUpdate(ThingWorxCluster cluster, ThingWorxCluster old) {
        validate(cluster, old);
    }

    private static void validate(ThingWorxCluster cluster, ThingWorxCluster old) {
        ThingWorxClusterSpec spec = cluster.getSpec();
        if (spec == null) throw new IllegalArgumentException("spec is required");

        // immutable checks
        if (old != null && old.getSpec() != null) {
            boolean oldHa = old.getSpec().isEnableHA();
            if (oldHa != spec.isEnableHA()) {
                throw new IllegalArgumentException("spec.enableHA is immutable");
            }
            Boolean oldInternal = old.getSpec().getDatabase() != null ? old.getSpec().getDatabase().isInternal() : null;
            Boolean newInternal = spec.getDatabase() != null ? spec.getDatabase().isInternal() : null;
            if (oldInternal != null && newInternal != null && !oldInternal.equals(newInternal)) {
                throw new IllegalArgumentException("spec.database.internal is immutable");
            }
        }

        // enableHA requires database.internal=false
        if (spec.isEnableHA() && Boolean.TRUE.equals(spec.getDatabase() != null ? spec.getDatabase().isInternal() : true)) {
            throw new IllegalArgumentException("enableHA requires database.internal=false and external HA PostgreSQL");
        }
        // enableHA requires 4 images
        if (spec.isEnableHA()) {
            ImageSet img = spec.getImages();
            if (img == null || img.getIgnite() == null || img.getCxServer() == null || img.getZookeeper() == null || img.getHaProxy() == null) {
                throw new IllegalArgumentException("enableHA requires ignite, cxServer, zookeeper, and haProxy images");
            }
            if (img.getIgnite().isBlank() || img.getCxServer().isBlank() || img.getZookeeper().isBlank() || img.getHaProxy().isBlank()) {
                throw new IllegalArgumentException("enableHA requires ignite, cxServer, zookeeper, and haProxy images");
            }
        }
        // enableHA requires cxServerAppKey
        if (spec.isEnableHA() && spec.getCxServerAppKey() == null) {
            throw new IllegalArgumentException("enableHA requires cxServerAppKey");
        }
        // extensionInstaller not :latest
        if (spec.getImages() != null && spec.getImages().getExtensionInstaller() != null
                && spec.getImages().getExtensionInstaller().endsWith(":latest")) {
            throw new IllegalArgumentException("spec.images.extensionInstaller must be pinned, not :latest");
        }
        // external PG requires host
        if (spec.getDatabase() != null && Boolean.FALSE.equals(spec.getDatabase().isInternal())) {
            if (spec.getDatabase().getHost() == null || spec.getDatabase().getHost().isBlank()) {
                throw new IllegalArgumentException("external PostgreSQL requires database.host");
            }
        }
    }

    private static ThingWorxCluster minimalCluster() {
        var images = new ImageSet();
        images.setPlatform("ghcr.io/us-ignite/thingworx/platform-postgres:10.1.2");
        images.setDatabaseInit("ghcr.io/us-ignite/thingworx/postgres-init:10.1.2");
        images.setSecurityCli("ghcr.io/us-ignite/thingworx/security-tool:10.1.2");
        var db = new DatabaseSpec();
        db.setInternal(true);
        db.setDatabase("thingworx");
        db.setSchema("thingworx");
        db.setUsername("thingworx");
        db.setAdminUsername("postgres");
        var creds = new SecretKeyReference(); creds.setName("thingworx-database"); creds.setKey("password");
        db.setCredentials(creds);
        var adminCreds = new SecretKeyReference(); adminCreds.setName("thingworx-database-admin"); adminCreds.setKey("password");
        db.setAdminCredentials(adminCreds);
        var storage = new StorageSpec(); storage.setStorageClassName("rwx");
        var admin = new SecretKeyReference(); admin.setName("thingworx-platform-admin"); admin.setKey("password");
        var ks = new SecretKeyReference(); ks.setName("thingworx-keystore"); ks.setKey("password");
        var spec = new ThingWorxClusterSpec();
        spec.setImages(images); spec.setDatabase(db); spec.setStorage(storage);
        spec.setIngress(new IngressSpec()); spec.setPlatformAdminPassword(admin); spec.setKeystorePassword(ks);
        spec.setEnableHA(false);
        var cluster = new ThingWorxCluster();
        cluster.setMetadata(new io.fabric8.kubernetes.api.model.ObjectMetaBuilder().withName("demo").withNamespace("thingworx").build());
        cluster.setSpec(spec);
        return cluster;
    }

    // --- CRD file existence checks -------------------------------------------------

    @Test
    void crdYamlContainsAllCelRules() throws Exception {
        File crd = resolveCrdFile();
        String yaml = Files.readString(crd.toPath());
        assertThat(yaml).contains("spec.enableHA is immutable");
        assertThat(yaml).contains("spec.database.internal is immutable");
        assertThat(yaml).contains("enableHA requires database.internal=false");
        assertThat(yaml).contains("enableHA requires ignite, cxServer, zookeeper, and haProxy images");
        assertThat(yaml).contains("enableHA requires cxServerAppKey");
        assertThat(yaml).contains("spec.images.extensionInstaller must be pinned, not :latest");
        assertThat(yaml).contains("pvcAnnotations:");
        assertThat(yaml).contains("jobTtlSecondsAfterFinished:");
        assertThat(yaml).contains("external PostgreSQL requires database.host");
        // Ensure x-kubernetes-validations block exists
        assertThat(yaml).contains("x-kubernetes-validations");
    }

    // --- CEL logical validation ----------------------------------------------------

    @Test
    void enableHaFlipIsRejected() {
        var old = minimalCluster();
        old.getSpec().setEnableHA(false);
        var updated = minimalCluster();
        updated.getSpec().setEnableHA(true);
        // even with other HA fields, immutable flip should be rejected first
        assertThatThrownBy(() -> validateForUpdate(updated, old))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spec.enableHA is immutable");
    }

    @Test
    void databaseInternalFlipIsRejected() {
        var old = minimalCluster();
        old.getSpec().getDatabase().setInternal(true);
        var updated = minimalCluster();
        updated.getSpec().getDatabase().setInternal(false);
        updated.getSpec().getDatabase().setHost("postgres.example.test");
        assertThatThrownBy(() -> validateForUpdate(updated, old))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spec.database.internal is immutable");
    }

    @Test
    void externalPostgresWithoutHostIsRejected() {
        var cluster = minimalCluster();
        cluster.getSpec().getDatabase().setInternal(false);
        cluster.getSpec().getDatabase().setHost(null);
        assertThatThrownBy(() -> validateForCreate(cluster))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("external PostgreSQL requires database.host");

        cluster.getSpec().getDatabase().setHost("");
        assertThatThrownBy(() -> validateForCreate(cluster))
                .hasMessageContaining("external PostgreSQL requires database.host");
    }

    @Test
    void haWithoutCxServerAppKeyIsRejected() {
        var cluster = minimalCluster();
        cluster.getSpec().setEnableHA(true);
        cluster.getSpec().getDatabase().setInternal(false);
        cluster.getSpec().getDatabase().setHost("postgres.example.test");
        cluster.getSpec().setCxServerAppKey(null);
        assertThatThrownBy(() -> validateForCreate(cluster))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("enableHA requires cxServerAppKey");
    }

    @Test
    void haRequiresFourImages() {
        var cluster = minimalCluster();
        cluster.getSpec().setEnableHA(true);
        cluster.getSpec().getDatabase().setInternal(false);
        cluster.getSpec().getDatabase().setHost("postgres.example.test");
        var key = new SecretKeyReference(); key.setName("cx-key"); key.setKey("key");
        cluster.getSpec().setCxServerAppKey(key);
        // null out one required image
        cluster.getSpec().getImages().setIgnite(null);
        assertThatThrownBy(() -> validateForCreate(cluster))
                .hasMessageContaining("enableHA requires ignite");

        cluster.getSpec().getImages().setIgnite("apacheignite/ignite:2.16.0");
        cluster.getSpec().getImages().setHaProxy("");
        assertThatThrownBy(() -> validateForCreate(cluster))
                .hasMessageContaining("enableHA requires ignite");
    }

    @Test
    void haRequiresExternalDatabase() {
        var cluster = minimalCluster();
        cluster.getSpec().setEnableHA(true);
        // internal true -> should fail
        cluster.getSpec().getDatabase().setInternal(true);
        var key = new SecretKeyReference(); key.setName("cx-key"); key.setKey("key");
        cluster.getSpec().setCxServerAppKey(key);
        assertThatThrownBy(() -> validateForCreate(cluster))
                .hasMessageContaining("enableHA requires database.internal=false");
    }

    @Test
    void extensionInstallerLatestIsRejected() {
        var cluster = minimalCluster();
        cluster.getSpec().getImages().setExtensionInstaller("ghcr.io/us-ignite/thingworx/extension-installer:latest");
        assertThatThrownBy(() -> validateForCreate(cluster))
                .hasMessageContaining("spec.images.extensionInstaller must be pinned");
    }

    @Test
    void validHaSpecPasses() {
        var cluster = minimalCluster();
        cluster.getSpec().setEnableHA(true);
        cluster.getSpec().getDatabase().setInternal(false);
        cluster.getSpec().getDatabase().setHost("postgres.example.test");
        var key = new SecretKeyReference(); key.setName("cx-key"); key.setKey("key");
        cluster.getSpec().setCxServerAppKey(key);
        // images already have defaults for ignite/cxServer/zookeeper/haProxy
        validateForCreate(cluster); // should not throw
    }

    @Test
    void validSingleNodeSpecPasses() {
        var cluster = minimalCluster();
        validateForCreate(cluster);
    }

    private File resolveCrdFile() {
        for (String candidate : List.of(
                "charts/thingworx-operator/crds/thingworx.us-ignite.org_thingworxclusters.yaml",
                "../../charts/thingworx-operator/crds/thingworx.us-ignite.org_thingworxclusters.yaml",
                "../charts/thingworx-operator/crds/thingworx.us-ignite.org_thingworxclusters.yaml")) {
            File f = new File(candidate);
            if (f.isFile()) return f.getAbsoluteFile();
        }
        Path cur = Path.of("").toAbsolutePath();
        for (int i = 0; i < 5; i++) {
            Path p = cur.resolve("charts/thingworx-operator/crds/thingworx.us-ignite.org_thingworxclusters.yaml");
            if (Files.isRegularFile(p)) return p.toFile();
            cur = cur.getParent();
            if (cur == null) break;
        }
        throw new IllegalStateException("CRD file not found from " + Path.of("").toAbsolutePath());
    }
}
