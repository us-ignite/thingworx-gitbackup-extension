package org.us_ignite.thingworx.operator.reconcile;

import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.EventSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.us_ignite.thingworx.operator.api.SecretKeyReference;
import org.us_ignite.thingworx.operator.api.ThingWorxCluster;
import org.us_ignite.thingworx.operator.api.ThingWorxExtension;
import org.us_ignite.thingworx.operator.api.ThingWorxExtensionStatus;
import org.us_ignite.thingworx.operator.metrics.OperatorMetrics;

/** Imports one digest-pinned OCI extension at a time for each target cluster. */
@ControllerConfiguration(finalizerName = "thingworx.us-ignite.org/extension-finalizer")
public class ThingWorxExtensionReconciler
        implements Reconciler<ThingWorxExtension>, Cleaner<ThingWorxExtension> {
    private static final Logger LOG = LoggerFactory.getLogger(ThingWorxExtensionReconciler.class);
    private static final String RESTART_ANNOTATION = "thingworx.us-ignite.org/extension-restart";
    private final KubernetesClient client;
    private final ExtensionDependencyAnalyzer dependencyAnalyzer;

    public ThingWorxExtensionReconciler(KubernetesClient client) {
        this(client, new ExtensionDependencyAnalyzer());
    }

    public ThingWorxExtensionReconciler(
            KubernetesClient client, ExtensionDependencyAnalyzer dependencyAnalyzer) {
        this.client = client;
        this.dependencyAnalyzer =
                dependencyAnalyzer != null ? dependencyAnalyzer : new ExtensionDependencyAnalyzer();
    }

    @Override
    public UpdateControl<ThingWorxExtension> reconcile(
            ThingWorxExtension extension, Context<ThingWorxExtension> context) {
        long startNanos = System.nanoTime();
        String extKey = extensionKey(extension);
        String entryPhase = extension.getStatus() == null ? null : extension.getStatus().getPhase();
        LOG.info(
                "extension={} clusterRef={} phase={} event=reconcileStart generation={}",
                extKey,
                extension.getSpec() == null || extension.getSpec().getClusterRef() == null
                        ? "unknown"
                        : extension.getSpec().getClusterRef().getName(),
                entryPhase,
                extension.getMetadata() == null ? null : extension.getMetadata().getGeneration());
        UpdateControl<ThingWorxExtension> result;
        try {
            var error = validate(extension);
            if (error != null) {
                result = status(extension, "Failed", "InvalidSpec", error, false);
            } else {
                result = reconcileInternal(extension, context);
            }
        } catch (Exception e) {
            LOG.error("extension={} phase={} event=reconcileError error={}", extKey, entryPhase, e.toString(), e);
            throw e;
        } finally {
            long durationNanos = System.nanoTime() - startNanos;
            OperatorMetrics.recordReconcile("ThingWorxExtension", extKey, durationNanos);
            LOG.info(
                    "extension={} phase={} event=reconcileComplete durationMs={}",
                    extKey,
                    entryPhase,
                    durationNanos / 1_000_000);
        }
        return result;
    }

    private UpdateControl<ThingWorxExtension> reconcileInternal(
            ThingWorxExtension extension, Context<ThingWorxExtension> context) {
        var error = validate(extension);
        if (error != null) return status(extension, "Failed", "InvalidSpec", error, false);
        var namespace = extension.getMetadata().getNamespace();
        var cluster =
                client.resources(ThingWorxCluster.class)
                        .inNamespace(namespace)
                        .withName(extension.getSpec().getClusterRef().getName())
                        .get();
        if (cluster == null)
            return status(
                    extension,
                    "WaitingForCluster",
                    "ClusterNotFound",
                    "Referenced ThingWorxCluster does not exist.",
                    false);
        if (!"READY".equals(cluster.getStatus() == null ? null : cluster.getStatus().getPhase())) {
            return status(
                    extension,
                    "WaitingForCluster",
                    "ClusterNotReady",
                    "Target cluster is not Ready.",
                    false);
        }
        if (cluster.getSpec().getExtensionImportAppKey() == null) {
            return status(
                    extension,
                    "Failed",
                    "ImportCredentialMissing",
                    "Target cluster requires spec.extensionImportAppKey.",
                    false);
        }
        // 012: extensionInstaller must be pinned when extensions are used
        var extensionImage =
                cluster.getSpec().getImages() == null
                        ? null
                        : cluster.getSpec().getImages().getExtensionInstaller();
        if (extensionImage == null || extensionImage.isBlank()) {
            return status(
                    extension,
                    "Failed",
                    "ExtensionInstallerMissing",
                    "Target cluster spec.images.extensionInstaller is required when extensions are used (pin to 10.1.2).",
                    false);
        }
        if (extensionImage.endsWith(":latest")) {
            return status(
                    extension,
                    "Failed",
                    "ExtensionInstallerNotPinned",
                    "spec.images.extensionInstaller must be pinned, not :latest (use 10.1.2).",
                    false);
        }
        var appKey = cluster.getSpec().getExtensionImportAppKey();
        var missingAppKeySecret = missingSecret(appKey.getName(), namespace);
        if (missingAppKeySecret != null) {
            return status(extension, "Failed", "ImportCredentialMissing", missingAppKeySecret, false);
        }
        var imagePull = cluster.getSpec().getImagePullSecret();
        if (imagePull != null && !imagePull.isBlank()) {
            var missingPull = missingSecret(imagePull, namespace);
            if (missingPull != null) return status(extension, "Failed", "MissingSecret", missingPull, false);
        }
        if (extension.getSpec().getArtifactPullSecret() != null
                && !extension.getSpec().getArtifactPullSecret().isBlank()) {
            var missingArtifactPull =
                    missingSecret(extension.getSpec().getArtifactPullSecret(), namespace);
            if (missingArtifactPull != null)
                return status(extension, "Failed", "MissingSecret", missingArtifactPull, false);
        }
        if (extension.getStatus() != null
                && "Restarting".equals(extension.getStatus().getPhase())) {
            return platformReady(cluster, extension)
                    ? ready(extension, fingerprint(extension))
                    : status(
                            extension,
                            "Restarting",
                            "PlatformRestarting",
                            "Waiting for readiness-gated platform rollout.",
                            false);
        }
        var extensions =
                new ArrayList<>(
                        client.resources(ThingWorxExtension.class)
                                .inNamespace(namespace)
                                .list()
                                .getItems());
        var dependencyAnalysis = analyzeDependencies(extension, extensions);
        if (dependencyAnalysis.crossClusterDependency() != null) {
            return status(
                    extension,
                    "Failed",
                    "DependencyClusterMismatch",
                    "Dependency '"
                            + dependencyAnalysis.crossClusterDependency()
                            + "' targets a different ThingWorxCluster.",
                    false);
        }
        if (dependencyAnalysis.missingDependency() != null) {
            return status(
                    extension,
                    "WaitingForDependencies",
                    "DependencyNotFound",
                    "Dependency '"
                            + dependencyAnalysis.missingDependency()
                            + "' does not exist in namespace '"
                            + namespace
                            + "'.",
                    false);
        }
        if (dependencyAnalysis.cycle() != null) {
            return status(
                    extension,
                    "Failed",
                    "DependencyCycle",
                    "Extension dependency cycle detected: "
                            + String.join(" -> ", dependencyAnalysis.cycle()),
                    false);
        }
        if (!dependenciesReady(extension, namespace)) {
            return status(
                    extension,
                    "WaitingForDependencies",
                    "DependenciesNotReady",
                    "Waiting for prerequisite extensions.",
                    false);
        }
        if (!isNextForCluster(extension, extensions))
            return status(
                    extension,
                    "WaitingForTurn",
                    "ImportSerialized",
                    "Another extension is being reconciled for this cluster.",
                    false);
        var desiredFingerprint = fingerprint(extension);
        if (extension.getStatus() != null
                && "Ready".equals(extension.getStatus().getPhase())
                && desiredFingerprint.equals(extension.getStatus().getObservedFingerprint()))
            return status(
                    extension,
                    "Ready",
                    "Installed",
                    "Requested OCI artifact and import policy are installed.",
                    false);

        var updating =
                extension.getStatus() != null
                        && extension.getStatus().getObservedFingerprint() != null;
        var jobName = jobName(extension, desiredFingerprint);
        var existing = client.batch().v1().jobs().inNamespace(namespace).withName(jobName).get();
        if (existing == null) {
            client.resource(installerJob(extension, cluster, jobName, desiredFingerprint)).create();
            return status(
                    extension,
                    updating ? "Updating" : "Importing",
                    "InstallerJobCreated",
                    updating
                            ? "Installer Job created to update the installed extension."
                            : "Installer Job created for OCI artifact.",
                    false);
        }
        if (failed(existing))
            return status(
                    extension, "Failed", "InstallerJobFailed", installerFailure(existing), false);
        if (!complete(existing))
            return status(
                    extension,
                    updating ? "Updating" : "Importing",
                    "InstallerRunning",
                    "Waiting for installer Job completion.",
                    false);
        if (extension.getSpec().getImportPolicy().requiresRestart()) {
            restartPlatform(cluster, extension, desiredFingerprint);
            var restartStatus =
                    statusObject(
                            extension,
                            "Restarting",
                            "PlatformRestartRequested",
                            "Installer completed; platform rollout requested for executable/web extension content.");
            restartStatus.setRequestedRestart(desiredFingerprint);
            extension.setStatus(restartStatus);
            return UpdateControl.patchStatus(extension).rescheduleAfter(Duration.ofSeconds(15));
        }
        return ready(extension, desiredFingerprint);
    }

    @Override
    public DeleteControl cleanup(
            ThingWorxExtension extension, Context<ThingWorxExtension> context) {
        var namespace = extension.getMetadata().getNamespace();
        var name = extension.getMetadata().getName();
        String extKey = extensionKey(extension);
        LOG.warn(
                "extension={} event=cleanup message=\"Deleting extension jobs; manual ThingWorx entity cleanup required\"",
                extKey);
        emitExtensionEvent(
                extension,
                "ExtensionDeleted",
                "ThingWorxExtension " + namespace + "/" + name + " deleted; imported ThingWorx entities remain and require manual database cleanup.",
                "Warning");
        client.batch()
                .v1()
                .jobs()
                .inNamespace(namespace)
                .withLabel("thingworx.us-ignite.org/extension", name)
                .delete();
        System.err.println(
                "WARNING: ThingWorxExtension "
                        + namespace
                        + "/"
                        + name
                        + " was deleted. Imported ThingWorx entities remain and require manual database cleanup.");
        return DeleteControl.defaultDelete();
    }

    private boolean isNextForCluster(ThingWorxExtension extension, List<ThingWorxExtension> all) {
        return dependencyAnalyzer.isNextForCluster(extension, all);
    }

    private boolean dependenciesReady(ThingWorxExtension extension, List<ThingWorxExtension> all) {
        return dependencyAnalyzer.dependenciesReady(extension, all);
    }

    private boolean dependenciesReady(ThingWorxExtension extension, String namespace) {
        return dependenciesReady(
                extension,
                new ArrayList<>(
                        client.resources(ThingWorxExtension.class)
                                .inNamespace(namespace)
                                .list()
                                .getItems()));
    }

    private ExtensionDependencyAnalyzer.DependencyAnalysis analyzeDependencies(
            ThingWorxExtension extension, List<ThingWorxExtension> all) {
        return dependencyAnalyzer.analyze(extension, all);
    }

    private List<String> dependencyNames(ThingWorxExtension extension) {
        return extension.getSpec() == null || extension.getSpec().getDependsOn() == null
                ? List.of()
                : extension.getSpec().getDependsOn();
    }

    private Job installerJob(
            ThingWorxExtension extension,
            ThingWorxCluster cluster,
            String jobName,
            String desiredFingerprint) {
        var spec = extension.getSpec();
        var appKey = cluster.getSpec().getExtensionImportAppKey();
        var environment =
                new ArrayList<>(List.of(
                        value("INSTALL_MODE", "apply-or-update"),
                        value("INSTALL_FINGERPRINT", desiredFingerprint),
                        value("OCI_ARTIFACT", spec.getArtifact().getRepository()),
                        value("OCI_DIGEST", spec.getArtifact().getDigest()),
                        value("EXPECTED_EXTENSION_NAME", spec.getArtifact().getExpectedName()),
                        value(
                                "EXPECTED_EXTENSION_VERSION",
                                spec.getArtifact().getExpectedVersion()),
                        value(
                                "THINGWORX_URL",
                                "http://"
                                        + ThingWorxResources.name(
                                                cluster,
                                                cluster.getSpec().isEnableHA()
                                                        ? "haproxy"
                                                        : "platform")
                                        + ":8080/Thingworx"),
                        value(
                                "ALLOW_ENTITIES",
                                String.valueOf(spec.getImportPolicy().isEntities())),
                        value(
                                "ALLOW_EXTENSIBLE_ENTITIES",
                                String.valueOf(spec.getImportPolicy().isExtensibleEntities())),
                        value(
                                "ALLOW_JAR_RESOURCES",
                                String.valueOf(spec.getImportPolicy().isJarResources())),
                        value(
                                "ALLOW_JAVASCRIPT_RESOURCES",
                                String.valueOf(spec.getImportPolicy().isJavascriptResources())),
                        value(
                                "ALLOW_CSS_RESOURCES",
                                String.valueOf(spec.getImportPolicy().isCssResources())),
                        value(
                                "ALLOW_JSON_RESOURCES",
                                String.valueOf(spec.getImportPolicy().isJsonResources())),
                        value(
                                "ALLOW_WEBAPP_RESOURCES",
                                String.valueOf(spec.getImportPolicy().isWebAppResources())),
                        secret("THINGWORX_EXTENSION_APP_KEY", appKey)));
        if (spec.getArtifactPullSecret() != null && !spec.getArtifactPullSecret().isBlank()) {
            environment.add(value("DOCKER_CONFIG", "/registry-auth"));
        }
        var metadata =
                new ObjectMetaBuilder()
                        .withName(jobName)
                        .withNamespace(extension.getMetadata().getNamespace())
                        .withLabels(
                                Map.of(
                                        "app.kubernetes.io/managed-by",
                                        "thingworx-operator",
                                        "thingworx.us-ignite.org/extension",
                                        extension.getMetadata().getName()));
        var owner = extension.getMetadata();
        if (owner.getUid() != null && !owner.getUid().isBlank()) {
            metadata.withOwnerReferences(
                    new io.fabric8.kubernetes.api.model.OwnerReferenceBuilder()
                            .withApiVersion("thingworx.us-ignite.org/v1alpha1")
                            .withKind("ThingWorxExtension")
                            .withName(owner.getName())
                            .withUid(owner.getUid())
                            .withController(true)
                            .withBlockOwnerDeletion(true)
                            .build());
        }
        var builder =
                new JobBuilder()
                        .withMetadata(metadata.build())
                        .withNewSpec()
                        .withTtlSecondsAfterFinished(ThingWorxResources.jobTtlSeconds(cluster))
                        .withBackoffLimit(3)
                        .withNewTemplate()
                        .withNewSpec()
                        .withRestartPolicy("OnFailure")
                        .addNewContainer()
                        .withName("installer")
                        .withImage(cluster.getSpec().getImages().getExtensionInstaller())
                        .withImagePullPolicy("IfNotPresent")
                        .withEnv(environment)
                        .endContainer()
                        .endSpec()
                        .endTemplate()
                        .endSpec();
        if (cluster.getSpec().getImagePullSecret() != null) {
            builder.editSpec()
                    .editTemplate()
                    .editSpec()
                    .addNewImagePullSecret()
                    .withName(cluster.getSpec().getImagePullSecret())
                    .endImagePullSecret()
                    .endSpec()
                    .endTemplate()
                    .endSpec();
        }
        if (spec.getArtifactPullSecret() != null && !spec.getArtifactPullSecret().isBlank()) {
            builder.editSpec()
                    .editTemplate()
                    .editSpec()
                    .addNewImagePullSecret()
                    .withName(spec.getArtifactPullSecret())
                    .endImagePullSecret()
                    .addNewVolume()
                    .withName("artifact-registry-auth")
                    .withNewSecret()
                    .withSecretName(spec.getArtifactPullSecret())
                    .addNewItem()
                    .withKey(".dockerconfigjson")
                    .withPath("config.json")
                    .endItem()
                    .endSecret()
                    .endVolume()
                    .editFirstContainer()
                    .addNewVolumeMount()
                    .withName("artifact-registry-auth")
                    .withMountPath("/registry-auth")
                    .withReadOnly(true)
                    .endVolumeMount()
                    .endContainer()
                    .endSpec()
                    .endTemplate()
                    .endSpec();
        }
        return builder.build();
    }

    private void restartPlatform(
            ThingWorxCluster cluster, ThingWorxExtension extension, String desiredFingerprint) {
        var statefulSet =
                client.apps()
                        .statefulSets()
                        .inNamespace(cluster.getMetadata().getNamespace())
                        .withName(ThingWorxResources.name(cluster, "platform"))
                        .get();
        if (statefulSet == null) return;
        var annotations = statefulSet.getSpec().getTemplate().getMetadata().getAnnotations();
        if (annotations == null) annotations = new java.util.HashMap<>();
        annotations.put(RESTART_ANNOTATION, desiredFingerprint);
        statefulSet.getSpec().getTemplate().getMetadata().setAnnotations(annotations);
        client.resource(statefulSet).fieldManager("thingworx-operator").serverSideApply();
    }

    private boolean platformReady(ThingWorxCluster cluster, ThingWorxExtension extension) {
        var statefulSet =
                client.apps()
                        .statefulSets()
                        .inNamespace(cluster.getMetadata().getNamespace())
                        .withName(ThingWorxResources.name(cluster, "platform"))
                        .get();
        var requestedRestart =
                extension.getStatus() == null ? null : extension.getStatus().getRequestedRestart();
        return PlatformReadinessChecker.isReady(statefulSet, requestedRestart);
    }

    private UpdateControl<ThingWorxExtension> ready(
            ThingWorxExtension extension, String desiredFingerprint) {
        var status =
                statusObject(extension, "Ready", "Installed", "Extension imported successfully.");
        if (extension.getStatus() != null
                && extension.getStatus().getObservedDigest() != null
                && !Objects.equals(
                        extension.getStatus().getObservedDigest(),
                        extension.getSpec().getArtifact().getDigest())) {
            status.setPreviousDigest(extension.getStatus().getObservedDigest());
        }
        status.setObservedDigest(extension.getSpec().getArtifact().getDigest());
        status.setObservedFingerprint(desiredFingerprint);
        status.setImportedName(extension.getSpec().getArtifact().getExpectedName());
        status.setImportedVersion(extension.getSpec().getArtifact().getExpectedVersion());
        extension.setStatus(status);
        String extKey = extensionKey(extension);
        OperatorMetrics.setPhase(extKey, "Ready");
        LOG.info("extension={} phase=Ready fingerprint={} event=extensionReady", extKey, desiredFingerprint);
        emitExtensionEvent(extension, "Installed", "Extension imported successfully: " + desiredFingerprint, "Normal");
        return UpdateControl.patchStatus(extension);
    }

    private UpdateControl<ThingWorxExtension> status(
            ThingWorxExtension extension,
            String phase,
            String reason,
            String message,
            boolean immediate) {
        var prevPhase = extension.getStatus() == null ? null : extension.getStatus().getPhase();
        var prevMessage = extension.getStatus() == null ? null : extension.getStatus().getMessage();
        var prevReason =
                extension.getStatus() == null || extension.getStatus().getConditions() == null
                        ? null
                        : extension.getStatus().getConditions().stream()
                                .filter(item -> "Error".equals(item.getType()))
                                .map(Condition::getReason)
                                .findFirst()
                                .orElse(null);
        extension.setStatus(statusObject(extension, phase, reason, message));
        String extKey = extensionKey(extension);
        OperatorMetrics.setPhase(extKey, phase);
        boolean changed =
                !Objects.equals(prevPhase, phase)
                        || !Objects.equals(prevReason, reason)
                        || !Objects.equals(prevMessage, message);
        if ("Failed".equals(phase)) {
            if (changed) OperatorMetrics.incrementJobFailures(extKey, reason);
            LOG.warn(
                    "extension={} phase={} prevPhase={} reason={} message=\"{}\" event=statusUpdate",
                    extKey, phase, prevPhase, reason, message);
            if (changed)
                emitExtensionEvent(extension, reason, phase + ": " + message, "Warning");
        } else {
            LOG.info(
                    "extension={} phase={} prevPhase={} reason={} message=\"{}\" event=statusUpdate",
                    extKey, phase, prevPhase, reason, message);
            String eventType = "Failed".equals(phase) ? "Warning" : "Normal";
            if (changed)
                emitExtensionEvent(extension, reason, phase + ": " + message, eventType);
        }
        var control = UpdateControl.patchStatus(extension);
        return immediate ? control : control.rescheduleAfter(Duration.ofSeconds(15));
    }

    private ThingWorxExtensionStatus statusObject(
            ThingWorxExtension extension, String phase, String reason, String message) {
        var status =
                extension.getStatus() == null
                        ? new ThingWorxExtensionStatus()
                        : extension.getStatus();
        status.setPhase(phase);
        status.setMessage(message);
        status.setObservedGeneration(extension.getMetadata().getGeneration());
        if (extension.getSpec() != null && extension.getSpec().getArtifact() != null) {
            status.setLastAttemptedDigest(extension.getSpec().getArtifact().getDigest());
            if (extension.getSpec().getArtifact().getDigest() != null
                    && extension.getSpec().getArtifact().getDigest().startsWith("sha256:")
                    && extension.getSpec().getImportPolicy() != null) {
                status.setInstallerJob(jobName(extension, fingerprint(extension)));
                status.setLastAttemptedFingerprint(fingerprint(extension));
            }
        }
        var failed = "Failed".equals(phase);
        var ready = "Ready".equals(phase);
        status.setConditions(
                List.of(
                        condition(
                                status.getConditions(),
                                "Ready",
                                ready ? "True" : "False",
                                ready ? "Installed" : reason,
                                message,
                                extension),
                        condition(
                                status.getConditions(),
                                "Progressing",
                                !ready && !failed ? "True" : "False",
                                reason,
                                message,
                                extension),
                        condition(
                                status.getConditions(),
                                "Error",
                                failed ? "True" : "False",
                                reason,
                                message,
                                extension)));
        return status;
    }

    private Condition condition(
            List<Condition> existing,
            String type,
            String value,
            String reason,
            String message,
            ThingWorxExtension extension) {
        var previous =
                existing == null
                        ? null
                        : existing.stream()
                                .filter(item -> type.equals(item.getType()))
                                .findFirst()
                                .orElse(null);
        var transition =
                previous != null
                                && value.equals(previous.getStatus())
                                && reason.equals(previous.getReason())
                                && message.equals(previous.getMessage())
                        ? previous.getLastTransitionTime()
                        : OffsetDateTime.now(ZoneOffset.UTC).toString();
        return new ConditionBuilder()
                .withType(type)
                .withStatus(value)
                .withReason(reason)
                .withMessage(message)
                .withLastTransitionTime(transition)
                .withObservedGeneration(extension.getMetadata().getGeneration())
                .build();
    }

    private String installerFailure(Job job) {
        if (job.getStatus() != null && job.getStatus().getConditions() != null) {
            return job.getStatus().getConditions().stream()
                    .filter(condition -> "Failed".equals(condition.getType()))
                    .map(condition -> condition.getReason() + ": " + condition.getMessage())
                    .findFirst()
                    .orElse(
                            "Installer Job failed; inspect Job logs and create a new digest after correction.");
        }
        return "Installer Job failed; inspect Job logs and create a new digest after correction.";
    }

    private String validate(ThingWorxExtension extension) {
        if (extension.getSpec() == null
                || extension.getSpec().getClusterRef() == null
                || blank(extension.getSpec().getClusterRef().getName()))
            return "spec.clusterRef.name is required.";
        if (extension.getSpec().getArtifact() == null
                || blank(extension.getSpec().getArtifact().getRepository())
                || blank(extension.getSpec().getArtifact().getDigest()))
            return "spec.artifact.repository and digest are required.";
        if (extension.getSpec().getImportPolicy() == null)
            return "spec.importPolicy must not be null.";
        if (!extension.getSpec().getArtifact().getDigest().startsWith("sha256:"))
            return "spec.artifact.digest must be sha256-pinned.";
        var dependencies = dependencyNames(extension);
        if (dependencies.contains(extension.getMetadata().getName()))
            return "An extension cannot depend on itself.";
        if (new HashSet<>(dependencies).size() != dependencies.size())
            return "spec.dependsOn must not contain duplicate extension names.";
        return null;
    }

    private boolean complete(Job job) {
        return job.getStatus() != null
                && job.getStatus().getSucceeded() != null
                && job.getStatus().getSucceeded() > 0;
    }

    private boolean failed(Job job) {
        return job.getStatus() != null
                && job.getStatus().getFailed() != null
                && job.getStatus().getFailed() > 0;
    }

    private String jobName(ThingWorxExtension extension, String fingerprint) {
        return extension.getMetadata().getName() + "-" + fingerprint.substring(0, 12);
    }

    private String fingerprint(ThingWorxExtension extension) {
        return FingerprintComputer.fingerprint(extension);
    }

    private String missingSecret(String name, String namespace) {
        if (blank(name) || blank(namespace)) return null;
        try {
            var secret = client.secrets().inNamespace(namespace).withName(name).get();
            if (secret == null) return "Secret '" + name + "' not found in namespace '" + namespace + "'";
        } catch (Exception exception) {
            return "Secret '" + name + "' not found in namespace '" + namespace + "'";
        }
        return null;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private io.fabric8.kubernetes.api.model.EnvVar value(String name, String value) {
        return new EnvVarBuilder().withName(name).withValue(value == null ? "" : value).build();
    }

    private io.fabric8.kubernetes.api.model.EnvVar secret(String name, SecretKeyReference ref) {
        return new EnvVarBuilder()
                .withName(name)
                .withNewValueFrom()
                .withNewSecretKeyRef(ref.getKey(), ref.getName(), false)
                .endValueFrom()
                .build();
    }

    private String extensionKey(ThingWorxExtension extension) {
        if (extension == null || extension.getMetadata() == null) return "unknown/unknown";
        String ns = extension.getMetadata().getNamespace();
        String name = extension.getMetadata().getName();
        if (ns == null || ns.isBlank()) ns = "default";
        if (name == null || name.isBlank()) name = "unknown";
        return ns + "/" + name;
    }

    private void emitExtensionEvent(ThingWorxExtension extension, String reason, String message, String type) {
        try {
            String namespace =
                    extension.getMetadata() == null || blank(extension.getMetadata().getNamespace())
                            ? "default"
                            : extension.getMetadata().getNamespace();
            String name = extension.getMetadata() == null ? "unknown" : extension.getMetadata().getName();
            String uid = extension.getMetadata() == null ? "" : extension.getMetadata().getUid();
            var now = Instant.now().toString();
            var event =
                    new EventBuilder()
                            .withNewMetadata()
                            .withGenerateName(name + "-")
                            .withNamespace(namespace)
                            .endMetadata()
                            .withReason(reason)
                            .withMessage(message)
                            .withType(type)
                            .withInvolvedObject(
                                    new ObjectReferenceBuilder()
                                            .withKind("ThingWorxExtension")
                                            .withName(name)
                                            .withNamespace(namespace)
                                            .withUid(uid == null ? "" : uid)
                                            .withApiVersion("thingworx.us-ignite.org/v1alpha1")
                                            .build())
                            .withSource(
                                    new EventSourceBuilder().withComponent("thingworx-operator").build())
                            .withFirstTimestamp(now)
                            .withLastTimestamp(now)
                            .withCount(1)
                            .build();
            try {
                try {
                    client.v1().events().inNamespace(namespace).resource(event).create();
                } catch (Exception e1) {
                    client.resource(event).inNamespace(namespace).create();
                }
            } catch (Exception nested) {
                LOG.debug("Failed to create extension Event: {}", nested.getMessage());
            }
        } catch (Exception exception) {
            LOG.debug("emitExtensionEvent failed: {}", exception.getMessage());
        }
    }
}
