package org.us_ignite.thingworx.jgit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.us_ignite.thingworx.jgit.test.util.ServiceResultAssertions.assertServiceSuccess;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Duration;
import java.util.Base64;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyEncryptionContext;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.us_ignite.thingworx.jgit.test.containers.JGitExtensionTestStack;
import org.us_ignite.thingworx.testcontainers.TestingCredentials;

/**
 * SSH remotes end to end: per-user keys stored as encrypted ThingWorx values, explicitly approved
 * host keys, and push, pull and fetch against Gitea's SSH server.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class SshTransportTest {
    private static final String THING = "ITSshTransportThing";
    private static final String PROJECT = "SshTransportProject";

    private TestingCredentials credentials;
    private JGitExtensionTestStack stack;
    private String generatedFingerprint;
    private String generatedPublicKey;

    @BeforeAll
    void beforeAll() throws Exception {
        credentials = new TestingCredentials();
        stack = new JGitExtensionTestStack(credentials);
    }

    @AfterAll
    void afterAll() {
        stack.close();
    }

    @Test
    @Order(1)
    void createRepositoryWithSshRemote() throws Exception {
        JsonObject project = new JsonObject();
        project.addProperty("name", PROJECT);
        assertEquals(200, callEntityService("CreateProject", project).statusCode());
        JsonObject body = new JsonObject();
        body.addProperty("RepoName", THING);
        body.addProperty(
                "GitRepoURL",
                "ssh://git@gitea:22/"
                        + credentials.giteaUser
                        + "/"
                        + credentials.repoName
                        + ".git");
        body.addProperty("RepoPath", "/" + THING);
        body.addProperty("BranchName", "main");
        body.add("TrackedProjects", trackedProjects(PROJECT));
        body.addProperty("GitCommitterUser", "");
        body.addProperty("GitCommitterPassword", "");
        body.addProperty("GitCommitterEmail", "ssh-test@example.com");
        body.addProperty("GitCommitterFullName", "SSH Test");
        assertServiceSuccess(callUtility("RepositoryCreate", body), "RepositoryCreate");
        Thread.sleep(5000);
    }

    @Test
    @Order(2)
    void fetchWithoutSelectedKeyExplainsWhatToDo() throws Exception {
        var res = callRepository("Fetch", null);
        String message = failureMessage(res, "Fetch without SSH key");
        assertTrue(message.contains("[AUTH]"), "Expected an [AUTH] error: " + message);
        assertTrue(message.contains("no SSH key is selected"), "Unexpected error: " + message);
    }

    @Test
    @Order(3)
    void generatedKeyReturnsOnlyPublicDetails() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("SshKeyLabel", "ssh-it generated");
        var res = callUtility("SshKeyGenerate", body);
        JsonObject row = firstResultRow(assertServiceSuccess(res, "SshKeyGenerate"));
        generatedFingerprint = row.get("SshKeyFingerprint").getAsString();
        generatedPublicKey = row.get("SshPublicKey").getAsString();
        assertTrue(generatedFingerprint.startsWith("SHA256:"), "Fingerprint: " + row);
        assertTrue(
                generatedPublicKey.startsWith("ssh-ed25519 ")
                        || generatedPublicKey.startsWith("ecdsa-sha2-nistp256 "),
                "Public key: " + generatedPublicKey);
        assertFalse(res.body().contains("PRIVATE KEY"), "Private key leaked: " + res.body());

        var list = callUtility("SshKeyListSummary", new JsonObject());
        assertServiceSuccess(list, "SshKeyListSummary");
        assertTrue(list.body().contains(generatedFingerprint), list.body());
        assertFalse(list.body().contains("PRIVATE KEY"), "Private key leaked: " + list.body());
        assertFalse(list.body().contains("SshPrivateKey"), "Secret field returned: " + list.body());
    }

    @Test
    @Order(4)
    void unregisteredKeyIsRejectedByServer() throws Exception {
        approveHostKey();
        selectKey(generatedFingerprint);
        String message = failureMessage(callRepository("Fetch", null), "Fetch unregistered key");
        assertTrue(message.contains("[AUTH]"), "Expected an [AUTH] error: " + message);
        assertTrue(message.contains("rejected the selected key"), "Unexpected error: " + message);
    }

    @Test
    @Order(5)
    void registeredKeyPullsAndPushesOverSsh() throws Exception {
        registerSshKeyWithGitea("ssh-it generated", generatedPublicKey);
        assertServiceSuccess(callRepository("Fetch", null), "Fetch over SSH");
        // The approved host key exists; resetting reports that it did.
        var reset = callRepository("ResetSshHostKeys", null);
        assertTrue(
                assertServiceSuccess(reset, "ResetSshHostKeys")
                        .get("Message")
                        .getAsString()
                        .contains("were reset"),
                "The approved host key should exist: " + reset.body());
        String afterReset = failureMessage(callRepository("Fetch", null), "Fetch after reset");
        assertTrue(afterReset.contains("host key"), "Unexpected error: " + afterReset);
        approveHostKey();
        assertServiceSuccess(callRepository("Fetch", null), "Fetch after approving the host again");

        JsonObject pull = new JsonObject();
        pull.addProperty("Force", false);
        assertServiceSuccess(callRepository("Pull", pull), "Pull over SSH");

        saveText("/" + THING + "/ssh-test.txt", "pushed over SSH");
        JsonObject add = new JsonObject();
        add.addProperty("File", THING + "/ssh-test.txt");
        add.addProperty("All", false);
        assertServiceSuccess(callRepository("Add", add), "Add");
        JsonObject commit = new JsonObject();
        commit.addProperty("Message", "SSH transport test");
        assertServiceSuccess(callRepository("Commit", commit), "Commit");
        assertServiceSuccess(callRepository("Push", null), "Push over SSH");

        var file =
                giteaApi(
                        "GET",
                        "/api/v1/repos/"
                                + credentials.giteaUser
                                + "/"
                                + credentials.repoName
                                + "/contents/"
                                + THING
                                + "/ssh-test.txt",
                        null);
        assertEquals(200, file.statusCode(), "Pushed file missing on Gitea: " + file.body());
    }

    @Test
    @Order(6)
    void resetHostKeysRequiresApprovalAgain() throws Exception {
        var reset = callRepository("ResetSshHostKeys", null);
        assertTrue(
                assertServiceSuccess(reset, "ResetSshHostKeys")
                        .get("Message")
                        .getAsString()
                        .contains("were reset"),
                reset.body());
        String message =
                failureMessage(callRepository("Fetch", null), "Fetch after host key reset");
        assertTrue(message.contains("[AUTH]"), "Expected an [AUTH] error: " + message);
        assertTrue(message.contains("host key"), "Unexpected error: " + message);
        approveHostKey();
        assertServiceSuccess(callRepository("Fetch", null), "Fetch after host key approval");
    }

    @Test
    @Order(7)
    void repositorySettingsNeverReturnSecrets() throws Exception {
        JsonObject query = new JsonObject();
        query.addProperty("GitThing", THING);
        var res = callUtility("GitCredentialGet", query);
        JsonObject row = firstResultRow(assertServiceSuccess(res, "GitCredentialGet"));
        assertEquals(generatedFingerprint, row.get("SshKeyFingerprint").getAsString());
        assertFalse(row.get("HasPassword").getAsBoolean(), "No password was saved: " + row);
        assertFalse(
                res.body().contains("GitCommitterPassword"), "Password returned: " + res.body());

        var list = callUtility("GitCredentialList", new JsonObject());
        assertServiceSuccess(list, "GitCredentialList");
        assertFalse(list.body().contains("GitCommitterPassword"), list.body());
    }

    @Test
    @Order(8)
    void importedEncryptedKeyNeedsItsPassphrase() throws Exception {
        KeyPair keyPair = KeyUtils.generateKeyPair(KeyPairProvider.ECDSA_SHA2_NISTP256, 256);
        OpenSSHKeyEncryptionContext encryption = new OpenSSHKeyEncryptionContext();
        encryption.setPassword("correct horse");
        encryption.setCipherName("AES");
        encryption.setCipherType("256");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OpenSSHKeyPairResourceWriter.INSTANCE.writePrivateKey(keyPair, "imported", encryption, out);
        String privateKey = out.toString(StandardCharsets.UTF_8);

        JsonObject wrong = new JsonObject();
        wrong.addProperty("SshPrivateKey", privateKey);
        wrong.addProperty("SshKeyPassphrase", "wrong passphrase");
        wrong.addProperty("SshKeyLabel", "imported");
        failureMessage(callUtility("SshKeyImport", wrong), "SshKeyImport wrong passphrase");

        JsonObject right = new JsonObject();
        right.addProperty("SshPrivateKey", privateKey);
        right.addProperty("SshKeyPassphrase", "correct horse");
        right.addProperty("SshKeyLabel", "imported");
        JsonObject row =
                firstResultRow(
                        assertServiceSuccess(callUtility("SshKeyImport", right), "SshKeyImport"));
        String fingerprint = row.get("SshKeyFingerprint").getAsString();
        assertEquals(KeyUtils.getFingerPrint(keyPair.getPublic()), fingerprint);

        // The imported key unlocks with its stored passphrase when used for a connection.
        registerSshKeyWithGitea("ssh-it imported", row.get("SshPublicKey").getAsString());
        selectKey(fingerprint);
        approveHostKey();
        assertServiceSuccess(callRepository("Fetch", null), "Fetch with imported key");

        JsonObject delete = new JsonObject();
        delete.addProperty("SshKeyFingerprint", fingerprint);
        assertServiceSuccess(callUtility("SshKeyDelete", delete), "SshKeyDelete");
        JsonObject query = new JsonObject();
        query.addProperty("GitThing", THING);
        JsonObject settings =
                firstResultRow(
                        assertServiceSuccess(
                                callUtility("GitCredentialGet", query), "GitCredentialGet"));
        assertEquals(
                "",
                settings.get("SshKeyFingerprint").getAsString(),
                "Deleting a key should clear it from repositories: " + settings);
    }

    @Test
    @Order(9)
    void remotesAreManagedInGitConfig() throws Exception {
        String sshUrl =
                "ssh://git@gitea:22/" + credentials.giteaUser + "/" + credentials.repoName + ".git";
        var listed = callRepository("GetRemotes", null);
        assertServiceSuccess(listed, "GetRemotes");
        assertTrue(listed.body().contains("\"Name\":\"origin\""), listed.body());
        assertTrue(listed.body().contains(sshUrl), listed.body());

        selectKey(generatedFingerprint);
        var tested = callRepository("TestRemote", null);
        assertTrue(
                assertServiceSuccess(tested, "TestRemote")
                        .get("Message")
                        .getAsString()
                        .contains("Connected to 'origin'"),
                tested.body());

        JsonObject add = new JsonObject();
        add.addProperty("Name", "mirror");
        add.addProperty("URL", "https://git.invalid/team/repo.git");
        assertServiceSuccess(callRepository("RemoteAdd", add), "RemoteAdd");
        assertTrue(
                failureMessage(callRepository("RemoteAdd", add), "RemoteAdd twice")
                        .contains("already exists"));

        JsonObject setUrl = new JsonObject();
        setUrl.addProperty("Name", "mirror");
        setUrl.addProperty("URL", sshUrl);
        assertServiceSuccess(callRepository("RemoteSetUrl", setUrl), "RemoteSetUrl");
        JsonObject mirror = new JsonObject();
        mirror.addProperty("Remote", "mirror");
        assertServiceSuccess(callRepository("Fetch", mirror), "Fetch mirror");
        assertServiceSuccess(callRepository("Pull", mirror), "Pull mirror");

        JsonObject remove = new JsonObject();
        remove.addProperty("Name", "mirror");
        assertServiceSuccess(callRepository("RemoteRemove", remove), "RemoteRemove");
        String afterRemove = callRepository("GetRemotes", null).body();
        assertFalse(afterRemove.contains("mirror"), afterRemove);
        assertTrue(
                failureMessage(callRepository("Fetch", mirror), "Fetch removed remote")
                        .contains("is not configured"));
    }

    @Test
    @Order(10)
    void unsafeRemoteUrlsAreRefused() throws Exception {
        for (String url :
                new String[] {
                    "/tmp/repo.git",
                    "file:///tmp/repo.git",
                    "git://gitea/repo.git",
                    "https://user:secret@gitea/repo.git"
                }) {
            JsonObject add = new JsonObject();
            add.addProperty("Name", "unsafe");
            add.addProperty("URL", url);
            failureMessage(callRepository("RemoteAdd", add), "RemoteAdd " + url);
        }
    }

    @Test
    @Order(11)
    void commitIdentityIsStoredOncePerUser() throws Exception {
        // RepositoryCreate was given a name and email; they belong to the user, not the repository.
        var loaded = callUtility("GitIdentityGet", new JsonObject());
        JsonObject identity = firstResultRow(assertServiceSuccess(loaded, "GitIdentityGet"));
        assertEquals("SSH Test", identity.get("GitUserName").getAsString(), loaded.body());
        assertEquals("ssh-test@example.com", identity.get("GitUserEmail").getAsString());

        JsonObject invalid = new JsonObject();
        invalid.addProperty("GitUserName", "Ada Lovelace");
        invalid.addProperty("GitUserEmail", "not-an-email");
        failureMessage(callUtility("GitIdentitySave", invalid), "GitIdentitySave invalid email");

        JsonObject save = new JsonObject();
        save.addProperty("GitUserName", "Ada Lovelace");
        save.addProperty("GitUserEmail", "ada@example.com");
        assertServiceSuccess(callUtility("GitIdentitySave", save), "GitIdentitySave");

        // Saving repository settings with blank identity fields leaves the identity alone, and
        // every repository reports the user's one identity.
        JsonObject settings = new JsonObject();
        settings.addProperty("GitThing", THING);
        settings.addProperty("GitCommitterUser", "");
        settings.addProperty("GitCommitterPassword", "");
        settings.addProperty("GitCommitterEmail", "");
        settings.addProperty("GitCommitterFullName", "");
        settings.addProperty("GpgKeyFingerprint", "");
        assertServiceSuccess(
                callUtility("GitRepositorySettingsSave", settings), "GitRepositorySettingsSave");
        JsonObject query = new JsonObject();
        query.addProperty("GitThing", THING);
        String summary = callUtility("GitCredentialGet", query).body();
        assertTrue(summary.contains("Ada Lovelace"), summary);
        assertTrue(summary.contains("ada@example.com"), summary);
    }

    @Test
    @Order(12)
    void eachRemoteHasItsOwnSignIn() throws Exception {
        String sshUrl =
                "ssh://git@gitea:22/" + credentials.giteaUser + "/" + credentials.repoName + ".git";
        // No repository-wide key: origin can only sign in with a key of its own.
        selectKey("");
        JsonObject origin = new JsonObject();
        origin.addProperty("Remote", "origin");
        String message = failureMessage(callRepository("Fetch", origin), "Fetch without key");
        assertTrue(message.contains("no SSH key is selected"), "Unexpected error: " + message);

        JsonObject before = signIn("origin");
        assertTrue(before.get("Inherited").getAsBoolean(), before.toString());

        saveSignIn("origin", generatedFingerprint);
        JsonObject saved = signIn("origin");
        assertFalse(saved.get("Inherited").getAsBoolean(), saved.toString());
        assertEquals(generatedFingerprint, saved.get("SshKeyFingerprint").getAsString());
        assertServiceSuccess(callRepository("Fetch", origin), "Fetch with the remote's own key");

        // A second remote does not inherit origin's sign-in.
        JsonObject add = new JsonObject();
        add.addProperty("Name", "second");
        add.addProperty("URL", sshUrl);
        assertServiceSuccess(callRepository("RemoteAdd", add), "RemoteAdd second");
        JsonObject second = new JsonObject();
        second.addProperty("Remote", "second");
        message = failureMessage(callRepository("Fetch", second), "Fetch second without key");
        assertTrue(message.contains("no SSH key is selected"), "Unexpected error: " + message);
        saveSignIn("second", generatedFingerprint);
        assertServiceSuccess(callRepository("Fetch", second), "Fetch second with its key");

        // Removing a remote deletes the sign-in saved for it, so a new remote with the same
        // name starts without one.
        JsonObject remove = new JsonObject();
        remove.addProperty("Name", "second");
        assertServiceSuccess(callRepository("RemoteRemove", remove), "RemoteRemove second");
        assertServiceSuccess(callRepository("RemoteAdd", add), "RemoteAdd second again");
        assertTrue(signIn("second").get("Inherited").getAsBoolean(), "Sign-in survived removal");
        assertServiceSuccess(callRepository("RemoteRemove", remove), "RemoteRemove second");

        // Clearing origin's own sign-in falls back to the repository-wide one.
        assertServiceSuccess(callRepository("RemoteSignInClear", origin), "RemoteSignInClear");
        message = failureMessage(callRepository("Fetch", origin), "Fetch after clearing");
        assertTrue(message.contains("no SSH key is selected"), "Unexpected error: " + message);
        selectKey(generatedFingerprint);
        assertServiceSuccess(callRepository("Fetch", origin), "Fetch with repository-wide key");
    }

    @Test
    @Order(13)
    void upgradeGivesEachRemoteTheRepositoryWideSignIn() throws Exception {
        // After the previous test, origin uses the repository-wide key.
        assertTrue(signIn("origin").get("Inherited").getAsBoolean(), "origin has its own sign-in");

        String report =
                assertServiceSuccess(callUtility("RunUpgrade", new JsonObject()), "RunUpgrade")
                        .toString();
        assertTrue(report.contains("sign-in copied"), report);
        assertFalse(report.contains("PRIVATE KEY"), "Secret in report: " + report);

        JsonObject origin = signIn("origin");
        assertFalse(origin.get("Inherited").getAsBoolean(), origin.toString());
        assertEquals(generatedFingerprint, origin.get("SshKeyFingerprint").getAsString());
        JsonObject query = new JsonObject();
        query.addProperty("GitThing", THING);
        JsonObject settings =
                firstResultRow(
                        assertServiceSuccess(
                                callUtility("GitCredentialGet", query), "GitCredentialGet"));
        assertEquals(
                "",
                settings.get("SshKeyFingerprint").getAsString(),
                "The repository-wide key should be removed: " + settings);
        JsonObject remote = new JsonObject();
        remote.addProperty("Remote", "origin");
        assertServiceSuccess(callRepository("Fetch", remote), "Fetch after upgrade");

        // Running it again changes nothing for this repository.
        String again =
                assertServiceSuccess(
                                callUtility("RunUpgrade", new JsonObject()), "RunUpgrade again")
                        .toString();
        assertFalse(again.contains(THING + " origin"), again);
    }

    private void saveSignIn(String remote, String fingerprint) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("Remote", remote);
        body.addProperty("GitCommitterUser", "");
        body.addProperty("GitCommitterPassword", "");
        body.addProperty("SshKeyFingerprint", fingerprint);
        assertServiceSuccess(callRepository("RemoteSignInSave", body), "RemoteSignInSave");
    }

    /** Returns one remote's row from RemoteSignInList, which must never include a password. */
    private JsonObject signIn(String remote) throws Exception {
        var res = callRepository("RemoteSignInList", null);
        JsonObject result = assertServiceSuccess(res, "RemoteSignInList");
        assertFalse(
                res.body().contains("GitCommitterPassword"), "Password returned: " + res.body());
        for (var row : result.getAsJsonObject("Result").getAsJsonArray("rows")) {
            if (remote.equals(row.getAsJsonObject().get("Remote").getAsString())) {
                return row.getAsJsonObject();
            }
        }
        throw new AssertionError("No sign-in row for " + remote + ": " + res.body());
    }

    private void approveHostKey() throws Exception {
        JsonObject probe = new JsonObject();
        probe.addProperty("Remote", "origin");
        JsonObject row =
                firstResultRow(
                        assertServiceSuccess(
                                callRepository("SshHostKeyProbe", probe), "SshHostKeyProbe"));
        assertTrue(row.get("Fingerprint").getAsString().startsWith("SHA256:"), row.toString());
        JsonObject approve = new JsonObject();
        approve.addProperty("Remote", "origin");
        approve.addProperty("Fingerprint", row.get("Fingerprint").getAsString());
        assertServiceSuccess(callRepository("SshHostKeyApprove", approve), "SshHostKeyApprove");
    }

    private void selectKey(String fingerprint) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("GitThing", THING);
        body.addProperty("SshKeyFingerprint", fingerprint);
        assertServiceSuccess(callUtility("SetRepositorySshKey", body), "SetRepositorySshKey");
    }

    private void registerSshKeyWithGitea(String title, String publicKey) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("title", title);
        body.addProperty("key", publicKey);
        var res = giteaApi("POST", "/api/v1/user/keys", body.toString());
        assertEquals(201, res.statusCode(), "Registering SSH key failed: " + res.body());
    }

    private HttpResponse<String> giteaApi(String method, String path, String json)
            throws Exception {
        String auth =
                Base64.getEncoder()
                        .encodeToString(
                                (credentials.giteaUser + ":" + credentials.giteaPass)
                                        .getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        "http://"
                                                + stack.gitea.getHost()
                                                + ":"
                                                + stack.gitea.getMappedPort(3000)
                                                + path))
                        .header("Authorization", "Basic " + auth)
                        .header("Content-Type", "application/json");
        request.method(
                method,
                json == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json));
        return stack.httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String loadText(String path) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("path", path);
        var res = callRepository("LoadText", body);
        if (res.statusCode() != 200) {
            String parent = path.substring(0, path.lastIndexOf('/'));
            JsonObject list = new JsonObject();
            list.addProperty("path", parent.isEmpty() ? "/" : parent);
            list.addProperty("nameMask", "*");
            var listing = callRepository("ListFiles", list);
            JsonObject root = new JsonObject();
            root.addProperty("path", "/");
            root.addProperty("nameMask", "*");
            var rootListing = callRepository("ListDirectories", root);
            throw new AssertionError(
                    "LoadText "
                            + path
                            + " failed: "
                            + res.body()
                            + "\nListFiles "
                            + parent
                            + ": "
                            + listing.body()
                            + "\nListDirectories /: "
                            + rootListing.body());
        }
        // FileRepository's LoadText returns a plain InfoTable with a Content field.
        return JsonParser.parseString(res.body())
                .getAsJsonObject()
                .getAsJsonArray("rows")
                .get(0)
                .getAsJsonObject()
                .get("Content")
                .getAsString();
    }

    private void saveText(String path, String content) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("path", path);
        body.addProperty("content", content);
        var res = callRepository("SaveText", body);
        assertTrue(
                res.statusCode() == 200 || res.statusCode() == 201,
                "SaveText " + path + " failed: " + res.statusCode() + " " + res.body());
    }

    private static String failureMessage(HttpResponse<String> response, String operation) {
        assertEquals(200, response.statusCode(), operation + ": " + response.body());
        JsonObject row =
                JsonParser.parseString(response.body())
                        .getAsJsonObject()
                        .getAsJsonArray("rows")
                        .get(0)
                        .getAsJsonObject();
        assertTrue(row.get("Error").getAsBoolean(), operation + " should fail: " + response.body());
        return row.get("Message").getAsString();
    }

    private static JsonObject firstResultRow(JsonObject serviceRow) {
        return serviceRow.getAsJsonObject("Result").getAsJsonArray("rows").get(0).getAsJsonObject();
    }

    private HttpResponse<String> callRepository(String service, JsonObject body) throws Exception {
        return stack.httpClient.send(
                stack.thingworx
                        .serviceRequest(THING, service, body == null ? null : body.toString())
                        .timeout(Duration.ofSeconds(60))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> callUtility(String service, JsonObject body) throws Exception {
        return stack.httpClient.send(
                stack.thingworx
                        .serviceRequest("GIT.Utility.Thing", service, body.toString())
                        .timeout(Duration.ofSeconds(60))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> callEntityService(String service, JsonObject body)
            throws Exception {
        String auth =
                Base64.getEncoder()
                        .encodeToString(
                                (credentials.thingworxAdminUser
                                                + ":"
                                                + credentials.thingworxAdminPass)
                                        .getBytes(StandardCharsets.UTF_8));
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        stack.thingworx.getExternalUrl()
                                                + "/Thingworx/Resources/EntityServices/Services/"
                                                + service))
                        .header("Authorization", "Basic " + auth)
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();
        return stack.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject trackedProjects(String... projects) {
        JsonObject field = new JsonObject();
        field.addProperty("name", "ProjectName");
        field.addProperty("baseType", "STRING");
        JsonObject fields = new JsonObject();
        fields.add("ProjectName", field);
        JsonObject shape = new JsonObject();
        shape.add("fieldDefinitions", fields);
        JsonArray rows = new JsonArray();
        for (String project : projects) {
            JsonObject row = new JsonObject();
            row.addProperty("ProjectName", project);
            rows.add(row);
        }
        JsonObject table = new JsonObject();
        table.add("dataShape", shape);
        table.add("rows", rows);
        return table;
    }
}
