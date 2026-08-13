package org.us_ignite.thingworx.jgit.extension;

import static org.us_ignite.thingworx.jgit.extension.Values.hasText;
import static org.us_ignite.thingworx.jgit.extension.Values.isBlank;
import static org.us_ignite.thingworx.jgit.extension.Values.primitiveString;

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
import com.thingworx.types.primitives.IPrimitiveType;
import com.thingworx.types.primitives.PasswordPrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Utility services for repository lifecycle, entity synchronization, user configuration, and
 * extension logging.
 *
 * <p>Services on this Thing operate on the calling ThingWorx user where credentials and GPG keys
 * are involved. Protected user properties must not be exported or logged as plain text.
 */
@ThingworxBaseTemplateDefinition(name = "GenericThing")
public class GitUtilityThingShape extends Thing {

    private static final long serialVersionUID = 9085129963750550674L;
    private static Logger _logger =
            LogUtilities.getInstance().getApplicationLogger(GitUtilityThingShape.class);

    public GitUtilityThingShape() {}

    /** Creates the repository/FileRepository Thing and installs the repository shape. */
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
            aspects = {"isEntityDataShape:true", "dataShape:GIT.OperationResult.DataShape"})
    public InfoTable RepositoryCreate(
            @ThingworxServiceParameter(name = "RepoName", description = "", baseType = "STRING")
                    String RepoName,
            @ThingworxServiceParameter(name = "GitRepoURL", description = "", baseType = "STRING")
                    String GitRepoURL,
            @ThingworxServiceParameter(name = "RepoPathName", description = "", baseType = "STRING")
                    String RepoPathName,
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
        if (isBlank(RepoName)) throw new IllegalArgumentException("RepoName is required.");
        EntityServices es = new EntityServices();
        String repositoryProject = "GIT.Repositories";
        if (EntityUtilities.findEntity(repositoryProject, ThingworxRelationshipTypes.Project)
                == null) {
            es.CreateProject(
                    repositoryProject,
                    "Component",
                    "Repository Things created by the JGit extension",
                    "",
                    new TagCollection());
        }
        if (hasText(ProjectName)
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
                "FileRepository");
        // Keep the repository as a single Thing: the FileRepository supplies the
        // working-tree storage and the Git shape supplies the version-control services.
        es.AddShapeToThing(RepoName, "GIT.Repository.ThingShape");
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
                Const.str_GitRepoURL, new StringPrimitive(GitRepoURL == null ? "" : GitRepoURL));
        repoThing.setPropertyValue(
                Const.str_RepoPathName,
                new StringPrimitive(RepoPathName == null ? "" : RepoPathName));
        repoThing.setPropertyValue(
                Const.str_InitialBranch,
                new StringPrimitive(BranchName == null ? "main" : BranchName));
        repoThing.setPropertyValue(
                Const.str_ProjectName, new StringPrimitive(ProjectName == null ? "" : ProjectName));
        repoThing.setPropertyValue(
                Const.str_UseProxy, new BooleanPrimitive(UseProxy != null && UseProxy));
        repoThing.setPropertyValue(
                Const.str_ProxyURL, new StringPrimitive(ProxyURL == null ? "" : ProxyURL));
        IntegerPrimitive proxyPort = new IntegerPrimitive();
        proxyPort.setValue(ProxyPort == null ? 0 : ProxyPort);
        repoThing.setPropertyValue(Const.str_ProxyPort, proxyPort);
        repoThing.setPropertyValue(
                Const.str_LocalizationTokensPrefix,
                new StringPrimitive(
                        LocalizationTokensPrefix == null ? "" : LocalizationTokensPrefix));
        GitCredentialCreate(
                GitCommitterUser,
                GitCommitterPassword,
                GitCommitterEmail,
                GitCommitterFullName,
                RepoName,
                "");
        return ServiceResults.operation("Repository created.");
        } catch (Exception e) {
            return ServiceResults.fromError("RepositoryCreate", e, "RepositoryCreate Error: " + e.getMessage());
        }
    }

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
            aspects = {"isEntityDataShape:true", "dataShape:GIT.RepositoryList.ServiceResult.DataShape"})
    public InfoTable RepositoryList() {
        try {
            InfoTable result = ServiceResults.emptyPayload("GIT.RepositoryList.DataShape");
            Searcher searcher =
                    (Searcher)
                            EntityUtilities.findEntity(
                                    "SearchFunctions", ThingworxRelationshipTypes.Resource);
            if (searcher == null)
                return ServiceResults.payload("GIT.RepositoryList.ServiceResult.DataShape", result, "");
            JSONObject empty = new JSONObject();
            InfoTable found =
                    searcher.SpotlightSearch(
                            "", new TagCollection(), empty, empty, empty, empty, empty, null, null,
                            false, false, "name", true, 30000.0, null, "GIT.Repositories", false);
            for (int i = 0; i < found.getRowCount(); i++) {
                ValueCollection row = found.getRow(i);
                ValueCollection out = new ValueCollection();
                out.put("RepoName", new StringPrimitive(primitiveString(row, "name")));
                result.addRow(out);
            }
            return ServiceResults.payload("GIT.RepositoryList.ServiceResult.DataShape", result, "");
        } catch (Exception e) {
            _logger.error("RepositoryList failed", e);
            return ServiceResults.payloadFailure(
                    "GIT.RepositoryList.ServiceResult.DataShape", "RepositoryList Error: " + e.getMessage());
        }
    }

    @ThingworxServiceDefinition(
            name = "RepositoryDelete",
            description =
                    "Deletes a GIT Repository Thing involves two operations: 1. Deleting the Thing itself and 2. Deleting the FileRepository subfolder that stored that Git repository.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.OperationResult.DataShape"})
    public InfoTable RepositoryDelete(
            @ThingworxServiceParameter(name = "RepoName", description = "", baseType = "STRING")
                    String RepoName) {
        try {
        Thing repoThing =
                (Thing) EntityUtilities.findEntity(RepoName, ThingworxRelationshipTypes.Thing);
        String str_RepositoryPathName;
        try {
            Object value = repoThing.getPropertyValue(Const.str_RepoPathName);
            Object raw = value instanceof IPrimitiveType ? ((IPrimitiveType<?, ?>) value).getValue() : value;
            str_RepositoryPathName = raw == null || isBlank(raw.toString()) ? "" : raw.toString();
        } catch (Exception e) {
            str_RepositoryPathName = "";
        }
        FileRepositoryThing fileRepo = (FileRepositoryThing) repoThing;

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
            ValueCollection deleteFolderParams = new ValueCollection();
            deleteFolderParams.put("path", new StringPrimitive(str_RepositoryPathName));
            fileRepo.processServiceRequest("DeleteFolder", deleteFolderParams);
        } catch (Exception ex) {
            _logger.error("Deleting Git Repository folder failed when deleting Thing " + RepoName);
        }
        String str_CurrentUser = UserUtilities.getCurrentUser();
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
                    user.setPropertyValue(
                            "UserRepositoryConfiguration", new InfoTablePrimitive(creds));
                }
            }
        } catch (Exception ex) {
            _logger.warn("Could not clean up credentials for deleted thing: " + RepoName);
        }
        _logger.warn(
                "GIT Repository Thing "
                        + RepoName
                        + " (self-hosted FileRepository) was deleted successfully.");
        return ServiceResults.operation("Repository deleted.");
        } catch (Exception e) {
            return ServiceResults.fromError("RepositoryDelete", e, "RepositoryDelete Error: " + e.getMessage());
        }
    }

    /** Initializes the per-user extension properties required by utility services. */
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
            aspects = {"isEntityDataShape:true", "dataShape:GIT.OperationResult.DataShape"})
    public InfoTable InitUserExtensionProperties() {
        try {
        ThingShape userExtensions =
                (ThingShape)
                        EntityUtilities.findEntity(
                                "UserExtensions", ThingworxRelationshipTypes.ThingShape);

        User user = null;
        String str_CurrentUser = null;
        try {
            str_CurrentUser = UserUtilities.getCurrentUser();
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
            addPropParams.put(
                    "dataShape",
                    new StringPrimitive("GIT.RepositoryConfiguration.UserExtension.DataShape"));
            userExtensions.processServiceRequest("AddPropertyDefinition", addPropParams);
            new EntityServices().RestartDependenciesForThingShape("UserExtensions");
        }

        if (user != null) {
            try {
                Object val = user.getPropertyValue("UserRepositoryConfiguration");
                if (val == null) {
                    InfoTable empty =
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
                            for (int i = 0; i < table.getRowCount(); i++) empty.addRow(table.getRow(i));
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
                                for (int j = 0; j < empty.getRowCount(); j++) {
                                    if (gitThing.equals(primitiveString(empty.getRow(j), "GitThing"))) {
                                        target = empty.getRow(j);
                                        break;
                                    }
                                }
                                if (target == null) {
                                    target = new ValueCollection();
                                    target.put("GitThing", source.getPrimitive("GitThing"));
                                    empty.addRow(target);
                                }
                                if (!isBlank(fingerprint))
                                    target.put(Const.str_GpgKeyFingerprint, new StringPrimitive(fingerprint));
                            }
                            user.setPropertyValue(Const.str_UserGpgKeys, new InfoTablePrimitive(keyStore));
                        }
                    }
                    user.setPropertyValue(
                            "UserRepositoryConfiguration", new InfoTablePrimitive(empty));
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
                    "description",
                    new StringPrimitive("User-owned GPG keys reusable across repositories."));
            addPropParams.put("readOnly", new BooleanPrimitive(false));
            addPropParams.put("type", new StringPrimitive("INFOTABLE"));
            addPropParams.put("name", new StringPrimitive(Const.str_UserGpgKeys));
            addPropParams.put("persistent", new BooleanPrimitive(true));
            addPropParams.put("dataShape", new StringPrimitive(Const.str_UserGpgKeyDataShapeName));
            userExtensions.processServiceRequest("AddPropertyDefinition", addPropParams);
            new EntityServices().RestartDependenciesForThingShape("UserExtensions");
        }
        if (user != null && user.getPropertyValue(Const.str_UserGpgKeys) == null) {
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
                        if (fingerprint.equals(primitiveString(keys.getRow(j), Const.str_GpgKeyFingerprint))) {
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
            user.setPropertyValue(Const.str_UserGpgKeys, new InfoTablePrimitive(keys));
        }
        return ServiceResults.operation("User extension properties initialized.");
        } catch (Exception e) {
            return ServiceResults.fromError(
                    "InitUserExtensionProperties", e, "InitUserExtensionProperties Error: " + e.getMessage());
        }
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
                    String GpgKeyPassphrase) {
        InfoTable result = ServiceResults.emptyPayload(Const.str_GpgKeyVerificationResultDataShapeName);
        try {
            String key = GpgPrivateKey;
            String passphrase = GpgKeyPassphrase;
            if (isBlank(key)) {
                User currentUser = requireCurrentUser();
                InfoTable storedKeys = getGpgKeysTable(currentUser);
                ValueCollection stored =
                        storedKeys == null || storedKeys.getRowCount() == 0
                                ? null
                                : storedKeys.getRow(0);
                if (stored != null) {
                    key =
                            ((PasswordPrimitive) stored.getPrimitive(Const.str_GpgPrivateKey))
                                    .getValue();
                    passphrase =
                            ((PasswordPrimitive) stored.getPrimitive(Const.str_GpgKeyPassphrase))
                                    .getValue();
                }
            }
            if (key != null && !key.startsWith("-----")) {
                try {
                    key = new String(Base64.getDecoder().decode(key), StandardCharsets.UTF_8);
                } catch (IllegalArgumentException ignored) {
                    // The supplied value is not base64 encoded.
                }
            }
            PastedKeyGpgSigner signer = new PastedKeyGpgSigner(key, passphrase);
            String fingerprint = signer.getFingerprint();
            ValueCollection row = new ValueCollection();
            row.put("GitThing", new StringPrimitive(""));
            row.put(
                    "GpgKeyFingerprint",
                    new StringPrimitive(
                            fingerprint != null ? fingerprint : "Unable to derive fingerprint"));
            result.addRow(row);
            signer.clearSensitiveData();
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            _logger.error(errors.toString());
            return ServiceResults.fromError(
                    "GIT.GpgKeyVerification.ServiceResult.DataShape",
                    "VerifyGpgKey",
                    e,
                    "Verification error: " + e.getMessage());
        }
        return ServiceResults.payload("GIT.GpgKeyVerification.ServiceResult.DataShape", result, "");
    }

    /** Returns the current user’s configured GPG key metadata. */
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
            User currentUser = requireCurrentUser();
            InfoTable gpgKeys = getGpgKeysTable(currentUser);
            if (gpgKeys == null) gpgKeys = ServiceResults.emptyPayload(Const.str_UserGpgKeyDataShapeName);
            return ServiceResults.payload("GIT.GpgKey.ServiceResult.DataShape", gpgKeys, "");
        } catch (Exception e) {
            _logger.error("GpgKeyList failed", e);
            return ServiceResults.payloadFailure(
                    "GIT.GpgKey.ServiceResult.DataShape", "GpgKeyList Error: " + e.getMessage());
        }
    }

    /** Creates a reusable GPG key for the current user. */
    @ThingworxServiceDefinition(
            name = "GpgKeyCreate",
            description = "Adds a reusable GPG key for the current user, keyed by fingerprint",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.OperationResult.DataShape"})
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
                    String GpgKeyFingerprint) {
        try {
        User currentUser = requireCurrentUser();
        if (isBlank(GpgKeyFingerprint)) {
            if (isBlank(GpgPrivateKey))
                throw new IllegalArgumentException(
                        "GpgPrivateKey or GpgKeyFingerprint is required.");
            PastedKeyGpgSigner signer = new PastedKeyGpgSigner(GpgPrivateKey, GpgKeyPassphrase);
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
            gpgKeys =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.str_UserGpgKeyDataShapeName);
        else {
            InfoTable cloned =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.str_UserGpgKeyDataShapeName);
            for (int i = 0; i < gpgKeys.getRowCount(); i++) {
                ValueCollection row = gpgKeys.getRow(i);
                if (GpgKeyFingerprint.equals(primitiveString(row, Const.str_GpgKeyFingerprint)))
                    throw new IllegalArgumentException(
                            "A GPG key with this fingerprint already exists.");
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
        return ServiceResults.operation("GPG key created.");
        } catch (Exception e) {
            return ServiceResults.fromError("GpgKeyCreate", e, "GpgKeyCreate Error: " + e.getMessage());
        }
    }

    @ThingworxServiceDefinition(
            name = "GpgKeyGet",
            description = "Returns one owned GPG key by fingerprint",
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
                    String fingerprint) {
        try {
        User user = requireCurrentUser();
        InfoTable result =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.str_UserGpgKeyDataShapeName);
        InfoTable keys = getGpgKeysTable(user);
        if (keys != null)
            for (int i = 0; i < keys.getRowCount(); i++)
                if (fingerprint != null
                        && fingerprint.equals(
                                primitiveString(keys.getRow(i), Const.str_GpgKeyFingerprint)))
                    result.addRow(keys.getRow(i));
        if (result.getRowCount() == 0)
            throw new IllegalArgumentException("GPG key not found: " + fingerprint);
        return ServiceResults.payload("GIT.GpgKey.ServiceResult.DataShape", result, "");
        } catch (Exception e) {
            return ServiceResults.fromError("GIT.GpgKey.ServiceResult.DataShape", "GpgKeyGet", e, "GpgKeyGet Error: " + e.getMessage());
        }
    }

    @ThingworxServiceDefinition(
            name = "GpgKeyUpdate",
            description = "Updates an owned GPG key",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.OperationResult.DataShape"})
    public InfoTable GpgKeyUpdate(
            @ThingworxServiceParameter(
                            name = "GpgKeyFingerprint",
                            description = "Key fingerprint",
                            baseType = "STRING")
                    String fingerprint,
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
        User user = requireCurrentUser();
        InfoTable keys = getGpgKeysTable(user);
        if (keys == null) throw new IllegalArgumentException("GPG key not found: " + fingerprint);
        boolean found = false;
        for (int i = 0; i < keys.getRowCount(); i++)
            if (fingerprint != null
                    && fingerprint.equals(
                            primitiveString(keys.getRow(i), Const.str_GpgKeyFingerprint))) {
                keys.getRow(i).put(Const.str_GpgPrivateKey, new PasswordPrimitive(privateKey));
                keys.getRow(i).put(Const.str_GpgKeyPassphrase, new PasswordPrimitive(passphrase));
                found = true;
            }
        if (!found) throw new IllegalArgumentException("GPG key not found: " + fingerprint);
        user.setPropertyValue(Const.str_UserGpgKeys, new InfoTablePrimitive(keys));
        return ServiceResults.operation("GPG key updated.");
        } catch (Exception e) {
            return ServiceResults.fromError("GpgKeyUpdate", e, "GpgKeyUpdate Error: " + e.getMessage());
        }
    }

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
            aspects = {"isEntityDataShape:true", "dataShape:GIT.OperationResult.DataShape"})
    public InfoTable GpgKeyDelete(
            @ThingworxServiceParameter(
                            name = "GpgKeyFingerprint",
                            description = "Key fingerprint",
                            baseType = "STRING")
                    String GpgKeyFingerprint) {
        try {
        User currentUser = requireCurrentUser();
        InfoTable keys = getGpgKeysTable(currentUser);
        boolean found = false;
        if (keys != null) {
            InfoTable cloned =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.str_UserGpgKeyDataShapeName);
            for (int i = 0; i < keys.getRowCount(); i++) {
                ValueCollection row = keys.getRow(i);
                if (GpgKeyFingerprint.equals(primitiveString(row, Const.str_GpgKeyFingerprint))) {
                    found = true;
                    continue;
                }
                cloned.addRow(row);
            }
            currentUser.setPropertyValue(Const.str_UserGpgKeys, new InfoTablePrimitive(cloned));
        }
        if (!found) throw new IllegalArgumentException("GPG key not found: " + GpgKeyFingerprint);
        InfoTable configurations = getGitCredentials(currentUser);
        if (configurations != null)
            for (int i = 0; i < configurations.getRowCount(); i++)
                if (GpgKeyFingerprint.equals(
                        primitiveString(configurations.getRow(i), Const.str_GpgKeyFingerprint))) {
                    configurations
                            .getRow(i)
                            .put(Const.str_GpgKeyFingerprint, new StringPrimitive(""));
                }
        if (configurations != null)
            currentUser.setPropertyValue(
                    Const.str_UserRepositoryConfiguration, new InfoTablePrimitive(configurations));
        return ServiceResults.operation("GPG key deleted.");
        } catch (Exception e) {
            return ServiceResults.fromError("GpgKeyDelete", e, "GpgKeyDelete Error: " + e.getMessage());
        }
    }

    /** Stores Git credentials and committer settings for a repository and user. */
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
            aspects = {"isEntityDataShape:true", "dataShape:GIT.OperationResult.DataShape"})
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
        User currentUser = requireCurrentUser();
        if (hasText(GpgKeyFingerprint)) {
            validateGpgKeyOwnership(currentUser, GpgKeyFingerprint);
        }

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
        if (hasText(GpgKeyFingerprint))
            entry.put(Const.str_GpgKeyFingerprint, new StringPrimitive(GpgKeyFingerprint));
        creds.addRow(entry);
        try {
            currentUser.setPropertyValue(
                    "UserRepositoryConfiguration", new InfoTablePrimitive(creds));
        } catch (Exception e) {
            _logger.error(
                    "Failed to save GitCredentials for user "
                            + UserUtilities.getCurrentUser()
                            + ": "
                            + e.getMessage());
            return ServiceResults.fromError(
                    "GitCredentialCreate", e, "GitCredentialCreate Error: " + e.getMessage());
        }
        return ServiceResults.operation("Git credentials created.");
        } catch (Exception e) {
            return ServiceResults.fromError(
                    "GitCredentialCreate", e, "GitCredentialCreate Error: " + e.getMessage());
        }
    }

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
            User user = requireCurrentUser();
            InfoTable result = getGitCredentials(user);
            if (result == null) result = ServiceResults.emptyPayload(Const.str_GitCredentialsDataShapeName);
            return ServiceResults.payload(
                    "GIT.RepositoryConfiguration.ServiceResult.DataShape", result, "");
        } catch (Exception e) {
            _logger.error("GitCredentialList failed", e);
            return ServiceResults.payloadFailure(
                    "GIT.RepositoryConfiguration.ServiceResult.DataShape",
                    "GitCredentialList Error: " + e.getMessage());
        }
    }

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
                        Const.str_GitCredentialsDataShapeName);
        InfoTable all = getGitCredentials(requireCurrentUser());
        if (all != null)
            for (int i = 0; i < all.getRowCount(); i++)
                if (gitThing != null && gitThing.equals(primitiveString(all.getRow(i), "GitThing")))
                    result.addRow(all.getRow(i));
        if (result.getRowCount() == 0)
            throw new IllegalArgumentException("Credentials not found for repository: " + gitThing);
        return ServiceResults.payload(
                "GIT.RepositoryConfiguration.ServiceResult.DataShape", result, "");
        } catch (Exception e) {
            return ServiceResults.fromError(
                    "GIT.RepositoryConfiguration.ServiceResult.DataShape",
                    "GitCredentialGet",
                    e,
                    "GitCredentialGet Error: " + e.getMessage());
        }
    }

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
            aspects = {"isEntityDataShape:true", "dataShape:GIT.OperationResult.DataShape"})
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
        User current = requireCurrentUser();
        if (hasText(GpgKeyFingerprint)) {
            validateGpgKeyOwnership(current, GpgKeyFingerprint);
        }
        InfoTable all = getGitCredentials(current);
        if (all == null)
            throw new IllegalArgumentException("Credentials not found for repository: " + gitThing);
        boolean found = false;
        for (int i = 0; i < all.getRowCount(); i++)
            if (gitThing != null && gitThing.equals(primitiveString(all.getRow(i), "GitThing"))) {
                ValueCollection row = all.getRow(i);
                row.put("GitCommitterUser", new StringPrimitive(user));
                row.put("GitCommitterPassword", new PasswordPrimitive(password));
                row.put("GitCommitterEmail", new StringPrimitive(email));
                row.put("GitCommitterFullName", new StringPrimitive(fullName));
                if (hasText(GpgKeyFingerprint))
                    row.put(Const.str_GpgKeyFingerprint, new StringPrimitive(GpgKeyFingerprint));
                else row.put(Const.str_GpgKeyFingerprint, new StringPrimitive(""));
                found = true;
            }
        if (!found)
            throw new IllegalArgumentException("Credentials not found for repository: " + gitThing);
        current.setPropertyValue(
                Const.str_UserRepositoryConfiguration, new InfoTablePrimitive(all));
        return ServiceResults.operation("Git credentials updated.");
        } catch (Exception e) {
            return ServiceResults.fromError(
                    "GitCredentialUpdate", e, "GitCredentialUpdate Error: " + e.getMessage());
        }
    }

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
            aspects = {"isEntityDataShape:true", "dataShape:GIT.OperationResult.DataShape"})
    public InfoTable GitCredentialDelete(
            @ThingworxServiceParameter(
                            name = "GitThing",
                            description = "Repository Thing",
                            baseType = "THINGNAME")
                    String gitThing) {
        try {
        User current = requireCurrentUser();
        InfoTable all = getGitCredentials(current);
        if (all == null)
            throw new IllegalArgumentException("Credentials not found for repository: " + gitThing);
        InfoTable kept =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.str_GitCredentialsDataShapeName);
        boolean found = false;
        for (int i = 0; i < all.getRowCount(); i++) {
            ValueCollection row = all.getRow(i);
            if (gitThing != null && gitThing.equals(primitiveString(row, "GitThing"))) found = true;
            else kept.addRow(row);
        }
        if (!found)
            throw new IllegalArgumentException("Credentials not found for repository: " + gitThing);
        current.setPropertyValue(
                Const.str_UserRepositoryConfiguration, new InfoTablePrimitive(kept));
        return ServiceResults.operation("Git credentials deleted.");
        } catch (Exception e) {
            return ServiceResults.fromError(
                    "GitCredentialDelete", e, "GitCredentialDelete Error: " + e.getMessage());
        }
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

    /** Resolve the authenticated User so UserExtension secret values remain user-scoped. */
    private User requireCurrentUser() throws Exception {
        String currentUserName = UserUtilities.getCurrentUser();
        if (currentUserName == null || currentUserName.trim().isEmpty()) {
            throw new IllegalStateException("No authenticated user context is available.");
        }
        User currentUser = UserUtilities.findUser(currentUserName);
        if (currentUser == null) {
            throw new IllegalStateException("Authenticated user was not found: " + currentUserName);
        }
        return currentUser;
    }

    private void validateGpgKeyOwnership(User user, String fingerprint) throws Exception {
        InfoTable keys = getGpgKeysTable(user);
        if (keys == null)
            throw new IllegalArgumentException(
                    "GPG key is not owned by the current user: " + fingerprint);
        for (int i = 0; i < keys.getRowCount(); i++) {
            if (fingerprint.equals(primitiveString(keys.getRow(i), Const.str_GpgKeyFingerprint)))
                return;
        }
        throw new IllegalArgumentException(
                "GPG key is not owned by the current user: " + fingerprint);
    }
}
