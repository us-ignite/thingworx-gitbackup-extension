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
    const info = await page.evaluate(() => {
      const allElements = document.querySelectorAll('[id^="root_"], input, button, ptcs-label, ptcs-textfield');
      const result = [];
      allElements.forEach(el => {
        const rect = el.getBoundingClientRect();
        if (rect.width > 0 && rect.height > 0) {
          result.push({
            id: el.id || '',
            tag: el.tagName,
            type: el.type || '',
            text: (el.textContent || '').trim().substring(0, 80),
            value: el.value || '',
            placeholder: el.placeholder || '',
          });
        }
      });
      return result;
    });
    info.forEach(el => console.log(' ', JSON.stringify(el)));
    return info;
  }

  await logStep('Initial page');

  // Fill in the ptcs-textfield (Git thing name)
  const textfield = await page.$('ptcs-textfield');
  if (textfield) {
    const input = await textfield.$('input');
    if (input) {
      await input.click({ clickCount: 3 });
      await input.type('PuppeteerTestRepo');
      console.log('  Filled textfield');
    } else {
      // Try setting value via evaluate
      console.log('  textfield has no input child, trying evaluate');
    }
  }

  // The simple text inputs
  const textInputs = await page.$$('input[type="text"]');
  console.log('  Text inputs found: ' + textInputs.length);
  if (textInputs.length > 0) {
    await textInputs[0].click({ clickCount: 3 });
    await textInputs[0].type('gituser');
  }

  const passInput = await page.$('input[type="password"]');
  if (passInput) {
    await passInput.type('gitpass');
  }

  await sleep(1000);

  // Check if Next button is now enabled
  const allButtons = await page.$$('button');
  console.log('  Buttons: ' + allButtons.length);
  for (const btn of allButtons) {
    const text = await btn.evaluate(el => el.textContent.trim());
    const disabled = await btn.evaluate(el => el.disabled);
    console.log('    Button: "' + text + '" disabled=' + disabled);
  }

  // Click Next
  for (const btn of allButtons) {
    const text = await btn.evaluate(el => el.textContent.trim());
    const disabled = await btn.evaluate(el => el.disabled);
    if (text === 'Next' && !disabled) {
      console.log('  Clicking Next...');
      await btn.click();
      await sleep(3000);
      await logStep('After Next');
    }
  }

  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
