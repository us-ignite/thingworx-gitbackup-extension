/* This element is only intended as a Polymer replacement in our demos */
import {compileTemplate, compileExpr, createNodes, NaV} from './templatize';

const isName = name => name.match(/^[a-zA-Z_][a-zA-Z0-9_]*$/);
const isFunc = func => typeof func === 'function';
const key = _key => isNaN(_key) ? _key : Number(_key);
const acc = (ctx, _key) => (ctx && ctx !== NaV) ? ctx[key(_key)] : NaV;


class Context_root {
    constructor(el) {
        this._el = el;
        this._observers = {}; // {propName: [observer(context), ...]}
        this._values = {}; // {propName: value}
        this._debounce = {}; // {propName: true || undefined}
    }

    addObserver(path, callback) {
        // TODO: This could be improved if we observe every path, instead of only monitoring the full property
        const propName = path.split('.')[0];

        if (this._observers[propName]) {
            this._observers[propName].push(callback);
        } else {
            if (propName in this._el) {
                this._values[propName] = this._el[propName]; // Store current value
            }
            this._observers[propName] = [callback];

            Object.defineProperty(this._el, propName, {
                get: function() {
                    const value = this.context.get(propName);
                    return value !== NaV ? value : undefined;
                },

                set: function(value) {
                    this.context.set(propName, value);
                },

                configurable: true
            });
        }
    }

    notifyObservers(path) {
        // TODO: only notify the elements that are affected by the specicic path...
        const propName = path.split('.')[0];

        if (this._observers[propName]) {
            this._observers[propName].forEach(observer => observer());
        }
    }

    get(path) {
        const steps = path.split('.');
        return this._values.hasOwnProperty(steps[0]) ? steps.reduce(acc, this._values) : NaV;
    }

    set(path, value) {
        // At root level. Must assign the value - and let it propagate back down
        const [propName, ...steps] = path.split('.');

        if (steps.length === 0) {
            if (this._values[propName] === value) {
                return;
            }
            this._values[propName] = value;
            this.notifyObservers(propName);
        } else {
            // Assign value to internal part
            const internal = steps.slice(0, steps.length - 1).reduce(acc, this._values[propName]);
            const k = key(steps[steps.length - 1]);

            if (internal[k] === value) {
                return;
            }
            internal[k] = value;

            if (Array.isArray(this._values[propName])) {
                // Make sure every observer of propName is notified (change whole array when any internal part of it changes)
                // FIXME: This is not optimal, performance wise, but simple
                // For now: debounce assignment in order to aggregate multiple changes
                if (!this._debounce[propName]) {
                    this._debounce[propName] = true;
                    requestAnimationFrame(() => {
                        this._debounce[propName] = undefined;
                        this._el[propName] = [... this._values[propName]];
                    });
                }
            } else {
                this.notifyObservers(propName);
            }
        }
    }

    method(methodName) {
        const f = this._el[methodName];
        if (isFunc(f)) {
            return f;
        }

        console.warn(`Called unknown function: ${methodName}`);
        return () => undefined;
    }
}


class DomBind extends HTMLElement {
    constructor() {
        super();
        this.$ = {};
    }

    connectedCallback() {
        // Better safe than sorry...
        if (this.__alreadyCreated) {
            return;
        }
        this.__alreadyCreated = true;

        this.style.display = 'none';

        this.context = this.$templatizeContext || new Context_root(this);

        this._instantiateTemplate();
    }

    _instantiateTemplate() {
        // Find the template
        const template = this.firstElementChild;
        if (!template || template.tagName !== 'TEMPLATE' || template.nextElementSibling) {
            throw new Error('dom-bind should contain a single template');
        }

        // Compile template
        const ctors = compileTemplate(template);

        // Instantiate template (when all web components are ready)
        ctors.whenDefined().then(() => {
            const assign = [];
            const nodes = createNodes(ctors, assign);

            // Assign context to nodes
            assign.forEach(f => f(this.context));

            // Attach nodes to DOM
            nodes.forEach(node => {
                this.parentNode.insertBefore(node, this);

                if (node.getAttribute && node.getAttribute('id')) {
                    this.$[node.getAttribute('id')] = node;
                }
                if (node.querySelectorAll) {
                    node.querySelectorAll('*[id]').forEach(el => {
                        this.$[el.getAttribute('id')] = el;
                    });
                }
            });
        });
    }

    /*
     * dom-bind public methods
     */
    get(path) {
        return this.context.get(path);
    }

    set(path, value) {
        this.context.set(path, value);
    }

    push(path, ...arg) {
        const [propName, ...steps] = path.split('.');
        steps.reduce(acc, this[propName]).push(...arg);
        this.context.notifyObservers(propName);
    }

    splice(path, index, count, ...items) {
        const [propName, ...steps] = path.split('.');
        steps.reduce(acc, this[propName]).splice(index, count, ...items);
        this.context.notifyObservers(propName);
    }

    setProperties(properties) {
        for (const propName in properties) {
            console.assert(isName(propName), `Invalid property name: ${JSON.stringify(propName)}`);
            this[propName] = properties[propName];
        }
    }

    _createPropertyObserver(propName, callback) {
        const {context} = this;
        let old = this[propName];
        const f = () => {
            (isFunc(callback) ? callback : context.method(callback))(this[propName], old);
            old = this[propName];
        };

        context.addObserver(propName, f);
    }

    _createMethodObserver(call) {
        const {context} = this;
        const propNames = new Set();
        const f1 = compileExpr(call, propNames);
        const f2 = () => f1(context);
        propNames.forEach(propName => context.addObserver(propName, f2));
    }
}

customElements.define('dom-bind', DomBind);
