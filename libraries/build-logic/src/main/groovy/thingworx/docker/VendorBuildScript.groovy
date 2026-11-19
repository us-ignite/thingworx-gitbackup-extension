package thingworx.docker

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject

/**
 * Runs a PTC vendor {@code build.sh} in an extracted build context: rewrites {@code build.env}
 * keys, applies the repo's idempotent text patches, runs the script with retries, then tags the
 * produced images. With {@code stamp} set, the task is up to date while its inputs are unchanged
 * and every {@code expectedImages} tag still exists locally.
 */
@DisableCachingByDefault(because = 'Produces Docker images, not files')
abstract class VendorBuildScript extends DefaultTask {
    @Internal
    abstract DirectoryProperty getWorkDir()

    @Input
    abstract ListProperty<String> getArguments()

    /** {@code KEY=value} lines rewritten in {@code build.env}. */
    @Input
    abstract MapProperty<String, String> getEnvOverrides()

    /** Literal text replacements in {@code build.env} (e.g. vendor placeholders). */
    @Input
    abstract MapProperty<String, String> getEnvPlaceholders()

    /** Relative path -> (literal -> replacement); each replacement must be idempotent. */
    @Input
    abstract MapProperty<String, Map<String, String>> getFilePatches()

    /** Files that must exist in the context before the script runs. */
    @Input
    abstract ListProperty<String> getRequiredFiles()

    /** Target tag -> source tag, applied after a successful build (targets are unique). */
    @Input
    abstract MapProperty<String, String> getImageTags()

    @Input
    abstract ListProperty<String> getExpectedImages()

    @Input
    abstract Property<String> getDisplayName()

    @Input
    abstract Property<Integer> getAttempts()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getContextFiles()

    @OutputFile
    @Optional
    abstract RegularFileProperty getStamp()

    @Inject
    abstract ExecOperations getExecOperations()

    VendorBuildScript() {
        group = 'images'
        attempts.convention(3)
        arguments.convention([])
        envPlaceholders.convention([:])
        filePatches.convention([:])
        requiredFiles.convention([])
        imageTags.convention([:])
        expectedImages.convention([])
        outputs.upToDateWhen { t ->
            stamp.isPresent() && DockerCli.imagesExist(execOperations, expectedImages.get())
        }
    }

    @TaskAction
    void build() {
        File dir = workDir.get().asFile
        requiredFiles.get().each { path ->
            File required = new File(dir, path)
            if (!required.isFile()) throw new org.gradle.api.GradleException("Missing required file for ${displayName.get()}: ${required}")
        }
        rewriteBuildEnv(new File(dir, 'build.env'))
        patchHostNetwork(new File(dir, 'build.sh'))
        filePatches.get().each { path, replacements ->
            File file = new File(dir, path)
            String text = file.text
            replacements.each { from, to -> text = text.replace(from, to) }
            file.text = text
        }
        beforeBuild(dir)

        logger.lifecycle("Building ${displayName.get()}...")
        DockerCli.runWithRetries(execOperations, logger, dir, ['bash', 'build.sh'] + arguments.get(),
                attempts.get(), '  [build] ', "build.sh for ${displayName.get()}")

        tags(dir).each { target, source -> DockerCli.tag(execOperations, logger, source, target) }
        if (!DockerCli.imagesExist(execOperations, expectedImages.get())) {
            throw new org.gradle.api.GradleException("Expected image tags are missing after building ${displayName.get()}")
        }
        if (stamp.isPresent()) {
            File marker = stamp.get().asFile
            marker.parentFile.mkdirs()
            marker.text = "Successful build for ${displayName.get()}\n"
        }
        logger.lifecycle("Done building ${displayName.get()}")
    }

    protected void rewriteBuildEnv(File buildEnv) {
        if (!buildEnv.exists()) return
        String text = buildEnv.text
        envPlaceholders.get().each { from, to -> text = text.replace(from, to) }
        envOverrides.get().each { key, value ->
            text = text.replaceAll(/(?m)^${java.util.regex.Pattern.quote(key)}=.*/, java.util.regex.Matcher.quoteReplacement("${key}=${value}"))
        }
        buildEnv.text = text
    }

    /** Same effect as the former sed: strip existing flags, then add them once to each build. */
    static void patchHostNetwork(File buildSh) {
        buildSh.text = buildSh.text
                .replace(' --network=host', '')
                .replace('--pull=false', '')
                .replace('docker build', 'docker build --pull=false --network=host')
    }

    /** Hook for subclasses that must validate the context before building. */
    protected void beforeBuild(File dir) {}

    /** Target tag -> source tag; subclasses may derive tags from the built context. */
    protected Map<String, String> tags(File dir) {
        imageTags.get()
    }
}
