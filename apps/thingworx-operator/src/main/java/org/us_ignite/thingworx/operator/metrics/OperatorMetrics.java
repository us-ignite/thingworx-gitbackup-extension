package org.us_ignite.thingworx.operator.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Minimal in-process metrics for the ThingWorx operator.
 *
 * <p>Kept deliberately lightweight — no Micrometer/Prometheus client dependency. Exposes
 * Prometheus text format via {@link #prometheusExposition()} and is scraped by the
 * operator's {@code /metrics} HTTP endpoint (see {@code OperatorApplication}).
 *
 * <p>Metrics:
 * <ul>
 *   <li>{@code thingworx_operator_reconcile_total} — counter per controller/cluster
 *   <li>{@code thingworx_operator_reconcile_duration_seconds} — sum/count for duration histogram analogue
 *   <li>{@code thingworx_operator_phase} — gauge (1 for current phase) per cluster
 *   <li>{@code thingworx_operator_job_failures_total} — counter for failed Jobs
 * </ul>
 */
public final class OperatorMetrics {
    private static final ConcurrentHashMap<String, AtomicLong> PHASE_GAUGES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> RECONCILE_COUNTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> RECONCILE_DURATION_NANOS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> JOB_FAILURES = new ConcurrentHashMap<>();

    private OperatorMetrics() {}

    public static void recordReconcile(String controller, String clusterKey, long durationNanos) {
        String key = controller + "|" + clusterKey;
        RECONCILE_COUNTS.computeIfAbsent(key, k -> new LongAdder()).increment();
        RECONCILE_DURATION_NANOS.computeIfAbsent(key, k -> new LongAdder()).add(durationNanos);
    }

    public static void setPhase(String clusterKey, String phase) {
        // Gauge semantics: 1 for current phase, 0 for others. We store current phase as value 1 key.
        // Also expose numeric phase ordering for simple alerting: phase_ordinal.
        PHASE_GAUGES.computeIfAbsent(clusterKey + "|" + phase, k -> new AtomicLong(0)).set(1);
        // Clear every previous phase, including mixed-case extension phases.
        String prefix = clusterKey + "|";
        PHASE_GAUGES.forEach(
                (key, gauge) -> {
                    if (key.startsWith(prefix) && !key.equals(prefix + phase)) gauge.set(0);
                });
    }

    public static void incrementJobFailures(String clusterKey) {
        JOB_FAILURES.computeIfAbsent(clusterKey, k -> new LongAdder()).increment();
    }

    public static void incrementJobFailures(String clusterKey, String jobName) {
        incrementJobFailures(clusterKey);
        JOB_FAILURES.computeIfAbsent(clusterKey + "|" + jobName, k -> new LongAdder()).increment();
    }

    /** Prometheus text exposition (minimal, no HELP/TYPE duplication for brevity). */
    public static String prometheusExposition() {
        var sb = new StringBuilder();
        sb.append("# HELP thingworx_operator_reconcile_total Total reconciliations per controller/cluster\n");
        sb.append("# TYPE thingworx_operator_reconcile_total counter\n");
        for (Map.Entry<String, LongAdder> e : RECONCILE_COUNTS.entrySet()) {
            String[] parts = e.getKey().split("\\|", 2);
            String controller = parts[0];
            String cluster = parts.length > 1 ? parts[1] : "unknown";
            sb.append(String.format("thingworx_operator_reconcile_total{controller=\"%s\",cluster=\"%s\"} %d\n",
                    escape(controller), escape(cluster), e.getValue().longValue()));
        }
        sb.append("# HELP thingworx_operator_reconcile_duration_seconds Total reconcile duration seconds\n");
        sb.append("# TYPE thingworx_operator_reconcile_duration_seconds counter\n");
        for (Map.Entry<String, LongAdder> e : RECONCILE_DURATION_NANOS.entrySet()) {
            String[] parts = e.getKey().split("\\|", 2);
            String controller = parts[0];
            String cluster = parts.length > 1 ? parts[1] : "unknown";
            double seconds = e.getValue().longValue() / 1_000_000_000.0;
            sb.append(String.format("thingworx_operator_reconcile_duration_seconds{controller=\"%s\",cluster=\"%s\"} %f\n",
                    escape(controller), escape(cluster), seconds));
        }
        sb.append("# HELP thingworx_operator_phase Current lifecycle phase gauge (1 if active)\n");
        sb.append("# TYPE thingworx_operator_phase gauge\n");
        for (Map.Entry<String, AtomicLong> e : PHASE_GAUGES.entrySet()) {
            String[] parts = e.getKey().split("\\|", 2);
            String cluster = parts[0];
            String phase = parts.length > 1 ? parts[1] : "unknown";
            sb.append(String.format("thingworx_operator_phase{cluster=\"%s\",phase=\"%s\"} %d\n",
                    escape(cluster), escape(phase), e.getValue().get()));
        }
        sb.append("# HELP thingworx_operator_job_failures_total Total failed Jobs observed\n");
        sb.append("# TYPE thingworx_operator_job_failures_total counter\n");
        for (Map.Entry<String, LongAdder> e : JOB_FAILURES.entrySet()) {
            String key = e.getKey();
            if (key.contains("|")) {
                String[] parts = key.split("\\|", 2);
                sb.append(String.format("thingworx_operator_job_failures_total{cluster=\"%s\",job=\"%s\"} %d\n",
                        escape(parts[0]), escape(parts[1]), e.getValue().longValue()));
            } else {
                sb.append(String.format("thingworx_operator_job_failures_total{cluster=\"%s\"} %d\n",
                        escape(key), e.getValue().longValue()));
            }
        }
        return sb.toString();
    }

    private static String escape(String v) {
        return v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    /** Reset all accumulated metrics. */
    public static void reset() {
        PHASE_GAUGES.clear();
        RECONCILE_COUNTS.clear();
        RECONCILE_DURATION_NANOS.clear();
        JOB_FAILURES.clear();
    }
}
