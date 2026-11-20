# Configuration

Each repository Thing stores its connection and local-storage settings in persistent Thing properties.
`RepositoryCreate` accepts these repository settings and the current user's Git credentials in a
single call.

| Setting | Description |
|---|---|
| `BranchName` | Initial or default branch. The default is `main`. |
| `UseProxy`, `ProxyURL`, `ProxyPort` | Reserved. These are not applied to Git connections; to use a proxy, set the JVM proxy options (`-Dhttps.proxyHost`, `-Dhttps.proxyPort`) on the ThingWorx server. |
| `LocalizationTokensPrefix` | Optional prefix used during localization-token export. |
| `TrackedProjects` | ThingWorx projects whose entities are synchronized (`GIT.TrackedProject.DataShape`: `ProjectName`, `Path`). Each project is exported to and imported from its own folder. `Path` is the folder itself, relative to the repository root, for example `src/thingworx/app`; a blank `Path` means `<ProjectName>` at the repository root. Paths may not contain `..`, `.git`, backslashes or drive letters, and no two projects' folders may be the same or nested. |
| `RepoPath`, `ProjectName` | Retired. Earlier versions kept one project in `ProjectName` and the parent folder of project folders in `RepoPath`. The upgrade converts them into `TrackedProjects` and clears them; see [Upgrading from earlier versions](#upgrading-from-earlier-versions). |

`RepositoryCreate` creates a Thing based on the `GIT.Repository.ThingTemplate` template, which extends
`FileRepository` and implements the `GIT.Repository.ThingShape`. The template provides the
FileRepository working-tree storage and the Git services, and keeping repository Things on a common
template enables mashups to run dynamic services against any selected repository. The service then
configures the repository properties and creates the current user's credential record. Passwords,
tokens, and private keys are passed to the services as plain `STRING` parameters, so call them
over HTTPS. They are stored as ThingWorx `PASSWORD` values, which the platform encrypts at rest
with its encryption key. Services never return them:
repository settings report only whether a password is stored (`HasPassword`), and SSH keys report
only their fingerprint and public key.

Per-user settings include Git credentials, committer identity overrides, GPG keys, and the per-repository commit-signing preference. These values are configured for each user through the ThingWorx UI on the `UserExtensions` UserExtension properties and are stored as protected ThingWorx properties where applicable.

## Tracked Data Tables

Entities carry a Data Table's definition but not its rows. To keep configuration rows in Git as
well, track the table. A Data Table is tracked when its file is in the repository, in the folder of
the tracked project that owns the table:

```
<project folder>/Data/<DataTableName>.json
```

No Thing property lists tracked tables: the files are the list. A clone therefore brings its own
tracked tables and their modes. Deleting a file stops tracking its table.

```json
{
  "dataShape": "IOT.SICD.DataShape.PermissionPolicyEntry",
  "dataTable": "IOT.SICD.DataTable.PermissionPolicyEntries",
  "mode": "Mirror",
  "primaryKey": ["id"],
  "rows": [
    {"id": "...", "...": "..."}
  ]
}
```

The file holds only the fields of the table's DataShape. Rows are sorted by primary key, and keys by
name, so an unchanged table always produces the same bytes and a changed row changes only its own
lines. ThingWorx fills in `key`, `timestamp`, `location`, `source`, `sourceType` and `tags` on every
write, so those fields are left out.

`mode` sets how Sync to ThingWorx writes the rows. A file without a `mode` mirrors.

- `Mirror` adds or updates every row by primary key, and deletes the rows whose key is not in the
  file. The table ends up exactly as in Git.
- `Upsert` only adds and updates rows, and never deletes any.

How each sync treats the files:

- **Sync from ThingWorx** reads the table of every file after exporting the projects, then rewrites
  the files whose rows changed. The `mode` in each file is kept. A file whose Data Table does not
  exist on this server is kept as it is and reported in the extension log.
- **Sync to ThingWorx** imports the files that Git tracks, staged or committed. It runs *after* the
  entities, so the Data Table and its DataShape exist and are current before their rows are written.
  A file that is not added to Git yet is reported as untracked and is not imported.
- Writing rows does not change a Data Table's modified date. Each sync therefore records a hash of
  the table's rows in `.git/config` (`thingworxDataTable.<name>.rowsHash`). Rows changed in
  ThingWorx since then are reported, like changed entities, before Sync to ThingWorx overwrites
  them. The hash covers only the rows, so changing the mode does not trigger the warning.

| Service | What it does |
|---|---|
| `GetTrackedDataTables` | Lists the tracked tables, each with its `Mode`, `File` and `InGit`. `InGit` tells whether Git tracks the file. |
| `GetAvailableDataTables` | Lists the Data Tables in the tracked projects that have no file yet. |
| `AddTrackedDataTable` | Writes a table's file with `Mode` `Mirror` (default) or `Upsert`. Commit the file to share it. The table must have a primary key, and its project must be tracked. |
| `SetTrackedDataTableMode` | Changes the `mode` in a table's file. |
| `RemoveTrackedDataTable` | Deletes a table's file, leaving the deletion unstaged for review. The rows in ThingWorx are never changed. |

The caller needs `ServiceInvoke` on the table for `GetDataTableEntries` to export. To import, the
caller also needs `ServiceInvoke` for `AddOrUpdateDataTableEntries`, plus `DeleteDataTableEntries`
in Mirror mode.

## Commit identity

The name and email recorded on your commits are stored once per user, in the `GitUserName` and
`GitUserEmail` user extension properties, like Git's `user.name` and `user.email`. Set them under
Commit identity on the Settings page, or with `GitIdentitySave`. They apply to every repository.

The services that still accept `GitCommitterFullName` and `GitCommitterEmail`
(`RepositoryCreate`, `GitCredentialCreate`, `GitCredentialUpdate`, `GitRepositorySettingsSave`)
save non-blank values to the same user-level identity and ignore blank ones. Earlier versions
stored a name and email per repository; those are still used for that repository until you save a
user-level identity.

## Remotes

Remotes are stored only in the repository's own Git configuration (`.git/config`), exactly as the
`git remote` command stores them. They are shared by every user of the repository Thing.

| Service | What it does |
|---|---|
| `GetRemotes` | Lists the remotes, like `git remote -v`. |
| `RemoteAdd` | Adds a remote. Deleted remote branches are pruned on fetch. |
| `RemoteSetUrl` | Changes a remote's URL, or its separate push URL. |
| `RemoteRemove` | Removes a remote, its remote-tracking branches, and the upstream settings of branches that tracked it. |
| `TestRemote` | Connects with your sign-in for that remote and reports the branches and tags on offer. Nothing is fetched. |
| `RemoteSignInList` | Lists each remote with your sign-in for it: username, whether a password is saved, the SSH key, and whether it is inherited. Passwords are never returned. |
| `RemoteSignInSave` | Saves your username, password or token, and SSH key for one remote. |
| `RemoteSignInClear` | Deletes your own sign-in for one remote. |

### Sign-in for each remote

Each remote has its own sign-in for each user, stored in the `UserRemoteCredentials` user
extension property: a username and password or token for `https://` URLs, and an SSH key for
`ssh://` URLs. Two remotes on different servers can therefore use different tokens or keys. In
Repository Settings, select a remote under **Remotes and sign-in** to see its connection setup,
change its sign-in, and approve its SSH host key.

Earlier versions stored one sign-in per repository. The upgrade (below) copies it to each remote
and then removes it. Until then, and for a repository whose remotes are added later, a remote
without a sign-in of its own uses the repository-wide one (`GitRepositorySettingsSave`,
`SetRepositorySshKey`). Clearing a remote's sign-in falls back to it as well.
A password is only sent to the origins the remote had when its sign-in was saved. Removing a
remote deletes your sign-in for it, so a new remote with the same name starts without one.

`RepositoryCreate` adds the URL it is given as `origin`. `Pull`, `Push`, `Fetch` and `TestRemote`
take an optional `Remote`; when it is blank they use the current branch's upstream remote, or
`origin` if the branch has none.

Remote URLs must be `https://`, `http://` or `ssh://` (including the `git@host:path` form), and
must not contain a password. HTTP(S) URLs must not embed a username. Local paths, `file://`,
`git://` and `ftp://` are refused, both when a remote is saved and again when it is used. Prefer
`https://` or `ssh://`. Plain `http://` is accepted for anonymous fetches, but saved credentials
are never sent over HTTP.

Connections are also checked against an egress policy at connect time. Loopback, link-local,
private-network, carrier-grade NAT and cloud metadata addresses are refused unless the destination
host is explicitly allowed. Set `THINGWORX_JGIT_NETWORK_ALLOWLIST` on the ThingWorx server to a
comma-separated list of host names or IP addresses that may be reached when they resolve to a
non-public address (for example `gitea` in the development stack). The equivalent Java system
property is `thingworx.jgit.network.allowlist`. The check runs on every connection, including
redirects, and re-resolves names at connect time.

Network operations have a bounded timeout. Set `thingworx.jgit.networkTimeoutSeconds` (default
`60`, maximum `3600`) when the default is unsuitable for a slow remote.

Earlier versions kept a single URL in the `GitRepoURL` Thing property and rewrote `origin` from it
on every operation. The first time such a repository is opened after upgrading, that URL is kept as
`origin` in `.git/config` and the property is cleared. The property is no longer read.

## Upgrading from earlier versions

The extension upgrades stored settings itself when it is imported and each time ThingWorx
starts. An administrator can also run `RunUpgrade` on `GIT.Utility.Thing`, which returns a report
of what changed. Every step is safe to repeat. The upgrade:

- opens every repository Thing, which moves a Git directory out of the FileRepository folder and
  the retired `GitRepoURL` property into the `origin` remote in `.git/config`;
- converts the retired `ProjectName` and `RepoPath` properties into the tracked projects table
  and clears them. Together they become one tracked project whose `Path` is
  `<RepoPath>/<ProjectName>`, the folder earlier versions exported it to, so no file moves. A
  repository that already had a tracked projects table keeps its rows, and each row without a
  `Path` gets `<RepoPath>/<ProjectName>` for the same reason. Opening a repository performs the
  same conversion, so a repository the upgrade missed is converted on first use. If the values
  cannot be converted, for example because the folders would overlap, they are left in place, the
  report says why, and synchronization is refused until they are cleared;
- stores each user's settings tables with their data shapes, so saved rows survive a reload;
- moves the commit name and email that were saved per repository to the user's own identity.
  A user whose repositories hold different names or emails keeps them until they choose one
  under Commit identity;
- copies each user's repository-wide sign-in to every remote of that repository, then removes
  the repository-wide copy. A remote gets the username and password only if it has an http(s)
  URL, and the SSH key only if it has an ssh URL. The password stays approved only for the URLs
  it was approved for before, so a remote whose URL changed since needs its sign-in saved again.
  Versions before approvals existed had a single remote and sent the password to its URL, so a
  sign-in saved by them is approved for `origin`, the remote that URL became. Other remotes need
  their sign-in saved before the password is sent to them.
  Remotes that already have their own sign-in keep it;
- deletes sign-in saved for remotes that no longer exist.

A repository that cannot be opened keeps its users' repository-wide sign-in, and the report says
so; the next run tries again. Repositories without remotes also keep it, so remotes added later
can use it.

## How credentials are used

Every network operation goes through a single JGit credentials provider backed by the current
user's settings. The stored password is decrypted only when a server asks for authentication and
is not kept after the operation. Nothing is installed JVM-wide, so one user's credentials are never
used for another user's operation.

The password is only sent to an origin the user approved when saving their credential: the remote
scheme, canonical host, effective port and repository path are recorded in the user's
`ApprovedRemoteOrigins` setting. If another user later changes the remote to a different server,
the next authentication fails instead of sending this user's password to the new destination. Open
Repository Settings and save the credential again to approve the new origin. If the server
redirects to a different host, port or scheme and the new server asks for sign-in, the operation
fails instead of handing over the password.

Passwords are never sent over plain HTTP. For isolated integration tests only, a stack may set
`THINGWORX_JGIT_ALLOW_INSECURE_CREDENTIALS=true` (or the Java property
`thingworx.jgit.allowInsecureCredentials`). Do not enable that override in a shared or production
deployment.

## What a repository's users can do to each other

A repository Thing is a shared resource, and these limits follow from that:

- Anyone who can run services on the repository Thing can change its remotes and can write files
  in the working tree. Git metadata is stored outside the FileRepository folder, so the inherited
  FileRepository services cannot read or write `.git/config` or `.git/ssh/known_hosts`.
  Credentials are bound to the origins a user approved, so changing a remote does not by itself
  redirect another user's saved password. Grant service access on a repository Thing only to
  people you would trust with its shared working tree and repository history, and use tokens
  scoped to that one Git repository.
- Git configuration can name programs to run: hooks, `core.hooksPath`, `filter.*.clean` and
  `filter.*.smudge` commands, and the commands of local-path remotes. The extension never runs
  any of them, so a user who can write to the repository cannot run commands on the ThingWorx
  server. Repositories that relied on Git hooks no longer run them.
- A user's stored secrets are in their own ThingWorx user settings. Other users cannot read them
  through this extension's services. ThingWorx administrators, and anyone allowed to read another
  user's properties and run server-side script, can decrypt `PASSWORD` values; that is a property
  of the platform's encryption, not something the extension can prevent.

## SSH remotes

SSH remotes (`ssh://git@host/owner/repo.git` or `git@host:owner/repo.git`) authenticate with a
per-user SSH key. In the UI:

1. Under **SSH keys** in Settings, generate a key or import an existing one, and copy its public key.
2. Add that public key to your account's SSH keys on the Git server.
3. In **Repository Settings**, select the remote under **Remotes and sign-in**, choose the key
   under **Sign-in for** the remote, and save.
4. In the same card, use **Check host key of selected**. Compare the
   fingerprint and key type in the dialog with the ones the Git server's operator gives you, then
   approve it.
5. Use **Test connection**.

The **Connection setup** section of the Remotes and sign-in card shows which of these steps are
done for the selected remote (or every remote, when none is selected), for the signed-in user. When a new repository is created with an SSH URL,
the **New Repository** page offers the host key approval straight after creating it, and asks for
confirmation first if no SSH key is selected or your commit identity is not set.

The equivalent services are `SshKeyGenerate` or `SshKeyImport`, `RemoteSignInSave`,
`SshHostKeyProbe`, `SshHostKeyApprove`, `TestRemote` and `GetConnectionStatus`.

The ThingWorx server's own `~/.ssh` directory is never read. SSH servers are not trusted on first
use. Before the first connection, call `SshHostKeyProbe` on the repository, compare the returned
SHA-256 fingerprint with the Git server operator out of band, then call `SshHostKeyApprove` with
that fingerprint. The approved host key is recorded in the repository's shared
`.git/ssh/known_hosts`. Connections use `StrictHostKeyChecking=yes`; an unapproved host or a key
that does not match the approved key is refused.

If the Git server is rebuilt and its key changes, call `ResetSshHostKeys` (which clears the shared
record), then probe and approve the new fingerprint again. Approving a different key while an old
key is still approved is refused.

Only public-key authentication is used for SSH. The HTTPS username and password are not sent to SSH
servers.

## Deleting a repository

**Delete repository…** at the bottom of Repository Settings deletes the repository for every user
after a confirmation (it calls `RepositoryDelete`). The working files and any uncommitted changes are
removed, and every user's settings and sign-ins for the repository are forgotten. The Git directory
is moved to `jgit/deleted/<name>-<timestamp>.git` without its remotes, so an administrator can still
recover commits that were never pushed. ThingWorx entities in the tracked projects are not changed.
The button needs design-time Delete permission on the repository Thing and permission to run
`EntityServices.DeleteThing`.

## Recommendations

Use repository-specific credentials with the minimum permissions needed for the required Git operations. Do not put passwords or private keys in repository URLs, source files, or exported documentation.

The related utility services, including `GitCredentialCreate`, `GitCredentialList`, `GpgKeyCreate`, and
`GpgKeyList`, are listed in the [service reference](service-reference.md).
