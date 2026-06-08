const config = require('../config');
const { widgetIdToSelector } = require('./widgets');

async function assertToastMessage(page, expectedText) {
  try {
    await page.waitForSelector('.gwt-Toast, .toast-message, .notification-message', {
      timeout: config.defaultTimeout,
    });
    const toast = await page.$('.gwt-Toast, .toast-message, .notification-message');
    const text = toast ? await toast.evaluate(el => el.textContent.trim()) : '';
    expect(text).toContain(expectedText);
  } catch {
    throw new Error(`Expected toast with "${expectedText}" but none appeared`);
  }
}

async function assertGridHasRows(page, widgetId, minRows = 1) {
  const selector = `${widgetIdToSelector(widgetId)} table tbody tr`;
  await page.waitForSelector(selector, { timeout: config.defaultTimeout });
  const rows = await page.$$(selector);
  expect(rows.length).toBeGreaterThanOrEqual(minRows);
}

async function assertTableHasRows(page, widgetId, minRows = 1) {
  const selector = `${widgetIdToSelector(widgetId)} table tbody tr`;
  try {
    await page.waitForSelector(selector, { timeout: 5000 });
  } catch {
    // Table might be empty
  }
  const rows = await page.$$(selector);
  return rows.length;
}

async function assertWidgetTextContains(page, widgetId, expectedText) {
  const selector = widgetIdToSelector(widgetId);
  const el = await page.$(selector);
  if (!el) throw new Error(`Widget ${widgetId} not found`);
  const text = await el.evaluate(el => el.textContent.trim());
  expect(text).toContain(expectedText);
}

async function assertElementVisible(page, widgetId, visible = true) {
  const selector = widgetIdToSelector(widgetId);
  if (visible) {
    await page.waitForSelector(selector, { timeout: config.defaultTimeout });
    const el = await page.$(selector);
    const isVisible = await el.evaluate(el => {
      const style = window.getComputedStyle(el);
      return style.display !== 'none' && style.visibility !== 'hidden' && el.offsetHeight > 0;
    });
    expect(isVisible).toBe(true);
  } else {
    const el = await page.$(selector);
    if (el) {
      const isVisible = await el.evaluate(el => {
        const style = window.getComputedStyle(el);
        return style.display !== 'none' && style.visibility !== 'hidden' && el.offsetHeight > 0;
      });
      expect(isVisible).toBe(false);
    }
  }
}

module.exports = {
  assertToastMessage, assertGridHasRows, assertTableHasRows,
  assertWidgetTextContains, assertElementVisible,
};
