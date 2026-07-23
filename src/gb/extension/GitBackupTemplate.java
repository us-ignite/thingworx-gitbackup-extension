package gb.extension;

import com.thingworx.metadata.annotations.ThingworxBaseTemplateDefinition;

/**
 * Backward-compatibility class for entities created by versions before the template was renamed to
 * {@link GitRepositoryTemplate}. Do not use for new entities.
 */
@Deprecated
@ThingworxBaseTemplateDefinition(name = "GenericThing")
public class GitBackupTemplate extends GitRepositoryTemplate {
    private static final long serialVersionUID = -6500080561143490845L;
}
