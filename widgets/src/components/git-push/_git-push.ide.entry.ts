import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-push.config.json';
registerIdeWidget(config as any);
