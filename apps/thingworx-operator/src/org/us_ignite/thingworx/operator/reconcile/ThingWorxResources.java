package org.us_ignite.thingworx.operator.reconcile;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.us_ignite.thingworx.operator.api.SecretKeyReference;
import org.us_ignite.thingworx.operator.api.ThingWorxCluster;

/**
 * Builds Kubernetes resources from a cluster spec. No PTC binaries or secret values are embedded.
 */
final class ThingWorxResources {
    private ThingWorxResources() {}

    static String name(ThingWorxCluster cluster, String suffix) {
        return cluster.getMetadata().getName() + "-" + suffix;
    }

    static Map<String, String> labels(ThingWorxCluster cluster, String component) {
        return Map.of(
                "app.kubernetes.io/managed-by",
                "thingworx-operator",
                "app.kubernetes.io/instance",
                cluster.getMetadata().getName(),
                "app.kubernetes.io/component",
                component);
    }

    static List<HasMetadata> precheck(ThingWorxCluster cluster) {
        var spec = cluster.getSpec();
        var resources = new ArrayList<HasMetadata>();
        resources.add(
                new ConfigMapBuilder()
                        .withMetadata(
                                new ObjectMetaBuilder()
                                        .withName(name(cluster, "runtime"))
                                        .withNamespace(cluster.getMetadata().getNamespace())
                                        .withLabels(labels(cluster, "runtime"))
                                        .build())
                        .withData(
                                Map.of(
                                        "database-host",
                                        databaseHost(cluster),
                                        "database-port",
                                        String.valueOf(spec.getDatabase().getPort()),
                                        "database-name",
                                        spec.getDatabase().getDatabase(),
                                        "database-schema",
                                        spec.getDatabase().getSchema()))
                        .build());
        resources.add(
                pvc(
                        cluster,
                        "shared",
                        spec.getStorage().getSharedStorageSize(),
                        List.of(spec.isEnableHA() ? "ReadWriteMany" : "ReadWriteOnce")));
        resources.add(service(cluster, "platform", 8080));
        if (spec.isEnableHA()) {
            resources.add(headlessService(cluster, "zookeeper", 2181));
            resources.add(headlessService(cluster, "ignite", 10800));
            resources.add(service(cluster, "cxserver", 8080));
            resources.add(service(cluster, "haproxy", 8080));
        }
        if (spec.getDatabase().isInternal()) {
            resources.add(
                    pvc(
                            cluster,
                            "postgres-data",
                            spec.getStorage().getComponentStorageSize(),
                            List.of("ReadWriteOnce")));
            resources.add(service(cluster, "postgres", 5432));
            resources.add(postgres(cluster));
        }
        return resources;
    }

    static List<HasMetadata> database(ThingWorxCluster cluster) {
        var databaseInit =
                job(
                        cluster,
                        "database-init",
                        cluster.getSpec().getImages().getDatabaseInit(),
                        List.of(
                                "/bin/sh",
                                "-ec",
                                "/usr/local/bin/db-check.sh && /usr/local/bin/db-setup.sh"),
                        true);
        var securityInit =
                job(
                        cluster,
                        "security-init",
                        cluster.getSpec().getImages().getSecurityCli(),
                        List.of("/bin/sh", "-ec", "/opt/docker-entrypoint.sh"),
                        false);
        var podSpec = securityInit.getSpec().getTemplate().getSpec();
        addVolume(
                podSpec,
                new VolumeBuilder()
                        .withName("thingworx-storage")
                        .withNewPersistentVolumeClaim()
                        .withClaimName(name(cluster, "shared"))
                        .endPersistentVolumeClaim()
                        .build());
        var container = podSpec.getContainers().getFirst();
        container.setVolumeMounts(
                List.of(
                        new VolumeMountBuilder()
                                .withName("thingworx-storage")
                                .withMountPath("/ThingworxStorage")
                                .build()));
        var environment = new ArrayList<EnvVar>();
        environment.add(env("KEYSTORE", "true"));
        environment.add(env("KEYSTORE_PASSWORD_FILE_PATH", "/ThingworxStorage"));
        environment.add(env("KEYSTORE_FILE_PATH", "/ThingworxStorage"));
        addSecret(environment, "KEYSTORE_PASSWORD", cluster.getSpec().getKeystorePassword());
        addSecret(
                environment,
                "SECRET_PROVISIONING_APP_KEY",
                cluster.getSpec().getProvisioningAppKey());
        if (cluster.getSpec().isEnableHA()) {
            addSecret(environment, "SECRET_CX_APP_KEY", cluster.getSpec().getCxServerAppKey());
        }
        container.setEnv(environment);
        return List.of(databaseInit, securityInit);
    }

