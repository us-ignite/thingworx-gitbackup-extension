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
    }

    @DapDataShape(name = "GIT.CommitChangedFiles.DataShape", generateServiceResult = false)
    public static final class CommitChangedFilesSpec {
        @DapField(name = "FileName", baseType = DapBaseType.STRING, ordinal = 1)
        private String fileName;

        @DapField(name = "Status", baseType = DapBaseType.STRING, ordinal = 2)
        private String status;
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
            name = "GIT.Remote.DataShape",
            description = "A configured local Git remote.",
            generateServiceResult = false)
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
    }

    @DapProject(name = "GIT", packageVersion = "1.0.0")
    public static final class ProjectSpec {}

    @DapProject(
            name = Const.RepositoryProjectName,
            description = "Repository Things created by the JGit extension",
            packageVersion = "1.0.0")
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
                                "try { me.InitUserExtensionProperties(); } catch(e) { logger.warn(\"InitializeUserExtensionsOnStartup: InitUserExtensionProperties failed: \" + e); }")
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
