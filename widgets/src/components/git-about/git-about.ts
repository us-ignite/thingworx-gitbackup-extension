import { html, css } from 'lit';
import { GitElementBase } from '../git-base.js';
import { themeVars } from '../../lib/git-styles.js';
import { state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { InfotableResponse, ExtensionVersion } from '../../lib/twx-types.js';

export class GitAbout extends GitElementBase {
  static styles = [themeVars, css`
    :host { display: block; padding: 16px; font-family: inherit; }
    .card { border: 1px solid var(--git-color-border, #e0e0e0); border-radius: var(--git-border-radius-lg, 8px); padding: 24px; max-width: 480px; }
    h2 { margin: 0 0 16px 0; color: var(--git-color-accent-light, #1976d2); }
    .row { display: flex; padding: 8px 0; border-bottom: 1px solid var(--git-color-bg-hover, #f5f5f5); }
    .row:last-child { border-bottom: none; }
    .label { flex: 0 0 140px; font-weight: 500; color: var(--git-color-text-secondary, #666); }
    .value { flex: 1; }
    .installed { color: var(--git-color-success, #2e7d32); font-weight: 500; }
    .not-installed { color: var(--git-color-error, #c62828); }
    .loading { text-align: center; padding: 24px; color: var(--git-color-text-muted, #999); }
  `];

  @state() private extensions: ExtensionVersion[] = [];
  @state() private loading = true;

  async connectedCallback() {
    super.connectedCallback();
    await this.load();
  }

  async load() {
    this.loading = true;
    this.clearLoadError();
    try {
      const res = await twx.invokeService<InfotableResponse<ExtensionVersion>>('GIT.Utility.Thing', 'GetGitExtensionVersion', {});
      this.extensions = res.rows || [];
    } catch (error) { this.reportLoadError('Unable to load extension information', error); }
    finally { this.loading = false; }
  }

  render() {
    if (this.loading) return html`<div class="loading">Loading...</div>`;
    return html`
      ${this.renderLoadError()}
      <div class="card">
        <h2>GitBackup Extension</h2>
        ${this.extensions.map(ext => html`
          <div class="row">
            <span class="label">${ext.ExtensionName}</span>
            <span class="value">
              <span class="${ext.IsInstalled ? 'installed' : 'not-installed'}">
                v${ext.ExtensionVersion} ${ext.IsInstalled ? '(installed)' : '(not installed)'}
              </span>
            </span>
          </div>
        `)}
        ${this.extensions.length === 0 ? html`<div class="row"><span class="value">No extension info available</span></div>` : ''}
      </div>
    `;
  }
}

if (!customElements.get('git-about')) { customElements.define('git-about', GitAbout); };
