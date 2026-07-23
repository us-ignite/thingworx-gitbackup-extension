# Changelog

## Branching Strategy

**Upstream** (PTCInc) maintained `v8.x` and `v9.x` branches, where the branch name reflected the ThingWorx platform version. Extension versions were tied to platform versions: V4.x for ThingWorx 9.0, V5.x for ThingWorx 9.3+.

**Fork** (US Ignite / Ryan Guild) split from upstream `v9.x` at upstream v5.1.0 and decoupled extension versioning from platform compatibility. The fork uses a single `main` branch with semver tags. The next major release is v6.0.0 — a clean break from the upstream numbering scheme.

```
v3.0.1 ─ v4.0.0 ─ v4.0.1 ─ v4.1.0 ─ v5.0.0 ─ v5.0.1 ─ v5.1.0 ─┤
  (TW 9.0)         (TW 9.0)  (TW 9.0)  (TW 9.3)  (TW 9.3)  (TW 9.3)  │
                                                                       │ fork
  v5.2.2 ─ v5.2.3 ─ v5.2.4 ─ v5.2.5 ─ v5.2.6 ─ v5.2.7 ─────── v6.0.0
  (TW 9.3+)                                          (next major)
```

---

## v6.0.0 (next)

First fork release. Extension versioning is now decoupled from the ThingWorx platform version. Supports TW 9.3 through 10.1.0.

### Naming and compatibility cleanup

- Git-native DataShapes now use `GIT.*`; extension-owned structures use `GITBACKUP.*`.
- Utility and repository entities use `GITBACKUP.Utility.*` and `GIT.Repository.*`.
- Credentials and GPG keys are now `GIT.GitCredentials.DataShape` and `GIT.GpgKey.DataShape`.
- Existing repository Things are migrated to the canonical repository template; legacy utility and Java package names remain compatibility surfaces during the transition.

### Build System & Project Structure

- Refactored Java source from `src/gb/` to `src/gb/extension/` package
- Migrated from Ant/Eclipse to **Gradle 8.12** with Gradle Wrapper
- Replaced embedded JGit source tree (~2000+ files) with Maven Central dependency (JGit 7.6.0)
- Added `bumpVersion` Gradle task for automated semver bumping with git tag
- Added `devSetup` task: one-command build → deploy → test repo creation
- Added `fetchJdk21` task: auto-downloads and caches Corretto JDK 21
- Dropped Eclipse `.classpath`/`.project` from version control; now `.gitignore`d

### GPG Commit Signing

- PGP key management via `PastedKeyGpgSigner` (BouncyCastle-backed implementation of JGit `Signer`)
- `VerifyGpgKey` service: validates ASCII-armored private keys, returns fingerprint
- `SetGpgKey` service: stores keys in per-user `GpgKeys` property, auto-initializes if missing
- `SignCommits=true/false` toggle per repository on Push
- GPG key isolation: each user's keys stored in their UserExtension properties
- `GpgKeySettings` mashup for key management UI
- Warning logged when signing is disabled but the remote may require it
- Key material zeroed in memory via `clearSensitiveData()`

### Entity & DataShape Restructuring

- Standardized DataShape file naming (dropped `.DataShape` suffix)
- New DataShapes: `GitBackup.GpgKey`, `GitBackup.ExtensionVersion`, `GitBackup.UserExtensionProperties`
- Rewired mashups: ConfirmDeleteThing, Main, ModifyRepo, Pull, PushSettings, Status, NewRepo
- New mashups: `ExtensionStatus`, `Version`, `LoadingPopup`
- New ThingShape: `TestingTS`

### New Mashups

- **BranchManager**: view, create, delete, rename branches
- **CommitHistory**: browse commit history with per-commit details
- **MergeRebase**: merge and rebase from the UI
- **GpgKeySettings**: paste key, set passphrase, verify, save

### Containerized Development Environment

- Docker Compose stack: PostgreSQL 15, ThingWorx 9.7.5, Gitea 1.22.3
- All containers networked together; JDK 21 auto-downloaded and mounted into ThingWorx
- Extension auto-installed via REST API multipart upload (`devSetup`)
- Gitea admin user and test repository created automatically

### Integration Testing with Testcontainers

