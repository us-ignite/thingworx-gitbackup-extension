# Issue: Spec 07 — Wrong Widget IDs in Branch Manager Test

**File:** `specs/07-mashup-branch-manager.spec.js`

**Lines 34, 44** use widget IDs that belong to the **Main** mashup, not the **BranchManager** mashup:
- `label-463` (line 34) — does not exist in `Mashups_GitBackup.BranchManager.Mashup.xml`
- `dhxlist-476` (line 44, for `readGridData`) — does not exist in `Mashups_GitBackup.BranchManager.Mashup.xml`

**Actual widget IDs in BranchManager mashup:**
- `lbl-current-branch` — label for current branch display
- `txt-branch` — textbox for branch name
- `txt-new-branch` — textbox for new branch name  
- `btn-create` — create branch button
- `btn-cancel` — cancel button

**Fix:** Update the selectors to reference actual BranchManager widget IDs. The mashup XML is at `Entities/Mashups_GitBackup.BranchManager.Mashup.xml`.

**No other issue touches this file.**
