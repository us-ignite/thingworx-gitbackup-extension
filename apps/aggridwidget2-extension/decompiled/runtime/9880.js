export var x = function () {
  function CustomColumnAggregateFooterRenderer() {
    var _this = this;
    this.loggingEnabled = true;
    this.log = function (logMessage) {
      if (_this.loggingEnabled) {
        console.log("AgGridWidget.CustomColumnAggregateFooterRenderer" + _this.logPrefix + logMessage);
      }
    };
  }
  CustomColumnAggregateFooterRenderer.prototype.init = function (params) {
    this.logPrefix = ".init(" + params.column.getId() + "): ";
    this.log("Starting...");
    this.params = params;
    this.loggingEnabled = this.params.context.EnableConsoleLogging;
    this.container = document.createElement("div");
    var colDef = params.colDef;
    if (colDef.hasOwnProperty("footerCellRendererParams")) {
      var footerParams = colDef.footerCellRendererParams;
      this.log("next column has footerParams: " + JSON.stringify(footerParams));
      if (footerParams.hasOwnProperty("purpose") && footerParams.hasOwnProperty("label")) {
        this.labelSpan = document.createElement("span");
        this.labelSpan.innerHTML = footerParams.label;
      }
      if (footerParams.hasOwnProperty("cellClass")) {
        this.container.setAttribute("class", footerParams.cellClass);
      }
      this.container.appendChild(this.labelSpan);
    }
    this.log("done, this.container = " + this.container.innerHTML);
  };
  CustomColumnAggregateFooterRenderer.prototype.getGui = function () {
    return this.container;
  };
  CustomColumnAggregateFooterRenderer.prototype.getValue = function () {};
  return CustomColumnAggregateFooterRenderer;
}();