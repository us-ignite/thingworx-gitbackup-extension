import {LitElement, html, css} from 'lit';
import {repeat} from 'lit/directives/repeat.js';
import {when} from 'lit/directives/when.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-hbar/ptcs-hbar.js';
import 'ptcs-vbar/ptcs-vbar.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-icons/cds-icons.js';
import 'ptcs-textfield/ptcs-textfield.js';
import 'ptcs-combobox/ptcs-combobox.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import moment from 'ptcs-moment/moment-import.js';

// Convert date to Date
function asDate(date) {
    if (!date) {
        return undefined;
    }
    if (date instanceof Date) {
        return date;
    }
    const date$ = moment(date);
    return date$.isValid() ? date$.toDate() : undefined;
}

PTCS.DatepickerCalendar = class extends PTCS.BehaviorFocus(PTCS.BehaviorStyleable(LitElement)) {
    static get styles() {
        return css`
            :host {
                display: inline-block;
                position: absolute;
                z-index: 99996;
            }

            :host([hidden]) {
                display: none;
            }

            /* Unthemable (without a part name...) */
            [part=datepicker-container] #divider {
                width: 285px;
                border: 0;
                border-top: 1px solid #d8d8de;
                margin-top: 16px;
            }

            [part=weekdays] {
                display: grid;
                grid-template-columns: repeat(7, minmax(0, 1fr));
            }

            [part=weekday] {
                box-sizing: border-box;
                user-select: none;
            }

            [part=days] {
                flex-grow: 1;
                display: grid;
                grid-template-columns: repeat(7, minmax(0, 1fr));
                user-select: none;
            }

            [part=day] {
                display: flex;
                justify-content: center;
                align-items: center;
                box-sizing: border-box;
                text-transform: none;
            }

            [part=days][any-data] [part=day] {
                flex-direction: column;
            }

            [part=day] {
                cursor: pointer;
            }

            [part=day][disabled] {
                cursor: default;
            }

            [part=day][out-of-range] {
                cursor: not-allowed;
            }

            /* Ugly hack to increase button hit area */
            .add-hit-area {
                min-height: 34px;
                min-width: 34px;
                align-self: center;
                margin-top: 20px;
            }

            .add-hit-area ptcs-button {
                margin: 8px;
            }

            [part=prev-month-button],
            [part=next-month-button] {
                flex: none;
            }

            [part~=month-dropdown] {
                width: calc(100% + 30px);
                margin: 4px;
            }

            [part~=year-dropdown] {
                width: calc(100% - 30px);
                margin: 4px;
            }

            [part~=hour-dropdown],
            [part~=minute-dropdown],
            [part~=second-dropdown],
            [part~=meridiem-dropdown] {
                width: 100%;
            }

            ptcs-hbar.reverse {
                flex-direction: row-reverse !important;
                justify-content: flex-start !important;
            }

            [part=header] {
                display: flex;
                flex-flow: row nowrap;
                place-content: center space-between;
                align-items: flex-end;
            }

            [part=time-controls] {
                display: flex;
                flex-flow: row nowrap;
                place-content: center flex-start;
                align-items: flex-end;
            }

            [part=time] {
                align-items: stretch !important;
            }

            [part=day] [part=dot] {
                visibility: hidden;
            }

            [part=day] [part=dot][visible] {
                visibility: visible;
            }

            [part=days][any-data] [part=day] [part=dot] {
                display: block;
            }

            [part=dot] {
                display: none;
            }
        `;
    }

    render() {
        return html`
            <div part="datepicker-container" ?date-only=${this.dateOnly}>
                <div part="header">
                    <div class="add-hit-area" @click=${this._previousMonthClick}>
                        <ptcs-button part="prev-month-button" id="prev" variant="small" .icon=${this.iconBackward}
                        tabindex=${this._delegatedFocus}></ptcs-button>
                    </div>
                    <ptcs-combobox part="month-dropdown time-units-label" id="currmonth" .items=${this._months} .text=${this._selectedMonth}
                        .label=${this.monthLabel} .noMatchesLabel=${this.noMatchesLabel} exportparts=${this._exportmonth}
                        selector="name" type-ahead tabindex=${this._delegatedFocus} undo-edit-on-blur hint-text=""
                        @text-changed=${this._selectedMonthChangeEvent}></ptcs-combobox>
                    <ptcs-combobox part="year-dropdown time-units-label" id="curryear" .items=${this._years} .text=${this._selectedYear}
                        .label=${this.yearLabel} selector="name" value-selector="year" exportparts=${this._exportyear} match-pattern="^[\\d]{4}$"
                        no-matches-label="" undo-on-error type-ahead value-input hint-text="" tabindex=${this._delegatedFocus}
                        @text-changed=${this._selectedYearChangeEvent}></ptcs-combobox>
                    <div class="add-hit-area" @click=${this._nextMonthClick}>
                        <ptcs-button part="next-month-button" id="next" variant="small" .icon=${this.iconForward}
                        tabindex=${this._delegatedFocus}></ptcs-button>
                    </div>
                </div>
                <div part="weekdays" id="weekdays">
                    ${this._weekdays.map((_day) => html`
                        <ptcs-label part="weekday" label=${_day} horizontal-alignment="center"></ptcs-label>
                    `)}
                </div>
                <div part="days" id="days" tabindex=${this._delegatedFocus} ?any-data=${this._anyData(this.datePresentedByDots)}
                    @mousedown=${this._selectDay} @dblclick=${this._setDay}>
                    ${repeat(this._days, (item, index) => index, (item, index) => html`
                        <div part="day" ?selected=${item.selected} ?range=${item.range} ?out-of-range=${item.outOfRange}
                            ?disabled=${item.disabled} ?focused=${this._isFocused(index, this._focusedDay)} .index = ${index}>${item.date}
                            <span part="dot" ?visible=${item.data}></span>
                        </div>
                    `)}
                </div>
                ${when(!this.dateOnly, () => html`
                    <ptcs-vbar part="time">
                        ${when(this.calendarStartTimeLabel && this.dateRangeSelectionCalendar, () => html`
                            <ptcs-label label=${this.calendarStartTimeLabel} part="start-time-label"></ptcs-label>
                        `)}
                        <div part="time-controls">
                            <ptcs-combobox part="hour-dropdown time-units-label" id="currhour" .label=${this.hoursLabel} .items=${this._hours}
                                type-ahead .text=${this._selectedHour} selector="displayName" hint-text="hh" undo-edit-on-blur
                                exportparts=${this._exporthour} tabindex=${this._delegatedFocus} .noMatchesLabel=${this.noMatchesLabel}
                                @text-changed=${this._selectedHourChangeEvent}></ptcs-combobox>
                            <ptcs-combobox part="minute-dropdown time-units-label" id="currminute" .label=${this.minutesLabel} .items=${this._minutes}
                                type-ahead .text=${this._selectedMinute} selector="displayName" hint-text="mm" undo-edit-on-blur
                                exportparts=${this._exportminute} tabindex=${this._delegatedFocus} .noMatchesLabel=${this.noMatchesLabel}
                                @text-changed=${this._selectedMinuteChangeEvent}></ptcs-combobox>
                            ${when(this.displaySeconds, () => html`
                                <ptcs-combobox part="second-dropdown time-units-label" id="currsecond" .label=${this.secondsLabel}
                                    .items=${this._seconds} type-ahead .text=${this._selectedSecond} selector="displayName" undo-edit-on-blur
                                    exportparts=${this._exportsecond} tabindex=${this._delegatedFocus} .noMatchesLabel=${this.noMatchesLabel}
                                    hint-text="ss" @text-changed=${this._selectedSecondChangeEvent}></ptcs-combobox>
                            `)}
                            ${when(this.twelveHourClock, () => html`
                                <ptcs-combobox part="meridiem-dropdown time-units-label" id="meridiem" .items=${this.meridiemStrings}
                                    .text=${this._selectedMeridiem} type-ahead .label=${this.meridiemLabel} exportparts=${this._exportmeridiem}
                                    undo-edit-on-blur hint-text="" tabindex=${this._delegatedFocus}
                                    @text-changed=${this._selectedMeridiemChangeEvent}></ptcs-combobox>
                            `)}
                        </div>
                        ${when(this.dateRangeSelectionCalendar, () => html`
                            <ptcs-label part="end-time-label" .label=${this.calendarEndTimeLabel}></ptcs-label>
                            <div part="time-controls">
                                <ptcs-combobox part="hour-dropdown time-units-label" id="currhour2" .label=${this.hoursLabel} .items=${this._hours}
                                    type-ahead .text=${this._selectedHour2} selector="displayName" hint-text="hh" undo-edit-on-blur
                                    exportparts=${this._exporthour} tabindex=${this._delegatedFocus} .noMatchesLabel=${this.noMatchesLabel}
                                    @text-changed=${this._selectedHour2ChangeEvent}></ptcs-combobox>
                                <ptcs-combobox part="minute-dropdown time-units-label" id="currminute2" .label=${this.minutesLabel}
                                    .items=${this._minutes} type-ahead .text=${this._selectedMinute2} selector="displayName" hint-text="mm"
                                    undo-edit-on-blur exportparts=${this._exportminute} .noMatchesLabel=${this.noMatchesLabel}
                                    tabindex=${this._delegatedFocus} @text-changed=${this._selectedMinute2ChangeEvent}></ptcs-combobox>
                                ${when(this.displaySeconds, () => html`
                                    <ptcs-combobox part="second-dropdown time-units-label" id="currsecond2" .label=${this.secondsLabel}
                                        .items=${this._seconds} type-ahead .text=${this._selectedSecond2} selector="displayName" hint-text="ss"
                                        undo-edit-on-blur exportparts=${this._exportsecond} tabindex=${this._delegatedFocus}
                                        .noMatchesLabel=${this.noMatchesLabel}  @text-changed=${this._selectedSecond2ChangeEvent}></ptcs-combobox>
                                `)}
                                ${when(this.twelveHourClock, () => html`
                                    <ptcs-combobox part="meridiem-dropdown time-units-label" id="meridiem2" .items=${this.meridiemStrings}
                                        .text=${this._selectedMeridiem2} type-ahead .label=${this.meridiemLabel} exportparts=${this._exportmeridiem}
                                        undo-edit-on-blur hint-text="" tabindex=${this._delegatedFocus}
                                        @text-changed=${this._selectedMeridiem2ChangeEvent}></ptcs-combobox>
                                `)}
                            </div>
                        `)}
                        <div>
                            <hr id="divider" noshade>
                        </div>
                    </ptcs-vbar>
                `)}
                <ptcs-hbar part="footer" end class=${this._clsButtons(this.actionPosition)}>
                    <ptcs-button part="apply-button" id="apply" variant="primary" exportparts=${this._exportapply} @click=${this._submit}
                        .label=${this.selectLabel} tabindex=${this._delegatedFocus}
                        ?disabled=${!this._isFromAndToSelected(this.dateRangeSelectionCalendar, this.date1, this.date2)}></ptcs-button>
                    <ptcs-button part="cancel-button" id="cancel" .label=${this.cancelLabel} variant="tertiary" @click=${this._cancel}
                        exportparts=${this._exportcancel} tabindex=${this._delegatedFocus}></ptcs-button>
                </ptcs-hbar>
            </div>
        `;
    }

    static get is() {
        return 'ptcs-datepicker-calendar';
    }

    static get properties() {
        return {
            // Select single date or range of dates?
            dateRangeSelectionCalendar: {
                type: Boolean
            },

            // Primary date: selected date or range start date (when dateRangeSelectionCalendar)
            date1: {
                type: Date
            },

            // Secondary date: End range date (when dateRangeSelectionCalendar, otherwise ignored)
            date2: {
                type: Date
            },

            disabled: {
                type:    Boolean,
                reflect: true
            },

            // Calendar data
            _days: {
                type:  Array,
                state: true
            },

            _selectedYear: {
                type:  String,
                state: true
            },

            _selectedMonth: {
                type:  String,
                state: true
            },

            _selectedHour: {
                type:  String,
                state: true
            },

            _selectedMinute: {
                type:  String,
                state: true
            },

            _selectedSecond: {
                type:  String,
                state: true
            },

            _selectedHour2: {
                type:  String,
                state: true
            },

            _selectedMinute2: {
                type:  String,
                state: true
            },

            _selectedSecond2: {
                type:  String,
                state: true
            },

            _focusedDay: {
                type:  Number,
                state: true
            },

            // AM/PM 12-hour clock?
            twelveHourClock: {
                type:       Boolean,
                attribute:  'twelve-hour-clock',
                hasChanged: (_new, _old) => (_new !== _old && _old !== undefined),
                reflect:    true
            },

            // am / pm indication of the calendar (when twelveHourClock is true)
            _selectedMeridiem: {
                type:  String,
                state: true
            },

            _selectedMeridiem2: {
                type:  String,
                state: true
            },

            // am / pm meridiemStrings for the 12-hour clock dropdown
            meridiemStrings: {
                type: Array
            },

            // Show only date - or show time too?
            dateOnly: {
                type: Boolean
            },

            displaySeconds: {
                type:      Boolean,
                attribute: 'display-seconds',
                reflect:   true
            },

            yearRange: {
                type: Number
            },

            min: {
                type: Date
            },

            max: {
                type: Date
            },

            minStartDate: {
                type: Date
            },

            maxStartDate: {
                type: Date
            },

            minEndDate: {
                type: Date
            },

            maxEndDate: {
                type: Date
            },

            maxRange: {
                type: Number
            },

            actionPosition: {
                type: String
            },

            weekStart: {
                type: String
            },

            iconBackward: {
                type: String
            },

            iconForward: {
                type: String
            },

            _delegatedFocus: {
                type: String
            },

            noMatchesLabel: {
                type: String
            },

            // buttons
            selectLabel: {
                type: String
            },

            cancelLabel: {
                type: String
            },

            monthLabel: {
                type: String
            },

            yearLabel: {
                type: String
            },

            hoursLabel: {
                type: String
            },

            minutesLabel: {
                type: String
            },

            secondsLabel: {
                type: String
            },

            meridiemLabel: {
                type: String
            },

            calendarStartTimeLabel: {
                type: String
            },

            calendarEndTimeLabel: {
                type: String
            },

            datePresentedByDots: {
                type: Set
            },

            _exportmonth: {
                type:  String,
                state: true
            },

            _exportyear: {
                type:  String,
                state: true
            },

            _exporthour: {
                type:  String,
                state: true
            },

            _exportminute: {
                type:  String,
                state: true
            },

            _exportsecond: {
                type:  String,
                state: true
            },

            _exportmeridiem: {
                type:  String,
                state: true
            },

            _exportapply: {
                type:  String,
                state: true
            },

            _exportcancel: {
                type:  String,
                state: true
            }
        };
    }

    constructor() {
        super();

        this.disabled = false;
        this._years = [];
        this._months = moment.localeData().months().map((m => ({name: m})));
        this._days = [];
        this._hours = PTCS.DatepickerCalendar.rangeTime(0, 23, {name: 'hour', format: 'HH'});
        this._minutes = PTCS.DatepickerCalendar.rangeTime(0, 59, {name: 'minute', format: 'mm'});
        this._seconds = PTCS.DatepickerCalendar.rangeTime(0, 59, {name: 'second', format: 'ss'});
        this.twelveHourClock = false;
        this.dateOnly = true;
        this.yearRange = 10;
        this.actionPosition = 'left';
        this.weekStart = 'Monday';
        this.iconBackward = 'cds:icon_chevron_left_mini';
        this.iconForward = 'cds:icon_chevron_right_mini';
        this._delegatedFocus = null;
        this._exportmonth = PTCS.exportparts('month-dropdown-', PTCS.ComboBox);
        this._exportyear = PTCS.exportparts('year-dropdown-', PTCS.ComboBox);
        this._exporthour = PTCS.exportparts('hour-dropdown-', PTCS.ComboBox);
        this._exportminute = PTCS.exportparts('minute-dropdown-', PTCS.ComboBox);
        this._exportsecond = PTCS.exportparts('second-dropdown-', PTCS.ComboBox);
        this._exportmeridiem = PTCS.exportparts('meridiem-dropdown-', PTCS.ComboBox);
        this._exportapply = PTCS.exportparts('apply-button-', PTCS.Button);
        this._exportcancel = PTCS.exportparts('cancel-button-', PTCS.Button);

        const date = moment().set({hour: 0, minute: 0, second: 0, millisecond: 0}).toDate();
        this.gotoTime(date); // initiate the days, month and years
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (changedProperties.has('weekStart')) {
            this._weekdays = this._computeWeekDaysName();
        }
        if (changedProperties.has('date2')) {
            // updates _selectedYear, _selectedMonth, _selectedHour2, _selectedMinute2, _selectedSecond2, _selectedMeridiem2
            this._date2Changed();
        }
        if (['_selectedYear', 'yearRange', 'dateRangeSelectionCalendar', 'min', 'max', 'minStartDate',
            'maxStartDate', 'minEndDate', 'maxEndDate'].some(propName => changedProperties.has(propName))) {
            // updates _years
            this._computeYears();
        }
        if (changedProperties.has('twelveHourClock')) {
            // updates _selectedHour, _selectedHour2
            this._twelveHourClockChanged();
            this._hours = this.twelveHourClock
                ? PTCS.DatepickerCalendar.rangeTime(1, 12, {name: 'hour', format: 'hh'})
                : PTCS.DatepickerCalendar.rangeTime(0, 23, {name: 'hour', format: 'HH'});
        }
        if (['weekStart', '_selectedYear', '_selectedMonth', 'datePresentedByDots', 'dateRangeSelectionCalendar', 'min', 'max', 'minStartDate',
            'maxStartDate', 'minEndDate', 'maxEndDate', 'maxRange'].some(propName => changedProperties.has(propName))) {
            // updates _days, _focusedDay
            this._daysChanged();
        }
        if (['dateRangeSelectionCalendar', 'date1', 'date2', '_days'].some(propName => changedProperties.has(propName))) {
            // _days
            this._datesChanged();
        }
    }

    firstUpdated() {
        super.firstUpdated();

        this.addEventListener('keydown', this._keyDown.bind(this));
        this.addEventListener('mousedown', this._mouseDown.bind(this));
        this._trackFocus(this.shadowRoot.getElementById('days'), () =>
            this._focusedDay >= 0 ? this.shadowRoot.getElementById('days').children[this._focusedDay] : null);
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        // notify events
        if (changedProperties.has('date1')) {
            this.dispatchEvent(new CustomEvent('date1-changed', {detail: {value: this.date1}}));
            if (this.date1) {
                // Possible to set invalid date in preview, disable or restore the Select button
                this.shadowRoot.getElementById('apply').disabled = this.date1.toString() === 'Invalid Date';
            }
        }
        if (changedProperties.has('date2')) {
            this.dispatchEvent(new CustomEvent('date2-changed', {detail: {value: this.date2}}));
        }
    }

    set _selectedHour(value) {
        if (value === this.___selectedHour$) {
            return;
        }
        this.___selectedHour$ = value;
        this._timeChanged();
    }

    get _selectedHour() {
        return this.___selectedHour$;
    }

    set _selectedMinute(value) {
        if (value === this.___selectedMinute$) {
            return;
        }
        this.___selectedMinute$ = value;
        this._timeChanged();
    }

    get _selectedMinute() {
        return this.___selectedMinute$;
    }

    set _selectedSecond(value) {
        if (value === this.___selectedSecond$) {
            return;
        }
        this.___selectedSecond$ = value;
        this._timeChanged();
    }

    get _selectedSecond() {
        return this.___selectedSecond$;
    }

    set _selectedMeridiem(value) {
        if (value === this.___selectedMeridiem$) {
            return;
        }
        this.___selectedMeridiem$ = value;
        this._timeChanged();
    }

    get _selectedMeridiem() {
        return this.___selectedMeridiem$;
    }

    set _selectedHour2(value) {
        if (value === this.___selectedHour2$) {
            return;
        }
        this.___selectedHour2$ = value;
        this._time2Changed();
    }

    get _selectedHour2() {
        return this.___selectedHour2$;
    }

    set _selectedMinute2(value) {
        if (value === this.___selectedMinute2$) {
            return;
        }
        this.___selectedMinute2$ = value;
        this._time2Changed();
    }

    get _selectedMinute2() {
        return this.___selectedMinute2$;
    }

    set _selectedSecond2(value) {
        if (value === this.___selectedSecond2$) {
            return;
        }
        this.___selectedSecond2$ = value;
        this._time2Changed();
    }

    get _selectedSecond2() {
        return this.___selectedSecond2$;
    }

    set _selectedMeridiem2(value) {
        if (value === this.___selectedMeridiem2$) {
            return;
        }
        this.___selectedMeridiem2$ = value;
        this._time2Changed();
    }

    get _selectedMeridiem2() {
        return this.___selectedMeridiem2$;
    }

    _isFocused(index, focusedDay) {
        return index === focusedDay;
    }

    _clsButtons(actionPosition) {
        return actionPosition.toLowerCase() === 'right' ? 'reverse' : '';
    }

    _anyData(datePresentedByDots) {
        return datePresentedByDots && datePresentedByDots.size;
    }

    _date2Changed(date2) {
        if (this.date2) {
            const d2 = x => x < 10 ? `0${Number(x)}` : x;
            const hours = `${d2(this.date2.getHours())}`;
            const minutes = `${d2(this.date2.getMinutes())}`;
            if (this._selectedHour2 !== hours || this._selectedMinute2 !== minutes) {
                this.gotoTime(this.date2, true);
            }
        } else {
            // Reset the End time dropdown selections
            this._date2Clear();
        }
    }

    capitalize(s) {
        return s.charAt(0).toUpperCase() + s.slice(1);
    }

    get firstFocusable() {
        return this.shadowRoot.getElementById('prev');
    }

    get lastFocusable() {
        return this.shadowRoot.querySelector('[part=cancel-button]');
    }

    focusOnOpen() {
        this._syncComboboxText();
        const _focusedDaySelected = this._days.findIndex(d => d.selected);
        this._focusedDay = _focusedDaySelected !== -1 ? _focusedDaySelected : this._days.findIndex(d => !d.disabled);
        this.shadowRoot.getElementById('days').focus();
    }

    _getDate$(year, month, day, hour = 0, minute = 0, second = 0, meridiem = false) {
        const d2 = x => x < 10 ? `0${Number(x)}` : x;
        const date = `${year}-${d2(month)}-${d2(day)}`;
        const format = 'YYYY-MMMM-DD';

        if (this.dateOnly) {
            return moment(date, format, true);
        }

        if (meridiem && hour === 0) {
            hour = 12; // Just in case...
        }

        const [hh, am, a] = meridiem ? ['hh', ` ${meridiem}`, ' a'] : ['HH', '', ''];
        const time = `${d2(hour)}:${d2(minute)}`;

        const r = this.displaySeconds
            ? moment(`${date} ${time}:${d2(second)}${am}`, `${format} ${hh}:mm:ss${a}`, true)
            : moment(`${date} ${time}${am}`, `${format} ${hh}:mm${a}`, true);

        if (r.isValid()) {
            return r;
        }

        // The date is not valid?
        throw this.displaySeconds
            ? `${date} ${time}:${d2(second)}${am} + ${format} ${hh}:mm:ss${a}`
            : `${date} ${time}${am} + ${format} ${hh}:mm${a}`;
    }

    _getSelectedMonth$() {
        return this._getDate$(this._selectedYear, this._selectedMonth, 1);
    }

    _getSelectedDate$(ixDay) {
        const day = this._days[ixDay].date || 1;
        if (this.dateOnly) {
            return this._getDate$(this._selectedYear, this._selectedMonth, day);
        }
        if (this.dateRangeSelectionCalendar && !this.dateOnly && this.date1) {
            // Date time range picker, date1 already exists, we are selecting the end range date - retrieve End Time settings.
            if (this._selectedHour2 && this._selectedMinute2) {
                return this._getDate$(this._selectedYear, this._selectedMonth, day,
                    this._selectedHour2, this._selectedMinute2, this._selectedSecond2, this.twelveHourClock && this._selectedMeridiem2);
            }
        }
        return this._getDate$(this._selectedYear, this._selectedMonth, day,
            this._selectedHour, this._selectedMinute, this._selectedSecond, this.twelveHourClock && this._selectedMeridiem);
    }

    gotoTime(date, time2) {
        this._settingTimeDropdowns = true;
        const m = moment(date);
        this._selectedYear = m.format('YYYY');
        this._selectedMonth = m.format('MMMM');
        if (time2) {
            this._selectedHour2 = m.format(this.twelveHourClock ? 'hh' : 'HH');
            this._selectedMinute2 = m.format('mm');
            this._selectedSecond2 = m.format('ss');
            this._selectedMeridiem2 = m.format('a');
        } else {
            this._selectedHour = m.format(this.twelveHourClock ? 'hh' : 'HH');
            this._selectedMinute = m.format('mm');
            this._selectedSecond = m.format('ss');
            this._selectedMeridiem = m.format('a');
        }
        this._settingTimeDropdowns = false;
    }

    static range(start, end, type) {
        const data = [];
        const name = type.name;
        const curr = moment({name: start});
        for (let i = start; i <= end; i++) {
            curr.set(name, i);
            data.push({name: curr.format(type.format), year: `${i}`});
        }
        return data;
    }

    static rangeTime(start, end, type) {
        const data = [];
        const curr = moment({hour: 0});
        for (let i = start; i <= end; i++) {
            curr.set(type.name, i);
            data.push({name: i, displayName: curr.format(type.format)});
        }
        return data;
    }

    _expandYears(start, end) {
        if (start > end) {
            return undefined; // Better safe than sorry
        }
        const format = {name: 'year', format: 'YYYY'};
        const d = (date, alt) => date instanceof Date ? date.getFullYear() : alt;
        const numExpected = 1 + 2 * this.yearRange;
        const needMore = () => end - start + 1 < numExpected;

        // Min / max for whole range
        const min = d(asDate(this.dateRangeSelectionCalendar ? this.minStartDate : this.min), Number.MIN_SAFE_INTEGER);
        const max = d(asDate(this.dateRangeSelectionCalendar ? this.maxEndDate : this.max), Number.MAX_SAFE_INTEGER);
        const limited = min < max;

        // Gap limit
        const max1 = d(asDate(this.dateRangeSelectionCalendar && this.maxStartDate));
        const min2 = d(asDate(this.dateRangeSelectionCalendar && this.minEndDate));

        if (max1 < min2) {
            if (start > max1 && start < min2) {
                start = max1;
            }
            if (end > max1 && end < min2) {
                end = min2;
            }
            // The configuration defines a gap
            const hasGap = () => start <= max1 && min2 <= end;
            const needMoreEx = () => hasGap() ? end - start - min2 + max1 + 2 < numExpected : needMore();

            while (needMoreEx() && (!limited || start > min || end < max)) {
                if (!limited || start > min) {
                    if (start === min2) {
                        start = max1; // Jump over gap
                    } else {
                        start--;
                    }
                }
                if ((!limited || end < max) && needMoreEx()) {
                    if (end === max1) {
                        end = min2; // Jump over gap
                    } else {
                        end++;
                    }
                }
            }
            if (hasGap()) {
                return [...PTCS.DatepickerCalendar.range(start, max1, format), ...PTCS.DatepickerCalendar.range(min2, end, format)];
            }
        } else if (limited) {
            while (needMore() && (start > min || end < max)) {
                if (start > min) {
                    start--;
                }
                if (end < max && needMore()) {
                    end++;
                }
            }
        } else {
            // No limit - should not happen
            while (needMore()) {
                start--;
                if (needMore()) {
                    end++;
                }
            }
        }
        return PTCS.DatepickerCalendar.range(start, end, format);
    }

    _generateYears() {
        const _year = Number(this._selectedYear);
        if (isNaN(_year)) {
            return undefined;
        }

        const y = date => date instanceof Date ? date.getFullYear() : undefined;
        const d1 = (date, alt) => y(date) > alt ? y(date) : alt;
        const d2 = (date, alt) => y(date) < alt ? y(date) : alt;
        const min = d1(asDate(this.dateRangeSelectionCalendar ? this.minStartDate : this.min), Number.MIN_SAFE_INTEGER);
        const max = d2(asDate(this.dateRangeSelectionCalendar ? this.maxEndDate : this.max), Number.MAX_SAFE_INTEGER);
        const year = Math.min(Math.max(_year, min), max);
        return this._expandYears(Math.max(min, year - this.yearRange), Math.min(max, year + this.yearRange));
    }

    _computeYears() {
        if (!this._selectedYear) {
            return;
        }

        const newYears = this._generateYears();
        if (!Array.isArray(newYears) || PTCS.sameArray(newYears, this._years, (a, b) => a.name === b.name)) {
            return;
        }
        this._years = newYears;
    }

    _computeWeekDaysName() {
        // The chinese weekday strings each consist of three Unicode chars, the two first being identical. So just
        // displaying the first char of these doesn't work, we have to use the last one instead...
        const useLastChar = ['zh-tw', 'zh-cn'].indexOf(moment.locale()) !== -1;
        const weekDays = moment.weekdays().map(day => this.capitalize(useLastChar ? day.slice(-1) : day.slice(0, 1)));
        return this.weekStart.toLowerCase() === 'monday' ? [...weekDays.slice(1), weekDays[0]] : weekDays;
    }

    _outOfRangeFunc() {
        if (this.dateRangeSelectionCalendar) {
            const min1 = this.minStartDate && moment(this.minStartDate).startOf('day');
            const max1 = this.maxStartDate && moment(this.maxStartDate).endOf('day');
            const min2 = this.minEndDate && moment(this.minEndDate).startOf('day');
            const max2 = this.maxEndDate && moment(this.maxEndDate).endOf('day');
            // eslint-disable-next-line no-nested-ternary
            const maxRangeDate = this.maxRange && (!this.date1 && this.date2 ? this.date2 : (this.date1 && !this.date2 ? this.date1 : null));
            const maxRangeMin = moment(maxRangeDate).subtract(this.maxRange - 1, 'days').startOf('day');
            const maxRangeMax = moment(maxRangeDate).add(this.maxRange - 1, 'days').endOf('day');

            if (maxRangeDate ||
               ((min1 || max1 || min2 || max2) && (!min1 || !max2 || min1.isBefore(max2)) && (!max1 || !min2 || max1.isBefore(min2)))) {
                return day => {
                    // Outside of max range?
                    if (maxRangeDate && (day.isBefore(maxRangeMin) || day.isAfter(maxRangeMax))) {
                        return true;
                    }
                    // Outside of full range?
                    if ((min1 && day.isBefore(min1)) || (max2 && day.isAfter(max2))) {
                        return true;
                    }
                    // Inside the unselectable range?
                    if (max1 && min2 && day.isAfter(max1) && day.isBefore(min2)) {
                        return true;
                    }
                    return false;
                };
            }
        } else {
            const min = this.min && moment(this.min).startOf('day');
            const max = this.max && moment(this.max).endOf('day');
            if ((min || max) && (!min || !max || min.isBefore(max))) {
                return day => (min && day.isBefore(min)) || (max && day.isAfter(max));
            }
        }
        return () => false; // No days are out of range
    }

    // Changed month: need to regenerate the days element of the calendar
    _daysChanged() {
        if (!this._selectedYear || !this._selectedMonth) {
            return;
        }
        const currentDay = this._getDate$(this._selectedYear, this._selectedMonth, 1);
        const month = currentDay.month();
        const firstDayOfWeek = this.weekStart.toLowerCase() === 'monday' ? currentDay.isoWeekday(1).day() : currentDay.isoWeekday(0).day();

        // Check out-of-range days
        const outOfRange = this._outOfRangeFunc();

        if (isNaN(firstDayOfWeek)) {
            throw 'firstDayOfWeek - NaN'; // Error that occurs sometimes - and  causes an eternal loop if not detected...
        }

        while (currentDay.day() !== firstDayOfWeek) { // Lazy approach, but simple
            currentDay.subtract(1, 'day');
        }

        const checkIfTheDayHasAnyData = () => this.datePresentedByDots.has(moment(currentDay).format('YYYY-M-D'));

        // fill an array with dates and properties
        const days = [];

        for (let index = 0; index < 42; index++) {
            const disabled = month !== currentDay.month();

            // Reached a new week that doesn't belong to the current month?
            if (disabled && index > 0 && currentDay.day() === firstDayOfWeek) {
                break; // Yes. Abort...
            }

            days.push({
                disabled,
                date:       (this.dateRangeSelectionCalendar || !disabled) ? currentDay.date() : '',
                range:      false,
                outOfRange: !disabled && outOfRange(currentDay),
                selected:   false,
                data:       !disabled && this.datePresentedByDots && checkIfTheDayHasAnyData()
            });

            currentDay.add(1, 'days');
        }

        // If the first week is empty, remove from they from array
        this._days = days[7].date === 1 ? days.slice(7) : days;

        if (this._focusedDay >= 0) {
            if (this._focusedDay >= this._days.length || this._days[this._focusedDay].disabled) {
                if (this._focusedDay - 7 >= 0 && !this._days[this._focusedDay - 7].disabled) {
                    this._focusedDay = this._focusedDay - 7;
                } else {
                    this._focusedDay = this._focusedDay + 7;
                }
            }
        }
    }

    _datesChanged() {
        // Clear all selections
        const clearAll = () => this._days.forEach((item, index) => {
            this.set(`_days.${index}.selected`, false);
            this.set(`_days.${index}.range`, false);
        });

        // Mark one day as selected (and clear all other days)
        const selectOne = day => this._days.forEach((item, index) => {
            if (item.disabled || item.date !== day) {
                this.set(`_days.${index}.selected`, false);
                this.set(`_days.${index}.range`, false);
            } else {
                this.set(`_days.${index}.selected`, true);
                this.set(`_days.${index}.range`, true);
            }
        });

        const eqMonth = (date1, date2) => date1 && date2 && date1.getFullYear() === date2.getFullYear() && date1.getMonth() === date2.getMonth();
        const eqDay = (date1, date2) => eqMonth(date1, date2) && date1.getDate() === date2.getDate();
        // Clear time
        const dateOnly = (date) => {
            const clearedDate = new Date(date);
            clearedDate.setHours(0, 0, 0, 0);
            return clearedDate;
        };

        // Mark selection range
        const selectRange = () => this._days.forEach((item, index) => {
            if (item.disabled) {
                this.set(`_days.${index}.selected`, false);
                this.set(`_days.${index}.range`, false);
            } else {
                const date = this._getSelectedDate$(index).toDate();
                this.set(`_days.${index}.selected`, dateOnly(this.date1) <= dateOnly(date) && dateOnly(date) <= dateOnly(this.date2));
                this.set(`_days.${index}.range`, eqDay(date, this.date1) || eqDay(date, this.date2));
            }
        });

        if (this.dateRangeSelectionCalendar) {
            if (this.date1) {
                if (this.date2) {
                    selectRange();
                } else if (eqMonth(this.date1, this._getSelectedMonth$().toDate())) {
                    selectOne(this.date1.getDate());
                }
            } else if (this.date2 && eqMonth(this.date2, this._getSelectedMonth$().toDate())) {
                selectOne(this.date2.getDate());
            } else {
                clearAll();
            }
        } else if (this.date1 && eqMonth(this.date1, this._getSelectedMonth$().toDate())) {
            selectOne(this.date1.getDate());
        } else {
            clearAll();
        }
    }

    _twelveHourClockChanged() {
        this._settingTimeDropdowns = true;
        try {
            if (this.dateRangeSelectionCalendar && !this.dateOnly && this._selectedHour2) {
                const time2$ = this._getDate$(this._selectedYear, this._selectedMonth, 1,
                    this._selectedHour2, this._selectedMinute2, this._selectedSecond2, !this.twelveHourClock && this._selectedMeridiem2);

                if (time2$.isValid()) {
                    this._selectedHour2 = time2$.format(this.twelveHourClock ? 'hh' : 'HH');
                }
            }
            const time$ = this._getDate$(this._selectedYear, this._selectedMonth, 1,
                this._selectedHour, this._selectedMinute, this._selectedSecond, !this.twelveHourClock && this._selectedMeridiem);

            if (time$.isValid()) {
                this._selectedHour = time$.format(this.twelveHourClock ? 'hh' : 'HH');
            }
        } finally {
            this._settingTimeDropdowns = false;
        }
    }

    _changeTime(date, hour, minute, second, meridiem) {
        return this._getDate$(date.getFullYear(), this._months[date.getMonth()].name, date.getDate(),
            hour, minute, second, meridiem).toDate();
    }

    // Observe dropdown selections that update the date properties
    _timeChanged() {
        if (this._settingTimeDropdowns) {
            return;
        }
        if (this.date1) {
            this.date1 = this._changeTime(this.date1, this._selectedHour, this._selectedMinute, this._selectedSecond,
                this.twelveHourClock && this._selectedMeridiem);
        }
    }

    // Observe dropdown selections that update the date2 properties
    _time2Changed() {
        if (this._settingTimeDropdowns) {
            return;
        }
        if (this.date2) {
            this.date2 = this._changeTime(this.date2, this._selectedHour2, this._selectedMinute2, this._selectedSecond2,
                this.twelveHourClock && this._selectedMeridiem2);
        }
    }

    _nextMonthClick() {
        this.shadowRoot.getElementById('next').focus();
        this._nextMonth();
    }

    _nextMonth() {
        this._setCalendarMonth(this._getSelectedMonth$().add(1, 'month'));
    }

    _nextYear() {
        this._setCalendarMonth(this._getSelectedMonth$().add(1, 'year'));
    }

    _previousMonthClick() {
        this.shadowRoot.getElementById('prev').focus();
        this._previousMonth();
    }

    _previousMonth() {
        this._setCalendarMonth(this._getSelectedMonth$().subtract(1, 'month'));
    }

    _previousYear() {
        this._setCalendarMonth(this._getSelectedMonth$().subtract(1, 'year'));
    }

    _setCalendarMonth(date) {
        const m = moment(date);

        this._selectedYear = String(m.year());
        this._selectedMonth = m.format('MMMM');
    }

    _selectDayIndex(ixDay) {
        if (!(ixDay >= 0) || this._days[ixDay].disabled || this._days[ixDay].outOfRange) {
            return;
        }

        this._focusedDay = ixDay;
        const date$ = this._getSelectedDate$(ixDay);
        const date = date$.toDate();

        if (this.dateRangeSelectionCalendar) {
            const date1 = this.dateOnly ? date$.startOf('day').toDate() : date;
            const date2 = this.dateOnly ? date$.endOf('day').toDate() : date;
            const adjustTime = (d, h = 0, m = 0, s = 0, ms = 0) => {
                d.setHours(h);
                d.setMinutes(m);
                d.setSeconds(s);
                d.setMilliseconds(ms);
                return d;
            };

            // If clicked in start range area, select date1
            // If clicked in end range area, select date2
            // If no date is selected, select date1
            // If one date is selected, then select the other - and make sure date1 < date2
            // If two dates are selected, then start new range from date1
            // Adjust start / end times when start / end dates are flipped and this.dateOnly is true
            if (date <= asDate(this.maxStartDate)) {
                this.date1 = date1;
            } else if (date >= asDate(this.minEndDate)) {
                this.date2 = date2;
            } else if (this.date1) {
                if (this.date2) {
                    if (this.dateRangeSelectionCalendar && !this.dateOnly) {
                        // Discard old range and start new range
                        this.date1 = this._reinitializeDateWithCurrentTime(date1);
                    } else {
                        this.date1 = date1;
                    }
                    this._date2Clear();
                } else if (this.date1 <= date) {
                    this.date2 = date2;
                } else {
                    [this.date1, this.date2] = [date1, this.date1];
                    if (this.dateOnly) {
                        [this.date1, this.date2] = [adjustTime(this.date1), adjustTime(this.date2, 23, 59, 59, 999)];
                    } else if (!this.dateOnly && this.dateRangeSelectionCalendar) {
                        this.date1 = this._reinitializeDateWithCurrentTime(this.date1);
                    }
                }
            } else if (this.date2) {
                if (date <= this.date2) {
                    this.date1 = date1;
                } else {
                    [this.date1, this.date2] = [this.date2, date2];
                    if (this.dateOnly) {
                        [this.date1, this.date2] = [adjustTime(this.date1), adjustTime(this.date2, 23, 59, 59, 999)];
                    }
                }
            } else {
                // Start new range
                this.date1 = date1;
            }
            this._daysChanged();
        } else {
            // Range selections not enabled
            this.date1 = date;
        }
    }

    _reinitializeDateWithCurrentTime(date) {
        const now = new Date();
        const seconds = this.displaySeconds ? now.getSeconds() : 0;

        const updatedDate = new Date(
            date.getFullYear(),
            date.getMonth(),
            date.getDate(),
            now.getHours(),
            now.getMinutes(),
            seconds
        );
        const m = moment(updatedDate);
        this._selectedHour = m.format(this.twelveHourClock ? 'hh' : 'HH');
        this._selectedMinute = m.format('mm');
        this._selectedSecond = m.format('ss');
        this._selectedMeridiem = m.format('a');
        return updatedDate;
    }

    _selectedMonthChangeEvent(ev) {
        this._selectedMonth = ev.detail.value;
    }

    _selectedYearChangeEvent(ev) {
        this._selectedYear = ev.detail.value;
    }

    _selectedHourChangeEvent(ev) {
        this._selectedHour = ev.detail.value;
    }

    _selectedMinuteChangeEvent(ev) {
        this._selectedMinute = ev.detail.value;
    }

    _selectedSecondChangeEvent(ev) {
        this._selectedSecond = ev.detail.value;
    }

    _selectedMeridiemChangeEvent(ev) {
        this._selectedMeridiem = ev.detail.value;
    }

    _selectedHour2ChangeEvent(ev) {
        this._selectedHour2 = ev.detail.value;
    }

    _selectedMinute2ChangeEvent(ev) {
        this._selectedMinute2 = ev.detail.value;
    }

    _selectedSecond2ChangeEvent(ev) {
        this._selectedSecond2 = ev.detail.value;
    }

    _selectedMeridiem2ChangeEvent(ev) {
        this._selectedMeridiem2 = ev.detail.value;
    }


    _selectDay(ev) {
        const el = ev.target.closest('[part=day]');
        if (el) {
            this._selectDayIndex(el.index);
        }
    }

    _setDay(ev) {
        const el = ev.target.closest('[part=day]');
        if (!el || el.hasAttribute('disabled') || el.hasAttribute('out-of-range')) {
            return;
        }
        this._selectDay(ev);
        this._submit();
    }

    _submit() {
        if (!this._isFromAndToSelected(this.dateRangeSelectionCalendar, this.date1, this.date2)) {
            return; // Invalid call
        }

        this.dispatchEvent(new CustomEvent('calendar-date-changed', {detail: {date1: this.date1, date2: this.date2, closeCalendar: true}}));
    }

    _cancel() {
        this.dispatchEvent(new CustomEvent('calendar-date-changed', {
            bubbles:  true,
            composed: true,
            detail:   {
                closeCalendar:         true,
                dateSelectionCanceled: true
            }
        }));
    }

    _syncComboboxText() {
        // User can enter arbitrary text in year combobox, and leave it in midst of editing via mouse click,
        // without committing the change. In such case the combobox text can differ from its textfield while
        // the datepicker preview still shows the currently selected year.
        const yearCombobox = this.shadowRoot.getElementById('curryear');
        const yearTextfield = yearCombobox.shadowRoot.getElementById('textfield');
        if (yearCombobox.text !== yearTextfield.text) {
            yearTextfield.text = yearCombobox.text;
        }
    }

    _mouseDown(ev) {
        // Cosmetic fix: This is to sync the visible text in year combobox back to selected year string if
        // user has clicked outside of the year combobox in midst of editing the year textfield.
        if (!ev.composedPath().includes(this.shadowRoot.getElementById('curryear'))) {
            this._syncComboboxText();
        }
    }

    _keyDown(ev) {
        if (this.disabled || ev.defaultPrevented) {
            return;
        }

        // Global keys
        switch (ev.key) {
            case 'Tab':
                if (ev.shiftKey && this.shadowRoot.activeElement === this.shadowRoot.getElementById('prev')) {
                    this.shadowRoot.querySelector('[part=cancel-button]').focus();
                    ev.preventDefault();
                } else if (!ev.shiftKey && this.shadowRoot.activeElement === this.shadowRoot.querySelector('[part=cancel-button]')) {
                    this.shadowRoot.getElementById('prev').focus();
                    ev.preventDefault();
                }
                break;
            case 'Enter':
                if (this.dateRangeSelectionCalendar) {
                    this._submit();
                } else {
                    this._selectDayIndex(this._focusedDay);
                    this._submit();
                }
                ev.preventDefault();
                break;
            case 'Escape':
                this._cancel();
                ev.preventDefault();
        }

        if (this.shadowRoot.activeElement !== this.shadowRoot.getElementById('days')) {
            return;
        }

        // Calendar days keys
        let fi = this._focusedDay;
        switch (ev.key) {
            case 'ArrowRight':
                fi++;
                if (fi >= this._days.length || this._days[fi].disabled) {
                    this._nextMonth();
                    fi = 0;
                    while (this._days[fi].disabled) {
                        fi++;
                    }
                }
                ev.preventDefault();
                break;
            case 'ArrowUp':
                fi -= 7;
                ev.preventDefault();
                break;
            case 'ArrowLeft':
                fi--;
                if (fi < 0 || (fi < 7 && this._days[fi].disabled)) {
                    this._previousMonth();
                    fi = this._days.length - 1;
                    while (this._days[fi].disabled) {
                        fi--;
                    }
                }
                ev.preventDefault();
                break;
            case 'ArrowDown':
                fi += 7;
                ev.preventDefault();
                break;
            case 'PageUp':
                if (ev.shiftKey) {
                    this._previousYear();
                } else {
                    this._previousMonth();
                }
                ev.preventDefault();
                break;
            case 'Home':
                // Start of week
                fi = Math.max(0, fi - (fi % 7));
                ev.preventDefault();
                break;
            case 'PageDown':
                if (ev.shiftKey) {
                    this._nextYear();
                } else {
                    this._nextMonth();
                }
                ev.preventDefault();
                break;
            case 'End':
                // End of week
                fi = Math.min(this._days.length, fi - (fi % 7) + 6);
                ev.preventDefault();
                break;
            case ' ':
                this._selectDayIndex(fi);
                ev.preventDefault();
                break;
        }

        // Check and fix limit errors
        if (fi < 0 || (fi < 7 && this._days[fi].disabled)) {
            fi = fi + 7;
        } else if (fi >= this._days.length || this._days[fi].disabled) {
            fi = fi - 7;
        }

        // Update focus
        if (fi !== this._focusedDay) {
            this._focusedDay = fi;
        }
    }

    _isFromAndToSelected(dateRangeSelectionCalendar, date1, date2) {
        return !dateRangeSelectionCalendar || (date1 || date2);
    }

    // Clear date2 and reset End time dropdowns (e.g. user initiated new range selection in calendar, or cleared end date
    // from datepicker controls before opening calendar)
    _date2Clear() {
        this.date2 = undefined;
        if (!this.dateOnly) {
            // Clear comboboxes for date2
            this._selectedHour2 = '';
            this._selectedMinute2 = '';
            this._selectedSecond2 = '';
            this._selectedMeridiem2 = '';
        }
    }
};

customElements.define(PTCS.DatepickerCalendar.is, PTCS.DatepickerCalendar);
