# Issue: Spec 15 — Mashup Opened Without Required Parameters

**File:** `specs/15-mashup-extension-status.spec.js`

**Line 25:** Opens `GitBackup.ExtensionStatus.Mashup` with no parameters:
```js
await openMashup(page, 'GitBackup.ExtensionStatus.Mashup');
```

But the mashup (`Mashups_GitBackup.ExtensionStatus.Mashup.xml`) expects parameters: `ExtensionName`, `IsInstalled`, `Version`. Without these, data-bound labels render empty, so the version text check (line 36) finds nothing.

**Also:** `restPost` import was already fixed in a previous pass. Confirm the import on line 2 includes `restPost`.

**Fix:** Pass the correct mashup parameters, or update the test to use a different strategy for detecting version info (e.g., check for any non-empty label text).

**No other issue touches this file.**
