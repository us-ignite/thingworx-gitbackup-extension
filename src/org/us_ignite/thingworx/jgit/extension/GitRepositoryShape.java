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
import com.thingworx.metadata.annotations.ThingworxConfigurationTableDefinition;
import com.thingworx.metadata.annotations.ThingworxConfigurationTableDefinitions;
import com.thingworx.metadata.annotations.ThingworxDataShapeDefinition;
import com.thingworx.metadata.annotations.ThingworxFieldDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceParameter;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.security.users.User;
import com.thingworx.system.ContextType;
import com.thingworx.things.Thing;
import com.thingworx.things.repository.FileRepositoryThing;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.BooleanPrimitive;
import com.thingworx.types.primitives.DatetimePrimitive;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.PasswordPrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import com.thingworx.webservices.context.ThreadLocalContext;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.CheckoutEntry;
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.Signers;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS.FileStoreAttributes;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.joda.time.DateTime;
import org.slf4j.Logger;

@ThingworxBaseTemplateDefinition(name = "GenericThing")
@ThingworxConfigurationTableDefinitions(
        tables = {
            @ThingworxConfigurationTableDefinition(
                    name = Const.str_ConfTableName,
                    description = "",
                    isMultiRow = false,
                    ordinal = 0,
                    dataShape =
                            @ThingworxDataShapeDefinition(
                                    fields = {
                                        @ThingworxFieldDefinition(
                                                name = Const.str_GitRepoURL,
                                                description = "",
                                                baseType = "STRING",
                                                ordinal = 4,
                                                aspects = {"friendlyName:Git Repo URL"}),
                                        @ThingworxFieldDefinition(
                                                name = Const.str_FileRepository,
                                                description = "",
                                                baseType = "THINGNAME",
                                                ordinal = 5,
                                                aspects = {
                                                    "thingTemplate:FileRepository",
                                                    "friendlyName:File Repository"
                                                }),
                                        @ThingworxFieldDefinition(
                                                name = Const.str_RepoPathName,
                                                description = "",
                                                baseType = "STRING",
                                                ordinal = 6,
                                                aspects = {"friendlyName:File Repository Path"}),
                                        @ThingworxFieldDefinition(
                                                name = Const.str_InitialBranch,
                                                description =
                                                        "Must be the main branch setup in the remote Git repository",
                                                baseType = "STRING",
                                                ordinal = 7,
                                                aspects = {
                                                    "friendlyName:Initial branch",
                                                    "defaultValue:main"
                                                }),
                                        @ThingworxFieldDefinition(
                                                name = Const.str_UseProxy,
                                                description = "Should Proxy be used?",
                                                baseType = "BOOLEAN",
                                                ordinal = 8,
                                                aspects = {
                                                    "friendlyName:Use Proxy?",
                                                    "defaultValue:false"
                                                }),
                                        @ThingworxFieldDefinition(
                                                name = Const.str_ProxyURL,
                                                description =
                                                        "The HTTP proxy used for connection to the remote; leave blank if not used ",
                                                baseType = "STRING",
                                                ordinal = 9,
                                                aspects = {"friendlyName:Proxy URL"}),
                                        @ThingworxFieldDefinition(
                                                name = Const.str_ProxyPort,
                                                description = "Proxy Port",
                                                baseType = "INTEGER",
                                                ordinal = 10,
                                                aspects = {
                                                    "friendlyName:Proxy Port",
                                                    "defaultValue:0"
                                                }),
                                        @ThingworxFieldDefinition(
                                                name = Const.str_LocalizationTokensPrefix,
                                                description =
                                                        "Prefix used for exporting Localization tokens",
                                                baseType = "STRING",
                                                ordinal = 10,
                                                aspects = {
                                                    "friendlyName:Localization Tokens Prefix"
                                                }),
                                        @ThingworxFieldDefinition(
                                                name = Const.str_ProjectName,
                                                description =
                                                        "ThingWorx project to sync entities from",
                                                baseType = "STRING",
                                                ordinal = 11,
                                                aspects = {"friendlyName:Project Name"}),

                                        // ,@ThingworxFieldDefinition(name =
                                        // Const.str_DefaultProjectToExport,
                                        // description = "", baseType = "STRING", ordinal = 8,
                                        // aspects = {
                                        // "friendlyName:Default Export Project" })

                                    }))
        })
public class GitRepositoryShape extends Thing {
    /** */
    // set background file system resolution to false and enable debugging
    static {
        FileStoreAttributes.setBackground(false);
    }

    private static final long serialVersionUID = -6500080561143490845L;

    // Complete git path will be calculated by concatenating the SCR absolute
    // path and the relative path
    private String str_GitRepoURL,
            str_FileRepository,
            str_FileRepoPath,
            str_CurrentBranchOrCommit,
            str_ProxyURL,
            str_ProjectName;
    private Integer int_ProxyPort;
    private boolean bool_isDetachedHead = false, bool_UseProxy;
    private Git gitObject;

    private static Logger _logger =
            LogUtilities.getInstance().getApplicationLogger(GitRepositoryShape.class);

    public GitRepositoryShape() {}

    @Override
    protected void stopThing(ContextType ctx) throws Exception {
        gitObject = null;
        super.stopThing(null);
    }

    @Override
    public void dispose() throws Exception {
        _logger.warn("Thing entered dispose phase");
        if (!str_GitRepoURL.equals(Const.str_GitRepoURLDefaultValue)) {
            Thread.sleep(50);
            FileRepositoryThing srcRepo =
                    (FileRepositoryThing)
                            EntityUtilities.findEntity(
                                    str_FileRepository, ThingworxRelationshipTypes.Thing);
            Git myGitObject = getGitObject("dispose");
            Thread.sleep(50);
            Repository myGitRepository = myGitObject.getRepository();
            myGitRepository.close();
            myGitRepository.close();
            myGitRepository.close();
            myGitRepository.close();
            myGitRepository.close();
            Thread.sleep(50);
            myGitObject.close();
            Thread.sleep(50);
            _logger.warn(
                    "Disposing GIT Repository Thing "
                            + getName()
                            + "; RepositoryCache contains: "
                            + RepositoryCache.getRegisteredKeys().size());
            RepositoryCache.clear();
            // System.gc();
            Thread.sleep(200);
            gitObject = null;
            String str_FolderPath = srcRepo.getRootPath();
            deleteGitFolder(new File(str_FolderPath), "dispose");
        }
        super.dispose();
    }

    @Override
    protected void initializeThing(ContextType ctx) throws Exception {

        // Initialize internal fields based on the Configuration Table
        this.str_GitRepoURL =
                ((String) getConfigurationSetting(Const.str_ConfTableName, Const.str_GitRepoURL));
        String fileRepo =
                (String) getConfigurationSetting(Const.str_ConfTableName, Const.str_FileRepository);
        this.str_FileRepository = orDefault(fileRepo, this.getName());
        this.str_FileRepoPath =
                ((String) getConfigurationSetting(Const.str_ConfTableName, Const.str_RepoPathName));
        this.str_CurrentBranchOrCommit =
                ((String)
                        getConfigurationSetting(Const.str_ConfTableName, Const.str_InitialBranch));
        this.bool_UseProxy =
                isTrue(
                        (Boolean)
                                getConfigurationSetting(
                                        Const.str_ConfTableName, Const.str_UseProxy));
        this.str_ProxyURL =
                ((String) getConfigurationSetting(Const.str_ConfTableName, Const.str_ProxyURL));
        this.int_ProxyPort =
                ((Integer) getConfigurationSetting(Const.str_ConfTableName, Const.str_ProxyPort));
        this.str_ProjectName =
                ((String) getConfigurationSetting(Const.str_ConfTableName, Const.str_ProjectName));
        ProxySelector.setDefault(
                new ProxySelector() {
                    @Override
                    public List<Proxy> select(URI uri) {
                        if (bool_UseProxy
                                && uri != null
                                && hasText(str_GitRepoURL)
                                && str_GitRepoURL.contains(uri.getHost())
                                && hasText(str_ProxyURL)
                                && int_ProxyPort != null) {
                            return List.of(
                                    new Proxy(
                                            Type.HTTP,
                                            InetSocketAddress.createUnresolved(
                                                    str_ProxyURL, int_ProxyPort)));
                        }
                        return List.of(Proxy.NO_PROXY);
                    }

                    @Override
                    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                        if (uri == null || sa == null || ioe == null) {
                            throw new IllegalArgumentException("Arguments can't be null.");
                        }
                    }
                });
        WindowCacheConfig wconfig = new WindowCacheConfig();
        wconfig.setPackedGitMMAP(false);
        wconfig.install();
        _logger.warn(
                "1. GIT Repository Thing: "
                        + this.getName()
                        + " initialize phase 1.1. PackedGitMMAP set false");
        // prevents creation of disk folder when no repo URL is configured
        if (hasText(str_GitRepoURL)) {
            _logger.warn(
                    "1. GIT Repository Thing: "
                            + this.getName()
                            + " initialize phase 1.2. GitRepoDefault contains real repository URL.");
            Git Mygit = getGitObject("initializeThing");
            String str_Branch = Mygit.getRepository().getFullBranch();
            bool_isDetachedHead = str_Branch == null || !str_Branch.startsWith("refs/heads/");
            str_CurrentBranchOrCommit =
                    (str_Branch != null)
                            ? Mygit.getRepository().getBranch()
                            : Const.str_InitialBranch;
        }

