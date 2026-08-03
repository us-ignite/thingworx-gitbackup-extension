# GPG signing

The extension can sign Git commits with an ASCII-armored private PGP key supplied through the ThingWorx user configuration.

## Configuration

1. Open the ThingWorx UI and edit the current user’s `UserExtensions` properties.
2. Add the armored private key and passphrase to the extension’s GPG key UserExtension data.
3. Verify that the key can be located and that its fingerprint is the expected value.
4. Enable commit signing for the repository in the corresponding UserExtension configuration.
5. Commit and verify the signature on the remote Git server.

Keys are isolated per ThingWorx user. The signing implementation clears its in-memory key and passphrase buffers when requested, but platform administrators should still protect user configuration and avoid unnecessary key exposure.

Use a dedicated signing key with an appropriate expiration date. Never commit private keys, passphrases, or exported ThingWorx user-property data to Git.
