import { html, css } from 'lit';
import { GitElementBase } from '../git-base.js';
import { themeVars, statusStyles } from '../../lib/git-styles.js';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import {readServiceResult, type InfotableResponse, type GitStatus, type ServiceResultResponse} from '../../lib/twx-types.js';

export class GitPush extends GitElementBase {
  static styles = [themeVars, statusStyles, css`
    :host { display: block; padding: 16px; }
    .files { margin: 12px 0; border: 1px solid var(--git-color-border, #e0e0e0); border-radius: var(--git-border-radius-sm, 4px); }
    .file-row { display: flex; align-items: center; padding: 6px 12px; border-bottom: 1px solid var(--git-color-border-light, #f0f0f0); }
    .file-row:last-child { border-bottom: none; }
    .file-row input { margin-right: 8px; }
    .file-name { flex: 1; }
    .status { padding: 2px 8px; border-radius: 3px; font-size: 12px; }
    .status.M { background: var(--git-color-bg-warning, #fff3e0); }
    .status.A { background: var(--git-color-bg-success, #e8f5e9); }
    .status.D { background: var(--git-color-bg-error, #ffebee); }
    .result { margin-top: 12px; padding: 12px; background: var(--git-color-bg-hover, #f5f5f5); border-radius: var(--git-border-radius-sm, 4px); }
  `];

  @property({ type: String }) gitThing = '';
  @state() private files: GitStatus[] = [];
  @state() private selectedFiles: Set<string> = new Set();
  @state() private loading = false;
  @state() private pushing = false;
  @state() private commitMessage = '';
  @state() private result = '';
  @state() private error = '';

  async connectedCallback() {
    super.connectedCallback();
    if (this.gitThing) this.loadStatus();
  }

  async loadStatus(): Promise<void> {
    if (!this.gitThing) return;
    this.loading = true;
    this.clearLoadError();
    try {
      const res = await twx.invokeService<InfotableResponse<GitStatus>>(this.gitThing, 'Status', {});
      this.files = res.rows || [];
      this.selectedFiles = new Set(this.files.map(f => f.File));
    } catch (error) { this.reportLoadError('Unable to load changed files', error); }
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
      const res = await twx.invokeService<ServiceResultResponse>(
        this.gitThing,
        'Push',
        {Message: this.commitMessage},
      );
      const message = readServiceResult(res, 'Push completed');
      if (/^Push Error:/i.test(message.trim())) {
        throw new Error(message);
      }
      this.result = message;
      this.commitMessage = '';
      this.selectedFiles = new Set();
      await this.loadStatus();
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
        ${this.renderLoadError()}
        <form @submit=${(e: SubmitEvent) => { e.preventDefault(); this.doPush(); }}>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <h3 style="margin:0">Commit & Push</h3>
          <ptcs-button label="Refresh" @click=${this.loadStatus} ?disabled=${this.loading}></ptcs-button>
        </div>

        <div style="margin:12px 0">
          <label style="display:block;font-size:13px;font-weight:600;color:#333;margin-bottom:4px">Commit Message</label>
          <textarea .value=${this.commitMessage} @input=${(e: InputEvent) => this.commitMessage = (e.target as HTMLTextAreaElement).value}
            style="width:100%;padding:8px;border:1px solid #ccc;border-radius:4px;font-size:13px;font-family:inherit;box-sizing:border-box;resize:vertical"
            rows="3" placeholder="Describe your changes..."></textarea>
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
          <ptcs-button label="Commit & Push" @click=${this.doPush} ?disabled=${this.pushing || this.files.length === 0}></ptcs-button>
          ${this.pushing ? html`<span style="margin-left:8px">Pushing...</span>` : ''}
        </div>

        ${this.result ? html`<div class="result">${this.result}</div>` : ''}
        ${this.error ? html`<div class="result error">${this.error}</div>` : ''}
        </form>
      </div>
    `;
  }
}

if (!customElements.get('git-push')) { customElements.define('git-push', GitPush); };
