package org.us_ignite.thingworx.jgit.extension;

import java.util.Locale;

/**
 * Decides which key signs a commit, from the user's saved choice for the repository, an optional
 * choice for this one commit, and the repository's RequireSigning setting.
 *
 * <p>A repository row records the format ({@code openpgp}, {@code ssh} or blank) and one
 * fingerprint per format. The format decides which fingerprint is read; the other is kept, so
 * switching back and forth loses nothing. A blank format with a GPG fingerprint is a row saved
 * before SSH signing existed, and signs with OpenPGP as it always did.
 */
public final class SigningSelection {
    public static final String OPENPGP = "openpgp";
    public static final String SSH = "ssh";

    /** For a per-commit choice only: do not sign this commit. */
    public static final String NONE = "none";

    /** The key a commit is signed with; {@code format} is blank when the commit is not signed. */
    public record Choice(String format, String fingerprint) {
        static final Choice UNSIGNED = new Choice("", "");

        public boolean signs() {
            return !format.isEmpty();
        }
    }

    private SigningSelection() {}

    /**
     * Normalizes a stored or requested format.
     *
     * @return {@code openpgp}, {@code ssh}, {@code none} or blank
     * @throws IllegalArgumentException for any other value
     */
    public static String format(String value) {
        String format = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (format) {
            case "", OPENPGP, SSH, NONE -> format;
            case "gpg", "pgp" -> OPENPGP; // the names people use for OpenPGP
            default -> unsupported(value);
        };
    }

    private static String unsupported(String value) {
        throw new IllegalArgumentException(
                "SigningFormat must be openpgp, ssh or blank, not '" + value + "'.");
    }

    /**
     * Parses a key reference as {@code GetSigningKeyOptions} returns it: {@code openpgp:<fp>},
     * {@code ssh:<fp>}, {@code none}, or blank for the repository's key.
     *
     * @return the format and fingerprint, both blank for a blank reference
     */
    public static Choice parseKey(String reference) {
        String value = reference == null ? "" : reference.trim();
        if (value.isEmpty()) return Choice.UNSIGNED;
        int colon = value.indexOf(':');
        String format = format(colon < 0 ? value : value.substring(0, colon));
        String fingerprint = colon < 0 ? "" : value.substring(colon + 1).trim();
        if (format.equals(NONE)) return new Choice(NONE, "");
        if (format.isEmpty() || fingerprint.isEmpty()) {
            throw new IllegalArgumentException(
                    "SigningKey must be openpgp:<fingerprint>, ssh:<fingerprint> or none, not '"
                            + reference
                            + "'.");
        }
        return new Choice(format, fingerprint);
    }

    /** The reference {@link #parseKey} reads: {@code format:fingerprint}, or blank. */
    public static String key(Choice choice) {
        return choice.signs() ? choice.format() + ":" + choice.fingerprint() : "";
    }

    /** The repository's saved choice, ignoring RequireSigning. */
    public static Choice saved(String storedFormat, String gpgFingerprint, String sshFingerprint) {
        String gpg = blankToEmpty(gpgFingerprint);
        String ssh = blankToEmpty(sshFingerprint);
        String format = format(storedFormat);
        if (format.isEmpty()) format = gpg.isEmpty() ? NONE : OPENPGP;
        return switch (format) {
            case OPENPGP -> gpg.isEmpty() ? Choice.UNSIGNED : new Choice(OPENPGP, gpg);
            case SSH -> ssh.isEmpty() ? Choice.UNSIGNED : new Choice(SSH, ssh);
            default -> Choice.UNSIGNED;
        };
    }

    /**
     * Resolves the key for one commit.
     *
     * @param overrideFormat blank to use the saved choice, {@code none} to commit unsigned, or a
     *     format with {@code overrideFingerprint}
     * @param requireSigning the repository's RequireSigning setting
     * @throws IllegalArgumentException when an override format has no fingerprint
     * @throws IllegalStateException when signing is required and no key applies
     */
    public static Choice resolve(
            String storedFormat,
            String gpgFingerprint,
            String sshFingerprint,
            String overrideFormat,
            String overrideFingerprint,
            boolean requireSigning) {
        String override = format(overrideFormat);
        Choice choice;
        if (override.isEmpty()) {
            choice = saved(storedFormat, gpgFingerprint, sshFingerprint);
        } else if (override.equals(NONE)) {
            choice = Choice.UNSIGNED;
        } else {
            String fingerprint = blankToEmpty(overrideFingerprint);
            if (fingerprint.isEmpty()) {
                throw new IllegalArgumentException(
                        Const.ERR_PREFIX_CONFIG
                                + "KeyFingerprint is required when SigningFormat is "
                                + override
                                + ".");
            }
            choice = new Choice(override, fingerprint);
        }
        if (requireSigning && !choice.signs()) {
            throw new IllegalStateException(
                    Const.ERR_PREFIX_CONFIG
                            + "This repository requires signed commits (RequireSigning), but no"
                            + " signing key is selected. Choose a signing key in Repository"
                            + " Settings or for this commit.");
        }
        return choice;
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
