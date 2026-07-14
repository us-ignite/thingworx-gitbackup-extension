import { readFileSync, existsSync, mkdirSync, writeFileSync, readdirSync, rmSync, createWriteStream, copyFileSync } from 'fs';
import { resolve, join, dirname } from 'path';
import { fileURLToPath } from 'url';
import * as esbuild from 'esbuild';

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = resolve(__dirname, '..');
const srcDir = join(root, 'src');
const outDir = join(root, 'dist');

function loadJson(p) {
  return JSON.parse(readFileSync(p, 'utf-8'));
}

function esc(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function toTwName(name) {
  return name.replace(/-/g, '').toLowerCase();
}

function packageWidgetName(name) {
  return process.env.GITBACKUP_WIDGET_NAMES_DASHED === 'true' ? name : toTwName(name);
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
      widgets.push({ name: entry.name, config: loadJson(configPath) });
    } catch { }
  }
  return widgets;
}

const sharedLitPlugin = {
  name: 'shared-lit',
  setup(build) {
    const fromShared = (...names) => ({
      contents: `const shared = window.__GW;
        if (!shared) throw new Error('GitBackup shared runtime was not loaded before a widget bundle');
        const { ${names.join(', ')} } = shared;
        export { ${names.join(', ')} };`,
      loader: 'js',
    });
    build.onResolve({ filter: /^lit$/ }, args => ({
      path: 'lit', namespace: 'gw-shared',
    }));
    build.onResolve({ filter: /^lit\// }, args => ({
      path: args.path, namespace: 'gw-shared',
    }));
    build.onResolve({ filter: /\/lib\/(twx-service|widget-bridge)\.js$/ }, args => ({
      path: args.path, namespace: 'gw-shared',
    }));
    build.onResolve({ filter: /\/components\/git-base\.js$/ }, args => ({
      path: args.path, namespace: 'gw-shared',
    }));

    build.onLoad({ filter: /.*/, namespace: 'gw-shared' }, args => {
      if (args.path === 'lit') {
        return fromShared('LitElement', 'html', 'css');
      }
      if (args.path === 'lit/decorators.js') {
        return fromShared('state', 'property');
      }
      if (args.path.includes('lit/directives/unsafe-html')) {
        return fromShared('unsafeHTML');
      }
      if (args.path.includes('lit/directives/')) {
        const part = args.path.split('/').pop().replace('.js', '');
        const camel = part.replace(/-([a-z])/g, (_, c) => c.toUpperCase());
        return fromShared(camel);
      }
      if (args.path.endsWith('/lib/twx-service.js')) {
        return fromShared('TwxService', 'twx');
      }
      if (args.path.endsWith('/lib/widget-bridge.js')) {
        return fromShared('WidgetBridge');
      }
      if (args.path.endsWith('/components/git-base.js')) {
        return fromShared('GitElementBase');
      }
      console.warn('  [plugin] unhandled shared module:', args.path);
      return null;
    });
  },
};

function verifyRuntimeBundle(result, widgetName) {
  const inputs = Object.keys(result.metafile?.inputs ?? {});
  const forbidden = inputs.filter(p =>
    p.includes('/node_modules/lit') ||
    p.includes('/node_modules/@lit/') ||
    p.includes('/twx-wc-sdk/')
  );
  if (forbidden.length) {
    throw new Error(`${widgetName}: shared dependencies leaked into the runtime bundle:\n${forbidden.join('\n')}`);
  }
}

