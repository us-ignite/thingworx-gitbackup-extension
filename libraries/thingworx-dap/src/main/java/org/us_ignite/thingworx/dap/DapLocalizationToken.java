package org.us_ignite.thingworx.dap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Declares one row of the default localization table. */
@Retention(RetentionPolicy.SOURCE)
public @interface DapLocalizationToken {
    String name();

    String value();

    String context() default "";

    String usage() default "";
}
