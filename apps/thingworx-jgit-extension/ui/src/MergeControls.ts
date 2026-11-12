@TWWidgetDefinition
class MergeControls extends TypescriptWidget {
  @property(bindingTarget, didBind("repositoryChanged")) repoName!: STRING;
  @twevent resolutionRequested!: TWEvent;
  private root?: HTMLElement;
  private generation = 0;
  private busy = false;
  private stateTimer?: number;
  private stateRequest = 0;

  afterRender() {
    this.root = this.jqElement[0] as HTMLElement;
    this.root.innerHTML = '<div class="git-merge-controls"><h2>Merge and rebase</h2><label>Branch <select aria-label="Branch"><option value="">Select a branch</option></select></label> <button type="button" data-action="merge">Merge Branch</button> <button type="button" data-action="rebase">Rebase onto Branch</button> <button type="button" data-action="resolve" hidden>Resolve Current Conflict</button><p role="status"></p></div>';
    this.root.querySelector('[data-action="merge"]')!.addEventListener('click', () => this.run('Merge'));
    this.root.querySelector('[data-action="rebase"]')!.addEventListener('click', () => this.run('Rebase'));
    this.root.querySelector('[data-action="resolve"]')!.addEventListener('click', () => this.openResolution());
    this.repositoryChanged();
    this.stateTimer = window.setInterval(() => { if (!document.hidden && !this.busy) this.refreshConflictState(); }, 10000);
  }
  beforeDestroy() { this.generation++; this.stateRequest++; if (this.stateTimer !== undefined) window.clearInterval(this.stateTimer); }
  private async invoke(service: string, body: object) {
    const response = await fetch('/Thingworx/Things/' + encodeURIComponent(this.repoName) + '/Services/' + service, {method:'POST', credentials:'same-origin', headers:{'Accept':'application/json','Content-Type':'application/json','X-XSRF-TOKEN':'TWX-XSRF-TOKEN-VALUE','X-Requested-By':'ThingWorx'}, body:JSON.stringify(body)});
    if (!response.ok) throw new Error('Request failed (HTTP ' + response.status + ')');
    const row = (await response.json()).rows?.[0];
    if (!row || typeof row.Error !== 'boolean') throw new Error('Invalid service response');
    return row;
  }
  private message(value: string) { if (this.root) this.root.querySelector('p')!.textContent = value; }
  private async repositoryChanged() {
    if (!this.root) return;
    const request = ++this.generation;
    this.stateRequest++;
    this.setConflictVisible(false);
    if (!this.repoName) return;
    this.refreshConflictState();
    const select = this.root.querySelector('select')!;
    select.replaceChildren(new Option('Select a branch', ''));
    try {
      const result = await this.invoke('GetBranchList', {});
      if (request !== this.generation) return;
      if (result.Error) throw new Error(result.Message);
      for (const branch of result.Result?.rows || []) {
        if (!branch.IsHead && branch.BranchName && !branch.BranchName.endsWith('/HEAD')) select.add(new Option(branch.ShortBranchName + ' (' + branch.BranchType.toLowerCase() + ')', branch.BranchName));
      }
    } catch (error) { if (request === this.generation) this.message((error as Error).message); }
  }
  private setConflictVisible(visible: boolean) {
    if (this.root) (this.root.querySelector('[data-action="resolve"]') as HTMLButtonElement).hidden = !visible;
  }
  private async refreshConflictState() {
    if (!this.root || !this.repoName) return;
    const request = ++this.stateRequest;
    const repository = this.repoName;
    try {
      const result = await this.invoke('GetConflictState', {});
      if (request !== this.stateRequest || repository !== this.repoName) return;
      if (result.Error) throw new Error(result.Message || 'Could not check conflict state');
      this.setConflictVisible(!!result.Result?.rows?.[0]?.InProgress);
    } catch (error) {
      if (request === this.stateRequest && repository === this.repoName) this.message((error as Error).message);
    }
  }
  private openResolution() {
    if (!this.repoName) return;
    this.resolutionRequested();
  }
  private async run(service: 'Merge' | 'Rebase') {
    if (!this.root || !this.repoName || this.busy) return;
    const branch = this.root.querySelector('select')!.value;
    if (!branch) { this.message('Select a branch first.'); return; }
    this.busy = true;
    this.root.querySelectorAll('button').forEach(button => button.disabled = true);
    this.message(service + ' in progress…');
    try {
      const result = await this.invoke(service, service === 'Merge' ? {BranchName: branch} : {UpstreamBranch: branch});
      this.message(result.Message || (result.Error ? service + ' failed.' : service + ' completed.'));
      if ((result.Message || '').includes('[CONFLICT]')) this.setConflictVisible(true);
      if (!result.Error) this.repositoryChanged();
      else await this.refreshConflictState();
    } catch (error) { this.message((error as Error).message); }
    finally { this.busy = false; this.root.querySelectorAll('button').forEach(button => button.disabled = false); }
  }
}
