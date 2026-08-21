package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.entities.interfaces.IServiceProvider;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.things.Thing;
import com.thingworx.things.repository.FileRepositoryThing;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.BooleanPrimitive;
import com.thingworx.types.primitives.IPrimitiveType;
import com.thingworx.types.primitives.StringPrimitive;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/** Operation-scoped access to the FileRepository Thing and its initialized JGit repository. */
final class FileRepositoryManager {
    private final FileRepositoryThing fileRepository;
    private final Git git;

    FileRepositoryManager(FileRepositoryThing resolvedTarget) throws Exception {
        fileRepository = resolvedTarget;
        git = openGit();
    }

    FileRepositoryManager(Thing thing) throws Exception {
        if (thing instanceof FileRepositoryThing) {
            fileRepository = (FileRepositoryThing) thing;
        } else {
            throw new IllegalAccessException("thing passed was not a file repository thing");
        }

        git = openGit();
    }

    FileRepositoryThing fileRepository() {
        return fileRepository;
    }

    String repositoryThingName() {
        return fileRepository.getName();
    }

    String gitRepoUrl() {
        return propertyString(Const.GitRepoURL, "");
    }

    String repositoryPath() {
        return propertyString(Const.RepoPath, "");
    }

    String projectName() {
        return propertyString(Const.ProjectName, "").trim();
    }

    String initialBranch() {
        return propertyString(Const.InitialBranch, "main");
    }

    Git git() {
        return git;
    }

    /** Exports the configured project, removes volatile metadata, and stages its tree. */
    void exportProjectEntities() throws Exception {
        if (projectName().isBlank()) {
            throw new Exception(Const.ERR_PREFIX_CONFIG + Const.ERR_PROJECT_NAME_REQUIRED);
        }
        IServiceProvider sourceControlFunctions =
                (IServiceProvider)
                        EntityUtilities.findEntity(
                                "SourceControlFunctions", ThingworxRelationshipTypes.Resource);
        if (sourceControlFunctions == null) {
            throw new Exception(Const.ERR_PREFIX_SYSTEM + Const.ERR_NO_SCF_RESOURCE);
        }

        ValueCollection params = new ValueCollection();
        params.put("repositoryName", new StringPrimitive(fileRepository.getName()));
        params.put("path", new StringPrimitive(repositoryPath()));
        params.put("projectName", new StringPrimitive(projectName()));
        params.put("includeDependents", new BooleanPrimitive(false));
        sourceControlFunctions.processServiceRequest("ExportSourceControlledEntities", params);
        sanitizeExportedXml(projectName());
        stagePath(normalizeRepositoryRelativePath(projectRepositoryPath()));
    }

    /**
     * Imports XML entities below the configured repository path.
     *
     * @return {@code true} when the platform import service was invoked; {@code false} when the
     *     configured path has no XML entities to import
     */
    boolean importProjectEntities() throws Exception {
        if (projectName().isBlank()) {
            throw new Exception(Const.ERR_PREFIX_CONFIG + Const.ERR_PROJECT_NAME_REQUIRED);
        }
        File root = new File(fileRepository.getRootPath(), repositoryPath());
        if (!root.exists() || !containsXmlFile(root)) return false;
        Object resource =
                EntityUtilities.findEntity(
                        "SourceControlFunctions", ThingworxRelationshipTypes.Resource);
        if (resource == null) {
            throw new Exception(Const.ERR_PREFIX_SYSTEM + Const.ERR_NO_SCF_RESOURCE);
        }
        ValueCollection params = new ValueCollection();
        params.put("repositoryName", new StringPrimitive(fileRepository.getName()));
        params.put("path", new StringPrimitive(repositoryPath()));
        params.put("useDefaultDataProvider", new BooleanPrimitive(true));
        params.put("withSubsystems", new BooleanPrimitive(false));
        params.put("overwritePropertyValues", new BooleanPrimitive(true));
        ((IServiceProvider) resource)
                .processServiceRequest("ImportSourceControlledEntities", params);
        return true;
    }

