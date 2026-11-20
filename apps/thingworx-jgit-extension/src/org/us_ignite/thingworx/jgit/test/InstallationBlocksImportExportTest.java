package org.us_ignite.thingworx.jgit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.us_ignite.thingworx.jgit.test.containers.JGitExtensionTestStack;
import org.us_ignite.thingworx.testcontainers.TestingCredentials;

/**
 * The extension ships its own JGit and XML libraries; installing it must not break the Export and
 * Import that ThingWorx Composer offers, which the extension's own sync never calls.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
public class InstallationBlocksImportExportTest {

    private static final String TEST_PROJECT = "IBIET_TestProject";
    private static final String TEST_THING = "IBIET.TestThing";
    private static final String TEST_THING_IMPORTED = "IBIET.TestThing.Imported";
    private static final String IMPORTER =
            "/Thingworx/Importer?purpose=import&usedefaultdataprovider=false&WithSubsystems=false";

    private JGitExtensionTestStack stack;
    private String authHeader;

    @BeforeAll
    public void beforeAll() throws Exception {
        TestingCredentials credentials = new TestingCredentials();
        stack = new JGitExtensionTestStack(credentials, false);
        authHeader =
                "Basic "
                        + Base64.getEncoder()
                                .encodeToString(
                                        (credentials.thingworxAdminUser
                                                        + ":"
                                                        + credentials.thingworxAdminPass)
                                                .getBytes());
        JsonObject project = new JsonObject();
        project.addProperty("name", TEST_PROJECT);
        entityService("CreateProject", project);
        JsonObject thing = new JsonObject();
        thing.addProperty("name", TEST_THING);
        thing.addProperty("thingTemplateName", "GenericThing");
        thing.addProperty("projectName", TEST_PROJECT);
        entityService("CreateThing", thing);
    }

    @AfterAll
    public void afterAll() {
        if (stack != null) stack.close();
    }

    @Test
    void composerExportAndImportStillWorkWithTheExtensionInstalled() throws Exception {
        var exported =
                send(
                        request("/Thingworx/Exporter/Things/" + TEST_THING)
                                .header("Accept", "text/xml")
                                .GET());
        assertEquals(200, exported.statusCode(), "Exporter: " + exported.body());
        assertTrue(exported.body().contains("<Thing"), "Exporter should return entity XML");

        String renamed =
                exported
                        .body()
                        .replaceFirst(
                                "name=\"" + TEST_THING + "\"",
                                "name=\"" + TEST_THING_IMPORTED + "\"");
        String boundary = "----ThingWorxBoundary" + System.currentTimeMillis();
        var imported =
                send(
                        request(IMPORTER)
                                .header("Accept", "application/json")
                                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                                .POST(multipartFile(renamed, boundary)));
        assertEquals(200, imported.statusCode(), "Importer: " + imported.body());

        var thing =
                send(
                        request("/Thingworx/Things/" + TEST_THING_IMPORTED)
                                .header("Accept", "application/json")
                                .GET());
        assertEquals(
                200,
                thing.statusCode(),
                "The imported Thing should exist; installing the extension broke ThingWorx"
                        + " entity import: "
                        + thing.body());
    }

    private void entityService(String service, JsonObject body) throws Exception {
        var response =
                send(
                        request("/Thingworx/Resources/EntityServices/Services/" + service)
                                .header("Accept", "application/json")
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(body.toString())));
        assertTrue(
                response.statusCode() == 200 || response.statusCode() == 201,
                service + ": " + response.statusCode() + " " + response.body());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(stack.thingworx.getExternalUrl() + path))
                .header("Authorization", authHeader)
                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                .header("X-Requested-By", "ThingWorx")
                .timeout(Duration.ofSeconds(60));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return stack.httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpRequest.BodyPublisher multipartFile(String xmlContent, String boundary) {
        var parts = new ArrayList<byte[]>();
        parts.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        parts.add(
                "Content-Disposition: form-data; name=\"file\"; filename=\"entity.xml\"\r\n"
                        .getBytes(StandardCharsets.UTF_8));
        parts.add("Content-Type: text/xml\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        parts.add(xmlContent.getBytes(StandardCharsets.UTF_8));
        parts.add(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArrays(parts);
    }
}
