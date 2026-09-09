package thingworx

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

abstract class PrepareCommitMsgTask extends DefaultTask {

    @Input @Optional
    String commitMsgFile = null

    @Input
    boolean dryRun = false

    @org.gradle.api.tasks.Internal
    File rootDir

    PrepareCommitMsgTask() {
        description = 'Applies Conventional Commit versioning policy for staged changes'
        group = 'verification'
        // capture rootDir at configuration time for config-cache
        rootDir = project.rootDir
    }

    @TaskAction
    void run() {
        File root = rootDir
        if (root == null) throw new GradleException("rootDir not set")
        boolean isDryRun = checkDryRun(root)
        File msgFile = resolveCommitMsgFile(root)

        if (msgFile == null || !msgFile.exists()) {
            throw new GradleException("versioning hook: commit message file not found: $msgFile (tried COMMIT_MSG_FILE env, commitMsgFile property, .git/COMMIT_EDITMSG)")
        }

        String rawMsg = msgFile.getText('UTF-8').replaceAll('\r', '')
        String header = rawMsg.readLines().find { it != null } ?: ''
        // first line only
        header = header.trim()
        // but keep original header logic: first line via sed -n '1p'
        def lines = rawMsg.split('\n')
        header = lines.length > 0 ? lines[0].replaceAll('\r', '').trim() : ''

        String conventionalHeader = /^[a-z][a-z0-9-]*(\([^)]+\))?(!)?:\s+.+$/
        String featHeader = /^feat(\(|:|!)/
        String patchHeader = /^(fix|perf)(\(|:|!)/

        boolean conventional = (header ==~ conventionalHeader)

        List<String> staged = VersionUtils.stagedPaths(root)
        boolean jgitChanged = false
        boolean dapChanged = false
        boolean operatorChanged = false
        for (p in staged) {
            if (p in ['apps/thingworx-jgit-extension/.version','extensions/jgit/.version','libraries/thingworx-dap/.version','libraries/thingworx-dap-runtime/.version','apps/thingworx-operator/.version','charts/thingworx-operator/.version']) {
                continue
            } else if (p.startsWith('apps/thingworx-jgit-extension/') || p.startsWith('extensions/jgit/')) {
                jgitChanged = true
            } else if (p.startsWith('libraries/thingworx-dap/') || p.startsWith('libraries/thingworx-dap-runtime/')) {
                dapChanged = true
            } else if (p.startsWith('apps/thingworx-operator/') || p.startsWith('charts/thingworx-operator/') || p.startsWith('images/thingworx-operator/')) {
                // images/thingworx-operator is.version-derived (OPERATOR_VERSION = file(.../.version) in images/.../build.gradle:5);
                // docs/vendored images (platform/security-tool/connection-server) are pin-based (ALL_TWX) and exempt.
                operatorChanged = true
            }
        }

        List<String> targets = []
        if (jgitChanged) targets << 'apps/thingworx-jgit-extension/.version'
        if (dapChanged) targets.addAll(['libraries/thingworx-dap/.version','libraries/thingworx-dap-runtime/.version'])
        if (operatorChanged) targets << 'apps/thingworx-operator/.version'

        List<String> allVersionFiles = ['apps/thingworx-jgit-extension/.version','extensions/jgit/.version','libraries/thingworx-dap/.version','libraries/thingworx-dap-runtime/.version','apps/thingworx-operator/.version','charts/thingworx-operator/.version']
        if (!targets.isEmpty() && !conventional) {
            throw new GradleException('versioning hook: component changes require a Conventional Commit header (for example: fix: repair clone handling).')
        }

        for (vf in allVersionFiles) {
            // git diff --cached --quiet -- vf  => exit 0 means no diff
            def proc = new ProcessBuilder(['git','diff','--cached','--quiet','--',vf]).directory(root).start()
            proc.waitFor()
            boolean hasDiff = proc.exitValue() != 0
            if (!hasDiff) continue
            if (vf == 'extensions/jgit/.version') continue // allow legacy deletion during rename
            boolean found = targets.contains(vf)
            if (!found) {
                throw new GradleException("$vf is managed by the hook and cannot be edited manually.")
            }
        }

        String bump = 'none'
        if (!targets.isEmpty()) {
            if ((header =~ /!:/) || (rawMsg =~ /(?m)^BREAKING[ \t-]+CHANGE:\s+/)) {
                bump = 'major'
            } else if (header =~ featHeader) {
                bump = 'minor'
            } else if (header =~ patchHeader) {
                bump = 'patch'
            }
        }

        if (dapChanged) {
            String core = VersionUtils.gitShow(root, ':libraries/thingworx-dap/.version') ?: VersionUtils.gitShow(root, 'HEAD:libraries/thingworx-dap/.version')
            String runtime = VersionUtils.gitShow(root, ':libraries/thingworx-dap-runtime/.version') ?: VersionUtils.gitShow(root, 'HEAD:libraries/thingworx-dap-runtime/.version')
            if (core != null) core = core.replaceAll(/\s+/, '')
            if (runtime != null) runtime = runtime.replaceAll(/\s+/, '')
            if (core != runtime) {
                throw new GradleException('DAP core and runtime versions must remain identical.')
            }
        }

        if (bump == 'none') {
            for (target in targets) {
                boolean hasHead = VersionUtils.gitCatFileExists(root, "HEAD:$target") || VersionUtils.gitCatFileExists(root, "HEAD:extensions/jgit/.version")
                // operator targets also check charts/.version as alternate source
                if (target == 'apps/thingworx-operator/.version') {
                    hasHead = hasHead || VersionUtils.gitCatFileExists(root, "HEAD:charts/thingworx-operator/.version")
                }
                if (hasHead) {
                    if (!VersionUtils.gitCatFileExists(root, "HEAD:$target")) {
                        // legacy exists but new not yet - rename transition, allow non-releasing without new file
                        // for operator, charts/.version may be the legacy
                        if (target == 'apps/thingworx-operator/.version' && VersionUtils.gitCatFileExists(root, "HEAD:charts/thingworx-operator/.version")) {
                            // allow if staging the canonical from charts legacy
                            continue
                        }
                        continue
                    }
                    def proc = new ProcessBuilder(['git','diff','--cached','--quiet','--',target]).directory(root).start()
                    proc.waitFor()
                    if (proc.exitValue() != 0) {
                        throw new GradleException("$target must not change for a non-releasing commit type.")
                    }
                    // charts/.version must stay in sync with canonical; check it too for non-releasing
                    if (target == 'apps/thingworx-operator/.version') {
                        def chartProc = new ProcessBuilder(['git','diff','--cached','--quiet','--','charts/thingworx-operator/.version']).directory(root).start()
                        chartProc.waitFor()
                        if (chartProc.exitValue() != 0) {
                            throw new GradleException("charts/thingworx-operator/.version must not change for a non-releasing commit type (keep in sync with canonical).")
                        }
                    }
                } else {
                    String stagedContent = VersionUtils.gitShow(root, ":$target")
                    if (stagedContent == null || stagedContent.isEmpty()) continue
                    stagedContent = stagedContent.replaceAll(/\s+/, '')
                    String expected = VersionUtils.bootstrapVersion(target)
                    if (stagedContent != expected) {
                        throw new GradleException("$target bootstraps as $stagedContent; expected $expected.")
                    }
                }
            }
            if (!targets.isEmpty()) {
                logger.lifecycle("versioning hook: no release version update ($header).")
            }
            return
        }

        boolean needsRetry = false
        for (target in targets) {
            String legacy = 'extensions/jgit/.version'
            String chartLegacy = 'charts/thingworx-operator/.version'
            boolean hasHead = false
            String current = null
            if (VersionUtils.gitCatFileExists(root, "HEAD:$target")) {
                hasHead = true
                current = VersionUtils.gitShow(root, "HEAD:$target")
            } else if (target == 'apps/thingworx-jgit-extension/.version' && VersionUtils.gitCatFileExists(root, "HEAD:$legacy")) {
                hasHead = true
                current = VersionUtils.gitShow(root, "HEAD:$legacy")
            } else if (target == 'apps/thingworx-jgit-extension/.version' && VersionUtils.gitCatFileExists(root, 'HEAD:.version')) {
                hasHead = true
                current = VersionUtils.gitShow(root, 'HEAD:.version')
            } else if (target == 'apps/thingworx-operator/.version' && VersionUtils.gitCatFileExists(root, "HEAD:$chartLegacy")) {
                hasHead = true
                current = VersionUtils.gitShow(root, "HEAD:$chartLegacy")
            }

            if (!hasHead) {
                String base = baselineVersion(root, target, 'HEAD')
                String expected = base
                if (bump != 'none') expected = VersionUtils.nextVersion(base, bump)
                String stagedVer = VersionUtils.gitShow(root, ":$target")
                stagedVer = stagedVer ? stagedVer.replaceAll(/\s+/, '') : ''
                if (stagedVer != expected) {
                    logger.lifecycle("versioning hook: $target -> $expected ($bump)")
                    if (!isDryRun) {
                        File f = new File(root, target)
                        f.parentFile.mkdirs()
                        f.setText(expected + '\n', 'UTF-8')
                        execGitAdd(root, target)
                        needsRetry = true
                        // keep charts/.version in sync with canonical operator version
                        if (target == 'apps/thingworx-operator/.version') {
                            File cf = new File(root, 'charts/thingworx-operator/.version')
                            cf.parentFile.mkdirs()
                            cf.setText(expected + '\n', 'UTF-8')
                            execGitAdd(root, 'charts/thingworx-operator/.version')
                        }
                    }
                } else {
                    logger.lifecycle("versioning hook: $target already staged at $expected")
                    if (!isDryRun && target == 'apps/thingworx-operator/.version') {
                        String chartStaged = VersionUtils.gitShow(root, ":charts/thingworx-operator/.version")
                        chartStaged = chartStaged ? chartStaged.replaceAll(/\s+/, '') : ''
                        if (chartStaged != expected) {
                            File cf = new File(root, 'charts/thingworx-operator/.version')
                            cf.parentFile.mkdirs()
                            cf.setText(expected + '\n', 'UTF-8')
                            execGitAdd(root, 'charts/thingworx-operator/.version')
                            needsRetry = true
                        }
                    }
                }
                continue
            }

            current = current.replaceAll(/\s+/, '')
            String proposed = VersionUtils.nextVersion(current, bump)
            String stagedVer2 = VersionUtils.gitShow(root, ":$target")
            stagedVer2 = stagedVer2 ? stagedVer2.replaceAll(/\s+/, '') : ''
            if (stagedVer2 != proposed) {
                logger.lifecycle("versioning hook: $target -> $proposed ($bump)")
                if (!isDryRun) {
                    File f = new File(root, target)
                    f.parentFile.mkdirs()
                    f.setText(proposed + '\n', 'UTF-8')
                    execGitAdd(root, target)
                    needsRetry = true
                    if (target == 'apps/thingworx-operator/.version') {
                        File cf = new File(root, 'charts/thingworx-operator/.version')
                        cf.parentFile.mkdirs()
                        cf.setText(proposed + '\n', 'UTF-8')
                        execGitAdd(root, 'charts/thingworx-operator/.version')
                    }
                }
            } else {
                logger.lifecycle("versioning hook: $target already staged at $proposed")
                if (!isDryRun && target == 'apps/thingworx-operator/.version') {
                    String chartStaged = VersionUtils.gitShow(root, ":charts/thingworx-operator/.version")
                    chartStaged = chartStaged ? chartStaged.replaceAll(/\s+/, '') : ''
                    if (chartStaged != proposed) {
                        File cf = new File(root, 'charts/thingworx-operator/.version')
                        cf.parentFile.mkdirs()
                        cf.setText(proposed + '\n', 'UTF-8')
                        execGitAdd(root, 'charts/thingworx-operator/.version')
                        needsRetry = true
                    }
                }
            }
        }

        if (needsRetry) {
            throw new GradleException('version files were updated and staged; rerun git commit with the same message.')
        }
    }

    private boolean checkDryRun(File root) {
        if (dryRun) return true
        // check project property via system property or gradle property file
        def prop = System.getProperty('dryRun') ?: System.getenv('DRY_RUN')
        if (prop != null) return prop.toString() == 'true'
        // also check root gradle.properties or command line -PdryRun
        // fallback: check if file .version dryRun marker exists
        // Try to read from project properties via file
        try {
            String v = new File(root, 'gradle.properties').text
            if (v.contains('dryRun=true')) return true
        } catch (e) {}
        // check env var COMMIT still
        return false
    }

    private File resolveCommitMsgFile(File root) {
        // 1) explicit property from task field
        if (commitMsgFile != null) {
            return new File(commitMsgFile)
        }
        def prop = System.getProperty('commitMsgFile') ?: System.getProperty('commit-msg-file')
        if (prop != null) return new File(prop.toString())
        def env = System.getenv('COMMIT_MSG_FILE')
        if (env != null && !env.isEmpty()) return new File(env)
        env = System.getenv('GIT_COMMIT_MSG_FILE')
        if (env != null && !env.isEmpty()) return new File(env)
        // 2) from args via project property hookArgs ?
        // 3) fallback to .git/COMMIT_EDITMSG via git rev-parse --git-dir
        def gitDir = VersionUtils.execGit(root, 'rev-parse', '--git-dir')
        if (gitDir != null) {
            File gitDirFile = new File(root, gitDir)
            if (!gitDirFile.isAbsolute()) gitDirFile = new File(root, gitDir)
            File candidate = new File(gitDirFile, 'COMMIT_EDITMSG')
            if (candidate.exists()) return candidate
        }
        // also try .git/COMMIT_EDITMSG directly
        File fallback = new File(root, '.git/COMMIT_EDITMSG')
        if (fallback.exists()) return fallback
        // last try: message file passed as system property prepareCommitMsg.messageFile (plugin shim)
        def shim = System.getenv('PREPARE_COMMIT_MSG_FILE')
        if (shim != null) return new File(shim)
        return fallback
    }

    private String baselineVersion(File root, String target, String parentRef) {
        // for jgit variants, try HEAD apps, then extensions, then .version
        if (target in ['apps/thingworx-jgit-extension/.version','extensions/jgit/.version']) {
            String v = VersionUtils.resolveJgitVersionAt(root, 'HEAD')
            if (v != null) return v
            return VersionUtils.bootstrapVersion(target)
        }
        if (target in ['apps/thingworx-operator/.version','charts/thingworx-operator/.version']) {
            String v = VersionUtils.resolveOperatorVersionAt(root, 'HEAD')
            if (v != null) return v
            return VersionUtils.bootstrapVersion(target)
        }
        return VersionUtils.bootstrapVersion(target)
    }

    private void execGitAdd(File root, String target) {
        logger.lifecycle("execGitAdd: root=$root target=$target exists=${new File(root, target).exists()} indexExists=${new File(root, '.git/index').exists()}")
        def proc = new ProcessBuilder(['git','add','--verbose','--',target]).directory(root).redirectErrorStream(true).start()
        def out = new ByteArrayOutputStream()
        proc.consumeProcessOutput(out, out)
        proc.waitFor()
        String msg = out.toString()
        logger.lifecycle("git add output: $msg exit=${proc.exitValue()}")
        if (proc.exitValue() != 0) {
            // try diagnostic
            def diag = new ProcessBuilder(['ls','-l','.git/index','.git/index.lock']).directory(root).redirectErrorStream(true).start()
            def diagOut = new ByteArrayOutputStream()
            diag.consumeProcessOutput(diagOut, diagOut)
            diag.waitFor()
            logger.lifecycle("diag ls: ${diagOut.toString()}")
            throw new GradleException("git add $target failed (exit ${proc.exitValue()}): $msg")
        }
    }
}
