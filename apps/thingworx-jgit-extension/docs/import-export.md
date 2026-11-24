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
`Status` as unstaged changes. Files whose content did not change are not rewritten, and nothing is
staged; the sync only refreshes the file timestamps the Git index keeps for unchanged files, so
`Status` does not have to re-read them. Users are exported without their server-local parts; see
[Users](#users). Files that cannot be identified as canonical entity exports are left alone. It refuses to run while a merge,
rebase or carried-change conflict is waiting.

`Status` only reads Git, so it stays fast on large projects. Use Sync from ThingWorx first when you
want ThingWorx changes to show in Changes. Then stage, `CommitStaged`, and `Push` as separate steps.

## Sync to ThingWorx (import)

`SyncToThingWorx` (the **Sync to ThingWorx** button) discovers XML entities recursively below each
tracked project's folder and imports them into ThingWorx. Only files tracked by Git, committed or
staged, are imported. An untracked file, such as one a Sync from ThingWorx wrote for a new entity or
one copied into the folder by hand, is never imported; the result names it, and the way to include it
is to add, commit and push it. This overwrites ThingWorx entities and
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

Users are imported first, one at a time, so Groups and Organizations can refer to them; see
[Users](#users). ApplicationKeys whose stored secret ThingWorx cannot decrypt are never imported; see
the extension log for any key that was skipped. Review ThingWorx logs after an import, especially
when an entity depends on another entity that is not present in the same revision.

## Partial success

Both syncs go as far as they can instead of failing as a whole when one item fails. Each returns a
`GIT.ThingworxSyncAction.ServiceResult`. Its `Result` lists the items that did not sync, one row per
item, with `ProjectName`, `Collection` (blank for a whole project), `EntityName` (blank for a whole
collection or project) and `Reason`. A sync where some items got through is a partial success, so
`Error` stays `false` and the rows in `Result` are the only sign of what was left behind:

| Outcome | `Error` | `Result` | `Message` |
| --- | --- | --- | --- |
| Everything synced | `false` | Empty | The usual completion message |
| Partial success | `false` | One row per item that did not sync | Starts with *partially imported* or *partially exported* and names up to ten of the items |
| Nothing synced | `true` | Empty | Names up to ten of the items that failed |

Check `Result`, not just `Error`, to find out whether every item synced.

**Sync to ThingWorx.** Each project is imported in one call first, as before. When the platform
refuses it, the project's entities are imported one at a time instead, in passes: an entity that fails
because something it depends on, such as its ThingTemplate, is not there yet is tried again after the
others. Entities that still fail are tried together once more, for dependencies that go both ways,
and whatever still fails is reported. A failed entity keeps its current ThingWorx copy; the next Sync
from ThingWorx shows it against the Git version in Changes. Users, Data Tables and projects each
fail on their own in the same way. The last sync time is recorded as usual.

**Sync from ThingWorx.** Each project is exported in one call first, as before. When that fails, the
project is exported one collection at a time (`Things`, `Mashups`, ...). A collection, project or
Data Table that still cannot be exported keeps its files in the working tree exactly as they were,
so a failed export never shows up as deleted entities, and files outside any collection folder are
also left alone for that project. The last sync time is only recorded when everything was exported,
so Sync to ThingWorx keeps warning about ThingWorx changes that did not reach the working tree.

## Entity types

Each tracked project synchronizes every entity type unless you choose a subset under **Entity types to
sync** in Repository Settings (`SetTrackedProjectEntityTypes`; `GetEntityTypeOptions` lists the
choices). A type is named by its collection, which is also its folder in the export: `Things`,
`ThingTemplates`, `Mashups`, `DataShapes`, `Users`, and so on. The project's `EntityTypes` column
stores the choice as a comma-separated list; blank, or every type selected, means all types,
including types not in the list.

With a subset, both directions only touch the chosen types' folders:

- Sync from ThingWorx drops other collection folders from the export, and never deletes the files of
  types that are not synchronized.
- Sync to ThingWorx imports only the chosen collection folders. Files directly in the project folder,
  outside any collection folder, are not imported.
- The changed-since-sync check only reports entities of the chosen types.

The project's own `Projects/<ProjectName>.xml` is always synchronized. Changing the types never
deletes or moves files: files of a type you deselect stay in the repository, unchanged, until you
remove them yourself.

## Users

Users in a tracked project travel through Git like any other entity, except for what belongs to the
server they live on. That is never written to Git, and an import never changes it:

- the password hash;
- account state: `enabled`, `locked` and `lockedTime`;
- every PASSWORD value in the user's extension properties, such as SSH and GPG private keys, key
  passphrases and per-remote Git sign-ins. This applies to PASSWORD properties and to the PASSWORD
  fields of INFOTABLE properties.

Everything else is exported and imported: description, tags, home mashups, project, permissions,
avatar, and every non-secret extension property (names, email addresses, Git identity, public keys,
key labels and fingerprints).

On import, the extension builds each User's file from the Git copy plus that user's live hash,
account state and server-encrypted secrets, and imports it on its own. The secrets are never
decrypted. Within an INFOTABLE property that has PASSWORD fields, rows are matched on the primary
key: a matched row keeps its secrets, and a row only the server has is kept. An extension property
the Git copy does not have keeps its server value.

A User that exists in Git but not on the server is created **disabled**, with a random password
nobody knows. The result of Sync to ThingWorx names it: set a password and enable it. A User that
fails to import, for example because the licence has no named users, is named in the result too, and
the other Users still import. Users are never deleted by a sync.

Always review changes in Git before importing them into a shared or production ThingWorx instance.
