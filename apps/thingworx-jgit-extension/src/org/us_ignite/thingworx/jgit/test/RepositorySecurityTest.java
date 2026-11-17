package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.transport.HttpConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositorySecurityTest {
    @TempDir Path temp;

    @Test
    void hardenOverridesUnsafeSystemHttpSettings() throws Exception {
        Config base = new Config();
        base.setBoolean("http", null, "sslVerify", false);
        base.setString("http", null, "cookieFile", "/tmp/cookies.txt");
        base.setString("http", null, "followRedirects", "true");
        base.setStringList("http", null, "extraHeader", List.of("Authorization: Basic c2VjcmV0"));
        base.setBoolean("http", "https://git.example.com", "sslVerify", false);
        base.setString("http", "https://git.example.com", "cookieFile", "/tmp/cookies.txt");
        base.setString("http", "https://git.example.com", "followRedirects", "true");
        base.setStringList(
                "http", "https://git.example.com", "extraHeader", List.of("X-Secret: yes"));

        FileBasedConfig local =
                new FileBasedConfig(base, temp.resolve("config").toFile(), FS.DETECTED);
        RepositorySecurity.harden(local);

        HttpConfig http =
                new HttpConfig(local, new URIish("https://git.example.com/team/repo.git"));
        assertTrue(http.isSslVerify());
        assertTrue(http.getCookieFile() == null || http.getCookieFile().isBlank());
        assertTrue(http.getExtraHeaders().isEmpty());
        assertEquals(HttpConfig.HttpRedirectMode.FALSE, http.getFollowRedirects());
        assertFalse(local.getBoolean("core", null, "symlinks", true));
        assertFalse(local.getBoolean("commit", null, "gpgSign", true));
    }

    @Test
    void validateRejectsUrlRewritingAndUnsafeRefspecs() throws Exception {
        StoredConfig rewritten = new FileBasedConfig(temp.resolve("rewrite").toFile(), FS.DETECTED);
        rewritten.setString(
                "url", "https://attacker.example", "insteadOf", "https://git.example.com");
        assertThrows(IOException.class, () -> RepositorySecurity.validate(rewritten));

        StoredConfig refspec = new FileBasedConfig(temp.resolve("refspec").toFile(), FS.DETECTED);
        refspec.setString("remote", "origin", "url", "https://git.example.com/team/repo.git");
        refspec.setString("remote", "origin", "fetch", "+refs/heads/*:refs/heads/*");
        assertThrows(IOException.class, () -> RepositorySecurity.validate(refspec));

        StoredConfig helper = new FileBasedConfig(temp.resolve("helper").toFile(), FS.DETECTED);
        helper.setString("remote", "origin", "url", "https://git.example.com/team/repo.git");
        helper.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        helper.setString("remote", "origin", "uploadpack", "/bin/sh -c evil");
        assertThrows(IOException.class, () -> RepositorySecurity.validate(helper));
    }
}
