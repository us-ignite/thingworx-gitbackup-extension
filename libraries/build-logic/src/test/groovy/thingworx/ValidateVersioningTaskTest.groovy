package thingworx

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

class ValidateVersioningTaskTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder()
    static final String OPERATOR = 'apps/thingworx-operator/.version'
    static final String CHART_VERSION = 'charts/thingworx-operator/.version'

    private File repository() {
        File root = temporary.newFolder()
        git(root, 'init', '-b', 'main')
        git(root, 'config', 'user.name', 'Hook Test')
        git(root, 'config', 'user.email', 'hook@example.invalid')
        git(root, 'config', 'core.hooksPath', '/dev/null')
        git(root, 'config', 'commit.gpgsign', 'false')
        write(root, OPERATOR, '0.4.0\n')
        write(root, CHART_VERSION, '0.4.0\n')
        write(root, 'libraries/thingworx-dap/.version', '0.1.0\n')
        write(root, 'libraries/thingworx-dap-runtime/.version', '0.1.0\n')
        commitAll(root, 'chore: fixture')
        return root
    }

    // main gains a released operator feature, then main is merged into a
    // feature branch: the merge commit's diff carries the operator change but
    // its subject is "Merge branch 'main' into ...".
    private File repositoryWithMergeFromMain() {
        File root = repository()
        git(root, 'checkout', '-b', 'test/branch')
        write(root, 'docs/notes.md', 'notes\n')
        commitAll(root, 'test: branch-only change')
        git(root, 'checkout', 'main')
        write(root, 'apps/thingworx-operator/change.txt', 'change\n')
        write(root, OPERATOR, '0.5.0\n')
        write(root, CHART_VERSION, '0.5.0\n')
        commitAll(root, 'feat: operator change')
        git(root, 'checkout', 'test/branch')
        git(root, 'merge', '--no-ff', '--no-edit', 'main')
        assertTrue(git(root, 'log', '-1', '--format=%s').startsWith('Merge branch'))
        return root
    }

    private ValidateVersioningTask task(File root, String range) {
        def project = ProjectBuilder.builder().withProjectDir(root).build()
        def task = project.tasks.create('validate', ValidateVersioningTask)
        task.rootDir = root
        task.range = range
        return task
    }

    @Test void skipsMergeCommitFromMainOnPullRequest() {
        File root = repositoryWithMergeFromMain()
        // ci.yml on a pull_request merge ref: HEAD^1 = main, HEAD^2 = branch head.
        task(root, 'main..test/branch').validate()
    }

    @Test void stillValidatesMergedInCommits() {
        File root = repository()
        git(root, 'checkout', '-b', 'test/branch')
        write(root, 'apps/thingworx-operator/change.txt', 'change\n')
        commitAll(root, 'feat: operator change without version bump')
        git(root, 'checkout', 'main')
        write(root, 'docs/notes.md', 'notes\n')
        commitAll(root, 'docs: main-only change')
        git(root, 'checkout', 'test/branch')
        git(root, 'merge', '--no-ff', '--no-edit', 'main')
        try {
            task(root, 'main..test/branch').validate()
            fail('Expected the unbumped operator commit to be rejected')
        } catch (GradleException e) {
            assertTrue(e.message, e.message.contains("missing required $OPERATOR update"))
        }
    }

    private static void commitAll(File root, String message) {
        git(root, 'add', '.')
        git(root, 'commit', '-m', message)
    }

    private static void write(File root, String path, String text) {
        File file = new File(root, path)
        file.parentFile.mkdirs()
        file.text = text
    }

    private static String git(File root, String... args) {
        def process = new ProcessBuilder(['git'] + args.toList()).directory(root).redirectErrorStream(true).start()
        String output = process.inputStream.text
        assertEquals(output, 0, process.waitFor())
        return output.trim()
    }
}
