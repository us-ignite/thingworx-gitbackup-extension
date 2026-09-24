package org.us_ignite.thingworx.operator.api;

import java.util.ArrayList;
import java.util.List;

/** Desired state for one OCI-hosted ThingWorx extension package. */
public class ThingWorxExtensionSpec {
    private ClusterReference clusterRef = new ClusterReference();
    private OciArtifactReference artifact = new OciArtifactReference();
    private ExtensionContentPolicy importPolicy = new ExtensionContentPolicy();
    private List<String> dependsOn = new ArrayList<>();
    private String artifactPullSecret;

    public ClusterReference getClusterRef() {
        return clusterRef;
    }

    public void setClusterRef(ClusterReference value) {
        clusterRef = value;
    }

    public OciArtifactReference getArtifact() {
        return artifact;
    }

    public void setArtifact(OciArtifactReference value) {
        artifact = value;
    }

    public ExtensionContentPolicy getImportPolicy() {
        return importPolicy;
    }

    public void setImportPolicy(ExtensionContentPolicy value) {
        importPolicy = value;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<String> value) {
        dependsOn = value;
    }

    public String getArtifactPullSecret() {
        return artifactPullSecret;
    }

    public void setArtifactPullSecret(String value) {
        artifactPullSecret = value;
    }
}
