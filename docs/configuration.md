# Configuration

Each repository Thing stores its connection and local-storage settings in persistent Thing properties.
`RepositoryCreate` accepts these repository settings and the current user's Git credentials in a
single call.

| Setting | Description |
|---|---|
| `GitRepoURL` | Remote Git repository URL. |
| `RepoPathName` | Directory within the repository Thing's FileRepository storage. |
| `BranchName` | Initial or default branch. The default is `main`. |
| `UseProxy` | Enables the configured HTTP proxy. |
| `ProxyURL` / `ProxyPort` | Proxy host and port when enabled. |
| `LocalizationTokensPrefix` | Optional prefix used during localization-token export. |
| `ProjectName` | Required ThingWorx project whose entities are synchronized. Repository sync operations use this project. |

`RepositoryCreate` creates a Thing based on the `GIT.Repository.ThingTemplate` template, which extends
`FileRepository` and implements the `GIT.Repository.ThingShape`. The template provides the
FileRepository working-tree storage and the Git services, and keeping repository Things on a common
template enables mashups to run dynamic services against any selected repository. The service then
configures the repository properties and creates the current user's credential record. The
credential service parameters are strings in the Java/XML service contract; sensitive values are
stored in protected user-property fields.

Per-user settings include Git credentials, committer identity overrides, GPG keys, and the per-repository commit-signing preference. These values are configured for each user through the ThingWorx UI on the `UserExtensions` UserExtension properties and are stored as protected ThingWorx properties where applicable.

Use repository-specific credentials with the minimum permissions needed for the required Git operations. Do not put passwords or private keys in repository URLs, source files, or exported documentation.

The related utility services, including `GitCredentialCreate`, `GitCredentialList`, `GpgKeyCreate`, and
`GpgKeyList`, are listed in the [service reference](service-reference.md).
