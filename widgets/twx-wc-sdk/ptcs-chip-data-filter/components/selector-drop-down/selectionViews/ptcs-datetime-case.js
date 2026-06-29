import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-datepicker/ptcs-datepicker.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import moment from 'ptcs-moment/moment-import.js';

const dropDownDefaultValue = 'within';
const operations = [
    {name: 'within', translationKey: 'stringWithinLast', label: 'within the last'},
    {name: 'between', translationKey: 'stringBetween', label: 'between'},
    {name: 'equals', translationKey: 'stringEquals', label: 'equal to'},
    {name: 'notEq', translationKey: 'stringNotEquals', label: 'not equal to'},
    {name: 'after', translationKey: 'stringAfter', label: 'after'},
    {name: 'afterEq', translationKey: 'stringAfterEq', label: 'after or equal to'},
    {name: 'before', translationKey: 'stringBefore', label: 'before'},
    {name: 'beforeEq', translationKey: 'stringBeforeEq', label: 'before or equal to'}
];
const dropDownWithinDefaultValue = 'h';
const withinUnits = [
    {name: 's', translationKey: 'stringSeconds', label: 'seconds'},
    {name: 'm', translationKey: 'stringMinuts', label: 'minutes'},
    {name: 'h', translationKey: 'stringHours', label: 'hours'},
    {name: 'd', translationKey: 'stringDays', label: 'days'},
    {name: 'w', translationKey: 'stringWeeks', label: 'weeks'},
    {name: 'M', translationKey: 'stringMonths', label: 'months'},
    {name: 'y', translationKey: 'stringYears', label: 'years'}
];

class PTCSDatetimeCase extends PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))) {

    static get styles() {
        return css`
                :host {
                    display: flex;
                    align-items: flex-end;
                }

                :host([hidden]) {
                    display: none;
                }

                :host([display="compact"]) {
                    width: 100%;
                    flex-direction: column;
                    align-items: flex-start;
                }

                :host([__current-selection-drop-down="between"][compact-mode]) [part=drop-down] {
                    align-self: flex-start;
                }

                #date-picker, #date-picker-to {
                    width: var(--ptcs-chip-data-filter-selector-dropdown-base-subcomponent-width);
                }

                :host([display="compact"]) #date-picker {
                    width: 100%;
                    margin-top: var(--ptcs-chip-data-filter-selector-subcomponent-compact-top-margin);
                    margin-right: 0px;
                }

                :host([display="compact"]) #date-picker-to {
                    width: 100%;
                    margin-top: var(--ptcs-chip-data-filter-selector-subcomponent-compact-top-margin);
                    margin-right: 0px;
                }

                #drop-down, #within-text-field, #within-drop-down {
                    width: var(--ptcs-chip-data-filter-selector-dropdown-number-case-subcomponent-width);
                }

                :host([display="compact"]) #drop-down {
                    width: 100%;
                }

                :host([display="compact"]) #within-text-field {
                    width: 100%;
                    margin-top: var(--ptcs-chip-data-filter-selector-subcomponent-compact-top-margin);
                }

                :host([display="compact"]) #within-drop-down {
                    width: 100%;
                    margin-top: var(--ptcs-chip-data-filter-selector-subcomponent-compact-top-margin);
                }

                #drop-down, #date-picker, #date-picker-to, #within-text-field, #within-drop-down {
                    margin-right: var(--subcomponent-margin-spacing);
                }

                span {
                    align-self: center;
                    margin: var(--subcomponent-margin-spacing) var(--subcomponent-margin-spacing) 0px 0px;
                }

                #within-container {
                    display: none;
                }

                #within-container[data-enabled] {
                    display: flex;
                }

                :host([display="compact"]) #within-container[data-enabled] {
                    flex-direction: column;
                    width: 100%;
                }

                #date-container {
                    display: none;
                }

                #date-container[data-enabled] {
                    display: flex;
                    align-items: flex-end;
                }

                :host([display="compact"]) #date-container[data-enabled] {
                    display: flex;
                    width: 100%;
                }

                #between-container {
                    display: none;
                }

                #between-container[data-enabled] {
                    display: flex;
                    align-items: flex-end;
                }

                ptcs-datepicker::part(label-container) {
                    display: none;
                }`;
    }

