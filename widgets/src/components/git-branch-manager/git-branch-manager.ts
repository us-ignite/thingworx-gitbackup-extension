import { LitElement, html, css } from 'lit';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { InfotableResponse } from '../../lib/twx-types.js';

interface BranchRow {
  BranchName: string;
  IsRemote: boolean;
  IsCurrent: boolean;
}

export class GitBranchManager extends LitElement {
  static styles = css`
    :host { display: block; padding: 16px; font-family: sans-serif; }
    .header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
    .title { font-size: 18px; font-weight: 600; margin: 0; }
    .current-badge { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 12px; background: #c8e6c9; color: #2e7d32; font-weight: 600; margin-left: 8px; }
    .branch-list { border: 1px solid #e0e0e0; border-radius: 4px; overflow: hidden; margin-bottom: 16px; }
    .branch-row { display: flex; align-items: center; padding: 8px 12px; border-bottom: 1px solid #f0f0f0; gap: 8px; }
    .branch-row:last-child { border-bottom: none; }
    .branch-row:hover { background: #fafafa; }
    .branch-row.current { background: #e8f5e9; }
    .branch-name { flex: 1; font-weight: 500; }
    .branch-tag { font-size: 11px; padding: 2px 6px; border-radius: 4px; background: #eceff1; color: #546e7a; }
    .branch-tag.remote { background: #fff3e0; color: #e65100; }
    .section-title { font-weight: 600; margin: 12px 0 6px; color: #555; }
    .input-row { display: flex; gap: 8px; margin-bottom: 8px; }
    input { flex: 1; padding: 8px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
    .result { margin-top: 8px; padding: 10px; border-radius: 4px; font-size: 14px; }
    .error { background: #ffebee; color: #c62828; }
    .success { background: #e8f5e9; color: #2e7d32; }
    .loading { opacity: 0.6; pointer-events: none; }
    .actions { display: flex; gap: 8px; }
  `;

  @property({ type: String }) gitThing = '';
  @state() private branches: BranchRow[] = [];
  @state() private currentBranch = '';
  @state() private loading = false;
  @state() private createName = '';
  @state() private checkoutName = '';
  @state() private deleteTarget = '';
  @state() private message = '';
  @state() private isError = false;

  async connectedCallback() {
    super.connectedCallback();
    if (this.gitThing) this.loadBranches();
  }

  async loadBranches() {
    if (!this.gitThing) return;
    this.loading = true;
    this.message = '';
    try {
      const [branchListRes, curRes] = await Promise.all([
        twx.invokeService<InfotableResponse<BranchRow>>(this.gitThing, 'GetBranchList', {}),
        twx.invokeService<InfotableResponse<{ BranchName: string }>>(this.gitThing, 'GetCurrentBranch', {}),
      ]);
      this.branches = branchListRes.rows || [];
      this.currentBranch = curRes.rows?.[0]?.BranchName || '';
    } catch { }
    finally { this.loading = false; }
  }

  async createBranch() {
    const name = this.createName.trim();
    if (!name) return;
    this.message = '';
    try {
      await twx.invokeService(this.gitThing, 'CreateBranch', { BranchName: name });
      this.message = `Created branch ${name}`;
      this.isError = false;
      this.createName = '';
      await this.loadBranches();
    } catch (e: any) {
      this.message = e.message || 'Failed to create branch';
      this.isError = true;
    }
  }

  async checkout() {
    const target = this.checkoutName.trim();
    if (!target) return;
    this.message = '';
    try {
      await twx.invokeService(this.gitThing, 'Checkout', { BranchNameOrCommit: target });
      this.message = `Switched to ${target}`;
      this.isError = false;
      this.checkoutName = '';
      await this.loadBranches();
    } catch (e: any) {
      this.message = e.message || 'Checkout failed';
      this.isError = true;
    }
  }

  async deleteBranch(branch: string) {
    if (this.deleteTarget !== branch) {
      this.deleteTarget = branch;
      this.message = '';
      return;
    }
    this.message = '';
    try {
      await twx.invokeService(this.gitThing, 'DeleteLocalBranch', { BranchName: branch, Force: true });
      this.message = `Deleted branch ${branch}`;
      this.isError = false;
      this.deleteTarget = '';
      await this.loadBranches();
    } catch (e: any) {
      this.message = e.message || 'Failed to delete branch';
      this.isError = true;
      this.deleteTarget = '';
    }
  }

  DoRefresh() {
    this.loadBranches();
  }

  DoCreateBranch(name: string) {
    this.createName = name;
    this.createBranch();
  }

  DoCheckout(target: string) {
    this.checkoutName = target;
    this.checkout();
  }

  DoDeleteBranch(name: string) {
    this.deleteBranch(name);
  }

  render() {
    return html`
      <div class=${this.loading ? 'loading' : ''}>
        <div class="header">
          <h3 class="title">Branches <span class="current-badge">${this.currentBranch}</span></h3>
          <ptcs-button label="Refresh" @click=${this.loadBranches} ?disabled=${this.loading}></ptcs-button>
        </div>

        <div class="section-title">Create Branch</div>
        <div class="input-row">
          <input placeholder="Branch name" .value=${this.createName} @input=${(e: InputEvent) => this.createName = (e.target as HTMLInputElement).value}>
          <ptcs-button label="Create" @click=${this.createBranch} ?disabled=${!this.createName.trim()}></ptcs-button>
        </div>

        <div class="section-title">Checkout</div>
        <div class="input-row">
          <input placeholder="Branch name or commit hash" .value=${this.checkoutName} @input=${(e: InputEvent) => this.checkoutName = (e.target as HTMLInputElement).value}>
          <ptcs-button label="Checkout" @click=${this.checkout} ?disabled=${!this.checkoutName.trim()}></ptcs-button>
        </div>

        <div class="section-title">All Branches</div>
        <div class="branch-list">
          ${this.branches.map(b => html`
            <div class="branch-row ${b.BranchName === this.currentBranch ? 'current' : ''}">
              <span class="branch-name">${b.BranchName}</span>
              ${b.BranchName === this.currentBranch ? html`<span class="branch-tag">HEAD</span>` : ''}
              ${b.IsRemote ? html`<span class="branch-tag remote">remote</span>` : ''}
              ${!b.IsRemote && b.BranchName !== this.currentBranch ? html`
                <ptcs-button label=${this.deleteTarget === b.BranchName ? 'Confirm?' : 'Delete'} @click=${() => this.deleteBranch(b.BranchName)}></ptcs-button>
              ` : ''}
            </div>
          `)}
          ${this.branches.length === 0 ? html`<div style="text-align:center;padding:24px;color:#999">No branches found</div>` : ''}
        </div>

        ${this.message ? html`<div class="result ${this.isError ? 'error' : 'success'}">${this.message}</div>` : ''}
      </div>
    `;
  }
}

customElements.define('git-branch-manager', GitBranchManager);
