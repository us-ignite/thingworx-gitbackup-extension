package org.us_ignite.thingworx.dap.runtime;

import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.BooleanPrimitive;
import com.thingworx.types.primitives.DatetimePrimitive;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.IntegerPrimitive;
import com.thingworx.types.primitives.PasswordPrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import org.joda.time.DateTime;

/** Primitive conversion used by generated live DataShape row proxies. */
public final class DapValues {
    private DapValues() {}

    public static String getString(ValueCollection row, String name) {
        Object value = value(row, name);
        return value == null ? null : String.valueOf(value);
    }

    public static void putString(ValueCollection row, String name, String value) {
        row.put(name, new StringPrimitive(value));
    }

    public static Boolean getBoolean(ValueCollection row, String name) {
        Object value = value(row, name);
        return value instanceof Boolean booleanValue ? booleanValue : null;
    }

    public static void putBoolean(ValueCollection row, String name, Boolean value) {
        row.put(name, new BooleanPrimitive(value));
    }

    public static Integer getInteger(ValueCollection row, String name) {
        Object value = value(row, name);
        return value instanceof Number number ? number.intValue() : null;
    }

    public static void putInteger(ValueCollection row, String name, Integer value) {
        IntegerPrimitive primitive = new IntegerPrimitive();
        primitive.setValue(value);
        row.put(name, primitive);
    }

    public static DateTime getDatetime(ValueCollection row, String name) {
        Object value = value(row, name);
        return value instanceof DateTime dateTime ? dateTime : null;
    }

    public static void putDatetime(ValueCollection row, String name, DateTime value) {
        row.put(name, new DatetimePrimitive(value));
    }

    public static InfoTable getInfoTable(ValueCollection row, String name) {
        Object value = value(row, name);
        return value instanceof InfoTable infoTable ? infoTable : null;
    }

    public static void putInfoTable(ValueCollection row, String name, InfoTable value) {
        row.put(name, new InfoTablePrimitive(value));
    }

    public static void putPassword(ValueCollection row, String name, String value) {
        row.put(name, new PasswordPrimitive(value));
    }

    public static String getPassword(ValueCollection row, String name) {
        return getString(row, name);
    }

    private static Object value(ValueCollection row, String name) {
        var primitive = row.getPrimitive(name);
        return primitive == null ? null : primitive.getValue();
    }
}