    static List<HasMetadata> coordination(ThingWorxCluster cluster) {
        var spec = cluster.getSpec();
        var zkConnection = zookeeperConnection(cluster);
        var igniteConfig =
                new ConfigMapBuilder()
                        .withMetadata(
                                new ObjectMetaBuilder()
                                        .withName(name(cluster, "ignite-config"))
                                        .withNamespace(cluster.getMetadata().getNamespace())
                                        .withLabels(labels(cluster, "ignite"))
                                        .build())
                        .withData(
                                Map.of(
                                        "ignite.xml",
                                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                                + "<beans xmlns=\"http://www.springframework.org/schema/beans\"\n"
                                                + "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                                + "       xsi:schemaLocation=\"\n"
                                                + "       http://www.springframework.org/schema/beans\n"
                                                + "       http://www.springframework.org/schema/beans/spring-beans.xsd\">\n"
                                                + "  <bean class=\"org.apache.ignite.configuration.IgniteConfiguration\">\n"
                                                + "    <property name=\"discoverySpi\">\n"
                                                + "      <bean class=\"org.apache.ignite.spi.discovery.zk.ZookeeperDiscoverySpi\">\n"
                                                + "        <property name=\"zkConnectionString\" value=\"${ZK_CONNECTION}\"/>\n"
                                                + "        <property name=\"basePath\" value=\"/ignite\"/>\n"
                                                + "      </bean>\n"
                                                + "    </property>\n"
                                                + "  </bean>\n"
                                                + "</beans>"))
                        .build();
        var igniteContainer =
                new ContainerBuilder()
                        .withName("ignite")
                        .withImage(spec.getImages().getIgnite())
                        .withImagePullPolicy("IfNotPresent")
                        .withEnv(
                                List.of(
                                        env("OPTION_LIBS", "ignite-zookeeper"),
                                        env("CONFIG_URI", "config/ignite-custom.xml"),
                                        env(
                                                "JVM_OPTS",
                                                "-Xms1g -Xmx1g -server -XX:MaxMetaspaceSize=256m -DZK_CONNECTION="
                                                        + zkConnection)))
                        .addNewPort()
                        .withContainerPort(10800)
                        .endPort()
                        .addNewVolumeMount()
                        .withName("ignite-config")
                        .withMountPath("/opt/ignite/apache-ignite/config/ignite-custom.xml")
                        .withSubPath("ignite.xml")
                        .endVolumeMount()
                        .build();
        var ignitePod =
                new PodTemplateSpecBuilder()
                        .withMetadata(
                                new ObjectMetaBuilder()
                                        .withLabels(labels(cluster, "ignite"))
                                        .build())
                        .withNewSpec()
                        .withContainers(igniteContainer)
                        .addToVolumes(
                                new VolumeBuilder()
                                        .withName("ignite-config")
                                        .withNewConfigMap()
                                        .withName(name(cluster, "ignite-config"))
                                        .endConfigMap()
                                        .build())
                        .endSpec()
                        .build();
        var igniteSts =
                new StatefulSetBuilder()
                        .withMetadata(
                                new ObjectMetaBuilder()
                                        .withName(name(cluster, "ignite"))
                                        .withNamespace(cluster.getMetadata().getNamespace())
                                        .withLabels(labels(cluster, "ignite"))
                                        .build())
                        .withNewSpec()
                        .withServiceName(name(cluster, "ignite"))
                        .withReplicas(2)
                        .withSelector(
                                new io.fabric8.kubernetes.api.model.LabelSelectorBuilder()
                                        .withMatchLabels(labels(cluster, "ignite"))
                                        .build())
                        .withTemplate(ignitePod)
                        .endSpec()
                        .build();
        return List.of(zookeeper(cluster), igniteConfig, igniteSts);
    }

