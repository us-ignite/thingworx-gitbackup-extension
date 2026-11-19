import { writeWidgetLog } from "./widget-logging";
import { icon } from "@fortawesome/fontawesome-svg-core";
import { faExternalLinkAlt } from "@fortawesome/free-solid-svg-icons";
const document: any = globalThis.document;

export class CustomHyperlinkRenderer {
  logPrefix: string;
  loggingEnabled: boolean;
  arrowElement: Element;
  containerId: string;
  container: any;
  anchorLinkText: any;
  anchorElement: any;

  constructor() {
    this.loggingEnabled = true;
    this.arrowElement = icon(
      faExternalLinkAlt,
    ).node[0];
    const now = new Date().getTime();
    this.containerId = (now + 1).toString();
  }
  log(message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomHyperlinkRenderer()",
      this.logPrefix,
      message,
    );
  }

  init(params) {
    this.logPrefix = `.init(${params.column.getId()}): `;
    this.loggingEnabled = params.context.EnableConsoleLogging;
    this.container = document.createElement("div");
    this.container.setAttribute("id", this.containerId);
    this.container.setAttribute("tabindex", "0");
    this.container.style.textAlign = "left";
    this.container.style.verticalAlign = "middle";
    if (params.value) {
      this.log(`Starting, params.value = ${params.value}`);
      if (params.hasOwnProperty("linkText")) {
        this.log(`using linkText = ${params.linkText}`);
        this.anchorLinkText = params.linkText;
      }
      if (params.hasOwnProperty("linkFieldName")) {
        const linkFieldName = params.linkFieldName;
        this.log(`using linkFieldName = ${linkFieldName}`);
        this.anchorLinkText = params.data[linkFieldName]
          ? params.data[linkFieldName]
          : params.value;
      }
      let url: URL;
      try {
        url = new URL(String(params.value), document.baseURI);
      } catch (_error) {
        return;
      }
      if (url.protocol !== "https:" && url.protocol !== "http:") return;
      this.anchorElement = document.createElement("a");
      this.anchorElement.setAttribute("href", url.href);
      if (this.anchorLinkText) {
        this.log(`using anchorLinkText = ${this.anchorLinkText}`);
        this.anchorElement.textContent = String(this.anchorLinkText);
      } else {
        this.log("using open external link icon");
        this.anchorElement.appendChild(this.arrowElement);
      }
      this.anchorElement.target = "_blank";
      this.anchorElement.rel = "noopener noreferrer";
      this.container.appendChild(this.anchorElement);
      this.log(`this.container = ${this.container}`);
    }
  }
  getGui() {
    return this.container;
  }
}
