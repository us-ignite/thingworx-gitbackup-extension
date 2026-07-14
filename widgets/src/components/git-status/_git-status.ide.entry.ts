import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-status.config.json';
registerIdeWidget(config as any);
