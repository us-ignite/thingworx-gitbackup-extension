package thingworx.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

/** A {@code -Dname=path} JVM argument whose file is a tracked input (and inferred task dependency). */
abstract class FileSystemProperty implements CommandLineArgumentProvider {
    @Input
    abstract org.gradle.api.provider.Property<String> getName()

    @InputFile
    @PathSensitive(PathSensitivity.NAME_ONLY)
    abstract RegularFileProperty getFile()

    @Override
    Iterable<String> asArguments() {
        ["-D${name.get()}=${file.get().asFile.absolutePath}".toString()]
    }
}
