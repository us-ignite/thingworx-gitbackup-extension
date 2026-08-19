package org.us_ignite.thingworx.jgit.extension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Arrays;
import java.util.Iterator;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.lib.GpgSignature;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Signer;
import org.eclipse.jgit.transport.CredentialsProvider;

/**
 * JGit signer that reads an ASCII-armored private key supplied by the ThingWorx user.
 *
 * <p>The key is held only in memory and can be explicitly cleared after signing. This class never
 * writes the private key to the repository or to a file.
 */
public class PastedKeyGpgSigner implements Signer {

    private final byte[] privateKeyData;
    private final char[] passphrase;

    /** Creates a signer from an armored private key and its passphrase. */
    public PastedKeyGpgSigner(String privateKeyArmored, String passphrase) {
        this.privateKeyData = privateKeyArmored.getBytes(StandardCharsets.UTF_8);
        this.passphrase = passphrase != null ? passphrase.toCharArray() : new char[0];
    }

    /** Overwrites the in-memory key and passphrase buffers. */
    public void clearSensitiveData() {
        Arrays.fill(privateKeyData, (byte) 0);
        Arrays.fill(passphrase, '\0');
    }

    @Override
    /** Signs commit data with the matching signing key and returns an armored GPG signature. */
    public GpgSignature sign(
            Repository repository,
            GpgConfig config,
            byte[] data,
            PersonIdent committer,
            String signingKey,
            CredentialsProvider credentialsProvider)
            throws CanceledException {
        try {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            PGPSecretKey secretKey = findSecretKey(privateKeyData, signingKey);
            if (secretKey == null) {
                throw new RuntimeException("No suitable PGP secret key found in the provided key data");
            }

            PGPPrivateKey privateKey = secretKey.extractPrivateKey(
                    new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(passphrase));

            int algorithm = secretKey.getPublicKey().getAlgorithm();

            PGPSignatureGenerator sigGen = new PGPSignatureGenerator(
                    new JcaPGPContentSignerBuilder(algorithm, PGPUtil.SHA256).setProvider("BC"));

            sigGen.init(PGPSignature.BINARY_DOCUMENT, privateKey);

            PGPSignatureSubpacketGenerator subGen = new PGPSignatureSubpacketGenerator();
            String userId = committer.getName() + " <" + committer.getEmailAddress() + ">";
            subGen.addSignerUserID(false, userId);
            sigGen.setHashedSubpackets(subGen.generate());

            sigGen.update(data);
            PGPSignature signature = sigGen.generate();

            ByteArrayOutputStream sigOut = new ByteArrayOutputStream();
            try (OutputStream armoredOut = new ArmoredOutputStream(sigOut)) {
                signature.encode(armoredOut);
            }

            byte[] signatureBytes = sigOut.toByteArray();
            return new GpgSignature(signatureBytes);

        } catch (PGPException | IOException e) {
            throw new RuntimeException("Failed to sign commit with pasted GPG key", e);
        }
    }

    @Override
    /** Reports whether the supplied key material contains the requested signing key. */
    public boolean canLocateSigningKey(
            Repository repository,
            GpgConfig config,
            PersonIdent committer,
            String signingKey,
            CredentialsProvider credentialsProvider)
            throws CanceledException {
        try {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            PGPSecretKey key = findSecretKey(privateKeyData, signingKey);
            return key != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the fingerprint of the first usable signing key, or {@code null} when none exists.
     */
    public String getFingerprint() throws IOException, PGPException {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        PGPSecretKey key = findSecretKey(privateKeyData, null);
        if (key == null) {
            return null;
        }
        byte[] fp = key.getPublicKey().getFingerprint();
        return Hex.toHexString(fp);
    }

    private PGPSecretKey findSecretKey(byte[] keyData, String signingKey) throws IOException, PGPException {
        try (InputStream in = PGPUtil.getDecoderStream(new ByteArrayInputStream(keyData))) {
            PGPSecretKeyRingCollection keyRings = new PGPSecretKeyRingCollection(in, new JcaKeyFingerprintCalculator());

            Iterator<PGPSecretKeyRing> ringIter = keyRings.getKeyRings();
            while (ringIter.hasNext()) {
                PGPSecretKeyRing keyRing = ringIter.next();
                Iterator<PGPSecretKey> keyIter = keyRing.getSecretKeys();
                while (keyIter.hasNext()) {
                    PGPSecretKey key = keyIter.next();
                    if (key.isSigningKey()) {
                        if (signingKey == null || signingKey.isEmpty()) {
                            return key;
                        }
                        String keyIdStr = Long.toHexString(key.getKeyID()).toLowerCase();
                        if (keyIdStr.contains(signingKey.toLowerCase())) {
                            return key;
                        }
                        byte[] fp = key.getPublicKey().getFingerprint();
                        String fingerprint = Hex.toHexString(fp);
                        if (fingerprint.contains(signingKey.toUpperCase())
                                || fingerprint.contains(signingKey.toLowerCase())) {
                            return key;
                        }
                    }
                }
            }
        }
        return null;
    }
}
