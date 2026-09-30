package org.us_ignite.thingworx.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.us_ignite.thingworx.testcontainers.ThingWorxContainer;

/** Unit tests for license file resolution (no containers started). */
class LicenseResolutionTest {

    @TempDir Path tempDir;

    @Test
    void explicitPropertyResolves() throws Exception {
        Path license = Files.write(tempDir.resolve("license.bin"), new byte[] {1, 2, 3});
        String previous = System.getProperty("test.licenseFile");
        System.setProperty("test.licenseFile", license.toString());
        try {
            assertEquals(license.toAbsolutePath(), ThingWorxContainer.resolveLicenseFile());
        } finally {
            restoreProperty(previous);
        }
    }

    @Test
    void explicitMissingPropertyThrows() {
        String previous = System.getProperty("test.licenseFile");
        System.setProperty("test.licenseFile", tempDir.resolve("absent.bin").toString());
        try {
            assertThrows(
                    IllegalStateException.class, ThingWorxContainer::resolveLicenseFile);
        } finally {
            restoreProperty(previous);
        }
    }

    @Test
    void base64LicenseIsReadableByContainerUser() throws Exception {
        String encoded = System.getenv("TWX_LICENSE_B64");
        assumeTrue(encoded != null && !encoded.isBlank());
        String previousProp = System.getProperty("test.licenseFile");
        String previousDir = System.getProperty("user.dir");
        System.clearProperty("test.licenseFile");
        System.setProperty("user.dir", tempDir.toString());
        try {
            Path license = ThingWorxContainer.resolveLicenseFile();
            assertNotNull(license);
            assertTrue(Files.isReadable(license));
            assertTrue(
                    Files.getPosixFilePermissions(license).contains(PosixFilePermission.OTHERS_READ),
                    "Docker's ThingWorx user must be able to read the bind-mounted license");
        } finally {
            restoreProperty(previousProp);
            System.setProperty("user.dir", previousDir);
        }
    }

    @Test
    void returnsNullWhenNothingAvailable() {
        assumeTrue(
                System.getenv("TWX_LICENSE_B64") == null,
                "TWX_LICENSE_B64 is set; null-branch not applicable");
        String previousProp = System.getProperty("test.licenseFile");
        String previousDir = System.getProperty("user.dir");
        System.clearProperty("test.licenseFile");
        System.setProperty("user.dir", tempDir.toString());
        try {
            assertNull(ThingWorxContainer.resolveLicenseFile());
        } finally {
            restoreProperty(previousProp);
            System.setProperty("user.dir", previousDir);
        }
    }

    private static void restoreProperty(String previous) {
        if (previous == null) {
            System.clearProperty("test.licenseFile");
        } else {
            System.setProperty("test.licenseFile", previous);
        }
    }
}
