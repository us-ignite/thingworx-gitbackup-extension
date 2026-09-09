package org.us_ignite.thingworx.dap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Declares one row of the default localization table. */
@Retention(RetentionPolicy.SOURCE)
public @interface DapLocalizationToken {
    /**
     * Returns the configured value.
     *
     * @return the localization token name.
     */
    String name();

    /**
     * Returns the configured value.
     *
     * @return the localized token value.
     */
    String value();

    /**
     * Returns the configured value.
     *
     * @return the optional token context.
     */
    String context() default "";

    /**
     * Returns the configured value.
     *
     * @return the optional token usage.
     */
    String usage() default "";
}
