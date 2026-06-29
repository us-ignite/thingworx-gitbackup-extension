// INPUT: columns: Array of
// {
//    label:     string || function   // Creates column header: label or function that creates an element: Default: ''
//    name:      string               // Specifies a name for the column. Sematics unclear. The header will occur in the footer (for whatever reason)
//    title:     string               // Specifies label for value when editied in a row-edit popup (several editable entries in the same popup)
//    value:     field  || function   // Extracts column value from data item. Field name or function(item, index, dataManager). Required
//    baseType                        // Data type of value. Required
//    width:     number || `${number}${unit}`   // Pixel value or unit value: fr for adaptive widths. Default '1fr'
//    minWidth:  number                         // Minium width. Default: 0
//    maxWidth:  number                         // Maximum width. Default: unspecified
//    halign:    'left' || 'center' || 'right'  // Horizontal alignment. Default: 'left'
//    valign:    'top || 'center' || 'bottom'   // Vertical alignment. Default: 'top'
//    resizable: boolean                        // Can column be resized? Default: false
//    editable:  boolean || field name          // Can column values be edited? Default: false
//    sortable:  boolean                        // Add a sort icon in the header label?
//    resolve:   boolean                        // Use delayed binding for column? If enabled, then value must be a field name
//    config:    object                         // baseType specific parameters. See below
//
// Editing:
//    enum:      Array || function     // Allowed values: [{label, value}, ...]. The function (item, index, dataManager) can return a Promise
//    encode:                          // (value, item, index, dataManager) -> proper encoding of value that can be assigned to the column field
// }

// NOTE: The value property must return a value that consistent with the specified baseType
//       Example: a numeric property may not return a string or an object

// The config parameter:
//----------------------
// All types:
//   - editable (copied from column definition): must be handled by the type
//   - cellMarker(cellElement): put marks on the cell that contains the UI control: handled automatically
//   - format(item, index): returns a value that is sent to the cell renderer. The type must be supported by the render.
//   - unresolvable(item, columnId (= value || name || label), index, dataManager) => true [if value can never be resolved (e.g. no row id)]
//
// DATETIME:
//   - locales: locales argument to Intl.DateTimeFormat([locales[, options]])
//   - options: options argument to Intl.DateTimeFormat([locales[, options]])
//
// IMAGELINK:
//   - size:     image size (see ptcs-image)
//   - position: image position (see ptcs-image)
//
// NUMBER:
//   - locales: locales argument to Intl.NumberFormat([locales[, options]])
//   - options: options argument to Intl.NumberFormat([locales[, options]])
//
// STRING:
//   - enum: list of choices (for editing)
//
import {PTCS} from 'ptcs-library/library.js';
import {ValueManager} from './grid-values.js';


// Create header elements
import {headerCreatorFunc} from './grid-view/gv-header';

// UI controls for types
import {uiBoolean} from './grid-view/gv-boolean';
import {uiString} from './grid-view/gv-string';
import {uiText} from './grid-view/gv-text';
import {uiNumber} from './grid-view/gv-number';
import {uiDatetime} from './grid-view/gv-datetime';
import {uiImagelink} from './grid-view/gv-imagelink';
import {uiHyperlink} from './grid-view/gv-hyperlink';
import {uiHtml} from './grid-view/gv-html';
import {uiLocation} from './grid-view/gv-location';
import {uiDefault} from './grid-view/gv-default';
import {uiFallback} from './grid-view/gv-fallback';
import {uiSelect, selectCreatorFunc} from './grid-view/gv-select';
import {uiDelete} from './grid-view/gv-delete-button';
import {uiBadge} from './grid-view/gv-badge';
import {uiGrouping} from './grid-view/gv-grouping';
import {uiResolveValue} from './grid-view/resolve-value';
import {uiTreeToggle} from './grid-view/tree-toggle';


/* eslint-disable no-nested-ternary */

//
// Sorting
//
const compareBool = (a, b) => a ? (b ? 0 : -1) : (b ? 1 : 0); // true > false

const lastChar = String.fromCharCode(parseInt(0x10ffff, 16));
const _s = s => typeof s === 'string' ? s : (typeof s === 'symbol' ? lastChar : '');
const _compareString = (a, b) => (a || '').localeCompare(b || '');
const compareString = (a, b) => _compareString(_s(a), _s(b));

