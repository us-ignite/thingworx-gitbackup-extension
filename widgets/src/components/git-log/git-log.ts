import { html, css } from 'lit';
import { GitElementBase } from '../git-base.js';
import { themeVars, statusStyles } from '../../lib/git-styles.js';
import { state } from 'lit/decorators.js';
import { twx } from '../../lib/twx-service.js';
import type { InfotableResponse } from '../../lib/twx-types.js';

interface LogEntry {
  Content: string;
  ID: string;
  ServiceName: string;
  Source: string;
  timestamp: string;
  User: string;
}

export class GitLog extends GitElementBase {
  static styles = [themeVars, statusStyles, css`
    :host { display: block; padding: 16px; font-family: sans-serif; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .header h3 { margin: 0; }
    .filters { display: flex; gap: 8px; margin-bottom: 12px; align-items: center; flex-wrap: wrap; }
    .filters input, .filters select { padding: 6px 10px; border: 1px solid var(--git-color-border-strong, #ccc); border-radius: var(--git-border-radius-sm, 4px); font-size: 13px; }
    .filters input { flex: 1; min-width: 150px; }
    .log-table { width: 100%; border-collapse: collapse; border: 1px solid var(--git-color-border, #e0e0e0); border-radius: var(--git-border-radius-sm, 4px); overflow: hidden; }
    .log-table th { text-align: left; padding: 8px 12px; background: var(--git-color-bg-stripe, #fafafa); border-bottom: 2px solid var(--git-color-border, #e0e0e0); font-size: 12px; color: var(--git-color-text-secondary, #666); text-transform: uppercase; }
    .log-table td { padding: 8px 12px; border-bottom: 1px solid var(--git-color-border-light, #f0f0f0); font-size: 13px; max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .log-table tr:hover td { background: var(--git-color-bg-hover, #f5f5f5); }
    .log-content { max-width: 400px; }
    .empty { text-align: center; padding: 32px; color: var(--git-color-text-muted, #999); }
    .timestamp { white-space: nowrap; color: var(--git-color-text-secondary, #666); font-size: 12px; }
  `];

  @state() private logs: LogEntry[] = [];
  @state() private loading = false;
  @state() private error = '';
  @state() private searchTerm = '';
  @state() private filterService = '';

  async connectedCallback() {
    super.connectedCallback();
    this.load();
  }

  async load() {
    this.loading = true;
    this.error = '';
    try {
      const res = await twx.invokeService<InfotableResponse<LogEntry>>('GitBackup.Log.DataTable', 'QueryDataTableEntries', {
        query: "*",
      });
      this.logs = (res.rows || []).sort((a, b) => String(b.timestamp || '').localeCompare(String(a.timestamp || '')));
    } catch (e: any) {
      this.error = e.message || 'Failed to load logs';
    } finally {
      this.loading = false;
    }
  }

  get filteredLogs() {
    let result = this.logs;
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(l =>
        (l.Content || '').toLowerCase().includes(term) ||
        (l.ServiceName || '').toLowerCase().includes(term) ||
        (l.Source || '').toLowerCase().includes(term) ||
        (l.User || '').toLowerCase().includes(term)
      );
    }
    if (this.filterService) {
      result = result.filter(l => l.ServiceName === this.filterService);
    }
    return result;
  }

  get serviceNames() {
    return [...new Set(this.logs.map(l => l.ServiceName).filter(Boolean))];
  }

  formatTime(ts: string) {
    if (!ts) return '';
    try { return new Date(ts).toLocaleString(); } catch { return ts; }
  }

  render() {
    return html`
      <div class=${this.loading ? 'loading' : ''}>
        <div class="header">
          <h3>Activity Log</h3>
          <ptcs-button label=${this.loading ? 'Loading...' : 'Refresh'} @click=${this.load} ?disabled=${this.loading}></ptcs-button>
        </div>

        ${this.error ? html`<div class="error">${this.error}</div>` : ''}

        <form @submit=${(e: SubmitEvent) => { e.preventDefault(); this.load(); }}>
        <div class="filters">
          <input placeholder="Search logs..." .value=${this.searchTerm} @input=${(e: InputEvent) => this.searchTerm = (e.target as HTMLInputElement).value} />
          <select .value=${this.filterService} @change=${(e: Event) => this.filterService = (e.target as HTMLSelectElement).value}>
            <option value="">All Services</option>
            ${this.serviceNames.map(s => html`<option value=${s}>${s}</option>`)}
          </select>
          <span style="font-size:12px;color:#999">${this.filteredLogs.length} entries</span>
        </div>
        </form>

        ${this.filteredLogs.length > 0 ? html`
          <div style="overflow-x:auto">
            <table class="log-table">
              <thead><tr>
                <th>Time</th>
                <th>User</th>
                <th>Service</th>
                <th>Source</th>
                <th>Content</th>
              </tr></thead>
              <tbody>
                ${this.filteredLogs.map(l => html`
                  <tr>
                    <td class="timestamp">${this.formatTime(l.timestamp)}</td>
                    <td>${l.User || '—'}</td>
                    <td>${l.ServiceName || '—'}</td>
                    <td>${l.Source || '—'}</td>
                    <td class="log-content" title=${l.Content || ''}>${l.Content || ''}</td>
                  </tr>
                `)}
              </tbody>
            </table>
          </div>
        ` : html`<div class="empty">${this.loading ? 'Loading...' : 'No log entries found'}</div>`}
      </div>
    `;
  }
}

if (!customElements.get('git-log')) { customElements.define('git-log', GitLog); };
