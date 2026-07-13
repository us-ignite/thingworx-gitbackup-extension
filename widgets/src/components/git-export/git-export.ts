import { LitElement, html, css } from 'lit';
import { property, state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { InfotableResponse } from '../../lib/twx-types.js';

interface EntityItem {
  name: string;
  type: string;
  projectName: string;
}

export class GitExport extends LitElement {
  static styles = css`
    :host { display: block; padding: 16px; font-family: sans-serif; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .header h3 { margin: 0; }
    .form-grid { display: grid; grid-template-columns: 140px 1fr auto; gap: 10px 12px; align-items: center; margin-bottom: 12px; }
    .form-grid label { font-size: 13px; color: #555; text-align: right; }
    .form-grid input, .form-grid select { padding: 7px 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 13px; width: 100%; box-sizing: border-box; }
    .entity-list { border: 1px solid #e0e0e0; border-radius: 4px; overflow: hidden; margin-bottom: 16px; }
    .entity-row { display: flex; align-items: center; padding: 8px 12px; border-bottom: 1px solid #f0f0f0; gap: 8px; }
    .entity-row:last-child { border-bottom: none; }
    .entity-row:hover { background: #f5f5f5; }
    .entity-row input[type="checkbox"] { margin: 0; }
    .entity-name { flex: 1; font-weight: 500; }
    .entity-type { font-size: 12px; padding: 2px 8px; border-radius: 4px; background: #eceff1; color: #546e7a; }
    .entity-project { font-size: 12px; color: #999; }
    .actions { margin-top: 12px; display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .result { margin-top: 12px; padding: 12px; border-radius: 4px; font-size: 13px; }
    .result.success { background: #e8f5e9; color: #2e7d32; }
    .result.error { background: #ffebee; color: #c62828; }
    .result.info { background: #e3f2fd; color: #1565c0; }
    .empty-state { text-align: center; padding: 24px; color: #999; }
    .loading { opacity: 0.6; pointer-events: none; }
    .card { border: 1px solid #e0e0e0; border-radius: 6px; padding: 16px; margin-bottom: 16px; }
    .section-title { font-size: 14px; font-weight: 600; color: #333; margin: 0 0 8px; }
    .entity-type-filter { display: flex; gap: 8px; align-items: center; }
  `;

  @property({ type: String }) gitThing = '';
  @state() private projectName = '';
  @state() private entityType = '';
  @state() private includeDependents = false;
  @state() private entities: EntityItem[] = [];
  @state() private selectedEntities: Set<string> = new Set();
  @state() private loading = false;
  @state() private exporting = false;
  @state() private message = '';
  @state() private isError = false;
  @state() private isInfo = false;
  @state() private commitMessage = '';

  async connectedCallback() {
    super.connectedCallback();
  }

  async searchEntities() {
    if (!this.projectName) return;
    this.loading = true;
    this.message = '';
    this.entities = [];
    try {
      const params: Record<string, unknown> = {
        project: this.projectName,
        entityName: '',
        includeDependents: this.includeDependents,
      };
      if (this.entityType) {
        params.entityType = this.entityType;
      }
      const res = await twx.invokeService<InfotableResponse<EntityItem>>('GIT.Utility.Thing', 'GetProjectEntities', params);
      this.entities = res.rows || [];
      this.selectedEntities = new Set();
    } catch (e: any) {
      this.message = e.message || 'Search failed';
      this.isError = true;
    } finally {
      this.loading = false;
    }
  }

  toggleEntity(name: string) {
    if (this.selectedEntities.has(name)) this.selectedEntities.delete(name);
    else this.selectedEntities.add(name);
  }

  selectAll() {
    if (this.selectedEntities.size === this.entities.length) {
      this.selectedEntities = new Set();
    } else {
      this.selectedEntities = new Set(this.entities.map(e => e.name));
    }
  }

  async doExport() {
    if (this.selectedEntities.size === 0 || !this.gitThing) return;
    this.exporting = true;
    this.message = '';
    this.isError = false;
    this.isInfo = false;
    try {
      const selected = this.entities.filter(e => this.selectedEntities.has(e.name));
      const entitiesInfoTable = {
        dataShape: {
          fieldDefinitions: {
            name: { name: "name", baseType: "STRING" },
            type: { name: "type", baseType: "STRING" },
          },
        },
        rows: selected.map(e => ({ name: e.name, type: e.type })),
      };
      await twx.invokeServiceWithInit(this.gitThing, 'ExportProjectEntities', {
        ProjectName: this.projectName,
        includeDependents: this.includeDependents,
        EntitiesToExport: entitiesInfoTable,
      });
      this.message = `Exported ${selected.length} entities from ${this.projectName}. Review and commit from the Commit & Push tab.`;
      this.isInfo = true;
    } catch (e: any) {
      this.message = e.message || 'Export failed';
      this.isError = true;
    } finally {
      this.exporting = false;
    }
  }

  get entityTypes() {
    return [...new Set(this.entities.map(e => e.type).filter(Boolean))].sort();
  }

  render() {
    const busy = this.loading || this.exporting;
    return html`
      <div class=${busy ? 'loading' : ''}>
        <div class="header">
          <h3>Export Entities</h3>
        </div>

        ${!this.gitThing ? html`<div class="empty-state">Set a GitThing to export entities</div>` : ''}

        <form @submit=${(e: SubmitEvent) => { e.preventDefault(); this.searchEntities(); }}>
        <div class="card">
          <div class="section-title">Search Entities</div>
          <div class="form-grid">
            <label>Project</label>
            <input .value=${this.projectName} @input=${(e: InputEvent) => this.projectName = (e.target as HTMLInputElement).value} placeholder="ProjectName" />
            <ptcs-button label="Search" @click=${this.searchEntities} ?disabled=${busy || !this.projectName}></ptcs-button>

            <label>Entity Type</label>
            <select .value=${this.entityType} @change=${(e: Event) => this.entityType = (e.target as HTMLSelectElement).value}>
              <option value="">All Types</option>
              <option value="Things">Things</option>
              <option value="Mashups">Mashups</option>
              <option value="Shapes">Shapes</option>
              <option value="Resources">Resources</option>
              <option value="DataTables">DataTables</option>
            </select>
            <div></div>

            <label></label>
            <div class="entity-type-filter">
              <input type="checkbox" .checked=${this.includeDependents} @change=${(e: Event) => this.includeDependents = (e.target as HTMLInputElement).checked} />
              <label style="text-align:left;font-size:13px;color:#555">Include dependents</label>
            </div>
            <div></div>
          </div>
        </div>
        </form>

        ${this.entities.length > 0 ? html`
          <div class="card">
            <div class="section-title">
              ${this.entities.length} entities found
              ${this.entityTypes.length > 1 ? html`
                (types: ${this.entityTypes.join(', ')})
              ` : ''}
            </div>
            <div class="entity-list">
              <div class="entity-row" style="background:#fafafa;font-weight:600">
                <input type="checkbox" @change=${this.selectAll} .checked=${this.selectedEntities.size === this.entities.length}>
                <span class="entity-name">Entity Name</span>
                <span class="entity-type">Type</span>
                <span class="entity-project">Project</span>
              </div>
              ${this.entities.map(e => html`
                <div class="entity-row">
                  <input type="checkbox" .checked=${this.selectedEntities.has(e.name)} @change=${() => this.toggleEntity(e.name)}>
                  <span class="entity-name">${e.name}</span>
                  <span class="entity-type">${e.type || '—'}</span>
                  <span class="entity-project">${e.projectName || '—'}</span>
                </div>
              `)}
            </div>

            <form @submit=${(e: SubmitEvent) => { e.preventDefault(); this.doExport(); }}>
            <div class="section-title">Commit</div>
            <div class="form-grid">
              <label>Message</label>
              <input .value=${this.commitMessage} @input=${(e: InputEvent) => this.commitMessage = (e.target as HTMLInputElement).value} placeholder="Export entities from ${this.projectName}" />
              <ptcs-button label="Export Selected (${this.selectedEntities.size})" @click=${this.doExport} ?disabled=${busy || this.selectedEntities.size === 0}></ptcs-button>
            </div>
            </form>
          </div>
        ` : ''}

        ${!this.gitThing ? '' : html`
          ${this.message ? html`<div class="result ${this.isError ? 'error' : this.isInfo ? 'info' : 'success'}">${this.message}</div>` : ''}
        `}
      </div>
    `;
  }
}

customElements.define('git-export', GitExport);
