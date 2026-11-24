package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.annotations.ThingworxBaseTemplateDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceParameter;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.system.ContextType;
import com.thingworx.things.Thing;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.BooleanPrimitive;
import com.thingworx.types.primitives.DatetimePrimitive;
import com.thingworx.types.primitives.InfoTablePrimitive;
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
import org.eclipse.jgit.api.RemoteSetUrlCommand;
import org.eclipse.jgit.api.errors.NotMergedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.CheckoutEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
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
import org.eclipse.jgit.lib.Signer;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
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
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.EntityTypeOptionSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.FileContentSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.HistoryRefSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.ReflogEntrySpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.RemoteSignInSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.RemoteSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.RemoteStatusSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.RepositorySigningSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.SigningKeyOptionSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.SshHostKeySpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.StatusSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.StringResultSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.TagListSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.ThingworxSyncActionSpec;
import org.us_ignite.thingworx.jgit.extension.JGitEntityDeclarations.TrackedDataTableSpec;
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
                    description =
                            "Retired: earlier parent folder of every project folder. The upgrade moves it into each tracked project's Path and clears it."),
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
                            "Retired: earlier single ThingWorx project. The upgrade moves it into TrackedProjects and clears it."),
            @DapProperty(
                    name = "TrackedProjects",
                    baseType = DapBaseType.INFOTABLE,
                    dataShape = "GIT.TrackedProject.DataShape",
                    ordinal = 10,
                    category = "Repository",
                    description = "ThingWorx projects synchronized with this repository"),
            @DapProperty(
                    name = "RequireSigning",
                    baseType = DapBaseType.BOOLEAN,
                    ordinal = 11,
                    category = "Repository",
                    defaultValue = "false",
                    description =
                            "When true, commits in this repository fail unless they can be signed")
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
     * Commits only the files already staged in the Git index, like {@link #CommitStaged}. ThingWorx
     * entities are not exported; use {@link #SyncFromThingWorx} first.
     *
     * @param Message commit message; required
     * @return a success message, or a prefixed error message when configuration, authentication,
     *     signing, synchronization, or Git commit processing fails
     */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "Commit",
            description =
                    "Commits only files already staged in the Git index. Same as CommitStaged; kept for existing callers. Does not sync ThingWorx entities or push to the remote.",
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
                    String Message,
            @ThingworxServiceParameter(
                            name = "SigningKey",
                            description =
                                    "Optional key for this commit only, as GetSigningKeyOptions returns it: openpgp:<fingerprint>, ssh:<fingerprint>, or none for unsigned. Blank uses the repository's signing key.",
                            baseType = "STRING")
                    String SigningKey) {
        return commitIndex("Commit", Message, SigningKey);
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
                    String Message,
            @ThingworxServiceParameter(
                            name = "SigningKey",
                            description =
                                    "Optional key for this commit only, as GetSigningKeyOptions returns it: openpgp:<fingerprint>, ssh:<fingerprint>, or none for unsigned. Blank uses the repository's signing key.",
                            baseType = "STRING")
                    String SigningKey) {
        return commitIndex("CommitStaged", Message, SigningKey);
    }

    /**
     * Signs with the repository's saved choice; see {@link #configureSigning(CommitCommand, String,
     * String)}.
     */
    private Signer configureSigning(CommitCommand commitCmd) throws Exception {
        return configureSigning(commitCmd, null, null);
    }

    /**
     * Signs {@code commitCmd} with the current user's signing key for this repository, or with the
     * key chosen for this one commit. The caller must pass the returned signer to {@link
     * #clearSigner(Signer)} after the commit.
     *
     * @param overrideFormat blank for the saved choice, {@code none} for unsigned, or {@code
     *     openpgp}/{@code ssh} with {@code overrideFingerprint}
     * @return the signer, or {@code null} when the commit is not signed
     * @throws IllegalStateException when the repository requires signing and no key applies, or the
     *     selected key has no stored private key
     */
    private Signer configureSigning(
            CommitCommand commitCmd, String overrideFormat, String overrideFingerprint)
            throws Exception {
        Signer signer = resolveSigner(overrideFormat, overrideFingerprint);
        if (signer == null) return null;
        commitCmd.setSign(true).setSigningKey(null).setSigner(signer);
        return signer;
    }

    /**
     * The current user's identity and signing key, applied to every commit JGit creates in {@code
     * repository} while the scope is open, such as the merge commit of a Pull or Merge and the
     * commits a Rebase rewrites. Closing it restores the configuration and clears the key.
     */
    private record CommitScope(CommitSettings settings, Signer signer) implements AutoCloseable {
        @Override
        public void close() {
            try {
                settings.close();
            } finally {
                clearSigner(signer);
            }
        }
    }

    /**
     * Opens a {@link CommitScope} for this repository's saved signing choice.
     *
     * @throws IllegalStateException when the repository requires signing and no key applies
     */
    private CommitScope commitScope(Repository repository) throws Exception {
        String thing = resolveTargetThing().getName();
        GitUserContextManager.Identity identity = new GitUserContextManager().identityFor(thing);
        Signer signer = resolveSigner(null, null);
        try {
            return new CommitScope(
                    CommitSettings.apply(repository, identity.name(), identity.email(), signer),
                    signer);
        } catch (RuntimeException e) {
            clearSigner(signer);
            throw e;
        }
    }

    /**
     * The signer for the current user's signing key in this repository, or for the key chosen for
     * one operation. The caller must pass it to {@link #clearSigner(Signer)} when done.
     *
     * @param overrideFormat blank for the saved choice, {@code none} for unsigned, or {@code
     *     openpgp}/{@code ssh} with {@code overrideFingerprint}
     * @return the signer, or {@code null} when commits are not signed
     * @throws IllegalStateException when the repository requires signing and no key applies, or the
     *     selected key has no stored private key
     */
    private Signer resolveSigner(String overrideFormat, String overrideFingerprint)
            throws Exception {
        String repository = resolveTargetThing().getName();
        GitUserContextManager userContext = new GitUserContextManager();
        if (userContext.user() == null) return null;
        ValueCollection row = userContext.credentialsFor(repository);
        SigningSelection.Choice choice =
                SigningSelection.resolve(
                        storedText(row, Const.SigningFormat),
                        storedText(row, Const.GpgKeyFingerprint),
                        storedText(row, Const.SigningSshKeyFingerprint),
                        overrideFormat,
                        overrideFingerprint,
                        requireSigning());
        if (!choice.signs()) return null;
        if (SigningSelection.SSH.equals(choice.format())) {
            ValueCollection key = userContext.sshKey(choice.fingerprint());
            if (key == null) {
                throw new IllegalStateException(
                        Const.ERR_PREFIX_CONFIG
                                + "The SSH key "
                                + choice.fingerprint()
                                + " selected for signing is not one of your SSH keys. Choose"
                                + " another signing key in Repository Settings.");
            }
            String privateKey = GitUserContextManager.secret(key, Const.SshPrivateKey);
            if (privateKey.isBlank()) {
                throw new IllegalStateException(
                        Const.ERR_PREFIX_CONFIG
                                + "The SSH key "
                                + choice.fingerprint()
                                + " selected for signing has no stored private key. Add the key"
                                + " again, or choose another signing key.");
            }
            return new StoredKeySshSigner(
                    privateKey, GitUserContextManager.secret(key, Const.SshKeyPassphrase));
        }

        ValueCollection gpgKey = userContext.gpgKey(choice.fingerprint());
        if (gpgKey == null) {
            if (overrideFormat != null && !overrideFormat.isBlank() || requireSigning()) {
                throw new IllegalStateException(
                        Const.ERR_PREFIX_CONFIG
                                + "The GPG key "
                                + choice.fingerprint()
                                + " selected for signing is not one of your GPG keys.");
            }
            // Earlier versions committed unsigned here, and repositories rely on that.
            log.warn(
                    "Repository {} references GPG key {} which is not owned by the current user;"
                            + " commit will not be signed.",
                    repository,
                    choice.fingerprint());
            return null;
        }
        String storedKey = GitUserContextManager.secret(gpgKey, Const.GpgPrivateKey);
        if (storedKey.isBlank()) {
            // Signing was requested, so do not fall back to an unsigned commit.
            throw new IllegalStateException(
                    Const.ERR_PREFIX_CONFIG
                            + "The GPG key "
                            + choice.fingerprint()
                            + " selected for signing in this repository has no stored private"
                            + " key (it may have been cleared). Add the key again in User"
                            + " Identity, or clear the repository's GPG key to commit"
                            + " unsigned.");
        }
        // A key without a passphrase is stored with an empty one.
        String passphrase = GitUserContextManager.secret(gpgKey, Const.GpgKeyPassphrase);
        return new PastedKeyGpgSigner(
                PastedKeyGpgSigner.normalizePrivateKey(storedKey), passphrase);
    }

    /** Clears the key material a signer from {@link #configureSigning} holds. */
    private static void clearSigner(Signer signer) {
        if (signer instanceof PastedKeyGpgSigner gpg) gpg.clearSensitiveData();
        else if (signer instanceof StoredKeySshSigner ssh) ssh.clearSensitiveData();
    }

    /**
     * This repository's RequireSigning property. False when the property is missing, which happens
     * on a repository Thing that predates the property until the upgrade adds it.
     */
    private boolean requireSigning() {
        try {
            Object value = resolveTargetThing().getPropertyValue(Const.RequireSigning);
            Object raw =
                    value instanceof com.thingworx.types.primitives.IPrimitiveType<?, ?> primitive
                            ? primitive.getValue()
                            : value;
            return Boolean.TRUE.equals(raw);
        } catch (Exception e) {
            return false;
        }
    }

    private InfoTable commitIndex(String serviceName, String Message, String signingKey) {
        log.trace("Entering Service: " + serviceName);
        FileRepositoryManager fileRepo = null;
        Signer gpgSigner = null;
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
            GitUserContextManager.Identity committer =
                    userContext.identityFor(resolveTargetThing().getName());
            String CommitterName = committer.name();
            String CommitterEmail = committer.email();
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
                            .setMessage(CommitAttribution.withAuthenticatedUser(Message))
                            .setCommitter(CommitterName, CommitterEmail);

            SigningSelection.Choice override = SigningSelection.parseKey(signingKey);
            gpgSigner = configureSigning(commitCmd, override.format(), override.fingerprint());

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
            clearSigner(gpgSigner);
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
                    "Pushes existing local commits using the current/upstream branch or explicit local and remote branch. It does not stage files, create commits, or sync ThingWorx entities.",
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
                            description =
                                    "Optional remote name; blank uses the branch upstream remote, or origin",
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
        GitTransport transport = null;
        try {
            long startTimePush = System.nanoTime();
            long endTimeOpenRepository = System.nanoTime();
            BigDecimal durationTimeOpenRepository =
                    new BigDecimal(
                                    (double) (endTimeOpenRepository - startTimePush)
                                            / (double) 1000000)
                            .setScale(3, RoundingMode.HALF_DOWN);
            transport = GitTransport.forRepository(resolveTargetThing().getName());
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
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
            String remote = Remotes.resolve(repository, Remote, sourceBranch);
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
            var pushCommand =
                    transport
                            .configure(myGitObject.push(), remote)
                            .setRemote(remote)
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
            List<String> pushed = new ArrayList<>();
            String pushError = null;
            for (PushResult pr : prList) {
                for (RemoteRefUpdate update : pr.getRemoteUpdates()) {
                    if (ResultMessages.accepted(update)) {
                        pushed.add(ResultMessages.pushed(pr, update, sourceBranch, remote));
                    } else if (pushError == null) {
                        pushError = ResultMessages.pushRejected(pr, update, remote);
                    }
                }
            }
            String LogResult = String.join("\n", pushed);
            log.debug(
                    "Push timings (ms): open repository {}, push {}",
                    durationTimeOpenRepository,
                    durationTimePushFinish);

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
            fileRepo.close();

            GitActionLogger.logAction("Push", resolveTargetThing().getName(), LogResult);
            return DapResults.success("Push", LogResult, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return transportFailure(CurrentMethodName, transport, e);
        } finally {
            if (transport != null) transport.close();
        }
    }

    /**
     * Logs and returns a failed network operation, replacing JGit's authentication errors with a
     * message that says what to fix.
     */
    private InfoTable transportFailure(String operation, GitTransport transport, Exception e) {
        String auth = transport == null ? null : transport.authenticationFailure(e);
        String repository = resolveTargetThing().getName();
        if (auth != null) {
            GitActionLogger.logAction(operation, repository, auth);
            return DapResults.failure(
                    operation, operation + " Error: " + auth, StringResultContract.SERVICE_RESULT);
        }
        GitActionLogger.logAction(operation, repository, String.valueOf(e.getMessage()));
        return DapResults.failure(operation, e, StringResultContract.SERVICE_RESULT);
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

    /**
     * Saves the current user's commit signing key for this repository, in either format. The key
     * must be one of the user's own keys.
     */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "SetRepositorySigningKey",
            description =
                    "Selects the current user's commit signing key for this repository: an OpenPGP (GPG) key or an SSH key. A blank SigningFormat turns signing off.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable SetRepositorySigningKey(
            @ThingworxServiceParameter(
                            name = "SigningFormat",
                            description = "openpgp, ssh, or blank to stop signing",
                            baseType = "STRING")
                    String SigningFormat,
            @ThingworxServiceParameter(
                            name = "KeyFingerprint",
                            description =
                                    "Fingerprint of one of the current user's GPG keys (openpgp) or SSH keys (ssh)",
                            baseType = "STRING")
                    String KeyFingerprint) {
        try {
            GitUserContextManager userContext = new GitUserContextManager();
            userContext.requireUser();
            String format = SigningSelection.format(SigningFormat);
            if (format.equals(SigningSelection.NONE)) format = "";
            String fingerprint = KeyFingerprint == null ? "" : KeyFingerprint.trim();
            if (!format.isEmpty() && fingerprint.isEmpty())
                throw new IllegalArgumentException(
                        "KeyFingerprint is required when SigningFormat is " + format + ".");
            if (format.equals(SigningSelection.OPENPGP)) {
                userContext.validateGpgKeyOwnership(fingerprint);
            } else if (format.equals(SigningSelection.SSH)
                    && userContext.sshKey(fingerprint) == null) {
                throw new IllegalArgumentException(
                        "SSH key is not owned by the current user: fingerprint " + fingerprint);
            }
            userContext.setRepositorySigning(resolveTargetThing().getName(), format, fingerprint);
            String message =
                    format.isEmpty()
                            ? "Commits are no longer signed."
                            : format.equals(SigningSelection.SSH)
                                    ? "Commits are signed with SSH key " + fingerprint + "."
                                    : "Commits are signed with GPG key " + fingerprint + ".";
            if (format.isEmpty() && requireSigning())
                message +=
                        " This repository requires signing, so commits will fail until a key is chosen.";
            GitActionLogger.logAction(
                    "SetRepositorySigningKey", resolveTargetThing().getName(), message);
            return DapResults.success(
                    "SetRepositorySigningKey", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure(
                    "SetRepositorySigningKey", e, StringResultContract.SERVICE_RESULT);
        }
    }

    /** Returns the current user's signing choice for this repository and RequireSigning. */
    @DapServicePayload(RepositorySigningSpec.class)
    @ThingworxServiceDefinition(
            name = "GetRepositorySigning",
            description =
                    "Returns the current user's commit signing key for this repository and whether the repository requires signing",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.RepositorySigning.ServiceResult.DataShape"
            })
    public InfoTable GetRepositorySigning() {
        try {
            GitUserContextManager userContext = new GitUserContextManager();
            userContext.requireUser();
            ValueCollection stored = userContext.credentialsFor(resolveTargetThing().getName());
            String gpg = storedText(stored, Const.GpgKeyFingerprint);
            String ssh = storedText(stored, Const.SigningSshKeyFingerprint);
            SigningSelection.Choice choice =
                    SigningSelection.saved(storedText(stored, Const.SigningFormat), gpg, ssh);
            InfoTable table =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            "GIT.RepositorySigning.DataShape");
            ValueCollection row = new ValueCollection();
            row.put(Const.SigningFormat, new StringPrimitive(choice.format()));
            row.put(Const.KeyFingerprint, new StringPrimitive(choice.fingerprint()));
            row.put(Const.GpgKeyFingerprint, new StringPrimitive(gpg));
            row.put(Const.SigningSshKeyFingerprint, new StringPrimitive(ssh));
            row.put(Const.RequireSigning, new BooleanPrimitive(requireSigning()));
            row.put(
                    "CanChangeRequireSigning",
                    new BooleanPrimitive(
                            hasRequireSigningProperty()
                                    && Boolean.TRUE.equals(
                                            resolveTargetThing()
                                                    .CheckDesignTimePermission("Update"))));
            table.addRow(row);
            return DapResults.success(
                    "GetRepositorySigning",
                    RepositorySigningTable.SERVICE_RESULT,
                    RepositorySigningTable.wrap(table));
        } catch (Exception e) {
            return DapResults.failure(
                    "GetRepositorySigning", e, RepositorySigningTable.SERVICE_RESULT);
        }
    }

    /**
     * Lists the keys the current user can sign commits with in this repository: None, then the
     * user's GPG and SSH keys. The saved key comes first, so a dropdown that selects its first row
     * starts on the repository's default. None is left out when the repository requires signing.
     */
    @DapServicePayload(SigningKeyOptionSpec.class)
    @ThingworxServiceDefinition(
            name = "GetSigningKeyOptions",
            description =
                    "Lists the current user's GPG and SSH keys that can sign commits in this repository, the saved key first",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.SigningKeyOption.ServiceResult.DataShape"
            })
    public InfoTable GetSigningKeyOptions() {
        try {
            String repository = resolveTargetThing().getName();
            GitUserContextManager userContext = new GitUserContextManager();
            userContext.requireUser();
            ValueCollection stored = userContext.credentialsFor(repository);
            String saved =
                    SigningSelection.key(
                            SigningSelection.saved(
                                    storedText(stored, Const.SigningFormat),
                                    storedText(stored, Const.GpgKeyFingerprint),
                                    storedText(stored, Const.SigningSshKeyFingerprint)));
            java.util.Set<String> signIn = new java.util.HashSet<>();
            signIn.add(storedText(stored, Const.SshKeyFingerprint));
            InfoTable remotes = userContext.remoteCredentials();
            if (remotes != null)
                for (int i = 0; i < remotes.getRowCount(); i++)
                    if (repository.equals(remotes.getRow(i).getStringValue("GitThing")))
                        signIn.add(storedText(remotes.getRow(i), Const.SshKeyFingerprint));
            signIn.remove("");

            java.util.List<ValueCollection> rows = new ArrayList<>();
            if (!requireSigning())
                rows.add(signingOption(SigningSelection.NONE, "", "", "None (not signed)", false));
            InfoTable gpg = userContext.gpgKeys();
            if (gpg != null)
                for (int i = 0; i < gpg.getRowCount(); i++) {
                    ValueCollection key = gpg.getRow(i);
                    rows.add(
                            signingOption(
                                    SigningSelection.OPENPGP,
                                    storedText(key, Const.GpgKeyFingerprint),
                                    storedText(key, Const.GpgKeyLabel),
                                    "GPG",
                                    false));
                }
            InfoTable ssh = userContext.sshKeys();
            if (ssh != null)
                for (int i = 0; i < ssh.getRowCount(); i++) {
                    ValueCollection key = ssh.getRow(i);
                    String fingerprint = storedText(key, Const.SshKeyFingerprint);
                    rows.add(
                            signingOption(
                                    SigningSelection.SSH,
                                    fingerprint,
                                    storedText(key, Const.SshKeyLabel),
                                    "SSH",
                                    signIn.contains(fingerprint)));
                }
            InfoTable table =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            "GIT.SigningKeyOption.DataShape");
            String defaultValue = saved.isEmpty() ? SigningSelection.NONE : saved;
            rows.stream()
                    .filter(row -> defaultValue.equals(row.getStringValue("Value")))
                    .findFirst()
                    .ifPresent(
                            row -> {
                                row.put("IsRepositoryDefault", new BooleanPrimitive(true));
                                row.put(
                                        "Display",
                                        new StringPrimitive(
                                                row.getStringValue("Display")
                                                        + " (repository default)"));
                                table.addRow(row);
                            });
            for (ValueCollection row : rows)
                if (!defaultValue.equals(row.getStringValue("Value"))) table.addRow(row);
            return DapResults.success(
                    "GetSigningKeyOptions",
                    SigningKeyOptionTable.SERVICE_RESULT,
                    SigningKeyOptionTable.wrap(table));
        } catch (Exception e) {
            return DapResults.failure(
                    "GetSigningKeyOptions", e, SigningKeyOptionTable.SERVICE_RESULT);
        }
    }

    private static ValueCollection signingOption(
            String format, String fingerprint, String label, String kind, boolean signIn) {
        ValueCollection row = new ValueCollection();
        boolean none = format.equals(SigningSelection.NONE);
        row.put("Value", new StringPrimitive(none ? format : format + ":" + fingerprint));
        String display =
                none
                        ? kind
                        : kind
                                + "  "
                                + (label.isBlank() ? "" : label + "  ")
                                + fingerprint
                                + (signIn ? "  (also sign-in)" : "");
        row.put("Display", new StringPrimitive(display));
        row.put("Format", new StringPrimitive(none ? "" : format));
        row.put("Fingerprint", new StringPrimitive(fingerprint));
        row.put("IsRepositoryDefault", new BooleanPrimitive(false));
        row.put("IsSignInKey", new BooleanPrimitive(signIn));
        return row;
    }

    /**
     * Turns RequireSigning on or off for this repository. It applies to every user, so it needs
     * design-time Update permission on the repository Thing.
     */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "SetRequireSigning",
            description =
                    "Turns RequireSigning on or off for every user of this repository. Needs design-time Update permission on the repository Thing.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable SetRequireSigning(
            @ThingworxServiceParameter(
                            name = "RequireSigning",
                            description = "true to refuse unsigned commits",
                            baseType = "BOOLEAN")
                    Boolean RequireSigning) {
        try {
            Thing thing = resolveTargetThing();
            if (!Boolean.TRUE.equals(thing.CheckDesignTimePermission("Update")))
                throw new SecurityException(
                        "Update permission denied for Thing " + thing.getName());
            if (!hasRequireSigningProperty())
                throw new IllegalStateException(
                        Const.ERR_PREFIX_CONFIG
                                + "This repository Thing has no RequireSigning property yet."
                                + " Restart the extension's Utility Thing to finish the upgrade.");
            boolean value = Boolean.TRUE.equals(RequireSigning);
            thing.setPropertyValue(Const.RequireSigning, new BooleanPrimitive(value));
            String message =
                    value
                            ? "This repository now requires signed commits."
                            : "This repository no longer requires signed commits.";
            GitActionLogger.logAction("SetRequireSigning", thing.getName(), message);
            return DapResults.success(
                    "SetRequireSigning", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("SetRequireSigning", e, StringResultContract.SERVICE_RESULT);
        }
    }

    private boolean hasRequireSigningProperty() {
        try {
            return resolveTargetThing().getEffectivePropertyDefinition(Const.RequireSigning)
                    != null;
        } catch (Exception e) {
            return false;
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
     * Fetches and integrates changes from the configured remote. A force pull resets the local
     * branch to the fetched remote state. ThingWorx entities are not changed; use {@link
     * #SyncToThingWorx} to apply the result.
     *
     * @param Remote remote name; blank selects the branch upstream remote or {@code origin}
     * @param Force when true, discard local divergence and reset to the fetched remote state
     * @return a pull summary, or a prefixed error message; conflicts are reported rather than
     *     silently discarded
     */
    public InfoTable Pull(
            @ThingworxServiceParameter(
                            name = "Remote",
                            description =
                                    "Optional remote name; blank uses the branch upstream remote, or origin",
                            baseType = "STRING")
                    String Remote,
            @ThingworxServiceParameter(
                            name = "Force",
                            description = "Forces a hard reset instead of a normal pull",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean Force) {
        String CurrentMethodName = "Pull";
        FileRepositoryManager fileRepo = null;
        GitTransport transport = null;
        try {
            transport = GitTransport.forRepository(resolveTargetThing().getName());
            final GitTransport pullTransport = transport;
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git myGitFolder = fileRepo.git();
            String LogResult;
            if (Boolean.TRUE.equals(Force)) {
                String target = ForcePull.resetToRemote(myGitFolder, transport, Remote);
                LogResult = "Force pull reset to " + target + ".";
            } else {
                Repository pullRepository = myGitFolder.getRepository();
                String pullBranch = pullRepository.getBranch();
                String pullRemote = Remotes.resolve(pullRepository, Remote, pullBranch);
                // The upstream branch name only applies to the remote the branch tracks. For
                // any other remote, pull the branch of the same name.
                boolean tracksPullRemote =
                        pullRemote.equals(
                                pullRepository
                                        .getConfig()
                                        .getString("branch", pullBranch, "remote"));
                PullResult[] pulled = new PullResult[1];
                ConflictWorkflow.Outcome outcome;
                try (CommitScope ignored = commitScope(pullRepository)) {
                    outcome =
                            ConflictWorkflow.run(
                                    myGitFolder,
                                    "Pull",
                                    () -> {
                                        var pull =
                                                pullTransport
                                                        .configure(myGitFolder.pull(), pullRemote)
                                                        .setRemote(pullRemote);
                                        if (!tracksPullRemote) pull.setRemoteBranchName(pullBranch);
                                        pulled[0] = pull.call();
                                        return ConflictWorkflow.outcome(pulled[0]);
                                    });
                }
                if (outcome == ConflictWorkflow.Outcome.CONFLICTS) {
                    return conflictResult(CurrentMethodName, "Pull", myGitFolder);
                }
                String upstreamMerge =
                        pullRepository.getConfig().getString("branch", pullBranch, "merge");
                String pulledBranch =
                        tracksPullRemote && upstreamMerge != null ? upstreamMerge : pullBranch;
                LogResult = ResultMessages.pull(pulled[0], pullRemote, pulledBranch, pullBranch);
            }

            GitActionLogger.logAction("Pull", resolveTargetThing().getName(), LogResult);
            return DapResults.success(
                    CurrentMethodName, LogResult, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return transportFailure(CurrentMethodName, transport, e);
        } finally {
            if (transport != null) transport.close();
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
                    "Fetches refs and objects from a remote without merging, checking out, resetting, or synchronizing entities. A blank remote uses the current branch's upstream remote, or origin.",
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
     * @param Remote remote name; blank selects the branch upstream remote or {@code origin}
     * @return a fetch summary or a prefixed error message
     */
    public InfoTable Fetch(
            @ThingworxServiceParameter(
                            name = "Remote",
                            description =
                                    "Optional remote name; blank uses the branch upstream remote, or origin",
                            baseType = "STRING")
                    String Remote) {
        String CurrentMethodName = "Fetch";
        GitTransport transport = null;
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());

            Git myGitFolder = fileRepo.git();
            transport = GitTransport.forRepository(resolveTargetThing().getName());
            Repository fetchRepository = myGitFolder.getRepository();
            String remote = Remotes.resolve(fetchRepository, Remote, fetchRepository.getBranch());
            transport.configure(myGitFolder.fetch(), remote).setRemote(remote).call();
            String LogResult = "Fetch from '" + remote + "' completed successfully.";

            GitActionLogger.logAction("Fetch", resolveTargetThing().getName(), LogResult);
            return DapResults.success(
                    CurrentMethodName, LogResult, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return transportFailure(CurrentMethodName, transport, e);
        } finally {
            if (transport != null) transport.close();
        }
    }

    @DapServicePayload(RemoteSpec.class)
    @ThingworxServiceDefinition(
            name = "GetRemotes",
            description = "Lists the remotes configured in this repository's .git/config",
            category = "Remotes",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.Remote.ServiceResult.DataShape"})
    /**
     * Lists remotes as {@code git remote -v} does. URLs are shown without any embedded password.
     *
     * @return a {@code GIT.Remote.ServiceResult.DataShape} table
     */
    public InfoTable GetRemotes() {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            RemoteTable remotes = RemoteTable.create();
            for (RemoteConfig remote : Remotes.list(fileRepo.git())) {
                StringBuilder specs = new StringBuilder();
                for (RefSpec spec : remote.getFetchRefSpecs()) {
                    if (specs.length() > 0) specs.append('\n');
                    specs.append(spec);
                }
                remotes.add(
                        RemoteRow.create()
                                .name(remote.getName())
                                .urls(Remotes.describe(remote.getURIs()))
                                .pushUrls(Remotes.describe(remote.getPushURIs()))
                                .fetchSpecs(specs.toString()));
            }
            return DapResults.success("GetRemotes", RemoteTable.SERVICE_RESULT, remotes);
        } catch (Exception e) {
            return DapResults.failure("GetRemotes", e, RemoteTable.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(RemoteStatusSpec.class)
    @ThingworxServiceDefinition(
            name = "GetConnectionStatus",
            description =
                    "Reports, for each remote, which setup steps the current user still needs before pulling and pushing: an allowed server address, saved sign-in, an approved credential origin and an approved SSH host key. Nothing is contacted except DNS.",
            category = "Remotes",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.RemoteStatus.ServiceResult.DataShape"
            })
    /**
     * Lists each remote with the setup steps it still needs for the calling user.
     *
     * @return a {@code GIT.RemoteStatus.ServiceResult.DataShape} table
     */
    public InfoTable GetConnectionStatus() {
        FileRepositoryManager fileRepo = null;
        try {
            Thing thing = resolveTargetThing();
            fileRepo = new FileRepositoryManager(thing);
            Git git = fileRepo.git();
            Repository repository = git.getRepository();
            String defaultRemote;
            try {
                defaultRemote = Remotes.resolve(repository, null, repository.getBranch());
            } catch (IllegalArgumentException e) {
                defaultRemote = "";
            }
            GitUserContextManager context = new GitUserContextManager();
            boolean allowInsecure = NetworkPolicy.allowInsecureCredentials();

            RemoteStatusTable result = RemoteStatusTable.create();
            for (RemoteConfig remote : Remotes.list(git)) {
                ValueCollection credentials =
                        context.credentialsFor(thing.getName(), remote.getName());
                String username = storedText(credentials, Const.GitCommitterUser);
                String password = storedText(credentials, Const.GitCommitterPassword);
                String sshFingerprint = storedText(credentials, Const.SshKeyFingerprint);
                boolean sshKeyAvailable = context.sshKey(sshFingerprint) != null;
                String approvedOrigins = storedText(credentials, Const.ApprovedRemoteOrigins);
                List<URIish> uris = new ArrayList<>(remote.getURIs());
                for (URIish push : remote.getPushURIs()) if (!uris.contains(push)) uris.add(push);
                List<URIish> approvedHostKeys = new ArrayList<>();
                List<String> networkProblems = new ArrayList<>();
                for (URIish uri : uris) {
                    if ("ssh".equals(ConnectionStatus.transport(uri))
                            && SshHostKeys.isApproved(repository, uri)) {
                        approvedHostKeys.add(uri);
                    }
                    try {
                        NetworkPolicy.requireAllowed(uri);
                    } catch (Exception e) {
                        networkProblems.add(e.getMessage());
                    }
                }
                ConnectionStatus.Status status =
                        ConnectionStatus.evaluate(
                                new ConnectionStatus.Inputs(
                                        remote.getName(),
                                        uris,
                                        remote.getName().equals(defaultRemote),
                                        username,
                                        password != null && !password.isEmpty(),
                                        approvedOrigins,
                                        sshFingerprint,
                                        sshKeyAvailable,
                                        approvedHostKeys,
                                        networkProblems,
                                        allowInsecure));
                result.add(
                        RemoteStatusRow.create()
                                .remote(status.remote())
                                .url(status.url())
                                .transport(status.transport())
                                .isDefault(status.isDefault())
                                .network(status.network())
                                .signIn(status.signIn())
                                .credentialOrigin(status.credentialOrigin())
                                .hostKey(status.hostKey())
                                .ready(status.ready())
                                .nextStep(status.nextStep()));
            }
            return DapResults.success(
                    "GetConnectionStatus", RemoteStatusTable.SERVICE_RESULT, result);
        } catch (Exception e) {
            return DapResults.failure("GetConnectionStatus", e, RemoteStatusTable.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    /** Reads a stored settings field; a user without settings for this repository has none. */
    private static String storedText(ValueCollection row, String field) {
        try {
            String value = row.getStringValue(field);
            return value == null ? "" : value;
        } catch (Exception e) {
            return "";
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "RemoteAdd",
            description =
                    "Adds a remote to this repository's .git/config. Only https://, http:// and ssh:// URLs without an embedded password are accepted.",
            category = "Remotes",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable RemoteAdd(
            @ThingworxServiceParameter(
                            name = "Name",
                            description = "Remote name, such as origin",
                            baseType = "STRING")
                    String Name,
            @ThingworxServiceParameter(
                            name = "URL",
                            description = "https://, http:// or ssh:// repository URL",
                            baseType = "STRING")
                    String URL) {
        return changeRemotes(
                "RemoteAdd",
                git -> {
                    String name = Name == null ? "" : Name.trim();
                    Remotes.add(git, name, URL);
                    return "Remote '" + name + "' added.";
                });
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "RemoteSetUrl",
            description =
                    "Changes the URL of a remote in this repository's .git/config. With Push true, sets the separate push URL instead; a blank URL with Push true removes the push URL.",
            category = "Remotes",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable RemoteSetUrl(
            @ThingworxServiceParameter(
                            name = "Name",
                            description = "Existing remote name",
                            baseType = "STRING")
                    String Name,
            @ThingworxServiceParameter(
                            name = "URL",
                            description = "https://, http:// or ssh:// repository URL",
                            baseType = "STRING")
                    String URL,
            @ThingworxServiceParameter(
                            name = "Push",
                            description = "Set the push URL instead of the fetch URL",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean Push) {
        return changeRemotes(
                "RemoteSetUrl",
                git -> {
                    String name = Name == null ? "" : Name.trim();
                    if (!Remotes.names(git.getRepository()).contains(name)) {
                        throw new IllegalArgumentException("Remote '" + name + "' does not exist.");
                    }
                    boolean push = Boolean.TRUE.equals(Push);
                    if (push && (URL == null || URL.isBlank())) {
                        return Remotes.clearPushUrls(git, name)
                                ? "Remote '"
                                        + name
                                        + "' push URL removed. Pushes use the fetch URL."
                                : "Remote '" + name + "' has no separate push URL.";
                    }
                    git.remoteSetUrl()
                            .setRemoteName(name)
                            .setRemoteUri(Remotes.parseUrl(URL))
                            .setUriType(
                                    push
                                            ? RemoteSetUrlCommand.UriType.PUSH
                                            : RemoteSetUrlCommand.UriType.FETCH)
                            .call();
                    return "Remote '" + name + "' " + (push ? "push " : "") + "URL updated.";
                });
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "RemoteRemove",
            description =
                    "Removes a remote from this repository's .git/config, with its remote-tracking branches and the upstream settings of branches that tracked it",
            category = "Remotes",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable RemoteRemove(
            @ThingworxServiceParameter(
                            name = "Name",
                            description = "Existing remote name",
                            baseType = "STRING")
                    String Name) {
        return changeRemotes(
                "RemoteRemove",
                git -> {
                    String name = Name == null ? "" : Name.trim();
                    Remotes.remove(git, name);
                    return "Remote '" + name + "' removed.";
                });
    }

    /**
     * Keeps the current user's sign-in bound to the remotes as they are now: each remote's own
     * sign-in to that remote's origins, and the repository-wide sign-in to every origin. Sign-in
     * saved for a remote that no longer exists is dropped, so a new remote with the same name
     * starts without it.
     */
    private static void bindApprovedOrigins(String repository, Git git) {
        try {
            GitUserContextManager context = new GitUserContextManager();
            Repository repo = git.getRepository();
            context.setApprovedOriginsIfConfigured(repository, Remotes.approvedOrigins(repo));
            Set<String> names = Remotes.names(repo);
            for (String name : names) {
                context.setRemoteApprovedOriginsIfConfigured(
                        repository, name, Remotes.approvedOrigins(repo, name));
            }
            InfoTable saved = context.remoteCredentials();
            if (saved == null) return;
            Set<String> gone = new HashSet<>();
            for (int i = 0; i < saved.getRowCount(); i++) {
                ValueCollection row = saved.getRow(i);
                String remote = row.getStringValue(Const.RemoteName);
                if (repository.equals(row.getStringValue("GitThing")) && !names.contains(remote)) {
                    gone.add(remote);
                }
            }
            for (String remote : gone) context.removeRemoteCredentials(repository, remote);
        } catch (Exception ignored) {
            // A user without stored credentials has nothing to bind.
        }
    }

    @DapServicePayload(RemoteSignInSpec.class)
    @ThingworxServiceDefinition(
            name = "RemoteSignInList",
            description =
                    "Lists each remote with the current user's sign-in for it: username, whether a password is saved and the SSH key. Passwords are never returned.",
            category = "Remotes",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.RemoteSignIn.ServiceResult.DataShape"
            })
    public InfoTable RemoteSignInList() {
        FileRepositoryManager fileRepo = null;
        try {
            String repository = resolveTargetThing().getName();
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            GitUserContextManager context = new GitUserContextManager();
            RemoteSignInTable result = RemoteSignInTable.create();
            for (RemoteConfig remote : Remotes.list(fileRepo.git())) {
                ValueCollection own = context.remoteCredentialsRow(repository, remote.getName());
                ValueCollection row = own != null ? own : context.credentialsFor(repository);
                result.add(
                        RemoteSignInRow.create()
                                .remote(remote.getName())
                                .gitCommitterUser(storedText(row, Const.GitCommitterUser))
                                .hasPassword(!storedText(row, Const.GitCommitterPassword).isEmpty())
                                .sshKeyFingerprint(storedText(row, Const.SshKeyFingerprint))
                                .inherited(own == null));
            }
            return DapResults.success("RemoteSignInList", RemoteSignInTable.SERVICE_RESULT, result);
        } catch (Exception e) {
            return DapResults.failure("RemoteSignInList", e, RemoteSignInTable.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "RemoteSignInSave",
            description =
                    "Saves the current user's sign-in for one remote: a username and password or token for http(s) URLs and an SSH key for ssh:// URLs. A blank password keeps the saved one. The password is only sent to the remote's URLs as they are now.",
            category = "Remotes",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable RemoteSignInSave(
            @ThingworxServiceParameter(
                            name = "Remote",
                            description = "Remote name",
                            baseType = "STRING")
                    String Remote,
            @ThingworxServiceParameter(
                            name = "GitCommitterUser",
                            description = "Git username",
                            baseType = "STRING")
                    String GitCommitterUser,
            @ThingworxServiceParameter(
                            name = "GitCommitterPassword",
                            description = "New password or token, or blank to keep the saved one",
                            baseType = "STRING")
                    String GitCommitterPassword,
            @ThingworxServiceParameter(
                            name = "SshKeyFingerprint",
                            description = "One of your SSH keys, or blank for none",
                            baseType = "STRING")
                    String SshKeyFingerprint) {
        FileRepositoryManager fileRepo = null;
        String repository = resolveTargetThing().getName();
        try {
            String name = Remote == null ? "" : Remote.trim();
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            Repository repo = fileRepo.git().getRepository();
            if (!Remotes.names(repo).contains(name)) {
                throw new IllegalArgumentException("Remote '" + name + "' does not exist.");
            }
            GitUserContextManager context = new GitUserContextManager();
            String fingerprint = SshKeyFingerprint == null ? "" : SshKeyFingerprint.trim();
            if (!fingerprint.isEmpty() && context.sshKey(fingerprint) == null) {
                throw new IllegalArgumentException(
                        "SSH key is not owned by the current user: " + fingerprint);
            }
            // A username and password are only kept for remotes with an http(s) URL.
            boolean http = false;
            RemoteConfig config = new RemoteConfig(repo.getConfig(), name);
            for (URIish uri : config.getURIs())
                http |= !"ssh".equals(ConnectionStatus.transport(uri));
            for (URIish uri : config.getPushURIs()) {
                http |= !"ssh".equals(ConnectionStatus.transport(uri));
            }
            String password =
                    !http
                            ? ""
                            : GitCommitterPassword == null || GitCommitterPassword.isEmpty()
                                    ? null
                                    : GitCommitterPassword;
            context.saveRemoteCredentials(
                    repository,
                    name,
                    http ? GitCommitterUser : "",
                    password,
                    fingerprint,
                    Remotes.approvedOrigins(repo, name));
            String message = "Sign-in for '" + name + "' saved.";
            GitActionLogger.logAction("RemoteSignInSave", repository, message);
            return DapResults.success(
                    "RemoteSignInSave", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("RemoteSignInSave", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "RemoteSignInClear",
            description =
                    "Deletes the current user's own sign-in for one remote. The remote then uses the repository-wide sign-in saved by earlier versions, if there is one.",
            category = "Remotes",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable RemoteSignInClear(
            @ThingworxServiceParameter(
                            name = "Remote",
                            description = "Remote name",
                            baseType = "STRING")
                    String Remote) {
        String repository = resolveTargetThing().getName();
        try {
            String name = Remote == null ? "" : Remote.trim();
            boolean removed = new GitUserContextManager().removeRemoteCredentials(repository, name);
            String message =
                    removed
                            ? "Sign-in for '" + name + "' deleted."
                            : "'" + name + "' has no sign-in of its own.";
            GitActionLogger.logAction("RemoteSignInClear", repository, message);
            return DapResults.success(
                    "RemoteSignInClear", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("RemoteSignInClear", e, StringResultContract.SERVICE_RESULT);
        }
    }

    private interface RemoteChange {
        String apply(Git git) throws Exception;
    }

    private InfoTable changeRemotes(String service, RemoteChange change) {
        FileRepositoryManager fileRepo = null;
        String repository = resolveTargetThing().getName();
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            String message = change.apply(fileRepo.git());
            bindApprovedOrigins(repository, fileRepo.git());
            GitActionLogger.logAction(service, repository, message);
            return DapResults.success(service, message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(service, repository, String.valueOf(e.getMessage()));
            return DapResults.failure(service, e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "TestRemote",
            description =
                    "Connects to a configured remote with the current user's credentials and reports how many branches and tags it offers. Nothing is fetched.",
            category = "Remotes",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable TestRemote(
            @ThingworxServiceParameter(
                            name = "Remote",
                            description =
                                    "Optional remote name; blank uses the branch upstream remote, or origin",
                            baseType = "STRING")
                    String Remote) {
        FileRepositoryManager fileRepo = null;
        GitTransport transport = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git git = fileRepo.git();
            Repository repository = git.getRepository();
            String remote = Remotes.resolve(repository, Remote, repository.getBranch());
            transport = GitTransport.forRepository(resolveTargetThing().getName());
            java.util.Collection<Ref> refs =
                    transport
                            .configure(git.lsRemote(), remote)
                            .setRemote(remote)
                            .setHeads(true)
                            .setTags(true)
                            .call();
            long branches =
                    refs.stream().filter(r -> r.getName().startsWith(Constants.R_HEADS)).count();
            String message =
                    "Connected to '"
                            + remote
                            + "': "
                            + branches
                            + " branch(es), "
                            + (refs.size() - branches)
                            + " tag(s).";
            GitActionLogger.logAction("TestRemote", resolveTargetThing().getName(), message);
            return DapResults.success("TestRemote", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return transportFailure("TestRemote", transport, e);
        } finally {
            if (transport != null) transport.close();
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "ResetSshHostKeys",
            description =
                    "Forgets the SSH host keys trusted for this repository; the next SSH connection trusts the server's current key",
            category = "SSH",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable ResetSshHostKeys() {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            boolean removed = GitTransport.resetKnownHosts(fileRepo.git().getRepository());
            String message =
                    removed
                            ? "Trusted SSH host keys were reset. The next SSH connection will trust the server's current key."
                            : "No SSH host keys were trusted for this repository.";
            GitActionLogger.logAction("ResetSshHostKeys", resolveTargetThing().getName(), message);
            return DapResults.success(
                    "ResetSshHostKeys", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("ResetSshHostKeys", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(SshHostKeySpec.class)
    @ThingworxServiceDefinition(
            name = "SshHostKeyProbe",
            description =
                    "Connects to an SSH remote only far enough to read and display its host key fingerprint. Nothing is trusted until SshHostKeyApprove is called.",
            category = "SSH",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.SshHostKey.ServiceResult.DataShape"
            })
    public InfoTable SshHostKeyProbe(
            @ThingworxServiceParameter(
                            name = "Remote",
                            description =
                                    "Optional remote name; blank uses the branch upstream remote, or origin",
                            baseType = "STRING")
                    String Remote) {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git git = fileRepo.git();
            String remote =
                    Remotes.resolve(git.getRepository(), Remote, git.getRepository().getBranch());
            URIish uri = sshRemoteUri(git, remote);
            NetworkPolicy.requireAllowed(uri);
            SshHostKeys.HostKey key = SshHostKeys.probe(uri);
            SshHostKeyTable result = SshHostKeyTable.create();
            result.add(
                    SshHostKeyRow.create()
                            .remote(remote)
                            .host(key.host())
                            .port(key.port())
                            .fingerprint(key.fingerprint())
                            .publicKey(key.publicKey()));
            GitActionLogger.logAction(
                    "SshHostKeyProbe",
                    resolveTargetThing().getName(),
                    "Probed SSH host key " + key.fingerprint());
            return DapResults.success("SshHostKeyProbe", SshHostKeyTable.SERVICE_RESULT, result);
        } catch (Exception e) {
            return DapResults.failure("SshHostKeyProbe", e, SshHostKeyTable.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "SshHostKeyApprove",
            description =
                    "Probes an SSH remote again, verifies the displayed SHA-256 fingerprint, and records the host key for this repository",
            category = "SSH",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable SshHostKeyApprove(
            @ThingworxServiceParameter(
                            name = "Remote",
                            description =
                                    "Optional remote name; blank uses the branch upstream remote, or origin",
                            baseType = "STRING")
                    String Remote,
            @ThingworxServiceParameter(
                            name = "Fingerprint",
                            description =
                                    "SHA-256 fingerprint shown by SshHostKeyProbe and verified out of band",
                            baseType = "STRING")
                    String Fingerprint) {
        FileRepositoryManager fileRepo = null;
        try {
            if (Fingerprint == null || Fingerprint.isBlank()) {
                throw new IllegalArgumentException("A host key fingerprint is required.");
            }
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git git = fileRepo.git();
            String remote =
                    Remotes.resolve(git.getRepository(), Remote, git.getRepository().getBranch());
            URIish uri = sshRemoteUri(git, remote);
            NetworkPolicy.requireAllowed(uri);
            SshHostKeys.approve(git.getRepository(), uri, Fingerprint);
            String message =
                    "Approved SSH host key " + Fingerprint.trim() + " for remote '" + remote + "'.";
            GitActionLogger.logAction("SshHostKeyApprove", resolveTargetThing().getName(), message);
            return DapResults.success(
                    "SshHostKeyApprove", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("SshHostKeyApprove", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    private static URIish sshRemoteUri(Git git, String remote) throws Exception {
        for (RemoteConfig config : Remotes.list(git)) {
            if (!remote.equals(config.getName())) continue;
            List<URIish> uris = config.getURIs();
            if (uris.isEmpty())
                throw new IllegalArgumentException("Remote '" + remote + "' has no URL.");
            URIish uri = uris.get(0);
            String scheme = uri.getScheme();
            if (!(scheme == null || "ssh".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("Remote '" + remote + "' is not an SSH remote.");
            }
            return uri;
        }
        throw new IllegalArgumentException("Remote '" + remote + "' is not configured.");
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
            Git myGitFolder = fileRepo.git();
            StartPoint = (StartPoint == null || StartPoint.isBlank()) ? "HEAD" : StartPoint;
            Ref branchRef =
                    myGitFolder.branchCreate().setName(BranchName).setStartPoint(StartPoint).call();

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

    /**
     * Squashes consecutive commits on the current branch into one signed commit and rebuilds the
     * commits after them, also signed. File contents, the working tree and the index do not change.
     * Commits that were already pushed need a forced push afterwards.
     *
     * @param CommitIDs comma-separated IDs of at least two consecutive commits
     * @param Message message for the squashed commit; blank joins the selected messages
     * @param SigningKey optional key, as for {@code CommitStaged}; blank uses the repository's key
     */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "SquashCommits",
            description =
                    "Squashes consecutive commits on the current branch into one commit, signed with the repository's signing key. Later commits are rebuilt on top of it and signed too. Files are not changed. Already-pushed commits need a forced push afterwards.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable SquashCommits(
            @ThingworxServiceParameter(
                            name = "CommitIDs",
                            description =
                                    "Comma-separated IDs of two or more consecutive commits on the current branch",
                            baseType = "STRING")
                    String CommitIDs,
            @ThingworxServiceParameter(
                            name = "Message",
                            description =
                                    "Message for the squashed commit. Blank joins the selected commits' messages, oldest first.",
                            baseType = "STRING")
                    String Message,
            @ThingworxServiceParameter(
                            name = "SigningKey",
                            description =
                                    "Optional key, as GetSigningKeyOptions returns it: openpgp:<fingerprint>, ssh:<fingerprint>, or none for unsigned. Blank uses the repository's signing key.",
                            baseType = "STRING")
                    String SigningKey) {
        final String serviceName = "SquashCommits";
        log.trace("Entering Service: " + serviceName);
        FileRepositoryManager fileRepo = null;
        Signer signer = null;
        try {
            String repository = resolveTargetThing().getName();
            GitUserContextManager.Identity committer =
                    new GitUserContextManager().identityFor(repository);
            if (committer.name() == null
                    || committer.name().isBlank()
                    || committer.email() == null
                    || committer.email().isBlank()) {
                return DapResults.failure(
                        serviceName,
                        serviceName + " Error: " + Const.ERR_PREFIX_CONFIG + Const.ERR_NO_COMMITTER,
                        StringResultContract.SERVICE_RESULT);
            }
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git git = fileRepo.git();
            ConflictWorkflow.requireNotInProgress(git);

            SigningSelection.Choice override = SigningSelection.parseKey(SigningKey);
            signer = resolveSigner(override.format(), override.fingerprint());
            String message =
                    Message == null || Message.isBlank()
                            ? ""
                            : CommitAttribution.withAuthenticatedUser(Message);
            SquashCommits.Result result =
                    org.us_ignite.thingworx.jgit.extension.SquashCommits.squash(
                            git.getRepository(),
                            List.of(CommitIDs == null ? new String[0] : CommitIDs.split(",")),
                            message,
                            new PersonIdent(committer.name(), committer.email()),
                            signer);

            String text =
                    "Squashed "
                            + result.squashed()
                            + " commits into "
                            + result.squashedId().abbreviate(8).name()
                            + (signer != null ? " (signed)" : " (unsigned)")
                            + "."
                            + (result.rewritten() > 0
                                    ? " Rebuilt "
                                            + result.rewritten()
                                            + " later commit"
                                            + (result.rewritten() == 1 ? "" : "s")
                                            + (signer != null ? ", also signed." : ".")
                                    : "")
                            + (result.published()
                                    ? " Some of these commits were already pushed: tick Force push"
                                            + " on Changes when you push, or the remote will"
                                            + " reject it."
                                    : "");
            GitActionLogger.logAction(serviceName, repository, text);
            return DapResults.success(serviceName, text, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    serviceName, resolveTargetThing().getName(), String.valueOf(e.getMessage()));
            return DapResults.failure(serviceName, e, StringResultContract.SERVICE_RESULT);
        } finally {
            clearSigner(signer);
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "Checkout",
            description =
                    "Checks out a branch, remote branch, tag or commit. Uncommitted changes are carried over. A tag or commit leaves HEAD detached. ThingWorx is not changed; use SyncToThingWorx afterwards.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Checks out a local branch, commit, tag, or an available remote branch. Uncommitted changes
     * are carried across the checkout, stopping for resolution when they conflict, unless {@code
     * Force} is set. A commit or tag leaves the repository on a detached HEAD. ThingWorx is not
     * changed; {@code SyncToThingWorx} applies the result.
     *
     * @param BranchNameOrCommit branch, remote branch, tag, or commit to check out
     */
    public InfoTable Checkout(
            @ThingworxServiceParameter(
                            name = "BranchNameOrCommit",
                            description =
                                    "Branch, remote branch, tag or commit ID to check out. A missing local branch is created only when a remote branch of that name exists.",
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
                            String startPoint =
                                    Remotes.trackingStartPoint(
                                            myGitFolder.getRepository(), BranchNameOrCommit);
                            if (startPoint == null) throw ex;
                            log.warn("Checkout: creating tracking branch from " + startPoint);
                            checkedOut[0] =
                                    myGitFolder
                                            .checkout()
                                            .setCreateBranch(true)
                                            .setName(BranchNameOrCommit)
                                            .setUpstreamMode(SetupUpstreamMode.TRACK)
                                            .setStartPoint(startPoint)
                                            .call();
                        }
                        return ConflictWorkflow.Outcome.COMPLETED;
                    };
            if (runCheckout(myGitFolder, "Checkout", Force, checkoutOperation)
                    == ConflictWorkflow.Outcome.CONFLICTS) {
                return conflictResult(CurrentMethodName, "Checkout", myGitFolder);
            }
            Ref ref = checkedOut[0];
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
                    "Switches to an existing local branch or creates a tracking branch for a matching remote branch, preferring origin.",
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
                            String startPoint =
                                    Remotes.trackingStartPoint(git.getRepository(), BranchName);
                            if (startPoint == null) throw e;
                            switched[0] =
                                    git.checkout()
                                            .setCreateBranch(true)
                                            .setName(BranchName)
                                            .setUpstreamMode(SetupUpstreamMode.TRACK)
                                            .setStartPoint(startPoint)
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
            ObjectId at =
                    ref != null && ref.getObjectId() != null
                            ? ref.getObjectId()
                            : git.getRepository().resolve(Constants.HEAD);
            String message =
                    "Created and checked out " + BranchName + " at " + ResultMessages.shortId(at);
            log.trace("Exiting Service: BranchCreateAndCheckout");
            return DapResults.success(
                    "BranchCreateAndCheckout", message, StringResultContract.SERVICE_RESULT);
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
                                        .replace("refs/remotes/origin/", "")
                                        .replace("refs/remotes/", "");
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
        GitTransport transport = null;
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git myGitFolder = fileRepo.git();

            List<String> lstr =
                    myGitFolder
                            .branchDelete()
                            .setForce(Force.booleanValue())
                            .setBranchNames("refs/heads/" + BranchName)
                            .call();
            String LogResult =
                    lstr.isEmpty()
                            ? "Branch '" + BranchName + "' was not found locally."
                            : "Deleted " + BranchName + " locally.";

            if (DeleteRemote.booleanValue()) {
                transport = GitTransport.forRepository(resolveTargetThing().getName());
                Repository deleteRepository = myGitFolder.getRepository();
                String deleteRemote =
                        Remotes.resolve(deleteRepository, Remote, deleteRepository.getBranch());
                Iterable<PushResult> results =
                        transport
                                .configure(myGitFolder.push(), deleteRemote)
                                .setRemote(deleteRemote)
                                .setRefSpecs(new RefSpec(":refs/heads/" + BranchName))
                                .call();
                for (PushResult result : results) {
                    for (RemoteRefUpdate update : result.getRemoteUpdates()) {
                        if (update.getStatus() == RemoteRefUpdate.Status.NON_EXISTING) {
                            LogResult += " It was not on " + deleteRemote + ".";
                            continue;
                        }
                        if (!ResultMessages.accepted(update)) {
                            String error =
                                    LogResult
                                            + " "
                                            + ResultMessages.pushRejected(
                                                    result, update, deleteRemote);
                            GitActionLogger.logAction(
                                    "BranchDelete", resolveTargetThing().getName(), error);
                            return DapResults.failure(
                                    "BranchDelete",
                                    "BranchDelete Error: " + Const.ERR_PREFIX_GIT + error,
                                    StringResultContract.SERVICE_RESULT);
                        }
                        LogResult +=
                                update.getStatus() == RemoteRefUpdate.Status.UP_TO_DATE
                                                || result.getAdvertisedRef(update.getRemoteName())
                                                        == null
                                        ? " It was not on " + deleteRemote + "."
                                        : " Deleted it on " + deleteRemote + ".";
                    }
                }
            }

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
            return transportFailure("BranchDelete", transport, e);
        } finally {
            if (transport != null) transport.close();
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
            // An upgrade can keep the earlier data shape without the signature columns.
            addStatusField(result, Const.SignatureFormat, com.thingworx.types.BaseTypes.STRING, 9);
            addStatusField(result, Const.SignatureKey, com.thingworx.types.BaseTypes.STRING, 10);
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
        CommitSignatures.Description signature =
                CommitSignatures.describe(commit.getRawGpgSignature());
        row.put(Const.SignatureFormat, new StringPrimitive(signature.format()));
        row.put(Const.SignatureKey, new StringPrimitive(signature.key()));
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

    /**
     * Sync from ThingWorx: exports the tracked projects into the working tree so their changes show
     * in Status. Git operations never do this themselves. It succeeds partially: what cannot be
     * exported keeps its files and is returned as rows, and the rest is still exported.
     */
    @DapServicePayload(ThingworxSyncActionSpec.class)
    @ThingworxServiceDefinition(
            name = "SyncFromThingWorx",
            description =
                    "Exports the tracked ThingWorx projects into the working tree, so their changes show in Status. Does not stage or commit. A project, collection or Data Table that cannot be exported keeps its files and is returned as a row; the rest is still exported.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.ThingworxSyncAction.ServiceResult.DataShape"
            })
    public InfoTable SyncFromThingWorx() {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            FileRepositoryManager.ExportResult result = fileRepo.syncFromThingWorx();
            String message = exportMessage(fileRepo.trackedProjectsLabel(), result);
            GitActionLogger.logAction("SyncFromThingWorx", resolveTargetThing().getName(), message);
            if (!result.failures().isEmpty() && !result.exported()) {
                return DapResults.failure(
                        "SyncFromThingWorx", message, ThingworxSyncActionTable.SERVICE_RESULT);
            }
            return DapResults.success(
                    "SyncFromThingWorx",
                    message,
                    ThingworxSyncActionTable.SERVICE_RESULT,
                    ThingworxSyncActionTable.wrap(result.failures().table()));
        } catch (Exception e) {
            return DapResults.failure(
                    "SyncFromThingWorx", e, ThingworxSyncActionTable.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    /** The message of a Sync from ThingWorx, naming what failed. */
    static String exportMessage(String projects, FileRepositoryManager.ExportResult result) {
        SyncFailures failures = result.failures();
        if (failures.isEmpty()) return "Exported " + projects + " from ThingWorx.";
        String message =
                result.exported()
                        ? "Partially exported " + projects + " from ThingWorx."
                        : "Nothing was exported from ThingWorx.";
        return message
                + failures.note("not exported")
                + " Their files were left as they were. The last sync time was not updated, so"
                + " Sync to ThingWorx still warns before overwriting ThingWorx changes.";
    }

    /**
     * Describes the ThingWorx changes a Sync to ThingWorx would overwrite: entities in the tracked
     * projects modified since the last sync. Blank when there are none.
     */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "GetThingWorxChangesSinceSync",
            description =
                    "Describes the tracked-project entities changed in ThingWorx since the last sync, which Sync to ThingWorx would overwrite. Blank when there are none.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    public InfoTable GetThingWorxChangesSinceSync() {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            return DapResults.success(
                    "GetThingWorxChangesSinceSync",
                    OverwriteWarning.forChanges(fileRepo.changedSinceSync()),
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure(
                    "GetThingWorxChangesSinceSync", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    /** How many untracked files a Sync to ThingWorx result names before summarizing. */
    private static final int UNTRACKED_SHOWN = 10;

    /**
     * Names the Users created disabled, or returns blank. Users that could not be imported are
     * reported with every other failure of the sync.
     */
    static String usersNote(List<String> created) {
        if (created.isEmpty()) return "";
        return " "
                + (created.size() == 1 ? "User " : "Users ")
                + String.join(", ", created)
                + (created.size() == 1 ? " was" : " were")
                + " created disabled with a random password; set a password and"
                + " enable "
                + (created.size() == 1 ? "it." : "them.");
    }

    /** Names the untracked files a Sync to ThingWorx left out, or returns blank. */
    static String untrackedNote(List<String> untracked) {
        if (untracked.isEmpty()) return "";
        List<String> shown = untracked.subList(0, Math.min(untracked.size(), UNTRACKED_SHOWN));
        String names = String.join(", ", shown);
        if (untracked.size() > shown.size()) {
            names += " and " + (untracked.size() - shown.size()) + " more";
        }
        return " "
                + untracked.size()
                + (untracked.size() == 1 ? " untracked file was" : " untracked files were")
                + " not imported: "
                + names
                + ". Add, commit and push a file to include it.";
    }

    /**
     * Sync to ThingWorx: imports the tracked project folders into ThingWorx. Git operations never
     * do this themselves. Unless forced, it refuses when ThingWorx entities changed since the last
     * sync, since the import would overwrite them. It succeeds partially: what cannot be imported
     * is returned as rows and keeps its ThingWorx copy, and the rest is still imported.
     */
    @DapServicePayload(ThingworxSyncActionSpec.class)
    @ThingworxServiceDefinition(
            name = "SyncToThingWorx",
            description =
                    "Imports the files tracked by Git in the tracked project folders into ThingWorx; untracked files are never imported. Unless Force is true, refuses when ThingWorx entities changed since the last sync, because the import would overwrite them. An entity, User, project or Data Table that cannot be imported is returned as a row and keeps its ThingWorx copy; the rest is still imported.",
            category = "",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.ThingworxSyncAction.ServiceResult.DataShape"
            })
    public InfoTable SyncToThingWorx(
            @ThingworxServiceParameter(
                            name = "Force",
                            description =
                                    "Import even when ThingWorx entities changed since the last sync, overwriting them",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean Force) {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            if (!Boolean.TRUE.equals(Force)) {
                String warning = OverwriteWarning.forChanges(fileRepo.changedSinceSync());
                if (!warning.isEmpty()) {
                    return DapResults.failure(
                            "SyncToThingWorx",
                            "SyncToThingWorx Error: " + Const.ERR_PREFIX_CONFIG + warning,
                            ThingworxSyncActionTable.SERVICE_RESULT);
                }
            }
            FileRepositoryManager.ImportResult result;
            try {
                result = fileRepo.importProjectEntities();
            } catch (Exception importEx) {
                String failure =
                        "Importing entities into ThingWorx failed for projects '"
                                + fileRepo.trackedProjectsLabel()
                                + "': "
                                + importEx.getMessage();
                log.error(failure, importEx);
                GitActionLogger.logAction(
                        "SyncToThingWorx", resolveTargetThing().getName(), failure);
                return DapResults.failure(
                        "SyncToThingWorx", failure, ThingworxSyncActionTable.SERVICE_RESULT);
            }
            String message = importMessage(result);
            GitActionLogger.logAction("SyncToThingWorx", resolveTargetThing().getName(), message);
            if (!result.failures().isEmpty() && !result.imported()) {
                return DapResults.failure(
                        "SyncToThingWorx", message, ThingworxSyncActionTable.SERVICE_RESULT);
            }
            return DapResults.success(
                    "SyncToThingWorx",
                    message,
                    ThingworxSyncActionTable.SERVICE_RESULT,
                    ThingworxSyncActionTable.wrap(result.failures().table()));
        } catch (Exception e) {
            return DapResults.failure(
                    "SyncToThingWorx", e, ThingworxSyncActionTable.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    /** The message of a Sync to ThingWorx, naming what was left out and what failed. */
    static String importMessage(FileRepositoryManager.ImportResult result) {
        SyncFailures failures = result.failures();
        String message;
        if (failures.isEmpty()) {
            message =
                    result.imported()
                            ? "Repository entities imported into ThingWorx."
                            : "No tracked XML entities found to import.";
        } else {
            message =
                    result.imported()
                            ? "Repository entities partially imported into ThingWorx."
                            : "Nothing was imported into ThingWorx.";
        }
        message += untrackedNote(result.untracked());
        message += usersNote(result.createdUsers());
        if (!result.dataTables().isEmpty()) {
            message += " Data Table rows imported: " + String.join(", ", result.dataTables()) + ".";
        }
        if (!result.dataTableNotes().isEmpty()) {
            message +=
                    " Data Tables left alone: " + String.join(", ", result.dataTableNotes()) + ".";
        }
        if (!failures.isEmpty()) {
            message +=
                    failures.note("not imported")
                            + " They keep their ThingWorx copy; fix them in Git and sync again.";
        }
        return message;
    }

    /** Reports staged and working-tree status. Reads Git only; it does not export ThingWorx. */
    @DapServicePayload(StatusSpec.class)
    @ThingworxServiceDefinition(
            name = "Status",
            description =
                    "Reports Git status. Reads Git only; use SyncFromThingWorx to export ThingWorx changes first.",
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
            description =
                    "Reads the working-tree copy of a file that is currently in conflict. Other paths are refused.",
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
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            java.nio.file.Path file = fileRepo.requireConflictFile(File, false);
            return DapResults.success(
                    "ReadConflictFile",
                    Files.readString(file, StandardCharsets.UTF_8),
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("ReadConflictFile", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "WriteConflictFile",
            description =
                    "Writes the working-tree copy of a file that is currently in conflict. Other paths are refused.",
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
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            java.nio.file.Path file = fileRepo.requireConflictFile(File, true);
            Files.createDirectories(file.getParent());
            Files.write(
                    file,
                    (Content == null ? "" : Content).getBytes(StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.LinkOption.NOFOLLOW_LINKS);
            return DapResults.success(
                    "WriteConflictFile",
                    "Wrote conflict file " + File,
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("WriteConflictFile", e, StringResultContract.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
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
                    "Completes a paused merge after conflicts are resolved and staged. Use SyncToThingWorx to apply the result.",
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
                    "Aborts the current merge and restores the previous state. ThingWorx entities are not changed.",
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
                    "Continues a paused rebase after conflicts are resolved and staged. Use SyncToThingWorx to apply the result.",
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
                    "Aborts the current rebase and restores the previous state. ThingWorx entities are not changed.",
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
                    "Completes the operation waiting on conflicts once every file is resolved: commits a merge, continues a rebase, or finishes re-applying uncommitted changes. Use SyncToThingWorx to apply the result.",
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
        Signer[] signer = new Signer[1];
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git git = fileRepo.git();
            String operation = ConflictWorkflow.state(git).operation();
            GitUserContextManager.Identity identity =
                    new GitUserContextManager().identityFor(resolveTargetThing().getName());
            String name = identity.name();
            String email = identity.email();
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
            ConflictWorkflow.Outcome outcome;
            // The scope covers the commits a continued rebase rewrites; a merge commit is
            // signed explicitly below.
            try (CommitScope ignored = commitScope(git.getRepository())) {
                outcome =
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
            }
            if (outcome == ConflictWorkflow.Outcome.CONFLICTS)
                return conflictResult("ConflictContinue", operation, git);
            String message =
                    operation + " completed. Use Sync to ThingWorx to apply it to ThingWorx.";
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
            clearSigner(signer[0]);
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
            ConflictWorkflow.Outcome outcome;
            try (CommitScope ignored = commitScope(git.getRepository())) {
                outcome = ConflictWorkflow.skip(git);
            }
            if (outcome == ConflictWorkflow.Outcome.CONFLICTS)
                return conflictResult("ConflictSkip", operation, git);
            String message =
                    operation + " completed. Use Sync to ThingWorx to apply it to ThingWorx.";
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

    @DapServicePayload(FileContentSpec.class)
    @ThingworxServiceDefinition(
            name = "GetReadme",
            description =
                    "Returns the README at the root of the commit HEAD points at (README.md"
                            + " preferred, name case ignored). The result has no rows when the"
                            + " repository has no commit or no README.",
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
    public InfoTable GetReadme() {
        log.trace("Entering Service: GetReadme");
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            InfoTable result =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.FileContentDataShapeName);
            var readme = RepositoryReadme.read(fileRepo.git().getRepository(), 1000000);
            if (readme.isPresent()) {
                ValueCollection row = new ValueCollection();
                row.put("Path", new StringPrimitive(readme.get().path()));
                row.put("Content", new StringPrimitive(readme.get().content()));
                DapValues.putLong(row, "Size", readme.get().size());
                row.put("Binary", new BooleanPrimitive(readme.get().binary()));
                row.put("Truncated", new BooleanPrimitive(readme.get().truncated()));
                result.addRow(row);
            }
            return DapResults.success(
                    "GetReadme", FileContentTable.SERVICE_RESULT, FileContentTable.wrap(result));
        } catch (Exception ex) {
            return DapResults.failure("GetReadme", ex, FileContentTable.SERVICE_RESULT);
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
     * Lists tracked projects with their configured {@code Path} (blank for the default {@code
     * <ProjectName>} folder at the repository root).
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
                        TrackedProjectTable.wrap(
                                FileRepositoryManager.trackedProjectTable(
                                        fileRepo.trackedProjectEntries())));
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
     * @param Path optional folder for the project's entities, relative to the repository root;
     *     blank means {@code <ProjectName>} at the repository root
     * @return a {@code GIT.StringResult.DataShape} status table
     */
    public InfoTable AddTrackedProject(
            @ThingworxServiceParameter(
                            name = "ProjectName",
                            description = "ThingWorx project to track",
                            baseType = "STRING")
                    String ProjectName,
            @ThingworxServiceParameter(
                            name = "Path",
                            description =
                                    "Optional folder for the project's entities, relative to the repository root; blank means <ProjectName> at the repository root",
                            baseType = "STRING")
                    String Path) {
        String repoName = "";
        try {
            String name = FileRepositoryManager.validateProjectFolderName(ProjectName);
            String path = FileRepositoryManager.validateProjectPath(Path);
            // A project the caller may not see or read is reported exactly like a missing one.
            var project = FileRepositoryManager.findAccessibleProject(name);
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
                    var projects = new ArrayList<>(fileRepo.trackedProjectEntries());
                    if (projects.stream().anyMatch(p -> p.name().equals(name))) {
                        throw new IllegalArgumentException(
                                "Project '" + name + "' is already tracked.");
                    }
                    projects.add(new FileRepositoryManager.TrackedProject(name, path));
                    fileRepo.setTrackedProjects(projects);
                }
            } finally {
                fileRepo.close();
            }
            String message =
                    "Project '"
                            + name
                            + "' is now tracked"
                            + (path.isEmpty() ? "." : " in folder '" + path + "'.");
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
     * DeleteFiles} is true, the project folder (its {@code Path}, or {@code <ProjectName>/} at the
     * root) is deleted from the working tree and the deletion is left unstaged for review in
     * Changes.
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
                    var projects = new ArrayList<>(fileRepo.trackedProjectEntries());
                    var removed = projects.stream().filter(p -> p.name().equals(name)).findFirst();
                    if (removed.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Project '" + name + "' is not tracked.");
                    }
                    projects.remove(removed.get());
                    if (Boolean.TRUE.equals(DeleteFiles)) {
                        deleted = fileRepo.deleteProjectExport(removed.get());
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

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "SetTrackedProjectPath",
            description =
                    "Changes the repository folder a tracked project's entities are exported to and imported from",
            category = "Projects",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Sets a tracked project's folder. Unless {@code MoveFiles} is false, the files in the old
     * folder are moved to the new one like {@code git mv}: tracked files are renamed in the index
     * with their staged content unchanged and untracked files are moved on disk. Nothing is moved
     * or changed when a destination file already exists, when one folder contains the other, or
     * while a conflict is in progress. The renames appear in Changes for review and commit.
     *
     * @param ProjectName a tracked project
     * @param Path folder relative to the repository root; blank means {@code <ProjectName>} at the
     *     repository root
     * @param MoveFiles whether to move the project's files; blank means true
     * @return a {@code GIT.StringResult.DataShape} status table
     */
    public InfoTable SetTrackedProjectPath(
            @ThingworxServiceParameter(
                            name = "ProjectName",
                            description = "Tracked ThingWorx project",
                            baseType = "STRING")
                    String ProjectName,
            @ThingworxServiceParameter(
                            name = "Path",
                            description =
                                    "Folder for the project's entities, relative to the repository root; blank means <ProjectName> at the repository root",
                            baseType = "STRING")
                    String Path,
            @ThingworxServiceParameter(
                            name = "MoveFiles",
                            description =
                                    "Move the project's files from the old folder to the new one, like git mv; blank means true",
                            baseType = "BOOLEAN")
                    Boolean MoveFiles) {
        String repoName = "";
        try {
            String name = FileRepositoryManager.validateProjectFolderName(ProjectName);
            String path = FileRepositoryManager.validateProjectPath(Path);
            boolean move = !Boolean.FALSE.equals(MoveFiles);
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            repoName = fileRepo.repositoryThingName();
            FileRepositoryManager.FolderChange change;
            try {
                synchronized (StatusExportReconciler.class) {
                    var projects = new ArrayList<>(fileRepo.trackedProjectEntries());
                    int index = -1;
                    for (int i = 0; i < projects.size(); i++) {
                        if (projects.get(i).name().equals(name)) index = i;
                    }
                    if (index < 0) {
                        throw new IllegalArgumentException(
                                "Project '" + name + "' is not tracked.");
                    }
                    change = fileRepo.changeProjectFolder(projects, index, path, move);
                }
            } finally {
                fileRepo.close();
            }
            String message =
                    "Project '"
                            + name
                            + "' now uses folder '"
                            + change.to()
                            + "'. "
                            + (change.from().equals(change.to())
                                    ? "The folder is unchanged."
                                    : !move
                                            ? "Existing files in '"
                                                    + change.from()
                                                    + "' were not moved."
                                            : change.moved() == 0
                                                    ? "There were no files to move."
                                                    : change.moved()
                                                            + (change.moved() == 1
                                                                    ? " file was"
                                                                    : " files were")
                                                            + " moved from '"
                                                            + change.from()
                                                            + "'; review and commit the move in Changes.");
            GitActionLogger.logAction("SetTrackedProjectPath", repoName, message);
            return DapResults.success(
                    "SetTrackedProjectPath", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "SetTrackedProjectPath", repoName, String.valueOf(e.getMessage()));
            return DapResults.failure(
                    "SetTrackedProjectPath", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(EntityTypeOptionSpec.class)
    @ThingworxServiceDefinition(
            name = "GetEntityTypeOptions",
            description = "Lists the entity types a tracked project can synchronize",
            category = "Projects",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.EntityTypeOption.ServiceResult.DataShape"
            })
    /**
     * Lists the entity types {@link #SetTrackedProjectEntityTypes} accepts, by collection name,
     * with a readable label.
     *
     * @return a {@code GIT.EntityTypeOption.ServiceResult.DataShape} table
     */
    public InfoTable GetEntityTypeOptions() {
        try {
            InfoTable table =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            "GIT.EntityTypeOption.DataShape");
            for (String type : SyncedEntityTypes.SELECTABLE) {
                ValueCollection row = new ValueCollection();
                row.put("EntityType", new StringPrimitive(type));
                row.put("Label", new StringPrimitive(SyncedEntityTypes.label(type)));
                table.addRow(row);
            }
            return DapResults.success(
                    "GetEntityTypeOptions",
                    EntityTypeOptionTable.SERVICE_RESULT,
                    EntityTypeOptionTable.wrap(table));
        } catch (Exception e) {
            return DapResults.failure(
                    "GetEntityTypeOptions", e, EntityTypeOptionTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "SetTrackedProjectEntityTypes",
            description =
                    "Chooses which entity types a tracked project synchronizes with this repository",
            category = "Projects",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Sets the entity types a tracked project exports and imports. Files of other types are kept in
     * the repository and in ThingWorx, but no longer exported, imported or deleted by a sync.
     *
     * @param ProjectName a tracked project
     * @param EntityTypes comma-separated collections from {@link #GetEntityTypeOptions}, such as
     *     {@code Things,Mashups}; blank, or every option, means all types
     * @return a {@code GIT.StringResult.DataShape} status table
     */
    public InfoTable SetTrackedProjectEntityTypes(
            @ThingworxServiceParameter(
                            name = "ProjectName",
                            description = "Tracked ThingWorx project",
                            baseType = "STRING")
                    String ProjectName,
            @ThingworxServiceParameter(
                            name = "EntityTypes",
                            description =
                                    "Comma-separated entity types, such as Things,Mashups; blank means all types",
                            baseType = "STRING")
                    String EntityTypes) {
        String repoName = "";
        try {
            String name = FileRepositoryManager.validateProjectFolderName(ProjectName);
            String types = SyncedEntityTypes.normalize(EntityTypes);
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            repoName = fileRepo.repositoryThingName();
            try {
                synchronized (StatusExportReconciler.class) {
                    var projects = new ArrayList<>(fileRepo.trackedProjectEntries());
                    int index = -1;
                    for (int i = 0; i < projects.size(); i++) {
                        if (projects.get(i).name().equals(name)) index = i;
                    }
                    if (index < 0) {
                        throw new IllegalArgumentException(
                                "Project '" + name + "' is not tracked.");
                    }
                    var current = projects.get(index);
                    projects.set(
                            index,
                            new FileRepositoryManager.TrackedProject(
                                    current.name(), current.path(), types));
                    fileRepo.setTrackedProjects(projects);
                }
            } finally {
                fileRepo.close();
            }
            String message =
                    types.isEmpty()
                            ? "Project '" + name + "' now synchronizes every entity type."
                            : "Project '"
                                    + name
                                    + "' now synchronizes "
                                    + SyncedEntityTypes.describe(types)
                                    + ". Files of other entity types stay in the repository but"
                                    + " are no longer exported, imported or deleted.";
            GitActionLogger.logAction("SetTrackedProjectEntityTypes", repoName, message);
            return DapResults.success(
                    "SetTrackedProjectEntityTypes", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "SetTrackedProjectEntityTypes", repoName, String.valueOf(e.getMessage()));
            return DapResults.failure(
                    "SetTrackedProjectEntityTypes", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(TrackedDataTableSpec.class)
    @ThingworxServiceDefinition(
            name = "GetTrackedDataTables",
            description =
                    "Lists the tracked Data Tables: the Data/<DataTableName>.json files in the tracked project folders, with each one's mode and whether Git tracks the file",
            category = "Data Tables",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.TrackedDataTable.ServiceResult.DataShape"
            })
    /**
     * Lists the tracked Data Tables, which are the files checked into, or added to, the tracked
     * project folders.
     *
     * @return a {@code GIT.TrackedDataTable.ServiceResult.DataShape} table
     */
    public InfoTable GetTrackedDataTables() {
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            try {
                return DapResults.success(
                        "GetTrackedDataTables",
                        TrackedDataTableTable.SERVICE_RESULT,
                        TrackedDataTableTable.wrap(
                                fileRepo.dataTableTable(fileRepo.dataTableFiles())));
            } finally {
                fileRepo.close();
            }
        } catch (Exception e) {
            return DapResults.failure(
                    "GetTrackedDataTables", e, TrackedDataTableTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(TrackedDataTableSpec.class)
    @ThingworxServiceDefinition(
            name = "GetAvailableDataTables",
            description =
                    "Lists the Data Tables in the tracked projects, visible to the caller, that are not tracked yet",
            category = "Data Tables",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {
                "isEntityDataShape:true",
                "dataShape:GIT.TrackedDataTable.ServiceResult.DataShape"
            })
    /**
     * Lists Data Tables that can be added with {@link #AddTrackedDataTable}, sorted by name, each
     * with the file tracking it would write.
     *
     * @return a {@code GIT.TrackedDataTable.ServiceResult.DataShape} table
     */
    public InfoTable GetAvailableDataTables() {
        try {
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            try {
                return DapResults.success(
                        "GetAvailableDataTables",
                        TrackedDataTableTable.SERVICE_RESULT,
                        TrackedDataTableTable.wrap(
                                fileRepo.dataTableTable(fileRepo.availableDataTables())));
            } finally {
                fileRepo.close();
            }
        } catch (Exception e) {
            return DapResults.failure(
                    "GetAvailableDataTables", e, TrackedDataTableTable.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "AddTrackedDataTable",
            description =
                    "Tracks a Data Table of a tracked project by writing its rows to <project folder>/Data/<DataTableName>.json; commit the file to share it",
            category = "Data Tables",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Tracks a Data Table by writing its file. The file is left untracked by Git for review; Sync
     * to ThingWorx imports it once it is added or committed.
     *
     * @param DataTableName a Data Table in a tracked project, with a primary key
     * @param Mode {@code Mirror} (the default) or {@code Upsert}, stored in the file
     * @return a {@code GIT.StringResult.DataShape} status table
     */
    public InfoTable AddTrackedDataTable(
            @ThingworxServiceParameter(
                            name = "DataTableName",
                            description = "Data Table to track",
                            baseType = "STRING")
                    String DataTableName,
            @ThingworxServiceParameter(
                            name = "Mode",
                            description =
                                    "Mirror (default): Sync to ThingWorx also deletes rows missing from the file. Upsert: rows are only added and updated",
                            baseType = "STRING")
                    String Mode) {
        String repoName = "";
        try {
            String name = TrackedDataTables.validateName(DataTableName);
            String mode = TrackedDataTables.normalizeMode(Mode);
            // A table the caller may not see is reported exactly like a missing one.
            var table = TrackedDataTables.find(name);
            if (table == null) {
                throw new IllegalArgumentException("Data Table '" + name + "' was not found.");
            }
            TrackedDataTables.requirePrimaryKey(table);
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            repoName = fileRepo.repositoryThingName();
            String file;
            try {
                file = fileRepo.trackDataTable(table, mode);
            } finally {
                fileRepo.close();
            }
            String message =
                    "Data Table '"
                            + name
                            + "' is now tracked as "
                            + TrackedDataTables.describeMode(mode)
                            + ". Its rows were written to '"
                            + file
                            + "'; commit the file to share it.";
            GitActionLogger.logAction("AddTrackedDataTable", repoName, message);
            return DapResults.success(
                    "AddTrackedDataTable", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "AddTrackedDataTable", repoName, String.valueOf(e.getMessage()));
            return DapResults.failure(
                    "AddTrackedDataTable", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "SetTrackedDataTableMode",
            description =
                    "Sets the mode in a tracked Data Table's file: Mirror deletes rows missing from the file on Sync to ThingWorx, Upsert only adds and updates rows",
            category = "Data Tables",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Changes the mode stored in a tracked Data Table's file; commit the file to share it.
     *
     * @param DataTableName a tracked Data Table
     * @param Mode {@code Mirror} or {@code Upsert}
     * @return a {@code GIT.StringResult.DataShape} status table
     */
    public InfoTable SetTrackedDataTableMode(
            @ThingworxServiceParameter(
                            name = "DataTableName",
                            description = "Tracked Data Table",
                            baseType = "STRING")
                    String DataTableName,
            @ThingworxServiceParameter(
                            name = "Mode",
                            description = "Mirror or Upsert",
                            baseType = "STRING")
                    String Mode) {
        String repoName = "";
        try {
            String name = TrackedDataTables.validateName(DataTableName);
            String mode = TrackedDataTables.normalizeMode(Mode);
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            repoName = fileRepo.repositoryThingName();
            String file;
            try {
                file = fileRepo.setDataTableMode(name, mode);
            } finally {
                fileRepo.close();
            }
            String message =
                    "Data Table '"
                            + name
                            + "' now syncs as "
                            + TrackedDataTables.describeMode(mode)
                            + ". Commit '"
                            + file
                            + "' to share the change.";
            GitActionLogger.logAction("SetTrackedDataTableMode", repoName, message);
            return DapResults.success(
                    "SetTrackedDataTableMode", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "SetTrackedDataTableMode", repoName, String.valueOf(e.getMessage()));
            return DapResults.failure(
                    "SetTrackedDataTableMode", e, StringResultContract.SERVICE_RESULT);
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "RemoveTrackedDataTable",
            description =
                    "Stops tracking a Data Table by deleting its file from the working tree; the rows in ThingWorx are not changed",
            category = "Data Tables",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Stops tracking a Data Table by deleting its file. The deletion is left unstaged for review in
     * Changes, and the rows in ThingWorx are never changed.
     *
     * @param DataTableName a tracked Data Table
     * @return a {@code GIT.StringResult.DataShape} status table
     */
    public InfoTable RemoveTrackedDataTable(
            @ThingworxServiceParameter(
                            name = "DataTableName",
                            description = "Tracked Data Table",
                            baseType = "STRING")
                    String DataTableName) {
        String repoName = "";
        try {
            String name = TrackedDataTables.validateName(DataTableName);
            var fileRepo = new FileRepositoryManager(resolveTargetThing());
            repoName = fileRepo.repositoryThingName();
            String file;
            try {
                file = fileRepo.untrackDataTable(name);
            } finally {
                fileRepo.close();
            }
            String message =
                    "Data Table '"
                            + name
                            + "' is no longer tracked; its rows in ThingWorx are unchanged. '"
                            + file
                            + "' was deleted; review and commit the deletion in Changes.";
            GitActionLogger.logAction("RemoveTrackedDataTable", repoName, message);
            return DapResults.success(
                    "RemoveTrackedDataTable", message, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction(
                    "RemoveTrackedDataTable", repoName, String.valueOf(e.getMessage()));
            return DapResults.failure(
                    "RemoveTrackedDataTable", e, StringResultContract.SERVICE_RESULT);
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
            Git myGitFolder = fileRepo.git();
            ObjectId mergeBase = myGitFolder.getRepository().resolve(BranchName);
            if (mergeBase == null) {
                return DapResults.failure(
                        "Merge",
                        "Merge Error: " + String.format(Const.ERR_BRANCH_NOT_FOUND, BranchName),
                        StringResultContract.SERVICE_RESULT);
            }
            MergeResult[] merged = new MergeResult[1];
            ConflictWorkflow.Outcome outcome;
            try (CommitScope ignored = commitScope(myGitFolder.getRepository())) {
                outcome =
                        ConflictWorkflow.run(
                                myGitFolder,
                                "Merge",
                                () -> {
                                    merged[0] = myGitFolder.merge().include(mergeBase).call();
                                    return ConflictWorkflow.outcome(merged[0]);
                                });
            }
            if (outcome == ConflictWorkflow.Outcome.CONFLICTS) {
                return conflictResult("Merge", "Merge", myGitFolder);
            }
            String LogResult =
                    "Merged "
                            + BranchName
                            + ": "
                            + ResultMessages.merge(
                                    merged[0], myGitFolder.getRepository().getBranch());
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
            ConflictWorkflow.Outcome outcome;
            try (CommitScope ignored = commitScope(myGitFolder.getRepository())) {
                outcome =
                        ConflictWorkflow.run(
                                myGitFolder,
                                "Rebase",
                                () -> {
                                    rebased[0] = myGitFolder.rebase().setUpstream(upstream).call();
                                    return ConflictWorkflow.outcome(rebased[0]);
                                });
            }
            if (outcome == ConflictWorkflow.Outcome.CONFLICTS) {
                return conflictResult("Rebase", "Rebase", myGitFolder);
            }
            String LogResult =
                    "Rebase onto "
                            + UpstreamBranch
                            + ": "
                            + ResultMessages.rebase(
                                    rebased[0], myGitFolder.getRepository().getBranch(), upstream);
            GitActionLogger.logAction("Rebase", resolveTargetThing().getName(), LogResult);
            return DapResults.success("Rebase", LogResult, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return DapResults.failure("Rebase", e, StringResultContract.SERVICE_RESULT);
        }
    }

    /**
     * Creates or moves a tag at a commit, {@code HEAD} by default. The tag is signed with the
     * current user's signing key for this repository, as commits are, and a signed tag is always
     * annotated. Nothing is pushed; see {@link #PushTags}.
     */
    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "CreateTag",
            description =
                    "Creates a tag on a commit (HEAD by default), signed with your signing key for this repository like commits. A signed or messaged tag is annotated. Force moves an existing tag. Nothing is pushed.",
            category = "Tags",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Creates or moves a local tag. Unsigned tags without a message are lightweight; every other
     * tag is annotated, with the current user's commit identity as tagger.
     *
     * @param TagName tag name, such as {@code v1.2.0}
     * @param Message annotation message; blank for a signed tag uses the tag name
     * @param CommitID optional target commit or ref; blank uses {@code HEAD}
     * @param Force replace an existing tag of the same name
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
                            description =
                                    "Optional tag message (creates an annotated tag); a signed tag without one uses the tag name",
                            baseType = "STRING")
                    String Message,
            @ThingworxServiceParameter(
                            name = "CommitID",
                            description = "Optional commit or ref to tag (defaults to HEAD)",
                            baseType = "STRING")
                    String CommitID,
            @ThingworxServiceParameter(
                            name = "Force",
                            description = "Move the tag if it already exists",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean Force) {
        String repoName = resolveTargetThing().getName();
        FileRepositoryManager fileRepo = null;
        Signer signer = null;
        try {
            String name = validTagName(TagName);
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git git = fileRepo.git();
            Repository repo = git.getRepository();
            String target = CommitID == null || CommitID.isBlank() ? "HEAD" : CommitID.trim();
            ObjectId commitId = repo.resolve(target + "^{commit}");
            if (commitId == null) {
                throw new IllegalArgumentException(
                        String.format(Const.ERR_COMMIT_NOT_FOUND, target));
            }
            boolean exists = repo.exactRef(Constants.R_TAGS + name) != null;
            boolean force = Boolean.TRUE.equals(Force);
            if (exists && !force) {
                throw new IllegalArgumentException(
                        "Tag '"
                                + name
                                + "' already exists. Choose Force to move it to this commit.");
            }
            RevCommit commit;
            try (RevWalk walk = new RevWalk(repo)) {
                commit = walk.parseCommit(commitId);
            }
            signer = resolveSigner(null, null);
            String message = Message == null ? "" : Message.trim();
            var command = git.tag().setName(name).setObjectId(commit).setForceUpdate(force);
            if (signer != null || !message.isEmpty()) {
                command.setAnnotated(true).setMessage(message.isEmpty() ? name : message);
                GitUserContextManager.Identity identity =
                        new GitUserContextManager().identityFor(repoName);
                if (identity.complete()) {
                    command.setTagger(new PersonIdent(identity.name(), identity.email()));
                }
            } else {
                command.setAnnotated(false);
            }
            if (signer != null) {
                command.setSigned(true).setSigningKey(null).setSigner(signer);
            } else {
                command.setSigned(false);
            }
            command.call();
            String result =
                    String.format(
                                    Const.SUCCESS_TAG_CREATED,
                                    name,
                                    commit.getId().abbreviate(7).name())
                            + (exists ? " (moved)" : "")
                            + (signer != null ? ", signed." : ".")
                            + " Push it to share it.";
            GitActionLogger.logAction("CreateTag", repoName, result);
            return DapResults.success("CreateTag", result, StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            GitActionLogger.logAction("CreateTag", repoName, String.valueOf(e.getMessage()));
            return DapResults.failure("CreateTag", e, StringResultContract.SERVICE_RESULT);
        } finally {
            clearSigner(signer);
            if (fileRepo != null) fileRepo.close();
        }
    }

    /** A tag name that Git accepts, without the {@code refs/tags/} prefix. */
    private static String validTagName(String tagName) {
        String name = tagName == null ? "" : tagName.trim();
        if (name.startsWith(Constants.R_TAGS)) name = name.substring(Constants.R_TAGS.length());
        if (name.isEmpty()) {
            throw new IllegalArgumentException(Const.ERR_PREFIX_CONFIG + Const.ERR_NO_TAG_NAME);
        }
        if (!Repository.isValidRefName(Constants.R_TAGS + name)) {
            throw new IllegalArgumentException("'" + name + "' is not a valid tag name.");
        }
        return name;
    }

    @DapServicePayload(TagListSpec.class)
    @ThingworxServiceDefinition(
            name = "GetTagList",
            description =
                    "Returns all local tags with their commit, message, date, tagger, and whether each is annotated and signed.",
            category = "Tags",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.TagList.ServiceResult.DataShape"})
    /**
     * Lists local tags, sorted by name. This is read-only and does not fetch tags from a remote.
     *
     * @return a {@code GIT.TagList.ServiceResult.DataShape} table
     */
    public InfoTable GetTagList() {
        FileRepositoryManager fileRepo = null;
        try {
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            Repository repo = fileRepo.git().getRepository();
            InfoTable tagList =
                    InfoTableInstanceFactory.createInfoTableFromDataShape("GIT.TagList.DataShape");
            // Existing installations keep their original DataShape on upgrade; extend a copy.
            tagList.setDataShape(tagList.getDataShape().clone());
            addStatusField(tagList, "Annotated", com.thingworx.types.BaseTypes.BOOLEAN, 5);
            addStatusField(tagList, "Signed", com.thingworx.types.BaseTypes.BOOLEAN, 6);
            addStatusField(tagList, "Tagger", com.thingworx.types.BaseTypes.STRING, 7);
            List<Ref> tags = new ArrayList<>(fileRepo.git().tagList().call());
            tags.sort(Comparator.comparing(Ref::getName));
            try (RevWalk walk = new RevWalk(repo)) {
                for (Ref ref : tags) {
                    String tagName = ref.getName().substring(Constants.R_TAGS.length());
                    RevObject object = walk.parseAny(ref.getObjectId());
                    String message = "";
                    String tagger = "";
                    DateTime date = new DateTime(0);
                    boolean annotated = object instanceof RevTag;
                    boolean signed = false;
                    if (object instanceof RevTag tag) {
                        message = tag.getShortMessage();
                        signed = tag.getRawGpgSignature() != null;
                        PersonIdent ident = tag.getTaggerIdent();
                        if (ident != null) {
                            tagger = ident.getName();
                            date = new DateTime(ident.getWhenAsInstant().toEpochMilli());
                        }
                    }
                    RevObject peeled = walk.peel(object);
                    if (peeled instanceof RevCommit commit && date.getMillis() == 0) {
                        date =
                                new DateTime(
                                        commit.getCommitterIdent()
                                                .getWhenAsInstant()
                                                .toEpochMilli());
                    }
                    ValueCollection row = new ValueCollection();
                    row.put("TagName", new StringPrimitive(tagName));
                    row.put("CommitID", new StringPrimitive(peeled.getId().name()));
                    row.put("Message", new StringPrimitive(message));
                    row.put("Date", new DatetimePrimitive(date));
                    row.put("Annotated", new BooleanPrimitive(annotated));
                    row.put("Signed", new BooleanPrimitive(signed));
                    row.put("Tagger", new StringPrimitive(tagger));
                    tagList.addRow(row);
                }
            }
            return DapResults.success(
                    "GetTagList", TagListTable.SERVICE_RESULT, TagListTable.wrap(tagList));
        } catch (Exception ex) {
            return DapResults.failure("GetTagList", ex, TagListTable.SERVICE_RESULT);
        } finally {
            if (fileRepo != null) fileRepo.close();
        }
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "PushTags",
            description =
                    "Pushes one tag, or every local tag when TagName is blank, to a remote. Force replaces a tag that points elsewhere on the remote.",
            category = "Tags",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Pushes tags with the current user's sign-in for the remote.
     *
     * @param Remote remote name; blank uses the current branch's upstream remote, or origin
     * @param TagName one tag; blank pushes every local tag
     * @param Force replace a remote tag that points at another object
     * @return a {@code GIT.StringResult.DataShape} table
     */
    public InfoTable PushTags(
            @ThingworxServiceParameter(
                            name = "Remote",
                            description =
                                    "Optional remote name; blank uses the current branch's upstream remote, or origin",
                            baseType = "STRING")
                    String Remote,
            @ThingworxServiceParameter(
                            name = "TagName",
                            description = "Tag to push; blank pushes every local tag",
                            baseType = "STRING")
                    String TagName,
            @ThingworxServiceParameter(
                            name = "Force",
                            description = "Replace a remote tag that points elsewhere",
                            baseType = "BOOLEAN",
                            aspects = {"defaultValue:false"})
                    Boolean Force) {
        String repoName = resolveTargetThing().getName();
        GitTransport transport = null;
        FileRepositoryManager fileRepo = null;
        try {
            boolean all = TagName == null || TagName.isBlank();
            String name = all ? "" : validTagName(TagName);
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git git = fileRepo.git();
            Repository repo = git.getRepository();
            if (!all && repo.exactRef(Constants.R_TAGS + name) == null) {
                throw new IllegalArgumentException("Tag '" + name + "' does not exist.");
            }
            String remote = Remotes.resolve(repo, Remote, repo.getBranch());
            transport = GitTransport.forRepository(repoName);
            String spec = all ? Constants.R_TAGS + "*" : Constants.R_TAGS + name;
            Iterable<PushResult> results =
                    transport
                            .configure(git.push(), remote)
                            .setRemote(remote)
                            .setForce(Boolean.TRUE.equals(Force))
                            .setRefSpecs(new RefSpec(spec + ":" + spec))
                            .call();
            String message = tagPushSummary(results, remote, "Pushed");
            GitActionLogger.logAction("PushTags", repoName, message);
            return DapResults.success("PushTags", message, StringResultContract.SERVICE_RESULT);
        } catch (TagPushRejected e) {
            GitActionLogger.logAction("PushTags", repoName, e.getMessage());
            return DapResults.failure(
                    "PushTags",
                    "PushTags Error: " + Const.ERR_PREFIX_GIT + e.getMessage(),
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return transportFailure("PushTags", transport, e);
        } finally {
            if (fileRepo != null) fileRepo.close();
            if (transport != null) transport.close();
        }
    }

    /** A remote refused a tag update; the message says which and why. */
    private static final class TagPushRejected extends Exception {
        private static final long serialVersionUID = 1L;

        TagPushRejected(String message) {
            super(message);
        }
    }

    /**
     * Summarizes a tag push, such as {@code Pushed v1.0, v1.1 to origin; v0.9 was already up to
     * date}, or throws {@link TagPushRejected} naming the first rejected tag.
     */
    private static String tagPushSummary(Iterable<PushResult> results, String remote, String verb)
            throws TagPushRejected {
        List<String> done = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (PushResult result : results) {
            for (RemoteRefUpdate update : result.getRemoteUpdates()) {
                String tag = Repository.shortenRefName(update.getRemoteName());
                if (!ResultMessages.accepted(update)) {
                    throw new TagPushRejected(ResultMessages.pushRejected(result, update, remote));
                }
                if (update.getStatus() == RemoteRefUpdate.Status.UP_TO_DATE) current.add(tag);
                else done.add(tag);
            }
        }
        if (done.isEmpty() && current.isEmpty()) return "There are no tags to push.";
        StringBuilder message = new StringBuilder();
        if (!done.isEmpty()) {
            message.append(verb).append(' ').append(String.join(", ", done)).append(" on ");
            message.append(remote);
        }
        if (!current.isEmpty()) {
            if (message.length() > 0) message.append("; ");
            message.append(String.join(", ", current))
                    .append(current.size() == 1 ? " was" : " were")
                    .append(" already up to date on ")
                    .append(remote);
        }
        return message.append('.').toString();
    }

    @DapServicePayload(StringResultSpec.class)
    @ThingworxServiceDefinition(
            name = "DeleteTag",
            description =
                    "Deletes a local tag and, when Remote is set, the tag of the same name on that remote.",
            category = "Tags",
            isAllowOverride = false,
            aspects = {"isAsync:false"})
    @ThingworxServiceResult(
            name = "Result",
            description = "",
            baseType = "INFOTABLE",
            aspects = {"isEntityDataShape:true", "dataShape:GIT.StringResult.DataShape"})
    /**
     * Deletes a tag locally and optionally on a remote. The remote tag is deleted first, so a
     * rejected remote deletion leaves the local tag in place to try again.
     *
     * @param TagName tag to delete
     * @param Remote optional remote whose tag of the same name is deleted too
     * @return a {@code GIT.StringResult.DataShape} table
     */
    public InfoTable DeleteTag(
            @ThingworxServiceParameter(
                            name = "TagName",
                            description = "Name of the tag to delete",
                            baseType = "STRING")
                    String TagName,
            @ThingworxServiceParameter(
                            name = "Remote",
                            description =
                                    "Optional remote to delete the tag from as well; blank deletes the local tag only",
                            baseType = "STRING")
                    String Remote) {
        String repoName = resolveTargetThing().getName();
        GitTransport transport = null;
        FileRepositoryManager fileRepo = null;
        try {
            String name = validTagName(TagName);
            fileRepo = new FileRepositoryManager(resolveTargetThing());
            Git git = fileRepo.git();
            Repository repo = git.getRepository();
            boolean local = repo.exactRef(Constants.R_TAGS + name) != null;
            List<String> done = new ArrayList<>();
            if (Remote != null && !Remote.isBlank()) {
                String remote = Remotes.resolve(repo, Remote, repo.getBranch());
                transport = GitTransport.forRepository(repoName);
                Iterable<PushResult> results =
                        transport
                                .configure(git.push(), remote)
                                .setRemote(remote)
                                .setRefSpecs(new RefSpec(":" + Constants.R_TAGS + name))
                                .call();
                tagPushSummary(results, remote, "Deleted");
                done.add("on " + remote);
            }
            if (local) {
                git.tagDelete().setTags(Constants.R_TAGS + name).call();
                done.add("locally");
            }
            if (done.isEmpty()) {
                throw new IllegalArgumentException("Tag '" + name + "' does not exist.");
            }
            String message = "Tag '" + name + "' deleted " + String.join(" and ", done) + ".";
            GitActionLogger.logAction("DeleteTag", repoName, message);
            return DapResults.success("DeleteTag", message, StringResultContract.SERVICE_RESULT);
        } catch (TagPushRejected e) {
            GitActionLogger.logAction("DeleteTag", repoName, e.getMessage());
            return DapResults.failure(
                    "DeleteTag",
                    "DeleteTag Error: " + Const.ERR_PREFIX_GIT + e.getMessage(),
                    StringResultContract.SERVICE_RESULT);
        } catch (Exception e) {
            return transportFailure("DeleteTag", transport, e);
        } finally {
            if (fileRepo != null) fileRepo.close();
            if (transport != null) transport.close();
        }
    }
}
