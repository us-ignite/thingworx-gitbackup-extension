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

    /**
     * The retired {@code RepoPath} property, which earlier versions used as the parent folder of
     * every project folder. It is read only by {@link #migrateLegacyProjectSettings()}.
     */
    String legacyRepositoryPath() {
        return propertyString(Const.RepoPath, "").trim();
    }

    /**
     * The retired single-project {@code ProjectName} property, which earlier versions used before
     * the tracked projects table. It is read only by {@link #migrateLegacyProjectSettings()}.
     */
    String legacyProjectName() {
        return propertyString(Const.LegacyProjectName, "").trim();
    }

    /** The outcome of {@link #migrateLegacyProjectSettings()}. */
    record ProjectSettingsMigration(
            String repoPath, String projectName, List<TrackedProject> projects) {}

    /**
     * Converts the retired {@code ProjectName} and {@code RepoPath} properties into the tracked
     * projects table, then clears both. When the table is empty, {@code ProjectName} becomes its
     * row; when the table already has rows, it superseded {@code ProjectName}, which is dropped.
     * Every project without its own {@code Path} then gets {@code <RepoPath>/<ProjectName>}, so its
     * files stay where they are. Runs when a repository is opened and from the extension upgrade,
     * and does nothing once both properties are blank.
     *
     * @return the conversion, or {@code null} when there was nothing to convert
     * @throws IllegalArgumentException when a stored value is invalid or the resulting folders
     *     overlap; nothing is changed then
     */
    ProjectSettingsMigration migrateLegacyProjectSettings() throws Exception {
        String storedPath = legacyRepositoryPath();
        String storedName = legacyProjectName();
        if (storedPath.isEmpty() && storedName.isEmpty()) return null;
        List<TrackedProject> converted =
                convertLegacyProjectSettings(trackedProjectEntries(), storedName, storedPath);
        if (!converted.isEmpty()) setTrackedProjects(converted);
        fileRepository.setPropertyValue(Const.RepoPath, new StringPrimitive(""));
        fileRepository.setPropertyValue(Const.LegacyProjectName, new StringPrimitive(""));
        return new ProjectSettingsMigration(
                validateRepositoryPath(storedPath), storedName, converted);
    }

    /**
     * Returns the tracked projects that replace the retired {@code ProjectName} and {@code
     * RepoPath} properties. With an empty table, the two properties together become one tracked
     * project whose {@code Path} is {@code <RepoPath>/<ProjectName>}, the folder the earlier export
     * wrote to. With a table, which superseded {@code ProjectName}, every row without its own path
     * gets {@code <RepoPath>/<ProjectName>} for the same reason. A blank {@code RepoPath} leaves
     * paths blank, which means {@code <ProjectName>} at the repository root, as before.
     *
     * @throws IllegalArgumentException when a value is invalid or the folders overlap
     */
    static List<TrackedProject> convertLegacyProjectSettings(
            List<TrackedProject> table, String projectName, String repoPath) {
        String base;
        try {
            base = validateRepositoryPath(repoPath);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "The retired RepoPath '" + repoPath + "' is not a valid folder.", e);
        }
        List<TrackedProject> projects = new ArrayList<>(table);
        String name = projectName == null ? "" : projectName.trim();
        if (projects.isEmpty() && !name.isEmpty()) projects.add(new TrackedProject(name, ""));
        List<TrackedProject> result = new ArrayList<>();
        for (TrackedProject project : projects) {
            result.add(
                    project.hasCustomPath() || base.isEmpty()
                            ? project
                            : new TrackedProject(project.name(), base + "/" + project.name()));
        }
        result = normalizeTrackedProjects(result);
        requireDistinctProjectFolders(result);
        return result;
    }

    /**
     * Converts the retired {@code ProjectName} and {@code RepoPath} properties when the repository
     * is opened, so a repository the upgrade did not reach never synchronizes different folders. A
     * value that cannot be converted is logged and left in place; synchronization then refuses to
     * run until it is fixed.
     */
    /** Names the retired properties a conversion took over, with their values. */
    static String retiredLabel(ProjectSettingsMigration migration) {
        List<String> parts = new ArrayList<>();
        if (!migration.projectName().isEmpty()) {
            parts.add("ProjectName '" + migration.projectName() + "'");
        }
        if (!migration.repoPath().isEmpty()) parts.add("RepoPath '" + migration.repoPath() + "'");
        return String.join(" and ", parts);
    }

    private void migrateLegacyProjectSettingsOnOpen() {
        try {
            ProjectSettingsMigration migration = migrateLegacyProjectSettings();
            if (migration != null) {
                com.thingworx.logging.LogUtilities.getInstance()
                        .getApplicationLogger(FileRepositoryManager.class)
                        .warn(
                                "Moved the retired "
                                        + retiredLabel(migration)
                                        + " of "
                                        + fileRepository.getName()
                                        + " into its tracked projects");
            }
        } catch (Exception e) {
            com.thingworx.logging.LogUtilities.getInstance()
                    .getApplicationLogger(FileRepositoryManager.class)
                    .warn(
                            "Could not convert the retired ProjectName and RepoPath of "
                                    + fileRepository.getName()
                                    + ": "
                                    + e.getMessage());
        }
    }

    /** Normalizes a ThingWorx FileRepository path, whose leading slash is repository-relative. */
    static String validateRepositoryPath(String value) {
        String path = value == null ? "" : value.trim();
        if (path.indexOf('\0') >= 0
                || path.contains("\\")
                || path.contains(":")
                || path.startsWith("//")) {
            throw new IllegalArgumentException("Invalid folder path.");
        }
        path = path.replaceAll("^/+|/+$", "");
        if (path.isEmpty()) return "";
        for (String part : path.split("/", -1)) {
            if (part.isEmpty()
                    || part.equals(".")
                    || part.equals("..")
                    || part.equalsIgnoreCase(".git")) {
                throw new IllegalArgumentException("Invalid folder path.");
            }
        }
        return path;
    }

    /** Refuses symlinks and non-directories in existing path components below the repository. */
    static Path resolveRepositoryPath(Path repositoryRoot, String value) {
        String path = validateRepositoryPath(value);
        Path root = repositoryRoot.toAbsolutePath().normalize();
        Path current = root;
        if (!path.isEmpty()) {
            for (String part : path.split("/")) {
                current = current.resolve(part);
                if (Files.isSymbolicLink(current)
                        || (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                                && !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS))) {
                    throw new IllegalArgumentException(
                            "A folder path must contain only directories, not symbolic links.");
                }
            }
        }
        if (!current.normalize().startsWith(root)) {
            throw new IllegalArgumentException(
                    "A folder path must stay within the FileRepository.");
        }
        return current;
    }

    /** Returns the names of the ThingWorx projects synchronized with this repository, in order. */
    List<String> trackedProjects() {
        return trackedProjectEntries().stream().map(TrackedProject::name).toList();
    }

    /**
     * A tracked project and its optional custom entity folder. A blank {@code path} means the
     * default folder {@code <ProjectName>} at the repository root; otherwise it is the folder that
     * holds the project's entities, relative to the repository root, normalized without leading or
     * trailing slashes.
     */
    record TrackedProject(String name, String path) {
        TrackedProject {
            path = path == null ? "" : path;
        }

        boolean hasCustomPath() {
            return !path.isEmpty();
        }
    }

    /**
     * Returns the tracked projects with their configured paths, in configured order. Rows with a
     * blank name are skipped; the first row for a name wins. Paths are returned as stored and are
     * validated when they are used.
     */
    List<TrackedProject> trackedProjectEntries() {
        java.util.LinkedHashMap<String, TrackedProject> projects = new java.util.LinkedHashMap<>();
        InfoTable table = propertyInfoTable(Const.TrackedProjects);
        if (table != null) {
            for (int i = 0; i < table.getRowCount(); i++) {
                ValueCollection row = table.getRow(i);
                String name = row.getStringValue(Const.ProjectName);
                if (name == null || name.isBlank()) continue;
                // Rows written before the Path column existed have no value for it.
                String path =
                        row.containsKey(Const.TrackedProjectPath)
                                ? row.getStringValue(Const.TrackedProjectPath)
                                : "";
                projects.putIfAbsent(
                        name.trim(),
                        new TrackedProject(name.trim(), path == null ? "" : path.trim()));
            }
        }
        return List.copyOf(projects.values());
    }

    /** Comma-separated tracked project names for messages and logs. */
    String trackedProjectsLabel() {
        return String.join(", ", trackedProjects());
    }

    /**
     * Persists the tracked project list. Every path is validated, and the project folders may not
     * coincide or contain one another, before anything is written.
     */
    void setTrackedProjects(List<TrackedProject> projects) throws Exception {
        List<TrackedProject> normalized = normalizeTrackedProjects(projects);
        requireDistinctProjectFolders(normalized);
        fileRepository.setPropertyValue(
                Const.TrackedProjects, new InfoTablePrimitive(trackedProjectTable(normalized)));
    }

    /**
     * Validates names and paths and drops repeated names, keeping the first. Paths are returned in
     * normalized form.
     */
    static List<TrackedProject> normalizeTrackedProjects(List<TrackedProject> projects) {
        java.util.LinkedHashMap<String, TrackedProject> result = new java.util.LinkedHashMap<>();
        for (TrackedProject project : projects) {
            String name = validateProjectFolderName(project.name());
            result.putIfAbsent(name, new TrackedProject(name, validateProjectPath(project.path())));
        }
        return List.copyOf(result.values());
    }

    /**
     * Normalizes a tracked project's custom {@code Path}: a folder relative to the repository root
     * that holds the project's entities. A blank value means the default folder. The same rules as
     * {@link #validateRepositoryPath} apply: no '..', '.', empty segments, backslashes, drive
     * letters or {@code .git} segments.
     */
    static String validateProjectPath(String value) {
        try {
            return validateRepositoryPath(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid project Path: " + value);
        }
    }

    /**
     * Returns a tracked project's entity folder relative to the repository root, without leading or
     * trailing slashes: its custom path, or {@code <ProjectName>} at the repository root.
     */
    static String projectFolder(TrackedProject project) {
        String custom = validateProjectPath(project.path());
        return custom.isEmpty() ? validateProjectFolderName(project.name()) : custom;
    }

    /**
     * Refuses tracked projects whose entity folders are the same or nested, since one project's
     * export and import would then include, and its status refresh could delete, another project's
     * files.
     */
    static void requireDistinctProjectFolders(List<TrackedProject> projects) {
        List<String> folders = new ArrayList<>();
        for (TrackedProject project : projects) {
            String folder = projectFolder(project);
            for (int i = 0; i < folders.size(); i++) {
                String other = folders.get(i);
                if (folder.equals(other)
                        || folder.startsWith(other + "/")
                        || other.startsWith(folder + "/")) {
                    throw new IllegalArgumentException(
                            "The folders of projects '"
                                    + projects.get(i).name()
                                    + "' ("
                                    + other
                                    + ") and '"
                                    + project.name()
                                    + "' ("
                                    + folder
                                    + ") overlap; each tracked project needs its own folder.");
                }
            }
            folders.add(folder);
        }
    }

    /**
     * Validates a project name for use as a folder name. ThingWorx's export writes each project to
     * {@code <path>/<ProjectName>/}, and a project without a custom path lives in {@code
     * <ProjectName>/} at the repository root.
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
     * Validates the name of a project to create for tracking. The name becomes a folder in the
     * repository, so it is limited to letters, digits, '.', '_' and '-', must not start with '.' or
     * '-', and cannot be the extension's own project.
     */
    static String validateNewProjectName(String project) {
        String name = project == null ? "" : project.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Enter a project name.");
        if (name.length() > 200 || !name.matches("[A-Za-z0-9_][A-Za-z0-9_.\\-]*")) {
            throw new IllegalArgumentException(
                    "A project name may contain only letters, digits, '.', '_' and '-', and cannot start with '.' or '-'.");
        }
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.equals("git")
                || lower.startsWith("git.")
                || Const.RepositoryProjectName.equalsIgnoreCase(name)) {
            throw new IllegalArgumentException(
                    "'" + name + "' is reserved for the Git extension's own entities.");
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
                && !Const.RepositoryProjectName.equals(project.getName())
                && canAccessProject(project);
    }

    /**
     * Returns whether the calling user may see and read a project: it must be visible to the
     * caller's security context and grant the caller design-time Read. Both checks use the security
     * context of the current request thread; a thread without one is refused.
     */
    static boolean canAccessProject(RootEntity project) {
        try {
            return project != null
                    && project.isVisible()
                    && Boolean.TRUE.equals(project.CheckDesignTimePermission("Read"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Looks up a project by name for the calling user. Returns {@code null} both when the project
     * does not exist and when the caller may not see or read it, so callers report the two cases
     * identically.
     */
    static RootEntity findAccessibleProject(String name) {
        RootEntity project = EntityUtilities.findEntity(name, ThingworxRelationshipTypes.Project);
        return canAccessProject(project) ? project : null;
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

    /** Builds a {@code GIT.TrackedProject.DataShape} table from project names, with blank paths. */
    static InfoTable projectTable(List<String> projects) throws Exception {
        return trackedProjectTable(
                projects.stream().map(name -> new TrackedProject(name, "")).toList());
    }

    /** Builds a {@code GIT.TrackedProject.DataShape} table from tracked projects. */
    static InfoTable trackedProjectTable(List<TrackedProject> projects) throws Exception {
        InfoTable table =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.TrackedProjectDataShapeName);
        // An upgrade can keep an earlier copy of the data shape, which is an editable extension
        // object, so add the Path field when it is missing rather than dropping the values.
        if (!table.hasField(Const.TrackedProjectPath)) {
            var field =
                    new com.thingworx.metadata.FieldDefinition(
                            Const.TrackedProjectPath, com.thingworx.types.BaseTypes.STRING);
            field.setOrdinal(2);
            table.addField(field);
        }
        for (TrackedProject project : projects) {
            ValueCollection row = new ValueCollection();
            row.put(Const.ProjectName, new StringPrimitive(project.name()));
            row.put(Const.TrackedProjectPath, new StringPrimitive(project.path()));
            table.addRow(row);
        }
        return table;
    }

    /**
     * Returns the tracked projects with normalized paths, after checking that there is at least
     * one, that every name and path is valid, and that no two project folders overlap.
     */
    private List<TrackedProject> requireTrackedProjects() throws Exception {
        List<TrackedProject> projects = trackedProjectEntries();
        if (projects.isEmpty()) {
            throw new Exception(Const.ERR_PREFIX_CONFIG + Const.ERR_PROJECT_NAME_REQUIRED);
        }
        if (!legacyRepositoryPath().isEmpty() || !legacyProjectName().isEmpty()) {
            // The conversion on open failed; synchronizing now would use different folders.
            throw new Exception(
                    Const.ERR_PREFIX_CONFIG
                            + "The retired ProjectName and RepoPath properties could not be"
                            + " converted into the tracked projects. Clear them and set each"
                            + " project and its folder in Repository Settings.");
        }
        try {
            projects = normalizeTrackedProjects(projects);
            requireDistinctProjectFolders(projects);
        } catch (IllegalArgumentException e) {
            throw new Exception(Const.ERR_PREFIX_CONFIG + e.getMessage(), e);
        }
        return projects;
    }

    String initialBranch() {
        return propertyString(Const.InitialBranch, "main");
    }

    Git git() {
        return git;
    }

    /** The FileRepository root, which is the Git working tree. */
    private File repositoryRoot() {
        return new File(fileRepository.getRootPath());
    }

    /**
     * Exports every tracked project and removes volatile metadata. Private - invisible to users.
     * Exported files are intentionally left unstaged; callers stage only the paths they want to
     * commit.
     */
    // intentionally not a ThingWorx service; sync is automatic via Pull/Push/Commit etc.
    void exportProjectEntities() throws Exception {
        requireExportPermissions();
        // An export overwrites tracked files, which would discard conflict markers and
        // resolutions while a merge, rebase, or carried-change conflict is waiting.
        ConflictWorkflow.requireNotInProgress(git);
        for (TrackedProject project : requireTrackedProjects()) {
            if (project.hasCustomPath()) {
                exportToCustomFolder(project);
            } else {
                exportProjectEntitiesTo("", project.name());
                sanitizeExportedXml(project.name());
            }
        }
    }

    /**
     * Exports a project with a custom path. The platform export always writes to {@code
     * <path>/<ProjectName>/}, so the project is exported to a temporary folder and its files are
     * copied into the configured folder, replacing files of the same name. Like the direct export,
     * files that are no longer exported are left in place.
     */
    private void exportToCustomFolder(TrackedProject project) throws Exception {
        Path base = new File(fileRepository.getRootPath()).toPath().toRealPath();
        Path temporary = Files.createTempDirectory(base, ".jgit-export-");
        try {
            Path snapshot = exportSnapshot(temporary, "0", project.name());
            Path target =
                    resolveRepositoryPath(
                            new File(fileRepository.getRootPath()).toPath(),
                            projectFolder(project));
            copyExportInto(snapshot, target, base);
        } finally {
            deleteRecursively(temporary);
        }
    }

    /**
     * Exports a project into {@code <temporary>/<slot>/<ProjectName>/}, removes volatile metadata,
     * and returns that folder.
     */
    private Path exportSnapshot(Path temporary, String slot, String project) throws Exception {
        String exportDirectory = "/" + temporary.getFileName() + "/" + slot;
        exportProjectEntitiesTo(exportDirectory, project);
        Path snapshot = temporary.resolve(slot).resolve(project).normalize();
        if (!snapshot.startsWith(temporary) || !Files.isDirectory(snapshot)) {
            throw new IOException(
                    "ThingWorx did not produce the expected export for project '" + project + "'.");
        }
        sanitizeXmlIn(snapshot.toFile());
        return snapshot;
    }

    /**
     * Copies the regular files below {@code source} into {@code target}, replacing existing files.
     * Symbolic links in the source are skipped; a target component that is a symbolic link, or an
     * existing target that is not a regular file, is refused, as is anything outside {@code root}.
     */
    private static void copyExportInto(Path source, Path target, Path root) throws IOException {
        List<Path> files;
        try (var paths = Files.walk(source)) {
            files = paths.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).toList();
        }
        for (Path file : files) {
            Path destination = target.resolve(source.relativize(file).toString()).normalize();
            if (!destination.startsWith(root)) {
                throw new IOException("Export path is outside the repository: " + destination);
            }
            Path current = root;
            for (Path component : root.relativize(destination)) {
                current = current.resolve(component);
                if (Files.isSymbolicLink(current)) {
                    throw new IOException("Refusing to write through a symbolic link: " + current);
                }
            }
            if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Export path is not a regular file: " + destination);
            }
            Files.createDirectories(destination.getParent());
            Files.copy(file, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static final String EXPORT_SERVICE = "ExportSourceControlledEntities";
    private static final String IMPORT_SERVICE = "ImportSourceControlledEntities";

    /**
     * Requires that the calling user may run both the project export and the entity import for
     * every tracked project. Services that import after changing the repository call this before
     * they start, so a missing permission is reported before any Git or file change is made.
     */
    void requireSyncPermissions() throws Exception {
        requireExportPermissions();
        requireImportPermissions();
    }

    /** Requires that the calling user may export every tracked project. */
    void requireExportPermissions() throws Exception {
        requireSourceControlPermission(EXPORT_SERVICE);
    }

    /** Requires that the calling user may import entities into every tracked project. */
    void requireImportPermissions() throws Exception {
        requireSourceControlPermission(IMPORT_SERVICE);
    }

    /**
     * The platform services are invoked directly from Java, which does not apply the calling user's
     * permissions, so they are checked here: ServiceInvoke for the exact service on the
     * SourceControlFunctions resource, and visibility plus design-time Read on each tracked
     * project. A tracked project that does not exist yet has nothing to read and is created by the
     * import, so only the service permission applies to it.
     */
    private IServiceProvider requireSourceControlPermission(String service) throws Exception {
        List<TrackedProject> projects = requireTrackedProjects();
        RootEntity resource =
                EntityUtilities.findEntity(
                        "SourceControlFunctions", ThingworxRelationshipTypes.Resource);
        if (!(resource instanceof IServiceProvider)) {
            throw new Exception(Const.ERR_PREFIX_SYSTEM + Const.ERR_NO_SCF_RESOURCE);
        }
        boolean allowed;
        try {
            allowed = Boolean.TRUE.equals(resource.CheckPermission("ServiceInvoke", service));
        } catch (Exception e) {
            allowed = false;
        }
        if (!allowed) {
            throw new SecurityException(
                    Const.ERR_PREFIX_AUTH
                            + "ServiceInvoke permission for "
                            + service
                            + " on the SourceControlFunctions resource is required.");
        }
        for (TrackedProject tracked : projects) {
            String project = tracked.name();
            RootEntity entity =
                    EntityUtilities.findEntity(project, ThingworxRelationshipTypes.Project);
            if (entity != null && !canAccessProject(entity)) {
                throw new SecurityException(
                        Const.ERR_PREFIX_AUTH
                                + "Visibility and design-time Read permission on tracked project '"
                                + project
                                + "' are required.");
            }
        }
        return (IServiceProvider) resource;
    }

    private void exportProjectEntitiesTo(String path, String project) throws Exception {
        IServiceProvider sourceControlFunctions = requireSourceControlPermission(EXPORT_SERVICE);
        resolveRepositoryPath(
                new File(fileRepository.getRootPath()).toPath(),
                validateRepositoryPath(path) + "/" + validateProjectFolderName(project));

        ValueCollection params = new ValueCollection();
        params.put("repositoryName", new StringPrimitive(fileRepository.getName()));
        params.put("path", new StringPrimitive(path));
        params.put("projectName", new StringPrimitive(project));
        params.put("includeDependents", new BooleanPrimitive(false));
        sourceControlFunctions.processServiceRequest(EXPORT_SERVICE, params);
    }

    /**
     * Refreshes the working tree from a separate project export without modifying the Git index.
     */
    void syncStatusFromThingWorx() throws Exception {
        requireExportPermissions();
        ConflictWorkflow.requireNotInProgress(git);
        synchronized (StatusExportReconciler.class) {
            syncStatusFromThingWorxLocked();
        }
    }

    private void syncStatusFromThingWorxLocked() throws Exception {
        List<TrackedProject> projects = requireTrackedProjects();
        Path base = new File(fileRepository.getRootPath()).toPath().toRealPath();
        Path temporary = Files.createTempDirectory(base, ".jgit-status-");
        try {
            // Export every project before reconciling any, so a failed export leaves the
            // working tree untouched. Each project gets its own export directory.
            List<Path> snapshots = new ArrayList<>();
            for (int i = 0; i < projects.size(); i++) {
                snapshots.add(exportSnapshot(temporary, String.valueOf(i), projects.get(i).name()));
            }
            for (int i = 0; i < projects.size(); i++) {
                TrackedProject project = projects.get(i);
                StatusExportReconciler.reconcileFolder(
                        git, snapshots.get(i), projectFolder(project), project.name());
            }
        } finally {
            deleteRecursively(temporary);
        }
    }

    /**
     * Deletes a tracked project's entity folder (its custom path, or {@code <ProjectName>/} at the
     * root) from the working tree. The deletion is left unstaged so it appears in Changes for
     * review. Symbolic links are removed rather than followed.
     *
     * @return {@code true} when a folder existed and was deleted
     */
    boolean deleteProjectExport(TrackedProject project) throws Exception {
        String relative = projectFolder(project);
        synchronized (StatusExportReconciler.class) {
            Path workTree = git.getRepository().getWorkTree().toPath().toRealPath();
            Path folder =
                    resolveRepositoryPath(
                            new File(fileRepository.getRootPath()).toPath(), relative);
            if (!Files.exists(folder, LinkOption.NOFOLLOW_LINKS)) return false;
            if (Files.isSymbolicLink(folder)
                    || !folder.toRealPath().startsWith(workTree)
                    || folder.toRealPath().equals(workTree)
                    || folder.toRealPath().startsWith(workTree.resolve(".git"))) {
                throw new IOException("Project folder is outside the repository: " + relative);
            }
            deleteRecursively(folder);
            return true;
        }
    }

    /** The outcome of {@link #changeProjectFolder}. */
    record FolderChange(String from, String to, int moved) {}

    /**
     * Changes the folder of the tracked project at {@code index} and, when {@code moveFiles} is
     * true, moves the project's files from the old folder to the new one like {@code git mv}. The
     * new project list is validated before any file is moved, and the files are moved back when the
     * list cannot be saved. Callers hold the {@link StatusExportReconciler} lock.
     */
    FolderChange changeProjectFolder(
            List<TrackedProject> projects, int index, String path, boolean moveFiles)
            throws Exception {
        TrackedProject current = projects.get(index);
        TrackedProject updated = new TrackedProject(current.name(), validateProjectPath(path));
        List<TrackedProject> next = new ArrayList<>(projects);
        next.set(index, updated);
        next = normalizeTrackedProjects(next);
        requireDistinctProjectFolders(next);
        String from = projectFolder(current);
        String to = projectFolder(updated);
        int moved = moveFiles && !from.equals(to) ? moveFolder(from, to) : 0;
        try {
            setTrackedProjects(next);
        } catch (Exception e) {
            if (moved > 0) moveFolder(to, from);
            throw e;
        }
        return new FolderChange(from, to, moved);
    }

    /**
     * Moves every file below one repository-relative folder to another, like {@code git mv}: index
     * entries are renamed with their staged content and mode unchanged, so staged and unstaged
     * changes are kept, and untracked files are moved on disk and stay untracked. Nothing is
     * changed when a conflict is in progress, when the folders are nested, when a destination file
     * already exists on disk or in the index, or when a symbolic link is involved. Folders left
     * empty below the source are removed.
     *
     * @return the number of files moved
     */
    int moveFolder(String from, String to) throws Exception {
        return moveFolder(git, from, to);
    }

    static int moveFolder(Git git, String from, String to) throws Exception {
        ConflictWorkflow.requireNotInProgress(git);
        String source = validateProjectPath(from);
        String target = validateProjectPath(to);
        if (source.isEmpty() || target.isEmpty()) {
            throw new IllegalArgumentException("A project folder cannot be the repository root.");
        }
        if (source.equals(target)) return 0;
        if (target.startsWith(source + "/") || source.startsWith(target + "/")) {
            throw new IllegalArgumentException(
                    "Cannot move the files of '"
                            + source
                            + "' to '"
                            + target
                            + "' because one folder contains the other.");
        }
        Path root = git.getRepository().getWorkTree().toPath().toRealPath();
        Path sourceDir = resolveRepositoryPath(root, source);
        Path targetDir = resolveRepositoryPath(root, target);

        org.eclipse.jgit.dircache.DirCache index = git.getRepository().lockDirCache();
        List<Path[]> movedOnDisk = new ArrayList<>();
        try {
            java.util.TreeMap<String, org.eclipse.jgit.dircache.DirCacheEntry> entries =
                    new java.util.TreeMap<>();
            java.util.Set<String> indexed = new java.util.HashSet<>();
            for (int i = 0; i < index.getEntryCount(); i++) {
                var entry = index.getEntry(i);
                String entryPath = entry.getPathString();
                indexed.add(entryPath);
                if (!entryPath.startsWith(source + "/")) continue;
                if (entry.getStage() != 0) {
                    throw new IllegalStateException(
                            "Resolve the conflict in " + entryPath + " before moving the folder.");
                }
                entries.put(entryPath.substring(source.length() + 1), entry);
            }
            java.util.TreeSet<String> files = new java.util.TreeSet<>(entries.keySet());
            if (Files.isDirectory(sourceDir, LinkOption.NOFOLLOW_LINKS)) {
                try (var paths = Files.walk(sourceDir)) {
                    for (Path p : paths.toList()) {
                        if (Files.isSymbolicLink(p)) {
                            throw new IOException(
                                    "Refusing to move a symbolic link: " + root.relativize(p));
                        }
                        if (Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) {
                            files.add(sourceDir.relativize(p).toString().replace('\\', '/'));
                        }
                    }
                }
            }
            for (String file : files) {
                String destination = target + "/" + file;
                Path destinationPath = root.resolve(destination).normalize();
                if (indexed.contains(destination)
                        || Files.exists(destinationPath, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IllegalArgumentException(
                            destination + " already exists; nothing was moved.");
                }
                requireNoLinkComponents(root, destinationPath);
            }
            for (String file : files) {
                Path src = sourceDir.resolve(file);
                if (!Files.exists(src, LinkOption.NOFOLLOW_LINKS)) continue; // missing tracked file
                Path dst = targetDir.resolve(file);
                Files.createDirectories(dst.getParent());
                Files.move(src, dst);
                movedOnDisk.add(new Path[] {src, dst});
            }
            var editor = index.editor();
            for (var item : entries.entrySet()) {
                var original = item.getValue();
                editor.add(new org.eclipse.jgit.dircache.DirCacheEditor.DeletePath(original));
                editor.add(
                        new org.eclipse.jgit.dircache.DirCacheEditor.PathEdit(
                                target + "/" + item.getKey()) {
                            @Override
                            public void apply(org.eclipse.jgit.dircache.DirCacheEntry ent) {
                                ent.copyMetaData(original);
                            }
                        });
            }
            editor.finish();
            index.write();
            if (!index.commit()) throw new IOException("Could not update the Git index.");
            index = null;
            deleteEmptyFolders(sourceDir);
            // Like git mv, leave no empty parent folders behind.
            for (Path parent = sourceDir.getParent();
                    parent != null && parent.startsWith(root) && !parent.equals(root);
                    parent = parent.getParent()) {
                if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) break;
                try (var children = Files.list(parent)) {
                    if (children.findAny().isPresent()) break;
                }
                Files.delete(parent);
            }
            return files.size();
        } catch (Exception e) {
            for (int i = movedOnDisk.size() - 1; i >= 0; i--) {
                try {
                    Files.createDirectories(movedOnDisk.get(i)[0].getParent());
                    Files.move(movedOnDisk.get(i)[1], movedOnDisk.get(i)[0]);
                } catch (IOException rollback) {
                    e.addSuppressed(rollback);
                }
            }
            throw e;
        } finally {
            if (index != null) index.unlock();
        }
    }

    /** Refuses a path below {@code root} when any existing component of it is a symbolic link. */
    private static void requireNoLinkComponents(Path root, Path target) throws IOException {
        Path current = root;
        for (Path component : root.relativize(target)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) {
                throw new IOException("Refusing to write through a symbolic link: " + current);
            }
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) return;
        }
    }

    /** Deletes empty folders below and including {@code folder}, deepest first. */
    private static void deleteEmptyFolders(Path folder) throws IOException {
        if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) return;
        try (var paths = Files.walk(folder)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (!Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) continue;
                try (var children = Files.list(p)) {
                    if (children.findAny().isEmpty()) Files.delete(p);
                }
            }
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
     * Imports the XML entities of each tracked project from its folder: its custom path, or {@code
     * <ProjectName>/} at the root. Folders of projects that are no longer tracked are not imported.
     * Folders are resolved below the FileRepository root, so a {@code Path} of {@code "/shared"}
     * does not resolve to the container filesystem root.
     *
     * @return {@code true} when the platform import service was invoked for at least one project;
     *     {@code false} when no tracked project folder contains XML entities
     */
    boolean importProjectEntities() throws Exception {
        // Never import files that still contain conflict markers or a half-finished merge.
        ConflictWorkflow.requireNotInProgress(git);
        List<TrackedProject> projects = requireTrackedProjects();
        IServiceProvider resource = requireSourceControlPermission(IMPORT_SERVICE);
        boolean imported = false;
        for (TrackedProject project : projects) {
            String relative = projectFolder(project);
            File folder =
                    resolveRepositoryPath(new File(fileRepository.getRootPath()).toPath(), relative)
                            .toFile();
            if (!folder.isDirectory() || !containsXmlFile(folder)) continue;
            ValueCollection params = new ValueCollection();
            params.put("repositoryName", new StringPrimitive(fileRepository.getName()));
            params.put("path", new StringPrimitive("/" + relative));
            params.put("useDefaultDataProvider", new BooleanPrimitive(true));
            params.put("withSubsystems", new BooleanPrimitive(false));
            params.put("overwritePropertyValues", new BooleanPrimitive(true));
            resource.processServiceRequest(IMPORT_SERVICE, params);
            imported = true;
        }
        return imported;
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

    /**
     * Collects the regular {@code *.xml} files below a directory. Symbolic links are skipped, both
     * to files and to directories and including the starting directory itself, so a link checked
     * out into the working tree cannot lead outside it.
     */
    static void collectXmlFilesOnDisk(File directory, List<File> result) {
        Deque<File> filesToCheck = new ArrayDeque<>();
        filesToCheck.add(directory);
        while (!filesToCheck.isEmpty()) {
            File file = filesToCheck.removeFirst();
            Path path = file.toPath();
            if (Files.isSymbolicLink(path)) continue;
            if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        filesToCheck.addLast(child);
                    }
                }
            } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                    && file.getName().toLowerCase().endsWith(".xml")) {
                result.add(file);
            }
        }
    }

    /** Returns whether {@link #collectXmlFilesOnDisk(File, List)} would find any file. */
    static boolean containsXmlFile(File directory) {
        List<File> found = new ArrayList<>();
        collectXmlFilesOnDisk(directory, found);
        return !found.isEmpty();
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

    /** Returns whether any segment of a repository-relative path is {@code .git}, in any case. */
    static boolean isGitMetadataPath(String relativePath) {
        for (String segment : relativePath.replace('\\', '/').split("/")) {
            if (".git".equalsIgnoreCase(segment)) return true;
        }
        return false;
    }

    /**
     * Returns the working-tree location of a file that is currently in conflict, for reading or for
     * writing its resolution. Refused are: paths outside the working tree, anything in a {@code
     * .git} folder, symbolic links, anything that is not a regular file, anything reached through a
     * link that leaves the working tree, and files Git does not report as conflicting (this
     * includes conflicts from re-applying carried local changes).
     *
     * @param forWrite when {@code true} the file may be missing, as after a delete/modify conflict;
     *     its nearest existing folder must then lie inside the working tree
     */
    Path requireConflictFile(String path, boolean forWrite) throws Exception {
        String clean = normalizeRepositoryRelativePath(path);
        Path root = new File(fileRepository.getRootPath()).toPath().toRealPath();
        Path file = root.resolve(clean).normalize();
        if (".".equals(clean)
                || isGitMetadataPath(clean)
                || !file.startsWith(root)
                || file.equals(root)) {
            throw new IllegalArgumentException("expected a conflicted file within the repository");
        }
        if (!ConflictWorkflow.unmerged(git).contains(clean)) {
            throw new IllegalArgumentException(clean + " is not a conflicted file.");
        }
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                    || !file.toRealPath().startsWith(root)) {
                throw new IllegalArgumentException("expected a regular file within the repository");
            }
            return file;
        }
        if (!forWrite) throw new IllegalArgumentException("file does not exist");
        Path parent = file.getParent();
        while (!Files.exists(parent, LinkOption.NOFOLLOW_LINKS)) parent = parent.getParent();
        if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)
                || !parent.toRealPath().startsWith(root)) {
            throw new IllegalArgumentException("expected a file within the repository");
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

    /** Folder next to the platform's {@code repository} folder that holds every Git directory. */
    static final String GIT_STORAGE_FOLDER = "jgit";

    private Git openGit() throws Exception {
        // Each service creates its own manager. Serialize initialization so concurrent reads
        // do not contend for the same config.lock while applying repository settings.
        synchronized (FileRepositoryManager.class) {
            File directory = new File(fileRepository.getRootPath());
            File gitDirectory = gitDirectoryFor(directory);
            if (migrateInTreeGitDirectory(directory, gitDirectory)) {
                com.thingworx.logging.LogUtilities.getInstance()
                        .getApplicationLogger(FileRepositoryManager.class)
                        .warn(
                                "Moved the Git directory of "
                                        + fileRepository.getName()
                                        + " out of its FileRepository folder to "
                                        + gitDirectory);
            }
            Git opened = openRepository(directory, gitDirectory, initialBranch());
            // The Git config is the only record of remotes and Git settings. It is written when
            // the repository is created, and once more to take over the URL that earlier
            // versions kept in the GitRepoURL Thing property.
            String legacyUrl = gitRepoUrl();
            StoredConfig config = opened.getRepository().getConfig();
            if (Remotes.migrateLegacyUrl(config, legacyUrl)) config.save();
            if (!legacyUrl.isBlank() && Remotes.sanitizeLegacyUrl(legacyUrl) != null) {
                clearLegacyUrl();
            }
            migrateLegacyProjectSettingsOnOpen();
            return opened;
        }
    }

    /** The Git directory of this repository, which lies outside the FileRepository folder. */
    File gitDirectory() {
        return git.getRepository().getDirectory();
    }

    /**
     * Returns where the Git directory of a FileRepository lives: {@code
     * <storage>/jgit/<folder>.git}, where the FileRepository root is {@code
     * <storage>/repository/<folder>}. Every FileRepository root is a direct child of the platform's
     * {@code repository} folder, so a sibling of that folder is outside all of them and the
     * inherited file services of no FileRepository Thing can read or write Git metadata. The folder
     * name is the one the platform already encoded into a single path component.
     */
    static File gitDirectoryFor(File workTree) throws IOException {
        Path root = workTree.toPath().toAbsolutePath().normalize();
        Path repositories = root.getParent();
        Path storage = repositories == null ? null : repositories.getParent();
        Path name = root.getFileName();
        if (storage == null || name == null || name.toString().isBlank()) {
            throw new IOException(
                    Const.ERR_PREFIX_SYSTEM
                            + "Cannot place the Git directory outside the FileRepository folder "
                            + workTree);
        }
        return storage.resolve(GIT_STORAGE_FOLDER).resolve(name + ".git").toFile();
    }

    /**
     * Moves a Git directory that an earlier version kept at {@code <workTree>/.git} to its location
     * outside the FileRepository folder. Runs once: only when {@code .git} is a real directory and
     * the outside Git directory does not exist yet. A {@code .git} that is a file or a symbolic
     * link is never adopted.
     *
     * @return {@code true} when a directory was moved
     */
    static boolean migrateInTreeGitDirectory(File workTree, File gitDirectory) throws IOException {
        Path legacy = workTree.toPath().resolve(".git");
        Path target = gitDirectory.toPath();
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                || !Files.isDirectory(legacy, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        Files.createDirectories(target.getParent());
        try {
            Files.move(legacy, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException differentFileStore) {
            // Fall through: copy to a temporary sibling, verify, publish it, then delete.
        }
        Path temporary = target.resolveSibling(target.getFileName() + ".migrating");
        deleteRecursively(temporary);
        List<String> copied = copyTree(legacy, temporary);
        if (!copied.equals(describeTree(temporary))) {
            deleteRecursively(temporary);
            throw new IOException(
                    Const.ERR_PREFIX_SYSTEM
                            + "Copying the Git directory out of "
                            + workTree
                            + " could not be verified; nothing was changed.");
        }
        Files.move(temporary, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        deleteRecursively(legacy);
        return true;
    }

    /** Copies a tree without following links and returns the description of the source. */
    private static List<String> copyTree(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path copy = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createDirectories(copy);
                } else {
                    Files.copy(
                            path,
                            copy,
                            LinkOption.NOFOLLOW_LINKS,
                            java.nio.file.StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
        return describeTree(source);
    }

    /** Lists every entry below a directory with its kind and, for files, size and content hash. */
    private static List<String> describeTree(Path root) throws IOException {
        List<String> entries = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted().toList()) {
                String relative = root.relativize(path).toString();
                if (Files.isSymbolicLink(path)) {
                    entries.add("l " + relative + " " + Files.readSymbolicLink(path));
                } else if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    entries.add("d " + relative);
                } else {
                    try {
                        byte[] hash =
                                java.security.MessageDigest.getInstance("SHA-256")
                                        .digest(Files.readAllBytes(path));
                        entries.add(
                                "f "
                                        + relative
                                        + " "
                                        + Files.size(path)
                                        + " "
                                        + java.util.HexFormat.of().formatHex(hash));
                    } catch (java.security.NoSuchAlgorithmException e) {
                        throw new IOException(e);
                    }
                }
            }
        }
        return entries;
    }

    /**
     * Opens, or creates, a repository whose Git directory and working tree are both given
     * explicitly. Nothing is discovered from the file system or read from configuration:
     *
     * <ul>
     *   <li>a {@code .git} entry in the working tree, whether a folder, a {@code gitdir:} pointer
     *       file or a symbolic link, is never consulted;
     *   <li>{@code core.worktree} cannot move the working tree, because JGit only reads it when no
     *       working tree was set on the builder;
     *   <li>a {@code commondir} file cannot move the object store or the config, because the common
     *       directory is set to the Git directory.
     * </ul>
     *
     * The Git directory must be a real directory outside the working tree.
     */
    static Git openRepository(File workTree, File gitDirectory, String initialBranch)
            throws Exception {
        Path tree = workTree.toPath().toAbsolutePath().normalize();
        Path metadata = gitDirectory.toPath().toAbsolutePath().normalize();
        if (metadata.startsWith(tree)) {
            throw new IOException(
                    Const.ERR_PREFIX_SYSTEM
                            + "The Git directory must be outside the FileRepository folder.");
        }
        boolean exists = Files.exists(metadata, LinkOption.NOFOLLOW_LINKS);
        if (exists && !Files.isDirectory(metadata, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    Const.ERR_PREFIX_SYSTEM
                            + "The Git directory "
                            + metadata
                            + " is not a directory; refusing to follow it.");
        }
        org.eclipse.jgit.util.FS fs = HardenedFs.create();
        boolean created =
                !exists
                        || !org.eclipse.jgit.lib.RepositoryCache.FileKey.isGitRepository(
                                metadata.toFile(), fs);
        if (!created) {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            builder.setFS(fs);
            builder.setGitDir(metadata.toFile());
            builder.setGitCommonDir(metadata.toFile());
            builder.setWorkTree(tree.toFile());
            builder.setMustExist(true);
            Git opened = new Git(builder.build(), true);
            StoredConfig config = opened.getRepository().getConfig();
            RepositorySecurity.harden(config);
            RepositorySecurity.validate(config);
            return opened;
        }
        Files.createDirectories(tree);
        Files.createDirectories(metadata.getParent());
        Git opened =
                Git.init()
                        .setFs(fs)
                        .setGitDir(metadata.toFile())
                        .setDirectory(tree.toFile())
                        .setInitialBranch(initialBranch)
                        .call();
        // For a separate Git directory JGit records core.worktree and writes a "gitdir:" pointer
        // file into the working tree. Neither is used here, and the pointer would only tell
        // FileRepository users where the Git directory is, so both are removed again.
        Path pointer = tree.resolve(".git");
        if (Files.isRegularFile(pointer, LinkOption.NOFOLLOW_LINKS)
                && Files.size(pointer) < 8192
                && Files.readString(pointer, StandardCharsets.UTF_8).startsWith("gitdir:")) {
            Files.delete(pointer);
        }
        StoredConfig config = opened.getRepository().getConfig();
        config.unset("core", null, "worktree");
        config.setString("core", null, "autocrlf", "input");
        config.setBoolean("repack", null, "writeBitmaps", false);
        RepositorySecurity.harden(config);
        RepositorySecurity.validate(config);
        config.save();
        return opened;
    }

    private void clearLegacyUrl() {
        try {
            fileRepository.setPropertyValue(Const.GitRepoURL, new StringPrimitive(""));
        } catch (Exception e) {
            // The URL is already in the Git config; the stale property is ignored from now on.
            com.thingworx.logging.LogUtilities.getInstance()
                    .getApplicationLogger(FileRepositoryManager.class)
                    .warn(
                            "Could not clear the retired GitRepoURL property of "
                                    + fileRepository.getName(),
                            e);
        }
    }

    /** Closes the JGit handle when a caller explicitly owns a Git operation. */
    public void close() {
        git.close();
    }
}
