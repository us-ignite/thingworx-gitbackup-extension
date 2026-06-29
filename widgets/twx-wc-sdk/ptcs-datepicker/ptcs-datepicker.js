import {LitElement, html, css} from 'lit';
import {when} from 'lit/directives/when.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-icons/cds-icons.js';
import 'ptcs-textfield/ptcs-textfield.js';
import 'ptcs-dropdown/ptcs-dropdown.js';
import './ptcs-datepicker-calendar.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';
import 'ptcs-behavior-validate/ptcs-behavior-validate.js';
import moment from 'ptcs-moment/moment-import.js';
import {enableDatePickerEditor, setDate, getDate, hasValidDate, removeDatePickerEditor, hintFormat} from './date-editor.js';

// Simulate invalid moment object
const invalidMomentTime = {isValid: () => false};

const WAIT_RANGE_START_END_UPDATE_MS = 40;

// Convert date to Date
export function asDate(date) {
    if (!date) {
        return undefined;
    }
    if (date instanceof Date) {
        return date;
    }
    const date$ = moment(date);
    return date$.isValid() ? date$.toDate() : undefined;
}

// Return a copy of newDate, unless it specifes the same Date as oldDate
// Reason: only trigger update callback if a new date is specified
function updateDate(newDate, oldDate) {
    if (!newDate) {
        return undefined; // Reset date
    }
    return (!oldDate || newDate.getTime() !== oldDate.getTime()) ? new Date(newDate.getTime()) : oldDate;
}

// Verify that a timeText is a valid 12-hour time
function valid12hourTime(timeText) {
    if (typeof timeText !== 'string') {
        return false;
    }
    const hour = timeText.split(':')[0];
    if (!hour || hour < 1 || hour > 12) {
        return false;
    }

    // Just make sure nothing is sneaky with the hour string, such as '4.7'
    return /^\d+$/.test(hour);
}

// Test if two dates specify the same time, ignoring milliseconds
function differentDates(date, date$) {
    // date:  JS Date
    // date$: moment date
    return moment(date).format('YYYY-MM-DD HH:mm:ss') !== date$.format('YYYY-MM-DD HH:mm:ss');
}

// Strip time from Date
function stripTime(date, end) {
    return date ? new Date(date.getFullYear(), date.getMonth(), date.getDate(), end ? 23 : 0, end ? 59 : 0, end ? 59 : 0) : undefined;
}

// Get meridiem strings (am / pm) as moment displays them (i.e. moment localization)
function meridiemValues() {
    const amLabel = moment('2021-12-10 09:00').format('a');
    const pmLabel = moment('2021-12-10 21:00').format('a');
    return [amLabel, pmLabel];
}

// Adjust time with  interval
function adjustTime(date, op, arg) {
    switch (arg.intervalType) {
        case 'h': date[op](arg.interval, 'hours'); break;
        case 'm': date[op](arg.interval, 'minutes'); break;
        case 's': date[op](arg.interval, 'seconds'); break;
        case 'd': date[op](arg.interval, 'days'); break;
    }
    return date;
}


