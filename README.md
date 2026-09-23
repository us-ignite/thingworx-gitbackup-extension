# US Ignite ThingWorx Extensions

This monorepo contains ThingWorx extensions and reusable Java libraries maintained by US Ignite.

- [JGit extension](apps/thingworx-jgit-extension/README.md) — Git integration for ThingWorx entities.
- [ThingWorx DAP](libraries/thingworx-dap/README.md) — annotation processor for generated
  ThingWorx entity XML.
- [ThingWorx DAP runtime](libraries/thingworx-dap-runtime/README.md) — InfoTable and typed
  service-result helpers for DAP consumers.

The local development stack remains available at the repository root:

```bash
docker compose up -d
```

The current public repository and documentation URLs remain unchanged until the planned fork
detachment and repository migration are complete.

## Repository structure — depth-2 `<type>/<artifact>`

Every buildable output lives at exactly one level below a **type** directory. The Gradle subproject
is `:<type>:<artifact>` (see `settings.gradle:11`). This enforces a uniform layout, discovery via
`build.gradle:103` aggregators, and per-component versioning.

```
<type>/<artifact>/        # e.g. apps/thingworx-jgit-extension
  .version                # (if independently versioned)
  build.gradle            # subproject build
  README.md
  src/ | templates/ | Dockerfile | Chart.yaml  # type-specific
```

| Type | Artifact (Gradle path) | Purpose | Published output |
|------|------------------------|---------|------------------|
| `apps` | `thingworx-jgit-extension` (`:apps:thingworx-jgit-extension`) | ThingWorx extension (JGit + DAP runtime) | `build/distributions/JGitExtension-v<version>-<twx>.zip` per ThingWorx 9.6/9.7/10.0/10.1 |
| `apps` | `thingworx-operator` (`:apps:thingworx-operator`) | Kubernetes operator (Java) | `build/libs/*.jar` + OCI image `ghcr.io/us-ignite/thingworx/thingworx-operator:<version>` |
| `libraries` | `thingworx-dap` (`:libraries:thingworx-dap`) | DAP annotation processor (`annotationProcessor`) | `io.github.us-ignite:thingworx-dap:<version>` (GitHub Packages) |
| `libraries` | `thingworx-dap-runtime` (`:libraries:thingworx-dap-runtime`) | DAP InfoTable helpers (runtime `implementation`) | `io.github.us-ignite:thingworx-dap-runtime:<version>` |
| `libraries` | `thingworx-testcontainers` (`:libraries:thingworx-testcontainers`) | Shared Testcontainers helpers | (internal, no independent version) |
| `libraries` | `git-hooks` (`:libraries:git-hooks`) | `thingworx.git-hooks` Gradle plugin + hook tasks | (build-time only, `0.1.0`) |
| `images` | `thingworx-operator` (`:images:thingworx-operator`) | Operator `Dockerfile` + `buildImage`/`pushImage` | Same OCI tags as `:apps:thingworx-operator` |
| `images` | `platform` / `security-tool` / `connection-server` | Vendor-derived ThingWorx container builds (pinned to `vendor/*.zip|*.gz`, not `.version` — see below) | `ghcr.io/us-ignite/thingworx/platform-postgres:<platformVersion>[-<jdk>]` etc. |
| `charts` | `thingworx-operator` | Helm chart for the operator | `charts/thingworx-operator/Chart.yaml` version = operator version |
| `examples` | `thingworx-dap-usage` / `thingworx-operator-deployments` | Example consumers / sample CRs | (not published) |

To add a new component: create `<type>/<name>/` with `build.gradle`, map it in
`settings.gradle:11`, and if independently versioned, add `<type>/<name>/.version`
(the root `build.gradle:8` reads it as `project.version`). CI and root aggregators
(`buildAll`, `checkAll`, `chartTest`) pick it up automatically.

## Versioning

Versioning is per-artifact and anchored to the depth-2 layout above.

### Single source of truth

| Component | `.version` file | Current | How build gets it |
|-----------|-----------------|---------|-------------------|
| JGit extension | `apps/thingworx-jgit-extension/.version` | `6.4.0` | `root build.gradle:10` `file('.version').text` / `apps/.../build.gradle:71` `packageVersion` |
| ThingWorx DAP (core + runtime, must match) | `libraries/thingworx-dap/.version` + `libraries/thingworx-dap-runtime/.version` | `0.4.0` | Each `build.gradle:24` `file('.version').text` |
| ThingWorx Operator (canonical) | `apps/thingworx-operator/.version` | `0.2.0` | Mirrored to `charts/thingworx-operator/.version`; `Chart.yaml` `version`+`appVersion` and `images/thingworx-operator/build.gradle:5` `OPERATOR_VERSION` are derived/verified |

