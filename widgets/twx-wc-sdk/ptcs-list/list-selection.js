import {PTCS} from 'ptcs-library/library.js';

/* eslint-disable no-nested-ternary */
const _selectionProps = ['_selectedIndexes', '_selectedItems', '_selectedValue', '_selected', '_multiSelect'];

export class ListSelection {
    /*
     * Observed and managed properties
     *
    static get properties() {
        return {
            _valueOf: Function // Extracts value from item

            // Items supplied by the client. Read-only
            items: {
                type:  Array,
                value: () => []
            },

            // Support multiple selections?
            multiSelect: {
                type:               Boolean,
                reflectToAttribute: true,
                observer:           '_multiSelectChanged'
            },

            // Indexes of selected items
            selectedIndexes: {
                type:   Array,
                notify: true,
                value:  () => []
            },

            selectedItems: {
                type:   Array,
                value:  () => [],
                notify: true
            },

            // Value of selected item, if single selection mode
            selectedValue: {
                type:               String,
                reflectToAttribute: true,
                notify:             true,
                observer:           '_selectedValueChanged'
            },

            // Index of selected object, if single selection mode
            selected: {
                type:     Number,
                notify:   true,
                observer: '_selectedChanged',
                value:    -1
            },

            autoSelectFirstRow: {
                type:     Boolean,
                value:    false,
                observer: '_autoSelectFirstRowChanged'
            }
        };
    }
    */

    constructor() {
        this._owners = new Set();
        this._valueOf = item => item;
        this._items = [];
        this._selectedIndexes = [];
        this._selectedItems = [];
        this._selectedValue = undefined;
        this._selected = -1;
    }

    bind(owner) {
        // Can only be bound to a single selection component
        console.assert(owner);

        this._owners.add(owner);

        // Inform owner about any non-default values
        const change = {};

        if (this._selectedValue !== undefined && this._selectedValue !== '') {
            change.selectedValue = this._selectedValue;
        }
        if (this._selected >= 0) {
            change.selected = this._selected;
        }
        if (this._selectedIndexes && this._selectedIndexes.length > 0) {
            change.selectedIndexes = this._selectedIndexes;
        }
        if (this._selectedItems && this._selectedItems.length > 0) {
            change.selectedItems = this._selectedItems;
        }
        if (this._multiSelect) {
            change.multiSelect = true;
        }

        if (Object.keys(change).length) {
            owner._updateSelection(change);
        }
    }

    unbind(owner) {
        this._owners.delete(owner);
        this._unselectAll();
    }

    _updateSelection(change, noChange) {
        this._owners.forEach(owner => owner._updateSelection(change, noChange));
    }

    // Manipulate selection data and block all selection callbacks
    _selectedWork(doWork, forceUpdate = false) {
        let update;
        if (!this.__protectSelectedWork) {
            const eq = (prop1, prop2) => PTCS.sameArray(prop1, prop2); // sameArray works with non-arrays too, if the values are identical
            this.__protectSelectedWork = true;

            try {
                const old = _selectionProps.map(name => this[name]);
                doWork();
                const change = {};
                update = false;
                _selectionProps.forEach((name, index) => {
                    if (!eq(old[index], this[name]) || forceUpdate) {
                        change[name.substring(1)] = this[name];
                        update = true;
                    }
                });

                if (update) {
                    this._updateSelection(change);
                }
            } catch (err) {
                console.error(err);
            } finally {
                this.__protectSelectedWork = undefined;
            }
        }
        return update; // undefined | true | false
    }

    // Client changes an "atomic" property
    _propChange(name, value, forceUpdate = false) {
        const old = this[name];
        if (!PTCS.sameArray(old, value)) { // Stop immediately, if no change is needed
            const updated = this._selectedWork(() => {
                this[name] = value;
                this[`${name}Changed`](value, old);
            }, forceUpdate);
            if (updated === false && !this._changesOnly) {
                this._updateSelection({[name.substring(1)]: this[name]}, true);
            }
        }
    }


    /*
     * Client property operations
    */

    get valueOf() {
        return this._valueOf;
    }

    set valueOf(_valueOf) {
        if (typeof _valueOf === 'function') {
            this._propChange('_valueOf', _valueOf);
        }
    }

    // If _changesOnly, only inform about chnages that affect the current mode
    // If !_changesOnly, also inform when assigned properties that are reset to the current value
    get changesOnly() {
        return this._changesOnly;
    }

