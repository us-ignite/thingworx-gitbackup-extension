package org.us_ignite.thingworx.jgit.tests.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPairGenerator;
import java.security.Security;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;

public class GPGGenerator {
    public static String generateTestGpgPrivateKey() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        var kpg = KeyPairGenerator.getInstance("RSA", "BC");
        kpg.initialize(1024);
        var kp = kpg.generateKeyPair();

        String userId = "Test User <test@example.com>";

        var pgpKeyPair =
                new JcaPGPKeyPair(PublicKeyAlgorithmTags.RSA_SIGN, kp, new java.util.Date());

        var digestProvBuilder = new JcaPGPDigestCalculatorProviderBuilder().setProvider("BC");
        var sha1Calc = digestProvBuilder.build().get(PGPUtil.SHA1);

        var keyRingGen =
                new PGPKeyRingGenerator(
                        PGPSignature.POSITIVE_CERTIFICATION,
                        pgpKeyPair,
                        userId,
                        sha1Calc,
                        null,
                        null,
                        new JcaPGPContentSignerBuilder(
                                        pgpKeyPair.getPublicKey().getAlgorithm(), PGPUtil.SHA256)
                                .setProvider("BC"),
                        null);

        var out = new ByteArrayOutputStream();
        try (var armoredOut = new ArmoredOutputStream(out)) {
            keyRingGen.generateSecretKeyRing().encode(armoredOut);
        }
        return out.toString("UTF-8");
    }

    public static String generateTestGpgPrivateKeyWithPassphrase(String passphrase) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        var kpg = KeyPairGenerator.getInstance("RSA", "BC");
        kpg.initialize(1024);
        var kp = kpg.generateKeyPair();

        String userId = "Test User <test@example.com>";

        var pgpKeyPair =
                new JcaPGPKeyPair(PublicKeyAlgorithmTags.RSA_SIGN, kp, new java.util.Date());

        var digestProvBuilder = new JcaPGPDigestCalculatorProviderBuilder().setProvider("BC");
        var sha1Calc = digestProvBuilder.build().get(PGPUtil.SHA1);
        var sha256Calc = digestProvBuilder.build().get(PGPUtil.SHA256);

        var encryptorBuilder =
                new org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder(
                                org.bouncycastle.openpgp.PGPEncryptedData.AES_256, sha256Calc)
                        .setProvider("BC");

        var keyRingGen =
                new PGPKeyRingGenerator(
                        PGPSignature.POSITIVE_CERTIFICATION,
                        pgpKeyPair,
                        userId,
                        sha1Calc,
                        null,
                        null,
                        new JcaPGPContentSignerBuilder(
                                        pgpKeyPair.getPublicKey().getAlgorithm(), PGPUtil.SHA256)
                                .setProvider("BC"),
                        encryptorBuilder.build(passphrase.toCharArray()));

        var out = new ByteArrayOutputStream();
        try (var armoredOut = new ArmoredOutputStream(out)) {
            keyRingGen.generateSecretKeyRing().encode(armoredOut);
        }
        return out.toString("UTF-8");
    }

    public static String publicKeyFromPrivateKey(String privateKeyArmored) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        var keyRings =
                new PGPSecretKeyRingCollection(
                        PGPUtil.getDecoderStream(
                                new ByteArrayInputStream(privateKeyArmored.getBytes("UTF-8"))),
                        new JcaKeyFingerprintCalculator());
        PGPSecretKeyRing keyRing = keyRings.getKeyRings().next();
        var out = new ByteArrayOutputStream();
        try (var armoredOut = new ArmoredOutputStream(out)) {
            keyRing.getPublicKey().encode(armoredOut);
        }
        return out.toString("UTF-8");
    }
}