Unversioned subprojects (`thingworx-testcontainers`, `examples/*`, `images/platform` etc.)
fall back to `0.1.0-SNAPSHOT` (`build.gradle:18`).

### Hook-managed SemVer (JGit + DAP)

The `thingworx.git-hooks` plugin (`libraries/git-hooks:17`, applied in `build.gradle:3`) installs
a `prepare-commit-msg` hook (`settings.gradle:36`). On commit it:

1. Inspects staged paths (ignores `.version` itself): `apps/thingworx-jgit-extension/**` → JGit target, `libraries/thingworx-dap*/**` → DAP targets.
2. Requires a Conventional Commit header (`PrepareCommitMsgTask.groovy:46` `^[a-z][a-z0-9-]*(\([^)]+\))?(!)?:\s+.+$`) when components changed.
3. Maps header to SemVer bump (`VersionUtils.groovy:7`): `!:` or `BREAKING CHANGE:` → `major`, `feat:` → `minor`, `fix:`/`perf:` → `patch`, otherwise `none` (docs/chore/etc. must not touch `.version`).
4. Computes next version from `HEAD:<.version>` (or `bootstrapVersion:26` `6.0.6` JGit / `0.1.0` DAP for new files) via `nextVersion()`, writes `.version` and `git add`s it, aborting the commit with `version files were updated and staged; rerun git commit with the same message`.
5. Direct `.version` edits are rejected. DAP core/runtime mismatch is rejected. CI replays the same policy on `HEAD^..HEAD` via `ValidateVersioningTask.groovy:24` / `.github/scripts/validate-versioning.sh` (`build-and-publish.yml:30`).

Inspect without committing after staging:

```sh
./gradlew prepareCommitMsg -PdryRun=true --console=plain
```

### Operator — manual sync + `verifyVersions`

The operator is **not** hook-bumped. `apps/thingworx-operator/.version` is canonical; every other
operator version reference is verified against it by `build.gradle:239 verifyVersions`:

| File | Field | Sync rule |
|------|-------|-----------|
| `charts/thingworx-operator/.version` | whole file | Must be `==` canonical |
| `charts/thingworx-operator/Chart.yaml` | `version` + `appVersion` | Manually set `==` canonical |
| `images/thingworx-operator/build.gradle` | `OPERATOR_VERSION` | Derived at build time `file("${rootDir}/apps/thingworx-operator/.version").text` (no edit) |
| Gradle project version (`:apps:thingworx-operator`, `:images:thingworx-operator`) | `project.version` | Derived via root `build.gradle:13` from canonical file |

Bump procedure (see also `apps/thingworx-operator/README.md:226`):

```sh
echo "0.2.1" > apps/thingworx-operator/.version
echo "0.2.1" > charts/thingworx-operator/.version
# edit charts/thingworx-operator/Chart.yaml: version: 0.2.1, appVersion: "0.2.1"
./gradlew verifyVersions          # fails if any operator version source diverges
./gradlew :apps:thingworx-operator:check
helm lint charts/thingworx-operator
helm template thingworx-operator charts/thingworx-operator > /dev/null
```

Commit all changed files together with a Conventional Commit.

### Vendor-pinned images — `images/platform|security-tool|connection-server` (no `.version`)

These images are **not** SemVer-bumped via `.version` or the `thingworx.git-hooks` hook. Their
version is the **upstream vendor artifact + base image** pinned in code. To change the ThingWorx
version you edit the `ALL_TWX` maps in the `build.gradle` and supply the matching vendor archive
in `vendor/` (each `vendor/*.zip|*.tar.gz|*.gz` is individually encrypted via the `twx-file`
git filter — `.gitattributes` + `.gitfilters/setup-twx-file` — into LFS, decrypted at checkout
time with `GPG_PASSPHRASE`).