    set changesOnly(_changesOnly) {
        this._changesOnly = _changesOnly;
    }

    get selectedValue() {
        return this._selectedValue;
    }

    set selectedValue(_selectedValue) {
        if ((_selectedValue === '' || _selectedValue === undefined) && (this._selectedValue === '' || this._selectedValue === undefined)) {
            // Bending over backwards for the IDE. The IDE localizes undefined values into '' and sends it back to the list / dropdown.
            // This code therefore interpret selectedValue === '' as undefined. This is a hack for backwards compatibility. We should
            // not have supported '' as an alias for undefiend, but we did and now we have to live with it...
            this._selectedValue = _selectedValue;
            return;
        }
        this._propChange('_selectedValue', _selectedValue);
    }

    get multiSelect() {
        return this._multiSelect;
    }

    set multiSelect(_multiSelect) {
        this._propChange('_multiSelect', _multiSelect);
    }

    get selected() {
        return this._selected;
    }

    set selected(_selected) {
        if (isNaN(_selected)) {
            if (!this._changesOnly) {
                this._updateSelection({selected: this.selected}, true);
            }
            return; // Invalid
        }
        this._propChange('_selected', _selected);
    }

    get autoSelectFirstRow() {
        return this._autoSelectFirstRow;
    }

    set autoSelectFirstRow(_autoSelectFirstRow) {
        this._propChange('_autoSelectFirstRow', _autoSelectFirstRow);
    }

    get selectedIndexes() {
        return this._selectedIndexes;
    }

    set selectedIndexes(_selectedIndexes) {
        // Someone somewhere (in ThingWorx) assigns selectedIndexes = [-1]. Must prevent it
        if (Array.isArray(_selectedIndexes)) {
            if (_selectedIndexes.some(i => i < 0)) {
                _selectedIndexes = _selectedIndexes.filter(i => i >= 0);
            }
        } else if (_selectedIndexes) {
            if (!this._changesOnly) {
                this._updateSelection({selectedIndexes: this._selectedIndexes}, true);
            }
            return; // Invalid
        }

        this._propChange('_selectedIndexes', _selectedIndexes || []);
    }

    get selectedItems() {
        return this._selectedItems;
    }

    set selectedItems(_selectedItems) {
        if (_selectedItems && !Array.isArray(_selectedItems)) {
            if (!this._changesOnly) {
                this._updateSelection({selectedItems: this._selectedItems}, true);
            }
            return; // Invalid
        }
        this._propChange('_selectedItems', _selectedItems || []);
    }

    set items(_items) {
        this._propChange('_items', Array.isArray(_items) ? _items : [], this._selectedItems && this._selectedItems.length > 0);
    }

    _xlateSelectedItemsToIndexes(selectedItems) {
        if (!Array.isArray(selectedItems)) {
            return []; // No selection
        }

        // Create a Set of the selected items / values
        const set = selectedItems.reduce((a, item) => {
            a.add(item);
            const v = this._valueOf(item);
            if (v && v !== item) {
                a.add(v);
            }
            return a;
        }, new Set());

        // Create a selectedIndexes array
        return this._items.reduce((a, item, index) => {
            if (set.has(item)) {
                a.push(index);
            } else if (set.has(this._valueOf(item))) {
                a.push(index);
            }
            return a;
        }, []);
    }

    _itemsChanged(_items) {
        if (!_items) {
            _items = [];
        }
        // A new list of items
        if (this._selectedItems.length && this._multiSelect && this._xferMS) {
            // Move old selection to new items
            const set = new Set(this._selectedItems);
            this._unselectAll();
            this._selectedIndexes = _items.reduce((a, item, index) => {
                if (set.has(item)) {
                    a.push(index);
                }
                return a;
            }, []);

            if (this._selectedIndexes.length === 0) {
                this._autoSelectFirstRowChanged(this._autoSelectFirstRow);
            } else {
                this._selectedItems = this._selectedIndexes.map(index => _items[index]);
            }
        } else if (this._selectedValue !== undefined && this._selectedValue !== '' && !this._multiSelect) {
            // if the new list has the same value select it again
            this._selected = _items.findIndex(item => this._valueOf(item) === this._selectedValue);
            if (this._selected >= 0 && this._selected < _items.length) {
                this._selectedIndexes = [this._selected];
                this._selectedItems = [_items[this._selected]];
            } else {
                this._unselectAll();
                this._autoSelectFirstRowChanged(this._autoSelectFirstRow);
            }
        } else {
            this._unselectAll();
            this._autoSelectFirstRowChanged(this._autoSelectFirstRow);
        }
    }

