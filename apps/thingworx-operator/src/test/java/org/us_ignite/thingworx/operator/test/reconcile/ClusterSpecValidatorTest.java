package org.us_ignite.thingworx.operator.test.reconcile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.us_ignite.thingworx.operator.api.DatabaseSpec;
import org.us_ignite.thingworx.operator.api.ImageSet;
import org.us_ignite.thingworx.operator.api.SecretKeyReference;
import org.us_ignite.thingworx.operator.api.StorageSpec;
import org.us_ignite.thingworx.operator.api.ThingWorxClusterSpec;
import org.us_ignite.thingworx.operator.api.ThingWorxClusterSpecValidator;
import org.us_ignite.thingworx.operator.reconcile.ClusterSpecValidator;

/**
 * Pure JUnit test for {@link ClusterSpecValidator} / {@link ThingWorxClusterSpecValidator}.
 * No Kubernetes mock — validates all {@code validate()} branches except existence checks.
 */
class ClusterSpecValidatorTest {

    @Test
    void nullSpecIsRejected() {
        assertThat(ThingWorxClusterSpecValidator.validateSpec(null))
                .hasValue("spec.images.platform is required.");
        assertThat(ThingWorxClusterSpecValidator.validateSpecNullable(null))
                .isEqualTo("spec.images.platform is required.");
        assertThat(ClusterSpecValidator.validateSpec(null))
                .hasValue("spec.images.platform is required.");
    }

    @Test
    void nullImagesIsRejected() {
        var spec = validSpec();
        spec.setImages(null);
        assertThat(ThingWorxClusterSpecValidator.validateSpec(spec))
                .hasValue("spec.images.platform is required.");
    }

    @Test
    void blankPlatformImageIsRejected() {
        var spec = validSpec();
        spec.getImages().setPlatform("");
        assertThat(validate(spec)).isEqualTo("spec.images.platform is required.");
        spec.getImages().setPlatform(null);
        assertThat(validate(spec)).isEqualTo("spec.images.platform is required.");
        spec.getImages().setPlatform("   ");
        assertThat(validate(spec)).isEqualTo("spec.images.platform is required.");
    }

    @Test
    void blankDatabaseInitIsRejected() {
        var spec = validSpec();
        spec.getImages().setDatabaseInit("");
        assertThat(validate(spec))
                .isEqualTo("Platform, database-init, and security-cli images are required.");
    }

    @Test
    void blankSecurityCliIsRejected() {
        var spec = validSpec();
        spec.getImages().setSecurityCli("  ");
        assertThat(validate(spec))
                .isEqualTo("Platform, database-init, and security-cli images are required.");
    }

    @Test
    void blankExtensionInstallerIsRejected() {
        var spec = validSpec();
        spec.getImages().setExtensionInstaller(null);
        assertThat(validate(spec))
                .isEqualTo(
                        "spec.images.extensionInstaller is required; pin to 10.1.2 to match platform (was :latest).");
        spec.getImages().setExtensionInstaller("");
        assertThat(validate(spec))
                .isEqualTo(
                        "spec.images.extensionInstaller is required; pin to 10.1.2 to match platform (was :latest).");
    }

    @Test
    void extensionInstallerLatestIsRejected() {
        var spec = validSpec();
        spec.getImages().setExtensionInstaller("ghcr.io/us-ignite/thingworx/extension-installer:latest");
        assertThat(validate(spec))
                .isEqualTo("spec.images.extensionInstaller must be pinned, not :latest (use 10.1.2).");
        spec.getImages().setExtensionInstaller("my-registry/extension-installer:latest");
        assertThat(validate(spec))
                .isEqualTo("spec.images.extensionInstaller must be pinned, not :latest (use 10.1.2).");
    }

