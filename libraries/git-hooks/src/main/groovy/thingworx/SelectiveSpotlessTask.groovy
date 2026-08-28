package thingworx

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

abstract class SelectiveSpotlessTask extends DefaultTask {

    @org.gradle.api.tasks.Internal
    File rootDir

    SelectiveSpotlessTask() {
        description = 'Runs spotlessCheck only for projects with staged changes'
        group = 'verification'
        rootDir = project.rootDir
    }

    @TaskAction
    void check() {
        File root = rootDir ?: project.rootDir
        List<String> staged = VersionUtils.stagedPaths(root)
        boolean hasRelevant = staged.any { it.endsWith('.java') || it.endsWith('.gradle') || it.endsWith('.groovy') }
        if (staged.isEmpty() || !hasRelevant) {
            logger.lifecycle('spotlessCheckSelective: no staged java/gradle files, skipping')
            return
        }
        logger.lifecycle("spotlessCheckSelective: relevant staged files detected (${staged.findAll { it.endsWith('.java') || it.endsWith('.gradle') }.join(', ')}), spotless will be checked via spotlessCheck task")
        // Actual check is done by spotlessCheck task (now selective via onlyIf). This task just gates.
    }
}
