# Import and export

The utility Thing supports bulk synchronization of ThingWorx entities with a Git repository. Every
repository Thing must have a `ProjectName` configured; repository operations synchronize that
project as part of their workflow.

## Export

Export collects all entities from the project configured on the `GIT.Repository` Thing—which is
itself the FileRepository—into the repository. Configure the repository’s `ProjectName` and
localization-token prefix when those resources should be included.

Export stages only the configured ThingWorx project tree; it never commits or pushes the resulting
files. Stage manual files with `Add`, review the index and working tree with `Status`, call `Commit`
with an explicit message, and call `Push` separately when the commit should be published.

## Import

`ImportProjectEntities` discovers XML entities recursively below the configured repository path and
returns an import summary. Use it for all repository imports; it uses the repository's required
`ProjectName`.
Review the result summary and ThingWorx logs after an import, especially when an entity depends on
another entity that is not present in the same revision.

Always review changes in Git before importing them into a shared or production ThingWorx instance.
