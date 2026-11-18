package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.entities.utils.UserUtilities;
import com.thingworx.metadata.PropertyDefinition;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.security.context.SecurityContext;
import com.thingworx.security.users.User;
import com.thingworx.thingshape.ThingShape;
import com.thingworx.types.BaseTypes;
import com.thingworx.webservices.context.ThreadLocalContext;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Synchronizes Users through Git without touching what each server holds for them.
 *
 * <p>A User's design-time configuration (description, tags, home mashups, project, permissions,
 * avatar) and its non-secret extension properties travel through Git. What is <b>server-local</b>
 * never does, and an import never changes it:
 *
 * <ul>
 *   <li>the password hash ({@code passwordHash*}, {@code password}, {@code plainTextPassword});
 *   <li>account state: {@code enabled}, {@code locked} and {@code lockedTime};
 *   <li>every PASSWORD value in the {@code UserExtensions} table, whether the property itself is a
 *       PASSWORD or an INFOTABLE with PASSWORD fields, such as SSH private keys and per-remote
 *       sign-ins.
 * </ul>
 *
 * <p><b>Export</b> ({@link #sanitizeExport}) removes the server-local attributes and blanks every
 * PASSWORD value. <b>Import</b> ({@link #prepareImport}) builds, for one User, the file the
 * platform imports: the Git copy, with the server-local parts taken from the live user. The live
 * PASSWORD values are copied in their server-encrypted form and the file is marked {@code
 * universal=""}, so the platform decrypt-tests them and stores them unchanged; nothing is
 * decrypted. A User that does not exist yet is created disabled, with a random password nobody
 * knows, for an administrator to set a password and enable.
 *
 * <p>Within an INFOTABLE extension property that has PASSWORD fields, rows are matched on the
 * primary key: a matched row keeps its live PASSWORD values, and a live row the Git copy lacks is
 * kept. Without a primary key the rows cannot be matched and the live table is kept as is. An
 * extension property the Git copy does not have keeps its live value.
 */
final class UserSync {
    static final String USERS = SyncExclusions.USERS;
    static final String USER_EXTENSIONS = "UserExtensions";

    /** Root attributes of a User that never leave the server. */
    static final List<String> PASSWORD_ATTRIBUTES =
            List.of(
                    "passwordHash",
                    "passwordHashAlgorithm",
                    "passwordHashSaltSizeInBytes",
                    "passwordHashSizeInBytes",
                    "passwordHashIterationCount");

    static final List<String> STATE_ATTRIBUTES = List.of("enabled", "locked", "lockedTime");

    private static final List<String> PLAIN_PASSWORD_ATTRIBUTES =
            List.of("password", "plainTextPassword");

    private static final SecureRandom RANDOM = new SecureRandom();

    private UserSync() {}

    /** One User from Git, as an {@code <Entities>} document holding only that User. */
    record GitUser(String name, String document) {}

    /**
     * The live user's {@code <User>} element as the platform serializes it for a full export,
     * password hash and server-encrypted PASSWORD values included.
     */
    record LiveUser(String userElement) {}

    // ---------------------------------------------------------------------------------------
    // Export
    // ---------------------------------------------------------------------------------------

    /**
     * Sanitizes every User below {@code folder} in place, see {@link #sanitizeExport(String)}.
     *
     * @return the names of the Users found
     */
    static List<String> sanitizeExportIn(Path folder, Predicate<String> isPasswordProperty)
            throws Exception {
        List<String> names = new ArrayList<>();
        List<File> files = new ArrayList<>();
        FileRepositoryManager.collectXmlFilesOnDisk(folder.toFile(), files);
        for (File file : files) {
            Path path = file.toPath();
            String xml = Files.readString(path, StandardCharsets.UTF_8);
            if (!mayContainUsers(xml)) continue;
            String sanitized = sanitizeExport(xml, isPasswordProperty, names);
            if (!sanitized.equals(xml)) Files.writeString(path, sanitized, StandardCharsets.UTF_8);
        }
        return names;
    }

    /**
     * Returns the XML with the server-local attributes removed from every User and every PASSWORD
     * value in its extension properties blanked, or the text unchanged when it holds no User.
     */
    static String sanitizeExport(
            String xml, Predicate<String> isPasswordProperty, List<String> names) throws Exception {
        Document document = parse(xml);
        boolean found = false;
        for (Element user : users(document)) {
            found = true;
            if (names != null) names.add(user.getAttribute("name"));
            for (String attribute : PASSWORD_ATTRIBUTES) user.removeAttribute(attribute);
            for (String attribute : PLAIN_PASSWORD_ATTRIBUTES) user.removeAttribute(attribute);
            for (String attribute : STATE_ATTRIBUTES) user.removeAttribute(attribute);
            blankPasswords(user, isPasswordProperty);
        }
        return found ? serialize(document) : xml;
    }

    // ---------------------------------------------------------------------------------------
    // Import
    // ---------------------------------------------------------------------------------------

    /**
     * Takes the Users out of the XML files below {@code folder}, one document per User. Files that
     * {@code include} rejects, given their path relative to {@code folder} with {@code /}
     * separators, are skipped.
     */
    static List<GitUser> collect(Path folder, Predicate<String> include) throws Exception {
        List<GitUser> users = new ArrayList<>();
        List<File> files = new ArrayList<>();
        FileRepositoryManager.collectXmlFilesOnDisk(folder.toFile(), files);
        for (File file : files) {
            String relative = folder.relativize(file.toPath()).toString().replace('\\', '/');
            if (!include.test(relative)) continue;
            String xml = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (!mayContainUsers(xml)) continue;
            users.addAll(split(xml));
        }
        return users;
    }

    /** One document per User in {@code xml}, each with the original {@code <Entities>} header. */
    static List<GitUser> split(String xml) throws Exception {
        Document source = parse(xml);
        List<GitUser> result = new ArrayList<>();
        for (Element user : users(source)) {
            Document single = parse(xml);
            Element root = single.getDocumentElement();
            while (root.getFirstChild() != null) root.removeChild(root.getFirstChild());
            Element collection = single.createElement(USERS);
            collection.appendChild(single.importNode(user, true));
            root.appendChild(collection);
            result.add(new GitUser(user.getAttribute("name"), serialize(single)));
        }
        return result;
    }

    /**
     * The document to import for one User: the Git copy with the server-local parts of {@code
     * live}, or, when {@code live} is {@code null}, a disabled new User with a random password.
     */
    static String prepareImport(GitUser user, LiveUser live, Predicate<String> isPasswordProperty)
            throws Exception {
        return prepareImport(user, live, isPasswordProperty, randomPassword());
    }

    static String prepareImport(
            GitUser user, LiveUser live, Predicate<String> isPasswordProperty, String newPassword)
            throws Exception {
        Document document = parse(user.document());
        Element root = document.getDocumentElement();
        // Live PASSWORD values are server-encrypted, never universal-password plain text.
        root.setAttribute("universal", "");
        List<Element> users = users(document);
        if (users.size() != 1) {
            throw new IllegalArgumentException("Expected one User in the document for " + user);
        }
        Element target = users.get(0);
        for (String attribute : PASSWORD_ATTRIBUTES) target.removeAttribute(attribute);
        for (String attribute : PLAIN_PASSWORD_ATTRIBUTES) target.removeAttribute(attribute);
        for (String attribute : STATE_ATTRIBUTES) target.removeAttribute(attribute);
        // Whatever Git holds for a PASSWORD is not a server-encrypted value; never import it.
        blankPasswords(target, isPasswordProperty);

        if (live == null) {
            target.setAttribute("plainTextPassword", newPassword);
            target.setAttribute("enabled", "false");
            target.setAttribute("locked", "false");
            return serialize(document);
        }

        Element liveUser =
                (Element) document.importNode(parse(live.userElement()).getDocumentElement(), true);
        // The platform keeps the live hash for an existing user on 10.x but reads it from the
        // file on 9.x, so the file always carries the live hash.
        for (String attribute : PASSWORD_ATTRIBUTES) copyAttribute(liveUser, target, attribute);
        for (String attribute : STATE_ATTRIBUTES) copyAttribute(liveUser, target, attribute);
        mergeExtensions(target, liveUser, isPasswordProperty);
        return serialize(document);
    }

    private static void copyAttribute(Element from, Element to, String attribute) {
        if (from.hasAttribute(attribute)) to.setAttribute(attribute, from.getAttribute(attribute));
    }

    private static void mergeExtensions(
            Element target, Element live, Predicate<String> isPasswordProperty) {
        Element liveTable = extensionTable(live);
        if (liveTable == null) return;
        Element targetTable = extensionTable(target);
        if (targetTable == null) {
            Element tables = child(target, "ConfigurationTables");
            if (tables == null) {
                tables = target.getOwnerDocument().createElement("ConfigurationTables");
                target.appendChild(tables);
            }
            tables.appendChild(liveTable.cloneNode(true));
            return;
        }
        Element targetRows = child(targetTable, "Rows");
        if (targetRows == null) {
            targetRows = target.getOwnerDocument().createElement("Rows");
            targetTable.appendChild(targetRows);
        }
        Map<String, Element> gitRows = rowsByName(targetRows);
        for (Map.Entry<String, Element> entry : rowsByName(child(liveTable, "Rows")).entrySet()) {
            String name = entry.getKey();
            Element liveRow = entry.getValue();
            Element gitRow = gitRows.get(name);
            if (gitRow == null) {
                targetRows.appendChild(liveRow.cloneNode(true));
            } else if (isPasswordProperty.test(name)) {
                replaceChild(gitRow, liveRow, "value");
            } else {
                mergeInfoTable(gitRow, liveRow);
            }
        }
    }

    /** Merges an INFOTABLE extension value whose data shape has PASSWORD fields. */
    private static void mergeInfoTable(Element gitRow, Element liveRow) {
        Element liveTable = infoTable(liveRow);
        if (liveTable == null) return;
        Set<String> secret = fieldsOfType(liveTable, "PASSWORD");
        if (secret.isEmpty()) return;
        Element gitTable = infoTable(gitRow);
        List<String> keys = primaryKeys(liveTable);
        if (gitTable == null || keys.isEmpty()) {
            replaceChild(gitRow, liveRow, "value");
            return;
        }
        Element gitRows = child(gitTable, "Rows");
        if (gitRows == null) {
            gitRows = gitTable.getOwnerDocument().createElement("Rows");
            gitTable.appendChild(gitRows);
        }
        Map<String, Element> liveByKey = rowsByKey(child(liveTable, "Rows"), keys);
        Set<String> matched = new HashSet<>();
        for (Element row : children(gitRows, "Row")) {
            String key = key(row, keys);
            Element liveRowOfKey = liveByKey.get(key);
            if (liveRowOfKey == null) continue;
            matched.add(key);
            for (String field : secret) replaceChild(row, liveRowOfKey, field);
        }
        for (Map.Entry<String, Element> entry : liveByKey.entrySet()) {
            if (!matched.contains(entry.getKey())) {
                gitRows.appendChild(entry.getValue().cloneNode(true));
            }
        }
    }

    /** Replaces {@code target}'s child element {@code name} with a copy of {@code source}'s. */
    private static void replaceChild(Element target, Element source, String name) {
        Element existing = child(target, name);
        Element replacement = child(source, name);
        if (replacement == null) {
            if (existing != null) target.removeChild(existing);
            return;
        }
        Node copy = target.getOwnerDocument().importNode(replacement, true);
        if (existing != null) target.replaceChild(copy, existing);
        else target.appendChild(copy);
    }

    /** Empties every PASSWORD extension value of a User, including PASSWORD fields of tables. */
    private static void blankPasswords(Element user, Predicate<String> isPasswordProperty) {
        for (Map.Entry<String, Element> row : extensionRows(user).entrySet()) {
            if (isPasswordProperty.test(row.getKey())) {
                Element value = child(row.getValue(), "value");
                if (value != null) {
                    while (value.getFirstChild() != null) value.removeChild(value.getFirstChild());
                }
            } else {
                blankNestedPasswords(row.getValue());
            }
        }
    }

    /** Empties every PASSWORD field of an INFOTABLE extension value. */
    private static void blankNestedPasswords(Element extensionRow) {
        Element table = infoTable(extensionRow);
        if (table == null) return;
        Set<String> secret = fieldsOfType(table, "PASSWORD");
        if (secret.isEmpty()) return;
        Element rows = child(table, "Rows");
        if (rows == null) return;
        for (Element row : children(rows, "Row")) {
            for (String field : secret) {
                Element value = child(row, field);
                if (value == null) continue;
                while (value.getFirstChild() != null) value.removeChild(value.getFirstChild());
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Live users (platform access)
    // ---------------------------------------------------------------------------------------

    /**
     * The live user named {@code name}, or {@code null} when it does not exist. It is read as the
     * super user, so extension properties the caller may not read are still preserved; the values
     * never leave the server.
     */
    static LiveUser readLive(String name) throws Exception {
        User user = UserUtilities.findUser(name);
        if (user == null) return null;
        SecurityContext previous = ThreadLocalContext.getSecurityContext();
        ThreadLocalContext.setSecurityContext(SecurityContext.createSuperUserContext());
        try {
            return new LiveUser(serializeLive(user));
        } finally {
            ThreadLocalContext.setSecurityContext(previous);
        }
    }

    /**
     * Serializes the user with no serialization options: password hash, account state and
     * server-encrypted PASSWORD values included. {@code RootEntity.toXML} and {@code
     * SerializationOptions} are not in the extension SDK but exist unchanged from 9.6 to 10.2.
     */
    private static String serializeLive(User user) throws Exception {
        ClassLoader loader = User.class.getClassLoader();
        Class<?> options =
                Class.forName("com.thingworx.common.utils.SerializationOptions", true, loader);
        Class<?> option =
                Class.forName("com.thingworx.common.utils.SerializationOption", true, loader);
        Object none =
                options.getConstructor(option.arrayType())
                        .newInstance((Object) Array.newInstance(option, 0));
        Document document = SafeXml.newDocumentBuilder().newDocument();
        Object element =
                ((CompletableFuture<?>)
                                user.getClass()
                                        .getMethod("toXML", Document.class, String.class, options)
                                        .invoke(user, document, "User", none))
                        .get();
        document.appendChild((Node) element);
        return serialize(document);
    }

    /** Which UserExtensions properties are PASSWORD properties. */
    static Predicate<String> passwordProperties() {
        ThingShape shape =
                (ThingShape)
                        EntityUtilities.findEntity(
                                USER_EXTENSIONS, ThingworxRelationshipTypes.ThingShape);
        Map<String, Boolean> cache = new HashMap<>();
        return name ->
                cache.computeIfAbsent(
                        name,
                        n -> {
                            if (shape == null) return false;
                            PropertyDefinition definition = shape.getEffectivePropertyDefinition(n);
                            return definition != null
                                    && definition.getBaseType() == BaseTypes.PASSWORD;
                        });
    }

    /** A random password that satisfies the platform's default password rules. */
    static String randomPassword() {
        String[] classes = {
            "ABCDEFGHJKLMNPQRSTUVWXYZ", "abcdefghijkmnopqrstuvwxyz", "23456789", "!#%*+-=?@_"
        };
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            String characters = classes[i % classes.length];
            password.append(characters.charAt(RANDOM.nextInt(characters.length())));
        }
        return password.toString();
    }

    // ---------------------------------------------------------------------------------------
    // XML helpers
    // ---------------------------------------------------------------------------------------

    private static final java.util.regex.Pattern USERS_COLLECTION =
            java.util.regex.Pattern.compile("<" + USERS + "[\\s/>]");

    static boolean mayContainUsers(String xml) {
        return xml != null && USERS_COLLECTION.matcher(xml).find();
    }

    private static List<Element> users(Document document) {
        List<Element> result = new ArrayList<>();
        Element root = document.getDocumentElement();
        if (!"Entities".equals(root.getTagName())) return result;
        for (Element collection : children(root, USERS))
            result.addAll(children(collection, "User"));
        return result;
    }

    private static Element extensionTable(Element user) {
        Element tables = child(user, "ConfigurationTables");
        if (tables == null) return null;
        for (Element table : children(tables, "ConfigurationTable")) {
            if (USER_EXTENSIONS.equals(table.getAttribute("name"))) return table;
        }
        return null;
    }

    private static Map<String, Element> extensionRows(Element user) {
        Element table = extensionTable(user);
        return table == null ? Map.of() : rowsByName(child(table, "Rows"));
    }

    private static Map<String, Element> rowsByName(Element rows) {
        Map<String, Element> result = new LinkedHashMap<>();
        if (rows == null) return result;
        for (Element row : children(rows, "Row")) {
            Element name = child(row, "name");
            if (name != null) result.put(name.getTextContent().trim(), row);
        }
        return result;
    }

    private static Element infoTable(Element extensionRow) {
        Element value = child(extensionRow, "value");
        return value == null ? null : child(value, "infoTable");
    }

    private static List<Element> fieldDefinitions(Element infoTable) {
        Element shape = child(infoTable, "DataShape");
        Element fields = shape == null ? null : child(shape, "FieldDefinitions");
        return fields == null ? List.of() : children(fields, "FieldDefinition");
    }

    private static Set<String> fieldsOfType(Element infoTable, String baseType) {
        Set<String> result = new HashSet<>();
        for (Element field : fieldDefinitions(infoTable)) {
            if (baseType.equals(field.getAttribute("baseType")))
                result.add(field.getAttribute("name"));
        }
        return result;
    }

    private static List<String> primaryKeys(Element infoTable) {
        List<String> result = new ArrayList<>();
        for (Element field : fieldDefinitions(infoTable)) {
            if ("true".equals(field.getAttribute("aspect.isPrimaryKey"))) {
                result.add(field.getAttribute("name"));
            }
        }
        result.sort(null);
        return result;
    }

    private static Map<String, Element> rowsByKey(Element rows, List<String> keys) {
        Map<String, Element> result = new LinkedHashMap<>();
        if (rows == null) return result;
        for (Element row : children(rows, "Row")) result.putIfAbsent(key(row, keys), row);
        return result;
    }

    private static String key(Element row, List<String> keys) {
        StringBuilder key = new StringBuilder();
        for (String field : keys) {
            Element value = child(row, field);
            key.append(value == null ? "" : value.getTextContent()).append('\u0000');
        }
        return key.toString();
    }

    private static Element child(Element parent, String name) {
        if (parent == null) return null;
        for (Node c = parent.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof Element e && name.equals(e.getTagName())) return e;
        }
        return null;
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        if (parent == null) return result;
        for (Node c = parent.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof Element e && name.equals(e.getTagName())) result.add(e);
        }
        return result;
    }

    private static Document parse(String xml) throws Exception {
        return SafeXml.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String serialize(Document document) throws Exception {
        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        StringWriter out = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(out));
        return out.toString();
    }
}