const distantPast = -16000 * 365.24 * 24 * 60 * 60 * 1000;
const compareDate = (a, b) => (a instanceof Date ? a.getTime() : distantPast) - (b instanceof Date ? b.getTime() : distantPast);

const compareNumber = (a, b) => {
    // isNaN(symbol) throws an exception!
    if (typeof a === 'symbol' || a === '' || isNaN(a)) {
        return (typeof b === 'symbol' || b === '' || isNaN(b)) ? 0 : -1;
    }
    if (typeof b === 'symbol' || b === '' || isNaN(b)) {
        return 1;
    }
    return a - b;
};

const compareLink = (a, b) => {
    if (!a || !b) {
        return a ? 1 : (b ? -1 : 0);
    }
    if (typeof a === 'object' && typeof b === 'object') {
        return compareString(a.label, b.label) || compareString(a.href, b.href);
    }
    return compareString(a, b);
};

const emptyLoc = {elevation: 0, latitude: 0, longitude: 0};
const compareLoc = (a, b) => {
    if (typeof a === 'object' && typeof b === 'object') {
        if (!a) {
            a = emptyLoc;
        }
        if (!b) {
            b = emptyLoc;
        }
        return (a.latitude - b.latitude) || (a.longitude - b.longitude) || (a.elevation - b.elevation);
    }
    if (typeof a === 'string' && typeof b === 'string') {
        return compareString(a, b);
    }
    if (!a) {
        return !b ? 0 : -1;
    }
    if (!b) {
        return 1;
    }
    return 0; // Don't know how to compare
};

const compareType = {
    BASETYPENAME: compareString,
    BOOLEAN:      compareBool,
    DATETIME:     compareDate,
    DEFAULT:      compareString,
    HTML:         compareString, // or what?
    HYPERLINK:    compareLink,
    // IMAGE - compare image data?
    IMAGELINK:    compareLink,
    INTEGER:      compareNumber,
    LOCATION:     compareLoc,
    LONG:         compareNumber,
    NUMBER:       compareNumber,
    STRING:       compareString,
    TEXT:         compareString,
    THINGNAME:    compareString,
    USERNAME:     compareString
};

// Returns a compare function for recognized baseTypes
export const baseTypeComparator = baseType => compareType[baseType];

//
// Mapping table
//
const uiMap = {
    BOOLEAN:      uiBoolean,
    DATETIME:     uiDatetime,
    DEFAULT:      uiDefault,
    HYPERLINK:    uiHyperlink,
    HTML:         uiHtml,
    IMAGE:        uiImagelink,
    IMAGELINK:    uiImagelink,
    INTEGER:      uiNumber,
    LONG:         uiNumber,
    NUMBER:       uiNumber,
    STRING:       uiString,
    TEXT:         uiText,
    // TWX TYPES
    BASETYPENAME: uiString,
    LOCATION:     uiLocation,
    XML:          uiText,
    JSON:         uiText,
    THINGNAME:    uiString,

    '#select': uiSelect,
    '#delete': uiDelete,
    '#badge':  uiBadge,
    '#group':  uiGrouping
};


function ui(type, config, $i18n) {
    let uiControl = (uiMap[type] || uiFallback)(config, $i18n);

    if (config && typeof config.cellMarker === 'function') {
        const create = uiControl.create;
        const cellMarker = config.cellMarker;
        uiControl = {
            create: cell => {
                cellMarker(cell);
                return create(cell);
            },

            assign: uiControl.assign
        };
    }

    return uiControl;
}


// Get index of item
const valueIndex = (_, index) => index + 1;

function decodeSize(size) {
    const _size = size && PTCS.cssDecodeSize(size);
    return _size > 0 ? _size : undefined;
}

function editAction(ev) {
    if (PTCS.wrongMouseButton(ev)) {
        return;
    }
    ev.target.dispatchEvent(new CustomEvent('edit-activated', {bubbles: true}));
}

function editKeydown(ev) {
    if (ev.key === ' ' || ev.key === 'Enter') {
        ev.target.click();
    }
}

// It has become messy to determine the name of a column. Lets do it in one place only
export function columnName(col) {
    return (typeof col.name === 'string' && col.name) || (typeof col.label === 'string' && col.label) || (typeof col.title === 'string' && col.title);
}

//
// The DataViewer
//
export class DataViewer {

