import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import {axisSpec} from 'ptcs-library/library-chart.js';
import {createTicks} from './library-axis-ticks.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';

// D3
import {select} from 'd3-selection';
import {scaleLinear, scaleTime, scaleBand} from 'd3-scale';

const FULLY_VISIBLE = 1.0;
const BARELY_VISIBLE = 0.1;

const affectsRange = ['_type', 'minValue', 'maxValue', 'specMin', 'specMax'];
const affectsScale = [
    '_type', 'side', 'reverse', 'scaleMin', 'scaleMax', 'size', 'dualTicks', 'outerPadding', 'innerPadding',
    'numberFormat', 'numberFormatSpecifier', 'dateFormatToken', 'tickFormat', 'numTicks', 'showZero'];


function hasChanged(newVal, oldVal) {
    if (newVal === oldVal) {
        return false;
    }
    if (newVal instanceof Date) {
        if (isNaN(newVal.getTime()) && oldVal instanceof Date && isNaN(oldVal.getTime())) {
            return false;
        }
        return (oldVal instanceof Date) ? newVal.getTime() !== oldVal.getTime() : true;
    }
    return !PTCS.sameArray(newVal, oldVal);
}

PTCS.Axis = class extends PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))) {

    static get styles() {
        return css`
        :host {
            display: flex;
            align-items: stretch;
            user-select: none;
        }

        :host([side=left]) {
            flex-direction: row;
            height: 100%;
        }

        :host([side=right]) {
            flex-direction: row-reverse;
            height: 100%;
        }

        :host([side=top]) {
            flex-direction: column;
            width: 100%;
        }

        :host([side=bottom]) {
            flex-direction: column-reverse;
            width: 100%;
        }

        :host([hidden]) {
            display: none !important;
        }

        :host([side=left]) #ticksdim {
            margin-left: var(--tick-offs-r, 8px);
        }

        :host([side=right]) #ticksdim {
            margin-right: var(--tick-offs-r, 8px);
        }

        :host([side=top]) #ticksdim {
            margin-top: var(--tick-offs-r, 8px);
        }

        :host([side=bottom]) #ticksdim {
            margin-bottom: var(--tick-offs-r, 8px);
        }

        [part=label-container] {
            text-align: center;
        }

        [part=label-container] > [part=label] {
            max-height: 100%;
            max-width: 100%;
        }

        :host([side=left]) [part=label-container] {
            writing-mode: vertical-rl;
            text-orientation: sideways;
            transform: rotate(180deg);
            margin-left: var(--ptcs-chart-axis-label--horizontal-margin, 0);
        }

        :host([side=right]) [part=label-container] {
            writing-mode: vertical-lr;
            text-orientation: sideways;
            transform: rotate(180deg);
            margin-right: var(--ptcs-chart-axis-label--horizontal-margin, 0);
        }

        :host([side=top]) [part=label-container] {
            margin-bottom: var(--ptcs-chart-axis-label--vertical-margin, 0);
        }

        :host([side=bottom]) [part=label-container] {
            margin-top: var(--ptcs-chart-axis-label--vertical-margin, 0);
        }

        :host([is-reference-lines]) [part=label-container] {
            display: none;
        }

        #ticks {
            position: relative;
            flex: 1 1 auto;
        }

        [part=tick-label], [part=tick-link] {
            position: absolute;
        }

        :host([side=left]) [part=tick-label],
        :host([side=left]) [part=tick-link] {
            right: 8px;
        }

        :host([side=right]) [part=tick-label],
        :host([side=right]) [part=tick-link] {
            left: 8px;
        }

        :host([side=top]) [part=tick-label],
        :host([side=top]) [part=tick-link] {
            bottom: 8px;
        }

        :host([side=bottom]) [part=tick-label],
        :host([side=bottom]) [part=tick-link]  {
            top: 8px;

            /* Absolute positioning with writing-mode=vertical-lr is messed up in Safari. Adding explicit "left" property fixes this. */
            left: 0;
        }

        [part=tick-label],
        [part=tick-link] {
            min-width: unset;
            min-height: unset;
        }

        :host([rotate-ticks]) {
            --rotate-values: 180deg;
        }

        :host([rotate-ticks]) [part=tick-label],
        :host([rotate-ticks]) [part=tick-link] {
            writing-mode: vertical-lr;
            text-orientation: sideways;
        }

        [part=label] {
            min-width: unset;
        }

        :host([align-label=start]) [part=label-container] {
            text-align: left;
        }

        :host([align-label=end]) [part=label-container] {
            text-align: right;
        }`;
    }

    render() {
        return html`
        <div part="label-container" id="cntr" ?hidden=${!this.label} align=${this.axisAlign}>
            <ptcs-label variant="label" .label=${this.label} part="label"></ptcs-label>
        </div>
        <div part="ticks-area" id="ticks"><div id="ticksdim"></div></div>`;
    }

    static get is() {
        return 'ptcs-chart-axis';
    }

    static get properties() {
        return {
            type: {
                type:      Object, // number || date || Array (of labels)
                value:     'number',
                converter: value => value
            },

            // type, but with eliminated duplicates
            _type: {
                type: Object
            },

            // Weights of type labels, if type is labels
            _weights: {
                type: Object
            },

            // left || right || top || bottom
            side: {
                type:    String,
                value:   'left',
                reflect: true
            },

            // Axis label
            label: {
                type: String
            },

            // Label alignment
            alignLabel: {
                type:      String,
                reflect:   true,
                attribute: 'align-label'
            },

            // Minimum value in data
            minValue: {
                type:      Object,
                hasChanged,
                attribute: 'min-value'
            },

            // Maximum value in data
            maxValue: {
                type:      Object,
                hasChanged,
                attribute: 'max-value'
            },

            // Specified minimum value: baseline || auto || value
            specMin: {
                type:      Object,
                hasChanged,
                attribute: 'spec-min'
            },

            // Specified maximum value: auto || value
            specMax: {
                type:      Object,
                hasChanged,
                attribute: 'spec-max'
            },

            // Specified number of label ticks
            numTicks: {
                type:      Number,
                attribute: 'num-ticks'
            },

            // Current size / length of axis (width or height depending on this.side)
            size: {
                type: Number
            },

            // Maximum size / length of axis
            maxSize: {
                type:      String,
                attribute: 'max-size'
            },

            // Reverse the axis direction
            reverse: {
                type: Boolean
            },

            rotateTicks: {
                type:      Boolean,
                value:     false,
                reflect:   true,
                attribute: 'rotate-ticks'
            },

            // Should the zero tick be displayed (if possible)
            showZero: {
                type:      Boolean,
                attribute: 'show-zero'
            },

            // The generated scale
            scale: {
                type:   Function,
                notify: true
            },

            axisId: {
                type:      String,
                reflect:   true,
                attribute: 'axis-id'
            },

            // Minimum value on current scale: function(minValue, maxValue, minSpec)
            scaleMin: {
                type:      Number,
                notify:    true,
                attribute: 'scale-min'
            },

            // Maximum value on current scale: function(minValue, maxValue, maxSpec)
            scaleMax: {
                type:      Number,
                notify:    true,
                attribute: 'scale-max'
            },

            // Axis ticks: [{label, value, offs}, ...]
            ticks: {
                type:   Array,
                notify: true
            },

            _focusedTick: Number,

            // Ticks from a dual axis that this axis should synchronize to
            dualTicks: {
                type:      Array,
                attribute: 'dual-ticks'
            },

            // Only for horizontal axes: Set explicit ticks rotation instead of using auto rotation
            ticksRotation: {
                type:      String,
                attribute: 'ticks-rotation'
            },

            _ticksRotation: {
                computed: '_computeTicksRotation(ticksRotation)'
            },

            _horizontal:  Boolean,
            _scaleLength: Number,

            // Padding
            outerPadding: {
                type:      String,
                attribute: 'outer-padding'
            },

            innerPadding: {
                type:      String,
                attribute: 'inner-padding'
            },

            // max label dimension
            _maxLabelWidth:  String,
            _maxLabelHeight: String,


            // Why do the axis have *three* tick formats!? (For two axis types!?!)
            // Why would _anyone_ want to specify a date format for a non date axis? Or vice versa?
            // Why do the formats have such inconsistent names! (Format, FormatSpecifier, and FormatToken?).
            //
            // Introducing a single tickFormat. Keeping the others _only_ for backwards compatibility
            tickFormat: {
                type:      String,
                attribute: 'tick-format'
            },

            // --------- Avoid Using ---------
            numberFormat:          String,
            numberFormatSpecifier: String,
            dateFormatToken:       String,
            // -------------------------------

            // Reference lines data
            referenceLines: {
                type: Array
            },

            // Filtered & processed reference lines data, ready for use as dualTicks data
            effReferenceLines: {
                type:   Array,
                value:  [],
                notify: true
            },

            disabled: {
                type: Boolean,
            },

            isReferenceLines: {
                type:      Boolean,
                observer:  '_isReferenceLinesChanged',
                reflect:   true,
                attribute: 'is-reference-lines'
            }
        };
    }

    static get observers() {
        return [
            // '_updateRange(_type, minValue, maxValue, specMin, specMax)',
            // eslint-disable-next-line max-len
            // '_updateScale(_type, side, reverse, scaleMin, scaleMax, size, dualTicks, outerPadding, innerPadding, numberFormat, numberFormatSpecifier, dateFormatToken, tickFormat, numTicks, showZero)',
            '_updateMaxSize(maxSize, _horizontal, size, label, rotateTicks, _ticksRotation, ticks)'
        ];
    }

    ready() {
        super.ready();

        this.$.ticks.addEventListener('click', ev => this._clickTick(ev));
        this.addEventListener('keydown', ev => this._keyDown(ev));
    }

    _computeTicksRotation(ticksRotation) {
        if (!ticksRotation) {
            return ticksRotation === 0 ? 0 : undefined;
        }
        const v = +ticksRotation;
        return -180 <= v && v <= 180 ? v : undefined;
    }

    _updateMaxSize(maxSize, _horizontal, size, label, rotateTicks, _ticksRotation, ticks) {
        const ticksSize = ticks ? this.ticks.length : 3;

        const mxs = PTCS.cssDecodeSize(this.maxSize, this, !this._horizontal);
        if (!(mxs > 0)) {
            // No size limit
            this._maxLabelWidth = '';
            this._maxLabelHeight = '';
        } else if (this._horizontal) {
            // Reserved height for axis label
            const dh = this.label ? PTCS.getElementHeight(this.$.cntr) : 0;

            // Fixed rotation?
            if (this._ticksRotation !== undefined) {
                const angle = Math.abs(this._ticksRotation);
                const sin1 = Math.sin(angle * (Math.PI / 180));
                const sin2 = Math.sin((90 - angle) * (Math.PI / 180));

                // Estimated standard value for font height of axis tick. Can vary widely
                const fontH = 18;

                // The maximum width that a rotated rectangle with the height fontH can have in the avilable area
                // (At first assume there is at least 3 ticks, after that we divide with the size of the ticks)
                this._maxLabelWidth =
                    (this._ticksRotation < 180 && this._ticksRotation > -180 && this._ticksRotation !== 0)
                        ? `${(mxs - dh - fontH * sin2) / sin1}px` : `${(this.clientWidth / ticksSize)}px`;
            } else {
                this._maxLabelWidth = '';
            }
            this._maxLabelHeight = `${Math.max(mxs - dh, 0)}px`;
        } else {
            // Vertical axis. Ticks never rotates in this mode
            this._maxLabelWidth = `${Math.max(mxs - (this.label ? PTCS.getElementWidth(this.$.cntr) : 0), 0)}px`;
            this._maxLabelHeight = '';
        }
    }

    _buildType(tree, list, _weights, level = 0, prefixString = '') {
        const prefix = prefixString ? s => `${prefixString}:$:${s}` : s => s;
        tree.forEach(item => {
            if (typeof item === 'string') {
                const key = prefix(item);
                if (_weights[key] === undefined) {
                    _weights[key] = list.length;
                    list.push({label: item, level, key: prefix(item)});
                }
            } else {
                const key = prefix(item.label);
                if (_weights[key] === undefined) {
                    _weights[key] = list.length;
                    list.push({
                        label:    item.label,
                        level,
                        key,
                        children: item.sub && item.sub.length > 0,
                        open:     item.open,
                        sub:      item.sub
                    });
                    if (item.sub && item.open) {
                        this._buildType(item.sub, list, _weights, level + 1, key);
                    }
                }
            }
        });
    }

    _typeChanged(type) {
        const _weights = {};
        let _type = type;

        if (type instanceof Array) {
            // Create weight for each label: {label: index} and eliminate duplicates
            _type = [];

            this._buildType(type, _type, _weights);
        }

        this.setProperties({_weights, _type});
    }

    _getRangeNumber(minValue, maxValue, specMin, specMax, exact) {
        const _specMin = axisSpec(specMin);
        const _specMax = axisSpec(specMax);
        const min = isNaN(_specMin) ? minValue : _specMin;
        const max = isNaN(_specMax) ? maxValue : _specMax;
        return (min !== max || exact) ? [min, max] : [min - 1, max + 1];
    }

    _getRangeDate(minValue, maxValue, specMin, specMax) {
        if ((!(minValue instanceof Date) || !(maxValue instanceof Date)) && (!(specMin instanceof Date) || !(specMax instanceof Date))) {
            // console.error('Invalid date min/max:' + minValue + ' / ' + maxValue);
            return null;
        }
        const minv = minValue instanceof Date ? minValue.getTime() : minValue;
        const maxv = maxValue instanceof Date ? maxValue.getTime() : maxValue;
        const mins = specMin instanceof Date ? specMin.getTime() : 'auto';
        const maxs = specMax instanceof Date ? specMax.getTime() : undefined;
        const [min, max] = this._getRangeNumber(minv, maxv, mins, maxs);
        return [new Date(min), new Date(max)];
    }

    __getMaxWlabel(maxValue) {
        let maxw = this._weights[maxValue];
        if (maxw !== undefined) {
            return maxw;
        }
        if (typeof maxValue === 'string') {
            const a = maxValue.split(':$:');
            if (a.length > 0) {
                maxw = a.length > 1 ? this._weights[a.splice(0, a.length - 1).join(':$:')] : undefined;
            }
            if (maxw !== undefined) {
                return maxw;
            }
        }
        if (this._type instanceof Array) {
            return Math.max(0, this._type.length - 1);
        }
        return 0;
    }

    __getMaxLabel(_type, specMax) {
        if (!(_type instanceof Array) || !this._weights) {
            return specMax;
        }
        const value = _type[this._weights[specMax]];
        if (!value || !value.open || !(value.sub instanceof Array) || value.sub.length === 0) {
            return specMax;
        }
        const last = value.sub[value.sub.length - 1];
        return `${value.label}:$:${last.label || last}`;
    }

    _getRangeLabel(_type, minValue, maxValue, specMin, specMax) {
        const minw = this._weights[minValue] || 0;
        const maxw = this.__getMaxWlabel(maxValue);
        // Only allow zooming on base values - not tick children (for now, at least)
        const baseValue = v => typeof v === 'string' ? v.split(':$:')[0] : v;
        const _specMin = baseValue(specMin);
        const _specMax = this.__getMaxLabel(_type, baseValue(specMax));
        const [min, max] = this._getRangeNumber(
            minw >= 0 ? minw : _specMin,
            maxw >= 0 ? maxw : _specMax,
            this._weights[_specMin],
            this._weights[_specMax],
            true);

        return [
            _type[Math.max(Math.min(_type.length - 1, Math.round(min)), 0)].key,
            _type[Math.max(Math.min(_type.length - 1, Math.round(max)), 0)].key
        ];
    }

    _updateRange() {
        const {_type, minValue, maxValue} = this;
        let {specMin, specMax} = this; // Needs massaging

        if (specMin === undefined || specMin === '') {
            specMin = 'auto';
        }
        if (specMax === undefined || specMax === '') {
            specMax = 'auto';
        }

        let minMax;
        if (_type === 'date') {
            minMax = this._getRangeDate(minValue, maxValue, specMin, specMax);
        } else if (_type instanceof Array) {
            if (_type.length === 0) {
                // Nothing to show on the axis. Probably no data provided.
                return;
            }

            minMax = this._getRangeLabel(_type, minValue, maxValue, specMin, specMax);
        } else {
            minMax = this._getRangeNumber(minValue, maxValue, specMin, specMax);
        }

        this.setProperties({
            scaleMin: minMax ? minMax[0] : undefined,
            scaleMax: minMax ? minMax[1] : undefined
        });
    }

    // Create scalar ticks
    _createTicks(dualTicks) {
        if (dualTicks instanceof Array) {
            this.requestUpdate();
            return undefined;
        }

        const scale = this.scale;
        if (typeof scale.ticks !== 'function') {
            return undefined;
        }

        const numTicks = this.numTicks >= 0 && (this._type === 'date' || this._type === 'number')
            ? this.numTicks
            : Math.max(3, Math.floor(this._scaleLength / 48));
        const formatSpecifier = (this._type === 'number' && (this.numberFormat || this.numberFormatSpecifier)) ||
                                (this._type === 'date' && this.dateFormatToken);
        const ticks = createTicks(scale, this._type, numTicks, formatSpecifier || this.tickFormat, this.showZero);

        // Make sure focus is still within ticks array
        if (!(0 <= this._focusedTick && this._focusedTick < ticks.length)) {
            this._focusedTick = 0;
        }
        return ticks;
    }

    // Create ordinal ticks (labels)
    _createLabelTicks(labels) {
        const scale = this.scale;
        const delta = scale.bandwidth() / 2;
        if (arguments.length === 0) {
            labels = this._zoomLabel(this._type, this.scaleMin, this.scaleMax);
        }

        // Keep track of focused item
        const focusKey = (this.ticks instanceof Array && this.ticks[this._focusedTick])
            ? this.ticks[this._focusedTick].key
            : undefined;

        // Optimal number of ticks
        const numTicks = Math.min(Math.max(3, Math.round(this._scaleLength / 24)), labels.length);

        // Needs sampling?
        if (labels.length > numTicks) {
            const excess = (labels.length - numTicks);
            const step = Math.max(2, Math.floor(excess / numTicks + 2));

            // Make sure focused tick is included in sample
            const focus = focusKey ? labels.findIndex(item => item === focusKey) : -1;
            const start = focus > 0 ? focus % step : 0;

            // Sampling
            const a = [];
            for (let i = start; i < labels.length; i += step) {
                a.push(labels[i]);
            }
            labels = a;
        }

        // Create ticks data
        const result = labels.map(value => {
            const key = value;
            const item = this._type[this._weights[key]];
            return {
                label:    item.label,
                key,
                children: item.children,
                value,
                offs:     scale(key) + delta,
                open:     item.open
            };
        });

        // Restore focus to the clicked element
        if (focusKey) {
            this._focusedTick = result.findIndex(item => item.key === focusKey);
        }

        // If focus is unavailable...
        if (!(0 <= this._focusedTick && this._focusedTick < result.length)) {
            this._focusedTick = 0;
        }

        return result;
    }

    _updateScale() {
        const {_type, side, reverse, scaleMin, scaleMax, size, dualTicks} = this;

        if (!size) {
            return;
        }

        // console.log(`${this.label}: ${_type}  ${scaleMin} to ${scaleMax} / ${size}`);
        this._horizontal = (side === 'top' || side === 'bottom');
        this._scaleLength = size; /* this._horizontal ? this.clientWidth : this.clientHeight; */

        if (_type === 'number') {
            this._createNumberScale(reverse, scaleMin, scaleMax, dualTicks);
        } else if (_type === 'date') {
            this._createDateScale(reverse, scaleMin, scaleMax, dualTicks);
        } else if (_type instanceof Array) {
            this._createLabelScale(_type, reverse, scaleMin, scaleMax);
        } else if (_type) {
            console.error('Unknown axis type: ' + JSON.stringify(_type));
            return;
        }
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        // These callbacks should be called _before_ rendering the current state (willUpdate)
        if (changedProperties.has('type')) {
            this._typeChanged(this.type);
        }

        if (affectsRange.some(propName => changedProperties.has(propName))) {
            this._updateRange();
        }

        if (affectsScale.some(propName => changedProperties.has(propName))) {
            this._updateScale();
        }
    }

    _createNumberScale(reverse, scaleMin, scaleMax, dualTicks) {
        if (!dualTicks) {
            // Update the axis scale

            let min = +scaleMin;
            let max = +scaleMax;
            if (isNaN(min) || isNaN(max)) {
                return;
            }

            // Enforce 5% distance between min and max
            if (min === max) {
                const d = 0.05 * Math.abs(this.maxValue - this.minValue);
                min -= d / 2;
                max += d / 2;
            }

            this.scale = scaleLinear()
                .domain(this._horizontal ? [min, max] : [max, min])
                .range(reverse ? [this._scaleLength, 0] : [0, this._scaleLength]);

            if (this.numTicks === 0) {
                this.scale._fixedNumTicks = true;
                this.scale.ticks = undefined;
            } else if (this.numTicks > 0) {
                this.scale._fixedNumTicks = true;
                this.scale.ticks = numTicks => {
                    const d = this.scale.domain();
                    const minimum = d[0];
                    const maximum = d[d.length - 1];

                    if (numTicks === 1) {
                        return [(minimum + maximum) / 2];
                    }

                    const interval = (maximum - minimum) / (numTicks - 1);
                    const result = [];
                    for (let i = 0; i < numTicks; i++) {
                        result.push(minimum + interval * i);
                    }
                    return result;
                };
            }
        } else {
            // Dummy function that is not actually used since we are in the dual ticks mode
            this.scale = () => {};
        }

        this.scale._formatTick = PTCS.formatNumber(this.numberFormat || this.numberFormatSpecifier || this.tickFormat);

        this.ticks = this._createTicks(dualTicks);
    }

    _createDateScale(reverse, scaleMin, scaleMax, dualTicks) {
        if (!(scaleMin instanceof Date)) {
            if ((scaleMin !== '' && scaleMin !== undefined) || (scaleMax !== '' && scaleMax !== undefined)) {
                console.warn(`Invalid min/max: ${JSON.stringify(scaleMin)} / ${JSON.stringify(scaleMax)}`);
            }

            this.ticks = [];
            return;
        }

        // Enforce 5% distance between min and max
        if (scaleMin.getTime() === scaleMax.getTime()) {
            if (this.minValue instanceof Date && this.maxValue instanceof Date) {
                const d = 0.05 * Math.abs(this.maxValue.getTime() - this.minValue.getTime());
                scaleMin = new Date(scaleMin.getTime() - d / 2);
                scaleMax = new Date(scaleMax.getTime() + d / 2);
            } else {
                return; // Invalid data
            }
        }

        this.scale = scaleTime()
            .domain(this._horizontal ? [scaleMin, scaleMax] : [scaleMax, scaleMin])
            .range(reverse ? [this._scaleLength, 0] : [0, this._scaleLength]);

        if (this.numTicks === 0) {
            this.scale._fixedNumTicks = true;
            this.scale.ticks = undefined;
        } else if (this.numTicks > 0) {
            this.scale._fixedNumTicks = true;
            this.scale.ticks = numTicks => {
                function chronologTime(scale) {
                    const d = scale.domain();
                    const min = d[0];
                    const max = d[d.length - 1];
                    return min < max ? [min, max] : [max, min];
                }
                const [minimum, maximum] = chronologTime(this.scale);

                if (numTicks === 1) {
                    return [new Date((minimum.getTime() + maximum.getTime()) / 2)];
                }

                const interval = (maximum.getTime() - minimum.getTime()) / (numTicks - 1);
                const result = [];
                for (let i = 0; i <= numTicks - 1; i++) {
                    result.push(new Date(minimum.getTime() + interval * i));
                }
                return result;
            };
        }

        this.scale._formatTick = PTCS.formatDate(this.dateFormatToken || this.tickFormat);

        this.ticks = this._createTicks(dualTicks);
    }

    // Convert padding format to d3
    _padding(padding) {
        const value = +padding;
        if (value > 0) {
            if (value < 100) {
                return value / 100;
            }
            return 1;
        }
        return 0;
    }

    _zoomLabel(_type, scaleMin, scaleMax) {
        if (scaleMin === undefined || scaleMin === null || scaleMax === undefined || scaleMax === null || !_type || _type.length === 0) {
            return _type.map(item => item.key);
        }
        if (_type[0].key === scaleMin && _type[_type.length - 1].key === scaleMax) {
            return _type.map(item => item.key);
        }
        return _type.slice(this._weights[scaleMin], this._weights[scaleMax] + 1).map(item => item.key);
    }

    _createLabelScale(_type, reverse, scaleMin, scaleMax) {
        const flip = reverse ? this._horizontal : !this._horizontal;
        const labels = this._zoomLabel(_type, scaleMin, scaleMax);
        this.scale = scaleBand()
            .domain(labels)
            .range(flip ? [this._scaleLength, 0] : [0, this._scaleLength])
            .padding(this._padding(this.outerPadding))
            .paddingInner(this._padding(this.innerPadding));

        this.ticks = this._createLabelTicks(labels); // labels has already been computed...
    }

    resized(force) {
        if (!this.scale) {
            return;
        }
        const _scaleLength = this._horizontal ? this.clientWidth : this.clientHeight;
        if (this._scaleLength !== _scaleLength || force) {
            this._scaleLength = _scaleLength;
            this.scale.range([0, _scaleLength]);

            if (this._type === 'number' || this._type === 'date') {
                this.ticks = this._createTicks(this.dualTicks);
            } else if (this._type instanceof Array) {
                this.ticks = this._createLabelTicks();
            }
        }
    }

    // This is part of a bug fix to allow reference line ticks to be keyboard navigated
    // It would be better if the ticks structure always contains the correct ticks,
    // but that would be a far bigger change - and therefore riskier
    _effectiveTicks(refresh) {
        if (this.isReferenceLines) {
            if (!this.scale) {
                return null;
            }
            if (refresh) {
                const scaleLength = this.size;

                const effReferenceLines = Array.isArray(this.referenceLines) && this.referenceLines
                    .map(item => ({label: item.label, value: item.value, offs: this.scale(item.value)}))
                    .filter(e => 0 <= e.offs && e.offs <= scaleLength)
                    .sort((a, b) => a.offs - b.offs);

                const sameItem = (a, b) => a.label === b.label && a.value === b.value && a.offs === b.offs;

                if (!PTCS.sameArray(effReferenceLines, this.effReferenceLines, sameItem)) {
                    this.effReferenceLines = effReferenceLines;
                }
            }

            return this.effReferenceLines;
        }

        if (Array.isArray(this.dualTicks)) {
            if (this.scale) {
                const formatTick = this.scale._formatTick || (v => v);

                // Format the dual ticks
                return this.dualTicks.map(item => {
                    return {label: formatTick(item.value), value: item.value, offs: item.offs, index: item.index};
                });
            }

            return this.dualTicks;
        }

        return this.ticks;
    }

    // For backwards compatibility (public method)
    refresh() {
        this.requestUpdate();
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        if (!this.isConnected) {
            return;
        }

        const isDisabled = this.disabled;
        const ticks = this._effectiveTicks(true);

        if (!ticks || this.hasAttribute('hidden')) {
            return;
        }

        // Dimension of widest / highest tick label
        let maxLabel = 0;

        function computeMaxH(bb) {
            if (maxLabel < bb.height) {
                maxLabel = bb.height;
            }
        }

        function computeMaxW(bb) {
            if (maxLabel < bb.width) {
                maxLabel = bb.width;
            }
        }

        const computeMax = this._horizontal ? computeMaxH : computeMaxW;

        const labelTranslate = PTCS.Axis._axisTranslate(this.side || 'left', this._ticksRotation);

        // eslint-disable-next-line no-nested-ternary
        const alignment = this._horizontal ? 'center' : (this.side === 'right' ? 'left' : 'right');

        const _maxLabelWidth = this._maxLabelWidth || '';
        const _maxLabelHeight = this._maxLabelHeight || '';

        const isNewLineCharacter = this.dateFormatToken && this.dateFormatToken.indexOf('\\n') >= 0 ;

        function createLabelTick() {
            const el = document.createElement('ptcs-label');
            el.setAttribute('part', 'tick-label');
            if (isNewLineCharacter) {
                el.preserveWhiteSpace = true;
            }
            return el;
        }

        function createLinkTick() {
            const el = document.createElement('ptcs-link');
            el.setAttribute('part', 'tick-link');
            el.noTabindex = true;
            el.singleLine = true;
            return el;
        }

        // Add tick label
        function processLabel(d) {
            const style = this.style;
            // Emit ptcs-label variant for non-interactive containers
            if (!d.children) {
                this.variant = 'body';
                this.maxWidth = this.reverse ? _maxLabelHeight : _maxLabelWidth;
            } else {
                this.disabled = isDisabled;
                // Emit ptcs-link variant primary for the expandable, interactive label containers
                this.variant = 'primary';
                this.textMaximumWidth = !this.reverse ? _maxLabelWidth : _maxLabelHeight;
                this.maxWidth = undefined;
            }
            this.label = d.label;
            this.horizontalAlignment = alignment;
            this.performUpdate();

            this.__task = d;
            style.maxWidth = _maxLabelWidth;
            style.maxHeight = _maxLabelHeight;

            const bb = this.getBoundingClientRect();
            style.transform = labelTranslate(d, bb.right - bb.left, bb.bottom - bb.top);
            computeMax(bb);
        }

        // Labels
        const labels = ticks.filter(d => !d.children);
        const links = ticks.filter(d => !!d.children);

        const join = select(this.$.ticks)
            .selectAll('ptcs-link')
            .data(links);

        // EXIT old elements not present in new data
        join.exit().remove();

        // UPDATE old elements present in new data
        join.each(processLabel);

        // ENTER new elements present in new data
        join.enter()
            .append(createLinkTick)
            .each(async function() {
                // Awful workaround for resetting the internal padding on the link label
                // The [part=tick-link]::part(label) CSS is activated too late
                await this.updateComplete;
                if (this.shadowRoot) {
                    this.shadowRoot.querySelector('[part~=label]').style.padding = '0px';
                }
            })
            .each(processLabel);

        const lab = select(this.$.ticks)
            .selectAll('ptcs-label')
            .data(labels);

        // EXIT old elements not present in new data
        lab.exit().remove();

        // UPDATE old elements present in new data
        lab.each(processLabel);

        // ENTER new elements present in new data
        lab.enter()
            .append(createLabelTick)
            .each(processLabel);

        // Store maximum dimension
        const f = () => {
            const s = this.$.ticksdim.style;
            if (this._horizontal) {
                s.width = '';
                s.height = `${maxLabel}px`;
                this._checkTickWidths();
            } else {
                s.width = `${maxLabel}px`;
                s.height = '';
                this.rotateTicks = false;
            }
        };

        // A somewhat hacky bug fix
        if (maxLabel === 0) {
            // Rare event: sometimes the scrolbars is created before the axis is attached. If so,
            // all the tick widths will be reported as zero, and must therefore be recomputed.
            requestAnimationFrame(() => {
                // Recompute tick widths
                this.$.ticks.querySelectorAll('ptcs-label[part=tick-label], ptcs-link[part=tick-link]')
                    .forEach(el => computeMax(el.getBoundingClientRect()));
                f();
            });
        } else {
            f();
        }

        // Horizontal *date* labels overflow the ticks area when the number of ticks is assigned a *fixed* value.
        // Because of how these ticks must be initialized - for legacy reasons - the first tick has the value of the minimum date
        // and the last tick the maximum date, so both labels overflow the ticks area by exactly half of their width.
        // To prevent the overflow, truncate these labels by setting a max width of half their actual width.
        if (this._type === 'date' && this.numTicks && !this._ticksRotation && !this.isReferenceLines &&
            !getComputedStyle(this).getPropertyValue('--rotate-values')) {
            const ticksBBLeft = this.$.ticks.getBoundingClientRect().left;
            this.$.ticks.querySelectorAll('[part=tick-label]:first-of-type, [part=tick-label]:last-of-type').forEach((label) => {
                const labelBB = label.getBoundingClientRect();
                if (labelBB.left < ticksBBLeft) {
                    label.style.transform = 'translate(0, 0)';
                }
                label.maxWidth = labelBB.width / 2;
            });
        }

        if (this.isReferenceLines) {
            this.labelCollisionFiltering();
        } else if (this.showZero && this.ticks && this.ticks[0] && this.ticks[0].value === 0) {
            // The first tick is zero in showZero mode, so it has probably been added as an extra tick: Hide any tick that collides with it
            this._zeroCollisionFilter();
        }
    }

    _isReferenceLinesChanged(isReferenceLines) {
        if (!isReferenceLines) {
            // Y2-Axis showing, not reference lines. Restore opacity on account of label ticks re-use
            const labelTicks = Array.from(this.$.ticks.querySelectorAll('[part=tick-label]'));
            labelTicks.forEach((label) => {
                label.style.opacity = FULLY_VISIBLE;
            });
        }
    }

    _overlappingRects(a, b) {
        return !((a.left >= b.right || b.left >= a.right) || (a.top >= b.bottom || b.top >= a.bottom));
    }

    labelCollisionFiltering() {
        if (!this.isReferenceLines) {
            return;
        }
        // Reference Lines labels collision filtering - change label opacity when its bounding box overlaps a preceding visible label's
        const labelTicks = Array.from(this.$.ticks.querySelectorAll('[part=tick-label]'));
        // Reference lines on y-axis (vertical sort) or x-axis (horizontal sort)?
        const sortVertically = this.getAttribute('id') === 'yaxis2';
        const candidates = sortVertically
            ? labelTicks.map(lbl => ({lbl, bb: lbl.getBoundingClientRect()})).sort((a, b) => (a.bb.y < b.bb.y) ? 1 : -1)
            : labelTicks.map(lbl => ({lbl, bb: lbl.getBoundingClientRect()})).sort((a, b) => (a.bb.x < b.bb.x) ? 1 : -1);
        let lastShowingLabel;
        candidates.forEach((label) => {
            if (!lastShowingLabel) {
                lastShowingLabel = label;
                label.lbl.style.opacity = FULLY_VISIBLE;
            } else if (this._overlappingRects(lastShowingLabel.bb, label.bb)) {
                // Label overlaps another fully visible label (collision), dim the label
                label.lbl.style.opacity = BARELY_VISIBLE;
            } else {
                // No overlap, show label fully
                lastShowingLabel = label;
                label.lbl.style.opacity = FULLY_VISIBLE;
            }
        });
    }

    _zeroCollisionFilter() {
        const tickEls = this.$.ticks.querySelectorAll('[part=tick-label]');
        if (tickEls.length < 2) {
            return;
        }
        const bb0 = tickEls[0].getBoundingClientRect();
        for (let i = 1; i < tickEls.length; i++) {
            if (this._overlappingRects(bb0, tickEls[i].getBoundingClientRect())) {
                tickEls[i].style.display = 'none';
            }
        }
    }

    /*
     * Returns a function that checks if the left box (bb0) intersects with the right box (bb1). In case of this.reverse the order is opposite.
     */
    get intersectFunc() {
        if (this.reverse) {
            if (this.rotateTicks) {
                return (bb1, bb0) => {
                    // Rotate back bounding box before testing overlap
                    const bb1Left = bb1.left + bb1.width / 2 - bb1.height / 2;
                    const bb0Right = bb0.left + bb0.width / 2 + bb0.height / 2;

                    return bb0Right > bb1Left;
                };
            }

            return (bb1, bb0) => bb0.right > bb1.left;
        }

        if (this.rotateTicks) {
            return (bb0, bb1) => {
                // Rotate back bounding box before testing overlap
                const bb1Left = bb1.left + bb1.width / 2 - bb1.height / 2;
                const bb0Right = bb0.left + bb0.width / 2 + bb0.height / 2;

                return bb0Right > bb1Left;
            };
        }

        return (bb0, bb1) => bb0.right > bb1.left;
    }

    _checkTickWidths() {
        if (this._ticksRotation !== undefined) {
            // Chart is using fixed rotation for the axis ticks
            this.rotateTicks = false;
            return;
        }

        // Fix to prevent horizontal axis oscillations. If the ticks has been rotated, don't allow ticks to rotate again,
        // unless axis width or ticks changes
        const w = this.$.ticks.getBoundingClientRect().width;
        if (this.rotateTicks && this.__oldCTW && this.__oldCTW.w === w && this.__oldCTW.ticks === this.ticks) {
            return;
        }
        this.__oldCTW = {w, ticks: this.ticks};

        // Check if the ticks needs to be rotated (if some of them overlaps)
        const list = this.$.ticks.querySelectorAll('ptcs-link, ptcs-label');
        const num = list.length;
        if (!num) {
            return;
        }
        const intersect = this.intersectFunc;
        let rotateTicks = false;
        const maxLblHeight = list[0].style.maxHeight;

        // I remove maxHeight to get the real size of the labels
        list[0].style.maxHeight = '';
        let bb0 = list[0].getBoundingClientRect();

        for (let i = 1; i < num; ++i) {
            list[i].style.maxHeight = '';

            const bb1 = list[i].getBoundingClientRect();

            if (intersect(bb0, bb1)) {
                list[i - 1].style.maxHeight = maxLblHeight;
                list[i].style.maxHeight = maxLblHeight;

                rotateTicks = true;
                break;
            }

            list[i - 1].style.maxHeight = maxLblHeight;

            bb0 = bb1;
        }

        list[num - 1].style.maxHeight = maxLblHeight;

        this.rotateTicks = rotateTicks;
    }

    _clickTick(ev) {
        if (this.disabled) {
            return;
        }
        const task = ev.target.__task;
        const focusKey = task ? (task.key || task.label) : undefined;

        // Set focus
        if (ev.target.parentNode === this.$.ticks && task) {
            const ticks = this._effectiveTicks();
            this._focusedTick = Array.isArray(ticks) ? ticks.findIndex(item => (item.key || item.label) === focusKey) : -1;
            console.assert(ev.target === this._getFocusEl());
            this.focus(); // Sometimes needed on Chrome
        }

        if (!task || typeof task.key !== 'string') {
            return;
        }

        const keys = task.key.split(':$:');
        let item = this.type.find(item2 => item2.label === keys[0]);
        for (let i = 1; i < keys.length; i++) {
            if (!item || !item.sub) {
                return;
            }
            item = item.sub.find(item2 => item2.label === keys[i]);
        }

        // A very simple click algorithm for now (single level accordian)
        if (item && item.sub && item.sub.length) {
            item.open = !item.open;

            // Close all other open items
            this.type.forEach(x => {
                if (x !== item && x.open) {
                    x.open = false;
                }
            });

            // Update types to reflect the newly expanded tick
            this._typeChanged(this.type);
        }
    }

    _getFocusEl() {
        if (this.disabled) {
            return null;
        }

        const ticks = this._effectiveTicks();
        if (!Array.isArray(ticks)) {
            return null;
        }

        function _key(el) {
            const r = (el && el.parentNode) ? el.__task : undefined;
            return r ? (r.key || r.label) : undefined;
        }

        if (0 <= this._focusedTick && this._focusedTick < ticks.length) {
            const r = ticks[this._focusedTick];
            const key = r.key || r.label;
            // Instant cache hit?
            if (_key(this.__focusEl) === key) {
                return this.__focusEl;
            }
            // Is the focus element on its expected place?
            const childTicks = this.$.ticks.children;
            this.__focusEl = childTicks[1 + this._focusedTick]; // 1+ for ticksdim
            if (_key(this.__focusEl) === key) {
                return this.__focusEl;
            }
            // Is the focus element anywhere?
            for (let i = 1; i < childTicks.length; i++) { // 1+ for ticksdim
                if (_key(childTicks[i]) === key) {
                    this.__focusEl = childTicks[i];
                    return this.__focusEl;
                }
            }
            this.__focusEl = null;
        }
        return null;
    }

    _initTrackFocus() {
        this._trackFocus(this, () => this._getFocusEl());
    }

    _notifyFocus() {
        let el = this._getFocusEl();
        if (!el) {
            this._focusedTick = 0;
            el = this._getFocusEl();
        }
        if (el) {
            el.scrollIntoViewIfNeeded();
        }
    }

    _keyDown(ev) {
        if (this.disabled) {
            return;
        }
        const ticks = this._effectiveTicks();
        if (!Array.isArray(ticks)) {
            return;
        }
        const el = this._getFocusEl();
        if (!el) {
            return;
        }
        let focus;
        switch (ev.key) {
            case 'Home':
            case 'PageUp':
                focus = 0;
                break;
            case 'ArrowUp':
            case 'ArrowLeft':
                focus = this._focusedTick - 1;
                break;
            case 'End':
            case 'PageDown':
                focus = ticks.length - 1;
                break;
            case 'ArrowDown':
            case 'ArrowRight':
                focus = this._focusedTick + 1;
                break;
            case 'Enter':
            case ' ':
                el.click();
                break;
            default:
                // Not handled
                return;
        }

        // We consumed this keyboard event. Don't propagate
        ev.preventDefault();

        if (0 <= focus && focus < ticks.length) {
            const childTicks = this.$.ticks.children;
            const focusCandidate = childTicks && childTicks[1 + focus]; // 1+ for ticksdim

            if (!focusCandidate || focusCandidate.style.display === 'none') {
                return; // don't navigate to the tick that is not visible or doesn't exist
            }

            this._focusedTick = focus;
            const el2 = this._getFocusEl();
            if (el2) {
                el2.scrollIntoViewIfNeeded();
            }
        }
    }
};

