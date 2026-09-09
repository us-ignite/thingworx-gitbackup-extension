# Operator package

`api/` defines the public `ThingWorxCluster` API (`spec`, `status`, `conditions`). `reconcile/` owns convergence and status reporting via `ThingWorxClusterReconciler` and `ThingWorxExtensionReconciler`.

## Lifecycle

```
PRECHECK → DATABASE → COORDINATION → PLATFORM_PRIMARY → PLATFORM_CLUSTER → CONNECTION_SERVERS → EDGE → OPTIONAL_SERVICES → READY
                                      │                                              │
                                      └────── Job Failed → INVALID ─────────────────┘
                                                               │
                                          READY ─runtime lost─▶ DEGRADED ─recovered─▶ READY
```

* `LifecyclePhase` enum in `reconcile/LifecyclePhase.java`.
* `status.observedGeneration` only on `READY` (`ThingWorxClusterReconciler.status()`); other phases preserve previous value. `READY` with `metadata.generation != status.observedGeneration` restarts at `DATABASE`.
* `status.conditions`: `Ready` (True when READY), `Progressing` (rolling), `Degraded` (True when DEGRADED), `Error` (True when INVALID). `status.phase` + `status.message` are human-readable.

## Failure modes

* **INVALID** (`status.phase=INVALID`, `Error=True`): spec validation or init Job failed. Fix spec/Secret/Job (`kubectl logs job/...`), controller requeues at `DATABASE`. Events `Warning/FailedValidation` or `Warning/InstallerJobFailed`, metric `thingworx_operator_job_failures_total`.
* **DEGRADED** (`Degraded=True`): `READY` workload not ready (statefulSet `readyReplicas != replicas`). Auto-recovery when `runtimeReady()` true. Event `Warning/Degraded`.
* **Extension `Failed`/`Restarting`**: Per-extension, not cluster. `Failed` (validation, `MissingSecret`, `DependencyCycle`, `InstallerJobFailed`) requires new `spec.artifact.digest`. `Restarting` waits for platform rollout (`thingworx.us-ignite.org/extension-restart` annotation + revision convergence). See `ThingWorxExtensionReconciler`.

## Manual DB cleanup

`ThingWorxExtensionReconciler.cleanup()` deletes labeled Jobs only:

```
WARNING: ThingWorxExtension <ns>/<name> was deleted. Imported ThingWorx entities remain and require manual database cleanup.
```

Event `Warning/ExtensionDeleted`. Operator never deletes ThingWorx data; delete entities via Composer or SQL.

## Observability

* **Events**: `client.v1().events()` — `Normal/PhaseTransition` on advances, `Warning` on INVALID/failed Jobs. See `emitClusterEvent()` / `emitExtensionEvent()` in reconcilers.
* **Metrics**: `metrics/OperatorMetrics.java` — lightweight `thingworx_operator_reconcile_total`, `thingworx_operator_reconcile_duration_seconds`, `thingworx_operator_phase`, `thingworx_operator_job_failures_total`; exposed via `OperatorApplication` `/metrics` (no Micrometer).
* **Structured logging**: SLF4J `cluster=<ns>/<name> phase=<PHASE> event=reconcileStart|statusUpdate|reconcileComplete durationMs=` and `extension=<ns>/<name> phase=` per phase.
* **OTel (optional)**: `spec.otelEnabled` controls optional `otel` Deployment; tracing env can be injected via `values.yaml extraEnv`.

See root `apps/thingworx-operator/README.md` for kubectl examples and `charts/thingworx-operator/README.md` for Helm notes.
