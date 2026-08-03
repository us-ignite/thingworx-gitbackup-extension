# Git operations

Repository Things expose services for the normal Git workflow, including initialization, status, commit, push, pull, fetch, checkout, branches, merge, rebase, tags, reset, and diff operations.

See the [service reference](service-reference.md) for the exact ThingWorx service names, argument
names, base types, and result shapes.

## Recommended workflow

1. Configure the remote URL, FileRepository, path, and initial branch.
2. Validate credentials and network/proxy access.
3. Use status to inspect local changes.
4. Export or stage the intended ThingWorx entities.
5. Commit with a meaningful message.
6. Push to the remote branch.

Operations return service results or InfoTables according to the ThingWorx service definition. Operation failures are logged to `GIT.ExtensionLog.DataTable` with the service name, source, user, timestamp, and message.

When a repository is in a detached-head state, check out a branch before committing changes intended for a remote branch. Resolve merge or rebase conflicts in the repository before retrying subsequent operations.
