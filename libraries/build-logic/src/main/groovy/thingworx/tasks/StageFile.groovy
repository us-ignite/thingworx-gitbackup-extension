package thingworx.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import thingworx.docker.VendorArchives

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Copies a single file to a single destination. Unlike a {@code Copy} into a shared directory,
 * the output is exactly one file, so several staging tasks can fill the same vendor build context
 * without overlapping outputs.
 */
@DisableCachingByDefault(because = 'Copying a file is cheaper than the build cache')
abstract class StageFile extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getSource()

    @OutputFile
    abstract RegularFileProperty getDestination()

    /** Fail fast when the source is still git-filter ciphertext. */
    @Input
    abstract Property<Boolean> getRequirePlaintext()

    StageFile() {
        group = 'images'
        requirePlaintext.convention(false)
    }

    @TaskAction
    void stage() {
        File from = source.get().asFile
        if (requirePlaintext.get()) VendorArchives.requirePlaintext(from)
        File to = destination.get().asFile
        to.parentFile.mkdirs()
        Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}
