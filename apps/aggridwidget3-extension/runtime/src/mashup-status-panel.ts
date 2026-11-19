const document: any = globalThis.document;

export class CustomMashupStatusPanel {
  params: any;
  visible: boolean;
  eGui: any;
  eButton: any;

  init(params) {
    this.params = params;
    this.visible = true;
    this.eGui = document.createElement("div");
    const mashup = document.createElement("embed");
    mashup.src =
      `/Thingworx/Mashups/${this.params.context.CustomMashupStatusPanelMashupName}`;
    this.eGui.appendChild(mashup);
  }
  getGui() {
    return this.eGui;
  }
  destroy() {
    this.eButton.removeEventListener("click", this.buttonListener);
  }
  buttonListener(arg0: string, buttonListener: any) {
    throw new Error("Method not implemented.");
  }
  onButtonClicked() {
    alert(`Selected Row Count: ${this.params.api.getSelectedRows().length}`);
  }
  setVisible(visible) {
    this.visible = visible;
    this.eGui.style.display = this.visible ? "block" : "none";
  }
  isVisible() {
    return this.visible;
  }
}
