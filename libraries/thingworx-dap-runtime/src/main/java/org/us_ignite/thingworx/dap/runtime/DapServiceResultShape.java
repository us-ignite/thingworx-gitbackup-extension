package org.us_ignite.thingworx.dap.runtime;

/**
 * Names the final service-result DataShape and its optional payload DataShape.
 *
 * @param resultDataShapeName the service-result DataShape name
 * @param payloadDataShapeName the optional payload DataShape name
 */
public record DapServiceResultShape(String resultDataShapeName, String payloadDataShapeName) {
    /**
     * Returns the documented value.
     *
     * @return whether this service result has a payload DataShape.
     */
    public boolean hasPayload() {
        return payloadDataShapeName != null && !payloadDataShapeName.isBlank();
    }
}
