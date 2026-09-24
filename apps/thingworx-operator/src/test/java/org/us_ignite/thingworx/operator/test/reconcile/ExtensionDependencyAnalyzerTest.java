package org.us_ignite.thingworx.operator.test.reconcile;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatusBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.us_ignite.thingworx.operator.api.ClusterReference;
import org.us_ignite.thingworx.operator.api.OciArtifactReference;
import org.us_ignite.thingworx.operator.api.ThingWorxExtension;
import org.us_ignite.thingworx.operator.api.ThingWorxExtensionSpec;
import org.us_ignite.thingworx.operator.api.ThingWorxExtensionStatus;
import org.us_ignite.thingworx.operator.reconcile.ExtensionDependencyAnalyzer;
import org.us_ignite.thingworx.operator.reconcile.FingerprintComputer;
import org.us_ignite.thingworx.operator.reconcile.PlatformReadinessChecker;

class ExtensionDependencyAnalyzerTest {

    private final ExtensionDependencyAnalyzer analyzer = new ExtensionDependencyAnalyzer();

    // --- dependency analysis pure tests ---

    @Test
    void reportsMissingDependencyWithItsName() {
        var all = map(extension("alpha", "production", "missing"));
        var ext = extension("alpha", "production", "missing");

        var result = analyzer.analyze(ext, List.copyOf(all.values()));

        assertThat(result.missingDependency()).isEqualTo("missing");
        assertThat(result.crossClusterDependency()).isNull();
        assertThat(result.cycle()).isNull();
    }

    @Test
    void reportsMissingViaStringOverload() {
        var shared = extension("shared", "production");
        Map<String, ThingWorxExtension> byName = Map.of("shared", shared);
        var result = analyzer.analyze("alpha", "production", List.of("missing"), byName);
        assertThat(result.missingDependency()).isEqualTo("missing");
    }

    @Test
    void rejectsDependencyAssignedToAnotherCluster() {
        var shared = extension("shared", "other-cluster");
        var alpha = extension("alpha", "production", "shared");
        var all = List.of(shared, alpha);

        var result = analyzer.analyze(alpha, all);

        assertThat(result.crossClusterDependency()).isEqualTo("shared");
        assertThat(result.missingDependency()).isNull();
    }

    @Test
    void detectsTwoNodeDependencyCycle() {
        var alpha = extension("alpha", "production", "beta");
        var beta = extension("beta", "production", "alpha");
        var all = List.of(alpha, beta);

        var result = analyzer.analyze(alpha, all);

        assertThat(result.cycle()).isNotNull();
        assertThat(result.cycle()).contains("alpha", "beta");
    }

    @Test
    void detectsThreeNodeDependencyCycle() {
        var alpha = extension("alpha", "production", "beta");
        var beta = extension("beta", "production", "gamma");
        var gamma = extension("gamma", "production", "alpha");
        var all = List.of(alpha, beta, gamma);

        var result = analyzer.analyze(alpha, all);

        assertThat(result.cycle()).isNotNull();
        assertThat(result.cycle()).contains("alpha", "beta", "gamma");
    }

    @Test
    void cycleNotReportedWhenUnrelatedToExtension() {
        // alpha -> beta, gamma <-> delta cycle unrelated
        var alpha = extension("alpha", "production", "beta");
        var beta = extension("beta", "production");
        var gamma = extension("gamma", "production", "delta");
        var delta = extension("delta", "production", "gamma");
        var all = List.of(alpha, beta, gamma, delta);

        var result = analyzer.analyze(alpha, all);

        // alpha does not reach the gamma/delta cycle, so cycle should be null for alpha
        assertThat(result.cycle()).isNull();
        assertThat(result.missingDependency()).isNull();
    }

    @Test
    void orderingIsNextForClusterRespectsDependencies() {
        // beta has no deps, alpha depends on beta. When beta not Ready, alpha is not next, beta is next.
        var beta = extension("beta", "production");
        var alpha = extension("alpha", "production", "beta");
        var all = List.of(alpha, beta);

        // beta should be next because its dependencies are ready (none)
        assertThat(analyzer.isNextForCluster(beta, all)).isTrue();
        // alpha should not be next because its dependency not ready
        assertThat(analyzer.isNextForCluster(alpha, all)).isFalse();
        assertThat(analyzer.dependenciesReady(alpha, all)).isFalse();
    }

