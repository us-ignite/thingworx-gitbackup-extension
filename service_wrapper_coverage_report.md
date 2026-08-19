# Service wrapper coverage report

Audit of all exposed services in `GIT.Repository.ThingShape` (`GitRepositoryShape.java`)
and `GIT.Utility.ThingShape` (`GitUtilityThingShape.java`) against three sources of truth:

- **`@ThingworxServiceResult` Java annotation** — the runtime contract.
- **ThingShape XML entity** (`src/org/us_ignite/thingworx/jgit/entities/*.xml`) — the Composer/import metadata.
- **`docs/service-reference.md`** — the user documentation.

Wrapper shapes (from `ServiceResults.java` / `GIT.ServiceResults.DataShapes.xml`):

- `GIT.OperationResult.DataShape` — `{Message, Error}` for command services.
- `GIT.StringResult.DataShape` — `{Response: STRING, Message, Error}` for string-payload services.
- `GIT.<X>.ServiceResult.DataShape` — `{Response: INFOTABLE, Message, Error}` for table-payload services.

Legend: ✅ consistent · ❌ defect (Java vs return type / XML / docs) · ⚠️ candidate or drift ·
👻 documented but absent from code/XML.

---

## GIT.Repository.ThingShape

### Properly wrapped — OperationResult (Infotable wrapper), Java = XML = return

| Service | Shape | Status |
|---|---|---|
| `Commit` | `GIT.OperationResult.DataShape` | ✅ |
| `Push` | `GIT.OperationResult.DataShape` | ✅ |
| `Pull` | `GIT.OperationResult.DataShape` | ✅ |
| `Fetch` | `GIT.OperationResult.DataShape` | ✅ |
| `DeleteLocalRepoContent` | `GIT.OperationResult.DataShape` | ✅ |
| `BranchCreate` | `GIT.OperationResult.DataShape` | ✅ |
| `Checkout` | `GIT.OperationResult.DataShape` | ✅ |
| `BranchSwitch` | `GIT.OperationResult.DataShape` | ✅ |
| `BranchDelete` | `GIT.OperationResult.DataShape` | ✅ |
| `SetGPGKeyForSigning` | `GIT.OperationResult.DataShape` | ✅ |
| `ExportProjectEntities` | `GIT.OperationResult.DataShape` | ✅ |
| `Merge` | `GIT.OperationResult.DataShape` | ✅ |
| `Rebase` | `GIT.OperationResult.DataShape` | ✅ |
| `DeleteTag` | `GIT.OperationResult.DataShape` | ✅ |
| `WriteConflictFile` | `GIT.OperationResult.DataShape` | ✅ |
| `CreateTag` | `GIT.OperationResult.DataShape` | ✅ fixed this session (annotation was `STRING`; now matches actual return + XML) |

### Properly wrapped — typed ServiceResult (Infotable payload)

| Service | Shape | Status |
|---|---|---|
| `Add` | `GIT.StringResult.DataShape` | ✅ fixed this session (was bare string vs `INFOTABLE`) |
| `ReadConflictFile` | `GIT.StringResult.DataShape` | ✅ |
| `GetDiffPerFile` | `GIT.StringResult.DataShape` | ✅ |
| `GetDiffPerFileBetweenCommits` | `GIT.StringResult.DataShape` | ✅ |
| `GetLog` | `GIT.CommitLog.ServiceResult.DataShape` | ✅ |
| `GetReflog` | `GIT.ReflogEntry.ServiceResult.DataShape` | ✅ |
| `GetCommitInfo` | `GIT.CommitInfo.ServiceResult.DataShape` | ✅ |
| `Status` | `GIT.Status.ServiceResult.DataShape` | ✅ |
| `GetConflictFiles` | `GIT.Status.ServiceResult.DataShape` | ✅ |
| `ImportProjectEntities` | `GIT.Status.ServiceResult.DataShape` | ✅ returns post-import working-tree status |
| `GetCurrentBranch` | `GIT.CurrentBranchStatus.ServiceResult.DataShape` | ✅ fixed this session (was raw `GIT.CurrentBranchStatus.DataShape`) |
| `GetBranchList` | `GIT.BranchList.ServiceResult.DataShape` | ✅ fixed this session (was raw `GIT.BranchList.DataShape`) |
| `GetTagList` | `GIT.TagList.ServiceResult.DataShape` | ✅ fixed this session (was raw `GIT.TagList.DataShape`) |

### Resolved — raw-table services now wrapped

The three services that previously returned a raw table with an orphaned
`.ServiceResult` wrapper shape now return the wrapper (Java annotation, method body,
ThingShape XML, docs, and tests updated in this session). The raw shapes remain only
as the nested `Response` payload. See the table above.

### In Java but absent from ThingShape XML

Services are invoked internally by `Commit`/etc. but are declared with
`@ThingworxServiceDefinition` and exported as public services — they are missing from
`GIT.Repository.ThingShape.xml` (and not in the service docs).

| Service | Java result | Status |
|---|---|---|
| `RemoveLastModifiedDate` | `INFOTABLE` `GIT.OperationResult.DataShape` | ⚠️ XML entry missing |
| `RemoveModelPersistenceProviderPackage` | `INFOTABLE` `GIT.OperationResult.DataShape` | ⚠️ XML entry missing |

### Bare-string services — consistent (`STRING`/`STRING`) but unwrapped

Functionally fine today, but they violate the wrapper convention now adopted for `Add`
and are candidates for conversion to `GIT.StringResult.DataShape`.