PTCS.Datepicker = class extends PTCS.BehaviorTabindex(PTCS.BehaviorValidate(PTCS.BehaviorTooltip(PTCS.BehaviorFocus(
    PTCS.BehaviorStyleable(L2Pw(LitElement)))))) {

    static get styles() {
        return css`
            :host {
                display: inline-flex;
                flex-direction: column;
                align-items: stretch;
                align-content: stretch;
                width: 100%;
                box-sizing: border-box;
                overflow-y: auto;
            }

            :host([hidden]) {
                display: none;
            }

            [part=label-container] {
                display: contents;
            }

            [part=label] {
                flex: 0 0 auto;
                width: 100%;
                flex-shrink: 0;
            }

            [part=controls] {
                flex: 1 1 auto;
                display: flex;
                align-items: flex-end;
                box-sizing: border-box;
            }

            [part=date-field] {
                position: relative;
                width: 100%;
                height: 100%;
                box-sizing: border-box;
            }

            [part=meridiem-dd] {
                height: 100%;
            }

            #calendarbutton[invisible] {
                visibility: hidden;
            }

            :host([left-menu-button]) [part=calendar-menu-button] {
                order: -1;
            }

            :host([hide-calendar-icon]) [part=calendar-menu-button] {
                display: none !important;
            }

            ptcs-textfield {
                flex: 1 1 auto;
            }

            :host(:not([editing]):not([opened])[validity]) ptcs-textfield[invalid]::part(text-box),
            :host(:not([editing]):not([opened])[validity]) ptcs-dropdown[invalid]::part(select-box) {
                background: var(--ptcs-invalid-background, #FFF0F0);
                border-color: var(--ptcs-invalid-border-color, #ce3939);
                border-width: var(--ptcs-invalid-border-width, 1px);
            }

            :host(:not([editing]):not([opened])[validity=valid]) ptcs-textfield::part(text-box),
            :host(:not([editing]):not([opened])[validity=valid]) ptcs-dropdown::part(select-box) {
                background: var(--ptcs-valid-background, #EFFFEF);
                border-color: var(--ptcs-valid-border-color, #178642);
                border-width: var(--ptcs-valid-border-width, 1px);
            }

            :host(:not([opened])[validity=unvalidated]) ptcs-textfield::part(text-box),
            :host(:not([opened])[validity=unvalidated]) ptcs-dropdown::part(select-box) {
                background: var(--ptcs-unvalidated-background, #FFFFFF);
                border-color: var(--ptcs-unvalidated-border-color, #c2c7ce);
                border-width: var(--ptcs-unvalidated-border-width, 1px);
            }
        `;
    }

    render() {
        return html`
            <div part="label-container">
                ${when(this.label, () => html`
                    <ptcs-label
                        id="label"
                        part="label"
                        .label=${this.label}
                        disable-tooltip
                        multi-line
                        horizontal-alignment=${this.labelAlignment}>
                    </ptcs-label>
                `)}
            </div>
            <div id="controls1" part="controls">
                <ptcs-textfield
                    id="datetext"
                    part="date-field"
                    .labelAlignment=${this.labelAlignment}
                    ?disabled=${this.disabled}
                    .label=${this._fieldtextLabel1}
                    spellcheck="false"
                    @focus=${this._field1Focus}
                    @clear-text=${this._clearedField1}
                    .showClearText=${!this.hideClearDate}
                    .hintText=${this._hintText1}
                    exportparts=${this._exportparts}
                    .tooltip=${this._tooltip}
                    .tooltipIcon=${this.tooltipIcon}
                    tabindex=${this._dfTabindex(this._delegatedFocus, this._noSpaceForMessage)}
                ></ptcs-textfield>
                ${when(!this.__hideExpandPanel, () => html`
                    <ptcs-textfield
                        id="datetext2"
                        part="date-field"
                        .labelAlignment=${this.labelAlignment}
                        ?disabled=${this.disabled}
                        .label=${this._fieldtextLabel2}
                        spellcheck="false"
                        @focus=${this._field2Focus}
                        @clear-text=${this._clearedField2}
                        .showClearText=${!this.hideClearDate}
                        .hintText=${this._hintText2}
                        exportparts=${this._exportparts}
                        .tooltip=${this._tooltip}
                        .tooltipIcon=${this.tooltipIcon}
                        tabindex=${this._dfTabindex(this._delegatedFocus, this._noSpaceForMessage)}
                    ></ptcs-textfield>
                    ${when(!this._bool(this.formatToken) && this._showTime && this.twelveHourClock, () => html`
                        <ptcs-dropdown
                            id="meridiem"
                            part="meridiem-dd"
                            @focus=${this._dropdownFocus}
                            @blur=${this._dropdownBlur}
                            ?disabled=${this.disabled}
                            .items=${this.meridiemStrings}
                            .label=${this.meridiemLabel}
                            exportparts=${this._exportmeridiem}
                            @selected-changed=${this._meridiemSelection}
                            .selectedValue=${this._meridiemValue(this._date1)}
                            .labelAlignment=${this.labelAlignment}
                            disable-no-item-selection
                            .tooltip=${this._tooltip}
                            .tooltipIcon=${this.tooltipIcon}
                            @mode-changed=${this._modeChanged}
                            tabindex=${this._dfTabindex(this._delegatedFocus, this._noSpaceForMessage)}
                        ></ptcs-dropdown>
                    `)}
                `)}
                <ptcs-button
                    id="calendarbutton"
                    part="calendar-menu-button"
                    variant="tertiary"
                    .icon=${this.icon}
                    .iconWidth=${this.iconWidth}
                    .iconHeight=${this.iconHeight}
                    ?disabled=${this.disabled}
                    @click=${this._openCalendar}
                    .tooltip=${this._tooltip}
                    .tooltipIcon=${this.tooltipIcon}
                    ?invisible=${this.__showBottomPanel}
                    ?selected=${this._opened}
                    tabindex=${this._dfTabindex(this._delegatedFocus, this._noSpaceForMessage, this.__showBottomPanel)}
                ></ptcs-button>
            </div>
            ${when(this.__showBottomPanel, () => html`
                <div id="controls2" part="controls" control2>
                    <ptcs-textfield
                        id="datetext3"
                        part="date-field"
                        .labelAlignment=${this.labelAlignment}
                        ?disabled=${this.disabled}
                        .label=${this.toFieldLabel}
                        spellcheck="false"
                        @focus=${this._field3Focus}
                        @clear-text=${this._clearedField3}
                        .showClearText=${!this.hideClearDate}
                        .hintText=${this._hintTextEndDate}
                        exportparts=${this._exportparts}
                        .tooltip=${this._tooltip}
                        .tooltipIcon=${this.tooltipIcon}
                        tabindex=${this._dfTabindex(this._delegatedFocus, this._noSpaceForMessage)}
                    ></ptcs-textfield>
                    <ptcs-textfield
                        id="datetext4"
                        part="date-field"
                        .labelAlignment=${this.labelAlignment}
                        ?disabled=${this.disabled}
                        .label=${this.toTimeLabel}
                        spellcheck="false"
                        @focus=${this._field4Focus}
                        @clear-text=${this._clearedField4}
                        .showClearText=${!this.hideClearDate}
                        .hintText=${this._hintTextEndTime}
                        exportparts=${this._exportparts}
                        .tooltip=${this._tooltip}
                        .tooltipIcon=${this.tooltipIcon}
                        tabindex=${this._dfTabindex(this._delegatedFocus, this._noSpaceForMessage)}
                    ></ptcs-textfield>
                    ${when(this.twelveHourClock, () => html`
                        <ptcs-dropdown
                            id="meridiem2"
                            part="meridiem-dd"
                            @focus=${this._dropdownFocus}
                            @blur=${this._dropdownBlur}
                            ?disabled=${this.disabled}
                            .items=${this.meridiemStrings}
                            .label=${this.meridiemLabel}
                            exportparts=${this._exportmeridiem}
                            @selected-changed=${this._meridiem2Selection}
                            .selectedValue=${this._meridiemValue(this._date2)}
                            .labelAlignment=${this.labelAlignment}
                            disable-no-item-selection
                            .tooltip=${this._tooltip}
                            .tooltipIcon=${this.tooltipIcon}
                            @mode-changed=${this._modeChanged}
                            tabindex=${this._dfTabindex(this._delegatedFocus, this._noSpaceForMessage)}
                        ></ptcs-dropdown>
                    `)}
                    <ptcs-button
                        id="calendarbutton2"
                        part="calendar-menu-button"
                        variant="tertiary"
                        .icon=${this.icon}
                        .iconWidth=${this.iconWidth}
                        .iconHeight=${this.iconHeight}
                        ?disabled=${this.disabled}
                        @click=${this._openCalendar}
                        .tooltip=${this._tooltip}
                        .tooltipIcon=${this.tooltipIcon}
                        ?selected=${this._opened}
                        tabindex=${this._dfTabindex(this._delegatedFocus, this._noSpaceForMessage)}
                    ></ptcs-button>
                </div>
            `)}
        `;
        // <ptcs-datepicker-calendar> is added on-demand by _setupCalendar
    }

    static get is() {
        return 'ptcs-datepicker';
    }

    static get properties() {
        return {
            // disabled?
            disabled: {
                type:    Boolean,
                reflect: true
            },

            // Don't use the formatting based editor
            disableMaskedInput: {
                type:      Boolean,
                attribute: 'disable-masked-input'
            },

            _maskedInput: {
                type:  Boolean,
                state: true
            },

            //
            // NOTE: date, dateTime, fromDate, toDate and selectedDate are needed for backwards compatibility.
            //       All internal date manipulations uses _date1 and _date2.
            //

            // The selected date, as a string
            date: {
                type:        String,
                observer:    '_dateChanged',
                observeWhen: 'immediate',
                reflect:     true
            },

            // The selected date, as Date object
            dateTime: {
                type:        Date,
                observer:    '_dateTimeChanged',
                observeWhen: 'immediate',
                attribute:   'date-time'
            },

            // The selected fromDate (for range calendar)
            fromDate: {
                type:        Date,
                observer:    '_fromDateChanged',
                observeWhen: 'immediate',
                attribute:   'from-date'
            },

            // The selected toDate (for range calendar)
            toDate: {
                type:        Date,
                observer:    '_toDateChanged',
                observeWhen: 'immediate',
                attribute:   'to-date'
            },

            // The selected date as a moment object or {fromDate, toDate}
            selectedDate: {
                type:      Object,
                attribute: 'selected-date'
            },


            //
            // NOTE: Actual dates directly manipulated by the datepicker
            //

            // Range picker?
            dateRangeSelection: {
                type:      Boolean,
                reflect:   true,
                attribute: 'date-range-selection'
            },

            // Primary date of datepicker. Range start date if dateRangeSelection
            _date1: {
                type:  Date,
                state: true
            },

            // Secondary date. Range end date if dateRangeSelection, otherwise ignored
            _date2: {
                type:  Date,
                state: true
            },

            // Position of the calendar menu button (left or right. Default: right)
            leftMenuButton: {
                type:      Boolean,
                reflect:   true,
                attribute: 'left-menu-button'
            },

            // button labels
            selectLabel: {
                type:      String,
                attribute: 'select-label'
            },

            cancelLabel: {
                type:      String,
                attribute: 'cancel-label'
            },

            monthLabel: {
                type:      String,
                attribute: 'month-label'
            },

            yearLabel: {
                type:      String,
                attribute: 'year-label'
            },

            // other labels

            noMatchesLabel: {
                type:      String,
                attribute: 'no-matches-label'
            },

            invalidDateLabel: {
                type:      String,
                attribute: 'invalid-date-label'
            },

            hoursLabel: {
                type:      String,
                attribute: 'hours-label'
            },

            minutesLabel: {
                type:      String,
                attribute: 'minutes-label'
            },

            secondsLabel: {
                type:      String,
                attribute: 'seconds-label'
            },

            // Datepicker icon
            icon: {
                type: String
            },

            iconWidth: {
                type:      String,
                attribute: 'iconWidth'
            },

            iconHeight: {
                type:      String,
                attribute: 'icon-height'
            },

            // Labels for datepicker
            label: {
                type: String
            },

            // Label for datetext textfield
            _fieldtextLabel1: {
                type:  String,
                state: true
            },

            // Label for datetext2 textfield
            _fieldtextLabel2: {
                type:  String,
                state: true
            },

            // Tooltip filtered against the visible labels (the textfield only filters out truncated tooltip against its own label)
            _tooltip: {
                type:  String,
                state: true
            },

            labelAlignment: {
                type:      String,
                reflect:   true,
                attribute: 'label-alignment'
            },

            dateLabel: {
                type:      String,
                attribute: 'date-label'
            },

            fromFieldLabel: {
                type:      String,
                attribute: 'from-field-label'
            },

            fromFieldHintText: {
                type:      String,
                attribute: 'from-field-hint-text'
            },

            toFieldLabel: {
                type:      String,
                attribute: 'to-field-label'
            },

            toFieldHintText: {
                type:      String,
                attribute: 'to-field-hint-text'
            },

            timeLabel: {
                type:      String,
                attribute: 'time-label'
            },

            fromTimeLabel: {
                type:      String,
                attribute: 'from-time-label'
            },

            toTimeLabel: {
                type:      String,
                attribute: 'to-time-label'
            },

            calendarStartTimeLabel: {
                type:      String,
                attribute: 'calendar-start-time-label'
            },

            calendarEndTimeLabel: {
                type:      String,
                attribute: 'calendar-end-time-label'
            },

            meridiemLabel: {
                type:      String,
                attribute: 'meridiem-label'
            },

            // How should the time be formatted?
            _timeFormat: {
                type:  String,
                state: true
            },

            // Dates are edited in numeric form in the current dateOrder using current dateDelimiter
            _editedDateFormat: {
                type:  String,
                state: true
            },

            // How should dates be formatted?
            _dateFormat: {
                type:  String,
                state: true
            },

            // Same as _dateFormat except no time part
            _dtFmtNoTime: {
                type:  String,
                state: true
            },

            // Specified hint text for datepicker
            hintText: {
                type:      String,
                attribute: 'hint-text'
            },

            // Hint to date editor is dependent on dateOrder and date delimiter
            _hintTextDate: {
                type:  String,
                state: true
            },

            // Hint to date editor is dependent on dateOrder and date delimiter
            _hintTextStartDate: {
                type:  String,
                state: true
            },

            // Hint to date editor is dependent on dateOrder and date delimiter
            _hintTextEndDate: {
                type:  String,
                state: true
            },

            // Specified hint text for time editor
            timeHintText: {
                type:      String,
                attribute: 'time-hint-text'
            },

            // Hint to time editor
            _hintTextTime: {
                type:  String,
                state: true
            },

            // Hint to start time range
            startTimeHintText: {
                type:      String,
                attribute: 'start-time-hint-text'
            },

            _hintTextStartTime: {
                type:  String,
                state: true
            },

            // Hint to end time range
            endTimeHintText: {
                type:      String,
                attribute: 'end-time-hint-text'
            },

            _hintTextEndTime: {
                type:  String,
                state: true
            },

            // Hint text of field1 (#datetext)
            _hintText1: {
                type:  String,
                state: true
            },

            // Hint text of field2 (#datetext2)
            _hintText2: {
                type:  String,
                state: true
            },

            // Initialize date with current time?
            initWithCurrentDateTime: {
                type:      Boolean,
                attribute: 'init-with-current-date-time'
            },

            // Date editing in-progress? This state is used to turn off invalid border highlight on textfield during editing
            editing: {
                type:        Boolean,
                reflect:     true,
                observer:    '_editingChanged',
                observeWhen: 'immediate',
            },

            // Display date with time?
            showTime: {
                type:      Boolean,
                reflect:   true,
                attribute: 'show-time'
            },

            _showTime: {
                type:  Boolean,
                state: true
            },

            // 12-hour clock (AM/PM)?
            twelveHourClock: {
                type:      Boolean,
                reflect:   true,
                attribute: 'twelve-hour-clock'
            },

            _twelveHourClock: {
                type:  Boolean,
                state: true
            },

            // am / pm strings for the 12-hour clock dropdown
            meridiemStrings: {
                type:      Array,
                attribute: 'meridiem-strings'
            },

            // This property is nowadays only for sending !_showTime to the calendar
            _dateOnly: {
                type:  Boolean,
                state: true
            },

            // Display hh:mm:ss?
            displaySeconds: {
                type:      Boolean,
                attribute: 'display-seconds'
            },

            _displaySeconds: {
                type:  Boolean,
                state: true
            },

            // Delimiter between date parts, as in 2021-12-08
            dateDelimiter: {
                type:      String,
                attribute: 'date-delimiter'
            },

            monthFormat: {
                type:      String,
                attribute: 'month-format'
            },

            dateOrder: {
                type:      String,
                attribute: 'date-order'
            },

            // Full override of format
            formatToken: {
                type:      String,
                attribute: 'format-token'
            },

            hideClearDate: {
                type:      Boolean,
                attribute: 'hide-clear-date'
            },

            // Is calendar open?
            _opened: {
                type:        Boolean,
                reflect:     true,
                observer:    '_openedChanged',
                observeWhen: 'immediate',
                state:       true
            },

            // The actual calendar (attached to <body>)
            // undefined: not ready to be configured
            // null:      ready to be configured
            // ? - do we need to keep it as a Lit property?
            __calendarObj: {
                type:  HTMLElement,
                state: true
            },

            // First day of week
            weekStart: {
                type:      String,
                attribute: 'week-start'
            },

            // Create a unique ID (only for testing, I think)
            _calendarId: {
                type:  String,
                state: true
            },

            // Type specifier for this.interval
            intervalType: {
                type:      String,
                attribute: 'interval-type'
            },

            interval: {
                type: Number
            },

            // The last interval that was assigned to dateTime / fromDate / endDate
            _intervalOld: {
                type:  Object,
                state: true
            },

            yearRange: {
                type:      Number,
                attribute: 'year-range'
            },

            actionPosition: {
                type:      String,
                attribute: 'action-position'
            },

            // FocusBehavior should simulate a click event when space is pressed
            _spaceActivate: {
                type:     Boolean,
                value:    true,
                readOnly: true
            },

            _delegatedFocus: {
                type:  String,
                state: true
            },

            daysContainingAnyData: {
                type:      Array,
                attribute: 'days-containing-any-data'
            },

            _exportparts: {
                type:     String,
                readOnly: true,
                value:    PTCS.exportparts('date-field-', PTCS.Textfield)
            },

            _exportmeridiem: {
                type:     String,
                readOnly: true,
                value:    PTCS.exportparts('meridiem-dd-', PTCS.Dropdown)
            },

            //
            // Validation
            //

            // Client-provided custom validation function
            extraValidation: {
                type:      Function,
                attribute: 'extra-validation'
            },

            // Validation: min Date
            min: {
                type: Date
            },

            minFailureMessage: {
                type:      String,
                attribute: 'min-failure-message'
            },

            // Validation: max Date
            max: {
                type: Date
            },

            maxFailureMessage: {
                type:      String,
                attribute: 'max-failure-message'
            },

            minStartDate: {
                type:      Date,
                attribute: 'min-start-date'
            },

            minStartDateFailureMessage: {
                type:      String,
                attribute: 'min-start-date-failure-message'
            },

            maxStartDate: {
                type:      Date,
                attribute: 'max-start-date'
            },

            maxStartDateFailureMessage: {
                type:      String,
                attribute: 'max-start-date-failure-message'
            },

            minEndDate: {
                type:      Date,
                attribute: 'min-end-date'
            },

            minEndDateFailureMessage: {
                type:      String,
                attribute: 'min-end-date-failure-message'
            },

            maxEndDate: {
                type:      Date,
                attribute: 'max-end-date'
            },

            maxEndDateFailureMessage: {
                type:      String,
                attribute: 'max-end-date-failure-message'
            },

            maxRange: {
                type:      Number,
                attribute: 'max-range'
            },

            maxRangeFailureMessage: {
                type:      String,
                attribute: 'max-range-failure-message'
            },

            // Validation: false if Date field is empty
            required: {
                type:    Boolean,
                isValue: required => !!required
            },

            requiredMessage: {
                type:      String,
                attribute: 'required-message'
            },

            __showBottomPanel: {
                type:  Boolean,
                state: true
            },

            __hideExpandPanel: {
                type:  Boolean,
                state: true
            },

            // A single point for validation. Increment __validateNo to force a new validation
            __validateNo: {
                type:     Number,
                // eslint-disable-next-line max-len
                validate: '_validateDatepicker(dateTime, fromDate, toDate, dateRangeSelection, _showTime, _displaySeconds, _twelveHourClock, required, min, max, minStartDate, maxStartDate, minEndDate, maxEndDate, maxRange, extraValidation)'
            }
        };
    }

    constructor() {
        super();

        this.disabled = false;
        this.showTime = false;
        this.displaySeconds = false;
        this._delegatedFocus = null;

        this.selectedDate = {};
        this.meridiemStrings = meridiemValues();
        this.daysContainingAnyData = () => [];
        this.noMatchesLabel = 'No matches';
        this.selectLabel = 'Select';
        this.cancelLabel = 'Cancel';
        this.monthLabel = 'Month';
        this.yearLabel = 'Year';
        this.hoursLabel = 'Hours';
        this.minutesLabel = 'Minutes';
        this.secondsLabel = 'Seconds';
        this.icon = 'cds:icon_calendar';
        this.labelAlignment = 'left';
        this.meridiemLabel = 'AM/PM';
        this.dateDelimiter = '-';
        this.monthFormat = 'full';
        this.dateOrder = 'YMD';
        this.weekStart = 'Monday';
        this.intervalType = 'h';
        this.actionPosition = '';

        this.interval = 0;
        this.yearRange = 10;
        this.__validateNo = 0;

        this.tooltipFunc = this._monitorTooltip.bind(this);

        this.__calendarObj = null; // Ready to be assigned

        if (this._stayUnvalidated === undefined) {
            this._stayUnvalidated = true;
        }

        if (this.disableMaskedInput === undefined) {
            this.disableMaskedInput = false;
        }

        // fix for datepicker losing focus -
        // _validationMessageResizeObserver: _noSpaceForMessage remains 'undefined', causing the datepicker to lose focus
        if (this._noSpaceForMessage === undefined) {
            this._noSpaceForMessage = false;
        }

        // Start in editing mode, unless otheriwse specfied, to prevent (visible) validation errors
        if (this.editing === undefined) {
            this.editing = this._stayUnvalidated;
        }
        if (this.invalidDateLabel === undefined) {
            this.invalidDateLabel = 'Invalid date';
        }
    }

    connectedCallback() {
        super.connectedCallback();

        if (this.__calendarObj) {
            this.__calendarObj.__saSa = this.__saSa;
            document.body.appendChild(this.__calendarObj);
        }
    }

    disconnectedCallback() {
        super.disconnectedCallback();

        if (this.__calendarObj) {
            document.body.removeChild(this.__calendarObj);
        }
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (changedProperties.has('daysContainingAnyData')) {
            this._daysContainingAnyDataChanged();
        }
        if (changedProperties.has('dateOrder')) {
            this.dateOrder = this._dateOrderChanged();
        }
        if (['showTime', 'formatToken'].some(propName => changedProperties.has(propName))) {
            this._showTime = this._computeShowTime();
            // For some mysterious reason, the datepicker uses showTime and the calendar dateOnly (!)
            // This bridges that conceptual difference...
            // In earlier implementation ranges did not have an explicit time (a range always spanned from start to end of a day)
            this._dateOnly = !this._showTime;
        }
        if (['twelveHourClock', 'formatToken'].some(propName => changedProperties.has(propName))) {
            this._twelveHourClock = this._computeTwelveHourClock();
        }
        if (['displaySeconds', 'formatToken'].some(propName => changedProperties.has(propName))) {
            this._displaySeconds = this._computeDisplaySeconds();
        }
        if (['_showTime', '_displaySeconds', '_twelveHourClock'].some(propName => changedProperties.has(propName))) {
            this._timeFormat = this._computeTimeFormat();
        }
        if (['dateOrder', 'dateDelimiter'].some(propName => changedProperties.has(propName))) {
            this._editedDateFormat = this._computeEditedDateFormat();
        }
        if (['_showTime', '_displaySeconds', 'dateDelimiter', 'monthFormat', 'dateOrder', '_timeFormat', 'formatToken'].some(
            propName => changedProperties.has(propName))) {
            this._dateFormat = this._computeDateFormat();
        }
        if (['_dateFormat', 'formatToken'].some(propName => changedProperties.has(propName))) {
            this._dtFmtNoTime = this._computeDateFormatWithoutTimePart();
        }
        if (['dateRangeSelection', '_showTime', 'formatToken'].some(propName => changedProperties.has(propName))) {
            this.__showBottomPanel = this._showBottomPanel();
        }
        if (['dateRangeSelection', '_showTime', 'formatToken'].some(propName => changedProperties.has(propName))) {
            this.__hideExpandPanel = this._hideExpandPanel();
        }
        if (['dateRangeSelection', 'fromFieldLabel', 'dateLabel'].some(propName => changedProperties.has(propName))) {
            this._fieldtextLabel1 = this._updateFieldtextLabel1();
        }
        if (['_showTime', 'dateRangeSelection', 'fromTimeLabel', 'toFieldLabel', 'timeLabel', 'formatToken'].some(
            propName => changedProperties.has(propName))) {
            this._fieldtextLabel2 = this._updateFieldtextLabel2();
        }
        if (['disableMaskedInput', 'formatToken'].some(propName => changedProperties.has(propName))) {
            this._maskedInput = this._computeMaskedInput();
        }
        if (['hintText', 'dateOrder', 'dateDelimiter', 'formatToken'].some(propName => changedProperties.has(propName))) {
            this._hintTextDate = this._computeDateHintText(this.hintText);
        }
        if (['fromFieldHintText', 'dateOrder', 'dateDelimiter', 'formatToken'].some(propName => changedProperties.has(propName))) {
            this._hintTextStartDate = this._computeDateHintText(this.fromFieldHintText);
        }
        if (['toFieldHintText', 'dateOrder', 'dateDelimiter', 'formatToken'].some(propName => changedProperties.has(propName))) {
            this._hintTextEndDate = this._computeDateHintText(this.toFieldHintText);
        }
        if (['timeHintText', '_displaySeconds'].some(propName => changedProperties.has(propName))) {
            this._hintTextTime = this._computeTimeHintText(this.timeHintText);
        }
        if (['startTimeHintText', '_displaySeconds'].some(propName => changedProperties.has(propName))) {
            this._hintTextStartTime = this._computeTimeHintText(this.startTimeHintText);
        }
        if (['endTimeHintText', '_displaySeconds'].some(propName => changedProperties.has(propName))) {
            this._hintTextEndTime = this._computeTimeHintText(this.endTimeHintText);
        }
        if (['dateRangeSelection', '_hintTextStartDate', '_hintTextDate'].some(propName => changedProperties.has(propName))) {
            this._hintText1 = this._computeHintText1();
        }
        if (['dateRangeSelection', '_showTime', '_hintTextTime', '_hintTextStartTime', '_hintTextEndDate', 'formatToken'].some(
            propName => changedProperties.has(propName))) {
            this._hintText2 = this._computeHintText2();
        }
        if (['tooltip', 'label', '_fieldtextLabel1', '_fieldtextLabel2', 'toFieldLabel', 'toTimeLabel', 'meridiemLabel',
            '_twelveHourClock', '_showTime', 'dateRangeSelection'].some(propName => changedProperties.has(propName))) {
            this._tooltip = this._computeTooltip();
        }
        if (['initWithCurrentDateTime', '_showTime'].some(propName => changedProperties.has(propName))) {
            this._observeInitWithCurrentDateTimeAndShowTime();
        }
        if (changedProperties.has('dateRangeSelection')) {
            this._setPublicDates(this._date1, this._date2);
        }
        if (changedProperties.has('invalidDateLabel')) {
            moment.updateLocale(moment.locale(), {
                invalidDate: this.invalidDateLabel
            });
        }
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        // notify events
        if (changedProperties.has('date')) {
            this.dispatchEvent(new CustomEvent('date-changed', {
                detail: {
                    value: this.date
                }
            }));
        }
        if (changedProperties.has('dateTime')) {
            this.dispatchEvent(new CustomEvent('date-time-changed', {
                detail: {
                    value: this.dateTime
                }
            }));
        }
        if (changedProperties.has('fromDate')) {
            this.dispatchEvent(new CustomEvent('from-date-changed', {
                detail: {
                    value: this.fromDate
                }
            }));
        }
        if (changedProperties.has('toDate')) {
            this.dispatchEvent(new CustomEvent('to-date-changed', {
                detail: {
                    value: this.toDate
                }
            }));
        }
        if (changedProperties.has('selectedDate')) {
            this.dispatchEvent(new CustomEvent('selected-date-changed', {
                detail: {
                    value: this.selectedDate
                }
            }));
        }
        if (['fromDate', 'toDate'].some(propName => changedProperties.has(propName))) {
            this.dispatchEvent(new CustomEvent('range-updated', {
                bubbles:  true,
                composed: true,
                detail:   {fromDate: this.fromDate, toDate: this.toDate}
            }));
        }

        if (['_date1', '_dtFmtNoTime'].some(propName => changedProperties.has(propName))) {
            this._field1Text(this._date1, this._dtFmtNoTime);
        }
        if (['_date1', '_date2', '_showTime', '_twelveHourClock', '_dtFmtNoTime', '_timeFormat', 'formatToken'].some(
            propName => changedProperties.has(propName))) {
            this._field2Text(this._date1, this._date2, this._showTime, this._twelveHourClock,
                this._dtFmtNoTime, this._timeFormat, this.formatToken);
        }
        if (['_date2', '_dtFmtNoTime'].some(propName => changedProperties.has(propName))) {
            this._field3Text(this._date2, this._dtFmtNoTime);
        }
        if (['_date2', '_twelveHourClock', '_timeFormat'].some(propName => changedProperties.has(propName))) {
            this._field4Text(this._date2, this._twelveHourClock, this._timeFormat);
        }
        if (['_maskedInput', 'dateRangeSelection', '_showTime', '_dtFmtNoTime', '_timeFormat', 'formatToken', 'min', 'max', 'minStartDate',
            'maxStartDate', 'minEndDate', 'maxEndDate'].some(propName => changedProperties.has(propName))) {
            this._configureEditor();
        }

        if (this.__datetext1restore !== undefined) {
            this._datetext1.text = this.__datetext1restore;
        }
        if (this.__datetext2restore !== undefined) {
            this._datetext2.text = this.__datetext2restore;
        }
        if (this.__datetext3restore !== undefined) {
            this._datetext3.text = this.__datetext3restore;
        }
        if (this.__datetext4restore !== undefined) {
            this._datetext4.text = this.__datetext4restore;
        }

        this.__datetext1restore = undefined;
        this.__datetext2restore = undefined;
        this.__datetext3restore = undefined;
        this.__datetext4restore = undefined;
    }

    get _calendar() {
        if (this.__calendarObj) {
            // Ready and available
            return this.__calendarObj;
        }
        if (this.__calendarObj === undefined) {
            // Not ready to be created
            return null;
        }
        // Set it up
        this._setupCalendar();
        console.assert(this.__calendarObj);
        return this.__calendarObj;
    }

    // eslint-disable-next-line max-len
    _computeTooltip() {
        if (this._showTime && this._twelveHourClock && this.tooltip === this.meridiemLabel) {
            return '';
        }
        if (this._showTime && this.dateRangeSelection) {
            return this.tooltip !== this.label &&
                this.tooltip !== this._fieldtextLabel1 &&
                this.tooltip !== this._fieldtextLabel2 &&
                this.tooltip !== this.toFieldLabel &&
                this.tooltip !== this.toTimeLabel ? this.tooltip : '';
        } else if (this.dateRangeSelection || this._showTime) {
            return this.tooltip !== this.label &&
                this.tooltip !== this._fieldtextLabel1 &&
                this.tooltip !== this._fieldtextLabel2 ? this.tooltip : '';
        } else if (this.tooltip === this._fieldtextLabel1) {
            return '';
        }
        return this.tooltip !== this.label ? this.tooltip : '';
    }

    _monitorTooltip() {
        // Filter out tooltip against a fully shown (not truncated) hint text
        if (this.dateRangeSelection && this.showTime) {
            if (!this._datetext3.hasText && this._tooltip === this._hintTextTime) {
                return '';
            }
            if (!this._datetext4.hasText && this._tooltip === this._hintTextEndDate) {
                return '';
            }
        }
        if (this.dateRangeSelection || this.showTime) {
            if (!this._datetext2.hasText && this._tooltip === this._hintText2) {
                return '';
            }
        }
        if (!this._datetext1.hasText && this._tooltip === this._hintText1) {
            return '';
        }
        return this._tooltip || '';
    }

    _getel(id) {
        return this.shadowRoot.getElementById(id) || {};
    }

    get _datetext1() {
        return this.$.datetext;
    }

    get _datetext2() {
        return this._getel('datetext2');
    }

    get _datetext3() {
        return this._getel('datetext3');
    }

    get _datetext4() {
        return this._getel('datetext4');
    }

    _bool(test) {
        return !!test;
    }

    _showBottomPanel() {
        return this.dateRangeSelection && this._showTime && !this.formatToken;
    }

    _hideExpandPanel() {
        return !(this.formatToken ? this.dateRangeSelection : (this.dateRangeSelection || this._showTime));
    }

    // The datepicker changes the public date(s)
    _setPublicDates(date1, date2) {
        if (this.__blockPublicDate) {
            // Publishing new dates is in progress. Ignore change callbacks
            return;
        }

        this.__blockPublicDate = true;
        try {
            if (this.dateRangeSelection) {
                if (date1 && date2 && date1 > date2) {
                    console.warn('Invalid range order - switching');
                    [date2, date1] = [date1, date2];
                    if (!this._showTime) {
                        // Set timestamp per calendar conventions
                        date1.setHours(0, 0, 0, 0);
                        date2.setHours(23, 59, 59, 999);
                    }
                }
                this.fromDate = updateDate(date1, this.fromDate);
                this.toDate = updateDate(date2, this.toDate);
            } else {
                this.dateTime = updateDate(date1, this.dateTime);
            }

            // The internal dates
            this._date1 = updateDate(date1, this._date1);
            this._date2 = updateDate(date2, this._date2);

            // The strange dates (strongly deprecated!)
            const twelveHourFormat = this.twelveHourClock && !this.formatToken ? ' a' : '';
            const format = this._showTime ? this._dateFormat + twelveHourFormat : this._dtFmtNoTime;

            if (this.dateRangeSelection) {
                this.selectedDate = (this.fromDate || this.toDate) ? {fromDate: moment(this.fromDate), toDate: moment(this.toDate)} : {};
                // eslint-disable-next-line no-nested-ternary
                this.date = (this.fromDate && this.toDate) // backwards compatible event
                    ? `${moment(this.fromDate).format(format)} - ${moment(this.toDate).format(format)}`
                    : ((this.fromDate || this.toDate) ? `${moment(this.fromDate || this.toDate).format(format)}` : '');
            } else {
                this.selectedDate = moment(date1);
                this.date = date1 ? this.selectedDate.format(format) : '';
            }

        } finally {
            this.__blockPublicDate = false;
        }
    }

    // Client has changed the date (text) property
    _dateChanged(date) {
        if (this.__blockPublicDate) {
            return; // Changed by datepicker itself. Ignore
        }
        if (this.dateRangeSelection) {
            return; // This property is not used in range mode
        }
        if (!date) {
            return; // Invalid. TODO: Should this really be ignored or should this reset the date? (Refactored, keeping original behavior)
        }

        if (typeof date === 'string') {
            const dateTime$ = moment(date);
            if (dateTime$.isValid() && (!this.dateTime || moment(this.dateTime).format('YYYY-MM-DD') !== dateTime$.format('YYYY-MM-DD'))) {
                this._setPublicDates(dateTime$.toDate());
            }
        }
    }

    _dateTimeChanged(dateTime) {
        if (this.__blockPublicDate) {
            return; // Changed by datepicker itself. Ignore
        }
        if (this.dateRangeSelection) {
            return; // This property is not used in range mode
        }
        const date = asDate(dateTime);

        if (date !== dateTime) {
            // dateTime might have been a string, a millisecond count, a moment date, etc... It _must_ be a Date though
            this.dateTime = date; // This will cause a new change callback
        } else if (date) {
            // Apply interval to dateTime and store the applied interval
            this._intervalOld = this.interval ? {intervalType: this.intervalType, interval: this.interval} : null;
            if (this._intervalOld) {
                this._setPublicDates(adjustTime(moment(date), 'add', this._intervalOld).toDate());
            } else {
                this._setPublicDates(date);
            }
        } else {
            this._setPublicDates(undefined);
        }
    }

    _fromDateChanged(fromDate) {
        if (!this.dateRangeSelection) {
            return; // This property is only used in range mode
        }
        this._fromDateChangedTimeStamp = Date.now();
        if (this.__blockPublicDate || this.__blockRangeIntervalAdjustment) {
            return; // Changed by datepicker itself. Ignore
        }
        const date = asDate(fromDate);
        if (date !== fromDate) {
            this.fromDate = date;
        } else {
            // Apply interval to date range and store the applied interval
            this._intervalOld = this.interval ? {intervalType: this.intervalType, interval: this.interval} : null;
            if (this._intervalOld) {
                this.__blockRangeIntervalAdjustment = true;
                setTimeout(() => {
                    // WAIT_RANGE_START_END_UPDATE_MS to let both end ranges to be updated. If toDate was not updated
                    // in that time window, use the already interval incremented _date2 instead of toDate (if it exists)
                    try {
                        const intervalAdjustedFromDate = adjustTime(moment(this.fromDate), 'add', this._intervalOld).toDate();
                        if (!this.toDate) {
                            this._setPublicDates(intervalAdjustedFromDate, undefined);
                        } else {
                            const startEndTimestampDiff = Math.abs(this._fromDateChangedTimeStamp - this._toDateChangedTimeStamp);
                            const intervalAdjustedToDate = this._date2 && startEndTimestampDiff > WAIT_RANGE_START_END_UPDATE_MS
                                ? this._date2
                                : adjustTime(moment(this.toDate), 'add', this._intervalOld).toDate();
                            this._setPublicDates(intervalAdjustedFromDate, intervalAdjustedToDate);
                        }
                    } finally {
                        this.__blockRangeIntervalAdjustment = false;
                    }
                }, WAIT_RANGE_START_END_UPDATE_MS);
            } else {
                // No interval adjustment
                this._setPublicDates(fromDate, fromDate && this._date2 && fromDate > this._date2 ? undefined : this._date2);
            }
        }
    }

    _toDateChanged(toDate) {
        if (!this.dateRangeSelection) {
            return; // This property is only used in range mode
        }
        this._toDateChangedTimeStamp = Date.now();
        if (this.__blockPublicDate || this.__blockRangeIntervalAdjustment) {
            return; // Changed by datepicker itself. Ignore
        }
        const date = asDate(toDate);
        if (date !== toDate) {
            this.toDate = date;
        } else {
            // Apply interval to date range and store the applied interval
            this._intervalOld = this.interval ? {intervalType: this.intervalType, interval: this.interval} : null;
            if (this._intervalOld) {
                this.__blockRangeIntervalAdjustment = true;
                setTimeout(() => {
                    // WAIT_RANGE_START_END_UPDATE_MS to let both end ranges to be updated. If fromDate was not updated
                    // in that time window, use the already interval incremented _date1 instead of fromDate (if it exists)
                    try {
                        const intervalAdjustedToDate = adjustTime(moment(this.toDate), 'add', this._intervalOld).toDate();
                        if (!this.fromDate) {
                            this._setPublicDates(undefined, intervalAdjustedToDate);
                        } else {
                            const startEndTimestampDiff = Math.abs(this._fromDateChangedTimeStamp - this._toDateChangedTimeStamp);
                            const intervalAdjustedFromDate = this._date1 && startEndTimestampDiff > WAIT_RANGE_START_END_UPDATE_MS
                                ? this._date1
                                : adjustTime(moment(this.fromDate), 'add', this._intervalOld).toDate();
                            this._setPublicDates(intervalAdjustedFromDate, intervalAdjustedToDate);
                        }
                    } finally {
                        this.__blockRangeIntervalAdjustment = false;
                    }
                }, WAIT_RANGE_START_END_UPDATE_MS);
            } else {
                // No interval adjustment
                this._setPublicDates(toDate && this._date1 && toDate < this._date1 ? undefined : this._date1, toDate);
            }
        }
    }

    _meridiemValue(date) {
        return date ? moment(date).format('a') : null;
    }

    // AM/PM change for dateTime or start of range
    _meridiemSelection(ev) {
        const selected = ev.detail.value;

        if (this._date1 && this._meridiemValue(this._date1) !== this.meridiemStrings[selected]) {
            if (selected === 0) {
                // pm => am
                this._setPublicDates(moment(this._date1).subtract(12, 'hours').toDate(), this._date2);
            } else if (selected === 1) {
                // am => pm
                this._setPublicDates(moment(this._date1).add(12, 'hours').toDate(), this._date2);
            }
        }

        this.editing = true;
    }

    // AM/PM change for end of range
    _meridiem2Selection(ev) {
        const selected = ev.detail.value;

        if (this._date2 && this._meridiemValue(this._date2) !== this.meridiemStrings[selected]) {
            if (selected === 0) {
                // pm => am
                this._setPublicDates(this._date1, moment(this._date2).subtract(12, 'hours').toDate());
            } else if (selected === 1) {
                // am => pm
                this._setPublicDates(this._date1, moment(this._date2).add(12, 'hours').toDate());
            }
        }

        this.editing = true;
    }

    _dateOrderChanged() {
        const dtOrder = this.dateOrder ? this.dateOrder.toUpperCase() : '';
        return dtOrder === 'AUTO' ? 'YMD' : dtOrder;
    }

    _editingChanged(editing) {
        this._stayUnvalidated = editing;
    }

    // TODO: Figure out how intervalType is supposed to work...
    _observeInterval(/* intervalType, interval */) {
        if (!this._intervalOld) {
            return; // No old interval to subtract
        }
        if (!this.dateTime && !this.dateRangeSelection) {
            this._intervalOld = null;
            return; // No single date to adjust
        }
        if (this.dateRangeSelection && !this.fromDate && !this.toDate) {
            this._intervalOld = null;
            return; // No date range to adjust
        }
        if (this.dateRangeSelection) {
            let from;
            if (this.fromDate) {
                const fromDate$ = moment(this.fromDate);
                adjustTime(fromDate$, 'subtract', this._intervalOld);
                from = fromDate$.toDate();
            }
            let to;
            if (this.toDate) {
                const toDate$ = moment(this.toDate);
                adjustTime(toDate$, 'subtract', this._intervalOld);
                to = toDate$.toDate();
            }
            this._setPublicDates(from, to);
        } else {
            // Single date, not a date range
            const dateTime$ = moment(this.dateTime);
            adjustTime(dateTime$, 'subtract', this._intervalOld);

            // Apply date and get new interval
            this._dateTimeChanged(dateTime$.toDate());
        }
    }

    _daysContainingAnyDataChanged() {
        if (this.daysContainingAnyData && this.daysContainingAnyData.length && this._calendar) {
            this._calendar.datePresentedByDots = this.__parseArrayStringToSet(this.daysContainingAnyData);
        }
    }

    _computeEditedDateFormat() {
        switch (this.dateOrder) {
            case 'MDY':
                return 'MM' + this.dateDelimiter + 'DD' + this.dateDelimiter + 'YYYY';
            case 'DMY':
                return 'DD' + this.dateDelimiter + 'MM' + this.dateDelimiter + 'YYYY';
            case 'YMD':
            case 'AUTO':
            default:
                return 'YYYY' + this.dateDelimiter + 'MM' + this.dateDelimiter + 'DD';
        }
    }

    _computeDateHintText(hintText) {
        if (hintText || hintText === '') {
            return hintText;
        }
        if (this.formatToken) {
            return hintFormat(this.formatToken);
        }
        switch (this.dateOrder) {
            case 'MDY':
                return 'mm' + this.dateDelimiter + 'dd' + this.dateDelimiter + 'yyyy';
            case 'DMY':
                return 'dd' + this.dateDelimiter + 'mm' + this.dateDelimiter + 'yyyy';
            case 'YMD':
            case 'auto':
            default:
                return 'yyyy' + this.dateDelimiter + 'mm' + this.dateDelimiter + 'dd';
        }
    }

    _computeTimeHintText(hintText) {
        return hintText || hintText === '' ? hintText : 'hh:mm' + (this._displaySeconds ? ':ss' : '');
    }

    _computeHintText1() {
        return this.dateRangeSelection ? this._hintTextStartDate : this._hintTextDate;
    }

    _computeHintText2() {
        if (this._showTime && !this.formatToken) {
            return this.dateRangeSelection ? this._hintTextStartTime : this._hintTextTime;
        }
        return this._hintTextEndDate;
    }

    _computeTimeFormat() {
        if (!this._showTime) {
            return '';
        }
        return `${this._twelveHourClock ? 'hh' : 'HH'}:mm${this._displaySeconds ? ':ss' : ''}`;
    }

    _computeShowTime() {
        return this.formatToken ? !!this.formatToken.match(/h|H|m|s|S|a|A|LT|LTS|LLL|LLLL/) : this.showTime;
    }

    _computeDisplaySeconds() {
        return this.formatToken ? !!this.formatToken.match(/s|LTS/) : this.displaySeconds;
    }

    _computeTwelveHourClock() {
        return this.formatToken ? !!this.formatToken.match(/a|A|h|LT|LTS|LLL|LLLL/) : this.twelveHourClock;
    }

    _computeDateFormatWithoutTimePart() {
        if (this.formatToken) {
            return this.formatToken;
        }
        return this._dateFormat.indexOf(' ') !== -1 ? this._dateFormat.substring(0, this._dateFormat.indexOf(' ')) : this._dateFormat;
    }

    _computeDateFormat() {
        if (this.formatToken) {
            return this.formatToken;
        }

        const _mflc = this.monthFormat.toLowerCase();
        let monthFormat;
        if (_mflc === 'short') {
            monthFormat = 'MMM';
        } else if (_mflc === 'numeric') {
            monthFormat = 'MM';
        } else {
            monthFormat = 'MMMM';
        }

        const timeFormat = this._showTime ? ' ' + this._timeFormat : '';
        let str;
        switch (this.dateOrder) {
            case 'DMY':
                return `DD${this.dateDelimiter}${monthFormat}${this.dateDelimiter}YYYY${timeFormat}`;

            case 'MDY':
                return `${monthFormat}${this.dateDelimiter}DD${this.dateDelimiter}YYYY${timeFormat}`;

            case 'AUTO':
                if (this._displaySeconds) {
                    str = ' LTS';
                } else {
                    str = ' LT';
                }
                str = this._showTime ? str : '';
                return 'LL' + str;

            // case 'YMD':
            default:
                return `YYYY${this.dateDelimiter}${monthFormat}${this.dateDelimiter}DD${timeFormat}`;
        }
    }

    // Automatically assign the selected date to "now"?
    _observeInitWithCurrentDateTimeAndShowTime() {
        if (!this.initWithCurrentDateTime) {
            return;
        }

        // eslint-disable-next-line no-nested-ternary
        let today = (this._showTime ? (this._displaySeconds
            ? moment().millisecond(0)
            : moment().second(0).millisecond(0)) : moment({hour: 0})).toDate();

        if (this.dateRangeSelection) {
            this._intervalOld = this.interval ? {intervalType: this.intervalType, interval: this.interval} : null;
            if (this._intervalOld) {
                today = adjustTime(moment(today), 'add', this._intervalOld).toDate();
            }

            if (today < asDate(this.minStartDate) || today > asDate(this.maxEndDate)) {
                return; // Not in valid range
            }
            if (asDate(this.maxStartDate) < today && today < asDate(this.minEndDate)) {
                return; // In unselectable gap
            }
            const date1 = asDate(this.maxStartDate) < today ? this._date1 : today;
            const date2 = asDate(this.minEndDate) > today ? this._date2 : today;

            this._setPublicDates(date1, date2);
        } else {
            if (today < asDate(this.min) || today > asDate(this.max)) {
                return;
            }
            this.dateTime = today;
        }
    }

    _updateFieldtextLabel1() {
        return this.dateRangeSelection ? this.fromFieldLabel : this.dateLabel;
    }

    _updateFieldtextLabel2() {
        if (this.formatToken) {
            return this.toFieldLabel;
        }
        if (this.dateRangeSelection) {
            return this._showTime ? this.fromTimeLabel : this.toFieldLabel;
        }
        return this.timeLabel;
    }

    _setupCalendar() {
        console.assert(!this.__calendarObj);

        // NOTE: this codes preceeds createSubComponent. Maybe it should be refactored...

        // properties in calendar that should be assigned by properties in this
        const toObj = {
            date1:                      '_date1',
            date2:                      '_date2',
            weekStart:                  'weekStart',
            dateOnly:                   '_dateOnly',
            displaySeconds:             '_displaySeconds',
            yearRange:                  'yearRange',
            min:                        'min',
            max:                        'max',
            minStartDate:               'minStartDate',
            maxStartDate:               'maxStartDate',
            minEndDate:                 'minEndDate',
            maxEndDate:                 'maxEndDate',
            maxRange:                   'maxRange',
            disabled:                   'disabled',
            noMatchesLabel:             'noMatchesLabel',
            monthLabel:                 'monthLabel',
            yearLabel:                  'yearLabel',
            hoursLabel:                 'hoursLabel',
            minutesLabel:               'minutesLabel',
            secondsLabel:               'secondsLabel',
            meridiemLabel:              'meridiemLabel',
            selectLabel:                'selectLabel',
            cancelLabel:                'cancelLabel',
            twelveHourClock:            '_twelveHourClock',
            meridiemStrings:            'meridiemStrings',
            dateRangeSelectionCalendar: 'dateRangeSelection',
            dateFormat:                 '_dateFormat',
            fromFieldLabel:             'fromFieldLabel',
            toFieldLabel:               'toFieldLabel',
            calendarStartTimeLabel:     'calendarStartTimeLabel',
            calendarEndTimeLabel:       'calendarEndTimeLabel',
            actionPosition:             'actionPosition'
        };

        // properties in calendar that should assign properties in this
        const fromObj = {
            // Preview changed date(s)
            date1: '_date1',
            date2: '_date2'
        };

        // Move calendar to <body>
        const calendar = this.__calendarObj = PTCS.createElement('ptcs-datepicker-calendar', {hidden: '', part: 'calendar', tabindex: '0'});
        calendar.__$mainCmpnt = this; // Create link to owner in the same way as createSubComponent()
        calendar.__saSa = this.__saSa;

        // Output from calendar object
        for (const srcName in fromObj) {
            const dstName = fromObj[srcName];
            calendar.addEventListener(`${window.camelToDashCase(srcName)}-changed`, ev => {
                if (!this._opened) {
                    return;
                }
                const value = ev.detail.value;
                if (ev.detail.path) {
                    this.notifyPath(ev.detail.path);
                } else if (value && value.indexSplices instanceof Array) {
                    this.notifySplices(dstName, value.indexSplices);
                } else {
                    this[dstName] = value;
                }
            });
        }

        // Input to calendar object
        for (const dstName in toObj) {
            const srcName = toObj[dstName];
            const propChanged = function(value) {
                this.__calendarObj[dstName] = value;
            };

            // Register observer
            this._createPropertyObserver(srcName, propChanged, false);

            // Initial call, if property already has a value
            if (this[srcName] !== undefined) {
                propChanged.call(this, this[srcName]);
            }
        }

        // Explicit events
        calendar.addEventListener('calendar-date-changed', this._calendarChanged.bind(this));

        this.setExternalComponentId();

        document.body.appendChild(this.__calendarObj);

        if (this.daysContainingAnyData && this.daysContainingAnyData.length) {
            calendar.datePresentedByDots = this.__parseArrayStringToSet(this.daysContainingAnyData);
        }
    }

    _getDimension() {
        const controlsBoxId = `controls${this._showBottomPanel() ? 2 : 1}`;
        const dpBox = this.shadowRoot.getElementById(controlsBoxId);

        return {
            // Get window dimension
            windowWidth:  window.innerWidth,
            windowHeight: window.innerHeight,
            // Get current scroll offset
            scrollDx:     document.documentElement.scrollLeft + document.body.scrollLeft,
            scrollDy:     document.documentElement.scrollTop + document.body.scrollTop,
            // Where is the dropdown box?
            box:          dpBox.getBoundingClientRect()
        };
    }

    _diffDimension(r1, r2) {
        if (r1.windowWidth !== r2.windowWidth || r1.windowHeight !== r2.windowHeight) {
            return true;
        }
        if (r1.scrollDx !== r2.scrollDx || r1.scrollDy !== r2.scrollDy) {
            return true;
        }
        if (r1.box.top !== r2.box.top || r1.box.bottom !== r2.box.bottom || r1.box.left !== r2.box.left) {
            return true;
        }

        return false;
    }

    _setCalendarPosition(dim) {
        const calendarDim = this._calendar.getBoundingClientRect();
        let x = this.leftMenuButton ? dim.box.left : dim.box.right - calendarDim.width;
        let y = dim.box.bottom;

        if (x + calendarDim.width > dim.windowWidth) {
            x = dim.windowWidth - calendarDim.width;
        }

        if (x < 0) {
            x = 0;
        }

        if (y + calendarDim.height > dim.windowHeight) {
            y = dim.box.top - calendarDim.height - 6;
        }

        if (y < 0) {
            y = 0;
        }

        // Start at default position
        this._calendar.style.top = (y + dim.scrollDy) + 'px';
        this._calendar.style.left = (x + dim.scrollDx) + 'px';
    }

    // Keep track of calendar position, if the datepicker box is moved or the view is scrolled
    _trackPosition(rOld) {
        if (this._opened) {
            const rNew = this._getDimension();

            /* Do not move the calendar unless the browser window height is greater than the calendar height: Without this
                check, the calendar will "jump" back to the top when user tries to interact with overflowing calendar parts */
            if (rNew.windowHeight > this._calendar.clientHeight) {
                if (this._diffDimension(rOld, rNew)) {
                    this._setCalendarPosition(rNew);
                }
            }

            setTimeout(() => this._trackPosition(rNew), 500);
        }
    }

    _restorePreview() {
        if (this.dateRangeSelection) {
            this._setPublicDates(this.fromDate, this.toDate);
        } else {
            this._setPublicDates(this.dateTime);
        }
    }

    _openCalendar() {
        if (this.disabled) {
            return;
        }

        // make sure the latests dates are made available to the calendar
        this._opened = true;
        this.editing = true;
    }

    _openedChanged() {
        if (!this._calendar) {
            return;
        }

        PTCS.setbattr(this._calendar, 'hidden', !this._opened);

        if (!this._opened) {
            if (this._closeEvent) {
                document.removeEventListener('mousedown', this._closeEvent);
                this._closeEvent = null;
            }

            const button = this._getel((this._showTime && this.dateRangeSelection) ? 'calendarbutton2' : 'calendarbutton');
            if (button.focus) {
                button.focus();
            }

            this.editing = false;

            // This is really only needed when starting with an assigned date and an unassigned time, where the time is assigned to 00:00:00
            this._forceValidation();
            return;
        }

        const dim = this._getDimension();

        this._calendar.updateComplete.then(() => {
            if (!this._opened) {
                return;
            }

            if (!this._closeEvent) {
                // Close the dropdown if the user clicks anywhere outside of it
                this._closeEvent = e => {
                    if (!PTCS.isMainComponentOf(this._calendar, e.target)) {
                        // Clicked outside calendar
                        this._restorePreview();
                        this._opened = false;
                    }
                };

                // using 'mousedown' instead of 'click' due to integration problems with MUB
                document.addEventListener('mousedown', this._closeEvent);
            }

            let __date1 = this._date1;
            if (!this._date1 && !this.dateRangeSelection) {
                __date1 = new Date();
                if (!this._showTime) {
                    __date1.setHours(0, 0, 0, 0);
                }
            }
            this._calendar.date1 = __date1;
            this._calendar.date2 = this._date2;

            this._calendar.gotoTime(this._date1 || new Date());
            if (this.dateRangeSelection && this._showTime && this._date2) {
                this._calendar.gotoTime(this._date2, true);
            }

            this._setCalendarPosition(dim);

            this._trackPosition(dim);

            this._calendar.updateComplete.then(() => {
                this._calendar.focusOnOpen();
            });
        });
    }

    _calendarChanged(ev) {
        ev.stopPropagation();

        const {closeCalendar, date1, date2, dateSelectionCanceled} = ev.detail;

        if (dateSelectionCanceled) {
            this._restorePreview();
        } else {
            this._setPublicDates(date1, date2);
        }

        if (closeCalendar) {
            this._setPublicDates(this._date1, this._date2);
            this._opened = false;
        }
    }

    getExternalComponentId() {
        return this._calendarId;
    }

    /*
     * Sets an id for external component
       NOTE: This is a public method, used e.g. by the widget wrapper. Don't remove
     */
    setExternalComponentId(id) {
        if (id) {
            this._calendarId = id;
        } else if (!this._calendarId) {
            this._calendarId = 'ptcs-datepicker-calendar-' + performance.now().toString().replace('.', '');
        }

        if (this.__calendarObj) {
            this.__calendarObj.setAttribute('id', this._calendarId);
        }
    }

    __parseArrayStringToSet(daysContainingAnyData) {
        return Array.isArray(daysContainingAnyData) && daysContainingAnyData.reduce((set, day) => {
            const date$ = moment(day);
            if (date$.isValid()) {
                set.add(date$.format('YYYY-M-D'));
            } else {
                console.error(`Incorrect data: ${day}`);
            }
            return set;
        }, new Set());
    }

    _setDate(datetext, date) {
        if (datetext instanceof Element) {
            setDate(datetext, date);
        }
    }

    _field1Text(_date1, _dtFmtNoTime) {
        if (this._maskedInput) {
            this._setDate(this._datetext1, _date1);
        } else {
            this._datetext1.text = _date1 ? moment(_date1).format(_dtFmtNoTime) : '';
        }
    }

    _field2Text(_date1, _date2, _showTime, _twelveHourClock, _dtFmtNoTime, _timeFormat, formatToken) {
        if (this._maskedInput) {
            this._setDate(this._datetext2, (_showTime && !formatToken) ? _date1 : _date2);
        } else if (_showTime) {
            this._datetext2.text = _date1 ? moment(_date1).format(_twelveHourClock ? _timeFormat.toLowerCase() : _timeFormat) : '';
        } else {
            this._datetext2.text = _date2 ? moment(_date2).format(_dtFmtNoTime) : '';
        }
    }

    _field3Text(_date2, _dtFmtNoTime) {
        if (this._maskedInput) {
            this._setDate(this._datetext3, _date2);
        } else {
            this._datetext3.text = _date2 ? moment(_date2).format(_dtFmtNoTime) : '';
        }
    }

    _field4Text(_date2, _twelveHourClock, _timeFormat) {
        if (this._maskedInput) {
            this._setDate(this._datetext4, _date2);
        } else {
            this._datetext4.text = _date2 ? moment(_date2).format(_twelveHourClock ? _timeFormat.toLowerCase() : _timeFormat) : '';
        }
    }

    _clearedField1(isUserEvent) {
        this._stayUnvalidated = false;
        if (this._showTime && !this.formatToken) {
            // Keep currently entered time unchanged
            const date1 = this._date1;
            this.__datetext2restore = this._datetext2.text;
            this._setPublicDates(undefined, this._date2);
            setDate(this._datetext2, date1);
            if (isUserEvent) {
                this._forceValidation();
            }
        } else {
            this._setPublicDates(undefined, this._date2);
        }
    }

    _clearedField2(isUserEvent) {
        this._stayUnvalidated = false;
        if (this._showTime) {
            // datetext2 textfield shows time
            if (this._date1) {
                this.__datetext1restore = this._datetext1.text;
                // Clear-out the date
                this._setPublicDates(undefined, this._date2);
                // Restore the day string part of the date (pending an update of corresponding time)
            } else if (isUserEvent) {
                this._forceValidation();
            }
        } else if (this.dateRangeSelection) {
            this._setPublicDates(this._date1, undefined); // Reset toDate
        }
    }

    _clearedField3(isUserEvent) {
        this._stayUnvalidated = false;
        // Keep currently entered time unchanged
        const date2 = this._date2;
        this.__datetext4restore = this._datetext4.text;
        this._setPublicDates(this._date1, undefined);
        setDate(this._datetext4, date2);
        if (isUserEvent) {
            this._forceValidation();
        }
    }

    _clearedField4(isUserEvent) {
        // datetext4 textfield always shows time only
        this._stayUnvalidated = false;
        if (this._date2) {
            this.__datetext3restore = this._datetext3.text;
            // Clear-out the end date
            this._setPublicDates(this._date1, undefined);
            // Restore the day string part of the end date (pending an update of corresponding time)
        } else if (isUserEvent) {
            this._forceValidation();
        }
    }

    _computeMaskedInput() {
        return !!(this.formatToken || !this.disableMaskedInput);
    }

    _configureEditor(/* Lots of properties that affects the date editor */) {
        // Give the browser some time to load all elements into the DOM
        if (this._maskedInput) {
            const tolc = !this.formatToken; // To LowerCase?
            if (this.dateRangeSelection) {
                const min1 = asDate(this.minStartDate);
                const max1 = asDate(this.maxStartDate);
                const min2 = asDate(this.minEndDate);
                const max2 = asDate(this.maxEndDate);
                if (this.formatToken) {
                    enableDatePickerEditor(this._datetext1, this.formatToken, this._date1, min1, max1, tolc);
                    enableDatePickerEditor(this._datetext2, this.formatToken, this._date2, min2, max2, tolc);
                } else if (this._showTime) {
                    enableDatePickerEditor(this._datetext1, this._dtFmtNoTime, this._date1, stripTime(min1), stripTime(max1, true), tolc);
                    enableDatePickerEditor(this._datetext2, this._timeFormat, this._date1, min1, max1, tolc);
                    enableDatePickerEditor(this._datetext3, this._dtFmtNoTime, this._date2, stripTime(min2), stripTime(max2, true), tolc);
                    enableDatePickerEditor(this._datetext4, this._timeFormat, this._date2, min2, max2, tolc);
                } else {
                    enableDatePickerEditor(this._datetext1, this._dtFmtNoTime, this._date1, min1, max1, tolc);
                    enableDatePickerEditor(this._datetext2, this._dtFmtNoTime, this._date2, min2, max2, tolc);
                }
            } else {
                const min = asDate(this.min);
                const max = asDate(this.max);
                if (this.formatToken) {
                    enableDatePickerEditor(this._datetext1, this.formatToken, this._date1, min, max, tolc);
                } else {
                    enableDatePickerEditor(this._datetext1, this._dtFmtNoTime, this._date1, min, max, tolc);
                    if (this._showTime) {
                        enableDatePickerEditor(this._datetext2, this._timeFormat, this._date1, stripTime(min), stripTime(max, true), tolc);
                    }
                }
            }
        } else {
            this.shadowRoot.querySelectorAll('ptcs-textfield').forEach(el => removeDatePickerEditor(el));
            this._field1Text(this._date1, this._dtFmtNoTime);
            this._field2Text(this._date1, this._date2, this._showTime, this._twelveHourClock,
                this._dtFmtNoTime, this._timeFormat, this.formatToken);
            this._field3Text(this._date2, this._dtFmtNoTime);
            this._field4Text(this._date2, this._twelveHourClock, this._timeFormat);
        }
    }

    // Edit a date textfield:
    // - set and restore date format, if edit format differs from display format
    // - handle Enter and Escape keys
    // - send changed data to commit function on blur
    _editField(textfield, commitName, date) {
        this.clearValidityTimeout();
        const textfieldValid = !textfield.hasAttribute('invalid');
        const orgText = textfield.text;
        const orgDetaTextEditFormated = !this.formatToken && date && moment(orgText).format(this._editedDateFormat);
        const newText = !this._maskedInput && date instanceof Date && this._editedDateFormat !== this._dtFmtNoTime &&
            textfieldValid && moment(date).format(this._editedDateFormat);
        const undoText = (textfieldValid && orgDetaTextEditFormated) || orgText;
        if (newText && orgText !== newText) {
            textfield.text = newText; // Change date format so it can be edited
            textfield.performUpdate();
            textfield.selectAll();
        }

        const BLUR_FOCUS_TIMEOUT = 150; // the value should be at least MARK_VALIDITY_TIMEOUT on ptcs-behavior-validate.js
        const isEditKey = ev => {
            const regex = /^[a-zA-Z0-9]$/;
            const allowedKes = ['ArrowUp', 'ArrowDown', 'Delete', 'Backspace'];
            return regex.test(ev.key) || allowedKes.includes(ev.key);
        };
        const keyDown = ev => {
            if (isEditKey(ev)) {
                this.editing = true;
            }
            switch (ev.key) {
                case 'Enter':
                    // Store new value
                    ev.preventDefault();
                    this._forcesettingValidity = true;
                    textfield.blur(); // Leave edit field
                    this._forcesettingValidity = false;
                    setTimeout(() => {
                        textfield.focus(); // Restart editing
                    }, BLUR_FOCUS_TIMEOUT);
                    break;

                case 'Escape':
                    // Restore original value
                    if (textfield.text !== undoText) {
                        textfield.text = undoText;
                        ev.preventDefault();
                        this._forcesettingValidity = true;
                        textfield.blur(); // Leave edit field
                        this._forcesettingValidity = false;
                        setTimeout(() => {
                            textfield.focus(); // Restart editing
                        }, BLUR_FOCUS_TIMEOUT);
                    }
                    break;
            }
        };

        textfield.addEventListener('keydown', keyDown);

        textfield.addEventListener('blur', () => {
            this.setValidityTimeout();

            textfield.removeEventListener('keydown', keyDown);

            if (textfield.id === 'datetext4' && moment(this._datetext3.text, this._dtFmtNoTime, true).isValid() && !textfield.text) {
                // The end date time field was cleared: Set end date time to default end of day
                this._field4Text(moment(this._datetext3.text).hour(23).minute(59).second(59).millisecond(999).toDate(),
                    this._twelveHourClock, this._timeFormat);
            }

            if (this._maskedInput) {
                const datefield = hasValidDate(textfield) && getDate(textfield);
                if (!this._showTime && datefield) {
                    // Adjust timestamps coming from date editor to match convention applied from calendar
                    if (textfield.id === 'datetext') {
                        datefield.setHours(0, 0, 0, 0);
                    } else if (this.dateRangeSelection && textfield.id === 'datetext2') {
                        datefield.setHours(23, 59, 59, 999);
                    }
                }
                if (hasValidDate(textfield)) {
                    this[commitName](getDate(textfield)); // Commit changed value
                } else {
                    this[commitName](undefined); // Invalid value - reset date
                }
            } else if (textfield.text === newText) {
                textfield.text = orgText; // No change, restore date format
            } else if (textfield.text !== orgText) {
                this[commitName](); // Commit changed value
            }
        }, {once: true});
    }

    _modeChanged(ev) {
        // The dropdown has focus and blur listeners. When the dropdown list is opened, the dropdown loses focus and _stayUnvalidated
        //  becomes false via _editingChanged. Set editing to true to keep the unvalidated appearance while the dropdown list is open.
        // following change: as mode-changed event is getting called before the blur one, saveing the relevant event details to be
        //  evaluated on the blur one
        if (ev.detail.value === 'open') {
            this._modeChangedID = ev.target.id;
            this._modeChangedtimeStamp = ev.timeStamp;
        }
    }

    _dropdownFocus() {
        if (!this._opened) {
            this.clearValidityTimeout();
        }
    }

    _dropdownBlur(ev) {
        // as mode-changed event is getting called before the blur one, saveing the relevant event details to be
        //  evaluated on the blur one
        const _allowBlur = this._modeChangedID !== ev.target.id || (ev.timeStamp - this._modeChangedtimeStamp > 150);
        if (!this._opened && _allowBlur) {
            this.setValidityTimeout();
        }
    }

    clearValidityTimeout() {
        if (this._debounceValidityID) {
            clearTimeout(this._debounceValidityID);
            this._debounceValidityID = null;
        }
    }

    setValidityTimeout() {
        if (this._debounceValidityID) {
            clearTimeout(this._debounceValidityID);
            this._debounceValidityID = null;
        }
        if (this._forcesettingValidity) {
            this.editing = false;
            return;
        }
        this._debounceValidityID = setTimeout(() => {
            this._debounceValidityID = null;
            this.editing = false;
            this._forceValidation();
        }, 100);
    }

    _field1Focus(ev) {
        this._editField(ev.target, '_field1DecodeDate', this._date1);
    }

    _field2Focus(ev) {
        // Only set edit format if textfield contains a date
        this._editField(ev.target, '_field2DecodeDate', this.dateRangeSelection && !this._showTime && this._date2);
    }

    _field3Focus(ev) {
        this._editField(ev.target, '_field3DecodeDate', this._date2);
    }

    _field4Focus(ev) {
        this._editField(ev.target, '_field4DecodeDate');
    }

    // When decoding dates from the textfields
    _decodeFullDate(dateText, timeText, meridiem, editingDate) {
        const dateFormat = editingDate ? this._editedDateFormat : this._dtFmtNoTime;

        if (!dateText) {
            // No date. This is not valid
            return invalidMomentTime;
        }

        if (!timeText) {
            // No time. Only validate date - if time is hidden
            return moment(dateText, dateFormat, true);
        }

        if (this._twelveHourClock) {
            // Hack to stop invalid 12-hour clocks, because I'm tired of trying to figure out how to make moment do this instead
            if (!valid12hourTime(timeText) || !meridiem) {
                return invalidMomentTime;
            }

            // 12-hour clock
            return moment(`${dateText} ${timeText} ${meridiem}`, `${dateFormat} ${this._timeFormat.toLowerCase()} a`, true);
        }

        // 24-hour clock
        return moment(`${dateText} ${timeText}`, `${dateFormat} ${this._timeFormat}`, true);
    }

    _combine(date, time, meridiemId) {
        const time$ = moment(time || date);
        const date$ = moment(date || time).hour(time$.hour()).minute(time$.minute()).second(time$.second());
        const meridiem = this._twelveHourClock && this._getel(meridiemId).selectedValue;
        if (!meridiem) {
            return date$.toDate();
        }
        if (date$.format('a') === meridiem) {
            return date$.toDate(); // Already has correct meridiem
        }
        const dh = date$.hours() < 12 ? 12 : -12;
        date$.add(dh, 'h');
        if (date$.format('a') !== meridiem) {
            return date$.subtract(dh, 'h').toDate(); // Some kind of error ...
        }
        return date$.toDate();
    }

    _field1DecodeDate(date) {
        if (date) {
            if (this._showTime && !this.formatToken) {
                this._setPublicDates(this._combine(date, getDate(this._datetext2), 'meridiem'), this._date2);
            } else {
                this._setPublicDates(date, this._date2);
            }
            return;
        }
        const dateText = this._datetext1.text;
        const timeText = this._showTime && this._datetext2.text;
        const meridiem = timeText && this._twelveHourClock && this._getel('meridiem').selectedValue;
        const date$ = this._decodeFullDate(dateText, timeText, meridiem, true);

        if (!dateText || !date$.isValid()) {
            this._clearedField1();
            if (dateText) {
                this.__datetext1restore = dateText;
            }
        } else if (date$.isValid() && differentDates(this._date1, date$)) {
            this._setPublicDates(date$.toDate(), this._date2);
        }
    }

    _field2DecodeDate(date) {
        if (!this.dateRangeSelection && !this._showTime) {
            return; // textfield2 is not used in this mode
        }
        if (date) {
            if (this.dateRangeSelection && (!this._showTime || this.formatToken)) {
                this._setPublicDates(this._date1, date);
            } else {
                this._setPublicDates(this._combine(getDate(this._datetext1), date, 'meridiem'), this._date2);
            }
            return;
        }
        if (this._showTime) {
            const dateText = this._datetext1.text;
            const timeText = this._datetext2.text;
            const meridiem = this._twelveHourClock && this._getel('meridiem').selectedValue;
            const date$ = this._decodeFullDate(dateText, timeText, meridiem);

            if (!dateText || !date$.isValid()) {
                this._clearedField2();
                if (timeText) {
                    this.__datetext2restore = timeText;
                }
            } else if (date$.isValid() && differentDates(this._date1, date$)) {
                this._setPublicDates(date$.toDate(), this._date2);
            }
        } else if (this.dateRangeSelection) {
            const date$ = moment(this._datetext2.text, this._editedDateFormat, true);

            if (!this._datetext2.text || !date$.isValid()) {
                const dateText = this._datetext2.text;
                this._clearedField2();
                if (dateText) {
                    this.__datetext2restore = dateText;
                }
            } else if (date$.isValid() && differentDates(this._date2, date$.hour(23).minute(59).second(59))) {
                this._setPublicDates(this._date1, date$.toDate());
            }
        }
    }

    _field3DecodeDate(date) {
        if (!this.dateRangeSelection || !this._showTime || this.formatToken) {
            return; // textfield3 is not used in this mode
        }
        if (date) {
            this._setPublicDates(this._date1, this._combine(date, getDate(this._datetext4), 'meridiem2'));
            return;
        }
        const dateText = this._datetext3.text;
        const timeText = this._showTime && this._datetext4.text;
        const meridiem = timeText && this._twelveHourClock && this._getel('meridiem2').selectedValue;
        const date$ = this._decodeFullDate(dateText, timeText, meridiem, true);

        if (!dateText || !date$.isValid()) {
            this._clearedField3();
            if (dateText) {
                this.__datetext3restore = dateText;
            }
        } else if (date$.isValid() && differentDates(this._date2, date$)) {
            this._setPublicDates(this._date1, date$.toDate());
        }
    }

    _field4DecodeDate(date) {
        if (!this.dateRangeSelection || !this._showTime || this.formatToken) {
            return; // textfield4 is not used in this mode
        }
        if (date) {
            this._setPublicDates(this._date1, this._combine(getDate(this._datetext3), date, 'meridiem2'));
            return;
        }

        const dateText = this._datetext3.text;
        const timeText = this._datetext4.text;
        const meridiem = this._twelveHourClock && this._getel('meridiem2').selectedValue;
        const date$ = this._decodeFullDate(dateText, timeText, meridiem);

        if (!timeText || !date$.isValid()) {
            this._clearedField4();
            if (dateText) {
                this.__datetext4restore = dateText;
            }
        } else if (date$.isValid() && differentDates(this._date2, date$)) {
            this._setPublicDates(this._date1, date$.toDate());
        }
    }

    _forceValidation() {
        this.__validateNo++;
    }

    _insertValidationMessage(messageElement) {
        this.defaultInsertValidationMessageForVerticalLayout(messageElement);
    }

    _dfTabindex(_delegatedFocus, _noSpaceForMessage, forceHide = false) {
        return forceHide || _noSpaceForMessage ? '-1' : '0'; // _delegatedFocus;
    }

    _validDate(dateEl) {
        if (this._maskedInput) {
            return hasValidDate(dateEl, false);
        }
        return !dateEl.text || moment(dateEl.text, this._dtFmtNoTime, true).isValid();
    }

    _validTime(timeEl) {
        if (!timeEl.text) {
            return true;
        }
        if (this._maskedInput) {
            return hasValidDate(timeEl, false);
        }
        return (!this._twelveHourClock || valid12hourTime(timeEl.text)) &&
                moment(`2021-02-25 ${timeEl.text}`, `YYYY-MM-DD ${this._timeFormat}`, true).isValid();
    }

    _noValue(textfield) {
        return this._maskedInput ? !getDate(textfield) : !textfield.text;
    }

    _validateDatepicker(dateTime, fromDate, toDate, dateRangeSelection, _showTime, _displaySeconds, _twelveHourClock,
        required, min, max, minStartDate, maxStartDate, minEndDate, maxEndDate, maxRange, extraValidation) {
        const messages = [];

        // Start with true if validation is needed, otherwise undefiend
        const result = (required || (!dateRangeSelection && (min || max)) || (dateRangeSelection && maxRange) ||
            (dateRangeSelection && (minStartDate || maxStartDate || minEndDate || maxEndDate))) ? true : undefined;

        // Each field is considered valid if it is empty (at this stage)
        const datefields = {};

        datefields.datetext1 = this._validDate(this._datetext1);
        datefields.datetext2 = _showTime ? this._validTime(this._datetext2) : (!dateRangeSelection || this._validDate(this._datetext2));
        datefields.meridiem = datefields.datetext2;
        datefields.datetext3 = !(dateRangeSelection && _showTime && !this._validDate(this._datetext3));
        datefields.datetext4 = !(dateRangeSelection && _showTime && !this._validTime(this._datetext4));
        datefields.meridiem2 = datefields.datetext4;

        const datefieldVisible = datefield => datefield instanceof HTMLElement && datefield.style.display !== 'none';

        // Check if any of the given text fields are empty
        const someEmpty = (...txtFields) => txtFields.some(field => datefieldVisible(field) && (!field.hasText || field.text === field.hintText));

        // required
        if (required) {
            if ((dateRangeSelection && _showTime && someEmpty(this._datetext1, this._datetext2, this._datetext3, this._datetext4)) ||
                ((dateRangeSelection || _showTime) && someEmpty(this._datetext1, this._datetext2)) || (someEmpty(this._datetext1))) {
                messages.push(this.requiredMessage || false);
            }

            // Mark all empty textfields that should not be empty
            if (this._noValue(this._datetext1)) {
                datefields.datetext1 = false;
            }
            if (dateRangeSelection && this.formatToken && this._noValue(this._datetext2)) {
                datefields.datetext2 = false;
            }
            if (!this.formatToken) {
                if ((_showTime || dateRangeSelection) && this._noValue(this._datetext2)) {
                    datefields.datetext2 = false;
                }
                if (dateRangeSelection && _showTime && this._noValue(this._datetext3)) {
                    datefields.datetext3 = false;
                }
                if (dateRangeSelection && _showTime && this._noValue(this._datetext4)) {
                    datefields.datetext4 = false;
                }
                if (_showTime && _twelveHourClock && !this._getel('meridiem').selectedValue) {
                    datefields.meridiem = false;
                }
                if (dateRangeSelection & _showTime && _twelveHourClock && !this._getel('meridiem2').selectedValue) {
                    datefields.meridiem2 = false;
                }
            }
        }

        const formatDate = d => {
            if (!d) {
                return '';
            }

            if (this.formatToken) {
                return moment(d).format(this.formatToken);
            }

            const dateOnly = moment(d).format(this._dtFmtNoTime);

            if ((d.getHours() === 0 && d.getMinutes() === 0) || !_showTime) {
                return dateOnly;
            }

            const timeFormat = this.twelveHourClock ? `${this._timeFormat.toLowerCase()} A` : this._timeFormat;
            const timeOnly = moment(d).format(timeFormat ? timeFormat : 'HH:mm');

            return `${dateOnly} ${timeOnly}`;
        };

        const stripSeconds = val => {
            const d = new Date(val);
            return _displaySeconds ? d.setMilliseconds(0) : d.setSeconds(0, 0);
        };

        // min/max constraints
        const validateMin = (value, minvalue, field1, field2, message) => {
            const _min = minvalue instanceof Date ? minvalue : new Date(minvalue);
            const decodedMessage = PTCS.replaceStringTokens(message, {value: formatDate(_min)});

            if (value) {
                const _value = value instanceof Date ? value : new Date(value);
                const failed = decodedMessage ? decodedMessage.join() : false;

                if (stripTime(_value) < stripTime(_min)) {
                    datefields[field1] = false;

                    messages.push(failed);
                } else if (_showTime && stripSeconds(_value) < stripSeconds(_min)) {
                    if (field2) {
                        datefields[field2] = false;
                    }

                    messages.push(failed);
                }
            }
        };

        const validateMax = (value, maxvalue, field1, field2, message) => {
            const _max = maxvalue instanceof Date ? maxvalue : new Date(maxvalue);
            const decodedMessage = PTCS.replaceStringTokens(message, {value: formatDate(_max)});

            if (value) {
                const _value = value instanceof Date ? value : new Date(value);
                const failed = decodedMessage ? decodedMessage.join() : false;

                if (stripTime(_value) > stripTime(_max)) {
                    datefields[field1] = false;

                    messages.push(failed);
                } else if (_showTime && stripSeconds(_value) > stripSeconds(_max)) {
                    if (field2) {
                        datefields[field2] = false;
                    }

                    messages.push(failed);
                }
            }
        };

        const validateRange = (valueFrom, valueTo, valueRange, field1, field2, message) => {
            const _valueFrom = stripTime(valueFrom instanceof Date ? valueFrom : new Date(valueFrom));
            const _valueTo = stripTime(valueTo instanceof Date ? valueTo : new Date(valueTo));
            const decodedMessage = PTCS.replaceStringTokens(message, {value: valueRange});
            const failed = decodedMessage ? decodedMessage.join() : false;
            const valueRangeTH = valueRange * 86400000;// 1000 (ms) * 60 (sec) * 60 (min) * 24 (hour)

            if ((_valueTo - _valueFrom) > valueRangeTH) {
                datefields[field1] = false;
                datefields[field2] = false;
                messages.push(failed);
            }
        };

        if (dateRangeSelection) {
            if (minStartDate) {
                validateMin(fromDate, minStartDate, 'datetext1', 'datetext2', this.minStartDateFailureMessage);
            }

            if (maxStartDate) {
                validateMax(fromDate, maxStartDate, 'datetext1', 'datetext2', this.maxStartDateFailureMessage);
            }

            if (minEndDate) {
                validateMin(toDate, minEndDate, _showTime ? 'datetext3' : 'datetext2', _showTime ? 'datetext4' : '', this.minEndDateFailureMessage);
            }

            if (maxEndDate) {
                validateMax(toDate, maxEndDate, _showTime ? 'datetext3' : 'datetext2', _showTime ? 'datetext4' : '', this.maxEndDateFailureMessage);
            }

            if (maxRange && fromDate && toDate) {
                validateRange(fromDate, toDate, maxRange, 'datetext1', _showTime ? 'datetext3' : 'datetext2', this.maxRangeFailureMessage);
            }
        } else {
            if (min) {
                validateMin(dateTime, min, 'datetext1', 'datetext2', this.minFailureMessage);
            }

            if (max) {
                validateMax(dateTime, max, 'datetext1', 'datetext2', this.maxFailureMessage);
            }
        }

        const _set = (el, bvalue) => (el && PTCS.setbattr(el, 'invalid', bvalue));
        const setinvalid = (id, bvalue) => _set(this.shadowRoot.getElementById(id), bvalue);

        // Mark each field
        setinvalid('datetext', !datefields.datetext1);
        setinvalid('datetext2', !datefields.datetext2);
        setinvalid('meridiem', !datefields.datetext2 || !datefields.meridiem);
        setinvalid('datetext3', !datefields.datetext3);
        setinvalid('datetext4', !datefields.datetext4);
        setinvalid('meridiem2', !datefields.datetext4 || !datefields.meridiem2);

        if (messages.length) {
            return messages;
        }

        // If any textfield is invalid, then the validation must fail
        if (result && (!datefields.datetext1 || !datefields.datetext2 || !datefields.meridiem || !datefields.datetext3 || !datefields.datetext4 ||
            !datefields.meridiem2)) {
            return false;
        }

        // Not yet invalid. Leave final say to custom validation, if any
        return typeof extraValidation === 'function' ? extraValidation(this) : result;
    }

    set intervalType(value) {
        if (value === this.__intervalType$) {
            return;
        }

        this.__intervalType$ = value;
        this._observeInterval();
    }

    get intervalType() {
        return this.__intervalType$;
    }

    set interval(value) {
        if (value === this.__interval$) {
            return;
        }

        this.__interval$ = value;
        this._observeInterval();
    }

    get interval() {
        return this.__interval$;
    }
};

customElements.define(PTCS.Datepicker.is, PTCS.Datepicker);
