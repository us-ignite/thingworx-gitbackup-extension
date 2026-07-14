import './git-main.js';
import { registerRuntimeWidget } from '../../lib/widget-registrator.js';
import config from './git-main.config.json';
registerRuntimeWidget(config as any, {});
