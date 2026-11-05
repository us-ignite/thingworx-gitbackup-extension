import { writeWidgetLog, writeWidgetLogObject } from "./widget-logging";
// Recovered ThingWorx runtime widget. Library imports replace webpack modules.
import {
  AG_GRID_ROW_ID_FIELD,
  SERVER_SIDE_ROW_COUNT_STATUS_PANEL_ID,
  createGridData,
} from "./grid-data";
import { DatePicker } from "./date-picker";
import { CustomImageRenderer } from "./image-renderer";
import { FileUploader } from "./file-uploader";
import { CustomDateRenderer } from "./date-renderer";
import { CustomJsonRenderer } from "./json-renderer";
import { CustomColumnAggregateFooterRenderer } from "./aggregate-footer";
import { icon } from "@fortawesome/fontawesome-svg-core";
import {
  faSpinner,
  faExternalLinkAlt,
} from "@fortawesome/free-solid-svg-icons";
import { createGrid } from "ag-grid-community";
import "ag-grid-enterprise";
import "multiple-select";
const document: any = globalThis.document;
import widgetCss from "./widget.css?inline";
import agGridCss from "ag-grid-community/styles/ag-grid.css?inline";
import alpineCss from "ag-grid-community/styles/ag-theme-alpine.css?inline";
import balhamCss from "ag-grid-community/styles/ag-theme-balham.css?inline";
import materialCss from "ag-grid-community/styles/ag-theme-material.css?inline";
import quartzCss from "ag-grid-community/styles/ag-theme-quartz.css?inline";
import multipleSelectCss from "multiple-select/dist/multiple-select.css?inline";

for (const css of [
  agGridCss,
  alpineCss,
  balhamCss,
  materialCss,
  quartzCss,
  multipleSelectCss,
  widgetCss,
]) {
  const style = document.createElement("style");
  style.textContent = css;
  document.head.appendChild(style);
}
function getSymbol(symbolDesc) {
  window.TW[symbolDesc] ||= window.Symbol
    ? window.Symbol(symbolDesc)
    : symbolDesc;
  return window.TW[symbolDesc];
}
const willBindSymbol = getSymbol("@@_willBind");
const didBindSymbol = getSymbol("@@_didBind");
const decoratedPropertiesSymbol = getSymbol("@@_decoratedProperties");
const decoratedServicesSymbol = getSymbol("@@_decoratedServices");
const versionSymbol = getSymbol("@@_version");
const _getInheritedProperty = function (proto, property) {
  if (proto.hasOwnProperty(property)) {
    return proto[property];
  }
  proto[property] = {};
  const superproto = Object.getPrototypeOf(proto);
  for (const key in superproto[property]) {
    proto[property][key] = superproto[property][key];
  }
  return proto[property];
};
const _getDecoratedProperties = (proto) =>
  _getInheritedProperty(proto, decoratedPropertiesSymbol);
const _getDecoratedServices = (proto) =>
  _getInheritedProperty(proto, decoratedServicesSymbol);
const _getDecoratedWillBind = (proto) =>
  _getInheritedProperty(proto, willBindSymbol);
const _getDecoratedDidBind = (proto) =>
  _getInheritedProperty(proto, didBindSymbol);
function property(...args): any {
  let name;
  let aspectsIndex;
  const decorator = (target, key, descriptor) => {
    let setter;
    const hasDescriptor = descriptor !== undefined;
    if (!hasDescriptor) {
      descriptor = {};
    }
    name ||= key;
    if (descriptor.set) {
      const previousSetter = descriptor.set;
      setter = function (value) {
        this.setProperty(name, value);
        previousSetter.apply(this, arguments);
      };
    } else {
      setter = function (value) {
        this.setProperty(name, value);
      };
    }
    descriptor.set = setter;
    descriptor.get = function () {
      return this.getProperty(name);
    };
    _getDecoratedProperties(target)[name] = key;
    if (aspectsIndex !== undefined) {
      for (let i = aspectsIndex; i < args.length; i++) {
        const aspect = args[i];
        switch (aspect._key) {
          case "_willBind":
            _getDecoratedWillBind(target)[name] = aspect._value;
            break;
          case "_didBind":
            _getDecoratedDidBind(target)[name] = aspect._value;
        }
      }
    }
    if (!hasDescriptor) {
      Object.defineProperty(target, key, descriptor);
    }
  };
  if (typeof args[0] == "string") {
    name = args[0];
    aspectsIndex = 1;
    return decorator;
  } else if (args[0]._key && args[0]._value) {
    aspectsIndex = 0;
    return decorator;
  } else {
    name = args[1];
    decorator.apply(this, arguments);
    return;
  }
}
function service(arg1, ..._decoratorArgs: any[]): any {
  let name = arg1;
  const decorator = (target, key, descriptor) => {
    _getDecoratedServices(target)[name] = key;
  };
  if (typeof arg1 == "string") {
    return decorator;
  }
  name = arguments[1];
  decorator.apply(this, arguments);
}
function widgetRuntimeSupport_event(arg1, ..._decoratorArgs: any[]): any {
  let name = arg1;
  const decorator = (target, key, descriptor) => {
    const event = function () {
      this.jqElement.triggerHandler(name);
    };
    if (descriptor) {
      descriptor.get = function () {
        return event;
      };
      descriptor.writable = false;
    } else {
      Object.defineProperty(target, key, {
        get: () => event,
      });
    }
  };
  if (typeof arg1 == "string") {
    return decorator;
  }
  name = arguments[1];
  decorator.apply(this, arguments);
}
function TWWidgetDefinition(arg1): any {
  let name;
  const decorator = function (widget) {
    Object.defineProperty(widget, "prototype", {
      writable: false,
    });
    _getDecoratedProperties(widget.prototype);
    _getDecoratedServices(widget.prototype);
    _getDecoratedWillBind(widget.prototype);
    _getDecoratedDidBind(widget.prototype);
    TW.Runtime.Widgets[name] = function () {
      return new widget();
    };
  };
  if (typeof arg1 == "string") {
    name = arg1;
    return decorator;
  }
  name = arg1.name;
  decorator(arg1);
}
if (typeof TW.Widget == "function") {
  (function () {
    let __BMTWInternalState;
    let TWWidgetConstructor = TW.Widget;
    if (window.TWRuntimeWidget) {
      if (
        !TWRuntimeWidget.prototype[versionSymbol] ||
        TWRuntimeWidget.prototype[versionSymbol] < 4
      ) {
        let prototype = {
          updateProperty(info) {
            const decoratedWillBind = this[willBindSymbol];
            const value = info.RawSinglePropertyValue;
            let shouldUpdate = true;
            if (info.TargetProperty in decoratedWillBind) {
              shouldUpdate = this[decoratedWillBind[info.TargetProperty]](
                value,
                info,
              );
            }
            const decoratedProperties = this[decoratedPropertiesSymbol];
            if (shouldUpdate && info.TargetProperty in decoratedProperties) {
              const currentValue = this.getProperty(info.TargetProperty);
              this[decoratedProperties[info.TargetProperty]] = value;
              const decoratedDidBind = this[didBindSymbol];
              if (info.TargetProperty in decoratedDidBind) {
                this[decoratedDidBind[info.TargetProperty]](currentValue, info);
              }
            }
          },
          serviceInvoked(name) {
            const decoratedServices = this[decoratedServicesSymbol];
            if (name in decoratedServices) {
              this[decoratedServices[name]]();
            }
          },
        };
        TWRuntimeWidget.prototype.updateProperty = prototype.updateProperty;
        TWRuntimeWidget.prototype.serviceInvoked = prototype.serviceInvoked;
        TWRuntimeWidget.prototype[versionSymbol] = 4;
      }
      return;
    }
    TW.Widget = function () {
      TWWidgetConstructor.apply(this, arguments);
      __BMTWInternalState = this;
      return this;
    };
    TW.Widget.prototype = TWWidgetConstructor.prototype;
    Object.keys(TWWidgetConstructor).forEach((key) => {
      TW.Widget[key] = TWWidgetConstructor[key];
    });
    let internalStates = new WeakMap();
    window.TWRuntimeWidget = function () {
      internalStates.set(this, __BMTWInternalState);
      Object.keys(__BMTWInternalState).forEach((key) => {
        let value = __BMTWInternalState[key];
        if (typeof value == "function") {
          TWRuntimeWidget.prototype[key] ||= function () {
            return internalStates.get(this)[key].apply(this, arguments);
          };
          __BMTWInternalState[key] = value.bind(this);
        } else {
          this[key] = value;
        }
      });
      __BMTWInternalState = undefined;
    };
    TWRuntimeWidget.prototype = Object.assign(
      Object.create(TWWidgetConstructor.prototype),
      {
        updateProperty(info) {
          const decoratedWillBind = this[willBindSymbol];
          const value = info.RawSinglePropertyValue;
          let shouldUpdate = true;
          if (info.TargetProperty in decoratedWillBind) {
            shouldUpdate = this[decoratedWillBind[info.TargetProperty]](
              value,
              info,
            );
          }
          const decoratedProperties = this[decoratedPropertiesSymbol];
          if (shouldUpdate && info.TargetProperty in decoratedProperties) {
            const currentValue = this.getProperty(info.TargetProperty);
            this[decoratedProperties[info.TargetProperty]] = value;
            const decoratedDidBind = this[didBindSymbol];
            if (info.TargetProperty in decoratedDidBind) {
              this[decoratedDidBind[info.TargetProperty]](currentValue, info);
            }
          }
        },
        serviceInvoked(name) {
          const decoratedServices = this[decoratedServicesSymbol];
          if (name in decoratedServices) {
            this[decoratedServices[name]]();
          }
        },
        [versionSymbol]: 4,
      },
    );
  })();
}

let VISIBLE_INDEX_POSITIONS;
class CustomSelectEditor {
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
    const _this = this;
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
      "select option changed, selectedTexts = " + selectedTexts.join(" and "),
    );
    const selectedItems = [];
    selectedValues.forEach(function (nextIndex) {
      const nextSelectedText = _this.valueToTextMap[nextIndex];
      selectedItems.push(nextSelectedText);
    });
    this.log(
      logPrefix,
      "select option changed, selectedItems = " + JSON.stringify(selectedItems),
    );
    this.selectedItem = selectedItems.join(" | ");
    const gridConfig = this.params.context.getGridConfig().ColumnConfig;
    this.log(logPrefix, "gridConfig = " + JSON.stringify(gridConfig));
    Object.keys(gridConfig).forEach(function (columnName) {
      if (columnName !== _this.params.column.getId()) {
        _this.log(
          logPrefix,
          "checking if other fields need to be updated, columnName = " +
            columnName,
        );
        const cellEditor = gridConfig[columnName]?.cellEditor;
        const cellEditorParams = gridConfig[columnName]?.cellEditorParams;
        if (
          CustomSelectEditor.name == cellEditor &&
          cellEditorParams?.drivingField == _this.params.column.getId()
        ) {
          _this.log(logPrefix, "that field is driven by me");
          const currentValue_1 = _this.params.data[columnName];
          _this.log(logPrefix, "currentValue = " + currentValue_1);
          const columnRelatedItems_1 = cellEditorParams.relatedItems;
          _this.log(
            logPrefix,
            "columnRelatedItems = " + JSON.stringify(columnRelatedItems_1),
          );
          const selectedItemIsInDrivingSet = selectedItems.every(
            function (item) {
              return columnRelatedItems_1[item].includes(currentValue_1);
            },
          );
          _this.log(
            logPrefix,
            "selectedItemIsInDrivingSet = " + selectedItemIsInDrivingSet,
          );
          if (!selectedItemIsInDrivingSet) {
            _this.params.data[columnName] = undefined;
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
      "pressed key = " + key + ", selectedValue = " + selectedValue,
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
      this.log(".onKeyDown(): ", "newValue = " + newValue);
      $(this.selectElement).multipleSelect("setSelects", [newValue]);
    }
  }
  init(params) {
    const _this = this;
    const logPrefix = ".init(" + params.column.getId() + "): ";
    this.params = params;
    this.loggingEnabled = this.params.context.EnableConsoleLogging;
    this.relatedItems = params.relatedItems;
    this.drivingField = params.drivingField;
    this.log(
      logPrefix,
      "Starting, this.relatedItems = " + JSON.stringify(this.relatedItems),
    );
    this.log(logPrefix, "Starting, this.drivingField = " + this.drivingField);
    this.log(
      logPrefix,
      "Starting, driving field selection = " +
        this.params.data[this.drivingField],
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
      "this.paramValue = " + this.paramValue + " " + typeof this.paramValue,
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
    this.log(logPrefix, "selectedItems = " + selectedItems);
    this.allItems.forEach(function (nextOption, index) {
      _this.log(logPrefix, "nextOption = " + nextOption);
      const option = new Option(
        nextOption,
        index.toString(),
        false,
        selectedItems.includes(nextOption.toString()),
      );
      _this.log(
        logPrefix,
        "appending nextOption " +
          option.outerHTML +
          ", selected? = " +
          option.selected,
      );
      _this.selectElementControl.appendChild(option);
      _this.valueToTextMap[index] = nextOption;
    });
    this.container.addEventListener("keydown", this.onKeyDown);
    this.log(
      logPrefix,
      "adding select element to container: " +
        this.selectElementControl.innerHTML,
    );
    this.container.appendChild(this.selectElementControl);
  }
  getGui() {
    return this.container;
  }
  afterGuiAttached() {
    const _this = this;
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
      "keysOfCellEditorParams = " + JSON.stringify(keysOfCellEditorParams),
    );
    const multiSelectOptions = {
      isOpen: true,
      onClose: function () {
        _this.params.stopEditing();
      },
      classes: "input",
      classPrefix: "input",
    };
    for (const nextOption in this.params) {
      if (!keysOfCellEditorParams.includes(nextOption)) {
        this.log(
          logPrefix,
          "adding nextOption " + nextOption + " : " + this.params[nextOption],
        );
        multiSelectOptions[nextOption] = this.params[nextOption];
      }
    }
    this.log(
      logPrefix,
      "done adding options, multiSelectOptions = " +
        JSON.stringify(multiSelectOptions),
    );
    $(this.selectElement).multipleSelect(multiSelectOptions);
    this.container.focus();
  }
  getValue() {
    this.updateSelectedItems();
    this.log(".getValue(): ", "returning " + this.selectedItem);
    return this.selectedItem;
  }
  destroy() {}
  isPopup() {
    return true;
  }
  getRelatedValues(drivingField) {}
}
class CustomActionCellRenderer {
  loggingEnabled: boolean;
  clickedButton: any;
  params: any;
  outerDiv: any;
  innerDiv: any;
  iconButtons: any;

  constructor() {
    const _this = this;
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
      "this.clickedButton: " + e.currentTarget.title,
    );
    this.clickedButton = e.currentTarget.title;
    this.params.context.emitCustomActionButtonClick(this.clickedButton);
  }
  init(params) {
    const _this = this;
    const logPrefix = ".init(" + params.column.getId() + "): ";
    this.params = params;
    this.loggingEnabled = params.context.EnableConsoleLogging;
    this.log(logPrefix, "Starting...");
    const rowHeightPx =
      (params.context.RowHeight ? params.context.RowHeight - 2 : 23) + "px";
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
      "this.iconButtons = " + JSON.stringify(this.iconButtons),
    );
    if (this.iconButtons) {
      this.iconButtons.forEach(function (nextIcon) {
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
              "AgGridWidget.CustomActionCellRenderer():" +
                params.data[buttonParams[1]],
            );
            if (params.data[buttonParams[1]]) {
              _this.outerDiv.className =
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
        buttonElement.addEventListener("click", function (e) {
          _this.onButtonClick(e);
        });
        _this.innerDiv.appendChild(buttonElement);
      });
    }
    this.outerDiv.appendChild(this.innerDiv);
  }
  getGui() {
    return this.outerDiv;
  }
}
class CustomCheckboxRenderer {
  loggingEnabled: boolean;
  checkboxState: any;
  params: any;
  paramValue: any;
  container: any;
  checkboxInput: any;
  logPrefix: string;

