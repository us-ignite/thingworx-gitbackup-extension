import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import {ListSelection} from './list-selection.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';
import {setTooltipByFocus} from 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-behavior-validate/ptcs-behavior-validate.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-textfield/ptcs-textfield.js';
import 'ptcs-checkbox/ptcs-checkbox.js';
import 'ptcs-radio/ptcs-radio.js';
import 'ptcs-link/ptcs-link.js';
import 'ptcs-v-scroller/ptcs-v-scroller2.js';
import './ptcs-list-item.js';
import './ptcs-list-group.js';
import 'ptcs-icons/cds-icons.js';

const $groupKey = Symbol('$groupKey');

PTCS.List = class extends PTCS.BehaviorTabindex(PTCS.BehaviorValidate(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))))) {
    static get styles() {
        return css`
            :host {
                display: flex;
                flex-direction: column;
                justify-content: space-between;
                align-items: stretch;
                min-width: 34px;
                min-height: 34px;
                box-sizing: border-box;
                overflow: hidden;

                /* If the container doesn't limit the height, then negotiate height with scroller */
                height: min(var(--ptcs-list-height, var(--ptcs-list-max-height, 571px)), var(--ptcs-list-max-height, 571px));
            }

            [part=label][hidden] {
                display: none;
            }

            [part=label] {
                display: block;
                padding-bottom: 4px;
                flex-shrink: 0;
                min-height: unset;
                min-width: unset;
            }

            [part=item-checkbox] {
                grid-column: 1;
                grid-row: 1;

                align-self: center;
                font-size: inherit;
                min-height: unset;
            }

            [part=item-radio] {
                grid-column: 1;
                grid-row: 1;

                align-self: center;
                font-size: inherit;
                min-height: unset;
            }

            [part=list-item][hidden] {
                display: none;
            }

            [part=list-group] {
                display: flex;
                width: 100%;
                box-sizing: border-box;
            }

            [part=group-label] {
                flex-grow: 1;
            }

            :host(:not([multi-select])) [part=item-checkbox] {
                display: none;
            }

            :host(:not([radio-button-selection])) [part=item-radio] {
                display: none;
            }

            :host(:not([multi-select])) [part=multi-select] {
                display: none;
            }

            :host([_hide-list]) [part=list-container] {
                display: none;
            }

            [part=multi-select][hidden] {
                display: none;
            }

            [part=multi-select] {
                display: flex;
                justify-content: space-between;
                align-items: center;

                flex: 0 0 auto;
            }

            [part=link] {
                flex: 1 1 auto;
            }

            [part=no-matches][hidden] {
                display: none;
            }

            [part=no-matches] {
                display: flex;
                justify-content: space-between;
                align-items: center;

                flex: 0 0 auto;
            }

            [part=no-matches-label] {
                flex: 1 1 auto;
            }

            [part=item-meta] {
                grid-column: 2;
                grid-row: 2;

                justify-content: stretch;
                align-content: center;
            }

            [part=item-meta][hidden] {
                display: none;
            }

            :host(:not([disabled])) [part=list-item]:not([disabled]):hover {
                cursor: pointer;
            }

            [part=filter] {
                display: flex;
                flex-direction: row;
                justify-content: space-between;
                align-items: center;
                flex-wrap: nowrap;

                flex: 0 0 auto;
            }

            [part=filter][hidden] {
                display: none !important;
            }

            [part=filter-field] {
                flex: 1 1 auto;
            }

            :host(:not([disabled])) [part=icon-close] {
                cursor: pointer;
            }

            [part=list-container] {
                flex: 1 1 auto;
                box-sizing: border-box;
                overflow: hidden;

                display: flex;
                flex-direction: column;
                flex-wrap: nowrap;
                justify-content: space-between;
                align-items: stretch;
            }

            [part=list-items-container] {
                flex: 1 1 auto;
                box-sizing: border-box;
            }

            /* Do not change the following selector as it could have side-effects e.g. on label of item for resetting single selection list */
            ptcs-div[part~=item-value] {
                grid-column: 2;
                grid-row: 1;

                display: flex;

                justify-content: flex-start;
                align-items: center;

                overflow: hidden;
            }

            [part~=item-value] {
                max-width: 100%;
            }

            [part=list-item] {
                min-height: var(--ptcs-list-item--height, 34px);
                width: 100%;
                box-sizing: border-box;
            }

            ptcs-list-item {
                display: grid;

                grid-template-columns: auto 1fr;
                grid-template-rows: 1fr auto;
            }

            /* Hide meta row? */
            ptcs-list-item[label-meta=''] {
                grid-template-rows: 1fr;
            }

            .unselect-item {
                display: flex;
                flex-direction: column;
                justify-content: center;
                align-items: stretch;
                align-content: stretch;
            }

            .unselect-item > [part~=item-value] {
                flex: 1 1 auto;
                position: relative;
            }

            /* The border settings from the Theme Engine should only affect the list item separator */
            /* NOTE: This should have been handled by the Theme Engine, but we don't want this to show up un the Style Tab */
            [part=list-item][first] {
                border-top: none !important;
            }
            [part=list-item] {
                border-left: none !important;
                border-right: none !important;
                border-bottom: none !important;
            }
        `;
    }

    render() {
        return html`
            <ptcs-label part="label" variant=${this.labelType} .label=${this.label} ?hidden=${!this.label}
                .horizontalAlignment=${this.labelAlignment}></ptcs-label>

            <div style="display: flex; height: 0px; width: var(--ptcs-list--auto-width, 0px)"></div>
            <div id="list-container" part="list-container">
                <!-- filter list -->
                <div part="filter" ?hidden=${this._filterHidden()} stretch>
                    <ptcs-textfield part="filter-field" icon="cds:icon_filter" .hintText=${this.hintText} .text=${this.filterString}
                        @text-changed=${this._textChangedEv} .disabled=${this.disabled} tabindex=${this._delegatedFocus}
                        .tooltip=${this.ownerTooltip} .tooltipIcon=${this.ownerTooltipIcon} exportparts=${this._exportFilter}>
                    </ptcs-textfield>
                </div>

                <!-- select all / clear selections -->
                <div part="multi-select" ?hidden=${!this._chunkerLength2}>
                    <ptcs-link part="link" variant="secondary" .label=${this._multiSelectLabel()}
                        .disabled=${this.disabled} @click=${this._clickMultiSelect} tabindex=${this._delegatedFocus}
                        exportparts=${this._exportLink}>
                    </ptcs-link>
                </div>

                <!-- Label displayed when the filter hides "everything" -->
                <div part="no-matches" ?hidden=${this._hideNoMatches()}>
                    <ptcs-label part="no-matches-label" variant="label" .label=${this.noMatchesLabel} .disabled=${this.disabled}></ptcs-label>
                </div>

                <!-- the list items -->
                <ptcs-v-scroller2 part="list-items-container" id="chunker" .numItems=${this._chunkerLength2} @dblclick=${this._dblClick}
                    @gap-changed=${this._gapChangedEv} tabindex=${this._delegatedFocus} .wrapFocus=${this.wrapFocus}
                    @repainted=${this._hasNewView}></ptcs-v-scroller2>
            </div>`;
    }

    static get is() {
        return 'ptcs-list';
    }

    static get properties() {
        return {
            label: {
                type:  String,
                value: ''
            },

            // {type: 'text' | 'image' | 'checkbox'| 'html' | 'function' }; }
            // {type: 'link', target: link @target attribute}
            // Default: {type: 'text'}
            itemMeta: {
                type:      Object,
                attribute: 'item-meta',
                observer:  '_itemMetaChanged'
            },

            labelAlignment: { // 'left', 'center', 'right'
                type:      String,
                attribute: 'label-alignment',
                value:     'left',
                reflect:   true
            },

            labelType: { // 'header', 'sub-header', 'label', 'body'
                type:      String,
                attribute: 'label-type',
                value:     'label'
            },

            alignment: { // 'left', 'center', 'right'
                type:     String,
                value:    'left',
                reflect:  true,
                observer: '_alignmentChanged'
            },

            // Items supplied by the client. Read-only
            items: {
                type:        Array,
                value:       () => [],
                observer:    '_itemsChanged',
                observeWhen: 'immediate'
            },

            // Enable changes-only mode? (= don't fire change events unless the property has changed. The legacy approach is to
            // fire change events if an assigned property value differs from the resulting value, even if it doesn't change it.)
            changesOnly: {
                type:       Boolean,
                attribute:  'changes-only',
                noAccessor: true // Don't requestUpdate when this property changes. No effect on render()
            },

            selectedItems: {
                type:       Array,
                attribute:  'selected-items',
                notify:     true,
                noAccessor: true
            },

            // Number of items that are visible in the list (read-only)
            _visibleItems: {
                type:   Number,
                notify: true
            },

            // Array of indexes to filtered and grouped items: _itemsIndexFiltered[x] = index into items || {$groupKey}
            _itemsIndexFiltered: {
                type:      Array,
                attribute: false
            },

            // Number of items visible in chunker
            _chunkerLength: {
                type:     Number,
                value:    0,
                observer: '_chunkerLengthChanged'
            },

            // Slowly tracks _chunkerLength, to avoid unnessecary v-scroller refreshs
            _chunkerLength2: {
                type:     Number,
                observer: '_chunkerLength2Changed',
                value:    0
            },

            // Is there a gap at the bottom of the list?
            _gap: {
                type:     Number,
                observer: '_gapChanged'
            },

            // A Boolean (filter on/off) or a JS "array filter" function
            filter: {
                value: false
            },

            // Filter string entered in filter textfield
            filterString: {
                type:      String,
                attribute: 'filter-string',
                value:     '',
                notify:    true
            },

            // Current JS array filter function
            _filter: {
                type: Function
            },

            _hidden: {
                type:  Function,
                value: null
            },

            // Allows the filter textfield to be hidden even when there is an active filter
            hideFilter: {
                type:      Boolean,
                attribute: 'hide-filter',
            },

            _hideList: {
                type:      Boolean,
                reflect:   true,
                attribute: '_hide-list',
                computed:  '_computeHideList(noMatchesLabel, _visibleItems, hideEmptyList)'
            },

            hideEmptyList: {
                type:      Boolean,
                attribute: 'hide-empty-list',
            },

            // Selection
            multiSelect: {
                type:      Boolean,
                attribute: 'multi-select',
                reflect:   true
            },

            // Radio button selection for single selection
            radioButtonSelection: {
                type:      Boolean,
                attribute: 'radio-button-selection',
                reflect:   true,
                observer:  '_radioButtonSelectionChanged'
            },

            // Indexes of selected items
            selectedIndexes: {
                type:       Array,
                attribute:  'selected-indexes',
                notify:     true,
                noAccessor: true
            },

            // For validation
            _selectedIndexesLength: {
                type:     Number,
                validate: '_validateList(required, extraValidation)'
            },

            // Value of selected item, if single selection mode
            selectedValue: {
                type:       String,
                attribute:  'selected-value',
                reflect:    true,
                notify:     true,
                noAccessor: true
            },

            // Index of selected object, if single selection mode
            selected: {
                type:       Number,
                notify:     true,
                noAccessor: true
            },

            // _selected = +selected (=> make sure it is a number)
            _selected: {
                type: Number
            },

            // Select label from item
            selector: {
                observer: '_selectorChanged'
            },

            // Select value from item (defaults to selector)
            valueSelector: {
                attribute: 'value-selector'
            },

            // Select enabled / disabled mode from item
            stateSelector: {
                attribute: 'state-selector',
                value:     null,
                observer:  '_stateSelectorChanged'
            },

            // Select meta label from item
            metaSelector: {
                attribute: 'meta-selector',
                value:     null,
                observer:  '_metaSelectorChanged'
            },

            treatValueAsString: {
                type:      Boolean,
                attribute: 'treat-value-as-string'
            },

            returnOriginalValue: {
                type:      Boolean,
                attribute: 'return-original-value'
            },

            disabled: {
                type:     Boolean,
                reflect:  true,
                observer: '_disabledChanged'
            },

            autoSelectFirstRow: {
                type:       Boolean,
                attribute:  'auto-select-first-row',
                noAccessor: true
            },

            multiLine: {
                type:      Boolean,
                attribute: 'multi-line',
                value:     false,
                observer:  '_multiLineChanged'
            },

            rowHeight: {
                type:      String,
                attribute: 'row-height',
                value:     '34',
                observer:  '_rowHeightChanged'
            },

            allowNoItemSelection: {
                type:      Boolean,
                attribute: 'allow-no-item-selection',
                observer:  '_allowNoItemSelectionChanged'
            },

            hintText: {
                type:      String,
                attribute: 'hint-text',
                value:     'Filter'
            },

            noMatchesLabel: {
                type:      String,
                attribute: 'no-matches-label',
            },

            clearSelectionLabel: {
                type:      String,
                attribute: 'clear-selection-label',
                observer:  '_clearSelectionLabelChanged'
            },

            selectAllLabel: {
                type:      String,
                attribute: 'select-all-label',
                value:     'Select All'
            },

            clearSelectedItemsLabel: {
                type:      String,
                attribute: 'clear-selected-items-label',
                value:     'Clear Selected Items'
            },

            // Specifies field name that contains group key or function that returns group key
            grouping: {
                type: String // or Function: (item) => String
            },

            _delegatedFocus: {
                type:  String,
                value: null
            },

            createListItemAdditionalProperties: Function,

            // Tooltip data provided by list owner (like a dropdown) to be shown during list item truncation tooltip
            ownerTooltip: {
                type:      String,
                attribute: 'owner-tooltip',
                observer:  '_ownerTooltipChanged'
            },

            ownerTooltipIcon: {
                type:      String,
                attribute: 'owner-tooltip-icon',
                observer:  '_ownerTooltipIconChanged'

            },

            // Validation properties
            required: {
                type:    Boolean,
                isValue: required => !!required
            },

            requiredMessage: {
                type:      String,
                attribute: 'required-message',
            },

            // Override the list's default _stayUnvalidated behavior?
            externalStayUnvalidated: {
                type:      Boolean,
                attribute: 'external-stay-unvalidated',
            },

            // Control the position of the the validation message:
            // - false:     below list
            // - true:      as list item
            // - undefined: fixed height ? as list item : below list
            validationMessageIsListItem: {
                type:      Boolean,
                observer:  '_validationMessageIsListItemChanged',
                attribute: 'validation-message-is-list-item'
            },

            // Custom validation function that complements the existing client-side validation
            extraValidation: {
                type:      Function,
                attribute: 'extra-validation',
            },

            // ARIA attributes

            ariaDisabled: {
                type:      String,
                attribute: 'aria-disabled',
                computed:  '_compute_ariaDisabled(disabled)',
                reflect:   true
            },

            ariaMultiselectable: {
                type:      String,
                attribute: 'aria-multiselectable',
                computed:  '_compute_ariaMultiselectable(multiSelect)',
                reflect:   true
            },

            role: {
                type:    String,
                value:   'listbox',
                reflect: true
            },

            _exportFilter: {
                type:     String,
                readOnly: true,
                value:    PTCS.exportparts('filter-field-', PTCS.Textfield)
            },

            _exportLink: {
                type:     String,
                readOnly: true,
                value:    PTCS.exportparts('link-', PTCS.Link)
            },

            // Only used when auto-sizing list width. This is the maximum width that the auto sizing will assign to the list
            maxAutoWidth: {
                type:      Number,
                attribute: 'max-auto-width'
            },

            // Wrap focus when keyboard navigating list
            wrapFocus: {
                type:      Boolean,
                attribute: 'wrap-focus'
            }
        };
    }

    static get observers() {
        return [
            '_createValueSelector(valueSelector, returnOriginalValue, treatValueAsString, selector)',
            '_preventFilterFlicker(filter, hideFilter)',
            '_computeFilter(filter, filterString, _hidden, grouping)',
            '_itemViewChanged(_filter, grouping)'
        ];
    }

    constructor(...arg) {
        super(arg);

        // Default values, sometimes needed during initialization
        this.returnOriginalValue = false;
        this._label = item => item || '';
        this._meta = () => '';
        this._disabled = () => false;

        this._oldSelection = {};
        this._selectionMgr = new ListSelection();
        this._selectedIndexesLength = 0;
        this._selected = -1;
    }

    ready() {
        super.ready();
        this.$.chunker.createItemElement = (index, el) => this._createListItem(index, el);

        this._createValueSelector(this.valueSelector, this.returnOriginalValue, this.treatValueAsString, this.selector); // Need this now
        this._selectorChanged(this.selector);
        this._selectionMgr.bind(this);

        // If there is a current selection, process it
        if (Array.isArray(this.selectedItems) && this.selectedItems.length > 0) {
            this._updateSelection({selectedItems: this.selectedItems, selected: this.selected});
        }

        this.addEventListener('click', ev => this._onClick(ev));

        // When the users interacts with the list
        const interacted = () => {
            this._userInteracted = true;
        };

        // All the ways that the user can interact with the list
        ['mousedown', 'keydown', 'touchstart'].forEach(evName =>
            this.$['list-container'].addEventListener(evName, interacted, {capture: true}));

        this.addEventListener('validity-changed', () => {
            this._chunkerLengthChanged(this._chunkerLength);
            this._refreshChunker();
            let index = 0;
            if (this.multiSelect) {
                if (this.selectedIndexes.length > 0) {
                    index = this.selectedIndexes[0];
                }
            } else if (typeof this._selected === 'number') {
                // Single select
                index = this._selected;
            }
            this.$.chunker.focusedItemIndex = this._itemIndexToViewIndex(index);
        });

        // Show the validation message when we lose focus
        this.addEventListener('blur', () => {
            if (this._userInteracted && !this.externalStayUnvalidated) {
                this._stayUnvalidated = false;
            }
        });

        if (this._stayUnvalidated === undefined) {
            this._stayUnvalidated = true;
        }

        // Initialize keyboard focus
        if (0 <= this._selected && this._selected < this.items.length) {
            // Focus on selected item
            this._chunker.focusedItemIndex = this._itemIndexToViewIndex(this._selected);
        }
    }

    async getUpdateComplete() {
        await super.getUpdateComplete();
        await new Promise(requestAnimationFrame); // Wait for the list items to be updated in the DOM
        await Promise.all(Array.from(this.shadowRoot.querySelectorAll('[part=list-item]')).map(item => item.updateComplete));
        return true;
    }

    _hasNewView() {
        if (!this.__hasNewItems) {
            return; // These items has already been "auto-sized"
        }

        this.__hasNewItems = false;
        this.style.removeProperty('--ptcs-list--auto-width');

        if (!(Array.isArray(this.items) && this.items.length > 0)) {
            return; // No items
        }

        const width = this.style.width;
        this.style.width = `${PTCS.cssDecodeSize(this.maxAutoWidth || 0) || 470}px`;
        const autoWidth = this.autoWidth;
        this.style.width = width;

        if (autoWidth > this.offsetWidth) {
            this.style.setProperty('--ptcs-list--auto-width', `${autoWidth}px`);
        }
    }

    get focusedItemIdx() {
        return this.$.chunker.focusedItemIndex;
    }

    get _chunker() {
        return this.shadowRoot && this.shadowRoot.getElementById('chunker');
    }

    // Callback from _selectionMgr
    _updateSelection(change, noChange) {
        if (noChange) {
            // Backwards compatibility fix
            for (const propName in change) {
                this.dispatchEvent(new CustomEvent(`${window.camelToDashCase(propName)}-changed`, {detail: {value: change[propName], noChange}}));
                this._oldSelection[propName] = change[propName];
            }
            return;
        }

        if (change.hasOwnProperty('selectedItems')) {
            this._selectedSet = this.multiSelect ? new Set(change.selectedItems) : undefined;
        }

        if (change.hasOwnProperty('multiSelect')) {
            if (change.hasOwnProperty('selectedIndexes') && !this.multiSelect) {
                this.reFilter();
            }
        }

        if (change.hasOwnProperty('selectedIndexes')) {
            this._selectedIndexesLength = (this.selectedIndexes && this.selectedIndexes.length) || 0;
            if (this._userInteracted && !this.externalStayUnvalidated) {
                this._stayUnvalidated = this._selectedIndexesLength === 0;
            }
        }

        if (this.multiSelect) {
            this.reFilter(); // The selection influences the filtering
            requestAnimationFrame(() => this._refreshSelection());
        } else {
            this._selectedSet = undefined; // Better safe than sorry
            if (change.hasOwnProperty('selected')) {
                const old = this._selected;
                switch (change.selected) {
                    case undefined:
                    case '':
                    case false:
                    case true:
                    case null:
                        this._selected = -1;
                        break;

                    default: {
                        const _selected = +change.selected;
                        this._selected = _selected >= 0 ? _selected : -1;
                    }
                }
                if (old !== this._selected) {
                    const unfiltered = ix => this._filter && ix >= 0 && !this._filter(this.items[ix], ix);
                    if (unfiltered(old) || unfiltered(this._selected)) {
                        this.reFilter();
                    }

                    const chunker = this._chunker;
                    if (chunker) {
                        requestAnimationFrame(() => {
                            if (old >= 0) {
                                this._refreshIndex(old);
                            }
                            if (this._selected >= 0) {
                                this._refreshIndex(this._selected);
                                this.scrollToIndex(this._selected);
                            }
                        });

                        // Change keyboard focus indicator
                        if (0 <= this._selected && this._selected < this.items.length) {
                            chunker.performUpdate();
                            chunker.focusedItemIndex = this._itemIndexToViewIndex(this._selected);
                        }
                    }
                }
            }
        }

        // Inform Lit about changes
        for (const propName in change) {
            this.requestUpdate(propName, this._oldSelection[propName]);
            this._oldSelection[propName] = change[propName];
        }
    }

    // Set the focus on the first item
    resetFocus(focus) {
        this.updateComplete.then(() => {
            this._chunker.focusedItemIndex = 0;
            if (focus) {
                this._chunker.focus();
            }
        });
    }

    // Set the focus on the last item
    resetFocusLast(focus) {
        this.updateComplete.then(() => {
            this._chunker.setFocusRowIndex(this.$.chunker.numItems - 1);
            if (focus) {
                this._chunker.focus();
            }
        });
    }

    resetSelection() {
        if (this.multiSelect) {
            this.unselectAll();
        } else {
            this.selectedIndexes =  (this.autoSelectFirstRow && Array.isArray(this.items) && this.items.length > 0) ? [0] : [];
        }
    }

    _refreshSelection() {
        const chunker = this._chunker;
        const listItems = (chunker && chunker.querySelectorAll('ptcs-list-item')) || [];
        for (let i = 0; i < listItems.length; i++) {
            const el = listItems[i];
            PTCS.setbattr(el, 'selected', this.isSelectedIndex(+el.getAttribute('index')));
        }
    }

    isSelectedIndex(index) {
        return this._selectedSet ? this._selectedSet.has(this.items[index]) : index === this._selected;
    }

    isSelectedItem(item) {
        return this._selectedSet ? this._selectedSet.has(item) : this.items[this._selected] === item;
    }

    scrollToIndex(index) {
        if (this.__scrollActivated) {
            return;
        }
        this.__scrollActivated = true;
        requestAnimationFrame(() => {
            this.__scrollActivated = false;
            const ixView = this._itemIndexToViewIndex(index);
            if (ixView >= 0) {
                this.$.chunker.scrollTo(ixView);
            }
        });
    }

    reFilter(force) {
        if (this._filter || force) {
            this._itemViewChanged();
        }
    }

    // Private functions
    _createUnselectItem(el, first) {
        // Create
        if (!el || !el.classList.contains('unselect-item')) {
            el = PTCS.createElement('div', {part: 'list-item', class: 'unselect-item', index: '-1', disabled: this.disabled, first});
            el.addEventListener('click', ev => this._clickUnselect(ev));

            const elValue = PTCS.createElement('ptcs-div', {part: 'item-value', disabled: this.disabled});
            const elLabel = PTCS.createElement('ptcs-label', {variant: 'list-item', style: 'width: 100%'});

            elValue.appendChild(elLabel);
            el.appendChild(elValue);

            el.tooltipFunc = () => {
                if (typeof elLabel.tooltipFunc === 'function') {
                    return elLabel.tooltipFunc();
                }
                return '';
            };
            el.tooltipIcon = this.ownerTooltipIcon;
        }

        // Update
        el.firstChild.firstChild.setProperties({
            multiLine:           this.multiLine,
            label:               this.clearSelectionLabel || 'None',
            horizontalAlignment: this.alignment,
            tooltip:             this.ownerTooltip,
            tooltipIcon:         this.ownerTooltipIcon,
            disabled:            this.disabled
        });

        return el;
    }

    _updateUnselectItemDisabled() {
        const el = this.$.chunker.querySelector('.unselect-item');
        if (el) {
            const elValue = el.firstChild;
            const elLabel = elValue.firstChild;
            PTCS.setbattr(el, 'disabled', this.disabled);
            PTCS.setbattr(elValue, 'disabled', this.disabled);
            PTCS.setbattr(elLabel, 'disabled', this.disabled);
        }
    }

    _clearSelectionLabelChanged() {
        if (this.allowNoItemSelection) {
            // Only affects the first item
            this.$.chunker.refresh(0);
        }
    }

    _createValidationItem(el, first) {
        if (!el || !el.getAttribute('validation-item')) {
            el = PTCS.createElement('div', {'validation-item': '', index: '-1', first});
            this._validationMessageEl.singleLine = true;
            el.addEventListener('mousedown', () => this.resetFocus());
            el.appendChild(this._validationMessageEl);
        }

        return el;
    }

    _createGroupItem(group, el, first, last) {
        // Create
        if (!el || el.tagName !== 'PTCS-LIST-GROUP') {
            el = PTCS.createElement('ptcs-list-group', {part: 'list-group', role: 'group'});
        }

        // Update
        PTCS.setbattr(el, 'first', first);
        PTCS.setbattr(el, 'last', last);
        el.setProperties({
            label:            group[$groupKey],
            disabled:         this.disabled,
            multiSelect:      this.multiSelect,
            multiLine:        this.multiLine,
            alignment:        this.alignment,
            ownerTooltip:     this.ownerTooltip,
            ownerTooltipIcon: this.ownerTooltipIcon,
        });

        return el;
    }

    _createListItem(index, el) {
        const first = index === 0;
        const last = (index + 1 === this._chunkerLength2);

        if (this._validationAsListItem()) {
            if (index === 0) {
                return this._createValidationItem(el, first);
            }
            // Adjust index
            --index;
        }

        if (this.allowNoItemSelection) {
            if (index === 0) {
                return this._createUnselectItem(el, first);
            }
            // Adjust index
            --index;
        }

        const ix = this._itemsIndexFiltered?.length ? this._itemsIndexFiltered[index] : index;

        if (typeof ix === 'object') {
            return this._createGroupItem(ix, el, first, last);
        }

        // Create
        if (!el || el.tagName !== 'PTCS-LIST-ITEM') {
            el = PTCS.createElement('ptcs-list-item', {part: 'list-item'});
            el.addEventListener('selected-changed', ev => this._onItemSelectedChanged(el, ev.detail.value));
            el.itemMeta = this.itemMeta;
        }

        const item = this.items[ix];

        // Update
        el.setAttribute('index', ix);
        PTCS.setbattr(el, 'alt', ix % 2 === 1);
        PTCS.setbattr(el, 'first', first);
        PTCS.setbattr(el, 'last', last);
        el.setProperties({
            label:                this._label(item),
            labelMeta:            this._meta(item) || false,
            selected:             this.isSelectedIndex(ix),
            disabled:             this.disabled || this._disabled(item),
            multiSelect:          this.multiSelect,
            multiLine:            this.multiLine,
            alignment:            this.alignment,
            ownerTooltip:         this.ownerTooltip,
            ownerTooltipIcon:     this.ownerTooltipIcon,
            radioButtonSelection: this.radioButtonSelection
        });

        if (this.createListItemAdditionalProperties) {
            // Make sure that the item is initialized before sending it to createListItemAdditionalProperties
            if (typeof el._getItem === 'function') {
                el._getItem();
            }
            this.createListItemAdditionalProperties(el, item);
        }

        return el;
    }

    // Translate item index to scroller index
    _itemIndexToViewIndex(index) {
        if (this._itemsIndexFiltered?.length) {
            // Remap index to filtered list
            index = this._itemsIndexFiltered.findIndex(i => i === index);
        }

        const validationAsListItem = this._validationAsListItem();

        if (validationAsListItem || this.allowNoItemSelection) {
            const extra = validationAsListItem && this.allowNoItemSelection ? 2 : 1;
            return index + extra;
        }
        return index;
    }

    // item[index] has changed. Reflect the change in the virtual scroller
    _refreshIndex(index) {
        if (this.__refreshChunkerOn || !this.shadowRoot) {
            return;
        }
        const ixView = this._itemIndexToViewIndex(index);
        if (ixView >= 0) {
            this.$.chunker.refresh(ixView);
        }
    }

    // Hide filter?
    _filterHidden() {
        // If filter is falsy, then hide the list filter field - unless filter is a string
        return (!this.filter && this.filter !== '') || this.hideFilter;
    }

    _preventFilterFlicker(filter, hideFilter) {
        const hidden = this._filterHidden(filter, hideFilter);
        if (!hidden !== !this.__filterIsHidden) {
            this.__filterIsHidden = hidden;
            this._gap = this.$.chunker.setGap();
        }
    }

    _itemsChanged(items) {
        if (!items) {
            this.items = []; // Restore invalid assignment
            return;
        }
        if (this.__hasNewItems !== undefined) {
            setTimeout(this._hasNewView.bind(this), 80);
        }
        this.__hasNewItems = true;
        this.reFilter(true);
        this._refreshChunker();
        this._selectionMgr.items = items;

        // The items changed, so the list should now be considered as non yet interacted
        this._userInteracted = undefined;
        if (!this.externalStayUnvalidated) {
            this._stayUnvalidated = true;
        }
    }

    _itemMetaChanged(itemMeta) {
        const list = this.$.chunker.querySelectorAll('ptcs-list-item');
        for (let i = list.length - 1; i >= 0; i--) {
            list[i].itemMeta = itemMeta;
        }
    }

    _refreshChunker(wipe) {
        const old = this.__refreshChunkerOn;
        this.__refreshChunkerOn = wipe ? 2 : (this.__refreshChunkerOn || 1); // refresh or rebuild?
        if (old) {
            return; // Already waiting on a refresh
        }
        requestAnimationFrame(() => {
            if (this._waitOnChunkerLength || !this.__refreshChunkerOn) {
                // If the chunker length has changed, then make sure the chunker knows that before refreshing,
                // or if the refresh request has already been handled, don't do it again
                return;
            }
            this.__doChunkerRefresh();
        });
    }

    __doChunkerRefresh() {
        if (this.shadowRoot) { // Sometimes called before the chunker exists
            this.$.chunker.numItems = this._chunkerLength2; // Can be out of sync because of delayed Lit processing
            if (this.__refreshChunkerOn === 2) {
                this.$.chunker.rebuild();
            } else {
                this.$.chunker.refresh();
            }
        }
        this.__refreshChunkerOn = undefined;
    }

    // The ptcs-list-item has changed its selection state (perhaps a user click?)
    _onItemSelectedChanged(el, selected) {
        const index = +el.getAttribute('index');
        if (this.isSelectedIndex(index) === (!!selected)) {
            return; // No change
        }

        if (this.multiSelect) {
            this.$.chunker.focusedItemIndex = this._itemIndexToViewIndex(index);
            if (selected) {
                this.push('selectedIndexes', index);
            } else {
                const i = this.selectedIndexes.findIndex(ix => ix === index);
                if (i >= 0) {
                    this.splice('selectedIndexes', i, 1);
                }
            }
        } else {
            this.selectedIndexes = selected ? [index] : [];
        }

        // Re-check tooltip
        // This function will almost always be caused by a user click, so we should
        // not have to worry about performance
        setTooltipByFocus(); // Clear cached entry
        setTooltipByFocus(el); // Retry with element
    }

    _chunkerLengthChanged(_chunkerLength, oldValue) {
        if (!this._waitOnChunkerLength) {
            this._waitOnChunkerLength = true;
            requestAnimationFrame(() => {
                this._waitOnChunkerLength = false;
                const validationAsListItem = this._validationAsListItem();
                if (validationAsListItem || this.allowNoItemSelection) {
                    // Add a validation message item first, and/or an "Unselect" item first, if there are any items to unselect
                    const extra = validationAsListItem && this.allowNoItemSelection ? 2 : 1;
                    this._chunkerLength2 = this._chunkerLength ? extra + this._chunkerLength : 0;
                } else {
                    this._chunkerLength2 = this._chunkerLength;
                }
                // Is there a pending chunker refresh?
                if (this.__refreshChunkerOn) {
                    this.__doChunkerRefresh();
                }
            });
        }
        this._visibleItems = _chunkerLength;
    }

    _chunkerLength2Changed(_chunkerLength2, oldValue) {
        if (!_chunkerLength2 && oldValue && this._validationMessageEl && this.validationMessageIsListItem) {
            // List was cleared. Re-insert list item validation message, if any
            this._insertValidationMessage(this._validationMessageEl);
        }
    }


    _validationMessageIsListItemChanged() {
        // The explicit position of the validation message changed. Re-insert validation message, if applicable
        if (this._showCurrentValidity() && this._validationMessageEl) {
            this._insertValidationMessage(this._validationMessageEl);
        }
    }

    _gapChangedEv(ev) {
        this._gap = ev.detail.value;
    }

    _gapChanged(_gap) {
        if (this._$frozenHeight) {
            return;
        }

        const {startIx, endIx, numItems} = this.$.chunker;
        const allLoaded = (startIx === 0 && endIx === numItems); // Has all items been loaded?
        const h0 = this.offsetHeight;

        if (_gap > 0 && allLoaded) {
            // Reduce grid height so gap is removed. Note: the "+ 1" is unfortunately needed to adjust for an old bugfix in the virtual scroller
            this.style.setProperty('--ptcs-list-height', `${(h0 - Math.min(this.$.chunker.viewportHeight, _gap + 1))}px`);
        } else if (_gap < 0) {
            // Part of the list is not visible
            const {scrollHeight} = this.$.chunker.elScroll;
            const scrollContainerHeight = this.$.chunker.elScroll.getBoundingClientRect().height; // get actual rendered size
            const cs = getComputedStyle(this);
            const marginTop = PTCS.cssDecodeSize(cs.marginTop, this, true) || 0;
            const marginBottom = PTCS.cssDecodeSize(cs.marginBottom, this, true) || 0;
            const container = this.$['list-container'];
            const containerExtraH = Math.max(0, container.scrollHeight - container.clientHeight); // The container height may be constrained
            const h = Math.ceil(marginTop + this.offsetHeight + marginBottom - scrollContainerHeight + scrollHeight + containerExtraH);
            const maxH = PTCS.cssDecodeSize(cs.getPropertyValue('--ptcs-list-max-height'), this, true) || 571;

            if (h < maxH && h0 > 0 && allLoaded) {
                // All elements of the list are loaded by the scroller, so we now know how high the list needs to be
                this.style.setProperty('--ptcs-list-height', `${h}px`);
            } else {
                // The list can probably become larger
                this.style.removeProperty('--ptcs-list-height');
            }
        }
        const h1 = this.offsetHeight;
        if (h0 !== h1) {
            // Avoid browser flashing by immediatly processing the new height (make the scroller fit without ever showing an incorrect height)
            this.$.chunker.resized();
        }

        // Let theme engine know if the list has a gap
        PTCS.setbattr(this, 'gap', this._gap > 0); // Use current value of _gap, in case this.$.chunker.resized() updated it
    }

    // Public function
    selectItem(index, selectOnly, noscroll) {
        const item = this.items[index];
        if (!item) {
            return;
        }
        const selected = this.isSelectedIndex(index);

        if (selected) {
            if (selectOnly) {
                return; // Item is already selected
            }
            // Unselect the item
            if (this.multiSelect) {
                const ix = this.selectedIndexes.findIndex(i => i === index);
                if (ix >= 0) {
                    this.selectedIndexes = this.selectedIndexes.filter(i => i !== index);
                }
            } else {
                this.selectedIndexes = [];
            }
        } else if (this.multiSelect) {
            this.selectedIndexes = [...this.selectedIndexes, index];
        } else {
            this.selected = index;
        }

        if (selectOnly && !selected && !noscroll) {
            this.scrollToIndex(index);
        }
    }

    get multiSelect() {
        return this._selectionMgr.multiSelect;
    }

    set multiSelect(_multiSelect) {
        this._selectionMgr.multiSelect = _multiSelect;
    }

    _radioButtonSelectionChanged() {
        this._refreshChunker();
    }

    // Expose the function that creates list labels
    static labelFunc(selector, itemMeta) {
        let _label;

        if (!selector) {
            _label = item => item;
        } else if (typeof selector === 'string') {
            _label = item => item[selector];
        } else if (selector.constructor && selector.call && selector.apply) {
            _label = selector; // item => selector(item);
        } else {
            console.error('Invalid ptcs-list label selector', selector);
            _label = item => item;
        }

        if (!itemMeta || (itemMeta.type !== 'link' && itemMeta.type !== 'function')) {
            return item => {
                const retLabel = item ? _label(item) : '';
                if (retLabel === undefined || retLabel === null) {
                    return '';
                }
                return typeof retLabel !== 'string' ? retLabel.toString() : retLabel;
            };
        }
        return item => {
            const retLabel = item ? _label(item) : '';
            return (retLabel === undefined || retLabel === null) ? '' : retLabel;
        };
    }

    // Selectors: pulls information from the items
    _selectorChanged(selector) {
        this._label = PTCS.List.labelFunc(selector, this.itemMeta);
        this._refreshChunker();
    }

    static valueFunc(valueSelector, returnOriginalValue, treatValueAsString, selector) {
        // Use label selector as default
        if (!valueSelector) {
            valueSelector = selector;
        }

        // Create selector function
        let _value;
        if (!valueSelector) {
            _value = item => item;
        } else if (typeof valueSelector === 'string') {
            _value = item => item[valueSelector];
        } else if (typeof valueSelector === 'function') {
            _value = valueSelector;
        } else {
            console.error('Invalid ptcs-list value selector', valueSelector);
            _value = item => item; // Fallback
        }

        if (returnOriginalValue === undefined ? treatValueAsString : !returnOriginalValue) {
            return item => {
                const retValue = item ? _value(item) : '';
                if (retValue === undefined || retValue === null) {
                    return '';
                }
                return typeof retValue === 'string' ? retValue : retValue.toString();
            };
        }
        return item => item !== undefined && item !== '' ? _value(item) : undefined;
    }

    _createValueSelector(valueSelector, returnOriginalValue, treatValueAsString, selector) {
        this._selectionMgr.valueOf = PTCS.List.valueFunc(valueSelector, returnOriginalValue, treatValueAsString, selector);
    }

    _stateSelectorChanged(stateSelector) {
        if (typeof stateSelector === 'string') {
            const _checkState = (stateToCheck) => {
                return item => {
                    if (!item) {
                        return false;
                    }
                    const state = item[stateSelector];
                    if (typeof state !== 'string') {
                        return false;
                    }
                    return state.toLowerCase() === stateToCheck;
                };
            };

            this._disabled = _checkState('disabled');
            this._hidden = _checkState('hidden');

        } else if (typeof stateSelector === 'function') {
            const _checkState = (stateToCheck) => {
                return item => {
                    if (!item) {
                        return false;
                    }
                    const state = stateSelector(item);
                    if (typeof state !== 'string') {
                        return false;
                    }
                    return state.toLowerCase() === stateToCheck;
                };
            };

            this._disabled = _checkState('disabled');
            this._hidden = _checkState('hidden');
        } else {
            this._disabled = () => false;
            this._hidden = null;
        }

        this._refreshChunker();
    }

    _metaSelectorChanged(metaSelector) {
        if (!metaSelector) {
            this._meta = () => '';
        } else {
            let _meta;
            if (typeof metaSelector === 'string') {
                _meta = item => item[metaSelector];
            } else if (metaSelector.constructor && metaSelector.call && metaSelector.apply) {
                _meta = metaSelector; // item => metaSelector(item);
            } else {
                console.error('Invalid ptcs-list metaSelector', metaSelector);
                _meta = () => '';
            }

            this._meta = item => {
                const retMeta = item ? _meta(item) : '';
                if (retMeta === undefined || retMeta === null) {
                    return '';
                }

                return typeof retMeta !== 'string' ? retMeta.toString() : retMeta;
            };
        }

        this._refreshChunker();
    }

    _textChangedEv(ev) {
        this.filterString = ev.detail.value;
    }

    // Filtering
    _computeFilter(filter, filterString, _hidden, grouping) {
        const _shown = _hidden ? (item) => !_hidden(item) : null;
        if (filter === undefined || filter === null || filter === 0) {
            this._filter = _shown;
        }
        const q = (filterString || '').toLowerCase();
        if (!q) {
            this._filter = typeof filter === 'function' ? filter(filterString, (_shown ? _shown : () => true)) : _shown;
        }
        const groupKey = grouping && (typeof grouping === 'function' ? grouping : item => item[grouping]);
        const f = _shown ? item => _shown(item) && this._filterMatch(item, q, groupKey) : item => this._filterMatch(item, q, groupKey);

        this._filter = typeof filter === 'function' ? filter(filterString, f) : f;
    }

    _filterMatch(item, fs, groupKey) {
        if (this.isSelectedItem(item)) {
            return true;
        }

        const label = this._label(item);
        if (typeof label === 'string' && label.toLowerCase().indexOf(fs) >= 0) {
            return true;
        }

        const meta = this._meta(item);
        if (typeof meta === 'string' && meta.replace(/\s/g, '').toLowerCase().indexOf(fs) >= 0) {
            return true;
        }

        if (groupKey) {
            const key = groupKey(item);
            if (typeof key === 'string' && key.toLowerCase().indexOf(fs) >= 0) {
                return true;
            }
        }

        return false;
    }

    _itemViewChanged() {
        const {_filter, grouping, items} = this;

        // Group items. Input: item indexes
        const groupIt = indexes => {
            const groupKey = typeof grouping === 'function' ? index => grouping(items[index], index) : index => items[index][grouping] || '';
            const grouped = Object.groupBy(indexes, groupKey);

            return Object.keys(grouped).reduce((acc, key) => {
                acc.push({[$groupKey]: key || 'Ungrouped'});
                acc.push(...grouped[key]);
                return acc;
            }, []);
        };

        if (_filter) {
            // Collect indexes to filtered items
            const filtered = [];
            for (let n = items.length, i = 0; i < n; i++) {
                if (_filter(items[i], i)) {
                    filtered.push(i);
                }
            }

            this._itemsIndexFiltered = grouping ? groupIt(filtered) : filtered;
            this._chunkerLength = this._itemsIndexFiltered.length;
        } else if (grouping) {
            // Grouping, but no filter
            this._itemsIndexFiltered = groupIt(items.map((_, index) => index));
            this._chunkerLength = this._itemsIndexFiltered.length;
        } else {
            // Standard view: no filter and no grouping
            this._chunkerLength = items.length;
            this._itemsIndexFiltered = [];
        }
        this._refreshChunker();
    }

    _rowHeightChanged(rowHeight) {
        const m = /^ *([0-9]+\.?[0-9]*)([a-zA-Z]*) *$/g.exec(rowHeight || '');
        this.style.setProperty('--ptcs-list-item--height', !m ? '34px' : m[1] + (m[2] || 'px'));
    }

    _multiSelectLabel() {
        // Anything to select or unselect?
        if (!this._chunkerLength) {
            return '';
        }

        // Only search list if anything is selected
        return this._selectedIndexesLength
            ? this.clearSelectedItemsLabel // At least one item is selected
            : this.selectAllLabel; // No displayed items are selected
    }

    _clickMultiSelect() {
        if (this.disabled || !this.items.length) {
            return;
        }
        if (this.selectedIndexes.length) {
            this.unselectAll();
        } else {
            this.selectAll();
        }

    }

    _clickUnselect() {
        if (this.disabled) {
            return;
        }
        this.unselectAll();
        this.$.chunker.focusedItemIndex = this._validationAsListItem() ? 1 : 0; // Move focus to unselect item
    }

    _selIx(ev, cb) {
        for (let el = ev.srcElement; el; el = el.parentNode) {
            const ix = el.getAttribute ? el.getAttribute('index') : null;

            if (ix) {
                const m = / *(-?[0-9]+) */g.exec(ix);

                if (m) {
                    cb(el, Number(m[1]));
                    break;
                }
            }
        }
    }

    _dblClick(ev) {
        if (this.disabled) {
            return;
        }

        this._selIx(ev, (el, ix) => {
            if (ix >= 0) {
                this.dispatchEvent(new CustomEvent('DoubleClicked', {
                    bubbles:  true,
                    composed: true,
                    detail:   {key: ix}
                }));
            }
        });
    }

    unselectAll() {
        if (!this.selectedIndexes || this.selectedIndexes.length === 0) {
            // No selections
            return; // Important: don't touch selectedIndexes if it has not been initialized
        }

        this.selectedIndexes = [];
    }

    selectAll() {
        if (this.items.length === 0 || !this.multiSelect) {
            return; // Nothing to select or invalid use
        }

        const extractNumbers = (a, v) => {
            if (typeof v === 'number') {
                a.push(v);
            }
            return a;
        };

        // Select all items
        this.selectedIndexes = this._itemsIndexFiltered.length
            ? this._itemsIndexFiltered.reduce(extractNumbers, [])
            : this.items.map((_, index) => index);
    }

    refresh() {
        // If client needs a refresh
        this._refreshChunker();
    }


    _disabledChanged() {
        this._refreshChunker();
        this._updateUnselectItemDisabled();
    }

    _multiLineChanged() {
        this._refreshChunker();
    }

    _alignmentChanged() {
        this._refreshChunker();
    }

    _allowNoItemSelectionChanged() {
        this._chunkerLengthChanged(this._chunkerLength);
        this._refreshChunker(true);
    }

    _hideNoMatches() {
        return this._chunkerLength2 > 0 || !this.noMatchesLabel;
    }

    set freezeListHeight(freeze) {
        this._$frozenHeight = freeze;
    }

    get changesOnly() {
        return this._selectionMgr.changesOnly;
    }

    set changesOnly(_changesOnly) {
        this._selectionMgr.changesOnly = _changesOnly;
    }

    get selectedIndexes() {
        return this._selectionMgr.selectedIndexes;
    }

    set selectedIndexes(_selectedIndexes) {
        this._selectionMgr.selectedIndexes = _selectedIndexes;
    }

    get selectedItems() {
        return this._selectionMgr.selectedItems;
    }

    set selectedItems(_selectedItems) {
        this._selectionMgr.selectedItems = _selectedItems;
    }

    get selectedValue() {
        return this._selectionMgr.selectedValue;
    }

    set selectedValue(value) {
        this._selectionMgr.selectedValue = value;
    }

    get selected() {
        return this._selectionMgr.selected;
    }

    set selected(value) {
        this._selectionMgr.selected = value;
    }

    get autoSelectFirstRow() {
        return this._selectionMgr.autoSelectFirstRow;
    }

    set autoSelectFirstRow(_autoSelectFirstRow) {
        this._selectionMgr.autoSelectFirstRow = _autoSelectFirstRow;
    }

    _insertValidationMessage(messageElement) {
        messageElement.setAttribute('pos', 'bottom'); // First try placing message at bottom

        const prevHeight = this.$.chunker.offsetHeight;
        this.shadowRoot.appendChild(messageElement);
        this._gap = this.$.chunker.setGap();
        this._hasFixedHeight = this.$.chunker.offsetHeight < prevHeight;

        if ((this._hasFixedHeight && this.validationMessageIsListItem !== false) || this.validationMessageIsListItem) {
            // In case there is a fixed height, insert the message as the first list-item instead of below the list
            this.shadowRoot.removeChild(messageElement);

            if (this.validationMessageIsListItem) {
                const msgParent = this.$.chunker.querySelector('div[validation-item]');
                if (msgParent) {
                    // Re-cycle an existing validation item parent div
                    if (msgParent.firstChild) {
                        msgParent.firstChild.remove();
                    }
                    msgParent.appendChild(messageElement);
                } else if (this.items.length) {
                    // Add validation message into an existing list
                    this._createListItem(0, messageElement);
                } else {
                    const listContainer = this.$['list-container'];

                    // If there is a previous <div> present, then remove it...
                    const prevMsgDiv = listContainer.querySelector('div[validation-item]');
                    if (prevMsgDiv) {
                        prevMsgDiv.remove();
                    }

                    listContainer.insertBefore(this._createListItem(0, messageElement), this.$.chunker);
                }
            }

            this._chunkerLengthChanged(this._chunkerLength);
            this._refreshChunker();
        }

        messageElement.setAttribute('pos', this._hasFixedHeight ? 'list-item' : 'bottom');
    }

    _removedValidationMessage() {
        this._gap = this.$.chunker.setGap();
    }

    _computeHideList(noMatchesLabel, _visibleItems, hideEmptyList) {
        return !noMatchesLabel && !_visibleItems && hideEmptyList;
    }

    // ARIA attributes

    _compute_ariaDisabled(disabled) {
        return disabled ? 'true' : false;
    }

    _compute_ariaMultiselectable(multiSelect) {
        return multiSelect ? 'true' : false;
    }

    static get $parts() {
        if (!this._$parts) {
            this._$parts = [
                'label', 'list', 'list-item', 'multi-select', 'link', 'filter',
                'filter-field', 'icon-close', 'list-container', 'list-items-container',
                'item-checkbox', 'item-value', /* 'state-value', */ 'item-meta', 'item-radio',
                ...PTCS.partnames('filter-field-', PTCS.Textfield),
                ...PTCS.partnames('link-', PTCS.Link)
            ];
        }
        return this._$parts;
    }

    _ownerTooltipChanged(ownerTooltip) {
        const list = this.$.chunker.querySelectorAll('ptcs-list-item');
        if (list) {
            for (let i = list.length - 1; i >= 0; i--) {
                list[i].ownerTooltip = ownerTooltip;
            }
        }
    }

    _ownerTooltipIconChanged(ownerTooltipIcon) {
        const list = this.$.chunker.querySelectorAll('ptcs-list-item');
        if (list) {
            for (let i = list.length - 1; i >= 0; i--) {
                list[i].ownerTooltipIcon = ownerTooltipIcon;
            }
        }
    }

    _onClick(ev) {
        if (!this.disabled && !this.isIDE) {
            ev.preventDefault();
        }
    }

    _validateList(required, extraValidation, _selectedIndexesLength) {
        const messages = [];

        if (!required && !extraValidation) {
            return undefined; // No internal or extra validation enabled
        }

        // Required
        if (required && _selectedIndexesLength === 0) {
            messages.push(this.requiredMessage);
            return messages;
        }

        // Leave final say to the custom validation, if any
        return typeof extraValidation === 'function' ? extraValidation(this) : true;
    }

    _validationAsListItem() {
        if (this.validationMessageIsListItem === false || !this._showCurrentValidity()) {
            return false; // Client has requested that validation message should be placed below list, not inside it
        }
        return this._hasFixedHeight || (this.validationMessageIsListItem && this._validationMessageEl);
    }

    get autoWidth() {
        let aw; // Maximum autoWidth of available list item
        let ae; // List item wirth maximum autoWidth

        // Find aw and ae
        this.$.chunker.querySelectorAll('ptcs-list-item').forEach(el => {
            const aw2 = el.autoWidth;
            if (!ae || aw < aw2) {
                ae = el;
                aw = aw2;
            }
        });

        if (!ae) {
            return Math.max(this.offsetWidth, 120); // Whatever...
        }

        // Compute padding
        const cs = getComputedStyle(ae);
        const pl = cs.getPropertyValue('padding-left');
        const pr = cs.getPropertyValue('padding-right');
        const padding = (Number(pl.substring(0, pl.indexOf('px'))) || 0) + (Number(pr.substring(0, pr.indexOf('px'))) || 0);

        // Add padding and borders + scrollbar (etc) + 1.5 for rounding errors
        return aw + padding + this.offsetWidth - this.$.chunker.viewportWidth + 1.5;
    }

    // Polymer to Lit hacks (push and pop should not clear selection)
    push(...arg) {
        const old = this._selectionMgr._xferMS;
        try {
            this._selectionMgr._xferMS = true;
            super.push(...arg);
        } finally {
            this._selectionMgr._xferMS = old;
        }
    }
    pop(...arg) {
        const old = this._selectionMgr._xferMS;
        try {
            this._selectionMgr._xferMS = true;
            super.pop(...arg);
        } finally {
            this._selectionMgr._xferMS = old;
        }
    }
};

customElements.define(PTCS.List.is, PTCS.List);
