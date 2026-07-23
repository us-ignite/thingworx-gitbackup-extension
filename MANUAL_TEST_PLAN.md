# Manual Test Plan — ThingWorx GitBackup Extension v6.0.0

## Prerequisites

- ThingWorx Platform 9.3.0+ with the GitBackup extension installed
- A Git remote server (GitHub, GitLab, Bitbucket, Gitea, etc.) with a repository to test against
- A User with Administrator or appropriate rights to create/modify Things
- A ThingWorx FileRepository configured and accessible

---

## 1. Extension Installation & Validation

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 1.1 | Import extension ZIP | Import `GitBackupExtension.zip` via ThingWorx Composer → Extension → Import | Import succeeds with no errors |
| 1.2 | Extension appears in list | Navigate to Extensions in Composer | GitBackupExtension v6.0.0 is listed as installed |
| 1.3 | Migration runs automatically | After import, check logs | `ExtMigrator` runs; UserExtension properties (`GitCredentials`, `GpgKeys`, `GitCommitterEmail`, `GitCommitterName`, `UseGitCommitUserValues`) are created for all users |
| 1.4 | Bundle entities present | Search for "GitBackup" in Composer | All entities (Things, ThingShapes, DataShapes, Mashups, StateDefinitions, Media, StyleTheme, DataTables) are present |
| 1.5 | CheckConfiguration validation | Navigate to Resources → GitBackupValidation → Services → CheckConfiguration; pass a FileRepository name with a relative path | Service returns failure — absolute path required |

---

## 2. GitBackup Thing Lifecycle

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 2.1 | Create GitBackup Thing manually | Create a new Thing from the `GitRepositoryTemplate` template | Thing is created; Configuration table is initialized with defaults |
| 2.2 | Create via `AddNewRepo` service | On `GIT.Utility.Thing`, invoke `AddNewRepo` with: GitRepoURL, FileRepository, RepoPathName, BranchName, User, Password, CommitEmail, CommitUser, Proxy params | A new GitBackup Thing is created and configured |
| 2.3 | Configure repo properties | On the created Thing, edit the Configuration table: set `GitRepoURL`, `FileRepository`, `RepoPathName`, `InitialBranch`, proxy settings | Values are saved |
| 2.4 | Delete GitBackup Thing | Delete the Thing via Composer | Thing is removed; no residual entities remain |

---

## 3. Git Operations — Core

### 3.1 Push (Commit & Push to Remote)

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 3.1.1 | Push without credentials | Invoke `Push` with no credentials set | Service returns error indicating missing credentials |
| 3.1.2 | Push to remote (initial) | Set valid credentials. Ensure the remote repo exists and is empty. Invoke `Push` with `CommitMessage` and `AuthorName` | Service succeeds. Local repo is initialized, initial commit created, files pushed to remote |
| 3.1.3 | Push with custom author name/email | Invoke `Push` with distinct `AuthorName` and `AuthorEmail` | Commit on remote shows the custom author |
| 3.1.4 | Push with commit message | Invoke `Push` with a non-empty `CommitMessage` | Commit message appears on remote |
| 3.1.5 | Push with GPG signing | If using 3.6.x GPG tests first, set `SignCommits=true` and store a valid PGP key. Invoke `Push` | Commit on remote is marked as "Verified" (signed) |
| 3.1.6 | Push — no new changes | Invoke `Push` twice in a row with no changes between | Second push reports nothing to commit (no error, but no new commit) |
| 3.1.7 | Push — pre-receive hook rejection | Configure a remote repo with a pre-receive hook that rejects unsigned commits. Attempt a `Push` without `SignCommits` enabled | Error message explicitly states "pre-receive hook declined" and includes a hint about enabling GPG-signed commits |
| 3.1.8 | Push — SignCommits warning logged | Invoke `Push` with `SignCommits=false` (default) | A warning log entry is written: "Push: SignCommits is not enabled for this repository. If the remote requires signed commits, the push will be rejected." |

