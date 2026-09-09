# ThingWorx operator deployments

This subproject contains deployable ThingWorx operator examples. Credentials, licences, database
instances, registries, storage classes, and images must
be provisioned separately.

## Gradle deployment tasks

```sh
./gradlew :examples:thingworx-operator-deployments:deployOperatorChart
./gradlew :examples:thingworx-operator-deployments:deployProductionCluster
./gradlew :examples:thingworx-operator-deployments:deployDevelopmentCluster
./gradlew :examples:thingworx-operator-deployments:verifyDeploymentManifests
```

Cluster tasks depend on `deployOperatorChart`, validate required resources, apply the selected
configuration, and wait for its `ThingWorxCluster` to reach `READY`. They do not create Secrets,
ConfigMaps, databases, licences, or registry credentials.

Useful overrides include:

```sh
./gradlew :examples:thingworx-operator-deployments:deployDevelopmentCluster \
  -PkubeContext=minikube \
  -PthingworxNamespace=thingworx \
  -PoperatorNamespace=thingworx-operator \
  -PoperatorImageTag=dev \
  -PplatformImage=registry.example/platform:tag
```

The moved `thingworxextension.yaml` is an apply-only example.

## Applying examples manually

These files are not Helm templates and are not installed by the chart. Apply one only after
creating the referenced Secrets, ConfigMap, RWX-capable StorageClass, external HA PostgreSQL
database, and published image references.

Create the licence as a namespace-local Kubernetes Secret from your securely stored licence file;
do not copy it into an image build context. The cluster examples reference that Secret as
`thingworx-license`.

```sh
kubectl apply -f thingworxcluster.yaml
kubectl get thingworxclusters
kubectl describe twx production
```
