package org.us_ignite.thingworx.dap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Declares a property on a generated ThingShape. */
@Retention(RetentionPolicy.SOURCE)
public @interface DapProperty {
    /**
     * Returns the configured value.
     *
     * @return the property name.
     */
    String name();

    /**
     * Returns the configured value.
     *
     * @return the ThingWorx base type.
     */
    DapBaseType baseType();

    /**
     * Returns the configured value.
     *
     * @return the property order in the ThingShape.
     */
    int ordinal();

    /**
     * Returns the configured value.
     *
     * @return the optional property description.
     */
    String description() default "";

    /**
     * Returns the configured value.
     *
     * @return the optional property category.
     */
    String category() default "";

    /**
     * Returns the configured value.
     *
     * @return the optional default value.
     */
    String defaultValue() default "";

    /**
     * Returns the configured value.
     *
     * @return whether the property persists its value.
     */
    boolean persistent() default true;

    /**
     * Returns the configured value.
     *
     * @return the cache time in seconds, or {@code -1} for the platform default.
     */
    double cacheTime() default -1;

    /**
     * Returns the configured value.
     *
     * @return the optional data-change event type.
     */
    String dataChangeType() default "";
}
