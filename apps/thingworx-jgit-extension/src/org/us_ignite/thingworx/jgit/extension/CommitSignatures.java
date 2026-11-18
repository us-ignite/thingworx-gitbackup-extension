package org.us_ignite.thingworx.jgit.extension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;
import org.bouncycastle.bcpg.sig.IssuerFingerprint;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.util.encoders.Hex;

/**
 * Describes a commit's signature for display: its format and the key it names. It does not verify
 * the signature; the extension keeps no allowed-signers list or keyring to check it against.
 */
public final class CommitSignatures {
    /** What a signature says about itself; both fields are blank for an unsigned commit. */
    public record Description(String format, String key) {}

    private static final Description UNSIGNED = new Description("", "");
    private static final String SSH_HEAD = "-----BEGIN SSH SIGNATURE-----";
    private static final String SSH_END = "-----END SSH SIGNATURE-----";
    private static final String PGP_HEAD = "-----BEGIN PGP SIGNATURE-----";
    private static final String X509_HEAD = "-----BEGIN SIGNED MESSAGE-----";
    private static final byte[] SSH_MAGIC = {'S', 'S', 'H', 'S', 'I', 'G'};

    private CommitSignatures() {}

    /**
     * Describes the raw {@code gpgsig} header of a commit.
     *
     * @param raw the header as {@code RevCommit.getRawGpgSignature()} returns it, or {@code null}
     * @return the format ({@code openpgp}, {@code ssh}, {@code x509} or {@code unknown}) and the
     *     key: the SSH {@code SHA256:} fingerprint, or the OpenPGP fingerprint or 16-digit key ID;
     *     the key is blank when it can't be read
     */
    public static Description describe(byte[] raw) {
        if (raw == null || raw.length == 0) return UNSIGNED;
        String text = new String(raw, StandardCharsets.US_ASCII).trim();
        if (text.startsWith(SSH_HEAD)) return new Description(SigningSelection.SSH, sshKey(text));
        if (text.startsWith(PGP_HEAD))
            return new Description(SigningSelection.OPENPGP, pgpKey(raw));
        if (text.startsWith(X509_HEAD)) return new Description("x509", "");
        return new Description("unknown", "");
    }

    private static String sshKey(String armored) {
        try {
            int end = armored.indexOf(SSH_END);
            String body =
                    armored.substring(SSH_HEAD.length(), end < 0 ? armored.length() : end)
                            .replaceAll("\\s", "");
            byte[] blob = Base64.getDecoder().decode(body);
            if (blob.length < SSH_MAGIC.length
                    || !Arrays.equals(Arrays.copyOf(blob, SSH_MAGIC.length), SSH_MAGIC)) return "";
            ByteArrayBuffer buffer =
                    new ByteArrayBuffer(blob, SSH_MAGIC.length, blob.length - SSH_MAGIC.length);
            buffer.getUInt(); // version
            PublicKey key = buffer.getPublicKey();
            return KeyUtils.getFingerPrint(key);
        } catch (Exception | AssertionError e) {
            return "";
        }
    }

    private static String pgpKey(byte[] raw) {
        try (InputStream in = PGPUtil.getDecoderStream(new ByteArrayInputStream(raw))) {
            Object object = new JcaPGPObjectFactory(in).nextObject();
            if (!(object instanceof PGPSignatureList list) || list.isEmpty()) return "";
            PGPSignature signature = list.get(0);
            IssuerFingerprint issuer =
                    signature.getHashedSubPackets() == null
                            ? null
                            : signature.getHashedSubPackets().getIssuerFingerprint();
            if (issuer != null)
                return Hex.toHexString(issuer.getFingerprint()).toUpperCase(Locale.ROOT);
            long keyId = signature.getKeyID();
            return keyId == 0 ? "" : String.format(Locale.ROOT, "%016X", keyId);
        } catch (Exception e) {
            return "";
        }
    }
}
