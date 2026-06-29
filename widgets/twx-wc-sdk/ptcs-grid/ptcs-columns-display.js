import {LitElement, html, css} from 'lit';
import {map} from 'lit/directives/map.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-icon/ptcs-icon.js';
import 'ptcs-icons/cds-icons.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-checkbox/ptcs-checkbox.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import {columnName} from './grid-view.js';
import {delegateToPrev} from 'ptcs-behavior-focus/ptcs-behavior-focus.js';

// Dragging mode for reorder column element
const startDragging = column => column.classList.add('dragging');
const stopDragging = column => column.classList.remove('dragging');
const isDragging = column => column.classList.contains('dragging');


PTCS.ColumnsDisplay = class extends PTCS.BehaviorTooltip(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement)))) {
    static get styles() {
        return css`
        :host([mode=closed]) {
            display: none;
        }

        .column-defs {
            outline: none;
            overflow: auto;
        }

        .column-def {
            width: 100%;
            box-sizing: border-box;
            display: flex;
            flex-direction: row;
            align-items: stretch;
        }

        .draggable {
            display: none;
            position: absolute;
        }

        .draggable.dragging {
            display: flex;
            cursor: grabbing;
        }

        ptcs-button {
            width: 100%;
        }

        [hidden] {
            display: none;
        }

        [part~="col-icon"][non-reorderable] {
            visibility: hidden;
        }

        .label-row {
            flex: 1 1 auto;
            flex-direction: row;
        }`;
    }

    render() {
        return html`
            <div class="column-defs" id="columns" tabindex=${this._delegatedFocus}
                @touchstart=${this._mouseDownColumnDef} @mousedown=${this._mouseDownColumnDef}
                @mousemove=${this._mouseMoveColumnDef} @mouseout=${this._mouseOutColumnDef}
            >${map(this._columns, (item, index) => html`
                <div class="column-def" part="column-def" .nonReorderable=${!this._isIconHidden() && item.nonReorderable}>
                    <ptcs-icon part="col-icon" ?hidden=${this._isIconHidden()} icon="cds:icon_drag_handle_mini"
                        ?non-reorderable=${item.nonReorderable}></ptcs-icon>
                    <ptcs-checkbox no-tabindex part="col-checkbox" ?hidden=${this._isCheckboxHidden()}
                        .label=${this._getCheckboxLabel(item.label)} class="label-row" .index=${index}
                        .checked=${!item.hidden} @checked-changed=${this._checkedChanged}></ptcs-checkbox>
                    <ptcs-label part="col-label" ?hidden=${this._isLabelHidden()} .label=${item.label}
                        variant="label" class="label-row" vertical-alignment="center"></ptcs-label>
                </div>`)}
                <div id="draggable" class="column-def draggable" part="column-def">
                    <ptcs-icon part="col-icon" ?hidden=${this._isIconHidden()} icon="cds:icon_drag_handle_mini"></ptcs-icon>
                    <ptcs-checkbox no-tabindex id="draggable-checkbox" part="col-checkbox" ?hidden=${this._isCheckboxHidden()}
                        class="label-row"></ptcs-checkbox>
                    <ptcs-label id="draggable-label" part="col-label" ?hidden=${this._isLabelHidden()} variant="label"
                        class="label-row" vertical-alignment="center"></ptcs-label>
                </div>
            </div>
            <div id="actions" part="actions">
                <ptcs-button id="apply" part="apply" variant="primary" .label=${this.applyButtonText}
                             @action=${this._apply} tabindex=${this._delegatedFocus}></ptcs-button>
                <ptcs-button id="cancel" part="cancel" variant="tertiary" .label=${this.cancelButtonText}
                             @action=${this._cancel} tabindex=${this._delegatedFocus}></ptcs-button>
            </div>`;
    }

    static get is() {
        return 'ptcs-columns-display';
    }

    static get properties() {
        return {
            view: {
                type: Object
            },

            options: {
                type:    String,
                value:   'both',
                reflect: true
            },

            visibleItems: {
                type:      Number,
                value:     6,
                attribute: 'visible-items'
            },

            mode: {
                type:     String,
                value:    'closed',
                reflect:  true,
                observer: '_modeChanged',
                notify:   true
            },

            // Focused column
            _focusedColumnIx: {
                type:  Number,
                value: 0
            },

            // Focused Item (0 = drag icon, 1 = checkbox)
            _focusedItemIx: {
                type:  Number,
                value: 0
            },

            _colH: {
                value: 34
            },

            _delegatedFocus: {
                type:  String,
                value: null
            },

            // Tooltip for the reorder icon
            tooltipReorder: {
                type: String
            },

            // Tooltip for the show checkbox
            tooltipShow: {
                type: String
            },

            // 'Apply' button label
            applyButtonText: {
                type:  String,
                value: 'Apply'
            },

            // 'Cancel' button label
            cancelButtonText: {
                type:  String,
                value: 'Cancel'
            },

            // Are we dragging the column display item up or down?
            _up: {
                type:    Boolean,
                reflect: true
            },

            maxColumnsHeight: {
                type: Number
            }
        };
    }

    static get observers() {
        return ['_setTooltipByFocus(_focusedItemIx, _focusedColumnIx)'];
    }

    ready() {
        super.ready();

        this.addEventListener('keydown', this._keyDown.bind(this));
        this._trackFocus(this.$.columns, this._focusedElement.bind(this));
    }

    _focusedElement() {
        if (this._focusedColumnIx < 0 || !Array.isArray(this._columns) || this._focusedColumnIx > this._columns.length - 1) {
            return null; // No focus
        }
        if (this.options === 'both') {
            return this.$.columns.children[this._focusedColumnIx].querySelector(this._focusedItemIx === 0 ? 'ptcs-icon' : '.label-row');
        }
        return this.$.columns.children[this._focusedColumnIx];
    }

    _loadColumns() {
        const columnsDef = this.view && this.view.columnsDef;
        if (!columnsDef) {
            return [];
        }
        let nonReorderableG = true;
        const columns = PTCS.decodeViewExpr(this.view.getVisibilityExpression()).map(([name, show]) => {
            const col = columnsDef.find(_col => columnName(_col) === name);
            nonReorderableG = nonReorderableG && col.nonReorderable;
            return {col, name, hidden: show === 'false', nonReorderable: nonReorderableG};
        });

        if (columns.length === columnsDef.length && columns.every(c => c.col) && columnsDef.every(d => columns.find(c => c.col === d))) {
            return columns.map(({col, hidden, nonReorderable}) => ({label: col.label, name: col.name, hidden, nonReorderable}));
        }
        // Aw... The visibility expression doesn't match the columns. Fallback
        const hidden = columns.reduce((a, c) => {
            a[c.name] = c.hidden;
            return a;
        }, {});

        // Get the interesting properties from the view column definitions
        return columnsDef.map(col => ({label: col.title, name: col.name, hidden: hidden[columnName(col)], nonReorderable: col.nonReorderable}));
    }

    _apply() {
        if (this.view) {
            // Set new visibility
            this.dispatchEvent(new CustomEvent('columns-changed', {detail: {
                columns: PTCS.encodeViewExpr(this._columns.map(col => [columnName(col), `${!col.hidden}`]))
            }}));
        }

        this.mode = 'closed';
        delegateToPrev(this);
    }

    _cancel() {
        this.mode = 'closed';
        this._columns = this._loadColumns(); // Restore default values
        delegateToPrev(this);
    }

    // If only one columns remains visible then its show/hide checkbox should be disabled
    _checkDisabled() {
        const numVisible = this._columns.reduce((num, col) => num + (col.hidden ? 0 : 1), 0);
        const visibleIndex = numVisible ? this._columns.findIndex(col => !col.hidden) : -1;

        this.$.columns.querySelectorAll('[part="col-checkbox"]').forEach((el, i) => {
            el.disabled = (numVisible === 1 && i === visibleIndex);
        });
    }

    _modeChanged(mode) {
        if (mode === 'open') {
            this.style.visibility = 'hidden';

            this._columns = this._loadColumns();

            this._checkDisabled();

            // Make sure the focused item shows a tooltip
            this._closeTooltip();
            this._focusedColumnIx = -1;

            requestAnimationFrame(() => {
                if (this.visibleItems && !isNaN(this.visibleItems) && this.visibleItems < this._columns.length) {
                // Get the height of one column item
                    this._colH = this.$.columns.children[0].getBoundingClientRect().height;
                    this.$.columns.style.height = `${this._colH * this.visibleItems}px`;
                } else {
                    this.$.columns.style.height = '';
                }

                this.$.columns.scrollTop = 0;

                // Focus on the first column
                this._focusedColumnIx = this._focusedItemIx = 0;
                this.style.visibility = '';

                const actionsArea = this.$.actions.getBoundingClientRect();
                this.$.columns.style.maxHeight = (this.maxColumnsHeight - actionsArea.height) + 'px';
            });
        } else {
            this._cleanDraggingStates();
        }
    }

    _cleanDraggingStates() {
        this.$.columns.querySelectorAll('.dragging').forEach((el) => {
            stopDragging(el);
        });
    }

    _isLabelHidden() {
        return this.options !== 'reorder';
    }

    _isCheckboxHidden() {
        return !this._isLabelHidden();
    }

    _isIconHidden() {
        return this.options === 'show';
    }

    _getCheckboxLabel(label) {
        return this._isLabelHidden() ? label : '';
    }

    _checkedChanged(ev) {
        const index = ev.target.index;
        const hidden = !ev.target.checked;
        if (!Array.isArray(this._columns) || this._columns[index].hidden === hidden) {
            return;
        }

        this._columns[index].hidden = hidden;
        this._checkDisabled();
        this.requestUpdate();
    }

    _showTooltip(ev, delay) {
        const tooltipEl = (!this._isIconHidden() && ev.target.closest('ptcs-icon')) ||
                          (!this._isCheckboxHidden()) && ev.target.closest('ptcs-checkbox');
        if (tooltipEl === this.__tooltipEl) {
            return;
        }

        const tooltip = tooltipEl && (tooltipEl.tagName === 'PTCS-ICON' ? this.tooltipReorder : this.tooltipShow);
        if (tooltip) {
            this.__tooltipEl = tooltipEl;
            this._tooltipEnter(this.__tooltipEl, ev.clientX, ev.clientY, tooltip, {showAnyway: true, delay});
        } else {
            this._closeTooltip();
        }
    }

    _closeTooltip() {
        if (this.__tooltipEl) {
            this._tooltipLeave(this.__tooltipEl);
            this.__tooltipEl = null;
        }
    }

    _setTooltipByFocus(_focusedItemIx /* , _focusedColumnIx */) {
        const target = this._focusedElement();
        this._closeTooltip();
        if (target) {
            this._showTooltip({target}, 25);
        }
    }

    _moveColumn(curr, ix) {
        if (curr === ix) {
            return;
        }

        this._columns.splice(ix, 0, this._columns.splice(curr, 1)[0]);

        this._checkDisabled(); // is it really needed on reorder?
        this.requestUpdate();
    }

    _mouseMoveColumnDef(ev) {
        this._showTooltip(ev);

        if (this.options === 'show') {
            return;
        }

        const cell = ev.target.closest('.column-def');
        if (!cell) {
            return;
        }

        if (!cell.nonReorderable &&
            ((ev.target.tagName === 'PTCS-ICON' && this.options === 'both') || (this.options === 'reorder'))) {
            cell.setAttribute('hovered', '');
            cell.style.cursor = 'grab';
        } else {
            cell.removeAttribute('hovered');
            cell.style.cursor = '';
        }
    }

    _mouseOutColumnDef(ev) {
        const cell = ev.target.closest('.column-def');
        if (!cell) {
            return;
        }

        cell.removeAttribute('hovered');
    }

    // Reorder columns
    _mouseDownColumnDef(ev) {
        if (PTCS.wrongMouseButton(ev)) {
            return;
        }

        // Click on icon or on checkbox / label?
        this._focusedItemIx = ev.target.tagName === 'PTCS-ICON' ? 0 : 1;

        const cell = ev.target.closest('.column-def');
        if ((cell && cell.nonReorderable) ||
            (ev.target.tagName !== 'PTCS-ICON' && this.options === 'both') || this.options === 'show') {
            // Set focus on clicked element
            this._focusedColumnIx = PTCS.getChildIndex(cell);
            return;
        }

        const y0 = PTCS.getCoordinatesFromEvent(ev).posY;
        const touch = (ev.type === 'touchstart');
        // eslint-disable-next-line prefer-const
        let dMin, dMax, yMin, yMax;
        let cells, firstDragTopPos, firstDragInx;
        let curr;

        const mouseMoveEv = touch ? 'touchmove' : 'mousemove';
        const mouseUpEv = touch ? 'touchend' : 'mouseup';

        this._cleanDraggingStates();

        // Loose columns focus on mouse down
        this._focusedColumnIx = -1;

        const all = cell.closest('.column-defs').querySelectorAll('.column-def:not(.draggable)');

        // Fill the dimensions of the column cells in an array
        const fillA = () => {
            cells = [];
            firstDragTopPos = null;
            firstDragInx = null;

            for (let i = 0; i < all.length; i++) {
                const el = all[i];
                const bb = el.getBoundingClientRect();

                cells.push({el, top: bb.top, bottom: bb.bottom, dragable: !el.nonReorderable});

                if (firstDragTopPos === null && !el.nonReorderable) {
                    firstDragTopPos = bb.top;
                    firstDragInx = i;
                }

                if (el === cell) {
                    curr = i;
                }
            }
        };

        const find = (y) => {
            const ix = cells.findIndex((item) => item.dragable && item.top - (this._colH / 2) <= y && y < item.bottom + (this._colH / 2));
            // eslint-disable-next-line no-nested-ternary
            return ix >= 0 ? ix : (y < firstDragTopPos ? firstDragInx : cells.length - 1);
        };

        const mouseMoveColumnDef = (ev2) => {
            const posY = PTCS.getCoordinatesFromEvent(ev2).posY;

            // Drag direction: up or down?
            this._up = (y0 - posY) > 0;

            // Delta how much you are far from the first mousedown
            const d = Math.min(Math.max(dMin, posY - y0), dMax);

            this.$.draggable.style.transform = `translateY(${d}px)`;

            const dR = this.$.draggable.getBoundingClientRect();

            // Check if we need to scroll to the new location
            if (dR.bottom >= yMax - 1) {
                this.$.columns.scrollTop += (posY - dR.top) / 2;
            } else if (dR.top <= yMin + 1) {
                this.$.columns.scrollTop -= (dR.bottom - posY) / 2;
            }
            fillA();

            const ix = find(dR.bottom);
            cells.forEach((item, i) => {
                if (i === ix) {
                    item.el.classList.add('droppable');
                } else {
                    item.el.classList.remove('droppable');
                }
            });

            ev2.preventDefault();

            // Stop the event here. Otherwise you will e.g. see grid resizers.
            ev2.stopPropagation();
        };

        const mouseUpColumnDef = () => {
            window.removeEventListener(mouseMoveEv, mouseMoveColumnDef, true);
            const dR = this.$.draggable.getBoundingClientRect();

            const ix = find(dR.bottom);

            cells.forEach(item => {
                item.el.style.transform = '';
                item.el.classList.remove('droppable');
            });

            if (ix !== curr) {
                this._moveColumn(curr, ix);
            }

            stopDragging(this.$.draggable);
            this.$.draggable.style.transform = '';

            this._focusedColumnIx = ix;
        };

        fillA();

        const cR = this.$.columns.getBoundingClientRect();
        yMin = cR.top;
        yMax = cR.bottom;
        dMax = cR.bottom - cells[curr].bottom;
        dMin = cR.top - cells[curr].top;

        startDragging(this.$.draggable);
        this.$.draggable.style.top = `${cells[curr].top - yMin}px`;
        this.$['draggable-label'].label = this._columns[curr].label;
        this.$['draggable-checkbox'].checked = !this._columns[curr].hidden;
        this.$['draggable-checkbox'].label = this._columns[curr].label;

        // Track mouse or touch
        window.addEventListener(mouseMoveEv, mouseMoveColumnDef, true);
        window.addEventListener(mouseUpEv, mouseUpColumnDef, {once: true});
        // }

        ev.preventDefault();
    }

    _activateRow(rowIndex, key) {
        const column = this.$.columns.children[rowIndex];
        if (!column) {
            return;
        }

        // Only activate row item on Space key
        if (key !== ' ') {
            return;
        }

        // Activate row item with focus
        const toggleDragging = () => {
            if (!column.nonReorderable) {
                column.classList.toggle('dragging');
            }
        };
        const toggleShowColumn = () => {
            const checkbox = column.querySelector('ptcs-checkbox');
            if (checkbox) {
                checkbox.click();
            }
        };
        switch (this.options) {
            case 'both':
                if (this._focusedItemIx === 0) {
                    toggleDragging();
                } else {
                    toggleShowColumn();
                }
                break;
            case 'show':
                toggleShowColumn();
                break;
            case 'reorder':
                toggleDragging();
        }
    }

    _keyDown(ev) {
        let ci = this._focusedColumnIx;
        const cl = this._columns.length - 1;
        const columns = this.$.columns.children;

        switch (ev.key) {
            case 'ArrowLeft':
                if (this.options !== 'both') {
                    return;
                }
                this._focusedItemIx = 0;
                break;
            case 'ArrowRight':
                if (this.options !== 'both') {
                    return;
                }
                stopDragging(columns[ci]);
                this._focusedItemIx = 1;
                break;
            case 'ArrowUp':
                if (isDragging(columns[ci]) && ci > 0) {
                    stopDragging(columns[ci]);
                    if (!columns[ci - 1].nonReorderable) {
                        this._moveColumn(ci, ci - 1);
                        startDragging(columns[ci - 1]);
                    }
                }
                if (this.options === 'both' && ci > 0 && columns[ci - 1].nonReorderable) {
                    this._focusedItemIx = 1;
                }
                ci = Math.max(ci - 1, 0);
                break;
            case 'ArrowDown':
                if (isDragging(columns[ci]) && ci < cl) {
                    this._moveColumn(ci, ci + 1);
                    stopDragging(columns[ci]);
                    startDragging(columns[ci + 1]);
                }
                ci = Math.min(ci + 1, cl);
                break;
            case 'Tab':
                // In "edge" cases closes the dialog and return to the "Display" button
                if ((!ev.shiftKey && this.shadowRoot.activeElement === this.$.cancel) ||
                    (ev.shiftKey && this.shadowRoot.activeElement === this.$.columns)) {
                    ev.preventDefault();
                    delegateToPrev(this);
                    this.mode = 'closed';
                }
                return;
            case 'Enter':
                // New behavior---an 'Enter' press in the dialog should simulate a click on the Apply
                // button, you should no longer have to TAB to it...
                this._apply();
                break;
            case ' ':
                if (this.shadowRoot.activeElement !== this.$.columns) {
                    return;
                }
                this._activateRow(ci, ev.key);
                break;
            case 'Escape':
                this._cancel();
                break;
            default:
                // Not handled
                return;
        }

        // Keyboard event has been consumed
        ev.preventDefault();

        if (this._focusedColumnIx === ci) {
            return; // Focus has not changed
        }

        this._focusedColumnIx = ci;

        const cR = this.$.columns.getBoundingClientRect();
        const colR = columns[ci].getBoundingClientRect();

        if (colR.top < cR.top) {
            // We're outside the columns boundaries
            // this.$.columns.scrollTop = (colR.top - col0.top);
            this.$.columns.scrollTop -= this._colH;
        } else if (colR.bottom > cR.bottom) {
            this.$.columns.scrollTop += this._colH;
        }
    }
};

customElements.define(PTCS.ColumnsDisplay.is, PTCS.ColumnsDisplay);
