import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-binary/ptcs-behavior-binary.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import {select} from 'd3-selection';

PTCS.ChartCoord = class extends PTCS.BehaviorStyleable(L2Pw(LitElement)) {

    static get styles() {
        return css`
    :host {
        width: 100%;
        height: 100%;
        clip-path: inset(-2px 0 -2px 0);
        display: grid;
        grid-template-columns: auto 1fr auto;
        grid-template-rows: auto 1fr auto;
    }

    [part=chart-slot] {
        grid-column: 2;
        grid-row: 2;
    }

    [part=rulers] {
        grid-column: 2;
        grid-row: 2;
        position: relative;
        pointer-events: none;
    }

    /* X-axis */
    :host(:not([flip-axes]):not([flip-x-axis])) [part=xaxis-slot] {
        grid-column: 2;
        grid-row: 3;
    }

    :host(:not([flip-axes])[flip-x-axis]) [part=xaxis-slot] {
        grid-column: 2;
        grid-row: 1;
    }

    :host([flip-axes]:not([flip-x-axis])) [part=xaxis-slot] {
        grid-column: 1;
        grid-row: 2;
    }

    :host([flip-axes][flip-x-axis]) [part=xaxis-slot] {
        grid-column: 3;
        grid-row: 2;
    }

    /* X-axis2 */
    :host(:not([flip-axes]):not([flip-x-axis])) [part=xaxis2-slot] {
        grid-column: 2;
        grid-row: 1;
    }

    :host(:not([flip-axes])[flip-x-axis]) [part=xaxis2-slot] {
        grid-column: 2;
        grid-row: 3;
    }

    :host([flip-axes]:not([flip-x-axis])) [part=xaxis2-slot] {
        grid-column: 3;
        grid-row: 2;
    }

    :host([flip-axes][flip-x-axis]) [part=xaxis2-slot] {
        grid-column: 1;
        grid-row: 2;
    }

    /* Y-axis */
    :host(:not([flip-axes]):not([flip-y-axis])) [part=yaxis-slot] {
        grid-column: 1;
        grid-row: 2;
    }

    :host(:not([flip-axes])[flip-y-axis]) [part=yaxis-slot] {
        grid-column: 3;
        grid-row: 2;
    }

    :host([flip-axes]:not([flip-y-axis])) [part=yaxis-slot] {
        grid-column: 2;
        grid-row: 3;
    }

    :host([flip-axes][flip-y-axis]) [part=yaxis-slot] {
        grid-column: 2;
        grid-row: 1;
    }

    /* Y-axis2 */
    :host(:not([flip-axes]):not([flip-y-axis])) [part=yaxis2-slot] {
        grid-column: 3;
        grid-row: 2;
    }

    :host(:not([flip-axes])[flip-y-axis]) [part=yaxis2-slot] {
        grid-column: 1;
        grid-row: 2;
    }

    :host([flip-axes]:not([flip-y-axis])) [part=yaxis2-slot] {
        grid-column: 2;
        grid-row: 1;
    }

    :host([flip-axes][flip-y-axis]) [part=yaxis2-slot] {
        grid-column: 2;
        grid-row: 3;
    }

    /* Y-rulers */
    :host(:not([show-y-rulers])) #yrulers {
        display: none;
    }

    :host(:not([show-y2-rulers])) #y2rulers {
        display: none;
    }

    [part~=y-ruler] {
        position: absolute;
    }

    :host(:not([flip-axes])) [part~=y-ruler] {
        left: -4px;
        right: 0;
        top: 0;
        height: 1px;
    }

    :host(:not([flip-axes])[flip-y-axis]) [part~=y-ruler] {
        left: 0;
        right: -4px;
    }

    :host([flip-axes]) [part~=y-ruler] {
        top: 0;
        bottom: -4px;
        left: 0;
        width: 1px;
    }

    :host([flip-axes][flip-y-axis]) [part~=y-ruler] {
        top: -4px;
        bottom: 0;
    }

    :host(:not([show-x-rulers])) #xrulers {
        display: none;
    }

    /* X-ruler */
    [part~=x-ruler] {
        position: absolute;
    }

    :host(:not([flip-axes])) [part~=x-ruler] {
        top: 0;
        bottom: -4px;
        left: 0;
        width: 1px;
    }

    :host(:not([flip-axes])[flip-x-axis]) [part~=x-ruler] {
        top: -4px;
        bottom: 0;
    }

    :host([flip-axes]) [part~=x-ruler] {
        left: -4px;
        right: 0;
        top: 0;
        height: 1px;
    }

    :host([flip-axes][flip-y-axis]) [part~=x-ruler] {
        left: 0;
        right: -4px;
    }

    :host([hide-zero-ruler]) [part~=zero-ruler] {
        display: none;
    }

    :host(:not([show-zero-ruler2])) [part~=zero-ruler2] {
        display: none;
    }

    [part~=zero-ruler], [part~=zero-ruler2] {
        position: absolute;
    }

    :host(:not([flip-axes])) [part~=zero-ruler], :host(:not([flip-axes])) [part~=zero-ruler2] {
        top: 0;
        height: 2px;
        left: 0;
        right: 0;
    }

    :host([flip-axes]) [part~=zero-ruler], :host([flip-axes]) [part~=zero-ruler2] {
        left: 0;
        width: 2px;
        top: 0;
        bottom: 0;
    }

    :host([front-rulers]) [part=rulers] {
        z-index: 12;
    }

    [part~=refline] {
        position: absolute;
        z-index: 13;
    }

    :host(:not([is-reference-lines])) [part~=refline] {
        display: none;
    }

    :host([flip-axes]) [part~=refline] {
        top: 0;
        bottom: -4px;
        left: 0;
        width: 0px;
        height: 100%
    }`;
    }

    render() {
        return html`
    <div part="xaxis-slot"><slot name="xaxis"></slot></div>
    <div part="yaxis-slot"><slot name="yaxis"></slot></div>
    <div part="xaxis2-slot"><slot name="xaxis2"></slot></div>
    <div part="yaxis2-slot"><slot name="yaxis2"></slot></div>
    <div part="rulers" ?hidden=${this.sparkView}>
      <div id="xrulers"></div>
      <div id="x2rulers"></div>
      <div id="yrulers"></div>
      <div id="y2rulers"></div>
      <div id="zero" part="zero-ruler ruler"></div>
      <div id="zero2" part="zero-ruler2 ruler"></div>
    </div>
    <div part="chart-slot"><slot name="chart"></slot></div>`;
    }

    static get is() {
        return 'ptcs-chart-coord';
    }

    // TODO: most attribute:'s are not needed
    static get properties() {
        return {
            // Swap xaxis and xaxis2
            flipXAxis: {
                type:      Boolean,
                reflect:   true,
                attribute: 'flip-x-axis'
            },

            // Swap yaxis and yaxis2
            flipYAxis: {
                type:      Boolean,
                reflect:   true,
                attribute: 'flip-y-axis'
            },

            // Flip axes (change place on xaxes and yaxes)
            flipAxes: {
                type:      Boolean,
                reflect:   true,
                attribute: 'flip-axes'
            },

            hideZeroRuler: {
                type:      Boolean,
                reflect:   true,
                attribute: 'hide-zero-ruler'

            },

            showZeroRuler2: {
                type:      Boolean,
                reflect:   true,
                attribute: 'show-zero-ruler2'
            },

            showXRulers: {
                type:      Boolean,
                reflect:   true,
                attribute: 'show-x-rulers'
            },

            showX2Rulers: {
                type:      Boolean,
                reflect:   true,
                attribute: 'show-x2-rulers'
            },

            frontRulers: {
                type:      Boolean,
                reflect:   true,
                attribute: 'front-rulers'
            },

            xTicks: {
                type: Array
            },

            x2Ticks: {
                type: Array
            },

            showYRulers: {
                type:      Boolean,
                reflect:   true,
                attribute: 'show-y-rulers'
            },

            showY2Rulers: {
                type:      Boolean,
                reflect:   true,
                attribute: 'show-y2-rulers'
            },

            yTicks: {
                type: Array
            },

            y2Ticks: {
                type: Array
            },

            isReferenceLines: {
                type:      Boolean,
                reflect:   true,
                attribute: 'is-reference-lines'
            },

            graphWidth: {
                type:   Number,
                notify: true
            },

            graphHeight: {
                type:   Number,
                notify: true
            },

            // Hide rulers on spark mode
            sparkView: {
                type:  Boolean,
                value: false
            },

            // Show rulers for the Y-axis: 'primary' or 'secondary'
            yAxisRulerAlignment: {
                type: String
            },

            hasY2: {
                type: Boolean
            },

            yScale: {
                type: Function
            },

            y2Scale: {
                type: Function
            }
        };
    }

    static get observers() {
        return [
            '_xRulers1(flipAxes, showXRulers, xTicks)',
            '_xRulers2(flipAxes, showX2Rulers, x2Ticks, isReferenceLines)',
            '_yRulers1(flipAxes, showYRulers, yTicks, yAxisRulerAlignment, hasY2, yScale)',
            '_yRulers2(flipAxes, showY2Rulers, y2Ticks, yAxisRulerAlignment, hasY2, y2Scale, isReferenceLines)'
        ];
    }

    constructor() {
        super();

        this._resizeObserver = new ResizeObserver(entries => {
            requestAnimationFrame(() => { // Avoid ResizeObserver loop limit exceeded, since it stops UTs
                const rect = entries[0].contentRect;
                this.setProperties({graphWidth: rect.width, graphHeight: rect.height});
            });
        });
    }

    ready() {
        super.ready();

        // Don't show zero-ruler unless it is ready
        const autoHide = style => {
            if (!style.transform) {
                style.display = 'none';
            }
        };

        autoHide(this.$.zero.style);
        autoHide(this.$.zero2.style);
    }

    connectedCallback() {
        super.connectedCallback();
        this.updateComplete.then(() => this._resizeObserver.observe(this.shadowRoot.querySelector('[part=chart-slot]')));
    }

    disconnectedCallback() {
        this._resizeObserver.unobserve(this.shadowRoot.querySelector('[part=chart-slot]'));
        super.disconnectedCallback();
    }

    _xRulers1(flipAxes, showXRulers, xTicks) {
        this._xRulers(flipAxes, showXRulers, xTicks, this.$.xrulers);
    }

    _xRulers2(flipAxes, showX2Rulers, x2Ticks, isReferenceLines) {
        this._xRulers(flipAxes, showX2Rulers, x2Ticks, this.$.x2rulers, isReferenceLines);
    }

    _xRulers(flipAxes, showXRulers, xTicks, ruler, isReferenceLines) {
        const setPos = flipAxes
            ? d => `translate(0,${d.offs}px)`
            : d => `translate(${d.offs}px,0)`;

        const partData = isReferenceLines ? 'refline x-refline' : 'ruler x-ruler';
        const join = select(ruler)
            .selectAll('[part="' + partData + '"]')
            .data((showXRulers && Array.isArray(xTicks)) ? xTicks : []);

        // Enter
        join.enter()
            .append('div')
            .attr('part', partData)
            .style('transform', setPos);

        // Update
        join.style('transform', setPos);

        // Exit
        join.exit().remove();
    }

    _yRulers1(flipAxes, showYRulers, yTicks, yAxisRulerAlignment, hasY2, yScale) {
        this._yRulers(flipAxes, showYRulers, yTicks, 'yrulers', yAxisRulerAlignment, hasY2, yScale);

    }

    _yRulers2(flipAxes, showY2Rulers, y2Ticks, yAxisRulerAlignment, hasY2, y2Scale, isReferenceLines) {
        this._yRulers(flipAxes, showY2Rulers, y2Ticks, 'y2rulers', yAxisRulerAlignment, hasY2, y2Scale, isReferenceLines);
    }

    _yRulers(flipAxes, showYRulers, yTicks, rulersId, yAxisRulerAlignment, hasY2, yScale, isReferenceLines) {
        const setPos = flipAxes
            ? d => `translateX(${d.offs}px)`
            : d => `translateY(${d.offs}px)`;

        // Show zero ruler for the primary Y Axis if we don't have Y2 data or the rulers are aligned to the primary Y Axis.
        // Show zero ruler for the second Y Axis if we have Y2 data and the rulers are aligned to the second Y Axis.
        if ((rulersId === 'yrulers' && (!yAxisRulerAlignment || yAxisRulerAlignment === 'primary' || !hasY2)) ||
            (rulersId === 'y2rulers' && yAxisRulerAlignment === 'secondary' && hasY2)) {
            this._showZeroRuler(flipAxes, yScale, this.$.zero, setPos);
        }

        // Handle second zero ruler in similar way as the first zero ruler, but opposite
        if (hasY2 && (rulersId === 'yrulers' && yAxisRulerAlignment === 'secondary') ||
            (rulersId === 'y2rulers' && (!yAxisRulerAlignment || yAxisRulerAlignment === 'primary'))) {
            this._showZeroRuler(flipAxes, yScale, this.$.zero2, setPos);
        }

        const partData = isReferenceLines ? 'refline y-refline' : 'ruler y-ruler';
        const join = select(this.$[rulersId])
            .selectAll('div[part]')
            .data((showYRulers && Array.isArray(yTicks)) ? yTicks : []);

        // Enter
        join.enter()
            .append('div')
            .attr('part', partData)
            .style('transform', setPos);

        // Update
        join.style('transform', setPos);

        // Exit
        join.exit().remove();
    }

    // Zero ruler
    _showZeroRuler(flipAxes, yScale, zeroEl, setPos) {
        // Only show zero rulers on numeric scales (dates will give fake zero lines)
        const zeroPt = (typeof yScale === 'function' && !yScale.bandwidth && yScale.domain && typeof yScale.domain()[0] === 'number')
            ? yScale(0) : NaN;
        const style = zeroEl.style;
        if (!isNaN(zeroPt)) {
            // For now, we *always* emit the zero line even in the edge cases to ensure that the
            // zero ruler isn't *completely* hidden in the edge cases
            const zeroDim = flipAxes ? zeroEl.clientWidth : zeroEl.clientHeight;
            style.transform = setPos({offs: zeroPt - zeroDim / 2 + 1});
            style.display = '';
            return;
        }
        style.display = 'none';
    }

};

customElements.define(PTCS.ChartCoord.is, PTCS.ChartCoord);
