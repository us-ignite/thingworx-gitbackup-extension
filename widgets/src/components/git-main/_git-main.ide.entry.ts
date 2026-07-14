import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-main.config.json';
registerIdeWidget(config as any);
