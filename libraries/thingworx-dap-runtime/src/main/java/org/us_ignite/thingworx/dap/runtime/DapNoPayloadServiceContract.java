package org.us_ignite.thingworx.dap.runtime;

import java.util.Objects;

/** Compile-time contract for a generated service-result envelope without a payload field. */
public record DapNoPayloadServiceContract(DapServiceResultShape shape) {
    public DapNoPayloadServiceContract {
        Objects.requireNonNull(shape, "shape");
        if (shape.hasPayload()) {
            throw new IllegalArgumentException(
                    "No-payload contract cannot declare a payload shape");
        }
    }
}
