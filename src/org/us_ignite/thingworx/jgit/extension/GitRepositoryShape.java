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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
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
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
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

/**
 * ThingWorx repository implementation backed by an on-disk JGit repository.
 *
 * <p>The repository location is resolved from this Thing, which is itself a FileRepository, and its
 * configured path. Services on this class operate on the current repository state and return either
 * a status message or the InfoTable shape declared by the corresponding ThingWorx service
 * annotation.
 */
@ThingworxBaseTemplateDefinition(name = "GenericThing")
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
     * Exports the configured ThingWorx project and creates a local commit from the current Git
     * index. This method does not push to the remote; callers must invoke {@link #Push(String,
     * String, String, Boolean)} separately. The commit identity and optional signing key come from
     * the current user's repository configuration.
     *
     * @param Message commit message; blank messages are passed to JGit and may be rejected
     * @return a success message, or a prefixed error message when configuration, authentication,
     *     signing, synchronization, or Git commit processing fails
     */
    @ThingworxServiceDefinition(
            name = "Commit",
            description =
                    "Syncs the ThingWorx project entities, then commits the current Git index. Does not push to the remote.",
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
        log.trace("Entering Service: Commit");
        String CurrentMethodName = "Commit";
        try {

            User currentUser = UserUtilities.findUser(UserUtilities.getCurrentUser());
            GitUserContextManager userContext = new GitUserContextManager();
            ValueCollection RepoCredentials =
                    userContext.credentialsFor(resolveTargetThing().getName());
            String CommitterName = RepoCredentials.getStringValue(Const.GitCommitterName);
            String CommitterEmail = RepoCredentials.getStringValue(Const.GitCommitterEmail);
            if (CommitterName.isBlank() || CommitterEmail.isBlank()) {

                return ServiceResults.failureFromString(
                        CurrentMethodName,
                        "Commit Error: " + Const.ERR_PREFIX_CONFIG + Const.ERR_NO_COMMITTER);
            }

            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git myGitObject = fileRepo.git();
            var commitCmd =
                    myGitObject
                            .commit()
                            .setMessage(Message)
                            .setCommitter(CommitterName, CommitterEmail);

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
                                if (fingerprint.equals(
                                        key.getStringValue(Const.GpgKeyFingerprint))) {
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
                        ((PasswordPrimitive) GpgKey.getPrimitive(Const.GpgPrivateKey)).getValue();
                GpgPassphrase =
                        ((PasswordPrimitive) GpgKey.getPrimitive(Const.GpgKeyPassphrase))
                                .getValue();
            }

            PastedKeyGpgSigner gpgSigner = null;
            if (GpgPrivateKey != null && !GpgPrivateKey.isBlank()) {
                gpgSigner = new PastedKeyGpgSigner(GpgPrivateKey, GpgPassphrase);
                Signers.set(GpgConfig.GpgFormat.OPENPGP, gpgSigner);
                commitCmd.setSign(true).setSigningKey(null).setSigner(gpgSigner);
                commitCmd.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(null, GpgPassphrase));
            }

            commitCmd.call();

            if (gpgSigner != null) {
                gpgSigner.clearSensitiveData();
            }

            String Result = String.format(Const.SUCCESS_COMMIT, Message);
            fileRepo.close();
            return ServiceResults.successFromString(CurrentMethodName, Result);
        } catch (Exception e) {
            return ServiceResults.fromError(CurrentMethodName, e);
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
                    Boolean SetUpstream) {
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

                return ServiceResults.failureFromString(
                        "Push", "Push Error: " + Const.ERR_PREFIX_AUTH + Const.ERR_NO_CREDENTIALS);
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
                return ServiceResults.failureFromString(
                        "Push",
                        "Push Error: "
                                + Const.ERR_PREFIX_CONFIG
                                + "A detached HEAD requires an explicit BranchName.");
            }
            if (repository.findRef("refs/heads/" + sourceBranch) == null) {
                fileRepo.close();
                return ServiceResults.failureFromString(
                        "Push",
                        "Push Error: "
                                + Const.ERR_PREFIX_CONFIG
                                + "Local branch '"
                                + sourceBranch
                                + "' was not found.");
            }
            String upstreamRemote =
                    repository.getConfig().getString("branch", sourceBranch, "remote");
            String remote =
                    Remote == null || Remote.isBlank()
                            ? (upstreamRemote == null || upstreamRemote.isBlank()
                                    ? "origin"
                                    : upstreamRemote)
                            : Remote;
            String destinationBranch =
                    RemoteBranchName == null || RemoteBranchName.isBlank()
                            ? sourceBranch
                            : RemoteBranchName;
            CredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(User, Password);
            var pushCommand =
                    myGitObject
                            .push()
                            .setRemote(remote)
                            .setCredentialsProvider(credentialsProvider);
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
                return ServiceResults.failureFromString(
                        "Push", "Push Error: " + Const.ERR_PREFIX_GIT + pushError);
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

            return ServiceResults.successFromString("Push", LogResult);
        } catch (Exception e) {
            return ServiceResults.fromError(CurrentMethodName, e);
        }
    }

    @ThingworxServiceDefinition(
            name = "SetGPGKeyForSigning",
            description = "Selects or clears the current user's signing key for this repository",
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
                            description = "Owned GPG fingerprint; blank disables signing",
                            baseType = "STRING")
                    String fingerprint) {
        try {
            GitUserContextManager userContext = new GitUserContextManager();
            userContext.requireUser();
            if (!fingerprint.isBlank()) {
                userContext.validateGpgKeyOwnership(fingerprint);
            }
            userContext.setRepositoryGpgKey(resolveTargetThing().getName(), fingerprint);
            return ServiceResults.successFromString(
                    "SetGPGKeyForSigning", "GPG signing key updated.");
        } catch (Exception e) {

            return ServiceResults.fromError("SetGPGKeyForSigning", e);
        }
    }

    /** Fetches and integrates changes from the configured remote repository. */
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
        try {

            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git myGitFolder = fileRepo.git();
            // An unborn repository has no local Git state to preserve. Exporting
            // ThingWorx entities before the first checkout would create untracked
            // files that conflict with the remote tree during bootstrap.
            if (myGitFolder.getRepository().resolve("HEAD") != null) {
                fileRepo.exportProjectEntities();
            }
            ValueCollection RepoCredentials =
                    new GitUserContextManager().credentialsFor(resolveTargetThing().getName());
            String User = RepoCredentials.getStringValue(Const.GitCommitterUser);
            String Password = RepoCredentials.getStringValue(Const.GitCommitterPassword);
            if (User.isBlank() || Password.isBlank()) {
                fileRepo.close();
                return ServiceResults.failureFromString(
                        CurrentMethodName,
                        "Pull Error: " + Const.ERR_PREFIX_AUTH + Const.ERR_NO_CREDENTIALS);
            }
            CredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(User, Password);
            if (Force.booleanValue()) {
                myGitFolder.reset().setMode(ResetType.HARD).call();
            }
            PullResult pr =
                    myGitFolder
                            .pull()
                            .setRemote("origin")
                            .setCredentialsProvider(credentialsProvider)
                            .call();
            String LogResult =
                    String.format(
                            Const.SUCCESS_PULL,
                            pr.isSuccessful() ? "Successful" : "Unsuccessful",
                            pr.toString());
            fileRepo.importProjectEntities();
            fileRepo.close();

            return ServiceResults.successFromString(CurrentMethodName, LogResult);
        } catch (Exception e) {
            return ServiceResults.fromError(CurrentMethodName, e);
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
                            baseType = "STRING")
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

                return ServiceResults.failureFromString(
                        CurrentMethodName,
                        "Fetch Error: " + Const.ERR_PREFIX_AUTH + Const.ERR_NO_CREDENTIALS);
            }
            CredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(User, Password);
            String remote = Remote.isBlank() ? "origin" : Remote;
            myGitFolder
                    .fetch()
                    .setRemote(remote)
                    .setCredentialsProvider(credentialsProvider)
                    .call();
            String LogResult = "Fetch from '" + remote + "' completed successfully.";

            return ServiceResults.successFromString(CurrentMethodName, LogResult);
        } catch (Exception e) {
            return ServiceResults.fromError(CurrentMethodName, e);
        }
    }

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

            return ServiceResults.failureFromString("BranchCreate", errMsg);
        }
        try {

            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git myGitFolder = fileRepo.git();
            StartPoint = Optional.ofNullable(StartPoint).orElse("HEAD");
            Ref branchRef =
                    myGitFolder.branchCreate().setName(BranchName).setStartPoint(StartPoint).call();
            fileRepo.importProjectEntities();

            log.trace("Exiting Service: BranchCreate");
            return ServiceResults.successFromString("BranchCreate", branchRef.toString());
        } catch (Exception e) {
            return ServiceResults.failureFromString(
                    "BranchCreate",
                    "BranchCreate Error: "
                            + Const.ERR_PREFIX_GIT
                            + "Branch '"
                            + BranchName
                            + "' already exists. Use a different branch name or delete the existing branch first.");
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

            return ServiceResults.failureFromString(
                    "Checkout", "Checkout: BranchNameOrCommit is required.");
        }
        String CurrentMethodName = "Checkout";
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git myGitFolder = fileRepo.git();
            Ref ref;
            try {
                var checkout = myGitFolder.checkout().setName(BranchNameOrCommit);
                if (CreateBranch.booleanValue()) {
                    checkout.setCreateBranch(true)
                            .setStartPoint(StartPoint.isBlank() ? "HEAD" : StartPoint);
                }
                checkout.setForced(Force.booleanValue());
                ref = checkout.call();
            } catch (RefNotFoundException ex) {
                if (CreateBranch.booleanValue()) throw ex;
                if (myGitFolder.getRepository().findRef("refs/remotes/origin/" + BranchNameOrCommit)
                        == null) {
                    throw ex;
                }
                log.warn("Checkout: creating tracking branch from origin/" + BranchNameOrCommit);
                ref =
                        myGitFolder
                                .checkout()
                                .setCreateBranch(true)
                                .setName(BranchNameOrCommit)
                                .setUpstreamMode(SetupUpstreamMode.TRACK)
                                .setStartPoint("origin/" + BranchNameOrCommit)
                                .call();
            }
            fileRepo.importProjectEntities();
            String LogResult = (ref != null) ? ref.toString() : "No message.";

            log.trace("Exiting Service: Checkout");
            return ServiceResults.successFromString("Checkout", LogResult);
        } catch (Exception e) {
            return ServiceResults.fromError(CurrentMethodName, e);
        }
    }

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
                    String BranchName) {
        try {
            if (BranchName.isBlank())
                return ServiceResults.failureFromString("BranchSwitch", "BranchName is required.");
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git git = fileRepo.git();
            Ref ref;
            try {
                ref = git.checkout().setName(BranchName).call();
            } catch (RefNotFoundException e) {
                ref =
                        git.checkout()
                                .setCreateBranch(true)
                                .setName(BranchName)
                                .setUpstreamMode(SetupUpstreamMode.TRACK)
                                .setStartPoint("origin/" + BranchName)
                                .call();
            }

            fileRepo.importProjectEntities();
            return ServiceResults.successFromString("BranchSwitch", ref.toString());
        } catch (Exception e) {
            return ServiceResults.fromError("BranchSwitch", e);
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
            String currentBranch = fullBranch != null ? fullBranch : repository.getBranch();
            ValueCollection vc = new ValueCollection();

            vc.put("BranchName", new StringPrimitive(currentBranch));
            vc.put("DetachedHEAD", new BooleanPrimitive(fullBranch.startsWith("refs/heads/")));
            CurrentBranchStatus.addRow(vc);
            log.trace("Exiting Service: GetCurrentBranch");
            return ServiceResults.successFromPayload(
                    "GetCurrentBranch",
                    "GIT.CurrentBranchStatus.ServiceResult.DataShape",
                    CurrentBranchStatus);
        } catch (Exception ex) {

            return ServiceResults.fromError(
                    "GetCurrentBranch", ex, "GIT.CurrentBranchStatus.ServiceResult.DataShape");
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
                BranchList.addRow(vc);
            }
            log.trace("Exiting Service: GetBranchList");
            return ServiceResults.successFromPayload(
                    "GetBranchList", "GIT.BranchList.ServiceResult.DataShape", BranchList);

        } catch (Exception ex) {
            return ServiceResults.fromError(
                    "GetBranchList", ex, "GIT.BranchList.ServiceResult.DataShape");
        }
    }

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

            return ServiceResults.failureFromString("BranchDelete", errMsg);
        }
        if (DeleteRemote.booleanValue() && Remote.isBlank()) {
            return ServiceResults.failureFromString(
                    "BranchDelete",
                    "BranchDelete Error: "
                            + Const.ERR_PREFIX_CONFIG
                            + "Remote is required when DeleteRemote is true.");
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

            return ServiceResults.successFromString("BranchDelete", LogResult);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("CannotDelete")) {
                return ServiceResults.failureFromString(
                        "BranchDelete",
                        "BranchDelete Error: "
                                + Const.ERR_PREFIX_GIT
                                + "Branch '"
                                + BranchName
                                + "' cannot be deleted. It may be the current branch. Checkout a different branch first.");
            }
            return ServiceResults.fromError("BranchDelete", e);
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
            aspects = {"isEntityDataShape:true", "dataShape:GIT.CommitLog.ServiceResult.DataShape"})
    /**
     * Returns commit history in newest-first order. A blank ref resolves to the current branch, or
     * to the configured initial branch when HEAD is detached. A zero or null limit returns all
     * reachable commits.
     *
     * @param Ref optional branch, tag, commit, or ref name
     * @param MaxEntries maximum number of rows; null or zero means no limit
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
                    Integer MaxEntries) {
        log.trace("Entering Service: GetLog");
        try {
            InfoTable result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.CommitLogDataShapeName);
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Repository repository = fileRepo.git().getRepository();
            String historyRef = Ref;
            if (historyRef.isBlank()) {
                String fullBranch = repository.getFullBranch();
                historyRef =
                        fullBranch != null && fullBranch.startsWith("refs/heads/")
                                ? fullBranch.substring("refs/heads/".length())
                                : null;
            }
            ObjectId objectId = repository.resolve(historyRef);
            if (objectId == null && !historyRef.startsWith("refs/")) {
                objectId = repository.resolve("refs/heads/" + historyRef);
            }
            if (objectId == null) {

                return ServiceResults.successFromPayload(
                        "GetLog", "GIT.CommitLog.ServiceResult.DataShape", result);
            }
            Iterable<RevCommit> commits = fileRepo.git().log().add(objectId).call();
            int count = 0;
            for (RevCommit commit : commits) {
                if (MaxEntries != null && MaxEntries > 0 && count >= MaxEntries) break;
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
                                author == null || author.getName() == null
                                        ? ""
                                        : author.getName()));
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
                count++;
            }
            return ServiceResults.successFromPayload(
                    "GetLog", "GIT.CommitLog.ServiceResult.DataShape", result);
        } catch (Exception ex) {

            return ServiceResults.fromError("GetLog", ex, "GIT.CommitLog.ServiceResult.DataShape");
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
            String ref = Ref.isBlank() ? "HEAD" : Ref;
            if (!ref.startsWith("refs/") && !"HEAD".equals(ref)) {
                ref = "refs/heads/" + ref;
            }
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
            return ServiceResults.successFromPayload(
                    "GetReflog", "GIT.ReflogEntry.ServiceResult.DataShape", result);
        } catch (Exception ex) {

            return ServiceResults.fromError(
                    "GetReflog", ex, "GIT.ReflogEntry.ServiceResult.DataShape");
        }
    }

    /** Returns the working-tree status using the declared GIT.Status data shape. */
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
            aspects = {"isEntityDataShape:true", "dataShape:GIT.Status.ServiceResult.DataShape"})
    /**
     * Reports working-tree changes against the local Git index. Project export is performed first,
     * so the status includes current ThingWorx entity changes.
     *
     * @return a {@code GIT.Status.DataShape} table containing modified, added, removed, and
     *     untracked paths
     */
    public InfoTable Status() {
        log.trace("Entering Service: Status");
        try {
            InfoTable Status =
                    InfoTableInstanceFactory.createInfoTableFromDataShape("GIT.Status.DataShape");
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git myGitObject = fileRepo.git();
            org.eclipse.jgit.api.Status status = myGitObject.status().call();
            var names =
                    Arrays.asList(
                            "Added",
                            "Changed",
                            "Ignored",
                            "Modified",
                            "Missing",
                            "Removed",
                            "Untracked",
                            "UntrackedFolder");
            var staged = Arrays.asList(true, true, false, false, false, true, false, false);
            var files =
                    Arrays.asList(
                            status.getAdded(),
                            status.getChanged(),
                            status.getIgnoredNotInIndex(),
                            status.getModified(),
                            status.getMissing(),
                            status.getRemoved(),
                            status.getUntracked(),
                            status.getUntrackedFolders());

            for (var i = 0; names.size() > i + 1; i++) {
                for (var f : files.get(i)) {
                    ValueCollection row = new ValueCollection();
                    row.put("File", new StringPrimitive(f));
                    row.put("Status", new StringPrimitive(names.get(i)));
                    row.put("Staged", new BooleanPrimitive(staged.get(i)));
                    Status.addRow(row);
                }
            }

            log.trace("Exiting Service: Status");
            return ServiceResults.successFromPayload(
                    "Status", "GIT.Status.ServiceResult.DataShape", Status);
        } catch (Exception e) {
            return ServiceResults.fromError("Status", e, "GIT.Status.ServiceResult.DataShape");
        }
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
            return ServiceResults.successFromPayload(
                    "GetConflictFiles", "GIT.Status.ServiceResult.DataShape", result);
        } catch (Exception e) {

            return ServiceResults.fromError(
                    "GetConflictFiles", e, "GIT.Status.ServiceResult.DataShape");
        }
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
            return ServiceResults.successFromString(
                    "ReadConflictFile", fileRepo.fileRepository().LoadText(File));
        } catch (Exception e) {
            return ServiceResults.fromError("ReadConflictFile", e);
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
            return ServiceResults.successFromString(
                    "WriteConflictFile", "Wrote conflict file " + File);
        } catch (Exception e) {
            return ServiceResults.fromError("WriteConflictFile", e);
        }
    }

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
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            boolean all = All.booleanValue();
            String pattern = all ? "." : File;
            fileRepo.stagePath(pattern);
            String message = all ? "Staged all non-ignored changes" : "Staged " + pattern;
            return ServiceResults.successFromString("Add", message);
        } catch (Exception e) {
            return ServiceResults.fromError("Add", e);
        }
    }

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
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.git().rm().setCached(true).addFilepattern(File).call();
            return ServiceResults.successFromString("Remove", "Removed from index " + File);
        } catch (Exception e) {
            return ServiceResults.fromError("Remove", e);
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
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable MergeContinue(
            @ThingworxServiceParameter(
                            name = "Message",
                            description = "Optional merge commit message",
                            baseType = "STRING")
                    String Message) {
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git git = fileRepo.git();
            Repository repo = git.getRepository();
            if (repo.getRepositoryState() != RepositoryState.MERGING
                    && repo.getRepositoryState() != RepositoryState.MERGING_RESOLVED)
                return ServiceResults.failureFromString(
                        "MergeContinue", "Error: no merge is in progress.");
            ValueCollection identity =
                    new GitUserContextManager().credentialsFor(resolveTargetThing().getName());
            String name = identity.getStringValue(Const.GitCommitterName);
            String email = identity.getStringValue(Const.GitCommitterEmail);
            if (name.isBlank() || email.isBlank())
                return ServiceResults.failureFromString(
                        "MergeContinue", "Error: " + Const.ERR_NO_COMMITTER);
            String msg = Message.isBlank() ? repo.readMergeCommitMsg() : Message;
            RevCommit commit =
                    git.commit()
                            .setMessage(msg.isBlank() ? "Merge resolved" : msg)
                            .setCommitter(name, email)
                            .call();
            fileRepo.importProjectEntities();
            return ServiceResults.successFromString(
                    "MergeContinue", "completed: " + commit.getId().name());
        } catch (Exception e) {
            return ServiceResults.fromError("MergeContinue", e);
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
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable MergeAbort() {
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git git = fileRepo.git();
            Repository repo = git.getRepository();
            if (repo.getRepositoryState() != RepositoryState.MERGING
                    && repo.getRepositoryState() != RepositoryState.MERGING_RESOLVED)
                return ServiceResults.failureFromString(
                        "MergeAbort", "Error: no merge is in progress.");
            git.reset()
                    .setMode(ResetType.MERGE)
                    .setRef(repo.resolve("ORIG_HEAD") == null ? "HEAD" : "ORIG_HEAD")
                    .call();
            repo.writeMergeCommitMsg(null);
            repo.writeMergeHeads(null);
            fileRepo.importProjectEntities();
            return ServiceResults.successFromString("MergeAbort", "aborted");
        } catch (Exception e) {
            return ServiceResults.fromError("MergeAbort", e);
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
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable RebaseContinue() {
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
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable RebaseSkip() {
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
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable RebaseAbort() {
        return rebaseOperation(org.eclipse.jgit.api.RebaseCommand.Operation.ABORT, "RebaseAbort");
    }

    private InfoTable rebaseOperation(
            org.eclipse.jgit.api.RebaseCommand.Operation successFromString, String service) {
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git git = fileRepo.git();
            if (!git.getRepository().getRepositoryState().isRebasing())
                return ServiceResults.failureFromString(
                        service, " Error: no rebase is in progress.");
            RebaseResult result = git.rebase().setOperation(successFromString).call();
            if (result.getStatus() == RebaseResult.Status.OK
                    || result.getStatus() == RebaseResult.Status.ABORTED) {
                fileRepo.importProjectEntities();
            }
            return ServiceResults.successFromString(service, result.getStatus().toString());
        } catch (Exception e) {
            return ServiceResults.fromError(service, e);
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
            return ServiceResults.successFromString("GetDiffPerFile", dif.toString("UTF-8"));
        } catch (Exception e) {
            return ServiceResults.fromError("GetDiffPerFile", e);
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
                return ServiceResults.failureFromString(
                        "GetDiffPerFileBetweenCommits",
                        "Diff Error: " + String.format(Const.ERR_COMMIT_NOT_FOUND, FromCommitID));
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
                return ServiceResults.failureFromString(
                        "GetDiffPerFileBetweenCommits",
                        String.format(Const.ERR_DIFF_TOO_LARGE, maxSize));
            }
            return ServiceResults.successFromString("GetDiffPerFileBetweenCommits", DiffResult);
        } catch (Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));

            return ServiceResults.fromError("GetDiffPerFileBetweenCommits", ex);
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
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.CommitInfo.ServiceResult.DataShape"
            })
    public InfoTable GetCommitInfo(
            @ThingworxServiceParameter(name = "CommitID", description = "", baseType = "STRING")
                    String CommitID) {
        log.trace("Entering Service: GetCommitInfo");
        if (CommitID == null) {
            return ServiceResults.failureFromResultShape(
                    "GetCommitInfo", "GIT.CommitInfo.ServiceResult.DataShape");
        }
        try {
            InfoTable Status =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.CommitInfoDataShapeName);
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Repository myGitRepository = fileRepo.git().getRepository();
            ObjectId commit = myGitRepository.resolve(CommitID);
            if (commit == null) {

                return ServiceResults.failureFromResultShape(
                        "GetCommitInfo", "GIT.CommitInfo.ServiceResult.DataShape");
            }
            try (RevWalk walk = new RevWalk(myGitRepository)) {
                RevCommit commitAgain = walk.parseCommit(commit);
                ValueCollection vc = new ValueCollection();
                vc.put("CommitID", new StringPrimitive(commitAgain.getId().name()));
                String parents = "";
                if (commitAgain.getParents() != null) {
                    for (RevCommit parent : commitAgain.getParents()) parents += parent.getName();
                }
                vc.put("Parents", new StringPrimitive(parents));
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
                List<DiffEntry> entries;
                if (commitAgain.getParentCount() != 0)
                    entries = diffFormatter.scan(commitAgain.getParent(0), commitAgain.getTree());
                else entries = diffFormatter.scan(null, commitAgain.getTree());

                for (DiffEntry diffEntry : entries) {
                    ValueCollection v2 = new ValueCollection();
                    v2.put("FileName", new StringPrimitive(diffEntry.getNewPath()));
                    v2.put("Status", new StringPrimitive(diffEntry.getChangeType().toString()));
                    CommitChangedFiles.addRow(v2);
                }
                diffFormatter.close();

                vc.put("ChangedFiles", new InfoTablePrimitive(CommitChangedFiles));
                Status.addRow(vc);
                walk.dispose();
                log.trace("Exiting Service: GetCommitInfo");
            }
            return ServiceResults.successFromPayload(
                    "GetCommitInfo", "GIT.CommitInfo.ServiceResult.DataShape", Status);
        } catch (Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            log.error("GetCommitInfo failed for CommitID '" + CommitID + "': " + errors.toString());
            return ServiceResults.fromError(
                    "GetCommitInfo", ex, "GIT.CommitInfo.ServiceResult.DataShape");
        }
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

            return ServiceResults.failureFromString("Merge", errMsg);
        }
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git myGitFolder = fileRepo.git();
            ObjectId mergeBase = myGitFolder.getRepository().resolve(BranchName);
            if (mergeBase == null) {
                return ServiceResults.failureFromString(
                        "Merge",
                        "Merge Error: " + String.format(Const.ERR_BRANCH_NOT_FOUND, BranchName));
            }
            MergeResult result = myGitFolder.merge().include(mergeBase).call();
            String LogResult = result.getMergeStatus().toString() + ": " + result.toString();
            if (result.getMergeStatus().isSuccessful()) {
                fileRepo.importProjectEntities();
            } else if (result.getMergeStatus() == MergeResult.MergeStatus.CONFLICTING) {

            }

            return ServiceResults.successFromString("Merge", LogResult);
        } catch (Exception e) {
            return ServiceResults.fromError("Merge", e);
        }
    }

    /** Rebases the current branch onto the requested upstream branch. */
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

            return ServiceResults.failureFromString("Rebase", errMsg);
        }
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            fileRepo.exportProjectEntities();
            Git myGitFolder = fileRepo.git();
            ObjectId upstream = myGitFolder.getRepository().resolve(UpstreamBranch);
            if (upstream == null) {
                return ServiceResults.failureFromString(
                        "Rebase",
                        "Rebase Error: "
                                + String.format(Const.ERR_UPSTREAM_NOT_FOUND, UpstreamBranch));
            }
            RebaseResult result = myGitFolder.rebase().setUpstream(upstream).call();
            String LogResult = result.getStatus().toString() + ": " + result.toString();
            if (result.getStatus() == RebaseResult.Status.OK) {
                fileRepo.importProjectEntities();
            } else if (result.getStatus() == RebaseResult.Status.STOPPED) {

            }

            return ServiceResults.successFromString("Rebase", LogResult);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Conflicting")) {
                return ServiceResults.failureFromString(
                        "Rebase", "Rebase Error: " + Const.ERR_REBASE_CONFLICT);
            }
            return ServiceResults.fromError("Rebase", e);
        }
    }

    /** Creates a tag at the requested commit or current repository head. */
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

            return ServiceResults.failureFromString(
                    "CreateTag",
                    "CreateTag skipped: " + Const.ERR_PREFIX_CONFIG + Const.ERR_NO_TAG_NAME);
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
                    return ServiceResults.failureFromString(
                            "CreateTag",
                            "CreateTag Error: "
                                    + String.format(Const.ERR_COMMIT_NOT_FOUND, CommitID));
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

            return ServiceResults.successFromString("CreateTag", LogResult);
        } catch (Exception e) {
            return ServiceResults.fromError(CurrentMethodName, e);
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
            return ServiceResults.successFromPayload(
                    "GetTagList", "GIT.TagList.ServiceResult.DataShape", TagList);
        } catch (Exception ex) {

            return ServiceResults.fromError(
                    "GetTagList", ex, "GIT.TagList.ServiceResult.DataShape");
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

            return ServiceResults.failureFromString(
                    CurrentMethodName, "DeleteTag Error: TagName is required.");
        }
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git myGitFolder = fileRepo.git();
            myGitFolder.tagDelete().setTags("refs/tags/" + TagName).call();
            String LogResult = String.format(Const.SUCCESS_TAG_DELETED, TagName);

            return ServiceResults.successFromString(CurrentMethodName, LogResult);
        } catch (Exception e) {
            return ServiceResults.failureFromString(
                    CurrentMethodName,
                    "DeleteTag Error: "
                            + Const.ERR_PREFIX_GIT
                            + "Failed to delete tag '"
                            + TagName
                            + "'. "
                            + e.getMessage());
        }
    }
}
