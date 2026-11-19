import { writeWidgetLog } from "./widget-logging";
const document: any = globalThis.document;

export class CustomPasswordRenderer {
  loggingEnabled: boolean;
  logPrefix: string;
  containerId: string;
  paramValue: any;
  container: any;
  input: any;

  constructor() {
    this.loggingEnabled = true;
    const now = new Date().getTime();
    this.containerId = (now + 1).toString();
  }
  log(message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomPasswordRenderer",
      this.logPrefix,
      message,
    );
  }
  init(params) {
    this.logPrefix = `.init(${params.column.getId()}): `;
    this.log("Starting...");
    this.loggingEnabled = params.context.EnableConsoleLogging;
    this.paramValue = params.value;
    this.container = document.createElement("div");
    this.input = document.createElement("p");
    this.input.className = "ag-cell-value";
    this.input.innerHTML = "*".repeat(this.paramValue?.length ?? 0);
    this.container.appendChild(this.input);
  }
  getGui() {
    return this.container;
  }
}
