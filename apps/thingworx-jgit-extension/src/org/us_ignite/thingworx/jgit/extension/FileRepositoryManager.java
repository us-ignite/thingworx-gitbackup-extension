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

    /**
     * Returns the repository path stripped of leading/trailing slashes for safe filesystem
     * resolution. Platform services receive the raw {@link #repositoryPath()} (which may include a
     * leading slash), but {@code java.io.File} resolution must not use an absolute second argument
     * or {@code fileRepository.getRootPath()} is ignored (e.g. {@code new File("/twx/storage/repo",
     * "/shared")} resolves to {@code /shared} instead of the FileRepository tree).
     */
    private String strippedRepositoryPath() {
        String p = repositoryPath();
        if (p == null) return "";
        return p.replaceAll("^/+|/+$", "");
    }

    /**
     * Resolves a repository-relative path below the FileRepository root, handling absolute {@code
     * RepoPath} values correctly. Returns {@code rootPath} itself when the repository path is
     * blank.
     */
    private File repositoryRoot() {
        String stripped = strippedRepositoryPath();
        File base = new File(fileRepository.getRootPath());
        if (stripped.isBlank()) return base;
        return new File(base, stripped);
    }

    /**
     * Exports the configured project and removes volatile metadata. Private - invisible to users.
     * Exported files are intentionally left unstaged; callers stage only the paths they want to
     * commit.
     */
    // intentionally not a ThingWorx service; sync is automatic via Pull/Push/Commit etc.
    void exportProjectEntities() throws Exception {
        exportProjectEntitiesTo(repositoryPath());
        sanitizeExportedXml(projectName());
    }

    private void exportProjectEntitiesTo(String path) throws Exception {
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
        params.put("path", new StringPrimitive(path));
        params.put("projectName", new StringPrimitive(projectName()));
        params.put("includeDependents", new BooleanPrimitive(false));
        sourceControlFunctions.processServiceRequest("ExportSourceControlledEntities", params);
    }

    /**
     * Refreshes the working tree from a separate project export without modifying the Git index.
     */
    void syncStatusFromThingWorx() throws Exception {
        synchronized (StatusExportReconciler.class) {
            syncStatusFromThingWorxLocked();
        }
    }

    private void syncStatusFromThingWorxLocked() throws Exception {
        java.nio.file.Path base = new File(fileRepository.getRootPath()).toPath().toRealPath();
        java.nio.file.Path temporary = Files.createTempDirectory(base, ".jgit-status-");
        try {
            exportProjectEntitiesTo("/" + temporary.getFileName());
            java.nio.file.Path project = temporary.resolve(projectName()).normalize();
            if (!project.startsWith(temporary) || !Files.isDirectory(project)) {
                throw new IOException("ThingWorx did not produce the expected project export.");
            }
            sanitizeXmlIn(project.toFile());
            StatusExportReconciler.reconcile(git, project, repositoryPath(), projectName());
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (java.nio.file.Path path :
                        paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    /**
     * Imports XML entities below the configured repository path. The filesystem check uses {@link
     * #repositoryRoot()} (stripped path) so that a configured {@code RepoPath} of {@code "/shared"}
     * does not resolve to the container filesystem root. The platform service itself receives the
     * raw {@link #repositoryPath()} value.
     *
     * @return {@code true} when the platform import service was invoked; {@code false} when the
     *     configured path has no XML entities to import
     */
    boolean importProjectEntities() throws Exception {
        if (projectName().isBlank()) {
            throw new Exception(Const.ERR_PREFIX_CONFIG + Const.ERR_PROJECT_NAME_REQUIRED);
        }
        File root = repositoryRoot();
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
        File root = repositoryRoot();
        if (exportedProjectName != null && !exportedProjectName.isBlank())
            root = new File(root, exportedProjectName);
        sanitizeXmlIn(root);
    }

    private void sanitizeXmlIn(File root) throws IOException {
        removeXmlAttributesIn(root, "\\s+lastModifiedDate=\"[^\"]*\"");
        removeXmlAttributesIn(root, "\\s+modelPersistenceProviderPackage=\"[^\"]*\"");
    }

    private void removeXmlAttributes(String project, String attributePattern) throws IOException {
        File root = repositoryRoot();
        if (project != null && !project.isBlank()) root = new File(root, project);
        removeXmlAttributesIn(root, attributePattern);
    }

    private void removeXmlAttributesIn(File root, String attributePattern) throws IOException {
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

    String normalizeRepositoryRelativePath(String path) {
        if (path == null) throw new IllegalArgumentException("File is required.");
        String clean = path.trim().replace('\\', '/');
        if (clean.matches("^[A-Za-z]:/.*")) {
            throw new IllegalArgumentException("Invalid file path: " + path);
        }
        // ThingWorx FileRepository paths may start with '/' but still resolve below its root.
        clean = clean.replaceAll("^/+|/+$", "");
        if (clean.isBlank()) throw new IllegalArgumentException("File is required.");
        if (".".equals(clean)) return ".";
        if (clean.indexOf('\0') >= 0) throw new IllegalArgumentException("Invalid file path.");
        try {
            java.nio.file.Path normalized = java.nio.file.Paths.get(clean).normalize();
            String result = normalized.toString().replace('\\', '/');
            if (normalized.isAbsolute()
                    || result.equals("..")
                    || result.startsWith("../")
                    || result.isBlank()) {
                throw new IllegalArgumentException("Invalid file path: " + path);
            }
            return result;
        } catch (java.nio.file.InvalidPathException e) {
            throw new IllegalArgumentException("Invalid file path: " + path, e);
        }
    }

    void stagePath(String path) throws Exception {
        String clean = normalizeRepositoryRelativePath(path);
        git.add().addFilepattern(clean).setUpdate(true).call();
        File workTreePath = new File(git.getRepository().getWorkTree(), clean);
        if (workTreePath.exists()) git.add().addFilepattern(clean).call();
    }

    void unstagePath(String path) throws Exception {
        String clean = normalizeRepositoryRelativePath(path);
        try {
            git.reset().addPath(clean).call();
        } catch (org.eclipse.jgit.api.errors.NoHeadException e) {
            git.rm().setCached(true).addFilepattern(clean).call();
        }
    }

    void unstageAll() throws Exception {
        try {
            git.reset().call();
        } catch (org.eclipse.jgit.api.errors.NoHeadException e) {
            org.eclipse.jgit.dircache.DirCache index = git.getRepository().readDirCache();
            List<String> paths = new ArrayList<>();
            for (int i = 0; i < index.getEntryCount(); i++) {
                paths.add(index.getEntry(i).getPathString());
            }
            for (String path : paths) {
                git.rm().setCached(true).addFilepattern(path).call();
            }
        }
    }

    void untrackPath(String path) throws Exception {
        String clean = normalizeRepositoryRelativePath(path);
        git.rm().setCached(true).addFilepattern(clean).call();
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
