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
import com.thingworx.security.users.User;
import com.thingworx.system.ContextType;
import com.thingworx.things.Thing;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.errors.NotMergedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.CheckoutEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.Signers;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS.FileStoreAttributes;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.us_ignite.thingworx.dap.DapBaseType;
import org.us_ignite.thingworx.dap.DapProperty;
import org.us_ignite.thingworx.dap.DapServicePayload;
import org.us_ignite.thingworx.dap.DapThingShape;
import org.us_ignite.thingworx.dap.runtime.DapResults;
import org.us_ignite.thingworx.dap.runtime.DapValues;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.BranchListSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.CommitInfoSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.CommitLogSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.CommitTreeEntrySpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.ConflictFileSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.ConflictStateSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.CurrentBranchStatusSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.FileContentSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.HistoryRefSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.ReflogEntrySpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.StatusSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.StringResultSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.TagListSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.TrackedProjectSpec;

/**
 * ThingWorx repository implementation backed by an on-disk JGit repository.
 *
 * <p>The repository location is resolved from this Thing, which is itself a FileRepository, and its
 * configured path. Services on this class operate on the current repository state and return either
 * a status message or the InfoTable shape declared by the corresponding ThingWorx service
 * annotation.
 */
@ThingworxBaseTemplateDefinition(name = "GenericThing")
@DapThingShape(
        name = "GIT.Repository.ThingShape",
        description =
                "Base ThingShape for GIT Repository Things providing core Git operations (commit, push, pull, branch, tag, etc.).",
        properties = {
            @DapProperty(
                    name = "GitRepoURL",
                    baseType = DapBaseType.STRING,
                    ordinal = 1,
                    category = "Repository",
                    description = "Remote Git repository URL"),
            @DapProperty(
                    name = "RepoPath",
                    baseType = DapBaseType.STRING,
                    ordinal = 2,
                    category = "Repository",
                    description = "Repository path within this FileRepository"),
            @DapProperty(
                    name = "BranchName",
                    baseType = DapBaseType.STRING,
                    ordinal = 4,
                    category = "Repository",
                    defaultValue = "main",
                    description = "Initial Git branch"),
            @DapProperty(
                    name = "UseProxy",
                    baseType = DapBaseType.BOOLEAN,
                    ordinal = 5,
                    category = "Repository",
                    defaultValue = "false",
                    description = "Use an HTTP proxy"),
            @DapProperty(
                    name = "ProxyURL",
                    baseType = DapBaseType.STRING,
                    ordinal = 6,
                    category = "Repository",
                    description = "HTTP proxy URL"),
            @DapProperty(
                    name = "ProxyPort",
                    baseType = DapBaseType.INTEGER,
                    ordinal = 7,
                    category = "Repository",
                    defaultValue = "0",
                    description = "HTTP proxy port"),
            @DapProperty(
                    name = "LocalizationTokensPrefix",
                    baseType = DapBaseType.STRING,
                    ordinal = 8,
                    category = "Repository",
                    description = "Localization token prefix"),
            @DapProperty(
                    name = "ProjectName",
                    baseType = DapBaseType.STRING,
                    ordinal = 9,
                    category = "Repository",
                    description =
                            "Legacy single ThingWorx project; superseded by TrackedProjects once that list is edited"),
            @DapProperty(
                    name = "TrackedProjects",
                    baseType = DapBaseType.INFOTABLE,
                    dataShape = "GIT.TrackedProject.DataShape",
                    ordinal = 10,
                    category = "Repository",
                    description = "ThingWorx projects synchronized with this repository")
        })
public class GitRepositoryShape extends Thing {
    // set background file system resolution to false and enable debugging
    static {
        FileStoreAttributes.setBackground(false);
    }

    private static final long serialVersionUID = -6500080561143490845L;

    private static Logger log =
            LogUtilities.getInstance().getApplicationLogger(GitRepositoryShape.class);

    public GitRepositoryShape() {}

    @Override
    protected void stopThing(ContextType ctx) throws Exception {
        super.stopThing(null);
    }

    @Override
    public void dispose() throws Exception {

        log.warn(
                "Disposing GIT Repository Thing "
                        + getName()
                        + "; RepositoryCache contains: "
                        + RepositoryCache.getRegisteredKeys().size());
        var fileRepo = new FileRepositoryManager(resolveTargetThing());
        String FolderPath = fileRepo.fileRepository().getRootPath();
        File folder = new File(FolderPath);
        if (folder.exists()) {
            try (var paths = Files.walk(folder.toPath())) {
                List<java.nio.file.Path> pathsToDelete =
                        paths.sorted(Comparator.reverseOrder())
                                .collect(java.util.stream.Collectors.toList());
                for (java.nio.file.Path path : pathsToDelete) {
                    Files.delete(path);
                }
            }
        }
        super.dispose();
    }

    @Override
    protected void initializeThing(ContextType ctx) throws Exception {

        WindowCacheConfig wconfig = new WindowCacheConfig();
        wconfig.setPackedGitMMAP(false);
        wconfig.install();
        var fileRepo = new FileRepositoryManager(resolveTargetThing());
        fileRepo.git();
        String JGIT_version =
                org.eclipse.jgit.lib.Repository.class.getPackage().getImplementationVersion();
        log.warn(
                "1. GIT Repository Thing: "
                        + this.getName()
                        + " final initialize phase ended. Jgit library version: "
                        + JGIT_version);
        fileRepo.close();
        super.initializeThing(null);
    }

