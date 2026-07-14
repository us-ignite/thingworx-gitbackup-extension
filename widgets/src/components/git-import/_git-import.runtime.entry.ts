import './git-import.js';
import { registerRuntimeWidget } from '../../lib/widget-registrator.js';
import config from './git-import.config.json';
const m: Record<string, string> = {};
for (const [k, v] of Object.entries((config as any).properties || {})) { const vv = v as any; m[k] = vv.src || k.charAt(0).toLowerCase() + k.slice(1); }
registerRuntimeWidget(config as any, m);