    constructor(columnsDef, options) {
        const opts = options || {};
        this.selectMethod = opts.selectMethod;
        this._canDelete = opts.canDelete;
        this._rowDepField = opts.rowDepField;
        this._singleLineHeader = opts.singleLineHeader;
        this.maxHeightHeader = opts.maxHeightHeader;
        this.maxHeightRow = opts.maxHeightRow;
        this.minHeightRow = opts.minHeightRow;
        this._singleLineRows = opts.singleLineRows;
        this._showRowNumbers = opts.showRowNumbers;
        this._editLevel = opts.editLevel;
        this._editControl = opts.editControl;
        this._editControlValue = opts.editControlValue;
        this._editControlVisibility = opts.editControlVisibility || 'hover';
        this._headerVerticalAlignment = opts.headerVerticalAlignment || 'top';
        this._rowsVerticalAlignment = opts.rowsVerticalAlignment || 'top';
        this._sortSelectionColumn = opts.sortSelectionColumn;
        // TODO: externalSort does NOT belong in the view configurator. (Only remains here because of backwards compatibility)
        this._externalSort = opts.externalSort;
        this._hideTreeToggle = opts.hideTreeToggle;
        this._dragRows = opts.dragRows;
        this._highlightNewRows = opts.highlightNewRows;
        this._defaultColumnSortState = opts.defaultColumnSortState;

        this._observers = new Set();
        this._rowDataAccessor = [];

        // The group row
        const uiGrp = ui('#group');
        this._groupRow = [{
            create: uiGrp.create,
            assign: uiGrp.assign,
            select: item => item,
            valign: 'center'
        }];

        this.columnsDef = columnsDef;

        this.valueManager = new ValueManager();
    }

    // observe changes
    observe(cb) {
        this._observers.add(cb);
    }

    // unobserve changes
    unobserve(cb) {
        this._observers.delete(cb);
    }

    // Notify observers
    _msg(msg, ...arg) {
        this._observers.forEach(cb => {
            if (typeof cb[msg] === 'function') {
                cb[msg](...arg);
            }
        });
    }

    set sortSelectionColumn(_sortSelectionColumn) {
        if (this._sortSelectionColumn === _sortSelectionColumn) {
            return;
        }

        this._sortSelectionColumn = _sortSelectionColumn;

        this._rebuildRowDef();
    }

    get sortSelectionColumn() {
        return this._sortSelectionColumn;
    }

    set externalSort(_externalSort) {
        if (this._externalSort === _externalSort) {
            return;
        }

        this._externalSort = _externalSort;

        this._rebuildRowDef(); // TODO: A very costly way to send the change message...
    }

    get externalSort() {
        return this._externalSort;
    }

    get initialSortExpr() {
        return this._initialSortExpr;
    }

    // Support header row single Line
    set singleLineHeader(_singleLineHeader) {
        if (this._singleLineHeader === _singleLineHeader) {
            return;
        }
        this._singleLineHeader = _singleLineHeader;
        this._rebuildRowDef();
    }

    // Support rows single Line
    set singleLineRows(_singleLineRows) {
        if (this._singleLineRows === _singleLineRows) {
            return;
        }
        this._singleLineRows = _singleLineRows;
        this._rebuildRowDef();
    }

    // Support rows numbers
    set showRowNumbers(_showRowNumbers) {
        if (this._showRowNumbers === _showRowNumbers) {
            return;
        }
        this._showRowNumbers = _showRowNumbers;
        this._rebuildRowDef();
    }

    get showRowNumbers() {
        return this._showRowNumbers;
    }

    // Support edit level
    set editLevel(_editLevel) {
        if (this._editLevel === _editLevel) {
            return;
        }
        this._editLevel = _editLevel;
        this._rebuildRowDef();
    }

    get editLevel() {
        return this._editLevel;
    }

    // Support edit level
    set highlightNewRows(highlightNewRows) {
        if (this._highlightNewRows === highlightNewRows) {
            return;
        }
        this._highlightNewRows = highlightNewRows;
        this._rebuildRowDef();
    }

    get highlightNewRows() {
        return this._highlightNewRows;
    }

    // Support edit control
    set editControl(_editControl) {
        if (this._editControl === _editControl) {
            return;
        }
        this._editControl = _editControl;
        this._rebuildRowDef();
    }

    get editControl() {
        return this._editControl;
    }

    // Set edit control value (icon name or link label)
    set editControlValue(_editControlValue) {
        if (this._editControlValue === _editControlValue) {
            return;
        }
        this._editControlValue = _editControlValue;
        this._rebuildRowDef();
    }

