import { LitElement, html, css } from 'lit';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { InfotableResponse } from '../../lib/twx-types.js';

interface DirEntry {
  name: string;
  path: string;
  isDirectory: boolean;
}

interface ImportResult {
  EntityName: string;
  Status: string;
  Message: string;
}

export class GitImport extends LitElement {
  static styles = css`
    :host { display: block; padding: 16px; font-family: sans-serif; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .header h3 { margin: 0; }
    .form-grid { display: grid; grid-template-columns: 160px 1fr; gap: 10px 16px; align-items: center; margin-bottom: 16px; }
    .form-grid label { font-size: 13px; color: #555; text-align: right; }
    .form-grid input, .form-grid select { padding: 7px 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 13px; width: 100%; box-sizing: border-box; }
    .file-list { border: 1px solid #e0e0e0; border-radius: 4px; overflow: hidden; margin-bottom: 16px; }
    .file-row { display: flex; align-items: center; padding: 8px 12px; border-bottom: 1px solid #f0f0f0; gap: 8px; }
    .file-row:last-child { border-bottom: none; }
    .file-row:hover { background: #f5f5f5; }
    .file-name { flex: 1; }
    .file-icon { font-size: 16px; width: 20px; text-align: center; }
    .actions { margin-top: 12px; display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .result { margin-top: 12px; padding: 12px; border-radius: 4px; font-size: 13px; }
    .result.success { background: #e8f5e9; color: #2e7d32; }
    .result.error { background: #ffebee; color: #c62828; }
    .result.info { background: #e3f2fd; color: #1565c0; }
    .empty-state { text-align: center; padding: 24px; color: #999; }
    .loading { opacity: 0.6; pointer-events: none; }
    .card { border: 1px solid #e0e0e0; border-radius: 6px; padding: 16px; margin-bottom: 16px; }
    .section-title { font-size: 14px; font-weight: 600; color: #333; margin: 0 0 8px; }
    .bulk-row { display: flex; gap: 8px; align-items: center; }
    .bulk-row input { flex: 1; }
  `;

  @property({ type: String }) gitThing = '';
  @state() private repoPath = '';
  @state() private fileRepo = 'GitRepository';
  @state() private files: DirEntry[] = [];
  @state() private loading = false;
  @state() private importing = false;
  @state() private message = '';
  @state() private isError = false;
  @state() private isInfo = false;
  @state() private importResults: ImportResult[] = [];
  @state() private bulkPath = '';

  async connectedCallback() {
    super.connectedCallback();
    if (this.gitThing) this.loadRepoConfig();
  }

  async loadRepoConfig() {
    if (!this.gitThing) return;
    this.loading = true;
    try {
      const res = await twx.invokeService<InfotableResponse<{ FileRepository: string; FileRepoPath: string }>>('GIT.Utility.Thing', 'GetRepoConfiguration', { GitThingName: this.gitThing });
      const row = res.rows?.[0];
      if (row) {
        this.fileRepo = row.FileRepository || 'GitRepository';
        this.repoPath = row.FileRepoPath || '';
        this.bulkPath = row.FileRepoPath || '';
      }
      this.loadFiles();
    } catch { }
    finally { this.loading = false; }
  }

  async loadFiles() {
    if (!this.gitThing || !this.repoPath) return;
    this.loading = true;
    try {
      const res = await twx.invokeService<InfotableResponse<DirEntry>>('GIT.Utility.Thing', 'GetRecursiveFileListing', { GitThingName: this.gitThing });
      this.files = (res.rows || []).filter(f => f.name.endsWith('.xml'));
    } catch {
      this.files = [];
    } finally {
      this.loading = false;
    }
  }

  async importFile(fileName: string) {
    this.importing = true;
    this.message = '';
    this.isError = false;
    this.isInfo = false;
    try {
      const entityPath = `${this.repoPath}/${fileName}`;
      await twx.invokeServiceWithInit('GIT.Utility.Thing', 'ImportEntity', {
        entityPath: entityPath,
        FileRepositoryName: this.fileRepo,
        ignoreDependencies: false,
      });
      this.message = `Imported ${fileName}`;
      this.isInfo = true;
    } catch (e: any) {
      this.message = e.message || `Failed to import ${fileName}`;
      this.isError = true;
    } finally {
      this.importing = false;
    }
  }

  async bulkImport() {
    if (!this.bulkPath) return;
    this.importing = true;
    this.message = '';
    this.isError = false;
    this.importResults = [];
    try {
      const res = await twx.invokeServiceWithInit<InfotableResponse<ImportResult>>('GIT.Utility.Thing', 'ImportProjectEntities', {
        GitThingName: this.gitThing,
        entityPath: this.bulkPath,
        ignoreDependencies: false,
      });
      this.importResults = res.rows || [];
      const success = this.importResults.filter(r => r.Status === 'Success').length;
      const failed = this.importResults.filter(r => r.Status === 'Failure').length;
      this.message = `Bulk import complete: ${success} succeeded, ${failed} failed`;
    } catch (e: any) {
      this.message = e.message || 'Bulk import failed';
      this.isError = true;
    } finally {
      this.importing = false;
    }
  }

  render() {
    const busy = this.loading || this.importing;
    return html`
      <div class=${busy ? 'loading' : ''}>
        <div class="header">
          <h3>Import Entities</h3>
          <div style="display:flex;gap:8px">
            <ptcs-button label="Refresh Files" @click=${this.loadFiles} ?disabled=${busy}></ptcs-button>
          </div>
        </div>

        ${!this.gitThing ? html`<div class="empty-state">Set a GitThing to browse and import entities</div>` : ''}

        <div class="card">
          <div class="section-title">Single Import</div>
          <div class="file-list">
            ${this.files.map(f => html`
              <div class="file-row">
                <span class="file-icon">📄</span>
                <span class="file-name">${f.name}</span>
                <ptcs-button label="Import" @click=${() => this.importFile(f.name)} ?disabled=${busy}></ptcs-button>
              </div>
            `)}
            ${this.files.length === 0 ? html`<div class="empty-state">No .xml files found in repository path</div>` : ''}
          </div>
        </div>

        <div class="card">
          <form @submit=${(e: SubmitEvent) => { e.preventDefault(); this.bulkImport(); }}>
          <div class="section-title">Bulk Import</div>
          <div class="bulk-row">
            <input placeholder="Entity path relative to repo (e.g. Entities)" .value=${this.bulkPath} @input=${(e: InputEvent) => this.bulkPath = (e.target as HTMLInputElement).value} />
            <ptcs-button label="Import All" @click=${this.bulkImport} ?disabled=${busy || !this.bulkPath || !this.gitThing}></ptcs-button>
          </div>
          ${this.importResults.length > 0 ? html`
            <div class="file-list" style="margin-top:12px">
              ${this.importResults.map(r => html`
                <div class="file-row">
                  <span class="file-name">${r.EntityName}</span>
                  <span style="font-size:12px;color:${r.Status === 'Success' ? '#2e7d32' : '#c62828'}">${r.Status}</span>
                </div>
              `)}
            </div>
          ` : ''}
        </div>
        </form>

        ${this.message ? html`<div class="result ${this.isError ? 'error' : this.isInfo ? 'info' : 'success'}">${this.message}</div>` : ''}
      </div>
    `;
  }
}

customElements.define('git-import', GitImport);
