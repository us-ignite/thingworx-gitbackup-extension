// AG Grid 3 IDE widget. Decorators come from typescriptwebpacksupport.
import widgetCss from "./widget.css?inline";
const style = document.createElement("style");
style.textContent = widgetCss;
document.head.appendChild(style);
import {
  TWWidgetDefinition,
  autoResizable,
  bindingSource,
  bindingTarget,
  defaultValue,
  description,
  didBind,
  event,
  property,
  selectOptions,
  service,
} from "typescriptwebpacksupport/widgetIDESupport";

@description("AG Grid widget")
@TWWidgetDefinition("AG Grid 2", autoResizable)
class AgGridWidget3 extends TWComposerWidget {
  [key: string]: any;
  constructor(...args: any[]) {
    // The widget factory passes the composer prototype, which the typings omit.
    // @ts-expect-error
    super(...args);
    this.logPrefix = "AgGrid(): ";
    this.loggingEnabled = true;
  }
  log(logMessage) {
    if (this.loggingEnabled) {
      console.log(`AgGridWidget.${this.logPrefix}${logMessage}`);
    }
  }
  dataDidBind() {
    this.logPrefix = "dataDidBind(): ";
    this.log("data bound...");
    var dataShape = JSON.parse(
      JSON.stringify(this.getInfotableMetadataForProperty("Data")),
    );
    this.log(`dataShape = ${JSON.stringify(dataShape)}`);
    var dataNotBoundOrNoDataShape = JSON.stringify(dataShape) == "{}";
    var formattedData = dataNotBoundOrNoDataShape
      ? {
          dataShape: {
            fieldDefinitions: {
              "Column 1": {},
              "Column 2": {},
              "Column 3": {},
              "Column 4": {},
            },
          },
        }
      : {
          dataShape: {
            fieldDefinitions: dataShape,
          },
        };
    this.log(
      `calling buildGrid with formattedData = ${JSON.stringify(formattedData)}`,
    );
    this.buildGrid(formattedData, dataNotBoundOrNoDataShape);
    this.log("done building grid, refreshing widget");
    return true;
  }
  widgetIconUrl() {
    return "data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcKICAgdmlld0JveD0iMCAwIDc3NCA4NzUuNyIKICAgdmVyc2lvbj0iMS4xIgogICBpZD0ic3ZnNzUiCiAgIHNvZGlwb2RpOmRvY25hbWU9Imljb24uc3ZnIgogICBpbmtzY2FwZTp2ZXJzaW9uPSIxLjEuMiAoYjhlMjViZTgzMywgMjAyMi0wMi0wNSkiCiAgIHhtbG5zOmlua3NjYXBlPSJodHRwOi8vd3d3Lmlua3NjYXBlLm9yZy9uYW1lc3BhY2VzL2lua3NjYXBlIgogICB4bWxuczpzb2RpcG9kaT0iaHR0cDovL3NvZGlwb2RpLnNvdXJjZWZvcmdlLm5ldC9EVEQvc29kaXBvZGktMC5kdGQiCiAgIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIKICAgeG1sbnM6c3ZnPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIKICAgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIgogICB4bWxuczpjYz0iaHR0cDovL2NyZWF0aXZlY29tbW9ucy5vcmcvbnMjIgogICB4bWxuczpkYz0iaHR0cDovL3B1cmwub3JnL2RjL2VsZW1lbnRzLzEuMS8iPgogIDxkZWZzCiAgICAgaWQ9ImRlZnM3OSIgLz4KICA8c29kaXBvZGk6bmFtZWR2aWV3CiAgICAgaWQ9Im5hbWVkdmlldzc3IgogICAgIHBhZ2Vjb2xvcj0iI2ZmZmZmZiIKICAgICBib3JkZXJjb2xvcj0iIzY2NjY2NiIKICAgICBib3JkZXJvcGFjaXR5PSIxLjAiCiAgICAgaW5rc2NhcGU6cGFnZXNoYWRvdz0iMiIKICAgICBpbmtzY2FwZTpwYWdlb3BhY2l0eT0iMC4wIgogICAgIGlua3NjYXBlOnBhZ2VjaGVja2VyYm9hcmQ9IjAiCiAgICAgc2hvd2dyaWQ9ImZhbHNlIgogICAgIGlua3NjYXBlOnpvb209IjAuMzIzMzk0MTYiCiAgICAgaW5rc2NhcGU6Y3g9Ii05MS4yMTk5NTMiCiAgICAgaW5rc2NhcGU6Y3k9IjIwOC43MjM2MiIKICAgICBpbmtzY2FwZTp3aW5kb3ctd2lkdGg9IjE5MjAiCiAgICAgaW5rc2NhcGU6d2luZG93LWhlaWdodD0iMTAxMCIKICAgICBpbmtzY2FwZTp3aW5kb3cteD0iLTciCiAgICAgaW5rc2NhcGU6d2luZG93LXk9IjM4IgogICAgIGlua3NjYXBlOndpbmRvdy1tYXhpbWl6ZWQ9IjEiCiAgICAgaW5rc2NhcGU6Y3VycmVudC1sYXllcj0ic3ZnNzUiIC8+CiAgPHRpdGxlCiAgICAgaWQ9InRpdGxlNDUiPmljb248L3RpdGxlPgogIDxnCiAgICAgdHJhbnNmb3JtPSJtYXRyaXgoOS4zODI4OTM1LDAsMCw5Ljc0MDIzMTEsLTk3NzIuMTczNiwtNDgxNy43NzYzKSIKICAgICBpZD0iZzczIj4KICAgIDxwYXRoCiAgICAgICBkPSJtIDEwOTkuNCw1NDkuNCB2IC0xMi41IGggLTIxLjMgbCAtMTIuNSwxMi41IHoiCiAgICAgICBzdHlsZT0iZmlsbDojZmY4YjAwO2ZpbGwtcnVsZTpub256ZXJvIgogICAgICAgaWQ9InBhdGg0NyIgLz4KICAgIDxwYXRoCiAgICAgICBkPSJtIDExMjMuNCw1MTguNCBoIC0yNi43IGwgLTEyLjYsMTIuNSBoIDM5LjMgeiIKICAgICAgIHN0eWxlPSJmaWxsOiM1NWIyYzY7ZmlsbC1ydWxlOm5vbnplcm8iCiAgICAgICBpZD0icGF0aDQ5IiAvPgogICAgPHBhdGgKICAgICAgIGQ9Im0gMTA1My4yLDU2MS45IDYuNCwtNi40IGggMjEuNiBWIDU2OCBoIC0yOCB6IgogICAgICAgc3R5bGU9ImZpbGw6I2ZmMDAwMDtmaWxsLXJ1bGU6bm9uemVybyIKICAgICAgIGlkPSJwYXRoNTEiIC8+CiAgICA8cGF0aAogICAgICAgZD0ibSAxMDU3LjksNTQzLjMgaCAxMy44IGwgMTIuNiwtMTIuNSBoIC0yNi40IHoiCiAgICAgICBzdHlsZT0iZmlsbDojYjRiYmJmO2ZpbGwtcnVsZTpub256ZXJvIgogICAgICAgaWQ9InBhdGg1MyIgLz4KICAgIDxwYXRoCiAgICAgICBkPSJtIDEwNDIuOCw1NjEuOSBoIDEwLjQgbCAxMi40LC0xMi41IGggLTIyLjggeiIKICAgICAgIHN0eWxlPSJmaWxsOiNiNGJiYmY7ZmlsbC1ydWxlOm5vbnplcm8iCiAgICAgICBpZD0icGF0aDU1IiAvPgogICAgPHBhdGgKICAgICAgIGQ9Im0gMTA5Ni43LDUxOC40IC02LjQsNi40IGggLTQwLjggdiAtMTIuNSBoIDQ3LjIgeiIKICAgICAgIHN0eWxlPSJmaWxsOiNiNGJiYmY7ZmlsbC1ydWxlOm5vbnplcm8iCiAgICAgICBpZD0icGF0aDU3IiAvPgogIDwvZz4KICA8bWV0YWRhdGEKICAgICBpZD0ibWV0YWRhdGExMTIzIj4KICAgIDxyZGY6UkRGPgogICAgICA8Y2M6V29yawogICAgICAgICByZGY6YWJvdXQ9IiI+CiAgICAgICAgPGRjOnRpdGxlPmljb248L2RjOnRpdGxlPgogICAgICA8L2NjOldvcms+CiAgICA8L3JkZjpSREY+CiAgPC9tZXRhZGF0YT4KPC9zdmc+Cg==";
  }
  renderHtml() {
    return '<div id="myGrid" class="widget-content" style="width: 100%; height: 100%;"></div>';
  }
  afterRender() {
    this.log(
      `data metadata = ${JSON.stringify(this.getInfotableMetadataForProperty("Data"))}`,
    );
    try {
      if (
        this.isPropertyBoundAsTarget("Data") &&
        JSON.stringify(this.getInfotableMetadataForProperty("Data")) !== "{}"
      ) {
        this.log("data was bound");
        this.dataDidBind();
      } else {
        this.buildGrid(
          {
            dataShape: {
              fieldDefinitions: {
                "Column 1": {},
                "Column 2": {},
                "Column 3": {},
                "Column 4": {},
              },
            },
          },
          true,
        );
      }
    } catch (err) {
      this.log(`error, err = ${err}`);
    }
  }
  beforeDestroy() {
    var currentGrid = document
      .getElementById(this.jqElement[0].id)
      .getElementsByTagName("table");
    if (currentGrid.length) {
      this.log("removing existing table");
      currentGrid[0].remove();
    }
  }
  beforeSetProperty(name, value) {
    this.logPrefix = "beforeSetProperty(): ";
    this.log(`property changed: ${name}`);
    return undefined;
  }
  afterSetProperty(name, value) {
    this.logPrefix = "afterSetProperty(): ";
    this.log(`property changed: ${name}`);
    this.allWidgetProperties();
    return false;
  }
  buildGrid(dataAsJson, dataNotBoundOrNoDataShape) {
    this.logPrefix = "buildGrid(): ";
    this.log(
      `Starting, dataNotBoundOrNoDataShape = ${dataNotBoundOrNoDataShape}`,
    );
    this.log(`dataAsJson = ${JSON.stringify(dataAsJson)}`);
    var container = document.getElementById(this.jqElement[0].id);
    var currentGrid = container.getElementsByTagName("table");
    if (currentGrid.length) {
      this.log("removing existing table");
      currentGrid[0].remove();
    }
    var table = document.createElement("table");
    table.setAttribute("class", "styled-table");
    if (dataNotBoundOrNoDataShape) {
      var noDatacell = table.insertRow().insertCell();
      noDatacell.setAttribute("colspan", "1000");
      var noDataText = document.createTextNode(
        "No data bound or bound infotable has no data shape",
      );
      noDatacell.appendChild(noDataText);
    }
    for (var i = 0; i < 4; i++) {
      var row = table.insertRow();
      for (
        var k = 0;
        k < Object.keys(dataAsJson.dataShape.fieldDefinitions).length;
        k++
      ) {
        var cell = row.insertCell();
        var text = document.createTextNode(`Sample row # ${i} data`);
        cell.appendChild(text);
      }
    }
    var thead = table.createTHead();
    var headRow = thead.insertRow();
    for (var nextFieldName in dataAsJson.dataShape.fieldDefinitions) {
      this.log(`adding next column ${nextFieldName}`);
      var th = document.createElement("th");
      text = document.createTextNode(nextFieldName);
      th.appendChild(text);
      headRow.appendChild(th);
    }
    thead.appendChild(headRow);
    this.log("adding table to container: ");
    console.dir(table);
    container.appendChild(table);
  }
  propertyDidSet(value) {}
  propertyWillSet(value) {}

