import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-about.config.json';

registerIdeWidget(config as any);
