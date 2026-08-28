package org.us_ignite.thingworx.operator.reconcile;

import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.us_ignite.thingworx.operator.api.SecretKeyReference;
import org.us_ignite.thingworx.operator.api.ThingWorxCluster;
import org.us_ignite.thingworx.operator.api.ThingWorxClusterStatus;

/** Reconciles the documented ThingWorx HA startup and update order. */
@ControllerConfiguration
public class ThingWorxClusterReconciler implements Reconciler<ThingWorxCluster> {
    private final KubernetesClient client;

    public ThingWorxClusterReconciler() {
        this(new KubernetesClientBuilder().build());
    }

    public ThingWorxClusterReconciler(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public UpdateControl<ThingWorxCluster> reconcile(
            ThingWorxCluster resource, Context<ThingWorxCluster> context) {
        var validation = validate(resource);
        if (validation != null) return status(resource, LifecyclePhase.INVALID, validation, false);
        var phase =
                LifecyclePhase.from(
                        resource.getStatus() == null ? null : resource.getStatus().getPhase());
        return switch (phase) {
            case PRECHECK ->
                    applyAndAdvance(
                            resource,
                            ThingWorxResources.precheck(resource),
                            LifecyclePhase.DATABASE,
                            "Prerequisites accepted; initializing the PostgreSQL schema.");
            case DATABASE ->
                    waitForJobs(
                            resource,
                            ThingWorxResources.database(resource),
                            "database-init",
                            "security-init",
                            resource.getSpec().isEnableHA()
                                    ? LifecyclePhase.COORDINATION
                                    : LifecyclePhase.PLATFORM_PRIMARY,
                            "Database and security initialization complete.");
            case COORDINATION ->
                    waitForStatefulSets(
                            resource,
                            ThingWorxResources.coordination(resource),
                            List.of("zookeeper", "ignite"),
                            LifecyclePhase.PLATFORM_PRIMARY,
                            "Coordination services are ready.");
            case PLATFORM_PRIMARY ->
                    waitForStatefulSets(
                            resource,
                            ThingWorxResources.platform(resource, 1),
                            List.of("platform"),
                            resource.getSpec().isEnableHA()
                                    ? LifecyclePhase.PLATFORM_CLUSTER
                                    : LifecyclePhase.EDGE,
                            resource.getSpec().isEnableHA()
                                    ? "First platform node is ready; expanding the platform cluster."
                                    : "The single-node ThingWorx platform is ready.");
            case PLATFORM_CLUSTER ->
                    waitForStatefulSets(
                            resource,
                            ThingWorxResources.platform(resource, 3),
                            List.of("platform"),
                            LifecyclePhase.CONNECTION_SERVERS,
                            "ThingWorx platform cluster is ready.");
            case CONNECTION_SERVERS ->
                    waitForDeployments(
                            resource,
                            ThingWorxResources.connectionServers(resource),
                            List.of("cxserver"),
                            LifecyclePhase.EDGE,
                            "Connection servers are ready.");
            case EDGE ->
                    resource.getSpec().isEnableHA()
                            ? waitForDeployments(
                                    resource,
                                    ThingWorxResources.edge(resource),
                                    List.of("haproxy"),
                                    LifecyclePhase.OPTIONAL_SERVICES,
                                    "Ingress edge is ready.")
                            : applyAndAdvance(
                                    resource,
                                    ThingWorxResources.directEdge(resource),
                                    LifecyclePhase.OPTIONAL_SERVICES,
                                    "Direct platform ingress is reconciled.");
            case OPTIONAL_SERVICES -> waitForOptionalServices(resource);
            case READY -> ready(resource);
            case DEGRADED -> recover(resource);
            case INVALID ->
                    applyAndAdvance(
                            resource,
                            ThingWorxResources.precheck(resource),
                            LifecyclePhase.DATABASE,
                            "Specification is valid again; restarting the ordered reconciliation workflow.");
        };
    }

    /** Restart the ordered rollout whenever a READY resource receives a new spec generation. */
    private UpdateControl<ThingWorxCluster> ready(ThingWorxCluster cluster) {
        if (desiredGenerationChanged(cluster)) {
            return applyAndAdvance(
                    cluster,
                    ThingWorxResources.precheck(cluster),
                    LifecyclePhase.DATABASE,
                    "Desired state changed; restarting the ordered reconciliation workflow.");
        }
        if (!runtimeReady(cluster)) {
            return status(
                    cluster,
                    LifecyclePhase.DEGRADED,
                    "A managed workload is no longer ready; restarting health-gated reconciliation.",
                    false);
        }
        prune(cluster);
        return status(
                cluster,
                LifecyclePhase.READY,
                "All desired vendor lifecycle components are reconciled.",
                false);
    }

    private UpdateControl<ThingWorxCluster> recover(ThingWorxCluster cluster) {
        if (runtimeReady(cluster)) {
            return status(
                    cluster,
                    LifecyclePhase.READY,
                    "All managed workloads recovered and are ready.",
                    true);
        }
        return status(
                cluster,
                LifecyclePhase.DEGRADED,
                "Waiting for managed workloads to recover.",
                false);
    }

    private boolean runtimeReady(ThingWorxCluster cluster) {
        if (!statefulSetReady(cluster, "platform")) return false;
        if (cluster.getSpec().getDatabase().isInternal() && !statefulSetReady(cluster, "postgres"))
            return false;
        if (!cluster.getSpec().isEnableHA()) return true;
        return statefulSetReady(cluster, "zookeeper")
                && statefulSetReady(cluster, "ignite")
                && deploymentReady(cluster, "cxserver")
                && deploymentReady(cluster, "haproxy");
    }

    private boolean desiredGenerationChanged(ThingWorxCluster cluster) {
        var status = cluster.getStatus();
        return status == null
                || !Objects.equals(
                        status.getObservedGeneration(), cluster.getMetadata().getGeneration());
    }

    private UpdateControl<ThingWorxCluster> waitForJobs(
            ThingWorxCluster cluster,
            List<HasMetadata> resources,
            String first,
            String second,
            LifecyclePhase next,
            String message) {
        apply(resources);
        if (cluster.getSpec().getDatabase().isInternal() && !statefulSetReady(cluster, "postgres"))
            return status(
                    cluster,
                    LifecyclePhase.DATABASE,
                    "Waiting for the internal PostgreSQL StatefulSet.",
                    false);
        if (jobComplete(cluster, first) && jobComplete(cluster, second))
            return status(cluster, next, message, true);
        if (jobFailed(cluster, first) || jobFailed(cluster, second))
            return status(
                    cluster,
                    LifecyclePhase.INVALID,
                    "A required initialization Job failed; inspect its logs before changing the spec.",
                    false);
        return status(
                cluster, LifecyclePhase.DATABASE, "Waiting for database and security Jobs.", false);
    }

    private UpdateControl<ThingWorxCluster> waitForStatefulSets(
            ThingWorxCluster cluster,
            List<HasMetadata> resources,
            List<String> names,
            LifecyclePhase next,
            String message) {
        apply(resources);
        boolean ready = names.stream().allMatch(name -> statefulSetReady(cluster, name));
        return ready
                ? status(cluster, next, message, true)
                : status(
                        cluster,
                        LifecyclePhase.from(
                                cluster.getStatus() == null
                                        ? null
                                        : cluster.getStatus().getPhase()),
                        "Waiting for StatefulSet readiness.",
                        false);
    }

    private UpdateControl<ThingWorxCluster> waitForDeployments(
            ThingWorxCluster cluster,
            List<HasMetadata> resources,
            List<String> names,
            LifecyclePhase next,
            String message) {
        apply(resources);
        boolean ready = names.stream().allMatch(name -> deploymentReady(cluster, name));
        return ready
                ? status(cluster, next, message, true)
                : status(
                        cluster,
                        LifecyclePhase.from(
                                cluster.getStatus() == null
                                        ? null
                                        : cluster.getStatus().getPhase()),
                        "Waiting for Deployment readiness.",
                        false);
    }

    private UpdateControl<ThingWorxCluster> waitForOptionalServices(ThingWorxCluster cluster) {
        apply(ThingWorxResources.optional(cluster));

        var waitingFor = new java.util.ArrayList<String>();
        if (cluster.getSpec().isKafkaEnabled() && !statefulSetReady(cluster, "kafka"))
            waitingFor.add("Kafka");
        if (cluster.getSpec().isOtelEnabled() && !deploymentReady(cluster, "otel"))
            waitingFor.add("OpenTelemetry");

        if (waitingFor.isEmpty())
            return status(cluster, LifecyclePhase.READY, "Optional services are ready.", true);
        return status(
                cluster,
                LifecyclePhase.OPTIONAL_SERVICES,
                "Waiting for optional service readiness: " + String.join(", ", waitingFor) + ".",
                false);
    }

    private UpdateControl<ThingWorxCluster> applyAndAdvance(
            ThingWorxCluster cluster,
            List<HasMetadata> resources,
            LifecyclePhase next,
            String message) {
        apply(resources);
        return status(cluster, next, message, true);
    }

    private void apply(List<HasMetadata> resources) {
        resources.forEach(
                resource -> {
                    attachOwner(resource);
                    var operation = client.resource(resource);
                    var existing = operation.get();
                    if (resource instanceof Job) {
                        if (existing == null) operation.create();
                    } else if (resource instanceof PersistentVolumeClaim && existing != null) {
                        validatePersistentVolumeClaimUpdate(
                                (PersistentVolumeClaim) existing, (PersistentVolumeClaim) resource);
                    } else {
                        if (existing == null) {
                            operation.create();
                        } else {
                            // Preserve controller-populated status when the mock-compatible
                            // replace path is used. The real Kubernetes API stores status
                            // separately, but replacing a typed object without carrying it
                            // would make readiness disappear during reconciliation.
                            if (resource instanceof StatefulSet
                                    && existing instanceof StatefulSet) {
                                ((StatefulSet) resource)
                                        .setStatus(((StatefulSet) existing).getStatus());
                            } else if (resource instanceof Deployment
                                    && existing instanceof Deployment) {
                                ((Deployment) resource)
                                        .setStatus(((Deployment) existing).getStatus());
                            }
                            try {
                                operation.fieldManager("thingworx-operator").serverSideApply();
                            } catch (
                                    io.fabric8.kubernetes.client.KubernetesClientException
                                            exception) {
                                if (exception.getCode() == 415 || exception.getCode() == 404) {
                                    operation.update();
                                } else {
                                    throw exception;
                                }
                            }
                        }
                    }
                });
    }

    private void attachOwner(HasMetadata resource) {
        var clusterName = resource.getMetadata().getLabels().get("app.kubernetes.io/instance");
        var cluster =
                client.resources(ThingWorxCluster.class)
                        .inNamespace(resource.getMetadata().getNamespace())
                        .withName(clusterName)
                        .get();
        if (cluster == null || blank(cluster.getMetadata().getUid())) return;
        resource.getMetadata()
                .setOwnerReferences(
                        List.of(
                                new OwnerReferenceBuilder()
                                        .withApiVersion("thingworx.us-ignite.org/v1alpha1")
                                        .withKind("ThingWorxCluster")
                                        .withName(cluster.getMetadata().getName())
                                        .withUid(cluster.getMetadata().getUid())
                                        .withController(true)
                                        .withBlockOwnerDeletion(true)
                                        .build()));
    }

    private void validatePersistentVolumeClaimUpdate(
            PersistentVolumeClaim existing, PersistentVolumeClaim desired) {
        if (!Objects.equals(
                existing.getSpec().getStorageClassName(),
                desired.getSpec().getStorageClassName())) {
            throw new IllegalArgumentException(
                    "Changing the StorageClass of an existing ThingWorx PVC is not supported.");
        }
        // Kubernetes validates whether the StorageClass permits the requested expansion. Applying
        // the size is intentionally left to a future reconcile after that capability is confirmed.
    }

    private boolean jobComplete(ThingWorxCluster cluster, String component) {
        return jobCompleteName(
                cluster, ThingWorxResources.initializationJobName(cluster, component));
    }

    private boolean jobCompleteName(ThingWorxCluster cluster, String name) {
        Job job =
                client.batch()
                        .v1()
                        .jobs()
                        .inNamespace(cluster.getMetadata().getNamespace())
                        .withName(name)
                        .get();
        return hasJobCondition(job, "Complete");
    }

    private boolean jobFailed(ThingWorxCluster cluster, String component) {
        return jobFailedName(cluster, ThingWorxResources.initializationJobName(cluster, component));
    }

    private boolean jobFailedName(ThingWorxCluster cluster, String name) {
        Job job =
                client.batch()
                        .v1()
                        .jobs()
                        .inNamespace(cluster.getMetadata().getNamespace())
                        .withName(name)
                        .get();
        return hasJobCondition(job, "Failed");
    }

    private boolean hasJobCondition(Job job, String type) {
        if (job == null || job.getStatus() == null) return false;
        if (job.getStatus().getConditions() != null
                && job.getStatus().getConditions().stream()
                        .anyMatch(
                                condition ->
                                        type.equals(condition.getType())
                                                && "True".equals(condition.getStatus()))) {
            return true;
        }
        if ("Complete".equals(type)
                && job.getStatus().getSucceeded() != null
                && job.getStatus().getSucceeded() > 0) {
            return true;
        }
        if ("Failed".equals(type)
                && job.getStatus().getFailed() != null
                && job.getStatus().getFailed() > 0) {
            return true;
        }
        return false;
    }

    private boolean statefulSetReady(ThingWorxCluster cluster, String component) {
        StatefulSet statefulSet =
                client.apps()
                        .statefulSets()
                        .inNamespace(cluster.getMetadata().getNamespace())
                        .withName(ThingWorxResources.name(cluster, component))
                        .get();
        return statefulSet != null
                && statefulSet.getStatus() != null
                && statefulSet.getSpec() != null
                && statefulSet.getStatus().getReadyReplicas() != null
                && statefulSet
                        .getStatus()
                        .getReadyReplicas()
                        .equals(statefulSet.getSpec().getReplicas());
    }

    private boolean deploymentReady(ThingWorxCluster cluster, String component) {
        Deployment deployment =
                client.apps()
                        .deployments()
                        .inNamespace(cluster.getMetadata().getNamespace())
                        .withName(ThingWorxResources.name(cluster, component))
                        .get();
        return deployment != null
                && deployment.getStatus() != null
                && deployment.getSpec() != null
                && deployment.getStatus().getAvailableReplicas() != null
                && deployment
                        .getStatus()
                        .getAvailableReplicas()
                        .equals(deployment.getSpec().getReplicas());
    }

    private UpdateControl<ThingWorxCluster> status(
            ThingWorxCluster cluster, LifecyclePhase phase, String message, boolean advance) {
        var status =
                cluster.getStatus() == null ? new ThingWorxClusterStatus() : cluster.getStatus();
        status.setPhase(phase.name());
        status.setMessage(message);
        // A generation is observed only after the complete desired state is READY. Keeping the old
        // value during rollout lets clients distinguish an in-flight update from a converged one.
        if (phase == LifecyclePhase.READY)
            status.setObservedGeneration(cluster.getMetadata().getGeneration());
        status.setCurrentVersion(cluster.getSpec().getImages().getPlatform());
        status.setMode(cluster.getSpec().isEnableHA() ? "HA" : "SINGLE_NODE");
        var ready = phase == LifecyclePhase.READY;
        var degraded = phase == LifecyclePhase.DEGRADED;
        var error = phase == LifecyclePhase.INVALID;
        status.setConditions(
                List.of(
                        condition(cluster, status.getConditions(), "Ready", ready, phase, message),
                        condition(
                                cluster,
                                status.getConditions(),
                                "Progressing",
                                !ready && !degraded && !error,
                                phase,
                                message),
                        condition(
                                cluster,
                                status.getConditions(),
                                "Degraded",
                                degraded,
                                phase,
                                message),
                        condition(
                                cluster, status.getConditions(), "Error", error, phase, message)));
        cluster.setStatus(status);
        var uc = UpdateControl.patchStatus(cluster);
        if (advance && phase != LifecyclePhase.READY && phase != LifecyclePhase.INVALID) {
            uc = uc.rescheduleAfter(Duration.ofSeconds(10));
        } else if (!advance) {
            uc = uc.rescheduleAfter(Duration.ofSeconds(15));
        }
        return uc;
    }

    private Condition condition(
            ThingWorxCluster cluster,
            List<Condition> existing,
            String type,
            boolean value,
            LifecyclePhase phase,
            String message) {
        var state = value ? "True" : "False";
        var previous =
                existing == null
                        ? null
                        : existing.stream()
                                .filter(condition -> type.equals(condition.getType()))
                                .findFirst()
                                .orElse(null);
        var transition =
                previous != null
                                && state.equals(previous.getStatus())
                                && phase.name().equals(previous.getReason())
                                && message.equals(previous.getMessage())
                        ? previous.getLastTransitionTime()
                        : OffsetDateTime.now(ZoneOffset.UTC).toString();
        return new ConditionBuilder()
                .withType(type)
                .withStatus(state)
                .withReason(phase.name())
                .withMessage(message)
                .withObservedGeneration(cluster.getMetadata().getGeneration())
                .withLastTransitionTime(transition)
                .build();
    }

    private String validate(ThingWorxCluster cluster) {
        if (cluster.getSpec() == null
                || cluster.getSpec().getImages() == null
                || blank(cluster.getSpec().getImages().getPlatform()))
            return "spec.images.platform is required.";
        if (blank(cluster.getSpec().getImages().getDatabaseInit())
                || blank(cluster.getSpec().getImages().getSecurityCli()))
            return "Platform, database-init, and security-cli images are required.";
        if (cluster.getSpec().isEnableHA()
                && (blank(cluster.getSpec().getImages().getIgnite())
                        || blank(cluster.getSpec().getImages().getCxServer())
                        || blank(cluster.getSpec().getImages().getZookeeper())
                        || blank(cluster.getSpec().getImages().getHaProxy())))
            return "HA requires ignite, cxServer, zookeeper, and haProxy images.";
        if (invalidSecretReference(cluster.getSpec().getPlatformAdminPassword()))
            return "spec.platformAdminPassword must reference a Secret name and key.";
        if (invalidSecretReference(cluster.getSpec().getKeystorePassword()))
            return "spec.keystorePassword must reference a Secret name and key.";
        if (cluster.getSpec().getDatabase() == null
                || (!cluster.getSpec().getDatabase().isInternal()
                        && blank(cluster.getSpec().getDatabase().getHost()))
                || blank(cluster.getSpec().getDatabase().getDatabase())
                || blank(cluster.getSpec().getDatabase().getSchema())
                || blank(cluster.getSpec().getDatabase().getUsername())
                || blank(cluster.getSpec().getDatabase().getAdminUsername()))
            return "Database, schema, username, and adminUsername are required; an external database also requires host.";
        if (cluster.getSpec().isEnableHA() && cluster.getSpec().getDatabase().isInternal())
            return "enableHA requires database.internal=false and an external HA PostgreSQL service.";
        if (cluster.getSpec().isEnableHA()
                && invalidSecretReference(cluster.getSpec().getCxServerAppKey()))
            return "enableHA requires spec.cxServerAppKey.";
        if (cluster.getSpec().getDatabase().isInternal()
                && (blank(cluster.getSpec().getDatabase().getImage())
                        || invalidSecretReference(
                                cluster.getSpec().getDatabase().getAdminCredentials())))
            return "Internal PostgreSQL requires an image and database.adminCredentials.";
        if (cluster.getSpec().getStorage() == null
                || blank(cluster.getSpec().getStorage().getStorageClassName()))
            return "spec.storage.storageClassName must provide RWX storage.";
        if (cluster.getSpec().isKafkaEnabled() && blank(cluster.getSpec().getImages().getKafka()))
            return "spec.images.kafka is required when Kafka is enabled.";
        if (cluster.getSpec().isOtelEnabled()
                && blank(cluster.getSpec().getImages().getOtelCollector()))
            return "spec.images.otelCollector is required when OpenTelemetry is enabled.";
        return null;
    }

    private void prune(ThingWorxCluster cluster) {
        var namespace = cluster.getMetadata().getNamespace();
        var instance = cluster.getMetadata().getName();
        var desired = new java.util.HashSet<String>();
        var resources = new ArrayList<HasMetadata>();
        resources.addAll(ThingWorxResources.precheck(cluster));
        resources.addAll(ThingWorxResources.database(cluster));
        resources.addAll(
                ThingWorxResources.platform(cluster, cluster.getSpec().isEnableHA() ? 3 : 1));
        resources.addAll(
                cluster.getSpec().isEnableHA()
                        ? ThingWorxResources.coordination(cluster)
                        : List.of());
        resources.addAll(
                cluster.getSpec().isEnableHA()
                        ? ThingWorxResources.connectionServers(cluster)
                        : List.of());
        resources.addAll(
                cluster.getSpec().isEnableHA()
                        ? ThingWorxResources.edge(cluster)
                        : ThingWorxResources.directEdge(cluster));
        resources.addAll(ThingWorxResources.optional(cluster));
        resources.forEach(
                resource ->
                        desired.add(resource.getKind() + "/" + resource.getMetadata().getName()));
        client
                .genericKubernetesResources("v1", "ConfigMap")
                .inNamespace(namespace)
                .withLabel("app.kubernetes.io/instance", instance)
                .list()
                .getItems()
                .stream()
                .filter(item -> !desired.contains("ConfigMap/" + item.getMetadata().getName()))
                .forEach(item -> client.resource(item).delete());
        // Workload and service pruning is handled by owner references on deletion and immutable
        // mode fields prevent destructive topology changes in-place.
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean invalidSecretReference(SecretKeyReference reference) {
        return reference == null || blank(reference.getName()) || blank(reference.getKey());
    }
}
