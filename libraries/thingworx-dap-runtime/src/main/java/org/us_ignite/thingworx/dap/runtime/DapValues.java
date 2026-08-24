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

    /**
     * Gets a String value from a row.
     *
     * @param row the source row
     * @param name the field name
     * @return the named String value, or {@code null} when absent
     */
    public static String getString(ValueCollection row, String name) {
        Object value = value(row, name);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Stores a String value in a row.
     *
     * @param row the target row
     * @param name the field name
     * @param value the value to store
     */
    public static void putString(ValueCollection row, String name, String value) {
        row.put(name, new StringPrimitive(value));
    }

    /**
     * Gets a Boolean value from a row.
     *
     * @param row the source row
     * @param name the field name
     * @return the named Boolean value, or {@code null} when absent
     */
    public static Boolean getBoolean(ValueCollection row, String name) {
        Object value = value(row, name);
        return value instanceof Boolean booleanValue ? booleanValue : null;
    }

    /**
     * Stores a Boolean value in a row.
     *
     * @param row the target row
     * @param name the field name
     * @param value the value to store
     */
    public static void putBoolean(ValueCollection row, String name, Boolean value) {
        row.put(name, new BooleanPrimitive(value));
    }

    /**
     * Gets an Integer value from a row.
     *
     * @param row the source row
     * @param name the field name
     * @return the named Integer value, or {@code null} when absent
     */
    public static Integer getInteger(ValueCollection row, String name) {
        Object value = value(row, name);
        return value instanceof Number number ? number.intValue() : null;
    }

    /**
     * Stores an Integer value in a row.
     *
     * @param row the target row
     * @param name the field name
     * @param value the value to store
     */
    public static void putInteger(ValueCollection row, String name, Integer value) {
        IntegerPrimitive primitive = new IntegerPrimitive();
        primitive.setValue(value);
        row.put(name, primitive);
    }

    /**
     * Gets a DateTime value from a row.
     *
     * @param row the source row
     * @param name the field name
     * @return the named DateTime value, or {@code null} when absent
     */
    public static DateTime getDatetime(ValueCollection row, String name) {
        Object value = value(row, name);
        return value instanceof DateTime dateTime ? dateTime : null;
    }

    /**
     * Stores a DateTime value in a row.
     *
     * @param row the target row
     * @param name the field name
     * @param value the value to store
     */
    public static void putDatetime(ValueCollection row, String name, DateTime value) {
        row.put(name, new DatetimePrimitive(value));
    }

    /**
     * Gets an InfoTable value from a row.
     *
     * @param row the source row
     * @param name the field name
     * @return the named InfoTable value, or {@code null} when absent
     */
    public static InfoTable getInfoTable(ValueCollection row, String name) {
        Object value = value(row, name);
        return value instanceof InfoTable infoTable ? infoTable : null;
    }

    /**
     * Stores an InfoTable value in a row.
     *
     * @param row the target row
     * @param name the field name
     * @param value the value to store
     */
    public static void putInfoTable(ValueCollection row, String name, InfoTable value) {
        row.put(name, new InfoTablePrimitive(value));
    }

    /**
     * Stores a password value in a row.
     *
     * @param row the target row
     * @param name the field name
     * @param value the value to store
     */
    public static void putPassword(ValueCollection row, String name, String value) {
        row.put(name, new PasswordPrimitive(value));
    }

    /**
     * Gets a password value from a row.
     *
     * @param row the source row
     * @param name the field name
     * @return the named password value, or {@code null} when absent
     */
    public static String getPassword(ValueCollection row, String name) {
        return getString(row, name);
    }

    private static Object value(ValueCollection row, String name) {
        var primitive = row.getPrimitive(name);
        return primitive == null ? null : primitive.getValue();
    }
}
