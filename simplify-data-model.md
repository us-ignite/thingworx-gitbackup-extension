# Data Model Simplification Plan

## Current Problems

- `Git.Utility.ThingShape` (ThingShape) is a separate entity that gets attached to every repo thing via `AddShapeToThing()` in `initializeThing()` — unnecessary indirection.
- `GIT.Utility.Thing` (singleton) implements the shape but has empty `<ServiceDefinitions>` — services come through the shape.
- Every repo thing inherits 42 utility services it doesn't need.

## Target: Two Clean Types

### Type 1: Utility Thing — `GIT.Utility.Thing` (singleton)

| Aspect | Value |
|---|---|
| Java class | `GitUtilityThing.java` (unchanged) |
| Entity file | `Things_GIT.Utility.Thing.xml` |
| Template | `GenericThing` |
| Service count | **33** — management/utility only (no repo-proxy wrappers) |

### Type 2: Git Repo Things — based on `GitRepositoryTemplate`

| Aspect | Value |
|---|---|
| Java class | `GitRepositoryTemplate.java` (unchanged) |
| Entity file | `ThingTemplates_GitRepositoryTemplate.xml` |
| Template | `GitRepositoryTemplate` |
| Service count | **20** — repo-level operations only |
| Shape | No shape attached at runtime |

## Services Excluded (9 repo-proxy wrappers)

These take a `GitThingName` and delegate to a repo thing. Callers talk to repo things directly instead.

| Service | Delegates to |
|---|---|
| `GetConfiguration` | repo thing's `GetConfigurationTable` |
| `GetRepoConfiguration` | repo thing's `GetConfigurationTable` |
| `GetRecursiveFileListing` | repo thing's config + FileRepository `ListFiles` |
| `GetLocalBranches` | repo thing's `GetBranchList` + filter LOCAL |
| `CreateBranch` | repo thing's `CreateBranch` |
| `QueryDiffFileList` | repo thing's `GetCommitInfo` + extract ChangedFiles |
| `QueryStatus` | repo thing's `Status` + filter |
| `CheckIfBitbucketCredentialsAreCorrect` | repo thing's config + HTTP auth check |
| `ImportProjectEntities` | reads config from repo thing, bulk-imports XML |

**Open question:** Should the Java methods for these 9 services be removed from `GitUtilityThing.java`, or just left as dead code (no XML definition = not callable from ThingWorx)?

## Steps

### Step 1 — Delete ThingShape entity
```
rm Entities/ThingShapes_Git.Utility.ThingShape.xml
```

### Step 2 — Rewrite `Things_GIT.Utility.Thing.xml`
- Drop `<ImplementedShape name="Git.Utility.ThingShape">`
- Replace empty `<ServiceDefinitions>` with the 33 management services (not the 9 proxies)
- Keep `effectiveThingPackage="GitUtilityThingPackage"` and `thingTemplate="GenericThing"`

### Step 3 — Remove `AddShapeToThing` from `GitRepositoryTemplate.java`
- Remove lines 328–335 (the `if (!this.implementsShape(...))` block)
- Remove corresponding import for `EntityServices` if no longer used

### Step 4 — Clean up `Const.java`
- Remove `str_UtilityThingShapeName = "Git.Utility.ThingShape"`
- Keep `str_UtilityThingName = "GIT.Utility.Thing"` (still used for sync/logging calls)

### Step 5 — Update `README.md`
- ThingShapes count from 1 to 0
- Remove `Git.Utility.ThingShape` from examples

## What Stays the Same

- `GitUtilityThing.java` — all 42 service methods (or 33 if we delete the proxy methods)
- `GitRepositoryTemplate.java` — all 20 repo services (only the shape-attach block removed)
- `ThingTemplates_GitRepositoryTemplate.xml` — already created with 20 service definitions
- `Things_GIT.Utility.Thing.xml` keeps the `tab-menu` property and `thingTemplate`
