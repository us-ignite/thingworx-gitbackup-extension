package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares the XML metadata surrounding a Java-backed ThingShape. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DapThingShape {
    String name();

    String projectName() default "GIT";

    String description() default "";

    DapProperty[] properties() default {};

    String[] alertProperties() default {};
}
