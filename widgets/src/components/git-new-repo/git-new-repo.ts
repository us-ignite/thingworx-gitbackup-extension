import { LitElement, html, css } from 'lit';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';

interface FormData {
  repoName: string;
  gitRepoUrl: string;
  repoPath: string;
  fileRepo: string;
  projectName: string;
  username: string;
  password: string;
  commitUser: string;
  commitEmail: string;
  initialBranch: string;
  useProxy: boolean;
  proxyUrl: string;
  proxyPort: string;
  localizationTokensPrefix: string;
}

export class GitNewRepo extends LitElement {
  static styles = css`
    :host { display: block; padding: 16px; }
    .form-grid { display: grid; grid-template-columns: 180px 1fr; gap: 10px 16px; align-items: center; }
    .form-grid label { font-size: 13px; color: #555; text-align: right; }
    .form-grid input, .form-grid select { padding: 7px 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 13px; width: 100%; }
    .form-grid .full-row { grid-column: 1 / -1; }
    .section-title { font-size: 14px; font-weight: 600; color: #1976d2; margin: 16px 0 8px; border-bottom: 1px solid #e0e0e0; padding-bottom: 4px; }
    .section-title:first-of-type { margin-top: 0; }
    .actions { margin-top: 20px; display: flex; gap: 8px; align-items: center; }
    .result { margin-top: 12px; padding: 12px; background: #f5f5f5; border-radius: 4px; white-space: pre-wrap; font-size: 13px; }
    .error { color: #c62828; }
    .success { color: #2e7d32; }
    .loading { opacity: 0.6; pointer-events: none; }
    .checkbox-row { display: flex; align-items: center; gap: 8px; }
    .checkbox-row input[type="checkbox"] { width: auto; }
    .hint { font-size: 11px; color: #999; grid-column: 2; margin-top: -6px; }
  `;

  @property({ type: String }) gitThing = '';
  @state() private data: FormData = {
    repoName: 'GitBackup.DevRepo',
    gitRepoUrl: 'http://gitea:3000/testadmin/gitbackup-test.git',
    repoPath: 'GitBackup',
    fileRepo: 'GitRepository',
    projectName: '',
    username: 'testadmin',
    password: 'testadmin123',
    commitUser: 'testadmin',
    commitEmail: 'admin@example.com',
    initialBranch: 'main',
    useProxy: false,
    proxyUrl: '',
    proxyPort: '3281',
    localizationTokensPrefix: '',
  };
  @state() private loading = false;
  @state() private doingOp = false;
  @state() private result = '';
  @state() private error = '';
  @state() private createdThing = '';

  private updateField(field: keyof FormData, value: string | boolean) {
    this.data = { ...this.data, [field]: value };
  }

  async doCreate() {
    this.loading = true;
    this.result = '';
    this.error = '';
    this.createdThing = '';
    try {
      await twx.invokeService('GIT.Utility.Thing', 'AddNewRepo', {
        RepoName: this.data.repoName,
        GitRepoURL: this.data.gitRepoUrl,
        RepoPath: this.data.repoPath,
        FileRepo: this.data.fileRepo,
        User: this.data.username,
        Password: this.data.password,
        CommitUser: this.data.commitUser,
        CommitEmail: this.data.commitEmail,
        InitialBranch: this.data.initialBranch,
        UseProxy: this.data.useProxy,
        ProxyURL: this.data.proxyUrl || 'none',
        ProxyPort: parseInt(this.data.proxyPort) || 3281,
        LocalizationTokensPrefix: this.data.localizationTokensPrefix,
        ProjectName: this.data.projectName,
      });
      this.createdThing = this.data.repoName;
      this.result = `Repository "${this.data.repoName}" created successfully.`;
      this.dispatchEvent(new CustomEvent('repo-created', { detail: { thingName: this.data.repoName } }));
    } catch (e: any) {
      this.error = e.message || 'Failed to create repository';
    } finally {
      this.loading = false;
    }
  }

  private async enableThing(thingName: string) {
    this.doingOp = true;
    try {
      await twx.invokeService(thingName, 'EnableThing', {});
      this.result = `"${thingName}" enabled.`;
    } catch (e: any) {
      this.error = `Enable failed: ${e.message}`;
    } finally {
      this.doingOp = false;
    }
  }

  private async startThing(thingName: string) {
    this.doingOp = true;
    try {
      await twx.invokeService(thingName, 'RestartThing', {});
      this.result = `"${thingName}" started.`;
    } catch (e: any) {
      this.error = `Start failed: ${e.message}`;
    } finally {
      this.doingOp = false;
    }
  }

