const config = require('../config');

async function acceptLicense(page) {
  try {
    const hasLicense = await page.evaluate(() => document.body.innerText.includes('I Accept'));
    if (hasLicense) {
      console.log('  Accepting ThingWorx license...');
      const acceptBtn = await page.evaluateHandle(() => {
        const btns = document.querySelectorAll('button');
        for (const b of btns) { if (b.textContent.trim() === 'I Accept') return b; }
        return null;
      });
      if (acceptBtn && acceptBtn.asElement()) {
        await acceptBtn.asElement().click();
        await page.waitForTimeout(3000);
        console.log('  License accepted.');
      }
    }
  } catch {
    // License dialog may not be present
  }
}

async function login(page) {
  const composerUrl = `${config.twxUrl}/Composer/index.html`;

  // Set basic auth credentials first (ThingWorx uses HTTP Basic Auth)
  await page.authenticate({ username: config.twxUser, password: config.twxPass });

  await page.goto(composerUrl, {
    waitUntil: 'load',
    timeout: config.navigationTimeout,
  });

  await acceptLicense(page);

  // Wait for the Composer navigation sidebar to appear
  const navSidebar = '.all-item-heading, .nav-group-heading, [aurelia-app]';
  try {
    await page.waitForSelector(navSidebar, { timeout: 15000 });
    console.log('  Composer loaded.');
  } catch {
    console.log('  Composer loaded (fallback).');
  }
}

async function restAuthHeader() {
  const base64 = Buffer.from(`${config.twxUser}:${config.twxPass}`).toString('base64');
  return {
    Authorization: `Basic ${base64}`,
    'Content-Type': 'application/json',
    'X-XSRF-TOKEN': 'TWX-XSRF-TOKEN-VALUE',
    'X-Requested-By': 'ThingWorx',
    Accept: 'application/json',
  };
}

async function restCall(method, path, body) {
  // Node 18 fetch hangs when HTTP_PROXY is set to empty string
  delete process.env.HTTP_PROXY;
  delete process.env.HTTPS_PROXY;
  delete process.env.http_proxy;
  delete process.env.https_proxy;
  const headers = await restAuthHeader();
  const url = `${config.twxUrl}${path}`;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), config.serviceTimeout);
  const res = await fetch(url, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
    signal: controller.signal,
  });
  clearTimeout(timeout);
  const text = await res.text();
  let data;
  try {
    data = JSON.parse(text);
  } catch {
    data = text;
  }
  if (res.status >= 400) {
    console.log(`  [REST ${method} ${path}] status=${res.status} response=${text.substring(0, 300)}`);
  }
  return { status: res.status, data };
}

async function restPost(path, body) {
  return restCall('POST', path, body);
}

async function restGet(path) {
  return restCall('GET', path);
}

async function restDelete(path) {
  return restCall('DELETE', path);
}

async function restPut(path, body) {
  return restCall('PUT', path, body);
}

module.exports = { login, restGet, restPost, restPut, restDelete, restAuthHeader };