### 3.2 Pull (Fetch & Merge from Remote)

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 3.2.1 | Pull without credentials | Invoke `Pull` with no credentials | Returns error |
| 3.2.2 | Pull with fast-forward | Create a commit on remote directly (e.g., via web UI). Invoke `Pull` on the Thing | Local repo updated with the remote commit |
| 3.2.3 | Pull with merge commit | Make a local commit (via Push), then a different commit on remote. Invoke `Pull` | A merge commit is created locally |
| 3.2.4 | Pull with Force=true | Manually create a conflicting local commit. Invoke `Pull` with `Force=true` | Local changes are discarded (hard reset to remote HEAD) |
| 3.2.5 | Pull with Force=false (conflict) | Make conflicting changes locally and on remote. Invoke `Pull` with `Force=false` | Service returns error about merge conflict |

### 3.3 Status

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 3.3.1 | Clean status | After a successful Push/Pull with no uncommitted changes | Status returns empty result (all clean) |
| 3.3.2 | Modified file | Manually edit a file in the local repo path via FileRepository | Status shows the file as "Modified" |
| 3.3.3 | New untracked file | Place a new file in the local repo path | Status shows the file as "Untracked" |
| 3.3.4 | QueryStatus filter | With multiple files in different statuses, invoke `QueryStatus` with a search term | Only matching status entries returned |

### 3.4 Checkout (Switch Branch / Commit)

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 3.4.1 | Checkout existing branch | Create a branch via remote or CreateBranch. Invoke `Checkout` with the branch name | HEAD switches to that branch; `GetCurrentBranch` confirms |
| 3.4.2 | Checkout remote branch | Invoke `Checkout` with a branch that exists only on remote | A local tracking branch is auto-created and checked out |
| 3.4.3 | Checkout commit (detached HEAD) | Invoke `Checkout` with a specific commit hash | HEAD becomes detached; `GetCurrentBranch` reports `DetachedHEAD=true` |

### 3.5 Branch Management

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 3.5.1 | CreateBranch — from HEAD | Invoke `CreateBranch` with `BranchName="test-branch"` (no start point) | Branch "test-branch" is created at HEAD |
| 3.5.2 | CreateBranch — from specific commit | Invoke `CreateBranch` with a `StartPoint` of a commit hash | Branch created at that commit |
| 3.5.3 | GetBranchList | After creating branches, invoke `GetBranchList` | Returns all local and remote branches with name, short name, and type |
| 3.5.4 | GetCurrentBranch | Invoke `GetCurrentBranch` | Returns current branch name and `DetachedHEAD=false` |
| 3.5.5 | DeleteLocalBranch | Switch to a different branch. Invoke `DeleteLocalBranch` with the test branch name | Branch is deleted; `GetBranchList` no longer shows it |
| 3.5.6 | DeleteLocalBranch — cannot delete current | Invoke `DeleteLocalBranch` on the currently checked-out branch | Service returns an error |
| 3.5.7 | GetLocalBranches | Invoke `GetLocalBranches` | Only local branches returned |

### 3.6 Commit History & Diff

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 3.6.1 | GetCommitList | After several pushes, invoke `GetCommitList` | Returns list of commits with ID, name, and time for the current branch |
| 3.6.2 | GetCommitInfo | Pick a commit SHA from the list and invoke `GetCommitInfo` | Returns detailed info: ID, parents, author, date, committer, description, and changed files list |
| 3.6.3 | QueryDiffFileList | On a commit that modified multiple files, query by a filename | Only that file's entry is returned |
| 3.6.4 | GetDiffPerFile — modified file | After modifying an untracked file, invoke `GetDiffPerFile` with the filename | Returns the diff text showing changes in the working tree |
| 3.6.5 | GetDiffPerFile — untracked file | Invoke `GetDiffPerFile` on an untracked (new) file | Returns empty diff or the full file content as added |
| 3.6.6 | GetDiffPerFileBetweenCommits | Pick a commit SHA and a file changed in it. Invoke the service with that commit and filename | Returns diff of that file between that commit and its parent |
| 3.6.7 | MaxDiffSize limit | Create a large diff (exceeding `MaxDiffSize`, default 500000 chars). Invoke `GetDiffPerFileBetweenCommits` | Returns a truncated message indicating diff exceeds limit |

