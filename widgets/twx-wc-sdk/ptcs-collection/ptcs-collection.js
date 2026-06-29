import {LitElement, html, css} from 'lit';
import {PTCS} from 'ptcs-library/library.js';
import {SelectionMgr} from 'ptcs-chart/selection/chart-selection.js';
import {getFocusable, getLastFocusable, delegateToPrev, delegateToNext} from 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-icon/ptcs-icon.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-v-scroller/ptcs-v-scroller2.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';

let __lastTabKey = 0; // Last time that the Tab key was pressed

const groupKey = Symbol('$groupKey');
const groupType = Symbol('$groupType'); // 'header' | 'footer'
const isCell = Symbol('$isCell');
const hasGhost = Symbol('$hasGhosts');
const altIx = Symbol('$altIx');

const userDataStates = {loading: 'loading', error: 'error'};
const iconPropNames = {loading: 'iconStateLoading', 'no-data': 'iconStateNoData', empty: 'iconStateEmpty', error: 'iconStateError'};
const labelPropNames = {loading: 'labelStateLoading', 'no-data': 'labelStateNoData', empty: 'labelStateEmpty', error: 'labelStateError'};

const DOUBLE_CLICK_DELAY = 400;
const LONG_CLICK_DELAY = 400;

function sortBy(field) {
    return (a, b) => {
        const v1 = a[field];
        const v2 = b[field];

        return (isNaN(v1) || isNaN(v2)) ? `${v1}`.localeCompare(`${v2}`) : (v1 - v2);
    };
}

function groupBy(field) {
    return item => item[field];
}

function clickOnScrollbar(ev) {
    const el = ev.composedPath()[0];
    if (!el) {
        return false; // Better safe than sorry
    }
    const bb = el.getBoundingClientRect();

    // Clicked on vertical scroll?
    if (el.clientHeight < el.scrollHeight && ev.clientX - bb.left >= el.clientWidth) {
        return true;
    }

    // Clicked on horizontal scroll?
    if (el.clientWidth < el.scrollWidth && ev.clientY - bb.top >= el.clientHeight) {
        return true;
    }

    return false;
}

function expandSlots(acc, el) {
    if (el instanceof HTMLSlotElement) {
        acc.push(...getFocusable(el));
    } else {
        acc.push(el);
    }
    return acc;
}

function stripGhosts(el) {
    if (el && el[hasGhost]) {
        while (el.lastElementChild.hasAttribute('ghost')) {
            el.lastElementChild.remove();
        }
        el[hasGhost] = undefined;
    }
}

const isScrollKey = key => ['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End', 'PageUp', 'PageDown'].indexOf(key) >= 0;

const getFocusableExpandSlots = el => [...getFocusable(el)].reduce(expandSlots, []);

// Focus on el, if it exist and has a tabindex. Otherwise return false
const focusOn = el => (!!el && (el.tabIndex >= 0 || el.hasAttribute('tabindex')) && el.focus()) === undefined; // only el.focus() returns undefined

const _leftMouseButtonOrTouch = ev => (ev instanceof MouseEvent && typeof ev.button === 'number' && ev.button === 0) ||
ev instanceof TouchEvent && ev.button === undefined;

// Compare row order of two row elements
const compareRows = (a, b) => Number(a.getAttribute('rowix')) - Number(b.getAttribute('rowix'));

// Grab cell elements of row element
const collectCells = (acc, row) => {
    acc.push(...row.children);
    return acc;
};


