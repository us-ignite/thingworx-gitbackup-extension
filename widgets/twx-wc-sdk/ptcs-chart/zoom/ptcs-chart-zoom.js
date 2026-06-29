import {LitElement, html, css} from 'lit';
import {PTCS} from 'ptcs-library/library.js';
import './ptcs-chart-zoom-interval.js';
import './ptcs-chart-zoom-range.js';
import './ptcs-chart-zoom-slider.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';

const converter = value => value;

PTCS.ChartZoom = class extends PTCS.BehaviorTabindex(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(LitElement))) {
    static get styles() {
        return css`
        :host {
            display: block;
        }

        :host([side=left]), :host([side=right]), :host([side=left]) ptcs-chart-zoom-slider, :host([side=right]) ptcs-chart-zoom-slider {
            height: 100%;
        }`;
    }

    render() {
        const interval = this._showInterval && html`<ptcs-chart-zoom-interval part="interval-picker"
        tabindex=${this._delegatedFocus}
        .disabled=${this.disabled}
        .label=${this.intervalLabel}
        .control=${this.intervalControl}
        .origin=${this.intervalOrigin}
        .anchor=${this.intervalAnchor}
        .interval=${this.interval}
        .type=${this.type}
        .minValue=${this.minValue}
        .maxValue=${this.maxValue}
        .zoomStart=${this.zoomStart} @zoom-start-changed=${this._onZoomStart}
        .zoomEnd=${this.zoomEnd} @zoom-end-changed=${this._onZoomEnd}
        .startLabel=${this.intervalFromLabel}
        .endLabel=${this.intervalToLabel}
        .showAnchor=${this.showIntervalAnchor}></ptcs-chart-zoom-interval>`;

        const range = this._showRange && html`<ptcs-chart-zoom-range part="range-picker"
        tabindex=${this._delegatedFocus}
        .disabled=${this.disabled}
        .type=${this.type}
        .minValue=${this.minValue}
        .maxValue=${this.maxValue}
        .zoomStart=${this.zoomStart} @zoom-start-changed=${this._onZoomStart}
        .zoomEnd=${this.zoomEnd} @zoom-end-changed=${this._onZoomEnd}
        .startLabel=${this.rangeStartLabel}
        .endLabel=${this.rangeEndLabel}
        .dateFormat=${this.rangeDateFormat}
        .dateWidth=${this.rangeDateWidth}
        .hintText=${this.dateRangeHintText}></ptcs-chart-zoom-range>`;

        const slider = this.slider && html`<ptcs-chart-zoom-slider part="slider"
        tabindex=${this._delegatedFocus}
        .disabled=${this.disabled}
        .side=${this.side}
        .type=${this.type}
        .minValue=${this.minValue}
        .maxValue=${this.maxValue}
        .zoomStart=${this.zoomStart} @zoom-start-changed=${this._onZoomStart}
        .zoomEnd=${this.zoomEnd} @zoom-end-changed=${this._onZoomEnd}
        .label=${this.sliderLabel}
        .minLabel=${this.sliderMinLabel}
        .maxLabel=${this.sliderMaxLabel}
        .showAxis=${this.showSliderAxis}
        .axisLength=${this.axisLength}
        .reverseAxis=${this.reverseSlider}></ptcs-chart-zoom-slider>`;

        return html`${interval || ''}${range || ''}${slider || ''}`;
    }

    static get is() {
        return 'ptcs-chart-zoom';
    }

    static get properties() {
        return {
            // bottom || top || left || right
            side: {
                type:    String,
                reflect: true
            },

            // Specify interval
            // interval-control: 'dropdown' || 'radio'
            // "date":   [{label, duration: time-duration}, ...]
            // "number": [{label, duration: count}, ...]
            // labels: [{label, duration: count}, ...]
            //----
            // interval-control: 'textfield'
            // "date":   time-unit
            // "number": multiplier (default: 1)
            // labels:   mulitplier (default: 1)
            //----
            // time-duration = `${number}${time-unit}`
            // time-unit= 'Y' || 'M' || 'D' || 'W' || 'd' || 'h' || 'm' || 's' || 'ms'
            interval: {
                type: Object,
                converter
            },

            // Show the range picker
            rangePicker: {
                type:      Boolean,
                attribute: 'range-picker'
            },

            // Show the slider
            slider: {
                type: Boolean
            },

            showSliderAxis: {
                type:      Boolean,
                attribute: 'show-slider-axis'
            },

            // Data type: 'number' || 'date' || Array of labels
            type: {
                type: Object,
                converter
            },

            // Minimum zoom value
            minValue: {
                type:      Object,
                attribute: 'min-value',
                converter
            },

            // Maximum zoom value
            maxValue: {
                type:      Object,
                attribute: 'max-value',
                converter
            },

            // zoomStart
            zoomStart: {
                type:      Object,
                attribute: 'zoom-start',
                converter
            },

            // zoomEnd
            zoomEnd: {
                type:      Object,
                attribute: 'zoom-end',
                converter
            },

            // Label for interval picker
            intervalLabel: {
                type:      String,
                attribute: 'interval-label',
            },

            // Label for zoom slider
            sliderLabel: {
                type:      String,
                attribute: 'slider-label'
            },

            // Max label for zoom slider
            sliderMaxLabel: {
                type:      String,
                attribute: 'slider-max-label'
            },

            // Min label for zoom slider
            sliderMinLabel: {
                type:      String,
                attribute: 'slider-min-label'
            },

            // Label for range start drop-down
            rangeStartLabel: {
                type: String
            },

            // Label for range end drop-down
            rangeEndLabel: {
                type:      String,
                attribute: 'range-end-label'
            },

            // Format of range-picker
            rangeDateFormat: {
                type:      String,
                attribute: 'range-date-format'
            },

            // Width of range-picker
            rangeDateWidth: {
                type:      Number,
                attribute: 'range-date-width'
            },

            // Label for interval from label
            intervalFromLabel: {
                type:      String,
                attribute: 'interval-from-label'
            },

            // Label for interval to label
            intervalToLabel: {
                type:      String,
                attribute: 'interval-to-label'
            },

            // 'radio' || 'dropdown' || 'textfield'
            intervalControl: {
                type:      String,
                attribute: 'interval-control'
            },

            // 'start' || 'end'
            intervalOrigin: {
                type:      String,
                attribute: 'interval-origin'
            },

            intervalAnchor: {
                type:      Object,
                attribute: 'interval-anchor',
                converter
            },

            // Show start interval control
            showIntervalAnchor: {
                type:      Boolean,
                attribute: 'show-interval-anchor'
            },

            // Reverse the direction of the slider
            reverseSlider: {
                type:      Boolean,
                attribute: 'reverse-slider'
            },

            disabled: {
                type: Boolean
            },

            _showInterval: {
                type: Boolean
            },

            _showRange: {
                type: Boolean
            },

            // Size of axis
            axisLength: {
                type:      String,
                attribute: 'axis-length'
            },

            _delegatedFocus: String
        };
    }

    constructor() {
        super();

        this._resizeObserver = new ResizeObserver(entries => {
            requestAnimationFrame(() => {
                if (this.id === 'zoomX') {
                    const xZoom = this.parentElement.xZoom;
                    if ((xZoom === true || Array.isArray(xZoom)) &&
                        ((Array.isArray(this.interval) || this.slider || this.rangePicker ||
                            this.parentElement.querySelector('[part=core-chart]').zoomSelect))) {
                        this.noTabindex = false;
                    } else {
                        this.noTabindex = true;
                    }
                }
                if (this.id === 'zoomY') {
                    const yZoom = this.parentElement.yZoom;
                    if ((yZoom === true || Array.isArray(yZoom)) &&
                        ((Array.isArray(this.interval) || this.slider || this.rangePicker ||
                            this.parentElement.querySelector('[part=core-chart]').zoomSelect))) {
                        this.noTabindex = false;
                    } else {
                        this.noTabindex = true;
                    }
                }
                for (let el = this.shadowRoot.firstChild; el; el = el.nextSibling) {
                    if (typeof el.resizeEv === 'function') {
                        el.resizeEv();
                    }
                }
            });
        });
    }

    connectedCallback() {
        super.connectedCallback();
        this._resizeObserver.observe(this);
    }

    disconnectedCallback() {
        this._resizeObserver.unobserve(this);
        super.disconnectedCallback();
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (['side', 'interval', 'type', 'intervalOrigin', 'intervalControl'].some(propName => changedProperties.has(propName))) {
            this._showInterval = this._computeShowInterval();
        }

        if (['side', 'rangePicker'].some(propName => changedProperties.has(propName))) {
            this._showRange = this._computeShowRange();
        }

        if (changedProperties.has('vertical')) {
            this._verticalChanged();
        }
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        ['zoomStart', 'zoomEnd'].forEach(propName => {
            if (changedProperties.has(propName)) {
                this.dispatchEvent(new CustomEvent(`${window.camelToDashCase(propName)}-changed`, {detail: {value: this[propName]}}));
            }
        });
    }

    _onZoomStart(ev) {
        this.zoomStart = ev.detail.value;
    }

    _onZoomEnd(ev) {
        this.zoomEnd = ev.detail.value;
    }

    _computeShowInterval() {
        const {side, interval, type, intervalOrigin, intervalControl} = this;
        if (side === 'left' || side === 'right') {
            return false;
        }
        if (intervalOrigin !== 'start' && intervalOrigin !== 'end') {
            return false;
        }
        if (intervalControl === 'radio' || intervalControl === 'dropdown') {
            return interval instanceof Array;
        }
        if (intervalControl === 'textfield') {
            if (type === 'date') {
                return typeof interval === 'string' && interval !== '';
            }
            if (type !== 'number' && !(type instanceof Array)) {
                return false;
            }
            return interval !== '' && !isNaN(+interval);
        }

        // Unknown interval type
        return false;
    }

    _computeShowRange() {
        return this.side !== 'left' && this.side !== 'right' && this.rangePicker;
    }

    _resetToDefaultValues() {
        this.zoomStart = undefined;
        this.zoomEnd = undefined;
        this.intervalAnchor = undefined;
    }
};

customElements.define(PTCS.ChartZoom.is, PTCS.ChartZoom);
