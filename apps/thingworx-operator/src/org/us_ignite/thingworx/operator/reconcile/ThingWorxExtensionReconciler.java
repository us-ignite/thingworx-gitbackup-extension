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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.us_ignite.thingworx.operator.api.SecretKeyReference;
import org.us_ignite.thingworx.operator.api.ThingWorxCluster;
import org.us_ignite.thingworx.operator.api.ThingWorxExtension;
import org.us_ignite.thingworx.operator.api.ThingWorxExtensionStatus;

/** Imports one digest-pinned OCI extension at a time for each target cluster. */
@ControllerConfiguration(finalizerName = "thingworx.us-ignite.org/extension-finalizer")
public class ThingWorxExtensionReconciler
        implements Reconciler<ThingWorxExtension>, Cleaner<ThingWorxExtension> {
    private static final String RESTART_ANNOTATION = "thingworx.us-ignite.org/extension-restart";
    private final KubernetesClient client;

    public ThingWorxExtensionReconciler(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public UpdateControl<ThingWorxExtension> reconcile(
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

        var desiredDigest = extension.getSpec().getArtifact().getDigest();
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
        return all.stream()
                .filter(
                        candidate ->
                                candidate.getSpec() != null
                                        && candidate.getSpec().getClusterRef() != null
                                        && extension
                                                .getSpec()
                                                .getClusterRef()
                                                .getName()
                                                .equals(
                                                        candidate
                                                                .getSpec()
                                                                .getClusterRef()
                                                                .getName()))
                .filter(
                        candidate ->
                                candidate.getStatus() == null
                                        || !"Ready".equals(candidate.getStatus().getPhase())
                                        || !Objects.equals(
                                                fingerprint(candidate),
                                                candidate.getStatus().getObservedFingerprint()))
                .filter(candidate -> dependenciesReady(candidate, all))
                .min(Comparator.comparing(candidate -> candidate.getMetadata().getName()))
                .map(candidate -> candidate.getMetadata().getName())
                .map(extension.getMetadata().getName()::equals)
                .orElse(true);
    }

    private boolean dependenciesReady(ThingWorxExtension extension, List<ThingWorxExtension> all) {
        var byName = new HashMap<String, ThingWorxExtension>();
        all.forEach(candidate -> byName.put(candidate.getMetadata().getName(), candidate));
        return dependencyNames(extension).stream()
                .map(byName::get)
                .allMatch(
                        dependency ->
                                dependency != null
                                        && dependency.getStatus() != null
                                        && "Ready".equals(dependency.getStatus().getPhase())
                                        && dependency.getSpec() != null
                                        && dependency.getSpec().getArtifact() != null
                                        && Objects.equals(
                                                fingerprint(dependency),
                                                dependency.getStatus().getObservedFingerprint()));
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

    private DependencyAnalysis analyzeDependencies(
            ThingWorxExtension extension, List<ThingWorxExtension> all) {
        var byName = new HashMap<String, ThingWorxExtension>();
        all.forEach(candidate -> byName.put(candidate.getMetadata().getName(), candidate));
        for (String dependency : dependencyNames(extension)) {
            var resource = byName.get(dependency);
            if (resource == null) return new DependencyAnalysis(dependency, null, null);
            if (resource.getSpec() == null
                    || resource.getSpec().getClusterRef() == null
                    || !extension
                            .getSpec()
                            .getClusterRef()
                            .getName()
                            .equals(resource.getSpec().getClusterRef().getName())) {
                return new DependencyAnalysis(null, dependency, null);
            }
        }

        var graph = new HashMap<String, List<String>>();
        for (var candidate : all) {
            if (candidate.getSpec() != null
                    && candidate.getSpec().getClusterRef() != null
                    && extension
                            .getSpec()
                            .getClusterRef()
                            .getName()
                            .equals(candidate.getSpec().getClusterRef().getName())) {
                graph.put(candidate.getMetadata().getName(), dependencyNames(candidate));
            }
        }
        var cycle = findCycle(graph);
        return new DependencyAnalysis(
                null,
                null,
                cycle != null
                                && reachesCycle(
                                        extension.getMetadata().getName(),
                                        graph,
                                        new HashSet<>(cycle))
                        ? cycle
                        : null);
    }

    private boolean reachesCycle(String node, Map<String, List<String>> graph, Set<String> cycle) {
        return reachesCycle(node, graph, cycle, new HashSet<>());
    }

    private boolean reachesCycle(
            String node, Map<String, List<String>> graph, Set<String> cycle, Set<String> visited) {
        if (cycle.contains(node)) return true;
        if (!visited.add(node)) return false;
        for (String dependency : graph.getOrDefault(node, List.of())) {
            if (reachesCycle(dependency, graph, cycle, visited)) return true;
        }
        return false;
    }

    private List<String> findCycle(Map<String, List<String>> graph) {
        var states = new HashMap<String, Integer>();
        var path = new ArrayList<String>();
        for (String node : graph.keySet().stream().sorted().toList()) {
            var cycle = findCycle(node, graph, states, path);
            if (cycle != null) return cycle;
        }
        return null;
    }

    private List<String> findCycle(
            String node,
            Map<String, List<String>> graph,
            Map<String, Integer> states,
            List<String> path) {
        if (states.getOrDefault(node, 0) == 1) {
            var start = path.indexOf(node);
            var cycle = new ArrayList<>(path.subList(start, path.size()));
            cycle.add(node);
            return cycle;
        }
        if (states.getOrDefault(node, 0) == 2) return null;
        states.put(node, 1);
        path.add(node);
        for (String dependency : graph.getOrDefault(node, List.of()).stream().sorted().toList()) {
            if (!graph.containsKey(dependency)) continue;
            var cycle = findCycle(dependency, graph, states, path);
            if (cycle != null) return cycle;
        }
        path.removeLast();
        states.put(node, 2);
        return null;
    }

    private List<String> dependencyNames(ThingWorxExtension extension) {
        return extension.getSpec() == null || extension.getSpec().getDependsOn() == null
                ? List.of()
                : extension.getSpec().getDependsOn();
    }

    private record DependencyAnalysis(
            String missingDependency, String crossClusterDependency, List<String> cycle) {}

    private Job installerJob(
            ThingWorxExtension extension,
            ThingWorxCluster cluster,
            String jobName,
            String desiredFingerprint) {
        var spec = extension.getSpec();
        var appKey = cluster.getSpec().getExtensionImportAppKey();
        var environment =
                List.of(
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
                                        + ThingWorxResources.name(cluster, "haproxy")
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
                        secret("THINGWORX_EXTENSION_APP_KEY", appKey));
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
                        .withBackoffLimit(3)
                        .withNewTemplate()
                        .withNewSpec()
                        .withRestartPolicy("OnFailure")
                        .addNewContainer()
                        .withName("installer")
                        .withImage(cluster.getSpec().getImages().getExtensionInstaller())
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
        if (statefulSet == null
                || statefulSet.getSpec() == null
                || statefulSet.getStatus() == null
                || statefulSet.getSpec().getReplicas() == null) return false;
        var requestedRestart =
                extension.getStatus() == null ? null : extension.getStatus().getRequestedRestart();
        var annotations = statefulSet.getSpec().getTemplate().getMetadata().getAnnotations();
        if (requestedRestart == null
                || annotations == null
                || !requestedRestart.equals(annotations.get(RESTART_ANNOTATION))) return false;
        var desiredReplicas = statefulSet.getSpec().getReplicas();
        var status = statefulSet.getStatus();
        return desiredReplicas.equals(status.getReplicas())
                && desiredReplicas.equals(status.getUpdatedReplicas())
                && desiredReplicas.equals(status.getReadyReplicas())
                && desiredReplicas.equals(status.getAvailableReplicas())
                && status.getUpdateRevision() != null
                && status.getUpdateRevision().equals(status.getCurrentRevision());
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
        return UpdateControl.patchStatus(extension);
    }

    private UpdateControl<ThingWorxExtension> status(
            ThingWorxExtension extension,
            String phase,
            String reason,
            String message,
            boolean immediate) {
        extension.setStatus(statusObject(extension, phase, reason, message));
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
        var spec = extension.getSpec();
        var artifact = spec.getArtifact();
        var policy = spec.getImportPolicy();
        var input =
                String.join(
                        "\u0000",
                        artifact.getRepository(),
                        artifact.getDigest(),
                        String.valueOf(artifact.getExpectedName()),
                        String.valueOf(artifact.getExpectedVersion()),
                        String.valueOf(policy.isEntities()),
                        String.valueOf(policy.isExtensibleEntities()),
                        String.valueOf(policy.isJarResources()),
                        String.valueOf(policy.isJavascriptResources()),
                        String.valueOf(policy.isCssResources()),
                        String.valueOf(policy.isJsonResources()),
                        String.valueOf(policy.isWebAppResources()),
                        String.valueOf(spec.getArtifactPullSecret()));
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JRE", exception);
        }
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
}
