// Recovered from webpack module 3190.
import { formatDate } from './date-format';
export class CustomDateRenderer {
    containerId: string;
    logPrefix: string;
    container: HTMLDivElement;
    loggingEnabled: any;
    constructor() {
        this.containerId = ((Date.now()) + 1).toString();
    }
    init(params) {
        this.logPrefix = ".init(" + params.colDef.field + "): ";
        console.log("Starting, params.value = " + params.value + ", type = " + typeof params.value);
        this.loggingEnabled = params.context.EnableConsoleLogging;
        if (params.value) {
            console.log("Starting, params.value = " + params.value + ", type = " + typeof params.value);
            this.container = document.createElement("div");
            const currentDate = new Date(params.value);
            const timeZoneOffset = currentDate.getTimezoneOffset();
            const utcDate = new Date(currentDate.valueOf() + timeZoneOffset * 60000);
            console.log("after setting time to now, currentDate = " + currentDate.getTime());
            console.log("timeZoneOffset = " + timeZoneOffset);
            console.log("utcDate = " + utcDate.getTime());
            this.container.innerHTML = params.value == "1970-01-01T00:00:00.000Z" ? "" : formatDate(utcDate.getTime(), params.context.DateFormat);
        }
    }
    getGui() {
        return this.container;
    }
}
