import {LitElement, html, css} from 'lit';
import {when} from 'lit/directives/when.js';
import {ifDefined} from 'lit/directives/if-defined.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import {axisMin, axisMax, typeValue, invTypeValue, typeIsFullRange} from 'ptcs-library/library-chart.js';
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
import './ptcs-chart-core-line.js';

/* eslint-disable max-len */
PTCS.ChartLine = class extends BehaviorChart(PTCS.BehaviorTabindex(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))))) {
    static get styles() {
        return css`
        :host {
            display: block;
            outline: none;
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
        }

        #chart2 {
            position: absolute;
            left: 0;
            top: 0;
            right: 0;
            bottom: 0;
            /*pointer-events: none;*/
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
            .flipAxes=${this.flipAxes} .flipXAxis=${this.flipXAxis} .flipYAaxis=${this.flipYAxis}
            .sparkView=${this.sparkView}
            .xAxis=${!this.hideXAxis} .xAxis2=${this.isReferenceLines}
            .yAxis=${!this.hideYAxis} .yAxis2=${this._showY2Axis()}
            .isReferenceLines=${this.isReferenceLines}
            .actionBar=${this._actionBar()} .chartState=${this._chartState}>
            <div part="title-area" slot="title" style=${'text-align:' + this._getHorizontalAlignment(this.titlePos, this.titleAlign)}>
                <ptcs-label part="title-label" .label=${this.titleLabel} variant=${this.titleVariant}
                    .horizontalAlignment=${this._getHorizontalAlignment(this.titlePos, this.titleAlign)} multi-line></ptcs-label>
            </div>
            <div part="notes-area" slot="notes" style=${'text-align:' + this._getHorizontalAlignment(this.notesPos, this.notesAlign)}>
                <ptcs-label part="notes-label" .label=${this.notesLabel} variant="body"
                    .horizontalAlignment=${this._getHorizontalAlignment(this.notesPos, this.notesAlign)} multi-line></ptcs-label>
            </div>
            <ptcs-chart-state part="chart-state" slot="chart-state"
                .chartStateExt=${this.chartState} .chartStateDataError=${this._chartStateDataError} .chartStateDataEmpty=${this._chartStateDataEmpty}
                @chart-state-changed=${this._chartStateChangedEv}
                .iconLoading=${this.iconStateLoading} .labelNoData=${this.labelStateNoData} .iconNoData=${this.iconStateNoData}
                .labelEmpty=${this.labelStateEmpty} .iconEmpty=${this.iconStateEmpty}
                .labelError=${this.labelStateError} .iconError=${this.iconStateError}></ptcs-chart-state>
            <ptcs-chart-coord slot="chart" part="chart"
                .flipAxes=${this.flipAxes} .flipXAxis=${this.flipXAxis} .flipYAxis=${this.flipYAxis}
                .xTicks=${this._xTicks} .yTicks=${this._yTicks}
                .yScale=${this._yScale} .x2Ticks=${this._xReferenceLines}
                .y2Ticks=${this.isReferenceLines ? this._yReferenceLines : this._y2Ticks} .y2Scale=${this._y2Scale}
                .showXRulers=${this.showXRulers} .showX2Rulers=${this.isReferenceLines} .hasY2=${this._hasY2()}
                .showYRulers=${this._showYRulers()} .showY2Rulers=${this._showY2Rulers()}
                .isReferenceLines=${this.isReferenceLines} .yAxisRulerAlignment=${this.yAxisRulerAlignment}
                .frontRulers=${this.frontRulers} .hideZeroRuler=${this.hideZeroRuler} .sparkView=${this.sparkView}
                @graph-width-changed=${this._graphWidthChangedEv}
                @graph-height-changed=${this._graphHeightChangedEv}>
                <ptcs-chart-core-line id="chart" slot="chart" part="core-chart" style="pointer-events: auto"
                    tabindex=${ifDefined(this._delegatedFocus)}
                    .disabled=${this.disabled}
                    .data=${this.data}
                    .hideDataTooltips=${this.hideDataTooltips}
                    .legend=${this.legend}
                    .tooltipTemplate=${this.tooltipTemplate}
                    .stackMethod=${this.stackMethod} .stackOrder=${this.stackOrder}
                    .stackMethod2=${this.stackMethod2} .stackOrder2=${this.stackOrder2}
                    .xDateFormatToken=${this._updateDateFormatToken(this.numberOfXLabels, this.xAxisDateFormatToken)}
                    .yChartValueFormat=${this.yChartValueFormat}
                    .y2ChartValueFormat=${this.y2ChartValueFormat}
                    .xType=${this._xType}
                    @x-min-changed=${this._xMinChangedEv}
                    @x-max-changed=${this._xMaxChangedEv}
                    .yType=${this.yType}
                    @y-min-changed=${this._yMinRealChangedEv}
                    @y-max-changed=${this._yMaxRealChangedEv}
                    .hideLines=${this.hideLines}
                    .showAreas=${this.showAreas}
                    .curve=${this._getCurve()}
                    .bundleBeta=${this.bundleBeta} .cardinalTension=${this.cardinalTension}
                    .catmullRomAlpha=${this.catmullRomAlpha} .stepPosition=${this.stepPosition}
                    .flipAxes=${this.flipAxes} .reverseXAxis=${this.reverseXAxis} .reverseYAxis=${this.reverseYAxis}
                    .xScale=${this._xScale} .yScale=${this._yScale}
                    .filterLegend=${this._selectedLegend}
                    .marker=${this._getMarker()} .markerSize=${this.markerSize}
                    .showValues=${this._showValues()}
                    .showY2Axis=${this.showY2Axis}
                    .data2=${this.data2} .y2Type=${this.y2Type}
                    @y2-min-changed=${this._y2MinRealChangedEv}
                    @y2-max-changed=${this._y2MaxRealChangedEv}
                    .y2Scale=${this._y2Scale}
                    .cursorType=${this._cursorType()} .cursorTarget=${this._cursorTarget()}
                    .sampleSize=${this.sampleSize}
                    .zoomSelect=${this._zoomSelect()}
                    .zoomDragX=${this._zoomDrag(this.xZoomDrag, this.noXZoom)} .zoomDragY=${this._zoomDrag(this.yZoomDrag, this.noYZoom)}
                    .selectionMode=${this.selectionMode} @chart-selection=${this._onSelectionChanged} @zoom-selection=${this._onZoomSelection}
                    @chart-state-data-error-changed=${this._chartStateDataErrorChangedEv}
                    @chart-state-data-empty-changed=${this._chartStateDataEmptyChangedEv}></ptcs-chart-core-line>
            </ptcs-chart-coord>
            <div part="action-bar-area" slot="action-bar">
                <ptcs-toolbar id="toolbar" tabindex=${ifDefined(this._gcTabindex())}
                    part="action-bar" .disabled=${this.disabled} variant="secondary" hide-filter @activated=${this._toolbarAction}>
                </ptcs-toolbar>
            </div>
            <div part="legend-area" slot="legend">
                <ptcs-chart-legend id="legend" part="legend" tabindex=${ifDefined(this._tabindex())}
                    .items=${this.legend}
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
                .type=${this._xType}
                .side=${this._xSide()}
                .axisLength=${this._xSize()}
                .minValue=${this._zoomMin(this._xMin, this._xMax, this._xType, this.specXMin, this.specXMax)}
                .maxValue=${this._zoomMax(this._xMin, this._xMax, this._xType, this.specXMax, this.specXMin)}
                .zoomStart=${this.xZoomStart}  @zoom-start-changed=${this._xZoomStartChangedEv}
                .zoomEnd=${this.xZoomEnd} @zoom-end-changed=${this._xZoomEndChangedEv}
                .rangePicker=${this._zoomArg(this.noXZoom, this.xZoomRange)}
                .rangeDateFormat=${this.xZoomRangeDateFormat}
                .rangeDateWidth=${this.xZoomRangeDateWidth}
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
                .intervalToLabel=${this.xZoomIntervalToLabel}
                .dateRangeHintText=${this.dateRangeHintText}></ptcs-chart-zoom>
            <ptcs-chart-axis id="xaxis" slot="xaxis" part="xaxis" style="pointer-events: auto"
                no-tabindex .disabled=${this.disabled} ?hidden=${this.hideXAxis}
                .type=${this._xType}
                .specMin=${this._specValueMin(this.specXMin, this.specXMax, this.xZoomStart, this.noXZoom, this._xMin, this._xMax, this._xType)}
                .specMax=${this._specValueMax(this.specXMin, this.specXMax, this.xZoomEnd, this.noXZoom, this._xMin, this._xMax, this._xType)}
                .side=${this._xSide()} .label=${this.xAxisLabel} .alignLabel=${this.xAxisAlign}
                .minValue=${this._xMin} .maxValue=${this._xMax}
                .numTicks=${this.numberOfXLabels} .size=${this._xSize()}
                .maxSize=${this.flipAxes ? this.verticalAxisMaxWidth : this.horizontalAxisMaxHeight}
                @ticks-changed=${this._xTicksChangedEv}
                .ticksRotation=${this.horizontalTicksRotation}
                .reverse=${this.reverseXAxis}
                @scale-changed=${this._xScaleChangedEv}
                .numberFormatSpecifier=${this.xAxisNumberFormatSpecifier}
                .dateFormatToken=${this._updateDateFormatToken(this.numberOfXLabels, this.xAxisDateFormatToken)}></ptcs-chart-axis>
                ${when(this.isReferenceLines, () => html`<ptcs-chart-axis id="xaxis2" slot="xaxis2" part="xaxis2" style="pointer-events: auto"
                    no-tabindex ?hidden=${!this.isReferenceLines} .disabled=${this.disabled}
                    .type=${this._xType}
                    .specMin=${this._specValueMin(this.specXMin, this.specXMax, this.xZoomStart, this.noXZoom, this._xMin, this._xMax, this._xType)}
                    .specMax=${this._specValueMax(this.specXMin, this.specXMax, this.xZoomEnd, this.noXZoom, this._xMin, this._xMax, this._xType)}
                    .side=${this._x2Side()}
                    .minValue=${this._xMin} .maxValue=${this._xMax}
                    .numTicks=${this.numberOfXLabels}
                    .size=${this._xSize()} .maxSize=${this.flipAxes ? this.verticalAxisMaxWidth : this.horizontalAxisMaxHeight}
                    @ticks-changed=${this._xTicksChangedEv}
                    .ticksRotation=${this.horizontalTicksRotation}
                    .reverse=${this.reverseXAxis}
                    @scale-changed=${this._xScaleChangedEv}
                    .numberFormatSpecifier=${this.xAxisNumberFormatSpecifier}
                    .dateFormatToken=${this._updateDateFormatToken(this.numberOfXLabels, this.xAxisDateFormatToken)}
                    .referenceLines=${this._xAxisReferenceLines}
                    @eff-reference-lines-changed=${this._xReferenceLinesChangedEv}
                    .isReferenceLines=${this.isReferenceLines}></ptcs-chart-axis>`)}
                <ptcs-chart-zoom slot="yzoom" id="zoomY" part="zoom-yaxis" ?hidden=${this.noYZoom}
                    tabindex=${ifDefined(this._delegatedFocus)} .disabled=${this.disabled}
                    .type=${this.yType}
                    .side=${this._ySide()}
                    .axisLength=${this._ySize()}
                    .minValue=${this._zoomMin(this._yMin, this._yMax, this.yType, this.specYMin, this.specYMax)}
                    .maxValue=${this._zoomMax(this._yMin, this._yMax, this.yType, this.specYMax, this.specYMin)}
                    .zoomStart=${this.yZoomStart}  @zoom-start-changed=${this._yZoomStartChangedEv}
                    .zoomEnd=${this.yZoomEnd}  @zoom-end-changed=${this._yZoomEndChangedEv}
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
                    .intervalToLabel=${this.yZoomIntervalToLabel}
                    .dateRangeHintText=${this.dateRangeHintText}></ptcs-chart-zoom>
            <ptcs-chart-axis id="yaxis" slot="yaxis" part="yaxis" style="pointer-events: auto" ?hidden=${this.hideYAxis}
                no-tabindex .disabled=${this.disabled} .type=${this.yType} .specMin=${this._specYValueMin} .specMax=${this._specYValueMax}
                .numTicks=${this.numberOfYLabels}
                .side=${this._ySide()}
                .label=${this.yAxisLabel} .alignLabel=${this.yAxisAlign}
                .minValue=${this._yMin} .maxValue=${this._yMax}
                .size=${this._ySize()} .maxSize=${this.flipAxes ? this.horizontalAxisMaxHeight : this.verticalAxisMaxWidth}
                @ticks-changed=${this._yTicksChangedEv}
                .ticksRotation=${this.horizontalTicksRotation}
                .reverse=${this.reverseYAxis}
                @scale-changed=${this._yScaleChangedEv}
                .numberFormatSpecifier=${this.yAxisNumberFormatSpecifier}
                .dateFormatToken=${this._updateDateFormatToken(this.numberOfYLabels, this.yAxisDateFormatToken)}></ptcs-chart-axis>

            ${when(this._showY2Axis(), () => html`
                <ptcs-chart-axis id="yaxis2" slot="yaxis2" part="yaxis2" style="pointer-events: auto"
                    no-tabindex .disabled=${this.disabled} .type=${this.isReferenceLines ? this.yType : this.y2Type}
                    .specMin=${this._specValueMinY2()} .specMax=${this._specValueMaxY2()}
                    .numTicks=${this.numberOfYLabels}
                    .side=${this._y2Side()}
                    .label=${this.y2AxisLabel}
                    .alignLabel=${this.y2AxisAlign}
                    .minValue=${this.isReferenceLines ? this._yMin : this._y2Min}
                    .maxValue=${this.isReferenceLines ? this._yMax : this._y2Max}
                    .size=${this._ySize()}
                    .maxSize=${this.flipAxes ? this.horizontalAxisMaxHeight : this.verticalAxisMaxWidth}
                    .reverse=${this.isReferenceLines ? this.reverseYAxis : this.reverseY2Axis}
                    @scale-changed=${this._y2ScaleChangedEv}
                    .numberFormatSpecifier=${this.y2AxisNumberFormatSpecifier}
                    .dateFormatToken=${this._updateDateFormatToken(this.numberOfYLabels, this.y2AxisDateFormatToken)}
                    .dualTicks=${this._dualTicks()}
                    @ticks-changed=${this._y2TicksChangedEv}
                    .referenceLines=${this._yAxisReferenceLines}
                    @eff-reference-lines-changed=${this._yReferenceLinesChangedEv}
                    .isReferenceLines=${this.isReferenceLines}></ptcs-chart-axis>`)}
        </ptcs-chart-layout>`;
    }
    /* eslint-enable max-len */

    static get is() {
        return 'ptcs-chart-line';
    }

    static get properties() {
        return {
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

            // Computed by ptcs-chart-core-line, based on data and data2
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

            // X-axis number of labels
            numberOfXLabels: {
                type:      Number,
                attribute: 'number-of-x-labels'
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

            // Y-axis number of labels
            numberOfYLabels: {
                type:      Number,
                attribute: 'number-of-y-labels'
            },

            hideYAxis: {
                type:      Boolean,
                observer:  '_hideYAxisChanged',
                attribute: 'hide-y-axis'
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

            // top || bottom || left || [right]
            legendPos: {
                type:      String,
                attribute: 'legend-pos'
            },

            _effLegendPos: {
                type:  String,
                state: true
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
                type:  Array,
                state: true
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

            // Connects ticks from x-axis to chart
            _xTicks: {
                type:  Array,
                state: true
            },

            // Connects ticks from y-axis to chart
            _yTicks: {
                type:  Array,
                state: true
            },

            // Connects ticks from y-axis to chart
            _y2Ticks: {
                type:  Array,
                state: true
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

            // Flag to use secondary axis for reference lines
            isReferenceLines: {
                type:      Boolean,
                attribute: 'is-reference-lines'
            },

            // Reference lines (a.k.a. threshold lines) raw data
            referenceLines: {
                type:      Array,
                observer:  'referenceLinesChanged',
                attribute: 'reference-lines'
            },

            // Reference lines for x-axis (only values valid according to the data type of the axis)
            _xAxisReferenceLines: {
                type:  Array,
                state: true
            },

            // Reference lines for y-axis (only values valid according to the data type of the axis)
            _yAxisReferenceLines: {
                type:  Array,
                state: true
            },

            // Sorted & filtered secondary x-axis data from ptcs-chart-axis
            _xReferenceLines: {
                type:  Array,
                state: true
            },

            // Sorted & filtered secondary y-axis data from ptcs-chart-axis
            _yReferenceLines: {
                type:  Array,
                state: true
            },

            // Watches for resizes
            _graphWidth: {
                type:  Number,
                state: true
            },

            // Watches for resizes
            _graphHeight: {
                type:  Number,
                state: true
            },

            // x-axis type: number || date || string
            xType: {
                type:      Object,
                attribute: 'x-type'
            },

            _xType: {
                type:     Object,
                computed: '_getXType(chartType, xType)',
                state:    true
            },

            // y-axis type: number || date || string
            yType: {
                type:      Object,
                attribute: 'y-type'
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
                type:  Object,
                state: true
            },

            // Maximum x value in data
            _xMax: {
                type:  Object,
                state: true
            },

            // Minimun y value in data
            _yMinReal: {
                type:  Object,
                state: true
            },

            // Maximum y value in data
            _yMaxReal: {
                type:  Object,
                state: true
            },

            // Minimun y value in data - unless it is identical to maximum y value (in which case it needs some extra delta)
            _yMin: {
                type:     Object,
                computed: '_computeYMin(_yMinReal, _yMaxReal, yType)',
                state:    true
            },

            // Maximum y value in data - unless it is identical to minimum y value (in which case it needs some extra delta)
            _yMax: {
                type:     Object,
                computed: '_computeYMax(_yMinReal, _yMaxReal, yType)',
                state:    true
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

            _specYValueMin: {
                type:  Object,
                state: false
            },

            // Specified y-max-value: auto || Number
            specYMax: {
                type:      Object,
                attribute: 'spec-y-max'
            },

            _specYValueMax: {
                type:  Object,
                state: true
            },

            // Move x-scale from x-axis to chart
            _xScale: {
                type:  Function,
                state: true
            },

            // Move y-scale from y-axis to chart
            _yScale: {
                type:  Function,
                state: true
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

            // Format of range-picker
            xZoomRangeDateFormat: {
                type:      String,
                attribute: 'x-zoom-range-date-format'
            },

            // Width of range-picker
            xZoomRangeDateWidth: {
                type:      Number,
                attribute: 'x-zoom-range-date-width'
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
                type:  Boolean,
                state: false
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
                type:     Array,
                observer: '_resetZoom'
            },

            // target method: auto (point) || horz || vert || cross
            pointerType: {
                type:      String,
                attribute: 'pointer-type'
            },

            // target method: auto (over) || horz || vert || both
            dataPointSelection: {
                type:      String,
                attribute: 'data-point-selection'
            },

            // Stack method: falsy || auto || expand || diverging || silhouette || wiggle
            // (If assigned, enables stacking.)
            stackMethod: {
                type:      String,
                attribute: 'stack-method'
            },

            // Stack order: auto || reverse || appearance || ascending || descending || insideout
            stackOrder: {
                type:      String,
                attribute: 'stack-order'
            },

            // The same stack options for the second y axis
            stackMethod2: {
                type:      String,
                attribute: 'stack-method2'
            },

            stackOrder2: {
                type:      String,
                attribute: 'stack-order2'
            },

            // Hide curve lines
            hideLines: {
                type:      Boolean,
                attribute: 'hide-lines'
            },

            // Show areas under chart lines
            showAreas: {
                type:      Boolean,
                attribute: 'show-areas'
            },

            // linear || basis || bundle || cardinal || catmull-rom || monotone-x || monotone-y || natural || step
            curve: {
                type: String
            },

            // linechart || runchart || stepchart || areachart || scatterchart || streamgraphchart
            chartType: {
                type:      String,
                attribute: 'chart-type'
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

            hideMarkers: {
                type:      Boolean,
                attribute: 'hide-markers'
            },

            hideValues: {
                type:      Boolean,
                attribute: 'hide-values'
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

            // above || on || below
            showValues: {
                type:      String,
                attribute: 'show-values'
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
                attribute: 'x-aAxis-date-format-token'
            },

            yAxisNumberFormatSpecifier: {
                type:      String,
                attribute: 'y-axis-number-format-specifier'
            },

            // Format specifier for values associated to primary y-axis
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
                attribute: 'show-y2-axis'
            },

            // y2-axis type: number || date || Array of string
            y2Type: {
                type:      Object,
                attribute: 'y2-type'
            },

            // Format specifier for values associated to secondary y-axis
            y2ChartValueFormat: {
                type:      String,
                attribute: 'y2-chart-value-format'
            },

            reverseY2Axis: {
                type:      Boolean,
                attribute: 'reverse-y2-axis'
            },

            // Move y2-scale from y2-axis to chart2
            _y2Scale: {
                type:  Function,
                state: true
            },

            data2: {
                type:     Array,
                observer: '_resetZoom'
            },

            // Minimun y value in data2
            _y2Min: {
                type:  Object,
                state: true
            },

            // Maximum y value in data2
            _y2Max: {
                type:  Object,
                state: true
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

            _delegatedFocus: {
                type:  String,
                state: true
            },

            // 'none' || 'single' || 'multiple'
            selectionMode: {
                type:      String,
                attribute: 'selection-mode'
            },

            // Current selection in chart
            _chartSelection: {
                type:  Object,
                state: true
            },

            tooltipTemplate: {
                type:      String,
                attribute: 'tooltip-template'
            },

            dateRangeHintText: {
                type:      String,
                attribute: 'date-range-hint-text'
            },

            // sampleSize: unassigned - use default sampling,
            //             number - sample down data to specified number,
            //             0 (zero) = no sampling = show all points
            sampleSize: {
                type:      Number,
                attribute: 'sample-size'
            },

            _isZoomable$tb: {
                // eslint-disable-next-line max-len
                computed: '_isZoomable(noXZoom, noYZoom, xZoomRange, yZoomRange, xZoomInterval, yZoomInterval, xZoomSlider, yZoomSlider, xZoomDrag, yZoomDrag, xZoomSelect, yZoomSelect, showZoomButtons)',
                state:    true
            },

            _resetButtonEnabled$tb: {
                computed: '_enableZoomReset(chartType, xType, _xMin, _xMax, specXMin, specXMax, xZoomStart, xZoomEnd, _yEnabled)',
                state:    true
            }
        };
    }

    static get observers() {
        return [
            '_observeYzoom(yType, _yMin, _yMax, yZoomStart, yZoomEnd, specYMin, specYMax, y2Type, specYMax, _y2Min, _y2Max, specY2Min, specY2Max)',
            '_observeIsReferenceLines(referenceLines, showY2Axis)',
            '_observeSpecYValueMin(specYMin, specYMax, yZoomStart, noYZoom, _yMin, _yMax, yType)',
            '_observeSpecValueMax(specYMin, specYMax, yZoomEnd, noYZoom, _yMin, _yMax, yType)'
        ];
    }

    constructor() {
        super();
        this.bundleBeta = 0.5;
        this.cardinalTension = 0.5;
        this.catmullRomAlpha = 0.5;
        this.xType = 'number';
        this.yType = 'number';
        this.yType2 = 'number';
    }

    _updateDateFormatToken(numberOfLabels, dateFormatToken) {
        return (numberOfLabels > 0 && !dateFormatToken) ? 'YYYY-MM-DD HH:mm:ss.SSS' : dateFormatToken;
    }

    _gcTabindex() {
        return this._hideToolbar ? undefined : this._delegatedFocus;
    }

    _tabindex() {
        return this.filterLegend ? this._delegatedFocus : undefined;
    }

    _isZoomable(/* lot's of depending properties  */) {
        return this._showZoomX() || this._showZoomY();
    }

    _computeYMin(_yMinReal, _yMaxReal, yType) {
        if (_yMinReal !== _yMaxReal) {
            return _yMinReal;
        }
        if (yType === 'number' && typeof _yMinReal === 'number') {
            return _yMinReal - 0.8; // Need some extra delta - arbitrary value
        }
        if (yType === 'date' && _yMinReal instanceof Date) {
            return new Date(_yMinReal.getTime() - 0.4 * 1000 * 60 * 60 * 24); // Need some extra delta - arbitrary value
        }
        return _yMinReal;
    }

    _computeYMax(_yMinReal, _yMaxReal, yType) {
        if (_yMinReal !== _yMaxReal) {
            return _yMaxReal;
        }
        if (yType === 'number' && typeof _yMaxReal === 'number') {
            return _yMaxReal + 0.8; // Need some extra delta - arbitrary value
        }
        if (yType === 'date' && _yMaxReal instanceof Date) {
            return new Date(_yMaxReal.getTime() + 0.4 * 1000 * 60 * 60 * 24); // Need some extra delta - arbitrary value
        }
        return _yMaxReal;
    }

    _enableZoomReset(chartType, xType, _xMin, _xMax, specXMin, specXMax, xZoomStart, xZoomEnd, _yEnabled) {
        return this._xEnabled(chartType, xType, _xMin, _xMax, specXMin, specXMax, xZoomStart, xZoomEnd) || _yEnabled;
    }

    _zoomSelect() {
        return (!this.noXZoom && this.xZoomSelect) || (!this.noYZoom && this.yZoomSelect);
    }

    _zoomDrag(drag, noZoom) {
        return !noZoom && drag;
    }

    _observeIsReferenceLines(referenceLines, showY2Axis) {
        this.isReferenceLines = !showY2Axis && (referenceLines && referenceLines.length > 0);
    }

    _observeSpecYValueMin(specYMin, specYMax, yZoomStart, noYZoom, _yMin, _yMax, yType) {
        this._specYValueMin = this._specValueMin(specYMin, specYMax, yZoomStart, noYZoom, _yMin, _yMax, yType);
    }

    _observeSpecValueMax(specYMin, specYMax, yZoomEnd, noYZoom, _yMin, _yMax, yType) {
        this._specYValueMax = this._specValueMax(specYMin, specYMax, yZoomEnd, noYZoom, _yMin, _yMax, yType);
    }

    referenceLinesChanged(referenceLines) {
        if (!(referenceLines instanceof Array)) {
            return;
        }

        const p = (type, line, acc) => {
            if (type === 'number') {
                if (!isNaN(line.value)) {
                    acc.push(line);
                }
            } else if (type === 'date') {
                const d = line.value instanceof Date ? line.value : new Date(line.value);
                if (!isNaN(d)) {
                    acc.push({...line, value: d});
                }
            }
        };

        // Partition referenceLines data according to axis and filter out lines whose value is invalid for its axis data type
        const validXLines = [];
        const validYLines = [];
        this._xAxisReferenceLines = [];
        this._yAxisReferenceLines = [];
        referenceLines.forEach((line) => {
            switch (line.axis) {
                case 'xaxis':
                    p(this.xType, line, validXLines);
                    break;
                case 'yaxis':
                    p(this.yType, line, validYLines);
                    break;
                default:
                    console.warn('Invalid axis value on reference line data: ' + line.axis);
            }
        });
        this._xAxisReferenceLines = validXLines;
        this._yAxisReferenceLines = validYLines;
    }

    _getXType(chartType, xType) {
        if (chartType === 'runchart' || chartType === 'streamgraphchart') {
            return 'date';
        }

        return xType;
    }

    _getCurve(chartType, curve) {
        switch (this.chartType) {
            case 'stepchart':
                return 'step';
            case 'scatterchart':
                return 'linear';
            case 'runchart':
                return this.curve || 'monotone-x';
        }
        return this.curve;
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

    _getMarker() {
        return ((this.sparkView || this.hideMarkers) && this.chartType !== 'scatterchart') ? 'none' : this.marker;
    }

    _hasY2() {
        return this.showY2Axis && this.data2 && this.data2.series && this.data2.series.length > 0;
    }

    _showYRulers() {
        return this.showYRulers && !(this._hasY2() && this.yAxisRulerAlignment === 'secondary');
    }

    _showY2Rulers() {
        return this.isReferenceLines || this.showYRulers && this._hasY2() && (this.yAxisRulerAlignment === 'secondary');
    }

    _showY2Axis() {
        return this.showY2Axis || this.isReferenceLines;
    }

    _showValues() {
        return this.sparkView || this.hideValues ? 'no' : this.showValues;
    }

    _showZoom(noZoom, zoomRange, zoomInterval, zoomSlider, zoomDrag, zoomSelect, showZoomButtons) {
        if (noZoom) {
            return false;
        }
        return zoomRange || zoomInterval || zoomSlider || zoomDrag || zoomSelect || showZoomButtons;
    }

    _showZoomX() {
        return this._showZoom(this.noXZoom, this.xZoomRange, this.xZoomInterval, this.xZoomSlider, this.xZoomDrag,
            this.xZoomSelect, this.showZoomButtons);
    }

    _showZoomY() {
        return this._showZoom(this.noYZoom, this.yZoomRange, this.yZoomInterval, this.yZoomSlider, this.yZoomDrag,
            this.yZoomSelect, this.showZoomButtons);
    }

    _specValueMin(specMin, specMax, zoomStart, noZoom, min, max, type) {
        if (!noZoom && zoomStart !== undefined && zoomStart !== '' && zoomStart !== null) {
            // Zooming
            return zoomStart;
        }
        return this._zoomMin(min, max, type, specMin, specMax);
    }

    _specValueMax(specMin, specMax, zoomEnd, noZoom, min, max, type) {
        if (!noZoom && zoomEnd !== undefined && zoomEnd !== '' && zoomEnd !== null) {
            // Zooming
            return zoomEnd;
        }
        // No zooming
        return this._zoomMax(min, max, type, specMax, specMin);
    }

    _specValueMinY2() {
        if (this.isReferenceLines) {
            return this._specYValueMin;
        }
        return this._specValueMin(this.specY2Min, this.specY2Max, this.y2ZoomStart, this.noYZoom, this._y2Min, this._y2Max, this.y2Type);
    }

    _specValueMaxY2() {
        if (this.isReferenceLines) {
            return this._specYValueMax;
        }
        return this._specValueMax(this.specY2Min, this.specY2Max, this.y2ZoomEnd, this.noYZoom, this._y2Min, this._y2Max, this.y2Type);
    }

    _zoomMin(min, max, type, spec, specMax) {
        return axisMin(min, max, type, spec, specMax);
    }

    _zoomMax(min, max, type, spec, specMin) {
        return axisMax(min, max, type, spec, specMin);
    }

    // Set _yEnabled - and make the secondary yaxis zoom in the same way as the primary yaxis
    _observeYzoom(/* yType, _yMin, _yMax, yZoomStart, yZoomEnd, specYMin, y2Type, specYMax, _y2Min, _y2Max, specY2Min, specY2Max */) {
        if (this.__observeYzoomActive) {
            // Wait until all changes has been reported
            return;
        }
        this.__observeYzoomActive = true;
        requestAnimationFrame(() => {
            // Update state of reset button and secondary axis scrolling
            this.__observeYzoomActive = false;
            const min = this._zoomMin(this._yMin, this._yMax, this.yType, this.specYMin, this.specYMax);
            const max = this._zoomMax(this._yMin, this._yMax, this.yType, this.specYMax, this.specYMin);
            const enabled = !typeIsFullRange(this.yType, min, max, this.yZoomStart, this.yZoomEnd);

            if (enabled && (this.showY2Axis || this.isReferenceLines) && this.data2 && this.data2.series && this.data2.series.length) {
                // Adjust scrolling of secondary y-axis
                const start = typeValue(min, this.yType);
                const end = typeValue(max, this.yType);
                const v1 = this.yZoomStart === undefined ? start : typeValue(this.yZoomStart, this.yType);
                const v2 = this.yZoomEnd === undefined ? end : typeValue(this.yZoomEnd, this.yType);
                const start2 = typeValue(this._zoomMin(this._y2Min, this._y2Max, this.y2Type, this.specY2Min, this.specY2Max), this.y2Type);
                const end2 = typeValue(this._zoomMax(this._y2Min, this._y2Max, this.y2Type, this.specY2Max, this.specY2Min), this.y2Type);
                const z1 = start2 + ((v1 - start) * (end2 - start2)) / (end - start);
                const z2 = end2 - ((end2 - start2) * (end - v2)) / (end - start);
                this.y2ZoomStart = invTypeValue(z1, this.y2Type);
                this.y2ZoomEnd = invTypeValue(z2, this.y2Type);
            } else {
                // Secondary y-axis is not scrolled
                this.y2ZoomStart = undefined;
                this.y2ZoomEnd = undefined;
            }
            this._yEnabled = enabled;
        });
    }

    _dualTicks() {
        if (this.isReferenceLines) {
            return null;
        }
        if (this.data2 && this.data2.series && this.data2.series.length > 0) {
            // We *have* a second set of data, so let it use its own ticks
            return null;
        }
        // Use the ticks from the "main" y-axis as-is
        return this._yTicks;
    }

    _xEnabled(chartType, xType, _xMin, _xMax, specXMin, specXMax, xZoomStart, xZoomEnd) {
        const type = this._getXType(chartType, xType);
        return !typeIsFullRange(
            type,
            this._zoomMin(_xMin, _xMax, type, specXMin, specXMax),
            this._zoomMax(_xMin, _xMax, type, specXMax, specXMin),
            xZoomStart,
            xZoomEnd);
    }

    _zoomArg(noZoom, option) {
        return noZoom ? undefined : option;
    }

    _actionBar() {
        return this._hideToolbar ? null : (this.actionBar || 'top');
    }

    _cursorType() {
        const map = this.flipAxes ? PTCS.ChartLine.mapPointerTypeFlip : PTCS.ChartLine.mapPointerType;
        return map[this.pointerType] || this.pointerType;
    }

    _cursorTarget() {
        const map = this.flipAxes ? PTCS.ChartLine.mapPoinSelectFlip : PTCS.ChartLine.mapPoinSelect;
        return map[this.dataPointSelection] || this.dataPointSelection;
    }

    _effLegendPosChangedEv(ev) {
        this._effLegendPos = ev.detail.value;
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

    _xMinChangedEv(ev) {
        this._xMin = ev.detail.value;
    }

    _xMaxChangedEv(ev) {
        this._xMax = ev.detail.value;
    }

    _yMinRealChangedEv(ev) {
        this._yMinReal = ev.detail.value;
    }

    _yMaxRealChangedEv(ev) {
        this._yMaxReal = ev.detail.value;
    }

    _y2MinRealChangedEv(ev) {
        this._y2Min = ev.detail.value;
    }

    _y2MaxRealChangedEv(ev) {
        this._y2Max = ev.detail.value;
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

    _xReferenceLinesChangedEv(ev) {
        this._xReferenceLines = ev.detail.value;
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

    _yReferenceLinesChangedEv(ev) {
        this._yReferenceLines = ev.detail.value;
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

    // Reset zoom whenever data changes
    _resetZoom() {
        requestAnimationFrame(() => {
            this.$.zoomX._resetToDefaultValues();
            this.$.zoomY._resetToDefaultValues();
            // Sometimes two resets are needed... (why?)
            requestAnimationFrame(() => {
                this.$.zoomX._resetToDefaultValues();
                this.$.zoomY._resetToDefaultValues();
            });
        });
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

    refreshData() {
        this.$.chart.refreshData();
    }
};

PTCS.ChartLine.mapPointerType = {horz: 'y', vert: 'x', cross: 'xy'};
PTCS.ChartLine.mapPoinSelect = {horz: 'y', vert: 'x', both: 'xy'};
PTCS.ChartLine.mapPointerTypeFlip = {horz: 'x', vert: 'y', cross: 'xy'};
PTCS.ChartLine.mapPoinSelectFlip = {horz: 'x', vert: 'y', both: 'xy'};

customElements.define(PTCS.ChartLine.is, PTCS.ChartLine);
