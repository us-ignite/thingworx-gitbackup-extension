import {LitElement, html, css} from 'lit';
import {when} from 'lit/directives/when.js';
import {map} from 'lit/directives/map.js';
import {ifDefined} from 'lit/directives/if-defined.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import {axisBarMin, axisBarMax, axisMin, axisMax, typeIsFullRange, invertScaleRange} from 'ptcs-library/library-chart.js';

import {BehaviorChart} from '../ptcs-behavior-chart.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';

import 'ptcs-toolbar/ptcs-toolbar.js';
import 'ptcs-label/ptcs-label.js';
import '../ptcs-chart-layout.js';
import '../ptcs-chart-legend.js';
import '../ptcs-chart-state.js';
import '../ptcs-chart-coord.js';
import '../ptcs-chart-bands.js';
import '../ptcs-chart-icons.js';
import '../axes/ptcs-chart-axis.js';
import '../zoom/ptcs-chart-zoom.js';
import '../ptcs-chart-display-axis.js';
import './ptcs-chart-core-combo.js';
import {__xv} from './ptcs-chart-core-bar.js';

import {DrawPlot} from '../draw/ptcs-chart-draw-plot.js';
import {DrawLine} from '../draw/ptcs-chart-draw-line.js';
import {DrawArea} from '../draw/ptcs-chart-draw-area.js';
import {DrawBar} from '../draw/ptcs-chart-draw-bar.js';
import {DrawStackedBars} from '../draw/ptcs-chart-draw-stacked-bars';
import {DrawStackedAreas} from '../draw/ptcs-chart-draw-stacked-areas';
import {markersSet} from '../draw/ptcs-chart-draw-library';

import {
    curveLinear, curveBasis, curveBundle, curveCardinal, curveCatmullRom, curveMonotoneX,
    curveMonotoneY, curveNatural, curveStepBefore, curveStepAfter, curveStep} from 'd3-shape';

const curveArg = (value, _default) => value !== undefined ? value : _default;

const curve = {
    linear: function() {
        return curveLinear;
    },
    basis: function() {
        return curveBasis;
    },
    bundle: function() {
        return curveBundle.beta(curveArg(this.bundleBeta, 0.5));
    },
    cardinal: function() {
        return curveCardinal.tension(curveArg(this.cardinalTension, 0.5));
    },
    'catmull-rom': function() {
        return curveCatmullRom.alpha(curveArg(this.catmullRomAlpha, 0.5));
    },
    'monotone-x': function() {
        return curveMonotoneX;
    },
    'monotone-y': function() {
        return curveMonotoneY;
    },
    natural: function() {
        return curveNatural;
    },
    step: function() {
        if (this.stepPosition === 'before') {
            return curveStepBefore;
        }
        if (this.stepPosition === 'after') {
            return curveStepAfter;
        }
        return curveStep;
    }
};


