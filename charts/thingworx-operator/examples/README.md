# ThingWorxCluster API examples

These examples are deliberately **not** Helm templates and are not installed by the chart. Apply one only after creating the referenced Secrets, ConfigMap, RWX-capable StorageClass, external HA PostgreSQL database, and published image references.

Create the licence as a namespace-local Kubernetes Secret from your securely stored licence file; do not copy it into an image build context. The sample's `spec.licenseSecret: thingworx-license` causes the operator to mount that Secret only into ThingWorx platform pods at runtime.

```sh
kubectl apply -f thingworxcluster.yaml
kubectl get thingworxclusters
kubectl describe twx production
```
