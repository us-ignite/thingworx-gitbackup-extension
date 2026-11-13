package org.us_ignite.thingworx.jgit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.us_ignite.thingworx.testcontainers.TestImages;
import org.us_ignite.thingworx.testcontainers.TestingCredentials;
import org.us_ignite.thingworx.testcontainers.ThingWorxTestStack;

/**
 * Regenerates the example export (one entity of every creatable ThingWorx entity kind) in the XML
 * format of the ThingWorx version under test. Not a test: it only runs when {@code
 * EXAMPLE_EXPORT_DIR} names the folder to write {@code Example/} into, for example
 *
 * <pre>
 * EXAMPLE_EXPORT_DIR=$PWD/examples/thingworx-jgit-extension-export-9.7 \
 *   ./gradlew :apps:thingworx-jgit-extension:v97:test --tests '*ExampleExportGenerator'
 * </pre>
 *
 * <p>The base entities ({@code Projects/Example.xml} and {@code Things/*.xml}) are imported from
 * the default example export so hand-edited content is kept; every other kind is created through
 * the REST API and then exported with ThingWorx's own source-control exporter. Users and
 * notification contents are not generated: the test licence has no named users, and the platform
 * has no notification handlers without extra extensions.
 */
@EnabledIfEnvironmentVariable(named = "EXAMPLE_EXPORT_DIR", matches = ".+")
class ExampleExportGenerator {

    private static final String PROJECT = "Example";
    private static final String REPOSITORY = "/ThingworxStorage/repository/SystemRepository";
    private static final String BASE_EXPORT = "../../examples/thingworx-jgit-extension-export";

    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final TestingCredentials credentials = new TestingCredentials();
    private ThingWorxTestStack stack;

    @Test
    void generate() throws Exception {
        Path output = Path.of(System.getenv("EXAMPLE_EXPORT_DIR")).toAbsolutePath().normalize();
        try (var started =
                new ThingWorxTestStack(
                        TestImages.dbInitImage(), TestImages.platformImage(), credentials)) {
            stack = started;
            importBaseEntities();
            createEntities();
            export(output.resolve(PROJECT));
        }
    }

    private void importBaseEntities() throws Exception {
        Path base = Path.of(BASE_EXPORT, PROJECT).toAbsolutePath().normalize();
        for (String folder : List.of("Projects", "Things")) {
            try (Stream<Path> files = Files.list(base.resolve(folder))) {
                for (Path file : files.toList()) {
                    // Written through ThingWorx so the repository folders stay owned by the
                    // platform user; a root-owned copy would block the later export.
                    JsonObject save = new JsonObject();
                    save.addProperty(
                            "path", "/seed/" + PROJECT + "/" + folder + "/" + file.getFileName());
                    save.addProperty("content", Files.readString(file, StandardCharsets.UTF_8));
                    service("Things/SystemRepository", "SaveText", save);
                }
            }
        }
        JsonObject body = new JsonObject();
        body.addProperty("repositoryName", "SystemRepository");
        body.addProperty("path", "/seed/" + PROJECT);
        body.addProperty("useDefaultDataProvider", true);
        body.addProperty("withSubsystems", false);
        body.addProperty("overwritePropertyValues", true);
        service("Resources/SourceControlFunctions", "ImportSourceControlledEntities", body);
    }

