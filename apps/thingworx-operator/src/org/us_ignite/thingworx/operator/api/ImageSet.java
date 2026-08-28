package org.us_ignite.thingworx.operator.api;

/** Immutable image references. Production values should be pinned by digest. */
public class ImageSet {
    private String platform = "ghcr.io/us-ignite/thingworx/platform-postgres:10.1.2";
    private String databaseInit = "ghcr.io/us-ignite/thingworx/postgres-init:10.1.2";
    private String securityCli = "ghcr.io/us-ignite/thingworx/security-tool:10.1.2";

    /** Apache Ignite's public upstream image. */
    private String ignite = "apacheignite/ignite:2.16.0";

    private String cxServer = "ghcr.io/us-ignite/thingworx/cxserver-twx:10.1";
    private String haProxy = "haproxy:3.0.10";
    private String zookeeper = "zookeeper:3.8.4";
    private String kafka;
    private String otelCollector;
    private String extensionInstaller = "ghcr.io/us-ignite/thingworx/extension-installer:latest";

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String value) {
        platform = value;
    }

    public String getDatabaseInit() {
        return databaseInit;
    }

    public void setDatabaseInit(String value) {
        databaseInit = value;
    }

    public String getSecurityCli() {
        return securityCli;
    }

    public void setSecurityCli(String value) {
        securityCli = value;
    }

    public String getIgnite() {
        return ignite;
    }

    public void setIgnite(String value) {
        ignite = value;
    }

    public String getCxServer() {
        return cxServer;
    }

    public void setCxServer(String value) {
        cxServer = value;
    }

    public String getHaProxy() {
        return haProxy;
    }

    public void setHaProxy(String value) {
        haProxy = value;
    }

    public String getZookeeper() {
        return zookeeper;
    }

    public void setZookeeper(String value) {
        zookeeper = value;
    }

    public String getKafka() {
        return kafka;
    }

    public void setKafka(String value) {
        kafka = value;
    }

    public String getOtelCollector() {
        return otelCollector;
    }

    public void setOtelCollector(String value) {
        otelCollector = value;
    }

    public String getExtensionInstaller() {
        return extensionInstaller;
    }

    public void setExtensionInstaller(String value) {
        extensionInstaller = value;
    }
}
