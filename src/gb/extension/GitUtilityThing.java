package gb.extension;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Base64;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.entities.utils.UserUtilities;
import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.annotations.ThingworxBaseTemplateDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceParameter;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.resources.entities.EntityServices;
import com.thingworx.security.users.User;
import com.thingworx.things.Thing;
import com.thingworx.things.repository.FileRepositoryThing;
import com.thingworx.thingshape.ThingShape;
import com.thingworx.thingtemplates.ThingTemplate;
import com.thingworx.types.InfoTable;
import com.thingworx.types.TagCollection;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.BooleanPrimitive;
import com.thingworx.types.primitives.DatetimePrimitive;
import com.thingworx.types.primitives.IPrimitiveType;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.PasswordPrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import com.thingworx.webservices.context.ThreadLocalContext;

@ThingworxBaseTemplateDefinition(name = "GenericThing")
public class GitUtilityThing extends Thing {

	private static final long serialVersionUID = 9085129963750550674L;
	private static Logger _logger = LogUtilities.getInstance().getApplicationLogger(GitUtilityThing.class);

	public GitUtilityThing() {
	}

	@ThingworxServiceDefinition(name = "AddEntitiesToExportList", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:SpotlightSearch" })
	public InfoTable AddEntitiesToExportList(
			@ThingworxServiceParameter(name = "existingEntities", description = "", baseType = "INFOTABLE", aspects = { "dataShape:SpotlightSearch" }) InfoTable existingEntities,
			@ThingworxServiceParameter(name = "newEntitiesToExport", description = "", baseType = "INFOTABLE", aspects = { "dataShape:SpotlightSearch" }) InfoTable newEntitiesToExport)
			throws Exception {
		if (existingEntities == null || existingEntities.getRowCount() == 0) {
			existingEntities = InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
		}
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
		for (int x = 0; x < existingEntities.getRowCount(); x++) {
			ValueCollection existingRow = existingEntities.getRow(x);
			String existingName = existingRow.getPrimitive("name").getValue().toString();
			boolean found = false;
			for (int y = 0; y < newEntitiesToExport.getRowCount(); y++) {
				String newName = newEntitiesToExport.getRow(y).getPrimitive("name").getValue().toString();
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

	@ThingworxServiceDefinition(name = "AddLogEntry", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void AddLogEntry(
			@ThingworxServiceParameter(name = "Content", description = "", baseType = "STRING") String Content,
			@ThingworxServiceParameter(name = "ServiceName", description = "", baseType = "STRING") String ServiceName,
			@ThingworxServiceParameter(name = "Source", description = "", baseType = "STRING") String Source,
			@ThingworxServiceParameter(name = "timestamp", description = "", baseType = "DATETIME") DateTime timestamp,
			@ThingworxServiceParameter(name = "User", description = "", baseType = "STRING") String User)
			throws Exception {
		InfoTable values = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.Log");
		ValueCollection entry = new ValueCollection();
		entry.put("ID", new StringPrimitive(java.util.UUID.randomUUID().toString()));
		entry.put("timestamp", new DatetimePrimitive(timestamp));
		entry.put("User", new StringPrimitive(User));
		entry.put("ServiceName", new StringPrimitive(ServiceName));
		entry.put("Content", new StringPrimitive(Content));
		entry.put("Source", new StringPrimitive(Source));
		values.addRow(entry);
		Thing dataTable = (Thing) EntityUtilities.findEntity("GitBackup.Log.DataTable", ThingworxRelationshipTypes.Thing);
		ValueCollection params = new ValueCollection();
		params.put("sourceType", new StringPrimitive(""));
		params.put("values", new InfoTablePrimitive(values));
		params.put("location", new StringPrimitive(""));
		params.put("source", new StringPrimitive(Source));
		params.put("tags", new StringPrimitive(""));
		dataTable.processServiceRequest("AddDataTableEntry", params);
	}

	@ThingworxServiceDefinition(name = "AddNewRepo", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void AddNewRepo(
			@ThingworxServiceParameter(name = "RepoName", description = "", baseType = "STRING") String RepoName,
			@ThingworxServiceParameter(name = "GitRepoURL", description = "", baseType = "STRING") String GitRepoURL,
			@ThingworxServiceParameter(name = "RepoPath", description = "", baseType = "STRING") String RepoPath,
			@ThingworxServiceParameter(name = "FileRepo", description = "", baseType = "STRING", aspects = { "defaultValue:GitRepository" }) String FileRepo,
			@ThingworxServiceParameter(name = "User", description = "", baseType = "STRING") String User,
			@ThingworxServiceParameter(name = "Password", description = "", baseType = "STRING") String Password,
			@ThingworxServiceParameter(name = "CommitUser", description = "", baseType = "STRING") String CommitUser,
			@ThingworxServiceParameter(name = "CommitEmail", description = "", baseType = "STRING") String CommitEmail,
			@ThingworxServiceParameter(name = "InitialBranch", description = "", baseType = "STRING") String InitialBranch,
			@ThingworxServiceParameter(name = "UseProxy", description = "", baseType = "BOOLEAN", aspects = { "defaultValue:false" }) Boolean UseProxy,
			@ThingworxServiceParameter(name = "ProxyURL", description = "", baseType = "STRING", aspects = { "defaultValue:none" }) String ProxyURL,
			@ThingworxServiceParameter(name = "ProxyPort", description = "", baseType = "INTEGER", aspects = { "defaultValue:3281" }) Integer ProxyPort,
			@ThingworxServiceParameter(name = "LocalizationTokensPrefix", description = "prefix used for exporting Localization Tokens", baseType = "STRING") String LocalizationTokensPrefix)
			throws Exception {
		if (RepoName == null || GitRepoURL == null) return;
		EntityServices es = new EntityServices();
		es.CreateThing(RepoName, "GitRepository created by user " + GetCurrentUser() + " at " + new java.util.Date(),
				new TagCollection(), "GitBackupTemplate");
		Thing repoThing = (Thing) EntityUtilities.findEntity(RepoName, ThingworxRelationshipTypes.Thing);
		repoThing.processServiceRequest("EnableThing", new ValueCollection());
		repoThing.processServiceRequest("RestartThing", new ValueCollection());

		InfoTable configTable = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.ConfigurationSetting");
		ValueCollection configRow = new ValueCollection();
		configRow.put("FileRepository", new StringPrimitive(FileRepo != null ? FileRepo : "GitRepository"));
		configRow.put("GitRepoURL", new StringPrimitive(GitRepoURL));
		configRow.put("RepoPathName", new StringPrimitive(RepoPath));
		configRow.put("BranchName", new StringPrimitive(InitialBranch));
		configRow.put("UseProxy", new BooleanPrimitive(UseProxy != null && UseProxy));
		configRow.put("ProxyURL", new StringPrimitive(ProxyURL));
		configRow.put("ProxyPort", new StringPrimitive(ProxyPort != null ? ProxyPort.toString() : "0"));
		configRow.put("LocalizationTokensPrefix", new StringPrimitive(LocalizationTokensPrefix));
		configTable.addRow(configRow);

		ValueCollection setConfigParams = new ValueCollection();
		setConfigParams.put("configurationTable", new InfoTablePrimitive(configTable));
		setConfigParams.put("persistent", new BooleanPrimitive(true));
		setConfigParams.put("tableName", new StringPrimitive("Configuration"));
		repoThing.processServiceRequest("SetConfigurationTable", setConfigParams);
		repoThing.processServiceRequest("RestartThing", new ValueCollection());

		SetGitCredentials(User, Password, CommitEmail, CommitUser, RepoName);
	}

	@ThingworxServiceDefinition(name = "DeteleGitThing", description = "Deletes a GitBackup Thing involves two operations: 1. Deleting the Thing itself and 2. Deleting the FileRepository subfolder that stored that Git repository.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void DeteleGitThing(
			@ThingworxServiceParameter(name = "RepoName", description = "", baseType = "STRING") String RepoName)
			throws Exception {
		Thing repoThing = (Thing) EntityUtilities.findEntity(RepoName, ThingworxRelationshipTypes.Thing);
		ValueCollection config = new ValueCollection();
		config.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", config);
		String str_RepositoryName = cfgTable.getRow(0).getPrimitive("FileRepository").getValue().toString();
		String str_RepositoryPathName = cfgTable.getRow(0).getPrimitive("FileRepoPath").getValue().toString();

		try {
			repoThing.processServiceRequest("DeleteLocalRepoContent", new ValueCollection());
		} catch (Exception ex) {
			_logger.error("Deleting Git Repository folder content failed when deleting Thing " + RepoName);
		}
		EntityServices es = new EntityServices();
		es.DeleteThing(RepoName);
		try {
			FileRepositoryThing fileRepo = (FileRepositoryThing) EntityUtilities.findEntity(str_RepositoryName,
					ThingworxRelationshipTypes.Thing);
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
					InfoTable updatedCreds = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.GitCredentials");
					for (int ci = 0; ci < creds.getRowCount(); ci++) {
						ValueCollection credRow = creds.getRow(ci);
						if (!RepoName.equals(credRow.getPrimitive("GitThing").getValue().toString())) {
							updatedCreds.addRow(credRow);
						}
					}
					creds = updatedCreds;
					user.setPropertyValue("GitCredentials", new InfoTablePrimitive(creds));
				}
			}
		} catch (Exception ex) {
			_logger.warn("Could not clean up credentials for deleted thing: " + RepoName);
		}
		_logger.warn("GitBackup Thing " + RepoName + " stored in File Repository: " + str_RepositoryName + " was deleted successfully.");
	}

	@ThingworxServiceDefinition(name = "GetEmptyInfotable", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = {})
	public InfoTable GetEmptyInfotable() throws Exception {
		return InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
	}

	@ThingworxServiceDefinition(name = "GetGitExtensionVersion", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:GitBackup.ExtensionVersion" })
	public InfoTable GetGitExtensionVersion() throws Exception {
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.ExtensionVersion");
		Thing platformSubsystem = (Thing) EntityUtilities.findEntity("PlatformSubsystem",
				ThingworxRelationshipTypes.Subsystem);
		InfoTable extensionList = (InfoTable) platformSubsystem
				.processServiceRequest("GetExtensionPackageList", new ValueCollection());

		String[][] extensions = {
			{ "GitBackupExtension", "GitBackupExtension" },
			{ "DiffViewer", "DiffViewer" },
			{ "ExportPlatformExt_Extension", "ExportPlatformExt_Extension" },
			{ "Autocomplete", "Autocomplete" },
			{ "GitInfoTableSelector_ExtensionPackage", "GitInfoTableSelector_ExtensionPackage" },
			{ "FileUtilities", "FileUtilities" }
		};

		for (String[] ext : extensions) {
			ValueCollection row = new ValueCollection();
			row.put("ExtensionName", new StringPrimitive(ext[1]));
			row.put("IsInstalled", new BooleanPrimitive(false));
			row.put("ExtensionVersion", new StringPrimitive("N/A"));
			for (int i = 0; i < extensionList.getRowCount(); i++) {
				String name = extensionList.getRow(i).getPrimitive("name").getValue().toString();
				if (name.contains(ext[0])) {
					row.put("IsInstalled", new BooleanPrimitive(true));
					IPrimitiveType pv = extensionList.getRow(i).getPrimitive("packageVersion");
					row.put("ExtensionVersion", new StringPrimitive(pv != null ? pv.getValue().toString() : "N/A"));
					break;
				}
			}
			result.addRow(row);
		}
		return result;
	}

	@ThingworxServiceDefinition(name = "GetGitHeaderTabs", description = "This service gets the list of the Git Things in the System, plus one additional Mashup for the plus sign", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:GitBackup.GitHeader" })
	public InfoTable GetGitHeaderTabs() throws Exception {
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.GitHeader");
		ThingTemplate gitTemplate = (ThingTemplate) EntityUtilities.findEntity("GitBackupTemplate",
				ThingworxRelationshipTypes.ThingTemplate);
		ValueCollection queryParams = new ValueCollection();
		queryParams.put("maxItems", new StringPrimitive(""));
		queryParams.put("nameMask", new StringPrimitive(""));
		queryParams.put("query", new StringPrimitive(""));
		queryParams.put("tags", new StringPrimitive(""));
		InfoTable gitThings = (InfoTable) gitTemplate.processServiceRequest("QueryImplementingThings", queryParams);
		for (int x = 0; x < gitThings.getRowCount(); x++) {
			ValueCollection row = gitThings.getRow(x);
			ValueCollection entry = new ValueCollection();
			entry.put("HeightY", new StringPrimitive("50"));
			entry.put("MashupName", new StringPrimitive("GitBackup.NameTab.Mashup"));
			entry.put("GitThingName", new StringPrimitive(row.getPrimitive("name").getValue().toString()));
			entry.put("WidthX", new StringPrimitive("230"));
			result.addRow(entry);
		}
		return result;
	}

	@ThingworxServiceDefinition(name = "GetGitUserExtensionsProperties", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:GitBackup.UserExtensionProperties" })
	public InfoTable GetGitUserExtensionsProperties() throws Exception {
		User currentUser = UserUtilities.findUser(GetCurrentUser());
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.UserExtensionProperties");
		try {
			IPrimitiveType propVal = currentUser.getPropertyValue("GitCommitterName");
			if (propVal != null && propVal.getValue() != null) {
				ValueCollection entry = new ValueCollection();
				entry.put("GitCommitterName", currentUser.getPropertyValue("GitCommitterName"));
				entry.put("GitCommitterEmail", currentUser.getPropertyValue("GitCommitterEmail"));
				entry.put("UseGitCommitUserValues", currentUser.getPropertyValue("UseGitCommitUserValues"));
				result.addRow(entry);
			}
		} catch (Exception e) {
		}
		return result;
	}

	@ThingworxServiceDefinition(name = "SetGitUserExtensionsProperties", description = "Sets the Git committer name, email, and the UseGitCommitUserValues flag on the current user's extension properties.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void SetGitUserExtensionsProperties(
			@ThingworxServiceParameter(name = "GitCommitterName", description = "", baseType = "STRING") String GitCommitterName,
			@ThingworxServiceParameter(name = "GitCommitterEmail", description = "", baseType = "STRING") String GitCommitterEmail,
			@ThingworxServiceParameter(name = "UseGitCommitUserValues", description = "", baseType = "BOOLEAN") Boolean UseGitCommitUserValues)
			throws Exception {
		User currentUser = UserUtilities.findUser(GetCurrentUser());
		ensureUserExtensionProperty(currentUser, "GitCommitterName", "STRING");
		ensureUserExtensionProperty(currentUser, "GitCommitterEmail", "STRING");
		ensureUserExtensionProperty(currentUser, "UseGitCommitUserValues", "BOOLEAN");

		currentUser.setPropertyValue("GitCommitterName", new StringPrimitive(GitCommitterName));
		currentUser.setPropertyValue("GitCommitterEmail", new StringPrimitive(GitCommitterEmail));
		currentUser.setPropertyValue("UseGitCommitUserValues", new BooleanPrimitive(UseGitCommitUserValues));
	}

	@ThingworxServiceDefinition(name = "GetProjectEntities", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:SpotlightSearch" })
	public InfoTable GetProjectEntities(
			@ThingworxServiceParameter(name = "project", description = "", baseType = "STRING") String project,
			@ThingworxServiceParameter(name = "entityName", description = "", baseType = "STRING") String entityName,
			@ThingworxServiceParameter(name = "entityType", description = "", baseType = "STRING") String entityType,
			@ThingworxServiceParameter(name = "includeDependents", description = "", baseType = "BOOLEAN") Boolean includeDependents,
			@ThingworxServiceParameter(name = "tags", description = "", baseType = "TAGS") String tags)
			throws Exception {
		if (project == null || project.isEmpty()) throw new Exception("Can not retrieve the entities that are part of the Project. The project name was not specified or it's empty");

		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
		Thing projectThing = (Thing) EntityUtilities.findEntity(project, ThingworxRelationshipTypes.Project);
		if (projectThing == null) throw new Exception("Project " + project + " not found");

		java.util.List<String> projectNames = new java.util.ArrayList<>();
		projectNames.add(project);
		if (includeDependents != null && includeDependents) {
			InfoTable deps = (InfoTable) projectThing.processServiceRequest("GetAllDependentProjectNames", new ValueCollection());
			for (int i = 0; i < deps.getRowCount(); i++) {
				projectNames.add(deps.getRow(i).getPrimitive("item").getValue().toString());
			}
		}

		for (String pname : projectNames) {
			ValueCollection searchParams = new ValueCollection();
			searchParams.put("maxItems", new StringPrimitive("30000"));
			searchParams.put("searchExpression", new StringPrimitive(""));
			searchParams.put("types", new StringPrimitive(""));
			searchParams.put("withPermissions", new StringPrimitive(""));
			searchParams.put("endDate", new StringPrimitive(""));
			searchParams.put("aspects", new StringPrimitive(""));
			searchParams.put("excludedAspects", new StringPrimitive(""));
			searchParams.put("tags", tags != null ? new StringPrimitive(tags) : new StringPrimitive(""));
			searchParams.put("thingTemplates", new StringPrimitive(""));
			searchParams.put("searchDescriptions", new StringPrimitive(""));
			searchParams.put("thingShapes", new StringPrimitive(""));
			searchParams.put("sortBy", new StringPrimitive("lastModifiedDate"));
			searchParams.put("isAscending", new BooleanPrimitive(false));
			searchParams.put("projectName", new StringPrimitive(pname));
			searchParams.put("maxSearchItems", new StringPrimitive(""));
			searchParams.put("startDate", new StringPrimitive(""));

			// Use SearchFunctions via processServiceRequest on the search subsystem
			Thing searchSubsystem = (Thing) EntityUtilities.findEntity("SearchFunctions",
					ThingworxRelationshipTypes.Resource);
			InfoTable searchResult = (InfoTable) searchSubsystem.processServiceRequest("SpotlightSearch", searchParams);
			result = unionInfoTables(result, searchResult);
		}

		if (entityName != null && !entityName.isEmpty()) {
			result = queryInfoTable(result, "name", "LIKE", "*" + entityName + "*");
		}
		if (entityType != null && !entityType.isEmpty()) {
			result = queryInfoTable(result, "type", "LIKE", "*" + entityType + "*");
		}
		return result;
	}

	@ThingworxServiceDefinition(name = "GetRepoConfiguration", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:GitBackup.Configuration" })
	public InfoTable GetRepoConfiguration(
			@ThingworxServiceParameter(name = "GitThingName", description = "", baseType = "STRING") String GitThingName)
			throws Exception {
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.Configuration");
		User currentUser = UserUtilities.findUser(GetCurrentUser());
		Thing repoThing = (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
		ValueCollection getConfigParams = new ValueCollection();
		getConfigParams.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
		InfoTable credentials = getGitCredentials(currentUser);
		ValueCollection credRow = null;
		if (credentials != null) {
			ValueCollection filter = new ValueCollection();
			filter.put("GitThing", new StringPrimitive(GitThingName));
			credRow = credentials.find(filter);
		}

		ValueCollection entry = new ValueCollection();
		entry.put("FileRepoPath", cfgTable.getRow(0).getPrimitive("RepoPathName"));
		entry.put("CommitUser", credRow != null ? credRow.getPrimitive("GitCommitterFullName") : new StringPrimitive(""));
		entry.put("CommitEmail", credRow != null ? credRow.getPrimitive("GitCommitterEmail") : new StringPrimitive(""));
		entry.put("User", credRow != null ? credRow.getPrimitive("GitCommitterUser") : new StringPrimitive(""));
		entry.put("FileRepository", cfgTable.getRow(0).getPrimitive("FileRepository"));
		entry.put("GitRepoURL", cfgTable.getRow(0).getPrimitive("GitRepoURL"));
		entry.put("InitialBranch", cfgTable.getRow(0).getPrimitive("BranchName"));
		entry.put("UseProxy", cfgTable.getRow(0).getPrimitive("UseProxy"));
		entry.put("ProxyURL", cfgTable.getRow(0).getPrimitive("ProxyURL"));
		entry.put("ProxyPort", cfgTable.getRow(0).getPrimitive("ProxyPort"));
		entry.put("Password", credRow != null ? credRow.getPrimitive("GitCommitterPassword") : new StringPrimitive(""));
		entry.put("LocalizationTokensPrefix", cfgTable.getRow(0).getPrimitive("LocalizationTokensPrefix"));
		result.addRow(entry);
		return result;
	}

	@ThingworxServiceDefinition(name = "ImportEntity", description = "This will import an entity in the system.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void ImportEntity(
			@ThingworxServiceParameter(name = "entityPath", description = "relative to the repository", baseType = "STRING") String entityPath,
			@ThingworxServiceParameter(name = "FileRepositoryName", description = "", baseType = "STRING") String FileRepositoryName,
			@ThingworxServiceParameter(name = "ignoreDependencies", description = "If true, strips dependency validation during import", baseType = "BOOLEAN", aspects = { "defaultValue:false" }) Boolean ignoreDependencies)
			throws Exception {
		_logger.warn("Started single entity import.");
		Thing appKeyThing = (Thing) EntityUtilities.findEntity("GitExtensionAppKey",
				ThingworxRelationshipTypes.ApplicationKey);
		if (appKeyThing == null) throw new Exception("GitExtensionAppKey not found. Run InitExtensionImportTargets first.");
		Thing importTargets = (Thing) EntityUtilities.findEntity("ExtensionImportTargets",
				ThingworxRelationshipTypes.Thing);
		if (importTargets == null) throw new Exception("ExtensionImportTargets not configured.");

		InfoTable targetsTable = (InfoTable) importTargets.getPropertyValue("importTargets");
		if (targetsTable == null || targetsTable.getRowCount() == 0)
			throw new Exception("ExtensionImportTargets not configured. Run InitExtensionImportTargets first.");

		String baseURL = targetsTable.getRow(0).getPrimitive("baseURL").getValue().toString();
		String appKey = appKeyThing.getPropertyValue("keyId").getValue().toString();
		String ignoreDeps = (ignoreDependencies != null && ignoreDependencies) ? "&ignoreDependencies=true" : "";

		String url = baseURL + "/Importer?IgnoreBadValueStreamData=false&WithSubsystems=false"
				+ "&overwritePropertyValues=true&purpose=import&usedefaultdataprovider=true" + ignoreDeps;

		ValueCollection params = new ValueCollection();
		params.put("proxyScheme", new StringPrimitive(""));
		params.put("headers", new StringPrimitive("{\"appKey\":\"" + appKey + "\",\"X-XSRF-TOKEN\":\"TWX-XSRF-TOKEN-VALUE\"}"));
		params.put("ignoreSSLErrors", new BooleanPrimitive(true));
		params.put("useNTLM", new BooleanPrimitive(false));
		params.put("partsToSend", new InfoTablePrimitive(InfoTableInstanceFactory.createInfoTableFromDataShape("")));
		params.put("workstation", new StringPrimitive(""));
		params.put("useProxy", new BooleanPrimitive(false));
		params.put("repository", new StringPrimitive(FileRepositoryName));
		params.put("proxyHost", new StringPrimitive(""));
		params.put("url", new StringPrimitive(url));
		params.put("timeout", new StringPrimitive(""));
		params.put("proxyPort", new StringPrimitive("0"));
		params.put("password", new StringPrimitive(""));
		params.put("pathOnRepository", new StringPrimitive(entityPath));
		params.put("domain", new StringPrimitive(""));
		params.put("username", new StringPrimitive(""));

		Thing contentLoader = (Thing) EntityUtilities.findEntity("ContentLoaderFunctions",
				ThingworxRelationshipTypes.Resource);
		InfoTable responseTable = contentLoader.processServiceRequest("PostMultipart", params);
		String importResponse = responseTable != null && responseTable.getRowCount() > 0
				? responseTable.getRow(0).getPrimitive("response").getValue().toString() : "";
		_logger.warn("Finished importing single Entity " + entityPath + ". Response: " + importResponse);
	}

	@ThingworxServiceDefinition(name = "ImportProjectEntities", description = "Bulk imports all entity XML files from a FileRepository path. Returns a summary INFOTABLE with success/failure per entity.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:GitBackup.TestResult" })
	public InfoTable ImportProjectEntities(
			@ThingworxServiceParameter(name = "GitThingName", description = "GitBackup Thing name whose FileRepository and path to scan", baseType = "STRING") String GitThingName,
			@ThingworxServiceParameter(name = "entityPath", description = "Relative path within the FileRepository to scan for XML files", baseType = "STRING") String entityPath,
			@ThingworxServiceParameter(name = "ignoreDependencies", description = "If true, strips dependency validation during import", baseType = "BOOLEAN", aspects = { "defaultValue:false" }) Boolean ignoreDependencies)
			throws Exception {
		_logger.warn("Started bulk import for GitThing: " + GitThingName);
		if (GitThingName == null || GitThingName.isEmpty())
			throw new Exception("GitThingName is required.");

		Thing repoThing = (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
		ValueCollection getConfigParams = new ValueCollection();
		getConfigParams.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
		String str_FileRepositoryName = cfgTable.getRow(0).getPrimitive("FileRepository").getValue().toString();
		String str_RepoPath = (entityPath != null && !entityPath.isEmpty()) ? entityPath
				: cfgTable.getRow(0).getPrimitive("RepoPathName").getValue().toString();

		Thing appKeyThing = (Thing) EntityUtilities.findEntity("GitExtensionAppKey",
				ThingworxRelationshipTypes.ApplicationKey);
		if (appKeyThing == null)
			throw new Exception("GitExtensionAppKey not found. Run InitExtensionImportTargets first.");
		Thing importTargets = (Thing) EntityUtilities.findEntity("ExtensionImportTargets",
				ThingworxRelationshipTypes.Thing);
		if (importTargets == null)
			throw new Exception("ExtensionImportTargets not configured. Run InitExtensionImportTargets first.");

		InfoTable targetsTable = (InfoTable) importTargets.getPropertyValue("importTargets");
		if (targetsTable == null || targetsTable.getRowCount() == 0)
			throw new Exception("ExtensionImportTargets not configured. Run InitExtensionImportTargets first.");

		String baseURL = targetsTable.getRow(0).getPrimitive("baseURL").getValue().toString();
		String appKey = appKeyThing.getPropertyValue("keyId").getValue().toString();
		String ignoreDeps = (ignoreDependencies != null && ignoreDependencies) ? "&ignoreDependencies=true" : "";

		Thing fileRepo = (Thing) EntityUtilities.findEntity(str_FileRepositoryName,
				ThingworxRelationshipTypes.Thing);

		// Recursively list all XML files
		InfoTable allFiles = InfoTableInstanceFactory.createInfoTableFromDataShape("FileSystemFile");
		collectXmlFiles(fileRepo, str_RepoPath, allFiles);
		_logger.warn("Found " + allFiles.getRowCount() + " XML files for import.");

		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.TestResult");
		int int_SuccessCount = 0;
		int int_FailCount = 0;

		for (int x = 0; x < allFiles.getRowCount(); x++) {
			ValueCollection fileRow = allFiles.getRow(x);
			String filePath = fileRow.getPrimitive("path").getValue().toString();
			String fileName = fileRow.getPrimitive("name").getValue().toString();
			ValueCollection entry = new ValueCollection();
			entry.put("testName", new StringPrimitive(filePath));
			entry.put("startTimestamp", new DatetimePrimitive(new DateTime(System.currentTimeMillis())));
			try {
				String url = baseURL + "/Importer?IgnoreBadValueStreamData=false&WithSubsystems=false"
						+ "&overwritePropertyValues=true&purpose=import&usedefaultdataprovider=true" + ignoreDeps;

				ValueCollection params = new ValueCollection();
				params.put("proxyScheme", new StringPrimitive(""));
				params.put("headers", new StringPrimitive(
						"{\"appKey\":\"" + appKey + "\",\"X-XSRF-TOKEN\":\"TWX-XSRF-TOKEN-VALUE\"}"));
				params.put("ignoreSSLErrors", new BooleanPrimitive(true));
				params.put("useNTLM", new BooleanPrimitive(false));
				params.put("partsToSend",
						new InfoTablePrimitive(InfoTableInstanceFactory.createInfoTableFromDataShape("")));
				params.put("workstation", new StringPrimitive(""));
				params.put("useProxy", new BooleanPrimitive(false));
				params.put("repository", new StringPrimitive(str_FileRepositoryName));
				params.put("proxyHost", new StringPrimitive(""));
				params.put("url", new StringPrimitive(url));
				params.put("timeout", new StringPrimitive(""));
				params.put("proxyPort", new StringPrimitive("0"));
				params.put("password", new StringPrimitive(""));
				params.put("pathOnRepository", new StringPrimitive(filePath));
				params.put("domain", new StringPrimitive(""));
				params.put("username", new StringPrimitive(""));

				Thing contentLoader = (Thing) EntityUtilities.findEntity("ContentLoaderFunctions",
						ThingworxRelationshipTypes.Resource);
				InfoTable responseTable = contentLoader.processServiceRequest("PostMultipart", params);
				String importResponse = responseTable != null && responseTable.getRowCount() > 0
						? responseTable.getRow(0).getPrimitive("response").getValue().toString() : "";
				entry.put("passed", new BooleanPrimitive(true));
				entry.put("comments",
						new StringPrimitive(importResponse != null ? importResponse : "Import completed"));
				int_SuccessCount++;
				_logger.warn("Successfully imported: " + filePath);
			} catch (Exception ex) {
				entry.put("passed", new BooleanPrimitive(false));
				entry.put("comments", new StringPrimitive("Import failed: " + ex.getMessage()));
				int_FailCount++;
				_logger.error("Failed to import: " + filePath + "; Error: " + ex.getMessage());
			}
			entry.put("endTimestamp", new DatetimePrimitive(new DateTime(System.currentTimeMillis())));
			result.addRow(entry);
		}
		_logger.warn("Bulk import completed. Success: " + int_SuccessCount + ", Failed: " + int_FailCount
				+ ", Total: " + allFiles.getRowCount());
		return result;
	}

	@ThingworxServiceDefinition(name = "CheckIfBitbucketCredentialsAreCorrect", description = "Checks if the provided Bitbucket credentials are correct by attempting to authenticate against the repository URL.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "BOOLEAN", aspects = {})
	public Boolean CheckIfBitbucketCredentialsAreCorrect(
			@ThingworxServiceParameter(name = "GitThingName", description = "", baseType = "STRING") String GitThingName,
			@ThingworxServiceParameter(name = "Username", description = "", baseType = "STRING") String Username,
			@ThingworxServiceParameter(name = "Password", description = "", baseType = "STRING") String Password)
			throws Exception {
		if (GitThingName == null || GitThingName.isEmpty()) return false;
		Thing repoThing = (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
		ValueCollection getConfigParams = new ValueCollection();
		getConfigParams.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
		String gitRepoURL = cfgTable.getRow(0).getPrimitive("GitRepoURL").getValue().toString();
		if (gitRepoURL == null || gitRepoURL.isEmpty()) return false;

		try {
			URL url = URI.create(gitRepoURL).toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			String auth = Base64.getEncoder()
					.encodeToString((Username + ":" + Password).getBytes("UTF-8"));
			conn.setRequestProperty("Authorization", "Basic " + auth);
			int responseCode = conn.getResponseCode();
			conn.disconnect();
			return responseCode == 200;
		} catch (Exception e) {
			_logger.error("Credential check failed for repo " + GitThingName + ": " + e.getMessage());
			return false;
		}
	}

	private void collectXmlFiles(Thing fileRepo, String path, InfoTable allFiles) throws Exception {
		ValueCollection listParams = new ValueCollection();
		listParams.put("path", new StringPrimitive(path));
		listParams.put("nameMask", new StringPrimitive("*.xml"));
		InfoTable files = (InfoTable) fileRepo.processServiceRequest("ListFiles", listParams);
		for (int i = 0; i < files.getRowCount(); i++) {
			ValueCollection row = files.getRow(i);
			ValueCollection newRow = new ValueCollection();
			newRow.put("name", row.getPrimitive("name"));
			newRow.put("path", row.getPrimitive("path"));
			boolean dup = false;
			for (int j = 0; j < allFiles.getRowCount(); j++) {
				if (allFiles.getRow(j).getPrimitive("path").getValue().toString()
						.equals(row.getPrimitive("path").getValue().toString())) {
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
			String dirPath = dirs.getRow(i).getPrimitive("path").getValue().toString();
			collectXmlFiles(fileRepo, dirPath, allFiles);
		}
	}

	private void ensureUserExtensionProperty(User currentUser, String propName, String baseType)
			throws Exception {
		Object propVal = null;
		try {
			propVal = currentUser.getPropertyValue(propName);
		} catch (Exception e) {
			// property doesn't exist, need to add it
		}
		if (propVal == null) {
			ThingShape userExtensions = (ThingShape) EntityUtilities.findEntity("UserExtensions",
					ThingworxRelationshipTypes.ThingShape);
			ValueCollection addPropParams = new ValueCollection();
			addPropParams.put("defaultValue", new StringPrimitive(""));
			addPropParams.put("description", new StringPrimitive(""));
			addPropParams.put("readOnly", new BooleanPrimitive(false));
			addPropParams.put("type", new StringPrimitive(baseType));
			addPropParams.put("name", new StringPrimitive(propName));
			addPropParams.put("persistent", new BooleanPrimitive(true));
			userExtensions.processServiceRequest("AddPropertyDefinition", addPropParams);
			EntityServices es = new EntityServices();
			es.RestartDependenciesForThingShape("UserExtensions");
		}
	}

	@ThingworxServiceDefinition(name = "InitExtensionImportTargets", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void InitExtensionImportTargets(
			@ThingworxServiceParameter(name = "address", description = "", baseType = "STRING") String address)
			throws Exception {
		String str_CurrentUser = GetCurrentUser();
		EntityServices es = new EntityServices();
		try {
			es.DeleteApplicationKey("GitExtensionAppKey");
		} catch (Exception e) {
			// may not exist
		}
		es.CreateApplicationKey("GitExtensionAppKey",
				"Appkey generated by the Git Extension; do not remove or reuse in any script or code;"
						+ " it's automatically refreshed each time the GitBackup.Main.Mashup is opened.",
				str_CurrentUser, new TagCollection(), "Bearer", "*",
				new DateTime(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000));

		Thing importTargets = (Thing) EntityUtilities.findEntity("ExtensionImportTargets",
				ThingworxRelationshipTypes.Thing);
		InfoTable currentTargets = (InfoTable) importTargets.getPropertyValue("importTargets");
		if (currentTargets != null) {
			InfoTable updatedTargets = InfoTableInstanceFactory.createInfoTableFromDataShape("ExtensionImportTargetDS");
			for (int ci = 0; ci < currentTargets.getRowCount(); ci++) {
				ValueCollection row = currentTargets.getRow(ci);
				if (!"localhost".equals(row.getPrimitive("name").getValue().toString())) {
					updatedTargets.addRow(row);
				}
			}
			currentTargets = updatedTargets;
		}
		Thing appKeyThing = (Thing) EntityUtilities.findEntity("GitExtensionAppKey",
				ThingworxRelationshipTypes.ApplicationKey);
		String keyId = appKeyThing.getPropertyValue("keyId").getValue().toString();
		ValueCollection entry = new ValueCollection();
		entry.put("baseURL", new StringPrimitive(address + "/Thingworx"));
		entry.put("name", new StringPrimitive("localhost"));
		entry.put("appKey", new PasswordPrimitive(keyId));
		if (currentTargets == null) {
			currentTargets = InfoTableInstanceFactory.createInfoTableFromDataShape("ExtensionImportTargetDS");
		}
		currentTargets.addRow(entry);
		importTargets.setPropertyValue("importTargets", new InfoTablePrimitive(currentTargets));
	}

	@ThingworxServiceDefinition(name = "InitUserExtensionProperties", description = "Adds UserExtension Properties needed by the GitBackupExtension", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void InitUserExtensionProperties() throws Exception {
		String str_CurrentUser = GetCurrentUser();
		User user = UserUtilities.findUser(str_CurrentUser);
		ThingShape userExtensions = (ThingShape) EntityUtilities.findEntity("UserExtensions",
				ThingworxRelationshipTypes.ThingShape);

		Object gitCreds = null;
		try {
			gitCreds = user.getPropertyValue("GitCredentials");
		} catch (Exception e) {
		}
		if (gitCreds == null) {
			ValueCollection addPropParams = new ValueCollection();
			addPropParams.put("defaultValue", new StringPrimitive(""));
			addPropParams.put("description", new StringPrimitive("User-specific Git credentials store."));
			addPropParams.put("readOnly", new BooleanPrimitive(false));
			addPropParams.put("type", new StringPrimitive("INFOTABLE"));
			addPropParams.put("name", new StringPrimitive("GitCredentials"));
			addPropParams.put("persistent", new BooleanPrimitive(true));
			addPropParams.put("dataShape", new StringPrimitive("GitBackup.GitCredentials"));
			userExtensions.processServiceRequest("AddPropertyDefinition", addPropParams);
			new EntityServices().RestartDependenciesForThingShape("UserExtensions");
		}

		String[] oldProps = {"UseGitCommitUserValues", "GitCommitterEmail", "GitCommitterPassword", "GitCommitterName"};
		for (String prop : oldProps) {
			try {
				Object val = user.getPropertyValue(prop);
				if (val != null) {
					ValueCollection removeParams = new ValueCollection();
					removeParams.put("name", new StringPrimitive(prop));
					userExtensions.processServiceRequest("RemovePropertyDefinition", removeParams);
					new EntityServices().RestartDependenciesForThingShape("UserExtensions");
				}
			} catch (Exception e) {
			}
		}
	}

	@ThingworxServiceDefinition(name = "InitUserExtensionGpgKeysProperty", description = "Initializes the GpgKeys UserExtension INFOTABLE property for all users", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void InitUserExtensionGpgKeysProperty() throws Exception {
		String str_CurrentUser = GetCurrentUser();
		User user = UserUtilities.findUser(str_CurrentUser);
		ThingShape userExtensions = (ThingShape) EntityUtilities.findEntity("UserExtensions",
				ThingworxRelationshipTypes.ThingShape);

		Object gpgKeys = null;
		try {
			gpgKeys = user.getPropertyValue("GpgKeys");
		} catch (Exception e) {
		}
		if (gpgKeys == null) {
			ValueCollection addPropParams = new ValueCollection();
			addPropParams.put("defaultValue", new StringPrimitive(""));
			addPropParams.put("description", new StringPrimitive("User-specific GPG signing keys store."));
			addPropParams.put("readOnly", new BooleanPrimitive(false));
			addPropParams.put("type", new StringPrimitive("INFOTABLE"));
			addPropParams.put("name", new StringPrimitive("GpgKeys"));
			addPropParams.put("persistent", new BooleanPrimitive(true));
			addPropParams.put("dataShape", new StringPrimitive("GitBackup.GpgKey"));
			userExtensions.processServiceRequest("AddPropertyDefinition", addPropParams);
			new EntityServices().RestartDependenciesForThingShape("UserExtensions");
		}
	}

	private InfoTable getGpgKeysTable(User user) throws Exception {
		Object propVal = null;
		try {
			propVal = user.getPropertyValue("GpgKeys");
		} catch (Exception e) {
		}
		if (propVal == null) return null;
		return ((InfoTablePrimitive) propVal).getValue();
	}

	@ThingworxServiceDefinition(name = "GetGpgKeys", description = "Returns all GPG keys configured for the current user across all repositories", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:GitBackup.GpgKey" })
	public InfoTable GetGpgKeys() throws Exception {
		User currentUser = UserUtilities.findUser(GetCurrentUser());
		InfoTable gpgKeys = getGpgKeysTable(currentUser);
		if (gpgKeys != null) return gpgKeys;
		return InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.GpgKey");
	}

	@ThingworxServiceDefinition(name = "SetGpgKey", description = "Saves or updates a GPG key for the current user for a specific repository", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void SetGpgKey(
			@ThingworxServiceParameter(name = "GitThing", description = "Git Thing name", baseType = "THINGNAME") String GitThing,
			@ThingworxServiceParameter(name = "GpgPrivateKey", description = "ASCII-armored PGP private key", baseType = "STRING") String GpgPrivateKey,
			@ThingworxServiceParameter(name = "GpgKeyPassphrase", description = "Passphrase for the PGP private key", baseType = "STRING") String GpgKeyPassphrase,
			@ThingworxServiceParameter(name = "SignCommits", description = "Whether to sign commits", baseType = "BOOLEAN") Boolean SignCommits,
			@ThingworxServiceParameter(name = "GpgKeyFingerprint", description = "GPG key fingerprint for display", baseType = "STRING") String GpgKeyFingerprint)
			throws Exception {
		User currentUser = UserUtilities.findUser(GetCurrentUser());
		ThingShape userExtensions = (ThingShape) EntityUtilities.findEntity("UserExtensions",
				ThingworxRelationshipTypes.ThingShape);

		Object existingProp = null;
		try {
			existingProp = currentUser.getPropertyValue("GpgKeys");
		} catch (Exception e) {
		}
		if (existingProp == null) {
			ValueCollection addPropParams = new ValueCollection();
			addPropParams.put("defaultValue", new StringPrimitive(""));
			addPropParams.put("description", new StringPrimitive("User-specific GPG signing keys store."));
			addPropParams.put("readOnly", new BooleanPrimitive(false));
			addPropParams.put("type", new StringPrimitive("INFOTABLE"));
			addPropParams.put("name", new StringPrimitive("GpgKeys"));
			addPropParams.put("persistent", new BooleanPrimitive(true));
			addPropParams.put("dataShape", new StringPrimitive("GitBackup.GpgKey"));
			userExtensions.processServiceRequest("AddPropertyDefinition", addPropParams);
			new EntityServices().RestartDependenciesForThingShape("UserExtensions");
		}

		InfoTable gpgKeys = getGpgKeysTable(currentUser);
		if (gpgKeys == null) {
			gpgKeys = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.GpgKey");
		} else {
			InfoTable cloned = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.GpgKey");
			for (int i = 0; i < gpgKeys.getRowCount(); i++) {
				ValueCollection row = gpgKeys.getRow(i);
				if (!row.getPrimitive("GitThing").getValue().toString().equals(GitThing)) {
					cloned.addRow(row);
				}
			}
			gpgKeys = cloned;
		}
		ValueCollection entry = new ValueCollection();
		entry.put("GitThing", new StringPrimitive(GitThing));
		entry.put("GpgPrivateKey", new PasswordPrimitive(GpgPrivateKey));
		entry.put("GpgKeyPassphrase", new PasswordPrimitive(GpgKeyPassphrase));
		entry.put("SignCommits", new BooleanPrimitive(SignCommits));
		entry.put("GpgKeyFingerprint", new StringPrimitive(GpgKeyFingerprint));
		gpgKeys.addRow(entry);
		currentUser.setPropertyValue("GpgKeys", new InfoTablePrimitive(gpgKeys));
	}

	@ThingworxServiceDefinition(name = "DeleteGpgKey", description = "Removes a GPG key configuration for the current user for a specific repository", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void DeleteGpgKey(
			@ThingworxServiceParameter(name = "GitThing", description = "Git Thing name", baseType = "THINGNAME") String GitThing)
			throws Exception {
		User currentUser = UserUtilities.findUser(GetCurrentUser());
		InfoTable gpgKeys = getGpgKeysTable(currentUser);
		if (gpgKeys != null) {
			InfoTable cloned = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.GpgKey");
			for (int i = 0; i < gpgKeys.getRowCount(); i++) {
				ValueCollection row = gpgKeys.getRow(i);
				if (!row.getPrimitive("GitThing").getValue().toString().equals(GitThing)) {
					cloned.addRow(row);
				}
			}
			currentUser.setPropertyValue("GpgKeys", new InfoTablePrimitive(cloned));
		}
	}

	@ThingworxServiceDefinition(name = "Pause", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void Pause(
			@ThingworxServiceParameter(name = "delay", description = "delay in seconds", baseType = "NUMBER") Double delay)
			throws Exception {
		if (delay != null) {
			Thread.sleep((long) (delay * 100));
		}
	}

	@ThingworxServiceDefinition(name = "RemoveEntitiesFromExportList", description = "Removes entities from the export entity list", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:SpotlightSearch" })
	public InfoTable RemoveEntitiesFromExportList(
			@ThingworxServiceParameter(name = "entitiesToRemove", description = "", baseType = "INFOTABLE", aspects = { "dataShape:SpotlightSearch" }) InfoTable entitiesToRemove,
			@ThingworxServiceParameter(name = "existingEntities", description = "", baseType = "INFOTABLE", aspects = { "dataShape:SpotlightSearch" }) InfoTable existingEntities)
			throws Exception {
		if (existingEntities != null && existingEntities.getRowCount() > 0) {
			InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
			for (int x = 0; x < existingEntities.getRowCount(); x++) {
				ValueCollection row = existingEntities.getRow(x);
				String name = row.getPrimitive("name").getValue().toString();
				boolean shouldRemove = false;
				for (int y = 0; y < entitiesToRemove.getRowCount(); y++) {
					String removeName = entitiesToRemove.getRow(y).getPrimitive("name").getValue().toString();
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
		return existingEntities != null ? existingEntities : InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
	}

	@ThingworxServiceDefinition(name = "UpdateRepo", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void UpdateRepo(
			@ThingworxServiceParameter(name = "RepoName", description = "", baseType = "STRING") String RepoName,
			@ThingworxServiceParameter(name = "GitRepoURL", description = "", baseType = "STRING") String GitRepoURL,
			@ThingworxServiceParameter(name = "RepoPath", description = "", baseType = "STRING") String RepoPath,
			@ThingworxServiceParameter(name = "User", description = "", baseType = "STRING") String User,
			@ThingworxServiceParameter(name = "Password", description = "", baseType = "STRING") String Password,
			@ThingworxServiceParameter(name = "CommitUser", description = "", baseType = "STRING") String CommitUser,
			@ThingworxServiceParameter(name = "CommitEmail", description = "", baseType = "STRING") String CommitEmail,
			@ThingworxServiceParameter(name = "InitialBranch", description = "", baseType = "STRING") String InitialBranch,
			@ThingworxServiceParameter(name = "UseProxy", description = "", baseType = "BOOLEAN") Boolean UseProxy,
			@ThingworxServiceParameter(name = "ProxyURL", description = "", baseType = "STRING") String ProxyURL,
			@ThingworxServiceParameter(name = "ProxyPort", description = "", baseType = "INTEGER") Integer ProxyPort,
			@ThingworxServiceParameter(name = "LocalizationTokensPrefix", description = "", baseType = "STRING") String LocalizationTokensPrefix)
			throws Exception {
		if (RepoName == null || GitRepoURL == null) {
			_logger.error("Could not update GitThing. Either RepoName or GitRepoURL did not contain data.");
			return;
		}
		Thing repoThing = (Thing) EntityUtilities.findEntity(RepoName, ThingworxRelationshipTypes.Thing);
		ValueCollection getConfigParams = new ValueCollection();
		getConfigParams.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
		ValueCollection row = cfgTable.getRow(0);
		if (InitialBranch != null && !InitialBranch.isEmpty()) row.put("BranchName", new StringPrimitive(InitialBranch));
		if (UseProxy != null) row.put("UseProxy", new BooleanPrimitive(UseProxy));
		if (ProxyURL != null) row.put("ProxyURL", new StringPrimitive(ProxyURL));
		if (ProxyPort != null) row.put("ProxyPort", new StringPrimitive(ProxyPort.toString()));
		if (LocalizationTokensPrefix != null) row.put("LocalizationTokensPrefix", new StringPrimitive(LocalizationTokensPrefix));

		InfoTable newCfg = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.ConfigurationSetting");
		newCfg.addRow(row);
		ValueCollection setConfigParams = new ValueCollection();
		setConfigParams.put("configurationTable", new InfoTablePrimitive(newCfg));
		setConfigParams.put("persistent", new BooleanPrimitive(false));
		setConfigParams.put("tableName", new StringPrimitive("Configuration"));
		repoThing.processServiceRequest("SetConfigurationTable", setConfigParams);
		repoThing.processServiceRequest("SaveConfigurationTables", new ValueCollection());
		repoThing.processServiceRequest("RestartThing", new ValueCollection());

		SetGitCredentials(User, Password, CommitEmail, CommitUser, RepoName);
		_logger.warn("Git Thing configuration was updated successfully.");
	}

	@ThingworxServiceDefinition(name = "ValidateGitThingName", description = "validates the Git Thing name", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "STRING", aspects = {})
	public String ValidateGitThingName(
			@ThingworxServiceParameter(name = "GitThingName", description = "", baseType = "STRING") String GitThingName)
			throws Exception {
		Thing searchFunctions = (Thing) EntityUtilities.findEntity("SearchFunctions",
				ThingworxRelationshipTypes.Resource);
		ValueCollection searchParams = new ValueCollection();
		searchParams.put("maxItems", new StringPrimitive("10000000"));
		searchParams.put("searchExpression", new StringPrimitive("*"));
		searchParams.put("types", new StringPrimitive("{\"items\":[\"Thing\"]}"));
		searchParams.put("withPermissions", new BooleanPrimitive(true));
		searchParams.put("aspects", new StringPrimitive("{\"isSystemObject\":false}"));
		searchParams.put("thingTemplates", new StringPrimitive(
				"{\"excludedItems\":[\"Timer\",\"Scheduler\",\"GenericConnector\",\"IndustrialGateway\"]}"));
		searchParams.put("thingShapes", new StringPrimitive(
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

		InfoTable results = (InfoTable) searchFunctions.processServiceRequest("SpotlightSearch", searchParams);
		boolean found = false;
		for (int i = 0; i < results.getRowCount(); i++) {
			String name = results.getRow(i).getPrimitive("name").getValue().toString();
			if (name.equalsIgnoreCase(GitThingName)) {
				found = true;
				break;
			}
		}
		if (found) return "Thing " + GitThingName + " already exists in the platform. Please provide another Thing name.";
		return "Success. The provided thing name is valid.";
	}

	@ThingworxServiceDefinition(name = "SetGitCredentials", description = "Stores or updates Git credentials for a GitBackup thing in the current user's GitCredentials property.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void SetGitCredentials(
			@ThingworxServiceParameter(name = "GitCommitterUser", description = "", baseType = "STRING") String GitCommitterUser,
			@ThingworxServiceParameter(name = "GitCommitterPassword", description = "", baseType = "STRING") String GitCommitterPassword,
			@ThingworxServiceParameter(name = "GitCommitterEmail", description = "", baseType = "STRING") String GitCommitterEmail,
			@ThingworxServiceParameter(name = "GitCommitterFullName", description = "", baseType = "STRING") String GitCommitterFullName,
			@ThingworxServiceParameter(name = "GitThing", description = "", baseType = "THINGNAME") String GitThing)
			throws Exception {
		User currentUser = UserUtilities.findUser(GetCurrentUser());
		if (currentUser == null) return;

		InfoTable creds = getGitCredentials(currentUser);
		if (creds == null) {
			creds = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.GitCredentials");
		} else {
			InfoTable cloned = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.GitCredentials");
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
		currentUser.setPropertyValue("GitCredentials", new InfoTablePrimitive(creds));
	}

	// ---- Services from GitUtilityThingShape (merged) ----

	@ThingworxServiceDefinition(name = "ExportLocalizationToken", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void ExportLocalizationToken(
			@ThingworxServiceParameter(name = "prefix", description = "", baseType = "STRING") String prefix)
			throws Exception {
		_logger.warn("ExportLocalizationToken not yet implemented in Java. Falling back to script if available.");
	}

	@ThingworxServiceDefinition(name = "ExportProjectData", description = "functionality that allows exporting data from the Project DataTables/Streams/ValueStreams", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void ExportProjectData(
			@ThingworxServiceParameter(name = "ProjectName", description = "", baseType = "STRING") String ProjectName)
			throws Exception {
		_logger.warn("ExportProjectData not yet implemented in Java. Falling back to script if available.");
	}

	@ThingworxServiceDefinition(name = "ExportProjectEntities", description = "Wrapper for the ExportToSourceControl service.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void ExportProjectEntities(
			@ThingworxServiceParameter(name = "ProjectName", description = "", baseType = "STRING") String ProjectName,
			@ThingworxServiceParameter(name = "includeDependents", description = "", baseType = "BOOLEAN") Boolean includeDependents,
			@ThingworxServiceParameter(name = "EntitiesToExport", description = "Optional; if not set all project entities will be exported", baseType = "INFOTABLE", aspects = { "dataShape:SpotlightSearch" }) InfoTable EntitiesToExport,
			@ThingworxServiceParameter(name = "commitMessage", description = "Optional commit message. If provided, a git commit and push will be performed after export.", baseType = "STRING") String commitMessage)
			throws Exception {
		_logger.warn("ExportProjectEntities not yet implemented in Java. Falling back to script if available.");
	}

	@ThingworxServiceDefinition(name = "ExportProjectExtensions", description = "wrapper for Extensions Export / ExportExtensionsToRepository", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void ExportProjectExtensions() throws Exception {
		_logger.warn("ExportProjectExtensions not yet implemented in Java. Falling back to script if available.");
	}

	@ThingworxServiceDefinition(name = "GetConfiguration", description = "This service wrapper allows getting the GitBackup Thing configuration without passing the Configuration table name.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:GitBackup.Configuration" })
	public InfoTable GetConfiguration() throws Exception {
		_logger.warn("GetConfiguration not yet implemented in Java. Falling back to script if available.");
		return InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.Configuration");
	}

	@ThingworxServiceDefinition(name = "GetFilteredDirectoryListing", description = "Gets recursively the directories found in a subfolder in a FileRepository. Not part of the services of a FileRepository", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:FileSystemDirectory" })
	public InfoTable GetFilteredDirectoryListing() throws Exception {
		_logger.warn("GetFilteredDirectoryListing not yet implemented in Java. Falling back to script if available.");
		return InfoTableInstanceFactory.createInfoTableFromDataShape("FileSystemDirectory");
	}

	@ThingworxServiceDefinition(name = "GetLocalBranches", description = "overload of GetBranchList", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:Git.BranchList" })
	public InfoTable GetLocalBranches() throws Exception {
		_logger.warn("GetLocalBranches not yet implemented in Java. Falling back to script if available.");
		return InfoTableInstanceFactory.createInfoTableFromDataShape("Git.BranchList");
	}

	@ThingworxServiceDefinition(name = "CreateBranch", description = "Creates a new local branch from an optional start point (commit, branch, or tag) without switching to it.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "STRING", aspects = {})
	public String CreateBranch(
			@ThingworxServiceParameter(name = "BranchName", description = "Name of the new branch", baseType = "STRING") String BranchName,
			@ThingworxServiceParameter(name = "StartPoint", description = "Optional: commit hash, branch name, or tag to branch from (defaults to HEAD)", baseType = "STRING") String StartPoint)
			throws Exception {
		_logger.warn("CreateBranch not yet implemented in Java. Falling back to script if available.");
		return "";
	}

	@ThingworxServiceDefinition(name = "QueryDiffFileList", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:GitBackup.CommitChangedFiles" })
	public InfoTable QueryDiffFileList(
			@ThingworxServiceParameter(name = "CommitID", description = "", baseType = "STRING") String CommitID,
			@ThingworxServiceParameter(name = "FileName", description = "", baseType = "STRING") String FileName)
			throws Exception {
		_logger.warn("QueryDiffFileList not yet implemented in Java. Falling back to script if available.");
		return InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.CommitChangedFiles");
	}

	@ThingworxServiceDefinition(name = "QueryStatus", description = "Filters through the result of the Status Git command. Search is implemented in ThingWorx for flexibility.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:Git.Status" })
	public InfoTable QueryStatus(
			@ThingworxServiceParameter(name = "SearchTerm", description = "", baseType = "STRING") String SearchTerm)
			throws Exception {
		_logger.warn("QueryStatus not yet implemented in Java. Falling back to script if available.");
		return InfoTableInstanceFactory.createInfoTableFromDataShape("Git.Status");
	}

	@ThingworxServiceDefinition(name = "RemoveConfigurationTableDefinitions", description = "Removes the ConfigurationTableDefinitions to allow compatibility with 8.2. This service should be used only if you're doing crossplatform development between 8.5 and 8.2/8.3. Enable or disable in the ExportProjectEntities services", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void RemoveConfigurationTableDefinitions() throws Exception {
		_logger.warn("RemoveConfigurationTableDefinitions not yet implemented in Java. Falling back to script if available.");
	}

	@ThingworxServiceDefinition(name = "RemoveLastModifiedDate", description = "Removes the lastModifiedDate. Change history is already removed by ExportToSourceControlEntities", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void RemoveLastModifiedDate() throws Exception {
		_logger.warn("RemoveLastModifiedDate not yet implemented in Java. Falling back to script if available.");
	}

	@ThingworxServiceDefinition(name = "RemoveMashupPreviewTag", description = "Removes the mashup preview tag to allow compatibility with 8.2. This service should be used only if you're doing crossplatform development between 8.5 and 8.2/8.3", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void RemoveMashupPreviewTag() throws Exception {
		_logger.warn("RemoveMashupPreviewTag not yet implemented in Java. Falling back to script if available.");
	}

	@ThingworxServiceDefinition(name = "RemoveModelPersistenceProviderPackage", description = "Removes the modelPersistenceProviderPackage.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void RemoveModelPersistenceProviderPackage() throws Exception {
		_logger.warn("RemoveModelPersistenceProviderPackage not yet implemented in Java. Falling back to script if available.");
	}

	private InfoTable getGitCredentials(User user) throws Exception {
		Object propVal = null;
		try {
			propVal = user.getPropertyValue("GitCredentials");
		} catch (Exception e) {
		}
		if (propVal == null) return null;
		return ((InfoTablePrimitive) propVal).getValue();
	}

	private String GetCurrentUser() {
		return ThreadLocalContext.getSecurityContext().getName();
	}

	private InfoTable unionInfoTables(InfoTable t1, InfoTable t2) throws Exception {
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
		java.util.Set<String> seen = new java.util.HashSet<>();
		for (int i = 0; i < t1.getRowCount(); i++) {
			ValueCollection row = t1.getRow(i);
			String key = row.getPrimitive("name").getValue().toString();
			if (!seen.contains(key)) {
				result.addRow(row);
				seen.add(key);
			}
		}
		for (int i = 0; i < t2.getRowCount(); i++) {
			ValueCollection row = t2.getRow(i);
			String key = row.getPrimitive("name").getValue().toString();
			if (!seen.contains(key)) {
				result.addRow(row);
				seen.add(key);
			}
		}
		return result;
	}

	private InfoTable queryInfoTable(InfoTable table, String fieldName, String filterType, String value) throws Exception {
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
		for (int i = 0; i < table.getRowCount(); i++) {
			ValueCollection row = table.getRow(i);
			String fieldVal = row.getPrimitive(fieldName).getValue().toString();
			if (filterType.equals("LIKE")) {
				String pattern = value.replace("*", ".*");
				if (fieldVal.matches("(?i)" + pattern)) {
					result.addRow(row);
				}
			}
		}
		return result;
	}
}
