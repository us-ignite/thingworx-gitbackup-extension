import {LitElement, html, css} from 'lit';
import {when} from 'lit/directives/when.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-list/ptcs-list.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-icons/cds-icons.js';

PTCS.ListShuttle = class extends PTCS.BehaviorTabindex(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement)))) {
    static get styles() {
        return css`
        :host {
            position: relative;
            width: 100%;
            height: 100%;
            box-sizing: border-box;

            display: inline-flex;
            justify-content: space-between;
            align-items: stretch;
        }

        #root {
            flex: 1 1 auto;

            display: grid;
            display: -ms-grid;
        }

        :host(:not([vertical])) #root {
            grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
            grid-template-rows: auto minmax(0, 1fr) 50px auto;
        }

        :host([vertical]) #root {
            grid-template-columns: minmax(0, 1fr);
            grid-template-rows: auto minmax(0, 1fr) auto minmax(0, 1fr) auto;
        }

        :host(:not([vertical])) [part~=head] {
            grid-column: 1 / 3;
            grid-row: 1;
        }

        :host([vertical]) [part~=head] {
            grid-column: 1;
            grid-row: 1;
        }

        :host(:not([vertical])) [part~=source-list-parent] {
            grid-column: 1;
            grid-row: 2 / 4;
        }

        :host([vertical]) [part~=source-list-parent] {
            grid-column: 1;
            grid-row: 2;
        }

        :host(:not([vertical])) [part~=target-list-parent] {
            grid-column: 2;
            grid-row: 2;
        }

        :host([vertical]) [part~=target-list-parent] {
            grid-column: 1;
            grid-row: 4;
        }

        :host(:not([vertical])) [part~=source-buttons] {
            grid-column: 1;
            grid-row: 4;
        }

        :host([vertical]) [part~=source-buttons] {
            grid-column: 1;
            grid-row: 3;
        }

        :host(:not([vertical])) [part~=target-buttons] {
            grid-column: 2;
            grid-row: 3 / 5;
        }

        :host([vertical]) [part~=target-buttons] {
            grid-column: 1;
            grid-row: 5;
        }

        [part=label] {
            width: 100%;
        }

        [part~=source-list-parent],
        [part~=target-list-parent] {
            position: relative;
        }

        [part~=source-list],
        [part~=target-list] {
            width: 100%;
            height: 100%;
        }

        [part~=target-buttons] {
            display: flex;
            flex-wrap: wrap;
            flex-direction: row;
            justify-content: flex-start;
            align-items: flex-start;
        }

        [part~=move-buttons] {
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: flex-start;
        }`;
    }

    render() {
        return html`
        <div id="root">
            ${when(!this._isEmpty(this.label), () => html`
                <div part="head">
                    <ptcs-label part="label" .label=${this.label} multi-line
                        .horizontalAlignment=${this.labelAlignment} .variant=${this.labelType}></ptcs-label>
                </div>`)}
            <div part="source-list-parent">
                <ptcs-list id="srclist" part="source-list" .label=${this.sourceLabel}
                    .labelAlignment=${this.sourceLabelAlignment} .labelType=${this.sourceLabelType}
                    .items=${this.items} .selector=${this.selector} .disabled=${this.disabled}
                    .clearSelectedItemsLabel=${this.clearSelectedItemsLabel} .selectAllLabel=${this.selectAllLabel}
                    .filter=${this._filter} .filterString=${this.sourceFilter} .hideFilter=${this.hideFilter}
                    .hintText=${this.filterHintText} .multiSelect=${!this.singleSelect}
                    @selected-indexes-changed=${this._srcSelectionChanged}
                    exportparts=${this._exportsource} tabindex=${this._delegatedFocus}></ptcs-list>
            </div>
            <div part="source-buttons buttons">
                <ptcs-button part="add-button button" variant="primary"
                    .label=${this.addLabel} tabindex=${this._delegatedFocus}
                    .disabled=${this._btnDisabled(this.disabled, this._selectionSrc)} @action=${this._addClick}></ptcs-button>
            </div>
            <div part="target-list-parent">
                <ptcs-list id="dstlist" part="target-list" .label=${this.targetLabel}
                    .labelAlignment=${this.targetLabelAlignment} .labelType=${this.targetLabelType}
                    .items=${this._selectedItems} .selector=${this.selector} .disabled=${this.disabled}
                    .clearSelectedItemsLabel=${this.clearSelectedItemsLabel} .selectAllLabel=${this.selectAllLabel}
                    .multiSelect=${!this.singleSelect} @selected-indexes-changed=${this._dstSelectionChanged}
                    .externalStayUnvalidated=${this._enableTargetListValidation}
                    .validationMessageIsListItem=${this._enableTargetListValidation}
                    .validationSuccessMessage=${this.validationSuccessMessage}
                    .validationSuccessDetails=${this.validationSuccessDetails}
                    .validationSuccessIcon=${this.validationSuccessIcon}
                    .validationCriteriaIcon=${this.validationCriteriaIcon}
                    .validationErrorIcon=${this.validationErrorIcon}
                    .hideValidationError=${this.hideValidationError}
                    .hideValidationSuccess=${this.hideValidationSuccess}
                    .hideValidationCriteria=${this.hideValidationCriteria}
                    .validationMessage=${this.validationMessage}
                    .validationCriteria=${this.validationCriteria}
                    @validation-output-changed=${this._dstListValidationOutputChanged}
                    @validation-completed=${this._dstListValidationCompleted}
                    exportparts=${this._exporttarget} tabindex=${this._delegatedFocus}></ptcs-list>
            </div>
            <div part="target-buttons buttons">
                <ptcs-button part="remove-button button" variant="primary" id="rembtn"
                    .label=${this.removeLabel} tabindex=${this._delegatedFocus}
                    .disabled=${this._btnDisabled(this.disabled, this._selectionDst)} @action=${this._removeClick}></ptcs-button>
                <div part="move-buttons">
                    <ptcs-button part="up-button button" variant="tertiary" id="upbtn"
                        .label=${this.labelUp} icon="cds:icon_ascending" tabindex=${this._delegatedFocus}
                        .disabled=${this._btnDisabled(this.disabled, this._canMoveUp)} @action=${this._upClick}></ptcs-button>
                    <ptcs-button part="down-button button" variant="tertiary" id="dnbtn"
                        .label=${this.labelDown} icon="cds:icon_descending" tabindex=${this._delegatedFocus}
                        .disabled=${this._btnDisabled(this.disabled, this._canMoveDn)} @action=${this._downClick}></ptcs-button>
                </div>
            </div>
        </div>`;
    }

    static get is() {
        return 'ptcs-list-shuttle';
    }

    static get properties() {
        return {
            disabled: {
                type:    Boolean,
                reflect: true
            },

            // Select value from item[]
            selector: {
                type: String
            },

            idSelector: {
                type:      String,
                attribute: 'id-selector'
            },

            // List input
            items: {
                type: Array
            },

            // Enable changes-only mode? (= don't fire change events unless the property has changed. The legacy approach is to
            // fire change events if an assigned property value differs from the resulting value, even if it doesn't change it.)
            changesOnly: {
                type:      Boolean,
                attribute: 'changes-only'
            },

            // Selected items in list input
            selectedItems: {
                type:       Array,
                attribute:  'selected-items',
                notify:     true,
                noAccessor: true
            },

            // Extracted items from selectedItems that only contain existing items
            _selectedItems: {
                type:        Array,
                observer:    '_selectedItemsChanged',
                observeWhen: 'immediate'
            },

            _defaultselectedItems: {
                type: Array
            },

            // Map: label => item[index]
            _label2item: {
                type: Object
            },

            singleSelect: {
                type:      Boolean,
                attribute: 'single-select'
            },

            _selectionSrc: {
                type: Boolean
            },

            _selectionDst: {
                type: Boolean
            },

            _canMoveUp: {
                type: Boolean,
            },

            _canMoveDn: {
                type: Boolean,
            },

            hideFilter: {
                type:      Boolean,
                attribute: 'hide-filter'
            },

            filterHintText: {
                type: String
            },

            _filterSet: {
                type: Set
            },

            _filter: {
                type:     Function,
                computed: '_computeFilter(hideFilter, _filterSet)'
            },

            sourceFilter: {
                type:      String,
                attribute: 'source-filter',
                notify:    true
            },

            _resizeObserver: {
                type: ResizeObserver
            },

            vertical: {
                type:    Boolean,
                reflect: true
            },

            // Labels
            label: {
                type: String
            },

            labelType: {
                type:      String,
                attribute: 'label-type'
            },

            labelAlignment: {
                type:      String,
                attribute: 'label-alignment'
            },

            sourceLabel: {
                type:      String,
                attribute: 'source-label'
            },

            sourceLabelType: {
                type:      String,
                attribute: 'source-label-type'
            },

            sourceLabelAlignment: {
                type:      String,
                attribute: 'source-label-alignment'
            },

            targetLabel: {
                type:      String,
                attribute: 'target-label'
            },

            targetLabelType: {
                type:      String,
                attribute: 'target-label-type'
            },

            targetLabelAlignment: {
                type:      String,
                attribute: 'target-label-alignment'
            },

            // Target List validation properties

            // Target list cannot be empty?
            required: {
                type:    Boolean,
                isValue: required => !!required
            },

            requiredMessage: {
                type:      String,
                attribute: 'required-message'
            },

            // Max number of items that can be added to target list
            targetListMaxItems: {
                type:      Number,
                attribute: 'target-list-max-items'
            },

            targetListMaxItemsFailureMessage: {
                type:      String,
                attribute: 'target-list-max-items-failure-message'
            },

            // Min number of items that must be added to target list
            targetListMinItems: {
                type:      Number,
                attribute: 'target-list-min-items'
            },

            targetListMinItemsFailureMessage: {
                type:      String,
                attribute: 'target-list-min-items-failure-message'
            },

            // Used to set ptcs-list flags to prevent its default _stayUnvalidated behavior and to insert the validation message as a list item
            _enableTargetListValidation: {
                type: Boolean
            },

            //
            // Validation properties in validation behavior
            //

            // undefined | 'unvalidated' | 'invalid' | 'valid' (reflects the target list's validation state)
            validationOutput: {
                type:      String,
                attribute: 'validation-output',
                notify:    true
            },

            // External (server side) validation
            externalValidity: {
                type:      String,
                attribute: 'external-validity'
            },

            // Icon for success state (valid)
            validationSuccessIcon: {
                type:      String,
                attribute: 'validation-success-icon'
            },

            // Icon for error state (invalid)
            validationErrorIcon: {
                type:      String,
                attribute: 'validation-error-icon'
            },

            // Icon for criteria state (unvalidated)
            validationCriteriaIcon: {
                type:      String,
                attribute: 'validation-criteria-icon'
            },

            // The validation (title) message
            validationMessage: {
                type:      String,
                attribute: 'validation-message'
            },

            // The validation success (title) message.
            validationSuccessMessage: {
                type:      String,
                attribute: 'validation-success-message'
            },

            // The validation details message
            validationCriteria: {
                type:      String,
                attribute: 'validation-criteria'
            },

            // The validation success details message
            validationSuccessDetails: {
                type:      String,
                attribute: 'validation-success-details'
            },

            // Don't show validation success state?
            hideValidationSuccess: {
                type:      Boolean,
                attribute: 'hide-validation-success'
            },

            // Don't show validation error state?
            hideValidationError: {
                type:      Boolean,
                attribute: 'hide-validation-error'
            },

            // Don't show validation criteria in unvalidated state?
            hideValidationCriteria: {
                type:      Boolean,
                attribute: 'hide-validation-criteria'
            },

            _itemsLength: {
                type:     Number,
                observer: '_itemsLengthChanged'
            },

            addLabel: {
                type:      String,
                attribute: 'add-label'
            },

            removeLabel: {
                type:      String,
                attribute: 'remove-label',
                observer:  '_alignButtons'
            },

            labelUp: {
                type:      String,
                attribute: 'label-up',
                observer:  '_alignButtons'
            },

            labelDown: {
                type:      String,
                attribute: 'label-down',
                observer:  '_alignButtons'
            },

            collapsedBtns: {
                type:      Boolean,
                attribute: 'collapsed-btns',
                reflect:   true
            },

            selectAllLabel: {
                type:      String,
                attribute: 'select-all-label'
            },

            clearSelectedItemsLabel: {
                type:      String,
                attribute: 'clear-selected-items-label'
            },

            _delegatedFocus: {
                type: String
            },

            _exportsource: {
                type:     String,
                readOnly: true,
                value:    PTCS.exportparts('source-list-', PTCS.List)
            },

            _exporttarget: {
                type:     String,
                readOnly: true,
                value:    PTCS.exportparts('target-list-', PTCS.List)
            }
        };
    }

    static get observers() {
        return [
            '_itemsChanged(items, selector, idSelector)',
            '_observeValidation(required, targetListMinItems, targetListMaxItems, externalValidity, _itemsLength)'
        ];
    }

    constructor() {
        super();

        this.filterHintText = 'Filter';
        this._filterSet = new Set();

        this.items = [];
        this.selectedItems = [];
        this._selectedItems = [];

        this._defaultselectedItems = [];

        this._label2item = {};
        this.singleSelect = false;
        this._canMoveUp = false;
        this._canMoveDn = false;

        this.sourceFilter = '';

        // Labels
        this.label = '';
        this.labelType = 'sub-header';
        this.labelAlignment = 'left';
        this.sourceLabel = 'Source';
        this.sourceLabelType = 'label';
        this.sourceLabelAlignment = 'left';
        this.targetLabel = 'Target';
        this.targetLabelType = 'label';
        this.targetLabelAlignment = 'left';

        this.addLabel = 'Add';
        this.removeLabel = 'Remove';
        this.labelUp = 'Up';
        this.labelDown = 'Down';

        this.selectAllLabel = 'Select All';
        this.clearSelectedItemsLabel = 'Clear Selected Items';

        this._delegatedFocus = null;

        // Create the resizeObserver in the constructor (and not in ready())
        this._resizeObserver = new ResizeObserver(entries => {
            const bw = Number(getComputedStyle(this).getPropertyValue('--ptcs-list-shuttle--break-width') || 530);
            const w = entries[0].contentRect.width;
            if (w > 0) {
                this.vertical = (w + 2) < bw;
            }
            this._alignButtons();
        });
    }

    ready() {
        super.ready();

        // Show the target list validation message when the list shuttle loses focus
        this.addEventListener('blur', () => {
            const dstlist = this.$.dstlist;
            const isValid = typeof dstlist.extraValidation === 'function' && dstlist.extraValidation() === true;
            if (this.externalValidity === 'unvalidated' && isValid) {
                return;
            }
            dstlist.enableValidationMessage(true);
        });
    }

    connectedCallback() {
        super.connectedCallback();
        this._resizeObserver.observe(this);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this._resizeObserver.unobserve(this);
    }

    _btnDisabled(disabled, canDoAction) {
        return disabled || !canDoAction;
    }

    _dstListValidationCompleted() {
        if (this._lastValidated !== this._needValidate) {
            this._lastValidated = this._needValidate;
            this.dispatchEvent(new CustomEvent('validation-completed'));
        }
    }

    _isEmpty(label) {
        if (!label || label === '') {
            return true;
        }

        return false;
    }

    _computeFilter(hideFilter, _filterSet) {
        if (hideFilter) {
            return () => {
                return (item, index) => !_filterSet.has(this.items[index]);
            };
        }
        // eslint-disable-next-line no-unused-vars
        return (filterString, filterFunc) => {
            return (item, index) => !_filterSet.has(this.items[index]) && filterFunc(item, index);
        };
    }

    // The list-shuttle want to change selectedItems
    // Note: every change of selectedItems must go via a new array, otherwise the list-shuttle generates
    // change events for every micro-change (which clients considered to be a bug)
    async _assignSelectedItems(selectedItems) {
        // Save current selection in dstlist
        const sel = this.$.dstlist.selectedIndexes.map(i => this._selectedItems[i]);

        // Update _selectedItems (show selectedItems in dstlist)
        this._selectedItems = selectedItems;

        // This is needed, otherwise we lose the current selection after e.g. an 'Up' click...
        this.performUpdate();
        await this.$.dstlist.updateComplete;

        // Restore saved selection
        const si = [];
        sel.forEach(item => {
            // Note: O(n^2) algorithm. (Probably not a problem for this function though, since n will - probably - be small)
            const ix = selectedItems.findIndex(item2 => item2 === item);
            if (ix >= 0) {
                si.push(ix);
            }
        });
        this.$.dstlist.selectedIndexes = si;

        // Notify client
        this.selectedItems = selectedItems;
    }

    get selectedItems() {
        return this._$selectedItems; // The value that the client assigned
    }

    set selectedItems(selectedItems) {
        const selectedSelector = this.idSelector || this.selector;
        const getId = PTCS.makeSelector(selectedSelector);
        const cmp = (a, b) => getId(a) === getId(b);

        const old = this._$selectedItems;
        this._$selectedItems = selectedItems; // Always keep the last assigned items

        if (PTCS.sameArray(old, selectedItems, cmp)) {
            if (old !== selectedItems && !this.changesOnly) {
                // Report no-change, for backwards compatibility
                this.dispatchEvent(new CustomEvent('selected-items-changed', {detail: {value: selectedItems, noChange: true}}));
                this.$.dstlist.selectedIndexes = []; // The selection of the target list should be reset whenever selectedItems is assigned
            }
            return; // Do nothing if the new selection identifies the same selection as the old
        }

        this.requestUpdate('selectedItems', old); // There hase been a change

        if (PTCS.sameArray(this._selectedItems, selectedItems, cmp)) {
            return; // This happens when the list-shuttle itself changes the selection
        }

        this._itemsChanged_sub(this.items, selectedItems, this.selector, this.idSelector);
    }

    _itemsChanged(items, selector, idSelector) {
        this._itemsChanged_sub(items, this._$selectedItems, selector, idSelector);
    }

    _itemsChanged_sub(items, selectedItems, selector, idSelector) {
        // Reset internal copy of selectedItems
        this._selectedItems = [];

        if (!(items instanceof Array)) {
            return; // No data
        }

        if (!(selectedItems instanceof Array)) {
            return; // Nothing selected
        }

        // This selectedItems has been given by the client, so use it as the default value
        if (this._defaultselectedItems !== selectedItems) {
            this._defaultselectedItems = selectedItems.slice(0);
        }

        if (selectedItems.length === 0 && this._filterSet.size === 0) {
            return; // Nothing selected
        }

        const selectedSelector = idSelector || selector;
        const getId = PTCS.makeSelector(selectedSelector);

        // Recompute labels?
        if (this._oldItems !== items || this._oldSelector !== selectedSelector) {
            this._oldItems = items;
            this._oldSelector = selectedSelector;
            this._label2item = {};
            items.forEach(item => {
                this._label2item[getId(item)] = item;
            });
        }

        // Map objects in selectedItems to objects in items
        this._filterSet = new Set();

        // Grab the actual selected items from the items array
        selectedItems.forEach(item => {
            const itemSrc = this._label2item[getId(item)];
            if (itemSrc) {
                this._filterSet.add(itemSrc);
            } else {
                console.warn('Unknown item in selectedItems: ' + getId(item));
            }
        });

        // The selectedItems that the shuttle will actually use
        this._selectedItems = [...this._filterSet];

        // Keep the validation 'unvalidated' from the beginning
        this.__hasInteracted = false;
        this.$.dstlist.enableValidationMessage(false);

        this.$.srclist.reFilter();
    }

    async _addClick() {
        // The user has now interacted with the List shuttle
        this.__hasInteracted = true;

        this.$.srclist.selectedIndexes.sort((x, y) => x - y).forEach(ix => {
            this._filterSet.add(this.items[ix]);
        });

        this._assignSelectedItems([...new Set([...(this.$.dstlist.items || []), ...(this.items || [])])].filter(_item => this._filterSet.has(_item)));
        this.$.srclist.unselectAll();
        this.$.srclist.reFilter();

        if (this.singleSelect) {
            // Needed to ensure that the single item in single selection appears selected
            await this.$.dstlist.updateComplete;
            this.$.dstlist.selectItem(this._selectedItems.length - 1, true);
        }
    }

    _removeClick() {
        // The user has now interacted with the List shuttle
        this.__hasInteracted = true;

        const si = this._dstSelectionSeg();
        const item = si.length && this._selectedItems[si[0][0]]; // First item that is selected
        for (let i = si.length - 1; i >= 0; i--) {
            const [from, to] = si[i];
            for (let j = from; j <= to; j++) {
                this._filterSet.delete(this._selectedItems[j]);
            }
        }

        this._assignSelectedItems((this.$.dstlist.items || []).filter(_item => this._filterSet.has(_item)));
        this.$.dstlist.unselectAll();
        this.$.srclist.reFilter();

        if (this.singleSelect && item) {
            this.$.srclist.selectItem(this.$.srclist.items.findIndex(x => x === item), true);
        }
    }

    _dstSelectionSeg() {
        const r = [];
        const a = this.$.dstlist.selectedIndexes.sort((x, y) => x - y);
        for (let i = 0; i < a.length;) {
            let i2 = i + 1;
            while (i2 < a.length && a[i2 - 1] + 1 === a[i2]) {
                i2++;
            }
            r.push([a[i], a[i2 - 1]]);
            i = i2;
            if (i > 100) {
                break;
            }
        }
        return r;
    }

    _upClick() {
        const a = [...this._selectedItems];
        this._dstSelectionSeg().forEach(seg => {
            if (seg[0] > 0) {
                a.splice(seg[0] - 1, 0, ...a.splice(seg[0], seg[1] - seg[0] + 1));
            }
        });
        this._assignSelectedItems(a);
    }

    _downClick() {
        const a = [...this._selectedItems];
        this._dstSelectionSeg().forEach(seg => {
            if (seg[1] < a.length - 1) {
                a.splice(seg[0] + 1, 0, ...a.splice(seg[0], seg[1] - seg[0] + 1));
            }
        });
        this._assignSelectedItems(a);
    }

    _srcSelectionChanged() {
        this._selectionSrc = this.$.srclist.selectedIndexes.length > 0;
    }

    _dstSelectionChanged() {
        const segList = this._dstSelectionSeg();
        this.setProperties({
            _selectionDst: this.$.dstlist.selectedIndexes.length > 0,
            _canMoveUp:    segList.some(seg => seg[0] > 0),
            _canMoveDn:    segList.some(seg => seg[1] < this._selectedItems.length - 1)
        });
    }

    _dstListValidationOutputChanged(ev) {
        this.validationOutput = ev.detail.value;
    }

    _selectedItemsChanged(_selectedItems) {
        this._itemsLength = Array.isArray(_selectedItems) ? this._selectedItems.length : 0;
    }

    _itemsLengthChanged(_itemsLength) {
        this.dispatchEvent(new CustomEvent('validate', {detail: {length: _itemsLength}}));
        this._needValidate = (this._needValidate || 0) + 1;
    }

    _resetToDefault() {
        this.selectedItems = this._defaultselectedItems.slice(0);
    }

    // Align the the target list buttons
    _alignButtons() {
        // Something may have affected the styling of the three buttons under the target list
        const e1 = this.$.rembtn;
        const e2 = this.$.upbtn;
        const e3 = this.$.dnbtn;
        if (!e1 || !e2 || !e3) {
            return;
        }

        // Reset styles
        e1.style.width = '';
        e2.style.width = '';
        e3.style.width = '';

        // Get new dimensions
        const b1 = e1.getBoundingClientRect();
        const b2 = e2.getBoundingClientRect();
        const collapsedBtns = b1.bottom < b2.top;
        const w1 = collapsedBtns ? b1.width : 0;
        const w2 = b2.width;
        const w3 = e3.getBoundingClientRect().width;
        const w = Math.max(w1, w2, w3);
        const ws = `${w}px`;

        if (collapsedBtns && w > w1) {
            e1.style.width = ws;
        }
        if (w > w2) {
            e2.style.width = ws;
        }
        if (w > w3) {
            e3.style.width = ws;
        }

        // Delay updating the state attribute until now
        this.collapsedBtns = collapsedBtns;
    }

    _observeValidation(required, targetListMinItems, targetListMaxItems, externalValidity, _itemsLength) {
        this._enableTargetListValidation = this.required || this.targetListMinItems || this.targetListMaxItems ||
          (this.externalValidity && this.externalValidity !== 'undefined');
        const dstlist = this.$.dstlist;
        if (!this._enableTargetListValidation) {
            // No internal or external validation for the target list
            dstlist.extraValidation = undefined;
        } else {
            // Target list has validation requirements. Validate using its extraValidation function, assigned below.
            dstlist.extraValidation = () => {
                const messages = [];

                // required
                if (this.required && this._itemsLength === 0) {
                    messages.push(this.requiredMessage);
                }

                // targetListMinItems
                if (this.targetListMinItems && this._itemsLength < this.targetListMinItems) {
                    const msg = PTCS.replaceStringTokens(this.targetListMinItemsFailureMessage, {value: this.targetListMinItems});
                    messages.push(msg ? msg.join('. ') : false);
                }

                // targetListMaxItems
                if (this.targetListMaxItems && (this._itemsLength > this.targetListMaxItems)) {
                    const msg = PTCS.replaceStringTokens(this.targetListMaxItemsFailureMessage, {value: this.targetListMaxItems});
                    messages.push(msg ? msg.join('. ') : false);
                }

                // At least one validation failed
                if (messages.length) {
                    return messages;
                }

                // All "internal" validation has succeeded. Check the external validation as well...
                if (this.externalValidity && this.externalValidity !== 'undefined') {
                    switch (this.externalValidity) {
                        case 'valid': return true;
                        case 'invalid': return false;
                        case 'unvalidated':
                            dstlist.enableValidationMessage(false);
                            break;
                    }
                }

                return true;
            };

            // The target list should remain in unvalidated state until it becomes valid or invalid (or loses focus)
            requestAnimationFrame(() => {
                if (!this.__hasInteracted) {
                    // If called before any physical interaction has taken place, don't turn on any messages
                    return;
                }
                if (this.targetListMinItems && this._itemsLength >= this.targetListMinItems) {
                    dstlist.enableValidationMessage(true);
                } else if (this.required && this._itemsLength > 0 && !this.targetListMinItems) {
                    dstlist.enableValidationMessage(true);
                }
                if (this.targetListMaxItems && this._itemsLength >= this.targetListMaxItems) {
                    dstlist.enableValidationMessage(true);
                }
                if (this.externalValidity && this.externalValidity === 'unvalidated') {
                    dstlist.enableValidationMessage(false);
                }
            });
        }
    }
};

customElements.define(PTCS.ListShuttle.is, PTCS.ListShuttle);
