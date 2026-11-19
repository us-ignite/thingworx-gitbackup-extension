package thingworx

import org.gradle.api.provider.Property

/** {@code thingworxWidget { extensionName = 'X'; extensionVersion = '1.0.0' }}. */
abstract class WidgetExtension {
    /** Extension name; also the zip base name. */
    abstract Property<String> getExtensionName()

    abstract Property<String> getExtensionVersion()

    /** Folder under ui/ in the zip that holds the bundles (defaults to the extension name). */
    abstract Property<String> getUiFolder()
}
