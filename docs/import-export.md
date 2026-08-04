# Import and export

The utility Thing supports bulk synchronization of ThingWorx entities with a Git repository. Every
repository Thing must have a `ProjectName` configured; repository operations synchronize that
project as part of their workflow.

## Export

Export can collect the configured project’s entities, data, extensions, and related resources into
the repository. The export list can also be adjusted explicitly before the export runs. Configure
the repository’s required `ProjectName` and localization-token prefix when those resources should
be included.

Export does not automatically commit or push the resulting files. Review the working tree, call
`Commit` with an explicit message, and call `Push` separately when the commit should be published.

## Import

`ImportProjectEntities` discovers XML entities recursively below the configured repository path and
returns an import summary. `ImportEntity` is the repository Thing service for importing the
configured path into ThingWorx; both services use the repository's required `ProjectName`.
Review the result summary and ThingWorx logs after an import, especially when an entity depends on
another entity that is not present in the same revision.

Always review changes in Git before importing them into a shared or production ThingWorx instance.
