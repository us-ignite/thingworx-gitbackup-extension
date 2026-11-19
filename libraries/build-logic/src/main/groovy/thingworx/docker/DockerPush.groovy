package thingworx.docker

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject

/**
 * Pushes image tags after scanning every layer of every tag for licence files. A final-filesystem
 * export would miss licences deleted in later layers, so the scanner inspects layers and fails
 * closed: nothing is pushed unless all tags pass.
 */
@DisableCachingByDefault(because = 'Publishes to a registry')
abstract class DockerPush extends DefaultTask {
    @Input
    abstract ListProperty<String> getTags()

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getLicenceScanner()

    @Inject
    abstract ExecOperations getExecOperations()

    DockerPush() {
        group = 'publishing'
        outputs.upToDateWhen { false }
    }

    @TaskAction
    void push() {
        def scan = execOperations.exec {
            commandLine(['python3', licenceScanner.get().asFile.absolutePath] + tags.get())
            ignoreExitValue = true
        }
        if (scan.exitValue != 0) throw new org.gradle.api.GradleException('Image licence layer scan failed; no images were pushed')
        tags.get().each { tag ->
            logger.lifecycle("Pushing ${tag}...")
            execOperations.exec { commandLine 'docker', 'push', tag }
        }
    }
}
