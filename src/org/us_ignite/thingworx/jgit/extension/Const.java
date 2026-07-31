package org.us_ignite.thingworx.jgit.extension;

public final class Const {
    // GIT Configuration names
    public static final String str_ConfTableName = "Configuration";
    public static final String str_GitRepoURL = "GitRepoURL";
    public static final String str_FileRepository = "FileRepository";
    public static final String str_RepoPathName = "RepoPathName";
    public static final String str_InitialBranch = "BranchName";
    public static final String str_DefaultProjectToExport = "DefaultExportProject";
    public static final String str_LocalizationTokensPrefix = "LocalizationTokensPrefix";
    public static final String str_ProjectName = "ProjectName";
    public static final String str_GitRepoURLDefaultValue = "";
    public static final String str_FileRepositoryDefaultValue = "GitRepository";

    // PlatformInfo property Names

    // Entity names
    public static final String str_CommitListDataShapeName = "GIT.CommitList.DataShape";
    public static final String str_CommitLogDataShapeName = "GIT.CommitLog.DataShape";
    public static final String str_ReflogEntryDataShapeName = "GIT.ReflogEntry.DataShape";
    public static final String str_CommitInfoDataShapeName = "GIT.CommitInfo.DataShape";
    public static final String str_CommitChangedFiles = "GIT.CommitChangedFiles.DataShape";
    public static final String str_UtilityThingName = "GIT.Utility.Thing";
    public static final String str_RepositoryThingTemplateName = "GIT.Repository.ThingTemplate";

    // Proxy related settings
    public static final String str_UseProxy = "UseProxy";
    public static final String str_ProxyURL = "ProxyURL";
    public static final String str_ProxyPort = "ProxyPort";

    // Various
    public static final String str_MaxDiffSize = "MaxDiffSize";
    public static final String str_GitCommitterUser = "GitCommitterUser";
    public static final String str_GitCommitterPassword = "GitCommitterPassword";
    public static final String str_GitCommitterName = "GitCommitterFullName";
    public static final String str_GitCommitterEmail = "GitCommitterEmail";
    public static final String str_GitCredentials = "UserRepositoryConfiguration";

    // GPG Key related constants
    public static final String str_UserGpgKeys = "UserGpgKeys";
    public static final String str_GpgPrivateKey = "GpgPrivateKey";
    public static final String str_GpgKeyPassphrase = "GpgKeyPassphrase";
    public static final String str_SignCommits = "SignCommits";
    public static final String str_GpgKeyFingerprint = "GpgKeyFingerprint";
    public static final String str_GpgKeyDataShapeName = "GIT.GpgKey.UserExtension.DataShape";
    public static final String str_GitCredentialsDataShapeName = "GIT.RepositoryConfiguration.UserExtension.DataShape";
    public static final String str_UserGpgKeyDataShapeName = "GIT.GpgKey.UserExtension.DataShape";
    public static final String str_GpgKeyVerificationResultDataShapeName = "GIT.GpgKeyVerificationResult.DataShape";

    // Error message constants
    public static final String ERR_PREFIX_CONFIG = "[CONFIG] ";
    public static final String ERR_PREFIX_AUTH = "[AUTH] ";
    public static final String ERR_PREFIX_NETWORK = "[NETWORK] ";
    public static final String ERR_PREFIX_GIT = "[GIT] ";
    public static final String ERR_PREFIX_SYSTEM = "[SYSTEM] ";