### 3.7 Merge

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 3.7.1 | Merge — fast-forward | Create branch from HEAD, commit on it. Checkout `main`. Invoke `Merge` with the feature branch | Fast-forward merge; `main` now includes the commit |
| 3.7.2 | Merge — merge commit | Make divergent commits on two branches. Checkout target. Invoke `Merge` with the source branch | Merge commit created |
| 3.7.3 | Merge — conflict | Make conflicting edits on two branches. Invoke `Merge` | Error returned indicating conflict |

### 3.8 Rebase

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 3.8.1 | Rebase — clean | Make commits on a feature branch. Invoke `Rebase` with `UpstreamBranch="main"` | Feature branch commits are replayed on top of `main` |
| 3.8.2 | Rebase — conflict | Make conflicting changes. Invoke `Rebase` | Error returned indicating conflict |

### 3.9 Tag Management

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 3.9.1 | CreateTag — lightweight | Invoke `CreateTag` with `TagName="v1.0-test"` and `TagType="LIGHTWEIGHT"` (default) | Tag created at HEAD |
| 3.9.2 | CreateTag — annotated | Invoke `CreateTag` with `TagName="v1.1-annotated"`, `TagType="ANNOTATED"`, `TagMessage="Release v1.1"` | Annotated tag created with message |
| 3.9.3 | GetTagList | Invoke `GetTagList` | Returns all tags with name, commit ID, message, and date |
| 3.9.4 | DeleteTag | Invoke `DeleteTag` with a tag name | Tag is deleted; `GetTagList` no longer shows it |

### 3.10 DeleteLocalRepoContent

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 3.10.1 | Delete repo content | Invoke `DeleteLocalRepoContent` | All files (including `.git` directory) are removed from the local repo path; Git repo is effectively reinitialized on next Push |

---

## 4. Export/Import Functionality

### 4.1 Export

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 4.1.1 | ExportProjectEntities — full project | Invoke `ExportProjectEntities` with a valid Project name and `ExportAllEntities=true` | All entities from that project are exported as XML to the local repo path |
| 4.1.2 | ExportProjectEntities — selective | Invoke `ExportProjectEntities` with specific entity names in `EntitiesList` | Only selected entities are exported |
| 4.1.3 | ExportProjectEntities — all entity types | Use a project containing Things, ThingTemplates, ThingShapes, Mashups, DataShapes, StateDefinitions, StyleThemes, etc. | All supported entity types (30+) are exported correctly |
| 4.1.4 | ExportProjectEntities — with auto-push | Invoke `ExportProjectEntities` with `commitMessage` parameter set to "Exported project entities" | After export completes, a git commit is automatically created and pushed to remote with the given message |
| 4.1.5 | ExportProjectEntities — auto-push without commitMessage | Invoke `ExportProjectEntities` without the `commitMessage` parameter | Export completes without auto-push (no commit created) |
| 4.1.6 | ExportProjectData | Invoke `ExportProjectData` targeting a project with DataTables, Streams, ValueStreams, Wikis, Blogs | Data is exported via DataExporter REST API |
| 4.1.7 | ExportProjectExtensions | Invoke `ExportProjectExtensions` | Installed extensions are exported to the repo via `ExtensionsExport.ExportExtensionsToRepository` |
| 4.1.8 | ExportLocalizationToken | Invoke `ExportLocalizationToken` with a `LocalizationTokensPrefix` set | Localization tables are exported; tokens not matching the prefix are filtered; `lastModifiedDate` and persistence provider attributes are removed |
| 4.1.9 | Configuration round-trip | Export entities → Push → Delete local content → Pull → Verify | Exported XML files are preserved round-trip through Git |
| 4.1.10 | RemoveLastModifiedDate | Inspect exported XML files | No `lastModifiedDate` attributes present |
| 4.1.11 | RemoveModelPersistenceProviderPackage | Inspect exported XML files | Persistence provider package attributes are stripped |

