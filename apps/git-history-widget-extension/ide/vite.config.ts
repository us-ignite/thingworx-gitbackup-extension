import { transformSync } from '@babel/core';
import { defineConfig } from 'vite';

export default defineConfig({
  esbuild: { keepNames: true },
  plugins: [{
    name: 'ide-es5-output',
    generateBundle(_options, bundle) {
      for (const output of Object.values(bundle)) {
        if (output.type !== 'chunk') continue;
        const result = transformSync(output.code, {
          babelrc: false,
          configFile: false,
          comments: false,
          compact: true,
          minified: true,
          presets: [['@babel/preset-env', {
            targets: { ie: '11' },
            modules: false,
            useBuiltIns: false,
          }]],
        });
        if (!result?.code) throw new Error(`Could not transpile ${output.fileName} to ES5`);
        output.code = result.code;
      }
    },
  }],
  build: {
    // Vite stops at ES2015; the plugin above converts the final IIFE to ES5.
    target: 'es2015',
    outDir: '../build/vite/ide',
    emptyOutDir: true,
    minify: 'esbuild',
    lib: {
      entry: 'src/main.ts',
      name: 'GitHistoryWidgetIde',
      formats: ['iife'],
      fileName: () => 'GitHistoryWidget.ide.bundle.js',
    },
  },
});
