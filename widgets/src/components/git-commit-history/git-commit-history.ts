import { LitElement, html, css } from 'lit';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { InfotableResponse } from '../../lib/twx-types.js';

interface CommitEntry {
  CommitID: string;
  Message: string;
  Author: string;
  CommitTime: string;
  AuthorEmail: string;
}

interface TagEntry {
  TagName: string;
  CommitID: string;
  Message: string;
}

interface CommitInfoEntry {
  CommitId: string;
  Message: string;
  Author: string;
  CommitTime: string;
  Changes: string;
}

export class GitCommitHistory extends LitElement {
  static styles = css`
    :host { display: block; padding: 16px; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .header h3 { margin: 0; }
    .layout { display: flex; gap: 16px; }
    .main { flex: 1; min-width: 0; }
    .sidebar { flex: 0 0 220px; }
    .sidebar h4 { margin: 0 0 8px 0; font-size: 14px; color: #666; }
    .tag { display: inline-block; padding: 3px 10px; margin: 3px; border-radius: 10px; font-size: 12px; background: #e8eaf6; color: #283593; }
    .commit { border: 1px solid #e0e0e0; border-radius: 6px; margin-bottom: 6px; overflow: hidden; }
    .commit-header { display: flex; align-items: center; padding: 10px 14px; cursor: pointer; gap: 12px; }
    .commit-header:hover { background: #f5f5f5; }
    .commit-header.selected { background: #e3f2fd; border-color: #1976d2; }
    .commit-id { font-family: monospace; font-weight: 600; color: #1976d2; font-size: 13px; min-width: 72px; }
    .commit-author { color: #333; font-weight: 500; flex: 0 0 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .commit-msg { flex: 1; color: #555; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .commit-date { color: #888; font-size: 12px; white-space: nowrap; }
    .commit-body { padding: 12px 14px; border-top: 1px solid #e0e0e0; background: #fafafa; }
    .commit-body .row { display: flex; padding: 4px 0; font-size: 13px; }
    .commit-body .label { flex: 0 0 100px; color: #666; }
    .commit-body .value { flex: 1; color: #333; }
    .commit-body .changes { white-space: pre-wrap; font-family: monospace; font-size: 12px; background: #fff; padding: 8px; border-radius: 4px; border: 1px solid #eee; margin-top: 6px; max-height: 300px; overflow: auto; }
    .empty { text-align: center; padding: 32px; color: #999; }
    .error { margin-top: 8px; padding: 10px; background: #ffebee; color: #c62828; border-radius: 4px; font-size: 13px; }
    .loading { opacity: 0.6; pointer-events: none; }
  `;

  @property({ type: String }) gitThing = '';
  @state() private commits: CommitEntry[] = [];
  @state() private tags: TagEntry[] = [];
  @state() private selectedCommitId = '';
  @state() private commitInfo: CommitInfoEntry | null = null;
  @state() private loading = false;
  @state() private error = '';

  async connectedCallback() {
    super.connectedCallback();
    if (this.gitThing) this.load();
  }

  async load() {
    if (!this.gitThing) return;
    this.loading = true;
    this.error = '';
    try {
      const [commitsRes, tagsRes] = await Promise.all([
        twx.invokeService<InfotableResponse<CommitEntry>>(this.gitThing, 'GetCommitList', {}),
        twx.invokeService<InfotableResponse<TagEntry>>(this.gitThing, 'GetTagList', {}),
      ]);
      this.commits = commitsRes.rows || [];
      this.tags = tagsRes.rows || [];
    } catch (e: any) {
      this.error = e.message || 'Failed to load commit history';
    } finally {
      this.loading = false;
    }
  }

  async selectCommit(commitId: string) {
    if (this.selectedCommitId === commitId) {
      this.selectedCommitId = '';
      this.commitInfo = null;
      return;
    }
    this.selectedCommitId = commitId;
    this.commitInfo = null;
    try {
      const res = await twx.invokeService<InfotableResponse<CommitInfoEntry>>(this.gitThing, 'GetCommitInfo', { CommitID: commitId });
      this.commitInfo = res.rows?.[0] || null;
    } catch (e: any) {
      this.error = e.message || 'Failed to load commit info';
    }
  }

  shortId(id: string) {
    return id ? id.substring(0, 7) : '';
  }

  formatDate(d: string) {
    if (!d) return '';
    try { return new Date(d).toLocaleString(); } catch { return d; }
  }

  render() {
    return html`
      <div class=${this.loading ? 'loading' : ''}>
        <div class="header">
          <h3>Commit History</h3>
          <ptcs-button label=${this.loading ? 'Loading...' : 'Refresh'} @click=${this.load} ?disabled=${this.loading}></ptcs-button>
        </div>

        ${this.error ? html`<div class="error">${this.error}</div>` : ''}

        <div class="layout">
          <div class="main">
            ${this.commits.map(c => html`
              <div class="commit">
                <div class="commit-header ${this.selectedCommitId === c.CommitID ? 'selected' : ''}" @click=${() => this.selectCommit(c.CommitID)}>
                  <span class="commit-id">${this.shortId(c.CommitID)}</span>
                  <span class="commit-author">${c.Author}</span>
                  <span class="commit-msg">${c.Message}</span>
                  <span class="commit-date">${this.formatDate(c.CommitTime)}</span>
                </div>
                ${this.selectedCommitId === c.CommitID && this.commitInfo ? html`
                  <div class="commit-body">
                    <div class="row"><span class="label">Commit ID</span><span class="value">${this.commitInfo.CommitId}</span></div>
                    <div class="row"><span class="label">Author</span><span class="value">${this.commitInfo.Author}</span></div>
                    <div class="row"><span class="label">Date</span><span class="value">${this.formatDate(this.commitInfo.CommitTime)}</span></div>
                    <div class="row"><span class="label">Message</span><span class="value">${this.commitInfo.Message}</span></div>
                    ${this.commitInfo.Changes ? html`<div class="row"><span class="label">Changes</span><div class="value changes">${this.commitInfo.Changes}</div></div>` : ''}
                  </div>
                ` : ''}
                ${this.selectedCommitId === c.CommitID && !this.commitInfo ? html`
                  <div class="commit-body" style="text-align:center;color:#999">Loading commit info...</div>
                ` : ''}
              </div>
            `)}
            ${this.commits.length === 0 && !this.loading ? html`<div class="empty">No commits found</div>` : ''}
          </div>

          ${this.tags.length > 0 ? html`
            <div class="sidebar">
              <h4>Tags</h4>
              ${this.tags.map(t => html`<span class="tag">${t.TagName}</span>`)}
            </div>
          ` : ''}
        </div>
      </div>
    `;
  }
}

customElements.define('git-commit-history', GitCommitHistory);
