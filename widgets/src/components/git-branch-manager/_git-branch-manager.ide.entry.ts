import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-branch-manager.config.json';
registerIdeWidget(config as any);
