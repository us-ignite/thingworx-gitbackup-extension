package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.entities.RootEntity;
import com.thingworx.entities.interfaces.IServiceProvider;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.things.Thing;
import com.thingworx.things.repository.FileRepositoryThing;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.BooleanPrimitive;
import com.thingworx.types.primitives.IPrimitiveType;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
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

    /**
     * Returns the ThingWorx projects synchronized with this repository, in configured order. When
     * the {@code TrackedProjects} property has no rows, the legacy single-project {@code
     * ProjectName} property is used so repositories configured before multi-project tracking keep
     * working until their list is first edited.
     */
    List<String> trackedProjects() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        InfoTable table = propertyInfoTable(Const.TrackedProjects);
        if (table != null) {
            for (int i = 0; i < table.getRowCount(); i++) {
                String name = table.getRow(i).getStringValue(Const.ProjectName);
                if (name != null && !name.isBlank()) names.add(name.trim());
            }
        }
        if (names.isEmpty()) {
            String legacy = propertyString(Const.ProjectName, "").trim();
            if (!legacy.isBlank()) names.add(legacy);
        }
        return List.copyOf(names);
    }

    /** Comma-separated tracked project names for messages and logs. */
    String trackedProjectsLabel() {
        return String.join(", ", trackedProjects());
    }

    /**
     * Persists the tracked project list and clears the legacy {@code ProjectName} property, which
     * is superseded once the list has been written.
     */
    void setTrackedProjects(List<String> projects) throws Exception {
        InfoTable table =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.TrackedProjectDataShapeName);
        for (String project : new LinkedHashSet<>(projects)) {
            ValueCollection row = new ValueCollection();
            row.put(Const.ProjectName, new StringPrimitive(project));
            table.addRow(row);
        }
        fileRepository.setPropertyValue(Const.TrackedProjects, new InfoTablePrimitive(table));
        fileRepository.setPropertyValue(Const.ProjectName, new StringPrimitive(""));
    }

    /**
     * Validates a project name for use as an export folder below the repository path. ThingWorx
     * writes each project's export to {@code <RepoPath>/<ProjectName>/}.
     */
    static String validateProjectFolderName(String project) {
        String name = project == null ? "" : project.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("ProjectName is required.");
        if (name.equals(".")
                || name.equals("..")
                || name.contains("/")
                || name.contains("\\")
                || name.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid project name: " + project);
        }
        return name;
    }

    /**
     * Returns whether a project can be tracked: it must exist, be visible to the caller, and not be
     * a system project, an extension-owned project, or the project holding repository Things.
     */
    static boolean isTrackableProject(RootEntity project) {
        return project != null
                && !project.isSystemObject()
                && !project.isExtensionObject()
                && !Const.RepositoryProjectName.equals(project.getName());
    }

    /**
     * Lists the names of projects visible to the caller that pass {@link
     * #isTrackableProject(RootEntity)}, sorted case-insensitively.
     */
    static List<String> trackableProjectNames() throws Exception {
        InfoTable found =
                new com.thingworx.resources.entities.EntityServices()
                        .GetEntityList(
                                "Project", "", new com.thingworx.types.TagCollection(), 100000.0);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < found.getRowCount(); i++) {
            String name = found.getRow(i).getStringValue("name");
            if (name == null) continue;
            var project = EntityUtilities.findEntity(name, ThingworxRelationshipTypes.Project);
            if (isTrackableProject(project)) names.add(name);
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** Builds a {@code GIT.TrackedProject.DataShape} table from project names. */
    static InfoTable projectTable(List<String> projects) throws Exception {
        InfoTable table =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.TrackedProjectDataShapeName);
        for (String project : projects) {
            ValueCollection row = new ValueCollection();
            row.put(Const.ProjectName, new StringPrimitive(project));
            table.addRow(row);
        }
        return table;
    }

    private List<String> requireTrackedProjects() throws Exception {
        List<String> projects = trackedProjects();
        if (projects.isEmpty()) {
            throw new Exception(Const.ERR_PREFIX_CONFIG + Const.ERR_PROJECT_NAME_REQUIRED);
        }
        for (String project : projects) validateProjectFolderName(project);
        return projects;
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
     * Exports every tracked project and removes volatile metadata. Private - invisible to users.
     * Exported files are intentionally left unstaged; callers stage only the paths they want to
     * commit.
     */
    // intentionally not a ThingWorx service; sync is automatic via Pull/Push/Commit etc.
    void exportProjectEntities() throws Exception {
        // An export overwrites tracked files, which would discard conflict markers and
        // resolutions while a merge, rebase, or carried-change conflict is waiting.
        ConflictWorkflow.requireNotInProgress(git);
        for (String project : requireTrackedProjects()) {
            exportProjectEntitiesTo(repositoryPath(), project);
            sanitizeExportedXml(project);
        }
    }

    private void exportProjectEntitiesTo(String path, String project) throws Exception {
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
        params.put("projectName", new StringPrimitive(project));
        params.put("includeDependents", new BooleanPrimitive(false));
        sourceControlFunctions.processServiceRequest("ExportSourceControlledEntities", params);
    }

    /**
     * Refreshes the working tree from a separate project export without modifying the Git index.
     */
    void syncStatusFromThingWorx() throws Exception {
        ConflictWorkflow.requireNotInProgress(git);
        synchronized (StatusExportReconciler.class) {
            syncStatusFromThingWorxLocked();
        }
    }

    private void syncStatusFromThingWorxLocked() throws Exception {
        List<String> projects = requireTrackedProjects();
        Path base = new File(fileRepository.getRootPath()).toPath().toRealPath();
        Path temporary = Files.createTempDirectory(base, ".jgit-status-");
        try {
            // Export every project before reconciling any, so a failed export leaves the
            // working tree untouched. Each project gets its own export directory.
            List<Path> snapshots = new ArrayList<>();
            for (int i = 0; i < projects.size(); i++) {
                String project = projects.get(i);
                String exportDirectory = "/" + temporary.getFileName() + "/" + i;
                exportProjectEntitiesTo(exportDirectory, project);
                Path snapshot = temporary.resolve(String.valueOf(i)).resolve(project).normalize();
                if (!snapshot.startsWith(temporary) || !Files.isDirectory(snapshot)) {
                    throw new IOException(
                            "ThingWorx did not produce the expected export for project '"
                                    + project
                                    + "'.");
                }
                sanitizeXmlIn(snapshot.toFile());
                snapshots.add(snapshot);
            }
            for (int i = 0; i < projects.size(); i++) {
                StatusExportReconciler.reconcile(
                        git, snapshots.get(i), repositoryPath(), projects.get(i));
            }
        } finally {
            deleteRecursively(temporary);
        }
    }

    /**
     * Deletes a tracked project's export folder ({@code <RepoPath>/<ProjectName>/}) from the
     * working tree. The deletion is left unstaged so it appears in Changes for review. Symbolic
     * links are removed rather than followed.
     *
     * @return {@code true} when a folder existed and was deleted
     */
    boolean deleteProjectExport(String project) throws Exception {
        String name = validateProjectFolderName(project);
        synchronized (StatusExportReconciler.class) {
            Path workTree = git.getRepository().getWorkTree().toPath().toRealPath();
            Path folder = repositoryRoot().toPath().resolve(name).normalize();
            if (!Files.exists(folder, LinkOption.NOFOLLOW_LINKS)) return false;
            if (Files.isSymbolicLink(folder)
                    || !folder.toRealPath().startsWith(workTree)
                    || folder.toRealPath().equals(workTree)
                    || folder.toRealPath().startsWith(workTree.resolve(".git"))) {
                throw new IOException("Project folder is outside the repository: " + name);
            }
            deleteRecursively(folder);
            return true;
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    /**
     * Imports the XML entities of each tracked project from its {@code <RepoPath>/<ProjectName>/}
     * folder. Folders of projects that are no longer tracked are not imported. The filesystem check
     * uses {@link #repositoryRoot()} (stripped path) so that a configured {@code RepoPath} of
     * {@code "/shared"} does not resolve to the container filesystem root.
     *
     * @return {@code true} when the platform import service was invoked for at least one project;
     *     {@code false} when no tracked project folder contains XML entities
     */
    boolean importProjectEntities() throws Exception {
        // Never import files that still contain conflict markers or a half-finished merge.
        ConflictWorkflow.requireNotInProgress(git);
        List<String> projects = requireTrackedProjects();
        Object resource =
                EntityUtilities.findEntity(
                        "SourceControlFunctions", ThingworxRelationshipTypes.Resource);
        if (resource == null) {
            throw new Exception(Const.ERR_PREFIX_SYSTEM + Const.ERR_NO_SCF_RESOURCE);
        }
        String stripped = strippedRepositoryPath();
        boolean imported = false;
        for (String project : projects) {
            File folder = new File(repositoryRoot(), project);
            if (!folder.isDirectory() || !containsXmlFile(folder)) continue;
            ValueCollection params = new ValueCollection();
            params.put("repositoryName", new StringPrimitive(fileRepository.getName()));
            params.put(
                    "path",
                    new StringPrimitive(
                            "/" + (stripped.isBlank() ? project : stripped + "/" + project)));
            params.put("useDefaultDataProvider", new BooleanPrimitive(true));
            params.put("withSubsystems", new BooleanPrimitive(false));
            params.put("overwritePropertyValues", new BooleanPrimitive(true));
            ((IServiceProvider) resource)
                    .processServiceRequest("ImportSourceControlledEntities", params);
            imported = true;
        }
        return imported;
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

    /**
     * Returns the selected file when it is a regular file inside the work tree, or {@code null}
     * when a tracked file is already missing from disk and only its deletion needs staging.
     */
    java.nio.file.Path requireDeletableSourceFile(String path, boolean tracked) throws Exception {
        String clean = normalizeRepositoryRelativePath(path);
        java.nio.file.Path root = git.getRepository().getWorkTree().toPath().toRealPath();
        java.nio.file.Path file = root.resolve(clean).normalize();
        if (".".equals(clean) || !file.startsWith(root) || file.startsWith(root.resolve(".git"))) {
            throw new IllegalArgumentException("expected a file within the repository");
        }
        if (!java.nio.file.Files.exists(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            if (tracked) return null;
            throw new IllegalArgumentException("file does not exist");
        }
        if (!java.nio.file.Files.isRegularFile(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                || !file.toRealPath().startsWith(root)) {
            throw new IllegalArgumentException("expected a regular file within the repository");
        }
        return file;
    }

    /**
     * Deletes a working-tree file without touching ThingWorx entities. A tracked file's deletion is
     * staged so the project export does not restore it before the removal is committed. A tracked
     * file that is already missing from disk only has its deletion staged.
     */
    void deleteSourceFile(String path, boolean tracked) throws Exception {
        java.nio.file.Path file = requireDeletableSourceFile(path, tracked);
        if (file != null) java.nio.file.Files.delete(file);
        if (tracked) stagePath(path);
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

    private InfoTable propertyInfoTable(String name) {
        try {
            Object value = fileRepository.getPropertyValue(name);
            Object raw =
                    value instanceof IPrimitiveType
                            ? ((IPrimitiveType<?, ?>) value).getValue()
                            : value;
            return raw instanceof InfoTable ? (InfoTable) raw : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Git openGit() throws Exception {
        // Each service creates its own manager. Serialize initialization so concurrent reads
        // do not contend for the same .git/config.lock while applying repository settings.
        synchronized (FileRepositoryManager.class) {
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
    }

    /** Closes the JGit handle when a caller explicitly owns a Git operation. */
    public void close() {
        git.close();
    }
}
