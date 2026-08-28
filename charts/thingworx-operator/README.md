# thingworx-operator chart

Install the operator after publishing its image:

```sh
helm upgrade --install thingworx-operator charts/thingworx-operator --set image.repository=ghcr.io/us-ignite/thingworx/thingworx-operator --set image.tag=0.1.0
```

The chart installs the `thingworxclusters.thingworx.us-ignite.org` CRD from `crds/` before
the operator resources, plus the cluster-scoped RBAC and namespace-scoped operator
Deployment. Helm does not upgrade or delete CRDs automatically; CRD upgrades are a
deliberate release operation.

The CRD exposes the namespaced `ThingWorxCluster` API (`twx` short name). The chart
does not create a cluster by default: use [examples/thingworxcluster.yaml](examples/thingworxcluster.yaml)
after preparing the referenced external dependencies and Secrets.

Extensions are separate `ThingWorxExtension` resources. The cluster-level extension list is not supported.
