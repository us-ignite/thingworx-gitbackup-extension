package org.us_ignite.thingworx.operator.api;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

/** Desired and observed state for a ThingWorx HA installation. */
@Group("thingworx.us-ignite.org")
@Version("v1alpha1")
@Kind("ThingWorxCluster")
@Plural("thingworxclusters")
@ShortNames("twx")
public class ThingWorxCluster extends CustomResource<ThingWorxClusterSpec, ThingWorxClusterStatus>
        implements Namespaced {
    private static final long serialVersionUID = 1L;
}
