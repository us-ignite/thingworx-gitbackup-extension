package org.us_ignite.thingworx.dap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Declares a property on a generated ThingShape. */
@Retention(RetentionPolicy.SOURCE)
public @interface DapProperty {
    String name();

    DapBaseType baseType();

    int ordinal();

    String description() default "";

    String category() default "";

    String defaultValue() default "";

    boolean persistent() default true;

    double cacheTime() default -1;

    String dataChangeType() default "";
}
