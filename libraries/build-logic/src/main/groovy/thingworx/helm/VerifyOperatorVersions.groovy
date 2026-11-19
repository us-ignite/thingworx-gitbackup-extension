package thingworx.helm

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Checks that the operator's .version, the chart's .version and Chart.yaml agree. */
@DisableCachingByDefault(because = 'Validation only')
abstract class VerifyOperatorVersions extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getCanonicalVersionFile()

    /** Optional so a missing file is reported as a mismatch, not a validation error. */
    @Internal
    abstract RegularFileProperty getChartVersionFile()

    @Internal
    abstract RegularFileProperty getChartYaml()

    @TaskAction
    void verify() {
        String canonical = canonicalVersionFile.get().asFile.text.trim()
        if (!canonical.matches(/^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.-]+)?$/)) {
            throw new GradleException("Canonical version '$canonical' is not SemVer")
        }
        logger.lifecycle("Canonical operator version: ${canonical} (from apps/thingworx-operator/.version)")
        List<String> errors = []

        File dotVersion = chartVersionFile.get().asFile
        if (dotVersion.exists()) {
            String v = dotVersion.text.trim()
            logger.lifecycle("charts/thingworx-operator/.version: ${v}")
            if (v != canonical) errors << "charts/thingworx-operator/.version ($v) != canonical ($canonical)"
        } else {
            errors << 'Missing charts/thingworx-operator/.version'
        }

        File yaml = chartYaml.get().asFile
        if (yaml.exists()) {
            String text = yaml.text
            def version = (text =~ /(?m)^version:\s*([^\s#]+)/)
            def appVersion = (text =~ /(?m)^appVersion:\s*"?([^"\s#]+)"?/)
            String chartVer = version.find() ? version.group(1).trim() : null
            String appVer = appVersion.find() ? appVersion.group(1).trim().replaceAll(/^"|"$/, '') : null
            logger.lifecycle("charts/thingworx-operator/Chart.yaml version: ${chartVer}, appVersion: ${appVer}")
            if (chartVer != canonical) errors << "charts/thingworx-operator/Chart.yaml version ($chartVer) != canonical ($canonical)"
            if (appVer != canonical) errors << "charts/thingworx-operator/Chart.yaml appVersion ($appVer) != canonical ($canonical)"
        } else {
            errors << 'Missing charts/thingworx-operator/Chart.yaml'
        }

        if (errors) {
            throw new GradleException("Version mismatch detected:\n - " + errors.join("\n - ") + "\nCanonical version is $canonical (apps/thingworx-operator/.version).")
        }
        logger.lifecycle("verifyVersions: all operator version sources agree on ${canonical} ✓")
    }
}
