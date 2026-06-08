# Issue: Spec 18 — No Label Widgets + Wrong Service Param Name

**File:** `specs/18-mashup-confirm-delete.spec.js`

Two distinct bugs in this file:

## 6a. No label widgets for confirmation dialog (line 34)
The test searches for `[id*="root_label"]` to find confirmation text, but the ConfirmDelete mashup has **no label widgets**. Confirmation text is rendered via an `expression` widget feeding into an `htmltextarea`. The selector returns zero results.

## 6b. Wrong parameter name for `DeteleGitThing` (lines 68-70)
The test passes `{ GitThingName: config.testThingName }` but the service definition in `Things_GIT.Utility.Thing.xml` expects `RepoName`, not `GitThingName`.

**Fixes:**
1. Update the confirmation text selector to target the htmltextarea/expression widget instead of labels. Check `Mashups_GitBackup.ConfirmDeleteThing.Mashup.xml` for actual widget IDs.
2. Change `GitThingName` to `RepoName` in the request body on line 69.

**No other issue touches this file.**
