package thingworx.dev

import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Installs extension zips through the compose {@code extension-init} service, which reads them from
 * {@code distributionDir} (mounted at /distributions). With {@code skipIfInstalled} set, the
 * package is only installed when ThingWorx reports it missing.
 */
@DisableCachingByDefault(because = 'Acts on a running development stack')
abstract class InstallThingworxExtensions extends DevStackTask {
    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    abstract ConfigurableFileCollection getPackages()

    @Internal
    abstract DirectoryProperty getDistributionDir()

    @Internal
    abstract DirectoryProperty getComposeDir()

    @Input
    abstract Property<Boolean> getVerifyGitThing()

    /** Extension package name to look up first; installed only on HTTP 404. */
    @Input
    @Optional
    abstract Property<String> getSkipIfInstalled()

    @Inject
    abstract ExecOperations getExecOperations()

    InstallThingworxExtensions() {
        verifyGitThing.convention(false)
    }

    @TaskAction
    void install() {
        if (skipIfInstalled.isPresent()) {
            int status = DevHttp.status(twx("ExtensionPackages/${skipIfInstalled.get()}"), twxAuth())
            if (status == 200) {
                logger.lifecycle("${skipIfInstalled.get()} is already installed; leaving the existing version in place.")
                return
            }
            if (status != 404) throw new GradleException("Could not check ${skipIfInstalled.get()} installation: HTTP ${status}")
        }
        File mounted = distributionDir.get().asFile
        mounted.mkdirs()
        packages.files.sort { it.name }.each { File zip ->
            File staged = new File(mounted, zip.name)
            if (staged.canonicalFile != zip.canonicalFile) {
                Files.copy(zip.toPath(), staged.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            logger.lifecycle("Installing ${zip.name}...")
            execOperations.exec {
                workingDir composeDir.get().asFile
                commandLine 'docker', 'compose', 'run', '--rm', '--no-deps',
                        '-e', "EXTENSION_ZIP=/distributions/${zip.name}",
                        '-e', "VERIFY_GIT_THING=${verifyGitThing.get()}",
                        'extension-init'
            }
        }
    }
}
