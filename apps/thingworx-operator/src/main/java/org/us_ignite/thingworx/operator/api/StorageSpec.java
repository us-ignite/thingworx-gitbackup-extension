package org.us_ignite.thingworx.operator.api;

/**
 * PVC requirements. The shared claim must be backed by an RWX-capable StorageClass.
 *
 * <p>Storage expansion is supported when the underlying {@code StorageClass} has
 * {@code allowVolumeExpansion: true}. The operator will patch an existing
 * {@code PersistentVolumeClaim} {@code spec.resources.requests.storage} when the
 * desired size is larger than the current size. Shrinking is disallowed: Kubernetes
 * does not support decreasing {@code requests.storage} on a bound PVC and the
 * operator will reject shrink requests (existing size is preserved and a warning
 * Event/Condition is emitted). Request the larger size via {@code sharedStorageSize}
 * or {@code componentStorageSize} and ensure the StorageClass permits expansion.
 */
public class StorageSpec {
    private String storageClassName;
    private String sharedStorageSize = "20Gi";
    private String componentStorageSize = "10Gi";
    /**
     * Optional annotations applied to every PVC the operator creates
     * ({@code shared} and, when internal, {@code postgres-data}).
     *
     * <p>Use for backup tooling, e.g. Velero {@code velero.io/exclude-from-backup},
     * {@code backup.velero.io/backup-volumes}, or snapshot-controller hints. The map is
     * merged into {@code metadata.annotations} on create; existing PVC annotations not
     * managed by the operator are preserved by server-side apply. Null or empty means no
     * extra annotations.
     */
    private java.util.Map<String, String> pvcAnnotations;

    public String getStorageClassName() {
        return storageClassName;
    }

    public void setStorageClassName(String value) {
        storageClassName = value;
    }

    public String getSharedStorageSize() {
        return sharedStorageSize;
    }

    public void setSharedStorageSize(String value) {
        sharedStorageSize = value;
    }

    public String getComponentStorageSize() {
        return componentStorageSize;
    }

    public void setComponentStorageSize(String value) {
        componentStorageSize = value;
    }

    public java.util.Map<String, String> getPvcAnnotations() {
        return pvcAnnotations;
    }

    public void setPvcAnnotations(java.util.Map<String, String> value) {
        pvcAnnotations = value;
    }
}
