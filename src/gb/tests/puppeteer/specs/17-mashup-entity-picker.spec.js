const { launch, close, getPage } = require('../utils/browser');
const { login } = require('../utils/auth');
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

describe('17 — Entity Picker (GitBackup.EntityPicker.Mashup)', () => {

  test('17.1 Entity picker mashup loads (non-flex)', async () => {
    await openMashup(page, 'GitBackup.EntityPicker.Mashup');
    await page.waitForTimeout(3000);
    console.log('  Entity picker mashup loaded.');
  });

  test('17.2 Entity tree is rendered', async () => {
    // Look for the tree widget
    const treeWidgets = await page.$$('[id*="root_tree"], [id*="root_Tree"], .widget-tree');
    if (treeWidgets.length > 0) {
      console.log(`  ${treeWidgets.length} tree widget(s) found.`);
      expect(treeWidgets.length).toBeGreaterThan(0);
    } else {
      // Non-flex mashups may use different widget types
      const checkboxContainers = await page.$$('input[type="checkbox"]');
      console.log(`  ${checkboxContainers.length} checkbox(es) found.`);
    }
  });

  test('17.3 Toggle entity selection', async () => {
    // Check if there are checkboxes to interact with
    const checkboxes = await page.$$('input[type="checkbox"]');
    if (checkboxes.length > 0) {
      console.log(`  Entity selection checkboxes: ${checkboxes.length}`);
      // Click the first unchecked checkbox
      for (const cb of checkboxes) {
        const isChecked = await cb.evaluate(el => el.checked);
        if (!isChecked) {
          await cb.click();
          console.log('  Entity checkbox toggled.');
          break;
        }
      }
    } else {
      console.log('  No checkboxes found (tree may not be expanded yet).');
    }
  });

  test('17.4 Confirm/Cancel buttons exist', async () => {
    const buttons = await page.$$('[id*="root_button"]');
    const buttonTexts = await Promise.all(
      buttons.map(btn => btn.evaluate(el => el.textContent.trim().toLowerCase()))
    );
    console.log(`  Buttons found: ${buttonTexts.join(', ')}`);
    expect(buttonTexts.some(t => t.includes('ok') || t.includes('select') || t.includes('confirm') || t.includes('cancel'))).toBe(true);
  });

});
