import { LitElement, html, css } from 'lit';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';

export class GitPull extends LitElement {
  static styles = css`
    :host { display: block; padding: 16px; }
    .result { margin-top: 12px; padding: 12px; background: #f5f5f5; border-radius: 4px; white-space: pre-wrap; }
    .error { color: #c62828; }
    .success { color: #2e7d32; }
    .loading { opacity: 0.6; pointer-events: none; }
    button { padding: 8px 24px; cursor: pointer; }
  `;

  @property({ type: String }) gitThing = '';
  @state() private loading = false;
  @state() private result = '';
  @state() private error = '';

  async pull(): Promise<void> {
    if (!this.gitThing || this.loading) return;
    this.loading = true;
    this.result = '';
    this.error = '';
    try {
      const res = await twx.invokeService(this.gitThing, 'Pull', {}) as any;
      this.result = res?.result || 'Pull completed successfully';
    } catch (e: any) {
      this.error = e.message || 'Pull failed';
    } finally {
      this.loading = false;
    }
  }

  render() {
    return html`
      <div class=${this.loading ? 'loading' : ''}>
        <ptcs-button label="Pull from Remote" @click=${this.pull} ?disabled=${this.loading}>
        </ptcs-button>
        ${this.loading ? html`<span> Pulling...</span>` : ''}
        ${this.result ? html`<div class="result success">${this.result}</div>` : ''}
        ${this.error ? html`<div class="result error">${this.error}</div>` : ''}
      </div>
    `;
  }
}

customElements.define('git-pull', GitPull);
