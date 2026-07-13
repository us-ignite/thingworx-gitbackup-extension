import { LitElement, html, css } from 'lit';
import { state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { InfotableResponse } from '../../lib/twx-types.js';

interface UserProps {
  GitCommitterName: string;
  GitCommitterEmail: string;
  UseGitCommitUserValues: boolean;
}

interface GpgKeyEntry {
  GitThing: string;
  GpgPrivateKey: string;
  GpgKeyPassphrase: string;
  SignCommits: boolean;
  GpgKeyFingerprint: string;
}

export class GitBackupExtensionSettings extends LitElement {
  static styles = css`
    :host { display: block; padding: 16px; font-family: sans-serif; }
    .section-title { font-size: 16px; font-weight: 600; color: #333; margin: 0 0 12px; border-bottom: 1px solid #e0e0e0; padding-bottom: 6px; }
    .section-title:not(:first-of-type) { margin-top: 24px; }
    .form-grid { display: grid; grid-template-columns: 180px 1fr; gap: 10px 16px; align-items: center; }
    .form-grid label { font-size: 13px; color: #555; text-align: right; }
    .form-grid input, .form-grid select, .form-grid textarea { padding: 7px 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 13px; width: 100%; box-sizing: border-box; }
    .form-grid textarea { min-height: 80px; font-family: monospace; font-size: 12px; }
    .form-grid input[type="checkbox"] { width: auto; }
    .checkbox-row { display: flex; align-items: center; gap: 8px; }
    .actions { margin-top: 16px; display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .result { margin-top: 12px; padding: 12px; border-radius: 4px; font-size: 13px; }
    .result.success { background: #e8f5e9; color: #2e7d32; }
    .result.error { background: #ffebee; color: #c62828; }
    .loading { opacity: 0.6; pointer-events: none; }
    .card { border: 1px solid #e0e0e0; border-radius: 6px; padding: 16px; margin-bottom: 16px; }
    .hint { font-size: 11px; color: #999; grid-column: 2; margin-top: -4px; }
    .key-list { border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden; }
    .key-row { display: flex; align-items: center; padding: 10px 14px; border-bottom: 1px solid #f0f0f0; gap: 12px; }
    .key-row:last-child { border-bottom: none; }
    .key-row:hover { background: #fafafa; }
    .key-thing { font-weight: 600; flex: 1; }
    .key-fingerprint { font-family: monospace; font-size: 12px; color: #1565c0; }
    .key-sign { font-size: 12px; padding: 2px 8px; border-radius: 4px; }
    .key-sign.on { background: #e8f5e9; color: #2e7d32; }
    .key-sign.off { background: #f5f5f5; color: #999; }
    .empty-state { text-align: center; padding: 24px; color: #999; }
  `;

  @state() private userProps: UserProps = { GitCommitterName: '', GitCommitterEmail: '', UseGitCommitUserValues: true };
  @state() private keys: GpgKeyEntry[] = [];
  @state() private form: GpgKeyEntry = { GitThing: '', GpgPrivateKey: '', GpgKeyPassphrase: '', SignCommits: true, GpgKeyFingerprint: '' };
  @state() private loading = false;
  @state() private saving = false;
  @state() private message = '';
  @state() private isError = false;
  @state() private verifyResult = '';

  async connectedCallback() {
    super.connectedCallback();
    this.loadAll();
  }

  async loadAll() {
    this.loading = true;
    this.message = '';
    try {
      const [userRes, gpgRes] = await Promise.all([
        twx.invokeService<InfotableResponse<UserProps>>('GIT.Utility.Thing', 'GetGitUserExtensionsProperties', {}),
        twx.invokeService<InfotableResponse<GpgKeyEntry>>('GIT.Utility.Thing', 'GetGpgKeys', {}),
      ]);
      const row = userRes.rows?.[0];
      if (row) this.userProps = { ...row };
      this.keys = gpgRes.rows || [];
    } catch { }
    finally { this.loading = false; }
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

  async saveGpgKey() {
    this.saving = true;
    this.message = '';
    this.isError = false;
    this.verifyResult = '';
    try {
      await twx.invokeService('GIT.Utility.Thing', 'SetGpgKey', {
        GitThing: this.form.GitThing || 'GIT.Utility.Thing',
        GpgPrivateKey: this.form.GpgPrivateKey,
        GpgKeyPassphrase: this.form.GpgKeyPassphrase,
        SignCommits: this.form.SignCommits,
        GpgKeyFingerprint: this.form.GpgKeyFingerprint,
      });
      this.message = 'GPG key saved';
      this.form = { GitThing: '', GpgPrivateKey: '', GpgKeyPassphrase: '', SignCommits: true, GpgKeyFingerprint: '' };
      await this.loadAll();
    } catch (e: any) {
      this.message = e.message || 'Failed to save';
      this.isError = true;
    } finally {
      this.saving = false;
    }
  }

  async deleteGpgKey(gitThing: string) {
    this.saving = true;
    this.message = '';
    this.isError = false;
    try {
      await twx.invokeService('GIT.Utility.Thing', 'DeleteGpgKey', { GitThing: gitThing });
      this.message = `GPG key deleted`;
      await this.loadAll();
    } catch (e: any) {
      this.message = e.message || 'Failed to delete';
      this.isError = true;
    } finally {
      this.saving = false;
    }
  }

  async verifyGpgKey() {
    if (!this.form.GpgPrivateKey) return;
    this.saving = true;
    this.verifyResult = '';
    try {
      const res = await twx.invokeService<InfotableResponse<{ fingerprint: string }>>('GIT.Utility.Thing', 'VerifyGpgKey', {
        GpgPrivateKey: this.form.GpgPrivateKey,
        GpgKeyPassphrase: this.form.GpgKeyPassphrase,
      });
      const fp = res.rows?.[0]?.fingerprint || '';
      this.verifyResult = fp ? `Fingerprint: ${fp}` : 'Key is valid';
      if (fp) this.form = { ...this.form, GpgKeyFingerprint: fp };
    } catch (e: any) {
      this.verifyResult = `Verification failed: ${e.message}`;
    } finally {
      this.saving = false;
    }
  }

  editGpgKey(k: GpgKeyEntry) {
    this.form = { ...k };
  }

  render() {
    const busy = this.loading || this.saving;
    const keysWithFingerprints = this.keys.filter(k => k.GpgKeyFingerprint);
    return html`
      <div class=${busy ? 'loading' : ''}>
        <div class="card">
          <form @submit=${(e: SubmitEvent) => { e.preventDefault(); this.saveUserProps(); }}>
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
            <span class="hint">When unchecked, each repo can have its own committer identity set in Repo Settings</span>
          </div>
          <div class="actions">
            <ptcs-button label="Save Settings" @click=${this.saveUserProps} ?disabled=${busy}></ptcs-button>
          </div>
          </form>
        </div>

        <div class="card">
          <div class="section-title">GPG Keys</div>
          ${keysWithFingerprints.length > 0 ? html`
            <div class="key-list">
              ${keysWithFingerprints.map(k => html`
                <div class="key-row">
                  <span class="key-thing">${k.GitThing}</span>
                  <span class="key-fingerprint">${k.GpgKeyFingerprint}</span>
                  <span class="key-sign ${k.SignCommits ? 'on' : 'off'}">${k.SignCommits ? 'Signing ON' : 'Signing OFF'}</span>
                  <ptcs-button label="Edit" @click=${() => this.editGpgKey(k)}></ptcs-button>
                  <ptcs-button label="Delete" @click=${() => this.deleteGpgKey(k.GitThing)}></ptcs-button>
                </div>
              `)}
            </div>
          ` : html`<div class="empty-state">No GPG keys configured</div>`}

          <form @submit=${(e: SubmitEvent) => { e.preventDefault(); this.saveGpgKey(); }}>
          <div style="margin-top:16px">
            <div class="section-title">${this.form.GpgPrivateKey ? 'Edit' : 'Add'} GPG Key</div>
            <div class="form-grid">
              <label>Label</label>
              <input .value=${this.form.GitThing} @input=${(e: InputEvent) => { this.form = { ...this.form, GitThing: (e.target as HTMLInputElement).value }; }} placeholder="e.g. My Work Key" />
              <label>Private Key (ASCII-armored)</label>
              <textarea .value=${this.form.GpgPrivateKey} @input=${(e: InputEvent) => { this.form = { ...this.form, GpgPrivateKey: (e.target as HTMLTextAreaElement).value }; }} placeholder="-----BEGIN PGP PRIVATE KEY-----"></textarea>
              <label>Passphrase</label>
              <input type="password" .value=${this.form.GpgKeyPassphrase} @input=${(e: InputEvent) => { this.form = { ...this.form, GpgKeyPassphrase: (e.target as HTMLInputElement).value }; }} />
              <label>Fingerprint</label>
              <input .value=${this.form.GpgKeyFingerprint} @input=${(e: InputEvent) => { this.form = { ...this.form, GpgKeyFingerprint: (e.target as HTMLInputElement).value }; }} placeholder="Will be filled by Verify" />
              <div class="checkbox-row" style="grid-column:2">
                <input type="checkbox" .checked=${this.form.SignCommits} @change=${(e: Event) => { this.form = { ...this.form, SignCommits: (e.target as HTMLInputElement).checked }; }} />
                <label style="text-align:left">Sign commits with this key by default</label>
              </div>
            </div>
            <div class="actions">
              <ptcs-button label="Verify Key" @click=${this.verifyGpgKey} ?disabled=${!this.form.GpgPrivateKey || busy}></ptcs-button>
              <ptcs-button label="Save Key" @click=${this.saveGpgKey} ?disabled=${busy}></ptcs-button>
              ${this.verifyResult ? html`<span style="font-size:12px;color:#666">${this.verifyResult}</span>` : ''}
            </div>
          </div>
          </form>
        </div>

        ${this.message ? html`<div class="result ${this.isError ? 'error' : 'success'}">${this.message}</div>` : ''}
      </div>
    `;
  }
}

customElements.define('git-backup-extension-settings', GitBackupExtensionSettings);
