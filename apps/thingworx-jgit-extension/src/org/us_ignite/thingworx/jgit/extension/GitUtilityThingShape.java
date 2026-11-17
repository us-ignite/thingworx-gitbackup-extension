package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.entities.RootEntity;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.entities.utils.UserUtilities;
import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.annotations.ThingworxBaseTemplateDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceParameter;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.resources.entities.EntityServices;
import com.thingworx.resources.queries.Searcher;
import com.thingworx.security.users.User;
import com.thingworx.things.Thing;
import com.thingworx.things.repository.FileRepositoryThing;
import com.thingworx.thingshape.ThingShape;
import com.thingworx.types.InfoTable;
import com.thingworx.types.TagCollection;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.BooleanPrimitive;
import com.thingworx.types.primitives.IPrimitiveType;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.IntegerPrimitive;
import com.thingworx.types.primitives.PasswordPrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.us_ignite.thingworx.dap.DapBaseType;
import org.us_ignite.thingworx.dap.DapProperty;
import org.us_ignite.thingworx.dap.DapServicePayload;
import org.us_ignite.thingworx.dap.DapThingShape;
import org.us_ignite.thingworx.dap.runtime.DapResults;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.ExtensionLogSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.GitLogSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.GpgKeySpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.GpgKeySummarySpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.GpgKeyVerificationSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.OperationListSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.RepositoryConfigurationSummarySpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.RepositoryListSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.SshKeySummarySpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.StringResultSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.TrackedProjectSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.UpgradeStepSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.UserIdentitySpec;

/**
 * Utility services for repository lifecycle, entity synchronization, user configuration, and
 * extension logging.
 *
 * <p>Services on this Thing operate on the calling ThingWorx user where credentials and GPG keys
 * are involved. Protected user properties must not be exported or logged as plain text.
 */
@ThingworxBaseTemplateDefinition(name = "GenericThing")
@DapThingShape(
        name = "GIT.Utility.ThingShape",
        description =
                "This Utility shape is used to add useful functionality to the GIT Repository Things, which are built in Java.",
        properties = {
            @DapProperty(
                    name = "MaxDiffSize",
                    baseType = DapBaseType.INTEGER,
                    ordinal = 2,
                    defaultValue = "500000",
                    cacheTime = 0.0,
                    dataChangeType = "VALUE",
                    description =
                            "Max size of the diff string that will be sent to the client browser. Anything larger than that will not be sent at all.")
        },
        alertProperties = {"MaxDiffSize"})
public class GitUtilityThingShape extends Thing {

    private static final long serialVersionUID = 9085129963750550674L;
    private static Logger _logger =
            LogUtilities.getInstance().getApplicationLogger(GitUtilityThingShape.class);

    public GitUtilityThingShape() {}

