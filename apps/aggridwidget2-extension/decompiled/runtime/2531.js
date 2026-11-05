var _fortawesome_fontawesome__WEBPACK_IMPORTED_MODULE_0__ = require("./4262.js");
var _fortawesome_fontawesome_free_solid__WEBPACK_IMPORTED_MODULE_1__ = require("./1727.js");
var _FileUploader__WEBPACK_IMPORTED_MODULE_2__ = require("./2982.js");
export var W = function () {
  function CustomImageRenderer() {
    var _this = this;
    this.loggingEnabled = true;
    this.fileUploader = new _FileUploader__WEBPACK_IMPORTED_MODULE_2__.l();
    this.buttonSpinnerElement = _fortawesome_fontawesome__WEBPACK_IMPORTED_MODULE_0__.Ay.icon(_fortawesome_fontawesome_free_solid__WEBPACK_IMPORTED_MODULE_1__.z1G, {
      classes: ["fa-spin"]
    }).node[0];
    this.now = new Date().getTime() + Math.floor(100000 + Math.random() * 900000).toString().substring(0, 4);
    this.log = function (logPrefix, logMessage) {
      if (_this.loggingEnabled) {
        console.log("AgGridWidget.CustomImageRenderer()" + logPrefix + logMessage);
      }
    };
    this.createImageSpan = function () {
      var resultElement = document.createElement("div");
      resultElement.style.height = "100%";
      resultElement.style.display = "flex";
      resultElement.style.justifyContent = "center";
      resultElement.style.alignContent = "center";
      if (_this.params) {
        _this.imageElement = document.createElement("img");
        _this.imageElement.src = _this.params.value;
        _this.imageElement.alt = "File not found";
        _this.imageElement.onerror = function () {
          return _this.imageElement.style.display = "none";
        };
        _this.imageElement.style.height = "100%";
        _this.imageElement.style.position = "absolute";
        if (_this.params.hasOwnProperty("imgStyle")) {
          resultElement.style.cssText = _this.params.imgStyle;
        }
        resultElement.appendChild(_this.imageElement);
      }
      return resultElement;
    };
    this.checkIfFieldIsEditable = function () {
      var logPrefix = ".checkIfFieldIsEditable(" + _this.now + "): ";
      var colDef = _this.params.colDef;
      _this.log(logPrefix, "image cell was double-clicked, columndef = " + JSON.stringify(colDef));
      var isEditable = undefined;
      if (colDef.hasOwnProperty("editable")) {
        var editableValue = colDef.editable;
        _this.log(logPrefix, "next column has editable, value = " + editableValue);
        if (editableValue.hasOwnProperty("canEdit")) {
          var drivingField_1 = editableValue.canEdit;
          isEditable = function () {
            return _this.params.data[drivingField_1];
          };
        } else {
          isEditable = editableValue.hasOwnProperty("useEditableFields") ? function () {
            var fieldIsEditable = true;
            var editableFieldName = _this.params.context.EditableFieldsColumn;
            _this.log(logPrefix, "checking to see if this cell is editable using field: " + editableFieldName);
            var editableFields = _this.params.data[editableFieldName] ? JSON.parse(_this.params.data[editableFieldName]) : {};
            _this.log(logPrefix, "editableFields = " + JSON.stringify(editableFields));
            _this.log(logPrefix, "does it have this column?: = " + colDef.field);
            if (editableFields.hasOwnProperty(colDef.field)) {
              fieldIsEditable = editableFields[colDef.field];
              _this.log(logPrefix, "Yes! And it = " + fieldIsEditable);
            }
            return fieldIsEditable;
          } : colDef.editable;
        }
        if (isEditable) {
          _this.log(logPrefix, "this field is editable, switching to upload button...");
          _this.switchToUploadButton();
        }
      }
    };
    this.startUpload = async (e) => {
      var logPrefix = ".startUpload(" + this.now + "): ";
      this.eGui.replaceChild(this.buttonSpinnerElement, this.fileContainer);
      try {
        var newFilePath = await this.fileUploader.startUpload(e, this.params, this.now);
        this.log(logPrefix, logPrefix + "upload complete");
        var colId = this.params.column.getColId();
        this.params.node.setDataValue(colId, newFilePath);
        this.imageElement.src = newFilePath;
        this.eGui.replaceChild(this.fileContainer, this.buttonSpinnerElement);
        this.log(logPrefix, logPrefix + "renderer update done");
      } catch (error) {
        this.log(logPrefix, logPrefix + "failure uploading...");
        this.eGui.replaceChild(this.imageElement, this.fileContainer);
      }
    };
    this.switchToUploadButton = function () {
      _this.log(".switchToUploadButton(): ", ".switchToUploadButton(): swithing to upload button...");
      _this.eGui.replaceChild(_this.fileContainer, _this.imageElement);
    };
    this.openFileInput = function () {
      _this.log(".openFileInput(): ", ".openFileInput(): button clicked....");
      _this.inputElement.dispatchEvent(new Event("click"));
      _this.inputElement.click();
    };
  }
  CustomImageRenderer.prototype.init = function (params) {
    this.now;
    this.params = params;
    this.loggingEnabled = this.params.context.EnableConsoleLogging;
    this.params.allow = params.hasOwnProperty("allow") ? params.allow : "/*";
    this.params.uploadThumbnail = !!params.hasOwnProperty("uploadThumbnail") && params.uploadThumbnail;
    this.params.repository = !!params.hasOwnProperty("repository") && params.repository;
    if (params.value) {
      var fileSplit = params.value.split("/");
      var currentFilePath = fileSplit.length > 3 ? "/" + fileSplit.slice(4, fileSplit.length - 1).join("/") : "/Ideas";
      this.params.newFilePath = params.hasOwnProperty("newFilePath") ? params.newFilePath : currentFilePath;
      this.params.repository = params.hasOwnProperty("repository") ? params.repository : fileSplit.length > 3 ? fileSplit[3] : "SystemRepository";
    } else {
      this.params.newFilePath = "/Ideas";
    }
    this.fileContainer = document.createElement("div");
    this.fileContainer.setAttribute("tabindex", "0");
    this.fileContainer.classList.add("outer-div");
    this.buttonDiv = document.createElement("div");
    this.buttonDiv.classList.add("button-div");
    this.inputDiv = document.createElement("div");
    this.inputDiv.classList.add("file-input");
    this.buttonElement = document.createElement("btn");
    this.buttonElement.setAttribute("id", this.now + "btn");
    this.buttonElement.setAttribute("name", "fileUploadButton");
    this.buttonElement.classList.add("button-13");
    this.buttonElement.addEventListener("click", this.openFileInput);
    this.buttonElement.textContent = "Upload";
    this.buttonDiv.appendChild(this.buttonElement);
    this.fileContainer.appendChild(this.buttonDiv);
    this.inputElement = document.createElement("input");
    this.inputElement.setAttribute("id", this.now + "input");
    this.inputElement.setAttribute("type", "file");
    this.inputElement.setAttribute("allow", "image/*");
    this.inputElement.addEventListener("change", this.startUpload);
    this.inputDiv.appendChild(this.inputElement);
    this.fileContainer.appendChild(this.inputDiv);
    this.eGui = this.createImageSpan();
    this.eGui.addEventListener("dblclick", this.checkIfFieldIsEditable);
  };
  CustomImageRenderer.prototype.getGui = function () {
    this.log(".getGui():", "created eGui, adding dblclick event listener, eGui = " + this.eGui.outerHTML);
    return this.eGui;
  };
  CustomImageRenderer.prototype.getValue = function () {
    return this.imageElement.src;
  };
  return CustomImageRenderer;
}();