const { launch, close } = require('./utils/browser');
const { login } = require('./utils/auth');
const { openMashup } = require('./utils/navigate');

const cases = [
  ['GitBackup.ExtensionStatus.Mashup', 'git-main'],
  ['GitBackup.Main.Mashup', 'git-repo'],
  ['GitBackup.ModifyRepo.Mashup', 'git-repo-settings'],
];

const loaderError = /__GW|shared runtime|multiple versions of Lit|custom element|not a constructor/i;

async function main() {
  const { page } = await launch();
  const failures = [];
  page.on('pageerror', error => {
    if (loaderError.test(error.message)) failures.push(error.message);
  });
  page.on('console', message => {
    if (message.type() === 'error' && loaderError.test(message.text())) failures.push(message.text());
  });

  try {
    await login(page);
    for (const [mashup, element] of cases) {
      await openMashup(page, mashup);
      await page.waitForTimeout(3000);
      const states = await Promise.all(page.frames().map(async frame => {
        try {
          return await frame.evaluate(tag => ({
            url: location.href,
            sharedRuntime: Boolean(window.__GW),
            customElement: Boolean(customElements.get(tag)),
            runtimeWidget: Boolean(window.TW?.Runtime?.Widgets?.[tag.replaceAll('-', '')]),
            resources: performance.getEntriesByType('resource')
              .map(entry => entry.name)
              .filter(url => url.includes('GitBackupUI')),
            scripts: [...document.scripts].map(script => script.src).filter(url => url.includes('GitBackupUI')),
            text: document.body?.innerText.slice(-2000),
          }), element);
        } catch {
          return null;
        }
      }));
      const state = states.find(item => item?.sharedRuntime && item?.customElement && item?.runtimeWidget);
      console.log(`${mashup}: ${JSON.stringify(states.filter(Boolean))}`);
      if (!state) {
        failures.push(`${mashup} did not register ${element}`);
      }
    }
  } finally {
    await close();
  }

  if (failures.length) throw new Error(failures.join('\n'));
}

main().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
