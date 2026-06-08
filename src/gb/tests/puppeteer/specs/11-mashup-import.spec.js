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

describe('11 — Import (GitBackup.Import.Mashup)', () => {

  test('11.1 Import mashup loads', async () => {
    await openMashup(page, 'GitBackup.Import.Mashup', {
      GitThingName: config.testThingName,
    });
    await page.waitForTimeout(2000);
    console.log('  Import mashup loaded.');
  });

  test('11.2 Init extension import targets', async () => {
    // Verify already initialized from setup, but call it again to be safe
    const res = await restPost('/Things/GIT.Utility.Thing/Services/InitExtensionImportTargets', {});
    console.log(`  InitExtensionImportTargets: ${res.status}`);
  });

  test('11.3 Import entity with ignoreDependencies=true', async () => {
    // Try importing an entity file from the repo path
    const res = await restPost('/Things/GIT.Utility.Thing/Services/ImportEntity', {
      GitThingName: config.testThingName,
      ignoreDependencies: true,
    });
    console.log(`  Import entity (ignore deps): ${res.status}`);
    // Should either succeed or give a meaningful error
  });

  test('11.4 Bulk import project entities', async () => {
    const res = await restPost('/Things/GIT.Utility.Thing/Services/ImportProjectEntities', {
      GitThingName: config.testThingName,
      ignoreDependencies: true,
    });
    console.log(`  Bulk import status: ${res.status}`);
    if (res.status === 200 && res.data.rows) {
      console.log(`  Import summary:`);
      for (const row of res.data.rows) {
        console.log(`    ${row.file || row.File}: ${row.status || row.Status}`);
      }
    }
  });

  test('11.5 Import with invalid GitThingName', async () => {
    const res = await restPost('/Things/GIT.Utility.Thing/Services/ImportProjectEntities', {
      GitThingName: 'NonExistentThingName',
    });
    console.log(`  Invalid GitThingName import status: ${res.status}`);
  });

});
