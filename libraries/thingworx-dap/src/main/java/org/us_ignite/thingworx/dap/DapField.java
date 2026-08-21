package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares one field in a generated ThingWorx DataShape. */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface DapField {
    String name();

    DapBaseType baseType();

    int ordinal();

    String description() default "";

    boolean primaryKey() default false;

    String dataShape() default "";
}
