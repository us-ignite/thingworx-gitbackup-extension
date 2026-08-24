package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a ThingTemplate entity used by the extension. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DapThingTemplate {
    /**
     * Returns the configured value.
     *
     * @return the ThingTemplate name.
     */
    String name();

    /**
     * Returns the configured value.
     *
     * @return the base ThingTemplate name.
     */
    String baseThingTemplate();

    /**
     * Returns the configured value.
     *
     * @return the owning project name.
     */
    String projectName() default "GIT";

    /**
     * Returns the configured value.
     *
     * @return the optional ThingTemplate description.
     */
    String description() default "";

    /**
     * Returns the configured value.
     *
     * @return the ThingShapes implemented by the template.
     */
    String[] implementedShapes() default {};
}
