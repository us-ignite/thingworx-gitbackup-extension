package thingworx.dev

import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Copies the extension zip with a patch version one above both the built and the installed
 * version, so ThingWorx always accepts it as an upgrade. Writes the chosen version to
 * {@code versionFile}.
 */
@DisableCachingByDefault(because = 'Depends on the version installed on the development stack')
abstract class MakeDevPackage extends DevStackTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getSource()

    @OutputFile
    abstract RegularFileProperty getDestination()

    @OutputFile
    abstract RegularFileProperty getVersionFile()

    protected static List<BigInteger> parse(Object value) {
        if (!(value instanceof String) || !(value ==~ /\d+\.\d+\.\d+/)) throw new GradleException("Expected a three-part extension version, got ${value}")
        value.tokenize('.').collect { it as BigInteger }
    }

    @TaskAction
    void make() {
        def conn = DevHttp.open(twx('ExtensionPackages/JGitExtension'), twxAuth())
        def installed = conn.responseCode == 404 ? null : parse(new JsonSlurper().parse(conn.inputStream).packageVersion)
        def zip = new ZipFile(source.get().asFile)
        String version
        try {
            String metadata = zip.getInputStream(zip.getEntry('metadata.xml')).getText('UTF-8')
            def matcher = metadata =~ /(<ExtensionPackage\b[^>]*\bpackageVersion=")([^"]+)(")/
            if (matcher.count != 1) throw new GradleException('Expected exactly one ExtensionPackage version in metadata.xml')
            def built = parse(matcher[0][2])
            def base = installed && (0..2).findResult { i -> built[i] == installed[i] ? null : (built[i] < installed[i]) } ? installed : built
            version = "${base[0]}.${base[1]}.${base[2] + 1}"
            String updated = metadata.substring(0, matcher.start(2)) + version + metadata.substring(matcher.end(2))
            destination.get().asFile.withOutputStream { out ->
                def output = new ZipOutputStream(out)
                try {
                    zip.entries().each { entry ->
                        def copy = new ZipEntry(entry.name)
                        copy.time = entry.time
                        output.putNextEntry(copy)
                        if (entry.name == 'metadata.xml') output.write(updated.getBytes('UTF-8'))
                        else zip.getInputStream(entry).withCloseable { it.transferTo(output) }
                        output.closeEntry()
                    }
                } finally {
                    output.close()
                }
            }
        } finally {
            zip.close()
        }
        versionFile.get().asFile.text = version
        logger.lifecycle("Development extension version: ${version}")
    }
}
