package org.us_ignite.thingworx.operator.api;

/** Desired state for a vendor-parity ThingWorx clustered installation. */
public class ThingWorxClusterSpec {
    private boolean enableHA;
    private ImageSet images = new ImageSet();
    private DatabaseSpec database = new DatabaseSpec();
    private StorageSpec storage = new StorageSpec();
    private IngressSpec ingress = new IngressSpec();
    private String settingsConfigMap;
    private String licenseSecret;
    private String imagePullSecret;
    private SecretKeyReference provisioningAppKey;
    private SecretKeyReference platformAdminPassword;
    private SecretKeyReference cxServerAppKey;
    private SecretKeyReference extensionImportAppKey;
    private SecretKeyReference keystorePassword;
    private ExtensionContentPolicy extensionImportPolicy = new ExtensionContentPolicy();
    private boolean kafkaEnabled;
    private boolean otelEnabled;

    public boolean isEnableHA() {
        return enableHA;
    }

    public void setEnableHA(boolean value) {
        enableHA = value;
    }

    public ImageSet getImages() {
        return images;
    }

    public void setImages(ImageSet value) {
        images = value;
    }

    public DatabaseSpec getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseSpec value) {
        database = value;
    }

    public StorageSpec getStorage() {
        return storage;
    }

    public void setStorage(StorageSpec value) {
        storage = value;
    }

    public IngressSpec getIngress() {
        return ingress;
    }

    public void setIngress(IngressSpec value) {
        ingress = value;
    }

    public String getSettingsConfigMap() {
        return settingsConfigMap;
    }

    public void setSettingsConfigMap(String value) {
        settingsConfigMap = value;
    }

    public String getLicenseSecret() {
        return licenseSecret;
    }

    public void setLicenseSecret(String value) {
        licenseSecret = value;
    }

    public String getImagePullSecret() {
        return imagePullSecret;
    }

    public void setImagePullSecret(String value) {
        imagePullSecret = value;
    }

    public SecretKeyReference getProvisioningAppKey() {
        return provisioningAppKey;
    }

    public void setProvisioningAppKey(SecretKeyReference value) {
        provisioningAppKey = value;
    }

    public SecretKeyReference getPlatformAdminPassword() {
        return platformAdminPassword;
    }

    public void setPlatformAdminPassword(SecretKeyReference value) {
        platformAdminPassword = value;
    }

    public SecretKeyReference getCxServerAppKey() {
        return cxServerAppKey;
    }

    public void setCxServerAppKey(SecretKeyReference value) {
        cxServerAppKey = value;
    }

    public SecretKeyReference getExtensionImportAppKey() {
        return extensionImportAppKey;
    }

    public void setExtensionImportAppKey(SecretKeyReference value) {
        extensionImportAppKey = value;
    }

    public SecretKeyReference getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(SecretKeyReference value) {
        keystorePassword = value;
    }

    public ExtensionContentPolicy getExtensionImportPolicy() {
        return extensionImportPolicy;
    }

    public void setExtensionImportPolicy(ExtensionContentPolicy value) {
        extensionImportPolicy = value;
    }

    public boolean isKafkaEnabled() {
        return kafkaEnabled;
    }

    public void setKafkaEnabled(boolean value) {
        kafkaEnabled = value;
    }

    public boolean isOtelEnabled() {
        return otelEnabled;
    }

    public void setOtelEnabled(boolean value) {
        otelEnabled = value;
    }
}
