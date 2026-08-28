package thingworx

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

abstract class ValidateVersioningTask extends DefaultTask {

    @Input @Optional
    String range = null

    @org.gradle.api.tasks.Internal
    File rootDir

    ValidateVersioningTask() {
        description = 'Validate Conventional Commit versioning in a git range'
        group = 'verification'
        rootDir = project.rootDir
        range = 'HEAD~1..HEAD'
    }

    @TaskAction
    void validate() {
        File root = rootDir
        if (root == null) throw new GradleException("rootDir not set")
        String r = range ?: 'HEAD~1..HEAD'
        if (!r.contains('..')) throw new GradleException("range must be base..head: $r")
        String base = r.split('\\.\\.')[0]
        String head = r.split('\\.\\.')[1]
        if (VersionUtils.execGit(root, 'rev-parse','--verify','-q', "${base}^{commit}") == null) {
            // exec returns null on failure? check via cat-file
            def proc = new ProcessBuilder(['git','rev-parse','--verify','-q', "${base}^{commit}"]).directory(root).start()
            proc.waitFor()
            if (proc.exitValue()!=0) throw new GradleException("unknown range base: $base")
        }
        def procHead = new ProcessBuilder(['git','rev-parse','--verify','-q', "${head}^{commit}"]).directory(root).start()
        procHead.waitFor()
        if (procHead.exitValue()!=0) throw new GradleException("unknown range head: $head")

        // get commits in range reverse
        def revList = new ProcessBuilder(['git','rev-list','--reverse', r]).directory(root).start()
        def out = new ByteArrayOutputStream()
        revList.consumeProcessOutput(out, new ByteArrayOutputStream())
        revList.waitFor()
        List<String> commits = out.toString().trim().split('\n').findAll { it }

        String conventionalHeader = /^[a-z][a-z0-9-]*(\([^)]+\))?(!)?:\s+.+$/
        String featHeader = /^feat(\(|:|!)/
        String patchHeader = /^(fix|perf)(\(|:|!)/

        for (commit in commits) {
            String parent = VersionUtils.execGit(root, 'rev-parse', "${commit}^")
            String header = VersionUtils.execGit(root, 'log','-1','--format=%s', commit) ?: ''
            String message = new ProcessBuilder(['git','log','-1','--format=%B', commit]).directory(root).start().with { p ->
                def o = new ByteArrayOutputStream()
                p.consumeProcessOutput(o, new ByteArrayOutputStream())
                p.waitFor()
                o.toString()
            }
            List<String> paths = []
            def diffProc = new ProcessBuilder(['git','diff-tree','--no-commit-id','--name-only','-r', parent, commit]).directory(root).start()
            def diffOut = new ByteArrayOutputStream()
            diffProc.consumeProcessOutput(diffOut, new ByteArrayOutputStream())
            diffProc.waitFor()
            paths = diffOut.toString().trim().split('\n').findAll { it }

            boolean jgit = false, dap = false
            for (p in paths) {
                if (p in ['apps/thingworx-jgit-extension/.version','extensions/jgit/.version','libraries/thingworx-dap/.version','libraries/thingworx-dap-runtime/.version']) continue
                else if (p.startsWith('apps/thingworx-jgit-extension/') || p.startsWith('extensions/jgit/')) jgit = true
                else if (p.startsWith('libraries/thingworx-dap/') || p.startsWith('libraries/thingworx-dap-runtime/')) dap = true
            }
            if (!jgit && !dap) continue
            if (!(header ==~ conventionalHeader)) throw new GradleException("$commit has component changes but an invalid Conventional Commit header: $header")
            String bump = 'none'
            if ((header =~ /!:/) || (message =~ /(?m)^BREAKING[ \t-]+CHANGE:\s+/)) bump = 'major'
            else if (header =~ featHeader) bump = 'minor'
            else if (header =~ patchHeader) bump = 'patch'

            List<String> targets = []
            if (jgit) targets << 'apps/thingworx-jgit-extension/.version'
            if (dap) targets.addAll(['libraries/thingworx-dap/.version','libraries/thingworx-dap-runtime/.version'])

            for (target in targets) {
                boolean changed = paths.contains(target)
                String legacy = 'extensions/jgit/.version'
                if (target == 'apps/thingworx-jgit-extension/.version' && paths.contains(legacy)) changed = true

                boolean hasParentVersion = false
                if (target == 'apps/thingworx-jgit-extension/.version') hasParentVersion = VersionUtils.hasJgitVersionAt(root, parent)
                else hasParentVersion = VersionUtils.gitCatFileExists(root, "$parent:$target")

                if (!hasParentVersion) {
                    if (!changed) throw new GradleException("$commit is missing bootstrap $target.")
                    String newVer = VersionUtils.gitShow(root, "$commit:$target") ?: (target == 'apps/thingworx-jgit-extension/.version' ? VersionUtils.gitShow(root, "$commit:$legacy") : null)
                    if (newVer == null) throw new GradleException("$commit is missing $target content.")
                    newVer = newVer.replaceAll(/\s+/, '')
                    String baseVer = VersionUtils.hasJgitVersionAt(root, parent) ? VersionUtils.resolveJgitVersionAt(root, parent) : VersionUtils.bootstrapVersion(target)
                    if (target != 'apps/thingworx-jgit-extension/.version') baseVer = VersionUtils.bootstrapVersion(target)
                    // baseline logic: for jgit, use resolve
                    if (target == 'apps/thingworx-jgit-extension/.version') {
                        baseVer = VersionUtils.resolveJgitVersionAt(root, parent) ?: VersionUtils.bootstrapVersion(target)
                    }
                    String expected = baseVer
                    if (bump != 'none') expected = VersionUtils.nextVersion(baseVer, bump)
                    if (newVer != expected) throw new GradleException("$commit bootstraps $target as $newVer; expected $expected.")
                    continue
                }

                if (bump == 'none') {
                    boolean isRenameOnly = false
                    if (target == 'apps/thingworx-jgit-extension/.version' && changed) {
                        String old = VersionUtils.resolveJgitVersionAt(root, parent)
                        String newVer = VersionUtils.gitShow(root, "$commit:$target") ?: VersionUtils.gitShow(root, "$commit:$legacy")
                        if (newVer != null) newVer = newVer.replaceAll(/\s+/, '')
                        if (old != null) old = old.replaceAll(/\s+/, '')
                        if (newVer == old) isRenameOnly = true
                    }
                    if (isRenameOnly) continue
                    if (changed) throw new GradleException("$commit changes $target for a non-releasing type.")
                } else {
                    if (!changed) throw new GradleException("$commit is missing required $target update.")
                    String old = VersionUtils.resolveJgitVersionAt(root, parent)
                    if (target != 'apps/thingworx-jgit-extension/.version') old = VersionUtils.gitShow(root, "$parent:$target")?.replaceAll(/\s+/, '')
                    else old = old?.replaceAll(/\s+/, '')
                    String newVer = VersionUtils.gitShow(root, "$commit:$target") ?: (target == 'apps/thingworx-jgit-extension/.version' ? VersionUtils.gitShow(root, "$commit:$legacy") : null)
                    newVer = newVer?.replaceAll(/\s+/, '')
                    String expected = VersionUtils.nextVersion(old, bump)
                    if (newVer != expected) throw new GradleException("$commit has $target=$newVer; expected $expected.")
                }
            }
            if (dap) {
                String core = VersionUtils.gitShow(root, "$commit:libraries/thingworx-dap/.version")?.replaceAll(/\s+/, '')
                String runtime = VersionUtils.gitShow(root, "$commit:libraries/thingworx-dap-runtime/.version")?.replaceAll(/\s+/, '')
                if (core != runtime) throw new GradleException("$commit leaves DAP core ($core) and runtime ($runtime) mismatched.")
            }
        }

        File coreFile = new File(root, 'libraries/thingworx-dap/.version')
        File runtimeFile = new File(root, 'libraries/thingworx-dap-runtime/.version')
        if (coreFile.exists() && runtimeFile.exists()) {
            String core = coreFile.text.trim()
            String runtime = runtimeFile.text.trim()
            if (core != runtime) throw new GradleException("working tree leaves DAP core ($core) and runtime ($runtime) mismatched.")
        }
        logger.lifecycle("versioning validation passed for $r")
    }
}
