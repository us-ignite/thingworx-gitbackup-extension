package org.us_ignite.thingworx.dap.example;

import com.thingworx.types.InfoTable;

/** Compile-time proof that generated row/table proxies are usable from ordinary Java. */
public final class ExampleUsage {
    private ExampleUsage() {}

    public static InfoTable oneCommit() throws Exception {
        ExampleCommitRow row =
                ExampleCommitRow.create().commitId("abc123").message("Generated binding");
        return ExampleCommitTable.create().add(row).toInfoTable();
    }
}
