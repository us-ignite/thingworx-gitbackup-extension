import {LitElement, html, css} from 'lit';
import {when} from 'lit/directives/when.js';
import {styleMap} from 'lit/directives/style-map.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-icon/ptcs-icon.js';
import 'ptcs-label/ptcs-label.js';

const TICK_MAX_DELAY = 50; // max timeout delay 50 ms
const SECONDS_FACTOR_BASE = 1000; // ms scale
const MINUTES_FACTOR_BASE = 60 * SECONDS_FACTOR_BASE;
const HOUR_FACTOR_BASE = 60 * MINUTES_FACTOR_BASE;
const DAY_FACTOR_BASE = 24 * HOUR_FACTOR_BASE;

PTCS.Timer = class extends PTCS.BehaviorTooltip(PTCS.BehaviorStyleable(LitElement)) {
    static get styles() {
        return css`
            :host {
                display: flex;
                width: 100%;
                height: 100%;
                flex-direction: column;
                overflow: auto;
            }

            :host([no-label-exist]) {
                justify-content: center;
            }

            [part=label] {
                margin-top: auto;
                margin-bottom: var(--ptcs-timer-inner-margin, 8px);
                min-height: fit-content;
                min-width: fit-content;
                flex-shrink: 0;
            }

            [part=value-ctr] {
                min-height: fit-content;
                min-width: fit-content;
                display: flex;
                flex-direction: row;
                align-items: center;
            }
        `;
    }

    render() {
        const formatedValue = this._getFormatedValue();
        const _valueContainerStyle = this._getValueContainerStyle();
        const _iconStyle = this._getIconStyle();

        return html`
            ${when(this.label, () => html`<ptcs-label id="label" part="label" .label=${this.label} .horizontalAlignment=${this.labelAlignment}
                variant=${this.labelType} disable-tooltip></ptcs-label>`)}
            <div id="value-ctr" part="value-ctr" style=${styleMap(_valueContainerStyle)}>
                <ptcs-label id="value" part="value" .label=${formatedValue} variant=${this.valueLabelType} disable-tooltip
                    ._depfield=${this.value}></ptcs-label>
                <ptcs-icon id="icon" part="icon" size="custom" .icon=${this._icon} style=${styleMap(_iconStyle)}
                    .iconWidth=${this._iconSize} .iconHeight=${this._iconSize} ._depfield=${this.value}></ptcs-icon>
            </div>
        `;
    }

    static get is() {
        return 'ptcs-timer';
    }

    static get properties() {
        return {
            label: {
                type: String
            },

            _noLabelExist: {
                type:      Boolean,
                attribute: 'no-label-exist',
                reflect:   true
            },

            labelType: {
                type:      String,
                attribute: 'label-type'
            },

            labelAlignment: { // left, center, right
                type:      String,
                attribute: 'label-alignment'
            },

            valueLabelType: {
                type:      String,
                attribute: 'value-label-type'
            },

            icon: {
                type: String
            },

            iconSize: {
                type:      Number,
                attribute: 'icon-size'
            },

            iconAlignment: { // left, right
                type:      String,
                attribute: 'icon-alignment'
            },

            alternateIcon: {
                type:      String,
                attribute: 'alternate-icon'
            },

            alternateIconSize: {
                type:      Number,
                attribute: 'alternate-icon-size'
            },

            alternateIconAlignment: { // left, right
                type:      String,
                attribute: 'alternate-icon-alignment'
            },

            alternateStyle: {
                type:      Boolean,
                reflect:   true,
                attribute: 'alternate-style'
            },

            horizontalAlignment: { // left, center, right
                type:      String,
                attribute: 'horizontal-alignment'
            },

            initialValue: {
                type:      Number,
                attribute: 'initial-value'
            },

            running: {
                type: Boolean
            },

            timerMode: { // stopwatch, countdown
                type:      String,
                attribute: 'timer-mode'
            },

            displayDays: {
                type:      Boolean,
                attribute: 'display-days'
            },

            displayMilliseconds: {
                type:      Boolean,
                attribute: 'display-milliseconds'
            },

            value: {
                type:      Number,
                attribute: false
            },

            _value: {
                state: true,
                type:  Number
            },
        };
    }

    constructor() {
        super();

        this._value = 0;
        this.__running$ = false;

        this.label = '';
        this.labelType = 'label';
        this.labelAlignment = 'left';
        this.valueLabelType = 'sub-header';
        this.horizontalAlignment = 'left';

        this.icon = '';
        this.iconSize = 16;
        this.iconAlignment = 'right';
        this.alternateIcon = '';
        this.alternateIconSize = 16;
        this.alternateIconAlignment = 'right';
        this.alternateStyle = false;

        this.displayDays = true;

        this.initialValue = 0;
        this.timerMode = 'stopwatch';
        this.displayMilliseconds = false;
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (['icon', 'alternateIcon', 'alternateStyle'].some(propName => changedProperties.has(propName))) {
            this._icon = this.alternateStyle ? this.alternateIcon : this.icon;
        }

        if (['iconSize', 'alternateIconSize', 'alternateStyle'].some(propName => changedProperties.has(propName))) {
            this._iconSize = this.alternateStyle ? this.alternateIconSize : this.iconSize;
        }

        if (['iconAlignment', 'alternateIconAlignment', 'alternateStyle'].some(propName => changedProperties.has(propName))) {
            this._iconAlignmentRight = 'right' === (this.alternateStyle ? this.alternateIconAlignment : this.iconAlignment);
        }

        if (changedProperties.has('label')) {
            this._noLabelExist = !this.label;
        }
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        // setting _depfield property for root state formatting
        this._depfield = this.value;

        if (changedProperties.has('_value')) {
            this.dispatchEvent(new CustomEvent('value-changed', {
                detail: {
                    value: this.value
                }
            }));
        }
    }

    get value() {
        return this._value;
    }

    set initialValue(initialValue) {
        if (initialValue === this.__initialValue$) {
            return;
        }
        this.__initialValue$ = initialValue;

        if (this._startValue === undefined) {
            this._value = initialValue;
        }
    }

    get initialValue() {
        return this.__initialValue$;
    }

    set timerMode(timerMode) {
        if (this.__timerMode$ === timerMode) {
            return;
        }

        const isRunning = this.running;
        if (isRunning) {
            this._stop();
        }

        this.__timerMode$ = timerMode;
        this._timerModeCountdown = timerMode === 'countdown';

        if (isRunning) {
            this._start();
        }
    }

    get timerMode() {
        return this.__timerMode$;
    }

    set running(running) {
        if (this.__running$ === running) {
            return;
        }

        if (running) {
            this._start();
        } else {
            this._stop();
        }
        this.__running$ = running;
    }

    get running() {
        return this.__running$;
    }

    reset() {
        const isRunning = this.running;
        if (isRunning) {
            this._stop();
        }
        this._value = this.initialValue;
        if (isRunning) {
            this._start();
        }
    }

    _setTimeout() {
        this._timeoutCall = true;
        const _timeoutDelay = this._timerModeCountdown && this.value < TICK_MAX_DELAY ? this.value : TICK_MAX_DELAY;
        this.timeoutID = setTimeout(() => {
            this.requestAnimationFrameID = requestAnimationFrame(this._tick.bind(this, false));
        }, _timeoutDelay);
    }

    _tick(forceStop) {
        if (forceStop && (this.timeoutID || this.requestAnimationFrameID)) {
            clearTimeout(this.timeoutID);
            cancelAnimationFrame(this.requestAnimationFrameID);
        }
        this.timeoutID = null;
        this.requestAnimationFrameID = null;

        if (this._timeoutCall) {
            const delatTime = Date.now() - this._startTick;
            const calcValue = this._startValue + (this._timerModeCountdown ? -delatTime : delatTime);
            this._value = calcValue < 0 ? 0 : calcValue;
            this._timeoutCall = false;

            if (this._timerModeCountdown && calcValue <= 0) {
                this.dispatchEvent(new CustomEvent('countdown-completed', {detail: 'ended'}));
                return;
            }
        }

        if (!forceStop) {
            this._setTimeout();
        }
    }

    _start() {
        if (this._timerModeCountdown && this.value <= 0) {
            return;
        }

        this._startTick = Date.now();
        this._startValue = this.value;
        this._internalRunning = true;
        this._setTimeout();
    }

    _stop() {
        if (this._timerModeCountdown && this.value === 0) {
            return;
        }
        this._tick(true);
    }

    _getFormatedValue() {
        let days = 0;
        let hours = 0;
        let minutes = 0;
        let seconds = 0;
        let milliseconds = 0;

        if (this.value >= 0) {
            days = Math.floor(this.value / DAY_FACTOR_BASE);
            hours = Math.floor(this.displayDays ? (this.value / HOUR_FACTOR_BASE) % 24 : this.value / HOUR_FACTOR_BASE);
            minutes = Math.floor((this.value / MINUTES_FACTOR_BASE) % 60);
            seconds = Math.floor((this.value / SECONDS_FACTOR_BASE) % 60);
            milliseconds = Math.floor(this.value % SECONDS_FACTOR_BASE);
        }

        const _daysStr = this.displayDays && days ? days + 'd ' : '';
        const _millisecondsStr = this.displayMilliseconds ? '.' + String(milliseconds).padStart(3, '0') : '';
        return _daysStr + String(hours).padStart(2, '0') + ':' + String(minutes).padStart(2, '0') +
            ':' + String(seconds).padStart(2, '0') + _millisecondsStr;
    }

    _getValueContainerStyle() {
        let _justifyContent;
        switch (this.horizontalAlignment) {
            case 'center':
                _justifyContent = 'center';
                break;

            case 'right':
                _justifyContent = 'flex-end';
                break;

            default:
                _justifyContent = 'flex-start';
        }
        const conStyle = {justifyContent: _justifyContent};

        if (this.label) {
            conStyle['margin-bottom'] = 'auto';
        }
        return conStyle;
    }

    _getIconStyle() {
        return {
            display:     this._icon ? 'inline-flex' : 'none',
            order:       this._iconAlignmentRight ? 1 : -1,
            marginRight: this._iconAlignmentRight ? '0px' : 'var(--ptcs-timer-icon-margin, 8px)',
            marginLeft:  this._iconAlignmentRight ? 'var(--ptcs-timer-icon-margin, 8px)' : '0px',
        };
    }
};

customElements.define(PTCS.Timer.is, PTCS.Timer);
