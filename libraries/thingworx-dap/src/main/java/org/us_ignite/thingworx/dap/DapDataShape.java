package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a ThingWorx DataShape and its generated Java row/table proxies. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DapDataShape {
    String name();

    String projectName() default "GIT";

    String description() default "";

    boolean generateServiceResult() default true;

    String serviceResultName() default "";

    String serviceResultDescription() default "";

    String payloadDescription() default "Payload";
}