- **`GitBackupExtensionTestStack`**: orchestrates Postgres → DBInit → ThingWorx → (Gitea) → ExtensionInstaller
- **`GiteaGitOperationsTest`**: end-to-end Git operations against a real Gitea container (Push, Pull, Status, Checkout, branches, merge, rebase, tags, GPG signing)
- **`ThingWorxEntitiesTest`**: parameterized across ThingWorx 9.5.0, 9.6.3, 9.7.5, 10.1.0
- **`GpgKeyVisibilityTest`**: validates GPG key persistence, isolation, and DataShape accessibility
- **`ThingWorxIntegrationTest`**: DB init, platform health, extension install verification
- **Puppeteer UI tests**: 19 Jest specs covering all mashup flows

### CI/CD with GitHub Actions

- Build workflow on push/PR to `main`: JDK 21 (Corretto), Gradle build, both ZIPs uploaded as artifacts
- Publish workflow on `v*` tags: creates GitHub Release with auto-generated notes, attaches both ZIPs
- Uses `gh` CLI for release creation (replaced deprecated `softprops/action-gh-release`)
- Stale issue management: daily cron

### Enhanced Git Operations

- **Push**: optional `AuthorName`/`AuthorEmail` override; per-user `UseGitCommitUserValues`; improved pre-receive hook declined error with GPG hint; timings logged
- **Pull**: `Force=true` option for hard reset to remote HEAD
- **Status**: `QueryStatus` with search/filter parameter
- **Commit Info**: `GetCommitInfo` returns changed files list; GPG signature detection
- **Diff**: `GetDiffPerFile` for working tree; `GetDiffPerFileBetweenCommits` for history; `MaxDiffSize` limit with truncation message
- **Checkout**: auto-creates remote-tracking branches; detached HEAD support; `GetCurrentBranch` returns `DetachedHEAD` flag
- **Logging**: all operations logged to `GitBackup.Log` DataTable

### Export / Import

- `ExportProjectEntities`: new optional `commitMessage` parameter for auto-push after export
- `ImportProjectEntities`: bulk batch import — recursively discovers XML files, imports all, returns pass/fail summary
- `ImportEntity`: new `ignoreDependencies` parameter; null-check for `GitExtensionAppKey`; PostMultipart response captured and logged

### Credential & Proxy Management

- Per-user credential storage in `GitCredentials` UserExtension property
- `GitCommitterPassword` stored as `PASSWORD` type (masked in UI)
- Base64-encoded credential/GPG key submission via REST API
- Proxy configuration per repository: `UseProxy`, `ProxyURL`, `ProxyPort`

### Migration & Upgrade

- `ExtMigrator` v2.3.0: initializes UserExtension properties on all users
- `ExtMigrator` v5.2.0: initializes `GpgKeys` property on all UserExtensions
- `SetGpgKey` auto-creates `GpgKeys` property on `UserExtensions` ThingShape if missing

### Bug Fixes

- Null-check for `GitExtensionAppKey` in `ImportEntity`
- Auto-initialize `GpgKeys` property in `SetGpgKey` if missing
- Improved push error handling for pre-receive hook declined — includes GPG hint
- FileRepository path concatenation uses `File.separator` (cross-platform fix)
- `PostMultipart` response captured and logged in `ImportEntity`

### Dependencies

- `org.eclipse.jgit:7.6.0.202603022253-r` (replaces embedded source)
- `com.jcraft:jsch:0.1.55` — SSH transport
- `org.bouncycastle:bcpg-jdk18on:1.72` — GPG signing
- `com.googlecode.javaewah:JavaEWAH:1.2.3`
- Test: JUnit Jupiter 5.8.2, Testcontainers 2.0.5, Gson 2.10.1, org.json
- ThingWorx Extension SDK 9.6.0 (downloaded separately)

---

## Fork History (v5.1.1 — v5.2.7)

The fork maintained temporary semver on the v5.x line while working toward v6.0.0. These releases backported changes from the upstream v5.1.0.

### v5.2.7 (2026-06-02)

- **Bug 1**: Add null-check for `GitExtensionAppKey` in `ImportEntity` — throws clear error if missing
- **Bug 2**: Auto-initialize `GpgKeys` property in `SetGpgKey` if the property doesn't exist on `UserExtensions`
- **Bug 3**: Improve push error handling for pre-receive hook declined — now shows explicit message with GPG hint
- **Bug 4**: Fix `FileRepository` path concatenation to use `File.separator` (cross-platform)
- **Bug 5**: Capture `PostMultipart` response in `ImportEntity` and log result (was silently discarded)
- **Bug 6**: Add optional `commitMessage` param to `ExportProjectEntities` for auto-push
- **Bug 7**: Add `ImportProjectEntities` bulk/batch import service — recursively discovers XML files, imports all, returns summary
- **Bug 8**: Add `ignoreDependencies` parameter to `ImportEntity`

