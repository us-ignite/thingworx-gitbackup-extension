package org.us_ignite.thingworx.dap.runtime;

/**
 * Compile-time pairing between a generated table proxy and its service-result envelope.
 *
 * @param <T> the generated payload table proxy type
 * @param shape the service-result DataShape definition
 */
public record DapServiceContract<T extends DapDataShapeProxy<?>>(DapServiceResultShape shape) {}
