import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-new-repo.config.json';
registerIdeWidget(config as any);
