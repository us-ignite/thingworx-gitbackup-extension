package thingworx.docker

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject

/** {@code docker build} of one Dockerfile with build args and tags. Always runs (Docker caches layers). */
@DisableCachingByDefault(because = 'Produces Docker images, not files')
abstract class DockerBuild extends DefaultTask {
    @Internal
    abstract DirectoryProperty getContextDir()

    @Internal
    abstract RegularFileProperty getDockerfile()

    @Input
    abstract MapProperty<String, String> getBuildArgs()

    @Input
    abstract ListProperty<String> getTags()

    @Input
    abstract ListProperty<String> getExtraArguments()

    @Inject
    abstract ExecOperations getExecOperations()

    DockerBuild() {
        group = 'images'
        buildArgs.convention([:])
        extraArguments.convention(['--progress=plain', '--pull=false', '--network=host'])
        outputs.upToDateWhen { false }
    }

    @TaskAction
    void build() {
        List<String> command = ['docker', 'build'] + extraArguments.get()
        buildArgs.get().each { key, value -> command += ['--build-arg', "${key}=${value}".toString()] }
        tags.get().each { command += ['-t', it] }
        command += ['-f', dockerfile.get().asFile.absolutePath, contextDir.get().asFile.absolutePath]
        logger.lifecycle("Building ${tags.get().first()}...")
        DockerCli.runWithRetries(execOperations, logger, contextDir.get().asFile, command, 1, '  [docker] ',
                "docker build for ${tags.get().first()}")
    }
}
