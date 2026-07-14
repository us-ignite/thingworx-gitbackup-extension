import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-loading.config.json';
registerIdeWidget(config as any);
