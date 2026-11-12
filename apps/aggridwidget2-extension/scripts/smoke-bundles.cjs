const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const acorn = require('acorn');
const esbuild = require('esbuild');

const formatterModule = { exports: {} };
const formatterBundle = esbuild.buildSync({
  entryPoints: [path.join(__dirname, '..', 'runtime', 'src', 'date-format.ts')],
  bundle: true,
  platform: 'node',
  format: 'cjs',
  write: false,
}).outputFiles[0].text;
vm.runInNewContext(formatterBundle, {
  module: formatterModule, exports: formatterModule.exports, Date, require,
});
const dateValue = new Date(2025, 0, 2, 15, 4, 5, 678).getTime();
assert.equal(formatterModule.exports.formatDate(dateValue, 'DD-MM-YYYY'), '02-01-2025');
assert.equal(formatterModule.exports.formatDate(dateValue, 'ddd, MMM Do YYYY'), 'Thu, Jan 2nd 2025');
assert.equal(formatterModule.exports.formatDate(dateValue, 'L LT'), '01/02/2025 3:04 PM');

for (const artifact of ['ide', 'runtime']) {
  const Widget = function Widget() {};
  const TW = {
    IDE: { Widget, Widgets: {} },
    Widget,
    Runtime: { Widgets: {} },
  };
  const styles = [];
  const document = {
    baseURI: 'http://localhost/',
    documentElement: { style: {} },
    head: { appendChild: (element) => styles.push(element) },
    createElement: () => ({ style: {}, appendChild() {}, setAttribute() {} }),
  };
  function $(element) { return { element }; }
  $.fn = {};
  $.extend = Object.assign;

  const context = {
    TW, document, $, Headers, URL, Symbol, console, setTimeout, clearTimeout,
    navigator: { userAgent: 'Mozilla/5.0' },
  };
  context.window = context;
  context.self = context;
  context.globalThis = context;

  const bundle = path.join(__dirname, '..', 'build', 'vite', artifact,
    `agGridWidget2.${artifact}.bundle.js`);
  const source = fs.readFileSync(bundle, 'utf8');
  acorn.parse(source, { ecmaVersion: artifact === 'ide' ? 5 : 2020, sourceType: 'script' });
  vm.runInNewContext(source, context, {
    filename: bundle,
    timeout: 15000,
  });

  const registry = artifact === 'ide' ? TW.IDE.Widgets : TW.Runtime.Widgets;
  assert.equal(typeof registry.AgGridWidget2, 'function');
  assert.ok(styles.length > 0, `${artifact} styles were not injected`);
  if (artifact === 'runtime') {
    assert.equal(typeof $.fn.multipleSelect, 'function');
    assert.equal(typeof context.relatedDropdownValues, 'object');
  }
  console.log(`${artifact} bundle registers AgGridWidget2`);
}
