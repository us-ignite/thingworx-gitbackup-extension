package org.us_ignite.thingworx.jgit.extension;

import org.us_ignite.thingworx.dap.DapBaseType;
import org.us_ignite.thingworx.dap.DapDataShape;
import org.us_ignite.thingworx.dap.DapExtensionPackage;
import org.us_ignite.thingworx.dap.DapField;
import org.us_ignite.thingworx.dap.DapLocalizationToken;
import org.us_ignite.thingworx.dap.DapNoPayloadServiceResult;
import org.us_ignite.thingworx.dap.DapProject;
import org.us_ignite.thingworx.dap.DapSubscription;
import org.us_ignite.thingworx.dap.DapThing;
import org.us_ignite.thingworx.dap.DapThingPackage;
import org.us_ignite.thingworx.dap.DapThingTemplate;

/** Complete non-service ThingWorx entity declaration for the JGit extension. */
public final class JGitEntityDeclarations {
    private JGitEntityDeclarations() {}

    @DapNoPayloadServiceResult(name = "GIT.StringResult.DataShape")
    public static final class StringResultSpec {}

    @DapDataShape(
            name = "GIT.RepositoryList.DataShape",
            description = "Available GIT Repository Things.",
            serviceResultName = "GIT.RepositoryList.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying repository names.",
            payloadDescription = "Repository list payload")
    public static final class RepositoryListSpec {
        @DapField(
                name = "RepoName",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                description = "Repository Thing name")
        private String repoName;
    }

    @DapDataShape(
            name = "GIT.CurrentBranchStatus.DataShape",
            serviceResultDescription = "Service result carrying the current branch.",
            payloadDescription = "Current branch payload")
    public static final class CurrentBranchStatusSpec {
        @DapField(name = "BranchName", baseType = DapBaseType.STRING, ordinal = 1)
        private String branchName;

        @DapField(name = "DetachedHEAD", baseType = DapBaseType.BOOLEAN, ordinal = 2)
        private Boolean detachedHead;

        @DapField(name = "AheadStatus", baseType = DapBaseType.STRING, ordinal = 3)
        private String aheadStatus;
    }

    @DapDataShape(
            name = "GIT.BranchList.DataShape",
            serviceResultDescription = "Service result carrying branch rows.",
            payloadDescription = "Branch payload")
    public static final class BranchListSpec {
        @DapField(name = "BranchName", baseType = DapBaseType.STRING, ordinal = 1)
        private String branchName;

        @DapField(name = "ShortBranchName", baseType = DapBaseType.STRING, ordinal = 2)
        private String shortBranchName;

        @DapField(name = "BranchType", baseType = DapBaseType.STRING, ordinal = 3)
        private String branchType;

        @DapField(
                name = "CommitID",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "Commit ID at the branch tip")
        private String commitId;

        @DapField(
                name = "IsHead",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 5,
                description = "Whether this branch is the current HEAD")
        private Boolean isHead;
    }

    @DapDataShape(
            name = "GIT.CommitLog.DataShape",
            description = "A commit history entry.",
            serviceResultDescription = "Service result carrying commit-log rows.",
            payloadDescription = "Commit-log payload")
    public static final class CommitLogSpec {
        @DapField(
                name = "CommitID",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                description = "Full commit object ID")
        private String commitId;

        @DapField(
                name = "CommitName",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Commit short message")
        private String commitName;

        @DapField(
                name = "CommitTime",
                baseType = DapBaseType.DATETIME,
                ordinal = 3,
                description = "Committer timestamp")
        private Object commitTime;

        @DapField(
                name = "AuthorName",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "Author name")
        private String authorName;

        @DapField(
                name = "AuthorEmail",
                baseType = DapBaseType.STRING,
                ordinal = 5,
                description = "Author email address")
        private String authorEmail;

        @DapField(
                name = "CommitterName",
                baseType = DapBaseType.STRING,
                ordinal = 6,
                description = "Committer name")
        private String committerName;

        @DapField(
                name = "CommitterEmail",
                baseType = DapBaseType.STRING,
                ordinal = 7,
                description = "Committer email address")
        private String committerEmail;

        @DapField(
                name = "ParentCommitIDs",
                baseType = DapBaseType.STRING,
                ordinal = 8,
                description = "Parent commit IDs, comma-separated")
        private String parentCommitIds;

        @DapField(
                name = "SignatureFormat",
                baseType = DapBaseType.STRING,
                ordinal = 9,
                description =
                        "Signature format: openpgp, ssh, x509 or unknown; blank for an unsigned commit. The signature is not verified.")
        private String signatureFormat;

        @DapField(
                name = "SignatureKey",
                baseType = DapBaseType.STRING,
                ordinal = 10,
                description =
                        "Key the signature names: SSH SHA256 fingerprint, or OpenPGP fingerprint or key ID")
        private String signatureKey;
    }

    @DapDataShape(
            name = "GIT.ReflogEntry.DataShape",
            description = "A local Git reflog entry.",
            serviceResultDescription = "Service result carrying reflog rows.",
            payloadDescription = "Reflog payload")
    public static final class ReflogEntrySpec {
        @DapField(
                name = "RefName",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                description = "Reflog ref name")
        private String refName;

