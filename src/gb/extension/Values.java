package gb.extension;

import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.IPrimitiveType;

/** Small, null-safe conversions at ThingWorx service and configuration boundaries. */
final class Values {
    private Values() {}

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static boolean hasText(String value) {
        return !isBlank(value);
    }

    static boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    static String orDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    static String primitiveString(ValueCollection row, String field) {
        if (row == null) return null;
        IPrimitiveType primitive = row.getPrimitive(field);
        if (primitive == null || primitive.getValue() == null) return null;
        return primitive.getValue().toString();
    }
}
