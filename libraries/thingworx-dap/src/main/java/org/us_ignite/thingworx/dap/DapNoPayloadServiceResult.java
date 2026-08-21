package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a Message/Error service result without a nested payload. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DapNoPayloadServiceResult {
    String name();

    String projectName() default "GIT";

    String description() default "Status and message returned by a service.";
}
