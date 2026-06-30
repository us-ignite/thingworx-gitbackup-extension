import { readFileSync, existsSync, mkdirSync, writeFileSync, readdirSync } from 'fs';
import { resolve, join } from 'path';
import { fileURLToPath } from 'url';
import * as esbuild from 'esbuild';

const __dirname = join(fileURLToPath(import.meta.url), '..');
const root = resolve(__dirname, '..');
const srcDir = join(root, 'src');
const outDir = join(root, 'dist');

function loadExtConfig() {
  const p = join(srcDir, 'extension.json');
  if (existsSync(p)) return JSON.parse(readFileSync(p, 'utf-8'));
  return { name: 'GitBackupWidgets', packageName: 'GitBackupUI', description: '', version: '1.0.0', vendor: '', minimumThingWorxVersion: '9.0.0' };
}

function discoverWidgets() {
  const widgets = [];
  const componentsDir = join(srcDir, 'components');
  if (!existsSync(componentsDir)) return widgets;
  for (const entry of readdirSync(componentsDir, { withFileTypes: true })) {
    if (!entry.isDirectory()) continue;
    const configPath = join(componentsDir, entry.name, `${entry.name}.config.json`);
    if (!existsSync(configPath)) continue;
    try {
      widgets.push({ name: entry.name, config: JSON.parse(readFileSync(configPath, 'utf-8')) });
    } catch { }
  }
  return widgets;
}

function toTwName(name) {
  return name.replace(/-/g, '').toLowerCase();
}

