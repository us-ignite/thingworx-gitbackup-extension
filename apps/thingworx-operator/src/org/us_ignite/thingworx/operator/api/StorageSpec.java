package org.us_ignite.thingworx.operator.api;

/** PVC requirements. The shared claim must be backed by an RWX-capable StorageClass. */
public class StorageSpec {
    private String storageClassName;
    private String sharedStorageSize = "20Gi";
    private String componentStorageSize = "10Gi";

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
}