    @Test
    void haRequiresAllFourImages() {
        var spec = validSpec();
        spec.setEnableHA(true);
        // missing ignite
        var baseline = validSpec();
        baseline.setEnableHA(true);
        baseline.getImages().setIgnite(null);
        assertThat(validate(baseline)).isEqualTo("HA requires ignite, cxServer, zookeeper, and haProxy images.");

        baseline = validSpec();
        baseline.setEnableHA(true);
        baseline.getImages().setCxServer("");
        assertThat(validate(baseline)).isEqualTo("HA requires ignite, cxServer, zookeeper, and haProxy images.");

        baseline = validSpec();
        baseline.setEnableHA(true);
        baseline.getImages().setZookeeper("  ");
        assertThat(validate(baseline)).isEqualTo("HA requires ignite, cxServer, zookeeper, and haProxy images.");

        baseline = validSpec();
        baseline.setEnableHA(true);
        baseline.getImages().setHaProxy(null);
        assertThat(validate(baseline)).isEqualTo("HA requires ignite, cxServer, zookeeper, and haProxy images.");

        // with all HA images present, but missing cxServerAppKey should trigger later branch, not HA images branch
        spec.setEnableHA(true);
        spec.getImages().setIgnite("apacheignite/ignite:2.16.0");
        spec.getImages().setCxServer("ghcr.io/us-ignite/thingworx/cxserver-twx:10.1");
        spec.getImages().setZookeeper("zookeeper:3.8.4");
        spec.getImages().setHaProxy("haproxy:3.0.10");
        // Need cxServerAppKey for HA to be valid
        var appKey = new SecretKeyReference();
        appKey.setName("cx-key");
        appKey.setKey("key");
        spec.setCxServerAppKey(appKey);
        spec.getDatabase().setInternal(false);
        assertThat(validate(spec)).isNull();
    }

    @Test
    void missingPlatformAdminPasswordIsRejected() {
        var spec = validSpec();
        spec.setPlatformAdminPassword(null);
        assertThat(validate(spec))
                .isEqualTo("spec.platformAdminPassword must reference a Secret name and key.");

        spec = validSpec();
        spec.getPlatformAdminPassword().setName("");
        assertThat(validate(spec))
                .isEqualTo("spec.platformAdminPassword must reference a Secret name and key.");

        spec = validSpec();
        spec.getPlatformAdminPassword().setKey(null);
        assertThat(validate(spec))
                .isEqualTo("spec.platformAdminPassword must reference a Secret name and key.");
    }

    @Test
    void missingKeystorePasswordIsRejected() {
        var spec = validSpec();
        spec.setKeystorePassword(null);
        assertThat(validate(spec))
                .isEqualTo("spec.keystorePassword must reference a Secret name and key.");

        spec = validSpec();
        spec.getKeystorePassword().setName("  ");
        assertThat(validate(spec))
                .isEqualTo("spec.keystorePassword must reference a Secret name and key.");

        spec = validSpec();
        spec.getKeystorePassword().setKey("");
        assertThat(validate(spec))
                .isEqualTo("spec.keystorePassword must reference a Secret name and key.");
    }

    @Test
    void databaseNullIsRejected() {
        var spec = validSpec();
        spec.setDatabase(null);
        assertThat(validate(spec))
                .isEqualTo(
                        "Database, schema, username, and adminUsername are required; an external database also requires host.");
    }

    @Test
    void externalDatabaseWithoutHostIsRejected() {
        var spec = validSpec();
        spec.getDatabase().setInternal(false);
        spec.getDatabase().setHost(null);
        assertThat(validate(spec))
                .isEqualTo(
                        "Database, schema, username, and adminUsername are required; an external database also requires host.");
        spec.getDatabase().setHost("  ");
        assertThat(validate(spec))
                .isEqualTo(
                        "Database, schema, username, and adminUsername are required; an external database also requires host.");
    }

    @Test
    void missingDatabaseFieldsAreRejected() {
        var spec = validSpec();
        spec.getDatabase().setDatabase("");
        assertThat(validate(spec))
                .isEqualTo(
                        "Database, schema, username, and adminUsername are required; an external database also requires host.");

        spec = validSpec();
        spec.getDatabase().setSchema(null);
        assertThat(validate(spec))
                .isEqualTo(
                        "Database, schema, username, and adminUsername are required; an external database also requires host.");

        spec = validSpec();
        spec.getDatabase().setUsername("  ");
        assertThat(validate(spec))
                .isEqualTo(
                        "Database, schema, username, and adminUsername are required; an external database also requires host.");

        spec = validSpec();
        spec.getDatabase().setAdminUsername(null);
        assertThat(validate(spec))
                .isEqualTo(
                        "Database, schema, username, and adminUsername are required; an external database also requires host.");
    }

