import { LitElement, html, css } from 'lit';
import { state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { InfotableResponse, ExtensionVersion } from '../../lib/twx-types.js';

export class GitAbout extends LitElement {
  static styles = css`
    :host { display: block; padding: 16px; font-family: inherit; }
    .card { border: 1px solid #e0e0e0; border-radius: 8px; padding: 24px; max-width: 480px; }
    h2 { margin: 0 0 16px 0; color: #1976d2; }
    .row { display: flex; padding: 8px 0; border-bottom: 1px solid #f5f5f5; }
    .row:last-child { border-bottom: none; }
    .label { flex: 0 0 140px; font-weight: 500; color: #666; }
    .value { flex: 1; }
    .installed { color: #2e7d32; font-weight: 500; }
    .not-installed { color: #c62828; }
    .loading { text-align: center; padding: 24px; color: #999; }
  `;

  @state() private extensions: ExtensionVersion[] = [];
  @state() private loading = true;

  async connectedCallback() {
    super.connectedCallback();
    await this.load();
  }

  async load() {
    this.loading = true;
    try {
      const res = await twx.invokeService<InfotableResponse<ExtensionVersion>>('GIT.Utility.Thing', 'GetExtensionVersions', {});
      this.extensions = res.rows || [];
    } catch { }
    finally { this.loading = false; }
  }

  render() {
    if (this.loading) return html`<div class="loading">Loading...</div>`;
    return html`
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

customElements.define('git-about', GitAbout);
