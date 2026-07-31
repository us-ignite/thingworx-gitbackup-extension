package org.us_ignite.thingworx.jgit.extension;

import static org.us_ignite.thingworx.jgit.extension.Values.hasText;
import static org.us_ignite.thingworx.jgit.extension.Values.isBlank;
import static org.us_ignite.thingworx.jgit.extension.Values.isTrue;
import static org.us_ignite.thingworx.jgit.extension.Values.orDefault;
import static org.us_ignite.thingworx.jgit.extension.Values.primitiveString;

import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.entities.interfaces.IServiceProvider;
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
import com.thingworx.types.primitives.DatetimePrimitive;
import com.thingworx.types.primitives.GUIDPrimitive;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.IntegerPrimitive;
import com.thingworx.types.primitives.PasswordPrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import com.thingworx.webservices.context.ThreadLocalContext;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.eclipse.jgit.api.Git;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.slf4j.Logger;

@ThingworxBaseTemplateDefinition(name = "GenericThing")
public class GitUtilityThingShape extends Thing {

    private static final long serialVersionUID = 9085129963750550674L;
    private static Logger _logger =
            LogUtilities.getInstance().getApplicationLogger(GitUtilityThingShape.class);

    public GitUtilityThingShape() {}

    @ThingworxServiceDefinition(
            name = "AddEntitiesToExportList",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:SpotlightSearch"})
    public InfoTable AddEntitiesToExportList(
            @ThingworxServiceParameter(
                            name = "existingEntities",
                            description = "",
                            baseType = "INFOTABLE",
                            aspects = {"dataShape:SpotlightSearch"})
                    InfoTable existingEntities,
            @ThingworxServiceParameter(
                            name = "newEntitiesToExport",
                            description = "",
                            baseType = "INFOTABLE",
                            aspects = {"dataShape:SpotlightSearch"})
                    InfoTable newEntitiesToExport)
            throws Exception {
        if (existingEntities == null || existingEntities.getRowCount() == 0) {
            existingEntities =
                    InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
        }
        InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
        for (int x = 0; x < existingEntities.getRowCount(); x++) {
            ValueCollection existingRow = existingEntities.getRow(x);
            String existingName = existingRow.getPrimitive("name").getValue().toString();
            boolean found = false;
            for (int y = 0; y < newEntitiesToExport.getRowCount(); y++) {
                String newName =
                        newEntitiesToExport.getRow(y).getPrimitive("name").getValue().toString();
                if (existingName.equals(newName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                result.addRow(existingRow);
            }
        }
        for (int x = 0; x < newEntitiesToExport.getRowCount(); x++) {
            result.addRow(newEntitiesToExport.getRow(x));
        }
        return result;
    }

    @ThingworxServiceDefinition(
            name = "AddLogEntry",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void AddLogEntry(
            @ThingworxServiceParameter(name = "Content", description = "", baseType = "STRING")
                    String Content,
            @ThingworxServiceParameter(name = "ServiceName", description = "", baseType = "STRING")
                    String ServiceName,
            @ThingworxServiceParameter(name = "Source", description = "", baseType = "STRING")
                    String Source,
            @ThingworxServiceParameter(name = "timestamp", description = "", baseType = "DATETIME")
                    DateTime timestamp,
            @ThingworxServiceParameter(name = "User", description = "", baseType = "STRING")
                    String User)
            throws Exception {
        InfoTable values =
                InfoTableInstanceFactory.createInfoTableFromDataShape("GIT.ExtensionLog.DataShape");
        ValueCollection entry = new ValueCollection();
        entry.put("ID", new GUIDPrimitive(java.util.UUID.randomUUID().toString()));
        entry.put("timestamp", new DatetimePrimitive(timestamp));
        entry.put("User", new StringPrimitive(User));
        entry.put("ServiceName", new StringPrimitive(ServiceName));
        entry.put("Content", new StringPrimitive(Content));
        entry.put("Source", new StringPrimitive(Source));
        values.addRow(entry);
        Thing dataTable =
                (Thing)
                        EntityUtilities.findEntity(
                                "GIT.ExtensionLog.DataTable", ThingworxRelationshipTypes.Thing);
        ValueCollection params = new ValueCollection();
        params.put("values", new InfoTablePrimitive(values));
        params.put("source", new StringPrimitive(Source));
        dataTable.processServiceRequest("AddDataTableEntry", params);
    }

    @ThingworxServiceDefinition(
            name = "AddNewRepo",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void AddNewRepo(
            @ThingworxServiceParameter(name = "RepoName", description = "", baseType = "STRING")
                    String RepoName,
            @ThingworxServiceParameter(name = "GitRepoURL", description = "", baseType = "STRING")
                    String GitRepoURL,
            @ThingworxServiceParameter(name = "RepoPath", description = "", baseType = "STRING")
                    String RepoPath,
            @ThingworxServiceParameter(name = "FileRepo", description = "", baseType = "STRING")
                    String FileRepo,
            @ThingworxServiceParameter(name = "User", description = "", baseType = "STRING")
                    String User,
            @ThingworxServiceParameter(name = "Password", description = "", baseType = "STRING")
                    String Password,
            @ThingworxServiceParameter(name = "CommitUser", description = "", baseType = "STRING")
                    String CommitUser,
            @ThingworxServiceParameter(name = "CommitEmail", description = "", baseType = "STRING")
                    String CommitEmail,
            @ThingworxServiceParameter(
                            name = "InitialBranch",
                            description = "",
                            baseType = "STRING")
                    String InitialBranch,
            @ThingworxServiceParameter(
                            name = "UseProxy",
                            description = "",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean UseProxy,
            @ThingworxServiceParameter(name = "ProxyURL", description = "", baseType = "STRING")
                    String ProxyURL,
            @ThingworxServiceParameter(
                            name = "ProxyPort",
                            description = "",
                            baseType = "INTEGER",
                            aspects = {"defaultValue:0"})
                    Integer ProxyPort,
            @ThingworxServiceParameter(
                            name = "LocalizationTokensPrefix",
                            description = "prefix used for exporting Localization Tokens",
                            baseType = "STRING")
                    String LocalizationTokensPrefix,
            @ThingworxServiceParameter(
                            name = "ProjectName",
                            description = "ThingWorx project to sync entities from (optional)",
                            baseType = "STRING")
                    String ProjectName)
            throws Exception {
        if (RepoName == null || GitRepoURL == null) {
            _logger.error("AddNewRepo: RepoName and GitRepoURL are required.");
            return;
        }
        EntityServices es = new EntityServices();
        es.CreateThing(
                RepoName,
                "GitRepository created by user " + GetCurrentUser() + " at " + new java.util.Date(),
                new TagCollection(),
                ProjectName,
                "GIT.Repository.ThingTemplate");
        Thing repoThing =
                (Thing) EntityUtilities.findEntity(RepoName, ThingworxRelationshipTypes.Thing);
        repoThing.processServiceRequest("EnableThing", new ValueCollection());
        repoThing.processServiceRequest("RestartThing", new ValueCollection());
        repoThing = (Thing) EntityUtilities.findEntity(RepoName, ThingworxRelationshipTypes.Thing);

        ValueCollection getConfigParams = new ValueCollection();
        getConfigParams.put("tableName", new StringPrimitive("Configuration"));
        InfoTable configTable =
                (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
        ValueCollection configRow =
                configTable.getRowCount() > 0 ? configTable.getRow(0) : new ValueCollection();
        configRow.put("FileRepository", new StringPrimitive(orDefault(FileRepo, "GitRepository")));
        configRow.put("GitRepoURL", new StringPrimitive(GitRepoURL));
        configRow.put("RepoPathName", new StringPrimitive(RepoPath));
        configRow.put("BranchName", new StringPrimitive(InitialBranch));
        configRow.put("UseProxy", new BooleanPrimitive(isTrue(UseProxy)));
        configRow.put("ProxyURL", new StringPrimitive(ProxyURL));
        configRow.put("ProxyPort", integerPrimitive(ProxyPort != null ? ProxyPort : 0));
        configRow.put("LocalizationTokensPrefix", new StringPrimitive(LocalizationTokensPrefix));
        if (hasText(ProjectName)) {
            configRow.put("ProjectName", new StringPrimitive(ProjectName));
        }
        if (configTable.getRowCount() == 0) configTable.addRow(configRow);

        ValueCollection setConfigParams = new ValueCollection();
        setConfigParams.put("configurationTable", new InfoTablePrimitive(configTable));
        setConfigParams.put("persistent", new BooleanPrimitive(false));
        setConfigParams.put("tableName", new StringPrimitive("Configuration"));
        repoThing.processServiceRequest("SetConfigurationTable", setConfigParams);
        repoThing.processServiceRequest("SaveConfigurationTables", new ValueCollection());
        repoThing.processServiceRequest("RestartThing", new ValueCollection());

        try {
            SetGitCredentials(User, Password, CommitEmail, CommitUser, RepoName);
        } catch (Exception e) {
            _logger.error(
                    "GIT Repository Thing "
                            + RepoName
                            + " was created but saving credentials failed: "
                            + e.getMessage());
        }
    }

    @ThingworxServiceDefinition(
            name = "DeleteGitThing",
            description =
                    "Deletes a GIT Repository Thing involves two operations: 1. Deleting the Thing itself and 2. Deleting the FileRepository subfolder that stored that Git repository.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void DeleteGitThing(
            @ThingworxServiceParameter(name = "RepoName", description = "", baseType = "STRING")
                    String RepoName)
            throws Exception {
        Thing repoThing =
                (Thing) EntityUtilities.findEntity(RepoName, ThingworxRelationshipTypes.Thing);
        ValueCollection config = new ValueCollection();
        config.put("tableName", new StringPrimitive("Configuration"));
        InfoTable cfgTable =
                (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", config);
        String str_RepositoryName =
                cfgTable.getRow(0).getPrimitive("FileRepository").getValue().toString();
        String str_RepositoryPathName =
                cfgTable.getRow(0).getPrimitive("FileRepoPath").getValue().toString();

        try {
            repoThing.processServiceRequest("DeleteLocalRepoContent", new ValueCollection());
        } catch (Exception ex) {
            _logger.error(
                    "Deleting Git Repository folder content failed when deleting Thing "
                            + RepoName);
        }
        EntityServices es = new EntityServices();
        es.DeleteThing(RepoName);
        try {
            FileRepositoryThing fileRepo =
                    (FileRepositoryThing)
                            EntityUtilities.findEntity(
                                    str_RepositoryName, ThingworxRelationshipTypes.Thing);
            ValueCollection deleteFolderParams = new ValueCollection();
            deleteFolderParams.put("path", new StringPrimitive(str_RepositoryPathName));
            fileRepo.processServiceRequest("DeleteFolder", deleteFolderParams);
        } catch (Exception ex) {
            _logger.error("Deleting Git Repository folder failed when deleting Thing " + RepoName);
        }
        String str_CurrentUser = GetCurrentUser();
        try {
            User user = UserUtilities.findUser(str_CurrentUser);
            if (user != null) {
                InfoTable creds = getGitCredentials(user);
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
                    user.setPropertyValue("UserRepositoryConfiguration", new InfoTablePrimitive(creds));
                }
            }
        } catch (Exception ex) {
            _logger.warn("Could not clean up credentials for deleted thing: " + RepoName);
        }
        _logger.warn(
                "GIT Repository Thing "
                        + RepoName
                        + " stored in File Repository: "
                        + str_RepositoryName
                        + " was deleted successfully.");
    }

    @ThingworxServiceDefinition(
            name = "GetEmptyInfotable",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {})
    public InfoTable GetEmptyInfotable() throws Exception {
        return InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
    }

    @ThingworxServiceDefinition(
            name = "GetProjectEntities",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:SpotlightSearch"})
    public InfoTable GetProjectEntities(
            @ThingworxServiceParameter(name = "project", description = "", baseType = "STRING")
                    String project,
            @ThingworxServiceParameter(name = "entityName", description = "", baseType = "STRING")
                    String entityName,
            @ThingworxServiceParameter(name = "entityType", description = "", baseType = "STRING")
                    String entityType,
            @ThingworxServiceParameter(
                            name = "includeDependents",
                            description = "",
                            baseType = "BOOLEAN")
                    Boolean includeDependents,
            @ThingworxServiceParameter(name = "tags", description = "", baseType = "TAGS")
                    TagCollection tags)
            throws Exception {
        if (isBlank(project))
            throw new Exception(
                    Const.ERR_PREFIX_CONFIG + "Project name was not specified or is empty.");

        InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
        IServiceProvider projectProvider =
                (IServiceProvider)
                        EntityUtilities.findEntity(project, ThingworxRelationshipTypes.Project);
        if (projectProvider == null)
            throw new Exception(String.format(Const.ERR_PROJECT_NOT_FOUND, project));

        java.util.List<String> projectNames = new java.util.ArrayList<>();
        projectNames.add(project);
        if (isTrue(includeDependents)) {
            InfoTable deps =
                    (InfoTable)
                            projectProvider.processServiceRequest(
                                    "GetAllDependentProjectNames", new ValueCollection());
            for (int i = 0; i < deps.getRowCount(); i++) {
                projectNames.add(deps.getRow(i).getPrimitive("item").getValue().toString());
            }
        }

        for (String pname : projectNames) {
            JSONObject emptyFilter = new JSONObject();
            Searcher searcher =
                    (Searcher)
                            EntityUtilities.findEntity(
                                    "SearchFunctions", ThingworxRelationshipTypes.Resource);
            InfoTable searchResult =
                    searcher.SpotlightSearch(
                            "", // searchExpression
                            tags != null ? tags : new TagCollection(), // tags
                            emptyFilter, // types
                            emptyFilter, // thingTemplates
                            emptyFilter, // thingShapes
                            emptyFilter, // aspects
                            emptyFilter, // excludedAspects
                            null, // startDate
                            null, // endDate
                            false, // searchDescriptions
                            false, // withPermissions
                            "lastModifiedDate", // sortBy
                            false, // isAscending
                            30000.0, // maxItems
                            null, // maxSearchItems
                            pname, // projectName
                            false // includeInheritedThingShapes
                            );
            result = unionInfoTables(result, searchResult);
        }

        if (hasText(entityName)) {
            result = queryInfoTable(result, "name", "LIKE", "*" + entityName + "*");
        }
        if (hasText(entityType)) {
            result = queryInfoTable(result, "type", "LIKE", "*" + entityType + "*");
        }
        return result;
    }

    @ThingworxServiceDefinition(
            name = "ImportEntity",
            description = "This will import an entity in the system.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void ImportEntity(
            @ThingworxServiceParameter(
                            name = "GitThingName",
                            description = "GIT Repository Thing used for the import",
                            baseType = "STRING")
                    String GitThingName,
            @ThingworxServiceParameter(
                            name = "entityPath",
                            description = "relative to the repository",
                            baseType = "STRING")
                    String entityPath,
            @ThingworxServiceParameter(
                            name = "FileRepositoryName",
                            description = "",
                            baseType = "STRING")
                    String FileRepositoryName,
            @ThingworxServiceParameter(
                            name = "ignoreDependencies",
                            description = "If true, strips dependency validation during import",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean ignoreDependencies)
            throws Exception {
        _logger.warn("Started single entity import.");
        if (isBlank(GitThingName))
            throw new Exception(Const.ERR_PREFIX_CONFIG + Const.ERR_GIT_THING_NAME_REQUIRED);
        repositoryThing(GitThingName);
        importSourceControlledEntities(FileRepositoryName, entityPath);
    }

    @ThingworxServiceDefinition(
            name = "ImportProjectEntities",
            description =
                    "Bulk imports all entity XML files from a FileRepository path. Returns a summary INFOTABLE with success/failure per entity.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {})
    public InfoTable ImportProjectEntities(
            @ThingworxServiceParameter(
                            name = "GitThingName",
                            description =
                                    "GIT Repository Thing name whose FileRepository and path to scan",
                            baseType = "STRING")
                    String GitThingName,
            @ThingworxServiceParameter(
                            name = "entityPath",
                            description =
                                    "Relative path within the FileRepository to scan for XML files",
                            baseType = "STRING")
                    String entityPath,
            @ThingworxServiceParameter(
                            name = "ignoreDependencies",
                            description = "If true, strips dependency validation during import",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean ignoreDependencies)
            throws Exception {
        _logger.warn("Started bulk import for GitThing: " + GitThingName);
        if (isBlank(GitThingName))
            throw new Exception(Const.ERR_PREFIX_CONFIG + Const.ERR_GIT_THING_NAME_REQUIRED);

        Thing repoThing =
                (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
        ValueCollection getConfigParams = new ValueCollection();
        getConfigParams.put("tableName", new StringPrimitive("Configuration"));
        InfoTable cfgTable =
                (InfoTable)
                        repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
        String str_FileRepositoryName =
                cfgTable.getRow(0).getPrimitive("FileRepository").getValue().toString();

        String str_RepoPath = entityPath;
        if (isBlank(str_RepoPath) || str_RepoPath.equals("*")) {
            str_RepoPath = cfgTable.getRow(0).getPrimitive("RepoPathName").getValue().toString();
        }
        while (str_RepoPath.startsWith("/")) str_RepoPath = str_RepoPath.substring(1);
        while (str_RepoPath.endsWith("/"))
            str_RepoPath = str_RepoPath.substring(0, str_RepoPath.length() - 1);

        Thing fileRepo =
                (Thing)
                        EntityUtilities.findEntity(
                                str_FileRepositoryName, ThingworxRelationshipTypes.Thing);

        // Recursively list all XML files
        InfoTable allFiles =
                InfoTableInstanceFactory.createInfoTableFromDataShape("FileSystemFile");
        collectXmlFiles(fileRepo, str_RepoPath, allFiles);
        _logger.warn("Found " + allFiles.getRowCount() + " XML files for import.");

        InfoTable result = new InfoTable();
        int int_SuccessCount = 0;
        int int_FailCount = 0;

        for (int x = 0; x < allFiles.getRowCount(); x++) {
            ValueCollection fileRow = allFiles.getRow(x);
            String filePath = fileRow.getPrimitive("path").getValue().toString();
            fileRow.getPrimitive("name").getValue().toString();
            // Strip leading slash from path — ListFiles returns paths like "/GIT/..."
            String cleanPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
            ValueCollection entry = new ValueCollection();
            entry.put("testName", new StringPrimitive(cleanPath));
            entry.put(
                    "startTimestamp",
                    new DatetimePrimitive(new DateTime(System.currentTimeMillis())));
            try {
                importSourceControlledEntities(str_FileRepositoryName, cleanPath);
                entry.put("passed", new BooleanPrimitive(true));
                entry.put("comments", new StringPrimitive("Import completed."));
                int_SuccessCount++;
                _logger.warn("Successfully imported: " + cleanPath);
            } catch (Exception ex) {
                entry.put("passed", new BooleanPrimitive(false));
                entry.put("comments", new StringPrimitive("Import failed: " + ex.getMessage()));
                int_FailCount++;
                _logger.error("Failed to import: " + cleanPath + "; Error: " + ex.getMessage());
            }
            entry.put(
                    "endTimestamp",
                    new DatetimePrimitive(new DateTime(System.currentTimeMillis())));
            result.addRow(entry);
        }
        _logger.warn(
                "Bulk import completed. Success: "
                        + int_SuccessCount
                        + ", Failed: "
                        + int_FailCount
                        + ", Total: "
                        + allFiles.getRowCount());
        return result;
    }

    private void collectXmlFiles(Thing fileRepo, String path, InfoTable allFiles) throws Exception {
        ValueCollection listParams = new ValueCollection();
        listParams.put("path", new StringPrimitive(path));
        listParams.put("nameMask", new StringPrimitive("*.xml"));
        InfoTable files = (InfoTable) fileRepo.processServiceRequest("ListFiles", listParams);
        for (int i = 0; i < files.getRowCount(); i++) {
            ValueCollection row = files.getRow(i);
            String name = primitiveString(row, "name");
            String p = primitiveString(row, "path");
            if (isBlank(name) || isBlank(p)) continue;
            ValueCollection newRow = new ValueCollection();
            newRow.put("name", new StringPrimitive(name));
            newRow.put("path", new StringPrimitive(p));
            boolean dup = false;
            for (int j = 0; j < allFiles.getRowCount(); j++) {
                if (p.equals(primitiveString(allFiles.getRow(j), "path"))) {
                    dup = true;
                    break;
                }
            }
            if (!dup) allFiles.addRow(newRow);
        }
        ValueCollection dirParams = new ValueCollection();
        dirParams.put("path", new StringPrimitive(path));
        dirParams.put("nameMask", new StringPrimitive(""));
        InfoTable dirs = (InfoTable) fileRepo.processServiceRequest("ListDirectories", dirParams);
        for (int i = 0; i < dirs.getRowCount(); i++) {
            String dirPath = primitiveString(dirs.getRow(i), "path");
            if (isBlank(dirPath)) continue;
            collectXmlFiles(fileRepo, dirPath, allFiles);
        }
    }

    private Thing repositoryThing(String thingName) throws Exception {
        Thing repoThing =
                (Thing) EntityUtilities.findEntity(thingName, ThingworxRelationshipTypes.Thing);
        if (repoThing == null)
            throw new Exception(Const.ERR_PREFIX_CONFIG + "GIT Repository Thing not found: " + thingName);
        return repoThing;
    }

    private void importSourceControlledEntities(String repositoryName, String path)
            throws Exception {
        Object resource =
                EntityUtilities.findEntity(
                        "SourceControlFunctions", ThingworxRelationshipTypes.Resource);
        if (resource == null)
            throw new Exception(Const.ERR_PREFIX_SYSTEM + Const.ERR_NO_SCF_RESOURCE);
        IServiceProvider sourceControlFunctions = (IServiceProvider) resource;
        ValueCollection params = new ValueCollection();
        params.put("repositoryName", new StringPrimitive(repositoryName));
        params.put("path", new StringPrimitive(path));
        params.put("useDefaultDataProvider", new BooleanPrimitive(true));
        params.put("withSubsystems", new BooleanPrimitive(false));
        params.put("overwritePropertyValues", new BooleanPrimitive(true));
        sourceControlFunctions.processServiceRequest("ImportSourceControlledEntities", params);
    }

    @ThingworxServiceDefinition(
            name = "InitUserExtensionProperties",
            description = "Adds UserExtension Properties needed by the JGitExtension",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void InitUserExtensionProperties() throws Exception {
        ThingShape userExtensions =
                (ThingShape)
                        EntityUtilities.findEntity(
                                "UserExtensions", ThingworxRelationshipTypes.ThingShape);

        User user = null;
        String str_CurrentUser = null;
        try {
            str_CurrentUser = GetCurrentUser();
            user = UserUtilities.findUser(str_CurrentUser);
        } catch (Exception e) {
            _logger.warn(
                    "InitUserExtensionProperties: no user context ("
                            + e.getMessage()
                            + "); updating ThingShape definitions only");
        }

        boolean hasGitCredentials = false;
        if (user != null) {
            try {
                hasGitCredentials = user.getPropertyValue("UserRepositoryConfiguration") != null;
            } catch (Exception e) {
            }
        }
        if (!hasGitCredentials) {
            ValueCollection addPropParams = new ValueCollection();
            addPropParams.put("defaultValue", new StringPrimitive(""));
            addPropParams.put(
                    "description", new StringPrimitive("User-specific Git credentials store."));
            addPropParams.put("readOnly", new BooleanPrimitive(false));
            addPropParams.put("type", new StringPrimitive("INFOTABLE"));
            addPropParams.put("name", new StringPrimitive("UserRepositoryConfiguration"));
            addPropParams.put("persistent", new BooleanPrimitive(true));
            addPropParams.put("dataShape", new StringPrimitive("GIT.RepositoryConfiguration.UserExtension.DataShape"));
            userExtensions.processServiceRequest("AddPropertyDefinition", addPropParams);
            new EntityServices().RestartDependenciesForThingShape("UserExtensions");
        }

        if (user != null) {
            try {
                Object val = user.getPropertyValue("UserRepositoryConfiguration");
                if (val == null) {
                    InfoTable empty = migrateLegacyUserRepositoryConfiguration(user);
                    user.setPropertyValue("UserRepositoryConfiguration", new InfoTablePrimitive(empty));
                }
            } catch (Exception e) {
                _logger.warn(
                        "Could not initialize GitCredentials property value for user "
                                + str_CurrentUser
                                + ": "
                                + e.getMessage());
            }

            String[] oldProps = {
                "UseGitCommitUserValues",
                "GitCommitterEmail",
                "GitCommitterPassword",
                "GitCommitterName"
            };
            for (String prop : oldProps) {
                try {
                    Object val = user.getPropertyValue(prop);
                    if (val != null) {
                        ValueCollection removeParams = new ValueCollection();
                        removeParams.put("name", new StringPrimitive(prop));
                        userExtensions.processServiceRequest(
                                "RemovePropertyDefinition", removeParams);
                        new EntityServices().RestartDependenciesForThingShape("UserExtensions");
                    }
                } catch (Exception e) {
                }
            }
        }

        boolean hasUserGpgKeys = false;
        if (user != null) {
            try {
                hasUserGpgKeys = user.getPropertyValue(Const.str_UserGpgKeys) != null;
            } catch (Exception e) {
            }
        }
        if (!hasUserGpgKeys) {
            ValueCollection addPropParams = new ValueCollection();
            addPropParams.put("defaultValue", new StringPrimitive(""));
            addPropParams.put(
                    "description", new StringPrimitive("User-owned GPG keys reusable across repositories."));
            addPropParams.put("readOnly", new BooleanPrimitive(false));
            addPropParams.put("type", new StringPrimitive("INFOTABLE"));
            addPropParams.put("name", new StringPrimitive(Const.str_UserGpgKeys));
            addPropParams.put("persistent", new BooleanPrimitive(true));
            addPropParams.put("dataShape", new StringPrimitive(Const.str_UserGpgKeyDataShapeName));
            userExtensions.processServiceRequest("AddPropertyDefinition", addPropParams);
            new EntityServices().RestartDependenciesForThingShape("UserExtensions");
        }
        if (user != null && user.getPropertyValue(Const.str_UserGpgKeys) == null) {
            user.setPropertyValue(
                    Const.str_UserGpgKeys,
                    new InfoTablePrimitive(migrateLegacyUserGpgKeys(user)));
        }
    }

    private InfoTable migrateLegacyUserGpgKeys(User user) throws Exception {
        InfoTable keys =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.str_UserGpgKeyDataShapeName);
        for (String propertyName : new String[] {"GpgKeys", "UserRepositoryConfiguration"}) {
            Object value = null;
            try {
                value = user.getPropertyValue(propertyName);
            } catch (Exception e) {
            }
            if (!(value instanceof InfoTablePrimitive)) continue;
            InfoTable source = ((InfoTablePrimitive) value).getValue();
            if (source == null) continue;
            for (int i = 0; i < source.getRowCount(); i++) {
                ValueCollection row = source.getRow(i);
                String fingerprint = primitiveString(row, Const.str_GpgKeyFingerprint);
                if (isBlank(fingerprint)) continue;
                boolean alreadyStored = false;
                for (int j = 0; j < keys.getRowCount(); j++) {
                    if (fingerprint.equals(
                            primitiveString(
                                    keys.getRow(j), Const.str_GpgKeyFingerprint))) {
                        alreadyStored = true;
                        break;
                    }
                }
                if (alreadyStored) continue;
                ValueCollection key = new ValueCollection();
                key.put(Const.str_GpgKeyFingerprint, new StringPrimitive(fingerprint));
                if (row.getPrimitive(Const.str_GpgPrivateKey) != null)
                    key.put(Const.str_GpgPrivateKey, row.getPrimitive(Const.str_GpgPrivateKey));
                if (row.getPrimitive(Const.str_GpgKeyPassphrase) != null)
                    key.put(Const.str_GpgKeyPassphrase, row.getPrimitive(Const.str_GpgKeyPassphrase));
                keys.addRow(key);
            }
        }
        return keys;
    }

    private InfoTable migrateLegacyUserRepositoryConfiguration(User user) throws Exception {
        InfoTable merged =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.str_GitCredentialsDataShapeName);
        Object credentials = null;
        Object gpgKeys = null;
        try {
            credentials = user.getPropertyValue("GitCredentials");
        } catch (Exception e) {
        }
        try {
            gpgKeys = user.getPropertyValue("GpgKeys");
        } catch (Exception e) {
        }
        if (credentials instanceof InfoTablePrimitive) {
            InfoTable table = ((InfoTablePrimitive) credentials).getValue();
            if (table != null) {
                for (int i = 0; i < table.getRowCount(); i++) merged.addRow(table.getRow(i));
            }
        }
        if (gpgKeys instanceof InfoTablePrimitive) {
            InfoTable table = ((InfoTablePrimitive) gpgKeys).getValue();
            if (table != null) {
                InfoTable keyStore =
                        InfoTableInstanceFactory.createInfoTableFromDataShape(
                                Const.str_UserGpgKeyDataShapeName);
                for (int i = 0; i < table.getRowCount(); i++) {
                    ValueCollection source = table.getRow(i);
                    String gitThing = primitiveString(source, "GitThing");
                    String fingerprint = primitiveString(source, Const.str_GpgKeyFingerprint);
                    if (!isBlank(fingerprint)) {
                        ValueCollection key = new ValueCollection();
                        key.put(Const.str_GpgKeyFingerprint, new StringPrimitive(fingerprint));
                        if (source.getPrimitive(Const.str_GpgPrivateKey) != null)
                            key.put(Const.str_GpgPrivateKey, source.getPrimitive(Const.str_GpgPrivateKey));
                        if (source.getPrimitive(Const.str_GpgKeyPassphrase) != null)
                            key.put(Const.str_GpgKeyPassphrase, source.getPrimitive(Const.str_GpgKeyPassphrase));
                        keyStore.addRow(key);
                    }
                    ValueCollection target = null;
                    for (int j = 0; j < merged.getRowCount(); j++) {
                        if (gitThing.equals(primitiveString(merged.getRow(j), "GitThing"))) {
                            target = merged.getRow(j);
                            break;
                        }
                    }
                    if (target == null) {
                        target = new ValueCollection();
                        target.put("GitThing", source.getPrimitive("GitThing"));
                        merged.addRow(target);
                    }
                    if (source.getPrimitive(Const.str_SignCommits) != null)
                        target.put(Const.str_SignCommits, source.getPrimitive(Const.str_SignCommits));
                    if (!isBlank(fingerprint))
                        target.put(Const.str_GpgKeyFingerprint, new StringPrimitive(fingerprint));
                }
                user.setPropertyValue(Const.str_UserGpgKeys, new InfoTablePrimitive(keyStore));
            }
        }
        return merged;
    }

    @ThingworxServiceDefinition(
            name = "InitUserExtensionGpgKeysProperty",
            description = "Initializes the GpgKeys UserExtension INFOTABLE property for all users",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void InitUserExtensionGpgKeysProperty() throws Exception {
        InitUserExtensionProperties();
    }

    private InfoTable getGpgKeysTable(User user) throws Exception {
        Object propVal = null;
        try {
            propVal = user.getPropertyValue(Const.str_UserGpgKeys);
        } catch (Exception e) {
        }
        if (propVal == null) return null;
        return ((InfoTablePrimitive) propVal).getValue();
    }

    @ThingworxServiceDefinition(
            name = "GetGpgKeys",
            description =
                    "Returns all GPG keys configured for the current user across all repositories",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.GpgKey.UserExtension.DataShape"})
    public InfoTable GetGpgKeys() throws Exception {
        User currentUser = requireCurrentUser();
        InfoTable gpgKeys = getGpgKeysTable(currentUser);
        if (gpgKeys != null) return gpgKeys;
        return InfoTableInstanceFactory.createInfoTableFromDataShape(Const.str_UserGpgKeyDataShapeName);
    }

    @ThingworxServiceDefinition(
            name = "SetGpgKey",
            description =
                    "Saves or updates a GPG key for the current user for a specific repository",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void SetGpgKey(
            @ThingworxServiceParameter(
                            name = "GitThing",
                            description = "Git Thing name",
                            baseType = "THINGNAME")
                    String GitThing,
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
                            name = "SignCommits",
                            description = "Whether to sign commits",
                            baseType = "BOOLEAN")
                    Boolean SignCommits,
            @ThingworxServiceParameter(
                            name = "GpgKeyFingerprint",
                            description = "GPG key fingerprint for display",
                            baseType = "STRING")
                    String GpgKeyFingerprint)
            throws Exception {
        User currentUser = requireCurrentUser();
        if (GitThing == null || GitThing.trim().isEmpty()) {
            throw new IllegalArgumentException("GitThing is required when storing a GPG key.");
        }
        if (isBlank(GpgKeyFingerprint)) {
            if (isBlank(GpgPrivateKey))
                throw new IllegalArgumentException(
                        "GpgPrivateKey or GpgKeyFingerprint is required.");
            PastedKeyGpgSigner signer =
                    new PastedKeyGpgSigner(GpgPrivateKey, GpgKeyPassphrase);
            try {
                GpgKeyFingerprint = signer.getFingerprint();
            } finally {
                signer.clearSensitiveData();
            }
            if (isBlank(GpgKeyFingerprint))
                throw new IllegalArgumentException("Could not derive the GPG key fingerprint.");
        }
        InitUserExtensionProperties();
        InfoTable gpgKeys = getGpgKeysTable(currentUser);
        if (gpgKeys == null)
            gpgKeys = InfoTableInstanceFactory.createInfoTableFromDataShape(Const.str_UserGpgKeyDataShapeName);
        else {
            InfoTable cloned =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(Const.str_UserGpgKeyDataShapeName);
            for (int i = 0; i < gpgKeys.getRowCount(); i++) {
                ValueCollection row = gpgKeys.getRow(i);
                if (!GpgKeyFingerprint.equals(primitiveString(row, Const.str_GpgKeyFingerprint)))
                    cloned.addRow(row);
            }
            gpgKeys = cloned;
        }
        ValueCollection key = new ValueCollection();
        key.put(Const.str_GpgKeyFingerprint, new StringPrimitive(GpgKeyFingerprint));
        key.put(Const.str_GpgPrivateKey, new PasswordPrimitive(GpgPrivateKey));
        key.put(Const.str_GpgKeyPassphrase, new PasswordPrimitive(GpgKeyPassphrase));
        gpgKeys.addRow(key);
        currentUser.setPropertyValue(Const.str_UserGpgKeys, new InfoTablePrimitive(gpgKeys));

        InfoTable configurations = getGitCredentials(currentUser);
        if (configurations == null)
            configurations = InfoTableInstanceFactory.createInfoTableFromDataShape(Const.str_GitCredentialsDataShapeName);
        ValueCollection configuration = null;
        for (int i = 0; i < configurations.getRowCount(); i++) {
            if (GitThing.equals(primitiveString(configurations.getRow(i), "GitThing"))) {
                configuration = configurations.getRow(i);
                break;
            }
        }
        if (configuration == null) {
            configuration = new ValueCollection();
            configuration.put("GitThing", new StringPrimitive(GitThing));
            configurations.addRow(configuration);
        }
        configuration.put(Const.str_SignCommits, new BooleanPrimitive(SignCommits));
        configuration.put(Const.str_GpgKeyFingerprint, new StringPrimitive(GpgKeyFingerprint));
        currentUser.setPropertyValue(Const.str_GitCredentials, new InfoTablePrimitive(configurations));
    }

    @ThingworxServiceDefinition(
            name = "DeleteGpgKey",
            description =
                    "Removes a GPG key configuration for the current user for a specific repository",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void DeleteGpgKey(
            @ThingworxServiceParameter(
                            name = "GitThing",
                            description = "Git Thing name",
                            baseType = "THINGNAME")
                    String GitThing)
            throws Exception {
        User currentUser = requireCurrentUser();
        InfoTable configurations = getGitCredentials(currentUser);
        if (configurations != null) {
            InfoTable cloned =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(Const.str_GitCredentialsDataShapeName);
            for (int i = 0; i < configurations.getRowCount(); i++) {
                ValueCollection row = configurations.getRow(i);
                if (GitThing.equals(primitiveString(row, "GitThing"))) {
                    row.put(Const.str_SignCommits, new BooleanPrimitive(false));
                    row.put(Const.str_GpgKeyFingerprint, new StringPrimitive(""));
                }
                cloned.addRow(row);
            }
            currentUser.setPropertyValue(Const.str_GitCredentials, new InfoTablePrimitive(cloned));
        }
    }

    @ThingworxServiceDefinition(
            name = "Pause",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void Pause(
            @ThingworxServiceParameter(
                            name = "delay",
                            description = "delay in seconds",
                            baseType = "NUMBER")
                    Double delay)
            throws Exception {
        if (delay != null) {
            Thread.sleep((long) (delay * 1000));
        }
    }

    @ThingworxServiceDefinition(
            name = "RemoveEntitiesFromExportList",
            description = "Removes entities from the export entity list",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:SpotlightSearch"})
    public InfoTable RemoveEntitiesFromExportList(
            @ThingworxServiceParameter(
                            name = "entitiesToRemove",
                            description = "",
                            baseType = "INFOTABLE",
                            aspects = {"dataShape:SpotlightSearch"})
                    InfoTable entitiesToRemove,
            @ThingworxServiceParameter(
                            name = "existingEntities",
                            description = "",
                            baseType = "INFOTABLE",
                            aspects = {"dataShape:SpotlightSearch"})
                    InfoTable existingEntities)
            throws Exception {
        if (existingEntities != null && existingEntities.getRowCount() > 0) {
            InfoTable result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
            for (int x = 0; x < existingEntities.getRowCount(); x++) {
                ValueCollection row = existingEntities.getRow(x);
                String name = row.getPrimitive("name").getValue().toString();
                boolean shouldRemove = false;
                for (int y = 0; y < entitiesToRemove.getRowCount(); y++) {
                    String removeName =
                            entitiesToRemove.getRow(y).getPrimitive("name").getValue().toString();
                    if (name.equals(removeName)) {
                        shouldRemove = true;
                        break;
                    }
                }
                if (!shouldRemove) {
                    result.addRow(row);
                }
            }
            return result;
        }
        return existingEntities != null
                ? existingEntities
                : InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
    }

    @ThingworxServiceDefinition(
            name = "UpdateRepo",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void UpdateRepo(
            @ThingworxServiceParameter(name = "RepoName", description = "", baseType = "STRING")
                    String RepoName,
            @ThingworxServiceParameter(name = "GitRepoURL", description = "", baseType = "STRING")
                    String GitRepoURL,
            @ThingworxServiceParameter(name = "RepoPath", description = "", baseType = "STRING")
                    String RepoPath,
            @ThingworxServiceParameter(name = "User", description = "", baseType = "STRING")
                    String User,
            @ThingworxServiceParameter(name = "Password", description = "", baseType = "STRING")
                    String Password,
            @ThingworxServiceParameter(name = "CommitUser", description = "", baseType = "STRING")
                    String CommitUser,
            @ThingworxServiceParameter(name = "CommitEmail", description = "", baseType = "STRING")
                    String CommitEmail,
            @ThingworxServiceParameter(
                            name = "InitialBranch",
                            description = "",
                            baseType = "STRING")
                    String InitialBranch,
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
                            name = "ProjectName",
                            description = "ThingWorx project to sync entities from (optional)",
                            baseType = "STRING")
                    String ProjectName)
            throws Exception {
        if (RepoName == null || GitRepoURL == null) {
            _logger.error(
                    "Could not update GitThing. Either RepoName or GitRepoURL did not contain data.");
            return;
        }
        Thing repoThing =
                (Thing) EntityUtilities.findEntity(RepoName, ThingworxRelationshipTypes.Thing);
        ValueCollection getConfigParams = new ValueCollection();
        getConfigParams.put("tableName", new StringPrimitive("Configuration"));
        InfoTable cfgTable =
                (InfoTable)
                        repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
        ValueCollection row = cfgTable.getRow(0);
        if (hasText(InitialBranch)) row.put("BranchName", new StringPrimitive(InitialBranch));
        if (UseProxy != null) row.put("UseProxy", new BooleanPrimitive(UseProxy));
        if (ProxyURL != null) row.put("ProxyURL", new StringPrimitive(ProxyURL));
        if (ProxyPort != null) row.put("ProxyPort", integerPrimitive(ProxyPort));
        if (LocalizationTokensPrefix != null)
            row.put("LocalizationTokensPrefix", new StringPrimitive(LocalizationTokensPrefix));
        if (ProjectName != null) row.put("ProjectName", new StringPrimitive(ProjectName));

        ValueCollection setConfigParams = new ValueCollection();
        setConfigParams.put("configurationTable", new InfoTablePrimitive(cfgTable));
        setConfigParams.put("persistent", new BooleanPrimitive(false));
        setConfigParams.put("tableName", new StringPrimitive("Configuration"));
        repoThing.processServiceRequest("SetConfigurationTable", setConfigParams);
        repoThing.processServiceRequest("SaveConfigurationTables", new ValueCollection());
        repoThing.processServiceRequest("RestartThing", new ValueCollection());

        try {
            SetGitCredentials(User, Password, CommitEmail, CommitUser, RepoName);
        } catch (Exception e) {
            _logger.error(
                    "Git Thing configuration was updated but saving credentials failed: "
                            + e.getMessage());
        }
        _logger.warn("Git Thing configuration was updated successfully.");
    }

    @ThingworxServiceDefinition(
            name = "ValidateGitThingName",
            description = "validates the Git Thing name",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String ValidateGitThingName(
            @ThingworxServiceParameter(name = "GitThingName", description = "", baseType = "STRING")
                    String GitThingName)
            throws Exception {
        Thing searchFunctions =
                (Thing)
                        EntityUtilities.findEntity(
                                "SearchFunctions", ThingworxRelationshipTypes.Resource);
        ValueCollection searchParams = new ValueCollection();
        searchParams.put("maxItems", new StringPrimitive("10000000"));
        searchParams.put("searchExpression", new StringPrimitive("*"));
        searchParams.put("types", new StringPrimitive("{\"items\":[\"Thing\"]}"));
        searchParams.put("withPermissions", new BooleanPrimitive(true));
        searchParams.put("aspects", new StringPrimitive("{\"isSystemObject\":false}"));
        searchParams.put(
                "thingTemplates",
                new StringPrimitive(
                        "{\"excludedItems\":[\"Timer\",\"Scheduler\",\"GenericConnector\",\"IndustrialGateway\"]}"));
        searchParams.put(
                "thingShapes",
                new StringPrimitive(
                        "{\"excludedItems\":[\"Blog\",\"DataTable\",\"Stream\",\"ValueStream\",\"Wiki\"]}"));
        searchParams.put("sortBy", new StringPrimitive("lastModifiedDate"));
        searchParams.put("isAscending", new BooleanPrimitive(false));
        searchParams.put("projectName", new StringPrimitive(""));
        searchParams.put("maxSearchItems", new StringPrimitive("100000"));
        searchParams.put("startDate", new StringPrimitive(""));
        searchParams.put("endDate", new StringPrimitive(""));
        searchParams.put("excludedAspects", new StringPrimitive(""));
        searchParams.put("tags", new StringPrimitive(""));
        searchParams.put("searchDescriptions", new StringPrimitive(""));

        InfoTable results =
                (InfoTable) searchFunctions.processServiceRequest("SpotlightSearch", searchParams);
        boolean found = false;
        for (int i = 0; i < results.getRowCount(); i++) {
            String name = results.getRow(i).getPrimitive("name").getValue().toString();
            if (name.equalsIgnoreCase(GitThingName)) {
                found = true;
                break;
            }
        }
        if (found)
            return "Thing "
                    + GitThingName
                    + " already exists in the platform. Please provide another Thing name.";
        return "Success. The provided thing name is valid.";
    }

    @ThingworxServiceDefinition(
            name = "SetGitCredentials",
            description =
                    "Stores or updates Git credentials for a GIT Repository thing in the current user's GitCredentials property.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void SetGitCredentials(
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
                    String GitThing)
            throws Exception {
        InitUserExtensionProperties();
        User currentUser = UserUtilities.findUser(GetCurrentUser());
        if (currentUser == null) return;

        InfoTable creds = getGitCredentials(currentUser);
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
                if (!row.getPrimitive("GitThing").getValue().toString().equals(GitThing)) {
                    cloned.addRow(row);
                }
            }
            creds = cloned;
        }
        ValueCollection entry = new ValueCollection();
        entry.put("GitCommitterUser", new StringPrimitive(GitCommitterUser));
        entry.put("GitCommitterPassword", new PasswordPrimitive(GitCommitterPassword));
        entry.put("GitCommitterEmail", new StringPrimitive(GitCommitterEmail));
        entry.put("GitCommitterFullName", new StringPrimitive(GitCommitterFullName));
        entry.put("GitThing", new StringPrimitive(GitThing));
        creds.addRow(entry);
        try {
            currentUser.setPropertyValue("UserRepositoryConfiguration", new InfoTablePrimitive(creds));
        } catch (Exception e) {
            _logger.error(
                    "Failed to save GitCredentials for user "
                            + GetCurrentUser()
                            + ": "
                            + e.getMessage());
            throw e;
        }
    }

    // ---- Services now defined directly on GIT.Utility.Thing ----

    @ThingworxServiceDefinition(
            name = "ExportLocalizationToken",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void ExportLocalizationToken(
            @ThingworxServiceParameter(name = "prefix", description = "", baseType = "STRING")
                    String prefix)
            throws Exception {
        _logger.warn(
                "ExportLocalizationToken not yet implemented in Java. Falling back to script if available.");
    }

    @ThingworxServiceDefinition(
            name = "ExportProjectData",
            description =
                    "functionality that allows exporting data from the Project DataTables/Streams/ValueStreams",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void ExportProjectData(
            @ThingworxServiceParameter(name = "ProjectName", description = "", baseType = "STRING")
                    String ProjectName)
            throws Exception {
        _logger.warn(
                "ExportProjectData not yet implemented in Java. Falling back to script if available.");
    }

    @ThingworxServiceDefinition(
            name = "ExportProjectEntities",
            description =
                    "Wrapper for the ExportToSourceControl service.\nBE WARNED: this service will clean ALL your project files with the lastModifiedDate and PersistanceProvider. This will happen regardless of what files you actually changed.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void ExportProjectEntities(
            @ThingworxServiceParameter(name = "ProjectName", description = "", baseType = "STRING")
                    String ProjectName,
            @ThingworxServiceParameter(
                            name = "includeDependents",
                            description = "",
                            baseType = "BOOLEAN")
                    Boolean includeDependents,
            @ThingworxServiceParameter(
                            name = "EntitiesToExport",
                            description =
                                    "Optional; if not set all project entities will be exported",
                            baseType = "INFOTABLE",
                            aspects = {"dataShape:SpotlightSearch"})
                    InfoTable EntitiesToExport,
            @ThingworxServiceParameter(
                            name = "commitMessage",
                            description =
                                    "Optional commit message. If provided, a git commit and push will be performed after export.",
                            baseType = "STRING")
                    String commitMessage)
            throws Exception {
        _logger.warn("Starting ExportProjectEntities for project: " + ProjectName);
        if (isBlank(ProjectName)) {
            throw new Exception(Const.ERR_PREFIX_CONFIG + Const.ERR_PROJECT_NAME_REQUIRED);
        }

        // 1. Read this GitThing's configuration to get FileRepository and repo path
        Thing repoThing = resolveCallingThing();
        ValueCollection getConfigParams = new ValueCollection();
        getConfigParams.put("tableName", new StringPrimitive("Configuration"));
        InfoTable cfgTable =
                (InfoTable)
                        repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
        String str_FileRepoName = primitiveString(cfgTable.getRow(0), "FileRepository");
        String str_RepoPath = primitiveString(cfgTable.getRow(0), "RepoPathName");
        _logger.warn(
                "ExportProjectEntities: FileRepository="
                        + str_FileRepoName
                        + ", RepoPath="
                        + str_RepoPath);

        // 2. Call SourceControlFunctions.ExportSourceControlledEntities
        Object scfObj =
                EntityUtilities.findEntity(
                        "SourceControlFunctions", ThingworxRelationshipTypes.Resource);
        if (scfObj == null) {
            throw new Exception(Const.ERR_PREFIX_SYSTEM + Const.ERR_NO_SCF_RESOURCE);
        }
        IServiceProvider scf = (IServiceProvider) scfObj;

        boolean bool_IncludeDeps = isTrue(includeDependents);

        if (EntitiesToExport == null || EntitiesToExport.getRowCount() == 0) {
            // Full project export - export directly to the repo path
            _logger.warn("Exporting all entities from project: " + ProjectName);
            ValueCollection params = new ValueCollection();
            params.put("repositoryName", new StringPrimitive(str_FileRepoName));
            params.put("path", new StringPrimitive(str_RepoPath));
            params.put("projectName", new StringPrimitive(ProjectName));
            params.put("includeDependents", new BooleanPrimitive(bool_IncludeDeps));
            scf.processServiceRequest("ExportSourceControlledEntities", params);
        } else {
            // Selective export: export all to a temp folder, then move only the requested files
            String str_TempPath = str_RepoPath + "/_temp_export_" + System.currentTimeMillis();
            _logger.warn(
                    "Selective export: exporting "
                            + EntitiesToExport.getRowCount()
                            + " entities to temp path: "
                            + str_TempPath);
            ValueCollection params = new ValueCollection();
            params.put("repositoryName", new StringPrimitive(str_FileRepoName));
            params.put("path", new StringPrimitive(str_TempPath));
            params.put("projectName", new StringPrimitive(ProjectName));
            params.put("includeDependents", new BooleanPrimitive(bool_IncludeDeps));
            scf.processServiceRequest("ExportSourceControlledEntities", params);

            // Move each selected entity's XML from temp to actual repo path
            Thing fileRepoThing =
                    (Thing)
                            EntityUtilities.findEntity(
                                    str_FileRepoName, ThingworxRelationshipTypes.Thing);
            for (int i = 0; i < EntitiesToExport.getRowCount(); i++) {
                String str_EntityName = primitiveString(EntitiesToExport.getRow(i), "name");
                String str_EntityType =
                        orDefault(primitiveString(EntitiesToExport.getRow(i), "type"), "");
                String str_TypeFolder = mapEntityTypeToCollectionFolder(str_EntityType);
                String str_Source =
                        str_TempPath
                                + "/"
                                + ProjectName
                                + "/"
                                + str_TypeFolder
                                + "/"
                                + str_EntityName
                                + ".xml";
                String str_Target =
                        str_RepoPath
                                + "/"
                                + ProjectName
                                + "/"
                                + str_TypeFolder
                                + "/"
                                + str_EntityName
                                + ".xml";

                ValueCollection moveParams = new ValueCollection();
                moveParams.put("sourcePath", new StringPrimitive(str_Source));
                moveParams.put("targetPath", new StringPrimitive(str_Target));
                moveParams.put("overwrite", new BooleanPrimitive(true));
                try {
                    fileRepoThing.processServiceRequest("MoveFile", moveParams);
                    _logger.warn("Moved entity file: " + str_EntityName);
                } catch (Exception e) {
                    _logger.warn(
                            "Could not move entity file " + str_Source + ": " + e.getMessage());
                }
            }

            // Delete temp folder
            ValueCollection deleteParams = new ValueCollection();
            deleteParams.put("path", new StringPrimitive(str_TempPath));
            try {
                fileRepoThing.processServiceRequest("DeleteFolder", deleteParams);
            } catch (Exception e) {
                _logger.warn("Could not delete temp folder: " + str_TempPath);
            }
        }

        // 3. Clean up lastModifiedDate from exported XML files
        try {
            removeLastModifiedDate(str_FileRepoName, str_RepoPath, ProjectName);
        } catch (Exception e) {
            _logger.warn("RemoveLastModifiedDate failed: " + e.getMessage());
        }

        // 4. Clean up modelPersistenceProviderPackage from exported XML files
        try {
            removeModelPersistenceProviderPackage(str_FileRepoName, str_RepoPath, ProjectName);
        } catch (Exception e) {
            _logger.warn("RemoveModelPersistenceProviderPackage failed: " + e.getMessage());
        }

        // 5. Stage all files in the git repo so they show as "Added" not "Untracked"
        try {
            FileRepositoryThing fileRepo =
                    (FileRepositoryThing)
                            EntityUtilities.findEntity(
                                    str_FileRepoName, ThingworxRelationshipTypes.Thing);
            String repoFullPath = new File(fileRepo.getRootPath(), str_RepoPath).getPath();
            try (Git git = Git.open(new File(repoFullPath))) {
                git.add().addFilepattern(".").call();
                _logger.warn("Staged all files in git repo: " + repoFullPath);
            }
        } catch (Exception e) {
            _logger.warn("Git add failed: " + e.getMessage());
        }

        // 6. If commitMessage is provided, auto-commit and push
        if (hasText(commitMessage)) {
            try {
                ValueCollection pushParams = new ValueCollection();
                pushParams.put("Message", new StringPrimitive(commitMessage));
                repoThing.processServiceRequest("Push", pushParams);
                _logger.warn("Auto-commit and push completed after export.");
            } catch (Exception e) {
                _logger.error("Auto-push after export failed: " + e.getMessage());
            }
        }

        _logger.warn("ExportProjectEntities completed for project: " + ProjectName);
    }

    @ThingworxServiceDefinition(
            name = "SyncProjectToRepository",
            description =
                    "Exports entities from the configured project to the FileRepository. Syncs the Git working tree to reflect live ThingWorx state.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void SyncProjectToRepository(
            @ThingworxServiceParameter(
                            name = "GitThingName",
                            description = "GIT Repository Thing name to sync",
                            baseType = "STRING")
                    String GitThingName)
            throws Exception {
        if (isBlank(GitThingName)) return;

        Thing repoThing =
                (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
        ValueCollection getConfigParams = new ValueCollection();
        getConfigParams.put("tableName", new StringPrimitive("Configuration"));
        InfoTable cfgTable =
                (InfoTable)
                        repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
        String str_FileRepoName = primitiveString(cfgTable.getRow(0), "FileRepository");
        String str_RepoPath = primitiveString(cfgTable.getRow(0), "RepoPathName");
        String str_ProjectName = primitiveString(cfgTable.getRow(0), "ProjectName");

        if (isBlank(str_ProjectName)) {
            _logger.warn(GitThingName + ": " + Const.WARN_NO_PROJECT_SKIP);
            return;
        }

        String str_TempPath = str_RepoPath + "/_sync_temp_" + System.currentTimeMillis();
        _logger.warn(
                "SyncProjectToRepository: Exporting "
                        + str_ProjectName
                        + " to temp path: "
                        + str_TempPath);

        Object scfObj =
                EntityUtilities.findEntity(
                        "SourceControlFunctions", ThingworxRelationshipTypes.Resource);
        if (scfObj == null) {
            throw new Exception(Const.ERR_PREFIX_SYSTEM + Const.ERR_NO_SCF_RESOURCE);
        }
        IServiceProvider scf = (IServiceProvider) scfObj;
        ValueCollection exportParams = new ValueCollection();
        exportParams.put("repositoryName", new StringPrimitive(str_FileRepoName));
        exportParams.put("path", new StringPrimitive(str_TempPath));
        exportParams.put("projectName", new StringPrimitive(str_ProjectName));
        exportParams.put("includeDependents", new BooleanPrimitive(false));
        scf.processServiceRequest("ExportSourceControlledEntities", exportParams);

        removeLastModifiedDate(str_FileRepoName, str_TempPath, str_ProjectName);
        removeModelPersistenceProviderPackage(str_FileRepoName, str_TempPath, str_ProjectName);

        FileRepositoryThing fileRepo =
                (FileRepositoryThing)
                        EntityUtilities.findEntity(
                                str_FileRepoName, ThingworxRelationshipTypes.Thing);
        String str_RepoFullPath = new File(fileRepo.getRootPath(), str_RepoPath).getPath();
        String str_TempFullPath = new File(fileRepo.getRootPath(), str_TempPath).getPath();
        String str_ProjectFullPath = new File(str_RepoFullPath, str_ProjectName).getPath();
        String str_TempProjectFullPath = new File(str_TempFullPath, str_ProjectName).getPath();

        List<File> repoFiles = new ArrayList<>();
        File repoProjectDir = new File(str_ProjectFullPath);
        if (repoProjectDir.exists()) {
            collectXmlFilesOnDisk(repoProjectDir, repoFiles);
        }

        List<File> tempFiles = new ArrayList<>();
        File tempProjectDir = new File(str_TempProjectFullPath);
        if (tempProjectDir.exists()) {
            collectXmlFilesOnDisk(tempProjectDir, tempFiles);
        }

        java.util.Set<String> tempRelativePaths = new java.util.HashSet<>();
        for (File f : tempFiles) {
            String rel =
                    str_TempProjectFullPath.length() + 1 < f.getAbsolutePath().length()
                            ? f.getAbsolutePath().substring(str_TempProjectFullPath.length() + 1)
                            : f.getName();
            tempRelativePaths.add(rel);
        }

        for (File f : tempFiles) {
            String rel =
                    str_TempProjectFullPath.length() + 1 < f.getAbsolutePath().length()
                            ? f.getAbsolutePath().substring(str_TempProjectFullPath.length() + 1)
                            : f.getName();
            File targetFile = new File(str_ProjectFullPath, rel);
            targetFile.getParentFile().mkdirs();
            Files.copy(
                    f.toPath(),
                    targetFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        for (File f : repoFiles) {
            String rel =
                    str_ProjectFullPath.length() + 1 < f.getAbsolutePath().length()
                            ? f.getAbsolutePath().substring(str_ProjectFullPath.length() + 1)
                            : f.getName();
            if (!tempRelativePaths.contains(rel)) {
                try {
                    Files.delete(f.toPath());
                    _logger.warn("SyncProjectToRepository: Removed deleted entity file: " + rel);
                } catch (Exception e) {
                    _logger.warn(
                            "SyncProjectToRepository: Could not delete "
                                    + f.getAbsolutePath()
                                    + ": "
                                    + e.getMessage());
                }
            }
        }

        if (repoProjectDir.exists()) {
            try {
                Files.walk(repoProjectDir.toPath())
                        .sorted((p1, p2) -> -p1.compareTo(p2))
                        .filter(p -> p.toFile().isDirectory())
                        .forEach(
                                p -> {
                                    try {
                                        if (p.toFile().listFiles() == null
                                                || p.toFile().listFiles().length == 0) {
                                            Files.delete(p);
                                        }
                                    } catch (IOException e) {
                                    }
                                });
            } catch (Exception e) {
                _logger.warn(
                        "SyncProjectToRepository: Could not clean empty dirs: " + e.getMessage());
            }
        }

        try {
            ValueCollection deleteParams = new ValueCollection();
            deleteParams.put("path", new StringPrimitive(str_TempPath));
            fileRepo.processServiceRequest("DeleteFolder", deleteParams);
        } catch (Exception e) {
            _logger.warn("SyncProjectToRepository: Could not delete temp folder: " + str_TempPath);
        }

        _logger.warn(
                "SyncProjectToRepository completed for "
                        + GitThingName
                        + ": "
                        + tempFiles.size()
                        + " files synced, "
                        + repoFiles.size()
                        + " existing files reconciled.");
    }

    @ThingworxServiceDefinition(
            name = "SetProjectName",
            description =
                    "Updates the ProjectName field on a GIT Repository Thing's Configuration table.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void SetProjectName(
            @ThingworxServiceParameter(
                            name = "GitThingName",
                            description = "GIT Repository Thing name",
                            baseType = "STRING")
                    String GitThingName,
            @ThingworxServiceParameter(
                            name = "ProjectName",
                            description = "ThingWorx project name",
                            baseType = "STRING")
                    String ProjectName)
            throws Exception {
        if (isBlank(GitThingName)) return;
        Thing repoThing =
                (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
        ValueCollection getConfigParams = new ValueCollection();
        getConfigParams.put("tableName", new StringPrimitive("Configuration"));
        InfoTable cfgTable =
                (InfoTable)
                        repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
        ValueCollection row = cfgTable.getRow(0);
        row.put("ProjectName", new StringPrimitive(orDefault(ProjectName, "")));
        ValueCollection setConfigParams = new ValueCollection();
        setConfigParams.put("configurationTable", new InfoTablePrimitive(cfgTable));
        setConfigParams.put("persistent", new BooleanPrimitive(false));
        setConfigParams.put("tableName", new StringPrimitive("Configuration"));
        repoThing.processServiceRequest("SetConfigurationTable", setConfigParams);
        repoThing.processServiceRequest("SaveConfigurationTables", new ValueCollection());
        repoThing.processServiceRequest("RestartThing", new ValueCollection());
        _logger.warn(
                "SetProjectName: Updated project name to '"
                        + ProjectName
                        + "' for "
                        + GitThingName);
    }

    @ThingworxServiceDefinition(
            name = "ExportProjectExtensions",
            description = "wrapper for Extensions Export / ExportExtensionsToRepository",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void ExportProjectExtensions() throws Exception {
        _logger.warn(
                "ExportProjectExtensions not yet implemented in Java. Falling back to script if available.");
    }

    @ThingworxServiceDefinition(
            name = "GetFilteredDirectoryListing",
            description =
                    "Gets recursively the directories found in a subfolder in a FileRepository. Not part of the services of a FileRepository",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:FileSystemDirectory"})
    public InfoTable GetFilteredDirectoryListing() throws Exception {
        Thing repoThing = resolveCallingThing();
        ValueCollection getConfigParams = new ValueCollection();
        getConfigParams.put("tableName", new StringPrimitive("Configuration"));
        InfoTable cfgTable =
                (InfoTable)
                        repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
        String str_FileRepoName =
                cfgTable.getRow(0).getPrimitive("FileRepository").getValue().toString();
        String str_RepoPath = cfgTable.getRow(0).getPrimitive("RepoPathName").getValue().toString();
        Thing fileRepo =
                (Thing)
                        EntityUtilities.findEntity(
                                str_FileRepoName, ThingworxRelationshipTypes.Thing);

        InfoTable raw =
                (InfoTable)
                        fileRepo.processServiceRequest(
                                "ListDirectories",
                                new ValueCollection() {
                                    {
                                        put("path", new StringPrimitive(str_RepoPath));
                                        put("nameMask", new StringPrimitive(""));
                                    }
                                });
        InfoTable result =
                InfoTableInstanceFactory.createInfoTableFromDataShape("FileSystemDirectory");
        for (int i = 0; i < raw.getRowCount(); i++) {
            String name = primitiveString(raw.getRow(i), "name");
            String p = primitiveString(raw.getRow(i), "path");
            if (isBlank(name) || isBlank(p)) continue;
            ValueCollection row = new ValueCollection();
            row.put("name", new StringPrimitive(name));
            row.put("path", new StringPrimitive(p));
            result.addRow(row);
        }
        _logger.warn(
                "GetFilteredDirectoryListing found "
                        + result.getRowCount()
                        + " directories (filtered from "
                        + raw.getRowCount()
                        + ").");
        return result;
    }

    @ThingworxServiceDefinition(
            name = "RemoveConfigurationTableDefinitions",
            description =
                    "Removes the ConfigurationTableDefinitions to allow compatibility with 8.2. This service should be used only if you're doing crossplatform development between 8.5 and 8.2/8.3. Enable or disable in the ExportProjectEntities services",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void RemoveConfigurationTableDefinitions() throws Exception {
        _logger.warn(
                "RemoveConfigurationTableDefinitions not yet implemented in Java. Falling back to script if available.");
    }

    @ThingworxServiceDefinition(
            name = "RemoveLastModifiedDate",
            description =
                    "Removes the lastModifiedDate. Change history is already removed by ExportToSourceControlEntities",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void RemoveLastModifiedDate() throws Exception {
        _logger.warn("RemoveLastModifiedDate called as service.");
        Thing repoThing = resolveCallingThing();
        ValueCollection getConfigParams = new ValueCollection();
        getConfigParams.put("tableName", new StringPrimitive("Configuration"));
        InfoTable cfgTable =
                (InfoTable)
                        repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
        String str_FileRepoName =
                cfgTable.getRow(0).getPrimitive("FileRepository").getValue().toString();
        String str_RepoPath = cfgTable.getRow(0).getPrimitive("RepoPathName").getValue().toString();
        removeLastModifiedDate(str_FileRepoName, str_RepoPath, null);
    }

    @ThingworxServiceDefinition(
            name = "RemoveMashupPreviewTag",
            description =
                    "Removes the mashup preview tag to allow compatibility with 8.2. This service should be used only if you're doing crossplatform development between 8.5 and 8.2/8.3",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void RemoveMashupPreviewTag() throws Exception {
        _logger.warn(
                "RemoveMashupPreviewTag not yet implemented in Java. Falling back to script if available.");
    }

    @ThingworxServiceDefinition(
            name = "RemoveModelPersistenceProviderPackage",
            description = "Removes the modelPersistenceProviderPackage.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void RemoveModelPersistenceProviderPackage() throws Exception {
        _logger.warn("RemoveModelPersistenceProviderPackage called as service.");
        Thing repoThing = resolveCallingThing();
        ValueCollection getConfigParams = new ValueCollection();
        getConfigParams.put("tableName", new StringPrimitive("Configuration"));
        InfoTable cfgTable =
                (InfoTable)
                        repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
        String str_FileRepoName =
                cfgTable.getRow(0).getPrimitive("FileRepository").getValue().toString();
        String str_RepoPath = cfgTable.getRow(0).getPrimitive("RepoPathName").getValue().toString();
        removeModelPersistenceProviderPackage(str_FileRepoName, str_RepoPath, null);
    }

    // ---- Helper Methods ----

    private String mapEntityTypeToCollectionFolder(String entityType) {
        if (isBlank(entityType)) return "";
        switch (entityType) {
            case "Thing":
            case "DataTable":
            case "Stream":
            case "ValueStream":
            case "Timer":
            case "Scheduler":
            case "IndustrialConnection":
            case "IntegrationConnector":
                return "Things";
            case "Mashup":
            case "Master":
            case "MashupTemplate":
            case "Gadget":
                return "Mashups";
            case "MediaEntity":
                return "MediaEntitie";
            case "UserGroup":
                return "Group";
            case "ModelTagVocabulary":
            case "ModelTag":
                return "ModelTags";
            case "ThingShape":
                return "ThingShapes";
            case "ThingTemplate":
                return "ThingTemplates";
            case "DataShape":
                return "DataShapes";
            case "StateDefinition":
                return "StateDefinitions";
            case "StyleTheme":
                return "StyleThemes";
            case "Project":
                return "Projects";
            case "Menu":
                return "Menus";
            case "Resource":
                return "Resources";
            case "Organization":
                return "Organizations";
            case "ApplicationKey":
                return "ApplicationKeys";
            case "DirectoryService":
                return "DirectoryServices";
            case "Authenticator":
                return "Authenticators";
            case "User":
                return "Users";
            case "Group":
                return "Groups";
            case "NotificationDefinition":
                return "NotificationDefinitions";
            case "NotificationContent":
                return "NotificationContent";
            default:
                return entityType + "s";
        }
    }

    private void removeLastModifiedDate(String fileRepoName, String repoPath, String projectName)
            throws IOException {
        FileRepositoryThing fileRepo =
                (FileRepositoryThing)
                        EntityUtilities.findEntity(fileRepoName, ThingworxRelationshipTypes.Thing);
        String fullPath = new File(fileRepo.getRootPath(), repoPath).getPath();
        if (hasText(projectName)) {
            fullPath = new File(fullPath, projectName).getPath();
        }
        File rootDir = new File(fullPath);
        if (!rootDir.exists()) {
            _logger.warn("removeLastModifiedDate: path does not exist: " + fullPath);
            return;
        }
        List<File> xmlFiles = new ArrayList<>();
        collectXmlFilesOnDisk(rootDir, xmlFiles);
        int count = 0;
        for (File f : xmlFiles) {
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            String modified = content.replaceAll("\\s+lastModifiedDate=\"[^\"]*\"", "");
            if (!modified.equals(content)) {
                Files.write(f.toPath(), modified.getBytes(StandardCharsets.UTF_8));
                count++;
            }
        }
        _logger.warn("removeLastModifiedDate: cleaned " + count + " files in " + fullPath);
    }

    private void removeModelPersistenceProviderPackage(
            String fileRepoName, String repoPath, String projectName) throws IOException {
        FileRepositoryThing fileRepo =
                (FileRepositoryThing)
                        EntityUtilities.findEntity(fileRepoName, ThingworxRelationshipTypes.Thing);
        String fullPath = new File(fileRepo.getRootPath(), repoPath).getPath();
        if (hasText(projectName)) {
            fullPath = new File(fullPath, projectName).getPath();
        }
        File rootDir = new File(fullPath);
        if (!rootDir.exists()) {
            _logger.warn("removeModelPersistenceProviderPackage: path does not exist: " + fullPath);
            return;
        }
        List<File> xmlFiles = new ArrayList<>();
        collectXmlFilesOnDisk(rootDir, xmlFiles);
        int count = 0;
        for (File f : xmlFiles) {
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            String modified =
                    content.replaceAll("\\s+modelPersistenceProviderPackage=\"[^\"]*\"", "");
            if (!modified.equals(content)) {
                Files.write(f.toPath(), modified.getBytes(StandardCharsets.UTF_8));
                count++;
            }
        }
        _logger.warn(
                "removeModelPersistenceProviderPackage: cleaned "
                        + count
                        + " files in "
                        + fullPath);
    }

    private void collectXmlFilesOnDisk(File directory, List<File> result) {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectXmlFilesOnDisk(f, result);
            } else if (f.getName().toLowerCase().endsWith(".xml")) {
                result.add(f);
            }
        }
    }

    private Thing resolveCallingThing() throws Exception {
        try {
            Object meObj = ThreadLocalContext.getMeContext();
            _logger.warn(
                    "resolveCallingThing: getMeContext="
                            + meObj
                            + " type="
                            + (meObj != null ? meObj.getClass().getName() : "null"));
            if (meObj instanceof Thing) {
                Thing t = (Thing) meObj;
                _logger.warn("resolveCallingThing: meContext Thing name=" + t.getName());
                return t;
            }
            if (meObj instanceof String) {
                String name = (String) meObj;
                _logger.warn("resolveCallingThing: meContext is String=" + name);
                Thing t =
                        (Thing) EntityUtilities.findEntity(name, ThingworxRelationshipTypes.Thing);
                if (t != null) return t;
            }
        } catch (Exception e) {
            _logger.warn("resolveCallingThing: getMeContext failed: " + e.getMessage());
        }
        try {
            String name = this.getName();
            _logger.warn("resolveCallingThing: this.getName()=" + name);
            if (hasText(name)) {
                Thing thing =
                        (Thing) EntityUtilities.findEntity(name, ThingworxRelationshipTypes.Thing);
                _logger.warn("resolveCallingThing: findEntity(" + name + ")=" + thing);
                if (thing != null) return thing;
            }
        } catch (Exception e) {
            _logger.warn("resolveCallingThing: this.getName() failed: " + e.getMessage());
        }
        // Try finding ancestor by checking the calling security context
        try {
            String secName = ThreadLocalContext.getSecurityContext().getName();
            _logger.warn("resolveCallingThing: security context name=" + secName);
        } catch (Exception e) {
            _logger.warn("resolveCallingThing: security context failed: " + e.getMessage());
        }
        throw new Exception(Const.ERR_PREFIX_SYSTEM + Const.ERR_COULD_NOT_RESOLVE_THING);
    }

    private InfoTable getGitCredentials(User user) throws Exception {
        Object propVal = null;
        try {
            propVal = user.getPropertyValue("UserRepositoryConfiguration");
        } catch (Exception e) {
        }
        if (propVal == null) return null;
        return ((InfoTablePrimitive) propVal).getValue();
    }

    private String GetCurrentUser() {
        return ThreadLocalContext.getSecurityContext().getName();
    }

    /** Resolve the authenticated User so UserExtension secret values remain user-scoped. */
    private User requireCurrentUser() throws Exception {
        String currentUserName = GetCurrentUser();
        if (currentUserName == null || currentUserName.trim().isEmpty()) {
            throw new IllegalStateException("No authenticated user context is available.");
        }
        User currentUser = UserUtilities.findUser(currentUserName);
        if (currentUser == null) {
            throw new IllegalStateException(
                    "Authenticated user was not found: " + currentUserName);
        }
        return currentUser;
    }

    private IntegerPrimitive integerPrimitive(int value) {
        IntegerPrimitive primitive = new IntegerPrimitive();
        primitive.setValue(value);
        return primitive;
    }

    private InfoTable unionInfoTables(InfoTable t1, InfoTable t2) throws Exception {
        InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < t1.getRowCount(); i++) {
            ValueCollection row = t1.getRow(i);
            String key = spotlightEntityKey(row);
            if (!seen.contains(key)) {
                result.addRow(row);
                seen.add(key);
            }
        }
        for (int i = 0; i < t2.getRowCount(); i++) {
            ValueCollection row = t2.getRow(i);
            String key = spotlightEntityKey(row);
            if (!seen.contains(key)) {
                result.addRow(row);
                seen.add(key);
            }
        }
        return result;
    }

    private String spotlightEntityKey(ValueCollection row) throws Exception {
        return orDefault(primitiveString(row, "type"), "")
                + '\u0000'
                + orDefault(primitiveString(row, "name"), "");
    }

    private InfoTable queryInfoTable(
            InfoTable table, String fieldName, String filterType, String value) throws Exception {
        InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
        Pattern pattern = filterType.equals("LIKE") ? wildcardPattern(value) : null;
        for (int i = 0; i < table.getRowCount(); i++) {
            ValueCollection row = table.getRow(i);
            String fieldVal = row.getPrimitive(fieldName).getValue().toString();
            if (filterType.equals("LIKE")) {
                if (pattern.matcher(fieldVal).matches()) {
                    result.addRow(row);
                }
            }
        }
        return result;
    }

    private Pattern wildcardPattern(String value) {
        String[] literalParts = value.split("\\*", -1);
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < literalParts.length; i++) {
            if (i > 0) regex.append(".*");
            regex.append(Pattern.quote(literalParts[i]));
        }
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
