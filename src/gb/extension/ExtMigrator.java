package gb.extension;

import com.thingworx.contentmanagement.ImportedEntityCollection;
import com.thingworx.entities.collections.RootEntityCollection;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.logging.LogUtilities;
import com.thingworx.migration.ExtensionMigratorBase;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.things.Thing;
import com.thingworx.types.primitives.StringPrimitive;
import org.slf4j.Logger;

public class ExtMigrator extends ExtensionMigratorBase {

    private static Logger _logger =
            LogUtilities.getInstance().getApplicationLogger(ExtMigrator.class);

    private static final String MIGRATOR_VERSION_PROP = "MigratorVersion";

    @Override
    public void migrate(ImportedEntityCollection imports) throws Exception {
        String fromVer = getFromVersion().getVersion();
        String toVer = getToVersion().getVersion();
        _logger.warn("ExtMigrator: Preparing migration from {} to {}.", fromVer, toVer);

        for (ThingworxRelationshipTypes type : imports.keySet()) {
            RootEntityCollection entities = imports.get(type);
            if (entities != null && !entities.isEmpty()) {
                _logger.warn("ExtMigrator: Importing {} entities of type {}:", entities.size(), type.name());
                for (String name : entities.keySet()) {
                    _logger.warn("  - {}: {}", type.name(), name);
                }
            }
        }

        if ("0.0.0".equals(fromVer)) {
            _logger.warn("ExtMigrator: First installation detected.");
            for (String entityName : KEY_ENTITIES) {
                if (entityExists(entityName)) {
                    _logger.warn(
                            "ExtMigrator: Entity '{}' already exists on first install — possible re-install after partial failure.",
                            entityName);
                }
            }
        } else {
            _logger.warn("ExtMigrator: Upgrade detected (from {} to {}).", fromVer, toVer);
        }
    }

    @Override
    public void postMigrate(ImportedEntityCollection imports) throws Exception {
        _logger.warn("ExtMigrator: Starting post-import migration.");

        Thing thing = getUtilityThing();
        if (thing == null) {
            _logger.error(
                    "ExtMigrator: GIT.Utility.Thing not found. Init services will be skipped.");
            return;
        }

        runInitService(thing, "InitUserExtensionProperties");
        runInitService(thing, "InitUserExtensionGpgKeysProperty");

        trackMigrationVersion(thing);

        verifyEntityHealth();

        _logger.warn("ExtMigrator: Post-import migration completed successfully.");
    }

    private Thing getUtilityThing() throws Exception {
        Thing thing =
                (Thing)
                        EntityUtilities.findEntity(
                                "GIT.Utility.Thing", ThingworxRelationshipTypes.Thing);
        if (thing == null) {
            _logger.error("ExtMigrator: GIT.Utility.Thing not found in entity registry.");
            return null;
        }
        _logger.warn("ExtMigrator: Found GIT.Utility.Thing.");
        return thing;
    }

    private void runInitService(Thing thing, String serviceName) {
        try {
            thing.processServiceRequest(serviceName, null);
            _logger.warn("ExtMigrator: Successfully executed {}.", serviceName);
        } catch (Exception e) {
            _logger.error("ExtMigrator: Failed to execute {}: {}", serviceName, e.getMessage());
        }
    }

    private void trackMigrationVersion(Thing thing) {
        String toVer = getToVersion().getVersion();
        try {
            thing.setPropertyValue(MIGRATOR_VERSION_PROP, new StringPrimitive(toVer));
            _logger.warn(
                    "ExtMigrator: Stored migration version {} on GIT.Utility.Thing.", toVer);
        } catch (Exception e) {
            _logger.warn(
                    "ExtMigrator: Could not store migration version (prop '{}' may not exist): {}",
                    MIGRATOR_VERSION_PROP,
                    e.getMessage());
        }
    }

    private static final String[][] ENTITY_CHECKS = {
        {"GIT.Utility.Thing", "Thing"},
        {"GitBackupTemplate", "ThingTemplate"},
        {"GIT.Provider.ThingTemplate", "ThingTemplate"},
        {"GitRepository", "Thing"},
        {"Git.Utility.ThingShape", "ThingShape"},
    };

    private static final String[] KEY_ENTITIES = {
        "GIT.Utility.Thing",
        "GitBackupTemplate",
        "GIT.Provider.ThingTemplate",
        "GitRepository",
    };

    private boolean entityExists(String name) {
        try {
            return EntityUtilities.exists(name, ThingworxRelationshipTypes.Thing)
                    || EntityUtilities.exists(name, ThingworxRelationshipTypes.ThingTemplate)
                    || EntityUtilities.exists(name, ThingworxRelationshipTypes.ThingShape);
        } catch (Exception e) {
            _logger.warn("ExtMigrator: Error checking existence of '{}': {}", name, e.getMessage());
            return false;
        }
    }

    private void verifyEntityHealth() {
        _logger.warn("ExtMigrator: Verifying entity health...");
        for (String[] check : ENTITY_CHECKS) {
            String name = check[0];
            String typeName = check[1];
            ThingworxRelationshipTypes relType = resolveType(typeName);
            if (relType == null) {
                continue;
            }
            try {
                boolean exists = EntityUtilities.exists(name, relType);
                if (exists) {
                    _logger.warn("ExtMigrator: Entity '{}' ({}) confirmed present.", name, typeName);
                } else {
                    _logger.warn("ExtMigrator: Entity '{}' ({}) is MISSING after import!", name, typeName);
                }
            } catch (Exception e) {
                _logger.warn(
                        "ExtMigrator: Error verifying entity '{}' ({}): {}",
                        name,
                        typeName,
                        e.getMessage());
            }
        }
    }

    private static ThingworxRelationshipTypes resolveType(String typeName) {
        try {
            return ThingworxRelationshipTypes.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            _logger.warn("ExtMigrator: Unknown entity type '{}' in health check.", typeName);
            return null;
        }
    }
}
