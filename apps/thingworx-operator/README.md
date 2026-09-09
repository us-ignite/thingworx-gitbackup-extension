# ThingWorx Kubernetes Operator

An unofficial Java operator for managing a PTC ThingWorx deployment on Kubernetes.
PTC-delivered binaries, licence credentials, customer artefacts, and vendor reference files are not part of this module.

## Build and test

The operator is built natively with Gradle. Production and test code share `src`, with the `test`
package excluded from production compilation and included only in the Gradle test source set.

```sh
# Gradle build and test paths
./gradlew :apps:thingworx-operator:test          # unit tests
./gradlew :apps:thingworx-operator:integrationTest # tests tagged integration
./gradlew :apps:thingworx-operator:build         # standard and executable JARs
./gradlew :apps:thingworx-operator:testAll       # alias for test
./gradlew :apps:thingworx-operator:buildAll      # alias for build
./gradlew :apps:thingworx-operator:check         # unit tests
./gradlew testAll                                # runs all subprojects including operator
./gradlew buildAll                               # builds all subprojects including operator

# Docker image (operator image definition is in images/thingworx-operator)
./gradlew :images:thingworx-operator:buildImage
# Legacy aliases kept for compatibility (prefer build / buildImage above):
./gradlew :apps:thingworx-operator:installDist   # alias for build
./gradlew :apps:thingworx-operator:jibDockerBuild # alias for :images:thingworx-operator:buildImage
./gradlew :apps:thingworx-operator:devSetup      # alias for :images:thingworx-operator:buildImage
```

`devSetup` is currently a compatibility alias that builds `thingworx-operator:dev`. Install
or upgrade the chart separately with the command in the chart README, then apply a sample
`ThingWorxCluster` after creating its referenced Secrets and providing a valid license.

The operator image definition is in [`../../images/thingworx-operator`](../../images/thingworx-operator/README.md),
and the Helm chart is in [`../../charts/thingworx-operator`](../../charts/thingworx-operator/README.md).

`ThingWorxCluster` resources model the cluster lifecycle. `ThingWorxExtension` resources install digest-pinned
OCI extension artifacts one at a time for each target cluster.

## Lifecycle phases and state diagram

The `ThingWorxCluster` reconciler is an ordered, idempotent state machine defined in
`LifecyclePhase` (`PRECHECK`, `DATABASE`, `COORDINATION`, `PLATFORM_PRIMARY`, `PLATFORM_CLUSTER`,
`CONNECTION_SERVERS`, `EDGE`, `OPTIONAL_SERVICES`, `READY`, `DEGRADED`, `INVALID`):

```
                    ┌──────────┐
              ┌────▶│ PRECHECK │◀─────────────┐
              │     └────┬─────┘              │
              │          │  prerequisites     │  spec becomes valid again
              │          ▼                    │  (INVALID -> DATABASE)
              │     ┌──────────┐              │
              │     │ DATABASE │─── Job Failed ──▶ INVALID ──┐
              │     └────┬─────┘              │               │
              │          │  Jobs Complete     │               │ fix spec
              │          ▼                    │               │ (requeues at DATABASE)
              │  ┌────────────────┐           │               │
              │  │ COORDINATION   │ (HA only) │               │
              │  └───────┬────────┘           │               │
              │          ▼                    │               │
              │  ┌──────────────────┐         │               │
              │  │ PLATFORM_PRIMARY │         │               │
              │  └───────┬──────────┘         │               │
              │          ▼                    │               │
              │  ┌──────────────────┐         │               │
              │  │ PLATFORM_CLUSTER │ (HA only)               │
              │  └───────┬──────────┘         │               │
              │          ▼                    │               │
              │  ┌────────────────────┐       │               │
              │  │ CONNECTION_SERVERS │ (HA)  │               │
              │  └─────────┬──────────┘       │               │
              │            ▼                  │               │
              │     ┌──────────────┐          │               │
              │     │ EDGE         │          │               │
              │     └──────┬───────┘          │               │
              │            ▼                  │               │
              │     ┌──────────────────┐      │               │
              │     │ OPTIONAL_SERVICES│      │               │
              │     └───────┬──────────┘      │               │
              │             ▼                 │               │
              │        ┌─────────┐            │               │
              └────────│  READY  │◀───────────┘               │
                       └───┬─────┘                            │
                           │ runtime not ready                 │
                           ▼                                  │
                       ┌──────────┐  runtime recovers ─────────┘
                       │ DEGRADED │─────────────────▶ READY
                       └──────────┘
```

