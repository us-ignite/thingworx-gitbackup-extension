// Embed a TypeScript helper's source and transpiled code into a
// BMTypescriptClassHost widget of a mashup export.
// Usage: node embed-typescript-helper.js <source.ts> <mashup.xml> <widget id>
const fs = require('fs');
const ts = require('typescript');
const [sourcePath, path, widgetId] = process.argv.slice(2);
if (!sourcePath || !path || !widgetId) {
  throw new Error('Usage: node embed-typescript-helper.js <source.ts> <mashup.xml> <widget id>');
}
const source = fs.readFileSync(sourcePath, 'utf8').trim();
const result = ts.transpileModule(source, {compilerOptions: {target: ts.ScriptTarget.ES2017,
  module: ts.ModuleKind.None, experimentalDecorators: true, useDefineForClassFields: false}, reportDiagnostics: true});
const errors = (result.diagnostics || []).filter(d => d.category === ts.DiagnosticCategory.Error);
if (errors.length) throw new Error(errors.map(d => ts.flattenDiagnosticMessageText(d.messageText, '\n')).join('\n'));
const xml = fs.readFileSync(path, 'utf8');
const tag = '<mashupContent><![CDATA[';
const start = xml.indexOf(tag) + tag.length;
const end = xml.indexOf(']]></mashupContent>', start);
if (start < tag.length || end < 0) throw new Error(`Mashup content not found: ${path}`);
const data = JSON.parse(xml.slice(start, end));
function find(widget) {
  if (widget.Properties && widget.Properties.Id === widgetId) return widget;
  for (const child of widget.Widgets || []) { const found = find(child); if (found) return found; }
}
const widget = find(data.UI);
if (!widget) throw new Error(`Helper ${widgetId} not found: ${path}`);
widget.Properties.Code = source;
widget.Properties.Exports = source;
widget.Properties.TranspiledCode = result.outputText;
const serialized = JSON.stringify(data, null, 2);
if (serialized.includes(']]>')) throw new Error('Invalid CDATA sequence in mashup');
fs.writeFileSync(path, xml.slice(0, start) + serialized + xml.slice(end));
