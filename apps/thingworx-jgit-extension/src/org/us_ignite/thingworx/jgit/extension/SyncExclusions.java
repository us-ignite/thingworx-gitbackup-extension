package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.entities.RootEntity;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.security.applicationkeys.ApplicationKey;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Entities that Git synchronization leaves out of imports and exports.
 *
 * <ul>
 *   <li><b>Users</b> are never imported, exported or deleted. A User carries the user's password
 *       and extension properties, including the per-remote Git sign-in. Source-control exports
 *       leave PASSWORD values blank, so importing a User from Git would overwrite the live user
 *       with empty credentials. Users are managed in ThingWorx, not through the repository.
 *   <li><b>ApplicationKeys whose stored secret cannot be decrypted</b> are left out of imports.
 *       ThingWorx refuses to update such a key ("Existing ApplicationKey property decode fail,
 *       cannot update"), which would otherwise fail the whole import after the Git change has been
 *       made. The Git copy carries no secret ({@code keyId=""}), so nothing is lost; the key must
 *       be repaired in ThingWorx by deleting and recreating it.
 * </ul>
 */
final class SyncExclusions {
    /** The collection element and export folder name of User entities. */
    static final String USERS = "Users";

    /** The collection element of ApplicationKey entities. */
    static final String APPLICATION_KEYS = "ApplicationKeys";

    private static final Pattern FILTERED_COLLECTION =
            Pattern.compile("<(" + USERS + "|" + APPLICATION_KEYS + ")[\\s/>]");

    /** Decides whether one entity is left out. */
    interface Rule {
        boolean excludes(String collection, String name);
    }

    /** One entity that was left out. */
    record Excluded(String collection, String name) {
        @Override
        public String toString() {
            return collection + "/" + name;
        }
    }

    /** The rule for exports: every User. */
    static final Rule EXPORT = (collection, name) -> USERS.equals(collection);

    /** The rule for imports: every User, and every ApplicationKey ThingWorx cannot update. */
    static final Rule IMPORT =
            (collection, name) ->
                    USERS.equals(collection)
                            || (APPLICATION_KEYS.equals(collection) && hasUndecodableSecret(name));

    private SyncExclusions() {}

    /**
     * Whether the live ApplicationKey has properties, such as its key ID, that could not be
     * decrypted when it was loaded, so ThingWorx will refuse to update it.
     */
    static boolean hasUndecodableSecret(String applicationKeyName) {
        if (applicationKeyName == null || applicationKeyName.isBlank()) return false;
        try {
            RootEntity entity =
                    EntityUtilities.findEntity(
                            applicationKeyName, ThingworxRelationshipTypes.ApplicationKey);
            if (!(entity instanceof ApplicationKey key)) return false;
            // Present on every supported platform (9.6 to 10.2) but not in the extension SDK.
            Object failures = key.getClass().getMethod("getPropertyDecodeFailures").invoke(key);
            return failures instanceof Set<?> set && !set.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /** Whether the XML text may contain a collection that a rule looks at. */
    static boolean mayContainFiltered(String xml) {
        return xml != null && FILTERED_COLLECTION.matcher(xml).find();
    }

    /** Whether a repository-relative path lies in a {@code Users/} export folder. */
    static boolean isUsersPath(String relativePath) {
        if (relativePath == null) return false;
        for (String part : relativePath.replace('\\', '/').split("/")) {
            if (USERS.equals(part)) return true;
        }
        return false;
    }

    /** The entities below {@code folder} that the rule leaves out, without changing anything. */
    static List<Excluded> find(Path folder, Rule rule) throws Exception {
        List<Excluded> found = new ArrayList<>();
        List<java.io.File> files = new ArrayList<>();
        FileRepositoryManager.collectXmlFilesOnDisk(folder.toFile(), files);
        for (java.io.File file : files) {
            String xml = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (mayContainFiltered(xml)) without(xml, rule, found);
        }
        return found;
    }

    /**
     * Copies the regular files below {@code source} into {@code target}, removing the entities the
     * rule leaves out. A file left with no entities is not copied. Symbolic links are skipped.
     *
     * @return the entities that were left out
     */
    static List<Excluded> copyExcluding(Path source, Path target, Rule rule) throws Exception {
        List<Excluded> removed = new ArrayList<>();
        List<Path> files;
        try (var paths = Files.walk(source)) {
            files = paths.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).toList();
        }
        for (Path file : files) {
            Path relative = source.relativize(file);
            boolean symlinked = false;
            Path current = source;
            for (Path component : relative) {
                current = current.resolve(component);
                if (Files.isSymbolicLink(current)) symlinked = true;
            }
            if (symlinked) continue;
            Path destination = target.resolve(relative.toString()).normalize();
            if (!destination.startsWith(target)) {
                throw new IOException("Path is outside the staging folder: " + relative);
            }
            Files.createDirectories(destination.getParent());
            if (!file.getFileName().toString().toLowerCase().endsWith(".xml")) {
                Files.copy(file, destination);
                continue;
            }
            String xml = Files.readString(file, StandardCharsets.UTF_8);
            if (!mayContainFiltered(xml)) {
                Files.copy(file, destination);
                continue;
            }
            String kept = without(xml, rule, removed);
            if (kept != null) Files.writeString(destination, kept, StandardCharsets.UTF_8);
        }
        return removed;
    }

    /**
     * Removes the entities the rule leaves out from every XML file below {@code folder}, in place.
     * Files left with no entities are deleted.
     *
     * @return the entities that were removed
     */
    static List<Excluded> removeIn(Path folder, Rule rule) throws Exception {
        List<Excluded> removed = new ArrayList<>();
        if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) return removed;
        List<java.io.File> files = new ArrayList<>();
        FileRepositoryManager.collectXmlFilesOnDisk(folder.toFile(), files);
        for (java.io.File file : files) {
            Path path = file.toPath();
            String xml = Files.readString(path, StandardCharsets.UTF_8);
            if (!mayContainFiltered(xml)) continue;
            String kept = without(xml, rule, removed);
            if (kept == null) Files.delete(path);
            else if (!kept.equals(xml)) Files.writeString(path, kept, StandardCharsets.UTF_8);
        }
        return removed;
    }

    /**
     * Returns the XML without the entities the rule leaves out, {@code null} when no entity is
     * left, or the text unchanged when nothing was removed or it is not an {@code <Entities>}
     * document. Removed entities are added to {@code removed}.
     */
    static String without(String xml, Rule rule, List<Excluded> removed) throws Exception {
        Document document = parse(xml);
        Element root = document.getDocumentElement();
        if (!"Entities".equals(root.getTagName())) return xml;
        boolean changed = false;
        boolean entitiesLeft = false;
        for (Node c = root.getFirstChild(); c != null; ) {
            Node nextCollection = c.getNextSibling();
            if (c instanceof Element collection) {
                String collectionName = collection.getTagName();
                for (Node e = collection.getFirstChild(); e != null; ) {
                    Node nextEntity = e.getNextSibling();
                    if (e instanceof Element entity) {
                        String name = entity.getAttribute("name");
                        if (rule.excludes(collectionName, name)) {
                            removed.add(new Excluded(collectionName, name));
                            collection.removeChild(e);
                            changed = true;
                        }
                    }
                    e = nextEntity;
                }
                boolean collectionEmpty = true;
                for (Node e = collection.getFirstChild(); e != null; e = e.getNextSibling()) {
                    if (e instanceof Element) collectionEmpty = false;
                }
                if (collectionEmpty && changed) root.removeChild(c);
                else if (!collectionEmpty) entitiesLeft = true;
            }
            c = nextCollection;
        }
        if (!changed) return xml;
        if (!entitiesLeft) return null;
        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        StringWriter out = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(out));
        return out.toString();
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
