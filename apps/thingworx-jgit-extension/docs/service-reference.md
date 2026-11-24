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
| `Commit` | `Message: STRING` | Commits the current index, like `CommitStaged`. Requires staged changes. |
| `CommitStaged` | `Message: STRING` | Commits the current index. Requires staged changes. |
| `SyncFromThingWorx` | — | Exports the tracked projects into the working tree so their changes show in `Status`. Does not stage or commit. Succeeds partially: `Result` holds a `GIT.ThingworxSyncAction` row for each project, collection or Data Table that could not be exported, whose files are left as they were; `Error` stays `false` on a partial success, so check `Result`; `Error=true`, with an empty `Result`, only when nothing was exported. See [Partial success](import-export.md#partial-success). |
| `SyncToThingWorx` | `Force: BOOLEAN` (default `false`) | Imports the Git-tracked files in the tracked project folders into ThingWorx; untracked files are never imported and are named in the result. Unless `Force` is true, returns `Error=true` naming the entities changed in ThingWorx since the last sync, which the import would overwrite. Succeeds partially: `Result` holds a `GIT.ThingworxSyncAction` row for each entity, User, project or Data Table that could not be imported; `Error` stays `false` on a partial success, so check `Result`; `Error=true`, with an empty `Result`, only when nothing was imported. See [Partial success](import-export.md#partial-success). |
| `GetThingWorxChangesSinceSync` | — | Describes the tracked-project entities changed in ThingWorx since the last sync; blank when there are none. Used by the Changes screen to confirm before Sync to ThingWorx. |
| `Push` | `Remote: STRING`, `BranchName: STRING`, `RemoteBranchName: STRING`, `SetUpstream: BOOLEAN` | Pushes the current/upstream branch or an explicit local branch to a remote branch. |
| `Pull` | `Remote: STRING` (optional), `Force: BOOLEAN` | Fetches and integrates remote changes. ThingWorx is not changed; use `SyncToThingWorx` to apply the result. |
| `Fetch` | `Remote: STRING` (optional) | Fetches remote refs without integrating them. |
| `GetRemotes` | — | Lists the remotes in the repository's `.git/config` (`GIT.Remote.DataShape`: `Name`, `URLs`, `PushURLs`, `FetchSpecs`). URLs are shown without any embedded password. |
| `RemoteAdd` | `Name: STRING`, `URL: STRING` | Adds a remote. Only `https://`, `http://` and `ssh://` URLs without a username or password are accepted. Plain HTTP is limited to anonymous access. |
| `GetConnectionStatus` | — | For each remote, reports which setup steps the current user still needs before pulling and pushing (`GIT.RemoteStatus.DataShape`: `Remote`, `URL`, `Transport`, `IsDefault`, `Network`, `SignIn`, `CredentialOrigin`, `HostKey`, `Ready`, `NextStep`). Step fields are `ok`, `missing` or `n/a`. Only the saved settings are checked; nothing is contacted except DNS, so use `TestRemote` for the final check. |
| `RemoteSetUrl` | `Name: STRING`, `URL: STRING`, `Push: BOOLEAN` | Changes a remote's URL, or its separate push URL when `Push` is true. A blank `URL` with `Push` true removes the push URL, so pushes use the fetch URL again. |
| `RemoteRemove` | `Name: STRING` | Removes a remote, its remote-tracking branches, and the upstream settings of branches that tracked it. Also deletes the caller's sign-in for that remote. |
| `RemoteSignInList` | — | Lists each remote with the caller's sign-in for it (`GIT.RemoteSignIn.DataShape`: `Remote`, `GitCommitterUser`, `HasPassword`, `SshKeyFingerprint`, `Inherited`). `Inherited` is true when the remote has no sign-in of its own and uses the repository-wide one. Passwords are never returned. |
| `RemoteSignInSave` | `Remote: STRING`, `GitCommitterUser: STRING`, `GitCommitterPassword: STRING`, `SshKeyFingerprint: STRING` | Saves the caller's sign-in for one existing remote. A blank password keeps the one the remote uses now. The SSH key must be one of the caller's keys. The password is approved only for the remote's current URLs. |
| `RemoteSignInClear` | `Remote: STRING` | Deletes the caller's own sign-in for one remote, which then uses the repository-wide sign-in, if any. |
| `TestRemote` | `Remote: STRING` (optional) | Connects to a remote with the current user's credentials and reports its branch and tag counts. Nothing is fetched. |
| `ResetSshHostKeys` | — | Forgets the SSH host keys approved for this repository. Every SSH connection is refused until a host key is probed and approved again. |
| `SshHostKeyProbe` | `Remote: STRING` (optional) | Connects only far enough to read the SSH server's host key and returns `GIT.SshHostKey.ServiceResult.DataShape` (`Remote`, `Host`, `Port`, `Fingerprint`, `PublicKey`). Nothing is trusted. |
| `SshHostKeyApprove` | `Remote: STRING` (optional), `Fingerprint: STRING` | Probes the host again, requires the SHA-256 fingerprint to match `Fingerprint`, and records the key in this repository's `.git/ssh/known_hosts`. Refuses to replace a different approved key. |
| `BranchCreate` | `BranchName: STRING`, `StartPoint: STRING` | Creates a local branch without switching. |
| `BranchSwitch` | `BranchName: STRING` | Switches to a local branch or creates a tracking branch for a matching remote branch, preferring `origin`. |
| `Checkout` | `BranchNameOrCommit: STRING`, `CreateBranch: BOOLEAN`, `StartPoint: STRING`, `Force: BOOLEAN` | Checks out a branch, tag, or commit, optionally creating a branch or forcing the update. |
| `GetCurrentBranch` | — | Returns `GIT.CurrentBranchStatus.ServiceResult` for the current branch or detached-head state. |
| `GetBranchList` | — | Returns `GIT.BranchList.ServiceResult`. |
| `BranchDelete` | `BranchName: STRING`, `Remote: STRING`, `DeleteRemote: BOOLEAN`, `Force: BOOLEAN` | Deletes a local branch and optionally its remote branch. |
| `GetLog` | `Ref: STRING`, `MaxEntries: INTEGER`, `AllBranches: BOOLEAN` | Returns `GIT.CommitLog` rows, newest first. A blank `Ref` uses the current branch; an unknown `Ref` returns `Error=true`. `MaxEntries` of zero or blank means no limit. |
| `GetReflog` | `Ref: STRING`, `MaxEntries: INTEGER` | Returns `GIT.ReflogEntry` rows, newest first. A blank `Ref` uses `HEAD`; short names such as `main` or `origin/main` resolve the way Git does, and an unknown `Ref` returns `Error=true`. Reflogs are local to this clone. |
| `GetHistoryRefs` | `ReflogOnly: BOOLEAN` | Returns `GIT.HistoryRef` rows for HEAD, local branches, remote-tracking branches, and tags, in that order. `DisplayName` is the short name that `GetLog` and `GetReflog` accept. With `ReflogOnly=true`, only refs with local reflog entries are returned. |
| `GetReadme` | — | Returns the README at the root of the commit HEAD points at as one `GIT.FileContent` row (`Path`, `Content`, `Size`, `Binary`, `Truncated`), reading at most 1,000,000 bytes. Names are matched ignoring case, preferring `README.md`, then `README.markdown`, `README.mdown`, `README` and `README.txt`. Uncommitted edits are not shown. Returns no rows when the repository has no commit yet or no README. Used by the repository overview page. |
| `Status` | `ShowUnchangedFiles: BOOLEAN`, `HideUntracked: BOOLEAN`, `HideUnchanged: BOOLEAN`, `HideIgnored: BOOLEAN` (all default `false`) | Returns `GIT.Status` rows (Git only; use `SyncFromThingWorx` first to include ThingWorx changes), optionally including unchanged paths and excluding untracked, unchanged, or ignored rows. The original `File`, `Status`, and `Staged` fields remain available, along with fields for the Changes view. |
| `Add` | `File: STRING`, `All: BOOLEAN` | Stages one repository-relative file; with `All=true`, stages all non-ignored additions, modifications, and deletions. |
| `Unstage` | `File: STRING`, `All: BOOLEAN` | Unstages one repository-relative file or, with `All=true`, every staged path. |
| `ApplyFileAction` | `Changes: INFOTABLE<GIT.WorkingTreeDisplay.DataShape>`, `Action: STRING` | Applies `stage`, `unstage`, `untrack`, or `delete` to the selected rows after checking current Git status. `delete` removes the files from the working tree and stages tracked deletions without changing ThingWorx entities. |
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
| `CreateTag` | `TagName: STRING`, `Message: STRING`, `CommitID: STRING`, `Force: BOOLEAN` | Creates a tag on `CommitID` (blank: `HEAD`). It is signed with your signing key for the repository, as commits are; a signed tag, or one with a message, is annotated with your commit identity as tagger, and `RequireSigning` refuses an unsigned one. `Force` moves an existing tag. Nothing is pushed. |
| `GetTagList` | — | Returns `GIT.TagList.ServiceResult`: each tag's commit, message, date, tagger, and whether it is `Annotated` and `Signed`. |
| `PushTags` | `Remote: STRING`, `TagName: STRING`, `Force: BOOLEAN` | Pushes one tag, or every tag when `TagName` is blank, with your sign-in for the remote (blank: the current branch's upstream, or `origin`). `Force` replaces a remote tag that points elsewhere. |
| `DeleteTag` | `TagName: STRING`, `Remote: STRING` | Deletes a local tag and, when `Remote` is set, the tag on that remote first. |
| `GetTrackedProjects` | — | Lists the tracked projects with their `Path` (blank for the default folder). Returns `GIT.TrackedProject.ServiceResult.DataShape`. |
| `AddTrackedProject` | `ProjectName: STRING`, `Path: STRING` (optional) | Starts synchronizing an existing project. `Path` is the folder that holds the project's entities, relative to the repository root; blank means `<ProjectName>` at the repository root. Fails when the folder is the same as, inside, or contains another tracked project's folder. |
| `SetTrackedProjectPath` | `ProjectName: STRING`, `Path: STRING`, `MoveFiles: BOOLEAN` (optional, default true) | Changes a tracked project's folder; a blank `Path` restores the default. With `MoveFiles`, the files in the old folder are moved to the new one like `git mv`: tracked files are renamed in the index with their staged content unchanged, untracked files are moved on disk, and empty folders left behind are removed. The renames appear in Changes for review and commit. Nothing is changed while a conflict is in progress, when a destination file already exists, when one folder contains the other, or when a symbolic link is involved. With `MoveFiles` false, files stay where they are and the next export writes to the new folder. |
| `RemoveTrackedProject` | `ProjectName: STRING`, `DeleteFiles: BOOLEAN` | Stops synchronizing a project. ThingWorx entities are never deleted. With `DeleteFiles`, the project's folder is deleted from the working tree and the deletion is left unstaged. |

`Push`, `Pull`, `Fetch`, `TestRemote`, and `BranchDelete` with `DeleteRemote` use a remote from the
repository’s `.git/config` and the current user’s sign-in for that remote (`RemoteSignInSave`), or
the repository-wide sign-in when the remote has none of its own. A blank `Remote`
selects the branch’s upstream remote, or `origin` when it has none. For HTTPS remotes, the stored username
and password or token are read only when the server asks for authentication, so anonymous remotes
work without credentials. For SSH remotes, the SSH key selected for the remote is
used, and a server host key must be explicitly probed and approved before the first connection (see [Configuration](configuration.md#ssh-remotes)).
Authentication failures return `Error=true` with an `[AUTH]` message that says what to fix.
Normal signed commits use the stored per-user GPG key configuration.

File arguments and `Status.File` values are relative to the Git working tree at the
FileRepository root, so they include the tracked project's folder. ThingWorx
FileRepository paths with a leading slash are also accepted by the single-file services.

`Status` reads Git only. `SyncFromThingWorx` exports the tracked projects to a temporary directory
and synchronizes their canonical exports into the working tree. A tracked export that no longer
belongs to the project is removed from the working tree and appears as an unstaged deletion. The
Git index is not changed. Tracked exports are overwritten with the current ThingWorx content;
existing untracked files and files that cannot be identified as canonical entity exports are
preserved. It refuses to run while project export files have unresolved Git conflicts.

`RemoveEverywhere` accepts one or more ThingWorx `Entities` XML files, including files with multiple
entities. It supports Things, Thing Templates, Thing Shapes, Networks, Media Entities, Model and
Data Tag Vocabularies, Style Definitions, and Notification Definitions and Contents. Other entity
types, non-entity files, absent files, entities outside the configured project, and system or
extension entities are rejected. An entity that is already absent is skipped and its XML file is
still removed. The caller needs delete permission on every existing entity and invoke
permission on the corresponding EntityServices deletion services. Deletion is not transactional.

Mashup XML files can also be removed, but the Mashups remain in ThingWorx and the result warns
about them. A later project export can recreate their XML; delete the Mashups in Composer to
remove them permanently. Status refresh preserves staged deletions until they are committed.

## Utility services

| Service | Arguments | Result / purpose |
|---|---|---|
| `RepositoryList` | — | Lists available repository Things. Returns an `INFOTABLE` with `RepoName: STRING` rows. |
| `RepositoryCreate` | `RepoName: STRING`, `GitRepoURL: STRING`, `BranchName: STRING`, `TrackedProjects: INFOTABLE` (`GIT.TrackedProject.DataShape`: `ProjectName` and optional `Path`), `UseProxy: BOOLEAN`, `ProxyURL: STRING`, `ProxyPort: INTEGER`, `LocalizationTokensPrefix: STRING`, `GitCommitterUser: STRING`, `GitCommitterPassword: STRING`, `GitCommitterEmail: STRING`, `GitCommitterFullName: STRING` | Creates and configures the repository Thing, adds `GitRepoURL` as the `origin` remote in `.git/config` when it is set, and, when any credential field is set, the current user's Git credentials. Fails without changes when a Thing with that name exists, when the caller lacks `ServiceInvoke` permission on `EntityServices.CreateThing`, or when a tracked project does not exist or the caller cannot see it or lacks design-time Read on it (both cases report the same not-found error). A blank `BranchName` means `main`. If a later step fails, the new Thing is deleted so the name can be reused. |
| `GetTrackableProjects` | — | Lists ThingWorx projects that a new repository can track, for the New Repository form. Only projects the caller can see and has design-time Read permission on are listed. Returns `GIT.TrackedProject.ServiceResult.DataShape`. |
| `ProjectCreate` | `ProjectName: STRING`, `Description: STRING` (optional) | Creates an empty ThingWorx project for a new repository to track, from the New Repository form. Names may contain letters, digits, `_`, `.` and `-`, must not start with `.` or `-`, and may be up to 200 characters. The name `git`, and names starting with `git.` (any case), are reserved for the extension. An existing project is refused; select it in the list instead. The caller needs `ServiceInvoke` permission on `EntityServices.CreateProject`. |
| `RepositoryDelete` | `RepoName: STRING` | Deletes the repository Thing and its working tree, moves its Git directory to `jgit/deleted` without remotes, and removes every user’s settings and sign-ins for it. Only deletes Things in the `GIT.Repositories` project that are based on `GIT.Repository.ThingTemplate`; any other Thing is refused. The caller needs design-time Delete permission on the repository Thing and `ServiceInvoke` permission on `EntityServices.DeleteThing`. |
| `RunUpgrade` | — | Administrator only. Upgrades every repository and every user's saved settings to the current format and returns a report (`GIT.UpgradeStep.DataShape`: `Scope`, `Name`, `Item`, `Action`, `Detail`). Runs automatically on import and at startup; safe to repeat. See [Upgrading from earlier versions](configuration.md#upgrading-from-earlier-versions). |
| `InitUserExtensionProperties` | — | Administrator only. Creates and initializes the Git credentials, reusable GPG-key, and reusable SSH-key UserExtension properties. Ordinary users initialize only their own rows automatically when they call a per-user service. |
| `GpgKeyListSummary` | — | Lists the current user’s GPG key labels and fingerprints without private key material. |
| `GpgKeyList` | — | Returns the current user’s reusable keys as `INFOTABLE` with `GIT.GpgKey.ServiceResult.DataShape` rows. Only `GpgKeyFingerprint` and `GpgKeyLabel` are filled; the `GpgPrivateKey` and `GpgKeyPassphrase` fields of the data shape are never returned. |
| `VerifyGpgKey` | `GpgPrivateKey: STRING`, `GpgKeyPassphrase: STRING`, `All: BOOLEAN`, `GpgKeyFingerprint: STRING`, `GpgKeyLabel: STRING` (all optional - see below) | Verifies a PGP key can be loaded and returns `GIT.GpgKeyVerification.ServiceResult` whose `Result` is `INFOTABLE<GIT.GpgKeyVerificationResult>` rows `{ GpgKeyFingerprint:STRING, Valid:BOOLEAN, Stored:BOOLEAN, GpgKeyLabel:STRING }`. All args are selectors (no `GitThing`): `All=true` → all stored keys; else `GpgKeyFingerprint`/`GpgKeyLabel` (one or both, both must match) → matching stored key(s); else `GpgPrivateKey`+`GpgKeyPassphrase` (ASCII-armored or `Base64`-encoded) → single pasted/new key. Blank with no selector is an error (fallback removed). `Valid` indicates loadable/signable, `Stored` indicates owned, `GpgKeyLabel="Not in User Keys"` when valid but not owned. See `docs/gpg.md` for all valid calling conventions. |
| `GpgKeyGet` | `GpgKeyFingerprint: STRING`, `GpgKeyLabel: STRING` (optional) | Returns matching key(s) as an `INFOTABLE` with `GIT.GpgKey.ServiceResult.DataShape`. Matches by fingerprint, label, or both (both must match when supplied); at least one is required. Fails if no owned key matches. Only `GpgKeyFingerprint` and `GpgKeyLabel` are filled; the private key and passphrase are never returned. |
| `GpgKeyCreate` | `GpgPrivateKey: STRING`, `GpgKeyPassphrase: STRING`, `GpgKeyFingerprint: STRING` (optional verification value; always derived from the private key), `GpgKeyLabel: STRING` (optional) | Creates a reusable current-user key. The fingerprint is always derived from the private key; a supplied fingerprint must match it. Fails when the fingerprint or label already exists. Key values are stored in protected user properties. |
| `GpgKeyUpdate` | `GpgKeyFingerprint: STRING`, `GpgKeyLabel: STRING`, `GpgPrivateKey: STRING`, `GpgKeyPassphrase: STRING` | Updates an existing current-user key selected by fingerprint or label. A non-blank `GpgPrivateKey` replaces the stored key and passphrase together; a blank `GpgPrivateKey` keeps both (`GpgKeyPassphrase` is then ignored), so a key can be renamed without resupplying it. When the fingerprint is supplied, `GpgKeyLabel` becomes the new label; fails when no owned key matches or the new label is already in use. |
| `GpgKeyDelete` | `GpgKeyFingerprint: STRING`, `GpgKeyLabel: STRING` (optional) | Deletes an owned key matched by fingerprint, label, or both (both must match when supplied; at least one is required) and clears matching repository signing selections; fails when no owned key matches. Returns the fingerprint and label of the deleted key(s), without the private key or passphrase. |
| `GitCredentialList` | — | Returns the current user’s repository settings as `GIT.RepositoryConfigurationSummary.ServiceResult.DataShape` rows. Passwords are never returned; `HasPassword` reports whether one is stored. |
| `GitCredentialGet` | `GitThing: THINGNAME` | Returns one repository’s settings using `GIT.RepositoryConfigurationSummary.ServiceResult.DataShape`, without the password; fails when no current-user record exists. |
| `GitCredentialCreate` | `GitCommitterUser: STRING`, `GitCommitterPassword: STRING`, `GitCommitterEmail: STRING`, `GitCommitterFullName: STRING`, `GitThing: THINGNAME`, `GpgKeyFingerprint: STRING` | Creates a current-user credential record; fails if a record already exists for `GitThing`. |
| `GitIdentityGet` | — | Returns the current user's commit name and email (`GIT.UserIdentity.DataShape`: `GitUserName`, `GitUserEmail`). |
| `GitIdentitySave` | `GitUserName: STRING`, `GitUserEmail: STRING` | Saves the name and email recorded on the current user's commits in every repository. Both are required. |
| `GitCommitIdentityUpdate` (deprecated; use `GitIdentitySave`, `GitThing` is ignored) | `GitThing: THINGNAME`, `GitCommitterFullName: STRING`, `GitCommitterEmail: STRING` | Updates the current user’s commit identity for an existing repository record, preserving its credentials and signing selection. |
| `GitCredentialUpdate` | `GitCommitterUser: STRING`, `GitCommitterPassword: STRING`, `GitCommitterEmail: STRING`, `GitCommitterFullName: STRING`, `GitThing: THINGNAME`, `GpgKeyFingerprint: STRING` | Updates an existing current-user record; fails if the record is missing. A blank fingerprint clears signing. |
| `GitRepositorySettingsSave` | `GitThing: THINGNAME`, `GitCommitterUser: STRING`, `GitCommitterPassword: STRING`, `GitCommitterEmail: STRING`, `GitCommitterFullName: STRING`, `GpgKeyFingerprint: STRING` | Creates or updates the current user’s repository credentials and signing selection. A blank password preserves an existing password; a blank fingerprint disables signing. |
| `GitCredentialDelete` | `GitThing: THINGNAME` | Deletes the current user’s credential record; fails if the record is missing. |
| `SshKeyGenerate` | `SshKeyLabel: STRING` | Generates an SSH key for the current user (Ed25519, or ECDSA P-256 when Ed25519 is unavailable) and returns `GIT.SshKeySummary.ServiceResult.DataShape` with its fingerprint and public key. The private key is stored encrypted and never returned. |
| `SshKeyImport` | `SshPrivateKey: STRING`, `SshKeyPassphrase: STRING`, `SshKeyLabel: STRING` | Stores an existing OpenSSH or PEM private key after checking that it loads with the passphrase. Returns its fingerprint and public key. Fails when the key is already stored. |
| `SshKeyListSummary` | — | Lists the current user’s SSH keys as `GIT.SshKeySummary.ServiceResult.DataShape` rows (fingerprint, label, public key) without private key material. |
| `SshKeyDelete` | `SshKeyFingerprint: STRING` | Deletes one of the current user’s SSH keys and clears it from the user’s repository settings. |
| `SetRepositorySshKey` | `GitThing: THINGNAME`, `SshKeyFingerprint: STRING` | Selects the current user’s repository-wide SSH key, used by SSH remotes without a sign-in of their own (see `RemoteSignInSave`); blank clears it. Creates the repository settings record when the user has none. |

| `GetGitLog` | `MaxEntries: NUMBER` (default 100, at most 1000) | Returns `GIT.GitLog` rows for recent Git actions (action, repository, user, message), newest first. Administrators see every repository; other callers only see entries for repository Things they can see and have design-time Read permission on. |
| `GetExtensionLog` | `MaxEntries: INTEGER` (default 200, at most 2000), `MinimumLevel: STRING`, `SearchText: STRING` | Administrator only. Returns `GIT.ExtensionLog` rows the extension wrote to the ApplicationLog, newest first. `MinimumLevel` is `TRACE`, `DEBUG`, `INFO`, `WARN`, or `ERROR`; blank includes every level. `SearchText` matches the message, ignoring case. |
For the exact InfoTable fields and service annotations, see the [generated Java API reference](api/index.md).

## Extension log

The extension writes to the ThingWorx ApplicationLog through loggers named after its classes,
so each entry's origin is an abbreviated class name such as `o.u.t.j.e.GitActionLogger`. Git
action entries (`GIT.ACTION | action | repository | message | user`) are written at `WARN`.
Only entries at or above the ApplicationLog's configured level are recorded; ThingWorx defaults
to `WARN`, so `INFO` entries such as service completion messages are not kept unless the level is
lowered. `GetExtensionLog` and the Extension Log page under Git User Settings read these entries.
Entries the extension wrote to the ScriptLog before this release stay in the ScriptLog.
For newly logged service failures with an exception, Source includes the exception's stack trace;
older entries and messages without an exception show the logger class only.
