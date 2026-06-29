import {LitElement, html, css} from 'lit';
import {ifDefined} from 'lit/directives/if-defined.js';
import {L2Pw} from 'ptcs-library/library-lit.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import {delegateToPrev, delegateToNext, getFocusable} from 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import {setTooltipByFocus} from 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-v-scroller/ptcs-v-scroller2.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-div/ptcs-div.js';
import './ptcs-edit-grid-cells.js';
import {columnName} from './grid-view.js';
import {ValueManager} from './grid-values.js';
import {uiText} from './grid-view/gv-text.js';
import {replaceOperationTokens, replaceLocalizationTokens} from 'ptcs-library/library-grid.js';

const preventScroll = {preventScroll: true};

// Default minimum column width as specified by UX (for fr units). Same value for all themes
const defaultColMinW = '75px';

// Specified baseIndex when adding a new row
const newRowBaseIndex = -1;

// Caching disabled value
const disabledField = Symbol('disabled');

// For silently adding validation information to cell elements
const validationField = Symbol('validation');

// validationErrorIcon should default to cds:icon_error
const defaultErrorIcon = 'cds:icon_error';

// The (deeply) slotted row - the actual row element
const slottedRow = Symbol('slottedRow');

const setOfLabels = new Set([
    'cancelButtonText', 'dateLabel', 'monthLabel', 'yearLabel', 'hoursLabel', 'minutesLabel', 'secondsLabel', 'meridiemLabel', 'selectLabel',
    'cancelLabel', 'parentLabel', 'noParentLabel'
]);

const rowEl = el => el[slottedRow] || el;

// Decode navigation mode
const Nav = {grid: 0, rowFirst: 1, cellFirst: 2, cellOnly: 3};

const _navigate = {
    false: {},
    true:  {
        'row-first':  Nav.rowFirst,
        'cell-first': Nav.cellFirst,
        'cell-only':  Nav.cellOnly
    }
};

const _navigateDflt = {false: Nav.cellOnly, true: Nav.rowFirst};

const nav = (treeMode, navMode) => {
    return _navigate[treeMode][navMode] || _navigateDflt[treeMode];
};

// Clone cell of selection element and turn off keyboard navigation on the clone
function cloneSelectionCell(el) {
    if (!el) {
        return el;
    }
    const cell = el.closest('.cell').cloneNode(true);
    [...cell.querySelectorAll('ptcs-checkbox')].forEach(e => {
        e.noTabindex = true;
    });
    return cell;
}

// Decode '3px' to [3, 'px']
const decode = w => {
    if (!w) {
        return undefined;
    }
    const m = /^(\d*(\.\d*)?)([\w%]*)$/.exec(w);
    return m && [+m[1], m[3] || 'px'];
};


