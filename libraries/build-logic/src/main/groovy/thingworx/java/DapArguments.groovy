package thingworx.java

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.process.CommandLineArgumentProvider

/** Annotation processor arguments; the generated entity directory is a declared compile output. */
abstract class DapArguments implements CommandLineArgumentProvider {
    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    @Input
    abstract Property<String> getPackageVersion()

    @Input
    abstract Property<String> getMinimumThingWorxVersion()

    @Override
    Iterable<String> asArguments() {
        ["-Adap.output=${outputDir.get().asFile.absolutePath}".toString(),
         "-Adap.packageVersion=${packageVersion.get()}".toString(),
         "-Adap.minimumThingWorxVersion=${minimumThingWorxVersion.get()}".toString()]
    }
}
