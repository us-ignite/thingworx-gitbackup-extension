package thingworx.dev

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Writes the runtime licence from TWX_LICENSE_B64 when it is missing (never baked into images). */
@DisableCachingByDefault(because = 'Writes a secret from the environment')
abstract class MaterializeLicense extends DefaultTask {
    @Internal
    abstract RegularFileProperty getLicenseFile()

    MaterializeLicense() {
        group = 'development'
        outputs.upToDateWhen { false }
    }

    @TaskAction
    void materialize() {
        File destination = licenseFile.get().asFile
        if (destination.isFile() && destination.length() > 0) {
            logger.lifecycle("license present: ${destination}")
            return
        }
        String encoded = System.getenv('TWX_LICENSE_B64')
        if (!encoded) throw new GradleException('no vendor/license.bin and TWX_LICENSE_B64 is unset')
        destination.parentFile.mkdirs()
        destination.bytes = Base64.mimeDecoder.decode(encoded)
        logger.lifecycle("wrote ${destination} from TWX_LICENSE_B64")
    }
}
