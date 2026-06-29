import { LitElement, html, css } from 'lit';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { GitCurrentBranch, InfotableResponse } from '../../lib/twx-types.js';

interface StatusRow {
  File: string;
  Status: string;
  ChangesInWorkingDir?: string;
}

export class GitStatus extends LitElement {
  static styles = css`
    :host { display: block; padding: 16px; font-family: sans-serif; }
    .header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
    .branch-name { font-size: 22px; font-weight: 700; color: #1565c0; }
    .file-list { border: 1px solid #e0e0e0; border-radius: 4px; overflow: hidden; }
    .file-row { display: flex; padding: 8px 12px; border-bottom: 1px solid #f0f0f0; cursor: pointer; }
    .file-row:hover { background: #f5f5f5; }
    .file-row.selected { background: #e3f2fd; }
    .file-name { flex: 1; }
    .file-status { width: 40px; text-align: center; font-weight: 700; font-size: 14px; }
    .status-A { color: #2e7d32; }
    .status-M { color: #e65100; }
    .status-D { color: #c62828; }
    .status-R { color: #1565c0; }
    .status-C { color: #6a1b9a; }
    .status-U { color: #4e342e; }
    .status-? { color: #9e9e9e; }
    .diff-panel { margin-top: 12px; border: 1px solid #e0e0e0; border-radius: 4px; overflow: hidden; }
    .diff-header { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background: #fafafa; border-bottom: 1px solid #e0e0e0; font-weight: 600; }
    .diff-body { padding: 12px; background: #f5f5f5; overflow-x: auto; white-space: pre-wrap; font-family: 'Courier New', monospace; font-size: 13px; line-height: 1.5; max-height: 400px; overflow-y: auto; }
    .loading { opacity: 0.6; pointer-events: none; }
    .empty-state { text-align: center; padding: 32px; color: #999; }
  `;

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
    try {
      const [statusRes, branchRes] = await Promise.all([
        twx.invokeService<InfotableResponse<StatusRow>>(this.gitThing, 'Status', {}),
        twx.invokeService<InfotableResponse<GitCurrentBranch>>(this.gitThing, 'GetCurrentBranch', {}),
      ]);
      this.files = statusRes.rows || [];
      this.currentBranch = branchRes.rows?.[0]?.BranchName || '';
    } catch { }
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

customElements.define('git-status', GitStatus);
