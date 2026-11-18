@TWWidgetDefinition
class RepositorySettings extends TypescriptWidget {
  @property(bindingTarget) repoName!: STRING;
  /** Signing key saved for this repository, as a Value of signingOptions. */
  @property(bindingSource) loadedSigningKey: STRING = "";
  /** GPG and SSH keys that can sign; Value is "openpgp:<fingerprint>" or "ssh:<fingerprint>". */
  @property(bindingSource) signingOptions!: INFOTABLE<{Display: STRING, Value: STRING}>;
  @property(bindingTarget, didBind("signingKeyChanged")) selectedSigningKey!: STRING;
  /** Reuse warning, SSH hint and RequireSigning warning under the Signing key dropdown. */
  @property(bindingSource) signingNote: STRING = "";
  @property(bindingSource) showSigningNote: BOOLEAN = false;
  @property(bindingSource) requireSigning: BOOLEAN = false;
  @property(bindingSource) requireSigningDisabled: BOOLEAN = true;
  @property(bindingTarget, didBind("signingKeyChanged")) requireSigningChecked!: BOOLEAN;
  @property(bindingSource) sshKeyOptions!: INFOTABLE<{SshKeyLabel: STRING, SshKeyFingerprint: STRING}>;
  @property(bindingSource) message: STRING = "";
  @property(bindingSource) saveDisabled: BOOLEAN = true;
  @property(bindingSource) trackedProjects!: INFOTABLE<{ProjectName: STRING, Path: STRING, Folder: STRING, EntityTypes: STRING, Types: STRING}>;
  /** Entity types a tracked project can synchronize, for the Entity types dropdown. */
  @property(bindingSource) entityTypeOptions!: INFOTABLE<{EntityType: STRING, Label: STRING}>;
  /** The selected project's entity types, selected in the Entity types dropdown. */
  @property(bindingSource) projectEntityTypes!: INFOTABLE<{EntityType: STRING, Label: STRING}>;
  /** The types chosen in the Entity types dropdown. */
  @property(bindingTarget) selectedEntityTypes!: INFOTABLE<{EntityType: STRING, Label: STRING}>;
  @property(bindingSource) availableProjects!: INFOTABLE<{ProjectName: STRING}>;
  @property(bindingTarget) selectedProjects!: INFOTABLE<{ProjectName: STRING, Path: STRING, EntityTypes: STRING}>;
  @property(bindingSource) projectMessage: STRING = "";
  /** Result of Add project, shown in the Add a tracked project card. */
  @property(bindingSource) addProjectMessage: STRING = "";
  @property(bindingSource) projectsBusy: BOOLEAN = true;
  @property(bindingSource) remotes!: INFOTABLE<{Name: STRING, URLs: STRING, PushURLs: STRING, FetchSpecs: STRING, SignIn: STRING}>;
  @property(bindingTarget) selectedRemotes!: INFOTABLE<{Name: STRING, URLs: STRING}>;
  @property(bindingSource) remoteMessage: STRING = "";
  @property(bindingSource) remotesBusy: BOOLEAN = true;
  /** Sign-in of the remote selected in the remotes list. */
  @property(bindingSource) signInTitle: STRING = "Sign-in";
  @property(bindingSource) signInHint: STRING = "Select a remote above to see and change how you sign in to it.";
  @property(bindingSource) signInUsername: STRING = "";
  @property(bindingSource) signInPassword: STRING = "";
  @property(bindingSource) signInSshFingerprint: STRING = "";
  @property(bindingSource) signInMessage: STRING = "";
  @property(bindingSource) signInDisabled: BOOLEAN = true;
  @property(bindingSource) clearSignInDisabled: BOOLEAN = true;
  @property(bindingSource) showPasswordFields: BOOLEAN = false;
  @property(bindingSource) showSshKey: BOOLEAN = false;
  @property(bindingSource) statusTitle: STRING = "Connection setup";
  /** Progress and result of Test connection. */
  @property(bindingSource) statusMessage: STRING = "";
  /** 1-based row the remotes list selects after loading, so a reload keeps the selection. */
  @property(bindingSource) remoteRowToSelect: STRING = "";
  /** The connection setup checklist, one block per remote. */
  @property(bindingSource) statusText: STRING = "Checking…";
  @property(bindingSource) hostKeyFingerprint: STRING = "";
  @property(bindingSource) hostKeyPrompt: STRING = "";
  @property(bindingSource) hostKeyMessage: STRING = "";
  @property(bindingSource) approveDisabled: BOOLEAN = true;
  /** Fires after a host key was read, to open the approval dialog. */
  @event hostKeyChecked!: TWEvent;
  /** Text of the Delete repository confirmation, naming the repository. */
  @property(bindingSource) deletePrompt: STRING = "";
  /** Progress and result of Delete repository. */
  @property(bindingSource) deleteMessage: STRING = "";
  @property(bindingSource) deleteDisabled: BOOLEAN = false;
  /** Fires after the repository was deleted, to leave this page. */
  @event repositoryDeleted!: TWEvent;
  private remoteRequestId = 0;
  private requestId = 0;
  private projectRequestId = 0;
  private statusRequestId = 0;
  /** The remote and fingerprint shown in the approval dialog. */
  private checkedHostKey: {remote: string, fingerprint: string} | null = null;
  /** Connection test results from this page visit, by remote name. */
  private tested: {[remote: string]: string} = {};
  /** Each remote's sign-in from RemoteSignInList, by remote name. */
  private signIns: {[remote: string]: any} = {};
  /** SSH key labels by fingerprint. */
  private sshKeyLabels: {[fingerprint: string]: string} = {};
  private signInBusy = false;
  private testing = false;
  /** The remote whose sign-in and messages are shown. */
  private shownRemote = "";

