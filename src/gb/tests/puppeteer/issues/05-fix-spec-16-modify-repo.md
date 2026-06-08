# Issue: Spec 16 — Wrong Mashup Parameter Name

**File:** `specs/16-mashup-modify-repo.spec.js`

**Line 26:** Passes `{ GitThing: config.testThingName }` to `openMashup`, but the ModifyRepo mashup (`Mashups_GitBackup.ModifyRepo.Mashup.xml`) defines its parameter as **`ThingName`**, not `GitThing`.

**Line 42:** The `expect(hasValue).toBe(true)` check fails because no config data loads (the parameter mismatch means the mashup never fetches configuration for the right thing).

**Fix:** Change `GitThing` to `ThingName` on line 26.

**No other issue touches this file.**
