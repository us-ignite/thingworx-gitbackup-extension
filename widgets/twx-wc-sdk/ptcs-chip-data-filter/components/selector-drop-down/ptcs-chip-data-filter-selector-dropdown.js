import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {when} from 'lit/directives/when.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-dropdown/ptcs-dropdown.js';
import moment from 'ptcs-moment/moment-import.js';

import './selectionViews/ptcs-number-case.js';
import './selectionViews/ptcs-string-case.js';
import './selectionViews/ptcs-boolean-case.js';
import './selectionViews/ptcs-datetime-case.js';
import './selectionViews/ptcs-location-case.js';

const extElem = new Set(['PTCS-NUMBER-CASE', 'PTCS-STRING-CASE', 'PTCS-BOOLEAN-CASE', 'PTCS-DATETIME-CASE', 'PTCS-LOCATION-CASE']);
const renderedSubcomponents = new Set();
const STRING_CASE = 'string-case';
const NUMBER_CASE = 'number-case';
const BOOLEAN_CASE = 'boolean-case';
const DATETIME_CASE = 'datetime-case';
const LOCATION_CASE = 'location-case';

class PTCSselector extends PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))) {

    static get styles() {
        return css`
                :host{
                    --subcomponent-margin-spacing: var(--ptcs-chip-data-filter-selector-dropdown-subcomponent-spacing);
                }

                :host([display="compact"][mode="open"]) {
                    display: block;
                }

                :host([display="compact"][mode="closed"]) {
                    display: none;
                }

                :host([display="compact"]) {
                    position: absolute;
                    z-index:  99990;
                }

                [part="filters-container"] {
                    display: flex;
                    flex-wrap: wrap;
                    align-items: flex-end;
                }

                :host([display="compact"]) [part="filters-container"] {
                    flex-direction: column;
                    align-items: flex-start;
                }

                [part="main-drop-down"] {
                    align-self: flex-start;
                }

                [part="main-drop-down"], [part="no-selection"] {
                    width:  var(--ptcs-chip-data-filter-selector-dropdown-base-subcomponent-width);
                    margin-right: var(--subcomponent-margin-spacing);
                }

                :host([display="compact"]) [part="main-drop-down"] {
                    width: 100%;
                }

                :host([display="compact"]) [part="no-selection"] {
                    width: 100%;
                }

                :host([display="expanded"]) [part="no-selection"] {
                    display: none;
                }

                [part="apply-button"], [part="cancel-button"] {
                    align-self: flex-end;
                }

                [hidden] {
                    display: none;
                }`;
    }

