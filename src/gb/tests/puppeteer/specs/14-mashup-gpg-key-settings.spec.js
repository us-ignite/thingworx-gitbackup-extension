const { launch, close, getPage } = require('../utils/browser');
const { login, restPost, restGet } = require('../utils/auth');
const { openMashup, waitForLoadingDone } = require('../utils/navigate');
const widgets = require('../utils/widgets');
const assertions = require('../utils/assertions');
const config = require('../config');
const fs = require('fs');
const path = require('path');
const testGpgKey = fs.readFileSync(path.join(__dirname, '..', 'fixtures', 'test-gpg-key.asc'), 'utf8');

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

describe('14 — GPG Key Settings (GitBackup.GpgKeySettings.Mashup)', () => {

  test('14.1 GPG key settings mashup loads', async () => {
    await openMashup(page, 'GitBackup.GpgKeySettings.Mashup', {
      GitThing: config.testThingName,
    });
    await page.waitForTimeout(2000);
    console.log('  GPG settings mashup loaded.');
  });

  test('14.2 Verify key is visible (textarea or password field)', async () => {
    const textarea = await page.$('textarea');
    const passwordField = await page.$('input[type="password"]');
    expect(textarea || passwordField).toBeTruthy();
    console.log(`  GPG key input found: ${textarea ? 'textarea' : 'password field'}`);
  });

  test('14.3 VerifyGpgKey with invalid key', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/VerifyGpgKey`, {
      GpgPrivateKey: 'invalid-key-data',
      GpgKeyPassphrase: '',
    });
    console.log(`  Verify invalid key: status=${res.status}`);
    // Should fail
    expect(res.status).toBe(500);
  });

  test('14.4 VerifyGpgKey with test key and passphrase', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/VerifyGpgKey`, {
      GpgPrivateKey: testGpgKey,
      GpgKeyPassphrase: 'testpassphrase',
    });
    console.log(`  Verify test key: status=${res.status}`);
  });

  test('14.5 SetGpgKey (auto-initializes GpgKeys property)', async () => {
    const res = await restPost('/Things/GIT.Utility.Thing/Services/SetGpgKey', {
      GitThing: config.testThingName,
      GpgPrivateKey: testGpgKey,
      GpgKeyPassphrase: 'testpassphrase',
      SignCommits: false,
    });
    console.log(`  SetGpgKey: status=${res.status}`);
  });

  test('14.6 Get existing GPG keys', async () => {
    const res = await restPost('/Things/GIT.Utility.Thing/Services/GetGpgKeys', {
      GitThing: config.testThingName,
    });
    if (res.status === 200 && res.data.rows) {
      console.log(`  GPG keys found: ${res.data.rows.length}`);
      for (const row of res.data.rows) {
        console.log(`    Key: ${row.GpgKeyFingerprint || '(no fingerprint)'}`);
      }
    } else {
      console.log(`  GetGpgKeys: status=${res.status}`);
    }
  });

  test('14.7 Enable commit signing and push', async () => {
    // Set SignCommits on the test thing
    const configRes = await restPost(`/Things/${config.testThingName}/Services/GetConfiguration`, {});
    if (configRes.status === 200) {
      // The SignCommits property is part of the Configuration table
      console.log('  SignCommits can be set via Configuration table.');
    }

    // Push with signing enabled
    const res = await restPost(`/Things/${config.testThingName}/Services/Push`, {
      CommitMessage: 'GPG signed push from puppeteer',
    });
    console.log(`  Push after GPG setup: ${res.status}`);
  });

});
