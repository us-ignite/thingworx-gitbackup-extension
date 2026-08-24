package org.us_ignite.thingworx.dap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Declares a ThingPackage entry in extension metadata. */
@Retention(RetentionPolicy.SOURCE)
public @interface DapThingPackage {
    /**
     * Returns the configured value.
     *
     * @return the ThingPackage name.
     */
    String name();

    /**
     * Returns the configured value.
     *
     * @return the fully qualified Java class name.
     */
    String className();

    /**
     * Returns the configured value.
     *
     * @return the optional ThingPackage description.
     */
    String description() default "";
}
