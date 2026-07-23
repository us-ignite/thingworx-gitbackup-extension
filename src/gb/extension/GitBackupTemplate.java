package gb.extension;

import com.thingworx.metadata.annotations.ThingworxBaseTemplateDefinition;

/** Compatibility bridge for the historical template class. */
@Deprecated
@ThingworxBaseTemplateDefinition(name = "GenericThing")
public class GitBackupTemplate extends gitbackup.extension.GitRepositoryShape {
    private static final long serialVersionUID = 1L;
}
