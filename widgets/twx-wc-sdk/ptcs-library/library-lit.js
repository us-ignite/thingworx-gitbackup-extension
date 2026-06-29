import './lit-polymer-polyfill.js';
import {PropertyObserver} from './property-observer.js';


const polymerField = Symbol('polymer');
const isInitingField = Symbol('isIniting');
const isPendingField = Symbol('isPendingProps');

function hasChanged(value, old) {
    return value !== old;
}

function addImmediateObserver(el, propName, observers) {
    const _propName = `__${propName}$`;

    Object.defineProperty(el, propName, {
        get: function() {
            return this[_propName];
        },

        set: function(value) {
            const old = this[_propName];
            const x = observers.find(item => item.hasChanged);
            if ((x ? x.hasChanged : hasChanged)(value, old)) {
                this[_propName] = value;
                observers.forEach(({observer}) => this[observer](value, old));
                this.requestUpdate(propName, old);
            }
        },
        configurable: true // Allow ProperyObserver to watch property
    });
}

function capitalizeFirstLetter(string) {
    return string.charAt(0).toUpperCase() + string.slice(1);
}

function addReadOnlyProperty(el, propName, observers) {
    const _propName = `__${propName}$`;

    Object.defineProperty(el, propName, {
        get: function() {
            return this[_propName];
        },
        configurable: true // Allow ProperyObserver to watch property
    });

    function setWithNoObservers(value) {
        const old = this[_propName];
        if (old !== value) {
            this[_propName] = value;
            this.requestUpdate(propName, old);
        }
    }

    function setWithObservers(value) {
        const old = this[_propName];
        if (old !== value) {
            this[_propName] = value;
            observers.forEach(({observer}) => this[observer](value, old));
            this.requestUpdate(propName, old);
        }
    }

    el[`_set${capitalizeFirstLetter(propName)}`] = observers ? setWithObservers : setWithNoObservers;
}