    private void createEntities() throws Exception {
        entityService(
                "CreateDataShape",
                withProject("Example.DataShape", "Example data shape")
                        .add("fields", dataShapeFields())
                        .json);
        entityService(
                "CreateThingShape", withProject("Example.ThingShape", "Example thing shape").json);
        entityService(
                "CreateThingTemplate",
                withProject("Example.ThingTemplate", "Example thing template")
                        .put("thingTemplateName", "GenericThing")
                        .json);
        entityService(
                "CreateMediaEntity",
                withProject("Example.MediaEntity", "Example media entity")
                        .put(
                                "content",
                                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")
                        .json);
        entityService(
                "CreateStyleDefinition",
                withProject("Example.StyleDefinition", "Example style definition")
                        .add("content", json("{\"content\":" + STYLE + "}"))
                        .json);
        entityService(
                "CreateLocalizationTable",
                withProject("en-x-example", "Example localization table").json);
        entityService(
                "CreateModelTagVocabulary",
                withProject("Example.ModelTagVocabulary", "Example model tag vocabulary").json);
        entityService(
                "CreateDataTagVocabulary",
                withProject("Example.DataTagVocabulary", "Example data tag vocabulary").json);
        entityService("CreateNetwork", withProject("Example.Network", "Example network").json);
        entityService(
                "CreateOrganization",
                withProject("Example.Organization", "Example organization")
                        .put("topOUName", "Example.Organization")
                        .put("topOUDescription", "Top organizational unit")
                        .json);
        entityService("CreateGroup", withProject("Example.Group", "Example user group").json);
        entityService(
                "CreateThingGroup", withProject("Example.ThingGroup", "Example thing group").json);
        entityService(
                "CreateNotificationDefinition",
                withProject("Example.NotificationDefinition", "Example notification definition")
                        .json);
        entityService(
                "CreateApplicationKey",
                withProject("Example.ApplicationKey", "Example application key")
                        .put("user", "Administrator")
                        .put("expirationDate", "2027-11-12T00:00:00.000Z")
                        .json);

        JsonObject mashup =
                withProject("Example.Mashup", "Example mashup")
                        .add(
                                "aspects",
                                json(
                                        "{\"isResponsive\":true,\"isFlex\":\"true\",\"mashupType\":\"mashup\"}"))
                        .add("parameterDefinitions", new JsonObject())
                        .put("mashupContent", MASHUP_CONTENT)
                        .put("columns", 0)
                        .put("rows", 0)
                        .json;
        create("Mashups", mashup);
        create(
                "Menus",
                withProject("Example.Menu", "Example menu")
                        .put("menuLabel", "Example")
                        .put("imageURL", "")
                        .add("groupReferences", new JsonArray())
                        .add("menuItems", JsonParser.parseString(MENU_ITEMS))
                        .json);
        create(
                "StateDefinitions",
                withProject("Example.StateDefinition", "Example state definition")
                        .add("content", json(STATES))
                        .json);

        JsonObject baseTheme = get("StyleThemes/Base%20Theme");
        JsonObject theme =
                withProject("Example.StyleTheme", "Example style theme")
                        .add("content", baseTheme.get("content"))
                        .add("aspects", new JsonObject())
                        .json;
        if (baseTheme.has("preview")) theme.add("preview", baseTheme.get("preview"));
        create("StyleThemes", theme);

        create(
                "Authenticators",
                withProject("Example.Authenticator", "Example authenticator (disabled)")
                        .put(
                                "className",
                                "com.thingworx.security.authentication.ThingworxHttpBasicAuthenticator")
                        .put("priority", 1000)
                        .put("enabled", false)
                        .json);
        create(
                "DirectoryServices",
                withProject("Example.DirectoryService", "Example LDAP directory service (disabled)")
                        .put(
                                "className",
                                "com.thingworx.security.directoryservices.LDAPDirectoryService")
                        .put("priority", 100)
                        .put("enabled", false)
                        .json);
        create(
                "PersistenceProviders",
                withProject(
                                "Example.PersistenceProvider",
                                "Example persistence provider (placeholder connection)")
                        .put("persistenceProviderPackage", influxPackage())
                        .json);
        create(
                "Dashboards",
                withProject("Example.Dashboard", "Example dashboard")
                        .put("dashboardContent", "{\"columns\":2,\"gadgets\":[]}")
                        .json);
    }

    /** InfluxDB 2 support arrived after 9.x; fall back to the InfluxDB 1 package there. */
    private String influxPackage() throws Exception {
        JsonArray rows = get("PersistenceProviderPackages").getAsJsonArray("rows");
        for (var row : rows) {
            String name = row.getAsJsonObject().get("name").getAsString();
            if (name.equals("Influx2PersistenceProviderPackage")) return name;
        }
        return "InfluxPersistenceProviderPackage";
    }

    private void export(Path output) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("repositoryName", "SystemRepository");
        body.addProperty("path", "/export");
        body.addProperty("projectName", PROJECT);
        body.addProperty("includeDependents", false);
        service("Resources/SourceControlFunctions", "ExportSourceControlledEntities", body);

        String exported = REPOSITORY + "/export/" + PROJECT;
        var listing =
                stack.thingworx.execInContainer("find", exported, "-type", "f", "-name", "*.xml");
        assertEquals(0, listing.getExitCode(), "Listing export failed: " + listing.getStderr());
        for (String file : listing.getStdout().split("\n")) {
            if (file.isBlank()) continue;
            Path target = output.resolve(file.substring(exported.length() + 1));
            Files.createDirectories(target.getParent());
            stack.thingworx.copyFileFromContainer(file, target.toString());
            // Same volatile-metadata cleanup the extension applies after every export.
            String xml = Files.readString(target, StandardCharsets.UTF_8);
            xml = xml.replaceAll("\\s+lastModifiedDate=\"[^\"]*\"", "");
            xml = xml.replaceAll("\\s+modelPersistenceProviderPackage=\"[^\"]*\"", "");
            Files.writeString(target, xml, StandardCharsets.UTF_8);
        }
    }

