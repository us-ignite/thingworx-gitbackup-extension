package org.us_ignite.thingworx.dap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares generated extension metadata.xml content. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DapExtensionPackage {
    String name();

    String description();

    String vendor();

    String migratorClass();

    boolean haCompatible() default true;

    DapThingPackage[] thingPackages() default {};

    DapLocalizationToken[] localizationTokens() default {};
}
