import { writeWidgetLog } from "./widget-logging";
// AG Grid 3 runtime widget. Decorators come from typescriptwebpacksupport.
import {
  AG_GRID_ROW_ID_FIELD,
  SERVER_SIDE_ROW_COUNT_STATUS_PANEL_ID,
  createGridData,
} from "./grid-data";
import { DatePicker } from "./date-picker";
import { CustomImageRenderer } from "./image-renderer";
import { CustomDateRenderer } from "./date-renderer";
import { CustomJsonRenderer } from "./json-renderer";
import { CustomColumnAggregateFooterRenderer } from "./aggregate-footer";
import { CustomSelectEditor } from "./select-editor";
import { CustomActionCellRenderer } from "./action-cell-renderer";
import { CustomCheckboxRenderer } from "./checkbox-renderer";
import { CustomMultiEditToolPanel } from "./multi-edit-tool-panel";
import { CustomNewRowToolPanel } from "./new-row-tool-panel";
import { CustomPasswordEditor } from "./password-editor";
import { CustomPasswordRenderer } from "./password-renderer";
import { CustomIconCellRenderer } from "./icon-cell-renderer";
import { CustomTreeColumnCellRenderer } from "./tree-column-cell-renderer";
import { CustomNumberEditor } from "./number-editor";
import { CustomLoadingCellRenderer } from "./loading-cell-renderer";
import { CustomHyperlinkRenderer } from "./hyperlink-renderer";
import { CustomMashupStatusPanel } from "./mashup-status-panel";
import { CustomServerSideTotalsStatusPanel } from "./server-side-totals-status-panel";
import { CustomLoadingOverlay } from "./loading-overlay";
import { icon } from "@fortawesome/fontawesome-svg-core";
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
import {
  TWWidgetDefinition,
  property,
  service,
  event,
} from "typescriptwebpacksupport/widgetRuntimeSupport";

