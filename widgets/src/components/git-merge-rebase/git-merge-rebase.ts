import { LitElement, html, css } from 'lit';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { InfotableResponse } from '../../lib/twx-types.js';

interface BranchEntry {
  BranchName: string;
  IsRemote: boolean;
  IsCurrent: boolean;
}

export class GitMergeRebase extends LitElement {
  static styles = css`
    :host { display: block; padding: 16px; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .header h3 { margin: 0; }
    .current-branch { padding: 10px 14px; background: #e8f5e9; border-radius: 6px; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
    .current-branch .label { color: #2e7d32; font-weight: 500; }
    .current-branch .name { font-family: monospace; font-weight: 600; color: #1b5e20; }
    .form { display: flex; flex-direction: column; gap: 12px; }
    .form label { font-size: 14px; color: #555; font-weight: 500; }
    .form select { padding: 8px 12px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; background: #fff; width: 100%; box-sizing: border-box; }
    .form select:focus { outline: none; border-color: #1976d2; }
    .actions { display: flex; gap: 8px; margin-top: 4px; }
    .result { margin-top: 12px; padding: 12px; border-radius: 4px; white-space: pre-wrap; }
    .result.success { background: #e8f5e9; color: #2e7d32; }
    .result.error { background: #ffebee; color: #c62828; }
    .empty { text-align: center; padding: 24px; color: #999; }
    .loading { opacity: 0.6; pointer-events: none; }
  `;

  @property({ type: String }) gitThing = '';
  @state() private branches: BranchEntry[] = [];
  @state() private currentBranch = '';
  @state() private selectedBranch = '';
  @state() private loading = false;
  @state() private operating = false;
  @state() private result = '';
  @state() private isError = false;

  async connectedCallback() {
    super.connectedCallback();
    if (this.gitThing) this.load();
  }

  async load() {
    if (!this.gitThing) return;
    this.loading = true;
    this.result = '';
    try {
      const res = await twx.invokeService<InfotableResponse<BranchEntry>>(this.gitThing, 'GetBranchList', {});
      this.branches = res.rows || [];
      const current = this.branches.find(b => b.IsCurrent);
      this.currentBranch = current?.BranchName || '';
      this.selectedBranch = '';
    } catch (e: any) {
      this.result = e.message || 'Failed to load branches';
      this.isError = true;
    } finally {
      this.loading = false;
    }
  }

  get targetBranches() {
    return this.branches.filter(b => !b.IsCurrent && !b.IsRemote);
  }

  async doMerge() {
    if (!this.selectedBranch || this.operating) return;
    this.operating = true;
    this.result = '';
    this.isError = false;
    try {
      const res = await twx.invokeService<string>(this.gitThing, 'Merge', { BranchName: this.selectedBranch });
      this.result = typeof res === 'string' ? res : (res as any)?.result || 'Merge completed';
    } catch (e: any) {
      this.result = e.message || 'Merge failed';
      this.isError = true;
    } finally {
      this.operating = false;
    }
  }

  async doRebase() {
    if (!this.selectedBranch || this.operating) return;
    this.operating = true;
    this.result = '';
    this.isError = false;
    try {
      const res = await twx.invokeService<string>(this.gitThing, 'Rebase', { UpstreamBranch: this.selectedBranch });
      this.result = typeof res === 'string' ? res : (res as any)?.result || 'Rebase completed';
    } catch (e: any) {
      this.result = e.message || 'Rebase failed';
      this.isError = true;
    } finally {
      this.operating = false;
    }
  }

  render() {
    return html`
      <div class=${this.loading || this.operating ? 'loading' : ''}>
        <div class="header">
          <h3>Merge / Rebase</h3>
          <ptcs-button label=${this.loading ? 'Loading...' : 'Refresh'} @click=${this.load} ?disabled=${this.loading}></ptcs-button>
        </div>

        <div class="current-branch">
          <span class="label">Current branch:</span>
          <span class="name">${this.currentBranch || '—'}</span>
        </div>

        ${this.targetBranches.length > 0 ? html`
          <form @submit=${(e: SubmitEvent) => { e.preventDefault(); this.doMerge(); }}>
          <div class="form">
            <label for="branch-select">Target branch</label>
            <select id="branch-select" .value=${this.selectedBranch} @change=${(e: Event) => this.selectedBranch = (e.target as HTMLSelectElement).value}>
              <option value="" ?selected=${!this.selectedBranch}>— Select a branch —</option>
              ${this.targetBranches.map(b => html`
                <option value=${b.BranchName} ?selected=${this.selectedBranch === b.BranchName}>${b.BranchName}</option>
              `)}
            </select>

            <div class="actions">
              <ptcs-button label="Merge into current" @click=${this.doMerge} ?disabled=${!this.selectedBranch || this.operating}></ptcs-button>
              <ptcs-button label="Rebase onto" @click=${this.doRebase} ?disabled=${!this.selectedBranch || this.operating}></ptcs-button>
            </div>
          </div>
          </form>
        ` : html`
          <div class="empty">No other local branches available</div>
        `}

        ${this.result ? html`<div class="result ${this.isError ? 'error' : 'success'}">${this.result}</div>` : ''}
      </div>
    `;
  }
}

customElements.define('git-merge-rebase', GitMergeRebase);
