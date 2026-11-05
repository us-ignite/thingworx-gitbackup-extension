var moment__WEBPACK_IMPORTED_MODULE_0__ = require("./5093.js");
var moment__WEBPACK_IMPORTED_MODULE_0___default = moment__WEBPACK_IMPORTED_MODULE_0__;
export var l = function () {
  function DatePicker() {
    var _this = this;
    this.datePickerSelected = function () {
      console.log("AgGridWidget.DatePicker().datePickerSelected(): closing date picker after selection...");
      $(_this.eInput).datepicker("hide");
    };
    this.datePickerClosed = function () {
      var cancelTheEditing = true;
      var date = $(_this.eInput).datepicker("getDate");
      if (date !== null) {
        var selectedDate = date.getTime();
        console.log("AgGridWidget.DatePicker().datePickerClosed(): closing date picker, selectedDate = " + selectedDate + ", this.originalValueWithOffset = " + _this.originalValueWithOffset);
        if (selectedDate !== _this.originalValueWithOffset) {
          cancelTheEditing = false;
        }
      }
      console.log("AgGridWidget.DatePicker().datePickerClosed(): stopping editing of cell, cancelTheEditing = " + cancelTheEditing);
      _this.params.api.stopEditing(cancelTheEditing);
    };
  }
  DatePicker.prototype.init = function (params) {
    this.elementId = "datepicker-" + new Date().getTime();
    this.params = params;
    this.eInput = document.createElement("input");
    this.eInput.id = this.elementId;
    this.logPrefix = "AgGridWidget.DatePicker(" + params.colDef.field + "): ";
    console.log(this.logPrefix + "colDef = " + JSON.stringify(params.colDef));
    var dateFormat = params.context.dateFormat ? params.context.dateFormat : "dd-mm-yy";
    var timeZoneOffset = new Date(params.value).getTimezoneOffset();
    this.originalValueWithOffset = new Date(params.value + timeZoneOffset * 60000).getTime();
    this.selectedDate = new Date(params.value);
    var minDate;
    var maxDate;
    var paramValueAsDate = new Date(params.value).getTime();
    var inputValue = paramValueAsDate ? moment__WEBPACK_IMPORTED_MODULE_0___default(this.originalValueWithOffset).format(params.context.DateFormat) : "";
    console.log(this.logPrefix + "dateFormat = " + dateFormat + ", params.value = " + params.value + ", paramValueAsDate = " + paramValueAsDate + ", inputValue = " + inputValue);
    console.log(this.logPrefix + "timeZoneOffset = " + timeZoneOffset);
    this.eInput.value = inputValue;
    this.eInput.classList.add("ag-input");
    this.eInput.style.height = "100%";
    try {
      console.log(this.logPrefix + "getting min/max dates = " + params.context.DateMinDate + "/" + params.context.DateMaxDate);
      if (params.context.DateMinDate) {
        minDate = new Date(Date.parse(params.context.DateMinDate));
      }
      if (params.context.DateMaxDate) {
        maxDate = new Date(Date.parse(params.context.DateMaxDate));
      }
    } catch (err) {
      console.log(this.logPrefix + "error creating min/max dates, err = " + err);
    }
    console.log(this.logPrefix + "after setting, min/max dates = " + minDate + "/" + maxDate);
    var defaultDate = params.value ? new Date(params.value) : new Date();
    $(this.eInput).datepicker({
      dateFormat,
      defaultDate,
      showButtonPanel: true,
      changeMonth: true,
      changeYear: true,
      minDate,
      maxDate,
      onSelect: this.datePickerSelected,
      onClose: this.datePickerClosed,
      showWeek: params.context.DateShowWeekNumbers,
      firstDay: 1
    });
  };
  DatePicker.prototype.getGui = function () {
    return this.eInput;
  };
  DatePicker.prototype.afterGuiAttached = function () {
    this.eInput.focus();
    this.eInput.select();
  };
  DatePicker.prototype.getValue = function () {
    var date = $(this.eInput).datepicker("getDate");
    console.log(this.logPrefix + "date = " + date + ", type = " + typeof date);
    if (date) {
      this.selectedDate = date;
    }
    return date || this.selectedDate;
  };
  DatePicker.prototype.destroy = function () {};
  DatePicker.prototype.isPopup = function () {
    return false;
  };
  return DatePicker;
}();