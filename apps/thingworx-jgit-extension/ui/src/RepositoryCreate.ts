@TWWidgetDefinition
class RepositoryCreate extends TypescriptWidget {
  @property(bindingSource) keyOptions!: INFOTABLE<{GpgKeyLabel: STRING, GpgKeyFingerprint: STRING}>;
  @property(bindingSource) availableProjects!: INFOTABLE<{ProjectName: STRING}>;
  @property(bindingTarget) selectedProjects!: INFOTABLE<{ProjectName: STRING}>;
  @property(bindingSource) message: STRING = "";
  @property(bindingSource) projectMessage: STRING = "";
  @property(bindingSource) createDisabled: BOOLEAN = true;
  @property(bindingSource) createdRepoName: STRING = "";
  @event created!: TWEvent;
  private busy = false;
  private destroyed = false;

  beforeDestroy() { this.destroyed = true; }

  private async invokeOn(thingName: string, serviceName: string, payload: object): Promise<any> {
    const response = await fetch("/Thingworx/Things/" + encodeURIComponent(thingName) + "/Services/" + serviceName, {
      method: "POST", credentials: "same-origin",
      headers: {"Accept": "application/json", "Content-Type": "application/json",
        "X-XSRF-TOKEN": "TWX-XSRF-TOKEN-VALUE", "X-Requested-By": "ThingWorx"},
      body: JSON.stringify(payload)
    });
    if (!response.ok) throw new Error("Request failed (HTTP " + response.status + ")");
    const result = await response.json();
    const row = result && result.rows && result.rows[0];
    if (!row || typeof row.Error !== "boolean") throw new Error("Invalid service response");
    return row;
  }

  private invoke(serviceName: string, payload: object): Promise<any> {
    return this.invokeOn("GIT.Utility.Thing", serviceName, payload);
  }

  private text(id: string): string {
    const value = this.getWidget(id).getProperty("Text");
    return value == null ? "" : String(value).trim();
  }

  @service async load() {
    this.createDisabled = false;
    this.projectMessage = "Loading projects…";
    const [keys, projects] = await Promise.allSettled([
      this.invoke("GpgKeyListSummary", {}),
      this.invoke("GetTrackableProjects", {})
    ]);
    if (this.destroyed) return;
    if (keys.status === "fulfilled" && !keys.value.Error) {
      const options = keys.value.Result;
      options.rows = [{GpgKeyLabel: "No signing", GpgKeyFingerprint: ""}, ...options.rows];
      this.keyOptions = options;
    } else {
      this.message = "Signing keys could not be loaded; the repository can still be created without one.";
    }
    if (projects.status === "fulfilled" && !projects.value.Error) {
      this.availableProjects = projects.value.Result;
      this.projectMessage = projects.value.Result.rows.length ? "" :
        "No projects can be tracked yet. You can add projects later in Repository Settings.";
    } else {
      const reason = projects.status === "fulfilled" ? projects.value.Message : (projects.reason as Error).message;
      this.projectMessage = "Could not load projects: " + (reason || "request failed");
    }
  }

  /** Returns a message describing the first invalid field, or "" when the form can be submitted. */
  private validate(): string {
    const name = this.text("create-name");
    if (!name) return "Enter a repository name.";
    if (!/^[A-Za-z0-9_][A-Za-z0-9_.\-]*$/.test(name)) {
      return "The repository name may contain only letters, digits, '.', '_' and '-', and cannot start with '.' or '-'.";
    }
    if (this.getWidget("create-use-proxy").getProperty("State")) {
      if (!this.text("create-proxy-url")) return "Enter a proxy URL, or turn off Use proxy.";
      const port = this.text("create-proxy-port");
      if (!/^\d+$/.test(port) || Number(port) < 1 || Number(port) > 65535) return "Enter a proxy port from 1 to 65535.";
    }
    return "";
  }

  @service async create() {
    if (this.busy) return;
    const problem = this.validate();
    if (problem) { this.message = problem; return; }
    this.busy = true;
    this.createDisabled = true;
    const name = this.text("create-name");
    const useProxy = !!this.getWidget("create-use-proxy").getProperty("State");
    const selected = (this.selectedProjects && this.selectedProjects.rows || [])
      .map((row: any) => ({ProjectName: row.ProjectName}));
    this.message = "Creating " + name + "… This can take up to a minute while the repository Thing starts.";
    try {
      const result = await this.invoke("RepositoryCreate", {
        RepoName: name,
        GitRepoURL: this.text("create-url"),
        RepoPath: this.text("create-path"),
        BranchName: this.text("create-branch") || "main",
        TrackedProjects: {
          dataShape: {fieldDefinitions: {ProjectName: {name: "ProjectName", baseType: "STRING"}}},
          rows: selected
        },
        UseProxy: useProxy,
        ProxyURL: useProxy ? this.text("create-proxy-url") : "",
        ProxyPort: useProxy ? Number(this.text("create-proxy-port")) : 0,
        LocalizationTokensPrefix: this.text("create-l10n-prefix"),
        GitCommitterUser: this.text("create-username"),
        GitCommitterPassword: this.getWidget("create-password").getProperty("Text") || "",
        GitCommitterEmail: this.text("create-email"),
        GitCommitterFullName: this.text("create-fullname")
      });
      if (result.Error) throw new Error(result.Message || "Unable to create the repository");
      const fingerprint = this.getWidget("create-key").getProperty("SelectedText") || "";
      if (fingerprint) {
        const signing = await this.invokeOn(name, "SetGPGKeyForSigning", {GpgKeyFingerprint: fingerprint});
        if (signing.Error) {
          this.message = "Repository created, but the signing key was not set: " + (signing.Message || "request failed") +
            " Opening Repository Settings…";
        }
      }
      if (this.destroyed) return;
      if (!this.message.startsWith("Repository created, but")) this.message = "Repository created. Opening Repository Settings…";
      this.createdRepoName = name;
      this.created();
    } catch (error) {
      this.message = "Could not create repository: " + (error as Error).message;
      this.createDisabled = false;
    } finally {
      this.busy = false;
    }
  }

}
