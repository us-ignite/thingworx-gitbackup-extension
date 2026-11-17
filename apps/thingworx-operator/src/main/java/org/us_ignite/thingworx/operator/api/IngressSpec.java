package org.us_ignite.thingworx.operator.api;

/** Ingress exposure. Configure TLS or explicitly acknowledge a trusted upstream TLS terminator. */
public class IngressSpec {
    private String host;
    private String className;
    private String tlsSecretName;
    private String certManagerIssuer;
    private boolean trustedUpstreamTls;

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

    public boolean isTrustedUpstreamTls() {
        return trustedUpstreamTls;
    }

    public void setTrustedUpstreamTls(boolean value) {
        trustedUpstreamTls = value;
    }
}