    render() {
        return html`<ptcs-dropdown id="drop-down" part="drop-down"
                .selectedValue=${this.__currentSelectionDropDown} .selector=${'label'} .valueSelector=${'name'}
                .label=${this.conditionLabel || this.dictionary.stringCondition}
                @selected-value-changed=${this._selectedValueChanged}
                @selected-indexes-changed=${this.__setIsFilled} tabindex=${this._delegatedFocus}
            ></ptcs-dropdown>
            <div id="date-container" ?data-enabled=${!this._isWithinMode}>
                <ptcs-datepicker
                    id="date-picker" part="date-picker"
                    .hintText=${this.dictionary.stringPleaseSelectDate}
                    .hoursLabel=${this.dictionary.stringHoursCap}
                    .minutesLabel=${this.dictionary.stringMinutsCap}
                    .secondsLabel=${this.dictionary.stringSecondsCap}
                    .selectLabel=${this.dictionary.stringSelect}
                    .cancelLabel=${this.dictionary.stringCancel}
                    show-time
                    display-seconds
                    .dateRangeSelection=${this._isRangeMode && this.compactMode}
                    .fromFieldLabel=${this.rangeStartValueLabel || this.dictionary.stringDate}
                    .fromTimeLabel=${this.dictionary.stringRangeStartTime || this.dictionary.stringTime}
                    .calendarStartTimeLabel=${this.startTimeValueLabel || this.dictionary.stringStartTime}
                    .calendarEndTimeLabel=${this.endTimeValueLabel || this.dictionary.stringEndTime}
                    .toFieldLabel=${this.rangeEndValueLabel || this.dictionary.stringToDate}
                    .toTimeLabel=${this.dictionary.stringRangeEndTime || this.dictionary.stringToTime}
                    .dateLabel=${this.dateValueLabel || this.dictionary.stringDate}
                    .timeLabel=${this.timeValueLabel || this.dictionary.stringTime}
                    .monthLabel=${this.dictionary.stringMonthsCap}
                    .yearLabel=${this.dictionary.stringYearsCap}
                    @selected-date-changed=${this.__getCurrentSelectedRange}
                    .formatToken=${this.formatToken}
                    .dateOrder=${this.dateOrder}
                    .daysContainingAnyData=${this.daysContainingAnyData}
                    tabindex=${this._delegatedFocus}
                ></ptcs-datepicker>
            </div>
            <div id="between-container" ?data-enabled=${this._expanded(this._isRangeMode, this.compactMode)}>
                <span part="between-to-span"> ${this.dictionary.stringTo} </span>
                <ptcs-datepicker
                    id="date-picker-to" part="date-picker"
                    @selected-date-changed=${this.__getCurrentSelectedRange}
                    .hintText=${this.dictionary.stringPleaseSelectDate}
                    .hoursLabel=${this.dictionary.stringHoursCap}
                    .minutesLabel=${this.dictionary.stringMinutsCap}
                    .secondsLabel=${this.dictionary.stringSecondsCap}
                    .selectLabel=${this.dictionary.stringSelect}
                    .cancelLabel=${this.dictionary.stringCancel}
                    show-time
                    display-seconds
                    .fromFieldLabel=${this.rangeStartValueLabel || this.dictionary.stringDate}
                    .fromTimeLabel=${this.rangeStartTimeValueLabel || this.dictionary.stringTime}
                    .calendarStartTimeLabel=${this.startTimeValueLabel || this.dictionary.stringStartTime}
                    .calendarEndTimeLabel=${this.endTimeValueLabel || this.dictionary.stringEndTime}
                    .toFieldLabel=${this.rangeEndValueLabel || this.dictionary.stringToDate}
                    .toTimeLabel=${this.rangeEndTimeValueLabel || this.dictionary.stringToTime}
                    .dateLabel=${this.dateValueLabel || this.dictionary.stringDate}
                    .timeLabel=${this.timeValueLabel || this.dictionary.stringTime}
                    .monthLabel=${this.dictionary.stringMonthsCap}
                    .yearLabel=${this.dictionary.stringYearsCap}
                    .formatToken=${this.formatToken}
                    .dateOrder=${this.dateOrder}
                    .daysContainingAnyData=${this.daysContainingAnyData}
                    tabindex=${this._delegatedFocus}
                </ptcs-datepicker>
            </div>
            <div id="within-container" ?data-enabled=${this._isWithinMode}>
                <ptcs-textfield id="within-text-field" part="text-field"
                  .label=${this.valueLabel || this.dictionary.stringValue}
                  .hintText=${this.dictionary.stringAddValue}
                  @text-changed=${this.__setIsFilled} @keyup=${this.__handleKeyUp}
                  tabindex=${this._delegatedFocus}
                ></ptcs-textfield>
                <ptcs-dropdown id="within-drop-down" part="drop-down" .label=${this.unitsLabel || this.dictionary.stringUnits}
                  .selectedValue=${this.__currentSelectionWithinDropDown} .selector=${'label'} .valueSelector=${'name'}
                  @selected-value-changed=${this._selectedWithinValueChanged}
                  @selected-indexes-changed=${this.__setIsFilled} tabindex=${this._delegatedFocus}
                ></ptcs-dropdown>
            </div>`;
    }

