const { launch, close, getPage } = require('../utils/browser');
const { login, restGet, restPost } = require('../utils/auth');
const { openMashup, waitForLoadingDone } = require('../utils/navigate');
const widgets = require('../utils/widgets');
const config = require('../config');

jest.setTimeout(config.navigationTimeout);

let browser, page;
let extensionVersion = '';

beforeAll(async () => {
  const ctx = await launch();
  browser = ctx.browser;
  page = ctx.page;
  await login(page);
});

afterAll(async () => {
  await close();
});

describe('15 — Extension Status (GitBackup.ExtensionStatus.Mashup)', () => {

  test('15.1 Extension status mashup loads', async () => {
    // Fetch extension version via REST to pass as mashup parameters
    const res = await restPost('/Things/GIT.Utility.Thing/Services/GetGitExtensionVersion', {});
    expect(res.status).toBe(200);
    const row = res.data && res.data.rows && res.data.rows[0];
    expect(row).toBeTruthy();
    extensionVersion = row.ExtensionVersion || '';
    const params = {
      ExtensionName: row.ExtensionName || 'GitBackupExtension',
      IsInstalled: row.IsInstalled === true || row.IsInstalled === 'true',
      Version: extensionVersion,
    };
    await openMashup(page, 'GitBackup.ExtensionStatus.Mashup', params);
    await page.waitForTimeout(2000);
    console.log(`  Extension status mashup loaded with params: ${JSON.stringify(params)}`);
  });

  test('15.2 Version information displayed', async () => {
    // Check for version-related labels
    const allLabels = await page.$$('[id*="root_label"]');
    let versionFound = false;
    for (const label of allLabels) {
      const text = await label.evaluate(el => el.textContent.trim());
      if (text.toLowerCase().includes('version') || /\d+\.\d+\.\d+/.test(text) || text === `v${extensionVersion}`) {
        console.log(`  Version info: "${text}"`);
        versionFound = true;
      }
    }
    expect(versionFound).toBe(true);
  });

  test('15.3 Extension version from REST API', async () => {
    const res = await restGet('/Things/GIT.Utility.Thing');
    expect(res.status).toBe(200);
    console.log(`  GIT.Utility.Thing accessible: ${res.status}`);
  });

  test('15.4 Get extension version via service', async () => {
    // Fetch via REST
    const res = await restPost('/Things/GIT.Utility.Thing/Services/GetGitExtensionVersion', {});
    console.log(`  GetGitExtensionVersion status: ${res.status}`);
  });

});
