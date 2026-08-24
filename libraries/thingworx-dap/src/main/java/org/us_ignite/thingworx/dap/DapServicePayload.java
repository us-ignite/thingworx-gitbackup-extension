package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Links a ThingWorx service to its logical DAP payload declaration. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface DapServicePayload {
    /**
     * Returns the configured value.
     *
     * @return the Java type that declares the service payload DataShape.
     */
    Class<?> value();
}
