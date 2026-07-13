import { LitElement, html, css } from 'lit';
import { property, state } from 'lit/decorators.js';
import '../git-push/git-push.js';
import '../git-pull/git-pull.js';
import '../git-branch-manager/git-branch-manager.js';
import '../git-status/git-status.js';
import '../git-delete/git-delete.js';
import '../git-repo-settings/git-repo-settings.js';
import '../git-log/git-log.js';

export class GitRepo extends LitElement {
  static styles = css`
    :host { display: block; padding: 16px; font-family: sans-serif; }
    .tabs { display: flex; gap: 4px; margin-bottom: 16px; border-bottom: 2px solid #e0e0e0; padding-bottom: 0; flex-wrap: wrap; }
    .tab { padding: 8px 18px; cursor: pointer; border: 1px solid transparent; border-bottom: none; border-radius: 4px 4px 0 0; font-size: 14px; font-weight: 500; color: #666; background: transparent; transition: all 0.15s; }
    .tab:hover { background: #f5f5f5; color: #333; }
    .tab.active { background: #fff; border-color: #e0e0e0; color: #1565c0; border-bottom: 2px solid #1565c0; margin-bottom: -2px; }
  `;

  @property({ type: String }) gitThing = '';
  @property({ type: String }) defaultTab = 'push';
  @state() private activeTab = 'push';

  connectedCallback() {
    super.connectedCallback();
    this.activeTab = this.defaultTab || 'push';
  }

  private switchTab(tab: string) {
    this.activeTab = tab;
  }

  render() {
    return html`
      <div class="tabs">
        ${this.renderTabHeader('Commit & Push', 'push')}
        ${this.renderTabHeader('Pull', 'pull')}
        ${this.renderTabHeader('Branch', 'branch')}
        ${this.renderTabHeader('Status', 'status')}
        ${this.renderTabHeader('Settings', 'settings')}
        ${this.renderTabHeader('Log', 'log')}
        ${this.renderTabHeader('Delete Repo', 'delete')}
      </div>
      <div class="tab-content">
        ${this.activeTab === 'push' ? html`<git-push .gitThing=${this.gitThing}></git-push>` : ''}
        ${this.activeTab === 'pull' ? html`<git-pull .gitThing=${this.gitThing}></git-pull>` : ''}
        ${this.activeTab === 'branch' ? html`<git-branch-manager .gitThing=${this.gitThing}></git-branch-manager>` : ''}
        ${this.activeTab === 'status' ? html`<git-status .gitThing=${this.gitThing}></git-status>` : ''}
        ${this.activeTab === 'settings' ? html`
          <git-repo-settings .gitThing=${this.gitThing}></git-repo-settings>
        ` : ''}
        ${this.activeTab === 'log' ? html`<git-log></git-log>` : ''}
        ${this.activeTab === 'delete' ? html`<git-delete .gitThing=${this.gitThing} .thingName=${this.gitThing} @deleted=${this.onRepoDeleted}></git-delete>` : ''}
      </div>
    `;
  }

  private renderTabHeader(label: string, key: string) {
    return html`
      <button class="tab ${this.activeTab === key ? 'active' : ''}" @click=${() => this.switchTab(key)}>
        ${label}
      </button>
    `;
  }

  private onRepoDeleted() {
    this.dispatchEvent(new CustomEvent('repo-deleted', { detail: { thingName: this.gitThing } }));
  }
}

customElements.define('git-repo', GitRepo);
