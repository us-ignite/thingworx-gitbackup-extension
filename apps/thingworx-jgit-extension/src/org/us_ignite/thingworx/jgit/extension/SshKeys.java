package org.us_ignite.thingworx.jgit.extension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Iterator;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.util.security.SecurityUtils;

/** SSH key generation, parsing and fingerprints, using the sshd library JGit's SSH uses. */
final class SshKeys {
    /** A parsed key with the public details shown to users. */
    record Parsed(KeyPair keyPair, String fingerprint, String publicKey) {}

    private SshKeys() {}

    /**
     * Generates a new key: Ed25519 when the platform's crypto providers support it, otherwise ECDSA
     * P-256. Both are accepted by GitHub, GitLab and Gitea.
     *
     * @return the OpenSSH private key text and its public details
     */
    static Generated generate(String comment) throws Exception {
        KeyPair keyPair =
                SecurityUtils.isEDDSACurveSupported()
                        ? KeyUtils.generateKeyPair(KeyPairProvider.SSH_ED25519, 256)
                        : KeyUtils.generateKeyPair(KeyPairProvider.ECDSA_SHA2_NISTP256, 256);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // The key is stored as a ThingWorx PASSWORD value, which the platform encrypts at rest,
        // so it is written without its own passphrase.
        OpenSSHKeyPairResourceWriter.INSTANCE.writePrivateKey(keyPair, comment, null, out);
        String privateKey = out.toString(StandardCharsets.UTF_8);
        return new Generated(privateKey, describe(keyPair, comment));
    }

    record Generated(String privateKey, Parsed parsed) {}

    /**
     * Parses an OpenSSH or PEM private key.
     *
     * @param passphrase blank when the key is not encrypted
     * @throws IllegalArgumentException when the text is not a single usable private key or the
     *     passphrase is wrong
     */
    static Parsed parse(String privateKey, String passphrase, String comment) {
        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalArgumentException("SshPrivateKey is required.");
        }
        try {
            Iterable<KeyPair> pairs =
                    SecurityUtils.loadKeyPairIdentities(
                            null,
                            NamedResource.ofName("pasted-ssh-key"),
                            new ByteArrayInputStream(
                                    privateKey.trim().getBytes(StandardCharsets.UTF_8)),
                            passphrase == null || passphrase.isEmpty()
                                    ? FilePasswordProvider.EMPTY
                                    : FilePasswordProvider.of(passphrase));
            Iterator<KeyPair> it = pairs == null ? null : pairs.iterator();
            if (it == null || !it.hasNext()) {
                throw new IllegalArgumentException("No private key was found in SshPrivateKey.");
            }
            KeyPair keyPair = it.next();
            if (it.hasNext()) {
                throw new IllegalArgumentException("SshPrivateKey must contain a single key.");
            }
            return describe(keyPair, comment);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "The SSH private key could not be read. Check the key text and passphrase.", e);
        }
    }

    private static Parsed describe(KeyPair keyPair, String comment) {
        PublicKey publicKey = keyPair.getPublic();
        String entry = PublicKeyEntry.toString(publicKey);
        if (comment != null && !comment.isBlank())
            entry += " " + comment.trim().replaceAll("\\s+", "_");
        return new Parsed(keyPair, KeyUtils.getFingerPrint(publicKey), entry);
    }
}
