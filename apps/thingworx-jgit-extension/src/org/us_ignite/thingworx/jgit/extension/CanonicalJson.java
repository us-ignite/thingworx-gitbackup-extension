package org.us_ignite.thingworx.jgit.extension;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Writes org.json values as indented JSON with object keys sorted, so the same value always
 * produces the same text and a change to one field changes one line of a diff.
 */
final class CanonicalJson {
    private CanonicalJson() {}

    private static final String INDENT = "  ";

    static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(out, value, "");
        return out.toString();
    }

    private static void write(StringBuilder out, Object value, String indent) {
        if (value == null || value == JSONObject.NULL) {
            out.append("null");
        } else if (value instanceof JSONObject object) {
            List<String> keys = new ArrayList<>(object.keySet());
            if (keys.isEmpty()) {
                out.append("{}");
                return;
            }
            keys.sort(null);
            String inner = indent + INDENT;
            out.append("{\n");
            for (int i = 0; i < keys.size(); i++) {
                out.append(inner).append(JSONObject.quote(keys.get(i))).append(": ");
                write(out, object.opt(keys.get(i)), inner);
                out.append(i + 1 < keys.size() ? ",\n" : "\n");
            }
            out.append(indent).append('}');
        } else if (value instanceof JSONArray array) {
            if (array.isEmpty()) {
                out.append("[]");
                return;
            }
            String inner = indent + INDENT;
            out.append("[\n");
            for (int i = 0; i < array.length(); i++) {
                out.append(inner);
                write(out, array.opt(i), inner);
                out.append(i + 1 < array.length() ? ",\n" : "\n");
            }
            out.append(indent).append(']');
        } else if (value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Number number) {
            double d = number.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) out.append("null");
            else out.append(JSONObject.numberToString(number));
        } else if (value instanceof java.util.Map<?, ?> map) {
            write(out, new JSONObject(map), indent);
        } else if (value instanceof java.util.Collection<?> collection) {
            write(out, new JSONArray(collection), indent);
        } else {
            out.append(JSONObject.quote(value.toString()));
        }
    }
}
