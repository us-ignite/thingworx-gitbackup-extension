import { LitElement, html, css } from 'lit';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { InfotableResponse } from '../../lib/twx-types.js';

interface UserProps {
  GitCommitterName: string;
  GitCommitterEmail: string;
  UseGitCommitUserValues: boolean;
}

interface GitThingItem {
  GitThingName: string;
}

export class GitPushSettings extends LitElement {
  static styles = css`
    :host { display: block; padding: 16px; font-family: sans-serif; }
    .section-title { font-size: 16px; font-weight: 600; color: #333; margin: 0 0 12px; border-bottom: 1px solid #e0e0e0; padding-bottom: 6px; }
    .section-title:not(:first-of-type) { margin-top: 24px; }
    .form-grid { display: grid; grid-template-columns: 180px 1fr; gap: 10px 16px; align-items: center; }
    .form-grid label { font-size: 13px; color: #555; text-align: right; }
    .form-grid input, .form-grid select { padding: 7px 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 13px; width: 100%; box-sizing: border-box; }
    .form-grid input[type="checkbox"] { width: auto; }
    .checkbox-row { display: flex; align-items: center; gap: 8px; }
    .actions { margin-top: 16px; display: flex; gap: 8px; align-items: center; }
    .result { margin-top: 12px; padding: 12px; border-radius: 4px; font-size: 13px; }
    .result.success { background: #e8f5e9; color: #2e7d32; }
    .result.error { background: #ffebee; color: #c62828; }
    .loading { opacity: 0.6; pointer-events: none; }
    .card { border: 1px solid #e0e0e0; border-radius: 6px; padding: 16px; margin-bottom: 16px; }
    .hint { font-size: 11px; color: #999; grid-column: 2; margin-top: -4px; }
  `;

  @property({ type: String }) gitThing = '';
  @state() private userProps: UserProps = { GitCommitterName: '', GitCommitterEmail: '', UseGitCommitUserValues: true };
  @state() private creds = { GitCommitterUser: '', GitCommitterPassword: '', GitCommitterEmail: '', GitCommitterFullName: '' };
  @state() private loading = false;
  @state() private saving = false;
  @state() private message = '';
  @state() private isError = false;

  async connectedCallback() {
    super.connectedCallback();
    this.loadAll();
  }

  async loadAll() {
    this.loading = true;
    this.message = '';
    try {
      const [userRes] = await Promise.all([
        twx.invokeService<InfotableResponse<UserProps>>('GIT.Utility.Thing', 'GetGitUserExtensionsProperties', {}),
      ]);
      const row = userRes.rows?.[0];
      if (row) this.userProps = { ...row };
      if (this.gitThing) this.loadCredentials();
    } catch { }
    finally { this.loading = false; }
  }

  async loadCredentials() {
    if (!this.gitThing) return;
    // Credentials aren't returned by any service, so we let user fill them in
  }

  async saveUserProps() {
    this.saving = true;
    this.message = '';
    this.isError = false;
    try {
      await twx.invokeService('GIT.Utility.Thing', 'SetGitUserExtensionsProperties', {
        GitCommitterName: this.userProps.GitCommitterName,
        GitCommitterEmail: this.userProps.GitCommitterEmail,
        UseGitCommitUserValues: this.userProps.UseGitCommitUserValues,
      });
      this.message = 'User settings saved';
    } catch (e: any) {
      this.message = e.message || 'Failed to save';
      this.isError = true;
    } finally {
      this.saving = false;
    }
  }

  async saveCredentials() {
    if (!this.gitThing) return;
    this.saving = true;
    this.message = '';
    this.isError = false;
    try {
      await twx.invokeService('GIT.Utility.Thing', 'SetGitCredentials', {
        GitCommitterUser: this.creds.GitCommitterUser,
        GitCommitterPassword: this.creds.GitCommitterPassword,
        GitCommitterEmail: this.creds.GitCommitterEmail,
        GitCommitterFullName: this.creds.GitCommitterFullName,
        GitThing: this.gitThing,
      });
      this.message = `Credentials saved for ${this.gitThing}`;
    } catch (e: any) {
      this.message = e.message || 'Failed to save credentials';
      this.isError = true;
    } finally {
      this.saving = false;
    }
  }

  render() {
    const busy = this.loading || this.saving;
    return html`
      <div class=${busy ? 'loading' : ''}>
        <div class="card">
          <div class="section-title">User Settings</div>
          <div class="form-grid">
            <label>Committer Name</label>
            <input .value=${this.userProps.GitCommitterName} @input=${(e: InputEvent) => { this.userProps = { ...this.userProps, GitCommitterName: (e.target as HTMLInputElement).value }; }} />

            <label>Committer Email</label>
            <input .value=${this.userProps.GitCommitterEmail} @input=${(e: InputEvent) => { this.userProps = { ...this.userProps, GitCommitterEmail: (e.target as HTMLInputElement).value }; }} />

            <div class="checkbox-row" style="grid-column:2">
              <input type="checkbox" .checked=${this.userProps.UseGitCommitUserValues} @change=${(e: Event) => { this.userProps = { ...this.userProps, UseGitCommitUserValues: (e.target as HTMLInputElement).checked }; }} />
              <label style="text-align:left">Use these values for all repositories</label>
            </div>
            <span class="hint">When unchecked, each repo can have its own committer identity</span>
          </div>
          <div class="actions">
            <ptcs-button label="Save Settings" @click=${this.saveUserProps} ?disabled=${busy}></ptcs-button>
          </div>
        </div>

        <div class="card">
          <div class="section-title">Repository Credentials</div>
          ${this.gitThing ? html`
            <div class="form-grid">
              <label>Username</label>
              <input .value=${this.creds.GitCommitterUser} @input=${(e: InputEvent) => { this.creds = { ...this.creds, GitCommitterUser: (e.target as HTMLInputElement).value }; }} />

              <label>Password / Token</label>
              <input type="password" .value=${this.creds.GitCommitterPassword} @input=${(e: InputEvent) => { this.creds = { ...this.creds, GitCommitterPassword: (e.target as HTMLInputElement).value }; }} />

              <label>Email</label>
              <input .value=${this.creds.GitCommitterEmail} @input=${(e: InputEvent) => { this.creds = { ...this.creds, GitCommitterEmail: (e.target as HTMLInputElement).value }; }} />

              <label>Full Name</label>
              <input .value=${this.creds.GitCommitterFullName} @input=${(e: InputEvent) => { this.creds = { ...this.creds, GitCommitterFullName: (e.target as HTMLInputElement).value }; }} />
            </div>
            <div class="actions">
              <ptcs-button label="Save Credentials" @click=${this.saveCredentials} ?disabled=${busy}></ptcs-button>
            </div>
          ` : html`<div style="color:#999;padding:12px 0">Set a GitThing property on this widget to manage per-repository credentials.</div>`}
        </div>

        ${this.message ? html`<div class="result ${this.isError ? 'error' : 'success'}">${this.message}</div>` : ''}
      </div>
    `;
  }
}

customElements.define('git-push-settings', GitPushSettings);
