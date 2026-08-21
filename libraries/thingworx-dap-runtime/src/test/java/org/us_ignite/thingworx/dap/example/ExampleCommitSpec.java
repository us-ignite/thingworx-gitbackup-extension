package org.us_ignite.thingworx.dap.example;

import org.us_ignite.thingworx.dap.DapBaseType;
import org.us_ignite.thingworx.dap.DapDataShape;
import org.us_ignite.thingworx.dap.DapField;

@DapDataShape(
        name = "GIT.ExampleCommit.DataShape",
        description = "Example commit returned by a service.",
        serviceResultDescription = "Example commit service result.")
public final class ExampleCommitSpec {
    @DapField(name = "CommitID", baseType = DapBaseType.STRING, ordinal = 1)
    private String commitId;

    @DapField(name = "Message", baseType = DapBaseType.STRING, ordinal = 2)
    private String message;
}
