# ThingWorx service reference

The extension exposes services on two ThingWorx entities:

- `GIT.Repository.ThingShape` — Git repository operations.
- `GIT.Utility.Thing` — repository lifecycle, entity synchronization, and per-user configuration.

Arguments below use the ThingWorx service names and base types defined by the Java annotations.
`INFOTABLE` arguments use the DataShape shown in Composer for that service. Password values must
be entered through the current user’s `UserExtensions` properties and must not be logged.

## Repository services

| Service | Arguments | Result / purpose |
|---|---|---|
| `Commit` | `Message: STRING` | Returns a commit status message. |
| `Push` | `Remote: STRING` | Pushes the current branch. |
| `VerifyGpgKey` | `GpgPrivateKey: STRING`, `GpgKeyPassphrase: STRING` | Returns `GIT.GpgKeyVerificationResult`. |
| `Pull` | `Force: BOOLEAN` | Fetches and integrates remote changes. |
| `Fetch` | `Remote: STRING` | Fetches remote refs without integrating them. |
| `DeleteLocalRepoContent` | — | Deletes the local repository content. Use with care. |
| `CreateBranch` | `BranchName: STRING`, `StartPoint: STRING` | Creates a local branch. |
| `Checkout` | `BranchNameOrCommit: STRING` | Checks out a branch or commit. |
| `GetCurrentBranch` | — | Returns the current branch or detached-head state. |
| `GetBranchList` | — | Returns `GIT.BranchList`. |
| `DeleteLocalBranch` | `BranchName: STRING` | Deletes a local branch. |
| `GetCommitList` | — | Returns `GIT.CommitList`. |
| `GetLog` | `Ref: STRING`, `MaxEntries: INTEGER` | Returns `GIT.CommitLog`. |
| `GetReflog` | `Ref: STRING`, `MaxEntries: INTEGER` | Returns `GIT.ReflogEntry`. |
| `Status` | — | Returns `GIT.Status` for the working tree. |
| `GetConflictFiles` | — | Returns conflicted files using `GIT.Status`. |
| `ReadConflictFile` | `File: STRING` | Returns the content of a conflict file. |
| `WriteConflictFile` | `File: STRING`, `Content: STRING` | Writes resolved conflict content. |
| `StageResolved` | `FilePattern: STRING` | Stages resolved files. |
| `MergeContinue` | `Message: STRING` | Completes a merge after conflicts are resolved. |
| `MergeAbort` | — | Aborts the current merge. |
| `RebaseContinue` | — | Continues a rebase after conflicts are resolved. |
| `RebaseSkip` | — | Skips the current rebase commit. |
| `RebaseAbort` | — | Aborts the current rebase. |
| `GetDiffPerFile` | `File: STRING` | Returns the working-tree diff for one file. |
| `GetDiffPerFileBetweenCommits` | `File: STRING`, `FromCommitID: STRING` | Returns a file diff for the requested commit against its parent. |
| `GetCommitInfo` | `CommitID: STRING` | Returns `GIT.CommitInfo`. |
| `ExportProjectEntities` | `ProjectName: STRING`, `includeDependents: BOOLEAN`, `EntitiesToExport: INFOTABLE`, `commitMessage: STRING` | Exports entities and optionally commits them. |
| `ImportProjectEntities` | `entityPath: STRING`, `ignoreDependencies: BOOLEAN` | Imports repository entities and returns a summary. |
| `Merge` | `BranchName: STRING` | Merges a branch into the current branch. |
| `Rebase` | `UpstreamBranch: STRING` | Rebases onto an upstream branch. |
| `CreateTag` | `TagName: STRING`, `Message: STRING`, `CommitID: STRING` | Creates an annotated tag. |
| `GetTagList` | — | Returns `GIT.TagList`. |
| `DeleteTag` | `TagName: STRING` | Deletes a local tag. |