  @description("The grid source data")
  @property(
    "INFOTABLE",
    bindingTarget,
    didBind("dataDidBind"),
  )
  declare Data: any;
  @description(
    "The filtered grid with any rows remaining after a filter operation. Also keeps rows in order if sorted.",
  )
  @property("INFOTABLE", bindingSource)
  declare FilteredTable: any;
  @description(
    "The entire grid with any updates (not updated if data source changes, only after a grid change detected)",
  )
  @property("INFOTABLE", bindingSource)
  declare UpdatedTable: any;
  @description("The selected rows")
  @property("INFOTABLE", bindingSource)
  declare SelectedRows: any;
  @description(
    'The selected row numbers as a comma-separated string (indexed starting at 1), e.g. "2,3,4,9,11,15"',
  )
  @property("STRING", bindingSource)
  declare SelectedRowNumbers: any;
  @description(
    "The entire grid with any updates (updated if data source changes as well as after a grid change is detected)",
  )
  @property("INFOTABLE", bindingSource)
  declare UpdatedTableOnDataChange: any;
  @description("Just grid rows that have been edited since loaded")
  @property("INFOTABLE", bindingSource)
  declare EditedTable: any;
  @description(
    "The grid as displayed, which will remove hidden columns in the corresponding datashape",
  )
  @property("INFOTABLE", bindingSource)
  declare RenderedTable: any;
  @description(
    'The saved state generated after the "Save State" service is executed as a stringified JSON',
  )
  @property("STRING", bindingSource, bindingTarget)
  declare SavedState: any;
  @description("The row selection choice")
  @property(
    "STRING",
    defaultValue("none"),
    selectOptions([
      {
        text: "None",
        value: "none",
      },
      {
        text: "Single",
        value: "single",
      },
      {
        text: "Multiple",
        value: "multiple",
      },
    ]),
  )
  declare Sorting: any;
  @description(
    "Filters can default to filter by type or by list of all values in infotable",
  )
  @property(
    "STRING",
    defaultValue("bytype"),
    bindingTarget,
    selectOptions([
      {
        text: "By Type",
        value: "type",
      },
      {
        text: "List of Values",
        value: "values",
      },
    ]),
  )
  declare FilterType: any;
  @description(
    "Specify the row selection methods, single, none or multiple rows",
  )
  @property(
    "STRING",
    bindingSource,
    bindingTarget,
    defaultValue("none"),
    selectOptions([
      {
        text: "none",
        value: "none",
      },
      {
        text: "single",
        value: "single",
      },
      {
        text: "multiple",
        value: "multiple",
      },
    ]),
  )
  declare RowSelection: any;
  @description("Enable checkbox selection")
  @property("BOOLEAN", bindingTarget, defaultValue(false))
  declare CheckBoxSelection: any;
  @description("Height of the grid rows")
  @property("INTEGER", bindingTarget, defaultValue(50))
  declare RowHeight: any;
  @description("Enable additional side panels")
  @property(
    "STRING",
    defaultValue("columns"),
    selectOptions([
      {
        text: "None",
        value: "none",
      },
      {
        text: "Columns",
        value: "columns",
      },
      {
        text: "Filters",
        value: "filters",
      },
      {
        text: "Columns and Filters",
        value: "both",
      },
    ]),
  )
  declare SideBarPanels: any;
  @description(
    "Emit double-click event on row double-click. This can be turned off if using the double-click for editing cells.",
  )
  @property("BOOLEAN", bindingTarget, defaultValue(true))
  declare EmitDoubleClickEvent: any;
  @description("Allow column resizing")
  @property("BOOLEAN", bindingTarget, defaultValue(true))
  declare ResizeColumns: any;
  @description("Allow column reordering")
  @property("BOOLEAN", bindingTarget, defaultValue(true))
  declare ReorderColumns: any;
  @description(
    "If editing is enabled, single click to edit, oherwise double-click is used",
  )
  @property("BOOLEAN", bindingTarget, defaultValue(true))
  declare SingleClickEdit: any;
  @description("If editing is allowed edit by cell or entire row")
  @property(
    "STRING",
    defaultValue("no"),
    selectOptions([
      {
        text: "No",
        value: "no",
      },
      {
        text: "Cell",
        value: "cell",
      },
      {
        text: "Row",
        value: "row",
      },
    ]),
  )
  declare AllowEditing: any;
  @description("Show floating filters")
  @property("BOOLEAN", bindingTarget, defaultValue(true))
  declare ShowFloatingFilters: any;
  @description(
    "JSON string with two properties: ColumnConfig and RowConfig. Each property value will be another JSON object for column and row settings. Those objects will contain key/value pairs where the key is the field name and the value is a JSON object which will override the general settings for the grid. Any column/row propery specified in AG Grid column/row properties documentation are accepted.",
  )
  @property("STRING", bindingTarget)
  declare GridConfig: any;
  @description(
    "Quick filter search string. Searches entire grid for string terms in any column.",
  )
  @property("STRING", bindingTarget)
  declare QuickFilterText: any;
  @description("Automatically open side bar on load")
  @property("BOOLEAN", bindingTarget, defaultValue(false))
  declare AutoOpenSideBar: any;
  @description(
    "If any column was defined to use a CustomActionCellRenderer with specified buttons, this property will provide which button was clicked when the ActionCellButtonClicked event is emitted",
  )
  @property("STRING", bindingSource)
  declare ClickedActionCellButton: any;
  @description("True if any row selected")
  @property("BOOLEAN", bindingSource, defaultValue(false))
  declare AnyRowSelected: any;
  @description(
    "A string to be bound to status messages for display in the case one is needed. This message will be updated for an info, warning, or error message. Once generated, one of the three status message events will be emitted.",
  )
  @property("STRING", bindingSource)
  declare StatusMessage: any;
  @description(
    "Target side bar panel to act upon for any side bar panel service",
  )
  @property("STRING", bindingTarget)
  declare TargetSideBarPanel: any;
  @description("True if no rows are selected")
  @property("BOOLEAN", bindingSource, defaultValue(true))
  declare NoRowSelected: any;
  @description(
    "Set to true to display data as a tree grid. Parent and Child fields must be defined",
  )
  @property("BOOLEAN", bindingTarget, defaultValue(false))
  declare ShowGridAsTree: any;
  @description(
    "Name of field to use as the parent id when displaying the grid as a tree",
  )
  @property("STRING", bindingTarget)
  declare ParentField: any;
  @description(
    "Name of field to use as the child id when displaying the grid as a tree",
  )
  @property("STRING", bindingTarget)
  declare ChildField: any;
  @description("Name of field to use as the column to display the tree")
  @property("STRING", bindingTarget)
  declare TreeColumnField: any;
  @description(
    "Name of field to use as the parent/child paths - see wiki for documentation. This is mutally exclusive from ParentField/ChidlField, only use those properties or only this one (and the delimeter if different than the default comma)",
  )
  @property("STRING", bindingTarget)
  declare TreePathField: any;
  @description(
    "String to use as delimeter for the value in TreePathField - see wiki for documentation",
  )
  @property("STRING", bindingTarget, defaultValue(","))
  declare TreePathFieldDelimeter: any;
  @description(
    "Specifies the row numbers to select in advance when the grid is loaded initially. You can type a numeric range such as 1-10, or a comma-separated list such as 2, 4, 5.",
  )
  @property("STRING", bindingTarget)
  declare DefaultSelectedRows: any;
  @description("If true grid resizes with parent")
  @property("BOOLEAN", bindingTarget, defaultValue(true))
  declare ResizeGridWithParent: any;
  @description("If true users can select text within a cell")
  @property("BOOLEAN", bindingTarget, defaultValue(true))
  declare EnableCellTextSelection: any;
  @description("If true users can select a range of cells (like in Excel)")
  @property("BOOLEAN", bindingTarget, defaultValue(true))
  declare EnableRangeSelection: any;
  @description("Just grid rows that have been deleted since loaded")
  @property("INFOTABLE", bindingSource)
  declare DeletedTable: any;
  @description(
    "New rows to add to grid in the form of a stringified JSON array of row objects, one per row. Specify NewRowIndex to insert after a certain row number",
  )
  @property("STRING", bindingTarget)
  declare NewRowData: any;
  @description("index to insert new row(s), default = 0")
  @property("INTEGER", bindingTarget)
  declare NewRowIndex: any;
  @description("Show user-controllable grouping header")
  @property("BOOLEAN", bindingTarget)
  declare EnableRowGrouping: any;
  @description(
    "Day.js format to use to render date values (AdvancedFormat and LocalizedFormat plugins enabled); see https://day.js.org/docs/en/display/format",
  )
  @property("STRING", bindingTarget)
  declare DateFormat: any;
  @description(
    'Specifies the row numbers to select when the service "SelectSpecifiedRows". You can type a numeric range such as 1-10, or a comma-separated list such as 2, 4, 5.',
  )
  @property("STRING", bindingTarget)
  declare RowsToSelect: any;
  @description(
    'Specifies the infotable field to use for row selection. Works in conjunction with the "FieldValueToUseForRowSelection" property and the "SelectRowsByFieldValues" service',
  )
  @property("STRING", bindingTarget)
  declare FieldNameToUseForRowSelection: any;
  @description(
    'Specifies the values to use for row selection. Works in conjunction with the "FieldNameToUseForRowSelection" property and the "SelectRowsByFieldValues" service. This property can be a single value or a comma-separated string of multiple values, but do not use values with commas in it or it will be parsed incorrectly. Also if row selection is set to none or single, then multiple values will not work, only the last value will be selected.',
  )
  @property("STRING", bindingTarget)
  declare FieldValuesToUseForRowSelection: any;
  @description(
    "Specifies the grid theme to use (https://www.ag-grid.com/javascript-data-grid/themes/)",
  )
  @property(
    "STRING",
    bindingTarget,
    defaultValue("ag-theme-alpine"),
    selectOptions([
      {
        text: "ag-theme-alpine",
        value: "ag-theme-alpine",
      },
      {
        text: "ag-theme-balham",
        value: "ag-theme-balham",
      },
      {
        text: "ag-theme-material",
        value: "ag-theme-material",
      },
    ]),
  )
  declare GridTheme: any;
  @description(
    'font size of main grid text in pixels. Default is Custom grid setting, "Main Text (body)"',
  )
  @property("INTEGER", bindingTarget)
  declare GridTextSize: any;
  @description(
    'font size of main grid text in pixels. Default is Custom grid setting, "Title Row Labels"',
  )
  @property("INTEGER", bindingTarget)
  declare GridHeaderSize: any;
  @description(
    "Name of mashup to display in CustomMashupStatusPanel, e.g. ViewManager",
  )
  @property("STRING", bindingTarget)
  declare CustomMashupStatusPanelMashupName: any;
  @description("Server-side row model total row count status panel")
  @property("STRING", bindingTarget)
  declare CustomServerSideStatusPanelRowCount: any;
  @description(
    'Index of grid row to scroll to when service, "ScrollToSpecifiedRow" is executed.',
  )
  @property("STRING", bindingTarget)
  declare ScrollIndex: any;
  @description(
    'Position of row scrolled to to when service, "ScrollToSpecifiedRow" is executed.',
  )
  @property(
    "STRING",
    bindingTarget,
    defaultValue("top"),
    selectOptions([
      {
        text: "top",
        value: "top",
      },
      {
        text: "middle",
        value: "middle",
      },
      {
        text: "bottom",
        value: "bottom",
      },
      {
        text: "default",
        value: "null",
      },
    ]),
  )
  declare ScrollPosition: any;
  @description("Enable console logging")
  @property("BOOLEAN", bindingTarget)
  declare EnableConsoleLogging: any;
  @description(
    "True if first row is selected of current grid (filtered, sorted, whatever)",
  )
  @property("BOOLEAN", bindingSource)
  declare FirstRowSelected: any;
  @description(
    "True if last row is selected of current grid (filtered, sorted, whatever)",
  )
  @property("BOOLEAN", bindingSource)
  declare LastRowSelected: any;
  @description("Removes borders from grid")
  @property("BOOLEAN", bindingTarget, defaultValue(false))
  declare HideBorders: any;
  @description("Relative path to service when using server-side row model")
  @property("STRING", bindingTarget)
  declare ServerSideDataSource: any;
  @description("Infotable column to use for rowid, must be unique")
  @property("STRING", bindingTarget)
  declare RowIdField: any;
  @description("Show row totals when using server-side row model")
  @property("BOOLEAN", bindingTarget)
  declare ShowServerSideRowTotalsStatusPanel: any;
  @description(
    'reindex rows on paste operation. Column to index is defined by property "PasteIndexColumn"',
  )
  @property("BOOLEAN", bindingTarget)
  declare ReindexOnPaste: any;
  @description("Infotable column to use for reindexing on paste")
  @property("STRING", bindingTarget)
  declare PasteIndexColumn: any;
  @description(
    'update the set filters dynamically using the JSON string bound to the property, "SetFiltersJson"',
  )
  @property("BOOLEAN", bindingTarget)
  declare UseAsyncSetFilters: any;
  @description(
    "Relative path to async filter service when using server-side row model and UseAsyncSetFilters is true",
  )
  @property("STRING", bindingTarget)
  declare ServerSideFiltersDataSource: any;
  @description(
    "If the server side data source adds any dynamically-generated columns, add a stingified json array of prefixes to include in the export. Otherwise they will be stripped out of the export download.",
  )
  @property("STRING", bindingTarget)
  declare ServerSideDynamicExportColumnsPrefix: any;
  @description(
    "If using server side data source and extra input params are needed for the server side data source, add or bind them here as a stringified json obect, each entry will be added to the request",
  )
  @property("STRING", bindingTarget)
  declare ServerSideAdditionalRequestParams: any;
  @description(
    "Specify if date editors need to be restricted to a minimum date",
  )
  @property("STRING", bindingTarget)
  declare DateEditorsMinimumDate: any;
  @description(
    "Specify if date editors need to be restricted to a maximum date",
  )
  @property("STRING", bindingTarget)
  declare DateEditorsMaximumDate: any;
  @description("Specify if date editors should show week numbers")
  @property("BOOLEAN", bindingTarget)
  declare DateEditorShowWeekNumbers: any;
  @description("Triggered when a cell is edited")
  @event
  declare CellEdited: any;
  @description("Triggered when the save of the grid state has completed")
  @event
  declare StateSaved: any;
  @description("Triggered when the grid build has completed")
  @event
  declare GridLoaded: any;
  @description("Triggered when a row is double-clicked")
  @event
  declare RowDoubleClicked: any;
  @description("Triggered when a grid filter was changed and applied")
  @event
  declare DataFiltered: any;
  @description(
    'Triggered when a button in a CustomActionCellRenderer was clicked. Use propert "ClickedActionCellButton" to determine which button was clicked',
  )
  @event
  declare ActionCellButtonClicked: any;
  @description(
    'Triggered when an info status message needs to be displayed. Use the "StatusMessage" property for the message source',
  )
  @event
  declare ShowInfoMessage: any;
  @description(
    'Triggered when a warning status message needs to be displayed. Use the "StatusMessage" property for the message source',
  )
  @event
  declare ShowWarningMessage: any;
  @description(
    'Triggered when an error status message needs to be displayed. Use the "StatusMessage" property for the message source',
  )
  @event
  declare ShowErrorMessage: any;
  @description(
    "Triggered when the grid is using the server-side row model and the grid gets updated",
  )
  @event
  declare ServerSideRowModelGridUpdated: any;
  @description("Triggered when there is a change of selected rows")
  @event
  declare SelectedRowsChanged: any;
  @description("Adds a new row")
  @service
  declare AddNewRow: any;
  @description("Adds a new row")
  @service
  declare AddNewRowToBottom: any;
  @description("Deletes the selected rows")
  @service
  declare DeleteSelectedRows: any;
  @description("Saves the current grid state")
  @service
  declare SaveState: any;
  @description("Restores a saved grid state")
  @service
  declare RestoreState: any;
  @description("Sizes columns to fit width of grid container")
  @service
  declare ResizeAll: any;
  @description("Resizes all columns to fit")
  @service
  declare SizeAllColumns: any;
  @description("Clears all filters and resets back to original state")
  @service
  declare ClearAllFilters: any;
  @description(
    "Selects every row that passes the current column, floating and quick filters",
  )
  @service
  declare SelectAllFiltered: any;
  @description("Clears the row selection")
  @service
  declare DeselectAll: any;
  @description(
    "Performs quick search of grid with text from QuickFilterText property",
  )
  @service
  declare QuickFilterSearch: any;
  @description(
    'Opens side bar with ID specified by property "TargetSideBarPanel"',
  )
  @service
  declare OpenSidePanel: any;
  @service
  declare CloseSidePanel: any;
  @description("Redraws grid with updated options")
  @service
  declare RefreshGrid: any;
  @description(
    "If grid has grouped/tree rows, this service will expand all row groups",
  )
  @service
  declare ExpandAllRowGroups: any;
  @description(
    "If grid has grouped/tree rows, this service will collapse all row groups",
  )
  @service
  declare CollapseAllRowGroups: any;
  @description("If grid has a side bar, show it")
  @service
  declare ShowSideBar: any;
  @description("If grid has a side bar, hide it")
  @service
  declare HideSideBar: any;
  @description("Adds new row(s) specified by NewRowData property")
  @service
  declare AddNewRowWithData: any;
  @description("Empties the DeletedTable property")
  @service
  declare ClearDeletedTable: any;
  @description("Restores the DeletedTable rows")
  @service
  declare RestoreDeletedTable: any;
  @description("Selects rows specified by RowsToSelect property")
  @service
  declare SelectSpecifiedRows: any;
  @description(
    "Selects rows specified by the FieldNameToUseForRowSelection and FieldValuesToUseForRowSelection (as a comma-separated string) properties",
  )
  @service
  declare SelectRowsByFieldValues: any;
  @description(
    "Selects rows specified by the FieldNameToUseForRowSelection and FieldValuesToUseForRowSelection (as a string represnting an array of strings) properties",
  )
  @service
  declare SelectRowsByArrayValues: any;
  @description(
    'Scrolls to row index defined in the property "ScrollIndex" and positions the row defined by the property, "ScrollPosition"',
  )
  @service
  declare ScrollToSpecifiedRow: any;
  @description(
    "Selects the row (if the last row is not already selected) after the currently-selected row (display grid, filtered, sorted, whatever)",
  )
  @service
  declare SelectNextRow: any;
  @description(
    "Selects the row before (if the first row is already not selected) the currently-selected row (display grid, filtered, sorted, whatever)",
  )
  @service
  declare SelectPreviousRow: any;
}
