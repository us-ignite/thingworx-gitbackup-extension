package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.logging.LogUtilities;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.BooleanPrimitive;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import org.slf4j.Logger;

/** Builds the one-row, strongly typed result models returned by extension services. */
final class ServiceResults {
    static final String OPERATION = "GIT.OperationResult.DataShape";
    static final String STRING = "GIT.StringResult.DataShape";
    private static final Logger LOGGER =
            LogUtilities.getInstance().getApplicationLogger(ServiceResults.class);

    private ServiceResults() {}

    static InfoTable operation(String message) {
        return operation(message, false);
    }

    static InfoTable failure(String message) {
        return operation(message, true);
    }

    /**
     * Logs the complete failure server-side and returns only the caller-safe message in the
     * standard operation result.
     */
    static InfoTable fromError(String serviceName, Throwable error, String message) {
        logError(serviceName, error, message);
        return failure(message);
    }

    static InfoTable operation(String message, boolean error) {
        return row(OPERATION, null, message, error);
    }

    static InfoTable string(String value, String message) {
        InfoTable result = table(STRING);
        ValueCollection row = statusRow(message, false);
        row.put("Response", new StringPrimitive(value == null ? "" : value));
        result.addRow(row);
        return result;
    }

    static InfoTable stringFailure(String message) {
        InfoTable result = table(STRING);
        ValueCollection row = statusRow(message, true);
        row.put("Response", new StringPrimitive(""));
        result.addRow(row);
        return result;
    }

    static InfoTable stringFromError(String serviceName, Throwable error, String message) {
        logError(serviceName, error, message);
        return stringFailure(message);
    }

    static InfoTable payload(String resultShape, InfoTable payload, String message) {
        return row(resultShape, payload, message, false);
    }

    static InfoTable payloadFailure(String resultShape, String message) {
        return row(resultShape, null, message, true);
    }

    static InfoTable fromError(
            String resultShape, String serviceName, Throwable error, String message) {
        logError(serviceName, error, message);
        return payloadFailure(resultShape, message);
    }

    static InfoTable emptyPayload(String payloadShape) {
        return table(payloadShape);
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

    private static void logError(String serviceName, Throwable error, String message) {
        LOGGER.error("{} failed: {}", serviceName, message, error);
    }

    private static InfoTable row(String resultShape, InfoTable payload, String message, boolean error) {
        InfoTable result = table(resultShape);
        ValueCollection row = statusRow(message, error);
        if (payload != null) row.put("Response", new InfoTablePrimitive(payload));
        result.addRow(row);
        return result;
    }

    private static ValueCollection statusRow(String message, boolean error) {
        ValueCollection row = new ValueCollection();
        row.put("Message", new StringPrimitive(message == null ? "" : message));
        row.put("Error", new BooleanPrimitive(error));
        return row;
    }

    private static InfoTable table(String dataShape) {
        try {
            return InfoTableInstanceFactory.createInfoTableFromDataShape(dataShape);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create service result DataShape " + dataShape, e);
        }
    }
}
