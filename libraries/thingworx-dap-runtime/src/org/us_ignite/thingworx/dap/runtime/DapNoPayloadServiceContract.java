package org.us_ignite.thingworx.dap.runtime;

import java.util.Objects;

/**
 * Compile-time contract for a generated service-result envelope without a payload field.
 *
 * @param shape the service-result DataShape definition
 */
public record DapNoPayloadServiceContract(DapServiceResultShape shape) {
    /** Validates that the result definition does not declare a payload DataShape. */
    public DapNoPayloadServiceContract {
        Objects.requireNonNull(shape, "shape");
        if (shape.hasPayload()) {
            throw new IllegalArgumentException(
                    "No-payload contract cannot declare a payload shape");
        }
    }
}