const VISIBLE_INDEX_POSITIONS = { top: "top", middle: "middle", bottom: "bottom" } as const;
const HEADERS = new Headers();
HEADERS.append("Content-Type", "application/json");
HEADERS.append("Accept", "application/json");
const relatedDropdownValues = {};
@TWWidgetDefinition
class AgGridWidget3 extends TWRuntimeWidget {
  [key: string]: any;
  constructor() {
    super();
    this.gridConfig = {
      ColumnConfig: {},
      RowConfig: {},
      GridOptions: {},
      EditableFieldsColumn: "",
      CellClassColumn: "",
      CellEditorSelectorConfig: {},
      CellRendererSelectorConfig: {},
      excelStyles: [],
    };
    this.alreadyLoaded = false;
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
      `AgGridWidget-${this.widgetId}.`,
      prefix,
      message,
    );
  }
  convertRowsToSelectToNumeric(rowsToSelect) {
    const logPrefix = `.convertRowsToSelectToNumeric(${rowsToSelect}): `;
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
          `startValue = ${startValue}, endValue = ${endValue}`,
        );
        for (let i = startValue; i < endValue + 1; i++) {
          this.log(logPrefix, `adding row to select # ${i}`);
          returnArray.push(i - 1);
        }
      }
      if (rowsToSelect.indexOf(",") > 0) {
        rowsToSelect.split(",").map(function (nextRowAsString) {
          return returnArray.push(parseInt(nextRowAsString) - 1);
        });
      }
    } catch (err) {
      this.log(logPrefix, `error selecting default rows, err = ${err}`);
    }
    return returnArray;
  }
  @service
  AddNewRow() {
    this.log(
      "AddNewRow(): ",
      `Adding new row, this.NewRowIndex = ${this.NewRowIndex}`,
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
    let _a;
    const logPrefix = "DeleteSelectedRows(): ";
    const selectedRows = this.widgetGridApi.getSelectedRows();
    this.log(
      logPrefix,
      `Deleting ${selectedRows.length} rows, selectedRows = ${JSON.stringify(selectedRows)}`,
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
      `deleting rows, currentEditedTableRows = ${JSON.stringify(currentEditedTableRows)}`,
    );
    currentEditedTableRows.forEach((nextEditedRow) => {
      this.log(logPrefix, `nextEditedRow = ${JSON.stringify(nextEditedRow)}`);
      let nextEditedRowWasDeleted = false;
      selectedRows.forEach(function (nextDeletedRow) {
        if (JSON.stringify(nextDeletedRow) == JSON.stringify(nextEditedRow)) {
          nextEditedRowWasDeleted = true;
        }
      });
      this.log(
        logPrefix,
        `nextEditedRowWasDeleted = ${nextEditedRowWasDeleted}`,
      );
      if (!nextEditedRowWasDeleted) {
        updatedEditedTableRows.push(nextEditedRow);
      }
    });
    (_a = this.DeletedTable.rows).push.apply(_a, selectedRows);
    this.EditedTable.rows = updatedEditedTableRows;
    this.log(
      logPrefix,
      `deleting rows, now EditedTableRows = ${JSON.stringify(this.EditedTable.rows)}`,
    );
  }
  @service
  ResizeAll() {
    this.log("ResizeAll(): ", "calling resize all...");
    try {
      this.widgetGridApi.sizeColumnsToFit();
    } catch (err) {
      this.log("ResizeAll(): ", `err: ${err}`);
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
      this.log(logPrefix, `restoring saved state: ${this.SavedState}`);
      try {
        const savedStateAsJson = JSON.parse(this.SavedState);
        if (
          this.widgetGridApi &&
          savedStateAsJson.hasOwnProperty("ColumnState")
        ) {
          this.log(
            logPrefix,
            `restoring columns to: ${JSON.stringify(savedStateAsJson)}`,
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
        this.log(logPrefix, `Error parsing JSON? err = ${err}`);
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
    this.log("SizeAllColumns(): ", `allColumnIds = ${allColumnIds}`);
    this.widgetGridApi.autoSizeColumns(allColumnIds, false);
  }
  @service
  ClearAllFilters() {
    this.widgetGridApi.setFilterModel(null);
    this.widgetGridApi.onFilterChanged();
  }
  /**
   * Applies a bulk selection change, publishing SelectedRows once instead of once per row.
   */
  applyBulkSelection(change: () => void) {
    this.bulkSelecting = true;
    try {
      change();
    } finally {
      this.bulkSelecting = false;
    }
    this.updateSelectedRows(null);
  }
  @service
  SelectAllFiltered() {
    this.log("SelectAllFiltered(): ", "selecting every row that passes the filters");
    this.applyBulkSelection(() =>
      this.widgetGridApi.selectAllFiltered("apiSelectAllFiltered"),
    );
  }
  @service
  DeselectAll() {
    this.log("DeselectAll(): ", "clearing the selection");
    this.applyBulkSelection(() => this.widgetGridApi.deselectAll("apiSelectAll"));
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
      `adding new row with data, newRowData = ${JSON.stringify(newRowData)}`,
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
      `restoring deleted rows, this.DeletedTable = ${this.DeletedTable.rows}`,
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
    const logPrefix = `SelectSpecifiedRows(${this.RowsToSelect}): `;
    this.log(
      logPrefix,
      `selecting specified rows, rowsToSelect = ${this.RowsToSelect}`,
    );
    const rowsToSelect = this.convertRowsToSelectToNumeric(this.RowsToSelect);
    this.log(
      logPrefix,
      `selecting specified rows, rowsToSelect = ${rowsToSelect}`,
    );
    this.widgetGridApi.forEachNode(function (node, index) {
      node.setSelected(rowsToSelect.includes(index));
    });
  }
  @service
  SelectRowsByFieldValues() {
    const logPrefix =
      `SelectRowsByFieldValues(${this.FieldNameToUseForRowSelection}): `;
    this.log(
      logPrefix,
      `selecting rows that match entries: ${this.FieldValuesToUseForRowSelection}`,
    );
    const valuesToSelect = this.FieldValuesToUseForRowSelection?.split(",");
    this.log(logPrefix, `valuesToSelect = ${valuesToSelect}`);
    this.widgetGridApi.forEachNode((node) => {
      const rowFieldValue = `${node.data[this.FieldNameToUseForRowSelection]}`;
      this.log(
        logPrefix,
        `next row this field has a value of: ${rowFieldValue}`,
      );
      if (valuesToSelect.includes(rowFieldValue)) {
        this.log(
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
    const logPrefix =
      `SelectRowsByArrayValues(${this.FieldNameToUseForRowSelection}): `;
    this.log(
      logPrefix,
      `selecting rows that match entries: ${this.FieldValuesToUseForRowSelection}`,
    );
    const valuesToSelect = JSON.parse(this.FieldValuesToUseForRowSelection);
    this.log(logPrefix, `valuesToSelect = ${valuesToSelect}`);
    this.widgetGridApi.forEachNode((node) => {
      const rowFieldValue = `${node.data[this.FieldNameToUseForRowSelection]}`;
      this.log(
        logPrefix,
        `next row this field has a value of: ${rowFieldValue}`,
      );
      if (valuesToSelect.includes(rowFieldValue)) {
        this.log(
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
    const logPrefix = `ScrollToSpecifiedRow(${this.ScrollIndex}): `;
    const indexAsInteger = Number(this.ScrollIndex);
    const ScrollToPosition = this.ScrollPosition;
    this.log(logPrefix, `scrolling to row using position: ${ScrollToPosition}`);
    if (!Number.isNaN(indexAsInteger)) {
      if (this.ShowGridAsTree) {
        this.log(
          logPrefix,
          `grid is a tree, expanding index # ${this.ScrollIndex}`,
        );
        this.widgetGridApi.forEachNode((node, index) => {
          if (index == this.ScrollIndex) {
            this.log(logPrefix, "node # matches, expanding row");
            this.widgetGridApi.setRowNodeExpanded(node, true);
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
    const renderedGrid = this.widgetGridApi.getRenderedNodes();
    let nextRowIsRowToSelect = false;
    const lastSelectedRowId = this.indices[this.indices.length - 1];
    this.log("SelectNextRow(): ", `lastSelectedRowId = ${lastSelectedRowId}`);
    renderedGrid.every((rowNode) => {
      const nextRowNodeId = parseInt(rowNode.id);
      this.log(
        "SelectNextRow(): ",
        `next rendered grid row id = ${nextRowNodeId}`,
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
    const renderedGrid = this.widgetGridApi.getRenderedNodes().reverse();
    let nextRowIsRowToSelect = false;
    const firstSelectedRowId = this.indices[0];
    this.log(
      "SelectPreviousRow(): ",
      `firstSelectedRowId = ${firstSelectedRowId}`,
    );
    renderedGrid.every((rowNode) => {
      const nextRowNodeId = parseInt(rowNode.id);
      this.log(
        "SelectPreviousRow(): ",
        `next rendered grid row id = ${nextRowNodeId}`,
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
    const logPrefix = "buildGrid(): ";
    this.widgetId = this.jqElementId;
    if (this.Data) {
      try {
        this.log(logPrefix, "starting...");
        const dataAsJson = JSON.parse(JSON.stringify(this.Data));
        this.log(logPrefix, `dataAsJson = ${JSON.stringify(dataAsJson)}`);
        this.log(
          logPrefix,
          `this.gridConfig = ${JSON.stringify(this.gridConfig)}`,
        );
        this.dataSourceDataShape = dataAsJson.dataShape;
        this.originalDataRows = JSON.parse(JSON.stringify(dataAsJson.rows));
        this.log(
          logPrefix,
          `originalDataRows = ${JSON.stringify(this.originalDataRows)}`,
        );
        const gridTheme = this.GridTheme ?? "ag-theme-alpine";
        this.log(
          logPrefix,
          `this.GridTheme = ${this.GridTheme}, so gridTheme = ${gridTheme}`,
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
          `getGridOptions with this.widgetId = ${this.widgetId}, alreadyLoaded = ${this.alreadyLoaded}`,
        );
        if (
          this.UseAsyncSetFilters &&
          this.gridConfig.hasOwnProperty("ColumnConfig")
        ) {
          const columnConfig_1 = this.gridConfig.ColumnConfig;
          this.log(
            logPrefix,
            `creating initialFilterValues, columnConfig = ${JSON.stringify(columnConfig_1)}`,
          );
          const initialFilterValues_1 = {};
          Object.keys(columnConfig_1).forEach((nextColumn) => {
            const nextColumnConfig = columnConfig_1[nextColumn];
            if (nextColumnConfig.filterParams?.hasOwnProperty("values")) {
              this.log(
                logPrefix,
                `initialFilterValues: nextColumn ${nextColumn} has filter params: ${JSON.stringify(nextColumnConfig.filterParams)}`,
              );
              const nextColumnFilterValues =
                nextColumnConfig.filterParams.values;
              this.log(
                logPrefix,
                `initialFilterValues: adding values for nextColumn ${nextColumn}: ${JSON.stringify(nextColumnFilterValues)}`,
              );
              initialFilterValues_1[nextColumn] = nextColumnFilterValues;
            }
          });
          this.log(
            logPrefix,
            `done creating initialFilterValues = ${JSON.stringify(initialFilterValues_1)}`,
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
          emitCustomActionButtonClick: (buttonName) => {
            this.log(
              logPrefix,
              `CustomActionCellButton clicked: ${buttonName}`,
            );
            this.ClickedActionCellButton = buttonName;
            this.ActionCellButtonClicked();
          },
          getGridConfig: () => {
            return this.gridConfig;
          },
          updateUpdatedTable: () => {
            this.updateUpdatedTable();
          },
          emitStatusMessage: (messageLevel, statusMessage) => {
            this.StatusMessage = statusMessage;
            switch (messageLevel) {
              case "info":
                this.ShowInfoMessage();
                break;
              case "warning":
                this.ShowWarningMessage();
                break;
              case "error":
                this.ShowErrorMessage();
            }
          },
          updateEditedTable: (updatedNodeData) => {
            const rowIdField =
              this.gridOptions.rowModelType == "serverSide"
                ? this.RowIdField
                : AG_GRID_ROW_ID_FIELD;
            this.log(
              logPrefix,
              `based on whether or not this is a client-side or server-side grid, rowIdField = ${rowIdField}`,
            );
            this.EditedTable.rows = this.EditedTable.rows.map(
              function (nextRow) {
                return (
                  updatedNodeData.find(function (nextUpdatedNode) {
                    return nextRow[rowIdField] === nextUpdatedNode[rowIdField];
                  }) || nextRow
                );
              },
            );
            const existingRowIds = this.EditedTable.rows.map(
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
            this.log(
              logPrefix,
              `existingRowIds = ${JSON.stringify(existingRowIds)}`,
            );
            this.log(
              logPrefix,
              `updatedRowIds = ${JSON.stringify(updatedRowIds)}`,
            );
            this.log(
              logPrefix,
              `idsThatDoNotExistYet = ${JSON.stringify(idsThatDoNotExistYet)}`,
            );
            updatedNodeData.forEach((nextUpdatedRow) => {
              if (idsThatDoNotExistYet.includes(nextUpdatedRow[rowIdField])) {
                this.EditedTable.rows.push(nextUpdatedRow);
              }
            });
            this.updateUpdatedTable();
            this.CellEdited();
          },
          getAsyncFilterValues: (column) => {
            return this.serverSideSetFilters[column];
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
          `gridOptions.context = ${JSON.stringify(this.gridOptions.context)}`,
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
          this.log(logPrefix, `adding sidebar, panels = ${this.SideBarPanels}`);
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
        this.gridOptions.columnDefs?.forEach((nextColumnDef, index) => {
          if (
            nextColumnDef.cellEditorParams?.hasOwnProperty(
              "useCellEditorSelector",
            )
          ) {
            this.log(
              logPrefix,
              `updating cellEditorSelector for column def # ${index}`,
            );
            this.gridOptions.columnDefs[index].cellEditorSelector =
              this.cellEditorSelector;
          }
          if (
            nextColumnDef.cellRendererParams?.hasOwnProperty(
              "useCellRendererSelector",
            )
          ) {
            this.log(
              logPrefix,
              `updating cellRendererSelector for column def # ${index}`,
            );
            this.gridOptions.columnDefs[index].cellRendererSelector =
              this.cellRendererSelector;
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
            this.log(logPrefix, `overriding next gridconfig prop ${prop}`);
            this.gridOptions[prop] = gridConfigOptions[prop];
          }
          if (gridConfigOptions.hasOwnProperty("isRowSelectableField")) {
            const isSelectableField_1 = gridConfigOptions.isRowSelectableField;
            this.log(
              logPrefix,
              `grid options has isRowSelectableField, defining isSelectable function, isSelectableField = ${isSelectableField_1}`,
            );
            this.gridOptions.isRowSelectable = (rowNode) => {
              const rowIsSelectable = rowNode.data[isSelectableField_1];
              this.log(
                logPrefix,
                `checking if rowNode.id ${rowNode.id} is rowSelectable? = ${rowIsSelectable}`,
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
            `statusPanels = ${JSON.stringify(this.gridOptions.statusBar.statusPanels)}`,
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
          this.gridOptions.autoGroupColumnDef.filterValueGetter = (
            params,
          ) => {
            return params.data[this.TreeColumnField];
          };
        }
        this.gridOptions.onCellValueChanged = this.cellValueChanged;
        this.gridOptions.onRowSelected = this.updateSelectedRows;
        this.gridOptions.onFilterChanged = () => {
          this.log("filterChanged(): ", "onFilterChanged event emitted...");
          this.updateFilteredTable();
          this.SaveState();
        };
        this.gridOptions.onSortChanged = () => {
          this.log(logPrefix, "onSortChanged event emitted...");
          this.SaveState();
        };
        this.gridOptions.onColumnEverythingChanged = () => {
          this.log(logPrefix, "onColumnEverythingChanged event emitted...");
          this.SaveState();
        };
        this.gridOptions.onColumnPivotChanged = () => {
          this.log(logPrefix, "onColumnPivotChanged event emitted...");
          this.SaveState();
        };
        this.gridOptions.onColumnPivotModeChanged = () => {
          this.log(logPrefix, "onColumnPivotModeChanged event emitted...");
          this.SaveState();
        };
        this.gridOptions.onColumnMoved = () => {
          this.log(logPrefix, "onColumnMoved event emitted...");
          this.SaveState();
        };
        this.gridOptions.onColumnResized = () => {
          this.log(logPrefix, "onColumnResized event emitted...");
          this.SaveState();
        };
        this.gridOptions.onColumnPinned = () => {
          this.log(logPrefix, "onColumnPinned event emitted...");
          this.SaveState();
        };
        this.gridOptions.onPasteEnd = () => {
          const gridRowCount = this.widgetGridApi.getDisplayedRowCount() - 1;
          this.log(
            logPrefix,
            `onPasteEnd event emitted, gridRowCount = ${gridRowCount}`,
          );
          for (let visibleRowIndex = 20; gridRowCount > visibleRowIndex; ) {
            this.log(
              logPrefix,
              `onPasteEnd ensureIndexVisible index = ${visibleRowIndex}`,
            );
            this.widgetGridApi.ensureIndexVisible(visibleRowIndex, "bottom");
            visibleRowIndex += 20;
          }
          this.widgetGridApi.ensureIndexVisible(gridRowCount, "bottom");
          this.log(
            logPrefix,
            "onPasteEnd event done, updating updated table...",
          );
          this.updateUpdatedTableOnDataChange();
          if (this.ReindexOnPaste) {
            this.reindexOnPaste();
          }
        };
        this.gridOptions.onGridReady = (event) => {
          this.log(logPrefix, "onGridReady...");
          if (this.AutoOpenSideBar) {
            event.api.openToolPanel("filters");
          } else {
            event.api.closeToolPanel();
          }
          this.SaveState();
          this.updateFilteredTable();
          this.DataFiltered();
          if (this.ShowGridAsTree) {
            this.updateUpdatedTable();
          }
          if (this.DefaultSelectedRows) {
            this.log(
              logPrefix,
              `selecting default rows, DefaultSelectedRows = ${this.DefaultSelectedRows}`,
            );
            try {
              const rowsToSelect_1 = this.convertRowsToSelectToNumeric(
                this.DefaultSelectedRows,
              );
              this.log(
                logPrefix,
                `selecting default rows, rowsToSelect = ${rowsToSelect_1}`,
              );
              event.api.forEachNode(function (node, index) {
                node.setSelected(rowsToSelect_1.includes(index));
              });
            } catch (err) {
              this.log(
                logPrefix,
                `error selecting default rows, err = ${err}`,
              );
            }
          }
          this.NoRowSelected = event.api.getSelectedNodes().length == 0;
          this.AnyRowSelected = event.api.getSelectedNodes().length > 0;
          this.updateUpdatedTableOnDataChange();
          if (this.AutoOpenSideBar) {
            this.log(logPrefix, "AutoOpenSideBar is true, opening it");
            const panelToOpen =
              this.SideBarPanels == "filters" ? "filters" : "columns";
            this.widgetGridApi.openToolPanel(panelToOpen);
          }
          if (this.ResizeGridWithParent) {
            this.log(
              logPrefix,
              "since ResizeGridWithParent is true, resizing grid",
            );
            this.widgetGridApi.sizeColumnsToFit();
          }
        };
        this.gridOptions.getRowId = (params) => {
          this.log(
            "getRowId(): ",
            `params.data = ${JSON.stringify(params.data)}`,
          );
          return params.data[AG_GRID_ROW_ID_FIELD].toString();
        };
        if (this.gridOptions.rowModelType == "serverSide") {
          this.log(
            logPrefix,
            `grid row model is server side, setting ServerSideDataSource, this.RowIdField = ${this.RowIdField}`,
          );
          this.gridOptions.getRowId = (params) => {
            return params.data[this.RowIdField];
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
          `this.gridOptions = ${JSON.stringify(this.gridOptions)}`,
        );
        const container = document.getElementById(this.jqElement[0].id);
        container.innerHTML = "";
        this.widgetGridApi = createGrid(container, this.gridOptions);
        this.alreadyLoaded = true;
        if (this.ResizeGridWithParent) {
          this.widgetGridApi.onGridSizeChanged = () => {
            this.log(logPrefix, "onGridSizeChanged event called");
            this.widgetGridApi.sizeColumnsToFit();
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
    this.log(
      "createServerSideDatasource(): ",
      `Creating server side data source, thingworxService = ${thingworxService}`,
    );
    return {
      getRows: (params) => {
        const logPrefix = "getRows(): ";
        this.log(
          logPrefix,
          `rows requested by grid: ${JSON.stringify(params.request)}`,
        );
        const requestParams = Object.keys(params.request);
        this.widgetGridApi
          .getStatusPanel(SERVER_SIDE_ROW_COUNT_STATUS_PANEL_ID)
          .updateRowCount("calculating...");
        const request = thingworxService;
        const body = {};
        requestParams.forEach((nextParam, index) => {
          const nextParamValue = params.request[nextParam];
          this.log(
            logPrefix,
            `${index}. nextParam  ${nextParam} = ${nextParamValue} (${typeof nextParamValue})`,
          );
          this.log(
            logPrefix,
            `${index}. nextParam isArray? = ${Array.isArray(nextParamValue)}`,
          );
          if (nextParamValue !== "") {
            body[nextParam] = JSON.stringify(nextParamValue);
          }
        });
        this.currentServerSideFilterParams = body;
        if (this.ServerSideAdditionalRequestParams) {
          this.log(
            logPrefix,
            `adding additional params specified by ServerSideAdditionalRequestParams: ${this.ServerSideAdditionalRequestParams}`,
          );
          const additionalParams_1 = JSON.parse(
            this.ServerSideAdditionalRequestParams,
          );
          Object.keys(additionalParams_1).forEach(
            (nextAdditionalParam) => {
              this.log(
                logPrefix,
                `adding nextAdditionalParam: ${nextAdditionalParam}`,
              );
              body[nextAdditionalParam] =
                additionalParams_1[nextAdditionalParam];
            },
          );
        }
        this.log(
          logPrefix,
          `calling request = ${request} with body = ${JSON.stringify(body)}`,
        );
        this.widgetGridApi.showLoadingOverlay();
        fetch(request, {
          method: "POST",
          headers: HEADERS,
          body: JSON.stringify(body),
        }).then((response) => {
          response.json().then((data) => {
            if (response.status !== 200) {
              this.log(
                logPrefix,
                `getRows request failed, error = ${JSON.stringify(response.json())}`,
              );
              params.fail();
            } else {
              this.log(logPrefix, "getRows request succeeded!");
              const lastRowIndex = this.getLastRowIndex(
                params.request,
                data.rows,
              );
              params.success({
                rowData: data.rows,
                rowCount: lastRowIndex,
              });
              this.log(
                logPrefix,
                `returning ${data.rows.length} rows, lastRowIndex = ${lastRowIndex}`,
              );
              this.ServerSideRowModelGridUpdated();
              this.widgetGridApi.hideOverlay();
              if (this.UseAsyncSetFilters) {
                this.updateAsyncColumnFilters(thingworxFilterService);
              }
            }
          });
        });
      },
    };
  }
  updateAsyncColumnFilters(thingworxFilterService) {
    const logPrefix = "getRows().updateAsyncColumnFilters(): ";
    const currentlyFilteredColumns = Object.keys(
      this.widgetGridApi.getFilterModel(),
    );
    this.log(
      logPrefix,
      `since UseAsyncSetFilters is true, updating filter set values, currentlyFilteredColumns = ${JSON.stringify(currentlyFilteredColumns)}`,
    );
    const filterRequest = thingworxFilterService;
    this.log(logPrefix, `calling filterRequest: ${filterRequest}`);
    return fetch(filterRequest, {
      method: "POST",
      headers: HEADERS,
    }).then((response) => {
      response.json().then((data) => {
        if (response.status !== 200) {
          this.log(
            logPrefix,
            `filterRequest failed, error = ${JSON.stringify(response.json())}`,
          );
        } else {
          this.log(
            logPrefix,
            `filterRequest succeeded, response = ${JSON.stringify(data)}`,
          );
          if (Object.keys(data).length > 0 && data.hasOwnProperty("rows")) {
            const updatedFilters_1 = JSON.parse(data.rows[0].result);
            this.log(
              logPrefix,
              `updatedFilters = ${JSON.stringify(updatedFilters_1)}`,
            );
            this.serverSideSetFilters = updatedFilters_1;
            Object.keys(updatedFilters_1).forEach((nextColumn) => {
              if (currentlyFilteredColumns.includes(nextColumn)) {
                this.log(
                  logPrefix,
                  `skipping nextColumn because it is already filtered: ${nextColumn}`,
                );
              } else {
                this.log(
                  logPrefix,
                  `updating nextColumn filter ${nextColumn} with values: ${updatedFilters_1[nextColumn]}`,
                );
                this.widgetGridApi
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
            action: () => {
              return this.exportServerSideGrid("csv");
            },
            icon: '<span class="ag-icon ag-icon-csv" unselectable="on" role="presentation"></span>',
          },
          {
            name: "Export To Excel",
            action: () => {
              return this.exportServerSideGrid("excel");
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
    for (var s = `${num}`; s.length < size; ) {
      s = `0${s}`;
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
    const logPrefix = `exportServerSideGrid(${format}): `;
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
        `sending request = ${request} with body: ${JSON.stringify(currentFilterRequestBody)}`,
      );
    }
    this.widgetGridApi.showLoadingOverlay();
    fetch(request, {
      method: "POST",
      headers: HEADERS,
      body: JSON.stringify(currentFilterRequestBody),
    })
      .then((response) => {
        if (response.status !== 200) {
          if (this.EnableConsoleLogging) {
            this.log(
              logPrefix,
              `error, response = ${JSON.stringify(response)}`,
            );
          }
          this.widgetGridApi.hideOverlay();
          this.ShowErrorMessage();
          return;
        }
        response.json().then((data) => {
          if (this.EnableConsoleLogging) {
            this.log(logPrefix, "got response, building export grid");
          }
          const rows = [];
          data.rows.forEach(function (nextRow, index) {
            nextRow[AG_GRID_ROW_ID_FIELD] = index;
            if (nextRow.hasOwnProperty("cellDataType")) {
              delete nextRow.cellDataType;
            }
            rows.push(nextRow);
          });
          if (this.EnableConsoleLogging) {
            this.log(
              logPrefix,
              `export grid will have ${rows.length} rows`,
            );
          }
          let exportGridConfig: any = {};
          const gridOptionsCopy = JSON.parse(JSON.stringify(this.gridOptions));
          const gridOptionPropertiesToIgnore = [
            "statusBar",
            "toolPanelParams",
            "rowData",
            "rowClassRules",
            "cacheBlockSize",
            "serverSideDatasource",
          ];
          if (this.EnableConsoleLogging) {
            this.log(
              logPrefix,
              `1. gridOptionsCopy = ${JSON.stringify(gridOptionsCopy)}`,
            );
          }
          Object.keys(gridOptionsCopy)
            .filter(function (nextProp) {
              return !gridOptionPropertiesToIgnore.includes(nextProp);
            })
            .forEach(function (nextProp) {
              exportGridConfig[nextProp] = gridOptionsCopy[nextProp];
            });
          if (this.EnableConsoleLogging) {
            this.log(
              logPrefix,
              `2. exportGridConfig = ${JSON.stringify(exportGridConfig)}`,
            );
          }
          if (this.EnableConsoleLogging) {
            this.log(logPrefix, `2. rows = ${JSON.stringify(rows)}`);
          }
          exportGridConfig.rowData = rows;
          exportGridConfig.rowModelType = "clientSide";
          delete exportGridConfig.cacheBlockSize;
          delete exportGridConfig.serverSideDatasource;
          if (this.EnableConsoleLogging) {
            this.log(
              logPrefix,
              `3. exportGridConfig = ${JSON.stringify(exportGridConfig)}`,
            );
          }
          const addedColumns = [];
          if (this.ServerSideDynamicExportColumnsPrefix) {
            if (this.EnableConsoleLogging) {
              this.log(
                logPrefix,
                `adding in dynamically-generated columns, ServerSideDynamicExportColumnsPrefix = ${this.ServerSideDynamicExportColumnsPrefix}`,
              );
            }
            const columnPrefixesToAdd_1 = JSON.parse(
              this.ServerSideDynamicExportColumnsPrefix,
            );
            const datashapeFields = Object.keys(
              data.dataShape.fieldDefinitions,
            );
            if (this.EnableConsoleLogging) {
              this.log(
                logPrefix,
                `datashapeFields = ${JSON.stringify(datashapeFields)}`,
              );
            }
            datashapeFields.forEach((nextField) => {
              if (this.EnableConsoleLogging) {
                this.log(
                  logPrefix,
                  `checking if we should add nextField = ${nextField}`,
                );
              }
              if (
                columnPrefixesToAdd_1.some(function (nextPrefix) {
                  return nextField.startsWith(nextPrefix);
                })
              ) {
                if (this.EnableConsoleLogging) {
                  this.log(
                    logPrefix,
                    `yes! we should add nextField = ${nextField}`,
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
          if (this.EnableConsoleLogging) {
            this.log(
              logPrefix,
              `creating export grid, exportGridConfig = ${JSON.stringify(exportGridConfig)}`,
            );
          }
          try {
            let exportedGrid = createGrid(exportDiv, exportGridConfig);
            if (this.EnableConsoleLogging) {
              this.log(
                logPrefix,
                `created export grid which has ${exportedGrid.getDisplayedRowCount()} rows`,
              );
            }
            if (this.SavedState) {
              if (this.EnableConsoleLogging) {
                this.log(
                  logPrefix,
                  `restoring saved state: ${this.SavedState}`,
                );
              }
              try {
                const savedStateAsJson_1 = JSON.parse(this.SavedState);
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
                    if (this.EnableConsoleLogging) {
                      this.log(logPrefix, "restoring filters...");
                    }
                    const savedFilters_1 = savedStateAsJson_1.FilterState;
                    Object.keys(savedFilters_1).forEach((nextFilter) => {
                      if (
                        savedFilters_1[nextFilter].filterType == "date" &&
                        savedFilters_1[nextFilter].type == "blank"
                      ) {
                        if (this.EnableConsoleLogging) {
                          this.log(
                            logPrefix,
                            `next filters ${nextFilter} is date=blank so replacing it with epoch 0`,
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
                  if (this.EnableConsoleLogging) {
                    this.log(
                      logPrefix,
                      `restoring saved state: ${this.SavedState}`,
                    );
                  }
                  if (this.EnableConsoleLogging) {
                    this.log(
                      logPrefix,
                      `after setFilterModel grid has ${exportedGrid.getDisplayedRowCount()} rows`,
                    );
                  }
                }
              } catch (err) {
                if (this.EnableConsoleLogging) {
                  this.log(logPrefix, `Error parsing JSON? err = ${err}`);
                }
                this.widgetGridApi.hideOverlay();
              }
            }
            if (this.EnableConsoleLogging) {
              this.log(
                logPrefix,
                `exporting grid which has ${exportedGrid.getDisplayedRowCount()} rows`,
              );
            }
            if (format == "excel") {
              exportedGrid.exportDataAsExcel({
                processCellCallback: this.dateCellCallBack,
              });
            }
            if (format == "csv") {
              exportedGrid.exportDataAsCsv({
                processCellCallback: this.dateCellCallBack,
              });
            }
            exportedGrid = null;
            exportGridConfig = null;
            this.widgetGridApi.hideOverlay();
          } catch (err) {
            if (this.EnableConsoleLogging) {
              this.log(logPrefix, `error exporting grid: ${err}`);
            }
          }
        });
      })
      .catch(function (err) {
        this.log(logPrefix, `err: ${err}`);
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
    this.log(logPrefix, `selectedRows has ${selectedRows.length} rows`);
    this.indices = selectedRows.map(function (nextRow) {
      return parseFloat(nextRow.id);
    });
    this.log(logPrefix, `indices = ${JSON.stringify(this.indices)}`);
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
    this.log(logPrefix, `firstRenderedRowId = ${firstRenderedRowId}`);
    this.log(logPrefix, `lastRenderedRowId = ${lastRenderedRowId}`);
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
        `setting nodes expanded/collapsed, levelsToExpand = ${levelsToExpand_1}`,
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
    const logPrefix = "cellValueChanged(): ";
    this.log(logPrefix, "cell was changed");
    this.log(logPrefix, `old value = ${event.oldValue}`);
    this.log(logPrefix, `new value = ${event.newValue}`);
    this.log(logPrefix, `source = ${event.source}`);
    event.node.data.changed = true;
    const updatedRows = [];
    this.widgetGridApi.forEachNode((node) => {
      if (node.data && node.data.changed) {
        this.log(
          logPrefix,
          `updating edited table, next node = ${JSON.stringify(node.data)}`,
        );
        this.log(
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
      `based on whether or not this is a client-side or server-side grid, rowIdField = ${rowIdField}`,
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
    this.log(logPrefix, `existingRowIds = ${JSON.stringify(existingRowIds)}`);
    this.log(logPrefix, `updatedRowIds = ${JSON.stringify(updatedRowIds)}`);
    this.log(
      logPrefix,
      `idsThatDoNotExistYet = ${JSON.stringify(idsThatDoNotExistYet)}`,
    );
    updatedRows.forEach((nextUpdatedRow) => {
      this.log(
        logPrefix,
        `nextUpdatedRow[rowIdField] = ${nextUpdatedRow[rowIdField]}`,
      );
      if (idsThatDoNotExistYet.includes(nextUpdatedRow[rowIdField])) {
        this.EditedTable.rows.push(nextUpdatedRow);
      }
    });
    this.log(logPrefix, "this.EditedTable updated...");
    this.CellEdited();
    this.updateUpdatedTable();
    this.updateRenderedTable();
  }
  updateSelectedRows(event) {
    // A bulk selection publishes once when it finishes; see applyBulkSelection.
    if (this.bulkSelecting) return;
    const logPrefix = "updateSelectedRows(): ";
    const selectedRows = this.widgetGridApi.getSelectedRows();
    this.SelectedRows = {
      dataShape: this.dataSourceDataShape,
      rows: selectedRows,
    };
    const selectedRowNumbers = [];
    this.widgetGridApi.getSelectedNodes().forEach((row) => {
      const nextRowIndex = row.rowIndex;
      this.log(logPrefix, `next selected row index = ${nextRowIndex}`);
      selectedRowNumbers.push(nextRowIndex + 1);
    });
    this.SelectedRowNumbers = selectedRowNumbers.join();
    this.log(logPrefix, `selectedRowNumbers = ${selectedRowNumbers}`);
    this.log(
      logPrefix,
      `Selection updated, SelectedRowNumbers = ${this.SelectedRowNumbers}`,
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
    const logPrefix = "updateUpdatedTableOnDataChange(): ";
    const rows = [];
    this.widgetGridApi.forEachNode((node) => {
      this.log(
        logPrefix,
        `updating UpdatedTable with next row: ${JSON.stringify(node.data)}`,
      );
      rows.push(node.data);
    });
    this.log(logPrefix, `updating UpdatedTable with ${rows.length} rows`);
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
        `updating FilteredTable with ${rows.length} rows`,
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
        `updating RenderedTable with ${rows_1.length} rows`,
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
        `overriding text size with grid property GridTextSize: ${this.GridTextSize}`,
      );
      newCssValues["--grid-font-size"] = `${this.GridTextSize}px`;
    }
    if (this.GridHeaderSize !== undefined) {
      this.log(
        logPrefix,
        `overriding header size with grid property GridHeaderSize: ${this.GridHeaderSize}`,
      );
      newCssValues["--header-font-size"] = `${this.GridHeaderSize}px`;
    }
    if (this.HideBorders) {
      this.log(logPrefix, "HideBorders is true, removing borders: ");
      newCssValues["--ag-borders"] = "none";
    }
    Object.keys(newCssValues).forEach((nextCssVariable) => {
      this.log(
        logPrefix,
        `setting nextCssVariable ${nextCssVariable} to ${newCssValues[nextCssVariable]}`,
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
      `cellEditorSelectorConfig = ${JSON.stringify(cellEditorSelectorConfig)}`,
    );
    if (this.gridConfig.hasOwnProperty("ColumnConfig")) {
      const columnConfig = this.gridConfig.ColumnConfig;
      this.log(logPrefix, `columnConfig = ${JSON.stringify(columnConfig)}`);
      this.log(
        logPrefix,
        `does it have that attribute? = ${columnConfig.hasOwnProperty(currentAttribute)}`,
      );
      if (columnConfig.hasOwnProperty(currentAttribute)) {
        const matchingFieldOptions = columnConfig[currentAttribute];
        this.log(
          logPrefix,
          `matchingFieldOptions = ${JSON.stringify(matchingFieldOptions)}`,
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
              `fieldThatDeterminesCellEditor = ${fieldThatDeterminesCellEditor}`,
            );
            const fieldValue = params.data[fieldThatDeterminesCellEditor];
            if (
              fieldValue &&
              (this.log(logPrefix, `that field has a value of = ${fieldValue}`),
              cellEditorSelectorConfig.hasOwnProperty(fieldValue))
            ) {
              const selectedCellConfig = cellEditorSelectorConfig[fieldValue];
              this.log(
                logPrefix,
                `CellEditorSelectorConfig has that value, the config = ${JSON.stringify(selectedCellConfig)}`,
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
      `returning returnValue = ${JSON.stringify(returnValue)}`,
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
        `cellRendererSelectorConfig = ${JSON.stringify(cellRendererSelectorConfig)}`,
      );
      if (this.gridConfig.hasOwnProperty("ColumnConfig")) {
        const columnConfig = this.gridConfig.ColumnConfig;
        this.log(logPrefix, `columnConfig = ${JSON.stringify(columnConfig)}`);
        this.log(
          logPrefix,
          `does it have that attribute? = ${columnConfig.hasOwnProperty(currentAttribute)}`,
        );
        if (columnConfig.hasOwnProperty(currentAttribute)) {
          const matchingFieldOptions = columnConfig[currentAttribute];
          this.log(
            logPrefix,
            `matchingFieldOptions = ${JSON.stringify(matchingFieldOptions)}`,
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
                `fieldThatDeterminesCellRenderer = ${fieldThatDeterminesCellRenderer}`,
              );
              const fieldValue = params.data[fieldThatDeterminesCellRenderer];
              if (
                fieldValue &&
                (this.log(
                  logPrefix,
                  `that field has a value of = ${fieldValue}`,
                ),
                cellRendererSelectorConfig.hasOwnProperty(fieldValue))
              ) {
                const selectedCellConfig =
                  cellRendererSelectorConfig[fieldValue];
                this.log(
                  logPrefix,
                  `cellRendererSelectorConfig has that value, the config = ${JSON.stringify(selectedCellConfig)}`,
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
        `returning returnValue = ${JSON.stringify(returnValue)}`,
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
    if (this.PasteIndexColumn) {
      this.log(
        "reindexOnPaste(): ",
        `Starting, reindexing column: ${this.PasteIndexColumn}`,
      );
      this.widgetGridApi.forEachNode((node, index) => {
        const nextRowData = node.data;
        this.log(
          "reindexOnPaste(): ",
          `reindexing row: ${JSON.stringify(nextRowData)}`,
        );
        nextRowData[this.PasteIndexColumn] = index + 1;
        node.setData(nextRowData);
      });
    }
  }
  @property
  set Data(Data) {
    if (Data) {
      this.log(
        "setData(): ",
        `setting Data, this.widgetGridApi is undefined? = ${this.widgetGridApi === undefined}, this.alreadyLoaded = ${this.alreadyLoaded}`,
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
      `selectedRowIndices = ${JSON.stringify(selectedRowIndices)}`,
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
    const logPrefix = `valueWillBind(${info.TargetProperty}): `;
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
        `setting CustomServerSideStatusPanelRowCount, rowCount = ${rowCount}`,
      );
      this.widgetGridApi
        .getStatusPanel(SERVER_SIDE_ROW_COUNT_STATUS_PANEL_ID)
        .updateRowCount(rowCount);
      this.updateCustomServerSideStatusPanelRowCount();
    }
  }
  @event
  declare CellEdited: any;
  @event
  declare StateSaved: any;
  @event
  declare GridLoaded: any;
  @event
  declare RowDoubleClicked: any;
  @event
  declare DataFiltered: any;
  @event
  declare ActionCellButtonClicked: any;
  @event
  declare ShowInfoMessage: any;
  @event
  declare ShowWarningMessage: any;
  @event
  declare ShowErrorMessage: any;
  @event
  declare ServerSideRowModelGridUpdated: any;
  @event
  declare SelectedRowsChanged: any;
}

// The original widget also published relatedDropdownValues on window. This rebuild keeps it
// private so it never overwrites the original agGridWidget2's object when both are installed.