    static List<HasMetadata> platform(ThingWorxCluster cluster, int replicas) {
        var platform =
                statefulSet(
                        cluster,
                        "platform",
                        cluster.getSpec().getImages().getPlatform(),
                        replicas,
                        8080,
                        platformEnvironment(cluster),
                        "shared");
        var podSpec = platform.getSpec().getTemplate().getSpec();
        if (cluster.getSpec().getSettingsConfigMap() != null) {
            addVolume(
                    podSpec,
                    new VolumeBuilder()
                            .withName("settings")
                            .withNewConfigMap()
                            .withName(cluster.getSpec().getSettingsConfigMap())
                            .endConfigMap()
                            .build());
            addMount(
                    podSpec.getContainers().getFirst(),
                    new io.fabric8.kubernetes.api.model.VolumeMountBuilder()
                            .withName("settings")
                            .withMountPath("/ThingworxPlatform/settings")
                            .withReadOnly(true)
                            .build());
        }
        if (cluster.getSpec().getLicenseSecret() != null) {
            addVolume(
                    podSpec,
                    new VolumeBuilder()
                            .withName("license")
                            .withNewSecret()
                            .withSecretName(cluster.getSpec().getLicenseSecret())
                            .endSecret()
                            .build());
            addMount(
                    podSpec.getContainers().getFirst(),
                    new io.fabric8.kubernetes.api.model.VolumeMountBuilder()
                            .withName("license")
                            .withSubPath("license.bin")
                            .withMountPath("/ThingworxPlatform/license.bin")
                            .withReadOnly(true)
                            .build());
        }
        return List.of(platform);
    }

    static List<HasMetadata> connectionServers(ThingWorxCluster cluster) {
        var environment =
                new ArrayList<EnvVar>(
                        List.of(
                                env("CXSERVER_SSL_ENABLED", "false"),
                                env("CXSERVER_HTTPS_ACTIVE", "false"),
                                env("SERVICE_DISCOVERY_ENDPOINT", zookeeperConnection(cluster)),
                                env("TWX_PLATFORM_SERVICE_NAME", name(cluster, "platform")),
                                env("TWX_PLATFORM_TLS_ENABLED", "false"),
                                env("TWX_PLATFORM_TRANSPORT", "WEBSOCKETS_ACTIVE_ACTIVE")));
        addSecret(environment, "CXSERVER_APP_KEY", cluster.getSpec().getCxServerAppKey());
        var deployment =
                deployment(
                        cluster,
                        "cxserver",
                        cluster.getSpec().getImages().getCxServer(),
                        2,
                        8080,
                        environment);
        var container = deployment.getSpec().getTemplate().getSpec().getContainers().getFirst();
        addPort(
                container,
                new ContainerPortBuilder().withName("health").withContainerPort(8081).build());
        container.setStartupProbe(tcpProbe(8081, 10, 30));
        container.setReadinessProbe(tcpProbe(8081, 10, 6));
        return List.of(deployment);
    }

    static List<HasMetadata> edge(ThingWorxCluster cluster) {
        var resources = new ArrayList<HasMetadata>();
        resources.add(
                new ConfigMapBuilder()
                        .withMetadata(
                                new ObjectMetaBuilder()
                                        .withName(name(cluster, "haproxy-config"))
                                        .withNamespace(cluster.getMetadata().getNamespace())
                                        .withLabels(labels(cluster, "haproxy"))
                                        .build())
                        .withData(Map.of("haproxy.cfg", haproxyConfig(cluster)))
                        .build());
        var haproxy =
                deployment(
                        cluster,
                        "haproxy",
                        cluster.getSpec().getImages().getHaProxy(),
                        2,
                        8080,
                        List.of());
        addVolume(
                haproxy.getSpec().getTemplate().getSpec(),
                new VolumeBuilder()
                        .withName("config")
                        .withNewConfigMap()
                        .withName(name(cluster, "haproxy-config"))
                        .endConfigMap()
                        .build());
        addMount(
                haproxy.getSpec().getTemplate().getSpec().getContainers().getFirst(),
                new io.fabric8.kubernetes.api.model.VolumeMountBuilder()
                        .withName("config")
                        .withMountPath("/usr/local/etc/haproxy")
                        .withReadOnly(true)
                        .build());
        resources.add(haproxy);
        if (cluster.getSpec().getIngress().getHost() != null) resources.add(ingress(cluster));
        return resources;
    }

    static List<HasMetadata> directEdge(ThingWorxCluster cluster) {
        return cluster.getSpec().getIngress().getHost() == null
                ? List.of()
                : List.of(ingress(cluster));
    }

    static List<HasMetadata> optional(ThingWorxCluster cluster) {
        var resources = new ArrayList<HasMetadata>();
        var spec = cluster.getSpec();
        if (spec.isKafkaEnabled()) {
            resources.add(
                    pvc(
                            cluster,
                            "kafka-data",
                            spec.getStorage().getComponentStorageSize(),
                            List.of("ReadWriteOnce")));
            resources.add(
                    statefulSet(
                            cluster,
                            "kafka",
                            spec.getImages().getKafka(),
                            1,
                            9092,
                            List.of(),
                            "kafka-data"));
        }
        if (spec.isOtelEnabled())
            resources.add(
                    deployment(
                            cluster,
                            "otel",
                            spec.getImages().getOtelCollector(),
                            1,
                            4317,
                            List.of()));
        return resources;
    }