* `PRECHECK → DATABASE → … → READY` is the happy path. Each phase applies its `ThingWorxResources.*` workload and waits for readiness (StatefulSet `readyReplicas == replicas` or Deployment `availableReplicas == replicas`, Jobs `Complete`) before advancing.
* **Generation handling:** `status.observedGeneration` is only updated when `phase == READY`. During rollout (`DATABASE … OPTIONAL_SERVICES`, `DEGRADED`, `INVALID`) the old `observedGeneration` is preserved so clients can distinguish *in-flight update* vs *converged*. A `READY` cluster with `metadata.generation != status.observedGeneration` restarts the ordered workflow at `DATABASE` (re-applies precheck with new images).
* **Pruning:** When `READY` and runtime is healthy, the operator garbage-collects orphaned managed resources (label `app.kubernetes.io/managed-by=thingworx-operator` + `app.kubernetes.io/instance=<cluster>`). PVCs are only pruned when annotated `thingworx.us-ignite.org/prune-pvc=true` to avoid data loss.

## Failure modes: INVALID vs DEGRADED vs extension Failed/Restarting

### ThingWorxCluster `INVALID` — spec error, fix spec to recover

* **Meaning:** Admission-time validation failed (missing `spec.images.platform`, unpinned `:latest` installer, missing Secret/ConfigMap, HA without external DB, etc.) or a required initialization Job (`database-init`, `security-init`) failed.
* **Status:** `phase: INVALID`, `conditions: Error=True (reason=FailedValidation or InstallerFailed)`, `Ready=False`, `Progressing=False`. `observedGeneration` is **not** advanced.
* **Recovery:** Fix the spec (correct images, create missing Secret `kubectl create secret ...`, fix Job logs `kubectl logs job/<cluster>-database-init-xxxx`). The reconciler requeues at `DATABASE` on next loop: `INVALID -> DATABASE` when validation passes. No manual `status` edit needed.
* **Events:** `Warning` event `FailedValidation` or `InstallerJobFailed` on the `ThingWorxCluster` object; job failure counter `thingworx_operator_job_failures_total` increments.

```sh
kubectl get thingworxclusters -n thingworx -o yaml
kubectl describe thingworxcluster demo -n thingworx   # Conditions + Events
kubectl get events -n thingworx --field-selector involvedObject.kind=ThingWorxCluster
kubectl logs job/demo-database-init-abc123 -n thingworx
kubectl get job demo-database-init-abc123 -n thingworx -o yaml | grep -A5 conditions
```

### ThingWorxCluster `DEGRADED` — runtime not ready

* **Meaning:** A previously `READY` cluster's managed workload became not ready (`platform` StatefulSet, `postgres` if internal, plus `zookeeper`/`ignite`/`cxserver`/`haproxy` in HA). The spec itself is valid.
* **Status:** `phase: DEGRADED`, `conditions: Degraded=True`, `Ready=False`. `observedGeneration` remains the last `READY` generation.
* **Recovery:** Automatic — the controller polls every 15s (`recover()`) and returns to `READY` when `runtimeReady()` is true. Investigate workload: `kubectl get sts,deploy -n thingworx -l app.kubernetes.io/instance=demo`, `kubectl describe pod ...`.
* **Events:** `Warning` `Degraded` on status transition; `Normal` `PhaseTransition` when recovered.

### ThingWorxExtension phases vs cluster `INVALID`

Extensions are serialized per cluster (alphabetical `metadata.name` order, after dependency readiness). Each extension reports its own `phase`:

| Phase | Meaning | Next step |
|-------|---------|-----------|
| `WaitingForCluster` | Target `ThingWorxCluster` missing or not `READY` | Wait for cluster `READY` |
| `WaitingForDependencies` | `spec.dependsOn` not `Ready` | Wait for prerequisite extensions |
| `WaitingForTurn` | Another extension is `Importing/Updating` for same cluster | Serialized; no action |
| `Importing` / `Updating` | Installer Job created/running (`InstallerJobCreated`, `InstallerRunning`) | Wait for Job `Complete` |
| `Restarting` | Job succeeded and `importPolicy.requiresRestart()` (jar/web etc.) — platform rollout annotated `thingworx.us-ignite.org/extension-restart=<fingerprint>` | Wait for `platform` StatefulSet `replicas==ready==updated==available` and `currentRevision==updateRevision` |
| `Ready` | `observedFingerprint` matches desired fingerprint | No action |
| `Failed` | Validation, missing Secret, dependency cycle, or `InstallerJobFailed` (`ImagePullBackOff`, etc.) | Fix spec/Secret/image and bump `spec.artifact.digest` to create a new fingerprint/Job |

