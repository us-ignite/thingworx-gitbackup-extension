const fs = require('fs');
const ts = require('typescript');
const source = fs.readFileSync('src/HomeNavigation.ts', 'utf8').trim();
const result = ts.transpileModule(source, {
  compilerOptions: {target: ts.ScriptTarget.ES2015, module: ts.ModuleKind.None,
    experimentalDecorators: true, useDefineForClassFields: false},
  reportDiagnostics: true
});
const errors = (result.diagnostics || []).filter(d => d.category === ts.DiagnosticCategory.Error);
if (errors.length) throw new Error(errors.map(d => ts.flattenDiagnosticMessageText(d.messageText, '\n')).join('\n'));
const path = 'Mashups_GIT.Mashup.Home.xml';
const xml = fs.readFileSync(path, 'utf8');
const startTag = '<mashupContent><![CDATA[';
const start = xml.indexOf(startTag) + startTag.length;
const end = xml.indexOf(']]></mashupContent>', start);
if (start < startTag.length || end < 0) throw new Error('Home mashup content not found');
const data = JSON.parse(xml.slice(start, end));
function find(widget) {
  if (widget.Properties && widget.Properties.Id === 'home-navigation') return widget;
  for (const child of widget.Widgets || []) { const found = find(child); if (found) return found; }
}
const widget = find(data.UI);
if (!widget) throw new Error('Home navigation host not found');
widget.Properties.Code = source;
widget.Properties.Exports = source;
widget.Properties.TranspiledCode = result.outputText;
const serialized = JSON.stringify(data, null, 2);
if (serialized.includes(']]>')) throw new Error('Invalid CDATA sequence in home mashup');
fs.writeFileSync(path, xml.slice(0, start) + serialized + xml.slice(end));
