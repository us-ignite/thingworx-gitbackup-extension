package org.us_ignite.thingworx.jgit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.concurrent.TimeUnit;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.signing.ssh.SshSignatureVerifier;
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.lib.GpgSignature;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.SignatureVerifier.SignatureVerification;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.us_ignite.thingworx.jgit.extension.StoredKeySshSigner;

class StoredKeySshSignerTest {

    private static final byte[] PAYLOAD =
            ("tree 4b825dc642cb6eb9a060e54bf8d69288fbee4904\n"
                            + "author Test <test@example.com> 1700000000 +0000\n"
                            + "committer Test <test@example.com> 1700000000 +0000\n\n"
                            + "signed commit\n")
                    .getBytes(StandardCharsets.UTF_8);

    @TempDir Path tmp;

    private static String privateKey(String type) throws Exception {
        if (KeyPairProvider.SSH_ED25519.equals(type)) {
            assumeTrue(SecurityUtils.isEDDSACurveSupported(), "Ed25519 not supported here");
        }
        int bits = KeyPairProvider.SSH_RSA.equals(type) ? 3072 : 256;
        KeyPair pair = KeyUtils.generateKeyPair(type, bits);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OpenSSHKeyPairResourceWriter.INSTANCE.writePrivateKey(pair, "test", null, out);
        return out.toString(StandardCharsets.UTF_8);
    }

    /** JGit only reports an SSH signature verified when its key is in an allowed-signers file. */
    private void trust(Git git, StoredKeySshSigner signer) throws Exception {
        Path allowed = tmp.resolve("allowed_signers");
        Files.writeString(allowed, "test@example.com " + signer.getPublicKey() + "\n");
        git.getRepository()
                .getConfig()
                .setString("gpg", "ssh", "allowedSignersFile", allowed.toString());
    }

    private GpgSignature sign(StoredKeySshSigner signer) throws Exception {
        return signer.sign(
                null, null, PAYLOAD, new PersonIdent("Test", "test@example.com"), null, null);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {KeyPairProvider.SSH_ED25519, "ecdsa-sha2-nistp256", KeyPairProvider.SSH_RSA})
    void signatureVerifiesWithJGit(String type) throws Exception {
        StoredKeySshSigner signer = new StoredKeySshSigner(privateKey(type), "");
        String armored = sign(signer).toExternalString();
        assertTrue(armored.startsWith("-----BEGIN SSH SIGNATURE-----\n"), armored);
        assertTrue(armored.endsWith("-----END SSH SIGNATURE-----\n"), armored);

        try (Git git = Git.init().setDirectory(tmp.toFile()).call()) {
            trust(git, signer);
            SignatureVerification result =
                    new SshSignatureVerifier()
                            .verify(
                                    git.getRepository(),
                                    new GpgConfig(git.getRepository().getConfig()),
                                    PAYLOAD,
                                    armored.getBytes(StandardCharsets.US_ASCII));
            assertTrue(result.verified(), "JGit rejected the signature: " + result.message());
            assertEquals(signer.getFingerprint(), result.keyFingerprint());
        }
        signer.clearSensitiveData();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {KeyPairProvider.SSH_ED25519, "ecdsa-sha2-nistp256", KeyPairProvider.SSH_RSA})
    void signatureVerifiesWithOpenSsh(String type) throws Exception {
        assumeTrue(new File("/usr/bin/ssh-keygen").canExecute(), "ssh-keygen not installed");
        StoredKeySshSigner signer = new StoredKeySshSigner(privateKey(type), "");
        Path sig = tmp.resolve("payload.sig");
        Files.write(sig, sign(signer).toExternalString().getBytes(StandardCharsets.US_ASCII));
        Path payload = tmp.resolve("payload");
        Files.write(payload, PAYLOAD);

        // check-novalidate checks the signature itself without an allowed-signers file.
        Process p =
                new ProcessBuilder(
                                "/usr/bin/ssh-keygen",
                                "-Y",
                                "check-novalidate",
                                "-n",
                                "git",
                                "-s",
                                sig.toString())
                        .redirectInput(payload.toFile())
                        .redirectErrorStream(true)
                        .start();
        assertTrue(p.waitFor(30, TimeUnit.SECONDS));
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, p.exitValue(), "ssh-keygen rejected the signature: " + output);
    }

    @Test
    void wrongNamespaceIsRejectedByOpenSsh() throws Exception {
        assumeTrue(new File("/usr/bin/ssh-keygen").canExecute(), "ssh-keygen not installed");
        StoredKeySshSigner signer =
                new StoredKeySshSigner(privateKey(KeyPairProvider.ECDSA_SHA2_NISTP256), "");
        Path sig = tmp.resolve("payload.sig");
        Files.write(sig, sign(signer).toExternalString().getBytes(StandardCharsets.US_ASCII));
        Path payload = tmp.resolve("payload");
        Files.write(payload, PAYLOAD);
        Process p =
                new ProcessBuilder(
                                "/usr/bin/ssh-keygen",
                                "-Y",
                                "check-novalidate",
                                "-n",
                                "file",
                                "-s",
                                sig.toString())
                        .redirectInput(payload.toFile())
                        .redirectErrorStream(true)
                        .start();
        assertTrue(p.waitFor(30, TimeUnit.SECONDS));
        assertFalse(p.exitValue() == 0, "a git signature must not verify as namespace file");
    }

    @Test
    void signsARealCommitThatJGitVerifies() throws Exception {
        StoredKeySshSigner signer =
                new StoredKeySshSigner(privateKey(KeyPairProvider.ECDSA_SHA2_NISTP256), "");
        try (Git git = Git.init().setDirectory(tmp.toFile()).call()) {
            git.getRepository().getConfig().setString("gpg", null, "format", "ssh");
            trust(git, signer);
            RevCommit commit =
                    git.commit()
                            .setMessage("signed")
                            .setAllowEmpty(true)
                            .setSign(true)
                            .setSigner(signer)
                            .setSigningKey(signer.getFingerprint())
                            .setAuthor(new PersonIdent("Test", "test@example.com"))
                            .setCommitter(new PersonIdent("Test", "test@example.com"))
                            .call();
            assertTrue(
                    new String(commit.getRawGpgSignature(), StandardCharsets.US_ASCII)
                            .startsWith("-----BEGIN SSH SIGNATURE-----"));
            SignatureVerification result =
                    git.verifySignature()
                            .addName(commit.name())
                            .call()
                            .get(commit.name())
                            .getVerification();
            assertTrue(
                    result.verified(), "JGit rejected the commit signature: " + result.message());
        }
    }

    @Test
    void clearedSignerRefusesToSign() throws Exception {
        StoredKeySshSigner signer =
                new StoredKeySshSigner(privateKey(KeyPairProvider.ECDSA_SHA2_NISTP256), "");
        signer.clearSensitiveData();
        assertFalse(signer.canLocateSigningKey(null, null, null, null, null));
        assertThrows(IllegalStateException.class, () -> sign(signer));
    }

    @Test
    void unreadableKeyIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new StoredKeySshSigner("nope", ""));
    }
}
