import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {when} from 'lit/directives/when.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import {delegateToPrev} from 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';

import './components/chip-container/ptcs-chip-data-filter-chip-container.js';
import './components/selector-drop-down/ptcs-chip-data-filter-selector-dropdown.js';
import 'ptcs-icons/cds-icons.js';

import {refDictionary, getStringBasedProperties} from './localization.js';

const filterOperatorDefaultValue = 'And';
const DATETIME_CASE = 'datetime-case'; // See ptcs-chip-data-filter-selector-dropdown.js

function focusIndex(el, focusable) {
    const index = focusable.indexOf(el); // Quick test
    return index >= 0 ? index : focusable.findIndex(e => e.shadowRoot && e.shadowRoot.contains(el));
}

const DataFilter = class extends PTCS.BehaviorTabindex(PTCS.BehaviorStyleable(L2Pw(LitElement))) {
    static get is() {
        return 'ptcs-chip-data-filter';
    }

    static get styles() {
        return css`
                :host {
                    display: inline-flex;
                    flex-direction: column;
                    width: 100%;
                }

                :host([chips-on-top]) {
                    flex-direction: column-reverse;
                }

                ptcs-button {
                    margin: var(--subcomponent-margin-spacing) var(--subcomponent-margin-spacing) 0px 0px;
                }

                [part="add-filter-button"]:not([display="compact"]) {
                    display: none;
                }

                [part="top-bar"] {
                    display: inline-flex;
                    align-items: flex-end;
                    justify-content: space-between;
                    box-sizing: border-box;
                }

                [part="top-bar"] > * {
                    flex: 0 0 auto;
                }

                :host([hide-filter]) ptcs-chip-data-filter-chip-container {
                    display: none;
                }

                :host([hide-filter]) [part="top-bar"] > * {
                    display: none;
                }

                *[hidden] {
                    display: none;
                }

                [part="small-show-button"][open] {
                    transform-origin: center;
                    transform: rotate(180deg);
                }

                :host([chips-disclosure=icon]) [part="show-button"] {
                    display: none;
                }

                :host(:not([chips-disclosure=icon])) [part="small-show-button"] {
                    display: none;
                }

                :host([chips-disclosure=none]) [part="show-button"] {
                    display: none;
                }

                :host([chips-disclosure=none]) [part="small-show-button"] {
                    display: none;
                }

                [part="filter-operator-drop-down"] {
                    width:  var(--ptcs-chip-data-filter-selector-dropdown-base-subcomponent-width);
                }`;
    }

    render() {

        const _filterOperator = () => html`<ptcs-dropdown id="filter-operator" part="filter-operator-drop-down"
                     tabindex=${this._tabindex} .selector=${'label'} .valueSelector=${'name'} ?hidden=${!this._showOperator}
                    .items=${this.filterOperators} .selectedValue=${this._selector.operator || filterOperatorDefaultValue}
                    @selected-value-changed=${this.__handleOperatorChange}></ptcs-dropdown>`;

        const _chipsDiclosureLink = () => html`<ptcs-link id="chips-toggle" part="show-button" tabindex=${this._tabindex}
                    ?hidden=${this._hidden(this._showChipToggleBtn, this.showAndHideFilters)} .label=${this._hideShowChipsLabel}
                    @click=${this.__handleHideChips}></ptcs-link>`;

        const _chipsDisclosureIcon = () => html`<ptcs-button id="small-chips-toggle" part="small-show-button"
                    .icon=${'cds:icon_double_chevron_down_mini'} variant="small" ?open=${this._showChips} tabindex=${this._tabindex}
                    ?hidden=${this._hidden(this._showChipToggleBtn, this.showAndHideFilters)}
                    @click=${this.__handleHideChips}></ptcs-button>`;

        return html`<div id="top-bar" part="top-bar">
                <ptcs-button id="add-filter" part="add-filter-button" variant="primary" .label=${this.dictionary.stringAddFilter}
                  .disabled=${this.disabled} .mode=${'label'} @click=${this.__handleAddFilterClick}
                  tabindex=${this._tabindex} display=${this.displayMode} ?selected=${this._selected}>
                </ptcs-button>
                <ptcs-label id="filters-counter" variant="label" part="filters-counter" ?hidden=${this.hideFilterCounter}
                   .label=${this.__filtersLabel(this.__filtersCount, this.dictionary.stringFilter, this.dictionary.stringFilters,
        this._showOperator, this.showAndOrOperator, this.dictionary.stringJoinedBy)}>
                </ptcs-label>
                 ${when(this.showAndOrOperator && this.__filtersCount, _filterOperator)}
                 ${when(this.chipsDisclosure === 'link' && this.showAndHideFilters, _chipsDiclosureLink)}
                 ${when(this.chipsDisclosure === 'icon' && this.showAndHideFilters, _chipsDisclosureIcon)}
                <div style="flex: 1 1 auto; display: block; overflow: hidden"><slot></slot></div>
            </div>
            <ptcs-chip-data-filter-selector-dropdown id="selector" part="selector"
                    @close=${this.__handleClose} @change=${this.__handleSelectorChange}
                    .showListFilter=${this.showListFilter} .dictionary=${this.dictionary}
                    tabindex=${this._xTabindex(this.tabindex, this.displayMode)} .subTabindex=${this._tabindex}
                    .dateOrder=${this.dateOrder} .formatToken=${this.formatToken} display=${this.displayMode}
                    .customBaseTypesMapping=${this.customBaseTypesMapping} .columnFormat=${this.columnFormat}
                    .sortFilters=${this.sortFilters} .unitsLabel=${this.unitsLabel} .valueLabel=${this.valueLabel}
                    .conditionLabel=${this.conditionLabel} .categoryLabel=${this.categoryLabel} .filterHintText=${this.filterHintText}
                    @load-query-completed=${this._unblockQuery}
                    .rangeStartValueLabel=${this.rangeStartValueLabel} .rangeEndValueLabel=${this.rangeEndValueLabel}
                    .startTimeValueLabel=${this.startTimeValueLabel} .rangeEndTimeValueLabel=${this.rangeEndTimeValueLabel}
                    .latitudeLabel=${this.latitudeLabel} .longitudeLabel=${this.longitudeLabel}
            ></ptcs-chip-data-filter-selector-dropdown>
            <ptcs-chip-data-filter-chip-container id="chip-container" part="chip-container"
                    .subTabindex=${this._tabindex} exportparts=${'oval-container, content, chip-child'}
                    @remove=${this.__removeEnteredData} ?hidden=${this._hideChipsArea(this._showChips, this._query)}
            ></ptcs-chip-data-filter-chip-container>`;
    }

    static get properties() {
        /*
            to avoid loooong list of string based properties, they are appended here using Object.assign
            if a string has been changed, then the 'dictionary' property is updated
        */
        // add observer param to all string based properties
        const stringBasedProperties = getStringBasedProperties();
        Object.keys(stringBasedProperties).forEach(stringParam => {
            stringBasedProperties[stringParam].observer = '__updateDictionary';
        });

        return Object.assign({
            dictionary: { //  object is passed from parent component to its child (those that need translation(s))
                type:     Object,
                observer: '__updateTranslations',
                value:    refDictionary
            },

            filterOperators: {
                type:  Object,
                value: () => [
                    {name: 'And', translationKey: 'stringAnd', label: 'AND'},
                    {name: 'Or', translationKey: 'stringOr', label: 'OR'}
                ],
                attribute: 'filter-operators'
            },

            // The two-way data binding query
            query: {
                type:      Object,
                converter: {
                    toAttribute(value) {
                        const retVal = typeof value === 'object' ? JSON.stringify(value) : value;
                        return retVal;
                    },

                    fromAttribute(value) {
                        const retVal = typeof value === 'object' ? JSON.stringify(value) : value;
                        return retVal;
                    }
                },
                notify:   true,
                observer: '_queryChanged'
            },

            // The actual query object
            _query: {
                type: Object
            },

            // The filter data
            data: {
                type:       String,
                noAccessor: true
            },

            // Private work copy to retain previous getter / setter
            _data: {
                type:        String,
                observer:    '_dataChanged',
                observeWhen: 'immediate'
            },

            disabled: {
                type: Boolean
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

            // Calendar start time label
            startTimeValueLabel: {
                type:      String,
                attribute: 'start-time-value-label'
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

            _tabindex: {
                type:     String,
                computed: '_computeTabIndex(tabindex)'
            },

            daysContainingData: {
                type:       Object,
                attribute:  'days-containing-data',
                noAccessor: true
            },

            // Private work copy to retain previous getter / setter
            _daysContainingData: {
                type:        Object,
                observeWhen: 'immediate',
                observer:    '__updateDaysContainingData'
            },

            // Full override of format
            formatToken: {
                type:      String,
                attribute: 'format-token'
            },

            dateOrder: {
                type:      String,
                attribute: 'date-order'
            },

            hideFilterCounter: {
                type:      Boolean,
                observer:  '_hideFilterCounterChanged',
                attribute: 'hide-filter-counter'
            },

            showListFilter: {
                type:      Boolean,
                attribute: 'show-list-filter'
            },

            customBaseTypesMapping: {
                type:      Object,
                attribute: 'custom-base-types-mapping'
            },

            columnFormat: {
                type:      String,
                value:     null,
                attribute: 'column-format'
            },

            sortFilters: {
                type:        Boolean,
                observer:    '_updateDisableFilterSorting',
                observeWhen: 'immediate',
                attribute:   'sort-filters'
            },

            disableFilterSorting: {
                type:        Boolean,
                value:       false,
                observer:    '_updateSortFilters',
                observeWhen: 'immediate',
                attribute:   'disable-filter-sorting'
            },

            maxWidth: {
                type:      Number,
                observer:  '_maxWidthChanged',
                attribute: 'max-width'
            },

            filtersMaxHeight: {
                type:      Number,
                attribute: 'filters-max-height'
            },

            __filtersCount: {
                type:  Number,
                value: 0
            },

            _showChips: {
                type:  Boolean,
                value: true
            },

            // In 'compact' mode the selector is displayed as a popup child of <body>, in 'expanded' mode the selector is
            // displayed within the component.
            displayMode: {
                type:      String,
                value:     'compact',
                reflect:   true,
                attribute: 'display-mode'
            },

            showAndHideFilters: {
                type:      Boolean,
                attribute: 'show-and-hide-filters'
            },

            showAndOrOperator: {
                type:      Boolean,
                attribute: 'show-and-or-operator'
            },

            chipsOnTop: {
                type:      Boolean,
                reflect:   true,
                attribute: 'chips-on-top'
            },

            chipsDisclosure: { // 'link' || 'icon' || 'none'
                type:      String,
                value:     'link',
                observer:  '_chipsDisclosureChanged',
                reflect:   true,
                attribute: 'chips-disclosure'
            },

            hideFilter: {
                type:      Boolean,
                reflect:   true,
                attribute: 'hide-filter'
            },

            borders: {
                type:     String,
                observer: '_bordersChanged'
            },

            _borderTop: {
                type:      Boolean,
                reflect:   true,
                attribute: '_border-top'
            },

            _borderBottom: {
                type:      Boolean,
                reflect:   true,
                attribute: '_border-bottom'
            },

            _borderLeft: {
                type:      Boolean,
                reflect:   true,
                attribute: '_border-left'
            },

            _borderRight: {
                type:      Boolean,
                reflect:   true,
                attribute: '_border-right'
            },

            _showChipToggleBtn: {
                type: Boolean
            },

            _showOperator: {
                type: Boolean
            },

            _selector: { // reference to the selector drop-down
                type: Object
            },

            _selectorId: {
                type: String
            },

            _reDrawSelector: {
                type:  Boolean,
                value: false
            },

            _selected: {
                type:    Boolean,
                reflect: true
            }

        }, stringBasedProperties);
    }

    get _selector() {
        if (!this.__selector) {
            this.__selector = this.shadowRoot && this.shadowRoot.getElementById('selector');
            if (this.__selector) {
                this.requestUpdate('_selector');
            }
        }
        return this.__selector;
    }

    constructor() {
        super();
        this.__filtersCount = 0;
    }

    ready() {
        super.ready();

        this.__updateTranslations();

        // Keyboard navigation
        this.shadowRoot.addEventListener('mousedown', () => this._mouseDown());
        this.addEventListener('keydown', ev => this._keyDown(ev));
        this.addEventListener('focus', ev => this._focusEv(ev));
        this.addEventListener('update-filters-count', () => this.__updateFiltersCount());
    }

    connectedCallback() {
        super.connectedCallback();
    }

    disconnectedCallback() {
        // Remove the selector under <body>, if any
        this._removeExternalSelector();
        super.disconnectedCallback();
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        if (changedProperties.has('displayMode')) {
            // In 'compact' mode the selector is appended as child of <body>, in 'expanded' mode the selector is
            // within the component.
            if (this.displayMode === 'compact') {
                this._selector.__saSa = this.__saSa;
                this.__selector = document.body.appendChild(this._selector);
                this._selector.mode = 'closed';
                this.setExternalComponentId();
            } else {
                const externalSelector = document.getElementById(this._selectorId);
                if (externalSelector) {
                    // 'expanded' mode
                    this.__selector = this.shadowRoot.insertBefore(externalSelector, this.$['chip-container']);
                    this._selector.mode = 'open';
                }
            }
        }

        if (changedProperties.has('_data')) {
            this._updateSelectorData(this._data);
        }

        if (changedProperties.has('dictionary') || changedProperties.has('filterOperators')) {
            this.__updateTranslations();
        }

        if (changedProperties.has('_daysContainingData')) {
            this.__updateDaysContainingData(this._daysContainingData);
        }
    }

    __updateDaysContainingData(_daysContainingData) {
        const supportedType = 'datetime';
        const fieldWithDays = Object.entries(_daysContainingData.dataShape.fieldDefinitions)[0][1];

        if (fieldWithDays && fieldWithDays.baseType.toLowerCase() === supportedType) {
            const arrayOfTimestamp = _daysContainingData.rows.map(row => row[fieldWithDays.name]);

            requestAnimationFrame(() => {
                // eslint-disable-next-line no-unused-vars
                const datetimeCmpnt = this._getDatetimeCase();
                this._selector.daysContainingAnyData = arrayOfTimestamp;
                this._selector._currentCase = '';
            });
        }
    }

    set data(data) {
        this._data = data;
    }

    get data() {
        return this._selector && this._selector.data;
    }

    _updateSelectorData(_data) {
        this._selector.data = _data;
        if (_data && this._pendingQuery !== undefined) {
            this._queryChanged(this._pendingQuery);
        }
    }

    _dataChanged(_data) {
        if (!this._selector) {
            // firstUpdated will address this assignment
            return;
        }
        this._updateSelectorData(_data);
    }

    set daysContainingData(daysContainingData) {
        this._daysContainingData = daysContainingData;
    }

    _getDatetimeCase() {
        this._selector._currentCase = DATETIME_CASE;
        const datetimeCmpnt = this._selector.getSubCmpnt(DATETIME_CASE);
        return datetimeCmpnt;
    }

    get daysContainingData() {
        const datepicker =  this._getDatetimeCase().shadowRoot.querySelector('#date-picker');
        return datepicker.daysContainingAnyData;
    }

    _removeExternalSelector() {
        const externalSelector = document.getElementById(this._selectorId);
        if (externalSelector) {
            externalSelector.remove();
        }
    }

    // Handles filterOperators object localization
    __updateTranslations() {
        if (this.dictionary) {
            for (const o of this.filterOperators) {
                if (this.dictionary[o.translationKey]) {
                    o.label = this.dictionary[o.translationKey];
                }
            }
        }

        const filterOperatorEl = this.$['top-bar'].querySelector('#filter-operator');
        if (filterOperatorEl) {
            filterOperatorEl.items = this.filterOperators;
        }
    }

    _bordersChanged(borders) {
        const b = (borders + '').toLowerCase();
        this._borderTop = b.includes('t');
        this._borderBottom = b.includes('b');
        this._borderLeft = b.includes('l');
        this._borderRight = b.includes('r');
    }

    // Return the height of part="top-bar"
    get topBarHeight() {
        return this.$['top-bar'].getBoundingClientRect().height;
    }

    __removeEnteredData(ev) {
        const triggerChip = ev.composedPath()[0];
        const triggerChipId = Number(triggerChip.getAttribute('data-id'));
        const triggerChipFieldName = triggerChip.getAttribute('field-name');

        this._selector.removeEnteredData(triggerChipId, triggerChipFieldName);

        // Set the focus back to either the 'Add' button or the dropdown list
        this.__setFocus(triggerChipId);
    }

    __handleClose() {
        this._selected = false;
        this._selector.$['main-drop-down'].selected = -1;
        if (this.__autoCloseEv) {
            document.removeEventListener('mousedown', this.__autoCloseEv);
            this.__autoCloseEv = undefined;
        }
        // Set the focus back to either the 'Add' button or the dropdown list
        this.__setFocusToAddOrDropdown();
    }

    __setFocusToAddOrDropdown() {
        if (this.displayMode === 'compact') {
            if (this.$['add-filter']) {
                this.$['add-filter'].focus();
            }
        } else if (this._selector && this._selector.shadowRoot.getElementById('main-drop-down')) {
            const dropdown = this._selector.$['main-drop-down'];
            if (dropdown) {
                dropdown.focus();
            }
        }
    }

    __setFocus(triggerChipId) {
        const chipsList = this.focusableElementsChipContainer;
        const chipsLength = chipsList.length;

        // Deleting the single chip while no other chip will be left, the focus should move to Add Filter button
        if (chipsLength <= 1) {
            this.__setFocusToAddOrDropdown();
        // Deleting the last chip, the focus should move to previous chip
        } else if (triggerChipId === chipsLength - 1) {
            chipsList[triggerChipId - 1].focus();
        // Deleting the first chip or one in the middle, the focus should move to next chip
        } else if (triggerChipId >= 0) {
            chipsList[triggerChipId].focus();
        }
    }

    _hideFilterCounterChanged(hideFilterCounter) {
        if (hideFilterCounter && !this._showChips) {
            this.__handleHideChips();
        }
    }

    _hideChipsArea(showChips) {
        const numFilters = (this._query && this._query.filters) ? this._query.filters.filters.length : 0;
        if (numFilters === 0) {
            return true;
        }
        return !showChips;
    }

    __handleAddFilterClick() {
        // Reset the selector to its initial state
        this._selector.reset();

        this._selector.mode = 'open';
        this._selected = true;
        this._selector.style.visibility = 'hidden'; // prevent selector from displaying before it's ready
        this._selector.style.width = '';

        // Need to wait a few animation frames for the list to stabilize (100ms ~ 6 animation frames)
        setTimeout(() => {
            this._selector.style.visibility = ''; // show selector in proper place

            const dim = this._get_dimension();
            this._set_selector_position(dim); // set list position

            if (this._selector.mode === 'open') {
                this._selector.focus();
            }

            // Keep track of list position
            this.track_position(dim);
        }, 100);

        // Can el somehow be linked to the selector?
        const isSelectorPart = el => {
            for (const selector = this._selector; el; el = el.__$mainCmpnt) {
                for (let e = el; e; e = e.getRootNode && e.getRootNode().host) {
                    if (e === selector) {
                        return true;
                    }
                }
            }
            return false;
        };

        // Close AddFilter popup if user clicks outside of it
        this.__autoCloseEv = ev => {

            const el = PTCS.isFirefox
                ? ev.target
                : this.shadowRoot.elementFromPoint(ev.clientX, ev.clientY) || document.elementFromPoint(ev.clientX, ev.clientY);

            if (el !== this.$['add-filter'] && !isSelectorPart(el)) {
                // Clicked on something that is not the AddFilter button nor anything that is connected to the selector
                if (this._selector) {
                    this._selector.cancelPopup();
                }
            }
        };

        document.addEventListener('mousedown', this.__autoCloseEv);
    }

    _hidden(a, b) {
        return !(a && b);
    }

    __handleHideChips() {
        if (this.disabled) {
            return;
        }
        this._showChips = !this._showChips;
        this._hideShowChipsLabel = this._showChips ? this.dictionary.stringHideFilters : this.dictionary.stringShowFilters;
    }

    __handleSelectorChange(ev) {
        const selectorDataEnteredByUser = ev.detail.data;
        const chipContainerData = selectorDataEnteredByUser.map((filterOption, index) => {
            return {
                content:   filterOption.formatted,
                error:     filterOption.isError,
                id:        index,
                fieldName: filterOption.fieldName
            };
        });

        const chipContainerEl = this.shadowRoot.querySelector('#chip-container');
        if (chipContainerEl || chipContainerData.length) {
            chipContainerEl.data = chipContainerData;
        }
        // Get the actual query
        this._query = this._selector.query;
        if (!this.__blockQuery) {
            // Make query publically available
            this.query = this._query;
        }
        this.__updateFiltersCount();
        ev.stopPropagation();
    }

    __handleOperatorChange(ev) {
        this._selector.operator = ev.detail.value;
        this._query = this._selector.query;
        if (!this.__blockQuery && JSON.stringify(this.query) !== JSON.stringify(this._query)) {
            // Make query publically available
            this.query = this._query;
        }
        ev.stopPropagation();
    }

    __updateDictionary() {
        // notice, that the whole dictionary is updated, even if the only one string was changed as a result of translation :|
        // for sure, this can be the subject of the further improvements
        const tmpDidc = Object.assign({}, this.dictionary);
        for (const stringProp of Object.keys(tmpDidc)) {
            tmpDidc[stringProp] = this[stringProp];
        }
        this.dictionary = tmpDidc;
    }

    _unblockQuery(ev) {
        this.__blockQuery = false;
        ev.stopPropagation();
    }

    _isQueryIdentical(query) {
        return (query === this._query || JSON.stringify(query) === JSON.stringify(this._query) ||
        (query && this._query && !Array.isArray(query.filters) && this._query.filters.filters.length === 1 &&
        JSON.stringify(query.filters) === JSON.stringify(this._query.filters.filters[0])));
    }

    // Assign query and build the filter chips
    _queryChanged(query) {
        if (this.__blockQuery) {
            this._pendingQuery = query || null; // Make sure the pending query is null instead of undefined (= not assigned)
            return;
        }

        if (!this._selector || !this._selector.data && query) {
            this._pendingQuery = query;
            return;
        }

        this._pendingQuery = undefined;

        if (this._isQueryIdentical(query)) {
            return; // No change
        }
        //
        // The client has changed the query
        //
        let el = this.shadowRoot.querySelector('#chip-container')
            ? this.shadowRoot.querySelector('#chip-container').shadowRoot.querySelector('ptcs-chip-data-filter-chip-child') : undefined;
        // Purge internal data from present query (if any): Iterate until current query becomes null
        this.__blockQuery = true;
        while (this._query && el) {
            // Remove the chips of existing query
            this._selector.removeEnteredData(Number(el.getAttribute('data-id')), el.getAttribute('field-name'));
            el = this.shadowRoot.querySelector('#chip-container').shadowRoot.querySelector('ptcs-chip-data-filter-chip-child');
        }

        // Assign the new query
        this._selector.loadQuery(query);
        if (query && query.filters) {
            this._selector.operator = this._resolveAndOrOperator(query.filters.type);
            const filterOperatorEl = this.$['top-bar'] && this.$['top-bar'].querySelector('#filter-operator');
            if (filterOperatorEl) {
                filterOperatorEl.selectedValue = this._selector.operator;
            }
        }

        // Store current query
        const q = this._selector.query;
        this._query = q;
        if (q && JSON.stringify(query) !== JSON.stringify(q)) {
            requestAnimationFrame(() => {
                this.query = q;
            });
        }
    }

    async __updateFiltersCount() {
        await this.updateComplete;
        clearTimeout(this._debounceUpdateFiltersCountTimeoutId);
        this._debounceUpdateFiltersCountTimeoutId = setTimeout(() => {
            const filters = (this._query && this._query.filters) ? this._query.filters.filters.length : 0;
            this._showOperator = filters > 1;
            this.__filtersCount = filters;
            if (filters > 0) {
                this._hideShowChipsLabel = this._showChips ? this.dictionary.stringHideFilters : this.dictionary.stringShowFilters;
                this._showChipToggleBtn = true;
                this.shadowRoot.querySelector('#chip-container').mode = 'closed';
            } else {
                this._showChipToggleBtn = false;
            }
        }, 50);
    }

    reset() {
        // Remove all chips
        const elContainer = this.shadowRoot.querySelector('#chip-container');
        let el = elContainer ? elContainer.shadowRoot.querySelector('ptcs-chip-data-filter-chip-child') : undefined;

        while (this._query && el) {
            this._selector.removeEnteredData(Number(el.getAttribute('data-id')), el.getAttribute('field-name'));
            el = elContainer.shadowRoot.querySelector('ptcs-chip-data-filter-chip-child');
        }

        // Also, reset the selector to its initial state
        this._selector.reset();
    }

    __filtersLabel(filters, stringFilter, stringFilters, showOperator, showAndOrOperator, stringJoinedBy) {
        return filters === 1 ? filters + ' ' + stringFilter
            : `${filters} ${stringFilters}${showOperator && showAndOrOperator ? stringJoinedBy : ''}`;
    }

    getExternalComponentId() {
        return this._selectorId;
    }

    /*
     * Sets an id for external component
     */
    setExternalComponentId(id) {
        if (id) {
            this._selectorId = id;
        } else if (!this._selectorId) {
            this._selectorId = 'ptcs-chip-data-filter-' + performance.now().toString().replace('.', '');
        }

        if (this._selector) {
            this._selector.setAttribute('id', this._selectorId);
        }
    }

    _get_dimension() {
        return {
            // Get window dimension
            windowWidth:  window.innerWidth,
            windowHeight: window.innerHeight,
            // Get current scroll offset
            scrollDx:     document.documentElement.scrollLeft + document.body.scrollLeft,
            scrollDy:     document.documentElement.scrollTop + document.body.scrollTop,
            // Where is the dropdown box?
            box:          this.$['top-bar'].getBoundingClientRect(),
            // Where is the Add Filter button?
            button:       this.$['add-filter'].getBoundingClientRect()
        };
    }
    _set_selector_position(r) {
        const dw = 0;
        const bbSelector = this._selector.getBoundingClientRect();
        const smallModeAllignemt = 8;
        let x;
        if (r.windowWidth - r.box.left - bbSelector.width > 0) {
            x = r.box.left;
        } else if (r.windowWidth > r.box.right && r.box.right - smallModeAllignemt - bbSelector.width > 0) {
            x = r.box.right - smallModeAllignemt - bbSelector.width;
        } else if (r.windowWidth - bbSelector.width - dw - 24 > 0) {
            x = r.windowWidth - bbSelector.width - dw - 24;
        } else {
            x = 2;
        }
        let y = r.button.bottom + smallModeAllignemt;
        if (y + bbSelector.height >= r.windowHeight) {
            // Show popup list above filter instead
            y = Math.max(r.button.top - smallModeAllignemt - bbSelector.height, 2);
        }

        // Set list position
        this._selector.style.left = `${r.scrollDx + x}px`;
        this._selector.style.top = `${r.scrollDy + y}px`;

    }

    // Keep track of list position, if the filter selector box is moved or the view is scrolled
    track_position(rOld) {
        if (this._selector.mode === 'open') {
            if (this._isHidden()) {
                this._selector.mode = 'closed';
            } else {
                const rNew = this._get_dimension();
                if (this._diff_dimension(rOld, rNew)) {
                    if (rNew.box.bottom < 0 || rNew.box.top > rNew.windowHeight) {
                        // The dropdown anchor has been scrolled out of sight. Close the popup
                        this._selector.mode = 'closed';
                        return;
                    }
                    this._set_selector_position(rNew);
                }

                // Take a fresh look at things in 0.2 seconds
                setTimeout(() => this.track_position(rNew), 200);
            }
        }
    }
    _isHidden() {
        return !(this.offsetWidth || this.offsetHeight || this.getClientRects().length);
    }
    _diff_dimension(r1, r2) {
        if (r1.windowWidth !== r2.windowWidth || r1.windowHeight !== r2.windowHeight) {
            return true;
        }
        if (r1.scrollDx !== r2.scrollDx || r1.scrollDy !== r2.scrollDy) {
            return true;
        }
        if (r1.box.width !== r2.box.width || r1.box.bottom !== r2.box.bottom || r1.box.left !== r2.box.left) {
            return true;
        }

        return false;
    }

    _maxWidthChanged(val) {
        if (val) {
            this.style.maxWidth = (val + '').indexOf('px') === -1 ? val + 'px' : val;
        } else {
            this.style.removeProperty('max-width');
        }
    }

    _chipsDisclosureChanged(chipsDisclosure) {
        if (chipsDisclosure === 'none' && !this._showChips) {
            // Toggles this._showChips to true and changes link text
            this.__handleHideChips();
        }
    }

    _resolveAndOrOperator(type) {
        if (!type || typeof type !== 'string') {
            return filterOperatorDefaultValue;
        }

        switch (type.toLowerCase()) {
            case 'and':
                return 'And';
            case 'or':
                return 'Or';
        }
        return filterOperatorDefaultValue;
    }

    _computeTabIndex(tabindex) {
        return tabindex && '-1';
    }

    _xTabindex(tabindex, displayMode) {
        return displayMode !== 'expanded' && tabindex;
    }

    get focusElement() {
        if (!this.tabindex) {
            return null; // Not focusable
        }
        let hit = this.shadowRoot.activeElement;
        let el = hit || document.activeElement;
        while (el && el.shadowRoot && el.shadowRoot.activeElement) {
            hit = hit || el === this || this.contains(el);
            el = el.shadowRoot.activeElement;
        }
        // Focused element must be  in slotted content or in shadow dom
        return (hit || this.shadowRoot.activeElement) && el;
    }

    get _focusableSlotted() {
        return [...this.querySelectorAll('[focusable]')]
            .filter(el => el.clientHeight > 0)
            .reduce((acc, el) => {
                const sub = el.focusableElements;
                if (Array.isArray(sub)) {
                    acc.push(...sub);
                } else {
                    acc.push(el);
                }
                return acc;
            }, []);
    }

    get focusableElements() {
        const selector = this._selector ? this._selector.focusableElements : [];
        const _topBarEls = this.$['top-bar'] ? [...this.$['top-bar'].querySelectorAll('[tabindex]')].filter(el => el.clientHeight > 0) : [];
        return [
            ..._topBarEls,
            ...this._focusableSlotted,
            ...selector
        ];
    }

    get focusableElementsChipContainer() {
        if (this._hideChipsArea(this._showChips, this._query)) {
            return [];
        }
        return [
            ...(this.shadowRoot.querySelector('#chip-container') ? this.shadowRoot.querySelector('#chip-container').focusableElements : [])
        ];
    }

    _focusEv(ev) {
        // Ignore if we don't support focusing or already have focus on a sub element
        if (!this.tabindex || this.shadowRoot.activeElement) {
            return;
        }

        // Restore old focus or initialize new focus
        const focusableElms = [...this.focusableElements, ...this.focusableElementsChipContainer];

        if (!this._focusEl || focusIndex(this._focusEl, focusableElms) === -1) {
            this._focusElTopBar = this._focusEl = this.focusableElements[0];
        }
        if (this._focusEl) {
            this._focusEl.focus();
        }
    }

    _mouseDown() {
        // Keep track of focused sub-element, so focus can be restored
        requestAnimationFrame(() => {
            this._focusElTopBar = this.focusElement;
        });
    }

    get chipFirstElementChild() {
        const chipContainer = this.shadowRoot.querySelector('#chip-container');
        return chipContainer ? chipContainer.shadowRoot.querySelector('#container').firstElementChild : null;
    }

    get activeElement() {
        return this.shadowRoot.activeElement;
    }

    _keyDown(ev) {
        // This element must be focusable, or the key event is only for a textfield
        if (ev.defaultPrevented || !this.tabindex) {
            return;
        }

        const key = ev.key;
        let focusEl = this.focusElement;
        if (!focusEl) {
            return;
        }

        if (!focusEl.tagName.toLowerCase().includes('ptcs-')) {
            focusEl = focusEl.getRootNode().host;
        }

        // Special rules for INPUT
        if (focusEl.tagName === 'INPUT' || focusEl.tagName === 'PTCS-TEXTFIELD') {
            switch (key) {
                case 'ArrowLeft':
                case 'Home':
                    if (focusEl.selectionEnd > 0) {
                        return; // Ignore unless cursor is at start of text
                    }
                    break;
                case 'End':
                case 'ArrowRight':
                    if (focusEl.selectionStart < focusEl.value.length) {
                        return; // Ignore unless cursor is at end of text
                    }
                    break;
                case ' ':
                    // Space should be added to the text as expected
                    return;
            }
        }
        const focusableContainer = this.focusableElementsChipContainer;
        const indexContainer = focusIndex(this._focusElChipContainer, focusableContainer);

        const focusable = this.focusableElements;
        let index = focusIndex(this._focusElTopBar, focusable);

        if (index === -1) {
            // Out of sync. Reset
            index = 0;
        }

        const chipContainerEl = this.shadowRoot.querySelector('#chip-container');
        let newFocusEl = focusEl;
        const currentFocusable =
        this.activeElement === chipContainerEl && !!chipContainerEl
            ? focusableContainer : focusable;
        const currentIndex =
            this.activeElement === chipContainerEl && !!chipContainerEl
                ? indexContainer : index;
        switch (ev.key) {
            case 'Tab' :
                if (ev.shiftKey) {
                    // Shift Tab
                    if (focusableContainer.includes(focusEl) && chipContainerEl) {
                        newFocusEl = (index !== -1) ? focusable[index] : focusable[0];
                        // Prevent backwards Tab navigation from stopping on this (ptcs-chip-data-filter) element
                    } else if (!delegateToPrev(this)) {
                        (this.focusElement || this).blur();
                    }
                    ev.preventDefault();
                // Tab
                } else if (!ev.shiftKey && focusable.includes(focusEl) && chipContainerEl ? !chipContainerEl.hidden : null) {
                    newFocusEl = (indexContainer !== -1) ? focusableContainer[indexContainer] : focusableContainer[0];
                    ev.preventDefault();
                }
                break;
            case 'Home':
                newFocusEl = currentFocusable[0];
                break;
            case 'End':
                newFocusEl = currentFocusable[currentFocusable.length - 1];
                break;
            case 'ArrowLeft':
            case 'ArrowUp':
                newFocusEl = currentFocusable[currentIndex === 0 ? currentFocusable.length - 1 : currentIndex - 1];
                break;
            case 'ArrowRight':
            case 'ArrowDown':
                newFocusEl = currentFocusable[currentIndex === currentFocusable.length - 1 ? 0 : currentIndex + 1];
                break;
            case 'Enter':
            case ' ':
                if (typeof focusEl.closeChip === 'function') {
                    focusEl.closeChip();
                } else {
                    focusEl.click();
                }
                ev.preventDefault();
                return;
        }

        if (newFocusEl && newFocusEl !== focusEl) {
            newFocusEl.focus();
            // Save last focused element of chip-container and top-bar
            if (focusableContainer.includes(newFocusEl)) {
                this._focusElChipContainer = newFocusEl;
            }
            if (focusable.includes(newFocusEl)) {
                this._focusElTopBar = newFocusEl;
            }
            this._focusEl = newFocusEl;
            ev.preventDefault();
        }
    }

    _updateSortFilters(v) {
        this.sortFilters = !v;
    }

    _updateDisableFilterSorting(v) {
        this.disableFilterSorting = !v;
    }
};

customElements.define(DataFilter.is, DataFilter);
