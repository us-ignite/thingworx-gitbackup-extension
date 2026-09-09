package org.us_ignite.thingworx.operator.reconcile;

import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.EventSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Quantity;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.us_ignite.thingworx.operator.api.SecretKeyReference;
import org.us_ignite.thingworx.operator.api.ThingWorxCluster;
import org.us_ignite.thingworx.operator.api.ThingWorxClusterStatus;
import org.us_ignite.thingworx.operator.metrics.OperatorMetrics;

/** Reconciles the documented ThingWorx HA startup and update order. */
@ControllerConfiguration
public class ThingWorxClusterReconciler implements Reconciler<ThingWorxCluster> {
    private static final Logger LOG = LoggerFactory.getLogger(ThingWorxClusterReconciler.class);
    private static final String MANAGED_BY_LABEL = "app.kubernetes.io/managed-by";
    private static final String MANAGED_BY_VALUE = "thingworx-operator";
    private static final String INSTANCE_LABEL = "app.kubernetes.io/instance";
    private static final String PRUNE_PVC_ANNOTATION = "thingworx.us-ignite.org/prune-pvc";
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
        long startNanos = System.nanoTime();
        String clusterKey = clusterKey(resource);
        LifecyclePhase entryPhase =
                LifecyclePhase.from(
                        resource.getStatus() == null ? null : resource.getStatus().getPhase());
        LOG.info(
                "cluster={} phase={} event=reconcileStart generation={} observedGeneration={}",
                clusterKey,
                entryPhase,
                resource.getMetadata() == null ? null : resource.getMetadata().getGeneration(),
                resource.getStatus() == null ? null : resource.getStatus().getObservedGeneration());
        UpdateControl<ThingWorxCluster> result;
        try {
            var validation = validate(resource);
            if (validation != null) {
                result = status(resource, LifecyclePhase.INVALID, validation, false);
            } else {
                result = doReconcileInternal(resource, context);
            }
        } catch (Exception e) {
            LOG.error(
                    "cluster={} phase={} event=reconcileError error={}",
                    clusterKey,
                    entryPhase,
                    e.toString(),
                    e);
            throw e;
        } finally {
            long durationNanos = System.nanoTime() - startNanos;
            OperatorMetrics.recordReconcile("ThingWorxCluster", clusterKey, durationNanos);
            LOG.info(
                    "cluster={} phase={} event=reconcileComplete durationMs={} generation={}",
                    clusterKey,
                    entryPhase,
                    durationNanos / 1_000_000,
                    resource.getMetadata() == null ? null : resource.getMetadata().getGeneration());
        }
        return result;
    }

    private UpdateControl<ThingWorxCluster> doReconcileInternal(
            ThingWorxCluster resource, Context<ThingWorxCluster> context) {
        // Ensure PVC storage expansion is reconciled on every invocation, not just when precheck
        // resources are applied. This allows size changes to be patched promptly even while the
        // cluster is in DATABASE or later phases.
        reconcilePvcStorage(resource);
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
            case INVALID -> recoverFromInvalid(resource);
        };
    }

    /** Restart the ordered rollout whenever a READY resource receives a new spec generation. */
    private UpdateControl<ThingWorxCluster> ready(ThingWorxCluster cluster) {
        if (desiredGenerationChanged(cluster)) {
            pruneSupersededInitializationJobs(cluster);
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

    /** Stay failed until changed Job inputs select a new immutable initialization Job. */
    private UpdateControl<ThingWorxCluster> recoverFromInvalid(ThingWorxCluster cluster) {
        if (jobFailed(cluster, "database-init") || jobFailed(cluster, "security-init")) {
            return status(
                    cluster,
                    LifecyclePhase.INVALID,
                    "A required initialization Job is still failed; change its inputs to create a new attempt.",
                    false);
        }
        return applyAndAdvance(
                cluster,
                ThingWorxResources.precheck(cluster),
                LifecyclePhase.DATABASE,
                "Specification is valid again; restarting the ordered reconciliation workflow.");
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
        if (jobFailed(cluster, first) || jobFailed(cluster, second)) {
            String failedJob = jobFailed(cluster, first) ? ThingWorxResources.initializationJobName(cluster, first) : ThingWorxResources.initializationJobName(cluster, second);
            String clusterKey = clusterKey(cluster);
            OperatorMetrics.incrementJobFailures(clusterKey, failedJob);
            LOG.warn(
                    "cluster={} phase={} event=jobFailed job={} message=\"{}\"",
                    clusterKey,
                    LifecyclePhase.DATABASE,
                    failedJob,
                    "A required initialization Job failed");
            emitClusterEvent(
                    cluster,
                    "InstallerJobFailed",
                    "Job " + failedJob + " failed; inspect its logs before changing the spec.",
                    "Warning");
            return status(
                    cluster,
                    LifecyclePhase.INVALID,
                    "A required initialization Job failed; inspect its logs before changing the spec.",
                    false);
        }
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
                    // Durable data must survive deletion of the custom resource. PVC cleanup is an
                    // explicit, annotation-gated operation in prune(), not owner-reference GC.
                    if (!(resource instanceof PersistentVolumeClaim)) attachOwner(resource);
                    var operation = client.resource(resource);
                    var existing = operation.get();
                    if (resource instanceof Job) {
                        if (existing == null) operation.create();
                    } else if (resource instanceof PersistentVolumeClaim && existing != null) {
                        validatePersistentVolumeClaimUpdate(
                                (PersistentVolumeClaim) existing, (PersistentVolumeClaim) resource);
                        reconcilePersistentVolumeClaimAnnotations(
                                (PersistentVolumeClaim) resource);
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

    /** Merge requested annotations without removing annotations owned by another controller. */
    private void reconcilePersistentVolumeClaimAnnotations(PersistentVolumeClaim desired) {
        var requested = desired.getMetadata().getAnnotations();
        if (requested == null || requested.isEmpty()) return;
        client.persistentVolumeClaims()
                .inNamespace(desired.getMetadata().getNamespace())
                .withName(desired.getMetadata().getName())
                .edit(
                        current -> {
                            var merged = new java.util.HashMap<String, String>();
                            if (current.getMetadata().getAnnotations() != null)
                                merged.putAll(current.getMetadata().getAnnotations());
                            merged.putAll(requested);
                            current.getMetadata().setAnnotations(merged);
                            return current;
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

    /**
     * Validates and reconciles a PVC update.
     *
     * <p>Storage expansion is supported: if {@code desired requests.storage} is larger than
     * {@code existing}, the PVC is patched to the larger size provided the StorageClass allows
     * expansion ({@code allowVolumeExpansion: true}). Shrinking is disallowed and will emit a
     * warning Event and throw {@code IllegalArgumentException} (Kubernetes does not support
     * decreasing {@code requests.storage} on a bound PVC). If the StorageClass does not allow
     * expansion, a warning Event is emitted and the update is rejected.
     */
    private void validatePersistentVolumeClaimUpdate(
            PersistentVolumeClaim existing, PersistentVolumeClaim desired) {
        if (!Objects.equals(
                existing.getSpec().getStorageClassName(),
                desired.getSpec().getStorageClassName())) {
            throw new IllegalArgumentException(
                    "Changing the StorageClass of an existing ThingWorx PVC is not supported.");
        }
        var existingQty = getStorageQuantity(existing);
        var desiredQty = getStorageQuantity(desired);
        if (existingQty == null || desiredQty == null) return;
        int cmp;
        try {
            cmp = desiredQty.compareTo(existingQty);
        } catch (Exception exception) {
            LOG.warn(
                    "Failed to compare quantities {} vs {}: {}",
                    desiredQty,
                    existingQty,
                    exception.getMessage());
            cmp = desiredQty.toString().compareTo(existingQty.toString());
        }
        if (cmp == 0) return;
        if (cmp < 0) {
            // Shrinking is disallowed — emit Event/Condition and reject.
            emitPvcEvent(
                    existing,
                    "VolumeShrinkDisallowed",
                    "Shrinking PVC storage is not supported for "
                            + existing.getMetadata().getName()
                            + ": existing "
                            + existingQty
                            + " desired "
                            + desiredQty
                            + ". Shrinking is disallowed.",
                    "Warning");
            throw new IllegalArgumentException(
                    "Shrinking PVC storage is not supported for "
                            + existing.getMetadata().getName()
                            + ": existing "
                            + existingQty
                            + " desired "
                            + desiredQty);
        }
        // Expansion requested
        if (!isStorageClassAllowsExpansion(existing.getSpec().getStorageClassName())) {
            emitPvcEvent(
                    existing,
                    "VolumeExpansionBlocked",
                    "StorageClass "
                            + existing.getSpec().getStorageClassName()
                            + " does not allow volume expansion for PVC "
                            + existing.getMetadata().getName()
                            + " (desired "
                            + desiredQty
                            + " > existing "
                            + existingQty
                            + ")",
                    "Warning");
            throw new IllegalArgumentException(
                    "StorageClass "
                            + existing.getSpec().getStorageClassName()
                            + " does not allow volume expansion for PVC "
                            + existing.getMetadata().getName());
        }
        patchPvcStorage(existing, desiredQty);
    }

    private Quantity getStorageQuantity(PersistentVolumeClaim pvc) {
        if (pvc.getSpec() == null
                || pvc.getSpec().getResources() == null
                || pvc.getSpec().getResources().getRequests() == null) return null;
        return pvc.getSpec().getResources().getRequests().get("storage");
    }

    private boolean isStorageClassAllowsExpansion(String storageClassName) {
        if (blank(storageClassName)) return false;
        try {
            var storageClass = client.storage().storageClasses().withName(storageClassName).get();
            if (storageClass == null) {
                LOG.warn(
                        "StorageClass {} not found; assuming expansion allowed for PVC patch",
                        storageClassName);
                return true;
            }
            return Boolean.TRUE.equals(storageClass.getAllowVolumeExpansion());
        } catch (Exception exception) {
            LOG.warn(
                    "Failed to fetch StorageClass {}: {}; assuming expansion allowed",
                    storageClassName,
                    exception.getMessage());
            return true;
        }
    }

    private void patchPvcStorage(PersistentVolumeClaim existing, Quantity desiredQty) {
        try {
            existing.getSpec().getResources().getRequests().put("storage", desiredQty);
            client.resource(existing).update();
            emitPvcEvent(
                    existing,
                    "VolumeExpansion",
                    "Expanded PVC "
                            + existing.getMetadata().getName()
                            + " to "
                            + desiredQty,
                    "Normal");
            LOG.info(
                    "Patched PVC {} requests.storage to {}",
                    existing.getMetadata().getName(),
                    desiredQty);
        } catch (Exception exception) {
            LOG.warn(
                    "Failed to patch PVC {}: {}",
                    existing.getMetadata().getName(),
                    exception.getMessage());
            throw exception instanceof RuntimeException
                    ? (RuntimeException) exception
                    : new RuntimeException(exception);
        }
    }

    private void emitPvcEvent(
            PersistentVolumeClaim pvc, String reason, String message, String type) {
        try {
            var now = Instant.now().toString();
            var event =
                    new EventBuilder()
                            .withNewMetadata()
                            .withGenerateName(pvc.getMetadata().getName() + "-")
                            .withNamespace(pvc.getMetadata().getNamespace())
                            .endMetadata()
                            .withReason(reason)
                            .withMessage(message)
                            .withType(type)
                            .withInvolvedObject(
                                    new ObjectReferenceBuilder()
                                            .withKind("PersistentVolumeClaim")
                                            .withName(pvc.getMetadata().getName())
                                            .withNamespace(pvc.getMetadata().getNamespace())
                                            .withUid(
                                                    pvc.getMetadata().getUid() == null
                                                            ? ""
                                                            : pvc.getMetadata().getUid())
                                            .build())
                            .withSource(
                                    new EventSourceBuilder()
                                            .withComponent("thingworx-operator")
                                            .build())
                            .withFirstTimestamp(now)
                            .withLastTimestamp(now)
                            .withCount(1)
                            .build();
            try {
                client.resource(event).inNamespace(pvc.getMetadata().getNamespace()).create();
            } catch (Exception nested) {
                // Best-effort: log if Event creation fails (e.g., mock server without Event CRD).
                LOG.debug("Failed to create Event for PVC {}: {}", pvc.getMetadata().getName(), nested.getMessage());
            }
        } catch (Exception exception) {
            LOG.debug("emitPvcEvent failed: {}", exception.getMessage());
        }
    }

    private void reconcilePvcStorage(ThingWorxCluster cluster) {
        var desiredPvcs = new ArrayList<PersistentVolumeClaim>();
        for (HasMetadata resource : ThingWorxResources.precheck(cluster)) {
            if (resource instanceof PersistentVolumeClaim pvc) desiredPvcs.add(pvc);
        }
        for (HasMetadata resource : ThingWorxResources.optional(cluster)) {
            if (resource instanceof PersistentVolumeClaim pvc) desiredPvcs.add(pvc);
        }
        for (var desired : desiredPvcs) {
            var existing =
                    client.persistentVolumeClaims()
                            .inNamespace(desired.getMetadata().getNamespace())
                            .withName(desired.getMetadata().getName())
                            .get();
            if (existing != null) {
                validatePersistentVolumeClaimUpdate(existing, desired);
            }
        }
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
        String prevPhase = status.getPhase();
        String prevMessage = status.getMessage();
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
        // Observability: structured logging, metrics gauge, Kubernetes Events
        String clusterKey = clusterKey(cluster);
        OperatorMetrics.setPhase(clusterKey, phase.name());
        String logLevel = error ? "warn" : degraded ? "warn" : "info";
        if (error || degraded) {
            LOG.warn(
                    "cluster={} phase={} prevPhase={} message=\"{}\" generation={} observedGeneration={} event=statusUpdate",
                    clusterKey,
                    phase,
                    prevPhase,
                    message,
                    cluster.getMetadata() == null ? null : cluster.getMetadata().getGeneration(),
                    status.getObservedGeneration());
        } else {
            LOG.info(
                    "cluster={} phase={} prevPhase={} message=\"{}\" generation={} observedGeneration={} event=statusUpdate",
                    clusterKey,
                    phase,
                    prevPhase,
                    message,
                    cluster.getMetadata() == null ? null : cluster.getMetadata().getGeneration(),
                    status.getObservedGeneration());
        }
        // Emit Kubernetes Event: Normal on phase advance, Warning on INVALID/DEGRADED/Failed
        String eventType = (error || degraded) ? "Warning" : "Normal";
        String reason = phase.name();
        if (error) reason = "FailedValidation";
        else if (degraded) reason = "Degraded";
        else if (advance) reason = "PhaseTransition";
        if (!phase.name().equals(prevPhase) || !Objects.equals(message, prevMessage)) {
            emitClusterEvent(cluster, reason, phase.name() + ": " + message, eventType);
        }
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
        // Pure spec validation — no KubernetesClient I/O
        var pure =
                org.us_ignite.thingworx.operator.api.ThingWorxClusterSpecValidator.validateSpecNullable(
                        cluster.getSpec());
        if (pure != null) return pure;
        // Client-dependent existence checks — only in reconcile path
        var existence = validateReferencedResourcesExist(cluster);
        if (existence != null) {
            emitValidationEvent(cluster, existence);
            return existence;
        }
        return null;
    }

    private String validateReferencedResourcesExist(ThingWorxCluster cluster) {
        var namespace = cluster.getMetadata() == null ? null : cluster.getMetadata().getNamespace();
        if (blank(namespace)) namespace = "default";
        var spec = cluster.getSpec();
        // ConfigMaps
        if (!blank(spec.getSettingsConfigMap())) {
            var missing = missingConfigMap(spec.getSettingsConfigMap(), namespace);
            if (missing != null) return missing;
        }
        // Secrets by name
        if (!blank(spec.getLicenseSecret())) {
            var missing = missingSecret(spec.getLicenseSecret(), namespace);
            if (missing != null) return missing;
        }
        if (!blank(spec.getImagePullSecret())) {
            var missing = missingSecret(spec.getImagePullSecret(), namespace);
            if (missing != null) return missing;
        }
        // SecretKeyReferences
        var checks = new ArrayList<SecretKeyReference>();
        if (spec.getPlatformAdminPassword() != null) checks.add(spec.getPlatformAdminPassword());
        if (spec.getKeystorePassword() != null) checks.add(spec.getKeystorePassword());
        if (spec.getProvisioningAppKey() != null) checks.add(spec.getProvisioningAppKey());
        if (spec.getCxServerAppKey() != null) checks.add(spec.getCxServerAppKey());
        if (spec.getExtensionImportAppKey() != null) checks.add(spec.getExtensionImportAppKey());
        if (spec.getDatabase() != null) {
            if (spec.getDatabase().getCredentials() != null) checks.add(spec.getDatabase().getCredentials());
            if (spec.getDatabase().getAdminCredentials() != null) checks.add(spec.getDatabase().getAdminCredentials());
        }
        for (var ref : checks) {
            if (ref == null || blank(ref.getName())) continue;
            var missing = missingSecret(ref.getName(), namespace);
            if (missing != null) return missing;
        }
        return null;
    }

    private String missingSecret(String name, String namespace) {
        if (blank(name) || blank(namespace)) return null;
        try {
            var secret = client.secrets().inNamespace(namespace).withName(name).get();
            if (secret == null) return "Secret '" + name + "' not found in namespace '" + namespace + "'";
        } catch (Exception exception) {
            LOG.debug("missingSecret check failed for {}/{}: {}", namespace, name, exception.getMessage());
            return "Secret '" + name + "' not found in namespace '" + namespace + "'";
        }
        return null;
    }

    private String missingConfigMap(String name, String namespace) {
        if (blank(name) || blank(namespace)) return null;
        try {
            var configMap = client.configMaps().inNamespace(namespace).withName(name).get();
            if (configMap == null)
                return "ConfigMap '" + name + "' not found in namespace '" + namespace + "'";
        } catch (Exception exception) {
            LOG.debug("missingConfigMap check failed for {}/{}: {}", namespace, name, exception.getMessage());
            return "ConfigMap '" + name + "' not found in namespace '" + namespace + "'";
        }
        return null;
    }

    private void emitValidationEvent(ThingWorxCluster cluster, String message) {
        try {
            var namespace =
                    cluster.getMetadata() == null || blank(cluster.getMetadata().getNamespace())
                            ? "default"
                            : cluster.getMetadata().getNamespace();
            var name = cluster.getMetadata() == null ? "unknown" : cluster.getMetadata().getName();
            var uid = cluster.getMetadata() == null ? "" : cluster.getMetadata().getUid();
            var now = Instant.now().toString();
            var event =
                    new EventBuilder()
                            .withNewMetadata()
                            .withGenerateName(name + "-")
                            .withNamespace(namespace)
                            .endMetadata()
                            .withReason("FailedValidation")
                            .withMessage(message)
                            .withType("Warning")
                            .withInvolvedObject(
                                    new ObjectReferenceBuilder()
                                            .withKind("ThingWorxCluster")
                                            .withName(name)
                                            .withNamespace(namespace)
                                            .withUid(uid == null ? "" : uid)
                                            .withApiVersion("thingworx.us-ignite.org/v1alpha1")
                                            .build())
                            .withSource(
                                    new EventSourceBuilder()
                                            .withComponent("thingworx-operator")
                                            .build())
                            .withFirstTimestamp(now)
                            .withLastTimestamp(now)
                            .withCount(1)
                            .build();
            try {
                client.resource(event).inNamespace(namespace).create();
            } catch (Exception nested) {
                LOG.debug("Failed to create validation Event: {}", nested.getMessage());
            }
        } catch (Exception exception) {
            LOG.debug("emitValidationEvent failed: {}", exception.getMessage());
        }
    }

    private void pruneSupersededInitializationJobs(ThingWorxCluster cluster) {
        var namespace = cluster.getMetadata().getNamespace();
        var instance = cluster.getMetadata().getName();
        var desired = new java.util.HashSet<String>();
        ThingWorxResources.database(cluster).stream()
                .filter(Job.class::isInstance)
                .map(Job.class::cast)
                .forEach(job -> desired.add(job.getMetadata().getName()));
        client.batch()
                .v1()
                .jobs()
                .inNamespace(namespace)
                .withLabel(INSTANCE_LABEL, instance)
                .list()
                .getItems()
                .stream()
                .filter(job -> isManaged(job, instance))
                .filter(job -> !desired.contains(job.getMetadata().getName()))
                .forEach(
                        job -> {
                            LOG.info(
                                    "Pruning superseded initialization Job {}/{}",
                                    namespace,
                                    job.getMetadata().getName());
                            client.resource(job).delete();
                        });
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
                resource -> {
                    var kind = resource.getKind();
                    if (kind == null) {
                        if (resource instanceof io.fabric8.kubernetes.api.model.ConfigMap)
                            kind = "ConfigMap";
                        else if (resource instanceof Job) kind = "Job";
                        else if (resource
                                instanceof io.fabric8.kubernetes.api.model.PersistentVolumeClaim)
                            kind = "PersistentVolumeClaim";
                        else if (resource instanceof StatefulSet) kind = "StatefulSet";
                        else if (resource instanceof Deployment) kind = "Deployment";
                        else if (resource
                                instanceof io.fabric8.kubernetes.api.model.Service)
                            kind = "Service";
                        else if (resource
                                instanceof
                                io.fabric8.kubernetes.api.model.networking.v1.Ingress)
                            kind = "Ingress";
                        else kind = resource.getClass().getSimpleName();
                    }
                    desired.add(kind + "/" + resource.getMetadata().getName());
                });
        // ConfigMap pruning — label-managed, safe to delete orphans directly (no PVC data risk).
        client.configMaps()
                .inNamespace(namespace)
                .withLabel(INSTANCE_LABEL, instance)
                .list()
                .getItems()
                .stream()
                .filter(item -> isManaged(item, instance))
                .filter(item -> !desired.contains("ConfigMap/" + item.getMetadata().getName()))
                .forEach(
                        item -> {
                            LOG.info(
                                    "Pruning orphan ConfigMap {}/{}",
                                    namespace,
                                    item.getMetadata().getName());
                            client.resource(item).delete();
                        });
        // Service pruning — label-managed resources not owned via controller GC in-place toggles.
        client.services()
                .inNamespace(namespace)
                .withLabel(INSTANCE_LABEL, instance)
                .list()
                .getItems()
                .stream()
                .filter(item -> isManaged(item, instance))
                .filter(item -> !desired.contains("Service/" + item.getMetadata().getName()))
                .forEach(
                        item -> {
                            LOG.info(
                                    "Pruning orphan Service {}/{}",
                                    namespace,
                                    item.getMetadata().getName());
                            client.resource(item).delete();
                        });
        // Ingress pruning — label-managed.
        client.network()
                .v1()
                .ingresses()
                .inNamespace(namespace)
                .withLabel(INSTANCE_LABEL, instance)
                .list()
                .getItems()
                .stream()
                .filter(item -> isManaged(item, instance))
                .filter(item -> !desired.contains("Ingress/" + item.getMetadata().getName()))
                .forEach(
                        item -> {
                            LOG.info(
                                    "Pruning orphan Ingress {}/{}",
                                    namespace,
                                    item.getMetadata().getName());
                            client.resource(item).delete();
                        });
        // PVC pruning — guarded by annotation to avoid accidental data loss.
        client.persistentVolumeClaims()
                .inNamespace(namespace)
                .withLabel(INSTANCE_LABEL, instance)
                .list()
                .getItems()
                .stream()
                .filter(item -> isManaged(item, instance))
                .filter(item -> !desired.contains("PersistentVolumeClaim/" + item.getMetadata().getName()))
                .filter(
                        item -> {
                            var annotations = item.getMetadata().getAnnotations();
                            return annotations != null
                                    && "true".equals(annotations.get(PRUNE_PVC_ANNOTATION));
                        })
                .forEach(
                        item -> {
                            LOG.info(
                                    "Pruning orphan PVC {}/{} (annotated {})",
                                    namespace,
                                    item.getMetadata().getName(),
                                    PRUNE_PVC_ANNOTATION);
                            client.resource(item).delete();
                        });
        // Deployment pruning — owned via ownerReferences but HA toggle needs explicit orphan removal.
        client.apps()
                .deployments()
                .inNamespace(namespace)
                .withLabel(INSTANCE_LABEL, instance)
                .list()
                .getItems()
                .stream()
                .filter(item -> isManaged(item, instance))
                .filter(item -> !desired.contains("Deployment/" + item.getMetadata().getName()))
                .forEach(
                        item -> {
                            LOG.info(
                                    "Pruning orphan Deployment {}/{}",
                                    namespace,
                                    item.getMetadata().getName());
                            client.resource(item).delete();
                        });
        // StatefulSet pruning — same guard as Deployments.
        client.apps()
                .statefulSets()
                .inNamespace(namespace)
                .withLabel(INSTANCE_LABEL, instance)
                .list()
                .getItems()
                .stream()
                .filter(item -> isManaged(item, instance))
                .filter(item -> !desired.contains("StatefulSet/" + item.getMetadata().getName()))
                .forEach(
                        item -> {
                            LOG.info(
                                    "Pruning orphan StatefulSet {}/{}",
                                    namespace,
                                    item.getMetadata().getName());
                            client.resource(item).delete();
                        });
    }

    private boolean isManaged(HasMetadata resource, String instance) {
        var labels = resource.getMetadata().getLabels();
        if (labels == null) return false;
        return MANAGED_BY_VALUE.equals(labels.get(MANAGED_BY_LABEL))
                && instance.equals(labels.get(INSTANCE_LABEL));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean invalidSecretReference(SecretKeyReference reference) {
        return reference == null || blank(reference.getName()) || blank(reference.getKey());
    }

    private String clusterKey(ThingWorxCluster cluster) {
        if (cluster == null || cluster.getMetadata() == null) return "unknown/unknown";
        String ns = cluster.getMetadata().getNamespace();
        String name = cluster.getMetadata().getName();
        if (ns == null || ns.isBlank()) ns = "default";
        if (name == null || name.isBlank()) name = "unknown";
        return ns + "/" + name;
    }

    private void emitClusterEvent(ThingWorxCluster cluster, String reason, String message, String type) {
        try {
            String namespace =
                    cluster.getMetadata() == null || blank(cluster.getMetadata().getNamespace())
                            ? "default"
                            : cluster.getMetadata().getNamespace();
            String name = cluster.getMetadata() == null ? "unknown" : cluster.getMetadata().getName();
            String uid = cluster.getMetadata() == null ? "" : cluster.getMetadata().getUid();
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
                                            .withKind("ThingWorxCluster")
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
                // Preferred: client.v1().events() — fallback to generic resource creation for mock compatibility
                try {
                    client.v1().events().inNamespace(namespace).resource(event).create();
                } catch (Exception e1) {
                    client.resource(event).inNamespace(namespace).create();
                }
            } catch (Exception nested) {
                LOG.debug("Failed to create cluster Event: {}", nested.getMessage());
            }
        } catch (Exception exception) {
            LOG.debug("emitClusterEvent failed: {}", exception.getMessage());
        }
    }
}
