const config = require('../config');

function widgetIdToSelector(widgetId) {
  return `[data-widgetid="${widgetId}"], [id="root_${widgetId}"]`;
}

function widgetInputSelector(widgetId) {
  const base = widgetIdToSelector(widgetId);
  return `${base} input[type="text"], ${base} input:not([type="password"]):not([type="checkbox"]):not([type="hidden"]), ${base} textarea`;
}

function widgetPasswordSelector(widgetId) {
  const base = widgetIdToSelector(widgetId);
  return `${base} input[type="password"]`;
}

function widgetCheckboxSelector(widgetId) {
  return `[id="root_${widgetId}-input"], ${widgetIdToSelector(widgetId)} input[type="checkbox"]`;
}

function widgetButtonSelector(widgetId) {
  return `${widgetIdToSelector(widgetId)} button, [id="root_${widgetId}"]`;
}

async function findWidget(page, widgetId) {
  const sel = widgetIdToSelector(widgetId);
  return page.$(sel);
}

async function fillTextbox(page, widgetId, value) {
  // Handle ptcs-textfield (web component with shadow DOM)
  const ptcsSelector = `[id="root_${widgetId}"]`;
  const ptcsEl = await page.$(ptcsSelector);
  if (ptcsEl) {
    const tag = await ptcsEl.evaluate(el => el.tagName);
    if (tag === 'PTCS-TEXTFIELD') {
      const input = await ptcsEl.evaluateHandle(el => el.shadowRoot.querySelector('#input'));
      if (input && input.asElement()) {
        await input.asElement().evaluate((el, val) => {
          el.value = val;
          el.dispatchEvent(new Event('input', { bubbles: true }));
          el.dispatchEvent(new Event('change', { bubbles: true }));
        }, value);
        return;
      }
    }
  }

  const sel = widgetInputSelector(widgetId);
  const el = await page.$(sel);
  if (!el) throw new Error(`Textbox ${widgetId} not found`);
  // Use evaluate-based fill to handle hidden elements
  await el.evaluate((el, val) => {
    el.value = val;
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
  }, value);
}

async function fillPassword(page, widgetId, value) {
  const sel = widgetPasswordSelector(widgetId);
  const el = await page.$(sel);
  if (!el) throw new Error(`Password field ${widgetId} not found`);
  await el.evaluate((el, val) => {
    el.value = val;
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
  }, value);
}

async function fillTextarea(page, widgetId, value) {
  const sel = widgetInputSelector(widgetId);
  const el = await page.$(sel);
  if (!el) throw new Error(`Textarea ${widgetId} not found`);
  await el.click({ clickCount: 3 });
  await el.type(value, { delay: 5 });
}

async function clickButton(page, widgetId) {
  const sel = widgetButtonSelector(widgetId);
  let el = await page.$(sel);
  if (!el) throw new Error(`Button ${widgetId} not found`);
  const tag = await el.evaluate(el => el.tagName);
  if (tag === 'DIV' || tag === 'SPAN') {
    const btn = await el.$('button, input[type="submit"], [role="button"]');
    if (btn) el = btn;
  }
  // Use evaluate to click (avoids "not clickable" issues with disabled/offscreen elements)
  await el.evaluate(el => el.click());
  await page.waitForTimeout(300);
}

async function toggleCheckbox(page, widgetId, checked = true) {
  const sel = widgetCheckboxSelector(widgetId);
  const el = await page.$(sel);
  if (!el) throw new Error(`Checkbox ${widgetId} not found`);
  const isChecked = await el.evaluate(el => el.checked);
  if (isChecked !== checked) {
    await el.evaluate(el => el.click());
  }
}

async function readLabel(page, widgetId) {
  const el = await findWidget(page, widgetId);
  if (!el) return '';
  return el.evaluate(el => el.textContent.trim());
}

async function selectDropdown(page, widgetId, optionText) {
  const base = widgetIdToSelector(widgetId);
  const selector = `${base} select, ${base} [role="listbox"]`;
  const el = await page.$(selector);
  if (el) {
    const tagName = await el.evaluate(el => el.tagName);
    if (tagName === 'SELECT') {
      await el.select(optionText);
      return;
    }
  }
  const dropdownButton = await page.$(`${base} .widget-dropdown, ${base} .dropdown-toggle`);
  if (dropdownButton) {
    await dropdownButton.click();
    await page.waitForTimeout(300);
    const option = await page.$(`[role="option"]:has-text("${optionText}"), li:has-text("${optionText}")`);
    if (option) {
      await option.click();
    }
  }
}

async function openEntityPicker(page, widgetId, entityName) {
  const base = widgetIdToSelector(widgetId);
  const input = await page.$(`${base} input`);
  if (input) {
    await input.evaluate((el, val) => {
      el.value = val;
      el.dispatchEvent(new Event('input', { bubbles: true }));
    }, entityName);
    await page.waitForTimeout(500);
  }
}

async function getTextboxValue(page, widgetId) {
  const selector = widgetInputSelector(widgetId);
  const el = await page.$(selector);
  if (!el) return '';
  return el.evaluate(el => el.value);
}

async function waitForWidgetVisible(page, widgetId, timeout = config.defaultTimeout) {
  const selector = widgetIdToSelector(widgetId);
  await page.waitForSelector(selector, { timeout });
}

async function clickTabByLabel(page, tabWidgetId, labelText) {
  // Click a tab by its label text within a tabs widget
  const base = widgetIdToSelector(tabWidgetId);
  const tab = await page.evaluateHandle((sel, text) => {
    const container = document.querySelector(sel);
    if (!container) return null;
    const tabs = container.querySelectorAll('[role="tab"], .tab, [class*="tab"]');
    for (const t of tabs) {
      if (t.textContent.trim() === text) return t;
    }
    return null;
  }, base, labelText);
  if (tab && tab.asElement()) {
    await tab.asElement().evaluate(el => el.click());
    await page.waitForTimeout(500);
    return true;
  }
  return false;
}

module.exports = {
  fillTextbox, fillPassword, fillTextarea, clickButton, toggleCheckbox,
  readLabel, selectDropdown, openEntityPicker, getTextboxValue, waitForWidgetVisible,
  widgetIdToSelector, findWidget, clickTabByLabel,
};
