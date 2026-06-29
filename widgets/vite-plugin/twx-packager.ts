import type { Plugin } from 'vite';

export function twxPackager(): Plugin {
  return {
    name: 'twx-packager',
    closeBundle() {
      // Production packaging handled by scripts/build-widgets.mjs
    },
  };
}
