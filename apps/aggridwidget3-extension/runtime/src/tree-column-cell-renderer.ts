import { writeWidgetLog } from "./widget-logging";
const document: any = globalThis.document;

export class CustomTreeColumnCellRenderer {
  loggingEnabled: boolean;
  params: any;
  container: any;
  logPrefix: string;

  constructor() {
    this.loggingEnabled = true;
  }
  log(message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomTreeColumnCellRenderer",
      this.logPrefix,
      message,
    );
  }
  init(params) {
    this.logPrefix = ".init(): ";
    this.params = params;
    this.loggingEnabled = this.params.context.EnableConsoleLogging;
    const treeColumnValue = params.data[params.context.TreeColumnField];
    this.log(`Starting, treeColumnValue = ${JSON.stringify(treeColumnValue)}`);
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
    this.container.style.justifyContent = "flex-start";
    this.container.style.display = "flex";
    this.container.style.height = `${rowHeight}px`;
    this.container.style.alignItems = "center";
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
      "customtreecolumncellrenderer-imageBeforeTextLink";
    const imageAfterText = document.createElement("img");
    imageAfterText.className = "customtreecolumncellrenderer-imageAfterText";
    if (suffixImageField === "isFavorite" && suffixImageFieldValue) {
      imageAfterText.style.height = "16px";
      imageAfterText.style.width = "16px";
    } else {
      imageAfterText.style.height = "var(--grid-font-size)";
      imageAfterText.style.width = "var(--grid-font-size)";
    }
    const text = document.createElement("span");
    text.textContent = treeColumnValue;
    text.style.display = "flex";
    text.style.verticalAlign = "middle";
    text.style.margin = "0px 8px";
    if (stateFieldValue && iconMap) {
      const iconPath = iconMap[stateFieldValue] || iconMap.default || "";
      imageBeforeTextMedia.src = `/Thingworx/MediaEntities/${iconPath}`;
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
