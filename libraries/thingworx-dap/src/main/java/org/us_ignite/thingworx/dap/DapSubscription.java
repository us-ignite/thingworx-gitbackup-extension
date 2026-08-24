package org.us_ignite.thingworx.dap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Declares the script subscription form used by extension Things. */
@Retention(RetentionPolicy.SOURCE)
public @interface DapSubscription {
    /**
     * Returns the configured value.
     *
     * @return the subscription name.
     */
    String name();

    /**
     * Returns the configured value.
     *
     * @return the event to which the subscription is attached.
     */
    String eventName();

    /**
     * Returns the configured value.
     *
     * @return the subscription script.
     */
    String script();

    /**
     * Returns the configured value.
     *
     * @return the optional event source.
     */
    String source() default "";

    /**
     * Returns the configured value.
     *
     * @return the optional source property.
     */
    String sourceProperty() default "";

    /**
     * Returns the configured value.
     *
     * @return whether the subscription is enabled.
     */
    boolean enabled() default true;
}
