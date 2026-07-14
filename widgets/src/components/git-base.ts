import {css, html, LitElement} from 'lit';
import type {CSSResultGroup} from 'lit';

export class GitElementBase extends LitElement {
  static get properties() {
    return {
      loadError: {state: true},
    };
  }

  static styles: CSSResultGroup = css`
    .git-load-error {
      margin: 12px 0;
      padding: 12px;
      border-radius: 4px;
      background: var(--git-color-bg-error, #ffebee);
      color: var(--git-color-error, #c62828);
    }
  `;

  protected loadError = '';

  protected clearLoadError(): void {
    this.loadError = '';
  }

  protected reportLoadError(action: string, error: unknown): void {
    const detail = error instanceof Error ? error.message : String(error);
    this.loadError = `${action}: ${detail}`;
    console.error(`[${this.localName || this.constructor.name}] ${action}`, error);
  }

  protected renderLoadError() {
    return this.loadError
      ? html`<div class="git-load-error" role="alert">${this.loadError}</div>`
      : null;
  }
}
