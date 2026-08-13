# ThingWorx service reference

The extension exposes services through two ThingWorx shapes/entities:

- `GIT.Repository.ThingShape` — Git repository operations and entity synchronization.
- `GIT.Utility.Thing` — repository lifecycle and per-user configuration.

Arguments below use the ThingWorx service names and base types defined by the Java annotations.
`INFOTABLE` arguments use the DataShape shown in Composer for that service. Password values must
be entered through the current user’s `UserExtensions` properties and must not be logged.

## Repository services

| Service | Arguments | Result / purpose |
|---|---|---|
| `Commit` | `Message: STRING` | Returns a commit status message. |
| `Push` | `Remote: STRING`, `BranchName: STRING`, `RemoteBranchName: STRING`, `SetUpstream: BOOLEAN` | Pushes the current/upstream branch or an explicit local branch to a remote branch. |
| `Pull` | `Force: BOOLEAN` | Fetches and integrates remote changes. |
| `Fetch` | `Remote: STRING` | Fetches remote refs without integrating them. |
| `DeleteLocalRepoContent` | — | Deletes the local repository content. Use with care. |
| `BranchCreate` | `BranchName: STRING`, `StartPoint: STRING` | Creates a local branch without switching. |
| `BranchSwitch` | `BranchName: STRING` | Switches to a local branch or creates a tracking branch for a matching origin branch. |
| `Checkout` | `BranchNameOrCommit: STRING`, `CreateBranch: BOOLEAN`, `StartPoint: STRING`, `Force: BOOLEAN` | Checks out a branch, tag, or commit, optionally creating a branch or forcing the update. |
| `GetCurrentBranch` | — | Returns the current branch or detached-head state. |
| `GetBranchList` | — | Returns `GIT.BranchList`. |
| `BranchDelete` | `BranchName: STRING`, `Remote: STRING`, `DeleteRemote: BOOLEAN`, `Force: BOOLEAN` | Deletes a local branch and optionally its remote branch. |
| `GetCommitList` | — | Returns `GIT.CommitList`. |
| `GetLog` | `Ref: STRING`, `MaxEntries: INTEGER` | Returns `GIT.CommitLog`. |
| `GetReflog` | `Ref: STRING`, `MaxEntries: INTEGER` | Returns `GIT.ReflogEntry`. |
| `Status` | — | Returns `GIT.Status` for the working tree. |
| `Add` | `File: STRING`, `All: BOOLEAN` | Stages one repository-relative file; with `All=true`, stages all non-ignored additions, modifications, and deletions. |
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
| `ExportProjectEntities` | `includeDependents: BOOLEAN` | Exports all entities from the `ProjectName` configured on the repository Thing and stages only its `RepoPathName/ProjectName` tree. It never commits. |
| `ImportProjectEntities` | `entityPath: STRING` | Imports repository entities into the repository Thing’s required configured `ProjectName` and returns the post-import Git status. |
| `SetGPGKeyForSigning` | `GpgKeyFingerprint: STRING` | Selects or clears the current user's signing key for this repository. |
| `Merge` | `BranchName: STRING` | Merges a branch into the current branch. |
| `Rebase` | `UpstreamBranch: STRING` | Rebases onto an upstream branch. |
| `CreateTag` | `TagName: STRING`, `Message: STRING`, `CommitID: STRING` | Creates an annotated tag. |
| `GetTagList` | — | Returns `GIT.TagList`. |
| `DeleteTag` | `TagName: STRING` | Deletes a local tag. |

`Push`, `Pull`, and `Fetch` use the repository’s configured remote and the current user’s Git
credentials from `UserExtensions`. Normal signed commits use the stored per-user GPG key
configuration.

## Utility services