    get editControlValue() {
        return this._editControlValue;
    }

    // Set edit control visibility: 'hover', 'always', 'never'
    set editControlVisibility(_editControlVisibility) {
        if (this._editControlVisibility === _editControlVisibility) {
            return;
        }
        this._editControlVisibility = _editControlVisibility;
        this._rebuildRowDef();
    }

    get editControlVisibility() {
        return this._editControlVisibility;
    }

    // Support header row max-height
    set maxHeightHeader(_maxHeightHeader) {
        const h = decodeSize(_maxHeightHeader);
        if (this._maxHeightHeader !== h) {
            this._maxHeightHeader = h;
            this._rebuildRowDef();
        }
    }

    get maxHeightHeader() {
        return this._maxHeightHeader;
    }

    // Support row max-height
    set maxHeightRow(_maxHeightRow) {
        const h = decodeSize(_maxHeightRow);
        if (this._maxHeightRow !== h) {
            this._maxHeightRow = h;
            this._rebuildRowDef();
        }
    }

    get maxHeightRow() {
        return this._maxHeightRow;
    }

    // Support row min-height
    set minHeightRow(_minHeightRow) {
        const h = decodeSize(_minHeightRow);
        if (this._minHeightRow !== h) {
            this._minHeightRow = h;
            this._rebuildRowDef();
        }
    }

    get minHeightRow() {
        return this._minHeightRow;
    }

    // Support Header Vertical Alignment
    set headerVerticalAlignment(_headerVerticalAlignment) {
        if (this._headerVerticalAlignment !== _headerVerticalAlignment) {
            this._headerVerticalAlignment = _headerVerticalAlignment;
            this._rebuildRowDef();
        }
    }

    // Support Rows vertical alignment
    set rowsVerticalAlignment(_rowsVerticalAlignment) {
        if (this._rowsVerticalAlignment !== _rowsVerticalAlignment) {
            this._rowsVerticalAlignment = _rowsVerticalAlignment;
            this._rebuildRowDef();
        }
    }

    get $i18n() {
        return this.__i18n;
    }

    set $i18n(i18n) {
        if (i18n !== this.__i18n) {
            this.__i18n = (i18n && typeof i18n === 'object') ? i18n : undefined;
            this._rebuildRowDef();
        }
    }

    // Support deletions: add a delete button column
    set canDelete(_canDelete) {
        if (!!_canDelete === !!this._canDelete) {
            return;
        }
        this._canDelete = !!_canDelete;
        this._rebuildRowDef();
    }

    get canDelete() {
        return !!this._canDelete;
    }

    // Support rows numbers
    set hideTreeToggle(_hideTreeToggle) {
        if (!this._hideTreeToggle === !_hideTreeToggle) {
            return;
        }
        this._hideTreeToggle = _hideTreeToggle;
        this._rebuildRowDef();
    }

    get hideTreeToggle() {
        return this._hideTreeToggle;
    }

    /* ********************************************************************
     * DRAG-AND-DROP-GRID-ROWS
     * ********************************************************************
    set dragRows(_dragRows) {
        if (this._dragRows === _dragRows) {
            return;
        }
        this._dragRows = _dragRows;
        this._rebuildRowDef();
    }

    get dragRows() {
        return this._dragRows;
    }
    ********************************************************************** */

    // Support selections: add a select button column: 'single' || 'multiple'
    set selectMethod(_selectMethod) {
        const sm = (_selectMethod === 'single' || _selectMethod === 'multiple') ? _selectMethod : undefined;
        if (sm === this._selectMethod) {
            return;
        }
        this._selectMethod = sm;
        this._rebuildRowDef();
    }

    get selectMethod() {
        return this._selectMethod || 'none';
    }

    get rowDepField() {
        return this._rowDepField;
    }

    get resolvedValues() {
        return this.valueManager.columnIds;
    }

    // Set column specification
    set columnsDef(_columnsDef) {
        if (_columnsDef === this._columnsDef) {
            return; // Just in case the same def are assigned several times ...
        }

        this._columnsDef = _columnsDef;
        this._msg('dvColumnsDef');
        this._rebuildRowDef();
    }

    get columnsDef() {
        return this._columnsDef;
    }

