package org.us_ignite.thingworx.operator.api;

import java.util.Optional;

/**
 * Alias for {@link ThingWorxClusterSpecValidator} with short name, for convenience.
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
