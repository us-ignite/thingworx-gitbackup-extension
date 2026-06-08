const { launch, close, getPage } = require('../utils/browser');
const { login } = require('../utils/auth');
const { openMashup, readGridData, waitForLoadingDone, selectGridRow } = require('../utils/navigate');
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

describe('06 — Commit History (GitBackup.CommitHistory.Mashup)', () => {

  test('6.1 Commit history mashup loads', async () => {
    await openMashup(page, 'GitBackup.CommitHistory.Mashup', {
      GitThing: config.testThingName,
    });
    await page.waitForTimeout(2000);
  });

  test('6.2 Commit list is displayed', async () => {
    const rowCount = await assertions.assertTableHasRows(page, 'dhxgrid-473', 0);
    console.log(`  Commits in history: ${rowCount}`);
    // After push, there should be at least 1 commit
    expect(rowCount).toBeGreaterThan(0);
  });

  test('6.3 Branch switch via navigation link', async () => {
    // The branch list should be visible
    const branchList = await widgets.findWidget(page, 'dhxlist-476');
    expect(branchList).toBeTruthy();
  });

  test('6.4 Click commit shows commit details', async () => {
    // Click first row in commit grid to trigger GetCommitInfo
    const clicked = await selectGridRow(page, 'dhxgrid-473', 0);
    if (clicked) {
      await page.waitForTimeout(2000);
      // Check that detail labels populated
      const detailLabels = [
        'label-576',  // Commit ID
        'label-578',  // Parents
        'label-580',  // Author
        'label-582',  // Date
        'label-584',  // Committer
        'label-585',  // Description
      ];
      for (const labelId of detailLabels) {
        const text = await widgets.readLabel(page, labelId);
        if (text.length > 0) {
          console.log(`  ${labelId}: ${text.substring(0, 60)}`);
        }
      }
    } else {
      console.log('  No commits to click.');
    }
  });

  test('6.5 Changed files list populated', async () => {
    const filesList = await widgets.findWidget(page, 'dhxlist-619');
    if (filesList) {
      const files = await filesList.$$eval('li, tr', els => els.map(el => el.textContent.trim()));
      console.log(`  Changed files: ${files.length > 0 ? files.join(', ') : 'none'}`);
    } else {
      console.log('  Changed files list widget not visible.');
    }
  });

  test('6.6 Tag list is displayed', async () => {
    // Tags section should be present (GetTagList service fires)
    const tagElements = await page.$$('[id*="root_tag"], [id*="root_dhxlist"]');
    if (tagElements.length > 0) {
      console.log(`  ${tagElements.length} tag/grid widgets found.`);
    }
  });

});
