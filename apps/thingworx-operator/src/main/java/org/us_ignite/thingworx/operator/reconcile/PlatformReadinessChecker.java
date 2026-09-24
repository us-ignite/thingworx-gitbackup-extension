package org.us_ignite.thingworx.operator.reconcile;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;

/**
 * Pure readiness check for platform StatefulSet rollout gating.
 * No KubernetesClient — caller provides the StatefulSet.
 */
public final class PlatformReadinessChecker {
    private static final String RESTART_ANNOTATION = "thingworx.us-ignite.org/extension-restart";

    private PlatformReadinessChecker() {}

    public static boolean isReady(StatefulSet statefulSet, String requestedRestart) {
        if (statefulSet == null
                || statefulSet.getSpec() == null
                || statefulSet.getStatus() == null
                || statefulSet.getSpec().getReplicas() == null) return false;
        var template = statefulSet.getSpec().getTemplate();
        if (template == null || template.getMetadata() == null) return false;
        var annotations = template.getMetadata().getAnnotations();
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
}
