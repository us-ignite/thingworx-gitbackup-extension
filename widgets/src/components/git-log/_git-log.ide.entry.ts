import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-log.config.json';
registerIdeWidget(config as any);