    @Test
    void haWithInternalDatabaseIsRejected() {
        var spec = validSpec();
        spec.setEnableHA(true);
        spec.getDatabase().setInternal(true);
        spec.getDatabase().setHost(null);
        // need to satisfy earlier image checks
        spec.getImages().setIgnite("apacheignite/ignite:2.16.0");
        spec.getImages().setCxServer("ghcr.io/us-ignite/thingworx/cxserver-twx:10.1");
        spec.getImages().setZookeeper("zookeeper:3.8.4");
        spec.getImages().setHaProxy("haproxy:3.0.10");
        var appKey = new SecretKeyReference();
        appKey.setName("cx-key");
        appKey.setKey("key");
        spec.setCxServerAppKey(appKey);
        // also need adminCredentials and image for internal, but HA+internal triggers before those? order matters: HA+internal is right after DB fields check, before internal PG check
        assertThat(validate(spec))
                .isEqualTo("enableHA requires database.internal=false and an external HA PostgreSQL service.");
    }

    @Test
    void haWithoutCxServerAppKeyIsRejected() {
        var spec = validSpec();
        spec.setEnableHA(true);
        spec.getDatabase().setInternal(false);
        spec.getDatabase().setHost("postgres.example.test");
        spec.getImages().setIgnite("apacheignite/ignite:2.16.0");
        spec.getImages().setCxServer("ghcr.io/us-ignite/thingworx/cxserver-twx:10.1");
        spec.getImages().setZookeeper("zookeeper:3.8.4");
        spec.getImages().setHaProxy("haproxy:3.0.10");
        spec.setCxServerAppKey(null);
        assertThat(validate(spec)).isEqualTo("enableHA requires spec.cxServerAppKey.");

        spec.setCxServerAppKey(new SecretKeyReference());
        spec.getCxServerAppKey().setName("");
        spec.getCxServerAppKey().setKey("key");
        assertThat(validate(spec)).isEqualTo("enableHA requires spec.cxServerAppKey.");

        spec.getCxServerAppKey().setName("cx-key");
        spec.getCxServerAppKey().setKey(null);
        assertThat(validate(spec)).isEqualTo("enableHA requires spec.cxServerAppKey.");
    }

    @Test
    void internalPostgresWithoutImageIsRejected() {
        var spec = validSpec();
        spec.getDatabase().setInternal(true);
        spec.getDatabase().setHost(null);
        spec.getDatabase().setImage(null);
        var admin = new SecretKeyReference();
        admin.setName("admin");
        admin.setKey("password");
        spec.getDatabase().setAdminCredentials(admin);
        assertThat(validate(spec))
                .isEqualTo("Internal PostgreSQL requires an image and database.adminCredentials.");

        spec.getDatabase().setImage("  ");
        assertThat(validate(spec))
                .isEqualTo("Internal PostgreSQL requires an image and database.adminCredentials.");
    }

    @Test
    void internalPostgresWithoutAdminCredentialsIsRejected() {
        var spec = validSpec();
        spec.getDatabase().setInternal(true);
        spec.getDatabase().setHost(null);
        spec.getDatabase().setImage("postgres:16");
        spec.getDatabase().setAdminCredentials(null);
        assertThat(validate(spec))
                .isEqualTo("Internal PostgreSQL requires an image and database.adminCredentials.");

        spec.getDatabase().setAdminCredentials(new SecretKeyReference());
        spec.getDatabase().getAdminCredentials().setName(null);
        spec.getDatabase().getAdminCredentials().setKey("password");
        assertThat(validate(spec))
                .isEqualTo("Internal PostgreSQL requires an image and database.adminCredentials.");

        spec.getDatabase().getAdminCredentials().setName("admin");
        spec.getDatabase().getAdminCredentials().setKey("  ");
        assertThat(validate(spec))
                .isEqualTo("Internal PostgreSQL requires an image and database.adminCredentials.");
    }

    @Test
    void storageValidation() {
        var spec = validSpec();
        spec.setStorage(null);
        assertThat(validate(spec))
                .isEqualTo("spec.storage.storageClassName must provide RWX storage.");

        spec = validSpec();
        spec.getStorage().setStorageClassName(null);
        assertThat(validate(spec))
                .isEqualTo("spec.storage.storageClassName must provide RWX storage.");

        spec.getStorage().setStorageClassName("  ");
        assertThat(validate(spec))
                .isEqualTo("spec.storage.storageClassName must provide RWX storage.");
    }

