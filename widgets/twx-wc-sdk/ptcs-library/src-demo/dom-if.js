/* This element is only intended as a Polymer replacement in our demos */
import {compileTemplate, createNodes} from './templatize';


class Context_if {
    constructor(el) {
        this._el = el;
    }

    addObserver(path, callback) {
        console.assert(path !== 'if');
        this._el.$templatizeContext.addObserver(path, callback);
    }

    get(path) {
        return path === 'if' ? this._el.if : this._el.$templatizeContext.get(path);
    }

    set(path, value) {
        console.assert(path !== 'if');
        this._el.$templatizeContext.set(path, value);
    }

    method(methodName) {
        return this._el.$templatizeContext.method(methodName);
    }
}


class DomIf extends HTMLElement {
    constructor() {
        super();
        this._ctors = []; // The compiled template
        this._nodes = []; // Created nodes
    }

    connectedCallback() {
        if (this.__alreadyCreated) {
            return;
        }
        this.__alreadyCreated = true;

        this.style.display = 'none';

        this.context = new Context_if(this);

        Object.defineProperty(this, 'if', {
            get: function() {
                return this._show || false;
            },

            set: function(_if) {
                if (!this._show !== !_if) {
                    this._show = !!_if;
                    this._updateView();
                }
            },

            configurable: true
        });

        this._compileTemplate();
    }

    _compileTemplate() {
        // Find the template
        const template = this.firstElementChild;
        if (!template || template.tagName !== 'TEMPLATE' || template.nextElementSibling) {
            throw new Error('dom-if should contain a single template');
        }

        this._ctors = compileTemplate(template);
        this._ctors.whenDefined(() => this._updateView());
    }

    _updateView() {
        if (this._show) {
            if (this._nodes.length === 0) {
                const assign = [];
                this._nodes = createNodes(this._ctors, assign);
                this._nodes.forEach(node => this.parentNode.insertBefore(node, this));
                assign.forEach(f => f(this.context));
            } else {
                this._nodes.forEach(node => this.parentNode.insertBefore(node, this));
            }
        } else if (this._nodes.length) {
            this._nodes.forEach(node => node.remove());
        }
    }
}

customElements.define('dom-if', DomIf);
