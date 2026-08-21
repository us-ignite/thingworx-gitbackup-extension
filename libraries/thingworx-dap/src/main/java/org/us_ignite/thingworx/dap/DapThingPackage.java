package org.us_ignite.thingworx.dap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Declares a ThingPackage entry in extension metadata. */
@Retention(RetentionPolicy.SOURCE)
public @interface DapThingPackage {
    String name();

    String className();

    String description() default "";
}
