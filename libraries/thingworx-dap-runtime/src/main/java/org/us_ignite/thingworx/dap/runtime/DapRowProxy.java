package org.us_ignite.thingworx.dap.runtime;

/**
 * Live typed facade over a platform row object.
 *
 * @param <T> the platform row representation
 */
public interface DapRowProxy<T> {
    /**
     * Returns the documented value.
     *
     * @return static identity information for the represented DataShape.
     */
    DapShape shape();

    /**
     * Returns the documented value.
     *
     * @return the represented platform row object.
     */
    T toValueCollection();
}
