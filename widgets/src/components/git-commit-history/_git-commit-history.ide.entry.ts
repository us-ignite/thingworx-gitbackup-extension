import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-commit-history.config.json';
registerIdeWidget(config as any);
