import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {ifDefined} from 'lit/directives/if-defined.js';
import {PTCS} from 'ptcs-library/library.js';
import {ListSelection} from 'ptcs-list/list-selection.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-list/ptcs-list.js';
import 'ptcs-icon/ptcs-icon.js';
import 'ptcs-icons/cds-icons.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-behavior-validate/ptcs-behavior-validate.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';

// Properties that the dropdown needs to push into the popup list
const sharedListProperties = {
    items:                              'items',
    disabled:                           'disabled',
    selector:                           'selector',
    valueSelector:                      'valueSelector',
    stateSelector:                      'stateSelector',
    metaSelector:                       'metaSelector',
    treatValueAsString:                 'treatValueAsString',
    returnOriginalValue:                'returnOriginalValue',
    alignment:                          'alignment',
    autoSelectFirstRow:                 'autoSelectFirstRow',
    rowHeight:                          'rowHeight',
    clearSelectionItem:                 'allowNoItemSelection',
    filter:                             'filter',
    filterHintText:                     'hintText',
    grouping:                           'grouping',
    _tabindex:                          'tabindex',
    clearSelectionLabel:                'clearSelectionLabel',
    itemMeta:                           'itemMeta',
    tooltip:                            'ownerTooltip',
    tooltipIcon:                        'ownerTooltipIcon',
    noMatchesLabel:                     'noMatchesLabel',
    selectAllLabel:                     'selectAllLabel',
    clearSelectedItemsLabel:            'clearSelectedItemsLabel',
    createListItemAdditionalProperties: 'createListItemAdditionalProperties',
    comboboxMode:                       'hideEmptyList'
};