    render() {

        const _boolean = () => html`<ptcs-boolean-case id="boolean-case" part="boolean-case" .display=${this.display}
                .dictionary=${this.dictionary} ?hidden=${this._currentCase !== BOOLEAN_CASE} tabindex=${this._tabindex}
                .conditionLabel=${this.conditionLabel} @is-filled-changed=${this.__toggleApply}></ptcs-boolean-case>`;

        const _datetime = () => html`<ptcs-datetime-case id="datetime-case" part="datetime-case" .display=${this.display}
                .dictionary=${this.dictionary} ?hidden=${this._currentCase !== DATETIME_CASE} tabindex=${this._tabindex}
                 .unitsLabel=${this.unitsLabel} .startTimeValueLabel=${this.startTimeValueLabel} .endTimeValueLabel=${this.endTimeValueLabel}
                 .conditionLabel=${this.conditionLabel} .valueLabel=${this.valueLabel} .formatToken=${this.formatToken}
                .dateOrder=${this.dateOrder} .daysContainingAnyData=${this.daysContainingAnyData}
                .compactMode=${this.display === 'compact'}
                @is-filled-changed=${this.__toggleApply} @data-approved=${this.__forceClickOntoApply}></ptcs-datetime-case>`;

        const _location = () => html`<ptcs-location-case id="location-case" part="location-case" .display=${this.display}
                .dictionary=${this.dictionary} ?hidden=${this._currentCase !== LOCATION_CASE} tabindex=${this._tabindex}
                .unitsLabel=${this.unitsLabel} .valueLabel=${this.valueLabel} .conditionLabel=${this.conditionLabel}
                .latitudeLabel=${this.latitudeLabel} .longitudeLabel=${this.longitudeLabel}
                @is-filled-changed=${this.__toggleApply} @data-approved=${this.__forceClickOntoApply}></ptcs-location-case>`;

        const _number = () => html`<ptcs-number-case id="number-case" part="number-case" .display=${this.display}
                .dictionary=${this.dictionary} ?hidden=${this._currentCase !== NUMBER_CASE} tabindex=${this._tabindex}
                .conditionLabel=${this.conditionLabel} .valueLabel=${this.valueLabel}
                .rangeStartValueLabel=${this.rangeStartValueLabel} .rangeEndValueLabel=${this.rangeEndValueLabel}
                 @is-filled-changed=${this.__toggleApply} @data-approved=${this.__forceClickOntoApply}></ptcs-number-case>`;

        const _string = () => html`<ptcs-string-case id="string-case" part="string-case" .display=${this.display}
                .dictionary=${this.dictionary} ?hidden=${this._currentCase !== STRING_CASE} tabindex=${this._tabindex}
                 .conditionLabel=${this.conditionLabel} .valueLabel=${this.valueLabel} .filterHintText=${this.filterHintText}
                 @is-filled-changed=${this.__toggleApply} @data-approved=${this.__forceClickOntoApply}></ptcs-string-case>`;

        return html`<div id="container" part="filters-container">
                <ptcs-dropdown id="main-drop-down" part="main-drop-down"
                  .label=${this.categoryLabel || this.dictionary.stringFilterBy}
                  @selected-indexes-changed=${this.__handleMainDropDownIndexChange} .filter=${this.showListFilter}
                  tabindex=${this._tabindex}
                  .selector=${'label'} .valueSelector=${'value'}></ptcs-dropdown>

                <ptcs-textfield id="no-selection-case-text-field" part="no-selection" no-tabindex
                .label=${this.conditionLabel || this.dictionary.stringCondition}
                .hintText=${this.dictionary.stringSelectFilterFirst} disabled></ptcs-textfield>
                ${when(this._currentCase === BOOLEAN_CASE || renderedSubcomponents.has(BOOLEAN_CASE) || this._renderSubcomponents, _boolean)}
                ${when(this._currentCase === DATETIME_CASE || renderedSubcomponents.has(DATETIME_CASE) || this._renderSubcomponents, _datetime)}
                ${when(this._currentCase === LOCATION_CASE || renderedSubcomponents.has(LOCATION_CASE) || this._renderSubcomponents, _location)}
                ${when(this._currentCase === NUMBER_CASE || renderedSubcomponents.has(NUMBER_CASE) || this._renderSubcomponents, _number)}
                ${when(this._currentCase === STRING_CASE || renderedSubcomponents.has(STRING_CASE) || this._renderSubcomponents, _string)}
                <span id="buttons-container" part="buttons-container">
                    <ptcs-button id="apply-button" part="apply-button" disabled variant="primary"
                        .label=${this._setApplyLabel(this.dictionary.stringAdd)}
                         @click=${this.__handleApplyClick} tabindex=${this._tabindex}></ptcs-button>
                    <ptcs-button id="cancel-button" part="cancel-button" variant="secondary" .label=${this.dictionary.stringCancel}
                         @click=${this.__handleCancelClick} tabindex=${this._tabindex}></ptcs-button>
                </span>
            </div>
        `;
    }

    static get is() {
        return 'ptcs-chip-data-filter-selector-dropdown';
    }

