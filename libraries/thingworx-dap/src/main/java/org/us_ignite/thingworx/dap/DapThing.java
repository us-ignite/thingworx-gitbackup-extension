package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a singleton Thing shipped by the extension. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DapThing {
    /**
     * Returns the configured value.
     *
     * @return the Thing name.
     */
    String name();

    /**
     * Returns the configured value.
     *
     * @return the ThingTemplate used by the Thing.
     */
    String thingTemplate();

    /**
     * Returns the configured value.
     *
     * @return the owning project name.
     */
    String projectName() default "GIT";

    /**
     * Returns the configured value.
     *
     * @return the optional Thing description.
     */
    String description() default "";

    /**
     * Returns the configured value.
     *
     * @return the optional effective ThingPackage name.
     */
    String effectiveThingPackage() default "";

    /**
     * Returns the configured value.
     *
     * @return whether the Thing is enabled.
     */
    boolean enabled() default true;

    /**
     * Returns the configured value.
     *
     * @return whether the Thing is published.
     */
    boolean published() default false;

    /**
     * Returns the configured value.
     *
     * @return the ThingShapes implemented by the Thing.
     */
    String[] implementedShapes() default {};

    /**
     * Returns the configured value.
     *
     * @return subscriptions declared for the Thing.
     */
    DapSubscription[] subscriptions() default {};
}
