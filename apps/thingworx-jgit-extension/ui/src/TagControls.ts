@TWWidgetDefinition
class TagControls extends TypescriptWidget {
  @property(bindingTarget, didBind("repositoryChanged")) repoName!: STRING;
  /** Commit to tag; blank tags HEAD. History binds the selected commit. */
  @property(bindingTarget, didBind("commitChanged")) commitId!: STRING;
  /** Fires after tags were created, pushed or deleted, to refresh tag labels elsewhere. */
  @twevent tagsChanged!: TWEvent;
  private root?: HTMLElement;
  private generation = 0;
  private busy = false;
  private tags: any[] = [];
  /**
   * Whether to list the tags with Push and Delete. A class host has no design-time property values, so the
   * widget id decides: history-tagcontrols-helper lists them, changes-tagcontrols-helper only creates and pushes.
   */
  private showList = false;

  afterRender() {
    this.root = this.jqElement[0] as HTMLElement;
    this.showList = /history-tagcontrols-helper$/.test(this.root.id || '');
    this.root.innerHTML = '<div class="git-tag-controls"><h2>Tags</h2>'
      + '<div class="git-tag-row"><label>Tag name <input name="tag" type="text" placeholder="v1.2.0" aria-label="Tag name"></label>'
      + '<label>Message <input name="message" type="text" placeholder="Optional; a signed tag uses the tag name" aria-label="Tag message"></label>'
      + '<label class="git-tag-check"><input name="force" type="checkbox"> Move if it exists</label>'
      + '<button type="button" data-action="create">Create tag</button></div>'
      + '<div class="git-tag-row"><label>Remote <select name="remote" aria-label="Remote"></select></label>'
      + '<label class="git-tag-check"><input name="push" type="checkbox" checked> Push new tags</label>'
      + '<button type="button" data-action="push-all">Push all tags</button></div>'
      + '<div class="git-tag-list" hidden><table><thead><tr><th><input type="checkbox" data-action="select-all" aria-label="Select all tags"></th>'
      + '<th>Tag</th><th>Commit</th><th>Signed</th><th>Date</th><th>Message</th></tr></thead><tbody></tbody></table>'
      + '<div class="git-tag-row"><button type="button" data-action="push-selected">Push selected</button>'
      + '<button type="button" data-action="delete-local">Delete selected here</button>'
      + '<button type="button" data-action="delete-remote">Delete selected here and on remote</button></div></div>'
      + '<p role="status"></p></div>';
    const on = (action: string, handler: () => void) =>
      this.root!.querySelector('[data-action="' + action + '"]')!.addEventListener('click', handler);
    on('create', () => this.create());
    on('push-all', () => this.push(['']));
    on('push-selected', () => this.push(this.selected()));
    on('delete-local', () => this.remove(false));
    on('delete-remote', () => this.remove(true));
    on('select-all', () => {
      const all = (this.root!.querySelector('[data-action="select-all"]') as HTMLInputElement).checked;
      this.root!.querySelectorAll<HTMLInputElement>('tbody input[type="checkbox"]').forEach(box => box.checked = all);
    });
    this.repositoryChanged();
  }

  beforeDestroy() { this.generation++; }

  private async invoke(service: string, body: object) {
    const response = await fetch('/Thingworx/Things/' + encodeURIComponent(this.repoName) + '/Services/' + service, {method: 'POST', credentials: 'same-origin',
      headers: {'Accept': 'application/json', 'Content-Type': 'application/json', 'X-XSRF-TOKEN': 'TWX-XSRF-TOKEN-VALUE', 'X-Requested-By': 'ThingWorx'},
      body: JSON.stringify(body)});
    if (!response.ok) throw new Error('Request failed (HTTP ' + response.status + ')');
    const row = (await response.json()).rows?.[0];
    if (!row || typeof row.Error !== 'boolean') throw new Error('Invalid service response');
    return row;
  }

  private field(name: string): HTMLInputElement { return this.root!.querySelector('[name="' + name + '"]') as HTMLInputElement; }
  private message(value: string) { if (this.root) this.root.querySelector('p[role="status"]')!.textContent = value; }
  private remote(): string { return (this.root!.querySelector('select[name="remote"]') as HTMLSelectElement).value; }

  private setBusy(busy: boolean) {
    this.busy = busy;
    this.root?.querySelectorAll<HTMLButtonElement>('button').forEach(button => button.disabled = busy);
  }

  /** Names the commit the Create tag button tags. */
  commitChanged() {
    if (!this.root) return;
    const id = String(this.commitId || '');
    this.root.querySelector('[data-action="create"]')!.textContent = id ? 'Create tag on ' + id.slice(0, 8) : 'Create tag on HEAD';
  }