Extension `Failed` does **not** put the cluster into `INVALID`; it is per-extension. Inspect `kubectl describe thingworxextension <name> -n thingworx`, `kubectl get events --field-selector involvedObject.kind=ThingWorxExtension`, and `kubectl logs job/<ext>-<fingerprint12>`.

### Extension cleanup warning — manual ThingWorx entity deletion

`ThingWorxExtension` uses finalizer `thingworx.us-ignite.org/extension-finalizer`. On deletion `cleanup()` deletes labeled installer Jobs and logs/emits:

```
WARNING: ThingWorxExtension <ns>/<name> was deleted. Imported ThingWorx entities remain and require manual database cleanup.
Event Warning ExtensionDeleted
```

**The operator never deletes data inside ThingWorx.** After `kubectl delete thingworxextension <name>` you must manually remove the entities inside ThingWorx (Composer → delete Thing, ThingTemplate, Extension, etc.) and, if needed, clean related rows in the PostgreSQL `thingworx` schema. Re-creating an extension with the same name/digest will re-import.

## Status fields, Conditions and kubectl examples

```yaml
apiVersion: thingworx.us-ignite.org/v1alpha1
kind: ThingWorxCluster
metadata:
  name: demo
  namespace: thingworx
  generation: 2
spec:
  images: { platform: registry.example/platform@sha256:..., extensionInstaller: registry.example/installer@sha256:... }
  database: { host: postgres.example.test, database: thingworx, schema: twx, username: twx, adminUsername: postgres }
  storage: { storageClassName: rwx }
status:
  phase: READY            # one of LifecyclePhase
  message: All desired vendor lifecycle components are reconciled.
  observedGeneration: 2   # only updated on READY; see generation handling above
  currentVersion: registry.example/platform@sha256:...
  mode: SINGLE_NODE | HA
  conditions:
  - type: Ready       # True only when phase==READY
    status: "True"
    reason: READY
    message: All desired vendor lifecycle components are reconciled.
    observedGeneration: 2
    lastTransitionTime: "2026-01-15T12:00:00Z"
  - type: Progressing # True when rolling (DATABASE…OPTIONAL_SERVICES)
    status: "False"
  - type: Degraded    # True when phase==DEGRADED
    status: "False"
  - type: Error       # True when phase==INVALID
    status: "False"
```

Extension `status` adds `observedDigest`, `observedFingerprint`, `lastAttemptedDigest`, `installerJob`, `importedName/version`, `requestedRestart`:

```sh
kubectl get thingworxclusters -n thingworx -o yaml
kubectl get thingworxclusters demo -n thingworx -o jsonpath='{.status.phase}{"\t"}{.status.observedGeneration}{"\n"}'
kubectl get thingworxextensions -n thingworx
kubectl get thingworxextension entities -n thingworx -o yaml
kubectl get thingworxextension entities -n thingworx -o jsonpath='{.status.conditions}' | jq
kubectl get events -n thingworx --sort-by=.lastTimestamp
kubectl logs deploy/thingworx-operator -n thingworx-operator | grep "cluster=thingworx/demo phase="
```

## Observability: Events, metrics, structured logging

### Kubernetes Events (`client.v1().events()`)

* **Cluster:** `Normal PhaseTransition` on every phase advance, `Warning FailedValidation` on spec errors, `Warning InstallerJobFailed` when `database-init`/`security-init` fails, `Warning Degraded` on health loss, `Normal/VolumeExpansion` and `Warning VolumeExpansionBlocked/VolumeShrinkDisallowed` for PVC storage, plus `Normal/Pruning` logs (Event for PVC expansion only; pruning is log-only).
* **Extension:** `Normal InstallerJobCreated/InstallerRunning/Installed`, `Warning InstallerJobFailed`, `Warning ExtensionDeleted` (cleanup), dependency failures (`DependencyCycle`, `DependencyClusterMismatch`).
* Query: `kubectl get events -n <ns> --field-selector reason=FailedValidation` or `kubectl describe thingworxcluster <name>`.

### Prometheus metrics (lightweight, no Micrometer)

Exposed at `GET http://<operator>:8080/metrics` (same port as `/health`). Text format `thingworx_operator_*`:

