package org.us_ignite.thingworx.jgit.extension;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.Base64;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.signature.BuiltinSignatures;
import org.apache.sshd.common.signature.Signature;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.UnsupportedSigningFormatException;
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.lib.GpgSignature;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Signer;
import org.eclipse.jgit.transport.CredentialsProvider;

/**
 * JGit signer that signs commits in the SSH signature format with an SSH key stored in ThingWorx.
 *
 * <p>JGit's own {@code SshSigner} reads keys from files or an ssh-agent. This signer instead takes
 * the stored OpenSSH private key text, parses it in memory and never writes it to disk. The output
 * is the armored {@code -----BEGIN SSH SIGNATURE-----} blob described in OpenSSH's PROTOCOL.sshsig,
 * with namespace {@code git} and hash {@code sha512}, which GitHub, GitLab and Gitea verify.
 */
public class StoredKeySshSigner implements Signer {

    private static final byte[] MAGIC = {'S', 'S', 'H', 'S', 'I', 'G'};
    private static final int VERSION = 1;
    static final String NAMESPACE = "git";
    private static final String HASH = "sha512";
    private static final String ARMOR_HEAD = "-----BEGIN SSH SIGNATURE-----";
    private static final String ARMOR_END = "-----END SSH SIGNATURE-----";
    private static final int LINE_LENGTH = 70;

    private KeyPair keyPair;
    private final String fingerprint;
    private final String publicKey;

    /**
     * Creates a signer from a stored private key.
     *
     * @param privateKey OpenSSH or PEM private key text
     * @param passphrase blank when the key is not encrypted
     * @throws IllegalArgumentException when the key can't be read or its type can't sign
     */
    public StoredKeySshSigner(String privateKey, String passphrase) {
        SshKeys.Parsed parsed = SshKeys.parse(privateKey, passphrase, null);
        this.keyPair = parsed.keyPair();
        this.fingerprint = parsed.fingerprint();
        this.publicKey = parsed.publicKey();
        signatureAlgorithm(keyPair.getPublic());
    }

    /** The key's {@code SHA256:} fingerprint. */
    public String getFingerprint() {
        return fingerprint;
    }

    /** The OpenSSH public key line, as added to a Git host as a signing key. */
    public String getPublicKey() {
        return publicKey;
    }

    /** Drops the reference to the private key so it can be collected. */
    public void clearSensitiveData() {
        keyPair = null;
    }

    @Override
    public GpgSignature sign(
            Repository repository,
            GpgConfig config,
            byte[] data,
            PersonIdent committer,
            String signingKey,
            CredentialsProvider credentialsProvider)
            throws CanceledException, UnsupportedSigningFormatException {
        KeyPair pair = keyPair;
        if (pair == null) {
            throw new IllegalStateException("The SSH signing key has been cleared.");
        }
        try {
            return new GpgSignature(sign(pair, data));
        } catch (GeneralSecurityException e) {
            throw new UnsupportedSigningFormatException(
                    "Failed to sign the commit with the stored SSH key: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean canLocateSigningKey(
            Repository repository,
            GpgConfig config,
            PersonIdent committer,
            String signingKey,
            CredentialsProvider credentialsProvider) {
        return keyPair != null;
    }

    /** Builds the armored SSHSIG signature of {@code data}. Package-visible for tests. */
    static byte[] sign(KeyPair pair, byte[] data) throws GeneralSecurityException {
        PublicKey publicKey = pair.getPublic();
        String algorithm = signatureAlgorithm(publicKey);
        byte[] hash = MessageDigest.getInstance("SHA-512").digest(data);

        Buffer toSign = new ByteArrayBuffer();
        toSign.putRawBytes(MAGIC);
        toSign.putString(NAMESPACE);
        toSign.putUInt(0); // reserved: empty string
        toSign.putString(HASH);
        toSign.putBytes(hash);

        BuiltinSignatures factory = BuiltinSignatures.fromFactoryName(algorithm);
        if (factory == null || !factory.isSupported()) {
            throw new GeneralSecurityException(
                    "This server can't create " + algorithm + " signatures.");
        }
        byte[] raw;
        try {
            Signature signer = factory.create();
            signer.initSigner(null, pair.getPrivate());
            signer.update(null, toSign.getCompactData());
            raw = signer.sign(null);
        } catch (GeneralSecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralSecurityException(e.getMessage(), e);
        }

        Buffer blob = new ByteArrayBuffer();
        blob.putRawBytes(MAGIC);
        blob.putUInt(VERSION);
        blob.putPublicKey(publicKey);
        blob.putString(NAMESPACE);
        blob.putUInt(0); // reserved
        blob.putString(HASH);
        Buffer sig = new ByteArrayBuffer();
        sig.putString(factory.getName());
        sig.putBytes(raw);
        blob.putBytes(sig.getCompactData());
        return armor(blob.getCompactData());
    }

    /**
     * The SSH signature algorithm for a key: Ed25519 and ECDSA keys sign with their own type, RSA
     * keys with {@code rsa-sha2-512}. Other types, such as DSA, are refused.
     */
    static String signatureAlgorithm(PublicKey publicKey) {
        String type = KeyUtils.getKeyType(publicKey);
        if (type == null) {
            throw new IllegalArgumentException("The SSH key type is not recognized.");
        }
        if (type.equals(KeyPairProvider.SSH_ED25519)
                || type.equals(KeyPairProvider.ECDSA_SHA2_NISTP256)
                || type.equals(KeyPairProvider.ECDSA_SHA2_NISTP384)
                || type.equals(KeyPairProvider.ECDSA_SHA2_NISTP521)) {
            return type;
        }
        if (type.equals(KeyPairProvider.SSH_RSA)) {
            return KeyUtils.RSA_SHA512_KEY_TYPE_ALIAS;
        }
        throw new IllegalArgumentException(
                "SSH keys of type "
                        + type
                        + " can't sign commits. Use an Ed25519, ECDSA or RSA key.");
    }

    private static byte[] armor(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String encoded = Base64.getEncoder().encodeToString(data);
        StringBuilder text = new StringBuilder(ARMOR_HEAD).append('\n');
        for (int i = 0; i < encoded.length(); i += LINE_LENGTH) {
            text.append(encoded, i, Math.min(encoded.length(), i + LINE_LENGTH)).append('\n');
        }
        text.append(ARMOR_END).append('\n');
        out.writeBytes(text.toString().getBytes(StandardCharsets.US_ASCII));
        return out.toByteArray();
    }
}