PTCS.Axis._axisTranslate = (side, ticksRotation) => {
    if (side === 'left' || side === 'right') {
        // No rotation for vertical ticks
        return (d, w, h) => `translate(0, ${d.offs - h / 2}px)`;
    }
    if (side === 'top' || side === 'bottom') {
        if (!ticksRotation) {
            // Default for horizontal ticks: supports auto-rotation
            return (d, w) => `translate(${d.offs - w / 2}px, 0) rotate(var(--rotate-values, 0deg))`;
        }

        // Explicit rotation of ticks
        const k1 = Math.sin(Math.abs(ticksRotation) * (Math.PI / 180)) / 2;
        const k2 = Math.sin((90 - Math.abs(ticksRotation)) * (Math.PI / 180)) / 2;
        const rot = (x, y) => `translate(${x}px, ${y}px) rotate(${ticksRotation}deg)`;
        const bottom = (side === 'bottom');

        if (ticksRotation < -90) {
            return bottom
                ? (d, w, h) => rot(d.offs - w * (k2 + 0.5), k1 * w - h * (k2 + 0.5))
                : (d, w, h) => rot(d.offs - w * (0.5 - k2), -k1 * w + h * (k2 + 0.5));
        }
        if (ticksRotation < 0) {
            return bottom
                ? (d, w, h) => rot(d.offs - w * (k2 + 0.5), k1 * w + h * (k2 - 0.5))
                : (d, w, h) => rot(d.offs - w * (0.5 - k2), -k1 * w - h * (k2 - 0.5));
        }
        if (ticksRotation <= 90) {
            return bottom
                ? (d, w, h) => rot(d.offs - w * (0.5 - k2), k1 * w + h * (k2 - 0.5))
                : (d, w, h) => rot(d.offs - w * (0.5 + k2), -k1 * w - h * (k2 - 0.5));
        }
        return bottom
            ? (d, w, h) => rot(d.offs - w * (0.5 - k2), k1 * w - h * (k2 + 0.5))
            : (d, w, h) => rot(d.offs - w * (0.5 + k2), -k1 * w + h * (k2 + 0.5));
    }

    // Should not be rechable
    return () => undefined;
};


customElements.define(PTCS.Axis.is, PTCS.Axis);
