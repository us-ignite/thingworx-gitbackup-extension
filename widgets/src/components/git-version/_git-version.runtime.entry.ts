import './git-version.js';
import { registerRuntimeWidget } from '../../lib/widget-registrator.js';
import config from './git-version.config.json';
registerRuntimeWidget(config as any, {});
