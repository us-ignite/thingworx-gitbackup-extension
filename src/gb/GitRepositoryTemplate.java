package gb;

import com.thingworx.metadata.annotations.ThingworxBaseTemplateDefinition;

/**
 * Compatibility class for entities created by releases before the Java package was moved to {@code
 * gb.extension}.
 */
@Deprecated
@ThingworxBaseTemplateDefinition(name = "GenericThing")
public class GitRepositoryTemplate extends gitbackup.extension.GitRepositoryShape {
    private static final long serialVersionUID = -6500080561143490845L;
}
