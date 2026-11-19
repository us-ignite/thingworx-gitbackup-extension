package thingworx.java

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

/** {@code thingworxDap { }}: options passed to the DAP annotation processor. */
abstract class DapExtension {
    abstract DirectoryProperty getOutputDir()

    abstract Property<String> getPackageVersion()

    abstract Property<String> getMinimumThingWorxVersion()
}
