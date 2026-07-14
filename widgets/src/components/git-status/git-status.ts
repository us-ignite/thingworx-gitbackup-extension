import { html, css } from 'lit';
import { GitElementBase } from '../git-base.js';
import { themeVars } from '../../lib/git-styles.js';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { GitCurrentBranch, InfotableResponse } from '../../lib/twx-types.js';

interface StatusRow {
  File: string;
  Status: string;
  ChangesInWorkingDir?: string;
}

export class GitStatus extends GitElementBase {
  static styles = [themeVars, css`
    :host { display: block; padding: 16px; font-family: sans-serif; }
    .header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
    .branch-name { font-size: 22px; font-weight: 700; color: var(--git-color-accent, #1565c0); }
    .file-list { border: 1px solid var(--git-color-border, #e0e0e0); border-radius: var(--git-border-radius-sm, 4px); overflow: hidden; }
    .file-row { display: flex; padding: 8px 12px; border-bottom: 1px solid var(--git-color-border-light, #f0f0f0); cursor: pointer; }
    .file-row:hover { background: var(--git-color-bg-hover, #f5f5f5); }
    .file-row.selected { background: var(--git-color-bg-selected, #e3f2fd); }
    .file-name { flex: 1; }
    .file-status { width: 40px; text-align: center; font-weight: 700; font-size: 14px; }
    .status-A { color: var(--git-color-success, #2e7d32); }
    .status-M { color: var(--git-color-warning, #e65100); }
    .status-D { color: var(--git-color-error, #c62828); }
    .status-R { color: var(--git-color-accent, #1565c0); }
    .diff-panel { margin-top: 12px; border: 1px solid var(--git-color-border, #e0e0e0); border-radius: var(--git-border-radius-sm, 4px); overflow: hidden; }
    .diff-header { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background: var(--git-color-bg-stripe, #fafafa); border-bottom: 1px solid var(--git-color-border, #e0e0e0); font-weight: 600; }
    .diff-body { padding: 12px; background: var(--git-color-bg-hover, #f5f5f5); overflow-x: auto; white-space: pre-wrap; font-family: 'Courier New', monospace; font-size: 13px; line-height: 1.5; max-height: 400px; overflow-y: auto; }
    .empty-state { text-align: center; padding: 32px; color: var(--git-color-text-muted, #999); }
  `];

  @property({ type: String }) gitThing = '';
  @state() private currentBranch = '';
  @state() private files: StatusRow[] = [];
  @state() private selectedFile = '';
  @state() private diffText = '';
  @state() private loading = false;
  @state() private diffLoading = false;

  async connectedCallback() {
    super.connectedCallback();
    if (this.gitThing) this.loadData();
  }

  async loadData() {
    if (!this.gitThing) return;
    this.loading = true;
    this.clearLoadError();
    try {
      const [statusRes, branchRes] = await Promise.all([
        twx.invokeService<InfotableResponse<StatusRow>>(this.gitThing, 'Status', {}),
        twx.invokeService<InfotableResponse<GitCurrentBranch>>(this.gitThing, 'GetCurrentBranch', {}),
      ]);
      this.files = statusRes.rows || [];
      this.currentBranch = branchRes.rows?.[0]?.BranchName || '';
    } catch (error) { this.reportLoadError('Unable to load repository status', error); }
    finally { this.loading = false; }
  }

  async selectFile(file: string) {
    if (this.selectedFile === file) {
      this.selectedFile = '';
      this.diffText = '';
      return;
    }
    this.selectedFile = file;
    this.diffLoading = true;
    this.diffText = '';
    try {
      const text = await twx.invokeService<string>(this.gitThing, 'GetDiffPerFile', { File: file });
      this.diffText = text || '(empty diff)';
    } catch {
      this.diffText = '(error loading diff)';
    } finally {
      this.diffLoading = false;
    }
  }

  DoRefresh() {
    this.loadData();
  }

  ShowDiff(file: string) {
    this.selectFile(file);
  }

  statusClass(status: string): string {
    const s = status.trim();
    if (s === 'A' || s === 'Added') return 'status-A';
    if (s === 'M' || s === 'Modified') return 'status-M';
    if (s === 'D' || s === 'Deleted') return 'status-D';
    if (s === 'R' || s === 'Renamed') return 'status-R';
    if (s === 'C' || s === 'Copied') return 'status-C';
    if (s === 'U' || s === 'Updated') return 'status-U';
    return 'status-?';
  }

  render() {
    return html`
      <div class=${this.loading ? 'loading' : ''}>
        ${this.renderLoadError()}
        <div class="header">
          <span class="branch-name">${this.currentBranch}</span>
          <ptcs-button label="Refresh" @click=${this.loadData} ?disabled=${this.loading}></ptcs-button>
        </div>

        <div class="file-list">
          ${this.files.map(f => html`
            <div class="file-row ${f.File === this.selectedFile ? 'selected' : ''}" @click=${() => this.selectFile(f.File)}>
              <span class="file-name">${f.File}</span>
              <span class="file-status ${this.statusClass(f.Status)}">${f.Status}</span>
            </div>
          `)}
          ${this.files.length === 0 ? html`<div class="empty-state">Working directory clean</div>` : ''}
        </div>

        ${this.selectedFile ? html`
          <div class="diff-panel">
            <div class="diff-header">
              <span>Diff: ${this.selectedFile}</span>
              <ptcs-button label="Close" @click=${() => { this.selectedFile = ''; this.diffText = ''; }}></ptcs-button>
            </div>
            <div class="diff-body">${this.diffLoading ? 'Loading diff...' : this.diffText}</div>
          </div>
        ` : ''}
      </div>
    `;
  }
}

if (!customElements.get('git-status')) { customElements.define('git-status', GitStatus); };