// Lit 2 Polymer Wrapper (for porting Polymer components)
export const L2Pw = superClass => class extends superClass {

    constructor() {
        super();

        // Implement $[id] to address elements in the shadow dom
        this.$ = new Proxy(this, {
            get(target, property) {
                const el = target.shadowRoot.getElementById(property);
                console.assert(el, `Cannot find $.${property}`);
                return el;
            }
        });

        const {immediateObserver, readOnly, extProp} = this._lit2polymerTables();

        for (const propName in immediateObserver) {
            if (!readOnly.has(propName)) {
                addImmediateObserver(this, propName, immediateObserver[propName]);
            }
        }

        for (const propName of readOnly) {
            addReadOnlyProperty(this, propName, immediateObserver[propName]);
        }

        // Observe non-lit properties
        if (extProp) {
            const cb = (value, old, propName) => {
                if (!this[isPendingField]) {
                    this[isPendingField] = new Map(); // Start update cycle
                    requestAnimationFrame(() => {
                        // Simulate lit life cycle calls
                        const changedProperties = this[isPendingField];
                        this.willUpdate(changedProperties);
                        this[isPendingField] = undefined; // Close update cycle
                        this.updated(changedProperties);
                    });
                }

                // Store old value
                if (!this[isPendingField].has(propName)) {
                    this[isPendingField].set(propName, old);
                }
            };

            extProp.forEach(propName => new PropertyObserver(this, propName, cb));
        }

        this[isInitingField] = true;
    }

    static createProperty(name, options) {
        if (options.readOnly || options.observeWhen === 'immediate') {
            // Set to true to avoid generating the default property accessor.
            options.noAccessor = true;
        }
        super.createProperty(name, options);
    }

    decodeObserverCall(call) {
        const m = /^([$\w]+)\((.+)\)$/.exec(call);
        if (!m) {
            throw new Error(`Invalid observer expression: ${call}`);
        }

        const args = m[2].split(',').map(s => s.trim());

        if (!args.every(s => /^(\$|[$\w]+)$/.test(s))) {
            throw new Error(`Invalid observer expression arguments: ${call}`);
        }

        const method = m[1];
        if (typeof this[method] !== 'function') {
            throw new Error(`Invalid observer: : ${call}`);
        }

        const f = function() {
            // NOTE: This is development code, for debugging change-in-update conditions
            // const isUpdatePending = this.isUpdatePending;
            this[method](...args.map(propName => this[propName]));
            // if (isUpdatePending !== this.isUpdatePending) {
            // eslint-disable-next-line max-len
            //    console.log(`%cchange-in-update: ${this.tagName.toLowerCase()}.${method}(${args.map(propName => JSON.stringify(this[propName])).join(', ')})`, 'color: red; font-weight: bold');
            // }
        };

        return {args, f};
    }


    // Decode property value, observer, computed, and observers arrays
    _lit2polymerTables() {
        if (this.constructor[polymerField]) {
            return this.constructor[polymerField];
        }

        const decl = {values: [], observer: {}, immediateObserver: {}, computers: {}, observers: {}, notify: new Set(), readOnly: new Set()};

        this.constructor[polymerField] = decl;

        const isComputed = new Set();

        const next = el => {
            for (let pof = Object.getPrototypeOf(el); pof; pof = Object.getPrototypeOf(pof)) {
                if (pof.constructor !== el.constructor) {
                    return pof;
                }
            }
            return null;
        };

        // Collect all registered properties
        const allProp = new Set();
        for (let el = this; el; el = next(el)) {
            const {properties} = el.constructor;
            for (const key in properties) {
                allProp.add(key);
            }
        }

        // Add observed key
        function addKey(obj, key, func) {
            if (!obj[key]) {
                obj[key] = [func];
            } else {
                obj[key].push(func);
            }

            if (!allProp.has(key)) {
                // key is not a lit property: need to observe it manually
                if (decl.extProp) {
                    decl.extProp.add(key);
                } else {
                    decl.extProp = new Set([key]);
                }
            }
        }

        // Find all validated properties with validation function and arguments
        for (let el = this; el; el = next(el)) {
            const {properties, observers} = el.constructor;

            // Property declarations
            if (properties) {
                for (const propName in properties) {
                    const {value, observer, observeWhen, computed, notify, readOnly} = properties[propName];

                    if (value !== undefined && decl.values.findIndex(item => item[0] === propName) === -1) {
                        decl.values.push([propName, value]);
                    }

                    if (observer) {
                        if (typeof this[observer] !== 'function') {
                            throw new Error(`Invalid observer: ${observer}`);
                        }

                        if (observeWhen === 'immediate') {
                            addKey(decl.immediateObserver, propName, {observer, hasChanged: properties[propName].hasChanged});
                        } else {
                            addKey(decl.observer, propName, observer);
                        }
                    }

                    if (computed && !isComputed.has(propName)) {
                        isComputed.add(propName); // Ony use highest level computed field

                        const m = /(\w+)\((.+)\)/.exec(computed);
                        if (!m) {
                            throw new Error(`Invalid computed expression: ${computed}`);
                        }

                        const args = m[2].split(',').map(s => s.trim());
                        if (!args.every(s => /^(\$|\w+\$?)$/.test(s))) {
                            throw new Error(`Invalid computed expression arguments: ${computed}`);
                        }

                        const method = m[1];
                        if (typeof this[method] !== 'function') {
                            throw new Error(`Invalid computed method: ${computed}`);
                        }

                        const f = function() {
                            this[propName] = this[method](...args.map(argName => this[argName]));
                        };

                        args.forEach(argName => addKey(decl.computers, argName, f));
                    }

                    if (notify) {
                        decl.notify.add(propName);
                    }

                    if (readOnly) {
                        decl.readOnly.add(propName);
                    }
                }
            }

            // observers property - for multi propery observers
            if (observers) {
                observers.forEach(call => {
                    const {args, f} = this.decodeObserverCall(call);
                    args.forEach(propName => addKey(decl.observers, propName, f));
                });
            }
        }

        return decl;
    }

    ready() {
        // Do nothing
    }

    connectedCallback() {
        super.connectedCallback();

        if (this[isInitingField]) {
            const {values, readOnly} = this._lit2polymerTables();

            const apply = f => typeof f === 'function' ? f.apply(this) : f;

            const preventChangeEvent = name => {
                if (!this._preventChangeEvent) {
                    this._preventChangeEvent = new Set(); // Prevents a change event dispatching for props with default values
                }
                this._preventChangeEvent.add(name);
            };

            // Apply default values
            values.forEach(([name, value]) => {
                const currValue = this[name];
                if (currValue === undefined) {
                    if (readOnly.has(name)) {
                        this[`_set${capitalizeFirstLetter(name)}`](apply(value));
                    } else {
                        this[name] = apply(value);
                    }
                    preventChangeEvent(name);
                } else if (value === currValue) {
                    // Don't generate change event if the initial value is the default value
                    preventChangeEvent(name);
                }
            });

            this[isInitingField] = undefined;
        }
    }

    firstUpdated() {
        super.firstUpdated();

        this.ready();
    }

    // Lit pre update: compute dependent values
    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        const {computers} = this._lit2polymerTables();

        const call = new Set();

        changedProperties.forEach((oldValue, propName) => {
            if (computers.hasOwnProperty(propName)) {
                computers[propName].forEach(f => call.add(f));
            }
        });

        call.forEach(f => f.call(this));
    }

    // Lit post update: call observers and generate change callbacks
    updated(changedProperties) {
        super.updated(changedProperties);

        const {notify, observer, observers} = this.constructor[polymerField];
        const observers2 = this.__$observers;

        const call = new Set();

        changedProperties.forEach((oldValue, propName) => {
            if (oldValue === this[propName]) {
                return; // Ignore change, since it has already changed back
            }
            if (notify.has(propName) && !(this._preventChangeEvent && this._preventChangeEvent.has(propName))) {
                this.dispatchEvent(new CustomEvent(`${window.camelToDashCase(propName)}-changed`, {detail: {value: this[propName]}}));
            }
            if (observer.hasOwnProperty(propName)) {
                observer[propName].forEach(funcName => {
                    // NOTE: This is development code, for debugging change-in-update conditions
                    // const isUpdatePending = this.isUpdatePending;
                    this[funcName](this[propName], oldValue);
                    // if (isUpdatePending !== this.isUpdatePending) {
                    // eslint-disable-next-line max-len
                    //    console.log(`%cchange-in-update: ${this.tagName.toLowerCase()}.${funcName}(${JSON.stringify(this[propName])}, ${JSON.stringify(oldValue)})`, 'color: red; font-weight: bold');
                    // }
                });
            }
            if (observers.hasOwnProperty(propName)) {
                observers[propName].forEach(f => call.add(f));
            }
            // Added with _createMethodObserver
            if (observers2 && observers2.hasOwnProperty(propName)) {
                observers2[propName].forEach(f => call.add(f));
            }
        });

        this._preventChangeEvent = undefined;
        call.forEach(f => f.call(this));
    }

    _createMethodObserver(call) {
        const {args, f} = this.decodeObserverCall(call);

        if (!this.__$observers) {
            this.__$observers = {};
        }

        const addProp = propName => {
            if (!this.__$observers[propName]) {
                this.__$observers[propName] = [f];
            } else {
                this.__$observers[propName].push(f);
            }
        };

        args.forEach(addProp);
    }
};