    static get properties() {
        return {
            dictionary: {
                type: Object
            },

            // The text displayed above the drop-down list for the filter categories
            categoryLabel: {
                type:      String,
                attribute: 'category-label'
            },

            // The text displayed above the drop-down list for the filter condition
            conditionLabel: {
                type:      String,
                attribute: 'condition-label'
            },

            // The text displayed above the box which contains the value for the condition
            valueLabel: {
                type:      String,
                attribute: 'value-label'
            },

            // Placeholder text for the simple filter
            filterHintText: {
                type:      String,
                attribute: 'filter-hint-text'
            },

            // The text displayed above the first input box when filtering a range of values.
            rangeStartValueLabel: {
                type:      String,
                attribute: 'range-start-value-label'
            },

            // The text displayed above the second input box when filtering a range of values.
            rangeEndValueLabel: {
                type:      String,
                attribute: 'range-end-value-label'
            },

            startTimeValueLabel: {
                type:      String,
                attribute: 'start-time-value-label'
            },

            endTimeValueLabel: {
                type:      String,
                attribute: 'end-time-value-label'
            },

            // The text above the field used to select the start of the time range
            rangeStartTimeValueLabel: {
                type:      String,
                attribute: 'range-start-time-value-label'
            },

            // The text above the field used to select the end of the time range
            rangeEndTimeValueLabel: {
                type:      String,
                attribute: 'range-end-time-value-label'
            },

            // The text displayed above the drop-down list that is used to set the units when filtering by location or date.
            unitsLabel: {
                type:      String,
                attribute: 'units-label'
            },

            // The text displayed above the input box for latitude when filtering by location.
            latitudeLabel: {
                type:      String,
                attribute: 'latitude-label'
            },

            // The text displayed above the input box for longitude when filtering by location.
            longitudeLabel: {
                type:      String,
                attribute: 'longitude-label'
            },

            _data: {
                type:     Object,
                value:    null,
                observer: '__handleDataChange'
            },

            _lastFieldDefinitions: {
                type:  Object,
                value: null
            },

            _delegatedFocus: {
                type: String
            },

            subTabindex: {
                type:      String,
                attribute: 'sub-tabindex'
            },

            _tabindex: {
                type:     String,
                computed: '_computeTabindex(_delegatedFocus, subTabindex)'
            },

            formatToken: {
                type:      String,
                attribute: 'format-token'
            },

            dateOrder: {
                type:      String,
                attribute: 'date-order'
            },

            showListFilter: {
                type:      Boolean,
                attribute: 'show-list-filter'
            },

            customBaseTypesMapping: {
                type:      Object,
                value:     () => {},
                attribute: 'custom-base-types-mapping'
            },

            columnFormat: {
                type:      String,
                value:     null,
                attribute: 'column-format'
            },

            sortFilters: {
                type:      Boolean,
                value:     true,
                observer:  '__updateData',
                attribute: 'sort-filters'
            },

            mode: {
                type:     String,
                value:    'closed',
                reflect:  true,
                observer: '_modeChanged'
            },

            display: {
                type:    String,
                value:   'compact',
                reflect: true
            },

            operator: {
                type:  String,
                value: 'And'
            },

            daysContainingAnyData: {
                type:      Array,
                attribute: 'days-containing-any-data'
            },

            // The id of the current subcomponent, derived from selection in the main drop-down
            _currentCase: {
                type: String
            },

            _renderSubcomponents: {
                type: Boolean
            }
        };
    }

    constructor() {
        super();
        this.__caseRelatedDataInOrder = []; // the property keeps data entered by a user related with the different filter options
        this.__selectedItemParamsMainDropDown = {
            idx:      undefined,
            dataType: undefined,
            name:     undefined,
            label:    undefined,
            aspects:  undefined
        };
    }

    ready() {
        super.ready();
        this.addEventListener('keydown', this._keyDown.bind(this));
    }

    _computeTabindex(_delegatedFocus, subTabindex) {
        // Return: undefined || '-1' || '0'
        const f = v => {
            if (v === -1 || v === '-1') {
                return '-1';
            }
            return ((v || v === 0) && v >= 0) ? '0' : undefined;
        };

        return f(_delegatedFocus || subTabindex);
    }

    get query() {
        const filters = this.__caseRelatedDataInOrder
            .filter(specCase => specCase.query)
            .map(specCase => specCase.query);

        if (filters.length) {
            const queryHeader = {
                filters: {
                    type:    this.operator,
                    filters: filters
                }
            };
            return queryHeader;
        }
        return null;
    }

    set data(inputData) {
        if (inputData && inputData.dataShape && inputData.dataShape.fieldDefinitions) {
            let shouldUpdateFilters = false;
            let colFormat;

            if (this.columnFormat) {
                colFormat = JSON.parse(this.columnFormat);
                this.columnFormat = null; // we will use it once

                if (!this.__wasDataShapeChanged(colFormat, inputData.dataShape.fieldDefinitions)) {
                    shouldUpdateFilters = true; // we have to re-load filters since items order/localization/visibility may change from datashape
                    this._lastFieldDefinitions = colFormat;
                }
                // else do nothing - ignore columnFormat that was prepared by IDE since actual dataShape doesn't match it
            }

            if (!shouldUpdateFilters && this.__wasDataShapeChanged(this._lastFieldDefinitions, inputData.dataShape.fieldDefinitions)) {
                shouldUpdateFilters = true;
                const validFormat = colFormat && Object.keys(colFormat).length > 0;

                // columns that doesn't exist in colFormat, but exists in DS fieldDefinitions
                // this means these are the newly added fields to the DS and have Show as false
                for (const [key, value] of Object.entries(inputData.dataShape.fieldDefinitions)) {
                    const notExists =  validFormat ? !colFormat.hasOwnProperty(key) || colFormat[key].__showThisField === false : false;
                    // pk_count determines the count of primary key columns in the columnFormat
                    if (notExists && (inputData.pk_count === undefined || !isNaN(inputData.pk_count) && inputData.pk_count !== 1)) {
                        value.__showThisField = false;
                    }
                }
                this._lastFieldDefinitions = inputData.dataShape.fieldDefinitions;
            }

            if (!shouldUpdateFilters) {
                return;
            }

            this.__updateData();
        } else {
            console.error('Incorrect format of data passed to selector [data-filter]', inputData);
        }
    }

