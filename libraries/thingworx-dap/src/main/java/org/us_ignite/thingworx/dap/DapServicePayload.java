package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Links a ThingWorx service to its logical DAP payload declaration. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface DapServicePayload {
    Class<?> value();
}
