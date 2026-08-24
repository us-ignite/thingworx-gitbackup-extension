package org.us_ignite.thingworx.dap.runtime;

/**
 * Live typed facade over a platform table object.
 *
 * @param <T> the platform table representation
 */
public interface DapDataShapeProxy<T> {
    /**
     * Returns the documented value.
     *
     * @return static identity information for the represented DataShape.
     */
    DapShape shape();

    /**
     * Returns the documented value.
     *
     * @return the represented platform table object.
     */
    T toInfoTable();
}
