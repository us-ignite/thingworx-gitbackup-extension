package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a ThingWorx DataShape and its generated Java row/table proxies. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DapDataShape {
    /**
     * Returns the configured value.
     *
     * @return the DataShape name.
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
     * @return the optional DataShape description.
     */
    String description() default "";

    /**
     * Returns the configured value.
     *
     * @return whether to generate a service-result DataShape.
     */
    boolean generateServiceResult() default true;

    /**
     * Returns the configured value.
     *
     * @return the optional generated service-result name.
     */
    String serviceResultName() default "";

    /**
     * Returns the configured value.
     *
     * @return the optional service-result description.
     */
    String serviceResultDescription() default "";

    /**
     * Returns the configured value.
     *
     * @return the payload field description in the service result.
     */
    String payloadDescription() default "Payload";
}
