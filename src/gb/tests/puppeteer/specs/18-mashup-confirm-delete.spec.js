const { launch, close, getPage } = require('../utils/browser');
const { login, restPost, restGet } = require('../utils/auth');
const { openMashup, waitForLoadingDone } = require('../utils/navigate');
const widgets = require('../utils/widgets');
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

describe('18 — Confirm Delete (GitBackup.ConfirmDeleteThing.Mashup)', () => {

  test('18.1 Confirm delete mashup loads', async () => {
    await openMashup(page, 'GitBackup.ConfirmDeleteThing.Mashup', {
      GitThing: config.testThingName,
    });
    await page.waitForTimeout(2000);
    console.log('  Confirm delete mashup loaded.');
  });

  test('18.2 Confirmation dialog visible', async () => {
    // Should show a confirmation prompt
    const labels = await page.$$('[id*="root_htmltextarea"]');
    let hasPrompt = false;
    for (const label of labels) {
      const text = await label.evaluate(el => el.textContent.trim().toLowerCase());
      if (text.includes('delete') || text.includes('confirm') || text.includes('are you sure')) {
        hasPrompt = true;
        console.log(`  Prompt text: "${text}"`);
        break;
      }
    }
    expect(hasPrompt).toBe(true);
  });

  test('18.3 Cancel button closes dialog', async () => {
    // Find and click Cancel
    const buttons = await page.$$('[id*="root_button"]');
    for (const btn of buttons) {
      const text = await btn.evaluate(el => el.textContent.trim().toLowerCase());
      if (text.includes('cancel') || text.includes('no')) {
        await btn.click();
        console.log('  Cancel clicked.');
        await page.waitForTimeout(1000);
        break;
      }
    }

    // Verify thing still exists after cancel
    const res = await restGet(`/Things/${config.testThingName}`);
    expect(res.status).toBe(200);
    console.log('  Thing still exists after cancel (good).');
  });

  test('18.4 Confirm delete via service (actual deletion)', async () => {
    // Use DeleteGitThing service for actual cleanup (will be done in teardown)
    const res = await restPost('/Things/GIT.Utility.Thing/Services/DeteleGitThing', {
      RepoName: config.testThingName,
    });
    console.log(`  DeleteGitThing service: ${res.status}`);

    // Verify thing is deleted
    if (res.status === 200) {
      const checkRes = await restGet(`/Things/${config.testThingName}`);
      if (checkRes.status === 404) {
        console.log('  Thing successfully deleted.');
      } else {
        console.log(`  Thing still exists (status: ${checkRes.status}).`);
      }
    }
  });

});