### 4.2 Import

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 4.2.1 | InitExtensionImportTargets | Invoke `InitExtensionImportTargets` on `GIT.Utility.Thing` | Extension import targets configured; `GitExtensionAppKey` ApplicationKey created |
| 4.2.2 | ImportEntity — single file | Invoke `ImportEntity` with `entityPath` pointing to a valid exported XML file within the FileRepository | Entity is imported into ThingWorx; response from PostMultipart is captured and logged |
| 4.2.3 | ImportEntity — with ignoreDependencies=true | Invoke `ImportEntity` with `ignoreDependencies=true` | Import succeeds even if the entity references missing dependencies |
| 4.2.4 | ImportEntity — with ignoreDependencies=false | Invoke `ImportEntity` with `ignoreDependencies=false` (default) | Import validates dependencies; fails if dependencies are missing |
| 4.2.5 | ImportEntity — missing GitExtensionAppKey | Delete the `GitExtensionAppKey` ApplicationKey. Invoke `ImportEntity` | Error thrown: "GitExtensionAppKey not found. Run InitExtensionImportTargets first." |
| 4.2.6 | ImportEntity — missing ExtensionImportTargets | Delete the `ExtensionImportTargets` Thing. Invoke `ImportEntity` | Error thrown: "ExtensionImportTargets not configured. Run InitExtensionImportTargets first." |
| 4.2.7 | ImportProjectEntities — bulk import all XMLs | Prepare a repository path containing multiple entity XML files (including subdirectories). Invoke `ImportProjectEntities` with `GitThingName` and optional `entityPath` | All XML files are recursively discovered and imported; returns an INFOTABLE summary with pass/fail per file; logs count of success/failure |
| 4.2.8 | ImportProjectEntities — with ignoreDependencies=true | Invoke `ImportProjectEntities` with `ignoreDependencies=true` | All entities imported even those with missing dependency references |
| 4.2.9 | ImportProjectEntities — invalid GitThingName | Invoke `ImportProjectEntities` with a non-existent `GitThingName` | Error thrown indicating GitThingName is required or Thing not found |

---

## 5. Credential Management

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 5.1 | Set credentials via services | On `GIT.Utility.Thing`, invoke `AddGitCredentials` (or similar service) with valid git credentials | Credentials stored in UserExtension property `GitCredentials` |
| 5.2 | Set credentials base64-encoded | Submit credentials with base64-encoded username/password | Credentials decoded and stored correctly |
| 5.3 | Credentials are PASSWORD type | Inspect `GitCredentials` property configuration | `GitCommitterPassword` field type is `PASSWORD` |
| 5.4 | Override committer identity | On the user's UserExtension properties, set `GitCommitterName`, `GitCommitterEmail`, and `UseGitCommitUserValues=true` | Push uses the overridden values instead of Thing-level ones |
| 5.5 | Disable override | Set `UseGitCommitUserValues=false` | Push falls back to Thing-level committer values |
| 5.6 | Missing credentials | Attempt a Push/Pull without any credentials for that Git Thing | Error returned pointing to missing credentials |

---

