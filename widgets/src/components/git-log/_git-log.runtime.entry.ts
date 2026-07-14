import './git-log.js';
import { registerRuntimeWidget } from '../../lib/widget-registrator.js';
import config from './git-log.config.json';
registerRuntimeWidget(config as any, {});
