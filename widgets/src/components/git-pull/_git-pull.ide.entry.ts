import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-pull.config.json';
registerIdeWidget(config as any);
