# ThingWorx service reference

The extension exposes services through two ThingWorx shapes/entities:

- `GIT.Repository.ThingTemplate` — base Thing Template (extends `FileRepository`) that repository
  Things are created from; implements `GIT.Repository.ThingShape`.
- `GIT.Repository.ThingShape` — Git repository operations and entity synchronization.
- `GIT.Utility.Thing` — repository lifecycle and per-user configuration.

Arguments below use the ThingWorx service names and base types defined by the Java annotations.
`INFOTABLE` arguments use the DataShape shown in Composer for that service. Password values must
be entered through the current user’s `UserExtensions` properties and must not be logged.

## Repository services

| Service | Arguments | Result / purpose |
|---|---|---|
| `Commit` | `Message: STRING` | Exports project entities without staging them, then commits the current index. Requires staged changes. |
| `CommitStaged` | `Message: STRING` | Commits the current index without exporting project entities. Requires staged changes. |
| `Push` | `Remote: STRING`, `BranchName: STRING`, `RemoteBranchName: STRING`, `SetUpstream: BOOLEAN` | Pushes the current/upstream branch or an explicit local branch to a remote branch. |
| `Pull` | `Force: BOOLEAN` | Fetches and integrates remote changes and always imports the configured `ProjectName` (sync is automatic/invisible). |
| `Fetch` | `Remote: STRING` | Fetches remote refs without integrating them. |
| `BranchCreate` | `BranchName: STRING`, `StartPoint: STRING` | Creates a local branch without switching. |
| `BranchSwitch` | `BranchName: STRING` | Switches to a local branch or creates a tracking branch for a matching origin branch. |
| `Checkout` | `BranchNameOrCommit: STRING`, `CreateBranch: BOOLEAN`, `StartPoint: STRING`, `Force: BOOLEAN` | Checks out a branch, tag, or commit, optionally creating a branch or forcing the update. |
| `GetCurrentBranch` | — | Returns `GIT.CurrentBranchStatus.ServiceResult` for the current branch or detached-head state. |
| `GetBranchList` | — | Returns `GIT.BranchList.ServiceResult`. |
| `BranchDelete` | `BranchName: STRING`, `Remote: STRING`, `DeleteRemote: BOOLEAN`, `Force: BOOLEAN` | Deletes a local branch and optionally its remote branch. |
| `GetLog` | `Ref: STRING`, `MaxEntries: INTEGER` | Returns `GIT.CommitLog`. |
| `GetReflog` | `Ref: STRING`, `MaxEntries: INTEGER` | Returns `GIT.ReflogEntry`. |
| `Status` | `ShowUnchangedFiles: BOOLEAN`, `HideUntracked: BOOLEAN`, `HideUnchanged: BOOLEAN`, `HideIgnored: BOOLEAN` (all default `false`) | Synchronizes the project export and returns `GIT.Status` rows, optionally including unchanged paths and excluding untracked, unchanged, or ignored rows. The original `File`, `Status`, and `Staged` fields remain available, along with fields for the Changes view. |
| `Add` | `File: STRING`, `All: BOOLEAN` | Stages one repository-relative file; with `All=true`, stages all non-ignored additions, modifications, and deletions. |
| `Unstage` | `File: STRING`, `All: BOOLEAN` | Unstages one repository-relative file or, with `All=true`, every staged path. |
| `ApplyFileAction` | `Changes: INFOTABLE<GIT.WorkingTreeDisplay.DataShape>`, `Action: STRING` | Applies `stage`, `unstage`, or `untrack` to the selected rows after checking current Git status. |
| `RemoveEverywhere` | `Changes: INFOTABLE<GIT.WorkingTreeDisplay.DataShape>` | Deletes supported ThingWorx entities described by the selected XML exports and deletes the files, staging tracked deletions. Every file and entity is validated before removal; failures during deletion can still leave partial results and are reported. |
| `Remove` | `File: STRING` | Removes a path from the index using cached semantics and preserves its working-tree file. |
| `GetConflictFiles` | — | Returns conflicted files using `GIT.Status`. |
| `ReadConflictFile` | `File: STRING` | Returns the content of a conflict file. |
| `WriteConflictFile` | `File: STRING`, `Content: STRING` | Writes resolved conflict content. |
| `MergeContinue` | `Message: STRING` | Completes a merge after conflicts are resolved. |
| `MergeAbort` | — | Aborts the current merge. |
| `RebaseContinue` | — | Continues a rebase after conflicts are resolved. |
| `RebaseSkip` | — | Skips the current rebase commit. |
| `RebaseAbort` | — | Aborts the current rebase. |
| `GetDiffPerFile` | `File: STRING` | Returns the working-tree diff for one file. |
| `GetDiffPerFileBetweenCommits` | `File: STRING`, `FromCommitID: STRING` | Returns a file diff for the requested commit against its parent. |
| `GetCommitInfo` | `CommitID: STRING` | Returns `GIT.CommitInfo`. |
| `SetGPGKeyForSigning` | `GpgKeyFingerprint: STRING`, `GpgKeyLabel: STRING` (optional; both must match when supplied) | Selects or clears the current user's signing key for this repository by fingerprint or label. Blank or no selector disables signing. |
| `Merge` | `BranchName: STRING` | Merges a branch into the current branch. |
| `Rebase` | `UpstreamBranch: STRING` | Rebases onto an upstream branch. |
| `CreateTag` | `TagName: STRING`, `Message: STRING`, `CommitID: STRING` | Creates an annotated tag. |
| `GetTagList` | — | Returns `GIT.TagList.ServiceResult`. |
| `DeleteTag` | `TagName: STRING` | Deletes a local tag. |

