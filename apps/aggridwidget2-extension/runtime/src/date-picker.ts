// Recovered from webpack module 4528.
import { formatDate } from "./date-format";
export class DatePicker {
  originalValueWithOffset: number;
  params: any;
  elementId: string;
  logPrefix: string;
  selectedDate: Date;
  eInput: HTMLInputElement;
  constructor() {
    this.datePickerSelected = this.datePickerSelected.bind(this);
    this.datePickerClosed = this.datePickerClosed.bind(this);
  }
  datePickerSelected(): void {
    console.log(
      "AgGridWidget.DatePicker().datePickerSelected(): closing date picker after selection...",
    );
    $(this.eInput).datepicker("hide");
  }
  datePickerClosed(): void {
    let cancelTheEditing = true;
    const date = $(this.eInput).datepicker("getDate");
    if (date !== null) {
      const selectedDate = date.getTime();
      console.log(
        "AgGridWidget.DatePicker().datePickerClosed(): closing date picker, selectedDate = " +
          selectedDate +
          ", this.originalValueWithOffset = " +
          this.originalValueWithOffset,
      );
      if (selectedDate !== this.originalValueWithOffset) {
        cancelTheEditing = false;
      }
    }
    console.log(
      "AgGridWidget.DatePicker().datePickerClosed(): stopping editing of cell, cancelTheEditing = " +
        cancelTheEditing,
    );
    this.params.api.stopEditing(cancelTheEditing);
  }
  init(params) {
    this.elementId = "datepicker-" + new Date().getTime();
    this.params = params;
    this.eInput = document.createElement("input");
    this.eInput.id = this.elementId;
    this.logPrefix = "AgGridWidget.DatePicker(" + params.colDef.field + "): ";
    console.log(this.logPrefix + "colDef = " + JSON.stringify(params.colDef));
    const dateFormat = params.context.dateFormat
      ? params.context.dateFormat
      : "dd-mm-yy";
    const timeZoneOffset = new Date(params.value).getTimezoneOffset();
    this.originalValueWithOffset = new Date(
      params.value + timeZoneOffset * 60000,
    ).getTime();
    this.selectedDate = new Date(params.value);
    let minDate;
    let maxDate;
    const paramValueAsDate = new Date(params.value).getTime();
    const inputValue = paramValueAsDate
      ? formatDate(this.originalValueWithOffset, params.context.DateFormat)
      : "";
    console.log(
      this.logPrefix +
        "dateFormat = " +
        dateFormat +
        ", params.value = " +
        params.value +
        ", paramValueAsDate = " +
        paramValueAsDate +
        ", inputValue = " +
        inputValue,
    );
    console.log(this.logPrefix + "timeZoneOffset = " + timeZoneOffset);
    this.eInput.value = inputValue;
    this.eInput.classList.add("ag-input");
    this.eInput.style.height = "100%";
    try {
      console.log(
        this.logPrefix +
          "getting min/max dates = " +
          params.context.DateMinDate +
          "/" +
          params.context.DateMaxDate,
      );
      if (params.context.DateMinDate) {
        minDate = new Date(Date.parse(params.context.DateMinDate));
      }
      if (params.context.DateMaxDate) {
        maxDate = new Date(Date.parse(params.context.DateMaxDate));
      }
    } catch (err) {
      console.log(
        this.logPrefix + "error creating min/max dates, err = " + err,
      );
    }
    console.log(
      this.logPrefix +
        "after setting, min/max dates = " +
        minDate +
        "/" +
        maxDate,
    );
    const defaultDate = params.value ? new Date(params.value) : new Date();
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
      firstDay: 1,
    });
  }
  getGui() {
    return this.eInput;
  }
  afterGuiAttached() {
    this.eInput.focus();
    this.eInput.select();
  }
  getValue() {
    const date = $(this.eInput).datepicker("getDate");
    console.log(this.logPrefix + "date = " + date + ", type = " + typeof date);
    if (date) {
      this.selectedDate = date;
    }
    return date || this.selectedDate;
  }
  destroy() {}
  isPopup() {
    return false;
  }
}
