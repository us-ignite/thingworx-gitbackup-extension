const config = require('../config');

const widgetSelector = (widgetId) => `[data-widgetid="${widgetId}"], [id="root_${widgetId}"]`;

async function openMashup(page, mashupName, params = {}) {
  const paramStr = Object.entries(params)
    .map(([k, v]) => `${k}=${encodeURIComponent(v)}`)
    .join('&');
  // Use the Runtime URL to load mashups directly (Composer hash routing doesn't work in 9.7.5)
  const url = `${config.twxUrl}/Runtime/index.html?mashup=${mashupName}&__enableWC=true${paramStr ? '&' + paramStr : ''}`;

  console.log(`  Navigating to ${mashupName}...`);
  await page.goto(url, {
    waitUntil: 'load',
    timeout: config.navigationTimeout,
  });

  // Wait for the mashup to render
  const runtimeSelector = '#runtime-workspace, .runtime-wrapper, [id^="root_"]';
  try {
    await page.waitForSelector(runtimeSelector, {
      timeout: 30000,
    });
  } catch {
    await new Promise(r => setTimeout(r, 5000));
  }

  // Wait for loading to complete
  await waitForLoadingDone(page);
}

async function waitForLoadingDone(page) {
  try {
    await page.waitForFunction(
      () => !document.querySelector('.loading-mask, .loading-indicator, .gwt-PopupPanel'),
      { timeout: config.serviceTimeout }
    );
  } catch {
    // Loading may have already completed
  }
  // Small settle time
  await page.evaluate(() => new Promise(r => setTimeout(r, 500)));
}

async function selectGridRow(page, gridWidgetId, rowIndex = 0) {
  const selector = `${widgetSelector(gridWidgetId)} table tbody tr`;
  const rows = await page.$$(selector);
  if (rows.length > rowIndex) {
    await rows[rowIndex].click();
    return true;
  }
  return false;
}

async function readGridData(page, gridWidgetId) {
  const selector = `${widgetSelector(gridWidgetId)} table tbody tr`;
  const rows = await page.$$(selector);
  const result = [];
  for (const row of rows) {
    const cells = await row.$$('td');
    const rowData = [];
    for (const cell of cells) {
      rowData.push(await cell.evaluate(el => el.textContent.trim()));
    }
    result.push(rowData);
  }
  return result;
}

module.exports = { openMashup, waitForLoadingDone, selectGridRow, readGridData, widgetSelector };