    _unselectAll() {
        this._selectedIndexes = [];
        this._selectedItems = [];
        this._selectedValue = undefined;
        this._selected = -1;
    }

    _valueOfChanged(_valueOf) {
        if (this._selected >= 0) {
            this._selectedValue = this._valueOf(this._items[this._selected]);
        }
    }

    _multiSelectChanged(_multiSelect) {
        // Transfer selected items between modes
        if (!_multiSelect && this._selectedIndexes.length > 1) {
            this._selected = this._selectedIndexes[0];
            this._selectedChanged(this._selected);
        }
    }

    _selectedIndexesChanged(_selectedIndexes) {
        if (this._multiSelect) {
            // Make sure indexes are sorted
            for (let i = 1; i < _selectedIndexes.length; i++) {
                if (_selectedIndexes[i - 1] >= _selectedIndexes[i]) {
                    // Make unique values and sort them
                    this._selectedIndexes = _selectedIndexes = [...new Set(_selectedIndexes)].sort((a, b) => a - b);
                    break;
                }
            }
            this._selectedItems = _selectedIndexes.map(index => this._items[index]).filter(item => item);
        } else if (_selectedIndexes.length > 0) {
            // Single selection mode. Only use selectedIndexes[0]
            this._selected = _selectedIndexes[0];
            this._selectedValue = this._valueOf(this._items[this._selected]);
            this._selectedItems = [this._items[this._selected]];
        } else {
            this._unselectAll();
        }
    }

    _selectedItemsChanged(_selectedItems) {
        if (!this._items || this._items.length === 0) {
            return;
        }

        if (PTCS.sameArray(_selectedItems, this._selectedIndexes, (item, index) => this._items[index] === item)) {
            return; // selecteItems follows selectedIndexes. No action is needed.
        }

        // Compute selectedIndexes according to selectedItems
        const si = this._xlateSelectedItemsToIndexes(_selectedItems);

        // Assign selection, according to selection mode
        if (this._multiSelect) {
            this._selectedIndexes = si;
        } else if (si && si.length > 0) {
            // Single selection mode. Only use selectedItems[0]
            const selected = si[0];
            this._selected = selected;
            this._selectedValue = this._valueOf(this._items[selected]);
            this._selectedIndexes = selected >= 0 ? [selected] : [];
        } else {
            this._unselectAll();
        }

        // Adjust selectedItems
        this._selectedItems = this._selectedIndexes.map(index => this._items[index]);
    }


    // Someone or something changed _selectedValue
    _selectedValueChanged(_selectedValue) {
        if (!this._items || this._items.length === 0) {
            return;
        }
        if (this._multiSelect) {
            this._selectedIndexes = this._items.reduce((acc, item, index) => {
                if (this._valueOf(item) === _selectedValue) {
                    acc.push(index);
                }
                return acc;
            }, []);
        } else {
            let selected = this._items.findIndex(item => this._valueOf(item) === _selectedValue);
            selected = selected === -1 && this._autoSelectFirstRow ? 0 : selected;
            this._selected = selected;
            this._selectedIndexes = selected >= 0 ? [selected] : [];
        }
        this._selectedItems = this._selectedIndexes.map(index => this._items[index]);
    }

    _selectedChanged() {
        if (!this._items.length === 0) {
            return;
        }
        if (this._selected >= 0 && this._items[this._selected]) {
            this._selectedValue = this._valueOf(this._items[this._selected]);
            this._selectedIndexes = [this._selected];
            this._selectedItems = [this._items[this._selected]];
        } else {
            this._selectedValue = undefined;
            this._selectedIndexes = [];
            this._selectedItems = [];
        }
    }

    _autoSelectFirstRowChanged(_autoSelectFirstRow) {
        if (!this._multiSelect && this._items && this._items.length) {
            const selectedIndexesLength = this._selectedIndexes ? this._selectedIndexes.length : 0;
            if (_autoSelectFirstRow && selectedIndexesLength === 0) {
                this._selected = 0;
                this._selectedValue = this._valueOf(this._items[this._selected]);
                this._selectedIndexes = [this._selected];
                this._selectedItems = [this._items[this._selected]];
            } else if (!_autoSelectFirstRow && selectedIndexesLength === 1) {
                // This is not correct but done anyway so the Thingworx IDE reflects the change
                this._unselectAll();
            }
        }
    }
}
