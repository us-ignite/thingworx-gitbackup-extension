// Recovered from webpack module 7386.
import { CustomDateRenderer } from "./date-renderer";
import { CustomImageRenderer } from "./image-renderer";
import { DatePicker } from "./date-picker";
import { CustomColumnAggregateFooterRenderer } from "./aggregate-footer";
import { CustomJsonRenderer } from "./json-renderer";
import { writeWidgetLog } from "./widget-logging";

export const AG_GRID_ROW_ID_FIELD = "agGridId";
export const SERVER_SIDE_ROW_COUNT_STATUS_PANEL_ID =
  "ServerSideStatusPanelRowCount";
const rows = [];
const childToParentIdTreeMap = {};
const enableLogging = false;
function log(logPrefix, logMessage, widgetId?, loggingEnabled?) {
  writeWidgetLog(
    loggingEnabled,
    `AgGridWidget-${widgetId}.Grid()`,
    logPrefix,
    logMessage,
  );
}
function aggregateRows(params, aggregatorParams, widgetId) {
  const fieldName = params.colDef.field;
  const logPrefix =
    ".aggregateRows(" + fieldName + "/row # " + params.node.rowIndex + "): ";
  const enableAggregationField = aggregatorParams.enableAggregationField;
  const alwaysAggregate = aggregatorParams.alwaysAggregate;
  let formula = aggregatorParams.formula;
  const columnsInFormula = aggregatorParams.columnsInFormula;
  const requiredColumns = aggregatorParams.requiredColumns;
  const decimalPlaces = aggregatorParams.decimalPlaces;
  const replaceEmptyFields = aggregatorParams.replaceEmptyFields;
  let returnValue;
  log(
    logPrefix,
    "alwaysAggregate = " + alwaysAggregate,
    widgetId,
    params.context.EnableConsoleLogging,
  );
  log(
    logPrefix,
    "requiredColumns = " + requiredColumns,
    widgetId,
    params.context.EnableConsoleLogging,
  );
  if (!alwaysAggregate) {
    log(
      logPrefix,
      "enable field = " +
        enableAggregationField +
        ", its value is: " +
        params.data[enableAggregationField],
      widgetId,
      params.context.EnableConsoleLogging,
    );
  }
  if (!params.node?.group) {
    let calculatedResult = undefined;
    let allRequiredColumnsDefined_1 = true;
    const returnObject = {
      toString: function () {
        if (params.data[fieldName]) {
          return params.data[fieldName];
        } else {
          return "";
        }
      },
    };
    requiredColumns.forEach(function (nextRequiredColumn) {
      log(
        logPrefix,
        "nextRequiredColumn = " +
          nextRequiredColumn +
          ", its value is: " +
          params.data[nextRequiredColumn],
        widgetId,
        params.context.EnableConsoleLogging,
      );
      if (isNaN(params.data[nextRequiredColumn])) {
        allRequiredColumnsDefined_1 = false;
      }
    });
    log(
      logPrefix,
      "allRequiredColumnsDefined = " + allRequiredColumnsDefined_1,
      widgetId,
      params.context.EnableConsoleLogging,
    );
    if (
      allRequiredColumnsDefined_1 &&
      (alwaysAggregate || params.data[enableAggregationField])
    ) {
      log(
        logPrefix,
        "enableAggregationField is enabled, calculating row, formula = " +
          formula,
        widgetId,
        params.context.EnableConsoleLogging,
      );
      log(
        logPrefix,
        "params.data = " + JSON.stringify(params.data),
        widgetId,
        params.context.EnableConsoleLogging,
      );
      columnsInFormula.forEach(function (nextColumnToReplace) {
        let nextFieldValue = params.data[nextColumnToReplace];
        log(
          logPrefix,
          "nextColumnToReplace = " +
            nextColumnToReplace +
            ", its value = " +
            nextFieldValue,
          widgetId,
          params.context.EnableConsoleLogging,
        );
        log(
          logPrefix,
          "typeof nextFieldValue = " +
            typeof nextFieldValue +
            ", it is gt or eq to 0? = " +
            (parseFloat(nextFieldValue) >= 0),
          widgetId,
          params.context.EnableConsoleLogging,
        );
        if (nextFieldValue === undefined) {
          log(
            logPrefix,
            "since it has no value, checking if default is defined,replaceEmptyFields = " +
              JSON.stringify(replaceEmptyFields),
            widgetId,
            params.context.EnableConsoleLogging,
          );
          if (replaceEmptyFields?.hasOwnProperty(nextColumnToReplace)) {
            nextFieldValue = replaceEmptyFields[nextColumnToReplace];
            log(
              logPrefix,
              "it does! nextFieldValue now = " + nextFieldValue,
              widgetId,
              params.context.EnableConsoleLogging,
            );
          } else {
            log(
              logPrefix,
              "it has no default, setting to NaN",
              widgetId,
              params.context.EnableConsoleLogging,
            );
            nextFieldValue = NaN;
          }
        }
        log(
          logPrefix,
          "replacing " +
            nextColumnToReplace +
            " with value = " +
            nextFieldValue,
          widgetId,
          params.context.EnableConsoleLogging,
        );
        const cellValueAsNumber = Number(nextFieldValue) + "";
        formula = formula.replaceAll(nextColumnToReplace, cellValueAsNumber);
      });
      log(
        logPrefix,
        "after variable substitution, formula = " + formula,
        widgetId,
        params.context.EnableConsoleLogging,
      );
      try {
        calculatedResult = eval(formula);
        if (decimalPlaces) {
          log(
            logPrefix,
            "calculatedResult = " +
              calculatedResult +
              ", rounding to " +
              decimalPlaces +
              " places",
            widgetId,
            params.context.EnableConsoleLogging,
          );
          calculatedResult = (Math.round(calculatedResult * 100) / 100).toFixed(
            decimalPlaces,
          );
        }
      } catch (err) {
        log(
          logPrefix,
          "err = " + err,
          widgetId,
          params.context.EnableConsoleLogging,
        );
        calculatedResult = "Error calculating cell, please check formula.";
      }
      log(
        logPrefix,
        "calculatedResult = " + calculatedResult,
        widgetId,
        params.context.EnableConsoleLogging,
      );
      if (allRequiredColumnsDefined_1 && calculatedResult !== "NaN") {
        log(
          logPrefix,
          "calculatedResult is a number, updating cell value...",
          widgetId,
          params.context.EnableConsoleLogging,
        );
        params.data[fieldName] = calculatedResult;
      }
    }
    returnValue =
      calculatedResult && calculatedResult !== "NaN"
        ? calculatedResult.toString()
        : "";
    log(
      logPrefix,
      "returning returnValue = " + returnValue,
      widgetId,
      params.context.EnableConsoleLogging,
    );
  }
  return returnValue;
}
function aggregateColumns(params, aggregatorParams, widgetId) {
  let aggregateValue;
  const fieldName = params.colDef.field;
  const rowValue = params.data[fieldName] ? params.data[fieldName] : 0;
  const logPrefix =
    ".aggregateColumns(" + fieldName + "/row # " + params.node.rowIndex + "): ";
  const enableAggregationField = aggregatorParams.enableAggregationField;
  const formula = aggregatorParams.formula;
  const decimalPlaces = aggregatorParams.decimalPlaces;
  const thisRowIsForAggregating = params.data[enableAggregationField];
  log(
    logPrefix,
    "row been updated to: " + rowValue,
    widgetId,
    params.context.EnableConsoleLogging,
  );
  log(
    logPrefix,
    "enableAggregationField = " +
      enableAggregationField +
      ", thisRowIsForAggregating = " +
      thisRowIsForAggregating,
    widgetId,
    params.context.EnableConsoleLogging,
  );
  if (params.data && !params.node?.group) {
    if (!thisRowIsForAggregating) {
      log(
        logPrefix,
        "row is aggregate row, just returning value: " + rowValue,
        widgetId,
        params.context.EnableConsoleLogging,
      );
      return rowValue;
    }
    let columnValues_1 = [];
    let enableAggregateRowNotReached_1 = true;
    params.api.forEachNode(function (node, index) {
      console.log(logPrefix, widgetId, "node = ", node);
      if (!node.data[enableAggregationField]) {
        const nextRowColumnValue = node.data[fieldName]
          ? node.data[fieldName]
          : 0;
        log(
          logPrefix,
          "row # " +
            index +
            ", enableAggregationField is disabled, adding value: " +
            nextRowColumnValue +
            " to columnValues",
          widgetId,
          params.context.EnableConsoleLogging,
        );
        if (enableAggregateRowNotReached_1) {
          columnValues_1.push(parseFloat(nextRowColumnValue));
        } else {
          enableAggregateRowNotReached_1 = false;
        }
      }
    });
    log(
      logPrefix,
      "columnValues = " + columnValues_1,
      widgetId,
      params.context.EnableConsoleLogging,
    );
    const valueSum = columnValues_1.reduce(function (acc, curr) {
      return acc + curr;
    }, 0);
    log(
      logPrefix,
      "valueSum = " + valueSum,
      widgetId,
      params.context.EnableConsoleLogging,
    );
    switch (formula) {
      case "SUM":
        log(
          logPrefix,
          "summing",
          widgetId,
          params.context.EnableConsoleLogging,
        );
        aggregateValue = valueSum;
        break;
      case "AVG":
        log(
          logPrefix,
          "averaging",
          widgetId,
          params.context.EnableConsoleLogging,
        );
        aggregateValue = valueSum / columnValues_1.length;
        break;
      case "MIN":
        log(
          logPrefix,
          "getting min",
          widgetId,
          params.context.EnableConsoleLogging,
        );
        aggregateValue = Math.min.apply(Math, columnValues_1);
        break;
      case "MAX":
        log(
          logPrefix,
          "getting max",
          widgetId,
          params.context.EnableConsoleLogging,
        );
        aggregateValue = Math.max.apply(Math, columnValues_1);
    }
    if (decimalPlaces) {
      log(
        logPrefix,
        "aggregateValue = " +
          aggregateValue +
          ", rounding to " +
          decimalPlaces +
          " places",
        widgetId,
        params.context.EnableConsoleLogging,
      );
      aggregateValue = (Math.round(aggregateValue * 100) / 100).toFixed(
        decimalPlaces,
      );
    }
    log(
      logPrefix,
      "updating aggregate cell using aggregateValue = " + aggregateValue,
      widgetId,
      params.context.EnableConsoleLogging,
    );
    return aggregateValue;
  }
}
function aggregateMultiple(params, multipleAggregators, widgetId) {
  const fieldName = params.colDef.field;
  let returnObject = {
    toString: function () {
      if (params.node[fieldName]) {
        return params.node[fieldName];
      } else {
        return "";
      }
    },
  };
  const logPrefix =
    ".aggregateMultiple(" +
    fieldName +
    "/row # " +
    params.node.rowIndex +
    "): ";
  log(
    logPrefix,
    "checking multiple aggregators...",
    widgetId,
    params.context.EnableConsoleLogging,
  );
  Object.keys(multipleAggregators).forEach(function (nextMultipleAggrgator) {
    const nextAggregatorObject = multipleAggregators[nextMultipleAggrgator];
    const nextAggregator = nextAggregatorObject.aggregator;
    const nextAggregatorParams = nextAggregatorObject.aggregatorParams;
    const enableAggregationField = nextAggregatorParams.enableAggregationField;
    const alwaysAggregate = nextAggregatorParams.alwaysAggregate;
    log(
      logPrefix,
      "  nextAggregatorObject = " + JSON.stringify(nextAggregatorObject),
      widgetId,
      params.context.EnableConsoleLogging,
    );
    log(
      logPrefix,
      "seeing whether to use nextAggregator: " +
        nextAggregator +
        " by using enableAggregationField: " +
        enableAggregationField,
      widgetId,
      params.context.EnableConsoleLogging,
    );
    if (
      !params.node?.group &&
      (log(
        logPrefix,
        "is alwaysAggregate or enableAggregationField true? alwaysAggregate = " +
          alwaysAggregate +
          ", enableAggregationField = " +
          params.data[enableAggregationField],
        widgetId,
        params.context.EnableConsoleLogging,
      ),
      alwaysAggregate || params.data[enableAggregationField])
    ) {
      log(
        logPrefix,
        "it is! switching to  = " + nextAggregator,
        widgetId,
        params.context.EnableConsoleLogging,
      );
      switch (nextAggregator) {
        case "aggregateRows":
          returnObject = aggregateRows(
            params,
            nextAggregatorParams,
            params.context.WidgetId,
          );
          break;
        case "aggregateColumns":
          returnObject = aggregateColumns(
            params,
            nextAggregatorParams,
            params.context.WidgetId,
          );
      }
    }
    log(logPrefix, "done...", widgetId, params.context.EnableConsoleLogging);
  });
  return returnObject;
}
const dateFilterParams: any = {
  suppressAndOrCondition: true,
  comparator: function (filterLocalDateAtMidnight, cellValue) {
    const logPrefix = ".DateFilter.comparator(): ";
    log(logPrefix, "filterLocalDateAtMidnight = " + filterLocalDateAtMidnight);
    log(logPrefix, "cellValue = " + cellValue);
    if (cellValue == null) {
      return 0;
    }
    const cellDate = new Date(cellValue).setHours(0, 0, 0, 0);
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
  browserDatePicker: true,
};
function selectFooterCellRenderer(params) {
  const logPrefix = ".selectFooterCellRenderer(): ";
  log(logPrefix, "Starting...", params.context.WidgetId);
  const footerDetails = {
    component: CustomColumnAggregateFooterRenderer,
  };
  if (params.node.footer) {
    log(logPrefix, "params.node.footer", params.context.WidgetId);
    return footerDetails;
  } else {
    return null;
  }
}
const FILTER_MAP = {
  DATETIME: "agDateColumnFilter",
  INTEGER: "agNumberColumnFilter",
  NUMBER: "agNumberColumnFilter",
  BOOLEAN: true,
};
function getAsyncFilterValues(params) {
  const columnName = params.column.getId();
  log(
    ".getAsyncFilterValues(): ",
    "Starting, updating column filter for: " + columnName,
    params.context.WidgetId,
  );
  const newValues = params.context.getAsyncFilterValues(columnName);
  log(
    ".getAsyncFilterValues(): ",
    "newValues = " + newValues,
    params.context.WidgetId,
  );
  params.success(newValues);
}
function createColumnDefs(
  dataAsJson,
  showFloatingFilters,
  checkboxSelection,
  useSetFilters,
  useAsyncSetFilters,
  columnConfig,
  gridWidgetId,
) {
  let logPrefix = ".createColumnDefs(): ";
  const columns = {};
  let firstColumn = true;
  log(
    logPrefix,
    "columnConfig = " + JSON.stringify(columnConfig),
    gridWidgetId,
  );
  const children = {};
  Object.keys(columnConfig).forEach(function (nextColumn) {
    if (columnConfig[nextColumn].hasOwnProperty("children")) {
      log(
        logPrefix,
        "column def = " +
          nextColumn +
          " has children, bringing them to the top of the columnConfig",
        gridWidgetId,
      );
      columnConfig[nextColumn].children.forEach(function (nextChild) {
        const nextChildField = nextChild.field;
        log(logPrefix, "nextChildField = " + nextChildField, gridWidgetId);
        children[nextChildField] = nextChild;
      });
    }
  });
  log(
    logPrefix,
    "after adding any children, children = " + JSON.stringify(children),
    gridWidgetId,
  );
  try {
    function _loop_1(nextFieldName) {
      const nextField = dataAsJson.dataShape.fieldDefinitions[nextFieldName];
      log(
        (logPrefix = ".createColumnDefs(" + nextFieldName + "): "),
        "adding next column " +
          nextFieldName +
          ", type = " +
          nextField.baseType,
        gridWidgetId,
      );
      let nextColumn: any = {
        field: nextFieldName,
        tooltipField: nextFieldName,
        headerName: nextFieldName,
        filter:
          nextField.baseType != "IMAGELINK" &&
          (useSetFilters
            ? "agSetColumnFilter"
            : !!showFloatingFilters &&
              (FILTER_MAP.hasOwnProperty(nextField.baseType)
                ? FILTER_MAP[nextField.baseType]
                : "agTextColumnFilter")),
        filterParams:
          nextField.baseType == "DATETIME"
            ? dateFilterParams
            : useSetFilters
              ? {
                  excelMode: "windows",
                  showTooltips: true,
                  buttons: [],
                  debounceMs: 200,
                }
              : undefined,
        floatingFilter: showFloatingFilters,
      };
      if (checkboxSelection && firstColumn) {
        nextColumn.checkboxSelection = true;
        firstColumn = false;
      }
      if (nextField.baseType == "IMAGELINK") {
        nextColumn.cellRenderer = CustomImageRenderer;
        nextColumn.cellRendererParams = {
          fields: nextFieldName,
        };
      }
      if (nextField.baseType == "JSON") {
        nextColumn.cellRenderer = CustomJsonRenderer;
      }
      if (nextField.baseType == "DATETIME") {
        log(
          logPrefix,
          "since it is DATETIME assigning cellEditor=DatePicker",
          gridWidgetId,
        );
        nextColumn.cellEditor = DatePicker;
        nextColumn.cellEditorPopup = true;
        nextColumn.cellRenderer = CustomDateRenderer;
        nextColumn.cellDataType = "date";
        log(
          logPrefix,
          "nextColumn = " + JSON.stringify(nextColumn),
          gridWidgetId,
        );
      }
      if (
        columnConfig.hasOwnProperty(nextFieldName) ||
        children.hasOwnProperty(nextFieldName)
      ) {
        log(
          logPrefix,
          "next column before override = " + JSON.stringify(nextColumn),
          gridWidgetId,
        );
        let filterParamsBeforeOverride_1 = {};
        log(
          logPrefix,
          "nextColumn.filterParams = " +
            JSON.stringify(nextColumn.filterParams),
        );
        if (nextColumn.filterParams) {
          filterParamsBeforeOverride_1 = nextColumn.filterParams;
          log(
            logPrefix,
            "filterParamsBeforeOverride = " +
              JSON.stringify(filterParamsBeforeOverride_1),
          );
        }
        const nextColumnDef = columnConfig.hasOwnProperty(nextFieldName)
          ? columnConfig[nextFieldName]
          : children[nextFieldName];
        if (
          (nextColumn = Object.assign(
            nextColumn,
            nextColumnDef,
          )).hasOwnProperty("filterParams") &&
          Object.keys(filterParamsBeforeOverride_1).length
        ) {
          log(
            logPrefix,
            "merging in missing filterParamsBeforeOverride",
            gridWidgetId,
          );
          Object.keys(filterParamsBeforeOverride_1).forEach(
            function (nextParam) {
              if (!nextColumn.filterParams.hasOwnProperty(nextParam)) {
                log(
                  logPrefix,
                  "adding back in preconfigured filter param " + nextParam,
                  gridWidgetId,
                );
                nextColumn.filterParams[nextParam] =
                  filterParamsBeforeOverride_1[nextParam];
              }
            },
          );
        }
        if (nextColumnDef.hasOwnProperty("cellClass")) {
          const cellClassValue = nextColumnDef.cellClass;
          log(
            logPrefix,
            "next column has cellClass, value = " + cellClassValue,
            gridWidgetId,
          );
          if (cellClassValue?.hasOwnProperty("useCellClassColumns")) {
            nextColumn.cellClass = function (params) {
              const logPrefix = ".cellClassFunction(" + nextFieldName + "): ";
              let newCellClass = "";
              if (!params.data) {
                return null;
              }
              const cellClassFieldName = params.context.CellClassColumn;
              log(
                logPrefix,
                "checking to see if this cell is styled using field: " +
                  cellClassFieldName,
                gridWidgetId,
              );
              if (params.data.hasOwnProperty(cellClassFieldName)) {
                log(
                  logPrefix,
                  cellClassFieldName +
                    " value = " +
                    params.data[cellClassFieldName],
                  gridWidgetId,
                );
                const cellClasses = params.data[cellClassFieldName]
                  ? typeof params.data[cellClassFieldName] == "object"
                    ? params.data[cellClassFieldName]
                    : JSON.parse(params.data[cellClassFieldName])
                  : {};
                log(
                  logPrefix,
                  "cellClasses = " + JSON.stringify(cellClasses),
                  gridWidgetId,
                );
                log(
                  logPrefix,
                  "does it have this column?: = " +
                    cellClasses.hasOwnProperty(nextFieldName),
                  gridWidgetId,
                );
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
          const editableValue = nextColumnDef.editable;
          log(
            logPrefix,
            "next column has editable, value = " + editableValue,
            gridWidgetId,
          );
          if (editableValue?.hasOwnProperty("canEdit")) {
            const drivingField_1 = editableValue.canEdit;
            nextColumn.editable = function (params) {
              return params.data[drivingField_1];
            };
          } else if (editableValue.hasOwnProperty("useEditableFields")) {
            nextColumn.editable = function (params) {
              if (!params.data) {
                return null;
              }
              let fieldIsEditable = true;
              const editableFieldName = params.context.EditableFieldsColumn;
              log(
                logPrefix,
                "useEditableFields(): checking to see if this cell is editable using field: " +
                  editableFieldName,
                gridWidgetId,
              );
              const editableFields = params.data[editableFieldName]
                ? typeof params.data[editableFieldName] == "object"
                  ? params.data[editableFieldName]
                  : JSON.parse(params.data[editableFieldName])
                : {};
              log(
                logPrefix,
                "editableFields = " + JSON.stringify(editableFields),
                gridWidgetId,
              );
              log(
                logPrefix,
                "does it have this column?: = " + nextFieldName,
                gridWidgetId,
              );
              if (editableFields.hasOwnProperty(nextFieldName)) {
                fieldIsEditable = editableFields[nextFieldName];
                log(
                  logPrefix,
                  "Yes! And it = " + fieldIsEditable,
                  gridWidgetId,
                );
              }
              return fieldIsEditable;
            };
          } else if (editableValue.hasOwnProperty("function")) {
            const editFunctionString = editableValue.function;
            log(
              logPrefix,
              "editable has a function, parsing it, it = " + editFunctionString,
              gridWidgetId,
            );
            const editFunction = new Function(editFunctionString);
            nextColumn.editable = editFunction;
          } else {
            nextColumn.editable = nextColumnDef.editable;
          }
        }
        if (nextColumnDef.hasOwnProperty("aggregator")) {
          const aggregator = nextColumnDef.aggregator;
          const aggregatorParams_1 = nextColumnDef.aggregatorParams;
          const multipleAggregators_1 = nextColumnDef.multipleAggregators;
          log(
            logPrefix,
            "next column has aggregator, value = " + aggregator,
            gridWidgetId,
          );
          if (aggregator == "aggregateRows") {
            nextColumn.valueGetter = function (params) {
              return aggregateRows(
                params,
                aggregatorParams_1,
                params.context.WidgetId,
              );
            };
          }
          if (aggregator == "aggregateColumns") {
            nextColumn.valueGetter = function (params) {
              return aggregateColumns(
                params,
                aggregatorParams_1,
                params.context.WidgetId,
              );
            };
          }
          if (aggregator == "aggregateMultiple") {
            nextColumn.valueGetter = function (params) {
              return aggregateMultiple(
                params,
                multipleAggregators_1,
                params.context.WidgetId,
              );
            };
          }
        }
        if (nextColumnDef.hasOwnProperty("footerCellRendererSelector")) {
          const footerCellRendererSelector =
            nextColumnDef.footerCellRendererSelector;
          log(
            logPrefix,
            "next column has footerCellRendererSelector, value = " +
              footerCellRendererSelector,
            gridWidgetId,
          );
          nextColumn.cellRendererSelector = selectFooterCellRenderer;
        }
        log(
          logPrefix,
          "next column after override = " + JSON.stringify(nextColumn),
          gridWidgetId,
        );
        if (useAsyncSetFilters && nextColumnDef.filter == "agSetColumnFilter") {
          log(
            logPrefix,
            "since useAsyncSetFilters is true, switching filter param values to function",
            gridWidgetId,
          );
          nextColumn.filterParams.values = getAsyncFilterValues;
        }
        if (nextField.baseType == "DATETIME") {
          nextColumn.cellRenderer = CustomDateRenderer;
        }
      }
      columns[nextFieldName] = nextColumn;
    }
    for (const nextFieldName in dataAsJson.dataShape.fieldDefinitions) {
      _loop_1(nextFieldName);
    }
  } catch (err) {
    log(logPrefix, "error creating columns, err = " + err, gridWidgetId);
  }
  log(
    (logPrefix = ".createColumnDefs(): "),
    "columns = " + JSON.stringify(columns),
    gridWidgetId,
  );
  const groupedColumns = {};
  let columnsToSkip = [];
  Object.keys(columnConfig).forEach(function (nextColumn, index) {
    if (columnConfig[nextColumn].hasOwnProperty("children")) {
      log(logPrefix, "adding grouped header = " + nextColumn, gridWidgetId);
      const childFields = columnConfig[nextColumn].children.map(
        function (nextChild) {
          return nextChild.field;
        },
      );
      log(logPrefix, "childFields = " + childFields, gridWidgetId);
      const groupedColumn = columnConfig[nextColumn];
      log(
        logPrefix,
        "groupedColumn.children before update = " +
          JSON.stringify(groupedColumn.children),
        gridWidgetId,
      );
      groupedColumn.children = childFields.map(function (nextChild) {
        return columns[nextChild];
      });
      log(
        logPrefix,
        "groupedColumn.children now = " +
          JSON.stringify(groupedColumn.children),
        gridWidgetId,
      );
      groupedColumns[index] = groupedColumn;
      columnsToSkip = columnsToSkip.concat(childFields);
    }
  });
  const sortedColumns = {};
  Object.keys(columnConfig).forEach(function (nextColumn) {
    if (columns.hasOwnProperty(nextColumn)) {
      sortedColumns[nextColumn] = columns[nextColumn];
    }
  });
  Object.keys(columns).forEach(function (nextColumn) {
    if (!sortedColumns.hasOwnProperty(nextColumn)) {
      sortedColumns[nextColumn] = columns[nextColumn];
    }
  });
  log(
    logPrefix,
    "groupedColumns = " + JSON.stringify(groupedColumns),
    gridWidgetId,
  );
  log(
    logPrefix,
    "sortedColumns = " + JSON.stringify(sortedColumns),
    gridWidgetId,
  );
  const finalColumns = [];
  Object.keys(sortedColumns).forEach(function (nextColumn, index) {
    log(
      logPrefix,
      "building finalColumns, nextColumn = " + nextColumn + " (" + index + ")",
      gridWidgetId,
    );
    if (groupedColumns.hasOwnProperty(index)) {
      finalColumns.push(groupedColumns[index]);
    } else if (!columnsToSkip.includes(nextColumn)) {
      finalColumns.push(sortedColumns[nextColumn]);
    }
  });
  log(
    logPrefix,
    "done building finalColumns: " + JSON.stringify(finalColumns),
    gridWidgetId,
  );
  return finalColumns;
}
function createRowData(
  dataAsJson,
  rowConfig,
  showGridAsTree,
  childField,
  parentField,
  treeColumnField,
  treePathField,
  treePathFieldDelimeter,
) {
  const logPrefix = ".createRowData(): ";
  let rows = [];
  log(
    logPrefix,
    "rows = " + JSON.stringify(dataAsJson.rows),
    "1",
    enableLogging,
  );
  dataAsJson.rows.forEach(function (nextRow, index) {
    nextRow[AG_GRID_ROW_ID_FIELD] = index;
    rows.push(nextRow);
  });
  if (showGridAsTree) {
    log(
      logPrefix,
      "grid is a tree, creating paths, parentField = " +
        parentField +
        ", childField = " +
        childField +
        ", treeColumnField = " +
        treeColumnField,
    );
    rows.forEach(function (nextRow) {
      childToParentIdTreeMap[nextRow[childField]] = nextRow[parentField];
    });
    try {
      rows.map(function (nextRow) {
        nextRow.agGridTreeGridPath = treePathField
          ? nextRow[treePathField]
              .split(treePathFieldDelimeter)
              .map(function (nextPath) {
                return nextPath.trim();
              })
          : buildDataPaths([], nextRow[childField]).reverse();
        return nextRow;
      });
      log(logPrefix, "rows = " + JSON.stringify(rows));
    } catch (err) {
      log(
        logPrefix,
        "error creating tree data, err = " + err + ", stack = " + err.stack,
      );
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
export function createGridData(
  dataAsJson,
  showFloatingFilters,
  checkboxSelection,
  useSetFilters,
  gridConfig,
  showGridAsTree,
  childField,
  parentField,
  treePathField,
  treePathFieldDelimeter,
  treeColumnField,
  gridDateFormat,
  EnableConsoleLogging,
  useAsyncSetFilters,
  gridWidgetId,
) {
  let enableLogging = EnableConsoleLogging;
  return {
    columnDefs: createColumnDefs(
      dataAsJson,
      showFloatingFilters,
      checkboxSelection,
      useSetFilters,
      useAsyncSetFilters,
      gridConfig.ColumnConfig,
      gridWidgetId,
    ),
    rowData: createRowData(
      dataAsJson,
      gridConfig.RowConfig,
      showGridAsTree,
      childField,
      parentField,
      treeColumnField,
      treePathField,
      treePathFieldDelimeter,
    ),
    undoRedoCellEditing: true,
    undoRedoCellEditingLimit: 2000,
    getDataPath: function (data) {
      return data.agGridTreeGridPath;
    },
    excelStyles: gridConfig.excelStyles ? gridConfig.excelStyles : [],
  };
}
const licenseKey = "";
