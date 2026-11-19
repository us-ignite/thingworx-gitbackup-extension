import { writeWidgetLog } from "./widget-logging";
const document: any = globalThis.document;

export class CustomPasswordEditor {
  loggingEnabled: boolean;
  containerId: string;
  paramValue: any;
  cellHeight: string;
  container: any;
  input: any;
  logPrefix: string;

  constructor() {
    this.loggingEnabled = true;
    const now = new Date().getTime();
    this.containerId = (now + 1).toString();
  }
  log(message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomPasswordEditor",
      this.logPrefix,
      message,
    );
  }
  init(params) {
    this.logPrefix = `.init(${params.column.getId()}): `;
    this.loggingEnabled = params.context.EnableConsoleLogging;
    this.log("Starting...");
    this.paramValue = params.value;
    this.cellHeight = "100%";
    this.container = document.createElement("div");
    this.input = document.createElement("input");
    this.input.id = "input";
    this.input.type = "password";
    this.input.classList.add("ag-cell", "ag-text-field-input");
    this.input.value = this.paramValue ? this.paramValue : "";
    this.container.appendChild(this.input);
    this.input.addEventListener("input", (event) => {
      this.paramValue = event.target.value;
    });
  }
  getGui() {
    return this.container;
  }
  afterGuiAttached() {
    this.input.focus();
  }
  getValue() {
    return this.paramValue;
  }
  destroy() {}
  isPopup() {
    return false;
  }
}
