import { html, css } from 'lit';
import { GitElementBase } from '../git-base.js';
import { themeVars, statusStyles } from '../../lib/git-styles.js';
import { state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { InfotableResponse } from '../../lib/twx-types.js';

interface ExtensionInfo {
  ExtensionName: string;
  ExtensionVersion: string;
  IsInstalled: boolean;
}

interface GitThingTab {
  GitThingName: string;
  MashupName: string;
}

export class GitMain extends GitElementBase {
  static styles = [themeVars, statusStyles, css`
    :host { display: block; padding: 16px; font-family: sans-serif; }
    .welcome { font-size: 24px; font-weight: 700; color: var(--git-color-accent, #1565c0); margin-bottom: 4px; }
    .subtitle { color: var(--git-color-text-secondary, #666); margin-bottom: 20px; font-size: 14px; }
    .card { border: 1px solid var(--git-color-border, #e0e0e0); border-radius: var(--git-border-radius-lg, 8px); padding: 16px; margin-bottom: 16px; }
    .card-title { font-size: 16px; font-weight: 600; margin-bottom: 12px; color: var(--git-color-text, #333); }
    .ext-grid { display: grid; grid-template-columns: 1fr auto auto; gap: 8px; align-items: center; }
    .ext-grid .header { font-weight: 600; color: var(--git-color-text-secondary, #666); font-size: 12px; text-transform: uppercase; }
    .ext-row { padding: 6px 0; border-bottom: 1px solid var(--git-color-border-light, #f0f0f0); font-size: 14px; }
    .ext-row:last-child { border-bottom: none; }
    .badge { padding: 2px 10px; border-radius: 10px; font-size: 12px; font-weight: 600; }
    .badge.installed { background: var(--git-color-bg-success, #e8f5e9); color: var(--git-color-success, #2e7d32); }
    .badge.missing { background: var(--git-color-bg-error, #ffebee); color: var(--git-color-error, #c62828); }
    .repo-table { width: 100%; border-collapse: collapse; }
    .repo-table th { text-align: left; padding: 8px 12px; background: var(--git-color-bg-stripe, #fafafa); border-bottom: 2px solid var(--git-color-border, #e0e0e0); font-size: 12px; color: var(--git-color-text-secondary, #666); text-transform: uppercase; }
    .repo-table td { padding: 8px 12px; border-bottom: 1px solid var(--git-color-border-light, #f0f0f0); font-size: 14px; }
    .repo-table tr:hover td { background: var(--git-color-bg-hover, #f5f5f5); }
    .empty-state { text-align: center; padding: 32px; color: var(--git-color-text-muted, #999); }
  `];

  @state() private extensions: ExtensionInfo[] = [];
  @state() private gitThings: GitThingTab[] = [];
  @state() private loading = false;
  @state() private error = '';

  async connectedCallback() {
    super.connectedCallback();
    this.load();
  }

  async load() {
    this.loading = true;
    this.error = '';
    try {
      const [extRes, tabsRes] = await Promise.all([
        twx.invokeService<InfotableResponse<ExtensionInfo>>('GIT.Utility.Thing', 'GetGitExtensionVersion', {}),
        twx.invokeService<InfotableResponse<GitThingTab>>('GIT.Utility.Thing', 'GetGitHeaderTabs', {}),
      ]);
      this.extensions = extRes.rows || [];
      this.gitThings = tabsRes.rows || [];
    } catch (e: any) {
      this.error = e.message || 'Failed to load dashboard';
    } finally {
      this.loading = false;
    }
  }

  handleSelectThing(name: string) {
    this.dispatchEvent(new CustomEvent('thing-selected', { detail: { thingName: name }, bubbles: true, composed: true }));
  }

  render() {
    const installed = this.extensions.filter(e => e.IsInstalled);
    const missing = this.extensions.filter(e => !e.IsInstalled);
    return html`
      <div class=${this.loading ? 'loading' : ''}>
        <div class="welcome">GitBackup Extension</div>
        <div class="subtitle">ThingWorx Git Integration Dashboard</div>
        ${this.error ? html`<div class="error">${this.error}</div>` : ''}

        <div class="card">
          <div class="card-title">Extensions</div>
          <div class="ext-grid">
            <span class="header">Extension</span>
            <span class="header">Version</span>
            <span class="header">Status</span>
            ${this.extensions.map(e => html`
              <span class="ext-row">${e.ExtensionName}</span>
              <span class="ext-row">${e.ExtensionVersion || '—'}</span>
              <span class="ext-row">
                <span class="badge ${e.IsInstalled ? 'installed' : 'missing'}">${e.IsInstalled ? 'Installed' : 'Missing'}</span>
              </span>
            `)}
            ${this.extensions.length === 0 ? html`<span class="empty-state" style="grid-column:1/-1">No extension data</span>` : ''}
          </div>
        </div>

        <div class="card">
          <div class="card-title">Git Repositories</div>
          ${this.gitThings.length > 0 ? html`
            <table class="repo-table">
              <thead><tr>
                <th>Thing Name</th>
                <th>Mashup</th>
                <th></th>
              </tr></thead>
              <tbody>
                ${this.gitThings.map(t => html`
                  <tr>
                    <td><strong>${t.GitThingName}</strong></td>
                    <td>${t.MashupName || '—'}</td>
                    <td><ptcs-button label="Select" @click=${() => this.handleSelectThing(t.GitThingName)}></ptcs-button></td>
                  </tr>
                `)}
              </tbody>
            </table>
          ` : html`<div class="empty-state">No Git repositories configured yet. Create one in the "New Repo" tab.</div>`}
        </div>

        <div style="display:flex;gap:8px">
          <ptcs-button label="Refresh" @click=${this.load} ?disabled=${this.loading}></ptcs-button>
        </div>
      </div>
    `;
  }
}

if (!customElements.get('git-main')) { customElements.define('git-main', GitMain); };
