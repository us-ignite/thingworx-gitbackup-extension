package thingworx.helm

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject

/**
 * Fails with a clear message unless every {@code kind/name} exists ({@code kubectl get}).
 * Namespaced resources are looked up in {@code namespace}.
 */
@DisableCachingByDefault(because = 'Checks cluster state')
abstract class RequireKubernetesResources extends DefaultTask {
    /** Entries such as {@code secret/thingworx-license} or {@code namespace/thingworx}. */
    @Input
    abstract ListProperty<String> getResources()

    @Input
    @Optional
    abstract Property<String> getNamespace()

    @Input
    @Optional
    abstract Property<String> getKubeContext()

    @Inject
    abstract ExecOperations getExecOperations()

    RequireKubernetesResources() {
        group = 'deployment'
        outputs.upToDateWhen { false }
    }

    @TaskAction
    void check() {
        resources.get().each { String resource ->
            def (kind, name) = resource.split('/', 2)
            List<String> command = ['kubectl']
            if (kubeContext.getOrElse('')) command += ['--context', kubeContext.get()]
            command += ['get', kind, name]
            if (kind != 'namespace' && namespace.isPresent()) command += ['-n', namespace.get()]
            logger.lifecycle("$command")
            def result = execOperations.exec {
                commandLine command
                ignoreExitValue = true
            }
            if (result.exitValue != 0) {
                throw new GradleException(kind == 'namespace'
                        ? "Kubernetes namespace '${name}' does not exist"
                        : "Required ${kind} '${name}' is missing from namespace '${namespace.getOrNull()}'")
            }
        }
    }
}
