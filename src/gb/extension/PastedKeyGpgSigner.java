package gb.extension;

/** Compatibility bridge for Java integrations using the historical package name. */
@Deprecated
public class PastedKeyGpgSigner extends gitbackup.extension.PastedKeyGpgSigner {
    public PastedKeyGpgSigner(String privateKeyArmored, String passphrase) {
        super(privateKeyArmored, passphrase);
    }
}
