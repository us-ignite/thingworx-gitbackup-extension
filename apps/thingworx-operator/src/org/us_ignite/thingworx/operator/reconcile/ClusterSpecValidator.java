package org.us_ignite.thingworx.operator.reconcile;

import java.util.Optional;
import org.us_ignite.thingworx.operator.api.ThingWorxClusterSpec;
import org.us_ignite.thingworx.operator.api.ThingWorxClusterSpecValidator;

/**
 * Alias for {@link ThingWorxClusterSpecValidator} in the reconcile package.
 * Keeps import path stable for existing tests that may reference
 * {@code org.us_ignite.thingworx.operator.reconcile.ClusterSpecValidator}.
 */
public final class ClusterSpecValidator {
    private ClusterSpecValidator() {}

    public static Optional<String> validateSpec(ThingWorxClusterSpec spec) {
        return ThingWorxClusterSpecValidator.validateSpec(spec);
    }

    public static String validateSpecNullable(ThingWorxClusterSpec spec) {
        return ThingWorxClusterSpecValidator.validateSpecNullable(spec);
    }
}