* `thingworx_operator_reconcile_total{controller="ThingWorxCluster|ThingWorxExtension",cluster="ns/name"}` — counter
* `thingworx_operator_reconcile_duration_seconds{controller,cluster}` — sum of durations (divide `…_duration_seconds / …_total` for avg)
* `thingworx_operator_phase{cluster="ns/name",phase="READY|DEGRADED|INVALID|..."} =1` — gauge for current phase
* `thingworx_operator_job_failures_total{cluster, job="..."}` — failed Job count

No extra dependency: `OperatorMetrics` is in-process `ConcurrentHashMap`/`LongAdder`. For production Prometheus, add a `ServiceMonitor` scraping `thingworx-operator:8080/metrics`. If Micrometer is desired, add `io.micrometer:micrometer-registry-prometheus` and bridge these counters in `OperatorApplication`; the file is intentionally thin to keep the 0.2.0 image small.

### Structured SLF4J logging

Every reconcile logs with `cluster=<ns>/<name> phase=<PHASE>` (or `extension=<ns>/<name>`) :

```
INFO  cluster=thingworx/demo phase=PRECHECK event=reconcileStart generation=2 observedGeneration=1
INFO  cluster=thingworx/demo phase=DATABASE prevPhase=PRECHECK message="Prerequisites accepted..." event=statusUpdate
WARN  cluster=thingworx/demo phase=INVALID prevPhase=DATABASE reason=FailedValidation message="Secret 'license' not found..." event=statusUpdate
INFO  extension=thingworx/entities phase=Importing reason=InstallerJobCreated message="Installer Job created for OCI artifact." event=statusUpdate
WARN  extension=thingworx/entities phase=Failed reason=InstallerJobFailed message="Installer Job failed; ImagePullBackOff..." event=statusUpdate
INFO  cluster=thingworx/demo phase=READY event=reconcileComplete durationMs=42 generation=2
```

Configure with `src/main/resources/logback.xml` (replace `slf4j-simple` with `logback-classic` if you need JSON). For OpenTelemetry, set `spec.otelEnabled: true` (adds `otel` Deployment) and enable OTel tracing via `extraEnv: [{name: OTEL_TRACES_EXPORTER, value: otlp}]`; the operator logs `spec.otelEnabled` at `READY` and future versions may add W3C trace headers to Jobs.

## Versioning (single source of truth)

The canonical operator version is [`apps/thingworx-operator/.version`](./.version) (`0.2.0` today) — mirrored to
[`charts/thingworx-operator/.version`](../../charts/thingworx-operator/.version). All other operator version
references **derive from or are verified against** this file:

| File | Field | How it stays in sync |
|------|-------|----------------------|
| `charts/thingworx-operator/Chart.yaml` | `version` / `appVersion` | Manual edit + verified by `verifyVersions` |
| `charts/thingworx-operator/.version` | whole file | Must be identical to `apps/thingworx-operator/.version` |
| `images/thingworx-operator/build.gradle` | `OPERATOR_VERSION` | **Derived at build time**: `file("${rootDir}/apps/thingworx-operator/.version").text.trim()` |
| Gradle project version (`:apps:thingworx-operator`, `:images:thingworx-operator`) | `project.version` | Derived from `.version` via root `build.gradle` `allprojects` block |

Root `build.gradle` sets a fallback `0.1.0-SNAPSHOT` for projects that have no `.version` file, but operator
subprojects pick up the canonical version automatically.

### Bumping the operator version

1. Update the canonical file:
   ```sh
   echo "0.2.1" > apps/thingworx-operator/.version
   echo "0.2.1" > charts/thingworx-operator/.version
   ```
2. Synchronize the chart metadata (keep them identical to the canonical version):
   ```sh
   # Chart.yaml: edit version: 0.2.0 / appVersion: "0.2.0" -> 0.2.1
   ```
   `images/thingworx-operator/build.gradle` needs no edit — it reads `.version` at configuration time.
3. Verify:
   ```sh
   ./gradlew verifyVersions
   ./gradlew :apps:thingworx-operator:check
   helm lint charts/thingworx-operator
   helm template thingworx-operator charts/thingworx-operator > /dev/null
   ```
   `./gradlew verifyVersions` fails the build if any source diverges and prints a diff-style message.
4. Commit all changed files together (Conventional Commits still apply for versioning policy).

The Gradle project version is derived from `.version` by the root build configuration.
