var _CustomDateRenderer__WEBPACK_IMPORTED_MODULE_0__ = require("./3190.js");
var _CustomImageRenderer__WEBPACK_IMPORTED_MODULE_1__ = require("./2531.js");
var _DatePicker__WEBPACK_IMPORTED_MODULE_2__ = require("./4528.js");
var _CustomColumnAggregateFooterRenderer__WEBPACK_IMPORTED_MODULE_3__ = require("./9880.js");
var _CustomJsonRenderer__WEBPACK_IMPORTED_MODULE_4__ = require("./8988.js");
export var ME = "agGridId";
export var Z$ = "ServerSideStatusPanelRowCount";
var rows = [];
var childToParentIdTreeMap = {};
var enableLogging = false;
function log(logPrefix, logMessage, widgetId, loggingEnabled) {
  if (loggingEnabled) {
    console.log(`AgGridWidget-${widgetId}.Grid()${logPrefix}${logMessage}`);
  }
}
function aggregateRows(params, aggregatorParams, widgetId) {
  var fieldName = params.colDef.field;
  var logPrefix = ".aggregateRows(" + fieldName + "/row # " + params.node.rowIndex + "): ";
  var enableAggregationField = aggregatorParams.enableAggregationField;
  var alwaysAggregate = aggregatorParams.alwaysAggregate;
  var formula = aggregatorParams.formula;
  var columnsInFormula = aggregatorParams.columnsInFormula;
  var requiredColumns = aggregatorParams.requiredColumns;
  var decimalPlaces = aggregatorParams.decimalPlaces;
  var replaceEmptyFields = aggregatorParams.replaceEmptyFields;
  var returnValue;
  log(logPrefix, "alwaysAggregate = " + alwaysAggregate, widgetId, params.context.EnableConsoleLogging);
  log(logPrefix, "requiredColumns = " + requiredColumns, widgetId, params.context.EnableConsoleLogging);
  if (!alwaysAggregate) {
    log(logPrefix, "enable field = " + enableAggregationField + ", its value is: " + params.data[enableAggregationField], widgetId, params.context.EnableConsoleLogging);
  }
  if (!params.node || !params.node.group) {
    var calculatedResult = undefined;
    var allRequiredColumnsDefined_1 = true;
    var returnObject = {
      toString: function () {
        if (params.data[fieldName]) {
          return params.data[fieldName];
        } else {
          return "";
        }
      }
    };
    requiredColumns.forEach(function (nextRequiredColumn) {
      log(logPrefix, "nextRequiredColumn = " + nextRequiredColumn + ", its value is: " + params.data[nextRequiredColumn], widgetId, params.context.EnableConsoleLogging);
      if (isNaN(params.data[nextRequiredColumn])) {
        allRequiredColumnsDefined_1 = false;
      }
    });
    log(logPrefix, "allRequiredColumnsDefined = " + allRequiredColumnsDefined_1, widgetId, params.context.EnableConsoleLogging);
    if (allRequiredColumnsDefined_1 && (alwaysAggregate || params.data[enableAggregationField])) {
      log(logPrefix, "enableAggregationField is enabled, calculating row, formula = " + formula, widgetId, params.context.EnableConsoleLogging);
      log(logPrefix, "params.data = " + JSON.stringify(params.data), widgetId, params.context.EnableConsoleLogging);
      columnsInFormula.forEach(function (nextColumnToReplace) {
        var nextFieldValue = params.data[nextColumnToReplace];
        log(logPrefix, "nextColumnToReplace = " + nextColumnToReplace + ", its value = " + nextFieldValue, widgetId, params.context.EnableConsoleLogging);
        log(logPrefix, "typeof nextFieldValue = " + typeof nextFieldValue + ", it is gt or eq to 0? = " + (parseFloat(nextFieldValue) >= 0), widgetId, params.context.EnableConsoleLogging);
        if (nextFieldValue === undefined) {
          log(logPrefix, "since it has no value, checking if default is defined,replaceEmptyFields = " + JSON.stringify(replaceEmptyFields), widgetId, params.context.EnableConsoleLogging);
          if (replaceEmptyFields && replaceEmptyFields.hasOwnProperty(nextColumnToReplace)) {
            nextFieldValue = replaceEmptyFields[nextColumnToReplace];
            log(logPrefix, "it does! nextFieldValue now = " + nextFieldValue, widgetId, params.context.EnableConsoleLogging);
          } else {
            log(logPrefix, "it has no default, setting to NaN", widgetId, params.context.EnableConsoleLogging);
            nextFieldValue = NaN;
          }
        }
        log(logPrefix, "replacing " + nextColumnToReplace + " with value = " + nextFieldValue, widgetId, params.context.EnableConsoleLogging);
        var cellValueAsNumber = Number(nextFieldValue) + "";
        formula = formula.replaceAll(nextColumnToReplace, cellValueAsNumber);
      });
      log(logPrefix, "after variable substitution, formula = " + formula, widgetId, params.context.EnableConsoleLogging);
      try {
        calculatedResult = eval(formula);
        if (decimalPlaces) {
          log(logPrefix, "calculatedResult = " + calculatedResult + ", rounding to " + decimalPlaces + " places", widgetId, params.context.EnableConsoleLogging);
          calculatedResult = (Math.round(calculatedResult * 100) / 100).toFixed(decimalPlaces);
        }
      } catch (err) {
        log(logPrefix, "err = " + err, widgetId, params.context.EnableConsoleLogging);
        calculatedResult = "Error calculating cell, please check formula.";
      }
      log(logPrefix, "calculatedResult = " + calculatedResult, widgetId, params.context.EnableConsoleLogging);
      if (allRequiredColumnsDefined_1 && calculatedResult !== "NaN") {
        log(logPrefix, "calculatedResult is a number, updating cell value...", widgetId, params.context.EnableConsoleLogging);
        params.data[fieldName] = calculatedResult;
      }
    }
    returnValue = calculatedResult && calculatedResult !== "NaN" ? calculatedResult.toString() : "";
    log(logPrefix, "returning returnValue = " + returnValue, widgetId, params.context.EnableConsoleLogging);
  }
  return returnValue;
}
function aggregateColumns(params, aggregatorParams, widgetId) {
  var aggregateValue;
  var fieldName = params.colDef.field;
  var rowValue = params.data[fieldName] ? params.data[fieldName] : 0;
  var logPrefix = ".aggregateColumns(" + fieldName + "/row # " + params.node.rowIndex + "): ";
  var enableAggregationField = aggregatorParams.enableAggregationField;
  var formula = aggregatorParams.formula;
  var decimalPlaces = aggregatorParams.decimalPlaces;
  var thisRowIsForAggregating = params.data[enableAggregationField];
  log(logPrefix, "row been updated to: " + rowValue, widgetId, params.context.EnableConsoleLogging);
  log(logPrefix, "enableAggregationField = " + enableAggregationField + ", thisRowIsForAggregating = " + thisRowIsForAggregating, widgetId, params.context.EnableConsoleLogging);
  if (params.data && (!params.node || !params.node.group)) {
    if (!thisRowIsForAggregating) {
      log(logPrefix, "row is aggregate row, just returning value: " + rowValue, widgetId, params.context.EnableConsoleLogging);
      return rowValue;
    }
    var columnValues_1 = [];
    var enableAggregateRowNotReached_1 = true;
    params.api.forEachNode(function (node, index) {
      console.log(logPrefix, widgetId, "node = ", node);
      if (!node.data[enableAggregationField]) {
        var nextRowColumnValue = node.data[fieldName] ? node.data[fieldName] : 0;
        log(logPrefix, "row # " + index + ", enableAggregationField is disabled, adding value: " + nextRowColumnValue + " to columnValues", widgetId, params.context.EnableConsoleLogging);
        if (enableAggregateRowNotReached_1) {
          columnValues_1.push(parseFloat(nextRowColumnValue));
        } else {
          enableAggregateRowNotReached_1 = false;
        }
      }
    });
    log(logPrefix, "columnValues = " + columnValues_1, widgetId, params.context.EnableConsoleLogging);
    var valueSum = columnValues_1.reduce(function (acc, curr) {
      return acc + curr;
    }, 0);
    log(logPrefix, "valueSum = " + valueSum, widgetId, params.context.EnableConsoleLogging);
    switch (formula) {
      case "SUM":
        log(logPrefix, "summing", widgetId, params.context.EnableConsoleLogging);
        aggregateValue = valueSum;
        break;
      case "AVG":
        log(logPrefix, "averaging", widgetId, params.context.EnableConsoleLogging);
        aggregateValue = valueSum / columnValues_1.length;
        break;
      case "MIN":
        log(logPrefix, "getting min", widgetId, params.context.EnableConsoleLogging);
        aggregateValue = Math.min.apply(Math, columnValues_1);
        break;
      case "MAX":
        log(logPrefix, "getting max", widgetId, params.context.EnableConsoleLogging);
        aggregateValue = Math.max.apply(Math, columnValues_1);
    }
    if (decimalPlaces) {
      log(logPrefix, "aggregateValue = " + aggregateValue + ", rounding to " + decimalPlaces + " places", widgetId, params.context.EnableConsoleLogging);
      aggregateValue = (Math.round(aggregateValue * 100) / 100).toFixed(decimalPlaces);
    }
    log(logPrefix, "updating aggregate cell using aggregateValue = " + aggregateValue, widgetId, params.context.EnableConsoleLogging);
    return aggregateValue;
  }
}
function aggregateMultiple(params, multipleAggregators, widgetId) {
  var fieldName = params.colDef.field;
  var returnObject = {
    toString: function () {
      if (params.node[fieldName]) {
        return params.node[fieldName];
      } else {
        return "";
      }
    }
  };
  var logPrefix = ".aggregateMultiple(" + fieldName + "/row # " + params.node.rowIndex + "): ";
  log(logPrefix, "checking multiple aggregators...", widgetId, params.context.EnableConsoleLogging);
  Object.keys(multipleAggregators).forEach(function (nextMultipleAggrgator) {
    var nextAggregatorObject = multipleAggregators[nextMultipleAggrgator];
    var nextAggregator = nextAggregatorObject.aggregator;
    var nextAggregatorParams = nextAggregatorObject.aggregatorParams;
    var enableAggregationField = nextAggregatorParams.enableAggregationField;
    var alwaysAggregate = nextAggregatorParams.alwaysAggregate;
    log(logPrefix, "  nextAggregatorObject = " + JSON.stringify(nextAggregatorObject), widgetId, params.context.EnableConsoleLogging);
    log(logPrefix, "seeing whether to use nextAggregator: " + nextAggregator + " by using enableAggregationField: " + enableAggregationField, widgetId, params.context.EnableConsoleLogging);
    if ((!params.node || !params.node.group) && (log(logPrefix, "is alwaysAggregate or enableAggregationField true? alwaysAggregate = " + alwaysAggregate + ", enableAggregationField = " + params.data[enableAggregationField], widgetId, params.context.EnableConsoleLogging), alwaysAggregate || params.data[enableAggregationField])) {
      log(logPrefix, "it is! switching to  = " + nextAggregator, widgetId, params.context.EnableConsoleLogging);
      switch (nextAggregator) {
        case "aggregateRows":
          returnObject = aggregateRows(params, nextAggregatorParams, params.context.WidgetId);
          break;
        case "aggregateColumns":
          returnObject = aggregateColumns(params, nextAggregatorParams, params.context.WidgetId);
      }
    }
    log(logPrefix, "done...", widgetId, params.context.EnableConsoleLogging);
  });
  return returnObject;
}
var dateFilterParams = {
  suppressAndOrCondition: true,
  comparator: function (filterLocalDateAtMidnight, cellValue) {
    var logPrefix = ".DateFilter.comparator(): ";
    log(logPrefix, "filterLocalDateAtMidnight = " + filterLocalDateAtMidnight);
    log(logPrefix, "cellValue = " + cellValue);
    if (cellValue == null) {
      return 0;
    }
    var cellDate = new Date(cellValue).setHours(0, 0, 0, 0);
    log(logPrefix, `cellDate = ${cellDate}`);
    if (cellDate < filterLocalDateAtMidnight) {
      log(logPrefix, "cell less than filter");
      return -1;
    } else if (cellDate > filterLocalDateAtMidnight) {
      log(logPrefix, "cell greater than filter");
      return 1;
    } else {
      return 0;
    }
  },
  browserDatePicker: true
};
function selectFooterCellRenderer(params) {
  var logPrefix = ".selectFooterCellRenderer(): ";
  log(logPrefix, "Starting...", params.context.WidgetId);
  var footerDetails = {
    component: _CustomColumnAggregateFooterRenderer__WEBPACK_IMPORTED_MODULE_3__.x
  };
  if (params.node.footer) {
    log(logPrefix, "params.node.footer", params.context.WidgetId);
    return footerDetails;
  } else {
    return null;
  }
}
var FILTER_MAP = {
  DATETIME: "agDateColumnFilter",
  INTEGER: "agNumberColumnFilter",
  NUMBER: "agNumberColumnFilter",
  BOOLEAN: true
};
function getAsyncFilterValues(params) {
  var columnName = params.column.getId();
  log(".getAsyncFilterValues(): ", "Starting, updating column filter for: " + columnName, params.context.WidgetId);
  var newValues = params.context.getAsyncFilterValues(columnName);
  log(".getAsyncFilterValues(): ", "newValues = " + newValues, params.context.WidgetId);
  params.success(newValues);
}
function createColumnDefs(dataAsJson, showFloatingFilters, checkboxSelection, useSetFilters, useAsyncSetFilters, columnConfig, gridWidgetId) {
  var logPrefix = ".createColumnDefs(): ";
  var columns = {};
  var firstColumn = true;
  log(logPrefix, "columnConfig = " + JSON.stringify(columnConfig), gridWidgetId);
  var children = {};
  Object.keys(columnConfig).forEach(function (nextColumn) {
    if (columnConfig[nextColumn].hasOwnProperty("children")) {
      log(logPrefix, "column def = " + nextColumn + " has children, bringing them to the top of the columnConfig", gridWidgetId);
      columnConfig[nextColumn].children.forEach(function (nextChild) {
        var nextChildField = nextChild.field;
        log(logPrefix, "nextChildField = " + nextChildField, gridWidgetId);
        children[nextChildField] = nextChild;
      });
    }
  });
  log(logPrefix, "after adding any children, children = " + JSON.stringify(children), gridWidgetId);
  try {
    function _loop_1(nextFieldName) {
      var nextField = dataAsJson.dataShape.fieldDefinitions[nextFieldName];
      log(logPrefix = ".createColumnDefs(" + nextFieldName + "): ", "adding next column " + nextFieldName + ", type = " + nextField.baseType, gridWidgetId);
      var nextColumn = {
        field: nextFieldName,
        tooltipField: nextFieldName,
        headerName: nextFieldName,
        filter: nextField.baseType != "IMAGELINK" && (useSetFilters ? "agSetColumnFilter" : !!showFloatingFilters && (FILTER_MAP.hasOwnProperty(nextField.baseType) ? FILTER_MAP[nextField.baseType] : "agTextColumnFilter")),
        filterParams: nextField.baseType == "DATETIME" ? dateFilterParams : useSetFilters ? {
          excelMode: "windows",
          showTooltips: true,
          buttons: [],
          debounceMs: 200
        } : undefined,
        floatingFilter: showFloatingFilters
      };
      if (checkboxSelection && firstColumn) {
        nextColumn.checkboxSelection = true;
        firstColumn = false;
      }
      if (nextField.baseType == "IMAGELINK") {
        nextColumn.cellRenderer = _CustomImageRenderer__WEBPACK_IMPORTED_MODULE_1__.W;
        nextColumn.cellRendererParams = {
          fields: nextFieldName
        };
      }
      if (nextField.baseType == "JSON") {
        nextColumn.cellRenderer = _CustomJsonRenderer__WEBPACK_IMPORTED_MODULE_4__.f;
      }
      if (nextField.baseType == "DATETIME") {
        log(logPrefix, "since it is DATETIME assigning cellEditor=DatePicker", gridWidgetId);
        nextColumn.cellEditor = _DatePicker__WEBPACK_IMPORTED_MODULE_2__.l;
        nextColumn.cellEditorPopup = true;
        nextColumn.cellRenderer = _CustomDateRenderer__WEBPACK_IMPORTED_MODULE_0__.j;
        nextColumn.cellDataType = "date";
        log(logPrefix, "nextColumn = " + JSON.stringify(nextColumn), gridWidgetId);
      }
      if (columnConfig.hasOwnProperty(nextFieldName) || children.hasOwnProperty(nextFieldName)) {
        log(logPrefix, "next column before override = " + JSON.stringify(nextColumn), gridWidgetId);
        var filterParamsBeforeOverride_1 = {};
        log(logPrefix, "nextColumn.filterParams = " + JSON.stringify(nextColumn.filterParams));
        if (nextColumn.filterParams) {
          filterParamsBeforeOverride_1 = nextColumn.filterParams;
          log(logPrefix, "filterParamsBeforeOverride = " + JSON.stringify(filterParamsBeforeOverride_1));
        }
        var nextColumnDef = columnConfig.hasOwnProperty(nextFieldName) ? columnConfig[nextFieldName] : children[nextFieldName];
        if ((nextColumn = Object.assign(nextColumn, nextColumnDef)).hasOwnProperty("filterParams") && Object.keys(filterParamsBeforeOverride_1).length) {
          log(logPrefix, "merging in missing filterParamsBeforeOverride", gridWidgetId);
          Object.keys(filterParamsBeforeOverride_1).forEach(function (nextParam) {
            if (!nextColumn.filterParams.hasOwnProperty(nextParam)) {
              log(logPrefix, "adding back in preconfigured filter param " + nextParam, gridWidgetId);
              nextColumn.filterParams[nextParam] = filterParamsBeforeOverride_1[nextParam];
            }
          });
        }
        if (nextColumnDef.hasOwnProperty("cellClass")) {
          var cellClassValue = nextColumnDef.cellClass;
          log(logPrefix, "next column has cellClass, value = " + cellClassValue, gridWidgetId);
          if (cellClassValue && cellClassValue.hasOwnProperty("useCellClassColumns")) {
            nextColumn.cellClass = function (params) {
              var logPrefix = ".cellClassFunction(" + nextFieldName + "): ";
              var newCellClass = "";
              if (!params.data) {
                return null;
              }
              var cellClassFieldName = params.context.CellClassColumn;
              log(logPrefix, "checking to see if this cell is styled using field: " + cellClassFieldName, gridWidgetId);
              if (params.data.hasOwnProperty(cellClassFieldName)) {
                log(logPrefix, cellClassFieldName + " value = " + params.data[cellClassFieldName], gridWidgetId);
                var cellClasses = params.data[cellClassFieldName] ? typeof params.data[cellClassFieldName] == "object" ? params.data[cellClassFieldName] : JSON.parse(params.data[cellClassFieldName]) : {};
                log(logPrefix, "cellClasses = " + JSON.stringify(cellClasses), gridWidgetId);
                log(logPrefix, "does it have this column?: = " + cellClasses.hasOwnProperty(nextFieldName), gridWidgetId);
                if (cellClasses.hasOwnProperty(nextFieldName)) {
                  newCellClass = cellClasses[nextFieldName];
                  log(logPrefix, "Yes! And it = " + newCellClass, gridWidgetId);
                }
              }
              return newCellClass;
            };
          } else {
            nextColumn.cellStyle = nextColumnDef.cellStyle;
          }
        }
        if (nextColumnDef.hasOwnProperty("editable")) {
          var editableValue = nextColumnDef.editable;
          log(logPrefix, "next column has editable, value = " + editableValue, gridWidgetId);
          if (editableValue && editableValue.hasOwnProperty("canEdit")) {
            var drivingField_1 = editableValue.canEdit;
            nextColumn.editable = function (params) {
              return params.data[drivingField_1];
            };
          } else if (editableValue.hasOwnProperty("useEditableFields")) {
            nextColumn.editable = function (params) {
              if (!params.data) {
                return null;
              }
              var fieldIsEditable = true;
              var editableFieldName = params.context.EditableFieldsColumn;
              log(logPrefix, "useEditableFields(): checking to see if this cell is editable using field: " + editableFieldName, gridWidgetId);
              var editableFields = params.data[editableFieldName] ? typeof params.data[editableFieldName] == "object" ? params.data[editableFieldName] : JSON.parse(params.data[editableFieldName]) : {};
              log(logPrefix, "editableFields = " + JSON.stringify(editableFields), gridWidgetId);
              log(logPrefix, "does it have this column?: = " + nextFieldName, gridWidgetId);
              if (editableFields.hasOwnProperty(nextFieldName)) {
                fieldIsEditable = editableFields[nextFieldName];
                log(logPrefix, "Yes! And it = " + fieldIsEditable, gridWidgetId);
              }
              return fieldIsEditable;
            };
          } else if (editableValue.hasOwnProperty("function")) {
            var editFunctionString = editableValue.function;
            log(logPrefix, "editable has a function, parsing it, it = " + editFunctionString, gridWidgetId);
            var editFunction = new Function(editFunctionString);
            nextColumn.editable = editFunction;
          } else {
            nextColumn.editable = nextColumnDef.editable;
          }
        }
        if (nextColumnDef.hasOwnProperty("aggregator")) {
          var aggregator = nextColumnDef.aggregator;
          var aggregatorParams_1 = nextColumnDef.aggregatorParams;
          var multipleAggregators_1 = nextColumnDef.multipleAggregators;
          log(logPrefix, "next column has aggregator, value = " + aggregator, gridWidgetId);
          if (aggregator == "aggregateRows") {
            nextColumn.valueGetter = function (params) {
              return aggregateRows(params, aggregatorParams_1, params.context.WidgetId);
            };
          }
          if (aggregator == "aggregateColumns") {
            nextColumn.valueGetter = function (params) {
              return aggregateColumns(params, aggregatorParams_1, params.context.WidgetId);
            };
          }
          if (aggregator == "aggregateMultiple") {
            nextColumn.valueGetter = function (params) {
              return aggregateMultiple(params, multipleAggregators_1, params.context.WidgetId);
            };
          }
        }
        if (nextColumnDef.hasOwnProperty("footerCellRendererSelector")) {
          var footerCellRendererSelector = nextColumnDef.footerCellRendererSelector;
          log(logPrefix, "next column has footerCellRendererSelector, value = " + footerCellRendererSelector, gridWidgetId);
          nextColumn.cellRendererSelector = selectFooterCellRenderer;
        }
        log(logPrefix, "next column after override = " + JSON.stringify(nextColumn), gridWidgetId);
        if (useAsyncSetFilters && nextColumnDef.filter == "agSetColumnFilter") {
          log(logPrefix, "since useAsyncSetFilters is true, switching filter param values to function", gridWidgetId);
          nextColumn.filterParams.values = getAsyncFilterValues;
        }
        if (nextField.baseType == "DATETIME") {
          nextColumn.cellRenderer = _CustomDateRenderer__WEBPACK_IMPORTED_MODULE_0__.j;
        }
      }
      columns[nextFieldName] = nextColumn;
    }
    for (var nextFieldName in dataAsJson.dataShape.fieldDefinitions) {
      _loop_1(nextFieldName);
    }
  } catch (err) {
    log(logPrefix, "error creating columns, err = " + err, gridWidgetId);
  }
  log(logPrefix = ".createColumnDefs(): ", "columns = " + JSON.stringify(columns), gridWidgetId);
  var groupedColumns = {};
  var columnsToSkip = [];
  Object.keys(columnConfig).forEach(function (nextColumn, index) {
    if (columnConfig[nextColumn].hasOwnProperty("children")) {
      log(logPrefix, "adding grouped header = " + nextColumn, gridWidgetId);
      var childFields = columnConfig[nextColumn].children.map(function (nextChild) {
        return nextChild.field;
      });
      log(logPrefix, "childFields = " + childFields, gridWidgetId);
      var groupedColumn = columnConfig[nextColumn];
      log(logPrefix, "groupedColumn.children before update = " + JSON.stringify(groupedColumn.children), gridWidgetId);
      groupedColumn.children = childFields.map(function (nextChild) {
        return columns[nextChild];
      });
      log(logPrefix, "groupedColumn.children now = " + JSON.stringify(groupedColumn.children), gridWidgetId);
      groupedColumns[index] = groupedColumn;
      columnsToSkip = columnsToSkip.concat(childFields);
    }
  });
  var sortedColumns = {};
  Object.keys(columnConfig).forEach(function (nextColumn) {
    if (columns.hasOwnProperty(nextColumn)) {
      sortedColumns[nextColumn] = columns[nextColumn];
    }
  });
  Object.keys(columns).forEach(function (nextColumn) {
    if (!sortedColumns.hasOwnProperty(nextColumn)) {
      if (columns.hasOwnProperty(nextColumn)) {
        sortedColumns[nextColumn] = columns[nextColumn];
      }
    }
  });
  log(logPrefix, "groupedColumns = " + JSON.stringify(groupedColumns), gridWidgetId);
  log(logPrefix, "sortedColumns = " + JSON.stringify(sortedColumns), gridWidgetId);
  var finalColumns = [];
  Object.keys(sortedColumns).forEach(function (nextColumn, index) {
    log(logPrefix, "building finalColumns, nextColumn = " + nextColumn + " (" + index + ")", gridWidgetId);
    if (groupedColumns.hasOwnProperty(index)) {
      finalColumns.push(groupedColumns[index]);
    } else if (!columnsToSkip.includes(nextColumn)) {
      finalColumns.push(sortedColumns[nextColumn]);
    }
  });
  log(logPrefix, "done building finalColumns: " + JSON.stringify(finalColumns), gridWidgetId);
  return finalColumns;
}
function createRowData(dataAsJson, rowConfig, showGridAsTree, childField, parentField, treeColumnField, treePathField, treePathFieldDelimeter) {
  var logPrefix = ".createRowData(): ";
  rows = [];
  log(logPrefix, "rows = " + JSON.stringify(dataAsJson.rows), "1", enableLogging);
  dataAsJson.rows.forEach(function (nextRow, index) {
    nextRow[ME] = index;
    rows.push(nextRow);
  });
  if (showGridAsTree) {
    log(logPrefix, "grid is a tree, creating paths, parentField = " + parentField + ", childField = " + childField + ", treeColumnField = " + treeColumnField);
    rows.forEach(function (nextRow) {
      childToParentIdTreeMap[nextRow[childField]] = nextRow[parentField];
    });
    try {
      rows.map(function (nextRow) {
        nextRow.agGridTreeGridPath = treePathField ? nextRow[treePathField].split(treePathFieldDelimeter).map(function (nextPath) {
          return nextPath.trim();
        }) : buildDataPaths([], nextRow[childField]).reverse();
        return nextRow;
      });
      log(logPrefix, "rows = " + JSON.stringify(rows));
    } catch (err) {
      log(logPrefix, "error creating tree data, err = " + err + ", stack = " + err.stack);
    }
  }
  return rows;
}
function buildDataPaths(dataPathArray, childId) {
  if (childToParentIdTreeMap.hasOwnProperty(childId)) {
    dataPathArray.push(childId);
    buildDataPaths(dataPathArray, childToParentIdTreeMap[childId]);
  }
  return dataPathArray;
}
export function tF(dataAsJson, showFloatingFilters, checkboxSelection, useSetFilters, gridConfig, showGridAsTree, childField, parentField, treePathField, treePathFieldDelimeter, treeColumnField, gridDateFormat, EnableConsoleLogging, useAsyncSetFilters, gridWidgetId) {
  enableLogging = EnableConsoleLogging;
  return {
    columnDefs: createColumnDefs(dataAsJson, showFloatingFilters, checkboxSelection, useSetFilters, useAsyncSetFilters, gridConfig.ColumnConfig, gridWidgetId),
    rowData: createRowData(dataAsJson, gridConfig.RowConfig, showGridAsTree, childField, parentField, treeColumnField, treePathField, treePathFieldDelimeter),
    undoRedoCellEditing: true,
    undoRedoCellEditingLimit: 2000,
    getDataPath: function (data) {
      return data.agGridTreeGridPath;
    },
    excelStyles: gridConfig.excelStyles ? gridConfig.excelStyles : []
  };
}
var licenseKey = "";