        @DapField(
                name = "OldObjectID",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Old object ID")
        private String oldObjectId;

        @DapField(
                name = "NewObjectID",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description = "New object ID")
        private String newObjectId;

        @DapField(
                name = "ActorName",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "Actor name")
        private String actorName;

        @DapField(
                name = "ActorEmail",
                baseType = DapBaseType.STRING,
                ordinal = 5,
                description = "Actor email address")
        private String actorEmail;

        @DapField(
                name = "EventTime",
                baseType = DapBaseType.DATETIME,
                ordinal = 6,
                description = "Reflog event timestamp")
        private Object eventTime;

        @DapField(
                name = "Comment",
                baseType = DapBaseType.STRING,
                ordinal = 7,
                description = "Reflog comment")
        private String comment;

        @DapField(
                name = "CheckoutSource",
                baseType = DapBaseType.STRING,
                ordinal = 8,
                description = "Checkout source branch, when available")
        private String checkoutSource;

        @DapField(
                name = "CheckoutTarget",
                baseType = DapBaseType.STRING,
                ordinal = 9,
                description = "Checkout target branch, when available")
        private String checkoutTarget;
    }

    @DapDataShape(
            name = "GIT.HistoryRef.DataShape",
            description = "A ref that log and reflog views can select.",
            serviceResultDescription = "Service result carrying history refs.",
            payloadDescription = "History ref payload")
    public static final class HistoryRefSpec {
        @DapField(
                name = "RefName",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                description = "Full ref name, or HEAD")
        private String refName;

        @DapField(
                name = "DisplayName",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description =
                        "Short ref name that GetLog and GetReflog resolve, such as main or origin/main")
        private String displayName;

        @DapField(
                name = "RefType",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description = "HEAD, LOCAL, REMOTE, or TAG")
        private String refType;

        @DapField(
                name = "CommitID",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "Commit the ref points to; blank for an unborn branch")
        private String commitId;

        @DapField(
                name = "IsHead",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 5,
                description = "Whether this is HEAD or the branch HEAD points to")
        private Boolean isHead;

        @DapField(
                name = "HasReflog",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 6,
                description = "Whether this clone has local reflog entries for the ref")
        private Boolean hasReflog;
    }

    @DapDataShape(
            name = "GIT.ConflictState.DataShape",
            description = "The conflicted operation a repository is waiting on.",
            serviceResultDescription = "Service result carrying the conflict state.",
            payloadDescription = "Conflict state payload")
    public static final class ConflictStateSpec {
        @DapField(
                name = "InProgress",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 1,
                description = "Whether an operation is waiting for conflict resolution")
        private Boolean inProgress;

        @DapField(
                name = "Phase",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "NONE, MERGE, REBASE, LOCAL_CHANGES, or OTHER")
        private String phase;

        @DapField(
                name = "Operation",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description = "Operation that stopped, such as Pull or Branch switch")
        private String operation;

        @DapField(
                name = "RepositoryState",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "JGit repository state, such as MERGING or REBASING_MERGE")
        private String repositoryState;

        @DapField(
                name = "CurrentLabel",
                baseType = DapBaseType.STRING,
                ordinal = 5,
                description = "Label for the existing side of each conflict")
        private String currentLabel;

        @DapField(
                name = "IncomingLabel",
                baseType = DapBaseType.STRING,
                ordinal = 6,
                description = "Label for the incoming side of each conflict")
        private String incomingLabel;

        @DapField(
                name = "Description",
                baseType = DapBaseType.STRING,
                ordinal = 7,
                description = "What happened, for display")
        private String description;

        @DapField(
                name = "ConflictCount",
                baseType = DapBaseType.INTEGER,
                ordinal = 8,
                description = "Number of files that still have conflicts")
        private Integer conflictCount;

        @DapField(
                name = "ConflictFiles",
                baseType = DapBaseType.STRING,
                ordinal = 9,
                description = "Newline-separated repository-relative paths that still conflict")
        private String conflictFiles;

        @DapField(
                name = "CanSkip",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 10,
                description = "Whether the current commit can be skipped (rebase only)")
        private Boolean canSkip;
    }

    @DapDataShape(
            name = "GIT.ConflictFile.DataShape",
            description = "The versions of one conflicted file.",
            serviceResultDescription = "Service result carrying conflicted file versions.",
            payloadDescription = "Conflicted file payload")
    public static final class ConflictFileSpec {
        @DapField(
                name = "File",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                description = "Repository-relative path")
        private String file;

        @DapField(
                name = "Conflicted",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 2,
                description = "Whether the file still has unmerged index entries")
        private Boolean conflicted;

        @DapField(
                name = "Binary",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 3,
                description = "Whether any version is binary; text fields are then blank")
        private Boolean binary;

        @DapField(
                name = "Working",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "Working-tree content, with conflict markers while unresolved")
        private String working;

        @DapField(
                name = "Current",
                baseType = DapBaseType.STRING,
                ordinal = 5,
                description = "Existing version")
        private String current;

        @DapField(
                name = "Incoming",
                baseType = DapBaseType.STRING,
                ordinal = 6,
                description = "Incoming version")
        private String incoming;

        @DapField(
                name = "Base",
                baseType = DapBaseType.STRING,
                ordinal = 7,
                description = "Common ancestor version")
        private String base;

        @DapField(name = "WorkingExists", baseType = DapBaseType.BOOLEAN, ordinal = 8)
        private Boolean workingExists;

        @DapField(name = "CurrentExists", baseType = DapBaseType.BOOLEAN, ordinal = 9)
        private Boolean currentExists;

