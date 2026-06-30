const path = require('node:path');

module.exports = {
  stories: ['../src/**/*.stories.@(ts|tsx|js|jsx)'],
  addons: ['@storybook/addon-essentials'],
  framework: {
    name: '@storybook/web-components-vite',
    options: {},
  },
  async viteFinal(config) {
    return {
      ...config,
      resolve: {
        ...config.resolve,
        alias: [
          ...(config.resolve?.alias ?? []),
          { find: /^(ptcs-.+)$/, replacement: path.resolve(__dirname, '../twx-wc-sdk/$1') },
        ],
      },
      optimizeDeps: {
        ...config.optimizeDeps,
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
      server: {
        ...config.server,
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
    };
  },
};
