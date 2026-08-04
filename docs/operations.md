# Git operations

Repository Things expose services for the normal Git workflow, including status, commit, push, pull, fetch, checkout, branches, merge, rebase, tags, conflict resolution, and the two supported file-diff operations.

See the [service reference](service-reference.md) for the exact ThingWorx service names, argument
names, base types, and result shapes.

## Recommended workflow

1. Configure the remote URL, repository path, and initial branch. Each repository Thing is also
   its own FileRepository.
2. Validate credentials and network/proxy access.
3. Use status to inspect local changes.
4. Export the intended ThingWorx entities, which stages only the configured project tree, or call
   `Add` to stage a manual repository file. Use `Add(All=true)` to stage all non-ignored changes.
5. Commit with a meaningful message. `Commit` uses the existing index and leaves unstaged changes alone.
6. Push to the remote branch. `Push` exports the configured ThingWorx project before pushing and imports the FileRepository tree afterward. It uses the checked-out branch's upstream when available; pass `BranchName`, `RemoteBranchName`, and `SetUpstream` when publishing a new branch explicitly.

Use `BranchCreate` to create a branch without changing the working tree and `BranchSwitch` to move to a branch. Use `Checkout` for tags, commits, detached HEAD, or the combined create-and-checkout workflow. `BranchDelete` is local-only by default; remote deletion requires both `Remote` and `DeleteRemote=true`.

`ExportProjectEntities` updates and stages only the configured ThingWorx project tree; it does not
create a commit. Manual files must be staged with `Add`. `Remove` removes a path from the index
without deleting its FileRepository working-tree file. Call `Commit` explicitly, then call `Push`
when the commit should be sent to the configured remote.

Operations return service results or InfoTables according to the ThingWorx service definition. Operation failures are logged to `GIT.ExtensionLog.DataTable` with the service name, source, user, timestamp, and message.

When a repository is in a detached-head state, check out a branch before committing changes intended
for a remote branch. Resolve merge or rebase conflicts in the repository, then use `Add(File)` for
each resolved path before calling `MergeContinue` or `RebaseContinue`.