## 6. GPG Commit Signing

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 6.1 | VerifyGpgKey — valid key | Generate a PGP key pair. Invoke `VerifyGpgKey` with the ASCII-armored private key and passphrase | Returns success with key fingerprint |
| 6.2 | VerifyGpgKey — invalid key | Invoke `VerifyGpgKey` with random text | Returns error indicating invalid key |
| 6.3 | VerifyGpgKey — wrong passphrase | Invoke `VerifyGpgKey` with a valid key and incorrect passphrase | Returns error (decryption failure) |
| 6.4 | VerifyGpgKey — base64 encoded | Submit a base64-encoded ASCII-armored key | Auto-decoded; returns success with fingerprint |
| 6.5 | SetGpgKey — auto-initializes GpgKeys property | Before running this test, delete or clear the `GpgKeys` property from the `UserExtensions` ThingShape. Invoke `SetGpgKey` with key details | Service auto-creates the `GpgKeys` property on `UserExtensions` ThingShape, then stores the key. No "property not found" error occurs |
| 6.6 | SetGpgKey — normal operation | Invoke `SetGpgKey` with key, passphrase, and `SignCommits=true` | Key stored in `GpgKeys` UserExtension property |
| 6.7 | Push with GPG signing | After 6.6, invoke `Push` with a commit message | Commit is GPG-signed; remote shows "Verified" |
| 6.8 | Push without signing | Set `SignCommits=false` for the repo. Invoke `Push` | Commit is not signed (no "Verified" badge) |
| 6.9 | Multiple GPG keys | Store different GPG keys for two different GitBackup Things | Each Thing uses its own key when signing |

---

## 7. Proxy Support

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 7.1 | Configure proxy | Set `UseProxy=true`, `ProxyURL` and `ProxyPort` on a Thing's Configuration table | Setting saved |
| 7.2 | Push via proxy | With proxy configured and a proxy server running, invoke `Push` | Git operations route through the proxy |
| 7.3 | Push without proxy (disabled) | Set `UseProxy=false`. Invoke `Push` | Git operations connect directly (not through proxy) |
| 7.4 | Invalid proxy | Set `UseProxy=true` with a non-existent proxy host | Push/Pull fail with a connection error |

---

## 8. Mashup UI Verification

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 8.1 | Main Dashboard | Navigate to `GitBackup.Main.Mashup` | Dashboard loads with list of GitBackup Things |
| 8.2 | Create New Repo (NewRepo Mashup) | Open `GitBackup.NewRepo.Mashup` and fill in all fields | New GitBackup Thing is created |
| 8.3 | Modify Repo | Open `GitBackup.ModifyRepo.Mashup` for an existing Thing | Configuration fields are pre-populated; save updates them |
| 8.4 | Status Mashup | Open `GitBackup.Status.Mashup` for a Thing | Displays working tree status table |
| 8.5 | Push Mashup | Open `GitBackup.Push.Mashup`; enter commit message; click Push | Push executes; success/failure displayed |
| 8.6 | Pull Mashup | Open `GitBackup.Pull.Mashup`; click Pull | Pull executes; result displayed |
| 8.7 | Commit History | Open `GitBackup.CommitHistory.Mashup` | List of commits displayed; clicking a commit shows details |
| 8.8 | Branch Manager | Open `GitBackup.BranchManager.Mashup` | List of branches; can create, delete, switch |
| 8.9 | Checkout Mashup | Open `GitBackup.Checkout.Mashup` and enter branch name or commit hash | Checkout executes |
| 8.10 | Merge / Rebase Mashup | Open `GitBackup.MergeRebase.Mashup` and select source branch | Merge/rebase executes |
| 8.11 | Export Mashup | Open `GitBackup.Export.Mashup`; select project/entities; click Export | Entities exported to repo path |
| 8.12 | Log Mashup | Open `GitBackup.Log.Mashup` | Operation log entries displayed |
| 8.13 | Push Settings | Open `GitBackup.PushSettings.Mashup` | Commit message and author fields; push executes with custom params |
| 8.14 | GPG Key Settings | Open `GitBackup.GpgKeySettings.Mashup` | Paste key, set passphrase, verify, save |
| 8.15 | Extension Status | Open `GitBackup.ExtensionStatus.Mashup` | Extension health and version info displayed |
| 8.16 | Entity Picker | In Export flow, open `GitBackup.EntityPicker.Mashup` | Entity tree with checkboxes; selected entities are added to export list |
| 8.17 | Delete Confirmation | Delete a GitBackup Thing; `GitBackup.ConfirmDeleteThing.Mashup` appears | Confirmation dialog shown; confirm deletes; cancel aborts |

---

