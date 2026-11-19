package thingworx.java

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property

/** {@code thingworxSdk { version = '9.6' }}; {@code jars} is built by {@code unpackSdk}. */
abstract class ThingworxSdkExtension {
    abstract Property<String> getVersion()

    /** The SDK jars, carrying a task dependency on {@code unpackSdk}. */
    abstract ConfigurableFileCollection getJars()
}