| Service |
|---|
| `Remove` |
| `MergeContinue` |
| `MergeAbort` |
| `RebaseContinue` |
| `RebaseSkip` |
| `RebaseAbort` |

---

## GIT.Utility.ThingShape

All Java services already return `InfoTable` wrappers. The drift is below the runtime
contract: the **XML entity and the docs** were never updated to match.

### Resolved — XML/docs `NOTHING` services now declare the OperationResult wrapper

All three services previously declared `NOTHING` in XML with "Returns NOTHING" in the
docs. This session corrected the XML `ResultType` and dropped the stale doc claims.

| Service | Java result | XML result | doc | Status |
|---|---|---|---|---|
| `GpgKeyCreate` | `INFOTABLE` `GIT.OperationResult.DataShape` | `INFOTABLE` `GIT.OperationResult.DataShape` | no NOTHING claim | ✅ fixed this session |
| `GitCredentialUpdate` | `INFOTABLE` `GIT.OperationResult.DataShape` | `INFOTABLE` `GIT.OperationResult.DataShape` | no NOTHING claim | ✅ fixed this session |
| `GitCredentialDelete` | `INFOTABLE` `GIT.OperationResult.DataShape` | `INFOTABLE` `GIT.OperationResult.DataShape` | no NOTHING claim | ✅ fixed this session |

### Resolved — UserExtension doc claims and missing `RepositoryList` dataShape

XML `ResultType`s now point at the ServiceResult wrappers and the docs describe the
wrapper shapes; `RepositoryList` now declares `GIT.RepositoryList.ServiceResult.DataShape`.

| Service | Java result | XML result | doc | Status |
|---|---|---|---|---|
| `GpgKeyList` | `GIT.GpgKey.ServiceResult.DataShape` | `GIT.GpgKey.ServiceResult.DataShape` | `GIT.GpgKey.ServiceResult.DataShape` | ✅ fixed this session |
| `GpgKeyGet` | `GIT.GpgKey.ServiceResult.DataShape` | `GIT.GpgKey.ServiceResult.DataShape` | `GIT.GpgKey.ServiceResult.DataShape` | ✅ fixed this session (docs) |
| `GitCredentialList` | `GIT.RepositoryConfiguration.ServiceResult.DataShape` | `GIT.RepositoryConfiguration.ServiceResult.DataShape` | `...ServiceResult.DataShape` | ✅ fixed this session |
| `GitCredentialGet` | `GIT.RepositoryConfiguration.ServiceResult.DataShape` | `GIT.RepositoryConfiguration.ServiceResult.DataShape` | `...ServiceResult.DataShape` | ✅ fixed this session |
| `RepositoryList` | `GIT.RepositoryList.ServiceResult.DataShape` | `GIT.RepositoryList.ServiceResult.DataShape` | — | ✅ fixed this session (XML dataShape) |

### Resolved — "Returns NOTHING" doc claims removed

These services return `GIT.OperationResult.DataShape` (Java + XML already correct); the
docs-only "Returns NOTHING" claims were dropped this session.

| Service | Status |
|---|---|
| `RepositoryDelete` | ✅ docs fixed this session |
| `GpgKeyUpdate` | ✅ docs fixed this session |
| `GpgKeyDelete` | ✅ docs fixed this session |
| `GitCredentialCreate` | ✅ docs fixed this session |

### Consistent

| Service | Java result | Status |
|---|---|---|
| `RepositoryCreate` | `GIT.OperationResult.DataShape` | ✅ Java = XML = docs |
| `InitUserExtensionProperties` | `GIT.OperationResult.DataShape` | ✅ |
| `VerifyGpgKey` | `GIT.GpgKeyVerification.ServiceResult.DataShape` | ✅ Java = XML (docs wording imprecise) |

### Ghost — documented but not present in code or XML

`AddLogEntry` was removed from the docs this session — it existed only in
`docs/service-reference.md` (and stale generated javadoc); it is not in source or XML.

No remaining ghosts.

---

## Summary of required fixes

1. **✅ Done — `CreateTag`** — Java result annotation corrected from `STRING` to `INFOTABLE` +
   `GIT.OperationResult.DataShape` (matches actual return + XML).
2. **✅ Done — Raw table services** — `GetCurrentBranch`, `GetBranchList`, `GetTagList` were
   wrapped in their `*.ServiceResult.DataShape` wrappers (Java, XML, docs, tests).
3. **Utility XML** — ✅ Done: `GpgKeyCreate`, `GitCredentialUpdate`, `GitCredentialDelete`
   (`NOTHING` → `INFOTABLE` + `GIT.OperationResult.DataShape`); `GpgKeyList`, `GpgKeyGet`,
   `GitCredentialList`, `GitCredentialGet` now point at `*.ServiceResult.DataShape`; `RepositoryList`
   now declares `GIT.RepositoryList.ServiceResult.DataShape`.
4. **Docs** — ✅ Done for all "Returns NOTHING" claims, the 7 utility services, the UserExtension
   shape claims, and `AddLogEntry` (dropped). Minor: `VerifyGpgKey` wording imprecise.
5. **Optional** — convert the six bare-string repository services (`Remove`, `MergeContinue`,
   `MergeAbort`, `RebaseContinue`, `RebaseSkip`, `RebaseAbort`) to `GIT.StringResult.DataShape`;
   decide on `RemoveLastModifiedDate` / `RemoveModelPersistenceProviderPackage` XML presence.