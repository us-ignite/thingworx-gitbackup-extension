import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-list/ptcs-list.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';


PTCS.ChartDisplayAxis = class extends PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))) {
    static get styles() {
        return css`
            :host {
                display: flex;
                flex-direction: column;
                position: absolute;
            }

            :host(:not([open])) {
                display: none;
            }

            ptcs-list {
                flex: 1 1 auto;
                width: 100%;
            }

            ptcs-list::part(multi-select) {
                display: none;
            }

            [part=actions] {
                flex: 0 0 auto;
                display: flex;
                flex-direction: column;
                box-sizing: border-box;
            }

            ptcs-button {
                width: 100%;
            }`;
    }

    render() {
        return html`
            <ptcs-list id="list" part="list" tabindex=${this._delegatedFocus} .items=${this.axes} multi-select
                       selector="label" .stateSelector=${this._stateSelector}></ptcs-list>
            <div part="actions">
                <ptcs-button id="apply" part="apply" variant="primary" .label=${this.applyButtonText}
                             @action=${this._apply} tabindex=${this._delegatedFocus}></ptcs-button>
                <ptcs-button id="cancel" part="cancel" variant="tertiary" .label=${this.cancelButtonText}
                             @action=${this._cancel} tabindex=${this._delegatedFocus}></ptcs-button>
            </div>`;
    }

    static get is() {
        return 'ptcs-chart-display-axis';
    }

    static get properties() {
        return {
            // Is this element open? (visible?)
            open: {
                type:     Boolean,
                notify:   true,
                reflect:  true,
                observer: '_openChanged'
            },

            // The axes (see ptcs-cart-combo for details)
            axes: {
                type:     Array,
                observer: '_axesChanged'
            },

            // Axis id's of enabled axes (i.e. axes in use on current chart)
            enabled: {
                type:     Array,
                observer: '_enabledChanged'
            },

            // Selected (visible) axes
            selectedIndexes: {
                type:   Array,
                notify: true
            },

            // 'Apply' button label
            applyButtonText: {
                type:      String,
                value:     'Apply',
                attribute: 'apply-button-text'
            },

            // 'Cancel' button label
            cancelButtonText: {
                type:      String,
                value:     'Cancel',
                attribute: 'cancel-button-text'
            },

            _delegatedFocus: {
                type: String
            }
        };
    }

    constructor() {
        super();

        // Click outside this element => cancel
        this._autoCancel = ev => ev.composedPath().every(el => el !== this) && this._cancel();

        this.axes = [];
        this.selectedIndexes = [];
    }

    ready() {
        super.ready();

        this._stateSelector = axis => ((this.enabled || []).indexOf(axis.id) >= 0) ? '' : 'disabled';

        this.addEventListener('keydown', this._keyDown.bind(this), true);
        this.addEventListener('blur', this._blur.bind(this)); // If the user tabs away from the element
    }

    _openChanged(open) {
        if (open) {
            const opt = {passive: true};
            document.addEventListener('mousedown', this._autoCancel, opt);
            document.addEventListener('touchstart', this._autoCancel, opt);
        } else {
            document.removeEventListener('mousedown', this._autoCancel);
            document.removeEventListener('touchstart', this._autoCancel);
        }
    }


    updated(changedProperties) {
        super.updated(changedProperties);

        if (changedProperties.has('selectedIndexes')) {
            this.$.list.selectedIndexes = [...(this.selectedIndexes || [])];
        }
    }

    _axesChanged() {
        // (Re)select all axes when the axes changes
        this.selectedIndexes = (this.axes || []).reduce((a, axis, i) => {
            if (!axis.hide) {
                a.push(i);
            }
            return a;
        }, []);

        this.$.list.selectedIndexes = [...this.selectedIndexes];
    }

    _enabledChanged() {
        // Some axes may have been disabled or enabled
        this.$.list.refresh();
    }

    _blur() {
        if (this.open) {
            this._cancel();
        }
    }

    _keyDown(ev) {
        if (ev.key === 'Escape' && !ev.defaultPrevented) {
            this._cancel();
            ev.preventDefault();
        }
    }

    _apply() {
        this.selectedIndexes = [...(this.$.list.selectedIndexes || [])];
        this.open = false;
    }

    _cancel() {
        this.$.list.selectedIndexes = [...(this.selectedIndexes || [])];
        this.open = false;
    }
};

customElements.define(PTCS.ChartDisplayAxis.is, PTCS.ChartDisplayAxis);
