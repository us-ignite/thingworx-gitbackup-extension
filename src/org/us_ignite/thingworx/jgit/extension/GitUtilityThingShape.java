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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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
            if (RepoName == null || RepoName.isBlank())
                throw new IllegalArgumentException("RepoName is required.");
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
                    Const.RepoPathName,
                    new StringPrimitive(RepoPathName == null ? "" : RepoPathName));
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
            return ServiceResults.successFromString("RepositoryCreate", "Repository created.");
        } catch (Exception e) {
            return ServiceResults.fromError("RepositoryCreate", e);
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
                return ServiceResults.successFromPayload(
                        "RepositoryList", "GIT.RepositoryList.ServiceResult.DataShape", result);
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
                            "GIT.Repositories",
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
            return ServiceResults.successFromPayload(
                    "RepositoryList", "GIT.RepositoryList.ServiceResult.DataShape", result);
        } catch (Exception e) {
            _logger.error("RepositoryList failed", e);
            return ServiceResults.fromError(
                    "RepositoryList", e, "GIT.RepositoryList.ServiceResult.DataShape");
        }
    }

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
            aspects = {"isEntityDataShape:true", "dataShape:GIT.OperationResult.DataShape"})
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
            return ServiceResults.successFromString("RepositoryDelete", "Repository deleted.");
        } catch (Exception e) {
            return ServiceResults.fromError("RepositoryDelete", e);
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
            return ServiceResults.successFromString(
                    "InitUserExtensionProperties", "User extension properties initialized.");
        } catch (Exception e) {
            return ServiceResults.fromError("InitUserExtensionProperties", e);
        }
    }

    private InfoTable getGpgKeysTable(User user) throws Exception {
        return new GitUserContextManager().gpgKeys();
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
                    String GpgPrivateKey) {
        InfoTable result = null;
        try {
            if (GpgPrivateKey == null || GpgPrivateKey.isBlank())
                throw new IllegalArgumentException("GpgPrivateKey is required.");
            result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.GpgKeyVerificationResultDataShapeName);
            String key = GpgPrivateKey;
            if (!key.startsWith("-----")) {
                key = new String(Base64.getDecoder().decode(key), StandardCharsets.UTF_8);
            }
            PastedKeyGpgSigner signer = new PastedKeyGpgSigner(key, null);
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
                    "VerifyGpgKey", e, "GIT.GpgKeyVerification.ServiceResult.DataShape");
        }
        return ServiceResults.successFromPayload(
                "VerifyGpgKey", "GIT.GpgKeyVerification.ServiceResult.DataShape", result);
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
            User currentUser = new GitUserContextManager().requireUser();
            InfoTable gpgKeys = getGpgKeysTable(currentUser);
            if (gpgKeys == null)
                gpgKeys =
                        InfoTableInstanceFactory.createInfoTableFromDataShape(
                                Const.UserGpgKeyDataShapeName);
            return ServiceResults.successFromPayload(
                    "GpgKeyList", "GIT.GpgKey.ServiceResult.DataShape", gpgKeys);
        } catch (Exception e) {
            _logger.error("GpgKeyList failed", e);
            return ServiceResults.fromError("GpgKeyList", e, "GIT.GpgKey.ServiceResult.DataShape");
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
            return ServiceResults.successFromString("GpgKeyCreate", "GPG key created.");
        } catch (Exception e) {
            return ServiceResults.fromError("GpgKeyCreate", e);
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
                    String fingerprint,
            @ThingworxServiceParameter(
                            name = "GpgKeyLabel",
                            description = "Optional key label",
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
            return ServiceResults.successFromPayload(
                    "GpgKeyGet", "GIT.GpgKey.ServiceResult.DataShape", result);
        } catch (Exception e) {
            return ServiceResults.fromError("GpgKeyGet", e, "GIT.GpgKey.ServiceResult.DataShape");
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
            boolean found = false;
            for (int i = 0; i < keys.getRowCount(); i++) {
                ValueCollection row = keys.getRow(i);
                boolean matches;
                if (fingerprint != null && !fingerprint.isBlank())
                    matches = fingerprint.equals(row.getStringValue(Const.GpgKeyFingerprint));
                else matches = GpgKeyLabel.equals(row.getStringValue(Const.GpgKeyLabel));
                if (matches) {
                    if (GpgKeyLabel != null
                            && !GpgKeyLabel.isBlank()
                            && fingerprint != null
                            && !fingerprint.isBlank())
                        if (!GpgKeyLabel.equals(row.getStringValue(Const.GpgKeyLabel)))
                            for (int j = 0; j < keys.getRowCount(); j++)
                                if (j != i
                                        && GpgKeyLabel.equals(
                                                keys.getRow(j).getStringValue(Const.GpgKeyLabel)))
                                    throw new IllegalArgumentException(
                                            "A GPG key with this label already exists: "
                                                    + GpgKeyLabel);
                    row.put(Const.GpgPrivateKey, new PasswordPrimitive(privateKey));
                    row.put(Const.GpgKeyPassphrase, new PasswordPrimitive(passphrase));
                    if (GpgKeyLabel != null && !GpgKeyLabel.isBlank())
                        row.put(Const.GpgKeyLabel, new StringPrimitive(GpgKeyLabel));
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
            return ServiceResults.successFromString("GpgKeyUpdate", "GPG key updated.");
        } catch (Exception e) {
            return ServiceResults.fromError("GpgKeyUpdate", e);
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
            return ServiceResults.successFromPayload(
                    "GpgKeyDelete", message, "GIT.GpgKey.ServiceResult.DataShape", deleted);
        } catch (Exception e) {
            return ServiceResults.fromError(
                    "GpgKeyDelete", e, "GIT.GpgKey.ServiceResult.DataShape");
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
            return ServiceResults.successFromString(
                    "GitCredentialCreate", "Git credentials created.");
        } catch (Exception e) {
            return ServiceResults.fromError("GitCredentialCreate", e);
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
            InfoTable result = new GitUserContextManager().credentials();
            if (result == null)
                result =
                        InfoTableInstanceFactory.createInfoTableFromDataShape(
                                Const.GitCredentialsDataShapeName);
            return ServiceResults.successFromPayload(
                    "GitCredentialList",
                    "GIT.RepositoryConfiguration.ServiceResult.DataShape",
                    result);
        } catch (Exception e) {
            _logger.error("GitCredentialList failed", e);
            return ServiceResults.fromError(
                    "GitCredentialList", e, "GIT.RepositoryConfiguration.ServiceResult.DataShape");
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
            return ServiceResults.successFromPayload(
                    "GitCredentialGet",
                    "GIT.RepositoryConfiguration.ServiceResult.DataShape",
                    result);
        } catch (Exception e) {
            return ServiceResults.fromError(
                    "GitCredentialGet", e, "GIT.RepositoryConfiguration.ServiceResult.DataShape");
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
            return ServiceResults.successFromString(
                    "GitCredentialUpdate", "Git credentials updated.");
        } catch (Exception e) {
            return ServiceResults.fromError("GitCredentialUpdate", e);
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
            return ServiceResults.successFromString(
                    "GitCredentialDelete", "Git credentials deleted.");
        } catch (Exception e) {
            return ServiceResults.fromError("GitCredentialDelete", e);
        }
    }

    private void validateGpgKeyOwnership(User user, String fingerprint) throws Exception {
        new GitUserContextManager().validateGpgKeyOwnership(fingerprint);
    }
}
