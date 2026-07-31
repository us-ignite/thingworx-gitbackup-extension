package org.us_ignite.thingworx.jgit.tests;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class InstallationBlocksImportExportTest {

    private static final String DB_INIT_IMAGE =
            System.getProperty("test.dbInitImage", "devopscadit/postgresql-init-twx:platform9.6.3");
    private static final String PLATFORM_IMAGE =
            System.getProperty("test.platformImage", "devopscadit/platform-postgres:platform9.6.3");

    private static final String TEST_PROJECT = "IBIET_TestProject";
    private static final String TEST_THING = "IBIET.TestThing";

    private TestingCredentials credentials;
    private JGitExtensionTestStack stack;
    private String authHeader;

    @BeforeAll
    public void beforeAll() throws Exception {
        credentials = new TestingCredentials();
        stack = new JGitExtensionTestStack(DB_INIT_IMAGE, PLATFORM_IMAGE, credentials, false);
        authHeader =
                "Basic "
                        + Base64.getEncoder()
                                .encodeToString(
                                        (credentials.thingworxAdminUser
                                                        + ":"
                                                        + credentials.thingworxAdminPass)
                                                .getBytes());
    }

    @AfterAll
    public void afterAll() {
        if (stack != null) stack.close();
    }

    @Test
    @Order(1)
    void extensionIsInstalled() throws Exception {
        var req =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        stack.thingworx.getExternalUrl()
                                                + "/Thingworx/Things/GIT.Utility.Thing"))
                        .header("Accept", "application/json")
                        .header("Authorization", authHeader)
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .GET()
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(),
                "GIT.Utility.Thing must be accessible: " + res.statusCode() + " " + res.body());
    }

    @Test
    @Order(2)
    void sourceControlFunctionsResourceExists() throws Exception {
        var req =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        stack.thingworx.getExternalUrl()
                                                + "/Thingworx/Resources/SourceControlFunctions"))
                        .header("Accept", "application/json")
                        .header("Authorization", authHeader)
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .GET()
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(),
                "SourceControlFunctions resource must be accessible: " + res.statusCode());
    }

    @Test
    @Order(3)
    void entityServicesResourceExists() throws Exception {
        var req =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        stack.thingworx.getExternalUrl()
                                                + "/Thingworx/Resources/EntityServices"))
                        .header("Accept", "application/json")
                        .header("Authorization", authHeader)
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .GET()
                        .timeout(Duration.ofSeconds(30))
                        .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(),
                "EntityServices resource must be accessible: " + res.statusCode());
    }

    @Test
    @Order(4)
    void createProjectViaEntityServices() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("name", TEST_PROJECT);
        body.addProperty("description", "Smoke test project");
        var uri = URI.create(stack.thingworx.getExternalUrl()
                + "/Thingworx/Resources/EntityServices/Services/CreateProject");
        var req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                .header("X-Requested-By", "ThingWorx")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .timeout(Duration.ofSeconds(30))
                .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertTrue(res.statusCode() == 200 || res.statusCode() == 201,
                "CreateProject should work: " + res.statusCode() + " " + res.body());
    }

    @Test
    @Order(5)
    void createThingViaEntityServices() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("name", TEST_THING);
        body.addProperty("description", "Original description");
        body.addProperty("thingTemplateName", "GenericThing");
        body.addProperty("projectName", TEST_PROJECT);
        var uri = URI.create(stack.thingworx.getExternalUrl()
                + "/Thingworx/Resources/EntityServices/Services/CreateThing");
        var req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                .header("X-Requested-By", "ThingWorx")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .timeout(Duration.ofSeconds(30))
                .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertTrue(res.statusCode() == 200 || res.statusCode() == 201,
                "CreateThing should work: " + res.statusCode() + " " + res.body());
    }

    @Test
    @Order(6)
    void exportEntitiesViaSourceControlFunctions() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("repositoryName", "GitRepository");
        body.addProperty("path", "/IBIET_SmokeTest");
        body.addProperty("projectName", TEST_PROJECT);
        body.addProperty("includeDependents", false);
        var uri = URI.create(stack.thingworx.getExternalUrl()
                + "/Thingworx/Resources/SourceControlFunctions/Services/ExportSourceControlledEntities");
        var req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                .header("X-Requested-By", "ThingWorx")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .timeout(Duration.ofSeconds(60))
                .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(),
                "ExportSourceControlledEntities should work. "
                + "Status: " + res.statusCode() + " Body: " + res.body());
    }

    @Test
    @Order(7)
    void importEntitiesViaSourceControlFunctions() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("repositoryName", "GitRepository");
        body.addProperty("path", "/IBIET_SmokeTest/" + TEST_PROJECT + "/Things");
        body.addProperty("useDefaultDataProvider", true);
        body.addProperty("withSubsystems", false);
        body.addProperty("overwritePropertyValues", true);
        var uri = URI.create(stack.thingworx.getExternalUrl()
                + "/Thingworx/Resources/SourceControlFunctions/Services/ImportSourceControlledEntities");
        var req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                .header("X-Requested-By", "ThingWorx")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .timeout(Duration.ofSeconds(60))
                .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(),
                "ImportSourceControlledEntities should work after extension install. "
                + "If this fails, the extension install broke default ThingWorx import. "
                + "Status: " + res.statusCode() + " Body: " + res.body());
    }

    private String exportedEntityXml;

    @Test
    @Order(8)
    void exportEntityToXmlViaExporter() throws Exception {
        var uri = URI.create(stack.thingworx.getExternalUrl()
                + "/Thingworx/Exporter/Things/" + TEST_THING);
        var req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "text/xml")
                .header("Authorization", authHeader)
                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                .header("X-Requested-By", "ThingWorx")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(),
                "GET /Thingworx/Exporter/Things/<name> with Accept: text/xml should return entity XML. "
                + "Status: " + res.statusCode() + " Body: " + res.body());
        assertTrue(res.body().contains("<Thing"),
                "Response should contain entity XML: " + res.body());
        exportedEntityXml = res.body();
    }

    private static final String TEST_THING_IMPORTED = "IBIET.TestThing.Imported";

    @Test
    @Order(9)
    void exportAndReimportByFileRoundTrip() throws Exception {
        var exportUri = URI.create(stack.thingworx.getExternalUrl()
                + "/Thingworx/Exporter/Things/" + TEST_THING);
        var exportReq = HttpRequest.newBuilder()
                .uri(exportUri)
                .header("Accept", "text/xml")
                .header("Authorization", authHeader)
                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                .header("X-Requested-By", "ThingWorx")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();
        var exportRes = stack.httpClient.send(exportReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, exportRes.statusCode(),
                "GET /Thingworx/Exporter/Things/<name> should return XML: " + exportRes.statusCode());
        assertTrue(exportRes.body().contains("<Thing"),
                "Response should contain entity XML");

        String xmlWithNewName = exportRes.body()
                .replaceFirst("name=\"" + TEST_THING + "\"", "name=\"" + TEST_THING_IMPORTED + "\"");

        String boundary = "----ThingWorxBoundary" + System.currentTimeMillis();
        var importUri = URI.create(stack.thingworx.getExternalUrl()
                + "/Thingworx/Importer?purpose=import&usedefaultdataprovider=false&WithSubsystems=false");
        var importReq = HttpRequest.newBuilder()
                .uri(importUri)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                .header("X-Requested-By", "ThingWorx")
                .POST(ofMultipartFileBody(xmlWithNewName, boundary))
                .timeout(Duration.ofSeconds(60))
                .build();
        var importRes = stack.httpClient.send(importReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, importRes.statusCode(),
                "POST /Thingworx/Importer with multipart XML file should work. "
                + "Status: " + importRes.statusCode() + " Body: " + importRes.body());
    }

    @Test
    @Order(10)
    void verifyImportedEntityExists() throws Exception {
        var uri = URI.create(stack.thingworx.getExternalUrl()
                + "/Thingworx/Things/" + TEST_THING_IMPORTED);
        var req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                .header("X-Requested-By", "ThingWorx")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();
        var res = stack.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(),
                "Imported Thing by file should exist. If this fails, the extension install "
                + "broke default ThingWorx entity import. "
                + "Status: " + res.statusCode() + " Body: " + res.body());
    }

    private HttpRequest.BodyPublisher ofMultipartFileBody(String xmlContent, String boundary) {
        var byteArrays = new ArrayList<byte[]>();
        byteArrays.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        byteArrays.add("Content-Disposition: form-data; name=\"file\"; filename=\"entity.xml\"\r\n".getBytes(StandardCharsets.UTF_8));
        byteArrays.add("Content-Type: text/xml\r\n".getBytes(StandardCharsets.UTF_8));
        byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
        byteArrays.add(xmlContent.getBytes(StandardCharsets.UTF_8));
        byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }
}
