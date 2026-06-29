import {LitElement, html, css} from 'lit';
import {when} from 'lit/directives/when.js';
import {L2Pw} from 'ptcs-library/library-lit.js';
import {PTCS} from 'ptcs-library/library.js';
import {DataManager} from './grid-data.js';
import {DataViewerAPI} from './grid-view-api.js';
import {GridIdSet} from './grid-id.js';
import {columnName} from './grid-view.js';
import 'ptcs-toolbar/ptcs-toolbar.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';
import 'ptcs-confirmation/ptcs-confirmation.js';
import {QuickFilter} from './quick-filter.js';
import './ptcs-columns-display.js';
import './ptcs-core-grid.js';
import './ptcs-data-load-bar';


// Grid proxy properties for the View Configurator
const dataViewerProperties = [
    'singleLineHeader',
    'singleLineRows',
    'showRowNumbers',
    'maxHeightHeader',
    'maxHeightRow',
    'minHeightRow',
    'headerVerticalAlignment',
    'rowsVerticalAlignment',
    'canDelete',
    'rowDepField',
    'sortSelectionColumn',
    'externalSort',
    'interpolation',
    'templateCompiler',
    'columnDefName',
    'dragRows'
];

PTCS.Grid = class extends PTCS.BehaviorTabindex(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement)))) {
    static get styles() {
        return css`
        :host {
            display: flex;
            flex-direction: column;
            position: relative;
            overflow: hidden;
        }

        [part="label"] {
            min-width: unset;
            min-height: unset;
        }

        :host(:not([label])) [part="label"] {
            visibility: hidden;
        }

        :host([label=""]) [part="label"] {
            visibility: hidden;
        }

        :host([label]:not([label=""]):not([show-filter])) [part="label"] {
            padding-bottom: 4px;
        }

        ptcs-core-grid {
            flex: 1 1 auto;
        }

        ptcs-label, ptcs-toolbar {
            flex: 0 0 auto;
        }

        [part=message-container] {
            display: flex;
            align-items: center;
            justify-content: center;
            box-sizing: border-box;
            grid-column: 1 / 1000;
        }

        /* When data-loading, the message should be invisible */
        [part=message-container][invisible] {
            visibility: hidden;
        }

        [part=grid-control] {
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            align-items: flex-end;
        }

        [part=reset-button] {
            margin-bottom: 8px;
        }

        [part=reset-button][hidden] {
            display: none;
        }

        [part=grid-control][hidden] {
            display: none;
        }

        [part=columns-display] {
            z-index: 2;
            position: absolute;
        }`;
    }

    render() {
        return html`
            <ptcs-label part="label" .label=${this.label} .multiLine=${true} .horizontalAlignment=${this.labelAlignment}
                variant=${this.labelVariant} .disableTooltip=${true}></ptcs-label>
            <ptcs-label part="selected-rows-label" .label=${this._selectedRowsLabel} ?hidden=${this.hideSelectedItemsText || !this._selectedRowsLabel}
                .disableTooltip=${true}></ptcs-label>
            <ptcs-toolbar id="toolbar" part="grid-control" variant="secondary" tabindex=${this._gcTabindex()} .disabled=${this.disabled}
                ?hidden=${this._hideToolbar} .hideFilter=${!this.showFilter} .simpleFilter=${true}
                .filterString=${this.filterString} @filter-string-changed=${this._filterStringChangedEv}
                .filterLabel=${this.filterLabel} .filterHintText=${this.filterHintText} .rightOverflowLabel=${this.toolbarRightText}
            ></ptcs-toolbar>
            <ptcs-columns-display id="columns-display" part=columns-display .view=${this.view}
                .mode=${this._columnsDisplayMode} @mode-changed=${this._modeChangedEv} @columns-changed=${this._columnDisplayChangedEv}
                .options=${this.columnsMenuOptions} .visibleItems=${this.columnsMenuVisibleItems}
                .tooltipReorder=${this.columnsMenuReorderTooltip} .tooltipShow=${this.columnsMenuVisibilityTooltip}
                .applyButtonText=${this.applyButtonText} .cancelButtonText=${this.cancelButtonText}
            ></ptcs-columns-display>
            <ptcs-core-grid id="grid" part="core-grid" tabindex=${this._delegatedFocus}
                .disabled=${this.disabled} .view=${this.view} .data=${this.data}
                .hideHeader=${this.hideHeader} .selectRow=${this.selectRow} .resizeColumns=${this.resizeColumns}
                .editControlVisibility=${this.editControlVisibility} .inlineEditing=${this.inlineEditing}
                .disableRow=${this.disableRow} .disableChildRows=${this.disableChildRows} @new-row=${this._onNewRow}
                .rowEditFormTitle=${this.rowEditFormTitle} .rowEditFormAddTitle=${this.rowEditFormAddTitle}
                .updateButtonText=${this.updateButtonText} .addButtonText=${this.addButtonText} .applyButtonText=${this.applyButtonText}
                .cancelButtonText=${this.cancelButtonText}
                .dateLabel=${this.dateLabel} .monthLabel=${this.monthLabel} .yearLabel=${this.yearLabel}
                .hoursLabel=${this.hoursLabel} .minutesLabel=${this.minutesLabel} .secondsLabel=${this.secondsLabel}
                .meridiemLabel=${this.meridiemLabel} .delayedLoadingIndicator=${this.delayedLoadingIndicator}
                .delayedLoadingTooltip=${this.delayedLoadingTooltip} .delayedErrorTooltip=${this.delayedErrorTooltip}
                .selectLabel=${this.selectLabel} .cancelLabel=${this.cancelLabel}
                .parentLabel=${this.parentLabel} .noParentLabel=${this.noParentLabel}
                .hideValidationError=${this.hideValidationError} .validationErrorIcon=${this.validationErrorIcon}
                .hideValidationCriteria=${this.hideValidationCriteria} .validationCriteriaIcon=${this.validationCriteriaIcon}
                .hideValidationSuccess=${this.hideValidationSuccess} .validationSuccessIcon=${this.validationSuccessIcon}
                ?highlight-drafts=${this.highlightDrafts} .reorderColumns=${this.reorderColumns} .autoScroll=${this.autoScroll}
                .navigation=${this.navigation} .preventFocusRowWrap=${this.preventFocusRowWrap} .selectFollowsFocus=${this.selectFollowsFocus}
                .footerData=${this.footerData} .showFooter=${this.showFooter} .showHeaderRowInFooter=${this.showHeaderRowInFooter}
                .highlightNewRows=${this.highlightNewRows} .externalComponentId=${this._externalComponentId}
                .preventDoubleClickedEvent=${this.preventDoubleClickedEvent} .slottedRows=${this.slottedRows}
                .shiftKeySelection=${this.shiftKeySelection} .showErrorMessage=${!!this.errorMessage}
                .delayedLoadingDebounce=${this.delayedLoadingDebounce}>
                <div part="message-container" ?invisible=${this.dataLoading || this.hideMessage}>
                    <ptcs-icon part="message-icon" .icon=${this._messageIcon}></ptcs-icon>
                    <ptcs-label variant="label" part="message-label" .label=${this._messageText}></ptcs-label>
                </div>
            </ptcs-core-grid>
            <ptcs-confirmation id="dlg" primary-action-label="OK" hide-cancel-action></ptcs-confirmation>
            ${when(this.dataLoading, () => html`<ptcs-data-load-bar .size=${this.loadingIndicatorSize} .image=${this.loadingIndicatorImage}
                .showBar=${this.dataLoading} .delay=${this.loadingIndicatorDelay}>`)}`;
    }

    static get is() {
        return 'ptcs-grid';
    }

    static get properties() {
        return {
            disabled: {
                type:    Boolean,
                reflect: true
            },

            disableRow: {
                type: Object
            },

            disableChildRows: { // Only used in tree grid mode
                type:      Boolean,
                attribute: 'disable-child-rows'
            },

            // show filter?
            showFilter: {
                type:      Boolean,
                value:     false,
                reflect:   true,
                attribute: 'show-filter'
            },

            // Width of the simple filter control
            filterWidth: {
                type:      Number,
                observer:  '_filterWidthChanged',
                attribute: 'filter-width'
            },

            // show reset?
            showResetButton: {
                type:      Boolean,
                value:     false,
                attribute: 'show-reset-button'
            },

            resetButtonText: {
                type:      String,
                attribute: 'reset-button-text'
            },

            resetButtonType: {
                type:      String,
                value:     'transparent',
                attribute: 'reset-button-type'
            },

            displayButtonText: {
                type:      String,
                attribute: 'display-button-text'
            },

            // Keep selected rows (when filtering, scrolling, TBD: sorting, ...)
            clearFilteredSelection: {
                type:      Boolean,
                observer:  '_clearFilteredSelectionChanged',
                attribute: 'clear-filtered-selection'
            },

            // Filter search string
            filterString: {
                type:        String,
                notify:      true,
                observer:    '_filterStringChanged',
                observeWhen: 'immediate',
                attribute:   'filter-string'
            },

            // Label above filter control
            filterLabel: {
                type:      String,
                attribute: 'filter-label'
            },

            // Hint text for the filter control text field
            filterHintText: {
                type:      String,
                attribute: 'filter-hint-text'
            },

            _hideToolbar: {
                type:     Boolean,
                // eslint-disable-next-line max-len
                computed: '_computeHideToolbar(showFilter, showResetButton, showEditButton, showDeleteRowButton, showAddRowButton, showExpandAll, asynchronousNodeLoading, alwaysExpanded, columnsMenuOptions, customActions, customActionsPosition)'
            },

            // Hide the grid message
            hideMessage: {
                type:      Boolean,
                attribute: 'hide-message'
            },

            // Message to display in message pane
            _messageText: {
                type: String
            },

            // Icon for the message pane
            _messageIcon: {
                type: String
            },

            // Button label for right toolbar overflow button
            toolbarRightText: {
                type:      String,
                attribute: 'toolbar-right-text'
            },

            // Selected item boilerplate (singular)
            selectedItemText: {
                type:      String,
                observer:  '_selectedItemTextChanged',
                attribute: 'selected-item-text'
            },

            // Selected items boilerplate (plural)
            selectedItemsText: {
                type:      String,
                observer:  '_selectedItemsTextChanged',
                attribute: 'selected-items-text'
            },

            // Selected rows label
            _selectedRowsLabel: {
                type:  String,
                value: ''
            },

            // Message on no data (prompt to bind data)
            bindDataText: {
                type:      String,
                value:     'Bind data to the grid.',
                attribute: 'bind-data-text'
            },

            // Message on no data (alternative wording for runtime)
            noDataToDisplayText: {
                type:      String,
                value:     'There is no data to display.',
                attribute: 'no-data-to-display-text'
            },

            // Message on no data to show (after binding, but no rows data)
            noResultsText: {
                type:      String,
                value:     'No results.',
                attribute: 'no-results-text'
            },

            // Message for when all rows have been filtered out
            noMatchesText: {
                type:      String,
                value:     'No matches found.',
                attribute: 'no-matches-text'
            },

            // Display error message
            errorMessage: {
                type:      String,
                attribute: 'error-message'
            },

            // Set while data is being loaded, to display the loading indicator (indeterminate progress bar)
            dataLoading: {
                type:      Boolean,
                attribute: 'data-loading'
            },

            // Container size in pixels (same height / width) for the data loading indicator
            loadingIndicatorSize: {
                type:      Number,
                value:     200,
                attribute: 'loading-indicator-size'
            },

            // Delay in ms before the loading indicator is shown
            loadingIndicatorDelay: {
                type:      Number,
                value:     1000,
                attribute: 'loading-indicator-delay'
            },

            // Custom data loading image source url (the image replaces the default progress bar if loaded successfully)
            loadingIndicatorImage: {
                type:      String,
                attribute: 'loading-indicator-image'
            },

            // Hide header
            hideHeader: {
                type:      Boolean,
                attribute: 'hide-header'
            },

            // Select row by clicking anywhere on the row
            selectRow: {
                type:      Boolean,
                attribute: 'select-row'
            },

            // Data Viewer / View Configurator
            view: {
                type:     Object,
                observer: '_viewChanged'
            },

            // View Configurator created from ptcs-grid-column-def elements
            // Only created if ptcs-grid contains ptcs-grid-column-def elements.
            // If the client manually assign a View Configurator, _viewColDef is disabled / unloaded
            __viewColDef: {
                type: Object
            },

            // Data Manager
            data: {
                type:     Object,
                observer: '_dataChanged'
            },

            // Simplified interface for specifying data items
            items: {
                type:        Array,
                observer:    '_itemsChanged',
                observeWhen: 'immediate'
            },

            selectMethod: {
                type:        String,
                observer:    '_selectMethodChanged',
                observeWhen: 'immediate',
                attribute:   'select-method'
            },

            subItems: {
                type:      String,
                observer:  '_subItemsChanged',
                attribute: 'sub-items'

            },

            // selected indexes of data manager
            selectedIndexes: {
                type:        Array,
                notify:      true,
                observer:    '_selectedIndexesChanged',
                observeWhen: 'immediate',
                attribute:   'selected-indexes'
            },

            // Grid title
            label: {
                type:    String,
                reflect: true
            },

            expandAllText: {
                type:      String,
                value:     'Expand All',
                attribute: 'expand-all-text'
            },

            collapseAllText: {
                type:      String,
                value:     'Collapse All',
                attribute: 'collapse-all-text'

            },

            // [left] || center || right
            labelAlignment: {
                type:      String,
                attribute: 'label-alignment'

            },

            // Grid title label variant
            labelVariant: {
                type:      String,
                attribute: 'label-variant'
            },

            // Should we store user interactions in browser local storage? (effective only when gridId is defined)
            cacheRuntimeChanges: {
                type:      Boolean,
                reflect:   true, // Why is this needed?
                attribute: 'cache-runtime-changes'
            },

            // Id for string user interactions in browser local storage
            gridId: {
                type:      String,
                reflect:   true, // Why is this needed?
                attribute: 'grid-id'
            },

            resizeColumns: {
                type:      Boolean, // true: resize columns, false: don't resize columns, undefined: only resize tree toggle
                attribute: 'resize-columns'
            },

            reorderColumns: {
                type:      Boolean,
                value:     false,
                attribute: 'reorder-columns'

            },

            _resetButtonDisabled: {
                type:     Boolean,
                // eslint-disable-next-line max-len
                computed: '_disabledResetButton(_sortDeftChg, _filterDeftChg, _selectedDeftChg, _changeResizeCol, _changeColumns)'
            },

            // Header Label for row edit form
            rowEditFormTitle: {
                type:      String,
                attribute: 'row-edit-form-title'

            },

            // Header label for row edit form when it adds a new row
            rowEditFormAddTitle: {
                type:      String,
                attribute: 'row-edit-form-add-title'
            },

            // Label for "Update" button in row edit form
            updateButtonText: {
                type:      String,
                attribute: 'update-button-text'
            },

            // Label for "Update" button in row edit form when it adds a new row
            addButtonText: {
                type:      String,
                attribute: 'add-button-text'
            },

            // Label for "Apply" button in column reorder form
            applyButtonText: {
                type:      String,
                attribute: 'apply-button-text'
            },

            // Label for "Cancel" buttons (in column reorder form and row editor)
            cancelButtonText: {
                type:      String,
                attribute: 'cancel-button-text'
            },

            // Calendar labels
            dateLabel: {
                type:      String,
                attribute: 'date-label'
            },

            monthLabel: {
                type:      String,
                attribute: 'month-label'
            },

            yearLabel: {
                type:      String,
                attribute: 'year-label'
            },

            hoursLabel: {
                type:      String,
                attribute: 'hours-label'
            },

            minutesLabel: {
                type:      String,
                attribute: 'minutes-label'
            },

            secondsLabel: {
                type:      String,
                attribute: 'seconds-label'
            },

            meridiemLabel: {
                type:      String,
                attribute: 'meridiem-label'
            },

            selectLabel: {
                type:      String,
                attribute: 'select-label'
            },

            cancelLabel: {
                type:      String,
                attribute: 'cancel-label'
            },

            // The label  "Parent" in the edit form when adding an item in the tree grid
            parentLabel: {
                type:      String,
                attribute: 'parent-label'
            },

            // The label used as the "Parent" value in the edit form when adding a root item
            noParentLabel: {
                type:      String,
                attribute: 'no-parent-label'
            },

            // Edit mode: 'cell', 'row', 'grid' (anything else disables editing)
            edit: {
                type: String
            },

            // Show edit button in toolbar that allows you to turn on/off edit mode?
            showEditButton: {
                type:      Boolean,
                attribute: 'show-edit-button'
            },

            // Label for Edit toolbar button
            editButtonText: {
                type:      String,
                attribute: 'edit-button-text'
            },

            // Label for Save Edit toolbar button
            saveEditButtonText: {
                type:      String,
                attribute: 'save_edit-button-text'
            },

            // Label for Cancel Edit toolbar button
            cancelEditButtonText: {
                type:      String,
                attribute: 'cancel-edit-button-text'
            },

            // The edit state: when true all editable cells can be editied
            isEditable: {
                type:      Boolean,
                notify:    true,
                attribute: 'is-editable'
            },

            // Hide validation error message
            hideValidationError: {
                type:      Boolean,
                attribute: 'hide-validation-error'
            },

            // Hide validation criteria message (info state)
            hideValidationCriteria: {
                type:      Boolean,
                attribute: 'hide-validation-criteria'
            },

            // Hide validation success message
            hideValidationSuccess: {
                type:      Boolean,
                attribute: 'hide-validation-success'
            },

            // Icon for validation error
            validationErrorIcon: {
                type:      String,
                attribute: 'validation-error-icon'
            },

            // Icon for validation success
            validationSuccessIcon: {
                type:      String,
                attribute: 'validation-success-icon'
            },

            // Icon for validation criteria (info state)
            validationCriteriaIcon: {
                type:      String,
                attribute: 'validation-criteria-icon'
            },

            // Show Delete Row button in toolbar that allows you to delete the selected rows?
            showDeleteRowButton: {
                type:      Boolean,
                attribute: 'show-delete-row-button'
            },

            // Label for Delete Row toolbar button
            deleteRowButtonText: {
                type:      String,
                attribute: 'delete-row-button-text'
            },

            // Show Add Row button in toolbar? (It triggers an event for the client to act on)
            showAddRowButton: {
                type:      Boolean,
                attribute: 'show-add-row-button'
            },

            // Label for Add Row toolbar button
            addRowButtonText: {
                type:      String,
                attribute: 'add-row-button-text'
            },

            // True if any rows are aditable
            _hasEditableRows: {
                type:     Boolean,
                observer: '_hasEditableRowsChanged'
            },

            // 'icon' (default), 'link', or 'none'
            editControl: {
                type:      String,
                attribute: 'edit-control'
            },

            editControlLabel: {
                type:      String,
                attribute: 'edit-control-label'
            },

            editControlIcon: {
                type:      String,
                attribute: 'edit-control-icon'
            },

            // How and when to show the edit control: 'hover' (default), 'always', 'never'
            editControlVisibility: {
                type:      String,
                attribute: 'edit-control-visibility'
            },

            inlineEditing: {
                type:      Boolean,
                attribute: 'inline-editing'
            },

            // Are any inline editied changes invalid?
            _isInvalid: {
                type:     Boolean,
                observer: '_isInvalidChanged'
            },

            highlightDrafts: {
                type:      Boolean,
                attribute: 'highlight-drafts'
            },

            highlightNewRows: {
                type:      Boolean,
                attribute: 'highlight-new-rows'
            },

            _columnsDisplayMode: {
                type:     String,
                value:    'closed',
                observer: '_columnsDisplayModeChanged'
            },

            // none / show / reorder / both
            columnsMenuOptions: {
                type:      String,
                value:     'none',
                attribute: 'columns-menu-options'
            },

            columnsMenuVisibleItems: {
                type:      Number,
                value:     6,
                attribute: 'columns-menu-visible-items'
            },

            autoScroll: {
                type:      Boolean,
                attribute: 'auto-scroll'
            },

            // Do we allow to move to the previous/next row when pressing left/right arrow key from the
            // first/last item on a row?
            preventFocusRowWrap: {
                type:      Boolean,
                attribute: 'prevent-focus-row-wrap'
            },

            navigation: {
                type: String // row-first (default), cell-first, cell-only
            },

            selectFollowsFocus: {
                type:      Boolean,
                attribute: 'select-follows-focus'
            },

            footerData: {
                type:      Array,
                value:     () => [],
                attribute: 'footer-data'
            },

            showFooter: {
                type:      Boolean,
                value:     false,
                attribute: 'show-footer'
            },

            showHeaderRowInFooter: {
                type:      Boolean,
                value:     false,
                attribute: 'show-header-row-in-footer'
            },

            // Tooltip for the reorder icon in the column reorder form
            columnsMenuReorderTooltip: {
                type:      String,
                attribute: 'columns-menu-reorder-tooltip'
            },

            // Tooltip for the column visibility checkbox in the column reorder form
            columnsMenuVisibilityTooltip: {
                type:      String,
                attribute: 'columns-menu-visibility-tooltip'
            },

            showExpandAll: {
                type:      Boolean,
                attribute: 'show-expand-all'
            },

            alwaysExpanded: {
                type:      Boolean,
                observer:  '_alwaysExpandedChanged',
                attribute: 'always-expanded'
            },

            maxExpandedRows: {
                type:      Number,
                attribute: 'max-expanded-rows'
            },

            maxRowsMessageTitle: {
                type:      String,
                value:     'Maximum number of items reached',
                attribute: 'max-rows-message-title'
            },

            maxRowsMessage: {
                type:      String,
                value:     'You have reached the maximum number of expanded rows.',
                attribute: 'max-rows-message'
            },

            preserveRowExpansion: {
                type:      Boolean,
                attribute: 'preserve-row-expansion'
            },

            // Field in items that contain unique id
            idField: {
                type:      String,
                attribute: 'id-field'
            },

            asynchronousNodeLoading: {
                type:      Boolean,
                attribute: 'asynchronous-node-loading'

            },

            selectParentOnly: { // Only used in tree grid mode
                type:      Boolean,
                attribute: 'select-parent-only'
            },

            childDataServiceEvent: {
                type:      Boolean,
                attribute: 'child-data-service-event'
            },

            customActions: {
                type: Array
            },

            customActionsPosition: {
                type:      String,
                value:     'after',
                attribute: 'custom-actions-position'
            },

            _externalComponentId: {
                type: String
            },

            _changeResizeCol: Boolean, // Has user changed column widths?

            _changeColumns: Boolean, // Has user changed the column order or column visibility?

            //
            // DataViewerAPI properties
            //
            /* $NUP */
            // Display header rows as single Line?
            singleLineHeader: {
                type:      Boolean,
                attribute: 'single-line-header'
            },

            // Display grid rows as single Line?
            singleLineRows: {
                type:      Boolean,
                attribute: 'single-line-rows'
            },

            // Display grid rows numbers?
            showRowNumbers: {
                type:      Boolean,
                attribute: 'show-row-numbers'
            },

            // Maximum header height
            maxHeightHeader: {
                type:      String,
                attribute: 'max-height-header'
            },

            // Maximum row height
            maxHeightRow: {
                type:      String,
                attribute: 'max-height-row'
            },

            // Minimum row height
            minHeightRow: {
                type:      String,
                attribute: 'min-height-row'
            },

            // Selection button vertical alignment in header
            headerVerticalAlignment: {
                type:      String,
                attribute: 'header-vertical-alignment'
            },

            // Selection button vertical alignment in rows
            rowsVerticalAlignment: {
                type:      String,
                attribute: 'rows-vertical-alignment'
            },

            // Add a delete button column?
            canDelete: {
                type:      Boolean,
                attribute: 'can-delete'
            },

            // Name of field for row state formatting
            rowDepField: {
                type:      String,
                attribute: 'row-dep-field'
            },

            // Allow sorting based on selection state?
            sortSelectionColumn: {
                type:      Boolean,
                attribute: 'sort-selection-column'
            },

            // Use internal or external sort function when user clicks sort icon
            externalSort: {
                type:      Boolean,
                attribute: 'external-sort'
            },

            // Name of column definition element. Default: ptcs-column-def
            columnDefName: {
                type:      String,
                attribute: 'column-def-name'
            },

            // Default: { "prefix": "${", "suffix": "}" }
            interpolation: Object,

            // Default: {compileHead: createTemplateHeader, compileCell: createTemplateElement}
            templateCompiler: Object,

            // Can grid rows be dragged to new positions?
            dragRows: {
                type:      Boolean,
                attribute: 'drag-rows'
            },

            // Support selections: add a select button column: 'single' || 'multiple'
            // Note: the view configurator calls this property selectMethod
            selectButton: {
                type:      String,
                attribute: 'select-button',
                observer:  '_selectButtonChanged'
            },

            // Prevent user select when DoubleClickedEvent is bound
            preventDoubleClickedEvent: {
                type:      Boolean,
                attribute: 'prevent-double-clicked-event'
            },

            // Hides the selected rows count label
            hideSelectedItemsText: {
                type:      Boolean,
                attribute: 'hide-selected-items-text',
            },

            // How to indicated delayed loading: 'bar', 'thin-bar', 'spinner' (or whatever)
            delayedLoadingIndicator: {
                type:      String,
                attribute: 'delayed-loading-indicator',
            },

            // Tooltip for the delayed loading indicator while loading
            delayedLoadingTooltip: {
                type:      String,
                attribute: 'delayed-loading-tooltip'
            },

            // Tooltip for the delayed loading indicator after it has failed
            delayedErrorTooltip: {
                type:      String,
                attribute: 'delayed-error-tooltip'
            },

            // Delayed loading debounce timeout (ms). Time that the viewport must have been stable before change event is fired
            delayedLoadingDebounce: {
                type:      Number,
                attribute: 'delayed-loading-debounce',
            },

            /* /$NUP */

            // Should generated elements be slotted?
            slottedRows: {
                type:      Boolean,
                attribute: 'slotted-rows'
            },

            // enable multi-selection
            shiftKeySelection: {
                type:      Boolean,
                attribute: 'shift-key-selection'
            },

            // Focus delegation
            _delegatedFocus: String,

            // Open a confirmation dialog before row deletion? (widget level)
            // $NUP
            // deleteRowsConfirmation: Boolean
        };
    }

    static get observers() {
        return [
            '_observeMessage(data, bindDataText, noDataToDisplayText, noResultsText, noMatchesText, errorMessage)',
            // eslint-disable-next-line max-len
            '_initToolbar(resetButtonText, editButtonText, saveEditButtonText, cancelEditButtonText, addRowButtonText, deleteRowButtonText, displayButtonText, resetButtonType, customActions, customActionsPosition)',
            '_modifyToolbar(showResetButton, _resetButtonDisabled, columnsMenuOptions)',
            '_setEditMode(edit, isEditable, showEditButton, showAddRowButton, showDeleteRowButton)',
            '_setEditControl(editControl, editControlLabel, editControlIcon, editControlVisibility)',
            '_setExpandMode(showExpandAll, alwaysExpanded)',
            '_runtimeChangesChanged(cacheRuntimeChanges, gridId, __cacheRuntimeIsDirty)',
            '_setMaxExpandedRows(maxExpandedRows, data)',
            '_setChildDataServiceEvent(childDataServiceEvent, data)'
        ];
    }

    constructor() {
        super();

        this.shiftKeySelection = false;

        this.__cache = {}; // The grid configuration (that can be saved on the local storage)

        this.__toolbarAction = ev => {
            switch (ev.detail.action.id) {
                case 'reset-button':
                    this.resetAction();
                    break;
                case 'edit':
                    this.isEditable = true;
                    this.dispatchEvent(new CustomEvent('edit-started'));
                    break;
                case 'save':
                    this.saveAction();
                    break;
                case 'cancel':
                    this.cancelAction();
                    break;
                case 'delete-rows':
                    this.dispatchEvent(new CustomEvent('delete-row-clicked', {detail: {rowsIndex: this.selectedIndexes}}));
                    if (!this.deleteRowsConfirmation) {
                        this.deleteSelectedRows();
                    }
                    break;
                case 'add-row':
                    this.$.grid.addRow();
                    break;
                case 'columns-display-button':
                    // A hack to stop the toolbar from reopening the reorder menu if clicking on the Display toolbar button
                    if (Date.now() - this.__columnsMenuClosedAtTime < 100) {
                        // User clicked on display button to close the menu.
                        // Don't reopen the menu
                        break;
                    }
                    this._columnsDisplayR = ev.detail.r;
                    this.$['columns-display'].view = this.view;
                    this._columnsDisplayMode = (this._columnsDisplayMode === 'open' ? 'closed' : 'open');
                    break;
                case 'collapse-all':
                    if (this.data) {
                        this.data.collapseAll();
                    }
                    break;
                case 'expand-all':
                    if (this.data) {
                        this.data.expandAll();
                    }
                    break;
                default:
                    this.dispatchEvent(new CustomEvent('custom-actions-activated', {
                        composed: true,
                        detail:   ev.detail
                    }));
            }
        };

        this.__toolbarValue = ev => {
            switch (ev.detail.action.id) {
                case 'toggle-edit':
                    this.isEditable = ev.detail.value;
                    // Don't trigger the changed edit event unless the toggle is visible
                    if (this.edit === 'cell' || this.edit === 'row') {
                        this.dispatchEvent(new CustomEvent(this.isEditable ? 'edit-started' : 'edit-completed'));
                    }
                    break;
                default:
                    this.dispatchEvent(new CustomEvent('custom-actions-value-changed', {
                        composed: true,
                        detail:   ev.detail
                    }));
            }
        };

        this._resizeObserver = new ResizeObserver(() => requestAnimationFrame(() => {
            if (this.offsetWidth === 0 || this.offsetHeight === 0) {
                return; // Grid is not visible. Ignore event
            }
            const el = this.$.grid;
            if (el) {
                el.adjustView(this.__cache.gridWidth === this.offsetWidth);
            }
        }));
    }

    ready() {
        super.ready();

        // Enable edit mode if still unassigned and edit button is not used
        if (this.isEditable === undefined && !this.showEditButton) {
            this.isEditable = true;
        }
        // Configure View Configurator DOM interface
        this._mutationObserver = new MutationObserver(this._mutatedDom.bind(this));
        this._mutationObserver.observe(this, {childList: true, subtree: true, attributes: true, characterData: true});
        this._initViewColDef();

        // Add observers for the properties that are directly mapped to the view configurator
        dataViewerProperties.forEach(propName => {
            this._createPropertyObserver(propName, propValue => this._setViewProp(propName, propValue), false);
        });

        this._initToolbar();

        this._clickOutsideHandler = ev => {
            if (this._isEventOutside(ev) && this._columnsDisplayMode === 'open') {
                ev.preventDefault();
                ev.stopPropagation();

                this._columnsDisplayMode = 'closed';

                document.addEventListener('mouseup', () => {
                    // A hack to stop the toolbar from reopening the reorder menu if clicking on the Display toolbar button
                    this.__columnsMenuClosedAtTime = Date.now();
                }, {capture: true, once: true});
            }
        };

        this._mouseOutsideHandler = ev => {
            if (this._isEventOutside(ev) && this._columnsDisplayMode === 'open') {
                ev.preventDefault();
                ev.stopPropagation();
            }
        };

        if (this.toolbarRightText === undefined) {
            this.toolbarRightText = 'View';
        }

        // When the user resizes the columns widths
        this.$.grid.addEventListener('columns-resized', () => {
            this._setCachedProperty('widths', this.view.getWidthsExpression(false));
        });

        this.setExternalComponentId();

        this.$.grid.slottedMessage = true;
    }

    connectedCallback() {
        super.connectedCallback();
        this._resizeObserver.observe(this);
    }

    disconnectedCallback() {
        this._resizeObserver.unobserve(this);
        super.disconnectedCallback();
    }

    // eslint-disable-next-line spaced-comment
    _isEventOutside(/**@type {MouseEvent}*/ ev) {
        if (this._columnsDisplayMode === 'closed') {
            return true;
        }

        const rect = this.$['columns-display'].getBoundingClientRect();

        const {left, right, top, bottom} = rect;

        const {posX: x, posY: y} = PTCS.getCoordinatesFromEvent(ev);

        return !(x >= Math.floor(left) && x <= Math.floor(right) &&
            y >= Math.floor(top) && y <= Math.floor(bottom));
    }

    _columnsDisplayModeChanged(mode) {
        this.$.toolbar.setSelected('columns-display-button', mode === 'open');
        if (mode === 'open') {
            const gR = this.getBoundingClientRect();
            const cR = this.$.grid.getBoundingClientRect();
            this.$['columns-display'].style.right = `${gR.right - this._columnsDisplayR.right}px`;
            this.$['columns-display'].style.top = `${this._columnsDisplayR.bottom - gR.top + 8}px`;
            this.$['columns-display'].maxColumnsHeight = cR.height;
            document.addEventListener('mousedown', this._clickOutsideHandler, true);

            if (this.hasAttribute('tabindex')) {
                // Set the tabindex to be whatever the "main" component is having
                this.$['columns-display'].setAttribute('tabindex', this.getAttribute('tabindex'));
                setTimeout(() => {
                    this.$['columns-display'].focus({preventScroll: true});
                }, 100);
            }

            return;
        }

        document.removeEventListener('mousedown', this._clickOutsideHandler, true);
    }

    _computeHideToolbar(showFilter, showResetButton, showEditButton, showDeleteRowButton, showAddRowButton, showExpandAll,
        asynchronousNodeLoading, alwaysExpanded, columnsMenuOptions, customActions, customActionsPosition) {
        return !showFilter && !showResetButton && !showEditButton && !showDeleteRowButton && !showAddRowButton &&
            !(showExpandAll && !asynchronousNodeLoading && !alwaysExpanded) && columnsMenuOptions === 'none' &&
            (!customActions || customActionsPosition === 'none');
    }

    _gcTabindex() {
        return this._hideToolbar ? false : this._delegatedFocus;
    }

    _initToolbar() {
        const toolbar = this.$.toolbar;
        if (!toolbar) {
            return;
        }

        this._initQuickFilter();

        let actions = [
            // Toggle edit mode
            {
                type:   'toggle',
                id:     'toggle-edit',
                label:  this.editButtonText || 'Edit Mode',
                hidden: true,
                value:  this.isEditable,
                opt:    {icon: 'cds:icon_edit', value: this.isEditable}
            },
            // Enable edit mode
            {
                type:   'link',
                id:     'edit',
                label:  this.editButtonText || 'Edit Grid',
                hidden: true,
            },
            // Add row
            {
                type:     'button',
                id:       'add-row',
                label:    this.addRowButtonText || 'Add',
                hidden:   true,
                disabled: this._hasEditableRows,
                opt:      {variant: 'transparent', icon: 'cds:icon_add'}
            },
            // Delete selected rows
            {
                type:     'button',
                id:       'delete-rows',
                label:    this.deleteRowButtonText || 'Delete',
                hidden:   true,
                disabled: !(Array.isArray(this.selectedIndexes) && this.selectedIndexes.length > 0),
                opt:      {variant: 'transparent', icon: 'cds:icon_delete'}
            },
            // Save edits
            {
                type:     'button',
                id:       'save',
                label:    this.saveEditButtonText || 'Save',
                hidden:   true,
                disabled: this._isInvalid,
                opt:      {variant: 'primary'}
            },
            // Cancel edits
            {
                type:   'button',
                id:     'cancel',
                label:  this.cancelEditButtonText || 'Cancel',
                hidden: true,
                opt:    {variant: 'tertiary'}
            },
            // Expand all nodes
            {
                type:   'link',
                id:     'expand-all',
                label:  this.expandAllText || 'Expand All',
                hidden: true
            },
            // Collapse all nodes
            {
                type:   'link',
                id:     'collapse-all',
                label:  this.collapseAllText || 'Collapse All',
                hidden: true
            }
        ];

        if (Array.isArray(this.customActions) && this.customActions.length > 0) {
            if (this.customActionsPosition === 'before') {
                actions = this.customActions.concat(actions);
            } else if (this.customActionsPosition === 'after') {
                actions = actions.concat(this.customActions);
            }
        }

        toolbar.actions = actions;

        toolbar.rightActions = [
            // Open Display Columns button
            {
                type:  'button',
                id:    'columns-display-button',
                label: this.displayButtonText || 'Display',
                opt:   {variant: 'tertiary', icon: 'cds:icon_chevron_down', iconPlacement: 'right'}
            },
            // Reset button
            {
                type:     'button',
                id:       'reset-button',
                label:    this.resetButtonText || 'Reset',
                hidden:   !this.showResetButton,
                disabled: this._resetButtonDisabled,
                opt:      {
                    icon:    'cds:icon_refresh',
                    variant: this.resetButtonType
                }
            }
        ];

        toolbar.removeEventListener('activated', this.__toolbarAction);
        toolbar.addEventListener('activated', this.__toolbarAction);
        toolbar.removeEventListener('value-changed', this.__toolbarValue);
        toolbar.addEventListener('value-changed', this.__toolbarValue);

        toolbar.setArrowDownActivate('columns-display-button', true);

        this._setEditMode(this.edit, this.isEditable, this.showEditButton, this.showAddRowButton, this.showDeleteRowButton);
        this._modifyToolbar(this.showResetButton, this._resetButtonDisabled, this.columnsMenuOptions);
    }

    _disableToolbarAction(id, disabled) {
        const toolbar = this.$.toolbar;
        if (toolbar) {
            toolbar.setDisabled(id, disabled);
        }
    }

    _hideToolbarAction(id, hidden) {
        const toolbar = this.$.toolbar;
        if (toolbar) {
            toolbar.setHidden(id, hidden);
        }
    }

    _setToolbarValue(id, value) {
        const toolbar = this.$.toolbar;
        if (toolbar) {
            toolbar.setValue(id, value);
        }
    }

    _modifyToolbar(showResetButton, _resetButtonDisabled, columnsMenuOptions) {
        if (!this.$.toolbar) {
            return;
        }

        this.$.toolbar.setHidden('reset-button', !showResetButton);
        this.$.toolbar.setDisabled('reset-button', _resetButtonDisabled);
        this.$.toolbar.setHidden('columns-display-button', columnsMenuOptions === 'none');
    }

    modifyCustomAction(actionId, value, updateType) {
        switch (updateType) {
            case 'value':
                this.$.toolbar.setValue(actionId, value);
                break;
            case 'disabled':
                this.$.toolbar.setDisabled(actionId, value);
                break;
            case 'visible':
                this.$.toolbar.setHidden(actionId, !value);
                break;
            default:
                break;
        }
    }

    _isInvalidChanged(_isInvalid) {
        if (this.$.toolbar) {
            this.$.toolbar.setDisabled('save', _isInvalid);
        }
    }

    _updateInvalid() {
        this._isInvalid = this.view && this.view.editLevel === 'grid' && !this.data.isValid;
    }

    __enableAddRowButton() {
        // Disable "Add Row" button if there isn't any editable fields or if the grid is a tree grid with more than one selected row.
        // For Tree Grid:
        // - zero selected rows: add new item last
        // - one selected row: add new item as child of the selected item
        // - multiple selected rows: not allowed to add item
        this._disableToolbarAction('add-row',
            !this._hasEditableRows || (this.data && this.data.isTreeGrid && this.selectedIndexes && this.selectedIndexes.length > 1));
    }

    _hasEditableRowsChanged() {
        this.__enableAddRowButton();
    }

    _setEditMode(edit, isEditable, showEditButton, showAddRowButton, showDeleteRowButton) {
        // Can rows be edded or deleted?
        const canAddDelete = !showEditButton || isEditable;

        // Is inline editing enabled?
        const inlineEditing = edit === 'cell' || edit === 'row' || edit === 'grid';

        // Should grid show an edit button?
        const editable = showEditButton && (inlineEditing || showAddRowButton || showDeleteRowButton);
        const showEditToggle = editable && edit !== 'grid'; // Show as single toggle?
        const showEditButtons = editable && edit === 'grid'; // Show as Edit / Save / Cancel?

        if (this.view) {
            this.view.editLevel = isEditable && inlineEditing && edit;
        }
        this._hideToolbarAction('toggle-edit', !showEditToggle);
        this._hideToolbarAction('edit', !(showEditButtons && !isEditable));
        this._hideToolbarAction('save', !(showEditButtons && isEditable));
        this._hideToolbarAction('cancel', !(showEditButtons && isEditable));
        this._hideToolbarAction('add-row', !(showAddRowButton && canAddDelete));
        this._hideToolbarAction('delete-rows', !(showDeleteRowButton && canAddDelete));

        this._setToolbarValue('toggle-edit', isEditable);
    }

    _setExpandMode() {
        if (this.showExpandAll && !this.alwaysExpanded && !this.asynchronousNodeLoading) {
            const expandState = this.data && this.data.expandState;
            if (this.__oldEM === expandState) {
                return;
            }
            this.__oldEM = expandState;
            this._hideToolbarAction('expand-all', expandState === 'expanded');
            this._hideToolbarAction('collapse-all', expandState === 'collapsed' || expandState === 'partial');
        } else {
            if (this.__oldEM === undefined) {
                return;
            }
            this.__oldEM = undefined;
            this._hideToolbarAction('expand-all', true);
            this._hideToolbarAction('collapse-all', true);
        }
    }

    _alwaysExpandedChanged(alwaysExpanded) {
        if (this.data && alwaysExpanded) {
            this._autoExpandEnd = 0;
            this._autoExpand(this.data.baseLength);
        }
        if (this.view) {
            this.view.hideTreeToggle = alwaysExpanded || undefined;
        }
    }

    _initQuickFilter() {
        if (!this._quickFilter) {
            this._quickFilter = new QuickFilter();

            this._quickFilter.setLabel = (label) => {
                this._selectedRowsLabel = label;
            };
        }
    }

    _selectedItemTextChanged(selectedItemText) {
        this._initQuickFilter();
        this._quickFilter.selectedItemText = selectedItemText;
    }

    _selectedItemsTextChanged(selectedItemsText) {
        this._initQuickFilter();
        this._quickFilter.selectedItemsText = selectedItemsText;
    }

    _clearFilteredSelectionChanged(clearFilteredSelection) {
        this._initQuickFilter();
        this._quickFilter.clearFilteredSelection = clearFilteredSelection;
    }

    _filterStringChangedEv(ev) {
        this.filterString = ev.detail.value;
    }

    _filterStringChanged(filterString) {
        if (this._quickFilter) {
            this._quickFilter.filterString = filterString;
        }
    }

    // Set width of the simple filter
    _filterWidthChanged(filterWidth) {
        this.$.toolbar.$.toolbar.$.filter.style.width = filterWidth ? filterWidth + 'px' : '';
    }

    _modeChangedEv(ev) {
        this._columnsDisplayMode = ev.detail.value;
    }

    _onNewRow() {
        this.filterString = '';
    }


    scrollTo(index) {
        this.$.grid.scrollTo(index);
    }

    // Return the CSS min-width of the quick filter set via theming (if any)
    get quickFilterMinWidth() {
        return this.$.toolbar.$.toolbar.simpleFilterMinWidth;
    }

    // Externally specified row to expand
    expandRows(idsToExpand, replace = true) {
        this.__shouldAutoExpand = undefined;
        this._autoExpandEnd = 0;

        if (replace || !this._autoExpandSet) {
            this._autoExpandSet = new GridIdSet(idsToExpand);
        } else {
            idsToExpand.forEach(id => this._autoExpandSet.add(id));
        }
        if (this._autoExpandSet.size === 0) {
            this._autoExpandSet = undefined;
        }
        if (this._autoExpandSet || this.alwaysExpanded) {
            setTimeout(() => this._autoExpand(this.data.baseLength), 0);
        }
    }

    selectRows(idsToSelect, replace = true) {
        this._autoSelectEnd = 0;

        if (replace || !this._autoSelectSet) {
            this._autoSelectSet = new GridIdSet(idsToSelect);
        } else if (Array.isArray(idsToSelect)) {
            idsToSelect.forEach(id => this._autoSelectSet.add(id));
        }

        setTimeout(() => this._autoSelect(this.data.baseLength), 100);
    }

    _autoExpand(to) {
        if (!(this._autoExpandSet || this.alwaysExpanded) || this._autoExpandEnd >= to) {
            return;
        }

        const {data} = this;
        const a = [];

        while (this._autoExpandEnd < to) {
            const baseIndex = this._autoExpandEnd++;
            const id = this._autoExpandSet && data.baseItem(baseIndex)[this.idField];

            if ((id && this._autoExpandSet.delete(id)) || (this.alwaysExpanded && data.isCollapsedNode(baseIndex))) {
                a.push(baseIndex);
            }
        }

        if (this._autoExpandSet && this._autoExpandSet.size === 0) {
            this._autoExpandSet = undefined;
        }

        a.forEach(baseIndex => data.subTree(data.translateBaseIndexToIndex(baseIndex), true));
    }

    _autoSelect(to) {
        if (!this._autoSelectSet || this._autoSelectEnd >= to) {
            return;
        }

        const {data} = this;
        const selection = [];

        const addSubtree = ix => {
            selection.push(ix);
            const children = data.childRange(ix);
            if (Array.isArray(children)) {
                const [first, last] = children;
                for (let i = first; i <= last; i++) {
                    addSubtree(i);
                }
            }
        };

        const add = data.selectParentOnly ? ix => selection.push(ix) : addSubtree;

        while (this._autoSelectEnd < to) {
            const id = data.baseItem(this._autoSelectEnd++)[this.idField];
            if (id !== undefined && this._autoSelectSet.delete(id)) {
                add(this._autoSelectEnd - 1);
            }
        }

        if (this._autoSelectSet.size === 0) {
            this._autoSelectSet = undefined;
        }
        if (selection.length === 0) {
            return;
        }

        data.selected = Array.isArray(data.selected) ? [...data.selected, ...selection] : selection;
    }

    _disabledResetButton(_sortDeftChg, _filterDeftChg, _selectedDeftChg, _changeResizeCol, _changeColumns) {
        return !(_sortDeftChg || _filterDeftChg || _selectedDeftChg || _changeResizeCol || _changeColumns);
    }

    resetAction() {
        if (this.view) {
            this.view.setSortExpression(this.view.defaultSortExpr()); // Reset sorting
            this.view.setVisibilityExpression(); // Reset order and visibility
            this.view.setWidthsExpression(); // Reset column widths

            // Immediately update __cache with the reset values
            this._getRuntimeChanges();
        } else {
            this._setCachedProperty('sort');
            this._setCachedProperty('columns');
            this._setCachedProperty('widths');
        }

        this.filterString = ''; // Clear filtering

        if (this._selectedDeftChg && this.data) {
            this.data.applyDefaultSelected();
        }

        this.$.grid._resetScrollbars();

        this.dispatchEvent(new CustomEvent('reset-to-default', {bubbles: true, composed: true, detail: {}}));
        return;
    }

    getSortExpression() {
        return this.view ? this.view.getSortExpression() : {short: '', full: []};
    }

    setSortExpression(_sortExpression, opt) {
        if (this.data && this.view) {
            this.view.setSortExpression(_sortExpression, this.data, opt);
        } else {
            this._setCachedProperty('sort', _sortExpression || undefined);
        }
    }

    getOrderExpression() {
        return this.view && this.view.getOrderExpression();
    }

    getWidthsExpression() {
        return this.view && this.view.getWidthsExpression();
    }

    getVisibilityExpression() {
        return this.view && this.view.getVisibilityExpression();
    }

    deleteSelectedRows() {
        const selectedIndexes = this.selectedIndexes;
        this.selectedIndexes = [];
        this.data.deleteBaseItems(selectedIndexes);
    }

    _setEditControl(editControl, editControlLabel, editControlIcon, editControlVisibility) {
        if (this.view) {
            this.view.editControl = editControl;
            this.view.editControlValue = editControl === 'link' ? editControlLabel : editControlIcon;
            this.view.editControlVisibility = editControlVisibility;
        }
    }

    //
    // Simplified Data Manager API (control data manager with items array)
    //

    _itemsChanged(items) {
        const _items = Array.isArray(items) ? items : [];
        if (!this.data) {
            const data = new DataManager(_items);
            data.selectMethod = this.selectMethod;
            data.selected = this.selectedIndexes;
            data.selectParentOnly = this.selectParentOnly;
            this.data = data;
            if (this.subItems) {
                this._subItemsChanged(this.subItems);
            }
        } else if (this.data instanceof DataManager) {
            this.data.items = _items;

            // Immediately reopen as many preserved rows as possible, if so configured
            if (this.preserveRowExpansion && this.idField && this.__cache.expanded && _items.length && this.data.isTreeGrid) {
                const set = new Set(this.__cache.expanded.split('|'));
                this.data.expandSome(item => set.has(item[this.idField]));
            }
        }
    }

    _dataChanged(data, old) {
        if (old) {
            old.unobserve(this);
        }
        if (data) {
            data.observe(this);
            this.dmSelectMethod(this.data.selectMethod);
            this.dmSelection(this.data.selected);
            data.traceUpdates(); // Always trace all updates

            // Potentially activate "Expand All" / "Collapse All" - button
            this._setExpandMode();

            if (this.data.baseLength) {
                this.dmNewItems();
            }
        }

        if (this._quickFilter) {
            this._quickFilter.data = data;
        }
    }

    // New items
    dmNewItems() {
        if (this.alwaysExpanded) {
            setTimeout(() => {
                if (this.data.baseLength) {
                    this._autoExpandEnd = 0;
                    this._autoExpand(this.data.baseLength);
                }
            }, 0);
        } else {
            this.__shouldAutoExpand = true;
        }
    }

    // The data view has changed
    dmView() {
        this._observeMessage(this.data, this.bindDataText, this.noDataToDisplayText, this.noResultsText, this.noMatchesText, this.errorMessage);
        this._setExpandMode();

        if ((this._autoExpandSet || this.alwaysExpanded) && this.data.baseLength > this._autoExpandEnd) {
            requestAnimationFrame(() => this._autoExpand(this.data.baseLength));
        }

        if (this._autoSelectSet && this.data.baseLength > this._autoSelectEnd) {
            requestAnimationFrame(() => this._autoSelect(this.data.baseLength));
        }

        // check filter differs from default
        this._filterDeftChg = this.data.filter !== null;

        // Auto expand?
        if (this.__shouldAutoExpand) {
            if (this.__cache.expanded && this.idField) {
                this.expandRows(this.__cache.expanded.split('|'), false);
            }
        } else {
            this._saveTreeExpansion();
        }
    }

    get valueManager() {
        return this.view && this.view.valueManager;
    }

    // View configuration has changed
    _viewChanged(view, old) {
        if (old) {
            if (old.requestAnimationFrameId) {
                cancelAnimationFrame(old.requestAnimationFrameId);
            }
            old.unobserve(this);
        }

        if (view) {
            view.observe(this);

            // Properties controlled by ptcs-grid
            if (this.edit) {
                this._setEditMode(this.edit, this.isEditable, this.showEditButton, this.showAddRowButton, this.showDeleteRowButton);
            }
            if (this.editControl) {
                this._setEditControl(this.editControl, this.editControlLabel, this.editControlIcon, this.editControlVisibility);
            }
            view.hideTreeToggle = this.alwaysExpanded || undefined;

            // Inform system that we have a new view configuration
            if (!this.dvColumnsDef()) {
                // dvColunmsDef had no effect. Apply the current settings
                view.setSortExpression(this.__cache.sort, this.data, {reset: true, viewChanged: true});
                view.setVisibilityExpression(this.__cache.columns);
                view.setWidthsExpression(this.__cache.widths);
            }
        }

        if (this._quickFilter) {
            this._quickFilter.view = view;
        }
    }

    // Initialize Simplified API for View Configurator
    _initViewColDef() {
        const coldefName = this.columnDefName || 'ptcs-grid-column-def';
        // Only use simple View Configurator API if view is unassigned and ptcs-grid has ptcs-grid-column-def elements
        if (!this.view && this.querySelector(`${coldefName}, slot, template`)) {
            this.view = this.__viewColDef = new DataViewerAPI(this, coldefName);
            dataViewerProperties.forEach(propName => {
                if (this[propName] !== undefined) {
                    this.view[propName] = this[propName];
                }
            });
            if (this.selectButton !== undefined) {
                this.view.selectMethod = this.selectButton;
            }
        }
    }

    // Mutations on attributes or descendants (not shadow dom)
    _mutatedDom(mutations) {
        if (this.__viewColDef) {
            if (this.__viewColDef !== this.view) {
                console.warn('ptcs-grid-column-def based view configurator has been replaced by client');
                this.__viewColDef = null; // Replaced by client
            } else {
                this.__viewColDef.mutationEvent(mutations);
            }
        } else if (this.__viewColDef === undefined) {
            this._initViewColDef();
        }
    }

    _setViewProp(propName, propValue) {
        if (this.view && propName in this.view) {
            this.view[propName] = propValue;
        }
    }

    _selectButtonChanged(selectButton) {
        this._setViewProp('selectMethod', selectButton);
    }

    rebuildColumnDefs() {
        if (this.__viewColDef) {
            this.__viewColDef.rebuildColumnDefs();
        }
    }

    //
    // Simplified selection API
    //

    // The client updates the selectMethod
    _selectMethodChanged(selectMethod) {
        if (!selectMethod) {
            this.selectMethod = 'none';
        } else if (this.data) {
            this.data.selectMethod = selectMethod;
        }

        if (this.selectMethod === 'multiple' && this.shadowRoot) {
            this.$.grid._multiSelectionClearStart();
        }
    }

    // The data manager updates the selectMethod
    dmSelectMethod(method) {
        this.selectMethod = method;
    }

    _subItemsChanged(subItems) {
        if (!this.data) {
            // TODO: assign this property to data manager when it becomes available
            return;
        }
        if (typeof subItems === 'string') {
            this.data.subItems = item => item[subItems];
            this.data.subItemsState = item => Array.isArray(item[subItems]) ? false : undefined;
        } else {
            console.assert(!subItems, 'grid.subItems should specify a field name');
            this.data.subItems = undefined;
            this.data.subItemsState = undefined;
        }
    }

    // The client updates the selection
    _selectedIndexesChanged(selectedIndexes) {
        if (this.data) {
            this.data.selected = selectedIndexes;

            // Hack to manually get current selection, if data manager rejected selection
            const sel = this.data.selected;
            // eslint-disable-next-line no-nested-ternary
            const selected = Array.isArray(sel) ? sel : (typeof sel === 'number' ? [sel] : []);
            if (!PTCS.sameArray(this.selectedIndexes, selected)) {
                this.dmSelection(selected);
            }
        }
    }

    // An item has changed
    dmItem() {
        this._updateInvalid();
        this._saveTreeExpansion();
    }

    // Items has been added to the view (possible by insertion)
    dmInserted() {
        this._updateInvalid();
        this._setExpandMode();

        if (this._autoExpandSet || this.alwaysExpanded || this._autoSelectSet) {
            // Give the system time to report more changes before we start expanding and selecting new children
            requestAnimationFrame(() => {
                // Some nodes has been added. Look for any new nodes to expand or select
                if (this._autoExpandSet || this.alwaysExpanded) {
                    this._autoExpand(this.data.baseLength);
                }

                if (this._autoSelectSet) {
                    this._autoSelect(this.data.baseLength);
                }
            });
        }

        this._saveTreeExpansion();
    }

    // Items has been removed from the view (possible by deletion)
    dmRemoved() {
        this._updateInvalid();
        this._setExpandMode();
        this._saveTreeExpansion();
    }

    // The data manager updates the selection
    dmSelection(selection) {
        if (this.selectedIndexes === selection) {
            // Update was written to the existing array
            this.dispatchEvent(new CustomEvent('selected-indexes-changed', {bubbles: false, composed: false, detail: {value: selection}}));
        } else {
            // eslint-disable-next-line no-nested-ternary
            this.selectedIndexes = Array.isArray(selection) ? selection : (typeof selection === 'number' ? [selection] : []);
        }

        // In tree grid mode, at most one row may be selected since this will be the parent of the new item
        this.__enableAddRowButton();

        this._disableToolbarAction('delete-rows', !this.selectedIndexes || this.selectedIndexes.length === 0);

        // check selected differs from default
        const _selection = selection || selection === 0 ? JSON.stringify(selection) : null;
        const _defaultSelected = this.data.defaultSelected && this.data.defaultSelected.length ? JSON.stringify(this.data.defaultSelected) : null;
        this._selectedDeftChg = _selection !== _defaultSelected;
    }

    // Select item index
    selectIndex(index, select) {
        if (this.data) {
            this.data.select(index, select);
        }
    }

    // Select row index (row index depends on filtering, sorting, etc...)
    selectRowIndex(row, select) {
        if (this.data) {
            this.data.select(this.data.baseIndex(row), select);
        }
    }

    // Returns true if item is selected
    isIndexSelected(index) {
        return this.data && this.data.isSelectedBaseIndex(index);
    }

    // Returns true if row is selected
    isRowSelected(row) {
        return this.data && this.data.isSelected(row);
    }

    // Displays an icon + message in message area when there are no rows in grid
    _observeMessage(data, bindDataText, noDataToDisplayText, noResultsText, noMatchesText, errorMessage) {
        clearTimeout(this._messageTimeout);
        this._messageTimeout = setTimeout(() => {
            this._messageText = this._messageIcon = undefined;
            if (errorMessage) {
                this._messageText = errorMessage;
                this._messageIcon = 'cds:icon_error';
            } else if (data) {
                if (data.length === 0) {
                    // Grid has no visible rows. Have they been filtered out or is the data empty?
                    this._messageText = data.baseLength > 0 ? noMatchesText : noResultsText;
                    this._messageIcon = 'cds:icon_not_visible';
                } // else - no message is shown
            } else if (this.isIDE) {
                // Prompt for builder to bind data
                this._messageText = bindDataText;
                this._messageIcon = 'cds:icon-bind';
            } else {
                // No data to display message
                this._messageText = noDataToDisplayText;
                this._messageIcon = 'cds:icon_not_visible';
            }
        }, 50);
    }

    /*
     * Grid configuration local storage
     */
    get gridStorageId() {
        return this.cacheRuntimeChanges && this.gridId && `${this.gridId}_ptcsgridconf`;
    }

    _runtimeChangesChanged(cacheRuntimeChanges, gridId, __cacheRuntimeIsDirty) {
        const $id = cacheRuntimeChanges ? gridId : undefined;

        // Switch id?
        if (this.__cache.$id !== $id) {
            this.__cache.$id = $id;
            if ($id) {
                this._loadRuntimeChanges();
            }
        }

        // Unsaved changes?
        if ($id && this.__cacheRuntimeIsDirty && !this.__savingRuntimeChanges) {
            this.__savingRuntimeChanges = true;
            requestAnimationFrame(() => {
                this.__savingRuntimeChanges = undefined;
                this._saveRuntimeChanges();
            });
        }
    }

    _loadRuntimeStorage(gridStorageId) {
        let gridConf = null;

        try {
            gridConf = JSON.parse(PTCS.restoreValueFromSession(gridStorageId));
        } catch (e) {
            console.warn('Grid failed to load configurations: Illegal JSON value', e);
        }

        const version = 3; // Current format version
        const formatVersion = gridConf && (gridConf.version || (Array.isArray(gridConf) ? 2 : 1)); // Older format?

        // Convert old format to new format if needed
        switch (formatVersion) {
            case 1:
                return {version, expanded: gridConf.expanded, configs: (gridConf.BaseID && [gridConf]) || []};
            case 2:
                return {version, expanded: gridConf[0].expanded, configs: gridConf};
            case 3:
                return {version, expanded: gridConf.expanded, configs: gridConf.configs || []};
            default:
                return {version, expanded: false, configs: []}; // No or unrecognized cached data. Reset
        }
    }

    _loadRuntimeChanges() {
        const gridStorageId = this.__columnNames && this.gridStorageId;
        if (!gridStorageId) {
            return false; // Nothing to load
        }

        this.__cache.$id = this.gridId; // Prevent double loading

        const gridConf =  this._loadRuntimeStorage(gridStorageId);

        if (this.__cache.expanded !== gridConf.expanded) {
            if (gridConf.expanded && this.idField) {
                this.expandRows(gridConf.expanded.split('|'), false);
            }
        }

        const config = gridConf.configs.find(item => item.BaseID === this.__columnNames);

        if (!config) {
            // No configuration found. Use default values
            this._getRuntimeChanges();
            return true; // "Loaded" default values and applied them
        }

        this.__cache.gridWidth = config.gridWidth; // Controls validity of widths expression

        if (this.__cache.sort !== config.sort) {
            this.__cache.sort = config.sort || undefined;
            if (this.view) {
                this.view.setSortExpression(config.sort, this.data, {reset: true});

                // For backwards compatibility
                requestAnimationFrame(() => this.dispatchEvent(new CustomEvent('grid-conf-sort-applied')));
            }
        }

        if (this.__cache.columns !== config.columns) {
            this.__cache.columns = config.columns;
            if (this.view) {
                this.view.setVisibilityExpression(config.columns);
            }
        }

        if (this.__cache.widths !== config.widths) {
            this.__cache.widths = config.widths;
            if (this.view) {
                this.view.setWidthsExpression(config.widths);
            }
        }

        this.__cacheRuntimeIsDirty = false;
        return true; // Loaded values and applied them
    }

    _saveRuntimeChanges() {
        const gridStorageId = this.__cacheRuntimeIsDirty && this.__columnNames && this.gridStorageId;
        if (!gridStorageId) {
            return; // Nothing to save
        }

        const gridConf =  this._loadRuntimeStorage(gridStorageId);
        const configs = gridConf.configs;
        const index = configs.findIndex(item => item.BaseID === this.__columnNames);
        if (index >= 0) {
            configs.splice(index, 1); // Remove old BaseID entry
        }
        const conf = {}; // Create new entry

        const assign = (propName, value) => {
            if (value) {
                conf[propName] = value;
            }
        };

        assign('sort', this.__cache.sort); // sorting
        assign('columns', this.__cache.columns); // column order and visibility
        assign('widths', this.__cache.widths); // columns width

        if (Object.keys(conf).length > 0) {
            assign('BaseID', this.__columnNames);
            assign('gridWidth', this.__cache.gridWidth);

            // Add new configuration first in array (LRU policy)
            configs.unshift(conf);

            // Avoid that configuration exceeds (say) 16 BaseID configurations
            if (configs.length > 16) {
                configs.pop();
            }
        }

        const updatedConf = {version: gridConf.version};
        if (this.__cache.expanded) {
            updatedConf.expanded = this.__cache.expanded; // expanded nodes
        }
        if (configs.length > 0) {
            updatedConf.configs = configs; // BaseID configs
        }

        // Save updated config
        PTCS.saveValueInSession(gridStorageId, updatedConf.expanded || updatedConf.configs ? JSON.stringify(updatedConf) : null);

        this.__cacheRuntimeIsDirty = false;
    }

    // Gte the current runtime changes from the view
    _getRuntimeChanges() {
        const {view} = this;
        this._setCachedProperty('columns', view ? view.getVisibilityExpression(false) : undefined); // column order and column visibility
        this._setCachedProperty('widths', view ? view.getWidthsExpression(false) : undefined);
        this._setCachedProperty('sort', (view && view.getSortExpression().short) || undefined);
    }


    _getColumnNames() {
        const columnsDef = this.view && this.view.columnsDef;
        return Array.isArray(columnsDef) && columnsDef.map(columnName).sort().join(',');
    }

    // When the view configurator changes (swiftly shift the user configuration)
    dvColumnsDef() {
        this._hasEditableRows = (this.view && this.view.columnsDef && this.view.columnsDef.some(d => d.editable)) || undefined;
        this._sortDeftChg = (this.__cache.sort !== (this.view.defaultSortExpr() || undefined));

        const old = this.__columnNames;
        const __columnNames = this._getColumnNames();
        if (__columnNames === old) {
            return false; // New and previous configuration is identical
        }
        if (old) {
            this._saveRuntimeChanges();
        }

        this.__columnNames = __columnNames;
        if (!this.__columnNames) {
            return false; // Nothing to change
        }

        // Loaded and applied changes?
        if (!this._loadRuntimeChanges()) {
            if (old) {
                // The current settings belongs to another kind of configuration (different column names). Get cache values from view
                this._getRuntimeChanges();
            }
            return false;
        }

        return true;
    }

    // Assign a config property (that can be saved on local storage)
    _setCachedProperty(name, value) {
        if (this.__cache[name] === value) {
            return;
        }
        this.__cache[name] = value;
        this._cachedPropertyChanged(name, value);
        this.__cacheRuntimeIsDirty = true;
        this.requestUpdate('__cacheRuntimeIsDirty');
    }

    // A config property has changed
    _cachedPropertyChanged(name, value) {
        switch (name) {
            case 'columns':
                this._changeColumns = !!value;
                break;
            case 'sort':
                this._sortDeftChg = (value !== ((this.view && this.view.defaultSortExpr()) || undefined));
                break;
            case 'widths':
                this._changeResizeCol = !!value; // Is there a non-default value?
                this.__cache.gridWidth = this.offsetWidth;
                break;
        }
    }

    dvChanged() {
        console.assert(this._getColumnNames() === this.__columnNames);
        this._setCachedProperty('columns', this.view.getVisibilityExpression(false)); // column order and column visibility
        this._setCachedProperty('widths', this.view.getWidthsExpression(false));
    }

    dvSort(expr) {
        this._setCachedProperty('sort', expr || undefined);
    }

    dvSortDefault(expr) {
        // The sort property works in mysterious ways (historical reasons)...
        this._cachedPropertyChanged('sort', this.__cache.sort); // Need to bump cache when default sort expression changes
    }

    _columnDisplayChangedEv(ev) {
        if (!this.view) {
            return; // Should never happen
        }

        const columnsOrderExp = ev.detail.columns;
        this.view.setVisibilityExpression(columnsOrderExp);
        this._setCachedProperty('columns', this.view.getVisibilityExpression(false));

        const columnsWidthExp = this.view.getWidthsExpression(false); // Don't want the default value
        if (columnsWidthExp) {
            const columnsOrder = columnsOrderExp.split(',').map(spec => spec.split(':'));
            const columnsWidth = columnsWidthExp.split(',').map(spec => spec.split(':'));

            let orderIndex = 0;
            const columnsWidthReordered = columnsWidth.map((el, i, arr) => {
                if (el[0].startsWith('#')) {
                    return el.join(':');
                }

                const orderName = columnsOrder.at(orderIndex++)[0];
                return arr.find(_el => orderName === _el[0]).join(':');
            });

            this.view.setWidthsExpression(columnsWidthReordered.join(','), {rebuildRowDef: false});
            this._setCachedProperty('widths', this.view.getWidthsExpression(false));
        }
    }

    _saveTreeExpansion() {
        if (!this.preserveRowExpansion || !this.data || !this.data.expandedOrCollapsed || !this.idField) {
            return;
        }

        const expanded = []; // Id's of expanded nodes (that has id's)

        this.data.getExpandedItems((item, isOpen) => {
            if (isOpen && item[this.idField]) {
                expanded.push(item[this.idField]);
            }
        });

        this._setCachedProperty('expanded', expanded.join('|'));
    }

    // Get view indexes of viewport
    get viewportRange() {
        if (!this.data || !this.view) {
            return undefined;
        }
        const coreGrid = this.$.grid;
        const chunker = coreGrid && coreGrid.$.chunker;
        if (!chunker) {
            return undefined;
        }
        const {startIx, endIx} = chunker;
        return (0 <= startIx && startIx < endIx && endIx <= this.data.length && [startIx, endIx]) || undefined;
    }

    // Get id of this grid that it attaches to its data editor popup
    getExternalComponentId() {
        return this._externalComponentId;
    }

    setExternalComponentId(id) {
        if (id) {
            this._externalComponentId = id;
        } else if (!this._externalComponentId) {
            this._externalComponentId = 'ptcs-grid-' + performance.now().toString().replace('.', '');
        }
    }

    // Rollback changes
    rollbackUpdates() {
        if (this.data) {
            this.data.rollbackUpdates();
        }
    }

    // Commit saved changes
    commitUpdates() {
        if (this.data) {
            this.data.commitUpdates();
        }
    }

    // Action when user clicks Save button
    saveAction() {
        if (this.isEditable) {
            this.isEditable = false;
            this.commitUpdates();
            this.dispatchEvent(new CustomEvent('edit-completed'));
        }
    }

    // Action when user clicks Cancel button
    cancelAction() {
        if (this.isEditable) {
            this.isEditable = false;
            this.rollbackUpdates();
            this.dispatchEvent(new CustomEvent('edit-cancelled'));
        }
    }

    _setMaxExpandedRows(maxExpandedRows, data) {
        if (data) {
            data.maxExpandedRows = maxExpandedRows;
        }
    }

    _setChildDataServiceEvent(childDataServiceEvent, data) {
        if (data) {
            data.childDataServiceEvent = childDataServiceEvent;
        }
    }

    openMaxExpandedRowsDialog() {
        const dlgEl = this.$.dlg;
        dlgEl.titleText = this.maxRowsMessageTitle;
        dlgEl.messageText = this.maxRowsMessage;
        dlgEl.open();
    }
};

customElements.define(PTCS.Grid.is, PTCS.Grid);