    _rebuildRowDef() {
        if (this.requestAnimationFrameId) {
            return;
        }

        this.requestAnimationFrameId = requestAnimationFrame(() => {
            const hadArray = Array.isArray(this._columns);

            this.requestAnimationFrameId = undefined;
            if (this._columnsDef) {
                this._rebuildRowDefNow();
            } else {
                this._columns = null;
            }

            this._rebuildRowDataAccessor();
            this._rebuildSortExpr();

            if (hadArray || Array.isArray(this._columns)) {
                this._msg('dvChanged');
            }
        });
    }

    getOrderExpression(defaultValue) {
        return (!defaultValue && this._orderExpression) ||
            (defaultValue !== false && PTCS.encodeViewExpr(this.columnsDef.map(columnName))) ||
            undefined;
    }

    getVisibilityExpression(defaultValue) {
        return (!defaultValue && this._visibilityExpression) ||
        (defaultValue !== false && PTCS.encodeViewExpr(this.columnsDef.map(col => [columnName(col), `${!col.hidden}`]))) ||
        undefined;
    }

    // This method sets visibility of columns and the column order
    setVisibilityExpression(expr) {
        if (expr === this._visibilityExpression) {
            return; // No change
        }
        this._visibilityExpression = (expr !== this.getVisibilityExpression(true)) ? expr : undefined;

        if (!this._columns) {
            return;
        }
        // Assign the visibility to the existing columns
        this._rebuildRowDef();
    }

    // Decode columns order and visibility - and make sure the spec works for columns (every column have a specified visibility, and vice versa)
    _decodeColumnVisibility() {
        if (!this._visibilityExpression || !this._columnsDef) {
            return undefined;
        }
        const order = PTCS.decodeViewExpr(this._visibilityExpression).map(([name, visible]) => { // ColumnName : isVisible?
            return {col: this._columnsDef.find(col => columnName(col) === name), hidden: visible === 'false'};
        });

        return order.length === this._columnsDef.length && this._columnsDef.every(col => order.find(obj => obj.col === col)) && order;
    }

    // This method should NOT be part of the view configurator. Kept for backwards compatibility
    getWidthsExpression(defaultValue) {
        return (!defaultValue && this._widthsExpression) ||
            (defaultValue !== false && PTCS.encodeViewExpr(this.columnsDef.map(col => [columnName(col), `${col.width || '1fr'}`]))) ||
            undefined;
    }

    // This method should NOT be part of the view configurator. Kept for backwards compatibility (so getWidthsExpression returns the expected value)
    setWidthsExpression(expr, opt) {
        if (expr === this._widthsExpression) {
            return; // Ignore
        }
        this._widthsExpression = (expr && expr !== this.getWidthsExpression(true)) ? expr : undefined;

        if (!opt || opt.rebuildRowDef !== false) {
            this._rebuildRowDef();
        }
    }

    /*
     * Builds sort expression from the current sort orders
     */
    getSortExpression() {
        const short = [];
        const full = [];

        PTCS.decodeViewExpr(this.__sortExpression).forEach(([name, order]) => {
            if (order === 'asc' || order === 'desc') {
                short.push([name, order]);

                full.push({
                    fieldName:   name,
                    isAscending: order === 'asc'
                });
            }
        });

        return {short: PTCS.encodeViewExpr(short), full};
    }

    /*
     * Sets the sort expression (ex "Title:desc,Col2:asc") and communicates it via dvSort
     *
     * NOTE: This method should not be part of the view configurator, but remains here for backwards compatibility
     */
    setSortExpression(expr, dm, opt) {
        const _expr = (typeof expr === 'string' && expr) || undefined; // Either a real string or undefined
        if (this.__sortExpression === _expr) {
            return; // No change
        }
        this.__sortExpression = _expr;
        this._msg('dvSort', _expr, opt);
    }

    createEditControl() {
        if (this._editControl === 'none') {
            return null;
        }
        let el;
        if (this._editControl === 'link') {
            el = document.createElement('ptcs-link');
            el.label = this._editControlValue || 'edit';
            el.singleLine = false;
            el.setAttribute('variant', 'primary'); // Start with correct variant
            el.setAttribute('is-link', ''); // Tell theme engine that this is alink
            el.noTabindex = true;
            if (this.editLevel === 'cell') {
                el.alignment = 'right';
            }
        } else {
            el = document.createElement('ptcs-icon');
            el.size = 'small';
            el.icon = this._editControlValue || 'cds:icon_edit';
            el.setAttribute('is-icon', ''); // Tell theme engine that this is an icon
            el.setAttribute('style-focus', ''); // Need help with focus styling
            el.addEventListener('keydown', editKeydown);
            el.style.flex = '0 0 auto';
        }
        el.setAttribute('part', 'edit-control');
        el.setAttribute('grid-action', '');
        el.setAttribute('tabindex', '-1');
        el.addEventListener('click', editAction);
        return el;
    }

