package thingworx.dev

import groovy.json.JsonSlurper
import groovy.xml.XmlSlurper
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Exports the tracked GIT mashups (Mashups_GIT.Mashup.*.xml in {@code sourceDir}) from a running
 * ThingWorx and atomically replaces them in {@code outputDir}. Connection details come from
 * TWX_URL / TWX_USERNAME / TWX_PASSWORD, defaulting to the development stack.
 */
@DisableCachingByDefault(because = 'Reads from a running ThingWorx')
abstract class SaveMashups extends DevStackTask {
    @Internal
    abstract DirectoryProperty getSourceDir()

    @Internal
    abstract DirectoryProperty getOutputDir()

    /** Only this mashup (e.g. {@code Branches}); all when absent. */
    @Input
    @Optional
    abstract Property<String> getMashup()

    SaveMashups() {
        thingworxUrl.convention(System.getenv('TWX_URL') ?: 'http://localhost:8080/Thingworx')
        thingworxUser.convention(System.getenv('TWX_USERNAME') ?: 'Administrator')
        thingworxPassword.convention(System.getenv('TWX_PASSWORD') ?: 'AdminP@ssw0rd!123')
    }

    @TaskAction
    void save() {
        File source = sourceDir.get().asFile
        File output = outputDir.get().asFile
        String only = mashup.getOrNull()
        def sources = source.listFiles().findAll {
            it.name ==~ /Mashups_GIT\.Mashup\..*\.xml/ && (!only || it.name == "Mashups_GIT.Mashup.${only}.xml")
        }.sort { it.name }
        if (!sources) throw new GradleException("No GIT mashup source files found in ${source}")
        def exports = sources.collect { File file ->
            String name = file.name.substring(8, file.name.length() - 4)
            def conn = DevHttp.open(twx("Exporter/Mashups/${URLEncoder.encode(name, 'UTF-8').replace('+', '%20')}"), twxAuth(), 30000)
            conn.setRequestProperty('Accept', 'text/xml')
            byte[] data = conn.inputStream.withCloseable { it.bytes }
            def xml = new XmlSlurper().parse(new ByteArrayInputStream(data))
            def entities = xml.Mashups.Mashup
            if (xml.name() != 'Entities' || entities.size() != 1 || entities[0].@name.text() != name) throw new GradleException("Unexpected exporter response for ${name}")
            if (!(new JsonSlurper().parseText(entities[0].mashupContent.text()) instanceof Map)) throw new GradleException("Invalid mashup content for ${name}")
            if (!new String(data, 'UTF-8').contains('<mashupContent><![CDATA[')) throw new GradleException("Export for ${name} is not in the CDATA format required by the build")
            [source: file, data: data]
        }
        output.mkdirs()
        exports.each { entry ->
            File destination = new File(output, entry.source.name)
            def temporary = Files.createTempFile(output.toPath(), 'mashup-', '.tmp')
            try {
                Files.write(temporary, entry.data as byte[])
                try {
                    Files.setPosixFilePermissions(temporary, Files.getPosixFilePermissions(entry.source.toPath()))
                } catch (UnsupportedOperationException ignored) {
                }
                Files.move(temporary, destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } finally {
                Files.deleteIfExists(temporary)
            }
            logger.lifecycle("Saved ${destination}")
        }
        logger.lifecycle('Embedded TypeScript and CSS are generated from ui/src during packaging; sync edits there as well.')
    }
}
