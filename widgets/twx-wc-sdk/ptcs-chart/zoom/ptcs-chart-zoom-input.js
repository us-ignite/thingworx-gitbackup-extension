import {LitElement, html} from 'lit';
import {choose} from 'lit/directives/choose.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-textfield/ptcs-textfield.js';
import 'ptcs-dropdown/ptcs-dropdown.js';
import 'ptcs-datepicker/ptcs-datepicker.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';

// NOTE:   This component creates it sub-components in the regular DOM, not in the shadow DOM
// REASON: The client should have direct styling access to to content, both via CSS and theme designer
//         (This component is only an internal implementation detail)

PTCS.ChartZoomInput = class extends PTCS.BehaviorFocus(L2Pw(LitElement)) {
    static get is() {
        return 'ptcs-chart-zoom-input';
    }

    render() {
        const _number = () => html`<ptcs-textfield part=${this.partNames} .disabled=${this.disabled} .label=${this.label}
            .text=${this.value} @text-changed=${this._onValueChanged} tabindex=${this.enableTabindex}></ptcs-textfield>`;

        const _date = () => html`<ptcs-datepicker part=${this.partNames} .disabled=${this.disabled} .label=${this.label}
            .dateTime=${this.value} @date-time-changed=${this._onValueChanged} .formatToken=${this.formatToken}
            style=${'width:' + PTCS.normalizeUnit(this.width)} .hintText=${this.hintText}
            show-time display-seconds tabindex=${this.enableTabindex}></ptcs-datepicker>`;

        const _labels = () => html`<ptcs-dropdown part=${this.partNames} .disabled=${this.disabled} .label=${this.label}
            .selectedValue=${this.value} @selected-value-changed=${this._onValueChanged} .items=${this.type} tabindex=${this.enableTabindex}
            .selector=${item => (typeof item === 'string' ? item : item.label)} .filter=${this.type.length > 6}></ptcs-dropdown>`;

        // eslint-disable-next-line no-nested-ternary
        const tf = t => (t === 'number' || t === 'date') ? t : (t instanceof Array ? 'labels' : false);

        return html`${choose(tf(this.type), [['number', _number], ['date', _date], ['labels', _labels]])}`;
    }

    static get properties() {
        return {
            type: {
                type: Object, // 'number' || 'date' || Array (of labels)
            },

            // Minimum value in data
            value: {
                type:   Object,
                notify: true
            },

            partNames: {
                type: String
            },

            formatToken: {
                type: String,
            },

            hintText: {
                type:  String,
                value: 'Select Date & Time'
            },

            width: {
                type: Number
            },

            label: {
                type: String
            },

            enableTabindex: {
                type: String
            },

            disabled: {
                type: Boolean
            }
        };
    }

    // The virtual scroller doesn't use a shadow dom
    createRenderRoot() {
        return this;
    }

    _onValueChanged(ev) {
        const value = ev.detail.value;
        if (this._isType(value)) {
            this.value = value;
        } else if (value === null || value === '') {
            // Reset
            this.value = value;
        }
    }

    _isType(value) {
        if (value === '' || value === null || value === undefined) {
            return false;
        }
        if (this.type === 'date') {
            return value instanceof Date;
        }
        if (this.type instanceof Array) {
            return this.type.some(s => s === value || s.label === value);
        }
        return !isNaN(+value);
    }
};

customElements.define(PTCS.ChartZoomInput.is, PTCS.ChartZoomInput);
