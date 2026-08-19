package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.DataShapeDefinition;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.BooleanPrimitive;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import org.slf4j.Logger;

/** Builds the one-row, strongly typed result models returned by extension services. */
final class ServiceResults {
    static final String STRING = "GIT.StringResult.DataShape";
    private static final Logger LOGGER = LogUtilities.getInstance().getApplicationLogger(ServiceResults.class);

    private ServiceResults() {}

    /**
     * Logs the complete failure server-side and returns only the caller-safe message in the
     * standard operation result.
     */
    static InfoTable fromError(String serviceName, Throwable error) {
        var format = String.format("%s failed: %s", serviceName, error);
        LOGGER.error(format);
        InfoTable result = runtimeUncheckedInfoTable();
        result.addRow(resultRow(true, format));
        return result;
    }

    static InfoTable fromError(String serviceName, Throwable error, DataShapeDefinition shape) {
        var format = String.format("%s failed: %s", serviceName, error);
        LOGGER.error(format);
        InfoTable result = runtimeUncheckedInfoTable(shape);
        result.addRow(resultRow(true, format, null));
        return result;
    }

    static InfoTable fromError(String serviceName, Throwable error, String shape) {
        var format = String.format("%s failed: %s", serviceName, error);
        LOGGER.error(format);
        InfoTable result = runtimeUncheckedInfoTable(shape);
        result.addRow(resultRow(true, format, null));
        return result;
    }

    static InfoTable successFromString(String serviceName, String value) {
        var format = String.format("%s completed: %s", serviceName, value);
        LOGGER.info(format);
        InfoTable result = runtimeUncheckedInfoTable();
        result.addRow(resultRow(false, value));
        return result;
    }

    static InfoTable failureFromString(String serviceName, String message) {
        var format = String.format("%s failed: %s", serviceName, message);
        LOGGER.error(format);
        InfoTable result = runtimeUncheckedInfoTable();
        result.addRow(resultRow(true, message));
        return result;
    }

    static InfoTable successFromPayload(String serviceName, String resultShape, InfoTable payload) {
        var format = String.format("%s completed with %d rows", serviceName, payload.getRowCount());
        LOGGER.info(format);
        InfoTable result = runtimeUncheckedInfoTable(resultShape);
        result.addRow(resultRow(false, format, payload));
        return result;
    }

    static InfoTable failureFromResultShape(String serviceName, String resultShape) {
        var format = String.format("%s failed: no results", serviceName);
        LOGGER.info(format);
        InfoTable result = runtimeUncheckedInfoTable(resultShape);
        result.addRow(resultRow(false, format, null));
        return result;
    }

    private static InfoTable runtimeUncheckedInfoTable(DataShapeDefinition shape) {
        try {
            return InfoTableInstanceFactory.createInfoTableFromDataShape(shape);
        } catch (Exception e) {
            throw new RuntimeException("unable to construct infotable for result");
        }
    }

    private static InfoTable runtimeUncheckedInfoTable(String shape) {
        try {
            return InfoTableInstanceFactory.createInfoTableFromDataShape(shape);
        } catch (Exception e) {
            throw new RuntimeException("unable to construct infotable for result");
        }
    }

    private static InfoTable runtimeUncheckedInfoTable() {
        try {
            return InfoTableInstanceFactory.createInfoTableFromDataShape(STRING);
        } catch (Exception e) {
            throw new RuntimeException("unable to construct infotable for result");
        }
    }

    private static ValueCollection resultRow(Boolean error, String message) {
        ValueCollection row = new ValueCollection();
        row.put("Message", new StringPrimitive(message));
        row.put("Error", new BooleanPrimitive(error));
        return row;
    }

    private static ValueCollection resultRow(Boolean error, String message, InfoTable data) {
        ValueCollection row = new ValueCollection();
        row.put("Message", new StringPrimitive(message));
        row.put("Error", new BooleanPrimitive(error));
        row.put("Result", new InfoTablePrimitive(data));
        return row;
    }

    static boolean isErr(InfoTable result) {
        if (result == null || result.getRowCount() != 1) return true;
        try {
            Object value = result.getRow(0).getPrimitive("Error").getValue();
            return !(value instanceof Boolean) || (Boolean) value;
        } catch (Exception e) {
            return true;
        }
    }

    static boolean isOk(InfoTable result) {
        return !isErr(result);
    }
}
