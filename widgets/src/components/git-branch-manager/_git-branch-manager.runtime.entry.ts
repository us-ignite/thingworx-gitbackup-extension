import './git-branch-manager.js';
import { registerRuntimeWidget } from '../../lib/widget-registrator.js';
import config from './git-branch-manager.config.json';

const propMap: Record<string, string> = {};
for (const [k, v] of Object.entries((config as any).properties || {})) {
  const vv = v as any;
  propMap[k] = vv.src || k.charAt(0).toLowerCase() + k.slice(1);
}
registerRuntimeWidget(config as any, propMap);
