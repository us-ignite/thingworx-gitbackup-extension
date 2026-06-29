import {LitElement, html, css} from 'lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import {select} from 'd3-selection';

PTCS.ChartBands = class extends PTCS.BehaviorStyleable(LitElement) {

    static get styles() {
        return css`
        :host {
            display: block;
        }

        .bands {
            position: relative;
            display: none;
            width: 100%;
            height: 100%;
        }

        :host([show-chart-bands]) .bands{
            display: block;
        }

        .band {
            position: absolute;
            bottom: 0;
            top: 0;
        }

        :host([flip-axes]) .band {
            left: 0;
            right: 0;
        }`;
    }

    render() {
        return html`<div class="bands"></div>`;
    }

    static get is() {
        return 'ptcs-chart-bands';
    }

    static get properties() {
        return {
            innerPadding: {
                type:      Number,
                attribute: 'inner-padding'
            },

            outerPadding: {
                type:      Number,
                attribute: 'outer-padding'
            },

            bandWidth: {
                type:      Number,
                attribute: 'band-width'
            },

            flipAxes: {
                type:      Boolean,
                reflect:   true,
                attribute: 'flip-axes'
            },

            showChartBands: {
                type:      Boolean,
                reflect:   true,
                attribute: 'show-chart-bands'
            }
        };
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        if (['flipAxes', 'bandwidth', 'innerPadding', 'outerPadding'].some(propName => changedProperties.has(propName))) {
            this._renderBands();
        }
    }

    _renderBands() {
        const el = this.shadowRoot && this.shadowRoot.firstElementChild;
        const br = this.getBoundingClientRect();
        const range = this.flipAxes ? br.height : br.width;

        if (range <= 0 || !this.bandwidth || !this.innerPadding) {
            // Not ready yet
            el.replaceChildren();
            return;
        }

        const outerPadding = this.outerPadding ? this.outerPadding : 0;
        const bandwidth = this.bandwidth;
        const step = bandwidth + this.innerPadding;
        const bandsNum = Math.floor((range - 2 * outerPadding) / step) + 1;
        const bands = new Array(bandsNum);

        const pos = i => outerPadding + i * step;

        function drawVerticalBand(d, i) {
            this.style.heigth = '';
            this.style.width = `${bandwidth}px`;
            this.style.top = '';
            this.style.left = `${pos(i)}px`;
        }

        function drawHorizontalBand(d, i) {
            this.style.height = `${bandwidth}px`;
            this.style.width = '';
            this.style.top = `${pos(i)}px`;
            this.style.left = '';
        }

        const drawBand = this.flipAxes ? drawHorizontalBand : drawVerticalBand;

        const join = select(el)
            .selectAll('div.band')
            .data(bands);

        // Enter
        join.enter()
            .append('div')
            .attr('class', 'band')
            .attr('part', 'band')
            .each(drawBand);

        // Update
        join.each(drawBand);

        // Exit
        join.exit().remove();
    }
};

customElements.define(PTCS.ChartBands.is, PTCS.ChartBands);
