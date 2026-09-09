package org.us_ignite.thingworx.operator.api;

import java.util.Optional;

/**
 * Pure validation for {@link ThingWorxClusterSpec}. No {@code KubernetesClient} I/O.
 *
 * <p>Covers all {@code ThingWorxClusterReconciler.validate()} branches except existence checks
 * ({@code validateReferencedResourcesExist}) which remain client-dependent in the reconciler.
 */
public final class ThingWorxClusterSpecValidator {
    private ThingWorxClusterSpecValidator() {}

    /**
     * Validates the spec without Kubernetes lookups.
     *
     * @return empty if valid, otherwise the human-readable error message (same strings as reconciler)
     */
    public static Optional<String> validateSpec(ThingWorxClusterSpec spec) {
        String msg = validateSpecNullable(spec);
        return msg == null ? Optional.empty() : Optional.of(msg);
    }

    /** Nullable variant for call-sites that historically used {@code String null == valid}. */
    public static String validateSpecNullable(ThingWorxClusterSpec spec) {
        if (spec == null
                || spec.getImages() == null
                || blank(spec.getImages().getPlatform())) {
            return "spec.images.platform is required.";
        }
        if (blank(spec.getImages().getDatabaseInit())
                || blank(spec.getImages().getSecurityCli())) {
            return "Platform, database-init, and security-cli images are required.";
        }
        if (blank(spec.getImages().getExtensionInstaller())) {
            return "spec.images.extensionInstaller is required; pin to 10.1.2 to match platform (was :latest).";
        }
        if (spec.getImages().getExtensionInstaller().endsWith(":latest")) {
            return "spec.images.extensionInstaller must be pinned, not :latest (use 10.1.2).";
        }
        if (spec.isEnableHA()
                && (blank(spec.getImages().getIgnite())
                        || blank(spec.getImages().getCxServer())
                        || blank(spec.getImages().getZookeeper())
                        || blank(spec.getImages().getHaProxy()))) {
            return "HA requires ignite, cxServer, zookeeper, and haProxy images.";
        }
        if (invalidSecretReference(spec.getPlatformAdminPassword())) {
            return "spec.platformAdminPassword must reference a Secret name and key.";
        }
        if (invalidSecretReference(spec.getKeystorePassword())) {
            return "spec.keystorePassword must reference a Secret name and key.";
        }
        if (spec.getDatabase() == null
                || (!spec.getDatabase().isInternal() && blank(spec.getDatabase().getHost()))
                || blank(spec.getDatabase().getDatabase())
                || blank(spec.getDatabase().getSchema())
                || blank(spec.getDatabase().getUsername())
                || blank(spec.getDatabase().getAdminUsername())) {
            return "Database, schema, username, and adminUsername are required; an external database also requires host.";
        }
        if (spec.isEnableHA() && spec.getDatabase().isInternal()) {
            return "enableHA requires database.internal=false and an external HA PostgreSQL service.";
        }
        if (spec.isEnableHA() && invalidSecretReference(spec.getCxServerAppKey())) {
            return "enableHA requires spec.cxServerAppKey.";
        }
        if (spec.getDatabase().isInternal()
                && (blank(spec.getDatabase().getImage())
                        || invalidSecretReference(spec.getDatabase().getAdminCredentials()))) {
            return "Internal PostgreSQL requires an image and database.adminCredentials.";
        }
        if (spec.getStorage() == null || blank(spec.getStorage().getStorageClassName())) {
            return "spec.storage.storageClassName must provide RWX storage.";
        }
        if (spec.isKafkaEnabled() && blank(spec.getImages().getKafka())) {
            return "spec.images.kafka is required when Kafka is enabled.";
        }
        if (spec.isOtelEnabled() && blank(spec.getImages().getOtelCollector())) {
            return "spec.images.otelCollector is required when OpenTelemetry is enabled.";
        }
        return null;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean invalidSecretReference(SecretKeyReference ref) {
        return ref == null || blank(ref.getName()) || blank(ref.getKey());
    }
}
