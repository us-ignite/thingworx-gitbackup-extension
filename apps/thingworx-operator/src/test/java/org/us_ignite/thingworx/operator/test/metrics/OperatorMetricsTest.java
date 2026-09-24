package org.us_ignite.thingworx.operator.test.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.us_ignite.thingworx.operator.metrics.OperatorMetrics;

class OperatorMetricsTest {

    @AfterEach
    void clearMetrics() {
        OperatorMetrics.reset();
    }

    @Test
    void changingAnExtensionPhaseClearsThePreviousMixedCaseGauge() {
        OperatorMetrics.setPhase("thingworx/entities", "Importing");
        OperatorMetrics.setPhase("thingworx/entities", "Ready");

        assertThat(OperatorMetrics.prometheusExposition())
                .contains(
                        "thingworx_operator_phase{cluster=\"thingworx/entities\",phase=\"Importing\"} 0",
                        "thingworx_operator_phase{cluster=\"thingworx/entities\",phase=\"Ready\"} 1");
    }
}