    // ---- REST helpers ----------------------------------------------------------------------

    private static final class Body {
        final JsonObject json = new JsonObject();

        Body put(String key, String value) {
            json.addProperty(key, value);
            return this;
        }

        Body put(String key, Number value) {
            json.addProperty(key, value);
            return this;
        }

        Body put(String key, boolean value) {
            json.addProperty(key, value);
            return this;
        }

        Body add(String key, com.google.gson.JsonElement value) {
            json.add(key, value);
            return this;
        }
    }

    private static Body withProject(String name, String description) {
        Body body = new Body().put("name", name).put("description", description);
        body.json.addProperty("projectName", PROJECT);
        body.json.add("tags", new JsonArray());
        return body;
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private void entityService(String service, JsonObject body) throws Exception {
        service("Resources/EntityServices", service, body);
    }

    private void service(String entity, String service, JsonObject body) throws Exception {
        send("POST", entity + "/Services/" + service, body, service);
    }

    /** Creates an entity by PUT to its collection, for kinds without an EntityServices call. */
    private void create(String collection, JsonObject body) throws Exception {
        send("PUT", collection, body, "Create " + collection);
    }

    private JsonObject get(String path) throws Exception {
        return JsonParser.parseString(send("GET", path, null, "GET " + path)).getAsJsonObject();
    }

    private String send(String method, String path, JsonObject body, String operation)
            throws Exception {
        String basic =
                Base64.getEncoder()
                        .encodeToString(
                                (credentials.thingworxAdminUser
                                                + ":"
                                                + credentials.thingworxAdminPass)
                                        .getBytes(StandardCharsets.UTF_8));
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(stack.thingworx.getExternalUrl() + "/Thingworx/" + path))
                        .timeout(Duration.ofSeconds(60))
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Basic " + basic)
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .method(
                                method,
                                body == null
                                        ? HttpRequest.BodyPublishers.noBody()
                                        : HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertTrue(
                response.statusCode() == 200,
                operation + " failed: " + response.statusCode() + " " + response.body());
        return response.body();
    }

    private static JsonObject dataShapeFields() {
        return json(
                """
                {"dataShape":{"fieldDefinitions":{
                  "name":{"name":"name","baseType":"STRING"},
                  "description":{"name":"description","baseType":"STRING"},
                  "baseType":{"name":"baseType","baseType":"BASETYPENAME"},
                  "ordinal":{"name":"ordinal","baseType":"INTEGER"},
                  "aspects":{"name":"aspects","baseType":"JSON"}}},
                 "rows":[
                  {"name":"id","description":"Row id","baseType":"STRING","ordinal":1,"aspects":{"isPrimaryKey":true}},
                  {"name":"value","description":"Row value","baseType":"NUMBER","ordinal":2,"aspects":{}}]}
                """);
    }

    private static final String STYLE =
            """
            {"image":"","backgroundColor":"#ffffff","secondBackgroundColor":"","foregroundColor":"#000000",
             "textDecoration":"","fontEmphasisBold":false,"fontEmphasisItalic":false,"fontEmphasisUnderline":false,
             "displayString":"","lineThickness":1,"lineStyle":"solid","lineColor":"#000000","fontFamily":"","fontSize":"normal"}
            """;

    private static final String STATES =
            """
            {"stateType":"numeric","stateDefinitions":[
              {"comparator":"<","displayString":"Low","defaultStyleDefinition":"Example.StyleDefinition","defaultValue":10,"name":"Low","description":""},
              {"displayString":"Normal","defaultStyleDefinition":"Example.StyleDefinition","name":"Normal","description":""}]}
            """;

    private static final String MENU_ITEMS =
            """
            [{"linkDestination":"Example.Mashup","isDefault":true,"linkTarget":"Self","imageURL":"",
              "description":"Opens the example mashup","linkType":"Mashup","title":"Example mashup","groupReferences":[]}]
            """;

    /** A responsive mashup whose label shows {@code Example.Thing.exampleProperty}. */
    private static final String MASHUP_CONTENT =
            """
            {
              "CustomMashupCss": "",
              "Data": {"Example.Thing": {"DataName": "Example.Thing", "EntityName": "Example.Thing", "EntityType": "Things", "Id": "Example.Thing-1",
                "Services": [{"APIMethod": "post", "Characteristic": "Services", "Id": "GetProperties-1", "Name": "GetProperties", "Parameters": {}, "RefreshInterval": 0, "Target": "GetProperties"}]}},
              "DataBindings": [{"Id": "b1a6f0e2-0c5e-4a5e-9a51-6d1c7f2e9a01",
                "PropertyMaps": [{"SourceProperty": "exampleProperty", "SourcePropertyBaseType": "STRING", "SourcePropertyType": "Field", "TargetProperty": "LabelText", "TargetPropertyBaseType": "STRING", "TargetPropertyType": "property"}],
                "SourceArea": "Data", "SourceDetails": "AllData", "SourceId": "GetProperties-1", "SourceSection": "Example.Thing", "TargetArea": "UI", "TargetId": "ptcslabel-3", "TargetSection": ""}],
              "DesignTimePermissions": {},
              "RunTimePermissions": {},
              "Events": [{"EventHandlerArea": "Data", "EventHandlerId": "Example.Thing-1", "EventHandlerService": "GetProperties", "EventTriggerArea": "Mashup", "EventTriggerEvent": "Loaded", "EventTriggerId": "mashup-root", "EventTriggerSection": "", "Id": "e3c2d1a4-6b7f-4f0a-8c1d-2b9e5f4a7c02"}],
              "UI": {"Properties": {"Area": "Mashup", "Columns": 0, "DisplayName": "Mashup", "Height": 618, "Id": "mashup-root", "Left": 0, "MinHeight": 0, "MinWidth": 0, "ResponsiveLayout": true, "Rows": 0, "ShowDataLoading": true, "Style": "DefaultMashupStyle", "StyleTheme": "PTC Convergence Theme", "Title": "", "Top": 0, "Type": "mashup", "UseMasterTheme": false, "UseTheme": false, "Visible": true, "Width": 1024, "Z-index": 10, "__TypeDisplayName": "Mashup", "_currentParameterDefs": [], "id_index": 3, "supportsAutoResize": true},
                "Widgets": [{"Properties": {"Area": "UI", "DisplayName": "container-2", "Id": "flexcontainer-2", "LastContainer": true, "ResponsiveLayout": true, "ShowDataLoading": true, "Style": "DefaultContainerStyle", "Type": "flexcontainer", "UseTheme": true, "Visible": true, "Z-index": 10, "__TypeDisplayName": "Responsive Container", "align-content": "flex-start", "align-items": "flex-start", "flex-basis": "auto", "flex-direction": "column", "flex-grow": 1, "flex-shrink": 1, "flex-size": "default", "flex-wrap": "nowrap", "justify-content": "flex-start", "positioning": "responsive", "userCannotRemove": true},
                  "Widgets": [{"Properties": {"Alignment": "left", "Area": "UI", "DisplayName": "Label-3", "Id": "ptcslabel-3", "LabelText": "Example mashup", "LabelType": "label", "Margin": "5", "MaxWidth": "", "ShowDataLoading": true, "TabSequence": 0, "Type": "ptcslabel", "Visible": true, "Z-index": 10, "__TypeDisplayName": "Label", "flex-grow": 0, "flex-shrink": 1, "flex-size": "auto", "positioning": "responsive"}, "Widgets": []}]}]}
            }
            """;
}
