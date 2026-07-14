import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-version.config.json';
registerIdeWidget(config as any);
