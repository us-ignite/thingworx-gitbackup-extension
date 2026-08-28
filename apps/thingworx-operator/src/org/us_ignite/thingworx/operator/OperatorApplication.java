package org.us_ignite.thingworx.operator;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.Operator;
import org.us_ignite.thingworx.operator.reconcile.ThingWorxClusterReconciler;
import org.us_ignite.thingworx.operator.reconcile.ThingWorxExtensionReconciler;

/** Starts the ThingWorx controller manager. */
public final class OperatorApplication {
    private OperatorApplication() {}

    public static void main(String[] args) {
        var operator = new Operator();
        var client = new KubernetesClientBuilder().build();
        operator.register(new ThingWorxClusterReconciler(client));
        operator.register(new ThingWorxExtensionReconciler(client));
        operator.installShutdownHook();
        operator.start();
        try {
            new java.util.concurrent.CountDownLatch(1).await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
