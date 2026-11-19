import { writeWidgetLog } from "./widget-logging";
import { AG_GRID_ROW_ID_FIELD } from "./grid-data";
import { CustomImageRenderer } from "./image-renderer";
import { FileUploader } from "./file-uploader";
import { icon } from "@fortawesome/fontawesome-svg-core";
import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import { CustomSelectEditor } from "./select-editor";
import { CustomCheckboxRenderer } from "./checkbox-renderer";
const document: any = globalThis.document;

export class CustomNewRowToolPanel {
  loggingEnabled: boolean;
  NOW: string;
  imgIds: {};
  fileUploader: FileUploader;
  buttonSpinnerElement: Element;
  eGui: any;
  params: Record<string, any>;

  constructor() {
    this.loggingEnabled = true;
    this.NOW = `${new Date().getTime()}`;
    this.imgIds = {};
    this.fileUploader = new FileUploader();
    this.buttonSpinnerElement = icon(
      faSpinner,
      {
        classes: ["fa-spin"],
      },
    ).node[0];
  }
  log(prefix: string, message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomNewRowToolPanel()",
      prefix,
      message,
    );
  }
  async startUpload(e, containerId, imgId) {
    const logPrefix = `.startUpload(${this.NOW}): `;
    this.log(
      logPrefix,
      `${logPrefix}getting containerElement with id = ${containerId}`,
    );
    this.log(logPrefix, `${logPrefix}getting imageelement with id = ${imgId}`);
    const containerElement = document.getElementById(containerId);
    Array.from(containerElement.children).forEach((nextNode) => {
      this.log(
        logPrefix,
        `${logPrefix}nextNode ${(nextNode as any).id} = ${(nextNode as any).innerHTML}`,
      );
    });
    const imgElement = document.getElementById(imgId);
    containerElement.replaceChild(this.buttonSpinnerElement, imgElement);
    try {
      const newFilePath = await this.fileUploader.startUpload(
        e,
        this.params,
        imgId,
      );
      this.log(logPrefix, `${logPrefix}upload complete`);
      const colId = this.params.column.getColId();
      this.params.node.setDataValue(colId, newFilePath);
      imgElement.src = newFilePath;
      containerElement.replaceChild(imgElement, this.buttonSpinnerElement);
      this.log(logPrefix, `${logPrefix}renderer update done`);
    } catch (error) {
      this.log(logPrefix, `${logPrefix}failure uploading...`);
      containerElement.replaceChild(imgElement, this.buttonSpinnerElement);
    }
  }

  openFileInput(e, fileInputId) {
    this.log(
      ".openFileInput(): ",
      `.openFileInput(): button clicked, opening file input element with id: ${fileInputId}`,
    );
    const fileInputElement = document.getElementById(fileInputId);
    fileInputElement.dispatchEvent(new Event("click"));
    fileInputElement.click();
  }
  init(params) {
    this.log(".init(): ", "Starting...");
    this.params = params;
    this.loggingEnabled = this.params.context.EnableConsoleLogging;
    this.params.allow = params.hasOwnProperty("allow") ? params.allow : "/*";
    this.params.uploadThumbnail =
      !!params.hasOwnProperty("uploadThumbnail") && params.uploadThumbnail;
    this.params.repository = params.hasOwnProperty("repository")
      ? params.repository
      : "SystemRepository";
    this.params.newFilePath = params.hasOwnProperty("newFilePath")
      ? params.newFilePath
      : "/Ideas";
    this.eGui = document.createElement("div");
    this.eGui.setAttribute("class", "multiEdit");
    params.api.addEventListener("modelUpdated", () => {
      this.eGui.innerHTML = this.buildEditElems();
      const fieldsRequired = params.fieldsRequired;
      const fieldDefaults = params.fieldDefaults;
      const newRowSaveButton = document.getElementById(
        `newRowSaveButton${this.NOW}`,
      );
      const newRowCancelButton = document.getElementById(
        `newRowCancelButton${this.NOW}`,
      );
      newRowSaveButton.addEventListener("click", (e) => {
        this.log(".init(): ", "newRowSaveButton clicked...");
        const missingRequiredFieldNames = [];
        let missingRequiredFields = false;
        const newRow = {};
        const gridIds = [];
        params.api.forEachNode(function (nextNode) {
          return gridIds.push(parseInt(nextNode.id));
        });
        newRow[AG_GRID_ROW_ID_FIELD] = Math.max.apply(Math, gridIds) + 1;
        this.log(
          ".init(): ",
          `current grid ids: ${gridIds}, new row id = ${newRow[AG_GRID_ROW_ID_FIELD]}`,
        );
        document
          .querySelectorAll('[id^="newRowField_"]')
          .forEach((nextEl) => {
            this.log(
              ".init(): ",
              `next field: ${nextEl.name} has a type of ${nextEl.type}`,
            );
            this.log(
              ".init(): ",
              `setting next field: ${nextEl.name} to ${nextEl.type == "checkbox" ? nextEl.checked : nextEl.value}`,
            );
            if (
              !!fieldsRequired.includes(nextEl.name) &&
              (!nextEl.value || nextEl.value.trim() == "")
            ) {
              missingRequiredFields = true;
              missingRequiredFieldNames.push(nextEl.name);
            }
            newRow[nextEl.name] =
              nextEl.type == "checkbox" ? nextEl.checked : nextEl.value;
          });
        if (missingRequiredFields) {
          const alertMessage =
            `Please provide a value for all the following required fields: ${missingRequiredFieldNames.join("; ")}`;
          params.context.emitStatusMessage("error", alertMessage);
        } else {
          this.log(
            ".init(): ",
            `newRow = ${JSON.stringify(newRow)}, applying any defaults`,
          );
          if (fieldDefaults) {
            Object.keys(fieldDefaults).forEach((nextDefault) => {
              if (!newRow.hasOwnProperty(nextDefault) || !newRow[nextDefault]) {
                this.log(
                  ".init(): ",
                  `adding nextDefault ${nextDefault} of: ${fieldDefaults[nextDefault]}`,
                );
                newRow[nextDefault] = fieldDefaults[nextDefault];
              }
            });
            this.log(
              ".init(): ",
              `after applying defaults, newRow = ${JSON.stringify(newRow)}`,
            );
          }
          params.api.applyTransaction({
            add: [newRow],
            addIndex: 0,
          });
          params.context.updateEditedTable([newRow]);
          params.api.closeToolPanel();
        }
      });
      newRowCancelButton.addEventListener("click", (e) => {
        this.log(".init(): ", "newRowCancelButton clicked...");
        params.api.closeToolPanel();
        document
          .querySelectorAll('[id^="newRowField_"]')
          .forEach(function (nextEl) {
            nextEl.value = "";
          });
      });
      Object.keys(this.imgIds).forEach((nextField) => {
        this.log(
          ".init(): ",
          `.init(): adding event listeners for next image field: ${nextField}`,
        );
        const nextImagesIds = this.imgIds[nextField];
        const buttonElem = document.getElementById(nextImagesIds.buttonId);
        const fileInputElem = document.getElementById(
          nextImagesIds.fileInputId,
        );
        buttonElem.addEventListener("click", (e) => {
          return this.openFileInput(e, nextImagesIds.fileInputId);
        });
        fileInputElem.addEventListener("change", (e) => {
          return this.startUpload(
            e,
            nextImagesIds.containerId,
            nextImagesIds.imgId,
          );
        });
      });
    });
  }
  getGui() {
    return this.eGui;
  }
  refresh() {}
  buildEditElems() {
    const logPrefix = ".buildEditElems(): ";
    this.log(logPrefix, "Starting...");
    const now = new Date().getTime();
    const fieldsToDisplay = this.params.fieldsToDisplay;
    const fieldDefinitions = this.params.fieldDefinitions;
    const fieldsRequired = this.params.fieldsRequired;
    const panelTitle = this.params.panelTitle;
    const gridConfig = this.params.context.getGridConfig();
    this.log(logPrefix, `fieldsToDisplay = ${JSON.stringify(fieldsToDisplay)}`);
    const returnDiv = document.createElement("div");
    const headerLabelDiv = document.createElement("div");
    headerLabelDiv.className = "center";
    const headerButtonDiv = document.createElement("div");
    headerButtonDiv.className = "center";
    const headerLabel = document.createElement("h3");
    headerLabel.innerHTML = panelTitle || "New row values:";
    headerLabelDiv.appendChild(headerLabel);
    const applyButton = document.createElement("button");
    applyButton.id = `newRowSaveButton${this.NOW}`;
    applyButton.className = "ptc-ok-button";
    applyButton.title = "Save new Row";
    const cancelButton = document.createElement("button");
    cancelButton.id = `newRowCancelButton${this.NOW}`;
    cancelButton.className = "ptc-close-button";
    cancelButton.title = "Cancel";
    headerButtonDiv.appendChild(applyButton);
    headerButtonDiv.appendChild(cancelButton);
    returnDiv.appendChild(headerLabelDiv);
    returnDiv.appendChild(headerButtonDiv);
    const horizRule = document.createElement("hr");
    horizRule.className = "editorDivider";
    returnDiv.appendChild(horizRule);
    const allAttributesDiv = document.createElement("div");
    Object.keys(fieldsToDisplay).forEach((nextField) => {
      this.log(logPrefix, `building edit element for ${nextField}`);
      const inputId =
        (now + 1).toString() +
        Math.floor(100000 + Math.random() * 900000)
          .toString()
          .substring(0, 4);
      const buttonId =
        (now + 1).toString() +
        Math.floor(100000 + Math.random() * 900000)
          .toString()
          .substring(0, 4);
      const imgId =
        (now + 1).toString() +
        Math.floor(100000 + Math.random() * 900000)
          .toString()
          .substring(0, 4);
      const nextFieldDivId =
        (now + 1).toString() +
        Math.floor(100000 + Math.random() * 900000)
          .toString()
          .substring(0, 4);
      const nextFieldRowDiv = document.createElement("div");
      nextFieldRowDiv.setAttribute("class", "newUserFieldRow");
      nextFieldRowDiv.setAttribute("id", nextFieldDivId);
      const nextFieldLabelDiv = document.createElement("div");
      nextFieldLabelDiv.className = "newUserField";
      const nextFieldLabel = document.createElement("label");
      nextFieldLabel.innerHTML =
        (fieldsRequired.includes(nextField)
          ? '<span style="color: red;">* </span>'
          : "") + fieldsToDisplay[nextField];
      nextFieldLabelDiv.appendChild(nextFieldLabel);
      nextFieldRowDiv.appendChild(nextFieldLabelDiv);
      const nextFieldInputDiv = document.createElement("div");
      nextFieldInputDiv.className = "newUserValue";
      let nextFieldCellEditor;
      const nextFieldColumnConfig = gridConfig.ColumnConfig[nextField];
      this.log(
        logPrefix,
        `nextFieldColumnConfig = ${JSON.stringify(nextFieldColumnConfig)}`,
      );
      if (nextFieldColumnConfig?.hasOwnProperty("cellEditor")) {
        nextFieldCellEditor = nextFieldColumnConfig.cellEditor;
      }
      if (
        nextFieldColumnConfig?.hasOwnProperty("cellRenderer") &&
        (nextFieldColumnConfig.cellRenderer == "CustomCheckboxRenderer" ||
          nextFieldColumnConfig.cellRenderer == "CustomImageRenderer")
      ) {
        this.log(
          logPrefix,
          `nextFieldColumnConfig is a checkbox or image renderer!: ${nextFieldColumnConfig.cellRenderer}`,
        );
        nextFieldCellEditor = nextFieldColumnConfig.cellRenderer;
      }
      if (nextFieldCellEditor) {
        this.log(logPrefix, `it has a cellEditor of ${nextFieldCellEditor}`);
        switch (nextFieldCellEditor) {
          case "CustomSelectEditor":
            const selectOptions = nextFieldColumnConfig.cellEditorParams.values;
            const selectMultiple =
              !!nextFieldColumnConfig.cellEditorParams.hasOwnProperty(
                "multiple",
              ) && nextFieldColumnConfig.cellEditorParams.multiple;
            const attributeSelectElem_1 = document.createElement("select");
            attributeSelectElem_1.className = "vertical-middle";
            let selectHeight_1 = 0;
            const option = document.createElement("option");
            option.value = "";
            option.text = "";
            attributeSelectElem_1.appendChild(option);
            selectHeight_1++;
            selectOptions.forEach((nextOption) => {
              this.log(
                logPrefix,
                `adding next select option of ${nextOption}`,
              );
              const option = document.createElement("option");
              option.value = nextOption;
              option.text = nextOption;
              attributeSelectElem_1.appendChild(option);
              selectHeight_1++;
            });
            attributeSelectElem_1.multiple = selectMultiple;
            attributeSelectElem_1.size = selectMultiple ? selectHeight_1 : 0;
            attributeSelectElem_1.id = `newRowField_${nextField}`;
            attributeSelectElem_1.name = nextField;
            nextFieldInputDiv.appendChild(attributeSelectElem_1);
            break;
          case "CustomImageRenderer":
            const inputDiv = document.createElement("div");
            inputDiv.classList.add("file-input");
            const fileButtonElement = document.createElement("btn");
            fileButtonElement.setAttribute("id", buttonId);
            fileButtonElement.setAttribute("name", "fileUploadButton");
            fileButtonElement.classList.add("button-13");
            fileButtonElement.textContent = "Upload";
            const fileInputElement = document.createElement("input");
            fileInputElement.setAttribute("id", inputId);
            fileInputElement.setAttribute("name", "fileUploadInput");
            fileInputElement.setAttribute("type", "file");
            fileInputElement.setAttribute(
              "allow",
              nextFieldColumnConfig.cellRendererParams?.hasOwnProperty("allow")
                ? nextFieldColumnConfig.cellRendererParams.allow
                : "/*",
            );
            inputDiv.appendChild(fileInputElement);
            const imageElement = document.createElement("img");
            imageElement.setAttribute("id", `newRowField_${imgId}`);
            imageElement.style.height = "100%";
            imageElement.style.position = "absolute";
            nextFieldInputDiv.appendChild(inputDiv);
            nextFieldInputDiv.appendChild(fileButtonElement);
            nextFieldInputDiv.appendChild(imageElement);
            this.imgIds[nextField] = {
              buttonId: `${buttonId}`,
              fileInputId: `${inputId}`,
              containerId: `${nextFieldDivId}`,
              imgId: `newRowField_${imgId}`,
            };
            break;
          case "CustomCheckboxRenderer":
            const checkboxInputElem = document.createElement("input");
            checkboxInputElem.className = "vertical-middle";
            checkboxInputElem.type = "checkbox";
            checkboxInputElem.id = `newRowField_${nextField}`;
            checkboxInputElem.name = nextField;
            nextFieldInputDiv.appendChild(checkboxInputElem);
            break;
          default:
            let textInput;
            (textInput = document.createElement("input")).className =
              "vertical-middle";
            textInput.type = "text";
            textInput.id = `newRowField_${nextField}`;
            textInput.name = nextField;
            nextFieldInputDiv.appendChild(textInput);
        }
        nextFieldRowDiv.appendChild(nextFieldInputDiv);
      } else if (fieldDefinitions.hasOwnProperty(nextField)) {
        switch (fieldDefinitions[nextField].baseType) {
          case "DATETIME":
            const dateInput = document.createElement("input");
            dateInput.className = "vertical-middle";
            dateInput.type = "date";
            dateInput.id = `newRowField_${nextField}`;
            dateInput.name = nextField;
            nextFieldInputDiv.appendChild(dateInput);
            break;
          case "IMAGELINK":
            break;
          default:
            let textInput;
            (textInput = document.createElement("input")).className =
              "vertical-middle";
            textInput.type = "text";
            textInput.id = `newRowField_${nextField}`;
            textInput.name = nextField;
            nextFieldInputDiv.appendChild(textInput);
            nextFieldRowDiv.appendChild(nextFieldInputDiv);
        }
        nextFieldRowDiv.appendChild(nextFieldInputDiv);
      }
      allAttributesDiv.appendChild(nextFieldRowDiv);
    });
    returnDiv.appendChild(allAttributesDiv);
    return returnDiv.innerHTML;
  }
}
