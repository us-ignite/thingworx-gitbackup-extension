const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const acorn = require('acorn');
for (const part of ['ide', 'runtime']) {
  const Widget = function Widget() {};
  const TW = { IDE: { Widget, Widgets: {} }, Widget, Runtime: { Widgets: {} } };
  const document = { head: { appendChild() {} }, createElement: () => ({ style: {} }) };
  const context = { TW, document, console, Symbol, setTimeout, clearTimeout };
  context.window = context; context.globalThis = context;
  const file = `build/vite/${part}/GitHistoryWidget.${part}.bundle.js`;
  const source = fs.readFileSync(file, 'utf8');
  acorn.parse(source, { ecmaVersion: part === 'ide' ? 5 : 2020 });
  vm.runInNewContext(source, context, { filename: file });
  assert.equal(typeof (part === 'ide' ? TW.IDE : TW.Runtime).Widgets.GitHistoryWidget, 'function');
  console.log(`${part}: GitHistoryWidget registered`);
}
(async () => {
  const { buildLaneLayout, buildSvgPrimitives } = await import('graph-lab');
  const rows = [
    { id: 'merge', predecessorIds: ['main', 'branch'] },
    { id: 'branch', predecessorIds: ['root'] },
    { id: 'main', predecessorIds: ['root'] },
    { id: 'root', predecessorIds: [] },
  ];
  const graph = buildSvgPrimitives(buildLaneLayout(rows), { rowHeight: 56 }, rows);
  assert.equal(graph.nodes.length, rows.length);
  graph.nodes.forEach((node, index) => {
    assert.equal(graph.totalHeight - node.cy - 28, 28 + 56 * index);
  });
  console.log('merge graph aligns with commit rows');
})().catch(error => { console.error(error); process.exitCode = 1; });
{
  const { JSDOM } = require('jsdom');
  const dom = new JSDOM('<!doctype html><html><head></head><body><div id="host"></div></body></html>', { runScripts: 'outside-only' });
  const window = dom.window;
  const values = { MaxEntries: 100, AllBranches: false };
  const events = [];
  function Widget() {
    this.jqElement = [window.document.getElementById('host')];
    this.jqElement.triggerHandler = name => events.push(name);
    this.setProperty = (name, value) => { values[name] = value; };
    this.getProperty = name => values[name];
  }
  window.TW = { Widget, Runtime: { Widgets: {} } };
  window.eval(fs.readFileSync('build/vite/runtime/GitHistoryWidget.runtime.bundle.js', 'utf8'));
  new window.TW.Widget();
  const widget = new window.TW.Runtime.Widgets.GitHistoryWidget();
  widget.jqElement[0].outerHTML = widget.renderHtml().replace('class="widget-content git-history"', 'id="host" class="widget-content git-history"');
  widget.jqElement[0] = window.document.getElementById('host');
  widget.afterRender();
  widget.updateProperty({ TargetProperty: 'Commits', RawSinglePropertyValue: { rows: [
    { CommitID: 'a', CommitName: 'First', ParentCommitIDs: '', AuthorName: 'Author' },
  ] } });
  widget.jqElement[0].querySelector('.gh-commit').click();
  assert.equal(values.SelectedCommitID, 'a');
  assert.equal(events.pop(), 'CommitSelected');
  widget.updateProperty({ TargetProperty: 'CommitInfo', RawSinglePropertyValue: { rows: [
    { CommitID: 'a', CommitDescription: 'First', ChangedFiles: { rows: [] } },
  ] } });
  widget.updateProperty({ TargetProperty: 'TreeEntries', RawSinglePropertyValue: { rows: [
    { Path: 'src/example.txt', Name: 'example.txt' },
  ] } });
  widget.jqElement[0].querySelector('.gh-tree-file').click();
  assert.equal(values.SelectedFilePath, 'src/example.txt');
  assert.equal(events.pop(), 'FileSelected');
  widget.updateProperty({ TargetProperty: 'FileContent', RawSinglePropertyValue: { rows: [
    { Path: 'src/example.txt', Content: '<safe>', Binary: false, Truncated: false },
  ] } });
  assert.equal(widget.jqElement[0].querySelector('.gh-code').textContent, '<safe>');
  console.log('runtime selection, file tree, and content rendering verified');
  dom.window.close();
}
