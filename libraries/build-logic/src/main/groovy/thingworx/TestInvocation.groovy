package thingworx

import org.gradle.api.invocation.Gradle

/**
 * Repo rule: tests (unit, integration, containerized) and the Docker image builds they need only
 * run when a test lifecycle task is requested explicitly. {@code build}/{@code check} stay
 * assemble/package only. This is the one place the rule is evaluated.
 */
class TestInvocation {
    static final List<String> LIFECYCLES = ['test', 'testAll', 'integrationTest', 'verify']

    static boolean requested(Gradle gradle) {
        gradle.startParameter.taskNames.any { String name ->
            LIFECYCLES.any { name == it || name.endsWith(":${it}") }
        }
    }
}
