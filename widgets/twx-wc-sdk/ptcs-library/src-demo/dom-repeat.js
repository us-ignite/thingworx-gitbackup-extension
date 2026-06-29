import {compileTemplate, createNodes} from './templatize';

/* This element is only intended as a Polymer replacement in our demos */
const $assign = Symbol('Assign');

const key = _key => isNaN(_key) ? _key : Number(_key);
const acc = (ctx, _key) => ctx && ctx[key(_key)];


class Context_repeat {
    constructor(el) {
        console.assert(el.$templatizeContext);
        this._el = el;
    }

    addObserver(path, callback) {
        this._el.$templatizeContext.addObserver(path, callback);
    }

    get(path) {
        return this._el.$templatizeContext.get(path);
    }

    set(path, value) {
        if (path === 'items' || path.startsWith('items.')) {
            this._el.dispatchEvent(new CustomEvent('items-changed', {detail: {value, path}}));
        } else {
            console.warn('unknown assignment: ' + JSON.stringify(path), value);
        }
    }

    method(methodName) {
        return this._el.$templatizeContext.method(methodName);
    }
}


class Context_repeat_item {
    constructor(context, index) {
        this.context = context;
        this.index = index;
    }

    addObserver(path, callback) {
        if (path === 'item' || path.startsWith('item.')) {
            // Replace 'item' with 'items.<index>'
            // path = `items.${this.index}${path.substring(4)}`;

            // Currently there is no support for observing (only) sub-changes of array (or any other objects)
            // Hence, ignore observer into item properties
            return;
        }

        this.context.addObserver(path, callback);
    }

    get(path) {
        if (path === 'index') {
            return this.index;
        }
        const [propName, ...steps] = path.split('.');

        return propName === 'item' ? steps.reduce(acc, this.context._el._items[this.index]) : this.context.get(path);
    }

    set(path, value) {
        if (path === 'item' || path.startsWith('item.')) {
            // Replace 'item' with 'items.<index>'
            this.context.set(`items.${this.index}${path.substring(4)}`, value);
        } else {
            this.context.set(path, value);
        }
    }

    method(methodName) {
        return this.context.method(methodName);
    }
}


class DomRepeat extends HTMLElement {
    constructor() {
        super();
        this._ctors = []; // The compiled template
        this._items = []; // The input
        this._nodes = []; // Created bodes
        this._itemContexts = [];
    }

    connectedCallback() {
        if (this.__alreadyCreated) {
            return;
        }
        this.__alreadyCreated = true;

        this.style.display = 'none';

        this.context = new Context_repeat(this);

        this._compileTemplate();

        // For whatever reason I have to declare my getter and setter for "items" like this - or it doesn't work
        Object.defineProperty(this, 'items', {
            get: function() {
                return this._items;
            },

            set: function(items) {
                this._items = items;
                this._updateView();
            },

            configurable: true
        });
    }

    _compileTemplate() {
        // Find the template
        const template = this.firstElementChild;
        if (!template || template.tagName !== 'TEMPLATE' || template.nextElementSibling) {
            throw new Error('dom-repeat should contain a single template');
        }

        this._ctors = compileTemplate(template);
        this._ctors.whenDefined().then(() => this._updateView());
    }

    _updateView() {
        // Add missing nodes
        while (this._items.length > this._nodes.length) {
            const assign = [];
            const nodes = createNodes(this._ctors, assign);
            nodes[$assign] = assign;
            nodes.forEach(node => this.parentNode.insertBefore(node, this));
            this._nodes.push(nodes);
            this._itemContexts.push(new Context_repeat_item(this.context, this._itemContexts.length));
        }

        // Remove superfluous nodes
        while (this._nodes.length > this._items.length) {
            this._nodes.pop().forEach(el => el.remove());
            this._itemContexts.pop();
        }

        // Bind nodes to their items
        this._nodes.forEach((nodes, index) => nodes[$assign].forEach(f => f(this._itemContexts[index])));
    }

    /*
     * dom-repeat public methods
     */
    indexForElement(el) {
        return this._nodes.findIndex(nodes => nodes.some(node => node === el || (node.contains && node.contains(el))));
    }

    itemForElement(el) {
        return this._nodes[this.indexForElement(el)];
    }
}

customElements.define('dom-repeat', DomRepeat);
