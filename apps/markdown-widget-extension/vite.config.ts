import { defineConfig } from 'vite';

export default defineConfig({
  root: 'runtime',
  esbuild: { keepNames: true },
  build: {
    // ThingWorx loads this as a classic script in modern browsers.
    target: 'es2020',
    outDir: '../build/vite/runtime',
    emptyOutDir: true,
    minify: 'esbuild',
    lib: {
      entry: 'src/main.ts',
      name: 'GitMarkdownWidgetRuntime',
      formats: ['iife'],
      fileName: () => 'GitMarkdownWidget.runtime.bundle.js',
    },
  },
});
