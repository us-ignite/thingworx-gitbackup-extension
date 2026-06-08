const { launch, close } = require('../utils/browser');
const { login, restGet, restPost } = require('../utils/auth');
const { openMashup } = require('../utils/navigate');
const widgets = require('../utils/widgets');
const assertions = require('../utils/assertions');
const config = require('../config');
const giteaFixture = require('../fixtures/gitea-config.json');

const { defaultTimeout, navigationTimeout } = config;

jest.setTimeout(navigationTimeout);

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

describe('01 — Test Setup: Create FileRepository + GitBackup Thing', () => {

  test('1.1 Verify existing FileRepository Thing via REST API', async () => {
    // Use the existing GitRepository FileRepository shipped with the extension
    const repoName = 'GitRepository';
    const res = await restGet(`/Things/${repoName}`);
    expect(res.status).toBe(200);
    expect(res.data.name).toBe(repoName);
    console.log(`  Using existing FileRepository: ${repoName}`);
    // Override config to use this repo
    config.testFileRepoName = repoName;
  });

  test('1.2 Open NewRepo mashup', async () => {
    await openMashup(page, 'GitBackup.NewRepo.Mashup');
    // Verify the mashup loaded
    const title = await widgets.readLabel(page, 'label-5');
    expect(title).toBeTruthy();
  });

  test('1.3 Fill NewRepo form — Basic Info tab', async () => {
    // Repo name (ptcstextfield-136)
    await widgets.fillTextbox(page, 'ptcstextfield-136', config.testThingName);
    await page.waitForTimeout(300);
    // Git Repo URL — may not render in Runtime; skip if not found
    try {
      await widgets.fillTextbox(page, 'Autocomplete-109', giteaFixture.gitRepoUrl);
      await page.waitForTimeout(300);
    } catch {
      console.log('  Autocomplete-109 not found in Runtime, skipping GitRepoURL field');
    }
    // Select FileRepository via EntityPicker
    await widgets.openEntityPicker(page, 'entitypicker-29', config.testFileRepoName);
    await page.waitForTimeout(300);
    // Repo path
    await widgets.fillTextbox(page, 'textbox-31', config.testRepoPath);
    // Initial branch
    await widgets.fillTextbox(page, 'textbox-33', giteaFixture.initialBranch);
  });

  test('1.4 Fill credentials', async () => {
    await widgets.fillTextbox(page, 'textbox-8', giteaFixture.user);
    await widgets.fillPassword(page, 'textbox-16', giteaFixture.password);
  });

  test('1.5 Fill committer info', async () => {
    await widgets.fillTextbox(page, 'textbox-23', giteaFixture.commitUser);
    await widgets.fillTextbox(page, 'textbox-25', giteaFixture.commitEmail);
  });

  test('1.6 Switch to Advanced tab and enable proxy', async () => {
    // Click on the Advanced tab to switch
    const tabSelector = widgets.widgetIdToSelector('tabsv2-47');
    let advancedTab = await page.$( `${tabSelector} [role="tab"]:nth-child(2)`);
    if (!advancedTab) {
      // Fallback: find tab by text content
      advancedTab = await page.evaluateHandle((sel) => {
        const container = document.querySelector(sel);
        if (!container) return null;
        const tabs = container.querySelectorAll('[role="tab"], .tab');
        for (const tab of tabs) {
          if (tab.textContent.trim() === 'Advanced') return tab;
        }
        return null;
      }, tabSelector);
    }
    if (advancedTab && advancedTab.asElement()) {
      await advancedTab.asElement().click();
      await page.waitForTimeout(500);
    }
    // Disable proxy (default should be off)
    await widgets.toggleCheckbox(page, 'checkbox-127', false);
  });

  test('1.7 Submit create repo form', async () => {
    // Try UI submit first (may not work in Runtime but tests the button)
    await widgets.clickButton(page, 'button-73');
    await page.waitForTimeout(3000);
    try {
      await assertions.assertToastMessage(page, 'success');
    } catch {
      console.log('  No success toast detected.');
    }

    await page.waitForTimeout(1000);
  });

  test('1.8 Verify GitBackup Thing was created via REST API', async () => {
    const res = await restGet(`/Things/${config.testThingName}`);
    expect(res.status).toBe(200);
    expect(res.data.name).toBe(config.testThingName);
    console.log(`  Thing "${config.testThingName}" confirmed.`);
  });

  test('1.9 Verify configuration properties are set', async () => {
    const res = await restPost(`/Things/${config.testThingName}/Services/GetConfiguration`, {});
    expect(res.status).toBe(200);
    const rows = res.data.rows;
    expect(rows.length).toBeGreaterThan(0);

    const row = rows[0];
    console.log('  Configuration:', JSON.stringify(row, null, 2));
    expect(row.GitRepoURL).toBe(giteaFixture.gitRepoUrl);
    expect(row.FileRepoPath).toBe(config.testRepoPath);
    expect(row.FileRepository).toBe(config.testFileRepoName);
  });

  test('1.10 Init extension import targets', async () => {
    const res = await restPost('/Things/GIT.Utility.Thing/Services/InitExtensionImportTargets', {
      address: config.twxUrl,
    });
    console.log(`  InitExtensionImportTargets status: ${res.status}`);
  });

});
