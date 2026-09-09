# thingworx-operator chart

Install the operator after publishing its image:

```sh
helm upgrade --install thingworx-operator charts/thingworx-operator --set image.repository=ghcr.io/us-ignite/thingworx/thingworx-operator --set image.tag=0.2.0
```

The chart installs the `thingworxclusters.thingworx.us-ignite.org` CRD from `crds/` before
the operator resources, plus the cluster-scoped RBAC and namespace-scoped operator
Deployment. Helm does not upgrade or delete CRDs automatically; CRD upgrades are a
deliberate release operation.

The CRD exposes the namespaced `ThingWorxCluster` API (`twx` short name). The chart
does not create a cluster by default: use [the deployment examples](../examples/thingworx-operator-deployments/thingworxcluster.yaml)
after preparing the referenced external dependencies and Secrets.

Extensions are separate `ThingWorxExtension` resources. The cluster-level extension list is not supported.

## Lifecycle and failure modes

`ThingWorxCluster` reconciles in order `PRECHECK → DATABASE → COORDINATION(HA) → PLATFORM_PRIMARY → PLATFORM_CLUSTER(HA) → CONNECTION_SERVERS(HA) → EDGE → OPTIONAL_SERVICES → READY`. `DEGRADED` means a `READY` cluster's workload became not ready (automatic recovery); `INVALID` means spec error or failed init Job (fix spec → requeues at `DATABASE`).

```
PRECHECK → DATABASE → COORDINATION → PLATFORM_PRIMARY → PLATFORM_CLUSTER → CONNECTION_SERVERS → EDGE → OPTIONAL_SERVICES → READY
                                      │                         │
                                      └──── Job Failed → INVALID ──┘
                                                               │
                                          READY ─runtime lost─▶ DEGRADED ─recovered─▶ READY
```

* `status.observedGeneration` is only set on `READY`; in-flight or `INVALID` keeps the old value so `kubectl get twx demo -o jsonpath='{.status.observedGeneration}'` distinguishes updating vs converged.
* `status.phase`, `status.message`, `status.conditions` (`Ready`, `Progressing`, `Degraded`, `Error`) and `kubectl get events --field-selector involvedObject.kind=ThingWorxCluster` are the primary debug signals.

```sh
kubectl get thingworxclusters -n thingworx -o yaml
kubectl describe thingworxcluster demo -n thingworx
kubectl get events -n thingworx --field-selector involvedObject.kind=ThingWorxCluster
kubectl get thingworxextensions -n thingworx -o yaml
kubectl describe thingworxextension my-ext -n thingworx   # Failed/Restarting/WaitingForDependencies
kubectl logs job/my-ext-abc123 -n thingworx
```

### Extension cleanup — manual ThingWorx entity deletion

Deleting a `ThingWorxExtension` deletes its installer Jobs (finalizer `thingworx.us-ignite.org/extension-finalizer`) and emits `Warning ExtensionDeleted`:

```
WARNING: ThingWorxExtension <ns>/<name> was deleted. Imported ThingWorx entities remain and require manual database cleanup.
```

The operator never deletes data inside ThingWorx. Remove entities via Composer or directly in PostgreSQL after `kubectl delete thingworxextension <name>`.

## Observability

* **Events:** `Normal PhaseTransition` on advances, `Warning FailedValidation/InstallerJobFailed/Degraded` on clusters, `Warning DependencyCycle/ExtensionDeleted` on extensions. View with `kubectl get events -A --sort-by=.lastTimestamp`.
* **Metrics:** `GET http://<operator>:8080/metrics` exposes Prometheus text:
  * `thingworx_operator_reconcile_total{controller,cluster}`
  * `thingworx_operator_reconcile_duration_seconds{controller,cluster}`
  * `thingworx_operator_phase{cluster,phase}=1`
  * `thingworx_operator_job_failures_total{cluster,job}`
  No Micrometer dependency; add a `ServiceMonitor` pointing at port `http` path `/metrics`.
* **Structured logs:** Every reconcile logs `cluster=<ns>/<name> phase=<PHASE> event=reconcileStart|statusUpdate|reconcileComplete durationMs=…` or `extension=<ns>/<name> phase=…`. Search operator logs: `kubectl logs deploy/thingworx-operator -n thingworx-operator | grep cluster=thingworx/demo`.
* **OTel (optional):** Set `spec.otelEnabled: true` in the cluster to deploy the `otel` Collector; add `extraEnv` with `OTEL_TRACES_EXPORTER=otlp` to forward traces. The operator logs `spec.otelEnabled` at `READY`.
