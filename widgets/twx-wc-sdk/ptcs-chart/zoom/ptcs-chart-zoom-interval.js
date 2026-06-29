import {LitElement, html, css} from 'lit';
import {choose} from 'lit/directives/choose.js';
import {map} from 'lit/directives/map.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import {typeValue} from 'ptcs-library/library-chart.js';
import 'ptcs-textfield/ptcs-textfield.js';
import 'ptcs-dropdown/ptcs-dropdown.js';
import 'ptcs-radio/ptcs-radio.js';
import './ptcs-chart-zoom-input.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';

PTCS.ChartZoomInterval = class extends PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))) {

    static get styles() {
        return css`
        :host {
            display: flex;
            justify-content: flex-start;
            align-items: flex-end;
        }
        [part~=radio-body] {
            display: flex;
            justify-content: flex-start;
            align-items: center;
            flex-direction: row;
            flex-wrap: wrap;
        }
        ptcs-chart-zoom-input {
            display: flex;
        }
        ptcs-chart-zoom-input[hidden] {
            display: none;
        }`;
    }

    render() {
        const textfield = () => html`<ptcs-textfield part="interval" tabindex=${this._delegatedFocus} .label=${this.label}
            .text=${this._textValue()} @text-changed=${this._onTextChanged} ></ptcs-textfield>`;

        const dropdown = () => {
            const {value, interval} = this;
            const index = Array.isArray(interval) ? interval.findIndex(item => item.duration === value) : -1;
            const items = Array.isArray(interval) ? interval.map(item => item.label || 'No label') : [];

            return html`<ptcs-dropdown part="interval" tabindex=${this._delegatedFocus} .label=${this.label}
                .items=${items} .selected=${index} .filter=${items.length > 6}
                @selected-changed=${this._onSelectedChanged}></ptcs-dropdown>`;
        };

        const radio = () => {
            const {value, interval} = this;
            const selected = Array.isArray(interval) ? interval.findIndex(item => item.duration === value) : -1;

            return html`<div part="interval radio-group">
                <ptcs-label part="label" variant="label" .label=${this.label}></ptcs-label>
                <div part="radio-body">${map(interval || [], (item, index) => html`<ptcs-radio
                    .label=${item.label} .checked=${selected === index}
                    @checked-changed=${ev => this._setValueRadio(index, ev.detail.value)}></ptcs-radio>`)}</div></div>`;
        };

        return html`
            <ptcs-chart-zoom-input part="pick" .label=${this.startLabel} .partNames=${this._startPartNames} .enableTabindex=${this._delegatedFocus}
                .disabled=${this.disabled}
                @value-changed=${this._startChanged}
                .type=${this._type} .value=${this._value(this._anchorStart, this.anchor, this.minValue)}
                ?hidden=${this._hideStart()}></ptcs-chart-zoom-input>
            <ptcs-chart-zoom-input part="pick" .label=${this.endLabel} .partNames=${this._endPartNames} .enableTabindex=${this._delegatedFocus}
                .disabled=${this.disabled}
                @value-changed=${this._endChanged}
                .type=${this._type} .value=${this._value(this._anchorEnd, this.anchor, this.maxValue)}
                ?hidden=${this._hideEnd()}></ptcs-chart-zoom-input>
            ${choose(this.control, [['textfield', textfield], ['dropdown', dropdown], ['radio', radio]])}`;
    }

    static get is() {
        return 'ptcs-chart-zoom-interval';
    }

    static get properties() {
        return {
            label: {
                type: String
            },

            // dropdown, radio, textfield
            control: {
                type:    String,
                reflect: true
            },

            // control: 'dropdown' || 'radio'
            // "date":    [{label, duration: time-duration}, ...]
            // "numbers": [{label, duration: count}, ...]
            // "labels":  [{label, duration: count}, ...]
            //----
            // control: 'textfield'
            // "date":    time-unit
            // "numbers": multiplier (default: 1)
            // "labels":  mulitplier (default: 1)
            //----
            // time-duration = `${number}${time-unit}`
            // time-unit= 'Y' || 'M' || 'D' || 'W' || 'd' || 'h' || 'm' || 's' || 'ms'
            interval: {
                type: Object
            },

            // 'start' || 'end' ==> zoomEnd = _anchorStart + intervalValue || zoomStart = _anchorEnd - intervalValue
            origin: {
                type: Object
            },

            // Externally specified anchor
            anchor: {
                type:        Object,
                observer:    '_anchorChanged',
                observeWhen: 'immediate'
            },

            // Specified start anchor (by UI control)
            _anchorStart: {
                type: Object
            },

            // Specified end anchor (by UI control)
            _anchorEnd: {
                type: Object
            },

            // The specified interval
            value: {
                type: Number
            },

            // Type of interval data
            type: {
                type:        Object, // 'number' || 'date' || Array (of labels) [=labels]
                observer:    '_typeChanged',
                observeWhen: 'immediate'
            },

            // Validated type
            _type: {
                type: Object
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
                type:     Object,
                notify:   true,
                observer: '_zoomStartChanged'
            },

            // zoomEnd
            zoomEnd: {
                type:     Object,
                notify:   true,
                observer: '_zoomEndChanged'
            },

            showAnchor: {
                type: Boolean
            },

            // i18n
            startLabel: {
                type: String
            },

            endLabel: {
                type: String
            },

            disabled: {
                type: Boolean
            },

            _delegatedFocus: {
                type: String,
            }
        };
    }

    constructor() {
        super();

        this._startPartNames = 'pick-start';
        this._endPartNames = 'pick-end';

        this.__old = {};
    }

    _onTextChanged(ev) {
        this._setValue(`${ev.detail.value}${this.interval}`);
    }

    _textValue() {
        const {value} = this;

        if (typeof value === 'string' && value !== '') {
            const m = /^([0-9.]+)(\w+)$/g.exec(value);
            if (m && m[2] === this.interval) {
                return m[1];
            }
        }
        return '';
    }

    _onSelectedChanged(ev) {
        this._setValueIndex(ev.detail.value);
    }

    _value(v1, v2, v3) {
        if (this._isType(v1)) {
            return v1;
        }
        return this._isType(v2) ? v2 : v3;
    }

    _hideStart() {
        return this.origin !== 'start' || !this.showAnchor;
    }

    _hideEnd() {
        return this.origin !== 'end' || !this.showAnchor;
    }

    _isType(value) {
        return !isNaN(typeValue(value, this._type));
    }

    _typeChanged(type) {
        if (type !== 'number' && type !== 'date' && !(type instanceof Array)) {
            return; // Invalid type
        }
        this._type = type;
    }

    _anchor() {
        if (this.origin === 'start') {
            return this._value(this._anchorStart, this.anchor, this.minValue);
        }
        if (this.origin === 'end') {
            return this._value(this._anchorEnd, this.anchor, this.maxValue);
        }
        return undefined;
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        const {origin, value} = this;

        if (value === undefined || value === null || value === '') {
            return;
        }

        const _anchor = this._anchor();
        if (!this._isType(_anchor)) {
            return;
        }

        if (value === this.__old.value && origin === this.__old.origin && _anchor === this.__old._anchor) {
            return;
        }

        this.__old = {value, origin, _anchor};

        switch (this._type) {
            case 'date':
                this._changeDateInterval(value, origin, _anchor);
                break;
            case 'number':
                this._changeNumberInterval(value, origin, _anchor);
                break;
            default:
                if (this._type instanceof Array) {
                    this._changeLabelInterval(value, origin, _anchor);
                }
        }

        this._zoomStart = this.zoomStart;
        this._zoomEnd = this.zoomEnd;
    }

    _anchorChanged() {
        this._anchorStart = undefined;
        this._anchorEnd = undefined;
    }

    _zoomStartChanged(zoomStart) {
        // zoomStart can be a number encoded as a string
        // eslint-disable-next-line eqeqeq
        if (zoomStart == this._zoomStart) {
            return;
        }
        if (zoomStart instanceof Date && this._zoomStart instanceof Date && zoomStart.getTime() === this._zoomStart.getTime()) {
            return;
        }
        this.value = '';
        this._zoomStart = zoomStart;
    }

    _zoomEndChanged(zoomEnd) {
        // zoomEnd can be a number encoded as a string
        // eslint-disable-next-line eqeqeq
        if (zoomEnd == this._zoomEnd) {
            return;
        }
        if (zoomEnd instanceof Date && this._zoomEnd instanceof Date && zoomEnd.getTime() === this._zoomEnd.getTime()) {
            return;
        }
        this.value = '';
        this._zoomEnd = zoomEnd;
    }

    _startChanged(ev) {
        const value = ev.detail.value;
        if (this._isType(value)) {
            this._anchorStart = value;
        } else if (value === null || value === '') {
            if (this._anchorStart !== undefined) {
                this._anchorStart = undefined;
            } else {
                // Need explicit reset
                ev.target.value = this._value(this.anchor, this.minValue);
            }
        }
    }

    _endChanged(ev) {
        const value = ev.detail.value;
        if (this._isType(value)) {
            this._anchorEnd = value;
        } else if (value === null || value === '') {
            if (this._anchorEnd !== undefined) {
                this._anchorEnd = undefined;
            } else {
                // Need explicit reset
                ev.target.value = this._value(this.anchor, this.maxValue);
            }
        }
    }

    _changeNumberInterval(value, origin, _anchor) {
        // Convert to number
        value = +value;
        _anchor = +_anchor;
        if (isNaN(value) || isNaN(_anchor)) {
            return;
        }
        if (origin === 'start') {
            this.zoomStart = _anchor;
            this.zoomEnd = Math.min(_anchor + value, this.maxValue);
        } else if (origin === 'end') {
            this.zoomStart = Math.max(_anchor - value, this.minValue);
            this.zoomEnd = _anchor;
        }
    }

    _changeLabelInterval(value, origin, _anchor) {
        value = +value; // Convert to number
        if (value <= 0 || isNaN(value)) {
            return;
        }
        const index = this._type.findIndex(s => s === _anchor);
        if (!(index >= 0)) {
            return;
        }
        if (origin === 'start') {
            this.zoomStart = _anchor;
            this.zoomEnd = this._type[Math.max(Math.min(index + value, this._type.length - 1), 0)];
        } else if (origin === 'end') {
            this.zoomStart = this._type[Math.max(index - value, 0)];
            this.zoomEnd = _anchor;
        }
    }

    _changeDateInterval(value, origin, _anchor) {
        if (typeof value !== 'string') {
            return;
        }
        const m = /^([0-9.]+)(\w+)$/g.exec(value);
        if (!m) {
            return;
        }
        const num = +m[1];
        const unit = m[2];
        let delta = 0;

        // Compute delta
        switch (unit) {
            case 'Y':
            case 'M':
                // delta is dependent on anchor
                break;
            case 'W':
                delta = num * 7 * 24 * 60 * 60 * 1000;
                break;
            case 'd':
                delta = num * 24 * 60 * 60 * 1000;
                break;
            case 'h':
                delta = num * 60 * 60 * 1000;
                break;
            case 'm':
                delta = num * 60 * 1000;
                break;
            case 's':
                delta = num * 1000;
                break;
            case 'ms':
                delta = num;
                break;
            default:
                // Failure
                return;
        }

        if (origin === 'start') {
            if (!delta) {
                const date = new Date(_anchor);
                if (unit === 'Y') {
                    date.setFullYear(date.getFullYear() + num);
                } else if (unit === 'M') {
                    const years = Math.floor(num / 12);
                    date.setFullYear(date.getFullYear() + years);
                    date.setMonth(date.getMonth() + (num - 12 * years));
                }
                delta = date.getTime() - _anchor.getTime();
            }
            this.zoomStart = new Date(_anchor);
            this.zoomEnd = new Date(Math.min(_anchor.getTime() + delta, this.maxValue.getTime()));
        } else if (origin === 'end') {
            if (!delta) {
                const date = new Date(_anchor);
                if (unit === 'Y') {
                    date.setFullYear(date.getFullYear() - num);
                } else if (unit === 'M') {
                    let months = date.getMonth();
                    if (months >= num) {
                        date.setMonth(months - num);
                    } else {
                        let years = Math.floor(num / 12);
                        const num2 = num - 12 * years;
                        if (months < num2) {
                            years++; // One more year to subtract
                            months += 12; // 12 more months to use
                        }
                        date.setFullYear(date.getFullYear() - years);
                        date.setMonth(months - num2);
                    }
                }
                delta = _anchor.getTime() - date.getTime();
            }
            this.zoomStart = new Date(Math.max(_anchor.getTime() - delta, this.minValue.getTime()));
            this.zoomEnd = new Date(_anchor);
        }
    }

    // String value
    _setValue(value) {
        this.value = value;
    }

    // Value by index
    _setValueIndex(index) {
        this._setValue(index >= 0 && this.interval instanceof Array ? this.interval[index].duration : '');
    }

    // Value by radio button index
    _setValueRadio(index, checked) {
        if (checked) {
            this._setValueIndex(index);
        }
    }

    reset() {
        if (this.disabled) {
            return;
        }
        this.zoomStart = undefined;
        this.zoomEnd = undefined;
        this._anchorStart = undefined;
        this._anchorEnd = undefined;
        this.dispatchEvent(new CustomEvent('zoom-reset', {bubbles: true, composed: true, detail: {}}));
    }
};

customElements.define(PTCS.ChartZoomInterval.is, PTCS.ChartZoomInterval);
