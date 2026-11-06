@TWWidgetDefinition
class HomeNavigation extends TypescriptWidget {
  @property(bindingTarget, didBind("openRepository")) selectedRepository!: STRING;

  @service openUserSettings() {
    window.location.assign("/Thingworx/Runtime/index.html#mashup=GIT.Mashup.Settings");
    window.location.reload();
  }

  private openRepository() {
    if (!this.selectedRepository) return;
    const target = "/Thingworx/Runtime/index.html#mashup=GIT.Mashup.Operations&repoName=" +
      encodeURIComponent(this.selectedRepository);
    window.location.assign(target);
    window.location.reload();
  }
}
