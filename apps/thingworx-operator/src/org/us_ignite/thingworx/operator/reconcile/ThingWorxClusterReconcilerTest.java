package org.us_ignite.thingworx.operator.reconcile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ThingWorxClusterReconcilerTest {
    @Test
    void reconcilerCanBeConstructed() {
        assertThat(new ThingWorxClusterReconciler()).isNotNull();
    }
}