    void removeLastModifiedDate() throws IOException {
        removeXmlAttributes(null, "\\s+lastModifiedDate=\"[^\"]*\"");
    }

    void removeModelPersistenceProviderPackage() throws IOException {
        removeXmlAttributes(null, "\\s+modelPersistenceProviderPackage=\"[^\"]*\"");
    }

    private void sanitizeExportedXml(String exportedProjectName) throws IOException {
        removeXmlAttributes(exportedProjectName, "\\s+lastModifiedDate=\"[^\"]*\"");
        removeXmlAttributes(exportedProjectName, "\\s+modelPersistenceProviderPackage=\"[^\"]*\"");
    }

    private void removeXmlAttributes(String project, String attributePattern) throws IOException {
        File root = new File(fileRepository.getRootPath(), repositoryPath());
        if (project != null && !project.isBlank()) root = new File(root, project);
        if (!root.exists()) return;
        List<File> files = new ArrayList<>();
        collectXmlFilesOnDisk(root, files);
        for (File file : files) {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Files.write(
                    file.toPath(),
                    content.replaceAll(attributePattern, "").getBytes(StandardCharsets.UTF_8));
        }
    }

    private void collectXmlFilesOnDisk(File directory, List<File> result) {
        Deque<File> filesToCheck = new ArrayDeque<>();
        filesToCheck.add(directory);
        while (!filesToCheck.isEmpty()) {
            File file = filesToCheck.removeFirst();
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        filesToCheck.addLast(child);
                    }
                }
            } else if (file.getName().toLowerCase().endsWith(".xml")) {
                result.add(file);
            }
        }
    }

    private boolean containsXmlFile(File directory) {
        Deque<File> filesToCheck = new ArrayDeque<>();
        filesToCheck.add(directory);
        while (!filesToCheck.isEmpty()) {
            File file = filesToCheck.removeFirst();
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        filesToCheck.addLast(child);
                    }
                }
            } else if (file.getName().toLowerCase().endsWith(".xml")) {
                return true;
            }
        }
        return false;
    }

    private String projectRepositoryPath() {
        return (repositoryPath().isBlank() ? "" : repositoryPath()) + "/" + projectName();
    }

    private String normalizeRepositoryRelativePath(String path) {
        String clean = path.replaceAll("^/+|/+$", "");
        if (clean.isBlank()) throw new IllegalArgumentException("File is required.");
        return clean;
    }

    void stagePath(String path) throws Exception {
        git.add().addFilepattern(path).setUpdate(true).call();
        File workTreePath = new File(git.getRepository().getWorkTree(), path);
        if (workTreePath.exists()) git.add().addFilepattern(path).call();
    }

    private String propertyString(String name, String defaultValue) {
        try {
            Object value = fileRepository.getPropertyValue(name);
            Object raw =
                    value instanceof IPrimitiveType
                            ? ((IPrimitiveType<?, ?>) value).getValue()
                            : value;
            return raw == null ? defaultValue : raw.toString();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private Git openGit() throws Exception {
        File directory = new File(fileRepository.getRootPath());
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        builder.addCeilingDirectory(directory);
        builder.findGitDir(directory);
        Git opened =
                builder.getGitDir() == null
                        ? Git.init()
                                .setDirectory(directory)
                                .setInitialBranch(initialBranch())
                                .call()
                        : new Git(builder.build());
        StoredConfig config = opened.getRepository().getConfig();
        config.setString("remote", "origin", "url", gitRepoUrl());
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.setString("remote", "origin", "prune", "true");
        config.setString("core", null, "autocrlf", "input");
        config.setBoolean("repack", null, "writeBitmaps", false);
        config.save();
        return opened;
    }

    /** Closes the JGit handle when a caller explicitly owns a Git operation. */
    public void close() {
        git.close();
    }
}
