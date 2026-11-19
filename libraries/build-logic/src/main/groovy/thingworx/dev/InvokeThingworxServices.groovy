package thingworx.dev

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** POSTs (without parameters) to each service on a Thing; failures are warnings. */
@DisableCachingByDefault(because = 'Acts on a running development stack')
abstract class InvokeThingworxServices extends DevStackTask {
    @Input
    abstract Property<String> getThing()

    @Input
    abstract ListProperty<String> getServiceNames()

    @TaskAction
    void invoke() {
        serviceNames.get().each { service ->
            def conn = DevHttp.open(twx("Things/${thing.get()}/Services/${service}"), twxAuth())
            conn.requestMethod = 'POST'
            DevHttp.TWX_POST_HEADERS.each { k, v -> conn.setRequestProperty(k, v) }
            conn.setRequestProperty('Content-Type', 'application/json')
            conn.doOutput = true
            int status = conn.responseCode
            if (status != 200) logger.warn("Initialization service ${service} failed with status ${status}")
        }
    }
}
