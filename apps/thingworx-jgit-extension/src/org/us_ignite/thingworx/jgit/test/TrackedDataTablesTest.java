package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class TrackedDataTablesTest {
    @Test
    void modeDefaultsToMirrorAndIgnoresCase() {
        assertEquals(TrackedDataTables.MIRROR, TrackedDataTables.normalizeMode(null));
        assertEquals(TrackedDataTables.MIRROR, TrackedDataTables.normalizeMode(" "));
        assertEquals(TrackedDataTables.MIRROR, TrackedDataTables.normalizeMode("mirror"));
        assertEquals(TrackedDataTables.UPSERT, TrackedDataTables.normalizeMode(" UPSERT "));
        assertThrows(
                IllegalArgumentException.class, () -> TrackedDataTables.normalizeMode("Replace"));
    }

    @Test
    void fileIsBelowTheProjectDataFolder() {
        assertEquals(
                "IOT/Data/IOT.SICD.DataTable.PermissionPolicyEntries.json",
                TrackedDataTables.relativeFile(
                        "IOT", "IOT.SICD.DataTable.PermissionPolicyEntries"));
        assertEquals(
                "entities/iot/Data/T.json", TrackedDataTables.relativeFile("/entities/iot/", "T"));
    }

    @Test
    void rejectsNamesThatAreNotFileNames() {
        for (String name : new String[] {"", " ", "../x", "a/b", "a\\b", ".hidden", "a\tb"}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> TrackedDataTables.validateName(name),
                    name);
        }
        assertEquals("My.Table", TrackedDataTables.validateName(" My.Table "));
    }

    @Test
    void documentIsSortedByPrimaryKeyThenFieldName() {
        JSONObject b = new JSONObject().put("name", "b").put("value", 2).put("enabled", true);
        JSONObject a = new JSONObject().put("value", 1.0).put("name", "a");
        String doc =
                TrackedDataTables.document(
                        "T", "T.DataShape", List.of("name"), List.of(b, a), "Upsert");
        assertEquals(
                String.join(
                        "\n",
                        "{",
                        "  \"dataShape\": \"T.DataShape\",",
                        "  \"dataTable\": \"T\",",
                        "  \"mode\": \"Upsert\",",
                        "  \"primaryKey\": [",
                        "    \"name\"",
                        "  ],",
                        "  \"rows\": [",
                        "    {",
                        "      \"name\": \"a\",",
                        "      \"value\": 1",
                        "    },",
                        "    {",
                        "      \"enabled\": true,",
                        "      \"name\": \"b\",",
                        "      \"value\": 2",
                        "    }",
                        "  ]",
                        "}",
                        ""),
                doc);
        // The same rows in another order give the same bytes, and so the same hash.
        assertEquals(
                doc,
                TrackedDataTables.document(
                        "T", "T.DataShape", List.of("name"), List.of(a, b), "upsert"));
        assertEquals(
                TrackedDataTables.hash(doc),
                TrackedDataTables.hash(
                        TrackedDataTables.document(
                                "T", "T.DataShape", List.of("name"), List.of(a, b), "Upsert")));
    }

    @Test
    void hashChangesWithAnyRow() {
        JSONObject a = new JSONObject().put("name", "a").put("value", 1);
        JSONObject changed = new JSONObject().put("name", "a").put("value", 2);
        assertNotEquals(
                TrackedDataTables.hash(
                        TrackedDataTables.document("T", "S", List.of("name"), List.of(a), "")),
                TrackedDataTables.hash(
                        TrackedDataTables.document(
                                "T", "S", List.of("name"), List.of(changed), "")));
    }

    @Test
    void parsesRowsAndRejectsOtherDocuments() {
        String doc =
                TrackedDataTables.document(
                        "T",
                        "S",
                        List.of("id"),
                        List.of(new JSONObject().put("id", "1").put("nested", new JSONObject())),
                        "Mirror");
        List<JSONObject> rows = TrackedDataTables.parseRows(doc, "T");
        assertEquals(1, rows.size());
        assertEquals("1", rows.get(0).getString("id"));
        assertThrows(IllegalArgumentException.class, () -> TrackedDataTables.parseRows("[", "T"));
        assertThrows(IllegalArgumentException.class, () -> TrackedDataTables.parseRows("{}", "T"));
        assertThrows(
                IllegalArgumentException.class,
                () -> TrackedDataTables.parseRows("{\"rows\":[1]}", "T"));
    }

    @Test
    void modeIsReadFromTheFileAndDefaultsToMirror() {
        String doc = TrackedDataTables.document("T", "S", List.of("id"), List.of(), "Upsert");
        assertEquals(TrackedDataTables.UPSERT, TrackedDataTables.modeOf(doc, "T"));
        assertEquals(TrackedDataTables.MIRROR, TrackedDataTables.modeOf("{\"rows\":[]}", "T"));
        assertThrows(
                IllegalArgumentException.class,
                () -> TrackedDataTables.modeOf("{\"mode\":\"Replace\"}", "T"));
        assertThrows(IllegalArgumentException.class, () -> TrackedDataTables.modeOf("[", "T"));
    }

    @Test
    void changingTheModeKeepsTheRowsAndTheirHash() {
        JSONObject row = new JSONObject().put("id", "1").put("value", 3);
        String upsert =
                TrackedDataTables.document("T", "S", List.of("id"), List.of(row), "Upsert");
        String mirror = TrackedDataTables.withMode(upsert, "T", "mirror");
        assertEquals(
                TrackedDataTables.document("T", "S", List.of("id"), List.of(row), "Mirror"),
                mirror);
        assertEquals(
                TrackedDataTables.rowsHash(upsert, "T"), TrackedDataTables.rowsHash(mirror, "T"));
    }

    @Test
    void canonicalJsonWritesNestedValuesStably() {
        JSONObject value =
                new JSONObject()
                        .put("z", new JSONArray().put(1).put("two").put(JSONObject.NULL))
                        .put("a", new JSONObject().put("y", false).put("x", "q\"uote"))
                        .put("empty", new JSONObject())
                        .put("list", new JSONArray());
        String text = CanonicalJson.write(value);
        assertEquals(
                String.join(
                        "\n",
                        "{",
                        "  \"a\": {",
                        "    \"x\": \"q\\\"uote\",",
                        "    \"y\": false",
                        "  },",
                        "  \"empty\": {},",
                        "  \"list\": [],",
                        "  \"z\": [",
                        "    1,",
                        "    \"two\",",
                        "    null",
                        "  ]",
                        "}"),
                text);
        assertTrue(new JSONObject(text).similar(value));
    }
}
