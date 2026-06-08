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

describe('16 — Modify Repo (GitBackup.ModifyRepo.Mashup)', () => {

  test('16.1 Modify repo mashup loads', async () => {
    await openMashup(page, 'GitBackup.ModifyRepo.Mashup', {
      ThingName: config.testThingName,
    });
    await page.waitForTimeout(2000);
    console.log('  Modify repo mashup loaded.');
  });

  test('16.2 Configuration fields pre-populated', async () => {
    const textboxes = await page.$$('[id^="root_"] input[type="text"]');
    let hasValue = false;
    for (const tb of textboxes) {
      const value = await tb.evaluate(el => el.value);
      if (value && value.length > 0) {
        hasValue = true;
        console.log(`  Pre-populated field: "${value.substring(0, 60)}"`);
      }
    }
    expect(hasValue).toBe(true);
  });

  test('16.3 Get current configuration via REST', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/GetConfiguration`, {});
    expect(res.status).toBe(200);
    const rows = res.data.rows || [];
    console.log(`  Configuration entries: ${rows.length}`);
    for (const row of rows) {
      console.log(`    ${row.name || row.Name} = ${row.value || row.Value}`);
    }
  });

  test('16.4 Modify configuration via service', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/SetConfiguration`, {
      configuration: [
        { name: 'InitialBranch', value: 'main' },
      ],
    });
    console.log(`  SetConfiguration: ${res.status}`);
  });

});