  render() {
    const d = this.data;
    const busy = this.loading || this.doingOp;
    return html`
      <div class=${busy ? 'loading' : ''}>
        <form @submit=${(e: SubmitEvent) => { e.preventDefault(); this.doCreate(); }}>
        <div class="section-title">Repository</div>
        <div class="form-grid">
          <label>Thing Name</label>
          <input .value=${d.repoName} @input=${(e: InputEvent) => this.updateField('repoName', (e.target as HTMLInputElement).value)} placeholder="GitBackup.MyRepo" />

          <label>Git Remote URL</label>
          <input .value=${d.gitRepoUrl} @input=${(e: InputEvent) => this.updateField('gitRepoUrl', (e.target as HTMLInputElement).value)} placeholder="https://github.com/user/repo.git" />

          <label>Local Path</label>
          <input .value=${d.repoPath} @input=${(e: InputEvent) => this.updateField('repoPath', (e.target as HTMLInputElement).value)} placeholder="GitBackup" />

          <label>File Repository</label>
          <input .value=${d.fileRepo} @input=${(e: InputEvent) => this.updateField('fileRepo', (e.target as HTMLInputElement).value)} placeholder="GitRepository" />
          <span class="hint">ThingWorx File Repository name</span>

          <label>Project Name</label>
          <input .value=${d.projectName} @input=${(e: InputEvent) => this.updateField('projectName', (e.target as HTMLInputElement).value)} placeholder="e.g. GitBackup" />
          <span class="hint">Syncs entities from this project on every Status/Push/Pull</span>

          <label>Initial Branch</label>
          <input .value=${d.initialBranch} @input=${(e: InputEvent) => this.updateField('initialBranch', (e.target as HTMLInputElement).value)} placeholder="main" />
        </div>

        <div class="section-title">Authentication</div>
        <div class="form-grid">
          <label>Username</label>
          <input .value=${d.username} @input=${(e: InputEvent) => this.updateField('username', (e.target as HTMLInputElement).value)} />

          <label>Password / Token</label>
          <input type="password" .value=${d.password} @input=${(e: InputEvent) => this.updateField('password', (e.target as HTMLInputElement).value)} />
        </div>

        <div class="section-title">Commit Author</div>
        <div class="form-grid">
          <label>Name</label>
          <input .value=${d.commitUser} @input=${(e: InputEvent) => this.updateField('commitUser', (e.target as HTMLInputElement).value)} />

          <label>Email</label>
          <input .value=${d.commitEmail} @input=${(e: InputEvent) => this.updateField('commitEmail', (e.target as HTMLInputElement).value)} />
        </div>

        <div class="section-title">Proxy <span style="font-weight:400;color:#999;font-size:12px">(optional)</span></div>
        <div class="form-grid">
          <div class="checkbox-row full-row">
            <input type="checkbox" .checked=${d.useProxy} @change=${(e: Event) => this.updateField('useProxy', (e.target as HTMLInputElement).checked)} />
            <label style="text-align:left">Use Proxy</label>
          </div>
          ${d.useProxy ? html`
            <label>Proxy URL</label>
            <input .value=${d.proxyUrl} @input=${(e: InputEvent) => this.updateField('proxyUrl', (e.target as HTMLInputElement).value)} placeholder="proxy.example.com" />
            <label>Port</label>
            <input .value=${d.proxyPort} @input=${(e: InputEvent) => this.updateField('proxyPort', (e.target as HTMLInputElement).value)} placeholder="3281" />
          ` : ''}
        </div>

        <div class="actions">
          <ptcs-button label="Create Repository" @click=${this.doCreate} ?disabled=${busy}></ptcs-button>
          ${busy ? html`<span>${this.loading ? 'Creating...' : 'Working...'}</span>` : ''}
        </div>

        ${this.createdThing ? html`
          <div class="result success">
            <div style="margin-bottom:8px">${this.result}</div>
            <div style="display:flex;gap:8px;flex-wrap:wrap">
              <ptcs-button label="Enable Thing" @click=${() => this.enableThing(this.createdThing)} ?disabled=${this.doingOp}></ptcs-button>
              <ptcs-button label="Start Thing" @click=${() => this.startThing(this.createdThing)} ?disabled=${this.doingOp}></ptcs-button>
              <span style="font-size:12px;color:#999;align-self:center">Now use this name in the GitThing field above</span>
            </div>
          </div>
        ` : ''}
        ${this.error && !this.createdThing ? html`<div class="result error">${this.error}</div>` : ''}
        ${this.error && this.createdThing ? html`<div class="result error">${this.error}</div>` : ''}
        </form>
      </div>
    `;
  }
}

customElements.define('git-new-repo', GitNewRepo);
