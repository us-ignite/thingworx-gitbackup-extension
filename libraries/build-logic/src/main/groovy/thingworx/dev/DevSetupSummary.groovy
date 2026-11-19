package thingworx.dev

import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Verifies ThingWorx reports the installed development version, then prints the stack endpoints. */
@DisableCachingByDefault(because = 'Acts on a running development stack')
abstract class DevSetupSummary extends GiteaTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getDevVersionFile()

    @Input
    abstract Property<String> getSourceVersion()

    @TaskAction
    void summarize() {
        String devVersion = devVersionFile.get().asFile.text.trim()
        def conn = DevHttp.open(twx('ExtensionPackages/JGitExtension'), twxAuth())
        if (conn.responseCode != 200) throw new GradleException("Could not verify installed JGitExtension version (HTTP ${conn.responseCode})")
        def installed = new JsonSlurper().parse(conn.inputStream).packageVersion
        if (installed != devVersion) throw new GradleException("Expected JGitExtension ${devVersion}, but ThingWorx reports ${installed}")
        logger.lifecycle('')
        logger.lifecycle("Installed JGitExtension version: ${installed} (source version: ${sourceVersion.get()})")
        logger.lifecycle('=== Dev Setup Complete ===')
        logger.lifecycle("ThingWorx:  ${thingworxUrl.get()}  (${thingworxUser.get()} / ${thingworxPassword.get()})")
        logger.lifecycle("Gitea:      ${giteaUrl.get()}             (${giteaUser.get()} / ${giteaPassword.get()})")
        logger.lifecycle('PostgreSQL: localhost:5432                    (postgres / adminpass_P@ssw0rd!)')
        logger.lifecycle('')
        logger.lifecycle('Gitea internal URL for Git thing config: http://gitea:3000/giteauser/jgit-test-repo.git')
        logger.lifecycle('Gitea external URL for cloning:          http://localhost:3000/giteauser/jgit-test-repo.git')
        logger.lifecycle('Gitea internal SSH URL for Git thing:    ssh://git@gitea:22/giteauser/jgit-test-repo.git')
        logger.lifecycle('Gitea external SSH URL for cloning:      ssh://git@localhost:2222/giteauser/jgit-test-repo.git')
        logger.lifecycle('  (SSH: generate a key in Settings, add its public key to giteauser in Gitea, then approve the host key in Repository Settings)')
    }
}
