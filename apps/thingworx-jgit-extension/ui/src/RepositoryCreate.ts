@TWWidgetDefinition
class RepositoryCreate extends TypescriptWidget {
  @property(bindingSource) keyOptions!: INFOTABLE<{GpgKeyLabel: STRING, GpgKeyFingerprint: STRING}>;
  @property(bindingSource) sshKeyOptions!: INFOTABLE<{SshKeyLabel: STRING, SshKeyFingerprint: STRING}>;
  @property(bindingSource) availableProjects!: INFOTABLE<{ProjectName: STRING}>;
  @property(bindingTarget) selectedProjects!: INFOTABLE<{ProjectName: STRING}>;
  @property(bindingSource) message: STRING = "";
  @property(bindingSource) projectMessage: STRING = "";
  @property(bindingSource) identityText: STRING = "";
  @property(bindingSource) createDisabled: BOOLEAN = true;
  @property(bindingSource) createdRepoName: STRING = "";
  /** Text of the dialog listing setup that is missing before the repository is created. */
  @property(bindingSource) missingPrompt: STRING = "";
  /** Text of the dialog asking to approve a new SSH remote's host key. */
  @property(bindingSource) hostKeyPrompt: STRING = "";
  /** Comma-separated project names for the grid to select after its list is reloaded. */
  @property(bindingSource) projectsToSelect: STRING = "";
  @property(bindingSource) projectBusy: BOOLEAN = false;
  @event created!: TWEvent;
  /** Fires after the project list was reloaded, so the grid reselects projectsToSelect. */
  @event projectsReloaded!: TWEvent;
  /** Fires when sign-in or identity is missing, to open the confirmation dialog. */
  @event setupIncomplete!: TWEvent;
  /** Fires after the new repository's SSH host key was read, to open the approval dialog. */
  @event hostKeyChecked!: TWEvent;
  private busy = false;
  private destroyed = false;
  private identityComplete = false;
  private sshKeyCount = 0;
  private checkedHostKey: {repository: string, fingerprint: string} | null = null;

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

  private selected(id: string): string {
    const value = this.getWidget(id).getProperty("SelectedText");
    return value == null ? "" : String(value);
  }

  @service async load() {
    this.createDisabled = false;
    this.projectMessage = "Loading projects…";
    const [keys, projects, sshKeys, identity] = await Promise.allSettled([
      this.invoke("GpgKeyListSummary", {}),
      this.invoke("GetTrackableProjects", {}),
      this.invoke("SshKeyListSummary", {}),
      this.invoke("GitIdentityGet", {})
    ]);
    if (this.destroyed) return;
    if (keys.status === "fulfilled" && !keys.value.Error) {
      const options = keys.value.Result;
      options.rows = [{GpgKeyLabel: "No signing", GpgKeyFingerprint: ""}, ...options.rows];
      this.keyOptions = options;
    } else {
      this.message = "Signing keys could not be loaded; the repository can still be created without one.";
    }
    if (sshKeys.status === "fulfilled" && !sshKeys.value.Error) {
      const options = sshKeys.value.Result;
      this.sshKeyCount = options.rows.length;
      options.rows = [{SshKeyLabel: "No SSH key", SshKeyFingerprint: ""}, ...options.rows];
      this.sshKeyOptions = options;
    }
    const who = identity.status === "fulfilled" && !identity.value.Error && identity.value.Result &&
      identity.value.Result.rows && identity.value.Result.rows[0] || {};
    this.identityComplete = !!(who.GitUserName && who.GitUserEmail);
    this.identityText = this.identityComplete ?
      "Your commits will be recorded as " + who.GitUserName + " <" + who.GitUserEmail + ">. Change this under Commit identity in Settings." :
      "Your commit name and email are not set yet. Set them under Commit identity in Settings before you commit.";
    if (projects.status === "fulfilled" && !projects.value.Error) {
      this.availableProjects = projects.value.Result;
      this.projectMessage = projects.value.Result.rows.length ? "" :
        "No projects can be tracked yet. You can add projects later in Repository Settings.";
    } else {
      const reason = projects.status === "fulfilled" ? projects.value.Message : (projects.reason as Error).message;
      this.projectMessage = "Could not load projects: " + (reason || "request failed");
    }
  }

  /**
   * Classifies a remote URL the way the server's Remotes.parseUrl does: "" when blank, otherwise
   * https, http or ssh (including git@host:path), or "invalid" for anything the server would refuse.
   */
  private urlKind(url: string): string {
    if (!url) return "";
    const scheme = /^([A-Za-z][A-Za-z0-9+.-]*):\/\//.exec(url);
    if (!scheme) {
      // scp-like ssh: [user@]host:path, where host has no slash before the colon.
      return /^([^@\/:\s]+@)?[^@\/:\s]+:[^\s]+$/.test(url) && !/^[A-Za-z]:[\\\/]/.test(url) ? "ssh" : "invalid";
    }
    const kind = scheme[1].toLowerCase();
    if (kind !== "https" && kind !== "http" && kind !== "ssh") return "invalid";
    const authority = url.slice(scheme[0].length).split("/")[0];
    const at = authority.lastIndexOf("@");
    if (at >= 0) {
      const user = authority.slice(0, at);
      if (user.indexOf(":") >= 0) return "invalid"; // embedded password
      if (kind !== "ssh") return "invalid"; // HTTP(S) URLs must not embed a user name
    }
    return kind;
  }

