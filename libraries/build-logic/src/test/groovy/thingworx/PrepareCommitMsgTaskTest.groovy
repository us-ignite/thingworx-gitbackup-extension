package thingworx

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

class PrepareCommitMsgTaskTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder()
    static final String OPERATOR = 'apps/thingworx-operator/.version'
    static final String CHART_VERSION = 'charts/thingworx-operator/.version'
    static final String CHART = 'charts/thingworx-operator/Chart.yaml'

    private File repository(boolean bootstrap = false) {
        File root = temporary.newFolder()
        git(root, 'init')
        git(root, 'config', 'user.name', 'Hook Test')
        git(root, 'config', 'user.email', 'hook@example.invalid')
        if (!bootstrap) {
            write(root, OPERATOR, '0.4.0\n')
            write(root, CHART_VERSION, '0.4.0\n')
        }
        write(root, CHART, 'apiVersion: v2\nname: thingworx-operator\nversion: 0.4.0 # chart release\nappVersion: "0.4.0"\ndescription: Keep this text\n')
        git(root, 'add', '.')
        git(root, '-c', 'core.hooksPath=/dev/null', 'commit', '-m', 'chore: fixture')
        write(root, 'apps/thingworx-operator/change.txt', 'change\n')
        git(root, 'add', 'apps/thingworx-operator/change.txt')
        return root
    }

    private PrepareCommitMsgTask task(File root, String message = 'feat: operator change') {
        write(root, 'message.txt', message + '\n')
        def project = ProjectBuilder.builder().withProjectDir(root).build()
        def task = project.tasks.create('hook', PrepareCommitMsgTask)
        task.commitMsgFile = new File(root, 'message.txt').absolutePath
        return task
    }

    private void expectRetry(PrepareCommitMsgTask task) {
        try {
            task.run()
            fail('Expected commit retry')
        } catch (GradleException e) {
            assertTrue(e.message, e.message.contains('updated and staged'))
        }
    }

    private void assertChart(File root, String version) {
        String chart = VersionUtils.gitShowRaw(root, ":$CHART")
        assertTrue(chart.contains("version: $version # chart release\n"))
        assertTrue(chart.contains("appVersion: \"$version\"\n"))
        assertTrue(chart.contains('description: Keep this text\n'))
        assertEquals(chart, new File(root, CHART).text)
    }

    @Test void bumpsChartAndRetryIsIdempotent() {
        File root = repository()
        def hook = task(root)
        expectRetry(hook)
        assertChart(root, '0.5.0')
        assertEquals('0.5.0', VersionUtils.gitShow(root, ":$OPERATOR"))
        assertEquals('0.5.0', VersionUtils.gitShow(root, ":$CHART_VERSION"))
        hook.run()
        assertChart(root, '0.5.0')
    }

    @Test void repairsChartWhenVersionBumpIsAlreadyStaged() {
        File root = repository()
        [OPERATOR, CHART_VERSION].each { write(root, it, '0.5.0\n'); git(root, 'add', it) }
        expectRetry(task(root))
        assertChart(root, '0.5.0')
    }

    @Test void synchronizesBootstrapVersion() {
        File root = repository(true)
        expectRetry(task(root))
        assertChart(root, '0.2.0')
    }

    @Test void dryRunDoesNotChangeIndexOrWorkingTree() {
        File root = repository()
        String before = git(root, 'diff', '--cached')
        String chart = new File(root, CHART).text
        def hook = task(root)
        hook.dryRun = true
        hook.run()
        assertEquals(before, git(root, 'diff', '--cached'))
        assertEquals(chart, new File(root, CHART).text)
    }

    @Test void refusesToStageUnrelatedUnstagedChartChanges() {
        File root = repository()
        new File(root, CHART).append('# unstaged comment\n')
        String before = git(root, 'diff', '--cached')
        try {
            task(root).run()
            fail('Expected unstaged changes rejection')
        } catch (GradleException e) {
            assertTrue(e.message, e.message.contains('unstaged changes'))
        }
        assertEquals(before, git(root, 'diff', '--cached'))
        assertTrue(new File(root, CHART).text.endsWith('# unstaged comment\n'))
    }

    @Test void repairsDriftWithoutBumpingForNonReleaseCommit() {
        File root = repository()
        write(root, CHART, new File(root, CHART).text.replace('0.4.0', '0.3.0'))
        git(root, 'add', CHART)
        def hook = task(root, 'chore: synchronize chart')
        expectRetry(hook)
        assertChart(root, '0.4.0')
        hook.run()
    }

    private static void write(File root, String path, String content) {
        File file = new File(root, path)
        file.parentFile.mkdirs()
        file.text = content
    }

    private static String git(File root, String... args) {
        def process = new ProcessBuilder((['git'] + args.toList())*.toString()).directory(root).redirectErrorStream(true).start()
        String output = process.inputStream.text
        assertEquals(output, 0, process.waitFor())
        return output
    }
}
