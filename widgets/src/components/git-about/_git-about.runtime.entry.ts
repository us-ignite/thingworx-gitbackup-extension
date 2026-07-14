import './git-about.js';
import { registerRuntimeWidget } from '../../lib/widget-registrator.js';
import config from './git-about.config.json';

registerRuntimeWidget(config as any, {});
