import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-chip-data-filter/ptcs-chip-data-filter.js';
import './ptcs-toolbar-tools.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';
import 'ptcs-icons/cds-icons.js';

/*
type ActionType = 'button' | 'link' | 'toggle' | 'dropdown';

type ButtonOptions = {
    // Default: 'transparent'
    variant?: string;

    // Action icon - see ptcs-icon
    icon?: string;
    iconSet?: string;
};

type LinkOptions = {
    // Default: 'primary'
    variant?: string;
};

type ToggleOptions = {
    // Initial value. Default: false. Can be changed with setValue(id, checked?)
    value?: boolean;

    // Hide selection icon?
    hideIcon?: boolean;
};

type DropdownValues = {
    // Displayed option label
    label: string;

    // Corresponding value. Default: label
    value?: any;
};

type DropdownOptions = {
    values: DropdownValues[];

    // Initial value
    value?: any;
};

type ActionOptions = ButtonOptions | LinkOptions | ToggleOptions | DropdownOptions;

type Action = {
    type: ActionType,

    // Identifies the action for hiding / disabling it
    id?: string | number;

    // Action label
    label?: string;

    // Alt action text for screen reader
    alt?: string;

    // Link
    link?: string;

    // Disable action? Can be changed with setDisabled(id, disabled);
    disabled?: boolean;

    // Hide action? Can be changed with setHidden(id, hidden);
    hidden?: boolean;

    // Explicit width of control
    width?: string | number;

    // Limit max width of control
    maxWidth?: string | number;

    // Options - dependent on ActionType
    opt?: ActionOptions;
}
*/

