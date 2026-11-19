package thingworx.docker

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject

/** Fails unless every listed image exists locally (images provided outside the build, e.g. CI cache). */
@DisableCachingByDefault(because = 'Checks external state')
abstract class RequireDockerImages extends DefaultTask {
    @Input
    abstract ListProperty<String> getImages()

    @Inject
    abstract ExecOperations getExecOperations()

    RequireDockerImages() {
        group = 'verification'
        outputs.upToDateWhen { false }
    }

    @TaskAction
    void check() {
        def missing = images.get().findAll { !DockerCli.imagesExist(execOperations, [it]) }
        if (missing) throw new GradleException("-PuseExistingImages set but images are missing locally: ${missing.join(', ')}")
    }
}
