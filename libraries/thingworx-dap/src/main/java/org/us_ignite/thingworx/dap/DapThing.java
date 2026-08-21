package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a singleton Thing shipped by the extension. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DapThing {
    String name();

    String thingTemplate();

    String projectName() default "GIT";

    String description() default "";

    String effectiveThingPackage() default "";

    boolean enabled() default true;

    boolean published() default false;

    String[] implementedShapes() default {};

    DapSubscription[] subscriptions() default {};
}