PTCS.CoreGrid = class extends PTCS.BehaviorTooltip(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement)))) {
    /* eslint-disable max-len */
    static get styles() {
        return css`
        :host {
            box-sizing: border-box;
            display: flex;
            flex-direction: column;
            flex-wrap: nowrap;
            justify-content: space-between;
            align-items: stretch;

            position: relative;

            /* If the container doesn't limit the height, then negotiate height with scroller */
            height: var(--ptcs-core-grid-height, var(--ptcs-core-grid-max-height, 750px));

            --ptcs-tooltip-start-delay: 100;

            /* Don't show horizontal scroller when the resizer comes out of the grid */
            overflow:hidden;
        }

        #chunker {
            flex: 1 1 auto;
            box-sizing: border-box;
        }

        #header {
            grid-template-columns: var(--ptcs-grid-columns) var(--vssbw, 0px);
        }

        .row {
            display: grid;
            grid-template-columns: var(--ptcs-grid-columns);
            box-sizing: border-box;
            top: 0;
            transition: top 250ms;
        }

        .row .cell {
            overflow: hidden;
        }

        [part=header] {
            flex: 0 0 auto;
            overflow: hidden;
            box-sizing: border-box;
            max-width: 100%;
        }

        [part=header][hidden] {
            display: none !important;
        }

        .cell {
            display: flex;
            box-sizing: border-box;
        }

        .cell[halign=left],
        .cell[header-halign=left] div {
            justify-content: flex-start;
        }

        .cell[halign=center],
        .cell[header-halign=center] div {
            justify-content: center;
        }

        .cell[halign=right],
        .cell[header-halign=right] div {
            justify-content: flex-end;
        }

        .cell[valign=top],
        .cell[header-valign=top] {
            align-items: flex-start;
        }

        .cell[valign=center],
        .cell[header-valign=center] {
            align-items: center;
        }

        .cell[valign=bottom],
        .cell[header-valign=bottom] {
            align-items: flex-end;
        }

        .cell[invalid] {
            justify-content: space-between !important;
        }

        ptcs-v-scroller2 {
            outline: none; /* No focus indication */
        }

        .reorder-indicator {
            display: none;
            position: absolute;
            top: 0;
            height: 100%;
            z-index: 10;
        }

        /* Resizer Base Styling */
        .resizer {
            display: none;
            position: absolute;
            height: 100%;
        }

        .resizer-handle {
            position: relative;
            top: 0;
            height: 100%;
        }

        #resizer {
            /* Hover Resizer should be on top of the grid and the second Focus Resizer */
            z-index: 100;
        }

        #resizer-handle {
            /* Resizer Handle should be on top of the first Hover Resizer */
            z-index: 200;
        }

        #resizer-focus {
            z-index: 10;
            display: none;
        }

        #resizer-focus[selected] {
            display: block;
        }

        :host(:focus-within) #resizer-focus[focused] {
            display: block;
        }

        #resizer-focus-handle {
            z-index: 20;
        }

        #resizer-focus-handle:focus {
            outline: var(--ptcs-focus-overlay--border-style, solid) var(--ptcs-focus-overlay--border-width, 2px) var(--ptcs-focus-overlay--border-color, #0094c8);
            outline-offset: calc(-1 * var(--ptcs-focus-overlay--border-width, 2px));
        }

        :host([hide-focus]) #resizer-focus-handle:focus {
            outline: none;
        }

        /* Only show a single edit control in a cell */
        ptcs-icon[part=invalid-icon] + [part=edit-control] {
            display: none;
        }

        [part=edit-control] {
            align-self: center;
        }

        ptcs-icon[part=edit-control] {
            cursor: pointer;
        }

        ptcs-link[part=edit-control] {
            min-width: min(var(--ptcs-edit-control-width, 0px), var(--ptcs-edit-control-min-width, 54px));
        }

        /* Dont show edit control in hover mode, unless cell is hovered or has focus */
        ptcs-v-scroller2[edit-visibility=cell] [part~=body-cell]:not(:hover):not([focus]) [part=edit-control] {
            opacity: 0;
        }
        ptcs-v-scroller2[edit-visibility=row] .row:not(:hover):not([focus]) [part=edit-control] {
            opacity: 0;
        }
        ptcs-v-scroller2:not([edit-visibility=always]):not(:focus-within):not(:hover) [part=edit-control] {
            opacity: 0;
        }
        ptcs-v-scroller2[edit-visibility=cell]:not(:focus-within) [part~=body-cell][focus]:not(:hover) [part=edit-control] {
            opacity: 0;
        }
        ptcs-v-scroller2[edit-visibility=row]:not(:focus-within) .row[focus]:not(:hover) [part=edit-control] {
            opacity: 0;
        }

        ptcs-v-scroller2:not([edit-visibility]) [part~=body-cell]:not([focus]):not(:hover) [part=edit-control] {
            opacity: 0;
        }

        [part=edit-control]:focus-within {
            opacity: 1 !important;
        }

        /* Internal actions that does not implement the focus behavior. Needs explicit styling */
        [style-focus]:focus-within {
            outline: var(--ptcs-focus-overlay--border-style, solid) var(--ptcs-focus-overlay--border-width, 2px) var(--ptcs-focus-overlay--border-color, #0094c8);
            border-radius: var(--ptcs-focus-overlay--border-radius, 2px);
            outline-offset: calc(-1 * var(--ptcs-focus-overlay--border-width, 2px));
        }

        :host([hide-focus]) [style-focus]:focus-within {
            outline: none;
        }

        :host([modal]) [part=header] {
            pointer-events: none;
        }

        :host([modal]) ptcs-v-scroller2 {
            pointer-events: none;
        }

        :host(:not([show-footer])) #footer {
            display: none;
        }

        :host(:not([show-header-row-in-footer])) #footer-header {
            display: none;
        }

        .footer {
            overflow: hidden;
            position: relative;
        }

        .footer-row {
            min-height: 34px;
        }

        .footer-cell {
            min-height: 34px;
            display: flex;
        }

        [part=tree-toggle-icon][disabled] {
            cursor: default !important;
        }

        .cell[valign=top] .tree-toggle {
            align-items: start;
        }

        .cell[valign=center] .tree-toggle {
           align-items: center;
        }

        .cell[valign=bottom] .tree-toggle {
            align-items: end;
        }

        [part=tree-toggle-icon]:not([grid-action], [loading]) {
            opacity: 0;
        }

        @keyframes spinner {
            from {transform: rotate(0deg);}
            to {transform: rotate(360deg);}
        }

        [part=tree-toggle-icon][loading] {
            animation-name: spinner;
            animation-duration: 750ms;
            animation-timing-function: linear;
            animation-iteration-count: infinite;
        }

        [part=change-badge] {
            visibility: hidden;
            position: absolute;
            left: 0px;
            flex: 0 0 auto;
            order: -1;
        }

        .new-row [part=change-badge] {
            visibility: visible;
        }

        .resolve:not([unresolved]) {
            display: contents;
        }

        :host([delayed-loading-indicator=bar]) .resolve[unresolved] {
            width: 100%;
            display: inline-flex;
        }

        :host([delayed-loading-indicator=pulse]) .resolve[unresolved] {
            width: 100%;
            display: flex;
            animation: animated-pulse 1500ms linear alternate infinite;
        }

        @keyframes animated-pulse {
            0% { background-color: var(--ptcs-delayed-color, #E4E7E9); }
            100% { background-color: var(--ptcs-delayed-color-alt, #BEC6CA); }
        }

        :host([delayed-loading-indicator=sweeping]) .resolve[unresolved] {
            width: 100%;
            display: flex;
            background-color: var(--ptcs-delayed-color, #E4E7E9);
            background-image: linear-gradient(to left, transparent, var(--ptcs-delayed-color-alt, #FFFFFF) 50%, transparent 100%);
            background-repeat: no-repeat;
            background-size: var(--ptcs-grid--spinner-bar-width, 300px);
            animation: animated-bar 2s linear infinite;
        }

        @keyframes animated-bar {
            0% { background-position: calc(-1 * var(--ptcs-grid--spinner-bar-width, 300px)) 0; }
            75% { background-position: calc(-1 * var(--ptcs-grid--spinner-bar-width, 300px)) 0; }
            100% { background-position: calc(100% + var(--ptcs-grid--spinner-bar-width, 300px)) 0; }
        }

        :host([delayed-loading-indicator=spinner]) [part=body-cell] [unresolved] {
            position: relative;
            width: var(--ptcs-delayed-size, 34px);
            height: var(--ptcs-delayed-size, 34px);
            aspect-ratio: 1;
        }

        :host([delayed-loading-indicator=spinner]) [part=body-cell] [unresolved]::before {
            content: "";
            position: absolute;
            inset: 0px;
            left: 0;
            right: 0;
            top: 0;
            bottom: 0;
            border-width: var(--ptcs-grid--spinner-border-width, 4px);
            border-radius: 50%;
            border-style: solid;
            border-color: var(--ptcs-delayed-color-alt, #E6E8EA);
        }

        :host([delayed-loading-indicator=spinner]) [part=body-cell] [unresolved]::after {
            content: "";
            position: absolute;
            inset: 0px;
            left: 0;
            right: 0;
            top: 0;
            bottom: 0;
            box-sizing: border-box;
            border-width: var(--ptcs-grid--spinner-border-width, 4px);
            border-radius: 50%;
            border-style: solid;
            border-color: var(--ptcs-delayed-color, #00890B);
            animation: spinner-1 1s infinite linear alternate, spinner-2 2s infinite linear;
        }

        @keyframes spinner-1 {
            0%    {clip-path: polygon(50% 50%,0       0,  50%   0%,  50%    0%, 50%    0%, 50%    0%, 50%    0% )}
            12.5% {clip-path: polygon(50% 50%,0       0,  50%   0%,  100%   0%, 100%   0%, 100%   0%, 100%   0% )}
            25%   {clip-path: polygon(50% 50%,0       0,  50%   0%,  100%   0%, 100% 100%, 100% 100%, 100% 100% )}
            50%   {clip-path: polygon(50% 50%,0       0,  50%   0%,  100%   0%, 100% 100%, 50%  100%, 0%   100% )}
            62.5% {clip-path: polygon(50% 50%,100%    0, 100%   0%,  100%   0%, 100% 100%, 50%  100%, 0%   100% )}
            75%   {clip-path: polygon(50% 50%,100% 100%, 100% 100%,  100% 100%, 100% 100%, 50%  100%, 0%   100% )}
            100%  {clip-path: polygon(50% 50%,50%  100%,  50% 100%,   50% 100%,  50% 100%, 50%  100%, 0%   100% )}
        }

        @keyframes spinner-2 {
            0%    {transform:scaleY(1)  rotate(0deg)}
            49.99%{transform:scaleY(1)  rotate(135deg)}
            50%   {transform:scaleY(-1) rotate(0deg)}
            100%  {transform:scaleY(-1) rotate(-135deg)}
        }`;
    }
    /* eslint-enable max-len */

    render() {
        return html`
        <div id="resizer" class="resizer" part=resizer>
            <div id="resizer-handle" class="resizer-handle" part=resizer-handle></div>
        </div>
        <div id="resizer-focus" class="resizer" part=resizer>
            <div id="resizer-focus-handle" class="resizer-handle" part=resizer-handle></div>
        </div>
        <div id="reorder-indicator" class="reorder-indicator" part=reorder-indicator></div>
        <div id="header" class="row" part="header" @dragstart=${this._onheaderdragstart} ?hidden=${this.hideHeader}
            @mousedown=${this._onDragStart} @touchstart=${this._onDragStart} @multi-select-changed=${this._onMultiSelectChanged}></div>
        <ptcs-v-scroller2 id="chunker" part="body"
            .recycleItemElement=${this.__recycleItemElement} .createItemElement=${this.__createItemElement}
            .removeItemElement=${this.__removeItemElement} @gap-changed=${this._gapChangedEv}
            @resized-width=${this._resizedChunkerWidth} @scroll-left-changed=${this._scrollLeftChanged}
            @mousemove=${this._mouseOverGrid} @mouseleave=${this._mouseLeaveGrid}
            @mousedown=${this._mouseDownOnGrid} @mouseup=${this._mouseUpOnGrid}
            @click=${this._clickOnGrid} @focused-item-updated=${this._chunkerFocusRowChanged}
            @edit-activated=${this._editActivated} edit-visibility=${ifDefined(this._editVisibility())}
            @repainted=${this._repaintedView} @unresolved-value=${this._unresolvedValue} @resolved-value=${this._resolvedValue}
            @edited-value=${this._editedValue1}></ptcs-v-scroller2>
        <div id="footer" part="footer" class="footer">
            <div id="footer-header" class="row footer-row"></div>
            <div class="footer-rows" id="footer-rows"></div>
        </div>`;
    }

    static get is() {
        return 'ptcs-core-grid';
    }

    static get properties() {
        return {
            disabled: {
                type:     Boolean,
                reflect:  true,
                observer: '_disabledChanged'
            },

            disableRow: {
                type: Object // property name or (item, baseIndex, dataManager) => disabled
            },

            disableChildRows: { // Only used in tree grid mode
                type: Boolean
            },

            _disableRow: {
                type:     Function,
                computed: '_computeDisableRow(disabled, disableRow, disableChildRows)',
                observer: '_disableRowChanged'
            },

            // Hide header
            hideHeader: {
                type:      Boolean,
                attribute: 'hide-header',
                reflect:   true
            },

            // Select row by clicking anywhere on the row
            selectRow: {
                type: Boolean
            },

            // Data Viewer / View Configurator
            view: {
                type:     Object,
                observer: '_viewChanged'
            },

            // Data Manager
            data: {
                type:     Object,
                observer: '_dataChanged'
            },

            // Is there a grid gap?
            _gap: {
                type:     Number,
                observer: '_gapChanged'
            },

            // Is any column a tree toggle?
            _treeToggle: {
                type: Boolean
            },

            highlightNewRows: {
                type:      Boolean,
                observer:  '_highlightNewRowsChanged',
                attribute: 'highlight-new-rows',
                reflect:   true
            },

            // How client specifies if columns should be resizable
            resizeColumns: {
                type: Boolean // true: resize columns, false: don't resize columns, undefined: only resize tree toggle
            },

            // If the component (internally) supports resizable columns (tree toggle columns should be resizable by default)
            _resizeColumns: {
                type:     Boolean,
                computed: '_computeResizeColumns(resizeColumns, _treeToggle)',
                observer: '_resizeColumnsChanged'
            },

            reorderColumns: {
                type:     Boolean,
                value:    false,
                observer: '_reorderColumnsChanged'
            },

            // Title for row editor, when editing a row
            rowEditFormTitle: {
                type: String
            },

            // Title for row editor, when adding a new row
            rowEditFormAddTitle: {
                type: String
            },

            // Label for "Update" button in row editor, when editing a row
            updateButtonText: {
                type: String
            },

            // Label for "Add" button in row editor, when adding a new row (different label for Update button)
            addButtonText: {
                type: String
            },

            // Label for "Apply" button in column reorder form
            applyButtonText: {
                type: String
            },

            // Label for "Cancel" buttons (in column reorder form and row editor)
            cancelButtonText: {
                type: String
            },

            // Calendar labels
            dateLabel: {
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

            selectLabel: {
                type: String
            },

            cancelLabel: {
                type: String
            },

            // The label  "Parent" in the edit form when adding an item in the tree grid
            parentLabel: {
                type: String
            },

            // The label used as the "Parent" value in the edit form when adding a root item
            noParentLabel: {
                type: String
            },

            // Hide validation error message (for edit components in inline editor)
            hideValidationError: {
                type: Boolean
            },

            // Hide validation criteria message (for edit components in inline editor)
            hideValidationCriteria: {
                type: Boolean
            },

            // Hide validation success message (edit components in inline editor)
            hideValidationSuccess: {
                type: Boolean
            },

            // Icon for validation error (edit components in inline editor AND in grid cells)
            validationErrorIcon: {
                type:     String,
                observer: '_validationErrorIconChanged'
            },

            // Icon for validation success (edit components in inline editor)
            validationSuccessIcon: {
                type: String
            },

            // Icon for validation criteria (edit components in inline editor)
            validationCriteriaIcon: {
                type: String
            },

            // Visibility of edit control when it occurs in cells: 'hover', 'always', 'never'
            editControlVisibility: {
                type:     String,
                observer: '_editControlVisibilityChanged'
            },

            inlineEditing: {
                type:     Boolean,
                observer: '_editControlVisibilityChanged' // Force rebuild when this option changes
            },

            // Is each cells editable, or only the whole row at once?
            _editCells: {
                type:     Boolean,
                observer: '_setHostEditVisibility'
            },

            // Scroll to selected item when the grid view is resized?
            autoScroll: {
                type: Boolean
            },

            navigation: {
                type: String // row-first (default), cell-first, cell-only
            },

            // Do we allow to move to the previous/next row when pressing left/right arrow key from the
            // first/last item on a row?
            preventFocusRowWrap: {
                type: Boolean
            },

            // In single select mode, should the focused row be selected?
            selectFollowsFocus: {
                type: Boolean
            },

            // Do the client want to show a slotted error message?
            showErrorMessage: {
                type:      Boolean,
                attribute: 'show-error-message',
                observer:  '_showErrorMessageChanged'
            },

            // Recycled rows
            _recycled: {
                type:  Map,
                value: () => new Map()
            },

            // The row with focus
            _focusedRow: {
                type:     Element,
                observer: '_focusedRowChanged'
            },

            // The cell with focus, in _focusedRow
            _focusedCell: {
                type:     Element,
                observer: '_focusedCellChanged'
            },

            // The action with focus, in _focusedCell, or this._resizerFocusHandle if the focus is on the column resizer
            _focusedAction: {
                type:     Element,
                observer: '_focusedActionChanged'
            },

            _resizedCell: {
                type:     Element,
                observer: '_resizedCellChanged'
            },

            // Dest index for dragged column
            _draggedDestIndex: {
                type: Number
            },

            _resizedFocusedCell: {
                type:     Element,
                observer: '_resizedFocusedCellChanged'
            },

            // Array of column widths: {minWidth, width, maxWidth}
            _colWidths: {
                type:  Array,
                value: () => []
            },

            _resizedColWidths: {
                type:  Array,
                value: () => []
            },

            __resizerHitArea: {
                value: 17
            },

            footerData: {
                type:     Array,
                value:    () => [],
                observer: '_rebuildFooter'
            },

            showFooter: {
                type:      Boolean,
                value:     false,
                attribute: 'show-footer',
                reflect:   true
            },

            showHeaderRowInFooter: {
                type:      Boolean,
                value:     false,
                attribute: 'show-header-row-in-footer',
                reflect:   true
            },

            // How to indicated delayed loading: 'bar', 'thin-bar', 'spinner' (or whatever)
            delayedLoadingIndicator: {
                type:      String,
                attribute: 'delayed-loading-indicator',
                reflect:   true
            },

            // The tooltip to show for cell during its delayed loading
            delayedLoadingTooltip: {
                type:      String,
                attribute: 'delayed-loading-tooltip'
            },

            // The tooltip to show for cell with a failed delayed loading
            delayedErrorTooltip: {
                type:      String,
                attribute: 'delayed-error-tooltip'
            },

            // Delayed loading debounce timeout (ms). Time that the viewport must have been stable before data viewport change event is fired
            delayedLoadingDebounce: {
                type:      Number,
                attribute: 'delayed-loading-debounce',
                observer:  '_delayedLoadingDebounceChanged'
            },

            // Do the slotted content contain a message for when the grid is empty?
            slottedMessage: {
                type:     Boolean,
                observer: '_slottedMessageChanged'
            },

            // Do the rows need to be slotted into the view? (Depends on vanilla CSS?)
            slottedRows: {
                type:     Boolean,
                observer: '_slottedRowsChanged'
            },

            // enable multi-selection
            shiftKeySelection: {
                type:      Boolean,
                attribute: 'shift-key-selection',
                observer:  '_shiftKeySelectionChanged'
            },

            _shiftKey: {
                type:      Boolean,
                value:     false,
                attribute: 'aria-multiselectable',
                converter: {
                    toAttribute(value) {
                        return value ? 'true' : 'false';
                    }
                },
                reflect: true
            },

            _messageEl: Element
        };
    }

    constructor() {
        super();

        this.__mouseOverHeader = this.__mouseOverHeaderEv.bind(this);
        this.__editedValue = this._editedValue.bind(this);
        this.__checkIfMouseLeftResizedCell = this.__checkIfMouseLeftResizedCellEv.bind(this);
        this.__resize = this.__resizeEv.bind(this);
        this.__stopResize = this.__stopResizeEv.bind(this);
        this.__resizeMouseUp = this.__resizeMouseUpEv.bind(this);
        this.__recycleItemElement = this._recycleGridRow.bind(this);
        this.__removeItemElement = this._unslotRow.bind(this);
        this.__createItemElement = this._createGridRow.bind(this);
        this.__slotNo = 0;

        this._newRows = new WeakSet();
    }

    ready() {
        super.ready();

        this._resizerFocusHandle = this.$['resizer-focus-handle'];

        if (this.editControlVisibility === undefined) {
            this.editControlVisibility = 'hover';
        }

        // Column sorting
        this.shadowRoot.addEventListener('sort-action', ev => this._sortActionEv(ev), {passive: true});

        // bulk-select
        this.shadowRoot.addEventListener('bulk-select-changed', this._onbulkSelectChanged.bind(this));

        this.addEventListener('keydown', ev => this._keyDown(ev));
        this.addEventListener('keyup', ev => this._keyUp(ev));

        this.shadowRoot.addEventListener('mousedown', ev => this._mouseDown(ev));
        this.shadowRoot.addEventListener('touchstart', ev => this._mouseDown(ev, true), {passive: true});
        this.shadowRoot.addEventListener('mousemove', ev => this._mouseTooltip(ev));
        this.shadowRoot.addEventListener('multiple-selection-changed', ev => this._multiSelectionChanged(ev));

        this.__resizeCall = true;

        // If no data manager is attached within 500ms, show an "empty message"
        if (!this.data) {
            setTimeout(() => !this.data && this._setDataLength(0), 500);
        }

        if (this.disableRow === undefined) {
            this.disableRow = null; // Force computation of _disableRow
        }
    }

    connectedCallback() {
        super.connectedCallback();

        if (this.__duplicateStylingEv) {
            document.removeEventListener('style-aggregator', this.__duplicateStylingEv); // Better safe than sorry
            document.addEventListener('style-aggregator', this.__duplicateStylingEv);
        }
        this.__parentEl = this.mainGrid;
        this.__parentEl.addEventListener('edited-value', this.__editedValue);
    }

    disconnectedCallback() {
        this._editDone({}, false); // Just in case the inline editor is still open

        if (this.__duplicateStylingEv) {
            document.removeEventListener('style-aggregator', this.__duplicateStylingEv);
        }
        this.__parentEl.removeEventListener('edited-value', this.__editedValue);

        super.disconnectedCallback();
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        // eslint-disable-next-line max-len
        if (this._editCells && this.editControlVisibility === 'never' && this.inlineEditing && [...changedProperties.keys()].some(k => setOfLabels.has(k)) && this.view && this.view.$i18n === this) {
            // Force i18n label reload (yes, this is somewhat hacky...)
            this.view.$i18n = undefined;
            this.view.$i18n = this;
        }
    }

    // Hack that presumes that this component always resides inside another component (a PTCS-GRID)
    get mainGrid() {
        return this.getRootNode().host;
    }

    // Element where the actual rows resides (at some level)
    get rowContainer() {
        return this.__slottedRows ? this.mainGrid : this.$.chunker;
    }

    // Find element in chunkner that slots in the row. Only differs from row when slottedRows
    _shadowRow(row) {
        try {
            return this.__slottedRows ? row.assignedSlot.parentElement.assignedSlot.parentElement : row;
        } catch (err) {
            return row;
        }
    }

    _rowIndex(row) {
        return this._shadowRow(row).index; // The index is on the (shadow) row (top-most item element) in the chunker
    }

    _editVisibility() {
        switch (this.editControlVisibility) {
            case 'hover':
                return this._editCells ? 'cell' : 'row';
            case 'always':
                return 'always';
            default:
                return undefined;
        }
    }

    _isResizable(cell) {
        if (!cell) {
            return false;
        }
        if (this.resizeColumns) {
            return !cell.hasAttribute('non-resizable');
        }
        // Tree toggle columns are resizable unless the client explicitly says they should not be
        return this.resizeColumns !== false && cell.hasAttribute('tree-toggle');
    }

    _rebuildFooter() {
        cancelAnimationFrame(this.__rebuildFooterAnimationFrame);

        this.__rebuildFooterAnimationFrame = requestAnimationFrame(() => {
            this.__rebuildFooter();
        });
    }

    __rebuildFooter() {
        this.$['footer-rows'].innerHTML = '';
        this.$['footer-header'].innerHTML = '';

        if (!this.view || !this.view.columns || !this.data || this.data.length === 0) {
            return;
        }

        const cellF = uiText({});

        const columns = this.view.columns;
        const maxHeightHeader = this.view.maxHeightHeader;

        // Create footer header
        columns.forEach(def => {
            if (def.hidden) {
                return;
            }

            const cell = document.createElement('div');
            cell.setAttribute('part', 'footer-cell');
            cell.classList.add('cell');

            if (def.name) {
                if (def.headerHAlign) {
                    cell.setAttribute('header-halign', def.headerHAlign);
                }
                if (def.headerVAlign) {
                    cell.setAttribute('header-valign', def.headerVAlign);
                }
                if (maxHeightHeader > 0) {
                    cell.style.maxHeight = maxHeightHeader + 'px';
                    cell.style.overflow = 'hidden';
                }

                if (typeof def.label === 'function') {
                    cell.appendChild(def.label(this.data, this.view, cell, {noActions: true}));
                }
            }

            this.$['footer-header'].appendChild(cell);
        });

        this.footerData.forEach(item => {
            const row = document.createElement('div');
            row.classList.add('footer-row');
            row.classList.add('row');

            let hiddenCount = 0;

            let cell;

            columns.forEach((def, i) => {
                if (!def.name) {
                    // I skip columns without a name. This way I also skip "functional" columns like selection or show row numbers.
                    return;
                }

                const key = def.name;

                if (def.hidden) {
                    hiddenCount++;
                }

                if (item.hasOwnProperty(key) && item[key] !== '#cspan' && !def.hidden) {
                    cell = document.createElement('div');
                    cell.setAttribute('part', 'footer-cell');
                    cell.classList.add('footer-cell');
                    cell.classList.add('cell');

                    cell.style['grid-column-start'] = i + 1 - hiddenCount;

                    let cellContent = item[key];
                    let align = 'left';
                    cellContent = cellContent.replace(/,\s*text-align:\s*(.*);/, (match, p1) => {
                        align = p1;
                        return '';
                    });

                    cellContent = replaceLocalizationTokens(cellContent, def);
                    cellContent = replaceOperationTokens(cellContent, def, this.data);

                    cell.appendChild(cellF.create(cell));
                    // eslint-disable-next-line no-nested-ternary
                    cell.style['justify-content'] = align === 'right' ? 'flex-end' : (align === 'center' ? 'center' : 'flex-start');

                    cellF.assign(cell.firstChild, cellContent);

                    row.appendChild(cell);

                    if (cell.previousSibling) {
                        cell.previousSibling.style['grid-column-end'] = i + 1 - hiddenCount;
                    }
                }
            });

            if (cell) {
                cell.style['grid-column-end'] = columns.length + 1 - hiddenCount;
            }

            this.$['footer-rows'].appendChild(row);
        });
    }

    _resetScrollbars() {
        const chunker = this.$.chunker;

        if (!chunker || !chunker.elScroll) {
            return;
        }

        chunker.elScroll.scrollTop = 0;
        chunker.elScroll.scrollLeft = 0;
    }

    // The client tells me that I have been resized
    adjustView(retainWidth) {
        if (this.view && this.view.getWidthsExpression(false)) {
            // Not the default column widths
            if (!(retainWidth || this.__resizing)) {
                this.view.setWidthsExpression(); // Reset widths
            }
        } else {
            requestAnimationFrame(this._adjustMinMaxColumnWidths.bind(this));
        }

        this._autoScroll();
        this._adjustEditorPlace();
    }

    // Load the focusable action elements in the cell
    _cellActions(cell) {
        // Find visible grid actions
        const r = [...cell.querySelectorAll('[grid-action]')].filter(el => el.offsetParent !== null);
        return r.length > 0 && r;
    }

    // Return default cell action (single action that will not interfere with keyboard navigation)
    _defaultCellAction(cellActions) {
        if (cellActions && cellActions.length === 1) {
            const ga = cellActions[0].getAttribute('grid-action');
            if (ga === '' || ga.split(' ').every(mode => mode !== 'updown' && mode !== 'tab')) {
                return cellActions[0];
            }
        }
        return null;
    }

    // Track focused row
    _focusedRowChanged(_focusedRow, old) {
        console.assert(!(_focusedRow && _focusedRow[slottedRow])); // Never focus on the slot container row
        if (old) {
            old.removeAttribute('focus');
        }
        if (_focusedRow) {
            this._focusedRow.setAttribute('focus', '');

            // Select follows Focus?
            if (this.selectFollowsFocus && this.data.selectMethod === 'single' && this._rowIndex(_focusedRow) >= 0) {
                this.data.select(this.data.baseIndex(this._rowIndex(_focusedRow)), true);
            }
        }
    }

    // Track focused cell
    _focusedCellChanged(_focusedCell, old) {
        this._closeTooltip();
        if (old) {
            old.removeAttribute('focus');
        }
        if (_focusedCell) {
            // Track focus on celpl level
            _focusedCell.setAttribute('focus', '');

            // Scroll cell into view
            const bb0 = this.$.chunker.elScroll.getBoundingClientRect();
            const bb = _focusedCell.getBoundingClientRect();

            if (bb.left < bb0.left) {
                this.$.chunker.elScroll.scrollLeft += bb.left - bb0.left - 16;
            } else if (bb.right > bb0.right) {
                this.$.chunker.elScroll.scrollLeft += bb.right - bb0.right + 16;
            }

            const unresolvedEl = _focusedCell.querySelector('div.resolve:is([unresolved], [error])');
            if (unresolvedEl) {
                _focusedCell.tooltip = unresolvedEl.hasAttribute('error') ? this.delayedErrorTooltip : this.delayedLoadingTooltip;
            }

            this.__tooltipEl = _focusedCell;
            this._tooltipEnter(this.__tooltipEl, bb.left + (bb.width / 2), bb.y, undefined, {showAnyway: true});
        }
    }

    // The activeElement of the shadow dom - plus any focused element in the slotted rows (which are visible inside this shadow dom)
    get _activeElement() {
        if (!this.__slottedRows) {
            return this.shadowRoot.activeElement;
        }

        // Using slotted rows. Focus got more complicated
        const parent = this.mainGrid;
        for (let ae = document.activeElement; ae; ae = ae.shadowRoot && ae.shadowRoot.activeElement) {
            if (ae === parent) {
                // The (parent) grid has focus-within. We therefore want the activeElement of us, if we are active
                return ae.shadowRoot.activeElement === this && this.shadowRoot.activeElement;
            }

            if (parent.contains(ae)) {
                // Focus is on an element inside the parents light dom. It should be a cell action element...
                return ae;
            }
        }
        return undefined;
    }

    _focusWithin() {
        return PTCS.hasFocus(this) || (this.__slottedRows && this._activeElement);
    }

    // Move focus between grid and sub-action
    _focusedActionChanged(_focusedAction, old) {
        if (old === this._resizerFocusHandle) {
            this.$['resizer-focus'].removeAttribute('selected');
            this.$['resizer-focus'].removeAttribute('focused');
            this._resizedFocusedCell = null;
        }
        if (_focusedAction) {
            _focusedAction.noTabindex = false;
            _focusedAction.setAttribute('tabindex', '0'); // tabindex must be 0 for focus delegation to work (e.g. datepicker)

            if (_focusedAction === this._resizerFocusHandle) {
                this._updateResizerPositions();
                this.$['resizer-focus'].setAttribute('focused', '');
            }

            requestAnimationFrame(() => {
                // Refocus on action - if grid still has focus
                if (this._focusedAction === _focusedAction && this._focusWithin() && this._activeElement !== _focusedAction) {
                    _focusedAction.focus(preventScroll);
                    setTooltipByFocus(_focusedAction);
                }
                _focusedAction.setAttribute('tabindex', '-1'); // tabindex must be -1 to prevent automatic Tab focus
            });
        } else if (this._activeElement) {
            // Move focus from action element to grid
            this.focus(preventScroll);
            this._trackMyFocus(); // Need to bump focus behavior (since it won't get a focus event)
            setTooltipByFocus(this);
        }
    }


    // Set focus on row / cell / action
    _setFocus(row, cell, actionCandidate) {
        console.assert(!row || ['row', 'header'].indexOf(row.getAttribute('part') >= 0), row);
        console.assert(!cell || (cell.parentNode === row && cell.classList.contains('cell')), cell);

        if (!row) {
            this._resetFocus();
            return;
        }

        // Are there any action elements in the cell?
        const cellActions = actionCandidate && cell && this._cellActions(cell);

        // Focus on internal cell action?
        let el;
        if (cellActions) {
            if (actionCandidate instanceof Element) {
                el = cellActions.find(e => e === actionCandidate || e.contains(actionCandidate));
            }
            if (!el) {
                // actionCandidate failed: do the cell contain a single element that can be navigated with the arrow keys?
                el = this._defaultCellAction(cellActions);
            }
        }

        this.setProperties({_focusedRow: row, _focusedCell: cell, _focusedAction: el});
    }

    // Set focus on this._resizerFocusHandle
    _setFocusResizer(resizedCell) {
        this._focusedRow = this.$.header;
        this._focusedCell = this.$['resizer-handle'];
        if (this._focusedAction === this._resizerFocusHandle) {
            this._focusedAction = undefined; // Need a change event, because position may have changed
        }
        this._focusedAction = this._resizerFocusHandle;
        this._resizedFocusedCell = resizedCell;
    }

    // Reset focus, but remember selected column - if applicable
    _resetFocus() {
        if (this._focusedCell) {
            this._focusChildNo = PTCS.getChildIndex(this._focusedCell);
        }

        // Reset focus
        this._focusedRow = undefined;
        this._focusedCell = undefined;
        this._focusedAction = undefined;
    }

    _getChunkerFocusRow() {
        // The "empty message" item should never get focus
        const row = this.$.chunker.getFocusRow();
        return (row && row.hasOwnProperty('index') && rowEl(row)) || undefined;
    }

    _chunkerFocusRowChanged() {
        if (this._focusedCell) {
            const focusRow = this._getChunkerFocusRow();
            if (!focusRow || !focusRow.contains(this._focusedCell)) {
                this._resetFocus();
            }
        }
    }

    _getFocus() {
        // Is the focus on the column resizer?
        if (this._focusedAction === this._resizerFocusHandle) {
            return this._focusedAction;
        }

        // Is focus in the header?
        if (this._focusedRow === this.$.header) {
            return this._focusedAction || this._focusedCell || this._focusedRow;
        }

        const focusRow = this._getChunkerFocusRow();
        if (focusRow !== this._focusedRow) {
            const cell = focusRow && (focusRow.children[this._focusChildNo] || focusRow.firstChild);
            switch (this._navigation) {
                case Nav.cellFirst:
                case Nav.cellOnly:
                    this._setFocus(focusRow, cell, true);
                    break;

                case Nav.rowFirst:
                    this._setFocus(focusRow);
                    break;
            }
        }

        return this._focusedAction || this._focusedCell || this._focusedRow;
    }

    _scrollFocusedRowIntoView(noScroll) {
        if (this.data && this.data.length) {
            // Scroll focused row into sight
            const fi = this.$.chunker.focusedItemIndex;
            if (!(fi >= 0)) {
                this.$.chunker.setFocusRowIndex(0); // No row had focus. Assign focus to first row
            } else if (!noScroll) {
                this.$.chunker.scrollTo(fi);
            }
        }
    }

    _notifyFocus() {
        // Delegate focus to focusable sub-part
        if (!this._getFocus()) {

            // No element has focus. Try to focus / refocus on row
            this._scrollFocusedRowIntoView(true);

            if (!this._getFocus()) {
                // Unable to focus on a grid row. Try to focus on something in the header
                for (let focus = this.$.header.firstElementChild; focus; focus = focus.nextSibling) {
                    // Focus on header cell if it has actions
                    const cellActions = this._cellActions(focus);
                    if (cellActions) {
                        this._setFocus(this.$.header, focus, this._defaultCellAction(cellActions));
                        return;
                    }

                    // Focus on header resizer if resizing is enabled
                    if (this._isResizable(focus)) {
                        this._setFocusResizer(focus);
                        return;
                    }
                }
            }
        }
        // Make sure the focus action, if any, actually has the focus
        if (this._focusedAction && this._activeElement !== this._focusedAction) {
            this._focusedAction.focus(preventScroll);
        }
    }

    _notifyBlur() {
        this._closeTooltip();
    }

    _initTrackFocus() {
        // If activeElement, then an action that doesn't use the focus behavior curently has focus. It will render the focus itself.
        this._trackFocus(this, () => this._activeElement ? null : this._getFocus());
    }

    _getGridCell(ev) {
        return ev.target.closest('.cell');
    }

    _getVisibleColDef(colNo) {
        const columns = this.view.columns;
        for (let i = 0; i < columns.length; i++) {
            if (!columns[i].hidden) {
                if (colNo-- === 0) {
                    return columns[i];
                }
            }
        }
        return null;
    }

    _mouseDown(ev, touch = false) {
        if (ev.defaultPrevented || PTCS.wrongMouseButton(ev)) {
            return;
        }

        if (touch && this._resizeColumns) {
            this.__setResizedCellEv(ev);
        }

        if (this._resizedCell) {
            const cellR = this._resizedCell.getBoundingClientRect();
            const posX = PTCS.getCoordinatesFromEvent(ev).posX;

            if (!(posX < cellR.right - this.__resizerHitArea || posX > cellR.right + this.__resizerHitArea)) {
                // Now we are entering the resizing mode so any other default browser actions should be disabled.
                // If not e.g. all the header cells become selected during the resizing.
                ev.preventDefault();

                const mouseMoveEv = touch ? 'touchmove' : 'mousemove';
                const mouseUpEv = touch ? 'touchend' : 'mouseup';

                window.addEventListener(mouseMoveEv, this.__resize);
                window.addEventListener(mouseUpEv, this.__stopResize);

                this.__resizing = true;

                this.$.resizer.setAttribute('selected', '');

                // When you click on hover resizer remove the focus from the focus resizer
                this.blur();
                this.$['resizer-focus'].removeAttribute('selected');
                this.$['resizer-focus'].style.display = 'none'; // Hide "focus resizer" while mouse is dragging "mouse resizer"

                return;
            }
        }

        const cell = this._getGridCell(ev);
        if (!cell) {
            return;
        }

        if (this.$.header.contains(cell) && !this._cellActions(cell)) {
            // Can't focus on header cell, because it doesn't have a focusable element
            return;
        }

        if (this.$.header === cell.parentNode) {
            // Clicked on header
            this._setFocus(cell.parentNode, cell, ev.target);
        } else {
            // Clicked on grid
            switch (this._navigation) {
                case Nav.cellFirst:
                case Nav.cellOnly:
                    this._setFocus(cell.parentNode, cell, ev.target);
                    break;

                case Nav.rowFirst:
                    // The mouse can only select rows
                    this._setFocus(cell.parentNode);
                    break;
            }
        }
    }

    _unslotRow(el) {
        const row = el[slottedRow];
        if (!row) {
            return el; // Not slotted
        }
        const link = row.assignedSlot.parentElement;
        el[slottedRow] = undefined;
        link.remove();
        row.remove();
        return row;
    }

    _recycleGridRow(el) {
        el = this._unslotRow(el);
        const reg = this._recycled.get(el.__rows);
        if (reg) {
            reg.push(el);
            console.assert(reg.length < 50, 'there are many elements in the recycle bin');
        } else if (el.__rows) {
            this._recycled.set(el.__rows, [el]);
        }
    }

    _fallbackRow() {
        // Temporary row, while waiting for a proper view configuration
        const el = document.createElement('div');
        el.style.height = '1000px';
        el.__rows = this;
        return el;
    }

    // Set boolean attribute
    _setbattr(el, attr, value) {
        if (value) {
            el.setAttribute(attr, '');
        } else {
            el.removeAttribute(attr);
        }
    }

    _isInlineEditing(cell) {
        if (!this.inlineEditing || this.editControlVisibility !== 'never') {
            return false;
        }
        const colDef = this._getVisibleColDef(PTCS.getChildIndex(cell));
        return !colDef.externalEdit;
    }


    _insertErrorIcon(cell, tooltip) {
        if (cell[validationField]) {
            // Replace tooltip on existing error icon
            cell.querySelector(':scope > [part=invalid-icon]').tooltip = tooltip;
        } else {
            const icon = document.createElement('ptcs-icon');
            icon.setAttribute('part', 'invalid-icon');
            icon.setAttribute('grid-action', '');
            icon.setAttribute('style-focus', ''); // Need help with focus styling
            icon.icon = this.validationErrorIcon || defaultErrorIcon;
            icon.size = 'small';
            icon.tooltip = tooltip;
            if (this._editing && this.view.editLevel !== 'row' && !this._isInlineEditing(cell)) {
                icon.style.cursor = 'pointer';
                icon.addEventListener('click', ev => this._editActivated(ev));
                icon.addEventListener('keydown', ev => (ev.key === ' ' || ev.key === 'Enter') && ev.target.click());
            }
            cell.insertBefore(icon, cell.firstChild.nextSibling); // Make sure any edit controls are after the error icon
        }
        cell.setAttribute('invalid', '');
        cell[validationField] = tooltip;
        this._focusedAction = this.view.editLevel === 'row' ? this._focusedAction : this._defaultCellAction(this._cellActions(cell));
    }

    _removeErrorIcon(cell) {
        cell.removeAttribute('invalid');
        cell[validationField] = undefined;
        cell.removeChild(cell.querySelector(':scope > [part=invalid-icon]'));
        this._focusedAction = this.view.editLevel === 'row' ? this._focusedAction : this._defaultCellAction(this._cellActions(cell));
    }

    _emptyMessageEl() {
        if (!this.__emptyMessageEl) {
            this.__emptyMessageEl = document.createElement('div');
            this.__emptyMessageEl.setAttribute('class', 'row');
            this.__emptyMessageEl.appendChild(document.createElement('slot'));
        }
        return this.__emptyMessageEl;
    }

    // Callback from scroller to create row element
    _createGridRow(index, el) {
        if (!this.data || this.data.length === 0 || this.showErrorMessage) {
            return this._emptyMessageEl();
        }

        const item = this.data.item(index);
        console.assert(item, `Invalid index: ${index}`);

        const rows = this.view ? this.view.getRowDef(item) : undefined;
        if (!rows) {
            return this._fallbackRow();
        }

        const rowDepField = this.view.rowDepField;

        // Create element
        if (!el || el.__rows !== rows) {
            el  = (this._recycled.get(rows) || []).pop();
            if (!el) {
                const minHeightRow = this.view.minHeightRow;
                const maxHeightRow = this.view.maxHeightRow;
                el = PTCS.createElement('ptcs-div', {class: 'row', part: 'row'});
                el.__rows = rows;

                if (rows) {
                    const inlineEditing = this.inlineEditing && this._editCells && this.editControlVisibility === 'never';
                    const frag = document.createDocumentFragment();

                    rows.forEach(def => {
                        if (def.hidden) {
                            return;
                        }

                        const cell = document.createElement('ptcs-div');
                        cell.value = def.select(item, index); // Start with the correct value, so the state manager can attach
                        cell.setAttribute('class', 'cell');

                        // Propagate row state formatting to the cell only if it doesn't define its own state formatting
                        cell.setAttribute('part', `body-cell${rowDepField && !def.depcolumn ? ' state-value' : ''}`);

                        // Apply column state formatting in case dependent column is defined
                        if (def.depcolumn) {
                            cell.setAttribute('state-key', def.name);
                        }

                        if (def.halign) {
                            cell.setAttribute('halign', def.halign);
                        }
                        if (def.valign) {
                            // Unfortunately, Thingworx specifies middle instead of center. Now need to support both to be backwards compatible
                            const valign = def.valign === 'middle' ? 'center' : def.valign;
                            cell.setAttribute('valign', valign);
                        }

                        if (minHeightRow > 0) {
                            cell.style.minHeight = `${minHeightRow}px`;
                        }
                        if (maxHeightRow > 0) {
                            cell.style.maxHeight = `${maxHeightRow}px`;
                            cell.style.overflow = 'hidden';
                        }

                        cell.appendChild(def.create(cell, def.editable && inlineEditing));

                        if (def.editable && this._editCells && (this.editControlVisibility !== 'never' || (def.externalEdit && inlineEditing))) {
                            const editControl = this.view.createEditControl();
                            if (editControl !== null) {
                                cell.style.justifyContent = 'space-between';
                                cell.appendChild(editControl);
                            }
                        }
                        frag.appendChild(cell);
                    });

                    el.appendChild(frag);
                }

                // Did we get our first body-cell row-selection-checkbox?
                if (this.view.selectMethod === 'multiple' && this.__watchSelectionColumnWidth$ && !this.__watchSelectionColumnWidth$.el2) {
                    const el2 = cloneSelectionCell(el.querySelector('[part~=row-selection-checkbox]'));
                    if (el2) {
                        this.__watchSelectionColumnWidth$.el2 = el2;
                        this.__watchSelectionColumnWidth$.el.appendChild(el2);
                    }
                }
            }

            if (this.__slottedRows) {
                const slot = `r${this.__slotNo++}`;

                el.setAttribute('slot', slot);
                this.rowContainer.appendChild(el);

                // slot el in parent to el in this (light dom)
                const link1 = PTCS.createElement('div', {slot, class: 'slot'});
                link1.appendChild(PTCS.createElement('slot', {name: slot}));
                this.appendChild(link1);

                // slot link1 in light dom to link2 in shadow dom
                const link2 = PTCS.createElement('div', {slot, class: 'slot', part: 'row'});
                link2.appendChild(PTCS.createElement('slot', {name: slot}));
                link2[slottedRow] = el;

                el = link2; // This is what the chunker gets
                el.__rows = rows;
            }

            if (this.view.valueManager.columnIds.length > 0) {
                this._unresolvedValue(); // Fake call, because we might have missed events
            }
        }

        // Bind element to data
        const oldIndex = el.index;
        el.index = index;
        const disabled = this._disabledRow(index);
        const cells = rowEl(el).children;
        if (rows) {
            const updated = this.data.updatedItem(index);
            const validation = updated && updated.$validation;

            let hiddenCount = 0;
            rows.forEach((def, colix) => {
                if (def.hidden) {
                    hiddenCount++;
                    return;
                }

                const cell = cells[colix - hiddenCount]; // Problem with spanning cells!
                if (cell) {
                    const value = def.select(item, index);
                    cell.value = value;

                    // Put row state formatting on the cell
                    if (rowDepField) {
                        cell._depfield = item[rowDepField];

                        if (typeof cell.firstChild.assignGridStateData === 'function') {
                            cell.firstChild.assignGridStateData('row', item[rowDepField]);
                        } else {
                            cell.firstChild._depfield = item[rowDepField];
                        }
                    }

                    // Apply column state formatting in case dependent column is defined
                    if (def.depcolumn) {
                        cell._depcolumn = item[def.depcolumn];

                        if (typeof cell.firstChild.assignGridStateData === 'function') {
                            cell.firstChild.assignGridStateData('column', item[def.depcolumn], def.name);
                        } else {
                            cell.firstChild.setAttribute('state-key', def.name);
                            cell.firstChild._depcolumn = item[def.depcolumn];
                        }
                    }

                    // Dirty state and validation? (Only used in editing mode)
                    if (def.editable) {
                        this._setbattr(cell, 'dirty',
                            updated && updated.hasOwnProperty(def.editable) && updated[def.editable] !== item[def.editable]);

                        const invalid = validation && validation[def.editable];
                        if (cell[validationField] !== (invalid || undefined)) {
                            if (invalid) {
                                this._insertErrorIcon(cell, invalid);
                            } else {
                                this._removeErrorIcon(cell);
                            }
                        }
                    }
                    this._disableCell(cell, disabled);

                    def.assign(cell.firstChild, (def.render && value !== ValueManager.NaV) ? def.render(item, index) : value, index, this.data);
                }
            });
        }

        // Set selection mode
        this._setbattr(el, 'selected', this.data.isSelected(index));
        this._setbattr(el, 'next-row-selected', this.data.isSelected(index + 1));

        // Cell Markers (first, last, alt)
        this._setbattr(el, 'first', index === 0);
        this._setbattr(el, 'last', index + 1 === this.data.length);
        if (this.data._subItems && this.data._subItemsState) {
            const baseIndex = this.data.baseIndex(index);
            const baseLevel = this.data.baseLevel(baseIndex);
            this._setbattr(el, 'alt', baseLevel % 2 === 1);
        } else {
            this._setbattr(el, 'alt', index % 2 === 1);
        }

        // Put state formatting on the row
        if (rowDepField) {
            el._depfield = item[rowDepField];
        }

        // Needed for performance when updating selected items
        this._selMap = null; // Visible items has changed

        // If the row element is reused, and the old row has an action item with focus...
        if (this._focusedAction && this._focusedCell && this._focusedCell.parentNode === el && oldIndex !== el.index) {
            requestAnimationFrame(() => this._resetFocus());
        }

        if (this.highlightNewRows) {
            if (this._newRows.has(item)) {
                rowEl(el).classList.add('new-row');
            } else {
                rowEl(el).classList.remove('new-row');
            }
        }

        return el;
    }

    _disableCell(el, isDisabled) {
        // Quick filter
        if (!!el[disabledField] === !!isDisabled) {
            return;
        }
        const disabled = !!isDisabled;
        el[disabledField] = disabled;

        for (const child of el.children) {
            if (typeof child.performGridDisable === 'function') {
                // Custom handling of disabling grid actions
                child.performGridDisable(disabled);
            } else {
                // Mark mode with disabled attribute (which should propagate to disabled property)
                PTCS.setbattr(child, 'disabled', disabled);
            }
        }
    }

    _disabledChanged(disabled) {
        for (let el = this.$.header.firstChild; el; el = el.nextSibling) {
            this._disableCell(el, disabled);
        }
    }

    _disabledRowFunction() {
        return this._disableRow ? bi => this._disableRow(this.data.baseItem(bi), bi, this.data) : undefined;
    }

    _disableRowChanged(_disableRow) {
        this.$.chunker.querySelectorAll('[part=row]').forEach(row => {
            const index = row.index;
            if (index >= 0) {
                const disabled = this._disabledRow(index);
                rowEl(row).querySelectorAll(':scope > [part~=body-cell]').forEach(cell => this._disableCell(cell, disabled));
            }
        });

        if (this.data) {
            this.data.isDisabled = this._disabledRowFunction();
        }
    }

    _disabledRow(index) {
        return this._disableRow && this._disableRow(this.data.item(index), this.data.baseIndex(index), this.data);
    }

    __disableRowAndChildren(item, baseIndex, data) {
        console.assert(this.data === data);
        console.assert(typeof this.disableRow === 'function');
        console.assert(!this.disabled);

        if (this.disableRow(item, baseIndex, data)) {
            return true;
        }
        for (let bi = data.treeParent(baseIndex); bi >= 0; bi = data.treeParent(bi)) {
            if (this.disableRow(data.baseItem(bi), bi, data)) {
                return true;
            }
        }
        return false;
    }

    _computeDisableRow(disabled, disableRow, disableChildRows) {
        if (disabled) {
            return () => true;
        }
        switch (typeof disableRow) {
            case 'string':
                return item => item[disableRow];
            case 'function':
                return disableChildRows ? this.__disableRowAndChildren.bind(this) : disableRow;
        }
        return null;
    }

    // A new view configurator
    _viewChanged(view, old) {
        if (old) {
            old.unobserve(this);
            old.valueManager.unobserve(this);
            if (old.$i18n === this) {
                old.$i18n = undefined;
            }
        }
        if (view) {
            view.observe(this);
            view.valueManager.observe(this);
            view.valueManager.debounce = this.delayedLoadingDebounce;
            if (this.highlightNewRows) {
                this._highlightNewRowsChanged(this.highlightNewRows);
            }
            this._createDefaultSort();
            view.$i18n = this;
        }
    }

    // Compute width of Edit Control
    _ecWidth() {
        const ec = (this.editControlVisibility !== 'never' || (this.inlineEditing && this._editCells)) && this.view.createEditControl();
        if (!ec) {
            return 0; // Client hides the edit control or doesn't use an edit control
        }
        // Compute ideal width of edit control
        ec.style.position = 'absolute';
        ec.style.visibility = 'hidden';
        ec.singleLine = true;
        document.body.appendChild(ec);
        ec.performUpdate();
        const label = ec.shadowRoot.getElementById('label');
        if (label) {
            label.performUpdate();
        }
        const w = ec.offsetWidth;
        ec.remove();
        return w + 1; // Round upwards, or we'll risk ugly line breaks
    }

    _colMinWidth(col) {
        const minW = PTCS.cssDecodeSize(col.minWidth, this);
        // eslint-disable-next-line no-nested-ternary
        const requiredMin = col?.config?.bulkSelection && col.sortable ? 109 : col.sortable ? 34 : this.__resizerHitArea;
        return (isNaN(minW) || minW < requiredMin) ? requiredMin : minW;
    }

    _assignColWidths() {
        const colWidths = [];
        let canGrow = false; // Can the grid expand to any width?
        const maxW = {};
        const colMaxWidths = [];

        // Width that should be added to all editable columns
        const ecWidth = this._editCells ? this._ecWidth() : 0;
        const inlineEditing = this.inlineEditing && this.editControlVisibility === 'never';

        // Add ecWidth to editable columns and --ptcs-toggle-depth to toggle columns
        const ecAdjust = (width, col) => {
            const bulkRequiredMinWidth = col.config?.bulkSelection && col.sortable && inlineEditing;
            if (ecWidth && col.editable && (!inlineEditing || col.externalEdit)) {
                return col.treeToggle
                    ? `calc(${width} + ${ecWidth}px + var(--ptcs-toggle-depth, 0) * var(--ptcs-toggle-indent, 24px))`
                    : `calc(${width} + ${ecWidth}px)`;
            }
            // eslint-disable-next-line no-nested-ternary
            return col.treeToggle
                ? `calc(${width} + var(--ptcs-toggle-depth, 0) * var(--ptcs-toggle-indent, 24px))`
                : bulkRequiredMinWidth ? `max(${width}, 109px)` : width;
        };

        // Decode minmax(5px, 10px) to [[5, 'px'], [10, 'px']]
        const decodeMinmax = w => {
            if (typeof w !== 'string') {
                return undefined;
            }
            const m = /^minmax\(([\d.\w%]+) *, *([\d.\w%]+)\)$/.exec(w);
            if (!m) {
                return undefined;
            }
            const v1 = decode(m[1]);
            const v2 = decode(m[2]);
            return v1 && v2 && [v1, v2];
        };

        this._colWidths.forEach((w, i) => {
            const minmax = decodeMinmax(w.width);
            let minWidth = decode(w.minWidth) || (minmax && minmax[0]);
            let width = decode(w.width);
            let maxWidth = decode(w.maxWidth) || (minmax && minmax[1]);

            // minWidth and maxWidth can not use fraction units
            if (minWidth && minWidth[1] === 'fr') {
                minWidth = undefined;
            }
            if (maxWidth && maxWidth[1] === 'fr') {
                maxWidth = undefined;
            }

            const isFrUnit = width && width[1] === 'fr';

            // Can we reduce the number of width specifiers to two, so with can be handled by minmax()?
            if (maxWidth) {
                if (!minWidth && !isFrUnit) {
                    // Cannot do this switch if width uses fraction units (don't know why, but both Chrome and Firefox fails otherwise)
                    minWidth = width;
                    width = maxWidth;
                    maxWidth = undefined;
                } else if (!width) {
                    width = maxWidth;
                    maxWidth = undefined;
                } else {
                    // There are 3 width specifiers, which CSS grid doesn't support. Need to monitor widths manually
                    colMaxWidths[i] = {width: width.join(''), maxWidth: maxWidth.join('')};
                }
            }

            // Assign widths
            const _minWidth = minWidth ? minWidth.join('') : defaultColMinW;
            if (width) {
                const _width = colMaxWidths[i] ? `var(--ptcs-grid-colw-${i})` : width.join('');
                if (width[1] === 'fr' || width[1] === '%') {
                    if (maxWidth) {
                        maxW[maxWidth[1]] = (maxW[maxWidth[1]] || 0) + maxWidth[0]; // Add value to its unit
                    } else {
                        canGrow = true;
                    }
                    colWidths[i] = `minmax(${ecAdjust(_minWidth, w)},${_width})`;
                } else {
                    colWidths[i] = ecAdjust(_width, w);
                    maxW[width[1]] = (maxW[width[1]] || 0) + width[0] + (w.editable ? ecWidth : 0); // Add value to its unit
                }
            } else if (typeof w.width === 'string' && w.width.startsWith('var(--')) {
                // Exact width via CSS variable
                colWidths[i] = w.width;
                console.assert(!maxW[w.width], `${w.width} occurs twice as column width`);
                maxW[w.width] = '';
            } else {
                canGrow = true;
                colWidths[i] = `minmax(${ecAdjust(_minWidth, w)},1fr)`;
            }
        });

        this._colMaxWidths = colMaxWidths.length && colMaxWidths;
        if (this._colMaxWidths) {
            // Initialize the CSS variables that holds the column widths
            this._colMaxWidths.forEach((w, i) => {
                this.style.setProperty(`--ptcs-grid-colw-${i}`, w.width);
            });
        }

        this.style.maxWidth = canGrow ? '' : `calc(${Object.keys(maxW).map(u => maxW[u] + u).join(' + ')} + var(--vssbw, 0px))`;
        this.style.setProperty('--ptcs-grid-columns', colWidths.join(' '));
        this._adjustMinMaxColumnWidths();

        this.style.setProperty('--ptcs-edit-control-width', `${ecWidth}px`);
    }

    _adjustMinMaxColumnWidths() {
        if (!this._colMaxWidths) {
            return;
        }

        if (this._colMaxWidths.clientWidth < this.clientWidth) {
            // Viewport has grown. Restore all default column widths
            this._colMaxWidths.forEach((w, i) => {
                if (w.maximized) {
                    this.style.setProperty(`--ptcs-grid-colw-${i}`, w.width);
                    w.maximized = undefined;
                }
            });
        }
        this._colMaxWidths.clientWidth = this.clientWidth;

        // Make sure no columns exceed the maximum width
        const header = this.$.header.children;

        let maximizedCols;
        do {
            maximizedCols = [];
            this._colMaxWidths.forEach((w, i) => {
                if (!w.maximized && header[i].clientWidth > PTCS.cssDecodeSize(w.maxWidth, header[i])) {
                    maximizedCols[i] = w.maxWidth;
                    w.maximized = true;
                }
            });
            maximizedCols.forEach((w, i) => {
                this.style.setProperty(`--ptcs-grid-colw-${i}`, w);
            });
        } while (maximizedCols.length && this._colMaxWidths.some(w => !w.maximized));
    }


    _resizedCellChanged(_resizedCell) {
        if (!_resizedCell) {
            this.$.resizer.style.display = 'none';
            this.style.cursor = '';

            return;
        }

        const cellR = _resizedCell.getBoundingClientRect();
        const headR = this.$.header.getBoundingClientRect();
        const gridR = this.getBoundingClientRect();

        this.$.resizer.style.display = 'block';
        this.$.resizer.style.left = `${Math.min(cellR.right - gridR.left - 1, headR.right - gridR.left - 1)}px`;
        this.$['resizer-handle'].style.height = `${cellR.height}px`;

        this.__setResizerHeight(cellR, this.$.resizer);
    }

    _resizedFocusedCellChanged(_resizedFocusedCell) {
        if (!_resizedFocusedCell) {
            return;
        }

        const cellR = _resizedFocusedCell.getBoundingClientRect();
        const gridR = this.getBoundingClientRect();

        this.$['resizer-focus'].style.left = `${cellR.right - gridR.left - 1}px`;
        this._resizerFocusHandle.style.height = `${cellR.height}px`;

        this.__setResizerHeight(cellR, this.$['resizer-focus']);
    }

    _updateResizerPositions() {
        if (this._resizedCell) {
            this._resizedCellChanged(this._resizedCell);
        }
        if (this._resizedFocusedCell) {
            this._resizedFocusedCellChanged(this._resizedFocusedCell);
        }
    }

    _updateWidthsInView() {
        if (!Array.isArray(this._resizedColWidths) || this._resizedColWidths.length === 0) {
            return;
        }
        const columnsWidths = PTCS.encodeViewExpr(this.view.columns
            .reduce((acc, col) => {
                if (!col.hidden) {
                    acc.push([col.id ? `#${col.id}` : (columnName(col) || '?')]);
                }
                return acc;
            }, [])
            .map((name, index) => [`${name}`, `${this._resizedColWidths[index]}`]));

        // Inform the view configurator that we have alternative column widths
        this.view.setWidthsExpression(columnsWidths, {rebuildRowDef: false});

        // It would probably be better if the view configurator generated this callback, but keeping it for backwards compatibility
        this.dispatchEvent(new CustomEvent('columns-resized', {bubbles: true, composed: true}));
    }

    __setCellWidthWithConstraints(resizedCell, newWidth) {
        newWidth = resizedCell.hasAttribute('resize-min-width') ? Math.max(resizedCell.getAttribute('resize-min-width'), newWidth) : newWidth;
        newWidth = resizedCell.hasAttribute('resize-max-width') ? Math.min(resizedCell.getAttribute('resize-max-width'), newWidth) : newWidth;

        const cellIndex = PTCS.getChildIndex(resizedCell);
        const hR = this.$.header.getBoundingClientRect();
        const rR = resizedCell.getBoundingClientRect();
        const oldRRight = rR.right;
        const rRight = rR.left + newWidth;
        const gridRect = this.mainGrid.getBoundingClientRect();
        const lastCell = this.$.header.lastChild;
        const lastCellIndex = PTCS.getChildIndex(lastCell);
        const lastCellRect = lastCell.getBoundingClientRect();
        const vScrollBarWidth = PTCS.getVerticalScrollbarWidth(this.$.chunker.elScroll);

        if (!Array.isArray(this._resizedColWidths) || this._resizedColWidths.length === 0) {
            this._resizedColWidths = [];

            for (let i = 0; i < this._colWidths.length; i++) {
                const cellR = this.$.header.children[i].getBoundingClientRect();
                this._resizedColWidths.push(`${cellR.width}px`);
            }
        }

        // When resizing columns, add the offset to the last column so it will grow accordingly and stay anchored to the right side of the grid
        if (lastCellRect.right < gridRect.right - vScrollBarWidth) {
            const offset = rRight - oldRRight;
            this._resizedColWidths[lastCellIndex] = `${lastCellRect.width + Math.abs(offset)}px`;
        }

        const colsWidthSum = this._resizedColWidths.reduce((acc, val, i) => acc + (i !== cellIndex ? Number(decode(val)[0]) : newWidth), 0);

        // Set 'resize-min-width' for the last cell to limit it from getting narrower than the width of grid
        const requiredMin = this._colMinWidth(this.view.columns[cellIndex]);
        const lastCellMinWidth = gridRect.width - vScrollBarWidth - colsWidthSum + newWidth;
        lastCell.setAttribute('resize-min-width', Math.max(lastCellMinWidth, requiredMin));

        this._resizedColWidths[cellIndex] = `${newWidth}px`;

        this.style.maxWidth = `calc(${this._resizedColWidths.reduce((acc, w) => PTCS.cssDecodeSize(w) + acc, 0)}px + var(--vssbw, 0px))`;
        this.style.setProperty('--ptcs-grid-columns', this._resizedColWidths.join(' '));

        // Show a 'not-allowed' cursor when trying to resize the last column to be narrower than the width of grid
        if (lastCellIndex === cellIndex && colsWidthSum < gridRect.width) {
            this.style.cursor = 'not-allowed';
        }

        if (!this.__resizeCall) {
            return;
        }

        this.__resizeCall = false;

        // Check if we need to scroll to the new location
        if (rRight > hR.right) {
            this.$.chunker.elScroll.scrollLeft = this.$.chunker.elScroll.scrollLeft + rRight - oldRRight;
        }

        requestAnimationFrame(() => {
            this.__resizeCall = true;
        });
    }

    _computeResizeColumns(resizeColumns, _treeToggle) {
        return resizeColumns || (_treeToggle && resizeColumns !== false);
    }

    _resizeColumnsChanged(_resizeColumns, old) {
        if (!_resizeColumns === !old) {
            return; // Effective Boolean value is unchanged
        }
        if (!_resizeColumns) {
            // Remove all the listeners
            this.$.header.removeEventListener('mousemove', this.__mouseOverHeader);
            this.$.header.removeEventListener('mouseleave', this.__checkIfMouseLeftResizedCell);

            this.$['resizer-focus'].removeEventListener('mouseleave', this.__checkIfMouseLeftResizedCell);

            this.$['resizer'].removeEventListener('mousemove', this.__checkIfMouseLeftResizedCell);
            this.$['resizer'].removeEventListener('mouseleave', this.__checkIfMouseLeftResizedCell);

            this.shadowRoot.removeEventListener('mouseup', this.__resizeMouseUp);

            // Hide the resizer leftovers
            this._resizedCell = null;
            this._resizedFocusedCell = null;

            return;
        }

        this.$.header.addEventListener('mousemove', this.__mouseOverHeader);
        this.$.header.addEventListener('mouseleave', this.__checkIfMouseLeftResizedCell);

        this.$['resizer-focus'].addEventListener('mouseleave', this.__checkIfMouseLeftResizedCell);

        this.$['resizer'].addEventListener('mousemove', this.__checkIfMouseLeftResizedCell);
        this.$['resizer'].addEventListener('mouseleave', this.__checkIfMouseLeftResizedCell);

        this.shadowRoot.addEventListener('mouseup', this.__resizeMouseUp);
    }

    _reorderColumnsChanged(reorderColumns) {
        const columns = ((this.view && this.view.columns) || []).filter(col => !col.hidden);
        this.$.header.querySelectorAll('[part~=header-cell]').forEach((el, i) => {
            this._setbattr(el, 'draggable', reorderColumns && columns[i] && !columns[i].nonReorderable);
        });
    }

    // Decode specified widths and make sure they work for columns (every visible column needs to have a specified width, and vice versa)
    _decodeColumnWidths(columns, widths) {
        if (!widths) {
            return undefined; // The expected path - no specified column widths
        }
        const ws = PTCS.decodeViewExpr(widths);
        const a = [];
        let i0 = 0;
        for (let i = 0; i < columns.length; i++) {
            const col = columns[i];
            if (col.hidden) {
                a.push('0'); // Anything, to balance the number of columns
                continue;
            }
            const w = ws[i0++];
            if (!w) {
                return undefined; // Unspecified
            }
            const [name, width] = w;
            if (name !== columnName(col) && (name !== `#${col.id}`)) {
                return undefined; // Mismatch
            }
            a.push(width);
        }

        return ws[i0] ? undefined : a; // Make sure all specified widths has been processed
    }

    gvValueChanged(_id) {
        if (this.__changedValuesSet) {
            this.__changedValuesSet.add(_id);
        } else {
            this.__changedValuesSet = new Set([_id]);
            requestAnimationFrame(() => {
                const {data} = this;
                const set = this.__changedValuesSet;
                this.__changedValuesSet = undefined;
                if (!data.valuesChanged()) {
                    const {startIx, endIx} = this.$.chunker;
                    const f = item => {
                        for (let i = startIx; i < endIx; i++) {
                            if (item === data.item(i)) {
                                return i;
                            }
                        }
                        return undefined;
                    };
                    set.forEach(id => {
                        const index = typeof id === 'number' ? data.translateBaseIndexToIndex(id) : f(id);
                        if (index !== undefined) {
                            this._refreshChunker(index);
                        }
                    });
                }
            });
        }
    }

    gvViewport(viewport, added, removed) {
        this.mainGrid.dispatchEvent(new CustomEvent('viewport-values', {detail: {viewport, added, removed}}));
    }

    // View configuration has changed
    dvChanged() {
        if (!this.view) {
            return;
        }

        // Is editing enabled?
        this._editing = this.view.editLevel === 'cell' || this.view.editLevel === 'row' || this.view.editLevel === 'grid';

        // Show an edit control in hovered / focused cells?
        this._editCells = this.view.editLevel === 'cell' || this.view.editLevel === 'grid';

        const columns = this.view.columns;
        const maxHeightHeader = this.view.maxHeightHeader;
        const header = this.$.header;
        while (header.firstChild) {
            header.removeChild(header.firstChild);
        }

        if (!columns || !columns.length) {
            console.log('The grid columns are not valid');
            this._rebuildChunker({wipe: true});
            return;
        }

        const columnWidths = this._decodeColumnWidths(columns, this.view.getWidthsExpression(false));

        // Are there any tree toggles
        this._treeToggle = columns.some(col => col.treeToggle);

        this._colWidths = [];
        this._resizedColWidths = [];

        // Create the header cells offscreen
        const frag = document.createDocumentFragment();

        columns.forEach((col, index) => {
            if (col.hidden) {
                return;
            }

            // Create header cell
            const cell = document.createElement('div');
            cell.setAttribute('part', 'header-cell');
            cell.setAttribute('class', 'cell');

            if (col.headerHAlign) {
                cell.setAttribute('header-halign', col.headerHAlign);
            }
            if (col.headerVAlign) {
                // Unfortunately, Thingworx specifies middle instead of center. Now need to support both to be backwards compatible
                cell.setAttribute('header-valign', col.headerVAlign === 'middle' ? 'center' : col.headerVAlign);
            }
            if (col.treeToggle) {
                cell.setAttribute('tree-toggle', '');
            }
            if (maxHeightHeader > 0) {
                cell.style.maxHeight = maxHeightHeader + 'px';
                cell.style.overflow = 'hidden';
            }

            if (this._resizeColumns) {
                if (col.nonresizable) {
                    cell.setAttribute('non-resizable', '');
                } else {
                    // Set min and max width for the resizing
                    const minW = this._colMinWidth(col);
                    const maxW = PTCS.cssDecodeSize(col.maxWidth, this);

                    if (minW && !Number.isNaN(minW)) {
                        cell.setAttribute('resize-min-width', minW);
                    }

                    if (maxW && !Number.isNaN(maxW)) {
                        cell.setAttribute('resize-max-width', maxW);
                    }
                }
            }

            // Create header cell content
            let el;
            if (!col.label) {
                el = document.createElement('div');
            } else if (typeof col.label === 'string') {
                el = document.createElement('ptcs-label');
                el.setAttribute('variant', 'grid-item');
                el.setAttribute('part', 'header-label');
                el.label = col.label;
            } else if (typeof col.label === 'function') {
                el = col.label(this.data, this.view, cell, {inlineEditing: this.inlineEditing});
            }

            if (el) {
                el.disabled = this.disabled;
                cell.appendChild(el);
            }

            if (this.view.selectMethod === 'multiple') {
                if (col.id === 'select') {
                    PTCS.setbattr(cell, 'selection', true);
                } else if (col.id === 'showRowNumbers') {
                    PTCS.setbattr(cell, 'show-row-numbers', true);
                }
            }

            if (this.reorderColumns && !col.nonReorderable) {
                cell.setAttribute('draggable', '');
            }

            frag.appendChild(cell);

            this._colWidths.push(columnWidths ? {width: columnWidths[index]} : {...col});
        });

        header.appendChild(frag);

        this._assignColWidths();

        // Drop all recycled elements. The format has changed
        this._recycled.clear();

        if (this._reSortGrid) {
            this._sortGrid(this.view.getSortExpression().short);
        }

        // The view configuration has been updated. Must rebuild now, or there will be strange flickering effects
        this.$.chunker.rebuild(true);

        // If chunker is rebuilt then the footer should be rebuild as well
        this._rebuildFooter();

        this.___rebuildChunkerOn = 0;

        this.$.header.querySelectorAll('[part=sort-icon]').forEach((icon) => this._setSortStyling(icon));

        this._watchSelectionColumnWidth();
    }

    // Observe widths of multi selection cells (header and body) and store max width in --ptcs-core-grid-selection-width
    _watchSelectionColumnWidth() {
        if (this.__watchSelectionColumnWidth$) {
            this.__watchSelectionColumnWidth$.ro.disconnect();
            this.__watchSelectionColumnWidth$.el.remove();
            this.__watchSelectionColumnWidth$ = undefined;
        }
        if (this.view.selectMethod === 'multiple') {
            requestAnimationFrame(() => {
                const el1 = cloneSelectionCell(this.$.header.querySelector('ptcs-grid-selection-observer'));
                const el2 = cloneSelectionCell(this.$.chunker.querySelector('[part~=row-selection-checkbox]'));
                if (!el1 && !el2) {
                    return; // Nothing to watch yet
                }

                // Create an invisble element with the multi selection cells that we can watch
                const el = document.createElement('div');
                el.style.visibility = 'hidden';
                el.style.position = 'absolute';
                el.style.top = '-100px';
                if (el1) {
                    el.appendChild(el1);
                }
                if (el2) {
                    el.appendChild(el2);
                }
                this.shadowRoot.appendChild(el);

                // Observe widths of cloned header and body cells (assume that any relevant CSS styling applies to them as well)
                const ro = new ResizeObserver(entries => {
                    requestAnimationFrame(() => this.style.setProperty('--ptcs-core-grid-selection-width', `${entries[0].contentRect.width}px`));
                });

                ro.observe(el);

                this.__watchSelectionColumnWidth$ = {el, ro, el2};
            });
        }
    }

    _setHostEditVisibility() {
        const editVisibility = this._editVisibility();
        const host = this.mainGrid;
        if (this.__slottedRows && editVisibility) {
            host.setAttribute('edit-visibility', editVisibility);
        } else {
            host.removeAttribute('edit-visibility');
        }
    }

    _editControlVisibilityChanged() {
        this._setHostEditVisibility();

        // Drop all recycled elements. The format has changed
        this._recycled.clear();

        // The view configuration has been updated. Must rebuild now, or there will be strange flickering effects
        this._rebuildChunker({wipe: true});
    }

    _doElsOverlap(el1, el2, midPoint = false) {
        const rect1 = el1.getBoundingClientRect();
        const rect2 = el2.getBoundingClientRect();

        const rect1MidPoint = rect1.left + ((rect1.width) / 2);

        if (midPoint) {
            // Checks if el1 middle is overlapping el2
            return !((rect1MidPoint < rect2.left) ||
                     (rect1MidPoint > rect2.right) ||
                     (rect1.bottom < rect2.top) ||
                     (rect1.top > rect2.bottom));
        }

        return !((rect1.right < rect2.left) ||
                 (rect1.left > rect2.right) ||
                 (rect1.bottom < rect2.top) ||
                 (rect1.top > rect2.bottom));
    }

    // Returns a list of grid header cells as objects conatining index, cell midPoint(x), html node, and rect details.
    _getHeaderCellsObj() {
        if (!this.$.header) {
            return null;
        }

        const headerCellsNodes = this.$.header.querySelectorAll('[part^=header-cell]');
        const objsList = [];

        headerCellsNodes.forEach((node) => {
            if (!node.hasAttribute('draggable')) {
                return;
            }

            const elRect = node.getBoundingClientRect();

            const obj = {
                node:     node,
                rect:     elRect,
                midPoint: elRect.left + ((elRect.width) / 2)
            };

            objsList.push(obj);
        });

        return objsList;
    }

    _onheaderdragstart() {
        return false;
    }

    _onDragStart(ev) {
        if (PTCS.wrongMouseButton(ev)) {
            return;
        }

        const actionEl = ev.target.closest('[grid-action]');
        if (actionEl) {
            return; // Clicked on a grid action (the sort button?)
        }
        const cell = ev.target.closest('[part~=header-cell]');
        const colIndex = PTCS.getChildIndex(cell);

        if (!this.reorderColumns || this._resizedCell || !cell.hasAttribute('draggable')) {
            return;
        }

        ev.preventDefault();

        this.__dragging = true;
        this.style.cursor = 'grabbing';

        const gridRect = this.getBoundingClientRect();

        const draggedCol = this._getDraggedColumn(colIndex, cell);
        this.shadowRoot.appendChild(draggedCol);

        let draggedColRect = draggedCol.getBoundingClientRect();
        const draggedColMiddle = draggedColRect.width / 2;

        this._updateReorderIndicator(gridRect, draggedCol);

        const elScroll = this.$.chunker.elScroll;
        const hScrollExist = elScroll.scrollWidth > elScroll.clientWidth;

        let {posX: x, posY: y} = PTCS.getCoordinatesFromEvent(ev);

        const mouseMoveHandler = (ev2) => {
            if (this.__resizing) {
                return;
            }

            draggedColRect = draggedCol.getBoundingClientRect();

            const {posX: moveX, posY: moveY} = PTCS.getCoordinatesFromEvent(ev2);

            // How far the mouse has been moved
            const dx = moveX - x;
            const dy = moveY - y;

            // Scroll horizontally while dragging, in case the draggedCol exceeds the grid's boundaries
            if (hScrollExist) {
                if (draggedColRect.right > gridRect.right) {
                    elScroll.scrollLeft += PTCS.scrollSpeed;
                } else if (draggedColRect.left < gridRect.left) {
                    elScroll.scrollLeft -= PTCS.scrollSpeed;
                }
            }

            let offsetX;
            // Check if draggedCol x values exceed grid boundaries
            if (((draggedColRect.right - (draggedColRect.width / 2)) + dx) > gridRect.right) {
                offsetX = gridRect.right - draggedColMiddle - draggedCol.width;
            } else if ((draggedCol.offsetLeft + dx) < -draggedColMiddle) {
                offsetX = -draggedColMiddle;
            } else {
                offsetX = draggedCol.offsetLeft + dx;
            }

            let offsetY;
            // Check if draggedCol y values exceed grid boundaries from the top
            if ((draggedCol.offsetTop + dy) < 0) {
                offsetY = 0;
            } else {
                offsetY = draggedCol.offsetTop + dy;
            }

            // Set the position of element
            draggedCol.style.top = `${offsetY}px`;
            draggedCol.style.left = `${offsetX}px`;

            // Re assign the position of the mouse
            x = moveX;
            y = moveY;

            this._updateReorderIndicator(gridRect, draggedCol);
        };

        const mouseUpHandler = () => {
            ['mousemove', 'touchmove'].forEach(evName => {
                document.removeEventListener(evName, mouseMoveHandler);
            });

            ['mouseup', 'touchend'].forEach(evName => {
                document.removeEventListener(evName, mouseUpHandler);
            });

            if (this._doElsOverlap(draggedCol, this.$.header)) {
                // ugly and inefficient, but simple
                const headerCellsNodes = this.$.header.querySelectorAll('[part^=header-cell]');
                const numVisibleNonReorderable = headerCellsNodes && headerCellsNodes.length
                    ? headerCellsNodes.length - this._getHeaderCellsObj().length
                    : 0;
                this._reorderColumns(colIndex, this._draggedDestIndex + numVisibleNonReorderable);
            }

            draggedCol.remove();
            this.style.cursor = '';
            this.__dragging = false;
            this.$['reorder-indicator'].style.display = 'none';
        };

        ['mousemove', 'touchmove'].forEach(evName => {
            document.addEventListener(evName, mouseMoveHandler);
        });

        ['mouseup', 'touchend'].forEach(evName => {
            document.addEventListener(evName, mouseUpHandler);
        });
    }

    // Updates reorder indicator position and dest index
    _updateReorderIndicator(gridRect, draggedCol) {
        const headerCellsObjs = this._getHeaderCellsObj();

        if (!headerCellsObjs) {
            return;
        }

        const indicatorStyle = this.$['reorder-indicator'].style;

        if (this._doElsOverlap(draggedCol, this.$.header)) {
            const draggedColRect = draggedCol.getBoundingClientRect();
            indicatorStyle.display = 'block';
            this.style.cursor = 'grabbing';

            for (let i = 0; i < headerCellsObjs.length; i++) {
                if (this._doElsOverlap(draggedCol, headerCellsObjs[i].node, true)) {
                    const draggedColMidPoint = draggedColRect.left + (draggedColRect.width / 2);

                    if (draggedColMidPoint < headerCellsObjs[i].midPoint) {
                        indicatorStyle.left = `${headerCellsObjs[i].rect.left - gridRect.left - 1}px`;
                        this._draggedDestIndex = i;
                    } else {
                        indicatorStyle.left = `${headerCellsObjs[i].rect.right - gridRect.left - 1}px`;
                        this._draggedDestIndex = i + 1;
                    }

                    break;
                }
            }
        } else {
            indicatorStyle.display = 'none';
            this.style.cursor = 'not-allowed';
        }
    }

    _reorderColumns(fromIndex, toIndex) {
        const leadingComplementaryColumns = this.view.columns.findIndex(col => !col.id);
        fromIndex -= leadingComplementaryColumns;
        toIndex -= leadingComplementaryColumns;
        const columns = PTCS.decodeViewExpr(this.view.getVisibilityExpression());
        const visibleColumns = columns.filter(pair => pair[1] !== 'false');

        // eslint-disable-next-line max-len
        if (fromIndex === toIndex || toIndex === fromIndex + 1 || fromIndex < 0 || toIndex < 0 || fromIndex >= visibleColumns.length || toIndex > visibleColumns.length) {
            return; // No move or bad move (should never happen)
        }

        // Find real indexes (not efficient, but simple)
        const from = columns.findIndex(pair => pair === visibleColumns[fromIndex]);
        const to = toIndex < visibleColumns.length ? columns.findIndex(pair => pair === visibleColumns[toIndex]) : columns.length;

        // Move column
        columns.splice(from < to ? to - 1 : to, 0, columns.splice(from, 1)[0]);

        // Update view configurator
        this.view.setVisibilityExpression(PTCS.encodeViewExpr(columns));

        // Move column width
        const columnsWidthExp = this.view.getWidthsExpression(false); // Don't want the default value
        if (columnsWidthExp) {
            // width expression unlike visibility expression
            const widthExpFrom = from + leadingComplementaryColumns;
            const widthExpTo = to + leadingComplementaryColumns;
            const columnsWidth = columnsWidthExp.split(',');
            columnsWidth.splice(widthExpFrom < widthExpTo ? widthExpTo - 1 : widthExpTo, 0, columnsWidth.splice(widthExpFrom, 1)[0]);
            this.view.setWidthsExpression(columnsWidth.join(','), {rebuildRowDef: false});
        }
    }

    _getDraggedColumn(colIndex, headerCell) {
        const nodesList = this.rowContainer.querySelectorAll(`[part=row]>*[part^=body-cell]:nth-child(${colIndex + 1})`);
        const cellR = headerCell.getBoundingClientRect();
        const gridR = this.getBoundingClientRect();

        const tempCol = document.createElement('div');
        tempCol.setAttribute('part', 'dragged-column');
        tempCol.style.position = 'absolute';
        tempCol.style.left = `${cellR.left - gridR.left - 1}px`;
        tempCol.style.top = `${cellR.top - gridR.top - 3}px`;
        tempCol.style.zIndex = 100;

        const tempHeader = headerCell.cloneNode(true);
        tempHeader.removeAttribute('draggable');
        tempHeader.setAttribute('dragged', '');
        tempHeader.style.width = `${cellR.width}px`;
        tempHeader.style.height = `${cellR.height}px`;
        tempHeader.style.zIndex = 10;

        const headerLabel = headerCell.querySelector('[part=header-label]');
        if (headerLabel) {
            const tempHeaderLabel = tempHeader.querySelector('[part=header-label]');
            tempHeaderLabel.label = headerLabel.label;
        }

        const rowsContainer = document.createElement('div');
        let rowsContainerHeight = 0;

        /* eslint-disable consistent-return */
        const getMatchingNode = (list, index) => {
            for (const node of list) {
                if (this._rowIndex(node.parentNode) === index) {
                    return node;
                }
            }
        };
        /* eslint-enable consistent-return */

        if (this.data && this.data.length) {
            let newCell;
            let newCellR;

            // Must turn off slotted rows when copying the draggable column image
            const {__slottedRows} = this;

            try {
                // Duplicate cells of the moved row
                for (let i = this.$.chunker.startIx; i < this.$.chunker.endIx; i++) {
                    const node = getMatchingNode(nodesList, i);
                    newCellR = node.getBoundingClientRect();
                    this.__slottedRows = undefined;
                    newCell = this._createGridRow(i).childNodes[colIndex];
                    this.__slottedRows = __slottedRows;
                    newCell.style.width = `${newCellR.width}px`;
                    newCell.style.height = `${newCellR.height}px`;
                    newCell.style.transform = node.parentNode.style.transform;
                    newCell.style.position = 'absolute';
                    rowsContainerHeight += newCellR.height;
                    rowsContainer.appendChild(newCell);
                }
            } finally {
                this.__slottedRows = __slottedRows;
            }
        }

        rowsContainer.style.height = `${rowsContainerHeight}px`;

        tempCol.appendChild(tempHeader);
        tempCol.appendChild(rowsContainer);

        return tempCol;
    }

    _setDataLength(_dataLength) {
        const dataLength = this.showErrorMessage ? 1 : _dataLength;
        this.$.chunker.numItems = this.slottedMessage
            ? Math.max(1, dataLength) // If no items, then show the slotted message
            : dataLength; // Don't show a message when the grid is empty
    }

    _slottedMessageChanged() {
        if (!this.data || this.data.length === 0) {
            this._setDataLength(0);
            this._refreshChunker(0);
        }
    }

    _showErrorMessageChanged() {
        this._setDataLength(this.data?.length || 0);
        this._refreshChunker();
    }

    _delayedLoadingDebounceChanged() {
        if (this.view) {
            this.view.valueManager.debounce = this.delayedLoadingDebounce;
        }
    }

    // Do the rows need to be slotted into the view? (Using vanilla CSS?)
    _slottedRowsChanged(slottedRows) {
        if (!this.__slottedRows === !slottedRows) {
            return; // No change
        }

        this.__slottedRows = !!slottedRows;

        this._setHostEditVisibility();

        // Remove previous styling, if any
        const removeStyling = () => [...this.mainGrid.querySelectorAll(':scope > style[xid]')].forEach(style => style.remove());

        // Duplicate shadow styling to "plain CSS" for parent grid element (where the row elements will reside)
        const duplicateStyling = () => {
            removeStyling(); // Remove old styling

            const xid = `X${performance.now().toString().replace('.', '')}`;
            const host = `ptcs-grid[xid=${xid}]`;

            // Convert shadow CSS selector to plain CSS selector
            const selector = s => {
                if (s.indexOf('#') >= 0) {
                    return ''; // Addresses unique elements in shadow dom. Not needed in host
                }

                if (s.startsWith('ptcs-v-scroller2')) {
                    if (s.indexOf('edit-visibility') < 0) {
                        return ''; // ptcs-v-scroller2 is an internal component. Cannot be styled in ptcs-grid
                    }
                    // Using edit-visibility in ptcs-v-scroller2, which is also available in ptcs-grid
                    const space = s.indexOf(' ');
                    return `${host}${s.substring(16, space)}${s.substring(space)}`;
                }

                const i = s.indexOf(':host');
                if (i < 0) {
                    return `${host} ${s}`;
                }

                if (i + 6 >= s.length) {
                    return ''; // Addresses ptcs-core-grid. Not needed in host
                }

                const pre = (i ? s.substring(0, i) : '') + host;
                if (s[i + 5] !== '(') {
                    return `${pre}${s.substring(i + 5)}`;
                }

                let j = i + 6;
                let pc = 1; // parenthesis count
                while (j < s.length) {
                    switch (s[j++]) {
                        case '(':
                            pc++;
                            break;
                        case ')':
                            if (--pc === 0) {
                                // parantheses has been balanced
                                if (j >= s.length) {
                                    return ''; // Addresses ptcs-core-grid. Not needed in host
                                }
                                return `${pre}${s.substring(i + 6, j - 1)}${s.substring(j)}`;
                            }
                    }
                }
                return ''; // Error
            };

            const rules = (this.shadowRoot.adoptedStyleSheets || []).reduce((acc, sheet) => {
                return [...sheet.rules].reduce((a, rule) => {
                    const s = (rule instanceof CSSStyleRule) && selector(rule.selectorText);
                    if (s) {
                        a.push(`${s}${rule.cssText.substring(rule.selectorText.length)}`);
                    }
                    return a;
                }, acc);
            }, []).join('\n');

            const style = PTCS.createElement('style', {xid});
            style.textContent = rules;

            const grid = this.mainGrid;
            grid.setAttribute('xid', xid);
            grid.appendChild(style);
        };

        if (this.__slottedRows) {
            duplicateStyling();

            if (!this.__duplicateStylingEv) {
                document.addEventListener('style-aggregator', this.__duplicateStylingEv = duplicateStyling);
            }
        } else {
            removeStyling(); // Remove old styling
            if (this.__duplicateStylingEv) {
                document.removeEventListener('style-aggregator', this.__duplicateStylingEv);
                this.__duplicateStylingEv = undefined;
            }
        }

        this._rebuildChunker({wipe: true, autoScroll: true});
    }

    _shiftKeySelectionChanged(shiftKeySelect) {
        if (!shiftKeySelect) {
            this._multiSelectionClearStart();
        }
    }

    _dataChanged(data, old) {
        if (old) {
            old.unobserve(this);
        }
        if (data) {
            data.observe(this);
            this._createDefaultSort();
            this._setDataLength(data.length);

            data.isDisabled = this._disabledRowFunction();

            // Sort the data?
            const sort = this.view && this.view.getSortExpression().short;
            if (sort) {
                this._sortGrid(sort);
            }
        } else {
            this._setDataLength(0);
        }
        if (old) {
            // Replaced the data manager. Need to refresh out all references to the old data manager in the view structure
            this.dvChanged();
            this._autoScroll();
        } else {
            this._rebuildChunker({autoScroll: true});
        }
    }

    _refreshChunker(index) {
        if (this.view && this.view.columns && this.data) {
            this.$.chunker.refresh(index);
        }
    }

    // opt: {wipe, autoScroll}
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
            if (this.view && this.view.columns && this.data) {
                this.$.chunker.rebuild(_wipe);

                if (opt && opt.autoScroll) {
                    this._autoScroll();
                }
            }
        });
    }

    // Get the currently active sort expression, as seen by the UI
    get sortExpression() {
        return PTCS.encodeViewExpr([...this.$.header.querySelectorAll('[part=sort-icon]')]
            .filter(el => el.sortOrder !== 'none')
            .map(el => [el.name, el.sortOrder]));
    }

    _createSortFunction(sortExpr) {
        // Figure out how to sort the initial data
        if (!this.view || !this.view.columns || !sortExpr || typeof sortExpr !== 'string') {
            return null;
        }
        const sortFields = PTCS.decodeViewExpr(sortExpr).reduce((acc, [name, sortOrder]) => {
            const colDef = sortOrder !== 'none' && this.view.columns.find(col => col.$sortName === name) ||
                this.view.columns.find(col => columnName(col) === name); // Fallback, mainly for initial sort expressions
            if (colDef) {
                const compare = colDef.compare;
                const select = colDef.sortSelect || colDef.select;
                if (compare && select) {
                    acc.push(sortOrder === 'asc'
                        ? (a, b, i1, i2, dm) => compare(select(a), select(b), i1, i2, dm)
                        : (a, b, i1, i2, dm) => compare(select(b), select(a), i2, i1, dm));
                }
            }
            return acc;
        }, []);

        // No sort fields?
        if (!sortFields.length) {
            return null;
        }

        // Single sort field?
        if (sortFields.length === 1) {
            return sortFields[0];
        }

        // Mutliple sort fields
        return (a, b, i1, i2, dm) => {
            for (const sortField of sortFields) {
                const c = sortField(a, b, i1, i2, dm);
                if (c) {
                    return c;
                }
            }
            return 0;
        };
    }

    _createDefaultSort() {
        if (!this.data || !this.view) {
            return;
        }
        if (this.view.externalSort) {
            // In external sort mode, only enable default sorting when all sort buttons are in off mode
            this.data.defaultSort = !this.view.getSortExpression().short && this._createSortFunction(this.view.initialSortExpr);
        } else {
            // In vanilla sort mode, always enable default sorting
            this.data.defaultSort = this._createSortFunction(this.view.initialSortExpr);
        }
    }

    // Sort data manager based on sort icons
    _sortGrid(sortExpr) {
        if (!this.data || !this.view || !this.view.columns) {
            this._reSortGrid = true; // Hack
            return;
        }

        this._reSortGrid = false;

        if (this.view.externalSort) {
            this._createDefaultSort();
        } else {
            this.data.sort = this._createSortFunction(sortExpr);
        }
    }

    // Set sort attribute in cell according to sort icon
    // TODO: Move this styling to the sort icon instead! Will simplify logic (a lot)!
    _setSortStyling(icon) {
        const headerCell = icon.closest('[part~=header-cell]');
        if (headerCell) {
            switch (icon.sortOrder) {
                case 'asc':
                    headerCell.setAttribute('sort', 'ascending');
                    break;
                case 'desc':
                    headerCell.setAttribute('sort', 'descending');
                    break;
                default:
                    headerCell.removeAttribute('sort');
            }
        }
    }

    _sortActionEv(ev) {
        const icon = ev.target;

        icon.sortOrder = ({asc: 'desc', desc: 'none'}[icon.sortOrder] || 'asc');

        this._setSortStyling(icon);

        this.view.setSortExpression(this.sortExpression, {}, {clicked: true});

        // I'm not sure why we don't let the setSortExpression generate this event via dvSort (below), but
        // maybe we need to 100% enforce that this click is reported when the user clicks on a sort icons
        icon.dispatchEvent(new CustomEvent('sort-icon-click', {bubbles: true, composed: true}));
    }

    dvSort(expr, opt) {
        const a = PTCS.decodeViewExpr(expr);

        // Synchronize icons according to sort expression
        this.$.header.querySelectorAll('[part=sort-icon]').forEach(icon => {
            const r = a.find(x => x[0] === icon.name);
            const order = (r && r[1]) || 'none';
            if (icon.sortOrder !== order) {
                icon.sortOrder = order;
                this._setSortStyling(icon);
            }
        });

        this._sortGrid(expr, opt && opt.reset);

        if (!(opt && opt.clicked)) {
            // Tell client that it needs to resort
            this.dispatchEvent(new CustomEvent('sort-icon-click', {bubbles: true, composed: true}));
        }
    }

    dvSortInitial() {
        this._createDefaultSort();
    }

    // Data Notifier: the data has changed
    dmView() {
        this._setDataLength(this.data.length);
        this._rebuildChunker({autoScroll: true});
        this._multiSelectionClearStart();
    }

    dmFilter() {
        this._rebuildFooter();
    }

    dmItem(index) {
        this._refreshChunker(index);

        this._rebuildFooter();
    }

    // Data Notifier: data has been added
    dmInserted(inserted) {
        this.$.chunker.inserted(inserted);
        this._rebuildFooter();
        this._multiSelectionClearStart();

        this.dispatchEvent(new CustomEvent('items-updated', {
            bubbles:  true,
            composed: true,
            detail:   {inserted}}));
    }

    // Data Notifier: data has been removed
    dmRemoved(removed) {
        this.$.chunker.removed(removed);
        this._rebuildFooter();
        this._multiSelectionClearStart();

        this.dispatchEvent(new CustomEvent('items-updated', {
            bubbles:  true,
            composed: true,
            detail:   {removed}}));

        // If the last visible item got removed
        if (this.data.length === 0) {
            setTimeout(() => {
                // This is a hack to force the chunker to show the empty message
                // when the last visible row are removed, after the scroll animation has ended
                if (!this.data || !this.data.length) {
                    this.$.chunker.numItems = 0;
                    this._setDataLength(0);
                }
            }, 300);
        }
    }

    dmSelected(baseIndex, selected) {
        // _selMap is only really needed when we have a projection on the data,
        // because then translateBaseIndexToIndex can get _really_ slow,
        // but it is good for performance whenever the number of data items is big
        if (!this._selMap) {
            const endIx = this.$.chunker.endIx;
            this._selMap = new Map();
            for (let i = this.$.chunker.startIx; i <= endIx; i++) {
                this._selMap.set(this.data.baseIndex(i), i);
            }
        }

        const index = this._selMap.get(baseIndex);
        if (index >= 0) {
            this._refreshChunker(index);
            const el = index > 0 && this.$.chunker.getRow(index - 1);
            if (el) {
                this._setbattr(el, 'next-row-selected', selected);
            }
        }
    }

    dmSelection() {
        if (this.autoScroll) {
            requestAnimationFrame(this._autoScroll.bind(this));
        }
    }

    dmDepth(depth) {
        this.style.setProperty('--ptcs-toggle-depth', depth);
    }

    dmCommit() {
        // Get rid of any dirty flags
        this._refreshChunker();
    }

    _onbulkSelectChanged(ev) {
        const fieldName = ev.detail?.columnName;
        const cbChecked = ev.detail?.value;
        if (fieldName) {
            for (let baseIndex = 0; baseIndex < this.data.baseLength; baseIndex++) {
                const item = this.data.baseItem(baseIndex);
                if (!(this._disableRow && this._disableRow(item, baseIndex, this.data)) && item[fieldName] !== cbChecked) {
                    this._applyEditUpdate(baseIndex, item, fieldName, {[fieldName]: cbChecked});
                }
            }
        }
    }

    _onMultiSelectChanged(ev) {
        const isDisabled = this._disabledRowFunction();
        if (isDisabled) {
            const isSelected = ev.detail.value
                ? (item, bi) => (!isDisabled(bi) || this.data.isSelectedBaseIndex(bi))
                : (item, bi) => (isDisabled(bi) && this.data.isSelectedBaseIndex(bi));
            const length = this.data.baseLength;
            const selected = [];
            for (let bi = 0; bi < length; bi++) {
                if (isSelected(this.data.baseItem(bi), bi)) {
                    selected.push(bi);
                }
            }
            this.data.selected = selected;
        } else if (ev.detail.value) {
            this.data.selectAllItems(true);
        } else {
            this.data.unselectAllItems(true);
        }
    }

    _resizedChunkerWidth(ev) {
        this.style.setProperty('--vssbw', `${ev.detail.sbWidth}px`);
        this.$.header.style.width = `${ev.detail.width + ev.detail.sbWidth}px`;
        this.$.footer.style.width = `${ev.detail.width + ev.detail.sbWidth}px`;
        this._updateResizerPositions();
    }

    _scrollLeftChanged(ev) {
        this.$.header.scrollLeft = ev.detail.value;
        this.$.footer.scrollLeft = ev.detail.value;
        this._updateResizerPositions();
    }

    // Mouse click on grid - select row?
    _clickOnGrid(ev) {
        if (PTCS.wrongMouseButton(ev)) {
            return;
        }
        const doubleClicked = (ev.detail === 2);
        if (this.disabled) {
            return;
        }
        // Did interactive cell content (e.g. checkbox, ...) already handle this interaction?
        if (ev.defaultPrevented) {
            return;
        }
        if (!this.data || !this.data.length) {
            return; // If there is a row in the chunker, it is the empty message
        }
        if (ev.target.closest('[grid-action]')) {
            return; // Clicked on a grid-action, so click event should be ignored
        }
        for (let el = ev.target; el; el = el.parentNode) {
            if (el.hasOwnProperty('index')) {
                const baseIndex = this.data.baseIndex(el.index);
                const disabled = this._disabledRow(el.index);
                const toggleIcon = el.querySelector('[part~=tree-toggle-icon]');
                if (!disabled && baseIndex >= 0 && toggleIcon !== ev.target) {
                    if (this.selectRow && this.data.selectMethod === 'single' && !this.selectFollowsFocus &&
                        (!doubleClicked || (this.data.selected !== baseIndex && this.preventDoubleClickedEvent))) {
                        this.data.select(baseIndex, undefined, true); // Toggle selection
                    }

                    this.dispatchEvent(new CustomEvent(doubleClicked ? 'row-double-click' : 'row-click',
                        {bubbles: true, composed: true, detail: {value: el.index, baseIndex}}));
                }
                break;
            }
        }
    }

    scrollTo(index) {
        this.$.chunker.scrollTo(index);
    }

    _autoScroll() {
        if (!this.autoScroll || !this.data || (this.data && this.data.selected) === null || this.__waitingForAutoScroll) {
            return;
        }

        // Debounce call
        this.__waitingForAutoScroll = true;
        requestAnimationFrame(() => {
            this.__waitingForAutoScroll = undefined;

            const selected = this.data.selected;

            if (typeof selected === 'number') {
                this.scrollTo(this.data.translateBaseIndexToIndex(selected));
            } else if (selected instanceof Array) {
                // Calculate the middle index of the viewport and checks which selected index is the closest
                const viewport = Math.round(this.$.chunker.startIx + ((this.$.chunker.endIx - this.$.chunker.startIx) / 2));

                const baseSelected = selected.map((index) => this.data.translateBaseIndexToIndex(index));

                const targetIndex = baseSelected.reduce((a, b) => {
                    return Math.abs(b - viewport) < Math.abs(a - viewport) ? b : a;
                });

                this.scrollTo(targetIndex);
            }
        });
    }

    _closeTooltip() {
        if (this.__tooltipEl) {
            this._tooltipLeave(this.__tooltipEl);
            this.__tooltipEl = null;
        }
    }

    _mouseTooltip(ev) {
        const el = this._getGridCell(ev);
        if (this.__tooltipEl === el) {
            return;
        }

        this._closeTooltip();

        if (el) {
            const unresolvedEl = el.querySelector('div.resolve:is([unresolved], [error])');
            if (unresolvedEl) {
                el.tooltip = unresolvedEl.hasAttribute('error') ? this.delayedErrorTooltip : this.delayedLoadingTooltip;
            }
            this.__tooltipEl = el;
            this._tooltipEnter(this.__tooltipEl, ev.clientX, ev.clientY, undefined, {showAnyway: true});
        }
    }

    _multiSelectionChanged(ev) {
        if (!this.shiftKeySelection) {
            return;
        }

        if (!this._shiftKey) { // setting the starting index
            this._multiselectStartIndex = ev.detail.index;
            this._multiselectStartState = ev.detail.selected;
            this._multiselectAppliedSelection = null;
        } else { // setting the ending index
            const startIndex = this._multiselectStartIndex;
            const endIndex = ev.detail.index;
            const lastAppliedIndex = this._multiselectAppliedSelection;
            if (startIndex === null || startIndex === endIndex || lastAppliedIndex === endIndex) {
                return;
            }

            const data = this.data;
            const applyChildren = data.isTreeGrid && !data.selectParentOnly;
            const isSelect = this._multiselectStartState;
            const isSelectingUp = endIndex > startIndex;
            const isLastAppliedIndex = lastAppliedIndex !== null;
            const isDisabled = this._disabledRowFunction() || (() => false);
            let selectionSet = new Set(data.selected ? data.selected : []);

            const collect = (low, high) => {
                const result = new Set();
                const process = (lo, hi, isBaseIndex) => {
                    for (let ix = lo; ix <= hi; ix++) {
                        const bi = isBaseIndex ? ix : data.baseIndex(ix);
                        if (!isDisabled(bi)) {
                            result.add(bi);
                        }
                        if (applyChildren) {
                            const range = data.childRange(bi);
                            if (Array.isArray(range)) {
                                process(range[0], range[1], true);
                            }
                        }
                    }
                };
                process(low, high);
                return result;
            };

            const select = (low, high) => {
                selectionSet = new Set([...selectionSet, ...collect(low, high)]);
            };

            const unselect = (low, high) => {
                selectionSet = selectionSet.difference(collect(low, high));
            };

            const update = isSelect ? select : unselect;
            const revert = isSelect ? unselect : select;
            let low, high;

            // reverting selections
            if (isLastAppliedIndex && isSelectingUp && lastAppliedIndex < startIndex ||
                isLastAppliedIndex && !isSelectingUp && startIndex < lastAppliedIndex) {
                low = Math.min(startIndex, lastAppliedIndex);
                high = Math.max(startIndex, lastAppliedIndex);
                revert(low, high);
            } else if (isLastAppliedIndex && isSelectingUp && endIndex < lastAppliedIndex ||
                isLastAppliedIndex && !isSelectingUp && lastAppliedIndex < endIndex) {
                low = Math.min(endIndex + 1, lastAppliedIndex);
                high = Math.max(endIndex - 1, lastAppliedIndex);
                revert(low, high);
            }

            // updating selections
            if (!isLastAppliedIndex || (isSelectingUp && lastAppliedIndex < startIndex) ||
                (!isSelectingUp && startIndex < lastAppliedIndex)) {
                low = Math.min(startIndex, endIndex);
                high = Math.max(startIndex, endIndex);
                update(low, high);
            } else if (isSelectingUp && lastAppliedIndex < endIndex || !isSelectingUp && endIndex < lastAppliedIndex) {
                low = Math.min(lastAppliedIndex, endIndex);
                high = Math.max(lastAppliedIndex, endIndex);
                update(low, high);
            }

            // verified end index selection
            update(endIndex, endIndex);

            this.data.setSelected([...selectionSet], true);
            this._multiselectAppliedSelection = endIndex;
        }
    }

    _multiSelectionClearStart() {
        this._multiselectStartIndex = null;
        this._multiselectStartState = null;
        this._multiselectAppliedSelection = null;
    }

    // Nav.rowFirst | Nav.cellFirst | Nav.cellOnly
    get _navigation() {
        return nav(this._treeToggle || false, this.navigation);
    }

    _unbadgeNewRow(el) {
        const row = this.highlightNewRows && el && el.closest('.new-row');
        const index = row ? this._rowIndex(row) : -1;
        if (index >= 0) {
            this._newRows.delete(this.data.item(index));
            row.classList.remove('new-row');
        }
    }

    _keyDown(ev) {
        this._shiftKey = ev.shiftKey && ev.key === 'Shift' ? true : this._shiftKey;

        if (ev.defaultPrevented || (ev.shiftKey && ev.key === 'Shift' && this.shiftKeySelection)) {
            return;
        }

        // Get the focused element
        const focusEl = this._getFocus();
        if (!focusEl) {
            // No element has focus. Try to bring a focused row into view
            this._scrollFocusedRowIntoView();
            return;
        }

        // Find key dispatcher
        const keyMethod = `__processKey${ev.key !== ' ' ? ev.key : 'Space'}`;
        if (typeof this[keyMethod] !== 'function') {
            return;
        }

        // Is the grid empty?
        const isGridEmpty = !this.data || this.data.length === 0;

        // Is the focused element in the header?
        const isInHeader = this._focusedRow === this.$.header;

        // Find all sub-focusable elements in the focused cell
        const cellActions = this._focusedCell && this._cellActions(this._focusedCell);

        // Find grid-action modes of current action (options: updown, tab, enter)
        const ga = this._focusedAction && this._focusedAction.getAttribute('grid-action');
        const navModes = ga ? ga.split(' ') : [];

        // If the current focus locked to an action in a cell? (Can only leave the cell via Escape or Tab)
        const lockedAction = this._focusedAction && cellActions && (cellActions.length > 1 || navModes.some(m => m === 'tab' || m === 'updown'));

        // Hack to select resize bar in header cell
        this.__keyDownSelectResizeBar = undefined;

        // Handle keyboard event
        const index = focusEl.index;
        const focusEl2 = this[keyMethod]({ev, focusEl, isInHeader, isGridEmpty, cellActions, navModes, lockedAction});
        if (focusEl === focusEl2) {
            if (index !== (focusEl2 && focusEl2.index)) {
                ev.preventDefault(); // Same element, but reused for other data item
            }
            return;
        }

        this._unbadgeNewRow(focusEl);

        switch (focusEl2) {
            case false:
            case null:
            case undefined:
                return;
            case true:
                ev.preventDefault();
                return;
        }

        // The keyboard processor wants us to focus on focusEl2
        if (focusEl2 instanceof Element) {
            const row = focusEl2.closest('.row');
            const cell = focusEl2.closest('.cell');
            if (row) {
                ev.preventDefault();
                if (row === this.$.header && this.__keyDownSelectResizeBar) {
                    console.assert(cell);
                    this._setFocusResizer(cell);
                } else {
                    // If the cell should be selected, and it has a default action, the action should always be selected instead
                    const action = focusEl2.closest('[grid-action]') || (cell && this._defaultCellAction(this._cellActions(cell)));
                    this._setFocus(row, cell, action);
                }
            }
        } else {
            console.error(focusEl2);
        }
    }

    __setResizerHeight(cellR, resizer) {
        const chunkerR = this.$.chunker.getBoundingClientRect();

        if (!this.data || this.data.length === 0) {
            // No real data in the grid, but the chunker might contain an "empty message"
            resizer.style.height = `${cellR.height}px`;
        } else if (this.$.chunker._itemsH < chunkerR.height) {
            // If there are less items than the chunker height then limit the height of the resizer
            resizer.style.height = `${cellR.height + this.$.chunker._itemsH}px`;
        } else {
            resizer.style.height = '';
        }
    }

    __setResizedCellEv(ev) {
        const cell = this._getGridCell(ev);
        if (!cell) {
            return;
        }

        const cellR = cell.getBoundingClientRect();
        const left = cellR.left;
        const posX = PTCS.getCoordinatesFromEvent(ev).posX;

        if (posX >= cellR.right - this.__resizerHitArea && posX <= cellR.right && this._isResizable(cell)) {
            this.$.resizer.setAttribute('hovered', '');
            this.style.cursor = 'ew-resize';

            this._resizedCell = cell;
        } else if (posX >= left && posX <= left + this.__resizerHitArea && this._isResizable(cell.previousSibling)) {
            this.$.resizer.setAttribute('hovered', '');
            this.style.cursor = 'ew-resize';

            this._resizedCell = cell.previousSibling;
        } else {
            this.$.resizer.removeAttribute('hovered');
            this.style.cursor = '';

            this._resizedCell = null;
        }
    }

    __mouseOverHeaderEv(ev) {
        if (this.__dragging || this.__resizing) {
            return;
        }

        this.__setResizedCellEv(ev);
    }

    __checkIfMouseLeftResizedCellEv(ev) {
        if (this.__resizing || !this._resizedCell) {
            return;
        }

        const cellR = this._resizedCell.getBoundingClientRect();
        if ((ev.targetTouches && ev.targetTouches.length === 0) || // We came from "touchend" event
            !(ev.clientY <= cellR.bottom && ev.clientY >= cellR.top && ev.clientX <= cellR.right + this.__resizerHitArea)) {
            this._resizedCell = null;
            this.$.resizer.removeAttribute('selected');
            this.$.resizer.removeAttribute('hovered');
        }
    }

    __resizeEv(ev) {
        const posX = PTCS.getCoordinatesFromEvent(ev).posX;
        const rR = this._resizedCell.getBoundingClientRect();
        const cNewWidth = posX - rR.left;
        this.__setCellWidthWithConstraints(this._resizedCell, cNewWidth);
        this.__setResizerHeight(rR, this.$.resizer);
        this._updateResizerPositions();
    }

    __stopResizeEv(ev) {
        if (PTCS.wrongMouseButton(ev)) {
            return;
        }
        window.removeEventListener('mousemove', this.__resize);
        window.removeEventListener('mouseup', this.__stopResize);
        window.removeEventListener('touchmove', this.__resize);
        window.removeEventListener('touchend', this.__stopResize);

        this.__resizing = false;

        const rRight = this._resizedCell.getBoundingClientRect().right;
        const hRight = this.$.header.getBoundingClientRect().right;

        // Check if we need to scroll to the new location
        if (rRight > hRight) {
            this.$.chunker.elScroll.scrollLeft = this.$.chunker.elScroll.scrollLeft + rRight - hRight;
        }

        this._updateWidthsInView();

        requestAnimationFrame(() => {
            const resizedCell = this._resizedCell;

            this.__checkIfMouseLeftResizedCell(ev);

            // TODO: The management of the grid resizers needs to be refactored.
            //       There are far too many explicit assignments of DOM properties (like this) spread out over the code.
            // Set keyboard focus on the resizer
            this.focus(preventScroll);
            this._resizedFocusedCellChanged(this._resizedFocusedCell);
            this._setFocusResizer(resizedCell);

            this.$['resizer-focus'].style.removeProperty('display');
        });
    }

    __resizeMouseUpEv(ev) {
        if (PTCS.wrongMouseButton(ev)) {
            return;
        }
        this.$.resizer.removeAttribute('selected');
    }

    __changeColumnWidth(dir, shiftKey) {
        const WIDTH_DELTA = 2;
        const delta = shiftKey ? 2 * WIDTH_DELTA : WIDTH_DELTA;
        const newWidth = this._resizedFocusedCell.getBoundingClientRect().width + (dir === 'right' ? delta : -delta);

        this.__setCellWidthWithConstraints(this._resizedFocusedCell, newWidth);
        this.__setResizerHeight(this._resizedFocusedCell.getBoundingClientRect(), this.$['resizer-focus']);

        this._updateWidthsInView();

        this._updateResizerPositions();
    }

    __retainColumnFocus(focus, cb) {
        // Save column number of cell element
        const colNo = PTCS.getChildIndex(focus === this._resizerFocusHandle ? this._resizedFocusedCell : this._focusedCell);

        // Do processing
        cb();

        // Move focus to old column in new row
        const focusRow = this._getChunkerFocusRow();
        if (focusRow) {
            return colNo >= 0 ? focusRow.children[colNo] : focusRow;
        }
        this._focusChildNo = colNo;
        return true; // preventDefault
    }

    // Return the resize bar for the cell
    __resizeBar(cell) {
        console.assert(cell.parentNode === this.$.header);
        // console.assert(this.__keyDownSelectResizeBar === undefined); -- can happen when ArrowLeft wrap jumps into the header
        this.__keyDownSelectResizeBar = true; // Tell _keyDown to select the resizer of this header cell
        return cell;
    }

    __moveFocusToNextCell(isGridEmpty) {
        const focusedCell = this._focusedCell.nextElementSibling;
        if (!focusedCell && !this.preventFocusRowWrap && !isGridEmpty) {
            const focusedRow = this._focusedRow;
            const focusEl2 = this.__processKeyArrowDown(arguments[0]);
            if (focusEl2 instanceof Element) {
                const row = focusEl2.closest('.row');
                return row !== focusedRow && (this._navigation === Nav.rowFirst ? row : row.firstElementChild);
            }
        }
        return focusedCell;
    }

    __processKeyArrowRight({ev, focusEl, isInHeader, isGridEmpty, cellActions, navModes, lockedAction}) {
        if (focusEl === this._resizerFocusHandle && this.$['resizer-focus'].hasAttribute('selected')) {
            this.__changeColumnWidth('right', ev.shiftKey);
            return true;
        }

        if (navModes.indexOf('tab') >= 0) {
            return false; // Cannot use ArrowRight to navigate current action
        }

        if (cellActions) {
            const idxOfAction = cellActions.findIndex(el => el === this._focusedAction) + 1;
            const action = lockedAction && cellActions[Math.min(idxOfAction, cellActions.length - 1)];

            // If action is last action in the same cell, move to next cell
            if (idxOfAction === cellActions.length && !isInHeader) {
                return this.__moveFocusToNextCell(isGridEmpty);
            } else if (action instanceof Element) {
                return action; // Found other action is same cell
            }
        }

        if (!isInHeader) {
            // If a row is focused, and it is collapsed, expands the current row
            // If a row is focused, and it is expanded, focuses the first cell in the row
            if (this._focusedRow && !this._focusedCell) {
                const index = this._rowIndex(this._focusedRow);
                const state = this.data.subTree(index);
                if (state === false || state === null) {
                    const toggle = this._focusedRow.querySelector('[part~=tree-toggle-icon]');
                    if (toggle && toggle._$activateToggle) {
                        toggle._$activateToggle(); // Animates toggle
                    } else {
                        this.data.subTree(index, true); // Expand whitout using toggle
                    }
                    return true;
                }

                return this._focusedRow.firstElementChild;
            }

            // If a cell is focused, moves one cell to the right.
            // If focus is on the right most cell, focus does not move --- (unless !this.preventFocusRowWrap)
            return this.__moveFocusToNextCell(isGridEmpty);

        }

        // In header, only focus on action elements or resizer
        let focusedCell;

        if (this._resizeColumns && this._focusedAction === this._resizerFocusHandle) {
            focusedCell = this._resizedFocusedCell.nextElementSibling;
        } else {
            if (this._isResizable(this._focusedCell)) {
                return this.__resizeBar(this._focusedCell);
            }
            focusedCell = this._focusedCell.nextElementSibling;
        }

        for (; focusedCell; focusedCell = focusedCell.nextElementSibling) {
            if (this._cellActions(focusedCell)) {
                return focusedCell;
            }
            if (this._isResizable(focusedCell)) {
                return this.__resizeBar(focusedCell);
            }
        }

        if (!this.preventFocusRowWrap && !isGridEmpty) {
            this.$.chunker.setFocusRowIndex(0);
            const focusRow = this._getChunkerFocusRow();
            if (focusRow) {
                return this._navigation === Nav.rowFirst ? focusRow : focusRow.firstElementChild;
            }
        }

        return null;
    }

    __processKeyArrowLeft({ev, focusEl, isInHeader, isGridEmpty, cellActions, navModes, lockedAction}) {
        if (focusEl === this._resizerFocusHandle && this.$['resizer-focus'].hasAttribute('selected')) {
            this.__changeColumnWidth('left', ev.shiftKey);
            return true;
        }

        if (navModes.indexOf('tab') >= 0) {
            return false; // Cannot use ArrowLeft to navigate current action
        }

        const action = lockedAction && cellActions[Math.max(cellActions.findIndex(el => el === this._focusedAction) - 1, 0)];
        if (action instanceof Element) {
            return action; // Found other action is same cell
        }

        let focusedCell;

        if (!isInHeader) {
            // If a row is focused, and it is expanded, collapses the current row.
            // If a row is focused, and it is collapsed, focus does not move.
            if (this._focusedRow && !this._focusedCell) {
                const index = this._rowIndex(this._focusedRow);
                if (this.data.subTree(index) === true) {
                    const toggle = this._focusedRow.querySelector('[part~=tree-toggle-icon]');
                    if (toggle && toggle._$activateToggle) {
                        toggle._$activateToggle(); // Animates toggle
                    } else {
                        this.data.subTree(index, false); // Collapse whitout using toggle
                    }
                    return true;
                }

                return null;
            }


            // If a cell in a [non first] column is focused, moves focus one cell to the left.
            focusedCell = this._focusedCell.previousElementSibling;
            if (focusedCell) {
                return focusedCell;
            }

            // If a cell in the first column is focused, focuses the row
            if ([Nav.rowFirst, Nav.cellFirst].indexOf(this._navigation) >= 0) {
                return this._focusedRow;
            }

            if (this.preventFocusRowWrap || isGridEmpty) {
                return null;
            }

            const focusRow = this._focusedRow;
            const focusEl2 = this.__processKeyArrowUp(arguments[0]);
            const focusRow2 = (focusEl2 instanceof Element) && focusEl2.closest('.row');
            if (!focusRow2 || focusRow2 === focusRow) {
                return null;
            }

            if (focusRow2 !== this.$.header) {
                // Still in grid
                return this._navigation === Nav.rowFirst ? focusRow2 : focusRow2.lastElementChild;
            }

            // Move into header
            focusedCell = focusRow2.lastElementChild;
        } else if (this._resizeColumns && this._focusedAction === this._resizerFocusHandle) {
            const cellActions2 = this._cellActions(this._resizedFocusedCell);
            if (cellActions2) {
                return cellActions2[cellActions2.length - 1];
            }
            focusedCell = this._resizedFocusedCell.previousElementSibling;
        } else {
            focusedCell = this._focusedCell.previousElementSibling;
        }

        // In header, only focus on action elements or resizer
        for (; focusedCell; focusedCell = focusedCell.previousElementSibling) {
            if (this._isResizable(focusedCell)) {
                return this.__resizeBar(focusedCell);
            }
            if (this._cellActions(focusedCell)) {
                return focusedCell;
            }
        }

        return null;
    }

    __processKeyArrowUp({focusEl, lockedAction, isInHeader}) {
        if (lockedAction || isInHeader) {
            return null; // Cannot use ArrowUp to navigate current action
        }
        const fi = this.$.chunker.focusedItemIndex - 1;
        if (fi >= 0) {
            return this.__retainColumnFocus(focusEl, () => this.$.chunker.setFocusRowIndex(fi));
        }

        // Move focus from table to header, if possible
        const focusedCell = this.$.header.children[PTCS.getChildIndex(this._focusedCell)] || this.$.header.firstElementChild;
        if (this._cellActions(focusedCell)) {
            return focusedCell;
        }
        if (this._isResizable(focusedCell)) {
            return this.__resizeBar(focusedCell);
        }

        return null;
    }

    __processKeyArrowDown({focusEl, lockedAction, isInHeader, isGridEmpty}) {
        if (lockedAction) {
            return null; // Cannot use ArrowDown to navigate current action
        }
        if (!isInHeader) {
            return this.__retainColumnFocus(focusEl, () => this.$.chunker.setFocusRowIndex(this.$.chunker.focusedItemIndex + 1));
        }

        // Move focus from header to table
        if (!isGridEmpty) {
            const r = this.__retainColumnFocus(focusEl, () => this.$.chunker.setFocusRowIndex(this.$.chunker.startIx));
            return (this._navigation === Nav.rowFirst && r instanceof Element) ? r.closest('.row') : r;
        }
        return null;
    }

    __processKeyHome({ev, focusEl, isInHeader, lockedAction}) {
        if (lockedAction) {
            return false; // Home cannot navigate focused action
        }

        if (ev.ctrlKey || !this._focusedCell) {
            return this.__retainColumnFocus(focusEl, () => this.$.chunker.setFocusRowIndex(0));
        }

        if (!isInHeader) {
            // Focus on first cell of row
            return this._focusedRow && this._focusedRow.firstElementChild;
        }

        // Move to first appropriate item in header
        for (let focusedCell = this.$.header.firstElementChild; focusedCell; focusedCell = focusedCell.nextElementSibling) {
            const cellActions2 = this._cellActions(focusedCell);
            if (cellActions2) {
                return cellActions2[0];
            }
            if (this._isResizable(focusedCell)) {
                return this.__resizeBar(focusedCell);
            }
        }

        return null;
    }

    __processKeyEnd({ev, focusEl, isInHeader, lockedAction}) {
        if (lockedAction) {
            return false; // End cannot navigate focused action
        }

        if (ev.ctrlKey || !this._focusedCell) {
            return this.__retainColumnFocus(focusEl, () => this.$.chunker.setFocusRowIndex(-1));
        }

        if (!isInHeader) {
            // Focus on first cell of row
            return this._focusedRow && this._focusedRow.lastElementChild;
        }

        // Move to last appropriate item in header
        for (let focusedCell = this.$.header.lastElementChild; focusedCell; focusedCell = focusedCell.previousElementSibling) {
            if (this._isResizable(focusedCell)) {
                return this.__resizeBar(focusedCell);
            }
            const cellActions2 = this._cellActions(focusedCell);
            if (cellActions2) {
                return cellActions2[0];
            }
        }

        return null;
    }

    __processKeySpace({ev}) {
        if (!this._focusedAction && this._focusedCell && this._toggleFocusedRow()) {
            return true;
        }

        // eslint-disable-next-line max-len
        if (ev.shiftKey && this.data.selectMethod === 'multiple' && this._focusedRow && this._focusedRow.hasOwnProperty('index') && !this._focusedAction) {
            // User can select items with Shift+Space
            const value = this._rowIndex(this._focusedRow);
            const baseIndex = this.data.baseIndex(value);
            if (baseIndex >= 0) {
                this.data.select(baseIndex); // Toggle selection
                this.dispatchEvent(new CustomEvent('row-click', {bubbles: true, composed: true, detail: {value, baseIndex}}));
            }
            return true;
        }

        return this.__processKeyEnter(arguments[0]);
    }

    __processKeyEnter({ev, focusEl, cellActions}) {
        if (focusEl === this._resizerFocusHandle) {
            if (this.$['resizer-focus'].hasAttribute('selected')) {
                this.$['resizer-focus'].removeAttribute('selected');
            } else {
                this.$['resizer-focus'].setAttribute('selected', '');
            }
            return true;
        }

        // eslint-disable-next-line max-len
        if (this.data.selectMethod === 'single' && this._focusedRow && this._focusedRow.hasOwnProperty('index') && !this._focusedAction && !(ev.key === 'Enter' && cellActions && cellActions.length > 0)) {
            const value = this._rowIndex(this._focusedRow);
            const baseIndex = this.data.baseIndex(value);
            if (baseIndex >= 0) {
                this.data.select(baseIndex); // Toggle selection
                this.dispatchEvent(new CustomEvent('row-click', {bubbles: true, composed: true, detail: {value, baseIndex}}));
            }
            return true;
        }

        // Toggle state?
        const toggleIcon = this._focusedCell && this._focusedCell.querySelector('[part~=tree-toggle-icon]');
        if (!(cellActions && (this._focusedAction === null || this._focusedAction !== toggleIcon)) && this._focusedCell && this._toggleFocusedRow()) {
            return true;
        }

        // Enter cell on Enter key (not Space)
        if (this._focusedAction || !cellActions || ev.key !== 'Enter') {
            return false;
        }

        this.__enterKeyAction = true; // Hack so the action don't react to the first keyup
        return cellActions[0]; // Focus on first cell action
    }

    __processKeyEscape({lockedAction}) {
        if (!lockedAction) {
            return false;
        }

        this._focusedAction = null;
        return true;
    }

    __processKeyTab({ev, cellActions, lockedAction}) {
        // In case focus is on locked sub-element, don't leave grid
        if (lockedAction) {
            const ae = this._focusedAction?.shadowRoot.activeElement;
            if (ae) {
                function expandSlots(acc, el) {
                    if (el instanceof HTMLSlotElement) {
                        acc.push(...getFocusable(el));
                    } else {
                        acc.push(el);
                    }
                    return acc;
                }
                const sub = [...getFocusable(this._focusedAction)].reduce(expandSlots, []);
                const i = sub.indexOf(ae);
                const action = i >= 0 && (ev.shiftKey ? sub[i - 1] : sub[i + 1]);
                if (action) {
                    action.focus();
                    ev.preventDefault();
                    return this._focusedAction;
                }
            }

            const i = cellActions && cellActions.indexOf(this._focusedAction);
            const action = i >= 0 && (ev.shiftKey ? cellActions[i - 1] : cellActions[i + 1]);
            if (action) {
                return action;
            }

            // Leave grid-action group
            this._focusedAction = null;
            return true;
        }

        // Move focus from core-grid (or at least try to)
        return ev.shiftKey ? delegateToPrev(this) : delegateToNext(this, true);
    }

    __processKeyPageUp(arg) {
        return this.__processKeyPage(arg);
    }

    __processKeyPageDown(arg) {
        return this.__processKeyPage(arg);
    }

    __processKeyPage({ev, focusEl, isInHeader, isGridEmpty, lockedAction}) {
        if (lockedAction) {
            return false; // PageUp / PageDown cannot navigate focused action
        }

        if (isInHeader) {
            const fi = ev.key === 'PageUp' ? this.$.chunker.startIx : this.$.chunker.endIx;
            // Move focus from header to table
            return isGridEmpty ? false : this.__retainColumnFocus(focusEl, () => this.$.chunker.setFocusRowIndex(fi));
        }

        const fi = this.$.chunker.focusedItemIndex;

        const focusEl2 = this.__retainColumnFocus(focusEl, () => this.$.chunker._keyDown(ev, true));

        if (fi === this.$.chunker.focusedItemIndex) {
            //  PageUp / PageDown stayed on same element - so we need a hack to show the tooltip
            this._closeTooltip();
            if (this._getFocus()) {
                this.__tooltipEl = this._getFocus();
                this._tooltipEnter(this.__tooltipEl, undefined, undefined, undefined, {showAnyway: true});
            }
        }

        return focusEl2;
    }

    /* Handle action with grid-action='enter': move focus to next action or back to cell level */
    _keyUp(ev) {
        this._shiftKey = ev.key === 'Shift' && ev.keyCode === 16 ? false : this._shiftKey;
        if (ev.defaultPrevented || ev.key !== 'Enter' || !this._focusedAction) {
            return;
        }

        /* Is this the keyup of the Enter-keydown that moved focus into the cell? If so, ignore */
        if (this.__enterKeyAction) {
            this.__enterKeyAction = false;
            return;
        }

        // User pressed Enter on an action. Is it an 'enter' action?
        const ga = this._focusedAction && this._focusedAction.getAttribute('grid-action');
        if (ga && ga.split(' ').indexOf('enter') >= 0) {
            const cellActions = this._focusedCell && this._cellActions(this._focusedCell);
            const i = cellActions && cellActions.findIndex(el => el === this._focusedAction);
            // Go to next action, if any, otherwise move focus to the cell
            this._focusedAction = cellActions && cellActions[i + 1];
            ev.preventDefault();
        }
    }

    _toggleFocusedRow() {
        const state = this.data.subTree(this._rowIndex(this._focusedRow));
        if (state !== undefined) {
            const toggle = this._focusedCell.querySelector('[part~=tree-toggle-icon]');
            if (toggle && toggle._$activateToggle) {
                toggle._$activateToggle(); // Animate toggle
                return true;
            }
        }
        return false;
    }

    _rowIndexFromPoint(x, y) {
        const el = this.shadowRoot.elementFromPoint(x, y);
        const row = el && el.closest('[part~=row]');
        return row && this._rowIndex(row);
    }

    _setHoverSibling(index) {
        if (this.__oldHoverRow) {
            this.__oldHoverRow.removeAttribute('next-row-hovers');
        }
        this.__oldHoverRow = this.$.chunker.getRow(index - 1);
        if (this.__oldHoverRow) {
            this.__oldHoverRow.setAttribute('next-row-hovers', '');
        }
    }

    get _hoverRow() {
        return this._$hoverRow;
    }

    set _hoverRow(row) {
        if (row && this._$hoverRow) {
            if (row.index !== this._$hoverRow.index) {
                this._setHoverSibling(row.index);
            }
        } else if (row) {
            this._setHoverSibling(row.index);
            // Need to update hover state if grid scrolls with fixed mouse position (e.g wheel scrolling)
            this.__hoverRowIID = setInterval(() => {
                const index = this._rowIndexFromPoint(this._$hoverRow.x, this._$hoverRow.y);
                if (index !== this._$hoverRow.index) {
                    this._$hoverRow.index = index;
                    this._setHoverSibling(index);
                }
            }, 250);
        } else {
            this._setHoverSibling(-1);
            clearInterval(this.__hoverRowIID);
        }
        this._$hoverRow = row;
    }

    get _pressedRow() {
        return this._$pressedRow;
    }

    set _pressedRow(row) {
        if (this._$pressedRow === row) {
            return;
        }
        if (this.__oldPressedPrevRow) {
            this.__oldPressedPrevRow.removeAttribute('next-row-pressed');
        }
        this.__oldPressedPrevRow = row > 0 && this.$.chunker.getRow(row - 1);
        if (this.__oldPressedPrevRow) {
            this.__oldPressedPrevRow.setAttribute('next-row-pressed', '');
        }
        this._$pressedRow = row;
    }

    _mouseOverGrid(ev) {
        this._hoverRow = {index: this._rowIndexFromPoint(ev.x, ev.y), x: ev.x, y: ev.y};
        if (this._pressedRow >= 0 && this._pressedRow !== this._hoverRow.index) {
            this._pressedRow = -1;
        }
    }

    _mouseLeaveGrid() {
        this._hoverRow = undefined;
        this._pressedRow = -1;
    }

    _mouseDownOnGrid(ev) {
        if (PTCS.wrongMouseButton(ev)) {
            return;
        }

        // Prevent double-click from selecting text
        if (ev.detail > 1 && this.preventDoubleClickedEvent) {
            ev.preventDefault();
        }

        this._pressedRow = this._rowIndexFromPoint(ev.x, ev.y);
        this._unbadgeNewRow(ev.target);
    }

    _mouseUpOnGrid(ev) {
        if (PTCS.wrongMouseButton(ev)) {
            return;
        }
        this._pressedRow = -1;
    }

    _gapChangedEv(ev) {
        this._gap = ev.detail.value;
    }

    _gapChanged(_gap) {
        const h0 = this.clientHeight;
        PTCS.setbattr(this, 'gap', _gap > 1); // Tell theme engine if there is a (not tiny) gap
        if (_gap > 0) {
            // Reduce grid height so gap is removed
            this.style.setProperty('--ptcs-core-grid-height', `${this.offsetHeight - Math.min(this.$.chunker.viewportHeight, _gap) + 1}px`);
        } else if (_gap < 0) {
            // Don't reduce height if scrollbar is visible. Let the grid grow
            this.style.removeProperty('--ptcs-core-grid-height');
        }
        const h1 = this.clientHeight;
        if (h0 !== h1) {
            // Avoid browser flashing by immediatly processing the new height (make the scroller fit without ever showing an incorrect height)
            this.$.chunker.resized();
        }
    }

    _repaintedView() {
        const {startIx, endIx} = this.$.chunker;

        if (this.__startIx !== startIx || this.__endIx !== endIx) {
            this.__startIx = startIx;
            this.__endIx = endIx;
            this.mainGrid.dispatchEvent(new CustomEvent('viewport-changed', {detail: {startIx, endIx}}));
        }
    }

    _resolvedValue(ev) {
        // Clear-out the delayed loading tooltip on ptcs-div grid cell
        ev.detail.cell.tooltip = undefined;
        this._unresolvedValue(); // Schedule a new scan for delayed values
    }

    _unresolvedValue() {
        if (!this.__recheckUnresolvedValues) {
            this.__recheckUnresolvedValues = true;
            requestAnimationFrame(() => {
                this.__recheckUnresolvedValues = undefined;
                const data = this.data;
                const rows = this.rowContainer.querySelectorAll('[part~=row]:has([unresolved])');
                const indexOfRow = this.__slottedRows
                    ? row => data.baseIndex(row.assignedSlot.parentElement.assignedSlot.parentElement.index)
                    : row => data.baseIndex(row.index);

                this.view.valueManager.setData(data); // Sometimes needed during initialization  ...
                this.view.valueManager.viewport = [...rows].map(indexOfRow);
            });
        }
    }

    _editedValue1(ev) {
        return !this.slottedRows && this._editedValue(ev);
    }

    _editedValue(ev, force) {
        const el = ev.target;
        const value = ev.detail.value;
        const validity = typeof el.getValidity === 'function' && el.getValidity();

        if (validity === 'unvalidated' && !force) {
            // Don't have a validation result yet
            let retried;
            const retry = () => {
                if (!retried) {
                    retried = true;
                    this._editedValue({target: el, detail: {value}}, true);
                }
            };

            el.addEventListener('validation-output-changed', retry, {once: true}); // Wait for validation change
            setTimeout(retry, 250); // But don't wait longer than 250ms (a hopefully reasonable timeout)
            return;
        }

        const cell = el.closest('.row > .cell');
        if (!cell) {
            return;
        }
        const row = cell.parentElement;
        const index = this._rowIndex(row);
        if (typeof index !== 'number') {
            return;
        }
        const baseIndex = this.data.baseIndex(index);
        if (!(0 <= baseIndex && baseIndex < this.data.baseLength)) {
            return;
        }
        const column = this.view.columns.filter(item => !item.hidden)[PTCS.getChildIndex(cell)];
        const field = column && column.editable;
        if (!field || typeof field !== 'string') {
            return;
        }
        const update = {[field]: value};

        let validation;
        if (validity === 'invalid') {
            const message = [el.validationMessage || 'Invalid'];
            if (el.validationCriteria) {
                message.push(el.validationCriteria);
            }
            validation = {[field]: message};
        }

        const done = () => {
            this.data.updateItem(index, update, validation);

            // Event that goes off whenever the user edits an item
            this.dispatchEvent(new CustomEvent('edit-item', {bubbles: true, composed: true, detail: {baseIndex, update, validation}}));

            // Get all changes to item before we try to submit them.
            const original = PTCS.clone(this.data.updatedBaseItem(baseIndex));

            // Submit change?
            switch (this.view.editLevel) {
                case 'cell':
                case 'row':
                    this.data.submitIfValid(baseIndex, field);
            }

            // NOTE: if _isItemUpdated(...) returns false, then all changes was submitted in the previous step.
            //       Otherwise there are validation errors.
            if (!this._isItemUpdated(baseIndex, field) && original) {
                // Remove unwanted property
                delete original.$validation;
            }

            // Inform client about submitted change! regardless validation status - aligned to legacy grid behaviors
            this.dispatchEvent(new CustomEvent('edit-item-completed', {
                bubbles:  true,
                composed: true,
                detail:   {baseIndex, field, item: this.data.baseItem(baseIndex), original}}));
        };

        if (this.view && this.view.findDependentChanges) {
            const item = this.data.item(index);
            const promise = this.view.findDependentChanges(item, update, field, baseIndex, this.data);
            if (promise instanceof Promise) {
                promise.then(reset => {
                    if (reset) {
                        for (const _field in reset) {
                            this.view.columns.forEach(col => {
                                if (col.editable === _field) {
                                    const v = reset[_field].value;
                                    update[_field] = col.encode ? col.encode(v, item, baseIndex, this.data) : v;
                                }
                            });
                        }
                    }
                    done();
                }, error => console.error(error));

                return; // The Promise calls done()
            }
        }

        done();
    }

    _highlightNewRowsChanged(highlightNewRows) {
        if (!this.view) {
            return;
        }

        this.view.highlightNewRows = highlightNewRows;

        // Need a full rebuild and empty the recycled elements.
        if (this.view.columns) {
            this.dvChanged();
        }
    }

    _isItemUpdated(baseIndex, field) {
        const updated = this.data.updatedBaseItem(baseIndex);
        if (!updated) {
            return false;
        }
        const item = this.data.baseItem(baseIndex);
        if (!item) {
            return false;
        }
        if (field) {
            return updated.hasOwnProperty(field) && updated[field] !== item[field];
        }
        for (const f in updated) {
            if (item.hasOwnProperty(f) && updated[f] !== item[f]) {
                return true;
            }
        }
        return false;
    }

    _editActivated(ev) {
        if (PTCS.wrongMouseButton(ev) || ev.target.disabled) {
            return;
        }

        // Find column that contains the edit control
        const cell = ev.target.closest('[part~=body-cell]');
        const colNo = PTCS.getChildIndex(cell);
        const colDef = this._getVisibleColDef(colNo);
        if (!colDef) {
            return;
        }
        const baseIndex = this.data.baseIndex(this._rowIndex(cell.parentNode));
        if (baseIndex === -1) {
            return; // This row is not editable (bug?)
        }

        const item = this.data.baseItem(baseIndex);

        // In "row" edit mode, the user has clicked on a column that don't specify editable field.
        // Therefore the launced editor becomes a row editor
        const field = colDef.editable;

        this._launchEditor(baseIndex, item, field, colNo, cell, this.rowEditFormTitle, this.updateButtonText);
    }

    _launchEditor(baseIndex, item, field, colNo, cellEl, title, updateButtonText, parentBaseIndex) {
        // Create inline editor (only once)
        if (!this._gridEditor) {
            this._gridEditor = document.createElement('ptcs-edit-grid-cells');
            this._gridEditor.setAttribute('part', 'grid-edit-cells');
            this._gridEditor.setAttribute('tabindex', '-1');
            this._gridEditor.style.position = 'absolute';
            this._gridEditor.style.left = '0px';
            this._gridEditor.style.top = '0px';
            this._gridEditor.addEventListener('close', this._editDone.bind(this));
            this._gridEditor.addEventListener('blur', this._closeEditByBlur.bind(this));
            this._gridEditor.__ro = new ResizeObserver(() => this._placeEditor(this._gridEditor.__lastCellEl));
        }

        // Keep track of last clicked element
        this._gridEditor.__lastCellEl = cellEl;

        // Updated data (need validation messages)
        const updated = this.data.updatedBaseItem(baseIndex);

        // Compute parent label
        const parentItem = this.data.isTreeGrid && parentBaseIndex >= 0 && this.data.baseItem(parentBaseIndex);
        const rows = (parentItem && this.view) && this.view.getRowDef(parentItem);
        const col = rows && rows.find(def => !def.hidden && def.treeToggle);
        const theParentLabel = parentItem ? (col && (col.select(parentItem) || '')) : (this.data.isTreeGrid && null);

        // Assign data to editor
        this._gridEditor.setProperties({
            label:       title,
            columns:     this.view.columns.filter(_item => !_item.hidden),
            field,
            item,
            baseIndex,
            colNo,
            parentBaseIndex,
            theParentLabel,
            dataManager: this.data,
            viewManager: this.view,

            // Validation setup
            validation:             updated && updated.$validation,
            hideValidationError:    this.hideValidationError,
            hideValidationCriteria: this.hideValidationCriteria,
            hideValidationSuccess:  this.hideValidationSuccess,
            validationErrorIcon:    this.validationErrorIcon,
            validationSuccessIcon:  this.validationSuccessIcon,
            validationCriteriaIcon: this.validationCriteriaIcon,
        });

        // Add / Update button
        if (updateButtonText) {
            this._gridEditor.updateButtonText = updateButtonText;
        }

        // Other labels
        setOfLabels.forEach(label => {
            if (this[label]) {
                this._gridEditor[label] = this[label];
            }
        });


        // Component id, if specified
        if (this.externalComponentId) {
            this._gridEditor.setAttribute('id', this.externalComponentId);
        }

        // Open inline editor
        this.setAttribute('modal', '');
        if (!this._gridEditor.parentElement) {
            document.body.appendChild(this._gridEditor);
            this._gridEditor.__ro.observe(this._gridEditor);
        }
        this._gridEditor.style.visibility = 'hidden';
        this._gridEditor.updateComplete.then(() => {
            this._placeEditor(cellEl);

            // Wait until grid editor is not being resized any longer
            let __pe;
            const f = () => {
                if (__pe === this._gridEditor.__pe) {
                    this._gridEditor.style.visibility = '';
                    // Move focus to (first) editable item (after some stabilization)
                    this._gridEditor.updateComplete.then(() => requestAnimationFrame(() => this._gridEditor.initFocus()));
                } else {
                    __pe = this._gridEditor.__pe;
                    setTimeout(f, 60);
                }
            };

            setTimeout(f, 80);
        });

        this.__closeEdit = (ev) => this._closeEditByOutsideClick(ev);
        window.addEventListener('mousedown', this.__closeEdit);

        // Inform client if the editing of this data just started
        if (!this._isItemUpdated(baseIndex, field)) {
            this.dispatchEvent(new CustomEvent('edit-item-started', {bubbles: true, composed: true, detail: {baseIndex, field, item}}));
        }
    }

    _validationErrorIconChanged(validationErrorIcon) {
        const iconElement = this.rowContainer.querySelectorAll('[part=invalid-icon]');
        iconElement.forEach(elem => {
            elem.icon = validationErrorIcon || defaultErrorIcon;
        });
    }

    _placeEditor(cell) {
        const body = document.body;
        const docEl = document.documentElement;
        const scrollTop = window.pageYOffset || docEl.scrollTop || body.scrollTop;
        const scrollLeft = window.pageXOffset || docEl.scrollLeft || body.scrollLeft;
        const clientTop = docEl.clientTop || body.clientTop || 0;
        const clientLeft = docEl.clientLeft || body.clientLeft || 0;
        const bb = this._gridEditor.getBoundingClientRect();

        // Arbitrary: leave at least 8px between left and top sides of browser window
        const minX = scrollLeft + 8;
        const minY = scrollTop + 8;

        // Arbitrary: leave at least 16px between right and bottom sides of browser window
        const maxX = scrollLeft + window.innerWidth - bb.width - 16;
        const maxY = scrollTop + window.innerHeight - bb.height - 16;

        // Prefered position
        let x, y;

        if (this._gridEditor.field) {
            // Cell editing
            const bbCell = cell.getBoundingClientRect();
            const cs = getComputedStyle(this._gridEditor);
            const dx = PTCS.cssDecodeSize(cs.getPropertyValue('--ptcs-offset-x'), this._gridEditor);
            const dy = PTCS.cssDecodeSize(cs.getPropertyValue('--ptcs-offset-y'), this._gridEditor, true);

            x = bbCell.left + (isNaN(dx) ? 0 : dx) + scrollLeft - clientLeft;
            y = bbCell.top + (isNaN(dy) ? 0 : dy) + scrollTop - clientTop;

        } else {
            // Row editing
            const bbGrid = this.getBoundingClientRect();

            x = bbGrid.right - (bb.right - bb.left) + scrollLeft - clientLeft;
            y = bbGrid.top + scrollTop - clientTop;
        }

        this._gridEditor.style.transform = `translate(${Math.max(Math.min(x, maxX), minX)}px, ${Math.max(Math.min(y, maxY), minY)}px)`;

        // Count number of calls
        this._gridEditor.__pe = (this._gridEditor.__pe || 0) + 1;
    }

    _adjustEditorPlace() {
        if (!this.__closeEdit) {
            return; // Editor is not open
        }
        const row = this.$.chunker.getRow(this.data.translateBaseIndexToIndex(this._gridEditor.baseIndex));
        const cell = row && row.children[this._gridEditor.colNo];

        // Is cell visible or are we creating a new row?
        if (cell || this._gridEditor.baseIndex === newRowBaseIndex) {
            this._placeEditor(cell);
        } else {
            this._gridEditor.save();
        }
    }

    _closeEditByBlur() {
        requestAnimationFrame(() => {
            if (!this.__closeEdit) {
                return; // Already detached
            }
            if (document.activeElement.matches('ptcs-datepicker-calendar, ptcs-list[is-dropdown]')) {
                return; // Ignore this blur event. It is (probably) part of the inline editing process
            }
            this._gridEditor.save();
        });
    }

    _closeEditByOutsideClick(ev) {
        if (PTCS.wrongMouseButton(ev)) {
            return;
        }
        if (ev.target.matches('ptcs-edit-grid-cells, ptcs-datepicker-calendar, ptcs-list[is-dropdown]')) {
            return; // Ignore this click. It is (probably) part of the inline editing process
        }
        this._gridEditor.save();
    }

    _editDone(ev, focus) {
        if (!this.__closeEdit) {
            return; // Already closed
        }

        const {baseIndex, item, field, parentBaseIndex} = this._gridEditor;

        // Close editor and return focus to grid
        window.removeEventListener('mousedown', this.__closeEdit);
        this.__closeEdit = undefined;
        document.body.removeChild(this._gridEditor);
        this._gridEditor.__ro.unobserve(this._gridEditor);
        this.removeAttribute('modal');
        if (focus !== false) {
            requestAnimationFrame(() => (this._focusedAction || this).focus(preventScroll));
        }

        // Get update that should be applied to data manager
        const update = ev.detail && ev.detail.values;
        if (!update) {
            if (!this._isItemUpdated(baseIndex, field)) {
                // Update client about cancelled change
                if (ev.detail?.action === 'cancel') {
                    this.dispatchEvent(new CustomEvent('edit-item-cancelled', {
                        bubbles:  true,
                        composed: true,
                        detail:   {baseIndex, field, item}}));
                } else {
                    // even when no change is done but the user clicks on update button/ presses Enter key
                    this.dispatchEvent(new CustomEvent('edit-item-completed', {
                        bubbles:  true,
                        composed: true,
                        detail:   {baseIndex, field, item}}));
                }
            }
            return;
        }

        // Apply the requested update
        this._applyEditUpdate(baseIndex, item, field, update, ev.detail.validation, parentBaseIndex);
    }

    async _applyEditUpdate(baseIndex, item, field, update, validation, parentBaseIndex) {
        // Adding a new row?
        if (baseIndex === newRowBaseIndex) {
            // Adding first item? If so, the empty message must be removed first
            if (this.data.length === 0 && this.$.chunker.numItems === 1) {
                this.$.chunker.numItems = 0;
            }
            const newItem = Object.assign({}, item, update);
            if (this.highlightNewRows) {
                this._newRows.add(newItem); // Add in advance, so it is available when grid renders new item
            }

            // Clear filter and resort
            this.dispatchEvent(new CustomEvent('new-row'));
            this.data.filter = null;

            const index = await this.__insertGridRow(newItem, validation, parentBaseIndex);
            if (!(index >= 0)) {
                console.error('internal error');
                return;
            }

            // Now we can get the new items baseIndex
            baseIndex = this.data.baseIndex(index);

            setTimeout(() => {
                // Focus on new item and scroll it into view (why so complex? translateBaseIndexToIndex can be expensive)
                this.$.chunker.setFocusRowIndex(this.data.baseIndex(index) === baseIndex ? index : this.data.translateBaseIndexToIndex(baseIndex));
            }, 300);

        } else {
            const index = this.data.translateBaseIndexToIndex(baseIndex);
            if (item !== this.data.baseItem(baseIndex)) {
                console.error('data item has changed during editing');
                return;
            }
            this.data.updateBaseItem(baseIndex, index, update, validation);
        }

        // Event that goes off whenever the user edits an item
        this.dispatchEvent(new CustomEvent('edit-item', {bubbles: true, composed: true, detail: {baseIndex, update, validation}}));

        // Get all changes to item before we try to submit them. Note: this returns the original values
        const itemUpdated = PTCS.clone(this.data.updatedBaseItem(baseIndex));

        // Submit change?
        switch (this.view.editLevel) {
            case 'cell':
            case 'row':
                this.data.submitIfValid(baseIndex, field);
        }

        // NOTE: if _isItemUpdated(...) returns false, then all changes was submitted in the previous step.
        //       Otherwise there are validation errors.
        if (!this._isItemUpdated(baseIndex, field) && itemUpdated) {
            // Remove unwanted property
            delete itemUpdated.$validation;
        }

        // Inform client about submitted change! regardless validation status - aligned to legacy grid behaviors
        this.dispatchEvent(new CustomEvent('edit-item-completed', {
            bubbles:  true,
            composed: true,
            detail:   {baseIndex, field, item, original: itemUpdated}}));
    }

    async __insertGridRow(item, validation, parentBaseIndex) {
        const data = this.data;

        if (parentBaseIndex >= 0) {
            const f = () => {
                switch (data.toggleState(parentBaseIndex)) {
                    case -1:
                        return -1;

                    case false:
                        // Has hidden children
                        data.subTree(data.translateBaseIndexToIndex(parentBaseIndex), true);
                        return data.insertTreeItem(item, data.childRange(parentBaseIndex)[1], 'after', validation);

                    case true:
                        // Has visible children
                        return data.insertTreeItem(item, data.childRange(parentBaseIndex)[1], 'after', validation);

                    case undefined:
                        // Has not children (leaf)
                        return data.insertTreeItem(item, parentBaseIndex, 'child', validation);

                    case null:
                        data.subTree(data.translateBaseIndexToIndex(parentBaseIndex), true);
                        break;
                }
                return undefined;
            };

            // Poll toggle until we have a result
            const fwait = resolve => {
                requestAnimationFrame(() => {
                    const index = f();
                    if (index !== undefined) {
                        resolve(index);
                    } else {
                        fwait(resolve);
                    }
                });
            };

            const index = f();
            return index !== undefined ? index : new Promise(fwait);
        }

        return data.insertItem(item, undefined, validation);
    }

    // Add new row. Start from (optional) item0
    addRow(item) {
        const _item = Object.assign({}, item || {});
        const selected = this.data.isTreeGrid && this.data.selected;
        const bi  = Array.isArray(selected) ? (selected.length === 1 && selected[0]) : (typeof selected === 'number' && selected);
        const parentBaseIndex = bi !== false ? bi : undefined;
        this._launchEditor(newRowBaseIndex, _item, undefined, -1, null, this.rowEditFormAddTitle, this.addButtonText, parentBaseIndex);
    }
};

customElements.define(PTCS.CoreGrid.is, PTCS.CoreGrid);
