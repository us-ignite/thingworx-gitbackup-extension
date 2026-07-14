import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-export.config.json';
registerIdeWidget(config as any);
