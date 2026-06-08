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

  // 1. How to find inputs relative to their widget containers
  //    e.g. root_textbox-8 contains an input, but the input has no ID
  const mapping = await page.evaluate(() => {
    const result = [];
    // Find all widget containers (elements with id starting with root_)
    const containers = document.querySelectorAll('[id^="root_"]');
    containers.forEach(container => {
      const containerId = container.id;
      // Check for inputs inside
      const inputs = container.querySelectorAll('input, textarea, select, button');
      if (inputs.length > 0) {
        Array.from(inputs).forEach(input => {
          result.push({
            containerId: containerId.substring(0, 50),
            inputTag: input.tagName,
            inputType: input.type || '',
            inputId: input.id || '',
            inputName: input.name || '',
            inputClass: (input.className || '').substring(0, 50),
          });
        });
      }
    });
    return result;
  });

  console.log('=== Widget containers with input elements ===');
  mapping.forEach(m => console.log(' ', JSON.stringify(m)));

  // 2. Check the ptcstextfield shadow DOM
  const ptcsInfo = await page.evaluate(() => {
    const el = document.querySelector('#root_ptcstextfield-136');
    if (!el) return { error: 'not found' };
    const info = {
      tag: el.tagName,
      id: el.id,
      shadowRoot: !!el.shadowRoot,
      innerHTML: el.innerHTML.substring(0, 500),
    };
    if (el.shadowRoot) {
      info.shadowHTML = el.shadowRoot.innerHTML.substring(0, 500);
    }
    return info;
  });
  console.log('\n=== ptcstextfield-136 info ===');
  console.log(' ', JSON.stringify(ptcsInfo, null, 2));

  // 3. What's the best way to find a specific widget's input?
  //    For textbox-8: the widget container is root_textbox-8, but the input is a sibling or child?
  const textbox8Info = await page.evaluate(() => {
    const container = document.querySelector('#root_textbox-8');
    if (!container) return { error: 'not found' };
    const info = {
      id: container.id,
      className: container.className,
      innerHTML: container.innerHTML.substring(0, 1000),
    };
    // Find closest input
    const parentDiv = container.closest('[id^="root_container-"]');
    if (parentDiv) {
      const inputs = parentDiv.querySelectorAll('input');
      info.nearbyInputs = Array.from(inputs).map(i => ({
        id: i.id,
        type: i.type,
        placeholder: i.placeholder,
        value: i.value,
      }));
    }
    return info;
  });
  console.log('\n=== textbox-8 info ===');
  console.log(' ', JSON.stringify(textbox8Info, null, 2));

  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
