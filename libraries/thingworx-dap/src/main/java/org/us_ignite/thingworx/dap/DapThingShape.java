package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares the XML metadata surrounding a Java-backed ThingShape. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DapThingShape {
    /**
     * Returns the configured value.
     *
     * @return the ThingShape name.
     */
    String name();

    /**
     * Returns the configured value.
     *
     * @return the owning project name.
     */
    String projectName() default "GIT";

    /**
     * Returns the configured value.
     *
     * @return the optional ThingShape description.
     */
    String description() default "";

    /**
     * Returns the configured value.
     *
     * @return properties declared by the ThingShape.
     */
    DapProperty[] properties() default {};

    /**
     * Returns the configured value.
     *
     * @return property names for which alerts are enabled.
     */
    String[] alertProperties() default {};
}
