package org.us_ignite.thingworx.jgit.tests.containers;

/** Image names used by the local Testcontainers integration stack. */
public final class TestImages {
    private static final String DEFAULT_DB_INIT_IMAGE =
            "ghcr.io/us-ignite/thingworx/postgres-init:10.1.2";
    private static final String DEFAULT_PLATFORM_IMAGE =
            "ghcr.io/us-ignite/thingworx/platform-postgres:10.1.2-openjdk";
    private static final String DEFAULT_POSTGRES_IMAGE = "postgres:16";

    private TestImages() {}

    public static String dbInitImage() {
        return System.getProperty("test.dbInitImage", DEFAULT_DB_INIT_IMAGE);
    }

    public static String platformImage() {
        return System.getProperty("test.platformImage", DEFAULT_PLATFORM_IMAGE);
    }

    public static String postgresImage() {
        return System.getProperty("test.postgresImage", DEFAULT_POSTGRES_IMAGE);
    }
}
