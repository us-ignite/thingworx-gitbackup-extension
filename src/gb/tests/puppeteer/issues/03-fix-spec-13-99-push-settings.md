# Issue: Specs 13 & 99 — `SetGitUserExtensionsProperties` Doesn't Exist

**Files:**
- `specs/13-mashup-push-settings.spec.js` — lines 41, 64
- `specs/99-teardown.spec.js` — line 25

The service `SetGitUserExtensionsProperties` is called on `GIT.Utility.Thing` but **does not exist** in `Things_GIT.Utility.Thing.xml`. Only `GetGitUserExtensionsProperties` exists.

**Fix options:**
1. Implement `SetGitUserExtensionsProperties` on `GIT.Utility.Thing` (`Things_GIT.Utility.Thing.xml`)
2. Or update specs to use a different existing service

**No other issue touches these files (13, 99).**
