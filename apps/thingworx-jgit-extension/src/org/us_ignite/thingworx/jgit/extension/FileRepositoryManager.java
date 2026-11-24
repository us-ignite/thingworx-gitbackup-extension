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
                            : new TrackedProject(
                                    project.name(),
                                    base + "/" + project.name(),
                                    project.entityTypes()));
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
     * A tracked project, its optional custom entity folder, and the entity types it synchronizes. A
     * blank {@code path} means the default folder {@code <ProjectName>} at the repository root;
     * otherwise it is the folder that holds the project's entities, relative to the repository
     * root, normalized without leading or trailing slashes. {@code entityTypes} is a
     * comma-separated list of collections, such as {@code Things,Mashups}; blank means every type
     * (see {@link SyncedEntityTypes}).
     */
    record TrackedProject(String name, String path, String entityTypes) {
        TrackedProject {
            path = path == null ? "" : path;
            entityTypes = entityTypes == null ? "" : entityTypes;
        }

        /** A project that synchronizes every entity type. */
        TrackedProject(String name, String path) {
            this(name, path, "");
        }

        boolean hasCustomPath() {
            return !path.isEmpty();
        }

        /** Whether this project synchronizes the collection, such as {@code Things}. */
        boolean syncs(String collection) {
            return SyncedEntityTypes.syncs(entityTypes, collection);
        }

        /** Whether this project synchronizes only some entity types. */
        boolean filtersEntityTypes() {
            return !entityTypes.isEmpty();
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
                // Rows written before the EntityTypes column existed sync every type.
                String types =
                        row.containsKey(Const.TrackedProjectEntityTypes)
                                ? row.getStringValue(Const.TrackedProjectEntityTypes)
                                : "";
                projects.putIfAbsent(
                        name.trim(),
                        new TrackedProject(
                                name.trim(),
                                path == null ? "" : path.trim(),
                                types == null ? "" : types.trim()));
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
     * Validates names, paths and entity types and drops repeated names, keeping the first. Paths
     * and entity types are returned in normalized form.
     */
    static List<TrackedProject> normalizeTrackedProjects(List<TrackedProject> projects) {
        java.util.LinkedHashMap<String, TrackedProject> result = new java.util.LinkedHashMap<>();
        for (TrackedProject project : projects) {
            String name = validateProjectFolderName(project.name());
            result.putIfAbsent(
                    name,
                    new TrackedProject(
                            name,
                            validateProjectPath(project.path()),
                            SyncedEntityTypes.normalize(project.entityTypes())));
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
        // object, so add fields that are missing rather than dropping the values.
        addStringFieldIfMissing(table, Const.TrackedProjectPath, 2);
        addStringFieldIfMissing(table, Const.TrackedProjectEntityTypes, 3);
        for (TrackedProject project : projects) {
            ValueCollection row = new ValueCollection();
            row.put(Const.ProjectName, new StringPrimitive(project.name()));
            row.put(Const.TrackedProjectPath, new StringPrimitive(project.path()));
            row.put(Const.TrackedProjectEntityTypes, new StringPrimitive(project.entityTypes()));
            table.addRow(row);
        }
        return table;
    }

    private static void addStringFieldIfMissing(InfoTable table, String name, int ordinal) {
        if (table.hasField(name)) return;
        var field =
                new com.thingworx.metadata.FieldDefinition(
                        name, com.thingworx.types.BaseTypes.STRING);
        field.setOrdinal(ordinal);
        table.addField(field);
    }

    /**
     * A tracked Data Table: a {@code Data/<name>.json} file in a tracked project's folder in the
     * working tree. {@code inGit} tells whether Git tracks the file, staged or committed; only then
     * does Sync to ThingWorx import it.
     */
    record DataTableFile(String name, String project, String file, boolean inGit) {}

    /**
     * The tracked Data Tables: the {@code Data/<name>.json} files in the folders of the tracked
     * projects, in project order and then by name. A name found in more than one folder is tracked
     * from the first; each later file is added to {@code notes}. Symbolic links and other files are
     * ignored.
     */
    List<DataTableFile> dataTableFiles(List<TrackedProject> projects, List<String> notes)
            throws IOException {
        Path root = git.getRepository().getWorkTree().toPath().toRealPath();
        java.util.Set<String> inGit = trackedPaths();
        java.util.LinkedHashMap<String, DataTableFile> tables = new java.util.LinkedHashMap<>();
        for (TrackedProject project : projects) {
            String folder;
            Path data;
            try {
                folder = projectFolder(project);
                data = resolveRepositoryPath(root, folder + "/" + TrackedDataTables.DATA_FOLDER);
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (!Files.isDirectory(data, LinkOption.NOFOLLOW_LINKS)) continue;
            List<Path> files;
            try (var list = Files.list(data)) {
                files = list.sorted().toList();
            }
            for (Path path : files) {
                String fileName = path.getFileName().toString();
                if (!fileName.endsWith(".json")
                        || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) continue;
                String name = fileName.substring(0, fileName.length() - ".json".length());
                String relative;
                try {
                    relative = TrackedDataTables.relativeFile(folder, name);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (tables.containsKey(name)) {
                    notes.add(
                            name
                                    + " (also in "
                                    + relative
                                    + "; "
                                    + tables.get(name).file()
                                    + " is used)");
                    continue;
                }
                tables.put(
                        name,
                        new DataTableFile(
                                name, project.name(), relative, inGit.contains(relative)));
            }
        }
        return List.copyOf(tables.values());
    }

    /** The tracked Data Tables of the tracked projects. */
    List<DataTableFile> dataTableFiles() throws IOException {
        return dataTableFiles(trackedProjectEntries(), new ArrayList<>());
    }

    /** The tracked Data Table named {@code name}, or an error saying it is not tracked. */
    private DataTableFile requireDataTableFile(String name) throws IOException {
        for (DataTableFile item : dataTableFiles()) {
            if (item.name().equals(name)) return item;
        }
        throw new IllegalArgumentException("Data Table '" + name + "' is not tracked.");
    }

    /**
     * Builds a {@code GIT.TrackedDataTable.DataShape} table. Each row's {@code Mode} is read from
     * its file; a file that cannot be read shows its error instead.
     */
    InfoTable dataTableTable(List<DataTableFile> tables) throws Exception {
        Path root = git.getRepository().getWorkTree().toPath().toRealPath();
        InfoTable table =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.TrackedDataTableDataShapeName);
        for (DataTableFile item : tables) {
            String mode = "";
            Path file = resolveRepositoryPath(root, item.file());
            if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    mode =
                            TrackedDataTables.modeOf(
                                    Files.readString(file, StandardCharsets.UTF_8), item.name());
                } catch (Exception e) {
                    mode = "Invalid file: " + e.getMessage();
                }
            }
            ValueCollection row = new ValueCollection();
            row.put(Const.DataTableName, new StringPrimitive(item.name()));
            row.put(Const.ProjectName, new StringPrimitive(item.project()));
            row.put(Const.TrackedDataTableMode, new StringPrimitive(mode));
            row.put(Const.TrackedDataTableFile, new StringPrimitive(item.file()));
            row.put(Const.TrackedDataTableInGit, new BooleanPrimitive(item.inGit()));
            table.addRow(row);
        }
        return table;
    }

    /**
     * The Data Tables of the tracked projects that are not tracked yet, each with the file that
     * tracking it would write.
     */
    List<DataTableFile> availableDataTables() throws Exception {
        java.util.Set<String> tracked = new java.util.HashSet<>();
        for (DataTableFile item : dataTableFiles()) tracked.add(item.name());
        List<DataTableFile> available = new ArrayList<>();
        for (TrackedProject project : trackedProjectEntries()) {
            String folder;
            try {
                folder = projectFolder(project);
            } catch (IllegalArgumentException e) {
                continue;
            }
            for (String name : TrackedDataTables.dataTablesOfProject(project.name())) {
                String relative;
                try {
                    relative = TrackedDataTables.relativeFile(folder, name);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (tracked.add(name)) {
                    available.add(new DataTableFile(name, project.name(), relative, false));
                }
            }
        }
        available.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name()));
        return available;
    }

    /**
     * Tracks a Data Table by writing its rows, with {@code mode}, to {@code Data/<name>.json} in
     * its project's folder. The file is left untracked by Git for review in Changes.
     *
     * @return the repository-relative file
     */
    String trackDataTable(com.thingworx.datatables.DataTableThing table, String mode)
            throws Exception {
        String project = table.GetProjectName();
        TrackedProject owner =
                trackedProjectEntries().stream()
                        .filter(p -> p.name().equals(project))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Data Table '"
                                                        + table.getName()
                                                        + "' belongs to project '"
                                                        + project
                                                        + "', which this repository does not"
                                                        + " track. Track the project first; the"
                                                        + " rows are stored in its folder."));
        synchronized (StatusExportReconciler.class) {
            for (DataTableFile item : dataTableFiles()) {
                if (item.name().equals(table.getName())) {
                    throw new IllegalArgumentException(
                            "Data Table '" + table.getName() + "' is already tracked.");
                }
            }
            String relative = TrackedDataTables.relativeFile(projectFolder(owner), table.getName());
            String content = TrackedDataTables.export(table, mode);
            writeDataTableFile(relative, content);
            recordDataTableHash(table.getName(), content);
            return relative;
        }
    }

    /** Sets the {@code mode} in a tracked Data Table's file; nothing else in it changes. */
    String setDataTableMode(String name, String mode) throws Exception {
        synchronized (StatusExportReconciler.class) {
            DataTableFile item = requireDataTableFile(name);
            Path root = git.getRepository().getWorkTree().toPath().toRealPath();
            Path file = resolveRepositoryPath(root, item.file());
            requireNoLinkComponents(root, file);
            String content = Files.readString(file, StandardCharsets.UTF_8);
            writeDataTableFile(item.file(), TrackedDataTables.withMode(content, name, mode));
            return item.file();
        }
    }

    /**
     * Stops tracking a Data Table by deleting its file from the working tree. The deletion is left
     * unstaged for review in Changes; the rows in ThingWorx are not changed.
     *
     * @return the repository-relative file that was deleted
     */
    String untrackDataTable(String name) throws Exception {
        synchronized (StatusExportReconciler.class) {
            DataTableFile item = requireDataTableFile(name);
            Path root = git.getRepository().getWorkTree().toPath().toRealPath();
            Path file = resolveRepositoryPath(root, item.file());
            requireNoLinkComponents(root, file);
            Files.delete(file);
            StoredConfig config = git.getRepository().getConfig();
            config.unsetSection(DATA_SYNC_SECTION, name);
            config.save();
            return item.file();
        }
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

    /**
     * Exports a project into {@code <temporary>/<slot>/<ProjectName>/}, removes volatile metadata
     * and the entity types the project does not synchronize, and returns that folder.
     */
    private Path exportSnapshot(Path temporary, String slot, TrackedProject tracked)
            throws Exception {
        String project = tracked.name();
        String exportDirectory = "/" + temporary.getFileName() + "/" + slot;
        exportProjectEntitiesTo(exportDirectory, project);
        Path snapshot = temporary.resolve(slot).resolve(project).normalize();
        if (!snapshot.startsWith(temporary) || !Files.isDirectory(snapshot)) {
            throw new IOException(
                    "ThingWorx did not produce the expected export for project '" + project + "'.");
        }
        removeUnsyncedCollections(snapshot, tracked);
        sanitizeXmlIn(snapshot.toFile());
        // Users are exported without their password hash, account state or secrets.
        UserSync.sanitizeExportIn(snapshot, UserSync.passwordProperties());
        return snapshot;
    }

    /**
     * Deletes the entries of an export that the project does not synchronize: collection folders of
     * other entity types, and, when only some types are synchronized, files outside any collection
     * folder.
     */
    static void removeUnsyncedCollections(Path snapshot, TrackedProject project)
            throws IOException {
        if (!project.filtersEntityTypes()) return;
        try (var children = Files.list(snapshot)) {
            for (Path child : children.toList()) {
                boolean folder = Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS);
                if (!project.syncs(folder ? child.getFileName().toString() : null)) {
                    deleteRecursively(child);
                }
            }
        }
    }

    /**
     * The collection of a path relative to a project folder: its first segment, or {@code null} for
     * a file directly in the project folder.
     */
    static String collectionOfPath(String relative) {
        String clean = relative.replace('\\', '/');
        int slash = clean.indexOf('/');
        return slash <= 0 ? null : clean.substring(0, slash);
    }

    private static final String EXPORT_SERVICE = "ExportSourceControlledEntities";
    private static final String IMPORT_SERVICE = "ImportSourceControlledEntities";

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
        exportProjectEntitiesTo(path, project, null);
    }

    /**
     * Exports one project, or, when {@code collection} is given, only that collection of it, such
     * as {@code Things}, into {@code <path>/<project>/}.
     */
    private void exportProjectEntitiesTo(String path, String project, String collection)
            throws Exception {
        IServiceProvider sourceControlFunctions = requireSourceControlPermission(EXPORT_SERVICE);
        resolveRepositoryPath(
                new File(fileRepository.getRootPath()).toPath(),
                validateRepositoryPath(path) + "/" + validateProjectFolderName(project));

        ValueCollection params = new ValueCollection();
        params.put("repositoryName", new StringPrimitive(fileRepository.getName()));
        params.put("path", new StringPrimitive(path));
        params.put("projectName", new StringPrimitive(project));
        params.put("includeDependents", new BooleanPrimitive(false));
        if (collection != null) params.put("collection", new StringPrimitive(collection));
        sourceControlFunctions.processServiceRequest(EXPORT_SERVICE, params);
    }

    /**
     * The outcome of {@link #syncFromThingWorx()}: how many tracked projects and Data Tables were
     * exported, in whole or in part, and every item that could not be exported, each with the
     * reason.
     */
    record ExportResult(int projects, int dataTables, SyncFailures failures) {
        /** Whether anything at all was exported. */
        boolean exported() {
            return projects > 0 || dataTables > 0;
        }
    }

    /**
     * Sync from ThingWorx: exports every tracked project and reconciles its folder in the working
     * tree, without staging anything; only the cached file stats of unchanged files are refreshed
     * (see {@link IndexStatRefresh}). Files of entities that left a project are deleted.
     *
     * <p>The export succeeds partially. A project whose export fails is exported one collection at
     * a time instead; a project, collection or Data Table that still cannot be exported keeps its
     * files in the working tree as they were, never deleted, and is reported in {@link
     * ExportResult#failures()}. The sync time (see {@link #changedSinceSync()}) is only recorded
     * when everything was exported, so ThingWorx changes that did not reach the working tree are
     * still protected from being overwritten by Sync to ThingWorx.
     */
    ExportResult syncFromThingWorx() throws Exception {
        requireExportPermissions();
        ConflictWorkflow.requireNotInProgress(git);
        // Taken before exporting, so an entity saved while the export runs still counts as changed.
        long started = System.currentTimeMillis();
        ExportResult result;
        synchronized (StatusExportReconciler.class) {
            result = syncFromThingWorxLocked();
        }
        if (result.failures().isEmpty()) recordSync(started);
        return result;
    }

    /** A project's export: the folder it was exported to, and its collections that failed. */
    private record Snapshot(Path folder, java.util.Set<String> failedCollections) {}

    private ExportResult syncFromThingWorxLocked() throws Exception {
        List<TrackedProject> projects = requireTrackedProjects();
        Path base = new File(fileRepository.getRootPath()).toPath().toRealPath();
        Path temporary = Files.createTempDirectory(base, ".jgit-status-");
        SyncFailures failures = new SyncFailures();
        try {
            // Export every project before reconciling any, so a project's files are only touched
            // once ThingWorx has produced its export. Each project gets its own export directory;
            // a project that cannot be exported at all gets none and is left alone.
            List<Snapshot> snapshots = new ArrayList<>();
            for (int i = 0; i < projects.size(); i++) {
                snapshots.add(
                        exportOrPartial(temporary, String.valueOf(i), projects.get(i), failures));
            }
            // Read every tracked Data Table before writing any file, like the projects above.
            List<String> skipped = new ArrayList<>();
            List<DataTableFile> tables = new ArrayList<>();
            List<String> contents = new ArrayList<>();
            Path workTree = git.getRepository().getWorkTree().toPath().toRealPath();
            for (DataTableFile item : dataTableFiles(projects, skipped)) {
                var thing = TrackedDataTables.find(item.name());
                if (thing == null) {
                    // Kept, so a table deleted or not yet imported here keeps its rows in Git.
                    skipped.add(item.name() + " (no such Data Table; " + item.file() + " is kept)");
                    continue;
                }
                // Each Data Table on its own: one that fails keeps its file as it is.
                try {
                    String current =
                            Files.readString(
                                    resolveRepositoryPath(workTree, item.file()),
                                    StandardCharsets.UTF_8);
                    String mode;
                    try {
                        mode = TrackedDataTables.modeOf(current, item.name());
                    } catch (IllegalArgumentException e) {
                        throw new Exception(Const.ERR_PREFIX_CONFIG + e.getMessage(), e);
                    }
                    contents.add(TrackedDataTables.export(thing, mode));
                    tables.add(item);
                } catch (Exception e) {
                    failures.add(item.project(), TrackedDataTables.DATA_FOLDER, item.name(), e);
                    warn("Could not export the rows of Data Table '{}' from {}", item.name(), e);
                }
            }
            int exportedProjects = 0;
            for (int i = 0; i < projects.size(); i++) {
                TrackedProject project = projects.get(i);
                Snapshot snapshot = snapshots.get(i);
                if (snapshot == null) continue;
                java.util.Set<String> failed = snapshot.failedCollections();
                StatusExportReconciler.reconcileFolder(
                        git,
                        snapshot.folder(),
                        projectFolder(project),
                        project.name(),
                        // A failed collection keeps its files; so do files outside any collection,
                        // which only a complete export can account for.
                        collection ->
                                project.syncs(collection)
                                        && (failed.isEmpty()
                                                || (collection != null
                                                        && !failed.contains(collection))));
                exportedProjects++;
            }
            for (int i = 0; i < tables.size(); i++) {
                writeDataTableFile(tables.get(i).file(), contents.get(i));
                recordDataTableHash(tables.get(i).name(), contents.get(i));
            }
            if (!skipped.isEmpty()) {
                GitActionLogger.logAction(
                        "SyncFromThingWorx",
                        fileRepository.getName(),
                        "Data Tables not exported: " + String.join(", ", skipped));
            }
            // Keeps Status fast: files that are unchanged but were rewritten (by an earlier
            // version of this sync, or by hand) would otherwise be re-hashed on every Status.
            IndexStatRefresh.refresh(git);
            return new ExportResult(exportedProjects, tables.size(), failures);
        } finally {
            deleteRecursively(temporary);
        }
    }

    /**
     * Exports a project as {@link #exportSnapshot} does. When the platform refuses to export the
     * whole project, exports it one collection at a time instead, see {@link #exportCollections}.
     *
     * @return the export, or {@code null} when nothing of the project could be exported
     */
    private Snapshot exportOrPartial(
            Path temporary, String slot, TrackedProject tracked, SyncFailures failures) {
        try {
            return new Snapshot(exportSnapshot(temporary, slot, tracked), java.util.Set.of());
        } catch (Exception whole) {
            warn(
                    "Exporting project '{}' from {} as a whole failed; exporting it one"
                            + " collection at a time",
                    tracked.name(),
                    whole);
            try {
                Snapshot partial = exportCollections(temporary, slot, tracked, failures);
                if (partial != null) {
                    // Every collection failed and each was reported: nothing to reconcile.
                    return partial.folder() == null ? null : partial;
                }
            } catch (Exception e) {
                whole.addSuppressed(e);
            }
            failures.add(tracked.name(), "", "", whole);
            return null;
        }
    }

    /**
     * Exports each collection of a project the project synchronizes, such as {@code Things}, on its
     * own, and puts them together into {@code <temporary>/<slot>/<ProjectName>/} like a whole
     * export. Every collection that fails is added to {@code failures}.
     *
     * @return the export with its failed collections, with a {@code null} folder when every
     *     collection failed; or {@code null} when the project or its collections cannot be found,
     *     with nothing added to {@code failures}
     */
    private Snapshot exportCollections(
            Path temporary, String slot, TrackedProject tracked, SyncFailures failures)
            throws Exception {
        String project = tracked.name();
        RootEntity found = EntityUtilities.findEntity(project, ThingworxRelationshipTypes.Project);
        if (!(found instanceof com.thingworx.projects.Project entity)) return null;
        java.util.Set<String> collections = new java.util.TreeSet<>();
        collections.add(SyncedEntityTypes.PROJECTS);
        for (var reference : entity.getProjectEntities()) {
            collections.add(SyncedEntityTypes.collectionOf(reference.getType()));
        }
        collections.removeIf(collection -> !tracked.syncs(collection));

        Path snapshot = temporary.resolve(slot).resolve(project).normalize();
        if (!snapshot.startsWith(temporary)) throw new IOException("Invalid project name.");
        // Whatever the failed whole export left behind is not part of this one.
        deleteRecursively(temporary.resolve(slot));
        Files.createDirectories(snapshot);
        java.util.Set<String> failed = new java.util.TreeSet<>();
        int n = 0;
        for (String collection : collections) {
            String part = slot + "-" + n++;
            try {
                exportProjectEntitiesTo(
                        "/" + temporary.getFileName() + "/" + part, project, collection);
                Path exported = temporary.resolve(part).resolve(project).resolve(collection);
                if (Files.isDirectory(exported, LinkOption.NOFOLLOW_LINKS)) {
                    Files.move(exported, snapshot.resolve(collection));
                }
            } catch (Exception e) {
                failed.add(collection);
                failures.add(project, collection, "", e);
                warn("Could not export collection '{}' from {}", project + "/" + collection, e);
            } finally {
                deleteRecursively(temporary.resolve(part));
            }
        }
        if (failed.size() == collections.size()) return new Snapshot(null, failed);
        removeUnsyncedCollections(snapshot, tracked);
        sanitizeXmlIn(snapshot.toFile());
        UserSync.sanitizeExportIn(snapshot, UserSync.passwordProperties());
        return new Snapshot(snapshot, failed);
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
        TrackedProject updated =
                new TrackedProject(
                        current.name(), validateProjectPath(path), current.entityTypes());
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
     * <p>Only files tracked by Git, committed or staged, are imported. An untracked file has never
     * been through Git, so it is left out and reported; add, commit and push it to include it.
     *
     * <p>The import succeeds partially: a User, entity, project or Data Table that cannot be
     * imported is reported in {@link ImportResult#failures()} and the rest are still imported. A
     * project is imported as a whole first; when that fails, its entities are imported one at a
     * time, see {@link #importTracked}.
     *
     * @return whether anything was imported, the untracked files that were left out, and what
     *     failed
     */
    ImportResult importProjectEntities() throws Exception {
        // Never import files that still contain conflict markers or a half-finished merge.
        ConflictWorkflow.requireNotInProgress(git);
        List<TrackedProject> projects = requireTrackedProjects();
        IServiceProvider resource = requireSourceControlPermission(IMPORT_SERVICE);
        java.util.Set<String> trackedPaths = trackedPaths();
        SyncFailures failures = new SyncFailures();
        // Users first, so Groups and Organizations imported below can refer to them.
        UserImport users = importUsers(resource, projects, trackedPaths, failures);
        boolean imported = !users.imported().isEmpty();
        List<String> untracked = new ArrayList<>();
        for (TrackedProject project : projects) {
            try {
                String relative = projectFolder(project);
                File folder =
                        resolveRepositoryPath(
                                        new File(fileRepository.getRootPath()).toPath(), relative)
                                .toFile();
                if (!folder.isDirectory() || !containsXmlFile(folder)) continue;
                if (importTracked(
                        resource,
                        folder.toPath(),
                        relative,
                        project,
                        trackedPaths,
                        untracked,
                        failures)) {
                    imported = true;
                }
            } catch (Exception e) {
                failures.add(project.name(), "", "", e);
                warn("Could not import project '{}' into {}", project.name(), e);
            }
        }
        // After the entities, so each Data Table and its DataShape are current before its rows.
        DataImport data = importDataTables(projects, trackedPaths, untracked, failures);
        if (!data.imported().isEmpty()) imported = true;
        // Imported entities now carry modified dates up to this point, so take it afterwards. An
        // entity that failed keeps the ThingWorx copy, which was not modified, so it is not
        // reported as a ThingWorx change; Sync from ThingWorx shows it against Git instead.
        recordSync(System.currentTimeMillis());
        return new ImportResult(
                imported, untracked, users.created(), data.imported(), data.notes(), failures);
    }

    /**
     * The outcome of {@link #importProjectEntities()}: whether anything was imported, the
     * repository-relative paths of untracked files that were left out, the Users that were created
     * disabled, the Data Tables whose rows were imported and those left alone, and every item that
     * could not be imported, each with the reason.
     */
    record ImportResult(
            boolean imported,
            List<String> untracked,
            List<String> createdUsers,
            List<String> dataTables,
            List<String> dataTableNotes,
            SyncFailures failures) {}

    /** The Data Tables whose rows were imported, and those left alone with why. */
    record DataImport(List<String> imported, List<String> notes) {}

    /**
     * Imports the rows of every tracked Data Table from its file, in the mode the file names, when
     * Git tracks the file; an untracked file is reported like any other untracked file.
     */
    private DataImport importDataTables(
            List<TrackedProject> projects,
            java.util.Set<String> trackedPaths,
            List<String> untracked,
            SyncFailures failures)
            throws Exception {
        List<String> notes = new ArrayList<>();
        List<String> imported = new ArrayList<>();
        Path root = git.getRepository().getWorkTree().toPath().toRealPath();
        for (DataTableFile item : dataTableFiles(projects, notes)) {
            String name = item.name();
            if (!trackedPaths.contains(item.file())) {
                untracked.add(item.file());
                continue;
            }
            // Each Data Table on its own: one that fails is reported and the others still import.
            try {
                Path file = resolveRepositoryPath(root, item.file());
                requireNoLinkComponents(root, file);
                var thing = TrackedDataTables.find(name);
                if (thing == null) {
                    notes.add(name + " (the Data Table does not exist)");
                    continue;
                }
                String content = Files.readString(file, StandardCharsets.UTF_8);
                boolean mirror =
                        TrackedDataTables.MIRROR.equals(TrackedDataTables.modeOf(content, name));
                imported.add(TrackedDataTables.importRows(thing, content, mirror).describe());
                recordDataTableHash(
                        name, TrackedDataTables.export(thing, TrackedDataTables.MIRROR));
            } catch (Exception e) {
                failures.add(item.project(), TrackedDataTables.DATA_FOLDER, name, e);
                warn("Could not import the rows of Data Table '{}' into {}", name, e);
            }
        }
        return new DataImport(imported, notes);
    }

    /** Logs a warning about this repository, naming {@code subject}, with the exception. */
    private void warn(String message, String subject, Exception e) {
        com.thingworx.logging.LogUtilities.getInstance()
                .getApplicationLogger(FileRepositoryManager.class)
                .warn(message, subject, fileRepository.getName(), e);
    }

    /** The Users imported, and those of them created disabled. */
    record UserImport(List<String> imported, List<String> created) {}

    /**
     * Imports the tracked Users of every tracked project, each on its own, with what the server
     * holds for it kept: see {@link UserSync}. A User that fails to import is added to {@code
     * failures} and the others still import; a server whose licence has no named users cannot
     * create one, for example. Each User is staged in its own temporary folder, deleted as soon as
     * it is imported, because the staged file carries the live password hash and server-encrypted
     * secrets.
     */
    private UserImport importUsers(
            IServiceProvider resource,
            List<TrackedProject> projects,
            java.util.Set<String> trackedPaths,
            SyncFailures failures)
            throws Exception {
        java.util.Map<String, UserSync.GitUser> users = new java.util.LinkedHashMap<>();
        java.util.Map<String, String> projectOf = new java.util.HashMap<>();
        Path root = new File(fileRepository.getRootPath()).toPath();
        for (TrackedProject project : projects) {
            if (!project.syncs(UserSync.USERS)) continue;
            String relative = projectFolder(project);
            Path folder = resolveRepositoryPath(root, relative);
            if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) continue;
            String prefix = relative.replace('\\', '/').replaceAll("^/+|/+$", "") + "/";
            for (UserSync.GitUser user :
                    UserSync.collect(folder, path -> trackedPaths.contains(prefix + path))) {
                if (users.putIfAbsent(user.name(), user) == null) {
                    projectOf.put(user.name(), project.name());
                }
            }
        }
        List<String> imported = new ArrayList<>();
        List<String> created = new ArrayList<>();
        if (users.isEmpty()) return new UserImport(imported, created);
        var isPasswordProperty = UserSync.passwordProperties();
        Path base = root.toRealPath();
        for (UserSync.GitUser user : users.values()) {
            Path temporary = Files.createTempDirectory(base, ".jgit-user-");
            try {
                UserSync.LiveUser live = UserSync.readLive(user.name());
                Path file = temporary.resolve("0").resolve(UserSync.USERS).resolve("User.xml");
                Files.createDirectories(file.getParent());
                Files.writeString(
                        file,
                        UserSync.prepareImport(user, live, isPasswordProperty),
                        StandardCharsets.UTF_8);
                importFolder(resource, "/" + temporary.getFileName() + "/0");
                imported.add(user.name());
                if (live == null) created.add(user.name());
            } catch (Exception e) {
                failures.add(projectOf.get(user.name()), UserSync.USERS, user.name(), e);
                warn("Could not import User '{}' into {}", user.name(), e);
            } finally {
                deleteRecursively(temporary);
            }
        }
        return new UserImport(imported, created);
    }

    /** The repository-relative paths of every file in the Git index. */
    private java.util.Set<String> trackedPaths() throws IOException {
        var index = git.getRepository().readDirCache();
        java.util.Set<String> paths = new java.util.HashSet<>();
        for (int i = 0; i < index.getEntryCount(); i++)
            paths.add(index.getEntry(i).getPathString());
        return paths;
    }

    /** Git config section and key holding the time of the last sync, in either direction. */
    static final String SYNC_SECTION = "thingworx";

    static final String SYNC_LAST = "lastSync";

    /**
     * The time of the last sync in either direction, or 0 when this repository was never synced.
     */
    long lastSync() {
        return git.getRepository().getConfig().getLong(SYNC_SECTION, null, SYNC_LAST, 0L);
    }

    /** Git config section holding, per Data Table, a hash of its rows at the last sync. */
    static final String DATA_SYNC_SECTION = "thingworxDataTable";

    static final String DATA_SYNC_HASH = "rowsHash";

    private String dataTableHash(String table) {
        return git.getRepository().getConfig().getString(DATA_SYNC_SECTION, table, DATA_SYNC_HASH);
    }

    private void recordDataTableHash(String table, String content) throws IOException {
        StoredConfig config = git.getRepository().getConfig();
        config.setString(
                DATA_SYNC_SECTION,
                table,
                DATA_SYNC_HASH,
                TrackedDataTables.rowsHash(content, table));
        config.save();
    }

    /** Writes a Data Table file unless it already holds exactly this content. */
    private void writeDataTableFile(String relative, String content) throws IOException {
        Path root = git.getRepository().getWorkTree().toPath().toRealPath();
        Path file = resolveRepositoryPath(root, relative);
        requireNoLinkComponents(root, file);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                && java.util.Arrays.equals(Files.readAllBytes(file), bytes)) return;
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);
    }

    private void recordSync(long millis) throws IOException {
        StoredConfig config = git.getRepository().getConfig();
        config.setLong(SYNC_SECTION, null, SYNC_LAST, millis);
        config.save();
    }

    /**
     * The tracked-project entities changed in ThingWorx since the last sync, as {@code "<Type>
     * <name>"}, which Sync to ThingWorx would overwrite. It reads each entity's modified date
     * rather than exporting, so it stays fast for large projects. Project entities themselves are
     * left out. Entities deleted in ThingWorx, and property values written at runtime, do not
     * change a modified date and are not reported. When the repository was never synced, every
     * entity in the tracked projects counts as changed.
     */
    List<String> changedSinceSync() throws Exception {
        requireImportPermissions();
        long since = lastSync();
        List<String> changed = new ArrayList<>();
        for (TrackedProject tracked : requireTrackedProjects()) {
            RootEntity found =
                    EntityUtilities.findEntity(tracked.name(), ThingworxRelationshipTypes.Project);
            if (!(found instanceof com.thingworx.projects.Project project)) continue;
            for (var reference : project.getProjectEntities()) {
                // The Project itself only groups the entities and changes whenever one is added,
                // so it is not a change worth protecting.
                if (reference.getType() == ThingworxRelationshipTypes.Project) continue;
                if (!tracked.syncs(SyncedEntityTypes.collectionOf(reference.getType()))) continue;
                RootEntity entity =
                        EntityUtilities.findEntity(reference.getName(), reference.getType());
                if (entity != null && modifiedAfter(entity, since)) {
                    changed.add(reference.getType() + " " + reference.getName());
                }
            }
        }
        // Writing rows does not change a Data Table's modified date, so compare its rows with
        // those of the last sync instead. Only files Git tracks are imported.
        for (DataTableFile item : dataTableFiles(requireTrackedProjects(), new ArrayList<>())) {
            if (!item.inGit()) continue;
            var thing = TrackedDataTables.find(item.name());
            if (thing == null) continue;
            String recorded = dataTableHash(item.name());
            String current =
                    TrackedDataTables.rowsHash(
                            TrackedDataTables.export(thing, TrackedDataTables.MIRROR), item.name());
            if (!current.equals(recorded)) changed.add("rows of Data Table " + item.name());
        }
        return changed;
    }

    private static boolean modifiedAfter(RootEntity entity, long since) {
        var modified = entity.GetLastModifiedDate();
        return modified == null ? since == 0L : modified.getMillis() > since;
    }

    private void importFolder(IServiceProvider resource, String path) throws Exception {
        ValueCollection params = new ValueCollection();
        params.put("repositoryName", new StringPrimitive(fileRepository.getName()));
        params.put("path", new StringPrimitive(path));
        params.put("useDefaultDataProvider", new BooleanPrimitive(true));
        params.put("withSubsystems", new BooleanPrimitive(false));
        params.put("overwritePropertyValues", new BooleanPrimitive(true));
        resource.processServiceRequest(IMPORT_SERVICE, params);
    }

    /**
     * Imports a project folder from a temporary copy that holds only its files tracked by Git,
     * without Users, which {@link #importUsers} has already imported, and ApplicationKeys ThingWorx
     * cannot update. See {@link SyncExclusions}. Every skipped ApplicationKey is reported in the
     * extension log with how to repair it, and untracked files are added to {@code untracked}.
     *
     * <p>The project is imported as a whole first. When the platform refuses that, its entities are
     * imported one at a time instead, in passes so an entity whose dependency fails at first is
     * tried again once the dependency is in (see {@link SyncFailures#inPasses}); the entities still
     * failing are then tried together once more, for dependencies that go both ways. Whatever still
     * fails is added to {@code failures} and the rest of the project is kept.
     *
     * @return whether at least one entity of the project was imported
     */
    private boolean importTracked(
            IServiceProvider resource,
            Path folder,
            String relative,
            TrackedProject tracked,
            java.util.Set<String> trackedPaths,
            List<String> untracked,
            SyncFailures failures)
            throws Exception {
        String project = tracked.name();
        String prefix = relative.replace('\\', '/').replaceAll("^/+|/+$", "") + "/";
        Path base = new File(fileRepository.getRootPath()).toPath().toRealPath();
        Path temporary = Files.createTempDirectory(base, ".jgit-import-");
        try {
            Path staged = temporary.resolve("0");
            List<SyncExclusions.Excluded> skipped =
                    SyncExclusions.copyExcluding(
                            folder,
                            staged,
                            SyncExclusions.IMPORT,
                            path -> {
                                // Data Table rows are imported after the entities.
                                if (TrackedDataTables.DATA_FOLDER.equals(collectionOfPath(path)))
                                    return false;
                                // Entity types the project does not sync are left alone.
                                if (!tracked.syncs(collectionOfPath(path))) return false;
                                if (trackedPaths.contains(prefix + path)) return true;
                                untracked.add(prefix + path);
                                return false;
                            });
            if (!skipped.isEmpty()) {
                com.thingworx.logging.LogUtilities.getInstance()
                        .getApplicationLogger(FileRepositoryManager.class)
                        .warn(
                                "Left out of the import of project '{}' in {}: {}",
                                project,
                                fileRepository.getName(),
                                skipped);
            }
            for (SyncExclusions.Excluded item : skipped) {
                if (!SyncExclusions.APPLICATION_KEYS.equals(item.collection())) continue;
                GitActionLogger.logAction(
                        "Import",
                        fileRepository.getName(),
                        "ApplicationKey '"
                                + item.name()
                                + "' was not updated: its stored key ID cannot be decrypted on"
                                + " this server (it was encrypted with another keystore), so"
                                + " ThingWorx refuses to update it. Delete and recreate the key,"
                                + " then sync again.");
            }
            if (!containsXmlFile(staged.toFile())) return false;
            try {
                importFolder(resource, "/" + temporary.getFileName() + "/0");
                return true;
            } catch (Exception whole) {
                warn(
                        "Importing project '{}' into {} as a whole failed; importing its entities"
                                + " one at a time",
                        project,
                        whole);
            }
            return importEach(resource, staged, project, failures);
        } finally {
            deleteRecursively(temporary);
        }
    }

    /**
     * Imports the entity files below {@code staged}, a project folder of collection folders, one at
     * a time, see {@link #importTracked}. Adds every entity that cannot be imported to {@code
     * failures}.
     *
     * @return whether at least one entity was imported
     */
    private boolean importEach(
            IServiceProvider resource, Path staged, String project, SyncFailures failures)
            throws Exception {
        List<File> found = new ArrayList<>();
        collectXmlFilesOnDisk(staged.toFile(), found);
        List<String> files = new ArrayList<>();
        for (File file : found) {
            files.add(staged.relativize(file.toPath()).toString().replace('\\', '/'));
        }
        java.util.Collections.sort(files);
        java.util.Map<String, Exception> failed =
                SyncFailures.inPasses(files, file -> importFiles(resource, staged, List.of(file)));
        // Entities that refer to each other can only be imported together.
        if (failed.size() > 1 && failed.size() < files.size()) {
            try {
                importFiles(resource, staged, List.copyOf(failed.keySet()));
                failed.clear();
            } catch (Exception together) {
                // Each keeps the error of its own attempt, which names its problem best.
            }
        }
        for (var entry : failed.entrySet()) {
            String file = entry.getKey();
            String name = file.substring(file.lastIndexOf('/') + 1).replaceFirst("\\.xml$", "");
            try {
                name =
                        SyncFailures.entityName(
                                Files.readString(staged.resolve(file), StandardCharsets.UTF_8),
                                name);
            } catch (IOException unreadable) {
                // The file name stands in for the entity name.
            }
            String collection = collectionOfPath(file);
            failures.add(project, collection, name, entry.getValue());
            warn("Could not import entity '{}' into {}", file, entry.getValue());
        }
        return failed.size() < files.size();
    }

    /**
     * Imports the given entity files, relative to {@code staged}, in one call to the platform's
     * import service, from a temporary folder that holds only them.
     */
    private void importFiles(IServiceProvider resource, Path staged, List<String> files)
            throws Exception {
        Path base = new File(fileRepository.getRootPath()).toPath().toRealPath();
        Path temporary = Files.createTempDirectory(base, ".jgit-entity-");
        try {
            Path copy = temporary.resolve("0");
            for (String file : files) {
                Path target = copy.resolve(file).normalize();
                if (!target.startsWith(copy)) throw new IOException("Invalid entity path: " + file);
                Files.createDirectories(target.getParent());
                Files.copy(staged.resolve(file), target, LinkOption.NOFOLLOW_LINKS);
            }
            importFolder(resource, "/" + temporary.getFileName() + "/0");
        } finally {
            deleteRecursively(temporary);
        }
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

    /** Folder below {@code jgit/} that keeps the Git directories of deleted repositories. */
    static final String RETIRED_GIT_FOLDER = "deleted";

    /**
     * Whether the Git directory of a FileRepository folder exists and holds anything a new
     * repository must not inherit: a remote or any ref. A directory freshly created by {@code git
     * init} has neither.
     */
    static boolean hasPreviousGitDirectory(File workTree) throws IOException {
        File gitDirectory = gitDirectoryFor(workTree);
        org.eclipse.jgit.util.FS fs = HardenedFs.create();
        if (!Files.isDirectory(gitDirectory.toPath(), LinkOption.NOFOLLOW_LINKS)
                || !org.eclipse.jgit.lib.RepositoryCache.FileKey.isGitRepository(
                        gitDirectory, fs)) {
            return false;
        }
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        builder.setFS(fs);
        builder.setGitDir(gitDirectory);
        builder.setGitCommonDir(gitDirectory);
        builder.setBare();
        builder.setMustExist(true);
        try (var repository = builder.build()) {
            return !repository.getRemoteNames().isEmpty() || repository.getRefDatabase().hasRefs();
        }
    }

    /**
     * Moves the Git directory of a FileRepository folder to {@code
     * <storage>/jgit/deleted/<folder>-<timestamp>.git} and removes its remotes, so no Thing uses it
     * and a repository created later under the same name starts empty. The history is kept for
     * recovery. Repository Git directories are always named {@code <folder>.git}, so the {@code
     * deleted} folder cannot collide with one.
     *
     * @return the new location, or {@code null} when there was no Git directory
     */
    static Path retireGitDirectory(File workTree) throws IOException {
        Path gitDirectory = gitDirectoryFor(workTree).toPath();
        if (!Files.isDirectory(gitDirectory, LinkOption.NOFOLLOW_LINKS)) return null;
        Path retired = gitDirectory.resolveSibling(RETIRED_GIT_FOLDER);
        Files.createDirectories(retired);
        String name = gitDirectory.getFileName().toString().replaceFirst("\\.git$", "");
        String stamp =
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
                        .withZone(java.time.ZoneOffset.UTC)
                        .format(java.time.Instant.now());
        Path target = retired.resolve(name + "-" + stamp + ".git");
        Files.move(gitDirectory, target);
        File configFile = target.resolve("config").toFile();
        if (configFile.isFile()) {
            var config =
                    new org.eclipse.jgit.storage.file.FileBasedConfig(
                            configFile, HardenedFs.create());
            try {
                config.load();
                for (String remote : config.getSubsections("remote")) {
                    config.unsetSection("remote", remote);
                }
                for (String branch : config.getSubsections("branch")) {
                    config.unset("branch", branch, "remote");
                    config.unset("branch", branch, "merge");
                }
                config.save();
            } catch (org.eclipse.jgit.errors.ConfigInvalidException e) {
                throw new IOException("Could not remove the remotes of " + target, e);
            }
        }
        return target;
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