    @Test
    void kafkaWithoutImageIsRejected() {
        var spec = validSpec();
        spec.setKafkaEnabled(true);
        spec.getImages().setKafka(null);
        assertThat(validate(spec)).isEqualTo("spec.images.kafka is required when Kafka is enabled.");
        spec.getImages().setKafka("  ");
        assertThat(validate(spec)).isEqualTo("spec.images.kafka is required when Kafka is enabled.");
    }

    @Test
    void otelWithoutImageIsRejected() {
        var spec = validSpec();
        spec.setOtelEnabled(true);
        spec.getImages().setOtelCollector(null);
        assertThat(validate(spec))
                .isEqualTo("spec.images.otelCollector is required when OpenTelemetry is enabled.");
        spec.getImages().setOtelCollector("");
        assertThat(validate(spec))
                .isEqualTo("spec.images.otelCollector is required when OpenTelemetry is enabled.");
    }

    @Test
    void validSpecReturnsEmpty() {
        var spec = validSpec();
        assertThat(ThingWorxClusterSpecValidator.validateSpec(spec)).isEmpty();
        assertThat(validate(spec)).isNull();
        // optional should be empty as pure test pattern
        assertThat(ClusterSpecValidator.validateSpec(validHaSpec())).isEmpty();
    }

    @Test
    void validHaSpecReturnsEmpty() {
        var spec = validHaSpec();
        assertThat(validate(spec)).isNull();
        assertThat(ThingWorxClusterSpecValidator.validateSpec(spec)).isEmpty();
    }

    @Test
    void optionalVariantViaApiAndReconcileAliasMatch() {
        var spec = validSpec();
        Optional<String> viaApi = ThingWorxClusterSpecValidator.validateSpec(spec);
        Optional<String> viaAlias = ClusterSpecValidator.validateSpec(spec);
        assertThat(viaAlias).isEqualTo(viaApi);

        spec.setKafkaEnabled(true);
        Optional<String> viaApiInvalid = ThingWorxClusterSpecValidator.validateSpec(spec);
        Optional<String> viaAliasInvalid = ClusterSpecValidator.validateSpec(spec);
        assertThat(viaAliasInvalid).isEqualTo(viaApiInvalid);
    }

    // helpers

    private static String validate(ThingWorxClusterSpec spec) {
        return ThingWorxClusterSpecValidator.validateSpecNullable(spec);
    }

    private static ThingWorxClusterSpec validSpec() {
        var images = new ImageSet();
        images.setPlatform("ghcr.io/us-ignite/thingworx/platform-postgres:10.1.2");
        images.setDatabaseInit("ghcr.io/us-ignite/thingworx/postgres-init:10.1.2");
        images.setSecurityCli("ghcr.io/us-ignite/thingworx/security-tool:10.1.2");
        images.setExtensionInstaller("ghcr.io/us-ignite/thingworx/extension-installer:10.1.2");
        // defaults for ignite etc are already valid, keep them
        images.setIgnite("apacheignite/ignite:2.16.0");
        images.setCxServer("ghcr.io/us-ignite/thingworx/cxserver-twx:10.1");
        images.setZookeeper("zookeeper:3.8.4");
        images.setHaProxy("haproxy:3.0.10");

        var database = new DatabaseSpec();
        database.setInternal(false);
        database.setHost("postgres.example.test");
        database.setPort(5432);
        database.setDatabase("thingworx");
        database.setSchema("thingworx");
        database.setUsername("thingworx");
        database.setAdminUsername("postgres");

        var storage = new StorageSpec();
        storage.setStorageClassName("rwx");

        var spec = new ThingWorxClusterSpec();
        spec.setImages(images);
        spec.setDatabase(database);
        spec.setStorage(storage);
        spec.setEnableHA(false);
        spec.setKafkaEnabled(false);
        spec.setOtelEnabled(false);

        var admin = new SecretKeyReference();
        admin.setName("thingworx-platform-admin");
        admin.setKey("password");
        spec.setPlatformAdminPassword(admin);

        var keystore = new SecretKeyReference();
        keystore.setName("thingworx-keystore");
        keystore.setKey("password");
        spec.setKeystorePassword(keystore);

        return spec;
    }

    private static ThingWorxClusterSpec validHaSpec() {
        var spec = validSpec();
        spec.setEnableHA(true);
        spec.getDatabase().setInternal(false);
        spec.getDatabase().setHost("postgres-ha.example.test");
        var cx = new SecretKeyReference();
        cx.setName("cx-key");
        cx.setKey("key");
        spec.setCxServerAppKey(cx);
        return spec;
    }
}
