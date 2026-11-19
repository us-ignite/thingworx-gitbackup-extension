import { writeWidgetLog } from "./widget-logging";
const document: any = globalThis.document;

export class CustomSelectEditor {
  valueToTextMap: {};
  loggingEnabled: boolean;
  selectId: string;
  containerId: string;
  selectedItem: string;
  params: any;
  multiple: any;
  allItems: any;
  relatedItems: any;
  drivingField: any;
  paramValue: any;
  container: any;
  selectElementControl: any;
  constructor() {
    this.valueToTextMap = {};
    this.loggingEnabled = true;
    const now = new Date().getTime();
    this.selectId = (now + 1).toString();
    this.containerId = (now + 1).toString();
    this.onKeyDown = this.onKeyDown.bind(this);
  }
  log(prefix: string, message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomSelectEditor()",
      prefix,
      message,
    );
  }
  updateSelectedItems() {
    const logPrefix = ".updateSelectedItems(): ";
    const selectedValues = $(this.selectElement).multipleSelect(
      "getSelects",
      "value",
    );
    const selectedTexts = $(this.selectElement).multipleSelect(
      "getSelects",
      "text",
    );
    this.log(
      logPrefix,
      `select option changed, selectedTexts = ${selectedTexts.join(" and ")}`,
    );
    const selectedItems = [];
    selectedValues.forEach((nextIndex) => {
      const nextSelectedText = this.valueToTextMap[nextIndex];
      selectedItems.push(nextSelectedText);
    });
    this.log(
      logPrefix,
      `select option changed, selectedItems = ${JSON.stringify(selectedItems)}`,
    );
    this.selectedItem = selectedItems.join(" | ");
    const gridConfig = this.params.context.getGridConfig().ColumnConfig;
    this.log(logPrefix, `gridConfig = ${JSON.stringify(gridConfig)}`);
    Object.keys(gridConfig).forEach((columnName) => {
      if (columnName !== this.params.column.getId()) {
        this.log(
          logPrefix,
          `checking if other fields need to be updated, columnName = ${columnName}`,
        );
        const cellEditor = gridConfig[columnName]?.cellEditor;
        const cellEditorParams = gridConfig[columnName]?.cellEditorParams;
        if (
          CustomSelectEditor.name == cellEditor &&
          cellEditorParams?.drivingField == this.params.column.getId()
        ) {
          this.log(logPrefix, "that field is driven by me");
          const currentValue_1 = this.params.data[columnName];
          this.log(logPrefix, `currentValue = ${currentValue_1}`);
          const columnRelatedItems_1 = cellEditorParams.relatedItems;
          this.log(
            logPrefix,
            `columnRelatedItems = ${JSON.stringify(columnRelatedItems_1)}`,
          );
          const selectedItemIsInDrivingSet = selectedItems.every(
            function (item) {
              return columnRelatedItems_1[item].includes(currentValue_1);
            },
          );
          this.log(
            logPrefix,
            `selectedItemIsInDrivingSet = ${selectedItemIsInDrivingSet}`,
          );
          if (!selectedItemIsInDrivingSet) {
            this.params.data[columnName] = undefined;
          }
        }
      }
    });
  }
  selectElement(selectElement: any) {
    throw new Error("Method not implemented.");
  }
  onKeyDown(e) {
    const key = e.key;
    const selectedValue = $(this.selectElement).multipleSelect(
      "getSelects",
      "value",
    );
    this.log(
      ".onKeyDown(): ",
      `pressed key = ${key}, selectedValue = ${selectedValue}`,
    );
    if (!this.multiple && (key == "ArrowDown" || key == "ArrowUp")) {
      const currentValue = parseInt(selectedValue, 10);
      let newValue = currentValue;
      if (key == "ArrowDown" && currentValue + 1 !== this.allItems.length) {
        newValue = currentValue + 1;
      }
      if (key == "ArrowUp" && currentValue > 0) {
        newValue = currentValue - 1;
      }
      this.log(".onKeyDown(): ", `newValue = ${newValue}`);
      $(this.selectElement).multipleSelect("setSelects", [newValue]);
    }
  }
  init(params) {
    const logPrefix = `.init(${params.column.getId()}): `;
    this.params = params;
    this.loggingEnabled = this.params.context.EnableConsoleLogging;
    this.relatedItems = params.relatedItems;
    this.drivingField = params.drivingField;
    this.log(
      logPrefix,
      `Starting, this.relatedItems = ${JSON.stringify(this.relatedItems)}`,
    );
    this.log(logPrefix, `Starting, this.drivingField = ${this.drivingField}`);
    this.log(
      logPrefix,
      `Starting, driving field selection = ${this.params.data[this.drivingField]}`,
    );
    this.paramValue = params.value;
    if (typeof this.paramValue == "number") {
      this.paramValue = this.paramValue.toString();
    }
    this.allItems =
      params.hasOwnProperty("relatedItems") &&
      this.relatedItems.hasOwnProperty(this.params.data[this.drivingField])
        ? this.relatedItems[this.params.data[this.drivingField]]
        : params.values;
    this.multiple = params.multiple;
    this.log(
      logPrefix,
      `this.paramValue = ${this.paramValue} ${typeof this.paramValue}`,
    );
    this.container = document.createElement("div");
    this.container.classList.add("custom-select-dropdown");
    this.container.setAttribute("id", this.containerId);
    this.container.setAttribute("tabindex", "0");
    this.selectElementControl = document.createElement("select");
    this.selectElementControl.setAttribute("id", this.selectId);
    this.selectElementControl.setAttribute("name", "singleSelect");
    this.selectElementControl.setAttribute("data-size", "lg");
    if (this.multiple) {
      this.selectElementControl.setAttribute("multiple", "multiple");
    }
    const selectedItems =
      typeof this.paramValue == "object"
        ? [this.paramValue]
        : this.paramValue
          ? this.paramValue.split("|").map(function (nextValue) {
              return nextValue.trim();
            })
          : [];
    this.log(logPrefix, `selectedItems = ${selectedItems}`);
    this.allItems.forEach((nextOption, index) => {
      this.log(logPrefix, `nextOption = ${nextOption}`);
      const option = new Option(
        nextOption,
        index.toString(),
        false,
        selectedItems.includes(nextOption.toString()),
      );
      this.log(
        logPrefix,
        `appending nextOption ${option.outerHTML}, selected? = ${option.selected}`,
      );
      this.selectElementControl.appendChild(option);
      this.valueToTextMap[index] = nextOption;
    });
    this.container.addEventListener("keydown", this.onKeyDown);
    this.log(
      logPrefix,
      `adding select element to container: ${this.selectElementControl.innerHTML}`,
    );
    this.container.appendChild(this.selectElementControl);
  }
  getGui() {
    return this.container;
  }
  afterGuiAttached() {
    const logPrefix = ".afterGuiAttached(): ";
    this.log(logPrefix, "converting select to multiple-select");
    const keysOfCellEditorParams = Object.values([
      "value",
      "eventKey",
      "column",
      "colDef",
      "node",
      "data",
      "rowIndex",
      "cellStartedEdit",
      "onKeyDown",
      "stopEditing",
      "eGridCell",
      "parseValue",
      "formatValue",
      "api",
      "context",
    ]);
    this.log(
      logPrefix,
      `keysOfCellEditorParams = ${JSON.stringify(keysOfCellEditorParams)}`,
    );
    const multiSelectOptions = {
      isOpen: true,
      onClose: () => {
        this.params.stopEditing();
      },
      classes: "input",
      classPrefix: "input",
    };
    for (const nextOption in this.params) {
      if (!keysOfCellEditorParams.includes(nextOption)) {
        this.log(
          logPrefix,
          `adding nextOption ${nextOption} : ${this.params[nextOption]}`,
        );
        multiSelectOptions[nextOption] = this.params[nextOption];
      }
    }
    this.log(
      logPrefix,
      `done adding options, multiSelectOptions = ${JSON.stringify(multiSelectOptions)}`,
    );
    $(this.selectElement).multipleSelect(multiSelectOptions);
    this.container.focus();
  }
  getValue() {
    this.updateSelectedItems();
    this.log(".getValue(): ", `returning ${this.selectedItem}`);
    return this.selectedItem;
  }
  destroy() {}
  isPopup() {
    return true;
  }
  getRelatedValues(drivingField) {}
}
