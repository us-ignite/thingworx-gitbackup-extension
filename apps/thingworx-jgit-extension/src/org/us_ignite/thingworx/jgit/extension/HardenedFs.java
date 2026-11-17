package org.us_ignite.thingworx.jgit.extension;

import java.io.File;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FS_POSIX;
import org.eclipse.jgit.util.FS_Win32;

/**
 * File-system access for repositories that never starts an external program.
 *
 * <p>JGit runs shell commands named in {@code .git/config}: hooks (including {@code
 * core.hooksPath}), {@code filter.*.clean} and {@code filter.*.smudge} commands, and the
 * upload-pack and receive-pack commands of local remotes. A repository's {@code .git} folder sits
 * inside a ThingWorx FileRepository, where file services can write to it, so honoring those
 * settings would let anyone who can write a file run commands as the ThingWorx server. Every
 * repository is opened with this file system, which refuses them.
 */
final class HardenedFs {
    private static final String REFUSED =
            "Running external commands from Git configuration is disabled in this extension.";

    static FS create() {
        FS detected = FS.DETECTED;
        if (detected instanceof FS_POSIX) return new Posix(detected);
        if (detected instanceof FS_Win32) return new Windows(detected);
        throw new IllegalStateException("Unsupported file system: " + detected.getClass());
    }

    private static final class Posix extends FS_POSIX {
        Posix(FS source) {
            super(source);
        }

        @Override
        public FS newInstance() {
            return new Posix(this);
        }

        @Override
        public File findHook(Repository repository, String hookName) {
            return null;
        }

        @Override
        public ProcessBuilder runInShell(String cmd, String[] args) {
            throw new UnsupportedOperationException(REFUSED);
        }
    }

    private static final class Windows extends FS_Win32 {
        Windows(FS source) {
            super(source);
        }

        @Override
        public FS newInstance() {
            return new Windows(this);
        }

        @Override
        public File findHook(Repository repository, String hookName) {
            return null;
        }

        @Override
        public ProcessBuilder runInShell(String cmd, String[] args) {
            throw new UnsupportedOperationException(REFUSED);
        }
    }

    private HardenedFs() {}
}
