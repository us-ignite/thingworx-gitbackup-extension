import { defineConfig } from 'vite';
import path from 'path';
import { fileURLToPath } from 'url';
import { twxPackager } from './vite-plugin/twx-packager.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const SDK = path.resolve(__dirname, 'twx-wc-sdk');

export default defineConfig({
  plugins: [twxPackager()],
  server: {
    port: 8081,
    open: true,
    proxy: {
      '/Thingworx': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('X-Requested-By', 'ThingWorx');
            proxyReq.setHeader('Authorization', 'Basic QWRtaW5pc3RyYXRvcjpUd3hBZG0xblBAc3N3MHJkIQ==');
          });
        },
      },
    },
  },
  optimizeDeps: {
    include: [
      'lit',
      'lit/decorators.js',
      'lit/directives/class-map.js',
      'lit/directives/style-map.js',
      'lit/directives/if-defined.js',
      '@polymer/polymer',
      '@polymer/iron-icons',
    ],
  },
  build: {
    target: 'es2021',
    rollupOptions: {
      input: {
        main: 'index.html',
      },
      preserveEntrySignatures: 'strict',
    },
  },
  resolve: {
    alias: [
      { find: /^(ptcs-.+)$/, replacement: `${SDK}/$1` },
    ],
    conditions: ['development', 'browser'],
  },
});
