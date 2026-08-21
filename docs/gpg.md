# GPG signing

The extension can sign Git commits with an ASCII-armored private PGP key supplied through the ThingWorx user configuration.

## Configuration

1. Initialize the current user’s extension properties with `InitUserExtensionProperties`.
2. Create or update a reusable key with `GpgKeyCreate` or `GpgKeyUpdate`. Both accept an optional
   `GpgKeyLabel` so a key can be looked up by a human-readable name instead of the fingerprint.
3. Use `VerifyGpgKey` to confirm key material - all 5 args are optional selectors (see below).
4. Associate that fingerprint or label with the repository using `SetGPGKeyForSigning` (accepts `GpgKeyFingerprint` and/or `GpgKeyLabel`; both must match when supplied), or pass the fingerprint to
   `GitCredentialCreate`/`GitCredentialUpdate` when creating or updating repository credentials.
5. Call `Commit`, then `Push` if the signed commit should be sent to the remote Git server.

`GpgKeyGet` and `GpgKeyDelete` accept `GpgKeyFingerprint` and/or `GpgKeyLabel`; when both are
supplied both must match. At least one selector is required.

## Verifying keys (`VerifyGpgKey`)

All arguments are **optional**; precedence is `All` > `GpgKeyFingerprint`/`GpgKeyLabel` > `GpgPrivateKey`/`GpgKeyPassphrase`. No `GitThing` is involved. `GpgPrivateKey` is required when `All=false` and no fingerprint/label is supplied; blank is now an error (fallback removed). Result is `GIT.GpgKeyVerification.ServiceResult` wrapping `INFOTABLE<GIT.GpgKeyVerificationResult>` rows:

| Field | Type | Meaning |
|---|---|---|
| `GpgKeyFingerprint` | `STRING` | Derived fingerprint or `"Unable to derive fingerprint"` |
| `Valid` | `BOOLEAN` | `true` when the key material loads and can sign via `PastedKeyGpgSigner` |
| `Stored` | `BOOLEAN` | `true` when fingerprint is owned by the current user |
| `GpgKeyLabel` | `STRING` | Owned label, or `"Not in User Keys"` when `Valid && !Stored`, else `""` |

**Valid calling conventions (one row per call unless `All`):**

| Call | Args | Returns | Notes |
|---|---|---|---|
| Pasted new key (no passphrase) | `GpgPrivateKey="<armored>"`, `GpgKeyPassphrase=""` | `Valid=true, Stored=false, GpgKeyLabel="Not in User Keys"` if not owned; `Stored=true` + owned label if already stored | Accepts `Base64`-encoded armored key as well; supports both encrypted and unencrypted keys |
| Pasted new key (passphrase-protected) | `GpgPrivateKey="<armored>"`, `GpgKeyPassphrase="<pass>"` | Same as above | `GpgKeyCreate` + `Commit` signing path also pass `GpgKeyPassphrase`; verification is consistent |
| Verify all stored keys | `All=true` | One row per owned key (all `Stored=true`) | Ignores `GpgPrivateKey`/`GpgKeyPassphrase`/`GpgKeyFingerprint`/`GpgKeyLabel` |
| Verify stored by fingerprint | `GpgKeyFingerprint="<fp>"` | One row for that fingerprint | Fails (`Error=true`) if no owned key matches |
| Verify stored by label | `GpgKeyLabel="<label>"` | One row for that label |  |
| Verify stored by fingerprint+label | `GpgKeyFingerprint="<fp>", GpgKeyLabel="<label>"` | One row iff both match same owned key | Both must match; fails otherwise |
| Invalid new key | `GpgPrivateKey="not-a-key"` | `Valid=false, Stored=false, GpgKeyLabel=""` | Still returns fingerprint `"Unable..."` or error path |
| Blank / no selector | `GpgPrivateKey=""` with `All=false` and no fingerprint/label | Error (`Error=true`, `"GpgPrivateKey is required..."`) | Blank fallback removed as confusing; callers must pass explicit selector |

Examples:

```javascript
// 1) Pasted unencrypted
VerifyGpgKey({GpgPrivateKey: armored})

// 2) Pasted encrypted
VerifyGpgKey({GpgPrivateKey: armored, GpgKeyPassphrase: "s3cret"})

// 3) All owned
VerifyGpgKey({All: true})

// 5) By fingerprint
VerifyGpgKey({GpgKeyFingerprint: "ABCD1234..." })

// 6) By label
VerifyGpgKey({GpgKeyLabel: "ci-signing-key"})

// 7) By both
VerifyGpgKey({GpgKeyFingerprint: "ABCD...", GpgKeyLabel: "ci-signing-key"})
```

Keys are isolated per ThingWorx user. The signing implementation reads the armored key in memory
and exposes an explicit cleanup operation for its key and passphrase buffers, but platform
administrators should still protect user configuration and avoid unnecessary key exposure.

Use a dedicated signing key with an appropriate expiration date. Never commit private keys, passphrases, or exported ThingWorx user-property data to Git.
