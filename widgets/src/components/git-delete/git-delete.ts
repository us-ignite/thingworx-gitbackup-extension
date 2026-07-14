import { html, css } from 'lit';
import { GitElementBase } from '../git-base.js';
import { themeVars, statusStyles } from '../../lib/git-styles.js';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';

export class GitDelete extends GitElementBase {
  static styles = [themeVars, statusStyles, css`
    :host { display: block; padding: 16px; }
    .warning { background: var(--git-color-bg-warning, #fff3e0); border: 1px solid #ffcc02; border-radius: 4px; padding: 16px; margin-bottom: 16px; }
    .warning h3 { margin: 0 0 8px 0; color: var(--git-color-warning, #e65100); }
    .actions { display: flex; gap: 8px; justify-content: flex-end; }
  `];

  @property({ type: String }) gitThing = '';
  @property({ type: String }) thingName = '';
  @state() private deleting = false;
  @state() private message = '';
  @state() private isError = false;
  @state() private confirmed = false;

  async deleteThing() {
    if (!this.confirmed) { this.confirmed = true; return; }
    this.deleting = true;
    this.message = '';
    try {
      await twx.invokeService('GIT.Utility.Thing', 'DeleteEntities', {
        values: { thingname: this.thingName || this.gitThing }
      });
      this.message = `Deleted ${this.thingName || this.gitThing}`;
      this.isError = false;
      this.dispatchEvent(new CustomEvent('deleted', { detail: { thingName: this.thingName || this.gitThing } }));
    } catch (e: any) {
      this.message = e.message || 'Delete failed';
      this.isError = true;
    } finally {
      this.deleting = false;
    }
  }

  cancel() {
    this.confirmed = false;
    this.dispatchEvent(new CustomEvent('cancel'));
  }

  render() {
    return html`
      <div class="warning">
        <h3>Delete Repository</h3>
        ${this.confirmed
          ? html`<p>This action cannot be undone. Are you absolutely sure?</p>`
          : html`<p>You are about to delete <strong>${this.thingName || this.gitThing}</strong>. This will remove the GitBackup thing and its configuration.</p>`}
      
      </div>
      <div class="actions">
        <ptcs-button label="Cancel" @click=${this.cancel} ?disabled=${this.deleting}></ptcs-button>
        <ptcs-button label=${this.confirmed ? 'Confirm Delete' : 'Delete'} @click=${this.deleteThing} ?disabled=${this.deleting}></ptcs-button>
      </div>
      ${this.message ? html`<div class="result ${this.isError ? 'error' : 'success'}">${this.message}</div>` : ''}
    `;
  }
}

if (!customElements.get('git-delete')) { customElements.define('git-delete', GitDelete); };
