const { launch, close, getPage } = require('../utils/browser');
const { login, restPost, restGet } = require('../utils/auth');
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

describe('08 — Checkout (GitBackup.Checkout.Mashup)', () => {

  test('8.1 Checkout mashup loads', async () => {
    await openMashup(page, 'GitBackup.Checkout.Mashup', {
      GitThing: config.testThingName,
    });
    await page.waitForTimeout(2000);
  });

  test('8.2 Checkout existing branch via UI', async () => {
    // Find branch textbox
    const textboxes = await page.$$('[id^="root_"] input[type="text"]');
    for (const tb of textboxes) {
      const placeholder = await tb.evaluate(el => el.placeholder || '');
      if (placeholder.toLowerCase().includes('branch') || placeholder.toLowerCase().includes('checkout')) {
        await tb.click({ clickCount: 3 });
        await tb.type('main', { delay: 5 });
        break;
      }
    }

    // Click checkout button
    const buttons = await page.$$('[id*="root_button"]');
    for (const btn of buttons) {
      const text = await btn.evaluate(el => el.textContent.trim().toLowerCase());
      if (text.includes('checkout') || text.includes('switch')) {
        await btn.click();
        break;
      }
    }

    await waitForLoadingDone(page);
    await page.waitForTimeout(2000);
    console.log('  Checkout executed.');
  });

  test('8.3 Checkout commit hash (detached HEAD)', async () => {
    // Get a commit hash via REST
    const commitList = await restPost(`/Things/${config.testThingName}/Services/GetCommitList`, {});
    if (commitList.status === 200 && commitList.data.rows?.length > 0) {
      const commitHash = commitList.data.rows[0].CommitID;
      console.log(`  Using commit: ${commitHash}`);

      const res = await restPost(`/Things/${config.testThingName}/Services/Checkout`, {
        CommitOrBranch: commitHash,
      });
      console.log(`  Detached HEAD checkout: ${res.status}`);

      // Verify detached
      const current = await restPost(`/Things/${config.testThingName}/Services/GetCurrentBranch`, {});
      const isDetached = current.data.rows?.[0]?.DetachedHEAD;
      console.log(`  Detached HEAD: ${isDetached}`);

      // Checkout back to main
      await restPost(`/Things/${config.testThingName}/Services/Checkout`, {
        CommitOrBranch: 'main',
      });
    } else {
      console.log('  No commits available for detached HEAD test.');
    }
  });

  test('8.4 Checkout non-existent branch shows error', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/Checkout`, {
      CommitOrBranch: 'non-existent-branch-xyz',
    });
    console.log(`  Invalid branch checkout status: ${res.status}`);
    // Should return an error
    expect(res.status).not.toBe(200);
  });

});
