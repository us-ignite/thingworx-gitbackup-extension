const puppeteer = require('puppeteer');
const config = require('./config');
const sleep = ms => new Promise(r => setTimeout(r, ms));

(async () => {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage'],
  });
  const page = await browser.newPage();
  page.waitForTimeout = ms => new Promise(r => setTimeout(r, ms));

  await page.authenticate({ username: config.twxUser, password: config.twxPass });
  await page.goto(config.twxUrl + '/Runtime/index.html?mashup=GitBackup.NewRepo.Mashup', {
    waitUntil: 'load', timeout: 30000,
  });
  await sleep(5000);

  // Fill ptcstextfield-136 via shadow DOM
  const ptcsEl = await page.$('#root_ptcstextfield-136');
  if (ptcsEl) {
    const input = await ptcsEl.evaluateHandle(function(el) {
      return el.shadowRoot.querySelector('#input');
    });
    if (input && input.asElement()) {
      await input.asElement().evaluate(function(el) {
        el.value = 'PuppeteerTestRepo';
        el.dispatchEvent(new Event('input', {bubbles: true}));
      });
      console.log('Filled ptcstextfield-136');
    }
  }

  // Fill textbox-8 (Git username)
  const userInput = await page.$('#root_textbox-8 input');
  if (userInput) {
    await userInput.evaluate(function(el) {
      el.value = 'testadmin';
      el.dispatchEvent(new Event('input', {bubbles: true}));
    });
    console.log('Filled textbox-8');
  }

  // Click 'Repo settings' tab label
  console.log('Clicking Repo settings tab...');
  const repoClicked = await page.evaluate(function() {
    const labels = document.querySelectorAll('#root_label-54');
    for (const l of labels) {
      if (l.textContent.trim() === 'Repo settings') {
        l.click();
        return true;
      }
    }
    return false;
  });
  console.log('Repo settings clicked:', repoClicked);
  await sleep(1000);

  // Check visible widgets
  const visibleIds = await page.evaluate(function() {
    const result = [];
    document.querySelectorAll('[id^="root_"]').forEach(function(el) {
      const rect = el.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0) {
        const shortId = el.id.replace(/^root_/, '');
        if (!shortId.includes('-bounding') && !shortId.includes('-box')) {
          result.push(shortId);
        }
      }
    });
    return result;
  });
  console.log('\nVisible widgets after tab click:');
  visibleIds.forEach(function(id) { console.log(' ', id); });

  // Check specific widgets
  const t23 = await page.$('#root_textbox-23 input');
  console.log('\ntextbox-23 input found:', !!t23);

  const btn73 = await page.$('#root_button-73 button');
  console.log('button-73 button found:', !!btn73);

  const cb127 = await page.$('#root_checkbox-127-input');
  console.log('checkbox-127 input found:', !!cb127);

  // Check all textboxes and buttons that exist
  const formWidgets = ['textbox-23', 'textbox-25', 'entitypicker-29', 'textbox-31', 'textbox-33', 'checkbox-127', 'button-73'];
  for (const w of formWidgets) {
    const el = await page.$('#root_' + w);
    console.log('Widget ' + w + ' exists:', !!el, 'visible:', el ? await el.evaluate(function(e) {
      const r = e.getBoundingClientRect();
      return r.width > 0 && r.height > 0;
    }) : false);
  }

  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
