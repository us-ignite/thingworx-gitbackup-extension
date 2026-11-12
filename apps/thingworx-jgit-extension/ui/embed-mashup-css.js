// Embed src/Shared.css plus the page's src/<Name>.css (if any) into each mashup's
// CustomMashupCss field without rewriting the rest of the exported XML.
const fs = require('fs');
const path = require('path');
const shared = fs.readFileSync(path.join('src', 'Shared.css'), 'utf8').trim();
const pages = ['Home', 'RepositoryRow', 'GitHeader', 'Changes', 'Branches', 'History', 'Log', 'Reflog',
  'ExtensionLog', 'RepositorySettings', 'RepositoryCreate', 'Settings', 'MergeControls', 'ConflictResolution'];
for (const name of pages) {
  const file = `Mashups_GIT.Mashup.${name}.xml`;
  const pageCss = path.join('src', `${name}.css`);
  const css = fs.existsSync(pageCss) ? `${shared}\n\n${fs.readFileSync(pageCss, 'utf8').trim()}` : shared;
  const xml = fs.readFileSync(file, 'utf8');
  const start = xml.indexOf('<mashupContent>');
  const end = xml.indexOf('</mashupContent>', start);
  if (start < 0 || end < 0) throw new Error(`Missing mashupContent in ${file}`);
  const content = xml.slice(start, end);
  const cdata = content.startsWith('<mashupContent><![CDATA[');
  const field = /("CustomMashupCss"\s*:\s*)"(?:\\.|[^"\\])*"/g;
  if ([...content.matchAll(field)].length !== 1) throw new Error(`Expected one CustomMashupCss in ${file}`);
  let encoded = JSON.stringify(css);
  if (cdata) {
    if (encoded.includes(']]>')) throw new Error(`CSS for ${file} contains a CDATA terminator`);
  } else {
    encoded = encoded.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }
  fs.writeFileSync(file, xml.slice(0, start) + content.replace(field, (_, key) => key + encoded) + xml.slice(end));
}
