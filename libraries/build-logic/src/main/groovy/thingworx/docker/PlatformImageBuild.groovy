package thingworx.docker

import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.work.DisableCachingByDefault

/**
 * Builds the four ThingWorx platform images for one version / JDK variant and tags them under
 * ghcr.io/us-ignite/thingworx. Source tags are derived from the vendor build.env after the build.
 */
@DisableCachingByDefault(because = 'Produces Docker images, not files')
abstract class PlatformImageBuild extends VendorBuildScript {
    static final List<String> IMAGES = ['base', 'platform-base', 'platform-postgres', 'postgres-init']
    static final String REGISTRY = 'ghcr.io/us-ignite/thingworx'

    @Input
    abstract Property<String> getJavaArchive()

    @Input
    abstract Property<String> getTomcatVersion()

    @Input
    abstract Property<String> getPlatformVersion()

    @Input
    abstract Property<String> getVariant()

    /** The default variant also receives the unsuffixed tags. */
    @Input
    abstract Property<Boolean> getDefaultVariant()

    static List<String> targetTags(String platformVersion, String variant, boolean defaultVariant) {
        IMAGES.collectMany { name ->
            def prefix = "${REGISTRY}/${name}:${platformVersion}"
            defaultVariant ? ["${prefix}-${variant}".toString(), prefix.toString()] : ["${prefix}-${variant}".toString()]
        }
    }

    /** Must match the vendor build.sh tag calculation (Java version embedded in the archive name). */
    static String vendorJavaVersion(String archiveName) {
        if (archiveName in ['graalvm-ce-java11-linux-amd64-22.3.3.tar.gz', 'corretto-11-x64-linux-jdk.tar.gz']) return '11.0.22'
        def update = (archiveName =~ /[0-9]+u[0-9]+/)
        if (update.find()) return update.group()
        def semantic = (archiveName =~ /[0-9]+(\.[0-9]+){2}/)
        if (semantic.find()) return semantic.group()
        throw new GradleException("Unable to determine Java version from archive: ${archiveName}")
    }

    @Override
    protected void beforeBuild(File dir) {
        // Licences are supplied at runtime as read-only volume mounts, never baked into images.
        def licences = []
        new File(dir, 'staging').eachFileRecurse { if (it.name in ['license.bin', 'trial.bin']) licences << it }
        if (!licences.empty || (new File(dir, 'build.env').text =~ /(?m)^LICENSE_FILE=.+/).find()) {
            throw new GradleException('Remove runtime licence files and LICENSE_FILE from the platform build context; mount the licence at runtime instead.')
        }
    }

    @Override
    protected Map<String, String> tags(File dir) {
        String env = new File(dir, 'build.env').text
        def platform = (env =~ /(?m)^PLATFORM_VERSION=([^\r\n]+)/)
        if (!platform.find()) throw new GradleException('Missing PLATFORM_VERSION in vendor build.env')
        String vendorPlatform = platform.group(1).trim()
        def postgres = (env =~ /(?m)^PLATFORM_POSTGRES_VERSION=([^\r\n]+)/)
        if (!postgres.find()) throw new GradleException('Missing PLATFORM_POSTGRES_VERSION in vendor build.env')
        String vendorPostgres = postgres.group(1).trim().replace('${PLATFORM_VERSION}', vendorPlatform)
        if (!(vendorPostgres ==~ /[0-9][0-9A-Za-z.-]*/)) throw new GradleException("Unsupported vendor platform version: ${vendorPostgres}")

        String tagVersion = "java${vendorJavaVersion(javaArchive.get())}-tomcat${tomcatVersion.get()}"
        String pgTag = "${tagVersion}-platform${vendorPostgres}"
        def sources = [
            'base'             : "thingworx/base:${tagVersion}",
            'platform-base'    : "thingworx/tw-platform-base:${tagVersion}",
            'platform-postgres': "thingworx/platform-postgres:${pgTag}",
            'postgres-init'    : "thingworx/postgresql-init-twx:${pgTag}",
        ]
        Map<String, String> result = [:]
        IMAGES.each { name -> result["${REGISTRY}/${name}:${platformVersion.get()}-${variant.get()}".toString()] = sources[name].toString() }
        if (defaultVariant.get()) {
            IMAGES.each { name -> result["${REGISTRY}/${name}:${platformVersion.get()}".toString()] = sources[name].toString() }
        }
        result
    }
}