PTCS.ChartCombo = class extends BehaviorChart(PTCS.BehaviorTabindex(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))))) {

    static get styles() {
        return css`
        :host {
            display: block;
        }

        :host([disabled]) {
            pointer-events: none;
        }

        .yaxis-container:not([flip-axes]) {
            display: flex;
            height: 100%;
            clip-path: inset(-40px 0px -40px 0px);
        }

        .yaxis-container[flip-axes] {
            overflow: hidden auto;
            height: 100%;
        }

        ptcs-chart-bands {
            width: 100%;
            height: 100%;
        }

        #below-yaxis, #below-yaxis2 {
            overflow: auto hidden;
            width: 100%;
            min-height: 28px;
        }

        :not([flip-axes]) > .yaxis-flex {
            display: flex;
            flex-direction: row;
            height: 100%;
        }

        [flip-axes] > .yaxis-flex {
            display: flex;
            flex-direction: column;
            width: 100%;
        }

        ptcs-chart-axis, [part=legend-area] {
            width: 100%;
            height: 100%;
            box-sizing: border-box;
        }

        ptcs-chart-axis:not([hidden]) ~ ptcs-chart-axis[side=left], ptcs-chart-axis:not([hidden]) ~ ptcs-chart-axis[side=right] {
            border-left: var(--ptcs-axis-separator-width) var(--ptcs-axis-separator-style) var(--ptcs-axis-separator-color);
        }

        ptcs-chart-axis:not([hidden]) ~ ptcs-chart-axis[side=top], ptcs-chart-axis:not([hidden]) ~ ptcs-chart-axis[side=bottom] {
            border-top: var(--ptcs-axis-separator-width) var(--ptcs-axis-separator-style) var(--ptcs-axis-separator-color);
        }

        :host([dragging]) :is(ptcs-chart-legend, ptcs-toolbar, ptcs-chart-zoom, ptcs-label, ptcs-axis)  {
            pointer-events: none;
            user-select: none;
        }`;
    }

    render() {
        return html`
        <ptcs-chart-display-axis id="display-axis" part="display-axis" tabindex=${this._delegatedFocus}
                                 .open=${this._showDisplayAxis} @open-changed=${this._showDisplayAxisChangedEv}
                                 .axes=${this.yAxes} .enabled=${this._enabledYAxes}
                                 .selectedIndexes=${this._showYAxes} @selected-indexes-changed=${this._showYAxesChangedEv}></ptcs-chart-display-axis>
        <ptcs-chart-layout id="chart-layout" style="height:100%" part="chart-layout"
                           .disabled=${this.disabled}
                           .titlePos=${this.titlePos} .hideTitle=${!this.titleLabel}
                           .notesPos=${this.notesPos} .notesAlign=${this.notesAlign} .hideNotes=${this._hideNotes()}
                           .legendPos=${this.legendPos} .hideLegend=${this._hideLegend()}
                           .effLegendPos=${this._effLegendPos} @eff-legend-pos-changed=${this._effLegendPosChangedEv}
                           .xZoom=${this._showZoomX()} .yZoom=${this._showZoomY()}
                           .flipAxes=${this.flipAxes} .flipXAxis=${this.flipXAxis} .flipYAxis=${this.flipYAxes}
                           .xAxis=${!this.hideXAxis} .xAxis2=${this._isXReferenceLines}
                           .yAxis=${!this.hideY1Axis} .yAxis2=${this._showY2Axis()}
                           .sparkView=${this.sparkView}
                           .actionBar=${this._actionBar()}
                           .chartState=${this._chartState}>
            <div part="title-area" slot="title" style=${'text-align:' + this._getHorizontalAlignment(this.titlePos, this.titleAlign)}>
                <ptcs-label part="title-label" .label=${this.titleLabel} variant=${this.titleVariant}
                    .horizontalAlignment=${this._getHorizontalAlignment(this.titlePos, this.titleAlign)} multi-line></ptcs-label>
            </div>
            <div part="notes-area" slot="notes" style=${'text-align:' + this._getHorizontalAlignment(this.notesPos, this.notesAlign)}">
                <ptcs-label part="notes-label" .label=${this.notesLabel} variant="label"
                    .horizontalAlignment=${this._getHorizontalAlignment(this.notesPos, this.notesAlign)} multi-line></ptcs-label>
            </div>
            <ptcs-chart-state part="chart-state" slot="chart-state"
                .chartStateExt=${this.chartState}
                .chartStateDataError=${this._chartStateDataError}
                .chartStateDataEmpty=${this._chartStateDataEmpty}
                @chart-state-changed=${this._chartStateChangedEv}
                .iconLoading=${this.iconStateLoading}
                .labelNoData=${this.labelStateNoData}
                .iconNoData=${this.iconStateNoData}
                .labelEmpty=${this.labelStateEmpty}
                .iconEmpty=${this.iconStateEmpty}
                .labelError=${this.labelStateError}
                .iconError=${this.iconStateError}></ptcs-chart-state>
            <ptcs-chart-bands slot="chart-bands" part="chart-bands" .showChartBands=${this._showChartBands} .flipAxes=${this.flipAxes}
                .innerPadding=${this._innerPadding} .outerPadding=${this._outerPadding} .bandwidth=${this._bandwidth}></ptcs-chart-bands>
            <ptcs-chart-coord slot="chart" part="chart"
                .flipAxes=${this.flipAxes}
                .flipXAxis=${this.flipXAxis}
                .flipYAxis=${this.flipYAxes}
                .xTicks=${this._xTicks}
                .x2Ticks=${this._xReferenceLines}
                .yTicks=${this._yTicks}
                .yScale=${this._yScale}
                .y2Ticks=${this._isYReferenceLines ? this._yReferenceLines : this._y2Ticks}
                .y2Scale=${this._y2Scale}
                .showXRulers=${this.showXRulers}
                .showX2Rulers=${this._isXReferenceLines}
                .hasY2=${this._showY2Axis()}
                .showYRulers=${this._showYRulers()}
                .showY2Rulers=${this._showY2Rulers()}
                .isReferenceLines=${this._isXReferenceLines || this._isYReferenceLines}
                .yAxisRulerAlignment=${this.yAxisRulerAlignment}
                .frontRulers=${this.frontRulers}
                .hideZeroRuler=${this.hideZeroRuler} .showZeroRuler2=${this.showZeroRuler2}
                .graphWidth=${this._graphWidth} @graph-width-changed=${this._graphWidthChangedEv}
                graph-height=${this._graphHeight} @graph-height-changed=${this._graphHeightChangedEv}
                .sparkView=${this.sparkView}>
                <ptcs-chart-core-combo slot="chart" id="chart" part="core-chart" style="pointer-events: auto"
                    tabindex=${this._delegatedFocus}
                    .disabled=${this.disabled}
                    .data=${this.data}
                    .drawables=${this._drawables}
                    .legend=${this.legend}
                    .tooltipTemplate=${this.tooltipTemplate}
                    .selectedLegend=${this._selectedLegend}
                    .xScale=${this._xScale}
                    .yScales=${this._ayScale}
                    .flipAxes=${this.flipAxes}
                    .showChartBands=${this._showChartBands}
                    .zoomSelect=${this._zoomSelect()}
                    .zoomDragX=${this._zoomDrag(this.xZoomDrag, this.noXZoom)}
                    .zoomDragY=${this._zoomDrag(this.yZoomDrag, this.noYZoom)}
                    .dragging=${this.dragging} @dragging-changed=${this._draggingChangedEv}
                    .selectionMode=${this.selectionMode}
                    .cursorType=${this._cursorType()}
                    .cursorTarget=${this._cursorTarget()}
                    @chart-selection=${this._onSelectionChanged}
                    @zoom-selection=${this._onZoomSelection}
                    @chart-state-data-error-changed=${this._chartStateDataErrorChangedEv}
                    @chart-state-data-empty-changed=${this._chartStateDataEmptyChangedEv}></ptcs-chart-core-combo>
            </ptcs-chart-coord>
            <div part="action-bar-area" slot="action-bar">
                <ptcs-toolbar id="toolbar" tabindex=${this._gcTabindex()}
                    part="action-bar" .disabled=${this.disabled} variant="secondary" hide-filter @activated=${this._toolbarAction}>
                </ptcs-toolbar>
            </div>
            <div part="legend-area" slot="legend">
                <ptcs-chart-legend
                    tabindex=${ifDefined(this._tabindex())}
                    id="legend"
                    part="legend"
                    .items=${this._legend}
                    .grouping=${!this.disableLegendGrouping}
                    .shape=${this.legendShape}
                    .filter=${this.filterLegend}
                    .horizontal=${this._horizLegend()}
                    .maxWidth=${this.legendMaxWidth}
                    .align=${this.legendAlign}
                    .disabled=${this.disabled}
                    .selected=${this._selectedLegend$} @selected-changed=${this._selectedLegend$Changed}></ptcs-chart-legend>
            </div>
            <ptcs-chart-zoom slot="xzoom" id="zoomX" part="zoom-xaxis"
                tabindex=${this._delegatedFocus}
                .disabled=${this.disabled}
                .type=${this._xType}
                .side=${this._xSide()}
                ?hidden=${this.noXZoom}
                .axisLength=${this._xSize()}
                .minValue=${this._zoomMin()}
                .maxValue=${this._zoomMax()}
                .zoomStart=${this.xZoomStart} @zoom-start-changed=${this._xZoomStartChanged}
                .zoomEnd=${this.xZoomEnd} @zoom-end-changed=${this._xZoomEndChanged}
                .rangePicker=${this.xZoomRange}
                .interval=${this.xZoomInterval}
                .intervalLabel=${this.xZoomIntervalLabel}
                .intervalControl=${this.xZoomIntervalControl}
                .intervalOrigin=${this.xZoomIntervalOrigin}
                .showIntervalAnchor=${this.xShowIntervalAnchor}
                .slider=${this.xZoomSlider}
                .sliderLabel=${this.xZoomSliderLabel}
                .sliderMinLabel=${this.xZoomSliderMinLabel}
                .sliderMaxLabel=${this.xZoomSliderMaxLabel}
                .rangeStartLabel=${this.xZoomRangeStartLabel}
                .rangeEndLabel=${this.xZoomRangeEndLabel}
                .reverseSlider=${this.reverseXAxis}
                .intervalFromLabel=${this.xZoomIntervalFromLabel}
                .intervalToLabel=${this.xZoomIntervalToLabel}></ptcs-chart-zoom>
            <ptcs-chart-axis slot="xaxis" id="xaxis" part="xaxis" style="pointer-events: auto"
                no-tabindex ?hidden=${this.hideXAxis}
                .type=${this._xType}
                .disabled=${this.disabled}
                .specMin=${this._specValueMin()}
                .specMax=${this._specValueMax()}
                .side=${this._xSide()}
                .label=${this.xAxisLabel}
                .alignLabel=${this.xAxisAlign}
                .minValue=${this._xMin}
                .maxValue=${this._xMax}
                .size=${this._xSize()}
                .maxSize=${this.flipAxes ? this.verticalAxisMaxWidth : this.horizontalAxisMaxHeight}
                .ticks=${this._xTicks} @ticks-changed=${this._xTicksChangedEv}
                .ticksRotation=${this.horizontalTicksRotation}
                .tickFormat=${this.xAxisTickFormat}
                .numTicks=${this.numberOfXLabels}
                .reverse=${this.reverseXAxis}
                @scale-changed=${this._xScaleChangedEv}
                .outerPadding=${this.outerPadding}
                .innerPadding=${this.innerPadding}></ptcs-chart-axis>
                ${when(this._isXReferenceLines, () => html`<ptcs-chart-axis id="xaxis2" slot="xaxis2" part="xaxis2" no-tabindex
                    style="pointer-events: auto"
                    .type=${this._xType}
                    .disabled=${this.disabled}
                    .specMin=${this._specValueMin()}
                    .specMax=${this._specValueMax()}
                    .side=${this._x2Side()}
                    .minValue=${this._xMin}
                    .maxValue=${this._xMax}
                    .size=${this._xSize()}
                    .maxSize=${this.flipAxes ? this.verticalAxisMaxWidth : this.horizontalAxisMaxHeight}
                    .ticks=${this._xTicks} @ticks-changed=${this._xTicksChangedEv}
                    .ticksRotation=${this.horizontalTicksRotation}
                    .reverse=${this.reverseXAxis}

                    .referenceLines=${this._xAxisReferenceLines}
                    @eff-reference-lines-changed=${this._xReferenceLinesChangedEv}
                    .isReferenceLines=${this._isXReferenceLines}></ptcs-chart-axis>`)}
            <ptcs-chart-zoom slot="yzoom" id="zoomY" part="zoom-yaxis"
                tabindex=${this._delegatedFocus}
                .disabled=${this.disabled}
                type="number"
                .side=${this._ySide()}
                ?hidden=${this.noYZoom}
                .axisLength=${this.flipAxes ? this._graphWidth : this._graphHeight}
                min-value="0"
                max-value="100"
                .zoomStart=${this.yZoomStart} @zoom-start-changed=${this._yZoomStartChangedEv}
                .zoomEnd=${this.yZoomEnd} @zoom-end-changed=${this._yZoomEndChangedEv}
                .rangePicker=${this.yZoomRange}
                .interval=${this.yZoomInterval}
                .intervalLabel=${this.yZoomIntervalLabel}
                .intervalControl=${this.yZoomIntervalControl}
                .intervalOrigin=${this.yZoomIntervalOrigin}
                .showIntervalAnchor=${this.yShowIntervalAnchor}
                .slider=${this.yZoomSlider}
                .sliderLabel=${this.yZoomSliderLabel}
                .sliderMinLabel=${this.yZoomSliderMinLabel}
                .sliderMaxLabel=${this.yZoomSliderMaxLabel}
                .rangeStartLabel=${this.yZoomRangeStartLabel}
                .rangeEndLabel=${this.yZoomRangeEndLabel}
                .reverseSlider=${this.reverseYAxis}
                .intervalFromLabel=${this.yZoomIntervalFromLabel}
                .intervalToLabel=${this.yZoomIntervalToLabel}></ptcs-chart-zoom>
            <div slot="yaxis" id="yaxis-container" class="yaxis-container" ?flip-axes=${this.flipAxes}><div class="yaxis-flex">
                ${map(this._primaryYAxes, (item, index) => html`<ptcs-chart-axis part="yaxis" style="pointer-events: auto" no-tabindex
                        ?hidden=${this._isHiddenAxis(item._minValue, item._maxValue, item.hide)}
                        .axisId=${item.id}
                        .disabled=${this.disabled}
                        .type=${item._type}
                        .specMin=${this._yAxisMin(item)}
                        .specMax=${this._yAxisMax(item)}
                        .side=${this._ySide()}
                        .label=${item.label}
                        .alignLabel=${item.align}
                        .minValue=${item._minValue}
                        .maxValue=${item._maxValue}
                        .size=${this.flipAxes ? this._graphWidth : this._graphHeight}
                        .maxSize=${this.flipAxes ? this.horizontalAxisMaxHeight : this.verticalAxisMaxWidth}
                        .ticksRotation=${this.horizontalTicksRotation}
                        .reverse=${this._reverseY(item.reverse, this.reverseYAxis)}
                        .tickFormat=${item.tickFormat}
                        .numTicks=${item.numTicks}
                        .showZero=${this._showZero1(index)}
                        @ticks-changed=${this._primaryYTicksChanged}
                        @scale-changed=${this._primaryYScaleChanged}></ptcs-chart-axis>`)}
            </div></div>
            <div slot="yaxis2" id="yaxis-container2" class="yaxis-container" ?flip-axes=${this.flipAxes}><div class="yaxis-flex">
                ${when(this._isYReferenceLines, () => html`<ptcs-chart-axis part="yaxis" style="pointer-events: auto" no-tabindex
                        .disabled=${this.disabled}
                        .scale=${this._yScaleReferenceLines}
                        .side=${this._y2Side()}
                        .size=${this.flipAxes ? this._graphWidth : this._graphHeight}
                        .maxSize=${this.flipAxes ? this.horizontalAxisMaxHeight : this.verticalAxisMaxWidth}
                        .ticksRotation=${this.horizontalTicksRotation}
                        .referenceLines=${this._yAxisReferenceLines}
                        @eff-reference-lines-changed=${this._yReferenceLinesChangedEv}
                        .isReferenceLines=${this._isYReferenceLines}></ptcs-chart-axis>`)}
                </template>
                ${map(this._secondaryYAxes, (item, index) => html`<ptcs-chart-axis part="yaxis" style="pointer-events: auto" no-tabindex
                        ?hidden=${this._isHiddenAxis(item._minValue, item._maxValue, item.hide)}
                        .disabled=${this.disabled}
                        .axisId=${item.id}
                        .type=${item._type}
                        .specMin=${this._yAxisMin(item)}
                        .specMax=${this._yAxisMax(item)}
                        .side=${this._y2Side()}
                        .label=${item.label}
                        .alignLabel=${item.align}
                        .minValue=${item._minValue}
                        .maxValue=${item._maxValue}
                        .size=${this.flipAxes ? this._graphWidth : this._graphHeight}
                        .maxSize=${this.flipAxes ? this.horizontalAxisMaxHeight : this.verticalAxisMaxWidth}
                        .reverse=${this._reverseY(item.reverse, this.reverseYAxis)}
                        .tickFormat=${item.tickFormat}
                        .numTicks=${item.numTicks}
                        .ticksRotation=${this.horizontalTicksRotation}
                        .showZero=${this._showZero2(index)}
                        @ticks-changed=${this._secondaryYTicksChanged}
                        @scale-changed=${this._secondaryYScaleChanged}></ptcs-chart-axis>`)}
            </div></div>
            <div slot="below-yaxis" id="below-yaxis"><div style="height:1px"></div></div>
            <div slot="below-yaxis2" id="below-yaxis2"><div style="height:1px"></div></div>
        </ptcs-chart-layout>`;
    }

    static get is() {
        return 'ptcs-chart-combo';
    }

    static get properties() {
        return {
            disabled: {
                type:    Boolean,
                reflect: true
            },

            // Title label
            titleLabel: {
                type:      String,
                attribute: 'title-label'
            },

            // [top] || bottom || left || right
            titlePos: {
                type:      String,
                attribute: 'title-pos'
            },

            // Title label variant
            titleVariant: {
                type:      String,
                attribute: 'title-variant'
            },

            // Title alignment: left || center || right
            titleAlign: {
                type:      String,
                attribute: 'title-align'
            },

            hideNotes: {
                type:      Boolean,
                attribute: 'hide-notes'
            },

            // Notes label
            notesLabel: {
                type:      String,
                attribute: 'notes-label'
            },

            // top || [bottom] || left || right
            notesPos: {
                type:      String,
                attribute: 'notes-pos'
            },

            notesAlign: {
                type:      String,
                attribute: 'notes-align'
            },

            // 'data', 'loading', 'no-data'
            chartState: {
                type:      String,
                attribute: 'chart-state'
            },

            // Computed by ptcs-chart-state, based on chartState, chartStateDataError and chartStateDataEmpty
            _chartState: {
                type: String
            },

            // Computed by ptcs-chart-core-combo, based on data and data2
            _chartStateDataEmpty: {
                type:  Boolean,
                state: true
            },

            iconStateLoading: {
                type:      String,
                attribute: 'icon-state-loading'
            },

            labelStateNoData: {
                type:      String,
                attribute: 'label-state-no-data'
            },

            iconStateNoData: {
                type:      String,
                attribute: 'icon-state-no-data'
            },

            labelStateEmpty: {
                type:      String,
                attribute: 'label-state-empty'
            },

            iconStateEmpty: {
                type:      String,
                attribute: 'icon-state-empty'
            },

            labelStateError: {
                type:      String,
                attribute: 'label-state-error'
            },

            iconStateError: {
                type:      String,
                attribute: 'icon-state-error'
            },

            // X-axis label
            xAxisLabel: {
                type:      String,
                attribute: 'x-axis-label'
            },

            xAxisAlign: {
                type:      String,
                attribute: 'x-axis-align'
            },

            xAxisTickFormat: {
                type:      String,
                attribute: 'x-axis-tick-format'
            },

            hideXAxis: {
                type:      Boolean,
                observer:  '_hideXAxisChanged',
                attribute: 'hide-x-axis'
            },

            // X-axis number of labels
            numberOfXLabels: {
                type:      Number,
                attribute: 'number-of-x-labels'
            },

            // [{id, type, label, position, align, reverse, hide, min, max, tickFormat}, ...]
            yAxes: {
                type:      Array,
                observer:  '_yAxesChanged',
                attribute: 'y-axes'
            },

            // yAxes ids of axes that are in use (not filtered out by legend filtering)
            _enabledYAxes: {
                type: Array,
            },

            // Array of yAxes indexes that should be displayed on chart
            _showYAxes: {
                type:     Array,
                observer: '_showYAxesChanged'
            },

            axisDisplayControl: {
                type:      Boolean,
                observer:  '_axisDisplayControlChanged',
                attribute: 'axis-display-control'
            },

            _primaryYAxes: {
                type: Array
            },

            _secondaryYAxes: {
                type: Array
            },

            _ayScale: {
                type: Object
            },

            // _yAxesValues holds the min and max values for each y-axis, as computed from the data
            // {axisId: [minValue, maxValue]}
            _yAxesValues: {
                type: Object
            },

            // {bar: [], area: [], line: [], axisMap: Map }
            _drawables: {
                type: Object
            },

            // [{id, method, order, curve}]
            stacks: {
                type: Array
            },

            hideY1Axis: {
                type:      Boolean,
                observer:  '_hideY1AxisChanged',
                attribute: 'hide-y1-axis'
            },

            hideLegend: {
                type:      Boolean,
                notify:    true, // Can be toggled via button
                attribute: 'hide-legend'
            },

            // Names of legends, if legends should be visible
            legend: {
                type: Array
            },

            // Same as legend, but with icons
            _legend: {
                type: Array
            },

            // top || bottom || left || [right]
            legendPos: {
                type:      String,
                attribute: 'legend-pos'
            },

            // Same as legendPos, unless chart size limitations forces legend to a different place
            _effLegendPos: {
                type: String
            },

            legendAlign: {
                type:      String,
                attribute: 'legend-align'
            },

            // square || circle || none
            legendShape: {
                type:      String,
                attribute: 'legend-shape'
            },

            // Filter chart using the legend?
            filterLegend: {
                type:      Boolean,
                attribute: 'filter-legend'
            },

            disableLegendGrouping: {
                type:      Boolean,
                attribute: 'disable-legend-grouping'
            },

            // Selected legends from legend component
            _selectedLegend$: {
                type: Array
            },

            // Legends currently selected in the legend component
            _selectedLegend: {
                type:     Array,
                computed: '_computeSelectedLegend(_selectedLegend$, legend)',
                observer: '_selectedLegendChanged'
            },

            // top || bottom
            actionBar: {
                type:      String,
                attribute: 'action-bar'
            },

            sparkView: {
                type:      Boolean,
                attribute: 'spark-view'
            },

            // Flip x- and y-axes
            flipAxes: {
                type:      Boolean,
                attribute: 'flip-axes'
            },

            // Flip x-axis side
            flipXAxis: {
                type:      Boolean,
                attribute: 'flip-x-axis'
            },

            // Flip y-axes sides
            flipYAxes: {
                type:      Boolean,
                attribute: 'flip-y-axes'
            },

            tooltipTemplate: {
                type:      String,
                attribute: 'tooltip-template'
            },

            // Connects ticks from x-axis to chart
            _xTicks: {
                type: Array
            },

            // Connects ticks from y-axis to chart
            _yTicks: {
                type: Array
            },

            // Show rulers for X-axis
            showXRulers: {
                type:      Boolean,
                attribute: 'show-x-rulers'
            },

            // Show rulers for Y-axis
            showYRulers: {
                type:      Boolean,
                attribute: 'show-y-rulers'
            },

            // Show rulers for the Y-axis: 'primary' or 'secondary'
            yAxisRulerAlignment: {
                type:      String,
                attribute: 'y-axis-ruler-alignment'
            },

            // Put rulers on top of chart
            frontRulers: {
                type:      Boolean,
                attribute: 'front-rulers'
            },

            // Reference lines (a.k.a. threshold lines) raw data
            referenceLines: {
                type:      Array,
                attribute: 'reference-lines'
            },

            _yScaleReferenceLines: {
                type: Function
            },

            // Is at least one y-reference line mapped to an axis with a value?
            _isXReferenceLines: {
                type: Boolean
            },

            // Reference lines for y-axes
            _xAxisReferenceLines: {
                type: Array
            },

            // Sorted & filtered reference lines from xaxis2 ptcs-chart-axis
            _xReferenceLines: {
                type: Array
            },

            // Is at least one y-reference line mapped to an axis with a value?
            _isYReferenceLines: {
                type: Boolean
            },

            // Reference lines for y-axes
            _yAxisReferenceLines: {
                type: Array
            },

            // Sorted & filtered secondary y-axis data from ptcs-chart-axis
            _yReferenceLines: {
                type: Array
            },

            // Watches for resizes
            _graphWidth: {
                type: Number
            },

            // Watches for resizes
            _graphHeight: {
                type: Number
            },

            // x-axis type: number || date || label || [string]
            xType: {
                type:      Object,
                attribute: 'x-type'
            },

            // x-axis type: number || date || [string]
            _xType: {
                type: Object
            },

            // Reverse direction of x-axis
            reverseXAxis: {
                type:      Boolean,
                attribute: 'reverse-x-axis'
            },

            // Reverse direction of y-axis
            reverseYAxis: {
                type:      Boolean,
                attribute: 'reverse-y-axis'
            },

            // Minimun x value in data
            _xMin: {
                type: Object
            },

            // Maximum x value in data
            _xMax: {
                type: Object
            },

            // Minimun y value in data - for zooming
            _yMin: {
                type: Number
            },

            // Maximum y value in data - for zooming
            _yMax: {
                type: Number
            },

            // Specified x-min-value: baseline || auto || Number
            specXMin: {
                type:      Object,
                attribute: 'spec-x-min'
            },

            // Specified x-max-value: auto || Number
            specXMax: {
                type:      Object,
                attribute: 'spec-x-max'
            },

            // Specified y-min-value: baseline || auto || Number
            specYMin: {
                type:      Object,
                attribute: 'spec-y-min'
            },

            // Specified y-max-value: auto || Number
            specYMax: {
                type:      Object,
                attribute: 'spec-y-max'
            },

            // Move x-scale from x-axis to chart
            _xScale: {
                type:     Function,
                observer: '_xScaleChanged'
            },

            // Move y-scale from y-axis to chart
            _yScale: {
                type: Function
            },

            // Disable X-axis zooming
            noXZoom: {
                type:      Boolean,
                attribute: 'no-x-zoom'
            },

            // Zooming based on properties
            xZoomStart: {
                type:      Object,
                attribute: 'x-zoom-start'
            },

            xZoomEnd: {
                type:      Object,
                attribute: 'x-zoom-end'
            },

            // Show zoom range UI control
            xZoomRange: {
                type:      Boolean,
                attribute: 'x-zoom-range'
            },

            // X-Axis Zoom Range Start Label
            xZoomRangeStartLabel: {
                type:      String,
                attribute: 'x-zoom-range-start-label'
            },

            // X-Axis Zoom Range End Label
            xZoomRangeEndLabel: {
                type:      String,
                attribute: 'x-zoom-range-end-label'
            },

            // Specify zoom interval
            xZoomInterval: {
                type:      Object,
                attribute: 'x-zoom-interval'
            },

            xZoomIntervalLabel: {
                type:      String,
                attribute: 'x-zoom-interval-label'
            },

            // 'dropdown' || 'radio' || 'textfield'
            xZoomIntervalControl: {
                type:      String,
                attribute: 'x-zoom-interval-control'
            },

            // 'start' || 'end'
            xZoomIntervalOrigin: {
                type:      String,
                attribute: 'x-zoom-interval-origin'
            },

            // Allow interval control to manipulate origin?
            xShowIntervalAnchor: {
                type:      Boolean,
                attribute: 'x-show-interval-anchor'
            },

            // Show zoom slider
            xZoomSlider: {
                type:      Boolean,
                attribute: 'x-zoom-slider'
            },

            xZoomSliderMaxLabel: {
                type:      String,
                attribute: 'x-zoom-slider-max-label'
            },

            xZoomSliderMinLabel: {
                type:      String,
                attribute: 'x-zoom-slider-min-label'
            },

            // X-zoom by selecting two elements
            xZoomSelect: {
                type:      Boolean,
                attribute: 'x-zoom-select'
            },

            // X-zoom by dragging the mouse over the chart
            xZoomDrag: {
                type:      Boolean,
                attribute: 'x-zoom-drag'
            },

            // Disable Y-axis zooming
            noYZoom: {
                type:      Boolean,
                attribute: 'n-y-zoom'
            },

            // Zooming based on properties
            // yZoomStart and yZoomEnd zooms all y-axes. The values are percentages which scales the y-axes
            yZoomStart: {
                type:      Number,
                attribute: 'y-zoom-start'
            },

            yZoomEnd: {
                type:      Number,
                attribute: 'y-zoom-end'
            },

            // Show zoom range UI control
            yZoomRange: {
                type:      Boolean,
                attribute: 'y-zoom-range'
            },

            yZoomRangeStartLabel: {
                type:      String,
                attribute: 'y-zoom-range-start-label'
            },

            yZoomRangeEndLabel: {
                type:      String,
                attribute: 'y-zoom-range-end-label'
            },

            // Specify zoom interval
            yZoomInterval: {
                type:      Object,
                attribute: 'y-zoom-interval'
            },

            yZoomIntervalLabel: {
                type:      String,
                attribute: 'y-zoom-interval-label'
            },

            // 'dropdown' || 'radio' || 'textfield'
            yZoomIntervalControl: {
                type:      String,
                attribute: 'y-zoom-interval-control'
            },

            // 'start' || 'end'
            yZoomIntervalOrigin: {
                type:      String,
                attribute: 'y-zoom-interval-origin'
            },

            // Allow interval control to manipulate origin?
            yShowIntervalAnchor: {
                type:      Boolean,
                attribute: 'y-show-interval-anchor'
            },

            // Show zoom slider
            yZoomSlider: {
                type:      Boolean,
                attribute: 'y-zoom-slider'
            },

            yZoomSliderLabel: {
                type:      String,
                attribute: 'y-zoom-slider-label'
            },

            yZoomSliderMaxLabel: {
                type:      String,
                attribute: 'y-zoom-slider-max-label'
            },

            yZoomSliderMinLabel: {
                type:      String,
                attribute: 'y-zoom-slider-min-label'
            },

            // Y-zoom by selecting two elements
            yZoomSelect: {
                type:      Boolean,
                attribute: 'y-zoom-select'
            },

            // Y-zoom by dragging the mouse over the chart
            yZoomDrag: {
                type:      Boolean,
                attribute: 'y-zoom-drag'
            },

            // When mouse is dragging on the chart
            dragging: {
                type:    Boolean,
                reflect: true
            },

            xZoomIntervalFromLabel: {
                type:      String,
                attribute: 'x-zoom-interval-from-label'
            },

            xZoomIntervalToLabel: {
                type:      String,
                attribute: 'x-zoom-interval-to-label'
            },

            yZoomIntervalFromLabel: {
                type:      String,
                attribute: 'y-zoom-interval-from-label'
            },

            yZoomIntervalToLabel: {
                type:      String,
                attribute: 'y-zoom-interval-to-label'
            },

            legendMaxWidth: {
                type:      Number,
                attribute: 'legend-max-width'
            },

            verticalAxisMaxWidth: {
                type:      Number,
                attribute: 'verticalAxisMaxWidth'
            },

            yAxesMaxWidth: {
                type:      String,
                attribute: 'y-axes-max-width'
            },

            alignBarsAtZero: {
                type:      Boolean,
                observer:  '_alignBarsAtZeroChanged',
                attribute: 'align-bars-at-zero'
            },

            horizontalAxisMaxHeight: {
                type:      Number,
                attribute: 'horizontal-axis-max-height'
            },

            horizontalTicksRotation: {
                type:      Number,
                attribute: 'horizontal-ticks-rotation'
            },

            // The chart data
            data: {
                type: Array
            },

            hideZeroRuler: {
                type:      Boolean,
                attribute: 'hide-zero-ruler'
            },

            showZeroRuler2: {
                type:      Boolean,
                attribute: 'show-zero-ruler2'
            },

            outerPadding: {
                type:      String,
                attribute: 'outer-padding'
            },

            innerPadding: {
                type:      String,
                attribute: 'inner-padding'
            },

            groupPadding: {
                type:      String,
                observer:  '_groupPaddingChanged',
                attribute: 'group-padding'
            },

            // Secondary y-axis
            hideY2Axis: {
                type:      Boolean,
                observer:  '_hideY2AxisChanged',
                attribute: 'hide-y2-axis'

            },

            _y2Scale: {
                type: Function,
            },

            _delegatedFocus: String,

            // Primary and secondary axes
            _primaryAxis:   Object,
            _secondaryAxis: Object,

            // 'none' || 'single' || 'multiple'
            selectionMode: {
                type:      String,
                attribute: 'selection-mode'
            },

            // Current selection in chart
            _chartSelection: {
                type: Object
            },

            // target method: auto (point) || horz || vert || cross
            pointerType: {
                type:      String,
                attribute: 'pointer-type'
            },

            // target method: auto (over) || horz || vert || both || none
            dataPointSelection: {
                type:      String,
                attribute: 'data-point-selection'
            },

            // sampleSize: unassigned - use default sampling,
            //             number - sample down data to specified number,
            //             0 (zero) = no sampling = show all points
            sampleSize: {
                type:      Number,
                observer:  '_sampleSizeChanged',
                attribute: 'sample-size'
            },

            // For the toolbar
            _isZoomable$tb: {
                // eslint-disable-next-line max-len
                computed: '_isZoomable(noXZoom, noYZoom, xZoomRange, yZoomRange, xZoomInterval, yZoomInterval, xZoomSlider, yZoomSlider, xZoomDrag, yZoomDrag, xZoomSelect, yZoomSelect, showZoomButtons)'
            },

            _resetButtonEnabled$tb: {
                computed: '_enableZoomReset(_xType, _xMin, _xMax, xZoomStart, xZoomEnd, _yMin, _yMax, yZoomStart, yZoomEnd, specYMin, specYMax)'
            },

            _showDisplayAxis: {
                type:     Boolean,
                observer: '_showDisplayAxisChanged'
            },

            // Show chart bands

            // user-provided property
            showChartBands: {
                type:      Boolean,
                attribute: 'show-chart-bands'
            },

            // calculated later depending on several conditions
            _showChartBands: {
                type: Boolean
            },

            _innerPadding: {
                type: Number
            },

            _outerPadding: {
                type: Number
            },

            _bandwidth: {
                type: Number
            }
        };
    }

    static get observers() {
        return [
            '_dataChanged(data, xType)',
            '_chartChanged(yAxes, data, legend, stacks)',
            '_resized(_graphWidth, graphHeight)',
            '_yAxesMaxSize(yAxesMaxWidth, flipAxes)',
            '_referenceLinesChanged(referenceLines, _xType)',
            '_updateChartBands(_xScale, _hasVisibleBars, showChartBands)'
        ];
    }

    constructor() {
        super();

        this.titleVariant = 'header';
        this._primaryYAxes = [];
        this._secondaryYAxes = [];
        this._ayScale = {};
        this._yAxesValues = {};
        this._drawables = {bar: [], area: [], line: [], axisMap: new Map()};
        this._yMin = 0;
        this._yMax = 100;
        this._yScaleReferenceLines = this.__yScaleReferenceLines.bind(this);
        this._resizeObserver = new ResizeObserver(this._onResizeYAxes.bind(this));
        this.hideXAxis = false;
        this.hideY1Axis = false;
        this.hideY2Axis = false;
        this.disableLegendGrouping = false;
    }

    ready() {
        super.ready();

        // Wire observers and event listeners for external scrolling region of yaxes container.
        // Note: this is done to allow axis ticks to extend above and below the scrollable region and to place the scrollbar
        // anywhere below the region, without overlapping the axes.
        const [scroll, start, move] = [this._onScrollYAxes.bind(this), this._onTouchstartYAxes.bind(this), this._onTouchmoveYAxes.bind(this)];
        this.$['below-yaxis'].addEventListener('scroll', scroll);
        this.$['below-yaxis2'].addEventListener('scroll', scroll);
        this.$['yaxis-container'].firstElementChild.addEventListener('touchstart', start);
        this.$['yaxis-container'].firstElementChild.addEventListener('touchmove', move);
        this.$['yaxis-container2'].firstElementChild.addEventListener('touchstart', start);
        this.$['yaxis-container2'].firstElementChild.addEventListener('touchmove', move);
    }

    connectedCallback() {
        super.connectedCallback();
        this._connected = true;
        this.updateComplete.then(() => {
            if (this._connected) {
                this._resizeObserver.observe(this.$['yaxis-container'].firstElementChild);
                this._resizeObserver.observe(this.$['yaxis-container2'].firstElementChild);
            }
        });
    }

    disconnectedCallback() {
        this._connected = false;
        this._resizeObserver.unobserve(this.$['yaxis-container'].firstElementChild);
        this._resizeObserver.unobserve(this.$['yaxis-container2'].firstElementChild);
        super.disconnectedCallback();
    }

    _gcTabindex() {
        return this._hideToolbar ? false : this._delegatedFocus;
    }

    _tabindex() {
        return this.filterLegend ? this._delegatedFocus : undefined;
    }

    _reverseY(reverse1, reverse2) {
        return reverse1 ? !reverse2 : reverse2;
    }

    _isZoomable(noXZoom, noYZoom, xZoomRange, yZoomRange, xZoomInterval, yZoomInterval, xZoomSlider, yZoomSlider, xZoomDrag, yZoomDrag,
        xZoomSelect, yZoomSelect, showZoomButtons) {
        return this._showZoom(noXZoom, xZoomRange, xZoomInterval, xZoomSlider, xZoomDrag, xZoomSelect, showZoomButtons) ||
            this._showZoom(noYZoom, yZoomRange, yZoomInterval, yZoomSlider, yZoomDrag, yZoomSelect, showZoomButtons);
    }

    _enableZoomReset(_xType, _xMin, _xMax, xZoomStart, xZoomEnd, _yMin, _yMax, yZoomStart, yZoomEnd, specYMin, specYMax) {
        return this._enabled(_xType, _xMin, _xMax, xZoomStart, xZoomEnd) ||
            this._yEnabled(_yMin, _yMax, yZoomStart, yZoomEnd, specYMin, specYMax);
    }

    _zoomSelect() {
        return (!this.noXZoom && this.xZoomSelect) || (!this.noYZoom && this.yZoomSelect);
    }

    _zoomDrag(drag, noZoom) {
        return !noZoom && drag;
    }

    _getHorizontalAlignment(pos, align) {
        if (pos === 'top' || pos === 'bottom') {
            return align;
        }

        return 'start';
    }

    _hideNotes() {
        return !this.notesLabel || this.hideNotes;
    }

    _hideLegend() {
        return this.hideLegend || !(this.legend instanceof Array) || !(this.legend.length > 0);
    }

    _horizLegend() {
        return this._effLegendPos === 'top' || this._effLegendPos === 'bottom';
    }

    _xSide() {
        // eslint-disable-next-line no-nested-ternary
        return this.flipAxes ? (this.flipXAxis ? 'right' : 'left') : (this.flipXAxis ? 'top' : 'bottom');
    }

    _x2Side() {
        // eslint-disable-next-line no-nested-ternary
        return this.flipAxes ? (this.flipXAxis ? 'right' : 'left') : (this.flipXAxis ? 'bottom' : 'top');
    }

    _ySide() {
        // eslint-disable-next-line no-nested-ternary
        return this.flipAxes ? (this.flipYAxes ? 'top' : 'bottom') : (this.flipYAxes ? 'right' : 'left');
    }

    _y2Side() {
        // eslint-disable-next-line no-nested-ternary
        return this.flipAxes ? (this.flipYAxes ? 'bottom' : 'top') : (this.flipYAxes ? 'left' : 'right');
    }

    _xSize() {
        return this.flipAxes ? this._graphHeight : this._graphWidth;
    }

    _actionBar() {
        return this._hideToolbar ? null : (this.actionBar || 'top');
    }

    _showYRulers() {
        return this.showYRulers && (this.hideY2Axis || this.yAxisRulerAlignment !== 'secondary');
    }

    _showY2Rulers() {
        return this._isYReferenceLines || (this.showYRulers && !this.hideY2Axis && this.yAxisRulerAlignment === 'secondary');
    }
    _showY2Axis() {
        return !this.hideY2Axis && (this._secondaryYAxes.length > 0 || this._isYReferenceLines);
    }

    _showZoom(noZoom, zoomRange, zoomInterval, zoomSlider, zoomDrag, zoomSelect, showZoomButtons) {
        if (noZoom) {
            return false;
        }
        return zoomRange || zoomInterval || zoomSlider || zoomDrag || zoomSelect || showZoomButtons;
    }

    _showZoomX() {
        return this._showZoom(this.noXZoom, this.xZoomRange, this.xZoomInterval, this.xZoomSlider, this.xZoomDrag, this.xZoomSelect);
    }
    _showZoomY() {
        return this._showZoom(this.noYZoom, this.yZoomRange, this.yZoomInterval, this.yZoomSlider, this.yZoomDrag, this.yZoomSelect);
    }

    _zoomMin() {
        return axisMin(this._xMin, this._xMax, this._xType, this.specXMin, this.specXMax);
    }

    _zoomMax() {
        return axisMax(this._xMin, this._xMax, this._xType, this.specXMax, this.specXMin);
    }

    _specValueMin() {
        if (!this.noXZoom && this.xZoomStart !== undefined && this.xZoomStart !== '' && this.xZoomStart !== null) {
            // Zooming
            return this.xZoomStart;
        }
        return axisMin(this._xMin, this._xMax, this._xType, this.specXMin, this.specXMax);
    }

    _specValueMax() {
        if (!this.noXZoom && this.xZoomEnd !== undefined && this.xZoomEnd !== '' && this.xZoomEnd !== null) {
            // Zooming
            return this.xZoomEnd;
        }
        // No zooming
        return axisMax(this._xMin, this._xMax, this._xType, this.specXMax, this.specXMin);
    }

    _yZoomMin(_yMin, _yMax, specYMin, specYMax) {
        return axisBarMin(_yMin, _yMax, specYMin, specYMax);
    }

    _yZoomMax(_yMin, _yMax, specYMax, specYMin) {
        return axisBarMax(_yMin, _yMax, specYMin, specYMax);
    }

    _yAxisMin({_minValue, _maxValue, _type, specMin, specMax}) {
        const min = axisMin(_minValue, _maxValue, _type, specMin, specMax);
        if (this.yZoomStart > 0) {
            const max = axisMax(_minValue, _maxValue, _type, specMax, specMin);
            if (_type === 'number') {
                return min + Math.min(this.yZoomStart, 100) * (max - min) / 100;
            }
            if (_type === 'date' && min instanceof Date && max instanceof Date) {
                return new Date(min.getTime() + Math.min(this.yZoomStart, 100) * (max.getTime() - min.getTime()) / 100);
            }
        }
        return min;
    }

    _yAxisMax({_minValue, _maxValue, _type, specMax, specMin}) {
        const max = axisMax(_minValue, _maxValue, _type, specMax, specMin);
        if (this.yZoomEnd < 100) {
            const min = axisMin(_minValue, _maxValue, _type, specMin, specMax);
            if (_type === 'number') {
                return max - Math.max(0, 100 - this.yZoomEnd) * (max - min) / 100;
            }
            if (_type === 'date' && max instanceof Date && min instanceof Date) {
                return new Date(max.getTime() - Math.max(0, 100 - this.yZoomEnd) * (max.getTime() - min.getTime()) / 100);
            }
        }
        return max;
    }

    _enabled(type, minValue, maxValue, zoomStart, zoomEnd) {
        return !typeIsFullRange(type, minValue, maxValue, zoomStart, zoomEnd);
    }

    _yEnabled(_yMin, _yMax, yZoomStart, yZoomEnd, specYMin, specYMax) {
        return this._enabled(
            'number',
            this._yZoomMin(_yMin, _yMax, specYMin, specYMax),
            this._yZoomMax(_yMin, _yMax, specYMax, specYMin),
            yZoomStart, yZoomEnd);
    }

    _cursorType() {
        const m = this.flipAxes ? {horz: 'x', vert: 'y', cross: 'xy'} : {horz: 'y', vert: 'x', cross: 'xy'};
        return m[this.pointerType] || this.pointerType;
    }

    _cursorTarget() {
        const m = this.flipAxes ? {horz: 'x', vert: 'y', both: 'xy'} : {horz: 'y', vert: 'x', both: 'xy'};
        return m[this.dataPointSelection] || this.dataPointSelection;
    }

    _showDisplayAxisChangedEv(ev) {
        this._showDisplayAxis = ev.detail.value;
    }

    _showYAxesChangedEv(ev) {
        this._showYAxes = ev.detail.value;
    }

    _effLegendPosChangedEv(ev) {
        this._effLegendPos = ev.detail.value;
    }

    _graphWidthChangedEv(ev) {
        this._graphWidth = ev.detail.value;
    }

    _graphHeightChangedEv(ev) {
        this._graphHeight = ev.detail.value;
    }

    _draggingChangedEv(ev) {
        this.dragging = ev.detail.value;
    }

    _selectedLegend$Changed(ev) {
        this._selectedLegend$ = ev.detail.value;
    }

    _xZoomStartChanged(ev) {
        this.xZoomStart = ev.detail.value;
    }

    _xZoomEndChanged(ev) {
        this.xZoomEnd = ev.detail.value;
    }

    _xTicksChangedEv(ev) {
        this._xTicks = ev.detail.value;
    }

    _xScaleChangedEv(ev) {
        this._xScale = ev.detail.value;
    }

    _yZoomStartChangedEv(ev) {
        this.yZoomStart = ev.detail.value;
    }

    _yZoomEndChangedEv(ev) {
        this.yZoomEnd = ev.detail.value;
    }

    _xReferenceLinesChangedEv(ev) {
        this._xReferenceLines = ev.detail.value;
    }

    _yReferenceLinesChangedEv(ev) {
        this._yReferenceLines = ev.detail.value;
    }

    _chartStateChangedEv(ev) {
        this._chartState = ev.detail.value;
    }

    _chartStateDataErrorChangedEv(ev) {
        this._chartStateDataError = ev.detail.value;
    }

    _chartStateDataEmptyChangedEv(ev) {
        this._chartStateDataEmpty = ev.detail.value;
    }

    get selectedData() {
        return this._chartSelection;
    }

    set selectedData(selection) {
        this.$.chart.selectData(selection);
    }

    // The core chart has changed the selection
    _onSelectionChanged(ev) {
        this._setChartSelection(ev.detail.selection);
    }

    _onZoomSelection(ev) {
        // The combo chart simulates an y-scale from 0 to 100 that specifies the current zooming
        const invertYscale = v => {
            const [y1, y2] = [this.yZoomStart || 0, this.yZoomEnd || 100];
            const d1 = this.flipAxes ? this._graphWidth : this._graphHeight;
            const d2 = Math.abs(y2 - y1);
            return y1 + d2 * (d1 - v) / d1;
        };

        const xScale = this._xScale;
        const yScale = {invert: invertYscale};
        let reverseXAxis = this.reverseXAxis;
        let reverseYAxis = this.reverseYAxis;
        let d = ev.detail;
        if (this.flipAxes) {
            d = {x: d.y, y: d.x, w: d.h, h: d.w};
            reverseXAxis = !reverseXAxis;
            reverseYAxis = !reverseYAxis;
        }
        if (this.xZoomDrag || this.xZoomSelect) {
            [this.xZoomStart, this.xZoomEnd] = reverseXAxis
                ? invertScaleRange(xScale, d.x + d.w, d.x) : invertScaleRange(xScale, d.x, d.x + d.w);
        }
        if (this.yZoomDrag || this.yZoomSelect) {
            const [start, end] = reverseYAxis // default y-axis is reversed
                ? invertScaleRange(yScale, d.y, d.y + d.h) : invertScaleRange(yScale, d.y + d.h, d.y);
            this.yZoomStart = start > 0 ? start : undefined;
            this.yZoomEnd = end < 100 ? end : undefined;
        }
    }

    refreshData() {
        this._dataChanged(this.data, this.xType);
    }

    _resetToDefaultValues() {
        this.$.legend._resetToDefaultValues();
        this.$.zoomX._resetToDefaultValues();
        this.$.zoomY._resetToDefaultValues();
    }

    _dataChanged(data, xType) {
        if (!Array.isArray(data)) {
            this._xType = this._xMin = this._xMax = undefined;
            return;
        }
        try {
            if (xType === 'label' || !xType) {
                this._xType = [...new Set(data.map(__xv))];
                this._xMin = this._xType[0];
                this._xMax = this._xType[this._xType.length - 1];
            } else if (xType === 'number') {
                const ax = data.map(item => item[0]);
                this._xType = xType;
                this._xMin = Math.min(...ax);
                this._xMax = Math.max(...ax);
            } else if (xType === 'date') {
                const ax = data.map(item => item[0].getTime());
                this._xType = xType;
                this._xMin = new Date(Math.min(...ax));
                this._xMax = new Date(Math.max(...ax));
            } else if (Array.isArray(xType) && xType.every(tick => typeof tick === 'string')) {
                this._xType = [...new Set(xType)];
                this._xMin = this._xType[0];
                this._xMax = this._xType[this._xType.length - 1];
            } else {
                this._xType = this._xMin = this._xMax = undefined;
                console.warn('Unknown xType: ' + xType);
            }
        } catch (e) {
            this._xType = this._xMin = this._xMax = undefined;
        }
    }

    // Inform drawables about x-zoom factor (filter out unmapped x-values)
    _setXZoom() {
        const zoomStart = this.xZoomStart !== undefined && this.xZoomStart !== null && this.xZoomStart !== this._xMin;
        const zoomEnd = this.xZoomEnd !== undefined && this.xZoomEnd !== null && this.xZoomEnd !== this._xMax;
        const xScale = this._xScale;
        const sampleSize = (this.sampleSize >= 0 && this.sampleSize !== '') ? Number(this.sampleSize) : undefined;
        const drawables = [...this._drawables.bar, ...this._drawables.line, ...this._drawables.area];
        const axisMap = this._drawables.axisMap;
        const toZeroScale = () => 0;
        const yScale = axisMap ? drawable => (this._ayScale[axisMap.get(drawable)] || toZeroScale) : () => toZeroScale;

        if (zoomStart || zoomEnd) {
            drawables.forEach(drawable => drawable.zoom(xScale, sampleSize, yScale(drawable)));
        } else {
            drawables.forEach(drawable => drawable.unZoom(xScale, sampleSize, yScale(drawable)));
        }
    }

    /**
     *  Show chart bands only when the next conditions are met:
     *  - chart is ready (has the scaling) and the user chose to show chart bands
     *  - chart has bar/stacked bar series
     *  - bars are wider than 6 px
     *  - inner padding is greater than 1px
     **/
    _updateChartBands(xScale, _hasVisibleBars, showChartBands) {
        const visibleBars = this._drawables.bar.filter(el => !el.hidden);

        this._showChartBands = showChartBands && xScale && xScale.bandwidth &&
            visibleBars.length > 0 &&
            visibleBars[0].barSize(xScale).barW > 6 &&
            xScale.step() * xScale.paddingInner() > 1;

        this._innerPadding = this._showChartBands ? xScale.step() * xScale.paddingInner() : undefined;
        this._outerPadding = this._showChartBands ? xScale.step() * xScale.paddingOuter() : undefined;
        this._bandwidth = this._showChartBands ? xScale.bandwidth() : undefined;
    }

    _xScaleChanged() {
        this._setXZoom();
    }

    _sampleSizeChanged() {
        if (this._xScale) {
            this._setXZoom();
            this.$.chart.requestUpdate('xScale');
        }
    }

    _hideXAxisChanged(hide) {
        if (!hide) {
            this.$.xaxis.refresh();
        }
    }

    _isHiddenAxis(_minValue, _maxValue, hide) {
        return (_minValue === null && _maxValue === null) || hide;
    }

    _showZero1(index) {
        return !this.hideZeroRuler && this._primaryYAxes[index] === this._primaryAxis;
    }

    _showZero2(index) {
        return this.showZeroRuler2 && this._secondaryYAxes[index] === this._secondaryAxis;
    }

    _curve(series) {
        const f = curve[series.curve];
        return f ? f.call(series) : curveLinear;
    }

    _alignBarAxes() {
        if (this.alignBarsAtZero) {
            const stacks = Array.isArray(this.stacks) ? this.stacks : [];
            const legend = Array.isArray(this.legend) ? this.legend : [];

            // Collect axes that render bars
            const barAxes = legend.reduce((set, series) => {
                const renderOn = series.renderOn;
                const stack = stacks.find(s => s.id === renderOn);
                if (stack) {
                    if (stack.curve === 'bar') {
                        set.add(stack.yaxis);
                    }
                } else if (series.curve === 'bar') {
                    set.add(renderOn);
                }
                return set;
            }, new Set());

            // Is a value?
            const isv = s => s !== undefined && s !== null && s !== '' && s !== false;

            // Collect min and max values of bar rendering axes that includes the zero value
            const av = [...this._primaryYAxes, ...this._secondaryYAxes].reduce((acc, yaxis) => {
                if (yaxis.type === 'number' && barAxes.has(yaxis.id)) {
                    const [_minValue, _maxValue] = this._yAxesValues[yaxis.id] || [null, null];
                    if (isv(_minValue) && _minValue <= 0 && isv(_maxValue) && _maxValue >= 0) {
                        acc.push([_minValue, _maxValue, yaxis.id]);
                    }
                }
                return acc;
            }, []);

            // Compute relative size of the negative side of the axis
            const avgK = av.reduce((acc, r) => acc + r[0] / (r[0] - r[1]), 0) / av.length;

            // Assign values to axis
            const f = axisGroup => (axis, i) => {
                const r = av.find(r2 => r2[2] === axis.id);

                if (r) {
                    axisGroup[i]._type = 'number';
                    const k = r[0] / (r[0] - r[1]);
                    if (k > avgK) {
                        // Increase max value
                        axisGroup[i]._minValue = r[0];
                        axisGroup[i]._maxValue = r[1] + ((avgK - 1) * r[0] - avgK * r[1]) / avgK;
                    } else if (k < avgK) {
                        // Increase min value
                        axisGroup[i]._minValue = r[0] + ((avgK - 1) * r[0] - avgK * r[1]) / (1 - avgK);
                        axisGroup[i]._maxValue = r[1];
                    } else {
                        axisGroup[i]._minValue = r[0];
                        axisGroup[i]._maxValue = r[1];
                    }
                    this.requestUpdate();
                } else {
                    const vv = this._yAxesValues[axis.id] || {};
                    if (vv) {
                        axisGroup[i]._type = vv[2] || this[axisGroup][i].type;
                        axisGroup[i]._minValue = vv[0];
                        axisGroup[i]._maxValue = vv[1];
                        this.requestUpdate();
                    }
                }
            };

            this._primaryYAxes.forEach(f(this._primaryYAxes));
            this._secondaryYAxes.forEach(f(this._secondaryYAxes));
        } else {
            const f = axisGroup => (axis, i) => {
                const vv = this._yAxesValues[axis.id] || {};
                if (vv) {
                    axisGroup[i]._type = vv[2] || axisGroup[i].type;
                    axisGroup[i]._minValue = vv[0];
                    axisGroup[i]._maxValue = vv[1];
                    this.requestUpdate();
                }
            };

            this._primaryYAxes.forEach(f(this._primaryYAxes));
            this._secondaryYAxes.forEach(f(this._secondaryYAxes));
        }
    }

    _alignBarsAtZeroChanged() {
        this._alignBarAxes();
    }

    _updateYRanges() {
        // Compute min / max
        if (!Array.isArray(this.yAxes)) {
            return;
        }

        const axisMap = this._drawables.axisMap;
        const drawables = [...this._drawables.bar, ...this._drawables.line, ...this._drawables.area];

        this.yAxes.forEach(axis => {
            let yMin = null;
            let yMax = null;
            let ySet = null;

            const drawList = drawables.filter(d => axis.id === axisMap.get(d));

            switch (axis.type) {
                case 'number':
                case 'date': {
                    drawList.forEach(drawObj => drawObj.eachY(y => {
                        if (yMin > y || yMin === null) {
                            yMin = y;
                        }
                        if (yMax < y || yMax === null) {
                            yMax = y;
                        }
                    }));
                    break;
                }

                case 'label': {
                    if (!ySet) {
                        ySet = new Set();
                    }
                    drawList.forEach(drawObj => drawObj.eachY(y => ySet.add(typeof y === 'string' ? y : `${y}`)));
                    break;
                }

                default:
                    if (Array.isArray(axis.type) && axis.type.every(tick => typeof tick === 'string')) {
                        console.error('LIST OF TICK STRINGS axis');
                    } else {
                        console.error('Unknown axis type');
                    }
            }

            if (ySet) {
                const yValues = [...ySet];
                const v = index => yValues[index] !== undefined ? yValues[index] : null;
                this._yAxesValues[axis.id] = [v(0), v(yValues.length - 1), yValues];
            } else {
                this._yAxesValues[axis.id] = [yMin, yMax];
            }
        });

        // Align yaxes that displays bar, if neeed, and then assign the new values to displayed axes
        this._alignBarAxes();

        // Tell axis selector about available axes
        const r = [];
        const f = axis => !this._isHiddenAxis(axis._minValue, axis._maxValue, false) && r.push(axis.id);
        this._primaryYAxes.forEach(f);
        this._secondaryYAxes.forEach(f);
        this._enabledYAxes = r;

        // Refresh chart
        this.$.chart.requestUpdate('yScales');
    }

    _yAxesChanged() {
        const hide = Array.isArray(this._showYAxes) ? i => !(this._showYAxes.indexOf(i) >= 0) : () => false;
        const sepName = {primary: 'primary', secondary: 'secondary'};
        const sepAxes = {primary: [], secondary: []};
        if (Array.isArray(this.yAxes)) {
            this.yAxes.forEach((axis, index) => {
                const where = sepName[axis.position] || (index ? 'secondary' : 'primary');
                sepAxes[where].push({...axis, _type: axis.type, hide: hide(index)});
            });
        }

        this.__yAxesTicks = {};
        this._primaryYAxes = sepAxes.primary;
        this._secondaryYAxes = sepAxes.secondary;

        // TEMPORARY HACK!
        this.$['display-axis']._axesChanged();
    }

    _yAxesMaxSize(yAxesMaxWidth, flipAxes) {
        const size = /^\d+$/.test(yAxesMaxWidth) ? `${yAxesMaxWidth}px` : yAxesMaxWidth;

        const assign = id => {
            const el = this.$[id];

            // Make sure old assignement is cleared (if yAxesMaxWidth is invalid)
            el.style.maxWidth = '';
            el.style.maxHeight = '';

            if (flipAxes) {
                el.style.maxHeight = size;
            } else {
                el.style.maxWidth = size;
            }
        };

        assign('yaxis-container');
        assign('yaxis-container2');
        assign('below-yaxis');
        assign('below-yaxis2');
    }

    _chartChanged(/* yAxes.*, data, legend, stacks */) {
        const legend = Array.isArray(this.legend) ? this.legend : [];
        const data = Array.isArray(this.data) ? this.data : [];
        const alwaysNull = () => null;
        const id2yAxis = Array.isArray(this.yAxes) ? id => this.yAxes.find(axis => axis.id === id) : alwaysNull;
        const id2stack = Array.isArray(this.stacks) ? id => this.stacks.find(_stack => _stack.id === id) : alwaysNull;
        const zIndex = series => isNaN(+series.zIndex) ? 0 : +series.zIndex;

        // Index to every x-value: [0, 1, ..., N - 1];
        let _allIxs;

        // Don't duplicate _allIxs array
        const allIxs = () => {
            if (!_allIxs) {
                _allIxs = Array.from(data, (_, index) => index);
            }
            return _allIxs;
        };

        const drawables = {bar: [], area: [], line: [], axisMap: new Map()};

        const mapStack = new Map();

        // Bind Drawable / Chart graph to yAxis
        const bindToYAxis = (drawable, yAxis) => {
            drawable.setSelectedSeries(this._selectedLegend);

            if (drawable && yAxis.id) {
                drawables.axisMap.set(drawable, yAxis.id);
            }
            return drawable;
        };

        // Process series (map to axes or stacks)
        legend.forEach((series, seriesIx) => {
            const renderOnStack = id2stack(series.renderOn);
            const renderOnYAxis = !renderOnStack && id2yAxis(series.renderOn);

            if (renderOnStack) {
                const stackList = mapStack.get(renderOnStack);
                if (stackList) {
                    stackList.push(seriesIx);
                } else {
                    mapStack.set(renderOnStack, [seriesIx]);
                }
            } else if (renderOnYAxis) {
                const valueFormat = series.showValues &&
                   (series.valueFormat || id2yAxis(series.renderOn).yChartValueFormat || id2yAxis(series.renderOn).tickFormat);
                if (series.curve === 'bar') {
                    drawables.bar.push(bindToYAxis(
                        new DrawBar(seriesIx, data, allIxs, +this.groupPadding, series.showValues, valueFormat, zIndex(series)),
                        renderOnYAxis));
                } else if (series.showArea) {
                    drawables.area.push(bindToYAxis(new DrawArea(
                        seriesIx, data, allIxs, this._curve(series), series.showLine,
                        series.marker, series.markerSize, series.showValues, valueFormat, zIndex(series)), renderOnYAxis));
                } else if (series.showLine) {
                    drawables.line.push(bindToYAxis(new DrawLine(
                        seriesIx, data, allIxs, this._curve(series),
                        series.marker, series.markerSize, series.showValues, valueFormat, zIndex(series)), renderOnYAxis));
                } else if (markersSet.has(series.marker)) {
                    drawables.line.push(bindToYAxis(new DrawPlot(
                        seriesIx, data, allIxs,
                        series.marker, series.markerSize, series.showValues, valueFormat, zIndex(series)), renderOnYAxis));
                }
            }
        });

        // Create stacked data
        mapStack.forEach((seriesIxs, stackOn) => {
            const renderOnYAxis = id2yAxis(stackOn.yaxis);
            if (!renderOnYAxis || renderOnYAxis.type !== 'number') {
                console.error('Data can only be stacked on numeric y-axis: ' + stackOn.yaxis);
                return;
            }

            const showValues = seriesIxs.map(i => legend[i].showValues);
            const formatValues = seriesIxs.map(i => PTCS.formatValue(legend[i].valueFormat || renderOnYAxis.tickFormat));
            const zI = Math.max(...seriesIxs.map(i => zIndex(legend[i])));

            if (stackOn.curve === 'bar') {
                drawables.bar.push(bindToYAxis(new DrawStackedBars( // Stacked bars must use "diverging", or they will fail to display negative values
                    seriesIxs, data, 'diverging', stackOn.order, +this.groupPadding, showValues, formatValues, zI, stackOn.showSum), renderOnYAxis));
            } else {
                const markers = seriesIxs.map(i => [legend[i].marker, legend[i].markerSize]);
                drawables.area.push(bindToYAxis(new DrawStackedAreas(
                    seriesIxs, data, stackOn.method, stackOn.order, this._curve(stackOn), markers, showValues, formatValues, zI), renderOnYAxis));
            }
        });

        // Assign legend icons
        this._createLegend(legend, drawables);

        // Inform bars about their available bandwidth
        drawables.bar.filter(bar => !bar.hidden).forEach((bar, index, a) => bar.setBand(index, a.length));

        // Publish new drawables
        this._drawables = drawables;

        // Compute min / max for all y-axes
        this._updateYRanges();

        // Adjust the zoom factor (filter out unmapped x-values)
        this._setXZoom();
    }

    _createLegend(legend, drawables) {
        const allDrawables = [...drawables.bar, ...drawables.area, ...drawables.line];
        const axisMap = drawables.axisMap;

        this._legend = legend.map((series, seriesIx) => {
            const drawable = allDrawables.find(d => d.displaysSeries(seriesIx));
            if (!drawable) {
                // Not mapped to any axis
                return {...series, empty: true};
            }
            const id = axisMap.get(drawable);
            const yaxis = this.yAxes.find(axis => axis.id === id);
            const label = yaxis && yaxis.label;
            const icon = `chart-icons:${drawable.chartType}`;
            return {...series, icon, group: {id, label}};
        });
    }

    _computeSelectedLegend(_selectedLegend$, legend) {
        if (Array.isArray(_selectedLegend$)) {
            return _selectedLegend$;
        }
        if (Array.isArray(legend)) {
            return legend.map((_, i) => i);
        }
        return [];
    }

    _selectedLegendChanged(_selectedLegend) {
        const selectedSeries = Array.isArray(_selectedLegend) ? _selectedLegend : [];

        // Inform all drawables about the new legend filter
        [...this._drawables.bar, ...this._drawables.line, ...this._drawables.area]
            .forEach(d => d.setSelectedSeries(selectedSeries));

        // Inform bars about their available bandwidth
        this._drawables.bar.filter(bar => !bar.hidden).forEach((bar, index, a) => bar.setBand(index, a.length));

        // The "show bar bands" component needs to be nudged when bars become visible / hidden
        this._hasVisibleBars = this._drawables.bar.some(bar => !bar.hidden);

        // Adapt y-axis ranges
        this._updateYRanges();

        // Make sure the changes are displayed
        this.$.chart.requestUpdate('drawables');
    }

    _groupPaddingChanged(groupPadding) {
        if (this._drawables.bar.length) {
            this._drawables.bar.forEach(drawable => drawable.setPadding(+groupPadding));
            this.$.chart.requestUpdate('drawables');
        }
    }

    // This function simulates a scale function for the reference lines
    // The value argument is an index into _yAxisReferenceLines
    // The return value is the current scale value of the reference lines
    __yScaleReferenceLines(value) {
        try {
            const line = Array.isArray(this._yAxisReferenceLines) && this._yAxisReferenceLines[value];
            if (!line || !(this._primaryYAxes.some(a => a.id === line.axis) || this._secondaryYAxes.some(a => a.id === line.axis))) {
                return undefined; // Axis is not in use
            }
            const scale = this._ayScale[line.axis];
            const d = scale.domain()[0];

            if (typeof d === 'number') {
                return scale(typeof line._value === 'number' ? line._value : Number(line._value));
            }
            if (d instanceof Date) {
                return scale(line._value instanceof Date ? line._value : new Date(line._value));
            }
            return scale(line._value);
        } catch (e) {
            // Ignore error. Many things can go wrong above...
        }
        return undefined;
    }

    // Request to recompute _isYReferenceLines
    _updateIsYReferenceLines() {
        if (this._computeIsReferenceLines) {
            return;
        }
        this._computeIsReferenceLines = true;
        requestAnimationFrame(() => {
            this._computeIsReferenceLines = undefined;
            this._isYReferenceLines = Array.isArray(this._yAxisReferenceLines) &&
                this._yAxisReferenceLines.some(item => item._value !== undefined && item.axis);

            // Update scale, so axis is updated
            const reflinesEl = this.$['yaxis-container2'].querySelector(':scope > [is-reference-lines]');
            if (reflinesEl) {
                reflinesEl.refresh();
            }
        });
    }

    _referenceLinesChanged(referenceLines, _xType) {
        const reset = v => v ? undefined : v; // Keep old value, if falsy

        if (!Array.isArray(referenceLines)) {
            this._xAxisReferenceLines = reset(this._xAxisReferenceLines);
            this._yAxisReferenceLines = reset(this._yAxisReferenceLines);
            this._isXReferenceLines = reset(this._isXReferenceLines);
            this._isYReferenceLines = reset(this._isYReferenceLines);
            return;
        }

        const xAxisReferenceLines = [];
        const yAxisReferenceLines = [];

        referenceLines.forEach((line, index) => {
            if (line.axis === 'xaxis') {
                if (_xType === 'number') {
                    if (!isNaN(line.value)) {
                        xAxisReferenceLines.push(line);
                    }
                } else if (_xType === 'date') {
                    const d = line.value instanceof Date ? line.value : new Date(line.value);
                    if (!isNaN(d)) {
                        xAxisReferenceLines.push({...line, value: d});
                    }
                }
            } else {
                yAxisReferenceLines.push({label: line.label, _value: line.value, axis: line.axis, value: yAxisReferenceLines.length});
            }
        });

        this._xAxisReferenceLines = xAxisReferenceLines.length && xAxisReferenceLines;
        this._yAxisReferenceLines = yAxisReferenceLines.length && yAxisReferenceLines;
        this._isXReferenceLines = xAxisReferenceLines.length > 0;
        this._updateIsYReferenceLines();
    }

    // The scale for axisId has changed
    _updateYReferenceValues(axisId) {
        if (!Array.isArray(this._yAxisReferenceLines)) {
            return;
        }
        if (this._yAxisReferenceLines.some(line => line.axis === axisId)) {
            // Need to recalculate _isYReferenceLines
            this._updateIsYReferenceLines();
        }
    }

    _yAxisRulersChange() {
        if (this.__yAxisRulersChange$) {
            return;
        }
        this.__yAxisRulersChange$ = true;
        requestAnimationFrame(() => {
            this.__yAxisRulersChange$ = undefined;

            const isVisible = axis => !this._isHiddenAxis(axis._minValue, axis._maxValue, axis.hide);
            const first = list => list && list.find(isVisible);
            const last = list => list && list.findLast(isVisible);
            const [_primaryAxis, _secondaryAxis] = this.flipYAxes
                ? [first(this._primaryYAxes), last(this._secondaryYAxes)]
                : [last(this._primaryYAxes), first(this._secondaryYAxes)];

            if (_primaryAxis && _primaryAxis.id) {
                this._yTicks = this.__yAxesTicks[_primaryAxis.id];
                this._yScale = this._ayScale[_primaryAxis.id];
            } else {
                this._yTicks = this._yScale = undefined;
            }

            if (_secondaryAxis && _secondaryAxis.id) {
                this._y2Ticks = this.__yAxesTicks[_secondaryAxis.id];
                this._y2Scale = this._ayScale[_secondaryAxis.id];
            } else {
                this._y2Ticks = this._y2Scale = undefined;
            }

            if (this._isYReferenceLines) {
                // Simulate a changed reference lines axis (__yScaleReferenceLines depends on the y-axes)
                this._yScaleReferenceLines = this.__yScaleReferenceLines.bind(this);
            }

            this.setProperties({_primaryAxis, _secondaryAxis});
        });
    }

    _primaryYTicksChanged(ev) {
        if (ev.target.axisId && this.__yAxesTicks) {
            this.__yAxesTicks[ev.target.axisId] = ev.detail.value;
            this._yAxisRulersChange();
        }
    }

    _primaryYScaleChanged(ev) {
        this._ayScale[ev.target.axisId] = ev.detail.value;
        this._updateYReferenceValues(ev.target.axisId);
        this.$.chart.requestUpdate('yScales');
    }

    _secondaryYTicksChanged(ev) {
        if (ev.target.axisId && this.__yAxesTicks) {
            this.__yAxesTicks[ev.target.axisId] = ev.detail.value;
            this._yAxisRulersChange();
        }
    }

    _secondaryYScaleChanged(ev) {
        this._ayScale[ev.target.axisId] = ev.detail.value;
        this._updateYReferenceValues(ev.target.axisId);
        this.$.chart.requestUpdate('yScales');
    }

    _hideY1AxisChanged(hideY1Axis) {
        if (!hideY1Axis) {
            this.shadowRoot.querySelectorAll('#yaxis-container [part=yaxis]').forEach(yaxis => yaxis.refresh());
        }
    }

    _hideY2AxisChanged(hideY2Axis) {
        if (!hideY2Axis) {
            this.shadowRoot.querySelectorAll('#yaxis-container2 [part=yaxis]').forEach(yaxis => yaxis.refresh());
        }
    }

    _axisDisplayControlChanged(axisDisplayControl) {
        this._showAxisDisplayButton = axisDisplayControl;
    }

    _showDisplayAxisChanged(_showDisplayAxis) {
        this.$.toolbar.setSelected('axis-display-button', _showDisplayAxis);

        if (!_showDisplayAxis) {
            this.__showDisplayAxisClosed = Date.now();
        }

        if (this.hasAttribute('tabindex')) {
            requestAnimationFrame(() => this.$[this._showDisplayAxis ? 'display-axis' : 'toolbar'].focus());
        }
    }

    _resized() {
        this._showDisplayAxis = false;
    }

    _showAxisDisplay(buttonRect) {
        // A hack to stop the toolbar from reopening the reorder menu if clicking on the Display toolbar button
        if (Date.now() - this.__showDisplayAxisClosed < 200) {
            // User clicked on display button to close the menu. Don't reopen the menu
            return;
        }

        const r = this.getBoundingClientRect();
        const dlg = this.$['display-axis'];

        this._showDisplayAxis = true;
        dlg.style.top = `${buttonRect.bottom - r.top + 8}px`;
        dlg.style.left = `${buttonRect.left - r.left}px`;
        const r2 = dlg.getBoundingClientRect();
        if (r2.right > r.right) {
            dlg.style.left = `${Math.max(0, buttonRect.left - r.left - (r2.right - r.right))}px`;
        }
    }

    _showYAxesChanged(_showYAxes) {
        const set = new Set((_showYAxes || []).map(i => this.yAxes[i].id));

        this._primaryYAxes.forEach((axis, i) => {
            this._primaryYAxes[i].hide = !set.has(axis.id);
        });

        this._secondaryYAxes.forEach((axis, i) => {
            this._secondaryYAxes[i].hide = !set.has(axis.id);
        });

        this._yAxisRulersChange();
        this.requestUpdate();

        // Axes sometimes needs a push to find the correct size
        requestAnimationFrame(() => [...this.$['chart-layout'].querySelectorAll('[part=yaxis]')].forEach(el => el.refresh()));
    }

    // Axis-below to Axis-container
    _b2c(el) {
        return this.$[{'below-yaxis': 'yaxis-container', 'below-yaxis2': 'yaxis-container2'}[el.getAttribute('id')]].firstElementChild;
    }

    // Axis-container to Axis-below
    _c2b(el) {
        return this.$[{'yaxis-container': 'below-yaxis', 'yaxis-container2': 'below-yaxis2'}[el.closest('[slot]').getAttribute('id')]];
    }

    // When the axis container is resized (align external scrollbar)
    _onResizeYAxes(entries) {
        // Avoid the darned "ResizeObserver loop limit exceeded" (benign) warning that cuases the unit tests to fail
        requestAnimationFrame(() => entries.forEach(e => {
            this._c2b(e.target).firstElementChild.style.width = this.flipAxes ? '' : `${e.contentRect.width}px`;
        }));
    }

    // When the (external) axis scroller is manipulated (move axis container according to scroller)
    _onScrollYAxes(ev) {
        const sb = ev.target;
        this._b2c(sb).style.transform = (!this.flipAxes && sb.scrollLeft) ? `translateX(-${sb.scrollLeft}px)` : '';
    }

    // Tablet touch start
    _onTouchstartYAxes(ev) {
        const {target, pageX} = ev.targetTouches[0];
        this._touch = [pageX, this._c2b(target).scrollLeft];
    }

    // Tablet touch move
    _onTouchmoveYAxes(ev) {
        const {target, pageX} = ev.targetTouches[0];
        this._c2b(target).scrollLeft = this._touch[1] + this._touch[0] - pageX;
        ev.preventDefault();
    }
};

customElements.define(PTCS.ChartCombo.is, PTCS.ChartCombo);