    __updateData() {
        if (!this._lastFieldDefinitions) {
            return;
        }

        const [dropDownItems, filteredData] = this.__getFilteredOutStructs(this._lastFieldDefinitions);
        const q = this.query;
        const dropdown = this.shadowRoot.getElementById('main-drop-down');
        if (dropdown) {
            [dropdown.items, this._data] = [dropDownItems, filteredData];
            this.performUpdate();
        }
        if (q) {
            this.loadQuery(q);
        }
    }

    get data() {
        return this._data;
    }

    _setApplyLabel(stringAdd) {
        return this.display === 'compact' ? this.dictionary.stringApply : stringAdd;
    }

    removeEnteredData(index, fieldName) {
        this.__clearCache(fieldName);
        this.__caseRelatedDataInOrder.splice(index, 1);
        this.__emitChangeEvent();
    }

    // Toggle the prompt to pick a filter
    _modeChanged(mode) {
        PTCS.setbattr(this.$['no-selection-case-text-field'], 'hidden', mode === 'closed');
    }

    __wasDataShapeChanged(fieldDefinitions1, fieldDefinitions2) {
        if (!fieldDefinitions1 && !fieldDefinitions2) {
            return false;
        }

        if (!fieldDefinitions1 || !fieldDefinitions2) {
            return true;
        }

        const keys1 = Object.keys(fieldDefinitions1);
        const keys2 = Object.keys(fieldDefinitions2);

        if (keys1.length !== keys2.length) {
            return true;
        }

        let shouldUpdateFilters = false;
        for (const prop in fieldDefinitions1) {
            if (fieldDefinitions1[prop].hasOwnProperty('__showThisField')) {
                break; // if any of the fields have __showThisField, this means the visibility is modified
            } else if (fieldDefinitions2[prop] === undefined) {
                shouldUpdateFilters = true;
                break;
            } else if (fieldDefinitions1[prop].baseType !== fieldDefinitions2[prop].baseType) {
                shouldUpdateFilters = true;
                break;
            }
        }

        return shouldUpdateFilters;
    }

    __emitChangeEvent() {
        this.dispatchEvent(new CustomEvent(
            'change',
            {
                bubbles:  true,
                composed: true,
                detail:   {
                    data: this.__caseRelatedDataInOrder.slice()
                }
            })
        );
        this.dispatchEvent(new CustomEvent(
            'update-filters-count',
            {
                bubbles:  true,
                composed: true
            })
        );
    }

    __emitCloseEvent(apply) {
        // Clear-out the drop-down selection _currentCase to hide the corresponding subcomponent
        this._currentCase = '';
        this.dispatchEvent(new CustomEvent(
            'close',
            {
                bubbles:  true,
                composed: true,
                detail:   {
                    data: apply
                }
            })
        );
    }

    __handleDataChange() {
        this.__setDefaultSelectorSetting();
        this.__caseRelatedDataInOrder = [];
    }

    __toggleApply(ev) {
        this.$['apply-button'].disabled = ev.detail ? !ev.detail.value : true;
    }

    __handleApplyClick() {
        if (!this.$['apply-button'].hasAttribute('disabled')) {
            this.mode = 'closed';
            this.__saveSelection();
            this.__setDefaultSelectorSetting();
            this.__emitCloseEvent(true);
        }
    }

    __handleCancelClick() {
        this.__setDefaultSelectorSetting();
        this.mode = 'closed';
        this.__emitCloseEvent(false);
    }

    cancelPopup() {
        if (this.display === 'compact' && this.mode !== 'closed') {
            this.__handleCancelClick();
        }
    }

    reset() {
        // Reset any data (as if the cancel button had been pressed)
        this.__handleCancelClick();
    }

    // Re-create query from parameter
    async loadQuery(query) {
        if (!query || !query.filters) {
            this._dispatchLoadQueryCompleted();
            return;
        }
        // Render the various '-case' subcomponents for the query resolution
        this._renderSubcomponents = true;
        this.performUpdate();
        await PTCS.wait();

        this.__loadQueryRelationsFilter(query.filters);
    }

    __loadQueryRelationsFilter(filter) {
        if (filter.type && (filter.type.toLowerCase() === 'and' || filter.type.toLowerCase() === 'or')) {
            this.__loadQuerySiblingChips(filter.filters);
        } else {
            this.__loadQuerySiblingChips([filter]);
        }
    }

