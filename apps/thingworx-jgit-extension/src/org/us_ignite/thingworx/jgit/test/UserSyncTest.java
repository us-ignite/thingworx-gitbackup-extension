package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class UserSyncTest {
    @TempDir Path directory;

    /** GitPassphrase stands in for a scalar PASSWORD extension property. */
    private static final Predicate<String> PASSWORD_PROPERTIES = "GitPassphrase"::equals;

    private static final String KEYS_SHAPE =
            "<DataShape><FieldDefinitions>"
                    + "<FieldDefinition aspect.isPrimaryKey=\"true\" baseType=\"STRING\" name=\"SshKeyFingerprint\" ordinal=\"1\"/>"
                    + "<FieldDefinition baseType=\"PASSWORD\" name=\"SshPrivateKey\" ordinal=\"2\"/>"
                    + "<FieldDefinition baseType=\"STRING\" name=\"SshKeyLabel\" ordinal=\"3\"/>"
                    + "</FieldDefinitions></DataShape>";

    private static String keyRow(String fingerprint, String privateKey, String label) {
        return "<Row><SshKeyFingerprint><![CDATA["
                + fingerprint
                + "]]></SshKeyFingerprint>"
                + (privateKey.isEmpty()
                        ? "<SshPrivateKey/>"
                        : "<SshPrivateKey><![CDATA[" + privateKey + "]]></SshPrivateKey>")
                + "<SshKeyLabel><![CDATA["
                + label
                + "]]></SshKeyLabel></Row>";
    }

    private static String extension(String name, String value) {
        return "<Row><lastUpdateTime>1970-01-01T00:00:00.000Z</lastUpdateTime><name><![CDATA["
                + name
                + "]]></name>"
                + (value.isEmpty() ? "<value/>" : "<value>" + value + "</value>")
                + "</Row>";
    }

    private static String extensions(String... rows) {
        return "<ConfigurationTables><ConfigurationTable name=\"UserExtensions\" isMultiRow=\"true\">"
                + "<DataShape><FieldDefinitions>"
                + "<FieldDefinition aspect.isPrimaryKey=\"true\" baseType=\"STRING\" name=\"name\"/>"
                + "<FieldDefinition baseType=\"STRING\" name=\"value\"/>"
                + "</FieldDefinitions></DataShape><Rows>"
                + String.join("", rows)
                + "</Rows></ConfigurationTable></ConfigurationTables>";
    }

    /** As the source-control export writes it: no hash, PASSWORD values blank. */
    private static final String GIT_USER =
            "<Entities majorVersion=\"10\" minorVersion=\"2\" universal=\"password\"><Users>"
                    + "<User name=\"alice\" description=\"from git\" enabled=\"true\" locked=\"false\""
                    + " lockedTime=\"1970-01-01T00:00:00.000Z\" projectName=\"Example\">"
                    + extensions(
                            extension("firstName", "<![CDATA[Alice]]>"),
                            extension("GitPassphrase", ""),
                            extension(
                                    "UserSshKeys",
                                    "<infoTable>"
                                            + KEYS_SHAPE
                                            + "<Rows>"
                                            + keyRow("SHA256:shared", "", "renamed in git")
                                            + keyRow("SHA256:gitonly", "", "git only")
                                            + "</Rows></infoTable>"))
                    + "</User></Users></Entities>";

    /** As the platform serializes the live user: hash, state and encrypted values included. */
    private static final String LIVE_USER =
            "<User name=\"alice\" description=\"live\" enabled=\"false\" locked=\"true\""
                    + " lockedTime=\"2026-11-01T00:00:00.000Z\" passwordHash=\"100000:abc:def\""
                    + " passwordHashAlgorithm=\"PBKDF2WithHmacSHA512\" passwordHashIterationCount=\"100000\""
                    + " passwordHashSaltSizeInBytes=\"64\" passwordHashSizeInBytes=\"64\" projectName=\"Example\">"
                    + extensions(
                            extension("firstName", "<![CDATA[Live]]>"),
                            extension("GitPassphrase", "<![CDATA[c2FsdA==:cGFzcw==]]>"),
                            extension("liveOnly", "<![CDATA[kept]]>"),
                            extension(
                                    "UserSshKeys",
                                    "<infoTable>"
                                            + KEYS_SHAPE
                                            + "<Rows>"
                                            + keyRow(
                                                    "SHA256:shared",
                                                    "c2FsdA==:a2V5MQ==",
                                                    "live label")
                                            + keyRow(
                                                    "SHA256:liveonly",
                                                    "c2FsdA==:a2V5Mg==",
                                                    "live only")
                                            + "</Rows></infoTable>"))
                    + "</User>";

    private static Document parse(String xml) throws Exception {
        return SafeXml.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String text(Document document, String xpath) throws Exception {
        return XPathFactory.newInstance().newXPath().evaluate(xpath, document);
    }

    private static int count(Document document, String xpath) throws Exception {
        return ((NodeList)
                        XPathFactory.newInstance()
                                .newXPath()
                                .evaluate(xpath, document, XPathConstants.NODESET))
                .getLength();
    }

    private static Element user(Document document) {
        return (Element) document.getElementsByTagName("User").item(0);
    }

    private static String extensionValue(String name) {
        return "//ConfigurationTable[@name='UserExtensions']/Rows/Row[name='" + name + "']/value";
    }

    private static String key(String fingerprint, String field) {
        return extensionValue("UserSshKeys")
                + "/infoTable/Rows/Row[SshKeyFingerprint='"
                + fingerprint
                + "']/"
                + field;
    }

    @Test
    void existingUserKeepsHashStateAndSecretsAndTakesTheRestFromGit() throws Exception {
        UserSync.GitUser git = UserSync.split(GIT_USER).get(0);
        Document staged =
                parse(
                        UserSync.prepareImport(
                                git,
                                new UserSync.LiveUser(LIVE_USER),
                                PASSWORD_PROPERTIES,
                                "unused"));
        Element alice = user(staged);

        assertEquals("", staged.getDocumentElement().getAttribute("universal"));
        assertEquals("from git", alice.getAttribute("description"));
        assertEquals("100000:abc:def", alice.getAttribute("passwordHash"));
        assertEquals("100000", alice.getAttribute("passwordHashIterationCount"));
        assertEquals("false", alice.getAttribute("enabled"));
        assertEquals("true", alice.getAttribute("locked"));
        assertEquals("2026-11-01T00:00:00.000Z", alice.getAttribute("lockedTime"));
        assertFalse(alice.hasAttribute("plainTextPassword"));

        assertEquals("Alice", text(staged, extensionValue("firstName")));
        assertEquals("c2FsdA==:cGFzcw==", text(staged, extensionValue("GitPassphrase")));
        assertEquals("kept", text(staged, extensionValue("liveOnly")));

        assertEquals("c2FsdA==:a2V5MQ==", text(staged, key("SHA256:shared", "SshPrivateKey")));
        assertEquals("renamed in git", text(staged, key("SHA256:shared", "SshKeyLabel")));
        assertEquals("", text(staged, key("SHA256:gitonly", "SshPrivateKey")));
        assertEquals("c2FsdA==:a2V5Mg==", text(staged, key("SHA256:liveonly", "SshPrivateKey")));
        assertEquals(3, count(staged, extensionValue("UserSshKeys") + "/infoTable/Rows/Row"));
    }

    @Test
    void newUserIsCreatedDisabledWithTheGivenPasswordAndNoSecrets() throws Exception {
        UserSync.GitUser git = UserSync.split(GIT_USER).get(0);
        Document staged =
                parse(UserSync.prepareImport(git, null, PASSWORD_PROPERTIES, "Rand0m!Passw0rd"));
        Element alice = user(staged);

        assertEquals("Rand0m!Passw0rd", alice.getAttribute("plainTextPassword"));
        assertEquals("false", alice.getAttribute("enabled"));
        assertEquals("false", alice.getAttribute("locked"));
        assertFalse(alice.hasAttribute("passwordHash"));
        assertFalse(alice.hasAttribute("lockedTime"));
        assertEquals("Alice", text(staged, extensionValue("firstName")));
        assertEquals("", text(staged, key("SHA256:shared", "SshPrivateKey")));
    }

    @Test
    void secretsWrittenIntoGitByHandAreNeverImported() throws Exception {
        String edited =
                GIT_USER.replace(
                                extension("GitPassphrase", ""),
                                extension("GitPassphrase", "<![CDATA[plain text]]>"))
                        .replace(
                                keyRow("SHA256:gitonly", "", "git only"),
                                keyRow("SHA256:gitonly", "-----BEGIN KEY-----", "git only"));
        Document staged =
                parse(
                        UserSync.prepareImport(
                                UserSync.split(edited).get(0), null, PASSWORD_PROPERTIES, "x"));
        assertEquals("", text(staged, extensionValue("GitPassphrase")));
        assertEquals("", text(staged, key("SHA256:gitonly", "SshPrivateKey")));
    }

    @Test
    void tableWithoutPrimaryKeyKeepsTheLiveRows() throws Exception {
        String noKeyShape = KEYS_SHAPE.replace(" aspect.isPrimaryKey=\"true\"", "");
        String git = GIT_USER.replace(KEYS_SHAPE, noKeyShape);
        String live = LIVE_USER.replace(KEYS_SHAPE, noKeyShape);
        Document staged =
                parse(
                        UserSync.prepareImport(
                                UserSync.split(git).get(0),
                                new UserSync.LiveUser(live),
                                PASSWORD_PROPERTIES,
                                "x"));
        assertEquals("live label", text(staged, key("SHA256:shared", "SshKeyLabel")));
        assertEquals(0, count(staged, key("SHA256:gitonly", "SshKeyLabel")));
    }

    @Test
    void exportRemovesHashAndStateAndBlanksEveryPassword() throws Exception {
        String exported =
                "<Entities universal=\"\"><Users>"
                        + LIVE_USER
                        + "</Users><Things><Thing name=\"T\"/></Things></Entities>";
        List<String> names = new ArrayList<>();
        Document sanitized = parse(UserSync.sanitizeExport(exported, PASSWORD_PROPERTIES, names));
        Element alice = user(sanitized);

        assertEquals(List.of("alice"), names);
        for (String attribute : UserSync.PASSWORD_ATTRIBUTES) {
            assertFalse(alice.hasAttribute(attribute), attribute);
        }
        for (String attribute : UserSync.STATE_ATTRIBUTES) {
            assertFalse(alice.hasAttribute(attribute), attribute);
        }
        assertEquals("live", alice.getAttribute("description"));
        assertEquals("Live", text(sanitized, extensionValue("firstName")));
        assertEquals("", text(sanitized, extensionValue("GitPassphrase")));
        assertEquals("", text(sanitized, key("SHA256:shared", "SshPrivateKey")));
        assertEquals("live label", text(sanitized, key("SHA256:shared", "SshKeyLabel")));
        assertEquals(1, count(sanitized, "//Things/Thing"));
    }

    @Test
    void exportInPlaceLeavesFilesWithoutUsersUntouched() throws Exception {
        Path snapshot = directory.resolve("snapshot");
        Path thing = snapshot.resolve("Things/T.xml");
        Path alice = snapshot.resolve("Users/alice.xml");
        Files.createDirectories(thing.getParent());
        Files.createDirectories(alice.getParent());
        String thingXml = "<Entities><Things><Thing name=\"T\"/></Things></Entities>";
        Files.writeString(thing, thingXml);
        Files.writeString(alice, "<Entities><Users>" + LIVE_USER + "</Users></Entities>");

        assertEquals(List.of("alice"), UserSync.sanitizeExportIn(snapshot, PASSWORD_PROPERTIES));
        assertEquals(thingXml, Files.readString(thing));
        assertFalse(Files.readString(alice).contains("passwordHash"));
    }

    @Test
    void collectSplitsUsersFromTrackedFilesOnly() throws Exception {
        Path folder = directory.resolve("project");
        Files.createDirectories(folder.resolve("Users"));
        Files.writeString(
                folder.resolve("Users/all.xml"),
                "<Entities universal=\"password\"><Users><User name=\"a\"/><User name=\"b\"/></Users>"
                        + "<Groups><Group name=\"G\"/></Groups></Entities>");
        Files.writeString(
                folder.resolve("Users/untracked.xml"),
                "<Entities><Users><User name=\"c\"/></Users></Entities>");

        List<UserSync.GitUser> users =
                UserSync.collect(folder, path -> !path.endsWith("untracked.xml"));

        assertEquals(List.of("a", "b"), users.stream().map(UserSync.GitUser::name).toList());
        Document first = parse(users.get(0).document());
        assertEquals("password", first.getDocumentElement().getAttribute("universal"));
        assertEquals(1, count(first, "//User"));
        assertEquals(0, count(first, "//Group"));
    }

    @Test
    void randomPasswordHasEveryCharacterClass() throws Exception {
        String password = UserSync.randomPassword();
        assertEquals(32, password.length());
        assertTrue(password.chars().anyMatch(Character::isUpperCase));
        assertTrue(password.chars().anyMatch(Character::isLowerCase));
        assertTrue(password.chars().anyMatch(Character::isDigit));
        assertTrue(password.chars().anyMatch(c -> !Character.isLetterOrDigit(c)));
        assertNull(UserSync.split("<Entities/>").stream().findFirst().orElse(null));
    }
}
