package org.us_ignite.thingworx.dap.runtime;

/** Live typed facade over a platform table object. */
public interface DapDataShapeProxy<T> {
    DapShape shape();

    T toInfoTable();
}
