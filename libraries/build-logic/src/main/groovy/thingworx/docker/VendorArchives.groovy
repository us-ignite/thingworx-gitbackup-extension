package thingworx.docker

import org.gradle.api.GradleException

/** Guards for vendor archives that are stored encrypted in git (twx-file filter). */
class VendorArchives {
    /**
     * Fail fast when the working tree still holds ciphertext (checkout without GPG_PASSPHRASE)
     * instead of failing obscurely inside tar/unzip.
     */
    static File requirePlaintext(File file) {
        if (!file.isFile()) {
            throw new GradleException("Missing required vendor file: ${file} (twx-file filter + GPG_PASSPHRASE required at checkout)")
        }
        byte[] header = new byte[4]
        file.withInputStream { it.read(header) }
        boolean zip = header[0] == 0x50 && header[1] == 0x4B
        boolean gzip = (header[0] & 0xFF) == 0x1F && (header[1] & 0xFF) == 0x8B
        if (!zip && !gzip) {
            throw new GradleException("Vendor file is not plaintext (still encrypted?): ${file} - re-checkout with GPG_PASSPHRASE set and the twx-file filter registered")
        }
        file
    }
}
