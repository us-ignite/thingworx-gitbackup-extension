import { writeWidgetLog } from "./widget-logging";
// Recovered from webpack module 9880.
// Recovered from webpack module 9880.
// Recovered from webpack module 9880.
// Recovered from webpack module 9880.
// Recovered from webpack module 9880.
// Recovered from webpack module 9880.
// Recovered from webpack module 9880.
// Recovered from webpack module 9880.
export class CustomColumnAggregateFooterRenderer {
  loggingEnabled: boolean;
  log(message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomColumnAggregateFooterRenderer",
      this.logPrefix,
      message,
    );
  }
  logPrefix: string;
  params: any;
  container: HTMLDivElement;
  labelSpan: HTMLSpanElement;
  constructor() {
    this.loggingEnabled = true;
  }
  init(params) {
    this.logPrefix = ".init(" + params.column.getId() + "): ";
    this.log("Starting...");
    this.params = params;
    this.loggingEnabled = this.params.context.EnableConsoleLogging;
    this.container = document.createElement("div");
    const footerParams = params.colDef?.footerCellRendererParams;
    if (footerParams) {
      this.log("next column has footerParams: " + JSON.stringify(footerParams));
      if (
        footerParams.purpose !== undefined &&
        footerParams.label !== undefined
      ) {
        this.labelSpan = document.createElement("span");
        this.labelSpan.innerHTML = footerParams.label;
        this.container.appendChild(this.labelSpan);
      }
      if (footerParams.cellClass !== undefined) {
        this.container.setAttribute("class", footerParams.cellClass);
      }
    }
    this.log("done, this.container = " + this.container.innerHTML);
  }
  getGui() {
    return this.container;
  }
  getValue() {}
}
