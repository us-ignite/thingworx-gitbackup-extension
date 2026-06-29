import {LitElement} from 'lit';
import {PTCS} from 'ptcs-library/library.js';
import {StyleAggregator} from './style-aggregator.js';

PTCS.StyleContext = class extends LitElement {
    static get is() {
        return 'ptcs-style-context';
    }

    static get properties() {
        return {
            styleAggregator: {
                type:       Object,
                noAccessor: true // We only supply a getter - no setter, to make this property read only.
            }
        };
    }

    constructor() {
        super();
        this._styleAggregator = new StyleAggregator();
    }

    createRenderRoot() {
        return this;
    }

    connectedCallback() {
        super.connectedCallback();
        PTCS.styleAggregator.attachContext(this);
    }

    disconnectedCallback() {
        PTCS.styleAggregator.detachContext(this);
        super.disconnectedCallback();
    }

    get styleAggregator() {
        return this._styleAggregator;
    }
};

customElements.define(PTCS.StyleContext.is, PTCS.StyleContext);
