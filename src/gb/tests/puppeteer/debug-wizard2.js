const puppeteer = require('puppeteer');
const config = require('./config');
const sleep = ms => new Promise(r => setTimeout(r, ms));

(async () => {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage'],
  });
  const page = await browser.newPage();

  await page.authenticate({ username: config.twxUser, password: config.twxPass });
  await page.goto(config.twxUrl + '/Runtime/index.html?mashup=GitBackup.NewRepo.Mashup', {
    waitUntil: 'load', timeout: 30000,
  });
  await sleep(5000);

  async function logStep(label) {
    console.log('\n=== ' + label + ' ===');
    const text = await page.evaluate(() => document.body.innerText);
    console.log('  Text:', text.replace(/\n/g, '\\n').substring(0, 300));
    const info = await page.evaluate(() => {
      const allElements = document.querySelectorAll('[id^="root_"], input, button');
      const result = [];
      allElements.forEach(el => {
        const rect = el.getBoundingClientRect();
        if (rect.width > 0 && rect.height > 0) {
          result.push({
            id: (el.id || '').substring(0, 50),
            tag: el.tagName,
            type: el.type || '',
            text: (el.textContent || '').trim().substring(0, 40),
          });
        }
      });
      return result;
    });
    info.forEach(el => console.log('  ' + JSON.stringify(el)));
  }

  await logStep('Initial');

  // Fill ptcstextfield-136 (Git thing name) — it's a web component, need special handling
  const textfieldEl = await page.$('#root_ptcstextfield-136');
  if (textfieldEl) {
    // Some web components use shadow DOM; try setting value directly
    await page.evaluate(() => {
      const el = document.querySelector('#root_ptcstextfield-136');
      if (el) {
        // Try the web component API
        if (el.value !== undefined) el.value = 'PuppeteerTestRepo';
        if (el.text !== undefined) el.text = 'PuppeteerTestRepo';
        // Dispatch input event
        el.dispatchEvent(new Event('input', { bubbles: true }));
      }
    });
    console.log('  Set ptcstextfield-136 value');
  }

  // Fill textbox-8 (Git username) — this is a DIV, find the actual INPUT child
  const inputs = await page.$$('input[type="text"]');
  console.log('  Text inputs: ' + inputs.length);
  // The first text input seems to be the one associated with textbox-8
  if (inputs.length > 0) {
    await inputs[0].click({ clickCount: 3 });
    await inputs[0].type('testadmin');
  }

  // Fill textbox-16 (Git account password)
  const passInput = await page.$('input[type="password"]');
  if (passInput) {
    await passInput.type('testadmin123');
  }

  await sleep(1000);

  // Check buttons states
  const allButtons = await page.$$('button');
  for (const btn of allButtons) {
    const text = await btn.evaluate(el => el.textContent.trim());
    const disabled = await btn.evaluate(el => el.disabled);
    console.log('  Button: "' + text + '" disabled=' + disabled);
  }

  // Click Next
  for (const btn of allButtons) {
    const text = await btn.evaluate(el => el.textContent.trim());
    const disabled = await btn.evaluate(el => el.disabled);
    if (text === 'Next' && !disabled) {
      console.log('\n  Clicking Next...');
      await btn.click();
      await sleep(3000);
      await logStep('After Next');
      break;
    }
  }

  // Try clicking Advanced button if visible
  const advancedBtns = await page.$$('button');
  for (const btn of advancedBtns) {
    const text = await btn.evaluate(el => el.textContent.trim());
    if (text === 'Advanced') {
      const disabled = await btn.evaluate(el => el.disabled);
      console.log('\n  Advanced button disabled=' + disabled);
      if (!disabled) {
        await btn.click();
        await sleep(2000);
        await logStep('After Advanced');
        break;
      }
    }
  }

  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