  beforeDestroy() { this.requestId++; this.projectRequestId++; this.remoteRequestId++; this.statusRequestId++; }

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
    this.deletePrompt = "Repository " + this.repoName + " will be deleted for every user. Its working files and any uncommitted "
      + "changes are removed, and everyone's settings and sign-ins for it are forgotten. Its history is moved to jgit/deleted "
      + "without its remotes, where only an administrator can recover unpushed commits. "
      + "ThingWorx entities in its tracked projects are not changed.";
    this.loadEntityTypeOptions();
    this.loadProjects();
    this.loadRemotes();
    this.loadStatus();
    this.loadSigning();
  }

  /** The RequireSigning value loaded, to save it only when the checkbox changed it. */
  private loadedRequireSigning = false;
  private canChangeRequireSigning = false;

  /** Loads the Commit signing card: the user's GPG and SSH keys and the saved choice. */
  private async loadSigning() {
    const repository = this.repoName;
    const request = ++this.requestId;
    this.saveDisabled = true;
    this.message = "Loading signing keys…";
    try {
      const [signing, options] = await Promise.all([
        this.invokeOn(repository, "GetRepositorySigning", {}),
        this.invokeOn(repository, "GetSigningKeyOptions", {})
      ]);
      if (request !== this.requestId) return;
      if (signing.Error) throw new Error(signing.Message || "Unable to load the signing key");
      if (options.Error) throw new Error(options.Message || "Unable to load signing keys");
      const saved = signing.Result.rows[0] || {};
      const rows: any[] = options.Result.rows;
      // Repository Settings always offers None, even where commits require signing.
      if (!rows.some(row => row.Value === "none")) rows.push({Value: "none", Display: "None (not signed)", Format: "", Fingerprint: ""});
      const selected = saved.SigningFormat ? saved.SigningFormat + ":" + saved.KeyFingerprint : "none";
      const keyMissing = selected !== "none" && !rows.some(row => row.Value === selected);
      if (keyMissing) rows.push({Display: "Unavailable signing key: " + saved.KeyFingerprint, Value: selected, Format: saved.SigningFormat, Fingerprint: saved.KeyFingerprint});
      this.signingRows = rows;
      this.signingOptions = {dataShape: options.Result.dataShape, rows} as any;
      this.loadedSigningKey = selected;
      this.loadedRequireSigning = !!saved.RequireSigning;
      this.canChangeRequireSigning = !!saved.CanChangeRequireSigning;
      this.requireSigning = this.loadedRequireSigning;
      this.requireSigningDisabled = !this.canChangeRequireSigning;
      this.saveDisabled = false;
      this.message = keyMissing ? "The saved signing key is unavailable. Choose another key or None." : "";
      this.signingKeyChanged();
    } catch (error) {
      if (request === this.requestId) this.message = "Could not load settings: " + (error as Error).message;
    }
  }

  /** Splits a signingOptions Value into its format and fingerprint. */
  private static parseSigningKey(value: string): {format: string, fingerprint: string} {
    const i = (value || "").indexOf(":");
    return i < 0 ? {format: "", fingerprint: ""} : {format: value.slice(0, i), fingerprint: value.slice(i + 1)};
  }

  /** Rows of signingOptions, from GetSigningKeyOptions. */
  private signingRows: any[] = [];

  /** Updates the note under the Signing key dropdown for the current selection. */
  @service signingKeyChanged() {
    const key = RepositorySettings.parseSigningKey(this.selectedSigningKey === undefined ? this.loadedSigningKey : this.selectedSigningKey);
    const require = this.requireSigningChecked === undefined ? this.requireSigning : !!this.requireSigningChecked;
    const notes: string[] = [];
    const row = this.signingRows.filter(option => option.Value === key.format + ":" + key.fingerprint)[0];
    if (row && row.IsSignInKey) {
      notes.push("The signing key is also your sign-in key. A leak of it exposes both.");
    }
    if (key.format === "ssh") {
      notes.push("Add this public key to your Git host as a signing key. Manage keys and identity shows it.");
    }
    if (require && !key.format) notes.push("This repository requires signed commits, so commits will fail until you choose a key.");
    this.signingNote = notes.join(" ");
    this.showSigningNote = notes.length > 0;
  }

  /** Saves the commit signing key, and RequireSigning when it changed. Sign-in is saved per remote by saveSignIn. */
  @service async save() {
    if (!this.repoName || this.saveDisabled) return;
    this.saveDisabled = true;
    this.message = "Saving…";
    try {
      const key = RepositorySettings.parseSigningKey(this.getWidget("settings-key").getProperty("SelectedText") || "");
      const result = await this.invokeOn(this.repoName, "SetRepositorySigningKey", {
        SigningFormat: key.format,
        KeyFingerprint: key.fingerprint
      });
      if (result.Error) throw new Error(result.Message || "Unable to save the signing key");
      const messages = [result.Message || "Signing key saved."];
      const require = !!this.getWidget("settings-require-signing").getProperty("State");
      if (this.canChangeRequireSigning && require !== this.loadedRequireSigning) {
        const changed = await this.invokeOn(this.repoName, "SetRequireSigning", {RequireSigning: require});
        if (changed.Error) throw new Error(changed.Message || "Unable to change Require signing");
        this.loadedRequireSigning = require;
        messages.push(changed.Message);
      }
      this.loadedSigningKey = key.format ? key.format + ":" + key.fingerprint : "";
      this.message = messages.join(" ");
    } catch (error) { this.message = "Could not save the signing key: " + (error as Error).message; }
    finally { this.saveDisabled = false; }
  }

  private sshKeyRows: any[] = [];
  private sshKeyShape: any = {fieldDefinitions: {
    SshKeyLabel: {name: "SshKeyLabel", baseType: "STRING"}, SshKeyFingerprint: {name: "SshKeyFingerprint", baseType: "STRING"}}};

  /** The transports a remote's fetch and push URLs use: "ssh", "https" and "http". */
  private transports(remote: any): string[] {
    const urls = [remote.URLs, remote.PushURLs].join("\n").split(/[\s,]+/).filter((url: string) => url);
    const found: string[] = [];
    for (const url of urls) {
      const scheme = /^([a-z][a-z0-9+.-]*):\/\//i.exec(url);
      const transport = scheme ? scheme[1].toLowerCase() : "ssh";
      if (found.indexOf(transport) < 0) found.push(transport);
    }
    return found;
  }

  private selectedRemoteRow(): any {
    const name = this.selectedRemote();
    const rows = this.remotes && this.remotes.rows || [];
    return name ? rows.find((row: any) => row.Name === name) || null : null;
  }

  /** Short description of a remote's sign-in for the remotes list. */
  private signInSummary(remote: any): string {
    const signIn = this.signIns[remote.Name];
    if (!signIn) return "";
    const transports = this.transports(remote);
    const parts: string[] = [];
    if (transports.indexOf("https") >= 0 || transports.indexOf("http") >= 0) {
      parts.push(signIn.GitCommitterUser ? signIn.GitCommitterUser + (signIn.HasPassword ? " (token saved)" : " (no token)") : "No username");
    }
    if (transports.indexOf("ssh") >= 0) {
      parts.push(signIn.SshKeyFingerprint ? "SSH key: " + (this.sshKeyLabels[signIn.SshKeyFingerprint] || "unavailable key") : "No SSH key");
    }
    return parts.join(", ") + (signIn.Inherited && (signIn.GitCommitterUser || signIn.SshKeyFingerprint) ? " – repository-wide" : "");
  }

  /** Called when the selection in the remotes list changes. */
  @service remoteSelected() {
    // Reloading the list selects the same remote again; keep its messages then.
    const name = this.selectedRemote();
    if (name !== this.shownRemote) {
      this.shownRemote = name;
      this.clearCheckedHostKey();
      this.hostKeyMessage = "";
      this.signInMessage = "";
      this.statusMessage = "";
      this.showSelectedRemoteFields();
    }
    this.showSelectedSignIn();
    this.loadStatus();
  }

  /** Fills Remote name, Fetch URL and Push URL with the selected remote, so Update selected remote edits them. */
  private showSelectedRemoteFields() {
    const remote = this.selectedRemoteRow();
    if (!remote) return;
    const first = (urls: string) => String(urls || "").split("\n")[0].trim();
    this.getWidget("settings-remote-name").setProperty("Text", remote.Name);
    this.getWidget("settings-remote-url").setProperty("Text", first(remote.URLs));
    this.getWidget("settings-remote-push-url").setProperty("Text", first(remote.PushURLs));
  }

  /** Fills the sign-in fields with the selected remote's saved sign-in. */
  private showSelectedSignIn() {
    const remote = this.selectedRemoteRow();
    this.signInPassword = "";
    if (!remote) {
      this.signInTitle = "Sign-in";
      this.signInHint = "Select a remote above to see and change how you sign in to it. Each remote has its own sign-in.";
      this.signInUsername = "";
      this.signInSshFingerprint = "";
      this.showPasswordFields = false;
      this.showSshKey = false;
      this.signInDisabled = true;
      this.clearSignInDisabled = true;
      return;
    }
    const signIn = this.signIns[remote.Name] || {};
    const transports = this.transports(remote);
    const ssh = transports.indexOf("ssh") >= 0;
    const https = transports.indexOf("https") >= 0;
    const http = transports.indexOf("http") >= 0;
    this.signInTitle = "Sign-in for " + remote.Name;
    const hints: string[] = [];
    if (https || http) hints.push("Enter your Git username and a password or access token for " + remote.Name + ". Leave the password blank to keep the saved one.");
    if (http) hints.push("Saved passwords are never sent over plain http://, so an http:// URL can only pull without signing in. Use https:// or ssh:// to push.");
    if (ssh) hints.push("Choose the SSH key " + remote.Name + " signs in with, and add its public key to your account on the Git server.");
    if (signIn.Inherited && (signIn.GitCommitterUser || signIn.SshKeyFingerprint)) {
      hints.push("This remote uses the repository-wide sign-in saved by an earlier version. Saving here gives it its own.");
    }
    this.signInHint = hints.join(" ");
    this.signInUsername = signIn.GitCommitterUser || "";
    const fingerprint = signIn.SshKeyFingerprint || "";
    const options = [{SshKeyLabel: "No SSH key", SshKeyFingerprint: ""}, ...this.sshKeyRows];
    if (fingerprint && !this.sshKeyLabels[fingerprint]) {
      options.push({SshKeyLabel: "Unavailable SSH key: " + fingerprint, SshKeyFingerprint: fingerprint});
    }
    this.sshKeyOptions = {dataShape: this.sshKeyShape, rows: options} as any;
    this.signInSshFingerprint = fingerprint;
    this.showPasswordFields = https || http;
    this.showSshKey = ssh;
    this.signInDisabled = this.signInBusy;
    this.clearSignInDisabled = this.signInBusy || !!signIn.Inherited;
  }

  /** Saves the selected remote's sign-in. */
  @service async saveSignIn() {
    const remote = this.selectedRemoteRow();
    if (!this.repoName || !remote || this.signInBusy) return;
    this.signInBusy = true;
    this.signInDisabled = true;
    this.signInMessage = "Saving…";
    try {
      const result = await this.invokeOn(this.repoName, "RemoteSignInSave", {
        Remote: remote.Name,
        GitCommitterUser: this.showPasswordFields ? (this.getWidget("settings-username").getProperty("Text") || "").trim() : (this.signIns[remote.Name] || {}).GitCommitterUser || "",
        GitCommitterPassword: this.showPasswordFields ? this.getWidget("settings-password").getProperty("Text") || "" : "",
        SshKeyFingerprint: this.showSshKey ? this.getWidget("settings-ssh-key").getProperty("SelectedText") || "" : (this.signIns[remote.Name] || {}).SshKeyFingerprint || ""
      });
      if (result.Error) throw new Error(result.Message || "request failed");
      this.signInMessage = result.Message;
      delete this.tested[remote.Name];
    } catch (error) { this.signInMessage = "Could not save the sign-in: " + (error as Error).message; }
    this.signInBusy = false;
    await this.loadRemotes();
    this.loadStatus();
  }

  /** Deletes the selected remote's own sign-in. */
  @service async clearSignIn() {
    const remote = this.selectedRemoteRow();
    if (!this.repoName || !remote || this.signInBusy) return;
    this.signInBusy = true;
    this.signInDisabled = true;
    this.signInMessage = "Deleting…";
    try {
      const result = await this.invokeOn(this.repoName, "RemoteSignInClear", {Remote: remote.Name});
      if (result.Error) throw new Error(result.Message || "request failed");
      this.signInMessage = result.Message;
      delete this.tested[remote.Name];
    } catch (error) { this.signInMessage = "Could not delete the sign-in: " + (error as Error).message; }
    this.signInBusy = false;
    await this.loadRemotes();
    this.loadStatus();
  }

  /**
   * Builds the setup checklist: the user's commit identity, then for each remote whether its
   * server is allowed, sign-in is saved, the saved password is approved for that server and an SSH
   * host key is approved. Nothing is contacted except by Test connection.
   */
  @service async loadStatus() {
    if (!this.repoName) return;
    const request = ++this.statusRequestId;
    try {
      const [status, identity] = await Promise.all([
        this.invokeOn(this.repoName, "GetConnectionStatus", {}),
        this.invoke("GitIdentityGet", {})
      ]);
      if (request !== this.statusRequestId) return;
      if (status.Error) throw new Error(status.Message || "Request failed");
      const lines: string[] = [];
      const who = !identity.Error && identity.Result && identity.Result.rows && identity.Result.rows[0] || {};
      if (who.GitUserName && who.GitUserEmail) {
        lines.push("✓ Commit identity: " + who.GitUserName + " <" + who.GitUserEmail + ">");
      } else {
        lines.push("✗ Commit identity is not set. Set your name and email under Commit identity in Settings.");
      }
      const allRows = status.Result && status.Result.rows || [];
      const focus = this.selectedRemote();
      const rows = focus ? allRows.filter((row: any) => row.Remote === focus) : allRows;
      this.statusTitle = focus ? "Connection setup for " + focus : "Connection setup";
      if (!allRows.length) {
        lines.push("", "✗ No remotes. Add one above to pull from and push to a Git server.");
      }
      const mark = (state: string) => state === "ok" ? "✓" : state === "missing" ? "✗" : "";
      for (const row of rows) {
        const ssh = row.Transport.indexOf("ssh") >= 0;
        const https = row.Transport.indexOf("http") >= 0;
        lines.push("", row.Remote + "  " + row.URL + (row.IsDefault ? "  (used by Pull, Push and Fetch)" : ""));
        lines.push("  " + mark(row.Network) + " Server address allowed");
        if (https) lines.push("  " + mark(row.SignIn) + " Git username and password or token saved");
        if (ssh) lines.push("  " + mark(row.SignIn) + " SSH key selected");
        if (row.CredentialOrigin !== "n/a") lines.push("  " + mark(row.CredentialOrigin) + " Saved password approved for this server");
        if (row.HostKey !== "n/a") lines.push("  " + mark(row.HostKey) + " SSH host key approved");
        const test = this.tested[row.Remote];
        lines.push("  " + (test === undefined ? "– Connection not tested yet" : test === "" ? "✓ Connection test passed" : "✗ Connection test failed: " + test));
        if (!row.Ready) lines.push("  Next: " + row.NextStep);
        else if (test === undefined) lines.push("  Settings complete. Next: select the remote and use Test connection to confirm the server accepts your sign-in.");
        else if (test === "") lines.push("  Ready to pull and push.");
        else lines.push("  Next: the settings are complete, so the server refused the connection. For SSH, check that the key's public key is added to your account on the Git server; for HTTPS, check the username and token.");
      }
      this.statusText = lines.join("\n");
    } catch (error) {
      if (request === this.statusRequestId) this.statusText = "Could not check the connection setup: " + (error as Error).message;
    }
  }

  @service async resetSshHostKeys() {
    if (!this.repoName) return;
    this.hostKeyMessage = "Resetting…";
    try {
      const result = await this.invokeOn(this.repoName, "ResetSshHostKeys", {});
      if (result.Error) throw new Error(result.Message || "Unable to reset SSH host keys");
      this.hostKeyMessage = result.Message;
    } catch (error) { this.hostKeyMessage = "Could not reset SSH host keys: " + (error as Error).message; }
    this.clearCheckedHostKey();
    this.loadStatus();
  }

  /** Deletes this repository, then fires repositoryDeleted to return to the repository list. */
  @service async deleteRepository() {
    if (!this.repoName || this.deleteDisabled) return;
    this.deleteDisabled = true;
    this.deleteMessage = "Deleting…";
    try {
      const result = await this.invoke("RepositoryDelete", {RepoName: this.repoName});
      if (result.Error) throw new Error(result.Message || "Unable to delete the repository");
      this.deleteMessage = result.Message || "Repository deleted.";
      this.repositoryDeleted();
    } catch (error) {
      this.deleteMessage = "Could not delete the repository: " + (error as Error).message;
      this.deleteDisabled = false;
    }
  }

  private clearCheckedHostKey() {
    this.checkedHostKey = null;
    this.hostKeyFingerprint = "";
    this.approveDisabled = true;
  }

  /** Reads the selected remote's SSH host key and opens the approval dialog. */
  @service async checkHostKey() {
    const name = this.selectedRemote();
    if (!this.repoName) return;
    if (!name) { this.hostKeyMessage = "Select an ssh:// remote in the list above."; return; }
    this.clearCheckedHostKey();
    this.hostKeyMessage = "Connecting to " + name + " to read its host key…";
    try {
      const result = await this.invokeOn(this.repoName, "SshHostKeyProbe", {Remote: name});
      if (result.Error) throw new Error(result.Message || "request failed");
      const key = result.Result && result.Result.rows && result.Result.rows[0];
      if (!key || !key.Fingerprint) throw new Error("The server did not return a host key.");
      this.checkedHostKey = {remote: name, fingerprint: key.Fingerprint};
      this.hostKeyFingerprint = key.Fingerprint;
      this.approveDisabled = false;
      this.hostKeyMessage = "Compare this fingerprint with the one the Git server's operator gives you, then approve it.";
      const type = String(key.PublicKey || "").split(" ")[0] || "unknown";
      this.hostKeyPrompt = "Remote " + name + " (" + key.Host + ", port " + key.Port + ") presented a " + type +
        " host key with the fingerprint " + key.Fingerprint + " — approve it only if this matches the " + type +
        " fingerprint the Git server's operator gave you. If you approve a key you have not checked, someone between " +
        "ThingWorx and the server could read and change what you pull and push.";
      this.hostKeyChecked();
    } catch (error) { this.hostKeyMessage = "Could not read the host key: " + (error as Error).message; }
  }

  /** Approves the fingerprint read by checkHostKey. The server must still present the same key. */
  @service async approveHostKey() {
    const checked = this.checkedHostKey;
    if (!this.repoName || !checked) { this.hostKeyMessage = "Check the host key first."; return; }
    this.approveDisabled = true;
    this.hostKeyMessage = "Approving…";
    try {
      const result = await this.invokeOn(this.repoName, "SshHostKeyApprove", {Remote: checked.remote, Fingerprint: checked.fingerprint});
      if (result.Error) throw new Error(result.Message || "request failed");
      this.hostKeyMessage = result.Message;
      this.clearCheckedHostKey();
    } catch (error) {
      this.hostKeyMessage = "Could not approve the host key: " + (error as Error).message;
      this.approveDisabled = false;
    }
    this.loadStatus();
  }

  @service async loadRemotes() {
    if (!this.repoName) return;
    const request = ++this.remoteRequestId;
    this.remotesBusy = true;
    try {
      // The SSH key names are loaded with the remotes, so the sign-in column can name them.
      const [result, signIns, sshKeys] = await Promise.all([
        this.invokeOn(this.repoName, "GetRemotes", {}),
        this.invokeOn(this.repoName, "RemoteSignInList", {}),
        this.invoke("SshKeyListSummary", {})
      ]);
      if (request !== this.remoteRequestId) return;
      if (result.Error) throw new Error(result.Message || "Request failed");
      if (signIns.Error) throw new Error(signIns.Message || "Could not load sign-in");
      if (sshKeys.Error) throw new Error(sshKeys.Message || "Could not load SSH keys");
      this.sshKeyLabels = {};
      for (const key of sshKeys.Result.rows) this.sshKeyLabels[key.SshKeyFingerprint] = key.SshKeyLabel || key.SshKeyFingerprint;
      this.sshKeyRows = sshKeys.Result.rows;
      this.sshKeyShape = sshKeys.Result.dataShape;
      this.signIns = {};
      for (const row of signIns.Result.rows) this.signIns[row.Remote] = row;
      const table = result.Result;
      table.dataShape.fieldDefinitions.SignIn = {name: "SignIn", baseType: "STRING", ordinal: 99};
      for (const row of table.rows) row.SignIn = this.signInSummary(row);
      const keep = table.rows.findIndex((row: any) => row.Name === this.selectedRemote());
      this.remoteRowToSelect = keep >= 0 ? String(keep + 1) : "";
      this.remotes = table;
      this.showSelectedSignIn();
      if (!this.remotes.rows.length && !this.remoteMessage) {
        this.remoteMessage = "No remotes are configured. Add one to pull from and push to a Git server.";
      }
    } catch (error) {
      if (request === this.remoteRequestId) this.remoteMessage = "Could not load remotes: " + (error as Error).message;
    } finally {
      if (request === this.remoteRequestId) this.remotesBusy = false;
    }
  }

  private selectedRemote(): string {
    const rows = this.selectedRemotes && this.selectedRemotes.rows || [];
    return rows.length ? rows[0].Name : "";
  }

  /**
   * Runs one remote service, shows its message, and reloads the list when it changes remotes.
   *
   * @return true when the service succeeded
   */
  private async remoteAction(progress: string, failure: string, serviceName: string, payload: object, reload: boolean): Promise<boolean> {
    if (!this.repoName || this.remotesBusy) return false;
    this.remotesBusy = true;
    this.remoteMessage = progress;
    let succeeded = false;
    try {
      const result = await this.invokeOn(this.repoName, serviceName, payload);
      if (result.Error) throw new Error(result.Message || "request failed");
      this.remoteMessage = result.Message;
      succeeded = true;
    } catch (error) { this.remoteMessage = failure + ": " + (error as Error).message; }
    if (succeeded && reload) await this.reloadRemotes();
    else this.remotesBusy = false;
    return succeeded;
  }

  @service async addRemote() {
    const name = this.fieldText("settings-remote-name");
    const url = this.fieldText("settings-remote-url");
    const pushUrl = this.fieldText("settings-remote-push-url");
    if (!this.repoName || this.remotesBusy) return;
    if (!name || !url) { this.remoteMessage = "Enter a remote name and fetch URL to add."; return; }
    if (!await this.remoteAction("Adding " + name + "…", "Could not add the remote", "RemoteAdd", {Name: name, URL: url}, !pushUrl)) return;
    if (pushUrl) {
      const added = this.remoteMessage;
      if (await this.remoteAction("Setting the push URL of " + name + "…", "Remote added, but could not set the push URL", "RemoteSetUrl",
          {Name: name, URL: pushUrl, Push: true}, true)) this.remoteMessage = added + " " + this.remoteMessage;
      else await this.reloadRemotes();
    }
  }

  /**
   * Saves Fetch URL and Push URL to the selected remote; a blank Push URL removes the separate
   * push URL. Only the fields that differ from the saved remote are changed.
   */
  @service async updateRemote() {
    const remote = this.selectedRemoteRow();
    if (!this.repoName || this.remotesBusy) return;
    if (!remote) { this.remoteMessage = "Select a remote to change."; return; }
    const name = this.fieldText("settings-remote-name");
    if (name !== remote.Name) {
      this.remoteMessage = "Remotes cannot be renamed. To use the name " + (name || "(blank)")
          + ", add it as a new remote and remove " + remote.Name + ".";
      return;
    }
    const url = this.fieldText("settings-remote-url");
    const pushUrl = this.fieldText("settings-remote-push-url");
    if (!url) { this.remoteMessage = "Enter the fetch URL of " + name + "."; return; }
    const saved = (urls: string) => String(urls || "").split("\n")[0].trim();
    const changes: {URL: string, Push: boolean}[] = [];
    if (url !== saved(remote.URLs)) changes.push({URL: url, Push: false});
    if (pushUrl !== saved(remote.PushURLs)) changes.push({URL: pushUrl, Push: true});
    if (!changes.length) { this.remoteMessage = "Remote '" + name + "' already uses these URLs."; return; }
    const messages: string[] = [];
    for (let i = 0; i < changes.length; i++) {
      const change = changes[i];
      const failure = change.Push ? (pushUrl ? "Could not set the push URL" : "Could not remove the push URL") : "Could not change the fetch URL";
      const ok = await this.remoteAction("Updating " + name + "…", failure, "RemoteSetUrl", {Name: name, URL: change.URL, Push: change.Push},
          i === changes.length - 1);
      if (!ok) {
        if (messages.length) { this.remoteMessage = messages.join(" ") + " " + this.remoteMessage; await this.reloadRemotes(); }
        return;
      }
      messages.push(this.remoteMessage);
    }
    this.remoteMessage = messages.join(" ");
  }

  /** Reloads the remotes and connection status after a change. */
  private async reloadRemotes() {
    this.tested = {};
    this.clearCheckedHostKey();
    await this.loadRemotes();
    this.loadStatus();
  }

  @service async testRemote() {
    const name = this.selectedRemote();
    if (!this.repoName) return;
    if (!name) { this.statusMessage = "Select a remote to test."; return; }
    if (this.testing) return;
    this.testing = true;
    this.statusMessage = "Connecting to " + name + "…";
    try {
      const result = await this.invokeOn(this.repoName, "TestRemote", {Remote: name});
      if (result.Error) throw new Error(result.Message || "request failed");
      this.statusMessage = result.Message;
      this.tested[name] = "";
    } catch (error) {
      this.statusMessage = "Connection test failed: " + (error as Error).message;
      this.tested[name] = (error as Error).message;
    } finally { this.testing = false; }
    this.loadStatus();
  }

  @service async removeRemote() {
    const name = this.selectedRemote();
    if (!name) { this.remoteMessage = "Select a remote to remove."; return; }
    await this.remoteAction("Removing " + name + "…", "Could not remove the remote", "RemoteRemove", {Name: name}, true);
  }

  private projectRows(row: any): any {
    if (row.Error) throw new Error(row.Message || "Request failed");
    return row.Result || {dataShape: {fieldDefinitions: {ProjectName: {name: "ProjectName", baseType: "STRING"}}}, rows: []};
  }

  /**
   * Adds a Folder column showing where each project's entities live in the repository, and a Types column
   * listing the entity types it synchronizes.
   */
  private withFolders(table: any): any {
    const fields = table.dataShape.fieldDefinitions;
    fields.Path = fields.Path || {name: "Path", baseType: "STRING", ordinal: 2};
    fields.EntityTypes = fields.EntityTypes || {name: "EntityTypes", baseType: "STRING", ordinal: 3};
    fields.Folder = {name: "Folder", baseType: "STRING", ordinal: 4};
    fields.Types = {name: "Types", baseType: "STRING", ordinal: 5};
    for (const row of table.rows) {
      const path = String(row.Path || "").replace(/^\/+|\/+$/g, "");
      row.Path = path;
      row.Folder = path ? path + "/" : row.ProjectName + "/ (default)";
      row.EntityTypes = String(row.EntityTypes || "");
      const types = this.typeList(row.EntityTypes);
      row.Types = types.length ? types.map(type => this.typeLabel(type)).join(", ") : "All";
    }
    return table;
  }

  /** The collections in a stored EntityTypes value; empty means all types. */
  private typeList(value: string): string[] {
    return String(value || "").split(",").map(type => type.trim()).filter(type => type);
  }

  private typeLabel(type: string): string {
    const option = (this.entityTypeOptions && this.entityTypeOptions.rows || []).find((row: any) => row.EntityType === type);
    return option ? option.Label : type.replace(/([a-z])([A-Z])/g, "$1 $2");
  }

  private typeTable(rows: any[]): any {
    return {dataShape: {fieldDefinitions: {EntityType: {name: "EntityType", baseType: "STRING", ordinal: 1},
      Label: {name: "Label", baseType: "STRING", ordinal: 2}}}, rows};
  }

  @service async loadEntityTypeOptions() {
    if (!this.repoName) return;
    try {
      const result = await this.invokeOn(this.repoName, "GetEntityTypeOptions", {});
      if (result.Error) throw new Error(result.Message || "Request failed");
      this.entityTypeOptions = result.Result || this.typeTable([]);
      // Labels in the Types column come from the options.
      if (this.trackedProjects) this.trackedProjects = this.withFolders(this.trackedProjects);
    } catch (error) { this.projectMessage = "Could not load the entity types: " + (error as Error).message; }
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
      this.trackedProjects = this.withFolders(this.projectRows(tracked));
      // Reloading clears the grid selection without a selection event.
      this.getWidget("settings-project-folder").setProperty("Text", "");
      this.projectEntityTypes = this.typeTable([]);
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

  private fieldText(widgetId: string): string {
    return (this.getWidget(widgetId).getProperty("Text") || "").trim();
  }

  /**
   * Fills Folder of selected project with the folder of the one selected project, and selects in Entity types
   * the types the selected projects synchronize when they all synchronize the same ones.
   */
  @service projectSelected() {
    const selected = this.selectedProjects && this.selectedProjects.rows || [];
    this.getWidget("settings-project-folder").setProperty("Text", selected.length === 1 ? selected[0].Path || "" : "");
    const stored = selected.map((row: any) => this.typeList(row.EntityTypes).join(","));
    const same = stored.length > 0 && stored.every(value => value === stored[0]);
    const types = same ? this.typeList(stored[0]) : [];
    const options = this.entityTypeOptions && this.entityTypeOptions.rows || [];
    this.projectEntityTypes = this.typeTable(!same ? [] : types.length
      ? options.filter((row: any) => types.indexOf(row.EntityType) >= 0) : options.slice());
  }

  /** Sets the entity types of every selected project to those chosen in Entity types. */
  @service async setProjectEntityTypes() {
    const selected = this.selectedProjects && this.selectedProjects.rows || [];
    if (!this.repoName || this.projectsBusy || !selected.length) return;
    const chosen = (this.selectedEntityTypes && this.selectedEntityTypes.rows || []).map((row: any) => row.EntityType);
    if (!chosen.length) { this.projectMessage = "Choose at least one entity type to synchronize."; return; }
    const options = this.entityTypeOptions && this.entityTypeOptions.rows || [];
    // Every option means all types, including any added later.
    const value = chosen.length >= options.length ? "" : chosen.join(",");
    this.projectsBusy = true;
    this.projectMessage = "Changing the entity types…";
    const messages: string[] = [];
    for (const row of selected) {
      try {
        const result = await this.invokeOn(this.repoName, "SetTrackedProjectEntityTypes", {ProjectName: row.ProjectName, EntityTypes: value});
        messages.push(result.Error ? "Could not change " + row.ProjectName + ": " + (result.Message || "request failed") : result.Message);
      } catch (error) { messages.push("Could not change " + row.ProjectName + ": " + (error as Error).message); }
    }
    this.projectMessage = messages.join(" ");
    await this.loadProjects();
  }

  @service async addProject() {
    const project = this.getWidget("settings-project-select").getProperty("SelectedText") || "";
    if (!this.repoName || this.projectsBusy) return;
    if (!project) { this.addProjectMessage = "Choose a project to add."; return; }
    this.projectsBusy = true;
    this.addProjectMessage = "Adding " + project + "…";
    try {
      const result = await this.invokeOn(this.repoName, "AddTrackedProject", {ProjectName: project, Path: this.fieldText("settings-project-add-folder")});
      if (result.Error) throw new Error(result.Message || "Unable to add project");
      this.addProjectMessage = result.Message || "Project added.";
      this.projectMessage = "";
      this.getWidget("settings-project-add-folder").setProperty("Text", "");
    } catch (error) { this.addProjectMessage = "Could not add project: " + (error as Error).message; }
    await this.loadProjects();
  }

  /**
   * Sets the folder of the selected project to Folder of selected project; a blank field restores the default.
   * With Move existing files checked, the project's files are moved to the new folder like git mv.
   */
  @service async setProjectPath() {
    const selected = this.selectedProjects && this.selectedProjects.rows || [];
    if (!this.repoName || this.projectsBusy) return;
    if (selected.length !== 1) { this.projectMessage = "Select one project to change its folder."; return; }
    const project = selected[0].ProjectName;
    this.projectsBusy = true;
    this.projectMessage = "Changing the folder of " + project + "…";
    try {
      const result = await this.invokeOn(this.repoName, "SetTrackedProjectPath", {ProjectName: project, Path: this.fieldText("settings-project-folder"),
          MoveFiles: this.getWidget("settings-project-move").getProperty("State") !== false});
      if (result.Error) throw new Error(result.Message || "Unable to change the folder");
      this.projectMessage = result.Message || "Folder changed.";
    } catch (error) { this.projectMessage = "Could not change the folder: " + (error as Error).message; }
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