  constructor() {
    const _this = this;
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
    this.log("checkbox changed, new state = " + this.checkboxState);
  }
  init(params) {
    const _this = this;
    this.logPrefix = ".init(" + params.column.getId() + "): ";
    this.params = params;
    this.loggingEnabled = this.params.context.EnableConsoleLogging;
    this.paramValue = params.value;
    let checkboxEditable;
    const colDef = params.colDef;
    if (colDef.hasOwnProperty("editable")) {
      const editableValue = colDef.editable;
      this.log("next column has editable, value = " + editableValue);
      if (editableValue.hasOwnProperty("canEdit")) {
        const drivingField_1 = editableValue.canEdit;
        this.log("next column has canEdit, drivingField = " + drivingField_1);
        checkboxEditable = function () {
          return _this.params.data[drivingField_1];
        };
      } else {
        checkboxEditable = editableValue.hasOwnProperty("useEditableFields")
          ? function () {
              let fieldIsEditable = true;
              const editableFieldName =
                _this.params.context.EditableFieldsColumn;
              _this.log(
                "checking to see if this cell is editable using field: " +
                  editableFieldName,
              );
              const editableFields = _this.params.data[editableFieldName]
                ? JSON.parse(_this.params.data[editableFieldName])
                : {};
              _this.log("editableFields = " + JSON.stringify(editableFields));
              _this.log("does it have this column?: = " + colDef.field);
              if (editableFields.hasOwnProperty(colDef.field)) {
                fieldIsEditable = editableFields[colDef.field];
                _this.log("Yes! And it = " + fieldIsEditable);
              }
              return fieldIsEditable;
            }
          : colDef.editable;
      }
    }
    this.log(
      "Starting, this.paramValue = " +
        JSON.stringify(this.paramValue) +
        ", checkboxEditable = " +
        checkboxEditable,
    );
    this.log("Starting, param checkboxDisabled = " + params.checkboxDisabled);
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

class CustomMultiEditToolPanel {
  loggingEnabled: boolean;
  columnConfigs: {};
  eGui: any;

  constructor() {
    const _this = this;
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
    const _this = this;
    this.log(".init(): ", "Starting...");
    this.loggingEnabled = params.context.EnableConsoleLogging;
    const gridConfig = params.context.getGridConfig();
    if (gridConfig.hasOwnProperty("ColumnConfig")) {
      this.columnConfigs = gridConfig.ColumnConfig;
    }
    this.eGui = document.createElement("div");
    this.eGui.className = "multiEdit";
    params.api.addEventListener("modelUpdated", function () {
      _this.eGui.innerHTML = _this.buildEditElems(params);
      const rowSelectedDiv = document.getElementById("rowSelectedDiv");
      if (params.api.getSelectedRows().length === 0) {
        rowSelectedDiv.style.display = "block";
        rowSelectedDiv.style.textAlign = "center";
      }
      params.api.addEventListener("selectionChanged", function () {
        const selectedRowCount = params.api.getSelectedRows().length;
        _this.log(
          ".init(): ",
          "row selection changed, selectedRowCount = " + selectedRowCount,
        );
        if (selectedRowCount == 0) {
          _this.log(".init(): ", "showing no row message");
          $(rowSelectedDiv).show();
        } else {
          _this.log(".init(): ", "hiding no row message");
          $(rowSelectedDiv).hide();
        }
      });
      const applyButtonElem = document.getElementById("applyMultiEditButton");
      const attributesCheckboxes = document.querySelectorAll(
        '[id^="multiEditSelector_"]',
      );
      attributesCheckboxes.forEach(function (nextEl) {
        _this.log(
          ".init(): ",
          "setting event listener for next checkbox id: " + nextEl.id,
        );
        const valueInputElem = document.getElementById(nextEl.name);
        nextEl.addEventListener("change", function (e) {
          const updateAttributeChecked = e.target.checked;
          _this.log(
            ".init(): ",
            "checkboxMassUpdateSelector changed, it now = " +
              updateAttributeChecked,
          );
          valueInputElem.disabled = !updateAttributeChecked;
        });
        applyButtonElem.disabled = $(attributesCheckboxes).is(":checked");
      });
      document
        .getElementById("multiSelectAllAttributes")
        .addEventListener("change", function (e) {
          const selectAllChecked = e.target.checked;
          _this.log(
            ".init(): ",
            "selectAllCheckbox changed, it now = " + selectAllChecked,
          );
          document
            .querySelectorAll('[id^="multiEditSelector_"]')
            .forEach(function (nextEl) {
              _this.log(".init(): ", "setting next checkbox id: " + nextEl.id);
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
      applyMultiEditButton.addEventListener("click", function (e) {
        const logPrefix = ".applyChanges(): ";
        _this.log(logPrefix, "applyMultiEditButton clicked...");
        const updatedValues = {};
        document
          .querySelectorAll('[id^="multiEditSelector_"]')
          .forEach(function (nextEl) {
            if (nextEl.checked) {
              const valueInputElem = document.getElementById(nextEl.name);
              const elementType = valueInputElem.tagName.toLowerCase();
              _this.log(logPrefix, "elementType = " + elementType);
              let newValue = valueInputElem.value;
              if (elementType == "select" && valueInputElem.multiple) {
                _this.log(logPrefix, "elementType is multiple select");
                const selectElement = document.getElementById(nextEl.name);
                newValue = Array.from(
                  selectElement.selectedOptions,
                  function (option) {
                    return (option as any).value;
                  },
                ).join(", ");
              }
              if (valueInputElem.getAttribute("type") === "checkbox") {
                _this.log(logPrefix, "elementType is checkbox");
                newValue = valueInputElem.checked;
              }
              _this.log(
                logPrefix,
                "setting next field: " +
                  valueInputElem.name +
                  " to " +
                  newValue,
              );
              updatedValues[valueInputElem.name] = newValue;
            }
          });
        const newItem = [updatedValues];
        _this.log(
          logPrefix,
          "applying mass update of fields:" + JSON.stringify(newItem),
        );
        const selectedNodeIds = params.api
          .getSelectedNodes()
          .map(function (nextNode) {
            return nextNode.id;
          });
        _this.log(
          logPrefix,
          "selectedNodeIds = " + JSON.stringify(selectedNodeIds),
        );
        const updatedRows = [];
        params.api.forEachNode(function (node) {
          if (selectedNodeIds.includes(node.id)) {
            _this.log(
              logPrefix,
              "next node id matches, checking if updated fields are editable",
            );
            const nodeData = node.data;
            _this.logObj(logPrefix, "before update, nodeData = ", nodeData);
            const updatedRowValues = Object.assign({}, updatedValues);
            for (const nextNodeColumn in updatedValues) {
              _this.log(
                logPrefix,
                "checking if columnConfigs has property: " + nextNodeColumn,
              );
              if (_this.columnConfigs.hasOwnProperty(nextNodeColumn)) {
                _this.log(logPrefix, "it does");
                const nextColumnDef = _this.columnConfigs[nextNodeColumn];
                if (nextColumnDef.hasOwnProperty("editable")) {
                  let fieldIsEditable = true;
                  const editableValue = nextColumnDef.editable;
                  if (editableValue.hasOwnProperty("canEdit")) {
                    const drivingField = editableValue.canEdit;
                    _this.log(
                      logPrefix,
                      "next column has canEdit, drivingField = " + drivingField,
                    );
                    fieldIsEditable = nodeData[drivingField];
                  } else if (
                    editableValue.hasOwnProperty("useEditableFields")
                  ) {
                    const editableFieldName =
                      params.context.EditableFieldsColumn;
                    _this.log(
                      logPrefix,
                      "checking to see if this cell is editable using field: " +
                        editableFieldName,
                    );
                    const editableFields = nodeData[editableFieldName]
                      ? nodeData[editableFieldName]
                      : {};
                    _this.logObj(
                      logPrefix,
                      "editableFields = ",
                      editableFields,
                    );
                    _this.log(
                      logPrefix,
                      "does it have this column?: = " + nextNodeColumn,
                    );
                    if (editableFields.hasOwnProperty(nextNodeColumn)) {
                      fieldIsEditable = editableFields[nextNodeColumn];
                      _this.log(logPrefix, "Yes! And it = " + fieldIsEditable);
                    } else {
                      _this.log(logPrefix, "No, leaving editability = true");
                    }
                  } else {
                    fieldIsEditable = nextColumnDef.editable;
                  }
                  _this.log(
                    logPrefix,
                    "So, for column: " +
                      nextNodeColumn +
                      ", fieldIsEditable = " +
                      fieldIsEditable,
                  );
                  if (!fieldIsEditable) {
                    _this.log(
                      logPrefix,
                      "Since it is not editable, removing it from the update for this row",
                    );
                    delete updatedRowValues[nextNodeColumn];
                  }
                }
              } else {
                _this.log(logPrefix, "it does not");
              }
            }
            Object.assign(nodeData, updatedRowValues);
            _this.log(
              logPrefix,
              "after update, nodeData = " + JSON.stringify(nodeData),
            );
            node.setData(nodeData);
            updatedRows.push(node.data);
          }
        });
        _this.log(
          logPrefix,
          "done updating rows, updatedRows = " + JSON.stringify(updatedRows),
        );
        params.context.updateEditedTable(updatedRows);
        params.api.closeToolPanel();
      });
      cancelMultiEditButton.addEventListener("click", function (e) {
        _this.log(".init(): ", "cancelMultiEditButton clicked...");
        params.api.closeToolPanel();
      });
    });
  }
  getGui() {
    return this.eGui;
  }
  refresh() {}
  buildEditElems(params) {
    const _this = this;
    const logPrefix = ".buildEditElems(): ";
    this.log(logPrefix, "Starting...");
    const fieldsToDisplay = params.fieldsToDisplay;
    const fieldDefinitions = params.fieldDefinitions;
    const gridConfig = params.context.getGridConfig();
    this.log(logPrefix, "fieldsToDisplay = " + JSON.stringify(fieldsToDisplay));
    const childrenFields = {};
    for (const nextColumnDef in gridConfig.ColumnConfig) {
      const nextColumn = gridConfig.ColumnConfig[nextColumnDef];
      if (nextColumn.hasOwnProperty("children")) {
        nextColumn.children.forEach(function (nextChild) {
          childrenFields[nextChild.field] = nextChild;
        });
      }
    }
    this.log(logPrefix, "childrenFields = " + JSON.stringify(childrenFields));
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
    Object.keys(fieldsToDisplay).forEach(function (nextField) {
      _this.log(logPrefix, `building edit element for ${nextField}`);
      const nextFieldRowDiv = document.createElement("div");
      nextFieldRowDiv.className = "newUserFieldRow";
      const nextFieldEnableCheckboxDiv = document.createElement("div");
      nextFieldEnableCheckboxDiv.className = "multiEditEnableCheckbox";
      const checkboxMassUpdateSelector = document.createElement("input");
      checkboxMassUpdateSelector.type = "checkbox";
      checkboxMassUpdateSelector.id = "multiEditSelector_" + nextField;
      checkboxMassUpdateSelector.name = "multiEdit_" + nextField;
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
      _this.log(
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
        _this.log(logPrefix, `it has a cellEditor of ${nextFieldCellEditor}`);
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
              selectOptions.forEach(function (nextOption) {
                _this.log(
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
            _this.log(
              logPrefix,
              `is select elememnt multiple? = ${selectMultiple}`,
            );
            attributeSelectElem_1.id = "multiEdit_" + nextField;
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
            checkboxInputElem.id = "multiEdit_" + nextField;
            checkboxInputElem.name = nextField;
            checkboxInputElem.setAttribute("style", "multiEditCheckbox");
            attributeValueDiv.appendChild(checkboxInputElem);
            checkboxInputElem;
            break;
          default:
            let textInput;
            (textInput = document.createElement("input")).disabled = true;
            textInput.type = "text";
            textInput.id = "multiEdit_" + nextField;
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
            dateInput.id = "multiEdit_" + nextField;
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
            textInput.id = "multiEdit_" + nextField;
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
const index_es = { Ay: { icon } };
const fontawesome_free_solid_index_es = {
  z1G: faSpinner,
  AaJ: faExternalLinkAlt,
};
class CustomNewRowToolPanel {
  loggingEnabled: boolean;
  NOW: string;
  imgIds: {};
  fileUploader: FileUploader;
  buttonSpinnerElement: Element;
  eGui: any;
  params: Record<string, any>;

  constructor() {
    const _this = this;
    this.loggingEnabled = true;
    this.NOW = new Date().getTime() + "";
    this.imgIds = {};
    this.fileUploader = new FileUploader();
    this.buttonSpinnerElement = index_es.Ay.icon(
      fontawesome_free_solid_index_es.z1G,
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
    const logPrefix = ".startUpload(" + this.NOW + "): ";
    this.log(
      logPrefix,
      logPrefix + "getting containerElement with id = " + containerId,
    );
    this.log(logPrefix, logPrefix + "getting imageelement with id = " + imgId);
    const containerElement = document.getElementById(containerId);
    Array.from(containerElement.children).forEach((nextNode) => {
      this.log(
        logPrefix,
        logPrefix +
          "nextNode " +
          (nextNode as any).id +
          " = " +
          (nextNode as any).innerHTML,
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
      this.log(logPrefix, logPrefix + "upload complete");
      const colId = this.params.column.getColId();
      this.params.node.setDataValue(colId, newFilePath);
      imgElement.src = newFilePath;
      containerElement.replaceChild(imgElement, this.buttonSpinnerElement);
      this.log(logPrefix, logPrefix + "renderer update done");
    } catch (error) {
      this.log(logPrefix, logPrefix + "failure uploading...");
      containerElement.replaceChild(imgElement, this.buttonSpinnerElement);
    }
  }

  openFileInput(e, fileInputId) {
    this.log(
      ".openFileInput(): ",
      ".openFileInput(): button clicked, opening file input element with id: " +
        fileInputId,
    );
    const fileInputElement = document.getElementById(fileInputId);
    fileInputElement.dispatchEvent(new Event("click"));
    fileInputElement.click();
  }
  init(params) {
    const _this = this;
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
    params.api.addEventListener("modelUpdated", function () {
      _this.eGui.innerHTML = _this.buildEditElems();
      const fieldsRequired = params.fieldsRequired;
      const fieldDefaults = params.fieldDefaults;
      const newRowSaveButton = document.getElementById(
        "newRowSaveButton" + _this.NOW,
      );
      const newRowCancelButton = document.getElementById(
        "newRowCancelButton" + _this.NOW,
      );
      newRowSaveButton.addEventListener("click", function (e) {
        _this.log(".init(): ", "newRowSaveButton clicked...");
        const missingRequiredFieldNames = [];
        let missingRequiredFields = false;
        const newRow = {};
        const gridIds = [];
        params.api.forEachNode(function (nextNode) {
          return gridIds.push(parseInt(nextNode.id));
        });
        newRow[AG_GRID_ROW_ID_FIELD] = Math.max.apply(Math, gridIds) + 1;
        _this.log(
          ".init(): ",
          "current grid ids: " +
            gridIds +
            ", new row id = " +
            newRow[AG_GRID_ROW_ID_FIELD],
        );
        document
          .querySelectorAll('[id^="newRowField_"]')
          .forEach(function (nextEl) {
            _this.log(
              ".init(): ",
              "next field: " + nextEl.name + " has a type of " + nextEl.type,
            );
            _this.log(
              ".init(): ",
              "setting next field: " +
                nextEl.name +
                " to " +
                (nextEl.type == "checkbox" ? nextEl.checked : nextEl.value),
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
            "Please provide a value for all the following required fields: " +
            missingRequiredFieldNames.join("; ");
          params.context.emitStatusMessage("error", alertMessage);
        } else {
          _this.log(
            ".init(): ",
            "newRow = " + JSON.stringify(newRow) + ", applying any defaults",
          );
          if (fieldDefaults) {
            Object.keys(fieldDefaults).forEach(function (nextDefault) {
              if (!newRow.hasOwnProperty(nextDefault) || !newRow[nextDefault]) {
                _this.log(
                  ".init(): ",
                  "adding nextDefault " +
                    nextDefault +
                    " of: " +
                    fieldDefaults[nextDefault],
                );
                newRow[nextDefault] = fieldDefaults[nextDefault];
              }
            });
            _this.log(
              ".init(): ",
              "after applying defaults, newRow = " + JSON.stringify(newRow),
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
      newRowCancelButton.addEventListener("click", function (e) {
        _this.log(".init(): ", "newRowCancelButton clicked...");
        params.api.closeToolPanel();
        document
          .querySelectorAll('[id^="newRowField_"]')
          .forEach(function (nextEl) {
            nextEl.value = "";
          });
      });
      Object.keys(_this.imgIds).forEach(function (nextField) {
        _this.log(
          ".init(): ",
          ".init(): adding event listeners for next image field: " + nextField,
        );
        const nextImagesIds = _this.imgIds[nextField];
        const buttonElem = document.getElementById(nextImagesIds.buttonId);
        const fileInputElem = document.getElementById(
          nextImagesIds.fileInputId,
        );
        buttonElem.addEventListener("click", function (e) {
          return _this.openFileInput(e, nextImagesIds.fileInputId);
        });
        fileInputElem.addEventListener("change", function (e) {
          return _this.startUpload(
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
    const _this = this;
    const logPrefix = ".buildEditElems(): ";
    this.log(logPrefix, "Starting...");
    const now = new Date().getTime();
    const fieldsToDisplay = this.params.fieldsToDisplay;
    const fieldDefinitions = this.params.fieldDefinitions;
    const fieldsRequired = this.params.fieldsRequired;
    const panelTitle = this.params.panelTitle;
    const gridConfig = this.params.context.getGridConfig();
    this.log(logPrefix, "fieldsToDisplay = " + JSON.stringify(fieldsToDisplay));
    const returnDiv = document.createElement("div");
    const headerLabelDiv = document.createElement("div");
    headerLabelDiv.className = "center";
    const headerButtonDiv = document.createElement("div");
    headerButtonDiv.className = "center";
    const headerLabel = document.createElement("h3");
    headerLabel.innerHTML = panelTitle || "New row values:";
    headerLabelDiv.appendChild(headerLabel);
    const applyButton = document.createElement("button");
    applyButton.id = "newRowSaveButton" + this.NOW;
    applyButton.className = "ptc-ok-button";
    applyButton.title = "Save new Row";
    const cancelButton = document.createElement("button");
    cancelButton.id = "newRowCancelButton" + this.NOW;
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
    Object.keys(fieldsToDisplay).forEach(function (nextField) {
      _this.log(logPrefix, `building edit element for ${nextField}`);
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
      _this.log(
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
        _this.log(
          logPrefix,
          `nextFieldColumnConfig is a checkbox or image renderer!: ${nextFieldColumnConfig.cellRenderer}`,
        );
        nextFieldCellEditor = nextFieldColumnConfig.cellRenderer;
      }
      if (nextFieldCellEditor) {
        _this.log(logPrefix, `it has a cellEditor of ${nextFieldCellEditor}`);
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
            selectOptions.forEach(function (nextOption) {
              _this.log(
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
            attributeSelectElem_1.id = "newRowField_" + nextField;
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
            imageElement.setAttribute("id", "newRowField_" + imgId);
            imageElement.style.height = "100%";
            imageElement.style.position = "absolute";
            nextFieldInputDiv.appendChild(inputDiv);
            nextFieldInputDiv.appendChild(fileButtonElement);
            nextFieldInputDiv.appendChild(imageElement);
            _this.imgIds[nextField] = {
              buttonId: buttonId + "",
              fileInputId: inputId + "",
              containerId: nextFieldDivId + "",
              imgId: "newRowField_" + imgId,
            };
            break;
          case "CustomCheckboxRenderer":
            const checkboxInputElem = document.createElement("input");
            checkboxInputElem.className = "vertical-middle";
            checkboxInputElem.type = "checkbox";
            checkboxInputElem.id = "newRowField_" + nextField;
            checkboxInputElem.name = nextField;
            nextFieldInputDiv.appendChild(checkboxInputElem);
            break;
          default:
            let textInput;
            (textInput = document.createElement("input")).className =
              "vertical-middle";
            textInput.type = "text";
            textInput.id = "newRowField_" + nextField;
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
            dateInput.id = "newRowField_" + nextField;
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
            textInput.id = "newRowField_" + nextField;
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
class CustomPasswordEditor {
  loggingEnabled: boolean;
  containerId: string;
  paramValue: any;
  cellHeight: string;
  container: any;
  input: any;
  logPrefix: string;

  constructor() {
    const _this = this;
    this.loggingEnabled = true;
    const now = new Date().getTime();
    this.containerId = (now + 1).toString();
  }
  log(message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomPasswordEditor",
      this.logPrefix,
      message,
    );
  }
  init(params) {
    const _this = this;
    this.logPrefix = ".init(" + params.column.getId() + "): ";
    this.loggingEnabled = params.context.EnableConsoleLogging;
    this.log("Starting...");
    this.paramValue = params.value;
    this.cellHeight = "100%";
    this.container = document.createElement("div");
    this.input = document.createElement("input");
    this.input.id = "input";
    this.input.type = "password";
    this.input.classList.add("ag-cell", "ag-text-field-input");
    this.input.value = this.paramValue ? this.paramValue : "";
    this.container.appendChild(this.input);
    this.input.addEventListener("input", function (event) {
      _this.paramValue = event.target.value;
    });
  }
  getGui() {
    return this.container;
  }
  afterGuiAttached() {
    this.input.focus();
  }
  getValue() {
    return this.paramValue;
  }
  destroy() {}
  isPopup() {
    return false;
  }
}
class CustomPasswordRenderer {
  loggingEnabled: boolean;
  logPrefix: string;
  containerId: string;
  paramValue: any;
  container: any;
  input: any;

  constructor() {
    const _this = this;
    this.loggingEnabled = true;
    const now = new Date().getTime();
    this.containerId = (now + 1).toString();
  }
  log(message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomPasswordRenderer",
      this.logPrefix,
      message,
    );
  }
  init(params) {
    this.logPrefix = ".init(" + params.column.getId() + "): ";
    this.log("Starting...");
    this.loggingEnabled = params.context.EnableConsoleLogging;
    this.paramValue = params.value;
    this.container = document.createElement("div");
    this.input = document.createElement("p");
    this.input.className = "ag-cell-value";
    this.input.innerHTML = "*".repeat(this.paramValue?.length ?? 0);
    this.container.appendChild(this.input);
  }
  getGui() {
    return this.container;
  }
}
class CustomIconCellRenderer {
  loggingEnabled: boolean;
  logPrefix: string;
  params: any;
  paramValue: any;
  container: any;

  constructor() {
    const _this = this;
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
    this.logPrefix = ".init(" + params.column.getId() + "): ";
    this.params = params;
    this.paramValue = params.value;
    this.loggingEnabled = this.params.context.EnableConsoleLogging;
    this.log("Starting, this.paramValue = " + JSON.stringify(this.paramValue));
    const stateField = this.params.stateField;
    const stateFieldValue = this.params.data[stateField];
    const iconMap = this.params.iconMap;
    const suffixImageField = this.params.suffixImage;
    const suffixMap = this.params.suffixMap;
    const suffixImageFieldValue = this.params.data[suffixImageField];
    const rowHeight = this.params.context.RowHeight;
    this.log("stateField = " + stateField + " = " + stateFieldValue);
    this.log("iconMap = " + JSON.stringify(iconMap));
    this.log(
      "suffixImageField = " + suffixImageField + " = " + suffixImageFieldValue,
    );
    this.log("suffixMap = " + JSON.stringify(suffixMap));
    this.container = document.createElement("div");
    this.container.style.justifyContent = "center";
    this.container.style.display = "flex";
    this.container.style.height = rowHeight + "px";
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
    imageBeforeTextLink.style.width = rowHeight + "px";
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
        "/Thingworx/MediaEntities/" + iconMap[stateFieldValue];
    } else if (stateFieldValue) {
      imageBeforeTextLink.src = stateFieldValue;
    }
    if (suffixImageFieldValue && suffixMap) {
      imageAfterText.src =
        "/Thingworx/MediaEntities/" + suffixMap[suffixImageFieldValue];
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
class CustomTreeColumnCellRenderer {
  loggingEnabled: boolean;
  params: any;
  container: any;
  logPrefix: string;

  constructor() {
    const _this = this;
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
    this.log("Starting, treeColumnValue = " + JSON.stringify(treeColumnValue));
    const stateField = this.params.stateField;
    const stateFieldValue = this.params.data[stateField];
    const iconMap = this.params.iconMap;
    const suffixImageField = this.params.suffixImage;
    const suffixMap = this.params.suffixMap;
    const suffixImageFieldValue = this.params.data[suffixImageField];
    const rowHeight = this.params.context.RowHeight;
    this.log("stateField = " + stateField + " = " + stateFieldValue);
    this.log("iconMap = " + JSON.stringify(iconMap));
    this.log(
      "suffixImageField = " + suffixImageField + " = " + suffixImageFieldValue,
    );
    this.log("suffixMap = " + JSON.stringify(suffixMap));
    this.container = document.createElement("div");
    this.container.style.justifyContent = "flex-start";
    this.container.style.display = "flex";
    this.container.style.height = rowHeight + "px";
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
    imageBeforeTextLink.style.width = rowHeight + "px";
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
      imageBeforeTextMedia.src = "/Thingworx/MediaEntities/" + iconPath;
    } else if (stateFieldValue) {
      imageBeforeTextLink.src = stateFieldValue;
    }
    if (suffixImageFieldValue && suffixMap) {
      imageAfterText.src =
        "/Thingworx/MediaEntities/" + suffixMap[suffixImageFieldValue];
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
class CustomNumberEditor {
  integerOnly: boolean;
  loggingEnabled: boolean;
  container: any;
  input: any;
  cancelBeforeStart: boolean;
  logPrefix: string;

  constructor() {
    const _this = this;
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
    const _this = this;
    this.logPrefix = ".init(" + params.column.getId() + "): ";
    this.loggingEnabled = params.context.EnableConsoleLogging;
    if (params.hasOwnProperty("integersOnly")) {
      this.integerOnly = params.integersOnly;
      this.log("params has integersOnly = " + this.integerOnly);
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
    this.input.addEventListener("keydown", function (event) {
      _this.input.classList.remove("invalid");
    });
    this.input.addEventListener("keypress", function (event) {
      if (_this.isKeyPressedNumeric(event)) {
        if (_this.isKeyPressedNavigation(event)) {
          event.stopPropagation();
        }
      } else {
        _this.input.focus();
        if (event.preventDefault) {
          event.preventDefault();
        }
      }
    });
    this.input.addEventListener("keyup", function (event) {
      const regex = new RegExp(/^-?\d{0,}(\.\d*)?(e\B)?(\d+)$/);
      if (_this.isKeyPressedNavigation(event)) {
        event.stopPropagation();
      } else {
        regex.test(_this.input.value);
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
      "inputVal = " + inputVal + ", this.integerOnly = " + this.integerOnly,
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
      "isCharNumeric, inputStr = " +
        inputStr +
        ", this.integerOnly = " +
        this.integerOnly,
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
class CustomLoadingCellRenderer {
  eGui: any;

  init(params) {
    this.eGui = document.createElement("div");
    this.eGui.innerHTML = `
            <div class="ag-custom-loading-cell" style="padding-left: 10px; line-height: 25px;">
                <i class="fas fa-spinner fa-pulse"></i>
                <span>${params.loadingMessage} </span>
            </div>
        `;
  }
  getGui() {
    return this.eGui;
  }
}
class CustomHyperlinkRenderer {
  logPrefix: string;
  loggingEnabled: boolean;
  arrowElement: Element;
  containerId: string;
  container: any;
  anchorLinkText: any;
  anchorElement: any;

  constructor() {
    const _this = this;
    this.loggingEnabled = true;
    this.arrowElement = index_es.Ay.icon(
      fontawesome_free_solid_index_es.AaJ,
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
    this.logPrefix = ".init(" + params.column.getId() + "): ";
    this.loggingEnabled = params.context.EnableConsoleLogging;
    this.container = document.createElement("div");
    this.container.setAttribute("id", this.containerId);
    this.container.setAttribute("tabindex", "0");
    this.container.style.textAlign = "left";
    this.container.style.verticalAlign = "middle";
    if (params.value) {
      this.log("Starting, params.value = " + params.value);
      if (params.hasOwnProperty("linkText")) {
        this.log("using linkText = " + params.linkText);
        this.anchorLinkText = params.linkText;
      }
      if (params.hasOwnProperty("linkFieldName")) {
        const linkFieldName = params.linkFieldName;
        this.log("using linkFieldName = " + linkFieldName);
        this.anchorLinkText = params.data[linkFieldName]
          ? params.data[linkFieldName]
          : params.value;
      }
      this.anchorElement = document.createElement("a");
      this.anchorElement.setAttribute("href", params.value);
      if (this.anchorLinkText) {
        this.log("using anchorLinkText = " + this.anchorLinkText);
        this.anchorElement.innerHTML = this.anchorLinkText;
      } else {
        this.log("using open external link icon");
        this.anchorElement.appendChild(this.arrowElement);
      }
      this.anchorElement.target = "_blank";
      this.container.appendChild(this.anchorElement);
      this.log("this.container = " + this.container);
    }
  }
  getGui() {
    return this.container;
  }
}
class CustomMashupStatusPanel {
  params: any;
  visible: boolean;
  eGui: any;
  eButton: any;

  init(params) {
    this.params = params;
    this.visible = true;
    this.eGui = document.createElement("div");
    const mashup = document.createElement("embed");
    mashup.src =
      "/Thingworx/Mashups/" +
      this.params.context.CustomMashupStatusPanelMashupName;
    this.eGui.appendChild(mashup);
  }
  getGui() {
    return this.eGui;
  }
  destroy() {
    this.eButton.removeEventListener("click", this.buttonListener);
  }
  buttonListener(arg0: string, buttonListener: any) {
    throw new Error("Method not implemented.");
  }
  onButtonClicked() {
    alert("Selected Row Count: " + this.params.api.getSelectedRows().length);
  }
  setVisible(visible) {
    this.visible = visible;
    this.eGui.style.display = this.visible ? "block" : "none";
  }
  isVisible() {
    return this.visible;
  }
}
class CustomServerSideTotalsStatusPanel {
  loggingEnabled: boolean;
  params: any;
  eGui: any;
  rowsLabel: any;

  constructor() {
    const _this = this;
    this.loggingEnabled = true;
  }
  log(prefix: string, message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomServerSideTotalsStatusPanel()",
      prefix,
      message,
    );
  }
  init(params) {
    this.params = params;
    this.loggingEnabled = this.params.context.EnableConsoleLogging;
    this.eGui = document.createElement("div");
    this.rowsLabel = document.createElement("span");
    this.rowsLabel.setAttribute("class", "ag-status-name-value-value");
    this.rowsLabel.innerHTML = "Rows: ";
    this.eGui.appendChild(this.rowsLabel);
    this.log(".init(): ", "Starting, this.eGui = " + JSON.stringify(this.eGui));
  }
  getGui() {
    return this.eGui;
  }
  destroy() {}
  updateRowCount(newTotals) {
    if (this.params.context.EnableConsoleLogging) {
      console.log(
        "CustomServerSideTotalsStatusPanel.updateRowCount(): newTotals = {}",
        newTotals,
      );
    }
    this.rowsLabel.innerHTML = "Rows: " + newTotals;
  }
}
class CustomLoadingOverlay {
  loggingEnabled: boolean;
  eGui: any;
  logPrefix: string;
  constructor() {
    const _this = this;
    this.loggingEnabled = true;
  }
  log(message: unknown): void {
    writeWidgetLog(
      this.loggingEnabled,
      "AgGridWidget.CustomLoadingOverlay",
      this.logPrefix,
      message,
    );
  }

  init(params) {
    this.logPrefix = ".init(): ";
    this.log(
      "Starting, creating overlay with message: " + params.loadingMessage,
    );
    this.eGui = document.createElement("div");
    this.eGui.innerHTML = `<div class="ag-overlay-loading-center" role="presentation">
               <div role="presentation" style="height:100px; width:100px; center / contain no-repeat; margin: 0 auto;"></div>
               <div aria-live="polite" aria-atomic="true">${params.loadingMessage}</div>
            </div>`;
    this.log("done...");
  }
  getGui() {
    return this.eGui;
  }
  refresh(params) {
    return false;
  }
}

const HEADERS = new Headers();
(function (VISIBLE_INDEX_POSITIONS) {
  VISIBLE_INDEX_POSITIONS.top = "top";
  VISIBLE_INDEX_POSITIONS.middle = "middle";
  VISIBLE_INDEX_POSITIONS.bottom = "bottom";
})((VISIBLE_INDEX_POSITIONS ||= {}));
HEADERS.append("Content-Type", "application/json");
HEADERS.append("Accept", "application/json");
const relatedDropdownValues = {};
@TWWidgetDefinition
class AgGridWidget2 extends TWRuntimeWidget {
  constructor(...args: any[]) {
    super(...args);
    const _this = this;
    _this.gridConfig = {
      ColumnConfig: {},
      RowConfig: {},
      GridOptions: {},
      EditableFieldsColumn: "",
      CellClassColumn: "",
      CellEditorSelectorConfig: {},
      CellRendererSelectorConfig: {},
      excelStyles: [],
    };
    _this.alreadyLoaded = false;
    this.onSelectionChanged = this.onSelectionChanged.bind(this);
    this.emitDoubleClickedEvent = this.emitDoubleClickedEvent.bind(this);
    this.updateUpdatedTable = this.updateUpdatedTable.bind(this);
    this.cellEditorSelector = this.cellEditorSelector.bind(this);
    this.cellRendererSelector = this.cellRendererSelector.bind(this);
    this.updateSelectedRows = this.updateSelectedRows.bind(this);
    this.getContextMenuItems = this.getContextMenuItems.bind(this);
    this.dateCellCallBack = this.dateCellCallBack.bind(this);
    this.cellValueChanged = this.debounce(
      this.cellValueChanged.bind(this),
      1000,
    );
  }
  log(prefix: string, message: unknown): void {
    writeWidgetLog(
      this.EnableConsoleLogging,
      "AgGridWidget-" + this.widgetId + ".",
      prefix,
      message,
    );
  }
  convertRowsToSelectToNumeric(rowsToSelect) {
    const logPrefix = ".convertRowsToSelectToNumeric(" + rowsToSelect + "): ";
    const returnArray = [];
    const singleInteger = Number(rowsToSelect);
    if (!Number.isNaN(singleInteger)) {
      returnArray.push(singleInteger - 1);
    }
    try {
      if (rowsToSelect.indexOf("-") > 0) {
        const startStopValues = rowsToSelect.split("-");
        const startValue = parseInt(startStopValues[0]);
        const endValue = parseInt(startStopValues[1]);
        this.log(
          logPrefix,
          "startValue = " + startValue + ", endValue = " + endValue,
        );
        for (let i = startValue; i < endValue + 1; i++) {
          this.log(logPrefix, "adding row to select # " + i);
          returnArray.push(i - 1);
        }
      }
      if (rowsToSelect.indexOf(",") > 0) {
        rowsToSelect.split(",").map(function (nextRowAsString) {
          return returnArray.push(parseInt(nextRowAsString) - 1);
        });
      }
    } catch (err) {
      this.log(logPrefix, "error selecting default rows, err = " + err);
    }
    return returnArray;
  }
  @service
  AddNewRow() {
    this.log(
      "AddNewRow(): ",
      "Adding new row, this.NewRowIndex = " + this.NewRowIndex,
    );
    const newRow = {};
    newRow[AG_GRID_ROW_ID_FIELD] = new Date().getTime();
    const newItems = [newRow];
    this.widgetGridApi.applyTransaction({
      add: newItems,
      addIndex: this.NewRowIndex ?? 0,
    });
    this.updateUpdatedTable();
    this.updateRenderedTable();
  }
  @service
  AddNewRowToBottom() {
    this.log(".AddNewRowToBottom(): ", "Adding new row to bottom of grid");
    const newRow = {};
    newRow[AG_GRID_ROW_ID_FIELD] = new Date().getTime();
    const newItems = [newRow];
    this.widgetGridApi.applyTransaction({
      add: newItems,
    });
    this.updateUpdatedTable();
    this.updateRenderedTable();
  }
  @service
  DeleteSelectedRows() {
    let _this = this;
    let _a;
    const logPrefix = "DeleteSelectedRows(): ";
    const selectedRows = this.widgetGridApi.getSelectedRows();
    this.log(
      logPrefix,
      "Deleting " +
        selectedRows.length +
        " rows, selectedRows = " +
        JSON.stringify(selectedRows),
    );
    if (this.gridOptions.rowModelType == "serverSide") {
      this.log(logPrefix, "deleting from server-side row model");
      this.widgetGridApi.applyServerSideTransaction({
        remove: selectedRows,
      });
    } else {
      this.log(logPrefix, "deleting from client-side row model");
      this.widgetGridApi.applyTransaction({
        remove: selectedRows,
      });
      this.updateUpdatedTable();
    }
    const currentEditedTableRows = this.EditedTable.rows;
    const updatedEditedTableRows = [];
    this.log(
      logPrefix,
      "deleting rows, currentEditedTableRows = " +
        JSON.stringify(currentEditedTableRows),
    );
    currentEditedTableRows.forEach(function (nextEditedRow) {
      _this.log(logPrefix, "nextEditedRow = " + JSON.stringify(nextEditedRow));
      let nextEditedRowWasDeleted = false;
      selectedRows.forEach(function (nextDeletedRow) {
        if (JSON.stringify(nextDeletedRow) == JSON.stringify(nextEditedRow)) {
          nextEditedRowWasDeleted = true;
        }
      });
      _this.log(
        logPrefix,
        "nextEditedRowWasDeleted = " + nextEditedRowWasDeleted,
      );
      if (!nextEditedRowWasDeleted) {
        updatedEditedTableRows.push(nextEditedRow);
      }
    });
    (_a = this.DeletedTable.rows).push.apply(_a, selectedRows);
    this.EditedTable.rows = updatedEditedTableRows;
    this.log(
      logPrefix,
      "deleting rows, now EditedTableRows = " +
        JSON.stringify(this.EditedTable.rows),
    );
  }
  @service
  ResizeAll() {
    this.log("ResizeAll(): ", "calling resize all...");
    try {
      this.widgetGridApi.sizeColumnsToFit();
    } catch (err) {
      this.log("ResizeAll(): ", "err: " + err);
    }
  }
  @service
  SaveState() {
    this.log("SaveState(): ", "saving state...");
    const newState: any = {};
    newState.ColumnState = this.widgetGridApi.getColumnState();
    newState.FilterState = this.widgetGridApi.getFilterModel();
    this.SavedState = JSON.stringify(newState);
    this.StateSaved();
    this.updateRenderedTable();
  }
  @service
  RestoreState() {
    const logPrefix = "RestoreState.Set(): ";
    this.log(logPrefix, "restoring saved state...");
    if (this.SavedState) {
      this.log(logPrefix, "restoring saved state: " + this.SavedState);
      try {
        const savedStateAsJson = JSON.parse(this.SavedState);
        if (
          this.widgetGridApi &&
          savedStateAsJson.hasOwnProperty("ColumnState")
        ) {
          this.log(
            logPrefix,
            "restoring columns to: " + JSON.stringify(savedStateAsJson),
          );
          this.widgetGridApi.applyColumnState({
            state: savedStateAsJson.ColumnState,
            applyOrder: true,
          });
        }
        if (savedStateAsJson.hasOwnProperty("FilterState")) {
          this.log(logPrefix, "restoring filters...");
          this.widgetGridApi.setFilterModel(savedStateAsJson.FilterState);
        }
      } catch (err) {
        this.log(logPrefix, "Error parsing JSON? err = " + err);
      }
    }
  }
  @service
  SizeAllColumns() {
    this.log("SizeAllColumns(): ", "resizing all columns...");
    const allColumnIds = [];
    this.widgetGridApi.getColumns().forEach(function (column) {
      allColumnIds.push(column.getId());
    });
    this.log("SizeAllColumns(): ", "allColumnIds = " + allColumnIds);
    this.widgetGridApi.autoSizeColumns(allColumnIds, false);
  }
  @service
  ClearAllFilters() {
    this.widgetGridApi.setFilterModel(null);
    this.widgetGridApi.onFilterChanged();
  }
  @service
  QuickFilterSearch() {
    this.widgetGridApi.setQuickFilter(this.QuickFilterText);
  }
  @service
  OpenSidePanel() {
    this.widgetGridApi.openToolPanel(this.TargetSideBarPanel);
  }
  @service
  CloseSidePanel() {
    this.widgetGridApi.closeToolPanel();
  }
  @service
  RefreshGrid() {
    this.widgetGridApi.destroy();
    this.widgetGridApi = createGrid(
      document.getElementById(this.jqElement[0].id),
      this.gridOptions,
    );
  }
  @service
  ExpandAllRowGroups() {
    this.widgetGridApi.expandAll();
  }
  @service
  CollapseAllRowGroups() {
    this.widgetGridApi.collapseAll();
  }
  @service
  ShowSideBar() {
    this.widgetGridApi.setSideBarVisible(true);
  }
  @service
  HideSideBar() {
    this.widgetGridApi.setSideBarVisible(false);
  }
  @service
  AddNewRowWithData() {
    const newRowData = JSON.parse(this.NewRowData);
    this.log(
      "AddNewRowWithData(): ",
      "adding new row with data, newRowData = " + JSON.stringify(newRowData),
    );
    this.widgetGridApi.applyTransaction({
      add: newRowData,
      addIndex: this.NewRowIndex ?? 0,
    });
    this.gridOptions.context.updateEditedTable(newRowData);
  }
  @service
  ClearDeletedTable() {
    this.DeletedTable = {
      dataShape: this.dataSourceDataShape,
      rows: [],
    };
  }
  @service
  RestoreDeletedTable() {
    const logPrefix = "RestoreDeletedTable(): ";
    this.log(
      logPrefix,
      "restoring deleted rows, this.DeletedTable = " + this.DeletedTable.rows,
    );
    const currentRows = [];
    this.widgetGridApi.forEachNode(function (node) {
      return currentRows.push(node.data);
    });
    this.DeletedTable.rows.forEach(function (nextDeletedRow) {
      return currentRows.push(nextDeletedRow);
    });
    this.widgetGridApi.setRowData(currentRows);
    this.log(logPrefix, "resseting deleted table...");
    this.DeletedTable = {
      dataShape: this.dataSourceDataShape,
      rows: [],
    };
    this.log(logPrefix, "done");
  }
  @service
  SelectSpecifiedRows() {
    const logPrefix = "SelectSpecifiedRows(" + this.RowsToSelect + "): ";
    this.log(
      logPrefix,
      "selecting specified rows, rowsToSelect = " + this.RowsToSelect,
    );
    const rowsToSelect = this.convertRowsToSelectToNumeric(this.RowsToSelect);
    this.log(
      logPrefix,
      "selecting specified rows, rowsToSelect = " + rowsToSelect,
    );
    this.widgetGridApi.forEachNode(function (node, index) {
      node.setSelected(rowsToSelect.includes(index));
    });
  }
  @service
  SelectRowsByFieldValues() {
    const _this = this;
    const logPrefix =
      "SelectRowsByFieldValues(" + this.FieldNameToUseForRowSelection + "): ";
    this.log(
      logPrefix,
      "selecting rows that match entries: " +
        this.FieldValuesToUseForRowSelection,
    );
    const valuesToSelect = this.FieldValuesToUseForRowSelection?.split(",");
    this.log(logPrefix, "valuesToSelect = " + valuesToSelect);
    this.widgetGridApi.forEachNode(function (node) {
      const rowFieldValue = node.data[_this.FieldNameToUseForRowSelection] + "";
      _this.log(
        logPrefix,
        "next row this field has a value of: " + rowFieldValue,
      );
      if (valuesToSelect.includes(rowFieldValue)) {
        _this.log(
          logPrefix,
          "this row matches, setting to expanded and selected",
        );
        for (let myParent = node.parent; myParent; ) {
          myParent.setExpanded(true);
          myParent = myParent.parent;
        }
        node.setSelected(true);
        node.setExpanded(true);
      }
    });
  }
  @service
  SelectRowsByArrayValues() {
    const _this = this;
    const logPrefix =
      "SelectRowsByArrayValues(" + this.FieldNameToUseForRowSelection + "): ";
    this.log(
      logPrefix,
      "selecting rows that match entries: " +
        this.FieldValuesToUseForRowSelection,
    );
    const valuesToSelect = JSON.parse(this.FieldValuesToUseForRowSelection);
    this.log(logPrefix, "valuesToSelect = " + valuesToSelect);
    this.widgetGridApi.forEachNode(function (node) {
      const rowFieldValue = node.data[_this.FieldNameToUseForRowSelection] + "";
      _this.log(
        logPrefix,
        "next row this field has a value of: " + rowFieldValue,
      );
      if (valuesToSelect.includes(rowFieldValue)) {
        _this.log(
          logPrefix,
          "this row matches, setting to expanded and selected",
        );
        for (let myParent = node.parent; myParent; ) {
          myParent.setExpanded(true);
          myParent = myParent.parent;
        }
        node.setSelected(true);
        node.setExpanded(true);
      }
    });
  }
  @service
  ScrollToSpecifiedRow() {
    const _this = this;
    const logPrefix = "ScrollToSpecifiedRow(" + this.ScrollIndex + "): ";
    const indexAsInteger = Number(this.ScrollIndex);
    const ScrollToPosition = this.ScrollPosition;
    this.log(logPrefix, "scrolling to row using position: " + ScrollToPosition);
    if (!Number.isNaN(indexAsInteger)) {
      if (this.ShowGridAsTree) {
        this.log(
          logPrefix,
          "grid is a tree, expanding index # " + this.ScrollIndex,
        );
        this.widgetGridApi.forEachNode(function (node, index) {
          if (index == _this.ScrollIndex) {
            _this.log(logPrefix, "node # matches, expanding row");
            _this.widgetGridApi.setRowNodeExpanded(node, true);
          }
        });
      }
      this.widgetGridApi.ensureIndexVisible(
        indexAsInteger,
        VISIBLE_INDEX_POSITIONS[ScrollToPosition],
      );
    }
    this.log(logPrefix, "done");
  }
  @service
  SelectNextRow() {
    const _this = this;
    const renderedGrid = this.widgetGridApi.getRenderedNodes();
    let nextRowIsRowToSelect = false;
    const lastSelectedRowId = this.indices[this.indices.length - 1];
    this.log("SelectNextRow(): ", "lastSelectedRowId = " + lastSelectedRowId);
    renderedGrid.every(function (rowNode) {
      const nextRowNodeId = parseInt(rowNode.id);
      _this.log(
        "SelectNextRow(): ",
        "next rendered grid row id = " + nextRowNodeId,
      );
      if (nextRowIsRowToSelect) {
        rowNode.setSelected(true, true);
        return false;
      } else {
        if (nextRowNodeId == lastSelectedRowId) {
          nextRowIsRowToSelect = true;
        }
        return true;
      }
    });
  }
  @service
  SelectPreviousRow() {
    const _this = this;
    const renderedGrid = this.widgetGridApi.getRenderedNodes().reverse();
    let nextRowIsRowToSelect = false;
    const firstSelectedRowId = this.indices[0];
    this.log(
      "SelectPreviousRow(): ",
      "firstSelectedRowId = " + firstSelectedRowId,
    );
    renderedGrid.every(function (rowNode) {
      const nextRowNodeId = parseInt(rowNode.id);
      _this.log(
        "SelectPreviousRow(): ",
        "next rendered grid row id = " + nextRowNodeId,
      );
      if (nextRowIsRowToSelect) {
        rowNode.setSelected(true, true);
        return false;
      } else {
        if (nextRowNodeId == firstSelectedRowId) {
          nextRowIsRowToSelect = true;
        }
        return true;
      }
    });
  }
  renderHtml() {
    return '<div id="myGrid" class="widget-content" style="width: 100%; height: 100%; overflow: visible !important;"></div>';
  }
  buildGrid() {
    const _this = this;
    const logPrefix = "buildGrid(): ";
    this.widgetId = this.jqElementId;
    if (this.Data) {
      try {
        this.log(logPrefix, "starting...");
        const dataAsJson = JSON.parse(JSON.stringify(this.Data));
        this.log(logPrefix, "dataAsJson = " + JSON.stringify(dataAsJson));
        this.log(
          logPrefix,
          "this.gridConfig = " + JSON.stringify(this.gridConfig),
        );
        this.dataSourceDataShape = dataAsJson.dataShape;
        this.originalDataRows = JSON.parse(JSON.stringify(dataAsJson.rows));
        this.log(
          logPrefix,
          "originalDataRows = " + JSON.stringify(this.originalDataRows),
        );
        const gridTheme = this.GridTheme ?? "ag-theme-alpine";
        this.log(
          logPrefix,
          "this.GridTheme = " +
            this.GridTheme +
            ", so gridTheme = " +
            gridTheme,
        );
        document.getElementById(this.jqElement[0].id).classList.add(gridTheme);
        this.EditedTable = {
          dataShape: this.dataSourceDataShape,
          rows: [],
        };
        this.DeletedTable = {
          dataShape: this.dataSourceDataShape,
          rows: [],
        };
        if (this.GridConfig) {
          this.gridConfig = JSON.parse(this.GridConfig);
        }
        this.log(
          logPrefix,
          "getGridOptions with this.widgetId = " +
            this.widgetId +
            ", alreadyLoaded = " +
            this.alreadyLoaded,
        );
        if (
          this.UseAsyncSetFilters &&
          this.gridConfig.hasOwnProperty("ColumnConfig")
        ) {
          const columnConfig_1 = this.gridConfig.ColumnConfig;
          this.log(
            logPrefix,
            "creating initialFilterValues, columnConfig = " +
              JSON.stringify(columnConfig_1),
          );
          const initialFilterValues_1 = {};
          Object.keys(columnConfig_1).forEach(function (nextColumn) {
            const nextColumnConfig = columnConfig_1[nextColumn];
            if (nextColumnConfig.filterParams?.hasOwnProperty("values")) {
              _this.log(
                logPrefix,
                "initialFilterValues: nextColumn " +
                  nextColumn +
                  " has filter params: " +
                  JSON.stringify(nextColumnConfig.filterParams),
              );
              const nextColumnFilterValues =
                nextColumnConfig.filterParams.values;
              _this.log(
                logPrefix,
                "initialFilterValues: adding values for nextColumn " +
                  nextColumn +
                  ": " +
                  JSON.stringify(nextColumnFilterValues),
              );
              initialFilterValues_1[nextColumn] = nextColumnFilterValues;
            }
          });
          this.log(
            logPrefix,
            "done creating initialFilterValues = " +
              JSON.stringify(initialFilterValues_1),
          );
          this.serverSideSetFilters = initialFilterValues_1;
        }
        this.gridOptions = createGridData(
          dataAsJson,
          this.ShowFloatingFilters,
          this.CheckBoxSelection,
          this.FilterType == "values",
          this.gridConfig,
          this.ShowGridAsTree,
          this.ChildField,
          this.ParentField,
          this.TreePathField,
          this.TreePathFieldDelimeter,
          this.TreeColumnField,
          this.DateFormat,
          this.EnableConsoleLogging,
          this.UseAsyncSetFilters,
          this.widgetId,
        );
        this.gridOptions.context = {
          EnableConsoleLogging: this.EnableConsoleLogging,
          TreeColumnField: this.TreeColumnField,
          DateFormat: this.DateFormat ? this.DateFormat : "DD-MM-YYYY",
          EditableFieldsColumn: this.gridConfig.EditableFieldsColumn,
          CellClassColumn: this.gridConfig.CellClassColumn,
          RowHeight: this.RowHeight,
          CustomMashupStatusPanelMashupName:
            this.CustomMashupStatusPanelMashupName,
          WidgetId: this.widgetId,
          DateMinDate: this.DateEditorsMinimumDate,
          DateMaxDate: this.DateEditorsMaximumDate,
          DateShowWeekNumbers: this.DateEditorShowWeekNumbers,
          emitCustomActionButtonClick: function (buttonName) {
            _this.log(
              logPrefix,
              "CustomActionCellButton clicked: " + buttonName,
            );
            _this.ClickedActionCellButton = buttonName;
            _this.ActionCellButtonClicked();
          },
          getGridConfig: function () {
            return _this.gridConfig;
          },
          updateUpdatedTable: function () {
            _this.updateUpdatedTable();
          },
          emitStatusMessage: function (messageLevel, statusMessage) {
            _this.StatusMessage = statusMessage;
            switch (messageLevel) {
              case "info":
                _this.ShowInfoMessage();
                break;
              case "warning":
                _this.ShowWarningMessage();
                break;
              case "error":
                _this.ShowErrorMessage();
            }
          },
          updateEditedTable: function (updatedNodeData) {
            const rowIdField =
              _this.gridOptions.rowModelType == "serverSide"
                ? _this.RowIdField
                : AG_GRID_ROW_ID_FIELD;
            _this.log(
              logPrefix,
              "based on whether or not this is a client-side or server-side grid, rowIdField = " +
                rowIdField,
            );
            _this.EditedTable.rows = _this.EditedTable.rows.map(
              function (nextRow) {
                return (
                  updatedNodeData.find(function (nextUpdatedNode) {
                    return nextRow[rowIdField] === nextUpdatedNode[rowIdField];
                  }) || nextRow
                );
              },
            );
            const existingRowIds = _this.EditedTable.rows.map(
              function (nextExistingRow) {
                return nextExistingRow[rowIdField];
              },
            );
            const updatedRowIds = updatedNodeData.map(
              function (nextUpdatedRow) {
                return nextUpdatedRow[rowIdField];
              },
            );
            const idsThatDoNotExistYet = updatedRowIds.filter(function (x) {
              return !existingRowIds.includes(x);
            });
            _this.log(
              logPrefix,
              "existingRowIds = " + JSON.stringify(existingRowIds),
            );
            _this.log(
              logPrefix,
              "updatedRowIds = " + JSON.stringify(updatedRowIds),
            );
            _this.log(
              logPrefix,
              "idsThatDoNotExistYet = " + JSON.stringify(idsThatDoNotExistYet),
            );
            updatedNodeData.forEach(function (nextUpdatedRow) {
              if (idsThatDoNotExistYet.includes(nextUpdatedRow[rowIdField])) {
                _this.EditedTable.rows.push(nextUpdatedRow);
              }
            });
            _this.updateUpdatedTable();
            _this.CellEdited();
          },
          getAsyncFilterValues: function (column) {
            return _this.serverSideSetFilters[column];
          },
          components: {
            DatePicker: DatePicker,
            CustomSelectEditor,
            CustomImageRenderer: CustomImageRenderer,
            CustomActionCellRenderer,
            CustomCheckboxRenderer,
            CustomColumnAggregateFooterRenderer:
              CustomColumnAggregateFooterRenderer,
            CustomDateRenderer: CustomDateRenderer,
            CustomMultiEditToolPanel,
            CustomNewRowToolPanel,
            CustomPasswordEditor,
            CustomPasswordRenderer,
            CustomIconCellRenderer,
            CustomTreeColumnCellRenderer,
            CustomNumberEditor,
            CustomLoadingCellRenderer,
            CustomHyperlinkRenderer,
            CustomMashupStatusPanel,
            CustomLoadingOverlay,
            CustomJsonRenderer: CustomJsonRenderer,
          },
        };
        this.log(
          logPrefix,
          "gridOptions.context = " + JSON.stringify(this.gridOptions.context),
        );
        this.gridOptions.defaultColDef = {
          sortable: true,
          resizable: this.ResizeColumns,
          suppressMovable: !this.ReorderColumns,
          filter: this.ShowFloatingFilters,
          editable: this.AllowEditing != "no",
        };
        this.gridOptions.rowGroupPanelShow = this.EnableRowGrouping
          ? "always"
          : "never";
        this.gridOptions.popupParent = document.querySelector("body");
        if (this.SideBarPanels !== "none") {
          this.log(logPrefix, "adding sidebar, panels = " + this.SideBarPanels);
          const toolPanels = [];
          if (this.SideBarPanels == "columns" || this.SideBarPanels == "both") {
            toolPanels.push({
              id: "columns",
              labelDefault: "Columns",
              labelKey: "columns",
              iconKey: "columns",
              toolPanel: "agColumnsToolPanel",
            });
          }
          if (this.SideBarPanels == "filter" || this.SideBarPanels == "both") {
            toolPanels.push({
              id: "filters",
              labelDefault: "Filters",
              labelKey: "filters",
              iconKey: "filter",
              toolPanel: "agFiltersToolPanel",
            });
          }
          this.gridOptions.sideBar = {
            toolPanels,
          };
        }
        if (this.RowSelection !== "none") {
          this.gridOptions.rowSelection =
            this.RowSelection == "single"
              ? "single"
              : this.RowSelection == "multiple"
                ? "multiple"
                : undefined;
          this.gridOptions.onSelectionChanged = this.onSelectionChanged;
        }
        if (this.AllowEditing == "row") {
          this.gridOptions.editType = "fullRow";
        }
        if (this.SingleClickEdit) {
          this.gridOptions.singleClickEdit = true;
        }
        if (this.RowHeight) {
          this.gridOptions.rowHeight = this.RowHeight;
        }
        if (this.EmitDoubleClickEvent) {
          this.gridOptions.onRowDoubleClicked = this.emitDoubleClickedEvent;
        }
        if (this.ShowGridAsTree) {
          this.gridOptions.treeData = true;
        }
        if (this.EnableCellTextSelection) {
          this.gridOptions.enableCellTextSelection = true;
        }
        if (this.EnableRangeSelection) {
          this.gridOptions.enableRangeSelection = true;
        }
        this.gridOptions.onRowDragEnd = this.updateUpdatedTable;
        this.gridOptions.columnDefs?.forEach(function (nextColumnDef, index) {
          if (
            nextColumnDef.cellEditorParams?.hasOwnProperty(
              "useCellEditorSelector",
            )
          ) {
            _this.log(
              logPrefix,
              "updating cellEditorSelector for column def # " + index,
            );
            _this.gridOptions.columnDefs[index].cellEditorSelector =
              _this.cellEditorSelector;
          }
          if (
            nextColumnDef.cellRendererParams?.hasOwnProperty(
              "useCellRendererSelector",
            )
          ) {
            _this.log(
              logPrefix,
              "updating cellRendererSelector for column def # " + index,
            );
            _this.gridOptions.columnDefs[index].cellRendererSelector =
              _this.cellRendererSelector;
          }
        });
        this.gridOptions.components = this.gridOptions.context.components;
        if (this.gridConfig.hasOwnProperty("GridOptions")) {
          const gridConfigOptions = this.gridConfig.GridOptions;
          this.log(
            logPrefix,
            "adding/overriding grid options specified in gridconfig...",
          );
          for (const prop in gridConfigOptions) {
            this.log(logPrefix, "overriding next gridconfig prop " + prop);
            this.gridOptions[prop] = gridConfigOptions[prop];
          }
          if (gridConfigOptions.hasOwnProperty("isRowSelectableField")) {
            const isSelectableField_1 = gridConfigOptions.isRowSelectableField;
            this.log(
              logPrefix,
              "grid options has isRowSelectableField, defining isSelectable function, isSelectableField = " +
                isSelectableField_1,
            );
            this.gridOptions.isRowSelectable = function (rowNode) {
              const rowIsSelectable = rowNode.data[isSelectableField_1];
              _this.log(
                logPrefix,
                "checking if rowNode.id " +
                  rowNode.id +
                  " is rowSelectable? = " +
                  rowIsSelectable,
              );
              return rowIsSelectable;
            };
          }
        }
        if (this.ShowServerSideRowTotalsStatusPanel) {
          this.log(
            logPrefix,
            "ShowServerSideRowTotalsStatusPanel is true, adding it",
          );
          const currentStatusPanels = this.gridOptions.statusBar.statusPanels;
          currentStatusPanels.push({
            statusPanel: CustomServerSideTotalsStatusPanel,
            key: SERVER_SIDE_ROW_COUNT_STATUS_PANEL_ID,
            align: "left",
          });
          this.gridOptions.statusBar.statusPanels = currentStatusPanels;
          this.log(
            logPrefix,
            "statusPanels = " +
              JSON.stringify(this.gridOptions.statusBar.statusPanels),
          );
        }
        if (this.ShowGridAsTree) {
          this.log(logPrefix, "grid is a tree, updating innerrenderer...");
          if (!this.gridOptions.hasOwnProperty("autoGroupColumnDef")) {
            this.gridOptions.autoGroupColumnDef = {};
          }
          if (
            !this.gridOptions.autoGroupColumnDef.hasOwnProperty(
              "cellRendererParams",
            )
          ) {
            this.gridOptions.autoGroupColumnDef.cellRendererParams = {};
          }
          this.gridOptions.autoGroupColumnDef.cellRendererParams.innerRenderer =
            "CustomTreeColumnCellRenderer";
          this.gridOptions.autoGroupColumnDef.filterValueGetter = function (
            params,
          ) {
            return params.data[_this.TreeColumnField];
          };
        }
        this.gridOptions.onCellValueChanged = this.cellValueChanged;
        this.gridOptions.onRowSelected = this.updateSelectedRows;
        this.gridOptions.onFilterChanged = function () {
          _this.log("filterChanged(): ", "onFilterChanged event emitted...");
          _this.updateFilteredTable();
          _this.SaveState();
        };
        this.gridOptions.onSortChanged = function () {
          _this.log(logPrefix, "onSortChanged event emitted...");
          _this.SaveState();
        };
        this.gridOptions.onColumnEverythingChanged = function () {
          _this.log(logPrefix, "onColumnEverythingChanged event emitted...");
          _this.SaveState();
        };
        this.gridOptions.onColumnPivotChanged = function () {
          _this.log(logPrefix, "onColumnPivotChanged event emitted...");
          _this.SaveState();
        };
        this.gridOptions.onColumnPivotModeChanged = function () {
          _this.log(logPrefix, "onColumnPivotModeChanged event emitted...");
          _this.SaveState();
        };
        this.gridOptions.onColumnMoved = function () {
          _this.log(logPrefix, "onColumnMoved event emitted...");
          _this.SaveState();
        };
        this.gridOptions.onColumnResized = function () {
          _this.log(logPrefix, "onColumnResized event emitted...");
          _this.SaveState();
        };
        this.gridOptions.onColumnPinned = function () {
          _this.log(logPrefix, "onColumnPinned event emitted...");
          _this.SaveState();
        };
        this.gridOptions.onPasteEnd = function () {
          const gridRowCount = _this.widgetGridApi.getDisplayedRowCount() - 1;
          _this.log(
            logPrefix,
            "onPasteEnd event emitted, gridRowCount = " + gridRowCount,
          );
          for (let visibleRowIndex = 20; gridRowCount > visibleRowIndex; ) {
            _this.log(
              logPrefix,
              "onPasteEnd ensureIndexVisible index = " + visibleRowIndex,
            );
            _this.widgetGridApi.ensureIndexVisible(visibleRowIndex, "bottom");
            visibleRowIndex += 20;
          }
          _this.widgetGridApi.ensureIndexVisible(gridRowCount, "bottom");
          _this.log(
            logPrefix,
            "onPasteEnd event done, updating updated table...",
          );
          _this.updateUpdatedTableOnDataChange();
          if (_this.ReindexOnPaste) {
            _this.reindexOnPaste();
          }
        };
        this.gridOptions.onGridReady = function (event) {
          _this.log(logPrefix, "onGridReady...");
          if (_this.AutoOpenSideBar) {
            event.api.openToolPanel("filters");
          } else {
            event.api.closeToolPanel();
          }
          _this.SaveState();
          _this.updateFilteredTable();
          _this.DataFiltered();
          if (_this.ShowGridAsTree) {
            _this.updateUpdatedTable();
          }
          if (_this.DefaultSelectedRows) {
            _this.log(
              logPrefix,
              "selecting default rows, DefaultSelectedRows = " +
                _this.DefaultSelectedRows,
            );
            try {
              const rowsToSelect_1 = _this.convertRowsToSelectToNumeric(
                _this.DefaultSelectedRows,
              );
              _this.log(
                logPrefix,
                "selecting default rows, rowsToSelect = " + rowsToSelect_1,
              );
              event.api.forEachNode(function (node, index) {
                node.setSelected(rowsToSelect_1.includes(index));
              });
            } catch (err) {
              _this.log(
                logPrefix,
                "error selecting default rows, err = " + err,
              );
            }
          }
          _this.NoRowSelected = event.api.getSelectedNodes().length == 0;
          _this.AnyRowSelected = event.api.getSelectedNodes().length > 0;
          _this.updateUpdatedTableOnDataChange();
          if (_this.AutoOpenSideBar) {
            _this.log(logPrefix, "AutoOpenSideBar is true, opening it");
            const panelToOpen =
              _this.SideBarPanels == "filters" ? "filters" : "columns";
            _this.widgetGridApi.openToolPanel(panelToOpen);
          }
          if (_this.ResizeGridWithParent) {
            _this.log(
              logPrefix,
              "since ResizeGridWithParent is true, resizing grid",
            );
            _this.widgetGridApi.sizeColumnsToFit();
          }
        };
        this.gridOptions.getRowId = function (params) {
          _this.log(
            "getRowId(): ",
            "params.data = " + JSON.stringify(params.data),
          );
          return params.data[AG_GRID_ROW_ID_FIELD].toString();
        };
        if (this.gridOptions.rowModelType == "serverSide") {
          this.log(
            logPrefix,
            "grid row model is server side, setting ServerSideDataSource, this.RowIdField = " +
              this.RowIdField,
          );
          this.gridOptions.getRowId = function (params) {
            return params.data[_this.RowIdField];
          };
          const serverSideDataSource = this.createServerSideDatasource(
            this.ServerSideDataSource,
            this.ServerSideFiltersDataSource,
          );
          this.gridOptions.serverSideDatasource = serverSideDataSource;
          this.gridOptions.getContextMenuItems = this.getContextMenuItems;
        }
        this.log(
          logPrefix,
          "this.gridOptions = " + JSON.stringify(this.gridOptions),
        );
        const container = document.getElementById(this.jqElement[0].id);
        container.innerHTML = "";
        this.widgetGridApi = createGrid(container, this.gridOptions);
        this.alreadyLoaded = true;
        if (this.ResizeGridWithParent) {
          this.widgetGridApi.onGridSizeChanged = function () {
            _this.log(logPrefix, "onGridSizeChanged event called");
            _this.widgetGridApi.sizeColumnsToFit();
          };
        }
        this.log(logPrefix, "done...");
        this.GridLoaded();
      } catch (err) {
        console.error("Error building grid, err = ", err);
      }
    }
  }
  createServerSideDatasource(thingworxService, thingworxFilterService) {
    const _this = this;
    this.log(
      "createServerSideDatasource(): ",
      "Creating server side data source, thingworxService = " +
        thingworxService,
    );
    return {
      getRows: function (params) {
        const logPrefix = "getRows(): ";
        _this.log(
          logPrefix,
          "rows requested by grid: " + JSON.stringify(params.request),
        );
        const requestParams = Object.keys(params.request);
        _this.widgetGridApi
          .getStatusPanel(SERVER_SIDE_ROW_COUNT_STATUS_PANEL_ID)
          .updateRowCount("calculating...");
        const request = thingworxService;
        const body = {};
        requestParams.forEach(function (nextParam, index) {
          const nextParamValue = params.request[nextParam];
          _this.log(
            logPrefix,
            index +
              ". nextParam  " +
              nextParam +
              " = " +
              nextParamValue +
              " (" +
              typeof nextParamValue +
              ")",
          );
          _this.log(
            logPrefix,
            index + ". nextParam isArray? = " + Array.isArray(nextParamValue),
          );
          if (nextParamValue !== "") {
            body[nextParam] = JSON.stringify(nextParamValue);
          }
        });
        _this.currentServerSideFilterParams = body;
        if (_this.ServerSideAdditionalRequestParams) {
          _this.log(
            logPrefix,
            "adding additional params specified by ServerSideAdditionalRequestParams: " +
              _this.ServerSideAdditionalRequestParams,
          );
          const additionalParams_1 = JSON.parse(
            _this.ServerSideAdditionalRequestParams,
          );
          Object.keys(additionalParams_1).forEach(
            function (nextAdditionalParam) {
              _this.log(
                logPrefix,
                "adding nextAdditionalParam: " + nextAdditionalParam,
              );
              body[nextAdditionalParam] =
                additionalParams_1[nextAdditionalParam];
            },
          );
        }
        _this.log(
          logPrefix,
          "calling request = " +
            request +
            " with body = " +
            JSON.stringify(body),
        );
        _this.widgetGridApi.showLoadingOverlay();
        fetch(request, {
          method: "POST",
          headers: HEADERS,
          body: JSON.stringify(body),
        }).then(function (response) {
          response.json().then(function (data) {
            if (response.status !== 200) {
              _this.log(
                logPrefix,
                "getRows request failed, error = " +
                  JSON.stringify(response.json()),
              );
              params.fail();
            } else {
              _this.log(logPrefix, "getRows request succeeded!");
              const lastRowIndex = _this.getLastRowIndex(
                params.request,
                data.rows,
              );
              params.success({
                rowData: data.rows,
                rowCount: lastRowIndex,
              });
              _this.log(
                logPrefix,
                "returning " +
                  data.rows.length +
                  " rows, lastRowIndex = " +
                  lastRowIndex,
              );
              _this.ServerSideRowModelGridUpdated();
              _this.widgetGridApi.hideOverlay();
              if (_this.UseAsyncSetFilters) {
                _this.updateAsyncColumnFilters(thingworxFilterService);
              }
            }
          });
        });
      },
    };
  }
  updateAsyncColumnFilters(thingworxFilterService) {
    const _this = this;
    const logPrefix = "getRows().updateAsyncColumnFilters(): ";
    const currentlyFilteredColumns = Object.keys(
      this.widgetGridApi.getFilterModel(),
    );
    this.log(
      logPrefix,
      "since UseAsyncSetFilters is true, updating filter set values, currentlyFilteredColumns = " +
        JSON.stringify(currentlyFilteredColumns),
    );
    const filterRequest = thingworxFilterService;
    this.log(logPrefix, "calling filterRequest: " + filterRequest);
    return fetch(filterRequest, {
      method: "POST",
      headers: HEADERS,
    }).then(function (response) {
      response.json().then(function (data) {
        if (response.status !== 200) {
          _this.log(
            logPrefix,
            "filterRequest failed, error = " + JSON.stringify(response.json()),
          );
        } else {
          _this.log(
            logPrefix,
            "filterRequest succeeded, response = " + JSON.stringify(data),
          );
          if (Object.keys(data).length > 0 && data.hasOwnProperty("rows")) {
            const updatedFilters_1 = JSON.parse(data.rows[0].result);
            _this.log(
              logPrefix,
              "updatedFilters = " + JSON.stringify(updatedFilters_1),
            );
            _this.serverSideSetFilters = updatedFilters_1;
            Object.keys(updatedFilters_1).forEach(function (nextColumn) {
              if (currentlyFilteredColumns.includes(nextColumn)) {
                _this.log(
                  logPrefix,
                  "skipping nextColumn because it is already filtered: " +
                    nextColumn,
                );
              } else {
                _this.log(
                  logPrefix,
                  "updating nextColumn filter " +
                    nextColumn +
                    " with values: " +
                    updatedFilters_1[nextColumn],
                );
                _this.widgetGridApi
                  .getColumnFilterInstance(nextColumn)
                  .then(function (nextColumn) {
                    nextColumn.refreshFilterValues();
                  });
              }
            });
          }
        }
      });
    });
  }
  getContextMenuItems(params) {
    const _this = this;
    return [
      "cut",
      "copy",
      "copyWithHeaders",
      "paste",
      "separator",
      {
        name: "Export",
        icon: '<span class="ag-icon ag-icon-save" unselectable="on" role="presentation"></span>',
        subMenu: [
          {
            name: "Export To CSV",
            action: function () {
              return _this.exportServerSideGrid("csv");
            },
            icon: '<span class="ag-icon ag-icon-csv" unselectable="on" role="presentation"></span>',
          },
          {
            name: "Export To Excel",
            action: function () {
              return _this.exportServerSideGrid("excel");
            },
            icon: '<span class="ag-icon ag-icon-excel" unselectable="on" role="presentation"></span>',
          },
        ],
      },
    ];
  }
  dateCellCallBack(params) {
    this.log(
      "dateCellCallBack(): params.column.colDef = ",
      JSON.stringify(params.column.colDef),
    );
    if (params.column.colDef.baseType == "Date") {
      const value = params.value;
      let excelFormatDate = "";
      this.log("dateCellCallBack(): converting date, value = ", value);
      if (value) {
        excelFormatDate = this.excelDateConverter(new Date(value));
        this.log("dateCellCallBack(): formatted date = ", excelFormatDate);
      }
      return excelFormatDate;
    }
    return params.value;
  }
  pad(num, size) {
    for (var s = num + ""; s.length < size; ) {
      s = "0" + s;
    }
    return s;
  }
  excelDateConverter(value) {
    const fullYear = value.getUTCFullYear();
    const month = this.pad(value.getUTCMonth() + 1, 2);
    const date = this.pad(value.getUTCDate(), 2);
    const hours = this.pad(value.getUTCHours(), 2);
    const minutes = this.pad(value.getUTCMinutes(), 2);
    const seconds = this.pad(value.getUTCSeconds(), 2);
    const milliseconds = this.pad(value.getUTCMilliseconds(), 3);
    const excelFormatDate = `${fullYear}-${month}-${date}T${hours}:${minutes}:${seconds}.${milliseconds}`;
    console.log(excelFormatDate);
    return excelFormatDate;
  }
  exportServerSideGrid(format) {
    const _this = this;
    const logPrefix = "exportServerSideGrid(" + format + "): ";
    this.log(logPrefix, "Starting...");
    const exportDiv = document.createElement("div");
    const request = this.ServerSideDataSource;
    const currentFilterRequestBody = this.currentServerSideFilterParams;
    currentFilterRequestBody.startRow = 0;
    currentFilterRequestBody.endRow = 0;
    currentFilterRequestBody.columnState = this.widgetGridApi.getColumnState();
    if (this.EnableConsoleLogging) {
      this.log(
        logPrefix,
        "sending request = " +
          request +
          " with body: " +
          JSON.stringify(currentFilterRequestBody),
      );
    }
    this.widgetGridApi.showLoadingOverlay();
    fetch(request, {
      method: "POST",
      headers: HEADERS,
      body: JSON.stringify(currentFilterRequestBody),
    })
      .then(function (response) {
        if (response.status !== 200) {
          if (_this.EnableConsoleLogging) {
            _this.log(
              logPrefix,
              "error, response = " + JSON.stringify(response),
            );
          }
          _this.widgetGridApi.hideOverlay();
          _this.ShowErrorMessage();
          return;
        }
        response.json().then(function (data) {
          if (_this.EnableConsoleLogging) {
            _this.log(logPrefix, "got response, building export grid");
          }
          const rows = [];
          data.rows.forEach(function (nextRow, index) {
            nextRow[AG_GRID_ROW_ID_FIELD] = index;
            if (nextRow.hasOwnProperty("cellDataType")) {
              delete nextRow.cellDataType;
            }
            rows.push(nextRow);
          });
          if (_this.EnableConsoleLogging) {
            _this.log(
              logPrefix,
              "export grid will have " + rows.length + " rows",
            );
          }
          let exportGridConfig: any = {};
          const gridOptionsCopy = JSON.parse(JSON.stringify(_this.gridOptions));
          const gridOptionPropertiesToIgnore = [
            "statusBar",
            "toolPanelParams",
            "rowData",
            "rowClassRules",
            "cacheBlockSize",
            "serverSideDatasource",
          ];
          if (_this.EnableConsoleLogging) {
            _this.log(
              logPrefix,
              "1. gridOptionsCopy = " + JSON.stringify(gridOptionsCopy),
            );
          }
          Object.keys(gridOptionsCopy)
            .filter(function (nextProp) {
              return !gridOptionPropertiesToIgnore.includes(nextProp);
            })
            .forEach(function (nextProp) {
              exportGridConfig[nextProp] = gridOptionsCopy[nextProp];
            });
          if (_this.EnableConsoleLogging) {
            _this.log(
              logPrefix,
              "2. exportGridConfig = " + JSON.stringify(exportGridConfig),
            );
          }
          if (_this.EnableConsoleLogging) {
            _this.log(logPrefix, "2. rows = " + JSON.stringify(rows));
          }
          exportGridConfig.rowData = rows;
          exportGridConfig.rowModelType = "clientSide";
          delete exportGridConfig.cacheBlockSize;
          delete exportGridConfig.serverSideDatasource;
          if (_this.EnableConsoleLogging) {
            _this.log(
              logPrefix,
              "3. exportGridConfig = " + JSON.stringify(exportGridConfig),
            );
          }
          const addedColumns = [];
          if (_this.ServerSideDynamicExportColumnsPrefix) {
            if (_this.EnableConsoleLogging) {
              _this.log(
                logPrefix,
                "adding in dynamically-generated columns, ServerSideDynamicExportColumnsPrefix = " +
                  _this.ServerSideDynamicExportColumnsPrefix,
              );
            }
            const columnPrefixesToAdd_1 = JSON.parse(
              _this.ServerSideDynamicExportColumnsPrefix,
            );
            const datashapeFields = Object.keys(
              data.dataShape.fieldDefinitions,
            );
            if (_this.EnableConsoleLogging) {
              _this.log(
                logPrefix,
                "datashapeFields = " + JSON.stringify(datashapeFields),
              );
            }
            datashapeFields.forEach(function (nextField) {
              if (_this.EnableConsoleLogging) {
                _this.log(
                  logPrefix,
                  "checking if we should add nextField = " + nextField,
                );
              }
              if (
                columnPrefixesToAdd_1.some(function (nextPrefix) {
                  return nextField.startsWith(nextPrefix);
                })
              ) {
                if (_this.EnableConsoleLogging) {
                  _this.log(
                    logPrefix,
                    "yes! we should add nextField = " + nextField,
                  );
                }
                exportGridConfig.columnDefs.push({
                  field: nextField,
                  hide: false,
                });
                addedColumns.push(nextField);
              }
            });
          }
          if (_this.EnableConsoleLogging) {
            _this.log(
              logPrefix,
              "creating export grid, exportGridConfig = " +
                JSON.stringify(exportGridConfig),
            );
          }
          try {
            let exportedGrid = createGrid(exportDiv, exportGridConfig);
            if (_this.EnableConsoleLogging) {
              _this.log(
                logPrefix,
                "created export grid which has " +
                  exportedGrid.getDisplayedRowCount() +
                  " rows",
              );
            }
            if (_this.SavedState) {
              if (_this.EnableConsoleLogging) {
                _this.log(
                  logPrefix,
                  "restoring saved state: " + _this.SavedState,
                );
              }
              try {
                const savedStateAsJson_1 = JSON.parse(_this.SavedState);
                const columnStates_1 = savedStateAsJson_1.ColumnState;
                if (savedStateAsJson_1?.hasOwnProperty("ColumnState")) {
                  if (addedColumns.length) {
                    addedColumns.forEach(function (nextAddedColumn) {
                      columnStates_1.push({
                        colId: nextAddedColumn,
                        hide: false,
                      });
                    });
                  }
                  exportedGrid.applyColumnState({
                    state: columnStates_1,
                    applyOrder: true,
                  });
                  if (savedStateAsJson_1.hasOwnProperty("FilterState")) {
                    if (_this.EnableConsoleLogging) {
                      _this.log(logPrefix, "restoring filters...");
                    }
                    const savedFilters_1 = savedStateAsJson_1.FilterState;
                    Object.keys(savedFilters_1).forEach(function (nextFilter) {
                      if (
                        savedFilters_1[nextFilter].filterType == "date" &&
                        savedFilters_1[nextFilter].type == "blank"
                      ) {
                        if (_this.EnableConsoleLogging) {
                          _this.log(
                            logPrefix,
                            "next filters " +
                              nextFilter +
                              " is date=blank so replacing it with epoch 0",
                          );
                        }
                        savedStateAsJson_1.FilterState[nextFilter] = {
                          dateFrom: "0",
                          dateTo: null,
                          filterType: "date",
                          type: "equals",
                        };
                      }
                    });
                    exportedGrid.setFilterModel(savedStateAsJson_1.FilterState);
                  }
                  if (_this.EnableConsoleLogging) {
                    _this.log(
                      logPrefix,
                      "restoring saved state: " + _this.SavedState,
                    );
                  }
                  if (_this.EnableConsoleLogging) {
                    _this.log(
                      logPrefix,
                      "after setFilterModel grid has " +
                        exportedGrid.getDisplayedRowCount() +
                        " rows",
                    );
                  }
                }
              } catch (err) {
                if (_this.EnableConsoleLogging) {
                  _this.log(logPrefix, "Error parsing JSON? err = " + err);
                }
                _this.widgetGridApi.hideOverlay();
              }
            }
            if (_this.EnableConsoleLogging) {
              _this.log(
                logPrefix,
                "exporting grid which has " +
                  exportedGrid.getDisplayedRowCount() +
                  " rows",
              );
            }
            if (format == "excel") {
              exportedGrid.exportDataAsExcel({
                processCellCallback: _this.dateCellCallBack,
              });
            }
            if (format == "csv") {
              exportedGrid.exportDataAsCsv({
                processCellCallback: _this.dateCellCallBack,
              });
            }
            exportedGrid = null;
            exportGridConfig = null;
            _this.widgetGridApi.hideOverlay();
          } catch (err) {
            if (_this.EnableConsoleLogging) {
              _this.log(logPrefix, "error exporting grid: " + err);
            }
          }
        });
      })
      .catch(function (err) {
        this.log(logPrefix, "err: " + err);
      });
  }
  getLastRowIndex(request, results) {
    if (results) {
      const currentLastRow = (request.startRow ?? 0) + results.length;
      if (currentLastRow < (request.endRow ?? 0)) {
        return currentLastRow;
      } else {
        return undefined;
      }
    }
  }
  emitDoubleClickedEvent(params) {
    this.log(
      "emitDoubleClickedEvent(): ",
      "Row double-clicked, emitting rowdoubleclicked event...",
    );
    this.RowDoubleClicked();
  }
  onSelectionChanged() {
    const logPrefix = "onSelectionChanged(): ";
    const selectedRows = this.widgetGridApi.getSelectedNodes();
    this.log(logPrefix, "selectedRows has " + selectedRows.length + " rows");
    this.indices = selectedRows.map(function (nextRow) {
      return parseFloat(nextRow.id);
    });
    this.log(logPrefix, "indices = " + JSON.stringify(this.indices));
    this.NoRowSelected = this.indices.length == 0;
    this.AnyRowSelected = this.indices.length > 0;
    this.log(logPrefix, "calling updateSelection");
    this.updateSelection("Data", this.indices);
    this.SelectedRowsChanged();
    this.log(logPrefix, "done calling updateSelection");
    const renderedGrid = this.widgetGridApi.getRenderedNodes();
    const firstRenderedRowId = parseInt(renderedGrid[0].id);
    const lastRenderedRowId = parseInt(
      renderedGrid[renderedGrid.length - 1].id,
    );
    this.log(logPrefix, "firstRenderedRowId = " + firstRenderedRowId);
    this.log(logPrefix, "lastRenderedRowId = " + lastRenderedRowId);
    this.FirstRowSelected = false;
    this.LastRowSelected = false;
    if (this.indices.includes(firstRenderedRowId)) {
      this.log(
        logPrefix,
        "firstRenderedRowId is in selected row ids, setting FirstRowSelected to true",
      );
      this.FirstRowSelected = true;
    }
    if (this.indices.includes(lastRenderedRowId)) {
      this.log(
        logPrefix,
        "lastRenderedRowId is in selected row ids, setting LastRowSelected to true",
      );
      this.LastRowSelected = true;
    }
  }
  onFirstDataRendered(params) {
    if (
      this.ShowGridAsTree &&
      this.gridOptions.hasOwnProperty("groupDefaultExpanded")
    ) {
      const levelsToExpand_1 = this.gridOptions.groupDefaultExpanded;
      this.log(
        "onFirstDataRendered(): ",
        "setting nodes expanded/collapsed, levelsToExpand = " +
          levelsToExpand_1,
      );
      this.widgetGridApi.forEachNode(function (node, index) {
        const nodeLevel = node.level;
        const expandThisNode =
          levelsToExpand_1 == -1 || nodeLevel < levelsToExpand_1;
        node.setExpanded(expandThisNode);
      });
    }
  }
  debounce(callback, wait) {
    let timeoutId = null;
    return function () {
      const args = [];
      for (let _i = 0; _i < arguments.length; _i++) {
        args[_i] = arguments[_i];
      }
      window.clearTimeout(timeoutId);
      timeoutId = window.setTimeout(function () {
        callback.apply(undefined, args);
      }, wait);
    };
  }
  cellValueChanged(event) {
    const _this = this;
    const logPrefix = "cellValueChanged(): ";
    this.log(logPrefix, "cell was changed");
    this.log(logPrefix, "old value = " + event.oldValue);
    this.log(logPrefix, "new value = " + event.newValue);
    this.log(logPrefix, "source = " + event.source);
    event.node.data.changed = true;
    const updatedRows = [];
    this.widgetGridApi.forEachNode(function (node) {
      if (node.data && node.data.changed) {
        _this.log(
          logPrefix,
          "updating edited table, next node = " + JSON.stringify(node.data),
        );
        _this.log(
          logPrefix,
          "since this row was changed, adding it to edited table",
        );
        updatedRows.push(node.data);
      }
    });
    const rowIdField =
      this.gridOptions.rowModelType == "serverSide"
        ? this.RowIdField
        : AG_GRID_ROW_ID_FIELD;
    this.log(
      logPrefix,
      "based on whether or not this is a client-side or server-side grid, rowIdField = " +
        rowIdField,
    );
    this.EditedTable.rows = this.EditedTable.rows.map(function (nextRow) {
      return (
        updatedRows.find(function (nextUpdatedNode) {
          return nextRow[rowIdField] === nextUpdatedNode[rowIdField];
        }) || nextRow
      );
    });
    const existingRowIds = this.EditedTable.rows.map(
      function (nextExistingRow) {
        return nextExistingRow[rowIdField];
      },
    );
    const updatedRowIds = updatedRows.map(function (nextUpdatedRow) {
      return nextUpdatedRow[rowIdField];
    });
    const idsThatDoNotExistYet = updatedRowIds.filter(function (x) {
      return !existingRowIds.includes(x);
    });
    this.log(logPrefix, "existingRowIds = " + JSON.stringify(existingRowIds));
    this.log(logPrefix, "updatedRowIds = " + JSON.stringify(updatedRowIds));
    this.log(
      logPrefix,
      "idsThatDoNotExistYet = " + JSON.stringify(idsThatDoNotExistYet),
    );
    updatedRows.forEach(function (nextUpdatedRow) {
      _this.log(
        logPrefix,
        "nextUpdatedRow[rowIdField] = " + nextUpdatedRow[rowIdField],
      );
      if (idsThatDoNotExistYet.includes(nextUpdatedRow[rowIdField])) {
        _this.EditedTable.rows.push(nextUpdatedRow);
      }
    });
    this.log(logPrefix, "this.EditedTable updated...");
    this.CellEdited();
    this.updateUpdatedTable();
    this.updateRenderedTable();
  }
  updateSelectedRows(event) {
    const _this = this;
    const logPrefix = "updateSelectedRows(): ";
    const selectedRows = this.widgetGridApi.getSelectedRows();
    this.SelectedRows = {
      dataShape: this.dataSourceDataShape,
      rows: selectedRows,
    };
    const selectedRowNumbers = [];
    this.widgetGridApi.getSelectedNodes().forEach(function (row) {
      const nextRowIndex = row.rowIndex;
      _this.log(logPrefix, "next selected row index = " + nextRowIndex);
      selectedRowNumbers.push(nextRowIndex + 1);
    });
    this.SelectedRowNumbers = selectedRowNumbers.join();
    this.log(logPrefix, "selectedRowNumbers = " + selectedRowNumbers);
    this.log(
      logPrefix,
      "Selection updated, SelectedRowNumbers = " + this.SelectedRowNumbers,
    );
  }
  updateUpdatedTable() {
    const rows = [];
    this.widgetGridApi.forEachNode(function (node) {
      rows.push(node.data);
    });
    this.UpdatedTable = {
      dataShape: this.dataSourceDataShape,
      rows,
    };
    this.updateRenderedTable();
    this.updateUpdatedTableOnDataChange();
  }
  updateUpdatedTableOnDataChange() {
    const _this = this;
    const logPrefix = "updateUpdatedTableOnDataChange(): ";
    const rows = [];
    this.widgetGridApi.forEachNode(function (node) {
      _this.log(
        logPrefix,
        "updating UpdatedTable with next row: " + JSON.stringify(node.data),
      );
      rows.push(node.data);
    });
    this.log(logPrefix, "updating UpdatedTable with " + rows.length + " rows");
    this.UpdatedTableOnDataChange = {
      dataShape: this.dataSourceDataShape,
      rows,
    };
    this.updateRenderedTable();
  }
  updateFilteredTable() {
    const logPrefix = "updateFilteredTable(): ";
    this.log(logPrefix, "filter was changed and applied...");
    const rows = [];
    if (
      this.gridOptions.hasOwnProperty("rowModelType") &&
      this.gridOptions.rowModelType !== "serverSide"
    ) {
      this.widgetGridApi.forEachNodeAfterFilterAndSort(function (node) {
        rows.push(node.data);
      });
      this.log(
        logPrefix,
        "updating FilteredTable with " + rows.length + " rows",
      );
      this.FilteredTable = {
        dataShape: this.dataSourceDataShape,
        rows,
      };
    } else {
      this.log(
        logPrefix,
        "not updating FilteredTable since grid is server-side",
      );
    }
    this.updateRenderedTable();
    this.DataFiltered();
  }
  updateRenderedTable() {
    const logPrefix = "updateRenderedTable(): ";
    this.log(logPrefix, "columns or data changed, updating rendered table.");
    if (this.dataSourceDataShape) {
      const rows_1 = [];
      let renderedDataShape_1 = JSON.parse(
        JSON.stringify(this.dataSourceDataShape),
      );
      const visibleColumns_1 = this.widgetGridApi
        .getColumnState()
        .filter(function (nextColumnState) {
          return !nextColumnState.hide;
        })
        .map(function (nextColumnState) {
          return nextColumnState.colId;
        });
      Object.keys(renderedDataShape_1.fieldDefinitions).forEach(
        function (nextField) {
          if (!visibleColumns_1.includes(nextField)) {
            delete renderedDataShape_1.fieldDefinitions[nextField];
          }
        },
      );
      if (this.ShowGridAsTree) {
        this.log(
          logPrefix,
          "since grid is a tree, updating RenderedTable with source datashape",
        );
        renderedDataShape_1 = this.dataSourceDataShape;
      }
      this.widgetGridApi.forEachNode(function (nextNode) {
        rows_1.push(nextNode.data);
      });
      this.log(
        logPrefix,
        "updating RenderedTable with " + rows_1.length + " rows",
      );
      this.RenderedTable = {
        dataShape: renderedDataShape_1,
        rows: rows_1,
      };
    }
  }
  async applyCustomThemeStyle(): Promise<void> {
    const logPrefix = "applyCustomThemeStyle(): ";
    this.log(logPrefix, "Starting, getting custom theme");
    const themeToCssVariableMap = {
      "global-text-body-fontsize": "--grid-font-size",
      "e-grid-text-fontsize": "--grid-font-size",
      "global-text-title-fontsize": "--header-font-size",
      "e-grid-header-label-fontsize": "--header-font-size",
    };
    const response = await fetch("", {
      method: "POST",
      headers: HEADERS,
    });
    const customThemeContent = await response.json();
    const props = customThemeContent.props;
    const propMap = {};
    if (props) {
      props.forEach(function (nextProp) {
        propMap[nextProp.name] = nextProp.value;
      });
    }
    const newCssValues = {};
    Object.keys(themeToCssVariableMap).forEach(function (nextThemeProp) {
      if (propMap.hasOwnProperty(nextThemeProp)) {
        newCssValues[themeToCssVariableMap[nextThemeProp]] =
          propMap[nextThemeProp];
      }
    });
    if (this.GridTextSize) {
      this.log(
        logPrefix,
        "overriding text size with grid property GridTextSize: " +
          this.GridTextSize,
      );
      newCssValues["--grid-font-size"] = this.GridTextSize + "px";
    }
    if (this.GridHeaderSize !== undefined) {
      this.log(
        logPrefix,
        "overriding header size with grid property GridHeaderSize: " +
          this.GridHeaderSize,
      );
      newCssValues["--header-font-size"] = this.GridHeaderSize + "px";
    }
    if (this.HideBorders) {
      this.log(logPrefix, "HideBorders is true, removing borders: ");
      newCssValues["--ag-borders"] = "none";
    }
    Object.keys(newCssValues).forEach((nextCssVariable) => {
      this.log(
        logPrefix,
        "setting nextCssVariable " +
          nextCssVariable +
          " to " +
          newCssValues[nextCssVariable],
      );
      document
        .getElementById(this.jqElement[0].id)
        .style.setProperty(nextCssVariable, newCssValues[nextCssVariable]);
    });
  }
  cellEditorSelector(params) {
    const logPrefix = "cellEditorSelector(): ";
    this.log(logPrefix, "Starting...");
    const returnValue: any = {
      component: "agTextCellEditor",
    };
    const currentAttribute = params.column.getColId();
    this.log(
      logPrefix,
      `checking cell editor selector for attribute: ${currentAttribute}`,
    );
    const cellEditorSelectorConfig = this.gridConfig.CellEditorSelectorConfig;
    this.log(
      logPrefix,
      "cellEditorSelectorConfig = " + JSON.stringify(cellEditorSelectorConfig),
    );
    if (this.gridConfig.hasOwnProperty("ColumnConfig")) {
      const columnConfig = this.gridConfig.ColumnConfig;
      this.log(logPrefix, "columnConfig = " + JSON.stringify(columnConfig));
      this.log(
        logPrefix,
        "does it have that attribute? = " +
          columnConfig.hasOwnProperty(currentAttribute),
      );
      if (columnConfig.hasOwnProperty(currentAttribute)) {
        const matchingFieldOptions = columnConfig[currentAttribute];
        this.log(
          logPrefix,
          "matchingFieldOptions = " + JSON.stringify(matchingFieldOptions),
        );
        if (matchingFieldOptions.cellEditorParams != null) {
          const matchingFieldCellEditorParams =
            matchingFieldOptions.cellEditorParams;
          if (
            matchingFieldCellEditorParams.hasOwnProperty(
              "fieldThatDeterminesCellEditor",
            )
          ) {
            const fieldThatDeterminesCellEditor =
              matchingFieldCellEditorParams.fieldThatDeterminesCellEditor;
            this.log(
              logPrefix,
              "fieldThatDeterminesCellEditor = " +
                fieldThatDeterminesCellEditor,
            );
            const fieldValue = params.data[fieldThatDeterminesCellEditor];
            if (
              fieldValue &&
              (this.log(logPrefix, "that field has a value of = " + fieldValue),
              cellEditorSelectorConfig.hasOwnProperty(fieldValue))
            ) {
              const selectedCellConfig = cellEditorSelectorConfig[fieldValue];
              this.log(
                logPrefix,
                "CellEditorSelectorConfig has that value, the config = " +
                  JSON.stringify(selectedCellConfig),
              );
              returnValue.component = selectedCellConfig.cellEditor;
              if (selectedCellConfig.hasOwnProperty("cellEditorPopup")) {
                returnValue.popup = selectedCellConfig.cellEditorPopup;
              }
              if (selectedCellConfig.hasOwnProperty("cellEditorParams")) {
                returnValue.params = selectedCellConfig.cellEditorParams;
              }
            }
          }
        }
      }
    }
    this.log(
      logPrefix,
      "returning returnValue = " + JSON.stringify(returnValue),
    );
    return returnValue;
  }
  cellRendererSelector(params) {
    const logPrefix = "cellRendererSelector(): ";
    if (params.data) {
      this.log(logPrefix, "Starting...");
      const returnValue: any = {
        component: undefined,
      };
      const currentAttribute = params.column.getColId();
      this.log(
        logPrefix,
        `checking cell editor selector for attribute: ${currentAttribute}`,
      );
      const cellRendererSelectorConfig =
        this.gridConfig.CellRendererSelectorConfig;
      this.log(
        logPrefix,
        "cellRendererSelectorConfig = " +
          JSON.stringify(cellRendererSelectorConfig),
      );
      if (this.gridConfig.hasOwnProperty("ColumnConfig")) {
        const columnConfig = this.gridConfig.ColumnConfig;
        this.log(logPrefix, "columnConfig = " + JSON.stringify(columnConfig));
        this.log(
          logPrefix,
          "does it have that attribute? = " +
            columnConfig.hasOwnProperty(currentAttribute),
        );
        if (columnConfig.hasOwnProperty(currentAttribute)) {
          const matchingFieldOptions = columnConfig[currentAttribute];
          this.log(
            logPrefix,
            "matchingFieldOptions = " + JSON.stringify(matchingFieldOptions),
          );
          if (matchingFieldOptions.cellRendererParams != null) {
            const matchingFieldCellRendererParams =
              matchingFieldOptions.cellRendererParams;
            if (
              matchingFieldCellRendererParams.hasOwnProperty(
                "fieldThatDeterminesCellRenderer",
              )
            ) {
              const fieldThatDeterminesCellRenderer =
                matchingFieldCellRendererParams.fieldThatDeterminesCellRenderer;
              this.log(
                logPrefix,
                "fieldThatDeterminesCellRenderer = " +
                  fieldThatDeterminesCellRenderer,
              );
              const fieldValue = params.data[fieldThatDeterminesCellRenderer];
              if (
                fieldValue &&
                (this.log(
                  logPrefix,
                  "that field has a value of = " + fieldValue,
                ),
                cellRendererSelectorConfig.hasOwnProperty(fieldValue))
              ) {
                const selectedCellConfig =
                  cellRendererSelectorConfig[fieldValue];
                this.log(
                  logPrefix,
                  "cellRendererSelectorConfig has that value, the config = " +
                    JSON.stringify(selectedCellConfig),
                );
                returnValue.component = selectedCellConfig.cellRenderer;
                if (selectedCellConfig.hasOwnProperty("cellRendererParams")) {
                  returnValue.params = selectedCellConfig.cellRendererParams;
                }
              }
            }
          }
        }
      }
      this.log(
        logPrefix,
        "returning returnValue = " + JSON.stringify(returnValue),
      );
      return returnValue;
    }
  }
  updateCustomServerSideStatusPanelRowCount() {
    this.widgetGridApi
      .getStatusBarComponent(SERVER_SIDE_ROW_COUNT_STATUS_PANEL_ID)
      .updateRowCount(this.CustomServerSideStatusPanelRowCount);
  }
  reindexOnPaste() {
    const _this = this;
    if (this.PasteIndexColumn) {
      this.log(
        "reindexOnPaste(): ",
        "Starting, reindexing column: " + this.PasteIndexColumn,
      );
      this.widgetGridApi.forEachNode(function (node, index) {
        const nextRowData = node.data;
        _this.log(
          "reindexOnPaste(): ",
          "reindexing row: " + JSON.stringify(nextRowData),
        );
        nextRowData[_this.PasteIndexColumn] = index + 1;
        node.setData(nextRowData);
      });
    }
  }
  @property
  set Data(Data) {
    if (Data) {
      this.log(
        "setData(): ",
        "setting Data, this.widgetGridApi is undefined? = " +
          (this.widgetGridApi === undefined) +
          ", this.alreadyLoaded = " +
          this.alreadyLoaded,
      );
      this.buildGrid();
    }
  }

  updateProperty(info) {
    super.updateProperty(info);
    if (info.TargetProperty === "RowSelection") {
      this.widgetGridApi.deselectAll();
      this.gridOptions.rowSelection = info.SinglePropertyValue;
    }
    if (info.TargetProperty === "Data") {
      console.log("Data changed, building grid...");
      this.buildGrid();
    }
  }
  handleSelectionUpdate(property, selectedRows, selectedRowIndices) {
    this.log(
      "handleSelectionUpdate(): ",
      "selectedRowIndices = " + JSON.stringify(selectedRowIndices),
    );
    if (selectedRowIndices.length == 0) {
      this.widgetGridApi.deselectAll();
    } else {
      this.widgetGridApi
        .getRowNode(selectedRowIndices[0].toString())
        .setSelected(true, true);
    }
  }
  valueWillBind(value, info) {
    const logPrefix = "valueWillBind(" + info.TargetProperty + "): ";
    if (this.EnableConsoleLogging) {
      this.log(logPrefix, "property updated!");
    }
    return true;
  }
  async afterRender(): Promise<void> {
    this.buildGrid();
  }
  beforeDestroy() {
    const container = document.getElementById(this.jqElement[0].id);
    if (container) {
      const currentGrid = container.firstChild;
      if (currentGrid) {
        currentGrid.remove();
      }
      container.remove();
    }
  }

  @property
  declare AllowEditing: any;
  @property
  declare AnyRowSelected: any;
  @property
  declare AutoOpenSideBar: any;
  @property
  declare CheckBoxSelection: any;
  @property
  declare ChildField: any;
  @property
  declare ClickedActionCellButton: any;
  @property
  declare DateFormat: any;
  @property
  declare DateEditorsMinimumDate: any;
  @property
  declare DateEditorsMaximumDate: any;
  @property
  declare DefaultSelectedRows: any;
  @property
  declare DeletedTable: any;
  @property
  declare DateEditorShowWeekNumbers: any;
  @property
  declare EditTable: any;
  @property
  declare EditedTable: any;
  @property
  declare TreeTable: any;
  @property
  declare EnableRowGrouping: any;
  @property
  declare EmitDoubleClickEvent: any;
  @property
  declare EnableCellTextSelection: any;
  @property
  declare EnableRangeSelection: any;
  @property
  declare FilterType: any;
  @property
  declare FilteredTable: any;
  @property
  declare GridConfig: any;
  @property
  declare GroupingColumns: any;
  @property
  declare NoRowSelected: any;
  @property
  declare ParentField: any;
  @property
  declare QuickFilterText: any;
  @property
  declare ReorderColumns: any;
  @property
  declare ResizeColumns: any;
  @property
  declare ResizeGridWithParent: any;
  @property
  declare RowHeight: any;
  @property
  declare RowSelection: any;
  @property
  declare RowsToSelect: any;
  @property
  declare ShowFloatingFilters: any;
  @property
  declare ShowGridAsTree: any;
  @property
  declare SideBarPanels: any;
  @property
  declare SingleClickEdit: any;
  @property
  declare Sorting: any;
  @property
  declare StatusMessage: any;
  @property
  declare TargetSideBarPanel: any;
  @property
  declare TransposeFirstRowToForm: any;
  @property
  declare TreeColumnField: any;
  @property
  declare UpdatedTable: any;
  @property
  declare SelectedRows: any;
  @property
  declare SelectedRowNumbers: any;
  @property
  declare UpdatedTableOnDataChange: any;
  @property
  declare SavedState: any;
  @property
  declare NewRowData: any;
  @property
  declare NewRowIndex: any;
  @property
  declare GridTextSize: any;
  @property
  declare GridHeaderSize: any;
  @property
  declare CustomMashupStatusPanelMashupName: any;
  @property
  declare FieldNameToUseForRowSelection: any;
  @property
  declare FieldValuesToUseForRowSelection: any;
  @property
  declare GridTheme: any;
  @property
  declare ScrollIndex: any;
  @property
  declare ScrollPosition: any;
  @property
  declare EnableConsoleLogging: any;
  @property
  declare FirstRowSelected: any;
  @property
  declare LastRowSelected: any;
  @property
  declare RenderedTable: any;
  @property
  declare TreePathField: any;
  @property
  declare TreePathFieldDelimeter: any;
  @property
  declare HideBorders: any;
  @property
  declare ServerSideDataSource: any;
  @property
  declare ServerSideAdditionalRequestParams: any;
  @property
  declare ServerSideDynamicExportColumnsPrefix: any;
  @property
  declare RowIdField: any;
  @property
  declare ShowServerSideRowTotalsStatusPanel: any;
  @property
  declare ReindexOnPaste: any;
  @property
  declare PasteIndexColumn: any;
  @property
  declare UseAsyncSetFilters: any;
  @property
  declare ServerSideFiltersDataSource: any;
  @property
  set CustomServerSideStatusPanelRowCount(rowCount) {
    if (rowCount) {
      this.log(
        "setCustomServerSideStatusPanelRowCount(): ",
        "setting CustomServerSideStatusPanelRowCount, rowCount = " + rowCount,
      );
      this.widgetGridApi
        .getStatusPanel(SERVER_SIDE_ROW_COUNT_STATUS_PANEL_ID)
        .updateRowCount(rowCount);
      this.updateCustomServerSideStatusPanelRowCount();
    }
  }
  @widgetRuntimeSupport_event
  declare CellEdited: any;
  @widgetRuntimeSupport_event
  declare StateSaved: any;
  @widgetRuntimeSupport_event
  declare GridLoaded: any;
  @widgetRuntimeSupport_event
  declare RowDoubleClicked: any;
  @widgetRuntimeSupport_event
  declare DataFiltered: any;
  @widgetRuntimeSupport_event
  declare ActionCellButtonClicked: any;
  @widgetRuntimeSupport_event
  declare ShowInfoMessage: any;
  @widgetRuntimeSupport_event
  declare ShowWarningMessage: any;
  @widgetRuntimeSupport_event
  declare ShowErrorMessage: any;
  @widgetRuntimeSupport_event
  declare ServerSideRowModelGridUpdated: any;
  @widgetRuntimeSupport_event
  declare SelectedRowsChanged: any;
}

// Preserve the webpack entry's public window export.
(window as any).relatedDropdownValues = relatedDropdownValues;
