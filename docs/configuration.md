# Configuration

Each repository Thing stores its connection and local-storage settings in its `Configuration` table.

| Setting | Description |
|---|---|
| `GitRepoURL` | Remote Git repository URL. |
| `FileRepository` | ThingWorx FileRepository used for the local clone. |
| `RepoPathName` | Directory within the FileRepository. |
| `BranchName` | Initial or default branch. The default is `main`. |
| `UseProxy` | Enables the configured HTTP proxy. |
| `ProxyURL` / `ProxyPort` | Proxy host and port when enabled. |
| `LocalizationTokensPrefix` | Optional prefix used during localization-token export. |
| `ProjectName` | Optional ThingWorx project whose entities are synchronized. |

Per-user settings include Git credentials, committer identity overrides, GPG keys, and the per-repository commit-signing preference. These values are configured for each user through the ThingWorx UI on the `UserExtensions` UserExtension properties and are stored as protected ThingWorx properties where applicable.

Use repository-specific credentials with the minimum permissions needed for the required Git operations. Do not put passwords or private keys in repository URLs, source files, or exported documentation.

The related utility services, including `SetGitCredentials`, `SetGpgKey`, `GetGpgKeys`, and
`DeleteGpgKey`, are listed in the [service reference](service-reference.md).
