import { writeWidgetLog } from "./widget-logging";
const document: any = globalThis.document;

export class CustomCheckboxRenderer {
  loggingEnabled: boolean;
  checkboxState: any;
  params: any;
  paramValue: any;
  container: any;
  checkboxInput: any;
  logPrefix: string;

  constructor() {
    this.loggingEnabled = true;
    this.checkedHandler = this.checkedHandler.bind(this);
  }
  log(message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomCheckboxRenderer",
      this.logPrefix,
      message,
    );
  }

  checkedHandler(e) {
    this.logPrefix = ".checkedHandler(): ";
    this.checkboxState = e.target.checked;
    const colId = this.params.column.getColId();
    this.params.node.setDataValue(colId, this.checkboxState);
    this.log(`checkbox changed, new state = ${this.checkboxState}`);
  }
  init(params) {
    this.logPrefix = `.init(${params.column.getId()}): `;
    this.params = params;
    this.loggingEnabled = this.params.context.EnableConsoleLogging;
    this.paramValue = params.value;
    let checkboxEditable;
    const colDef = params.colDef;
    if (colDef.hasOwnProperty("editable")) {
      const editableValue = colDef.editable;
      this.log(`next column has editable, value = ${editableValue}`);
      if (editableValue.hasOwnProperty("canEdit")) {
        const drivingField_1 = editableValue.canEdit;
        this.log(`next column has canEdit, drivingField = ${drivingField_1}`);
        checkboxEditable = () => {
          return this.params.data[drivingField_1];
        };
      } else {
        checkboxEditable = editableValue.hasOwnProperty("useEditableFields")
          ? () => {
              let fieldIsEditable = true;
              const editableFieldName =
                this.params.context.EditableFieldsColumn;
              this.log(
                `checking to see if this cell is editable using field: ${editableFieldName}`,
              );
              const editableFields = this.params.data[editableFieldName]
                ? JSON.parse(this.params.data[editableFieldName])
                : {};
              this.log(`editableFields = ${JSON.stringify(editableFields)}`);
              this.log(`does it have this column?: = ${colDef.field}`);
              if (editableFields.hasOwnProperty(colDef.field)) {
                fieldIsEditable = editableFields[colDef.field];
                this.log(`Yes! And it = ${fieldIsEditable}`);
              }
              return fieldIsEditable;
            }
          : colDef.editable;
      }
    }
    this.log(
      `Starting, this.paramValue = ${JSON.stringify(this.paramValue)}, checkboxEditable = ${checkboxEditable}`,
    );
    this.log(`Starting, param checkboxDisabled = ${params.checkboxDisabled}`);
    this.container = document.createElement("div");
    this.container.style.cssText =
      "text-align: center; vertical-align: middle;";
    this.checkboxInput = document.createElement("input");
    this.checkboxInput.type = "checkbox";
    if (!!params.checkboxDisabled || !checkboxEditable) {
      this.checkboxInput.setAttribute("disabled", "");
      this.checkboxInput.setAttribute("readonly", "");
    }
    this.checkboxInput.checked = params.value;
    this.checkboxState = params.value;
    this.checkboxInput.addEventListener("click", this.checkedHandler);
    this.container.appendChild(this.checkboxInput);
  }
  getGui() {
    return this.container;
  }
  getValue() {
    return this.checkboxState;
  }
}
