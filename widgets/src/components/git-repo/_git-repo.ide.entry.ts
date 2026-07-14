import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-repo.config.json';
registerIdeWidget(config as any);
