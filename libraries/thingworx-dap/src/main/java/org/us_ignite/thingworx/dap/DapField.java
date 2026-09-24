package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares one field in a generated ThingWorx DataShape. */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface DapField {
    /**
     * Returns the configured value.
     *
     * @return the field name.
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
     * @return the field order in the DataShape.
     */
    int ordinal();

    /**
     * Returns the configured value.
     *
     * @return the optional field description.
     */
    String description() default "";

    /**
     * Returns the configured value.
     *
     * @return whether the field is a primary key.
     */
    boolean primaryKey() default false;

    /**
     * Returns the configured value.
     *
     * @return the nested DataShape name for an INFOTABLE field.
     */
    String dataShape() default "";
}
