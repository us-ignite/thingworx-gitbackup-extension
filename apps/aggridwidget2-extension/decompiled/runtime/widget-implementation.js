// Extracted from webcrack deobfuscated.js for navigation.
// This uses helpers and globals defined elsewhere in the bundle; it is not standalone source.
    (function (_super) {
      function AgGridWidget2() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.gridConfig = {
          ColumnConfig: {},
          RowConfig: {},
          GridOptions: {},
          EditableFieldsColumn: "",
          CellClassColumn: "",
          CellEditorSelectorConfig: {},
          CellRendererSelectorConfig: {},
          excelStyles: []
        };
        _this.alreadyLoaded = false;
        _this.log = function (logPrefix, logMessage) {
          if (_this.EnableConsoleLogging) {
            console.log("AgGridWidget-" + _this.widgetId + "." + logPrefix + logMessage);
          }
        };
        _this.convertRowsToSelectToNumeric = function (rowsToSelect) {
          var logPrefix = ".convertRowsToSelectToNumeric(" + rowsToSelect + "): ";
          var returnArray = [];
          var singleInteger = Number(rowsToSelect);
          if (!Number.isNaN(singleInteger)) {
            returnArray.push(singleInteger - 1);
          }
          try {
            if (rowsToSelect.indexOf("-") > 0) {
              var startStopValues = rowsToSelect.split("-");
              var startValue = parseInt(startStopValues[0]);
              var endValue = parseInt(startStopValues[1]);
              _this.log(logPrefix, "startValue = " + startValue + ", endValue = " + endValue);
              for (var i = startValue; i < endValue + 1; i++) {
                _this.log(logPrefix, "adding row to select # " + i);
                returnArray.push(i - 1);
              }
            }
            if (rowsToSelect.indexOf(",") > 0) {
              rowsToSelect.split(",").map(function (nextRowAsString) {
                return returnArray.push(parseInt(nextRowAsString) - 1);
              });
            }
          } catch (err) {
            _this.log(logPrefix, "error selecting default rows, err = " + err);
          }
          return returnArray;
        };
        _this.AddNewRow = function () {
          _this.log("AddNewRow(): ", "Adding new row, this.NewRowIndex = " + _this.NewRowIndex);
          var newRow = {};
          newRow[grid.ME] = new Date().getTime();
          var newItems = [newRow];
          _this.widgetGridApi.applyTransaction({
            add: newItems,
            addIndex: _this.NewRowIndex ? _this.NewRowIndex : 0
          });
          _this.updateUpdatedTable();
          _this.updateRenderedTable();
        };
        _this.AddNewRowToBottom = function () {
          _this.log(".AddNewRowToBottom(): ", "Adding new row to bottom of grid");
          var newRow = {};
          newRow[grid.ME] = new Date().getTime();
          var newItems = [newRow];
          _this.widgetGridApi.applyTransaction({
            add: newItems
          });
          _this.updateUpdatedTable();
          _this.updateRenderedTable();
        };
        _this.DeleteSelectedRows = function () {
          var _a;
          var logPrefix = "DeleteSelectedRows(): ";
          var selectedRows = _this.widgetGridApi.getSelectedRows();
          _this.log(logPrefix, "Deleting " + selectedRows.length + " rows, selectedRows = " + JSON.stringify(selectedRows));
          if (_this.gridOptions.hasOwnProperty("rowModelType") && _this.gridOptions.rowModelType == "serverSide") {
            _this.log(logPrefix, "deleting from server-side row model");
            _this.widgetGridApi.applyServerSideTransaction({
              remove: selectedRows
            });
          } else {
            _this.log(logPrefix, "deleting from client-side row model");
            _this.widgetGridApi.applyTransaction({
              remove: selectedRows
            });
            _this.updateUpdatedTable();
          }
          var currentEditedTableRows = _this.EditedTable.rows;
          var updatedEditedTableRows = [];
          _this.log(logPrefix, "deleting rows, currentEditedTableRows = " + JSON.stringify(currentEditedTableRows));
          currentEditedTableRows.forEach(function (nextEditedRow) {
            _this.log(logPrefix, "nextEditedRow = " + JSON.stringify(nextEditedRow));
            var nextEditedRowWasDeleted = false;
            selectedRows.forEach(function (nextDeletedRow) {
              if (JSON.stringify(nextDeletedRow) == JSON.stringify(nextEditedRow)) {
                nextEditedRowWasDeleted = true;
              }
            });
            _this.log(logPrefix, "nextEditedRowWasDeleted = " + nextEditedRowWasDeleted);
            if (!nextEditedRowWasDeleted) {
              updatedEditedTableRows.push(nextEditedRow);
            }
          });
          (_a = _this.DeletedTable.rows).push.apply(_a, selectedRows);
          _this.EditedTable.rows = updatedEditedTableRows;
          _this.log(logPrefix, "deleting rows, now EditedTableRows = " + JSON.stringify(_this.EditedTable.rows));
        };
        _this.ResizeAll = function () {
          _this.log("ResizeAll(): ", "calling resize all...");
          try {
            _this.widgetGridApi.sizeColumnsToFit();
          } catch (err) {
            _this.log("ResizeAll(): ", "err: " + err);
          }
        };
        _this.SaveState = function () {
          _this.log("SaveState(): ", "saving state...");
          var newState = {};
          newState.ColumnState = _this.widgetGridApi.getColumnState();
          newState.FilterState = _this.widgetGridApi.getFilterModel();
          _this.SavedState = JSON.stringify(newState);
          _this.StateSaved();
          _this.updateRenderedTable();
        };
        _this.RestoreState = function () {
          var logPrefix = "RestoreState.Set(): ";
          _this.log(logPrefix, "restoring saved state...");
          if (_this.SavedState) {
            _this.log(logPrefix, "restoring saved state: " + _this.SavedState);
            try {
              var savedStateAsJson = JSON.parse(_this.SavedState);
              if (_this.widgetGridApi && savedStateAsJson.hasOwnProperty("ColumnState")) {
                _this.log(logPrefix, "restoring columns to: " + JSON.stringify(savedStateAsJson));
                _this.widgetGridApi.applyColumnState({
                  state: savedStateAsJson.ColumnState,
                  applyOrder: true
                });
              }
              if (savedStateAsJson.hasOwnProperty("FilterState")) {
                _this.log(logPrefix, "restoring filters...");
                _this.widgetGridApi.setFilterModel(savedStateAsJson.FilterState);
              }
            } catch (err) {
              _this.log(logPrefix, "Error parsing JSON? err = " + err);
            }
          }
        };
        _this.SizeAllColumns = function () {
          _this.log("SizeAllColumns(): ", "resizing all columns...");
          var allColumnIds = [];
          _this.widgetGridApi.getColumns().forEach(function (column) {
            allColumnIds.push(column.getId());
          });
          _this.log("SizeAllColumns(): ", "allColumnIds = " + allColumnIds);
          _this.widgetGridApi.autoSizeColumns(allColumnIds, false);
        };
        _this.ClearAllFilters = function () {
          _this.widgetGridApi.setFilterModel(null);
          _this.widgetGridApi.onFilterChanged();
        };
        _this.QuickFilterSearch = function () {
          _this.widgetGridApi.setQuickFilter(_this.QuickFilterText);
        };
        _this.OpenSidePanel = function () {
          _this.widgetGridApi.openToolPanel(_this.TargetSideBarPanel);
        };
        _this.CloseSidePanel = function () {
          _this.widgetGridApi.closeToolPanel();
        };
        _this.RefreshGrid = function () {
          _this.widgetGridApi.destroy();
          _this.widgetGridApi = createGrid(document.getElementById(_this.jqElement[0].id), _this.gridOptions);
        };
        _this.ExpandAllRowGroups = function () {
          _this.widgetGridApi.expandAll();
        };
        _this.CollapseAllRowGroups = function () {
          _this.widgetGridApi.collapseAll();
        };
        _this.ShowSideBar = function () {
          _this.widgetGridApi.setSideBarVisible(true);
        };
        _this.HideSideBar = function () {
          _this.widgetGridApi.setSideBarVisible(false);
        };
        _this.AddNewRowWithData = function () {
          var newRowData = JSON.parse(_this.NewRowData);
          _this.log("AddNewRowWithData(): ", "adding new row with data, newRowData = " + JSON.stringify(newRowData));
          _this.widgetGridApi.applyTransaction({
            add: newRowData,
            addIndex: _this.NewRowIndex ? _this.NewRowIndex : 0
          });
          _this.gridOptions.context.updateEditedTable(newRowData);
        };
        _this.ClearDeletedTable = function () {
          _this.DeletedTable = {
            dataShape: _this.dataSourceDataShape,
            rows: []
          };
        };
        _this.RestoreDeletedTable = function () {
          var logPrefix = "RestoreDeletedTable(): ";
          _this.log(logPrefix, "restoring deleted rows, this.DeletedTable = " + _this.DeletedTable.rows);
          var currentRows = [];
          _this.widgetGridApi.forEachNode(function (node) {
            return currentRows.push(node.data);
          });
          _this.DeletedTable.rows.forEach(function (nextDeletedRow) {
            return currentRows.push(nextDeletedRow);
          });
          _this.widgetGridApi.setRowData(currentRows);
          _this.log(logPrefix, "resseting deleted table...");
          _this.DeletedTable = {
            dataShape: _this.dataSourceDataShape,
            rows: []
          };
          _this.log(logPrefix, "done");
        };
        _this.SelectSpecifiedRows = function () {
          var logPrefix = "SelectSpecifiedRows(" + _this.RowsToSelect + "): ";
          _this.log(logPrefix, "selecting specified rows, rowsToSelect = " + _this.RowsToSelect);
          var rowsToSelect = _this.convertRowsToSelectToNumeric(_this.RowsToSelect);
          _this.log(logPrefix, "selecting specified rows, rowsToSelect = " + rowsToSelect);
          _this.widgetGridApi.forEachNode(function (node, index) {
            node.setSelected(rowsToSelect.includes(index));
          });
        };
        _this.SelectRowsByFieldValues = function () {
          var _a;
          var logPrefix = "SelectRowsByFieldValues(" + _this.FieldNameToUseForRowSelection + "): ";
          _this.log(logPrefix, "selecting rows that match entries: " + _this.FieldValuesToUseForRowSelection);
          var valuesToSelect = (_a = _this.FieldValuesToUseForRowSelection) === null || _a === undefined ? undefined : _a.split(",");
          _this.log(logPrefix, "valuesToSelect = " + valuesToSelect);
          _this.widgetGridApi.forEachNode(function (node) {
            var rowFieldValue = node.data[_this.FieldNameToUseForRowSelection] + "";
            _this.log(logPrefix, "next row this field has a value of: " + rowFieldValue);
            if (valuesToSelect.includes(rowFieldValue)) {
              _this.log(logPrefix, "this row matches, setting to expanded and selected");
              for (var myParent = node.parent; myParent;) {
                myParent.setExpanded(true);
                myParent = myParent.parent;
              }
              node.setSelected(true);
              node.setExpanded(true);
            }
          });
        };
        _this.SelectRowsByArrayValues = function () {
          var logPrefix = "SelectRowsByArrayValues(" + _this.FieldNameToUseForRowSelection + "): ";
          _this.log(logPrefix, "selecting rows that match entries: " + _this.FieldValuesToUseForRowSelection);
          var valuesToSelect = JSON.parse(_this.FieldValuesToUseForRowSelection);
          _this.log(logPrefix, "valuesToSelect = " + valuesToSelect);
          _this.widgetGridApi.forEachNode(function (node) {
            var rowFieldValue = node.data[_this.FieldNameToUseForRowSelection] + "";
            _this.log(logPrefix, "next row this field has a value of: " + rowFieldValue);
            if (valuesToSelect.includes(rowFieldValue)) {
              _this.log(logPrefix, "this row matches, setting to expanded and selected");
              for (var myParent = node.parent; myParent;) {
                myParent.setExpanded(true);
                myParent = myParent.parent;
              }
              node.setSelected(true);
              node.setExpanded(true);
            }
          });
        };
        _this.ScrollToSpecifiedRow = function () {
          var logPrefix = "ScrollToSpecifiedRow(" + _this.ScrollIndex + "): ";
          var indexAsInteger = Number(_this.ScrollIndex);
          var ScrollToPosition = _this.ScrollPosition;
          _this.log(logPrefix, "scrolling to row using position: " + ScrollToPosition);
          if (!Number.isNaN(indexAsInteger)) {
            if (_this.ShowGridAsTree) {
              _this.log(logPrefix, "grid is a tree, expanding index # " + _this.ScrollIndex);
              _this.widgetGridApi.forEachNode(function (node, index) {
                if (index == _this.ScrollIndex) {
                  _this.log(logPrefix, "node # matches, expanding row");
                  _this.widgetGridApi.setRowNodeExpanded(node, true);
                }
              });
            }
            _this.widgetGridApi.ensureIndexVisible(indexAsInteger, VISIBLE_INDEX_POSITIONS[ScrollToPosition]);
          }
          _this.log(logPrefix, "done");
        };
        _this.SelectNextRow = function () {
          var renderedGrid = _this.widgetGridApi.getRenderedNodes();
          var nextRowIsRowToSelect = false;
          var lastSelectedRowId = _this.indices[_this.indices.length - 1];
          _this.log("SelectNextRow(): ", "lastSelectedRowId = " + lastSelectedRowId);
          renderedGrid.every(function (rowNode) {
            var nextRowNodeId = parseInt(rowNode.id);
            _this.log("SelectNextRow(): ", "next rendered grid row id = " + nextRowNodeId);
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
        };
        _this.SelectPreviousRow = function () {
          var renderedGrid = _this.widgetGridApi.getRenderedNodes().reverse();
          var nextRowIsRowToSelect = false;
          var firstSelectedRowId = _this.indices[0];
          _this.log("SelectPreviousRow(): ", "firstSelectedRowId = " + firstSelectedRowId);
          renderedGrid.every(function (rowNode) {
            var nextRowNodeId = parseInt(rowNode.id);
            _this.log("SelectPreviousRow(): ", "next rendered grid row id = " + nextRowNodeId);
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
        };
        _this.renderHtml = function () {
          return "<div id=\"myGrid\" class=\"widget-content\" style=\"width: 100%; height: 100%; overflow: visible !important;\"></div>";
        };
        _this.buildGrid = function () {
          var logPrefix = "buildGrid(): ";
          _this.widgetId = _this.jqElementId;
          if (_this.Data) {
            try {
              _this.log(logPrefix, "starting...");
              var dataAsJson = JSON.parse(JSON.stringify(_this.Data));
              _this.log(logPrefix, "dataAsJson = " + JSON.stringify(dataAsJson));
              _this.log(logPrefix, "this.gridConfig = " + JSON.stringify(_this.gridConfig));
              _this.dataSourceDataShape = dataAsJson.dataShape;
              _this.originalDataRows = JSON.parse(JSON.stringify(dataAsJson.rows));
              _this.log(logPrefix, "originalDataRows = " + JSON.stringify(_this.originalDataRows));
              var gridTheme = _this.GridTheme ?? "ag-theme-alpine";
              _this.log(logPrefix, "this.GridTheme = " + _this.GridTheme + ", so gridTheme = " + gridTheme);
              document.getElementById(_this.jqElement[0].id).classList.add(gridTheme);
              _this.EditedTable = {
                dataShape: _this.dataSourceDataShape,
                rows: []
              };
              _this.DeletedTable = {
                dataShape: _this.dataSourceDataShape,
                rows: []
              };
              if (_this.GridConfig) {
                _this.gridConfig = JSON.parse(_this.GridConfig);
              }
              _this.log(logPrefix, "getGridOptions with this.widgetId = " + _this.widgetId + ", alreadyLoaded = " + _this.alreadyLoaded);
              if (_this.UseAsyncSetFilters && _this.gridConfig.hasOwnProperty("ColumnConfig")) {
                var columnConfig_1 = _this.gridConfig.ColumnConfig;
                _this.log(logPrefix, "creating initialFilterValues, columnConfig = " + JSON.stringify(columnConfig_1));
                var initialFilterValues_1 = {};
                Object.keys(columnConfig_1).forEach(function (nextColumn) {
                  var nextColumnConfig = columnConfig_1[nextColumn];
                  if (nextColumnConfig.hasOwnProperty("filterParams") && nextColumnConfig.filterParams.hasOwnProperty("values")) {
                    _this.log(logPrefix, "initialFilterValues: nextColumn " + nextColumn + " has filter params: " + JSON.stringify(nextColumnConfig.filterParams));
                    var nextColumnFilterValues = nextColumnConfig.filterParams.values;
                    _this.log(logPrefix, "initialFilterValues: adding values for nextColumn " + nextColumn + ": " + JSON.stringify(nextColumnFilterValues));
                    initialFilterValues_1[nextColumn] = nextColumnFilterValues;
                  }
                });
                _this.log(logPrefix, "done creating initialFilterValues = " + JSON.stringify(initialFilterValues_1));
                _this.serverSideSetFilters = initialFilterValues_1;
              }
              _this.gridOptions = (0, grid.tF)(dataAsJson, _this.ShowFloatingFilters, _this.CheckBoxSelection, _this.FilterType == "values", _this.gridConfig, _this.ShowGridAsTree, _this.ChildField, _this.ParentField, _this.TreePathField, _this.TreePathFieldDelimeter, _this.TreeColumnField, _this.DateFormat, _this.EnableConsoleLogging, _this.UseAsyncSetFilters, _this.widgetId);
              _this.gridOptions.context = {
                EnableConsoleLogging: _this.EnableConsoleLogging,
                TreeColumnField: _this.TreeColumnField,
                DateFormat: _this.DateFormat ? _this.DateFormat : "DD-MM-YYYY",
                EditableFieldsColumn: _this.gridConfig.EditableFieldsColumn,
                CellClassColumn: _this.gridConfig.CellClassColumn,
                RowHeight: _this.RowHeight,
                CustomMashupStatusPanelMashupName: _this.CustomMashupStatusPanelMashupName,
                WidgetId: _this.widgetId,
                DateMinDate: _this.DateEditorsMinimumDate,
                DateMaxDate: _this.DateEditorsMaximumDate,
                DateShowWeekNumbers: _this.DateEditorShowWeekNumbers,
                emitCustomActionButtonClick: function (buttonName) {
                  _this.log(logPrefix, "CustomActionCellButton clicked: " + buttonName);
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
                  var rowIdField = _this.gridOptions.hasOwnProperty("rowModelType") && _this.gridOptions.rowModelType == "serverSide" ? _this.RowIdField : grid.ME;
                  _this.log(logPrefix, "based on whether or not this is a client-side or server-side grid, rowIdField = " + rowIdField);
                  _this.EditedTable.rows = _this.EditedTable.rows.map(function (nextRow) {
                    return updatedNodeData.find(function (nextUpdatedNode) {
                      return nextRow[rowIdField] === nextUpdatedNode[rowIdField];
                    }) || nextRow;
                  });
                  var existingRowIds = _this.EditedTable.rows.map(function (nextExistingRow) {
                    return nextExistingRow[rowIdField];
                  });
                  var updatedRowIds = updatedNodeData.map(function (nextUpdatedRow) {
                    return nextUpdatedRow[rowIdField];
                  });
                  var idsThatDoNotExistYet = updatedRowIds.filter(function (x) {
                    return !existingRowIds.includes(x);
                  });
                  _this.log(logPrefix, "existingRowIds = " + JSON.stringify(existingRowIds));
                  _this.log(logPrefix, "updatedRowIds = " + JSON.stringify(updatedRowIds));
                  _this.log(logPrefix, "idsThatDoNotExistYet = " + JSON.stringify(idsThatDoNotExistYet));
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
                  DatePicker: DatePicker.l,
                  CustomSelectEditor,
                  CustomImageRenderer: CustomImageRenderer.W,
                  CustomActionCellRenderer,
                  CustomCheckboxRenderer,
                  CustomColumnAggregateFooterRenderer: CustomColumnAggregateFooterRenderer.x,
                  CustomDateRenderer: CustomDateRenderer.j,
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
                  CustomJsonRenderer: CustomJsonRenderer.f
                }
              };
              _this.log(logPrefix, "gridOptions.context = " + JSON.stringify(_this.gridOptions.context));
              _this.gridOptions.defaultColDef = {
                sortable: true,
                resizable: _this.ResizeColumns,
                suppressMovable: !_this.ReorderColumns,
                filter: _this.ShowFloatingFilters,
                editable: _this.AllowEditing != "no"
              };
              _this.gridOptions.rowGroupPanelShow = _this.EnableRowGrouping ? "always" : "never";
              _this.gridOptions.popupParent = document.querySelector("body");
              if (_this.SideBarPanels !== "none") {
                _this.log(logPrefix, "adding sidebar, panels = " + _this.SideBarPanels);
                var toolPanels = [];
                if (_this.SideBarPanels == "columns" || _this.SideBarPanels == "both") {
                  toolPanels.push({
                    id: "columns",
                    labelDefault: "Columns",
                    labelKey: "columns",
                    iconKey: "columns",
                    toolPanel: "agColumnsToolPanel"
                  });
                }
                if (_this.SideBarPanels == "filter" || _this.SideBarPanels == "both") {
                  toolPanels.push({
                    id: "filters",
                    labelDefault: "Filters",
                    labelKey: "filters",
                    iconKey: "filter",
                    toolPanel: "agFiltersToolPanel"
                  });
                }
                _this.gridOptions.sideBar = {
                  toolPanels
                };
              }
              if (_this.RowSelection !== "none") {
                _this.gridOptions.rowSelection = _this.RowSelection == "single" ? "single" : _this.RowSelection == "multiple" ? "multiple" : undefined;
                _this.gridOptions.onSelectionChanged = _this.onSelectionChanged;
              }
              if (_this.AllowEditing == "row") {
                _this.gridOptions.editType = "fullRow";
              }
              if (_this.SingleClickEdit) {
                _this.gridOptions.singleClickEdit = true;
              }
              if (_this.RowHeight) {
                _this.gridOptions.rowHeight = _this.RowHeight;
              }
              if (_this.EmitDoubleClickEvent) {
                _this.gridOptions.onRowDoubleClicked = _this.emitDoubleClickedEvent;
              }
              if (_this.ShowGridAsTree) {
                _this.gridOptions.treeData = true;
              }
              if (_this.EnableCellTextSelection) {
                _this.gridOptions.enableCellTextSelection = true;
              }
              if (_this.EnableRangeSelection) {
                _this.gridOptions.enableRangeSelection = true;
              }
              _this.gridOptions.onRowDragEnd = _this.updateUpdatedTable;
              if (_this.gridOptions.hasOwnProperty("columnDefs")) {
                _this.gridOptions.columnDefs.forEach(function (nextColumnDef, index) {
                  if (nextColumnDef.hasOwnProperty("cellEditorParams") && nextColumnDef.cellEditorParams.hasOwnProperty("useCellEditorSelector")) {
                    _this.log(logPrefix, "updating cellEditorSelector for column def # " + index);
                    _this.gridOptions.columnDefs[index].cellEditorSelector = _this.cellEditorSelector;
                  }
                  if (nextColumnDef.hasOwnProperty("cellRendererParams") && nextColumnDef.cellRendererParams.hasOwnProperty("useCellRendererSelector")) {
                    _this.log(logPrefix, "updating cellRendererSelector for column def # " + index);
                    _this.gridOptions.columnDefs[index].cellRendererSelector = _this.cellRendererSelector;
                  }
                });
              }
              _this.gridOptions.components = _this.gridOptions.context.components;
              if (_this.gridConfig.hasOwnProperty("GridOptions")) {
                var gridConfigOptions = _this.gridConfig.GridOptions;
                _this.log(logPrefix, "adding/overriding grid options specified in gridconfig...");
                for (var prop in gridConfigOptions) {
                  _this.log(logPrefix, "overriding next gridconfig prop " + prop);
                  _this.gridOptions[prop] = gridConfigOptions[prop];
                }
                if (gridConfigOptions.hasOwnProperty("isRowSelectableField")) {
                  var isSelectableField_1 = gridConfigOptions.isRowSelectableField;
                  _this.log(logPrefix, "grid options has isRowSelectableField, defining isSelectable function, isSelectableField = " + isSelectableField_1);
                  _this.gridOptions.isRowSelectable = function (rowNode) {
                    var rowIsSelectable = rowNode.data[isSelectableField_1];
                    _this.log(logPrefix, "checking if rowNode.id " + rowNode.id + " is rowSelectable? = " + rowIsSelectable);
                    return rowIsSelectable;
                  };
                }
              }
              if (_this.ShowServerSideRowTotalsStatusPanel) {
                _this.log(logPrefix, "ShowServerSideRowTotalsStatusPanel is true, adding it");
                var currentStatusPanels = _this.gridOptions.statusBar.statusPanels;
                currentStatusPanels.push({
                  statusPanel: CustomServerSideTotalsStatusPanel,
                  key: grid.Z$,
                  align: "left"
                });
                _this.gridOptions.statusBar.statusPanels = currentStatusPanels;
                _this.log(logPrefix, "statusPanels = " + JSON.stringify(_this.gridOptions.statusBar.statusPanels));
              }
              if (_this.ShowGridAsTree) {
                _this.log(logPrefix, "grid is a tree, updating innerrenderer...");
                if (!_this.gridOptions.hasOwnProperty("autoGroupColumnDef")) {
                  _this.gridOptions.autoGroupColumnDef = {};
                }
                if (!_this.gridOptions.autoGroupColumnDef.hasOwnProperty("cellRendererParams")) {
                  _this.gridOptions.autoGroupColumnDef.cellRendererParams = {};
                }
                _this.gridOptions.autoGroupColumnDef.cellRendererParams.innerRenderer = "CustomTreeColumnCellRenderer";
                _this.gridOptions.autoGroupColumnDef.filterValueGetter = function (params) {
                  return params.data[_this.TreeColumnField];
                };
              }
              _this.gridOptions.onCellValueChanged = _this.cellValueChanged;
              _this.gridOptions.onRowSelected = _this.updateSelectedRows;
              _this.gridOptions.onFilterChanged = function () {
                _this.log("filterChanged(): ", "onFilterChanged event emitted...");
                _this.updateFilteredTable();
                _this.SaveState();
              };
              _this.gridOptions.onSortChanged = function () {
                _this.log(logPrefix, "onSortChanged event emitted...");
                _this.SaveState();
              };
              _this.gridOptions.onColumnEverythingChanged = function () {
                _this.log(logPrefix, "onColumnEverythingChanged event emitted...");
                _this.SaveState();
              };
              _this.gridOptions.onColumnPivotChanged = function () {
                _this.log(logPrefix, "onColumnPivotChanged event emitted...");
                _this.SaveState();
              };
              _this.gridOptions.onColumnPivotModeChanged = function () {
                _this.log(logPrefix, "onColumnPivotModeChanged event emitted...");
                _this.SaveState();
              };
              _this.gridOptions.onColumnMoved = function () {
                _this.log(logPrefix, "onColumnMoved event emitted...");
                _this.SaveState();
              };
              _this.gridOptions.onColumnResized = function () {
                _this.log(logPrefix, "onColumnResized event emitted...");
                _this.SaveState();
              };
              _this.gridOptions.onColumnPinned = function () {
                _this.log(logPrefix, "onColumnPinned event emitted...");
                _this.SaveState();
              };
              _this.gridOptions.onPasteEnd = function () {
                var gridRowCount = _this.widgetGridApi.getDisplayedRowCount() - 1;
                _this.log(logPrefix, "onPasteEnd event emitted, gridRowCount = " + gridRowCount);
                for (var visibleRowIndex = 20; gridRowCount > visibleRowIndex;) {
                  _this.log(logPrefix, "onPasteEnd ensureIndexVisible index = " + visibleRowIndex);
                  _this.widgetGridApi.ensureIndexVisible(visibleRowIndex, "bottom");
                  visibleRowIndex += 20;
                }
                _this.widgetGridApi.ensureIndexVisible(gridRowCount, "bottom");
                _this.log(logPrefix, "onPasteEnd event done, updating updated table...");
                _this.updateUpdatedTableOnDataChange();
                if (_this.ReindexOnPaste) {
                  _this.reindexOnPaste();
                }
              };
              _this.gridOptions.onGridReady = function (event) {
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
                  _this.log(logPrefix, "selecting default rows, DefaultSelectedRows = " + _this.DefaultSelectedRows);
                  try {
                    var rowsToSelect_1 = _this.convertRowsToSelectToNumeric(_this.DefaultSelectedRows);
                    _this.log(logPrefix, "selecting default rows, rowsToSelect = " + rowsToSelect_1);
                    event.api.forEachNode(function (node, index) {
                      node.setSelected(rowsToSelect_1.includes(index));
                    });
                  } catch (err) {
                    _this.log(logPrefix, "error selecting default rows, err = " + err);
                  }
                }
                _this.NoRowSelected = event.api.getSelectedNodes().length == 0;
                _this.AnyRowSelected = event.api.getSelectedNodes().length > 0;
                _this.updateUpdatedTableOnDataChange();
                if (_this.AutoOpenSideBar) {
                  _this.log(logPrefix, "AutoOpenSideBar is true, opening it");
                  var panelToOpen = _this.SideBarPanels == "filters" ? "filters" : "columns";
                  _this.widgetGridApi.openToolPanel(panelToOpen);
                }
                if (_this.ResizeGridWithParent) {
                  _this.log(logPrefix, "since ResizeGridWithParent is true, resizing grid");
                  _this.widgetGridApi.sizeColumnsToFit();
                }
              };
              _this.gridOptions.getRowId = function (params) {
                _this.log("getRowId(): ", "params.data = " + JSON.stringify(params.data));
                return params.data[grid.ME].toString();
              };
              if (_this.gridOptions.hasOwnProperty("rowModelType") && _this.gridOptions.rowModelType == "serverSide") {
                _this.log(logPrefix, "grid row model is server side, setting ServerSideDataSource, this.RowIdField = " + _this.RowIdField);
                _this.gridOptions.getRowId = function (params) {
                  return params.data[_this.RowIdField];
                };
                var serverSideDataSource = _this.createServerSideDatasource(_this.ServerSideDataSource, _this.ServerSideFiltersDataSource);
                _this.gridOptions.serverSideDatasource = serverSideDataSource;
                _this.gridOptions.getContextMenuItems = _this.getContextMenuItems;
              }
              _this.log(logPrefix, "this.gridOptions = " + JSON.stringify(_this.gridOptions));
              var container = document.getElementById(_this.jqElement[0].id);
              container.innerHTML = "";
              _this.widgetGridApi = createGrid(container, _this.gridOptions);
              _this.alreadyLoaded = true;
              if (_this.ResizeGridWithParent) {
                _this.widgetGridApi.onGridSizeChanged = function () {
                  _this.log(logPrefix, "onGridSizeChanged event called");
                  _this.widgetGridApi.sizeColumnsToFit();
                };
              }
              _this.log(logPrefix, "done...");
              _this.GridLoaded();
            } catch (err) {
              console.error("Error building grid, err = ", err);
            }
          }
        };
        _this.createServerSideDatasource = function (thingworxService, thingworxFilterService) {
          _this.log("createServerSideDatasource(): ", "Creating server side data source, thingworxService = " + thingworxService);
          return {
            getRows: function (params) {
              var logPrefix = "getRows(): ";
              _this.log(logPrefix, "rows requested by grid: " + JSON.stringify(params.request));
              var requestParams = Object.keys(params.request);
              _this.widgetGridApi.getStatusPanel(grid.Z$).updateRowCount("calculating...");
              var request = thingworxService;
              var body = {};
              requestParams.forEach(function (nextParam, index) {
                var nextParamValue = params.request[nextParam];
                _this.log(logPrefix, index + ". nextParam  " + nextParam + " = " + nextParamValue + " (" + typeof nextParamValue + ")");
                _this.log(logPrefix, index + ". nextParam isArray? = " + Array.isArray(nextParamValue));
                if (nextParamValue !== "") {
                  body[nextParam] = JSON.stringify(nextParamValue);
                }
              });
              _this.currentServerSideFilterParams = body;
              if (_this.ServerSideAdditionalRequestParams) {
                _this.log(logPrefix, "adding additional params specified by ServerSideAdditionalRequestParams: " + _this.ServerSideAdditionalRequestParams);
                var additionalParams_1 = JSON.parse(_this.ServerSideAdditionalRequestParams);
                Object.keys(additionalParams_1).forEach(function (nextAdditionalParam) {
                  _this.log(logPrefix, "adding nextAdditionalParam: " + nextAdditionalParam);
                  body[nextAdditionalParam] = additionalParams_1[nextAdditionalParam];
                });
              }
              _this.log(logPrefix, "calling request = " + request + " with body = " + JSON.stringify(body));
              _this.widgetGridApi.showLoadingOverlay();
              fetch(request, {
                method: "POST",
                headers: HEADERS,
                body: JSON.stringify(body)
              }).then(function (response) {
                response.json().then(function (data) {
                  if (response.status !== 200) {
                    _this.log(logPrefix, "getRows request failed, error = " + JSON.stringify(response.json()));
                    params.fail();
                  } else {
                    _this.log(logPrefix, "getRows request succeeded!");
                    var lastRowIndex = _this.getLastRowIndex(params.request, data.rows);
                    params.success({
                      rowData: data.rows,
                      rowCount: lastRowIndex
                    });
                    _this.log(logPrefix, "returning " + data.rows.length + " rows, lastRowIndex = " + lastRowIndex);
                    _this.ServerSideRowModelGridUpdated();
                    _this.widgetGridApi.hideOverlay();
                    if (_this.UseAsyncSetFilters) {
                      _this.updateAsyncColumnFilters(thingworxFilterService);
                    }
                  }
                });
              });
            }
          };
        };
        _this.updateAsyncColumnFilters = function (thingworxFilterService) {
          var logPrefix = "getRows().updateAsyncColumnFilters(): ";
          var currentlyFilteredColumns = Object.keys(_this.widgetGridApi.getFilterModel());
          _this.log(logPrefix, "since UseAsyncSetFilters is true, updating filter set values, currentlyFilteredColumns = " + JSON.stringify(currentlyFilteredColumns));
          var filterRequest = thingworxFilterService;
          _this.log(logPrefix, "calling filterRequest: " + filterRequest);
          return fetch(filterRequest, {
            method: "POST",
            headers: HEADERS
          }).then(function (response) {
            response.json().then(function (data) {
              if (response.status !== 200) {
                _this.log(logPrefix, "filterRequest failed, error = " + JSON.stringify(response.json()));
              } else {
                _this.log(logPrefix, "filterRequest succeeded, response = " + JSON.stringify(data));
                if (Object.keys(data).length > 0 && data.hasOwnProperty("rows")) {
                  var updatedFilters_1 = JSON.parse(data.rows[0].result);
                  _this.log(logPrefix, "updatedFilters = " + JSON.stringify(updatedFilters_1));
                  _this.serverSideSetFilters = updatedFilters_1;
                  Object.keys(updatedFilters_1).forEach(function (nextColumn) {
                    if (currentlyFilteredColumns.includes(nextColumn)) {
                      _this.log(logPrefix, "skipping nextColumn because it is already filtered: " + nextColumn);
                    } else {
                      _this.log(logPrefix, "updating nextColumn filter " + nextColumn + " with values: " + updatedFilters_1[nextColumn]);
                      _this.widgetGridApi.getColumnFilterInstance(nextColumn).then(function (nextColumn) {
                        nextColumn.refreshFilterValues();
                      });
                    }
                  });
                }
              }
            });
          });
        };
        _this.getContextMenuItems = function (params) {
          return ["cut", "copy", "copyWithHeaders", "paste", "separator", {
            name: "Export",
            icon: "<span class=\"ag-icon ag-icon-save\" unselectable=\"on\" role=\"presentation\"></span>",
            subMenu: [{
              name: "Export To CSV",
              action: function () {
                return _this.exportServerSideGrid("csv");
              },
              icon: "<span class=\"ag-icon ag-icon-csv\" unselectable=\"on\" role=\"presentation\"></span>"
            }, {
              name: "Export To Excel",
              action: function () {
                return _this.exportServerSideGrid("excel");
              },
              icon: "<span class=\"ag-icon ag-icon-excel\" unselectable=\"on\" role=\"presentation\"></span>"
            }]
          }];
        };
        _this.dateCellCallBack = function (params) {
          _this.log("dateCellCallBack(): params.column.colDef = ", JSON.stringify(params.column.colDef));
          if (params.column.colDef.baseType == "Date") {
            var value = params.value;
            var excelFormatDate = "";
            _this.log("dateCellCallBack(): converting date, value = ", value);
            if (value) {
              excelFormatDate = _this.excelDateConverter(new Date(value));
              _this.log("dateCellCallBack(): formatted date = ", excelFormatDate);
            }
            return excelFormatDate;
          }
          return params.value;
        };
        _this.pad = function (num, size) {
          for (var s = num + ""; s.length < size;) {
            s = "0" + s;
          }
          return s;
        };
        _this.excelDateConverter = function (value) {
          var fullYear = value.getUTCFullYear();
          var month = _this.pad(value.getUTCMonth() + 1, 2);
          var date = _this.pad(value.getUTCDate(), 2);
          var hours = _this.pad(value.getUTCHours(), 2);
          var minutes = _this.pad(value.getUTCMinutes(), 2);
          var seconds = _this.pad(value.getUTCSeconds(), 2);
          var milliseconds = _this.pad(value.getUTCMilliseconds(), 3);
          var excelFormatDate = `${fullYear}-${month}-${date}T${hours}:${minutes}:${seconds}.${milliseconds}`;
          console.log(excelFormatDate);
          return excelFormatDate;
        };
        _this.exportServerSideGrid = function (format) {
          var logPrefix = "exportServerSideGrid(" + format + "): ";
          _this.log(logPrefix, "Starting...");
          var exportDiv = document.createElement("div");
          var request = _this.ServerSideDataSource;
          var currentFilterRequestBody = _this.currentServerSideFilterParams;
          currentFilterRequestBody.startRow = 0;
          currentFilterRequestBody.endRow = 0;
          currentFilterRequestBody.columnState = _this.widgetGridApi.getColumnState();
          if (_this.EnableConsoleLogging) {
            _this.log(logPrefix, "sending request = " + request + " with body: " + JSON.stringify(currentFilterRequestBody));
          }
          _this.widgetGridApi.showLoadingOverlay();
          fetch(request, {
            method: "POST",
            headers: HEADERS,
            body: JSON.stringify(currentFilterRequestBody)
          }).then(function (response) {
            if (response.status !== 200) {
              if (_this.EnableConsoleLogging) {
                _this.log(logPrefix, "error, response = " + JSON.stringify(response));
              }
              _this.widgetGridApi.hideOverlay();
              _this.ShowErrorMessage();
              return;
            }
            response.json().then(function (data) {
              if (_this.EnableConsoleLogging) {
                _this.log(logPrefix, "got response, building export grid");
              }
              var rows = [];
              data.rows.forEach(function (nextRow, index) {
                nextRow[grid.ME] = index;
                if (nextRow.hasOwnProperty("cellDataType")) {
                  delete nextRow.cellDataType;
                }
                rows.push(nextRow);
              });
              if (_this.EnableConsoleLogging) {
                _this.log(logPrefix, "export grid will have " + rows.length + " rows");
              }
              var exportGridConfig = {};
              var gridOptionsCopy = JSON.parse(JSON.stringify(_this.gridOptions));
              var gridOptionPropertiesToIgnore = ["statusBar", "toolPanelParams", "rowData", "rowClassRules", "cacheBlockSize", "serverSideDatasource"];
              if (_this.EnableConsoleLogging) {
                _this.log(logPrefix, "1. gridOptionsCopy = " + JSON.stringify(gridOptionsCopy));
              }
              Object.keys(gridOptionsCopy).filter(function (nextProp) {
                return !gridOptionPropertiesToIgnore.includes(nextProp);
              }).forEach(function (nextProp) {
                exportGridConfig[nextProp] = gridOptionsCopy[nextProp];
              });
              if (_this.EnableConsoleLogging) {
                _this.log(logPrefix, "2. exportGridConfig = " + JSON.stringify(exportGridConfig));
              }
              if (_this.EnableConsoleLogging) {
                _this.log(logPrefix, "2. rows = " + JSON.stringify(rows));
              }
              exportGridConfig.rowData = rows;
              exportGridConfig.rowModelType = "clientSide";
              delete exportGridConfig.cacheBlockSize;
              delete exportGridConfig.serverSideDatasource;
              if (_this.EnableConsoleLogging) {
                _this.log(logPrefix, "3. exportGridConfig = " + JSON.stringify(exportGridConfig));
              }
              var addedColumns = [];
              if (_this.ServerSideDynamicExportColumnsPrefix) {
                if (_this.EnableConsoleLogging) {
                  _this.log(logPrefix, "adding in dynamically-generated columns, ServerSideDynamicExportColumnsPrefix = " + _this.ServerSideDynamicExportColumnsPrefix);
                }
                var columnPrefixesToAdd_1 = JSON.parse(_this.ServerSideDynamicExportColumnsPrefix);
                var datashapeFields = Object.keys(data.dataShape.fieldDefinitions);
                if (_this.EnableConsoleLogging) {
                  _this.log(logPrefix, "datashapeFields = " + JSON.stringify(datashapeFields));
                }
                datashapeFields.forEach(function (nextField) {
                  if (_this.EnableConsoleLogging) {
                    _this.log(logPrefix, "checking if we should add nextField = " + nextField);
                  }
                  if (columnPrefixesToAdd_1.some(function (nextPrefix) {
                    return nextField.startsWith(nextPrefix);
                  })) {
                    if (_this.EnableConsoleLogging) {
                      _this.log(logPrefix, "yes! we should add nextField = " + nextField);
                    }
                    exportGridConfig.columnDefs.push({
                      field: nextField,
                      hide: false
                    });
                    addedColumns.push(nextField);
                  }
                });
              }
              if (_this.EnableConsoleLogging) {
                _this.log(logPrefix, "creating export grid, exportGridConfig = " + JSON.stringify(exportGridConfig));
              }
              try {
                var exportedGrid = createGrid(exportDiv, exportGridConfig);
                if (_this.EnableConsoleLogging) {
                  _this.log(logPrefix, "created export grid which has " + exportedGrid.getDisplayedRowCount() + " rows");
                }
                if (_this.SavedState) {
                  if (_this.EnableConsoleLogging) {
                    _this.log(logPrefix, "restoring saved state: " + _this.SavedState);
                  }
                  try {
                    var savedStateAsJson_1 = JSON.parse(_this.SavedState);
                    var columnStates_1 = savedStateAsJson_1.ColumnState;
                    if (savedStateAsJson_1 && savedStateAsJson_1.hasOwnProperty("ColumnState")) {
                      if (addedColumns.length) {
                        addedColumns.forEach(function (nextAddedColumn) {
                          columnStates_1.push({
                            colId: nextAddedColumn,
                            hide: false
                          });
                        });
                      }
                      exportedGrid.applyColumnState({
                        state: columnStates_1,
                        applyOrder: true
                      });
                      if (savedStateAsJson_1.hasOwnProperty("FilterState")) {
                        if (_this.EnableConsoleLogging) {
                          _this.log(logPrefix, "restoring filters...");
                        }
                        var savedFilters_1 = savedStateAsJson_1.FilterState;
                        Object.keys(savedFilters_1).forEach(function (nextFilter) {
                          if (savedFilters_1[nextFilter].filterType == "date" && savedFilters_1[nextFilter].type == "blank") {
                            if (_this.EnableConsoleLogging) {
                              _this.log(logPrefix, "next filters " + nextFilter + " is date=blank so replacing it with epoch 0");
                            }
                            savedStateAsJson_1.FilterState[nextFilter] = {
                              dateFrom: "0",
                              dateTo: null,
                              filterType: "date",
                              type: "equals"
                            };
                          }
                        });
                        exportedGrid.setFilterModel(savedStateAsJson_1.FilterState);
                      }
                      if (_this.EnableConsoleLogging) {
                        _this.log(logPrefix, "restoring saved state: " + _this.SavedState);
                      }
                      if (_this.EnableConsoleLogging) {
                        _this.log(logPrefix, "after setFilterModel grid has " + exportedGrid.getDisplayedRowCount() + " rows");
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
                  _this.log(logPrefix, "exporting grid which has " + exportedGrid.getDisplayedRowCount() + " rows");
                }
                if (format == "excel") {
                  exportedGrid.exportDataAsExcel({
                    processCellCallback: _this.dateCellCallBack
                  });
                }
                if (format == "csv") {
                  exportedGrid.exportDataAsCsv({
                    processCellCallback: _this.dateCellCallBack
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
          }).catch(function (err) {
            this.log(logPrefix, "err: " + err);
          });
        };
        _this.getLastRowIndex = function (request, results) {
          if (results) {
            var currentLastRow = (request.startRow || 0) + results.length;
            if (currentLastRow < (request.endRow || 0)) {
              return currentLastRow;
            } else {
              return undefined;
            }
          }
        };
        _this.emitDoubleClickedEvent = function (params) {
          _this.log("emitDoubleClickedEvent(): ", "Row double-clicked, emitting rowdoubleclicked event...");
          _this.RowDoubleClicked();
        };
        _this.onSelectionChanged = function () {
          var logPrefix = "onSelectionChanged(): ";
          var selectedRows = _this.widgetGridApi.getSelectedNodes();
          _this.log(logPrefix, "selectedRows has " + selectedRows.length + " rows");
          _this.indices = selectedRows.map(function (nextRow) {
            return parseFloat(nextRow.id);
          });
          _this.log(logPrefix, "indices = " + JSON.stringify(_this.indices));
          _this.NoRowSelected = _this.indices.length == 0;
          _this.AnyRowSelected = _this.indices.length > 0;
          _this.log(logPrefix, "calling updateSelection");
          _this.updateSelection("Data", _this.indices);
          _this.SelectedRowsChanged();
          _this.log(logPrefix, "done calling updateSelection");
          var renderedGrid = _this.widgetGridApi.getRenderedNodes();
          var firstRenderedRowId = parseInt(renderedGrid[0].id);
          var lastRenderedRowId = parseInt(renderedGrid[renderedGrid.length - 1].id);
          _this.log(logPrefix, "firstRenderedRowId = " + firstRenderedRowId);
          _this.log(logPrefix, "lastRenderedRowId = " + lastRenderedRowId);
          _this.FirstRowSelected = false;
          _this.LastRowSelected = false;
          if (_this.indices.includes(firstRenderedRowId)) {
            _this.log(logPrefix, "firstRenderedRowId is in selected row ids, setting FirstRowSelected to true");
            _this.FirstRowSelected = true;
          }
          if (_this.indices.includes(lastRenderedRowId)) {
            _this.log(logPrefix, "lastRenderedRowId is in selected row ids, setting LastRowSelected to true");
            _this.LastRowSelected = true;
          }
        };
        _this.onFirstDataRendered = function (params) {
          if (_this.ShowGridAsTree && _this.gridOptions.hasOwnProperty("groupDefaultExpanded")) {
            var levelsToExpand_1 = _this.gridOptions.groupDefaultExpanded;
            _this.log("onFirstDataRendered(): ", "setting nodes expanded/collapsed, levelsToExpand = " + levelsToExpand_1);
            _this.widgetGridApi.forEachNode(function (node, index) {
              var nodeLevel = node.level;
              var expandThisNode = levelsToExpand_1 == -1 || nodeLevel < levelsToExpand_1;
              node.setExpanded(expandThisNode);
            });
          }
        };
        _this.debounce = function (callback, wait) {
          var timeoutId = null;
          return function () {
            var args = [];
            for (var _i = 0; _i < arguments.length; _i++) {
              args[_i] = arguments[_i];
            }
            window.clearTimeout(timeoutId);
            timeoutId = window.setTimeout(function () {
              callback.apply(undefined, args);
            }, wait);
          };
        };
        _this.cellValueChanged = _this.debounce(function (event) {
          var logPrefix = "cellValueChanged(): ";
          _this.log(logPrefix, "cell was changed");
          _this.log(logPrefix, "old value = " + event.oldValue);
          _this.log(logPrefix, "new value = " + event.newValue);
          _this.log(logPrefix, "source = " + event.source);
          event.node.data.changed = true;
          var updatedRows = [];
          _this.widgetGridApi.forEachNode(function (node) {
            if (node.data && node.data.changed) {
              _this.log(logPrefix, "updating edited table, next node = " + JSON.stringify(node.data));
              _this.log(logPrefix, "since this row was changed, adding it to edited table");
              updatedRows.push(node.data);
            }
          });
          var rowIdField = _this.gridOptions.hasOwnProperty("rowModelType") && _this.gridOptions.rowModelType == "serverSide" ? _this.RowIdField : grid.ME;
          _this.log(logPrefix, "based on whether or not this is a client-side or server-side grid, rowIdField = " + rowIdField);
          _this.EditedTable.rows = _this.EditedTable.rows.map(function (nextRow) {
            return updatedRows.find(function (nextUpdatedNode) {
              return nextRow[rowIdField] === nextUpdatedNode[rowIdField];
            }) || nextRow;
          });
          var existingRowIds = _this.EditedTable.rows.map(function (nextExistingRow) {
            return nextExistingRow[rowIdField];
          });
          var updatedRowIds = updatedRows.map(function (nextUpdatedRow) {
            return nextUpdatedRow[rowIdField];
          });
          var idsThatDoNotExistYet = updatedRowIds.filter(function (x) {
            return !existingRowIds.includes(x);
          });
          _this.log(logPrefix, "existingRowIds = " + JSON.stringify(existingRowIds));
          _this.log(logPrefix, "updatedRowIds = " + JSON.stringify(updatedRowIds));
          _this.log(logPrefix, "idsThatDoNotExistYet = " + JSON.stringify(idsThatDoNotExistYet));
          updatedRows.forEach(function (nextUpdatedRow) {
            _this.log(logPrefix, "nextUpdatedRow[rowIdField] = " + nextUpdatedRow[rowIdField]);
            if (idsThatDoNotExistYet.includes(nextUpdatedRow[rowIdField])) {
              _this.EditedTable.rows.push(nextUpdatedRow);
            }
          });
          _this.log(logPrefix, "this.EditedTable updated...");
          _this.CellEdited();
          _this.updateUpdatedTable();
          _this.updateRenderedTable();
        }, 1000);
        _this.updateSelectedRows = function (event) {
          var logPrefix = "updateSelectedRows(): ";
          var selectedRows = _this.widgetGridApi.getSelectedRows();
          _this.SelectedRows = {
            dataShape: _this.dataSourceDataShape,
            rows: selectedRows
          };
          var selectedRowNumbers = [];
          _this.widgetGridApi.getSelectedNodes().forEach(function (row) {
            var nextRowIndex = row.rowIndex;
            _this.log(logPrefix, "next selected row index = " + nextRowIndex);
            selectedRowNumbers.push(nextRowIndex + 1);
          });
          _this.SelectedRowNumbers = selectedRowNumbers.join();
          _this.log(logPrefix, "selectedRowNumbers = " + selectedRowNumbers);
          _this.log(logPrefix, "Selection updated, SelectedRowNumbers = " + _this.SelectedRowNumbers);
        };
        _this.updateUpdatedTable = function () {
          var rows = [];
          _this.widgetGridApi.forEachNode(function (node) {
            rows.push(node.data);
          });
          _this.UpdatedTable = {
            dataShape: _this.dataSourceDataShape,
            rows
          };
          _this.updateRenderedTable();
          _this.updateUpdatedTableOnDataChange();
        };
        _this.updateUpdatedTableOnDataChange = function () {
          var logPrefix = "updateUpdatedTableOnDataChange(): ";
          var rows = [];
          _this.widgetGridApi.forEachNode(function (node) {
            _this.log(logPrefix, "updating UpdatedTable with next row: " + JSON.stringify(node.data));
            rows.push(node.data);
          });
          _this.log(logPrefix, "updating UpdatedTable with " + rows.length + " rows");
          _this.UpdatedTableOnDataChange = {
            dataShape: _this.dataSourceDataShape,
            rows
          };
          _this.updateRenderedTable();
        };
        _this.updateFilteredTable = function () {
          var logPrefix = "updateFilteredTable(): ";
          _this.log(logPrefix, "filter was changed and applied...");
          var rows = [];
          if (_this.gridOptions.hasOwnProperty("rowModelType") && _this.gridOptions.rowModelType !== "serverSide") {
            _this.widgetGridApi.forEachNodeAfterFilterAndSort(function (node) {
              rows.push(node.data);
            });
            _this.log(logPrefix, "updating FilteredTable with " + rows.length + " rows");
            _this.FilteredTable = {
              dataShape: _this.dataSourceDataShape,
              rows
            };
          } else {
            _this.log(logPrefix, "not updating FilteredTable since grid is server-side");
          }
          _this.updateRenderedTable();
          _this.DataFiltered();
        };
        _this.updateRenderedTable = function () {
          var logPrefix = "updateRenderedTable(): ";
          _this.log(logPrefix, "columns or data changed, updating rendered table.");
          if (_this.dataSourceDataShape) {
            var rows_1 = [];
            var renderedDataShape_1 = JSON.parse(JSON.stringify(_this.dataSourceDataShape));
            var visibleColumns_1 = _this.widgetGridApi.getColumnState().filter(function (nextColumnState) {
              return !nextColumnState.hide;
            }).map(function (nextColumnState) {
              return nextColumnState.colId;
            });
            Object.keys(renderedDataShape_1.fieldDefinitions).forEach(function (nextField) {
              if (!visibleColumns_1.includes(nextField)) {
                delete renderedDataShape_1.fieldDefinitions[nextField];
              }
            });
            if (_this.ShowGridAsTree) {
              _this.log(logPrefix, "since grid is a tree, updating RenderedTable with source datashape");
              renderedDataShape_1 = _this.dataSourceDataShape;
            }
            _this.widgetGridApi.forEachNode(function (nextNode) {
              rows_1.push(nextNode.data);
            });
            _this.log(logPrefix, "updating RenderedTable with " + rows_1.length + " rows");
            _this.RenderedTable = {
              dataShape: renderedDataShape_1,
              rows: rows_1
            };
          }
        };
        _this.applyCustomThemeStyle = function () {
          return runtime_awaiter(_this, undefined, undefined, function () {
            var logPrefix;
            var themeToCssVariableMap;
            var _this = this;
            return runtime_generator(this, function (_a) {
              switch (_a.label) {
                case 0:
                  logPrefix = "applyCustomThemeStyle(): ";
                  this.log(logPrefix, "Starting, getting custom theme");
                  themeToCssVariableMap = {
                    "global-text-body-fontsize": "--grid-font-size",
                    "e-grid-text-fontsize": "--grid-font-size",
                    "global-text-title-fontsize": "--header-font-size",
                    "e-grid-header-label-fontsize": "--header-font-size"
                  };
                  return [4, fetch("", {
                    method: "POST",
                    headers: HEADERS
                  }).then(function (response) {
                    return response.json();
                  }).then(function (customThemeContent) {
                    var props = customThemeContent.props;
                    var propMap = {};
                    if (props) {
                      props.forEach(function (nextProp) {
                        propMap[nextProp.name] = nextProp.value;
                      });
                    }
                    var newCssValues = {};
                    Object.keys(themeToCssVariableMap).forEach(function (nextThemeProp) {
                      if (propMap.hasOwnProperty(nextThemeProp)) {
                        newCssValues[themeToCssVariableMap[nextThemeProp]] = propMap[nextThemeProp];
                      }
                    });
                    if (_this.GridTextSize) {
                      _this.log(logPrefix, "overriding text size with grid property GridTextSize: " + _this.GridTextSize);
                      newCssValues["--grid-font-size"] = _this.GridTextSize + "px";
                    }
                    if (_this.GridHeaderSize !== undefined) {
                      _this.log(logPrefix, "overriding header size with grid property GridHeaderSize: " + _this.GridHeaderSize);
                      newCssValues["--header-font-size"] = _this.GridHeaderSize + "px";
                    }
                    if (_this.HideBorders) {
                      _this.log(logPrefix, "HideBorders is true, removing borders: ");
                      newCssValues["--ag-borders"] = "none";
                    }
                    Object.keys(newCssValues).forEach(function (nextCssVariable) {
                      _this.log(logPrefix, "setting nextCssVariable " + nextCssVariable + " to " + newCssValues[nextCssVariable]);
                      document.getElementById(_this.jqElement[0].id).style.setProperty(nextCssVariable, newCssValues[nextCssVariable]);
                    });
                  })];
                case 1:
                  _a.sent();
                  return [2];
              }
            });
          });
        };
        _this.cellEditorSelector = function (params) {
          var logPrefix = "cellEditorSelector(): ";
          _this.log(logPrefix, "Starting...");
          var returnValue = {
            component: "agTextCellEditor"
          };
          var currentAttribute = params.column.getColId();
          _this.log(logPrefix, `checking cell editor selector for attribute: ${currentAttribute}`);
          var cellEditorSelectorConfig = _this.gridConfig.CellEditorSelectorConfig;
          _this.log(logPrefix, "cellEditorSelectorConfig = " + JSON.stringify(cellEditorSelectorConfig));
          if (_this.gridConfig.hasOwnProperty("ColumnConfig")) {
            var columnConfig = _this.gridConfig.ColumnConfig;
            _this.log(logPrefix, "columnConfig = " + JSON.stringify(columnConfig));
            _this.log(logPrefix, "does it have that attribute? = " + columnConfig.hasOwnProperty(currentAttribute));
            if (columnConfig.hasOwnProperty(currentAttribute)) {
              var matchingFieldOptions = columnConfig[currentAttribute];
              _this.log(logPrefix, "matchingFieldOptions = " + JSON.stringify(matchingFieldOptions));
              if (matchingFieldOptions.hasOwnProperty("cellEditorParams")) {
                var matchingFieldCellEditorParams = matchingFieldOptions.cellEditorParams;
                if (matchingFieldCellEditorParams.hasOwnProperty("fieldThatDeterminesCellEditor")) {
                  var fieldThatDeterminesCellEditor = matchingFieldCellEditorParams.fieldThatDeterminesCellEditor;
                  _this.log(logPrefix, "fieldThatDeterminesCellEditor = " + fieldThatDeterminesCellEditor);
                  var fieldValue = params.data[fieldThatDeterminesCellEditor];
                  if (fieldValue && (_this.log(logPrefix, "that field has a value of = " + fieldValue), cellEditorSelectorConfig.hasOwnProperty(fieldValue))) {
                    var selectedCellConfig = cellEditorSelectorConfig[fieldValue];
                    _this.log(logPrefix, "CellEditorSelectorConfig has that value, the config = " + JSON.stringify(selectedCellConfig));
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
          _this.log(logPrefix, "returning returnValue = " + JSON.stringify(returnValue));
          return returnValue;
        };
        _this.cellRendererSelector = function (params) {
          var logPrefix = "cellRendererSelector(): ";
          if (params.data) {
            _this.log(logPrefix, "Starting...");
            var returnValue = {
              component: undefined
            };
            var currentAttribute = params.column.getColId();
            _this.log(logPrefix, `checking cell editor selector for attribute: ${currentAttribute}`);
            var cellRendererSelectorConfig = _this.gridConfig.CellRendererSelectorConfig;
            _this.log(logPrefix, "cellRendererSelectorConfig = " + JSON.stringify(cellRendererSelectorConfig));
            if (_this.gridConfig.hasOwnProperty("ColumnConfig")) {
              var columnConfig = _this.gridConfig.ColumnConfig;
              _this.log(logPrefix, "columnConfig = " + JSON.stringify(columnConfig));
              _this.log(logPrefix, "does it have that attribute? = " + columnConfig.hasOwnProperty(currentAttribute));
              if (columnConfig.hasOwnProperty(currentAttribute)) {
                var matchingFieldOptions = columnConfig[currentAttribute];
                _this.log(logPrefix, "matchingFieldOptions = " + JSON.stringify(matchingFieldOptions));
                if (matchingFieldOptions.hasOwnProperty("cellRendererParams")) {
                  var matchingFieldCellRendererParams = matchingFieldOptions.cellRendererParams;
                  if (matchingFieldCellRendererParams.hasOwnProperty("fieldThatDeterminesCellRenderer")) {
                    var fieldThatDeterminesCellRenderer = matchingFieldCellRendererParams.fieldThatDeterminesCellRenderer;
                    _this.log(logPrefix, "fieldThatDeterminesCellRenderer = " + fieldThatDeterminesCellRenderer);
                    var fieldValue = params.data[fieldThatDeterminesCellRenderer];
                    if (fieldValue && (_this.log(logPrefix, "that field has a value of = " + fieldValue), cellRendererSelectorConfig.hasOwnProperty(fieldValue))) {
                      var selectedCellConfig = cellRendererSelectorConfig[fieldValue];
                      _this.log(logPrefix, "cellRendererSelectorConfig has that value, the config = " + JSON.stringify(selectedCellConfig));
                      returnValue.component = selectedCellConfig.cellRenderer;
                      if (selectedCellConfig.hasOwnProperty("cellRendererParams")) {
                        returnValue.params = selectedCellConfig.cellRendererParams;
                      }
                    }
                  }
                }
              }
            }
            _this.log(logPrefix, "returning returnValue = " + JSON.stringify(returnValue));
            return returnValue;
          }
        };
        _this.updateCustomServerSideStatusPanelRowCount = function () {
          _this.widgetGridApi.getStatusBarComponent(grid.Z$).updateRowCount(_this.CustomServerSideStatusPanelRowCount);
        };
        _this.reindexOnPaste = function () {
          if (_this.PasteIndexColumn) {
            _this.log("reindexOnPaste(): ", "Starting, reindexing column: " + _this.PasteIndexColumn);
            _this.widgetGridApi.forEachNode(function (node, index) {
              var nextRowData = node.data;
              _this.log("reindexOnPaste(): ", "reindexing row: " + JSON.stringify(nextRowData));
              nextRowData[_this.PasteIndexColumn] = index + 1;
              node.setData(nextRowData);
            });
          }
        };
        return _this;
      }
      __extends(AgGridWidget2, _super);
      Object.defineProperty(AgGridWidget2.prototype, "Data", {
        set: function (Data) {
          if (Data) {
            this.log("setData(): ", "setting Data, this.widgetGridApi is undefined? = " + (this.widgetGridApi === undefined) + ", this.alreadyLoaded = " + this.alreadyLoaded);
            this.buildGrid();
          }
        },
        enumerable: false,
        configurable: true
      });
      Object.defineProperty(AgGridWidget2.prototype, "CustomServerSideStatusPanelRowCount", {
        set: function (rowCount) {
          if (rowCount) {
            this.log("setCustomServerSideStatusPanelRowCount(): ", "setting CustomServerSideStatusPanelRowCount, rowCount = " + rowCount);
            this.widgetGridApi.getStatusPanel(grid.Z$).updateRowCount(rowCount);
            this.updateCustomServerSideStatusPanelRowCount();
          }
        },
        enumerable: false,
        configurable: true
      });
      AgGridWidget2.prototype.updateProperty = function (info) {
        _super.prototype.updateProperty.call(this, info);
        if (info.TargetProperty === "RowSelection") {
          this.widgetGridApi.deselectAll();
          this.gridOptions.rowSelection = info.SinglePropertyValue;
        }
        if (info.TargetProperty === "Data") {
          console.log("Data changed, building grid...");
          this.buildGrid();
        }
      };
      AgGridWidget2.prototype.handleSelectionUpdate = function (property, selectedRows, selectedRowIndices) {
        this.log("handleSelectionUpdate(): ", "selectedRowIndices = " + JSON.stringify(selectedRowIndices));
        if (selectedRowIndices.length == 0) {
          this.widgetGridApi.deselectAll();
        } else {
          this.widgetGridApi.getRowNode(selectedRowIndices[0].toString()).setSelected(true, true);
        }
      };
      AgGridWidget2.prototype.valueWillBind = function (value, info) {
        var logPrefix = "valueWillBind(" + info.TargetProperty + "): ";
        if (this.EnableConsoleLogging) {
          this.log(logPrefix, "property updated!");
        }
        return true;
      };
      AgGridWidget2.prototype.afterRender = function () {
        return runtime_awaiter(this, undefined, undefined, function () {
          return runtime_generator(this, function (_a) {
            this.buildGrid();
            return [2];
          });
        });
      };
      AgGridWidget2.prototype.beforeDestroy = function () {
        var container = document.getElementById(this.jqElement[0].id);
        if (container) {
          var currentGrid = container.firstChild;
          if (currentGrid) {
            currentGrid.remove();
          }
          container.remove();
        }
      };
      __decorate([property], AgGridWidget2.prototype, "Data", null);
      __decorate([property], AgGridWidget2.prototype, "AllowEditing", undefined);
      __decorate([property], AgGridWidget2.prototype, "AnyRowSelected", undefined);
      __decorate([property], AgGridWidget2.prototype, "AutoOpenSideBar", undefined);
      __decorate([property], AgGridWidget2.prototype, "CheckBoxSelection", undefined);
      __decorate([property], AgGridWidget2.prototype, "ChildField", undefined);
      __decorate([property], AgGridWidget2.prototype, "ClickedActionCellButton", undefined);
      __decorate([property], AgGridWidget2.prototype, "DateFormat", undefined);
      __decorate([property], AgGridWidget2.prototype, "DateEditorsMinimumDate", undefined);
      __decorate([property], AgGridWidget2.prototype, "DateEditorsMaximumDate", undefined);
      __decorate([property], AgGridWidget2.prototype, "DefaultSelectedRows", undefined);
      __decorate([property], AgGridWidget2.prototype, "DeletedTable", undefined);
      __decorate([property], AgGridWidget2.prototype, "DateEditorShowWeekNumbers", undefined);
      __decorate([property], AgGridWidget2.prototype, "EditTable", undefined);
      __decorate([property], AgGridWidget2.prototype, "EditedTable", undefined);
      __decorate([property], AgGridWidget2.prototype, "TreeTable", undefined);
      __decorate([property], AgGridWidget2.prototype, "EnableRowGrouping", undefined);
      __decorate([property], AgGridWidget2.prototype, "EmitDoubleClickEvent", undefined);
      __decorate([property], AgGridWidget2.prototype, "EnableCellTextSelection", undefined);
      __decorate([property], AgGridWidget2.prototype, "EnableRangeSelection", undefined);
      __decorate([property], AgGridWidget2.prototype, "FilterType", undefined);
      __decorate([property], AgGridWidget2.prototype, "FilteredTable", undefined);
      __decorate([property], AgGridWidget2.prototype, "GridConfig", undefined);
      __decorate([property], AgGridWidget2.prototype, "GroupingColumns", undefined);
      __decorate([property], AgGridWidget2.prototype, "NoRowSelected", undefined);
      __decorate([property], AgGridWidget2.prototype, "ParentField", undefined);
      __decorate([property], AgGridWidget2.prototype, "QuickFilterText", undefined);
      __decorate([property], AgGridWidget2.prototype, "ReorderColumns", undefined);
      __decorate([property], AgGridWidget2.prototype, "ResizeColumns", undefined);
      __decorate([property], AgGridWidget2.prototype, "ResizeGridWithParent", undefined);
      __decorate([property], AgGridWidget2.prototype, "RowHeight", undefined);
      __decorate([property], AgGridWidget2.prototype, "RowSelection", undefined);
      __decorate([property], AgGridWidget2.prototype, "RowsToSelect", undefined);
      __decorate([property], AgGridWidget2.prototype, "ShowFloatingFilters", undefined);
      __decorate([property], AgGridWidget2.prototype, "ShowGridAsTree", undefined);
      __decorate([property], AgGridWidget2.prototype, "SideBarPanels", undefined);
      __decorate([property], AgGridWidget2.prototype, "SingleClickEdit", undefined);
      __decorate([property], AgGridWidget2.prototype, "Sorting", undefined);
      __decorate([property], AgGridWidget2.prototype, "StatusMessage", undefined);
      __decorate([property], AgGridWidget2.prototype, "TargetSideBarPanel", undefined);
      __decorate([property], AgGridWidget2.prototype, "TransposeFirstRowToForm", undefined);
      __decorate([property], AgGridWidget2.prototype, "TreeColumnField", undefined);
      __decorate([property], AgGridWidget2.prototype, "UpdatedTable", undefined);
      __decorate([property], AgGridWidget2.prototype, "SelectedRows", undefined);
      __decorate([property], AgGridWidget2.prototype, "SelectedRowNumbers", undefined);
      __decorate([property], AgGridWidget2.prototype, "UpdatedTableOnDataChange", undefined);
      __decorate([property], AgGridWidget2.prototype, "SavedState", undefined);
      __decorate([property], AgGridWidget2.prototype, "NewRowData", undefined);
      __decorate([property], AgGridWidget2.prototype, "NewRowIndex", undefined);
      __decorate([property], AgGridWidget2.prototype, "GridTextSize", undefined);
      __decorate([property], AgGridWidget2.prototype, "GridHeaderSize", undefined);
      __decorate([property], AgGridWidget2.prototype, "CustomMashupStatusPanelMashupName", undefined);
      __decorate([property], AgGridWidget2.prototype, "FieldNameToUseForRowSelection", undefined);
      __decorate([property], AgGridWidget2.prototype, "FieldValuesToUseForRowSelection", undefined);
      __decorate([property], AgGridWidget2.prototype, "GridTheme", undefined);
      __decorate([property], AgGridWidget2.prototype, "ScrollIndex", undefined);
      __decorate([property], AgGridWidget2.prototype, "ScrollPosition", undefined);
      __decorate([property], AgGridWidget2.prototype, "EnableConsoleLogging", undefined);
      __decorate([property], AgGridWidget2.prototype, "FirstRowSelected", undefined);
      __decorate([property], AgGridWidget2.prototype, "LastRowSelected", undefined);
      __decorate([property], AgGridWidget2.prototype, "RenderedTable", undefined);
      __decorate([property], AgGridWidget2.prototype, "TreePathField", undefined);
      __decorate([property], AgGridWidget2.prototype, "TreePathFieldDelimeter", undefined);
      __decorate([property], AgGridWidget2.prototype, "HideBorders", undefined);
      __decorate([property], AgGridWidget2.prototype, "ServerSideDataSource", undefined);
      __decorate([property], AgGridWidget2.prototype, "ServerSideAdditionalRequestParams", undefined);
      __decorate([property], AgGridWidget2.prototype, "ServerSideDynamicExportColumnsPrefix", undefined);
      __decorate([property], AgGridWidget2.prototype, "RowIdField", undefined);
      __decorate([property], AgGridWidget2.prototype, "ShowServerSideRowTotalsStatusPanel", undefined);
      __decorate([property], AgGridWidget2.prototype, "ReindexOnPaste", undefined);
      __decorate([property], AgGridWidget2.prototype, "PasteIndexColumn", undefined);
      __decorate([property], AgGridWidget2.prototype, "UseAsyncSetFilters", undefined);
      __decorate([property], AgGridWidget2.prototype, "ServerSideFiltersDataSource", undefined);
      __decorate([property], AgGridWidget2.prototype, "CustomServerSideStatusPanelRowCount", null);
      __decorate([widgetRuntimeSupport_event], AgGridWidget2.prototype, "CellEdited", undefined);
      __decorate([widgetRuntimeSupport_event], AgGridWidget2.prototype, "StateSaved", undefined);
      __decorate([widgetRuntimeSupport_event], AgGridWidget2.prototype, "GridLoaded", undefined);
      __decorate([widgetRuntimeSupport_event], AgGridWidget2.prototype, "RowDoubleClicked", undefined);
      __decorate([widgetRuntimeSupport_event], AgGridWidget2.prototype, "DataFiltered", undefined);
      __decorate([widgetRuntimeSupport_event], AgGridWidget2.prototype, "ActionCellButtonClicked", undefined);
      __decorate([widgetRuntimeSupport_event], AgGridWidget2.prototype, "ShowInfoMessage", undefined);
      __decorate([widgetRuntimeSupport_event], AgGridWidget2.prototype, "ShowWarningMessage", undefined);
      __decorate([widgetRuntimeSupport_event], AgGridWidget2.prototype, "ShowErrorMessage", undefined);
      __decorate([widgetRuntimeSupport_event], AgGridWidget2.prototype, "ServerSideRowModelGridUpdated", undefined);
      __decorate([widgetRuntimeSupport_event], AgGridWidget2.prototype, "SelectedRowsChanged", undefined);
      __decorate([service], AgGridWidget2.prototype, "AddNewRow", undefined);
      __decorate([service], AgGridWidget2.prototype, "AddNewRowToBottom", undefined);
      __decorate([service], AgGridWidget2.prototype, "DeleteSelectedRows", undefined);
      __decorate([service], AgGridWidget2.prototype, "ResizeAll", undefined);
      __decorate([service], AgGridWidget2.prototype, "SaveState", undefined);
      __decorate([service], AgGridWidget2.prototype, "RestoreState", undefined);
      __decorate([service], AgGridWidget2.prototype, "SizeAllColumns", undefined);
      __decorate([service], AgGridWidget2.prototype, "ClearAllFilters", undefined);
      __decorate([service], AgGridWidget2.prototype, "QuickFilterSearch", undefined);
      __decorate([service], AgGridWidget2.prototype, "OpenSidePanel", undefined);
      __decorate([service], AgGridWidget2.prototype, "CloseSidePanel", undefined);
      __decorate([service], AgGridWidget2.prototype, "RefreshGrid", undefined);
      __decorate([service], AgGridWidget2.prototype, "ExpandAllRowGroups", undefined);
      __decorate([service], AgGridWidget2.prototype, "CollapseAllRowGroups", undefined);
      __decorate([service], AgGridWidget2.prototype, "ShowSideBar", undefined);
      __decorate([service], AgGridWidget2.prototype, "HideSideBar", undefined);
      __decorate([service], AgGridWidget2.prototype, "AddNewRowWithData", undefined);
      __decorate([service], AgGridWidget2.prototype, "ClearDeletedTable", undefined);
      __decorate([service], AgGridWidget2.prototype, "RestoreDeletedTable", undefined);
      __decorate([service], AgGridWidget2.prototype, "SelectSpecifiedRows", undefined);
      __decorate([service], AgGridWidget2.prototype, "SelectRowsByFieldValues", undefined);
      __decorate([service], AgGridWidget2.prototype, "SelectRowsByArrayValues", undefined);
      __decorate([service], AgGridWidget2.prototype, "ScrollToSpecifiedRow", undefined);
      __decorate([service], AgGridWidget2.prototype, "SelectNextRow", undefined);
      __decorate([service], AgGridWidget2.prototype, "SelectPreviousRow", undefined);
      AgGridWidget2 = __decorate([TWWidgetDefinition], AgGridWidget2);
    })(TWRuntimeWidget);
