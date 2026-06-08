const { launch, close, getPage } = require('../utils/browser');
const { login, restPost } = require('../utils/auth');
const { openMashup, waitForLoadingDone } = require('../utils/navigate');
const widgets = require('../utils/widgets');
const config = require('../config');

let browser, page;

beforeAll(async () => {
  const ctx = await launch();
  browser = ctx.browser;
  page = ctx.page;
  await login(page);
});

afterAll(async () => {
  await close();
});

describe('09 — Merge / Rebase (GitBackup.MergeRebase.Mashup)', () => {

  test('9.1 Merge/Rebase mashup loads', async () => {
    await openMashup(page, 'GitBackup.MergeRebase.Mashup', {
      GitThing: config.testThingName,
    });
    await page.waitForTimeout(2000);
  });

  test('9.2 Create a feature branch for testing', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/CreateBranch`, {
      BranchName: 'merge-test-branch',
    });
    console.log(`  Create merge-test-branch: ${res.status}`);
  });

  test('9.3 Execute merge via service (fast-forward)', async () => {
    // First push a commit on the feature branch
    const checkoutRes = await restPost(`/Things/${config.testThingName}/Services/Checkout`, {
      CommitOrBranch: 'merge-test-branch',
    });
    console.log(`  Checkout to merge-test-branch: ${checkoutRes.status}`);

    // Merge back to main via service
    await restPost(`/Things/${config.testThingName}/Services/Checkout`, {
      CommitOrBranch: 'main',
    });
    const mergeRes = await restPost(`/Things/${config.testThingName}/Services/Merge`, {
      BranchName: 'merge-test-branch',
    });
    console.log(`  Merge (fast-forward): ${mergeRes.status}`);

    // Cleanup
    await restPost(`/Things/${config.testThingName}/Services/DeleteLocalBranch`, {
      BranchName: 'merge-test-branch',
    });
  });

  test('9.4 Create divergent branches for merge-commit test', async () => {
    // Create two branches from main
    await restPost(`/Things/${config.testThingName}/Services/CreateBranch`, {
      BranchName: 'ff-branch',
    });
    await restPost(`/Things/${config.testThingName}/Services/CreateBranch`, {
      BranchName: 'conflict-branch',
    });
    console.log('  Test branches created.');
  });

  test('9.5 Rebase on clean branch', async () => {
    // Checkout feature branch, then rebase onto main
    await restPost(`/Things/${config.testThingName}/Services/Checkout`, {
      CommitOrBranch: 'ff-branch',
    });
    const rebaseRes = await restPost(`/Things/${config.testThingName}/Services/Rebase`, {
      UpstreamBranch: 'main',
    });
    console.log(`  Rebase (clean): ${rebaseRes.status}`);

    // Return to main
    await restPost(`/Things/${config.testThingName}/Services/Checkout`, {
      CommitOrBranch: 'main',
    });
  });

  test('9.6 Cleanup test branches', async () => {
    const branches = ['ff-branch', 'conflict-branch'];
    for (const branch of branches) {
      await restPost(`/Things/${config.testThingName}/Services/DeleteLocalBranch`, {
        BranchName: branch,
      });
    }
    console.log('  Test branches cleaned up.');
  });

});