    @Test
    void orderingBecomesNextAfterDependencyReady() {
        var beta = extension("beta", "production");
        // make beta Ready with correct fingerprint
        var betaReady = markReady(beta);
        var alpha = extension("alpha", "production", "beta");
        var all = List.of(alpha, betaReady);

        assertThat(analyzer.dependenciesReady(alpha, all)).isTrue();
        // Both candidates? Now beta is Ready, so filtered out from "not ready" set.
        // Therefore alpha should be next.
        assertThat(analyzer.isNextForCluster(alpha, all)).isTrue();
        // Sorting: smallest name among not-ready with ready deps wins. Create extra "aardvark" depends on beta but not ready?
        var aardvark = extension("aardvark", "production", "beta");
        var all2 = List.of(alpha, betaReady, aardvark);
        // aardvark < alpha, so aardvark should be next, not alpha
        assertThat(analyzer.isNextForCluster(alpha, all2)).isFalse();
        assertThat(analyzer.isNextForCluster(aardvark, all2)).isTrue();
    }

    @Test
    void dependenciesReadyFailsWhenFingerprintMismatch() {
        var beta = extension("beta", "production");
        var status = new ThingWorxExtensionStatus();
        status.setPhase("Ready");
        // set observedFingerprint to something else (stale)
        status.setObservedFingerprint("stale-fingerprint");
        beta.setStatus(status);
        var alpha = extension("alpha", "production", "beta");
        var all = List.of(alpha, beta);

        assertThat(analyzer.dependenciesReady(alpha, all)).isFalse();
    }

    @Test
    void rejectsSelfDependency() {
        var err = analyzer.validate("alpha", List.of("alpha"));
        assertThat(err).contains("cannot depend on itself");

        var ext = extension("alpha", "production", "alpha");
        var validation = analyzer.validate(ext);
        assertThat(validation).contains("cannot depend on itself");
    }

    @Test
    void rejectsDuplicateDependencies() {
        var err = analyzer.validate("beta", List.of("ready", "ready"));
        assertThat(err).contains("duplicate");

        var ext = extension("beta", "production", "ready", "ready");
        assertThat(analyzer.validate(ext)).contains("duplicate");
    }

    @Test
    void missingDependencyPrecedenceOverCycle() {
        // If dependency missing, should return missing before cycle detection
        var alpha = extension("alpha", "production", "missing");
        var all = List.of(alpha);
        var result = analyzer.analyze(alpha, all);
        assertThat(result.missingDependency()).isEqualTo("missing");
        assertThat(result.cycle()).isNull();
    }

    // --- fingerprint pure tests ---

    @Test
    void fingerprintIsDeterministic() {
        var a = extension("entities", "production");
        var b = extension("entities", "production");
        assertThat(FingerprintComputer.fingerprint(a)).isEqualTo(FingerprintComputer.fingerprint(b));
        assertThat(FingerprintComputer.fingerprint(a)).matches("[0-9a-f]{64}");
    }

