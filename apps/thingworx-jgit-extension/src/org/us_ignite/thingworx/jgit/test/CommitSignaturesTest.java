package org.us_ignite.thingworx.jgit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Locale;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Signer;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.us_ignite.thingworx.jgit.extension.CommitSignatures;
import org.us_ignite.thingworx.jgit.extension.CommitSignatures.Description;
import org.us_ignite.thingworx.jgit.extension.PastedKeyGpgSigner;
import org.us_ignite.thingworx.jgit.extension.StoredKeySshSigner;
import org.us_ignite.thingworx.jgit.test.util.GPGGenerator;

/** The History columns for an OpenPGP-signed, an SSH-signed and an unsigned commit. */
class CommitSignaturesTest {
    @TempDir Path tmp;

    private RevCommit commit(Git git, Signer signer) throws Exception {
        var command =
                git.commit()
                        .setMessage("commit")
                        .setAllowEmpty(true)
                        .setAuthor(new PersonIdent("Test", "test@example.com"))
                        .setCommitter(new PersonIdent("Test", "test@example.com"));
        if (signer != null) command.setSign(true).setSigningKey(null).setSigner(signer);
        else command.setSign(false);
        return command.call();
    }

    @Test
    void describesEachKindOfCommit() throws Exception {
        String gpgKey = GPGGenerator.generateTestGpgPrivateKey();
        PastedKeyGpgSigner gpg = new PastedKeyGpgSigner(gpgKey, "");
        String gpgFingerprint = gpg.getFingerprint().toUpperCase(Locale.ROOT);

        KeyPair pair = KeyUtils.generateKeyPair("ecdsa-sha2-nistp256", 256);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OpenSSHKeyPairResourceWriter.INSTANCE.writePrivateKey(pair, "test", null, out);
        StoredKeySshSigner ssh = new StoredKeySshSigner(out.toString(StandardCharsets.UTF_8), "");

        try (Git git = Git.init().setDirectory(tmp.toFile()).call()) {
            Description unsigned =
                    CommitSignatures.describe(commit(git, null).getRawGpgSignature());
            assertEquals(new Description("", ""), unsigned);

            Description openpgp = CommitSignatures.describe(commit(git, gpg).getRawGpgSignature());
            assertEquals("openpgp", openpgp.format());
            assertTrue(
                    gpgFingerprint.endsWith(openpgp.key()),
                    "expected the fingerprint or key ID of " + gpgFingerprint + ", got " + openpgp);
            assertTrue(openpgp.key().length() >= 16, openpgp.key());

            Description sshSigned =
                    CommitSignatures.describe(commit(git, ssh).getRawGpgSignature());
            assertEquals(new Description("ssh", ssh.getFingerprint()), sshSigned);
        }
    }

    @Test
    void unreadableSignatureKeepsItsFormat() {
        byte[] raw =
                "-----BEGIN SSH SIGNATURE-----\nnot-base64!\n-----END SSH SIGNATURE-----\n"
                        .getBytes(StandardCharsets.US_ASCII);
        assertEquals(new Description("ssh", ""), CommitSignatures.describe(raw));
        assertEquals(
                new Description("unknown", ""),
                CommitSignatures.describe("garbage".getBytes(StandardCharsets.US_ASCII)));
    }
}
