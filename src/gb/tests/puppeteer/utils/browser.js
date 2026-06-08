const puppeteer = require('puppeteer');
const config = require('../config');

let browser;
let page;

async function launch() {
  browser = await puppeteer.launch({
    headless: config.headless,
    slowMo: config.slowMo,
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage',
      '--disable-gpu',
    ],
    defaultViewport: {
      width: config.viewportWidth,
      height: config.viewportHeight,
    },
  });
  page = await browser.newPage();
  page.setDefaultTimeout(config.defaultTimeout);
  // Restore deprecated waitForTimeout for backward compatibility
  page.waitForTimeout = (ms) => new Promise(r => setTimeout(r, ms));
  return { browser, page };
}

async function close() {
  if (browser) await browser.close();
}

function getPage() {
  return page;
}

function getBrowser() {
  return browser;
}

module.exports = { launch, close, getPage, getBrowser };
