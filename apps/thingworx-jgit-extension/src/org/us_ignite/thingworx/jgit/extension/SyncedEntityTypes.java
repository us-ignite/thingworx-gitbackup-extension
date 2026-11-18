package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The entity types a tracked project synchronizes. A type is named by its collection, which is also
 * its folder in a project export: {@code Things}, {@code Mashups}, {@code DataShapes}, and so on. A
 * project stores its selection as a comma-separated list; a blank list means every type, including
 * types not listed in {@link #SELECTABLE}.
 *
 * <p>The project's own {@code Projects} entry is always synchronized, since it is the project, so
 * it cannot be selected.
 */
final class SyncedEntityTypes {
    /** The collection of Project entities, always synchronized. */
    static final String PROJECTS = "Projects";

    /** The entity types a project can choose from, in display order. */
    static final List<String> SELECTABLE =
            List.of(
                    "ApplicationKeys",
                    "Authenticators",
                    "Dashboards",
                    "DataShapes",
                    "DataTags",
                    "DirectoryServices",
                    "Groups",
                    "LocalizationTables",
                    "Mashups",
                    "MediaEntities",
                    "Menus",
                    "ModelTags",
                    "Networks",
                    "NotificationContents",
                    "NotificationDefinitions",
                    "Organizations",
                    "PersistenceProviders",
                    "StateDefinitions",
                    "StyleDefinitions",
                    "StyleThemes",
                    "ThingGroups",
                    "ThingShapes",
                    "ThingTemplates",
                    "Things",
                    "Users");

    private SyncedEntityTypes() {}

    /**
     * Normalizes a stored or entered selection: entries separated by commas or whitespace, matched
     * to {@link #SELECTABLE} ignoring case, without repeats, in display order. Blank, or every
     * selectable type, gives {@code ""}, meaning all types.
     *
     * @throws IllegalArgumentException for an entry that is not a selectable type
     */
    static String normalize(String value) {
        if (value == null || value.isBlank()) return "";
        Set<String> chosen = new LinkedHashSet<>();
        for (String part : value.split("[,\\s]+")) {
            if (part.isEmpty()) continue;
            String match = null;
            for (String type : SELECTABLE) {
                if (type.equalsIgnoreCase(part)) match = type;
            }
            if (match == null) {
                throw new IllegalArgumentException(
                        "Unknown entity type '"
                                + part
                                + "'. Use one or more of: "
                                + String.join(", ", SELECTABLE)
                                + ".");
            }
            chosen.add(match);
        }
        if (chosen.isEmpty() || chosen.size() == SELECTABLE.size()) return "";
        List<String> ordered = new ArrayList<>();
        for (String type : SELECTABLE) if (chosen.contains(type)) ordered.add(type);
        return String.join(",", ordered);
    }

    /** The types in a normalized selection; empty means all. */
    static List<String> list(String entityTypes) {
        if (entityTypes == null || entityTypes.isBlank()) return List.of();
        return List.of(entityTypes.split(","));
    }

    /**
     * Whether a project with this normalized selection synchronizes the collection. Projects are
     * always synchronized; a {@code null} collection (a file outside any collection folder) only
     * when every type is.
     */
    static boolean syncs(String entityTypes, String collection) {
        if (entityTypes == null || entityTypes.isBlank()) return true;
        if (PROJECTS.equals(collection)) return true;
        return collection != null && list(entityTypes).contains(collection);
    }

    /** The collection, and export folder, of an entity type, such as {@code Things}. */
    static String collectionOf(ThingworxRelationshipTypes type) {
        String name = type.name();
        return switch (name) {
            case "DataTagVocabulary" -> "DataTags";
            case "ModelTagVocabulary" -> "ModelTags";
            default ->
                    name.endsWith("y") ? name.substring(0, name.length() - 1) + "ies" : name + "s";
        };
    }

    /** A readable name for a collection: {@code ThingTemplates} becomes {@code Thing Templates}. */
    static String label(String collection) {
        return collection.replaceAll("(?<=[a-z])(?=[A-Z])", " ");
    }

    /**
     * A readable summary of a normalized selection, such as {@code All} or {@code Things, Mashups}.
     */
    static String describe(String entityTypes) {
        List<String> types = list(entityTypes);
        if (types.isEmpty()) return "All";
        return String.join(", ", types.stream().map(SyncedEntityTypes::label).toList());
    }
}
