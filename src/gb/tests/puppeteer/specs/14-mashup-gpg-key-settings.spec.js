const fs = require('fs');
const path = require('path');
const {launch, close} = require('../utils/browser');
const {login} = require('../utils/auth');
const {openMashup} = require('../utils/navigate');
const config = require('../config');

const privateKey = fs.readFileSync(path.join(__dirname, '..', 'fixtures', 'test-gpg-key.asc'), 'utf8');
const keyInfo = require('../fixtures/test-gpg-key.json');

jest.setTimeout(config.navigationTimeout);

let page;
let widget;
let shadow;

async function replaceValue(handle, value) {
  await handle.click({clickCount: 3});
  await handle.press('Backspace');
  await handle.type(value);
}

async function shadowText() {
  return shadow.evaluate(root => root.textContent || '');
}

async function clickPtcsButton(label) {
  const host = await shadow.$(`ptcs-button[label="${label}"]`);
  const clickable = await host.evaluateHandle(element =>
    element.shadowRoot?.querySelector('button') || element,
  );
  await clickable.asElement().click();
}

beforeAll(async () => {
  ({page} = await launch());
  page.on('response', async response => {
    if (/VerifyGpgKey|SetGpgKey/.test(response.url())) {
      console.log(`GPG service ${response.status()}: ${response.url()} ${await response.text()}`);
    }
  });
  await login(page);
  await openMashup(page, 'GitBackup.ExtensionSettings.Mashup');
  await page.waitForSelector('git-backup-extension-settings', {timeout: 30000});
  widget = await page.$('git-backup-extension-settings');
  shadow = await widget.evaluateHandle(element => element.shadowRoot);
  await page.waitForFunction(
    element => element.shadowRoot?.querySelector('textarea'),
    {timeout: 30000},
    widget,
  );
});

afterAll(async () => {
  await close();
});

describe('14 — GPG signing through the real ThingWorx mashup UI', () => {
  test('14.1 Extension Settings renders its interactive GPG form', async () => {
    expect(await shadow.$('textarea')).toBeTruthy();
    expect(await shadow.$('input[type="password"]')).toBeTruthy();
    expect(await shadow.$('ptcs-button[label="Verify Key"]')).toBeTruthy();
    expect(await shadow.$('ptcs-button[label="Save Key"]')).toBeTruthy();
  });

  test('14.2 Fill the repository, private key, passphrase, and signing checkbox', async () => {
    const label = await shadow.$('input[placeholder="e.g. My Work Key"]');
    const key = await shadow.$('textarea');
    const passphrase = await shadow.$('input[type="password"]');
    const signing = (await shadow.$$('input[type="checkbox"]')).at(-1);

    await replaceValue(label, config.testThingName);
    await replaceValue(key, privateKey);
    await replaceValue(passphrase, keyInfo.passphrase);
    if (!await signing.evaluate(input => input.checked)) await signing.click();

    expect(await label.evaluate(input => input.value)).toBe(config.testThingName);
    expect(await key.evaluate(input => input.value)).toContain('BEGIN PGP PRIVATE KEY BLOCK');
    expect(await signing.evaluate(input => input.checked)).toBe(true);
  });

  test('14.3 Click Verify Key and display the expected fingerprint', async () => {
    await clickPtcsButton('Verify Key');
    await page.waitForFunction(
      (element, fingerprint) => {
        const input = element.shadowRoot?.querySelector('input[placeholder="Will be filled by Verify"]');
        return input?.value.toLowerCase() === fingerprint.toLowerCase();
      },
      {timeout: config.serviceTimeout},
      widget,
      keyInfo.fingerprint,
    );
    const fingerprint = await shadow.$('input[placeholder="Will be filled by Verify"]');
    expect((await fingerprint.evaluate(input => input.value)).toLowerCase()).toBe(keyInfo.fingerprint.toLowerCase());
  });

  test('14.4 Click Save Key and render the saved Signing ON row', async () => {
    await clickPtcsButton('Save Key');
    await page.waitForFunction(
      (element, thing, fingerprint) => {
        const text = element.shadowRoot?.textContent || '';
        return text.includes(thing) && text.toLowerCase().includes(fingerprint.toLowerCase()) && text.includes('Signing ON');
      },
      {timeout: config.serviceTimeout},
      widget,
      config.testThingName,
      keyInfo.fingerprint,
    );
    const text = await shadowText();
    expect(text).toContain(config.testThingName);
    expect(text.toLowerCase()).toContain(keyInfo.fingerprint.toLowerCase());
    expect(text).toContain('Signing ON');
  });
});
