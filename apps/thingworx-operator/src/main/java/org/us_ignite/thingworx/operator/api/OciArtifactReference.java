package org.us_ignite.thingworx.operator.api;

/** Immutable OCI artifact location. Digest must use the sha256 form. */
public class OciArtifactReference {
    private String repository;
    private String digest;
    private String expectedName;
    private String expectedVersion;

    public String getRepository() {
        return repository;
    }

    public void setRepository(String value) {
        repository = value;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String value) {
        digest = value;
    }

    public String getExpectedName() {
        return expectedName;
    }

    public void setExpectedName(String value) {
        expectedName = value;
    }

    public String getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(String value) {
        expectedVersion = value;
    }
}
