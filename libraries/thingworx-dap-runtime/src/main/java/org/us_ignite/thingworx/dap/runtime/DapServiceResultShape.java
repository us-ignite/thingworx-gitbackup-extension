package org.us_ignite.thingworx.dap.runtime;

/** Names the final service-result DataShape and its optional payload DataShape. */
public record DapServiceResultShape(String resultDataShapeName, String payloadDataShapeName) {
    public boolean hasPayload() {
        return payloadDataShapeName != null && !payloadDataShapeName.isBlank();
    }
}
