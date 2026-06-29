// A grid value cache for delayed values
import {PTCS} from 'ptcs-library/library.js';

const NaV = Symbol('Not a value');
const ErrorValue = Symbol('Error');

const noDataManager = {baseLength: 0, baseItem: () => undefined};

export class ValueManager {
    constructor() {
        this._observers = new Set();
        this._dataManager = noDataManager;
        this._columnIds = [];
        this._values = new WeakMap();
        this._viewport = [];
    }

    // observe changes
    observe(cbObj) {
        this._observers.add(cbObj);
        return this;
    }

    // unobserve changes
    unobserve(cbObj) {
        this._observers.delete(cbObj);
        return this;
    }

    // Notify observers
    _msg(msg, ...arg) {
        this._observers.forEach(cbObj => typeof cbObj[msg] === 'function' && cbObj[msg](...arg));
    }

    // Not a Value token
    static get NaV() {
        return NaV;
    }

    // Error value token
    static get ErrorValue() {
        return ErrorValue;
    }

    // Number of items
    get size() {
        return this._dataManager.baseLength;
    }

    item(index) {
        return this._dataManager.baseItem(index);
    }

    isResolved(index, columnId) {
        const item = this.item(index);
        if (!item) {
            return false;
        }
        const values = this._values.get(item);
        const resolved = values
            ? colId => values.hasOwnProperty(colId) || item[colId] !== undefined
            : colId => item[colId] !== undefined;
        return columnId ? resolved(columnId) : this._columnIds.every(resolved);
    }

    _isUnresolved(index) {
        const item = this.item(index);
        if (!item) {
            return false;
        }
        const values = this._values.get(item);
        const resolved = values
            ? colId => values.hasOwnProperty(colId) || item[colId] !== undefined
            : colId => item[colId] !== undefined;
        return !this._columnIds.every(resolved);
    }

    unresolvedItems(max = Number.MAX_SAFE_INTEGER) {
        const size = this.size;
        const a = [];

        for (let index = 0; index < size && a.length < max; index++) {
            if (this._isUnresolved(index)) {
                a.push(index);
            }
        }

        return a;
    }

    get isActive() {
        return this._columnIds.length > 0;
    }

    get debounce() {
        return this._debounce;
    }

    set debounce(_debounce) {
        this._debounce = _debounce;
    }

    get viewport() {
        return this._viewport.filter(this._isUnresolved.bind(this));
    }

    set viewport(indexes) {
        const size = this.size;
        console.assert(Array.isArray(indexes) && indexes.every(i => typeof i === 'number' && 0 <= i && i < size));

        const subtract = (a, b) => {
            const r = [];
            let i = 0;
            let j = 0;
            while (i < a.length && j < b.length) {
                const c = a[i] - b[j];
                if (c < 0) {
                    r.push(a[i++]);
                } else if (c > 0) {
                    j++;
                } else {
                    i++;
                    j++;
                }
            }
            while (i < a.length) {
                r.push(a[i++]);
            }
            return r;
        };

        const f = () => {
            this._debounceTO = undefined;
            const viewport = [...new Set(indexes)].sort((a, b) => a - b);
            if (!PTCS.sameArray(this._viewport, viewport)) {
                const old = this._viewport;
                this._viewport = viewport;
                this._msg('gvViewport', [...this._viewport], subtract(viewport, old), subtract(old, viewport));
            }
        };

        if (this._debounce > 0) {
            if (this._debounceTO) {
                clearTimeout(this._debounceTO);
            }
            this._debounceTO = setTimeout(f, this._debounce);
        } else {
            f();
        }
    }

    setData(dataManager) {
        this._dataManager = dataManager || noDataManager;
        return this;
    }

    get columnIds() {
        return [...this._columnIds];
    }

    set columnIds(columnIds) {
        this._columnIds = Array.isArray(columnIds) ? columnIds : [];
    }

    value(key, columnId) {
        const item = (typeof key === 'number' ? this.item(key) : key);
        if (!item) {
            return NaV;
        }

        const values = this._values.get(item);
        if (values && values.hasOwnProperty(columnId)) {
            return values[columnId];
        }

        return item[columnId] !== undefined ? item[columnId] : NaV;
    }

    setValue(key, columnId, value) {
        if (this._columnIds.indexOf(columnId) === -1) {
            throw new Error('Invalid column id: ' + JSON.stringify(columnId));
        }
        const item = (typeof key === 'number' ? this.item(key) : key);
        if (!item) {
            throw new Error('Invalid index: ' + JSON.stringify(key));
        }
        let values = this._values.get(item);
        if (!values) {
            values = [];
            this._values.set(item, values);
        }

        if (values.hasOwnProperty(columnId) && values[columnId] === value) {
            return; // Same value. Ignore
        }

        values[columnId] = value;

        this._msg('gvValueChanged', key, columnId, value);
    }

    setErrorValue(key, columnId) {
        this.setValue(key, columnId, ErrorValue);
    }

    resetValue(key, columnId) {
        if (this._columnIds.indexOf(columnId) === -1) {
            throw new Error('Invalid column id: ' + JSON.stringify(columnId));
        }
        const item = (typeof key === 'number' ? this.item(key) : key);
        if (!item) {
            throw new Error('Invalid index: ' + JSON.stringify(key));
        }
        const values = this._values.get(item);
        if (!values || !values.hasOwnProperty(columnId)) {
            return;
        }

        delete values[columnId];

        this._msg('gvValueChanged', key, columnId);
    }

    resolveValue(key, columnId) {
        this.setValue(key, columnId, NaV);
    }
}

