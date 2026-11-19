package thingworx.docker

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject

/**
 * Extracts a gzip-compressed vendor tarball (with the system tar, preserving modes) into a vendor
 * build context. The vendor build scripts then write into that same directory, so the declared
 * output is an extraction stamp rather than the directory itself: re-extraction happens only when
 * the archive changes or the context is deleted.
 */
@DisableCachingByDefault(because = 'Extracted vendor contexts are local working directories')
abstract class ExtractVendorArchive extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getArchive()

    @Internal
    abstract DirectoryProperty getDestination()

    @OutputFile
    abstract RegularFileProperty getStamp()

    @Inject
    abstract ExecOperations getExecOperations()

    ExtractVendorArchive() {
        group = 'images'
        stamp.convention(destination.map { dir -> dir.file('../.' + dir.asFile.name + '.extracted') })
        outputs.upToDateWhen { destination.get().asFile.isDirectory() }
    }

    @TaskAction
    void extract() {
        File source = VendorArchives.requirePlaintext(archive.get().asFile)
        File dest = destination.get().asFile
        dest.mkdirs()
        logger.lifecycle("Extracting ${source.name} into ${dest}")
        execOperations.exec {
            workingDir dest
            commandLine 'tar', 'xzf', source.absolutePath
        }
        stamp.get().asFile.text = "${source.name}\n"
    }
}
