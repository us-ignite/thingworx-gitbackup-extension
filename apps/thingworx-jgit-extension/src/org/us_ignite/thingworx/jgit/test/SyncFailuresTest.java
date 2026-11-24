package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Partial success of Sync to and from ThingWorx: what failed is collected and reported. */
class SyncFailuresTest {

    @Test
    void inPassesRetriesItemsWhoseDependencyLandsLater() {
        // "Thing" needs "Template", which comes after it in the list.
        Set<String> done = new HashSet<>();
        List<String> attempts = new ArrayList<>();
        Map<String, Exception> failed =
                SyncFailures.inPasses(
                        List.of("Thing", "Template", "Mashup"),
                        item -> {
                            attempts.add(item);
                            if (item.equals("Thing") && !done.contains("Template")) {
                                throw new Exception("Template not found");
                            }
                            done.add(item);
                        });
        assertTrue(failed.isEmpty(), failed.toString());
        assertEquals(List.of("Thing", "Template", "Mashup", "Thing"), attempts);
    }

    @Test
    void inPassesStopsWhenAPassMakesNoProgressAndKeepsTheLastError() {
        List<String> attempts = new ArrayList<>();
        int[] tries = {0};
        Map<String, Exception> failed =
                SyncFailures.inPasses(
                        List.of("Broken", "Fine", "AlsoBroken"),
                        item -> {
                            attempts.add(item);
                            if (item.startsWith("Fine")) return;
                            throw new Exception(item + " try " + ++tries[0]);
                        });
        assertEquals(List.of("Broken", "AlsoBroken"), List.copyOf(failed.keySet()));
        assertEquals("Broken try 3", failed.get("Broken").getMessage());
        assertEquals("AlsoBroken try 4", failed.get("AlsoBroken").getMessage());
        // Pass 1 gets Fine through, pass 2 gets nothing, so there is no pass 3.
        assertEquals(List.of("Broken", "Fine", "AlsoBroken", "Broken", "AlsoBroken"), attempts);
    }

    @Test
    void noteNamesEachFailureWithItsReason() {
        SyncFailures failures = new SyncFailures();
        assertEquals("", failures.note("not imported"));
        failures.add("IOT", "Things", "Pump.1", "Thing Template [Pump] not found");
        failures.add("IOT", "Mashups", "", "Export failed");
        failures.add("Sabel.System", "", "", "");
        assertEquals(
                " 3 items were not imported: IOT/Things/Pump.1 (Thing Template [Pump] not"
                        + " found); IOT/Mashups (Export failed); Sabel.System.",
                failures.note("not imported"));
    }

    @Test
    void noteSummarizesBeyondTheShownFailures() {
        SyncFailures failures = new SyncFailures();
        for (int i = 0; i < SyncFailures.SHOWN + 2; i++) failures.add("P", "Things", "T" + i, "x");
        String note = failures.note("not exported");
        assertTrue(note.startsWith(" 12 items were not exported: P/Things/T0 (x);"), note);
        assertTrue(note.endsWith("P/Things/T9 (x); and 2 more."), note);
        assertFalse(note.contains("T10"), note);
    }

    @Test
    void oneFailureIsSingular() {
        SyncFailures failures = new SyncFailures();
        failures.add("IOT", "Users", "ryan", "License is not available");
        assertEquals(
                " 1 item was not imported: IOT/Users/ryan (License is not available).",
                failures.note("not imported"));
    }

    @Test
    void reasonDropsTheStackTraceMarkerAndFallsBackToTheCause() {
        assertEquals(
                "Import failed",
                SyncFailures.reason(new Exception("Import failed [GIT_STACK_TRACE_BASE64:abc=]")));
        assertEquals(
                "inner problem",
                SyncFailures.reason(new Exception((String) null, new Exception("inner problem"))));
        assertEquals("IllegalStateException", SyncFailures.reason(new IllegalStateException()));
        assertEquals("", SyncFailures.reason(null));
    }

    @Test
    void entityNameIsReadFromTheExportXml() {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<Entities\n majorVersion=\"9\"\n minorVersion=\"7\">\n"
                        + "    <Things>\n"
                        + "        <Thing\n"
                        + "         description=\"probe\"\n"
                        + "         homeMashup=\"\"\n"
                        + "         name=\"Probe &amp; Co.Thing\"\n"
                        + "         projectName=\"Example\">\n";
        assertEquals("Probe & Co.Thing", SyncFailures.entityName(xml, "fallback"));
        assertEquals("fallback", SyncFailures.entityName("<Entities/>", "fallback"));
        assertEquals("fallback", SyncFailures.entityName(null, "fallback"));
    }

    @Test
    void importMessageReportsPartialSuccess() {
        SyncFailures failures = new SyncFailures();
        failures.add("IOT", "Things", "Pump.1", "Thing Template [Pump] not found");
        String message =
                ResultMessages.importMessage(
                        new FileRepositoryManager.ImportResult(
                                true,
                                List.of(),
                                List.of("new.user"),
                                List.of(),
                                List.of(),
                                failures));
        assertTrue(
                message.startsWith("Repository entities partially imported into ThingWorx."),
                message);
        assertTrue(message.contains("User new.user was created disabled"), message);
        assertTrue(
                message.contains(
                        " 1 item was not imported: IOT/Things/Pump.1 (Thing Template [Pump] not"
                                + " found). They keep their ThingWorx copy"),
                message);
    }

    @Test
    void importMessageIsUnchangedWithoutFailures() {
        String message =
                ResultMessages.importMessage(
                        new FileRepositoryManager.ImportResult(
                                true,
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                new SyncFailures()));
        assertEquals("Repository entities imported into ThingWorx.", message);
    }

    @Test
    void importMessageWhenNothingWasImported() {
        SyncFailures failures = new SyncFailures();
        failures.add("IOT", "", "", "Project IOT depends on extension [Missing]");
        String message =
                ResultMessages.importMessage(
                        new FileRepositoryManager.ImportResult(
                                false, List.of(), List.of(), List.of(), List.of(), failures));
        assertTrue(message.startsWith("Nothing was imported into ThingWorx. 1 item was"), message);
    }

    @Test
    void exportMessageSaysFilesWereKeptAndTheSyncTimeWasNotUpdated() {
        assertEquals(
                "Exported IOT from ThingWorx.",
                ResultMessages.exportMessage(
                        "IOT", new FileRepositoryManager.ExportResult(1, 0, new SyncFailures())));
        SyncFailures failures = new SyncFailures();
        failures.add("IOT", "Mashups", "", "Export failed");
        String partial =
                ResultMessages.exportMessage(
                        "IOT, Sabel.System",
                        new FileRepositoryManager.ExportResult(2, 0, failures));
        assertTrue(
                partial.startsWith(
                        "Partially exported IOT, Sabel.System from ThingWorx. 1 item was not"
                                + " exported: IOT/Mashups (Export failed)."),
                partial);
        assertTrue(partial.contains("Their files were left as they were."), partial);
        assertTrue(partial.contains("The last sync time was not updated"), partial);
        assertTrue(
                ResultMessages.exportMessage(
                                "IOT", new FileRepositoryManager.ExportResult(0, 0, failures))
                        .startsWith("Nothing was exported from ThingWorx."));
    }
}