    @Test
    void fingerprintChangesWhenDigestChanges() {
        var a = extension("entities", "production");
        var b = extension("entities", "production");
        b.getSpec().getArtifact().setDigest("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertThat(FingerprintComputer.fingerprint(a)).isNotEqualTo(FingerprintComputer.fingerprint(b));
    }

    @Test
    void fingerprintChangesWhenPolicyChanges() {
        var a = extension("entities", "production");
        var b = extension("entities", "production");
        b.getSpec().getImportPolicy().setJarResources(true);
        assertThat(FingerprintComputer.fingerprint(a)).isNotEqualTo(FingerprintComputer.fingerprint(b));
    }

    @Test
    void fingerprintChangesWhenPullSecretChanges() {
        var a = extension("entities", "production");
        var b = extension("entities", "production");
        b.getSpec().setArtifactPullSecret("my-secret");
        assertThat(FingerprintComputer.fingerprint(a)).isNotEqualTo(FingerprintComputer.fingerprint(b));
    }

    @Test
    void fingerprintIncludesExpectedNameAndVersion() {
        var a = extension("entities", "production");
        a.getSpec().getArtifact().setExpectedName("my-ext");
        a.getSpec().getArtifact().setExpectedVersion("1.2.3");
        var b = extension("entities", "production");
        b.getSpec().getArtifact().setExpectedName("other-ext");
        b.getSpec().getArtifact().setExpectedVersion("1.2.3");
        assertThat(FingerprintComputer.fingerprint(a)).isNotEqualTo(FingerprintComputer.fingerprint(b));
    }

    // --- platform readiness pure tests ---

    @Test
    void platformReadyOnlyWhenHealthy() {
        var ready = platform(
                "sha256:restart",
                new StatefulSetStatusBuilder()
                        .withReplicas(1)
                        .withUpdatedReplicas(1)
                        .withReadyReplicas(1)
                        .withAvailableReplicas(1)
                        .withCurrentRevision("new")
                        .withUpdateRevision("new")
                        .build());
        assertThat(PlatformReadinessChecker.isReady(ready, "sha256:restart")).isTrue();
    }

    @Test
    void platformNotReadyWhenRevisionMismatch() {
        var notReady = platform(
                "sha256:restart",
                new StatefulSetStatusBuilder()
                        .withReplicas(1)
                        .withReadyReplicas(1)
                        .withAvailableReplicas(1)
                        .withCurrentRevision("old")
                        .withUpdateRevision("new")
                        .build());
        assertThat(PlatformReadinessChecker.isReady(notReady, "sha256:restart")).isFalse();
    }

    @Test
    void platformNotReadyWhenAnnotationMismatch() {
        var ss = platform(
                "sha256:other",
                new StatefulSetStatusBuilder()
                        .withReplicas(1)
                        .withUpdatedReplicas(1)
                        .withReadyReplicas(1)
                        .withAvailableReplicas(1)
                        .withCurrentRevision("new")
                        .withUpdateRevision("new")
                        .build());
        assertThat(PlatformReadinessChecker.isReady(ss, "sha256:restart")).isFalse();
    }

    @Test
    void platformNotReadyWhenNull() {
        assertThat(PlatformReadinessChecker.isReady(null, "sha256:restart")).isFalse();
        var ss = new StatefulSetBuilder()
                .withNewSpec().withReplicas(1).endSpec()
                .withNewStatus().withReplicas(1).endStatus()
                .build();
        // missing template annotations
        assertThat(PlatformReadinessChecker.isReady(ss, "sha256:restart")).isFalse();
    }

    // --- helpers ---

    private ThingWorxExtension extension(String name, String cluster, String... dependencies) {
        var artifact = new OciArtifactReference();
        artifact.setRepository("registry.example/extensions/" + name);
        artifact.setDigest("sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        var spec = new ThingWorxExtensionSpec();
        var ref = new ClusterReference();
        ref.setName(cluster);
        spec.setClusterRef(ref);
        spec.setArtifact(artifact);
        spec.setDependsOn(dependencies == null ? List.of() : List.of(dependencies));
        var extension = new ThingWorxExtension();
        extension.setMetadata(
                new ObjectMetaBuilder()
                        .withName(name)
                        .withNamespace("thingworx")
                        .withGeneration(1L)
                        .build());
        extension.setSpec(spec);
        return extension;
    }

    private ThingWorxExtension markReady(ThingWorxExtension ext) {
        var status = new ThingWorxExtensionStatus();
        status.setPhase("Ready");
        status.setObservedFingerprint(FingerprintComputer.fingerprint(ext));
        status.setObservedDigest(ext.getSpec().getArtifact().getDigest());
        ext.setStatus(status);
        return ext;
    }

    private Map<String, ThingWorxExtension> map(ThingWorxExtension... extensions) {
        var m = new HashMap<String, ThingWorxExtension>();
        for (var e : extensions) m.put(e.getMetadata().getName(), e);
        return m;
    }

    private StatefulSet platform(String restart, io.fabric8.kubernetes.api.model.apps.StatefulSetStatus status) {
        var statefulSet = new StatefulSetBuilder()
                .withNewMetadata().withName("production-platform").withNamespace("thingworx").endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata().withAnnotations(Map.of("thingworx.us-ignite.org/extension-restart", restart)).endMetadata()
                .endTemplate()
                .endSpec()
                .build();
        statefulSet.setStatus(status);
        return statefulSet;
    }
}
