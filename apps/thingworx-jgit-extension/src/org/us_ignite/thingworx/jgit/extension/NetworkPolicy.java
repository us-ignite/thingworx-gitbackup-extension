package org.us_ignite.thingworx.jgit.extension;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import org.eclipse.jgit.transport.URIish;

/**
 * Decides which hosts a repository may contact.
 *
 * <p>Remote URLs are user-controlled and services run inside the ThingWorx server, so an
 * unrestricted URL is a server-side request forgery primitive against the container network and
 * cloud metadata endpoints. Loopback, link-local, private and other non-global addresses are
 * refused unless the host name is explicitly approved by the operator.
 */
final class NetworkPolicy {
    static final String ALLOWLIST_ENVIRONMENT = "THINGWORX_JGIT_NETWORK_ALLOWLIST";
    static final String ALLOWLIST_PROPERTY = "thingworx.jgit.network.allowlist";
    static final String INSECURE_CREDENTIALS_ENVIRONMENT =
            "THINGWORX_JGIT_ALLOW_INSECURE_CREDENTIALS";
    static final String INSECURE_CREDENTIALS_PROPERTY = "thingworx.jgit.allowInsecureCredentials";
    static final String TIMEOUT_PROPERTY = "thingworx.jgit.networkTimeoutSeconds";
    static final int DEFAULT_TIMEOUT_SECONDS = 60;
    static final int MAX_TIMEOUT_SECONDS = 3600;

    static void requireAllowed(URIish uri) {
        requireAllowed(uri, configuredAllowlist());
    }

    static void requireAllowed(URIish uri, String allowlist) {
        String host = uri == null ? null : uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("A remote host is required.");
        }
        if (isAllowlisted(host, allowlist)) return;

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Could not resolve remote host '" + host + "'.");
        }
        for (InetAddress address : addresses) {
            if (!isGlobal(address)) {
                throw new IllegalArgumentException(
                        "Remote host '"
                                + host
                                + "' resolves to a non-public address; add it to "
                                + ALLOWLIST_ENVIRONMENT
                                + " if it is an approved internal Git server.");
            }
        }
    }

    /** True when the destination is a normal public unicast address. */
    static boolean isGlobal(InetAddress address) {
        if (address == null
                || address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet6Address) {
            if ((bytes[0] & 0xfe) == 0xfc) return false; // unique local fc00::/7
            if (isIpv4Mapped(bytes)) return isGlobalIpv4(bytes, 12);
            return true;
        }
        return isGlobalIpv4(bytes, 0);
    }

    private static boolean isIpv4Mapped(byte[] bytes) {
        if (bytes.length != 16) return false;
        for (int i = 0; i < 10; i++) if (bytes[i] != 0) return false;
        return (bytes[10] & 0xff) == 0xff && (bytes[11] & 0xff) == 0xff;
    }

    private static boolean isGlobalIpv4(byte[] bytes, int offset) {
        int a = bytes[offset] & 0xff;
        int b = bytes[offset + 1] & 0xff;
        int c = bytes[offset + 2] & 0xff;
        int d = bytes[offset + 3] & 0xff;
        if (a == 0 || a == 10 || a == 127) return false;
        if (a == 100 && b >= 64 && b <= 127) return false; // 100.64.0.0/10
        if (a == 169 && b == 254) return false; // link-local and cloud metadata
        if (a == 172 && b >= 16 && b <= 31) return false;
        if (a == 192 && b == 168) return false;
        if (a == 192 && b == 0 && c == 0) return false;
        if (a == 192 && b == 0 && c == 2) return false;
        if (a == 198 && (b == 18 || b == 19)) return false;
        if (a == 198 && b == 51 && c == 100) return false;
        if (a == 203 && b == 0 && c == 113) return false;
        if (a >= 224) return false; // multicast, reserved and broadcast
        return d >= 0;
    }

    static boolean isAllowlisted(String host, String allowlist) {
        if (host == null || allowlist == null || allowlist.isBlank()) return false;
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        for (String entry : allowlist.split(",")) {
            String normalized = entry.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) continue;
            if (normalized.startsWith("*.")) {
                String suffix = normalized.substring(1);
                if (normalizedHost.endsWith(suffix) && normalizedHost.length() > suffix.length()) {
                    return true;
                }
            } else if (normalized.equals(normalizedHost)) {
                return true;
            }
        }
        return false;
    }

    static boolean allowInsecureCredentials() {
        return allowInsecureCredentials(
                firstNonBlank(
                        System.getenv(INSECURE_CREDENTIALS_ENVIRONMENT),
                        System.getProperty(INSECURE_CREDENTIALS_PROPERTY)));
    }

    static boolean allowInsecureCredentials(String configured) {
        return configured != null && Boolean.parseBoolean(configured.trim());
    }

    static int networkTimeoutSeconds() {
        return parseTimeout(
                firstNonBlank(
                        System.getProperty(TIMEOUT_PROPERTY),
                        System.getenv("THINGWORX_JGIT_NETWORK_TIMEOUT_SECONDS")));
    }

    static int parseTimeout(String value) {
        if (value == null || value.isBlank()) return DEFAULT_TIMEOUT_SECONDS;
        try {
            int seconds = Integer.parseInt(value.trim());
            if (seconds < 1) return DEFAULT_TIMEOUT_SECONDS;
            return Math.min(seconds, MAX_TIMEOUT_SECONDS);
        } catch (NumberFormatException e) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
    }

    static String configuredAllowlist() {
        return firstNonBlank(
                System.getenv(ALLOWLIST_ENVIRONMENT), System.getProperty(ALLOWLIST_PROPERTY));
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        return second == null ? "" : second;
    }

    private NetworkPolicy() {}
}
