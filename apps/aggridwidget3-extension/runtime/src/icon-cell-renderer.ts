import { writeWidgetLog } from "./widget-logging";
const document: any = globalThis.document;

export class CustomIconCellRenderer {
  loggingEnabled: boolean;
  logPrefix: string;
  params: any;
  paramValue: any;
  container: any;

  constructor() {
    this.loggingEnabled = true;
  }
  log(message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomIconCellRenderer",
      this.logPrefix,
      message,
    );
  }
  init(params) {
    this.logPrefix = `.init(${params.column.getId()}): `;
    this.params = params;
    this.paramValue = params.value;
    this.loggingEnabled = this.params.context.EnableConsoleLogging;
    this.log(`Starting, this.paramValue = ${JSON.stringify(this.paramValue)}`);
    const stateField = this.params.stateField;
    const stateFieldValue = this.params.data[stateField];
    const iconMap = this.params.iconMap;
    const suffixImageField = this.params.suffixImage;
    const suffixMap = this.params.suffixMap;
    const suffixImageFieldValue = this.params.data[suffixImageField];
    const rowHeight = this.params.context.RowHeight;
    this.log(`stateField = ${stateField} = ${stateFieldValue}`);
    this.log(`iconMap = ${JSON.stringify(iconMap)}`);
    this.log(
      `suffixImageField = ${suffixImageField} = ${suffixImageFieldValue}`,
    );
    this.log(`suffixMap = ${JSON.stringify(suffixMap)}`);
    this.container = document.createElement("div");
    this.container.style.justifyContent = "center";
    this.container.style.display = "flex";
    this.container.style.height = `${rowHeight}px`;
    this.container.style.alignItems = "center";
    this.container.style.width = "fit-content";
    this.container.className = "customtreecolumncellrenderer";
    const imageBeforeTextMedia = document.createElement("img");
    imageBeforeTextMedia.alt = "Node image";
    imageBeforeTextMedia.style.height = "var(--grid-font-size)";
    imageBeforeTextMedia.style.width = "var(--grid-font-size)";
    imageBeforeTextMedia.className =
      "customtreecolumncellrenderer-imageBeforeTextMedia";
    const imageBeforeTextLink = document.createElement("img");
    imageBeforeTextLink.style.height = "100%";
    imageBeforeTextLink.style.width = `${rowHeight}px`;
    imageBeforeTextLink.className =
      "ustomtreecolumncellrenderer-imageBeforeTextLink";
    const imageAfterText = document.createElement("img");
    imageAfterText.style.height = "var(--grid-font-size)";
    imageAfterText.style.width = "var(--grid-font-size)";
    imageAfterText.className = "customtreecolumncellrenderer-imageAfterText";
    const text = document.createElement("span");
    text.textContent = this.paramValue;
    text.style.display = "flex";
    text.style.verticalAlign = "middle";
    text.style.margin = "0px 8px";
    if (stateFieldValue && iconMap) {
      imageBeforeTextMedia.src =
        `/Thingworx/MediaEntities/${iconMap[stateFieldValue]}`;
    } else if (stateFieldValue) {
      imageBeforeTextLink.src = stateFieldValue;
    }
    if (suffixImageFieldValue && suffixMap) {
      imageAfterText.src =
        `/Thingworx/MediaEntities/${suffixMap[suffixImageFieldValue]}`;
    }
    if (stateFieldValue && iconMap) {
      this.container.appendChild(imageBeforeTextMedia);
    } else if (stateFieldValue) {
      this.container.appendChild(imageBeforeTextLink);
    }
    this.container.appendChild(text);
    if (suffixImageFieldValue && suffixMap) {
      this.container.appendChild(imageAfterText);
    }
  }
  getGui() {
    return this.container;
  }
}