  /** Returns a message describing the first invalid field, or "" when the form can be submitted. */
  private validate(): string {
    const name = this.text("create-name");
    if (!name) return "Enter a repository name.";
    if (!/^[A-Za-z0-9_][A-Za-z0-9_.\-]*$/.test(name)) {
      return "The repository name may contain only letters, digits, '.', '_' and '-', and cannot start with '.' or '-'.";
    }
    if (this.urlKind(this.text("create-url")) === "invalid") {
      return "Enter an https://, http:// or ssh:// remote URL (git@host:path also works) without a password in it, " +
        "or leave it blank to add a remote later. Put your user name and password or token under Credentials, not in the URL.";
    }
    return "";
  }

  /** Lists setup the new repository will be missing, in sentences for the confirmation dialog. */
  private missingSetup(): string[] {
    const missing: string[] = [];
    const kind = this.urlKind(this.text("create-url"));
    const hasLogin = !!this.text("create-username") && !!(this.getWidget("create-password").getProperty("Text") || "");
    if (!this.identityComplete) {
      missing.push("Your commit name and email are not set, so you cannot commit yet. Set them under Commit identity in Settings.");
    }
    if (kind === "https" && !hasLogin) {
      missing.push("No Git username and password or token is entered. Pulling from a private repository and pushing will fail until you add them in Repository Settings.");
    } else if (kind === "http") {
      missing.push("The remote uses plain http://. Saved passwords are never sent over HTTP, so this remote can only be used without signing in. Use https:// or ssh:// to push.");
    } else if (kind === "ssh" && !this.selected("create-ssh-key")) {
      missing.push(this.sshKeyCount ?
        "No SSH key is selected for this ssh:// remote. Choose one under Credentials, or later in Repository Settings." :
        "You have no SSH keys for this ssh:// remote. Generate one under SSH keys in Settings and add its public key to your account on the Git server.");
    } else if (!kind) {
      missing.push("No remote URL is entered. The repository can only be used locally until you add a remote in Repository Settings.");
    }
    if (!(this.selectedProjects && this.selectedProjects.rows || []).length) {
      missing.push("No ThingWorx project is selected for tracking. Sync from ThingWorx and Sync to ThingWorx cover only tracked projects, " +
        "so Pull fails until one is tracked. Select or create a project under Tracked projects, or add one later in Repository Settings.");
    }
    return missing;
  }

  /** Creates a ThingWorx project, reloads the list, and selects the new project for tracking. */
  @service async createProject() {
    if (this.projectBusy) return;
    const name = this.text("create-new-project");
    if (!name) { this.projectMessage = "Enter a name for the new project."; return; }
    if (!/^[A-Za-z0-9_][A-Za-z0-9_.\-]*$/.test(name)) {
      this.projectMessage = "A project name may contain only letters, digits, '.', '_' and '-', and cannot start with '.' or '-'.";
      return;
    }
    this.projectBusy = true;
    this.projectMessage = "Creating project " + name + "…";
    const keep = (this.selectedProjects && this.selectedProjects.rows || []).map((row: any) => row.ProjectName);
    try {
      const result = await this.invoke("ProjectCreate", {ProjectName: name, Description: ""});
      if (result.Error) throw new Error(result.Message || "request failed");
      const projects = await this.invoke("GetTrackableProjects", {});
      if (projects.Error) throw new Error(projects.Message || "Could not reload projects");
      if (this.destroyed) return;
      this.availableProjects = projects.Result;
      this.projectsToSelect = [...keep, name].join(",");
      // Let the grid render the new rows before it selects them.
      await new Promise(resolve => setTimeout(resolve, 300));
      if (this.destroyed) return;
      this.projectsReloaded();
      this.getWidget("create-new-project").setProperty("Text", "");
      this.projectMessage = result.Message + " It is selected, so this repository will track it.";
    } catch (error) {
      this.projectMessage = "Could not create the project: " + (error as Error).message;
    } finally {
      this.projectBusy = false;
    }
  }

  /** Checks the form and the user's setup, asking for confirmation when setup is missing. */
  @service async create() {
    if (this.busy) return;
    const problem = this.validate();
    if (problem) { this.message = problem; return; }
    const missing = this.missingSetup();
    if (missing.length) {
      // The confirmation dialog does not keep line breaks, so the items are joined as sentences.
      this.missingPrompt = missing.join(" ") + " You can create the repository now and finish this later.";
      this.setupIncomplete();
      return;
    }
    await this.createRepository();
  }

  /** Creates the repository after the user accepted the missing setup. */
  @service async createAnyway() {
    if (this.busy) return;
    const problem = this.validate();
    if (problem) { this.message = problem; return; }
    await this.createRepository();
  }