`Push`, `Pull`, and `Fetch` use the repository’s configured remote and the current user’s Git
credentials from `UserExtensions`. `VerifyGpgKey` is a validation service; normal signed commits
use the stored per-user GPG key configuration.

## Utility services

| Service | Arguments | Result / purpose |
|---|---|---|
| `AddEntitiesToExportList` | `existingEntities: INFOTABLE`, `newEntitiesToExport: INFOTABLE` | Merges export-list rows. |
| `AddLogEntry` | `Content: STRING`, `ServiceName: STRING`, `Source: STRING`, `timestamp: DATETIME`, `User: STRING` | Writes an extension log entry. |
| `AddNewRepo` | `RepoName: STRING`, `GitRepoURL: STRING`, `RepoPath: STRING`, `User: STRING`, `Password: STRING`, `CommitUser: STRING`, `CommitEmail: STRING`, `InitialBranch: STRING`, `UseProxy: BOOLEAN`, `ProxyURL: STRING`, `ProxyPort: INTEGER`, `LocalizationTokensPrefix: STRING`, `ProjectName: STRING` | Creates and configures a repository Thing. |
| `DeleteGitThing` | `RepoName: STRING` | Deletes a repository Thing and its stored user configuration. |
| `GetEmptyInfotable` | — | Returns an empty `SpotlightSearch` InfoTable. |
| `GetProjectEntities` | `project: STRING`, `entityName: STRING`, `entityType: STRING`, `includeDependents: BOOLEAN`, `tags: TAGS` | Returns project entities for export. |
| `ImportEntity` | `GitThingName: STRING`, `entityPath: STRING`, `FileRepositoryName: STRING`, `ignoreDependencies: BOOLEAN` | Imports one entity. |
| `InitUserExtensionProperties` | — | Creates missing Git UserExtension properties. |
| `InitUserExtensionGpgKeysProperty` | — | Creates the GPG UserExtension property. |
| `GetGpgKeys` | — | Returns the current user’s GPG key metadata. |
| `SetGpgKey` | `GitThing: THINGNAME`, `GpgPrivateKey: STRING`, `GpgKeyPassphrase: STRING`, `SignCommits: BOOLEAN`, `GpgKeyFingerprint: STRING` | Stores per-user GPG configuration. |
| `DeleteGpgKey` | `GitThing: THINGNAME` | Removes the user’s GPG configuration for a repository. |
| `Pause` | `delay: INTEGER` | Pauses service execution for the requested delay. |
| `RemoveEntitiesFromExportList` | `entitiesToRemove: INFOTABLE`, `existingEntities: INFOTABLE` | Removes rows from an export list. |
| `UpdateRepo` | `RepoName: STRING`, `GitRepoURL: STRING`, `RepoPath: STRING`, `User: STRING`, `Password: STRING`, `CommitUser: STRING`, `CommitEmail: STRING`, `InitialBranch: STRING`, `UseProxy: BOOLEAN`, `ProxyURL: STRING`, `ProxyPort: INTEGER`, `LocalizationTokensPrefix: STRING`, `ProjectName: STRING` | Updates repository configuration. |
| `ValidateGitThingName` | `GitThingName: STRING` | Validates a repository Thing name. |
| `SetGitCredentials` | `GitCommitterUser: STRING`, `GitCommitterPassword: STRING`, `GitCommitterEmail: STRING`, `GitCommitterFullName: STRING`, `GitThing: THINGNAME` | Stores per-user repository credentials and identity. |
| `SetProjectName` | `GitThingName: THINGNAME`, `ProjectName: STRING` | Sets the project associated with a repository. |
| `GetFilteredDirectoryListing` | — | Returns the filtered FileRepository directory listing. |
| `RemoveLastModifiedDate` | — | Removes imported last-modified metadata. |
| `RemoveModelPersistenceProviderPackage` | — | Removes the model-persistence-provider package entity when required during import. |

For the exact InfoTable fields and service annotations, see the [generated Java API reference](api/index.md).
