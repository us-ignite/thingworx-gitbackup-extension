import {LitElement} from 'lit';
import {PTCS} from 'ptcs-library/library.js';

PTCS.BehaviorTabindex = superClass => {
    return class extends superClass {
        static get properties() {
            return {
                tabindex: {
                    type: String
                },
                noTabindex: {
                    type:      Boolean,
                    attribute: 'no-tabindex'
                }
            };
        }

        constructor() {
            super();

            this.tabindex = '0';
        }

        _initTabindex() {
            const cb = this.__setTabindex.bind(this);
            this._createPropertyObserver('tabindex', cb);
            this._createPropertyObserver('noTabindex', cb);
            this.__setTabindex();
        }

        ready() {
            super.ready();

            // Initialize non Lit elements
            if (!(this instanceof LitElement)) {
                this._initTabindex();
            }
        }

        firstUpdated() {
            super.firstUpdated();

            // Initialize Lit elements
            console.assert(this instanceof LitElement);
            this._initTabindex();
        }

        __setTabindex() {
            const {tabindex, noTabindex} = this;
            if (noTabindex || (noTabindex === undefined && (tabindex === null || tabindex === ''))) {
                this.removeAttribute('tabindex');
            } else {
                this.setAttribute('tabindex', tabindex || '0');
            }
        }
    };
};
