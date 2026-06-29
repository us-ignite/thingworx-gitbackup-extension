import {LitElement, html, css} from 'lit';
import {map} from 'lit/directives/map.js';
import {when} from 'lit/directives/when.js';
import {styleMap} from 'lit/directives/style-map.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-behavior-validate/ptcs-behavior-validate.js';
import 'ptcs-state-unit/ptcs-state-unit.js';
import 'ptcs-icon/ptcs-icon.js';
import 'ptcs-label/ptcs-label.js';

const Layouts = Object.freeze({
    DIAL:       'dial',
    HORIZONTAL: 'horizontal',
    VERTICAL:   'vertical'
});

const Directions = Object.freeze({
    CLOCKWISE:         'clockwise',
    COUNTER_CLOCKWISE: 'counter-clockwise',
    LEFT_TO_RIGHT:     'left to right',
    RIGHT_TO_LEFT:     'right to left',
    BOTTOM_TO_TOP:     'bottom to top',
    TOP_TO_BOTTOM:     'top to bottom',
});

const ICON_PORTION = 0.15; // 15%

const isValidNum = val => !isNaN(parseFloat(val));

// eslint-disable-next-line max-len
PTCS.KpiDial = class extends PTCS.BehaviorTabindex(PTCS.BehaviorTooltip(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(PTCS.BehaviorValidate(LitElement))))) {
    static get styles() {
        return css`
            :host {
                display: flex;
                flex-direction: column;
                box-sizing: border-box;
                outline: none;
            }

            [part=root] {
                display: flex;
                flex: 1 1 auto;
                position: relative;
                min-width: 0;
                min-height: 0;
            }

            :host([layout=horizontal]) [part=root] {
                flex-direction: column;
            }

            :host([layout=vertical]) [part=root] {
                flex-direction: row;
            }

            :host([layout=dial]) [part=root] {
                align-items: center;
                justify-content: center;
            }

            [part=value-ctr] {
                display: flex;
                justify-content: center;
                align-items: center;
                box-sizing: border-box;
                min-width: 0;
                min-height: 0;
            }

            :host([layout=horizontal]) [part=value-ctr], :host([layout=vertical]) [part=value-ctr] {
                flex: 1 1 auto;
            }

            [part=value] {
                min-width: auto;
            }

            [part=value]::part(label) {
                text-overflow: unset;
            }

            [part=value][min-font-size]::part(label) {
                text-overflow: ellipsis;
            }

            [part=UOM] {
                min-width: 12px;
                max-width: 50%;
            }

            :host([layout=horizontal]) [part=value-tracker] {
                display: flex;
                flex-direction: var(--ptcs-kpi-direction);
                width: 100%;
                height: 18%;
            }

            :host([layout=vertical]) [part=value-tracker] {
                display: flex;
                flex-direction: var(--ptcs-kpi-direction);
                width: 12%;
                height: 100%;
            }

            :host([layout=horizontal]) [part=value-tracker-filling] {
                width: var(--ptcs-kpi-percent);
                max-width: 100%;
                height: 100%;
            }

            :host([layout=vertical]) [part=value-tracker-filling] {
                width: 100%;
                max-height: 100%;
                height: var(--ptcs-kpi-percent);
            }

            :host([layout=horizontal]) [part=target-range-tracker] {
                display: flex;
                flex-direction: var(--ptcs-kpi-direction);
                align-items: flex-end;
                width: 100%;
                height: 12%; /* 7% height + 5% gap */
            }

            :host([layout=vertical]) [part=target-range-tracker] {
                display: flex;
                flex-direction: var(--ptcs-kpi-direction);
                width: 4%;
                height: 100%;
                margin-right: 5%;
            }

            :host([layout=dial]) [part=target-range-tracker] {
                position: absolute;
                transform: var(--ptcs-kpi-direction);
                fill: none;
            }

            :host([layout=dial]) #tracker-svg {
                position: absolute;
                transform: var(--ptcs-kpi-direction);
                fill: none;
            }

            :host([layout=horizontal]) [part=target-states] {
                height: 58.333%; /* 7% height devided in total percentage (12) */
            }

            [part=icon] {
                position: absolute;
            }

            [hidden] {
                display: none;
            }
        `;
    }

    render() {
        switch (this.layout) {
            case Layouts.HORIZONTAL:
                return this.renderHorizontalLayout();
            case Layouts.VERTICAL:
                return this.renderVerticalLayout();
            default:
                return this.renderDialLayout();
        }
    }

    renderLabel() {
        return html`
            <ptcs-label id="label" part="label" variant="${this.labelType}" .label=${this.label}
                .horizontalAlignment=${this.labelAlignment} ?hidden=${!this.label} disable-tooltip></ptcs-label>
        `;
    }

    renderIcon() {
        const position = {
            right:  this.layout === Layouts.VERTICAL || this.iconPosition === 'right' ? 0 : 'unset',
            left:   this.layout !== Layouts.VERTICAL && this.iconPosition === 'left' ? 0 : 'unset',
            top:    this.layout !== Layouts.VERTICAL || this.iconPosition === 'top' ? 0 : 'unset',
            bottom: this.layout === Layouts.VERTICAL && this.iconPosition === 'bottom' ? 0 : 'unset'
        };

        return html`
            <ptcs-icon id="icon" part="icon" size="custom" .icon=${this._icon} ?hidden=${!this._icon} style=${styleMap(position)}></ptcs-icon>
        `;
    }

    renderValue() {
        return html`
            <div id="value-ctr" part="value-ctr">
                <ptcs-label id="value" part="value" .label=${this._formattedValue} disable-tooltip></ptcs-label>
                <ptcs-label id="UOM" part="UOM" variant="caption" .label=${this.unitOfMeasure}
                    ?hidden=${!this.showUnitOfMeasure || !this.unitOfMeasure || !this._hasValue} disable-tooltip></ptcs-label>
            </div>
        `;
    }

    renderValueTracker() {
        if (this.layout === Layouts.DIAL) {
            /* eslint-disable indent */
            return html`
                <svg id="tracker-svg" stroke-width="9%">
                    <circle id="value-tracker" part="value-tracker" cx="50%" cy="50%"></circle>
                    <circle id="value-tracker-filling" part="value-tracker-filling" cx="50%" cy="50%"></circle>
                </svg>
                ${when(this._statesValues && !(this._statesValues.length === 1 && this._statesValues[0].default), () =>
                    html`<svg id="target-range-tracker" part="target-range-tracker" stroke-width="3.5%" padding="2.5%"></svg>`
                )}
            `;
        }

        return html`
            <div id="value-tracker" part="value-tracker">
                <div id="value-tracker-filling" part="value-tracker-filling"></div>
            </div>
        `;
    }

    renderTargetRangeTracker() {
        /* eslint-disable indent */
        return html`
            ${when(this._statesValues && !(this._statesValues.length === 1 && this._statesValues[0].default), () =>
                html`
                    <div id="target-range-tracker" part="target-range-tracker">
                        ${map(this._statesValues, (curr, i) => {
                            const range = this._getValuesRange(this._statesValues, i);
                            const trackerLength = Math.abs(this.maxValue - this.minValue);
                            const percent = (range / trackerLength) * 100;
                            const aspect = this.layout === Layouts.HORIZONTAL ? 'width' : 'height';
                            return html`
                                <div id="${curr.name}" part="target-states" ?default=${curr.default}
                                    style="${aspect}: ${percent}%; background: ${curr.color};"></div>
                            `;
                        })}
                    </div>
                `
            )}
        `;
    }

    renderDialLayout() {
        return html`
            <div id="root" part="root">
                ${this.renderValueTracker()}
                ${this.renderValue()}
                ${this.renderIcon()}
            </div>
            ${this.renderLabel()}
        `;
    }

    renderHorizontalLayout() {
        return html`
            <div id="root" part="root">
                ${this.renderValue()}
                ${this.renderValueTracker()}
                ${this.renderTargetRangeTracker()}
                ${this.renderIcon()}
            </div>
            ${this.renderLabel()}
        `;
    }

    renderVerticalLayout() {
        return html`
            <div id="root" part="root">
                ${this.renderTargetRangeTracker()}
                ${this.renderValueTracker()}
                ${this.renderValue()}
                ${this.renderIcon()}
            </div>
            ${this.renderLabel()}
        `;
    }

    static get is() {
        return 'ptcs-kpi-dial';
    }

    static get properties() {
        return {
            layout: {
                type:    String,
                reflect: true
            },

            label: {
                type: String
            },

            labelType: {
                type:      String,
                attribute: 'label-type'
            },

            minValue: {
                type:      Number,
                attribute: 'min-value'
            },

            maxValue: {
                type:      Number,
                attribute: 'max-value'
            },

            value: {
                type:     Number,
                validate: '_validateValue(minValidValue, maxValidValue, extraValidation)',
            },

            _hasValue: {
                type:  Boolean,
                state: true
            },

            _formattedValue: {
                type:  String,
                state: true
            },

            valueFormat: {
                type:      String,
                attribute: 'value-format'
            },

            _decimalPlaces: {
                type:  Number,
                state: true
            },

            tooltipValueFormat: {
                type:      String,
                attribute: 'tooltip-value-format'
            },

            unitOfMeasure: {
                type:      String,
                attribute: 'unit-of-measure'
            },

            showUnitOfMeasure: {
                type:      Boolean,
                attribute: 'show-unit-of-measure'
            },

            _icon: {
                type:  String,
                state: true
            },

            iconPosition: {
                type:      String,
                attribute: 'icon-position'
            },

            maxIconSize: {
                type:      Number,
                attribute: 'max-icon-size'
            },

            labelAlignment: {
                type:      String,
                attribute: 'label-alignment'
            },

            direction: {
                type: String
            },

            startAngle: {
                type:      Number,
                attribute: 'start-angle'
            },

            endAngle: {
                type:      Number,
                attribute: 'end-angle'
            },

            _startOffset: {
                type:  Number,
                state: true
            },

            _endOffset: {
                type:  Number,
                state: true
            },

            _circumference: {
                type:  Number,
                state: true
            },

            _hiddenAnglesHeight: {
                type:  Number,
                state: true
            },

            stateFormat: {
                type:      Object,
                attribute: 'state-format'
            },

            _statesValues: {
                type:  Array,
                state: true
            },

            _spaceActivate: {
                type:  Boolean,
                state: true
            },

            _enterActivate: {
                type:  Boolean,
                state: true
            },

            minValidValue: {
                type:      Number,
                attribute: 'min-valid-value',
                isValue:   minValidValue => isValidNum(minValidValue)
            },

            maxValidValue: {
                type:      Number,
                attribute: 'max-valid-value',
                isValue:   maxValidValue => isValidNum(maxValidValue)
            },

            minValueFailureMessage: {
                type:      String,
                attribute: 'min-value-failure-message'
            },

            maxValueFailureMessage: {
                type:      String,
                attribute: 'max-value-failure-message'
            },

            // Client-provided custom validation function
            extraValidation: {
                type:      Function,
                attribute: 'extra-validation'
            }
        };
    }

    constructor() {
        super();

        this.layout = Layouts.DIAL;
        this.direction = Directions.CLOCKWISE;
        this.label = 'Label';
        this.labelType = 'label';
        this.labelAlignment = 'left';
        this.minValue = 0;
        this.maxValue = 100;
        this.showUnitOfMeasure = false;
        this.iconPosition = 'right';
        this.startAngle = 45;
        this.endAngle = 315;
        this.valueFormat = '0000';
        this._hiddenAnglesHeight = 0;
        this._spaceActivate = true;
        this._enterActivate = true;

        // Validation props
        this.minValueFailureMessage = '${value} is the minimum value';
        this.maxValueFailureMessage = '${value} is the maximum value';
        this.hideValidationCriteria = true;
        this.hideValidationSuccess = true;
        this.hideValidationError = true;
        this._stayUnvalidated = true;

        this.tooltipFunc = this._monitorTooltip.bind(this);
        this._resizeObserver = new ResizeObserver(this._refreshLayout.bind(this));
        this._resizeObserverUOM = new ResizeObserver(this._scaleText.bind(this));
    }

    connectedCallback() {
        super.connectedCallback();
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this._resizeObserver.disconnect();
        this._resizeObserverUOM.disconnect();
    }

    firstUpdated() {
        super.firstUpdated();

        // Generate unique ID, only if none was already defined by customer(as mashup), to every KPI so we will not have state formats collisions
        if (!this.id) {
            this.id = `kpi-${(Date.now() * Math.random()).toString(16).replace('.', '').toUpperCase()}`;
        }
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (changedProperties.has('valueFormat')) {
            this._decimalPlaces = this._getDecimalPlaces(this.valueFormat);
            this._scaleText();
        }

        if (['value', 'minValue', 'maxValue', 'layout', 'valueFormat'].some(propName => changedProperties.has(propName))) {
            this._valueUpdated(this.value, changedProperties.get('value'));
        }

        if (changedProperties.has('showUnitOfMeasure') || changedProperties.has('unitOfMeasure')) {
            this._scaleText();
        }
    }

    update(changedProperties) {
        super.update(changedProperties);

        if (changedProperties.has('stateFormat')) {
            this._updateStatesValues();
            this._updateStateUnit();
            this._updateStyleUnit();
            this._refreshLayout();
        }

        const props = ['layout', 'startAngle', 'endAngle', 'minValue', 'maxValue', 'stateFormat', 'labelType', 'maxIconSize'];
        if (props.some(propName => changedProperties.has(propName))) {
            this._refreshLayout();
        }

        if (changedProperties.has('direction')) {
            this._updateDirection();
        }

        if (changedProperties.has('layout')) {
            this._updateDirection();
            this._resetIconPosition();
            this._resizeObserver.disconnect();
            this._resizeObserver.observe(this.shadowRoot.getElementById('root')); // Root element changed
            this._resizeObserverUOM.disconnect();
            this._resizeObserverUOM.observe(this.shadowRoot.getElementById('UOM'));
            this._insertValidationMessage(this._validationMessageEl); // Re-insert the validation message after the layout was changed
        }

        if (changedProperties.has('value') || changedProperties.has('hideValidationError') || changedProperties.has('hideValidationSuccess')) {
            this._stayUnvalidated = isValidNum(this.value) ? (!!this.hideValidationError && !!this.hideValidationSuccess) : true;
        }

        if (changedProperties.has('_hasValue')) {
            PTCS.setAttribute(this, 'no-value', !this._hasValue);
            this._updateStateUnit();
            this._updateStyleUnit();
        }

        if (changedProperties.has('_hiddenAnglesHeight')) {
            if (this._validationMessageEl) {
                this._validationMessageEl.style.marginTop = this.layout === Layouts.DIAL ? `-${this._hiddenAnglesHeight - 1}px` : 'unset';
            }
        }
    }

    updated(changedProperties) {
        super.updated(changedProperties);
        if (changedProperties.has('value')) {
            // The external validation setup listens to a 'changed' event
            this.dispatchEvent(new CustomEvent('value-changed', {detail: {value: this.value}}));
        }
    }

    // Wait for sub elements to update as well & paint animation frame
    async getUpdateComplete() {
        await super.getUpdateComplete();

        const label = this.shadowRoot.getElementById('label');
        const value = this.shadowRoot.getElementById('value');
        const UOM = this.shadowRoot.getElementById('UOM');
        const icon = this.shadowRoot.getElementById('icon');

        await Promise.all([label, value, UOM, icon].map(el => el.updateComplete));
        await new Promise(requestAnimationFrame);

        return true;
    }

    get value() {
        return this._value;
    }

    set value(newVal) {
        this._setNumber('_value', newVal);
    }

    get minValue() {
        return this._minValue;
    }

    set minValue(newVal) {
        this._setNumber('_minValue', newVal);
    }

    get maxValue() {
        return this._maxValue;
    }

    set maxValue(newVal) {
        this._setNumber('_maxValue', newVal);
    }

    get labelType() {
        return this._labelType;
    }

    set labelType(newVal) {
        const options = ['caption', 'body', 'label', 'title', 'large-title', 'sub-header', 'header', 'large-header'];
        this._labelType = options.includes(typeof newVal === 'string' ? newVal.toLowerCase() : null) ? newVal : this._labelType;
    }

    get hideValidationCriteria() {
        return this._hideValidationCriteria;
    }

    set hideValidationCriteria(newVal) {
        this._hideValidationCriteria = true; // Always hide validation criteria message (unvalidated)
    }

    _setNumber(propName, value) {
        if (value === undefined || value === null) {
            this[propName] = value;
        } else {
            const parsed = parseFloat(value);
            this[propName] = !isNaN(parsed) ? parsed : NaN;
        }
    }

    _monitorTooltip() {
        const prettify = str => str.split(/[-, ]+/g).map(part => part.charAt(0).toUpperCase() + part.slice(1)).join(' ');
        const getStateFormatName = () => {
            if (!this._statesValues) {
                return '';
            }
            // eslint-disable-next-line max-len
            const res = this._statesValues.find((state, i) => state.name === this.getAttribute('ptcstate-value') && (!state.default || state.name !== `state-${i}`));
            return res ? prettify(res.name) : '';
        };

        let value = isValidNum(this.value) ? this.value : NaN;
        if (!isNaN(value) && this.tooltipValueFormat) {
            value = this._getFormattedValue(value, this._getDecimalPlaces(this.tooltipValueFormat)); // Format value
        }

        const lineBreak = '\n\n';
        const label = this.label || '';
        const maxValue = isValidNum(this.maxValue) ? this.maxValue : this.maxValue || NaN;
        const unitOfMeasure = this.unitOfMeasure && this.showUnitOfMeasure ? this.unitOfMeasure : '';
        const statesValues = this._statesValues;
        const stateFormat = getStateFormatName();
        const dash = label && stateFormat ? ' - ' : '';
        const title = `${label}${dash}${stateFormat}${label || stateFormat ? lineBreak : ''}`;

        let tooltip = `${title}` + `${value} / ${maxValue} ${unitOfMeasure}`.trim();

        if (statesValues) {
            for (let i = 0; i < statesValues.length; i++) {
                const prev = i > 0 ? statesValues[i - 1] : {value: this.minValue};
                const curr = statesValues[i];

                if (curr.default) {
                    break;
                }

                const startValue = prev.comparator === '<=' ? prev.value + 1 : prev.value;
                const endValue = i < statesValues.length - 1 ? curr.value : this.maxValue;

                /* eslint-disable no-useless-concat */
                tooltip += `${lineBreak}${prettify(curr.name)}: ${startValue} - ${endValue} ${unitOfMeasure}`;
            }
        }

        return tooltip.trim();
    }

    // Scale the font size so the text will grow and fill the available space of its parent container
    async _scaleText() {
        const MIN_FONT_SIZE = 12;

        await this.updateComplete;

        const valueCtr = this.shadowRoot.getElementById('value-ctr');
        const value = this.shadowRoot.getElementById('value');
        const innerLabel = value.shadowRoot.getElementById('label');
        const UOM = this.shadowRoot.getElementById('UOM');

        const valueCtrCS = getComputedStyle(valueCtr);
        const paddingLeft = parseFloat(valueCtrCS.paddingLeft);
        const paddingRight = parseFloat(valueCtrCS.paddingRight);
        const paddingTop = parseFloat(valueCtrCS.paddingTop);
        const paddingBottom = parseFloat(valueCtrCS.paddingBottom);

        const uomWidth = UOM.offsetWidth + 4; // Give it a bit of extra space to prevent unnecessarily label overflow
        const availableWidth = valueCtr.clientWidth - (paddingLeft + paddingRight) - uomWidth;
        const availableHeight = valueCtr.clientHeight - (paddingTop + paddingBottom);
        const currentWidth = innerLabel.scrollWidth;
        const currentHeight = innerLabel.clientHeight;
        const scale = Math.min((availableWidth / currentWidth), (availableHeight / currentHeight));
        const currFontSize = parseFloat(getComputedStyle(value).fontSize);
        const newFontSize = Math.max(MIN_FONT_SIZE, (scale * currFontSize));

        value.style.fontSize = `${newFontSize}px`;
        value.style.maxWidth = `${availableWidth}px`;
        value.style.maxHeight = `${availableHeight}px`;

        PTCS.setAttribute(value, 'min-font-size', newFontSize === MIN_FONT_SIZE); // Reached minimum font-size
    }

    // Counts the number of decimal places in valueFormat
    _getDecimalPlaces(format) {
        format = format || '';
        const decimalIx = format.lastIndexOf('.');

        if (decimalIx === -1) {
            return 0;
        }

        let i = decimalIx + 1;
        while ('0' <= format[i] && format[i] <= '9') {
            i++;
        }

        return i - decimalIx - 1;
    }

    // Returns the value formatted according to decimalPlaces
    _getFormattedValue(value, decimalPlaces) {
        return this._hasValue ? parseFloat(value).toFixed(decimalPlaces) : '0';
    }

    // Simple comparison to check if the number of digits are equal or not
    _compareLength(val1, val2) {
        const getLength = val => val.toString().length;
        return getLength(val1) === getLength(val2);
    }

    // Get normalized value while taking min/max values into considiration
    _normalizedValue(value, inverse = false) {
        if (value <= this.minValue) {
            return inverse ? 1 : 0;
        }
        if (value >= this.maxValue) {
            return inverse ? 0 : 1;
        }
        const normal = (value - this.minValue) / (this.maxValue - this.minValue);
        return inverse ? 1 - normal : normal;
    }

    _valueUpdated(newVal, oldVal) {
        this._hasValue = isValidNum(newVal) && isValidNum(this.minValue) && isValidNum(this.maxValue);
        this._formattedValue = this._getFormattedValue(newVal, this._decimalPlaces);
        if (!this._compareLength(this._formattedValue, this._getFormattedValue(oldVal, this._decimalPlaces))) {
            this._scaleText(); // Re-scal the text if the number of digits changed
        }
        this._fillPercentage = this._hasValue ? this._normalizedValue(newVal) : 0;
        this.style.setProperty('--ptcs-kpi-percent', `${this._fillPercentage * 100}%`);
        if (this.layout === Layouts.DIAL) {
            this._updateDialProgress();
        }
    }

    // Update the KPI direction according to its layout
    _updateDirection() {
        this.direction = this.direction.toLowerCase();
        let styling;

        switch (this.layout) {
            case Layouts.HORIZONTAL:
                if (this.direction !== Directions.LEFT_TO_RIGHT && this.direction !== Directions.RIGHT_TO_LEFT) {
                    this.direction = Directions.LEFT_TO_RIGHT; // Reset to default
                }
                styling = this.direction === Directions.RIGHT_TO_LEFT ? 'row-reverse' : 'row';
                break;
            case Layouts.VERTICAL:
                if (this.direction !== Directions.BOTTOM_TO_TOP && this.direction !== Directions.TOP_TO_BOTTOM) {
                    this.direction = Directions.BOTTOM_TO_TOP; // Reset to default
                }
                styling = this.direction === Directions.TOP_TO_BOTTOM ? 'column' : 'column-reverse';
                break;
            case Layouts.DIAL:
                if (this.direction !== Directions.CLOCKWISE && this.direction !== Directions.COUNTER_CLOCKWISE) {
                    this.direction = Directions.CLOCKWISE; // Reset to default
                }
                styling = this.direction === Directions.COUNTER_CLOCKWISE ? 'scale(-1, 1) rotate(90deg)' : 'rotate(90deg)';
                break;
        }

        this.style.setProperty('--ptcs-kpi-direction', styling);
    }

    _resetIconPosition() {
        const pos = this.iconPosition;
        if (this.layout === Layouts.VERTICAL) {
            this.iconPosition = pos !== 'top' && pos !== 'bottom' ? 'top' : pos;
            return;
        }
        this.iconPosition = pos !== 'right' && pos !== 'left' ? 'right' : pos;
    }

    _refreshLayout() {
        requestAnimationFrame(() => {
            if (this.layout === Layouts.DIAL) {
                this._initDialSize();
                this._updateDialProgress();
            }
            this._setMaxIconSize();
            this._scaleText();
        });
    }

    _setMaxIconSize() {
        /* eslint-disable no-nested-ternary */
        const icon = this.shadowRoot.getElementById('icon');
        const root = this.shadowRoot.getElementById('root');
        const valueCtr = this.shadowRoot.getElementById('value-ctr');
        const isVertical = this.layout === Layouts.VERTICAL;
        const isDial = this.layout === Layouts.DIAL;
        const ctr = isDial ? root : valueCtr;

        // Reset padding
        root.style.padding = 'unset';
        valueCtr.style.padding = 'unset';

        if (icon.hidden) {
            return;
        }

        const iconCS = getComputedStyle(icon);
        const minIconSize = Math.min(parseFloat(iconCS.minWidth), parseFloat(iconCS.minHeight));
        const maxIconSize = this.maxIconSize || Number.MAX_SAFE_INTEGER;
        const ctrW = ctr.clientWidth;
        const ctrH = ctr.clientHeight;
        const space = isVertical ? (ctrH * ICON_PORTION) / 2 : (ctrW * ICON_PORTION) / 2;
        const maxW = isVertical ? ctrW : space;
        const maxH = isVertical ? space : isDial ? (ctrH - this._hiddenAnglesHeight) : ctrH;
        const maxSize = Math.max(minIconSize, Math.min(maxIconSize, maxW, maxH));

        icon.style.maxWidth = `${maxSize}px`;
        icon.style.maxHeight = `${maxSize}px`;
        ctr.style.paddingTop = isVertical ? `${maxSize}px` : 'unset';
        ctr.style.paddingRight = isVertical ? 'unset' : `${maxSize}px`;
        ctr.style.paddingBottom = isVertical ? `${maxSize}px` : 'unset';
        ctr.style.paddingLeft = isVertical ? 'unset' : `${maxSize}px`;
    }

    // Initialize the dial's size and offset according to start/end angles
    _initDialSize() {
        const dialCtr = this.shadowRoot.getElementById('root');
        const trackerSVG = this.shadowRoot.getElementById('tracker-svg');
        this._dialValueTracker = this.shadowRoot.getElementById('value-tracker');
        this._dialValueTrackerFilling = this.shadowRoot.getElementById('value-tracker-filling');

        if (!dialCtr.clientWidth) {
            return;
        }

        PTCS.setAttribute(this._dialValueTrackerFilling, 'hidden', true); // Hide the tracker filling while updating to prevent a jiggling feeling

        // Get the min size of the container and set the SVG circle accordingly
        const dialCtrCS = getComputedStyle(dialCtr);
        const dialCtrWidth = parseFloat(dialCtrCS.width);
        const dialCtrHeight = parseFloat(dialCtrCS.height);
        const availableSpace = Math.min(dialCtrWidth, dialCtrHeight);

        trackerSVG.style.width = `${availableSpace}px`;
        trackerSVG.style.height = `${availableSpace}px`;

        // The stroke is drawn along the edge of the circle, which means it extends both inside and outside the circle's boundary
        const strokeWidth = (parseFloat(trackerSVG.getAttribute('stroke-width')) / 100) * availableSpace;
        const radius = (availableSpace - strokeWidth) / 2;
        this._circumference = 2 * Math.PI * radius;
        this._startOffset = this._circumference * (+this.startAngle / 360);
        this._endOffset = this._circumference * (+this.endAngle / 360);

        this._dialValueTracker.setAttribute('r', radius);
        this._dialValueTracker.style.strokeDasharray = `${this._endOffset - this._startOffset} ${this._circumference}`;
        this._dialValueTracker.style.strokeDashoffset = -this._startOffset;
        this._dialValueTrackerFilling.setAttribute('r', radius);
        this._dialValueTrackerFilling.style.strokeDashoffset = -this._startOffset;

        // Calculate the hidden part of the circle and adjust the main label accordingly
        this._hiddenAnglesHeight = this._calcHiddenHeight(radius, strokeWidth / 2); // The outer part of the stroke
        const extraHeight = dialCtrHeight > dialCtrWidth ? (dialCtrHeight - dialCtrWidth) / 2 : 0;
        this.shadowRoot.getElementById('label').style.transform = `translateY(-${(this._hiddenAnglesHeight + extraHeight)}px)`;

        this._initDialTargets(availableSpace, radius, strokeWidth);
        requestAnimationFrame(() => this._scaleText());

        PTCS.setAttribute(this._dialValueTrackerFilling, 'hidden', false);
    }

    _updateDialProgress() {
        if (!this._dialValueTrackerFilling) {
            return;
        }

        const dashArrayFilling = this._fillPercentage * (this._endOffset - this._startOffset);
        this._dialValueTrackerFilling.style.strokeDasharray = `${dashArrayFilling} ${this._circumference}`;
    }

    // Creates a circle element for the target range values
    _createCircleEl(id, radius, curr, startAngle, endAngle) {
        let circle = this.shadowRoot.getElementById(id);

        if (!circle) {
            circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
            circle.id = id;
            circle.setAttribute('cx', '50%');
            circle.setAttribute('cy', '50%');
            circle.setAttribute('part', 'target-states');
        }

        const circumference = 2 * Math.PI * radius;
        const startOffset = circumference * (startAngle / 360);
        const endOffset = circumference * (endAngle / 360);

        PTCS.setAttribute(circle, 'state', curr.name);
        PTCS.setAttribute(circle, 'stroke', curr.color);
        PTCS.setbattr(circle, 'default', !curr.color);
        circle.setAttribute('r', radius);
        circle.style.strokeDasharray = `${endOffset - startOffset} ${circumference}`;
        circle.style.strokeDashoffset = -startOffset;

        return circle;
    }

    // Get the range in between two state formats values
    _getValuesRange(statesValues, i) {
        const prev = i === 0 ? {value: this.minValue} : statesValues[i - 1];
        const curr = statesValues[i];
        const next = i < statesValues.length ? statesValues[i + 1] : null;

        const val = Math.min(Math.max(curr.value, this.minValue), this.maxValue); // restrict according to min/max values
        const range = Math.abs(val - prev.value);

        // When both values are equal, it means that one of them has comparator of '<=', and it needs to take a chunk of 1 out of it.
        if (next && val === next.value) {
            return range - 1;
        } else if (i > 0 && val === prev.value) {
            return 1;
        }
        return range;
    }

    // Initialize the dial's range tracker size and creates the targets range according to stateFormat values
    _initDialTargets(availableSpace, dialRadius, valueStrokeWidth) {
        const statesValues = !this._statesValues || (this._statesValues.length === 1 && this._statesValues[0].default) ? null : this._statesValues;

        if (!statesValues) {
            this._updateValueCtrDimensions(dialRadius, availableSpace);
            return;
        }

        const targetRangeTracker = this.shadowRoot.getElementById('target-range-tracker');
        targetRangeTracker.style.width = `${availableSpace}px`;
        targetRangeTracker.style.height = `${availableSpace}px`;

        const targetStrokeWidth = (parseFloat(targetRangeTracker.getAttribute('stroke-width')) / 100) * availableSpace;
        const paddingWidth = (parseFloat(targetRangeTracker.getAttribute('padding')) / 100) * availableSpace;
        const radius = ((availableSpace - targetStrokeWidth) / 2) - (valueStrokeWidth + paddingWidth);
        const circumference = 2 * Math.PI * radius;
        const visibleAngles = +this.endAngle - +this.startAngle;

        const getPadding = (state, val) => {
            if (visibleAngles !== 360 && ((state === 'startAngle' && val === this.minValue) || (state === 'endAngle' && val === this.maxValue))) {
                return 0;
            }
            return (360 / circumference) / 2; // Degrees per pixel - Calculate how many degrees correspond to 1 pixel for padding purpose
        };

        let valuesSum = this.minValue;

        // 1. Calculate the start/end angles for each value according to its precentage out of the visible angles
        // 2. Add a padding of 1px between each target, excluding the edges when it's an arc and not a full circle
        // 3. Create a new circle element for each new target and set its id, radius, angles and color
        // 4. In case needed, create a background circle for the remaining space
        for (let i = 0; i < statesValues.length; i++) {
            const range = this._getValuesRange(statesValues, i);
            const normal = this._normalizedValue(valuesSum);

            const startAngle = (+this.startAngle) + (normal * visibleAngles) + getPadding('startAngle', valuesSum);

            valuesSum += range;

            const inverseNormal = this._normalizedValue(valuesSum, 'inverse');

            const endAngle = (+this.endAngle) - (inverseNormal * visibleAngles) - getPadding('endAngle', valuesSum);

            if (range > 0) {
                targetRangeTracker.appendChild(this._createCircleEl(`s${i + 1}`, radius, statesValues[i], startAngle, endAngle));
            }

            if (valuesSum >= this.maxValue) {
                break;
            }
        }

        this._updateValueCtrDimensions(radius, availableSpace);
    }

    // When the dial size changes or affected by start/end angles, the value needs to scale accordingly
    _updateValueCtrDimensions(radius, availableSpace) {
        const valueCtr = this.shadowRoot.getElementById('value-ctr');

        const diagonal = (radius * 2) / Math.sqrt(2); // Length of diagonal
        const hiddenPercentage = this._hiddenAnglesHeight / availableSpace;
        const labelOffset = diagonal * hiddenPercentage;

        valueCtr.style.width = `${diagonal}px`;
        valueCtr.style.height = `${diagonal - labelOffset}px`;
        valueCtr.style.transform = `translateY(-${labelOffset * hiddenPercentage}px)`;
    }

    // Calculate what is the height of the hidden part of the arc
    _calcHiddenHeight(radius, hStrokeWidth) {
        const round = num => Math.round(num * 100) / 100; // Round with 2 decimal places

        radius += hStrokeWidth;
        const startAnglePoint = this._getAngleCoordinates(radius, +this.startAngle);
        const endAnglePoint = this._getAngleCoordinates(radius, +this.endAngle);
        const maxPoint = round(Math.max(startAnglePoint.y, endAnglePoint.y));

        // Start/End angles crossed the mid point of the circle
        if (maxPoint < 0) {
            radius -= hStrokeWidth;
            const percent = Math.min(Math.abs(maxPoint) / radius, 0.65); // How much is hidden from the upper side with a 65% limit
            return radius + (percent * radius) - (percent * hStrokeWidth);
        }

        return round(radius) - maxPoint;
    }

    // Returns the angle's coordinates on the circle
    _getAngleCoordinates(radius, angle) {
        /* eslint-disable no-shadow */

        // Convert an angle in degrees to radians
        const degreesToRadians = degrees => (degrees * Math.PI) / 180;

        // Convert an angle to Cartesian coordinates (x, y)
        const getCartesianCoords = (radius, angle) => {
            angle = degreesToRadians(angle);
            const x = radius * Math.cos(angle);
            const y = radius * Math.sin(angle);
            return {x, y};
        };

        // Rotate a point (x, y) by the given angle
        const rotatePoint = (point, angle) => {
            const {x, y} = point;
            angle = degreesToRadians(angle);
            const xRotated = x * Math.cos(angle) - y * Math.sin(angle);
            const yRotated = x * Math.sin(angle) + y * Math.cos(angle);
            return {x: xRotated, y: yRotated};
        };

        // Rotate the point by 90 degrees as the SVG circles
        return rotatePoint(getCartesianCoords(radius, angle), 90);
    }

    // Create an internal parsed stateFormat values array which is filtered & sorted
    _updateStatesValues() {
        if (!this.stateFormat) {
            this._statesValues = null;
            return;
        }

        const values = Object.values(this.stateFormat).map(state => {
            if (!state.comparator) {
                state.comparator = '<';
            }
            return state;
        });

        const defaultState = values.find(state => state.value === null || state.value === undefined); // Default value

        // Clear illegal values and then sort by value
        let statesValues = values.filter(state => isValidNum(state.value)).sort((a, b) => {
            // First, compare based on the 'value' property
            if (a.value !== b.value) {
                return a.value - b.value;
            }

            // If 'value' is equal, compare based on the 'comparator' property
            if (a.comparator === '<' && b.comparator === '<=') {
                return -1; // '<' goes first
            } else if (a.comparator === '<=' && b.comparator === '<') {
                return 1; // '<=' goes after
            }

            return 0; // no change if both 'value' and 'comparator' are equal
        });

        // Clear duplications (both value & comparator are equal)
        statesValues = statesValues.filter((state, i, arr) => {
            return i > 0 ? state.value !== arr[i - 1].value || state.comparator !== arr[i - 1].comparator : true;
        });

        // Add a default default state in case the target state is not reaching maxValue
        // The user might have set a default state with no assigned value
        if ((statesValues.length && statesValues[statesValues.length - 1].value < this.maxValue) || (!statesValues.length && defaultState)) {
            statesValues.push({
                default:        true,
                comparator:     '<=',
                name:           defaultState && defaultState.name ? defaultState.name : 'default',
                value:          Number.MAX_SAFE_INTEGER,
                color:          defaultState && defaultState.color ? defaultState.color : null,
                icon:           defaultState && defaultState.icon ? defaultState.icon : null,
                fontWeight:     defaultState && defaultState.fontWeight ? defaultState.fontWeight : null,
                fontStyle:      defaultState && defaultState.fontStyle ? defaultState.fontStyle : null,
                textDecoration: defaultState && defaultState.textDecoration ? defaultState.textDecoration : null
            });
        }

        this._statesValues = statesValues;
    }

    // Creates a ptcstate that will update the KPI's state according to the stateFormat values
    _updateStateUnit() {
        if (!this._statesValues || !this._hasValue) {
            if (this._stateUnit) {
                document.body.removeChild(this._stateUnit);
                this._stateUnit = null;
                this._icon = null;
            }
            return;
        }

        this._stateUnit = this._stateUnit || document.createElement('ptcs-state-unit');

        this._stateUnit.wc = `#${this.id}`;
        this._stateUnit.property = 'value';
        this._stateUnit.state = {
            name: 'value',
            func: () => {
                // eslint-disable-next-line no-shadow
                const state = this._statesValues.find(state => {
                    if (state.comparator && state.comparator === '<=') {
                        return this.value <= state.value;
                    }
                    return this.value < state.value;
                });

                this._icon = state && state.icon ? state.icon : null;
                requestAnimationFrame(() => this._setMaxIconSize());
                return state && state.name ? state.name : null;
            }
        };

        document.body.appendChild(this._stateUnit);
    }

    // Creates a style-unit with the relevant theming for each state in stateFormat
    _updateStyleUnit() {
        if (!this._statesValues || !this._hasValue) {
            if (this._styleUnit) {
                document.body.removeChild(this._styleUnit);
                this._styleUnit = null;
            }
            return;
        }

        this._styleUnit = this._styleUnit || document.createElement('ptcs-style-unit');

        this._styleUnit.wc = `#${this.id}.PTCS-KPI-DIAL`;
        this._styleUnit.textContent = this._statesValues.map(state => {
            return `
                :host([ptcstate-value=${state.name}]) [part~=value-tracker-filling] {
                    background: ${state.color} !important;
                }
                :host([ptcstate-value=${state.name}]) [part~=value-tracker-filling] {
                    stroke: ${state.color} !important;
                }
                :host([ptcstate-value=${state.name}]) [part~=icon] {
                    color: ${state.color} !important;
                }
                :host([ptcstate-value=${state.name}]) [part~=value] {
                    color: ${state.color} !important;
                    font-weight: ${state.fontWeight} !important;
                    font-style: ${state.fontStyle} !important;
                    text-decoration: ${state.textDecoration} !important;
                }
            `;
        }).join('');

        document.body.appendChild(this._styleUnit);
    }

    _insertValidationMessage(messageElement) {
        if (messageElement) {
            this.shadowRoot.appendChild(messageElement);
        }
    }

    _validateValue(minValidValue, maxValidValue, extraValidation, value) {
        const messages = [];

        // Don't show any validation when either value or min/max values are not valid
        if (this._stayUnvalidated || !this._hasValue) {
            return undefined;
        }

        // Value is less than minValidValue
        if (isValidNum(minValidValue) && value < minValidValue) {
            const msg = PTCS.replaceStringTokens(this.minValueFailureMessage, {value: minValidValue});
            messages.push(msg ? msg.join('. ') : false);
        }

        // Value is bigger than maxValidValue
        if (isValidNum(maxValidValue) && value > maxValidValue) {
            const msg = PTCS.replaceStringTokens(this.maxValueFailureMessage, {value: maxValidValue});
            messages.push(msg ? msg.join('. ') : false);
        }

        // At least one validation failed
        if (messages.length) {
            return messages;
        }

        // All standard validation has succeeded. Leave final say to the custom validation, if any
        return typeof extraValidation === 'function' ? extraValidation(this) : true;
    }
};

customElements.define(PTCS.KpiDial.is, PTCS.KpiDial);