PTCS.ToolBar = class extends PTCS.BehaviorTabindex(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement)))) {
    static get styles() {
        return css`
        :host {
            display: flex;
            width: 100%;
            box-sizing: border-box;
        }
        :host([aria-disabled="true"]) {
            pointer-events: none;
        }
        ptcs-textfield[hidden] {
            display: none;
        }
        ptcs-toolbar-tools[hidden] {
            display: none;
        }
        [part=data-filter] {
            box-sizing: border-box;
            display: inline-flex;
            position: relative;
        }
        [part=pipe-container] {
            position: absolute;
        }
        :host(:not([chips-on-top])) [part=pipe-container] {
            top: 0;
        }
        :host([chips-on-top]) [part=pipe-container] {
            bottom: 0;
        }
        [part=pipe-container][hidden] {
            display: none;
        }`;
    }

    render() {
        return html`
        <ptcs-chip-data-filter part="data-filter" id="data-filter"
            tabindex=${this._delegatedFocus}
            .data=${this.filterData}
            .query=${this.query}
            @query-changed=${this._queryChanged}
            .disabled=${this.disabled}
            .showAndHideFilters=${this.showAndHideFilters}
            .hideFilterCounter=${!this.showAndHideFilters}
            .chipsOnTop=${this.chipsOnTop}
            .chipsDisclosure=${this.chipsDisclosure}
            .hideFilter=${this._hideDataFilter(this.hideFilter, this.simpleFilter)}
            .showListFilter=${this.showListFilter}
            .sortFilters=${this.sortFilters}
            .borders=${this.borders}
            .categoryLabel=${this.categoryLabel}
            .conditionLabel=${this.conditionLabel}
            .filterHintText=${this.filterHintText}
            .latitudeLabel=${this.latitudeLabel}
            .longitudeLabel=${this.longitudeLabel}
            .rangeStartValueLabel=${this.rangeStartValueLabel}
            .rangeEndValueLabel=${this.rangeEndValueLabel}
            .unitsLabel=${this.unitsLabel}
            .valueLabel=${this.valueLabel}
            .customBaseTypesMapping=${this.customBaseTypesMapping}
            .columnFormat=${this.columnFormat}>
            <div part="pipe-container"
                ?hidden=${this._hidden(this.separator, this.actions, this.rightActions, this.hideFilter, this.simpleFilter, this.query)}>
                <div part="pipe" id="pipe-right"></div>
            </div>
            <div style="display: flex; align-items: flex-end; overflow: hidden" id="toolbar-container">
                <ptcs-toolbar-tools id="toolbar" part="tools"
                    focusable=${this._delegatedFocus}
                    .disabled=${this.disabled}
                    .actions=${this.actions}
                    .rightActions=${this.rightActions}
                    .minWidth=${this.toolbarMinWidth}
                    @min-width-changed=${this._minWidthChanged}
                    .showFilter=${this._showToolsFilter(this.hideFilter, this.simpleFilter)}
                    .filterLabel=${this.filterLabel}
                    .additionalLabel=${this.additionalLabel}
                    .filterIcon=${this.filterIcon}
                    .filterWidth=${this.simpleFilterWidth}
                    .filterHintText=${this.filterHintText}
                    .filterPos=${this.simpleFilterPos}
                    .filterAlign=${this.simpleFilterAlignment}
                    .filterTooltip=${this.filterTooltip}
                    .filterString=${this.filterString}
                    @filter-string-changed=${this._filterStringChanged}
                    .rightOverflowLabel=${this.rightOverflowLabel}>
                </ptcs-toolbar-tools>
            </div>
        </ptcs-chip-data-filter>`;
    }

    static get is() {
        return 'ptcs-toolbar';
    }

    static get properties() {
        return {
            variant: {
                type:    String,
                reflect: true
            },

            // Disables the toolbar
            disabled: {
                type: Boolean
            },

            // Self-reported minimum width of ptcs-toolbar-tools
            toolbarMinWidth: {
                type:      Number,
                attribute: 'toolbar-min-width'
            },

            // The data for the filter options in the ptcs-chips-data-filter
            filterData: {
                type:      Object,
                attribute: 'filter-data'
            },

            // Hides the filter region
            hideFilter: {
                type:      Boolean,
                attribute: 'hide-filter'
            },

            // Use simple filter instead of chips filter
            simpleFilter: {
                type:      Boolean,
                attribute: 'simple-filter'
            },

            // The position of the simple filter.
            // You can place the filter in the `'left'`, `'right'` , or `'center'` regions.
            simpleFilterPos: {
                type:      String,
                attribute: 'simple-filter-pos'
            },

            simpleFilterAlignment: {
                type:      String,
                attribute: 'simple-filter-alignment'
            },

            // Actions for the action region
            actions: {
                type: Array // Action[]
            },

            // Actions for the right actions region
            rightActions: {
                type: Array // Action[]
            },

            // Generated filter query from the ptcs-chips-data-filter
            query: {
                type:   Object,
                notify: true
            },

            // Show filter chips on top of the toolbar?
            chipsOnTop: {
                type:      Boolean,
                attribute: 'chips-on-top',
                reflect:   true
            },

            // Specifies the icon in the simple filter.
            filterIcon: {
                type:      String,
                attribute: 'filter-icon'
            },

            // Tooltip for the simple filter
            filterTooltip: {
                type:      String,
                attribute: 'filter-tooltip'
            },

            // Placeholder text for the simple filter
            filterHintText: {
                type:      String,
                attribute: 'filter-hint-text'
            },

            // The text that has been entered in the simple filter text
            filterString: {
                type:      String,
                attribute: 'filter-string',
                notify:    true
            },

            // Specifies how to handle the "Show Filters" / "Hide Filters" toggle in the ptcs-chips-data-filter:
            // `'link'` - use a ptcs-link,
            // `'icon'` - use a ptcs-icon,
            // `'none'` - hide the disclosure toggle and always show the active filters / chips.
            chipsDisclosure: {
                type:      String,
                attribute: 'chips-disclosure'
            },

            // letters 'tblr' in any order to enable border-top / border-bottom / border-left / border-right
            borders: {
                type: String
            },

            // Toggle between hiding the filter disclosure controls and expanding the chip container for the data filter
            showAndHideFilters: {
                type:      Boolean,
                attribute: 'show-and-hide-filters'
            },

            // Toggle to show filter box in the dropdown list of filter categories
            showListFilter: {
                type:      Boolean,
                attribute: 'show-list-filter'
            },

            // Sorts the list of options for the data filter categories in alphabetical order
            sortFilters: {
                type:      Boolean,
                attribute: 'sort-filters'
            },

            // Toggle to display vertical separator between the chip data filter and actions
            separator: {
                type: Boolean
            },

            ariaDisabled: {
                type:      String,
                computed:  '_disabled(disabled)',
                attribute: 'aria-disabled',
                reflect:   true
            },

            // Full override of date format
            filterFormatToken: {
                type:      String,
                attribute: 'filter-format-token'
            },

            filterDateOrder: {
                type:      String, //  auto, YMD, MDY, DMY (auto is default format)
                attribute: 'filter-date-order'
            },

            // Label above the simple filter control
            filterLabel: {
                type:      String,
                attribute: 'filterLabel'
            },

            // Specifies an additional label that occurs before the toolbar left actions
            additionalLabel: {
                type:      String,
                attribute: 'additional-label'
            },

            // Sets a specific width in pixels for the simple filter box, although its actual width
            // may be limited by CSS `min-width` on part `simple-filter`
            simpleFilterWidth: {
                type:      Number,
                attribute: 'simple-filter-width'
            },

            // The text displayed above the drop-down list for the filter categories in the chip filter
            categoryLabel: {
                type:      String,
                attribute: 'category-label'
            },

            // The text displayed above the drop-down list for the filter condition in the chip filter
            conditionLabel: {
                type:      String,
                attribute: 'condition-label'
            },

            // The text displayed above the box which contains the value for the condition in the chip filter
            valueLabel: {
                type:      String,
                attribute: 'value-label'
            },

            // The text displayed above the first input box when filtering a range of values in the chip filter
            rangeStartValueLabel: {
                type:      String,
                attribute: 'range-start-value-label'
            },

            // The text displayed above the second input box when filtering a range of values in the chip filter
            rangeEndValueLabel: {
                type:      String,
                attribute: 'range-end-value-label'
            },

            // The text displayed above the drop-down list that is used to set the units when filtering by location or date in the chip filter
            unitsLabel: {
                type:      String,
                attribute: 'units-label'
            },

            // The text displayed above the input box for latitude when filtering by location in the chip filter
            latitudeLabel: {
                type:      String,
                attribute: 'latitude-label'
            },

            // The text displayed above the input box for longitude when filtering by location in the chip filter
            longitudeLabel: {
                type:      String,
                attribute: 'longitude-label'
            },

            // Specifies a label for the collapsed right area button
            rightOverflowLabel: {
                type:      String,
                attribute: 'right-overflow-label'
            },

            isEmpty: {
                type:      Boolean,
                computed:  '_computeIsEmpty(hideFilter, actions, rightActions)',
                attribute: 'is-empty',
                reflect:   true
            },

            // ARIA attributes

            role: {
                type:    String,
                reflect: true
            },

            customBaseTypesMapping: {
                type:      Object,
                attribute: 'custom-base-types-mapping'
            },

            columnFormat: {
                type:      String,
                attribute: 'column-format'
            },

            _delegatedFocus: String,

            _resizeObserver: ResizeObserver,
        };
    }

    static get observers() {
        // _updateMinWidth: Properties that affects the minimum width of the toolbar
        // Note: should also watch the private _showChips in the chips-filter, but...
        return [
            '_updateMinWidth(toolbarMinWidth, query, showAndHideFilters, chipsDisclosure, hideFilter, simpleFilter)',
            '_updateSeparator(separator, simpleFilter, actions, rightActions, query)'
        ];
    }

    constructor() {
        super();

        // Initialize values here instead of in the properties
        this.filterIcon = 'cds:icon_filter';
        this.filterHintText = 'Filter';
        this.role = 'toolbar';
        this.columnFormat = null;

        // Create the ResizeObserver in the constructor
        this.__monitorWidthCb = this._monitorWidth.bind(this);
        this._resizeObserver = new ResizeObserver(this.__monitorWidthCb);
    }

    ready() {
        super.ready();
        if (this.simpleFilter === undefined) {
            this.simpleFilter = false;
        }

        if (this.actions === undefined) {
            this.actions = null; // Make sure ptcs-toolbar-tools is hidden if no actions (or views)
        }
        if (this.variant === undefined) {
            this.variant = 'primary';
        }
    }

    connectedCallback() {
        super.connectedCallback();

        // Make sure everything (like the toolbar part used below) is created
        this.performUpdate();

        this._resizeObserver.observe(this);
        this._resizeObserver.observe(this.$.toolbar);

        // Complement the resize observer to monitor size changes of the browser window itself. If you resize
        // quickly, the resizeObserver is not getting invoked sufficiently (original comment by Hasse).
        window.addEventListener('resize', this.__monitorWidthCb);
    }

    disconnectedCallback() {
        this._resizeObserver.unobserve(this);
        this._resizeObserver.unobserve(this.$.toolbar);
        window.removeEventListener('resize', this.__monitorWidthCb);
        super.disconnectedCallback();
    }

    _filterStringChanged(ev) {
        if (this.filterString !== ev.detail.value) {
            this.filterString = ev.detail.value;
        }
    }

    _queryChanged(ev) {
        if (this.query !== ev.detail.value) {
            this.query = ev.detail.value;
        }
    }

    _minWidthChanged(ev) {
        if (this.minWidth !== ev.detail.value) {
            this.minWidth = ev.detail.value;
        }
    }

    // Return the CSS min-width of the simple filter set via theming (if any)
    get simpleFilterMinWidth() {
        return this.$.toolbar.simpleFilterMinWidth;
    }

    _disabled(disabled) {
        // In Lit, set the value to 'undefined' if not true to get the same result as in Polymer
        return disabled ? 'true' : undefined;
    }

    // Whenever the available space (might) have changed
    _monitorWidth() {
        if (!this.__resizing) {
            this.__resizing = true;
            requestAnimationFrame(() => {
                this.__resizing = undefined;
                const bb = this.$.toolbar.getBoundingClientRect();
                this.$.toolbar.maxWidth = bb.right - bb.left;
            });
        }
    }

    // ptcs-toolbar-tools has a new minimum width
    _updateMinWidth(toolbarMinWidth /* , query, showAndHideFilters, chipsDisclosure, hideFilter, simpleFilter */) {
        const bb0 = this.getBoundingClientRect();
        const bb1 = this.$.toolbar.getBoundingClientRect();
        const cs = getComputedStyle(this);
        const margin = PTCS.cssDecodeSize(cs.marginLeft, this) + PTCS.cssDecodeSize(cs.marginRight, this);
        const cstb = getComputedStyle(this.$.toolbar);
        const padding = PTCS.cssDecodeSize(cstb.paddingLeft, this.$.toolbar) + PTCS.cssDecodeSize(cstb.paddingRight, this.$.toolbar);

        // Add width for chips filter, if visible, toolbar tools padding, and toolbar margins
        this.style.minWidth = `${bb1.left - bb0.left + toolbarMinWidth + margin + padding + bb0.right - bb1.right}px`;
    }

    _hideDataFilter(hideFilter, simpleFilter) {
        return hideFilter || simpleFilter;
    }

    _showToolsFilter(hideFilter, simpleFilter) {
        return !hideFilter && simpleFilter;
    }

    setDisabled(id, disabled) {
        this.performUpdate();
        this.$.toolbar.setDisabled(id, disabled);
    }

    setLabel(id, label) {
        this.performUpdate();
        this.$.toolbar.setLabel(id, label);
    }

    setTooltip(id, alt) {
        this.performUpdate();
        this.$.toolbar.setTooltip(id, alt);
    }

    setHidden(id, hidden) {
        this.performUpdate();
        this.$.toolbar.setHidden(id, hidden);
    }

    setValue(id, value) {
        this.performUpdate();
        this.$.toolbar.setValue(id, value);
    }

    setSelected(id, selected) {
        this.performUpdate();
        this.$.toolbar.setSelected(id, selected);
    }

    setArrowDownActivate(id, activate) {
        this.$.toolbar.setArrowDownActivate(id, activate);
    }

    _computeIsEmpty(hideFilter, actions, rightActions) {
        if (!hideFilter) {
            return false;
        }
        if (Array.isArray(actions) && actions.length > 0) {
            return false;
        }
        if (Array.isArray(rightActions) && rightActions.length > 0) {
            return false;
        }
        return true;
    }

    _updateSeparator(separator, simpleFilter, actions, rightActions /* , query */) {
        PTCS.setbattr(this, 'pipe-right', separator && !simpleFilter);
        if (separator && (actions !== null || rightActions !== null)) {
            const df = this.$['data-filter'];
            requestAnimationFrame(() => {
                this.$['pipe-right'].style.height = df.topBarHeight + 'px';
            });
        }
    }

    _hidden(separator, actions, rightActions, hideFilter, simpleFilter, /* , query */) {
        return !separator || hideFilter || simpleFilter ||
            (actions === null && (rightActions === null || rightActions === undefined));
    }
};

customElements.define(PTCS.ToolBar.is, PTCS.ToolBar);
