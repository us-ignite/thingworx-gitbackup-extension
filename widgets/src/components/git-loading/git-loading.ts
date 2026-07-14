import { html, css } from 'lit';
import { GitElementBase } from '../git-base.js';
import { themeVars } from '../../lib/git-styles.js';
import { property } from 'lit/decorators.js';

export class GitLoading extends GitElementBase {
  static styles = [themeVars, css`
    :host { display: flex; align-items: center; justify-content: center; padding: 48px; }
    .spinner { width: 40px; height: 40px; border: 4px solid var(--git-color-border, #e0e0e0); border-top-color: var(--git-color-accent-light, #1976d2); border-radius: 50%; animation: spin 0.8s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .message { margin-left: 16px; color: var(--git-color-text-secondary, #666); font-size: 14px; }
  `];

  @property({ type: String }) message = 'Loading...';

  render() {
    return html`
      <div class="spinner"></div>
      <span class="message">${this.message}</span>
    `;
  }
}

if (!customElements.get('git-loading')) { customElements.define('git-loading', GitLoading); };
