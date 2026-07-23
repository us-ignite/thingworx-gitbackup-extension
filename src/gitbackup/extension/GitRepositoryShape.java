package gitbackup.extension;

import static gitbackup.extension.Values.hasText;
import static gitbackup.extension.Values.isBlank;
import static gitbackup.extension.Values.isTrue;
import static gitbackup.extension.Values.orDefault;
import static gitbackup.extension.Values.primitiveString;

import com.thingworx.data.util.InfoTableInstanceFactory;
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
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
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
                    "Disposing GitBackup Thing "
                            + getName()
                            + "; RepositoryCache contains: "
                            + RepositoryCache.getRegisteredKeys().size());
            RepositoryCache.clear();
            // System.gc();
            Thread.sleep(200);
            gitObject = null;
            String str_FolderPath = new File(srcRepo.getRootPath(), str_FileRepoPath).getPath();
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
        this.str_FileRepository = orDefault(fileRepo, Const.str_FileRepositoryDefaultValue);
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
                "1. GitBackup Thing: "
                        + this.getName()
                        + " initialize phase 1.1. PackedGitMMAP set false");
        // prevents creation of disk folder when no repo URL is configured
        if (hasText(str_GitRepoURL)) {
            _logger.warn(
                    "1. GitBackup Thing: "
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
                "1. GitBackup Thing: "
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
        syncFromThingworx();
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
                            ((PasswordPrimitive)
                                            vc_GpgKey.getPrimitive(Const.str_GpgKeyPassphrase))
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
                    "This will execute a push of all the files for the specific project. You might need to edit the global gitignore file to include file types you might want in the commit, like log files. This is usually stored in Windows in the the [user]/Documents/gitignore_global.txt ",
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
                            name = "Message",
                            description = "A message that will appear in the git for this commit",
                            baseType = "STRING")
                    String Message)
            throws Exception, GitAPIException {
        _logger.trace("Entering Service: Push");
        refreshConfiguration();
        syncFromThingworx();
        String str_CurrentMethodName = "Push";
        boolean bool_SignCommits = false;
        if (isBlank(str_GitRepoURL)) {
            _logger.warn(Const.ERR_NO_REPO_URL);
            return "Push Error: " + Const.ERR_PREFIX_CONFIG + Const.ERR_NO_REPO_URL_PUSH;
        }
        try {
            Thread.sleep(1000);
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
            String str_CommitterName =
                    primitiveString(vc_RepoCredentials, Const.str_GitCommitterName);
            String str_CommitterEmail =
                    primitiveString(vc_RepoCredentials, Const.str_GitCommitterEmail);
            if (isBlank(str_User)
                    || isBlank(str_Password)
                    || isBlank(str_CommitterName)
                    || isBlank(str_CommitterEmail)) {
                _logger.warn(Const.ERR_NO_CREDENTIALS);
                return "Push Error: " + Const.ERR_PREFIX_AUTH + Const.ERR_NO_CREDENTIALS;
            }
            myGitObject.add().addFilepattern(".").call();
            long endTimeAddFiles = System.nanoTime();
            BigDecimal durationTimeAddFiles =
                    new BigDecimal(
                                    (double) (endTimeAddFiles - endTimeOpenRepository)
                                            / (double) 1000000)
                            .setScale(3, RoundingMode.HALF_DOWN);
            myGitObject.add().addFilepattern(".").setUpdate(true).call();
            long endTimeAddAllFilesWithSetUpdate = System.nanoTime();
            BigDecimal durationTimeAddAllFilesWithSetUpdate =
                    new BigDecimal(
                                    (double) (endTimeAddAllFilesWithSetUpdate - endTimeAddFiles)
                                            / (double) 1000000)
                            .setScale(3, RoundingMode.HALF_DOWN);
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
                            ((PasswordPrimitive)
                                            vc_GpgKey.getPrimitive(Const.str_GpgKeyPassphrase))
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

            long endTimeCommitToLocalRepository = System.nanoTime();
            BigDecimal durationTimeCommitToLocalRepository =
                    new BigDecimal(
                                    (double)
                                                    (endTimeCommitToLocalRepository
                                                            - endTimeAddAllFilesWithSetUpdate)
                                            / (double) 1000000)
                            .setScale(3, RoundingMode.HALF_DOWN);
            if (!bool_SignCommits) {
                _logger.warn(Const.WARN_NO_SIGN_COMMITS);
            }

            CredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(str_User, str_Password);
            Iterable<PushResult> prList =
                    myGitObject
                            .push()
                            .setRemote("origin")
                            .setCredentialsProvider(credentialsProvider)
                            .call();
            long endTimePushFinish = System.nanoTime();
            BigDecimal durationTimePushFinish =
                    new BigDecimal(
                                    (double) (endTimePushFinish - endTimeCommitToLocalRepository)
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
                        if (status == RemoteRefUpdate.Status.REJECTED_OTHER_REASON
                                && !bool_SignCommits) {
                            hint += Const.ERR_PUSH_SIGNING_REQUIRED;
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
                            + "#2.AddFiles: "
                            + durationTimeAddFiles
                            + "#3.AddAllDeletedFiles: "
                            + durationTimeAddAllFilesWithSetUpdate
                            + "#4.CommitToLocalRepository: "
                            + durationTimeCommitToLocalRepository
                            + "#5.Push: "
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
                String hint = "";
                if (!bool_SignCommits) {
                    hint = Const.ERR_PUSH_SIGNING_REQUIRED;
                }
                errMsg = "Push rejected by remote server (pre-receive hook)." + hint;
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
            aspects = {"isEntityDataShape:true", "dataShape:GIT.GpgKeyVerificationResult.DataShape"})
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
                            baseType = "BOOLEAN")
                    Boolean Force) {
        String str_CurrentMethodName = "Pull";
        try {
            refreshConfiguration();
            syncFromThingworx();
            _logger.warn("Starting Pull for GitBackup Thing: " + this.getName());
            Thread.sleep(500);
            Git myGitFolder = getGitObject("Pull");
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
            _logger.warn("Finished Pull for GitBackup Thing: " + this.getName());
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
            String str_FolderPath = new File(srcRepo.getRootPath(), str_FileRepoPath).getPath();
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
                    InfoTableInstanceFactory.createInfoTableFromDataShape("GIT.BranchList.DataShape");
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

    private void syncFromThingworx() {
        try {
            if (isBlank(str_ProjectName)) {
                _logger.trace(Const.WARN_NO_PROJECT_SKIP);
                return;
            }
            Thing utilityThing =
                    (Thing)
                            EntityUtilities.findEntity(
                                    Const.str_UtilityThingName, ThingworxRelationshipTypes.Thing);
            if (utilityThing == null) return;
            ValueCollection params = new ValueCollection();
            params.put("GitThingName", new StringPrimitive(this.getName()));
            utilityThing.processServiceRequest("SyncProjectToRepository", params);
        } catch (Exception e) {
            _logger.error(
                    String.format(Const.WARN_SYNC_FAILED, this.getName() + ": " + e.getMessage()));
        }
    }

    /** Re-read configuration written immediately before a service invocation. */
    private void refreshConfiguration() {
        try {
            Thing targetThing = resolveTargetThing();
            if (targetThing != null) {
                ValueCollection params = new ValueCollection();
                params.put("tableName", new StringPrimitive(Const.str_ConfTableName));
                Object rawTable = targetThing.processServiceRequest("GetConfigurationTable", params);
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
                            orDefault(
                                    primitiveString(row, Const.str_FileRepository),
                                    Const.str_FileRepositoryDefaultValue);
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
                        Const.str_FileRepositoryDefaultValue);
        this.str_FileRepoPath =
                (String)
                        getConfigurationSetting(Const.str_ConfTableName, Const.str_RepoPathName);
        this.str_CurrentBranchOrCommit =
                (String)
                        getConfigurationSetting(Const.str_ConfTableName, Const.str_InitialBranch);
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
            Thing utilityThing =
                    (Thing)
                            EntityUtilities.findEntity(
                                    Const.str_UtilityThingName, ThingworxRelationshipTypes.Thing);
            if (utilityThing == null) return;
            ValueCollection params = new ValueCollection();
            params.put("GitThingName", new StringPrimitive(this.getName()));
            params.put("entityPath", new StringPrimitive(""));
            params.put("ignoreDependencies", new BooleanPrimitive(true));
            utilityThing.processServiceRequest("ImportProjectEntities", params);
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
                "Retrieving GitBackup Thing : "
                        + this.getName()
                        + "; Object is: "
                        + (gitObject != null)
                        + "; Method: "
                        + str_CallerMethod);
        if (gitObject == null) {
            FileRepositoryThing srcRepo =
                    (FileRepositoryThing)
                            EntityUtilities.findEntity(
                                    str_FileRepository, ThingworxRelationshipTypes.Thing);
            String str_FolderPath = new File(srcRepo.getRootPath(), str_FileRepoPath).getPath();
            Git myGitObject = openOrCreate(new File(str_FolderPath));
            StoredConfig config = myGitObject.getRepository().getConfig();
            config.setString("remote", "origin", "url", str_GitRepoURL);
            config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
            config.setString("remote", "origin", "prune", "true");
            config.setString("core", null, "autocrlf", "input");
            // prevents Bitmap creation during GarbageCollection
            config.setBoolean("repack", null, "writeBitmaps", false);
            config.save();
            _logger.warn(
                    "GitBackup Thing: "
                            + this.getName()
                            + " Git object found null, initialized. Git object created and stored internally for future use. "
                            + "; Method: "
                            + str_CallerMethod);
            gitObject = myGitObject;
        }
        return gitObject;
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
                    key.put(Const.str_SignCommits, repositoryConfig.getPrimitive(Const.str_SignCommits));
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
