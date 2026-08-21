package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a ThingTemplate entity used by the extension. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DapThingTemplate {
    String name();

    String baseThingTemplate();

    String projectName() default "GIT";

    String description() default "";

    String[] implementedShapes() default {};
}
