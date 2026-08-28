package org.us_ignite.thingworx.operator.reconcile;

enum LifecyclePhase {
    PRECHECK,
    DATABASE,
    COORDINATION,
    PLATFORM_PRIMARY,
    PLATFORM_CLUSTER,
    CONNECTION_SERVERS,
    EDGE,
    OPTIONAL_SERVICES,
    READY,
    DEGRADED,
    INVALID;

    static LifecyclePhase from(String value) {
        if (value == null || value.isBlank()) return PRECHECK;
        try {
            return LifecyclePhase.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return INVALID;
        }
    }
}
