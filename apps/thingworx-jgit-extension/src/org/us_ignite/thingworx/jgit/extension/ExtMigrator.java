package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.contentmanagement.ImportedEntityCollection;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.logging.LogUtilities;
import com.thingworx.migration.ExtensionMigratorBase;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.things.Thing;
import org.slf4j.Logger;

/** Initializes extension-owned per-user properties and runs the upgrade after an import. */
public class ExtMigrator extends ExtensionMigratorBase {

    private static Logger _logger =
            LogUtilities.getInstance().getApplicationLogger(ExtMigrator.class);

    @Override
    /** Performs compatibility migration for imported entities. */
    public void migrate(ImportedEntityCollection imports) throws Exception {
        // no-op: no previous versions to migrate from
    }

    @Override
    /** Initializes the utility Thing’s user properties after entity import completes. */
    public void postMigrate(ImportedEntityCollection imports) throws Exception {
        _logger.warn("ExtMigrator: Starting post-import initialization.");
        Thing thing =
                (Thing)
                        EntityUtilities.findEntity(
                                "GIT.Utility.Thing", ThingworxRelationshipTypes.Thing);
        if (thing == null) {
            throw new IllegalStateException("ExtMigrator: GIT.Utility.Thing was not imported.");
        }
        thing.processServiceRequest("InitUserExtensionProperties", null);
        _logger.warn("ExtMigrator: UserExtension properties initialized.");
        // Moves repositories and every user's settings to the current format; see UpgradeManager.
        thing.processServiceRequest("RunUpgrade", null);
        _logger.warn("ExtMigrator: Upgrade finished.");
    }
}