    private static io.fabric8.kubernetes.api.model.PersistentVolumeClaim pvc(
            ThingWorxCluster cluster, String suffix, String size, List<String> modes) {
        var storage = cluster.getSpec().getStorage();
        return new PersistentVolumeClaimBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withName(name(cluster, suffix))
                                .withNamespace(cluster.getMetadata().getNamespace())
                                .withLabels(labels(cluster, suffix))
                                .build())
                .withNewSpec()
                .withAccessModes(modes)
                .withStorageClassName(storage.getStorageClassName())
                .withResources(
                        new io.fabric8.kubernetes.api.model.VolumeResourceRequirementsBuilder()
                                .addToRequests(
                                        "storage",
                                        new io.fabric8.kubernetes.api.model.Quantity(size))
                                .build())
                .endSpec()
                .build();
    }

    private static io.fabric8.kubernetes.api.model.Service service(
            ThingWorxCluster cluster, String component, int port) {
        return new ServiceBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withName(name(cluster, component))
                                .withNamespace(cluster.getMetadata().getNamespace())
                                .withLabels(labels(cluster, component))
                                .build())
                .withNewSpec()
                .withSelector(labels(cluster, component))
                .addNewPort()
                .withPort(port)
                .withTargetPort(new io.fabric8.kubernetes.api.model.IntOrString(port))
                .endPort()
                .endSpec()
                .build();
    }

    private static io.fabric8.kubernetes.api.model.Service headlessService(
            ThingWorxCluster cluster, String component, int port) {
        var service = service(cluster, component, port);
        service.getSpec().setClusterIP("None");
        service.getSpec().setPublishNotReadyAddresses(true);
        return service;
    }

    private static StatefulSet zookeeper(ThingWorxCluster cluster) {
        var component = "zookeeper";
        var pod =
                pod(
                        cluster,
                        component,
                        cluster.getSpec().getImages().getZookeeper(),
                        2181,
                        List.of(
                                env("ZOO_SERVERS", zookeeperServers(cluster)),
                                env("ZOO_4LW_COMMANDS_WHITELIST", "ruok,mntr")),
                        null);
        pod.getSpec()
                .getContainers()
                .getFirst()
                .setVolumeMounts(
                        List.of(
                                new VolumeMountBuilder()
                                        .withName("data")
                                        .withMountPath("/data")
                                        .build()));
        return new StatefulSetBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withName(name(cluster, component))
                                .withNamespace(cluster.getMetadata().getNamespace())
                                .withLabels(labels(cluster, component))
                                .build())
                .withNewSpec()
                .withServiceName(name(cluster, component))
                .withPodManagementPolicy("Parallel")
                .withReplicas(3)
                .withSelector(
                        new io.fabric8.kubernetes.api.model.LabelSelectorBuilder()
                                .withMatchLabels(labels(cluster, component))
                                .build())
                .withTemplate(pod)
                .withVolumeClaimTemplates(
                        new PersistentVolumeClaimBuilder()
                                .withMetadata(new ObjectMetaBuilder().withName("data").build())
                                .withNewSpec()
                                .withAccessModes("ReadWriteOnce")
                                .withStorageClassName(
                                        cluster.getSpec().getStorage().getStorageClassName())
                                .withResources(
                                        new io.fabric8.kubernetes.api.model
                                                        .VolumeResourceRequirementsBuilder()
                                                .addToRequests(
                                                        "storage",
                                                        new io.fabric8.kubernetes.api.model
                                                                .Quantity(
                                                                cluster.getSpec()
                                                                        .getStorage()
                                                                        .getComponentStorageSize()))
                                                .build())
                                .endSpec()
                                .build())
                .endSpec()
                .build();
    }

    private static io.fabric8.kubernetes.api.model.apps.StatefulSet postgres(
            ThingWorxCluster cluster) {
        var database = cluster.getSpec().getDatabase();
        var container =
                new ContainerBuilder()
                        .withName("postgres")
                        .withImage(database.getImage())
                        .withImagePullPolicy("IfNotPresent")
                        .withEnv(
                                List.of(
                                        env("POSTGRES_DB", database.getDatabase()),
                                        env("POSTGRES_USER", database.getAdminUsername()),
                                        secretEnv(
                                                "POSTGRES_PASSWORD",
                                                database.getAdminCredentials().getName(),
                                                database.getAdminCredentials().getKey())))
                        .addNewPort()
                        .withContainerPort(5432)
                        .endPort()
                        .withVolumeMounts(
                                new io.fabric8.kubernetes.api.model.VolumeMountBuilder()
                                        .withName("data")
                                        .withMountPath("/var/lib/postgresql/data")
                                        .build())
                        .build();
        var pod =
                new PodTemplateSpecBuilder()
                        .withMetadata(
                                new ObjectMetaBuilder()
                                        .withLabels(labels(cluster, "postgres"))
                                        .build())
                        .withNewSpec()
                        .withContainers(container)
                        .addToVolumes(
                                new VolumeBuilder()
                                        .withName("data")
                                        .withNewPersistentVolumeClaim()
                                        .withClaimName(name(cluster, "postgres-data"))
                                        .endPersistentVolumeClaim()
                                        .build())
                        .endSpec()
                        .build();
        return new StatefulSetBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withName(name(cluster, "postgres"))
                                .withNamespace(cluster.getMetadata().getNamespace())
                                .withLabels(labels(cluster, "postgres"))
                                .build())
                .withNewSpec()
                .withServiceName(name(cluster, "postgres"))
                .withReplicas(1)
                .withSelector(
                        new io.fabric8.kubernetes.api.model.LabelSelectorBuilder()
                                .withMatchLabels(labels(cluster, "postgres"))
                                .build())
                .withTemplate(pod)
                .endSpec()
                .build();
    }

    private static io.fabric8.kubernetes.api.model.apps.StatefulSet statefulSet(
            ThingWorxCluster cluster,
            String component,
            String image,
            int replicas,
            int port,
            List<EnvVar> env,
            String claimName) {
        var pod = pod(cluster, component, image, port, env, claimName);
        return new StatefulSetBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withName(name(cluster, component))
                                .withNamespace(cluster.getMetadata().getNamespace())
                                .withLabels(labels(cluster, component))
                                .build())
                .withNewSpec()
                .withServiceName(name(cluster, component))
                .withReplicas(replicas)
                .withSelector(
                        new io.fabric8.kubernetes.api.model.LabelSelectorBuilder()
                                .withMatchLabels(labels(cluster, component))
                                .build())
                .withTemplate(pod)
                .endSpec()
                .build();
    }

    private static io.fabric8.kubernetes.api.model.apps.Deployment deployment(
            ThingWorxCluster cluster,
            String component,
            String image,
            int replicas,
            int port,
            List<EnvVar> env) {
        return new DeploymentBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withName(name(cluster, component))
                                .withNamespace(cluster.getMetadata().getNamespace())
                                .withLabels(labels(cluster, component))
                                .build())
                .withNewSpec()
                .withReplicas(replicas)
                .withSelector(
                        new io.fabric8.kubernetes.api.model.LabelSelectorBuilder()
                                .withMatchLabels(labels(cluster, component))
                                .build())
                .withTemplate(pod(cluster, component, image, port, env, null))
                .endSpec()
                .build();
    }

    private static PodTemplateSpec pod(
            ThingWorxCluster cluster,
            String component,
            String image,
            int port,
            List<EnvVar> environment,
            String claimName) {
        var container =
                new ContainerBuilder()
                        .withName(component)
                        .withImage(image)
                        .withImagePullPolicy("IfNotPresent")
                        .withEnv(environment)
                        .addNewPort()
                        .withContainerPort(port)
                        .endPort()
                        .build();
        if ("platform".equals(component)) configureThingWorxHealthProbes(container);
        if (claimName != null) {
            container.setVolumeMounts(
                    List.of(
                            new io.fabric8.kubernetes.api.model.VolumeMountBuilder()
                                    .withName("data")
                                    .withMountPath("/ThingworxStorage")
                                    .build()));
        }
        var spec = new PodSpecBuilder().withContainers(container);
        if (cluster.getSpec().getImagePullSecret() != null)
            spec.withImagePullSecrets(
                    List.of(
                            new io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder()
                                    .withName(cluster.getSpec().getImagePullSecret())
                                    .build()));
        if (claimName != null) {
            spec.addToVolumes(
                    new VolumeBuilder()
                            .withName("data")
                            .withNewPersistentVolumeClaim()
                            .withClaimName(name(cluster, claimName))
                            .endPersistentVolumeClaim()
                            .build());
        }
        return new PodTemplateSpecBuilder()
                .withMetadata(
                        new ObjectMetaBuilder().withLabels(labels(cluster, component)).build())
                .withSpec(spec.build())
                .build();
    }

    private static io.fabric8.kubernetes.api.model.batch.v1.Job job(
            ThingWorxCluster cluster,
            String component,
            String image,
            List<String> command,
            boolean databaseEnv) {
        var env = new ArrayList<EnvVar>();
        if (databaseEnv) env.addAll(databaseEnvironment(cluster));
        return new JobBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withName(
                                        initializationJobName(
                                                cluster, component, image, command, env))
                                .withNamespace(cluster.getMetadata().getNamespace())
                                .withLabels(labels(cluster, component))
                                .build())
                .withNewSpec()
                .withBackoffLimit(3)
                .withNewTemplate()
                .withMetadata(
                        new ObjectMetaBuilder().withLabels(labels(cluster, component)).build())
                .withNewSpec()
                .withRestartPolicy("OnFailure")
                .addNewContainer()
                .withName(component)
                .withImage(image)
                .withImagePullPolicy("IfNotPresent")
                .withCommand(command)
                .withEnv(env)
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    static String initializationJobName(ThingWorxCluster cluster, String component) {
        var image =
                "database-init".equals(component)
                        ? cluster.getSpec().getImages().getDatabaseInit()
                        : cluster.getSpec().getImages().getSecurityCli();
        var command =
                "database-init".equals(component)
                        ? List.of(
                                "/bin/sh",
                                "-ec",
                                "/usr/local/bin/db-check.sh && /usr/local/bin/db-setup.sh")
                        : List.of("/bin/sh", "-ec", "/opt/docker-entrypoint.sh");
        var env =
                "database-init".equals(component)
                        ? databaseEnvironment(cluster)
                        : List.<EnvVar>of();
        return initializationJobName(cluster, component, image, command, env);
    }

    private static String initializationJobName(
            ThingWorxCluster cluster,
            String component,
            String image,
            List<String> command,
            List<EnvVar> environment) {
        var input = new StringBuilder(component).append('\u0000').append(image);
        command.forEach(value -> input.append('\u0000').append(value));
        environment.forEach(
                value -> {
                    input.append('\u0000')
                            .append(value.getName())
                            .append('=')
                            .append(value.getValue());
                    if (value.getValueFrom() != null
                            && value.getValueFrom().getSecretKeyRef() != null) {
                        var reference = value.getValueFrom().getSecretKeyRef();
                        input.append("@secret:")
                                .append(reference.getName())
                                .append('/')
                                .append(reference.getKey());
                    }
                });
        byte[] digest;
        try {
            digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(input.toString().getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JRE", exception);
        }
        var hash = java.util.HexFormat.of().formatHex(digest, 0, 6);
        var base = name(cluster, component);
        if (base.length() > 50) base = base.substring(0, 50);
        return base + "-" + hash;
    }

    private static List<EnvVar> platformEnvironment(ThingWorxCluster cluster) {
        var result = new ArrayList<>(databaseEnvironment(cluster));
        result.add(env("ENABLE_CLUSTERED_MODE", String.valueOf(cluster.getSpec().isEnableHA())));
        if (cluster.getSpec().isEnableHA()) {
            result.add(env("COORDINATOR_HOSTS", zookeeperConnection(cluster)));
            result.add(env("IGNITE_CLIENT_MODE", "true"));
            result.add(env("IGNITE_ZK_CONNECTION", zookeeperConnection(cluster)));
        }
        addSecret(
                result,
                "THINGWORX_INITIAL_ADMIN_PASSWORD",
                cluster.getSpec().getPlatformAdminPassword());
        addSecret(result, "SECRET_PROVISIONING_APP_KEY", cluster.getSpec().getProvisioningAppKey());
        addSecret(result, "KEYSTORE_PASSWORD", cluster.getSpec().getKeystorePassword());
        if (cluster.getSpec().isEnableHA()) {
            addSecret(result, "SECRET_CX_APP_KEY", cluster.getSpec().getCxServerAppKey());
        }
        addImportPolicyEnvironment(result, cluster);
        return result;
    }

    private static void addImportPolicyEnvironment(List<EnvVar> result, ThingWorxCluster cluster) {
        var policy = cluster.getSpec().getExtensionImportPolicy();
        result.add(env("EXTPKG_IMPORT_POLICY_ENABLED", "true"));
        result.add(env("EXTPKG_IMPORT_POLICY_ALLOW_ENTITIES", String.valueOf(policy.isEntities())));
        result.add(
                env(
                        "EXTPKG_IMPORT_POLICY_ALLOW_EXTENTITIES",
                        String.valueOf(policy.isExtensibleEntities())));
        result.add(
                env("EXTPKG_IMPORT_POLICY_ALLOW_JARRES", String.valueOf(policy.isJarResources())));
        result.add(
                env(
                        "EXTPKG_IMPORT_POLICY_ALLOW_JSRES",
                        String.valueOf(policy.isJavascriptResources())));
        result.add(
                env("EXTPKG_IMPORT_POLICY_ALLOW_CSSRES", String.valueOf(policy.isCssResources())));
        result.add(
                env(
                        "EXTPKG_IMPORT_POLICY_ALLOW_JSONRES",
                        String.valueOf(policy.isJsonResources())));
        result.add(
                env(
                        "EXTPKG_IMPORT_POLICY_ALLOW_WEBAPPRES",
                        String.valueOf(policy.isWebAppResources())));
    }

    private static List<EnvVar> databaseEnvironment(ThingWorxCluster cluster) {
        var database = cluster.getSpec().getDatabase();
        var result = new ArrayList<EnvVar>();
        result.add(env("DATABASE_HOST", databaseHost(cluster)));
        result.add(env("DATABASE_PORT", String.valueOf(database.getPort())));
        result.add(env("TWX_DATABASE_DBNAME", database.getDatabase()));
        result.add(env("TWX_DATABASE_SCHEMA", database.getSchema()));
        result.add(env("TWX_DATABASE_USERNAME", database.getUsername()));
        result.add(env("DATABASE_ADMIN_USERNAME", database.getAdminUsername()));
        result.add(env("DATABASE_ADMIN_DBNAME", "postgres"));
        addSecret(result, "TWX_DATABASE_PASSWORD", database.getCredentials());
        addSecret(result, "DATABASE_ADMIN_PASSWORD", database.getAdminCredentials());
        return result;
    }

    static String databaseHost(ThingWorxCluster cluster) {
        return cluster.getSpec().getDatabase().isInternal()
                ? name(cluster, "postgres")
                : cluster.getSpec().getDatabase().getHost();
    }

    private static void addSecret(List<EnvVar> values, String env, SecretKeyReference reference) {
        if (reference != null) values.add(secretEnv(env, reference.getName(), reference.getKey()));
    }

    private static EnvVar env(String name, String value) {
        return new EnvVarBuilder().withName(name).withValue(value).build();
    }

    private static EnvVar secretEnv(String env, String secret, String key) {
        return new EnvVarBuilder()
                .withName(env)
                .withNewValueFrom()
                .withNewSecretKeyRef(key, secret, false)
                .endValueFrom()
                .build();
    }

    private static io.fabric8.kubernetes.api.model.networking.v1.Ingress ingress(
            ThingWorxCluster cluster) {
        var ingress = cluster.getSpec().getIngress();
        var metadata =
                new ObjectMetaBuilder()
                        .withName(name(cluster, "ingress"))
                        .withNamespace(cluster.getMetadata().getNamespace())
                        .withLabels(labels(cluster, "ingress"));
        if (ingress.getCertManagerIssuer() != null)
            metadata.addToAnnotations(
                    "cert-manager.io/cluster-issuer", ingress.getCertManagerIssuer());
        var builder =
                new IngressBuilder()
                        .withMetadata(metadata.build())
                        .withNewSpec()
                        .withIngressClassName(ingress.getClassName())
                        .addNewRule()
                        .withHost(ingress.getHost())
                        .withNewHttp()
                        .addNewPath()
                        .withPath("/")
                        .withPathType("Prefix")
                        .withNewBackend()
                        .withNewService()
                        .withName(
                                name(
                                        cluster,
                                        cluster.getSpec().isEnableHA() ? "haproxy" : "platform"))
                        .withNewPort()
                        .withNumber(8080)
                        .endPort()
                        .endService()
                        .endBackend()
                        .endPath()
                        .endHttp()
                        .endRule()
                        .endSpec();
        if (ingress.getTlsSecretName() != null || ingress.getCertManagerIssuer() != null) {
            var tlsSecret =
                    ingress.getTlsSecretName() == null
                            ? name(cluster, "tls")
                            : ingress.getTlsSecretName();
            builder.editOrNewSpec()
                    .withTls(
                            List.of(
                                    new io.fabric8.kubernetes.api.model.networking.v1
                                                    .IngressTLSBuilder()
                                            .withHosts(ingress.getHost())
                                            .withSecretName(tlsSecret)
                                            .build()))
                    .endSpec();
        }
        return builder.build();
    }

    private static String haproxyConfig(ThingWorxCluster cluster) {
        return "global\n  daemon\ndefaults\n  mode http\n  timeout connect 5s\n  timeout client 1h\n  timeout server 1h\n"
                + "frontend public\n  bind *:8080\n"
                + "  acl connection_server path_beg -i /Thingworx/WS /Thingworx/WSS\n"
                + "  use_backend cxserver if connection_server\n  default_backend platform\n"
                + "backend platform\n  balance roundrobin\n  cookie TWXNODE insert indirect nocache\n"
                + "  option httpchk GET /Thingworx/health\n  server platform "
                + name(cluster, "platform")
                + ":8080 check cookie platform\n"
                + "backend cxserver\n  balance roundrobin\n  cookie CXNODE insert indirect nocache\n  server cxserver "
                + name(cluster, "cxserver")
                + ":8080 check cookie cxserver\n";
    }

    /**
     * ThingWorx only receives traffic after its vendor-supported health endpoint succeeds. The
     * startup probe gives a cold platform (including post-extension restart) up to ten minutes to
     * initialize.
     */
    private static void configureThingWorxHealthProbes(
            io.fabric8.kubernetes.api.model.Container container) {
        container.setStartupProbe(httpProbe(15, 5, 40, 0));
        container.setReadinessProbe(httpProbe(15, 5, 6, 120));
    }

    private static io.fabric8.kubernetes.api.model.Probe httpProbe(
            int periodSeconds, int timeoutSeconds, int failureThreshold, int initialDelaySeconds) {
        return new ProbeBuilder()
                .withNewHttpGet()
                .withPath("/Thingworx/health")
                .withPort(new IntOrString(8080))
                .withScheme("HTTP")
                .endHttpGet()
                .withPeriodSeconds(periodSeconds)
                .withTimeoutSeconds(timeoutSeconds)
                .withFailureThreshold(failureThreshold)
                .withInitialDelaySeconds(initialDelaySeconds)
                .build();
    }

    private static io.fabric8.kubernetes.api.model.Probe tcpProbe(
            int port, int periodSeconds, int failureThreshold) {
        return new ProbeBuilder()
                .withNewTcpSocket()
                .withPort(new IntOrString(port))
                .endTcpSocket()
                .withPeriodSeconds(periodSeconds)
                .withTimeoutSeconds(5)
                .withFailureThreshold(failureThreshold)
                .build();
    }

    private static String zookeeperConnection(ThingWorxCluster cluster) {
        return java.util.stream.IntStream.range(0, 3)
                .mapToObj(
                        index ->
                                name(cluster, "zookeeper")
                                        + "-"
                                        + index
                                        + "."
                                        + name(cluster, "zookeeper")
                                        + ":2181")
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static String zookeeperServers(ThingWorxCluster cluster) {
        return java.util.stream.IntStream.range(0, 3)
                .mapToObj(
                        index ->
                                "server."
                                        + (index + 1)
                                        + "="
                                        + name(cluster, "zookeeper")
                                        + "-"
                                        + index
                                        + "."
                                        + name(cluster, "zookeeper")
                                        + ":2888:3888;2181")
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private static void addVolume(
            io.fabric8.kubernetes.api.model.PodSpec podSpec,
            io.fabric8.kubernetes.api.model.Volume volume) {
        var volumes = new ArrayList<io.fabric8.kubernetes.api.model.Volume>();
        if (podSpec.getVolumes() != null) volumes.addAll(podSpec.getVolumes());
        volumes.add(volume);
        podSpec.setVolumes(volumes);
    }

    private static void addMount(
            io.fabric8.kubernetes.api.model.Container container,
            io.fabric8.kubernetes.api.model.VolumeMount mount) {
        var mounts = new ArrayList<io.fabric8.kubernetes.api.model.VolumeMount>();
        if (container.getVolumeMounts() != null) mounts.addAll(container.getVolumeMounts());
        mounts.add(mount);
        container.setVolumeMounts(mounts);
    }

    private static void addPort(
            io.fabric8.kubernetes.api.model.Container container,
            io.fabric8.kubernetes.api.model.ContainerPort port) {
        var ports = new ArrayList<io.fabric8.kubernetes.api.model.ContainerPort>();
        if (container.getPorts() != null) ports.addAll(container.getPorts());
        ports.add(port);
        container.setPorts(ports);
    }
}
