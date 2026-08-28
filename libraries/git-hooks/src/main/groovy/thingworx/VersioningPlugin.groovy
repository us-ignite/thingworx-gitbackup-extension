package thingworx

import org.gradle.api.Plugin
import org.gradle.api.Project

class VersioningPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.tasks.register('prepareCommitMsg', PrepareCommitMsgTask) { t ->
            t.rootDir = project.rootDir
            t.dryRun = project.hasProperty('dryRun') ? project.property('dryRun').toString() == 'true' : false
            if (project.hasProperty('commitMsgFile')) t.commitMsgFile = project.property('commitMsgFile').toString()
        }
        project.tasks.register('validateVersioning', ValidateVersioningTask) { t ->
            t.rootDir = project.rootDir
            if (project.hasProperty('range')) t.range = project.property('range').toString()
        }
        project.tasks.register('spotlessCheckSelective', SelectiveSpotlessTask) { t ->
            t.rootDir = project.rootDir
        }

        // Make spotlessCheck selective - skip if no staged java/gradle changes
        project.gradle.taskGraph.whenReady { graph ->
            // configure after task graph ready, but onlyIf is evaluated at execution
        }
        // Configure all spotlessCheck tasks to be skipped when no relevant staged files
        File root = project.rootDir
        project.allprojects { p ->
            p.tasks.configureEach { t ->
                if (t.name == 'spotlessCheck') {
                    t.onlyIf {
                        List<String> staged = VersionUtils.stagedPaths(root)
                        boolean hasRelevant = staged.any { it.endsWith('.java') || it.endsWith('.gradle') || it.endsWith('.groovy') }
                        if (!hasRelevant) {
                            // Use task logger, not project.logger for config-cache compatibility
                            t.logger.lifecycle("spotlessCheck: no staged java/gradle files, skipping")
                            return false
                        }
                        return true
                    }
                }
            }
        }

        // Provide lazy version providers for build scripts to use (dynamic, no smudge)
        project.extensions.add('versionFromFile', { String path ->
            project.providers.fileContents(project.layout.projectDirectory.file(path)).asText.map { it.trim() }
        })
    }
}
