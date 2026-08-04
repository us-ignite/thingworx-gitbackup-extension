# Git operations

Repository Things expose services for the normal Git workflow, including status, commit, push, pull, fetch, checkout, branches, merge, rebase, tags, conflict resolution, and the two supported file-diff operations.

See the [service reference](service-reference.md) for the exact ThingWorx service names, argument
names, base types, and result shapes.

## Recommended workflow

1. Configure the remote URL, repository path, and initial branch. Each repository Thing is also
   its own FileRepository.
2. Validate credentials and network/proxy access.
3. Use status to inspect local changes.
4. Export the intended ThingWorx entities or stage repository edits.
5. Commit with a meaningful message.
6. Push to the remote branch.

`ExportProjectEntities` only updates the working tree; its compatibility `commitMessage` argument does not create a commit. Call `Commit` explicitly, then call `Push` when the commit should be sent to the configured remote.

Operations return service results or InfoTables according to the ThingWorx service definition. Operation failures are logged to `GIT.ExtensionLog.DataTable` with the service name, source, user, timestamp, and message.

When a repository is in a detached-head state, check out a branch before committing changes intended for a remote branch. Resolve merge or rebase conflicts in the repository before retrying subsequent operations.
