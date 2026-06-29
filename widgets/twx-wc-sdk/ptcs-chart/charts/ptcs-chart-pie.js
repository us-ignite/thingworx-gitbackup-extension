import {LitElement, html, css, nothing} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import '../axes/library-axis-ticks.js';
import {BehaviorChart} from '../ptcs-behavior-chart.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';

import 'ptcs-toolbar/ptcs-toolbar.js';
import 'ptcs-label/ptcs-label.js';
import '../ptcs-chart-layout.js';
import '../ptcs-chart-legend.js';
import '../ptcs-chart-state.js';
import './ptcs-chart-core-pie.js';

const hasArrayChanged = (newVal, oldVal) => Array.isArray(newVal) || Array.isArray(oldVal);

PTCS.ChartPie = class extends BehaviorChart(PTCS.BehaviorTabindex(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))))) {
    static get styles() {
        return css`
            :host {
                display: block;
            }

            :host([disabled]) {
                pointer-events: none;
            }

            [part=legend-area] {
                width: 100%;
                height: 100%;
            }

            [part=chart] {
                position: relative;
            }

            :host(:focus) {
                outline: none;
            }
        `;
    }

    render() {
        return html`
            <ptcs-chart-layout
                id="chart-layout"
                style="height:100%"
                part="chart-layout"
                .titlePos=${this.titlePos}
                .hideTitle=${!this.titleLabel}
                .notesPos=${this.notesPos}
                .notesAlign=${this.notesAlign}
                .hideNotes=${this._hideNotes()}
                .legendPos=${this.legendPos}
                .hideLegend=${this._hideLegend()}
                @eff-legend-pos-changed=${this._effLegendPosChangedEv}
                .sparkView=${this.sparkView}
                .actionBar=${this._actionBar()}
                .chartState=${this._chartState}>
                <div part="title-area"
                    slot="title"
                    style=${'text-align:' + this._getHorizontalAlignment(this.titlePos, this.titleAlign)}>
                    <ptcs-label part="title-label"
                        .label=${this.titleLabel}
                        variant=${this.titleVariant}
                        .horizontal-alignment=${this._getHorizontalAlignment(this.titlePos, this.titleAlign)}
                        multi-line>
                    </ptcs-label>
                </div>
                <div part="notes-area"
                    slot="notes"
                    style=${'text-align:' + this._getHorizontalAlignment(this.notesPos, this.notesAlign)}>
                    <ptcs-label
                        part="notes-label"
                        .label=${this.notesLabel}
                        variant="body"
                        .horizontal-alignment=${this._getHorizontalAlignment(this.notesPos, this.notesAlign)}
                        multi-line>
                    </ptcs-label>
                </div>
                <ptcs-chart-state
                    part="chart-state"
                    slot="chart-state"
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
                    .iconError=${this.iconStateError}>
                </ptcs-chart-state>
                <ptcs-chart-core-pie
                    id="chart"
                    slot="chart"
                    part="core-chart"
                    tabindex=${this._delegatedFocus}
                    .data=${this._data}
                    .valueFormatSpecifier=${this.valueFormatSpecifier}
                    .legend=${this._legend}
                    .tooltipTemplate=${this.tooltipTemplate}
                    .hideDataTooltips=${this.hideDataTooltips}
                    .filterLegend=${this._selectedLegend}
                    .donut=${this.donut}
                    .polar=${this.polar}
                    .padAngle=${this.padAngle}
                    .startAngle=${this.startAngle}
                    .endAngle=${this.endAngle}
                    .cornerRadius=${this.cornerRadius}
                    .highlightSelection=${this.highlightSelection}
                    .showValues=${this._showValues()}
                    .valuePos=${this.valuePos}
                    .percentLabel=${this.percentLabel}
                    .insideLabelShowHide=${this.insideLabelShowHide}
                    .singleInsideValueLabelType=${this.singleInsideValueLabelType}
                    .selectionMode=${this.selectionMode}
                    @chart-selection=${this._onSelectionChangedEv}
                    @chart-state-data-error-changed=${this._chartStateDataErrorChangedEv}
                    @chart-state-data-empty-changed=${this._chartStateDataEmptyChangedEv}>
                </ptcs-chart-core-pie>
                <div part="action-bar-area"
                    slot="action-bar">
                    <ptcs-toolbar
                        id="toolbar"
                        tabindex=${this._gcTabindex()}
                        part="action-bar"
                        .disabled=${this.disabled}
                        variant="secondary"
                        hide-filter
                        @activated=${this._toolbarAction}>
                    </ptcs-toolbar>
                </ptcs-toolbar>

                </ptcs-toolbar>
                </div>
                <div part="legend-area"
                    slot="legend">
                    <ptcs-chart-legend
                        id="legend"
                        part="legend"
                        tabindex=${this._tabindex()}
                        .items=${this._legend}
                        .shape=${this.legendShape}
                        .filter=${this.filterLegend}
                        .horizontal=${this._horizLegend()}
                        .maxWidth=${this.legendMaxWidth}
                        .align=${this.legendAlign}
                        .disabled=${this.disabled}
                        @selected-changed=${this._selectedLegendChangedEv}></ptcs-chart-legend>
                </div>
            </ptcs-chart-layout>
        `;
    }

    static get is() {
        return 'ptcs-chart-pie';
    }

    static get properties() {
        return {
            // Title label
            titleLabel: {
                type:      String,
                attribute: 'titleL-label'
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
                type:      Boolean,
                attribute: 'hide-data-tooltips'
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
                type:  String,
                state: true
            },

            _chartStateDataError: {
                type:  Boolean,
                state: true
            },

            // Computed by ptcs-chart-core-pie,
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

            hideLegend: {
                type:      Boolean,
                attribute: 'hide-legend'
            },

            // Names of legend items, if legend should be visible
            // Generated from data
            _legend: {
                type:  Array,
                state: true
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

            // Legend itemss currently selected in the legend component
            _selectedLegend: {
                type:  Array,
                state: true,
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

            legendMaxWidth: {
                type:      Number,
                attribute: 'legend-max-width'
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

            donut: {
                type: Number
            },

            polar: {
                type: Boolean
            },

            padAngle: {
                type:      Number,
                attribute: 'pad-angle'
            },

            startAngle: {
                type:      Number,
                attribute: 'start-angle'
            },

            endAngle: {
                type:      Number,
                attribute: 'end-angle'
            },

            cornerRadius: {
                type:      Number,
                attribute: 'corner-radius'
            },

            // highlight the selected selection
            highlightSelection: {
                type:      Boolean,
                attribute: 'highlight-selection'
            },

            showValues: {
                type:      Boolean,
                attribute: 'show-values'
            },

            // value Position: marker || in || out || out with line
            valuePos: {
                type:      String,
                attribute: 'value-pos'
            },

            percentLabel: {
                type:      Boolean,
                attribute: 'percent-label'
            },

            insideLabelShowHide: {
                type:      Boolean,
                attribute: 'inside-label-show-hide'
            },

            // Single Inside Value Label Type type: caption || body || label || title || large-title || sub-header || header || large-header
            singleInsideValueLabelType: {
                type:      String,
                attribute: 'single-inside-value-label-type'
            },

            // Specified chart data
            data: {
                type:       Array,
                hasChanged: hasArrayChanged
            },

            // chart data formated
            _data: {
                type:  Array,
                state: true
            },

            tooltipTemplate: {
                type:      String,
                attribute: 'tooltip-template'
            },

            // label type: number || date || string
            valueLabelType: {
                type:      String,
                attribute: 'value-label-type'
            },

            labelNumberFormatSpecifier: {
                type:      String,
                attribute: 'label-number-format-specifier'
            },

            labelDateFormatToken: {
                type:      String,
                attribute: 'label-date-format-token'
            },

            valueFormatSpecifier: {
                type:      String,
                attribute: 'value-format-specifier'
            },

            _delegatedFocus: {
                type:  String,
                state: true
            }
        };
    }

    constructor() {
        super();

        this.disabled = false;
        this.hideNotes = false;
        this.sparkView = false;
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (['valueLabelType', 'labelNumberFormatSpecifier', 'labelDateFormatToken', 'data'].some(
            propName => changedProperties.has(propName))) {
            this._updateData();
        }
    }

    static get _rightActions() {
        // Remove zoom buttons
        const set = new Set(['zoom-in', 'zoom-out', 'reset']);
        return Object.getPrototypeOf(this)._rightActions.filter(item => !set.has(item.id));
    }

    _gcTabindex() {
        return this._hideToolbar ? nothing : this._delegatedFocus;
    }

    _tabindex() {
        return this.filterLegend ? this._delegatedFocus : nothing;
    }

    _hideNotes() {
        return !this.notesLabel || this.hideNotes;
    }

    _hideLegend() {
        return this.hideLegend || !(this._legend instanceof Array) || !(this._legend.length > 0);
    }

    _horizLegend() {
        return this.effLegendPos === 'top' || this.effLegendPos === 'bottom';
    }

    _getHorizontalAlignment(pos, align) {
        if (pos === 'top' || pos === 'bottom') {
            return align;
        }

        return 'start';
    }

    _showValues() {
        return !this.sparkView && this.showValues;
    }

    // Someting has changed in the data
    _updateData() {
        let formater;
        let defaultLabel = '';
        if (this.valueLabelType === 'number') {
            formater = PTCS.formatNumber(this.labelNumberFormatSpecifier);
            defaultLabel = 0;
        } else if (this.valueLabelType === 'date') {
            formater = PTCS.formatDate(this.labelDateFormatToken);
        } else {
            formater = v => v;
        }
        this._data = (this.data instanceof Array
            ? this.data
            : []).map(item => [formater(item[0] || defaultLabel).toString(), item[1], item[2], item[3]]);
        this._legend = (this._data).map(item => ({
            label:    item[0],
            depfield: item[2] && /* istanbul ignore next */ item[2][0] ? /* istanbul ignore next */ item[2][0] : item[1][0]
        }));
    }

    _resetToDefaultValues() {
        this.shadowRoot.getElementById('legend')._resetToDefaultValues();
    }

    refreshData() {
        this.shadowRoot.getElementById('chart').refreshData();
    }

    _actionBar() {
        if (this._hideToolbar) {
            return null;
        }

        return this.actionBar || 'top';
    }

    get selectedData() {
        return this._chartSelection;
    }

    set selectedData(selection) {
        this.shadowRoot.getElementById('chart').selectData(selection);
    }

    // The core chart has changed the selection
    _onSelectionChangedEv(ev) {
        this._setChartSelection(ev.detail.selection);
        this.dispatchEvent(new CustomEvent('selected-data-changed', {
            bubbles:  true,
            composed: true,
            detail:   this._chartSelection || []
        }));
    }

    _effLegendPosChangedEv(ev) {
        this.effLegendPos = ev.detail.value;
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

    _selectedLegendChangedEv(ev) {
        this._selectedLegend = ev.detail.value;
    }
};

customElements.define(PTCS.ChartPie.is, PTCS.ChartPie);