    async __loadQuerySiblingChips(filters) {
        if (!filters) {
            this._dispatchLoadQueryCompleted();
            return;
        }

        // ptcs-chip-data-filter-selector-dropdown
        const fieldDefinitionsArray = Object.values(this._data.dataShape.fieldDefinitions);
        const dropdown = this.$['main-drop-down'];

        let stringCaseComp, datetimeCaseComp, locationCaseComp, booleanCaseComp, numberCaseComp;

        let idx, baseType;
        for (const expr of filters) {
            // Future support for nested operations
            if (expr.type && (expr.type.toLowerCase() === 'and' || expr.type.toLowerCase() === 'or')) {
                this.__loadQueryRelationsFilter(expr.filters);
                continue;
            }
            // Boolean or number category: A chip for a category replaces an existing chip in same category.
            // Select the filter item in main dropdown and resolve its baseType
            idx = fieldDefinitionsArray.findIndex(item => item.name === expr.fieldName);
            if (idx === -1) {
                console.warn('Ignoring filter condition. Unknown field name "' + expr.fieldName + '" in query: ', filters);
                continue;
            }

            const obj = {};
            dropdown.selected = idx;
            await dropdown.updateComplete;

            baseType = this.__getBaseDataType(fieldDefinitionsArray[idx].baseType);
            switch (baseType) {

                case 'string': {
                    stringCaseComp = await this.getSubCmpnt(STRING_CASE);
                    switch (expr.type) {
                        case 'EQ':
                            obj.operation = 'exact';
                            obj.value = expr.value;
                            break;
                        case 'NOTLIKE':
                            if (expr.value.startsWith('*') && expr.value.endsWith('*')) {
                                obj.operation = 'notContains';
                                obj.value = expr.value.substr(1, expr.value.length - 2);
                            } else if (expr.value.startsWith('*')) {
                                obj.operation = 'notEndWith';
                                obj.value = expr.value.substr(1, expr.value.length - 1);
                            } else if (expr.value.endsWith('*')) {
                                obj.operation = 'notStartWith';
                                obj.value = expr.value.substr(0, expr.value.length - 1);
                            } else {
                                obj.operation = 'isNot';
                                obj.value = expr.value;
                            }
                            break;
                        case 'LIKE':
                            if (expr.value.startsWith('*') && expr.value.endsWith('*')) {
                                obj.operation = 'contains';
                                obj.value = expr.value.substr(1, expr.value.length - 2);
                            } else if (expr.value.startsWith('*')) {
                                obj.operation = 'endWith';
                                obj.value = expr.value.substr(1, expr.value.length - 1);
                            } else if (expr.value.endsWith('*')) {
                                obj.operation = 'startWith';
                                obj.value = expr.value.substr(0, expr.value.length - 1);
                            } else { // default case
                                obj.operation = 'contains';
                                obj.value = expr.value;
                            }
                            break;
                        default:
                            console.warn('Ignoring filter condition. Unknown operation type: "' + expr.type + '" in condition: ', expr);
                            continue;
                    }
                    stringCaseComp.dataEnteredByUser = obj;
                    stringCaseComp.queryFieldName(expr.fieldName);
                    break;
                }

                case 'long':
                case 'integer':
                case 'number': {
                    numberCaseComp = await this.getSubCmpnt(NUMBER_CASE);
                    switch (expr.type) {
                        case 'EQ':
                            obj.value = expr.value;
                            obj.operation = '=';
                            break;
                        case 'NE':
                            obj.value = expr.value;
                            obj.operation = '≠';
                            break;
                        case 'LT':
                            obj.operation = '<';
                            obj.value = expr.value;
                            break;
                        case 'LE':
                            obj.operation = '<=';
                            obj.value = expr.value;
                            break;
                        case 'GT':
                            obj.operation = '>';
                            obj.value = expr.value;
                            break;
                        case 'GE':
                            obj.operation = '>=';
                            obj.value = expr.value;
                            break;
                        case 'NOTBETWEEN':
                            obj.from = expr.from;
                            obj.to = expr.to;
                            obj.operation = 'notBetween';
                            break;
                        case 'BETWEEN': {
                            obj.from = expr.from;
                            obj.to = expr.to;
                            obj.operation = 'between';
                            break;
                        }
                    }
                    numberCaseComp.dataEnteredByUser = obj;
                    numberCaseComp.queryFieldName(expr.fieldName);
                    break;
                }

                case 'boolean':
                    booleanCaseComp = await this.getSubCmpnt(BOOLEAN_CASE);
                    booleanCaseComp.dataEnteredByUser = expr.value;
                    booleanCaseComp.queryFieldName(expr.fieldName);
                    break;

                case 'datetime': {
                    datetimeCaseComp = await this.getSubCmpnt(DATETIME_CASE);
                    switch (expr.type) {
                        case 'EQ': {// backward compatibility
                            obj.date = moment(expr.value);
                            obj.operation = 'equals';
                            break;
                        }
                        case 'LT':
                            obj.operation = 'before';
                            obj.date = moment(expr.value);
                            break;
                        case 'LE':
                            obj.operation = 'beforeEq';
                            obj.date = moment(expr.value);
                            break;
                        case 'GT':
                            obj.operation = 'after';
                            obj.date = moment(expr.value);
                            break;
                        case 'GE':
                            obj.operation = 'afterEq';
                            obj.date = moment(expr.value);
                            break;
                        case 'NE':
                            obj.operation = 'notEq';
                            obj.date = moment(expr.value);
                            break;
                        case 'BETWEEN': {
                            const fromDate = moment(expr.from);
                            const toDate = moment(expr.to);

                            const diffFromNow = moment().diff(toDate, 'seconds');
                            if (diffFromNow >= 0 && diffFromNow < 60) { // toDate is less then 1min from now. winthin case
                                const diff = moment.duration(toDate.diff(fromDate));
                                obj.operation = 'within'; // the only case when start time isn't 00:00:00
                                let totalDuration = diff.asYears() | 0;
                                if (totalDuration !== 0) {
                                    obj.units = 'y';
                                    obj.value = totalDuration;
                                    break;
                                }
                                totalDuration = diff.asMonths() | 0;
                                if (totalDuration !== 0) {
                                    obj.units = 'M';
                                    obj.value = totalDuration;
                                    break;
                                }
                                totalDuration = diff.asWeeks() | 0;
                                if (totalDuration !== 0) {
                                    obj.units = 'w';
                                    obj.value = totalDuration;
                                    break;
                                }
                                totalDuration = diff.asDays() | 0;
                                if (totalDuration !== 0) {
                                    obj.units = 'd';
                                    obj.value = totalDuration;
                                    break;
                                }
                                totalDuration = diff.asHours() | 0;
                                if (totalDuration !== 0) {
                                    obj.units = 'h';
                                    obj.value = totalDuration;
                                    break;
                                }
                                totalDuration = diff.asMinutes() | 0;
                                if (totalDuration !== 0) {
                                    obj.units = 'm';
                                    obj.value = totalDuration;
                                    break;
                                }
                                totalDuration = diff.asSeconds() | 0;
                                if (totalDuration !== 0) {
                                    obj.units = 's';
                                    obj.value = totalDuration;
                                    break;
                                }
                            } else {
                                obj.from = fromDate;
                                obj.to = toDate;
                                obj.operation = 'between';
                            }
                            break;
                        }
                    }
                    datetimeCaseComp.dataEnteredByUser = obj;
                    datetimeCaseComp.queryFieldName(expr.fieldName);
                    break;
                }

                case 'location':
                    locationCaseComp = await this.getSubCmpnt(LOCATION_CASE);
                    obj.type = expr.type;
                    obj.value = expr.distance;
                    obj.units = this._convertLocationUnits(expr.units);
                    obj.latitude = expr.location.latitude;
                    obj.longitude = expr.location.longitude;
                    locationCaseComp.dataEnteredByUser = obj;
                    locationCaseComp.queryFieldName(expr.fieldName);
                    break;

                default:
                    console.warn('baseType "' + baseType + '" not handled', expr);
                    continue;
            }

            this.__saveSelection();
            this.__setDefaultSelectorSetting();
        }
        this._dispatchLoadQueryCompleted();
    }