    assignEditControl(/* el, item, index, dm */) {
        // Do nothing
    }

    _rebuildRowDefNow() {
        console.assert(this._columnsDef);
        const columnsId = [];
        const colDefs = this._decodeColumnVisibility() || (this._columnsDef.map(col => ({col, hidden: !!col.hidden})));

        const _editable = (editable, fieldName, baseType) => {
            if (!editable) {
                return undefined;
            }
            if (baseType === 'INFOTABLE' || baseType === 'TAGS') {
                // These types should never be editable, whatever the config says
                return undefined;
            }
            if (typeof editable === 'string') {
                return editable;
            }
            return typeof fieldName === 'string' ? fieldName : undefined;
        };

        // Create map of current sort orders
        const sortOrders = this.__sortExpression && this.__sortExpression.split(',').reduce((acc, key) => {
            const a = key.split(':');
            const name = PTCS.restoreViewExprChars(a[0]);
            if (a.length !== 2 || (a[1] !== 'asc' && a[1] !== 'desc') || !colDefs.some(({col}) => columnName(col) === name)) {
                acc = false; // Bad format or unrecognized column
            } else if (acc) {
                acc[name] = a[1];
            }
            return acc;
        }, {});

        // Get value of item
        const valueFunc = (value, resolve) => {
            switch (typeof value) {
                case 'string':
                    if (value === '#index') {
                        return valueIndex;
                    }
                    return resolve ? item => this.valueManager.value(item, value) : item => item[value];

                case 'function':
                    return value;
            }
            throw new Error(`Unknown value accessor ${value}`);
        };

        this._columns = colDefs.map(({col, hidden}) => {
            const select = valueFunc(col.value, col.resolve);
            let sortSelect = select;

            switch (col.baseType) {
                case 'DATETIME':
                    sortSelect = item => {
                        const v = select(item);

                        if (typeof v === 'string' && col.name) {
                            return item[col.name];
                        }

                        return v;
                    };
                    break;
            }

            const compare = col.compare || compareType[col.baseType];
            const editable = _editable(col.editable, col.value, col.baseType);
            const colConfig = { // Extract column properties that the column renderer needs to know
                baseType:           col.baseType,
                maxHeightRow:       this._maxHeightRow !== undefined && this._maxHeightRow > 0 ? this._maxHeightRow : '',
                minHeightRow:       this._minHeightRow !== undefined && this._minHeightRow > 0 ? this._minHeightRow : '',
                singleLineRows:     this._singleLineRows,
                halign:             col.halign,
                valign:             col.valign || this._rowsVerticalAlignment,
                enum:               col.enum,
                encode:             col.encode,
                validationFunction: col.validationFunction,
                preserveWhiteSpace: col.preserveWhiteSpace && col.config?.editor === 'textarea'
            };
            Object.assign(colConfig, col.config); // Add clients column properties, with preference
            const __uiCol = col.$uiCtrl || ui(col.baseType, Object.assign({editable}, colConfig), this.$i18n);
            const name = columnName(col);

            const resolveAs = col.resolve && ((typeof col.value === 'string' && col.value) || name);
            if (resolveAs) {
                columnsId.push(resolveAs);
            }

            const _uiCol = resolveAs ? uiResolveValue(resolveAs, {unresolvable: colConfig.unresolvable, ...__uiCol}) : __uiCol;
            const uiCol = col.treeToggle ? uiTreeToggle(_uiCol, {toggle: col.treeToggle, hideToggle: this._hideTreeToggle}) : _uiCol;

            const _sortable = col.sortable || (col.sortable === undefined && this._defaultColumnSortState);
            const $sortName = _sortable && name;
            const sortOrder = _sortable && (sortOrders ? sortOrders[$sortName] : col.sortOrder) || 'none';
            const _isBulkSelectEnabled = col.baseType === 'BOOLEAN' && col.config?.bulkSelection && this._editLevel !== 'row' &&
                this._editControlVisibility === 'never' && (col.config?.editor || 'checkbox') === 'checkbox' &&
                !!col.editable && !!this.editLevel;

            return {
                name:      col.name,
                depcolumn: col.depcolumn,
                label:     headerCreatorFunc({
                    label:         col.label,
                    sortable:      _sortable,
                    sortOrder,
                    compare,
                    singleLine:    this._singleLineHeader,
                    maxHeight:     this._maxHeightHeader ? this._maxHeightHeader : '',
                    hAlign:        col.headerHAlign,
                    vAlign:        col.valign || this._rowsVerticalAlignment,
                    minWidth:      col.minWidth,
                    maxWidth:      col.maxWidth,
                    name,
                    bulkSelection: _isBulkSelectEnabled
                }),
                create:         uiCol.create,
                assign:         uiCol.assign,
                format:         uiCol.format,
                select,
                render:         colConfig.format,
                sortSelect,
                compare,
                width:          col.width,
                minWidth:       col.minWidth,
                maxWidth:       col.maxWidth,
                halign:         col.halign,
                valign:         col.valign || this._rowsVerticalAlignment,
                headerHAlign:   col.headerHAlign,
                headerVAlign:   col.headerVAlign || this._headerVerticalAlignment,
                resizable:      col.resizable,
                $sortName,
                sortable:       _sortable,
                sortOrder,
                type:           col.baseType,
                hidden,
                treeToggle:     col.treeToggle,
                nonReorderable: col.nonReorderable,

                // Needed for inline editing
                editable,
                noRowEdit:          col.noRowEdit,
                title:              col.label,
                config:             col.config,
                validationFunction: col.validationFunction,
                enum:               col.enum,
                encode:             col.encode,
                externalEdit:       uiCol.externalEdit
            };
        });

        this.valueManager.columnIds = columnsId;

        if (this._columns.length === 0) {
            // Client did not specify any columns, so view configurator should not add any extra columns
            return;
        }

        const sortSelection = (this._selectMethod === 'multiple' && this._sortSelectionColumn);

        if (this.editLevel === 'row' && this._editControlVisibility !== 'never') {
            this._columns.unshift({
                id:             'edit',
                label:          '',
                create:         this.createEditControl.bind(this),
                assign:         this.assignEditControl,
                select:         item => item,
                width:          '52px',
                halign:         'center',
                valign:         this._rowsVerticalAlignment,
                nonresizable:   true,
                nonReorderable: true
            });
        }

        if (this._selectMethod === 'single' || this._selectMethod === 'multiple') {
            const uiSel = ui('#select', {selectMethod: this._selectMethod, showRowNumbers: this._showRowNumbers});
            this._columns.unshift({
                id:             'select',
                label:          selectCreatorFunc(this._selectMethod, this._singleLineHeader, this._maxHeightHeader, sortSelection),
                create:         uiSel.create,
                assign:         uiSel.assign,
                format:         null,
                select:         item => item,
                compare:        (a, b, i1, i2, dm) => Number(dm.isSelectedBaseIndex(i1)) - Number(dm.isSelectedBaseIndex(i2)),
                $sortName:      '#select',
                sortable:       sortSelection,
                sortOrder:      sortSelection && ((sortOrders && sortOrders['#select']) || 'none'),
                width:          `var(--ptcs-core-grid-selection-width,${sortSelection ? 66 : 34}px)`,
                halign:         sortSelection ? 'left' : 'center',
                valign:         this._rowsVerticalAlignment,
                headerHAlign:   sortSelection ? 'left' : 'center',
                headerVAlign:   this._headerVerticalAlignment,
                nonresizable:   true,
                nonReorderable: true
            });
        }

        if (this._showRowNumbers) {
            const uiRowNumber = ui('NUMBER', {selectMethod: this._selectMethod, showRowNumbers: this._showRowNumbers});
            this._columns.unshift({
                id:             'showRowNumbers',
                create:         uiRowNumber.create,
                assign:         uiRowNumber.assign,
                format:         null,
                select:         (item, index) => index + 1,
                width:          '51px',
                halign:         'left',
                valign:         this._rowsVerticalAlignment,
                nonReorderable: true
            });
        }

        if (this._canDelete) {
            const uiDel = ui('#delete');
            this._columns.push({
                id:             'delete',
                create:         uiDel.create,
                assign:         uiDel.assign,
                select:         item => item,
                format:         null,
                width:          '36px',
                halign:         'center',
                valign:         this._rowsVerticalAlignment,
                headerHAlign:   'center',
                headerVAlign:   this._headerVerticalAlignment,
                nonReorderable: true
            });
        }

        if (this._highlightNewRows) {
            const _uiBadge = ui('#badge');
            this._columns.unshift({
                id:             'badge',
                create:         _uiBadge.create,
                assign:         _uiBadge.assign,
                select:         item => item,
                format:         null,
                width:          'var(--ptcs-core-grid-badge-width, 24px)',
                halign:         'center',
                valign:         'center',
                nonresizable:   true,
                nonReorderable: true
            });
        }

        let nonReorderable = true;
        this._columns.forEach(col => {
            nonReorderable = col.nonReorderable = nonReorderable && col.nonReorderable;
        });
    }

