# ThingWorx GitBackup Extension

[Unofficial/Not Supported] Fork of the ThingWorx GitBackup Extension. Provides Git integration (push, pull, branch, merge, rebase, tag, diff, GPG signing, bulk import/export) for managing ThingWorx application entities under source control.

## Features

- **Git operations**: init, add, commit, push, pull, status, checkout, branch, merge, rebase, tag, diff — all through ThingWorx services and mashups
- **GPG commit signing**: PGP key management via UI, per-repository signing toggle, per-user key isolation
- **Bulk import/export**: export full projects (entities + data + extensions) to Git in one action; batch-import all XMLs from a repository with recursive discovery and pass/fail summary
- **Auto-push**: export can automatically commit and push to remote
- **Credential & proxy management**: per-user credentials stored as `PASSWORD` type, base64-encoded API support, per-repository proxy configuration
- **Logging**: all Git operations logged to a DataTable with timestamps and user context
- **Containerized dev environment**: Docker Compose (PostgreSQL 15, ThingWorx 9.7.5, Gitea 1.22.3)
- **Multi-platform testing**: Testcontainers integration tests against ThingWorx 9.5.0, 9.6.3, 9.7.5, 10.1.0
- **CI/CD**: GitHub Actions build, test, and auto-publish releases

## Architecture

```
┌─────────────────────────────────────────────┐
│  Mashups (22)                               │
│  BranchManager, CommitHistory, MergeRebase, │
│  GpgKeySettings, Push, Pull, Status, Export,│
│  Import, Log, ExtensionStatus, ...          │
├─────────────────────────────────────────────┤
│  Things / ThingShapes / DataShapes (55 XML) │
│  GIT.Utility.Thing (utility services)       │
│  GIT.Repository entities                         │
├─────────────────────────────────────────────┤
│  Java Backend (src/gitbackup/extension/)     │
│  GitRepositoryShape      — JGit-backed services │
│  PastedKeyGpgSigner  — BouncyCastle GPG     │
│  ExtMigrator         — upgrade migrations   │
│  GitBackupValidation — config validation    │
├─────────────────────────────────────────────┤
│  Testcontainers (src/gb/tests/)             │
│  Postgres → DBInit → ThingWorx → Gitea     │
│  Git operations, GPG, entity, UI tests     │
├─────────────────────────────────────────────┤
│  Build: Gradle 8.12  │  CI: GitHub Actions  │
│  Dependencies: JGit, BC, JSch, JavaEWAH     │
└─────────────────────────────────────────────┘
```

### Java Backend (`src/gitbackup/extension/`)

- **`GitRepositoryShape.java`** (1331 lines) — `GitRepositoryShape` ThingShape implementation. Provides all Git services backed by JGit 7.6.0.
- **`GitUtilityThingShape.java`** — `GIT.Utility.ThingShape` implementation. Manages repos, credentials, GPG keys, project export/sync, and user-level config.
- **`PastedKeyGpgSigner.java`** (156 lines) — Implements JGit's `Signer` interface using BouncyCastle. Accepts ASCII-armored PGP keys, produces GPG signatures for commits.
- **`ExtMigrator.java`** — Runs on extension upgrade. Initializes UserExtension properties and GpgKeys.
- **`GitBackupValidation.java`** — Resource with configuration validation service.
- **`Const.java`** — Constants for configuration names, entity names, credential/proxy/GPG fields.

### ThingWorx Entities (`Entities/`)

55 XML files defining the ThingWorx-side configuration, UI, and JavaScript logic:

| Type | Count | Examples |
|---|---|---|
| DataShapes | 14 | `GIT.BranchList.DataShape`, `GIT.GpgKeyVerificationResult.DataShape`, `GIT.RepositoryConfiguration.UserExtension.DataShape` |
| Mashups | 22 | BranchManager, CommitHistory, MergeRebase, GpgKeySettings, Push, Pull, Status, Export, Import, Log, ExtensionStatus |
| Things | 3 | `GIT.Utility.Thing`, `GIT.ExtensionLog.DataTable` |
| ThingShapes | 2 | `GIT.Utility.ThingShape`, `GIT.Repository.ThingShape` |
| StateDefinitions | 0 | None currently exported |
| Other | 7 | Media, Projects, StyleThemes |

### Thing Configuration

Each GitBackup Thing has a `Configuration` table with these fields:

| Field | Type | Description |
|---|---|---|
| `GitRepoURL` | STRING | Remote Git repository URL |
| `FileRepository` | STRING | ThingWorx FileRepository for local storage |
| `RepoPathName` | STRING | Subdirectory within the FileRepository |
| `BranchName` | STRING | Initial/default branch name |
| `UseProxy` | BOOLEAN | Enable proxy for Git operations |
| `ProxyURL` | STRING | Proxy host |
| `ProxyPort` | INTEGER | Proxy port |
| `LocalizationTokensPrefix` | STRING | Prefix filter for localization token export |