    /**
     * Syncs the configured ThingWorx project, then commits only the files already staged in the Git
     * index. Exported project files are left unstaged.
     *
     * @param Message commit message; required
     * @return a success message, or a prefixed error message when configuration, authentication,
     *     signing, synchronization, or Git commit processing fails
     */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "Commit",
            description =
                    "Syncs the ThingWorx project entities, then commits only files already staged in the Git index. Does not stage exported files or push to the remote.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable Commit(
            @ThingworxServiceParameter(
                            name = "Message",
                            description = "The commit message",
                            baseType = "STRING")
                    String Message) {
        return commitIndex("Commit", Message, true);
    }

    /**
     * Commits only the files already staged in the Git index. Does not export project entities or
     * push to the remote.
     *
     * @param Message commit message; required
     * @return a success message, or a prefixed error message when configuration, signing, or Git
     *     commit processing fails
     */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "CommitStaged",
            description =
                    "Commits only files already staged in the Git index. Does not export project entities or push to the remote.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable CommitStaged(
            @ThingworxServiceParameter(
                            name = "Message",
                            description = "The commit message",
                            baseType = "STRING")
                    String Message) {
        return commitIndex("CommitStaged", Message, false);
    }

    /**
     * Signs {@code commitCmd} with the current user's GPG key selected for this repository, when
     * one is configured. The caller must clear the returned signer after the commit.
     *
     * @return the signer, or {@code null} when the commit is not signed
     */
    private PastedKeyGpgSigner configureSigning(CommitCommand commitCmd) throws Exception {
        User currentUser = UserUtilities.findUser(UserUtilities.getCurrentUser());
        GitUserContextManager userContext = new GitUserContextManager();
        PastedKeyGpgSigner gpgSigner = null;
        String GpgPrivateKey = null;
        String GpgPassphrase = null;
        ValueCollection GpgKey = null;
        if (currentUser != null) {
            ValueCollection repositoryConfig =
                    userContext.credentialsFor(resolveTargetThing().getName());
            String fingerprint = repositoryConfig.getStringValue(Const.GpgKeyFingerprint);
            if (fingerprint != null && !fingerprint.isBlank()) {
                Object property = currentUser.getPropertyValue(Const.UserGpgKeys);
                if (property instanceof InfoTablePrimitive) {
                    InfoTable keys = ((InfoTablePrimitive) property).getValue();
                    if (keys != null) {
                        for (int i = 0; i < keys.getRowCount(); i++) {
                            ValueCollection key = keys.getRow(i);
                            if (fingerprint.equals(key.getStringValue(Const.GpgKeyFingerprint))) {
                                GpgKey = key;
                                break;
                            }
                        }
                    }
                }
                if (GpgKey == null)
                    log.warn(
                            "Repository {} references GPG key {} which is not owned by the current user; commit will not be signed.",
                            resolveTargetThing().getName(),
                            fingerprint);
            }
        }
        if (GpgKey != null) {
            GpgPrivateKey =
                    PastedKeyGpgSigner.normalizePrivateKey(
                            ((PasswordPrimitive) GpgKey.getPrimitive(Const.GpgPrivateKey))
                                    .getValue());
            GpgPassphrase =
                    ((PasswordPrimitive) GpgKey.getPrimitive(Const.GpgKeyPassphrase)).getValue();
        }

        if (GpgPrivateKey != null && !GpgPrivateKey.isBlank()) {
            gpgSigner = new PastedKeyGpgSigner(GpgPrivateKey, GpgPassphrase);
            Signers.set(GpgConfig.GpgFormat.OPENPGP, gpgSigner);
            commitCmd.setSign(true).setSigningKey(null).setSigner(gpgSigner);
            commitCmd.setCredentialsProvider(
                    new UsernamePasswordCredentialsProvider(null, GpgPassphrase));
        }
        return gpgSigner;
    }

    private InfoTable commitIndex(String serviceName, String Message, boolean exportProject) {
        log.trace("Entering Service: " + serviceName);
        FileRepositoryManager fileRepo = null;
        PastedKeyGpgSigner gpgSigner = null;
        try {
            if (Message == null || Message.isBlank()) {
                GitActionLogger.logAction(
                        serviceName,
                        resolveTargetThing().getName(),
                        Const.ERR_COMMIT_MESSAGE_REQUIRED);
                return DapResults.failure(
                        serviceName,
                        serviceName
                                + " Error: "
                                + Const.ERR_PREFIX_CONFIG
                                + Const.ERR_COMMIT_MESSAGE_REQUIRED,
                        StringResultContract.SERVICE_RESULT);
            }

            GitUserContextManager userContext = new GitUserContextManager();
            ValueCollection RepoCredentials =
                    userContext.credentialsFor(resolveTargetThing().getName());
            String CommitterName = RepoCredentials.getStringValue(Const.GitCommitterName);
            String CommitterEmail = RepoCredentials.getStringValue(Const.GitCommitterEmail);
            if (CommitterName == null
                    || CommitterName.isBlank()
                    || CommitterEmail == null
                    || CommitterEmail.isBlank()) {
                GitActionLogger.logAction(
                        serviceName,
                        resolveTargetThing().getName(),
                        "no committer name/email configured");
                return DapResults.failure(
                        serviceName,
                        serviceName + " Error: " + Const.ERR_PREFIX_CONFIG + Const.ERR_NO_COMMITTER,
                        StringResultContract.SERVICE_RESULT);
            }

            fileRepo = new FileRepositoryManager(resolveTargetThing());
            if (exportProject) fileRepo.exportProjectEntities();
            Git myGitObject = fileRepo.git();
            // A paused merge or rebase is completed from the Conflicts page, which also
            // restores carried local changes; a plain commit here would skip that.
            ConflictWorkflow.requireNotInProgress(myGitObject);
            org.eclipse.jgit.api.Status status = myGitObject.status().call();
            if (status.getAdded().isEmpty()
                    && status.getChanged().isEmpty()
                    && status.getRemoved().isEmpty()) {
                GitActionLogger.logAction(
                        serviceName, resolveTargetThing().getName(), Const.ERR_NO_STAGED_CHANGES);
                return DapResults.failure(
                        serviceName,
                        serviceName
                                + " Error: "
                                + Const.ERR_PREFIX_GIT
                                + Const.ERR_NO_STAGED_CHANGES,
                        StringResultContract.SERVICE_RESULT);
            }

            var commitCmd =
                    myGitObject
                            .commit()
                            .setMessage(Message)
                            .setCommitter(CommitterName, CommitterEmail);

            gpgSigner = configureSigning(commitCmd);

            commitCmd.call();

            String Result = String.format(Const.SUCCESS_COMMIT, Message);
            GitActionLogger.logAction(serviceName, resolveTargetThing().getName(), Result);
            return DapResults.success(serviceName, Result, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    serviceName,
                    resolveTargetThing().getName(),
                    e == null ? "" : String.valueOf(e.getMessage()));
            return DapResults.failure(serviceName, e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (gpgSigner != null) gpgSigner.clearSensitiveData();
            if (fileRepo != null) fileRepo.close();
        }
    }

    /**
     * Pushes existing local commits to a remote. It exports the configured project before the
     * successFromString and imports the FileRepository afterward; it does not stage files or create
     * a commit. When {@code Remote} is blank, the branch upstream or {@code origin} is used.
     *
     * @param Remote remote name; blank selects the branch upstream remote or {@code origin}
     * @param BranchName optional local branch; blank selects the current branch
     * @param RemoteBranchName optional remote branch; blank uses the source branch name
     * @param SetUpstream records the remote tracking branch after a successful explicit push
     * @return a push summary, or a prefixed error message for missing configuration, credentials,
     *     transport failures, or remote rejection
     */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "Push",
            description =
                    "Exports the ThingWorx project, pushes existing local commits using the current/upstream branch or explicit local and remote branch, then imports the FileRepository tree. It does not stage files or create commits.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable Push(
            @ThingworxServiceParameter(
                            name = "Remote",
                            description = "Optional remote name; defaults to origin",
                            baseType = "STRING")
                    String Remote,
            @ThingworxServiceParameter(
                            name = "BranchName",
                            description = "Optional local branch; blank uses the current branch",
                            baseType = "STRING")
                    String BranchName,
            @ThingworxServiceParameter(
                            name = "RemoteBranchName",
                            description =
                                    "Optional remote branch; blank uses the local branch name",
                            baseType = "STRING")
                    String RemoteBranchName,
            @ThingworxServiceParameter(
                            name = "SetUpstream",
                            description = "Sets the local branch upstream after a successful push",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean SetUpstream,
            @ThingworxServiceParameter(
                            name = "Force",
                            description = "Force the push to update the remote branch",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean Force) {
        log.trace("Entering Service: Push");

        String CurrentMethodName = "Push";
        try {
            long startTimePush = System.nanoTime();
            long endTimeOpenRepository = System.nanoTime();
            BigDecimal durationTimeOpenRepository =
                    new BigDecimal(
                                    (double) (endTimeOpenRepository - startTimePush)
                                            / (double) 1000000)
                            .setScale(3, RoundingMode.HALF_DOWN);
            ValueCollection RepoCredentials =
                    new GitUserContextManager().credentialsFor(resolveTargetThing().getName());
            String User = RepoCredentials.getStringValue(Const.GitCommitterUser);
            String Password = RepoCredentials.getStringValue(Const.GitCommitterPassword);
            if (User.isBlank() || Password.isBlank()) {

                GitActionLogger.logAction(
                        "Push", resolveTargetThing().getName(), "no credentials configured");
                return DapResults.failure(
                        "Push",
                        "Push Error: " + Const.ERR_PREFIX_AUTH + Const.ERR_NO_CREDENTIALS,
                        StringResultContract.SERVICE_RESULT);
            }
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git myGitObject = fileRepo.git();
            Repository repository = myGitObject.getRepository();
            String sourceBranch =
                    BranchName == null || BranchName.isBlank()
                            ? repository.getBranch()
                            : BranchName;
            if (sourceBranch.isBlank() || "HEAD".equals(sourceBranch)) {
                fileRepo.close();
                GitActionLogger.logAction(
                        "Push",
                        resolveTargetThing().getName(),
                        "detached HEAD requires explicit BranchName");
                return DapResults.failure(
                        "Push",
                        "Push Error: "
                                + Const.ERR_PREFIX_CONFIG
                                + "A detached HEAD requires an explicit BranchName.",
                        StringResultContract.SERVICE_RESULT);
            }
            if (repository.findRef("refs/heads/" + sourceBranch) == null) {
                fileRepo.close();
                GitActionLogger.logAction(
                        "Push",
                        resolveTargetThing().getName(),
                        "local branch '" + sourceBranch + "' not found");
                return DapResults.failure(
                        "Push",
                        "Push Error: "
                                + Const.ERR_PREFIX_CONFIG
                                + "Local branch '"
                                + sourceBranch
                                + "' was not found.",
                        StringResultContract.SERVICE_RESULT);
            }
            String upstreamRemote =
                    repository.getConfig().getString("branch", sourceBranch, "remote");
            String remote =
                    Remote == null || Remote.isBlank()
                            ? (upstreamRemote == null || upstreamRemote.isBlank()
                                    ? "origin"
                                    : upstreamRemote)
                            : Remote;
            String upstreamMerge =
                    repository.getConfig().getString("branch", sourceBranch, "merge");
            String destinationBranch =
                    RemoteBranchName == null || RemoteBranchName.isBlank()
                            ? (remote.equals(upstreamRemote)
                                            && upstreamMerge != null
                                            && upstreamMerge.startsWith("refs/heads/")
                                    ? upstreamMerge.substring("refs/heads/".length())
                                    : sourceBranch)
                            : RemoteBranchName;
            CredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(User, Password);
            var pushCommand =
                    myGitObject
                            .push()
                            .setRemote(remote)
                            .setCredentialsProvider(credentialsProvider)
                            .setForce(Boolean.TRUE.equals(Force));
            pushCommand.setRefSpecs(
                    new RefSpec("refs/heads/" + sourceBranch + ":refs/heads/" + destinationBranch));
            Iterable<PushResult> prList = pushCommand.call();
            long endTimePushFinish = System.nanoTime();
            BigDecimal durationTimePushFinish =
                    new BigDecimal(
                                    (double) (endTimePushFinish - endTimeOpenRepository)
                                            / (double) 1000000)
                            .setScale(3, RoundingMode.HALF_DOWN);
            String LogResult = "";
            String pushError = null;
            for (PushResult pr : prList) {
                for (RemoteRefUpdate update : pr.getRemoteUpdates()) {
                    LogResult += update;
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
            LogResult +=
                    " Debug Timings (ms): #1.OpenGit: "
                            + durationTimeOpenRepository
                            + "#2.Push: "
                            + durationTimePushFinish;

            if (pushError != null) {
                fileRepo.close();
                GitActionLogger.logAction("Push", resolveTargetThing().getName(), pushError);
                return DapResults.failure(
                        "Push",
                        "Push Error: " + Const.ERR_PREFIX_GIT + pushError,
                        StringResultContract.SERVICE_RESULT);
            }
            if (SetUpstream.booleanValue()) {
                StoredConfig config = repository.getConfig();
                config.setString("branch", sourceBranch, "remote", remote);
                config.setString(
                        "branch", sourceBranch, "merge", "refs/heads/" + destinationBranch);
                config.save();
            }
            fileRepo.importProjectEntities();
            fileRepo.close();

            GitActionLogger.logAction("Push", resolveTargetThing().getName(), LogResult);
            return DapResults.success("Push", LogResult, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "Push",
                    resolveTargetThing().getName(),
                    e == null ? "" : String.valueOf(e.getMessage()));
            return DapResults.failure(CurrentMethodName, e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "SetGPGKeyForSigning",
            description =
                    "Selects or clears the current user's signing key for this repository by fingerprint or label",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable SetGPGKeyForSigning(
            @ThingworxServiceParameter(
                            name = "GpgKeyFingerprint",
                            description =
                                    "Owned GPG fingerprint; when both fingerprint and label are supplied both must match; blank disables signing",
                            baseType = "STRING")
                    String fingerprint,
            @ThingworxServiceParameter(
                            name = "GpgKeyLabel",
                            description =
                                    "Optional key label; when both fingerprint and label are supplied both must match",
                            baseType = "STRING")
                    String label) {
        try {
            GitUserContextManager userContext = new GitUserContextManager();
            userContext.requireUser();
            boolean hasFingerprint = fingerprint != null && !fingerprint.isBlank();
            boolean hasLabel = label != null && !label.isBlank();
            String fingerprintToStore = "";
            if (hasFingerprint || hasLabel) {
                userContext.validateGpgKeyOwnership(fingerprint, label);
                ValueCollection row = userContext.gpgKeyByFingerprintOrLabel(fingerprint, label);
                if (row != null) {
                    String resolved = row.getStringValue(Const.GpgKeyFingerprint);
                    fingerprintToStore = resolved != null ? resolved : "";
                } else if (hasFingerprint) {
                    fingerprintToStore = fingerprint;
                }
            }
            userContext.setRepositoryGpgKey(resolveTargetThing().getName(), fingerprintToStore);
            return DapResults.success(
                    "SetGPGKeyForSigning",
                    "GPG signing key updated.",
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {

            return DapResults.failure(
                    "SetGPGKeyForSigning", e, StringResultContract.SERVICE_RESULT);
        }
    }

    /** Fetches and integrates changes from the configured remote repository. */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "Pull",
            description = "Pulls the last commit to the File Repository path",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Fetches and integrates changes from the configured remote, then imports the synchronized
     * project back into ThingWorx. A force pull resets the local branch to the fetched remote state
     * before importing. An unborn local repository is bootstrapped without exporting untracked
     * project files first.
     *
     * @param Force when true, discard local divergence and reset to the fetched remote state
     * @return a pull summary, or a prefixed error message; conflicts are reported rather than
     *     silently discarded
     */
    public InfoTable Pull(
            @ThingworxServiceParameter(
                            name = "Force",
                            description = "Forces a hard reset instead of a normal pull",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean Force) {
        String CurrentMethodName = "Pull";
        FileRepositoryManager fileRepo = null;
        try {
            ValueCollection RepoCredentials =
                    new GitUserContextManager().credentialsFor(resolveTargetThing().getName());
            String User = RepoCredentials.getStringValue(Const.GitCommitterUser);
            String Password = RepoCredentials.getStringValue(Const.GitCommitterPassword);
            if (User == null || User.isBlank() || Password == null || Password.isBlank()) {
                GitActionLogger.logAction(
                        "Pull", resolveTargetThing().getName(), "no credentials configured");
                return DapResults.failure(
                        CurrentMethodName,
                        "Pull Error: " + Const.ERR_PREFIX_AUTH + Const.ERR_NO_CREDENTIALS,
                        StringResultContract.SERVICE_RESULT);
            }
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git myGitFolder = fileRepo.git();
            // An unborn repository has no local Git state to preserve. Exporting
            // ThingWorx entities before the first checkout would create untracked
            // files that conflict with the remote tree during bootstrap.
            if (myGitFolder.getRepository().resolve("HEAD") != null) {
                fileRepo.exportProjectEntities();
            }
            CredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(User, Password);
            String LogResult;
            if (Boolean.TRUE.equals(Force)) {
                String target = ForcePull.resetToOrigin(myGitFolder, credentialsProvider);
                LogResult = "Force pull reset to " + target + ".";
            } else {
                PullResult[] pulled = new PullResult[1];
                ConflictWorkflow.Outcome outcome =
                        ConflictWorkflow.run(
                                myGitFolder,
                                "Pull",
                                () -> {
                                    pulled[0] =
                                            myGitFolder
                                                    .pull()
                                                    .setRemote("origin")
                                                    .setCredentialsProvider(credentialsProvider)
                                                    .call();
                                    return ConflictWorkflow.outcome(pulled[0]);
                                });
                if (outcome == ConflictWorkflow.Outcome.CONFLICTS) {
                    return conflictResult(CurrentMethodName, "Pull", myGitFolder);
                }
                LogResult =
                        String.format(
                                Const.SUCCESS_PULL,
                                "Successful",
                                pulled[0] == null ? "" : pulled[0].toString());
            }
            // Always import the configured project - sync is invisible to users.
            // Import is private (FileRepositoryManager) and not exposed as a service.
            try {
                boolean imported = fileRepo.importProjectEntities();
                if (!imported) {
                    log.warn(
                            "Pull: no XML entities found under the tracked project folders ({}) - nothing to import.",
                            fileRepo.trackedProjectsLabel());
                }
            } catch (Exception importEx) {
                log.error(
                        "Pull: import of projects '" + fileRepo.trackedProjectsLabel() + "' failed",
                        importEx);
                String failureMessage =
                        "Git Pull completed, but importing entities into ThingWorx failed for projects '"
                                + fileRepo.trackedProjectsLabel()
                                + "': "
                                + importEx.getMessage();
                LogResult += " Import error: " + importEx.getMessage();
                GitActionLogger.logAction("Pull", resolveTargetThing().getName(), failureMessage);
                return DapResults.failure(
                        CurrentMethodName, failureMessage, StringResultContract.SERVICE_RESULT);
            }

            GitActionLogger.logAction("Pull", resolveTargetThing().getName(), LogResult);
            return DapResults.success(
                    CurrentMethodName, LogResult, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "Pull",
                    resolveTargetThing().getName(),
                    e == null ? "" : String.valueOf(e.getMessage()));
            return DapResults.failure(CurrentMethodName, e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) {
                try {
                    fileRepo.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @DapServicePayload(StringResultSpec.class)
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
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Fetches refs from the configured remote without merging them or importing entities into
     * ThingWorx. The remote-tracking refs are updated in the local repository.
     *
     * @param Remote remote name; blank selects {@code origin}
     * @return a fetch summary or a prefixed error message
     */
    public InfoTable Fetch(
            @ThingworxServiceParameter(
                            name = "Remote",
                            description = "Optional remote name; defaults to origin",
                            baseType = "STRING",
                            aspects = {"defaultValue:origin"})
                    String Remote) {
        String CurrentMethodName = "Fetch";
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());

            Git myGitFolder = fileRepo.git();
            ValueCollection RepoCredentials =
                    new GitUserContextManager().credentialsFor(resolveTargetThing().getName());
            String User = RepoCredentials.getStringValue(Const.GitCommitterUser);
            String Password = RepoCredentials.getStringValue(Const.GitCommitterPassword);
            if (User.isBlank() || Password.isBlank()) {

                GitActionLogger.logAction(
                        "Fetch", resolveTargetThing().getName(), "no credentials configured");
                return DapResults.failure(
                        CurrentMethodName,
                        "Fetch Error: " + Const.ERR_PREFIX_AUTH + Const.ERR_NO_CREDENTIALS,
                        StringResultContract.SERVICE_RESULT);
            }
            CredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(User, Password);
            String remote = Remote == null || Remote.isBlank() ? "origin" : Remote;
            myGitFolder
                    .fetch()
                    .setRemote(remote)
                    .setCredentialsProvider(credentialsProvider)
                    .call();
            String LogResult = "Fetch from '" + remote + "' completed successfully.";

            GitActionLogger.logAction("Fetch", resolveTargetThing().getName(), LogResult);
            return DapResults.success(
                    CurrentMethodName, LogResult, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "Fetch",
                    resolveTargetThing().getName(),
                    e == null ? "" : String.valueOf(e.getMessage()));
            return DapResults.failure(CurrentMethodName, e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "BranchCreate",
            description =
                    "Creates a new local branch from an optional start point (commit, branch, or tag) without switching to it.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Creates a local branch without checking it out. If a start point is omitted, the branch is
     * created from {@code HEAD}. The configured ThingWorx project is exported before and imported
     * after the branch successFromString so the FileRepository remains synchronized.
     *
     * @param BranchName name of the branch to create
     * @param StartPoint optional commit, branch, or tag; blank defaults to {@code HEAD}
     * @return the created ref summary or a prefixed error message
     */
    public InfoTable BranchCreate(
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
                    String StartPoint) {
        log.trace("Entering Service: BranchCreate");
        if (BranchName.isBlank()) {
            String errMsg =
                    "BranchCreate Error: " + Const.ERR_PREFIX_CONFIG + "BranchName is required.";

            GitActionLogger.logAction("BranchCreate", resolveTargetThing().getName(), errMsg);
            return DapResults.failure("BranchCreate", errMsg, StringResultContract.SERVICE_RESULT);
        }
        try {

            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git myGitFolder = fileRepo.git();
            StartPoint = (StartPoint == null || StartPoint.isBlank()) ? "HEAD" : StartPoint;
            Ref branchRef =
                    myGitFolder.branchCreate().setName(BranchName).setStartPoint(StartPoint).call();
            fileRepo.importProjectEntities();

            log.trace("Exiting Service: BranchCreate");
            String BranchCreateLogResult = branchRef.toString();
            GitActionLogger.logAction(
                    "BranchCreate", resolveTargetThing().getName(), BranchCreateLogResult);
            return DapResults.success(
                    "BranchCreate", BranchCreateLogResult, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "BranchCreate",
                    resolveTargetThing().getName(),
                    e == null ? "" : String.valueOf(e.getMessage()));
            return DapResults.failure(
                    "BranchCreate",
                    "BranchCreate Error: "
                            + Const.ERR_PREFIX_GIT
                            + "Branch '"
                            + BranchName
                            + "' already exists. Use a different branch name or delete the existing branch first.",
                    StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "Checkout",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Checks out a local branch, commit, or an available remote branch. Project entities are
     * exported before checkout and imported afterward, so the ThingWorx project is synchronized
     * with the resulting tree. A checkout may leave the repository detached when the target is a
     * commit rather than a branch.
     *
     * @param BranchNameOrCommit branch, remote branch, tag, or commit to check out
     */
    public InfoTable Checkout(
            @ThingworxServiceParameter(
                            name = "BranchNameOrCommit",
                            description =
                                    "Switches the working tree to the specified branch. This is a wrapper on top of checkout <branch>.It does not autocreate new branches.",
                            baseType = "STRING")
                    String BranchNameOrCommit,
            @ThingworxServiceParameter(
                            name = "CreateBranch",
                            description = "Creates a new branch before checkout",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean CreateBranch,
            @ThingworxServiceParameter(
                            name = "StartPoint",
                            description = "Optional start point when CreateBranch is true",
                            baseType = "STRING")
                    String StartPoint,
            @ThingworxServiceParameter(
                            name = "Force",
                            description = "Forces checkout over conflicting working-tree changes",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean Force) {
        log.trace("Entering Service: Checkout");
        if (BranchNameOrCommit.isBlank()) {

            return DapResults.failure(
                    "Checkout",
                    "Checkout: BranchNameOrCommit is required.",
                    StringResultContract.SERVICE_RESULT);
        }
        String CurrentMethodName = "Checkout";
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git myGitFolder = fileRepo.git();
            Ref[] checkedOut = new Ref[1];
            ConflictWorkflow.GitOperation checkoutOperation =
                    () -> {
                        try {
                            var checkout = myGitFolder.checkout().setName(BranchNameOrCommit);
                            if (CreateBranch.booleanValue()) {
                                checkout.setCreateBranch(true)
                                        .setStartPoint(StartPoint.isBlank() ? "HEAD" : StartPoint);
                            }
                            checkout.setForced(Boolean.TRUE.equals(Force));
                            checkedOut[0] = checkout.call();
                        } catch (RefNotFoundException ex) {
                            if (CreateBranch.booleanValue()) throw ex;
                            if (myGitFolder
                                            .getRepository()
                                            .findRef("refs/remotes/origin/" + BranchNameOrCommit)
                                    == null) {
                                throw ex;
                            }
                            log.warn(
                                    "Checkout: creating tracking branch from origin/"
                                            + BranchNameOrCommit);
                            checkedOut[0] =
                                    myGitFolder
                                            .checkout()
                                            .setCreateBranch(true)
                                            .setName(BranchNameOrCommit)
                                            .setUpstreamMode(SetupUpstreamMode.TRACK)
                                            .setStartPoint("origin/" + BranchNameOrCommit)
                                            .call();
                        }
                        return ConflictWorkflow.Outcome.COMPLETED;
                    };
            if (runCheckout(myGitFolder, "Checkout", Force, checkoutOperation)
                    == ConflictWorkflow.Outcome.CONFLICTS) {
                return conflictResult(CurrentMethodName, "Checkout", myGitFolder);
            }
            Ref ref = checkedOut[0];
            fileRepo.importProjectEntities();
            String LogResult = (ref != null) ? ref.toString() : "No message.";

            log.trace("Exiting Service: Checkout");
            return DapResults.success("Checkout", LogResult, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure(CurrentMethodName, e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "BranchSwitch",
            description =
                    "Switches to an existing local branch or creates a tracking branch for a matching origin branch.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable BranchSwitch(
            @ThingworxServiceParameter(
                            name = "BranchName",
                            description = "Local branch or uniquely matching remote branch name",
                            baseType = "STRING")
                    String BranchName,
            @ThingworxServiceParameter(
                            name = "Force",
                            description = "Forces checkout over conflicting working-tree changes",
                            baseType = "BOOLEAN")
                    Boolean Force) {
        try {
            if (BranchName.isBlank())
                GitActionLogger.logAction(
                        "BranchSwitch", resolveTargetThing().getName(), "BranchName is required");
            if (BranchName.isBlank())
                return DapResults.failure(
                        "BranchSwitch",
                        "BranchName is required.",
                        StringResultContract.SERVICE_RESULT);
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git git = fileRepo.git();
            Ref[] switched = new Ref[1];
            ConflictWorkflow.GitOperation switchOperation =
                    () -> {
                        try {
                            switched[0] =
                                    git.checkout()
                                            .setName(BranchName)
                                            .setForced(Boolean.TRUE.equals(Force))
                                            .call();
                        } catch (RefNotFoundException e) {
                            switched[0] =
                                    git.checkout()
                                            .setCreateBranch(true)
                                            .setName(BranchName)
                                            .setUpstreamMode(SetupUpstreamMode.TRACK)
                                            .setStartPoint("origin/" + BranchName)
                                            .call();
                        }
                        return ConflictWorkflow.Outcome.COMPLETED;
                    };
            if (runCheckout(git, "Branch switch", Force, switchOperation)
                    == ConflictWorkflow.Outcome.CONFLICTS) {
                GitActionLogger.logAction(
                        "BranchSwitch", resolveTargetThing().getName(), "stopped on conflicts");
                return conflictResult("BranchSwitch", "Branch switch", git);
            }
            Ref ref = switched[0];

            fileRepo.importProjectEntities();
            GitActionLogger.logAction(
                    "BranchSwitch", resolveTargetThing().getName(), ref.toString());
            return DapResults.success(
                    "BranchSwitch", ref.toString(), StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "BranchSwitch",
                    resolveTargetThing().getName(),
                    e == null ? "" : String.valueOf(e.getMessage()));
            return DapResults.failure("BranchSwitch", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "BranchCreateAndCheckout",
            description =
                    "Creates a new local branch from an optional start point and checks it out.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable BranchCreateAndCheckout(
            @ThingworxServiceParameter(
                            name = "BranchName",
                            description = "Name of the new branch",
                            baseType = "STRING")
                    String BranchName,
            @ThingworxServiceParameter(
                            name = "StartPoint",
                            description =
                                    "Optional start point when creating the branch (defaults to HEAD)",
                            baseType = "STRING")
                    String StartPoint,
            @ThingworxServiceParameter(
                            name = "Force",
                            description = "Forces checkout over conflicting working-tree changes",
                            baseType = "BOOLEAN")
                    Boolean Force) {
        log.trace("Entering Service: BranchCreateAndCheckout");
        if (BranchName.isBlank()) {
            String errMsg =
                    "BranchCreateAndCheckout Error: "
                            + Const.ERR_PREFIX_CONFIG
                            + "BranchName is required.";
            GitActionLogger.logAction(
                    "BranchCreateAndCheckout", resolveTargetThing().getName(), errMsg);
            return DapResults.failure(
                    "BranchCreateAndCheckout", errMsg, StringResultContract.SERVICE_RESULT);
        }
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git git = fileRepo.git();
            StartPoint = (StartPoint == null || StartPoint.isBlank()) ? "HEAD" : StartPoint;
            ConflictWorkflow.requireNotInProgress(git);
            git.branchCreate().setName(BranchName).setStartPoint(StartPoint).call();
            Ref[] checkedOut = new Ref[1];
            if (runCheckout(
                            git,
                            "Branch checkout",
                            Force,
                            () -> {
                                checkedOut[0] =
                                        git.checkout()
                                                .setName(BranchName)
                                                .setForced(Boolean.TRUE.equals(Force))
                                                .call();
                                return ConflictWorkflow.Outcome.COMPLETED;
                            })
                    == ConflictWorkflow.Outcome.CONFLICTS) {
                return conflictResult("BranchCreateAndCheckout", "Branch checkout", git);
            }
            Ref ref = checkedOut[0];
            fileRepo.importProjectEntities();
            log.trace("Exiting Service: BranchCreateAndCheckout");
            return DapResults.success(
                    "BranchCreateAndCheckout", ref.toString(), StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "BranchCreateAndCheckout",
                    resolveTargetThing().getName(),
                    e == null ? "" : String.valueOf(e.getMessage()));
            return DapResults.failure(
                    "BranchCreateAndCheckout", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(CurrentBranchStatusSpec.class)
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
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.CurrentBranchStatus.ServiceResult.DataShape"
            })
    /**
     * Reads the repository HEAD state without changing the working tree. The result identifies the
     * current branch or reports the full detached-HEAD reference.
     *
     * @return a {@code GIT.CurrentBranchStatus.ServiceResult.DataShape} table; an empty table
     *     indicates that the repository state could not be read
     */
    public InfoTable GetCurrentBranch() {
        try {
            log.trace("Entering Service: GetCurrentBranch");
            InfoTable CurrentBranchStatus =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            "GIT.CurrentBranchStatus.DataShape");
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Repository repository = fileRepo.git().getRepository();
            String fullBranch = repository.getFullBranch();
            boolean detachedHead = fullBranch == null || !fullBranch.startsWith("refs/heads/");
            String currentBranch = repository.getBranch();
            ValueCollection vc = new ValueCollection();

            vc.put("BranchName", new StringPrimitive(currentBranch));
            vc.put("DetachedHEAD", new BooleanPrimitive(detachedHead));
            var tracking =
                    detachedHead
                            ? null
                            : org.eclipse.jgit.lib.BranchTrackingStatus.of(
                                    repository, currentBranch);
            String aheadStatus =
                    detachedHead
                            ? "Ahead: unavailable (detached HEAD)"
                            : tracking == null
                                    ? "Ahead: unavailable (no upstream branch)"
                                    : "Ahead: "
                                            + tracking.getAheadCount()
                                            + " commits (since last fetch)";
            vc.put("AheadStatus", new StringPrimitive(aheadStatus));
            CurrentBranchStatus.addRow(vc);
            log.trace("Exiting Service: GetCurrentBranch");
            return DapResults.success(
                    "GetCurrentBranch",
                    CurrentBranchStatusTable.SERVICE_RESULT,
                    CurrentBranchStatusTable.wrap(CurrentBranchStatus));
        } catch (Exception ex) {

            return DapResults.failure(
                    "GetCurrentBranch", ex, CurrentBranchStatusTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(BranchListSpec.class)
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
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.BranchList.ServiceResult.DataShape"
            })
    /**
     * Lists local and remote branches using JGit's all-refs mode. This is read-only and does not
     * fetch the remote first.
     *
     * @return a {@code GIT.BranchList.ServiceResult.DataShape} table, or an empty table when
     *     listing fails
     */
    public InfoTable GetBranchList() {
        log.trace("Entering Service: GetBranchList");

        try {
            InfoTable BranchList =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            "GIT.BranchList.DataShape");
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git myGit = fileRepo.git();
            List<Ref> branches = myGit.branchList().setListMode(ListMode.ALL).call();
            String headRef = myGit.getRepository().getFullBranch();
            for (Iterator<Ref> iterator = branches.iterator(); iterator.hasNext(); ) {
                Ref ref = (Ref) iterator.next();
                ValueCollection vc = new ValueCollection();

                String LongBranchName = ref.getName();
                String ShortBranchName, BranchType;
                ShortBranchName =
                        ("HEAD".equals(LongBranchName))
                                ? "HEAD"
                                : LongBranchName.replace("refs/heads/", "")
                                        .replace("refs/remotes/origin/", "");
                BranchType =
                        ("HEAD".equals(LongBranchName))
                                ? "HEAD"
                                : (LongBranchName.indexOf("refs/heads/") >= 0 ? "LOCAL" : "REMOTE");
                vc.put("BranchName", new StringPrimitive(LongBranchName));
                vc.put("ShortBranchName", new StringPrimitive(ShortBranchName));
                vc.put("BranchType", new StringPrimitive(BranchType));
                vc.put("CommitID", new StringPrimitive(ref.getObjectId().getName()));
                vc.put("IsHead", new BooleanPrimitive(LongBranchName.equals(headRef)));
                BranchList.addRow(vc);
            }
            log.trace("Exiting Service: GetBranchList");
            return DapResults.success(
                    "GetBranchList",
                    BranchListTable.SERVICE_RESULT,
                    BranchListTable.wrap(BranchList));

        } catch (Exception ex) {
            return DapResults.failure("GetBranchList", ex, BranchListTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "BranchDelete",
            description =
                    "Deletes a local branch and optionally deletes its remote branch when explicitly requested.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable BranchDelete(
            @ThingworxServiceParameter(
                            name = "BranchName",
                            description = "Branch name to be deleted, without the refs/heads/ part",
                            baseType = "STRING")
                    String BranchName,
            @ThingworxServiceParameter(
                            name = "Remote",
                            description = "Optional remote name used with DeleteRemote",
                            baseType = "STRING")
                    String Remote,
            @ThingworxServiceParameter(
                            name = "DeleteRemote",
                            description = "Explicitly delete the matching remote branch",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean DeleteRemote,
            @ThingworxServiceParameter(
                            name = "Force",
                            description = "Force deletion when the branch is not merged",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean Force) {
        log.trace("Entering Service: BranchDelete");
        if (BranchName.isBlank()) {
            String errMsg =
                    "BranchDelete Error: " + Const.ERR_PREFIX_CONFIG + "BranchName is required.";

            GitActionLogger.logAction("BranchDelete", resolveTargetThing().getName(), errMsg);
            return DapResults.failure("BranchDelete", errMsg, StringResultContract.SERVICE_RESULT);
        }
        if (DeleteRemote.booleanValue() && Remote.isBlank()) {
            GitActionLogger.logAction(
                    "BranchDelete",
                    resolveTargetThing().getName(),
                    "Remote is required when DeleteRemote is true");
            return DapResults.failure(
                    "BranchDelete",
                    "BranchDelete Error: "
                            + Const.ERR_PREFIX_CONFIG
                            + "Remote is required when DeleteRemote is true.",
                    StringResultContract.SERVICE_RESULT);
        }
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git myGitFolder = fileRepo.git();

            List<String> lstr =
                    myGitFolder
                            .branchDelete()
                            .setForce(Force.booleanValue())
                            .setBranchNames("refs/heads/" + BranchName)
                            .call();
            String LogResult = "";
            if (lstr.size() == 0) {
                LogResult += " Branch '" + BranchName + "' was not found or could not be deleted.";
            }
            for (String str : lstr) {
                LogResult += str;
            }
            fileRepo.importProjectEntities();

            if (DeleteRemote.booleanValue()) {
                ValueCollection credentials =
                        new GitUserContextManager().credentialsFor(resolveTargetThing().getName());
                String userName = credentials.getStringValue(Const.GitCommitterUser);
                String password = credentials.getStringValue(Const.GitCommitterPassword);
                Iterable<PushResult> results =
                        myGitFolder
                                .push()
                                .setRemote(Remote)
                                .setCredentialsProvider(
                                        new UsernamePasswordCredentialsProvider(userName, password))
                                .setRefSpecs(new RefSpec(":refs/heads/" + BranchName))
                                .call();
                LogResult += " Remote delete: " + results.iterator().next();
            }
            fileRepo.importProjectEntities();

            GitActionLogger.logAction("BranchDelete", resolveTargetThing().getName(), LogResult);
            return DapResults.success(
                    "BranchDelete", LogResult, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            if (e instanceof NotMergedException) {
                GitActionLogger.logAction(
                        "BranchDelete", resolveTargetThing().getName(), "branch not merged");
                return DapResults.failure(
                        "BranchDelete",
                        "BranchDelete Error: "
                                + Const.ERR_PREFIX_GIT
                                + "Branch '"
                                + BranchName
                                + "' has commits that are not merged into the current branch. Merge it first, or use Force Delete Branch to delete it anyway.",
                        StringResultContract.SERVICE_RESULT);
            }
            if (e.getMessage() != null && e.getMessage().contains("CannotDelete")) {
                GitActionLogger.logAction(
                        "BranchDelete",
                        resolveTargetThing().getName(),
                        "cannot delete current branch");
                return DapResults.failure(
                        "BranchDelete",
                        "BranchDelete Error: "
                                + Const.ERR_PREFIX_GIT
                                + "Branch '"
                                + BranchName
                                + "' cannot be deleted. It may be the current branch. Checkout a different branch first.",
                        StringResultContract.SERVICE_RESULT);
            }
            GitActionLogger.logAction(
                    "BranchDelete",
                    resolveTargetThing().getName(),
                    e == null ? "" : String.valueOf(e.getMessage()));
            return DapResults.failure("BranchDelete", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(CommitLogSpec.class)
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
            aspects = {"isEntityDataShape:true", "dataShape:GIT.CommitLog.ServiceResult.DataShape"})
    /**
     * Returns commit history in newest-first order. A blank ref resolves to the current branch, or
     * to the configured initial branch when HEAD is detached. A zero or null limit returns all
     * reachable commits.
     *
     * @param Ref optional branch, tag, commit, or ref name
     * @param MaxEntries maximum number of rows; null or zero means no limit
     * @param AllBranches when true, include commits reachable from all branches and tags
     * @return a {@code GIT.CommitLog.DataShape} table
     */
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
                    Integer MaxEntries,
            @ThingworxServiceParameter(
                            name = "AllBranches",
                            description = "Include commits from all branches and tags",
                            baseType = "BOOLEAN")
                    Boolean AllBranches) {
        log.trace("Entering Service: GetLog");
        try {
            InfoTable result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.CommitLogDataShapeName);
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Repository repository = fileRepo.git().getRepository();
            int count = 0;
            if (Boolean.TRUE.equals(AllBranches)) {
                try (RevWalk walk = new RevWalk(repository)) {
                    Set<ObjectId> marked = new HashSet<>();
                    for (Ref ref :
                            repository
                                    .getRefDatabase()
                                    .getRefsByPrefix(
                                            Constants.R_HEADS,
                                            Constants.R_REMOTES,
                                            Constants.R_TAGS)) {
                        Ref peeled = repository.getRefDatabase().peel(ref);
                        ObjectId target =
                                peeled.getPeeledObjectId() != null
                                        ? peeled.getPeeledObjectId()
                                        : peeled.getObjectId();
                        if (target == null || !marked.add(target)) continue;
                        try {
                            walk.markStart(walk.parseCommit(target));
                        } catch (Exception ignored) {
                            // Skip refs that do not point at commits.
                        }
                    }
                    walk.sort(RevSort.TOPO);
                    for (RevCommit commit : walk) {
                        if (MaxEntries != null && MaxEntries > 0 && count >= MaxEntries) break;
                        addCommitLogRow(result, commit);
                        count++;
                    }
                }
            } else {
                String historyRef = Ref;
                if (historyRef == null || historyRef.isBlank()) {
                    String fullBranch = repository.getFullBranch();
                    historyRef =
                            fullBranch != null && fullBranch.startsWith("refs/heads/")
                                    ? fullBranch.substring("refs/heads/".length())
                                    : "HEAD";
                }
                ObjectId objectId = repository.resolve(historyRef);
                if (objectId == null && historyRef != null && !historyRef.startsWith("refs/")) {
                    objectId = repository.resolve("refs/heads/" + historyRef);
                }
                if (objectId == null && Ref != null && !Ref.isBlank()) {
                    return DapResults.failure(
                            "GetLog",
                            Const.ERR_PREFIX_GIT + "Ref '" + Ref + "' was not found.",
                            CommitLogTable.SERVICE_RESULT);
                }
                if (objectId != null) {
                    for (RevCommit commit : fileRepo.git().log().add(objectId).call()) {
                        if (MaxEntries != null && MaxEntries > 0 && count >= MaxEntries) break;
                        addCommitLogRow(result, commit);
                        count++;
                    }
                }
            }
            return DapResults.success(
                    "GetLog", CommitLogTable.SERVICE_RESULT, CommitLogTable.wrap(result));
        } catch (Exception ex) {

            return DapResults.failure("GetLog", ex, CommitLogTable.SERVICE_RESULT);
        }
    }

    private static void addCommitLogRow(InfoTable result, RevCommit commit) {
        ValueCollection row = new ValueCollection();
        row.put("CommitID", new StringPrimitive(commit.getId().name()));
        row.put("CommitName", new StringPrimitive(commit.getShortMessage()));
        row.put(
                "CommitTime",
                new DatetimePrimitive(new DateTime((long) commit.getCommitTime() * 1000)));
        PersonIdent author = commit.getAuthorIdent();
        row.put(
                "AuthorName",
                new StringPrimitive(
                        author == null || author.getName() == null ? "" : author.getName()));
        row.put(
                "AuthorEmail",
                new StringPrimitive(
                        author == null || author.getEmailAddress() == null
                                ? ""
                                : author.getEmailAddress()));
        PersonIdent committer = commit.getCommitterIdent();
        row.put(
                "CommitterName",
                new StringPrimitive(
                        committer == null || committer.getName() == null
                                ? ""
                                : committer.getName()));
        row.put(
                "CommitterEmail",
                new StringPrimitive(
                        committer == null || committer.getEmailAddress() == null
                                ? ""
                                : committer.getEmailAddress()));
        StringBuilder parentIds = new StringBuilder();
        for (RevCommit parent : commit.getParents()) {
            if (parentIds.length() > 0) parentIds.append(",");
            parentIds.append(parent.getId().name());
        }
        row.put("ParentCommitIDs", new StringPrimitive(parentIds.toString()));
        result.addRow(row);
    }

    @DapServicePayload(ReflogEntrySpec.class)
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
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.ReflogEntry.ServiceResult.DataShape"
            })
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
                    Integer MaxEntries) {
        log.trace("Entering Service: GetReflog");
        try {
            InfoTable result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.ReflogEntryDataShapeName);
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git git = fileRepo.git();
            String requested = Ref == null || Ref.isBlank() ? Constants.HEAD : Ref.trim();
            Repository repository = git.getRepository();
            Ref target =
                    Constants.HEAD.equals(requested)
                            ? repository.exactRef(Constants.HEAD)
                            : repository.findRef(requested);
            if (target == null) {
                return DapResults.failure(
                        "GetReflog",
                        Const.ERR_PREFIX_GIT + "Ref '" + requested + "' was not found.",
                        ReflogEntryTable.SERVICE_RESULT);
            }
            String ref = target.getName();
            int count = 0;
            for (ReflogEntry entry : git.reflog().setRef(ref).call()) {
                if (MaxEntries != null && MaxEntries > 0 && count >= MaxEntries) break;
                ValueCollection row = new ValueCollection();
                row.put("RefName", new StringPrimitive(ref));
                row.put(
                        "OldObjectID",
                        new StringPrimitive(
                                entry.getOldId() == null ? "" : entry.getOldId().name()));
                row.put(
                        "NewObjectID",
                        new StringPrimitive(
                                entry.getNewId() == null ? "" : entry.getNewId().name()));
                PersonIdent who = entry.getWho();
                row.put(
                        "ActorName",
                        new StringPrimitive(
                                who == null || who.getName() == null ? "" : who.getName()));
                row.put(
                        "ActorEmail",
                        new StringPrimitive(
                                who == null || who.getEmailAddress() == null
                                        ? ""
                                        : who.getEmailAddress()));
                row.put(
                        "EventTime",
                        new DatetimePrimitive(
                                new DateTime(
                                        who == null ? 0 : who.getWhenAsInstant().toEpochMilli())));
                row.put(
                        "Comment",
                        new StringPrimitive(entry.getComment() == null ? "" : entry.getComment()));
                CheckoutEntry checkout = entry.parseCheckout();
                row.put(
                        "CheckoutSource",
                        new StringPrimitive(
                                checkout == null || checkout.getFromBranch() == null
                                        ? ""
                                        : checkout.getFromBranch()));
                row.put(
                        "CheckoutTarget",
                        new StringPrimitive(
                                checkout == null || checkout.getToBranch() == null
                                        ? ""
                                        : checkout.getToBranch()));
                result.addRow(row);
                count++;
            }
            return DapResults.success(
                    "GetReflog", ReflogEntryTable.SERVICE_RESULT, ReflogEntryTable.wrap(result));
        } catch (Exception ex) {

            return DapResults.failure("GetReflog", ex, ReflogEntryTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(HistoryRefSpec.class)
    @ThingworxServiceDefinition(
            name = "GetHistoryRefs",
            description =
                    "Lists HEAD, local branches, remote-tracking branches, and tags for log and reflog views. ReflogOnly limits the rows to refs that have local reflog entries.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.HistoryRef.ServiceResult.DataShape"
            })
    /**
     * Lists the refs a log or reflog view can select: HEAD first, then local branches,
     * remote-tracking branches, and tags, each group sorted by name. Symbolic remote HEAD refs such
     * as {@code refs/remotes/origin/HEAD} are omitted. This is read-only and does not fetch.
     *
     * @param ReflogOnly when true, include only refs with local reflog entries
     * @return a {@code GIT.HistoryRef.ServiceResult.DataShape} table
     */
    public InfoTable GetHistoryRefs(
            @ThingworxServiceParameter(
                            name = "ReflogOnly",
                            description = "Only include refs that have local reflog entries",
                            baseType = "BOOLEAN")
                    Boolean ReflogOnly) {
        log.trace("Entering Service: GetHistoryRefs");
        try {
            InfoTable result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.HistoryRefDataShapeName);
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Repository repository = fileRepo.git().getRepository();
            var refDatabase = repository.getRefDatabase();
            String currentBranch = repository.getFullBranch();
            List<Ref> refs = new ArrayList<>();
            Ref head = repository.exactRef(Constants.HEAD);
            if (head != null) refs.add(head);
            for (String prefix :
                    List.of(Constants.R_HEADS, Constants.R_REMOTES, Constants.R_TAGS)) {
                List<Ref> group = new ArrayList<>(refDatabase.getRefsByPrefix(prefix));
                group.sort(Comparator.comparing(Ref::getName));
                refs.addAll(group);
            }
            boolean reflogOnly = Boolean.TRUE.equals(ReflogOnly);
            for (Ref ref : refs) {
                String name = ref.getName();
                if (name.startsWith(Constants.R_REMOTES) && ref.isSymbolic()) continue;
                ReflogReader reader = refDatabase.getReflogReader(ref);
                boolean hasReflog = reader != null && reader.getLastEntry() != null;
                if (reflogOnly && !hasReflog) continue;
                String type;
                boolean isHead = false;
                if (Constants.HEAD.equals(name)) {
                    type = "HEAD";
                    isHead = true;
                } else if (name.startsWith(Constants.R_HEADS)) {
                    type = "LOCAL";
                    isHead = name.equals(currentBranch);
                } else if (name.startsWith(Constants.R_REMOTES)) {
                    type = "REMOTE";
                } else {
                    type = "TAG";
                }
                // The short name is what selectors pass back to GetLog and GetReflog.
                String display = Repository.shortenRefName(name);
                Ref peeled = ref.isPeeled() ? ref : refDatabase.peel(ref);
                ObjectId target =
                        peeled.getPeeledObjectId() != null
                                ? peeled.getPeeledObjectId()
                                : peeled.getObjectId();
                ValueCollection row = new ValueCollection();
                row.put("RefName", new StringPrimitive(name));
                row.put("DisplayName", new StringPrimitive(display));
                row.put("RefType", new StringPrimitive(type));
                row.put("CommitID", new StringPrimitive(target == null ? "" : target.name()));
                row.put("IsHead", new BooleanPrimitive(isHead));
                row.put("HasReflog", new BooleanPrimitive(hasReflog));
                result.addRow(row);
            }
            log.trace("Exiting Service: GetHistoryRefs");
            return DapResults.success(
                    "GetHistoryRefs", HistoryRefTable.SERVICE_RESULT, HistoryRefTable.wrap(result));
        } catch (Exception ex) {
            return DapResults.failure("GetHistoryRefs", ex, HistoryRefTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "SyncToThingWorx",
            description = "Imports repository XML entities into ThingWorx.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable SyncToThingWorx() {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            boolean imported = fileRepo.importProjectEntities();
            return DapResults.success(
                    "SyncToThingWorx",
                    imported
                            ? "Repository entities imported into ThingWorx."
                            : "No XML entities found to import.",
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("SyncToThingWorx", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    /** Synchronizes ThingWorx exports and reports staged and working-tree status. */
    @DapServicePayload(StatusSpec.class)
    @ThingworxServiceDefinition(
            name = "Status",
            description = "Synchronizes the configured ThingWorx project and reports Git status.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.Status.ServiceResult.DataShape"})
    public InfoTable Status(
            @ThingworxServiceParameter(
                            name = "ShowUnchangedFiles",
                            description = "Include unchanged tracked files",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean ShowUnchangedFiles,
            @ThingworxServiceParameter(
                            name = "HideUntracked",
                            description = "Exclude untracked working-tree files",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean HideUntracked,
            @ThingworxServiceParameter(
                            name = "HideUnchanged",
                            description = "Exclude unchanged tracked files",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean HideUnchanged,
            @ThingworxServiceParameter(
                            name = "HideIgnored",
                            description = "Exclude ignored working-tree paths",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean HideIgnored) {
        FileRepositoryManager fileRepo = null;
        try {
            InfoTable result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape("GIT.Status.DataShape");
            // Existing installations retain their original DataShape on extension upgrades.
            // Extend a private copy so older installations return the new fields too.
            result.setDataShape(result.getDataShape().clone());
            addStatusField(result, "FileName", com.thingworx.types.BaseTypes.STRING, 4);
            addStatusField(result, "Path", com.thingworx.types.BaseTypes.STRING, 5);
            addStatusField(result, "Area", com.thingworx.types.BaseTypes.STRING, 6);
            addStatusField(result, "ChangeType", com.thingworx.types.BaseTypes.STRING, 7);
            addStatusField(result, "CanStage", com.thingworx.types.BaseTypes.BOOLEAN, 8);
            addStatusField(result, "Tracked", com.thingworx.types.BaseTypes.BOOLEAN, 9);
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.syncStatusFromThingWorx();
            Git git = fileRepo.git();
            org.eclipse.jgit.api.Status status = git.status().call();
            Set<String> conflicting = new HashSet<>(status.getConflicting());
            Set<String> changedPaths = new HashSet<>();
            for (String file : status.getAdded()) {
                if (conflicting.contains(file)) continue;
                addWorkingTreeRow(
                        result, file, "Added", "Staged Changes", "Added", true, false, true);
                changedPaths.add(file);
            }
            for (String file : status.getChanged()) {
                if (conflicting.contains(file)) continue;
                addWorkingTreeRow(
                        result, file, "Changed", "Staged Changes", "Modified", true, false, true);
                changedPaths.add(file);
            }
            for (String file : status.getRemoved()) {
                if (conflicting.contains(file)) continue;
                addWorkingTreeRow(
                        result, file, "Removed", "Staged Changes", "Deleted", true, false, false);
                changedPaths.add(file);
            }
            for (String file : status.getMissing()) {
                if (conflicting.contains(file)) continue;
                addWorkingTreeRow(result, file, "Missing", "Changes", "Deleted", false, true, true);
                changedPaths.add(file);
            }
            for (String file : status.getModified()) {
                if (conflicting.contains(file)) continue;
                addWorkingTreeRow(
                        result, file, "Modified", "Changes", "Modified", false, true, true);
                changedPaths.add(file);
            }
            for (String file : status.getUntracked()) {
                if (conflicting.contains(file)) continue;
                if (!Boolean.TRUE.equals(HideUntracked)) {
                    addWorkingTreeRow(
                            result, file, "Untracked", "Changes", "Untracked", false, true, false);
                }
                changedPaths.add(file);
            }
            for (String file : status.getIgnoredNotInIndex()) {
                if (!Boolean.TRUE.equals(HideIgnored)) {
                    addWorkingTreeRow(
                            result, file, "Ignored", "Ignored", "Ignored", false, false, false);
                }
                changedPaths.add(file);
            }
            for (String file : conflicting) {
                addWorkingTreeRow(
                        result,
                        file,
                        "Conflicting",
                        "Conflicted",
                        "Conflicted",
                        false,
                        false,
                        true);
                changedPaths.add(file);
            }
            if (Boolean.TRUE.equals(ShowUnchangedFiles) && !Boolean.TRUE.equals(HideUnchanged)) {
                var index = git.getRepository().readDirCache();
                for (int i = 0; i < index.getEntryCount(); i++) {
                    String path = index.getEntry(i).getPathString();
                    if (!changedPaths.contains(path)) {
                        addWorkingTreeRow(
                                result,
                                path,
                                "Unchanged",
                                "Unchanged",
                                "Unchanged",
                                false,
                                false,
                                true);
                    }
                }
            }
            return DapResults.success(
                    "Status", StatusTable.SERVICE_RESULT, StatusTable.wrap(result));
        } catch (Exception e) {
            return DapResults.failure("Status", e, StatusTable.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    private static void addStatusField(
            InfoTable table, String name, com.thingworx.types.BaseTypes type, int ordinal) {
        if (!table.hasField(name)) {
            var field = new com.thingworx.metadata.FieldDefinition(name, type);
            field.setOrdinal(ordinal);
            table.addField(field);
        }
    }

    private void addWorkingTreeRow(
            InfoTable table,
            String file,
            String legacyStatus,
            String area,
            String changeType,
            boolean staged,
            boolean canStage,
            boolean tracked) {
        ValueCollection row = new ValueCollection();
        row.put("File", new StringPrimitive(file));
        row.put("Status", new StringPrimitive(legacyStatus));
        int separator = file.lastIndexOf('/');
        row.put("FileName", new StringPrimitive(file.substring(separator + 1)));
        row.put("Path", new StringPrimitive(separator < 0 ? "" : file.substring(0, separator)));
        row.put("Area", new StringPrimitive(area));
        row.put("ChangeType", new StringPrimitive(changeType));
        row.put("Staged", new BooleanPrimitive(staged));
        row.put("CanStage", new BooleanPrimitive(canStage));
        row.put("Tracked", new BooleanPrimitive(tracked));
        table.addRow(row);
    }

    @DapServicePayload(StatusSpec.class)
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
            aspects = {"isEntityDataShape:true", "dataShape:GIT.Status.ServiceResult.DataShape"})
    public InfoTable GetConflictFiles() {
        try {
            var result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape("GIT.Status.DataShape");
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            for (String file : fileRepo.git().status().call().getConflicting()) {
                ValueCollection row = new ValueCollection();
                row.put("File", new StringPrimitive(file));
                row.put("Status", new StringPrimitive("Conflicting"));
                row.put("Staged", new BooleanPrimitive(false));
                result.addRow(row);
            }
            return DapResults.success(
                    "GetConflictFiles", StatusTable.SERVICE_RESULT, StatusTable.wrap(result));
        } catch (Exception e) {

            return DapResults.failure("GetConflictFiles", e, StatusTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "ReadConflictFile",
            description = "Reads a conflicted repository file for UI editing.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable ReadConflictFile(
            @ThingworxServiceParameter(
                            name = "File",
                            description = "Repository-relative file path",
                            baseType = "STRING")
                    String File) {
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            return DapResults.success(
                    "ReadConflictFile",
                    fileRepo.fileRepository().LoadText(File),
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("ReadConflictFile", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "WriteConflictFile",
            description = "Writes a UI-resolved repository file while a merge or rebase is paused.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable WriteConflictFile(
            @ThingworxServiceParameter(
                            name = "File",
                            description = "Repository-relative file path",
                            baseType = "STRING")
                    String File,
            @ThingworxServiceParameter(
                            name = "Content",
                            description = "Resolved file content",
                            baseType = "STRING")
                    String Content) {
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.fileRepository().SaveText(File, Content == null ? "" : Content);
            return DapResults.success(
                    "WriteConflictFile",
                    "Wrote conflict file " + File,
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("WriteConflictFile", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "Add",
            description =
                    "Stages one repository-relative file, or all non-ignored changes when All is true.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable Add(
            @ThingworxServiceParameter(
                            name = "File",
                            description = "Repository-relative file path; ignored when All is true",
                            baseType = "STRING")
                    String File,
            @ThingworxServiceParameter(
                            name = "All",
                            description =
                                    "Stages all non-ignored additions, modifications, and deletions",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean All) {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            boolean all = Boolean.TRUE.equals(All);
            String pattern = all ? "." : File;
            fileRepo.stagePath(pattern);
            String message = all ? "Staged all non-ignored changes" : "Staged " + pattern;
            return DapResults.success("Add", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("Add", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "Unstage",
            description =
                    "Unstages one repository-relative file, or all staged files when All is true.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable Unstage(
            @ThingworxServiceParameter(
                            name = "File",
                            description = "Repository-relative file path; ignored when All is true",
                            baseType = "STRING")
                    String File,
            @ThingworxServiceParameter(
                            name = "All",
                            description = "Unstages every staged change",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean All) {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            boolean all = Boolean.TRUE.equals(All);
            if (all) {
                fileRepo.unstageAll();
            } else {
                fileRepo.unstagePath(File);
            }
            String message = all ? "Unstaged all changes" : "Unstaged " + File;
            GitActionLogger.logAction("Unstage", resolveTargetThing().getName(), message);
            return DapResults.success("Unstage", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "Unstage",
                    resolveTargetThing().getName(),
                    e == null ? "" : String.valueOf(e.getMessage()));
            return DapResults.failure("Unstage", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "ApplyFileAction",
            description =
                    "Applies stage, unstage, untrack, or delete to selected working-tree changes.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable ApplyFileAction(
            @ThingworxServiceParameter(
                            name = "Changes",
                            description = "Selected rows from Status; only the File field is used",
                            baseType = "INFOTABLE",
                            aspects = {"dataShape:GIT.WorkingTreeDisplay.DataShape"})
                    InfoTable Changes,
            @ThingworxServiceParameter(
                            name = "Action",
                            description = "stage, unstage, untrack, or delete",
                            baseType = "STRING")
                    String Action) {
        FileRepositoryManager fileRepo = null;
        try {
            String action = Action == null ? "" : Action.trim().toLowerCase(Locale.ROOT);
            if (!action.equals("stage")
                    && !action.equals("unstage")
                    && !action.equals("untrack")
                    && !action.equals("delete")) {
                return DapResults.failure(
                        "ApplyFileAction",
                        "ApplyFileAction Error: "
                                + Const.ERR_PREFIX_CONFIG
                                + String.format(Const.ERR_UNKNOWN_FILE_ACTION, Action),
                        StringResultContract.SERVICE_RESULT);
            }
            if (Changes == null || Changes.getRowCount() == 0) {
                return DapResults.failure(
                        "ApplyFileAction",
                        "ApplyFileAction Error: No files selected.",
                        StringResultContract.SERVICE_RESULT);
            }

            fileRepo = new FileRepositoryManager(resolveTargetThing());
            org.eclipse.jgit.api.Status status = fileRepo.git().status().call();
            Set<String> conflicting = new HashSet<>(status.getConflicting());
            Set<String> staged = new HashSet<>();
            staged.addAll(status.getAdded());
            staged.addAll(status.getChanged());
            staged.addAll(status.getRemoved());
            Set<String> tracked = new HashSet<>();
            var index = fileRepo.git().getRepository().readDirCache();
            for (int i = 0; i < index.getEntryCount(); i++) {
                tracked.add(index.getEntry(i).getPathString());
            }
            Set<String> known = new HashSet<>(tracked);
            known.addAll(staged);
            known.addAll(status.getMissing());
            known.addAll(status.getModified());
            known.addAll(conflicting);
            known.addAll(status.getUntracked());

            LinkedHashSet<String> paths = new LinkedHashSet<>();
            List<String> errors = new ArrayList<>();
            for (int i = 0; i < Changes.getRowCount(); i++) {
                String file = Changes.getRow(i).getStringValue("File");
                String path;
                try {
                    path = fileRepo.normalizeRepositoryRelativePath(file);
                } catch (Exception e) {
                    errors.add(String.valueOf(file) + ": " + e.getMessage());
                    continue;
                }
                if (conflicting.contains(path)) {
                    errors.add(path + ": resolve the conflict first");
                    continue;
                }
                if (!known.contains(path)) {
                    errors.add(path + ": not a current working-tree change");
                    continue;
                }
                if (action.equals("unstage") && !staged.contains(path)) {
                    errors.add(path + ": not staged");
                    continue;
                }
                if (action.equals("untrack") && !tracked.contains(path)) {
                    errors.add(path + ": not tracked");
                    continue;
                }
                if (action.equals("delete")) {
                    try {
                        fileRepo.requireDeletableSourceFile(path, tracked.contains(path));
                    } catch (Exception e) {
                        errors.add(path + ": " + e.getMessage());
                        continue;
                    }
                }
                paths.add(path);
            }

            if (!errors.isEmpty()) {
                String message = "No files were changed. " + String.join("; ", errors);
                GitActionLogger.logAction(
                        "ApplyFileAction", resolveTargetThing().getName(), message);
                return DapResults.failure(
                        "ApplyFileAction",
                        "ApplyFileAction Error: " + message,
                        StringResultContract.SERVICE_RESULT);
            }

            int applied = 0;
            List<String> failures = new ArrayList<>();
            for (String path : paths) {
                try {
                    if (action.equals("stage")) {
                        fileRepo.stagePath(path);
                    } else if (action.equals("unstage")) {
                        fileRepo.unstagePath(path);
                    } else if (action.equals("delete")) {
                        fileRepo.deleteSourceFile(path, tracked.contains(path));
                    } else {
                        fileRepo.untrackPath(path);
                    }
                    applied++;
                } catch (Exception e) {
                    failures.add(path + ": " + e.getMessage());
                }
            }

            if (!failures.isEmpty()) {
                String message =
                        "Applied "
                                + action
                                + " to "
                                + applied
                                + " file(s); failed: "
                                + String.join("; ", failures);
                GitActionLogger.logAction(
                        "ApplyFileAction", resolveTargetThing().getName(), message);
                return DapResults.failure(
                        "ApplyFileAction",
                        "ApplyFileAction Error: " + message,
                        StringResultContract.SERVICE_RESULT);
            }

            String message = "Applied " + action + " to " + applied + " file(s).";
            GitActionLogger.logAction("ApplyFileAction", resolveTargetThing().getName(), message);
            return DapResults.success(
                    "ApplyFileAction", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "ApplyFileAction",
                    resolveTargetThing().getName(),
                    e == null ? "" : String.valueOf(e.getMessage()));
            return DapResults.failure("ApplyFileAction", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "RemoveEverywhere",
            description =
                    "Removes selected entity exports, their ThingWorx entities, and their Git index entries.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable RemoveEverywhere(
            @ThingworxServiceParameter(
                            name = "Changes",
                            description = "Selected working-tree rows",
                            baseType = "INFOTABLE",
                            aspects = {"dataShape:GIT.WorkingTreeDisplay.DataShape"})
                    InfoTable Changes) {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            String message =
                    org.us_ignite.thingworx.jgit.extension.RemoveEverywhere.remove(
                            fileRepo, Changes);
            GitActionLogger.logAction("RemoveEverywhere", resolveTargetThing().getName(), message);
            return DapResults.success(
                    "RemoveEverywhere", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "RemoveEverywhere",
                    resolveTargetThing().getName(),
                    String.valueOf(e.getMessage()));
            return DapResults.failure("RemoveEverywhere", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "Remove",
            description =
                    "Removes a repository-relative file from the Git index without deleting it.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable Remove(
            @ThingworxServiceParameter(
                            name = "File",
                            description = "Repository-relative file path",
                            baseType = "STRING")
                    String File) {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.untrackPath(File);
            return DapResults.success(
                    "Remove", "Removed from index " + File, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("Remove", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
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
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable MergeContinue(
            @ThingworxServiceParameter(
                            name = "Message",
                            description = "Optional merge commit message",
                            baseType = "STRING")
                    String Message) {
        if (!mergeInProgress())
            return DapResults.failure(
                    "MergeContinue",
                    "Error: no merge is in progress.",
                    StringResultContract.SERVICE_RESULT);
        return ConflictContinue(Message);
    }

    @DapServicePayload(StringResultSpec.class)
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
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable MergeAbort() {
        if (!mergeInProgress())
            return DapResults.failure(
                    "MergeAbort",
                    "Error: no merge is in progress.",
                    StringResultContract.SERVICE_RESULT);
        return ConflictAbort();
    }

    private boolean mergeInProgress() {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            RepositoryState state = fileRepo.git().getRepository().getRepositoryState();
            return state == RepositoryState.MERGING || state == RepositoryState.MERGING_RESOLVED;
        } catch (Exception e) {
            return false;
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
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
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable RebaseContinue() {
        return rebaseOperation(
                org.eclipse.jgit.api.RebaseCommand.Operation.CONTINUE, "RebaseContinue");
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "RebaseSkip",
            description = "Skips the current commit in a paused rebase.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable RebaseSkip() {
        return rebaseOperation(org.eclipse.jgit.api.RebaseCommand.Operation.SKIP, "RebaseSkip");
    }

    @DapServicePayload(StringResultSpec.class)
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
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable RebaseAbort() {
        return rebaseOperation(org.eclipse.jgit.api.RebaseCommand.Operation.ABORT, "RebaseAbort");
    }

    private InfoTable rebaseOperation(
            org.eclipse.jgit.api.RebaseCommand.Operation successFromString, String service) {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            if (!fileRepo.git().getRepository().getRepositoryState().isRebasing())
                return DapResults.failure(
                        service,
                        service + " Error: no rebase is in progress.",
                        StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure(service, e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
        return switch (successFromString) {
            case CONTINUE -> ConflictContinue("");
            case SKIP -> ConflictSkip();
            default -> ConflictAbort();
        };
    }

    // ---------------------------------------------------------------- conflict resolution

    /** Runs a checkout, carrying uncommitted changes across it unless {@code force} is set. */
    private static ConflictWorkflow.Outcome runCheckout(
            Git git, String operation, Boolean force, ConflictWorkflow.GitOperation checkout)
            throws Exception {
        if (Boolean.TRUE.equals(force)) {
            ConflictWorkflow.requireNotInProgress(git);
            return checkout.run();
        }
        return ConflictWorkflow.run(git, operation, checkout);
    }

    /**
     * Builds the failure returned when an operation stops on conflicts. The message starts with
     * {@link Const#ERR_PREFIX_CONFLICT} so the UI can offer to resolve or cancel.
     */
    private InfoTable conflictResult(String service, String operation, Git git) throws Exception {
        int count = ConflictWorkflow.unmerged(git).size();
        String message =
                Const.ERR_PREFIX_CONFLICT
                        + String.format(Const.ERR_CONFLICTS_STOPPED, operation, count);
        GitActionLogger.logAction(service, resolveTargetThing().getName(), message);
        return DapResults.failure(service, message, StringResultContract.SERVICE_RESULT);
    }

    @DapServicePayload(ConflictStateSpec.class)
    @ThingworxServiceDefinition(
            name = "GetConflictState",
            description =
                    "Reports whether an operation is waiting for conflict resolution, with labels for each side and the files that still conflict.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.ConflictState.ServiceResult.DataShape"
            })
    public InfoTable GetConflictState() {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            ConflictWorkflow.State state = ConflictWorkflow.state(fileRepo.git());
            ConflictStateTable table = ConflictStateTable.create();
            table.add(
                    ConflictStateRow.create()
                            .inProgress(state.inProgress())
                            .phase(state.phase().name())
                            .operation(state.operation())
                            .repositoryState(state.repositoryState())
                            .currentLabel(state.currentLabel())
                            .incomingLabel(state.incomingLabel())
                            .description(state.description())
                            .conflictCount(state.conflicts().size())
                            .conflictFiles(String.join("\n", state.conflicts()))
                            .canSkip(state.canSkip()));
            return DapResults.success("GetConflictState", ConflictStateTable.SERVICE_RESULT, table);
        } catch (Exception e) {
            return DapResults.failure("GetConflictState", e, ConflictStateTable.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(ConflictFileSpec.class)
    @ThingworxServiceDefinition(
            name = "GetConflictFile",
            description =
                    "Returns the working, current, incoming, and base versions of a conflicted file.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.ConflictFile.ServiceResult.DataShape"
            })
    public InfoTable GetConflictFile(
            @ThingworxServiceParameter(
                            name = "File",
                            description = "Repository-relative file path",
                            baseType = "STRING")
                    String File) {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            ConflictWorkflow.Versions versions =
                    ConflictWorkflow.versions(
                            fileRepo.git(), fileRepo.normalizeRepositoryRelativePath(File));
            boolean binary = versions.binary();
            ConflictFileTable table = ConflictFileTable.create();
            table.add(
                    ConflictFileRow.create()
                            .file(versions.path())
                            .conflicted(versions.conflicted())
                            .binary(binary)
                            .working(binary ? "" : utf8(versions.working()))
                            .current(binary ? "" : utf8(versions.current()))
                            .incoming(binary ? "" : utf8(versions.incoming()))
                            .base(binary ? "" : utf8(versions.base()))
                            .workingExists(versions.working() != null)
                            .currentExists(versions.current() != null)
                            .incomingExists(versions.incoming() != null)
                            .baseExists(versions.base() != null));
            return DapResults.success("GetConflictFile", ConflictFileTable.SERVICE_RESULT, table);
        } catch (Exception e) {
            return DapResults.failure("GetConflictFile", e, ConflictFileTable.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    private static String utf8(byte[] content) {
        return content == null ? "" : new String(content, StandardCharsets.UTF_8);
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "ResolveConflictFile",
            description =
                    "Resolves one conflicted file with the current version, the incoming version, edited content, or deletion, and stages the result.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable ResolveConflictFile(
            @ThingworxServiceParameter(
                            name = "File",
                            description = "Repository-relative file path",
                            baseType = "STRING")
                    String File,
            @ThingworxServiceParameter(
                            name = "Resolution",
                            description = "current, incoming, content, or delete",
                            baseType = "STRING")
                    String Resolution,
            @ThingworxServiceParameter(
                            name = "Content",
                            description = "Resolved text when Resolution is content",
                            baseType = "STRING")
                    String Content) {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            String message =
                    ConflictWorkflow.resolve(
                            fileRepo.git(),
                            fileRepo.normalizeRepositoryRelativePath(File),
                            Resolution,
                            Content);
            GitActionLogger.logAction(
                    "ResolveConflictFile", resolveTargetThing().getName(), message);
            return DapResults.success(
                    "ResolveConflictFile", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure(
                    "ResolveConflictFile", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "ConflictContinue",
            description =
                    "Completes the operation waiting on conflicts once every file is resolved: commits a merge, continues a rebase, or finishes re-applying uncommitted changes. Then imports the result into ThingWorx.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable ConflictContinue(
            @ThingworxServiceParameter(
                            name = "Message",
                            description = "Optional merge commit message",
                            baseType = "STRING")
                    String Message) {
        FileRepositoryManager fileRepo = null;
        PastedKeyGpgSigner[] signer = new PastedKeyGpgSigner[1];
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git git = fileRepo.git();
            String operation = ConflictWorkflow.state(git).operation();
            ValueCollection identity =
                    new GitUserContextManager().credentialsFor(resolveTargetThing().getName());
            String name = identity.getStringValue(Const.GitCommitterName);
            String email = identity.getStringValue(Const.GitCommitterEmail);
            RepositoryState repositoryState = git.getRepository().getRepositoryState();
            boolean merging =
                    repositoryState == RepositoryState.MERGING
                            || repositoryState == RepositoryState.MERGING_RESOLVED;
            if (merging && (name == null || name.isBlank() || email == null || email.isBlank()))
                return DapResults.failure(
                        "ConflictContinue",
                        "ConflictContinue Error: "
                                + Const.ERR_PREFIX_CONFIG
                                + Const.ERR_NO_COMMITTER,
                        StringResultContract.SERVICE_RESULT);
            ConflictWorkflow.Outcome outcome =
                    ConflictWorkflow.continueOperation(
                            git,
                            Message,
                            commit -> {
                                commit.setCommitter(name, email);
                                try {
                                    signer[0] = configureSigning(commit);
                                } catch (Exception e) {
                                    throw new IllegalStateException(e.getMessage(), e);
                                }
                            });
            if (outcome == ConflictWorkflow.Outcome.CONFLICTS)
                return conflictResult("ConflictContinue", operation, git);
            fileRepo.importProjectEntities();
            String message = operation + " completed and imported into ThingWorx.";
            GitActionLogger.logAction("ConflictContinue", resolveTargetThing().getName(), message);
            return DapResults.success(
                    "ConflictContinue", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "ConflictContinue",
                    resolveTargetThing().getName(),
                    String.valueOf(e.getMessage()));
            return DapResults.failure("ConflictContinue", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (signer[0] != null) signer[0].clearSensitiveData();
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "ConflictSkip",
            description = "Skips the commit a paused rebase stopped on.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable ConflictSkip() {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git git = fileRepo.git();
            String operation = ConflictWorkflow.state(git).operation();
            if (ConflictWorkflow.skip(git) == ConflictWorkflow.Outcome.CONFLICTS)
                return conflictResult("ConflictSkip", operation, git);
            fileRepo.importProjectEntities();
            String message = operation + " completed and imported into ThingWorx.";
            GitActionLogger.logAction("ConflictSkip", resolveTargetThing().getName(), message);
            return DapResults.success("ConflictSkip", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("ConflictSkip", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "ConflictAbort",
            description =
                    "Cancels the operation waiting on conflicts and restores the original branch, commit, and uncommitted changes. ThingWorx entities are not changed.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable ConflictAbort() {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git git = fileRepo.git();
            String operation = ConflictWorkflow.state(git).operation();
            ConflictWorkflow.abort(git);
            String message = operation + " was cancelled. The repository is back where it was.";
            GitActionLogger.logAction("ConflictAbort", resolveTargetThing().getName(), message);
            return DapResults.success(
                    "ConflictAbort", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "ConflictAbort",
                    resolveTargetThing().getName(),
                    String.valueOf(e.getMessage()));
            return DapResults.failure("ConflictAbort", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "GetDiffPerFile",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable GetDiffPerFile(
            @ThingworxServiceParameter(name = "File", description = "", baseType = "STRING")
                    String File) {
        log.trace("Entering Service: GetDiffPerFile");
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git myGitObject = fileRepo.git();
            ByteArrayOutputStream dif = new ByteArrayOutputStream();
            myGitObject.diff().setPathFilter(PathFilter.create(File)).setOutputStream(dif).call();
            log.trace("Exiting Service: GetDiffPerFile");
            return DapResults.success(
                    "GetDiffPerFile", dif.toString("UTF-8"), StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("GetDiffPerFile", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "GetDiffPerFileBetweenCommits",
            description = "",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable GetDiffPerFileBetweenCommits(
            @ThingworxServiceParameter(name = "File", description = "", baseType = "STRING")
                    String File,
            @ThingworxServiceParameter(name = "FromCommitID", description = "", baseType = "STRING")
                    String FromCommitID) {
        log.trace("Entering Service: GetDiffPerFileBetweenCommits");
        try {
            String ToCommitID = "";
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git myGitObject = fileRepo.git();
            Repository repo = myGitObject.getRepository();
            ObjectId commit = repo.resolve(FromCommitID);
            if (commit == null) {
                return DapResults.failure(
                        "GetDiffPerFileBetweenCommits",
                        "Diff Error: " + String.format(Const.ERR_COMMIT_NOT_FOUND, FromCommitID),
                        StringResultContract.SERVICE_RESULT);
            }
            RevWalk walk = new RevWalk(repo);
            RevCommit toCommit = walk.parseCommit(commit);
            if (toCommit.getParentCount() > 0) {
                ToCommitID = toCommit.getParent(0).getName();
            }
            AbstractTreeIterator newTreeParser = prepareTreeParser(repo, FromCommitID);
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
                AbstractTreeIterator oldTreeParser = prepareTreeParser(repo, ToCommitID);
                myGitObject
                        .diff()
                        .setNewTree(newTreeParser)
                        .setOldTree(oldTreeParser)
                        .setPathFilter(PathFilter.create(File))
                        .setOutputStream(dif)
                        .call();
            }
            walk.close();
            log.trace("Exiting Service: GetDiffPerFileBetweenCommits");
            String DiffResult = dif.toString("UTF-8");
            Thing utilityThing =
                    (Thing)
                            EntityUtilities.findEntity(
                                    Const.UtilityThingName, ThingworxRelationshipTypes.Thing);
            int maxSize =
                    utilityThing != null
                            ? utilityThing.GetIntegerPropertyValue(Const.MaxDiffSize)
                            : 500000;
            if (DiffResult.length() > maxSize) {
                return DapResults.failure(
                        "GetDiffPerFileBetweenCommits",
                        String.format(Const.ERR_DIFF_TOO_LARGE, maxSize),
                        StringResultContract.SERVICE_RESULT);
            }
            return DapResults.success(
                    "GetDiffPerFileBetweenCommits",
                    DiffResult,
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));

            return DapResults.failure(
                    "GetDiffPerFileBetweenCommits", ex, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(CommitInfoSpec.class)
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
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.CommitInfo.ServiceResult.DataShape"
            })
    public InfoTable GetCommitInfo(
            @ThingworxServiceParameter(name = "CommitID", description = "", baseType = "STRING")
                    String CommitID) {
        log.trace("Entering Service: GetCommitInfo");
        if (CommitID == null) {
            return DapResults.failure(
                    "GetCommitInfo", "CommitID is required", CommitInfoTable.SERVICE_RESULT);
        }
        try {
            InfoTable Status =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.CommitInfoDataShapeName);
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Repository myGitRepository = fileRepo.git().getRepository();
            ObjectId commit = myGitRepository.resolve(CommitID);
            if (commit == null) {

                return DapResults.failure(
                        "GetCommitInfo", "Commit not found", CommitInfoTable.SERVICE_RESULT);
            }
            try (RevWalk walk = new RevWalk(myGitRepository)) {
                RevCommit commitAgain = walk.parseCommit(commit);
                ValueCollection vc = new ValueCollection();
                vc.put("CommitID", new StringPrimitive(commitAgain.getId().name()));
                List<String> parentIds = new ArrayList<>();
                for (RevCommit parent : commitAgain.getParents()) {
                    parentIds.add(parent.getName());
                }
                vc.put("Parents", new StringPrimitive(String.join(",", parentIds)));
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
                String SigVerification = "";
                byte[] rawBuf = commitAgain.getRawBuffer();
                if (rawBuf != null) {
                    String rawStr = new String(rawBuf, java.nio.charset.StandardCharsets.UTF_8);
                    if (rawStr.contains("\ngpgsig ")) {
                        SigVerification = "SIGNED";
                    }
                }
                vc.put("SignatureVerification", new StringPrimitive(SigVerification));
                InfoTable CommitChangedFiles =
                        InfoTableInstanceFactory.createInfoTableFromDataShape(
                                Const.CommitChangedFiles);
                DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
                diffFormatter.setRepository(myGitRepository);
                diffFormatter.setDetectRenames(true);
                List<DiffEntry> entries;
                if (commitAgain.getParentCount() != 0)
                    entries = diffFormatter.scan(commitAgain.getParent(0), commitAgain.getTree());
                else entries = diffFormatter.scan(null, commitAgain.getTree());

                for (DiffEntry diffEntry : entries) {
                    ValueCollection v2 = new ValueCollection();
                    String displayPath =
                            DiffEntry.DEV_NULL.equals(diffEntry.getNewPath())
                                    ? diffEntry.getOldPath()
                                    : diffEntry.getNewPath();
                    v2.put("FileName", new StringPrimitive(displayPath));
                    v2.put("Status", new StringPrimitive(diffEntry.getChangeType().toString()));
                    if (!DiffEntry.DEV_NULL.equals(diffEntry.getOldPath())) {
                        v2.put("OldFileName", new StringPrimitive(diffEntry.getOldPath()));
                    }
                    CommitChangedFiles.addRow(v2);
                }
                diffFormatter.close();

                vc.put("ChangedFiles", new InfoTablePrimitive(CommitChangedFiles));
                Status.addRow(vc);
                walk.dispose();
                log.trace("Exiting Service: GetCommitInfo");
            }
            return DapResults.success(
                    "GetCommitInfo", CommitInfoTable.SERVICE_RESULT, CommitInfoTable.wrap(Status));
        } catch (Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            log.error("GetCommitInfo failed for CommitID '" + CommitID + "': " + errors.toString());
            return DapResults.failure("GetCommitInfo", ex, CommitInfoTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(CommitTreeEntrySpec.class)
    @ThingworxServiceDefinition(
            name = "GetCommitTree",
            description = "Returns the file entries present in a commit tree.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.CommitTreeEntry.ServiceResult.DataShape"
            })
    public InfoTable GetCommitTree(
            @ThingworxServiceParameter(name = "CommitID", description = "", baseType = "STRING")
                    String CommitID) {
        log.trace("Entering Service: GetCommitTree");
        if (CommitID == null || CommitID.isBlank()) {
            return DapResults.failure(
                    "GetCommitTree", "CommitID is required", CommitTreeEntryTable.SERVICE_RESULT);
        }
        try {
            InfoTable result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.CommitTreeEntryDataShapeName);
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Repository repository = fileRepo.git().getRepository();
            ObjectId objectId = repository.resolve(CommitID);
            if (objectId == null) {
                return DapResults.failure(
                        "GetCommitTree",
                        String.format(Const.ERR_COMMIT_NOT_FOUND, CommitID),
                        CommitTreeEntryTable.SERVICE_RESULT);
            }
            try (RevWalk walk = new RevWalk(repository);
                    ObjectReader reader = repository.newObjectReader()) {
                RevCommit commit = walk.parseCommit(objectId);
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(commit.getTree());
                    treeWalk.setRecursive(true);
                    while (treeWalk.next()) {
                        String path = treeWalk.getPathString();
                        int slash = path.lastIndexOf('/');
                        String name = slash >= 0 ? path.substring(slash + 1) : path;
                        String parentPath = slash >= 0 ? path.substring(0, slash) : "";
                        FileMode mode = treeWalk.getFileMode(0);
                        long size = 0;
                        if (!FileMode.GITLINK.equals(mode)) {
                            try {
                                size =
                                        reader.getObjectSize(
                                                treeWalk.getObjectId(0), Constants.OBJ_BLOB);
                            } catch (Exception ignored) {
                                size = 0;
                            }
                        }
                        ValueCollection row = new ValueCollection();
                        row.put("Path", new StringPrimitive(path));
                        row.put("Name", new StringPrimitive(name));
                        row.put("ParentPath", new StringPrimitive(parentPath));
                        DapValues.putLong(row, "Size", size);
                        row.put(
                                "Mode",
                                new StringPrimitive(
                                        FileMode.GITLINK.equals(mode)
                                                ? "SUBMODULE"
                                                : mode.toString()));
                        result.addRow(row);
                    }
                }
            }
            log.trace("Exiting Service: GetCommitTree");
            return DapResults.success(
                    "GetCommitTree",
                    CommitTreeEntryTable.SERVICE_RESULT,
                    CommitTreeEntryTable.wrap(result));
        } catch (Exception ex) {
            return DapResults.failure("GetCommitTree", ex, CommitTreeEntryTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(FileContentSpec.class)
    @ThingworxServiceDefinition(
            name = "GetFileContent",
            description = "Returns the content of a file at a commit.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.FileContent.ServiceResult.DataShape"
            })
    public InfoTable GetFileContent(
            @ThingworxServiceParameter(name = "CommitID", description = "", baseType = "STRING")
                    String CommitID,
            @ThingworxServiceParameter(name = "Path", description = "", baseType = "STRING")
                    String Path,
            @ThingworxServiceParameter(
                            name = "MaxBytes",
                            description = "Maximum bytes to read; zero or omitted uses the default",
                            baseType = "INTEGER")
                    Integer MaxBytes) {
        log.trace("Entering Service: GetFileContent");
        if (CommitID == null || CommitID.isBlank()) {
            return DapResults.failure(
                    "GetFileContent", "CommitID is required", FileContentTable.SERVICE_RESULT);
        }
        if (Path == null || Path.isBlank()) {
            return DapResults.failure(
                    "GetFileContent", "Path is required", FileContentTable.SERVICE_RESULT);
        }
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Repository repository = fileRepo.git().getRepository();
            ObjectId objectId = repository.resolve(CommitID);
            if (objectId == null) {
                return DapResults.failure(
                        "GetFileContent",
                        String.format(Const.ERR_COMMIT_NOT_FOUND, CommitID),
                        FileContentTable.SERVICE_RESULT);
            }
            int maxBytes =
                    MaxBytes == null || MaxBytes <= 0 ? 1000000 : Math.min(MaxBytes, 1000000);
            try (RevWalk walk = new RevWalk(repository);
                    ObjectReader reader = repository.newObjectReader()) {
                RevCommit commit = walk.parseCommit(objectId);
                TreeWalk treeWalk = TreeWalk.forPath(repository, Path, commit.getTree());
                if (treeWalk == null) {
                    return DapResults.failure(
                            "GetFileContent",
                            "File not found in commit: " + Path,
                            FileContentTable.SERVICE_RESULT);
                }
                try (TreeWalk ignored = treeWalk) {
                    FileMode mode = treeWalk.getFileMode(0);
                    if (FileMode.TREE.equals(mode)) {
                        return DapResults.failure(
                                "GetFileContent",
                                "Path is a directory: " + Path,
                                FileContentTable.SERVICE_RESULT);
                    }
                    if (FileMode.GITLINK.equals(mode)) {
                        return DapResults.failure(
                                "GetFileContent",
                                "Path is a submodule: " + Path,
                                FileContentTable.SERVICE_RESULT);
                    }
                    ObjectLoader loader = reader.open(treeWalk.getObjectId(0));
                    long fullSize = loader.getSize();
                    byte[] bytes;
                    try (var stream = loader.openStream()) {
                        bytes = stream.readNBytes(maxBytes);
                    }
                    boolean truncated = fullSize > bytes.length;
                    boolean binary = RawText.isBinary(bytes);
                    InfoTable result =
                            InfoTableInstanceFactory.createInfoTableFromDataShape(
                                    Const.FileContentDataShapeName);
                    ValueCollection row = new ValueCollection();
                    row.put("Path", new StringPrimitive(Path));
                    row.put(
                            "Content",
                            new StringPrimitive(
                                    binary ? "" : new String(bytes, StandardCharsets.UTF_8)));
                    DapValues.putLong(row, "Size", fullSize);
                    row.put("Binary", new BooleanPrimitive(binary));
                    row.put("Truncated", new BooleanPrimitive(truncated));
                    result.addRow(row);
                    return DapResults.success(
                            "GetFileContent",
                            FileContentTable.SERVICE_RESULT,
                            FileContentTable.wrap(result));
                }
            }
        } catch (Exception ex) {
            return DapResults.failure("GetFileContent", ex, FileContentTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(TrackedProjectSpec.class)
    @ThingworxServiceDefinition(
            name = "GetTrackedProjects",
            description = "Lists the ThingWorx projects synchronized with this repository",
            category = "Projects",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.TrackedProject.ServiceResult.DataShape"
            })
    /**
     * Lists tracked projects. A repository configured with only the legacy {@code ProjectName}
     * property reports that project.
     *
     * @return a {@code GIT.TrackedProject.ServiceResult.DataShape} table
     */
    public InfoTable GetTrackedProjects() {
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            try {
                return DapResults.success(
                        "GetTrackedProjects",
                        TrackedProjectTable.SERVICE_RESULT,
                        TrackedProjectTable.wrap(projectTable(fileRepo.trackedProjects())));
            } finally {
                fileRepo.close();
            }
        } catch (Exception e) {
            return DapResults.failure("GetTrackedProjects", e, TrackedProjectTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(TrackedProjectSpec.class)
    @ThingworxServiceDefinition(
            name = "GetAvailableProjects",
            description =
                    "Lists ThingWorx projects visible to the caller that this repository can track and does not already track",
            category = "Projects",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.TrackedProject.ServiceResult.DataShape"
            })
    /**
     * Lists projects that can be added with {@link #AddTrackedProject(String)}, sorted by name.
     * System projects, extension-owned projects, and the project holding repository Things are
     * excluded.
     *
     * @return a {@code GIT.TrackedProject.ServiceResult.DataShape} table
     */
    public InfoTable GetAvailableProjects() {
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Set<String> tracked;
            try {
                tracked = new HashSet<>(fileRepo.trackedProjects());
            } finally {
                fileRepo.close();
            }
            List<String> available = new ArrayList<>(FileRepositoryManager.trackableProjectNames());
            available.removeIf(tracked::contains);
            return DapResults.success(
                    "GetAvailableProjects",
                    TrackedProjectTable.SERVICE_RESULT,
                    TrackedProjectTable.wrap(projectTable(available)));
        } catch (Exception e) {
            return DapResults.failure(
                    "GetAvailableProjects", e, TrackedProjectTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "AddTrackedProject",
            description =
                    "Adds an existing ThingWorx project to the projects synchronized with this repository",
            category = "Projects",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Adds a project to the tracked list. The project is exported on the next status refresh or
     * repository operation; nothing is staged.
     *
     * @param ProjectName an existing, trackable ThingWorx project
     * @return a {@code GIT.StringResult.DataShape} status table
     */
    public InfoTable AddTrackedProject(
            @ThingworxServiceParameter(
                            name = "ProjectName",
                            description = "ThingWorx project to track",
                            baseType = "STRING")
                    String ProjectName) {
        String repoName = "";
        try {
            String name = FileRepositoryManager.validateProjectFolderName(ProjectName);
            var project = EntityUtilities.findEntity(name, ThingworxRelationshipTypes.Project);
            if (project == null) {
                throw new IllegalArgumentException(
                        String.format(Const.ERR_PROJECT_NOT_FOUND, name));
            }
            if (!FileRepositoryManager.isTrackableProject(project)) {
                throw new IllegalArgumentException(
                        "Project '" + name + "' cannot be tracked by a repository.");
            }
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            repoName = fileRepo.repositoryThingName();
            try {
                synchronized (StatusExportReconciler.class) {
                    List<String> projects = new ArrayList<>(fileRepo.trackedProjects());
                    if (projects.contains(name)) {
                        throw new IllegalArgumentException(
                                "Project '" + name + "' is already tracked.");
                    }
                    projects.add(name);
                    fileRepo.setTrackedProjects(projects);
                }
            } finally {
                fileRepo.close();
            }
            String message = "Project '" + name + "' is now tracked.";
            GitActionLogger.logAction("AddTrackedProject", repoName, message);
            return DapResults.success(
                    "AddTrackedProject", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "AddTrackedProject", repoName, String.valueOf(e.getMessage()));
            return DapResults.failure("AddTrackedProject", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "RemoveTrackedProject",
            description =
                    "Stops synchronizing a ThingWorx project with this repository and optionally deletes its exported folder from the working tree",
            category = "Projects",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Removes a project from the tracked list. ThingWorx entities are never deleted. When {@code
     * DeleteFiles} is true, {@code <RepoPath>/<ProjectName>/} is deleted from the working tree and
     * the deletion is left unstaged for review in Changes.
     *
     * @param ProjectName a tracked project
     * @param DeleteFiles whether to delete the project's exported folder
     * @return a {@code GIT.StringResult.DataShape} status table
     */
    public InfoTable RemoveTrackedProject(
            @ThingworxServiceParameter(
                            name = "ProjectName",
                            description = "Tracked ThingWorx project to remove",
                            baseType = "STRING")
                    String ProjectName,
            @ThingworxServiceParameter(
                            name = "DeleteFiles",
                            description =
                                    "Also delete the project's exported folder from the working tree",
                            baseType = "BOOLEAN")
                    Boolean DeleteFiles) {
        String repoName = "";
        try {
            String name = FileRepositoryManager.validateProjectFolderName(ProjectName);
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            repoName = fileRepo.repositoryThingName();
            boolean deleted = false;
            try {
                synchronized (StatusExportReconciler.class) {
                    List<String> projects = new ArrayList<>(fileRepo.trackedProjects());
                    if (!projects.remove(name)) {
                        throw new IllegalArgumentException(
                                "Project '" + name + "' is not tracked.");
                    }
                    if (Boolean.TRUE.equals(DeleteFiles)) {
                        deleted = fileRepo.deleteProjectExport(name);
                    }
                    fileRepo.setTrackedProjects(projects);
                }
            } finally {
                fileRepo.close();
            }
            String message =
                    "Project '"
                            + name
                            + "' is no longer tracked."
                            + (Boolean.TRUE.equals(DeleteFiles)
                                    ? deleted
                                            ? " Its exported files were deleted; review and commit the deletion in Changes."
                                            : " No exported files were found."
                                    : " Its exported files were kept.");
            GitActionLogger.logAction("RemoveTrackedProject", repoName, message);
            return DapResults.success(
                    "RemoveTrackedProject", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "RemoveTrackedProject", repoName, String.valueOf(e.getMessage()));
            return DapResults.failure(
                    "RemoveTrackedProject", e, StringResultContract.SERVICE_RESULT);
        }
    }

    private static InfoTable projectTable(List<String> projects) throws Exception {
        return FileRepositoryManager.projectTable(projects);
    }

    private Thing resolveTargetThing() {
        try {
            Object meContext = ThreadLocalContext.getMeContext();
            if (meContext instanceof Thing) return (Thing) meContext;
            if (meContext instanceof String && !((String) meContext).isBlank()) {
                return (Thing)
                        EntityUtilities.findEntity(
                                (String) meContext, ThingworxRelationshipTypes.Thing);
            }
        } catch (Exception e) {
            log.trace("Could not resolve repository service target: " + e.getMessage());
        }
        return null;
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

    /** Merges the requested branch into the current branch. */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "Merge",
            description = "Merges the specified branch into the current branch.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Merges the named branch into the current branch and imports the resulting project state into
     * ThingWorx. A conflict leaves the repository in a merge-in-progress state; callers should
     * resolve files and use {@link #MergeContinue(String)} or call {@link #MergeAbort()}.
     *
     * @param BranchName branch or ref to merge into the current branch
     * @return merge status or a prefixed error message
     */
    public InfoTable Merge(
            @ThingworxServiceParameter(
                            name = "BranchName",
                            description = "The branch to merge from",
                            baseType = "STRING")
                    String BranchName) {
        if (BranchName.isBlank()) {
            String errMsg = "Merge Error: " + Const.ERR_PREFIX_CONFIG + "BranchName is required.";

            return DapResults.failure("Merge", errMsg, StringResultContract.SERVICE_RESULT);
        }
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git myGitFolder = fileRepo.git();
            ObjectId mergeBase = myGitFolder.getRepository().resolve(BranchName);
            if (mergeBase == null) {
                return DapResults.failure(
                        "Merge",
                        "Merge Error: " + String.format(Const.ERR_BRANCH_NOT_FOUND, BranchName),
                        StringResultContract.SERVICE_RESULT);
            }
            MergeResult[] merged = new MergeResult[1];
            ConflictWorkflow.Outcome outcome =
                    ConflictWorkflow.run(
                            myGitFolder,
                            "Merge",
                            () -> {
                                merged[0] = myGitFolder.merge().include(mergeBase).call();
                                return ConflictWorkflow.outcome(merged[0]);
                            });
            if (outcome == ConflictWorkflow.Outcome.CONFLICTS) {
                return conflictResult("Merge", "Merge", myGitFolder);
            }
            String LogResult = merged[0].getMergeStatus().toString() + ": " + merged[0].toString();
            fileRepo.importProjectEntities();
            GitActionLogger.logAction("Merge", resolveTargetThing().getName(), LogResult);
            return DapResults.success("Merge", LogResult, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("Merge", e, StringResultContract.SERVICE_RESULT);
        }
    }

    /** Rebases the current branch onto the requested upstream branch. */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "Rebase",
            description = "Rebases the current branch onto the specified upstream branch.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Rebases the current branch onto the requested upstream ref. Successful results are imported
     * into ThingWorx; conflicts leave the rebase paused for resolution, continuation, skipping, or
     * aborting through the corresponding services.
     *
     * @param UpstreamBranch branch, tag, or commit to use as the rebase upstream
     * @return rebase status or a prefixed error message
     */
    public InfoTable Rebase(
            @ThingworxServiceParameter(
                            name = "UpstreamBranch",
                            description = "The branch or commit to rebase onto",
                            baseType = "STRING")
                    String UpstreamBranch) {
        if (UpstreamBranch.isBlank()) {
            String errMsg =
                    "Rebase Error: " + Const.ERR_PREFIX_CONFIG + "UpstreamBranch is required.";

            return DapResults.failure("Rebase", errMsg, StringResultContract.SERVICE_RESULT);
        }
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git myGitFolder = fileRepo.git();
            ObjectId upstream = myGitFolder.getRepository().resolve(UpstreamBranch);
            if (upstream == null) {
                return DapResults.failure(
                        "Rebase",
                        "Rebase Error: "
                                + String.format(Const.ERR_UPSTREAM_NOT_FOUND, UpstreamBranch),
                        StringResultContract.SERVICE_RESULT);
            }
            RebaseResult[] rebased = new RebaseResult[1];
            ConflictWorkflow.Outcome outcome =
                    ConflictWorkflow.run(
                            myGitFolder,
                            "Rebase",
                            () -> {
                                rebased[0] = myGitFolder.rebase().setUpstream(upstream).call();
                                return ConflictWorkflow.outcome(rebased[0]);
                            });
            if (outcome == ConflictWorkflow.Outcome.CONFLICTS) {
                return conflictResult("Rebase", "Rebase", myGitFolder);
            }
            String LogResult = rebased[0].getStatus().toString();
            fileRepo.importProjectEntities();
            GitActionLogger.logAction("Rebase", resolveTargetThing().getName(), LogResult);
            return DapResults.success("Rebase", LogResult, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("Rebase", e, StringResultContract.SERVICE_RESULT);
        }
    }

    /** Creates a tag at the requested commit or current repository head. */
    @DapServicePayload(StringResultSpec.class)
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
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Creates an annotated local tag. The project is exported first so the tag points at a commit
     * representing current ThingWorx state. No remote tag is created or pushed automatically.
     *
     * @param TagName tag name
     * @param Message annotation message
     * @param CommitID optional target commit; blank uses {@code HEAD}
     * @return a {@code GIT.StringResult.DataShape} table
     */
    public InfoTable CreateTag(
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
                    String CommitID) {
        String CurrentMethodName = "CreateTag";
        if (TagName.isBlank()) {

            return DapResults.failure(
                    "CreateTag",
                    "CreateTag skipped: " + Const.ERR_PREFIX_CONFIG + Const.ERR_NO_TAG_NAME,
                    StringResultContract.SERVICE_RESULT);
        }
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git myGitFolder = fileRepo.git();
            Repository repo = myGitFolder.getRepository();
            ObjectId commitId;
            if (CommitID != null && !CommitID.isBlank()) {
                commitId = repo.resolve(CommitID);
                if (commitId == null) {
                    return DapResults.failure(
                            "CreateTag",
                            "CreateTag Error: "
                                    + String.format(Const.ERR_COMMIT_NOT_FOUND, CommitID),
                            StringResultContract.SERVICE_RESULT);
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
            if (Message != null && !Message.isBlank()) {
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
            String LogResult =
                    String.format(Const.SUCCESS_TAG_CREATED, TagName, tagRef.getObjectId().name());

            return DapResults.success("CreateTag", LogResult, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure(CurrentMethodName, e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(TagListSpec.class)
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
            aspects = {"isEntityDataShape:true", "dataShape:GIT.TagList.ServiceResult.DataShape"})
    /**
     * Lists local tags and their target commits. This successFromString is read-only and does not
     * fetch tags from the remote.
     *
     * @return a {@code GIT.TagList.ServiceResult.DataShape} table, or an empty table when listing
     *     fails
     */
    public InfoTable GetTagList() {
        log.trace("Entering Service: GetTagList");
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git myGitFolder = fileRepo.git();
            Repository repo = myGitFolder.getRepository();
            InfoTable TagList =
                    InfoTableInstanceFactory.createInfoTableFromDataShape("GIT.TagList.DataShape");
            List<Ref> tags = myGitFolder.tagList().call();
            for (Ref tagRef : tags) {
                tagRef = repo.getRefDatabase().peel(tagRef);
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
                TagList.addRow(vc);
            }
            log.trace("Exiting Service: GetTagList");
            return DapResults.success(
                    "GetTagList", TagListTable.SERVICE_RESULT, TagListTable.wrap(TagList));
        } catch (Exception ex) {

            return DapResults.failure("GetTagList", ex, TagListTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "DeleteTag",
            description = "Deletes a tag from the repository.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable DeleteTag(
            @ThingworxServiceParameter(
                            name = "TagName",
                            description = "Name of the tag to delete",
                            baseType = "STRING")
                    String TagName) {
        String CurrentMethodName = "DeleteTag";
        if (TagName.isBlank()) {

            return DapResults.failure(
                    CurrentMethodName,
                    "DeleteTag Error: TagName is required.",
                    StringResultContract.SERVICE_RESULT);
        }
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git myGitFolder = fileRepo.git();
            myGitFolder.tagDelete().setTags("refs/tags/" + TagName).call();
            String LogResult = String.format(Const.SUCCESS_TAG_DELETED, TagName);

            return DapResults.success(
                    CurrentMethodName, LogResult, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure(
                    CurrentMethodName,
                    "DeleteTag Error: "
                            + Const.ERR_PREFIX_GIT
                            + "Failed to delete tag '"
                            + TagName
                            + "'. "
                            + e.getMessage(),
                    StringResultContract.SERVICE_RESULT);
        }
    }
}
