import { LitElement, html, css } from 'lit';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { InfotableResponse, GitStatus } from '../../lib/twx-types.js';

export class GitPush extends LitElement {
  static styles = css`
    :host { display: block; padding: 16px; }
    .files { margin: 12px 0; border: 1px solid #e0e0e0; border-radius: 4px; }
    .file-row { display: flex; align-items: center; padding: 6px 12px; border-bottom: 1px solid #f0f0f0; }
    .file-row:last-child { border-bottom: none; }
    .file-row input { margin-right: 8px; }
    .file-name { flex: 1; }
    .status { padding: 2px 8px; border-radius: 3px; font-size: 12px; }
    .status.M { background: #fff3e0; }
    .status.A { background: #e8f5e9; }
    .status.D { background: #ffebee; }
    .status.? { background: #f3e5f5; }
    .actions { margin-top: 12px; }
    .result { margin-top: 12px; padding: 12px; background: #f5f5f5; border-radius: 4px; }
    .error { color: #c62828; }
    .loading { opacity: 0.6; pointer-events: none; }
  `;

  @property({ type: String }) gitThing = '';
  @state() private files: GitStatus[] = [];
  @state() private selectedFiles: Set<string> = new Set();
  @state() private loading = false;
  @state() private pushing = false;
  @state() private result = '';
  @state() private error = '';

  async connectedCallback() {
    super.connectedCallback();
    if (this.gitThing) this.loadStatus();
  }

  async loadStatus(): Promise<void> {
    if (!this.gitThing) return;
    this.loading = true;
    try {
      const res = await twx.invokeService<InfotableResponse<GitStatus>>(this.gitThing, 'QueryStatus', {});
      this.files = res.rows || [];
      this.selectedFiles = new Set(this.files.map(f => f.File));
    } catch { }
    finally { this.loading = false; }
  }

  toggleFile(file: string) {
    if (this.selectedFiles.has(file)) this.selectedFiles.delete(file);
    else this.selectedFiles.add(file);
  }

  selectAll() { this.selectedFiles = new Set(this.files.map(f => f.File)); }

  async doPush(): Promise<void> {
    if (!this.gitThing || this.pushing) return;
    this.pushing = true;
    this.result = '';
    this.error = '';
    try {
      const res = await twx.invokeService(this.gitThing, 'Push', {}) as any;
      this.result = res?.result || 'Push completed';
    } catch (e: any) {
      this.error = e.message || 'Push failed';
    } finally {
      this.pushing = false;
    }
  }

  render() {
    const statusColor = (s: string) => `status ${s}`;
    return html`
      <div class=${this.loading || this.pushing ? 'loading' : ''}>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <h3 style="margin:0">Push to Remote</h3>
          <ptcs-button label="Refresh" @click=${this.loadStatus} ?disabled=${this.loading}></ptcs-button>
        </div>

        <div class="files">
          <div class="file-row" style="background:#fafafa;font-weight:bold">
            <input type="checkbox" @change=${this.selectAll} .checked=${this.selectedFiles.size === this.files.length && this.files.length > 0}>
            <span class="file-name">File</span>
            <span>Status</span>
          </div>
          ${this.files.map(f => html`
            <div class="file-row">
              <input type="checkbox" .checked=${this.selectedFiles.has(f.File)} @change=${() => this.toggleFile(f.File)}>
              <span class="file-name">${f.File}</span>
              <span class="${statusColor(f.Status)}">${f.Status}</span>
            </div>
          `)}
          ${this.files.length === 0 ? html`<div class="file-row" style="justify-content:center;color:#999">No changes to push</div>` : ''}
        </div>

        <div class="actions">
          <ptcs-button label="Push" @click=${this.doPush} ?disabled=${this.pushing || this.files.length === 0}></ptcs-button>
          ${this.pushing ? html`<span style="margin-left:8px">Pushing...</span>` : ''}
        </div>

        ${this.result ? html`<div class="result">${this.result}</div>` : ''}
        ${this.error ? html`<div class="result error">${this.error}</div>` : ''}
      </div>
    `;
  }
}

customElements.define('git-push', GitPush);
