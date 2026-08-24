package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a Message/Error service result without a nested payload. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DapNoPayloadServiceResult {
    /**
     * Returns the configured value.
     *
     * @return the service-result DataShape name.
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
     * @return the service-result DataShape description.
     */
    String description() default "Status and message returned by a service.";
}
