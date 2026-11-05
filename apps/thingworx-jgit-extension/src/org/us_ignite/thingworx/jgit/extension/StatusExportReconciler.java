package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.entities.RootEntity;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Reconciles canonical project exports while leaving unrelated and untracked files alone. */
final class StatusExportReconciler {
    private static final long MAX_ENTITY_XML_BYTES = 20L * 1024 * 1024;

    @FunctionalInterface
    interface EntityLookup {
        boolean belongsToProject(String type, String name, String project) throws Exception;
    }

    static void reconcile(Git git, Path snapshot, String repositoryPath, String project)
            throws Exception {
        reconcile(
                git,
                snapshot,
                repositoryPath,
                project,
                (type, name, configuredProject) -> {
                    final ThingworxRelationshipTypes entityType;
                    try {
                        entityType = ThingworxRelationshipTypes.valueOf(type);
                    } catch (IllegalArgumentException e) {
                        // Unknown XML types cannot safely be classified as removed entities.
                        return true;
                    }
                    RootEntity entity = EntityUtilities.findEntityDirect(name, entityType);
                    return entity != null && configuredProject.equals(entity.GetProjectName());
                });
    }

    static void reconcile(
            Git git, Path snapshot, String repositoryPath, String project, EntityLookup lookup)
            throws Exception {
        Path root = git.getRepository().getWorkTree().toPath().toRealPath();
        String repoPath =
                repositoryPath == null
                        ? ""
                        : repositoryPath.replace('\\', '/').replaceAll("^/+|/+$", "");
        Path relativeProject =
                (repoPath.isEmpty() ? Path.of("") : Path.of(repoPath)).resolve(project).normalize();
        if (relativeProject.isAbsolute()
                || relativeProject.startsWith("..")
                || relativeProject.getNameCount() == 0
                || project.contains("/")
                || project.contains("\\")
                || project.equals(".")
                || project.equals("..")) {
            throw new IOException("Invalid project export path.");
        }
        Path projectRoot = root.resolve(relativeProject).normalize();
        if (!projectRoot.startsWith(root))
            throw new IOException("Project path is outside the repository.");
        String prefix = relativeProject.toString().replace('\\', '/') + "/";
        for (String path : git.status().call().getConflicting()) {
            if (path.startsWith(prefix))
                throw new IOException(
                        "Resolve project export conflicts before refreshing status: " + path);
        }
        DirCache index = git.getRepository().readDirCache();
        Map<String, DirCacheEntry> tracked = new HashMap<>();
        for (int i = 0; i < index.getEntryCount(); i++) {
            DirCacheEntry entry = index.getEntry(i);
            if (entry.getPathString().startsWith(prefix)) tracked.put(entry.getPathString(), entry);
        }
        Map<String, Path> exported = new HashMap<>();
        try (var paths = Files.walk(snapshot)) {
            for (Path source :
                    paths.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).toList()) {
                String suffix = snapshot.relativize(source).toString().replace('\\', '/');
                exported.put(prefix + suffix, source);
            }
        }
        List<Map.Entry<Path, Path>> copies = new ArrayList<>();
        for (var item : exported.entrySet()) {
            Path destination = root.resolve(item.getKey()).normalize();
            checkSafeTarget(root, destination);
            if (tracked.containsKey(item.getKey())
                    || !Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                copies.add(Map.entry(item.getValue(), destination));
            }
        }
        List<Path> deletions = new ArrayList<>();
        for (var item : tracked.entrySet()) {
            if (exported.containsKey(item.getKey())) continue;
            String suffix = item.getKey().substring(prefix.length());
            Path destination = root.resolve(item.getKey()).normalize();
            if (!Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) continue;
            checkSafeTarget(root, destination);
            String[] parts = suffix.split("/", -1);
            if (parts.length != 2 || !parts[1].endsWith(".xml")) continue;
            EntityIdentity identity = readCanonicalEntity(git, item.getValue(), parts[0], parts[1]);
            if (identity == null
                    || (identity.type().equals("Project") && identity.name().equals(project)))
                continue;
            if (!lookup.belongsToProject(identity.type(), identity.name(), project))
                deletions.add(destination);
        }
        for (var item : copies) {
            Files.createDirectories(item.getValue().getParent());
            Files.copy(item.getKey(), item.getValue(), StandardCopyOption.REPLACE_EXISTING);
        }
        for (Path deletion : deletions) Files.delete(deletion);
    }

    private static void checkSafeTarget(Path root, Path target) throws IOException {
        if (!target.startsWith(root))
            throw new IOException("Export path is outside the repository.");
        Path current = root;
        for (Path component : root.relativize(target)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current))
                throw new IOException("Refusing to write through a symbolic link: " + current);
        }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                && !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Export path is not a regular file: " + target);
        }
    }

    private record EntityIdentity(String type, String name) {}

    private static EntityIdentity readCanonicalEntity(
            Git git, DirCacheEntry entry, String collection, String filename) {
        try {
            var loader = git.getRepository().open(entry.getObjectId());
            if (loader.getSize() > MAX_ENTITY_XML_BYTES) return null;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Element document =
                    factory.newDocumentBuilder()
                            .parse(new ByteArrayInputStream(loader.getBytes()))
                            .getDocumentElement();
            if (!"Entities".equals(document.getTagName())) return null;
            Element group = null;
            for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element) {
                    if (group != null) return null;
                    group = (Element) n;
                }
            }
            if (group == null || !collection.equals(group.getTagName())) return null;
            Element entity = null;
            for (Node n = group.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element) {
                    if (entity != null) return null;
                    entity = (Element) n;
                }
            }
            if (entity == null) return null;
            String name = entity.getAttribute("name");
            if (name.isBlank() || !(name + ".xml").equals(filename)) return null;
            String type = entity.getTagName();
            String plural =
                    type.endsWith("y") ? type.substring(0, type.length() - 1) + "ies" : type + "s";
            if (!collection.equals(plural)) return null;
            return new EntityIdentity(type, name);
        } catch (Exception e) {
            // A path that cannot be proven to be a canonical export is never deleted.
            return null;
        }
    }

    private StatusExportReconciler() {}
}