        @DapField(name = "IncomingExists", baseType = DapBaseType.BOOLEAN, ordinal = 10)
        private Boolean incomingExists;

        @DapField(name = "BaseExists", baseType = DapBaseType.BOOLEAN, ordinal = 11)
        private Boolean baseExists;
    }

    @DapDataShape(
            name = "GIT.Status.DataShape",
            serviceResultDescription = "Service result carrying repository status rows.",
            payloadDescription = "Status payload")
    public static final class StatusSpec {
        @DapField(name = "File", baseType = DapBaseType.STRING, ordinal = 1)
        private String file;

        @DapField(name = "Status", baseType = DapBaseType.STRING, ordinal = 2)
        private String status;

        @DapField(
                name = "Staged",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 3,
                description = "Whether the change is currently staged in the Git index")
        private Boolean staged;

        @DapField(name = "FileName", baseType = DapBaseType.STRING, ordinal = 4)
        private String fileName;

        @DapField(name = "Path", baseType = DapBaseType.STRING, ordinal = 5)
        private String path;

        @DapField(name = "Area", baseType = DapBaseType.STRING, ordinal = 6)
        private String area;

        @DapField(name = "ChangeType", baseType = DapBaseType.STRING, ordinal = 7)
        private String changeType;

        @DapField(name = "CanStage", baseType = DapBaseType.BOOLEAN, ordinal = 8)
        private Boolean canStage;

        @DapField(name = "Tracked", baseType = DapBaseType.BOOLEAN, ordinal = 9)
        private Boolean tracked;
    }

    @DapDataShape(
            name = "GIT.WorkingTreeDisplay.DataShape",
            description = "A staged, unstaged, or conflicted working-tree change.",
            serviceResultDescription = "Service result carrying working-tree status rows.",
            payloadDescription = "Working-tree status payload")
    public static final class WorkingTreeStatusSpec {
        @DapField(name = "File", baseType = DapBaseType.STRING, ordinal = 1)
        private String file;

        @DapField(name = "FileName", baseType = DapBaseType.STRING, ordinal = 7)
        private String fileName;

        @DapField(name = "Path", baseType = DapBaseType.STRING, ordinal = 8)
        private String path;

        @DapField(
                name = "Area",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Grouping area: Staged Changes, Changes, Conflicted, or Unchanged")
        private String area;

        @DapField(
                name = "ChangeType",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description = "Added, Modified, Deleted, Untracked, Conflicted, or Unchanged")
        private String changeType;

        @DapField(
                name = "Staged",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 4,
                description = "Whether the change is currently staged in the Git index")
        private Boolean staged;

        @DapField(
                name = "CanStage",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 5,
                description = "Whether the change can be staged from the working tree")
        private Boolean canStage;

        @DapField(
                name = "Tracked",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 6,
                description = "Whether the path is tracked by Git")
        private Boolean tracked;
    }

    @DapDataShape(name = "GIT.CommitChangedFiles.DataShape", generateServiceResult = false)
    public static final class CommitChangedFilesSpec {
        @DapField(name = "FileName", baseType = DapBaseType.STRING, ordinal = 1)
        private String fileName;

        @DapField(name = "Status", baseType = DapBaseType.STRING, ordinal = 2)
        private String status;

        @DapField(
                name = "OldFileName",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description = "Previous path for a rename or delete")
        private String oldFileName;
    }

    @DapDataShape(
            name = "GIT.CommitTreeEntry.DataShape",
            description = "A file entry in a commit tree.",
            serviceResultDescription = "Service result carrying commit tree entries.",
            payloadDescription = "Commit tree payload")
    public static final class CommitTreeEntrySpec {
        @DapField(name = "Path", baseType = DapBaseType.STRING, ordinal = 1)
        private String path;

        @DapField(name = "Name", baseType = DapBaseType.STRING, ordinal = 2)
        private String name;

        @DapField(name = "ParentPath", baseType = DapBaseType.STRING, ordinal = 3)
        private String parentPath;

        @DapField(name = "Size", baseType = DapBaseType.LONG, ordinal = 4)
        private Long size;

        @DapField(name = "Mode", baseType = DapBaseType.STRING, ordinal = 5)
        private String mode;
    }

    @DapDataShape(
            name = "GIT.FileContent.DataShape",
            description = "Content of a file at a commit.",
            serviceResultDescription = "Service result carrying file content.",
            payloadDescription = "File content payload")
    public static final class FileContentSpec {
        @DapField(name = "Path", baseType = DapBaseType.STRING, ordinal = 1)
        private String path;

        @DapField(name = "Content", baseType = DapBaseType.STRING, ordinal = 2)
        private String content;

        @DapField(name = "Size", baseType = DapBaseType.LONG, ordinal = 3)
        private Long size;

        @DapField(name = "Binary", baseType = DapBaseType.BOOLEAN, ordinal = 4)
        private Boolean binary;

        @DapField(name = "Truncated", baseType = DapBaseType.BOOLEAN, ordinal = 5)
        private Boolean truncated;
    }

    @DapDataShape(
            name = "GIT.CommitInfo.DataShape",
            serviceResultDescription = "Service result carrying commit details.",
            payloadDescription = "Commit information payload")
    public static final class CommitInfoSpec {
        @DapField(name = "CommitID", baseType = DapBaseType.STRING, ordinal = 1)
        private String commitId;

        @DapField(name = "Parents", baseType = DapBaseType.STRING, ordinal = 2)
        private String parents;

