package thingworx

import org.gradle.api.GradleException

class VersionUtils {

    static String nextVersion(String current, String part) {
        def m = (current =~ /^([0-9]+)\.([0-9]+)\.([0-9]+)$/)
        if (!m.matches()) {
            throw new GradleException("version '$current' must be numeric SemVer (X.Y.Z)")
        }
        int major = m.group(1) as int
        int minor = m.group(2) as int
        int patch = m.group(3) as int
        switch (part) {
            case 'major': return "${major + 1}.0.0"
            case 'minor': return "${major}.${minor + 1}.0"
            case 'patch': return "${major}.${minor}.${patch + 1}"
            default: throw new GradleException("unknown bump $part")
        }
    }

    static String bootstrapVersion(String target) {
        switch (target) {
            case 'apps/thingworx-jgit-extension/.version':
            case 'extensions/jgit/.version':
                return '6.0.6'
            case 'libraries/thingworx-dap/.version':
            case 'libraries/thingworx-dap-runtime/.version':
                return '0.1.0'
            case 'apps/thingworx-operator/.version':
            case 'charts/thingworx-operator/.version':
                return '0.1.0'
            default:
                throw new GradleException("no bootstrap version is defined for $target")
        }
    }

    static String execGit(File dir, String... args) {
        def proc = new ProcessBuilder(['git'] + args.toList())
                .directory(dir)
                .redirectErrorStream(false)
                .start()
        def out = new StringBuilder()
        def err = new StringBuilder()
        proc.consumeProcessOutput(out, err)
        proc.waitFor()
        if (proc.exitValue() != 0) {
            return null
        }
        return out.toString().trim()
    }

    static boolean gitCatFileExists(File dir, String refPath) {
        def proc = new ProcessBuilder(['git', 'cat-file', '-e', refPath])
                .directory(dir)
                .redirectErrorStream(true)
                .start()
        proc.waitFor()
        return proc.exitValue() == 0
    }

    static String gitShow(File dir, String refPath) {
        def proc = new ProcessBuilder(['git', 'show', refPath])
                .directory(dir)
                .start()
        def out = new ByteArrayOutputStream()
        def err = new ByteArrayOutputStream()
        proc.consumeProcessOutput(out, err)
        proc.waitFor()
        if (proc.exitValue() != 0) {
            return null
        }
        return out.toString().trim().replaceAll(/\s+/, '')
    }

    static String gitShowRaw(File dir, String refPath) {
        def proc = new ProcessBuilder(['git', 'show', refPath])
                .directory(dir)
                .start()
        def out = new ByteArrayOutputStream()
        proc.consumeProcessOutput(out, new ByteArrayOutputStream())
        proc.waitFor()
        if (proc.exitValue() != 0) return null
        return out.toString()
    }

    static List<String> stagedPaths(File dir) {
        def proc = new ProcessBuilder(['git', 'diff', '--cached', '--name-only', '-z'])
                .directory(dir)
                .start()
        def out = new ByteArrayOutputStream()
        proc.consumeProcessOutput(out, new ByteArrayOutputStream())
        proc.waitFor()
        def raw = out.toByteArray()
        if (raw.length == 0) return []
        // split by zero byte
        def list = []
        def cur = new ByteArrayOutputStream()
        for (b in raw) {
            if (b == 0) {
                if (cur.size() > 0) {
                    list << cur.toString('UTF-8')
                    cur.reset()
                }
            } else {
                cur.write(b)
            }
        }
        if (cur.size() > 0) list << cur.toString('UTF-8')
        return list
    }

    static String resolveJgitVersionAt(File dir, String ref) {
        def cands = [
                "${ref}:apps/thingworx-jgit-extension/.version",
                "${ref}:extensions/jgit/.version",
                "${ref}:.version"
        ]
        for (c in cands) {
            if (gitCatFileExists(dir, c)) {
                return gitShow(dir, c)
            }
        }
        return null
    }

    static boolean hasJgitVersionAt(File dir, String ref) {
        return resolveJgitVersionAt(dir, ref) != null
    }

    static String resolveOperatorVersionAt(File dir, String ref) {
        def cands = [
                "${ref}:apps/thingworx-operator/.version",
                "${ref}:charts/thingworx-operator/.version",
        ]
        for (c in cands) {
            if (gitCatFileExists(dir, c)) {
                return gitShow(dir, c)
            }
        }
        return null
    }

    static boolean hasOperatorVersionAt(File dir, String ref) {
        return resolveOperatorVersionAt(dir, ref) != null
    }
}
