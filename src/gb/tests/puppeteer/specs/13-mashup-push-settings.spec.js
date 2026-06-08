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

describe('13 — Push Settings (GitBackup.PushSettings.Mashup)', () => {

  test('13.1 PushSettings mashup loads', async () => {
    await openMashup(page, 'GitBackup.PushSettings.Mashup');
    await page.waitForTimeout(2000);
    console.log('  PushSettings mashup loaded.');
  });

  test('13.2 Get current user extension properties', async () => {
    const res = await restPost('/Things/GIT.Utility.Thing/Services/GetGitUserExtensionsProperties', {});
    if (res.status === 200 && res.data.rows) {
      console.log(`  User extension properties found: ${res.data.rows.length}`);
      for (const row of res.data.rows) {
        console.log(`    ${row.name || row.Name}: ${row.value || row.Value}`);
      }
    }
  });

  test('13.3 Set committer name and email via service', async () => {
    const res = await restPost('/Things/GIT.Utility.Thing/Services/SetGitUserExtensionsProperties', {
      GitCommitterName: 'Puppeteer Override',
      GitCommitterEmail: 'override@test.local',
      UseGitCommitUserValues: true,
    });
    console.log(`  Set user extension properties: ${res.status}`);
  });

  test('13.4 Verify settings persisted', async () => {
    const res = await restPost('/Things/GIT.Utility.Thing/Services/GetGitUserExtensionsProperties', {});
    if (res.status === 200 && res.data.rows) {
      const props = {};
      for (const row of res.data.rows) {
        props[row.name || row.Name] = row.value || row.Value;
      }
      console.log(`  GitCommitterName: ${props.GitCommitterName}`);
      console.log(`  GitCommitterEmail: ${props.GitCommitterEmail}`);
      console.log(`  UseGitCommitUserValues: ${props.UseGitCommitUserValues}`);
      expect(props.GitCommitterName).toBe('Puppeteer Override');
    }
  });

  test('13.5 Disable committer override', async () => {
    const res = await restPost('/Things/GIT.Utility.Thing/Services/SetGitUserExtensionsProperties', {
      GitCommitterName: '',
      GitCommitterEmail: '',
      UseGitCommitUserValues: false,
    });
    console.log(`  Disabled override: ${res.status}`);
  });

  test('13.6 Push with custom settings via service', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/Push`, {
      CommitMessage: 'Push from puppeteer settings test',
      AuthorName: 'Settings Tester',
      AuthorEmail: 'settings@test.local',
    });
    console.log(`  Push with custom author: ${res.status}`);
  });

});
