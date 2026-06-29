import {LitElement, html, css} from 'lit';
import {PTCS} from 'ptcs-library/library.js';
import {typeValue} from 'ptcs-library/library-chart.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-slider/ptcs-slider.js';
import '../axes/ptcs-chart-axis.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import {select} from 'd3-selection';

function neq(a, b) {
    // eslint-disable-next-line eqeqeq
    return (a instanceof Date && b instanceof Date) ? (a.getTime() !== b.getTime()) : (a != b); // Need to compare some strings as numbers
}

PTCS.ChartZoomSlider = class extends PTCS.BehaviorFocus(PTCS.BehaviorStyleable(LitElement)) {

    static get styles() {
        return css`
        :host {
            display: flex;
            justify-content: flex-start;
            align-items: center;
            flex-direction: row;
            overflow: hidden;
            box-sizing: border-box;
        }

        :host([vertical]) {
            flex-direction: column;
        }

        [part=slider-container] {
            flex: 1 1 auto;
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-direction: column;
            box-sizing: border-box;
        }

        :host([vertical]) [part=slider-container] {
            flex-direction: row-reverse;
            height: 100%;
        }

        [part=zoom-slider] {
            display: flex;
            position: relative;
            width: 100%;
        }

        :host([vertical]) [part=zoom-slider] {
            height: 100%;
        }

        [part=zoom-axis] {
            width: calc(100% - 2 * 9px);
        }

        [part=ticks] {
            position: absolute;
        }

        :host(:not([vertical])) [part=ticks] {
            left: 0px;
            right: 0px;
        }

        :host([vertical]) [part=ticks] {
            top: 0px;
            bottom: 0px;
        }

        [part=tick-line] {
            position: absolute;
        }

        :host(:not([vertical])) [part=tick-line] {
            top: 0;
            bottom: 0;
            width: 1px;
        }

        :host([vertical]) [part=tick-line] {
            left: 0;
            right: 0;
            height: 1px;
        }

        :host(:not([vertical])) #slider {
            width: 100%;
            heigth: 100%;
        }

        :host([vertical]) #slider {
            heigth: 100%;
            width: 100%;
        }`;
    }

    render() {
        return html`
        <div part="slider-container" id="slider-container">
        <div part="zoom-slider">
            <div part="ticks" id="ticks" ?hidden=${!this.showAxis}></div>
            <ptcs-slider part="slider" id="slider" tabindex=${this._delegatedFocus}
                .disabled=${this.disabled} .precision=${this._precision()} .vertical=${this.vertical}
                .label=${this.label} .minLabel=${this.minLabel} .maxLabel=${this.maxLabel}
                .minValue=${this._typeVal(this.minValue)}  .maxValue=${this._typeVal(this.maxValue)}
                .value=${this._typeVal2(this.zoomStart, this.minValue)} .value2=${this._typeVal2(this.zoomEnd, this.maxValue)} range
                .step=${this._stepSize()}
                @value-changed=${this._thumb1Changed} @value2-changed=${this._thumb2Changed}
                .reverseMinmax=${this._reverseAxis()}
                thumb-icon="#circle" thumb2-icon="#circle" overlap-thumbs></ptcs-slider>
        </div>
        <ptcs-chart-axis part="zoom-axis" id="axis" ?hidden=${!this.showAxis}
            .type=${this.type}
            .minValue=${this.minValue}
            .maxValue=${this.maxValue}
            .size=${this._size}
            .reverse=${this.reverseAxis}
            @scale-changed=${this._onScaleChanged}
            @ticks-changed=${this._onTicksChanged}
            .side=${this.side} zoom></ptcs-chart-axis>
        </div>`;
    }

    static get is() {
        return 'ptcs-chart-zoom-slider';
    }

    static get properties() {
        return {
            // bottom || top || left || right
            side: {
                type: String
            },

            vertical: {
                type:    Boolean,
                reflect: true
            },

            type: {
                type: Object // "number" || "date" || Array (of labels)
            },

            // Length in pixels of control. Used by slider axis
            _size: {
                type: Number
            },

            // Minimum value in data
            minValue: {
                type:      Object,
                attribute: 'min-value'
            },

            // Maximum value in data
            maxValue: {
                type:      Object,
                attribute: 'max-value'
            },

            // zoomStart
            zoomStart: {
                type:      Object,
                attribute: 'zoom-start'
            },

            // zoomEnd
            zoomEnd: {
                type:      Object,
                attribute: 'zoom-end'
            },

            // Reverse the axis direction
            reverseAxis: {
                type:      Boolean,
                reflect:   true,
                attribute: 'reverse-axis'
            },

            // Slider label
            label: {
                type: String
            },

            // Slider Min label
            minLabel: {
                type: String
            },

            // Slider Max label
            maxLabel: {
                type: String
            },

            // Show the slider axis?
            showAxis: {
                type:      Boolean,
                attribute: 'show-axis'
            },

            // Length of axis (or use full length of element)
            axisLength: {
                type:      String,
                attribute: 'axis-length'
            },

            // Axis scale
            _scale: {
                type: Function
            },

            disabled: {
                type: Boolean
            },

            // Axis ticks
            _ticks: {
                type: Array
            },

            _delegatedFocus: String
        };
    }

    constructor() {
        super();
        this.type = 'number';
    }

    set side(_side) {
        if (this._$side === _side || ['bottom', 'top', 'left', 'right'].indexOf(_side) === -1) {
            return;
        }
        this._$side = _side;

        this.vertical = _side === 'left' || _side === 'right';
    }

    get side() {
        return this._$side;
    }

    _onScaleChanged(ev) {
        this._scale = ev.detail.value;
    }

    _onTicksChanged(ev) {
        this._ticks = ev.detail.value;
    }

    _precision() {
        return this.type === 'number' ? 6 : undefined;
    }

    _stepSize() {
        const v1 = typeValue(this.minValue, this.type);
        const v2 = typeValue(this.maxValue, this.type);

        if (isNaN(v1) || isNaN(v2)) {
            return false;
        }

        return Math.abs(v2 - v1) / 50;
    }

    _verticalChanged() {
        const slider = this.shadowRoot.getElementById('slider');

        if (slider) {
            const rect = slider.getBoundingClientRect();
            this._size = this.vertical ? rect.height : rect.width;
        }
    }

    _reverseAxis() {
        return this.vertical ? !this.reverseAxis : this.reverseAxis;
    }

    _axisLengthChanged() {
        const el = this.shadowRoot.getElementById('slider-container');
        const style = el.style;
        const al = PTCS.cssDecodeSize(this.axisLength, el, this.vertical);
        if (al >= 0) {
            if (this.vertical) {
                style.width = '';
                style.height = `${al}px`;
            } else {
                style.width = `${al}px`;
                style.height = '';
            }
            style.flex = '0 0 auto';
        } else {
            style.width = '';
            style.height = '';
            style.flex = '';
        }
    }

    _isType(value, type) {
        return !isNaN(typeValue(value, type));
    }

    _typeVal(value) {
        return typeValue(value, this.type);
    }

    _typeVal2(v1, v2) {
        const x = typeValue(v1, this.type);
        return isNaN(x) ? typeValue(v2, this.type) : x;
    }

    _number2value(value) {
        if (this.type === 'date') {
            return new Date(+value);
        }
        if (this.type instanceof Array) {
            const item = (0 <= value && value < this.type.length) ? this.type[+value] : this.minValue;
            return item !== undefined ? (item.label || item) : undefined;
        }
        return +value;
    }

    _thumb1Changed(ev) {
        const value = this._number2value(ev.detail.value);
        if (this._isType(value, this.type) && neq(value, this.zoomStart)) {
            this.zoomStart = neq(value, this.minValue) ? value : undefined;
        }
    }

    _thumb2Changed(ev) {
        const value = this._number2value(ev.detail.value);
        if (this._isType(value, this.type) && neq(value, this.zoomEnd)) {
            this.zoomEnd = neq(value, this.maxValue) ? value : undefined;
        }
    }

    _ticksChanged() {
        if (!this._scale) {
            return; // Not ready
        }
        const setPos = this.vertical
            ? d => `translate(0, ${d.offs}px)`
            : d => `translate(${d.offs}px, 0)`;

        const ticksEl = this.shadowRoot.getElementById('ticks');
        const slider = this.shadowRoot.getElementById('slider');

        if (this.vertical) {
            ticksEl.style.width = `${slider.clientWidth}px`;
            ticksEl.style.height = '';
        } else {
            ticksEl.style.width = '';
            ticksEl.style.height = `${slider.clientHeight}px`;
        }

        const join = select(ticksEl)
            .selectAll('[part=tick-line]')
            .data(this._ticks || []);

        // EXIT old elements not present in new data
        join.exit().remove();

        // UPDATE old elements present in new data
        join.style('transform', setPos);

        // ENTER new elements present in new data
        join.enter()
            .append('div')
            .attr('part', 'tick-line')
            .style('transform', setPos);
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (changedProperties.has('vertical')) {
            this._verticalChanged();
        }
    }


    updated(changedProperties) {
        super.updated(changedProperties);

        if (['_ticks', 'vertical'].some(prop => changedProperties.has(prop))) {
            this._ticksChanged();
        }

        if (['axisLength', 'vertical', '_size'].some(prop => changedProperties.has(prop))) {
            this._axisLengthChanged();
        }

        ['zoomStart', 'zoomEnd'].forEach(prop => {
            if (changedProperties.has(prop)) {
                this.dispatchEvent(new CustomEvent(`${window.camelToDashCase(prop)}-changed`, {detail: {value: this[prop]}}));
            }
        });
    }

    resizeEv() {
        this._verticalChanged();
    }

    reset() {
        if (this.disabled) {
            return;
        }
        this.zoomStart = undefined;
        this.zoomEnd = undefined;
        this.dispatchEvent(new CustomEvent('zoom-reset', {bubbles: true, composed: true, detail: {}}));
    }
};

customElements.define(PTCS.ChartZoomSlider.is, PTCS.ChartZoomSlider);
