package org.us_ignite.thingworx.dap.runtime;

/** Compile-time pairing between a generated table proxy and its service-result envelope. */
public record DapServiceContract<T extends DapDataShapeProxy<?>>(DapServiceResultShape shape) {}
