package org.us_ignite.thingworx.jgit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.us_ignite.thingworx.jgit.extension.SigningSelection;
import org.us_ignite.thingworx.jgit.extension.SigningSelection.Choice;

class SigningSelectionTest {
    private static final String GPG = "4F2A00000000000000000000000000000000009C";
    private static final String SSH = "SHA256:77ef";

    private static Choice resolve(String format, String gpg, String ssh) {
        return SigningSelection.resolve(format, gpg, ssh, null, null, false);
    }

    @Test
    void legacyRowWithOnlyGpgFingerprintSignsWithOpenPgp() {
        assertEquals(new Choice("openpgp", GPG), resolve("", GPG, ""));
        assertEquals(new Choice("openpgp", GPG), resolve(null, GPG, null));
    }

    @Test
    void blankRowIsUnsigned() {
        assertFalse(resolve("", "", "").signs());
        assertFalse(resolve(null, null, null).signs());
    }

    @Test
    void formatPicksItsFingerprintAndKeepsTheOther() {
        assertEquals(new Choice("ssh", SSH), resolve("ssh", GPG, SSH));
        assertEquals(new Choice("openpgp", GPG), resolve("openpgp", GPG, SSH));
        assertEquals(new Choice("openpgp", GPG), resolve("OpenPGP", GPG, SSH));
    }

    @Test
    void formatWithoutItsFingerprintIsUnsigned() {
        assertFalse(resolve("ssh", GPG, "").signs());
        assertFalse(resolve("openpgp", "", SSH).signs());
    }

    @Test
    void perCommitOverrideReplacesTheSavedKey() {
        assertEquals(
                new Choice("ssh", "SHA256:other"),
                SigningSelection.resolve("openpgp", GPG, SSH, "ssh", "SHA256:other", false));
        assertFalse(SigningSelection.resolve("openpgp", GPG, SSH, "none", "", false).signs());
        assertThrows(
                IllegalArgumentException.class,
                () -> SigningSelection.resolve("", GPG, "", "ssh", "", false));
    }

    @Test
    void requireSigningRefusesEveryUnsignedCase() {
        IllegalStateException e =
                assertThrows(
                        IllegalStateException.class,
                        () -> SigningSelection.resolve("", "", "", null, null, true));
        assertTrue(e.getMessage().contains("RequireSigning"), e.getMessage());
        assertThrows(
                IllegalStateException.class,
                () -> SigningSelection.resolve("ssh", GPG, "", null, null, true));
        assertThrows(
                IllegalStateException.class,
                () -> SigningSelection.resolve("openpgp", GPG, SSH, "none", null, true));
        assertEquals(
                new Choice("openpgp", GPG),
                SigningSelection.resolve("", GPG, "", null, null, true));
    }

    @Test
    void unknownFormatIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> resolve("x509", GPG, SSH));
        assertEquals("openpgp", SigningSelection.format("gpg"));
    }
}
