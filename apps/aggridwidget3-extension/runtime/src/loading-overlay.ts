import { writeWidgetLog } from "./widget-logging";
const document: any = globalThis.document;

export class CustomLoadingOverlay {
  loggingEnabled: boolean;
  eGui: any;
  logPrefix: string;
  constructor() {
    this.loggingEnabled = true;
  }
  log(message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomLoadingOverlay",
      this.logPrefix,
      message,
    );
  }

  init(params) {
    this.logPrefix = ".init(): ";
    this.log(
      `Starting, creating overlay with message: ${params.loadingMessage}`,
    );
    this.eGui = document.createElement("div");
    this.eGui.innerHTML = `<div class="ag-overlay-loading-center" role="presentation">
               <div role="presentation" style="height:100px; width:100px; center / contain no-repeat; margin: 0 auto;"></div>
               <div aria-live="polite" aria-atomic="true">${params.loadingMessage}</div>
            </div>`;
    this.log("done...");
  }
  getGui() {
    return this.eGui;
  }
  refresh(params) {
    return false;
  }
}
