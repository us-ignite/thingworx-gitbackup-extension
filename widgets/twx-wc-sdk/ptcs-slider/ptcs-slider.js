import {LitElement, html, css, nothing} from 'lit';
import {styleMap} from 'lit/directives/style-map.js';
import {when} from 'lit/directives/when.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-icon/ptcs-icon.js';
import 'ptcs-icons/cds-icons.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';
import 'ptcs-behavior-validate/ptcs-behavior-validate.js';
import {updateTooltipInFocus, hoverTooltip, closeTooltip} from 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';


Math._sldrPercent = v => (100 * v).toFixed(2);

const hasChanged = (_new, _old) => (_new !== _old && _old !== undefined);
PTCS.Slider = class extends PTCS.BehaviorTabindex(PTCS.BehaviorValidate(PTCS.BehaviorFocus(
    PTCS.BehaviorStyleable(LitElement)))) {

    static get styles() {
        return css`
            :host {
                position: relative;
                display: inline-flex;
                flex-wrap: nowrap;
                align-items: center;
                align-content: center;
                justify-content: space-between;
                flex-direction: column;
                outline: none;
            }

            :host([dragging-thumb]) {
                user-select: none;
            }

            :host(:not([vertical]):not([_no-space-for-message])) {
                justify-content: center;
            }

            :host([vertical]) {
                justify-content: space-between;
            }

            [part=label] {
                width: 100%;
                flex-shrink: 0;
            }

            .grid {
                display: grid;
            }

            :host(:not([vertical])) .grid {
                width: 100%;
                grid-template-columns: auto 1fr 1fr auto;
                grid-template-rows: auto auto auto;
            }

            :host([vertical]) .grid {
                flex: 1 1 auto;
                grid-template-columns: auto auto auto;
                grid-template-rows: auto 1fr 1fr auto;
            }

            :host(:not([vertical])) [part=icon-min] {
                grid-column: 1;
                grid-row: 2;
                align-self: center;
            }

            :host(:not([vertical])[reverse-minmax]) [part=icon-min] {
                grid-column: 4;
            }

            :host(:not([vertical])) [part=icon-max] {
                grid-column: 4;
                grid-row: 2;
                align-self: center;
            }

            :host(:not([vertical])[reverse-minmax]) [part=icon-max] {
                grid-column: 1;
            }

            .value-container {
                position: relative;
            }

            :host(:not([vertical])) .value-container {
                grid-column: 2 / 4;
                grid-row: 1;
            }

            :host(:not([vertical])[reverse-labels]) .value-container {
                grid-row: 3;
            }

            :host([vertical]) .value-container {
                grid-column: 1;
                grid-row: 2 / 4;
            }

            :host([vertical][reverse-labels]) .value-container {
                grid-column: 3;
            }

            .value-container-inner {
                display: flex;
            }

            :host(:not([vertical])) .value-container-inner {
                width: 100%;
            }

            :host([vertical]) .value-container-inner {
                height: 100%;
            }

            :host(:not([vertical]):not([reverse-minmax])) .value-container-inner {
                flex-direction: row;
            }

            :host(:not([vertical])[reverse-minmax]) .value-container-inner {
                flex-direction: row-reverse;
            }

            :host([vertical]:not([reverse-minmax])) .value-container-inner {
                flex-direction: column;
            }

            :host([vertical][reverse-minmax]) .value-container-inner {
                flex-direction: column-reverse;
            }

            :host(:not([dragging-thumb])) .value-container[show-value=drag]
            {
                visibility: hidden;
            }

            :host(:not([vertical])) [part=slider-container] {
                grid-column: 2 / 4;
                grid-row: 2;
                align-self: center;
            }

            :host(:not([vertical])) .min-label-cntr {
                grid-column: 2;
                grid-row: 3;
            }

            :host(:not([vertical])[reverse-minmax]) .min-label-cntr {
                grid-column: 3;
                text-align: right;
            }

            :host(:not([vertical])[reverse-labels]) .min-label-cntr {
                grid-row: 1;
            }

            :host(:not([vertical])) .max-label-cntr {
                grid-column: 3;
                grid-row: 3;
                text-align: right;
            }

            :host(:not([vertical])[reverse-minmax]) .max-label-cntr {
                grid-column: 2;
                text-align: left;
            }

            :host(:not([vertical])[reverse-labels]) .max-label-cntr {
                grid-row: 1;
            }

            :host([vertical]) [part=icon-min] {
                grid-column: 2;
                grid-row: 1;
                justify-self: center;
            }

            :host([vertical][reverse-minmax]) [part=icon-min] {
                grid-row: 4;
            }

            :host([vertical]) [part=icon-max] {
                grid-column: 2;
                grid-row: 4;
                justify-self: center;
            }

            :host([vertical][reverse-minmax]) [part=icon-max] {
                grid-row: 1;
            }

            :host([vertical]) [part=slider-container] {
                grid-column: 2;
                grid-row: 2 / 4;
                justify-self: center;
            }

            :host([vertical]) .min-label-cntr {
                grid-column: 3;
                grid-row: 2;
            }

            :host([vertical][reverse-minmax]) .min-label-cntr {
                grid-row: 3;
                align-self: end;
            }

            :host([vertical][reverse-labels]) .min-label-cntr {
                grid-column: 1;
            }

            :host([vertical]) .max-label-cntr {
                grid-column: 3;
                grid-row: 3;
                align-self: end;
            }

            :host([vertical][reverse-minmax]) .max-label-cntr {
                grid-row: 2;
                align-self: start;
            }

            :host([vertical][reverse-labels]) .max-label-cntr {
                grid-column: 1;
            }

            [part~=value] {
                display: flex;
                justify-content: center;
                align-items: center;
            }

            [part~=value][editing] .read {
                display: none;
            }

            .write {
                outline: none; /* Handled by theming */
                border: none;
                text-align: center;
                padding: 8px;
            }

            [part~=value]:not([editing]) .write {
                display: none;
            }

            [part~=value] input {
                width: calc(100% - 0.5em);
                max-width: 4em;
            }

            [part=slider-container] {
                display: inline-block;
                flex: 1 1 auto;
                position: relative;
            }

            :host(:not([vertical])) [part=slider-container] {
                width: 100%;
            }

            :host([vertical]) [part=slider-container] {
                height: 100%;
            }

            [part~=thumb] {
                position: absolute;
                box-sizing: border-box;
                fill: currentColor;

                /* Remove default padding in ptcs-icon */
                padding: 0px;
            }

            [part~=thumb1][z-top]:not(:focus) {
                z-index: 15;
            }

            [part~=thumb]:focus {
                z-index: 16;
            }

            [part=track] {
                position: absolute;
                overflow: hidden;
                box-sizing: border-box;

                display: flex;
                flex-wrap: nowrap;
                justify-content: space-between;
                align-items: stretch;
                align-content: stretch;
            }

            :host(:not([vertical])) [part=track] {
                flex-direction: row;
            }

            :host([vertical]) [part=track] {
                flex-direction: column;
            }

            :host(:not([vertical])[reverse-minmax]) [part=track] {
                flex-direction: row-reverse;
            }

            :host([vertical][reverse-minmax]) [part=track] {
                flex-direction: column-reverse;
            }

            [part=track-between] {
                flex-grow: 1;
            }

            [part=track-before], [part=track-after] {
                flex: 0 0 auto;
            }

            :host(:not([vertical])) [part=track-before],
            :host(:not([vertical])) [part=track-between],
            :host(:not([vertical])) [part=track-after] {
                height: 100%;
            }

            :host([vertical]) [part=track-before],
            :host([vertical]) [part=track-between],
            :host([vertical]) [part=track-after] {
                width: 100%;
            }

            /* Arrow on value container */
            [part~=value] {
                position: relative;
            }

            [part~=value-arrow] {
                position: absolute;
                border: 2px solid transparent;
            }

            :host(:not([vertical]):not([reverse-labels])) [part~=value-arrow] {
                bottom: -5px;
                left: calc(50% - 2px);
                border-top-color: currentColor;
            }

            :host(:not([vertical])[reverse-labels]) [part~=value-arrow] {
                top: -5px;
                left: calc(50% - 2px);
                border-bottom-color: currentColor;
            }

            :host([vertical]:not([reverse-labels])) [part~=value-arrow] {
                top: calc(50% - 2px);
                right: -5px;
                border-left-color: currentColor;
            }

            :host([vertical][reverse-labels]) [part~=value-arrow] {
                top: calc(50% - 2px);
                left: -5px;
                border-right-color: currentColor;
            }
        `;
    }

    render() {
        const _sliderContainerStyle = this._getSliderContainerStyle();
        const _minLabelCntrStyle = this._getMinmaxLabelPadding(true);
        const _maxLabelCntrStyle = this._getMinmaxLabelPadding();
        const _valueStyle = this._getValueWidth();
        const _value2Style = this._getValueWidth(true);
        const _valueSep1Style = this._getValueSep1Style();
        const _valueSep2Style = this._getValueSep2Style();
        const _valueSep3Style = this._getValueSep3Style();
        const _thumb1Style = this._getThumbStyle();
        const _thumb2Style = this._getThumbStyle(true);
        const _thumbWidthHeight = `${this.thumbSize > 0 ? this.thumbSize : 44}px`;
        const _trackStyle = this._getTrackStyle();
        const _trackBeforeStyle = this._getTrackBeforeStyle();
        const _trackAfterStyle = this._getTrackAfterStyle();

        return html`
            ${when(this._show(this.label), () => html`<ptcs-label part="label" .label=${this.label} .horizontalAlignment=${this.labelAlignment}
                variant=${this.labelVariant} multi-line></ptcs-label>`)}
            <div class="grid">
            ${when(this._show(this.minIcon), () => html`<ptcs-icon part="icon-min" .iconSet=${this.iconSet} .icon=${this.minIcon}
                .size=${this.minIconSize}></ptcs-icon>`)}
            ${when(this._attrShowValue(this.showValue), () => html`<div class="value-container"
                show-value=${this._attrShowValue(this.showValue) || nothing}>
                <div class="value-container-inner">
                <div id="value-sep1" style=${styleMap(_valueSep1Style)}></div>
                <div part="value1 value" id="value1" @click=${this._editValue1} ?editing=${this._edit1} style=${styleMap(_valueStyle)}
                    @mouseenter=${this._mouseTooltip} @mouseleave=${this._mouseleaveValue}>
                <div part="value-arrow" ?editing=${this._edit1}></div>
                <span class="read">${this._prec(this._value, this.precision)}</span>
                ${when(this.editValue, () => html`<input class="write" id="input1" type="text" @change=${this._onStopEdit1}
                    @blur=${this._onStopEdit1}>`)}
                </div>
                <div id="value-sep2" style=${styleMap(_valueSep2Style)}></div>
                ${when(this.range, () => html`<div part="value2 value" id="value2" @click=${this._editValue2} ?editing=${this._edit2}
                    style=${styleMap(_value2Style)} @mouseenter=${this._mouseTooltip} @mouseleave=${this._mouseleaveValue}>
                <div part="value-arrow" ?editing=${this._edit2}></div>
                <span class="read">${this._prec(this._value2, this.precision)}</span>
                ${when(this.editValue, () => html`<input class="write" id="input2" type="text" @change=${this._onStopEdit2}
                    @blur=${this._onStopEdit2}>`)}
                </div>`)}
                <div id="value-sep3" style=${styleMap(_valueSep3Style)}></div>
                </div>
                </div>`)}
            <div part="slider-container" id="slider-container" style=${styleMap(_sliderContainerStyle)}>
            <div part="track" id="track" style=${styleMap(_trackStyle)}>
            <div part="track-before" id="track-before" style=${styleMap(_trackBeforeStyle)} @click=${this._clickTrack1}></div>
            ${when(this.range && !this.rangeCollapsed, () => html`<div part="track-between" id="track-between" @click=${this._clickTrack2}></div>`)}
            <div part="track-after" id="track-after" style=${styleMap(_trackAfterStyle)} @click=${this._clickTrack3}></div>
            </div>
            <ptcs-icon part="thumb thumb1" id="thumb" size="custom" tabindex=${this._dfTabindex(this._delegatedFocus, this._noSpaceForMessage)}
                ?z-top=${this._thumb1AtTop} .iconSet=${this._iconSet(this.iconSet, this.thumbIcon)}
                .icon=${this._thumb1Icon(this.vertical, this.reverseMinmax, this.thumbIcon)}
                .tooltip=${this.thumbTooltip} .tooltipIcon=${this.thumbTooltipIcon}
                .tooltipPos=${this._thumbPos1(this.reverseLabels, this.reverseMinmax)}
                style=${styleMap(_thumb1Style)} .iconWidth = ${_thumbWidthHeight} .iconHeight = ${_thumbWidthHeight}
                @mousedown=${this._mousedownThumb1} @touchstart=${this._touchstartThumb1} @keydown=${this._keydownThumb1}
                @dragstart=${this._ondragstart}></ptcs-icon>
            ${when(this.range, () => html`<ptcs-icon part="thumb thumb2" id="thumb2" size="custom"
                tabindex=${this._dfTabindex(this._delegatedFocus, this._noSpaceForMessage)}
                .iconSet=${this._iconSet(this.iconSet, this.thumb2Icon, this.thumbIcon)}
                .icon=${this._thumb2Icon(this.vertical, this.reverseMinmax, this.thumb2Icon, this.thumbIcon)}
                .tooltip=${this.thumb2Tooltip} .tooltipIcon=${this.thumb2TooltipIcon}
                .tooltipPos=${this._thumbPos2(this.reverseLabels, this.reverseMinmax)}
                style=${styleMap(_thumb2Style)} .iconWidth = ${_thumbWidthHeight} .iconHeight = ${_thumbWidthHeight}
                @mousedown=${this._mousedownThumb2} @touchstart=${this._touchstartThumb2} @keydown=${this._keydownThumb2}
                @dragstart=${this._ondragstart}></ptcs-icon>`)}
            </div>
            ${when(this._showLabel(this.minLabel), () => html`<div class="min-label-cntr" style=${styleMap(_minLabelCntrStyle)}>
            <div part="min-label min-max-label">${this.minLabel}</div>
                </div>`)}
            ${when(this._showLabel(this.maxLabel), () => html`<div class="max-label-cntr" style=${styleMap(_maxLabelCntrStyle)}>
            <div part="max-label min-max-label">${this.maxLabel}</div>
                </div>`)}
            ${when(this._show(this.maxIcon), () => html`<ptcs-icon part="icon-max" .iconSet=${this.iconSet} .icon=${this.maxIcon}
                .size=${this.maxIconSize}></ptcs-icon>`)}
            </div>
        `;
    }

    static get is() {
        return 'ptcs-slider';
    }

    static get properties() {
        return {
            variant: {
                type:    String,
                reflect: true
            },

            label: {
                type: String
            },

            labelVariant: {
                type:      String,
                attribute: 'label-variant'
            },

            // left, center, right. left is default
            labelAlignment: {
                type:      String,
                attribute: 'label-alignment'
            },

            value: {
                type: String,
                hasChanged
            },

            _value: {
                type: Number
            },

            _thumb1AtTop: {
                type:  Boolean,
                state: true
            },

            value2: {
                type: String,
                hasChanged
            },

            _value2: {
                type: Number
            },

            minValueWidth: {
                type:      Number,
                attribute: 'min-value-width'
            },

            maxValueWidth: {
                type:      Number,
                attribute: 'max-value-width'
            },

            // Displayed current value(s)?
            showValue: {
                type:      String, // yes, true, false, no, <undefined>, drag (= only when dragging)
                attribute: 'show-value'
            },

            // Can displayed value be edited?
            editValue: {
                type:      Boolean,
                attribute: 'edit-value'
            },

            // Is value1 beeing edited?
            _edit1: {
                type:  Boolean,
                state: true
            },

            // Is value2 beeing edited?
            _edit2: {
                type:  Boolean,
                state: true
            },

            minValue: {
                type:      String,
                attribute: 'min-value'
            },

            // = Number(minValue), if minValue is a valid Number
            _minValue: {
                type:  Number,
                state: true
            },

            maxValue: {
                type:      String,
                attribute: 'max-value'
            },

            // = Number(maxValue), if maxValue is a valid Number
            _maxValue: {
                type:  Number,
                state: true
            },

            minValidValue: {
                type:      Number,
                attribute: 'min-valid-value',
                isValue:   minValidValue => !!minValidValue && !isNaN(minValidValue)
            },

            minValueFailureMessage: {
                type:      String,
                attribute: 'min-value-failure-message'
            },

            maxValidValue: {
                type:      Number,
                attribute: 'max-valid-value',
                isValue:   maxValidValue => !!maxValidValue && !isNaN(maxValidValue)
            },

            maxValueFailureMessage: {
                type:      String,
                attribute: 'max-value-failure-message'
            },

            // Client-provided custom validation function
            extraValidation: {
                type:      Function,
                attribute: 'extra-validation'
            },

            numStep: {
                type:      Number,
                attribute: 'num-step'
            },

            sizeStep: {
                type:      Number,
                attribute: 'size-step'
            },

            precision: {
                type: Number
            },

            _stepUnit: {
                type:  Number,
                state: true
            },

            minLabel: {
                type:      String,
                attribute: 'min-label'
            },

            maxLabel: {
                type:      String,
                attribute: 'max-label'
            },

            showMinMaxLabels: {
                type:      Boolean,
                attribute: 'show-min-max-labels'
            },

            hideMinMaxLabels: {
                type:      Boolean,
                attribute: 'hide-min-max-labels'
            },

            vertical: {
                type:    Boolean,
                reflect: true
            },

            disabled: {
                type:    Boolean,
                reflect: true
            },

            range: {
                type:    Boolean,
                reflect: true
            },

            rangeCollapsed: {
                type:      Boolean,
                attribute: 'range-collapsed',
                reflect:   true
            },

            overlapThumbs: {
                type:      Boolean,
                attribute: 'overlap-thumbs'
            },

            thumbSize: {
                type:      Number,
                attribute: 'thumb-size'
            },

            trackSize: {
                type:      Number,
                attribute: 'track-size'
            },

            fullTrack: {
                type:      Boolean,
                attribute: 'full-track'
            },

            trackPlacement: { // center, start, end, (horizontal) {top=start, bottom=end}, (vertical){left=start, right=end}
                type:      String,
                attribute: 'track-placement'
            },

            iconSet: {
                type:      String,
                attribute: 'icon-set'
            },

            thumbIcon: {
                type:      String,
                attribute: 'thumb-icon'
            },

            thumbTooltip: {
                type:      String,
                attribute: 'thumb-tooltip'
            },

            thumbTooltipIcon: {
                type:      String,
                attribute: 'thumb-tooltip-icon'
            },

            thumb2Icon: {
                type:      String,
                attribute: 'thumb2-icon'
            },

            thumb2Tooltip: {
                type:      String,
                attribute: 'thumb2-tooltip'
            },

            thumb2TooltipIcon: {
                type:      String,
                attribute: 'thumb2-tooltip-icon'
            },

            minIcon: {
                type:      String,
                attribute: 'min-icon'
            },

            minIconSize: {
                type:      String,
                attribute: 'min-icon-size'
            },

            maxIcon: {
                type:      String,
                attribute: 'max-icon'
            },

            maxIconSize: {
                type:      String,
                attribute: 'max-icon-size'
            },

            reverseMinmax: {
                type:      Boolean,
                attribute: 'reverse-minmax',
                reflect:   true
            },

            reverseLabels: {
                type:      Boolean,
                attribute: 'reverse-labels',
                reflect:   true
            },

            draggingThumb: {
                type:      Boolean,
                attribute: 'dragging-thumb',
                reflect:   true
            },

            // How long is single step? (arrow key navigation)
            step: {
                type: Number
            },

            _delegatedFocus: {
                type: String
            },

            /* ARIA */
            role: {
                type:    String,
                reflect: true
            },

            ariaValuenow: {
                type:      String,
                attribute: 'aria-valuenow',
                reflect:   true,
                validate:  '_validateSlider(minValidValue, maxValidValue, extraValidation)'
            },

            ariaValuemin: {
                type:      String,
                attribute: 'aria-valuemin',
                reflect:   true
            },

            ariaValuemax: {
                type:      String,
                attribute: 'aria-valuemax',
                reflect:   true
            },

            ariaOrientation: {
                type:      String,
                attribute: 'aria-orientation',
                reflect:   true
            },

            ariaDisabled: {
                type:      String,
                attribute: 'aria-disabled',
                reflect:   true
            },

            ariaLabel: {
                type:      String,
                attribute: 'aria-label',
                reflect:   true
            },

            _noSpaceForMessage: {
                type:      Boolean,
                attribute: '_no-space-for-message',
                reflect:   true
            }
        };
    }

    constructor() {
        super();
        this.label = '';
        this.labelVariant = 'label';
        this.value = '0';
        this.value2 = '100';
        this.minValue = '0';
        this.maxValue = '100';
        this.precision = 0;
        this.vertical = false;
        this.disabled = false;
        this.range = false;
        this.thumbSize = 44;
        this.trackSize = 20;
        this.thumbIcon = null;
        this.thumb2Icon = null;
        this.minIcon = null;
        this.maxIcon = null;
        this.reverseMinmax = false;
        this._delegatedFocus = null;
        this._showMinMaxLabels = true;

        // Unless otherwise noted, a slider should not start validation until the user moves the thumb
        if (this._stayUnvalidated === undefined) {
            this._stayUnvalidated = true;
        }
    }

    firstUpdated() {
        super.firstUpdated();

        // The focus manager should only track the focus of the thumbs, thumb2 will be handled dynamically
        this._trackFocus(this.shadowRoot.getElementById('thumb'));
        this.shadowRoot.getElementById('slider-container').addEventListener('click', this._beginValidate.bind(this), {once: true});
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (changedProperties.has('minValue')) {
            const v = Number(this.minValue);
            if (!isNaN(v)) {
                this._minValue = v;
            }
        }
        if (changedProperties.has('maxValue')) {
            const v = Number(this.maxValue);
            if (!isNaN(v)) {
                this._maxValue = v;
            }
        }
        if (['numStep', 'sizeStep', '_minValue', '_maxValue', 'precision'].some(propName => changedProperties.has(propName))) {
            this._stepUnit = this._computeStepUnit();
        }
        if (['_minValue', '_maxValue', '_stepUnit', 'precision'].some(propName => changedProperties.has(propName))) {
            this._computeValue();// updating _value, value2
            this._computeValue2();// updating _value2, value, _value
        }
        if (['_value', '_value2'].some(propName => changedProperties.has(propName))) {
            this.rangeCollapsed = this._value === this._value2;
        }
        if (['_value', '_value2', '_minValue', '_maxValue', 'thumbSize',
            'range', 'overlapThumbs', 'vertical', 'reverseMinmax', 'trackPlacement'].some(propName => changedProperties.has(propName))) {
            this._thumb1AtTop = !this.draggingThumb && !changedProperties.has('_value2');
        }
        if (changedProperties.has('_delegatedFocus')) {
            this.role = (this._delegatedFocus !== false && this._delegatedFocus !== undefined) ? 'slider' : false;
        }
        if (['_value', '_value2', 'precision', 'range'].some(propName => changedProperties.has(propName))) {
            this.ariaValuenow = this.range
                ? `${this._prec(this._value, this.precision)} to ${this._prec(this._value2, this.precision)}`
                : this._prec(this._value, this.precision);
        }
        if (['_minValue', 'precision'].some(propName => changedProperties.has(propName))) {
            this.ariaValuemin = this._prec(this._minValue, this.precision);
        }
        if (['_maxValue', 'precision'].some(propName => changedProperties.has(propName))) {
            this.ariaValuemax = this._prec(this._maxValue, this.precision);
        }
        if (changedProperties.has('vertical')) {
            this.ariaOrientation = this.vertical ? 'vertical' : false;
        }
        if (changedProperties.has('label')) {
            this.ariaLabel = this.label ? this.label : false;
        }
        if (changedProperties.has('disabled')) {
            this.ariaDisabled = this.disabled;
        }

        // updates The focus manager removing thumb2
        if (changedProperties.has('range') && changedProperties.get('range') !== undefined && this.range === false) {
            this._untrackFocus(this.shadowRoot.getElementById('thumb2'));
        }
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        // notify enents
        if (changedProperties.has('value')) {
            this.dispatchEvent(new CustomEvent('value-changed', {
                detail: {
                    value: this.value
                }
            }));
        }
        if (changedProperties.has('value2')) {
            this.dispatchEvent(new CustomEvent('value2-changed', {
                detail: {
                    value: this.value2
                }
            }));
        }

        // updating tooltips & events
        if (['_value', '_minValue', '_maxValue', 'thumbSize',
            'range', 'overlapThumbs', 'vertical', 'reverseMinmax', 'trackPlacement'].some(propName => changedProperties.has(propName))) {
            this._updateTooltip();
        }
        if (['_value2', '_minValue', '_maxValue', 'thumbSize',
            'overlapThumbs', 'vertical', 'reverseMinmax', 'trackPlacement'].some(propName => changedProperties.has(propName)) && this.range) {
            this._updateTooltip2();
        }
        if (changedProperties.has('_edit1') && this._edit1) {
            const el = this.shadowRoot.getElementById('input1');
            el.select();
            el.focus();
        }
        if (changedProperties.has('_edit2') && this._edit2) {
            const el = this.shadowRoot.getElementById('input2');
            el.select();
            el.focus();
        }

        // updates The focus manager addinging thumb2
        if (changedProperties.has('range') && this.range === true) {
            this._trackFocus(this.shadowRoot.getElementById('thumb2'));
        }
    }

    set value(v) {
        if (this.__$value === v || isNaN(v) || v === null) {
            return;
        }
        this.__value$ = v;
        this._computeValue();
    }

    get value() {
        return this.__value$;
    }

    set value2(v) {
        if (this.__value2$ === v || isNaN(v) || v === null) {
            return;
        }
        this.__value2$ = v;
        this._computeValue2();
    }

    get value2() {
        return this.__value2$;
    }

    _beginValidate() {
        this._stayUnvalidated = false;
    }

    _mousedownThumb1(ev) {
        this._stayUnvalidated = true;
        this._mouseDn1(ev);
        document.addEventListener('mouseup', this._beginValidate.bind(this), {once: true});
    }

    _touchstartThumb1(ev) {
        this._stayUnvalidated = true;
        this._mouseDn1(ev, true);
        document.addEventListener('touchend', this._beginValidate.bind(this), {once: true});
    }

    _keydownThumb1(ev) {
        this._stayUnvalidated = true;
        this._keyDn1(ev);
        document.addEventListener('keyup', this._beginValidate.bind(this), {once: true});
    }

    _mousedownThumb2(ev) {
        this._stayUnvalidated = true;
        this._mouseDn2(ev);
        document.addEventListener('mouseup', this._beginValidate.bind(this), {once: true});
    }

    _touchstartThumb2(ev) {
        this._stayUnvalidated = true;
        this._mouseDn2(ev, true);
        document.addEventListener('touchend', this._beginValidate.bind(this), {once: true});
    }

    _keydownThumb2(ev) {
        this._stayUnvalidated = true;
        this._keyDn2(ev);
        document.addEventListener('keyup', this._beginValidate.bind(this), {once: true});
    }

    _mouseleaveValue() {
        requestAnimationFrame(closeTooltip);
    }

    _ondragstart() {
        return false;
    }

    _show(prop) {
        return prop;
    }

    _showLabel(label) {
        return label && this._showMinMaxLabels;
    }

    _prec(value, precision) {
        if (!(precision > 0)) {
            precision = 0;
        }
        return (typeof value === 'number' && value.toFixed) ? value.toFixed(precision) : value;
    }

    _iconSet(iconSet, thumbIcon, thumbIcon2) {
        const thumb = thumbIcon || thumbIcon2;
        if (typeof thumb !== 'string' || thumb[0] !== '#') {
            return iconSet;
        }
        switch (thumb) {
            case '#circle':
            case '#hexagon':
            case '#split':
                return undefined;
        }
        return iconSet;
    }

    _thumb1Icon(vertical, reverseMinmax, thumbIcon) {
        switch (thumbIcon) {
            case '#circle':
                return 'cds:icon_thumb_circle';
            case '#hexagon':
                return 'cds:icon_thumb_hexagon';
            case '#split':
                if (vertical) {
                    return reverseMinmax ? 'cds:icon_thumb_split_bottom' : 'cds:icon_thumb_split_top';
                }
                return reverseMinmax ? 'cds:icon_thumb_split_right' : 'cds:icon_thumb_split_left';
        }
        return thumbIcon;
    }

    _thumb2Icon(vertical, reverseMinmax, thumbIcon, thumbIconAlt) {
        switch (thumbIcon) {
            case '#circle':
                return 'cds:icon_thumb_circle';
            case '#hexagon':
                return 'cds:icon_thumb_hexagon';
        }

        if ((thumbIcon || thumbIconAlt) === '#split') {
            if (vertical) {
                return reverseMinmax ? 'cds:icon_thumb_split_top' : 'cds:icon_thumb_split_bottom';
            }
            return reverseMinmax ? 'cds:icon_thumb_split_left' : 'cds:icon_thumb_split_right';
        }

        return thumbIcon;
    }

    _thumbPos1(reverseLabels, reverseMinmax) {
        if (reverseMinmax) {
            return reverseLabels ? 'tr tl' : 'br bl';
        }
        return reverseLabels ? 'tl tr' : 'bl br';
    }

    _thumbPos2(reverseLabels, reverseMinmax) {
        if (reverseMinmax) {
            return reverseLabels ? 'tl tr' : 'bl br';
        }
        return reverseLabels ? 'tr tl' : 'br bl';
    }

    _v2s(v) {
        return this._prec(v, this.precision);
    }

    _oneStep() {
        const minStep = this._stepUnit ? this._stepUnit : 1;
        return Number(this.step > minStep ? this.step : minStep);
    }

    _onePage() {
        return 5 * this._oneStep();
    }

    _computeStepUnit() {
        if (this.numStep >= 1 && this._maxValue > this._minValue) {
            return (this._maxValue - this._minValue) / this.numStep;
        }
        if (this.sizeStep > 0) {
            return this.sizeStep;
        }
        if (this.precision > 0) {
            return Math.pow(10, -this.precision);
        }

        return 1;
    }

    _attrShowValue(showValue) {
        switch (showValue) {
            case 'yes': case 'true': case '':
                return 'yes';
            case 'drag':
                return 'drag';
        }

        return false;
    }

    _getMinmaxLabelPadding(isMinLabel) {
        if (this.fullTrack) {
            return {};
        }
        const v = `${this.thumbSize / 2}px`;
        const _reverseMinmax = isMinLabel ? this.reverseMinmax : !this.reverseMinmax;
        const paddingTop = this.vertical && !_reverseMinmax ? v : '';
        const paddingBottom = this.vertical && _reverseMinmax ? v : '';
        const paddingLeft = !this.vertical && !_reverseMinmax ? v : '';
        const paddingRight = !this.vertical && _reverseMinmax ? v : '';
        return {paddingTop, paddingBottom, paddingLeft, paddingRight};
    }


    // slider-container width, height style
    _getSliderContainerStyle() {
        const size = `${Math.max(this.thumbSize, this.trackSize)}px`;
        return {
            width:  this.vertical ? size : '',
            height: this.vertical ? '' : size
        };
    }

    _getThumbStyle(isValue2) {
        const v = ((isValue2 ? this._value2 : this._value) - this._minValue) / (this._maxValue - this._minValue);
        const numThumbs = ((isValue2 || !isValue2 && this.range) && !this.overlapThumbs) ? 2 : 1;
        const adjustThumb1 = (!isValue2 || this.overlapThumbs) ? 0 : 1;
        const offsv = `calc(${Math._sldrPercent(v)}% - ${Math.floor((v * numThumbs - adjustThumb1) * this.thumbSize)}px)`;
        const center = `calc(50% - ${this.thumbSize / 2}px)`;
        let top, bottom, left, right;
        if (this.vertical) {
            if (this.reverseMinmax) {
                top = '';
                bottom = offsv;
            } else {
                top = offsv;
                bottom = '';
            }
            switch (this.trackPlacement) {
                case 'start': case 'left':
                    left = '0px';
                    right = '';
                    break;

                case 'end': case 'right':
                    left = '';
                    right = '0px';
                    break;

                default:
                    left = center;
                    right = '';
            }
        } else {
            if (this.reverseMinmax) {
                left = '';
                right = offsv;
            } else {
                left = offsv;
                right = '';
            }
            switch (this.trackPlacement) {
                case 'start': case 'left':
                    top = '0px';
                    bottom = '';
                    break;

                case 'end': case 'right':
                    top = '';
                    bottom = '0px';
                    break;

                default:
                    top = center;
                    bottom = '';
            }
        }
        return {top, bottom, left, right};
    }

    __compute(value) {
        // Make sure max and min values always can be selected
        if (!isNaN(this.minValue) && value <= this.minValue) {
            return Number(this.minValue);
        }
        if (!isNaN(this.maxValue) && value >= this.maxValue) {
            return Number(this.maxValue);
        }
        // Normalize value according to settings
        if (this._stepUnit) {
            value = Math.round(value / this._stepUnit) * this._stepUnit;
        }
        if (!(this.precision > 0)) {
            value = Math.round(value);
        }
        if (!isNaN(this.minValue)) {
            value = Math.max(value, this.minValue);
        }
        if (!isNaN(this.maxValue)) {
            value = Math.min(value, this.maxValue);
        }
        return value;
    }

    _computeValue() {
        const v = Number(this.value);
        if (isNaN(v)) {
            // Ignore non numeric values
            return;
        }
        this._value = this.__compute(v);
        if (this._value2 < this._value) {
            this.value2 = this._v2s(this._value);
        }
    }

    _computeValue2() {
        const v = Number(this.value2);
        if (isNaN(v)) {
            // Ignore non numeric values
            return;
        }
        this._value2 = this.__compute(v);
        if (this._value > this._value2) {
            this.value = this._v2s(this._value2);
            this._value = this._value2;
        }
    }

    _getValueSep1Style() {
        const _sepValue = (this._value - this._minValue) / (this._maxValue - this._minValue);

        // Very temporary fix
        let _thumbSize = this.thumbSize;
        if (_thumbSize < 30) {
            _thumbSize = 30;
        }

        const style = this.range && this.overlapThumbs
            ? `calc(${Math._sldrPercent(_sepValue)}%)`
            : `calc(${Math._sldrPercent(_sepValue)}% - ${_sepValue * _thumbSize / 2}px)`;

        const height = this.vertical ? style : '';
        const width = this.vertical ? '' : style;
        return {height, width};
    }

    _getValueSep2Style() {
        if (!this.range) {
            return {};
        }

        const _sepValue = (this._value2 - this._value) / (this._maxValue - this._minValue);

        // Very temporary fix
        let _thumbSize = this.thumbSize;
        if (_thumbSize < 30) {
            _thumbSize = 30;
        }

        const style = `calc(${Math._sldrPercent(_sepValue)}% - ${(this.overlapThumbs ? 1 : _sepValue) * _thumbSize}px)`;

        const height = this.vertical ? style : '';
        const width = this.vertical ? '' : style;
        return {height, width};
    }

    _getValueSep3Style() {
        const v1 = (this._value - this._minValue) / (this._maxValue - this._minValue);
        const v2 = (this._value2 - this._minValue) / (this._maxValue - this._minValue);
        const _sepValue = 1 - (this.range ? v2 : v1);

        // Very temporary fix
        let _thumbSize = this.thumbSize;
        if (_thumbSize < 30) {
            _thumbSize = 30;
        }

        const style = this.range && this.overlapThumbs
            ? `calc(${Math._sldrPercent(_sepValue)}%)`
            : `calc(${Math._sldrPercent(_sepValue)}% - ${_sepValue * _thumbSize / 2}px)`;

        const height = this.vertical ? style : '';
        const width = this.vertical ? '' : style;
        return {height, width};
    }

    _getTrackBeforeStyle() {
        const _value = (this._value - this._minValue) / (this._maxValue - this._minValue);
        let length;
        if (this.range && !this.rangeCollapsed) {
            if (this.overlapThumbs) {
                length = `${100 * _value}%`;
            } else {
                length = `calc(${100 * _value}% - ${_value * this.thumbSize}px)`;
            }
        } else {
            length = `${100 * _value}%`;
        }
        return {width: !this.vertical ? length : '', height: this.vertical ? length : ''};
    }

    _getTrackAfterStyle() {
        const _value1 = (this._value - this._minValue) / (this._maxValue - this._minValue);
        let length;
        if (this.range && !this.rangeCollapsed) {
            const _value2 = (this._value2 - this._minValue) / (this._maxValue - this._minValue);
            if (this.overlapThumbs) {
                length = `${100 * (1 - _value2)}%`;
            } else {
                length = `calc(${100 * (1 - _value2)}% - ${(1 - _value2) * this.thumbSize}px)`;
            }
        } else {
            length = `${100 * (1 - _value1)}%`;
        }
        return {width: !this.vertical ? length : '', height: this.vertical ? length : ''};
    }

    _getTrackStyle() {
        let top, bottom, left, right, width, height;
        if (this.vertical) {
            width = `${this.trackSize}px`;
            height = '';
            top = bottom = this.fullTrack ? '0px' : `${this.thumbSize / 2}px`;

            switch (this.trackPlacement) {
                case 'start': case 'left':
                    left = '0px';
                    right = '';
                    break;

                case 'end': case 'right':
                    left = '';
                    right = '0px';
                    break;

                default:
                    left = `calc(50% - ${this.trackSize / 2}px)`;
                    right = '';
            }
        } else {
            width = '';
            height = `${this.trackSize}px`;
            left = right = this.fullTrack ? '0px' : `${this.thumbSize / 2}px`;

            switch (this.trackPlacement) {
                case 'start': case 'top':
                    top = '0px';
                    bottom = '';
                    break;

                case 'end': case 'bottom':
                    top = '';
                    bottom = '0px';
                    break;

                default:
                    top = `calc(50% - ${this.trackSize / 2}px)`;
                    bottom = '';
            }
        }
        return {top, bottom, left, right, width, height};
    }

    __getPosFromEvent(ev) {
        let x, y;
        if (ev.clientX && ev.clientY) {
            x = ev.clientX;
            y = ev.clientY;
        } else if (ev.targetTouches) {
            x = ev.targetTouches[0].clientX;
            y = ev.targetTouches[0].clientY;
            ev.preventDefault();
        }
        return {x, y};
    }

    _mouseToValue1(ev, hit) {
        const pos = this.__getPosFromEvent(ev);
        const r = this.shadowRoot.getElementById('slider-container').getBoundingClientRect();
        const thumbAdjust = this.range && !this.overlapThumbs ? this.thumbSize : 0;
        let value;
        if (this.vertical) {
            const y = this.reverseMinmax ? r.bottom - pos.y : pos.y - r.top;
            const h = r.height - thumbAdjust;
            const _d = hit ? hit.y : this.thumbSize / 2;
            const d = this.reverseMinmax ? this.thumbSize - _d : _d;
            value = (y - d) * (this._maxValue - this._minValue) / (h - this.thumbSize) + this._minValue;
        } else {
            const x = this.reverseMinmax ? r.right - pos.x : pos.x - r.left;
            const w = r.width - thumbAdjust;
            const _d = hit ? hit.x : this.thumbSize / 2;
            const d = this.reverseMinmax ? this.thumbSize - _d : _d;
            value = (x - d) * (this._maxValue - this._minValue) / (w - this.thumbSize) + this._minValue;
        }
        return this.__compute(value);
    }

    _mouseToValue2(ev, hit) {
        const pos = this.__getPosFromEvent(ev);
        const r = this.shadowRoot.getElementById('slider-container').getBoundingClientRect();
        const thumbAdjust = this.overlapThumbs ? 0 : Number(this.thumbSize);
        let value;
        if (this.vertical) {
            const y = (this.reverseMinmax ? r.bottom - pos.y : pos.y - r.top) - thumbAdjust;
            const h = r.height - thumbAdjust;
            const _d = hit ? hit.y : this.thumbSize / 2;
            const d = this.reverseMinmax ? this.thumbSize - _d : _d;
            value = (y - d) * (this._maxValue - this._minValue) / (h - this.thumbSize) + this._minValue;
        } else {
            const x = (this.reverseMinmax ? r.right - pos.x : pos.x - r.left) - thumbAdjust;
            const w = r.width - thumbAdjust;
            const _d = hit ? hit.x : this.thumbSize / 2;
            const d = this.reverseMinmax ? this.thumbSize - _d : _d;
            value = (x - d) * (this._maxValue - this._minValue) / (w - this.thumbSize) + this._minValue;
        }
        return this.__compute(value);
    }

    _trackMouse(setValue, thumb, touch) {
        const mmv = ev => {
            if (ev.defaultPrevented) {
                // Move event has already been processed
                return;
            }
            if (!ev.cancelable) {
                // Cannot cancel moveevent
                return;
            }
            setValue(ev);
        };

        this.draggingThumb = thumb;
        this._thumb1AtTop = (thumb === 'thumb1');

        const mouseMoveEv = touch ? 'touchmove' : 'mousemove';
        const mouseUpEv = touch ? 'touchend' : 'mouseup';

        const mup = () => {
            window.removeEventListener(mouseMoveEv, mmv);
            window.removeEventListener(mouseUpEv, mup);
            this.draggingThumb = false;
        };

        window.addEventListener(mouseMoveEv, mmv, {passive: false});
        window.addEventListener(mouseUpEv, mup);
    }

    _mouseDn1(ev, touch) {
        if (this.disabled || ev.defaultPrevented) {
            return;
        }
        const r = this.shadowRoot.getElementById('thumb').getBoundingClientRect();
        const pos = this.__getPosFromEvent(ev);
        const hit = {x: pos.x - r.left, y: pos.y - r.top};
        this._trackMouse(e => {
            this.value = this._v2s(this._mouseToValue1(e, hit));
        }, 'thumb1', touch);
    }


    _mouseDn2(ev, touch) {
        if (this.disabled || ev.defaultPrevented) {
            return;
        }
        const r = this.shadowRoot.getElementById('thumb2').getBoundingClientRect();
        const pos = this.__getPosFromEvent(ev);
        const hit = {x: pos.x - r.left, y: pos.y - r.top};
        this._trackMouse(e => {
            this.value2 = this._v2s(this._mouseToValue2(e, hit));
        }, 'thumb2', touch);
    }

    _clickTrack1(ev) {
        if (this.disabled) {
            return;
        }
        this.value = this._v2s(this._mouseToValue1(ev));
    }

    _clickTrack2(ev) {
        if (this.disabled) {
            return;
        }
        this.value = this.value2 = this._v2s((this._mouseToValue1(ev) + this._mouseToValue2(ev)) / 2);
    }

    _clickTrack3(ev) {
        if (this.disabled) {
            return;
        }
        if (this.range) {
            this.value2 = this._v2s(this._mouseToValue2(ev));
        } else {
            this.value = this._v2s(this._mouseToValue1(ev));
        }
    }

    _editValue1() {
        if (!this.editValue || this.disabled) {
            return;
        }
        this._edit1 = true;
        this.shadowRoot.getElementById('input1').value = this.value;
    }

    _editValue2() {
        if (!this.editValue || this.disabled) {
            return;
        }

        this._edit2 = true;
        this.shadowRoot.getElementById('input2').value = this.value2;
    }

    _onStopEdit1() {
        if (this.disabled) {
            return;
        }
        this.value = this.shadowRoot.getElementById('input1').value;
        this._edit1 = false;
    }

    _onStopEdit2() {
        if (this.disabled) {
            return;
        }
        this.value2 = this.shadowRoot.getElementById('input2').value;
        this._edit2 = false;
    }

    _handleKeyDown1(key, value) {
        if (this.disabled) {
            return undefined;
        }
        switch (key) {
            case 'ArrowRight':
            case 'ArrowUp':
                return Math.min(value + this._oneStep(), this._maxValue);

            case 'ArrowLeft':
            case 'ArrowDown':
                return Math.max(value - this._oneStep(), this._minValue);

            case 'Home':
                return this._minValue;

            case 'End':
                return this._maxValue;

            case 'PageUp':
                return Math.min(value + this._onePage(), this._maxValue);

            case 'PageDown':
                return Math.max(value - this._onePage(), this._minValue);
        }

        return undefined;
    }

    _handleKeyDown(ev, value) {
        if (this.disabled) {
            return undefined;
        }
        const v = this._handleKeyDown1(ev.key, value);

        if (v === undefined) {
            return value;
        }

        ev.preventDefault();
        return v;
    }

    _keyDn1(ev) {
        if (this.disabled) {
            return;
        }
        this.value = this._v2s(this._handleKeyDown(ev, this._value));
    }

    _keyDn2(ev) {
        if (this.disabled) {
            return;
        }
        this.value2 = this._v2s(this._handleKeyDown(ev, this._value2));
    }

    _updateTooltip() {
        if (this.thumbTooltip) {
            updateTooltipInFocus(this.shadowRoot.getElementById('thumb'));
        }
    }

    _updateTooltip2() {
        if (this.thumb2Tooltip) {
            updateTooltipInFocus(this.shadowRoot.getElementById('thumb2'));
        }
    }

    _mouseTooltip(ev) {
        const el = ev.target.querySelector('.read');
        if (el && ev.target.offsetWidth <= el.offsetWidth) {
            // The value overflows the span container
            const b = ev.target.getBoundingClientRect();
            const mousePointerWidth = 12;
            this.__tooltipEl = ev.target;
            hoverTooltip(this.__tooltipEl, Math.min(ev.clientX, b.x + b.width - 2 * mousePointerWidth), ev.clientY, el.innerText);
        }
    }

    _getValueWidth(isValue2) {
        const valEl = this.shadowRoot.getElementById(isValue2 ? 'value2' : 'value1');
        const offsetWidth = valEl ? valEl.offsetWidth : 0;
        const val = isValue2 ? this._value2 : this._value;

        const maxw = this.maxValueWidth || 96;
        const maxWidth = maxw + 'px';
        let minWidth;
        if (val > 1000) {
            minWidth = PTCS.isFirefox ? '-moz-fit-content' : 'fit-content';
        } else {
            const minw = this.minValueWidth || 34;
            minWidth = minw + 'px';
        }
        if (!PTCS.isFirefox && offsetWidth >= maxw || PTCS.isFirefox && val > 1E10) {
            minWidth = maxw + 'px';
        }
        return {minWidth, maxWidth};
    }

    _insertValidationMessage(messageElement) {
        this.defaultInsertValidationMessageForVerticalLayout(messageElement);
    }

    set showMinMaxLabels(val) {
        this._showMinMaxLabels = val;
    }

    get showMinMaxLabels() {
        return this._showMinMaxLabels;
    }

    set hideMinMaxLabels(val) {
        this._showMinMaxLabels = !val;
    }

    get hideMinMaxLabels() {
        return !this._showMinMaxLabels;
    }

    _validateSlider(minValidValue, maxValidValue, extraValidation) {
        const messages = [];

        const valueNum = Number(this._value);
        const valueNum2 = Number(this._value2);
        const minValue = minValidValue !== '' ? Number(minValidValue) : NaN;
        const maxValue = maxValidValue !== '' ? Number(maxValidValue) : NaN;

        // minValidValue
        if (valueNum !== undefined && valueNum < minValue) {
            const msg = PTCS.replaceStringTokens(this.minValueFailureMessage, {value: minValue});
            messages.push(msg ? msg.join('. ') : false);
        }

        // maxValidValue
        const secondaryValue = this.ariaValuenow.includes('to') ? valueNum2 : valueNum;
        if (secondaryValue !== undefined && secondaryValue > maxValue) {
            const msg = PTCS.replaceStringTokens(this.maxValueFailureMessage, {value: maxValue});
            messages.push(msg ? msg.join('. ') : false);
        }

        // At least one validation failed
        if (messages.length) {
            return messages;
        }

        // All standard validation has succeeded. Leave final say to the custom validation, if any
        return typeof extraValidation === 'function' ? extraValidation(this) : true;
    }

    _dfTabindex(_delegatedFocus, _noSpaceForMessage) {
        return _noSpaceForMessage ? '-1' : (_delegatedFocus || nothing);
    }

};

customElements.define(PTCS.Slider.is, PTCS.Slider);
