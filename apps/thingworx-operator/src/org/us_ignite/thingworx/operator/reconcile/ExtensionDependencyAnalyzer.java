package org.us_ignite.thingworx.operator.reconcile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.us_ignite.thingworx.operator.api.ThingWorxExtension;

/**
 * Pure extension dependency graph analysis — no KubernetesClient, no I/O.
 * Testable with in-memory Maps.
 */
public class ExtensionDependencyAnalyzer {

    public record DependencyAnalysis(
            String missingDependency, String crossClusterDependency, List<String> cycle) {}

    /**
     * Pure overload requested in the issue: extensionName + dependsOn + allForCluster map.
     * Also carries clusterName via lookup of extensionName entry if present, otherwise
     * assumes caller will use the 4-arg overload with explicit clusterName.
     */
    public DependencyAnalysis analyze(
            String extensionName, List<String> dependsOn, Map<String, ThingWorxExtension> allForCluster) {
        // Infer clusterName from the extension entry if present in map; fallback to empty.
        String clusterName = null;
        if (allForCluster != null && allForCluster.containsKey(extensionName)) {
            var self = allForCluster.get(extensionName);
            if (self != null && self.getSpec() != null && self.getSpec().getClusterRef() != null) {
                clusterName = self.getSpec().getClusterRef().getName();
            }
        }
        // If not found, we cannot check cross-cluster; just perform missing/cycle with empty cluster.
        // Prefer caller to use the 4-arg overload when cluster is known.
        if (clusterName == null) {
            // Build lookup from map values; use empty cluster for cycle detection (conservative)
            return analyze(extensionName, null, dependsOn, allForCluster);
        }
        return analyze(extensionName, clusterName, dependsOn, allForCluster);
    }

    public DependencyAnalysis analyze(
            String extensionName,
            String clusterName,
            List<String> dependsOn,
            Map<String, ThingWorxExtension> allByName) {
        var byName = allByName != null ? allByName : Map.<String, ThingWorxExtension>of();
        List<String> deps = dependsOn != null ? dependsOn : List.of();

        for (String dependency : deps) {
            var resource = byName.get(dependency);
            if (resource == null) return new DependencyAnalysis(dependency, null, null);
            if (clusterName != null) {
                if (resource.getSpec() == null
                        || resource.getSpec().getClusterRef() == null
                        || !clusterName.equals(resource.getSpec().getClusterRef().getName())) {
                    return new DependencyAnalysis(null, dependency, null);
                }
            }
        }

        var graph = new HashMap<String, List<String>>();
        for (var candidate : byName.values()) {
            if (candidate.getSpec() != null
                    && candidate.getSpec().getClusterRef() != null
                    && (clusterName == null
                            || clusterName.equals(candidate.getSpec().getClusterRef().getName()))) {
                graph.put(candidate.getMetadata().getName(), dependencyNames(candidate));
            } else if (clusterName == null) {
                // when clusterName unknown, include all
                if (candidate.getSpec() != null) {
                    graph.put(candidate.getMetadata().getName(), dependencyNames(candidate));
                }
            }
        }
        // Ensure current extension node is in graph even if not in map (for cycle reaching check)
        if (clusterName != null && !graph.containsKey(extensionName)) {
            graph.put(extensionName, deps);
        }
        var cycle = findCycle(graph);
        boolean reaches =
                cycle != null && reachesCycle(extensionName, graph, new HashSet<>(cycle));
        return new DependencyAnalysis(null, null, reaches ? cycle : null);
    }

    public DependencyAnalysis analyze(ThingWorxExtension extension, List<ThingWorxExtension> all) {
        var byName = new HashMap<String, ThingWorxExtension>();
        if (all != null) {
            for (var candidate : all) {
                if (candidate.getMetadata() != null && candidate.getMetadata().getName() != null) {
                    byName.put(candidate.getMetadata().getName(), candidate);
                }
            }
        }
        String extensionName = extension.getMetadata().getName();
        String clusterName =
                extension.getSpec() != null && extension.getSpec().getClusterRef() != null
                        ? extension.getSpec().getClusterRef().getName()
                        : null;
        List<String> dependsOn = dependencyNames(extension);
        var analysis = analyze(extensionName, clusterName, dependsOn, byName);
        return analysis;
    }

    // ---- Ordering / readiness (pure, no client) ----

    public boolean isNextForCluster(ThingWorxExtension extension, List<ThingWorxExtension> all) {
        String clusterName =
                extension.getSpec() != null && extension.getSpec().getClusterRef() != null
                        ? extension.getSpec().getClusterRef().getName()
                        : null;
        String extensionName = extension.getMetadata().getName();
        return isNextForCluster(extensionName, clusterName, all);
    }