  private async createRepository() {
    this.busy = true;
    this.createDisabled = true;
    const name = this.text("create-name");
    const url = this.text("create-url");
    const selected = (this.selectedProjects && this.selectedProjects.rows || [])
      .map((row: any) => ({ProjectName: row.ProjectName}));
    this.message = "Creating " + name + "… This can take up to a minute while the repository Thing starts.";
    try {
      const result = await this.invoke("RepositoryCreate", {
        RepoName: name,
        GitRepoURL: url,
        BranchName: this.text("create-branch") || "main",
        TrackedProjects: {
          dataShape: {fieldDefinitions: {ProjectName: {name: "ProjectName", baseType: "STRING"}}},
          rows: selected
        },
        UseProxy: false,
        ProxyURL: "",
        ProxyPort: 0,
        LocalizationTokensPrefix: this.text("create-l10n-prefix"),
        // Sign-in is saved for the origin remote below, not for the whole repository.
        GitCommitterUser: "",
        GitCommitterPassword: "",
        // The commit name and email are user-level settings, saved from the Settings page.
        GitCommitterEmail: "",
        GitCommitterFullName: ""
      });
      if (result.Error) throw new Error(result.Message || "Unable to create the repository");
    } catch (error) {
      this.message = "Could not create repository: " + (error as Error).message;
      this.createDisabled = false;
      this.busy = false;
      return;
    }
    const problems: string[] = [];
    const fingerprint = this.selected("create-key");
    if (fingerprint) {
      try {
        const signing = await this.invokeOn(name, "SetGPGKeyForSigning", {GpgKeyFingerprint: fingerprint});
        if (signing.Error) problems.push("the signing key was not set: " + (signing.Message || "request failed"));
      } catch (error) { problems.push("the signing key was not set: " + (error as Error).message); }
    }
    const sshKey = this.selected("create-ssh-key");
    const username = this.text("create-username");
    const password = this.getWidget("create-password").getProperty("Text") || "";
    if (sshKey || username || password) {
      try {
        const signIn = await this.invokeOn(name, "RemoteSignInSave", {
          Remote: "origin", GitCommitterUser: username, GitCommitterPassword: password, SshKeyFingerprint: sshKey
        });
        if (signIn.Error) problems.push("the sign-in for origin was not saved: " + (signIn.Message || "request failed"));
      } catch (error) { problems.push("the sign-in for origin was not saved: " + (error as Error).message); }
    }
    if (this.destroyed) return;
    const prefix = problems.length ? "Repository created, but " + problems.join("; ") + "." : "Repository created.";
    this.createdRepoName = name;
    if (this.urlKind(url) === "ssh") {
      await this.checkHostKey(name, prefix);
      return;
    }
    this.finish(prefix);
  }

  /** Reads the new repository's SSH host key and opens the approval dialog. */
  private async checkHostKey(name: string, prefix: string) {
    this.message = prefix + " Reading the SSH server's host key…";
    try {
      const result = await this.invokeOn(name, "SshHostKeyProbe", {Remote: "origin"});
      if (result.Error) throw new Error(result.Message || "request failed");
      const key = result.Result && result.Result.rows && result.Result.rows[0];
      if (!key || !key.Fingerprint) throw new Error("The server did not return a host key.");
      if (this.destroyed) return;
      this.checkedHostKey = {repository: name, fingerprint: key.Fingerprint};
      const type = String(key.PublicKey || "").split(" ")[0] || "unknown";
      this.hostKeyPrompt = "Before ThingWorx connects to " + key.Host + " (port " + key.Port + ") over SSH, approve the server's " +
        type + " host key with the fingerprint " + key.Fingerprint + " — only if it matches the " + type +
        " fingerprint the Git server's operator gave you. If you skip this, you can check and approve the key later under Remotes and sign-in in Repository Settings.";
      this.message = prefix + " Waiting for you to approve the SSH host key…";
      this.hostKeyChecked();
    } catch (error) {
      this.finish(prefix + " The SSH host key could not be read (" + (error as Error).message +
        "). Check and approve it under Remotes and sign-in in Repository Settings.");
    }
  }

  @service async approveHostKey() {
    const checked = this.checkedHostKey;
    if (!checked) return;
    this.checkedHostKey = null;
    try {
      const result = await this.invokeOn(checked.repository, "SshHostKeyApprove", {Remote: "origin", Fingerprint: checked.fingerprint});
      if (result.Error) throw new Error(result.Message || "request failed");
      this.finish("Repository created and the SSH host key approved.");
    } catch (error) {
      this.finish("Repository created, but the SSH host key was not approved: " + (error as Error).message +
        " Check and approve it under Remotes and sign-in in Repository Settings.");
    }
  }

  @service skipHostKey() {
    if (!this.checkedHostKey) return;
    this.checkedHostKey = null;
    this.finish("Repository created. SSH connections are refused until you approve the host key under Remotes in Repository Settings.");
  }

  private finish(message: string) {
    this.busy = false;
    if (this.destroyed) return;
    this.message = message + " Opening Repository Settings…";
    this.created();
  }
}
