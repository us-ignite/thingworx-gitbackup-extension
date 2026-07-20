package gb.extension;

import static gb.extension.Values.hasText;
import static gb.extension.Values.isBlank;
import static gb.extension.Values.isTrue;
import static gb.extension.Values.orDefault;
import static gb.extension.Values.primitiveString;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.entities.interfaces.IServiceProvider;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.security.applicationkeys.ApplicationKey;
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
import com.thingworx.system.subsystems.platform.PlatformSubsystem;
import com.thingworx.things.Thing;
import com.thingworx.things.repository.FileRepositoryThing;
import com.thingworx.thingshape.ThingShape;
import com.thingworx.thingtemplates.ThingTemplate;
import com.thingworx.types.InfoTable;
import com.thingworx.types.TagCollection;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.BooleanPrimitive;
import com.thingworx.types.primitives.DatetimePrimitive;
import com.thingworx.types.primitives.GUIDPrimitive;
import com.thingworx.types.primitives.IPrimitiveType;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.IntegerPrimitive;
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
		entry.put("ID", new GUIDPrimitive(java.util.UUID.randomUUID().toString()));
		entry.put("timestamp", new DatetimePrimitive(timestamp));
		entry.put("User", new StringPrimitive(User));
		entry.put("ServiceName", new StringPrimitive(ServiceName));
		entry.put("Content", new StringPrimitive(Content));
		entry.put("Source", new StringPrimitive(Source));
		values.addRow(entry);
		Thing dataTable = (Thing) EntityUtilities.findEntity("GitBackup.Log.DataTable", ThingworxRelationshipTypes.Thing);
		ValueCollection params = new ValueCollection();
		params.put("values", new InfoTablePrimitive(values));
		params.put("source", new StringPrimitive(Source));
		dataTable.processServiceRequest("AddDataTableEntry", params);
	}

	@ThingworxServiceDefinition(name = "AddNewRepo", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void AddNewRepo(
			@ThingworxServiceParameter(name = "RepoName", description = "", baseType = "STRING") String RepoName,
			@ThingworxServiceParameter(name = "GitRepoURL", description = "", baseType = "STRING") String GitRepoURL,
			@ThingworxServiceParameter(name = "RepoPath", description = "", baseType = "STRING") String RepoPath,
			@ThingworxServiceParameter(name = "FileRepo", description = "", baseType = "STRING") String FileRepo,
			@ThingworxServiceParameter(name = "User", description = "", baseType = "STRING") String User,
			@ThingworxServiceParameter(name = "Password", description = "", baseType = "STRING") String Password,
			@ThingworxServiceParameter(name = "CommitUser", description = "", baseType = "STRING") String CommitUser,
			@ThingworxServiceParameter(name = "CommitEmail", description = "", baseType = "STRING") String CommitEmail,
			@ThingworxServiceParameter(name = "InitialBranch", description = "", baseType = "STRING") String InitialBranch,
			@ThingworxServiceParameter(name = "UseProxy", description = "", baseType = "BOOLEAN", aspects = { "defaultValue:false" }) Boolean UseProxy,
			@ThingworxServiceParameter(name = "ProxyURL", description = "", baseType = "STRING") String ProxyURL,
			@ThingworxServiceParameter(name = "ProxyPort", description = "", baseType = "INTEGER", aspects = {
					"defaultValue:0" }) Integer ProxyPort,
			@ThingworxServiceParameter(name = "LocalizationTokensPrefix", description = "prefix used for exporting Localization Tokens", baseType = "STRING") String LocalizationTokensPrefix,
			@ThingworxServiceParameter(name = "ProjectName", description = "ThingWorx project to sync entities from (optional)", baseType = "STRING") String ProjectName)
			throws Exception {
		if (RepoName == null || GitRepoURL == null) return;
		EntityServices es = new EntityServices();
		es.CreateThing(RepoName, "GitRepository created by user " + GetCurrentUser() + " at " + new java.util.Date(),
				new TagCollection(), "GitBackupTemplate");
		Thing repoThing = (Thing) EntityUtilities.findEntity(RepoName, ThingworxRelationshipTypes.Thing);
		repoThing.processServiceRequest("EnableThing", new ValueCollection());
		repoThing.processServiceRequest("RestartThing", new ValueCollection());
		repoThing = (Thing) EntityUtilities.findEntity(RepoName, ThingworxRelationshipTypes.Thing);

		InfoTable configTable = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.ConfigurationSetting");
		ValueCollection configRow = new ValueCollection();
		configRow.put("FileRepository", new StringPrimitive(orDefault(FileRepo, "GitRepository")));
		configRow.put("GitRepoURL", new StringPrimitive(GitRepoURL));
		configRow.put("RepoPathName", new StringPrimitive(RepoPath));
		configRow.put("BranchName", new StringPrimitive(InitialBranch));
		configRow.put("UseProxy", new BooleanPrimitive(isTrue(UseProxy)));
		configRow.put("ProxyURL", new StringPrimitive(ProxyURL));
		configRow.put("ProxyPort", new IntegerPrimitive(ProxyPort != null ? ProxyPort : 0));
		configRow.put("LocalizationTokensPrefix", new StringPrimitive(LocalizationTokensPrefix));
		if (hasText(ProjectName)) {
			configRow.put("ProjectName", new StringPrimitive(ProjectName));
		}
		configTable.addRow(configRow);

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
			_logger.error("GitBackup Thing " + RepoName + " was created but saving credentials failed: " + e.getMessage());
		}
	}

	@ThingworxServiceDefinition(name = "DeleteGitThing", description = "Deletes a GitBackup Thing involves two operations: 1. Deleting the Thing itself and 2. Deleting the FileRepository subfolder that stored that Git repository.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void DeleteGitThing(
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
		PlatformSubsystem platformSubsystem = (PlatformSubsystem) EntityUtilities.findEntity("PlatformSubsystem",
				ThingworxRelationshipTypes.Subsystem);
		InfoTable extensionList = platformSubsystem.GetExtensionPackageList();

		String[][] extensions = {
			{ "GitBackupExtension", "GitBackupExtension" },
			{ "GitBackupUI", "GitBackupUI" }
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
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "dataShape:GitBackup.GitHeader" })
	public InfoTable GetGitHeaderTabs() throws Exception {
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.GitHeader");
		ThingTemplate gitTemplate = (ThingTemplate) EntityUtilities.findEntity("GitBackupTemplate",
				ThingworxRelationshipTypes.ThingTemplate);
		ValueCollection queryParams = new ValueCollection();
		queryParams.put("maxItems", new IntegerPrimitive(0));
		queryParams.put("nameMask", new StringPrimitive(""));
		queryParams.put("query", new StringPrimitive(""));
		queryParams.put("tags", new StringPrimitive(""));
		InfoTable gitThings = (InfoTable) gitTemplate.processServiceRequest("QueryImplementingThings", queryParams);
		for (int x = 0; x < gitThings.getRowCount(); x++) {
			ValueCollection row = gitThings.getRow(x);
			ValueCollection entry = new ValueCollection();
			entry.put("MashupName", new StringPrimitive("GitBackup.NameTab.Mashup"));
			entry.put("GitThingName", new StringPrimitive(row.getPrimitive("name").getValue().toString()));
			result.addRow(entry);
		}
		return result;
	}

	@ThingworxServiceDefinition(name = "GetGitUserExtensionsProperties", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:GitBackup.UserExtensionProperties" })
	public InfoTable GetGitUserExtensionsProperties() throws Exception {
		User currentUser = UserUtilities.findUser(GetCurrentUser());
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.UserExtensionProperties");
		ValueCollection entry = new ValueCollection();
		try {
			IPrimitiveType propVal = currentUser.getPropertyValue("GitCommitterName");
			if (propVal != null && propVal.getValue() != null) {
				entry.put("GitCommitterName", currentUser.getPropertyValue("GitCommitterName"));
			} else {
				entry.put("GitCommitterName", new StringPrimitive(""));
			}
		} catch (Exception e) {
			entry.put("GitCommitterName", new StringPrimitive(""));
		}
		try {
			IPrimitiveType propVal = currentUser.getPropertyValue("GitCommitterEmail");
			if (propVal != null && propVal.getValue() != null) {
				entry.put("GitCommitterEmail", currentUser.getPropertyValue("GitCommitterEmail"));
			} else {
				entry.put("GitCommitterEmail", new StringPrimitive(""));
			}
		} catch (Exception e) {
			entry.put("GitCommitterEmail", new StringPrimitive(""));
		}
		try {
			IPrimitiveType propVal = currentUser.getPropertyValue("UseGitCommitUserValues");
			if (propVal != null && propVal.getValue() != null) {
				entry.put("UseGitCommitUserValues", currentUser.getPropertyValue("UseGitCommitUserValues"));
			} else {
				entry.put("UseGitCommitUserValues", new BooleanPrimitive(false));
			}
		} catch (Exception e) {
			entry.put("UseGitCommitUserValues", new BooleanPrimitive(false));
		}
		result.addRow(entry);
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
			@ThingworxServiceParameter(name = "tags", description = "", baseType = "TAGS") TagCollection tags)
			throws Exception {
		if (isBlank(project)) throw new Exception("Can not retrieve the entities that are part of the Project. The project name was not specified or it's empty");

		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("SpotlightSearch");
		IServiceProvider projectProvider = (IServiceProvider) EntityUtilities.findEntity(project, ThingworxRelationshipTypes.Project);
		if (projectProvider == null) throw new Exception("Project " + project + " not found");

		java.util.List<String> projectNames = new java.util.ArrayList<>();
		projectNames.add(project);
		if (isTrue(includeDependents)) {
			InfoTable deps = (InfoTable) projectProvider.processServiceRequest("GetAllDependentProjectNames", new ValueCollection());
			for (int i = 0; i < deps.getRowCount(); i++) {
				projectNames.add(deps.getRow(i).getPrimitive("item").getValue().toString());
			}
		}

		for (String pname : projectNames) {
			JSONObject emptyFilter = new JSONObject();
			Searcher searcher = (Searcher) EntityUtilities.findEntity("SearchFunctions",
					ThingworxRelationshipTypes.Resource);
			InfoTable searchResult = searcher.SpotlightSearch(
				"",                           // searchExpression
				tags != null ? tags : new TagCollection(),  // tags
				emptyFilter,                   // types
				emptyFilter,                   // excludedAspects
				emptyFilter,                   // aspects
				emptyFilter,                   // thingTemplates
				emptyFilter,                   // thingShapes
				null,                          // endDate
				null,                          // startDate
				false,                         // searchDescriptions
				false,                         // withPermissions
				"lastModifiedDate",            // sortBy
				false,                         // isAscending
				30000.0,                       // maxItems
				null,                          // maxSearchItems
				pname                          // projectName
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

	@ThingworxServiceDefinition(name = "ImportEntity", description = "This will import an entity in the system.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void ImportEntity(
			@ThingworxServiceParameter(name = "entityPath", description = "relative to the repository", baseType = "STRING") String entityPath,
			@ThingworxServiceParameter(name = "FileRepositoryName", description = "", baseType = "STRING") String FileRepositoryName,
			@ThingworxServiceParameter(name = "ignoreDependencies", description = "If true, strips dependency validation during import", baseType = "BOOLEAN", aspects = { "defaultValue:false" }) Boolean ignoreDependencies)
			throws Exception {
		_logger.warn("Started single entity import.");
		String ignoreDeps = isTrue(ignoreDependencies) ? "&ignoreDependencies=true" : "";

		Thing importTargets = (Thing) EntityUtilities.findEntity("ExtensionImportTargets",
				ThingworxRelationshipTypes.Thing);
		if (importTargets == null)
			throw new Exception("ExtensionImportTargets not configured. Run InitExtensionImportTargets first.");

		InfoTable targetsTable = null;
		if (importTargets.hasProperty("importTargets")) {
			targetsTable = ((InfoTablePrimitive) importTargets.getPropertyValue("importTargets")).getValue();
		}
		if (targetsTable == null || targetsTable.getRowCount() == 0)
			throw new Exception("ExtensionImportTargets not configured. Run InitExtensionImportTargets first.");

		String baseURL = targetsTable.getRow(0).getPrimitive("baseURL").getValue().toString();
		String appKey = targetsTable.getRow(0).getPrimitive("appKey").getValue().toString();

		String urlStr = baseURL + "/Importer?IgnoreBadValueStreamData=false&WithSubsystems=false"
				+ "&overwritePropertyValues=true&purpose=import&usedefaultdataprovider=true" + ignoreDeps;

		FileRepositoryThing fileRepo = (FileRepositoryThing) EntityUtilities.findEntity(FileRepositoryName,
				ThingworxRelationshipTypes.Thing);
		String fullPath = new File(fileRepo.getRootPath(), entityPath).getPath();
		byte[] fileBytes = Files.readAllBytes(new File(fullPath).toPath());
		_logger.warn("Read file from: " + fullPath + " (" + fileBytes.length + " bytes)");

		String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
		String header = "--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"file\"; filename=\"import.xml\"\r\n"
				+ "Content-Type: text/xml\r\n\r\n";
		String footer = "\r\n--" + boundary + "--\r\n";

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(header.getBytes(StandardCharsets.UTF_8));
		baos.write(fileBytes);
		baos.write(footer.getBytes(StandardCharsets.UTF_8));

		HttpURLConnection conn = (HttpURLConnection) new URI(urlStr).toURL().openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		conn.setRequestProperty("Authorization", "Bearer " + appKey);
		conn.setDoOutput(true);
		conn.setConnectTimeout(30000);
		conn.setReadTimeout(30000);
		conn.getOutputStream().write(baos.toByteArray());
		int responseCode = conn.getResponseCode();
		String responseBody = responseCode >= 200 && responseCode < 300
				? new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
				: new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
		conn.disconnect();
		_logger.warn("ImportEntity response: " + responseCode + " " + responseBody);
	}

	@ThingworxServiceDefinition(name = "ImportProjectEntities", description = "Bulk imports all entity XML files from a FileRepository path. Returns a summary INFOTABLE with success/failure per entity.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:GitBackup.TestResult" })
	public InfoTable ImportProjectEntities(
			@ThingworxServiceParameter(name = "GitThingName", description = "GitBackup Thing name whose FileRepository and path to scan", baseType = "STRING") String GitThingName,
			@ThingworxServiceParameter(name = "entityPath", description = "Relative path within the FileRepository to scan for XML files", baseType = "STRING") String entityPath,
			@ThingworxServiceParameter(name = "ignoreDependencies", description = "If true, strips dependency validation during import", baseType = "BOOLEAN", aspects = { "defaultValue:false" }) Boolean ignoreDependencies)
			throws Exception {
		_logger.warn("Started bulk import for GitThing: " + GitThingName);
		if (isBlank(GitThingName))
			throw new Exception("GitThingName is required.");

		Thing repoThing = (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
		ValueCollection getConfigParams = new ValueCollection();
		getConfigParams.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
		String str_FileRepositoryName = cfgTable.getRow(0).getPrimitive("FileRepository").getValue().toString();

		// Normalize entityPath: strip wildcards, leading slashes, empty
		String str_RepoPath = entityPath;
		if (isBlank(str_RepoPath) || str_RepoPath.equals("*")) {
			str_RepoPath = cfgTable.getRow(0).getPrimitive("RepoPathName").getValue().toString();
		}
		while (str_RepoPath.startsWith("/")) str_RepoPath = str_RepoPath.substring(1);
		while (str_RepoPath.endsWith("/")) str_RepoPath = str_RepoPath.substring(0, str_RepoPath.length() - 1);

		Thing importTargets = (Thing) EntityUtilities.findEntity("ExtensionImportTargets",
				ThingworxRelationshipTypes.Thing);
		if (importTargets == null)
			throw new Exception("ExtensionImportTargets not configured. Run InitExtensionImportTargets first.");

		InfoTable targetsTable = null;
		if (importTargets.hasProperty("importTargets")) {
			targetsTable = ((InfoTablePrimitive) importTargets.getPropertyValue("importTargets")).getValue();
		}
		if (targetsTable == null || targetsTable.getRowCount() == 0)
			throw new Exception("ExtensionImportTargets not configured. Run InitExtensionImportTargets first.");

		String baseURL = targetsTable.getRow(0).getPrimitive("baseURL").getValue().toString();
		String appKey = targetsTable.getRow(0).getPrimitive("appKey").getValue().toString();
		String ignoreDeps = isTrue(ignoreDependencies) ? "&ignoreDependencies=true" : "";

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
			// Strip leading slash from path — ListFiles returns paths like "/GitBackup/..."
			String cleanPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
			ValueCollection entry = new ValueCollection();
			entry.put("testName", new StringPrimitive(cleanPath));
			entry.put("startTimestamp", new DatetimePrimitive(new DateTime(System.currentTimeMillis())));
			try {
				String urlStr = baseURL + "/Importer?IgnoreBadValueStreamData=false&WithSubsystems=false"
						+ "&overwritePropertyValues=true&purpose=import&usedefaultdataprovider=true" + ignoreDeps;

				// Read file directly from disk
				FileRepositoryThing frThing = (FileRepositoryThing) EntityUtilities.findEntity(
						str_FileRepositoryName, ThingworxRelationshipTypes.Thing);
				String fullPath = new File(frThing.getRootPath(), cleanPath).getPath();
				byte[] fileBytes = Files.readAllBytes(new File(fullPath).toPath());

				String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
				String header = "--" + boundary + "\r\n"
						+ "Content-Disposition: form-data; name=\"file\"; filename=\"import.xml\"\r\n"
						+ "Content-Type: text/xml\r\n\r\n";
				String footer = "\r\n--" + boundary + "--\r\n";
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				baos.write(header.getBytes(StandardCharsets.UTF_8));
				baos.write(fileBytes);
				baos.write(footer.getBytes(StandardCharsets.UTF_8));

				HttpURLConnection conn = (HttpURLConnection) new URI(urlStr).toURL().openConnection();
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
				conn.setRequestProperty("Authorization", "Bearer " + appKey);
				conn.setDoOutput(true);
				conn.setConnectTimeout(30000);
				conn.setReadTimeout(30000);
				conn.getOutputStream().write(baos.toByteArray());
				int responseCode = conn.getResponseCode();
				String responseBody = responseCode >= 200 && responseCode < 300
						? new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
						: new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
				conn.disconnect();

				entry.put("passed", new BooleanPrimitive(responseCode >= 200 && responseCode < 300));
				entry.put("comments", new StringPrimitive("HTTP " + responseCode + ": " + responseBody));
				if (responseCode >= 200 && responseCode < 300) {
					int_SuccessCount++;
					_logger.warn("Successfully imported: " + cleanPath);
				} else {
					int_FailCount++;
					_logger.error("Failed to import: " + cleanPath + "; HTTP " + responseCode + ": " + responseBody);
				}
			} catch (Exception ex) {
				entry.put("passed", new BooleanPrimitive(false));
				entry.put("comments", new StringPrimitive("Import failed: " + ex.getMessage()));
				int_FailCount++;
				_logger.error("Failed to import: " + cleanPath + "; Error: " + ex.getMessage());
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
		if (isBlank(GitThingName)) return false;
		Thing repoThing = (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
		ValueCollection getConfigParams = new ValueCollection();
		getConfigParams.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
		String gitRepoURL = primitiveString(cfgTable.getRow(0), "GitRepoURL");
		if (isBlank(gitRepoURL)) return false;

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
		if (importTargets == null) {
			es.CreateThing("ExtensionImportTargets",
					"GitBackup extension import targets; auto-created by InitExtensionImportTargets",
					new TagCollection(), "GitBackupTemplate");
			importTargets = (Thing) EntityUtilities.findEntity("ExtensionImportTargets",
					ThingworxRelationshipTypes.Thing);
			importTargets.processServiceRequest("EnableThing", new ValueCollection());
			importTargets.processServiceRequest("RestartThing", new ValueCollection());
		}
		InfoTable currentTargets = null;
		if (importTargets.hasProperty("importTargets")) {
			currentTargets = ((InfoTablePrimitive) importTargets.getPropertyValue("importTargets")).getValue();
		}
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
		ApplicationKey appKeyThing = (ApplicationKey) EntityUtilities.findEntity("GitExtensionAppKey",
				ThingworxRelationshipTypes.ApplicationKey);
		String keyId = appKeyThing.GetKeyID();
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
		// Ensure the property value is initialized on the current user.
		// After AddPropertyDefinition + RestartDependenciesForThingShape, the user
		// entity may not yet have the property available for setPropertyValue.
		// Initialize it with an empty InfoTable so subsequent reads/writes work.
		try {
			Object val = user.getPropertyValue("GitCredentials");
			if (val == null) {
				InfoTable empty = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.GitCredentials");
				user.setPropertyValue("GitCredentials", new InfoTablePrimitive(empty));
			}
		} catch (Exception e) {
			_logger.warn("Could not initialize GitCredentials property value for user " + str_CurrentUser + ": " + e.getMessage());
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
			Thread.sleep((long) (delay * 1000));
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
			@ThingworxServiceParameter(name = "LocalizationTokensPrefix", description = "", baseType = "STRING") String LocalizationTokensPrefix,
			@ThingworxServiceParameter(name = "ProjectName", description = "ThingWorx project to sync entities from (optional)", baseType = "STRING") String ProjectName)
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
		if (hasText(InitialBranch)) row.put("BranchName", new StringPrimitive(InitialBranch));
		if (UseProxy != null) row.put("UseProxy", new BooleanPrimitive(UseProxy));
		if (ProxyURL != null) row.put("ProxyURL", new StringPrimitive(ProxyURL));
		if (ProxyPort != null) row.put("ProxyPort", new IntegerPrimitive(ProxyPort));
		if (LocalizationTokensPrefix != null) row.put("LocalizationTokensPrefix", new StringPrimitive(LocalizationTokensPrefix));
		if (ProjectName != null) row.put("ProjectName", new StringPrimitive(ProjectName));

		InfoTable newCfg = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.ConfigurationSetting");
		newCfg.addRow(row);
		ValueCollection setConfigParams = new ValueCollection();
		setConfigParams.put("configurationTable", new InfoTablePrimitive(newCfg));
		setConfigParams.put("persistent", new BooleanPrimitive(false));
		setConfigParams.put("tableName", new StringPrimitive("Configuration"));
		repoThing.processServiceRequest("SetConfigurationTable", setConfigParams);
		repoThing.processServiceRequest("SaveConfigurationTables", new ValueCollection());
		repoThing.processServiceRequest("RestartThing", new ValueCollection());

		try {
			SetGitCredentials(User, Password, CommitEmail, CommitUser, RepoName);
		} catch (Exception e) {
			_logger.error("Git Thing configuration was updated but saving credentials failed: " + e.getMessage());
		}
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
		InitUserExtensionProperties();
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
		try {
			currentUser.setPropertyValue("GitCredentials", new InfoTablePrimitive(creds));
		} catch (Exception e) {
			_logger.error("Failed to save GitCredentials for user " + GetCurrentUser() + ": " + e.getMessage());
			throw e;
		}
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

	@ThingworxServiceDefinition(name = "ExportProjectEntities", description = "Wrapper for the ExportToSourceControl service.\nBE WARNED: this service will clean ALL your project files with the lastModifiedDate and PersistanceProvider. This will happen regardless of what files you actually changed.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void ExportProjectEntities(
			@ThingworxServiceParameter(name = "ProjectName", description = "", baseType = "STRING") String ProjectName,
			@ThingworxServiceParameter(name = "includeDependents", description = "", baseType = "BOOLEAN") Boolean includeDependents,
			@ThingworxServiceParameter(name = "EntitiesToExport", description = "Optional; if not set all project entities will be exported", baseType = "INFOTABLE", aspects = { "dataShape:SpotlightSearch" }) InfoTable EntitiesToExport,
			@ThingworxServiceParameter(name = "commitMessage", description = "Optional commit message. If provided, a git commit and push will be performed after export.", baseType = "STRING") String commitMessage)
			throws Exception {
		_logger.warn("Starting ExportProjectEntities for project: " + ProjectName);
		if (isBlank(ProjectName)) {
			throw new Exception("ProjectName is required for ExportProjectEntities.");
		}

		// 1. Read this GitThing's configuration to get FileRepository and repo path
		Thing repoThing = resolveCallingThing();
		ValueCollection getConfigParams = new ValueCollection();
		getConfigParams.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
		String str_FileRepoName = primitiveString(cfgTable.getRow(0), "FileRepository");
		String str_RepoPath = primitiveString(cfgTable.getRow(0), "RepoPathName");
		_logger.warn("ExportProjectEntities: FileRepository=" + str_FileRepoName + ", RepoPath=" + str_RepoPath);

		// 2. Call SourceControlFunctions.ExportSourceControlledEntities
		Object scfObj = EntityUtilities.findEntity("SourceControlFunctions", ThingworxRelationshipTypes.Resource);
		if (scfObj == null) {
			throw new Exception("SourceControlFunctions resource not found. Cannot export entities.");
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
			_logger.warn("Selective export: exporting " + EntitiesToExport.getRowCount()
					+ " entities to temp path: " + str_TempPath);
			ValueCollection params = new ValueCollection();
			params.put("repositoryName", new StringPrimitive(str_FileRepoName));
			params.put("path", new StringPrimitive(str_TempPath));
			params.put("projectName", new StringPrimitive(ProjectName));
			params.put("includeDependents", new BooleanPrimitive(bool_IncludeDeps));
			scf.processServiceRequest("ExportSourceControlledEntities", params);

			// Move each selected entity's XML from temp to actual repo path
			Thing fileRepoThing = (Thing) EntityUtilities.findEntity(str_FileRepoName,
					ThingworxRelationshipTypes.Thing);
			for (int i = 0; i < EntitiesToExport.getRowCount(); i++) {
				String str_EntityName = primitiveString(EntitiesToExport.getRow(i), "name");
				String str_EntityType = orDefault(primitiveString(EntitiesToExport.getRow(i), "type"), "");
				String str_TypeFolder = mapEntityTypeToCollectionFolder(str_EntityType);
				String str_Source = str_TempPath + "/" + ProjectName + "/" + str_TypeFolder + "/"
						+ str_EntityName + ".xml";
				String str_Target = str_RepoPath + "/" + ProjectName + "/" + str_TypeFolder + "/"
						+ str_EntityName + ".xml";

				ValueCollection moveParams = new ValueCollection();
				moveParams.put("sourcePath", new StringPrimitive(str_Source));
				moveParams.put("targetPath", new StringPrimitive(str_Target));
				moveParams.put("overwrite", new BooleanPrimitive(true));
				try {
					fileRepoThing.processServiceRequest("MoveFile", moveParams);
					_logger.warn("Moved entity file: " + str_EntityName);
				} catch (Exception e) {
					_logger.warn("Could not move entity file " + str_Source + ": " + e.getMessage());
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
			FileRepositoryThing fileRepo = (FileRepositoryThing) EntityUtilities.findEntity(str_FileRepoName,
					ThingworxRelationshipTypes.Thing);
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

	@ThingworxServiceDefinition(name = "SyncProjectToRepository", description = "Exports entities from the configured project to the FileRepository. Syncs the Git working tree to reflect live ThingWorx state.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void SyncProjectToRepository(
			@ThingworxServiceParameter(name = "GitThingName", description = "GitBackup Thing name to sync", baseType = "STRING") String GitThingName)
			throws Exception {
		if (isBlank(GitThingName)) return;

		Thing repoThing = (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
		ValueCollection getConfigParams = new ValueCollection();
		getConfigParams.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
		String str_FileRepoName = primitiveString(cfgTable.getRow(0), "FileRepository");
		String str_RepoPath = primitiveString(cfgTable.getRow(0), "RepoPathName");
		String str_ProjectName = primitiveString(cfgTable.getRow(0), "ProjectName");

		if (isBlank(str_ProjectName)) {
			_logger.warn("SyncProjectToRepository: No project configured for " + GitThingName + ", skipping sync.");
			return;
		}

		String str_TempPath = str_RepoPath + "/_sync_temp_" + System.currentTimeMillis();
		_logger.warn("SyncProjectToRepository: Exporting " + str_ProjectName + " to temp path: " + str_TempPath);

		Object scfObj = EntityUtilities.findEntity("SourceControlFunctions", ThingworxRelationshipTypes.Resource);
		if (scfObj == null) {
			throw new Exception("SourceControlFunctions resource not found.");
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

		FileRepositoryThing fileRepo = (FileRepositoryThing) EntityUtilities.findEntity(str_FileRepoName,
				ThingworxRelationshipTypes.Thing);
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
			String rel = str_TempProjectFullPath.length() + 1 < f.getAbsolutePath().length()
					? f.getAbsolutePath().substring(str_TempProjectFullPath.length() + 1)
					: f.getName();
			tempRelativePaths.add(rel);
		}

		for (File f : tempFiles) {
			String rel = str_TempProjectFullPath.length() + 1 < f.getAbsolutePath().length()
					? f.getAbsolutePath().substring(str_TempProjectFullPath.length() + 1)
					: f.getName();
			File targetFile = new File(str_ProjectFullPath, rel);
			targetFile.getParentFile().mkdirs();
			Files.copy(f.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}

		for (File f : repoFiles) {
			String rel = str_ProjectFullPath.length() + 1 < f.getAbsolutePath().length()
					? f.getAbsolutePath().substring(str_ProjectFullPath.length() + 1)
					: f.getName();
			if (!tempRelativePaths.contains(rel)) {
				try {
					Files.delete(f.toPath());
					_logger.warn("SyncProjectToRepository: Removed deleted entity file: " + rel);
				} catch (Exception e) {
					_logger.warn("SyncProjectToRepository: Could not delete " + f.getAbsolutePath() + ": " + e.getMessage());
				}
			}
		}

		if (repoProjectDir.exists()) {
			try {
				Files.walk(repoProjectDir.toPath())
					.sorted((p1, p2) -> -p1.compareTo(p2))
					.filter(p -> p.toFile().isDirectory())
					.forEach(p -> {
						try {
							if (p.toFile().listFiles() == null || p.toFile().listFiles().length == 0) {
								Files.delete(p);
							}
						} catch (IOException e) {}
					});
			} catch (Exception e) {
				_logger.warn("SyncProjectToRepository: Could not clean empty dirs: " + e.getMessage());
			}
		}

		try {
			ValueCollection deleteParams = new ValueCollection();
			deleteParams.put("path", new StringPrimitive(str_TempPath));
			fileRepo.processServiceRequest("DeleteFolder", deleteParams);
		} catch (Exception e) {
			_logger.warn("SyncProjectToRepository: Could not delete temp folder: " + str_TempPath);
		}

		_logger.warn("SyncProjectToRepository completed for " + GitThingName
				+ ": " + tempFiles.size() + " files synced, " + repoFiles.size() + " existing files reconciled.");
	}

	@ThingworxServiceDefinition(name = "SetProjectName", description = "Updates the ProjectName field on a GitBackup Thing's Configuration table.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void SetProjectName(
			@ThingworxServiceParameter(name = "GitThingName", description = "GitBackup Thing name", baseType = "STRING") String GitThingName,
			@ThingworxServiceParameter(name = "ProjectName", description = "ThingWorx project name", baseType = "STRING") String ProjectName)
			throws Exception {
		if (isBlank(GitThingName)) return;
		Thing repoThing = (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
		ValueCollection getConfigParams = new ValueCollection();
		getConfigParams.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
		ValueCollection row = cfgTable.getRow(0);
		row.put("ProjectName", new StringPrimitive(orDefault(ProjectName, "")));
		InfoTable newCfg = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.ConfigurationSetting");
		newCfg.addRow(row);
		ValueCollection setConfigParams = new ValueCollection();
		setConfigParams.put("configurationTable", new InfoTablePrimitive(newCfg));
		setConfigParams.put("persistent", new BooleanPrimitive(false));
		setConfigParams.put("tableName", new StringPrimitive("Configuration"));
		repoThing.processServiceRequest("SetConfigurationTable", setConfigParams);
		repoThing.processServiceRequest("SaveConfigurationTables", new ValueCollection());
		repoThing.processServiceRequest("RestartThing", new ValueCollection());
		_logger.warn("SetProjectName: Updated project name to '" + ProjectName + "' for " + GitThingName);
	}

	@ThingworxServiceDefinition(name = "ExportProjectExtensions", description = "wrapper for Extensions Export / ExportExtensionsToRepository", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void ExportProjectExtensions() throws Exception {
		_logger.warn("ExportProjectExtensions not yet implemented in Java. Falling back to script if available.");
	}

	@ThingworxServiceDefinition(name = "GetConfiguration", description = "This service wrapper allows getting the GitBackup Thing configuration without passing the Configuration table name.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:GitBackup.Configuration" })
	public InfoTable GetConfiguration(
			@ThingworxServiceParameter(name = "GitThingName", description = "GitBackup Thing name", baseType = "STRING") String GitThingName)
			throws Exception {
		if (isBlank(GitThingName)) {
			_logger.warn("GetConfiguration: GitThingName is required.");
			return InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.Configuration");
		}
		Thing repoThing = (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
		ValueCollection getConfigParams = new ValueCollection();
		getConfigParams.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
		ValueCollection cfgRow = cfgTable.getRow(0);
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.Configuration");
		ValueCollection row = new ValueCollection();
		row.put("FileRepository", cfgRow.getPrimitive("FileRepository"));
		row.put("FileRepoPath", cfgRow.getPrimitive("RepoPathName"));
		row.put("GitRepoURL", cfgRow.getPrimitive("GitRepoURL"));
		row.put("InitialBranch", cfgRow.getPrimitive("BranchName"));
		row.put("UseProxy", cfgRow.getPrimitive("UseProxy") != null ? cfgRow.getPrimitive("UseProxy") : new BooleanPrimitive(false));
		row.put("ProxyURL", cfgRow.getPrimitive("ProxyURL"));
		row.put("ProxyPort", cfgRow.getPrimitive("ProxyPort"));
		row.put("LocalizationTokensPrefix", cfgRow.getPrimitive("LocalizationTokensPrefix"));
		result.addRow(row);
		return result;
	}

	@ThingworxServiceDefinition(name = "GetRepoConfiguration", description = "Returns the FileRepository and RepoPath for a given GitThing.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:GitBackup.Configuration" })
	public InfoTable GetRepoConfiguration(
			@ThingworxServiceParameter(name = "GitThingName", description = "", baseType = "STRING") String GitThingName)
			throws Exception {
		Thing repoThing = (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
		ValueCollection getConfigParams = new ValueCollection();
		getConfigParams.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
		String fileRepo = primitiveString(cfgTable.getRow(0), "FileRepository");
		String repoPath = primitiveString(cfgTable.getRow(0), "RepoPathName");
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.Configuration");
		ValueCollection row = new ValueCollection();
		row.put("FileRepository", new StringPrimitive(fileRepo));
		row.put("FileRepoPath", new StringPrimitive(repoPath));
		result.addRow(row);
		return result;
	}

	@ThingworxServiceDefinition(name = "GetFilteredDirectoryListing", description = "Gets recursively the directories found in a subfolder in a FileRepository. Not part of the services of a FileRepository", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:FileSystemDirectory" })
	public InfoTable GetFilteredDirectoryListing() throws Exception {
		Thing repoThing = resolveCallingThing();
		ValueCollection getConfigParams = new ValueCollection();
		getConfigParams.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
		String str_FileRepoName = cfgTable.getRow(0).getPrimitive("FileRepository").getValue().toString();
		String str_RepoPath = cfgTable.getRow(0).getPrimitive("RepoPathName").getValue().toString();
		Thing fileRepo = (Thing) EntityUtilities.findEntity(str_FileRepoName, ThingworxRelationshipTypes.Thing);

		InfoTable raw = (InfoTable) fileRepo.processServiceRequest("ListDirectories",
				new ValueCollection() {{ put("path", new StringPrimitive(str_RepoPath)); put("nameMask", new StringPrimitive("")); }});
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("FileSystemDirectory");
		for (int i = 0; i < raw.getRowCount(); i++) {
			String name = primitiveString(raw.getRow(i), "name");
			String p = primitiveString(raw.getRow(i), "path");
			if (isBlank(name) || isBlank(p)) continue;
			ValueCollection row = new ValueCollection();
			row.put("name", new StringPrimitive(name));
			row.put("path", new StringPrimitive(p));
			result.addRow(row);
		}
		_logger.warn("GetFilteredDirectoryListing found " + result.getRowCount() + " directories (filtered from " + raw.getRowCount() + ").");
		return result;
	}

	@ThingworxServiceDefinition(name = "GetRecursiveFileListing", description = "Recursively lists all XML files in the repository path for a GitThing.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:FileSystemFile" })
	public InfoTable GetRecursiveFileListing(
			@ThingworxServiceParameter(name = "GitThingName", description = "", baseType = "STRING") String GitThingName)
			throws Exception {
		Thing repoThing = (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
		ValueCollection getConfigParams = new ValueCollection();
		getConfigParams.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
		String str_FileRepositoryName = cfgTable.getRow(0).getPrimitive("FileRepository").getValue().toString();
		String str_RepoPath = cfgTable.getRow(0).getPrimitive("RepoPathName").getValue().toString();
		Thing fileRepo = (Thing) EntityUtilities.findEntity(str_FileRepositoryName, ThingworxRelationshipTypes.Thing);
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("FileSystemFile");
		collectXmlFiles(fileRepo, str_RepoPath, result);
		_logger.warn("GetRecursiveFileListing found " + result.getRowCount() + " XML files.");
		return result;
	}

	@ThingworxServiceDefinition(name = "GetLocalBranches", description = "overload of GetBranchList", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:Git.BranchList" })
	public InfoTable GetLocalBranches(
			@ThingworxServiceParameter(name = "GitThingName", description = "GitBackup Thing name", baseType = "STRING") String GitThingName)
			throws Exception {
		if (isBlank(GitThingName)) {
			_logger.warn("GetLocalBranches: GitThingName is required.");
			return InfoTableInstanceFactory.createInfoTableFromDataShape("Git.BranchList");
		}
		Thing repoThing = (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
		InfoTable allBranches = (InfoTable) repoThing.processServiceRequest("GetBranchList", new ValueCollection());
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("Git.BranchList");
		for (int i = 0; i < allBranches.getRowCount(); i++) {
			ValueCollection row = allBranches.getRow(i);
			String branchType = primitiveString(row, "BranchType");
			if ("LOCAL".equals(branchType)) {
				result.addRow(row);
			}
		}
		return result;
	}

	@ThingworxServiceDefinition(name = "CreateBranch", description = "Creates a new local branch from an optional start point (commit, branch, or tag) without switching to it.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "STRING", aspects = {})
	public String CreateBranch(
			@ThingworxServiceParameter(name = "GitThingName", description = "GitBackup Thing name", baseType = "STRING") String GitThingName,
			@ThingworxServiceParameter(name = "BranchName", description = "Name of the new branch", baseType = "STRING") String BranchName,
			@ThingworxServiceParameter(name = "StartPoint", description = "Optional: commit hash, branch name, or tag to branch from (defaults to HEAD)", baseType = "STRING") String StartPoint)
			throws Exception {
		if (isBlank(GitThingName)) {
			_logger.warn("CreateBranch: GitThingName is required.");
			return "";
		}
		Thing repoThing = (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
		ValueCollection params = new ValueCollection();
		params.put("BranchName", new StringPrimitive(BranchName));
		if (hasText(StartPoint)) {
			params.put("StartPoint", new StringPrimitive(StartPoint));
		}
		InfoTable resultTable = (InfoTable) repoThing.processServiceRequest("CreateBranch", params);
		String result = resultTable.getRow(0).getPrimitive("result").getValue().toString();
		_logger.info("CreateBranch: Created branch " + BranchName + " on " + GitThingName + ": " + result);
		return result;
	}

	@ThingworxServiceDefinition(name = "QueryDiffFileList", description = "", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:GitBackup.CommitChangedFiles" })
	public InfoTable QueryDiffFileList(
			@ThingworxServiceParameter(name = "GitThingName", description = "GitBackup Thing name", baseType = "STRING") String GitThingName,
			@ThingworxServiceParameter(name = "CommitID", description = "", baseType = "STRING") String CommitID,
			@ThingworxServiceParameter(name = "FileName", description = "", baseType = "STRING") String FileName)
			throws Exception {
		if (isBlank(GitThingName) || isBlank(CommitID)) {
			_logger.warn("QueryDiffFileList: GitThingName and CommitID are required.");
			return InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.CommitChangedFiles");
		}
		Thing repoThing = (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
		ValueCollection params = new ValueCollection();
		params.put("CommitID", new StringPrimitive(CommitID));
		InfoTable commitInfo = (InfoTable) repoThing.processServiceRequest("GetCommitInfo", params);
		if (commitInfo.getRowCount() > 0) {
			IPrimitiveType changedFilesPrimitive = commitInfo.getRow(0).getPrimitive("ChangedFiles");
			if (changedFilesPrimitive instanceof InfoTablePrimitive) {
				InfoTable changedFiles = ((InfoTablePrimitive) changedFilesPrimitive).getValue();
				if (hasText(FileName)) {
					InfoTable filtered = InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.CommitChangedFiles");
					for (int i = 0; i < changedFiles.getRowCount(); i++) {
						String fname = primitiveString(changedFiles.getRow(i), "FileName");
						if (fname != null && fname.contains(FileName)) {
							filtered.addRow(changedFiles.getRow(i));
						}
					}
					return filtered;
				}
				return changedFiles;
			}
		}
		return InfoTableInstanceFactory.createInfoTableFromDataShape("GitBackup.CommitChangedFiles");
	}

	@ThingworxServiceDefinition(name = "QueryStatus", description = "Filters through the result of the Status Git command. Search is implemented in ThingWorx for flexibility.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = { "isEntityDataShape:true", "dataShape:Git.Status" })
	public InfoTable QueryStatus(
			@ThingworxServiceParameter(name = "GitThingName", description = "GitBackup Thing name", baseType = "STRING") String GitThingName,
			@ThingworxServiceParameter(name = "SearchTerm", description = "", baseType = "STRING") String SearchTerm)
			throws Exception {
		if (isBlank(GitThingName)) {
			_logger.warn("QueryStatus: GitThingName is required.");
			return InfoTableInstanceFactory.createInfoTableFromDataShape("Git.Status");
		}
		Thing repoThing = (Thing) EntityUtilities.findEntity(GitThingName, ThingworxRelationshipTypes.Thing);
		InfoTable status = (InfoTable) repoThing.processServiceRequest("Status", new ValueCollection());
		if (hasText(SearchTerm)) {
			InfoTable filtered = InfoTableInstanceFactory.createInfoTableFromDataShape("Git.Status");
			for (int i = 0; i < status.getRowCount(); i++) {
				ValueCollection row = status.getRow(i);
				String file = primitiveString(row, "File");
				String stat = primitiveString(row, "Status");
				if ((file != null && file.toLowerCase().contains(SearchTerm.toLowerCase()))
						|| (stat != null && stat.toLowerCase().contains(SearchTerm.toLowerCase()))) {
					filtered.addRow(row);
				}
			}
			return filtered;
		}
		return status;
	}

	@ThingworxServiceDefinition(name = "RemoveConfigurationTableDefinitions", description = "Removes the ConfigurationTableDefinitions to allow compatibility with 8.2. This service should be used only if you're doing crossplatform development between 8.5 and 8.2/8.3. Enable or disable in the ExportProjectEntities services", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void RemoveConfigurationTableDefinitions() throws Exception {
		_logger.warn("RemoveConfigurationTableDefinitions not yet implemented in Java. Falling back to script if available.");
	}

	@ThingworxServiceDefinition(name = "RemoveLastModifiedDate", description = "Removes the lastModifiedDate. Change history is already removed by ExportToSourceControlEntities", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void RemoveLastModifiedDate() throws Exception {
		_logger.warn("RemoveLastModifiedDate called as service.");
		Thing repoThing = resolveCallingThing();
		ValueCollection getConfigParams = new ValueCollection();
		getConfigParams.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
		String str_FileRepoName = cfgTable.getRow(0).getPrimitive("FileRepository").getValue().toString();
		String str_RepoPath = cfgTable.getRow(0).getPrimitive("RepoPathName").getValue().toString();
		removeLastModifiedDate(str_FileRepoName, str_RepoPath, null);
	}

	@ThingworxServiceDefinition(name = "RemoveMashupPreviewTag", description = "Removes the mashup preview tag to allow compatibility with 8.2. This service should be used only if you're doing crossplatform development between 8.5 and 8.2/8.3", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void RemoveMashupPreviewTag() throws Exception {
		_logger.warn("RemoveMashupPreviewTag not yet implemented in Java. Falling back to script if available.");
	}

	@ThingworxServiceDefinition(name = "RemoveModelPersistenceProviderPackage", description = "Removes the modelPersistenceProviderPackage.", category = "", isAllowOverride = false, aspects = { "isAsync:false" })
	@ThingworxServiceResult(name = "result", description = "", baseType = "NOTHING", aspects = {})
	public void RemoveModelPersistenceProviderPackage() throws Exception {
		_logger.warn("RemoveModelPersistenceProviderPackage called as service.");
		Thing repoThing = resolveCallingThing();
		ValueCollection getConfigParams = new ValueCollection();
		getConfigParams.put("tableName", new StringPrimitive("Configuration"));
		InfoTable cfgTable = (InfoTable) repoThing.processServiceRequest("GetConfigurationTable", getConfigParams);
		String str_FileRepoName = cfgTable.getRow(0).getPrimitive("FileRepository").getValue().toString();
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

	private void removeLastModifiedDate(String fileRepoName, String repoPath, String projectName) throws IOException {
		FileRepositoryThing fileRepo = (FileRepositoryThing) EntityUtilities.findEntity(fileRepoName,
				ThingworxRelationshipTypes.Thing);
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

	private void removeModelPersistenceProviderPackage(String fileRepoName, String repoPath, String projectName) throws IOException {
		FileRepositoryThing fileRepo = (FileRepositoryThing) EntityUtilities.findEntity(fileRepoName,
				ThingworxRelationshipTypes.Thing);
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
			String modified = content.replaceAll("\\s+modelPersistenceProviderPackage=\"[^\"]*\"", "");
			if (!modified.equals(content)) {
				Files.write(f.toPath(), modified.getBytes(StandardCharsets.UTF_8));
				count++;
			}
		}
		_logger.warn("removeModelPersistenceProviderPackage: cleaned " + count + " files in " + fullPath);
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
			_logger.warn("resolveCallingThing: getMeContext=" + meObj
					+ " type=" + (meObj != null ? meObj.getClass().getName() : "null"));
			if (meObj instanceof Thing) {
				Thing t = (Thing) meObj;
				_logger.warn("resolveCallingThing: meContext Thing name=" + t.getName());
				return t;
			}
			if (meObj instanceof String) {
				String name = (String) meObj;
				_logger.warn("resolveCallingThing: meContext is String=" + name);
				Thing t = (Thing) EntityUtilities.findEntity(name, ThingworxRelationshipTypes.Thing);
				if (t != null) return t;
			}
		} catch (Exception e) {
			_logger.warn("resolveCallingThing: getMeContext failed: " + e.getMessage());
		}
		try {
			String name = this.getName();
			_logger.warn("resolveCallingThing: this.getName()=" + name);
			if (hasText(name)) {
				Thing thing = (Thing) EntityUtilities.findEntity(name, ThingworxRelationshipTypes.Thing);
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
		throw new Exception("Could not resolve calling GitThing. Ensure the service is invoked on a GitBackup Thing.");
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
