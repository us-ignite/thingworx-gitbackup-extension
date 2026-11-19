package thingworx

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category

/**
 * Variant attributes for artifacts exchanged between projects. Producers publish an outgoing
 * variant; consumers declare an ordinary project dependency on a resolvable configuration, so
 * task dependencies are inferred by Gradle instead of being wired by path or by reading another
 * project's build directory.
 */
class ThingworxArtifacts {
    static final Attribute<String> KIND = Attribute.of('io.github.us-ignite.thingworx.artifact', String)

    /** An installable ThingWorx extension zip. */
    static final String EXTENSION = 'extension-zip'
    /** The generated GIT mashup entity directory. */
    static final String UI_ENTITIES = 'ui-entities'
    /** A generated Javadoc directory. */
    static final String JAVADOC = 'javadoc-dir'

    private static void describe(Project project, AttributeContainer attributes, String kind) {
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category, 'thingworx'))
        attributes.attribute(KIND, kind)
    }

    /** Outgoing variant: add artifacts to the returned configuration. */
    static Configuration outgoing(Project project, String name, String kind) {
        project.configurations.maybeCreate(name).tap {
            canBeConsumed = true
            canBeResolved = false
            describe(project, attributes, kind)
        }
    }

    /** Resolvable configuration selecting {@code kind} variants from its project dependencies. */
    static Configuration incoming(Project project, String name, String kind) {
        project.configurations.maybeCreate(name).tap {
            canBeConsumed = false
            canBeResolved = true
            transitive = false
            describe(project, attributes, kind)
        }
    }
}
