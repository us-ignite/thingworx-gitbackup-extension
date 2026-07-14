import { registerIdeWidget } from '../../lib/widget-registrator.js';
import config from './git-backup-extension-settings.config.json';

registerIdeWidget(config as any);