        @DapField(name = "Author", baseType = DapBaseType.STRING, ordinal = 3)
        private String author;

        @DapField(name = "Date", baseType = DapBaseType.DATETIME, ordinal = 4)
        private Object date;

        @DapField(name = "Commiter", baseType = DapBaseType.STRING, ordinal = 5)
        private String commiter;

        @DapField(name = "CommitDescription", baseType = DapBaseType.STRING, ordinal = 6)
        private String commitDescription;

        @DapField(
                name = "ChangedFiles",
                baseType = DapBaseType.INFOTABLE,
                ordinal = 7,
                dataShape = "GIT.CommitChangedFiles.DataShape")
        private Object changedFiles;

        @DapField(
                name = "SignatureVerification",
                baseType = DapBaseType.STRING,
                ordinal = 8,
                description =
                        "GPG signature verification status (e.g. GOODSIG, BADSIG, or empty if unsigned)")
        private String signatureVerification;
    }

    @DapDataShape(
            name = "GIT.TagList.DataShape",
            serviceResultDescription = "Service result carrying tag rows.",
            payloadDescription = "Tag payload")
    public static final class TagListSpec {
        @DapField(name = "TagName", baseType = DapBaseType.STRING, ordinal = 1)
        private String tagName;

        @DapField(name = "CommitID", baseType = DapBaseType.STRING, ordinal = 2)
        private String commitId;

        @DapField(name = "Message", baseType = DapBaseType.STRING, ordinal = 3)
        private String message;

        @DapField(name = "Date", baseType = DapBaseType.DATETIME, ordinal = 4)
        private Object date;
    }

    @DapDataShape(
            name = "GIT.UserIdentity.DataShape",
            description = "The name and email recorded on a user's commits.",
            serviceResultName = "GIT.UserIdentity.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying a user's commit identity",
            payloadDescription = "Commit identity")
    public static final class UserIdentitySpec {
        @DapField(
                name = "GitUserName",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                description = "Name recorded on commits")
        private String gitUserName;

        @DapField(
                name = "GitUserEmail",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Email recorded on commits")
        private String gitUserEmail;
    }

    @DapDataShape(
            name = "GIT.Remote.DataShape",
            description = "A remote configured in the repository's .git/config.",
            serviceResultName = "GIT.Remote.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying configured Git remotes",
            payloadDescription = "Configured Git remotes")
    public static final class RemoteSpec {
        @DapField(
                name = "Name",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                description = "Remote name")
        private String name;

        @DapField(
                name = "URLs",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Fetch URLs")
        private String urls;

        @DapField(
                name = "PushURLs",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description = "Push URLs")
        private String pushUrls;

        @DapField(
                name = "FetchSpecs",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "Fetch refspecs")
        private String fetchSpecs;
    }

    @DapDataShape(
            name = "GIT.TrackedProject.DataShape",
            description = "A ThingWorx project synchronized with a repository",
            serviceResultName = "GIT.TrackedProject.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying ThingWorx project names",
            payloadDescription = "ThingWorx project names")
    public static final class TrackedProjectSpec {
        @DapField(
                name = "ProjectName",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                primaryKey = true,
                description = "ThingWorx project name")
        private String projectName;

        @DapField(
                name = "Path",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description =
                        "Folder holding the project's entities, relative to the repository root; blank means <ProjectName> at the repository root")
        private String path;

        @DapField(
                name = "EntityTypes",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description =
                        "Entity types synchronized, as comma-separated collections such as Things,Mashups; blank means all types")
        private String entityTypes;
    }

    @DapDataShape(
            name = "GIT.EntityTypeOption.DataShape",
            description = "An entity type a tracked project can synchronize, for a dropdown",
            serviceResultName = "GIT.EntityTypeOption.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying entity type options",
            payloadDescription = "Entity type options")
    public static final class EntityTypeOptionSpec {
        @DapField(
                name = "EntityType",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                primaryKey = true,
                description = "Collection and export folder name, such as ThingTemplates")
        private String entityType;

        @DapField(
                name = "Label",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Readable name, such as Thing Templates")
        private String label;
    }

    @DapDataShape(
            name = "GIT.GpgKeySummary.DataShape",
            description = "Public metadata for a user's GPG key",
            serviceResultName = "GIT.GpgKeySummary.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying GPG key metadata",
            payloadDescription = "GPG key metadata")
    public static final class GpgKeySummarySpec {
        @DapField(
                name = "GpgKeyFingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                primaryKey = true)
        private String fingerprint;

        @DapField(name = "GpgKeyLabel", baseType = DapBaseType.STRING, ordinal = 2)
        private String label;
    }

    @DapDataShape(
            name = "GIT.SshKey.UserExtension.DataShape",
            description =
                    "SSH keys owned by a user and reusable across repositories; private keys are stored encrypted",
            generateServiceResult = false)
    public static final class SshKeySpec {
        @DapField(
                name = "SshKeyFingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                primaryKey = true,
                description = "SHA256 fingerprint of the public key")
        private String sshKeyFingerprint;

        @DapField(
                name = "SshPrivateKey",
                baseType = DapBaseType.PASSWORD,
                ordinal = 2,
                description = "OpenSSH private key")
        private String sshPrivateKey;

        @DapField(
                name = "SshKeyPassphrase",
                baseType = DapBaseType.PASSWORD,
                ordinal = 3,
                description = "Passphrase of the private key, if it has one")
        private String sshKeyPassphrase;

