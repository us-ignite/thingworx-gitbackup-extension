package org.us_ignite.thingworx.jgit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.us_ignite.thingworx.jgit.test.util.ServiceResultAssertions.assertServiceSuccess;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.us_ignite.thingworx.jgit.test.containers.JGitExtensionTestStack;
import org.us_ignite.thingworx.testcontainers.TestImages;
import org.us_ignite.thingworx.testcontainers.TestingCredentials;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Pulls the example export repository, which holds one entity of every ThingWorx entity kind, into
 * a fresh ThingWorx and checks that each kind was imported. Every kind (top-level folder of the
 * export, such as {@code Things} or {@code Mashups}) is reported as its own test.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EntityKindSyncTest {

    private static final String PROJECT = "Example";
    private static final String GIT_THING = "EntityKindSyncRepo";
    private static final String REPO_PATH = "shared";

    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final TestingCredentials credentials = new TestingCredentials();
    private JGitExtensionTestStack stack;

    /** Collection name (e.g. "Things") to the entities the export declares in it. */
    private final Map<String, List<ExpectedEntity>> expectedByKind = new TreeMap<>();

    private record ExpectedEntity(String file, String name, String description) {}

    @BeforeAll
    void setup(@TempDir Path clone) throws Exception {
        Path exampleProject = exampleProjectFolder();
        readExpectedEntities(exampleProject);

        stack = new JGitExtensionTestStack(credentials);
        seedGitea(clone, exampleProject);

        // RepositoryCreate only tracks projects that already exist on the instance.
        HttpResponse<String> project =
                send(entityServiceRequest("CreateProject", "{\"name\":\"" + PROJECT + "\"}"));
        assertEquals(200, project.statusCode(), "CreateProject failed: " + project.body());
        assertServiceSuccess(
                send(
                        stack.thingworx
                                .serviceRequest(
                                        "GIT.Utility.Thing",
                                        "RepositoryCreate",
                                        repositoryCreateBody().toString())
                                .timeout(Duration.ofSeconds(60))
                                .build()),
                "RepositoryCreate");

        JsonObject pull = new JsonObject();
        pull.addProperty("Force", true);
        assertServiceSuccess(
                send(
                        stack.thingworx
                                .serviceRequest(GIT_THING, "Pull", pull.toString())
                                .timeout(Duration.ofMinutes(3))
                                .build()),
                "Pull");
    }

    @AfterAll
    void tearDown() {
        if (stack != null) stack.close();
    }

    @TestFactory
    Stream<DynamicTest> everyEntityKindIsImported() {
        assertFalse(expectedByKind.isEmpty(), "Example export contains no entities");
        return expectedByKind.entrySet().stream()
                .map(
                        kind ->
                                DynamicTest.dynamicTest(
                                        kind.getKey(),
                                        () -> {
                                            for (ExpectedEntity entity : kind.getValue()) {
                                                assertImported(kind.getKey(), entity);
                                            }
                                        }));
    }

    private void assertImported(String collection, ExpectedEntity expected) throws Exception {
        HttpResponse<String> response =
                send(
                        authorized(
                                        URI.create(
                                                stack.thingworx.getExternalUrl()
                                                        + "/Thingworx/"
                                                        + collection
                                                        + "/"
                                                        + URLEncoder.encode(
                                                                        expected.name(),
                                                                        StandardCharsets.UTF_8)
                                                                .replace("+", "%20")))
                                .GET()
                                .build());
        assertEquals(
                200,
                response.statusCode(),
                collection
                        + "/"
                        + expected.name()
                        + " (from "
                        + expected.file()
                        + ") was not imported: "
                        + response.body());
        JsonObject entity = JsonParser.parseString(response.body()).getAsJsonObject();
        assertEquals(expected.name(), entity.get("name").getAsString(), "name");
        assertEquals(
                PROJECT,
                entity.get("projectName").getAsString(),
                collection + "/" + expected.name() + " projectName");
        assertEquals(
                expected.description(),
                entity.get("description").getAsString(),
                collection + "/" + expected.name() + " description");
    }

    /**
     * The example export lives at the repository root, outside this Gradle project. ThingWorx 9.x
     * cannot be assumed to read 10.x exports, so 9.x platforms use the export generated on 9.7 (see
     * {@link ExampleExportGenerator}). {@code test.exampleExport} overrides the choice.
     */
    private static Path exampleProjectFolder() {
        String image = TestImages.platformImage();
        String version = image.substring(image.lastIndexOf(':') + 1);
        String export =
                version.startsWith("9.")
                        ? "../../examples/thingworx-jgit-extension-export-9.7"
                        : "../../examples/thingworx-jgit-extension-export";
        Path folder =
                Path.of(System.getProperty("test.exampleExport", export), PROJECT)
                        .toAbsolutePath()
                        .normalize();
        assertTrue(Files.isDirectory(folder), "Example export not found at " + folder);
        return folder;
    }

    /** Reads each export file's collection element and the entities declared inside it. */
    private void readExpectedEntities(Path exampleProject) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        List<Path> files;
        try (Stream<Path> walk = Files.walk(exampleProject)) {
            files = walk.filter(p -> p.toString().endsWith(".xml")).sorted().toList();
        }
        for (Path file : files) {
            Element root = factory.newDocumentBuilder().parse(file.toFile()).getDocumentElement();
            for (Element collection : childElements(root)) {
                for (Element entity : childElements(collection)) {
                    expectedByKind
                            .computeIfAbsent(collection.getTagName(), k -> new ArrayList<>())
                            .add(
                                    new ExpectedEntity(
                                            exampleProject.relativize(file).toString(),
                                            entity.getAttribute("name"),
                                            entity.getAttribute("description")));
                }
            }
        }
    }

    private static List<Element> childElements(Element parent) {
        List<Element> children = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element element) children.add(element);
        }
        return children;
    }

    /** Commits the example project to {@code <REPO_PATH>/<PROJECT>} on the Gitea main branch. */
    private void seedGitea(Path clone, Path exampleProject) throws Exception {
        String url =
                "http://"
                        + stack.gitea.getHost()
                        + ":"
                        + stack.gitea.getMappedPort(3000)
                        + "/"
                        + credentials.giteaUser
                        + "/"
                        + credentials.repoName
                        + ".git";
        var auth =
                new UsernamePasswordCredentialsProvider(
                        credentials.giteaUser, credentials.giteaPass);
        try (Git git =
                Git.cloneRepository()
                        .setURI(url)
                        .setBranch("main")
                        .setDirectory(clone.toFile())
                        .setCredentialsProvider(auth)
                        .call()) {
            Path target = clone.resolve(REPO_PATH).resolve(PROJECT);
            try (Stream<Path> walk = Files.walk(exampleProject)) {
                for (Path source : walk.toList()) {
                    Path destination = target.resolve(exampleProject.relativize(source).toString());
                    if (Files.isDirectory(source)) Files.createDirectories(destination);
                    else Files.copy(source, destination);
                }
            }
            git.add().addFilepattern(REPO_PATH).call();
            git.commit()
                    .setMessage("Seed example entities")
                    .setAuthor("Entity Kind Sync", "entity-kind-sync@example.com")
                    .setCommitter("Entity Kind Sync", "entity-kind-sync@example.com")
                    .call();
            git.push().setRemote("origin").setCredentialsProvider(auth).call();
        }
    }

    private JsonObject repositoryCreateBody() {
        JsonObject field = new JsonObject();
        field.addProperty("name", "ProjectName");
        field.addProperty("baseType", "STRING");
        JsonObject fields = new JsonObject();
        fields.add("ProjectName", field);
        JsonObject shape = new JsonObject();
        shape.add("fieldDefinitions", fields);
        JsonObject row = new JsonObject();
        row.addProperty("ProjectName", PROJECT);
        JsonArray rows = new JsonArray();
        rows.add(row);
        JsonObject tracked = new JsonObject();
        tracked.add("dataShape", shape);
        tracked.add("rows", rows);

        JsonObject body = new JsonObject();
        body.addProperty("RepoName", GIT_THING);
        body.addProperty(
                "GitRepoURL",
                "http://gitea:3000/" + credentials.giteaUser + "/" + credentials.repoName + ".git");
        body.addProperty("RepoPath", "/" + REPO_PATH);
        body.addProperty("BranchName", "main");
        body.add("TrackedProjects", tracked);
        body.addProperty("GitCommitterUser", credentials.giteaUser);
        body.addProperty("GitCommitterPassword", credentials.giteaPass);
        body.addProperty("GitCommitterEmail", "entity-kind-sync@example.com");
        body.addProperty("GitCommitterFullName", "Entity Kind Sync");
        return body;
    }

    private HttpRequest entityServiceRequest(String service, String body) {
        return authorized(
                        URI.create(
                                stack.thingworx.getExternalUrl()
                                        + "/Thingworx/Resources/EntityServices/Services/"
                                        + service))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private HttpRequest.Builder authorized(URI uri) {
        String basic =
                Base64.getEncoder()
                        .encodeToString(
                                (credentials.thingworxAdminUser
                                                + ":"
                                                + credentials.thingworxAdminPass)
                                        .getBytes(StandardCharsets.UTF_8));
        return HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("Authorization", "Basic " + basic)
                .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                .header("X-Requested-By", "ThingWorx");
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
