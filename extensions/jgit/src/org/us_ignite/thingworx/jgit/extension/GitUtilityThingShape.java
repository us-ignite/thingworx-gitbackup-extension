package org.us_ignite.thingworx.jgit.extension;

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
import com.thingworx.resources.queries.Searcher;
import com.thingworx.security.users.User;
import com.thingworx.things.Thing;
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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
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
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.GpgKeySpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.GpgKeyVerificationSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.RepositoryConfigurationSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.RepositoryListSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.StringResultSpec;

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
            @ThingworxServiceParameter(name = "RepoPath", description = "", baseType = "STRING")
                    String RepoPath,
            @ThingworxServiceParameter(name = "BranchName", description = "", baseType = "STRING")
                    String BranchName,
            @ThingworxServiceParameter(name = "ProjectName", description = "", baseType = "STRING")
                    String ProjectName,
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
        try {
            if (RepoName == null || RepoName.isBlank())
                throw new IllegalArgumentException("RepoName is required.");
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
            if (ProjectName != null
                    && !ProjectName.isBlank()
                    && EntityUtilities.findEntity(ProjectName, ThingworxRelationshipTypes.Project)
                            == null) {
                es.CreateProject(
                        ProjectName,
                        "Component",
                        "Project for entity synchronization via the JGit extension",
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
                    Const.GitRepoURL, new StringPrimitive(GitRepoURL == null ? "" : GitRepoURL));
            repoThing.setPropertyValue(
                    Const.RepoPath, new StringPrimitive(RepoPath == null ? "" : RepoPath));
            repoThing.setPropertyValue(
                    Const.InitialBranch,
                    new StringPrimitive(BranchName == null ? "main" : BranchName));
            repoThing.setPropertyValue(
                    Const.ProjectName, new StringPrimitive(ProjectName == null ? "" : ProjectName));
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
            GitCredentialCreate(
                    GitCommitterUser,
                    GitCommitterPassword,
                    GitCommitterEmail,
                    GitCommitterFullName,
                    RepoName,
                    "");
            return DapResults.success(
                    "RepositoryCreate", "Repository created.", StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("RepositoryCreate", e, StringResultContract.SERVICE_RESULT);
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
            }
            _logger.warn(
                    "GIT Repository Thing "
                            + RepoName
                            + " (self-hosted FileRepository) was deleted successfully.");
            return DapResults.success(
                    "RepositoryDelete", "Repository deleted.", StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("RepositoryDelete", e, StringResultContract.SERVICE_RESULT);
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
            ThingShape userExtensions =
                    (ThingShape)
                            EntityUtilities.findEntity(
                                    "UserExtensions", ThingworxRelationshipTypes.ThingShape);

            String CurrentUser = UserUtilities.getCurrentUser();
            User user = UserUtilities.findUser(CurrentUser);

            boolean hasGitCredentials = false;
            if (user != null) {
                hasGitCredentials = user.getPropertyValue("UserRepositoryConfiguration") != null;
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

                String[] oldProps = {
                    "UseGitCommitUserValues",
                    "GitCommitterEmail",
                    "GitCommitterPassword",
                    "GitCommitterName"
                };
                for (String prop : oldProps) {
                    Object val = user.getPropertyValue(prop);
                    if (val != null) {
                        ValueCollection removeParams = new ValueCollection();
                        removeParams.put("name", new StringPrimitive(prop));
                        userExtensions.processServiceRequest(
                                "RemovePropertyDefinition", removeParams);
                        new EntityServices().RestartDependenciesForThingShape("UserExtensions");
                    }
                }
            }

            boolean hasUserGpgKeys = false;
            if (user != null) {
                hasUserGpgKeys = user.getPropertyValue(Const.UserGpgKeys) != null;
            }
            if (!hasUserGpgKeys) {
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
            return DapResults.success(
                    "InitUserExtensionProperties",
                    "User extension properties initialized.",
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure(
                    "InitUserExtensionProperties", e, StringResultContract.SERVICE_RESULT);
        }
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
                        String keyForVerify = storedKey;
                        if (keyForVerify != null && !keyForVerify.startsWith("-----")) {
                            try {
                                keyForVerify =
                                        new String(
                                                Base64.getDecoder().decode(keyForVerify),
                                                StandardCharsets.UTF_8);
                            } catch (IllegalArgumentException iae) {
                                // keep original value; signer will report invalid
                            }
                        }
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
                    String keyForVerify = storedKey;
                    if (keyForVerify != null && !keyForVerify.startsWith("-----")) {
                        try {
                            keyForVerify =
                                    new String(
                                            Base64.getDecoder().decode(keyForVerify),
                                            StandardCharsets.UTF_8);
                        } catch (IllegalArgumentException iae) {
                            // keep original value; signer will report invalid
                        }
                    }
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
                String key = GpgPrivateKey;
                String passphrase = GpgKeyPassphrase;
                if (key == null || key.isBlank()) {
                    throw new IllegalArgumentException(
                            "GpgPrivateKey is required when All is false and no GpgKeyFingerprint/GpgKeyLabel is supplied.");
                }
                if (key != null && !key.startsWith("-----")) {
                    try {
                        key = new String(Base64.getDecoder().decode(key), StandardCharsets.UTF_8);
                    } catch (IllegalArgumentException iae) {
                        // keep original value; signer will report invalid
                    }
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
            InfoTable gpgKeys = getGpgKeysTable(currentUser);
            if (gpgKeys == null)
                gpgKeys =
                        InfoTableInstanceFactory.createInfoTableFromDataShape(
                                Const.UserGpgKeyDataShapeName);
            return DapResults.success(
                    "GpgKeyList", GpgKeyTable.SERVICE_RESULT, GpgKeyTable.wrap(gpgKeys));
        } catch (Exception e) {
            _logger.error("GpgKeyList failed", e);
            return DapResults.failure("GpgKeyList", e, GpgKeyTable.SERVICE_RESULT);
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
                            description = "GPG key fingerprint for display",
                            baseType = "STRING")
                    String GpgKeyFingerprint,
            @ThingworxServiceParameter(
                            name = "GpgKeyLabel",
                            description = "Optional human-readable label for this key",
                            baseType = "STRING")
                    String GpgKeyLabel) {
        try {
            User currentUser = new GitUserContextManager().requireUser();
            if (GpgKeyFingerprint.isBlank()) {
                if (GpgPrivateKey.isBlank())
                    throw new IllegalArgumentException(
                            "GpgPrivateKey or GpgKeyFingerprint is required.");
                PastedKeyGpgSigner signer = new PastedKeyGpgSigner(GpgPrivateKey, GpgKeyPassphrase);
                try {
                    GpgKeyFingerprint = signer.getFingerprint();
                } finally {
                    signer.clearSensitiveData();
                }
            }
            InitUserExtensionProperties();
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
                    "GpgKeyGet", GpgKeyTable.SERVICE_RESULT, GpgKeyTable.wrap(result));
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
                    row.put(Const.GpgPrivateKey, new PasswordPrimitive(privateKey));
                    row.put(Const.GpgKeyPassphrase, new PasswordPrimitive(passphrase));
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
                    "GpgKeyDelete", message, GpgKeyTable.SERVICE_RESULT, GpgKeyTable.wrap(deleted));
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
            InitUserExtensionProperties();
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
            entry.put("GitCommitterEmail", new StringPrimitive(GitCommitterEmail));
            entry.put("GitCommitterFullName", new StringPrimitive(GitCommitterFullName));
            entry.put("GitThing", new StringPrimitive(GitThing));
            if (!GpgKeyFingerprint.isBlank())
                entry.put(Const.GpgKeyFingerprint, new StringPrimitive(GpgKeyFingerprint));
            creds.addRow(entry);
            currentUser.setPropertyValue(
                    "UserRepositoryConfiguration", new InfoTablePrimitive(creds));
            return DapResults.success(
                    "GitCredentialCreate",
                    "Git credentials created.",
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure(
                    "GitCredentialCreate", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(RepositoryConfigurationSpec.class)
    @ThingworxServiceDefinition(
            name = "GitCredentialList",
            description = "Lists the current user's repository credentials",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.RepositoryConfiguration.ServiceResult.DataShape"
            })
    public InfoTable GitCredentialList() {
        try {
            InfoTable result = new GitUserContextManager().credentials();
            if (result == null)
                result =
                        InfoTableInstanceFactory.createInfoTableFromDataShape(
                                Const.GitCredentialsDataShapeName);
            return DapResults.success(
                    "GitCredentialList",
                    RepositoryConfigurationTable.SERVICE_RESULT,
                    RepositoryConfigurationTable.wrap(result));
        } catch (Exception e) {
            _logger.error("GitCredentialList failed", e);
            return DapResults.failure(
                    "GitCredentialList", e, RepositoryConfigurationTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(RepositoryConfigurationSpec.class)
    @ThingworxServiceDefinition(
            name = "GitCredentialGet",
            description = "Returns credentials for one repository",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.RepositoryConfiguration.ServiceResult.DataShape"
            })
    public InfoTable GitCredentialGet(
            @ThingworxServiceParameter(
                            name = "GitThing",
                            description = "Repository Thing",
                            baseType = "THINGNAME")
                    String gitThing) {
        try {
            InfoTable result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.GitCredentialsDataShapeName);
            InfoTable all = new GitUserContextManager().credentials();
            if (all != null)
                for (int i = 0; i < all.getRowCount(); i++)
                    if (gitThing != null
                            && gitThing.equals(all.getRow(i).getStringValue("GitThing")))
                        result.addRow(all.getRow(i));
            if (result.getRowCount() == 0)
                throw new IllegalArgumentException(
                        "Credentials not found for repository: " + gitThing);
            return DapResults.success(
                    "GitCredentialGet",
                    RepositoryConfigurationTable.SERVICE_RESULT,
                    RepositoryConfigurationTable.wrap(result));
        } catch (Exception e) {
            return DapResults.failure(
                    "GitCredentialGet", e, RepositoryConfigurationTable.SERVICE_RESULT);
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
                    row.put("GitCommitterEmail", new StringPrimitive(email));
                    row.put("GitCommitterFullName", new StringPrimitive(fullName));
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

    private void validateGpgKeyOwnership(User user, String fingerprint) throws Exception {
        new GitUserContextManager().validateGpgKeyOwnership(fingerprint);
    }
}
