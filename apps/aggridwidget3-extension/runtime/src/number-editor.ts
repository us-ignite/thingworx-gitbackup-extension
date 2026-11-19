import { writeWidgetLog } from "./widget-logging";
const document: any = globalThis.document;

export class CustomNumberEditor {
  integerOnly: boolean;
  loggingEnabled: boolean;
  container: any;
  input: any;
  cancelBeforeStart: boolean;
  logPrefix: string;

  constructor() {
    this.integerOnly = false;
    this.loggingEnabled = true;
  }
  log(message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomNumberEditor",
      this.logPrefix,
      message,
    );
  }
  init(params) {
    this.logPrefix = `.init(${params.column.getId()}): `;
    this.loggingEnabled = params.context.EnableConsoleLogging;
    if (params.hasOwnProperty("integersOnly")) {
      this.integerOnly = params.integersOnly;
      this.log(`params has integersOnly = ${this.integerOnly}`);
    }
    this.container = document.createElement("div");
    this.input = document.createElement("input");
    this.input.classList.add("ag-cell", "numeric-input");
    this.input.type = "number";
    if (this.isCharNumeric(params.eventKey)) {
      this.input.value = params.eventKey;
    } else if (params.value != null) {
      this.input.value = params.value;
    }
    this.input.addEventListener("keydown", (event) => {
      this.input.classList.remove("invalid");
    });
    this.input.addEventListener("keypress", (event) => {
      if (this.isKeyPressedNumeric(event)) {
        if (this.isKeyPressedNavigation(event)) {
          event.stopPropagation();
        }
      } else {
        this.input.focus();
        if (event.preventDefault) {
          event.preventDefault();
        }
      }
    });
    this.input.addEventListener("keyup", (event) => {
      const regex = new RegExp(/^-?\d{0,}(\.\d*)?(e\B)?(\d+)$/);
      if (this.isKeyPressedNavigation(event)) {
        event.stopPropagation();
      } else {
        regex.test(this.input.value);
      }
    });
    const charPressIsNotANumber =
      params.eventKey && "-1234567890.e".indexOf(params.eventKey) < 0;
    this.cancelBeforeStart = !!charPressIsNotANumber;
  }
  isKeyPressedNavigation(event) {
    return event.key === "ArrowLeft" || event.key === "ArrowRight";
  }
  getGui() {
    return this.input;
  }
  afterGuiAttached() {
    this.input.focus();
  }
  isCancelBeforeStart() {
    return this.cancelBeforeStart;
  }
  isCancelAfterEnd() {
    return !new RegExp(/^-?\d{0,}(\.\d*)?(e\B)?(\d+)$/).test(this.input.value);
  }
  getValue() {
    const inputVal = this.input.value;
    this.log(
      `inputVal = ${inputVal}, this.integerOnly = ${this.integerOnly}`,
    );
    if (this.integerOnly) {
      return parseInt(inputVal);
    } else {
      return parseFloat(inputVal);
    }
  }
  destroy() {}
  isPopup() {
    return false;
  }
  isCharNumeric(charStr) {
    const inputStr = this.getValue() + charStr;
    this.log(
      `isCharNumeric, inputStr = ${inputStr}, this.integerOnly = ${this.integerOnly}`,
    );
    const allowedInput = this.integerOnly ? "1234567890-e" : "1234567890-.e";
    let flag = false;
    if (charStr && !(allowedInput.indexOf(charStr) < 0)) {
      const matchedChar = this.integerOnly
        ? inputStr.match(/(-+)(e+)/g)
        : inputStr.match(/(-+)|(\.+)|(e+)/g);
      if (matchedChar) {
        const countedChar = matchedChar.reduce(function (allChar, chars) {
          let _a;
          const currCount = allChar[chars] ?? 0;
          return Object.assign({}, allChar, { [chars]: currCount + 1 });
        }, {});
        if (
          !(countedChar["-"] > 1) &&
          !(countedChar["."] > 1) &&
          !(countedChar.e > 1)
        ) {
          flag = true;
        }
      } else {
        flag = true;
      }
    }
    return flag;
  }
  isKeyPressedNumeric(event) {
    const charStr = event.key;
    return this.isCharNumeric(charStr);
  }
  isNumericKey(event) {
    const charStr = event.key;
    return this.isCharNumeric(charStr);
  }
}
