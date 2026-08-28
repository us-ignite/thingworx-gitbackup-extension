package org.us_ignite.thingworx.operator.api;

import io.fabric8.kubernetes.api.model.Condition;
import java.util.ArrayList;
import java.util.List;

/** Observed status exposed to users through kubectl. */
public class ThingWorxClusterStatus {
    private String phase;
    private String message;
    private Long observedGeneration;
    private String currentVersion;
    private String mode;
    private List<Condition> conditions = new ArrayList<>();

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getObservedGeneration() {
        return observedGeneration;
    }

    public void setObservedGeneration(Long observedGeneration) {
        this.observedGeneration = observedGeneration;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String value) {
        currentVersion = value;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String value) {
        mode = value;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> value) {
        conditions = value;
    }
}
