package org.us_ignite.thingworx.jgit.test.util;

import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Turns off JGit's background auto-GC for every repository the test JVM opens.
 *
 * <p>Merge, rebase, fetch and receive-pack end with {@code Repository.autoGC}. With the default
 * {@code gc.autoDetach=true}, JGit takes {@code .git/gc.log.lock} before it checks {@code gc.auto},
 * then releases it from a worker thread. A test that ends straight after a merge can reach
 * {@code @TempDir} cleanup while that lock file still exists, and the cleanup then fails. Setting
 * {@code gc.auto=0} on the repository does not help, because the lock comes first.
 *
 * <p>With {@code gc.autoDetach=false} the auto-GC runs inline and, with {@code gc.auto=0}, returns
 * before taking any lock. The values go into the JGit-level config, the root of every repository's
 * config chain, so they also cover clones and bare remotes that a test cannot configure before
 * their first fetch. Git CLI subprocesses and the developer's own config files are not touched.
 */
public final class ForegroundGitGc implements LauncherSessionListener {
    @Override
    public void launcherSessionOpened(LauncherSession session) {
        SystemReader.setInstance(
                new SystemReader.Delegate(SystemReader.getInstance()) {
                    @Override
                    public FileBasedConfig openJGitConfig(Config parent, FS fs) {
                        FileBasedConfig base = super.openJGitConfig(parent, fs);
                        return new FileBasedConfig(parent, base.getFile(), fs) {
                            @Override
                            public void load() throws IOException, ConfigInvalidException {
                                super.load();
                                setBoolean("gc", null, "autoDetach", false);
                                setInt("gc", null, "auto", 0);
                            }
                        };
                    }
                });
    }
}
