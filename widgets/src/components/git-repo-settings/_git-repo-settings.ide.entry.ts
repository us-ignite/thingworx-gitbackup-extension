import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-repo-settings.config.json';
registerIdeWidget(config as any);
