package org.us_ignite.thingworx.jgit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.us_ignite.thingworx.jgit.test.util.ServiceResultAssertions.assertServiceSuccess;
import static org.us_ignite.thingworx.jgit.test.util.ServiceResultAssertions.responseRows;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.us_ignite.thingworx.jgit.test.containers.JGitExtensionTestStack;
import org.us_ignite.thingworx.testcontainers.TestingCredentials;

/**
 * Integration tests for the permission checks on the repository lifecycle services of {@code
 * GIT.Utility.Thing}: {@code RepositoryDelete} only deletes repository Things the caller may
 * delete, and project names are only accepted when the caller can see and read the project.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
public class ServiceAuthorizationTest {

    private static final String UTILITY = "Things/GIT.Utility.Thing";
    private static final String ENTITY_SERVICES = "Resources/EntityServices";
    private static final String REPO = "ITAuthRepoThing";
    private static final String PLAIN_THING = "ITAuthPlainThing";
    private static final String HIDDEN_PROJECT = "ITAuthHiddenProject";
    private static final String USER = "ITAuthLimitedUser";
    private static final String USER_PASSWORD = "It-Limited-Passw0rd!2026"; // ggignore

    private TestingCredentials credentials;
    private JGitExtensionTestStack stack;
    private String adminAuth;
    private String userAuth;

    @BeforeAll
    public void beforeAll() throws Exception {
        credentials = new TestingCredentials();
        stack = new JGitExtensionTestStack(credentials, false);
        adminAuth = basic(credentials.thingworxAdminUser, credentials.thingworxAdminPass);
        userAuth = basic(USER, USER_PASSWORD);

        JsonObject project = new JsonObject();
        project.addProperty("name", HIDDEN_PROJECT);
        assertEquals(200, call(adminAuth, ENTITY_SERVICES, "CreateProject", project).statusCode());

        JsonObject thing = new JsonObject();
        thing.addProperty("name", PLAIN_THING);
        thing.addProperty("thingTemplateName", "GenericThing");
        thing.addProperty("projectName", HIDDEN_PROJECT);
        assertEquals(200, call(adminAuth, ENTITY_SERVICES, "CreateThing", thing).statusCode());

        JsonObject repo = new JsonObject();
        repo.addProperty("RepoName", REPO);
        repo.addProperty("BranchName", "main");
        assertServiceSuccess(
                call(adminAuth, UTILITY, "RepositoryCreate", repo), "RepositoryCreate as admin");

        // A user who may call the utility services and EntityServices.CreateThing, but has no
        // design-time permissions and no visibility on the project or the repository Thing.
        JsonObject user = new JsonObject();
        user.addProperty("name", USER);
        user.addProperty("password", USER_PASSWORD);
        assertEquals(200, call(adminAuth, ENTITY_SERVICES, "CreateUser", user).statusCode());
        JsonObject member = new JsonObject();
        member.addProperty("name", "Everyone");
        member.addProperty("member", USER);
        member.addProperty("type", "User");
        call(adminAuth, "Organizations/Everyone", "AddMember", member);
        for (String entity : new String[] {UTILITY, ENTITY_SERVICES}) {
            JsonObject visibility = new JsonObject();
            visibility.addProperty("principal", "Everyone");
            visibility.addProperty("principalType", "Organization");
            call(adminAuth, entity, "AddVisibilityPermission", visibility);
            JsonObject invoke = new JsonObject();
            invoke.addProperty("principal", USER);
            invoke.addProperty("principalType", "User");
            invoke.addProperty("type", "ServiceInvoke");
            invoke.addProperty("resource", "*");
            invoke.addProperty("allow", true);
            call(adminAuth, entity, "AddRunTimePermission", invoke);
        }
    }

    @AfterAll
    public void afterAll() {
        stack.close();
    }

    @Test
    void repositoryDeleteRejectsBlankName() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("RepoName", " ");
        assertFailure(call(adminAuth, UTILITY, "RepositoryDelete", body), "RepoName is required");
    }

    @Test
    void repositoryDeleteRejectsMissingThing() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("RepoName", "ITAuthNoSuchThing");
        assertFailure(call(adminAuth, UTILITY, "RepositoryDelete", body), "was not found");
    }

    @Test
    void repositoryDeleteRefusesThingThatIsNotARepository() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("RepoName", PLAIN_THING);
        assertFailure(
                call(adminAuth, UTILITY, "RepositoryDelete", body), "is not a Git repository");
        assertTrue(thingExists(PLAIN_THING), "The non-repository Thing must not be deleted");
        // The utility Thing itself must be refused as well, even for an administrator.
        body.addProperty("RepoName", "GIT.Utility.Thing");
        assertFailure(
                call(adminAuth, UTILITY, "RepositoryDelete", body), "is not a Git repository");
        assertTrue(thingExists("GIT.Utility.Thing"), "The utility Thing must not be deleted");
    }

    @Test
    void repositoryDeleteRefusesCallerWithoutDeletePermission() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("RepoName", REPO);
        var res = call(userAuth, UTILITY, "RepositoryDelete", body);
        assertTrue(thingExists(REPO), "The repository must survive: " + res.body());
        assumeLimitedUserReachedService(res);
        assertFailure(res, null);
        // Other Things are refused for the limited user too.
        body.addProperty("RepoName", PLAIN_THING);
        assertFailure(call(userAuth, UTILITY, "RepositoryDelete", body), null);
        assertTrue(thingExists(PLAIN_THING), "The non-repository Thing must not be deleted");
    }

    @Test
    void initUserExtensionPropertiesRequiresAdministrator() throws Exception {
        var res = call(userAuth, UTILITY, "InitUserExtensionProperties", null);
        assumeLimitedUserReachedService(res);
        assertFailure(res, null);
    }

    @Test
    void extensionLogRequiresAdministrator() throws Exception {
        var res = call(userAuth, UTILITY, "GetExtensionLog", null);
        assumeLimitedUserReachedService(res);
        assertFailure(res, null);
    }

    @Test
    void gitLogOmitsRepositoriesTheCallerCannotRead() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("MaxEntries", 100);
        var res = call(userAuth, UTILITY, "GetGitLog", body);
        assumeLimitedUserReachedService(res);
        assertServiceSuccess(res, "GetGitLog as limited user");
        assertFalse(
                res.body().contains(REPO),
                "A repository the caller cannot read must not be disclosed: " + res.body());
    }

    @Test
    void repositoryCreateHidesProjectsTheCallerCannotAccess() throws Exception {
        String hidden = createWithProject("ITAuthCreateHidden", HIDDEN_PROJECT);
        String missing = createWithProject("ITAuthCreateMissing", "ITAuthNoSuchProject");
        // Same message for a hidden project as for one that does not exist.
        assertEquals(
                missing.replace("ITAuthNoSuchProject", HIDDEN_PROJECT),
                hidden,
                "A project the caller cannot access must look like a missing project");
        assertTrue(hidden.contains("not found"), "Expected a not-found message: " + hidden);
        assertFalse(thingExists("ITAuthCreateHidden"), "No repository may be created");
    }

    @Test
    void getTrackableProjectsOmitsProjectsTheCallerCannotAccess() throws Exception {
        var admin = call(adminAuth, UTILITY, "GetTrackableProjects", null);
        assertServiceSuccess(admin, "GetTrackableProjects as admin");
        assertTrue(
                projectNames(admin.body()).contains(HIDDEN_PROJECT),
                "The administrator can track the project: " + admin.body());
        var limited = call(userAuth, UTILITY, "GetTrackableProjects", null);
        assumeLimitedUserReachedService(limited);
        assertServiceSuccess(limited, "GetTrackableProjects as limited user");
        assertFalse(
                projectNames(limited.body()).contains(HIDDEN_PROJECT),
                "A project the caller cannot access must not be listed: " + limited.body());
    }

    /** Calls RepositoryCreate as the limited user and returns the failure message. */
    private String createWithProject(String repoName, String projectName) throws Exception {
        JsonObject field = new JsonObject();
        field.addProperty("name", "ProjectName");
        field.addProperty("baseType", "STRING");
        JsonObject fields = new JsonObject();
        fields.add("ProjectName", field);
        JsonObject shape = new JsonObject();
        shape.add("fieldDefinitions", fields);
        JsonObject row = new JsonObject();
        row.addProperty("ProjectName", projectName);
        JsonArray rows = new JsonArray();
        rows.add(row);
        JsonObject table = new JsonObject();
        table.add("dataShape", shape);
        table.add("rows", rows);
        JsonObject body = new JsonObject();
        body.addProperty("RepoName", repoName);
        body.addProperty("BranchName", "main");
        body.add("TrackedProjects", table);
        var res = call(userAuth, UTILITY, "RepositoryCreate", body);
        assumeLimitedUserReachedService(res);
        return assertFailure(res, null);
    }

    /**
     * Skips the test when the limited user could not invoke the service at all, which means the
     * permission fixture in {@link #beforeAll()} needs adjusting for this platform version rather
     * than that the extension's own check ran.
     */
    private static void assumeLimitedUserReachedService(HttpResponse<String> res) {
        assumeTrue(
                res.statusCode() == 200,
                "Limited user could not invoke the service: " + res.statusCode());
    }

    /** Asserts the ServiceResult envelope reports an error and returns its message. */
    private static String assertFailure(HttpResponse<String> res, String expectedMessagePart) {
        assertEquals(200, res.statusCode(), "Transport failed: " + res.body());
        JsonObject row =
                JsonParser.parseString(res.body())
                        .getAsJsonObject()
                        .getAsJsonArray("rows")
                        .get(0)
                        .getAsJsonObject();
        assertTrue(row.get("Error").getAsBoolean(), "Expected Error=true: " + res.body());
        String message = row.has("Message") ? row.get("Message").getAsString() : "";
        if (expectedMessagePart != null)
            assertTrue(
                    message.contains(expectedMessagePart),
                    "Expected '" + expectedMessagePart + "' in: " + res.body());
        return message;
    }

    private static List<String> projectNames(String body) {
        List<String> names = new ArrayList<>();
        responseRows(body)
                .forEach(r -> names.add(r.getAsJsonObject().get("ProjectName").getAsString()));
        return names;
    }

    private boolean thingExists(String name) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        stack.thingworx.getExternalUrl()
                                                + "/Thingworx/Things/"
                                                + name))
                        .header("Authorization", adminAuth)
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();
        return stack.httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()
                == 200;
    }

    private HttpResponse<String> call(String auth, String entity, String service, JsonObject body)
            throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        stack.thingworx.getExternalUrl()
                                                + "/Thingworx/"
                                                + entity
                                                + "/Services/"
                                                + service))
                        .header("Authorization", auth)
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(120))
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        body == null ? "{}" : body.toString()))
                        .build();
        return stack.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String basic(String user, String password) {
        return "Basic "
                + Base64.getEncoder()
                        .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }
}
