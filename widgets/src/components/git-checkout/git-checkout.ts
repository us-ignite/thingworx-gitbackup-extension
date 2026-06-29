import { LitElement, html, css } from 'lit';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { InfotableResponse, GitBranch, GitCurrentBranch } from '../../lib/twx-types.js';

export class GitCheckout extends LitElement {
  static styles = css`
    :host { display: block; padding: 16px; }
    .branch-list { margin: 12px 0; }
    .branch-row { display: flex; align-items: center; padding: 8px 12px; border: 1px solid #e0e0e0; border-radius: 4px; margin-bottom: 4px; cursor: pointer; }
    .branch-row:hover { background: #f5f5f5; }
    .branch-row.active { background: #e3f2fd; border-color: #1976d2; }
    .branch-name { flex: 1; font-weight: 500; }
    .current-header { background: #e8f5e9; border: 1px solid #a5d6a7; border-radius: 4px; padding: 10px 16px; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
    .branch-type { font-size: 12px; color: #666; }
    .badge { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 11px; background: #e8eaf6; margin-left: 8px; }
    .badge.current { background: #c8e6c9; }
    .result { margin-top: 12px; padding: 12px; border-radius: 4px; }
    .error { background: #ffebee; color: #c62828; }
    .success { background: #e8f5e9; color: #2e7d32; }
    input { width: 100%; padding: 8px; box-sizing: border-box; margin-bottom: 8px; border: 1px solid #ccc; border-radius: 4px; }
  `;

  @property({ type: String }) gitThing = '';
  @state() private branches: GitBranch[] = [];
  @state() private currentBranch = '';
  @state() private loading = false;
  @state() private newBranchName = '';
  @state() private message = '';
  @state() private isError = false;
  @state() private loaded = false;

  async connectedCallback() {
    super.connectedCallback();
    if (this.gitThing) this.loadBranches();
  }

  async loadBranches() {
    if (!this.gitThing) return;
    this.loading = true;
    try {
      const [br, cur] = await Promise.all([
        twx.invokeService<InfotableResponse<GitBranch>>(this.gitThing, 'GetBranchList', {}),
        twx.invokeService<{ rows: GitCurrentBranch[] }>(this.gitThing, 'GetCurrentBranch', {}),
      ]);
      this.branches = br.rows || [];
      this.currentBranch = cur.rows?.[0]?.BranchName
        ?.replace('refs/heads/', '')
        ?.replace('refs/remotes/origin/', '') || '';
    } catch { }
    finally { this.loading = false; this.loaded = true; }
  }

  async checkout(branch: string) {
    this.message = '';
    try {
      await twx.invokeService(this.gitThing, 'Checkout', { BranchNameOrCommit: branch });
      this.message = `Switched to branch ${branch}`;
      this.isError = false;
      this.currentBranch = branch;
    } catch (e: any) {
      this.message = e.message || 'Checkout failed';
      this.isError = true;
    }
  }

  async createAndCheckout() {
    if (!this.newBranchName.trim()) return;
    this.message = '';
    try {
      await twx.invokeService(this.gitThing, 'CreateBranch', { BranchName: this.newBranchName.trim() });
      await this.checkout(this.newBranchName.trim());
      this.newBranchName = '';
      await this.loadBranches();
    } catch (e: any) {
      const msg = e.message || '';
      if (msg.includes('Ref HEAD cannot be resolved')) {
        this.message = 'Repository has no commits yet. Go to Push tab and push an initial commit first, then create branches.';
      } else {
        this.message = msg;
      }
      this.isError = true;
    }
  }

  render() {
    return html`
      <div>
        ${this.currentBranch ? html`
          <div class="current-header">
            <span style="font-weight:500">Current branch:</span>
            <span style="font-weight:700;color:#2e7d32">${this.currentBranch}</span>
          </div>
        ` : ''}
        <div style="display:flex;justify-content:space-between;align-items:center">
          <h3 style="margin:0">All Branches</h3>
          <ptcs-button label=${this.loading ? 'Loading...' : 'Refresh'} @click=${this.loadBranches} ?disabled=${this.loading}></ptcs-button>
        </div>

        <div style="margin:12px 0;display:flex;gap:8px">
          <input placeholder="New branch name" .value=${this.newBranchName} @input=${(e: any) => this.newBranchName = e.target.value}>
          <ptcs-button label="Create & Checkout" @click=${this.createAndCheckout} ?disabled=${!this.newBranchName.trim()}></ptcs-button>
        </div>

        <div class="branch-list">
          ${this.branches.map(b => html`
            <div class="branch-row ${b.BranchName === this.currentBranch ? 'active' : ''}" @click=${() => this.checkout(b.BranchName)}>
              <span class="branch-name">${b.ShortBranchName || b.BranchName}</span>
              <span class="branch-type">${b.BranchType}</span>
              ${b.BranchName === this.currentBranch ? html`<span class="badge current">current</span>` : ''}
            </div>
          `)}
          ${this.loaded && this.branches.length === 0 ? html`<div style="color:#999;text-align:center;padding:24px">No branches found</div>` : ''}
        </div>

        ${this.message ? html`<div class="result ${this.isError ? 'error' : 'success'}">${this.message}</div>` : ''}
      </div>
    `;
  }
}

customElements.define('git-checkout', GitCheckout);
