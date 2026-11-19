package thingworx.dev

import org.gradle.api.GradleException

/**
 * Reads a service environment value from docker-compose.yml, so the development stack's
 * credentials are defined in one place (the compose file) rather than copied into build code.
 */
class ComposeEnvironment {
    static String value(File composeFile, String key) {
        def match = (composeFile.text =~ /(?m)^\s+${java.util.regex.Pattern.quote(key)}:\s*(\S+)\s*$/)
        if (!match.find()) throw new GradleException("${key} is not set in ${composeFile}")
        match.group(1)
    }
}
