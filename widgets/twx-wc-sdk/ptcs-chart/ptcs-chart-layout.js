import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-binary/ptcs-behavior-binary.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';


const STATE_DATA = 'data';
const STATE_LOADING = 'loading';

PTCS.ChartLayout = class extends PTCS.BehaviorStyleable(L2Pw(LitElement)) {

    static get styles() {
        return css`
    :host {
        display: grid;
        overflow: hidden;
        box-sizing: border-box;
    }

    [part=notes]:not([hidden]) {
        display: flex;
    }

    [part=notes][align=center] {
        align-items: center;
        justify-content: center;
    }

    [part=notes][align=end] {
        align-items: flex-end;
    }

    [part=notes][align=right] {
        justify-content: flex-end;
    }`;
    }

    render() {
        return html`
    <div part="title" ?hidden=${this.sparkView || this.hideTitle || this._chartStateHideTitle}><slot name="title"></slot></div>
    <div part="notes" ?hidden=${this.sparkView || this.hideNotes || this._chartStateHideOthers}
         align=${this.notesAlign}><slot name="notes"></slot></div>
    <div part="chart-state"><slot name="chart-state"></slot></div>
    <div part="chart-bands"><slot name="chart-bands"></slot></div>
    <div part="chart" ?hidden=${this._chartStateHideOthers}><slot name="chart"></slot></div>
    <div part="action-bar"><slot name="action-bar"></slot></div>
    <div part="legend" ?hidden=${this.sparkView || this.hideLegend || this._chartStateHideOthers}><slot name="legend"></slot></div>
    <div part="xzoom"><slot name="xzoom"></slot></div>
    <div part="xaxis"><slot name="xaxis"></slot></div>
    <div part="xaxis2"><slot name="xaxis2"></div>
    <div part="yzoom"><slot name="yzoom"></slot></div>
    <div part="yaxis"><slot name="yaxis"></slot></div>
    <div part="yaxis2"><slot name="yaxis2"></slot></div>
    <div part="below-yaxis"><slot name="below-yaxis"></slot></div>
    <div part="below-yaxis2"><slot name="below-yaxis2"></slot></div>`;
    }

    static get is() {
        return 'ptcs-chart-layout';
    }

    // TODO: This is an internal component only. We only need attributes for styling, not for prioperty assignements. Mnay attribute: can be removed
    static get properties() {
        return {
            // [top] || bottom
            titlePos: {
                type:      String,
                reflect:   true,
                attribute: 'title-pos'
            },

            hideTitle: {
                type:      Boolean,
                attribute: 'hide-title'
            },

            // top || [bottom]
            notesPos: {
                type:      String,
                reflect:   true,
                attribute: 'notes-pos'
            },

            hideNotes: {
                type:      Boolean,
                attribute: 'hide-notes'
            },

            // start || center || end
            notesAlign: {
                type:      String,
                attribute: 'notes-align'
            },

            // top || bottom || left || [right]
            legendPos: {
                type:      String,
                reflect:   true,
                attribute: 'legend-pos'
            },

            hideLegend: {
                type:      Boolean,
                attribute: 'hide-legend'
            },

            // Show xAxis area, unless sparkView
            xAxis: {
                type:      Boolean,
                attribute: 'x-axis'
            },

            // Show yAxis area, unless sparkView
            yAxis: {
                type:      Boolean,
                attribute: 'y-axis'
            },

            // Show secondary xAxis area, unless sparkView
            xAxis2: {
                type:      Boolean,
                attribute: 'x-axis2'
            },

            // Show secondary yAxis area, unless sparkView
            yAxis2: {
                type:      Boolean,
                attribute: 'y-axis2'
            },

            // Show reference (a.k.a. threshold) lines
            isReferenceLines: {
                type:      Boolean,
                attribute: 'is-reference-lines'
            },

            // Show xzoom area, unless sparkView
            xZoom: {
                type:      Boolean,
                attribute: 'x-zoom'
            },

            // Show yzoom area, unless sparkView
            yZoom: {
                type:      Boolean,
                attribute: 'y-zoom'
            },

            // top || bottom
            actionBar: {
                type:      String,
                reflect:   true,
                attribute: 'action-bar'
            },

            // Flip axes (change place on xaxis/xzoom and yaxis/yzoom)
            flipAxes: {
                type:      Boolean,
                reflect:   true,
                attribute: 'flip-axes'
            },

            // Flip XAxis side
            flipXAxis: {
                type:      Boolean,
                attribute: 'flip-x-axis'
            },

            // Flip YAxis side
            flipYAxis: {
                type:      Boolean,
                attribute: 'flip-y-axis'
            },

            // Hide everything except the chart pane
            sparkView: {
                type:      Boolean,
                value:     false,
                attribute: 'spark-view'
            },

            // Is the view narrow (mobile)?
            narrow: {
                type: Boolean
            },

            // Effective title position
            effTitlePos: {
                type:      String,
                notify:    true,
                reflect:   true,
                attribute: 'eff-title-pos'
            },

            // Effective notes position
            effNotesPos: {
                type:      String,
                notify:    true,
                reflect:   true,
                attribute: 'eff-notes-pos'
            },

            // Effective legend position
            effLegendPos: {
                type:      String,
                notify:    true,
                reflect:   true,
                attribute: 'eff-legend-pos'
            },

            chartState: {
                type:      String,
                attribute: 'chart-state'
            },

            // chartState hide the title only
            _chartStateHideTitle: {
                type:     Boolean,
                computed: '_computeChartStateHideTitle(chartState)'
            },

            // chartState hide all component but the title
            _chartStateHideOthers: {
                type:     Boolean,
                computed: '_computeChartStateHideOthers(chartState)'
            }
        };
    }

    static get observers() {
        return [
            // eslint-disable-next-line max-len
            '_change(titlePos, hideTitle, notesPos, hideNotes, legendPos, hideLegend, sparkView, _chartStateHideTitle, _chartStateHideOthers, narrow)',
            // eslint-disable-next-line max-len
            '_updateLayout(effTitlePos, effNotesPos, effLegendPos, sparkView, _chartStateHideTitle, _chartStateHideOthers, flipAxes, flipXAxis, flipYAxis, xAxis, yAxis, xAxis2, yAxis2, xZoom, yZoom, actionBar)'
        ];
    }

    constructor() {
        super();

        this._resizeObserver = new ResizeObserver(entries => {
            requestAnimationFrame(() => { // Avoid ResizeObserver loop limit exceeded, since it stops UTs
                const rect = entries[0].contentRect;
                this.narrow = rect.width <= 480;
                [...this.querySelectorAll('ptcs-label[part=title-label], ptcs-label[part=notes-label]')].forEach(el => {
                    el.maxWidth = rect.width;
                });
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

    _computeChartStateHideTitle(chartState) {
        return chartState === STATE_LOADING;
    }

    _computeChartStateHideOthers(chartState) {
        return chartState !== STATE_DATA;
    }

    _change(titlePos, hideTitle, notesPos, hideNotes, legendPos, hideLegend, sparkView, _chartStateHideTitle, _chartStateHideOthers, narrow) {
        // Resolve value
        function prop(pos, hide, dflt) {
            if (hide) {
                return '';
            }
            if (narrow && (pos === 'left' || pos === 'right')) {
                return dflt;
            }
            return pos || dflt;
        }
        // Resolve title value (can only be "top" (default) or "bottom")
        function propTitle() {
            if (hideTitle || _chartStateHideTitle) {
                return '';
            }
            // top || bottom
            return titlePos !== 'bottom' ? 'top' : titlePos;
        }

        // Compute effective values
        this.setProperties({
            effTitlePos:  propTitle(),
            effNotesPos:  prop(notesPos, sparkView || hideNotes || _chartStateHideOthers, 'top'),
            effLegendPos: prop(legendPos || 'right', sparkView || hideLegend || _chartStateHideOthers, 'top')
        });
    }

    // eslint-disable-next-line max-len
    _updateLayout(/* effTitlePos, effNotesPos, effLegendPos, sparkView, _chartStateHideTitle, _chartStateHideOthers, flipAxes, flipXAxis, flipYAxis, xAxis, yAxis, xAxis2, yAxis2, xZoom, yZoom, actionBar */) {
        // Debounce layout computations
        if (!this.__callUpdateLayout) {
            this.__callUpdateLayout = true;
            requestAnimationFrame(() => {
                this.__callUpdateLayout = false;
                this.__updateLayout();
            });
        }
    }

    __updateLayout() {
        // Select axis visibility
        const f1 = (a, b, fab) => fab ? b : a;
        const f = this.sparkView || this._chartStateHideOthers
            ? (() => false)
            : ((a1, a2, fa, b1, b2, fb) => this.flipAxes ? f1(b1, b2, fb) : f1(a1, a2, fa));

        // Compute active areas
        const secondYAxis = this.yAxis2 || this.isReferenceLines;
        const [title, notes, legend] = [this.effTitlePos, this.effNotesPos, this.effLegendPos];
        const [xa, xa2, fx] = [this.xAxis, this.xAxis2, this.flipXAxis];
        const [ya, ya2, fy] = [this.yAxis, secondYAxis, this.flipYAxis];
        const actionBar = this.sparkView || this._chartStateHideOthers ? null : this.actionBar;
        const xAxis = f(xa, xa2, fx, ya, ya2, fy);
        const xAxis2 = f(xa2, xa, fx, ya2, ya, fy);
        const yAxis = f(ya, ya2, fy, xa, xa2, fx);
        const yAxis2 = f(ya2, ya, fy, xa2, xa, fx);
        const _xZoom = this.xZoom && !(this.sparkView || this._chartStateHideOthers);
        const _yZoom = this.yZoom && !(this.sparkView || this._chartStateHideOthers);
        const [xZoom, yZoom] = this.flipAxes ? [_yZoom, _xZoom] : [_xZoom, _yZoom];

        // Diagnostic functions
        const left = v => v === 'left';
        const right = v => v === 'right';
        const top = v => v === 'top';
        const bottom = v => v === 'bottom';
        const vert = v => v === 'left' || v === 'right';
        const horz = v => v === 'top' || v === 'bottom';

        // List of actual sides
        const posList = [title, notes, legend, actionBar].filter(s => !!s);

        let row = 1; // Number of rows
        let col = 1; // Number of columns

        // Chart region
        const chartP = {x: 1, y: 1};

        posList.forEach(pos => {
            if (vert(pos)) {
                col++;
                if (left(pos)) {
                    chartP.x++;
                }
            } else {
                row++;
                if (top(pos)) {
                    chartP.y++;
                }
            }
        });

        // Compute legend position (always closest to chart except on top - above action-bar)
        const legendP = {x: chartP.x, y: chartP.y};
        switch (legend) {
            case 'top':
                legendP.x = 1;
                legendP.x2 = chartP.x + 1;
                legendP.y = chartP.y - 1;
                break;
            case 'bottom':
                legendP.x = 1;
                legendP.x2 = chartP.x + 1;
                legendP.y++;
                break;
            case 'left':
                legendP.x--;
                legendP.x2 = legendP.x + 1;
                break;
            case 'right':
                legendP.x++;
                legendP.x2 = legendP.x + 1;
                break;
        }

        // Compute title position (may depend on legend)
        // title x is not used any longer, since titles can only be 'top' or 'bottom' nowadays
        // eslint-disable-next-line no-unused-vars
        let titleY;
        switch (title) {
            case 'top':
                titleY = 1;
                break;
            case 'bottom':
                titleY = chartP.y + 1 + (bottom(legend) ? 1 : 0) + (bottom(actionBar) ? 1 : 0);
                break;
        }

        // Compute notes position (may depend on title and legend)
        let notesY;
        switch (notes) {
            case 'top':
                notesY = top(title) ? 2 : 1;
                break;
            case 'bottom':
                notesY = row;
                break;
        }

        let actionY;
        switch (actionBar) {
            case 'top':
                actionY = (top(title) ? 1 : 0) + (top(notes) ? 1 : 0) + 1;
                break;
            case 'bottom':
                actionY = legendP.y + (top(legend) ? 2 : 1);
                break;
        }

        const addRow = () => {
            row++;
            if (bottom(title)) {
                titleY++;
            }
            if (bottom(notes)) {
                notesY++;
            }
            if (bottom(legend)) {
                legendP.y++;
            }
            if (bottom(actionBar)) {
                actionY++;
            }
        };

        const addCol = () => {
            col++;
            if (right(legend)) {
                legendP.x++;
                legendP.x2++;
            } else if (horz(legend)) {
                legendP.x2++;
            }
        };

        // xAxis: unshift a row after the chart area
        if (xAxis) {
            addRow();
        }

        // xAxis2: unshift a row before the chart area
        if (xAxis2) {
            addRow();
            chartP.y++; // Shift chart down one step
        }

        // yAxis: unshift a row after the chart area
        if (yAxis) {
            addCol();
            chartP.x++; // Shift chart right one step
        }

        // yAxis2: add a column after the chart area
        if (yAxis2) {
            addCol();
        }

        // xzoom: append a row at the end of the chart strucure for the xzoom area
        let xZoomY;
        if (xZoom) {
            addRow();
            xZoomY = xAxis ? chartP.y + 2 : chartP.y + 1;
        }

        // yzoom: unshift a column for the yzoom area
        if (yZoom) {
            addCol();
            chartP.x++; // Shift chart right one step
        }

        // generate grid-template-columns / -rows
        const g = (v, c) => (v === c - 1) ? 'minmax(0, 1fr)' : 'auto';
        const tc = [...Array(col).keys()].map(cno => g(cno, chartP.x));
        const tr = [...Array(row).keys()].map(rno => g(rno, chartP.y));

        const elChart = this.shadowRoot.querySelector('[part=chart]');
        const elChartState = this.shadowRoot.querySelector('[part=chart-state]');
        const elChartBands = this.shadowRoot.querySelector('[part=chart-bands]');
        const elTitle = this.shadowRoot.querySelector('[part=title]');
        const elNotes = this.shadowRoot.querySelector('[part=notes]');
        const elLegend = this.shadowRoot.querySelector('[part=legend]');
        const elActionBar = this.shadowRoot.querySelector('[part=action-bar]');
        let elXAxis = this.shadowRoot.querySelector('[part=xaxis]');
        let elYAxis = this.shadowRoot.querySelector('[part=yaxis]');
        let elYAxisBelow = this.shadowRoot.querySelector('[part=below-yaxis]');
        let elXAxis2 = this.shadowRoot.querySelector('[part=xaxis2]');
        let elYAxis2 = this.shadowRoot.querySelector('[part=yaxis2]');
        let elYAxis2Below = this.shadowRoot.querySelector('[part=below-yaxis2]');
        let elXZoom = this.shadowRoot.querySelector('[part=xzoom]');
        let elYZoom = this.shadowRoot.querySelector('[part=yzoom]');

        if (this.flipXAxis) {
            [elXAxis, elXAxis2] = [elXAxis2, elXAxis];
        }
        if (this.flipYAxis) {
            [elYAxis, elYAxis2] = [elYAxis2, elYAxis];
            [elYAxisBelow, elYAxis2Below] = [elYAxis2Below, elYAxisBelow];
        }
        if (this.flipAxes) {
            [elXAxis, elYAxis, elXAxis2, elYAxis2, elXZoom, elYZoom] =
            [elYAxis, elXAxis, elYAxis2, elXAxis2, elYZoom, elXZoom];
            elYAxisBelow.style.display = 'none';
            elYAxis2Below.style.display = 'none';
            elYAxisBelow = elYAxis2Below = undefined;
        } else {
            elYAxisBelow.style.display = '';
            elYAxis2Below.style.display = '';
        }

        this.style.gridTemplateColumns = tc.join(' ');
        this.style.gridTemplateRows = tr.join(' ');

        [elChart, elChartState, elChartBands].forEach(el => {
            el.style.gridColumn = `${chartP.x}`;
            el.style.gridRow = `${chartP.y}`;
        });

        if (legend) {
            elLegend.style.gridColumn = `${legendP.x} / ${legendP.x2}`;
            if (vert(legend)) {
                // Stretch down to end of chart / bottom xaxis
                elLegend.style.gridRow = `${legendP.y} / ${chartP.y + (xAxis ? 2 : 1)}`;
            } else {
                elLegend.style.gridRow = `${legendP.y}`;
            }
            elLegend.style.display = '';
        } else {
            elLegend.style.display = 'none';
        }
        if (title) {
            elTitle.style.gridColumn = `1/${col + 1}`;
            elTitle.style.gridRow = `${titleY}`;
            elTitle.style.display = '';
        } else {
            elTitle.style.display = 'none';
        }
        if (notes) {
            elNotes.style.gridColumn = `1/${col + 1}`;
            elNotes.style.gridRow = `${notesY}`;
            elNotes.style.display = '';
        } else {
            elNotes.style.display = 'none';
        }
        if (actionBar) {
            elActionBar.style.gridColumn = `1/${col + 1}`;
            elActionBar.style.gridRow = `${actionY}`;
            elActionBar.style.display = '';
        } else {
            elActionBar.style.display = 'none';
        }
        if (xAxis) {
            elXAxis.style.gridColumn = `${chartP.x}`;
            elXAxis.style.gridRow = `${chartP.y + 1}`;
            elXAxis.style.display = '';
        } else {
            elXAxis.style.display = 'none';
        }
        if (xAxis2) {
            elXAxis2.style.gridColumn = `${chartP.x}`;
            elXAxis2.style.gridRow = `${chartP.y - 1}`;
            elXAxis2.style.display = '';
        } else {
            elXAxis2.style.display = 'none';
        }
        if (yAxis) {
            elYAxis.style.gridColumn = `${chartP.x - 1}`;
            elYAxis.style.gridRow = `${chartP.y}`;
            elYAxis.style.display = '';
            if (elYAxisBelow) {
                elYAxisBelow.style.gridColumn = `${chartP.x - 1}`;
                elYAxisBelow.style.gridRow = `${chartP.y + 1}`;
                elYAxisBelow.style.display = '';
            }
        } else {
            elYAxis.style.display = 'none';
            if (elYAxisBelow) {
                elYAxisBelow.style.display = 'none';
            }
        }
        if (yAxis2) {
            elYAxis2.style.gridColumn = `${chartP.x + 1}`;
            elYAxis2.style.gridRow = `${chartP.y}`;
            elYAxis2.style.display = '';
            if (elYAxis2Below) {
                elYAxis2Below.style.gridColumn = `${chartP.x + 1}`;
                elYAxis2Below.style.gridRow = `${chartP.y + 1}`;
                elYAxis2Below.style.display = '';
            }
        } else {
            elYAxis2.style.display = 'none';
            if (elYAxis2Below) {
                elYAxis2Below.style.display = 'none';
            }
        }
        if (xZoom) {
            elXZoom.style.gridColumn = `${chartP.x}/${col + 1}`;
            elXZoom.style.gridRow = `${xZoomY}`;
            elXZoom.style.display = '';
        } else {
            elXZoom.style.display = 'none';
        }
        if (yZoom) {
            elYZoom.style.gridColumn = `${chartP.x - (yAxis ? 2 : 1)}`;
            elYZoom.style.gridRow = `${chartP.y}`;
            elYZoom.style.display = '';
        } else {
            elYZoom.style.display = 'none';
        }
    }
};

customElements.define(PTCS.ChartLayout.is, PTCS.ChartLayout);
