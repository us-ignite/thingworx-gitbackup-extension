import {LitElement, html, css} from 'lit';
import {ifDefined} from 'lit/directives/if-defined.js';
import {when} from 'lit/directives/when.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import {axisBarMin, axisBarMax, typeIsFullRange} from 'ptcs-library/library-chart.js';
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
import {__xv} from './ptcs-chart-core-bar.js';


PTCS.ChartBar = class extends BehaviorChart(PTCS.BehaviorTabindex(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))))) {

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
        }`;
    }

    render() {
        return html`
        <ptcs-chart-layout id="chart-layout" style="height:100%" part="chart-layout"
            disabled=${this.disabled}
            .titlePos=${this.titlePos} .hideTitle=${!this.titleLabel}
            .notesPos=${this.notesPos} .notesAlign=${this.notesAlign} .hideNotes=${this._hideNotes()}
            .legendPos=${this.legendPos} .hideLegend=${this._hideLegend()}
            @eff-legend-pos-changed=${this._effLegendPosChangedEv}
            .xZoom=${this._showZoomX()} .yZoom=${this._showZoomY()}
            .flipAxes=${this.flipAxes} .flipXAxis=${this.flipXAxis} .flipYAxis=${this.flipYAxis}
            .sparkView=${this.sparkView} .actionBar=${this._actionBar()}
            .xAxis=${!this.hideXAxis} .yAxis=${!this.hideYAxis} .yAxis2=${this._showY2Axis()} .isReferenceLines=${this.isReferenceLines}
            .chartState=${this._chartState}>
            <div part="title-area" slot="title" style=${'text-align:' + this._getHorizontalAlignment(this.titlePos, this.titleAlign)}>
                <ptcs-label part="title-label" .label=${this.titleLabel} .variant=${this.titleVariant}
                    .horizontalAlignment=${this._getHorizontalAlignment(this.titlePos, this.titleAlign)} multi-line></ptcs-label>
            </div>
            <div part="notes-area" slot="notes" style=${'text-align:' + this._getHorizontalAlignment(this.notesPos, this.notesAlign)}>
                <ptcs-label part="notes-label" .label=${this.notesLabel} variant="body"
                    .horizontalAlignment=${this._getHorizontalAlignment(this.notesPos, this.notesAlign)} multi-line></ptcs-label>
            </div>
            <ptcs-chart-state part="chart-state" slot="chart-state"
                .chartStateExt=${this.chartState} .chartStateDataError=${this._chartStateDataError} .chartStateDataEmpty=${this._chartStateDataEmpty}
                @chart-state-changed=${this._chartStateChangedEv}
                .iconLoading=${this.iconStateLoading}
                .labelNoData=${this.labelStateNoData} .iconNoData=${this.iconStateNoData}
                .labelEmpty=${this.labelStateEmpty} .iconEmpty=${this.iconStateEmpty}
                .labelError=${this.labelStateError} .iconError=${this.iconStateError}></ptcs-chart-state>
            <ptcs-chart-coord slot="chart" part="chart"
                .flipAxes=${this.flipAxes} .flipXAxis=${this.flipXAxis} .flipYAxis=${this.flipYAxis}
                .xTicks=${this._xTicks} .yTicks=${this._yTicks} .yScale=${this._yScale}
                .y2Ticks=${this.isReferenceLines ? this._yReferenceLines : this._y2Ticks} .y2Scale=${this._y2Scale}
                .showXRulers=${this.showXRulers} .hasY2=${this._hasY2()}
                .showYRulers=${this._showYRulers()} .showY2Rulers=${this._showY2Rulers()}
                .isReferenceLines=${this.isReferenceLines}
                .yAxisRulerAlignment=${this.yAxisRulerAlignment}
                .frontRulers=${this.frontRulers}
                .hideZeroRuler=${this.hideZeroRuler}
                @graph-width-changed=${this._graphWidthChangedEv}
                @graph-height-changed=${this._graphHeightChangedEv}
                .sparkView=${this.sparkView}>
                <ptcs-chart-core-bar slot="chart" id="chart" part="core-chart" exportparts="bar" style="pointer-events: auto"
                    tabindex=${ifDefined(this._delegatedFocus)}
                    .data=${this.data}
                    .disabled=${this.disabled}
                    .legend=${this.legend}
                    .tooltipTemplate=${this.tooltipTemplate}
                    .stackMethod=${this.stackMethod}
                    .stackOrder=${this.stackOrder}
                    .stackMethod2=${this.stackMethod2}
                    .yChartValueFormat=${this.yChartValueFormat}
                    @y-min-changed=${this._yMinChangedEv} @y-max-changed=${this._yMaxChangedEv}
                    @y-axis-number-format-changed=${this._yAxisNumberFormatChangedEv}
                    .data2=${this.data2}
                    @y2-min-changed=${this._y2MinChangedEv}
                    @y2-max-changed=${this._y2MaxChangedEv}
                    .showY2Axis=${this._showY2Axis()}
                    .y2Scale=${this._y2Scale}
                    @y2-axis-number-format-changed=${this._y2AxisNumberFormatChangedEv}
                    .flipAxes=${this.flipAxes}
                    .reverseXAxis=${this.reverseXAxis} .reverseYAxis=${this.reverseYAxis} .reverseY2Axis=${this.reverseY2Axis}
                    .xScale=${this._xScale} .yScale=${this._yScale}
                    .filterLegend=${this._selectedLegend}
                    .hideDataTooltips=${this.hideDataTooltips}
                    .showValues=${this._showValues()}
                    .groupPadding=${this.groupPadding}
                    .zoomSelect=${this._zoomSelect()}
                    .zoomDragX=${this._zoomDrag(this.xZoomDrag, this.noXZoom)} .zoomDragY=${this._zoomDrag(this.yZoomDrag, this.noYZoom)}
                    .selectionMode=${this.selectionMode}
                    @chart-selection=${this._onSelectionChanged} @zoom-selection=${this._onZoomSelection}
                    @chart-state-data-error-changed=${this._chartStateDataErrorChangedEv}
                    @chart-state-data-empty-changed=${this._chartStateDataEmptyChangedEv}></ptcs-chart-core-bar>
            </ptcs-chart-coord>
            <div part="action-bar-area" slot="action-bar">
                <ptcs-toolbar id="toolbar" tabindex=${ifDefined(this._gcTabindex())}
                    part="action-bar" .disabled=${this.disabled} variant="secondary" hide-filter @activated=${this._toolbarAction}></ptcs-toolbar>
            </div>
            <div part="legend-area" slot="legend">
                <ptcs-chart-legend id="legend" part="legend" tabindex=${ifDefined(this._tabindex())}
                    .items=${this.legend} .shape=${this.legendShape} .filter=${this.filterLegend}
                    .horizontal=${this._horizLegend()} .maxWidth=${this.legendMaxWidth} .align=${this.legendAlign}
                    .disabled=${this.disabled}
                    @selected-changed=${this._selectedLegendChangedEv}></ptcs-chart-legend>
            </div>
            <ptcs-chart-zoom slot="xzoom" id="zoomX" part="zoom-xaxis" tabindex=${ifDefined(this._delegatedFocus)}
                .disabled=${this.disabled}
                .type=${this.labels}
                .side=${this._xSide()}
                ?hidden=${ifDefined(this.noXZoom || undefined)}
                _no-reset-slider
                .axisLength=${this._xSize()}
                .minValue=${this._xMin} .maxValue=${this._xMax}
                .zoomStart=${this.xZoomStart} @zoom-start-changed=${this._xZoomStartChangedEv}
                .zoomEnd=${this.xZoomEnd} @zoom-end-changed=${this._xZoomEndChangedEv}
                .rangePicker=${this.xZoomRange}
                .interval=${this.xZoomInterval} .intervalLabel=${this.xZoomIntervalLabel} .intervalControl=${this.xZoomIntervalControl}
                .intervalOrigin=${this.xZoomIntervalOrigin} .showIntervalAnchor=${this.xShowIntervalAnchor}
                .slider=${this.xZoomSlider} .sliderLabel=${this.xZoomSliderLabel} .reverseSlider=${this.reverseXAxis}
                .sliderMinLabel=${this.xZoomSliderMinLabel} .sliderMaxLabel=${this.xZoomSliderMaxLabel}
                .rangeStartLabel=${this.xZoomRangeStartLabel} .rangeEndLabel=${this.xZoomRangeEndLabel}
                .intervalFromLabel=${this.xZoomIntervalFromLabel} .intervalToLabel=${this.xZoomIntervalToLabel}></ptcs-chart-zoom>
            <ptcs-chart-axis slot="xaxis" id="xaxis" part="xaxis" style="pointer-events: auto" no-tabindex ?hidden=${this.hideXAxis}
                .type=${this.labels}
                .disabled=${this.disabled}
                .specMin=${this._specValue(this.specXMin, this.xZoomStart, this.noXZoom)}
                .specMax=${this._specValue(this.specXMax, this.xZoomEnd, this.noXZoom)}
                .side=${this._xSide()}
                .label=${this.xAxisLabel}
                .alignLabel=${this.xAxisAlign}
                .minValue=${this._xMin} .maxValue=${this._xMax}
                .size=${this._xSize()}
                .maxSize=${this.flipAxes ? this.verticalAxisMaxWidth : this.horizontalAxisMaxHeight}
                @ticks-changed=${this._xTicksChangedEv}
                .ticksRotation=${this.horizontalTicksRotation}
                .reverse=${this.reverseXAxis}
                @scale-changed=${this._xScaleChangedEv}
                .outerPadding=${this.outerPadding}
                .innerPadding=${this.innerPadding}></ptcs-chart-axis>
            <ptcs-chart-zoom slot="yzoom" id="zoomY" part="zoom-yaxis" tabindex=${ifDefined(this._delegatedFocus)}
                .disabled=${this.disabled} ?hidden=${this.noYZoom}
                type="number"
                .side=${this._ySide()}
                .axisLength=${this._ySize()}
                .minValue=${this._yZoomMin(this.specYMin, this.specYMax)}
                .maxValue=${this._yZoomMax(this.specYMin, this.specYMax)}
                .zoomStart=${this.yZoomStart} @zoom-start-changed=${this._yZoomStartChangedEv}
                .zoomEnd=${this.yZoomEnd} @zoom-end-changed=${this._yZoomEndChangedEv}
                .rangePicker=${this.yZoomRange}
                .interval=${this.yZoomInterval} .intervalLabel=${this.yZoomIntervalLabel}
                .intervalControl=${this.yZoomIntervalControl} .intervalOrigin=${this.yZoomIntervalOrigin}
                .showIntervalAnchor=${this.yShowIntervalAnchor}
                .slider=${this.yZoomSlider} .sliderLabel=${this.yZoomSliderLabel} .reverseSlider=${this.reverseYAxis}
                .sliderMinLabel=${this.yZoomSliderMinLabel} .sliderMaxLabel=${this.yZoomSliderMaxLabel}
                .rangeStartLabel=${this.yZoomRangeStartLabel} .rangeEndLabel=${this.yZoomRangeEndLabel}
                .intervalFromLabel=${this.yZoomIntervalFromLabel} .intervalToLabel=${this.yZoomIntervalToLabel}></ptcs-chart-zoom>
            <ptcs-chart-axis slot="yaxis" id="yaxis" part="yaxis" style="pointer-events: auto" no-tabindex
                .disabled=${this.disabled} ?hidden=${this.hideYAxis}
                type="number"
                .specMin=${this._specYValueMin} .specMax=${this._specYValueMax}
                .numTicks=${this.numberOfYLabels}
                .side=${this._ySide()}
                .label=${this.yAxisLabel} .alignLabel=${this.yAxisAlign}
                .minValue=${this._yMin} .maxValue=${this._yMax}
                .size=${this._ySize()} .maxSize=${this.flipAxes ? this.horizontalAxisMaxHeight : this.verticalAxisMaxWidth}
                @ticks-changed=${this._yTicksChangedEv}
                .ticksRotation=${this.horizontalTicksRotation}
                .reverse=${this.reverseYAxis}
                @scale-changed=${this._yScaleChangedEv}
                .numberFormat=${this._yAxisNumberFormat}
                .numberFormatSpecifier=${this.yAxisNumberFormatSpecifier}></ptcs-chart-axis>
                ${when(this._showY2Axis(), () => html`<ptcs-chart-axis id="yaxis2" slot="yaxis2" part="yaxis2" style="pointer-events: auto"
                    no-tabindex .disabled=${this.disabled}
                    type="number"
                    .specMin=${this._specValueMinY2()} .specMax=${this._specValueMaxY2()}
                    .numTicks=${this.numberOfYLabels}
                    .side=${this._y2Side()}
                    .label=${this.y2AxisLabel} .alignLabel=${this.y2AxisAlign}
                    .minValue=${this.isReferenceLines ? this._yMin : this._y2Min}
                    .maxValue=${this.isReferenceLines ? this._yMax : this._y2Max}
                    .size=${this._ySize()} .maxSize=${this.flipAxes ? this.horizontalAxisMaxHeight : this.verticalAxisMaxWidth}
                    .reverse=${this.isReferenceLines ? this.reverseYAxis : this.reverseY2Axis}
                    @scale-changed=${this._y2ScaleChangedEv}
                    .numberFormat=${this._y2AxisNumberFormat}
                    .numberFormatSpecifier=${this.y2AxisNumberFormatSpecifier}
                    .dualTicks=${this._dualTicks()}
                    @ticks-changed=${this._y2TicksChangedEv}
                    .ticksRotation=${this.horizontalTicksRotation}
                    .referenceLines=${this._yAxisReferenceLines}
                    @eff-reference-lines-changed=${this._yReferenceLinesChangedEv}
                    .isReferenceLines=${this.isReferenceLines}></ptcs-chart-axis>`)}
        </ptcs-chart-layout>`;
    }

    static get is() {
        return 'ptcs-chart-bar';
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

            disabled: {
                type:    Boolean,
                reflect: true
            },

            hideNotes: {
                type:      Boolean,
                attribute: 'hide-notes'
            },

            hideDataTooltips: {
                type: Boolean
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
                type:  String,
                state: true
            },

            // Computed by ptcs-chart-core-bar, based on data and data2
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

            // Names of legends, if legends should be visible
            legend: {
                type: Array
            },

            // top || bottom || left || [right]
            legendPos: {
                type:      String,
                attribute: 'legend-pos'
            },

            effLegendPos: {
                type:      String,
                attribute: 'eff-legend-pos'
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

            // Legends currently selected in the legend component
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

            // Reference lines for y-axis (only values valid according to the data type of the axis)
            _yAxisReferenceLines: {
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

            // The x-value on a bar-chart is always [string] (labels)
            /*
            // x-axis type: number || date || [string]
            xType: {
                type: Object
            },
            */

            // x-axis labels
            labels: {
                type:  Array, // of labels
                state: true
            },

            // The y-value on a bar-chart is always number
            /*
            // y-axis type: number || date || string
            yType: {
                type: Object
            },
            */

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
            _yMin: {
                type:  Object,
                state: true
            },

            // Maximum y value in data
            _yMax: {
                type:  Object,
                state: true
            },

            // Needed by chart behavior for zooming
            _xType: {
                type:     Array,
                computed: '_alias(labels)',
                state:    true
            },

            _yType: {
                type:  String,
                state: true
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
                state: true
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
                attribute: 'no-y-zoom'
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

            // The chart data
            data: {
                type: Array
            },

            // uniform || sparse
            format: {
                type: String,
            },

            // Stack method: auto || expand /*|| diverging || silhouette || wiggle*/
            // (If assigned, enables stacking.)
            stackMethod: {
                type:      Boolean,
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

            showValues: {
                type:      String,
                attribute: 'show-values'
            },

            hideValues: {
                type:      Boolean,
                attribute: 'hide-values'
            },

            hideZeroRuler: {
                type:      Boolean,
                attribute: 'hide-zero-ruler'
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
                attribute: 'group-padding'
            },

            _yAxisNumberFormat: {
                type:  String,
                state: true
            },

            yAxisNumberFormatSpecifier: {
                type:      String,
                attribute: 'y-axis-number-format-specifier'
            },

            // Format specifier for the y-values on the chart
            yChartValueFormat: {
                type:      String,
                attribute: 'y-chart-value-format'
            },

            // Secondary y-axis
            showY2Axis: {
                type:      Boolean,
                attribute: 'show-y2-axis'
            },

            reverseY2Axis: {
                type:      Boolean,
                attribute: 'reverse-y2-axis'
            },

            _y2Scale: {
                type:  Function,
                state: true
            },

            data2: {
                type: Array
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

            _y2AxisNumberFormat: {
                type:  String,
                state: true
            },

            y2AxisNumberFormatSpecifier: {
                type:      String,
                attribute: 'y2-axis-number-format-specifier'
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

            // For the toolbar
            _isZoomable$tb: {
                // eslint-disable-next-line max-len
                computed: '_isZoomable(noXZoom, noYZoom, xZoomRange, yZoomRange, xZoomInterval, yZoomInterval, xZoomSlider, yZoomSlider, xZoomDrag, yZoomDrag, xZoomSelect, yZoomSelect, showZoomButtons)',
                state:    true
            },

            _resetButtonEnabled$tb: {
                computed: '_enableZoomReset(labels, _xMin, _xMax, xZoomStart, xZoomEnd, _yMin, _yMax, yZoomStart, yZoomEnd, specYMin, specYMax)',
                state:    true
            }
        };
    }

    static get observers() {
        return [
            '_observeIsReferenceLines(referenceLines, showY2Axis)',
            '_observeSpecValueMin(specYMin, specYMax, yZoomStart, noYZoom, _yMin, _yMax)',
            '_observeSpecValueMax(specYMin, specYMax, yZoomEnd, noYZoom, _yMin, _yMax)'
        ];
    }

    constructor() {
        super();

        this.titleVariant = 'header';
        this._yType = 'number';
    }

    _gcTabindex() {
        return this._hideToolbar ? undefined : this._delegatedFocus;
    }

    _tabindex() {
        return this.filterLegend ? this._delegatedFocus : undefined;
    }

    _isZoomable(noXZoom, noYZoom, xZoomRange, yZoomRange, xZoomInterval, yZoomInterval, xZoomSlider, yZoomSlider, xZoomDrag, yZoomDrag,
        xZoomSelect, yZoomSelect, showZoomButtons) {
        return this._showZoom(noXZoom, xZoomRange, xZoomInterval, xZoomSlider, xZoomDrag, xZoomSelect, showZoomButtons) ||
            this._showZoom(noYZoom, yZoomRange, yZoomInterval, yZoomSlider, yZoomDrag, yZoomSelect, showZoomButtons);
    }

    _enableZoomReset(labels, _xMin, _xMax, xZoomStart, xZoomEnd, _yMin, _yMax, yZoomStart, yZoomEnd, specYMin, specYMax) {
        return this._enabled(labels, _xMin, _xMax, xZoomStart, xZoomEnd) ||
            this._yEnabled(_yMin, _yMax, yZoomStart, yZoomEnd, specYMin, specYMax);
    }

    _zoomSelect() {
        return (!this.noXZoom && this.xZoomSelect) || (!this.noYZoom && this.yZoomSelect);
    }

    _zoomDrag(drag, noZoom) {
        return !noZoom && drag;
    }

    referenceLinesChanged(referenceLines) {
        if (!Array.isArray(referenceLines)) {
            return;
        }
        // The y-axis data type is always "number", filter out invalid data
        this._yAxisReferenceLines = referenceLines.filter(line => !isNaN(line.value));
    }

    _observeIsReferenceLines(referenceLines, showY2Axis) {
        this.isReferenceLines = !showY2Axis && (referenceLines && referenceLines.length > 0);
    }

    _observeSpecValueMin(specYMin, specYMax, yZoomStart, noYZoom, _yMin, _yMax) {
        this._specYValueMin = this._specValueMin(specYMin, specYMax, yZoomStart, noYZoom, _yMin, _yMax);
    }

    _observeSpecValueMax(specYMin, specYMax, yZoomEnd, noYZoom, _yMin, _yMax) {
        this._specYValueMax = this._specValueMax(specYMin, specYMax, yZoomEnd, noYZoom, _yMin, _yMax);
    }

    _getHorizontalAlignment(pos, align) {
        return (pos === 'top' || pos === 'bottom') ? align : 'start';
    }

    _hideNotes() {
        return !this.notesLabel || this.hideNotes;
    }

    _hideLegend() {
        return this.hideLegend || !(this.legend instanceof Array) || !(this.legend.length > 0);
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

    _actionBar() {
        return this._hideToolbar ? undefined : (this.actionBar || 'top');
    }

    /*
     * Computes labels that are shown on the X Axis according to the data and data2
     */
    _computeLabels() {
        let xValues = [];

        if (Array.isArray(this.data) || (this._hasY2() && Array.isArray(this.data2))) {
            xValues = Array.isArray(this.data) ? this.data.map(item => __xv(item)) : [];

            if (this._hasY2() && Array.isArray(this.data2)) {
                const xValues2 = this.data2.map(item => __xv(item));

                // Filter duplicate labels in O(n + m)
                xValues = [...new Set([...xValues, ...xValues2])];
            }
        } else {
            xValues = ['error'];
        }

        this.labels = xValues;
        this._xMin = xValues[0];
        this._xMax = xValues[xValues.length - 1];
    }

    _hasY2() {
        return this.showY2Axis && this.data2 && this.data2.series && this.data2.series.length > 0;
    }

    _showYRulers() {
        return this.showYRulers && !(this.yAxisRulerAlignment === 'secondary' && this._hasY2());
    }

    _showY2Rulers() {
        return this.isReferenceLines || (this.showYRulers && this.yAxisRulerAlignment === 'secondary' && this._hasY2());
    }

    _showY2Axis() {
        return this.showY2Axis || this.isReferenceLines;
    }

    _showValues() {
        return this.sparkView || this.hideValues ? false : this.showValues;
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

    _specValue(spec, zoom, noZoom) {
        if (noZoom || zoom === undefined || zoom === '' || zoom === null) {
            return spec;
        }
        return zoom;
    }

    _specValueMin(specYMin, specYMax, yZoomStart, noYZoom, _yMin, _yMax) {
        if (!noYZoom && yZoomStart !== undefined && yZoomStart !== '' && yZoomStart !== null) {
            // Zooming
            return yZoomStart;
        }
        return this._yZoomMin(specYMin, specYMax);
    }

    _specValueMax(specYMin, specYMax, yZoomEnd, noYZoom, _yMin, _yMax) {
        if (!noYZoom && yZoomEnd !== undefined && yZoomEnd !== '' && yZoomEnd !== null) {
            // Zooming
            return yZoomEnd;
        }
        // No zooming
        return this._yZoomMax(specYMin, specYMax);
    }

    _specValueMinY2() {
        if (this.isReferenceLines) {
            return this._specYValueMin;
        }
        return this._specValueMin(this.specY2Min, this.specY2Max, this.yZoomStart, this.noYZoom, this._y2Min, this._y2Max);
    }

    _specValueMaxY2() {
        if (this.isReferenceLines) {
            return this._specYValueMax;
        }
        return this._specValueMax(this.specY2Min, this.specY2Max, this.yZoomEnd, this.noYZoom, this._y2Min, this._y2Max);
    }

    _yZoomMin(specYMin, specYMax) {
        return axisBarMin(this._yMin, this._yMax, specYMin, specYMax);
    }

    _yZoomMax(specYMin, specYMax) {
        return axisBarMax(this._yMin, this._yMax, specYMin, specYMax);
    }

    _enabled(type, minValue, maxValue, zoomStart, zoomEnd) {
        return !typeIsFullRange(type, minValue, maxValue, zoomStart, zoomEnd);
    }

    _yEnabled(_yMin, _yMax, yZoomStart, yZoomEnd, specYMin, specYMax) {
        return this._enabled('number', this._yZoomMin(specYMin, specYMax), this._yZoomMax(specYMin, specYMax), yZoomStart, yZoomEnd);
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

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (['data', 'data2', 'showY2Axis'].some(propName => changedProperties.has(propName))) {
            this._computeLabels();
        }
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

    _yMinChangedEv(ev) {
        this._yMin = ev.detail.value;
    }

    _yMaxChangedEv(ev) {
        this._yMax = ev.detail.value;
    }

    _y2MinChangedEv(ev) {
        this._y2Min = ev.detail.value;
    }

    _y2MaxChangedEv(ev) {
        this._y2Max = ev.detail.value;
    }

    _yAxisNumberFormatChangedEv(ev) {
        this._yAxisNumberFormat = ev.detail.value;
    }

    _y2AxisNumberFormatChangedEv(ev) {
        this._y2AxisNumberFormat = ev.detail.value;
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

    refreshData() {
        this.$.chart.refreshData();
        this._computeLabels();
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
};

customElements.define(PTCS.ChartBar.is, PTCS.ChartBar);
