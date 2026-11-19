import { writeWidgetLog } from "./widget-logging";
const document: any = globalThis.document;

export class CustomActionCellRenderer {
  loggingEnabled: boolean;
  clickedButton: any;
  params: any;
  outerDiv: any;
  innerDiv: any;
  iconButtons: any;

  constructor() {
    this.loggingEnabled = true;
  }
  log(prefix: string, message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomActionCellRenderer()",
      prefix,
      message,
    );
  }
  onButtonClick(e) {
    this.log(
      ".onButtonClick(): ",
      `this.clickedButton: ${e.currentTarget.title}`,
    );
    this.clickedButton = e.currentTarget.title;
    this.params.context.emitCustomActionButtonClick(this.clickedButton);
  }
  init(params) {
    const logPrefix = `.init(${params.column.getId()}): `;
    this.params = params;
    this.loggingEnabled = params.context.EnableConsoleLogging;
    this.log(logPrefix, "Starting...");
    const rowHeightPx =
      `${params.context.RowHeight ? params.context.RowHeight - 2 : 23}px`;
    this.outerDiv = document.createElement("div");
    this.outerDiv.className =
      "widget-bounding-box nonresponsive iqnox-widget template_IqnoxButtonWidget-GridButton";
    this.outerDiv.style.height = rowHeightPx;
    this.innerDiv = document.createElement("div");
    this.innerDiv.className = "widget-content widget-button-iqnox";
    this.innerDiv.style.height = "100%";
    this.innerDiv.style.display = "flex";
    this.iconButtons = params.icons;
    this.log(
      logPrefix,
      `this.iconButtons = ${JSON.stringify(this.iconButtons)}`,
    );
    if (this.iconButtons) {
      this.iconButtons.forEach((nextIcon) => {
        const buttonElement = document.createElement("button");
        buttonElement.setAttribute(
          "title",
          nextIcon.hasOwnProperty("title") ? nextIcon.title : "title",
        );
        if (!nextIcon.hasOwnProperty("title")) {
          console.log(
            'AgGridWidget.CustomActionCellRenderer(): Button "title" field is NOT set! ClickedActionCellButton = title',
          );
        }
        buttonElement.setAttribute(
          "style",
          nextIcon.hasOwnProperty("style") ? nextIcon.style : "",
        );
        if (nextIcon.hasOwnProperty("className")) {
          let buttonIcon = undefined;
          if (nextIcon.className.startsWith("fa")) {
            (buttonIcon = document.createElement("i")).className =
              nextIcon.className;
          } else if (nextIcon.className.startsWith("image")) {
            const buttonParams = nextIcon.className.split(":");
            console.log(
              `AgGridWidget.CustomActionCellRenderer():${params.data[buttonParams[1]]}`,
            );
            if (params.data[buttonParams[1]]) {
              this.outerDiv.className =
                "widget-bounding-box nonresponsive iqnox-widget template_IqnoxButtonWidget-GridImageButton";
              (buttonIcon = document.createElement("img")).src =
                params.data[buttonParams[1]];
            } else {
              buttonIcon = document.createElement("span");
              let className = "material-symbols-outlined";
              let textName = "info";
              if (buttonParams[2] && buttonParams[3]) {
                className = buttonParams[2];
                textName = buttonParams[3];
              }
              (buttonIcon = document.createElement("span")).className =
                className;
              buttonIcon.textContent = textName;
            }
            buttonIcon.alt = nextIcon["button-text"];
            buttonIcon.style = "height: 100%;width: auto;";
          } else if (nextIcon.className.startsWith("text")) {
            buttonIcon = document.createElement("span");
            const buttonParam = nextIcon.className.split(":")[1];
            buttonIcon.textContent = params.data[buttonParam];
          } else {
            (buttonIcon = document.createElement("span")).className =
              nextIcon.className.split(":")[0];
            buttonIcon.textContent = nextIcon.className.split(":")[1];
          }
          buttonElement.appendChild(buttonIcon);
        }
        if (nextIcon.hasOwnProperty("textContent")) {
          const buttonTextArea = document.createElement("span");
          buttonTextArea.className = "button-text";
          buttonTextArea.textContent = nextIcon.textContent
            ? nextIcon.textContent
            : "";
          buttonElement.appendChild(buttonTextArea);
        }
        buttonElement.addEventListener("click", (e) => {
          this.onButtonClick(e);
        });
        this.innerDiv.appendChild(buttonElement);
      });
    }
    this.outerDiv.appendChild(this.innerDiv);
  }
  getGui() {
    return this.outerDiv;
  }
}
