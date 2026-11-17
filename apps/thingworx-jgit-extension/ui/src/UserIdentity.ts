@TWWidgetDefinition
class UserIdentity extends TypescriptWidget {
  @property(bindingSource) loadedName: STRING = "";
  @property(bindingSource) loadedEmail: STRING = "";
  @property(bindingSource) message: STRING = "";
  @property(bindingSource) saveDisabled: BOOLEAN = true;

  beforeDestroy() {}

  private async invoke(serviceName: string, payload: object): Promise<any> {
    const response = await fetch("/Thingworx/Things/GIT.Utility.Thing/Services/" + serviceName, {
      method: "POST", credentials: "same-origin",
      headers: {"Accept": "application/json", "Content-Type": "application/json",
        "X-XSRF-TOKEN": "TWX-XSRF-TOKEN-VALUE", "X-Requested-By": "ThingWorx"},
      body: JSON.stringify(payload)
    });
    if (!response.ok) throw new Error("Request failed (HTTP " + response.status + ")");
    const result = await response.json();
    const row = result && result.rows && result.rows[0];
    if (!row || typeof row.Error !== "boolean") throw new Error("Invalid service response");
    if (row.Error) throw new Error(row.Message || "Request failed");
    return row;
  }

  @service async load() {
    try {
      const row = await this.invoke("GitIdentityGet", {});
      const identity = row.Result && row.Result.rows && row.Result.rows[0] || {};
      this.loadedName = identity.GitUserName || "";
      this.loadedEmail = identity.GitUserEmail || "";
      this.message = this.loadedName && this.loadedEmail ? "" :
        "Set your name and email before committing. They are recorded on every commit you make.";
    } catch (error) { this.message = "Could not load your commit identity: " + (error as Error).message; }
    this.saveDisabled = false;
  }

  @service async save() {
    if (this.saveDisabled) return;
    this.saveDisabled = true;
    this.message = "Saving…";
    try {
      const row = await this.invoke("GitIdentitySave", {
        GitUserName: this.getWidget("identity-name").getProperty("Text") || "",
        GitUserEmail: this.getWidget("identity-email").getProperty("Text") || ""
      });
      this.message = row.Message;
    } catch (error) { this.message = "Could not save your commit identity: " + (error as Error).message; }
    this.saveDisabled = false;
  }
}
