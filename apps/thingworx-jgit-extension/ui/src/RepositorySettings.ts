@TWWidgetDefinition
class RepositorySettings extends TypescriptWidget {
  @property(bindingTarget) repoName!: STRING;
  @property(bindingSource) loadedUsername: STRING = "";
  @property(bindingSource) loadedPassword: STRING = "";
  @property(bindingSource) loadedFullName: STRING = "";
  @property(bindingSource) loadedEmail: STRING = "";
  @property(bindingSource) loadedFingerprint: STRING = "";
  @property(bindingSource) keyOptions!: INFOTABLE<{GpgKeyLabel: STRING, GpgKeyFingerprint: STRING}>;
  @property(bindingSource) message: STRING = "";
  @property(bindingSource) saveDisabled: BOOLEAN = true;
  @property(bindingSource) trackedProjects!: INFOTABLE<{ProjectName: STRING}>;
  @property(bindingSource) availableProjects!: INFOTABLE<{ProjectName: STRING}>;
  @property(bindingTarget) selectedProjects!: INFOTABLE<{ProjectName: STRING}>;
  @property(bindingSource) projectMessage: STRING = "";
  @property(bindingSource) projectsBusy: BOOLEAN = true;
  private requestId = 0;
  private projectRequestId = 0;

  beforeDestroy() { this.requestId++; this.projectRequestId++; }

  private invoke(serviceName: string, payload: object): Promise<any> {
    return this.invokeOn("GIT.Utility.Thing", serviceName, payload);
  }

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

  @service async load() {
    if (!this.repoName) return;
    this.loadProjects();
    const repository = this.repoName;
    const request = ++this.requestId;
    this.saveDisabled = true;
    this.message = "Loading repository settings…";
    try {
      const [credentials, keys] = await Promise.all([
        this.invoke("GitCredentialGet", {GitThing: repository}),
        this.invoke("GpgKeyListSummary", {})
      ]);
      if (request !== this.requestId) return;
      if (keys.Error) throw new Error(keys.Message || "Unable to load signing keys");
      if (credentials.Error && !(credentials.Message || "").includes("Credentials not found for repository:")) {
        throw new Error(credentials.Message || "Unable to load credentials");
      }
      const row = !credentials.Error && credentials.Result && credentials.Result.rows && credentials.Result.rows[0];
      this.loadedUsername = row && row.GitCommitterUser || "";
      this.loadedFullName = row && row.GitCommitterFullName || "";
      this.loadedEmail = row && row.GitCommitterEmail || "";
      const options = keys.Result;
      const selected = row && row.GpgKeyFingerprint || "";
      const keyMissing = !!selected && !options.rows.some((key: any) => key.GpgKeyFingerprint === selected);
      options.rows = [{GpgKeyLabel: "No signing", GpgKeyFingerprint: ""}, ...options.rows];
      if (keyMissing) options.rows.push({GpgKeyLabel: "Unavailable signing key: " + selected, GpgKeyFingerprint: selected});
      this.keyOptions = options;
      this.loadedFingerprint = selected;
      this.saveDisabled = false;
      this.message = keyMissing ? "The saved signing key is unavailable. Choose another key or No signing." :
        credentials.Error ? "No credentials saved yet. Add credentials for this repository." : "";
    } catch (error) {
      if (request === this.requestId) this.message = "Could not load settings: " + (error as Error).message;
    }
  }

  @service async save() {
    if (!this.repoName || this.saveDisabled) return;
    this.saveDisabled = true;
    this.message = "Saving…";
    try {
      const result = await this.invoke("GitRepositorySettingsSave", {
        GitThing: this.repoName,
        GitCommitterUser: this.getWidget("settings-username").getProperty("Text") || "",
        GitCommitterPassword: this.getWidget("settings-password").getProperty("Text") || "",
        GitCommitterEmail: this.getWidget("settings-email").getProperty("Text") || "",
        GitCommitterFullName: this.getWidget("settings-name").getProperty("Text") || "",
        GpgKeyFingerprint: this.getWidget("settings-key").getProperty("SelectedText") || ""
      });
      if (result.Error) throw new Error(result.Message || "Unable to save settings");
      this.loadedPassword = "";
      this.message = "Repository settings saved.";
    } catch (error) { this.message = "Could not save settings: " + (error as Error).message; }
    finally { this.saveDisabled = false; }
  }

  private projectRows(row: any): any {
    if (row.Error) throw new Error(row.Message || "Request failed");
    return row.Result || {dataShape: {fieldDefinitions: {ProjectName: {name: "ProjectName", baseType: "STRING"}}}, rows: []};
  }

  @service async loadProjects() {
    if (!this.repoName) return;
    const repository = this.repoName;
    const request = ++this.projectRequestId;
    this.projectsBusy = true;
    try {
      const [tracked, available] = await Promise.all([
        this.invokeOn(repository, "GetTrackedProjects", {}),
        this.invokeOn(repository, "GetAvailableProjects", {})
      ]);
      if (request !== this.projectRequestId) return;
      this.trackedProjects = this.projectRows(tracked);
      this.availableProjects = this.projectRows(available);
      if (!this.trackedProjects.rows.length && !this.projectMessage) {
        this.projectMessage = "No projects are tracked. Add a project to synchronize its entities with this repository.";
      }
    } catch (error) {
      if (request === this.projectRequestId) this.projectMessage = "Could not load projects: " + (error as Error).message;
    } finally {
      if (request === this.projectRequestId) this.projectsBusy = false;
    }
  }

  @service async addProject() {
    const project = this.getWidget("settings-project-select").getProperty("SelectedText") || "";
    if (!this.repoName || this.projectsBusy) return;
    if (!project) { this.projectMessage = "Choose a project to add."; return; }
    this.projectsBusy = true;
    this.projectMessage = "Adding " + project + "…";
    try {
      const result = await this.invokeOn(this.repoName, "AddTrackedProject", {ProjectName: project});
      if (result.Error) throw new Error(result.Message || "Unable to add project");
      this.projectMessage = result.Message || "Project added.";
    } catch (error) { this.projectMessage = "Could not add project: " + (error as Error).message; }
    await this.loadProjects();
  }

  @service removeProjects() { return this.removeSelected(false); }

  @service removeProjectsAndFiles() { return this.removeSelected(true); }

  private async removeSelected(deleteFiles: boolean) {
    const selected = this.selectedProjects && this.selectedProjects.rows || [];
    if (!this.repoName || this.projectsBusy || !selected.length) return;
    this.projectsBusy = true;
    this.projectMessage = "Removing…";
    const messages: string[] = [];
    for (const row of selected) {
      try {
        const result = await this.invokeOn(this.repoName, "RemoveTrackedProject",
          {ProjectName: row.ProjectName, DeleteFiles: deleteFiles});
        messages.push(result.Error ? "Could not remove " + row.ProjectName + ": " + (result.Message || "request failed") : result.Message);
      } catch (error) { messages.push("Could not remove " + row.ProjectName + ": " + (error as Error).message); }
    }
    this.projectMessage = messages.join(" ");
    await this.loadProjects();
  }

}
