# Import and export

The utility Thing supports bulk synchronization of ThingWorx entities with a Git repository.

## Export

Export can collect a project’s entities, data, extensions, and related resources into the repository. The export list can also be adjusted explicitly before the export runs. Configure the repository’s project and localization-token prefix when those resources should be included.

An export may automatically commit and push the resulting files. Enable auto-push only when the repository permissions and review workflow support that behavior.

## Import

Import discovers XML entities recursively in the repository and reports successes and failures. Review the result summary and ThingWorx logs after an import, especially when an entity depends on another entity that is not present in the same revision.

Always review changes in Git before importing them into a shared or production ThingWorx instance.
