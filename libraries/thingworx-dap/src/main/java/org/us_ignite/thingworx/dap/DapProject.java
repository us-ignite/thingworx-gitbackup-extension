package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a ThingWorx Project entity. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DapProject {
    String name();

    String description() default "";

    String packageVersion() default "1.0.0";

    /** Whether ThingWorx may modify entities created in this extension project at runtime. */
    boolean editable() default false;
}
