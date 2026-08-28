package org.us_ignite.thingworx.operator.api;

/** Ingress exposure. Set either tlsSecretName or certManagerIssuer. */
public class IngressSpec {
    private String host;
    private String className;
    private String tlsSecretName;
    private String certManagerIssuer;

    public String getHost() {
        return host;
    }

    public void setHost(String value) {
        host = value;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String value) {
        className = value;
    }

    public String getTlsSecretName() {
        return tlsSecretName;
    }

    public void setTlsSecretName(String value) {
        tlsSecretName = value;
    }

    public String getCertManagerIssuer() {
        return certManagerIssuer;
    }

    public void setCertManagerIssuer(String value) {
        certManagerIssuer = value;
    }
}