function esc(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

async function build() {
  const ext = loadExtConfig();
  const widgets = discoverWidgets();
  const pn = ext.packageName;

  if (widgets.length === 0) {
    console.log('No widgets found');
    return;
  }

  console.log(`Building ${widgets.length} widgets for extension: ${pn}`);

  const buildDir = join(root, 'build');
  mkdirSync(buildDir, { recursive: true });
  mkdirSync(outDir, { recursive: true });

  for (const w of widgets) {
    const tn = toTwName(w.name);
    const wDisplayName = w.config.name;
    const wDir = join(buildDir, 'ui', wDisplayName);
    mkdirSync(wDir, { recursive: true });
    const componentDir = join(srcDir, 'components', w.name);

    console.log(`  Building: ${w.name} (${tn})`);

    // Build runtime bundle
    const rtContents = readFileSync(join(componentDir, `${w.name}.ts`), 'utf-8');
    const propMapEntries = Object.entries(w.config.properties || {})
      .map(([k, v]) => `"${k}": "${v.src || k.charAt(0).toLowerCase() + k.slice(1)}"`)
      .join(',\n          ');

    const rtWrapper = `
      // Auto-generated ThingWorx runtime wrapper
      const _twName = '${tn}';
      TW.Runtime.Widgets[_twName] = function() {
        let _el = null;
        const _propMap = {
          ${propMapEntries}
        };
        this.renderHtml = function() {
          return '<${w.config.elementName}></${w.config.elementName}>';
        };
        this.afterRender = function() {
          _el = this.jqElement ? (this.jqElement[0] ? this.jqElement[0].firstElementChild : null) : null;
          if (_el) {
            for (const [prop, attr] of Object.entries(_propMap)) {
              const val = this.getProperty ? this.getProperty(prop) : undefined;
              if (val !== undefined) _el[attr] = val;
            }
          }
        };
        this.updateProperty = function(info) {
          if (!_el) return;
          const attr = _propMap[info.TargetProperty];
          if (attr) _el[attr] = info.SinglePropertyValue;
        };
        this.beforeDestroy = function() { _el = null; };
      };
    `;

    const rtEntry = join(componentDir, `_${w.name}.runtime.entry.ts`);
    writeFileSync(rtEntry, rtContents + '\n' + rtWrapper);

    await esbuild.build({
      entryPoints: [rtEntry],
      bundle: true,
      format: 'iife',
      globalName: 'TW',
      outfile: join(wDir, `${tn}.runtime.bundle.js`),
      loader: { '.ts': 'ts' },
      tsconfig: join(root, 'tsconfig.json'),
      minify: true,
      logLevel: 'warning',
    });

    // Build IDE bundle
    const propsJson = JSON.stringify(w.config.properties || {});
    const eventsJson = JSON.stringify(w.config.events || {});
    const ideWrapper = `
      // Auto-generated ThingWorx IDE wrapper
      const _twName = '${tn}';
      TW.IDE.Widgets[_twName] = function() {
        this.widgetIconUrl = function() { return null; };
        this.widgetProperties = function() {
          return {
            name: ${JSON.stringify(w.config.name)},
            description: ${JSON.stringify(w.config.description || '')},
            category: ${JSON.stringify(w.config.category || ['Common'])},
            properties: ${propsJson},
            events: ${eventsJson}
          };
        };
        this.renderHtml = function() {
          return '<div style="padding:32px;border:1px dashed #ccc;border-radius:4px;text-align:center;color:#999;font-family:sans-serif;font-size:14px">${esc(w.config.name)}</div>';
        };
        this.afterSetProperty = function() { return true; };
        this.beforeDestroy = function() {};
      };
    `;

    const ideEntry = join(componentDir, `_${w.name}.ide.entry.ts`);
    writeFileSync(ideEntry, ideWrapper);

    await esbuild.build({
      entryPoints: [ideEntry],
      bundle: true,
      format: 'iife',
      globalName: 'TW',
      outfile: join(wDir, `${tn}.ide.bundle.js`),
      tsconfig: join(root, 'tsconfig.json'),
      minify: true,
      logLevel: 'warning',
    });

    // Clean up temp entries
    try { writeFileSync(rtEntry, ''); } catch {}
    try { writeFileSync(ideEntry, ''); } catch {}
  }

  // Remove stale entities from previous builds (they conflict with main extension)
  const staleEntitiesDir = join(buildDir, 'Entities');
  if (existsSync(staleEntitiesDir)) {
    const { rmSync } = await import('fs');
    rmSync(staleEntitiesDir, { recursive: true, force: true });
  }

  // Generate metadata.xml
  let xml = `<?xml version="1.0" encoding="UTF-8"?>
<Entities majorVersion="9" minorVersion="3" universal="password">
  <ExtensionPackages>
    <ExtensionPackage
      name="${esc(pn)}"
      description="${esc(ext.description)}"
      vendor="${esc(ext.vendor)}"
      packageVersion="${esc(ext.version)}"
      minimumThingWorxVersion="${esc(ext.minimumThingWorxVersion)}"
    />
  </ExtensionPackages>
  <Widgets>\n`;
  for (const w of widgets) {
    const tn = toTwName(w.name);
    xml += `    <Widget name="${esc(w.config.name)}">
      <UIResources>
        <FileResource type="JS" file="${tn}.runtime.bundle.js" description="" isDevelopment="false" isRuntime="true"/>
        <FileResource type="JS" file="${tn}.ide.bundle.js" description="" isDevelopment="true" isRuntime="false"/>
      </UIResources>
    </Widget>\n`;
  }
  xml += `  </Widgets>
</Entities>`;
  writeFileSync(join(buildDir, 'metadata.xml'), xml);

  // Create ZIP file using Node.js built-in zlib
  const { createWriteStream, createReadStream, statSync } = await import('fs');
  const { deflateRawSync } = await import('zlib');
  const { PassThrough, pipeline } = await import('stream');
  const zipPath = join(outDir, `${pn}-v${ext.version}.zip`);
  try {
    // Use archiver if available
    const { createRequire } = await import('module');
    const require = createRequire(import.meta.url);
    const archiver = require('archiver');
    const output = createWriteStream(zipPath);
    const archive = archiver('zip', { zlib: { level: 9 } });
    await new Promise((resolve, reject) => {
      output.on('close', () => {
        console.log(`\nExtension ZIP: ${zipPath} (${archive.pointer()} bytes)`);
        resolve();
      });
      archive.on('error', reject);
      archive.pipe(output);
      archive.directory(buildDir, false);
      archive.finalize();
    });
  } catch {
    console.log(`\nBuild files ready at: ${buildDir}\nTo create extension ZIP: cd "${buildDir}" && zip -r "${zipPath}" .`);
  }
}

build().catch(e => { console.error(e); process.exit(1); });
