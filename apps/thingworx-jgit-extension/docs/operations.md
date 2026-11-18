# Git operations

Repository Things expose services for the normal Git workflow, including status, commit, push, pull, fetch, checkout, branches, merge, rebase, tags, conflict resolution, and the two supported file-diff operations.

See the [service reference](service-reference.md) for the exact ThingWorx service names, argument
names, base types, and result shapes.

## Recommended workflow

1. Configure the remote URL, initial branch, and tracked projects with their folders. Each repository Thing is also
   its own FileRepository.
2. Validate credentials and network/proxy access.
3. Use `SyncFromThingWorx` to export the tracked projects into the working tree, then `Status` to
   inspect the changes.
4. Stage the intended files with `Add`, or use `Add(All=true)` to stage all non-ignored changes.
5. Commit with a meaningful message. `Commit` uses the existing index and leaves unstaged changes alone.
6. Push to the remote branch. `Push` only sends commits; it does not export or import ThingWorx entities. It uses the checked-out branch's upstream when available; pass `BranchName`, `RemoteBranchName`, and `SetUpstream` when publishing a new branch explicitly.

Use `BranchCreate` to create a branch without changing the working tree and `BranchSwitch` to move to a branch. Use `Checkout` for tags, commits, detached HEAD, or the combined create-and-checkout workflow. `BranchDelete` is local-only by default; remote deletion requires both `Remote` and `DeleteRemote=true`.

Git operations never move entities between ThingWorx and the repository. Use `SyncFromThingWorx` to
export the tracked projects before committing, and `SyncToThingWorx` to import the repository into
ThingWorx after `Pull`, `Checkout`, `Merge` or `Rebase`; see [Import and export](import-export.md).
Manual files must be staged with `Add`. `Remove` removes a path from the
index without deleting its FileRepository working-tree file. Call `Commit` explicitly, then call
`Push` when the commit should be sent to the configured remote.

Operations return service results or InfoTables according to the ThingWorx service definition. Operation failures are logged to `GIT.ExtensionLog.DataTable` with the service name, source, user, timestamp, and message.

When a repository is in a detached-head state, check out a branch before committing changes intended
for a remote branch. Resolve merge or rebase conflicts in the repository, then use `Add(File)` for
each resolved path before calling `MergeContinue` or `RebaseContinue`.