`Push`, `Pull`, and `Fetch` use the repository’s configured remote and the current user’s Git
credentials from `UserExtensions`. Normal signed commits use the stored per-user GPG key
configuration.

File arguments and `Status.File` values are relative to the Git working tree at the
FileRepository root. They include the configured `RepoPath` folder when one is set. ThingWorx
FileRepository paths with a leading slash are also accepted by the single-file services.

The status service exports the configured project to a temporary directory and synchronizes its
canonical exports into the working tree before reporting Git status. A tracked export that no
longer belongs to the project is removed from the working tree and appears as an unstaged deletion.
The Git index is not changed. Tracked exports are overwritten with the current ThingWorx content;
existing untracked files and files that cannot be identified as canonical entity exports are
preserved. Status refuses to synchronize while project export files have unresolved Git conflicts.

`RemoveEverywhere` accepts one or more ThingWorx `Entities` XML files, including files with multiple
entities. It supports Things, Thing Templates, Thing Shapes, Networks, Media Entities, Model and
Data Tag Vocabularies, Style Definitions, and Notification Definitions and Contents. Other entity
types, non-entity files, absent files, entities outside the configured project, and system or
extension entities are rejected. The caller needs delete permission on every entity and invoke
permission on the corresponding EntityServices deletion services. Deletion is not transactional.

## Utility services

