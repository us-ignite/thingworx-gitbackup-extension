const { launch, close, getPage } = require('../utils/browser');
const { login, restPost, restGet } = require('../utils/auth');
const { openMashup, waitForLoadingDone } = require('../utils/navigate');
const widgets = require('../utils/widgets');
const assertions = require('../utils/assertions');
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

describe('10 — Export (GitBackup.Export.Mashup)', () => {

  test('10.1 Export mashup loads', async () => {
    await openMashup(page, 'GitBackup.Export.Mashup', {
      GitThingName: config.testThingName,
    });
    await page.waitForTimeout(2000);
  });

  test('10.2 Export project entities', async () => {
    // The Export button triggers ExportProjectEntities
    const buttons = await page.$$('[id*="root_button"]');
    for (const btn of buttons) {
      const text = await btn.evaluate(el => el.textContent.trim().toLowerCase());
      if (text.includes('export') && text.includes('project')) {
        await btn.click();
        await waitForLoadingDone(page);
        await page.waitForTimeout(3000);
        console.log(`  Export project entities clicked.`);
        break;
      }
    }
  });

  test('10.3 Export project data', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/ExportProjectData`, {
      GitThingName: config.testThingName,
      projectName: 'GitBackup',
    });
    console.log(`  Export data status: ${res.status}`);
  });

  test('10.4 Export extensions', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/ExportProjectExtensions`, {
      GitThingName: config.testThingName,
    });
    console.log(`  Export extensions status: ${res.status}`);
  });

  test('10.5 Export localization tokens', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/ExportLocalizationToken`, {
      GitThingName: config.testThingName,
      LocalizationTokensPrefix: 'GitBackup',
    });
    console.log(`  Export localization tokens status: ${res.status}`);
  });

  test('10.6 Export with auto-push (commit message)', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/ExportProjectEntities`, {
      projectName: 'GitBackup',
      ExportAllEntities: true,
      commitMessage: 'Puppeteer: auto-export with push',
    });
    console.log(`  Export with auto-push status: ${res.status}`);
  });

  test('10.7 Verify exported files exist in repository', async () => {
    // Check status to see exported files
    const statusRes = await restPost(`/Things/${config.testThingName}/Services/QueryStatus`, {});
    if (statusRes.status === 200) {
      const files = statusRes.data.rows || [];
      console.log(`  Files in repo after export: ${files.length}`);
      for (const f of files.slice(0, 5)) {
        console.log(`    ${f.File} (${f.Status})`);
      }
    }
  });

});