        // will display the right library version in case we use directly the source
        // code
        String str_JGIT_version =
                org.eclipse.jgit.lib.Repository.class.getPackage().getImplementationVersion();
        str_JGIT_version =
                str_JGIT_version == null
                        ? "6.10.1.202505221210-r; custom source code"
                        : str_JGIT_version;
        _logger.warn(
                "1. GIT Repository Thing: "
                        + this.getName()
                        + " final initialize phase ended. Jgit library version: "
                        + str_JGIT_version);
        super.initializeThing(null);
    }

    @ThingworxServiceDefinition(
            name = "Commit",
            description =
                    "Syncs the ThingWorx project entities to the local working tree, stages all changes, and creates a commit. Does not push to the remote.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String Commit(
            @ThingworxServiceParameter(
                            name = "Message",
                            description = "The commit message",
                            baseType = "STRING")
                    String Message)
            throws Exception, GitAPIException {
        _logger.trace("Entering Service: Commit");
        refreshConfiguration();
        if (!syncFromThingworx()) {
            return "Commit Error: " + Const.ERR_PREFIX_SYSTEM + "Project synchronization failed.";
        }
        String str_CurrentMethodName = "Commit";
        boolean bool_SignCommits = false;
        if (isBlank(str_GitRepoURL)) {
            _logger.warn(Const.ERR_NO_REPO_URL);
            return "Commit Error: " + Const.ERR_PREFIX_CONFIG + Const.ERR_NO_REPO_URL;
        }
        try {
            Thread.sleep(1000);
            Git myGitObject = getGitObject("Commit");
            User us_currentUser = UserUtilities.findUser(UserUtilities.getCurrentUser());
            ValueCollection vc_RepoCredentials = getGitRepoRemoteCredential(us_currentUser);
            String str_CommitterName =
                    primitiveString(vc_RepoCredentials, Const.str_GitCommitterName);
            String str_CommitterEmail =
                    primitiveString(vc_RepoCredentials, Const.str_GitCommitterEmail);
            if (isBlank(str_CommitterName) || isBlank(str_CommitterEmail)) {
                _logger.warn(Const.ERR_NO_COMMITTER);
                return "Commit Error: " + Const.ERR_PREFIX_CONFIG + Const.ERR_NO_COMMITTER;
            }
            myGitObject.add().addFilepattern(".").call();
            myGitObject.add().addFilepattern(".").setUpdate(true).call();
            var commitCmd =
                    myGitObject
                            .commit()
                            .setAll(true)
                            .setMessage(Message)
                            .setCommitter(str_CommitterName, str_CommitterEmail);

            String str_GpgPrivateKey = null;
            String str_GpgPassphrase = null;
            try {
                ValueCollection vc_GpgKey = getUserGpgKey(us_currentUser);
                if (vc_GpgKey != null && vc_GpgKey.getPrimitive(Const.str_SignCommits) != null) {
                    str_GpgPrivateKey =
                            ((PasswordPrimitive) vc_GpgKey.getPrimitive(Const.str_GpgPrivateKey))
                                    .getValue();
                    str_GpgPassphrase =
                            ((PasswordPrimitive) vc_GpgKey.getPrimitive(Const.str_GpgKeyPassphrase))
                                    .getValue();
                    bool_SignCommits =
                            ((BooleanPrimitive) vc_GpgKey.getPrimitive(Const.str_SignCommits))
                                    .getValue();
                }
            } catch (Exception e) {
                _logger.warn(Const.WARN_NO_GPG_KEYS);
            }

            PastedKeyGpgSigner gpgSigner = null;
            if (bool_SignCommits && hasText(str_GpgPrivateKey)) {
                gpgSigner = new PastedKeyGpgSigner(str_GpgPrivateKey, str_GpgPassphrase);
                Signers.set(GpgConfig.GpgFormat.OPENPGP, gpgSigner);
                commitCmd.setSign(true).setSigningKey(null).setSigner(gpgSigner);
                commitCmd.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(null, str_GpgPassphrase));
            }

            commitCmd.call();

            if (gpgSigner != null) {
                gpgSigner.clearSensitiveData();
            }

            String str_Result = String.format(Const.SUCCESS_COMMIT, Message);
            _logger.warn(str_Result);
            LogOperationResult(str_Result, str_CurrentMethodName);
            return str_Result;
        } catch (Exception e) {
            String errMsg = buildErrorResult("Commit", e);
            LogOperationResult(errMsg, str_CurrentMethodName);
            return errMsg;
        }
    }

    @ThingworxServiceDefinition(
            name = "Push",
            description =
                    "Pushes existing local commits to a remote. This service does not synchronize entities, stage files, or create commits. The remote defaults to origin.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String Push(
            @ThingworxServiceParameter(
                            name = "Remote",
                            description = "Optional remote name; defaults to origin",
                            baseType = "STRING")
                    String Remote)
            throws Exception, GitAPIException {
        _logger.trace("Entering Service: Push");
        refreshConfiguration();
        String str_CurrentMethodName = "Push";
        if (isBlank(str_GitRepoURL)) {
            _logger.warn(Const.ERR_NO_REPO_URL);
            return "Push Error: " + Const.ERR_PREFIX_CONFIG + Const.ERR_NO_REPO_URL_PUSH;
        }
        try {
            long startTimePush = System.nanoTime();
            Git myGitObject = getGitObject("Push");
            long endTimeOpenRepository = System.nanoTime();
            BigDecimal durationTimeOpenRepository =
                    new BigDecimal(
                                    (double) (endTimeOpenRepository - startTimePush)
                                            / (double) 1000000)
                            .setScale(3, RoundingMode.HALF_DOWN);
            User us_currentUser = UserUtilities.findUser(UserUtilities.getCurrentUser());
            ValueCollection vc_RepoCredentials = getGitRepoRemoteCredential(us_currentUser);
            String str_User = primitiveString(vc_RepoCredentials, Const.str_GitCommitterUser);
            String str_Password =
                    primitiveString(vc_RepoCredentials, Const.str_GitCommitterPassword);
            if (isBlank(str_User) || isBlank(str_Password)) {
                _logger.warn(Const.ERR_NO_CREDENTIALS);
                return "Push Error: " + Const.ERR_PREFIX_AUTH + Const.ERR_NO_CREDENTIALS;
            }
            String remote = isBlank(Remote) ? "origin" : Remote;
            CredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(str_User, str_Password);
            Iterable<PushResult> prList =
                    myGitObject
                            .push()
                            .setRemote(remote)
                            .setCredentialsProvider(credentialsProvider)
                            .call();
            long endTimePushFinish = System.nanoTime();
            BigDecimal durationTimePushFinish =
                    new BigDecimal(
                                    (double) (endTimePushFinish - endTimeOpenRepository)
                                            / (double) 1000000)
                            .setScale(3, RoundingMode.HALF_DOWN);
            String str_LogResult = "";
            String pushError = null;
            for (PushResult pr : prList) {
                for (RemoteRefUpdate update : pr.getRemoteUpdates()) {
                    str_LogResult += update;
                    RemoteRefUpdate.Status status = update.getStatus();
                    if (status != RemoteRefUpdate.Status.OK
                            && status != RemoteRefUpdate.Status.UP_TO_DATE) {
                        String hint;
                        if (status == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD) {
                            hint = " " + Const.ERR_PUSH_NON_FAST_FORWARD;
                        } else {
                            hint = " Check the remote repository policy and credentials.";
                        }
                        pushError =
                                "Remote rejected "
                                        + update.getRemoteName()
                                        + " with status "
                                        + status
                                        + "."
                                        + hint;
                    }
                }
            }
            str_LogResult +=
                    " Debug Timings (ms): #1.OpenGit: "
                            + durationTimeOpenRepository
                            + "#2.Push: "
                            + durationTimePushFinish;
            Thread.sleep(2000);
            LogOperationResult(str_LogResult, str_CurrentMethodName);
            if (pushError != null) return "Push Error: " + Const.ERR_PREFIX_GIT + pushError;
            return str_LogResult;
        } catch (Exception e) {
            String errMsg;
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String fullTrace = errors.toString();
            _logger.error(fullTrace);
            if (fullTrace.contains("pre-receive hook declined")
                    || fullTrace.contains("REJECTED_OTHER_REASON")) {
                errMsg = "Push rejected by remote server (pre-receive hook).";
            } else {
                errMsg = buildErrorResult("Push", e);
            }
            LogOperationResult(errMsg, str_CurrentMethodName);
            return errMsg;
        }
    }

    @ThingworxServiceDefinition(
            name = "VerifyGpgKey",
            description =
                    "Verifies a pasted PGP private key can be loaded and used for signing. Returns the key fingerprint on success.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.GpgKeyVerificationResult.DataShape"
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
                    String GpgKeyPassphrase)
            throws Exception {
        String str_CurrentMethodName = "VerifyGpgKey";
        InfoTable iftbl_Result =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.str_GpgKeyVerificationResultDataShapeName);
        try {
            // if no key was passed, try to read from stored GpgKeys property
            String resolvedKey = GpgPrivateKey;
            String resolvedPassphrase = GpgKeyPassphrase;
            if (GpgPrivateKey == null || GpgPrivateKey.trim().isEmpty()) {
                try {
                    User us_currentUser = UserUtilities.findUser(UserUtilities.getCurrentUser());
                    ValueCollection vc_GpgKey = getUserGpgKey(us_currentUser);
                    if (vc_GpgKey != null) {
                        resolvedKey =
                                ((PasswordPrimitive)
                                                vc_GpgKey.getPrimitive(Const.str_GpgPrivateKey))
                                        .getValue();
                        resolvedPassphrase =
                                ((PasswordPrimitive)
                                                vc_GpgKey.getPrimitive(Const.str_GpgKeyPassphrase))
                                        .getValue();
                    }
                } catch (Exception e) {
                    _logger.warn("No stored GpgKeys found; proceeding with provided key if any.");
                }
            }
            // auto-detect base64-encoded keys for REST API calls where multi-line JSON escaping may
            // cause issues
            String decodedKey = resolvedKey;
            if (resolvedKey != null && !resolvedKey.startsWith("-----")) {
                try {
                    byte[] decoded = Base64.getDecoder().decode(resolvedKey);
                    decodedKey = new String(decoded, StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    // not valid base64, use as-is
                }
            }
            PastedKeyGpgSigner signer = new PastedKeyGpgSigner(decodedKey, resolvedPassphrase);
            PersonIdent committer = new PersonIdent("temp", "temp@temp.com");
            boolean canLocate = signer.canLocateSigningKey(null, null, committer, null, null);
            String fingerprint = signer.getFingerprint();

            ValueCollection vc = new ValueCollection();
            vc.put("GitThing", new StringPrimitive(repositoryThingName()));
            vc.put("SignCommits", new BooleanPrimitive(canLocate));
            vc.put(
                    "GpgKeyFingerprint",
                    new StringPrimitive(
                            fingerprint != null ? fingerprint : "Unable to derive fingerprint"));
            iftbl_Result.addRow(vc);

            signer.clearSensitiveData();

            String str_LogResult =
                    "GPG Key verification "
                            + (canLocate ? "succeeded" : "failed")
                            + ". Fingerprint: "
                            + (fingerprint != null ? fingerprint : "N/A");
            LogOperationResult(str_LogResult, str_CurrentMethodName);
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            _logger.error(errors.toString());
            LogOperationResult(errors.toString(), str_CurrentMethodName);
            ValueCollection vc = new ValueCollection();
            vc.put("GitThing", new StringPrimitive(repositoryThingName()));
            vc.put("SignCommits", new BooleanPrimitive(false));
            vc.put(
                    "GpgKeyFingerprint",
                    new StringPrimitive("Verification error: " + e.getMessage()));
            iftbl_Result.addRow(vc);
        }
        return iftbl_Result;
    }

    @ThingworxServiceDefinition(
            name = "Pull",
            description = "Pulls the last commit to the File Repository path",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String Pull(
            @ThingworxServiceParameter(
                            name = "Force",
                            description = "Forces a hard reset instead of a normal pull",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean Force) {
        String str_CurrentMethodName = "Pull";
        try {
            refreshConfiguration();
            _logger.warn("Starting Pull for GIT Repository Thing: " + this.getName());
            Thread.sleep(500);
            Git myGitFolder = getGitObject("Pull");
            // An unborn repository has no local Git state to preserve. Exporting
            // ThingWorx entities before the first checkout would create untracked
            // files that conflict with the remote tree during bootstrap.
            if (myGitFolder.getRepository().resolve("HEAD") != null) {
                syncFromThingworx();
            }
            User us_currentUser = UserUtilities.findUser(UserUtilities.getCurrentUser());
            ValueCollection vc_RepoCredentials = getGitRepoRemoteCredential(us_currentUser);
            String str_User = primitiveString(vc_RepoCredentials, Const.str_GitCommitterUser);
            String str_Password =
                    primitiveString(vc_RepoCredentials, Const.str_GitCommitterPassword);
            if (isBlank(str_User) || isBlank(str_Password)) {
                _logger.warn(Const.ERR_NO_CREDENTIALS);
                return "Pull Error: " + Const.ERR_PREFIX_AUTH + Const.ERR_NO_CREDENTIALS;
            }
            if (isBlank(str_GitRepoURL)) {
                _logger.warn(Const.ERR_NO_REPO_URL);
                return "Pull Error: " + Const.ERR_PREFIX_CONFIG + Const.ERR_NO_REPO_URL;
            }
            CredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(str_User, str_Password);
            if (isTrue(Force)) {
                myGitFolder.reset().setMode(ResetType.HARD).call();
            }
            PullResult pr =
                    myGitFolder
                            .pull()
                            .setRemote("origin")
                            .setCredentialsProvider(credentialsProvider)
                            .call();
            String str_LogResult =
                    String.format(
                            Const.SUCCESS_PULL,
                            pr.isSuccessful() ? "Successful" : "Unsuccessful",
                            pr.toString());
            if (pr.isSuccessful()) {
                try {
                    syncFromRepository();
                } catch (Exception syncEx) {
                    _logger.warn(
                            "Pull succeeded but sync from repository failed: "
                                    + syncEx.getMessage());
                }
            }
            Thread.sleep(2000);
            LogOperationResult(str_LogResult, str_CurrentMethodName);
            _logger.warn("Finished Pull for GIT Repository Thing: " + this.getName());
            return str_LogResult;
        } catch (Exception e) {
            String errMsg = buildErrorResult("Pull", e);
            try {
                LogOperationResult(errMsg, str_CurrentMethodName);
            } catch (Exception e1) {
                _logger.error("LogOperationResult failed for Pull: " + e1.toString());
            }
            return errMsg;
        }
    }

    @ThingworxServiceDefinition(
            name = "Fetch",
            description =
                    "Fetches refs and objects from a remote without merging, checking out, resetting, or synchronizing entities. The remote defaults to origin.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String Fetch(
            @ThingworxServiceParameter(
                            name = "Remote",
                            description = "Optional remote name; defaults to origin",
                            baseType = "STRING")
                    String Remote) {
        String str_CurrentMethodName = "Fetch";
        try {
            refreshConfiguration();
            _logger.warn("Starting Fetch for GIT Repository Thing: " + this.getName());
            Git myGitFolder = getGitObject("Fetch");
            User us_currentUser = UserUtilities.findUser(UserUtilities.getCurrentUser());
            ValueCollection vc_RepoCredentials = getGitRepoRemoteCredential(us_currentUser);
            String str_User = primitiveString(vc_RepoCredentials, Const.str_GitCommitterUser);
            String str_Password =
                    primitiveString(vc_RepoCredentials, Const.str_GitCommitterPassword);
            if (isBlank(str_User) || isBlank(str_Password)) {
                _logger.warn(Const.ERR_NO_CREDENTIALS);
                return "Fetch Error: " + Const.ERR_PREFIX_AUTH + Const.ERR_NO_CREDENTIALS;
            }
            if (isBlank(str_GitRepoURL)) {
                _logger.warn(Const.ERR_NO_REPO_URL);
                return "Fetch Error: " + Const.ERR_PREFIX_CONFIG + Const.ERR_NO_REPO_URL;
            }
            CredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(str_User, str_Password);
            String remote = isBlank(Remote) ? "origin" : Remote;
            myGitFolder
                    .fetch()
                    .setRemote(remote)
                    .setCredentialsProvider(credentialsProvider)
                    .call();
            String str_LogResult = "Fetch from '" + remote + "' completed successfully.";
            LogOperationResult(str_LogResult, str_CurrentMethodName);
            _logger.warn("Finished Fetch for GIT Repository Thing: " + this.getName());
            return str_LogResult;
        } catch (Exception e) {
            String errMsg = buildErrorResult("Fetch", e);
            try {
                LogOperationResult(errMsg, str_CurrentMethodName);
            } catch (Exception e1) {
                _logger.error("LogOperationResult failed for Fetch: " + e1.toString());
            }
            return errMsg;
        }
    }

    @ThingworxServiceDefinition(
            name = "DeleteLocalRepoContent",
            description =
                    "Deletes all files from the local repo path including the git configuration files. This operation is needed in case the git operations throw up strange errors. Reinitializes the Git object entirely.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void DeleteLocalRepoContent()
            throws IllegalStateException,
                    GitAPIException,
                    IOException,
                    InterruptedException,
                    Throwable {
        _logger.warn("DeleteLocalRepoContent:1 for entity: " + this.getName());
        Thread.sleep(5);
        FileRepositoryThing srcRepo =
                (FileRepositoryThing)
                        EntityUtilities.findEntity(
                                str_FileRepository, ThingworxRelationshipTypes.Thing);
        try {
            String str_FolderPath = srcRepo.getRootPath();
            closeGit();
            _logger.warn(
                    "DeleteLocalRepoContent:2, starting to delete files. All files should not be locked;");
            // deleteDirectory(new File(str_FolderPath), "DeleteLocalRepoContent");
            deleteGitFolder(new File(str_FolderPath), "DeleteLocalRepoContent");
            Thread.sleep(5);
        } catch (Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            _logger.error(
                    "Error encountered in DeleteLocalRepoContent for entity: "
                            + this.getName()
                            + "; Error Message: "
                            + errors.toString());
        }
        _logger.warn("DeleteLocalRepoContent:3 for entity: " + this.getName());
    }

    @ThingworxServiceDefinition(
            name = "CreateBranch",
            description =
                    "Creates a new local branch from an optional start point (commit, branch, or tag) without switching to it.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String CreateBranch(
            @ThingworxServiceParameter(
                            name = "BranchName",
                            description = "Name of the new branch",
                            baseType = "STRING")
                    String BranchName,
            @ThingworxServiceParameter(
                            name = "StartPoint",
                            description =
                                    "Optional: commit hash, branch name, or tag to branch from (defaults to HEAD)",
                            baseType = "STRING")
                    String StartPoint)
            throws Throwable, GitAPIException {
        _logger.trace("Entering Service: CreateBranch");
        String str_CurrentMethodName = "CreateBranch";
        if (isBlank(BranchName)) {
            String errMsg =
                    "CreateBranch Error: " + Const.ERR_PREFIX_CONFIG + "BranchName is required.";
            _logger.warn(errMsg);
            LogOperationResult(errMsg, str_CurrentMethodName);
            return errMsg;
        }
        try {
            syncFromThingworx();
            Git myGitFolder = getGitObject("CreateBranch");
            String str_StartPoint = orDefault(StartPoint, "HEAD");
            Ref branchRef =
                    myGitFolder
                            .branchCreate()
                            .setName(BranchName)
                            .setStartPoint(str_StartPoint)
                            .call();
            String str_LogResult =
                    String.format(
                            Const.SUCCESS_BRANCH_CREATED,
                            BranchName,
                            str_StartPoint,
                            branchRef.toString());
            LogOperationResult(str_LogResult, str_CurrentMethodName);
            _logger.trace("Exiting Service: CreateBranch");
            return branchRef.toString();
        } catch (Exception e) {
            String errMsg;
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                errMsg =
                        "CreateBranch Error: "
                                + Const.ERR_PREFIX_GIT
                                + "Branch '"
                                + BranchName
                                + "' already exists. Use a different branch name or delete the existing branch first.";
            } else {
                errMsg = buildErrorResult("CreateBranch", e);
            }
            LogOperationResult(errMsg, str_CurrentMethodName);
            return errMsg;
        }
    }

    @ThingworxServiceDefinition(
            name = "Checkout",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void Checkout(
            @ThingworxServiceParameter(
                            name = "BranchNameOrCommit",
                            description =
                                    "Switches the working tree to the specified branch. This is a wrapper on top of checkout <branch>.It does not autocreate new branches.",
                            baseType = "STRING")
                    String BranchNameOrCommit)
            throws Throwable, GitAPIException {
        _logger.trace("Entering Service: Checkout");
        if (isBlank(BranchNameOrCommit)) {
            _logger.warn("Checkout: BranchNameOrCommit is required.");
            return;
        }
        String str_CurrentMethodName = "Checkout";
        try {
            syncFromThingworx();
            Git myGitFolder = getGitObject("Checkout");
            Ref ref;
            try {
                ref = myGitFolder.checkout().setName(BranchNameOrCommit).call();
            } catch (RefNotFoundException ex) {
                _logger.warn(
                        "Checkout: Local branch '"
                                + BranchNameOrCommit
                                + "' not found. Attempting to create tracking branch from origin/'"
                                + BranchNameOrCommit
                                + "'.");
                ref =
                        myGitFolder
                                .checkout()
                                .setCreateBranch(true)
                                .setName(BranchNameOrCommit)
                                .setUpstreamMode(SetupUpstreamMode.TRACK)
                                .setStartPoint("origin/" + BranchNameOrCommit)
                                .call();
            }
            bool_isDetachedHead =
                    getGitObject("Checkout").getRepository().getFullBranch().indexOf("refs/heads")
                                    != -1
                            ? false
                            : true;
            str_CurrentBranchOrCommit = BranchNameOrCommit;
            syncFromRepository();
            String str_LogResult = (ref != null) ? ref.toString() : "No message.";
            LogOperationResult(str_LogResult, str_CurrentMethodName);
            _logger.trace("Exiting Service: Checkout");
        } catch (Exception e) {
            _logger.error("Checkout failed for " + BranchNameOrCommit + ": " + e.toString());
            throw e;
        }
    }

    @ThingworxServiceDefinition(
            name = "GetCurrentBranch",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.CurrentBranchStatus.DataShape"})
    public InfoTable GetCurrentBranch() {
        try {
            _logger.trace("Entering Service: GetCurrentBranch");
            InfoTable iftbl_CurrentBranchStatus =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            "GIT.CurrentBranchStatus.DataShape");
            Repository repository = getGitObject("GetCurrentBranch").getRepository();
            String fullBranch = repository.getFullBranch();
            String currentBranch = bool_isDetachedHead ? fullBranch : repository.getBranch();
            ValueCollection vc = new ValueCollection();

            vc.put("BranchName", new StringPrimitive(currentBranch));
            vc.put("DetachedHEAD", new BooleanPrimitive(bool_isDetachedHead));
            iftbl_CurrentBranchStatus.addRow(vc);
            _logger.trace("Exiting Service: GetCurrentBranch");
            return iftbl_CurrentBranchStatus;
        } catch (Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            _logger.error("GetCurrentBranch failed: " + errors.toString());
            return new InfoTable();
        }
    }

    @ThingworxServiceDefinition(
            name = "GetBranchList",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.BranchList.DataShape"})
    public InfoTable GetBranchList() {
        _logger.trace("Entering Service: GetBranchList");

        try {
            InfoTable iftbl_BranchList =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            "GIT.BranchList.DataShape");
            Git myGit = getGitObject("GetBranchList");
            List<Ref> branches = myGit.branchList().setListMode(ListMode.ALL).call();
            for (Iterator<Ref> iterator = branches.iterator(); iterator.hasNext(); ) {
                Ref ref = (Ref) iterator.next();
                ValueCollection vc = new ValueCollection();

                String str_LongBranchName = ref.getName();
                String str_ShortBranchName, str_BranchType;
                str_ShortBranchName =
                        ("HEAD".equals(str_LongBranchName))
                                ? "HEAD"
                                : str_LongBranchName
                                        .replace("refs/heads/", "")
                                        .replace("refs/remotes/origin/", "");
                str_BranchType =
                        ("HEAD".equals(str_LongBranchName))
                                ? "HEAD"
                                : (str_LongBranchName.indexOf("refs/heads/") >= 0
                                        ? "LOCAL"
                                        : "REMOTE");
                vc.put("BranchName", new StringPrimitive(str_LongBranchName));
                vc.put("ShortBranchName", new StringPrimitive(str_ShortBranchName));
                vc.put("BranchType", new StringPrimitive(str_BranchType));
                iftbl_BranchList.addRow(vc);
            }
            _logger.trace("Exiting Service: GetBranchList");
            return iftbl_BranchList;

        } catch (Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            _logger.error("GetBranchList failed: " + errors.toString());
            _logger.trace("Exiting Service: GetBranchList");
            return new InfoTable();
        }
    }

    @ThingworxServiceDefinition(
            name = "DeleteLocalBranch",
            description =
                    "This method deletes a local branch. Used in the case a remote branch was deleted/pruned and you want to remove your local copy.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String DeleteLocalBranch(
            @ThingworxServiceParameter(
                            name = "BranchName",
                            description = "Branch name to be deleted, without the refs/heads/ part",
                            baseType = "STRING")
                    String BranchName)
            throws IOException, GitAPIException {
        _logger.trace("Entering Service: DeleteLocalBranch");
        String str_CurrentMethodName = "DeleteLocalBranch";
        if (isBlank(BranchName)) {
            String errMsg =
                    "DeleteLocalBranch Error: "
                            + Const.ERR_PREFIX_CONFIG
                            + "BranchName is required.";
            _logger.warn(errMsg);
            return errMsg;
        }
        try {
            Git myGitFolder = getGitObject("DeleteLocalBranch");

            List<String> lstr =
                    myGitFolder
                            .branchDelete()
                            .setForce(true)
                            .setBranchNames("refs/heads/" + BranchName)
                            .call();
            String str_LogResult = "";
            if (lstr.size() == 0) {
                str_LogResult +=
                        " Branch '" + BranchName + "' was not found or could not be deleted.";
            }
            for (String str : lstr) {
                str_LogResult += str;
            }

            LogOperationResult(str_LogResult, str_CurrentMethodName);
            return str_LogResult;
        } catch (Exception e) {
            String errMsg;
            if (e.getMessage() != null && e.getMessage().contains("CannotDelete")) {
                errMsg =
                        "DeleteLocalBranch Error: "
                                + Const.ERR_PREFIX_GIT
                                + "Branch '"
                                + BranchName
                                + "' cannot be deleted. It may be the current branch. Checkout a different branch first.";
            } else {
                errMsg = buildErrorResult("DeleteLocalBranch", e);
            }
            _logger.error(errMsg);
            try {
                LogOperationResult(errMsg, str_CurrentMethodName);
            } catch (Exception e1) {
                _logger.error("LogOperationResult failed for DeleteLocalBranch: " + e1.toString());
            }
            return errMsg;
        }
    }

    @ThingworxServiceDefinition(
            name = "GetCommitList",
            description =
                    "Get a list of the commits for the current branch; if the current index is pointing to a commit, then it will return the commit list for the Initial branch configured in the Config section",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.CommitList.DataShape"})
    public InfoTable GetCommitList() {
        _logger.trace("Entering Service: GetCommitList");
        try {
            Git myGitFolder = getGitObject("GetCommitList");
            InfoTable iftbl_CommitList =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.str_CommitListDataShapeName);
            // Git myGit = GetRepository();
            ObjectId obj =
                    myGitFolder.getRepository().resolve("refs/heads/" + str_CurrentBranchOrCommit);
            if (obj == null)
                obj =
                        myGitFolder
                                .getRepository()
                                .resolve(
                                        "refs/heads/"
                                                + ((String)
                                                        getConfigurationSetting(
                                                                Const.str_ConfTableName,
                                                                Const.str_InitialBranch)));
            if (obj != null) {
                Iterable<RevCommit> commits = myGitFolder.log().add(obj).call();
                for (Iterator<RevCommit> iterator = commits.iterator(); iterator.hasNext(); ) {
                    RevCommit commit = (RevCommit) iterator.next();
                    ValueCollection vc = new ValueCollection();
                    vc.put(
                            "CommitTime",
                            new DatetimePrimitive(
                                    new DateTime(((long) commit.getCommitTime() * 1000))));
                    vc.put("CommitName", new StringPrimitive(commit.getShortMessage()));
                    vc.put("CommitID", new StringPrimitive(commit.getId().name()));
                    iftbl_CommitList.addRow(vc);
                }
            }
            _logger.trace("Exiting Service: GetCommitList");
            return iftbl_CommitList;
        } catch (Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            _logger.error("GetCommitList failed: " + errors.toString());
            return new InfoTable();
        }
    }

    @ThingworxServiceDefinition(
            name = "GetLog",
            description =
                    "Returns commit history, newest first. Ref is optional and defaults to the current branch, or the configured branch when HEAD is detached. MaxEntries of zero or omitted means no limit.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.CommitLog.DataShape"})
    public InfoTable GetLog(
            @ThingworxServiceParameter(
                            name = "Ref",
                            description = "Optional branch, tag, commit, or ref name",
                            baseType = "STRING")
                    String Ref,
            @ThingworxServiceParameter(
                            name = "MaxEntries",
                            description =
                                    "Maximum number of entries; zero or omitted means no limit",
                            baseType = "INTEGER")
                    Integer MaxEntries)
            throws Exception {
        _logger.trace("Entering Service: GetLog");
        InfoTable result =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.str_CommitLogDataShapeName);
        try {
            Repository repository = getGitObject("GetLog").getRepository();
            ObjectId objectId = resolveHistoryRef(repository, Ref, false);
            if (objectId == null) {
                _logger.warn("GetLog could not resolve ref: " + Ref);
                return result;
            }
            Iterable<RevCommit> commits = getGitObject("GetLog").log().add(objectId).call();
            int count = 0;
            for (RevCommit commit : commits) {
                if (MaxEntries != null && MaxEntries > 0 && count >= MaxEntries) break;
                ValueCollection row = new ValueCollection();
                row.put("CommitID", new StringPrimitive(commit.getId().name()));
                row.put("CommitName", new StringPrimitive(commit.getShortMessage()));
                row.put(
                        "CommitTime",
                        new DatetimePrimitive(new DateTime((long) commit.getCommitTime() * 1000)));
                putPerson(row, "AuthorName", "AuthorEmail", commit.getAuthorIdent());
                putPerson(row, "CommitterName", "CommitterEmail", commit.getCommitterIdent());
                row.put("ParentCommitIDs", new StringPrimitive(parentIds(commit)));
                result.addRow(row);
                count++;
            }
            return result;
        } catch (Exception ex) {
            _logger.error("GetLog failed: " + ex.getMessage(), ex);
            return result;
        }
    }

    @ThingworxServiceDefinition(
            name = "GetReflog",
            description =
                    "Returns local reflog entries, newest first. Reflogs are local to this repository clone and are not remote history. Ref defaults to HEAD; MaxEntries of zero or omitted means no limit.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.ReflogEntry.DataShape"})
    public InfoTable GetReflog(
            @ThingworxServiceParameter(
                            name = "Ref",
                            description = "Optional ref name; defaults to HEAD",
                            baseType = "STRING")
                    String Ref,
            @ThingworxServiceParameter(
                            name = "MaxEntries",
                            description =
                                    "Maximum number of entries; zero or omitted means no limit",
                            baseType = "INTEGER")
                    Integer MaxEntries)
            throws Exception {
        _logger.trace("Entering Service: GetReflog");
        InfoTable result =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.str_ReflogEntryDataShapeName);
        try {
            Git git = getGitObject("GetReflog");
            String ref = isBlank(Ref) ? "HEAD" : Ref;
            if (!ref.startsWith("refs/") && !"HEAD".equals(ref)) {
                ref = "refs/heads/" + ref;
            }
            int count = 0;
            for (ReflogEntry entry : git.reflog().setRef(ref).call()) {
                if (MaxEntries != null && MaxEntries > 0 && count >= MaxEntries) break;
                ValueCollection row = new ValueCollection();
                row.put("RefName", new StringPrimitive(ref));
                row.put("OldObjectID", new StringPrimitive(objectIdName(entry.getOldId())));
                row.put("NewObjectID", new StringPrimitive(objectIdName(entry.getNewId())));
                putPerson(row, "ActorName", "ActorEmail", entry.getWho());
                PersonIdent who = entry.getWho();
                row.put(
                        "EventTime",
                        new DatetimePrimitive(
                                new DateTime(
                                        who == null ? 0 : who.getWhenAsInstant().toEpochMilli())));
                row.put("Comment", new StringPrimitive(orEmpty(entry.getComment())));
                CheckoutEntry checkout = entry.parseCheckout();
                row.put(
                        "CheckoutSource",
                        new StringPrimitive(
                                checkout == null ? "" : orEmpty(checkout.getFromBranch())));
                row.put(
                        "CheckoutTarget",
                        new StringPrimitive(
                                checkout == null ? "" : orEmpty(checkout.getToBranch())));
                result.addRow(row);
                count++;
            }
            return result;
        } catch (Exception ex) {
            _logger.error("GetReflog failed: " + ex.getMessage(), ex);
            return result;
        }
    }

    private ObjectId resolveHistoryRef(
            Repository repository, String requestedRef, boolean defaultHead) throws IOException {
        String ref = requestedRef;
        if (isBlank(ref)) {
            if (defaultHead) return repository.resolve("HEAD");
            String fullBranch = repository.getFullBranch();
            String current =
                    fullBranch != null && fullBranch.startsWith("refs/heads/")
                            ? fullBranch.substring("refs/heads/".length())
                            : null;
            ref = isBlank(current) ? str_CurrentBranchOrCommit : current;
            if (isBlank(ref)) {
                ref =
                        (String)
                                getConfigurationSetting(
                                        Const.str_ConfTableName, Const.str_InitialBranch);
            }
        }
        ObjectId resolved = repository.resolve(ref);
        if (resolved == null && !ref.startsWith("refs/"))
            resolved = repository.resolve("refs/heads/" + ref);
        return resolved;
    }

    private static void putPerson(
            ValueCollection row, String nameField, String emailField, PersonIdent person) {
        row.put(nameField, new StringPrimitive(person == null ? "" : orEmpty(person.getName())));
        row.put(
                emailField,
                new StringPrimitive(person == null ? "" : orEmpty(person.getEmailAddress())));
    }

    private static String parentIds(RevCommit commit) {
        StringBuilder ids = new StringBuilder();
        for (RevCommit parent : commit.getParents()) {
            if (ids.length() > 0) ids.append(",");
            ids.append(parent.getId().name());
        }
        return ids.toString();
    }

    private static String objectIdName(ObjectId id) {
        return id == null ? "" : id.name();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    @ThingworxServiceDefinition(
            name = "Status",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.Status.DataShape"})
    public InfoTable Status() throws Exception {
        _logger.trace("Entering Service: Status");
        InfoTable iftbl_Status =
                InfoTableInstanceFactory.createInfoTableFromDataShape("GIT.Status.DataShape");
        try {
            syncFromThingworx();
            Git myGitObject = getGitObject("Status");
            org.eclipse.jgit.api.Status status = myGitObject.status().call();
            for (String stat : status.getModified()) {
                ValueCollection vc = new ValueCollection();
                vc.put("File", new StringPrimitive(stat));
                vc.put("Status", new StringPrimitive("Modified"));
                iftbl_Status.addRow(vc);
            }
            for (String stat : status.getAdded()) {
                ValueCollection vc = new ValueCollection();
                vc.put("File", new StringPrimitive(stat));
                vc.put("Status", new StringPrimitive("Added"));
                iftbl_Status.addRow(vc);
            }
            for (String stat : status.getChanged()) {
                ValueCollection vc = new ValueCollection();
                vc.put("File", new StringPrimitive(stat));
                vc.put("Status", new StringPrimitive("Changed"));
                iftbl_Status.addRow(vc);
            }
            for (String stat : status.getIgnoredNotInIndex()) {
                ValueCollection vc = new ValueCollection();
                vc.put("File", new StringPrimitive(stat));
                vc.put("Status", new StringPrimitive("Ignored"));
                iftbl_Status.addRow(vc);
            }
            for (String stat : status.getMissing()) {
                ValueCollection vc = new ValueCollection();
                vc.put("File", new StringPrimitive(stat));
                vc.put("Status", new StringPrimitive("Missing"));
                iftbl_Status.addRow(vc);
            }
            for (String stat : status.getRemoved()) {
                ValueCollection vc = new ValueCollection();
                vc.put("File", new StringPrimitive(stat));
                vc.put("Status", new StringPrimitive("Removed"));
                iftbl_Status.addRow(vc);
            }
            for (String stat : status.getUntracked()) {
                ValueCollection vc = new ValueCollection();
                vc.put("File", new StringPrimitive(stat));
                vc.put("Status", new StringPrimitive("Untracked"));
                iftbl_Status.addRow(vc);
            }
            for (String stat : status.getUntrackedFolders()) {
                ValueCollection vc = new ValueCollection();
                vc.put("File", new StringPrimitive(stat));
                vc.put("Status", new StringPrimitive("UntrackedFolder"));
                iftbl_Status.addRow(vc);
            }
            _logger.trace("Exiting Service: Status");
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            _logger.error("Status failed: " + errors.toString());
        }
        return iftbl_Status;
    }

    @ThingworxServiceDefinition(
            name = "GetConflictFiles",
            description = "Returns Git-conflicted files without synchronizing entities.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.Status.DataShape"})
    public InfoTable GetConflictFiles() throws Exception {
        InfoTable result =
                InfoTableInstanceFactory.createInfoTableFromDataShape("GIT.Status.DataShape");
        try {
            for (String file : getGitObject("GetConflictFiles").status().call().getConflicting()) {
                ValueCollection row = new ValueCollection();
                row.put("File", new StringPrimitive(file));
                row.put("Status", new StringPrimitive("Conflicting"));
                row.put("Staged", new BooleanPrimitive(false));
                result.addRow(row);
            }
        } catch (Exception e) {
            _logger.error("GetConflictFiles failed: " + e.getMessage(), e);
        }
        return result;
    }

    @ThingworxServiceDefinition(
            name = "ReadConflictFile",
            description = "Reads a conflicted repository file for UI editing.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String ReadConflictFile(
            @ThingworxServiceParameter(
                            name = "File",
                            description = "Repository-relative file path",
                            baseType = "STRING")
                    String File)
            throws Exception {
        try {
            return getConfiguredFileRepository().LoadText(repositoryRelativePath(File));
        } catch (Exception e) {
            return "ReadConflictFile Error: " + Const.ERR_PREFIX_GIT + e.getMessage();
        }
    }

    @ThingworxServiceDefinition(
            name = "WriteConflictFile",
            description = "Writes a UI-resolved repository file while a merge or rebase is paused.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String WriteConflictFile(
            @ThingworxServiceParameter(
                            name = "File",
                            description = "Repository-relative file path",
                            baseType = "STRING")
                    String File,
            @ThingworxServiceParameter(
                            name = "Content",
                            description = "Resolved file content",
                            baseType = "STRING")
                    String Content)
            throws Exception {
        try {
            getConfiguredFileRepository()
                    .SaveText(repositoryRelativePath(File), Content == null ? "" : Content);
            return "Wrote conflict file " + File;
        } catch (Exception e) {
            return "WriteConflictFile Error: " + Const.ERR_PREFIX_GIT + e.getMessage();
        }
    }

    @ThingworxServiceDefinition(
            name = "StageResolved",
            description =
                    "Stages manually resolved files without exporting ThingWorx entities over the edits.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String StageResolved(
            @ThingworxServiceParameter(
                            name = "FilePattern",
                            description =
                                    "Optional resolved file pattern; omitted stages all changes",
                            baseType = "STRING")
                    String FilePattern) {
        try {
            stageChanges(getGitObject("StageResolved"), FilePattern);
            return "Staged resolved " + (isBlank(FilePattern) ? "files" : FilePattern);
        } catch (Exception e) {
            return "StageResolved Error: " + Const.ERR_PREFIX_GIT + e.getMessage();
        }
    }

    @ThingworxServiceDefinition(
            name = "MergeContinue",
            description =
                    "Completes a paused merge after conflicts are resolved and staged, then synchronizes ThingWorx.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String MergeContinue(
            @ThingworxServiceParameter(
                            name = "Message",
                            description = "Optional merge commit message",
                            baseType = "STRING")
                    String Message) {
        try {
            Git git = getGitObject("MergeContinue");
            Repository repo = git.getRepository();
            if (repo.getRepositoryState() != RepositoryState.MERGING
                    && repo.getRepositoryState() != RepositoryState.MERGING_RESOLVED)
                return "MergeContinue Error: no merge is in progress.";
            ValueCollection identity =
                    getGitRepoRemoteCredential(
                            UserUtilities.findUser(UserUtilities.getCurrentUser()));
            String name = primitiveString(identity, Const.str_GitCommitterName);
            String email = primitiveString(identity, Const.str_GitCommitterEmail);
            if (isBlank(name) || isBlank(email))
                return "MergeContinue Error: " + Const.ERR_NO_COMMITTER;
            String msg = isBlank(Message) ? repo.readMergeCommitMsg() : Message;
            RevCommit commit =
                    git.commit()
                            .setMessage(isBlank(msg) ? "Merge resolved" : msg)
                            .setCommitter(name, email)
                            .call();
            syncFromRepository();
            return "Merge completed: " + commit.getId().name();
        } catch (Exception e) {
            return "MergeContinue Error: " + Const.ERR_PREFIX_GIT + e.getMessage();
        }
    }

    @ThingworxServiceDefinition(
            name = "MergeAbort",
            description =
                    "Aborts the current merge and synchronizes the restored state into ThingWorx.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String MergeAbort() {
        try {
            Git git = getGitObject("MergeAbort");
            Repository repo = git.getRepository();
            if (repo.getRepositoryState() != RepositoryState.MERGING
                    && repo.getRepositoryState() != RepositoryState.MERGING_RESOLVED)
                return "MergeAbort Error: no merge is in progress.";
            git.reset()
                    .setMode(ResetType.MERGE)
                    .setRef(repo.resolve("ORIG_HEAD") == null ? "HEAD" : "ORIG_HEAD")
                    .call();
            repo.writeMergeCommitMsg(null);
            repo.writeMergeHeads(null);
            syncFromRepository();
            return "Merge aborted.";
        } catch (Exception e) {
            return "MergeAbort Error: " + Const.ERR_PREFIX_GIT + e.getMessage();
        }
    }

    @ThingworxServiceDefinition(
            name = "RebaseContinue",
            description =
                    "Continues a paused rebase after conflicts are resolved and staged, then synchronizes ThingWorx.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String RebaseContinue() {
        return rebaseOperation(
                org.eclipse.jgit.api.RebaseCommand.Operation.CONTINUE, "RebaseContinue");
    }

    @ThingworxServiceDefinition(
            name = "RebaseSkip",
            description = "Skips the current commit in a paused rebase.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String RebaseSkip() {
        return rebaseOperation(org.eclipse.jgit.api.RebaseCommand.Operation.SKIP, "RebaseSkip");
    }

    @ThingworxServiceDefinition(
            name = "RebaseAbort",
            description =
                    "Aborts the current rebase and synchronizes the restored state into ThingWorx.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String RebaseAbort() {
        return rebaseOperation(org.eclipse.jgit.api.RebaseCommand.Operation.ABORT, "RebaseAbort");
    }

    private String rebaseOperation(
            org.eclipse.jgit.api.RebaseCommand.Operation operation, String service) {
        try {
            Git git = getGitObject(service);
            if (!git.getRepository().getRepositoryState().isRebasing())
                return service + " Error: no rebase is in progress.";
            RebaseResult result = git.rebase().setOperation(operation).call();
            if (result.getStatus() == RebaseResult.Status.OK
                    || result.getStatus() == RebaseResult.Status.ABORTED) syncFromRepository();
            return service + ": " + result.getStatus() + ": " + result;
        } catch (Exception e) {
            return service + " Error: " + Const.ERR_PREFIX_GIT + e.getMessage();
        }
    }

    @ThingworxServiceDefinition(
            name = "GetDiffPerFile",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String GetDiffPerFile(
            @ThingworxServiceParameter(name = "File", description = "", baseType = "STRING")
                    String File)
            throws Exception, GitAPIException {
        _logger.trace("Entering Service: GetDiffPerFile");
        if (File == null) return "";
        try {
            syncFromThingworx();
            Git myGitObject = getGitObject("GetDiffPerFile");
            ByteArrayOutputStream dif = new ByteArrayOutputStream();
            myGitObject.diff().setPathFilter(PathFilter.create(File)).setOutputStream(dif).call();
            _logger.trace("Exiting Service: GetDiffPerFile");
            return dif.toString("UTF-8");
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            _logger.error("GetDiffPerFile failed for file '" + File + "': " + errors.toString());
            return "GetDiffPerFile Error: "
                    + Const.ERR_PREFIX_GIT
                    + "Could not compute diff for file '"
                    + File
                    + "'. "
                    + e.getMessage();
        }
    }

    @ThingworxServiceDefinition(
            name = "GetDiffPerFileBetweenCommits",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String GetDiffPerFileBetweenCommits(
            @ThingworxServiceParameter(name = "File", description = "", baseType = "STRING")
                    String File,
            @ThingworxServiceParameter(name = "FromCommitID", description = "", baseType = "STRING")
                    String str_FromCommitID)
            throws Exception, GitAPIException {
        _logger.trace("Entering Service: GetDiffPerFileBetweenCommits");
        if (File == null) return "";
        try {
            String str_ToCommitID = "";
            Git myGitObject = getGitObject("GetDiffPerFileBetweenCommits");
            Repository repo = myGitObject.getRepository();
            ObjectId commit = repo.resolve(str_FromCommitID);
            if (commit == null) {
                _logger.error(
                        "GetDiffPerFileBetweenCommits: Commit '"
                                + str_FromCommitID
                                + "' not found.");
                return "Diff Error: " + String.format(Const.ERR_COMMIT_NOT_FOUND, str_FromCommitID);
            }
            RevWalk walk = new RevWalk(repo);
            RevCommit toCommit = walk.parseCommit(commit);
            if (toCommit.getParentCount() > 0) {
                str_ToCommitID = toCommit.getParent(0).getName();
            }
            AbstractTreeIterator newTreeParser = prepareTreeParser(repo, str_FromCommitID);
            ByteArrayOutputStream dif = new ByteArrayOutputStream();
            if (toCommit.getParentCount() == 0) {
                myGitObject
                        .diff()
                        .setNewTree(newTreeParser)
                        .setOldTree(new EmptyTreeIterator())
                        .setPathFilter(PathFilter.create(File))
                        .setOutputStream(dif)
                        .call();
            } else {
                AbstractTreeIterator oldTreeParser = prepareTreeParser(repo, str_ToCommitID);
                myGitObject
                        .diff()
                        .setNewTree(newTreeParser)
                        .setOldTree(oldTreeParser)
                        .setPathFilter(PathFilter.create(File))
                        .setOutputStream(dif)
                        .call();
            }
            walk.close();
            _logger.trace("Exiting Service: GetDiffPerFileBetweenCommits");
            String str_DiffResult = dif.toString("UTF-8");
            Thing utilityThing =
                    (Thing)
                            EntityUtilities.findEntity(
                                    Const.str_UtilityThingName, ThingworxRelationshipTypes.Thing);
            int maxSize =
                    utilityThing != null
                            ? utilityThing.GetIntegerPropertyValue(Const.str_MaxDiffSize)
                            : 500000;
            return str_DiffResult.length() > maxSize
                    ? String.format(Const.ERR_DIFF_TOO_LARGE, maxSize)
                    : str_DiffResult;
        } catch (Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            _logger.error("GetDiffPerFileBetweenCommits failed: " + errors.toString());
            return "Diff Error: "
                    + Const.ERR_PREFIX_GIT
                    + "Could not compute diff for file '"
                    + File
                    + "' between commits. "
                    + ex.getMessage();
        }
    }

    @ThingworxServiceDefinition(
            name = "GetCommitInfo",
            description = "This service gets a commit information based on the Commit ID",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.CommitInfo.DataShape"})
    public InfoTable GetCommitInfo(
            @ThingworxServiceParameter(name = "CommitID", description = "", baseType = "STRING")
                    String CommitID)
            throws Exception {
        _logger.trace("Entering Service: GetCommitInfo");
        InfoTable iftbl_Status =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.str_CommitInfoDataShapeName);
        if (CommitID == null) {
            _logger.warn("GetCommitInfo: No CommitID provided.");
            return iftbl_Status;
        }
        try {
            Repository myGitRepository = getGitObject("GetCommitInfo").getRepository();
            ObjectId commit = myGitRepository.resolve(CommitID);
            if (commit == null) {
                _logger.error("GetCommitInfo: Commit '" + CommitID + "' not found.");
                return iftbl_Status;
            }
            try (RevWalk walk = new RevWalk(myGitRepository)) {
                RevCommit commitAgain = walk.parseCommit(commit);
                ValueCollection vc = new ValueCollection();
                vc.put("CommitID", new StringPrimitive(commitAgain.getId().name()));
                vc.put("Parents", new StringPrimitive(ProcessRevCommit(commitAgain.getParents())));
                vc.put(
                        "Author",
                        new StringPrimitive(
                                commitAgain.getAuthorIdent().getName()
                                        + " "
                                        + commitAgain.getAuthorIdent().getEmailAddress()));
                vc.put(
                        "Date",
                        new DatetimePrimitive(
                                new DateTime(((long) commitAgain.getCommitTime() * 1000))));
                vc.put(
                        "Commiter",
                        new StringPrimitive(
                                commitAgain.getCommitterIdent().getName()
                                        + " "
                                        + commitAgain.getCommitterIdent().getEmailAddress()));
                vc.put("CommitDescription", new StringPrimitive(commitAgain.getFullMessage()));
                String str_SigVerification = "";
                byte[] rawBuf = commitAgain.getRawBuffer();
                if (rawBuf != null) {
                    String rawStr = new String(rawBuf, java.nio.charset.StandardCharsets.UTF_8);
                    if (rawStr.contains("\ngpgsig ")) {
                        str_SigVerification = "SIGNED";
                    }
                }
                vc.put("SignatureVerification", new StringPrimitive(str_SigVerification));
                InfoTable iftbl_CommitChangedFiles =
                        InfoTableInstanceFactory.createInfoTableFromDataShape(
                                Const.str_CommitChangedFiles);
                DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
                diffFormatter.setRepository(myGitRepository);
                List<DiffEntry> entries;
                if (commitAgain.getParentCount() != 0)
                    entries = diffFormatter.scan(commitAgain.getParent(0), commitAgain.getTree());
                else entries = diffFormatter.scan(null, commitAgain.getTree());

                for (DiffEntry diffEntry : entries) {
                    ValueCollection v2 = new ValueCollection();

                    switch (diffEntry.getChangeType()) {
                        case ADD:
                            {
                                v2.put("FileName", new StringPrimitive(diffEntry.getNewPath()));
                                break;
                            }
                        case DELETE:
                            {
                                v2.put("FileName", new StringPrimitive(diffEntry.getOldPath()));
                                break;
                            }
                        case MODIFY:
                            {
                                v2.put("FileName", new StringPrimitive(diffEntry.getNewPath()));
                                break;
                            }
                        case COPY:
                            {
                                v2.put("FileName", new StringPrimitive(diffEntry.getNewPath()));
                                break;
                            }
                        case RENAME:
                            {
                                v2.put("FileName", new StringPrimitive(diffEntry.getNewPath()));
                                break;
                            }
                    }
                    v2.put("Status", new StringPrimitive(diffEntry.getChangeType().toString()));
                    iftbl_CommitChangedFiles.addRow(v2);
                }
                diffFormatter.close();

                vc.put("ChangedFiles", new InfoTablePrimitive(iftbl_CommitChangedFiles));
                iftbl_Status.addRow(vc);
                walk.dispose();
                _logger.trace("Exiting Service: GetCommitInfo");
            }
            return iftbl_Status;
        } catch (Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            _logger.error(
                    "GetCommitInfo failed for CommitID '" + CommitID + "': " + errors.toString());
            return iftbl_Status;
        }
    }

    private String ProcessRevCommit(RevCommit[] parents) {

        if (parents == null) {
            return "";
        }
        String str_Parents = "";
        for (int x = 0; x < parents.length; x++) {
            str_Parents += parents[x].getName();
        }
        return str_Parents;
    }

    @ThingworxServiceDefinition(
            name = "ExportProjectEntities",
            description = "Exports the configured ThingWorx project directly to this repository.",
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
                                    "Optional compatibility parameter; committing is handled by Commit.",
                            baseType = "STRING")
                    String commitMessage)
            throws Exception {
        exportProjectEntities(ProjectName, includeDependents, EntitiesToExport);
    }

    private void exportProjectEntities(
            String ProjectName, Boolean includeDependents, InfoTable EntitiesToExport)
            throws Exception {
        if (isBlank(ProjectName)) {
            throw new Exception(Const.ERR_PREFIX_CONFIG + Const.ERR_PROJECT_NAME_REQUIRED);
        }

        FileRepositoryThing fileRepo = getConfiguredFileRepository();
        String repoPath = orDefault(str_FileRepoPath, "");
        boolean includeDeps = isTrue(includeDependents);
        IServiceProvider sourceControlFunctions =
                (IServiceProvider)
                        EntityUtilities.findEntity(
                                "SourceControlFunctions", ThingworxRelationshipTypes.Resource);
        if (sourceControlFunctions == null) {
            throw new Exception(Const.ERR_PREFIX_SYSTEM + Const.ERR_NO_SCF_RESOURCE);
        }

        ValueCollection params = new ValueCollection();
        params.put("repositoryName", new StringPrimitive(fileRepo.getName()));
        params.put("path", new StringPrimitive(repoPath));
        params.put("projectName", new StringPrimitive(ProjectName));
        params.put("includeDependents", new BooleanPrimitive(includeDeps));
        sourceControlFunctions.processServiceRequest("ExportSourceControlledEntities", params);
        removeLastModifiedDate(fileRepo.getName(), repoPath, ProjectName);
        removeModelPersistenceProviderPackage(fileRepo.getName(), repoPath, ProjectName);
    }

    @ThingworxServiceDefinition(
            name = "ImportProjectEntities",
            description = "Imports all XML entities below this repository's configured path.",
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
                            name = "entityPath",
                            description =
                                    "Relative path within this FileRepository; blank uses RepoPathName.",
                            baseType = "STRING")
                    String entityPath,
            @ThingworxServiceParameter(
                            name = "ignoreDependencies",
                            description = "If true, strips dependency validation during import",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean ignoreDependencies)
            throws Exception {
        FileRepositoryThing fileRepo = getConfiguredFileRepository();
        String repoPath = entityPath;
        if (isBlank(repoPath) || "*".equals(repoPath)) repoPath = str_FileRepoPath;
        while (repoPath.startsWith("/")) repoPath = repoPath.substring(1);
        while (repoPath.endsWith("/")) repoPath = repoPath.substring(0, repoPath.length() - 1);

        InfoTable files = InfoTableInstanceFactory.createInfoTableFromDataShape("FileSystemFile");
        collectXmlFiles(fileRepo, repoPath, files);
        InfoTable result = new InfoTable();
        if (files.getRowCount() == 0) return result;
        // SourceControlFunctions imports a directory tree, not an individual XML file.
        importSourceControlledEntities(fileRepo.getName(), repoPath);
        ValueCollection summary = new ValueCollection();
        summary.put("testName", new StringPrimitive(repoPath));
        summary.put("passed", new BooleanPrimitive(true));
        summary.put(
                "comments", new StringPrimitive("Imported " + files.getRowCount() + " XML files."));
        result.addRow(summary);
        /*
        for (int i = 0; i < files.getRowCount(); i++) {
            String path = primitiveString(files.getRow(i), "path");
            if (isBlank(path)) continue;
            String cleanPath = path.startsWith("/") ? path.substring(1) : path;
            ValueCollection row = new ValueCollection();
            row.put("testName", new StringPrimitive(cleanPath));
            row.put(
                    "startTimestamp",
                    new DatetimePrimitive(new DateTime(System.currentTimeMillis())));
            try {
                importSourceControlledEntities(fileRepo.getName(), cleanPath);
                row.put("passed", new BooleanPrimitive(true));
                row.put("comments", new StringPrimitive("Import completed."));
            } catch (Exception e) {
                row.put("passed", new BooleanPrimitive(false));
                row.put("comments", new StringPrimitive("Import failed: " + e.getMessage()));
            }
            row.put(
                    "endTimestamp",
                    new DatetimePrimitive(new DateTime(System.currentTimeMillis())));
            result.addRow(row);
        }
        */
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
            String filePath = primitiveString(row, "path");
            if (isBlank(name) || isBlank(filePath)) continue;
            ValueCollection newRow = new ValueCollection();
            newRow.put("name", new StringPrimitive(name));
            newRow.put("path", new StringPrimitive(filePath));
            allFiles.addRow(newRow);
        }
        ValueCollection dirParams = new ValueCollection();
        dirParams.put("path", new StringPrimitive(path));
        dirParams.put("nameMask", new StringPrimitive(""));
        InfoTable dirs = (InfoTable) fileRepo.processServiceRequest("ListDirectories", dirParams);
        for (int i = 0; i < dirs.getRowCount(); i++) {
            String dirPath = primitiveString(dirs.getRow(i), "path");
            if (!isBlank(dirPath)) collectXmlFiles(fileRepo, dirPath, allFiles);
        }
    }

    private void importSourceControlledEntities(String repositoryName, String path)
            throws Exception {
        Object resource =
                EntityUtilities.findEntity(
                        "SourceControlFunctions", ThingworxRelationshipTypes.Resource);
        if (resource == null) {
            throw new Exception(Const.ERR_PREFIX_SYSTEM + Const.ERR_NO_SCF_RESOURCE);
        }
        ValueCollection params = new ValueCollection();
        params.put("repositoryName", new StringPrimitive(repositoryName));
        params.put("path", new StringPrimitive(path));
        params.put("useDefaultDataProvider", new BooleanPrimitive(true));
        params.put("withSubsystems", new BooleanPrimitive(false));
        params.put("overwritePropertyValues", new BooleanPrimitive(true));
        ((IServiceProvider) resource)
                .processServiceRequest("ImportSourceControlledEntities", params);
    }

    private void removeLastModifiedDate(String fileRepoName, String repoPath, String projectName)
            throws IOException {
        FileRepositoryThing fileRepo =
                (FileRepositoryThing)
                        EntityUtilities.findEntity(fileRepoName, ThingworxRelationshipTypes.Thing);
        File root = new File(fileRepo.getRootPath(), repoPath);
        if (hasText(projectName)) root = new File(root, projectName);
        if (!root.exists()) return;
        List<File> files = new ArrayList<>();
        collectXmlFilesOnDisk(root, files);
        for (File file : files) {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Files.write(
                    file.toPath(),
                    content.replaceAll("\\s+lastModifiedDate=\"[^\"]*\"", "")
                            .getBytes(StandardCharsets.UTF_8));
        }
    }

    private void removeModelPersistenceProviderPackage(
            String fileRepoName, String repoPath, String projectName) throws IOException {
        FileRepositoryThing fileRepo =
                (FileRepositoryThing)
                        EntityUtilities.findEntity(fileRepoName, ThingworxRelationshipTypes.Thing);
        File root = new File(fileRepo.getRootPath(), repoPath);
        if (hasText(projectName)) root = new File(root, projectName);
        if (!root.exists()) return;
        List<File> files = new ArrayList<>();
        collectXmlFilesOnDisk(root, files);
        for (File file : files) {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Files.write(
                    file.toPath(),
                    content.replaceAll("\\s+modelPersistenceProviderPackage=\"[^\"]*\"", "")
                            .getBytes(StandardCharsets.UTF_8));
        }
    }

    private void collectXmlFilesOnDisk(File directory, List<File> result) {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) collectXmlFilesOnDisk(file, result);
            else if (file.getName().toLowerCase().endsWith(".xml")) result.add(file);
        }
    }

    private boolean syncFromThingworx() {
        try {
            if (isBlank(str_ProjectName)) {
                _logger.trace(Const.WARN_NO_PROJECT_SKIP);
                return true;
            }
            exportProjectEntities(str_ProjectName, false, null);
            return true;
        } catch (Exception e) {
            _logger.error(
                    String.format(Const.WARN_SYNC_FAILED, this.getName() + ": " + e.getMessage()));
            return false;
        }
    }

    /** Re-read configuration written immediately before a service invocation. */
    private void refreshConfiguration() {
        try {
            Thing targetThing = resolveTargetThing();
            if (targetThing != null) {
                ValueCollection params = new ValueCollection();
                params.put("tableName", new StringPrimitive(Const.str_ConfTableName));
                Object rawTable =
                        targetThing.processServiceRequest("GetConfigurationTable", params);
                InfoTable table =
                        rawTable instanceof InfoTable
                                ? (InfoTable) rawTable
                                : rawTable instanceof InfoTablePrimitive
                                        ? ((InfoTablePrimitive) rawTable).getValue()
                                        : null;
                if (table != null && table.getRowCount() > 0) {
                    ValueCollection row = table.getRow(0);
                    this.str_GitRepoURL = primitiveString(row, Const.str_GitRepoURL);
                    this.str_FileRepository =
                            targetThing instanceof FileRepositoryThing
                                    ? targetThing.getName()
                                    : orDefault(
                                            primitiveString(row, Const.str_FileRepository),
                                            this.getName());
                    this.str_FileRepoPath = primitiveString(row, Const.str_RepoPathName);
                    this.str_CurrentBranchOrCommit = primitiveString(row, Const.str_InitialBranch);
                    this.bool_UseProxy = isTrue((Boolean) valueOf(row, Const.str_UseProxy));
                    this.str_ProxyURL = primitiveString(row, Const.str_ProxyURL);
                    this.int_ProxyPort = (Integer) valueOf(row, Const.str_ProxyPort);
                    this.str_ProjectName = primitiveString(row, Const.str_ProjectName);
                    return;
                }
            }
        } catch (Exception e) {
            _logger.warn("Could not refresh Configuration table for repository service", e);
        }

        this.str_GitRepoURL =
                (String) getConfigurationSetting(Const.str_ConfTableName, Const.str_GitRepoURL);
        this.str_FileRepository =
                orDefault(
                        (String)
                                getConfigurationSetting(
                                        Const.str_ConfTableName, Const.str_FileRepository),
                        this.getName());
        this.str_FileRepoPath =
                (String) getConfigurationSetting(Const.str_ConfTableName, Const.str_RepoPathName);
        this.str_CurrentBranchOrCommit =
                (String) getConfigurationSetting(Const.str_ConfTableName, Const.str_InitialBranch);
        this.bool_UseProxy =
                isTrue(
                        (Boolean)
                                getConfigurationSetting(
                                        Const.str_ConfTableName, Const.str_UseProxy));
        this.str_ProxyURL =
                (String) getConfigurationSetting(Const.str_ConfTableName, Const.str_ProxyURL);
        this.int_ProxyPort =
                (Integer) getConfigurationSetting(Const.str_ConfTableName, Const.str_ProxyPort);
        this.str_ProjectName =
                (String) getConfigurationSetting(Const.str_ConfTableName, Const.str_ProjectName);
    }

    private Thing resolveTargetThing() {
        try {
            Object meContext = ThreadLocalContext.getMeContext();
            if (meContext instanceof Thing) return (Thing) meContext;
            if (meContext instanceof String && hasText((String) meContext)) {
                return (Thing)
                        EntityUtilities.findEntity(
                                (String) meContext, ThingworxRelationshipTypes.Thing);
            }
        } catch (Exception e) {
            _logger.trace("Could not resolve repository service target: " + e.getMessage());
        }
        return null;
    }

    private String repositoryThingName() {
        Thing targetThing = resolveTargetThing();
        if (targetThing != null && hasText(targetThing.getName())) return targetThing.getName();
        return this.getName();
    }

    private Object valueOf(ValueCollection row, String fieldName) {
        return row.getPrimitive(fieldName) == null ? null : row.getPrimitive(fieldName).getValue();
    }

    private void syncFromRepository() {
        try {
            if (isBlank(str_ProjectName)) {
                _logger.trace(Const.WARN_NO_PROJECT_SKIP);
                return;
            }
            ImportProjectEntities("", true);
        } catch (Exception e) {
            _logger.error(
                    "syncFromRepository failed for " + this.getName() + ": " + e.getMessage());
        }
    }

    private void LogOperationResult(String str_OperationResult, String str_ServiceName) {
        try {
            Thing rsc =
                    (Thing)
                            EntityUtilities.findEntity(
                                    Const.str_UtilityThingName, ThingworxRelationshipTypes.Thing);
            ValueCollection vc = new ValueCollection();
            vc.put("timestamp", new DatetimePrimitive(new DateTime(System.currentTimeMillis())));
            vc.put("User", new StringPrimitive(GetCurrentUser()));
            vc.put("ServiceName", new StringPrimitive(str_ServiceName));
            vc.put("Content", new StringPrimitive(str_OperationResult));
            vc.put("Source", new StringPrimitive(this.getName()));
            rsc.processServiceRequest("AddLogEntry", vc);
        } catch (Exception ex) {
            _logger.error("LogOperationResult failed: " + ex.getMessage());
        }
    }

    private String GetCurrentUser() {
        return ThreadLocalContext.getSecurityContext().getName();
    }

    private void closeGit()
            throws InterruptedException,
                    NoSuchMethodException,
                    SecurityException,
                    IllegalAccessException,
                    IllegalArgumentException,
                    InvocationTargetException,
                    IOException,
                    GitAPIException {
        Git gitObjectToClose = getGitObject("closeGit");
        FileRepository gitRepository = (FileRepository) gitObjectToClose.getRepository();
        Thread.sleep(200);
        _logger.warn("Method DeleteLocalRepoContent, performing 1st myGitRepository.close()");
        gitRepository.close();
        Thread.sleep(200);
        // _logger.warn("Method DeleteLocalRepoContent, performing 2nd
        // myGitRepository.close()");
        //		myGitRepository.close();
        //		_logger.warn("Method DeleteLocalRepoContent, performing 3rd myGitRepository.close()");
        //		myGitRepository.close();
        //		Thread.sleep(200);
        RepositoryCache.clear();
        _logger.warn("Method DeleteLocalRepoContent, performing myGitObject.close()");
        gitObjectToClose.close();
        WindowCacheConfig wconfig = new WindowCacheConfig();
        wconfig.setPackedGitMMAP(false);
        wconfig.install();
        gitObject = null;
    }

    private void getFilesToDelete(
            File path,
            String str_Source,
            ArrayList<File> lst_FilesToDelete,
            ArrayList<File> lst_FoldersToDelete) {
        // _logger.warn("Starting to remove files from directory "+path.toString());
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    lst_FoldersToDelete.add(files[i]);
                    getFilesToDelete(files[i], str_Source, lst_FilesToDelete, lst_FoldersToDelete);
                } else {
                    lst_FilesToDelete.add(files[i]);
                }
            }
        }
    }

    private void deleteGitFolder(File path, String str_Source) throws InterruptedException {
        ArrayList<File> lst_FilesToDelete = new ArrayList<File>();
        ArrayList<File> lst_FoldersToDelete = new ArrayList<File>();
        getFilesToDelete(path, str_Source, lst_FilesToDelete, lst_FoldersToDelete);
        _logger.warn("Folders to delete size: " + lst_FoldersToDelete.size());
        _logger.warn("Files to delete size: " + lst_FilesToDelete.size());
        ArrayList<File> lst_DeletedFiles = new ArrayList<File>();
        ArrayList<File> lst_DeletedFolders = new ArrayList<File>();
        for (File file : lst_FilesToDelete) {
            Thread.sleep(1);
            try {
                Files.delete(file.toPath());
                lst_DeletedFiles.add(file);
            } catch (IOException ex) {
                StringWriter errors = new StringWriter();
                ex.printStackTrace(new PrintWriter(errors));
                _logger.error(
                        "Source method: "
                                + str_Source
                                + "; Error removing file "
                                + file.getAbsolutePath()
                                + "; Error: "
                                + ex.toString()
                                + "; Attempting to remove read-only flag.");
                try {
                    file.setWritable(true);
                    Files.delete(file.toPath());
                    lst_DeletedFiles.add(file);
                    _logger.warn(
                            "Source method: "
                                    + str_Source
                                    + "; File deleted successfully after read-only was removed "
                                    + file.getAbsolutePath());
                } catch (IOException ex2) {
                    StringWriter errors2 = new StringWriter();
                    ex.printStackTrace(new PrintWriter(errors2));
                    _logger.error(
                            "Source method: "
                                    + str_Source
                                    + "; Error removing read-only flag for file "
                                    + file.getAbsolutePath()
                                    + "; Error: "
                                    + ex.toString());
                }
            }
        }
        lst_FilesToDelete.removeAll(lst_DeletedFiles);
        _logger.warn(
                "Files to delete size after removal (should be zero): " + lst_FilesToDelete.size());
        lst_FoldersToDelete.sort(
                (f1, f2) ->
                        Integer.compare(f2.toPath().getNameCount(), f1.toPath().getNameCount()));
        for (File file : lst_FoldersToDelete) {
            try {
                Files.delete(file.toPath());
                lst_DeletedFolders.add(file);
            } catch (IOException ex) {
                StringWriter errors = new StringWriter();
                ex.printStackTrace(new PrintWriter(errors));
                _logger.error(
                        "Source method: "
                                + str_Source
                                + "; Error removing folder "
                                + file.getAbsolutePath()
                                + "; Error: "
                                + ex.toString());
            }
        }
        // Removes the final subfolder from the ThingWorx File Repository
        path.delete();
    }

    private Git openOrCreate(File gitDirectory)
            throws IOException, GitAPIException, InterruptedException {
        Git myGitObject;
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        repositoryBuilder.addCeilingDirectory(gitDirectory);
        repositoryBuilder.findGitDir(gitDirectory);
        if (repositoryBuilder.getGitDir() == null) {
            myGitObject =
                    Git.init()
                            .setDirectory(gitDirectory)
                            .setInitialBranch(str_CurrentBranchOrCommit)
                            .call();
            // Wait 2 seconds in case any antivirus locks the git repository files
            Thread.sleep(2000);

        } else {
            myGitObject = new Git(repositoryBuilder.build());
        }
        return myGitObject;
    }

    private Git getGitObject(String str_CallerMethod)
            throws IOException,
                    GitAPIException,
                    InterruptedException,
                    NoSuchMethodException,
                    SecurityException,
                    IllegalAccessException,
                    IllegalArgumentException,
                    InvocationTargetException {
        refreshConfiguration();
        // modified so that it will store the Git repository object (from JGIT) in an
        // internal private field, not requiring reopen each time.
        _logger.warn(
                "Retrieving GIT Repository Thing : "
                        + this.getName()
                        + "; Object is: "
                        + (gitObject != null)
                        + "; Method: "
                        + str_CallerMethod);
        if (gitObject == null) {
            Thing targetThing = resolveTargetThing();
            if (targetThing instanceof FileRepositoryThing) {
                str_FileRepository = targetThing.getName();
            }
            FileRepositoryThing srcRepo =
                    (FileRepositoryThing)
                            EntityUtilities.findEntity(
                                    str_FileRepository, ThingworxRelationshipTypes.Thing);
            // The FileRepository root is the Git working tree. RepoPath is only
            // the subdirectory where exported ThingWorx entities are stored.
            Git myGitObject = openOrCreate(new File(srcRepo.getRootPath()));
            StoredConfig config = myGitObject.getRepository().getConfig();
            config.setString("remote", "origin", "url", str_GitRepoURL);
            config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
            config.setString("remote", "origin", "prune", "true");
            config.setString("core", null, "autocrlf", "input");
            // prevents Bitmap creation during GarbageCollection
            config.setBoolean("repack", null, "writeBitmaps", false);
            config.save();
            _logger.warn(
                    "GIT Repository Thing: "
                            + this.getName()
                            + " Git object found null, initialized. Git object created and stored internally for future use. "
                            + "; Method: "
                            + str_CallerMethod);
            gitObject = myGitObject;
        }
        return gitObject;
    }

    private void stageChanges(Git git, String filePattern) throws GitAPIException {
        String pattern = isBlank(filePattern) ? "." : filePattern;
        git.add().addFilepattern(pattern).call();
        git.add().addFilepattern(pattern).setUpdate(true).call();
    }

    private FileRepositoryThing getConfiguredFileRepository() throws Exception {
        refreshConfiguration();
        Thing targetThing = resolveTargetThing();
        if (targetThing instanceof FileRepositoryThing) {
            return (FileRepositoryThing) targetThing;
        }
        FileRepositoryThing repository =
                (FileRepositoryThing)
                        EntityUtilities.findEntity(
                                str_FileRepository, ThingworxRelationshipTypes.Thing);
        if (repository == null) throw new IllegalStateException(Const.ERR_FILE_REPO_NOT_FOUND);
        return repository;
    }

    private String repositoryRelativePath(String file) {
        if (isBlank(file)) throw new IllegalArgumentException("File is required.");
        String clean = file.replace('\\', '/');
        while (clean.startsWith("/")) clean = clean.substring(1);
        if (clean.equals("..") || clean.startsWith("../") || clean.contains("/../")) {
            throw new IllegalArgumentException("File must remain inside the repository path.");
        }
        String root = str_FileRepoPath == null ? "" : str_FileRepoPath.replace('\\', '/');
        while (root.startsWith("/")) root = root.substring(1);
        while (root.endsWith("/")) root = root.substring(0, root.length() - 1);
        return isBlank(root) ? clean : root + "/" + clean;
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId)
            throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(ObjectId.fromString(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }
            walk.dispose();
            return treeParser;
        }
    }

    @ThingworxServiceDefinition(
            name = "Merge",
            description = "Merges the specified branch into the current branch.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String Merge(
            @ThingworxServiceParameter(
                            name = "BranchName",
                            description = "The branch to merge from",
                            baseType = "STRING")
                    String BranchName)
            throws Exception, GitAPIException {
        String str_CurrentMethodName = "Merge";
        if (isBlank(BranchName)) {
            String errMsg = "Merge Error: " + Const.ERR_PREFIX_CONFIG + "BranchName is required.";
            _logger.warn(errMsg);
            return errMsg;
        }
        try {
            syncFromThingworx();
            Git myGitFolder = getGitObject("Merge");
            ObjectId mergeBase = myGitFolder.getRepository().resolve(BranchName);
            if (mergeBase == null) {
                return "Merge Error: " + String.format(Const.ERR_BRANCH_NOT_FOUND, BranchName);
            }
            MergeResult result = myGitFolder.merge().include(mergeBase).call();
            String str_LogResult = result.getMergeStatus().toString() + ": " + result.toString();
            if (result.getMergeStatus().isSuccessful()) {
                syncFromRepository();
            } else if (result.getMergeStatus() == MergeResult.MergeStatus.CONFLICTING) {
                _logger.warn(Const.ERR_MERGE_CONFLICT);
            }
            LogOperationResult(str_LogResult, str_CurrentMethodName);
            return str_LogResult;
        } catch (Exception e) {
            String errMsg;
            if (e.getMessage() != null && e.getMessage().contains("CheckoutConflict")) {
                errMsg = "Merge Error: " + Const.ERR_MERGE_CONFLICT;
            } else {
                errMsg = buildErrorResult("Merge", e);
            }
            _logger.error(errMsg);
            try {
                LogOperationResult(errMsg, str_CurrentMethodName);
            } catch (Exception e1) {
                _logger.error(e1.toString());
            }
            return errMsg;
        }
    }

    @ThingworxServiceDefinition(
            name = "Rebase",
            description = "Rebases the current branch onto the specified upstream branch.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String Rebase(
            @ThingworxServiceParameter(
                            name = "UpstreamBranch",
                            description = "The branch or commit to rebase onto",
                            baseType = "STRING")
                    String UpstreamBranch)
            throws Exception, GitAPIException {
        String str_CurrentMethodName = "Rebase";
        if (isBlank(UpstreamBranch)) {
            String errMsg =
                    "Rebase Error: " + Const.ERR_PREFIX_CONFIG + "UpstreamBranch is required.";
            _logger.warn(errMsg);
            return errMsg;
        }
        try {
            syncFromThingworx();
            Git myGitFolder = getGitObject("Rebase");
            ObjectId upstream = myGitFolder.getRepository().resolve(UpstreamBranch);
            if (upstream == null) {
                return "Rebase Error: "
                        + String.format(Const.ERR_UPSTREAM_NOT_FOUND, UpstreamBranch);
            }
            RebaseResult result = myGitFolder.rebase().setUpstream(upstream).call();
            String str_LogResult = result.getStatus().toString() + ": " + result.toString();
            if (result.getStatus() == RebaseResult.Status.OK) {
                syncFromRepository();
            } else if (result.getStatus() == RebaseResult.Status.STOPPED) {
                _logger.warn(Const.ERR_REBASE_CONFLICT);
            }
            LogOperationResult(str_LogResult, str_CurrentMethodName);
            return str_LogResult;
        } catch (Exception e) {
            String errMsg;
            if (e.getMessage() != null && e.getMessage().contains("Conflicting")) {
                errMsg = "Rebase Error: " + Const.ERR_REBASE_CONFLICT;
            } else {
                errMsg = buildErrorResult("Rebase", e);
            }
            _logger.error(errMsg);
            try {
                LogOperationResult(errMsg, str_CurrentMethodName);
            } catch (Exception e1) {
                _logger.error(e1.toString());
            }
            return errMsg;
        }
    }

    @ThingworxServiceDefinition(
            name = "CreateTag",
            description =
                    "Creates a lightweight or annotated tag on the specified commit (defaults to HEAD).",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "STRING",
            aspects = {})
    public String CreateTag(
            @ThingworxServiceParameter(
                            name = "TagName",
                            description = "Name of the tag to create",
                            baseType = "STRING")
                    String TagName,
            @ThingworxServiceParameter(
                            name = "Message",
                            description = "Optional tag message (creates annotated tag)",
                            baseType = "STRING")
                    String Message,
            @ThingworxServiceParameter(
                            name = "CommitID",
                            description = "Optional commit to tag (defaults to HEAD)",
                            baseType = "STRING")
                    String CommitID)
            throws Exception, GitAPIException {
        String str_CurrentMethodName = "CreateTag";
        if (isBlank(TagName)) {
            LogOperationResult(Const.ERR_NO_TAG_NAME, str_CurrentMethodName);
            return "CreateTag skipped: " + Const.ERR_PREFIX_CONFIG + Const.ERR_NO_TAG_NAME;
        }
        try {
            syncFromThingworx();
            Git myGitFolder = getGitObject("CreateTag");
            Repository repo = myGitFolder.getRepository();
            ObjectId commitId;
            if (hasText(CommitID)) {
                commitId = repo.resolve(CommitID);
                if (commitId == null) {
                    return "CreateTag Error: "
                            + String.format(Const.ERR_COMMIT_NOT_FOUND, CommitID);
                }
            } else {
                commitId = repo.resolve("HEAD");
            }
            RevCommit revCommit;
            try (RevWalk walk = new RevWalk(repo)) {
                revCommit = walk.parseCommit(commitId);
                walk.dispose();
            }
            Ref tagRef;
            if (hasText(Message)) {
                tagRef =
                        myGitFolder
                                .tag()
                                .setName(TagName)
                                .setMessage(Message)
                                .setObjectId(revCommit)
                                .call();
            } else {
                tagRef = myGitFolder.tag().setName(TagName).setObjectId(revCommit).call();
            }
            String str_LogResult =
                    String.format(Const.SUCCESS_TAG_CREATED, TagName, tagRef.getObjectId().name());
            LogOperationResult(str_LogResult, str_CurrentMethodName);
            return str_LogResult;
        } catch (Exception e) {
            String errMsg;
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                errMsg =
                        "CreateTag Error: "
                                + Const.ERR_PREFIX_GIT
                                + "Tag '"
                                + TagName
                                + "' already exists. Use a different tag name or delete the existing tag first.";
            } else {
                errMsg = buildErrorResult("CreateTag", e);
            }
            _logger.error(errMsg);
            try {
                LogOperationResult(errMsg, str_CurrentMethodName);
            } catch (Exception e1) {
                _logger.error(e1.toString());
            }
            return errMsg;
        }
    }

    @ThingworxServiceDefinition(
            name = "GetTagList",
            description = "Returns all tags in the repository.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.TagList.DataShape"})
    public InfoTable GetTagList() {
        _logger.trace("Entering Service: GetTagList");
        try {
            Git myGitFolder = getGitObject("GetTagList");
            Repository repo = myGitFolder.getRepository();
            InfoTable iftbl_TagList =
                    InfoTableInstanceFactory.createInfoTableFromDataShape("GIT.TagList.DataShape");
            List<Ref> tags = myGitFolder.tagList().call();
            for (Ref tagRef : tags) {
                ValueCollection vc = new ValueCollection();
                String tagName = tagRef.getName().replace("refs/tags/", "");
                vc.put("TagName", new StringPrimitive(tagName));
                ObjectId commitId = tagRef.getPeeledObjectId();
                if (commitId == null) {
                    commitId = tagRef.getObjectId();
                }
                vc.put("CommitID", new StringPrimitive(commitId != null ? commitId.name() : ""));
                String message = "";
                DateTime tagDate = new DateTime(0);
                if (tagRef.getPeeledObjectId() != null) {
                    try (RevWalk walk = new RevWalk(repo)) {
                        RevObject obj = walk.parseAny(tagRef.getObjectId());
                        if (obj instanceof RevTag) {
                            RevTag revTag = (RevTag) obj;
                            message = revTag.getShortMessage();
                            long epochSeconds =
                                    revTag.getTaggerIdent() != null
                                            ? revTag.getTaggerIdent()
                                                    .getWhenAsInstant()
                                                    .getEpochSecond()
                                            : 0L;
                            tagDate =
                                    epochSeconds > 0
                                            ? new DateTime((long) epochSeconds * 1000)
                                            : new DateTime(0);
                        }
                    }
                }
                vc.put("Message", new StringPrimitive(message));
                vc.put("Date", new DatetimePrimitive(tagDate));
                iftbl_TagList.addRow(vc);
            }
            _logger.trace("Exiting Service: GetTagList");
            return iftbl_TagList;
        } catch (Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            _logger.error("GetTagList failed: " + errors.toString());
            return new InfoTable();
        }
    }

    @ThingworxServiceDefinition(
            name = "DeleteTag",
            description = "Deletes a tag from the repository.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "NOTHING",
            aspects = {})
    public void DeleteTag(
            @ThingworxServiceParameter(
                            name = "TagName",
                            description = "Name of the tag to delete",
                            baseType = "STRING")
                    String TagName)
            throws Exception, GitAPIException {
        String str_CurrentMethodName = "DeleteTag";
        if (isBlank(TagName)) {
            _logger.warn("DeleteTag: TagName is required.");
            return;
        }
        try {
            Git myGitFolder = getGitObject("DeleteTag");
            myGitFolder.tagDelete().setTags("refs/tags/" + TagName).call();
            String str_LogResult = String.format(Const.SUCCESS_TAG_DELETED, TagName);
            LogOperationResult(str_LogResult, str_CurrentMethodName);
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            _logger.error("DeleteTag failed for tag '" + TagName + "': " + errors.toString());
            try {
                LogOperationResult(
                        "DeleteTag Error: "
                                + Const.ERR_PREFIX_GIT
                                + "Failed to delete tag '"
                                + TagName
                                + "'. "
                                + e.getMessage(),
                        str_CurrentMethodName);
            } catch (Exception e1) {
                _logger.error("LogOperationResult failed for DeleteTag: " + e1.toString());
            }
        }
    }

    private String buildErrorResult(String serviceName, Exception e) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        String errMsg = errors.toString();
        _logger.error(errMsg);
        String userFriendly;
        String msg = e.getMessage();
        if (msg != null) {
            if (msg.contains("Remote does not") || msg.contains("remote:")) {
                userFriendly =
                        Const.ERR_PREFIX_GIT + "Remote repository rejected the operation: " + msg;
            } else if (msg.contains("NotAuthorizedException")
                    || msg.contains("not authorized")
                    || msg.contains("401")
                    || msg.contains("Authentication")) {
                userFriendly =
                        Const.ERR_PREFIX_AUTH
                                + "Authentication failed. "
                                + String.format(Const.ERR_AUTH_FAILED, str_GitRepoURL);
            } else if (msg.contains("TransportException")
                    || msg.contains("Timeout")
                    || msg.contains("timeout")
                    || msg.contains("connect")
                    || msg.contains("Connection refused")) {
                userFriendly =
                        Const.ERR_PREFIX_NETWORK
                                + "Network error. "
                                + String.format(Const.ERR_NETWORK_FAILED, str_GitRepoURL);
            } else if (msg.contains("MergeConflict") || msg.contains("CheckoutConflict")) {
                userFriendly = Const.ERR_PREFIX_GIT + Const.ERR_MERGE_CONFLICT;
            } else {
                userFriendly =
                        Const.ERR_PREFIX_SYSTEM
                                + serviceName
                                + " failed: "
                                + (msg.length() > 200 ? msg.substring(0, 200) + "..." : msg);
            }
        } else {
            userFriendly =
                    Const.ERR_PREFIX_SYSTEM
                            + serviceName
                            + " failed with an unknown error. Check logs for details.";
        }
        return serviceName + " Error: " + userFriendly;
    }

    private ValueCollection getUserGpgKey(User user) throws Exception {
        if (user == null) return null;
        ValueCollection repositoryConfig = getGitRepoRemoteCredential(user);
        String fingerprint = primitiveString(repositoryConfig, Const.str_GpgKeyFingerprint);
        if (isBlank(fingerprint)) return null;
        Object property = user.getPropertyValue(Const.str_UserGpgKeys);
        if (!(property instanceof InfoTablePrimitive)) return null;
        InfoTable keys = ((InfoTablePrimitive) property).getValue();
        if (keys == null) return null;
        for (int i = 0; i < keys.getRowCount(); i++) {
            ValueCollection key = keys.getRow(i);
            if (fingerprint.equals(primitiveString(key, Const.str_GpgKeyFingerprint))) {
                if (repositoryConfig.getPrimitive(Const.str_SignCommits) != null)
                    key.put(
                            Const.str_SignCommits,
                            repositoryConfig.getPrimitive(Const.str_SignCommits));
                return key;
            }
        }
        return null;
    }

    private ValueCollection getGitRepoRemoteCredential(User us_currentUser) throws Exception {
        if (us_currentUser == null) return new ValueCollection();
        var propValue = us_currentUser.getPropertyValue(Const.str_GitCredentials);
        if (!(propValue instanceof InfoTablePrimitive)) return new ValueCollection();
        InfoTable iftbl_CredentialStore = ((InfoTablePrimitive) propValue).getValue();
        if (iftbl_CredentialStore == null) return new ValueCollection();
        for (int i = 0; i < iftbl_CredentialStore.getRowCount(); i++) {
            ValueCollection row = iftbl_CredentialStore.getRow(i);
            if (repositoryThingName().equals(primitiveString(row, "GitThing"))) return row;
        }
        return new ValueCollection();
    }
}
