import { writeWidgetLog, writeWidgetLogObject } from "./widget-logging";
import { CustomSelectEditor } from "./select-editor";
import { CustomCheckboxRenderer } from "./checkbox-renderer";
const document: any = globalThis.document;

export class CustomMultiEditToolPanel {
  loggingEnabled: boolean;
  columnConfigs: {};
  eGui: any;

  constructor() {
    this.loggingEnabled = false;
    this.columnConfigs = {};
  }
  log(prefix: string, message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomMultiEditToolPanel()",
      prefix,
      message,
    );
  }
  logObj(prefix: string, message: unknown, object: unknown): void {
    writeWidgetLogObject(
      this.loggingEnabled,
      "AgGridWidget.CustomMultiEditToolPanel()",
      prefix,
      message,
      object,
    );
  }
  init(params) {
    this.log(".init(): ", "Starting...");
    this.loggingEnabled = params.context.EnableConsoleLogging;
    const gridConfig = params.context.getGridConfig();
    if (gridConfig.hasOwnProperty("ColumnConfig")) {
      this.columnConfigs = gridConfig.ColumnConfig;
    }
    this.eGui = document.createElement("div");
    this.eGui.className = "multiEdit";
    params.api.addEventListener("modelUpdated", () => {
      this.eGui.innerHTML = this.buildEditElems(params);
      const rowSelectedDiv = document.getElementById("rowSelectedDiv");
      if (params.api.getSelectedRows().length === 0) {
        rowSelectedDiv.style.display = "block";
        rowSelectedDiv.style.textAlign = "center";
      }
      params.api.addEventListener("selectionChanged", () => {
        const selectedRowCount = params.api.getSelectedRows().length;
        this.log(
          ".init(): ",
          `row selection changed, selectedRowCount = ${selectedRowCount}`,
        );
        if (selectedRowCount == 0) {
          this.log(".init(): ", "showing no row message");
          $(rowSelectedDiv).show();
        } else {
          this.log(".init(): ", "hiding no row message");
          $(rowSelectedDiv).hide();
        }
      });
      const applyButtonElem = document.getElementById("applyMultiEditButton");
      const attributesCheckboxes = document.querySelectorAll(
        '[id^="multiEditSelector_"]',
      );
      attributesCheckboxes.forEach((nextEl) => {
        this.log(
          ".init(): ",
          `setting event listener for next checkbox id: ${nextEl.id}`,
        );
        const valueInputElem = document.getElementById(nextEl.name);
        nextEl.addEventListener("change", (e) => {
          const updateAttributeChecked = e.target.checked;
          this.log(
            ".init(): ",
            `checkboxMassUpdateSelector changed, it now = ${updateAttributeChecked}`,
          );
          valueInputElem.disabled = !updateAttributeChecked;
        });
        applyButtonElem.disabled = $(attributesCheckboxes).is(":checked");
      });
      document
        .getElementById("multiSelectAllAttributes")
        .addEventListener("change", (e) => {
          const selectAllChecked = e.target.checked;
          this.log(
            ".init(): ",
            `selectAllCheckbox changed, it now = ${selectAllChecked}`,
          );
          document
            .querySelectorAll('[id^="multiEditSelector_"]')
            .forEach((nextEl) => {
              this.log(".init(): ", `setting next checkbox id: ${nextEl.id}`);
              nextEl.checked = selectAllChecked;
              nextEl.dispatchEvent(new Event("change"));
            });
        });
      const applyMultiEditButton = document.getElementById(
        "applyMultiEditButton",
      );
      const cancelMultiEditButton = document.getElementById(
        "cancelMultiEditButton",
      );
      applyMultiEditButton.addEventListener("click", (e) => {
        const logPrefix = ".applyChanges(): ";
        this.log(logPrefix, "applyMultiEditButton clicked...");
        const updatedValues = {};
        document
          .querySelectorAll('[id^="multiEditSelector_"]')
          .forEach((nextEl) => {
            if (nextEl.checked) {
              const valueInputElem = document.getElementById(nextEl.name);
              const elementType = valueInputElem.tagName.toLowerCase();
              this.log(logPrefix, `elementType = ${elementType}`);
              let newValue = valueInputElem.value;
              if (elementType == "select" && valueInputElem.multiple) {
                this.log(logPrefix, "elementType is multiple select");
                const selectElement = document.getElementById(nextEl.name);
                newValue = Array.from(
                  selectElement.selectedOptions,
                  function (option) {
                    return (option as any).value;
                  },
                ).join(", ");
              }
              if (valueInputElem.getAttribute("type") === "checkbox") {
                this.log(logPrefix, "elementType is checkbox");
                newValue = valueInputElem.checked;
              }
              this.log(
                logPrefix,
                `setting next field: ${valueInputElem.name} to ${newValue}`,
              );
              updatedValues[valueInputElem.name] = newValue;
            }
          });
        const newItem = [updatedValues];
        this.log(
          logPrefix,
          `applying mass update of fields:${JSON.stringify(newItem)}`,
        );
        const selectedNodeIds = params.api
          .getSelectedNodes()
          .map(function (nextNode) {
            return nextNode.id;
          });
        this.log(
          logPrefix,
          `selectedNodeIds = ${JSON.stringify(selectedNodeIds)}`,
        );
        const updatedRows = [];
        params.api.forEachNode((node) => {
          if (selectedNodeIds.includes(node.id)) {
            this.log(
              logPrefix,
              "next node id matches, checking if updated fields are editable",
            );
            const nodeData = node.data;
            this.logObj(logPrefix, "before update, nodeData = ", nodeData);
            const updatedRowValues = Object.assign({}, updatedValues);
            for (const nextNodeColumn in updatedValues) {
              this.log(
                logPrefix,
                `checking if columnConfigs has property: ${nextNodeColumn}`,
              );
              if (this.columnConfigs.hasOwnProperty(nextNodeColumn)) {
                this.log(logPrefix, "it does");
                const nextColumnDef = this.columnConfigs[nextNodeColumn];
                if (nextColumnDef.hasOwnProperty("editable")) {
                  let fieldIsEditable = true;
                  const editableValue = nextColumnDef.editable;
                  if (editableValue.hasOwnProperty("canEdit")) {
                    const drivingField = editableValue.canEdit;
                    this.log(
                      logPrefix,
                      `next column has canEdit, drivingField = ${drivingField}`,
                    );
                    fieldIsEditable = nodeData[drivingField];
                  } else if (
                    editableValue.hasOwnProperty("useEditableFields")
                  ) {
                    const editableFieldName =
                      params.context.EditableFieldsColumn;
                    this.log(
                      logPrefix,
                      `checking to see if this cell is editable using field: ${editableFieldName}`,
                    );
                    const editableFields = nodeData[editableFieldName]
                      ? nodeData[editableFieldName]
                      : {};
                    this.logObj(
                      logPrefix,
                      "editableFields = ",
                      editableFields,
                    );
                    this.log(
                      logPrefix,
                      `does it have this column?: = ${nextNodeColumn}`,
                    );
                    if (editableFields.hasOwnProperty(nextNodeColumn)) {
                      fieldIsEditable = editableFields[nextNodeColumn];
                      this.log(logPrefix, `Yes! And it = ${fieldIsEditable}`);
                    } else {
                      this.log(logPrefix, "No, leaving editability = true");
                    }
                  } else {
                    fieldIsEditable = nextColumnDef.editable;
                  }
                  this.log(
                    logPrefix,
                    `So, for column: ${nextNodeColumn}, fieldIsEditable = ${fieldIsEditable}`,
                  );
                  if (!fieldIsEditable) {
                    this.log(
                      logPrefix,
                      "Since it is not editable, removing it from the update for this row",
                    );
                    delete updatedRowValues[nextNodeColumn];
                  }
                }
              } else {
                this.log(logPrefix, "it does not");
              }
            }
            Object.assign(nodeData, updatedRowValues);
            this.log(
              logPrefix,
              `after update, nodeData = ${JSON.stringify(nodeData)}`,
            );
            node.setData(nodeData);
            updatedRows.push(node.data);
          }
        });
        this.log(
          logPrefix,
          `done updating rows, updatedRows = ${JSON.stringify(updatedRows)}`,
        );
        params.context.updateEditedTable(updatedRows);
        params.api.closeToolPanel();
      });
      cancelMultiEditButton.addEventListener("click", (e) => {
        this.log(".init(): ", "cancelMultiEditButton clicked...");
        params.api.closeToolPanel();
      });
    });
  }
  getGui() {
    return this.eGui;
  }
  refresh() {}
  buildEditElems(params) {
    const logPrefix = ".buildEditElems(): ";
    this.log(logPrefix, "Starting...");
    const fieldsToDisplay = params.fieldsToDisplay;
    const fieldDefinitions = params.fieldDefinitions;
    const gridConfig = params.context.getGridConfig();
    this.log(logPrefix, `fieldsToDisplay = ${JSON.stringify(fieldsToDisplay)}`);
    const childrenFields = {};
    for (const nextColumnDef in gridConfig.ColumnConfig) {
      const nextColumn = gridConfig.ColumnConfig[nextColumnDef];
      if (nextColumn.hasOwnProperty("children")) {
        nextColumn.children.forEach(function (nextChild) {
          childrenFields[nextChild.field] = nextChild;
        });
      }
    }
    this.log(logPrefix, `childrenFields = ${JSON.stringify(childrenFields)}`);
    const returnDiv = document.createElement("div");
    const headerSelectAllDiv = document.createElement("div");
    const headerLabelDiv = document.createElement("div");
    const headerButtonDiv = document.createElement("div");
    headerSelectAllDiv.className = "center";
    headerLabelDiv.className = "center";
    headerButtonDiv.className = "center";
    const headerLabel = document.createElement("h3");
    headerLabel.innerHTML = "Select attributes to update:<br/>";
    headerLabelDiv.appendChild(headerLabel);
    const selectAllCheckbox = document.createElement("input");
    selectAllCheckbox.type = "checkbox";
    selectAllCheckbox.id = "multiSelectAllAttributes";
    selectAllCheckbox.className = "multiEditAttributesSelectors";
    headerSelectAllDiv.appendChild(selectAllCheckbox);
    const selectAllLabel = document.createElement("label");
    selectAllLabel.innerHTML = "Select All";
    headerSelectAllDiv.appendChild(selectAllLabel);
    const applyButton = document.createElement("button");
    applyButton.id = "applyMultiEditButton";
    applyButton.className = "ptc-ok-button";
    applyButton.title = "Apply Changes";
    applyButton.disabled = true;
    const cancelButton = document.createElement("button");
    cancelButton.id = "cancelMultiEditButton";
    cancelButton.className = "ptc-close-button";
    cancelButton.title = "Cancel";
    headerButtonDiv.appendChild(applyButton);
    headerButtonDiv.appendChild(cancelButton);
    returnDiv.appendChild(headerLabelDiv);
    returnDiv.appendChild(headerButtonDiv);
    returnDiv.appendChild(headerSelectAllDiv);
    const horizRule = document.createElement("hr");
    horizRule.className = "editorDivider";
    returnDiv.appendChild(horizRule);
    const allAttributesDiv = document.createElement("div");
    const rowSelectedDiv = document.createElement("div");
    rowSelectedDiv.id = "rowSelectedDiv";
    rowSelectedDiv.style.textAlign = "center";
    rowSelectedDiv.style.display = "none";
    const noRowSelectedMessage = document.createElement("h6");
    noRowSelectedMessage.id = "noRowSelectedMessage";
    noRowSelectedMessage.style.color = "red";
    noRowSelectedMessage.innerHTML =
      "Please select at least one row in the grid for which to apply changes";
    rowSelectedDiv.appendChild(noRowSelectedMessage);
    allAttributesDiv.appendChild(rowSelectedDiv);
    Object.keys(fieldsToDisplay).forEach((nextField) => {
      this.log(logPrefix, `building edit element for ${nextField}`);
      const nextFieldRowDiv = document.createElement("div");
      nextFieldRowDiv.className = "newUserFieldRow";
      const nextFieldEnableCheckboxDiv = document.createElement("div");
      nextFieldEnableCheckboxDiv.className = "multiEditEnableCheckbox";
      const checkboxMassUpdateSelector = document.createElement("input");
      checkboxMassUpdateSelector.type = "checkbox";
      checkboxMassUpdateSelector.id = `multiEditSelector_${nextField}`;
      checkboxMassUpdateSelector.name = `multiEdit_${nextField}`;
      checkboxMassUpdateSelector.className = "multiEditAttributesSelectors";
      nextFieldEnableCheckboxDiv.appendChild(checkboxMassUpdateSelector);
      nextFieldRowDiv.appendChild(nextFieldEnableCheckboxDiv);
      const nextFieldLabelDiv = document.createElement("div");
      nextFieldLabelDiv.className = "newUserField";
      const nextFieldLabel = document.createElement("label");
      nextFieldLabel.innerHTML = fieldsToDisplay[nextField];
      nextFieldLabel.className = "multiEditAttributesLabel";
      nextFieldLabelDiv.appendChild(nextFieldLabel);
      nextFieldRowDiv.appendChild(nextFieldLabelDiv);
      let nextFieldCellEditor;
      const attributeValueDiv = document.createElement("div");
      attributeValueDiv.className = "multiEditAttributes";
      const nextFieldColumnConfig = gridConfig.ColumnConfig.hasOwnProperty(
        nextField,
      )
        ? gridConfig.ColumnConfig[nextField]
        : childrenFields.hasOwnProperty(nextField)
          ? childrenFields[nextField]
          : undefined;
      this.log(
        logPrefix,
        `nextFieldColumnConfig = ${JSON.stringify(nextFieldColumnConfig)}`,
      );
      if (nextFieldColumnConfig?.hasOwnProperty("cellEditor")) {
        nextFieldCellEditor = nextFieldColumnConfig.cellEditor;
      }
      if (
        nextFieldColumnConfig?.hasOwnProperty("cellRenderer") &&
        nextFieldColumnConfig.cellRenderer == "CustomCheckboxRenderer"
      ) {
        nextFieldCellEditor = "CustomCheckboxRenderer";
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
            attributeSelectElem_1.disabled = true;
            let selectHeight_1 = 0;
            if (selectOptions) {
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
            }
            this.log(
              logPrefix,
              `is select elememnt multiple? = ${selectMultiple}`,
            );
            attributeSelectElem_1.id = `multiEdit_${nextField}`;
            attributeSelectElem_1.name = nextField;
            attributeSelectElem_1.multiple = selectMultiple;
            attributeSelectElem_1.size = selectMultiple ? selectHeight_1 : 0;
            attributeValueDiv.appendChild(attributeSelectElem_1);
            attributeSelectElem_1;
            break;
          case "CustomCheckboxRenderer":
            const checkboxInputElem = document.createElement("input");
            checkboxInputElem.disabled = true;
            checkboxInputElem.type = "checkbox";
            checkboxInputElem.id = `multiEdit_${nextField}`;
            checkboxInputElem.name = nextField;
            checkboxInputElem.setAttribute("style", "multiEditCheckbox");
            attributeValueDiv.appendChild(checkboxInputElem);
            checkboxInputElem;
            break;
          default:
            let textInput;
            (textInput = document.createElement("input")).disabled = true;
            textInput.type = "text";
            textInput.id = `multiEdit_${nextField}`;
            textInput.name = nextField;
            attributeValueDiv.appendChild(textInput);
            textInput;
        }
        nextFieldRowDiv.appendChild(attributeValueDiv);
      } else if (fieldDefinitions.hasOwnProperty(nextField)) {
        switch (fieldDefinitions[nextField].baseType) {
          case "DATETIME":
            const dateInput = document.createElement("input");
            dateInput.disabled = true;
            dateInput.type = "date";
            dateInput.id = `multiEdit_${nextField}`;
            dateInput.name = nextField;
            attributeValueDiv.appendChild(dateInput);
            dateInput;
            break;
          case "IMAGELINK":
            break;
          default:
            let textInput;
            (textInput = document.createElement("input")).disabled = true;
            textInput.type = "text";
            textInput.id = `multiEdit_${nextField}`;
            textInput.name = nextField;
            attributeValueDiv.appendChild(textInput);
            allAttributesDiv.appendChild(attributeValueDiv);
            attributeValueDiv;
        }
        nextFieldRowDiv.appendChild(attributeValueDiv);
      }
      allAttributesDiv.appendChild(nextFieldRowDiv);
    });
    returnDiv.appendChild(allAttributesDiv);
    return returnDiv.innerHTML;
  }
}
