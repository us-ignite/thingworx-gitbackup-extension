package gitbackup.extension;

import com.thingworx.contentmanagement.ImportedEntityCollection;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.logging.LogUtilities;
import com.thingworx.migration.ExtensionMigratorBase;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.resources.entities.EntityServices;
import com.thingworx.things.Thing;
import com.thingworx.thingtemplates.ThingTemplate;
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
        // Remove the pre-5.11 global importer state before the replacement entities are imported.
        // Existing repositories retain their Git configuration and use current-user
        // authorization for internal source-control imports.
        EntityServices entityServices = new EntityServices();
        try {
            entityServices.DeleteApplicationKey("GitExtensionAppKey");
            _logger.warn("Removed legacy GitExtensionAppKey.");
        } catch (Exception ignored) {
            // The legacy key is optional on clean installations.
        }
        try {
            if (EntityUtilities.exists("ExtensionImportTargets", ThingworxRelationshipTypes.Thing)) {
                entityServices.DeleteThing("ExtensionImportTargets");
                _logger.warn("Removed legacy ExtensionImportTargets Thing.");
            }
        } catch (Exception e) {
            _logger.warn("Could not remove legacy ExtensionImportTargets Thing.", e);
        }
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

        migrateTemplate("GitBackupTemplate");
        migrateTemplate("GitRepositoryTemplate");

        if (EntityUtilities.exists("GITBACKUP.Utility.Thing", ThingworxRelationshipTypes.Thing)) {
            _logger.warn(
                    "Legacy utility Thing GITBACKUP.Utility.Thing retained for compatibility; "
                            + "new code uses GIT.Utility.Thing.");
        }
    }

    private void migrateTemplate(String oldTemplateName) throws Exception {
        ThingTemplate oldTemplate =
                (ThingTemplate)
                        EntityUtilities.findEntity(
                                oldTemplateName, ThingworxRelationshipTypes.ThingTemplate);
        if (oldTemplate == null) {
            return;
        }

        _logger.warn(
                "Migrating Things from "
                        + oldTemplateName
                        + " to GIT.Repository.ThingTemplate...");
        var implementingThings = oldTemplate.QueryImplementingThingsV2(null, null, null, null, null);
        for (int i = 0; i < implementingThings.getRowCount(); i++) {
            String thingName =
                    implementingThings.getRow(i).getPrimitive("name").getValue().toString();
            Thing gitThing =
                    (Thing)
                            EntityUtilities.findEntity(
                                    thingName, ThingworxRelationshipTypes.Thing);
            if (gitThing != null) {
                gitThing.setThingTemplateName("GIT.Repository.ThingTemplate");
            }
        }
        new EntityServices().DeleteThingTemplate(oldTemplateName);
        _logger.warn(oldTemplateName + " migrated and removed.");
    }
}
