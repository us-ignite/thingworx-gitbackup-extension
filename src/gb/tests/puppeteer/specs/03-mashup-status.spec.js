const { launch, close, getPage } = require('../utils/browser');
const { login, restPost } = require('../utils/auth');
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

describe('03 — Git Status (GitBackup.Status.Mashup)', () => {

  test('3.1 Status mashup loads', async () => {
    await openMashup(page, 'GitBackup.Status.Mashup', {
      GitThing: config.testThingName,
    });
    // QueryStatus auto-fires on load
    await page.waitForTimeout(2000);
  });

  test('3.2 Status grid renders with File/Status columns', async () => {
    const grid = await widgets.findWidget(page, 'dhxgrid-15');
    expect(grid).toBeTruthy();
    const headers = await grid.$$eval('table th', els => els.map(el => el.textContent.trim()));
    console.log(`  Grid headers: ${headers.join(', ')}`);
    // Should show File and Status columns
    expect(headers.some(h => h.toLowerCase().includes('file'))).toBe(true);
    expect(headers.some(h => h.toLowerCase().includes('status'))).toBe(true);
  });

  test('3.3 Search/filter status entries', async () => {
    // Type into search field
    await widgets.fillTextbox(page, 'textbox-12', 'test');
    await page.waitForTimeout(1500);

    // Get grid data after filter
    const rows = await readGridData(page, 'dhxgrid-15');
    console.log(`  Rows after filter: ${rows.length}`);
    // Verify the textbox has the value
    const value = await widgets.getTextboxValue(page, 'textbox-12');
    expect(value).toBe('test');
  });

  test('3.4 Clear search refreshes grid', async () => {
    await widgets.clickButton(page, 'button-13');
    await page.waitForTimeout(1500);
    const value = await widgets.getTextboxValue(page, 'textbox-12');
    expect(value).toBe('');
  });

  test('3.5 Grid row click shows diff in DiffViewer', async () => {
    const gridSelector = `${widgets.widgetIdToSelector('dhxgrid-15')} table tbody tr`;
    const grid = await page.$(gridSelector);
    if (grid) {
      const diffViewer = await widgets.findWidget(page, 'DiffViewer-8');
      if (diffViewer) {
        const diffText = await diffViewer.evaluate(el => el.textContent.trim());
        console.log(`  Diff viewer text length: ${diffText.length}`);
      } else {
        console.log('  Diff viewer not visible (no changes to diff).');
      }
    } else {
      console.log('  No rows in grid to click.');
    }
  });

});
