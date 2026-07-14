import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-merge-rebase.config.json';
registerIdeWidget(config as any);
