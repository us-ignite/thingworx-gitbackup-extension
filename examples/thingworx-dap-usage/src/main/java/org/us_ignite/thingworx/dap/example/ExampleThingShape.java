package org.us_ignite.thingworx.dap.example;

import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.types.InfoTable;
import org.us_ignite.thingworx.dap.DapServicePayload;
import org.us_ignite.thingworx.dap.DapThingShape;
import org.us_ignite.thingworx.dap.runtime.DapResults;

@DapThingShape(
        name = "GIT.Example.ThingShape",
        projectName = "GIT",
        description = "Example Java-backed ThingShape.")
public final class ExampleThingShape {
    @DapServicePayload(ExampleCommitSpec.class)
    @ThingworxServiceDefinition(
            name = "ListCommits",
            description = "Lists example commits.",
            category = "",
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"dataShape:GIT.ExampleCommit.ServiceResult.DataShape"})
    public InfoTable listCommits() {
        try {
            return DapResults.success(
                    "ListCommits", ExampleCommitTable.SERVICE_RESULT, ExampleCommitTable.create());
        } catch (Exception failure) {
            return DapResults.failure("ListCommits", failure, ExampleCommitTable.SERVICE_RESULT);
        }
    }
}
