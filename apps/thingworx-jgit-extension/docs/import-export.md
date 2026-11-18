# Import and export

Every repository Thing tracks one or more ThingWorx projects (`TrackedProjects`), each in its own
folder. Moving entities between ThingWorx and that folder is always a manual step, like saving files
before using a desktop Git client. Git operations (`Commit`, `Push`, `Pull`, `Checkout`,
`BranchSwitch`, `Merge`, `Rebase`, conflict handling, tags) only read and change the repository; they
never export from or import into ThingWorx. Configure the tracked projects and
localization-token prefix in Repository Settings.

## Sync from ThingWorx (export)

`SyncFromThingWorx` (the **Sync from ThingWorx** button on Changes) exports each tracked project to a
temporary folder and reconciles it into the project's folder in the working tree. Changed entities
overwrite their files, and the file of an entity that left the project is deleted; both appear in
`Status` as unstaged changes. The Git index is not changed. Users are never exported, and files that
cannot be identified as canonical entity exports are left alone. It refuses to run while a merge,
rebase or carried-change conflict is waiting.

`Status` only reads Git, so it stays fast on large projects. Use Sync from ThingWorx first when you
want ThingWorx changes to show in Changes. Then stage, `CommitStaged`, and `Push` as separate steps.

## Sync to ThingWorx (import)

`SyncToThingWorx` (the **Sync to ThingWorx** button) discovers XML entities recursively below each
tracked project's folder and imports them into ThingWorx. This overwrites ThingWorx entities and
property values with the repository versions. After a `Pull`, `Checkout`, `Merge` or `Rebase`, use it
to apply the result to ThingWorx.

To avoid losing work, it refuses when entities in the tracked projects changed in ThingWorx since the
last sync in either direction, and names them. Pass `Force=true` to import anyway. The Changes screen
asks first: it calls `GetThingWorxChangesSinceSync`, and when that names any entities it asks for
confirmation before importing with `Force=true`.

The check compares each entity's last-modified date with the time of the last sync, which is
recorded in the repository's own Git config (`thingworx.lastSync`). It reads entity metadata instead
of exporting, so it is quick. It does not see entities deleted in ThingWorx, or property values
written at runtime, because neither changes a modified date. A repository that was never synced
treats every entity in its tracked projects as changed.

Users, and ApplicationKeys whose stored secret ThingWorx cannot decrypt, are never imported; see
the extension log for any key that was skipped. Review ThingWorx logs after an import, especially
when an entity depends on another entity that is not present in the same revision.

Always review changes in Git before importing them into a shared or production ThingWorx instance.
