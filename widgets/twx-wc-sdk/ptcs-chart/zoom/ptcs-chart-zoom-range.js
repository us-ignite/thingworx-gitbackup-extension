import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import {typeValue} from 'ptcs-library/library-chart.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import './ptcs-chart-zoom-input.js';

PTCS.ChartZoomRange = class extends PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))) {
    static get styles() {
        return css`
        :host {
            display: flex;
            justify-content: flex-start;
            align-items: flex-end;
            flex-direction: row;
            overflow: hidden;
            box-sizing: border-box;
        }

        ptcs-textfield, ptcs-dropdown, ptcs-datepicker {
            flex: 1 1 auto;
        }

        ptcs-chart-zoom-input {
            display: flex;
        }`;
    }

    render() {
        return html`
        <ptcs-chart-zoom-input part="pick" .label=${this.startLabel} .partNames=${this._startPartNames}
                               .width=${this.dateWidth} .enableTabindex=${this._delegatedFocus} .disabled=${this.disabled}
                               .type=${this.type} .value=${this._value(this.zoomStart, this.minValue)} .hintText=${this.hintText}
                               .formatToken=${this.dateFormat} @value-changed=${this._startChanged}></ptcs-chart-zoom-input>
        <ptcs-chart-zoom-input part="pick" .label=${this.endLabel} .partNames=${this._endPartNames}
                               .width=${this.dateWidth} .enableTabindex=${this._delegatedFocus} .disabled=${this.disabled}
                               .type=${this.type} .value=${this._value(this.zoomEnd, this.maxValue)} .hintText=${this.hintText}
                               .formatToken=${this.dateFormat} @value-changed=${this._endChanged}></ptcs-chart-zoom-input>`;
    }

    static get is() {
        return 'ptcs-chart-zoom-range';
    }

    static get properties() {
        return {
            type: {
                type: Object // 'number' || 'date' || Array (of labels)
            },

            // Minimum value in data
            minValue: {
                type: Object
            },

            // Maximum value in data
            maxValue: {
                type: Object
            },

            // zoomStart
            zoomStart: {
                type:   Object,
                notify: true
            },

            // zoomEnd
            zoomEnd: {
                type:   Object,
                notify: true
            },

            // Format of range-picker
            dateFormat: {
                type: String
            },

            // Width of range-picker
            dateWidth: {
                type: Number
            },

            _startPartNames: {
                type:  String,
                value: 'pick-start'
            },

            _endPartNames: {
                type:  String,
                value: 'pick-end'
            },

            startLabel: {
                type: String
            },

            endLabel: {
                type: String
            },

            disabled: {
                type: Boolean
            },

            // Hint text for the date range
            hintText: {
                type: String
            },

            _delegatedFocus: String
        };
    }

    _value(v1, v2) {
        const gv = v => {
            const x = this._typeVal(v);
            if (isNaN(x)) {
                return undefined;
            }
            // Normlize compound keys to base key
            return this.type instanceof Array ? (this.type[x].label || this.type[x]) : v;
        };
        const v = gv(v1);
        return v !== undefined ? v : gv(v2);
    }

    _typeVal(value) {
        return typeValue(value, this.type);
    }

    // Reset range
    reset() {
        if (this.disabled) {
            return;
        }
        this.zoomStart = undefined;
        this.zoomEnd = undefined;
        this.dispatchEvent(new CustomEvent('zoom-reset', {bubbles: true, composed: true, detail: {}}));
    }

    _startChanged(ev) {
        const value = ev.detail.value;
        if (value === this.zoomStart) {
            return; // No change
        }
        const tv = this._typeVal(value);
        if (isNaN(tv)) {
            this.zoomStart = undefined; // Invalid. Reset
        } else {
            this.zoomStart = value;
            if (this._typeVal(this.zoomEnd) < tv) {
                this.zoomEnd = PTCS.clone(value);
            }
        }
    }

    _endChanged(ev) {
        const value = ev.detail.value;
        if (value === this.zoomEnd) {
            return; // No change
        }
        const tv = this._typeVal(value);
        if (isNaN(tv)) {
            this.zoomEnd = undefined; // Invalid. Reset
        } else {
            this.zoomEnd = value;
            if (this._typeVal(this.zoomStart) > tv) {
                this.zoomStart = PTCS.clone(value);
            }
        }
    }
};

PTCS.ChartZoomRange.formatToken = {
    year:   'YYYY MMM DD',
    month:  'MMM DD',
    day:    'MMM DD kk:mm',
    hour:   'kk:mm',
    minute: 'kk:mm.ss',
    second: ':ss'
};

customElements.define(PTCS.ChartZoomRange.is, PTCS.ChartZoomRange);
