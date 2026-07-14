import './git-backup-extension-settings.js';
import { registerRuntimeWidget } from '../../lib/widget-registrator.js';
import config from './git-backup-extension-settings.config.json';

registerRuntimeWidget(config as any, {});
