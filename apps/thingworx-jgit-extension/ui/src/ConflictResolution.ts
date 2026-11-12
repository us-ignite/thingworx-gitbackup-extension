@TWWidgetDefinition
class ConflictResolution extends TypescriptWidget {
  @property(bindingTarget, didBind("repositoryChanged")) repoName!: STRING;
  @twevent resolutionComplete!: TWEvent;
  private root?: HTMLElement;
  private generation = 0;
  private busy = false;
  private state: any;
  private file: any;
  afterRender() {
    this.root = this.jqElement[0] as HTMLElement;
    this.root.innerHTML = `<section class="git-conflict-resolution"><header class="conflict-heading"><ptcs-label class="page-title" label="Conflict resolution" variant="header"></ptcs-label><p role="status"></p></header><div class="conflict-file"><label>Conflicted file <select aria-label="Conflicted file"><option value="">Select a file</option></select></label><p class="file-info"></p></div><div class="versions"><label>Base<textarea data-version="base" readonly></textarea></label><label><span data-label="current">Current</span><textarea data-version="current" readonly></textarea></label><label><span data-label="incoming">Incoming</span><textarea data-version="incoming" readonly></textarea></label></div><label class="resolved">Resolved content<textarea data-version="working"></textarea></label><div class="file-actions action-group"><ptcs-button variant="primary" label="Use current" data-action="current"></ptcs-button><ptcs-button variant="primary" label="Use incoming" data-action="incoming"></ptcs-button><ptcs-button variant="primary" label="Save edited content" data-action="content"></ptcs-button><ptcs-button variant="danger" label="Delete file" data-action="delete"></ptcs-button></div><div class="operation-actions action-group"><ptcs-button variant="primary" label="Continue operation" data-action="continue"></ptcs-button><ptcs-button variant="secondary" label="Skip commit" data-action="skip"></ptcs-button><ptcs-button variant="danger" label="Abort operation" data-action="abort"></ptcs-button><ptcs-button variant="secondary" label="Refresh" data-action="refresh"></ptcs-button></div></section>`;
    this.root.querySelector('select')!.addEventListener('change', () => this.loadFile());
    for (const action of ['current','incoming','content','delete']) this.root.querySelector('[data-action="' + action + '"]')!.addEventListener('click', () => this.resolve(action));
    for (const action of ['continue','skip','abort','refresh']) this.root.querySelector('[data-action="' + action + '"]')!.addEventListener('click', () => this.operation(action));
    this.repositoryChanged();
  }
  beforeDestroy() { this.generation++; }
  private async invoke(service: string, body: object) {
    const response = await fetch('/Thingworx/Things/' + encodeURIComponent(this.repoName) + '/Services/' + service, {method:'POST',credentials:'same-origin',headers:{'Accept':'application/json','Content-Type':'application/json','X-XSRF-TOKEN':'TWX-XSRF-TOKEN-VALUE','X-Requested-By':'ThingWorx'},body:JSON.stringify(body)});
    if (!response.ok) throw new Error('Request failed (HTTP ' + response.status + ')');
    const row = (await response.json()).rows?.[0];
    if (!row || typeof row.Error !== 'boolean') throw new Error('Invalid service response');
    return row;
  }
  private message(value: string) { if (this.root) this.root.querySelector('[role="status"]')!.textContent = value; }
  private repositoryChanged() { if (this.root && this.repoName) this.refresh(); }
  private updateButtons() {
    if (!this.root) return;
    for (const action of ['current','incoming','content','delete']) {
      const button = this.root.querySelector('[data-action="' + action + '"]') as HTMLElement & { disabled: boolean };
      button.disabled = this.busy || !this.file || (action === 'content' && this.file.Binary) || (action === 'current' && !this.file.CurrentExists) || (action === 'incoming' && !this.file.IncomingExists);
    }
    for (const action of ['continue','skip','abort']) {
      const button = this.root.querySelector('[data-action="' + action + '"]') as HTMLElement & { disabled: boolean };
      button.disabled = this.busy || !this.state?.InProgress || (action === 'continue' && this.state.ConflictCount > 0) || (action === 'skip' && !this.state.CanSkip);
    }
  }
  private async refresh() {
    if (!this.root || !this.repoName) return;
    const request = ++this.generation;
    this.file = undefined;
    try {
      const result = await this.invoke('GetConflictState', {});
      if (request !== this.generation) return;
      if (result.Error) throw new Error(result.Message);
      this.state = result.Result?.rows?.[0];
      if (!this.state) throw new Error('Missing conflict state');
      this.root.querySelector('[data-label="current"]')!.textContent = this.state.CurrentLabel || 'Current';
      this.root.querySelector('[data-label="incoming"]')!.textContent = this.state.IncomingLabel || 'Incoming';
      const select = this.root.querySelector('select')!;
      select.replaceChildren(new Option('Select a file', ''));
      for (const path of (this.state.ConflictFiles || '').split('\n').filter(Boolean)) select.add(new Option(path, path));
      this.clearFile();
      this.message(this.state.InProgress ? (this.state.Description || this.state.Operation + ' is waiting for resolution.') + ' ' + this.state.ConflictCount + ' file(s) remaining.' : 'No operation is waiting for conflict resolution.');
      this.updateButtons();
    } catch (error) { if (request === this.generation) { this.state = undefined; this.message((error as Error).message); this.updateButtons(); } }
  }
  private clearFile() {
    if (!this.root) return;
    this.file = undefined;
    this.root.querySelector('.file-info')!.textContent = '';
    this.root.querySelectorAll('textarea').forEach(area => area.value = '');
  }
  private async loadFile() {
    if (!this.root) return;
    const path = this.root.querySelector('select')!.value;
    this.clearFile(); this.updateButtons();
    if (!path) return;
    const request = ++this.generation;
    try {
      const result = await this.invoke('GetConflictFile', {File:path});
      if (request !== this.generation || !this.root) return;
      if (result.Error) throw new Error(result.Message);
      this.file = result.Result?.rows?.[0];
      if (!this.file) throw new Error('Missing file data');
      this.root.querySelector('.file-info')!.textContent = this.file.Binary ? 'Binary file: choose a version or delete the file.' : 'Edit the working version or choose a version. A missing version is shown as empty.';
      for (const name of ['base','current','incoming','working']) {
        const field = name.charAt(0).toUpperCase() + name.slice(1);
        (this.root.querySelector('[data-version="' + name + '"]') as HTMLTextAreaElement).value = this.file[field] || '';
      }
      this.updateButtons();
    } catch (error) { if (request === this.generation) this.message((error as Error).message); }
  }
  private async resolve(resolution: string) {
    if (!this.root || !this.file || this.busy) return;
    const path = this.root.querySelector('select')!.value;
    if (resolution === 'delete' && !window.confirm('Resolve this conflict by deleting ' + path + '?')) return;
    this.busy = true; this.updateButtons();
    try {
      const result = await this.invoke('ResolveConflictFile', {File:path, Resolution:resolution, Content:(this.root.querySelector('[data-version="working"]') as HTMLTextAreaElement).value});
      if (result.Error) throw new Error(result.Message);
      await this.refresh();
    } catch (error) { this.message((error as Error).message); }
    finally { this.busy = false; this.updateButtons(); }
  }
  private async operation(action: string) {
    if (action === 'refresh') { if (!this.busy) await this.refresh(); return; }
    if (this.busy || !this.state?.InProgress) return;
    if (action === 'abort' && !window.confirm('Abort the operation and restore its original state?')) return;
    if (action === 'skip' && !window.confirm('Skip the current rebase commit?')) return;
    const service = action === 'continue' ? 'ConflictContinue' : action === 'skip' ? 'ConflictSkip' : 'ConflictAbort';
    let completed = false;
    this.busy = true; this.updateButtons();
    try {
      const result = await this.invoke(service, action === 'continue' ? {Message:''} : {});
      if (result.Error && !(result.Message || '').includes('[CONFLICT]')) throw new Error(result.Message);
      await this.refresh();
      if (this.state && !this.state.InProgress) { this.message(result.Message || 'Operation completed.'); completed = true; }
    } catch (error) { this.message((error as Error).message); }
    finally { this.busy = false; this.updateButtons(); }
    if (completed) this.resolutionComplete();
  }
}
