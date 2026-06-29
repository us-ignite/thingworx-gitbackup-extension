package gb.extension;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Iterator;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceParameter;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.things.Thing;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.BooleanPrimitive;
import com.thingworx.types.primitives.DatetimePrimitive;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.StringPrimitive;

public class GitUtilityThingShape {

	private static Logger _logger = LogUtilities.getInstance().getApplicationLogger(GitUtilityThingShape.class);

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
}