    // Get columns configuration
    get columns() {
        return this._columns;
    }

    // Get rows configuration
    getRowDef(item) {
        return item.hasOwnProperty('$groupKey') ? this._groupRow : this._columns;
    }

    _rebuildRowDataAccessor() {
        this._rowDataAccessor = Array.isArray(this._columns)
            ? this._columns
                .filter(col => !col.hidden && col.format !== null)
                .map(col => {
                    const select = col.select;
                    const render = col.render;
                    const format = col.format;
                    if (typeof format === 'function') {
                        return (item, i, dataManager) => format(select(item, i, dataManager), render ? render(item, i, dataManager) : undefined);
                    }
                    if (render) {
                        return (item, i, dataManager) => [`${render(item, i, dataManager)}`, `${select(item, i, dataManager)}`];
                    }
                    return select;
                })
            : [];
    }

    // Returns the default sort expression as specified by columnsDef, using sortable and sortOrder
    defaultSortExpr() {
        return this._defaultSortExpr;
    }

    // Called whenever columnsDef has changed and its corresponding columns have been created
    _rebuildSortExpr() {
        const initialSortExpr = (PTCS.encodeViewExpr(Array.isArray(this.columnsDef) && this.columnsDef.reduce((acc, col) => {
            if (!col.sortable && (col.sortOrder === 'asc' || col.sortOrder === 'desc')) {
                // This column does not have a sort button but it does have a sort order
                acc.push([columnName(col), col.sortOrder]);
            }
            return acc;
        }, []))) || undefined;

        // Default sort order (as specified by columnsDef)
        const defaultSortExpr = PTCS.encodeViewExpr(Array.isArray(this._columnsDef) && this._columnsDef.reduce((acc, col) => {
            const sortName = col.sortable && columnName(col);
            if (sortName && (col.sortOrder === 'asc' || col.sortOrder === 'desc')) {
                acc.push([sortName, col.sortOrder]);
            }
            return acc;
        }, []));

        // Actual sort expression (default sort order combined with latest sorting)
        const sortExpr = PTCS.encodeViewExpr(Array.isArray(this._columns) && this._columns.reduce((acc, col) => {
            if (col.$sortName && (col.sortOrder === 'asc' || col.sortOrder === 'desc')) {
                acc.push([col.$sortName, col.sortOrder]);
            }
            return acc;
        }, []));

        // The inital sort expression (when no sort buttons are active)
        if (initialSortExpr !== this._initialSortExpr) {
            this._initialSortExpr = initialSortExpr;
            this._msg('dvSortInitial', initialSortExpr); // The default sort expression changed
        }

        // The default sort expression (the default values of the sort buttons)
        if (defaultSortExpr !== this._defaultSortExpr) {
            this._defaultSortExpr = defaultSortExpr;
            this._msg('dvSortDefault', defaultSortExpr); // The default sort expression changed
        }

        // Communicate the sort expression
        this.setSortExpression(sortExpr || undefined);
    }


    // Get values of row as an array of strings
    // Note: some values might be duplictated, but with different "formatting"
    getRowStrings(item, index, dataManager) {
        return item.hasOwnProperty('$groupKey')
            ? [item.$groupKey.toString()]
            : this._rowDataAccessor.reduce((a, f) => {
                const v = f(item, index, dataManager);
                if (typeof v === 'string') {
                    a.push(v); // Column produces a single string
                } else if (Array.isArray(v)) {
                    v.forEach(s => a.push(s)); // Column produces multiple strings
                } else if (v) {
                    a.push(v.toString());
                }
                return a;
            }, []);
    }
}
