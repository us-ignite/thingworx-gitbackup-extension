package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConnectionStatusTest {
    @TempDir Path tempDir;

    private static ConnectionStatus.Inputs inputs(
            List<URIish> uris,
            String username,
            boolean hasPassword,
            String approvedOrigins,
            String sshKey,
            boolean sshKeyAvailable,
            List<URIish> approvedHostKeys,
            List<String> networkProblems,
            boolean allowInsecure) {
        return new ConnectionStatus.Inputs(
                "origin",
                uris,
                true,
                username,
                hasPassword,
                approvedOrigins,
                sshKey,
                sshKeyAvailable,
                approvedHostKeys,
                networkProblems,
                allowInsecure);
    }

    @Test
    void httpsRemoteNeedsSignInThenAnApprovedOrigin() throws Exception {
        URIish https = new URIish("https://git.example.com/team/repo.git");
        var none =
                ConnectionStatus.evaluate(
                        inputs(
                                List.of(https),
                                "",
                                false,
                                "",
                                "",
                                false,
                                List.of(),
                                List.of(),
                                false));
        assertEquals(ConnectionStatus.MISSING, none.signIn());
        assertEquals(ConnectionStatus.NOT_NEEDED, none.hostKey());
        assertFalse(none.ready());
        assertTrue(none.nextStep().contains("password or access token"), none.nextStep());

        var unapproved =
                ConnectionStatus.evaluate(
                        inputs(
                                List.of(https),
                                "me",
                                true,
                                "https://other.example.com:443/x",
                                "",
                                false,
                                List.of(),
                                List.of(),
                                false));
        assertEquals(ConnectionStatus.OK, unapproved.signIn());
        assertEquals(ConnectionStatus.MISSING, unapproved.credentialOrigin());
        assertTrue(unapproved.nextStep().contains("save again"), unapproved.nextStep());

        var ready =
                ConnectionStatus.evaluate(
                        inputs(
                                List.of(https),
                                "me",
                                true,
                                Remotes.credentialScope(https),
                                "",
                                false,
                                List.of(),
                                List.of(),
                                false));
        assertTrue(ready.ready());
        assertEquals("https", ready.transport());
    }

    @Test
    void sshRemoteNeedsAKeyThenAnApprovedHostKey() throws Exception {
        URIish ssh = new URIish("ssh://git@git.example.com/team/repo.git");
        var noKey =
                ConnectionStatus.evaluate(
                        inputs(
                                List.of(ssh),
                                "",
                                false,
                                "",
                                "",
                                false,
                                List.of(),
                                List.of(),
                                false));
        assertEquals(ConnectionStatus.MISSING, noKey.signIn());
        assertEquals(ConnectionStatus.NOT_NEEDED, noKey.credentialOrigin());
        assertTrue(noKey.nextStep().contains("choose its SSH key"), noKey.nextStep());

        var deletedKey =
                ConnectionStatus.evaluate(
                        inputs(
                                List.of(ssh),
                                "",
                                false,
                                "",
                                "SHA256:gone",
                                false,
                                List.of(),
                                List.of(),
                                false));
        assertTrue(deletedKey.nextStep().contains("no longer exists"), deletedKey.nextStep());

        var noHostKey =
                ConnectionStatus.evaluate(
                        inputs(
                                List.of(ssh),
                                "",
                                false,
                                "",
                                "SHA256:key",
                                true,
                                List.of(),
                                List.of(),
                                false));
        assertEquals(ConnectionStatus.OK, noHostKey.signIn());
        assertEquals(ConnectionStatus.MISSING, noHostKey.hostKey());
        assertTrue(noHostKey.nextStep().contains("host key"), noHostKey.nextStep());

        var ready =
                ConnectionStatus.evaluate(
                        inputs(
                                List.of(ssh),
                                "",
                                false,
                                "",
                                "SHA256:key",
                                true,
                                List.of(ssh),
                                List.of(),
                                false));
        assertTrue(ready.ready());
    }

    @Test
    void plainHttpCannotSignInUnlessTheTestOverrideIsSet() throws Exception {
        URIish http = new URIish("http://gitea:3000/team/repo.git");
        var refused =
                ConnectionStatus.evaluate(
                        inputs(
                                List.of(http),
                                "me",
                                true,
                                Remotes.credentialScope(http),
                                "",
                                false,
                                List.of(),
                                List.of(),
                                false));
        assertFalse(refused.ready());
        assertTrue(refused.nextStep().contains("never sent over HTTP"), refused.nextStep());

        var allowed =
                ConnectionStatus.evaluate(
                        inputs(
                                List.of(http),
                                "me",
                                true,
                                Remotes.credentialScope(http),
                                "",
                                false,
                                List.of(),
                                List.of(),
                                true));
        assertTrue(allowed.ready());
    }

    @Test
    void blockedNetworkAddressIsReportedFirst() throws Exception {
        URIish ssh = new URIish("ssh://git@10.0.0.5/team/repo.git");
        var blocked =
                ConnectionStatus.evaluate(
                        inputs(
                                List.of(ssh),
                                "",
                                false,
                                "",
                                "",
                                false,
                                List.of(),
                                List.of("Remote host 10.0.0.5 resolves to a non-public address."),
                                false));
        assertEquals(ConnectionStatus.MISSING, blocked.network());
        assertTrue(
                blocked.nextStep().contains("THINGWORX_JGIT_NETWORK_ALLOWLIST"),
                blocked.nextStep());
    }

    @Test
    void aSeparateSshPushUrlNeedsItsOwnHostKey() throws Exception {
        URIish https = new URIish("https://git.example.com/team/repo.git");
        URIish push = new URIish("ssh://git@push.example.com/team/repo.git");
        var status =
                ConnectionStatus.evaluate(
                        inputs(
                                List.of(https, push),
                                "me",
                                true,
                                Remotes.credentialScope(https),
                                "SHA256:key",
                                true,
                                List.of(),
                                List.of(),
                                false));
        assertEquals("https, ssh", status.transport());
        assertEquals(ConnectionStatus.OK, status.credentialOrigin());
        assertEquals(ConnectionStatus.MISSING, status.hostKey());
        assertFalse(status.ready());
    }

    @Test
    void approvedHostKeyIsFoundForTheRemoteHostAndPort() throws Exception {
        try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
            URIish defaultPort = new URIish("git@git.example.com:team/repo.git");
            URIish otherPort = new URIish("ssh://git@git.example.com:2222/team/repo.git");
            assertFalse(SshHostKeys.isApproved(git.getRepository(), defaultPort));
            SshHostKeys.record(
                    git.getRepository(),
                    new SshHostKeys.HostKey(
                            "git.example.com", 22, "SHA256:x", "ssh-ed25519 AAAA x"));
            assertTrue(SshHostKeys.isApproved(git.getRepository(), defaultPort));
            assertFalse(
                    SshHostKeys.isApproved(git.getRepository(), otherPort),
                    "A key approved for port 22 must not count for another port");
        }
    }

    @Test
    void clearingThePushUrlLeavesTheFetchUrl() throws Exception {
        try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
            Remotes.add(git, "origin", "https://git.example.com/team/repo.git");
            assertFalse(Remotes.clearPushUrls(git, "origin"));
            git.remoteSetUrl()
                    .setRemoteName("origin")
                    .setRemoteUri(new URIish("ssh://git@git.example.com/team/repo.git"))
                    .setUriType(org.eclipse.jgit.api.RemoteSetUrlCommand.UriType.PUSH)
                    .call();
            assertEquals(1, Remotes.list(git).get(0).getPushURIs().size());
            assertTrue(Remotes.clearPushUrls(git, "origin"));
            var remote = Remotes.list(git).get(0);
            assertTrue(remote.getPushURIs().isEmpty());
            assertEquals(
                    "https://git.example.com/team/repo.git", remote.getURIs().get(0).toString());
        }
    }
}
