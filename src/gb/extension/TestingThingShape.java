package gb.extension;

import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceParameter;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.types.InfoTable;
import org.slf4j.Logger;

public class TestingThingShape {

    private static Logger _logger =
            LogUtilities.getInstance().getApplicationLogger(TestingThingShape.class);

    @ThingworxServiceDefinition(
            name = "CreateTestData",
            description = "",
            category = "",
            isAllowOverride = true,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "JSON",
            aspects = {})
    public String CreateTestData() throws Exception {
        _logger.warn(
                "CreateTestData not yet implemented in Java. Falling back to script if available.");
        return "{}";
    }

    @ThingworxServiceDefinition(
            name = "DeleteTestData",
            description = "",
            category = "",
            isAllowOverride = true,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "JSON",
            aspects = {})
    public String DeleteTestData(
            @ThingworxServiceParameter(name = "InputParams", description = "", baseType = "JSON")
                    String InputParams)
            throws Exception {
        _logger.warn(
                "DeleteTestData not yet implemented in Java. Falling back to script if available.");
        return "{}";
    }

    @ThingworxServiceDefinition(
            name = "ExecuteTest",
            description = "General service for executing a test - needs to be overridden",
            category = "Test",
            isAllowOverride = true,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GitBackup.TestResult"})
    public InfoTable ExecuteTest(
            @ThingworxServiceParameter(name = "TestName", description = "", baseType = "STRING")
                    String TestName,
            @ThingworxServiceParameter(name = "InputParams", description = "", baseType = "JSON")
                    String InputParams,
            @ThingworxServiceParameter(name = "ExpectedOutput", description = "", baseType = "JSON")
                    String ExpectedOutput,
            @ThingworxServiceParameter(
                            name = "PreviousTestResults",
                            description = "",
                            baseType = "INFOTABLE",
                            aspects = {"dataShape:GitBackup.TestResult"})
                    InfoTable PreviousTestResults)
            throws Exception {
        _logger.warn(
                "ExecuteTest not yet implemented in Java. Falling back to script if available.");
        return PreviousTestResults;
    }

    @ThingworxServiceDefinition(
            name = "FormatTestResults",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "HTML",
            aspects = {})
    public String FormatTestResults(
            @ThingworxServiceParameter(
                            name = "tests",
                            description = "",
                            baseType = "INFOTABLE",
                            aspects = {"dataShape:GitBackup.TestResult"})
                    InfoTable tests)
            throws Exception {
        _logger.warn(
                "FormatTestResults not yet implemented in Java. Falling back to script if available.");
        return "<html><body><p>No results available.</p></body></html>";
    }

    @ThingworxServiceDefinition(
            name = "RunAllTests",
            description = "",
            category = "",
            isAllowOverride = true,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GitBackup.TestResult"})
    public InfoTable RunAllTests() throws Exception {
        _logger.warn(
                "RunAllTests not yet implemented in Java. Falling back to script if available.");
        return null;
    }

    @ThingworxServiceDefinition(
            name = "RunAllTestsFormatResult",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "HTML",
            aspects = {})
    public String RunAllTestsFormatResult(
            @ThingworxServiceParameter(
                            name = "publishSolutionIfSuccessful",
                            description = "",
                            baseType = "BOOLEAN")
                    Boolean publishSolutionIfSuccessful,
            @ThingworxServiceParameter(name = "hostName", description = "", baseType = "STRING")
                    String hostName,
            @ThingworxServiceParameter(name = "artifactId", description = "", baseType = "STRING")
                    String artifactId,
            @ThingworxServiceParameter(name = "groupId", description = "", baseType = "STRING")
                    String groupId,
            @ThingworxServiceParameter(
                            name = "packageVersion",
                            description = "",
                            baseType = "STRING")
                    String packageVersion)
            throws Exception {
        _logger.warn(
                "RunAllTestsFormatResult not yet implemented in Java. Falling back to script if available.");
        return "<html><body><p>No results available.</p></body></html>";
    }
}
