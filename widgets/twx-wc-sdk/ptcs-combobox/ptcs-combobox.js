import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import {closeTooltip} from 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import {delegateToPrev, delegateToNext} from 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-textfield/ptcs-textfield.js';
import 'ptcs-dropdown/ptcs-dropdown.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-list/ptcs-list.js';
import 'ptcs-icon/ptcs-icon.js';
import 'ptcs-icons/cds-icons.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-validate/ptcs-behavior-validate.js';

const _startsWith = (item, selectorF, text) => {
    if (item) {
        const itemText = selectorF(item);
        if (typeof itemText === 'string') {
            return itemText.toLowerCase().startsWith(text.toLowerCase());
        }
    }
    return false;
};

const _getItemSuggestion = (item, selectorF, length) => {
    if (item) {
        return selectorF(item).substr(length);
    }
    return '';
};

const _createFilterFunction = (selectorF) => {
    return (text) => {
        return (item) => _startsWith(item, selectorF, text);
    };
};

PTCS.ComboBox = class extends PTCS.BehaviorValidate(PTCS.BehaviorTooltip(PTCS.BehaviorFocus(
    PTCS.BehaviorStyleable(L2Pw(LitElement))))) {

    static get styles() {
        return css`
            :host {
                display: inline-flex;
                flex-direction: column;
                width: 100%;
                outline: none;
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

            [part=text-field] {
                width: 100%;
                min-width: 16px;
            }

            ptcs-textfield::part(text-box) {
                min-width: 16px;
            }

            [part=text-field][hidden] {
                display: none;
            }

            [part=select-box] {
                display: inline-flex;
                flex-grow: 1;
                box-sizing: border-box;
            }

            :host(:not([label=""]):not([hide-label])) [part="label"] {
                display: inline-flex;
                padding-bottom: 4px;
            }`;
    }

    render() {
        return html`
        <ptcs-label id="label" part="label" label=${this.label} ?hidden=${this._hideLabel(this.label, this.hideLabel)}
            variant=${this.labelVariant} multi-line horizontal-alignment=${this.labelAlignment} disable-tooltip></ptcs-label>
        <div id="select" part="select-box">
            <ptcs-textfield id="textfield" part="text-field" ?disabled=${this.disabled} disable-tooltip
                ?hide-clear-text=${!this.showClearButton} text=${this._textInternal} text-alignment=${this.alignment}
                hint-text=${this.hintText} ?hidden=${this.valueHide} tabindex=${this._delegatedFocus}
                disable-tooltip exportparts=${this._exportTextfield} @focus=${this._textfieldFocus}
                @blur=${this._textfieldBlur} @text-changed=${this._textfieldChanged}></ptcs-textfield>
            <ptcs-dropdown id="dropdown" part="drop-down" .selector=${this.selector} .items=${this.items}
                .selected=${this.selected} combobox-mode ?disabled=${this.disabled} alignment=${this.alignment}
                .autoSelectFirstRow=${this.autoSelectFirstRow} ?multi-line=${this.multiLine} row-height=${this.rowHeight}
                no-tabindex no-matches-label=${this.noMatchesLabel} value-hide .customListPosRect=${this._dropdownRect}
                @selected-value-changed=${this._dropdownValueChanged} @dropdown-closed=${this._dropdownClosed}
                @click=${this._dropdownClick} @selected-changed=${this._dropdownSelectedChanged}
                .grouping=${this.grouping} exportparts=${this._exportDropdown}></ptcs-dropdown>
        </div>`;
    }

    static get is() {
        return 'ptcs-combobox';
    }

    static get properties() {
        return {
            items: {
                type:        Array,
                observer:    '_itemsChanged',
                observeWhen: 'immediate',
                noAccessor:  true
            },

            selector: {
                type:        String,
                observer:    '_selectorChanged',
                observeWhen: 'immediate',
                noAccessor:  true
            },

            _selectorF: {
                type: Function
            },

            noMatchesLabel: {
                type:      String,
                attribute: 'no-matches-label'
            },

            showClearButton: {
                type: Boolean
            },

            // Activate the "typeAhead" functionality
            typeAhead: {
                type:      Boolean,
                attribute: 'type-ahead'
            },

            // Allow the user to enter values not present in the items array
            valueInput: {
                type:        Boolean,
                attribute:   'value-input',
                observer:    '_valueInputChanged',
                observeWhen: 'immediate',
                noAccessor:  true
            },

            // This is the text value that we *expose*, that we can observe and change
            // from the "outside"
            text: {
                type:        String,
                notify:      true,
                observer:    '_textChanged',
                observeWhen: 'immediate',
                noAccessor:  true
            },

            // In case the combobox gets the text *before* it gets the items, then store this value and
            // use it when the items has been propagated
            _pendingText: {
                type: String
            },

            // This is the current value from the textfield
            _textInternal: {
                type:        String,
                observer:    '_textInternalChanged',
                observeWhen: 'immediate',
                noAccessor:  true
            },

            // This is a RE string. If the _textInternal that the user is typing matches this, it should be set as the new text. This
            // is used from the date picker in the "year" combobox to detect that what is currently typed is a "correct" year...
            matchPattern: {
                type:      'String',
                attribute: 'match-pattern',
                observer:  '_matchPatternChanged'
            },

            hintText: {
                type:      String,
                attribute: 'hint-text',
                reflect:   true
            },

            label: {
                type:    String,
                reflect: true
            },

            labelVariant: {
                type:      String,
                attribute: 'label-variant'
            },

            hideLabel: {
                type:      Boolean,
                attribute: 'hide-label',
                reflect:   true
            },

            labelAlignment: { // 'left', 'center', 'right'
                type:      String,
                attribute: 'label-alignment'
            },

            alignment: { // 'left', 'center', 'right'
                type:    String,
                reflect: true
            },

            valueHide: {
                type:      Boolean,
                attribute: 'value-hide',
                reflect:   true
            },

            disabled: {
                type:    Boolean,
                reflect: true
            },

            selected: {
                type:        Number,
                notify:      true,
                observer:    '_selectedChanged',
                observeWhen: 'immediate',
                noAccessor:  true
            },

            autoSelectFirstRow: {
                type:      Boolean,
                attribute: 'auto-select-first-row'
            },

            multiLine: {
                type:      Boolean,
                attribute: 'multi-line'
            },

            rowHeight: {
                type:      String,
                attribute: 'row-height'
            },

            // If the input doesn't satisfy matchPattern on blur, revert to the value it had on initial focus (if that was a match)
            undoOnError: {
                type:      Boolean,
                attribute: 'undo-on-error'
            },

            // Revert to previous text when this flag is set and textfield blurs
            undoEditOnBlur: {
                type:      Boolean,
                attribute: 'undo-edit-on-blur'
            },

            // If this property is true, then pressing Escape will clear the text field, otherwise
            // it will "reset" the value to the previous one
            escapeKeyClearsContent: {
                type:      Boolean,
                attribute: 'escape-key-clears-content'
            },

            // We store the last string successfully added in valueInput mode in case we need to revert to it after
            // the user presses Escape...
            _lastNonItemValue: {
                type: String
            },

            // This is the ptcs-list popup of the dropdown
            _popup: {
                type: Object
            },

            listMarginTop: {
                type: Number
            },

            maxListHeight: {
                type: Number
            },

            // Specifies field name that contains group key or function that returns group key
            grouping: {
                type: String // or Function: (item) => String
            },

            _exportTextfield: {
                type:     String,
                readOnly: true,
                value:    PTCS.exportparts('textfield-', PTCS.Textfield)
            },

            _exportDropdown: {
                type:     String,
                readOnly: true,
                value:    PTCS.exportparts('dropdown-', PTCS.Dropdown)
            },

            _delegatedFocus: {
                type: String
            }
        };
    }

    constructor() {
        super();

        // Set the initial property values here instead of in the property declarations
        this.items = [];
        this.selected = -1;

        this.noMatchesLabel = 'No matches';
        this.showClearButton = false;
        this.hintText = 'Select...';

        this.label = '';
        this.labelVariant = 'label';
        this.listMarginTop = 8;
        this.rowHeight = '34';

        // Default list height according to visual design is 571px
        this.maxListHeight = 571;
        this._delegatedFocus = null;
    }

    ready() {
        super.ready();
        this.tooltipFunc = this._monitorTooltip.bind(this);
        this.addEventListener('keydown', this._keyDown.bind(this));

        this.addEventListener('focus', () => {
            // Make sure the popup is opened the first time it is needed
            this.__tabbingAway = false;
        });
    }

    firstUpdated() {
        super.firstUpdated();

        // Call the 'items' observer manually in case it was updated before the dropdown was created...
        this._itemsChanged(this.items);
    }

    // User clicked in dropdown
    _dropdownClick(ev) {
        // Do this from here before the dropdown itself has the chance to open it (to ensure that the popup is displayed at the right location)
        this._openPopup('');
        ev.preventDefault();
    }

    // New value selected in the dropdown
    _dropdownValueChanged(ev) {
        const selectedValue = ev.detail.value;
        if (selectedValue !== this._textInternal) {
            this.__textSetFromList = true;

            const match = selectedValue;

            // If we found match in the items list or the current input are not empty text
            if (match) {
                this._updateSelected(match);
            } else if (selectedValue) {
                this._textInternal = selectedValue;
            }
        }
    }

    // React to the closing of a popup
    _dropdownClosed(ev) {
        if (this.text !== this._textInternal && this.undoEditOnBlur) {
            this._restorePreviousSelection();
        }
        // Pass the focus back to the "main" component, UNLESS the popup is closed because we are
        // "Tab":ing away from it...
        if (this.__tabbingAway) {
            this.__tabbingAway = false;
        } else {
            this.$.textfield.focus();
        }
    }

    // The selection in the dropdown changed
    _dropdownSelectedChanged(ev) {
        this.selected = ev.detail.value;
    }

    // Focus event in textfield - store the initial value if it matches matchPattern and undoOnError is true
    _textfieldFocus(ev) {
        const text = ev.target.text;
        this._textOnFocus = this.undoOnError && this._matchPattern && text.match(this._matchPattern) ? text : undefined;
    }

    // Blur event in textfield
    _textfieldBlur(ev) {
        // During editing text and _textInternal can differ. Restore the previous
        // text if undoEditOnBlur is set.
        if (this.text !== this._textInternal && this.undoEditOnBlur && this._focusOutsideComboBox(ev.relatedTarget)) {
            this._restorePreviousSelection();
        } else if (this.undoOnError && (this.text === undefined || this._matchPattern && !this.text.match(this._matchPattern))) {
            // If the text on blur doesn't satisfy the matchPattern, restore the text from when textfield got focus
            this._restorePreviousSelection(true);
        }
    }

    // The text in the textfield changed
    _textfieldChanged(ev) {
        this._textInternal = ev.detail.value;
    }

    // Is the element that gets the focus from the textfield still within the
    _focusOutsideComboBox(target) {
        return target !== this && target !== this._popup;
    }

    _restorePreviousSelection(fromInitialFocus) {
        const previousText = this.selected === -1 ? (this._lastNonItemValue || '') : this._getItemText(this.items[this.selected]);
        const textToRestore = fromInitialFocus ? this._textOnFocus : previousText;
        this.__textSetFromList = true;
        this._textInternal = textToRestore;
        this._updateText(textToRestore);
        this.__textChangedFromWithin = true;
    }

    _keyDown(ev) {
        if (this.disabled || !this.items || this.items.length === 0) {
            return;
        }
        requestAnimationFrame(closeTooltip);
        switch (ev.key) {
            case 'Escape':
                if (this.$.dropdown.mode === 'open') {
                    this._closePopup();
                }
                if (this.escapeKeyClearsContent) {
                    // Clear the text on Esc
                    this.__textSetFromList = true;
                    this._textInternal = '';
                } else {
                    // Here 'Escape' should revert to any previous selection
                    this._restorePreviousSelection();
                }
                // Don't propagate to the parent
                ev.stopPropagation();
                break;
            case 'ArrowUp':
            case 'ArrowDown':
                if (this.$.dropdown.mode !== 'open') {
                    this._openPopup('');
                }

                // Focus on the list items (the vscroller)
                setTimeout(() => {
                    if (ev.key === 'ArrowUp') {
                        this._popup.resetFocusLast(true);
                    } else {
                        this._popup.resetFocus(true);
                    }
                }, 100);
                ev.preventDefault();
                break;
            case 'Tab':
                // Set a flag to prevent the dropdown-closed event handler from moving the focus
                // back to the main component
                this.__tabbingAway = true;
                this.__textSetFromList = true;
                this._setText();
                if (ev.shiftKey) {
                    // Prevent backwards Tab navigation from stopping on the main element
                    if (!delegateToPrev(this)) {
                        this.blur();
                    }
                    ev.preventDefault();
                } else {
                    // Prevent forward Tab navigation to stay in this element
                    if (!delegateToNext(this, true)) {
                        this.blur();
                    }
                    ev.preventDefault();
                }
                break;
            case 'Enter':
                this._setText();
                this.$.textfield.focus();
                this.$.textfield.selectAll();
                // Don't propagate to the parent
                ev.stopPropagation();
                break;
            default:
                this.__tabbingAway = false;
                this.__textSetFromList = false;
                break;
        }
    }

    // This event listener is called before the "standard one, so it can "tweak" the standfard dropdown/list behavior
    _keyDownDropDown(ev) {
        if (this.disabled || !this.items || this.items.length === 0) {
            return;
        }

        const tf = this.$.textfield;
        const len = tf.getFullText().length;

        let newPos;

        switch (ev.key) {
            case 'ArrowLeft':
                newPos = Math.max(tf.selectionStart - 1, 0);
                break;
            case 'ArrowRight':
                newPos = Math.min(tf.selectionEnd + 1, len);
                break;
            case 'Home':
                newPos = 0;
                break;
            case 'End':
                newPos = len;
                break;
        }

        // Did we process the event?
        if (newPos !== undefined) {
            // Disable the selectAll effect when the focus goes back to the text field
            tf.noAutoSelect = true;

            tf.focus();

            // Now set the selection according to the spec
            tf.setSelectionRange(newPos, newPos);

            // Wait a short while before clearing the noAutoSelect flag...
            setTimeout(() => {
                tf.noAutoSelect = undefined;
            }, 50);

            ev.preventDefault();
        }
    }

    _createSelector(selector) {
        // We could use PTCS.makeSelector for this, but that is supposedly about to be depricated...
        if (!selector) {
            return item => item;
        }
        if (typeof selector === 'string') {
            return item => item[selector];
        }
        if (selector.constructor && selector.call && selector.apply) {
            return selector;
        }
        console.error('Invalid selector: ', selector);
        return () => 'invalid-selector';
    }

    _selectorChanged(selector) {
        // Create a new selector function...
        this._selectorF = this._createSelector(selector);

        // ...and apply it to the current selection
        this._selectedChanged(this.selected);
    }

    _getSelectorF() {
        // Make sure the _selectorF is created even if no selector is set
        if (!this._selectorF) {
            this._selectorF = this._createSelector(this.selector);
        }
        return this._selectorF;
    }

    // Return the first item that matches the search text
    _checkMatch(text) {
        return this.items.find(item => _startsWith(item, this._getSelectorF(), text));
    }

    // Return the complete value of the matched item
    _getItemMatchedText(text) {
        const match = this._checkMatch(text);
        return match ? this._getSelectorF()(match) : '';
    }

    // Return the missing part of the suggested value
    _getSuggestion(text) {
        const match = this._checkMatch(text);
        if (match) {
            return _getItemSuggestion(match, this._getSelectorF(), text.length);
        }

        // No match
        return '';
    }

    // This does the stuff needed for the "custom" location of the popup window and opens it
    _openPopup(filterString) {
        if (this.disabled) {
            return;
        }
        // _textInternalChanged can invoke _openPopup while TAB:ing away
        if (this.__tabbingAway) {
            this.__tabbingAway = false;
            return;
        }
        const dropdown = this.$.dropdown;

        // Time to display the popup---make sure it is created
        if (!this._popup) {
            this._popup = dropdown.createPopupList();

            // We need to "tweak" the default dropdown behavior a bit (adding e.g. wraparound selection and
            // handling ArrowLeft/ArrowRight/Home/End differently)
            this._popup.wrapFocus = true;
            // TODO: This is a very ugly hack that is both harmful and risky. The combobox should not even be aware of that
            //       the dropdown has a virtual scroller. This prevents the dropdown from being refactored in the future
            //       (which is precisly why I need to do this change, while refactoring from Polymer to Lit)
            this._popup.updateComplete.then(() => {
                this._popup.$.chunker.addEventListener('keydown', this._keyDownDropDown.bind(this), true);
            });
        }

        // Get current position of the dropdown button, for custom positioning of the dropdown list (the
        // getBoundingClientRect() call returns a read-only object, hence the copying)
        const popupRect = JSON.parse(JSON.stringify(this.getBoundingClientRect()));

        popupRect.left = 0;
        popupRect.top += this.listMarginTop;

        // Make the popup menu the same width as the combobox itself
        const w = `${popupRect.width}px`;
        dropdown.listMaxWidth = popupRect.width;
        this._popup.style.width = w;
        this._popup.style.maxWidth = w;

        this._dropdownRect = popupRect;

        // The popup should now open
        dropdown.mode = 'open';

        this._popup.filterString = filterString;
        this._popup.filter = _createFilterFunction(this._getSelectorF());
        this._popup.hideFilter = true;

        // Set focus on textfield
        this.$.textfield.focus();
    }

    _closePopup() {
        const dropdown = this.shadowRoot && this.shadowRoot.getElementById('dropdown');
        if (dropdown && dropdown.mode !== 'closed') {
            dropdown.mode = 'closed';
        }
    }

    _setText() {
        // This is called when the user presses 'Enter' or 'Tab' in the text field. It
        // should look at the text actually in the textfield and set the text property
        // accordingly. If there is a current 'suggestion' active, then this is used,
        // otherwise we use the text as-is (since we must be able to add data that is
        // a subset of the items without finding an item where this is a prefix)
        this._updateSelected(this.$.textfield.getFullText());
    }

    _valueInputChanged(valueInput) {
        // If we are disabling the valueInput while we have an "active", non-items text, then clear
        // the text value
        if (valueInput === false && this.selected === -1) {
            this.__textSetFromList = true;
            this._textInternal = '';
            this._updateText('');
        }
    }

    _itemsChanged(items) {
        // Make 100% sure that the dropdown items are updated *immediately* and not some time in the distant future
        const dropdown = this.shadowRoot && this.shadowRoot.getElementById('dropdown');
        if (dropdown) {
            dropdown.items = items;
            dropdown.performUpdate();
        }

        if (items.length > 0) {
            // Items is now set---check if there is a _pendingText somewhere that was set before the items
            // were set...
            if (this._pendingText) {
                // Prevent it from opening a popup
                this.__textSetFromList = true;

                this._updateSelected(this._pendingText);
                this._pendingText = null;
            }
        }
    }

    _getItemText(item) {
        const itemText = this._getSelectorF()(item);
        return typeof itemText === 'string' ? itemText : '';
    }

    _updateSelected(fullText) {
        // Look at the data, is the data in fulltext present in the items?
        // This will set the selection from the outside
        const selectedIndex = this.items.findIndex(item => this._getItemText(item).toLowerCase() === fullText.toLowerCase());

        if (selectedIndex !== -1) {
            // Exact hit found, use this as the "selected item" (this will update the _textInternal as well)
            if (this.selected !== selectedIndex) {
                this.selected = selectedIndex;
            } else {
                // Make sure the text is updated even when the selectedIndex hasn't changed
                this._closePopup();
                // The Lit version of the textfield component might not be ready yet
                this.$.textfield.performUpdate();
                this.$.textfield.selectAll();
            }
            this._updateText(this._getItemText(this.items[selectedIndex]));
        } else {
            // The current _textInternal is *not* part of the items array
            if (this.valueInput || fullText === '') {
                // With valueInput set, we are *allowed* to enter values not present in the array
                this._textInternal = fullText;
                // Store the value in case we need to revert to it after Escape was pressed
                this._lastNonItemValue = fullText;
                this.__textSetFromList = true;
                this._updateText(fullText);
                this.selected = -1;
            } else {
                // Here we are *not* allowed to add new values---so revert to the previous selection
                const previousText = this.selected === -1 ? '' : this._getItemText(this.items[this.selected]);
                this._textInternal = previousText;
                this._updateText(previousText);
                this.__textChangedFromWithin = true;
            }
            this._closePopup();
        }
    }

    _textChanged(result) {
        const textfield = this.shadowRoot && this.shadowRoot.getElementById('textfield');

        if (textfield) {
            textfield.performUpdate();
        }

        if (!this.items || this.items.length === 0) {
            // The text was set before the items were set, remember it
            this._pendingText = result;
            return;
        }

        if (this.__textChangedFromWithin) {
            this.__textChangedFromWithin = false;
        } else {
            // Prevent it from opening a popup
            this.__textSetFromList = true;
            this._updateSelected(result);
        }
    }

    _updateText(text) {
        if (this.text !== text) {
            this.__textChangedFromWithin = true;
            this.text = text;
        }
    }

    _matchPatternChanged(pattern) {
        this._matchPattern = new RegExp(pattern);
    }

    // Does the text match one of the items?
    _textMatchesItem(text) {
        const foundIdx = this.items.findIndex(item => this._getItemText(item).toLowerCase() === text.toLowerCase());
        if (foundIdx !== -1) {
            this.selected = foundIdx;
            this._closePopup();
            return true;
        }
        return false;
    }

    _textMatchesPattern(text) {
        if (this.valueInput && this._matchPattern && text.match(this._matchPattern)) {
            this.text = text;
            this._closePopup();
            return true;
        }
        return false;
    }

    _textMatches(text) {
        return this._textMatchesItem(text) || this._textMatchesPattern(text);
    }

    _textInternalChanged(text) {
        // Prevent that we open the popup on create
        if (!this._popup && !text) {
            return;
        }

        // If the change to _textInternal came from e.g. a selection in the list, then don't re-open the list...
        if (this.__textSetFromList) {
            this.__textSetFromList = false;
            return;
        }

        if (this.typeAhead) {
            const suggestion = this._getSuggestion(text);

            if (suggestion) {
                const textfieldEl = this.$.textfield;
                if (textfieldEl) {
                    textfieldEl.appendSuggestion(suggestion);
                }
            }
        }

        // Now---does the currently entered value match one of the items OR the "match" RE? If so, we consider it to be
        // the eqivalent of selecting it from the list...so you don't loose it if you don't press Enter or Tab
        if (this._textMatches(text)) {
            return;
        }

        // Make sure the popup is opened, with the current text value as the dropdown filterString
        this._openPopup(text);
    }

    _selectedChanged(selected) {
        if (!this.items) {
            return;
        }

        // This call might come before the ID lookup has been built
        const dropdown = this.shadowRoot && this.shadowRoot.getElementById('dropdown');

        if (typeof selected === 'string') {
            selected = parseInt(selected);
        }

        // Only allow index values "within" the boundaries of the items array
        if (!isNaN(selected) && selected >= 0 && selected < this.items.length) {
            const value = this._getItemText(this.items[selected]);
            if (dropdown) {
                dropdown.selected = selected;
            }
            this.__textSetFromList = true;
            this._textInternal = value;
            this._updateText(value);
        }
    }

    _hideLabel(label, hideLabel) {
        return !label || hideLabel;
    }

    _monitorTooltip() {
        const el = this.$.textfield;
        const hint = el.shadowRoot.querySelector('[part=hint-text]');
        // Don't show tooltip when same label
        const tooltip = this.tooltip !== this.label ? this.tooltip : '';
        if (el.hasText && el.isTruncated()) {
            // Combobox textfield text is truncated, use it as tooltip
            if (tooltip && tooltip !== el.text) {
                // Truncated text and tooltip differ, show both
                return el.text + '\n\n' + tooltip;
            }
            // Show truncated text only
            return el.text;
        }
        // Truncated hint text?
        if (!el.hasText && this.hintText && ((el.offsetWidth < hint.scrollWidth) || hint.scrollWidth === 0)) {
            if (tooltip && tooltip !== this.hintText) {
                return this.hintText + '\n\n' + tooltip;
            }
            return this.hintText;
        }
        // No truncation. Show tooltip if different from the currently shown text
        if (tooltip !== el.text) {
            return tooltip || '';
        }
        return '';
    }

    static get $parts() {
        if (!this._$parts) {
            this._$parts = [...PTCS.partnames('dropdown-', PTCS.Dropdown), ...PTCS.partnames('textfield-', PTCS.Textfield)];
        }
        return this._$parts;
    }
};

customElements.define(PTCS.ComboBox.is, PTCS.ComboBox);
