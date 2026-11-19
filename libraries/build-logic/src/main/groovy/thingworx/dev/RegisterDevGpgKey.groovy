package thingworx.dev

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject
import java.nio.file.Files

/**
 * Ensures a passphrase-less development GPG signing key exists in ThingWorx (GIT.Utility.Thing)
 * and that its public half is registered with the Gitea user, recovering the public key from
 * ThingWorx when the local copy is gone.
 */
@DisableCachingByDefault(because = 'Acts on a running development stack')
abstract class RegisterDevGpgKey extends GiteaTask {
    @Input
    abstract Property<String> getKeyLabel()

    /** Local cache of the public key. */
    @Internal
    abstract RegularFileProperty getPublicKeyFile()

    @Inject
    abstract ExecOperations getExecOperations()

    RegisterDevGpgKey() {
        keyLabel.convention('Dev Setup Test Key')
    }

    protected Map invoke(String service, Map payload) {
        def result = DevHttp.json(twx("Things/GIT.Utility.Thing/Services/${service}"), twxAuth(), payload, DevHttp.TWX_POST_HEADERS)
        if (result.rows.size() != 1 || !result.rows[0].containsKey('Error')) throw new GradleException("${service} returned an invalid service result")
        if (result.rows[0].Error) throw new GradleException("${service} failed: ${result.rows[0].Message ?: ''}")
        result.rows[0]
    }

    protected String gpg(List<String> args, String input = null) {
        def stdout = new ByteArrayOutputStream()
        def stderr = new ByteArrayOutputStream()
        def result = execOperations.exec {
            commandLine(['gpg'] + args)
            standardInput = new ByteArrayInputStream((input ?: '').getBytes('UTF-8'))
            standardOutput = stdout
            errorOutput = stderr
            ignoreExitValue = true
        }
        if (result.exitValue != 0) throw new GradleException("GPG failed: ${stderr.toString('UTF-8')}")
        stdout.toString('UTF-8')
    }

    protected <T> T withHome(Closure<T> action) {
        File home = Files.createTempDirectory('thingworx-dev-gpg-').toFile()
        try {
            action(home)
        } finally {
            home.deleteDir()
        }
    }

    protected String fingerprint(String key) {
        withHome { File home ->
            def output = gpg(['--homedir', home.absolutePath, '--batch', '--with-colons', '--import-options', 'show-only', '--import'], key)
            def line = output.readLines().find { it.startsWith('fpr:') }
            if (!line) throw new GradleException('GPG did not return a fingerprint')
            line.split(':')[9]
        }
    }

    protected boolean registered(String fp) {
        for (int page = 1; ; page++) {
            def keys = DevHttp.json(gitea("api/v1/user/gpg_keys?page=${page}&limit=50"), giteaAuth())
            if (keys.any { (it.key_id ?: '').equalsIgnoreCase(fp[-16..-1]) }) return true
            if (keys.size() < 50) return false
        }
    }

    @TaskAction
    void register() {
        String label = keyLabel.get()
        File publicFile = publicKeyFile.get().asFile
        def existing = invoke('GpgKeyList', [:]).Result.rows.find { it.GpgKeyLabel == label }
        if (existing && registered(existing.GpgKeyFingerprint)) {
            logger.lifecycle("GPG key '${label}' is already registered with ThingWorx and Gitea.")
            return
        }
        String publicKey = null
        if (existing && publicFile.exists()) {
            String candidate = publicFile.getText('UTF-8')
            if (fingerprint(candidate).equalsIgnoreCase(existing.GpgKeyFingerprint)) publicKey = candidate
        }
        withHome { File home ->
            def common = ['--homedir', home.absolutePath, '--batch', '--pinentry-mode', 'loopback', '--passphrase', '']
            if (!existing) {
                gpg(common + ['--quick-generate-key', 'ThingWorx Dev Test <test@example.com>', 'rsa2048', 'sign', '0'])
                String privateKey = gpg(common + ['--armor', '--export-secret-keys'])
                publicKey = gpg(common + ['--armor', '--export'])
                if (!privateKey.trim() || !publicKey.trim()) throw new GradleException('GPG did not export the generated test key')
                publicFile.parentFile.mkdirs()
                publicFile.setText(publicKey, 'UTF-8')
                invoke('GpgKeyCreate', [GpgPrivateKey: privateKey, GpgKeyPassphrase: '', GpgKeyLabel: label])
            } else if (publicKey == null) {
                String privateKey = invoke('GpgKeyGet', [GpgKeyLabel: label]).Result.rows[0].GpgPrivateKey ?: ''
                if (!privateKey.contains('PRIVATE KEY BLOCK')) throw new GradleException("Cannot recover the existing dev key's public half. Delete '${label}' using GpgKeyDelete and rerun devSetup, or register its public key in Gitea manually.")
                gpg(common + ['--import'], privateKey)
                publicKey = gpg(common + ['--armor', '--export'])
                if (!fingerprint(publicKey).equalsIgnoreCase(existing.GpgKeyFingerprint)) throw new GradleException('Recovered public key does not match the ThingWorx key')
                publicFile.parentFile.mkdirs()
                publicFile.setText(publicKey, 'UTF-8')
            }
        }
        if (!registered(fingerprint(publicKey))) DevHttp.json(gitea('api/v1/user/gpg_keys'), giteaAuth(), [armored_public_key: publicKey])
        logger.lifecycle("GPG key '${label}' is registered with ThingWorx and Gitea.")
    }
}
