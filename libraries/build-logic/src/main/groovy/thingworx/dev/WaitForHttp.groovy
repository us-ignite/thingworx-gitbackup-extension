package thingworx.dev

import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Polls a URL until it answers HTTP 200, optionally with ThingWorx basic auth. */
@DisableCachingByDefault(because = 'Acts on a running development stack')
abstract class WaitForHttp extends DevStackTask {
    @Input
    abstract Property<String> getUrl()

    @Input
    abstract Property<Boolean> getAuthenticated()

    @Input
    abstract Property<Integer> getAttempts()

    @Input
    abstract Property<Integer> getIntervalSeconds()

    @Input
    abstract Property<String> getLabel()

    @Input
    @Optional
    abstract Property<String> getSuccessMessage()

    @Input
    abstract Property<String> getFailureMessage()

    WaitForHttp() {
        authenticated.convention(false)
        attempts.convention(60)
        intervalSeconds.convention(15)
    }

    @TaskAction
    void await() {
        String auth = authenticated.get() ? twxAuth() : null
        int last = -1
        for (int i = 1; i <= attempts.get(); i++) {
            last = DevHttp.status(url.get(), auth, 5000)
            if (last == 200) {
                if (successMessage.isPresent()) logger.lifecycle(successMessage.get())
                return
            }
            if (i < attempts.get()) {
                logger.lifecycle("  Waiting for ${label.get()}... (${i}/${attempts.get()})")
                Thread.sleep(intervalSeconds.get() * 1000L)
            }
        }
        throw new GradleException("${failureMessage.get()} (last HTTP status ${last})")
    }
}
