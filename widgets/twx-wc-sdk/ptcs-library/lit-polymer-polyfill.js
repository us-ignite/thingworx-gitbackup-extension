import {LitElement} from 'lit';
import {PropertyObserver} from './property-observer.js';

const empty = new Set();

//
// Polyfill Polymer functions to LitElement
//
console.assert(LitElement.prototype.setProperties === undefined);
LitElement.prototype.setProperties = function(properties) {
    for (const name in properties) {
        this[name] = properties[name];
    }
};

console.assert(LitElement.prototype.notifyPath === undefined);
LitElement.prototype.notifyPath = function(path) {
    this.requestUpdate(path.split('.')[0]);
};

console.assert(LitElement.prototype._createPropertyObserver === undefined);
LitElement.prototype._createPropertyObserver = function(propName, callback) {
    const cb = () => {
        if (typeof callback === 'function') {
            return callback;
        }
        if (typeof this[callback] === 'function') {
            return this[callback];
        }

        // Late binding (callback function not available yet)
        return (...arg) => this[callback](...arg);
    };

    const {readOnly} = this._lit2polymerTables ? this._lit2polymerTables() : {readOnly: empty};

    new PropertyObserver(this, readOnly.has(propName) ? `__${propName}$` : propName, cb());
};

const key = _key => isNaN(_key) ? _key : Number(_key);
const acc = (ctx, _key) => ctx[key(_key)];

console.assert(LitElement.prototype._$keyedValue === undefined);
LitElement.prototype._$keyedValue = function(keys) {
    const _keys = typeof keys === 'string' ? keys.split('.') : keys;
    return _keys.reduce(acc, this);
};

console.assert(LitElement.prototype.get === undefined);
LitElement.prototype.get = function(path) {
    return this._$keyedValue(path);
};

console.assert(LitElement.prototype.set === undefined);
LitElement.prototype.set = function(path, value) {
    const keys = path.split('.');
    const propName = keys[0];
    const last = keys.pop();
    this._$keyedValue(keys)[key(last)] = value;
    this.requestUpdate(propName);
};

console.assert(LitElement.prototype.push === undefined);
LitElement.prototype.push = function(path, ...items) {
    const array = [...this._$keyedValue(path)];
    const len = array.push(...items);
    this.set(path, array);
    return len;
};

console.assert(LitElement.prototype.pop === undefined);
LitElement.prototype.pop = function(path, value) {
    const array = [...this._$keyedValue(path)];
    const item = array.pop(value);
    this.set(path, array);
    return item;
};

console.assert(LitElement.prototype.unshift === undefined);
LitElement.prototype.unshift = function(path, ...items) {
    const array = [...this._$keyedValue(path)];
    const len = array.unshift(...items);
    this.set(path, array);
    return len;
};

console.assert(LitElement.prototype.shift === undefined);
LitElement.prototype.shift = function(path) {
    const array = [...this._$keyedValue(path)];
    const item = array.shift();
    this.set(path, array);
    return item;
};

console.assert(LitElement.prototype.splice === undefined);
LitElement.prototype.splice = function(path, index, removeCount, ...items) {
    const array = [...this._$keyedValue(path)];
    const removedItems = array.splice(index, removeCount, ...items);
    this.set(path, array);
    return removedItems;
};
