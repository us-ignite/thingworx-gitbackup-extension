package thingworx.dev

import groovy.json.JsonOutput
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Creates (idempotently) a public, empty Gitea repository owned by the Gitea user. */
@DisableCachingByDefault(because = 'Acts on a running development stack')
abstract class CreateGiteaRepository extends GiteaTask {
    @Input
    abstract Property<String> getRepository()

    @TaskAction
    void create() {
        for (int i = 1; i <= 5; i++) {
            try {
                def conn = DevHttp.open(gitea('api/v1/user/repos'), giteaAuth(), 5000)
                conn.requestMethod = 'POST'
                conn.setRequestProperty('Content-Type', 'application/json')
                conn.doOutput = true
                conn.outputStream.withCloseable {
                    it.write(JsonOutput.toJson([name: repository.get(), auto_init: false, private: false]).bytes)
                }
                int status = conn.responseCode
                if (status == 201 || status == 200) {
                    logger.lifecycle("Gitea test repo '${repository.get()}' created!")
                    return
                }
                if (status == 409) {
                    logger.lifecycle("Gitea test repo '${repository.get()}' already exists.")
                    return
                }
                logger.lifecycle("  Gitea responded ${status}, retrying... (${i}/5)")
            } catch (IOException ignored) {
                logger.lifecycle("  Waiting for Gitea API... (${i}/5)")
                Thread.sleep(3000)
            }
        }
    }
}
