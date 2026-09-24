package org.us_ignite.thingworx.operator.reconcile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.us_ignite.thingworx.operator.api.ThingWorxExtension;

/**
 * Pure deterministic SHA-256 fingerprint over extension identity and policy.
 * No KubernetesClient dependency.
 */
public final class FingerprintComputer {
    private FingerprintComputer() {}

    public static String fingerprint(ThingWorxExtension extension) {
        return compute(extension);
    }

    public static String compute(ThingWorxExtension extension) {
        var spec = extension.getSpec();
        var artifact = spec.getArtifact();
        var policy = spec.getImportPolicy();
        var input =
                String.join(
                        "\u0000",
                        artifact.getRepository(),
                        artifact.getDigest(),
                        String.valueOf(artifact.getExpectedName()),
                        String.valueOf(artifact.getExpectedVersion()),
                        String.valueOf(policy.isEntities()),
                        String.valueOf(policy.isExtensibleEntities()),
                        String.valueOf(policy.isJarResources()),
                        String.valueOf(policy.isJavascriptResources()),
                        String.valueOf(policy.isCssResources()),
                        String.valueOf(policy.isJsonResources()),
                        String.valueOf(policy.isWebAppResources()),
                        String.valueOf(spec.getArtifactPullSecret()));
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JRE", exception);
        }
    }
}