PTCS.Dropdown = class extends PTCS.BehaviorTabindex(PTCS.BehaviorValidate(PTCS.BehaviorTooltip(PTCS.BehaviorFocus(
    PTCS.BehaviorStyleable((L2Pw(LitElement)), ['open', 'closed']))))) {

    static get styles() {
        return css`
      :host {
        display: inline-flex;
        flex-direction: column;
        width: 100%;
      }

      :host([value-hide]) {
        width: auto;
      }

      [part=label] {
        flex: 0 0 auto;

        flex-shrink: 0;
      }

      [part=label][hidden] {
        display: none;
      }

      [part=select-box] {
        display: flex;
        flex-flow: row nowrap;
        place-content: center space-between;
        align-items: center;
        flex-grow: 1;
        box-sizing: border-box;
      }

      /* #select[part~=select-box] - added specifity for IE/Edge to be stronger than theme styling */
      :host([display-mode=small]) #select[part~=select-box] {
        min-height: unset;
        border-style: hidden;
        background-color: transparent;
      }

      :host(:not([label=""])) [part="label"] {
        display: inline-flex;
        padding-bottom: 4px;
      }

      /* value element */
      ptcs-list-item[part=list-item] {
        height: 100%;
        width: calc(100% - 18px);
      }

      [part~=selected-item-value] {
        grid-row: 1;
        grid-column: 2;
        align-self: center;
        overflow: hidden;
        max-width: 100%;
      }

      ptcs-label[variant=list-item] {
        max-width: 100%;
      }

      :host([alignment=left]) [part~=selected-item-value] {
        justify-self: start;
      }

      :host([alignment=center]) [part~=selected-item-value] {
        justify-self: center;
      }

      :host([alignment=right]) [part~=selected-item-value] {
        justify-self: end;
      }

      /* value first child - responsible for alignment */
      div[part=list-item] {
        max-width: 100%;
        height: 100%;
        display: flex;

        justify-content: flex-start; /* flex-start / center / flex-end */
        align-items: center; /* vertical alignment */
      }

      .img-dropdown {
        max-width: 100%;
      }

      /* CSS selector for ptcs-tabs dropdown implementation */
      ptcs-list-item[hidden]{
        display: none;
      }

      ptcs-list-item {
        display: grid;

        grid-template-columns: auto minmax(0, 1fr);
        grid-template-rows: 1fr auto;

        overflow: hidden;
      }`;
    }

    render() {
        return html`<ptcs-label id="label" part="label" label=${this.label} ?hidden=${!this.label}
           multi-line .horizontalAlignment=${this.labelAlignment} disable-tooltip></ptcs-label>
        <ptcs-div id="select" part="select-box" display-mode=${this.displayMode} state-key=${ifDefined(this._stateKey(this._selectedIndexesLength))}
            @click=${this._onClick}>
            <ptcs-list-item id="item" part="list-item" .disabled=${this.disabled} disable-tooltip .itemValue=${'selected-item-value'}
             .label=${this._value} .itemMeta=${this._itemType} .alignment=${this.alignment} .hint=${this.hintText}
             ?hidden=${this.valueHide}></ptcs-list-item>
        </ptcs-div>`;
    }

    static get is() {
        return 'ptcs-dropdown';
    }

    static get properties() {
        return {
            displayMode: {
                type:      String,
                value:     'default',
                attribute: 'display-mode',
                observer:  '_displayModeChanged'
            },

            items: {
                type:        Array,
                observer:    '_itemsChanged',
                observeWhen: 'immediate'
            },

            noMatchesLabel: {
                type:      String,
                attribute: 'no-matches-label'
            },

            // Enable changes-only mode? (= don't fire change events unless the property has changed. The legacy approach is to
            // fire change events if an assigned property value differs from the resulting value, even if it doesn't change it.)
            changesOnly: {
                type:      Boolean,
                attribute: 'changes-only'
            },

            selectedItems: {
                type:       Array,
                notify:     true,
                attribute:  'selected-items',
                noAccessor: true
            },

            _visibleItems: {
                type:     Number,
                observer: '_visibleItemsChanged'
            },

            hintText: {
                type:      String,
                value:     '',
                reflect:   true,
                attribute: 'hint-text'
            },

            // Should the 'filter' option of the list be activated?
            filter: {
                type: Boolean
            },

            // Hint text of the *filter* (the dropdown has a separate one)
            filterHintText: {
                type:      String,
                attribute: 'filter-hint-text'
            },

            // Flag set when the dropdown is used within a combobox (this makes slight changes to things
            // like the focus and shrinking the dropdown list when shown above the dropdown)
            comboboxMode: {
                type:      Boolean,
                attribute: 'combobox-mode'
            },

            clearSelectionLabel: {
                type:      String,
                value:     '',
                reflect:   true,
                attribute: 'clear-selection-label'
            },

            clearSelectionItem: {
                type:      Boolean,
                attribute: 'clear-selection-item'
            },

            itemMeta: {
                type:      Object,
                value:     {type: 'text'},
                attribute: 'item-meta'
            },

            icon: {
                type: String
            },

            _itemType: {
                type:     Object,
                computed: '_computeItemType(_selectedIndexesLength, itemMeta)'
            },

            _listId: {
                type: String
            },

            _labelFunc: {
                type: Function
            },

            label: {
                type:         String,
                value:        '',
                reflect:      true,
                defaultValue: ''
            },

            labelAlignment: { // 'left', 'center', 'right'
                type:      String,
                value:     'left',
                reflect:   true,
                attribute: 'label-alignment'
            },

            alignment: { // 'left', 'center', 'right'
                type:    String,
                value:   'left',
                reflect: true
            },

            valueHide: {
                type:      Boolean,
                reflect:   true,
                attribute: 'value-hide'
            },

            disabled: {
                type:     Boolean,
                reflect:  true,
                observer: '_reflect_disabled_to_small_button'
            },

            // 'closed' || 'open'
            mode: {
                type:        String,
                observer:    '_modeChanged',
                observeWhen: 'immediate',
                notify:      true
            },

            multiSelect: {
                type:      Boolean,
                attribute: 'multi-select'
            },

            selectedIndexes: {
                type:       Array,
                notify:     true,
                attribute:  'selected-indexes',
                noAccessor: true
            },

            // For validation
            _selectedIndexesLength: {
                type:     Number,
                validate: '_validateDropdown(required, extraValidation)'
            },

            selected: {
                type:       Number,
                notify:     true,
                noAccessor: true
            },

            selectedValue: {
                type:       String,
                notify:     true,
                validate:   '_validateSelectedValue(extraValidation)',
                indirect:   true, // This property can change indirectly. Validator need to listen to the change event
                attribute:  'selected-value',
                noAccessor: true
            },

            selector: {
                observer:    '_createValueSelector',
                observeWhen: 'immediate',
            },

            valueSelector: {
                observer:    '_createValueSelector',
                observeWhen: 'immediate',
                attribute:   'value-selector'
            },

            stateSelector: {
                value:     null,
                attribute: 'state-selector'
            },

            treatValueAsString: { // Deprecated
                type:        Boolean,
                observer:    '_createValueSelector',
                observeWhen: 'immediate',
                attribute:   'treat-value-as-string'
            },

            returnOriginalValue: {
                observer:    '_createValueSelector',
                observeWhen: 'immediate',
                type:        Boolean,
                attribute:   'return-original-value'
            },

            metaSelector: {
                value:     null,
                attribute: 'meta-selector'
            },

            autoSelectFirstRow: {
                type:       Boolean,
                attribute:  'auto-select-first-row',
                noAccessor: true
            },

            rowHeight: {
                type:      String,
                attribute: 'row-height'
            },

            maxListHeight: {
                type:      Number,
                attribute: 'max-list-height'
            },

            listMaxWidth: {
                type:      Number,
                attribute: 'list-max-width'
            },

            _tabindex: {
                type:     String,
                computed: '_computeTabindex(tabindex)'
            },

            selectAllLabel: {
                type:      String,
                value:     'Select All',
                attribute: 'select-all-label'
            },

            clearSelectedItemsLabel: {
                type:      String,
                attribute: 'clear-selected-items-label'
            },

            allLabel: {
                type:      String,
                attribute: 'all-label'
            },

            selectedLabel: {
                type:      String,
                attribute: 'selected-label'
            },

            listMarginTop: {
                type:      Number,
                attribute: 'list-margin-top'
            },

            // Handles its own focus styling - no need for FocusBehavior to track its position
            _ownFocusStyling: {
                type:     Boolean,
                readOnly: true
            },

            // Specifies field name that contains group key or function that returns group key
            grouping: {
                type: String // or Function: (item) => String
            },

            createListItemAdditionalProperties: Function,

            // To override the default dropdown list position (mainly an ad hoc adjustment for ptcs-tabs for now)
            customListPosRect: {
                type:      Object,
                attribute: 'custom-list-post-rect'
            },

            // Validation properties
            required: {
                type:    Boolean,
                isValue: required => !!required
            },

            requiredMessage: {
                type:      String,
                attribute: 'required-message'
            }
        };
    }

    static get observers() {
        return [
            '_updateSelectedStateForSmallButton(displayMode, mode)',
            '_createLabelSelector(selector, itemMeta)'
        ];
    }

    constructor() {
        super();
        this._oldSelection = {};
        this._selectionMgr = new ListSelection();
        this.displayMode = 'default';
        this.items = [];
        this.selectedItems = [];
        this._selectedIndexesLength = 0;
        this.filter = false;
        this.filterHintText = 'Filter';
        this.comboboxMode = false;
        this.clearSelectionItem = false;
        this.mode = 'closed';
        this.multiSelect = false;
        this.selectedIndexes = [];
        this.rowHeight = '34';
        this.listMaxWidth = 330;
        this.clearSelectedItemsLabel = 'Clear Selected Items';
        this.allLabel = 'All';
        this.selectedLabel = 'Selected';
        this.listMarginTop = 8;
        this.returnOriginalValue = false;
        this._setOwnFocusStyling = true;
    }

    ready() {
        super.ready();
        this.tooltipFunc = this._monitorTooltip.bind(this);
        this.addEventListener('focus', this._showTooltip);
        this.addEventListener('blur', this._tooltipClose);
        this.addEventListener('keydown', this._keyDown);
        this._trackFocus(this, this.$.select);
        this._createValueSelector(); // Need this now
        this._selectionMgr.bind(this);

        if (this._stayUnvalidated === undefined) {
            this._stayUnvalidated = true;
        }
    }

    connectedCallback() {
        super.connectedCallback();
        if (this._list) {
            this._attachList();
        }
    }

    disconnectedCallback() {
        // Remove the dropdown list
        if (this._list && this._list.parentNode) {
            document.body.removeChild(this._list);
        }
        super.disconnectedCallback();
    }

    _keyDown(ev) {
        // Open the list from select box
        switch (ev.key) {
            case ' ':
            case 'ArrowDown':
            case 'Enter':
                this.shadowRoot.getElementById('select').click();
                ev.preventDefault();
        }
    }

    // Called e.g. by the combobox to access the list object
    createPopupList() {
        if (!this._list) {
            this._initList();
        }
        return this._list;
    }

    _focusOnDropdown() {
        requestAnimationFrame(() => {
            this.focus();
        });
    }

    resetFocus() {
        if (this._list !== undefined) {
            this._list.resetFocus();
        }
    }

    resetSelection() {
        if (this._list !== undefined) {
            this._list.resetSelection();
        } else if (this.autoSelectFirstRow) {
            this.selected = 0;
        } else {
            this.selectedIndexes = []; // TW-94189
        }
    }

    _attachList() {
        if (this._list.parentElement) {
            // The list is already in place
            return;
        }

        this._list.__saSa = this.__saSa;

        document.body.appendChild(this._list);

        // Hack to add CSS styling
        if (!this._list.__$12$345) {
            this._list.__$12$345 = true;

            // update css rules of the list
            this._list.style.position = 'absolute';
            this._list.style.zIndex = '99996';
            this._list.style.boxSizing = 'border-box';
            this._list.style.cursor = 'pointer';

            // add 2 css rules for the created list as a first <style> child so reules added later can override them.
            const style = document.createElement('style');
            style.appendChild(document.createTextNode('[part=list-container] { background: #ffffff; box-sizing: border-box; }'));
            style.appendChild(document.createTextNode('[part=list-item] { box-sizing: border-box; padding-left: 8px; padding-right: 8px; }'));
            this._list.performUpdate();
            this._list.shadowRoot.insertBefore(style, this._list.shadowRoot.firstChild);

            const filterElt = this._list.shadowRoot.querySelector('[part=filter]');
            if (filterElt) {
                filterElt.addEventListener('click', ev => {
                    ev.cancelBubble = true;
                });
            }
        }
    }

    get selectedValue() {
        return this._selectionMgr.selectedValue;
    }

    set selectedValue(_selectedValue) {
        // TW-87197: apparently some kind of hack needed for Angular.
        if (_selectedValue === undefined) {
            return;
        }

        this._selectionMgr.selectedValue = _selectedValue;
    }

    async _displayModeChanged(displayMode) {
        let el = this.shadowRoot.getElementById('icon');
        if (el) {
            this.$.select.removeChild(el);
        }

        if (displayMode === 'small') {
            el = document.createElement('ptcs-button');
            el.setAttribute('mode', 'icon');
            el.setAttribute('variant', 'small');
            el.setAttribute('exportparts', PTCS.exportparts('icon-', PTCS.Button));
            // remove default tabindex
            el.noTabindex = true;
        } else {
            // 'default'
            el = document.createElement('ptcs-icon');
        }
        if (this.icon) {
            el.setAttribute('icon', this.icon);
        } else {
            el.setAttribute('icon', 'cds:icon_chevron_right_mini');
        }
        el.setAttribute('exportparts', PTCS.exportparts('icon-', PTCS.Icon));
        el.setAttribute('part', 'icon');
        el.setAttribute('id', 'icon');

        this.$.select.appendChild(el);

        if (!this.icon) {
            await el.updateComplete;
            switch (el.tagName) {
                case 'PTCS-BUTTON': el.shadowRoot.querySelector('ptcs-icon').style.transform = 'rotate(90deg)'; break;
                case 'PTCS-ICON': el.style.transform = 'rotate(90deg)'; break;
            }
        }
    }

    _reflect_disabled_to_small_button(disabled) {
        const el = this.shadowRoot.getElementById('icon');
        if (el) {
            el.disabled = disabled;
        }
    }

    _updateSelectedStateForSmallButton(displayMode, mode) {
        const el = this.shadowRoot.getElementById('icon');

        if (!el) {
            return;
        }

        if (displayMode === 'small' && mode === 'open') {
            el.setAttribute('selected', '');
        } else {
            el.removeAttribute('selected');
        }
    }

    _computeItemType(_selectedIndexesLength, itemMeta) {
        if (_selectedIndexesLength > 1) {
            return {type: 'text'};
        }
        return itemMeta;
    }

    _createLabelSelector(selector, itemMeta) {
        this._labelFunc = PTCS.List.labelFunc(selector, itemMeta);
    }

    _createValueSelector() {
        const {valueSelector, returnOriginalValue, treatValueAsString, selector} = this;
        this._selectionMgr.valueOf = PTCS.List.valueFunc(valueSelector, returnOriginalValue, treatValueAsString, selector);
    }

    get _value() {
        if (!Array.isArray(this.selectedIndexes) || this.selectedIndexes.length <= 0) {
            // Nothing is selected
            return this.hintText || '';
        }

        if (this.selectedIndexes.length === 1 || !this.multiSelect) {
            if (!this._labelFunc) {
                this._createLabelSelector(this.selector, this.itemMeta);
            }
            // Single item is selected
            return this._labelFunc(this.items[this.selectedIndexes[0]]);
        }

        return this.selectedIndexes.length !== this.items.length
            ? `${this.selectedIndexes.length} ${this.selectedLabel}` // Several items are selected
            : `${this.allLabel} ${this.selectedLabel}`; // All items are selected
    }

    _modeChanged(mode) {
        if (mode === 'open') {
            this.setAttribute('open', '');
            this.removeAttribute('closed');
            this._showList();
        } else if (this._list) {
            this.setAttribute('closed', '');
            this.removeAttribute('open');
            this._hideList();
            // Inform client that the dropdown list closed
            requestAnimationFrame(() => {
                this.dispatchEvent(new CustomEvent('dropdown-closed', {
                    bubbles:  false,
                    composed: false,
                    detail:   this.selectedValue
                }));
            });
        }
    }

    _getFirstSubFocusable(cntr) {
        for (let el = cntr.firstChild; el; el = el.nextSibling) {
            if (el.tabIndex >= 0) {
                const br = el.getBoundingClientRect();
                if (br.width || br.height) {
                    return el;
                }
            }
            const el2 = this._getFirstSubFocusable(el);
            if (el2) {
                return el2;
            }
        }
        return null;
    }

    // _list usage
    async _showList() {
        // Make sure we have a list
        if (!this._list) {
            this._initList();
            console.assert(this._list);
        }

        if (this._list) {
            this._attachList();
        }

        // No restriction on the list height
        this._list.style.removeProperty('--ptcs-list-max-height');

        // Default list dimensions according to visual design - width: 330px, height: 571px
        // The max-height depends on if the list shows a filter and/or a multi-select
        // select-all option. A proper support for that would be complex. Subtracting 34px
        // for each such option is _hopefully_ an acceptable fallback for now. Subtracting
        // 24px from max-width is mainly for handling scrollbars
        const _list = this._list;
        const box = this.$.select.getBoundingClientRect();
        const above = box.top;
        const below = window.innerHeight - box.bottom;
        const extra = 4 + this.listMarginTop + (_list.multiSelect ? 34 : 0) + (_list.filter && !_list.hideFilter ? 34 : 0);
        const maxHeight = Math.min(Math.max(above, below) - extra, this.maxListHeight > 0 ? this.maxListHeight : 571);
        const maxWidth = Math.min(window.innerWidth - 24, Math.max(this.listMaxWidth, box.width));

        this._list.style.setProperty('--ptcs-list-max-height', `${maxHeight}px`);

        _list.style.width = `${maxWidth}px`; // Start as wide as possible
        _list.style.visibility = 'hidden'; // prevent list from displaying before it's ready
        _list.style.display = ''; // remove 'display: none'
        _list.style.left = '0'; // Avoid scrollbar jump
        _list.style.top = '0';

        // Need to wait a few animation frames for the list to stabilize (100ms ~ 6 animation frames)
        await PTCS.wait(100);

        // Now we should have some items so we can compute the auto width
        if (!this.comboboxMode) {
            // We want autoWidth - unless it is greater than maxWidth or less than box.width
            this._list.style.width = `${Math.max(Math.min(_list.autoWidth, maxWidth), box.width)}px`;
        }

        this._list.style.visibility = ''; // show list in proper place

        const dim = this._get_dimension();
        this._set_list_position(dim); // set list position

        if (this.mode === 'open') {
            if (!this._close_ev) {
                // Close the dropdown if the user clicks anywhere outside of it
                this._close_ev = (ev) => {
                    const selectElement = this.$.select;
                    // Checks if each element in the path is not equal to the select-box element
                    if (ev.composedPath().every(el => el !== selectElement)) {
                        if (!this.comboboxMode && PTCS.hasFocus(this._list)) {
                            // If this is a "click outside of list", *don't* focus on the dropdown (TW-113003)
                            if (ev.type !== 'mousedown') {
                                this._focusOnDropdown();
                            }
                        }
                        this.mode = 'closed';
                    }
                };
                if (!PTCS.isAndroid && !PTCS.isTouchDevice) {
                    window.addEventListener('resize', this._close_ev);
                }
                document.addEventListener('mousedown', this._close_ev);
            }
            if (!this._keydown_ev) {
                // Close the dropdown if the user presses Tab, Enter or Escape
                this._keydown_ev = ev => {
                    switch (ev.key) {
                        case 'Enter':
                        case 'Escape':
                            ev.preventDefault();
                            this.mode = 'closed';
                            if (!this.comboboxMode) {
                                this._focusOnDropdown();
                            }
                            break;
                        case 'Tab': {
                            const root = this._list.shadowRoot;
                            const elem = ev.shiftKey
                                ? this._getFirstSubFocusable(root)
                                : root.querySelector('[part=list-items-container]');
                            if (root.activeElement === elem) {
                                this.mode = 'closed';
                                if (!this.comboboxMode) {
                                    this._focusOnDropdown();
                                }
                                ev.preventDefault();
                            }
                        }
                    }
                };

                this._list.addEventListener('keydown', this._keydown_ev);
            }

            // Close single selection lists if user makes a selection
            if (!this.multiSelect && !this._singleSelect_ev) {
                this._singleSelect_ev = () => {
                    this.mode = 'closed';
                    if (!this.comboboxMode) {
                        this._focusOnDropdown();
                    }
                };
                this._list.addEventListener('selected-changed', this._singleSelect_ev);
            }

            if (this.selected >= 0 && !this.comboboxMode) {
                this._list.scrollToIndex(this.selected);
            }

            // We should initially focus on the list items (the 'list' accessor on the list returns this
            // object), unless we are in "combobox" mode, in which case the focus should remain with the
            // text field
            if (!this.comboboxMode) {
                this._list.focus();
            }

            // Keep track of list position while open
            this.track_position(dim);
        }

        this._stayUnvalidated = true; // Open list in unvalidated state
    }

    _hideList() {
        if (this._close_ev) {
            document.removeEventListener('mousedown', this._close_ev);
            window.removeEventListener('resize', this._close_ev);
            this._close_ev = null;
        }

        if (this._keydown_ev) {
            this._list.removeEventListener('keydown', this._keydown_ev);
            this._keydown_ev = null;
        }

        if (this._singleSelect_ev) {
            this._list.removeEventListener('selected-changed', this._singleSelect_ev);
            this._singleSelect_ev = null;
        }

        this._list.style.display = 'none';
        if (!this.comboboxMode) {
            this._list.style.width = '';
        }
        this._list.style.top = '';
        this._list.style.left = '';
        this._list.freezeListHeight = false;

        // Reset filter string when closing popup
        this._list.filterString = '';
        if (this._list) {
            this._list.remove();
        }

        this._stayUnvalidated = false; // Validate when list is closed
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (this._list) {
            // Copy relevant changes to popup list
            changedProperties.forEach((value, key) => {
                const propName = sharedListProperties[key];
                if (propName) {
                    this._list[propName] = this[key];
                }
            });
        }
    }

    // _list usage
    _initList() {
        console.assert(!this._list);

        this._list = PTCS.createElement('ptcs-list', {part: 'list', 'is-dropdown': ''});

        this._list.addEventListener('_visible-items-changed', ev => {
            this._visibleItems = ev.detail.value;
        });

        this._list.addEventListener('click', ev => {
            if (!this.multiSelect && !PTCS.wrongMouseButton(ev) && !ev.defaultPrevented) {
                this.mode = 'closed';
                if (!this.comboboxMode) {
                    this._focusOnDropdown();
                }
            }
            ev.stopImmediatePropagation();
        });

        this._list.addEventListener('mousedown', ev => {
            ev.cancelBubble = true;
        });

        // Copy relevant properties
        for (const propName in sharedListProperties) {
            const value = this[propName];
            if (value !== undefined) {
                this._list[sharedListProperties[propName]] = value;
            }
        }

        // Dropdown and list needs to share the same selection manager
        this._list._selectionMgr.unbind(this._list);
        this._list._selectionMgr = this._selectionMgr;
        this._list._selectionMgr.bind(this._list);

        // Create popup list id
        this.setExternalComponentId();
    }

    _get_dimension() {
        return {
            dd:           this.getBoundingClientRect(),
            // Get window dimension
            windowWidth:  window.innerWidth,
            windowHeight: window.innerHeight,
            // Get current scroll offset
            scrollDx:     document.documentElement.scrollLeft + document.body.scrollLeft,
            scrollDy:     document.documentElement.scrollTop + document.body.scrollTop,
            // Where is the dropdown box?
            box:          this.$.select.getBoundingClientRect()
        };
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

    _set_list_position(r) {
        // It is smelly to access internal parts in ptcs-list, but ...
        const listBody = this._list.shadowRoot.querySelector('[part=list-items-container]');
        console.assert(listBody);
        const dw = (!this._list.style.width && PTCS.isFirefox) ? PTCS.getVerticalScrollbarWidth(listBody) : 0;
        const bbList = this._list.getBoundingClientRect();
        const smallModeAlignment = this.displayMode === 'small' ? 8 : 0;
        let x;
        if (r.windowWidth - r.box.left - bbList.width > 0) {
            x = r.box.left;
        } else if (r.windowWidth > r.box.right && r.box.right - smallModeAlignment - bbList.width > 0) {
            x = r.box.right - smallModeAlignment - bbList.width;
        } else if (r.windowWidth - bbList.width - dw - 24 > 0) {
            x = r.windowWidth - bbList.width - dw - 24;
        } else {
            x = 2;
        }
        let y = this._noSpaceForMessage ? r.dd.bottom : r.box.bottom + this.listMarginTop;
        if (y + bbList.height >= r.windowHeight) {
            // Show popup list above dropdown instead
            y = Math.max(r.box.top - this.listMarginTop - bbList.height, 2);
            if (this.comboboxMode) {
                // TW-96675 The popup list height should shrink to fit when displayed above the dropdown
                listBody.style.height = '';
            } else {
                // TW-87200 When the popup list is displayed above the dropdown value box and is filtered, the list should retain its height.
                this._list.freezeListHeight = true;
            }
        }

        // Set list position
        if (this.customListPosRect) {
            // Dropdown invoked with custom positioning (e.g. ptcs-tabs, ptcs-breadcrumb, ptcs-combobox), may need to be refined for more use cases
            if (this.customListPosRect.left > 0) {
                this._list.style.left = `${this.customListPosRect.left}px`;
            } else {
                this._list.style.left = `${r.scrollDx + this.customListPosRect.right - bbList.width}px`;
            }
            this._list.style.top = `${r.scrollDy + Math.min(this.customListPosRect.top + this.customListPosRect.height, y)}px`;
        } else {
            this._list.style.left = `${r.scrollDx + x}px`;
            this._list.style.top = `${r.scrollDy + y}px`;
        }

        // Freeze list width?
        if (!this._list.style.width) {
            // adding 0.1px, fixing minor delta calculation between list and v-scroller
            this._list.style.width = `${bbList.width + dw + 0.1}px`;
        }
    }

    _isHidden() {
        return !(this.offsetWidth || this.offsetHeight || this.getClientRects().length);
    }

    // Keep track of list position, if the droplist box is moved or the view is scrolled
    track_position(rOld) {
        if (this.mode === 'open') {
            if (this._isHidden()) {
                this.mode = 'closed';
            } else {
                const rNew = this._get_dimension();
                if (this._diff_dimension(rOld, rNew)) {
                    if (rNew.box.bottom < 0 || rNew.box.top > rNew.windowHeight) {
                        // The dropdown anchor has been scrolled out of sight. Close the popup
                        this.mode = 'closed';
                        return;
                    }
                    this._set_list_position(rNew);
                }

                // Take a fresh look at things in 0.2 seconds
                setTimeout(() => this.track_position(rNew), 200);
            }
        }
    }

    _onClick(ev) {
        if (ev.defaultPrevented || this.disabled || !this.items || this.items.length === 0) {
            return;
        }
        this.mode = (this.mode === 'open' ? 'closed' : 'open');
    }

    _monitorTooltip() { // Implements ptcs-dropdown's tooltip behavior on label truncation
        const el = this.shadowRoot.querySelector('[part~=selected-item-value]').querySelector('ptcs-label');
        if (el && el.isTruncated()) { // Truncated label to be used as tooltip?
            if (!this.selectedIndexes.length) {
                // Nothing is selected, label is the hinttext
                if (this.tooltip && this.tooltip !== this.hintText) {
                    return el.label + '\n\n' + this.tooltip;
                }
            } else if (this.tooltip && this.tooltip !== this.label) {
                return el.label + '\n\n' + this.tooltip;
            }
            return el.label;
        }
        // No truncation
        if (!this.selectedIndexes.length && this.tooltip && this.tooltip === this.hintText) {
            return ''; // No selection: Don't show tooltip if same as hint text
        }
        if (this.tooltip === this.label) {
            return ''; // Don't show tooltip if same as label
        }
        return this.tooltip || ''; // No label truncation, but possibly dropdown's own tooltip
    }

    _showTooltip(ev) {
        const tooltip = this.tooltipFunc;
        this._tooltipEnter(this, ev.clientX, ev.clientY, tooltip, {showAnyway: true});
    }

    getExternalComponentId() {
        return this._listId;
    }

    /*
     * Sets an id for external component
     */
    setExternalComponentId(id) {
        if (id) {
            this._listId = id;
        } else if (!this._listId) {
            this._listId = 'ptcs-dropdown-list-' + performance.now().toString().replace('.', '');
        }
        if (this._list) {
            this._list.setAttribute('id', this._listId);
        }
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

        if (change.hasOwnProperty('selectedIndexes')) {
            this._selectedIndexesLength = this.selectedIndexes && this.selectedIndexes.length ? this.selectedIndexes.length : 0;

            if (this.mode === 'open') {
                this._stayUnvalidated = this._selectedIndexesLength === 0; // Change to unvalidated state when no items are selected
            }

            // Update _depfield
            if (typeof this.createListItemAdditionalProperties === 'function' && this.shadowRoot) {
                this.$.select.__stateValueEl = this.$.item.__stateValueEl;
                this.createListItemAdditionalProperties(this.$.select, this._selectedIndexesLength === 1 && this.items[this.selectedIndexes[0]]);
            }
        }

        // Inform Lit about changes
        for (const propName in change) {
            this.requestUpdate(propName, this._oldSelection[propName]);
            this._oldSelection[propName] = change[propName];
        }
    }

    _itemsChanged(items) {
        this._selectionMgr.items = items;
    }

    get multiSelect() {
        return this._selectionMgr.multiSelect;
    }

    set multiSelect(_multiSelect) {
        if (_multiSelect === this._selectionMgr.multiSelect) {
            return;
        }
        this._selectionMgr.multiSelect = _multiSelect;
    }

    _stateKey(_selectedIndexesLength) {
        return _selectedIndexesLength ? 'selected' : undefined;
    }

    get selectedIndexes() {
        return this._selectionMgr.selectedIndexes;
    }

    set selectedIndexes(_selectedIndexes) {
        this._selectionMgr.selectedIndexes = _selectedIndexes;
    }

    get changesOnly() {
        return this._selectionMgr.changesOnly;
    }

    set changesOnly(_changesOnly) {
        this._selectionMgr.changesOnly = _changesOnly;
    }

    get selectedItems() {
        return this._selectionMgr.selectedItems;
    }

    set selectedItems(_selectedItems) {
        this._selectionMgr.selectedItems = _selectedItems;
    }

    get selected() {
        return this._selectionMgr.selected;
    }

    set selected(_selected) {
        this._selectionMgr.selected = _selected;
    }

    get autoSelectFirstRow() {
        return this._selectionMgr.autoSelectFirstRow;
    }

    set autoSelectFirstRow(_autoSelectFirstRow) {
        this._selectionMgr.autoSelectFirstRow = _autoSelectFirstRow;
    }

    _computeTabindex(tabindex) {
        return tabindex && typeof tabindex === 'string' && '0';
    }

    reFilter() {
        if (this._list) {
            this._list.reFilter();
        }
    }

    _insertValidationMessage(messageElement) {
        this.defaultInsertValidationMessageForVerticalLayout(messageElement);
    }

    static get $parts() {
        if (!this._$parts) {
            this._$parts = [
                'label', 'select-box', 'list-item', 'selected-item-value', 'icon',
                ...PTCS.partnames('icon-', PTCS.Icon)
                // ...PTCS.partnames('icon-', PTCS.Button) // Ignore this (for now). Keep parts list at a reasonable size
            ];
        }
        return this._$parts;
    }

    _validateDropdown(required, extraValidation, _selectedIndexesLength) {
        const messages = [];

        if (!required) {
            return undefined; // No internal validation enabled
        }

        if (required && _selectedIndexesLength === 0) {
            messages.push(this.requiredMessage);
        }

        if (messages.length) {
            return messages;
        }

        return typeof extraValidation === 'function' ? extraValidation(this) : true;
    }

    _validateSelectedValue(extraValidation) {
        return typeof extraValidation === 'function' ? extraValidation(this) : true;
    }

    _visibleItemsChanged(/* _visibleItems */) {
        if (this.mode === 'open' && this.comboboxMode) {
            // Debounce _showList calls
            if (!this.__callShowList) {
                this.__callShowList = true;
                requestAnimationFrame(() => {
                    this.__callShowList = false;
                    this._showList();
                });
            }
        }
    }
};

customElements.define(PTCS.Dropdown.is, PTCS.Dropdown);
