package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.entities.RootEntity;
import com.thingworx.entities.interfaces.IServiceProvider;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.StringPrimitive;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Validates and removes selected entity exports in the caller's security context. */
final class RemoveEverywhere {
    private static final Map<String, String> SERVICES =
            Map.ofEntries(
                    Map.entry("Thing", "DeleteThing"),
                    Map.entry("ThingTemplate", "DeleteThingTemplate"),
                    Map.entry("ThingShape", "DeleteThingShape"),
                    Map.entry("Network", "DeleteNetwork"),
                    Map.entry("MediaEntity", "DeleteMediaEntity"),
                    Map.entry("ModelTagVocabulary", "DeleteModelTagVocabulary"),
                    Map.entry("DataTagVocabulary", "DeleteDataTagVocabulary"),
                    Map.entry("StyleDefinition", "DeleteStyleDefinition"),
                    Map.entry("NotificationDefinition", "DeleteNotificationDefinition"),
                    Map.entry("NotificationContent", "DeleteNotificationContent"));

    private record Target(String type, String name) {}

    static String remove(FileRepositoryManager repo, InfoTable changes) throws Exception {
        if (changes == null || changes.getRowCount() == 0) {
            throw new IllegalArgumentException("No files selected.");
        }
        if (repo.projectName().isBlank()) {
            throw new IllegalArgumentException("A repository project is required.");
        }
        Path root = repo.git().getRepository().getWorkTree().toPath().toRealPath();
        var status = repo.git().status().call();
        var index = repo.git().getRepository().readDirCache();
        LinkedHashSet<String> indexed = new LinkedHashSet<>();
        for (int i = 0; i < index.getEntryCount(); i++)
            indexed.add(index.getEntry(i).getPathString());
        LinkedHashSet<String> known = new LinkedHashSet<>(indexed);
        known.addAll(status.getUntracked());
        known.addAll(status.getRemoved());
        known.addAll(status.getMissing());
        LinkedHashMap<String, Path> paths = new LinkedHashMap<>();
        LinkedHashMap<String, Target> targets = new LinkedHashMap<>();
        for (int i = 0; i < changes.getRowCount(); i++) {
            String rel =
                    repo.normalizeRepositoryRelativePath(changes.getRow(i).getStringValue("File"));
            if (rel.equals(".") || !known.contains(rel) || status.getConflicting().contains(rel)) {
                throw new IllegalArgumentException(
                        rel + ": file is not an eligible working-tree path.");
            }
            Path path = root.resolve(rel).normalize();
            if (!path.startsWith(root)
                    || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                    || !path.toRealPath().startsWith(root)) {
                throw new IllegalArgumentException(
                        rel + ": expected a regular file within the repository.");
            }
            paths.put(rel, path);
            for (Target target : readTargets(path))
                targets.put(target.type() + "\0" + target.name(), target);
        }
        IServiceProvider entityServices =
                (IServiceProvider)
                        EntityUtilities.findEntity(
                                "EntityServices", ThingworxRelationshipTypes.Resource);
        if (entityServices == null)
            throw new IllegalStateException("EntityServices is unavailable.");
        for (Target target : targets.values()) {
            ThingworxRelationshipTypes type = ThingworxRelationshipTypes.valueOf(target.type());
            RootEntity entity = EntityUtilities.findEntity(target.name(), type);
            if (entity == null)
                throw new IllegalArgumentException(
                        target.type() + " " + target.name() + ": entity not found.");
            if (entity.isSystemObject() || entity.isExtensionObject()) {
                throw new IllegalArgumentException(
                        target.type()
                                + " "
                                + target.name()
                                + ": system or extension entity cannot be removed.");
            }
            if (!repo.projectName().equals(entity.GetProjectName())) {
                throw new IllegalArgumentException(
                        target.type()
                                + " "
                                + target.name()
                                + ": entity is not in project "
                                + repo.projectName()
                                + ".");
            }
            if (!Boolean.TRUE.equals(entity.CheckDesignTimePermission("Delete"))) {
                throw new SecurityException(
                        "Delete permission denied for " + target.type() + " " + target.name());
            }
            if (entityServices.getInstanceServiceDefinition(SERVICES.get(target.type())) == null) {
                throw new IllegalArgumentException(
                        "Deletion service unavailable for " + target.type());
            }
            if (!Boolean.TRUE.equals(
                    ((RootEntity) entityServices)
                            .CheckPermission("ServiceInvoke", SERVICES.get(target.type())))) {
                throw new SecurityException(
                        "Service permission denied for " + SERVICES.get(target.type()));
            }
        }
        List<String> completed = new ArrayList<>();
        try {
            for (Target target : targets.values()) {
                ValueCollection params = new ValueCollection();
                params.put("name", new StringPrimitive(target.name()));
                entityServices.processAPIServiceRequest(SERVICES.get(target.type()), params);
                completed.add(target.type() + " " + target.name());
            }
            for (Map.Entry<String, Path> file : paths.entrySet()) {
                Files.delete(file.getValue());
                completed.add("file " + file.getKey());
                if (indexed.contains(file.getKey())) repo.stagePath(file.getKey());
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Removal stopped after "
                            + (completed.isEmpty() ? "no changes" : String.join(", ", completed))
                            + "; remaining items were not processed: "
                            + e.getMessage(),
                    e);
        }
        return "Removed "
                + targets.size()
                + " ThingWorx entity/entities and "
                + paths.size()
                + " file(s); tracked deletions are staged.";
    }

    static List<Target> readTargets(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Element root = factory.newDocumentBuilder().parse(path.toFile()).getDocumentElement();
        if (!"Entities".equals(root.getTagName()))
            throw new IllegalArgumentException(
                    path.getFileName() + ": expected a ThingWorx Entities XML export.");
        List<Target> targets = new ArrayList<>();
        for (Node group = root.getFirstChild(); group != null; group = group.getNextSibling()) {
            if (!(group instanceof Element)) continue;
            for (Node item = group.getFirstChild(); item != null; item = item.getNextSibling()) {
                if (!(item instanceof Element entity)) continue;
                String type = entity.getTagName();
                String name = entity.getAttribute("name");
                if (!SERVICES.containsKey(type) || name.isBlank()) {
                    throw new IllegalArgumentException(
                            path.getFileName()
                                    + ": unsupported entity type or missing name: "
                                    + type);
                }
                targets.add(new Target(type, name));
            }
        }
        if (targets.isEmpty())
            throw new IllegalArgumentException(path.getFileName() + ": no entities found.");
        return targets;
    }

    private RemoveEverywhere() {}
}
