package org.us_ignite.thingworx.operator.api;

import io.fabric8.kubernetes.api.model.Condition;
import java.util.ArrayList;
import java.util.List;

/** Observed extension import state. */
public class ThingWorxExtensionStatus {
    private String phase;
    private String message;
    private List<Condition> conditions = new ArrayList<>();
    private String installerJob;
    private String lastAttemptedDigest;
    private String lastAttemptedFingerprint;
    private String observedDigest;
    private String observedFingerprint;
    private String previousDigest;
    private String importedName;
    private String importedVersion;
    private String requestedRestart;
    private Long observedGeneration;

    public String getPhase() {
        return phase;
    }

    public void setPhase(String value) {
        phase = value;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String value) {
        message = value;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> value) {
        conditions = value;
    }

    public String getInstallerJob() {
        return installerJob;
    }

    public void setInstallerJob(String value) {
        installerJob = value;
    }

    public String getLastAttemptedDigest() {
        return lastAttemptedDigest;
    }

    public void setLastAttemptedDigest(String value) {
        lastAttemptedDigest = value;
    }

    public String getLastAttemptedFingerprint() {
        return lastAttemptedFingerprint;
    }

    public void setLastAttemptedFingerprint(String value) {
        lastAttemptedFingerprint = value;
    }

    public String getObservedDigest() {
        return observedDigest;
    }

    public void setObservedDigest(String value) {
        observedDigest = value;
    }

    public String getObservedFingerprint() {
        return observedFingerprint;
    }

    public void setObservedFingerprint(String value) {
        observedFingerprint = value;
    }

    public String getPreviousDigest() {
        return previousDigest;
    }

    public void setPreviousDigest(String value) {
        previousDigest = value;
    }

    public String getImportedName() {
        return importedName;
    }

    public void setImportedName(String value) {
        importedName = value;
    }

    public String getImportedVersion() {
        return importedVersion;
    }

    public void setImportedVersion(String value) {
        importedVersion = value;
    }

    public String getRequestedRestart() {
        return requestedRestart;
    }

    public void setRequestedRestart(String value) {
        requestedRestart = value;
    }

    public Long getObservedGeneration() {
        return observedGeneration;
    }

    public void setObservedGeneration(Long value) {
        observedGeneration = value;
    }
}
