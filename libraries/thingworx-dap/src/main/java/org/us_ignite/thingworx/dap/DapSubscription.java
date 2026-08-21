package org.us_ignite.thingworx.dap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Declares the script subscription form used by extension Things. */
@Retention(RetentionPolicy.SOURCE)
public @interface DapSubscription {
    String name();

    String eventName();

    String script();

    String source() default "";

    String sourceProperty() default "";

    boolean enabled() default true;
}