## 9. Logging

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 9.1 | Log entry created on Push | Perform a Push | Entry created in `GitBackup.Log` DataTable with ServiceName="Push", Source, User, timestamp, Content |
| 9.2 | Log entry on Pull | Perform a Pull | Corresponding log entry created |
| 9.3 | Log entry on Status | Perform a Status | Log entry created |
| 9.4 | Log entry on error | Attempt a Push without credentials | Error is logged with descriptive Content |
| 9.5 | View Log Mashup | Open `GitBackup.Log.Mashup` | All log entries visible, filterable |

---

## 10. Error & Edge Cases

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 10.1 | Invalid GitRepoURL | Set an invalid URL, attempt Push | Descriptive error returned |
| 10.2 | Network failure | Disconnect network, attempt Push/Pull | Error about network/fetch failure |
| 10.3 | Non-existent FileRepository | Set `FileRepository` to a non-existent Thing | Error on any Git operation |
| 10.4 | Non-existent RepoPathName | Set `RepoPathName` to a directory that doesn't exist | Path is created automatically on first operation |
| 10.5 | Very large commit | Create 1000+ files in repo, attempt Push | Push succeeds (may take longer) |
| 10.6 | Special characters in commit message | Push with emojis, Unicode, quotes, newlines in commit message | Commit is created successfully; message preserved |
| 10.7 | Concurrency | Invoke Push and Pull simultaneously from two browser tabs | One operation fails with a "repository locked" or similar error (JGit locking) |
| 10.8 | Detached HEAD then branch | Checkout a commit, then create and checkout a branch from there | Branch is created from the detached commit |
| 10.9 | Rebase with uncommitted changes | Have uncommitted changes, then invoke Rebase | Service should either auto-stash or return an error |

---

## 11. Migration / Upgrade

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 11.1 | Upgrade from v4.x to v6.x | Import v6.0.0 over an existing v4.x installation | `ExtMigrator` runs; UserExtension properties (`GitCredentials`, `GpgKeys`, `GitCommitterEmail`, `GitCommitterName`, `UseGitCommitUserValues`) are added to all users; existing data preserved |
| 11.2 | Upgrade from v5.x to v6.0.0 | Import v6.0.0 over an existing v5.x installation | `ExtMigrator` runs; `GpgKeys` UserExtension property initialized for all users if missing; no data loss |
| 11.3 | GpgKeys auto-initialization on SetGpgKey | On a fresh install where GpgKeys was never used, invoke `SetGpgKey` | Service auto-creates the `GpgKeys` property on the `UserExtensions` ThingShape and initializes it; no manual setup required |
| 11.4 | Downgrade not supported | Attempt to install an older version over v6.0.0 | Platform should block or warn about downgrade |

---

## 12. Security

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 12.1 | GPG key visibility | User A stores a GPG key. User B accesses `GpgKeys` on User A's UserExtension | User B cannot read User A's GPG key (password field masked) |
| 12.2 | Credential isolation | User A stores credentials for Thing X. User B uses Thing X | User B cannot see User A's credentials |
| 12.3 | REST API base64 encoding | Submit credentials or GPG keys via REST API with base64 encoding | Service decodes and stores correctly |

---

## 13. SSH Support (if applicable)

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 13.1 | SSH remote URL | Set `GitRepoURL` to an `ssh://` URL and configure SSH credentials | Push/Pull succeed over SSH (JSch transport) |

---

## 14. Docker Dev Environment

| # | Test Case | Steps | Expected Result |
|---|---|---|---|
| 14.1 | `docker-compose up` | Run `docker compose up -d` | PostgreSQL, ThingWorx 9.7.5, Gitea 1.22.3 start |
| 14.2 | Gitea availability | Browse to `http://localhost:3000` | Gitea login page loads; `testadmin` can log in |
| 14.3 | ThingWorx availability | Browse to ThingWorx Composer URL | Composer loads; can log in as Administrator |
| 14.4 | `devSetup` end-to-end | Run `./gradlew devSetup` | Extension is built, stack is started, extension is installed, Gitea test repo is created |
