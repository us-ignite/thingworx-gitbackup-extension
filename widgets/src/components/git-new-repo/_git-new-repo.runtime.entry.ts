import './git-new-repo.js';
import { registerRuntimeWidget } from '../../lib/widget-registrator.js';
import config from './git-new-repo.config.json';
registerRuntimeWidget(config as any, {});
