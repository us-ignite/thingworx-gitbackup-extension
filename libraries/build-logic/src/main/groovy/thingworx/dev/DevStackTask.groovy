package thingworx.dev

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.work.DisableCachingByDefault

/** Base for tasks that talk to the local docker-compose development stack. Never up to date. */
@DisableCachingByDefault(because = 'Acts on a running development stack')
abstract class DevStackTask extends DefaultTask {
    @Input
    abstract Property<String> getThingworxUrl()

    @Input
    abstract Property<String> getThingworxUser()

    @Input
    abstract Property<String> getThingworxPassword()

    DevStackTask() {
        group = 'development'
        thingworxUrl.convention('http://localhost:8080/Thingworx')
        thingworxUser.convention('Administrator')
        thingworxPassword.convention('AdminP@ssw0rd!123')
        outputs.upToDateWhen { false }
    }

    protected String twxAuth() {
        DevHttp.basic(thingworxUser.get(), thingworxPassword.get())
    }

    protected String twx(String path) {
        "${thingworxUrl.get().replaceAll('/+$', '')}/${path}"
    }
}