        @DapField(
                name = "SshPublicKey",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "Public key in authorized_keys format")
        private String sshPublicKey;

        @DapField(
                name = "SshKeyLabel",
                baseType = DapBaseType.STRING,
                ordinal = 5,
                description = "Human-readable label")
        private String sshKeyLabel;
    }

    @DapDataShape(
            name = "GIT.SshKeySummary.DataShape",
            description = "Public details of a user's SSH key",
            serviceResultName = "GIT.SshKeySummary.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying SSH key public details",
            payloadDescription = "SSH key public details")
    public static final class SshKeySummarySpec {
        @DapField(
                name = "SshKeyFingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                primaryKey = true)
        private String sshKeyFingerprint;

        @DapField(name = "SshKeyLabel", baseType = DapBaseType.STRING, ordinal = 2)
        private String sshKeyLabel;

        @DapField(
                name = "SshPublicKey",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description = "Paste this into your account's SSH keys on the Git server")
        private String sshPublicKey;
    }

    @DapDataShape(
            name = "GIT.GpgKey.UserExtension.DataShape",
            description = "Encrypted GPG keys owned by a user and reusable across repositories",
            serviceResultName = "GIT.GpgKey.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying GPG keys.",
            payloadDescription = "GPG key payload")
    public static final class GpgKeySpec {
        @DapField(
                name = "GpgKeyFingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                primaryKey = true,
                description =
                        "OpenPGP key fingerprint used as the stable Git signing key identifier")
        private String gpgKeyFingerprint;

        @DapField(
                name = "GpgPrivateKey",
                baseType = DapBaseType.PASSWORD,
                ordinal = 2,
                description = "ASCII-armored PGP private key")
        private String gpgPrivateKey;

        @DapField(
                name = "GpgKeyPassphrase",
                baseType = DapBaseType.PASSWORD,
                ordinal = 3,
                description = "PGP private-key passphrase")
        private String gpgKeyPassphrase;

        @DapField(
                name = "GpgKeyLabel",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description =
                        "Optional human-readable label used to look up the key instead of the fingerprint")
        private String gpgKeyLabel;
    }

    @DapDataShape(
            name = "GIT.GpgKeyVerificationResult.DataShape",
            description = "Result returned when validating a GPG signing key",
            serviceResultName = "GIT.GpgKeyVerification.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying GPG-key verification.",
            payloadDescription = "Verification payload")
    public static final class GpgKeyVerificationSpec {
        @DapField(
                name = "GpgKeyFingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                description = "OpenPGP key fingerprint or verification error")
        private String gpgKeyFingerprint;

        @DapField(
                name = "Valid",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 2,
                description = "True when the key material can be loaded and used for signing")
        private Boolean valid;

        @DapField(
                name = "GpgKeyLabel",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description =
                        "Optional human-readable label for this key; Not in User Keys when the key is valid but not owned")
        private String gpgKeyLabel;

        @DapField(
                name = "Stored",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 4,
                description = "True when the key fingerprint is owned by the current user")
        private Boolean stored;
    }

    @DapDataShape(
            name = "GIT.RepositoryConfiguration.UserExtension.DataShape",
            description =
                    "Persistent Git identity, credentials, and signing settings for a user and repository",
            serviceResultName = "GIT.RepositoryConfiguration.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying repository credentials.",
            payloadDescription = "Credential payload")
    public static final class RepositoryConfigurationSpec {
        @DapField(
                name = "GitThing",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                primaryKey = true,
                description = "GIT Repository Thing for which these settings apply")
        private String gitThing;

        @DapField(
                name = "GitCommitterUser",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Git username used for authentication")
        private String gitCommitterUser;

        @DapField(
                name = "GitCommitterPassword",
                baseType = DapBaseType.PASSWORD,
                ordinal = 3,
                description = "Git password or personal access token")
        private String gitCommitterPassword;

        @DapField(
                name = "GitCommitterEmail",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "Committer email address")
        private String gitCommitterEmail;

        @DapField(
                name = "GitCommitterFullName",
                baseType = DapBaseType.STRING,
                ordinal = 5,
                description = "Committer full name")
        private String gitCommitterFullName;

        @DapField(
                name = "GpgKeyFingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 6,
                description =
                        "Fingerprint of the user's GPG key used for this repository; signing is enabled when this field is non-empty")
        private String gpgKeyFingerprint;

        @DapField(
                name = "SshKeyFingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 7,
                description = "Fingerprint of the user's SSH key used for SSH remotes")
        private String sshKeyFingerprint;

        @DapField(
                name = "ApprovedRemoteOrigins",
                baseType = DapBaseType.STRING,
                ordinal = 8,
                description =
                        "Newline-separated remote origins approved for these credentials; a changed remote URL requires saving the settings again")
        private String approvedRemoteOrigins;

        @DapField(
                name = "SigningFormat",
                baseType = DapBaseType.STRING,
                ordinal = 9,
                description =
                        "Commit signing format: openpgp, ssh, or blank (OpenPGP when GpgKeyFingerprint is set, otherwise unsigned)")
        private String signingFormat;

        @DapField(
                name = "SigningSshKeyFingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 10,
                description =
                        "Fingerprint of the user's SSH key that signs commits when SigningFormat is ssh")
        private String signingSshKeyFingerprint;
    }