    /** Creates the repository/FileRepository Thing and installs the repository shape. */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "RepositoryCreate",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable RepositoryCreate(
            @ThingworxServiceParameter(name = "RepoName", description = "", baseType = "STRING")
                    String RepoName,
            @ThingworxServiceParameter(name = "GitRepoURL", description = "", baseType = "STRING")
                    String GitRepoURL,
            @ThingworxServiceParameter(name = "BranchName", description = "", baseType = "STRING")
                    String BranchName,
            @ThingworxServiceParameter(
                            name = "TrackedProjects",
                            description =
                                    "Existing ThingWorx projects to synchronize with the repository, each with an optional Path (folder relative to the repository root; blank means <ProjectName>); may be empty and edited later in Repository Settings",
                            baseType = "INFOTABLE",
                            aspects = {"dataShape:GIT.TrackedProject.DataShape"})
                    InfoTable TrackedProjects,
            @ThingworxServiceParameter(name = "UseProxy", description = "", baseType = "BOOLEAN")
                    Boolean UseProxy,
            @ThingworxServiceParameter(name = "ProxyURL", description = "", baseType = "STRING")
                    String ProxyURL,
            @ThingworxServiceParameter(name = "ProxyPort", description = "", baseType = "INTEGER")
                    Integer ProxyPort,
            @ThingworxServiceParameter(
                            name = "LocalizationTokensPrefix",
                            description = "",
                            baseType = "STRING")
                    String LocalizationTokensPrefix,
            @ThingworxServiceParameter(
                            name = "GitCommitterUser",
                            description = "",
                            baseType = "STRING")
                    String GitCommitterUser,
            @ThingworxServiceParameter(
                            name = "GitCommitterPassword",
                            description = "",
                            baseType = "STRING")
                    String GitCommitterPassword,
            @ThingworxServiceParameter(
                            name = "GitCommitterEmail",
                            description = "",
                            baseType = "STRING")
                    String GitCommitterEmail,
            @ThingworxServiceParameter(
                            name = "GitCommitterFullName",
                            description = "",
                            baseType = "STRING")
                    String GitCommitterFullName) {
        boolean thingCreated = false;
        try {
            if (RepoName == null || RepoName.isBlank())
                throw new IllegalArgumentException("RepoName is required.");
            RepoName = RepoName.trim();
            if (EntityUtilities.findEntity(RepoName, ThingworxRelationshipTypes.Thing) != null)
                throw new IllegalArgumentException(
                        "A Thing named '" + RepoName + "' already exists.");
            // EntityServices called from Java does not apply the caller's permissions.
            requireEntityServicePermission("CreateThing");
            if (!isBlank(GitRepoURL)) Remotes.parseUrl(GitRepoURL);
            // Validate every project before creating anything, using the same rules as
            // AddTrackedProject: each must be an existing, trackable project.
            List<FileRepositoryManager.TrackedProject> projects = new ArrayList<>();
            if (TrackedProjects != null) {
                for (int i = 0; i < TrackedProjects.getRowCount(); i++) {
                    ValueCollection row = TrackedProjects.getRow(i);
                    String raw = row.getStringValue(Const.ProjectName);
                    if (raw == null || raw.isBlank()) continue;
                    String name = FileRepositoryManager.validateProjectFolderName(raw);
                    String path =
                            FileRepositoryManager.validateProjectPath(
                                    row.containsKey(Const.TrackedProjectPath)
                                            ? row.getStringValue(Const.TrackedProjectPath)
                                            : "");
                    var project =
                            EntityUtilities.findEntity(name, ThingworxRelationshipTypes.Project);
                    // A project the caller cannot see or read is reported exactly like a
                    // missing one, so the service does not reveal which projects exist.
                    if (project == null || !callerCanAccessProject(project)) {
                        throw new IllegalArgumentException(
                                String.format(Const.ERR_PROJECT_NOT_FOUND, name));
                    }
                    if (!FileRepositoryManager.isTrackableProject(project)) {
                        throw new IllegalArgumentException(
                                "Project '" + name + "' cannot be tracked by a repository.");
                    }
                    projects.add(new FileRepositoryManager.TrackedProject(name, path));
                }
            }
            projects = FileRepositoryManager.normalizeTrackedProjects(projects);
            FileRepositoryManager.requireDistinctProjectFolders(projects);
            EntityServices es = new EntityServices();
            String repositoryProject = Const.RepositoryProjectName;
            if (EntityUtilities.findEntity(repositoryProject, ThingworxRelationshipTypes.Project)
                    == null) {
                es.CreateProject(
                        repositoryProject,
                        "Component",
                        "Repository Things created by the JGit extension",
                        "",
                        new TagCollection());
            }
            es.CreateThing(
                    RepoName,
                    "GitRepository created by user "
                            + UserUtilities.getCurrentUser()
                            + " at "
                            + new java.util.Date(),
                    new TagCollection(),
                    repositoryProject,
                    Const.RepositoryThingTemplateName);
            thingCreated = true;
            // The repository is a single Thing based on the GIT.Repository.ThingTemplate
            // (which extends FileRepository). FileRepository supplies the working-tree storage
            // and the template's implemented GIT.Repository.ThingShape supplies the
            // version-control services, keeping the Thing usable with mashup dynamic services.
            Thing repoThing =
                    (Thing) EntityUtilities.findEntity(RepoName, ThingworxRelationshipTypes.Thing);
            repoThing.processServiceRequest("EnableThing", new ValueCollection());
            repoThing.processServiceRequest("RestartThing", new ValueCollection());
            for (int i = 0; i < 60; i++) {
                if (repoThing.isEnabled() && repoThing.isRunning()) break;
                Thread.sleep(1000);
            }
            // RestartThing replaces the runtime Thing instance. Re-fetch it before
            // writing persistent properties so the migrated configuration is saved
            // on the live repository entity.
            repoThing =
                    (Thing) EntityUtilities.findEntity(RepoName, ThingworxRelationshipTypes.Thing);
            repoThing.setPropertyValue(
                    Const.InitialBranch,
                    new StringPrimitive(
                            BranchName == null || BranchName.isBlank()
                                    ? "main"
                                    : BranchName.trim()));
            repoThing.setPropertyValue(
                    Const.TrackedProjects,
                    new InfoTablePrimitive(FileRepositoryManager.trackedProjectTable(projects)));
            repoThing.setPropertyValue(
                    Const.UseProxy, new BooleanPrimitive(UseProxy != null && UseProxy));
            repoThing.setPropertyValue(
                    Const.ProxyURL, new StringPrimitive(ProxyURL == null ? "" : ProxyURL));
            IntegerPrimitive proxyPort = new IntegerPrimitive();
            proxyPort.setValue(ProxyPort == null ? 0 : ProxyPort);
            repoThing.setPropertyValue(Const.ProxyPort, proxyPort);
            repoThing.setPropertyValue(
                    Const.LocalizationTokensPrefix,
                    new StringPrimitive(
                            LocalizationTokensPrefix == null ? "" : LocalizationTokensPrefix));
            // The commit identity belongs to the user, not to the repository.
            if (!isBlank(GitCommitterEmail) || !isBlank(GitCommitterFullName)) {
                initializeCurrentUserConfiguration();
                new GitUserContextManager().setIdentity(GitCommitterFullName, GitCommitterEmail);
            }
            boolean hasSignIn = !isBlank(GitCommitterUser) || !isBlank(GitCommitterPassword);
            // Sign-in is saved for the origin remote below. Without a URL there is no remote yet,
            // so it is kept as the repository-wide sign-in, which the upgrade manager moves to
            // the remotes once they exist.
            if (hasSignIn && isBlank(GitRepoURL)) {
                InfoTable credentialResult =
                        GitCredentialCreate(
                                nullToEmpty(GitCommitterUser),
                                nullToEmpty(GitCommitterPassword),
                                "",
                                "",
                                RepoName,
                                "");
                ValueCollection credentialRow =
                        credentialResult == null || credentialResult.getRowCount() == 0
                                ? null
                                : credentialResult.getRow(0);
                if (credentialRow == null || Boolean.TRUE.equals(credentialRow.getValue("Error"))) {
                    throw new IllegalStateException(
                            "Could not save credentials: "
                                    + (credentialRow == null
                                            ? "no result"
                                            : credentialRow.getStringValue("Message")));
                }
            }
            // The remote is recorded only in the new repository's .git/config.
            if (!isBlank(GitRepoURL)) {
                FileRepositoryManager created = new FileRepositoryManager(repoThing);
                try {
                    Remotes.add(created.git(), Remotes.DEFAULT_REMOTE, GitRepoURL);
                    if (hasSignIn) {
                        initializeCurrentUserConfiguration();
                        new GitUserContextManager()
                                .saveRemoteCredentials(
                                        RepoName,
                                        Remotes.DEFAULT_REMOTE,
                                        GitCommitterUser,
                                        nullToEmpty(GitCommitterPassword),
                                        "",
                                        Remotes.approvedOrigins(
                                                created.git().getRepository(),
                                                Remotes.DEFAULT_REMOTE));
                    }
                } finally {
                    created.close();
                }
                bindApprovedOrigins(RepoName);
            }
            gitActionLog("RepositoryCreate", RepoName, "Repository created.");
            return DapResults.success(
                    "RepositoryCreate", "Repository created.", StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            if (thingCreated) {
                // Remove the partly configured Thing so the same name can be retried.
                try {
                    new EntityServices().DeleteThing(RepoName);
                } catch (Exception cleanup) {
                    _logger.warn("RepositoryCreate could not remove " + RepoName, cleanup);
                }
            }
            gitActionLog(
                    "RepositoryCreate", RepoName, e == null ? "" : String.valueOf(e.getMessage()));
            return DapResults.failure("RepositoryCreate", e, StringResultContract.SERVICE_RESULT);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Returns whether the calling user may see and read the given project. {@code
     * EntityUtilities.findEntity} is not a permission-checked lookup, so services that accept a
     * project name must apply this before using the project.
     */
    /** True when the caller is a member of the Administrators group. */
    private static boolean callerIsAdmin() {
        try {
            UserUtilities.validateAdminMembership();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** True when the caller may read a repository Thing's design-time entity. */
    private static boolean callerCanReadRepository(String repoName) {
        if (repoName == null || repoName.isBlank()) return false;
        try {
            RootEntity entity =
                    EntityUtilities.findEntity(repoName, ThingworxRelationshipTypes.Thing);
            return entity != null
                    && entity.isVisible()
                    && Boolean.TRUE.equals(entity.CheckDesignTimePermission("Read"));
        } catch (Exception e) {
            return false;
        }
    }

    static boolean callerCanAccessProject(RootEntity project) {
        try {
            return project != null
                    && project.isVisible()
                    && Boolean.TRUE.equals(project.CheckDesignTimePermission("Read"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the origins of the remotes currently configured for a repository Thing. Credentials
     * are bound to these values when the user saves them, so a later remote change cannot redirect
     * a stored token. Returns an empty string when the repository or its Git directory is not yet
     * available.
     */
    private static String approvedOriginsFor(String gitThing) {
        if (gitThing == null || gitThing.isBlank()) return "";
        FileRepositoryManager repository = null;
        try {
            Object entity = EntityUtilities.findEntity(gitThing, ThingworxRelationshipTypes.Thing);
            if (!(entity instanceof Thing)) return "";
            repository = new FileRepositoryManager((Thing) entity);
            return Remotes.approvedOrigins(repository.git().getRepository());
        } catch (Exception e) {
            return "";
        } finally {
            if (repository != null) repository.close();
        }
    }

    /** Refreshes the current user's approved origins after a remote is added or changed. */
    private static void bindApprovedOrigins(String gitThing) {
        try {
            String origins = approvedOriginsFor(gitThing);
            new GitUserContextManager().setApprovedOriginsIfConfigured(gitThing, origins);
        } catch (Exception ignored) {
            // A user without a credential row has nothing to bind.
        }
    }

    /**
     * Fails unless the calling user may invoke the named EntityServices service. Calling
     * EntityServices from Java bypasses the platform's own check of the caller's permissions.
     */
    private static void requireEntityServicePermission(String serviceName) throws Exception {
        RootEntity entityServices =
                EntityUtilities.findEntity("EntityServices", ThingworxRelationshipTypes.Resource);
        if (entityServices == null)
            throw new IllegalStateException("EntityServices is unavailable.");
        if (!Boolean.TRUE.equals(entityServices.CheckPermission("ServiceInvoke", serviceName))) {
            throw new SecurityException("Service permission denied for " + serviceName);
        }
    }

    /**
     * Resolves a repository Thing for deletion. The Thing must exist, be a repository created by
     * this extension, and the calling user must hold design-time Delete permission on it.
     */
    private static Thing requireDeletableRepository(String repoName) throws Exception {
        RootEntity entity = EntityUtilities.findEntity(repoName, ThingworxRelationshipTypes.Thing);
        // A Thing the caller cannot see is reported like a missing one.
        if (!(entity instanceof Thing) || !entity.isVisible())
            throw new IllegalArgumentException("Repository '" + repoName + "' was not found.");
        Thing thing = (Thing) entity;
        boolean repositoryTemplate =
                Const.RepositoryThingTemplateName.equals(thing.getThingTemplateName())
                        || Boolean.TRUE.equals(
                                thing.IsDerivedFromTemplate(Const.RepositoryThingTemplateName));
        if (!(thing instanceof FileRepositoryThing)
                || !repositoryTemplate
                || !Const.RepositoryProjectName.equals(thing.GetProjectName())) {
            throw new IllegalArgumentException(
                    "Thing '" + repoName + "' is not a Git repository created by this extension.");
        }
        if (!Boolean.TRUE.equals(thing.CheckDesignTimePermission("Delete"))) {
            throw new SecurityException("Delete permission denied for Thing " + repoName);
        }
        requireEntityServicePermission("DeleteThing");
        return thing;
    }

    /**
     * Copies GPG key rows for a service result. Only the fingerprint and label are copied; the
     * private key and passphrase fields of the data shape are left unset.
     */
    private static InfoTable withoutGpgSecrets(InfoTable keys) throws Exception {
        InfoTable result =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.UserGpgKeyDataShapeName);
        if (keys == null) return result;
        for (int i = 0; i < keys.getRowCount(); i++) {
            ValueCollection source = keys.getRow(i);
            ValueCollection row = new ValueCollection();
            row.put(
                    Const.GpgKeyFingerprint,
                    new StringPrimitive(
                            nullToEmpty(source.getStringValue(Const.GpgKeyFingerprint))));
            row.put(
                    Const.GpgKeyLabel,
                    new StringPrimitive(nullToEmpty(source.getStringValue(Const.GpgKeyLabel))));
            result.addRow(row);
        }
        return result;
    }

    /** Lists projects a new repository can track, for the repository create form. */
    @DapServicePayload(TrackedProjectSpec.class)
    @ThingworxServiceDefinition(
            name = "GetTrackableProjects",
            description =
                    "Lists ThingWorx projects visible to the caller that a repository can track",
            category = "Projects",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.TrackedProject.ServiceResult.DataShape"
            })
    public InfoTable GetTrackableProjects() {
        try {
            // The Java project lookup is not permission-checked; keep only projects the caller
            // can see and read.
            List<String> names = new java.util.ArrayList<>();
            for (String name : FileRepositoryManager.trackableProjectNames()) {
                if (callerCanAccessProject(
                        EntityUtilities.findEntity(name, ThingworxRelationshipTypes.Project)))
                    names.add(name);
            }
            return DapResults.success(
                    "GetTrackableProjects",
                    TrackedProjectTable.SERVICE_RESULT,
                    TrackedProjectTable.wrap(FileRepositoryManager.projectTable(names)));
        } catch (Exception e) {
            return DapResults.failure(
                    "GetTrackableProjects", e, TrackedProjectTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "ProjectCreate",
            description =
                    "Creates an empty ThingWorx project that a repository can track. The caller needs permission to run EntityServices.CreateProject.",
            category = "Projects",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable ProjectCreate(
            @ThingworxServiceParameter(
                            name = "ProjectName",
                            description =
                                    "Letters, digits, '.', '_' and '-'; also the folder name in the repository",
                            baseType = "STRING")
                    String ProjectName,
            @ThingworxServiceParameter(
                            name = "Description",
                            description = "Optional project description",
                            baseType = "STRING")
                    String Description) {
        try {
            String name = FileRepositoryManager.validateNewProjectName(ProjectName);
            if (EntityUtilities.findEntity(name, ThingworxRelationshipTypes.Project) != null) {
                throw new IllegalArgumentException(
                        "A project named '"
                                + name
                                + "' already exists. Select it in the list instead.");
            }
            // EntityServices called from Java does not apply the caller's permissions.
            requireEntityServicePermission("CreateProject");
            new EntityServices()
                    .CreateProject(
                            name,
                            "Component",
                            isBlank(Description)
                                    ? "Created by "
                                            + UserUtilities.getCurrentUser()
                                            + " for Git tracking"
                                    : Description.trim(),
                            "",
                            new TagCollection());
            var project = EntityUtilities.findEntity(name, ThingworxRelationshipTypes.Project);
            if (!FileRepositoryManager.isTrackableProject(project)) {
                throw new IllegalStateException(
                        "Project '"
                                + name
                                + "' was created, but you cannot read it, so it cannot be tracked.");
            }
            gitActionLog("ProjectCreate", "", "Project '" + name + "' created.");
            return DapResults.success(
                    "ProjectCreate",
                    "Project '" + name + "' created.",
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("ProjectCreate", e, StringResultContract.SERVICE_RESULT);
        }
    }

    /**
     * Writes a repository lifecycle entry to the extension log; failures never break the service.
     */
    private void gitActionLog(String action, String repoName, String message) {
        try {
            GitActionLogger.logAction(action, repoName, message);
        } catch (Exception ignored) {
        }
    }

    @DapServicePayload(RepositoryListSpec.class)
    @ThingworxServiceDefinition(
            name = "RepositoryList",
            description = "Lists available GIT Repository Things",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.RepositoryList.ServiceResult.DataShape"
            })
    public InfoTable RepositoryList() {
        try {
            InfoTable result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            "GIT.RepositoryList.DataShape");
            Searcher searcher =
                    (Searcher)
                            EntityUtilities.findEntity(
                                    "SearchFunctions", ThingworxRelationshipTypes.Resource);
            if (searcher == null)
                return DapResults.success(
                        "RepositoryList",
                        RepositoryListTable.SERVICE_RESULT,
                        RepositoryListTable.wrap(result));
            JSONObject empty = new JSONObject();
            InfoTable found =
                    searcher.SpotlightSearch(
                            "",
                            new TagCollection(),
                            empty,
                            empty,
                            empty,
                            empty,
                            empty,
                            null,
                            null,
                            false,
                            false,
                            "name",
                            true,
                            30000.0,
                            null,
                            Const.RepositoryProjectName,
                            false);
            for (int i = 0; i < found.getRowCount(); i++) {
                ValueCollection row = found.getRow(i);
                ValueCollection out = new ValueCollection();
                out.put(
                        "RepoName",
                        new StringPrimitive(
                                Optional.ofNullable(row)
                                        .map(r -> r.getPrimitive("name"))
                                        .map(IPrimitiveType::getValue)
                                        .map(Object::toString)
                                        .orElse(null)));
                result.addRow(out);
            }
            return DapResults.success(
                    "RepositoryList",
                    RepositoryListTable.SERVICE_RESULT,
                    RepositoryListTable.wrap(result));
        } catch (Exception e) {
            _logger.error("RepositoryList failed", e);
            return DapResults.failure("RepositoryList", e, RepositoryListTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "RepositoryDelete",
            description =
                    "Deletes a GIT Repository Thing and its local FileRepository working tree.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable RepositoryDelete(
            @ThingworxServiceParameter(name = "RepoName", description = "", baseType = "STRING")
                    String RepoName) {
        try {
            if (RepoName == null || RepoName.isBlank())
                throw new IllegalArgumentException("RepoName is required.");
            RepoName = RepoName.trim();
            requireDeletableRepository(RepoName);
            EntityServices es = new EntityServices();
            es.DeleteThing(RepoName);
            String CurrentUser = UserUtilities.getCurrentUser();
            User user = UserUtilities.findUser(CurrentUser);
            if (user != null) {
                InfoTable creds = new GitUserContextManager().credentials();
                if (creds != null) {
                    InfoTable updatedCreds =
                            InfoTableInstanceFactory.createInfoTableFromDataShape(
                                    "GIT.RepositoryConfiguration.UserExtension.DataShape");
                    for (int ci = 0; ci < creds.getRowCount(); ci++) {
                        ValueCollection credRow = creds.getRow(ci);
                        if (!RepoName.equals(
                                credRow.getPrimitive("GitThing").getValue().toString())) {
                            updatedCreds.addRow(credRow);
                        }
                    }
                    creds = updatedCreds;
                    user.setPropertyValue(
                            "UserRepositoryConfiguration", new InfoTablePrimitive(creds));
                }
                new GitUserContextManager().removeRemoteCredentials(RepoName, null);
            }
            _logger.warn(
                    "GIT Repository Thing "
                            + RepoName
                            + " (self-hosted FileRepository) was deleted successfully.");
            gitActionLog("RepositoryDelete", RepoName, "Repository deleted.");
            return DapResults.success(
                    "RepositoryDelete", "Repository deleted.", StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            gitActionLog(
                    "RepositoryDelete", RepoName, e == null ? "" : String.valueOf(e.getMessage()));
            return DapResults.failure("RepositoryDelete", e, StringResultContract.SERVICE_RESULT);
        }
    }

    /** Upgrades every repository and every user's settings to the current storage format. */
    @DapServicePayload(UpgradeStepSpec.class)
    @ThingworxServiceDefinition(
            name = "RunUpgrade",
            description =
                    "Upgrades every repository and every user's saved Git settings to the current format: moves retired repository settings into .git/config, gives each remote its own copy of the repository-wide sign-in, and moves per-repository commit identities to the user. Safe to run again. Administrators only.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.UpgradeStep.ServiceResult.DataShape"
            })
    public InfoTable RunUpgrade() {
        try {
            UserUtilities.validateAdminMembership();
            UpgradeStepTable table = UpgradeStepTable.create();
            for (UpgradeManager.Step step : UpgradeManager.run()) {
                table.add(
                        UpgradeStepRow.create()
                                .scope(step.scope())
                                .name(step.name())
                                .item(step.item())
                                .action(step.action())
                                .detail(step.detail()));
            }
            return DapResults.success("RunUpgrade", UpgradeStepTable.SERVICE_RESULT, table);
        } catch (Exception e) {
            return DapResults.failure("RunUpgrade", e, UpgradeStepTable.SERVICE_RESULT);
        }
    }

    /** Initializes the per-user extension properties required by utility services. */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "InitUserExtensionProperties",
            description = "Adds UserExtension Properties needed by the JGitExtension",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable InitUserExtensionProperties() {
        try {
            UserUtilities.validateAdminMembership();
            return initializeUserExtensionProperties(true);
        } catch (Exception e) {
            return DapResults.failure(
                    "InitUserExtensionProperties", e, StringResultContract.SERVICE_RESULT);
        }
    }

    /**
     * Ensures the current user's rows use the current data shapes. It never changes ThingShape
     * property definitions, so an ordinary user cannot trigger a platform-wide restart.
     */
    private void initializeCurrentUserConfiguration() {
        try {
            initializeUserExtensionProperties(false);
        } catch (Exception e) {
            _logger.warn("Could not initialize the current user's Git configuration.", e);
        }
    }

    private InfoTable initializeUserExtensionProperties(boolean includeGlobalDefinitions) {
        try {
            ThingShape userExtensions =
                    (ThingShape)
                            EntityUtilities.findEntity(
                                    "UserExtensions", ThingworxRelationshipTypes.ThingShape);
            if (userExtensions == null) {
                throw new IllegalStateException("UserExtensions ThingShape is unavailable.");
            }
            if (!includeGlobalDefinitions && !userDefinitionsPresent(userExtensions)) {
                return DapResults.success(
                        "InitUserExtensionProperties",
                        "User extension properties are not initialized. An administrator must run InitUserExtensionProperties.",
                        StringResultContract.SERVICE_RESULT);
            }

            String CurrentUser = UserUtilities.getCurrentUser();
            User user = UserUtilities.findUser(CurrentUser);

            boolean hasGitCredentials = false;
            if (user != null) {
                hasGitCredentials = user.getPropertyValue("UserRepositoryConfiguration") != null;
            }
            if (includeGlobalDefinitions && !hasGitCredentials) {
                ValueCollection addPropParams = new ValueCollection();
                addPropParams.put("defaultValue", new StringPrimitive(""));
                addPropParams.put(
                        "description", new StringPrimitive("User-specific Git credentials store."));
                addPropParams.put("readOnly", new BooleanPrimitive(false));
                addPropParams.put("type", new StringPrimitive("INFOTABLE"));
                addPropParams.put("name", new StringPrimitive("UserRepositoryConfiguration"));
                addPropParams.put("persistent", new BooleanPrimitive(true));
                addPropParams.put(
                        "dataShape",
                        new StringPrimitive("GIT.RepositoryConfiguration.UserExtension.DataShape"));
                userExtensions.processServiceRequest("AddPropertyDefinition", addPropParams);
                new EntityServices().RestartDependenciesForThingShape("UserExtensions");
            }

            if (user != null) {
                Object configuration = user.getPropertyValue("UserRepositoryConfiguration");
                if (configuration == null) {
                    InfoTable empty =
                            InfoTableInstanceFactory.createInfoTableFromDataShape(
                                    Const.GitCredentialsDataShapeName);
                    Object credentials = user.getPropertyValue("GitCredentials");
                    Object gpgKeys = user.getPropertyValue("GpgKeys");
                    if (credentials instanceof InfoTablePrimitive) {
                        InfoTable table = ((InfoTablePrimitive) credentials).getValue();
                        if (table != null) {
                            for (int i = 0; i < table.getRowCount(); i++)
                                empty.addRow(table.getRow(i));
                        }
                    }
                    if (gpgKeys instanceof InfoTablePrimitive) {
                        InfoTable table = ((InfoTablePrimitive) gpgKeys).getValue();
                        if (table != null) {
                            InfoTable keyStore =
                                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                                            Const.UserGpgKeyDataShapeName);
                            for (int i = 0; i < table.getRowCount(); i++) {
                                ValueCollection source = table.getRow(i);
                                String gitThing =
                                        Optional.ofNullable(source)
                                                .map(r -> r.getPrimitive("GitThing"))
                                                .map(IPrimitiveType::getValue)
                                                .map(Object::toString)
                                                .orElse(null);
                                String fingerprint =
                                        Optional.ofNullable(source)
                                                .map(r -> r.getPrimitive(Const.GpgKeyFingerprint))
                                                .map(IPrimitiveType::getValue)
                                                .map(Object::toString)
                                                .orElse(null);
                                if (fingerprint != null && !fingerprint.isBlank()) {
                                    ValueCollection key = new ValueCollection();
                                    key.put(
                                            Const.GpgKeyFingerprint,
                                            new StringPrimitive(fingerprint));
                                    if (source.getPrimitive(Const.GpgPrivateKey) != null)
                                        key.put(
                                                Const.GpgPrivateKey,
                                                source.getPrimitive(Const.GpgPrivateKey));
                                    if (source.getPrimitive(Const.GpgKeyPassphrase) != null)
                                        key.put(
                                                Const.GpgKeyPassphrase,
                                                source.getPrimitive(Const.GpgKeyPassphrase));
                                    keyStore.addRow(key);
                                }
                                ValueCollection target = null;
                                for (int j = 0; j < empty.getRowCount(); j++) {
                                    if (gitThing.equals(
                                            Optional.ofNullable(empty.getRow(j))
                                                    .map(r -> r.getPrimitive("GitThing"))
                                                    .map(IPrimitiveType::getValue)
                                                    .map(Object::toString)
                                                    .orElse(null))) {
                                        target = empty.getRow(j);
                                        break;
                                    }
                                }
                                if (target == null) {
                                    target = new ValueCollection();
                                    target.put("GitThing", source.getPrimitive("GitThing"));
                                    empty.addRow(target);
                                }
                                if (fingerprint != null && !fingerprint.isBlank())
                                    target.put(
                                            Const.GpgKeyFingerprint,
                                            new StringPrimitive(fingerprint));
                            }
                            user.setPropertyValue(
                                    Const.UserGpgKeys, new InfoTablePrimitive(keyStore));
                        }
                    }
                    user.setPropertyValue(
                            "UserRepositoryConfiguration", new InfoTablePrimitive(empty));
                }
            }

            boolean hasUserGpgKeys = false;
            if (user != null) {
                hasUserGpgKeys = user.getPropertyValue(Const.UserGpgKeys) != null;
            }
            if (includeGlobalDefinitions && !hasUserGpgKeys) {
                ValueCollection addPropParams = new ValueCollection();
                addPropParams.put("defaultValue", new StringPrimitive(""));
                addPropParams.put(
                        "description",
                        new StringPrimitive("User-owned GPG keys reusable across repositories."));
                addPropParams.put("readOnly", new BooleanPrimitive(false));
                addPropParams.put("type", new StringPrimitive("INFOTABLE"));
                addPropParams.put("name", new StringPrimitive(Const.UserGpgKeys));
                addPropParams.put("persistent", new BooleanPrimitive(true));
                addPropParams.put("dataShape", new StringPrimitive(Const.UserGpgKeyDataShapeName));
                userExtensions.processServiceRequest("AddPropertyDefinition", addPropParams);
                new EntityServices().RestartDependenciesForThingShape("UserExtensions");
            }
            if (user != null && user.getPropertyValue(Const.UserGpgKeys) == null) {
                InfoTable keys =
                        InfoTableInstanceFactory.createInfoTableFromDataShape(
                                Const.UserGpgKeyDataShapeName);
                for (String propertyName :
                        new String[] {"GpgKeys", "UserRepositoryConfiguration"}) {
                    Object value = null;
                    value = user.getPropertyValue(propertyName);
                    if (!(value instanceof InfoTablePrimitive)) continue;
                    InfoTable source = ((InfoTablePrimitive) value).getValue();
                    if (source == null) continue;
                    for (int i = 0; i < source.getRowCount(); i++) {
                        ValueCollection row = source.getRow(i);
                        String fingerprint = row.getStringValue(Const.GpgKeyFingerprint);
                        if (fingerprint.isBlank()) continue;
                        boolean alreadyStored = false;
                        for (int j = 0; j < keys.getRowCount(); j++) {
                            if (fingerprint.equals(
                                    keys.getRow(j).getStringValue(Const.GpgKeyFingerprint))) {
                                alreadyStored = true;
                                break;
                            }
                        }
                        if (alreadyStored) continue;
                        ValueCollection key = new ValueCollection();
                        key.put(Const.GpgKeyFingerprint, new StringPrimitive(fingerprint));
                        if (row.getPrimitive(Const.GpgPrivateKey) != null)
                            key.put(Const.GpgPrivateKey, row.getPrimitive(Const.GpgPrivateKey));
                        if (row.getPrimitive(Const.GpgKeyPassphrase) != null)
                            key.put(
                                    Const.GpgKeyPassphrase,
                                    row.getPrimitive(Const.GpgKeyPassphrase));
                        keys.addRow(key);
                    }
                }
                user.setPropertyValue(Const.UserGpgKeys, new InfoTablePrimitive(keys));
            }
            boolean identityAdded = false;
            for (String property : new String[] {Const.GitUserName, Const.GitUserEmail}) {
                if (!includeGlobalDefinitions) break;
                if (userExtensions.getEffectivePropertyDefinition(property) != null) continue;
                ValueCollection addPropParams = new ValueCollection();
                addPropParams.put("defaultValue", new StringPrimitive(""));
                addPropParams.put(
                        "description",
                        new StringPrimitive(
                                Const.GitUserName.equals(property)
                                        ? "Name recorded on the user's Git commits."
                                        : "Email recorded on the user's Git commits."));
                addPropParams.put("readOnly", new BooleanPrimitive(false));
                addPropParams.put("type", new StringPrimitive("STRING"));
                addPropParams.put("name", new StringPrimitive(property));
                addPropParams.put("persistent", new BooleanPrimitive(true));
                userExtensions.processServiceRequest("AddPropertyDefinition", addPropParams);
                identityAdded = true;
            }
            if (identityAdded) {
                new EntityServices().RestartDependenciesForThingShape("UserExtensions");
                if (user != null) user = UserUtilities.findUser(CurrentUser);
            }
            if (includeGlobalDefinitions
                    && userExtensions.getEffectivePropertyDefinition(Const.UserSshKeys) == null) {
                ValueCollection addPropParams = new ValueCollection();
                addPropParams.put("defaultValue", new StringPrimitive(""));
                addPropParams.put(
                        "description",
                        new StringPrimitive("User-owned SSH keys reusable across repositories."));
                addPropParams.put("readOnly", new BooleanPrimitive(false));
                addPropParams.put("type", new StringPrimitive("INFOTABLE"));
                addPropParams.put("name", new StringPrimitive(Const.UserSshKeys));
                addPropParams.put("persistent", new BooleanPrimitive(true));
                addPropParams.put("dataShape", new StringPrimitive(Const.UserSshKeyDataShapeName));
                userExtensions.processServiceRequest("AddPropertyDefinition", addPropParams);
                new EntityServices().RestartDependenciesForThingShape("UserExtensions");
                if (user != null) user = UserUtilities.findUser(CurrentUser);
            }
            if (includeGlobalDefinitions
                    && userExtensions.getEffectivePropertyDefinition(Const.UserRemoteCredentials)
                            == null) {
                ValueCollection addPropParams = new ValueCollection();
                addPropParams.put("defaultValue", new StringPrimitive(""));
                addPropParams.put(
                        "description",
                        new StringPrimitive("User-specific sign-in for each repository remote."));
                addPropParams.put("readOnly", new BooleanPrimitive(false));
                addPropParams.put("type", new StringPrimitive("INFOTABLE"));
                addPropParams.put("name", new StringPrimitive(Const.UserRemoteCredentials));
                addPropParams.put("persistent", new BooleanPrimitive(true));
                addPropParams.put(
                        "dataShape", new StringPrimitive(Const.RemoteCredentialsDataShapeName));
                userExtensions.processServiceRequest("AddPropertyDefinition", addPropParams);
                new EntityServices().RestartDependenciesForThingShape("UserExtensions");
                if (user != null) user = UserUtilities.findUser(CurrentUser);
            }
            if (user != null
                    && userExtensions.getEffectivePropertyDefinition(Const.UserRemoteCredentials)
                            != null
                    && new GitUserContextManager().remoteCredentials() == null) {
                user.setPropertyValue(
                        Const.UserRemoteCredentials,
                        new InfoTablePrimitive(
                                InfoTableInstanceFactory.createInfoTableFromDataShape(
                                        Const.RemoteCredentialsDataShapeName)));
            }
            if (user != null && new GitUserContextManager().sshKeys() == null) {
                user.setPropertyValue(
                        Const.UserSshKeys,
                        new InfoTablePrimitive(
                                InfoTableInstanceFactory.createInfoTableFromDataShape(
                                        Const.UserSshKeyDataShapeName)));
            }
            if (user != null) {
                // Tables created from an empty default have no fields and lose their rows on
                // reload; store them with their data shape.
                for (String repaired : new GitUserContextManager().repairStoredTables()) {
                    _logger.warn(
                            "Repaired user property {} for {}: it was stored without its data shape.",
                            repaired,
                            CurrentUser);
                }
            }
            return DapResults.success(
                    "InitUserExtensionProperties",
                    "User extension properties initialized.",
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure(
                    "InitUserExtensionProperties", e, StringResultContract.SERVICE_RESULT);
        }
    }

    private static boolean userDefinitionsPresent(ThingShape userExtensions) {
        for (String property :
                new String[] {
                    Const.UserRepositoryConfiguration,
                    Const.UserGpgKeys,
                    Const.GitUserName,
                    Const.GitUserEmail,
                    Const.UserSshKeys,
                    Const.UserRemoteCredentials
                }) {
            if (userExtensions.getEffectivePropertyDefinition(property) == null) return false;
        }
        return true;
    }

    private InfoTable getGpgKeysTable(User user) throws Exception {
        return new GitUserContextManager().gpgKeys();
    }

    @DapServicePayload(GpgKeyVerificationSpec.class)
    @ThingworxServiceDefinition(
            name = "VerifyGpgKey",
            description = "Verifies a pasted PGP private key can be loaded and used for signing.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.GpgKeyVerification.ServiceResult.DataShape"
            })
    public InfoTable VerifyGpgKey(
            @ThingworxServiceParameter(
                            name = "GpgPrivateKey",
                            description = "ASCII-armored PGP private key",
                            baseType = "STRING")
                    String GpgPrivateKey,
            @ThingworxServiceParameter(
                            name = "GpgKeyPassphrase",
                            description = "Passphrase for the PGP private key",
                            baseType = "STRING")
                    String GpgKeyPassphrase,
            @ThingworxServiceParameter(
                            name = "All",
                            description =
                                    "When true, verifies all stored keys for the current user",
                            baseType = "BOOLEAN")
                    Boolean All,
            @ThingworxServiceParameter(
                            name = "GpgKeyFingerprint",
                            description =
                                    "Optional GPG key fingerprint to verify a stored key; when both fingerprint and label are supplied both must match",
                            baseType = "STRING")
                    String GpgKeyFingerprint,
            @ThingworxServiceParameter(
                            name = "GpgKeyLabel",
                            description =
                                    "Optional GPG key label to verify a stored key; when both fingerprint and label are supplied both must match",
                            baseType = "STRING")
                    String GpgKeyLabel) {
        InfoTable result = null;
        try {
            result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.GpgKeyVerificationResultDataShapeName);
            boolean all = All != null && All;
            if (all) {
                User currentUser = new GitUserContextManager().requireUser();
                InfoTable storedKeys = getGpgKeysTable(currentUser);
                if (storedKeys != null) {
                    for (int i = 0; i < storedKeys.getRowCount(); i++) {
                        ValueCollection stored = storedKeys.getRow(i);
                        String storedKey =
                                stored.getPrimitive(Const.GpgPrivateKey) != null
                                        ? ((PasswordPrimitive)
                                                        stored.getPrimitive(Const.GpgPrivateKey))
                                                .getValue()
                                        : null;
                        String storedPassphrase =
                                stored.getPrimitive(Const.GpgKeyPassphrase) != null
                                        ? ((PasswordPrimitive)
                                                        stored.getPrimitive(Const.GpgKeyPassphrase))
                                                .getValue()
                                        : null;
                        String storedFingerprint = stored.getStringValue(Const.GpgKeyFingerprint);
                        String keyForVerify = PastedKeyGpgSigner.normalizePrivateKey(storedKey);
                        boolean valid = false;
                        String fingerprint = storedFingerprint;
                        PastedKeyGpgSigner signer = null;
                        try {
                            signer = new PastedKeyGpgSigner(keyForVerify, storedPassphrase);
                            String derived = signer.getFingerprint();
                            if (derived != null && !derived.isBlank()) {
                                fingerprint = derived;
                                valid = true;
                            }
                        } catch (Exception ex) {
                            if (fingerprint == null || fingerprint.isBlank()) {
                                fingerprint = "Unable to derive fingerprint";
                            }
                        } finally {
                            if (signer != null) signer.clearSensitiveData();
                        }
                        ValueCollection row = new ValueCollection();
                        row.put("GpgKeyFingerprint", new StringPrimitive(fingerprint));
                        row.put("Valid", new BooleanPrimitive(valid));
                        row.put("Stored", new BooleanPrimitive(true));
                        String storedLabel = stored.getStringValue(Const.GpgKeyLabel);
                        row.put(
                                Const.GpgKeyLabel,
                                new StringPrimitive(storedLabel != null ? storedLabel : ""));
                        result.addRow(row);
                    }
                }
            } else if ((GpgKeyFingerprint != null && !GpgKeyFingerprint.isBlank())
                    || (GpgKeyLabel != null && !GpgKeyLabel.isBlank())) {
                boolean hasFingerprint = GpgKeyFingerprint != null && !GpgKeyFingerprint.isBlank();
                boolean hasLabel = GpgKeyLabel != null && !GpgKeyLabel.isBlank();
                User currentUser = new GitUserContextManager().requireUser();
                InfoTable storedKeys = getGpgKeysTable(currentUser);
                if (storedKeys == null || storedKeys.getRowCount() == 0) {
                    throw new IllegalArgumentException(
                            "GPG key not found: "
                                    + (hasLabel && hasFingerprint
                                            ? "fingerprint "
                                                    + GpgKeyFingerprint
                                                    + " and label '"
                                                    + GpgKeyLabel
                                                    + "'"
                                            : hasFingerprint
                                                    ? "fingerprint " + GpgKeyFingerprint
                                                    : "label '" + GpgKeyLabel + "'"));
                }
                boolean anyMatched = false;
                for (int i = 0; i < storedKeys.getRowCount(); i++) {
                    ValueCollection stored = storedKeys.getRow(i);
                    String fp = stored.getStringValue(Const.GpgKeyFingerprint);
                    String lbl = stored.getStringValue(Const.GpgKeyLabel);
                    boolean fingerprintMatches =
                            GpgKeyFingerprint != null && GpgKeyFingerprint.equals(fp);
                    boolean labelMatches = GpgKeyLabel != null && GpgKeyLabel.equals(lbl);
                    boolean matches;
                    if (hasFingerprint && hasLabel) {
                        matches = fingerprintMatches && labelMatches;
                    } else if (hasFingerprint) {
                        matches = fingerprintMatches;
                    } else {
                        matches = labelMatches;
                    }
                    if (!matches) continue;
                    anyMatched = true;
                    String storedKey =
                            stored.getPrimitive(Const.GpgPrivateKey) != null
                                    ? ((PasswordPrimitive) stored.getPrimitive(Const.GpgPrivateKey))
                                            .getValue()
                                    : null;
                    String storedPassphrase =
                            stored.getPrimitive(Const.GpgKeyPassphrase) != null
                                    ? ((PasswordPrimitive)
                                                    stored.getPrimitive(Const.GpgKeyPassphrase))
                                            .getValue()
                                    : null;
                    String storedFingerprint = fp;
                    String keyForVerify = PastedKeyGpgSigner.normalizePrivateKey(storedKey);
                    boolean valid = false;
                    String fingerprint = storedFingerprint;
                    PastedKeyGpgSigner signer = null;
                    try {
                        signer = new PastedKeyGpgSigner(keyForVerify, storedPassphrase);
                        String derived = signer.getFingerprint();
                        if (derived != null && !derived.isBlank()) {
                            fingerprint = derived;
                            valid = true;
                        }
                    } catch (Exception ex) {
                        if (fingerprint == null || fingerprint.isBlank()) {
                            fingerprint = "Unable to derive fingerprint";
                        }
                    } finally {
                        if (signer != null) signer.clearSensitiveData();
                    }
                    ValueCollection row = new ValueCollection();
                    row.put("GpgKeyFingerprint", new StringPrimitive(fingerprint));
                    row.put("Valid", new BooleanPrimitive(valid));
                    row.put("Stored", new BooleanPrimitive(true));
                    String storedLabel = lbl;
                    row.put(
                            Const.GpgKeyLabel,
                            new StringPrimitive(storedLabel != null ? storedLabel : ""));
                    result.addRow(row);
                }
                if (!anyMatched) {
                    throw new IllegalArgumentException(
                            "GPG key not found: "
                                    + (hasLabel && hasFingerprint
                                            ? "fingerprint "
                                                    + GpgKeyFingerprint
                                                    + " and label '"
                                                    + GpgKeyLabel
                                                    + "'"
                                            : hasFingerprint
                                                    ? "fingerprint " + GpgKeyFingerprint
                                                    : "label '" + GpgKeyLabel + "'"));
                }
            } else {
                String key = PastedKeyGpgSigner.normalizePrivateKey(GpgPrivateKey);
                String passphrase = GpgKeyPassphrase;
                if (key == null || key.isBlank()) {
                    throw new IllegalArgumentException(
                            "GpgPrivateKey is required when All is false and no GpgKeyFingerprint/GpgKeyLabel is supplied.");
                }
                PastedKeyGpgSigner signer = null;
                String fingerprint = null;
                boolean valid = false;
                try {
                    signer = new PastedKeyGpgSigner(key, passphrase);
                    fingerprint = signer.getFingerprint();
                    valid = fingerprint != null && !fingerprint.isBlank();
                    if (!valid) {
                        fingerprint = "Unable to derive fingerprint";
                    }
                } catch (Exception ex) {
                    fingerprint = "Unable to derive fingerprint";
                    valid = false;
                } finally {
                    if (signer != null) {
                        try {
                            signer.clearSensitiveData();
                        } catch (Exception ignored) {
                        }
                    }
                }
                String gpgKeyLabel = "";
                if (valid) {
                    try {
                        User labelUser = new GitUserContextManager().requireUser();
                        InfoTable labelKeys = getGpgKeysTable(labelUser);
                        boolean found = false;
                        if (labelKeys != null) {
                            for (int j = 0; j < labelKeys.getRowCount(); j++) {
                                ValueCollection r = labelKeys.getRow(j);
                                if (fingerprint.equals(r.getStringValue(Const.GpgKeyFingerprint))) {
                                    String lbl = r.getStringValue(Const.GpgKeyLabel);
                                    gpgKeyLabel = lbl != null ? lbl : "";
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            gpgKeyLabel = "Not in User Keys";
                        }
                    } catch (Exception ignored) {
                        if (valid) {
                            gpgKeyLabel = "Not in User Keys";
                        }
                    }
                } else {
                    try {
                        User labelUser = new GitUserContextManager().requireUser();
                        InfoTable labelKeys = getGpgKeysTable(labelUser);
                        if (labelKeys != null && fingerprint != null) {
                            for (int j = 0; j < labelKeys.getRowCount(); j++) {
                                ValueCollection r = labelKeys.getRow(j);
                                if (fingerprint.equals(r.getStringValue(Const.GpgKeyFingerprint))) {
                                    String lbl = r.getStringValue(Const.GpgKeyLabel);
                                    gpgKeyLabel = lbl != null ? lbl : "";
                                    break;
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
                boolean stored = false;
                try {
                    User storedUser = new GitUserContextManager().requireUser();
                    InfoTable storedKeysForFlag = getGpgKeysTable(storedUser);
                    if (fingerprint != null && storedKeysForFlag != null) {
                        for (int j = 0; j < storedKeysForFlag.getRowCount(); j++) {
                            if (fingerprint.equals(
                                    storedKeysForFlag
                                            .getRow(j)
                                            .getStringValue(Const.GpgKeyFingerprint))) {
                                stored = true;
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
                ValueCollection row = new ValueCollection();
                row.put(
                        "GpgKeyFingerprint",
                        new StringPrimitive(
                                fingerprint != null
                                        ? fingerprint
                                        : "Unable to derive fingerprint"));
                row.put("Valid", new BooleanPrimitive(valid));
                row.put("Stored", new BooleanPrimitive(stored));
                row.put(Const.GpgKeyLabel, new StringPrimitive(gpgKeyLabel));
                result.addRow(row);
            }
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            _logger.error(errors.toString());
            return DapResults.failure("VerifyGpgKey", e, GpgKeyVerificationTable.SERVICE_RESULT);
        }
        return DapResults.success(
                "VerifyGpgKey",
                GpgKeyVerificationTable.SERVICE_RESULT,
                GpgKeyVerificationTable.wrap(result));
    }

    /** Returns the current user’s configured GPG key metadata. */
    @DapServicePayload(GpgKeySpec.class)
    @ThingworxServiceDefinition(
            name = "GpgKeyList",
            description =
                    "Returns all GPG keys configured for the current user across all repositories",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.GpgKey.ServiceResult.DataShape"})
    public InfoTable GpgKeyList() {
        try {
            User currentUser = new GitUserContextManager().requireUser();
            // Stored rows hold the private key and passphrase; services never return them.
            InfoTable gpgKeys = withoutGpgSecrets(getGpgKeysTable(currentUser));
            return DapResults.success(
                    "GpgKeyList", GpgKeyTable.SERVICE_RESULT, GpgKeyTable.wrap(gpgKeys));
        } catch (Exception e) {
            _logger.error("GpgKeyList failed", e);
            return DapResults.failure("GpgKeyList", e, GpgKeyTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(GpgKeySummarySpec.class)
    @ThingworxServiceDefinition(
            name = "GpgKeyListSummary",
            description =
                    "Lists the current user's GPG key labels and fingerprints without private key material",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.GpgKeySummary.ServiceResult.DataShape"
            })
    public InfoTable GpgKeyListSummary() {
        try {
            InfoTable result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            "GIT.GpgKeySummary.DataShape");
            InfoTable keys = new GitUserContextManager().gpgKeys();
            if (keys != null) {
                for (int i = 0; i < keys.getRowCount(); i++) {
                    ValueCollection key = keys.getRow(i);
                    ValueCollection row = new ValueCollection();
                    row.put(
                            Const.GpgKeyFingerprint,
                            new StringPrimitive(key.getStringValue(Const.GpgKeyFingerprint)));
                    row.put(
                            Const.GpgKeyLabel,
                            new StringPrimitive(key.getStringValue(Const.GpgKeyLabel)));
                    result.addRow(row);
                }
            }
            return DapResults.success(
                    "GpgKeyListSummary",
                    GpgKeySummaryTable.SERVICE_RESULT,
                    GpgKeySummaryTable.wrap(result));
        } catch (Exception e) {
            return DapResults.failure("GpgKeyListSummary", e, GpgKeySummaryTable.SERVICE_RESULT);
        }
    }

    /** Creates a reusable GPG key for the current user. */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "GpgKeyCreate",
            description =
                    "Adds a reusable GPG key for the current user, keyed by fingerprint with optional label",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable GpgKeyCreate(
            @ThingworxServiceParameter(
                            name = "GpgPrivateKey",
                            description = "ASCII-armored PGP private key",
                            baseType = "STRING")
                    String GpgPrivateKey,
            @ThingworxServiceParameter(
                            name = "GpgKeyPassphrase",
                            description = "Passphrase for the PGP private key",
                            baseType = "STRING")
                    String GpgKeyPassphrase,
            @ThingworxServiceParameter(
                            name = "GpgKeyFingerprint",
                            description =
                                    "Optional fingerprint used to verify the fingerprint derived from the private key",
                            baseType = "STRING")
                    String GpgKeyFingerprint,
            @ThingworxServiceParameter(
                            name = "GpgKeyLabel",
                            description = "Optional human-readable label for this key",
                            baseType = "STRING")
                    String GpgKeyLabel) {
        try {
            GpgPrivateKey = PastedKeyGpgSigner.normalizePrivateKey(GpgPrivateKey);
            if (GpgPrivateKey == null || GpgPrivateKey.isBlank())
                throw new IllegalArgumentException("GpgPrivateKey is required.");
            User currentUser = new GitUserContextManager().requireUser();
            String derivedFingerprint;
            PastedKeyGpgSigner signer = new PastedKeyGpgSigner(GpgPrivateKey, GpgKeyPassphrase);
            try {
                derivedFingerprint = signer.getFingerprint();
            } finally {
                signer.clearSensitiveData();
            }
            if (derivedFingerprint == null || derivedFingerprint.isBlank())
                throw new IllegalArgumentException(
                        "Unable to derive fingerprint from GpgPrivateKey.");
            if (GpgKeyFingerprint == null || GpgKeyFingerprint.isBlank())
                GpgKeyFingerprint = derivedFingerprint;
            else if (!derivedFingerprint.equalsIgnoreCase(GpgKeyFingerprint))
                throw new IllegalArgumentException(
                        "GpgKeyFingerprint does not match the supplied GpgPrivateKey.");
            initializeCurrentUserConfiguration();
            InfoTable gpgKeys = getGpgKeysTable(currentUser);
            if (gpgKeys == null)
                gpgKeys =
                        InfoTableInstanceFactory.createInfoTableFromDataShape(
                                Const.UserGpgKeyDataShapeName);
            else {
                InfoTable cloned =
                        InfoTableInstanceFactory.createInfoTableFromDataShape(
                                Const.UserGpgKeyDataShapeName);
                for (int i = 0; i < gpgKeys.getRowCount(); i++) {
                    ValueCollection row = gpgKeys.getRow(i);
                    if (GpgKeyFingerprint.equals(row.getStringValue(Const.GpgKeyFingerprint)))
                        throw new IllegalArgumentException(
                                "A GPG key with this fingerprint already exists.");
                    if (GpgKeyLabel != null
                            && !GpgKeyLabel.isBlank()
                            && GpgKeyLabel.equals(row.getStringValue(Const.GpgKeyLabel)))
                        throw new IllegalArgumentException(
                                "A GPG key with this label already exists: " + GpgKeyLabel);
                    cloned.addRow(row);
                }
                gpgKeys = cloned;
            }
            ValueCollection key = new ValueCollection();
            key.put(Const.GpgKeyFingerprint, new StringPrimitive(GpgKeyFingerprint));
            if (GpgKeyLabel != null && !GpgKeyLabel.isBlank())
                key.put(Const.GpgKeyLabel, new StringPrimitive(GpgKeyLabel));
            key.put(Const.GpgPrivateKey, new PasswordPrimitive(GpgPrivateKey));
            key.put(Const.GpgKeyPassphrase, new PasswordPrimitive(GpgKeyPassphrase));
            gpgKeys.addRow(key);
            currentUser.setPropertyValue(Const.UserGpgKeys, new InfoTablePrimitive(gpgKeys));
            return DapResults.success(
                    "GpgKeyCreate", "GPG key created.", StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("GpgKeyCreate", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(GpgKeySpec.class)
    @ThingworxServiceDefinition(
            name = "GpgKeyGet",
            description = "Returns one owned GPG key by fingerprint or label",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.GpgKey.ServiceResult.DataShape"})
    public InfoTable GpgKeyGet(
            @ThingworxServiceParameter(
                            name = "GpgKeyFingerprint",
                            description = "Key fingerprint",
                            baseType = "STRING")
                    String fingerprint,
            @ThingworxServiceParameter(
                            name = "GpgKeyLabel",
                            description =
                                    "Optional key label; when both are supplied both must match",
                            baseType = "STRING")
                    String label) {
        try {
            if ((fingerprint == null || fingerprint.isBlank())
                    && (label == null || label.isBlank()))
                throw new IllegalArgumentException("GpgKeyFingerprint or GpgKeyLabel is required.");
            User user = new GitUserContextManager().requireUser();
            InfoTable result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.UserGpgKeyDataShapeName);
            InfoTable keys = getGpgKeysTable(user);
            boolean hasFingerprint = fingerprint != null && !fingerprint.isBlank();
            boolean hasLabel = label != null && !label.isBlank();
            if (keys != null)
                for (int i = 0; i < keys.getRowCount(); i++) {
                    ValueCollection row = keys.getRow(i);
                    boolean fingerprintMatches =
                            fingerprint != null
                                    && fingerprint.equals(
                                            row.getStringValue(Const.GpgKeyFingerprint));
                    boolean labelMatches =
                            label != null && label.equals(row.getStringValue(Const.GpgKeyLabel));
                    if (hasFingerprint && hasLabel) {
                        if (fingerprintMatches && labelMatches) result.addRow(row);
                    } else if (hasFingerprint) {
                        if (fingerprintMatches) result.addRow(row);
                    } else if (hasLabel) {
                        if (labelMatches) result.addRow(row);
                    }
                }
            if (result.getRowCount() == 0)
                throw new IllegalArgumentException(
                        "GPG key not found: "
                                + (hasLabel
                                        ? "label '" + label + "'"
                                        : "fingerprint " + fingerprint));
            return DapResults.success(
                    "GpgKeyGet",
                    GpgKeyTable.SERVICE_RESULT,
                    GpgKeyTable.wrap(withoutGpgSecrets(result)));
        } catch (Exception e) {
            return DapResults.failure("GpgKeyGet", e, GpgKeyTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "GpgKeyUpdate",
            description =
                    "Updates an owned GPG key selected by fingerprint or label; when fingerprint is supplied it is the sole selector and the label becomes the new value",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable GpgKeyUpdate(
            @ThingworxServiceParameter(
                            name = "GpgKeyFingerprint",
                            description =
                                    "Key fingerprint; when supplied selection is by fingerprint only",
                            baseType = "STRING")
                    String fingerprint,
            @ThingworxServiceParameter(
                            name = "GpgKeyLabel",
                            description =
                                    "Optional key label; when GpgKeyFingerprint is set this becomes the new label",
                            baseType = "STRING")
                    String GpgKeyLabel,
            @ThingworxServiceParameter(
                            name = "GpgPrivateKey",
                            description = "ASCII-armored private key",
                            baseType = "STRING")
                    String privateKey,
            @ThingworxServiceParameter(
                            name = "GpgKeyPassphrase",
                            description = "Private-key passphrase",
                            baseType = "STRING")
                    String passphrase) {
        try {
            privateKey = PastedKeyGpgSigner.normalizePrivateKey(privateKey);
            if ((fingerprint == null || fingerprint.isBlank())
                    && (GpgKeyLabel == null || GpgKeyLabel.isBlank()))
                throw new IllegalArgumentException("GpgKeyFingerprint or GpgKeyLabel is required.");
            User user = new GitUserContextManager().requireUser();
            InfoTable keys = getGpgKeysTable(user);
            if (keys == null) throw new IllegalArgumentException("GPG key not found.");
            boolean hasFingerprint = fingerprint != null && !fingerprint.isBlank();
            boolean hasLabel = GpgKeyLabel != null && !GpgKeyLabel.isBlank();
            boolean found = false;
            Map<String, String> fingerprintUpdates = new HashMap<>();
            for (int i = 0; i < keys.getRowCount(); i++) {
                ValueCollection row = keys.getRow(i);
                // Fingerprint wins: when fingerprint is supplied, selection is by fingerprint only.
                // The supplied label (if any) is treated as the new label to write, not a second
                // selector.
                boolean matches;
                if (hasFingerprint)
                    matches = fingerprint.equals(row.getStringValue(Const.GpgKeyFingerprint));
                else matches = GpgKeyLabel.equals(row.getStringValue(Const.GpgKeyLabel));
                if (matches) {
                    // If fingerprint selected the row and a different label was supplied, treat as
                    // rename.
                    if (hasFingerprint
                            && hasLabel
                            && !GpgKeyLabel.equals(row.getStringValue(Const.GpgKeyLabel)))
                        for (int j = 0; j < keys.getRowCount(); j++)
                            if (j != i
                                    && GpgKeyLabel.equals(
                                            keys.getRow(j).getStringValue(Const.GpgKeyLabel)))
                                throw new IllegalArgumentException(
                                        "A GPG key with this label already exists: " + GpgKeyLabel);
                    String oldFingerprint = row.getStringValue(Const.GpgKeyFingerprint);
                    // When new key material is supplied (label-supplied update or
                    // fingerprint-supplied
                    // rotation), derive the fingerprint from the key so the stored fingerprint
                    // stays
                    // consistent with the private key.
                    if (privateKey != null && !privateKey.isBlank()) {
                        String derivedFingerprint = null;
                        PastedKeyGpgSigner signer = new PastedKeyGpgSigner(privateKey, passphrase);
                        try {
                            derivedFingerprint = signer.getFingerprint();
                        } finally {
                            signer.clearSensitiveData();
                        }
                        if (derivedFingerprint != null
                                && !derivedFingerprint.isBlank()
                                && !derivedFingerprint.equals(oldFingerprint)) {
                            for (int j = 0; j < keys.getRowCount(); j++)
                                if (j != i
                                        && derivedFingerprint.equals(
                                                keys.getRow(j)
                                                        .getStringValue(Const.GpgKeyFingerprint)))
                                    throw new IllegalArgumentException(
                                            "A GPG key with this fingerprint already exists: "
                                                    + derivedFingerprint);
                            row.put(
                                    Const.GpgKeyFingerprint,
                                    new StringPrimitive(derivedFingerprint));
                            // Helper codepath: when updating by label and the fingerprint rotates,
                            // keep the user's repository credentials in sync so signing does not
                            // break.
                            if (!hasFingerprint && hasLabel) {
                                fingerprintUpdates.put(oldFingerprint, derivedFingerprint);
                            } else if (hasFingerprint) {
                                // Also keep repo credentials in sync for fingerprint-wins rotation;
                                // callers who selected by fingerprint expect their repos to keep
                                // signing.
                                fingerprintUpdates.put(oldFingerprint, derivedFingerprint);
                            }
                        }
                    }
                    // Services do not return key material, so an edit form cannot send it
                    // back: a blank private key keeps the stored key and passphrase.
                    if (privateKey != null && !privateKey.isBlank()) {
                        row.put(Const.GpgPrivateKey, new PasswordPrimitive(privateKey));
                        row.put(Const.GpgKeyPassphrase, new PasswordPrimitive(passphrase));
                    }
                    if (hasLabel) row.put(Const.GpgKeyLabel, new StringPrimitive(GpgKeyLabel));
                    found = true;
                }
            }
            if (!found)
                throw new IllegalArgumentException(
                        "GPG key not found: "
                                + (fingerprint != null && !fingerprint.isBlank()
                                        ? "fingerprint " + fingerprint
                                        : "label '" + GpgKeyLabel + "'"));
            user.setPropertyValue(Const.UserGpgKeys, new InfoTablePrimitive(keys));
            // Propagate fingerprint rotation to repository credentials referencing the old
            // fingerprint.
            if (!fingerprintUpdates.isEmpty()) {
                InfoTable configurations = new GitUserContextManager().credentials();
                if (configurations != null) {
                    boolean changed = false;
                    for (int i = 0; i < configurations.getRowCount(); i++) {
                        ValueCollection credRow = configurations.getRow(i);
                        String cfgFp = credRow.getStringValue(Const.GpgKeyFingerprint);
                        String newFp = fingerprintUpdates.get(cfgFp);
                        if (newFp != null) {
                            credRow.put(Const.GpgKeyFingerprint, new StringPrimitive(newFp));
                            changed = true;
                        }
                    }
                    if (changed) {
                        user.setPropertyValue(
                                Const.UserRepositoryConfiguration,
                                new InfoTablePrimitive(configurations));
                    }
                }
            }
            return DapResults.success(
                    "GpgKeyUpdate", "GPG key updated.", StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("GpgKeyUpdate", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(GpgKeySpec.class)
    @ThingworxServiceDefinition(
            name = "GpgKeyDelete",
            description = "Removes an owned GPG key and clears repository signing references",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.GpgKey.ServiceResult.DataShape"})
    public InfoTable GpgKeyDelete(
            @ThingworxServiceParameter(
                            name = "GpgKeyFingerprint",
                            description = "Key fingerprint",
                            baseType = "STRING")
                    String GpgKeyFingerprint,
            @ThingworxServiceParameter(
                            name = "GpgKeyLabel",
                            description = "Optional key label",
                            baseType = "STRING")
                    String GpgKeyLabel) {
        try {
            if ((GpgKeyFingerprint == null || GpgKeyFingerprint.isBlank())
                    && (GpgKeyLabel == null || GpgKeyLabel.isBlank()))
                throw new IllegalArgumentException("GpgKeyFingerprint or GpgKeyLabel is required.");
            User currentUser = new GitUserContextManager().requireUser();
            InfoTable keys = getGpgKeysTable(currentUser);
            boolean hasFingerprint = GpgKeyFingerprint != null && !GpgKeyFingerprint.isBlank();
            boolean found = false;
            InfoTable deleted =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.UserGpgKeyDataShapeName);
            Set<String> deletedFingerprints = new HashSet<>();
            if (keys != null) {
                InfoTable cloned =
                        InfoTableInstanceFactory.createInfoTableFromDataShape(
                                Const.UserGpgKeyDataShapeName);
                for (int i = 0; i < keys.getRowCount(); i++) {
                    ValueCollection row = keys.getRow(i);
                    boolean fingerprintMatches =
                            GpgKeyFingerprint != null
                                    && GpgKeyFingerprint.equals(
                                            row.getStringValue(Const.GpgKeyFingerprint));
                    boolean labelMatches =
                            GpgKeyLabel != null
                                    && GpgKeyLabel.equals(row.getStringValue(Const.GpgKeyLabel));
                    boolean matches;
                    if (hasFingerprint && GpgKeyLabel != null && !GpgKeyLabel.isBlank())
                        matches = fingerprintMatches && labelMatches;
                    else if (hasFingerprint) matches = fingerprintMatches;
                    else matches = labelMatches;
                    if (matches) {
                        found = true;
                        deleted.addRow(row);
                        String fingerprint = row.getStringValue(Const.GpgKeyFingerprint);
                        if (fingerprint != null && !fingerprint.isBlank())
                            deletedFingerprints.add(fingerprint);
                        continue;
                    }
                    cloned.addRow(row);
                }
                currentUser.setPropertyValue(Const.UserGpgKeys, new InfoTablePrimitive(cloned));
            }
            if (!found)
                throw new IllegalArgumentException(
                        "GPG key not found: "
                                + (hasFingerprint
                                        ? "fingerprint " + GpgKeyFingerprint
                                        : "label '" + GpgKeyLabel + "'"));
            InfoTable configurations = new GitUserContextManager().credentials();
            int cleared = 0;
            if (configurations != null)
                for (int i = 0; i < configurations.getRowCount(); i++) {
                    String configuredFingerprint =
                            configurations.getRow(i).getStringValue(Const.GpgKeyFingerprint);
                    if (configuredFingerprint != null
                            && deletedFingerprints.contains(configuredFingerprint)) {
                        configurations
                                .getRow(i)
                                .put(Const.GpgKeyFingerprint, new StringPrimitive(""));
                        cleared++;
                    }
                }
            if (configurations != null)
                currentUser.setPropertyValue(
                        Const.UserRepositoryConfiguration, new InfoTablePrimitive(configurations));
            String message =
                    "GPG key deleted."
                            + (cleared > 0
                                    ? " Cleared signing references from "
                                            + cleared
                                            + " repositories."
                                    : "");
            return DapResults.success(
                    "GpgKeyDelete",
                    message,
                    GpgKeyTable.SERVICE_RESULT,
                    GpgKeyTable.wrap(withoutGpgSecrets(deleted)));
        } catch (Exception e) {
            return DapResults.failure("GpgKeyDelete", e, GpgKeyTable.SERVICE_RESULT);
        }
    }

    /** Stores Git credentials and committer settings for a repository and user. */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "GitCredentialCreate",
            description =
                    "Stores or updates Git credentials for a GIT Repository thing in the current user's GitCredentials property.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable GitCredentialCreate(
            @ThingworxServiceParameter(
                            name = "GitCommitterUser",
                            description = "",
                            baseType = "STRING")
                    String GitCommitterUser,
            @ThingworxServiceParameter(
                            name = "GitCommitterPassword",
                            description = "",
                            baseType = "STRING")
                    String GitCommitterPassword,
            @ThingworxServiceParameter(
                            name = "GitCommitterEmail",
                            description = "",
                            baseType = "STRING")
                    String GitCommitterEmail,
            @ThingworxServiceParameter(
                            name = "GitCommitterFullName",
                            description = "",
                            baseType = "STRING")
                    String GitCommitterFullName,
            @ThingworxServiceParameter(name = "GitThing", description = "", baseType = "THINGNAME")
                    String GitThing,
            @ThingworxServiceParameter(
                            name = "GpgKeyFingerprint",
                            description =
                                    "Optional GPG key fingerprint; when set, commits are signed with this key",
                            baseType = "STRING")
                    String GpgKeyFingerprint) {
        try {
            initializeCurrentUserConfiguration();
            User currentUser = new GitUserContextManager().requireUser();
            if (!GpgKeyFingerprint.isBlank()) {
                validateGpgKeyOwnership(currentUser, GpgKeyFingerprint);
            }

            InfoTable creds = new GitUserContextManager().credentials();
            if (creds == null) {
                creds =
                        InfoTableInstanceFactory.createInfoTableFromDataShape(
                                "GIT.RepositoryConfiguration.UserExtension.DataShape");
            } else {
                InfoTable cloned =
                        InfoTableInstanceFactory.createInfoTableFromDataShape(
                                "GIT.RepositoryConfiguration.UserExtension.DataShape");
                for (int i = 0; i < creds.getRowCount(); i++) {
                    ValueCollection row = creds.getRow(i);
                    if (row.getPrimitive("GitThing").getValue().toString().equals(GitThing))
                        throw new IllegalArgumentException(
                                "Credentials already exist for repository: " + GitThing);
                    cloned.addRow(row);
                }
                creds = cloned;
            }
            ValueCollection entry = new ValueCollection();
            entry.put("GitCommitterUser", new StringPrimitive(GitCommitterUser));
            entry.put("GitCommitterPassword", new PasswordPrimitive(GitCommitterPassword));
            entry.put("GitThing", new StringPrimitive(GitThing));
            entry.put(
                    Const.ApprovedRemoteOrigins, new StringPrimitive(approvedOriginsFor(GitThing)));
            if (!GpgKeyFingerprint.isBlank())
                entry.put(Const.GpgKeyFingerprint, new StringPrimitive(GpgKeyFingerprint));
            creds.addRow(entry);
            currentUser.setPropertyValue(
                    "UserRepositoryConfiguration", new InfoTablePrimitive(creds));
            // The commit identity belongs to the user, not to one repository.
            new GitUserContextManager().setIdentity(GitCommitterFullName, GitCommitterEmail);
            return DapResults.success(
                    "GitCredentialCreate",
                    "Git credentials created.",
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure(
                    "GitCredentialCreate", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(RepositoryConfigurationSummarySpec.class)
    @ThingworxServiceDefinition(
            name = "GitCredentialList",
            description =
                    "Lists the current user's repository settings; passwords are never returned",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.RepositoryConfigurationSummary.ServiceResult.DataShape"
            })
    public InfoTable GitCredentialList() {
        try {
            return DapResults.success(
                    "GitCredentialList",
                    RepositoryConfigurationSummaryTable.SERVICE_RESULT,
                    RepositoryConfigurationSummaryTable.wrap(
                            credentialSummaries(new GitUserContextManager().credentials(), null)));
        } catch (Exception e) {
            _logger.error("GitCredentialList failed", e);
            return DapResults.failure(
                    "GitCredentialList", e, RepositoryConfigurationSummaryTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(RepositoryConfigurationSummarySpec.class)
    @ThingworxServiceDefinition(
            name = "GitCredentialGet",
            description =
                    "Returns the current user's settings for one repository; the password is never returned",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.RepositoryConfigurationSummary.ServiceResult.DataShape"
            })
    public InfoTable GitCredentialGet(
            @ThingworxServiceParameter(
                            name = "GitThing",
                            description = "Repository Thing",
                            baseType = "THINGNAME")
                    String gitThing) {
        try {
            InfoTable result =
                    gitThing == null
                            ? credentialSummaries(null, null)
                            : credentialSummaries(
                                    new GitUserContextManager().credentials(), gitThing);
            if (result.getRowCount() == 0)
                throw new IllegalArgumentException(
                        "Credentials not found for repository: " + gitThing);
            return DapResults.success(
                    "GitCredentialGet",
                    RepositoryConfigurationSummaryTable.SERVICE_RESULT,
                    RepositoryConfigurationSummaryTable.wrap(result));
        } catch (Exception e) {
            return DapResults.failure(
                    "GitCredentialGet", e, RepositoryConfigurationSummaryTable.SERVICE_RESULT);
        }
    }

    /**
     * Copies stored repository settings without the password. {@code onlyThing} limits the rows to
     * one repository when it is not {@code null}.
     */
    private static InfoTable credentialSummaries(InfoTable stored, String onlyThing)
            throws Exception {
        InfoTable result =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        "GIT.RepositoryConfigurationSummary.DataShape");
        if (stored == null) return result;
        GitUserContextManager context = new GitUserContextManager();
        for (int i = 0; i < stored.getRowCount(); i++) {
            ValueCollection row = stored.getRow(i);
            String thing = row.getStringValue("GitThing");
            if (onlyThing != null && !onlyThing.equals(thing)) continue;
            String password = row.getStringValue(Const.GitCommitterPassword);
            GitUserContextManager.Identity identity = context.identityFor(thing);
            ValueCollection summary = new ValueCollection();
            summary.put("GitThing", new StringPrimitive(nullToEmpty(thing)));
            summary.put(
                    Const.GitCommitterUser,
                    new StringPrimitive(nullToEmpty(row.getStringValue(Const.GitCommitterUser))));
            summary.put(
                    "HasPassword", new BooleanPrimitive(password != null && !password.isEmpty()));
            summary.put(Const.GitCommitterEmail, new StringPrimitive(identity.email()));
            summary.put(Const.GitCommitterName, new StringPrimitive(identity.name()));
            summary.put(
                    Const.GpgKeyFingerprint,
                    new StringPrimitive(nullToEmpty(row.getStringValue(Const.GpgKeyFingerprint))));
            summary.put(
                    Const.SshKeyFingerprint,
                    new StringPrimitive(nullToEmpty(row.getStringValue(Const.SshKeyFingerprint))));
            result.addRow(summary);
        }
        return result;
    }

    @DapServicePayload(UserIdentitySpec.class)
    @ThingworxServiceDefinition(
            name = "GitIdentityGet",
            description = "Returns the current user's commit name and email",
            category = "Identity",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.UserIdentity.ServiceResult.DataShape"
            })
    public InfoTable GitIdentityGet() {
        try {
            GitUserContextManager.Identity identity = new GitUserContextManager().identityFor(null);
            UserIdentityTable table = UserIdentityTable.create();
            table.add(
                    UserIdentityRow.create()
                            .gitUserName(identity.name())
                            .gitUserEmail(identity.email()));
            return DapResults.success("GitIdentityGet", UserIdentityTable.SERVICE_RESULT, table);
        } catch (Exception e) {
            return DapResults.failure("GitIdentityGet", e, UserIdentityTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "GitIdentitySave",
            description =
                    "Saves the name and email recorded on the current user's commits in every repository",
            category = "Identity",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable GitIdentitySave(
            @ThingworxServiceParameter(
                            name = "GitUserName",
                            description = "Name recorded on commits",
                            baseType = "STRING")
                    String name,
            @ThingworxServiceParameter(
                            name = "GitUserEmail",
                            description = "Email recorded on commits",
                            baseType = "STRING")
                    String email) {
        try {
            if (isBlank(name) || isBlank(email)) {
                throw new IllegalArgumentException("Commit name and email are both required.");
            }
            if (!email.trim().matches("[^@\\s<>]+@[^@\\s<>]+")) {
                throw new IllegalArgumentException("Enter a valid email address.");
            }
            initializeCurrentUserConfiguration();
            new GitUserContextManager().setIdentity(name, email);
            return DapResults.success(
                    "GitIdentitySave",
                    "Commit identity saved. It applies to your commits in every repository.",
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("GitIdentitySave", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "GitCommitIdentityUpdate",
            description =
                    "Deprecated: use GitIdentitySave. Saves the current user's commit identity, which applies to every repository; GitThing is ignored",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable GitCommitIdentityUpdate(
            @ThingworxServiceParameter(
                            name = "GitThing",
                            description = "Repository Thing",
                            baseType = "THINGNAME")
                    String gitThing,
            @ThingworxServiceParameter(
                            name = "GitCommitterFullName",
                            description = "Commit name",
                            baseType = "STRING")
                    String fullName,
            @ThingworxServiceParameter(
                            name = "GitCommitterEmail",
                            description = "Commit email",
                            baseType = "STRING")
                    String email) {
        try {
            if (fullName == null || fullName.isBlank() || email == null || email.isBlank())
                throw new IllegalArgumentException("Commit name and email are required.");
            initializeCurrentUserConfiguration();
            new GitUserContextManager().setIdentity(fullName, email);
            return DapResults.success(
                    "GitCommitIdentityUpdate",
                    "Commit identity saved.",
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure(
                    "GitCommitIdentityUpdate", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "GitCredentialUpdate",
            description = "Updates credentials for an existing repository record",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable GitCredentialUpdate(
            @ThingworxServiceParameter(
                            name = "GitCommitterUser",
                            description = "",
                            baseType = "STRING")
                    String user,
            @ThingworxServiceParameter(
                            name = "GitCommitterPassword",
                            description = "",
                            baseType = "STRING")
                    String password,
            @ThingworxServiceParameter(
                            name = "GitCommitterEmail",
                            description = "",
                            baseType = "STRING")
                    String email,
            @ThingworxServiceParameter(
                            name = "GitCommitterFullName",
                            description = "",
                            baseType = "STRING")
                    String fullName,
            @ThingworxServiceParameter(name = "GitThing", description = "", baseType = "THINGNAME")
                    String gitThing,
            @ThingworxServiceParameter(
                            name = "GpgKeyFingerprint",
                            description = "Optional GPG key fingerprint; blank clears signing",
                            baseType = "STRING")
                    String GpgKeyFingerprint) {
        try {
            initializeCurrentUserConfiguration();
            User current = new GitUserContextManager().requireUser();
            if (!GpgKeyFingerprint.isBlank()) {
                validateGpgKeyOwnership(current, GpgKeyFingerprint);
            }
            InfoTable all = new GitUserContextManager().credentials();
            if (all == null)
                throw new IllegalArgumentException(
                        "Credentials not found for repository: " + gitThing);
            boolean found = false;
            for (int i = 0; i < all.getRowCount(); i++)
                if (gitThing != null && gitThing.equals(all.getRow(i).getStringValue("GitThing"))) {
                    ValueCollection row = all.getRow(i);
                    row.put("GitCommitterUser", new StringPrimitive(user));
                    row.put("GitCommitterPassword", new PasswordPrimitive(password));
                    row.put(
                            Const.ApprovedRemoteOrigins,
                            new StringPrimitive(approvedOriginsFor(gitThing)));
                    if (!GpgKeyFingerprint.isBlank())
                        row.put(Const.GpgKeyFingerprint, new StringPrimitive(GpgKeyFingerprint));
                    else row.put(Const.GpgKeyFingerprint, new StringPrimitive(""));
                    found = true;
                }
            if (!found)
                throw new IllegalArgumentException(
                        "Credentials not found for repository: " + gitThing);
            current.setPropertyValue(
                    Const.UserRepositoryConfiguration, new InfoTablePrimitive(all));
            new GitUserContextManager().setIdentity(fullName, email);
            return DapResults.success(
                    "GitCredentialUpdate",
                    "Git credentials updated.",
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure(
                    "GitCredentialUpdate", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "GitRepositorySettingsSave",
            description =
                    "Creates or updates the current user's repository credentials and signing key; a blank password preserves an existing password",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable GitRepositorySettingsSave(
            @ThingworxServiceParameter(
                            name = "GitThing",
                            description = "Repository Thing",
                            baseType = "THINGNAME")
                    String gitThing,
            @ThingworxServiceParameter(
                            name = "GitCommitterUser",
                            description = "Git username",
                            baseType = "STRING")
                    String user,
            @ThingworxServiceParameter(
                            name = "GitCommitterPassword",
                            description = "New password, or blank to keep the existing password",
                            baseType = "STRING")
                    String password,
            @ThingworxServiceParameter(
                            name = "GitCommitterEmail",
                            description = "Committer email",
                            baseType = "STRING")
                    String email,
            @ThingworxServiceParameter(
                            name = "GitCommitterFullName",
                            description = "Committer name",
                            baseType = "STRING")
                    String fullName,
            @ThingworxServiceParameter(
                            name = "GpgKeyFingerprint",
                            description = "Signing key, or blank to disable signing",
                            baseType = "STRING")
                    String fingerprint) {
        try {
            if (gitThing == null || gitThing.isBlank()) {
                throw new IllegalArgumentException("Repository is required.");
            }
            GitUserContextManager context = new GitUserContextManager();
            InfoTable credentials = context.credentials();
            ValueCollection existing = null;
            if (credentials != null) {
                for (int i = 0; i < credentials.getRowCount(); i++) {
                    ValueCollection row = credentials.getRow(i);
                    if (gitThing.equals(row.getStringValue("GitThing"))) {
                        existing = row;
                        break;
                    }
                }
            }
            String safeUser = user == null ? "" : user;
            String safePassword = password == null ? "" : password;
            String safeEmail = email == null ? "" : email;
            String safeName = fullName == null ? "" : fullName;
            String safeFingerprint = fingerprint == null ? "" : fingerprint;
            if (existing == null) {
                return GitCredentialCreate(
                        safeUser, safePassword, safeEmail, safeName, gitThing, safeFingerprint);
            }
            if (safePassword.isEmpty() && existing.getPrimitive("GitCommitterPassword") != null) {
                safePassword =
                        ((PasswordPrimitive) existing.getPrimitive("GitCommitterPassword"))
                                .getValue();
            }
            return GitCredentialUpdate(
                    safeUser, safePassword, safeEmail, safeName, gitThing, safeFingerprint);
        } catch (Exception e) {
            return DapResults.failure(
                    "GitRepositorySettingsSave", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "GitCredentialDelete",
            description = "Deletes credentials for a repository",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable GitCredentialDelete(
            @ThingworxServiceParameter(
                            name = "GitThing",
                            description = "Repository Thing",
                            baseType = "THINGNAME")
                    String gitThing) {
        try {
            User current = new GitUserContextManager().requireUser();
            InfoTable all = new GitUserContextManager().credentials();
            if (all == null)
                throw new IllegalArgumentException(
                        "Credentials not found for repository: " + gitThing);
            InfoTable kept =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.GitCredentialsDataShapeName);
            boolean found = false;
            for (int i = 0; i < all.getRowCount(); i++) {
                ValueCollection row = all.getRow(i);
                if (gitThing != null && gitThing.equals(row.getStringValue("GitThing")))
                    found = true;
                else kept.addRow(row);
            }
            if (!found)
                throw new IllegalArgumentException(
                        "Credentials not found for repository: " + gitThing);
            current.setPropertyValue(
                    Const.UserRepositoryConfiguration, new InfoTablePrimitive(kept));
            return DapResults.success(
                    "GitCredentialDelete",
                    "Git credentials deleted.",
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure(
                    "GitCredentialDelete", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(SshKeySummarySpec.class)
    @ThingworxServiceDefinition(
            name = "SshKeyGenerate",
            description =
                    "Generates an SSH key for the current user and returns its public key; the private key is stored encrypted and never returned",
            category = "SSH",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.SshKeySummary.ServiceResult.DataShape"
            })
    public InfoTable SshKeyGenerate(
            @ThingworxServiceParameter(
                            name = "SshKeyLabel",
                            description = "Label for the new key",
                            baseType = "STRING")
                    String SshKeyLabel) {
        try {
            SshKeys.Generated generated = SshKeys.generate(SshKeyLabel);
            return sshKeySummaryResult(
                    "SshKeyGenerate",
                    storeSshKey(generated.privateKey(), "", generated.parsed(), SshKeyLabel));
        } catch (Exception e) {
            return DapResults.failure("SshKeyGenerate", e, SshKeySummaryTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(SshKeySummarySpec.class)
    @ThingworxServiceDefinition(
            name = "SshKeyImport",
            description =
                    "Stores an existing SSH private key for the current user; the key is stored encrypted and never returned",
            category = "SSH",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.SshKeySummary.ServiceResult.DataShape"
            })
    public InfoTable SshKeyImport(
            @ThingworxServiceParameter(
                            name = "SshPrivateKey",
                            description = "OpenSSH or PEM private key",
                            baseType = "STRING")
                    String SshPrivateKey,
            @ThingworxServiceParameter(
                            name = "SshKeyPassphrase",
                            description = "Passphrase, or blank when the key has none",
                            baseType = "STRING")
                    String SshKeyPassphrase,
            @ThingworxServiceParameter(
                            name = "SshKeyLabel",
                            description = "Label for the key",
                            baseType = "STRING")
                    String SshKeyLabel) {
        try {
            String passphrase = nullToEmpty(SshKeyPassphrase);
            SshKeys.Parsed parsed = SshKeys.parse(SshPrivateKey, passphrase, SshKeyLabel);
            return sshKeySummaryResult(
                    "SshKeyImport",
                    storeSshKey(SshPrivateKey.trim() + "\n", passphrase, parsed, SshKeyLabel));
        } catch (Exception e) {
            return DapResults.failure("SshKeyImport", e, SshKeySummaryTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(SshKeySummarySpec.class)
    @ThingworxServiceDefinition(
            name = "SshKeyListSummary",
            description = "Lists the current user's SSH keys without private key material",
            category = "SSH",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.SshKeySummary.ServiceResult.DataShape"
            })
    public InfoTable SshKeyListSummary() {
        try {
            InfoTable result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.SshKeySummaryDataShapeName);
            InfoTable keys = new GitUserContextManager().sshKeys();
            if (keys != null) {
                for (int i = 0; i < keys.getRowCount(); i++) {
                    result.addRow(sshKeySummary(keys.getRow(i)));
                }
            }
            return DapResults.success(
                    "SshKeyListSummary",
                    SshKeySummaryTable.SERVICE_RESULT,
                    SshKeySummaryTable.wrap(result));
        } catch (Exception e) {
            return DapResults.failure("SshKeyListSummary", e, SshKeySummaryTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "SshKeyDelete",
            description =
                    "Removes one of the current user's SSH keys and clears it from the user's repositories",
            category = "SSH",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable SshKeyDelete(
            @ThingworxServiceParameter(
                            name = "SshKeyFingerprint",
                            description = "Fingerprint of the key to delete",
                            baseType = "STRING")
                    String SshKeyFingerprint) {
        try {
            GitUserContextManager context = new GitUserContextManager();
            if (context.sshKey(SshKeyFingerprint) == null) {
                throw new IllegalArgumentException("SSH key not found: " + SshKeyFingerprint);
            }
            InfoTable keys = context.sshKeys();
            InfoTable kept =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.UserSshKeyDataShapeName);
            for (int i = 0; i < keys.getRowCount(); i++) {
                ValueCollection row = keys.getRow(i);
                if (!SshKeyFingerprint.equals(row.getStringValue(Const.SshKeyFingerprint))) {
                    kept.addRow(row);
                }
            }
            User user = context.requireUser();
            user.setPropertyValue(Const.UserSshKeys, new InfoTablePrimitive(kept));
            InfoTable configurations = context.credentials();
            if (configurations != null) {
                boolean changed = false;
                for (int i = 0; i < configurations.getRowCount(); i++) {
                    ValueCollection row = configurations.getRow(i);
                    if (SshKeyFingerprint.equals(row.getStringValue(Const.SshKeyFingerprint))) {
                        row.put(Const.SshKeyFingerprint, new StringPrimitive(""));
                        changed = true;
                    }
                }
                if (changed) {
                    user.setPropertyValue(
                            Const.UserRepositoryConfiguration,
                            new InfoTablePrimitive(configurations));
                }
            }
            context.clearRemoteSshKey(SshKeyFingerprint);
            return DapResults.success(
                    "SshKeyDelete", "SSH key deleted.", StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("SshKeyDelete", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "SetRepositorySshKey",
            description =
                    "Selects which of the current user's SSH keys is used for a repository's SSH remotes; blank clears it",
            category = "SSH",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable SetRepositorySshKey(
            @ThingworxServiceParameter(
                            name = "GitThing",
                            description = "Repository Thing",
                            baseType = "THINGNAME")
                    String GitThing,
            @ThingworxServiceParameter(
                            name = "SshKeyFingerprint",
                            description = "Key fingerprint, or blank to clear",
                            baseType = "STRING")
                    String SshKeyFingerprint) {
        try {
            if (GitThing == null || GitThing.isBlank()) {
                throw new IllegalArgumentException("Repository is required.");
            }
            String fingerprint = nullToEmpty(SshKeyFingerprint).trim();
            GitUserContextManager context = new GitUserContextManager();
            if (!fingerprint.isEmpty() && context.sshKey(fingerprint) == null) {
                throw new IllegalArgumentException(
                        "SSH key is not owned by the current user: " + fingerprint);
            }
            context.setRepositorySshKey(GitThing, fingerprint);
            return DapResults.success(
                    "SetRepositorySshKey",
                    fingerprint.isEmpty() ? "SSH key cleared." : "SSH key selected.",
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure(
                    "SetRepositorySshKey", e, StringResultContract.SERVICE_RESULT);
        }
    }

    /** Adds a key to the current user's SSH keys and returns its summary row. */
    private InfoTable storeSshKey(
            String privateKey, String passphrase, SshKeys.Parsed parsed, String label)
            throws Exception {
        initializeCurrentUserConfiguration();
        GitUserContextManager context = new GitUserContextManager();
        if (context.sshKey(parsed.fingerprint()) != null) {
            throw new IllegalArgumentException(
                    "This SSH key is already stored: " + parsed.fingerprint());
        }
        InfoTable keys = context.sshKeys();
        if (keys == null) {
            keys =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.UserSshKeyDataShapeName);
        }
        ValueCollection row = new ValueCollection();
        row.put(Const.SshKeyFingerprint, new StringPrimitive(parsed.fingerprint()));
        row.put(Const.SshPrivateKey, new PasswordPrimitive(privateKey));
        row.put(Const.SshKeyPassphrase, new PasswordPrimitive(passphrase));
        row.put(Const.SshPublicKey, new StringPrimitive(parsed.publicKey()));
        row.put(Const.SshKeyLabel, new StringPrimitive(nullToEmpty(label).trim()));
        keys.addRow(row);
        context.requireUser().setPropertyValue(Const.UserSshKeys, new InfoTablePrimitive(keys));
        InfoTable result =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.SshKeySummaryDataShapeName);
        result.addRow(sshKeySummary(row));
        return result;
    }

    private static ValueCollection sshKeySummary(ValueCollection key) {
        ValueCollection row = new ValueCollection();
        row.put(
                Const.SshKeyFingerprint,
                new StringPrimitive(nullToEmpty(key.getStringValue(Const.SshKeyFingerprint))));
        row.put(
                Const.SshKeyLabel,
                new StringPrimitive(nullToEmpty(key.getStringValue(Const.SshKeyLabel))));
        row.put(
                Const.SshPublicKey,
                new StringPrimitive(nullToEmpty(key.getStringValue(Const.SshPublicKey))));
        return row;
    }

    /** Result for a newly stored key; the message gives the public key to register. */
    private static InfoTable sshKeySummaryResult(String service, InfoTable rows) throws Exception {
        String publicKey = rows.getRow(0).getStringValue(Const.SshPublicKey);
        return DapResults.success(
                service,
                "SSH key stored. Add this public key to your account's SSH keys on the Git server: "
                        + publicKey,
                SshKeySummaryTable.SERVICE_RESULT,
                SshKeySummaryTable.wrap(rows));
    }

    private void validateGpgKeyOwnership(User user, String fingerprint) throws Exception {
        new GitUserContextManager().validateGpgKeyOwnership(fingerprint);
    }

    /**
     * Returns the Git operation tab list used by the ManagementConsole mashup.
     *
     * <p>The returned rows carry the tab value ({@code Value}) and display label ({@code
     * Description}) consumed by the operations dropdown and tab set. Kept as its own service so the
     * Operation names stay data-driven rather than hard-coded in the mashup.
     */
    @DapServicePayload(OperationListSpec.class)
    @ThingworxServiceDefinition(
            name = "GetOperations",
            description = "Returns the Git operations tab list for the ManagementConsole mashup",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.OperationList.ServiceResult.DataShape"
            })
    public InfoTable GetOperations() {
        try {
            OperationListTable operations = OperationListTable.create();
            operations.add(
                    OperationListRow.create()
                            .value("Commit")
                            .description("Commit & Push")
                            .ordinal(1));
            operations.add(OperationListRow.create().value("Pull").description("Pull").ordinal(2));
            operations.add(
                    OperationListRow.create().value("Branch").description("Branch").ordinal(3));
            operations.add(
                    OperationListRow.create().value("Settings").description("Settings").ordinal(4));
            operations.add(OperationListRow.create().value("Log").description("Log").ordinal(5));
            operations.add(
                    OperationListRow.create().value("New Repo").description("New Repo").ordinal(6));
            operations.add(
                    OperationListRow.create()
                            .value("Delete")
                            .description("Delete Repo")
                            .ordinal(7));
            return DapResults.success(
                    "GetOperations", OperationListTable.SERVICE_RESULT, operations);
        } catch (Exception e) {
            return DapResults.failure("GetOperations", e, OperationListTable.SERVICE_RESULT);
        }
    }

    /**
     * Returns recent Git action entries recorded by {@link GitActionLogger} in the ApplicationLog.
     *
     * <p>A dedicated Git action log stream may replace the storage later without changing this row
     * contract.
     */
    @DapServicePayload(GitLogSpec.class)
    @ThingworxServiceDefinition(
            name = "GetGitLog",
            description = "Returns recent Git action entries recorded in the extension log",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.GitLog.ServiceResult.DataShape"})
    public InfoTable GetGitLog(
            @ThingworxServiceParameter(
                            name = "MaxEntries",
                            description = "Maximum number of log entries to return; default 100",
                            baseType = "NUMBER")
                    Double MaxEntries) {
        try {
            int maxEntries =
                    MaxEntries == null || MaxEntries < 1
                            ? 100
                            : (int) Math.min(MaxEntries.doubleValue(), 1000.0);
            boolean admin = callerIsAdmin();
            GitLogTable log = GitLogTable.create();
            java.util.List<ExtensionLog.Entry> entries;
            try {
                entries = ExtensionLog.read(GitActionLogger.class, maxEntries);
            } catch (IllegalArgumentException e) {
                if (!"ApplicationLog entity is not available".equals(e.getMessage())) throw e;
                entries = java.util.List.of();
            }
            for (ExtensionLog.Entry entry : entries) {
                GitActionLogger.Action action = GitActionLogger.parse(entry.content());
                if (action == null) continue;
                if (!admin && !callerCanReadRepository(action.repoName())) continue;
                log.add(
                        GitLogRow.create()
                                .timestamp(entry.timestamp())
                                .user(action.user().isBlank() ? entry.user() : action.user())
                                .action(action.action())
                                .repoName(action.repoName())
                                .level(entry.level())
                                .message(action.message()));
            }
            return DapResults.success("GetGitLog", GitLogTable.SERVICE_RESULT, log);
        } catch (Exception e) {
            _logger.error("GetGitLog failed", e);
            return DapResults.failure("GetGitLog", e, GitLogTable.SERVICE_RESULT);
        }
    }

    /**
     * Returns entries the extension wrote to the ApplicationLog, newest first. Only entries at or
     * above the ApplicationLog's configured level are recorded; ThingWorx defaults to WARN.
     */
    @DapServicePayload(ExtensionLogSpec.class)
    @ThingworxServiceDefinition(
            name = "GetExtensionLog",
            description =
                    "Returns entries the Git extension wrote to the ApplicationLog, newest first.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.ExtensionLog.ServiceResult.DataShape"
            })
    public InfoTable GetExtensionLog(
            @ThingworxServiceParameter(
                            name = "MaxEntries",
                            description = "Maximum number of entries; default 200, at most 2000",
                            baseType = "INTEGER")
                    Integer MaxEntries,
            @ThingworxServiceParameter(
                            name = "MinimumLevel",
                            description =
                                    "Lowest level to include: TRACE, DEBUG, INFO, WARN, or ERROR. Blank includes every level.",
                            baseType = "STRING")
                    String MinimumLevel,
            @ThingworxServiceParameter(
                            name = "SearchText",
                            description =
                                    "Only include entries whose message contains this text, ignoring case",
                            baseType = "STRING")
                    String SearchText) {
        try {
            UserUtilities.validateAdminMembership();
            int maxEntries =
                    MaxEntries == null || MaxEntries < 1 ? 200 : Math.min(MaxEntries, 2000);
            ExtensionLogTable log = ExtensionLogTable.create();
            for (ExtensionLog.Entry entry :
                    ExtensionLog.readAll(maxEntries, MinimumLevel, SearchText)) {
                log.add(
                        ExtensionLogRow.create()
                                .timestamp(entry.timestamp())
                                .level(entry.level())
                                .source(entry.source())
                                .message(entry.content())
                                .user(entry.user())
                                .thread(entry.thread())
                                .origin(entry.origin()));
            }
            return DapResults.success("GetExtensionLog", ExtensionLogTable.SERVICE_RESULT, log);
        } catch (Exception e) {
            _logger.error("GetExtensionLog failed", e);
            return DapResults.failure("GetExtensionLog", e, ExtensionLogTable.SERVICE_RESULT);
        }
    }

    /**
     * Returns the AG Grid column configuration for the entity grid used by the Commit/Push mashup.
     * Ported from the Sabel Git.ThingShape.ReleaseManager script implementation so the mashup no
     * longer depends on app-owned entities.
     */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "GetAllEntitiesGridConfig",
            description = "Returns the AG Grid column configuration for the entity grid",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable GetAllEntitiesGridConfig() {
        return DapResults.success(
                "GetAllEntitiesGridConfig",
                ALL_ENTITIES_GRID_CONFIG_JSON,
                StringResultContract.SERVICE_RESULT);
    }

    private static final String ALL_ENTITIES_GRID_CONFIG_JSON =
            "{\"ColumnConfig\":{"
                    + "\"name\":{\"field\":\"name\",\"headerName\":\"Name\",\"filter\":\"agTextColumnFilter\",\"editable\":false,\"hide\":false,\"enableRowGroup\":true,\"headerCheckboxSelectionFilteredOnly\":true,\"headerCheckboxSelection\":true,\"minWidth\":300,\"suppressHeaderMenuButton\":true},"
                    + "\"type\":{\"field\":\"type\",\"headerName\":\"Type\",\"filter\":\"agSetColumnFilter\",\"editable\":false,\"hide\":false,\"enableRowGroup\":true,\"width\":95,\"suppressHeaderMenuButton\":true},"
                    + "\"changeAction\":{\"suppressHeaderMenuButton\":true,\"field\":\"changeAction\",\"headerName\":\"Change Action\",\"filter\":\"agSetColumnFilter\",\"editable\":false,\"hide\":false,\"enableRowGroup\":true,\"width\":128},"
                    + "\"changeReason\":{\"suppressHeaderMenuButton\":true,\"field\":\"changeReason\",\"headerName\":\"Change Reason\",\"filter\":\"agTextColumnFilter\",\"editable\":false,\"hide\":false,\"enableRowGroup\":true,\"flex\":1},"
                    + "\"user\":{\"suppressHeaderMenuButton\":true,\"field\":\"user\",\"headerName\":\"Modified By\",\"filter\":\"agSetColumnFilter\",\"editable\":false,\"hide\":false,\"enableRowGroup\":true,\"minWidth\":200},"
                    + "\"timestamp\":{\"suppressHeaderMenuButton\":true,\"field\":\"timestamp\",\"headerName\":\"Modified\",\"filter\":\"agDateColumnFilter\",\"editable\":false,\"hide\":false,\"enableRowGroup\":true,\"width\":150,\"sort\":\"desc\"},"
                    + "\"project\":{\"suppressHeaderMenuButton\":true,\"field\":\"project\",\"headerName\":\"Project\",\"filter\":\"agSetColumnFilter\",\"editable\":false,\"hide\":false,\"enableRowGroup\":true,\"minWidth\":200},"
                    + "\"id\":{\"suppressHeaderMenuButton\":true,\"field\":\"id\",\"headerName\":\"ID\",\"filter\":\"agSetColumnFilter\",\"editable\":false,\"hide\":true,\"enableRowGroup\":true},"
                    + "\"parentId\":{\"suppressHeaderMenuButton\":true,\"field\":\"parentId\",\"headerName\":\"Parent ID\",\"filter\":\"agSetColumnFilter\",\"editable\":false,\"hide\":true,\"enableRowGroup\":true},"
                    + "\"link\":{\"suppressHeaderMenuButton\":true,\"field\":\"link\",\"headerName\":\"Link\",\"cellRenderer\":\"CustomHyperlinkRenderer\",\"editable\":false,\"hide\":false,\"width\":68},"
                    + "\"committed\":{\"suppressHeaderMenuButton\":true,\"field\":\"committed\",\"headerName\":\"Committed\",\"filter\":\"agSetColumnFilter\",\"cellRenderer\":\"CustomCheckboxRenderer\",\"editable\":false,\"hide\":false,\"width\":110}"
                    + "},\"GridOptions\":{\"statusBar\":{\"statusPanels\":[{\"statusPanel\":\"agTotalAndFilteredRowCountComponent\",\"align\":\"left\"}]}}}";
}