    _dispatchLoadQueryCompleted() {
        this.dispatchEvent(new CustomEvent(
            'load-query-completed',
            {
                bubbles:  true,
                composed: true
            }));
    }

    __setDefaultSelectorSetting() {
        this.$['main-drop-down'].selected = -1;
        this.$['apply-button'].disabled = true;
        this.$['main-drop-down'].disabled = this.$['main-drop-down'].items.length === 0;

        this.$['no-selection-case-text-field'].disabled = true;
    }

    //    Returns <ptcs-number-case>, <ptcs-text-case>, <ptcs-datetime-case>, <ptcs-boolean-case>, ...
    __getSelectedCaseComponent() {
        const dataType = this.__selectedItemParamsMainDropDown.dataType;
        const selectedCaseComponent = dataType && this.$.container.querySelector(`ptcs-${dataType.toLowerCase()}-case`);

        return selectedCaseComponent;
    }

    __saveSelection() {
        const selectedCaseComponent = this.__getSelectedCaseComponent();
        if (selectedCaseComponent) {
            const query = selectedCaseComponent.query;
            if (!query) {
                return;
            }
            const selectedItemIdxDropDown = this.__selectedItemParamsMainDropDown.idx;
            const selectedItemCategoryDropDown = this.__selectedItemParamsMainDropDown.label;
            const dataToBeInserted = {
                query:             query,
                dataEnteredByUser: selectedCaseComponent.dataEnteredByUser,
                formatted:         `${selectedItemCategoryDropDown}: ${selectedCaseComponent.getFormatted()}`,
                isError:           selectedCaseComponent.isError(),
                innerIdx:          selectedItemIdxDropDown,
                fieldName:         selectedCaseComponent.__queryFieldName,
                element:           selectedCaseComponent
            };
            this.__caseRelatedDataInOrder.push(dataToBeInserted); // save new data
            this.__emitChangeEvent();
        }
    }

