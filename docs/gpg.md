# GPG signing

The extension can sign Git commits with an ASCII-armored private PGP key supplied through the ThingWorx user configuration.

## Configuration

1. Initialize the current user’s extension properties with `InitUserExtensionProperties`.
2. Create or update a reusable key with `GpgKeyCreate` or `GpgKeyUpdate`. Both accept an optional
   `GpgKeyLabel` so a key can be looked up by a human-readable name instead of the fingerprint.
3. Use `VerifyGpgKey` or the returned fingerprint to confirm the key material.
4. Associate that fingerprint with the repository using `SetGPGKeyForSigning`, or pass it to
   `GitCredentialCreate`/`GitCredentialUpdate` when creating or updating repository credentials.
5. Call `Commit`, then `Push` if the signed commit should be sent to the remote Git server.

`GpgKeyGet` and `GpgKeyDelete` accept `GpgKeyFingerprint` and/or `GpgKeyLabel`; when both are
supplied both must match. At least one selector is required.

Keys are isolated per ThingWorx user. The signing implementation reads the armored key in memory
and exposes an explicit cleanup operation for its key and passphrase buffers, but platform
administrators should still protect user configuration and avoid unnecessary key exposure.

Use a dedicated signing key with an appropriate expiration date. Never commit private keys, passphrases, or exported ThingWorx user-property data to Git.