    static get is() {
        return 'ptcs-datetime-case';
    }

    static get properties() {
        return {
            dictionary: {
                type:     Object,
                observer: '__updateTranslations'
            },

            unitsLabel: {
                type: String
            },

            conditionLabel: {
                type: String
            },

            valueLabel: {
                type: String
            },

            dateValueLabel: {
                type: String},

            timeValueLabel: {
                type: String
            },

            startTimeValueLabel: {
                type: String
            },

            endTimeValueLabel: {
                type: String
            },

            rangeStartValueLabel: {
                type: String
            },

            rangeStartTimeValueLabel: {
                type: String
            },

            rangeEndValueLabel: {
                type: String
            },

            rangeEndTimeValueLabel: {
                type: String
            },

            isFilled: { // carries information whether the view is filled with enough amount of data
                readOnly: true,
                notify:   true,
                value:    false
            },

            display: {
                type:    String,
                value:   'compact',
                reflect: true
            },

            _delegatedFocus: {
                type:  String,
                value: null
            },

            formatToken: {
                type:      String,
                attribute: 'format-token'
            },

            dateOrder: {
                type:      String,
                attribute: 'date-order'
            },

            daysContainingAnyData: {
                type:      Array,
                attribute: 'days-containing-any-data'
            },

            __currentSelectionDropDown: {
                type:        String,
                reflect:     true,
                observer:    '__changeOperation',
                observeWhen: 'immediate'
            },

            __currentSelectionWithinDropDown: {
                type: String
            },

            _isRangeMode: {
                type:  Boolean,
                value: false
            },

            _isWithinMode: {
                type:  Boolean,
                value: false
            },

            compactMode: {
                type:      Boolean,
                reflect:   true,
                attribute: 'compact-mode'
            }
        };
    }

    constructor() {
        super();
        this.dateOrder = 'YMD'; //  auto, YMD, MDY, DMY
    }

    ready() {
        super.ready();
        this.__updateTranslations();
    }

    firstUpdated() {
        super.firstUpdated();
        this.setDefaultValues();
    }

    set dataEnteredByUser(newData) {
        if (!newData) {
            this.setDefaultValues();
            return;
        }
        if (!newData.operation) {
            console.warn('dataEnteredByUser: Missing operation. newData: ', newData);
            return;
        }
        this.__currentSelectionDropDown = newData.operation;
        switch (newData.operation) {
            case 'between':
                if (!newData.from || !newData.to) {
                    console.warn('dataEnteredByUser: Missing from/to. newData: ', newData);
                    return;
                }
                if (this.compactMode) {
                    // In compact mode we use a single range datetime picker
                    this.$['date-picker'].fromDate = newData.from;
                    this.$['date-picker'].toDate = newData.to;
                } else {
                    // In expanded mode we use separate datetime pickers (one each for each end of the range)
                    this.$['date-picker'].dateTime = newData.from;
                    this.$['date-picker-to'].dateTime = newData.to;
                }
                break;
            case 'within':
                if (!newData.value || !newData.units) {
                    console.warn('dataEnteredByUser: Missing from/to. newData: ', newData);
                    return;
                }
                this.$['within-text-field'].text = newData.value;
                this.$['within-drop-down'].selectedValue = newData.units;
                break;
            default:
                if (!newData.date) {
                    console.warn('dataEnteredByUser: Missing date. newData: ', newData);
                    return;
                }
                this.$['date-picker'].dateTime = newData.date;
        }
    }

