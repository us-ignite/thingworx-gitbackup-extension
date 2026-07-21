package gb.extension;

import com.thingworx.contentmanagement.ImportedEntityCollection;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.logging.LogUtilities;
import com.thingworx.migration.ExtensionMigratorBase;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.things.Thing;
import org.slf4j.Logger;

public class ExtMigrator extends ExtensionMigratorBase {

    private static Logger _logger =
            LogUtilities.getInstance().getApplicationLogger(ExtMigrator.class);

    @Override
    public void migrate(ImportedEntityCollection imports) throws Exception {
        _logger.warn(
                "ExtMigrator: Preparing migration from "
                        + getFromVersion()
                        + " to "
                        + getToVersion()
                        + ".");
    }

    @Override
    public void postMigrate(ImportedEntityCollection imports) throws Exception {
        _logger.warn("ExtMigrator: Starting post-import migration.");
        // Migrated from any other version to 2.3.0
        Thing thing =
                (Thing)
                        EntityUtilities.findEntity(
                                "GIT.Utility.Thing", ThingworxRelationshipTypes.Thing);
        if (thing == null) {
            throw new IllegalStateException("ExtMigrator: GIT.Utility.Thing was not imported.");
        }
        thing.processServiceRequest("InitUserExtensionProperties", null);
        _logger.warn("Performed one-time migration to 2.3.0.");

        // Migrate to 5.2.0: Initialize GpgKeys UserExtension property for all users
        thing.processServiceRequest("InitUserExtensionGpgKeysProperty", null);
        _logger.warn(
                "Performed one-time migration to 5.2.0: GpgKeys UserExtension property initialized.");
    }
}