async function build() {
  const ext = loadJson(join(srcDir, 'extension.json'));
  const widgets = discoverWidgets();
  const pn = process.env.GITBACKUP_UI_PACKAGE_NAME || ext.packageName;
  const version = process.env.GITBACKUP_UI_VERSION || ext.version;

  if (widgets.length === 0) {
    console.log('No widgets found');
    return;
  }

  console.log(`Building ${widgets.length} widgets for extension: ${pn}`);
  const buildDir = join(root, 'build');
  rmSync(join(buildDir, 'ui'), { recursive: true, force: true });
  mkdirSync(buildDir, { recursive: true });
  mkdirSync(outDir, { recursive: true });

  const sharedDir = join(buildDir, 'ui', '0-gitbackup-shared-runtime');
  mkdirSync(sharedDir, { recursive: true });

  console.log('  Building shared runtime...');
  await esbuild.build({
    entryPoints: [join(srcDir, 'shared-runtime.entry.ts')],
    bundle: true,
    format: 'iife',
    outfile: join(sharedDir, 'shared.runtime.bundle.js'),
    banner: { js: `if (!window.__GW || window.__GW.version !== ${JSON.stringify(version)}) {` },
    footer: { js: '}' },
    define: { __GW_VERSION__: JSON.stringify(version) },
    tsconfig: join(root, 'tsconfig.json'),
    minify: true,
    logLevel: 'warning',
  });

  for (const w of widgets) {
    const tn = packageWidgetName(w.name);
    const wDir = join(buildDir, 'ui', tn);
    mkdirSync(wDir, { recursive: true });
    copyFileSync(
      join(sharedDir, 'shared.runtime.bundle.js'),
      join(wDir, 'shared.runtime.bundle.js'),
    );

    console.log(`  Building: ${w.name} (${tn})`);

    const componentDir = join(srcDir, 'components', w.name);

    const runtimeResult = await esbuild.build({
      entryPoints: [join(componentDir, `_${w.name}.runtime.entry.ts`)],
      bundle: true,
      format: 'iife',
      outfile: join(wDir, `${tn}.runtime.bundle.js`),
      plugins: [sharedLitPlugin],
      loader: { '.ts': 'ts', '.json': 'json' },
      tsconfig: join(root, 'tsconfig.json'),
      minify: true,
      metafile: true,
      logLevel: 'warning',
    });
    verifyRuntimeBundle(runtimeResult, w.name);

    await esbuild.build({
      entryPoints: [join(componentDir, `_${w.name}.ide.entry.ts`)],
      bundle: true,
      format: 'iife',
      outfile: join(wDir, `${tn}.ide.bundle.js`),
      loader: { '.ts': 'ts', '.json': 'json' },
      tsconfig: join(root, 'tsconfig.json'),
      minify: true,
      logLevel: 'warning',
    });
  }

  const staleEntitiesDir = join(buildDir, 'Entities');
  if (existsSync(staleEntitiesDir)) {
    rmSync(staleEntitiesDir, { recursive: true, force: true });
  }

  let xml = `<?xml version="1.0" encoding="UTF-8"?>
<Entities majorVersion="9" minorVersion="3" universal="password">
  <ExtensionPackages>
    <ExtensionPackage
      name="${esc(pn)}"
      description="${esc(ext.description)}"
      vendor="${esc(ext.vendor)}"
      packageVersion="${esc(version)}"
      minimumThingWorxVersion="${esc(ext.minimumThingWorxVersion)}"
    />
  </ExtensionPackages>
  <Widgets>\n`;
  for (const w of widgets) {
    const tn = packageWidgetName(w.name);
    xml += `    <Widget name="${esc(tn)}">
      <UIResources>
        <FileResource type="JS" file="shared.runtime.bundle.js" description="GitBackup shared runtime" isDevelopment="false" isRuntime="true"/>
        <FileResource type="JS" file="${tn}.runtime.bundle.js" description="" isDevelopment="false" isRuntime="true"/>
        <FileResource type="JS" file="${tn}.ide.bundle.js" description="" isDevelopment="true" isRuntime="false"/>
      </UIResources>
    </Widget>\n`;
  }
  xml += `  </Widgets>
</Entities>`;
  writeFileSync(join(buildDir, 'metadata.xml'), xml);

  const zipPath = join(outDir, `${pn}-v${version}.zip`);
  try {
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
    console.log(`\nBuild files ready at: ${buildDir}`);
  }
}

build().catch(e => { console.error(e); process.exit(1); });
