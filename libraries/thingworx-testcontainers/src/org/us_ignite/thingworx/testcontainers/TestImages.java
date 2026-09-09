package org.us_ignite.thingworx.testcontainers;

/** Image names used by the local Testcontainers integration stack. */
public final class TestImages {
    private static final String DEFAULT_DB_INIT_IMAGE =
            "ghcr.io/us-ignite/thingworx/postgres-init:10.1.2";
    private static final String DEFAULT_PLATFORM_IMAGE =
            "ghcr.io/us-ignite/thingworx/platform-postgres:10.1.2-openjdk";
    private static final String DEFAULT_POSTGRES_IMAGE = "postgres:16";
    private static final String DEFAULT_SECURITY_IMAGE =
            "ghcr.io/us-ignite/thingworx/security-tool:10.1.2";

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

    public static String securityImage() {
        return System.getProperty("test.securityImage", DEFAULT_SECURITY_IMAGE);
    }

    public static String securityImageForPlatform(String platformImage) {
        if (platformImage == null) return securityImage();
        try {
            String tag = platformImage.substring(platformImage.lastIndexOf(':') + 1);
            String version = tag.split("-")[0];
            String securityVersion;
            if (version.startsWith("9.6")) securityVersion = "9.6.9";
            else if (version.startsWith("9.7")) securityVersion = "9.7.9";
            else if (version.startsWith("10.0")) securityVersion = "10.0.9";
            else if (version.startsWith("10.1")) securityVersion = "10.1.2";
            else securityVersion = version;
            return "ghcr.io/us-ignite/thingworx/security-tool:" + securityVersion;
        } catch (Exception e) {
            return securityImage();
        }
    }
}
