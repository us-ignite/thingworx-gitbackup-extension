import { defineConfig } from 'vite';

export default defineConfig({
  esbuild: { keepNames: true },
  build: {
    // ThingWorx loads this as a classic script in modern browsers.
    target: 'es2020',
    outDir: '../build/vite/runtime',
    emptyOutDir: true,
    minify: 'esbuild',
    lib: {
      entry: 'src/main.ts',
      name: 'AgGridWidget2Runtime',
      formats: ['iife'],
      fileName: () => 'agGridWidget2.runtime.bundle.js',
    },
  },
});
