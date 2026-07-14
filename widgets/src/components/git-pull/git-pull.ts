import { html, css } from 'lit';
import { GitElementBase } from '../git-base.js';
import { themeVars, statusStyles } from '../../lib/git-styles.js';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import {readServiceResult, type ServiceResultResponse} from '../../lib/twx-types.js';

export class GitPull extends GitElementBase {
  static styles = [themeVars, statusStyles, css`
    :host { display: block; padding: 16px; }
    .result { margin-top: 12px; padding: 12px; background: var(--git-color-bg-hover, #f5f5f5); border-radius: var(--git-border-radius-sm, 4px); white-space: pre-wrap; }
    .loading { opacity: 0.6; pointer-events: none; }
    button { padding: 8px 24px; cursor: pointer; }
  `];

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
      const res = await twx.invokeService<ServiceResultResponse>(this.gitThing, 'Pull', {});
      const message = readServiceResult(res, 'Pull completed successfully');
      if (/^Pull Error:/i.test(message.trim())) throw new Error(message);
      this.result = message;
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

if (!customElements.get('git-pull')) { customElements.define('git-pull', GitPull); };
