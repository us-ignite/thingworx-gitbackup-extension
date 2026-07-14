import { html, css } from 'lit';
import { GitElementBase } from '../git-base.js';
import { themeVars } from '../../lib/git-styles.js';
import { state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';

export class GitVersion extends GitElementBase {
  static styles = [themeVars, css`
    :host { display: block; padding: 16px; }
    .card { border: 1px solid var(--git-color-border, #e0e0e0); border-radius: var(--git-border-radius-lg, 8px); padding: 16px; text-align: center; }
    .version { font-size: 24px; font-weight: 300; color: var(--git-color-accent-light, #1976d2); margin: 8px 0; }
    .label { font-size: 12px; color: var(--git-color-text-muted, #999); text-transform: uppercase; letter-spacing: 1px; }
    .error { color: var(--git-color-error, #c62828); font-size: 14px; margin-top: 8px; }
  `];

  @state() private version = '';
  @state() private error = '';

  async connectedCallback() {
    super.connectedCallback();
    try {
      const res = await twx.invokeService<any>('GIT.Utility.Thing', 'GetGitExtensionVersion', {});
      this.version = res?.version || res?.result || 'Unknown';
    } catch {
      this.error = 'Could not retrieve version';
    }
  }

  render() {
    return html`
      <div class="card">
        <div class="label">Extension Version</div>
        <div class="version">${this.version || '...'}</div>
        ${this.error ? html`<div class="error">${this.error}</div>` : ''}
      </div>
    `;
  }
}

if (!customElements.get('git-version')) { customElements.define('git-version', GitVersion); };