    __loadDataForSelectedOption() {
        const selectedCaseComponent = this.__getSelectedCaseComponent();
        if (selectedCaseComponent) {
            selectedCaseComponent.setDefaultValues();
            selectedCaseComponent.queryFieldName(this.__selectedItemParamsMainDropDown.name);
            selectedCaseComponent.setAspects(this.__processAspects());
        }
    }

    /**
     * Preprocesses aspects field for uniformity...
     * selectOptions -> _selectOptions
     *     Assumes comma delimited string in pattern of val1:displayText1,val2:displayText2 where the each entry is parsed into 2 parts
     *     the first is the internal value separated by a colon and the second value is the display text
     * @returns {object}
     * @private
     */
    __processAspects() {
        if (!this.__selectedItemParamsMainDropDown.aspects) {
            return undefined;
        }

        const aspects = this.__selectedItemParamsMainDropDown.aspects;
        if (aspects.selectOptions) {
            const items = aspects.selectOptions.split(',');
            aspects._selectOptions = items.map(item => {
                const parts = item.split(':');
                const val = parts.shift();
                const label = parts.join(':');
                return {val, label};
            });
        }
        return aspects;
    }

    // Function that retrieves or renders subcomponents (ptcs-number-case, ptcs-string-case, etc) based on its id
    getSubCmpnt(componentId) {
        this._currentCase = componentId;
        renderedSubcomponents.add(componentId);
        this.performUpdate();

        return this.$[componentId];
    }

    async __handleMainDropDownIndexChange(ev) {
        // The main dropdown in ptcs-chip-data-filter-selector-dropdown leads to displaying a subcomponent based on
        // type, i.e. ptcs-string-case, ptcs-number-case, ptcs-location-case etc

        PTCS.setbattr(this.$['no-selection-case-text-field'], 'hidden', true); // Hide the prompt to pick a filter

        const outputIdx = ev.detail.value[0];

        // If there is a change event before the data is ready, the change must be telling us that nothing is selected
        console.assert(this._data || ev.detail.value.length === 0);

        const setSelectedItemParams = () => {
            let outputDataType, outputName, outputLabel, outputAspects;
            if (outputIdx !== undefined) {
                const fieldDefinitionsArray = Object.values(this._data.dataShape.fieldDefinitions);

                outputDataType = outputIdx < fieldDefinitionsArray.length
                    ? this.__getBaseDataType(fieldDefinitionsArray[outputIdx].baseType) : undefined;
                outputName = fieldDefinitionsArray[outputIdx].name;
                outputLabel = fieldDefinitionsArray[outputIdx].Title ? fieldDefinitionsArray[outputIdx].Title : fieldDefinitionsArray[outputIdx].name;
                outputAspects = fieldDefinitionsArray[outputIdx].aspects;

                const subCmpnt = outputDataType.toLowerCase();
                switch (subCmpnt) {
                    case 'datetime':
                    case 'location':
                    case 'boolean':
                    case 'number':
                    case 'string':
                        this._currentCase = subCmpnt + '-case';
                        break;
                    case 'long':
                    case 'integer':
                        this._currentCase = 'number-case';
                        break;
                    default: {
                        this._currentCase = '';
                        console.warn('unhandled basetype');
                    }
                }
            }

            this.__selectedItemParamsMainDropDown = { // update __selectedItemParamsMainDropDown based on the selection of mainDropDown
                idx:      outputIdx,
                dataType: outputDataType,
                name:     outputName,
                label:    outputLabel,
                aspects:  outputAspects
            };
        };

        setSelectedItemParams();

        if (this._currentCase) {
            await this.getSubCmpnt(this._currentCase);
            if (outputIdx !== undefined) {
                this.__loadDataForSelectedOption();
            }
        }

        // Give some time for lit to render
        await PTCS.wait();

        // Give some time for lit to render the subcomponent on-demand from this._currentCase assignment
        this.performUpdate();
        await this.updateComplete;

        if (outputIdx !== undefined) {
            const selectedCaseComponent = this.__getSelectedCaseComponent();
            this.$['apply-button'].disabled = selectedCaseComponent ? !selectedCaseComponent.isFilled : true;
        }
    }

