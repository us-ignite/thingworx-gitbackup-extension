const fs = require('fs');
const ts = require('typescript');
const source = fs.readFileSync('src/HomeNavigation.ts', 'utf8').trim();
const popoverSource = fs.readFileSync('src/GitResultPopover.ts', 'utf8').trim();
function transpile(code) {
const result = ts.transpileModule(code, {
  compilerOptions: {target: ts.ScriptTarget.ES2015, module: ts.ModuleKind.None,
    experimentalDecorators: true, useDefineForClassFields: false},
  reportDiagnostics: true
});
const errors = (result.diagnostics || []).filter(d => d.category === ts.DiagnosticCategory.Error);
if (errors.length) throw new Error(errors.map(d => ts.flattenDiagnosticMessageText(d.messageText, '\n')).join('\n'));
return result.outputText;
}
const javascript = transpile(source);
const popoverJavascript = transpile(popoverSource);
for (const path of ['Mashups_GIT.Mashup.Home.xml', 'Mashups_GIT.Mashup.RepositoryRow.xml',
  'Mashups_GIT.Mashup.Changes.xml', 'Mashups_GIT.Mashup.Settings.xml']) {
  const xml = fs.readFileSync(path, 'utf8');
  const startTag = '<mashupContent><![CDATA[';
  const start = xml.indexOf(startTag) + startTag.length;
  const end = xml.indexOf(']]></mashupContent>', start);
  if (start < startTag.length || end < 0) throw new Error(`Mashup content not found: ${path}`);
  const data = JSON.parse(xml.slice(start, end));
  let found = 0;
  function update(widget) {
    if (widget.Properties && ['home-navigation', 'git-result-popover', 'breadcrumb-navigation'].includes(widget.Properties.Id)) {
      const isPopover = widget.Properties.Id === 'git-result-popover';
      widget.Properties.Code = isPopover ? popoverSource : source;
      widget.Properties.Exports = isPopover ? popoverSource : source;
      widget.Properties.TranspiledCode = isPopover ? popoverJavascript : javascript;
      found++;
    }
    for (const child of widget.Widgets || []) update(child);
  }
  update(data.UI);
  if (!found) throw new Error(`Navigation host not found: ${path}`);
  const serialized = JSON.stringify(data, null, 2);
  if (serialized.includes(']]>')) throw new Error('Invalid CDATA sequence in mashup');
  fs.writeFileSync(path, xml.slice(0, start) + serialized + xml.slice(end));
}
