package thingworx.helm

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject

/**
 * Renders one Helm chart with {@code helm template}; with {@code verify} also runs
 * {@code helm lint}, {@code helm unittest} (when the plugin is installed) and kubeconform (when
 * installed) against the rendered manifests.
 */
@DisableCachingByDefault(because = 'Validation only; helm and plugins are host tools')
abstract class HelmChartCheck extends DefaultTask {
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getChart()

    @Input
    abstract Property<Boolean> getVerify()

    @Inject
    abstract ExecOperations getExecOperations()

    HelmChartCheck() {
        verify.convention(false)
    }

    @TaskAction
    void check() {
        File dir = chart.get().asFile
        String name = dir.name
        if (verify.get()) {
            logger.lifecycle("Linting chart ${name}...")
            execOperations.exec { commandLine 'helm', 'lint', dir.absolutePath }
        }
        logger.lifecycle("Templating chart ${name}...")
        def rendered = new ByteArrayOutputStream()
        execOperations.exec {
            commandLine 'helm', 'template', name, dir.absolutePath
            standardOutput = rendered
        }
        if (!verify.get()) {
            logger.lifecycle(rendered.toString('UTF-8'))
            return
        }

        if (capture(['helm', 'plugin', 'list']).contains('unittest')) {
            logger.lifecycle("Running helm unittest for ${name} (CEL validations)...")
            execOperations.exec { commandLine 'helm', 'unittest', dir.absolutePath }
        } else {
            logger.lifecycle("helm unittest plugin not installed — skipping unittest for ${name} (CEL covered by Java mock tests)")
        }

        if (onPath('kubeconform')) {
            logger.lifecycle("Running kubeconform for ${name}...")
            def result = execOperations.exec {
                commandLine 'kubeconform', '-summary', '-verbose'
                standardInput = new ByteArrayInputStream(rendered.toByteArray())
                ignoreExitValue = true
            }
            if (result.exitValue != 0) throw new GradleException("kubeconform failed for ${name}")
        } else {
            logger.lifecycle('kubeconform not installed — skipping (CEL covered by Java tests)')
        }
    }

    protected String capture(List<String> command) {
        def out = new ByteArrayOutputStream()
        execOperations.exec {
            commandLine command
            standardOutput = out
            errorOutput = new ByteArrayOutputStream()
            ignoreExitValue = true
        }
        out.toString('UTF-8')
    }

    protected static boolean onPath(String tool) {
        System.getenv('PATH')?.split(File.pathSeparator)?.any { new File(it, tool).canExecute() }
    }
}
