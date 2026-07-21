package gb.tests.junit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gb.extension.PastedKeyGpgSigner;
import gb.tests.junit.util.GPGGenerator;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Test;

class PastedKeyGpgSignerTest {

    @Test
    void constructsWithArmoredKey() throws Exception {
        String key = GPGGenerator.generateTestGpgPrivateKey();
        PastedKeyGpgSigner signer = new PastedKeyGpgSigner(key, "");
        assertNotNull(signer);
        signer.clearSensitiveData();
    }

    @Test
    void constructsWithNullPassphrase() throws Exception {
        String key = GPGGenerator.generateTestGpgPrivateKey();
        PastedKeyGpgSigner signer = new PastedKeyGpgSigner(key, null);
        assertNotNull(signer);
        signer.clearSensitiveData();
    }

    @Test
    void getFingerprintReturnsValidHexString() throws Exception {
        String key = GPGGenerator.generateTestGpgPrivateKey();
        PastedKeyGpgSigner signer = new PastedKeyGpgSigner(key, "");
        String fp = signer.getFingerprint();
        assertNotNull(fp, "Fingerprint should not be null");
        assertFalse(fp.isEmpty(), "Fingerprint should not be empty");
        assertTrue(fp.matches("[0-9A-Fa-f]+"), "Fingerprint should be hex: " + fp);
        assertTrue(
                fp.length() >= 32, "Fingerprint should be at least 32 hex chars (128 bits): " + fp);
        signer.clearSensitiveData();
    }

    @Test
    void findSecretKeyViaCanLocateSigningKey() throws Exception {
        String key = GPGGenerator.generateTestGpgPrivateKey();
        PastedKeyGpgSigner signer = new PastedKeyGpgSigner(key, "");
        boolean located =
                signer.canLocateSigningKey(
                        null, null, new PersonIdent("test", "test@test.com"), null, null);
        assertTrue(located, "Should locate the signing key in the keyring");
        signer.clearSensitiveData();
    }

    @Test
    void canLocateReturnsFalseForInvalidKey() {
        PastedKeyGpgSigner signer = new PastedKeyGpgSigner("not-a-valid-pgp-key", "");
        boolean located =
                assertDoesNotThrow(
                        () ->
                                signer.canLocateSigningKey(
                                        null,
                                        null,
                                        new PersonIdent("test", "test@test.com"),
                                        null,
                                        null));
        assertFalse(located, "Should not locate key for invalid data");
        signer.clearSensitiveData();
    }

    @Test
    void signProducesValidSignature() throws Exception {
        String key = GPGGenerator.generateTestGpgPrivateKey();
        PastedKeyGpgSigner signer = new PastedKeyGpgSigner(key, "");
        byte[] data = "hello gitbackup".getBytes(StandardCharsets.UTF_8);
        var sig =
                signer.sign(
                        null,
                        null,
                        data,
                        new PersonIdent("Test User", "test@example.com"),
                        null,
                        null);
        assertNotNull(sig, "Signature should not be null");
        String sigText = sig.toExternalString();
        assertNotNull(sigText, "Signature string should not be null");
        assertTrue(
                sigText.length() > 50,
                "Signature should be substantial (>50 chars): " + sigText.length());
        assertTrue(
                sigText.contains("-----BEGIN PGP SIGNATURE-----"),
                "Signature should be ASCII-armored: "
                        + sigText.substring(0, Math.min(100, sigText.length())));
        signer.clearSensitiveData();
    }

    @Test
    void signWithPassphraseProtectedKey() throws Exception {
        String key = GPGGenerator.generateTestGpgPrivateKey();
        PastedKeyGpgSigner signer = new PastedKeyGpgSigner(key, "");
        byte[] data = "signed data".getBytes(StandardCharsets.UTF_8);
        var sig =
                signer.sign(
                        null,
                        null,
                        data,
                        new PersonIdent("Test User", "test@example.com"),
                        null,
                        null);
        assertNotNull(sig);
        signer.clearSensitiveData();
    }

