package thingworx.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Downloads one file. With {@code sha256} set the file is verified (and re-downloaded when a
 * cached copy does not match); without it an existing non-empty file is reused.
 */
@DisableCachingByDefault(because = 'Downloading is faster than packing and unpacking the build cache')
abstract class VerifiedDownload extends DefaultTask {
    @Input
    abstract Property<String> getUrl()

    @Input
    @Optional
    abstract Property<String> getSha256()

    @OutputFile
    abstract RegularFileProperty getOutputFile()

    VerifiedDownload() {
        group = 'build'
        outputs.upToDateWhen { valid(outputFile.get().asFile) }
    }

    @TaskAction
    void download() {
        File target = outputFile.get().asFile
        if (valid(target)) {
            logger.lifecycle("${target.name} already downloaded")
            return
        }
        target.parentFile.mkdirs()
        File partial = new File(target.parentFile, "${target.name}.part")
        try {
            logger.lifecycle("Downloading ${url.get()}")
            new URL(url.get()).openStream().withCloseable { input ->
                partial.withOutputStream { output -> input.transferTo(output) }
            }
            if (!valid(partial)) {
                throw new GradleException("SHA-256 mismatch for ${url.get()}: expected ${sha256.get()}, got ${digest(partial)}")
            }
            Files.move(partial.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } finally {
            partial.delete()
        }
    }

    protected boolean valid(File file) {
        if (!file.isFile() || file.length() == 0L) return false
        !sha256.isPresent() || digest(file) == sha256.get().toLowerCase()
    }

    static String digest(File file) {
        def md = MessageDigest.getInstance('SHA-256')
        file.withInputStream { input ->
            byte[] buffer = new byte[1 << 16]
            int read
            while ((read = input.read(buffer)) > 0) md.update(buffer, 0, read)
        }
        md.digest().encodeHex().toString()
    }
}
