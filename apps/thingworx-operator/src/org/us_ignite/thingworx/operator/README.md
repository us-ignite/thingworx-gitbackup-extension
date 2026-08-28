# Operator package

`api/` defines the public `ThingWorxCluster` API. `reconcile/` owns convergence and status reporting. The package currently creates no workloads: that starts only after the documented HA contract is captured in the custom-resource specification.