| Service | Arguments | Result / purpose |
|---|---|---|
| `AddLogEntry` | `Content: STRING`, `ServiceName: STRING`, `Source: STRING`, `timestamp: DATETIME`, `User: STRING` | Writes an extension log entry. |
| `RepositoryList` | — | Lists available repository Things. Returns an `INFOTABLE` with `RepoName: STRING` rows. |
| `RepositoryCreate` | `RepoName: STRING`, `GitRepoURL: STRING`, `RepoPathName: STRING`, `BranchName: STRING`, `ProjectName: STRING`, `UseProxy: BOOLEAN`, `ProxyURL: STRING`, `ProxyPort: INTEGER`, `LocalizationTokensPrefix: STRING`, `GitCommitterUser: STRING`, `GitCommitterPassword: STRING`, `GitCommitterEmail: STRING`, `GitCommitterFullName: STRING` | Creates and configures the repository Thing and the current user's Git credentials in one call. |
| `RepositoryDelete` | `RepoName: STRING` | Deletes the repository Thing, local content, and the current user’s associated configuration. Returns `NOTHING`. |
| `InitUserExtensionProperties` | — | Creates and initializes the Git credentials and reusable GPG-key UserExtension properties. |
| `GpgKeyList` | — | Returns the current user’s reusable keys as `INFOTABLE` with `GIT.GpgKey.UserExtension.DataShape` rows. Private keys and passphrases remain protected. |
| `VerifyGpgKey` | `GpgPrivateKey: STRING`, `GpgKeyPassphrase: STRING` | Verifies a supplied or stored GPG key and returns `GIT.GpgKeyVerificationResult`. |
| `GpgKeyGet` | `GpgKeyFingerprint: STRING` | Returns one matching key as an `INFOTABLE` with `GIT.GpgKey.UserExtension.DataShape`. Fails if the fingerprint is not owned by the current user. |
| `GpgKeyCreate` | `GpgPrivateKey: STRING`, `GpgKeyPassphrase: STRING`, `GpgKeyFingerprint: STRING` (optional; derived when blank) | Creates a reusable current-user key. Returns `NOTHING`; fails when the fingerprint already exists. Key values are stored in protected user properties. |
| `GpgKeyUpdate` | `GpgKeyFingerprint: STRING`, `GpgPrivateKey: STRING`, `GpgKeyPassphrase: STRING` | Replaces the protected key material for an existing current-user fingerprint. Returns `NOTHING`; fails when the fingerprint does not exist. |
| `GpgKeyDelete` | `GpgKeyFingerprint: STRING` | Deletes an owned key and clears matching repository signing selections. Returns `NOTHING`; fails when the fingerprint does not exist. |
| `GitCredentialList` | — | Returns the current user’s credentials as an `INFOTABLE` with `GIT.RepositoryConfiguration.UserExtension.DataShape` rows. Passwords remain protected. |
| `GitCredentialGet` | `GitThing: THINGNAME` | Returns one repository credential record using `GIT.RepositoryConfiguration.UserExtension.DataShape`; fails when no current-user record exists. |
| `GitCredentialCreate` | `GitCommitterUser: STRING`, `GitCommitterPassword: STRING`, `GitCommitterEmail: STRING`, `GitCommitterFullName: STRING`, `GitThing: THINGNAME`, `GpgKeyFingerprint: STRING` | Creates a current-user credential record and returns `NOTHING`; fails if a record already exists for `GitThing`. |
| `GitCredentialUpdate` | `GitCommitterUser: STRING`, `GitCommitterPassword: STRING`, `GitCommitterEmail: STRING`, `GitCommitterFullName: STRING`, `GitThing: THINGNAME`, `GpgKeyFingerprint: STRING` | Updates an existing current-user record and returns `NOTHING`; fails if the record is missing. A blank fingerprint clears signing. |
| `GitCredentialDelete` | `GitThing: THINGNAME` | Deletes the current user’s credential record and returns `NOTHING`; fails if the record is missing. |

For the exact InfoTable fields and service annotations, see the [generated Java API reference](api/index.md).
