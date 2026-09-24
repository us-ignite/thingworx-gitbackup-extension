package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a ThingWorx Project entity. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DapProject {
    /**
     * Returns the configured value.
     *
     * @return the project name.
     */
    String name();

    /**
     * Returns the configured value.
     *
     * @return the optional project description.
     */
    String description() default "";

    /**
     * Returns the configured value.
     *
     * @return the project package version.
     */
    String packageVersion() default "1.0.0";

    /**
     * Returns the configured value.
     *
     * @return whether ThingWorx may modify entities created in this project at runtime.
     */
    boolean editable() default false;
}