| Image | Pin source (`images/*/build.gradle:5`) | Vendor artifact(s) | Effective image tag |
|-------|----------------------------------------|--------------------|---------------------|
| `images/platform` | `ALL_TWX:5` `platformVersion` / `platformDockerFiles` (`MED-61268`) + `platformPostgres` (`MED-61111`) + `tomcatVersion` + `baseImage` | `MED-61268-CD-*_ThingWorx-Platform-DockerFiles-*.gz`, `MED-61111-CD-*_ThingWorx-Platform-Postgres-*.zip`, Tomcat `9.0.120`→`11.0.0`, JDK `21.0.2` (openjdk/corretto/graalvm), `ubuntu:22.04`→`24.04` | `ghcr.io/us-ignite/thingworx/platform-postgres:<platformVersion>[-<jdk>]` — e.g. `9.6.9-corretto` (alias `9.6.9`), `10.1.2-openjdk`; `base`/`platform-base`/`postgres-init` mirrored the same. See `buildAll_9_6_openjdk:176` / `pushAll:318` |
| `images/security-tool` | `ALL_TWX:5` `securityDockerFiles` (`MED-61282`) + `securityToolArchive` + `templateProcessorArchive` + `baseImage` | `MED-61282-CD-*_Security-Management-Tool-DockerFiles-*.gz`, `security-common-cli-*.tar.gz` (`1.7.2.309` / `1.6.1.217`), `template-processor-*.tar.gz` (`13.0.1.51` / `12.4.0.34`), JDK `corretto-11` vs `21.0.2.13.1` | `ghcr.io/us-ignite/thingworx/security-tool:<twx>.<patch>` — `versionTag` is `9.6.9`/`9.7.9`/`10.0.9` vs `10.1.2` (via `baseImage == ubuntu:24.04 ? ".2" : ".9"` in `buildSecurityTool_*:168`); `latest` alias |
| `images/connection-server` | `ALL_TWX:5` `cxServerDockerFiles` (`MED-61352`) + `cxVersion` + `ALL_CX_BASE:28` `javaArchive` | `MED-61352-CD-*_ThingWorx-Connection-Server-DockerFiles-*.tar.gz`, CX `9.2.2.2` (JDK 11) / `9.3.0.4` (JDK 21) | `ghcr.io/us-ignite/thingworx-cxserver-base:<cxVersion>` (`9.2.2.2`/`9.3.0.4`) and `ghcr.io/us-ignite/thingworx-cxserver:<twxVersion>` (`9.6`→`9.2.2.2`, `9.7`+→`9.3.0.4`) — `buildCxServerBase_*`/`buildCxServer_*:87` |

Builds are driven by explicit tasks, not `.version`:

```sh
./gradlew :images:platform:buildAll              # all TW + all JDK variants (openjdk/corretto/graalvm)
./gradlew :images:platform:buildAll_10_1_corretto
./gradlew :images:security-tool:buildAll
./gradlew :images:connection-server:buildAll
./gradlew :images:thingworx-operator:buildImage   # operator is the exception — versioned via .version
```

Bumping a vendor-pinned image: edit the `ALL_TWX` entry, add the new `MED-*`
archive to `vendor/` (it is auto-encrypted into LFS on `git add` while `GPG_PASSPHRASE`
is set), run the `decryptVendor`/`buildAll` chain locally, then commit
the `build.gradle` + `vendor/*` diff with a Conventional Commit — no `.version`
change expected (CI does **not** require a version bump for `images/platform/**`).

Contrast: `images/thingworx-operator` has **no** vendor pin (`decryptVendor` is a
placeholder `build.gradle:10`). Its tag **is** `OPERATOR_VERSION` from
`apps/thingworx-operator/.version:5` (`ghcr.io/us-ignite/thingworx/thingworx-operator:<version>`).

### Publishing (4 workflows, all `push:main` only; PRs only build/test via `ci.yml`)

| Workflow | Trigger on `main` | What it builds + tags on `main` |
|---|---|---|
| `publish-extensions.yml` | `apps/thingworx-jgit-extension/.version` diff | `:apps:thingworx-jgit-extension:buildAll` + matrix `test` 9.6/9.7/10.1 → `jgit-v<version>` tag + GH Release zips |
| `publish-jars.yml` (ex `publish-dap.yml:23`) | `libraries/thingworx-dap(-runtime)/.version` diff | `:libraries:thingworx-dap:check` + `publish` → `GH Packages` + `dap-v<version>` tag |
| `publish-charts.yml` | `apps/thingworx-operator/.version` or `charts/thingworx-operator/.version` or `Chart.yaml` diff | `verifyVersions` + `chartTest` → `helm package` → artifact `thingworx-operator-chart-<version>` + `chart-v<version>` tag + Release |
| `publish-images.yml` (+ `workflow_dispatch` `inputs.image`) | `apps/thingworx-operator/.version|images/thingworx-operator/**|Chart.yaml` → operator; `images/platform/**|vendor/MED-61268|61111` → platform; `MED-61282` → security-tool; `MED-61352` → connection-server | Operator: `verifyVersions` → `:images:thingworx-operator:buildImage` → `ghcr.io/.../thingworx-operator:<version>` + `operator-v*` tag; Vendor: `setup-twx-file` + re-checkout (needs `GPG_PASSPHRASE`) → `:images:platform:buildAll` etc. → `ghcr.io/...` vendor tags (no `.version`) |

PRs run `ci.yml` only (`pull_request:main`): `validate-versioning.sh`, `verifyVersions`+`chartTest`+`:apps:thingworx-operator:check`, `buildAll`+`verifyDapExample` — no `ghcr` push, no tags. See `.github/workflows/*.yml` `on:`.
