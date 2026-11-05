var moment__WEBPACK_IMPORTED_MODULE_0__ = require("./5093.js");
var moment__WEBPACK_IMPORTED_MODULE_0___default = moment__WEBPACK_IMPORTED_MODULE_0__;
export var j = function () {
  function CustomDateRenderer() {
    var _this = this;
    this.loggingEnabled = false;
    this.log = function (logMessage) {
      _this.loggingEnabled;
    };
    var now = new Date().getTime();
    this.containerId = (now++).toString();
  }
  CustomDateRenderer.prototype.init = function (params) {
    this.logPrefix = ".init(" + params.colDef.field + "): ";
    this.log("Starting, params.value = " + params.value + ", type = " + typeof params.value);
    this.loggingEnabled = params.context.EnableConsoleLogging;
    if (params.value) {
      this.log("Starting, params.value = " + params.value + ", type = " + typeof params.value);
      this.container = document.createElement("div");
      var currentDate = new Date(params.value);
      var timeZoneOffset = currentDate.getTimezoneOffset();
      var utcDate = new Date(currentDate.valueOf() + timeZoneOffset * 60000);
      this.log("after setting time to now, currentDate = " + currentDate.getTime());
      this.log("timeZoneOffset = " + timeZoneOffset);
      this.log("utcDate = " + utcDate.getTime());
      this.container.innerHTML = params.value == "1970-01-01T00:00:00.000Z" ? "" : moment__WEBPACK_IMPORTED_MODULE_0___default(utcDate.getTime()).format(params.context.DateFormat);
    }
  };
  CustomDateRenderer.prototype.getGui = function () {
    return this.container;
  };
  return CustomDateRenderer;
}();