    get dataEnteredByUser() {
        return this.__getCurrentData();
    }

    _selectedValueChanged(ev) {
        if (ev.detail.value) {
            this.__currentSelectionDropDown = ev.detail.value;
        }
    }

    _selectedWithinValueChanged(ev) {
        if (ev.detail.value) {
            this.__currentSelectionWithinDropDown = ev.detail.value;
        }
    }

    get query() {
        if (this.isError() || !this.__queryFieldName) {
            return null;
        }

        const parseToTimestamp = (day) => day < 0 ? Date.parse(moment(day).format('YYYYYY-MM-DDTHH:mm:ss.SSS')) : Date.parse(day);
        // https://tc39.es/ecma262/multipage/numbers-and-dates.html#sec-date.parse:
        // Date.parse(): Per the spec, only the date time string format is explicitly specified to be supported. The function first
        // attempts to parse the String according to the format described in Date Time String Format, including expanded years,
        // see https://tc39.es/ecma262/multipage/numbers-and-dates.html#sec-date-time-string-format.

        // TW-111017: For dates prior to Epoch boundary, use the Expanded Years format. See https://momentjs.com/docs/#/displaying/ and
        // https://tc39.es/ecma262/#sec-expanded-years

        const curDateObj = this.__getCurrentData();
        let retObj = {fieldName: this.__queryFieldName};

        switch (curDateObj.operation) {
            case 'between':
                retObj.type = 'BETWEEN';
                retObj.from = parseToTimestamp(curDateObj.from);
                retObj.to = parseToTimestamp(curDateObj.to);
                break;
            case 'equals':
                retObj.type = 'EQ';
                retObj.value = parseToTimestamp(curDateObj.date);
                break;
            case 'before':
                retObj.type = 'LT';
                retObj.value = parseToTimestamp(curDateObj.date);
                break;
            case 'beforeEq':
                retObj.type = 'LE';
                retObj.value = parseToTimestamp(curDateObj.date);
                break;
            case 'after':
                retObj.type = 'GT';
                retObj.value = parseToTimestamp(curDateObj.date);
                break;
            case 'afterEq':
                retObj.type = 'GE';
                retObj.value = parseToTimestamp(curDateObj.date);
                break;
            case 'notEq':
                retObj.type = 'NE';
                retObj.value = parseToTimestamp(curDateObj.date);
                break;
            case 'within': {
                const toDate = moment(); // current time
                const fromDate = moment(toDate).add(-1 * curDateObj.value, curDateObj.units);

                retObj.type = 'BETWEEN';
                retObj.from = parseToTimestamp(fromDate);
                retObj.to = parseToTimestamp(toDate);
                break;
            }
            default:
                console.warn('Unknown operation type: "' + curDateObj.operation + '". DateObj: ', curDateObj);
                retObj = null;
        }

        return retObj;
    }

    queryFieldName(newFieldName) {
        if (arguments.length === 0) {
            return this.__queryFieldName;
        }
        this.__queryFieldName = newFieldName;
        return this.__queryFieldName;
    }

    setDefaultValues() {
        this.$['date-picker'].dateTime = null;
        this.$['date-picker-to'].dateTime = null;
        this.$['within-text-field'].text = null;

        this.__currentSelectionDropDown = dropDownDefaultValue;
        this.$['drop-down'].selectedValue = dropDownDefaultValue;
        this.__currentSelectionWithinDropDown = dropDownWithinDefaultValue;
        this.$['within-drop-down'].selectedValue = dropDownWithinDefaultValue;
    }

    setAspects(aspects) {
        this.__aspects = aspects;
    }

    clearCache() {
        this.setDefaultValues();
    }

