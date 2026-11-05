import { writeWidgetLog } from "./widget-logging";
// Recovered from webpack module 2531.
import { icon } from "@fortawesome/fontawesome-svg-core";
import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import { FileUploader } from "./file-uploader";

export class CustomImageRenderer {
  loggingEnabled: boolean;
  fileUploader: FileUploader;
  buttonSpinnerElement: Element;
  now: string;
  log(prefix: string, message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomImageRenderer()",
      prefix,
      message,
    );
  }
  createImageSpan(): HTMLDivElement {
    const resultElement = document.createElement("div");
    resultElement.style.height = "100%";
    resultElement.style.display = "flex";
    resultElement.style.justifyContent = "center";
    resultElement.style.alignContent = "center";
    if (this.params) {
      this.imageElement = document.createElement("img");
      this.imageElement.src = this.params.value;
      this.imageElement.alt = "File not found";
      this.imageElement.onerror = () => {
        return (this.imageElement.style.display = "none");
      };
      this.imageElement.style.height = "100%";
      this.imageElement.style.position = "absolute";
      if (this.params?.imgStyle !== undefined) {
        resultElement.style.cssText = this.params.imgStyle;
      }
      resultElement.appendChild(this.imageElement);
    }
    return resultElement;
  }
  params: any;
  imageElement: HTMLImageElement;
  checkIfFieldIsEditable(): void {
    const _this = this;
    const logPrefix = ".checkIfFieldIsEditable(" + this.now + "): ";
    const colDef = this.params.colDef;
    this.log(
      logPrefix,
      "image cell was double-clicked, columndef = " + JSON.stringify(colDef),
    );
    let isEditable = undefined;
    if (colDef?.editable !== undefined) {
      const editableValue = colDef.editable;
      this.log(logPrefix, "next column has editable, value = " + editableValue);
      if (editableValue?.canEdit !== undefined) {
        const drivingField_1 = editableValue.canEdit;
        isEditable = function () {
          return _this.params.data[drivingField_1];
        };
      } else {
        isEditable =
          editableValue?.useEditableFields !== undefined
            ? function () {
                let fieldIsEditable = true;
                const editableFieldName =
                  _this.params.context.EditableFieldsColumn;
                _this.log(
                  logPrefix,
                  "checking to see if this cell is editable using field: " +
                    editableFieldName,
                );
                const editableFields = _this.params.data[editableFieldName]
                  ? JSON.parse(_this.params.data[editableFieldName])
                  : {};
                _this.log(
                  logPrefix,
                  "editableFields = " + JSON.stringify(editableFields),
                );
                _this.log(
                  logPrefix,
                  "does it have this column?: = " + colDef.field,
                );
                if (editableFields?.[colDef.field] !== undefined) {
                  fieldIsEditable = editableFields[colDef.field];
                  _this.log(logPrefix, "Yes! And it = " + fieldIsEditable);
                }
                return fieldIsEditable;
              }
            : colDef.editable;
      }
      if (isEditable) {
        this.log(
          logPrefix,
          "this field is editable, switching to upload button...",
        );
        this.switchToUploadButton();
      }
    }
  }
  async startUpload(e): Promise<void> {
    const logPrefix = ".startUpload(" + this.now + "): ";
    this.eGui.replaceChild(this.buttonSpinnerElement, this.fileContainer);
    try {
      const newFilePath = await this.fileUploader.startUpload(
        e,
        this.params,
        this.now,
      );
      this.log(logPrefix, logPrefix + "upload complete");
      const colId = this.params.column.getColId();
      this.params.node.setDataValue(colId, newFilePath);
      this.imageElement.src = newFilePath;
      this.eGui.replaceChild(this.fileContainer, this.buttonSpinnerElement);
      this.log(logPrefix, logPrefix + "renderer update done");
    } catch (error) {
      this.log(logPrefix, logPrefix + "failure uploading...");
      this.eGui.replaceChild(this.imageElement, this.fileContainer);
    }
  }
  eGui: any;
  openFileInput(): void {
    this.log(".openFileInput(): ", ".openFileInput(): button clicked....");
    this.inputElement.dispatchEvent(new Event("click"));
    this.inputElement.click();
  }
  inputElement: any;
  buttonDiv: HTMLDivElement;
  inputDiv: HTMLDivElement;
  buttonElement: HTMLElement;
  fileContainerDiv: HTMLDivElement;
  constructor() {
    const _this = this;
    this.loggingEnabled = true;
    this.fileUploader = new FileUploader();
    this.buttonSpinnerElement = icon(faSpinner, {
      classes: ["fa-spin"],
    }).node[0];
    this.now =
      new Date().getTime() +
      Math.floor(100000 + Math.random() * 900000)
        .toString()
        .substring(0, 4);
    this.openFileInput = this.openFileInput.bind(this);
    this.startUpload = this.startUpload.bind(this);
    this.checkIfFieldIsEditable = this.checkIfFieldIsEditable.bind(this);
  }
  switchToUploadButton() {
    this.log(
      ".switchToUploadButton(): ",
      ".switchToUploadButton(): swithing to upload button...",
    );
    this.eGui.replaceChild(this.fileContainer, this.imageElement);
  }
  fileContainer: HTMLElement;
  init(params) {
    this.now;
    this.params = params;
    this.loggingEnabled = this.params.context.EnableConsoleLogging;
    this.params.allow = params.allow ?? "/*";
    this.params.uploadThumbnail = params.uploadThumbnail ?? false;
    this.params.repository = params.repository ?? false;
    if (params.value) {
      const fileSplit = params.value.split("/");
      const currentFilePath =
        fileSplit.length > 3
          ? "/" + fileSplit.slice(4, fileSplit.length - 1).join("/")
          : "/Ideas";
      this.params.newFilePath = params.newFilePath ?? currentFilePath;
      this.params.repository =
        params.repository ??
        (fileSplit.length > 3 ? fileSplit[3] : "SystemRepository");
    } else {
      this.params.newFilePath = "/Ideas";
    }
    this.fileContainerDiv = document.createElement("div");
    this.fileContainerDiv.setAttribute("tabindex", "0");
    this.fileContainerDiv.classList.add("outer-div");
    this.buttonDiv = document.createElement("div");
    this.buttonDiv.classList.add("button-div");
    this.inputDiv = document.createElement("div");
    this.inputDiv.classList.add("file-input");
    this.buttonElement = document.createElement("btn");
    this.buttonElement.setAttribute("id", this.now + "btn");
    this.buttonElement.setAttribute("name", "fileUploadButton");
    this.buttonElement.classList.add("button-13");
    this.buttonElement.addEventListener("click", this.openFileInput);
    this.buttonElement.textContent = "Upload";
    this.buttonDiv.appendChild(this.buttonElement);
    this.fileContainerDiv.appendChild(this.buttonDiv);
    this.inputElement = document.createElement("input");
    this.inputElement.setAttribute("id", this.now + "input");
    this.inputElement.setAttribute("type", "file");
    this.inputElement.setAttribute("allow", "image/*");
    this.inputElement.addEventListener("change", this.startUpload);
    this.inputDiv.appendChild(this.inputElement);
    this.fileContainerDiv.appendChild(this.inputDiv);
    this.eGui = this.createImageSpan();
    this.eGui.addEventListener("dblclick", this.checkIfFieldIsEditable);
  }
  getGui() {
    this.log(
      ".getGui():",
      "created eGui, adding dblclick event listener, eGui = " +
        this.eGui.outerHTML,
    );
    return this.eGui;
  }
  getValue() {
    return this.imageElement.src;
  }
}