### GPG Key Configuration (per-user)

Stored in UserExtension properties (one set per platform user):

| Property | Type | Description |
|---|---|---|
| `GpgKeys` | PASSWORD | JSON blob of GPG private keys |
| `SignCommits` | BOOLEAN | Enable GPG signing per repository |
| `GitCredentials` | PASSWORD | Per-user Git credentials |
| `GitCommitterEmail` | STRING | Override committer email |
| `GitCommitterName` | STRING | Override committer name |
| `UseGitCommitUserValues` | BOOLEAN | Use overrides instead of Thing-level values |

## Prerequisites

- **JDK 21** — Corretto recommended. The `fetchJdk21` task can download and cache it automatically.
- **ThingWorx Extension SDK 9.6.0** — Download from PTC support portal, place at `twx-lib/MED-61098-CD-096_9-6-0_ThingWorx-Extension-SDK-9-6-0.zip` (gitignored).
- **Docker** — Required for the development environment and integration tests.

## Quick Start

```bash
# Build the extension
./gradlew build

# Start dev environment (builds, starts stack, installs extension, creates test repo)
./gradlew devSetup
```

Set container registry credentials in `.env` (gitignored) at the project root:

```
LS_USERNAME=your-license-server-username
LS_PASSWORD=your-license-server-password
```

### Output Artifacts

Produced in `build/distributions/`:

- **`GitBackupExtension.zip`** — The extension package (JAR + entities + metadata)
- **`GitBackupExtensionPack.zip`** — Extension plus third-party dependencies

### Gradle Tasks

| Task | Description |
|---|---|
| `build` | Cleans and packages the extension (default) |
| `packageExtension` | Package extension ZIP |
| `packagePack` | Package extension pack ZIP |
| `devSetup` | Full dev environment: build → start stack → install extension → create test repo |
| `test` | Run integration tests (disabled in `build`) |
| `fetchJdk21` | Download and cache Corretto JDK 21 |
| `fetchLemminx` | Download XML language server |
| `composeUp` | Start Docker Compose stack |
| `composeDown` | Stop stack |
| `composeDownClean` | Stop stack and delete volumes |
| `composeLogs` | Follow container logs |
| `bumpVersion` | Bump version (`-Pbump=major|minor|patch` or `-PnewVersion=X.Y.Z`) |

## Development Environment

The Docker Compose stack provides a complete development setup:

- **PostgreSQL 15** — Database for ThingWorx
- **ThingWorx 9.7.5** — Platform instance (devopscadit image)
- **Gitea 1.22.3** — Self-hosted Git server for testing

After `./gradlew devSetup`:

```
ThingWorx:  http://localhost:8080/Thingworx  (Administrator / AdminP@ssw0rd!123)
Gitea:      http://localhost:3000             (giteauser / giteapass123)
PostgreSQL: localhost:5432                    (postgres / twx_password_123)
```

Internal URL for thing config: `http://gitea:3000/giteauser/gitbackup-test-repo.git`

## Testing

### Integration Tests (Testcontainers)

JUnit 5 tests that spin up real containers, install the extension, and run Git operations against Gitea via the ThingWorx REST API:

```bash
# Require Docker; JDK 21 auto-downloaded if missing
./gradlew test
```

| Test | What it covers |
|---|---|
| `GiteaGitOperationsTest` | Push, Pull, Status, Checkout, branches, merge, rebase, tags, GPG signing |
| `ThingWorxEntitiesTest` | Parameterized across 9.5.0, 9.6.3, 9.7.5, 10.1.0 |
| `GpgKeyVisibilityTest` | GPG key persistence, isolation, DataShape access |
| `ThingWorxIntegrationTest` | DB init, platform health, extension installation |



## CI/CD — GitHub Actions

The workflow (`.github/workflows/build-and-publish.yml`) does:

- **On push/PR to `main`**: builds with JDK 21 (Corretto), uploads both ZIPs as build artifacts
- **On tags matching `v*`**: creates a GitHub Release with auto-generated notes, attaches both ZIPs

### Release Process

```bash
# Bump version (auto-commits and tags)
./gradlew bumpVersion -Pbump=major     # 6.0.0 → 7.0.0
./gradlew bumpVersion -Pbump=minor     # 6.0.0 → 6.1.0
./gradlew bumpVersion -Pbump=patch     # 6.0.0 → 6.0.1
./gradlew bumpVersion -PnewVersion=6.2.0  # explicit

# Push tags to trigger release
git push origin main --tags
```

## Manual Test Plan

See `MANUAL_TEST_PLAN.md` for a comprehensive 295-line test plan covering all features: installation, Git operations, export/import, credential management, GPG signing, proxy, mashup UI, logging, edge cases, migration, security, SSH, and Docker dev environment.

## License

MIT — see `LICENSE`. This extension is provided as-is without warranty or support. It is not part of the PTC product suite.