### v5.2.6

Version bump only.

### v5.2.5

- Keep `GitBackupExtension.zip` after `packagePack` (was being deleted)
- Update ThingWorx SDK to 9.6.0

### v5.2.4

Version bump only.

### v5.2.3

Refined `build.gradle` dependencies and version configuration.

### v5.2.2 (2026-05-27)

- Replace `softprops/action-gh-release` with `gh` CLI in CI to comply with org action policy
- Sync version scheme

### v5.2.1 / v5.1.1 (2026-05-27)

- **Signed commit tests**: Added `GpgKeyVisibilityTest` — verifies GPG key DataShape accessibility via REST, round-trip `GetGpgKey`/`SetGpgKey`, unrelated DataShape isolation
- Added `ExtensionStatus` and `Version` mashups
- Fixed mashup wiring and entity references across all 22 mashups
- Package refactoring: all registered things now use correct `gb.extension.*` class names

### v11.0.0-rc.1 (2026-05-22)

First fork tag. Squash of all fork work at the time. This was the "everything in one giant commit" that consolidated:
- GPG signing with containerized testing (full BouncyCastle integration)
- Docker Compose dev environment (PostgreSQL, ThingWorx 9.7.5, Gitea)
- Testcontainers integration test framework
- Rewrote `GiteaGitOperationsTest` end-to-end
- Rewrote README for Gradle/Docker workflow
- New mashups: BranchManager, CommitHistory, MergeRebase
- CI/CD: GitHub Actions build-and-publish
- Extension auto-installer for dev environment

---

## Fork Early Attempts

### backup-before-split (2026-05-11)

Tagged on an abandoned early fork attempt. This was the first pass at GPG signing and containerized testing, built on an initial Ant→Gradle migration. The approach was restarted from scratch with cleaner separation, becoming the `main` branch history above.

---

## Upstream History (PTCInc)

### v5.1.0-9.3 (2025-07-22)

Target: ThingWorx 9.3.

- Platform update for ThingWorx 9.3 compatibility
- Added GitHub Actions workflow for stale issue management (daily cron)
- README updates

### v5.0.1-9.3 (2024-06-05)

Target: ThingWorx 9.3.

- 3 bug fixes
- Various cleanup (unused libraries, classpath/project files)
- Improved readability and punctuation in UI strings (#56)

### v5.0.0-9.3 (2023-05-15)

Target: ThingWorx 9.3.

- Major version bump for ThingWorx 9.3 support
- Updated `build-extension.xml` for the new TW 9.3 SDK
- Removed unused Bitbucket ThingShape
- Updated `metadata.xml` with new dependencies
- Updated third-party JAR libraries
- Eclipse project file cleanup

### v4.1.0-9.0 / v4.1.0 (2020-10-07)

Target: ThingWorx 9.0.

- **Log capability**: all Git operations logged to a DataTable
- **Auto-prune**: remote branches that are closed/deleted are automatically pruned locally
- UI support for deleting a local branch

### v4.0.1-9.0 / v4.0.1 (2020-09-07)

Target: ThingWorx 9.0.

- Renamed `InfotableSelector` to `GitInfotableSelector` to avoid naming conflicts
- Documentation updates

### v4.0.0-9.0 / v4.0.0 (2020-08-31)

Target: ThingWorx 9.0.

- Major version for ThingWorx 9.0 compatibility
- Extension SDK upgraded to 9.0
- Initial 9.0 platform support

### v4.0.0 (2020-08-31)

Version for the v8.x branch (parallel release for ThingWorx 8.x platform).

- Equivalent feature set to v4.0.0-9.0 but for TW 8.x
- Entity picker with tag filtering support (PR #38 from jmccuen)

### v3.0.1 (2020-08-19)

Initial release. ThingWorx GitBackup Extension for ThingWorx 8.x.

- Basic Git integration: push, pull, status, checkout, branch management
- Export/import of ThingWorx entities to Git
- Ant/Eclipse build system with embedded JGit source
- MIT license
