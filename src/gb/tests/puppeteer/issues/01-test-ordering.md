# Issue: Test Ordering

All specs 02–18 depend on the test GitBackup Thing created by `01-setup.spec.js`. Since they each run as independent Jest test files, the Thing must already exist before they execute. Currently the Thing is created inside `01-setup` at test runtime rather than in global setup, and there's no guarantee it persists for subsequent spec files.

**Goal:** Ensure the test Thing is created before specs 02–18 run, not during spec 01 execution.

**Options:**
- Move `AddNewRepo` call into `jest.global-setup.js` so the Thing exists before any spec runs
- Or make `01-setup` a proper dependency that blocks all later specs

**No other issue touches this concern.**
