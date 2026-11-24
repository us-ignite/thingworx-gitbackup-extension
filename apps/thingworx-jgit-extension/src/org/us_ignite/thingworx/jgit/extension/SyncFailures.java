package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.StringPrimitive;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a Sync to or from ThingWorx could not move, so the rest of the sync can go ahead: a sync
 * succeeds partially rather than failing as a whole when one entity, collection, project or Data
 * Table fails. Each failure names its project, collection and entity where known, with the reason.
 */
final class SyncFailures {
    /** The data shape of one failure row in a sync result. */
    static final String DATA_SHAPE = "GIT.SyncFailure.DataShape";

    static final String PROJECT_NAME = "ProjectName";
    static final String COLLECTION = "Collection";
    static final String ENTITY_NAME = "EntityName";
    static final String REASON = "Reason";

    /** How many failures a result message names before summarizing. */
    static final int SHOWN = 10;

    /**
     * One item a sync could not move. A blank collection means the whole project, a blank entity
     * the whole collection.
     */
    record Failure(String project, String collection, String entity, String reason) {
        /** {@code Project/Collection/Entity}, leaving out blank parts. */
        String item() {
            StringBuilder item = new StringBuilder(project);
            if (!collection.isEmpty()) item.append('/').append(collection);
            if (!entity.isEmpty()) item.append('/').append(entity);
            return item.toString();
        }
    }

    private final List<Failure> failures = new ArrayList<>();

    void add(String project, String collection, String entity, String reason) {
        failures.add(
                new Failure(
                        blankIfNull(project),
                        blankIfNull(collection),
                        blankIfNull(entity),
                        blankIfNull(reason)));
    }

    void add(String project, String collection, String entity, Throwable cause) {
        add(project, collection, entity, reason(cause));
    }

    List<Failure> list() {
        return List.copyOf(failures);
    }

    boolean isEmpty() {
        return failures.isEmpty();
    }

    int size() {
        return failures.size();
    }

    /**
     * Names the failures for a result message, such as {@code " 2 items were not imported:
     * IOT/Things/A (reason); IOT/Mashups (reason)."}, or blank when there are none.
     */
    String note(String notDone) {
        if (failures.isEmpty()) return "";
        List<String> shown = new ArrayList<>();
        for (Failure failure : failures.subList(0, Math.min(failures.size(), SHOWN))) {
            shown.add(
                    failure.reason().isEmpty()
                            ? failure.item()
                            : failure.item() + " (" + failure.reason() + ")");
        }
        String names = String.join("; ", shown);
        if (failures.size() > shown.size()) {
            names += "; and " + (failures.size() - shown.size()) + " more";
        }
        return " "
                + failures.size()
                + (failures.size() == 1 ? " item was" : " items were")
                + " "
                + notDone
                + ": "
                + names
                + ".";
    }

    /** The failures as rows of {@value #DATA_SHAPE}. */
    InfoTable table() throws Exception {
        InfoTable table = InfoTableInstanceFactory.createInfoTableFromDataShape(DATA_SHAPE);
        for (Failure failure : failures) {
            ValueCollection row = new ValueCollection();
            row.put(PROJECT_NAME, new StringPrimitive(failure.project()));
            row.put(COLLECTION, new StringPrimitive(failure.collection()));
            row.put(ENTITY_NAME, new StringPrimitive(failure.entity()));
            row.put(REASON, new StringPrimitive(failure.reason()));
            table.addRow(row);
        }
        return table;
    }

    /** A short reason for a failure: its message without the stack trace marker. */
    static String reason(Throwable cause) {
        if (cause == null) return "";
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            Throwable inner = cause.getCause();
            message =
                    inner != null && inner.getMessage() != null
                            ? inner.getMessage()
                            : cause.getClass().getSimpleName();
        }
        int marker =
                message.indexOf(
                        org.us_ignite.thingworx.dap.runtime.DapResults.STACK_TRACE_MARKER.trim());
        if (marker >= 0) message = message.substring(0, marker);
        return message.strip();
    }

    /** One step of {@link #inPasses}, which may throw. */
    interface Step<T> {
        void run(T item) throws Exception;
    }

    /**
     * Runs {@code step} on every item, one at a time, in passes: the items that fail are tried
     * again in the next pass, as long as the previous pass got at least one more item through. An
     * item that depends on another, such as a Thing on its ThingTemplate, so succeeds once the
     * other has. Returns the items that never succeeded, in their original order, each with the
     * error of its last attempt.
     */
    static <T> Map<T, Exception> inPasses(List<T> items, Step<T> step) {
        Map<T, Exception> errors = new LinkedHashMap<>();
        List<T> pending = new ArrayList<>(items);
        while (!pending.isEmpty()) {
            List<T> next = new ArrayList<>();
            for (T item : pending) {
                try {
                    step.run(item);
                    errors.remove(item);
                } catch (Exception e) {
                    errors.put(item, e);
                    next.add(item);
                }
            }
            if (next.size() == pending.size()) break;
            pending = next;
        }
        Map<T, Exception> failed = new LinkedHashMap<>();
        for (T item : items) {
            if (errors.containsKey(item)) failed.put(item, errors.get(item));
        }
        return failed;
    }

    private static final Pattern ENTITY_NAME_ATTRIBUTE =
            Pattern.compile("<Entities[^>]*>\\s*<\\w+[^>]*>\\s*<\\w+\\s[^>]*?\\bname=\"([^\"]*)\"");

    /**
     * The name of the entity in a source-control export file, read from its XML, or {@code
     * fallback} (such as the file name) when it cannot be found.
     */
    static String entityName(String xml, String fallback) {
        if (xml != null) {
            Matcher matcher = ENTITY_NAME_ATTRIBUTE.matcher(xml);
            if (matcher.find()) return unescapeXml(matcher.group(1));
        }
        return fallback;
    }

    private static String unescapeXml(String value) {
        return value.replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }

    private static String blankIfNull(String value) {
        return value == null ? "" : value;
    }
}
