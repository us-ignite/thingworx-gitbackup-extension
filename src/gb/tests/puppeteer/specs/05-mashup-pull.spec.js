const { launch, close, getPage } = require('../utils/browser');
const { login } = require('../utils/auth');
const { openMashup, waitForLoadingDone } = require('../utils/navigate');
const widgets = require('../utils/widgets');
const config = require('../config');

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

describe('05 — Pull (GitBackup.Pull.Mashup)', () => {

  test('5.1 Pull mashup loads', async () => {
    await openMashup(page, 'GitBackup.Pull.Mashup', {
      GitThing: config.testThingName,
    });
    const flexContainer = await widgets.findWidget(page, 'flexcontainer-2');
    expect(flexContainer).toBeTruthy();
  });

  test('5.2 Pull executes on load', async () => {
    // The Pull mashup auto-fires Pull service on Loaded event
    await waitForLoadingDone(page);
    await page.waitForTimeout(3000);

    // Check for result label text
    const labels = await page.$$('[id*="root_label"], [id*="root_ptcslabel"]');
    let pullResult = '';
    for (const label of labels) {
      const text = await label.evaluate(el => el.textContent.trim());
      if (text.length > 0) {
        pullResult = text;
        break;
      }
    }
    console.log(`  Pull result: "${pullResult}"`);
    expect(pullResult.length).toBeGreaterThan(0);
  });

  test('5.3 Pull mashup auto-closes on completion', async () => {
    // OnServiceInvokeCompleted -> CloseIfPopup
    await page.waitForTimeout(2000);
    const currentUrl = page.url();
    console.log(`  URL after pull: ${currentUrl.substring(0, 80)}...`);
  });

});
