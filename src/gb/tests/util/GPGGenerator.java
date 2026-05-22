package gb.tests.util;

import java.io.ByteArrayOutputStream;
import java.security.KeyPairGenerator;
import java.security.Security;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;

public class GPGGenerator {
    public static String generateTestGpgPrivateKey() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        var kpg = KeyPairGenerator.getInstance("RSA", "BC");
        kpg.initialize(1024);
        var kp = kpg.generateKeyPair();

        String userId = "Test User <test@example.com>";

        var pgpKeyPair = new JcaPGPKeyPair(
                PublicKeyAlgorithmTags.RSA_SIGN, kp, new java.util.Date());

        var digestProvBuilder = new JcaPGPDigestCalculatorProviderBuilder()
                .setProvider("BC");
        var sha1Calc = digestProvBuilder.build()
                .get(PGPUtil.SHA1);

        var keyRingGen = new PGPKeyRingGenerator(
                PGPSignature.POSITIVE_CERTIFICATION,
                pgpKeyPair,
                userId,
                sha1Calc,
                null, null,
                new JcaPGPContentSignerBuilder(
                        pgpKeyPair.getPublicKey().getAlgorithm(),
                        PGPUtil.SHA256).setProvider("BC"),
                null);

        var out = new ByteArrayOutputStream();
        try (var armoredOut = new ArmoredOutputStream(out)) {
            keyRingGen.generateSecretKeyRing().encode(armoredOut);
        }
        return out.toString("UTF-8");
    }
}
