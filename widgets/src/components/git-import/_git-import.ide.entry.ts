import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-import.config.json';
registerIdeWidget(config as any);
