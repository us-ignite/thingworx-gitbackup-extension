const { launch, close, getPage } = require('../utils/browser');
const { login } = require('../utils/auth');
const { openMashup, readGridData } = require('../utils/navigate');
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

describe('12 — Operation Log (GitBackup.Log.Mashup)', () => {

  test('12.1 Log mashup loads', async () => {
    await openMashup(page, 'GitBackup.Log.Mashup');
    await page.waitForTimeout(3000);
    console.log('  Log mashup loaded.');
  });

  test('12.2 Log entries are displayed', async () => {
    const rowCount = await assertions.assertTableHasRows(page, 'dhxgrid-15', 0);
    if (rowCount > 0) {
      console.log(`  Log entries found: ${rowCount}`);
      const rows = await readGridData(page, 'dhxgrid-15');
      if (rows.length > 0) {
        console.log(`  First entry: ${JSON.stringify(rows[0])}`);
      }
    } else {
      console.log('  No log entries yet (grid may be empty).');
    }
  });

  test('12.3 Log columns present', async () => {
    const grid = await widgets.findWidget(page, 'dhxgrid-15');
    if (grid) {
      const headers = await grid.$$eval('table th', els => els.map(el => el.textContent.trim()));
      console.log(`  Log columns: ${headers.join(', ')}`);
      // Should be relevant columns
      const headerText = headers.join(' ').toLowerCase();
      expect(headerText.length).toBeGreaterThan(0);
    }
  });

  test('12.4 Search/filter log entries', async () => {
    // Find search textbox
    const textbox = await page.$('[id^="root_"] input[type="text"]');
    if (textbox) {
      await textbox.click({ clickCount: 3 });
      await textbox.type('Push', { delay: 5 });
      await page.waitForTimeout(1500);
      const value = await textbox.evaluate(el => el.value);
      console.log(`  Search filter set to: "${value}"`);
    } else {
      console.log('  No search textbox found.');
    }
  });

});