    public static final String ERR_NO_REPO_URL =
            "No Git repository URL configured. Set 'GitRepoURL' in the Configuration table and restart the Thing.";
    public static final String ERR_NO_REPO_URL_PUSH =
            "No Git repository URL configured for this thing. Set 'GitRepoURL' in the Configuration table.";
    public static final String ERR_NO_CREDENTIALS =
            "Missing Git credentials (username/password). Open the extension settings and configure credentials for this repository.";
    public static final String ERR_NO_COMMITTER =
            "Missing committer identity (name/email). Configure 'GitCommitterFullName' and 'GitCommitterEmail' in the extension settings.";
    public static final String ERR_PROJECT_NOT_FOUND =
            "Project '%s' not found. Verify the project name exists in ThingWorx.";
    public static final String ERR_PROJECT_NAME_REQUIRED =
            "ProjectName is required and was not provided.";
    public static final String ERR_GIT_THING_NAME_REQUIRED =
            "GitThingName is required and was not provided.";
    public static final String ERR_BRANCH_NOT_FOUND =
            "Branch '%s' not found. Verify the branch name exists locally. Use 'Pull' to fetch remote branches first.";
    public static final String ERR_UPSTREAM_NOT_FOUND =
            "Upstream '%s' not found. Verify the branch or commit reference exists.";
    public static final String ERR_COMMIT_NOT_FOUND =
            "Commit '%s' not found. Verify the commit hash is correct.";
    public static final String ERR_NO_TAG_NAME = "No tag name provided. Skipping tag creation.";
    public static final String ERR_NO_SCF_RESOURCE =
            "SourceControlFunctions resource not found. Cannot export entities.";
    public static final String ERR_COULD_NOT_RESOLVE_THING =
            "Could not resolve calling GitThing. Ensure the service is invoked on a GIT Repository Thing.";
    public static final String ERR_DIFF_TOO_LARGE =
            "Diff size exceeds the configured maximum (%d bytes). Increase 'MaxDiffSize' in the thing properties to view this diff.";
    public static final String ERR_FILE_REPO_NOT_FOUND =
            "FileRepository '%s' not found. Verify the FileRepository thing exists.";
    public static final String ERR_NO_PROJECT_CONFIGURED =
            "No project configured for this repository. Set 'ProjectName' in the Configuration table. Sync skipped.";
    public static final String ERR_AUTH_FAILED =
            "Authentication failed for repository URL '%s'. Verify your credentials and repository access.";
    public static final String ERR_NETWORK_FAILED =
            "Network error connecting to '%s'. Check network connectivity, proxy settings, and repository URL.";
    public static final String ERR_MERGE_CONFLICT =
            "Merge conflict detected between branches. Resolve conflicts manually, then commit the result.";
    public static final String ERR_PUSH_NON_FAST_FORWARD =
            "Push rejected: remote has commits you don't have locally. Pull or rebase the remote branch before pushing again.";
    public static final String ERR_PUSH_SIGNING_REQUIRED =
            " Push rejected. The remote may require GPG-signed commits. Configure a GPG key via 'SetGpgKey' with 'SignCommits=true', then retry.";
    public static final String ERR_REBASE_CONFLICT =
            "Rebase conflict detected. Resolve conflicts, then use 'git rebase --continue' or 'Rebase' again after resolving.";

    // Warning message constants
    public static final String WARN_NO_GPG_KEYS =
            "No GpgKeys UserExtension property found. GPG signing will be skipped. Run 'InitUserExtensionGpgKeysProperty' to initialize.";
    public static final String WARN_NO_SIGN_COMMITS =
            "SignCommits is not enabled for this repository. If the remote requires signed commits, the push will be rejected.";
    public static final String WARN_SYNC_FAILED =
            "Project sync from ThingWorx to repository failed: %s. The git operation will continue but the working tree may be out of date.";
    public static final String WARN_GIT_CREDENTIALS_NOT_FOUND =
            "No stored GitCredentials found for this repository. Configure credentials in the extension settings.";
    public static final String WARN_DETACHED_HEAD =
            "Repository is in detached HEAD state. Some operations may behave unexpectedly. Checkout a branch to normalize.";
    public static final String WARN_NO_PROJECT_SKIP =
            "No ProjectName configured. Skipping ThingWorx entity sync for this operation.";
    public static final String WARN_GIT_THING_NAME_MISSING =
            "GitThingName is required but was not provided. Returning empty result.";

    // Success message templates
    public static final String SUCCESS_COMMIT = "Commit succeeded: %s";
    public static final String SUCCESS_TAG_CREATED = "Tag '%s' created at commit %s";
    public static final String SUCCESS_TAG_DELETED = "Tag '%s' deleted.";
    public static final String SUCCESS_BRANCH_CREATED = "Branch '%s' created from '%s': %s";
    public static final String SUCCESS_BRANCH_DELETED = "Branch '%s' deleted.";
    public static final String SUCCESS_GPG_VERIFIED =
            "GPG key verification succeeded. Fingerprint: %s";
    public static final String SUCCESS_GPG_FAILED = "GPG key verification failed. Fingerprint: %s";
    public static final String SUCCESS_PULL = "Pull %s: %s";
    public static final String SUCCESS_BULK_IMPORT =
            "Bulk import completed. Success: %d, Failed: %d, Total: %d";
}
