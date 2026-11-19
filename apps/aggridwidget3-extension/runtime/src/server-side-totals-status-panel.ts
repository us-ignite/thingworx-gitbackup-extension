import { writeWidgetLog } from "./widget-logging";
const document: any = globalThis.document;

export class CustomServerSideTotalsStatusPanel {
  loggingEnabled: boolean;
  params: any;
  eGui: any;
  rowsLabel: any;

  constructor() {
    this.loggingEnabled = true;
  }
  log(prefix: string, message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomServerSideTotalsStatusPanel()",
      prefix,
      message,
    );
  }
  init(params) {
    this.params = params;
    this.loggingEnabled = this.params.context.EnableConsoleLogging;
    this.eGui = document.createElement("div");
    this.rowsLabel = document.createElement("span");
    this.rowsLabel.setAttribute("class", "ag-status-name-value-value");
    this.rowsLabel.innerHTML = "Rows: ";
    this.eGui.appendChild(this.rowsLabel);
    this.log(".init(): ", `Starting, this.eGui = ${JSON.stringify(this.eGui)}`);
  }
  getGui() {
    return this.eGui;
  }
  destroy() {}
  updateRowCount(newTotals) {
    if (this.params.context.EnableConsoleLogging) {
      console.log(
        "CustomServerSideTotalsStatusPanel.updateRowCount(): newTotals = {}",
        newTotals,
      );
    }
    this.rowsLabel.innerHTML = `Rows: ${newTotals}`;
  }
}
