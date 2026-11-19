package thingworx.docker

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations

import java.io.ByteArrayOutputStream

/** Docker CLI calls shared by the image task types. */
class DockerCli {
    static boolean imagesExist(ExecOperations exec, Collection<String> images) {
        if (images.isEmpty()) return true
        exec.exec {
            commandLine(['docker', 'image', 'inspect'] + images.collect { it.toString() })
            standardOutput = new ByteArrayOutputStream()
            errorOutput = new ByteArrayOutputStream()
            ignoreExitValue = true
        }.exitValue == 0
    }

    /** Retried: parallel image builds contend on dockerd and a single tag can fail transiently. */
    static void tag(ExecOperations exec, Logger logger, String source, String target) {
        for (int attempt = 1; ; attempt++) {
            def result = exec.exec {
                commandLine 'docker', 'tag', source, target
                ignoreExitValue = true
            }
            if (result.exitValue == 0) return
            if (attempt >= 3) throw new GradleException("docker tag failed after 3 attempts: ${source} -> ${target}")
            logger.lifecycle("docker tag failed (attempt ${attempt}/3), retrying in 5s: ${source} -> ${target}")
            Thread.sleep(5000)
        }
    }

    /** Runs a command, prefixing each output line, retrying up to {@code attempts} times. */
    static void runWithRetries(ExecOperations exec, Logger logger, File workDir, List<String> command,
                               int attempts, String prefix, String what) {
        for (int attempt = 1; attempt <= attempts; attempt++) {
            if (attempt > 1) logger.lifecycle("Retrying ${what} attempt ${attempt}/${attempts} ...")
            def output = new PrefixingOutputStream(logger, prefix)
            def result = exec.exec {
                workingDir workDir
                commandLine command
                standardOutput = output
                errorOutput = output
                ignoreExitValue = true
            }
            output.close()
            if (result.exitValue == 0) return
            if (attempt < attempts) Thread.sleep(5000)
        }
        throw new GradleException("${what} failed after ${attempts} attempts")
    }

    /** Line-buffers process output into the Gradle log with a prefix. */
    static class PrefixingOutputStream extends OutputStream {
        private final Logger logger
        private final String prefix
        private final ByteArrayOutputStream line = new ByteArrayOutputStream()

        PrefixingOutputStream(Logger logger, String prefix) {
            this.logger = logger
            this.prefix = prefix
        }

        @Override
        synchronized void write(int b) {
            if (b == '\n' as char) flushLine() else line.write(b)
        }

        private void flushLine() {
            logger.lifecycle("${prefix}${line.toString('UTF-8')}")
            line.reset()
        }

        @Override
        synchronized void close() {
            if (line.size() > 0) flushLine()
        }
    }
}
