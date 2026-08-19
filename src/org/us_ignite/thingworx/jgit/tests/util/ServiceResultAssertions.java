package org.us_ignite.thingworx.jgit.tests.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Assertions and payload accessors for the extension's one-row ServiceResult InfoTables. */
public final class ServiceResultAssertions {
    private ServiceResultAssertions() {}

    public static JsonObject assertSuccess(String response) {
        JsonObject table = JsonParser.parseString(response).getAsJsonObject();
        assertTrue(table.has("rows"), "Service result must be an InfoTable: " + response);
        JsonArray rows = table.getAsJsonArray("rows");
        assertEquals(1, rows.size(), "Service result must contain one row: " + response);
        JsonObject row = rows.get(0).getAsJsonObject();
        assertTrue(row.has("Error"), "Service result must include Error: " + response);
        assertFalse(row.get("Error").getAsBoolean(), "Service returned an error: " + response);
        return row;
    }

    public static JsonArray responseRows(String response) {
        JsonObject row = assertSuccess(response);
        assertTrue(row.has("Result"), "Service result must include Result: " + response);
        JsonObject payload = row.getAsJsonObject("Result");
        assertTrue(payload.has("rows"), "Service payload must be an InfoTable: " + response);
        return payload.getAsJsonArray("rows");
    }

    public static JsonObject responseDataShape(String response) {
        JsonObject row = assertSuccess(response);
        assertTrue(row.has("Result"), "Service result must include Result: " + response);
        JsonObject payload = row.getAsJsonObject("Result");
        assertTrue(payload.has("dataShape"), "Service payload must include DataShape metadata: " + response);
        return payload.getAsJsonObject("dataShape");
    }
}