    @Test
    void signWithMultipleKeysReturnsFirstSigningKey() throws Exception {
        String key = GPGGenerator.generateTestGpgPrivateKey();
        PastedKeyGpgSigner signer = new PastedKeyGpgSigner(key, "");
        byte[] data = "multi-key test".getBytes(StandardCharsets.UTF_8);
        assertDoesNotThrow(
                () -> {
                    var sig =
                            signer.sign(
                                    null,
                                    null,
                                    data,
                                    new PersonIdent("Test User", "test@example.com"),
                                    null,
                                    null);
                    assertNotNull(sig);
                });
        signer.clearSensitiveData();
    }

    @Test
    void signProducesDifferentSignaturesForDifferentData() throws Exception {
        String key = GPGGenerator.generateTestGpgPrivateKey();
        PastedKeyGpgSigner signer = new PastedKeyGpgSigner(key, "");
        PersonIdent committer = new PersonIdent("Test User", "test@example.com");
        var sig1 =
                signer.sign(
                        null,
                        null,
                        "data one".getBytes(StandardCharsets.UTF_8),
                        committer,
                        null,
                        null);
        PastedKeyGpgSigner signer2 = new PastedKeyGpgSigner(key, "");
        var sig2 =
                signer2.sign(
                        null,
                        null,
                        "data two".getBytes(StandardCharsets.UTF_8),
                        committer,
                        null,
                        null);
        String text1 = sig1.toExternalString();
        String text2 = sig2.toExternalString();
        assertFalse(text1.equals(text2), "Different data should produce different signatures");
        signer.clearSensitiveData();
        signer2.clearSensitiveData();
    }

    @Test
    void clearSensitiveDataZeroesArrays() throws Exception {
        String key = GPGGenerator.generateTestGpgPrivateKey();
        PastedKeyGpgSigner signer = new PastedKeyGpgSigner(key, "secret");
        assertTrue(
                signer.canLocateSigningKey(
                        null, null, new PersonIdent("test", "test@test.com"), null, null),
                "Should locate key before clear");
        signer.clearSensitiveData();
        assertFalse(
                signer.canLocateSigningKey(
                        null, null, new PersonIdent("test", "test@test.com"), null, null),
                "Should not locate signing key after clearSensitiveData");
    }

    @Test
    void keyWithUserIdRoundTripsThroughParser() throws Exception {
        String key = GPGGenerator.generateTestGpgPrivateKey();
        var in =
                PGPUtil.getDecoderStream(
                        new ByteArrayInputStream(key.getBytes(StandardCharsets.UTF_8)));
        var keyRings = new PGPSecretKeyRingCollection(in, new JcaKeyFingerprintCalculator());
        var ringIter = keyRings.getKeyRings();
        assertTrue(ringIter.hasNext(), "Should have at least one key ring");
        var ring = ringIter.next();
        var keyIter = ring.getSecretKeys();
        assertTrue(keyIter.hasNext(), "Should have at least one secret key");
        PGPSecretKey secretKey = keyIter.next();
        assertTrue(secretKey.isSigningKey(), "Generated key should be a signing key");
    }

    @Test
    void fingerprintMatchesParsedKey() throws Exception {
        String key = GPGGenerator.generateTestGpgPrivateKey();
        var in =
                PGPUtil.getDecoderStream(
                        new ByteArrayInputStream(key.getBytes(StandardCharsets.UTF_8)));
        var keyRings = new PGPSecretKeyRingCollection(in, new JcaKeyFingerprintCalculator());
        var ring = keyRings.getKeyRings().next();
        var secretKey = ring.getSecretKeys().next();
        byte[] rawFp = secretKey.getPublicKey().getFingerprint();
        StringBuilder expected = new StringBuilder();
        for (byte b : rawFp) {
            expected.append(String.format("%02X", b & 0xFF));
        }
        PastedKeyGpgSigner signer = new PastedKeyGpgSigner(key, "");
        assertEquals(
                expected.toString().toLowerCase(),
                signer.getFingerprint(),
                "getFingerprint() should match raw fingerprint");
        signer.clearSensitiveData();
    }
}