  private async repositoryChanged() {
    if (!this.root) return;
    const request = ++this.generation;
    (this.root.querySelector('.git-tag-list') as HTMLElement).hidden = !this.showList;
    this.commitChanged();
    if (!this.repoName) return;
    const select = this.root.querySelector('select[name="remote"]') as HTMLSelectElement;
    try {
      const remotes = await this.invoke('GetRemotes', {});
      if (request !== this.generation) return;
      if (remotes.Error) throw new Error(remotes.Message);
      select.replaceChildren(...(remotes.Result?.rows || []).map((row: any) => new Option(row.Name, row.Name)));
      if (!select.options.length) select.add(new Option('No remotes', ''));
      const origin = Array.from(select.options).find(option => option.value === 'origin');
      if (origin) origin.selected = true;
    } catch (error) { if (request === this.generation) this.message('Could not load remotes: ' + (error as Error).message); }
    if (this.showList) await this.loadTags();
  }

  private async loadTags() {
    const request = this.generation;
    try {
      const result = await this.invoke('GetTagList', {});
      if (request !== this.generation) return;
      if (result.Error) throw new Error(result.Message);
      this.tags = result.Result?.rows || [];
      this.renderTags();
    } catch (error) { if (request === this.generation) this.message('Could not load tags: ' + (error as Error).message); }
  }

  private renderTags() {
    const body = this.root!.querySelector('tbody')!;
    body.replaceChildren();
    for (const tag of this.tags) {
      const row = document.createElement('tr');
      const tick = document.createElement('input');
      tick.type = 'checkbox';
      tick.value = tag.TagName;
      tick.setAttribute('aria-label', 'Select tag ' + tag.TagName);
      const cell = (text: string, title?: string) => { const td = document.createElement('td'); td.textContent = text; if (title) td.title = title; return td; };
      const first = document.createElement('td');
      first.append(tick);
      const date = tag.Date && new Date(tag.Date).getTime() > 0 ? new Date(tag.Date).toLocaleString() : '';
      row.append(first, cell(tag.TagName), cell(String(tag.CommitID || '').slice(0, 8), tag.CommitID),
        cell(tag.Signed ? 'Signed' : tag.Annotated ? 'Annotated' : 'Lightweight'), cell(date), cell(tag.Message || '', tag.Tagger ? 'Tagger: ' + tag.Tagger : ''));
      body.append(row);
    }
    if (!this.tags.length) {
      const row = document.createElement('tr');
      const td = document.createElement('td');
      td.colSpan = 6;
      td.textContent = 'No tags yet.';
      row.append(td);
      body.append(row);
    }
    (this.root!.querySelector('[data-action="select-all"]') as HTMLInputElement).checked = false;
  }

  private selected(): string[] {
    return Array.from(this.root!.querySelectorAll<HTMLInputElement>('tbody input[type="checkbox"]:checked')).map(box => box.value);
  }

  private async create() {
    if (!this.repoName || this.busy) return;
    const name = this.field('tag').value.trim();
    if (!name) { this.message('Enter a tag name.'); return; }
    this.setBusy(true);
    this.message('Creating ' + name + '…');
    const messages: string[] = [];
    try {
      const result = await this.invoke('CreateTag', {TagName: name, Message: this.field('message').value,
        CommitID: this.commitId || '', Force: this.field('force').checked});
      if (result.Error) throw new Error(result.Message || 'Could not create the tag');
      messages.push(result.Message);
      this.field('tag').value = '';
      this.field('message').value = '';
      if (this.field('push').checked && this.remote()) {
        const pushed = await this.invoke('PushTags', {Remote: this.remote(), TagName: name, Force: this.field('force').checked});
        messages.push(pushed.Error ? 'Push failed: ' + (pushed.Message || 'request failed') : pushed.Message);
      }
    } catch (error) { messages.push((error as Error).message); }
    this.finish(messages);
  }

  private async push(names: string[]) {
    if (!this.repoName || this.busy) return;
    if (!names.length) { this.message('Select the tags to push.'); return; }
    if (!this.remote()) { this.message('Add a remote in Repository Settings first.'); return; }
    this.setBusy(true);
    this.message('Pushing…');
    const messages: string[] = [];
    for (const name of names) {
      try {
        const result = await this.invoke('PushTags', {Remote: this.remote(), TagName: name, Force: false});
        messages.push(result.Error ? (name || 'Tags') + ': ' + (result.Message || 'request failed') : result.Message);
      } catch (error) { messages.push((error as Error).message); }
    }
    this.finish(messages);
  }

  private async remove(onRemote: boolean) {
    const names = this.selected();
    if (!this.repoName || this.busy) return;
    if (!names.length) { this.message('Select the tags to delete.'); return; }
    if (onRemote && !this.remote()) { this.message('Choose a remote.'); return; }
    const where = onRemote ? 'here and on ' + this.remote() : 'in this repository';
    if (!window.confirm('Delete ' + names.join(', ') + ' ' + where + '?')) return;
    this.setBusy(true);
    this.message('Deleting…');
    const messages: string[] = [];
    for (const name of names) {
      try {
        const result = await this.invoke('DeleteTag', {TagName: name, Remote: onRemote ? this.remote() : ''});
        messages.push(result.Error ? name + ': ' + (result.Message || 'request failed') : result.Message);
      } catch (error) { messages.push((error as Error).message); }
    }
    this.finish(messages);
  }

  private async finish(messages: string[]) {
    this.message(messages.join('\n'));
    this.setBusy(false);
    this.tagsChanged();
    if (this.showList) await this.loadTags();
  }
}
