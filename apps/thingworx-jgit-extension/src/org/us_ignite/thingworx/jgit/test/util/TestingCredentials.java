package org.us_ignite.thingworx.jgit.test.util;

import java.util.HashMap;

/**
 * @deprecated use the shared thingworx-testcontainers credentials type.
 */
@Deprecated
public class TestingCredentials extends org.us_ignite.thingworx.testcontainers.TestingCredentials {
    public TestingCredentials() {
        super();
    }

    public TestingCredentials(HashMap<String, String> overrides) {
        super(overrides);
    }
}
