package thingworx.dev

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.work.DisableCachingByDefault

/** Base for tasks that also talk to the development Gitea. */
@DisableCachingByDefault(because = 'Acts on a running development stack')
abstract class GiteaTask extends DevStackTask {
    @Input
    abstract Property<String> getGiteaUrl()

    @Input
    abstract Property<String> getGiteaUser()

    @Input
    abstract Property<String> getGiteaPassword()

    GiteaTask() {
        giteaUrl.convention('http://localhost:3000')
        giteaUser.convention('giteauser')
        giteaPassword.convention('giteapass123')
    }

    protected String giteaAuth() {
        DevHttp.basic(giteaUser.get(), giteaPassword.get())
    }

    protected String gitea(String path) {
        "${giteaUrl.get().replaceAll('/+$', '')}/${path}"
    }
}
