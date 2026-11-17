package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.Test;

class NetworkPolicyTest {
    @Test
    void blocksLoopbackPrivateLinkLocalAndMetadataAddresses() {
        for (String url :
                new String[] {
                    "http://127.0.0.1/repo.git",
                    "http://10.0.0.1/repo.git",
                    "http://172.16.0.1/repo.git",
                    "http://192.168.1.1/repo.git",
                    "http://169.254.169.254/latest/meta-data",
                    "http://100.64.0.1/repo.git",
                    "http://0.0.0.0/repo.git",
                    "http://[::1]/repo.git"
                }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> NetworkPolicy.requireAllowed(new URIish(url), ""),
                    url);
        }
    }

    @Test
    void allowsPublicAddressesAndExplicitlyApprovedPrivateHosts() throws Exception {
        assertDoesNotThrow(() -> NetworkPolicy.requireAllowed(new URIish("https://8.8.8.8/r"), ""));
        assertDoesNotThrow(
                () -> NetworkPolicy.requireAllowed(new URIish("http://127.0.0.1/r"), "127.0.0.1"));
        assertDoesNotThrow(
                () -> NetworkPolicy.requireAllowed(new URIish("ssh://gitea/r"), "gitea"));
        assertTrue(NetworkPolicy.isAllowlisted("git.internal", " git.internal , other "));
        assertTrue(NetworkPolicy.isAllowlisted("git.internal", "*.internal"));
    }

    @Test
    void timeoutIsBounded() {
        assertEquals(NetworkPolicy.DEFAULT_TIMEOUT_SECONDS, NetworkPolicy.parseTimeout(""));
        assertEquals(NetworkPolicy.DEFAULT_TIMEOUT_SECONDS, NetworkPolicy.parseTimeout("nope"));
        assertEquals(NetworkPolicy.DEFAULT_TIMEOUT_SECONDS, NetworkPolicy.parseTimeout("0"));
        assertEquals(15, NetworkPolicy.parseTimeout("15"));
        assertEquals(NetworkPolicy.MAX_TIMEOUT_SECONDS, NetworkPolicy.parseTimeout("999999"));
        assertEquals(true, NetworkPolicy.allowInsecureCredentials("true"));
        assertEquals(false, NetworkPolicy.allowInsecureCredentials("false"));
    }
}
