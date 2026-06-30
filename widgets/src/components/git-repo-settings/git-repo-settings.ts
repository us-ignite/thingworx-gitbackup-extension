import { LitElement, html, css } from 'lit';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { InfotableResponse } from '../../lib/twx-types.js';

interface RepoCreds {
  GitCommitterUser: string;
  GitCommitterPassword: string;
  GitCommitterEmail: string;
  GitCommitterFullName: string;
}

interface GpgKeyEntry {
  GitThing: string;
  GpgPrivateKey: string;
  GpgKeyPassphrase: string;
  SignCommits: boolean;
  GpgKeyFingerprint: string;
}

export class GitRepoSettings extends LitElement {
  static styles = css`
    :host { display: block; padding: 16px; font-family: sans-serif; }
    .section-title { font-size: 16px; font-weight: 600; color: #333; margin: 0 0 12px; border-bottom: 1px solid #e0e0e0; padding-bottom: 6px; }
    .section-title:not(:first-of-type) { margin-top: 24px; }
    .form-grid { display: grid; grid-template-columns: 180px 1fr; gap: 10px 16px; align-items: center; }
    .form-grid label { font-size: 13px; color: #555; text-align: right; }
    .form-grid input, .form-grid select { padding: 7px 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 13px; width: 100%; box-sizing: border-box; }
    .actions { margin-top: 16px; display: flex; gap: 8px; align-items: center; }
    .result { margin-top: 12px; padding: 12px; border-radius: 4px; font-size: 13px; }
    .result.success { background: #e8f5e9; color: #2e7d32; }
    .result.error { background: #ffebee; color: #c62828; }
    .loading { opacity: 0.6; pointer-events: none; }
    .card { border: 1px solid #e0e0e0; border-radius: 6px; padding: 16px; margin-bottom: 16px; }
    .hint { font-size: 11px; color: #999; grid-column: 2; margin-top: -4px; }
  `;

  @property({ type: String }) gitThing = '';
  @state() private creds: RepoCreds = { GitCommitterUser: '', GitCommitterPassword: '', GitCommitterEmail: '', GitCommitterFullName: '' };
  @state() private gpgKeys: GpgKeyEntry[] = [];
  @state() private selectedGpgThing = '';
  @state() private signCommits = false;
  @state() private loading = false;
  @state() private saving = false;
  @state() private message = '';
  @state() private isError = false;

  async connectedCallback() {
    super.connectedCallback();
    this.loadData();
  }

  async loadData() {
    if (!this.gitThing) return;
    this.loading = true;
    this.message = '';
    try {
      const [gpgRes] = await Promise.all([
        twx.invokeService<InfotableResponse<GpgKeyEntry>>('GIT.Utility.Thing', 'GetGpgKeys', {}),
      ]);
      this.gpgKeys = gpgRes.rows || [];
      const currentKey = this.gpgKeys.find(k => k.GitThing === this.gitThing);
      if (currentKey) {
        this.selectedGpgThing = currentKey.GitThing;
        this.signCommits = currentKey.SignCommits;
      }
    } catch { }
    finally { this.loading = false; }
  }

  async saveCredentials() {
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
      this.message = 'Repository credentials saved';
    } catch (e: any) {
      this.message = e.message || 'Failed to save credentials';
      this.isError = true;
    } finally {
      this.saving = false;
    }
  }

  async saveGpgSelection() {
    this.saving = true;
    this.message = '';
    this.isError = false;
    try {
      const key = this.gpgKeys.find(k => k.GitThing === this.selectedGpgThing);
      await twx.invokeService('GIT.Utility.Thing', 'SetGpgKey', {
        GitThing: this.gitThing,
        GpgPrivateKey: key?.GpgPrivateKey || '',
        GpgKeyPassphrase: key?.GpgKeyPassphrase || '',
        SignCommits: this.signCommits,
        GpgKeyFingerprint: key?.GpgKeyFingerprint || '',
      });
      this.message = 'GPG key selection saved for this repository';
    } catch (e: any) {
      this.message = e.message || 'Failed to save GPG selection';
      this.isError = true;
    } finally {
      this.saving = false;
    }
  }

  render() {
    const busy = this.loading || this.saving;
    const availableKeys = this.gpgKeys.filter(k => k.GpgKeyFingerprint);
    return html`
      <div class=${busy ? 'loading' : ''}>
        <div class="card">
          <div class="section-title">Repository Credentials</div>
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
        </div>

        <div class="card">
          <div class="section-title">GPG Signing Key</div>
          <div class="form-grid">
            <label>Signing Key</label>
            <select .value=${this.selectedGpgThing} @change=${(e: Event) => { this.selectedGpgThing = (e.target as HTMLSelectElement).value; }}>
              <option value="">None (no signing)</option>
              ${availableKeys.map(k => html`
                <option value=${k.GitThing}>${k.GpgKeyFingerprint}${k.GitThing !== this.gitThing ? ` (${k.GitThing})` : ''}</option>
              `)}
            </select>
            <span class="hint">Select a GPG key to sign commits for this repository. Manage keys in Extension Settings.</span>
            <label>Sign Commits</label>
            <div style="display:flex;align-items:center;gap:8px">
              <input type="checkbox" .checked=${this.signCommits} @change=${(e: Event) => { this.signCommits = (e.target as HTMLInputElement).checked; }} />
              <span style="font-size:13px;color:#666">Enable commit signing</span>
            </div>
          </div>
          <div class="actions">
            <ptcs-button label="Save GPG Selection" @click=${this.saveGpgSelection} ?disabled=${busy}></ptcs-button>
          </div>
        </div>

        ${this.message ? html`<div class="result ${this.isError ? 'error' : 'success'}">${this.message}</div>` : ''}
      </div>
    `;
  }
}

customElements.define('git-repo-settings', GitRepoSettings);
