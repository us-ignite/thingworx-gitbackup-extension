// Extracted from webcrack deobfuscated.js for navigation.
// This uses helpers and globals defined elsewhere in the bundle; it is not standalone source.
    var CustomSelectEditor = function () {
      function CustomSelectEditor() {
        var _this = this;
        this.valueToTextMap = {};
        this.loggingEnabled = true;
        this.log = function (logPrefix, logMessage) {
          if (_this.loggingEnabled) {
            console.log("AgGridWidget.CustomSelectEditor()" + logPrefix + logMessage);
          }
        };
        this.updateSelectedItems = function () {
          var logPrefix = ".updateSelectedItems(): ";
          var selectedValues = $(_this.selectElement).multipleSelect("getSelects", "value");
          var selectedTexts = $(_this.selectElement).multipleSelect("getSelects", "text");
          _this.log(logPrefix, "select option changed, selectedTexts = " + selectedTexts.join(" and "));
          var selectedItems = [];
          selectedValues.forEach(function (nextIndex) {
            var nextSelectedText = _this.valueToTextMap[nextIndex];
            selectedItems.push(nextSelectedText);
          });
          _this.log(logPrefix, "select option changed, selectedItems = " + JSON.stringify(selectedItems));
          _this.selectedItem = selectedItems.join(" | ");
          var gridConfig = _this.params.context.getGridConfig().ColumnConfig;
          _this.log(logPrefix, "gridConfig = " + JSON.stringify(gridConfig));
          Object.keys(gridConfig).forEach(function (columnName) {
            if (columnName !== _this.params.column.getId()) {
              _this.log(logPrefix, "checking if other fields need to be updated, columnName = " + columnName);
              var cellEditor = gridConfig[columnName]?.cellEditor;
              var cellEditorParams = gridConfig[columnName]?.cellEditorParams;
              if (CustomSelectEditor.name == cellEditor && (cellEditorParams == null ? undefined : cellEditorParams.drivingField) == _this.params.column.getId()) {
                _this.log(logPrefix, "that field is driven by me");
                var currentValue_1 = _this.params.data[columnName];
                _this.log(logPrefix, "currentValue = " + currentValue_1);
                var columnRelatedItems_1 = cellEditorParams.relatedItems;
                _this.log(logPrefix, "columnRelatedItems = " + JSON.stringify(columnRelatedItems_1));
                var selectedItemIsInDrivingSet = selectedItems.every(function (item) {
                  return columnRelatedItems_1[item].includes(currentValue_1);
                });
                _this.log(logPrefix, "selectedItemIsInDrivingSet = " + selectedItemIsInDrivingSet);
                if (!selectedItemIsInDrivingSet) {
                  _this.params.data[columnName] = undefined;
                }
              }
            }
          });
        };
        this.onKeyDown = function (e) {
          var key = e.key;
          var selectedValue = $(_this.selectElement).multipleSelect("getSelects", "value");
          _this.log(".onKeyDown(): ", "pressed key = " + key + ", selectedValue = " + selectedValue);
          if (!_this.multiple && (key == "ArrowDown" || key == "ArrowUp")) {
            var currentValue = parseInt(selectedValue, 10);
            var newValue = currentValue;
            if (key == "ArrowDown" && currentValue + 1 !== _this.allItems.length) {
              newValue = currentValue + 1;
            }
            if (key == "ArrowUp" && currentValue > 0) {
              newValue = currentValue - 1;
            }
            _this.log(".onKeyDown(): ", "newValue = " + newValue);
            $(_this.selectElement).multipleSelect("setSelects", [newValue]);
          }
        };
        var now = new Date().getTime();
        this.selectId = (now++).toString();
        this.containerId = (now++).toString();
      }
      CustomSelectEditor.prototype.init = function (params) {
        var _this = this;
        var logPrefix = ".init(" + params.column.getId() + "): ";
        this.params = params;
        this.loggingEnabled = this.params.context.EnableConsoleLogging;
        this.relatedItems = params.relatedItems;
        this.drivingField = params.drivingField;
        this.log(logPrefix, "Starting, this.relatedItems = " + JSON.stringify(this.relatedItems));
        this.log(logPrefix, "Starting, this.drivingField = " + this.drivingField);
        this.log(logPrefix, "Starting, driving field selection = " + this.params.data[this.drivingField]);
        this.paramValue = params.value;
        if (typeof this.paramValue == "number") {
          this.paramValue = this.paramValue.toString();
        }
        this.allItems = params.hasOwnProperty("relatedItems") && this.relatedItems.hasOwnProperty(this.params.data[this.drivingField]) ? this.relatedItems[this.params.data[this.drivingField]] : params.values;
        this.multiple = params.multiple;
        this.log(logPrefix, "this.paramValue = " + this.paramValue + " " + typeof this.paramValue);
        this.container = document.createElement("div");
        this.container.classList.add("custom-select-dropdown");
        this.container.setAttribute("id", this.containerId);
        this.container.setAttribute("tabindex", "0");
        this.selectElement = document.createElement("select");
        this.selectElement.setAttribute("id", this.selectId);
        this.selectElement.setAttribute("name", "singleSelect");
        this.selectElement.setAttribute("data-size", "lg");
        if (this.multiple) {
          this.selectElement.setAttribute("multiple", "multiple");
        }
        var selectedItems = typeof this.paramValue == "object" ? [this.paramValue] : this.paramValue ? this.paramValue.split("|").map(function (nextValue) {
          return nextValue.trim();
        }) : [];
        this.log(logPrefix, "selectedItems = " + selectedItems);
        this.allItems.forEach(function (nextOption, index) {
          _this.log(logPrefix, "nextOption = " + nextOption);
          var option = new Option(nextOption, index.toString(), false, selectedItems.includes(nextOption.toString()));
          _this.log(logPrefix, "appending nextOption " + option.outerHTML + ", selected? = " + option.selected);
          _this.selectElement.appendChild(option);
          _this.valueToTextMap[index] = nextOption;
        });
        this.container.addEventListener("keydown", this.onKeyDown);
        this.log(logPrefix, "adding select element to container: " + this.selectElement.innerHTML);
        this.container.appendChild(this.selectElement);
      };
      CustomSelectEditor.prototype.getGui = function () {
        return this.container;
      };
      CustomSelectEditor.prototype.afterGuiAttached = function () {
        var _this = this;
        var logPrefix = ".afterGuiAttached(): ";
        this.log(logPrefix, "converting select to multiple-select");
        var keysOfCellEditorParams = Object.values(["value", "eventKey", "column", "colDef", "node", "data", "rowIndex", "cellStartedEdit", "onKeyDown", "stopEditing", "eGridCell", "parseValue", "formatValue", "api", "context"]);
        this.log(logPrefix, "keysOfCellEditorParams = " + JSON.stringify(keysOfCellEditorParams));
        var multiSelectOptions = {
          isOpen: true,
          onClose: function () {
            _this.params.stopEditing();
          },
          classes: "input",
          classPrefix: "input"
        };
        for (var nextOption in this.params) {
          if (!keysOfCellEditorParams.includes(nextOption)) {
            this.log(logPrefix, "adding nextOption " + nextOption + " : " + this.params[nextOption]);
            multiSelectOptions[nextOption] = this.params[nextOption];
          }
        }
        this.log(logPrefix, "done adding options, multiSelectOptions = " + JSON.stringify(multiSelectOptions));
        $(this.selectElement).multipleSelect(multiSelectOptions);
        this.container.focus();
      };
      CustomSelectEditor.prototype.getValue = function () {
        this.updateSelectedItems();
        this.log(".getValue(): ", "returning " + this.selectedItem);
        return this.selectedItem;
      };
      CustomSelectEditor.prototype.destroy = function () {};
      CustomSelectEditor.prototype.isPopup = function () {
        return true;
      };
      CustomSelectEditor.prototype.getRelatedValues = function (drivingField) {};
      return CustomSelectEditor;
    }();
    var CustomImageRenderer = __webpack_require__(2531);
    var CustomActionCellRenderer = function () {
      function CustomActionCellRenderer() {
        var _this = this;
        this.loggingEnabled = true;
        this.log = function (logPrefix, logMessage) {
          if (_this.loggingEnabled) {
            console.log("AgGridWidget.CustomActionCellRenderer()" + logPrefix + logMessage);
          }
        };
        this.onButtonClick = function (e) {
          _this.log(".onButtonClick(): ", "this.clickedButton: " + e.currentTarget.title);
          _this.clickedButton = e.currentTarget.title;
          _this.params.context.emitCustomActionButtonClick(_this.clickedButton);
        };
      }
      CustomActionCellRenderer.prototype.init = function (params) {
        var _this = this;
        var logPrefix = ".init(" + params.column.getId() + "): ";
        this.params = params;
        this.loggingEnabled = params.context.EnableConsoleLogging;
        this.log(logPrefix, "Starting...");
        var rowHeightPx = (params.context.RowHeight ? params.context.RowHeight - 2 : 23) + "px";
        this.outerDiv = document.createElement("div");
        this.outerDiv.className = "widget-bounding-box nonresponsive iqnox-widget template_IqnoxButtonWidget-GridButton";
        this.outerDiv.style.height = rowHeightPx;
        this.innerDiv = document.createElement("div");
        this.innerDiv.className = "widget-content widget-button-iqnox";
        this.innerDiv.style.height = "100%";
        this.innerDiv.style.display = "flex";
        this.iconButtons = params.icons;
        this.log(logPrefix, "this.iconButtons = " + JSON.stringify(this.iconButtons));
        if (this.iconButtons) {
          this.iconButtons.forEach(function (nextIcon) {
            var buttonElement = document.createElement("button");
            buttonElement.setAttribute("title", nextIcon.hasOwnProperty("title") ? nextIcon.title : "title");
            if (!nextIcon.hasOwnProperty("title")) {
              console.log("AgGridWidget.CustomActionCellRenderer(): Button \"title\" field is NOT set! ClickedActionCellButton = title");
            }
            buttonElement.setAttribute("style", nextIcon.hasOwnProperty("style") ? nextIcon.style : "");
            if (nextIcon.hasOwnProperty("className")) {
              var buttonIcon = undefined;
              if (nextIcon.className.startsWith("fa")) {
                (buttonIcon = document.createElement("i")).className = nextIcon.className;
              } else if (nextIcon.className.startsWith("image")) {
                var buttonParams = nextIcon.className.split(":");
                console.log("AgGridWidget.CustomActionCellRenderer():" + params.data[buttonParams[1]]);
                if (params.data[buttonParams[1]]) {
                  _this.outerDiv.className = "widget-bounding-box nonresponsive iqnox-widget template_IqnoxButtonWidget-GridImageButton";
                  (buttonIcon = document.createElement("img")).src = params.data[buttonParams[1]];
                } else {
                  buttonIcon = document.createElement("span");
                  var className = "material-symbols-outlined";
                  var textName = "info";
                  if (buttonParams[2] && buttonParams[3]) {
                    className = buttonParams[2];
                    textName = buttonParams[3];
                  }
                  (buttonIcon = document.createElement("span")).className = className;
                  buttonIcon.textContent = textName;
                }
                buttonIcon.alt = nextIcon["button-text"];
                buttonIcon.style = "height: 100%;width: auto;";
              } else if (nextIcon.className.startsWith("text")) {
                buttonIcon = document.createElement("span");
                var buttonParam = nextIcon.className.split(":")[1];
                buttonIcon.textContent = params.data[buttonParam];
              } else {
                (buttonIcon = document.createElement("span")).className = nextIcon.className.split(":")[0];
                buttonIcon.textContent = nextIcon.className.split(":")[1];
              }
              buttonElement.appendChild(buttonIcon);
            }
            if (nextIcon.hasOwnProperty("textContent")) {
              var buttonTextArea = document.createElement("span");
              buttonTextArea.className = "button-text";
              buttonTextArea.textContent = nextIcon.textContent ? nextIcon.textContent : "";
              buttonElement.appendChild(buttonTextArea);
            }
            buttonElement.addEventListener("click", function (e) {
              _this.onButtonClick(e);
            });
            _this.innerDiv.appendChild(buttonElement);
          });
        }
        this.outerDiv.appendChild(this.innerDiv);
      };
      CustomActionCellRenderer.prototype.getGui = function () {
        return this.outerDiv;
      };
      return CustomActionCellRenderer;
    }();
    var CustomCheckboxRenderer = function () {
      function CustomCheckboxRenderer() {
        var _this = this;
        this.loggingEnabled = true;
        this.log = function (logMessage) {
          if (_this.loggingEnabled) {
            console.log("AgGridWidget.CustomCheckboxRenderer" + _this.logPrefix + logMessage);
          }
        };
        this.checkedHandler = function (e) {
          _this.logPrefix = ".checkedHandler(): ";
          _this.checkboxState = e.target.checked;
          var colId = _this.params.column.getColId();
          _this.params.node.setDataValue(colId, _this.checkboxState);
          _this.log("checkbox changed, new state = " + _this.checkboxState);
        };
      }
      CustomCheckboxRenderer.prototype.init = function (params) {
        var _this = this;
        this.logPrefix = ".init(" + params.column.getId() + "): ";
        this.params = params;
        this.loggingEnabled = this.params.context.EnableConsoleLogging;
        this.paramValue = params.value;
        var checkboxEditable;
        var colDef = params.colDef;
        if (colDef.hasOwnProperty("editable")) {
          var editableValue = colDef.editable;
          this.log("next column has editable, value = " + editableValue);
          if (editableValue.hasOwnProperty("canEdit")) {
            var drivingField_1 = editableValue.canEdit;
            this.log("next column has canEdit, drivingField = " + drivingField_1);
            checkboxEditable = function () {
              return _this.params.data[drivingField_1];
            };
          } else {
            checkboxEditable = editableValue.hasOwnProperty("useEditableFields") ? function () {
              var fieldIsEditable = true;
              var editableFieldName = _this.params.context.EditableFieldsColumn;
              _this.log("checking to see if this cell is editable using field: " + editableFieldName);
              var editableFields = _this.params.data[editableFieldName] ? JSON.parse(_this.params.data[editableFieldName]) : {};
              _this.log("editableFields = " + JSON.stringify(editableFields));
              _this.log("does it have this column?: = " + colDef.field);
              if (editableFields.hasOwnProperty(colDef.field)) {
                fieldIsEditable = editableFields[colDef.field];
                _this.log("Yes! And it = " + fieldIsEditable);
              }
              return fieldIsEditable;
            } : colDef.editable;
          }
        }
        this.log("Starting, this.paramValue = " + JSON.stringify(this.paramValue) + ", checkboxEditable = " + checkboxEditable);
        this.log("Starting, param checkboxDisabled = " + params.checkboxDisabled);
        this.container = document.createElement("div");
        this.container.style.cssText = "text-align: center; vertical-align: middle;";
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
      };
      CustomCheckboxRenderer.prototype.getGui = function () {
        return this.container;
      };
      CustomCheckboxRenderer.prototype.getValue = function () {
        return this.checkboxState;
      };
      return CustomCheckboxRenderer;
    }();
    function __assign() {
      __assign = Object.assign || function (t) {
        var s;
        for (var i = 1, n = arguments.length; i < n; i++) {
          for (var p in s = arguments[i]) {
            if (Object.prototype.hasOwnProperty.call(s, p)) {
              t[p] = s[p];
            }
          }
        }
        return t;
      };
      return __assign.apply(this, arguments);
    }
    var CustomMultiEditToolPanel = function () {
      function CustomMultiEditToolPanel() {
        var _this = this;
        this.loggingEnabled = false;
        this.columnConfigs = {};
        this.log = function (logPrefix, logMessage) {
          if (_this.loggingEnabled) {
            console.log("AgGridWidget.CustomMultiEditToolPanel()" + logPrefix + logMessage);
          }
        };
        this.logObj = function (logPrefix, logMessage, logObject) {
          if (_this.loggingEnabled) {
            console.log("AgGridWidget.CustomMultiEditToolPanel()" + logPrefix + logMessage);
            console.table(logObject);
          }
        };
      }
      CustomMultiEditToolPanel.prototype.init = function (params) {
        var _this = this;
        this.log(".init(): ", "Starting...");
        this.loggingEnabled = params.context.EnableConsoleLogging;
        var gridConfig = params.context.getGridConfig();
        if (gridConfig.hasOwnProperty("ColumnConfig")) {
          this.columnConfigs = gridConfig.ColumnConfig;
        }
        this.eGui = document.createElement("div");
        this.eGui.className = "multiEdit";
        params.api.addEventListener("modelUpdated", function () {
          _this.eGui.innerHTML = _this.buildEditElems(params);
          var rowSelectedDiv = document.getElementById("rowSelectedDiv");
          if (params.api.getSelectedRows().length === 0) {
            rowSelectedDiv.style.display = "block";
            rowSelectedDiv.style.textAlign = "center";
          }
          params.api.addEventListener("selectionChanged", function () {
            var selectedRowCount = params.api.getSelectedRows().length;
            _this.log(".init(): ", "row selection changed, selectedRowCount = " + selectedRowCount);
            if (selectedRowCount == 0) {
              _this.log(".init(): ", "showing no row message");
              $(rowSelectedDiv).show();
            } else {
              _this.log(".init(): ", "hiding no row message");
              $(rowSelectedDiv).hide();
            }
          });
          var applyButtonElem = document.getElementById("applyMultiEditButton");
          var attributesCheckboxes = document.querySelectorAll("[id^=\"multiEditSelector_\"]");
          attributesCheckboxes.forEach(function (nextEl) {
            _this.log(".init(): ", "setting event listener for next checkbox id: " + nextEl.id);
            var valueInputElem = document.getElementById(nextEl.name);
            nextEl.addEventListener("change", function (e) {
              var updateAttributeChecked = e.target.checked;
              _this.log(".init(): ", "checkboxMassUpdateSelector changed, it now = " + updateAttributeChecked);
              valueInputElem.disabled = !updateAttributeChecked;
            });
            applyButtonElem.disabled = $(attributesCheckboxes).is(":checked");
          });
          document.getElementById("multiSelectAllAttributes").addEventListener("change", function (e) {
            var selectAllChecked = e.target.checked;
            _this.log(".init(): ", "selectAllCheckbox changed, it now = " + selectAllChecked);
            document.querySelectorAll("[id^=\"multiEditSelector_\"]").forEach(function (nextEl) {
              _this.log(".init(): ", "setting next checkbox id: " + nextEl.id);
              nextEl.checked = selectAllChecked;
              nextEl.dispatchEvent(new Event("change"));
            });
          });
          var applyMultiEditButton = document.getElementById("applyMultiEditButton");
          var cancelMultiEditButton = document.getElementById("cancelMultiEditButton");
          applyMultiEditButton.addEventListener("click", function (e) {
            var logPrefix = ".applyChanges(): ";
            _this.log(logPrefix, "applyMultiEditButton clicked...");
            var updatedValues = {};
            document.querySelectorAll("[id^=\"multiEditSelector_\"]").forEach(function (nextEl) {
              if (nextEl.checked) {
                var valueInputElem = document.getElementById(nextEl.name);
                var elementType = valueInputElem.tagName.toLowerCase();
                _this.log(logPrefix, "elementType = " + elementType);
                var newValue = valueInputElem.value;
                if (elementType == "select" && valueInputElem.multiple) {
                  _this.log(logPrefix, "elementType is multiple select");
                  var selectElement = document.getElementById(nextEl.name);
                  newValue = Array.from(selectElement.selectedOptions, function (option) {
                    return option.value;
                  }).join(", ");
                }
                if (valueInputElem.getAttribute("type") === "checkbox") {
                  _this.log(logPrefix, "elementType is checkbox");
                  newValue = valueInputElem.checked;
                }
                _this.log(logPrefix, "setting next field: " + valueInputElem.name + " to " + newValue);
                updatedValues[valueInputElem.name] = newValue;
              }
            });
            var newItem = [updatedValues];
            _this.log(logPrefix, "applying mass update of fields:" + JSON.stringify(newItem));
            var selectedNodeIds = params.api.getSelectedNodes().map(function (nextNode) {
              return nextNode.id;
            });
            _this.log(logPrefix, "selectedNodeIds = " + JSON.stringify(selectedNodeIds));
            var updatedRows = [];
            params.api.forEachNode(function (node) {
              if (selectedNodeIds.includes(node.id)) {
                _this.log(logPrefix, "next node id matches, checking if updated fields are editable");
                var nodeData = node.data;
                _this.logObj(logPrefix, "before update, nodeData = ", nodeData);
                var updatedRowValues = __assign({}, updatedValues);
                for (var nextNodeColumn in updatedValues) {
                  _this.log(logPrefix, "checking if columnConfigs has property: " + nextNodeColumn);
                  if (_this.columnConfigs.hasOwnProperty(nextNodeColumn)) {
                    _this.log(logPrefix, "it does");
                    var nextColumnDef = _this.columnConfigs[nextNodeColumn];
                    if (nextColumnDef.hasOwnProperty("editable")) {
                      var fieldIsEditable = true;
                      var editableValue = nextColumnDef.editable;
                      if (editableValue.hasOwnProperty("canEdit")) {
                        var drivingField = editableValue.canEdit;
                        _this.log(logPrefix, "next column has canEdit, drivingField = " + drivingField);
                        fieldIsEditable = nodeData[drivingField];
                      } else if (editableValue.hasOwnProperty("useEditableFields")) {
                        var editableFieldName = params.context.EditableFieldsColumn;
                        _this.log(logPrefix, "checking to see if this cell is editable using field: " + editableFieldName);
                        var editableFields = nodeData[editableFieldName] ? nodeData[editableFieldName] : {};
                        _this.logObj(logPrefix, "editableFields = ", editableFields);
                        _this.log(logPrefix, "does it have this column?: = " + nextNodeColumn);
                        if (editableFields.hasOwnProperty(nextNodeColumn)) {
                          fieldIsEditable = editableFields[nextNodeColumn];
                          _this.log(logPrefix, "Yes! And it = " + fieldIsEditable);
                        } else {
                          _this.log(logPrefix, "No, leaving editability = true");
                        }
                      } else {
                        fieldIsEditable = nextColumnDef.editable;
                      }
                      _this.log(logPrefix, "So, for column: " + nextNodeColumn + ", fieldIsEditable = " + fieldIsEditable);
                      if (!fieldIsEditable) {
                        _this.log(logPrefix, "Since it is not editable, removing it from the update for this row");
                        delete updatedRowValues[nextNodeColumn];
                      }
                    }
                  } else {
                    _this.log(logPrefix, "it does not");
                  }
                }
                Object.assign(nodeData, updatedRowValues);
                _this.log(logPrefix, "after update, nodeData = " + JSON.stringify(nodeData));
                node.setData(nodeData);
                updatedRows.push(node.data);
              }
            });
            _this.log(logPrefix, "done updating rows, updatedRows = " + JSON.stringify(updatedRows));
            params.context.updateEditedTable(updatedRows);
            params.api.closeToolPanel();
          });
          cancelMultiEditButton.addEventListener("click", function (e) {
            _this.log(".init(): ", "cancelMultiEditButton clicked...");
            params.api.closeToolPanel();
          });
        });
      };
      CustomMultiEditToolPanel.prototype.getGui = function () {
        return this.eGui;
      };
      CustomMultiEditToolPanel.prototype.refresh = function () {};
      CustomMultiEditToolPanel.prototype.buildEditElems = function (params) {
        var _this = this;
        var logPrefix = ".buildEditElems(): ";
        this.log(logPrefix, "Starting...");
        var fieldsToDisplay = params.fieldsToDisplay;
        var fieldDefinitions = params.fieldDefinitions;
        var gridConfig = params.context.getGridConfig();
        this.log(logPrefix, "fieldsToDisplay = " + JSON.stringify(fieldsToDisplay));
        var childrenFields = {};
        for (var nextColumnDef in gridConfig.ColumnConfig) {
          var nextColumn = gridConfig.ColumnConfig[nextColumnDef];
          if (nextColumn.hasOwnProperty("children")) {
            nextColumn.children.forEach(function (nextChild) {
              childrenFields[nextChild.field] = nextChild;
            });
          }
        }
        this.log(logPrefix, "childrenFields = " + JSON.stringify(childrenFields));
        var returnDiv = document.createElement("div");
        var headerSelectAllDiv = document.createElement("div");
        var headerLabelDiv = document.createElement("div");
        var headerButtonDiv = document.createElement("div");
        headerSelectAllDiv.className = "center";
        headerLabelDiv.className = "center";
        headerButtonDiv.className = "center";
        var headerLabel = document.createElement("h3");
        headerLabel.innerHTML = "Select attributes to update:<br/>";
        headerLabelDiv.appendChild(headerLabel);
        var selectAllCheckbox = document.createElement("input");
        selectAllCheckbox.type = "checkbox";
        selectAllCheckbox.id = "multiSelectAllAttributes";
        selectAllCheckbox.className = "multiEditAttributesSelectors";
        headerSelectAllDiv.appendChild(selectAllCheckbox);
        var selectAllLabel = document.createElement("label");
        selectAllLabel.innerHTML = "Select All";
        headerSelectAllDiv.appendChild(selectAllLabel);
        var applyButton = document.createElement("button");
        applyButton.id = "applyMultiEditButton";
        applyButton.className = "ptc-ok-button";
        applyButton.title = "Apply Changes";
        applyButton.disabled = true;
        var cancelButton = document.createElement("button");
        cancelButton.id = "cancelMultiEditButton";
        cancelButton.className = "ptc-close-button";
        cancelButton.title = "Cancel";
        headerButtonDiv.appendChild(applyButton);
        headerButtonDiv.appendChild(cancelButton);
        returnDiv.appendChild(headerLabelDiv);
        returnDiv.appendChild(headerButtonDiv);
        returnDiv.appendChild(headerSelectAllDiv);
        var horizRule = document.createElement("hr");
        horizRule.className = "editorDivider";
        returnDiv.appendChild(horizRule);
        var allAttributesDiv = document.createElement("div");
        var rowSelectedDiv = document.createElement("div");
        rowSelectedDiv.id = "rowSelectedDiv";
        rowSelectedDiv.style.textAlign = "center";
        rowSelectedDiv.style.display = "none";
        var noRowSelectedMessage = document.createElement("h6");
        noRowSelectedMessage.id = "noRowSelectedMessage";
        noRowSelectedMessage.style.color = "red";
        noRowSelectedMessage.innerHTML = "Please select at least one row in the grid for which to apply changes";
        rowSelectedDiv.appendChild(noRowSelectedMessage);
        allAttributesDiv.appendChild(rowSelectedDiv);
        Object.keys(fieldsToDisplay).forEach(function (nextField) {
          _this.log(logPrefix, `building edit element for ${nextField}`);
          var nextFieldRowDiv = document.createElement("div");
          nextFieldRowDiv.className = "newUserFieldRow";
          var nextFieldEnableCheckboxDiv = document.createElement("div");
          nextFieldEnableCheckboxDiv.className = "multiEditEnableCheckbox";
          var checkboxMassUpdateSelector = document.createElement("input");
          checkboxMassUpdateSelector.type = "checkbox";
          checkboxMassUpdateSelector.id = "multiEditSelector_" + nextField;
          checkboxMassUpdateSelector.name = "multiEdit_" + nextField;
          checkboxMassUpdateSelector.className = "multiEditAttributesSelectors";
          nextFieldEnableCheckboxDiv.appendChild(checkboxMassUpdateSelector);
          nextFieldRowDiv.appendChild(nextFieldEnableCheckboxDiv);
          var nextFieldLabelDiv = document.createElement("div");
          nextFieldLabelDiv.className = "newUserField";
          var nextFieldLabel = document.createElement("label");
          nextFieldLabel.innerHTML = fieldsToDisplay[nextField];
          nextFieldLabel.className = "multiEditAttributesLabel";
          nextFieldLabelDiv.appendChild(nextFieldLabel);
          nextFieldRowDiv.appendChild(nextFieldLabelDiv);
          var nextFieldCellEditor;
          var attributeValueDiv = document.createElement("div");
          attributeValueDiv.className = "multiEditAttributes";
          var nextFieldColumnConfig = gridConfig.ColumnConfig.hasOwnProperty(nextField) ? gridConfig.ColumnConfig[nextField] : childrenFields.hasOwnProperty(nextField) ? childrenFields[nextField] : undefined;
          _this.log(logPrefix, `nextFieldColumnConfig = ${JSON.stringify(nextFieldColumnConfig)}`);
          if (nextFieldColumnConfig && nextFieldColumnConfig.hasOwnProperty("cellEditor")) {
            nextFieldCellEditor = nextFieldColumnConfig.cellEditor;
          }
          if (nextFieldColumnConfig && nextFieldColumnConfig.hasOwnProperty("cellRenderer") && nextFieldColumnConfig.cellRenderer == "CustomCheckboxRenderer") {
            nextFieldCellEditor = "CustomCheckboxRenderer";
          }
          if (nextFieldCellEditor) {
            _this.log(logPrefix, `it has a cellEditor of ${nextFieldCellEditor}`);
            switch (nextFieldCellEditor) {
              case "CustomSelectEditor":
                var selectOptions = nextFieldColumnConfig.cellEditorParams.values;
                var selectMultiple = !!nextFieldColumnConfig.cellEditorParams.hasOwnProperty("multiple") && nextFieldColumnConfig.cellEditorParams.multiple;
                var attributeSelectElem_1 = document.createElement("select");
                attributeSelectElem_1.disabled = true;
                var selectHeight_1 = 0;
                if (selectOptions) {
                  selectOptions.forEach(function (nextOption) {
                    _this.log(logPrefix, `adding next select option of ${nextOption}`);
                    var option = document.createElement("option");
                    option.value = nextOption;
                    option.text = nextOption;
                    attributeSelectElem_1.appendChild(option);
                    selectHeight_1++;
                  });
                }
                _this.log(logPrefix, `is select elememnt multiple? = ${selectMultiple}`);
                attributeSelectElem_1.id = "multiEdit_" + nextField;
                attributeSelectElem_1.name = nextField;
                attributeSelectElem_1.multiple = selectMultiple;
                attributeSelectElem_1.size = selectMultiple ? selectHeight_1 : 0;
                attributeValueDiv.appendChild(attributeSelectElem_1);
                attributeSelectElem_1;
                break;
              case "CustomCheckboxRenderer":
                var checkboxInputElem = document.createElement("input");
                checkboxInputElem.disabled = true;
                checkboxInputElem.type = "checkbox";
                checkboxInputElem.id = "multiEdit_" + nextField;
                checkboxInputElem.name = nextField;
                checkboxInputElem.setAttribute("style", "multiEditCheckbox");
                attributeValueDiv.appendChild(checkboxInputElem);
                checkboxInputElem;
                break;
              default:
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
                var dateInput = document.createElement("input");
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
                var textInput;
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
      };
      return CustomMultiEditToolPanel;
    }();
    var index_es = __webpack_require__(4262);
    var fontawesome_free_solid_index_es = __webpack_require__(1727);
    var FileUploader = __webpack_require__(2982);
    var CustomNewRowToolPanel = function () {
      function CustomNewRowToolPanel() {
        var _this = this;
        this.loggingEnabled = true;
        this.NOW = new Date().getTime() + "";
        this.imgIds = {};
        this.fileUploader = new FileUploader.l();
        this.buttonSpinnerElement = index_es.Ay.icon(fontawesome_free_solid_index_es.z1G, {
          classes: ["fa-spin"]
        }).node[0];
        this.log = function (logPrefix, logMessage) {
          if (_this.loggingEnabled) {
            console.log("AgGridWidget.CustomNewRowToolPanel()" + logPrefix + logMessage);
          }
        };
        this.startUpload = async (e, containerId, imgId) => {
          var logPrefix = ".startUpload(" + this.NOW + "): ";
          this.log(logPrefix, logPrefix + "getting containerElement with id = " + containerId);
          this.log(logPrefix, logPrefix + "getting imageelement with id = " + imgId);
          var containerElement = document.getElementById(containerId);
          Array.from(containerElement.children).forEach(nextNode => {
            this.log(logPrefix, logPrefix + "nextNode " + nextNode.id + " = " + nextNode.innerHTML);
          });
          var imgElement = document.getElementById(imgId);
          containerElement.replaceChild(this.buttonSpinnerElement, imgElement);
          try {
            var newFilePath = await this.fileUploader.startUpload(e, this.params, imgId);
            this.log(logPrefix, logPrefix + "upload complete");
            var colId = this.params.column.getColId();
            this.params.node.setDataValue(colId, newFilePath);
            imgElement.src = newFilePath;
            containerElement.replaceChild(imgElement, this.buttonSpinnerElement);
            this.log(logPrefix, logPrefix + "renderer update done");
          } catch (error) {
            this.log(logPrefix, logPrefix + "failure uploading...");
            containerElement.replaceChild(imgElement, this.buttonSpinnerElement);
          }
        };
        this.openFileInput = function (e, fileInputId) {
          _this.log(".openFileInput(): ", ".openFileInput(): button clicked, opening file input element with id: " + fileInputId);
          var fileInputElement = document.getElementById(fileInputId);
          fileInputElement.dispatchEvent(new Event("click"));
          fileInputElement.click();
        };
      }
      CustomNewRowToolPanel.prototype.init = function (params) {
        var _this = this;
        this.log(".init(): ", "Starting...");
        this.params = params;
        this.loggingEnabled = this.params.context.EnableConsoleLogging;
        this.params.allow = params.hasOwnProperty("allow") ? params.allow : "/*";
        this.params.uploadThumbnail = !!params.hasOwnProperty("uploadThumbnail") && params.uploadThumbnail;
        this.params.repository = params.hasOwnProperty("repository") ? params.repository : "SystemRepository";
        this.params.newFilePath = params.hasOwnProperty("newFilePath") ? params.newFilePath : "/Ideas";
        this.eGui = document.createElement("div");
        this.eGui.setAttribute("class", "multiEdit");
        params.api.addEventListener("modelUpdated", function () {
          _this.eGui.innerHTML = _this.buildEditElems();
          var fieldsRequired = params.fieldsRequired;
          var fieldDefaults = params.fieldDefaults;
          var newRowSaveButton = document.getElementById("newRowSaveButton" + _this.NOW);
          var newRowCancelButton = document.getElementById("newRowCancelButton" + _this.NOW);
          newRowSaveButton.addEventListener("click", function (e) {
            _this.log(".init(): ", "newRowSaveButton clicked...");
            var missingRequiredFieldNames = [];
            var missingRequiredFields = false;
            var newRow = {};
            var gridIds = [];
            params.api.forEachNode(function (nextNode) {
              return gridIds.push(parseInt(nextNode.id));
            });
            newRow[grid.ME] = Math.max.apply(Math, gridIds) + 1;
            _this.log(".init(): ", "current grid ids: " + gridIds + ", new row id = " + newRow[grid.ME]);
            document.querySelectorAll("[id^=\"newRowField_\"]").forEach(function (nextEl) {
              _this.log(".init(): ", "next field: " + nextEl.name + " has a type of " + nextEl.type);
              _this.log(".init(): ", "setting next field: " + nextEl.name + " to " + (nextEl.type == "checkbox" ? nextEl.checked : nextEl.value));
              if (!!fieldsRequired.includes(nextEl.name) && (!nextEl.value || nextEl.value.trim() == "")) {
                missingRequiredFields = true;
                missingRequiredFieldNames.push(nextEl.name);
              }
              newRow[nextEl.name] = nextEl.type == "checkbox" ? nextEl.checked : nextEl.value;
            });
            if (missingRequiredFields) {
              var alertMessage = "Please provide a value for all the following required fields: " + missingRequiredFieldNames.join("; ");
              params.context.emitStatusMessage("error", alertMessage);
            } else {
              _this.log(".init(): ", "newRow = " + JSON.stringify(newRow) + ", applying any defaults");
              if (fieldDefaults) {
                Object.keys(fieldDefaults).forEach(function (nextDefault) {
                  if (!newRow.hasOwnProperty(nextDefault) || newRow.hasOwnProperty(nextDefault) && !newRow[nextDefault]) {
                    _this.log(".init(): ", "adding nextDefault " + nextDefault + " of: " + fieldDefaults[nextDefault]);
                    newRow[nextDefault] = fieldDefaults[nextDefault];
                  }
                });
                _this.log(".init(): ", "after applying defaults, newRow = " + JSON.stringify(newRow));
              }
              params.api.applyTransaction({
                add: [newRow],
                addIndex: 0
              });
              params.context.updateEditedTable([newRow]);
              params.api.closeToolPanel();
            }
          });
          newRowCancelButton.addEventListener("click", function (e) {
            _this.log(".init(): ", "newRowCancelButton clicked...");
            params.api.closeToolPanel();
            document.querySelectorAll("[id^=\"newRowField_\"]").forEach(function (nextEl) {
              nextEl.value = "";
            });
          });
          Object.keys(_this.imgIds).forEach(function (nextField) {
            _this.log(".init(): ", ".init(): adding event listeners for next image field: " + nextField);
            var nextImagesIds = _this.imgIds[nextField];
            var buttonElem = document.getElementById(nextImagesIds.buttonId);
            var fileInputElem = document.getElementById(nextImagesIds.fileInputId);
            buttonElem.addEventListener("click", function (e) {
              return _this.openFileInput(e, nextImagesIds.fileInputId);
            });
            fileInputElem.addEventListener("change", function (e) {
              return _this.startUpload(e, nextImagesIds.containerId, nextImagesIds.imgId);
            });
          });
        });
      };
      CustomNewRowToolPanel.prototype.getGui = function () {
        return this.eGui;
      };
      CustomNewRowToolPanel.prototype.refresh = function () {};
      CustomNewRowToolPanel.prototype.buildEditElems = function () {
        var _this = this;
        var logPrefix = ".buildEditElems(): ";
        this.log(logPrefix, "Starting...");
        var now = new Date().getTime();
        var fieldsToDisplay = this.params.fieldsToDisplay;
        var fieldDefinitions = this.params.fieldDefinitions;
        var fieldsRequired = this.params.fieldsRequired;
        var panelTitle = this.params.panelTitle;
        var gridConfig = this.params.context.getGridConfig();
        this.log(logPrefix, "fieldsToDisplay = " + JSON.stringify(fieldsToDisplay));
        var returnDiv = document.createElement("div");
        var headerLabelDiv = document.createElement("div");
        headerLabelDiv.className = "center";
        var headerButtonDiv = document.createElement("div");
        headerButtonDiv.className = "center";
        var headerLabel = document.createElement("h3");
        headerLabel.innerHTML = panelTitle || "New row values:";
        headerLabelDiv.appendChild(headerLabel);
        var applyButton = document.createElement("button");
        applyButton.id = "newRowSaveButton" + this.NOW;
        applyButton.className = "ptc-ok-button";
        applyButton.title = "Save new Row";
        var cancelButton = document.createElement("button");
        cancelButton.id = "newRowCancelButton" + this.NOW;
        cancelButton.className = "ptc-close-button";
        cancelButton.title = "Cancel";
        headerButtonDiv.appendChild(applyButton);
        headerButtonDiv.appendChild(cancelButton);
        returnDiv.appendChild(headerLabelDiv);
        returnDiv.appendChild(headerButtonDiv);
        var horizRule = document.createElement("hr");
        horizRule.className = "editorDivider";
        returnDiv.appendChild(horizRule);
        var allAttributesDiv = document.createElement("div");
        Object.keys(fieldsToDisplay).forEach(function (nextField) {
          _this.log(logPrefix, `building edit element for ${nextField}`);
          var inputId = (now++).toString() + Math.floor(100000 + Math.random() * 900000).toString().substring(0, 4);
          var buttonId = (now++).toString() + Math.floor(100000 + Math.random() * 900000).toString().substring(0, 4);
          var imgId = (now++).toString() + Math.floor(100000 + Math.random() * 900000).toString().substring(0, 4);
          var nextFieldDivId = (now++).toString() + Math.floor(100000 + Math.random() * 900000).toString().substring(0, 4);
          var nextFieldRowDiv = document.createElement("div");
          nextFieldRowDiv.setAttribute("class", "newUserFieldRow");
          nextFieldRowDiv.setAttribute("id", nextFieldDivId);
          var nextFieldLabelDiv = document.createElement("div");
          nextFieldLabelDiv.className = "newUserField";
          var nextFieldLabel = document.createElement("label");
          nextFieldLabel.innerHTML = (fieldsRequired.includes(nextField) ? "<span style=\"color: red;\">* </span>" : "") + fieldsToDisplay[nextField];
          nextFieldLabelDiv.appendChild(nextFieldLabel);
          nextFieldRowDiv.appendChild(nextFieldLabelDiv);
          var nextFieldInputDiv = document.createElement("div");
          nextFieldInputDiv.className = "newUserValue";
          var nextFieldCellEditor;
          var nextFieldColumnConfig = gridConfig.ColumnConfig[nextField];
          _this.log(logPrefix, `nextFieldColumnConfig = ${JSON.stringify(nextFieldColumnConfig)}`);
          if (nextFieldColumnConfig && nextFieldColumnConfig.hasOwnProperty("cellEditor")) {
            nextFieldCellEditor = nextFieldColumnConfig.cellEditor;
          }
          if (nextFieldColumnConfig && nextFieldColumnConfig.hasOwnProperty("cellRenderer") && (nextFieldColumnConfig.cellRenderer == "CustomCheckboxRenderer" || nextFieldColumnConfig.cellRenderer == "CustomImageRenderer")) {
            _this.log(logPrefix, `nextFieldColumnConfig is a checkbox or image renderer!: ${nextFieldColumnConfig.cellRenderer}`);
            nextFieldCellEditor = nextFieldColumnConfig.cellRenderer;
          }
          if (nextFieldCellEditor) {
            _this.log(logPrefix, `it has a cellEditor of ${nextFieldCellEditor}`);
            switch (nextFieldCellEditor) {
              case "CustomSelectEditor":
                var selectOptions = nextFieldColumnConfig.cellEditorParams.values;
                var selectMultiple = !!nextFieldColumnConfig.cellEditorParams.hasOwnProperty("multiple") && nextFieldColumnConfig.cellEditorParams.multiple;
                var attributeSelectElem_1 = document.createElement("select");
                attributeSelectElem_1.className = "vertical-middle";
                var selectHeight_1 = 0;
                var option = document.createElement("option");
                option.value = "";
                option.text = "";
                attributeSelectElem_1.appendChild(option);
                selectHeight_1++;
                selectOptions.forEach(function (nextOption) {
                  _this.log(logPrefix, `adding next select option of ${nextOption}`);
                  var option = document.createElement("option");
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
                var inputDiv = document.createElement("div");
                inputDiv.classList.add("file-input");
                var fileButtonElement = document.createElement("btn");
                fileButtonElement.setAttribute("id", buttonId);
                fileButtonElement.setAttribute("name", "fileUploadButton");
                fileButtonElement.classList.add("button-13");
                fileButtonElement.textContent = "Upload";
                var fileInputElement = document.createElement("input");
                fileInputElement.setAttribute("id", inputId);
                fileInputElement.setAttribute("name", "fileUploadInput");
                fileInputElement.setAttribute("type", "file");
                fileInputElement.setAttribute("allow", nextFieldColumnConfig.cellRendererParams && nextFieldColumnConfig.cellRendererParams.hasOwnProperty("allow") ? nextFieldColumnConfig.cellRendererParams.allow : "/*");
                inputDiv.appendChild(fileInputElement);
                var imageElement = document.createElement("img");
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
                  imgId: "newRowField_" + imgId
                };
                break;
              case "CustomCheckboxRenderer":
                var checkboxInputElem = document.createElement("input");
                checkboxInputElem.className = "vertical-middle";
                checkboxInputElem.type = "checkbox";
                checkboxInputElem.id = "newRowField_" + nextField;
                checkboxInputElem.name = nextField;
                nextFieldInputDiv.appendChild(checkboxInputElem);
                break;
              default:
                (textInput = document.createElement("input")).className = "vertical-middle";
                textInput.type = "text";
                textInput.id = "newRowField_" + nextField;
                textInput.name = nextField;
                nextFieldInputDiv.appendChild(textInput);
            }
            nextFieldRowDiv.appendChild(nextFieldInputDiv);
          } else if (fieldDefinitions.hasOwnProperty(nextField)) {
            switch (fieldDefinitions[nextField].baseType) {
              case "DATETIME":
                var dateInput = document.createElement("input");
                dateInput.className = "vertical-middle";
                dateInput.type = "date";
                dateInput.id = "newRowField_" + nextField;
                dateInput.name = nextField;
                nextFieldInputDiv.appendChild(dateInput);
                break;
              case "IMAGELINK":
                break;
              default:
                var textInput;
                (textInput = document.createElement("input")).className = "vertical-middle";
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
      };
      return CustomNewRowToolPanel;
    }();
    var CustomPasswordEditor = function () {
      function CustomPasswordEditor() {
        var _this = this;
        this.loggingEnabled = true;
        this.log = function (logMessage) {
          if (_this.loggingEnabled) {
            console.log("AgGridWidget.CustomPasswordEditor" + _this.logPrefix + logMessage);
          }
        };
        var now = new Date().getTime();
        this.containerId = (now++).toString();
      }
      CustomPasswordEditor.prototype.init = function (params) {
        var _this = this;
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
      };
      CustomPasswordEditor.prototype.getGui = function () {
        return this.container;
      };
      CustomPasswordEditor.prototype.afterGuiAttached = function () {
        this.input.focus();
      };
      CustomPasswordEditor.prototype.getValue = function () {
        return this.paramValue;
      };
      CustomPasswordEditor.prototype.destroy = function () {};
      CustomPasswordEditor.prototype.isPopup = function () {
        return false;
      };
      return CustomPasswordEditor;
    }();
    var CustomPasswordRenderer = function () {
      function CustomPasswordRenderer() {
        var _this = this;
        this.loggingEnabled = true;
        this.log = function (logMessage) {
          if (_this.loggingEnabled) {
            console.log("AgGridWidget.CustomPasswordRenderer" + _this.logPrefix + logMessage);
          }
        };
        var now = new Date().getTime();
        this.containerId = (now++).toString();
      }
      CustomPasswordRenderer.prototype.init = function (params) {
        this.logPrefix = ".init(" + params.column.getId() + "): ";
        this.log("Starting...");
        this.loggingEnabled = params.context.EnableConsoleLogging;
        this.paramValue = params.value;
        this.container = document.createElement("div");
        this.input = document.createElement("p");
        this.input.className = "ag-cell-value";
        this.input.innerHTML = this.paramValue ? "*".repeat(this.paramValue.length) : "";
        this.container.appendChild(this.input);
      };
      CustomPasswordRenderer.prototype.getGui = function () {
        return this.container;
      };
      return CustomPasswordRenderer;
    }();
    var CustomIconCellRenderer = function () {
      function CustomIconCellRenderer() {
        var _this = this;
        this.loggingEnabled = true;
        this.log = function (logMessage) {
          if (_this.loggingEnabled) {
            console.log("AgGridWidget.CustomIconCellRenderer" + _this.logPrefix + logMessage);
          }
        };
      }
      CustomIconCellRenderer.prototype.init = function (params) {
        this.logPrefix = ".init(" + params.column.getId() + "): ";
        this.params = params;
        this.paramValue = params.value;
        this.loggingEnabled = this.params.context.EnableConsoleLogging;
        this.log("Starting, this.paramValue = " + JSON.stringify(this.paramValue));
        var stateField = this.params.stateField;
        var stateFieldValue = this.params.data[stateField];
        var iconMap = this.params.iconMap;
        var suffixImageField = this.params.suffixImage;
        var suffixMap = this.params.suffixMap;
        var suffixImageFieldValue = this.params.data[suffixImageField];
        var rowHeight = this.params.context.RowHeight;
        this.log("stateField = " + stateField + " = " + stateFieldValue);
        this.log("iconMap = " + JSON.stringify(iconMap));
        this.log("suffixImageField = " + suffixImageField + " = " + suffixImageFieldValue);
        this.log("suffixMap = " + JSON.stringify(suffixMap));
        this.container = document.createElement("div");
        this.container.style.justifyContent = "center";
        this.container.style.display = "flex";
        this.container.style.height = rowHeight + "px";
        this.container.style.alignItems = "center";
        this.container.style.width = "fit-content";
        this.container.className = "customtreecolumncellrenderer";
        var imageBeforeTextMedia = document.createElement("img");
        imageBeforeTextMedia.alt = "Node image";
        imageBeforeTextMedia.style.height = "var(--grid-font-size)";
        imageBeforeTextMedia.style.width = "var(--grid-font-size)";
        imageBeforeTextMedia.className = "customtreecolumncellrenderer-imageBeforeTextMedia";
        var imageBeforeTextLink = document.createElement("img");
        imageBeforeTextLink.style.height = "100%";
        imageBeforeTextLink.style.width = rowHeight + "px";
        imageBeforeTextLink.className = "ustomtreecolumncellrenderer-imageBeforeTextLink";
        var imageAfterText = document.createElement("img");
        imageAfterText.style.height = "var(--grid-font-size)";
        imageAfterText.style.width = "var(--grid-font-size)";
        imageAfterText.className = "customtreecolumncellrenderer-imageAfterText";
        var text = document.createElement("span");
        text.textContent = this.paramValue;
        text.style.display = "flex";
        text.style.verticalAlign = "middle";
        text.style.margin = "0px 8px";
        if (stateFieldValue && iconMap) {
          imageBeforeTextMedia.src = "/Thingworx/MediaEntities/" + iconMap[stateFieldValue];
        } else if (stateFieldValue) {
          imageBeforeTextLink.src = stateFieldValue;
        }
        if (suffixImageFieldValue && suffixMap) {
          imageAfterText.src = "/Thingworx/MediaEntities/" + suffixMap[suffixImageFieldValue];
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
      };
      CustomIconCellRenderer.prototype.getGui = function () {
        return this.container;
      };
      return CustomIconCellRenderer;
    }();
    var CustomTreeColumnCellRenderer = function () {
      function CustomTreeColumnCellRenderer() {
        var _this = this;
        this.loggingEnabled = true;
        this.log = function (logMessage) {
          if (_this.loggingEnabled) {
            console.log("AgGridWidget.CustomTreeColumnCellRenderer" + _this.logPrefix + logMessage);
          }
        };
      }
      CustomTreeColumnCellRenderer.prototype.init = function (params) {
        this.logPrefix = ".init(): ";
        this.params = params;
        this.loggingEnabled = this.params.context.EnableConsoleLogging;
        var treeColumnValue = params.data[params.context.TreeColumnField];
        this.log("Starting, treeColumnValue = " + JSON.stringify(treeColumnValue));
        var stateField = this.params.stateField;
        var stateFieldValue = this.params.data[stateField];
        var iconMap = this.params.iconMap;
        var suffixImageField = this.params.suffixImage;
        var suffixMap = this.params.suffixMap;
        var suffixImageFieldValue = this.params.data[suffixImageField];
        var rowHeight = this.params.context.RowHeight;
        this.log("stateField = " + stateField + " = " + stateFieldValue);
        this.log("iconMap = " + JSON.stringify(iconMap));
        this.log("suffixImageField = " + suffixImageField + " = " + suffixImageFieldValue);
        this.log("suffixMap = " + JSON.stringify(suffixMap));
        this.container = document.createElement("div");
        this.container.style.justifyContent = "flex-start";
        this.container.style.display = "flex";
        this.container.style.height = rowHeight + "px";
        this.container.style.alignItems = "center";
        this.container.className = "customtreecolumncellrenderer";
        var imageBeforeTextMedia = document.createElement("img");
        imageBeforeTextMedia.alt = "Node image";
        imageBeforeTextMedia.style.height = "var(--grid-font-size)";
        imageBeforeTextMedia.style.width = "var(--grid-font-size)";
        imageBeforeTextMedia.className = "customtreecolumncellrenderer-imageBeforeTextMedia";
        var imageBeforeTextLink = document.createElement("img");
        imageBeforeTextLink.style.height = "100%";
        imageBeforeTextLink.style.width = rowHeight + "px";
        imageBeforeTextLink.className = "customtreecolumncellrenderer-imageBeforeTextLink";
        var imageAfterText = document.createElement("img");
        imageAfterText.className = "customtreecolumncellrenderer-imageAfterText";
        if (suffixImageField === "isFavorite" && suffixImageFieldValue) {
          imageAfterText.style.height = "16px";
          imageAfterText.style.width = "16px";
        } else {
          imageAfterText.style.height = "var(--grid-font-size)";
          imageAfterText.style.width = "var(--grid-font-size)";
        }
        var text = document.createElement("span");
        text.textContent = treeColumnValue;
        text.style.display = "flex";
        text.style.verticalAlign = "middle";
        text.style.margin = "0px 8px";
        if (stateFieldValue && iconMap) {
          var iconPath = iconMap[stateFieldValue] || iconMap.default || "";
          imageBeforeTextMedia.src = "/Thingworx/MediaEntities/" + iconPath;
        } else if (stateFieldValue) {
          imageBeforeTextLink.src = stateFieldValue;
        }
        if (suffixImageFieldValue && suffixMap) {
          imageAfterText.src = "/Thingworx/MediaEntities/" + suffixMap[suffixImageFieldValue];
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
      };
      CustomTreeColumnCellRenderer.prototype.getGui = function () {
        return this.container;
      };
      return CustomTreeColumnCellRenderer;
    }();
    function CustomNumberEditor_assign() {
      CustomNumberEditor_assign = Object.assign || function (t) {
        var s;
        for (var i = 1, n = arguments.length; i < n; i++) {
          for (var p in s = arguments[i]) {
            if (Object.prototype.hasOwnProperty.call(s, p)) {
              t[p] = s[p];
            }
          }
        }
        return t;
      };
      return CustomNumberEditor_assign.apply(this, arguments);
    }
    var CustomNumberEditor = function () {
      function CustomNumberEditor() {
        var _this = this;
        this.integerOnly = false;
        this.loggingEnabled = true;
        this.log = function (logMessage) {
          if (_this.loggingEnabled) {
            console.log("AgGridWidget.CustomNumberEditor" + _this.logPrefix + logMessage);
          }
        };
      }
      CustomNumberEditor.prototype.init = function (params) {
        var _this = this;
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
        } else if (params.value !== undefined && params.value !== null) {
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
          var regex = new RegExp(/^-?\d{0,}(\.\d*)?(e\B)?(\d+)$/);
          if (_this.isKeyPressedNavigation(event)) {
            event.stopPropagation();
          } else {
            regex.test(_this.input.value);
          }
        });
        var charPressIsNotANumber = params.eventKey && "-1234567890.e".indexOf(params.eventKey) < 0;
        this.cancelBeforeStart = !!charPressIsNotANumber;
      };
      CustomNumberEditor.prototype.isKeyPressedNavigation = function (event) {
        return event.key === "ArrowLeft" || event.key === "ArrowRight";
      };
      CustomNumberEditor.prototype.getGui = function () {
        return this.input;
      };
      CustomNumberEditor.prototype.afterGuiAttached = function () {
        this.input.focus();
      };
      CustomNumberEditor.prototype.isCancelBeforeStart = function () {
        return this.cancelBeforeStart;
      };
      CustomNumberEditor.prototype.isCancelAfterEnd = function () {
        return !new RegExp(/^-?\d{0,}(\.\d*)?(e\B)?(\d+)$/).test(this.input.value);
      };
      CustomNumberEditor.prototype.getValue = function () {
        var inputVal = this.input.value;
        this.log("inputVal = " + inputVal + ", this.integerOnly = " + this.integerOnly);
        if (this.integerOnly) {
          return parseInt(inputVal);
        } else {
          return parseFloat(inputVal);
        }
      };
      CustomNumberEditor.prototype.destroy = function () {};
      CustomNumberEditor.prototype.isPopup = function () {
        return false;
      };
      CustomNumberEditor.prototype.isCharNumeric = function (charStr) {
        var inputStr = this.getValue() + charStr;
        this.log("isCharNumeric, inputStr = " + inputStr + ", this.integerOnly = " + this.integerOnly);
        var allowedInput = this.integerOnly ? "1234567890-e" : "1234567890-.e";
        var flag = false;
        if (charStr && !(allowedInput.indexOf(charStr) < 0)) {
          var matchedChar = this.integerOnly ? inputStr.match(/(-+)(e+)/g) : inputStr.match(/(-+)|(\.+)|(e+)/g);
          if (matchedChar) {
            var countedChar = matchedChar.reduce(function (allChar, chars) {
              var _a;
              var currCount = allChar[chars] ?? 0;
              return CustomNumberEditor_assign(CustomNumberEditor_assign({}, allChar), ((_a = {})[chars] = currCount + 1, _a));
            }, {});
            if (!(countedChar["-"] > 1) && !(countedChar["."] > 1) && !(countedChar.e > 1)) {
              flag = true;
            }
          } else {
            flag = true;
          }
        }
        return flag;
      };
      CustomNumberEditor.prototype.isKeyPressedNumeric = function (event) {
        var charStr = event.key;
        return this.isCharNumeric(charStr);
      };
      CustomNumberEditor.prototype.isNumericKey = function (event) {
        var charStr = event.key;
        return this.isCharNumeric(charStr);
      };
      return CustomNumberEditor;
    }();
    var CustomLoadingCellRenderer = function () {
      function CustomLoadingCellRenderer() {}
      CustomLoadingCellRenderer.prototype.init = function (params) {
        this.eGui = document.createElement("div");
        this.eGui.innerHTML = `
            <div class="ag-custom-loading-cell" style="padding-left: 10px; line-height: 25px;">  
                <i class="fas fa-spinner fa-pulse"></i> 
                <span>${params.loadingMessage} </span>
            </div>
        `;
      };
      CustomLoadingCellRenderer.prototype.getGui = function () {
        return this.eGui;
      };
      return CustomLoadingCellRenderer;
    }();
    var CustomHyperlinkRenderer = function () {
      function CustomHyperlinkRenderer() {
        var _this = this;
        this.loggingEnabled = true;
        this.arrowElement = index_es.Ay.icon(fontawesome_free_solid_index_es.AaJ).node[0];
        this.log = function (logMessage) {
          if (_this.loggingEnabled) {
            console.log("AgGridWidget.CustomHyperlinkRenderer()" + _this.logPrefix + logMessage);
          }
        };
        var now = new Date().getTime();
        this.containerId = (now++).toString();
      }
      CustomHyperlinkRenderer.prototype.init = function (params) {
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
            var linkFieldName = params.linkFieldName;
            this.log("using linkFieldName = " + linkFieldName);
            this.anchorLinkText = params.data[linkFieldName] ? params.data[linkFieldName] : params.value;
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
      };
      CustomHyperlinkRenderer.prototype.getGui = function () {
        return this.container;
      };
      return CustomHyperlinkRenderer;
    }();
    var CustomMashupStatusPanel = function () {
      function CustomMashupStatusPanel() {}
      CustomMashupStatusPanel.prototype.init = function (params) {
        this.params = params;
        this.visible = true;
        this.eGui = document.createElement("div");
        var mashup = document.createElement("embed");
        mashup.src = "/Thingworx/Mashups/" + this.params.context.CustomMashupStatusPanelMashupName;
        this.eGui.appendChild(mashup);
      };
      CustomMashupStatusPanel.prototype.getGui = function () {
        return this.eGui;
      };
      CustomMashupStatusPanel.prototype.destroy = function () {
        this.eButton.removeEventListener("click", this.buttonListener);
      };
      CustomMashupStatusPanel.prototype.onButtonClicked = function () {
        alert("Selected Row Count: " + this.params.api.getSelectedRows().length);
      };
      CustomMashupStatusPanel.prototype.setVisible = function (visible) {
        this.visible = visible;
        this.eGui.style.display = this.visible ? "block" : "none";
      };
      CustomMashupStatusPanel.prototype.isVisible = function () {
        return this.visible;
      };
      return CustomMashupStatusPanel;
    }();
    var CustomServerSideTotalsStatusPanel = function () {
      function CustomServerSideTotalsStatusPanel() {
        var _this = this;
        this.loggingEnabled = true;
        this.log = function (logPrefix, logMessage) {
          if (_this.loggingEnabled) {
            console.log("AgGridWidget.CustomServerSideTotalsStatusPanel()" + logPrefix + logMessage);
          }
        };
      }
      CustomServerSideTotalsStatusPanel.prototype.init = function (params) {
        this.params = params;
        this.loggingEnabled = this.params.context.EnableConsoleLogging;
        this.eGui = document.createElement("div");
        this.rowsLabel = document.createElement("span");
        this.rowsLabel.setAttribute("class", "ag-status-name-value-value");
        this.rowsLabel.innerHTML = "Rows: ";
        this.eGui.appendChild(this.rowsLabel);
        this.log(".init(): ", "Starting, this.eGui = " + JSON.stringify(this.eGui));
      };
      CustomServerSideTotalsStatusPanel.prototype.getGui = function () {
        return this.eGui;
      };
      CustomServerSideTotalsStatusPanel.prototype.destroy = function () {};
      CustomServerSideTotalsStatusPanel.prototype.updateRowCount = function (newTotals) {
        if (this.params.context.EnableConsoleLogging) {
          console.log("CustomServerSideTotalsStatusPanel.updateRowCount(): newTotals = {}", newTotals);
        }
        this.rowsLabel.innerHTML = "Rows: " + newTotals;
      };
      return CustomServerSideTotalsStatusPanel;
    }();
    var CustomColumnAggregateFooterRenderer = __webpack_require__(9880);
    var CustomLoadingOverlay = function () {
      function CustomLoadingOverlay() {
        var _this = this;
        this.loggingEnabled = true;
        this.log = function (logMessage) {
          if (_this.loggingEnabled) {
            console.log("AgGridWidget.CustomLoadingOverlay" + _this.logPrefix + logMessage);
          }
        };
      }
      CustomLoadingOverlay.prototype.init = function (params) {
        this.logPrefix = ".init(): ";
        this.log("Starting, creating overlay with message: " + params.loadingMessage);
        this.eGui = document.createElement("div");
        this.eGui.innerHTML = `<div class="ag-overlay-loading-center" role="presentation">
               <div role="presentation" style="height:100px; width:100px; center / contain no-repeat; margin: 0 auto;"></div>
               <div aria-live="polite" aria-atomic="true">${params.loadingMessage}</div>
            </div>`;
        this.log("done...");
      };
      CustomLoadingOverlay.prototype.getGui = function () {
        return this.eGui;
      };
      CustomLoadingOverlay.prototype.refresh = function (params) {
        return false;
      };
      return CustomLoadingOverlay;
    }();
    var CustomDateRenderer = __webpack_require__(3190);
    var CustomJsonRenderer = __webpack_require__(8988);
