require("./8131.js");
export var f = function () {
  function CustomJsonRenderer() {
    var _this = this;
    this.loggingEnabled = true;
    this.log = function (logMessage) {
      if (_this.loggingEnabled) {
        console.log("AgGridWidget.CustomJsonRenderer" + _this.logPrefix + logMessage);
      }
    };
    var now = new Date().getTime();
    this.containerId = (now++).toString();
  }
  CustomJsonRenderer.prototype.init = function (params) {
    this.logPrefix = ".init(" + params.column.getId() + "): ";
    this.log("Starting...");
    this.loggingEnabled = params.context.EnableConsoleLogging;
    this.paramValue = params.value;
    this.container = document.createElement("div");
    this.input = document.createElement("p");
    this.input.className = "ag-cell-value";
    this.input.innerHTML = JSON.stringify(this.paramValue);
    this.container.appendChild(this.input);
  };
  CustomJsonRenderer.prototype.getGui = function () {
    return this.container;
  };
  return CustomJsonRenderer;
}();