| Service | Arguments | Result / purpose |
|---|---|---|
| `RepositoryList` | — | Lists available repository Things. Returns an `INFOTABLE` with `RepoName: STRING` rows. |
| `RepositoryCreate` | `RepoName: STRING`, `GitRepoURL: STRING`, `RepoPath: STRING`, `BranchName: STRING`, `ProjectName: STRING`, `UseProxy: BOOLEAN`, `ProxyURL: STRING`, `ProxyPort: INTEGER`, `LocalizationTokensPrefix: STRING`, `GitCommitterUser: STRING`, `GitCommitterPassword: STRING`, `GitCommitterEmail: STRING`, `GitCommitterFullName: STRING` | Creates and configures the repository Thing and the current user's Git credentials in one call. |
| `RepositoryDelete` | `RepoName: STRING` | Deletes the repository Thing, local content, and the current user’s associated configuration. |
| `InitUserExtensionProperties` | — | Creates and initializes the Git credentials and reusable GPG-key UserExtension properties. |
| `GpgKeyListSummary` | — | Lists the current user’s GPG key labels and fingerprints without private key material. |
| `GpgKeyList` | — | Returns the current user’s reusable keys as `INFOTABLE` with `GIT.GpgKey.ServiceResult.DataShape` rows (includes the optional `GpgKeyLabel`). Private keys and passphrases remain protected. |
| `VerifyGpgKey` | `GpgPrivateKey: STRING`, `GpgKeyPassphrase: STRING`, `All: BOOLEAN`, `GpgKeyFingerprint: STRING`, `GpgKeyLabel: STRING` (all optional - see below) | Verifies a PGP key can be loaded and returns `GIT.GpgKeyVerification.ServiceResult` whose `Result` is `INFOTABLE<GIT.GpgKeyVerificationResult>` rows `{ GpgKeyFingerprint:STRING, Valid:BOOLEAN, Stored:BOOLEAN, GpgKeyLabel:STRING }`. All args are selectors (no `GitThing`): `All=true` → all stored keys; else `GpgKeyFingerprint`/`GpgKeyLabel` (one or both, both must match) → matching stored key(s); else `GpgPrivateKey`+`GpgKeyPassphrase` (ASCII-armored or `Base64`-encoded) → single pasted/new key. Blank with no selector is an error (fallback removed). `Valid` indicates loadable/signable, `Stored` indicates owned, `GpgKeyLabel="Not in User Keys"` when valid but not owned. See `docs/gpg.md` for all valid calling conventions. |
| `GpgKeyGet` | `GpgKeyFingerprint: STRING`, `GpgKeyLabel: STRING` (optional) | Returns matching key(s) as an `INFOTABLE` with `GIT.GpgKey.ServiceResult.DataShape`. Matches by fingerprint, label, or both (both must match when supplied); at least one is required. Fails if no owned key matches. |
| `GpgKeyCreate` | `GpgPrivateKey: STRING`, `GpgKeyPassphrase: STRING`, `GpgKeyFingerprint: STRING` (optional verification value; always derived from the private key), `GpgKeyLabel: STRING` (optional) | Creates a reusable current-user key. The fingerprint is always derived from the private key; a supplied fingerprint must match it. Fails when the fingerprint or label already exists. Key values are stored in protected user properties. |
| `GpgKeyUpdate` | `GpgKeyFingerprint: STRING`, `GpgKeyLabel: STRING`, `GpgPrivateKey: STRING`, `GpgKeyPassphrase: STRING` | Replaces the protected key material for an existing current-user key selected by fingerprint or label. When the fingerprint is supplied, `GpgKeyLabel` becomes the new label; fails when no owned key matches or the new label is already in use. |
| `GpgKeyDelete` | `GpgKeyFingerprint: STRING`, `GpgKeyLabel: STRING` (optional) | Deletes an owned key matched by fingerprint, label, or both (both must match when supplied; at least one is required) and clears matching repository signing selections; fails when no owned key matches. |
| `GitCredentialList` | — | Returns the current user’s credentials as an `INFOTABLE` with `GIT.RepositoryConfiguration.ServiceResult.DataShape` rows. Passwords remain protected. |
| `GitCredentialGet` | `GitThing: THINGNAME` | Returns one repository credential record using `GIT.RepositoryConfiguration.ServiceResult.DataShape`; fails when no current-user record exists. |
| `GitCredentialCreate` | `GitCommitterUser: STRING`, `GitCommitterPassword: STRING`, `GitCommitterEmail: STRING`, `GitCommitterFullName: STRING`, `GitThing: THINGNAME`, `GpgKeyFingerprint: STRING` | Creates a current-user credential record; fails if a record already exists for `GitThing`. |
| `GitCommitIdentityUpdate` | `GitThing: THINGNAME`, `GitCommitterFullName: STRING`, `GitCommitterEmail: STRING` | Updates the current user’s commit identity for an existing repository record, preserving its credentials and signing selection. |
| `GitCredentialUpdate` | `GitCommitterUser: STRING`, `GitCommitterPassword: STRING`, `GitCommitterEmail: STRING`, `GitCommitterFullName: STRING`, `GitThing: THINGNAME`, `GpgKeyFingerprint: STRING` | Updates an existing current-user record; fails if the record is missing. A blank fingerprint clears signing. |
| `GitCredentialDelete` | `GitThing: THINGNAME` | Deletes the current user’s credential record; fails if the record is missing. |

For the exact InfoTable fields and service annotations, see the [generated Java API reference](api/index.md).