    __forceClickOntoApply() {
        this.$['apply-button'].click();
    }

    __clearCache(fieldName) {
        const typeElementRelatedtoChips = this.__caseRelatedDataInOrder.find((el) => el.fieldName === fieldName);
        if (typeElementRelatedtoChips && typeElementRelatedtoChips.element.clearCache) {
            typeElementRelatedtoChips.element.clearCache();
        }
    }

    __localizeItems(dropDownItems) {
        for (const item of dropDownItems) {
            const _item = item[1];
            const name = _item.Title;
            if (name && (name.startsWith('[[') || name.startsWith('tw.'))) {
                _item.Title = PTCS.Formatter.localize(name);
            }
        }
    }

    __sortByLocalizedTitle(a, b) {
        if (!a && !b) {
            return 0;
        }

        if (!a) {
            return -1;
        }

        if (!b) {
            return 1;
        }

        const aName = this.__extractLabel(a[1]);
        const bName = this.__extractLabel(b[1]);

        return aName.localeCompare(bName);
    }

    __extractLabel(fieldDefinition) {
        let name = fieldDefinition.Title;

        if (!name) {
            name = fieldDefinition.name;
        }

        return name;
    }

    __getFilteredOutStructs(fieldDefinitions) {
        const filteredData = {
            dataShape: {
                fieldDefinitions: {}
            }
        };

        let dropDownItems = Object.entries(fieldDefinitions);
        this.__localizeItems(dropDownItems);

        if (this.sortFilters) {
            dropDownItems = dropDownItems.sort(this.__sortByLocalizedTitle.bind(this));
        }

        dropDownItems = dropDownItems.map(filterOption => {
            const [filterOptionKey, filterOptionValue] = filterOption;
            if (filterOptionValue.__showThisField === false) {
                console.warn(`${filterOptionValue.name} is hidden by Composer`);
                return null;
            }
            const dataTypeCase = filterOptionValue.baseType;
            if (this.__getBaseDataType(dataTypeCase)) {
                filteredData.dataShape.fieldDefinitions[filterOptionKey] = filterOptionValue;
                return {label: filterOptionValue.Title ? filterOptionValue.Title : filterOptionValue.name, value: filterOptionValue.name};
            }

            return null;
        }).filter(el => el); // filter out null elements
        return [dropDownItems, filteredData];
    }

    __getBaseDataType(proposedBaseType) {
        const supportedCases = ['string', 'boolean', 'number', 'datetime', 'location'];

        proposedBaseType = proposedBaseType && proposedBaseType.toLowerCase();
        if (supportedCases.includes(proposedBaseType)) {
            return proposedBaseType;
        }

        let baseType = null;
        if (this.customBaseTypesMapping) {
            baseType = this.customBaseTypesMapping[proposedBaseType];
            if (!baseType) {
                baseType = this.customBaseTypesMapping['default'];
            }

            if (baseType === 'unsupported') {
                return null;
            }

            if (!supportedCases.includes(baseType)) {
                console.warn('chip filter: Unknown base type (' + baseType + ') for proposed base type (' + proposedBaseType + ')');
                return null;
            }
        }

        return baseType;
    }

    _convertLocationUnits(units) {
        if (units.length === 1) {
            return units;
        }

        units = units.toLowerCase();
        switch (units) {
            case 'miles':
                return 'M';
            case 'kilometers':
                return 'K';
            case 'nautical miles':
                return 'N';
        }
        return units;
    }

    get focusableElements() {
        const result = [];

        const collect = el => {
            for (let e = el.firstElementChild; e; e = e.nextElementSibling) {
                if (!(e.clientHeight > 0)) {
                    continue; // hidden
                }
                if (extElem.has(e.tagName)) {
                    collect(e.shadowRoot); // focus on sub elements
                } else {
                    if (e.hasAttribute('tabindex')) {
                        result.push(e);
                    }
                    collect(e);
                }
            }
        };

        collect(this.$.container);

        return result;
    }

    _keyDown(ev) {
        if (this.display !== 'compact') {
            return;
        }
        const close = () => {
            ev.preventDefault();
            this.cancelPopup();
        };
        switch (ev.key) {
            case 'Escape':
                close();
                break;

            case 'Tab':
                if (ev.shiftKey) {
                    if (this.shadowRoot.activeElement === this.$['main-drop-down']) {
                        // Shift-Tab on first focusable element
                        close();
                    }
                } else if (this.shadowRoot.activeElement === this.$['cancel-button']) {
                    // Shift-Tab on last focusable element
                    close();
                }
                break;
        }
    }
}

customElements.define(PTCSselector.is, PTCSselector);
