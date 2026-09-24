package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares generated extension metadata.xml content. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DapExtensionPackage {
    /**
     * Returns the configured value.
     *
     * @return the extension package name.
     */
    String name();

    /**
     * Returns the configured value.
     *
     * @return the extension package description.
     */
    String description();

    /**
     * Returns the configured value.
     *
     * @return the extension vendor name.
     */
    String vendor();

    /**
     * Returns the configured value.
     *
     * @return the fully qualified extension migrator class name.
     */
    String migratorClass();

    /**
     * Returns the configured value.
     *
     * @return whether the extension supports high availability.
     */
    boolean haCompatible() default true;

    /**
     * Returns the configured value.
     *
     * @return the ThingPackages included in the extension.
     */
    DapThingPackage[] thingPackages() default {};

    /**
     * Returns the configured value.
     *
     * @return localization tokens included in the extension.
     */
    DapLocalizationToken[] localizationTokens() default {};
}