PTCS.Collection = class extends PTCS.BehaviorTabindex(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(LitElement))) {
    static get styles() {
        return css`
        :host {
            min-width: 34px;
            min-height: 34px;
            position: relative;
            display: block;
            height: var(--ptcs-collection-height, var(--ptcs-collection-max-height, 750px));
        }

        :host(:not([pin-headers])) [sticky-header] {
            display: none;
        }

        :host(:not([pin-footers])) [sticky-footer] {
            display: none;
        }

        [sticky-header], [sticky-footer] {
            position: absolute;
            left: var(--ptcs-collection-space-left, 0px);
            right: calc(var(--ptcs-collection-space-right, 0px) + var(--ptcs-collection-sbw, 0px));
            pointer-events: none;
        }

        [sticky-header] {
            top: 0;
            border-bottom: solid 2px white;
        }

        [sticky-footer] {
            bottom: 0;
            border-top: solid 2px white;
        }

        #chunker {
            height: 100%;
            width: 100%;
        }

        [part=container] {
            width: 100%;
            height: 100%;
        }

        [part=row], [part=header], [part=footer] {
            display: flex;
            justify-content: space-evenly;
            justify-items: center;
            align-items: center;
            box-sizing: border-box;
            padding-left: var(--ptcs-collection-space-left, 0px);
            padding-right: var(--ptcs-collection-space-right, 0px);
        }

        :host([layout=grid]) [part=row] {
            display: grid;
            grid-template-columns: repeat(var(--ptcs-collection-num-col, 12), minmax(0, 1fr));
            grid-column-gap: var(--ptcs-collection-column-gap, 4px);
        }

        :host([row-horizontal-alignment=space-between]) [part=row] {
            justify-content: space-between;
        }

        :host([layout=flex][last-line-alignment=space-between]) [part=row][last-line] {
            justify-content: space-between;
        }

        :host([row-horizontal-alignment=left]) [part=row] {
            justify-content: left;
            justify-items: start;
        }

        :host([layout=flex][last-line-alignment=left]) [part=row][last-line] {
            justify-content: left;
            justify-items: start;
        }

        :host([row-horizontal-alignment=center]) [part=row] {
            justify-content: center;
        }

        :host([layout=flex][last-line-alignment=center]) [part=row][last-line] {
            justify-content: center;
        }

        :host([row-horizontal-alignment=right]) [part=row] {
            justify-content: right;
            justify-items: end;
        }

        :host([layout=flex][last-line-alignment=right]) [part=row][last-line] {
            justify-content: right;
            justify-items: end;
        }

        :host([row-horizontal-alignment=stretch]) [part=row] {
            justify-content: stretch;
            justify-items: stretch;
        }

        :host([last-line-alignment=stretch]) [part=row][last-line] {
            justify-content: stretch;
            justify-items: stretch;
        }

        :host([layout=flex][row-horizontal-alignment=stretch]) [part=row] > * {
            flex: 1 0 auto;
        }

        :host([layout=flex][last-line-alignment=stretch]) [part=row][last-line] > * {
            flex: 1 0 auto;
        }

        :host([layout=flex][last-line-alignment]:not([last-line-alignment=stretch])) [part=row][last-line] > * {
            flex: 0 0 auto;
        }

        :host([layout=grid][row-horizontal-alignment=stretch]) [part=row] > * {
            width: unset !important;
        }

        :host([layout=table]) [part=row] > * {
            width: calc(var(--ptcs-collection--table-width,100%) - var(--ptcs-collection-space-left,0px) - var(--ptcs-collection-space-right,0px));
        }

        :host([row-vertical-alignment=top]) [part=row] {
            align-items: flex-start;
        }

        :host([row-vertical-alignment=bottom]) [part=row] {
            align-items: flex-end;
        }

        :host([row-vertical-alignment=stretch]) [part=row] {
            align-items: stretch;
        }

        :host([stretch-single-row-height]) [part=row] {
            align-items: stretch;
        }

        [part=header] > *, [part=footer] > * {
            grid-column-start: 1;
            grid-column-end: var(--ptcs-collection-num-col, 12);
        }

        [part=header][top], [part=row][top] {
            padding-top: var(--ptcs-collection-space-above, 0px);
        }

        [part=footer][bottom], [part=row][bottom] {
            padding-bottom: var(--ptcs-collection-space-below, 0px);
        }

        [part=header]:not([top]), [part=row][start]:not([top]) {
            padding-top: var(--ptcs-collection-group-gap, var(--ptcs-collection-row-gap, 8px));
        }

        [part=row]:not([top]), [part=footer] {
            padding-top: var(--ptcs-collection-row-gap, 8px);
        }

        :host([layout=flex]) [part=row] {
            gap: var(--ptcs-collection-column-gap, 4px);
        }

        [part=message-container] {
            position: absolute;
            left: 0;
            right: 0;
            top: 0;
            bottom: 0;
            display: flex;
            flex-direction: column;
            flex-wrap: wrap;
            justify-content: center;
            align-items: center;
            align-content: center;
        }

        [part=message-container][hidden] {
            display: none !important;
        }

        :host([custom-message]) [part=message-icon] {
            display: none !important;
        }

        :host([custom-message]) [part=message-label] {
            display: none !important;
        }

        :host(:not([custom-message])) [part=message-custom] {
            display: none !important;
        }

        [rowix] > :focus {
            outline: none;
        }

        :host(:not([hide-focus])) [rowix] > :focus {
            outline-style: var(--ptcs-focus-overlay--border-style, solid);
            outline-width: var(--ptcs-focus-overlay--border-width, 2px);
            outline-color: var(--ptcs-focus-overlay--border-color, #0094c8);
            /* If we put the outline on top of the cell, the cell content may hide it  */
            /* outline-offset: calc(-1 * var(--ptcs-focus-overlay--border-width, 2px)); */
        }

        div[invisible] {
            visibility: hidden !important;
            overflow: hidden;
        }

        div[ghost] {
            visibility: hidden;
            pointer-events: none;
        }`;
    }

    render() {
        // eslint-disable-next-line max-len
        const dataState = userDataStates[this.dataState] || (!Array.isArray(this.items) && 'no-data') || (this.items.length === 0 && 'empty') || this.customMessage;
        const icon = dataState && this[iconPropNames[dataState]];
        const label = dataState && this[labelPropNames[dataState]];

        return html`<div part="message-container" ?hidden=${!(icon || label || this.customMessage)} state=${dataState || 'data'}>
                <ptcs-icon part="message-icon" .icon=${icon}></ptcs-icon>
                <ptcs-label variant="label" part="message-label" .label=${label}></ptcs-label>
                <div part="custom-message" ?hidden=${!this.customMessage}><slot name="custom-message"></slot></div>
            </div>
            <div part="container" ?invisible=${icon || label || this.customMessage}>
                <ptcs-v-scroller2 id="chunker" @gap-changed=${this._onGapChanged} @resized-width=${this._onResizeWidth}
                    .createItemElement=${this._createCollectionRowRef} .recycleItemElement=${this._recycleCollectionRowRef}
                    .removeItemElement=${this._removeCollectionRowRef}
                    .pickReuseItemElement=${this._pickReuseItemElementRef} .loadAll=${this.loadAll}
                    @mousedown=${this._onMouseDown} @mouseup=${this._onMouseUp} @contextmenu=${this._onContextMenu}
                    @touchstart=${this._onMouseDown} @touchend=${this._onMouseUp}
                    @repainted=${this._onScroll}
                    .contentAlignment=${this.contentAlignment}></ptcs-v-scroller2>
                <div sticky-header id="stickyhead"></div><div sticky-footer id="stickyfoot"></div>
            </div>`;
    }

    static get is() {
        return 'ptcs-collection';
    }

    static get properties() {
        return {
            // 'flex' | 'grid' | 'table'
            layout: {
                type:    String,
                reflect: true
            },

            // Collection items
            items: {
                type: Array
            },

            disabled: {
                type:       Boolean,
                reflect:    true,
                hasChanged: (value, old) => !value !== !old // Compare effective boolean value
            },

            // 'space-between' | 'space-evenly' | 'left' | 'center' | 'right' | 'stretch'
            rowHorizontalAlignment: {
                type:      String,
                reflect:   true,
                attribute: 'row-horizontal-alignment'
            },

            // 'space-between' | 'space-evenly' | 'left' | 'center' | 'right' | 'stretch'
            lastLineAlignment: {
                type:      String,
                reflect:   true,
                attribute: 'last-line-alignment'
            },

            // Pad last rows with "ghost cells" (same width as the last cell)?
            leftAlignLastRow: {
                type:      Boolean,
                attribute: 'left-align-last-row'
            },

            // 'top' | 'center' | 'bottom' | 'stretch'
            rowVerticalAlignment: {
                type:      String,
                reflect:   true,
                attribute: 'row-vertical-alignment'
            },

            uniformHeight: {
                type:      Boolean,
                attribute: 'uniform-height'
            },

            // 'top' | 'center' | 'bottom'
            contentAlignment: {
                type:      String,
                attribute: 'content-alignment'
            },

            // Get width of item: field name or function(items[index], index)
            itemWidth: {
                type:      String,
                attribute: 'item-width'
            },

            // Get height of item: field name or function(items[index], index)
            itemHeight: {
                type:      String,
                attribute: 'item-height'
            },

            // Field name or compare function
            sort: {
                type: String
            },

            // 'none' || 'single' || 'multiple'
            selectionMode: {
                type:      String,
                attribute: 'selection-mode'
            },

            // Group field name or group function(items[index], index) => group value
            group: {
                type: String
            },

            // Put whole group in a single row?
            disableWrapping: {
                type:      Boolean,
                attribute: 'disable-wrapping'
            },

            // Stretch height of (single) row, if disableWrapping?
            stretchRowHeight: {
                type:      Boolean,
                attribute: 'stretch-row-height'
            },

            // Scroll to selected item when the collection is resized?
            autoScroll: {
                type:      Boolean,
                attribute: 'auto-scroll'
            },

            // Automatically select first cell
            autoSelectFirstItem: {
                type:      Boolean,
                attribute: 'auto-select-first-item'
            },

            // Create empty cell
            createCell: {
                type: Function
            },

            // Display content in cell
            assignCell: {
                type: Function // (cellEl, item, index, selected, this)
            },

            // Create empty header
            createHeader: {
                type: Function
            },

            // Display content in header
            assignHeader: {
                type: Function
            },

            // Create empty footer
            createFooter: {
                type: Function
            },

            // Display content in footer
            assignFooter: {
                type: Function
            },

            // Show headers?
            headers: {
                type: Boolean
            },

            // Show footers?
            footers: {
                type: Boolean
            },

            // Current header key
            _headerKey: {
                type: String
            },

            // True if the sticky header should be displayed (= is the actual top header NOT fully visible)
            _showStickyHeader: {
                type: Boolean
            },

            // Current footer key
            _footerKey: {
                type: String
            },

            // True if the sticky footer should be displayed (= is the actual bottom header NOT fully visible)
            _showStickyFooter: {
                type: Boolean
            },

            // Create a collection row element
            _createCollectionRowRef: {
                type:  Function,
                state: true
            },

            // Set explicit data state: 'data', 'loading', 'error'
            dataState: {
                type:      String,
                attribute: 'data-state'
            },

            labelStateLoading: {
                type:      String,
                attribute: 'label-state-loading'
            },

            iconStateLoading: {
                type:      String,
                attribute: 'icon-state-loading'
            },

            labelStateNoData: {
                type:      String,
                attribute: 'label-state-no-data'
            },

            iconStateNoData: {
                type:      String,
                attribute: 'icon-state-no-data'
            },

            labelStateEmpty: {
                type:      String,
                attribute: 'label-state-empty'
            },

            iconStateEmpty: {
                type:      String,
                attribute: 'icon-state-empty'
            },

            labelStateError: {
                type:      String,
                attribute: 'label-state-error'
            },

            iconStateError: {
                type:      String,
                attribute: 'icon-state-error'
            },

            // Display a custom message via <... slot="custom-message">custom message</...>
            customMessage: {
                type:      Boolean,
                reflect:   true,
                attribute: 'custom-message'
            },

            preventCellContextMenu: {
                type: Boolean
            },

            // Control focus tabbing:
            // - 'cell' - stays inside cell - rollback focus bewteen items. Must press Escape to leave card
            // - 'collection' - move to following / preceding cell when at first / last item
            // - 'item' - navigate tabbable elements in cards only, ignore cards (only navigate items in the DOM, ignore virtual scrolling)
            cellTabKeyScope: {
                type:      String,
                attribute: 'cell-tab-key-scope'
            },

            _disableCellDoubleClicked: {
                type: Boolean
            }
        };
    }

    constructor() {
        super();

        this.layout = 'flex';
        this.rowHorizontalAlignment = 'space-evenly';
        this.__$verticalAlignment = 'center';
        this.__$uniformHeight = false;
        this._uniformHeight = false;
        this._itemHeight = this._createSelector(null, 44);
        this._itemWidth = this._createSelector(null, 44);
        this._recycledRows = [];
        this.__$spaceLeft = this.__$spaceRight = 0;
        this.__$columnGap = '4px'; // The default value for --ptcs-collection-column-gap
        this.__$rowGap = '8px'; // The default value for --ptcs-collection-row-gap
        this.__$groupGap = '8px'; // The default value for --ptcs-collection-group-gap
        this.__sbWidth = 0;
        this.__width = 0;
        this.__clickCount = 0;
        this.__lastTabKey = 0; // If last pressed key was a Tab, when was it processed?

        this._ownFocusStyling = true;

        // Cell indexes per row: _rows[rowNumber] = [cellIx1, ..., cellIxN]
        // this._rows = [];

        // Default element creators
        this.createCell = () => document.createElement('div');
        this.assignCell = (el, item, index) => {
            el.innerHTML = `ITEM ${index + 1}`;
        };

        this.createHeader = () => document.createElement('div');
        this.assignHeader = (el, key, set) => {
            el.innerHTML = `Group: ${key}`;
        };

        this.createFooter = () => document.createElement('div');
        this.assignFooter = (el, key, set) => {
            el.innerHTML = `End Group: ${key}`;
        };

        this._createCollectionRowRef = null; // this._createCollectionFlexRow.bind(this);
        this._recycleCollectionRowRef = this._recycleCollectionRow.bind(this);
        this._pickReuseItemElementRef = this._pickReuseItemElement.bind(this);
        this._removeCollectionRowRef = this._removeCollectionRow.bind(this);

        // Keep track of selections
        this._selectionMgr = new SelectionMgr((a, b) => a - b); // Compares base indexes
        this._selectionMgr.observe(this);
    }

    _setTabindex(cell, index) {
        if (this.tabindex && this.cellTabKeyScope !== 'item') {
            cell.setAttribute('tabindex', (this._focusedIndex || 0) === index ? '0' : '-1');
        } else {
            cell.removeAttribute('tabindex');
        }
    }

    attributeChangedCallback(name, oldValue, newValue) {
        super.attributeChangedCallback(name, oldValue, newValue);
        if (name === 'tabindex' && !oldValue !== !newValue && this.shadowRoot) {
            this.shadowRoot.querySelectorAll('[rowix] > *').forEach(cell => this._setTabindex(cell, +cell.getAttribute('index')));
        }
    }

    get _chunker() {
        return this.shadowRoot?.getElementById('chunker');
    }

    get layout() {
        return this.__$layout;
    }

    set layout(_layout) {
        if (_layout !== this.__$layout && {flex: true, grid: true, table: true}[_layout]) {
            this.__$layout = _layout;
        }
    }

    _setWidth(propName, value, cssPropName) {
        const v = PTCS.cssDecodeSize(value);
        if (v && v !== this[propName]) {
            this[propName] = v;
            this.style.setProperty(cssPropName, `${v}px`);
            this._reLayoutItems();
        }
    }

    _setHeight(propName, value, cssPropName) {
        const v = PTCS.normalizeUnit(value);
        if (v && v !== this[propName]) {
            this[propName] = v;
            this.style.setProperty(cssPropName, v);
            this._rebuildChunker();
        }
    }

    get rowGap() {
        return this.__$rowGap;
    }

    set rowGap(_rowGap) {
        this._setHeight('__$rowGap', _rowGap, '--ptcs-collection-row-gap');
    }

    get groupGap() {
        return this.__$groupGap;
    }

    set groupGap(_groupGap) {
        this._setHeight('__$groupGap', _groupGap, '--ptcs-collection-group-gap');
    }

    get spaceAbove() {
        return this.__$spaceAbove;
    }

    set spaceAbove(_spaceAbove) {
        this._setHeight('__$spaceAbove', _spaceAbove, '--ptcs-collection-space-above');
    }

    get spaceBelow() {
        return this.__$spaceBelow;
    }

    set spaceBelow(_spaceBelow) {
        this._setHeight('__$spaceBelow', _spaceBelow, '--ptcs-collection-space-below');
    }

    get spaceLeft() {
        return this.__$spaceLeft;
    }

    set spaceLeft(_spaceLeft) {
        this._setWidth('__$spaceLeft', _spaceLeft, '--ptcs-collection-space-left');
    }

    get spaceRight() {
        return this.__$spaceRight;
    }

    set spaceRight(_spaceRight) {
        this._setWidth('__$spaceRight', _spaceRight, '--ptcs-collection-space-right');
    }

    set columnGap(_columnGap) {
        const cg = PTCS.normalizeUnit(_columnGap);
        if (cg && cg !== this.__$columnGap) {
            this.__$columnGap = cg;
            this.style.setProperty('--ptcs-collection-column-gap', cg);

            // The column gap affects the layout
            this._reLayoutItems();
        }
    }

    get rowVerticalAlignment() {
        return this.__$verticalAlignment;
    }

    set rowVerticalAlignment(_verticalAlignment) {
        if (this.__$verticalAlignment !== _verticalAlignment) {
            const old = this.__$verticalAlignment;
            this.__$verticalAlignment = _verticalAlignment;
            if (old === 'stretch' || _verticalAlignment === 'stretch') {
                this._rebuildChunker(); // NOTE: This could be done with CSS only, but this saves as a call to _itemHeight for each cell...
            }
        }
    }

    get uniformHeight() {
        return this.__$uniformHeight;
    }

    set uniformHeight(_uniformHeight) {
        const uh = _uniformHeight || false;
        if (this.__$uniformHeight !== uh) {
            this.__$uniformHeight = uh;
            this._uniformHeight = this._computeUniformHeight();
            this._rebuildChunker();
        }
    }

    get cellTabKeyScope() {
        return this._$cellTabKeyScope || 'cell';
    }

    set cellTabKeyScope(_cellTabKeyScope) {
        if ((this._$cellTabKeyScope || 'cell') === (_cellTabKeyScope || 'cell')) {
            return; // No change
        }
        // Ignore all values except 'cell', 'collection', and 'item'
        if (_cellTabKeyScope === 'collection' || _cellTabKeyScope === 'item') {
            this._$cellTabKeyScope = _cellTabKeyScope;
        } else if (_cellTabKeyScope === 'cell') {
            this._$cellTabKeyScope = undefined;
        } else {
            return; // Invalid value
        }

        this.shadowRoot?.querySelectorAll('[rowix] > :focus-within').forEach(cell => this._setTabindex(cell, +cell.getAttribute('index')));
    }

    _computeUniformHeight() {
        return this.__$uniformHeight && Array.isArray(this.items) &&
            PTCS.normalizeUnit(this.items.reduce((a, item, i) => Math.max(a, this._itemHeight(item, i)), 0));
    }

    _createSelector(selector, defaultValue) {
        switch (typeof selector) {
            case 'function':
                return (item, i) => selector(item, i, this) || defaultValue;

            case 'string':
                return item => item[selector] || defaultValue;
        }
        return () => defaultValue;
    }

    firstUpdated() {
        super.firstUpdated();

        // Keyboard navigation
        this.addEventListener('keydown', this._keyDown.bind(this), true);
        this.shadowRoot.addEventListener('keydown', this._keyDownPost.bind(this));
        this.shadowRoot.addEventListener('keyup', this._keyUp.bind(this));
        this.shadowRoot.addEventListener('focusin', this._focusinEv.bind(this));
        // Need this to track when someone manually tabs into the collection, because 'focusin' is not always fired (only mostly...)
        this.addEventListener('focus', this._focusFixEv.bind(this), true);
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (changedProperties.has('layout')) {
            this._createCollectionRowRef = this.layout === 'table'
                ? this._createCollectionTableRow.bind(this)
                : this._createCollectionFlexRow.bind(this);
        }

        if (changedProperties.has('itemWidth')) {
            this._itemWidth = this._createSelector(this.itemWidth, 44);
        }

        if (changedProperties.has('itemHeight')) {
            this._itemHeight = this._createSelector(this.itemHeight, 44);
        }

        if (['items', 'sort', 'group', 'headers', 'footers'].some(propName => changedProperties.has(propName))) {
            this._viewItemsChanged();
        }

        if (['group', 'headers', 'footers'].some(propName => changedProperties.has(propName))) {
            this._headerFooterChanged();
        }
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        if (changedProperties.has('selectionMode')) {
            this._selectionModeChanged();
        }

        // eslint-disable-next-line max-len
        if (['items', 'layout', '_itemWidth', '_itemHeight', '_createCollectionRowRef', 'createCell', 'assignCell', 'sort', 'group', 'headers', 'footers', 'disableWrapping', 'stretchRowHeight'].some(propName => changedProperties.has(propName))) {
            this._layoutItems(true);
        } else if (changedProperties.has('leftAlignLastRow')) {
            this._rebuildChunker();
        } else if (changedProperties.has('disabled')) {
            this._disabledChanged();
        }

        if (changedProperties.has('_showStickyHeader')) {
            // Unfortunately, this attribute cannot be assigned in the render template because of what appears to be a lit bug
            // Lit apparently gets confused by the scroller element. (Maybe this bug has been fixed in Lit 3?)
            PTCS.setbattr(this.shadowRoot.getElementById('stickyhead'), 'invisible', !this._showStickyHeader);
        }

        if (changedProperties.has('_headerKey')) {
            this._headerKeyChanged();
        }

        if (changedProperties.has('_showStickyFooter')) {
            // Unfortunately, this attribute cannot be assigned in the render template because of what appears to be a lit bug
            // Lit apparently gets confused by the scroller element. (Maybe this bug has been fixed in Lit 3?)
            PTCS.setbattr(this.shadowRoot.getElementById('stickyfoot'), 'invisible', !this._showStickyFooter);
        }

        if (changedProperties.has('_footerKey')) {
            this._footerKeyChanged();
        }

        if (['selectionMode', 'autoSelectFirstItem', 'items'].some(propName => changedProperties.has(propName))) {
            if (this.autoSelectFirstItem && this._selectionMgr.selection === null && this.items && this.items.length > 0) {
                // Auto-select the first visible item (not necessarily the first data item)
                this._selectionMgr.selection = [this._viewItems ? this._viewItems.find(item => typeof item === 'number') : 0];
            }
        }

        if (changedProperties.has('layout')) {
            this._layoutChanged(changedProperties.get('layout'));
        }
    }

    // Find item cells and headers and footers
    findCell(el) {
        while (el) {
            const parent = el.assignedSlot || (el.nodeType === 11 && el.host) || el.parentNode;
            if (parent && parent.nodeType === 1 && parent.hasAttribute('rowix') && el.hasAttribute('index') && this.shadowRoot.contains(parent)) {
                return el;
            }
            el = parent;
        }
        return null;
    }

    _onResizeWidth(ev) {
        if (this.__width === ev.detail.width && this.__sbWidth === ev.detail.sbWidth) {
            return; // Width is unchanged. No need to recompute layout
        }

        this.__width = ev.detail.width;
        this.__sbWidth = ev.detail.sbWidth;
        this.style.setProperty('--ptcs-collection-sbw', `${this.__sbWidth}px`);

        // Ignore the resize in the "table" mode since the rows contain only one cell (unless chunker is still uninitialized)
        if (this.layout !== 'table' || this._chunker.numItems !== ((Array.isArray(this.items) && this.items.length) || 0)) {
            this._reLayoutItems();
        } else {
            this._autoScroll();
        }
    }

    _onGapChanged(ev) {
        const _gap = ev.detail.value;
        const h0 = this.clientHeight;

        if (_gap > 0) {
            this.setAttribute('gap', ''); // Tell theme engine there is a gap
            // Reduce grid height so gap is removed
            this.style.setProperty('--ptcs-collection-height', `${(this.offsetHeight - Math.min(this._chunker.viewportHeight, _gap))}px`);
        } else {
            this.removeAttribute('gap'); // Tell theme engine there is no gap
            if (_gap < 0) {
                // Don't reduce height if scrollbar is visible. Let the grid grow.
                this.style.removeProperty('--ptcs-collection-height');
            }
        }

        if (h0 !== this.clientHeight) {
            // Avoid browser flashing by immediatly processing the new height (make the scroller fit without ever showing an incorrect height)
            this._chunker.resized();
        }
    }

    _selectionModeChanged() {
        this._selectionMgr.selectMethod = this.selectionMode;
    }

    // opt: {wipe}
    _rebuildChunker(opt) {
        if (this.___rebuildChunkerOn) {
            if (opt && opt.wipe) {
                this.___rebuildChunkerOn = 2;
            }
            return;
        }

        this.___rebuildChunkerOn = (opt && opt.wipe) ? 2 : 1;

        requestAnimationFrame(() => {
            if (!this.___rebuildChunkerOn) {
                return;
            }

            const _wipe = this.___rebuildChunkerOn === 2;
            this.___rebuildChunkerOn = 0;

            // Make sure chunker (virtual scroller) is available before rebuilding it
            const chunker = this._chunker;
            if (chunker) {
                chunker.rebuild(_wipe);
            }
        });
    }

    // Force table layout cells to align to the width of the viewport
    _layoutChanged(old) {
        if (this.layout === 'table') {
            const f = () => this.style.setProperty('--ptcs-collection--table-width', `${this._chunker.viewportWidth}px`);
            if (!this._tableResizeObserver) {
                this._tableResizeObserver = new ResizeObserver(f);
            }
            f();
            this._tableResizeObserver.observe(this._chunker.elScroll);
        } else if (old === 'table') {
            this._tableResizeObserver.unobserve(this._chunker.elScroll);
            this.style.removeProperty('--ptcs-collection--table-width');
        }
    }

    // Apply sorting and grouping to items, if applicable
    _viewItemsChanged() {
        const items = Array.isArray(this.items) ? this.items : [];
        switch (typeof this.sort) {
            case 'function':
                this._sort = this.sort;
                break;

            case 'string':
                this._sort = sortBy(this.sort);
                break;

            default:
                this._sort = undefined;
        }

        const sort = this._sort && ((a, b) => this._sort(items[a], items[b]));

        switch (typeof this.group) {
            case 'function':
                this._group = this.group;
                break;

            case 'string': {
                this._group = groupBy(this.group);
                break;
            }

            default:
                this._group = undefined;
        }

        if (this._group) {
            const map = items.reduce((_map, item, index) => {
                const key = this._group(item, index) || 'Ungrouped';
                const set = _map.get(key);
                if (!set) {
                    _map.set(key, [index]);
                } else {
                    set.push(index);
                }
                return _map;
            }, new Map());

            const _groupedItems = [];
            const appendGroup = (set, key) => _groupedItems.push({[groupKey]: key, set}, ...set, {[groupKey]: key, set, footer: true});

            if (sort) {
                const _map = [];

                // Create array of group keys
                map.forEach((_, key) => _map.push(key));

                // Sort group keys and items
                _map.sort().forEach(key => appendGroup(map.get(key).sort(sort), key));
            } else {
                // Append groups in data order
                map.forEach(appendGroup);
            }

            this._viewItems = _groupedItems;
        } else if (sort) {
            this._viewItems = items.map((_, i) => i).sort(sort);
        } else {
            this._viewItems = undefined;
        }

        if (this._focusedIndex === undefined && this._viewItems && this._viewItems.length > 0) {
            this._focusedIndex = this._viewItems.find(i => typeof i === 'number') || undefined;
        }
    }

    // Public interface for updating layout (call when items change size)
    reLayout() {
        this._reLayoutItems();
    }

    reLayoutNow() {
        this._layoutItems(true);
    }

    _reLayoutItems() {
        if (!this.___reLayout) {
            this.__reLayout = true;
            const chunker = this._chunker;
            setTimeout(() => this.__reLayout && this._layoutItems(), (chunker && chunker.numItems) ? 350 : 0); // Speed up if chunker is empty
        }
    }

    // Map cells to rows: (_viewItems || items) => _rows
    _layoutItems(wipe) {
        this.__reLayout = undefined;
        this.__layoutCounter = (this.__layoutCounter || 0) + 1;
        this._uniformHeight = this._computeUniformHeight();

        const chunker = this._chunker;
        if (!chunker) {
            return; // Not available
        }
        const cw = chunker.getBoundingClientRect().width;
        if (!(cw > 0)) {
            if (!chunker.numItems) {
                return; // Nothing to layout
            }
            if (!this.__resizeObserver) {
                // Make sure that items are relayouted when collection becomes visible again
                this.__resizeObserver = new ResizeObserver(entries => {
                    const {width, height} = entries[0].contentRect;
                    if (width > 0 && height > 0) {
                        if (!this.__reLayout && this.__resizeObserver.__layoutCounter === this.__layoutCounter) {
                            this._layoutItems(wipe);
                        }
                        this.__resizeObserver.disconnect();
                        this.__resizeObserver = undefined;
                    }
                });
                this.__resizeObserver.observe(chunker);
            }
            this.__resizeObserver.__layoutCounter = this.__layoutCounter;
            return; // No dimension. Not ready
        }
        // eslint-disable-next-line max-len
        const chunkerWidth = Math.max(cw - this.__sbWidth - this.__$spaceLeft - this.__$spaceRight, 34);
        const numItems = (this._viewItems && this._viewItems.length) || (Array.isArray(this.items) && this.items.length) || 0;
        const indexOf = this._viewItems ? i => this._viewItems[i] : i => i;

        if (wipe) {
            this._recycledRows = []; // Empty recycle bin
        }

        // Build rows of cells
        let rowCells = [];
        rowCells[altIx] = 0;

        const hasCells = () => rowCells.length > 0;

        const addCell = ix => rowCells.push(ix);

        const flushCells = (nextIx = rowCells[altIx] + rowCells.length) => {
            this._rows.push(rowCells);
            rowCells = [];
            rowCells[altIx] = nextIx;
        };

        if (this.layout === 'table') {
            // Table layout
            if (this.group) {
                this._rows = [];

                for (let i = 0; i < numItems; i++) {
                    const ix = indexOf(i);

                    if (typeof ix === 'object') {
                        if (ix.footer ? this.footers : this.headers) {
                            this._rows.push(ix);
                        }
                        rowCells[altIx] = 0; // New group. Reset alternate counting
                    } else {
                        addCell(i);
                        flushCells();
                    }
                }

                chunker.numItems = this._rows.length;

            } else {
                // No grouping. One cell in each row - layed out exactly the same as _viewItems || items. Don't need _rows.
                this._rows = null;
                chunker.numItems = numItems;
            }
        } else {
            this._rows = [];

            const _gap = PTCS.cssDecodeSize(this.__$columnGap);
            const gap = isNaN(_gap) ? 0 : _gap;

            if (this.layout === 'grid') {
                // Grid layout: put the cells in a grid where all columns have the same width
                const maxWidth = (this.items || []).reduce((a, item, i) => Math.max(a, this._itemWidth(item, i)), 0);
                const numCol = Math.max(1, Math.floor(1 + (chunkerWidth - maxWidth) / (maxWidth + gap)));

                this.style.setProperty('--ptcs-collection-num-col', numCol); // For the CSS grid

                for (let i = 0; i < numItems; i++) {
                    const ix = indexOf(i);

                    if (typeof ix === 'object') {
                        // header or footer
                        if (hasCells()) {
                            rowCells.endGroup = true;
                            flushCells(0);
                        }
                        rowCells.startGroup = true;
                        if (ix.footer ? this.footers : this.headers) {
                            this._rows.push(ix);
                        }
                    } else {
                        // Is row full?
                        if (rowCells.length === numCol) {
                            flushCells();
                        }

                        addCell(i);
                    }
                }

                if (hasCells()) {
                    flushCells();
                }
            } else {
                // Flex layout: place as many cells as possible on each row
                const _chunkerWidth = this.disableWrapping ? Number.MAX_SAFE_INTEGER : chunkerWidth;
                let rowWidth = 0;
                let maxRowWidth = 0;

                for (let i = 0; i < numItems; i++) {
                    const ix = indexOf(i);

                    if (typeof ix === 'object') {
                        // header or footer
                        if (hasCells()) {
                            rowCells.endGroup = true;
                            flushCells(0);
                            maxRowWidth = Math.max(rowWidth, maxRowWidth);
                            rowWidth = 0;
                        }
                        rowCells.startGroup = true;
                        if (ix.footer ? this.footers : this.headers) {
                            this._rows.push(ix);
                        }
                    } else {
                        const width = this._itemWidth(this.items[ix], ix);

                        // Is row full?
                        if (hasCells() && rowWidth + width >= _chunkerWidth) {
                            flushCells();
                            maxRowWidth = Math.max(rowWidth, maxRowWidth);
                            rowWidth = 0;
                        }

                        addCell(i);
                        rowWidth += width + gap;
                    }
                }

                if (hasCells()) {
                    flushCells();
                    maxRowWidth = Math.max(rowWidth, maxRowWidth);
                }
            }

            chunker.numItems = this._rows.length;
        }

        // Is the view a single row that should be stretched vertically to fit the viewport?
        PTCS.setbattr(this, 'stretch-single-row-height', this.disableWrapping && this.stretchRowHeight && chunker.numItems === 1);

        this._rebuildChunker({wipe});

        this._autoScroll();
    }

    _removeCollectionRow(el) {
        this.dispatchEvent(new CustomEvent('unlink-row', {detail: {el, type: el[groupType] || 'row'}}));
    }

    _recycleCollectionRow(el) {
        stripGhosts(el);
        const type = el[groupType] || el.children.length;
        if (this._recycledRows[type]) {
            this._recycledRows[type].push(el);
        } else {
            this._recycledRows[type] = [el];
        }
        this.dispatchEvent(new CustomEvent('unlink-row', {detail: {el, type: el[groupType] || 'row', recycled: true}}));

        console.assert(this.loadAll || this._recycledRows[type].length < 50, 'there are many elements in the recycle bin');
    }

    // type: 'header' | 'footer' | number of contained cells -> row element of correct type
    __createCollectionRow(type) {
        let el = this._recycledRows[type] && this._recycledRows[type].pop(); // Is recycled row available?
        if (!el) {
            // Create new row element
            el = document.createElement('div');

            // Configure element
            switch (type) {
                case 'header':
                case 'footer':
                    el.setAttribute('part', type);
                    el[groupType] = type;
                    el.appendChild(type === 'header' ? this.createHeader() : this.createFooter());
                    break;

                default:
                    el.setAttribute('part', 'row');
                    for (let i = 0; i < type; i++) {
                        const cell = this.createCell();
                        cell[isCell] = true;
                        el.appendChild(cell);
                    }
            }
        }
        return el;
    }

    _cellsOf(i) {
        if (this._rows) {
            return this._rows[i];
        }
        const x = this._viewItems ? this._viewItems[i] : i;
        return typeof x === 'number' ? [x] : x;
    }

    _pickReuseItemElement(listOfItemElements, i) {
        if (!this.items || listOfItemElements.length === 0) {
            return null; // Fake call
        }
        const cells = this._cellsOf(i);
        console.assert(cells);
        const type = cells[groupKey] ? (cells.footer && 'footer') || 'header' : cells.length;
        const fi = listOfItemElements.findIndex(el => (el[groupType] || el.children.length) === type);
        return fi >= 0 ? listOfItemElements.splice(fi, 1)[0] : null;
    }

    _assignHeader(header, rowix, key, set) {
        this.assignHeader(header, key, set);
        header.setAttribute('index', -rowix - 1);
        this._setTabindex(header, -rowix - 1);
    }

    _assignFooter(footer, index, key, set) {
        this.assignFooter(footer, key, set);
        footer.setAttribute('index', -index - 1);
        this._setTabindex(footer, -index - 1);
    }

    // Multiple cells in each row
    _createCollectionFlexRow(i, el) {
        stripGhosts(el);
        const cells = this._rows[i];
        const type1 = cells[groupKey] ? (cells.footer && 'footer') || 'header' : cells.length;
        const type2 = el && (el[groupType] || el.children.length);

        if (type1 !== type2) {
            // Reuse el or reclaim / create element of proper type
            el = this.__createCollectionRow(type1);
        }

        const top = (i <= 0);
        const bottom = (i + 1 >= this._rows.length);

        switch (type1) {
            case 'header':
                this._assignHeader(el.firstChild, i, cells[groupKey], cells.set);
                break;

            case 'footer':
                this._assignFooter(el.firstChild, i, cells[groupKey], cells.set);
                break;

            default: {
                const start = cells.startGroup && !this.headers;
                const end = cells.endGroup && !this.footers;

                // Stretch height of (single) row so it fits viewport?
                const srh = this.disableWrapping && this.stretchRowHeight && this._rows.length === 1;

                // Stretch cells in current row vertically?
                const stretch = srh || this.rowVerticalAlignment === 'stretch';

                // Reduce function for maximum height
                const maxHeight = (w, i0) => {
                    const ix = this._viewItems ? this._viewItems[i0] : i0;
                    return Math.max(w, this._itemHeight(this.items[ix], ix));
                };

                const height = !srh && (this._uniformHeight || (stretch && PTCS.normalizeUnit(cells.reduce(maxHeight, 0))));

                if (height) {
                    // eslint-disable-next-line max-len
                    const t = top ? ' + var(--ptcs-collection-space-above, 0px)' : '';
                    const b = bottom ? ' + var(--ptcs-collection-space-below, 0px)' : '';
                    // eslint-disable-next-line max-len
                    const g = top ? '' : ((start && ' + var(--ptcs-collection-group-gap, var(--ptcs-collection-row-gap, 8px))') || ' + var(--ptcs-collection-row-gap, 8px)');
                    el.style.height = (t || b || g) ? `calc(${height}${t}${g}${b})` : height;
                } else {
                    el.style.height = srh ? '100%' : '';
                }

                let rowWidth = 0;
                let width = 0; // Width of last cell

                cells.forEach((i0, index) => {
                    const ix = this._viewItems ? this._viewItems[i0] : i0;
                    const selected = this._selectionMgr.isSelected(ix);
                    const cellEl = el.children[index];
                    width = this._itemWidth(this.items[ix], ix);
                    rowWidth += width;
                    cellEl.style.width = PTCS.normalizeUnit(width);
                    cellEl.style.height = stretch ? '' : PTCS.normalizeUnit(this._itemHeight(this.items[ix], ix));
                    PTCS.setbattr(cellEl, 'alt', (cells[altIx] + index) % 2 === 1);
                    this.assignCell(cellEl, this.items[ix], ix, selected, this);
                    cellEl.setAttribute('index', ix);
                    PTCS.setbattr(cellEl, 'selected', selected);
                    this._setTabindex(cellEl, ix);
                });

                PTCS.setbattr(el, 'start', start);
                PTCS.setbattr(el, 'end', end);

                const lastRow = cells.endGroup || i + 1 >= this._rows.length;
                PTCS.setbattr(el, 'last-line', lastRow);

                // Need to pad with ghost cells?
                if (lastRow && this.leftAlignLastRow && width > 0 && this.layout === 'flex') {
                    const _gap = PTCS.cssDecodeSize(this.__$columnGap);
                    const gap = (isNaN(_gap) ? 0 : _gap);
                    const egap = (cells.length - 1) * gap; // Gaps between existing cells
                    const wg = width + gap; // Width needed for a ghost cell
                    const numGhost = Math.floor((this._chunker.viewportWidth - this.spaceLeft - this.spaceRight - egap - rowWidth) / wg);
                    if (numGhost > 0) {
                        // eslint-disable-next-line no-shadow
                        const {width, height} = el.lastElementChild.style;
                        const attrs = {ghost: '', style: `width:${width};height:${height};`};
                        for (let j = 0; j < numGhost; j++) {
                            el.appendChild(PTCS.createElement('div', attrs));
                        }
                        el[hasGhost] = true;
                    }
                }
            }
        }

        PTCS.setbattr(el, 'top', top);
        PTCS.setbattr(el, 'bottom', bottom);
        el.setAttribute('rowix', i);

        return el;
    }

    // Each cell is a row
    _createCollectionTableRow(i, el) {
        let x;
        if (this._rows) {
            x = this._rows[i];
            if (Array.isArray(x) && x.length === 1) {
                x = this._viewItems ? this._viewItems[x[0]] : x[0];
            }
        } else {
            x = this._viewItems ? this._viewItems[i] : i;
        }
        const type1 = typeof x === 'number' ? 1 : (x.footer && 'footer') || 'header';
        const type2 = el && (el[groupType] || el.children.length);

        if (type1 !== type2) {
            // Reuse el or reclaim / create element of proper type
            el = this.__createCollectionRow(type1);
        }
        el.style.width = ''; // 100%

        const cellEl = el.firstChild;
        switch (type1) {
            case 'header':
                cellEl.style.height = '';
                this._assignHeader(cellEl, i, x[groupKey], x.set);
                break;

            case 'footer':
                cellEl.style.height = '';
                this._assignFooter(cellEl, i, x[groupKey], x.set);
                break;

            default: {
                const selected = this._selectionMgr.isSelected(x);
                cellEl.style.height = this._uniformHeight || PTCS.normalizeUnit(this._itemHeight(this.items[x], x));
                PTCS.setbattr(cellEl, 'alt', (this._rows ? this._rows[i][altIx] : i) % 2 === 1);
                this.assignCell(cellEl, this.items[x], x, selected, this);
                cellEl.setAttribute('index', x);
                PTCS.setbattr(cellEl, 'selected', selected);
                this._setTabindex(cellEl, x);
            }
        }

        PTCS.setbattr(el, 'top', i <= 0);
        // eslint-disable-next-line no-nested-ternary
        PTCS.setbattr(el, 'bottom', i + 1 >= (this._rows ? this._rows.length : (this._viewItems ? this._viewItems.length : this.items.length)));
        el.setAttribute('rowix', i);

        return el;
    }

    _headerFooterChanged() {
        PTCS.setbattr(this, 'pin-headers', this._group && this.headers === 'pinned');
        PTCS.setbattr(this, 'pin-footers', this._group && this.footers === 'pinned');
        this._onScroll(); // Update content
    }

    _headerKeyChanged() {
        const header = this.shadowRoot.getElementById('stickyhead');
        if (!header.firstChild) {
            header.appendChild(this.createHeader());
        }
        this.assignHeader(header.firstChild, this._headerKey);
    }

    _footerKeyChanged() {
        const footer = this.shadowRoot.getElementById('stickyfoot');
        if (!footer.firstChild) {
            footer.appendChild(this.createFooter());
        }
        this.assignFooter(footer.firstChild, this._footerKey);
    }

    _disabledChanged() {
        const chunker = this._chunker;
        if (!chunker) {
            return;
        }
        chunker.itemElements.forEach(row => {
            for (const cellEl of row.children) {
                if (cellEl[isCell]) {
                    const index = Number(cellEl.getAttribute('index'));
                    const selected = this._selectionMgr.isSelected(index);
                    this.assignCell(cellEl, this.items[index], index, selected, this);
                }
            }
        });
    }


    // View has scrolled - find new group keys for pinned header and footer
    _onScroll() {
        const chunker = this._chunker;
        if (!chunker) {
            return; // pre-mature call
        }

        const items = this._rows || this._viewItems;

        if (!this._group || (this.headers !== 'pinned' && this.footers !== 'pinned') || !items || items.length === 0) {
            return; // Don't waste any time
        }

        if (this.headers === 'pinned') {
            for (let i = chunker.firstVisibleIx; i >= 0; i--) {
                const cells = items[i];
                if (cells[groupKey] && !cells.footer) {
                    const headerEl = this._chunker.querySelector(`[part=header][rowix="${i}"]`);
                    // Only show sticky header when the inline header is not fully visible
                    // eslint-disable-next-line max-len
                    this._showStickyHeader = !(headerEl && headerEl.firstChild && headerEl.firstChild.getBoundingClientRect().top >= this._chunker.getBoundingClientRect().top);
                    this._headerKey = cells[groupKey];
                    break;
                }
            }
        }

        if (this.footers === 'pinned') {
            for (let i = chunker.lastVisibleIx; i < items.length; i++) {
                const cells = items[i];
                if (cells[groupKey] && cells.footer) {
                    const footerEl = this._chunker.querySelector(`[part=footer][rowix="${i}"]`);
                    // Only show sticky footer when the inline footer is not fully visible
                    // eslint-disable-next-line max-len
                    this._showStickyFooter = !(footerEl && footerEl.firstChild && footerEl.firstChild.getBoundingClientRect().bottom <= this._chunker.getBoundingClientRect().bottom);
                    this._footerKey = cells[groupKey];
                    break;
                }
            }
        }
    }

    // Message from SelectionMgr
    selectionChanged(selection) {
        this._autoScroll();
        this.dispatchEvent(new CustomEvent('selection', {detail: {selection}}));
    }

    // Message from SelectionMgr
    selectedChanged(index, selected) {
        const cell = this._chunker.querySelector(`[part=row] > [index="${index}"]`);
        if (cell) {
            PTCS.setbattr(cell, 'selected', selected);
        }
        this.dispatchEvent(new CustomEvent('cell-select-mode', {detail: {index, selected}})); // notify
    }

    selectAll() {
        if (this.selectionMode !== 'multiple') {
            return;
        }

        this._selectionMgr.selection = this.items.reduce((a, _, i) => {
            a.push(i);
            return a;
        }, []);
    }

    unselectAll() {
        this._selectionMgr.selection = null;
    }

    select(index, selected) {
        this._selectionMgr.select(index, selected);
    }

    isSelected(index) {
        return this._selectionMgr.isSelected(index);
    }

    get selection() {
        return this._selectionMgr.selection;
    }

    set selection(_selection) {
        this._selectionMgr.selection = _selection;
    }

    _autoScroll() {
        if (!this.autoScroll || !(this.items && this.items.length > 0) || this._selectionMgr.selection === null || this.__waitingForAutoScroll) {
            return;
        }

        this.__waitingForAutoScroll = true;
        requestAnimationFrame(() => {
            this.__waitingForAutoScroll = undefined;

            const chunker = this._chunker;
            if (!chunker) {
                return;
            }
            const selectionMgr = this._selectionMgr;
            const indexOf = this._viewItems ? i => this._viewItems[i] : i => i;

            // Returns index of first selected cell in row, or -1
            const ixSelected = this._rows ? rowNo => {
                const cells = this._rows[rowNo];
                return cells[groupKey] ? -1 : cells.findIndex(i => selectionMgr.isSelected(indexOf(i)));
            } : rowNo => {
                const x = indexOf(rowNo);
                return typeof x === 'number' && selectionMgr.isSelected(x) ? 0 : -1;
            };

            // Scroll to row and set focus on the first selected cell in the row
            const scrollToSelectedRow = rowNo => {
                const ix = ixSelected(rowNo);
                if (ix === -1) {
                    return false; // No selected cells in this row
                }

                chunker.scrollTo(rowNo);

                // Move focus to the selected row (first cell), to prevent unpleasant scroll jumps
                const row = chunker.getRow(rowNo);
                const cell = row && row.children[ix];
                this._focusedOnCell(cell); // Make cell focusable
                if (PTCS.hasFocus(this)) {
                    if (this.cellTabKeyScope === 'item') {
                        focusOn(getFocusableExpandSlots(cell)[0]);
                    } else {
                        focusOn(cell);
                    }
                }
                return true;
            };

            let startIx = chunker.startIx + 1; // Ignore first line, since it can be partly hidden
            let endIx = chunker.endIx - 1; // Ignore last line, since it can be partly hidden

            for (let rowNo = startIx; rowNo < endIx; rowNo++) {
                if (scrollToSelectedRow(rowNo)) {
                    return; // A selected item is already loaded
                }
            }

            // eslint-disable-next-line max-len
            const numRows = (this._rows && this._rows.length) || (this._viewItems && this._viewItems.length) || (Array.isArray(this.items) && this.items.length) || 0;
            while (startIx > 0 || endIx < numRows) {
                if ((startIx > 0 && scrollToSelectedRow(--startIx)) || (endIx < numRows && scrollToSelectedRow(endIx++))) {
                    return;
                }
            }
        });
    }

    // items[index] needs to be refreshed, if it is displayed
    refreshCell(index) {
        const chunker = this._chunker;
        if (!chunker) {
            return;
        }
        const indexOf = this._viewItems ? i => this._viewItems[i] : i => i;
        const startIx = chunker.startIx;
        const endIx = chunker.endIx;
        for (let i = startIx; i < endIx; i++) {
            const cells = this._cellsOf(i);
            if (!cells[groupKey] && cells.some(x => indexOf(x) === index)) {
                chunker.refresh(i);
                break;
            }
        }
    }

    refresh() {
        this._rebuildChunker({wipe: true});
    }

    // User clicked on view - notify the client
    _findCellIndex(el) {
        const cell = this.findCell(el);
        return cell ? +cell.getAttribute('index') : undefined;
    }

    _dispatchCellEvent(el, eventName) {
        const index = this._findCellIndex(el);

        if (index >= 0) {
            this.dispatchEvent(new CustomEvent(eventName, {
                detail: {
                    index,
                    clickedOn: el
                }
            }));
        }
    }

    _onContextMenu(ev) {
        if (!this.preventCellContextMenu) {
            return;
        }

        const index = this._findCellIndex(ev.target);

        if (index >= 0) {
            ev.preventDefault();
        }
    }

    _onMouseDown(ev) {
        this.__lastTabKey = __lastTabKey = 0; // mousedown resets the Tab counter

        // mousedown and touchstart may come as a pair. Only act on one of them
        const [type, when] = this.__lastDown || [null, 0];
        this.__lastDown = [ev.type, Date.now()];

        if (ev.defaultPrevented || clickOnScrollbar(ev) || (type !== ev.type && Date.now() - when < 25)) {
            return;
        }

        if (_leftMouseButtonOrTouch(ev)) {
            clearTimeout(this.__longClickTimeout);
            this.__longClick = false;

            const target = ev.target;

            this.__longClickTimeout = setTimeout(() => {
                this.__longClick = true;
                this._dispatchCellEvent(target, 'cell-long-clicked');
            }, LONG_CLICK_DELAY);
        }

        // Set focus on clicked element (deepest focusable element or cell)
        // NOTE: I expected that the browser (Chrome) should handle this automatically, but it fails on slotted content
        const cell = this.findCell(ev.target);
        if (cell) {
            let e = ev.composedPath()[0];

            // Stop at first ancestor Element (nodeType 1) that is focusable (tabIndex > 0 || tabindex='-1'))
            while (e && e !== cell && !(e.nodeType === 1 && (e.tabIndex >= 0 || e.getAttribute('tabindex') === '-1'))) {
                e = e.assignedSlot || (e.nodeType === 11 && e.host) || e.parentNode;
            }
            if (this.cellTabKeyScope === 'item') {
                focusOn(e); // item doesn't focus on the cards
            } else {
                focusOn(e || cell);
            }
        }
    }

    _onMouseUp(ev) {
        // mouseup and touchend may come as a pair. Only act on one of them
        const [type, when] = this.__lastUp || [null, 0];
        this.__lastUp = [ev.type, Date.now()];

        if (ev.defaultPrevented || clickOnScrollbar(ev) || (type !== ev.type && Date.now() - when < 25)) {
            return;
        }

        console.assert(this.__clickCount === 0 || this.__clickCount === 1);

        this.__clickCount++;

        clearTimeout(this.__longClickTimeout);

        if (!this.__longClick && _leftMouseButtonOrTouch(ev)) {
            if (this.__clickCount === 2) {
                clearTimeout(this.__dblClickTimeout);
                this.__clickCount = 0;
                this._dispatchCellEvent(ev.target, 'cell-double-clicked');
            } else if (!this.__longClick) {
                const target = ev.target;

                if (!this._disableCellDoubleClicked) {
                    this.__dblClickTimeout = setTimeout(() => {
                        this.__clickCount = 0;
                        this._dispatchCellEvent(target, 'cell-clicked');
                    }, DOUBLE_CLICK_DELAY);
                } else {
                    this.__clickCount = 0;
                    this._dispatchCellEvent(target, 'cell-clicked');
                }
            }
        } else {
            this.__clickCount = 0;

            if (ev.button === 2) {
                this._dispatchCellEvent(ev.target, 'cell-right-clicked');
            }
        }

        this.__longClick = false;
    }

    _findFirstVisibleRowCell(childNo) {
        // TODO: Move "find first visible row" functionality into scroller?
        const {top} = this._chunker.getBoundingClientRect();
        const row = [...this._chunker.querySelectorAll('[rowix]')]
            .sort(compareRows)
            .find(_row => {
                const bb = _row.getBoundingClientRect();
                return bb.bottom - top >= bb.height * 0.5;
            });

        return row && row.querySelector(`:scope > :is(:nth-child(${childNo + 1}), :last-child)`);
    }

    _findLastVisibleRowCell(childNo) {
        // TODO: Move "find last visible row" functionality into scroller?
        const {bottom} = this._chunker.getBoundingClientRect();
        const row = [...this._chunker.querySelectorAll('[rowix]')]
            .sort((a, b) => compareRows(b, a))
            .find(_row => {
                const bb = _row.getBoundingClientRect();
                return bottom - bb.top >= bb.height * 0.5;
            });

        return row && row.querySelector(`:scope > :is(:nth-child(${childNo + 1}), :last-child)`);
    }

    _firstVisibleCell() {
        const select = (e1, e2) => {
            // Prefer row over header or footer
            const v1 = e1.getAttribute('part') === 'row' ? 0 : 1;
            const v2 = e2.getAttribute('part') === 'row' ? 0 : 1;
            if (v1 !== v2) {
                return v1 < v2 ? e1 : e2;
            }
            // Prefer earlier row before later row
            return compareRows(e1, e2) < 0 ? e1 : e2;
        };

        const row = [...this._chunker.querySelectorAll('[rowix]')].reduce((best, el) => best ? select(best, el) : el, null);

        return row && row.firstElementChild;
    }

    _focusedOnCell(cell) {
        if (!cell) {
            return;
        }

        // Get focus index
        this._focusedIndex = +cell.getAttribute('index');

        if (this.cellTabKeyScope === 'item') {
            return;
        }

        // Unset tabindex for non-focused cells
        [...this._chunker.querySelectorAll('[rowix] > [tabindex="0"]')].forEach(c => c !== cell && c.setAttribute('tabindex', '-1'));

        // Set tabindex for current cell
        cell.setAttribute('tabindex', '0');
    }

    _notifyFocus(defaultPrevented) {
        if (defaultPrevented || this.shadowRoot.activeElement || !this._chunker) {
            return;
        }

        if (this.cellTabKeyScope === 'item') {
            // Move focus from collection to first item
            if (this.__backwardsFocusing) {
                return;
            }
            const cells = [...this.shadowRoot.querySelectorAll('[rowix]')].sort(compareRows).reduce(collectCells, []);
            for (let i = 0; i < cells.length; i++) {
                const f = getFocusableExpandSlots(cells[i]);
                if (f.length) {
                    this._focusedOnCell(cells[i]);
                    focusOn(f[0]);
                    break;
                }
            }
            return;
        }

        // Delegate focus to a child
        const cell = this._chunker.querySelector(`[rowix] > [index="${this._focusedIndex}"]`) || this._firstVisibleCell();
        if (cell) {
            // Configure the delegated cell for focusing ...
            this._focusedOnCell(cell);

            // ... then focus on it
            cell.focus();
        }
    }

    // Hack to track if an external agent (the browser, javascript, etc) sets focus on element in the collection in an unacceptable way.
    // If the content is slotted _focusinEv isn't always fired. (Maybe a bug, maybe a feature). This fix makes sure _focusinEv is called.
    _focusFixEv() {
        this.__focusFix = true;
        setTimeout(() => {
            if (!this.__focusFix) {
                return; // Already fired
            }
            const ae = this.getRootNode().activeElement;
            const target = this.shadowRoot.activeElement || (ae && this.contains(ae) && ae);
            if (target) {
                this._focusinEv({target}); // An unprocessed focus change (probably)
            }
        }, 1);
    }

    // This should be invoken whenever a new element get focus in any cell
    _focusinEv(ev) {
        this.__focusFix = undefined;
        this.___oldae = undefined; // Focus has changed so the old active element must be reset
        const chunker = this._chunker;
        if (!chunker) {
            return;
        }

        const now = Date.now();
        const {target} = ev;
        const cell = this.findCell(target); // Cell that contains focused element

        if (this.cellTabKeyScope === 'item') {
            // Make sure the cell that contain the focused item has the official focus
            if (cell) {
                this._focusedOnCell(cell);
            } else {
                // focus-in event on collection itself. Most likely, someone is trying to move focus backwards, so focus should
                // be delegated to the first focusable element before the collection - not the first item in the collection
                delegateToPrev(this);
            }

            return;
        }

        // Did we get this focus event because someone Tabbed into the collection?
        if (now - __lastTabKey < 250 && now - this.__lastTabKey > 250) {
            // When someone tabs into the collection the focus should be delegated to the lastly focused cell
            if (!cell || target !== cell || this._focusedIndex !== +cell.getAttribute('index')) {
                // Adjust focus to the last focused cell, if available, or the first visible cell
                if (focusOn(chunker.querySelector('[rowix] > [tabindex="0"]') || this._firstVisibleCell())) {
                    return;
                }
            }
        }

        const focusIndex = cell ? +cell.getAttribute('index') : (this._focusedIndex || 0);
        if (focusIndex !== this._focusedIndex) {
            // HACK: did a sub-element process a Tab key in a way that it moved focus out of its cell? If so, restore focus to old cell.
            const legitFocus = (now - this.__lastTabKey < 50) && chunker.querySelector('[rowix] > [tabindex="0"]');

            if (focusOn(legitFocus)) {
                // Restored focus to the cell that was robbed from focus (presumbaly by one of its sub-elements)
                return;
            }

            // Officially move focus to cell
            this._focusedOnCell(cell);
        }

        // Make sure focused element is fully visible
        this._scrollFocusIntoView();
    }

    // Give the elements some time to stabilize, then do two attempts to scroll the focused element into view.
    _scrollFocusIntoView(once) {
        const doFocus = force => {
            for (let ae = document.activeElement; ae;) {
                const next = ae.shadowRoot && ae.shadowRoot.activeElement;
                if (!next && this.findCell(ae)) {
                    if (force || this.___oldae !== ae) {
                        this._scrollElementIntoView(this.___oldae = ae);
                    }
                    break;
                }
                ae = next;
            }
        };

        if (!this.___willScrollFocus) {
            this.___willScrollFocus = once ? 1 : 2;
            requestAnimationFrame(() => {
                doFocus();
                requestAnimationFrame(() => {
                    const mode = this.___willScrollFocus;
                    this.___willScrollFocus = undefined;
                    doFocus(mode === 2);
                });
            });
        } else if (!once) {
            this.___willScrollFocus = 2;
        }
    }

    _scrollElementIntoView(el) {
        const chunker = this._chunker;
        if (!chunker) {
            return;
        }

        // Make sure element is fully visible
        const _bb0 = chunker.elScroll.getBoundingClientRect();
        const bb0 = {left: _bb0.left, top: _bb0.top, right: _bb0.right, bottom: _bb0.bottom};
        const bb = el.getBoundingClientRect();
        const stickyhead = this._group && this.headers === 'pinned' && this.shadowRoot.getElementById('stickyhead');
        const stickyfoot = this._group && this.headers === 'pinned' && this.shadowRoot.getElementById('stickyfoot');

        if (stickyhead && stickyhead.clientHeight) {
            bb0.top = Math.max(bb0.top, stickyhead.getBoundingClientRect().bottom); // A sticky header covers the viewport
        }
        if (stickyfoot && stickyfoot.clientHeight) {
            bb0.bottom = Math.min(bb0.bottom, stickyfoot.getBoundingClientRect().top); // A sticky footer covers the viewport
        }

        // horizontal scroll possible?
        if (bb.left > bb0.left || bb.right < bb0.right) {
            // There is some space to either the left or right side of the focused item (so it can make sense to scroll it)
            if (bb.left < bb0.left) {
                chunker.elScroll.scrollLeft -= bb0.left - bb.left;
            } else if (bb.right > bb0.right) {
                chunker.elScroll.scrollLeft += bb.right - bb0.right;
            }
        }

        // vertical scroll needed?
        if (bb.top < bb0.top || bb.bottom > bb0.bottom) {
            if (bb.top < bb0.top) {
                chunker.scrollDY(bb0.top - bb.top + 4);
            } else if (bb.bottom > bb0.bottom) {
                chunker.scrollDY(bb0.bottom - bb.bottom - 4);
            }
        }

        el.scrollIntoViewIfNeeded();
    }

    _moveFocusToCell(rowIx, cellIx) {
        if (isNaN(rowIx) || rowIx < 0 || rowIx >= this._chunker.numItems || this.cellTabKeyScope === 'item') {
            return false;
        }
        this._chunker.scrollTo(rowIx);

        let child;

        switch (cellIx) {
            case 0: child = ':first-child'; break;
            case -1: child = ':last-child'; break;
            default: child = `:is(:nth-child(${cellIx + 1}), :last-child)`;
        }

        return focusOn(this._chunker.querySelector(`[rowix="${rowIx}"] > ${child}`));
    }

    _scrollCell(cell, _dx, _dy) {
        const dx = Math.round(Math.abs(_dx)) * Math.sign(_dx);
        const dy = Math.round(Math.abs(_dy)) * Math.sign(_dy);
        // Find scrollable element
        const find = (el, cv, sv, ov, v) => {
            const f = p => {
                for (const e of (p instanceof HTMLSlotElement ? p.assignedElements() : p.children)) {
                    const r = (e[cv] < e[sv] && e[ov] === v) ? e : f(e);
                    if (r) {
                        return r;
                    }
                }
                return null;
            };

            return el[cv] < el[sv] ? el : f(el);
        };

        // Vertically scrollable element
        const elv = dx && find(cell, 'clientWidth', 'scrollWidth', 'offsetWidth', cell.clientWidth);
        if (elv) {
            elv.scrollLeft += dx;
        }

        // Horizontally scrollable element
        const elh = dy && find(cell, 'clientHeight', 'scrollHeight', 'offsetHeight', cell.clientHeight);
        if (elh) {
            elh.scrollTop += dy;
        }

        return elv || elh;
    }

    // Take a look at the keydown before the descendants sees it
    _keyDown(ev) {
        this.__lastTabKey = ev.key === 'Tab' ? Date.now() : 0; // Last time a Tab event occured inside this component
        if (this.disabled || ev.defaultPrevented || !(this.tabIndex >= 0)) {
            return;
        }

        const ae = this.shadowRoot.activeElement;

        // Is focus on a mashup element?
        if (!ae || (ae.shadowRoot && ae.shadowRoot.activeElement)) {
            // Focus is somewhere inside the mashup
            return;
        }
        const rowEl = ae.parentNode;
        if (!rowEl || !rowEl.hasAttribute('rowix')) {
            return;
        }
        const rowIx = +rowEl.getAttribute('rowix');

        switch (ev.key) {
            case 'Tab': {
                // Tab key when focus is on cell. Move focus outside of collection
                const _top = el => {
                    for (let e = el.parentNode; e; e = e.parentNode) {
                        const r = e.assignedSlot || (e.nodeType === 9 && e) || (e.nodeType === 11 && e.host) || (e[isCell] && e);
                        if (r) {
                            return r;
                        }
                    }
                    return null;
                };

                let cur = this;
                let focused = false;
                for (let top = _top(cur); top; top = _top(top)) {
                    if (top.tagName === 'PTCS-COLLECTION') {
                        return; // Inside another collection. Let it handle the Tab
                    }
                    const focusable = getFocusableExpandSlots(top);
                    const index = focusable.findIndex(e => e === cur);
                    if (ev.shiftKey ? index > 0 : (index + 1 < focusable.length && index >= 0)) {
                        if (focusOn(ev.shiftKey ? getLastFocusable(focusable[index - 1]) : focusable[index + 1])) {
                            focused = true;
                            break;
                        }
                    }
                    cur = top;
                }

                if (!focused) {
                    // failed tabbing
                    console.warn('failed to move focus outside of collection ...');
                    this.blur();
                }
                break;
            }

            case 'ArrowLeft':
                if (!(ev.ctrlKey && this._scrollCell(ae, -ae.clientWidth / 2, 0)) && !focusOn(ae.previousSibling)) {
                    return; // No action
                }
                break;

            case 'ArrowRight':
                if (!(ev.ctrlKey && this._scrollCell(ae, ae.clientWidth / 2, 0)) && !focusOn(ae.nextSibling)) {
                    return; // No action
                }
                break;

            case 'ArrowUp':
                if (!(ev.ctrlKey && this._scrollCell(ae, 0, -ae.clientHeight / 2)) && !this._moveFocusToCell(rowIx - 1, PTCS.getChildIndex(ae))) {
                    return;
                }
                break;

            case 'ArrowDown':
                if (!(ev.ctrlKey && this._scrollCell(ae, 0, ae.clientHeight / 2)) && !this._moveFocusToCell(rowIx + 1, PTCS.getChildIndex(ae))) {
                    return;
                }
                break;

            case 'Home':
                if (ev.ctrlKey) {
                    if (!this._moveFocusToCell(0, 0)) {
                        return; // Already on the first row
                    }
                } else if (!ae.previousElementSibling || !focusOn(rowEl.firstElementChild)) {
                    return; // No action
                }
                break;

            case 'End':
                if (ev.ctrlKey) {
                    if (!this._moveFocusToCell(this._chunker.numItems - 1, -1)) {
                        return; // Already on the last cell of the last row
                    }
                } else if (!ae.nextElementSibling || !focusOn(rowEl.lastElementChild)) {
                    return; // No action
                }
                break;

            case 'PageUp':
                if (rowIx === 0 || !focusOn(this._findFirstVisibleRowCell(PTCS.getChildIndex(ae)))) {
                    return;
                }
                this._chunker.scrollRowToBottom(+this.shadowRoot.activeElement.parentNode.getAttribute('rowix'));
                break;

            case 'PageDown':
                if (rowIx + 1 >= this._chunker.numItems || !focusOn(this._findLastVisibleRowCell(PTCS.getChildIndex(ae)))) {
                    return;
                }
                this._chunker.scrollRowToTop(+this.shadowRoot.activeElement.parentNode.getAttribute('rowix'));
                break;

            case 'Enter': {
                const focusable = getFocusableExpandSlots(ae);
                if (!focusOn(focusable && focusable[0])) {
                    return; // Noting focusable in cell
                }
                break;
            }

            case ' ': // Space
                if (!ev.ctrlKey || this._selectionMgr.selectMethod === 'none') {
                    return;
                }
                this.select(+ae.getAttribute('index'));
                break;

            default:
                return;
        }

        ev.preventDefault();
    }

    // Got a Tab event on a cell item (ev.target)
    _processCellItemTab(cell, ev) {
        // Follow the focused cell item across slots and shadow doms
        const composedPath = ev.composedPath();
        let item = composedPath[0];

        // Tabbing forwards into the component?
        if (!ev.shiftKey && item.shadowRoot && focusOn(getFocusableExpandSlots(item)[0])) {
            return true;
        }

        // Find next element in list, if el is current element
        const nextFocus = (el, list, shiftKey) => {
            const index = list.findIndex(e => e === el);
            if (index >= 0) {
                return list[shiftKey ? index - 1 : index + 1]; // Preceeding or following?
            }
            // el did not occur in list. It probably has tabindex="-1". Figure out where it belongs in list (ignoring tabindex reordering...)
            const bf = Node.DOCUMENT_POSITION_FOLLOWING | Node.DOCUMENT_POSITION_CONTAINED_BY;
            const after = list.findIndex(e => (el.compareDocumentPosition(e) & bf) !== 0);
            return list[shiftKey ? after - 1 : after];
        };

        for (let el = item; el !== cell;) {
            const ctx = (el.nodeType === 11 && el.host);
            if (ctx) {
                // A hack. It ctx is a sub ptcs-collection then it would have handled the Tab event if it could. It couldn't, so it must be skipped
                const fe = ctx.tagName !== 'PTCS-COLLECTION' && nextFocus(item, getFocusableExpandSlots(ctx), ev.shiftKey);
                if (fe && focusOn(ev.shiftKey ? getLastFocusable(fe) : fe)) {
                    return true;
                }
                item = (el.nodeType === 11 && el.host) || item;
                el = ctx;
            } else {
                el = (el.assignedSlot || el).parentNode;
            }
        }

        const focusable = getFocusableExpandSlots(cell);
        const index = focusable.findIndex(e => composedPath.indexOf(e) >= 0);

        /* istanbul ignore next */
        if (index < 0) {
            return false; // Should never happen
        }
        if (ev.shiftKey) {
            // Move focus backwards
            if (index > 0) {
                focusOn(getLastFocusable(focusable[index - 1]));
            } else if (this.cellTabKeyScope === 'cell') {
                focusOn(getLastFocusable(focusable[focusable.length - 1])); // Find last sub-item of item
            } else if (this.cellTabKeyScope === 'item') {
                // Find focusable sub-element in preceding cell. Only look at cells in the DOM
                const rowix = Number(cell.parentNode.getAttribute('rowix'));
                const cells = [...this.shadowRoot.querySelectorAll('[rowix]')]
                    .filter(row => Number(row.getAttribute('rowix')) <= rowix)
                    .sort(compareRows)
                    .reduce(collectCells, []);

                for (let i = cells.indexOf(cell) - 1; i >= 0; i--) {
                    const f = getFocusableExpandSlots(cells[i]);
                    if (f.length) {
                        this._focusedOnCell(cells[i]);
                        focusOn(getLastFocusable(f[f.length - 1]));
                        this._chunker.scrollTo(Number(cells[i].parentNode.getAttribute('rowix')));
                        return true;
                    }
                }

                if (delegateToPrev(this)) {
                    return true;
                }
                // Collection appears to be the first focusable element in the DOM. Let's focus on it, or we'll be locked here.
                this.__backwardsFocusing = true;
                const r = focusOn(this);
                this.__backwardsFocusing = undefined;
                return r;
            } else {
                // On first item in 'collection' cellTabKeyScope - go back to cell
                cell.focus();
            }
        } else {
            // Move focus forwards
            // eslint-disable-next-line no-lonely-if
            if (index + 1 < focusable.length) {
                focusOn(focusable[index + 1]);
            } else if (this.cellTabKeyScope === 'cell') {
                // Rollback to first cell on row
                focusOn(focusable[0]);
            } else if (this.cellTabKeyScope === 'item') {
                // Find focusable sub-element in following cell. Only look at cells in the DOM
                const rowix = Number(cell.parentNode.getAttribute('rowix'));
                const cells = [...this.shadowRoot.querySelectorAll('[rowix]')]
                    .filter(row => Number(row.getAttribute('rowix')) >= rowix)
                    .sort(compareRows)
                    .reduce(collectCells, []);

                for (let i = cells.indexOf(cell) + 1; i < cells.length; i++) {
                    const f = getFocusableExpandSlots(cells[i]);
                    if (f.length) {
                        this._focusedOnCell(cells[i]);
                        focusOn(f[0]);
                        this._chunker.scrollTo(Number(cells[i].parentNode.getAttribute('rowix')));
                        return true;
                    }
                }
                // Worst case scenario - collection is last element, so focus goes back to random element in virtual scroller
                return delegateToNext(this, true);
            } else { // this.cellTabKeyScope === 'collection'
                // On last item in 'collection' cellTabKeyScope - go back to cell
                cell.focus();
            }
        }

        return true;
    }

    // Take another look at the keydown after every descendant has had a chance to act on it
    _keyDownPost(ev) {
        if (this.disabled || !(this.tabIndex >= 0)) {
            return;
        }

        if (ev.defaultPrevented) {
            this._scrollFocusIntoView(true); // Someone else took the event. Scroll it into sight
            return;
        }

        if (!isScrollKey(ev.key)) {
            this._scrollFocusIntoView(true);
        }

        const cell = this.findCell(ev.target);
        if (!cell || ev.target === cell) {
            return; // Post-processing keydown only applies to elements inside the cells, not the cells themselves
        }

        switch (ev.key) {
            case 'ArrowLeft':
                if (!(ev.ctrlKey && this._scrollCell(cell, -cell.clientWidth / 2, 0))) {
                    return;
                }
                break;

            case 'ArrowRight':
                if (!(ev.ctrlKey && this._scrollCell(cell, cell.clientWidth / 2, 0))) {
                    return;
                }
                break;

            case 'ArrowUp':
                if (!(ev.ctrlKey && this._scrollCell(cell, 0, -cell.clientHeight / 2))) {
                    return;
                }
                break;

            case 'ArrowDown':
                if (!(ev.ctrlKey && this._scrollCell(cell, 0, cell.clientHeight / 2))) {
                    return;
                }
                break;

            case 'Tab':
                if (!this._processCellItemTab(cell, ev)) {
                    return; // Could not process the event
                }
                break;

            case 'F2':
            case 'Escape':
                if (this.cellTabKeyScope === 'item') {
                    return;
                }
                // Move focus back to cell
                cell.focus();
                break;

            default:
                return;
        }

        ev.preventDefault();
    }

    _keyUp(ev) {
        // Only needed when we Tab into the collection. We'll miss keydown, but not keyup
        if (!isScrollKey(ev.key)) {
            this._scrollFocusIntoView(true);
        }
    }
};

customElements.define(PTCS.Collection.is, PTCS.Collection);


// Hack to track Tab events - so we can adjust focus when someone tabs into the collection and the browser focuses on a wrong element
document.addEventListener('keydown', ev => {
    __lastTabKey = ev.key === 'Tab' && Date.now();
}, {capture: true, passive: true});
