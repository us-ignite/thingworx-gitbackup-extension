package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.entities.interfaces.IServiceProvider;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.BooleanPrimitive;
import com.thingworx.types.primitives.IPrimitiveType;
import com.thingworx.types.primitives.NumberPrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.joda.time.DateTime;
import org.us_ignite.thingworx.dap.runtime.DapResults;

/**
 * Reads the extension's own entries from the ThingWorx ApplicationLog.
 *
 * <p>ThingWorx has no per-extension log. The extension writes through application loggers named
 * after its classes, and the ApplicationLog records each entry's origin as the abbreviated logger
 * name, such as {@code o.u.t.j.e.GitActionLogger}. {@code QueryLogEntries} matches origins exactly,
 * so this class queries each extension logger and merges the results.
 */
final class ExtensionLog {

    /** Classes whose loggers write extension entries. Add a class here when it gets a logger. */
    static final List<Class<?>> LOGGER_CLASSES =
            List.of(
                    GitActionLogger.class,
                    GitRepositoryShape.class,
                    GitUtilityThingShape.class,
                    ExtMigrator.class,
                    DapResults.class);

    private static final List<String> LEVELS = List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR");

    private ExtensionLog() {}

    /** One ApplicationLog entry written by an extension logger. */
    record Entry(
            DateTime timestamp,
            String level,
            String origin,
            String source,
            String content,
            String user,
            String thread) {}

    /**
     * Returns the ApplicationLog origin recorded for a logger named after {@code type}: every
     * package segment shortened to its first letter, followed by the simple class name.
     */
    static String origin(Class<?> type) {
        String[] parts = type.getName().split("\\.");
        StringBuilder origin = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            origin.append(parts[i].charAt(0)).append('.');
        }
        return origin.append(parts[parts.length - 1]).toString();
    }

    /**
     * Returns the newest entries written by one extension logger.
     *
     * @param type the class whose logger wrote the entries
     * @param maxItems the maximum number of entries to read
     */
    static List<Entry> read(Class<?> type, int maxItems) throws Exception {
        Object logEntity =
                EntityUtilities.findEntity("ApplicationLog", ThingworxRelationshipTypes.Log);
        if (logEntity == null) {
            throw new IllegalArgumentException("ApplicationLog entity is not available");
        }
        ValueCollection params = new ValueCollection();
        params.put("maxItems", new NumberPrimitive((double) maxItems));
        params.put("origin", new StringPrimitive(origin(type)));
        params.put("oldestFirst", new BooleanPrimitive(false));
        InfoTable rows =
                ((IServiceProvider) logEntity).processServiceRequest("QueryLogEntries", params);
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < rows.getRowCount(); i++) {
            ValueCollection row = rows.getRow(i);
            IPrimitiveType timestamp = row.getPrimitive("timestamp");
            String content = text(row, "content");
            String source = type.getSimpleName();
            int marker = content.lastIndexOf(DapResults.STACK_TRACE_MARKER);
            if (marker >= 0 && content.endsWith("]")) {
                try {
                    String encoded =
                            content.substring(
                                    marker + DapResults.STACK_TRACE_MARKER.length(),
                                    content.length() - 1);
                    source +=
                            "\n"
                                    + new String(
                                            Base64.getDecoder().decode(encoded),
                                            StandardCharsets.UTF_8);
                    content = content.substring(0, marker);
                } catch (IllegalArgumentException ignored) {
                    // Preserve unrecognized log content as-is.
                }
            }
            entries.add(
                    new Entry(
                            new DateTime(timestamp == null ? null : timestamp.getValue()),
                            text(row, "level"),
                            text(row, "origin"),
                            source,
                            content,
                            text(row, "user"),
                            text(row, "thread")));
        }
        return entries;
    }

    /**
     * Returns the newest entries from every extension logger, newest first.
     *
     * @param maxEntries the maximum number of entries to return
     * @param minimumLevel the lowest level to include; blank includes every level
     * @param searchText text the message must contain, ignoring case; blank matches everything
     */
    static List<Entry> readAll(int maxEntries, String minimumLevel, String searchText)
            throws Exception {
        int minimumRank = rank(minimumLevel);
        String search =
                searchText == null || searchText.isBlank()
                        ? null
                        : searchText.trim().toLowerCase(Locale.ROOT);
        // Filters drop rows after the read, so read further back when they are set.
        int readLimit =
                minimumRank > 0 || search != null ? Math.min(maxEntries * 5, 5000) : maxEntries;
        List<Entry> entries = new ArrayList<>();
        for (Class<?> type : LOGGER_CLASSES) {
            for (Entry entry : read(type, readLimit)) {
                if (rank(entry.level()) < minimumRank) continue;
                if (search != null && !entry.content().toLowerCase(Locale.ROOT).contains(search)) {
                    continue;
                }
                entries.add(entry);
            }
        }
        entries.sort(Comparator.comparing(Entry::timestamp).reversed());
        return entries.size() > maxEntries ? entries.subList(0, maxEntries) : entries;
    }

    /** Returns a level's position in {@link #LEVELS}; blank or unknown levels rank lowest. */
    private static int rank(String level) {
        if (level == null || level.isBlank()) return 0;
        int rank = LEVELS.indexOf(level.trim().toUpperCase(Locale.ROOT));
        return Math.max(rank, 0);
    }

    private static String text(ValueCollection row, String field) {
        String value = row.getStringValue(field);
        return value == null ? "" : value;
    }
}
