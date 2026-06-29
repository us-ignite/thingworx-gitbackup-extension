import {LitElement, html, css} from 'lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';

const STATE_DATA = 'data';
const STATE_LOADING = 'loading';
const STATE_NO_DATA = 'no-data';
const STATE_EMPTY = 'empty';
const STATE_ERROR = 'error';

PTCS.ChartState = class extends PTCS.BehaviorStyleable(LitElement) {

    static get styles() {
        return css`
        :host {
            width: 100%;
            height: 100%;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
        }

        :host([chart-state=data]) {
            display: none;
        }

        :host([chart-state=loading]) {
            background-size: 300% 100%;
            background-image: var(--ptcs-chart-state-animation-gradient-color,
                linear-gradient(270deg, rgba(255, 255, 255, 0) 25.56%, #FFFFFF 58.51%, rgba(255, 255, 255, 0) 100%));

            animation-direction: reverse;
            animation-timing-function: ease;
            animation-duration: 3s;
            animation-iteration-count: infinite;
            animation-name: animationMoveRight;
        }

        :host([chart-state=loading]) [part=icon] {
            width: 50%;
            height: 50%;
        }

        @keyframes animationMoveRight {
            0% {
                background-position: 0%;
            }
            100% {
                background-position: 100%;
            }
        }`;
    }

    render() {
        return html`
        <ptcs-icon  part="icon" .icon=${this._setIcon()}></ptcs-icon>
        <ptcs-label part="label" variant="title" .label=${this._setLabel()}></ptcs-label>`;
    }

    static get is() {
        return 'ptcs-chart-state';
    }

    static get properties() {
        return {
            // 'data', 'loading', 'no-data', 'error'
            chartStateExt: {
                type:      String,
                attribute: 'chart-state-ext'
            },

            chartStateDataError: {
                type:      Boolean,
                attribute: 'chart-state-data-error'
            },

            chartStateDataEmpty: {
                type:      Boolean,
                attribute: 'chart-state-data-empty'
            },

            iconLoading: {
                type:      String,
                attribute: 'icon-loading'
            },

            labelNoData: {
                type:      String,
                attribute: 'label-no-data'
            },

            iconNoData: {
                type:      String,
                attribute: 'icon-no-data'
            },

            labelEmpty: {
                type:      String,
                attribute: 'label-empty'
            },

            iconEmpty: {
                type:      String,
                attribute: 'icon-empty'
            },

            labelError: {
                type:      String,
                attribute: 'label-error'
            },

            iconError: {
                type:      String,
                attribute: 'icon-error'
            }
        };
    }

    firstUpdated() {
        super.firstUpdated();

        this._setChartState();
    }

    _computeChartState() {
        switch (this.chartStateExt) {
            case STATE_LOADING:
            case STATE_NO_DATA:
            case STATE_ERROR:
                return this.chartStateExt;

            case STATE_DATA:
            default:
                if (this.chartStateDataError) {
                    return STATE_ERROR;
                }
        }

        return this.chartStateDataEmpty ? STATE_EMPTY : STATE_DATA;
    }

    _setChartState() {
        const value = this._computeChartState();

        if (value !== this._$chartState) {
            this._$chartState = value;
            this.setAttribute('chart-state', value); // reflect
            this.dispatchEvent(new CustomEvent('chart-state-changed', {detail: {value}})); // notify
        }
    }

    get chartState() {
        this._setChartState(); // Make sure we have the correct value (don't wait for updated() to complete)
        return this._$chartState;
    }

    _setLabel() {
        switch (this.chartState) {
            case STATE_NO_DATA:
                return this.labelNoData;
            case STATE_EMPTY:
                return this.labelEmpty;
            case STATE_ERROR:
                return this.labelError;
            case STATE_DATA:
            default:
                return '';
        }
    }

    _setIcon() {
        switch (this.chartState) {
            case STATE_LOADING:
                return this.iconLoading;
            case STATE_NO_DATA:
                return this.iconNoData;
            case STATE_EMPTY:
                return this.iconEmpty;
            case STATE_ERROR:
                return this.iconError;
            case STATE_DATA:
            default:
                return '';
        }
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        if (['chartStateExt', 'chartStateDataEmpty', 'chartStateDataError'].some(propName => changedProperties.has(propName))) {
            this._setChartState();
        }
    }
};

customElements.define(PTCS.ChartState.is, PTCS.ChartState);
