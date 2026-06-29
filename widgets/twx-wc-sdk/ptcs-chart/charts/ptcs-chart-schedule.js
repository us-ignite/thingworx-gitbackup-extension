import {LitElement, html, css} from 'lit';
import {when} from 'lit/directives/when.js';
import {ifDefined} from 'lit/directives/if-defined.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import {axisMin, axisMax, typeValue, typeIsFullRange} from 'ptcs-library/library-chart.js';

import 'ptcs-toolbar/ptcs-toolbar.js';
import 'ptcs-datepicker/ptcs-datepicker.js';
import {BehaviorChart} from '../ptcs-behavior-chart.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';

import '../ptcs-chart-layout.js';
import '../ptcs-chart-legend.js';
import '../ptcs-chart-state.js';
import '../ptcs-chart-coord.js';
import '../axes/ptcs-chart-axis.js';
import '../zoom/ptcs-chart-zoom.js';
import './ptcs-chart-core-schedule.js';


PTCS.ChartSchedule = class extends BehaviorChart(PTCS.BehaviorTabindex(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))))) {
    static get styles() {
        return css`
            :host {
                display: block;
            }

            ptcs-chart-axis, [part=legend-area] {
                width: 100%;
                height: 100%;
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
                           .xZoom=${this._showZoomX()}
                           .yZoom=${this._showZoomY()}
                           .flipAxes=${!this.flipAxes}
                           .flipXAxis=${this.flipXAxis}
                           .flipYAxis=${this.flipYAxis}
                           .sparkView=${this.sparkView}
                           .xAxis=${!this.hideXAxis}
                           .yAxis=${!this.hideYAxis}
                           .yAxis2=${this._showY2Axis()}
                           .isReferenceLines=${this.isReferenceLines}
                           .actionBar=${this._actionBar()}
                           .chartState=${this._chartState}>
            <div part="title-area" slot="title"
                style=${'text-align:' + this._getHorizontalAlignment(this.titlePos, this.titleAlign)}>
                <ptcs-label part="title-label" .label=${this.titleLabel} variant=${this.titleVariant}
                    .horizontalAlignment=${this._getHorizontalAlignment(this.titlePos, this.titleAlign)} multi-line></ptcs-label>
            </div>
            <div part="notes-area" slot="notes"
                style=${'text-align:' + this._getHorizontalAlignment(this.notesPos, this.notesAlign)}>
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
            <ptcs-chart-coord slot="chart" part="chart"
                .flipAxes=${!this.flipAxes}
                .flipXAxis=${this.flipXAxis}
                .flipYAxis=${this.flipYAxis}
                .xTicks=${this._xTicks}
                .yTicks=${this._yTicks}
                .y2Ticks=${this.isReferenceLines ? this._yReferenceLines : this._y2Ticks}
                .showXRulers=${this.showXRulers}
                .showYRulers=${this.showYRulers}
                .showY2Rulers=${this.isReferenceLines}
                .isReferenceLines=${this.isReferenceLines}
                .yAxisRulerAlignment=${this.yAxisRulerAlignment}
                .frontRulers=${this.frontRulers}
                hide-zero-ruler
                @graph-width-changed=${this._graphWidthChangedEv}
                @graph-height-changed=${this._graphHeightChangedEv}
                .sparkView=${this.sparkView}>
                <ptcs-chart-core-schedule slot="chart" id="chart" part="core-chart" style="pointer-events: auto"
                    tabindex=${ifDefined(this.__delegatedFocus)}
                    .disabled=${this.disabled}
                    .legend=${this.legend}
                    .tooltipTemplate=${this.tooltipTemplate}
                    .hideDataTooltips=${this.hideDataTooltips}
                    .data=${this.data}
                    @labels-changed=${this._labelsChangedEv}
                    @x-min-changed=${this._xMinChangedEv}
                    @x-max-changed=${this._xMaxChangedEv}
                    @y-min-changed=${this._yMinChangedEv}
                    @y-max-changed=${this._yMaxChangedEv}
                    .flipAxes=${this.flipAxes}
                    .reverseXAxis=${this._reverseXAxis}
                    .reverseYAxis=${this.reverseYAxis}
                    .xScale=${this._xScale}
                    .yScale=${this._yScale}
                    .filterLegend=${this._selectedLegend}
                    .zoomSelect=${this._zoomMouseOpt(this.zoomSelect, this.noXZoom, this.noYZoom)}
                    .zoomDrag=${this._zoomMouseOpt(this.zoomDrag, this.noXZoom, this.noYZoom)}
                    .selectionMode=${this.selectionMode}
                    @chart-selection=${this._onSelectionChanged}
                    @selection=${this._onChartSelection}
                    @chart-state-data-error-changed=${this._chartStateDataErrorChangedEv}
                    @chart-state-data-empty-changed=${this._chartStateDataEmptyChangedEv}></ptcs-chart-core-schedule>
            </ptcs-chart-coord>
            <div part="action-bar-area" slot="action-bar">
                <ptcs-toolbar id="toolbar" tabindex=${ifDefined(this._gcTabindex())}
                    part="action-bar" .disabled=${this.disabled} variant="secondary" hide-filter @activated=${this._toolbarAction}>
                </ptcs-toolbar>
            </div>
            <div part="legend-area" ?hidden=${this.hideLegend} slot="legend">
                <ptcs-chart-legend part="legend" tabindex=${ifDefined(this._tabindex())}
                    .disabled=${this.disabled}
                    .items=${this._legend()}
                    .shape=${this.legendShape}
                    .filter=${this.filterLegend}
                    .horizontal=${this._horizLegend()}
                    max-width=${this.legendMaxWidth}
                    align=${this.legendAlign}
                    @selected-changed=${this._selectedLegendChangedEv}></ptcs-chart-legend>
            </div>
            <ptcs-chart-zoom slot="xzoom" id="zoomX" part="zoom-xaxis" .type=${this.labels} ?hidden=${this.noXZoom}
                tabindex=${ifDefined(this.__delegatedFocus)}
                .disabled=${this.disabled}
                .side=${this._xSide()}
                .minValue=${this._xMin}
                .maxValue=${this._xMax}
                .zoomStart=${this.xZoomStart} @zoom-start-changed=${this._xZoomStartChangedEv}
                .zoomEnd=${this.xZoomEnd} @zoom-end-changed=${this._xZoomEndChangedEv}
                .axisLength=${this._xSize()}
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
                .reverseSlider=${this._reverseXAxis}
                .intervalFromLabel=${this.xZoomIntervalFromLabel}
                .intervalToLabel=${this.xZoomIntervalToLabel}></ptcs-chart-zoom>
            <ptcs-chart-axis slot="xaxis" id="xaxis" part="xaxis" style="pointer-events: auto"
                tabindex=${ifDefined(this.__delegatedFocus)}
                .disabled=${this.disabled}
                .type=${this.labels} zoom
                .specMin=${this._specValue(this.specXMin, this.xZoomStart, this.noXZoom)}
                .specMax=${this._specValue(this.specXMax, this.xZoomEnd, this.noXZoom)}
                .side=${this._xSide()}
                .label=${this.xAxisLabel}
                .alignLabel=${this.xAxisAlign}
                .minValue=${this._xMin}
                .maxValue=${this._xMax}
                .size=${this._xSize()}
                .maxSize=${this.flipAxes ? this.horizontalAxisMaxHeight : this.verticalAxisMaxWidth}
                @ticks-changed=${this._xTicksChangedEv}
                .ticksRotation=${this.horizontalTicksRotation}
                .reverse=${this._reverseXAxis}
                @scale-changed=${this._xScaleChangedEv}
                ?hidden=${this.hideXAxis}
                .outerPadding=${this.outerPadding}
                .innerPadding=${this.innerPadding}></ptcs-chart-axis>
            <ptcs-chart-zoom slot="yzoom" id="zoomY" part="zoom-yaxis" type="date" ?hidden=${this.noYZoom}
                tabindex=${ifDefined(this.__delegatedFocus)}
                .disabled=${this.disabled}
                .side=${this._ySide()}
                .axisLength=${this._ySize()}
                .minValue=${this._yZoomMin()}
                .maxValue=${this._yZoomMax()}
                .zoomStart=${this.yZoomStart} @zoom-start-changed=${this._yZoomStartChangedEv}
                .zoomEnd=${this.yZoomEnd} @zoom-end-changed=${this._yZoomEndChangedEv}
                .rangePicker=${this.yZoomRange}
                .rangeStartLabel=${this.yZoomRangeStartLabel}
                .rangeEndLabel=${this.yZoomRangeEndLabel}
                .interval=${this.yZoomInterval}
                .intervalLabel=${this.yZoomIntervalLabel}
                .intervalControl=${this.yZoomIntervalControl}
                .intervalOrigin=${this.yZoomIntervalOrigin}
                .showIntervalAnchor=${this.yShowIntervalAnchor}
                .slider=${this.yZoomSlider}
                .sliderLabel=${this.yZoomSliderLabel}
                .sliderMinLabel=${this.yZoomSliderMinLabel}
                .sliderMaxLabel=${this.yZoomSliderMaxLabel}
                .reverseSlider=${this.reverseYAxis}
                .intervalFromLabel=${this.yZoomIntervalFromLabel}
                .intervalToLabel=${this.yZoomIntervalToLabel}
                .dateRangeHintText=${this.dateRangeHintText}></ptcs-chart-zoom>
            <ptcs-chart-axis slot="yaxis" id="yaxis" part="yaxis" style="pointer-events: auto"
                no-tabindex
                .disabled=${this.disabled}
                type="date" zoom
                .dateFormatToken=${this._updateDateFormatToken(this.numberOfYLabels, this.dateFormatToken)}
                .specMin=${this._specValue(this.specYMin, this.yZoomStart, this.noYZoom)}
                .specMax=${this._specValue(this.specYMax, this.yZoomEnd, this.noYZoom)}
                .numTicks=${this.numberOfYLabels}
                .side=${this._ySide()}
                .label=${this.yAxisLabel}
                .alignLabel=${this.yAxisAlign}
                .minValue=${this._yMin}
                .maxValue=${this._yMax}
                .size=${this._ySize()}
                .maxSize=${this.flipAxes ? this.verticalAxisMaxWidth : this.horizontalAxisMaxHeight}
                @ticks-changed=${this._yTicksChangedEv}
                .ticksRotation=${this.horizontalTicksRotation}
                .reverse=${this.reverseYAxis}
                @scale-changed=${this._yScaleChangedEv}
                ?hidden=${this.hideYAxis}></ptcs-chart-axis>
                ${when(this._showY2Axis(), () => html`<ptcs-chart-axis id="yaxis2" slot="yaxis2" part="yaxis2" style="pointer-events: auto"
                    no-tabindex
                    .disabled=${this.disabled}
                    type="date" zoom
                    .dateFormatToken=${this._updateDateFormatToken(this.numberOfYLabels, this.y2AxisDateFormatToken)}
                    .specMin=${this._specValue(this.specYMin, this.yZoomStart, this.noYZoom)}
                    .specMax=${this._specValue(this.specYMax, this.yZoomEnd, this.noYZoom)}
                    .numTicks=${this.numberOfYLabels}
                    .side=${this._y2Side()}
                    .label=${this.y2AxisLabel}
                    .alignLabel=${this.y2AxisAlign}
                    .minValue=${this._yMin}
                    .maxValue=${this._yMax}
                    .size=${this._ySize()}
                    .max-size=${this.flipAxes ? this.verticalAxisMaxWidth : this.horizontalAxisMaxHeight}
                    .reverse=${this.reverseYAxis}
                    @scale-changed=${this._yScaleChangedEv}
                    @ticks-changed=${this._y2TicksChangedEv}
                    .ticksRotation=${this.horizontalTicksRotation}
                    .dualTicks=${this._dualTicks()}
                    .referenceLines=${this._yAxisReferenceLines}
                    @eff-reference-lines-changed=${this._yReferenceLinesChangedEv}
                    .isReferenceLines=${this.isReferenceLines}
                    ?hidden=${!this._showY2Axis()}></ptcs-chart-axis>`)}
        </ptcs-chart-layout>`;
    }

    static get is() {
        return 'ptcs-chart-schedule';
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
                type: String
            },

            // Computed by ptcs-chart-core-schedule, based on data and data2
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
                attribute: 'hide-x-axis',
                observer:  '_hideXAxisChanged'
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
                attribute: 'hide-y-axis',
                observer:  '_hideYAxisChanged'
            },

            // Y-axis number of labels
            numberOfYLabels: {
                type:      Number,
                attribute: 'number-of-y-labels'
            },

            // Secondary y-axis
            showY2Axis: {
                type:      Boolean,
                attribute: 'show-y2-axis'
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

            y2AxisDateFormatToken: {
                type:      String,
                attribute: 'y2-axis-date-format-token'
            },

            dateFormatToken: {
                type:      String,
                attribute: 'date-format-token'
            },

            hideLegend: {
                type:      Boolean,
                attribute: 'hide-legend',
                notify:    true // Can be toggled via button
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

            // Legends currently selected in the legend component
            _selectedLegend: {
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

            // Connects ticks from x-axis to chart
            _xTicks: {
                type: Array
            },

            // Connects ticks from y-axis to chart
            _yTicks: {
                type: Array
            },

            // Connects ticks from second y-axis to chart
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

            // Put rulers on top of chart
            frontRulers: {
                type:      Boolean,
                attribute: 'front-rulers'
            },

            // Flag to use secondary axis for reference lines
            isReferenceLines: {
                type:      Boolean,
                attribute: 'is-reference-lines',
                computed:  '_computeIsReferenceLines(_yAxisReferenceLines, showY2Axis)'
            },

            // Reference lines (a.k.a. threshold lines) raw data
            referenceLines: {
                type:      Array,
                attribute: 'reference-lines',
                observer:  '_referenceLinesChanged'
            },

            // Reference lines for y-axis (only values valid according to the data type of the axis)
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

            // The x-value on a bar-chart is always [string] (labels)
            /*
            // x-axis type: number || date || [string]
            xType: {
                type: Object
            },
            */

            // x-axis labels
            labels: {
                type: Array
            },

            // Needed by chart behavior for zooming
            _xType: {
                type:     Array,
                computed: '_alias(labels)'
            },

            _yType: {
                type: String
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

            _specYValueMin: {
                type: Object,
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

            // zoom by dragging mouse: "x" || "y" || "xy" || undefined
            zoomDrag: {
                type:      String,
                attribute: 'zoom-drag'
            },

            // zoom by selecting two elements: "x" || "y" || "xy" || undefined
            zoomSelect: {
                type:      String,
                attribute: 'zoom-select'
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

            outerPadding: {
                type:      String,
                attribute: 'outer-padding'
            },

            innerPadding: {
                type:      String,
                attribute: 'inner-padding'
            },

            _delegatedFocus: String,

            // 'none' || 'single' || 'multiple'
            selectionMode: {
                type:      String,
                attribute: 'selection-mode'
            },

            tooltipTemplate: {
                type:      String,
                attribute: 'tooltip-template'
            },

            dateRangeHintText: {
                type:      String,
                attribute: 'date-range-hint-text'
            },

            legendcolors: {
                type: Object
            },

            _isZoomable$tb: {
                type:     Boolean,
                // eslint-disable-next-line max-len
                computed: '_isZoomable(noXZoom, noYZoom, xZoomRange, yZoomRange, xZoomInterval, yZoomInterval, xZoomSlider, yZoomSlider, zoomDrag, zoomSelect, showZoomButtons)'
            },

            _resetButtonEnabled$tb: {
                computed: '_enableZoomReset(_xType, _xMin, _xMax, xZoomStart, xZoomEnd, _yMin, _yMax, yZoomStart, yZoomEnd)'
            }
        };
    }

    constructor() {
        super();

        this.titleVariant = 'header';
        this.disabled = false;
        this._yType = 'date';
        this.flipAxes = false;
        this.hideXAxis = false;
        this.hideYAxis = false;
    }

    get _reverseXAxis() {
        return this.flipAxes ? this.reverseXAxis : !this.reverseXAxis;
    }

    get __delegatedFocus() {
        return this._delegatedFocus || false;
    }

    _updateDateFormatToken(numberOfLabels, dateFormatToken) {
        return (numberOfLabels > 0 && !dateFormatToken) ? 'YYYY-MM-DD HH:mm:ss.SSS' : dateFormatToken;
    }

    _gcTabindex() {
        return (!this._hideToolbar && this._delegatedFocus) || undefined;
    }

    _tabindex() {
        return (this.filterLegend && this._delegatedFocus) || undefined;
    }

    _isZoomable(noXZoom, noYZoom, xZoomRange, yZoomRange, xZoomInterval, yZoomInterval, xZoomSlider, yZoomSlider, zoomDrag, zoomSelect,
        showZoomButtons) {
        return this._showZoomX(showZoomButtons) || this._showZoomY(showZoomButtons);
    }

    _enableZoomReset(_xType, _xMin, _xMax, xZoomStart, xZoomEnd, _yMin, _yMax, yZoomStart, yZoomEnd) {
        return this._enabled(_xType, _xMin, _xMax, xZoomStart, xZoomEnd) || this._yEnabled(_yMin, _yMax, yZoomStart, yZoomEnd);
    }

    _zoomMouseOpt(zoom, noXZoom, noYZoom) {
        if (zoom === 'x') {
            return noXZoom ? false : 'x';
        }
        if (zoom === 'y') {
            return noYZoom ? false : 'y';
        }
        if (zoom === 'xy') {
            if (noXZoom) {
                return noYZoom ? false : 'y';
            }
            return noYZoom ? 'x' : 'xy';
        }
        return false;
    }

    _referenceLinesChanged(referenceLines) {
        if (!Array.isArray(referenceLines)) {
            this._yAxisReferenceLines = undefined;
            return;
        }

        // Filter out invalid values
        this._yAxisReferenceLines = referenceLines.reduce((acc, line) => {
            if (line.value instanceof Date) {
                acc.push(line);
            } else {
                const ms = Date.parse(line.value);
                if (!isNaN(ms)) {
                    acc.push({...line, value: new Date(ms)});
                }
            }
            return acc;
        }, []);
    }

    _computeIsReferenceLines(_yAxisReferenceLines, showY2Axis) {
        return !showY2Axis && _yAxisReferenceLines && _yAxisReferenceLines.length > 0;
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
        return this.flipAxes ? (this.flipXAxis ? 'top' : 'bottom') : (this.flipXAxis ? 'right' : 'left');
    }

    _ySide() {
        // eslint-disable-next-line no-nested-ternary
        return this.flipAxes ? (this.flipYAxis ? 'right' : 'left') : (this.flipYAxis ? 'top' : 'bottom');
    }

    _y2Side() {
        // eslint-disable-next-line no-nested-ternary
        return this.flipAxes ? (this.flipYAxis ? 'left' : 'right') : (this.flipYAxis ? 'bottom' : 'top');
    }

    _dualTicks() {
        return this.isReferenceLines ? null : this._yTicks;
    }

    _xSize() {
        return this.flipAxes ? this._graphWidth : this._graphHeight;
    }

    _ySize() {
        return this.flipAxes ? this._graphHeight : this._graphWidth;
    }

    _showY2Axis() {
        return this.showY2Axis || this.isReferenceLines;
    }

    _showZoom(noZoom, zoomRange, zoomInterval, zoomSlider, zoomMouse, showZoomButtons) {
        if (noZoom) {
            return false;
        }
        return zoomRange || zoomInterval || zoomSlider || zoomMouse || showZoomButtons;
    }

    _showZoomX(showZoomButtons) {
        const zoomMouse = [this.zoomDrag, this.zoomSelect].find(item => item === 'x' || item === 'xy');
        return this._showZoom(this.noXZoom, this.xZoomRange, this.xZoomInterval, this.xZoomSlider, zoomMouse, showZoomButtons);
    }

    _showZoomY(showZoomButtons) {
        const zoomMouse = [this.zoomDrag, this.zoomSelect].find(item => item === 'y' || item === 'xy');
        return this._showZoom(this.noYZoom, this.yZoomRange, this.yZoomInterval, this.yZoomSlider, zoomMouse, showZoomButtons);
    }

    _specValue(spec, zoom, noZoom) {
        if (noZoom || zoom === undefined || zoom === '' || zoom === null) {
            return spec;
        }
        return zoom;
    }

    _yZoomMin() {
        // If specYMin or specYMax is unspecified, the default behavior is to add 20% margin. We don't want this in the schedule chart
        return axisMin(this._yMin, this._yMax, 'date', this.specYMin || this._yMin, this.specYMax || this._yMax);
    }

    _yZoomMax() {
        // If specYMin or specYMax is unspecified, the default behavior is to add 20% margin. We don't want this in the schedule chart
        return axisMax(this._yMin, this._yMax, 'date', this.specYMax || this._yMax, this.specYMin || this._yMin);
    }

    _enabled(type, minValue, maxValue, zoomStart, zoomEnd) {
        return !typeIsFullRange(type, minValue, maxValue, zoomStart, zoomEnd);
    }

    _yEnabled(_yMin, _yMax, yZoomStart, yZoomEnd) {
        return this._enabled('date', _yMin, _yMax, yZoomStart, yZoomEnd);
    }

    _actionBar() {
        if (this._hideToolbar) {
            return null;
        }

        return this.actionBar || 'top';
    }

    _legend() {
        if (!this.legendcolors || !Array.isArray(this.legend)) {
            this.__oldLegend = undefined;
            return this.legend;
        }

        const legend = this.legend.map(item => {
            const color = this.legendcolors[(item && item.label) || item];
            return color ? Object.assign({color}, typeof item === 'string' ? {label: item} : {...item}) : (item || '');
        });

        const cmp = (a, b) => {
            return (a === b) ? true : (JSON.stringify(a) === JSON.stringify(b)); // A little hacky...
        };

        if (!PTCS.sameArray(legend, this.__oldLegend, cmp)) {
            this.__oldLegend = legend;
        }

        return this.__oldLegend;
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

        // For backwards compatibility. (Might not be needed)
        this.dispatchEvent(new CustomEvent('selected-data-changed', {detail: this._chartSelection}));
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

    _labelsChangedEv(ev) {
        this.labels = ev.detail.value;
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

    _y2TicksChangedEv(ev) {
        this._y2Ticks = ev.detail.value;
    }

    _yReferenceLinesChangedEv(ev) {
        this._yReferenceLines = ev.detail.value;
    }

    _onChartSelection(ev) {
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
        let reverseXAxis = this._reverseXAxis;
        let reverseYAxis = this.reverseYAxis;
        let d = ev.detail;
        if (!this.flipAxes) {
            d = {x: d.y, y: d.x, w: d.h, h: d.w};
            reverseXAxis = !reverseXAxis;
            reverseYAxis = !reverseYAxis;
        }
        const zd = this._zoomMouseOpt(this.zoomDrag, this.noXZoom, this.noYZoom);
        const zs = this._zoomMouseOpt(this.zoomSelect, this.noXZoom, this.noYZoom);

        if (zd === 'x' || zd === 'xy' || zs === 'x' || zs === 'xy') {
            // Make sure the selection at least covers one task bar
            const domain = xScale.domain();
            const [start, end] = reverseXAxis ? invert(xScale, d.x + d.w, d.x) : invert(xScale, d.x, d.x + d.w);
            if (typeValue(start, domain) <= typeValue(end, domain)) {
                [this.xZoomStart, this.xZoomEnd] = [start, end];
            }
        }
        if (zd === 'y' || zd === 'xy' || zs === 'y' || zs === 'xy') {
            [this.yZoomStart, this.yZoomEnd] = reverseYAxis // default y-axis is reversed
                ? invert(yScale, d.y, d.y + d.h) : invert(yScale, d.y + d.h, d.y);
        }
    }

    refreshData() {
        this.$.chart.refreshData();
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

customElements.define(PTCS.ChartSchedule.is, PTCS.ChartSchedule);
