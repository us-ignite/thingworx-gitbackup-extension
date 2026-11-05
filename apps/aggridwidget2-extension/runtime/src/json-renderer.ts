import { writeWidgetLog } from "./widget-logging";

export class CustomJsonRenderer {
  loggingEnabled: boolean;
  log(message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomJsonRenderer",
      this.logPrefix,
      message,
    );
  }
  containerId: string;
  logPrefix: string;
  paramValue: any;
  container: HTMLDivElement;
  input: HTMLParagraphElement;
  constructor() {
    this.loggingEnabled = true;
    const now = new Date().getTime();
    this.containerId = (now + 1).toString();
  }
  init(params) {
    this.logPrefix = ".init(" + params.column.getId() + "): ";
    this.log("Starting...");
    this.loggingEnabled = params.context.EnableConsoleLogging;
    this.paramValue = params.value;
    this.container = document.createElement("div");
    this.input = document.createElement("p");
    this.input.className = "ag-cell-value";
    this.input.innerHTML = JSON.stringify(this.paramValue);
    this.container.appendChild(this.input);
  }
  getGui() {
    return this.container;
  }
}
