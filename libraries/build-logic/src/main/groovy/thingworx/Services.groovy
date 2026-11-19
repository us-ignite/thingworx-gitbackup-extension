package thingworx

import org.gradle.api.file.FileSystemOperations
import org.gradle.process.ExecOperations

import javax.inject.Inject

/** Injected Gradle services for script plugins: {@code objects.newInstance(Services)}. */
abstract class Services {
    @Inject
    abstract FileSystemOperations getFileSystem()

    @Inject
    abstract ExecOperations getExec()
}
