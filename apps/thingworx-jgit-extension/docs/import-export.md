# Import and export

The utility Thing supports bulk synchronization of ThingWorx entities with a Git repository. Every
repository Thing must have a `ProjectName` configured; repository operations synchronize that
project as part of their workflow.

## Export

Export is internal and automatic: operations that need the current ThingWorx model (`Commit`, `Push`,
`Pull` bootstrap, `Checkout`, `Merge`, `Rebase`, etc.) export the project configured on the
`GIT.Repository` Thing—which is itself the FileRepository—into the repository without user action.
Configure the repository's `ProjectName` and localization-token prefix when those resources should be
included.

Export stages only the configured ThingWorx project tree; it never commits or pushes the resulting
files. Stage manual files with `Add`, review the index and working tree with `Status`, call `Commit`
with an explicit message, and call `Push` separately when the commit should be published. There is no
direct `ExportProjectEntities` service - sync is invisible.

## Import

Import is internal and automatic: `Pull` (and other integrating operations) discover XML entities
recursively below the configured repository path and import the configured `ProjectName` back into
ThingWorx without user action. There is no direct `ImportProjectEntities` service.
Review ThingWorx logs after a `Pull`, especially when an entity depends on another entity that is not
present in the same revision. `Pull` always imports the configured project.

When `Pull` successfully fetches and merges Git history but importing one or more entities into
ThingWorx throws, the service preserves the pulled Git changes and returns a failure envelope
(`Error=true`) with the message `Git Pull completed, but importing entities into ThingWorx failed for
project '<ProjectName>': <cause>`. The working tree remains at the pulled revision; no automatic
reset or rollback of the potentially partial ThingWorx import is attempted. Cleanup still runs, and
the case where no XML entities exist under the repository path remains a warning, not a failure.

Always review changes in Git before importing them into a shared or production ThingWorx instance.
