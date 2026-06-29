import {LitElement, html, css} from 'lit';
import {ifDefined} from 'lit/directives/if-defined.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import {axisMin, axisMax, typeIsFullRange, typeValue, invTypeValue} from 'ptcs-library/library-chart.js';
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
import '../axes/ptcs-chart-axis.js';
import '../zoom/ptcs-chart-zoom.js';
import './ptcs-chart-core-pareto.js';

const yType = 'number';
const observeYzoomProperties = ['_yMin', '_yMax', 'yZoomStart', 'yZoomEnd', 'specYMin', 'specYMax', 'specYMax', 'specY2Min', 'specY2Max'];

PTCS.ChartPareto = class extends BehaviorChart(PTCS.BehaviorTabindex(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))))) {
    static get styles() {
        return css`
        :host {
            display: block;
        }

        :host([disabled]) {
            pointer-events: none;
        }

        ptcs-chart-axis, [part=legend-area] {
            width: 100%;
            height: 100%;
        }

        [part=chart] {
            position: relative;
        }`;
    }

    render() {
        return html`
        <ptcs-chart-layout id="chart-layout" style="height:100%" part="chart-layout"
                           .disabled=${this.disabled}
                           .titlePos=${this.titlePos} .hideTitle=${!this.titleLabel}
                           .notesPos=${this.notesPos} .notesAlign=${this.notesAlign} .hideNotes=${this._hideNotes()}
                           .legendPos=${this.legendPos} .hideLegend=${this._hideLegend()}
                           @eff-legend-pos-changed=${this._effLegendPosChangedEv}
                           .xZoom=${this._showZoomX()} .yZoom=${this._showZoomY()}
                           .flipAxes=${this.flipAxes} .flipXAxis=${this.flipXAxis} .flipYAxis=${this.flipYAxis}
                           .sparkView=${this.sparkView}
                           .xAxis=${!this.hideXAxis} .yAxis=${!this.hideYAxis} .yAxis2=${this.showY2Axis}
                           .actionBar=${ifDefined(this._actionBar())}
                           .chartState=${this._chartState}>
            <div part="title-area" slot="title" style=${'text-align:' + this._getHorizontalAlignment(this.titlePos, this.titleAlign)}>
                <ptcs-label part="title-label" variant=${this.titleVariant} .label=${this.titleLabel}
                    .horizontalAlignment=${this._getHorizontalAlignment(this.titlePos, this.titleAlign)} multi-line></ptcs-label>
            </div>
            <div part="notes-area" slot="notes"
                style=${'text-align:' + this._getHorizontalAlignment(this.notesPos, this.notesAlign)}>
                <ptcs-label part="notes-label" variant="label" .label=${this.notesLabel}
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
            <ptcs-chart-coord slot="chart" part="chart"
                .flipAxes=${this.flipAxes} .flipXAxis=${this.flipXAxis} .flipYAxis=${this.flipYAxis}
                .xTicks=${this._xTicks} .yTicks=${this._yTicks} .y2Ticks=${this._y2Ticks} .hasY2=${this.showY2Axis}
                .showXRulers=${this.showXRulers} .showYRulers=${this._showYRulers()} .showY2Rulers=${this._showY2Rulers()}
                .yAxisRulerAlignment=${this.yAxisRulerAlignment} .frontRulers=${this.frontRulers} .hideZeroRuler=${this.hideZeroRuler}
                .sparkView=${this.sparkView}
                @graph-width-changed=${this._graphWidthChangedEv} @graph-height-changed=${this._graphHeightChangedEv}>
                <ptcs-chart-core-pareto id="chart" slot="chart" part="core-chart" style="pointer-events:auto" exportparts="bar"
                    tabindex=${ifDefined(this._delegatedFocus)}
                    .disabled=${this.disabled}
                    .thresholdValue=${this.thresholdValue} .thresholdLine=${this.thresholdLine}
                    .emphasizeThresholdFactors=${this.emphasizeThresholdFactors}
                    .hideCumulativePercentage=${this._hideCumulative()}
                    .data=${this.data}
                    .legend=${this.legend}
                    .hideDataTooltips=${this.hideDataTooltips}
                    .tooltipTemplate=${this.tooltipTemplate} .tooltipTemplate2=${this.tooltipTemplate2}
                    .stackOrder=${this.stackOrder}
                    @x-type-changed=${this._labelsChangedEv}
                    @x-min-changed=${this._xMinChangedEv} @x-max-changed=${this._xMaxChangedEv}
                    @y-min-changed=${this._yMinChangedEv} @y-max-changed=${this._yMaxChangedEv}
                    .showValues=${this._showValues(this.showValues)}
                    .yValueFormat=${this.yChartValueFormat || this.yAxisNumberFormatSpecifier}
                    .curve=${this.curve} .bundleBeta=${this.bundleBeta} .cardinalTension=${this.cardinalTension}
                    .catmullRomAlpha=${this.catmullRomAlpha} .stepPosition=${this.stepPosition}
                    .flipAxes=${this.flipAxes} .reverseXAxis=${this.reverseXAxis} .reverseYAxis=${this.reverseYAxis}
                    .xScale=${this._xScale} .yScale=${this._yScale}
                    .filterLegend=${this._selectedLegend2}
                    .marker=${this._getMarker()} .markerSize=${this.markerSize}
                    .showMarkerValues=${this._showValues(this.showMarkerValues)} .markerValueFormat=${this.y2AxisNumberFormatSpecifier}
                    .showY2Axis=${this.showY2Axis}
                    .y2Min=${this._y2Min} .y2Max=${this._y2Max} .y2Scale=${this._y2Scale}
                    .zoomSelect=${this._zoomSelect()}
                    .zoomDragX=${this._zoomDrag(this.xZoomDrag, this.noXZoom)} .zoomDragY=${this._zoomDrag(this.yZoomDrag, this.noYZoom)}
                    .selectionMode=${this.selectionMode} .unselectable=${this.unselectable}
                    .sampleSize=${this.sampleSize}
                    @chart-selection=${this._onSelectionChanged}
                    @zoom-selection=${this._onZoomSelection}
                    @chart-state-data-error-changed=${this._chartStateDataErrorChangedEv}
                    @chart-state-data-empty-changed=${this._chartStateDataEmptyChangedEv}></ptcs-chart-core-pareto>
            </ptcs-chart-coord>
            <div part="action-bar-area" slot="action-bar">
                <ptcs-toolbar id="toolbar" variant="secondary" part="action-bar" tabindex=${ifDefined(this._gcTabindex())}
                    .disabled=${this.disabled} hide-filter @activated=${this._toolbarAction}>
                </ptcs-toolbar>
            </div>
            <div part="legend-area" slot="legend">
                <ptcs-chart-legend id="legend" part="legend"
                    tabindex=${ifDefined(this._tabindex())}
                    .items=${this._legendData}
                    .shape=${this.legendShape}
                    .filter=${this.filterLegend}
                    .horizontal=${this._horizLegend()}
                    .maxWidth=${this.legendMaxWidth}
                    .align=${this.legendAlign}
                    .disabled=${this.disabled}
                    @selected-changed=${this._selectedLegendChangedEv}></ptcs-chart-legend>
            </div>
            <ptcs-chart-zoom slot="xzoom" id="zoomX" part="zoom-xaxis" ?hidden=${this.noXZoom}
                tabindex=${ifDefined(this._delegatedFocus)}
                .disabled=${this.disabled}
                .type=${this._labels}
                .side=${this._xSide()}
                .axisLength=${this._xSize()}
                .minValue=${this._zoomMinX(this._xMin, this._xMax, this._labels, this.specXMin, this.specXMax)}
                .maxValue=${this._zoomMaxX(this._xMin, this._xMax, this._labels, this.specXMax, this.specXMin)}
                .zoomStart=${this.xZoomStart} @zoom-start-changed=${this._xZoomStartChangedEv}
                .zoomEnd=${this.xZoomEnd} @zoom-end-changed=${this._xZoomEndChangedEv}
                .rangePicker=${this._zoomArg(this.noXZoom, this.xZoomRange)}
                .interval=${this._zoomArg(this.noXZoom, this.xZoomInterval)}
                .intervalLabel=${this.xZoomIntervalLabel}
                .intervalControl=${this.xZoomIntervalControl}
                .intervalOrigin=${this.xZoomIntervalOrigin}
                .showIntervalAnchor=${this.xShowIntervalAnchor}
                .slider=${this._zoomArg(this.noXZoom, this.xZoomSlider)}
                .reverseSlider=${this.reverseXAxis}
                .sliderLabel=${this.xZoomSliderLabel}
                .sliderMinLabel=${this.xZoomSliderMinLabel}
                .sliderMaxLabel=${this.xZoomSliderMaxLabel}
                .rangeStartLabel=${this.xZoomRangeStartLabel}
                .rangeEndLabel=${this.xZoomRangeEndLabel}
                .intervalFromLabel=${this.xZoomIntervalFromLabel}
                .intervalToLabel=${this.xZoomIntervalToLabel}></ptcs-chart-zoom>
            <ptcs-chart-axis id="xaxis" slot="xaxis" part="xaxis" style="pointer-events:auto" no-tabindex
                .type=${this._labels}
                .specMin=${this._specValueMinX()} .specMax=${this._specValueMaxX()}
                .side=${this._xSide()}
                .label=${this.xAxisLabel} .alignLabel=${this.xAxisAlign}
                .minValue=${this._xMin} .maxValue=${this._xMax}
                .size=${this._xSize()} .maxSize=${this.flipAxes ? this.verticalAxisMaxWidth : this.horizontalAxisMaxHeight}
                @ticks-changed=${this._xTicksChangedEv}
                .ticksRotation=${this.horizontalTicksRotation}
                .reverse=${this.reverseXAxis}
                @scale-changed=${this._xScaleChangedEv}
                .outerPadding=${this.outerPadding}
                .innerPadding=${this.innerPadding}
                .numberFormatSpecifier=${this.xAxisNumberFormatSpecifier}
                .dateFormatToken=${this.xAxisDateFormatToken}
                ?hidden=${this.hideXAxis}></ptcs-chart-axis>
            <ptcs-chart-zoom slot="yzoom" id="zoomY" part="zoom-yaxis" ?hidden=${this.noYZoom}
                .disabled=${this.disabled}
                tabindex=${ifDefined(this._delegatedFocus)}
                type="number"
                .side=${this._ySide()}
                .axisLength=${this._ySize()}
                .minValue=${this._zoomMinY(this._yMin, this._yMax, this.specYMin, this.specYMax)}
                .maxValue=${this._zoomMaxY(this._yMin, this._yMax, this.specYMax, this.specYMin)}
                .zoomStart=${this.yZoomStart} @zoom-start-changed=${this._yZoomStartChangedEv}
                .zoomEnd=${this.yZoomEnd} @zoom-end-changed=${this._yZoomEndChangedEv}
                .rangePicker=${this._zoomArg(this.noYZoom, this.yZoomRange)}
                .interval=${this._zoomArg(this.noYZoom, this.yZoomInterval)}
                .intervalLabel=${this.yZoomIntervalLabel}
                .intervalControl=${this.yZoomIntervalControl}
                .intervalOrigin=${this.yZoomIntervalOrigin}
                .showIntervalAnchor=${this.yShowIntervalAnchor}
                .slider=${this._zoomArg(this.noYZoom, this.yZoomSlider)}
                .reverseSlider=${this.reverseYAxis}
                .sliderLabel=${this.yZoomSliderLabel}
                .sliderMinLabel=${this.yZoomSliderMinLabel}
                .sliderMaxLabel=${this.yZoomSliderMaxLabel}
                .intervalFromLabel=${this.yZoomIntervalFromLabel}
                .intervalToLabel=${this.yZoomIntervalToLabel}></ptcs-chart-zoom>
            <ptcs-chart-axis id="yaxis" slot="yaxis" part="yaxis" style="pointer-events: auto" no-tabindex
                type="number"
                .specMin=${this._specValueMinY(this.specYMin, this.specYMax, this.yZoomStart, this.noYZoom, this._yMin, this._yMax)}
                .specMax=${this._specValueMaxY(this.specYMin, this.specYMax, this.yZoomEnd, this.noYZoom, this._yMin, this._yMax)}
                .side=${this._ySide()}
                .label=${this.yAxisLabel} .alignLabel=${this.yAxisAlign}
                .minValue=${this._yMin} .maxValue=${this._yMax}
                .numTicks=${this.numberOfYLabels}
                .size=${this._ySize()} .maxSize=${this.flipAxes ? this.horizontalAxisMaxHeight : this.verticalAxisMaxWidth}
                @ticks-changed=${this._yTicksChangedEv}
                .ticksRotation=${this.horizontalTicksRotation}
                .reverse=${this.reverseYAxis}
                @scale-changed=${this._yScaleChangedEv}
                .numberFormatSpecifier=${this.yAxisNumberFormatSpecifier}
                .dateFormatToken=${this.yAxisDateFormatToken}
                ?hidden=${this.hideYAxis}></ptcs-chart-axis>
            <ptcs-chart-axis id="yaxis2" slot="yaxis2" part="yaxis2" style="pointer-events: auto"
                no-tabindex
                type="number"
                .specMin=${this._specValueMinY(this.specY2Min, this.specY2Max, this.y2ZoomStart, this.noYZoom, this._y2Min, this._y2Max)}
                .specMax=${this._specValueMaxY(this.specY2Min, this.specY2Max, this.y2ZoomEnd, this.noYZoom, this._y2Min, this._y2Max)}
                .side=${this._y2Side()}
                .label=${this.y2AxisLabel}
                .alignLabel=${this.y2AxisAlign}
                .minValue=${this._y2Min}
                .maxValue=${this._y2Max}
                .numTicks=${this.numberOfYLabels}
                .size=${this._ySize()}
                .maxSize=${this.flipAxes ? this.horizontalAxisMaxHeight : this.verticalAxisMaxWidth}
                .reverse=${this.reverseY2Axis}
                @scale-changed=${this._y2ScaleChangedEv}
                .numberFormatSpecifier=${this.y2AxisNumberFormatSpecifier}
                .dateFormatToken=${this.y2AxisDateFormatToken}
                @ticks-changed=${this._y2TicksChangedEv}
                .ticksRotation=${this.horizontalTicksRotation}
                ?hidden=${!this.showY2Axis}></ptcs-chart-axis>
        </ptcs-chart-layout>`;
    }

    static get is() {
        return 'ptcs-chart-pareto';
    }

    static get properties() {
        return {
            // Pareto threshold value (in %)
            thresholdValue: {
                type:      Number,
                attribute: 'threshold-value'
            },

            // 'horizontal' || 'vertical' || 'both' || 'none'
            thresholdLine: {
                type:      String,
                attribute: 'threshold-line'
            },

            emphasizeThresholdFactors: {
                type:      Boolean,
                attribute: 'emphasize-threshold-factors'
            },

            // Hide cumulative line via property
            hideCumulativePercentage: {
                type:      Boolean,
                attribute: 'hide-cumulative-percentage'
            },

            // Hide cumulative line via legend
            _hideCumulativePercentage: {
                type: Boolean
            },

            cumulativeLegend: {
                type:      String,
                attribute: 'cumulative-legend'
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

            // [left] || center || right
            titleAlign: {
                type:      String,
                attribute: 'title-align'
            },

            // Title label variant
            titleVariant: {
                type:      String,
                attribute: 'title-variant'
            },

            // Notes label
            notesLabel: {
                type:      String,
                attribute: 'notes-label'
            },

            hideNotes: {
                type:      Boolean,
                attribute: 'hide-notes'
            },

            hideDataTooltips: {
                type: Boolean
            },

            disabled: {
                type:    Boolean,
                reflect: true
            },

            // top || [bottom] || left || right
            notesPos: {
                type:      String,
                attribute: 'notes-pos'
            },

            // [left] || center || right
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

            // [left] || center || right
            xAxisAlign: {
                type:      String,
                attribute: 'x-axis-align'
            },

            hideXAxis: {
                type:      Boolean,
                observer:  '_hideXAxisChanged',
                attribute: 'hide-x-axis'
            },

            // Y-axis label
            yAxisLabel: {
                type:      String,
                attribute: 'y-axis-label'
            },

            // [left] || center || right
            yAxisAlign: {
                type:      String,
                attribute: 'y-axis-align'
            },

            hideYAxis: {
                type:      Boolean,
                observer:  '_hideYAxisChanged',
                attribute: 'hide-y-axis'
            },

            // Y-axis number of labels
            numberOfYLabels: {
                type:      Number,
                attribute: 'number-of-y-labels'
            },

            hideLegend: {
                type:      Boolean,
                notify:    true, // Can be toggled via button
                attribute: 'hide-legend'
            },

            // Names of legend items, if legend should be visible
            legend: {
                type: Array
            },

            _legendData: {
                type: Array
            },

            // top || bottom || left || [right]
            legendPos: {
                type:      String,
                attribute: 'legend-pos'
            },

            // The effective position of the legend may change because of overflow rules.
            // legendPos is the prefered position; effLegendPos is the actual position
            effLegendPos: {
                type:      String,
                attribute: 'eff-legend-pos'
            },

            legendAlign: {
                type:      String,
                attribute: 'legend-align'
            },

            // none || square || circle
            legendShape: {
                type:      String,
                attribute: 'legend-shape'
            },

            // Filter chart based on legend interaction?
            filterLegend: {
                type:      Boolean,
                attribute: 'filter-legend'
            },

            // Legend items currently selected in the legend component
            _selectedLegend: {
                type: Array
            },

            // Legend Filter sent to core-chart (excluding cumulativeLine)
            _selectedLegend2: {
                type: Array
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

            // Flip y-axis side
            flipYAxis: {
                type:      Boolean,
                attribute: 'flip-y-axis'
            },

            outerPadding: {
                type:      String,
                attribute: 'outer-padding'
            },

            innerPadding: {
                type:      String,
                attribute: 'inner-padding'
            },

            // Connects ticks from x-axis to chart
            _xTicks: {
                type: Array
            },

            // Connects ticks from y-axis to chart
            _yTicks: {
                type: Array
            },

            // Connects ticks from y-axis to chart
            _y2Ticks: {
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

            // Watches for resizes
            _graphWidth: {
                type: Number
            },

            // Watches for resizes
            _graphHeight: {
                type: Number
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

            // x-axis labels
            _labels: {
                type: Array
            },

            // Minimun x value in data
            _xMin: {
                type: Object
            },

            // Maximum x value in data
            _xMax: {
                type: Object
            },

            // Minimun y value in data
            _yMin: {
                type: Object
            },

            // Maximum y value in data
            _yMax: {
                type: Object
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
                type: Function,
            },

            // Move y-scale from y-axis to chart
            _yScale: {
                type: Function,
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

            // Label for the X-axis range dropdown START (FROM) value
            xZoomRangeStartLabel: {
                type:      String,
                attribute: 'x-zoom-range-start-label'
            },

            // Label for the X-axis range dropdown END (TO) value
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

            // X-Axis Zoom Slider Label
            xZoomSliderLabel: {
                type:      String,
                attribute: 'x-zoom-slider-label'
            },

            // X-Axis Zoom Slider Max Label
            xZoomSliderMaxLabel: {
                type:      String,
                attribute: 'x-zoom-slider-max-label'
            },

            // X-Axis Zoom Slider Min Label
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
                attribute: 'no-y-zoom'
            },

            // Is yAxis zoomed? (i.e. zoom is enabled AND the axis has been zoomed into)
            _yEnabled: {
                type: Boolean
            },

            // Zooming based on properties
            yZoomStart: {
                type:      Object,
                attribute: 'y-zoom-start'
            },

            yZoomEnd: {
                type:      Object,
                attribute: 'y-zoom-end'
            },

            // Show zoom range UI control
            yZoomRange: {
                type:      Boolean,
                attribute: 'y-zoom-range'
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

            // Show zoom slider
            yZoomSlider: {
                type:      Boolean,
                attribute: 'y-zoom-slider'
            },

            // Y-Axis Zoom Slider Label
            yZoomSliderLabel: {
                type:      String,
                attribute: 'y-zoom-slider-label'
            },

            // Y-Axis Zoom Slider Max Label
            yZoomSliderMaxLabel: {
                type:      String,
                attribute: 'y-zoom-slider-max-label'
            },

            // Y-Axis Zoom Slider Min Label
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

            yShowIntervalAnchor: {
                type:      Boolean,
                attribute: 'y-show-interval-anchor'
            },

            // Zooming of secondary y-axis - computed from the zooming of the primary y-axis
            y2ZoomStart: {
                type:      Object,
                attribute: 'y2-zoom-start'
            },

            y2ZoomEnd: {
                type:      Object,
                attribute: 'y2-zoom-end'
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
                attribute: 'x-zoom-interval-to-label'
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
                attribute: 'vertical-axis-max-width'
            },

            horizontalAxisMaxHeight: {
                type:      Number,
                attribute: 'horizontal-axis-max-height'
            },

            horizontalTicksRotation: {
                type:      Number,
                attribute: 'horizontal-ticks-rotation'
            },

            // Specified chart data
            data: {
                type: Array
            },

            // 'inside' || 'outside' || 'inside-end'
            showValues: {
                type:      String,
                attribute: 'show-values'
            },

            // Hide all values (bar values and marker values)
            hideValues: {
                type:      Boolean,
                attribute: 'hide-values'
            },

            // Stack order: auto || reverse || appearance || ascending || descending || insideout
            stackOrder: {
                type:      String,
                attribute: 'stack-order'
            },

            // linear || basis || bundle || cardinal || catmull-rom || monotone-x || monotone-y || natural || step
            curve: {
                type: String,
            },

            // when curve === bundle
            bundleBeta: {
                type:      Number,
                attribute: 'bundle-beta'
            },

            // when curve === cardinal
            cardinalTension: {
                type:      Number,
                attribute: 'cardinal-tension'
            },

            // when curve === catmull-rom
            catmullRomAlpha: {
                type:      Number,
                attribute: 'catmull-rom-alpha'
            },

            // when curve === step
            stepPosition: {
                type:      String, // center || before || after
                attribute: 'step-position'
            },

            // Markers on the percentage line
            hideMarkers: {
                type:      Boolean,
                attribute: 'hide-markers'
            },

            // none || square || circle || triangle || plus || cross
            marker: {
                type: String
            },

            // small || medium || large || xlarge || <number>
            markerSize: {
                type:      String,
                attribute: 'marker-size'
            },

            // 'no' || 'above' || 'on' || 'below'
            showMarkerValues: {
                type:      String,
                attribute: 'show-marker-values'
            },

            hideZeroRuler: {
                type:      Boolean,
                attribute: 'hide-zero-ruler'
            },

            xAxisNumberFormatSpecifier: {
                type:      String,
                attribute: 'x-axis-number-format-specifier'
            },

            xAxisDateFormatToken: {
                type:      String,
                attribute: 'x-axis-date-format-token'
            },

            // Format specifier for ticks (and for values on the chart if yChartValueFormat is undefined)
            yAxisNumberFormatSpecifier: {
                type:      String,
                attribute: 'y-axis-number-format-specifier'
            },

            // Format specifier for the values on the chart
            yChartValueFormat: {
                type:      String,
                attribute: 'y-chart-value-format'
            },

            yAxisDateFormatToken: {
                type:      String,
                attribute: 'y-axis-date-format-token'
            },

            // Secondary y-axis
            showY2Axis: {
                type:      Boolean,
                observer:  '_showY2AxisChanged',
                attribute: 'show-y2-axis'
            },

            reverseY2Axis: {
                type:      Boolean,
                attribute: 'reverse-y2-axis'
            },

            // Move y2-scale from y2-axis to chart2
            _y2Scale: {
                type: Function,
            },

            // Secondary y-axis starts at 0%
            _y2Min: {
                type:     Number,
                readOnly: true
            },

            // Secondary y-axis ends at 100%
            _y2Max: {
                type:     Number,
                readOnly: true
            },

            specY2Min: {
                type:      String,
                attribute: 'spec-y2-min'
            },

            specY2Max: {
                type:      String,
                attribute: 'spec-y2-max'
            },

            y2AxisLabel: {
                type:      String,
                attribute: 'y2-axis-label'
            },

            // [left] || center || right
            y2AxisAlign: {
                type:      String,
                attribute: 'y2-axis-align'
            },

            y2AxisNumberFormatSpecifier: {
                type:      String,
                attribute: 'y2-axis-number-format-specifier'
            },

            y2AxisDateFormatToken: {
                type:      String,
                attribute: 'y2-axis-date-format-token'
            },

            // Needed by chart behavior for zooming
            _xType: {
                type:     Array,
                computed: '_alias(_labels)'
            },

            _yType: {
                type: String
            },

            _delegatedFocus: String,

            // 'none' || 'single' || 'multiple'
            selectionMode: {
                type:      String,
                attribute: 'selection-mode'
            },

            // Unselectable items (array of indexes, where data[index] is unselectable)
            unselectable: {
                type: Array
            },

            // Current selection in chart
            _chartSelection: {
                type: Object
            },

            // sampleSize: unassigned - use default sampling,
            //             number - sample down data to specified number,
            //             0 (zero) = no sampling = show all points
            sampleSize: {
                type:      Number,
                attribute: 'sample-size'
            },

            tooltipTemplate: {
                type:      String,
                attribute: 'tooltip-template'
            },

            tooltipTemplate2: {
                type:      String,
                attribute: 'tooltip-template2'
            },

            _isZoomable$tb: {
                // eslint-disable-next-line max-len
                computed: '_isZoomable(noXZoom, noYZoom, xZoomRange, yZoomRange, xZoomInterval, yZoomInterval, xZoomSlider, yZoomSlider, xZoomDrag, yZoomDrag, xZoomSelect, yZoomSelect, showZoomButtons)'
            },

            _resetButtonEnabled$tb: {
                computed: '_enableZoomReset(_labels, _xMin, _xMax, specXMin, specXMax, xZoomStart, xZoomEnd, _yEnabled)'
            },

            _chartStateDataError: {
                type: String
            },

            _chartStateDataEmpty: {
                type: String
            }
        };
    }

    constructor() {
        super();

        this.thresholdValue = 85;
        this.titleLabel = null;
        this.notesLabel = null;
        this.flipAxes = false;
        this.hideXAxis = false;
        this.hideYAxis = false;
        this.showY2Axis = false;
        this.specY2Min = 0;
        this.specY2Max = 100 + 5; // Some extra space to fit the pareto line
        this._set_y2Min(0);
        this._set_y2Max(100);
        this._yType = yType;
    }

    _gcTabindex() {
        return this._hideToolbar ? undefined : this._delegatedFocus;
    }

    _tabindex() {
        return this.filterLegend ? this._delegatedFocus : undefined;
    }

    // eslint-disable-next-line max-len
    _isZoomable(/* noXZoom, noYZoom, xZoomRange, yZoomRange, xZoomInterval, yZoomInterval, xZoomSlider, yZoomSlider, xZoomDrag, yZoomDrag, xZoomSelect, yZoomSelect, showZoomButtons */) {
        return this._showZoomX() || this._showZoomY();
    }

    _enableZoomReset(_labels, _xMin, _xMax, specXMin, specXMax, xZoomStart, xZoomEnd, _yEnabled) {
        return this._xEnabled(_labels, _xMin, _xMax, specXMin, specXMax, xZoomStart, xZoomEnd) || _yEnabled;
    }

    _zoomSelect() {
        return (!this.noXZoom && this.xZoomSelect) || (!this.noYZoom && this.yZoomSelect);
    }

    _zoomDrag(drag, noZoom) {
        return !noZoom && drag;
    }

    _hideNotes() {
        return !this.notesLabel || this.hideNotes;
    }

    _textAlign(align) {
        return align ? `text-align: ${align}` : false;
    }

    _hideLegend() {
        return this.hideLegend || !Array.isArray(this.legend) || !(this.legend.length > 0);
    }

    _horizLegend() {
        return this.effLegendPos === 'top' || this.effLegendPos === 'bottom';
    }

    _xSide() {
        // eslint-disable-next-line no-nested-ternary
        return this.flipAxes ? (this.flipXAxis ? 'right' : 'left') : (this.flipXAxis ? 'top' : 'bottom');
    }

    _ySide() {
        // eslint-disable-next-line no-nested-ternary
        return this.flipAxes ? (this.flipYAxis ? 'top' : 'bottom') : (this.flipYAxis ? 'right' : 'left');
    }

    _y2Side() {
        // eslint-disable-next-line no-nested-ternary
        return this.flipAxes ? (this.flipYAxis ? 'bottom' : 'top') : (this.flipYAxis ? 'left' : 'right');
    }


    _xSize() {
        return this.flipAxes ? this._graphHeight : this._graphWidth;
    }

    _ySize() {
        return this.flipAxes ? this._graphWidth : this._graphHeight;
    }

    _getHorizontalAlignment(pos, align) {
        if (pos === 'top' || pos === 'bottom') {
            return align;
        }

        return 'start';
    }
    _showYRulers() {
        return this.showYRulers && !(this.showY2Axis && this.yAxisRulerAlignment === 'secondary');
    }

    _showY2Rulers() {
        return this.showYRulers && this.showY2Axis && (this.yAxisRulerAlignment === 'secondary');
    }

    _getMarker() {
        return (this.sparkView || this.hideMarkers) ? 'none' : this.marker;
    }

    _showValues(showValues) {
        return this.sparkView || this.hideValues ? 'no' : showValues;
    }

    _showZoom(noZoom, zoomRange, zoomInterval, zoomSlider, zoomDrag, zoomSelect, showZoomButtons) {
        if (noZoom) {
            return false;
        }
        return zoomRange || zoomInterval || zoomSlider || zoomDrag || zoomSelect || showZoomButtons;
    }

    _showZoomX() {
        return this._showZoom(this.noXZoom, this.xZoomRange, this.xZoomInterval, this.xZoomSlider, this.xZoomDrag, this.xZoomSelect,
            this.showZoomButtons);
    }

    _showZoomY() {
        return this._showZoom(this.noYZoom, this.yZoomRange, this.yZoomInterval, this.yZoomSlider, this.yZoomDrag, this.yZoomSelect,
            this.showZoomButtons);
    }

    _zoomValue(noZoom, zoom, unzoomed) {
        if (!noZoom && zoom !== undefined && zoom !== '' && zoom !== null) {
            // Zooming
            return zoom;
        }
        // Not zooming
        return unzoomed();
    }

    _specValueMinX() {
        return this._zoomValue(this.noXZoom, this.xZoomStart,
            () => this._zoomMinX(this._xMin, this._xMax, this._labels, this.specXMin, this.specXMax));
    }

    _specValueMinY(specYMin, specYMax, zoomStart, noYZoom, min, max) {
        return this._zoomValue(noYZoom, zoomStart, () => this._zoomMinY(min, max, specYMin, specYMax));
    }

    _specValueMaxX() {
        return this._zoomValue(this.noXZoom, this.xZoomEnd,
            () => this._zoomMaxX(this._xMin, this._xMax, this._labels, this.specXMax, this.specXMin));
    }

    _specValueMaxY(specMin, specMax, zoomEnd, noYZoom, min, max, type) {
        return this._zoomValue(noYZoom, zoomEnd, () => this._zoomMaxY(min, max, specMax, specMin));
    }

    _zoomMinY(min, max, spec, specMax) {
        // specYMin on y-axis defaults to 'baseline' for pareto charts
        if ((spec === '' || spec === undefined || spec === null || spec === 'baseline') && min >= 0) {
            return 0;
        }
        return axisMin(min, max, yType, spec, specMax);
    }

    _zoomMaxY(min, max, spec, specMin) {
        return axisMax(min, max, yType, spec, specMin);
    }

    _zoomMinX(min, max, type, spec, specMax) {
        return axisMin(min, max, type, spec, specMax);
    }

    _zoomMaxX(min, max, type, spec, specMin) {
        return axisMax(min, max, type, spec, specMin);
    }

    // Set _yEnabled - and make the secondary yaxis zoom in the same way as the primary yaxis
    _observeYzoom() {
        // Update state of reset button and secondary axis scrolling
        this.__observeYzoomActive = false;
        const min = this._zoomMinY(this._yMin, this._yMax, this.specYMin, this.specYMax);
        const max = this._zoomMaxY(this._yMin, this._yMax, this.specYMax, this.specYMin);
        const enabled = !typeIsFullRange(yType, min, max, this.yZoomStart, this.yZoomEnd);

        if (enabled && this.showY2Axis) {
            // Adjust scrolling of secondary y-axis
            const start = typeValue(min, yType);
            const end = typeValue(max, yType);
            const v1 = this.yZoomStart === undefined ? start : typeValue(this.yZoomStart, yType);
            const v2 = this.yZoomEnd === undefined ? end : typeValue(this.yZoomEnd, yType);
            const start2 = typeValue(this._zoomMinY(this._y2Min, this._y2Max, this.specY2Min, this.specY2Max), yType);
            const end2 = typeValue(this._zoomMaxY(this._y2Min, this._y2Max, this.specY2Max, this.specY2Min), yType);
            const z1 = start2 + ((v1 - start) * (end2 - start2)) / (end - start);
            const z2 = end2 - ((end2 - start2) * (end - v2)) / (end - start);
            this.y2ZoomStart = invTypeValue(z1, yType);
            this.y2ZoomEnd = invTypeValue(z2, yType);
        } else {
            // Secondary y-axis is not scrolled
            this.y2ZoomStart = undefined;
            this.y2ZoomEnd = undefined;
        }
        this._yEnabled = enabled;
    }

    _xEnabled(_labels, _xMin, _xMax, specXMin, specXMax, xZoomStart, xZoomEnd) {
        return !typeIsFullRange(
            _labels,
            this._zoomMinX(_xMin, _xMax, _labels, specXMin, specXMax),
            this._zoomMaxX(_xMin, _xMax, _labels, specXMax, specXMin),
            xZoomStart,
            xZoomEnd);
    }

    _zoomArg(noZoom, option) {
        return noZoom ? undefined : option;
    }

    _effLegendPosChangedEv(ev) {
        this.effLegendPos = ev.detail.value;
    }

    _chartStateChangedEv(ev) {
        this._chartState = ev.detail.value;
    }

    _graphWidthChangedEv(ev) {
        this._graphWidth = ev.detail.value;
    }

    _graphHeightChangedEv(ev) {
        this._graphHeight = ev.detail.value;
    }

    _labelsChangedEv(ev) {
        this._labels = ev.detail.value;
    }

    _xMinChangedEv(ev) {
        this._xMin = ev.detail.value;
    }

    _xMaxChangedEv(ev) {
        this._xMax = ev.detail.value;
    }

    _yMinChangedEv(ev) {
        this._yMin = ev.detail.value;
    }

    _yMaxChangedEv(ev) {
        this._yMax = ev.detail.value;
    }

    _chartStateDataErrorChangedEv(ev) {
        this._chartStateDataError = ev.detail.value;
    }

    _chartStateDataEmptyChangedEv(ev) {
        this._chartStateDataEmpty = ev.detail.value;
    }

    _selectedLegendChangedEv(ev) {
        this._selectedLegend = ev.detail.value;
    }

    _xZoomStartChangedEv(ev) {
        this.xZoomStart = ev.detail.value;
    }

    _xZoomEndChangedEv(ev) {
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

    _yTicksChangedEv(ev) {
        this._yTicks = ev.detail.value;
    }

    _yScaleChangedEv(ev) {
        this._yScale = ev.detail.value;
    }

    _y2ScaleChangedEv(ev) {
        this._y2Scale = ev.detail.value;
    }

    _y2TicksChangedEv(ev) {
        this._y2Ticks = ev.detail.value;
    }

    _legendChanged() {
        if (this.hideCumulativePercentage) {
            this._legendData = Array.isArray(this.legend) ? PTCS.clone(this.legend) : [];
        } else {
            const r = {label: this.cumulativeLegend || '', class: 'cumulative-line'};
            this._legendData = Array.isArray(this.legend) ? [...PTCS.clone(this.legend), r] : [r];
        }
    }

    _updateLegendFilter() {
        const {legend, _selectedLegend} = this;

        if (!legend || !_selectedLegend) {
            return; // Not ready
        }

        if (this.hideCumulativePercentage) {
            if (this._selectedLegend2 !== _selectedLegend) {
                this._selectedLegend2 = _selectedLegend;
            }
            this._hideCumulativePercentage = true;
            return;
        }

        const r = _selectedLegend.filter(i => i < legend.length);
        if (!this._selectedLegend2 || r.join('+') !== this._selectedLegend2.join('+')) {
            this._selectedLegend2 = r;
        }

        this._hideCumulativePercentage = _selectedLegend[_selectedLegend.length - 1] < legend.length;
    }


    willUpdate(changedProperties) {
        if (observeYzoomProperties.some(propName => changedProperties.has(propName))) {
            this._observeYzoom();
        }

        if (['legend', 'cumulativeLegend', 'hideCumulativePercentage'].some(propName => changedProperties.has(propName))) {
            this._legendChanged();
        }

        if (['legend', '_selectedLegend', 'hideCumulativePercentage'].some(propName => changedProperties.has(propName))) {
            this._updateLegendFilter();
        }

        super.willUpdate(changedProperties);
    }


    _hideCumulative() {
        return this.hideCumulativePercentage || this._hideCumulativePercentage;
    }

    _actionBar() {
        if (this._hideToolbar) {
            return undefined;
        }

        return this.actionBar || 'top';
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

    // The user has selected a zoom area
    _onZoomSelection(ev) {
        function invert(scale, v1, v2) {
            if (scale.invert) {
                return [scale.invert(v1), scale.invert(v2)];
            }
            const domain = scale.domain();
            if (domain.length <= 1) {
                return [domain[0], domain[0]];
            }
            let a = scale(domain[0]);
            let b = scale(domain[1]);
            let min, max;

            if (a < b) {
                const d = (b - a);
                const p = d * scale.padding();
                min = Math.ceil((v1 - a + p) / d - 1);
                max = Math.floor((v2 - a) / d);
            } else {
                a = scale(domain[domain.length - 1]);
                b = scale(domain[domain.length - 2]);
                const d = (b - a);
                const p = d * scale.padding();
                max = domain.length - 1 - Math.ceil((v2 - a + p) / d - 1);
                min = domain.length - 1 - Math.floor((v1 - a) / d);
            }

            min = Math.max(Math.min(min, domain.length), 0);
            max = Math.max(Math.min(max, domain.length), min);

            return [domain[min], domain[max]];
        }

        const xScale = this._xScale;
        const yScale = this._yScale;
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
                ? invert(xScale, d.x + d.w, d.x) : invert(xScale, d.x, d.x + d.w);
        }
        if (this.yZoomDrag || this.yZoomSelect) {
            [this.yZoomStart, this.yZoomEnd] = reverseYAxis // default y-axis is reversed
                ? invert(yScale, d.y, d.y + d.h) : invert(yScale, d.y + d.h, d.y);
        }
    }

    _resetToDefaultValues() {
        this.$.legend._resetToDefaultValues();
        this.$.zoomX._resetToDefaultValues();
        this.$.zoomY._resetToDefaultValues();
    }

    _hideXAxisChanged(hide) {
        if (!hide) {
            this.$.xaxis.refresh();
        }
    }

    _hideYAxisChanged(hide) {
        if (!hide) {
            this.$.yaxis.refresh();
        }
    }

    _showY2AxisChanged(show) {
        if (show) {
            this.$.yaxis2.refresh();
        }
    }

    refreshData() {
        this.$.chart.refreshData();
    }
};

customElements.define(PTCS.ChartPareto.is, PTCS.ChartPareto);
