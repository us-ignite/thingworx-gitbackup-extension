package org.us_ignite.thingworx.dap.runtime;

/** Live typed facade over a platform row object. */
public interface DapRowProxy<T> {
    DapShape shape();

    T toValueCollection();
}
