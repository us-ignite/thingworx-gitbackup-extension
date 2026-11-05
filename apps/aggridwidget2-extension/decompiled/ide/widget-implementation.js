// Extracted from webcrack deobfuscated.js for navigation.
// This uses helpers and globals defined elsewhere in the bundle; it is not standalone source.
    (function (_super) {
      function AgGridWidget2() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.logPrefix = "AgGrid(): ";
        _this.loggingEnabled = true;
        _this.log = function (logMessage) {
          if (_this.loggingEnabled) {
            console.log("AgGridWidget." + _this.logPrefix + logMessage);
          }
        };
        _this.dataDidBind = function () {
          _this.logPrefix = "dataDidBind(): ";
          _this.log("data bound...");
          var dataShape = JSON.parse(JSON.stringify(_this.getInfotableMetadataForProperty("Data")));
          _this.log("dataShape = " + JSON.stringify(dataShape));
          var dataNotBoundOrNoDataShape = JSON.stringify(dataShape) == "{}";
          var formattedData = dataNotBoundOrNoDataShape ? {
            dataShape: {
              fieldDefinitions: {
                "Column 1": {},
                "Column 2": {},
                "Column 3": {},
                "Column 4": {}
              }
            }
          } : {
            dataShape: {
              fieldDefinitions: dataShape
            }
          };
          _this.log("calling buildGrid with formattedData = " + JSON.stringify(formattedData));
          _this.buildGrid(formattedData, dataNotBoundOrNoDataShape);
          _this.log("done building grid, refreshing widget");
          return true;
        };
        _this.widgetIconUrl = function () {
          return "data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcKICAgdmlld0JveD0iMCAwIDc3NCA4NzUuNyIKICAgdmVyc2lvbj0iMS4xIgogICBpZD0ic3ZnNzUiCiAgIHNvZGlwb2RpOmRvY25hbWU9Imljb24uc3ZnIgogICBpbmtzY2FwZTp2ZXJzaW9uPSIxLjEuMiAoYjhlMjViZTgzMywgMjAyMi0wMi0wNSkiCiAgIHhtbG5zOmlua3NjYXBlPSJodHRwOi8vd3d3Lmlua3NjYXBlLm9yZy9uYW1lc3BhY2VzL2lua3NjYXBlIgogICB4bWxuczpzb2RpcG9kaT0iaHR0cDovL3NvZGlwb2RpLnNvdXJjZWZvcmdlLm5ldC9EVEQvc29kaXBvZGktMC5kdGQiCiAgIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIKICAgeG1sbnM6c3ZnPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIKICAgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIgogICB4bWxuczpjYz0iaHR0cDovL2NyZWF0aXZlY29tbW9ucy5vcmcvbnMjIgogICB4bWxuczpkYz0iaHR0cDovL3B1cmwub3JnL2RjL2VsZW1lbnRzLzEuMS8iPgogIDxkZWZzCiAgICAgaWQ9ImRlZnM3OSIgLz4KICA8c29kaXBvZGk6bmFtZWR2aWV3CiAgICAgaWQ9Im5hbWVkdmlldzc3IgogICAgIHBhZ2Vjb2xvcj0iI2ZmZmZmZiIKICAgICBib3JkZXJjb2xvcj0iIzY2NjY2NiIKICAgICBib3JkZXJvcGFjaXR5PSIxLjAiCiAgICAgaW5rc2NhcGU6cGFnZXNoYWRvdz0iMiIKICAgICBpbmtzY2FwZTpwYWdlb3BhY2l0eT0iMC4wIgogICAgIGlua3NjYXBlOnBhZ2VjaGVja2VyYm9hcmQ9IjAiCiAgICAgc2hvd2dyaWQ9ImZhbHNlIgogICAgIGlua3NjYXBlOnpvb209IjAuMzIzMzk0MTYiCiAgICAgaW5rc2NhcGU6Y3g9Ii05MS4yMTk5NTMiCiAgICAgaW5rc2NhcGU6Y3k9IjIwOC43MjM2MiIKICAgICBpbmtzY2FwZTp3aW5kb3ctd2lkdGg9IjE5MjAiCiAgICAgaW5rc2NhcGU6d2luZG93LWhlaWdodD0iMTAxMCIKICAgICBpbmtzY2FwZTp3aW5kb3cteD0iLTciCiAgICAgaW5rc2NhcGU6d2luZG93LXk9IjM4IgogICAgIGlua3NjYXBlOndpbmRvdy1tYXhpbWl6ZWQ9IjEiCiAgICAgaW5rc2NhcGU6Y3VycmVudC1sYXllcj0ic3ZnNzUiIC8+CiAgPHRpdGxlCiAgICAgaWQ9InRpdGxlNDUiPmljb248L3RpdGxlPgogIDxnCiAgICAgdHJhbnNmb3JtPSJtYXRyaXgoOS4zODI4OTM1LDAsMCw5Ljc0MDIzMTEsLTk3NzIuMTczNiwtNDgxNy43NzYzKSIKICAgICBpZD0iZzczIj4KICAgIDxwYXRoCiAgICAgICBkPSJtIDEwOTkuNCw1NDkuNCB2IC0xMi41IGggLTIxLjMgbCAtMTIuNSwxMi41IHoiCiAgICAgICBzdHlsZT0iZmlsbDojZmY4YjAwO2ZpbGwtcnVsZTpub256ZXJvIgogICAgICAgaWQ9InBhdGg0NyIgLz4KICAgIDxwYXRoCiAgICAgICBkPSJtIDExMjMuNCw1MTguNCBoIC0yNi43IGwgLTEyLjYsMTIuNSBoIDM5LjMgeiIKICAgICAgIHN0eWxlPSJmaWxsOiM1NWIyYzY7ZmlsbC1ydWxlOm5vbnplcm8iCiAgICAgICBpZD0icGF0aDQ5IiAvPgogICAgPHBhdGgKICAgICAgIGQ9Im0gMTA1My4yLDU2MS45IDYuNCwtNi40IGggMjEuNiBWIDU2OCBoIC0yOCB6IgogICAgICAgc3R5bGU9ImZpbGw6I2ZmMDAwMDtmaWxsLXJ1bGU6bm9uemVybyIKICAgICAgIGlkPSJwYXRoNTEiIC8+CiAgICA8cGF0aAogICAgICAgZD0ibSAxMDU3LjksNTQzLjMgaCAxMy44IGwgMTIuNiwtMTIuNSBoIC0yNi40IHoiCiAgICAgICBzdHlsZT0iZmlsbDojYjRiYmJmO2ZpbGwtcnVsZTpub256ZXJvIgogICAgICAgaWQ9InBhdGg1MyIgLz4KICAgIDxwYXRoCiAgICAgICBkPSJtIDEwNDIuOCw1NjEuOSBoIDEwLjQgbCAxMi40LC0xMi41IGggLTIyLjggeiIKICAgICAgIHN0eWxlPSJmaWxsOiNiNGJiYmY7ZmlsbC1ydWxlOm5vbnplcm8iCiAgICAgICBpZD0icGF0aDU1IiAvPgogICAgPHBhdGgKICAgICAgIGQ9Im0gMTA5Ni43LDUxOC40IC02LjQsNi40IGggLTQwLjggdiAtMTIuNSBoIDQ3LjIgeiIKICAgICAgIHN0eWxlPSJmaWxsOiNiNGJiYmY7ZmlsbC1ydWxlOm5vbnplcm8iCiAgICAgICBpZD0icGF0aDU3IiAvPgogIDwvZz4KICA8bWV0YWRhdGEKICAgICBpZD0ibWV0YWRhdGExMTIzIj4KICAgIDxyZGY6UkRGPgogICAgICA8Y2M6V29yawogICAgICAgICByZGY6YWJvdXQ9IiI+CiAgICAgICAgPGRjOnRpdGxlPmljb248L2RjOnRpdGxlPgogICAgICA8L2NjOldvcms+CiAgICA8L3JkZjpSREY+CiAgPC9tZXRhZGF0YT4KPC9zdmc+Cg==";
        };
        _this.renderHtml = function () {
          return "<div id=\"myGrid\" class=\"widget-content\" style=\"width: 100%; height: 100%;\"></div>";
        };
        _this.afterRender = function () {
          _this.log("data metadata = " + JSON.stringify(_this.getInfotableMetadataForProperty("Data")));
          try {
            if (_this.isPropertyBoundAsTarget("Data") && JSON.stringify(_this.getInfotableMetadataForProperty("Data")) !== "{}") {
              _this.log("data was bound");
              _this.dataDidBind();
            } else {
              _this.buildGrid({
                dataShape: {
                  fieldDefinitions: {
                    "Column 1": {},
                    "Column 2": {},
                    "Column 3": {},
                    "Column 4": {}
                  }
                }
              }, true);
            }
          } catch (err) {
            _this.log("error, err = " + err);
          }
        };
        _this.beforeDestroy = function () {
          var currentGrid = document.getElementById(_this.jqElement[0].id).getElementsByTagName("table");
          if (currentGrid.length) {
            _this.log("removing existing table");
            currentGrid[0].remove();
          }
        };
        _this.beforeSetProperty = function (name, value) {
          _this.logPrefix = "beforeSetProperty(): ";
          _this.log("property changed: " + name);
        };
        _this.afterSetProperty = function (name, value) {
          _this.logPrefix = "afterSetProperty(): ";
          _this.log("property changed: " + name);
          _this.allWidgetProperties();
          return false;
        };
        _this.buildGrid = function (dataAsJson, dataNotBoundOrNoDataShape) {
          _this.logPrefix = "buildGrid(): ";
          _this.log("Starting, dataNotBoundOrNoDataShape = " + dataNotBoundOrNoDataShape);
          _this.log("dataAsJson = " + JSON.stringify(dataAsJson));
          var container = document.getElementById(_this.jqElement[0].id);
          var currentGrid = container.getElementsByTagName("table");
          if (currentGrid.length) {
            _this.log("removing existing table");
            currentGrid[0].remove();
          }
          var table = document.createElement("table");
          table.setAttribute("class", "styled-table");
          if (dataNotBoundOrNoDataShape) {
            var noDatacell = table.insertRow().insertCell();
            noDatacell.setAttribute("colspan", "1000");
            var noDataText = document.createTextNode("No data bound or bound infotable has no data shape");
            noDatacell.appendChild(noDataText);
          }
          for (var i = 0; i < 4; i++) {
            var row = table.insertRow();
            for (var k = 0; k < Object.keys(dataAsJson.dataShape.fieldDefinitions).length; k++) {
              var cell = row.insertCell();
              var text = document.createTextNode("Sample row # " + i + " data");
              cell.appendChild(text);
            }
          }
          var thead = table.createTHead();
          var headRow = thead.insertRow();
          for (var nextFieldName in dataAsJson.dataShape.fieldDefinitions) {
            _this.log("adding next column " + nextFieldName);
            var th = document.createElement("th");
            text = document.createTextNode(nextFieldName);
            th.appendChild(text);
            headRow.appendChild(th);
          }
          thead.appendChild(headRow);
          _this.log("adding table to container: ");
          console.dir(table);
          container.appendChild(table);
        };
        return _this;
      }
      var name;
      __extends(AgGridWidget2, _super);
      AgGridWidget2.prototype.propertyDidSet = function (value) {};
      AgGridWidget2.prototype.propertyWillSet = function (value) {};
      __decorate([description("The grid source data"), property("INFOTABLE", bindingTarget, (name = "dataDidBind", TWPropertyAspect.aspectWithKeyAndValue("_didBind", name)))], AgGridWidget2.prototype, "Data", undefined);
      __decorate([description("The filtered grid with any rows remaining after a filter operation. Also keeps rows in order if sorted."), property("INFOTABLE", bindingSource)], AgGridWidget2.prototype, "FilteredTable", undefined);
      __decorate([description("The entire grid with any updates (not updated if data source changes, only after a grid change detected)"), property("INFOTABLE", bindingSource)], AgGridWidget2.prototype, "UpdatedTable", undefined);
      __decorate([description("The selected rows"), property("INFOTABLE", bindingSource)], AgGridWidget2.prototype, "SelectedRows", undefined);
      __decorate([description("The selected row numbers as a comma-separated string (indexed starting at 1), e.g. \"2,3,4,9,11,15\""), property("STRING", bindingSource)], AgGridWidget2.prototype, "SelectedRowNumbers", undefined);
      __decorate([description("The entire grid with any updates (updated if data source changes as well as after a grid change is detected)"), property("INFOTABLE", bindingSource)], AgGridWidget2.prototype, "UpdatedTableOnDataChange", undefined);
      __decorate([description("Just grid rows that have been edited since loaded"), property("INFOTABLE", bindingSource)], AgGridWidget2.prototype, "EditedTable", undefined);
      __decorate([description("The grid as displayed, which will remove hidden columns in the corresponding datashape"), property("INFOTABLE", bindingSource)], AgGridWidget2.prototype, "RenderedTable", undefined);
      __decorate([description("The saved state generated after the \"Save State\" service is executed as a stringified JSON"), property("STRING", bindingSource, bindingTarget)], AgGridWidget2.prototype, "SavedState", undefined);
      __decorate([description("The row selection choice"), property("STRING", defaultValue("none"), selectOptions([{
        text: "None",
        value: "none"
      }, {
        text: "Single",
        value: "single"
      }, {
        text: "Multiple",
        value: "multiple"
      }]))], AgGridWidget2.prototype, "Sorting", undefined);
      __decorate([description("Filters can default to filter by type or by list of all values in infotable"), property("STRING", defaultValue("bytype"), bindingTarget, selectOptions([{
        text: "By Type",
        value: "type"
      }, {
        text: "List of Values",
        value: "values"
      }]))], AgGridWidget2.prototype, "FilterType", undefined);
      __decorate([description("Specify the row selection methods, single, none or multiple rows"), property("STRING", bindingSource, bindingTarget, defaultValue("none"), selectOptions([{
        text: "none",
        value: "none"
      }, {
        text: "single",
        value: "single"
      }, {
        text: "multiple",
        value: "multiple"
      }]))], AgGridWidget2.prototype, "RowSelection", undefined);
      __decorate([description("Enable checkbox selection"), property("BOOLEAN", bindingTarget, defaultValue(false))], AgGridWidget2.prototype, "CheckBoxSelection", undefined);
      __decorate([description("Height of the grid rows"), property("INTEGER", bindingTarget, defaultValue(50))], AgGridWidget2.prototype, "RowHeight", undefined);
      __decorate([description("Enable additional side panels"), property("STRING", defaultValue("columns"), selectOptions([{
        text: "None",
        value: "none"
      }, {
        text: "Columns",
        value: "columns"
      }, {
        text: "Filters",
        value: "filters"
      }, {
        text: "Columns and Filters",
        value: "both"
      }]))], AgGridWidget2.prototype, "SideBarPanels", undefined);
      __decorate([description("Emit double-click event on row double-click. This can be turned off if using the double-click for editing cells."), property("BOOLEAN", bindingTarget, defaultValue(true))], AgGridWidget2.prototype, "EmitDoubleClickEvent", undefined);
      __decorate([description("Allow column resizing"), property("BOOLEAN", bindingTarget, defaultValue(true))], AgGridWidget2.prototype, "ResizeColumns", undefined);
      __decorate([description("Allow column reordering"), property("BOOLEAN", bindingTarget, defaultValue(true))], AgGridWidget2.prototype, "ReorderColumns", undefined);
      __decorate([description("If editing is enabled, single click to edit, oherwise double-click is used"), property("BOOLEAN", bindingTarget, defaultValue(true))], AgGridWidget2.prototype, "SingleClickEdit", undefined);
      __decorate([description("If editing is allowed edit by cell or entire row"), property("STRING", defaultValue("no"), selectOptions([{
        text: "No",
        value: "no"
      }, {
        text: "Cell",
        value: "cell"
      }, {
        text: "Row",
        value: "row"
      }]))], AgGridWidget2.prototype, "AllowEditing", undefined);
      __decorate([description("Show floating filters"), property("BOOLEAN", bindingTarget, defaultValue(true))], AgGridWidget2.prototype, "ShowFloatingFilters", undefined);
      __decorate([description("JSON string with two properties: ColumnConfig and RowConfig. Each property value will be another JSON object for column and row settings. Those objects will contain key/value pairs where the key is the field name and the value is a JSON object which will override the general settings for the grid. Any column/row propery specified in AG Grid column/row properties documentation are accepted."), property("STRING", bindingTarget)], AgGridWidget2.prototype, "GridConfig", undefined);
      __decorate([description("Quick filter search string. Searches entire grid for string terms in any column."), property("STRING", bindingTarget)], AgGridWidget2.prototype, "QuickFilterText", undefined);
      __decorate([description("Automatically open side bar on load"), property("BOOLEAN", bindingTarget, defaultValue(false))], AgGridWidget2.prototype, "AutoOpenSideBar", undefined);
      __decorate([description("If any column was defined to use a CustomActionCellRenderer with specified buttons, this property will provide which button was clicked when the ActionCellButtonClicked event is emitted"), property("STRING", bindingSource)], AgGridWidget2.prototype, "ClickedActionCellButton", undefined);
      __decorate([description("True if any row selected"), property("BOOLEAN", bindingSource, defaultValue(false))], AgGridWidget2.prototype, "AnyRowSelected", undefined);
      __decorate([description("A string to be bound to status messages for display in the case one is needed. This message will be updated for an info, warning, or error message. Once generated, one of the three status message events will be emitted."), property("STRING", bindingSource)], AgGridWidget2.prototype, "StatusMessage", undefined);
      __decorate([description("Target side bar panel to act upon for any side bar panel service"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "TargetSideBarPanel", undefined);
      __decorate([description("True if no rows are selected"), property("BOOLEAN", bindingSource, defaultValue(true))], AgGridWidget2.prototype, "NoRowSelected", undefined);
      __decorate([description("Set to true to display data as a tree grid. Parent and Child fields must be defined"), property("BOOLEAN", bindingTarget, defaultValue(false))], AgGridWidget2.prototype, "ShowGridAsTree", undefined);
      __decorate([description("Name of field to use as the parent id when displaying the grid as a tree"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "ParentField", undefined);
      __decorate([description("Name of field to use as the child id when displaying the grid as a tree"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "ChildField", undefined);
      __decorate([description("Name of field to use as the column to display the tree"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "TreeColumnField", undefined);
      __decorate([description("Name of field to use as the parent/child paths - see wiki for documentation. This is mutally exclusive from ParentField/ChidlField, only use those properties or only this one (and the delimeter if different than the default comma)"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "TreePathField", undefined);
      __decorate([description("String to use as delimeter for the value in TreePathField - see wiki for documentation"), property("STRING", bindingTarget, defaultValue(","))], AgGridWidget2.prototype, "TreePathFieldDelimeter", undefined);
      __decorate([description("Specifies the row numbers to select in advance when the grid is loaded initially. You can type a numeric range such as 1-10, or a comma-separated list such as 2, 4, 5."), property("STRING", bindingTarget)], AgGridWidget2.prototype, "DefaultSelectedRows", undefined);
      __decorate([description("If true grid resizes with parent"), property("BOOLEAN", bindingTarget, defaultValue(true))], AgGridWidget2.prototype, "ResizeGridWithParent", undefined);
      __decorate([description("If true users can select text within a cell"), property("BOOLEAN", bindingTarget, defaultValue(true))], AgGridWidget2.prototype, "EnableCellTextSelection", undefined);
      __decorate([description("If true users can select a range of cells (like in Excel)"), property("BOOLEAN", bindingTarget, defaultValue(true))], AgGridWidget2.prototype, "EnableRangeSelection", undefined);
      __decorate([description("Just grid rows that have been deleted since loaded"), property("INFOTABLE", bindingSource)], AgGridWidget2.prototype, "DeletedTable", undefined);
      __decorate([description("New rows to add to grid in the form of a stringified JSON array of row objects, one per row. Specify NewRowIndex to insert after a certain row number"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "NewRowData", undefined);
      __decorate([description("index to insert new row(s), default = 0"), property("INTEGER", bindingTarget)], AgGridWidget2.prototype, "NewRowIndex", undefined);
      __decorate([description("Show user-controllable grouping header"), property("BOOLEAN", bindingTarget)], AgGridWidget2.prototype, "EnableRowGrouping", undefined);
      __decorate([description("moment.js format to use to render date values, use format found at https://momentjs.com/docs/#/displaying/"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "DateFormat", undefined);
      __decorate([description("Specifies the row numbers to select when the service \"SelectSpecifiedRows\". You can type a numeric range such as 1-10, or a comma-separated list such as 2, 4, 5."), property("STRING", bindingTarget)], AgGridWidget2.prototype, "RowsToSelect", undefined);
      __decorate([description("Specifies the infotable field to use for row selection. Works in conjunction with the \"FieldValueToUseForRowSelection\" property and the \"SelectRowsByFieldValues\" service"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "FieldNameToUseForRowSelection", undefined);
      __decorate([description("Specifies the values to use for row selection. Works in conjunction with the \"FieldNameToUseForRowSelection\" property and the \"SelectRowsByFieldValues\" service. This property can be a single value or a comma-separated string of multiple values, but do not use values with commas in it or it will be parsed incorrectly. Also if row selection is set to none or single, then multiple values will not work, only the last value will be selected."), property("STRING", bindingTarget)], AgGridWidget2.prototype, "FieldValuesToUseForRowSelection", undefined);
      __decorate([description("Specifies the grid theme to use (https://www.ag-grid.com/javascript-data-grid/themes/)"), property("STRING", bindingTarget, defaultValue("ag-theme-alpine"), selectOptions([{
        text: "ag-theme-alpine",
        value: "ag-theme-alpine"
      }, {
        text: "ag-theme-balham",
        value: "ag-theme-balham"
      }, {
        text: "ag-theme-material",
        value: "ag-theme-material"
      }]))], AgGridWidget2.prototype, "GridTheme", undefined);
      __decorate([description("font size of main grid text in pixels. Default is Custom grid setting, \"Main Text (body)\""), property("INTEGER", bindingTarget)], AgGridWidget2.prototype, "GridTextSize", undefined);
      __decorate([description("font size of main grid text in pixels. Default is Custom grid setting, \"Title Row Labels\""), property("INTEGER", bindingTarget)], AgGridWidget2.prototype, "GridHeaderSize", undefined);
      __decorate([description("Name of mashup to display in CustomMashupStatusPanel, e.g. ViewManager"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "CustomMashupStatusPanelMashupName", undefined);
      __decorate([description("Server-side row model total row count status panel"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "CustomServerSideStatusPanelRowCount", undefined);
      __decorate([description("Index of grid row to scroll to when service, \"ScrollToSpecifiedRow\" is executed."), property("STRING", bindingTarget)], AgGridWidget2.prototype, "ScrollIndex", undefined);
      __decorate([description("Position of row scrolled to to when service, \"ScrollToSpecifiedRow\" is executed."), property("STRING", bindingTarget, defaultValue("top"), selectOptions([{
        text: "top",
        value: "top"
      }, {
        text: "middle",
        value: "middle"
      }, {
        text: "bottom",
        value: "bottom"
      }, {
        text: "default",
        value: "null"
      }]))], AgGridWidget2.prototype, "ScrollPosition", undefined);
      __decorate([description("Enable console logging"), property("BOOLEAN", bindingTarget)], AgGridWidget2.prototype, "EnableConsoleLogging", undefined);
      __decorate([description("True if first row is selected of current grid (filtered, sorted, whatever)"), property("BOOLEAN", bindingSource)], AgGridWidget2.prototype, "FirstRowSelected", undefined);
      __decorate([description("True if last row is selected of current grid (filtered, sorted, whatever)"), property("BOOLEAN", bindingSource)], AgGridWidget2.prototype, "LastRowSelected", undefined);
      __decorate([description("Removes borders from grid"), property("BOOLEAN", bindingTarget, defaultValue(false))], AgGridWidget2.prototype, "HideBorders", undefined);
      __decorate([description("Relative path to service when using server-side row model"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "ServerSideDataSource", undefined);
      __decorate([description("Infotable column to use for rowid, must be unique"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "RowIdField", undefined);
      __decorate([description("Show row totals when using server-side row model"), property("BOOLEAN", bindingTarget)], AgGridWidget2.prototype, "ShowServerSideRowTotalsStatusPanel", undefined);
      __decorate([description("reindex rows on paste operation. Column to index is defined by property \"PasteIndexColumn\""), property("BOOLEAN", bindingTarget)], AgGridWidget2.prototype, "ReindexOnPaste", undefined);
      __decorate([description("Infotable column to use for reindexing on paste"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "PasteIndexColumn", undefined);
      __decorate([description("update the set filters dynamically using the JSON string bound to the property, \"SetFiltersJson\""), property("BOOLEAN", bindingTarget)], AgGridWidget2.prototype, "UseAsyncSetFilters", undefined);
      __decorate([description("Relative path to async filter service when using server-side row model and UseAsyncSetFilters is true"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "ServerSideFiltersDataSource", undefined);
      __decorate([description("If the server side data source adds any dynamically-generated columns, add a stingified json array of prefixes to include in the export. Otherwise they will be stripped out of the export download."), property("STRING", bindingTarget)], AgGridWidget2.prototype, "ServerSideDynamicExportColumnsPrefix", undefined);
      __decorate([description("If using server side data source and extra input params are needed for the server side data source, add or bind them here as a stringified json obect, each entry will be added to the request"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "ServerSideAdditionalRequestParams", undefined);
      __decorate([description("Specify if date editors need to be restricted to a minimum date"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "DateEditorsMinimumDate", undefined);
      __decorate([description("Specify if date editors need to be restricted to a maximum date"), property("STRING", bindingTarget)], AgGridWidget2.prototype, "DateEditorsMaximumDate", undefined);
      __decorate([description("Specify if date editors should show week numbers"), property("BOOLEAN", bindingTarget)], AgGridWidget2.prototype, "DateEditorShowWeekNumbers", undefined);
      __decorate([description("Triggered when a cell is edited"), widgetIDESupport_event], AgGridWidget2.prototype, "CellEdited", undefined);
      __decorate([description("Triggered when the save of the grid state has completed"), widgetIDESupport_event], AgGridWidget2.prototype, "StateSaved", undefined);
      __decorate([description("Triggered when the grid build has completed"), widgetIDESupport_event], AgGridWidget2.prototype, "GridLoaded", undefined);
      __decorate([description("Triggered when a row is double-clicked"), widgetIDESupport_event], AgGridWidget2.prototype, "RowDoubleClicked", undefined);
      __decorate([description("Triggered when a grid filter was changed and applied"), widgetIDESupport_event], AgGridWidget2.prototype, "DataFiltered", undefined);
      __decorate([description("Triggered when a button in a CustomActionCellRenderer was clicked. Use propert \"ClickedActionCellButton\" to determine which button was clicked"), widgetIDESupport_event], AgGridWidget2.prototype, "ActionCellButtonClicked", undefined);
      __decorate([description("Triggered when an info status message needs to be displayed. Use the \"StatusMessage\" property for the message source"), widgetIDESupport_event], AgGridWidget2.prototype, "ShowInfoMessage", undefined);
      __decorate([description("Triggered when a warning status message needs to be displayed. Use the \"StatusMessage\" property for the message source"), widgetIDESupport_event], AgGridWidget2.prototype, "ShowWarningMessage", undefined);
      __decorate([description("Triggered when an error status message needs to be displayed. Use the \"StatusMessage\" property for the message source"), widgetIDESupport_event], AgGridWidget2.prototype, "ShowErrorMessage", undefined);
      __decorate([description("Triggered when the grid is using the server-side row model and the grid gets updated"), widgetIDESupport_event], AgGridWidget2.prototype, "ServerSideRowModelGridUpdated", undefined);
      __decorate([description("Triggered when there is a change of selected rows"), widgetIDESupport_event], AgGridWidget2.prototype, "SelectedRowsChanged", undefined);
      __decorate([description("Adds a new row"), service], AgGridWidget2.prototype, "AddNewRow", undefined);
      __decorate([description("Adds a new row"), service], AgGridWidget2.prototype, "AddNewRowToBottom", undefined);
      __decorate([description("Deletes the selected rows"), service], AgGridWidget2.prototype, "DeleteSelectedRows", undefined);
      __decorate([description("Saves the current grid state"), service], AgGridWidget2.prototype, "SaveState", undefined);
      __decorate([description("Restores a saved grid state"), service], AgGridWidget2.prototype, "RestoreState", undefined);
      __decorate([description("Sizes columns to fit width of grid container"), service], AgGridWidget2.prototype, "ResizeAll", undefined);
      __decorate([description("Resizes all columns to fit"), service], AgGridWidget2.prototype, "SizeAllColumns", undefined);
      __decorate([description("Clears all filters and resets back to original state"), service], AgGridWidget2.prototype, "ClearAllFilters", undefined);
      __decorate([description("Performs quick search of grid with text from QuickFilterText property"), service], AgGridWidget2.prototype, "QuickFilterSearch", undefined);
      __decorate([description("Opens side bar with ID specified by property \"TargetSideBarPanel\""), service], AgGridWidget2.prototype, "OpenSidePanel", undefined);
      __decorate([service], AgGridWidget2.prototype, "CloseSidePanel", undefined);
      __decorate([description("Redraws grid with updated options"), service], AgGridWidget2.prototype, "RefreshGrid", undefined);
      __decorate([description("If grid has grouped/tree rows, this service will expand all row groups"), service], AgGridWidget2.prototype, "ExpandAllRowGroups", undefined);
      __decorate([description("If grid has grouped/tree rows, this service will collapse all row groups"), service], AgGridWidget2.prototype, "CollapseAllRowGroups", undefined);
      __decorate([description("If grid has a side bar, show it"), service], AgGridWidget2.prototype, "ShowSideBar", undefined);
      __decorate([description("If grid has a side bar, hide it"), service], AgGridWidget2.prototype, "HideSideBar", undefined);
      __decorate([description("Adds new row(s) specified by NewRowData property"), service], AgGridWidget2.prototype, "AddNewRowWithData", undefined);
      __decorate([description("Empties the DeletedTable property"), service], AgGridWidget2.prototype, "ClearDeletedTable", undefined);
      __decorate([description("Restores the DeletedTable rows"), service], AgGridWidget2.prototype, "RestoreDeletedTable", undefined);
      __decorate([description("Selects rows specified by RowsToSelect property"), service], AgGridWidget2.prototype, "SelectSpecifiedRows", undefined);
      __decorate([description("Selects rows specified by the FieldNameToUseForRowSelection and FieldValuesToUseForRowSelection (as a comma-separated string) properties"), service], AgGridWidget2.prototype, "SelectRowsByFieldValues", undefined);
      __decorate([description("Selects rows specified by the FieldNameToUseForRowSelection and FieldValuesToUseForRowSelection (as a string represnting an array of strings) properties"), service], AgGridWidget2.prototype, "SelectRowsByArrayValues", undefined);
      __decorate([description("Scrolls to row index defined in the property \"ScrollIndex\" and positions the row defined by the property, \"ScrollPosition\""), service], AgGridWidget2.prototype, "ScrollToSpecifiedRow", undefined);
      __decorate([description("Selects the row (if the last row is not already selected) after the currently-selected row (display grid, filtered, sorted, whatever)"), service], AgGridWidget2.prototype, "SelectNextRow", undefined);
      __decorate([description("Selects the row before (if the first row is already not selected) the currently-selected row (display grid, filtered, sorted, whatever)"), service], AgGridWidget2.prototype, "SelectPreviousRow", undefined);
      AgGridWidget2 = __decorate([description("AG Grid widget"), TWWidgetDefinition("AG Grid 2", autoResizable)], AgGridWidget2);
    })(TWComposerWidget);
