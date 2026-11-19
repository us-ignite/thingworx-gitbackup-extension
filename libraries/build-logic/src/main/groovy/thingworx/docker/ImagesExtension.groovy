package thingworx.docker

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

/** {@code thingworxImages { }}: shared locations for the image projects. */
abstract class ImagesExtension {
    /** images/build: vendor contexts, docker contexts, JDK cache and stamps (shared by all image projects). */
    abstract DirectoryProperty getStagingRoot()

    /** The repository vendor/ directory (git-filter-decrypted PTC archives). */
    abstract DirectoryProperty getVendorDir()

    Provider<Directory> context(String image, String twxVersion) {
        stagingRoot.dir("vendor-extracted/${image}/${twxVersion}")
    }

    Provider<RegularFile> vendorFile(String name) {
        vendorDir.file(name)
    }
}