    public boolean isNextForCluster(
            String extensionName, String clusterName, List<ThingWorxExtension> all) {
        if (clusterName == null || all == null) return true;
        return all.stream()
                .filter(
                        candidate ->
                                candidate.getSpec() != null
                                        && candidate.getSpec().getClusterRef() != null
                                        && clusterName.equals(
                                                candidate.getSpec().getClusterRef().getName()))
                .filter(
                        candidate ->
                                candidate.getStatus() == null
                                        || !"Ready".equals(candidate.getStatus().getPhase())
                                        || !Objects.equals(
                                                FingerprintComputer.fingerprint(candidate),
                                                candidate.getStatus().getObservedFingerprint()))
                .filter(candidate -> dependenciesReady(candidate, all))
                .min(Comparator.comparing(candidate -> candidate.getMetadata().getName()))
                .map(candidate -> candidate.getMetadata().getName())
                .map(extensionName::equals)
                .orElse(true);
    }

    public boolean dependenciesReady(ThingWorxExtension extension, List<ThingWorxExtension> all) {
        var byName = new HashMap<String, ThingWorxExtension>();
        if (all != null) {
            for (var candidate : all) {
                if (candidate.getMetadata() != null && candidate.getMetadata().getName() != null) {
                    byName.put(candidate.getMetadata().getName(), candidate);
                }
            }
        }
        return dependencyNames(extension).stream()
                .map(byName::get)
                .allMatch(
                        dependency ->
                                dependency != null
                                        && dependency.getStatus() != null
                                        && "Ready".equals(dependency.getStatus().getPhase())
                                        && dependency.getSpec() != null
                                        && dependency.getSpec().getArtifact() != null
                                        && Objects.equals(
                                                FingerprintComputer.fingerprint(dependency),
                                                dependency.getStatus().getObservedFingerprint()));
    }

    /** Validate self-dependency and duplicate entries (pure). Returns error message or null. */
    public String validate(String extensionName, List<String> dependsOn) {
        List<String> deps = dependsOn != null ? dependsOn : List.of();
        if (deps.contains(extensionName)) return "An extension cannot depend on itself.";
        if (new HashSet<>(deps).size() != deps.size())
            return "spec.dependsOn must not contain duplicate extension names.";
        return null;
    }

    public String validate(ThingWorxExtension extension) {
        var dependsOn = dependencyNames(extension);
        String name = extension.getMetadata() != null ? extension.getMetadata().getName() : null;
        if (name != null && dependsOn.contains(name)) return "An extension cannot depend on itself.";
        if (new HashSet<>(dependsOn).size() != dependsOn.size())
            return "spec.dependsOn must not contain duplicate extension names.";
        return null;
    }

    // ---- helpers ----

    private List<String> dependencyNames(ThingWorxExtension extension) {
        return extension.getSpec() == null || extension.getSpec().getDependsOn() == null
                ? List.of()
                : extension.getSpec().getDependsOn();
    }

    private boolean reachesCycle(String node, Map<String, List<String>> graph, Set<String> cycle) {
        return reachesCycle(node, graph, cycle, new HashSet<>());
    }

    private boolean reachesCycle(
            String node, Map<String, List<String>> graph, Set<String> cycle, Set<String> visited) {
        if (cycle.contains(node)) return true;
        if (!visited.add(node)) return false;
        for (String dependency : graph.getOrDefault(node, List.of())) {
            if (reachesCycle(dependency, graph, cycle, visited)) return true;
        }
        return false;
    }

    private List<String> findCycle(Map<String, List<String>> graph) {
        var states = new HashMap<String, Integer>();
        var path = new ArrayList<String>();
        for (String node : graph.keySet().stream().sorted().toList()) {
            var cycle = findCycle(node, graph, states, path);
            if (cycle != null) return cycle;
        }
        return null;
    }

    private List<String> findCycle(
            String node,
            Map<String, List<String>> graph,
            Map<String, Integer> states,
            List<String> path) {
        if (states.getOrDefault(node, 0) == 1) {
            var start = path.indexOf(node);
            var cycle = new ArrayList<>(path.subList(start, path.size()));
            cycle.add(node);
            return cycle;
        }
        if (states.getOrDefault(node, 0) == 2) return null;
        states.put(node, 1);
        path.add(node);
        for (String dependency : graph.getOrDefault(node, List.of()).stream().sorted().toList()) {
            if (!graph.containsKey(dependency)) continue;
            var cycle = findCycle(dependency, graph, states, path);
            if (cycle != null) return cycle;
        }
        path.removeLast();
        states.put(node, 2);
        return null;
    }
}
