package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.datatables.DataTableThing;
import com.thingworx.entities.RootEntity;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.metadata.DataShapeDefinition;
import com.thingworx.metadata.FieldDefinition;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.NumberPrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Rows of tracked Data Tables, stored in Git as JSON next to the entities of the project that owns
 * the table: {@code <project folder>/Data/<DataTableName>.json}. The files define which tables are
 * tracked, and each file holds its table's {@code mode}, so a clone brings its own data sync
 * settings: tracking a table writes its file, and deleting the file stops tracking it.
 *
 * <p>The file holds only the fields of the table's DataShape, so the values ThingWorx fills in on
 * every write ({@code key}, {@code timestamp}, {@code location}, {@code source}, {@code
 * sourceType}, {@code tags}) never show as changes. Rows are sorted by primary key and objects by
 * field name, so exporting an unchanged table always writes the same bytes.
 *
 * <p>Sync to ThingWorx imports the rows after the entities, so the Data Table and its DataShape
 * exist and are current before their rows are written. {@link #MIRROR} also deletes rows that are
 * not in the file; {@link #UPSERT} only adds and updates.
 */
final class TrackedDataTables {
    private TrackedDataTables() {}

    /** Folder of a project folder that holds its tracked Data Tables. */
    static final String DATA_FOLDER = "Data";

    static final String MIRROR = "Mirror";
    static final String UPSERT = "Upsert";

    /** Rows are read in one call; ThingWorx caps the result at this many. */
    private static final double MAX_ROWS = 10_000_000d;

    static final String GET_ENTRIES = "GetDataTableEntries";
    static final String ADD_OR_UPDATE = "AddOrUpdateDataTableEntries";
    static final String DELETE_ENTRIES = "DeleteDataTableEntries";

    /** {@link #MIRROR} for a blank value; otherwise the matching mode, ignoring case. */
    static String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) return MIRROR;
        String value = mode.trim();
        if (MIRROR.equalsIgnoreCase(value)) return MIRROR;
        if (UPSERT.equalsIgnoreCase(value)) return UPSERT;
        throw new IllegalArgumentException(
                "Unknown Data Table sync mode '" + value + "'; use Mirror or Upsert.");
    }

    /**
     * A Data Table name usable as a file name: no path separators, no leading dot and no control
     * characters.
     */
    static String validateName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("A Data Table name is required.");
        if (value.startsWith(".")
                || value.contains("/")
                || value.contains("\\")
                || value.chars().anyMatch(c -> c < 0x20 || c == 0x7f)) {
            throw new IllegalArgumentException("Invalid Data Table name '" + value + "'.");
        }
        return value;
    }

    /** The file of a table, relative to the repository root, below its project's folder. */
    static String relativeFile(String projectFolder, String table) {
        String folder = projectFolder.replace('\\', '/').replaceAll("^/+|/+$", "");
        return folder + "/" + DATA_FOLDER + "/" + validateName(table) + ".json";
    }

    /**
     * The Data Table Thing named {@code name}, or {@code null} when there is none or the caller may
     * not see it.
     */
    static DataTableThing find(String name) {
        RootEntity entity = EntityUtilities.findEntity(name, ThingworxRelationshipTypes.Thing);
        if (!(entity instanceof DataTableThing table)) return null;
        try {
            return table.isVisible() ? table : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Requires that the caller may invoke {@code service} on the table. */
    static void requireServicePermission(DataTableThing table, String service) {
        boolean allowed;
        try {
            allowed = Boolean.TRUE.equals(table.CheckPermission("ServiceInvoke", service));
        } catch (Exception e) {
            allowed = false;
        }
        if (!allowed) {
            throw new SecurityException(
                    Const.ERR_PREFIX_AUTH
                            + "ServiceInvoke permission for "
                            + service
                            + " on Data Table '"
                            + table.getName()
                            + "' is required.");
        }
    }

    /** The table's primary key fields, sorted by name. */
    static List<String> primaryKey(DataShapeDefinition shape) {
        TreeSet<String> key = new TreeSet<>();
        for (FieldDefinition field : shape.getFields().getOrderedFieldsByOrdinal()) {
            if (field.isPrimaryKey()) key.add(field.getName());
        }
        return List.copyOf(key);
    }

    /** Field names of the table's DataShape. */
    private static List<String> fieldNames(DataShapeDefinition shape) {
        List<String> names = new ArrayList<>();
        for (FieldDefinition field : shape.getFields().getOrderedFieldsByOrdinal()) {
            names.add(field.getName());
        }
        return names;
    }

    /** Refuses a table without a primary key: its rows cannot be matched to update or delete. */
    static List<String> requirePrimaryKey(DataTableThing table) {
        DataShapeDefinition shape = table.getDataShape();
        List<String> key = shape == null ? List.of() : primaryKey(shape);
        if (key.isEmpty()) {
            throw new IllegalArgumentException(
                    "Data Table '"
                            + table.getName()
                            + "' has no primary key in its DataShape, so its rows cannot be"
                            + " synchronized.");
        }
        return key;
    }

    /** Reads every row of the table and returns the file content, with {@code mode}. */
    static String export(DataTableThing table, String mode) throws Exception {
        requireServicePermission(table, GET_ENTRIES);
        List<String> key = requirePrimaryKey(table);
        DataShapeDefinition shape = table.getDataShape();
        List<String> fields = fieldNames(shape);
        InfoTable entries = readEntries(table);
        List<JSONObject> rows = new ArrayList<>();
        for (int i = 0; i < entries.getRowCount(); i++) {
            JSONObject all = entries.getRow(i).toJSON();
            JSONObject row = new JSONObject();
            for (String field : fields) {
                Object value = all.opt(field);
                if (value != null && value != JSONObject.NULL) row.put(field, value);
            }
            rows.add(row);
        }
        return document(table.getName(), table.getDataShapeName(), key, rows, mode);
    }

    /** The file content for rows of a table: rows sorted by primary key, keys sorted by name. */
    static String document(
            String table,
            String dataShape,
            List<String> primaryKey,
            List<JSONObject> rows,
            String mode) {
        List<JSONObject> sorted = new ArrayList<>(rows);
        sorted.sort(
                (a, b) -> {
                    for (String field : primaryKey) {
                        int c = keyPart(a, field).compareTo(keyPart(b, field));
                        if (c != 0) return c;
                    }
                    return CanonicalJson.write(a).compareTo(CanonicalJson.write(b));
                });
        JSONObject doc = new JSONObject();
        doc.put("dataTable", table);
        doc.put("dataShape", dataShape == null ? "" : dataShape);
        doc.put("mode", normalizeMode(mode));
        doc.put("primaryKey", new JSONArray(primaryKey));
        doc.put("rows", new JSONArray(sorted));
        return CanonicalJson.write(doc) + "\n";
    }

    private static String keyPart(JSONObject row, String field) {
        Object value = row.opt(field);
        return value == null || value == JSONObject.NULL ? "" : value.toString();
    }

    /** The parsed file, or an {@link IllegalArgumentException} naming the table. */
    private static JSONObject parse(String content, String table) {
        try {
            return new JSONObject(content);
        } catch (org.json.JSONException e) {
            throw new IllegalArgumentException(
                    "The file of Data Table '" + table + "' is not valid JSON: " + e.getMessage(),
                    e);
        }
    }

    /** The {@code mode} of a file; a file without one mirrors. */
    static String modeOf(String content, String table) {
        try {
            return normalizeMode(parse(content, table).optString("mode", ""));
        } catch (IllegalArgumentException e) {
            if (e.getCause() instanceof org.json.JSONException) throw e;
            throw new IllegalArgumentException(
                    "The file of Data Table '" + table + "': " + e.getMessage(), e);
        }
    }

    /** The file content with its {@code mode} set, in the same canonical form. */
    static String withMode(String content, String table, String mode) {
        JSONObject doc = parse(content, table);
        doc.put("mode", normalizeMode(mode));
        return CanonicalJson.write(doc) + "\n";
    }

    /**
     * SHA-256 of a file's rows alone, to notice rows changed in ThingWorx since a sync. Changing
     * the mode does not change it.
     */
    static String rowsHash(String content, String table) {
        JSONArray rows = parse(content, table).optJSONArray("rows");
        return hash(CanonicalJson.write(rows == null ? new JSONArray() : rows));
    }

    /** SHA-256 of a text, as hexadecimal. */
    static String hash(String content) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The rows of a file, which must be a JSON object with a {@code rows} array of objects. */
    static List<JSONObject> parseRows(String content, String table) {
        JSONObject doc = parse(content, table);
        JSONArray rows = doc.optJSONArray("rows");
        if (rows == null) {
            throw new IllegalArgumentException(
                    "The file of Data Table '" + table + "' has no \"rows\" array.");
        }
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) {
                throw new IllegalArgumentException(
                        "Row "
                                + (i + 1)
                                + " in the file of Data Table '"
                                + table
                                + "' is not an object.");
            }
            result.add(row);
        }
        return result;
    }

    /** What importing one table changed. */
    record ImportCount(String table, int written, int deleted) {
        String describe() {
            return table
                    + " ("
                    + written
                    + (written == 1 ? " row" : " rows")
                    + (deleted > 0 ? ", " + deleted + " deleted" : "")
                    + ")";
        }
    }

    /**
     * Writes the rows of a file into the table: adds or updates every row by primary key and, in
     * {@link #MIRROR} mode, deletes the rows whose key is not in the file. Fields not in the file
     * are written empty, as in the file.
     */
    static ImportCount importRows(DataTableThing table, String content, boolean mirror)
            throws Exception {
        requireServicePermission(table, ADD_OR_UPDATE);
        if (mirror) {
            requireServicePermission(table, GET_ENTRIES);
            requireServicePermission(table, DELETE_ENTRIES);
        }
        List<String> key = requirePrimaryKey(table);
        DataShapeDefinition shape = table.getDataShape();
        InfoTable values = new InfoTable(shape);
        Map<String, Boolean> keys = new LinkedHashMap<>();
        for (JSONObject json : parseRows(content, table.getName())) {
            JSONObject row = new JSONObject();
            for (String field : json.keySet()) {
                if (shape.hasField(field)) row.put(field, json.get(field));
            }
            ValueCollection typed = ValueCollection.fromJSONTyped(row, shape);
            String rowKey = rowKey(typed, key);
            if (rowKey == null) {
                throw new IllegalArgumentException(
                        "A row in the file of Data Table '"
                                + table.getName()
                                + "' has no value for primary key "
                                + key
                                + ".");
            }
            if (keys.put(rowKey, Boolean.TRUE) != null) {
                throw new IllegalArgumentException(
                        "The file of Data Table '"
                                + table.getName()
                                + "' has more than one row with primary key "
                                + rowKey
                                + ".");
            }
            values.addRow(typed);
        }
        int deleted = 0;
        if (mirror) {
            InfoTable current = readEntries(table);
            InfoTable stale = new InfoTable(shape);
            for (int i = 0; i < current.getRowCount(); i++) {
                ValueCollection row = current.getRow(i);
                String rowKey = rowKey(row, key);
                if (rowKey == null || keys.containsKey(rowKey)) continue;
                ValueCollection keyOnly = new ValueCollection();
                for (String field : key) keyOnly.put(field, row.getPrimitive(field));
                stale.addRow(keyOnly);
            }
            if (stale.getRowCount() > 0) {
                ValueCollection params = new ValueCollection();
                params.put("values", new InfoTablePrimitive(stale));
                table.processServiceRequest(DELETE_ENTRIES, params);
                deleted = stale.getRowCount();
            }
        }
        if (values.getRowCount() > 0) {
            ValueCollection params = new ValueCollection();
            params.put("values", new InfoTablePrimitive(values));
            params.put("sourceType", new StringPrimitive("Thing"));
            table.processServiceRequest(ADD_OR_UPDATE, params);
        }
        return new ImportCount(table.getName(), values.getRowCount(), deleted);
    }

    private static InfoTable readEntries(DataTableThing table) throws Exception {
        ValueCollection params = new ValueCollection();
        params.put("maxItems", new NumberPrimitive(MAX_ROWS));
        InfoTable entries = table.processServiceRequest(GET_ENTRIES, params);
        return entries == null ? new InfoTable() : entries;
    }

    /**
     * The primary key of a row as one string, or {@code null} when a key field has no value. The
     * parts are separated by a character that cannot occur in a ThingWorx string value.
     */
    static String rowKey(ValueCollection row, List<String> key) {
        StringBuilder result = new StringBuilder();
        for (String field : key) {
            String value = row.getStringValue(field);
            if (value == null) return null;
            if (result.length() > 0) result.append('\u0000');
            result.append(value);
        }
        return result.toString();
    }

    /** Names of the Data Tables in a project that the caller can see, sorted. */
    static List<String> dataTablesOfProject(String projectName) throws Exception {
        List<String> names = new ArrayList<>();
        RootEntity found =
                EntityUtilities.findEntity(projectName, ThingworxRelationshipTypes.Project);
        if (!(found instanceof com.thingworx.projects.Project project)) return names;
        for (var reference : project.getProjectEntities()) {
            if (reference.getType() != ThingworxRelationshipTypes.Thing) continue;
            if (find(reference.getName()) != null) names.add(reference.getName());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** Mode text for messages. */
    static String describeMode(String mode) {
        return MIRROR.equals(mode)
                ? "mirror (rows missing from the file are deleted)"
                : "upsert (rows are added and updated, never deleted)";
    }
}