    isError() {
        if (this._isRangeMode && this.compactMode) {
            // In compact mode, verify that the one ptcs-datepicker's fromDate and toDate have been assigned
            return !(this.$['date-picker'].fromDate && this.$['date-picker'].toDate);
        } else if (this._isRangeMode) {
            // In expanded mode, verify that we have a range as defined by the two distinct datepickers
            return !(this.$['date-picker'].dateTime && this.$['date-picker-to'].dateTime);
        } else if (this._isWithinMode) {
            return !(this.$['within-drop-down'].selectedValue && this.$['within-text-field'].text);
        }
        return !this.$['date-picker'].dateTime;
    }

    getFormatted() {
        const dateFormat = this.formatToken ? this.formatToken : 'DD-MMMM-YYYY, HH:mm:ss';
        const dateObj = this.__getCurrentData();

        switch (dateObj.operation) {
            case 'between':
                return `${this.dictionary.stringBetween} ${moment(dateObj.from).format(dateFormat)} ` +
                    `${this.dictionary.stringAnd} ${moment(dateObj.to).format(dateFormat)}`;
            case 'equals':
                return `${this.dictionary.stringEquals} ${moment(dateObj.date).format(dateFormat)}`;
            case 'before':
                return `${this.dictionary.stringBefore} ${moment(dateObj.date).format(dateFormat)}`;
            case 'beforeEq':
                return `${this.dictionary.stringBeforeEq} ${moment(dateObj.date).format(dateFormat)}`;
            case 'after':
                return `${this.dictionary.stringAfter} ${moment(dateObj.date).format(dateFormat)}`;
            case 'afterEq':
                return `${this.dictionary.stringAfterEq} ${moment(dateObj.date).format(dateFormat)}`;
            case 'notEq':
                return `${this.dictionary.stringNotEquals} ${moment(dateObj.date).format(dateFormat)}`;
            case 'within':
                return `${this.dictionary.stringWithinLast} ${dateObj.value} ${withinUnits.find(o => o.name === dateObj.units).label}`;
        }

        console.warn('Unsupported operation: "' + dateObj.operation + '". DateObj: ', dateObj);
        return null;
    }

    __setIsFilled() {
        this._setIsFilled(!this.isError());
    }

    __getCurrentSelectedRange() {
        this.__setIsFilled();
    }

    __getCurrentData() {
        if (this.isError()) {
            return null;
        }

        if (this._isRangeMode && this.compactMode) {
            return {
                operation: this.__currentSelectionDropDown,
                from:      this.$['date-picker'].fromDate,
                to:        this.$['date-picker'].toDate
            };
        } else if (this._isRangeMode) {
            return {
                operation: this.__currentSelectionDropDown,
                from:      this.$['date-picker'].dateTime,
                to:        this.$['date-picker-to'].dateTime
            };
        } else if (this._isWithinMode) {
            return {
                operation: this.__currentSelectionDropDown,
                value:     this.$['within-text-field'].text,
                units:     this.$['within-drop-down'].selectedValue};
        }
        return {
            operation: this.__currentSelectionDropDown,
            date:      this.$['date-picker'].dateTime
        };
    }
    __updateTranslations() {
        if (this.dictionary) {
            for (const o of operations) {
                if (this.dictionary[o.translationKey]) {
                    o.label = this.dictionary[o.translationKey];
                }
            }
            for (const o of withinUnits) {
                if (this.dictionary[o.translationKey]) {
                    o.label = this.dictionary[o.translationKey];
                }
            }
        }
        this.$['drop-down'].items = operations;
        this.$['within-drop-down'].items = withinUnits;

        this.$['drop-down'].selectedValue = '';
        this.$['within-drop-down'].selectedValue = '';
        this.$['drop-down'].selectedValue = dropDownDefaultValue;
        this.$['within-drop-down'].selectedValue = dropDownWithinDefaultValue;

    }

    __changeOperation(operation) {
        this._isRangeMode = operation === 'between';
        this._isWithinMode = operation === 'within';
        this.__setIsFilled();
    }

    _expanded(a, b) {
        return a && !b;
    }

    __handleKeyUp(event) {
        if (event.key === 'Enter' && !this.isError()) {
            this.blur();
            this.dispatchEvent(new CustomEvent('data-approved', {
                bubbles:  true,
                composed: true
            }));
        }
    }
}

customElements.define(PTCSDatetimeCase.is, PTCSDatetimeCase);