    @DapDataShape(
            name = "GIT.RepositorySigning.DataShape",
            description = "The current user's commit signing choice for one repository",
            serviceResultName = "GIT.RepositorySigning.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying the commit signing choice.",
            payloadDescription = "Commit signing choice")
    public static final class RepositorySigningSpec {
        @DapField(
                name = "SigningFormat",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                description = "openpgp or ssh; blank when commits are not signed")
        private String signingFormat;

        @DapField(
                name = "KeyFingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Fingerprint of the signing key; blank when commits are not signed")
        private String keyFingerprint;

        @DapField(
                name = "GpgKeyFingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description = "Saved OpenPGP key, kept while another format is selected")
        private String gpgKeyFingerprint;

        @DapField(
                name = "SigningSshKeyFingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "Saved SSH signing key, kept while another format is selected")
        private String signingSshKeyFingerprint;

        @DapField(
                name = "RequireSigning",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 5,
                description = "Whether this repository refuses unsigned commits")
        private Boolean requireSigning;

        @DapField(
                name = "CanChangeRequireSigning",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 6,
                description =
                        "Whether the current user may change RequireSigning (design-time Update permission)")
        private Boolean canChangeRequireSigning;
    }

    @DapDataShape(
            name = "GIT.SigningKeyOption.DataShape",
            description = "A key the current user can sign commits with, for a dropdown",
            serviceResultName = "GIT.SigningKeyOption.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying signing key options.",
            payloadDescription = "Signing key options")
    public static final class SigningKeyOptionSpec {
        @DapField(
                name = "Value",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                description =
                        "Key reference for SigningKey parameters: openpgp:<fingerprint>, ssh:<fingerprint>, or none")
        private String value;

        @DapField(
                name = "Display",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Format, label and fingerprint, for display")
        private String display;

        @DapField(
                name = "Format",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description = "openpgp, ssh, or blank for None")
        private String format;

        @DapField(name = "Fingerprint", baseType = DapBaseType.STRING, ordinal = 4)
        private String fingerprint;

        @DapField(
                name = "IsRepositoryDefault",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 5,
                description = "The key saved for this repository; listed first")
        private Boolean isRepositoryDefault;

        @DapField(
                name = "IsSignInKey",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 6,
                description = "Also used to sign in to one of this repository's remotes")
        private Boolean isSignInKey;
    }

    @DapDataShape(
            name = "GIT.RemoteCredentials.UserExtension.DataShape",
            description =
                    "A user's sign-in for one remote of one repository; the password is stored encrypted",
            generateServiceResult = false)
    public static final class RemoteCredentialsSpec {
        @DapField(
                name = "GitThing",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                description = "GIT Repository Thing the remote belongs to")
        private String gitThing;

        @DapField(
                name = "Remote",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Remote name, such as origin")
        private String remote;

        @DapField(
                name = "GitCommitterUser",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description = "Git username for http(s) URLs of this remote")
        private String gitCommitterUser;

        @DapField(
                name = "GitCommitterPassword",
                baseType = DapBaseType.PASSWORD,
                ordinal = 4,
                description = "Password or access token for http(s) URLs of this remote")
        private String gitCommitterPassword;

        @DapField(
                name = "SshKeyFingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 5,
                description =
                        "Fingerprint of the user's SSH key used for ssh:// URLs of this remote")
        private String sshKeyFingerprint;

        @DapField(
                name = "ApprovedRemoteOrigins",
                baseType = DapBaseType.STRING,
                ordinal = 6,
                description =
                        "Newline-separated origins of this remote when the sign-in was saved; the password is only sent to these")
        private String approvedRemoteOrigins;
    }

    @DapDataShape(
            name = "GIT.UpgradeStep.DataShape",
            description = "One change the upgrade manager made, or chose not to make",
            serviceResultName = "GIT.UpgradeStep.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying the upgrade report.",
            payloadDescription = "Upgrade report")
    public static final class UpgradeStepSpec {
        @DapField(
                name = "Scope",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                description = "system, repository or user")
        private String scope;

        @DapField(
                name = "Name",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Repository Thing or user name")
        private String name;

        @DapField(
                name = "Item",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description = "The setting, repository or remote concerned")
        private String item;

        @DapField(
                name = "Action",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "What was done, such as moved, copied, removed or kept")
        private String action;

        @DapField(
                name = "Detail",
                baseType = DapBaseType.STRING,
                ordinal = 5,
                description = "Why, or what to do next")
        private String detail;
    }

    @DapDataShape(
            name = "GIT.RemoteSignIn.DataShape",
            description =
                    "The current user's sign-in for one remote, without the password. Inherited is true when the remote has no sign-in of its own and uses the repository-wide one.",
            serviceResultName = "GIT.RemoteSignIn.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying the sign-in of each remote.",
            payloadDescription = "Remote sign-in")
    public static final class RemoteSignInSpec {
        @DapField(
                name = "Remote",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                primaryKey = true,
                description = "Remote name")
        private String remote;

        @DapField(
                name = "GitCommitterUser",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Git username")
        private String gitCommitterUser;

        @DapField(
                name = "HasPassword",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 3,
                description = "True when a password or token is saved")
        private Boolean hasPassword;

        @DapField(
                name = "SshKeyFingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "Selected SSH key")
        private String sshKeyFingerprint;

        @DapField(
                name = "Inherited",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 5,
                description =
                        "True when these values come from the repository-wide sign-in saved by earlier versions")
        private Boolean inherited;
    }

    @DapDataShape(
            name = "GIT.SshHostKey.DataShape",
            description = "The SSH host key a repository remote currently presents",
            serviceResultName = "GIT.SshHostKey.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying an SSH host key fingerprint.",
            payloadDescription = "SSH host key")
    public static final class SshHostKeySpec {
        @DapField(
                name = "Remote",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                description = "Remote name the host key was probed from")
        private String remote;

        @DapField(
                name = "Host",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "SSH server host name")
        private String host;

        @DapField(
                name = "Port",
                baseType = DapBaseType.INTEGER,
                ordinal = 3,
                description = "SSH server port")
        private Integer port;

        @DapField(
                name = "Fingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "SHA-256 fingerprint of the server host key")
        private String fingerprint;

        @DapField(
                name = "PublicKey",
                baseType = DapBaseType.STRING,
                ordinal = 5,
                description = "OpenSSH-encoded server host public key")
        private String publicKey;
    }

    @DapDataShape(
            name = "GIT.RemoteStatus.DataShape",
            description =
                    "Which setup steps a remote still needs before the current user can pull and push. Step fields are ok, missing or n/a.",
            serviceResultName = "GIT.RemoteStatus.ServiceResult.DataShape",
            serviceResultDescription = "Service result carrying the setup status of each remote.",
            payloadDescription = "Remote setup status")
    public static final class RemoteStatusSpec {
        @DapField(
                name = "Remote",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                primaryKey = true,
                description = "Remote name")
        private String remote;

        @DapField(
                name = "URL",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Fetch and push URLs, without any embedded password")
        private String url;

        @DapField(
                name = "Transport",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description = "https, http or ssh")
        private String transport;

        @DapField(
                name = "IsDefault",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 4,
                description = "True for the remote Pull, Push and Fetch use when none is named")
        private Boolean isDefault;

        @DapField(
                name = "Network",
                baseType = DapBaseType.STRING,
                ordinal = 5,
                description = "Whether the egress policy allows the server's address")
        private String network;

        @DapField(
                name = "SignIn",
                baseType = DapBaseType.STRING,
                ordinal = 6,
                description = "Whether the current user saved a password or token, or SSH key")
        private String signIn;

        @DapField(
                name = "CredentialOrigin",
                baseType = DapBaseType.STRING,
                ordinal = 7,
                description = "Whether the saved password is approved for this remote's server")
        private String credentialOrigin;

        @DapField(
                name = "HostKey",
                baseType = DapBaseType.STRING,
                ordinal = 8,
                description = "Whether an SSH host key is approved for this remote's server")
        private String hostKey;

        @DapField(
                name = "Ready",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 9,
                description = "True when every step is done")
        private Boolean ready;

        @DapField(
                name = "NextStep",
                baseType = DapBaseType.STRING,
                ordinal = 10,
                description = "What to do next, in a sentence")
        private String nextStep;
    }

    @DapDataShape(
            name = "GIT.RepositoryConfigurationSummary.DataShape",
            description =
                    "A user's repository settings without secrets; reports whether a password is stored",
            serviceResultName = "GIT.RepositoryConfigurationSummary.ServiceResult.DataShape",
            serviceResultDescription =
                    "Service result carrying repository settings without secrets.",
            payloadDescription = "Repository settings")
    public static final class RepositoryConfigurationSummarySpec {
        @DapField(
                name = "GitThing",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                primaryKey = true,
                description = "GIT Repository Thing for which these settings apply")
        private String gitThing;

        @DapField(
                name = "GitCommitterUser",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Git username used for authentication")
        private String gitCommitterUser;

        @DapField(
                name = "HasPassword",
                baseType = DapBaseType.BOOLEAN,
                ordinal = 3,
                description = "True when a password or token is stored")
        private Boolean hasPassword;

        @DapField(
                name = "GitCommitterEmail",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "Committer email address")
        private String gitCommitterEmail;

        @DapField(
                name = "GitCommitterFullName",
                baseType = DapBaseType.STRING,
                ordinal = 5,
                description = "Committer full name")
        private String gitCommitterFullName;

        @DapField(
                name = "GpgKeyFingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 6,
                description = "Fingerprint of the GPG key used for this repository")
        private String gpgKeyFingerprint;

        @DapField(
                name = "SshKeyFingerprint",
                baseType = DapBaseType.STRING,
                ordinal = 7,
                description = "Fingerprint of the SSH key used for SSH remotes")
        private String sshKeyFingerprint;

        @DapField(
                name = "ApprovedRemoteOrigins",
                baseType = DapBaseType.STRING,
                ordinal = 8,
                description = "Remote origins approved for the saved credentials")
        private String approvedRemoteOrigins;
    }

    @DapDataShape(
            name = "GIT.OperationList.DataShape",
            description = "A Git operation tab descriptor.",
            serviceResultDescription = "Service result carrying operation tab rows.",
            payloadDescription = "Operation list payload")
    public static final class OperationListSpec {
        @DapField(
                name = "Value",
                baseType = DapBaseType.STRING,
                ordinal = 1,
                description = "Operation key matching the mashup tab value")
        private String value;

        @DapField(
                name = "Description",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Display label for the operation")
        private String description;

        @DapField(
                name = "Ordinal",
                baseType = DapBaseType.INTEGER,
                ordinal = 3,
                description = "Display order")
        private Integer ordinal;
    }

    @DapDataShape(
            name = "GIT.GitLog.DataShape",
            description = "A Git action log entry.",
            serviceResultDescription = "Service result carrying Git log rows.",
            payloadDescription = "Git log payload")
    public static final class GitLogSpec {
        @DapField(
                name = "Timestamp",
                baseType = DapBaseType.DATETIME,
                ordinal = 1,
                description = "Log entry timestamp")
        private Object timestamp;

        @DapField(
                name = "User",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "User that executed the action")
        private String user;

        @DapField(
                name = "Action",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description = "Git service that was executed")
        private String action;

        @DapField(
                name = "RepoName",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "Repository Thing the action was executed against")
        private String repoName;

        @DapField(
                name = "Level",
                baseType = DapBaseType.STRING,
                ordinal = 5,
                description = "Log level of the entry")
        private String level;

        @DapField(
                name = "Message",
                baseType = DapBaseType.STRING,
                ordinal = 6,
                description = "Log message")
        private String message;
    }

    @DapDataShape(
            name = "GIT.ExtensionLog.DataShape",
            description = "An entry the Git extension wrote to the ApplicationLog.",
            serviceResultDescription = "Service result carrying extension log rows.",
            payloadDescription = "Extension log payload")
    public static final class ExtensionLogSpec {
        @DapField(
                name = "Timestamp",
                baseType = DapBaseType.DATETIME,
                ordinal = 1,
                description = "Log entry timestamp")
        private Object timestamp;

        @DapField(
                name = "Level",
                baseType = DapBaseType.STRING,
                ordinal = 2,
                description = "Log level, such as WARN or ERROR")
        private String level;

        @DapField(
                name = "Source",
                baseType = DapBaseType.STRING,
                ordinal = 3,
                description = "Extension class and exception stack trace when available")
        private String source;

        @DapField(
                name = "Message",
                baseType = DapBaseType.STRING,
                ordinal = 4,
                description = "Log message")
        private String message;

        @DapField(
                name = "User",
                baseType = DapBaseType.STRING,
                ordinal = 5,
                description = "User whose request wrote the entry")
        private String user;

        @DapField(
                name = "Thread",
                baseType = DapBaseType.STRING,
                ordinal = 6,
                description = "Server thread that wrote the entry")
        private String thread;

        @DapField(
                name = "Origin",
                baseType = DapBaseType.STRING,
                ordinal = 7,
                description = "ApplicationLog origin, the abbreviated logger name")
        private String origin;
    }

    @DapProject(name = "GIT", packageVersion = "1.0.0", editable = true)
    public static final class ProjectSpec {}

    @DapProject(
            name = Const.RepositoryProjectName,
            description = "Repository Things created by the JGit extension",
            packageVersion = "1.0.0",
            editable = true)
    public static final class RepositoriesProjectSpec {}

    @DapThingTemplate(
            name = "GIT.Repository.ThingTemplate",
            baseThingTemplate = "FileRepository",
            description =
                    "Base template for GIT Repository Things providing core Git operations (commit, push, pull, branch, tag, etc.).",
            implementedShapes = {"GIT.Repository.ThingShape"})
    public static final class RepositoryThingTemplateSpec {}

    @DapThing(
            name = "GIT.Utility.Thing",
            thingTemplate = "GenericThing",
            description =
                    "Contains services required for working with the GIT Repository things, but which don't make sense to be present at the Git thing level.",
            effectiveThingPackage = "GIT.Utility.ThingPackage",
            implementedShapes = {"GIT.Utility.ThingShape"},
            subscriptions = {
                @DapSubscription(
                        name = "InitializeUserExtensionsOnStartup",
                        eventName = "ThingStart",
                        script =
                                "try { me.InitUserExtensionProperties(); } catch(e) { logger.warn(\"InitializeUserExtensionsOnStartup: InitUserExtensionProperties failed: \" + e); } try { me.RunUpgrade(); } catch(e) { logger.warn(\"InitializeUserExtensionsOnStartup: RunUpgrade failed: \" + e); }")
            })
    public static final class UtilityThingSpec {}

    @DapExtensionPackage(
            name = "JGitExtension",
            description =
                    "This extension provides various GIT capabilities that allow storing ThingWorx applications in a third party Git server.",
            vendor = "US Ignite / Ryan Guild",
            migratorClass = "org.us_ignite.thingworx.jgit.extension.ExtMigrator",
            thingPackages = {
                @DapThingPackage(
                        name = "GIT.Utility.ThingPackage",
                        className = "org.us_ignite.thingworx.jgit.extension.GitUtilityThingShape")
            },
            localizationTokens = {
                @DapLocalizationToken(name = "Commit Name", value = "Commit Username"),
                @DapLocalizationToken(name = "Commit Email", value = "Commit Email"),
                @DapLocalizationToken(name = "Git Repo URL", value = "Git Repo URL"),
                @DapLocalizationToken(
                        name = "File Repository Path",
                        value = "File Repository Path"),
                @DapLocalizationToken(name = "Configuration", value = "Configuration"),
                @DapLocalizationToken(name = "Initial branch", value = "Initial branch"),
                @DapLocalizationToken(name = "Use Proxy?", value = "Use Proxy?"),
                @DapLocalizationToken(name = "Proxy URL", value = "Proxy URL"),
                @DapLocalizationToken(name = "Proxy Port", value = "Proxy Port"),
                @DapLocalizationToken(
                        name = "Localization Tokens Prefix",
                        value = "Localization Tokens Prefix")
            })
    public static final class ExtensionSpec {}
}
