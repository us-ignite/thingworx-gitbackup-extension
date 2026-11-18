// Load both bundles the way ThingWorx does and check rendering and sanitising.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const acorn = require('acorn');
const { JSDOM } = require('jsdom');

const ideSource = fs.readFileSync('ide/GitMarkdownWidget.ide.js', 'utf8');
acorn.parse(ideSource, { ecmaVersion: 5 });
const ideTW = { IDE: { Widgets: {} } };
vm.runInNewContext(ideSource, { TW: ideTW, document: { head: { appendChild() {} }, createElement: () => ({}) } });
const ide = new ideTW.IDE.Widgets.GitMarkdownWidget();
assert.deepEqual(Object.keys(ide.widgetProperties().properties).sort(),
  ['ContentField', 'Data', 'EmptyText', 'Height', 'Markdown', 'Width']);
console.log('ide: GitMarkdownWidget registered');

const runtimeFile = 'build/vite/runtime/GitMarkdownWidget.runtime.bundle.js';
const runtimeSource = fs.readFileSync(runtimeFile, 'utf8');
acorn.parse(runtimeSource, { ecmaVersion: 2020 });
const dom = new JSDOM('<!doctype html><html><head></head><body><div id="host"></div></body></html>', { runScripts: 'outside-only' });
const window = dom.window;
window.TW = { Runtime: { Widgets: {} } };
window.eval(runtimeSource);
const values = { ContentField: 'Content', EmptyText: 'No README in this repository.' };
const widget = new window.TW.Runtime.Widgets.GitMarkdownWidget();
widget.getProperty = name => values[name];
widget.setProperty = (name, value) => { values[name] = value; };
const host = window.document.getElementById('host');
host.outerHTML = widget.renderHtml().replace('class=', 'id="host" class=');
widget.jqElement = [window.document.getElementById('host')];
widget.afterRender();
const root = widget.jqElement[0];
assert.equal(root.textContent, '', 'nothing shown before data arrives');

widget.updateProperty({ TargetProperty: 'Data', RawSinglePropertyValue: { rows: [] } });
assert.equal(root.textContent, 'No README in this repository.');

const readme = [
  '# Demo Repo', '', 'Intro with [docs](docs/guide.md), [site](https://example.com) and [jump](#usage).', '',
  '## Usage', '', '- [x] done', '- [ ] todo', '', '| a | b |', '|---|---|', '| 1 | 2 |', '', '```js', 'alert(1)', '```', '',
  '<script>window.pwned = 1</script><img src=x onerror="window.pwned=2"><a href="javascript:window.pwned=3">x</a>',
  '<h2 id="status" style="color:red" class="x">Status</h2>', '', '![logo](images/logo.png) ![remote](https://example.com/a.png)',
].join('\n');
widget.updateProperty({ TargetProperty: 'Data', RawSinglePropertyValue: { rows: [{ Content: readme }] } });
assert.equal(root.querySelector('h1').textContent, 'Demo Repo');
assert.ok(root.querySelector('table td'), 'GFM table rendered');
assert.deepEqual([...root.querySelectorAll('li')].slice(0, 2).map(li => li.textContent.trim()), ['☑ done', '☐ todo']);
assert.ok(root.querySelector('pre code'), 'code block rendered');
assert.equal(root.querySelector('script'), null);
assert.equal(root.querySelector('[onerror]'), null);
assert.ok(![...root.querySelectorAll('a')].some(a => /javascript:/i.test(a.getAttribute('href') || '')));
assert.equal(root.querySelector('[id]'), null, 'ids stripped');
assert.equal(root.querySelector('[style]'), null, 'inline styles stripped');
const external = [...root.querySelectorAll('a')].find(a => a.textContent === 'site');
assert.equal(external.getAttribute('target'), '_blank');
assert.equal(external.getAttribute('rel'), 'noopener noreferrer');
const local = [...root.querySelectorAll('a')].find(a => a.textContent === 'docs');
assert.equal(local.hasAttribute('href'), false);
assert.equal(local.getAttribute('data-gm'), 'local-link');
const jump = [...root.querySelectorAll('a')].find(a => a.textContent === 'jump');
assert.equal(jump.getAttribute('data-anchor'), 'usage');
assert.equal(jump.getAttribute('href'), '#');
assert.ok(root.querySelector('[data-heading="usage"]'));
assert.deepEqual([...root.querySelectorAll('[data-gm="missing-image"]')].map(e => e.textContent), ['[image: x]', '[image: logo]']);
assert.equal(root.querySelectorAll('img').length, 1, 'only the https image is kept');
assert.equal(window.pwned, undefined);
console.log('runtime: markdown rendered and sanitised');

widget.updateProperty({ TargetProperty: 'Markdown', SinglePropertyValue: '**direct**' });
assert.equal(root.querySelector('strong').textContent, 'direct');
console.log('runtime: Markdown property overrides Data');
