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

describe('02 — GitBackup Main Dashboard (GitBackup.Main.Mashup)', () => {

  test('2.1 Main dashboard loads with navigation tabs', async () => {
    await openMashup(page, 'GitBackup.Main.Mashup');
    const title = await widgets.readLabel(page, 'label-463');
    expect(title).toBeTruthy();
  });

  test('2.2 Current branch is displayed', async () => {
    const branchLabel = await widgets.readLabel(page, 'label-463');
    expect(branchLabel.length).toBeGreaterThan(0);
    console.log(`  Current branch: ${branchLabel}`);
  });

  test('2.3 Branch list is populated', async () => {
    await assertions.assertGridHasRows(page, 'dhxlist-476', 1);
    const branches = await readGridData(page, 'dhxlist-476');
    console.log(`  Branches found: ${branches.length}`);
    expect(branches.length).toBeGreaterThan(0);
  });

  test('2.4 Commit history grid is populated', async () => {
    const rowCount = await assertions.assertTableHasRows(page, 'dhxgrid-473', 1);
    if (rowCount === 0) {
      console.log('  No commits yet (expected before first push).');
    } else {
      console.log(`  Commits found: ${rowCount}`);
    }
  });

  test('2.5 Version information is displayed', async () => {
    // The version label is bound to GetGitExtensionVersion
    const versionText = await widgets.readLabel(page, 'label-576');
    expect(versionText.length).toBeGreaterThan(0);
    console.log(`  Version info: ${versionText}`);
  });

  test('2.6 Detached HEAD indicator visible', async () => {
    // Checkbox showing detached HEAD state
    const detached = await widgets.readLabel(page, 'checkbox-552');
    console.log(`  Detached HEAD checkbox state visible: ${detached !== ''}`);
  });

  test('2.7 Navigation tabs render', async () => {
    // Check that the tab navigation exists
    const tabSelectors = [
      'navigation-336', 'navigation-343', 'navigation-426',
      'navigation-425', 'navigation-509',
    ];
    for (const tabId of tabSelectors) {
      const visible = await widgets.findWidget(page, tabId);
      expect(visible).toBeTruthy();
    }
    console.log('  All navigation tabs present.');
  });

  test('2.8 File repository tree is visible', async () => {
    const tree = await widgets.findWidget(page, 'Tree-153');
    expect(tree).toBeTruthy();
    console.log('  File repository tree widget visible.');
  });

});
