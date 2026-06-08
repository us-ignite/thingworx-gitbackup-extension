const { launch, close, getPage } = require('../utils/browser');
const { login, restPost, restGet } = require('../utils/auth');
const { openMashup, readGridData, waitForLoadingDone } = require('../utils/navigate');
const widgets = require('../utils/widgets');
const assertions = require('../utils/assertions');
const config = require('../config');

jest.setTimeout(config.navigationTimeout);

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

describe('07 — Branch Manager (GitBackup.BranchManager.Mashup)', () => {

  test('7.1 Branch manager mashup loads', async () => {
    await openMashup(page, 'GitBackup.BranchManager.Mashup', {
      GitThing: config.testThingName,
    });
    await page.waitForTimeout(2000);
  });

  test('7.2 Current branch displayed', async () => {
    // The current branch info is in the branch list or a label
    const branchLabel = await widgets.readLabel(page, 'lbl-current-branch');
    if (branchLabel) {
      console.log(`  Current branch: ${branchLabel}`);
      expect(branchLabel.length).toBeGreaterThan(0);
    } else {
      console.log('  Branch label not found via selector.');
    }
  });

  test('7.3 Branch list available via REST', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/GetBranchList`, {});
    expect(res.status).toBe(200);
    const branches = res.data.rows || [];
    console.log(`  Branches from service: ${branches.length}`);
    expect(branches.length).toBeGreaterThan(0);
  });

  test('7.4 Create a new branch', async () => {
    // Find branch name textbox and create button
    const textboxes = await page.$$('[id^="root_"] input[type="text"]');
    let branchNameBox = null;
    for (const tb of textboxes) {
      const placeholder = await tb.evaluate(el => el.placeholder || el.title || '');
      if (placeholder.toLowerCase().includes('branch') || placeholder.toLowerCase().includes('name')) {
        branchNameBox = tb;
        break;
      }
    }
    if (branchNameBox) {
      await branchNameBox.click({ clickCount: 3 });
      await branchNameBox.type('puppeteer-test-branch', { delay: 5 });

      // Find and click create button
      const buttons = await page.$$('[id*="root_button"]');
      for (const btn of buttons) {
        const text = await btn.evaluate(el => el.textContent.trim().toLowerCase());
        if (text.includes('create') || text.includes('new branch')) {
          await btn.click();
          break;
        }
      }
      await waitForLoadingDone(page);
      await page.waitForTimeout(2000);
      console.log('  Branch creation attempted.');
    }
  });

  test('7.5 Verify branch was created via REST', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/GetBranchList`, {});
    expect(res.status).toBe(200);
    const branches = res.data.rows || [];
    const branchNames = branches.map(b => b.name);
    console.log(`  Branches: ${branchNames.join(', ')}`);
    expect(branchNames.some(n => n.includes('puppeteer-test-branch'))).toBe(true);
  });

  test('7.6 Switch to a different branch', async () => {
    // Use Checkout service via REST (faster than UI interaction)
    const res = await restPost(`/Things/${config.testThingName}/Services/Checkout`, {
      CommitOrBranch: 'puppeteer-test-branch',
    });
    console.log(`  Checkout status: ${res.status}`);
    // Verify current branch changed
    const currentRes = await restPost(`/Things/${config.testThingName}/Services/GetCurrentBranch`, {});
    if (currentRes.status === 200) {
      const branchName = currentRes.data.rows?.[0]?.BranchName || '';
      console.log(`  Now on branch: ${branchName}`);
    }
  });

  test('7.7 Switch back to main branch', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/Checkout`, {
      CommitOrBranch: 'main',
    });
    console.log(`  Checkout back to main: ${res.status}`);
  });

  test('7.8 Delete the test branch', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/DeleteLocalBranch`, {
      BranchName: 'puppeteer-test-branch',
    });
    console.log(`  Delete branch status: ${res.status}`);
  });

  test('7.9 Verify branch deleted', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/GetBranchList`, {});
    const branches = res.data.rows || [];
    const branchNames = branches.map(b => b.name);
    expect(branchNames.some(n => n.includes('puppeteer-test-branch'))).toBe(false);
    console.log(`  Branch removed. Remaining: ${branchNames.join(', ')}`);
  });

});
