// Runtime helper for GIT.Mashup.GitHeader. It hides the repository crumb and
// current branch on pages without a repository, and loads the branch only
// when a repository is known, so the header can be used on every Git page.
@TWWidgetDefinition
class GitHeader extends TypescriptWidget {
  @property(bindingTarget, didBind("repositoryUpdated")) repoName!: STRING;
  @property(bindingSource) hasRepository: BOOLEAN = false;
  @twevent repositoryChanged!: TWEvent;
  private loadedRepository = "";
  private timer?: number;

  afterRender() {
    this.schedule();
  }

  beforeDestroy() {
    if (this.timer !== undefined) window.clearTimeout(this.timer);
  }

  private repositoryUpdated() {
    this.schedule();
  }

  // Defer so the header's other repoName bindings, including the repository
  // entity used by GetCurrentBranch, are applied before the event fires.
  private schedule() {
    if (this.timer !== undefined) window.clearTimeout(this.timer);
    this.timer = window.setTimeout(() => {
      this.timer = undefined;
      const repository = this.repoName || "";
      this.hasRepository = repository !== "";
      if (repository && repository !== this.loadedRepository) {
        this.loadedRepository = repository;
        this.repositoryChanged();
      }
    }, 0);
  }

  // Reloads the current branch, for the header's RefreshRequested event.
  @service refresh() {
    if (this.repoName) this.repositoryChanged();
  }
}
