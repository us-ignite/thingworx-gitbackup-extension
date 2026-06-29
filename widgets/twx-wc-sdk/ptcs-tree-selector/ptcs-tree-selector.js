import {LitElement, html, css} from 'lit';
import {map} from 'lit/directives/map.js';
import {when} from 'lit/directives/when.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-icon/ptcs-icon.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-divider/ptcs-divider.js';
import 'ptcs-textfield/ptcs-textfield.js';
import 'ptcs-list/ptcs-list.js';

// The "areas" in which the currently focused element might be
const AREA_CLEAR_SELECTION = 1;
const AREA_HIERARCHY = 2;
const AREA_OPEN_SEARCH = 3;
const AREA_NODES = 4;
const AREA_SEARCH = 5;

// Constants for the search area focus
const FOCUS_SEARCH_FIELD = -1;
const FOCUS_SEARCH_CLOSE_BUTTON = -2;
const FOCUS_SEARCH_RECENT_LIST = -3;

PTCS.TreeSelector = class extends PTCS.BehaviorTabindex(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(LitElement))) {
    static get styles() {
        return css`
            /* Basic styling, considered part of the component */
            :host {
                display: flex;
                flex-direction: column;
                outline: none;
            }

            [part~=main-label] {
                flex: 0 0 auto;
            }

            [part~=main] {
                display: flex;
                overflow: hidden;
                outline: none;
                height: 100%;
            }

            [part~=hierarchy-root] {
                display: flex;
                flex-direction: column;
                flex: 0 0 40%;
                box-sizing: border-box;
            }

            [part~=header] {
                display: flex;
                flex-direction: column;
            }

            [part~=clear-selection] {
                align-self: flex-end;
            }

            [part~=hierarchy] {
                display: flex;
                flex-direction: column;
                align-items: stretch;
                overflow: auto;
            }

            [part~=hierarchy]::-webkit-scrollbar {
                width: 6px;
            }
    
            [part~=hierarchy]::-webkit-scrollbar-thumb {
                width: 6px;
                background-color: #909090;
                border-radius: 3px;
            }
    
            [part~=parent] {
                display: flex;
                flex: 1 1 auto;
                align-items: center;
            }

            [part~=parent-node] {
                flex: 1 1 auto;
            }

            [part~=node-root] {
                display: flex;
                flex-direction: column;
                flex: 1 1 60%;
                box-sizing: border-box;                
            }

            [part~=search-header] {
                display: flex;
                flex-direction: row;
                justify-content: space-between;
                align-items: center;
            }

            [part~=search-header][no-label] {
                justify-content: flex-end;
            }

            [part~=node]::part(icon-image) {
                color: var(--ptcs-tree-selector--icon-color);
            }

            [part~=node][disabled]::part(icon-image) {
                color: var(--ptcs-tree-selector--disabled-icon-color, #d3d3d3);
            }

            [part~=parent-node]::part(icon-image) {
                color: var(--ptcs-tree-selector--icon-color);
            }

            [part~=parent-node][disabled]::part(icon-image) {
                color: var(--ptcs-tree-selector--disabled-icon-color, #d3d3d3);
            }

            [part~=hit-node]::part(icon-image) {
                color: var(--ptcs-tree-selector--icon-color);
            }

            [part~=hit-node][disabled]::part(icon-image) {
                color: var(--ptcs-tree-selector--disabled-icon-color, #d3d3d3);
            }

            [part~=nodes] {
                overflow: auto;
            }

            [part~=nodes]::-webkit-scrollbar {
                width: 6px;
            }
    
            [part~=nodes]::-webkit-scrollbar-thumb {
                width: 6px;
                background-color: #909090;
                border-radius: 3px;
            }

            [part~=search] {
                display: flex;
                flex-direction: column;
                flex: 1 1 auto;
            }

            [part~=search-hits] {
                display: flex;
                flex-direction: column;
                flex: 1 1 0px;
                align-items: stretch;
                overflow: auto;
            }

            [part~=search-hits]::-webkit-scrollbar {
                width: 6px;
            }
    
            [part~=search-hits]::-webkit-scrollbar-thumb {
                width: 6px;
                background-color: #909090;
                border-radius: 3px;
            }

            [part~=hit] {
                display: flex;
                flex: 0 0 auto;
                align-items: stretch;
            }

            [part~=hit-node] {
                flex: 1 1 auto;
            }

            [part~=nothing-to-select] {
                display: flex;
                flex-direction: row;
                align-items: center;
                justify-content: center;
            }

            [part~=no-selection] {
                display: flex;
                flex-direction: row;
                align-items: center;
                justify-content: center;
            }

            [part~=no-search-result] {
                display: flex;
                flex-direction: row;
                align-items: center;
                justify-content: center;
            }

            [part~=error-root] {
                height: 100%;
                display: flex;
                flex-direction: column;
                align-items: center;
            }

            [part~=error] {
                display: flex;
                flex: 1 1 auto;
                flex-direction: column;
                align-items: center;
            }

            [part=leaf-indicator] {
                width: 100%;
                flex: 0 0 auto;
                text-align: center;
            }

            [part=search-top] {
                display: flex;
                flex-direction: row;
                justify-content: space-between;
                align-items: flex-start;
            }
            `;
    }

    render() {
        return html`
            ${when(this.titleLabel, () => html`
                <ptcs-label part="main-label" .label=${this.titleLabel} variant="large-title"></ptcs-label>
            `)}
            ${when(this._searchModeOpen, this._renderSearch(), this._renderMain())}
        `;
    }

    _renderMain() {
        return () => html`
            <div part="main">
                ${this._renderHierarchyRegion()}
                <ptcs-divider part="divider" vertical></ptcs-divider>
                ${this._renderNodeRegion()}
            </div>`;
    }

    _renderClearSelection() {
        return () => html`
            <ptcs-button part="clear-selection" id="clearselection" .label=${this.clearSelectionLabel} variant="transparent"
                ?disabled=${this.disabled || !this._hasValidData() || !this._currentNode}
                no-tabindex @click=${this._clearSelectionClicked}></ptcs-button>`;
    }

    _renderParentNodes() {
        return () => map(this.getHierarchy(), (node, index) => html`
                <div part="parent">
                    <ptcs-icon part="parent-marker" icon=${index > 0 ? 'cds:icon_arrow_downwards_right' : 'cds:icon_empty'}
                        style="--ptcs-tree-selector--indent: ${index}"></ptcs-icon>
                    <ptcs-button part="parent-node" .label=${node.name}
                        .icon=${this.colorDotDisabled ? undefined : 'cds:icon_thumb_circle'}
                        variant="transparent" content-align="left" ?active-node=${node === this._displayedNode}
                        style="--ptcs-tree-selector--icon-color: ${node.color || 'var(--ptcs-tree-selector--default-icon-color, #40A3FE)'}"
                        ?disabled=${this.disabled} node-id=${node.__ID__} no-tabindex @click=${this._parentNodeClicked}></ptcs-button>
                </div>`);
    }

    _renderNoParents() {
        return () => html`
            <div part="no-selection">
                <ptcs-icon part="no-selection-icon" icon=${this.noSelectionIcon}></ptcs-icon>
                <ptcs-label part="no-selection-label" .label=${this.noSelectionMessage}></ptcs-label>
            </div>`;
    }

    _renderHierarchyRegion() {
        return html`
            <div part="hierarchy-root">
                <div part="header">
                    ${when(this.clearSelectionLabel, this._renderClearSelection())}
                </div>
                ${when(this._hasValidData(), () => html`
                    <div id="hierarchy" part="hierarchy">
                        ${when(this._currentNode, this._renderParentNodes(), this._renderNoParents())}
                    </div>`)}
            </div>`;
    }

    _renderSearchHeader() {
        return () => html`
            <div part="search-header" ?no-label=${!this.selectionLabel}>
                ${when(this.selectionLabel, () => html`
                    <ptcs-label part="search-label" .label=${this.selectionLabel} variant="title"></ptcs-label>
                `)}
                ${when(!this.disableSearch, () => html`
                    <ptcs-button part="open-search" id="opensearch" icon="cds:icon_search" variant="tertiary"
                        ?disabled=${this.disabled || !this._hasValidData()} no-tabindex
                        @click=${this.toggleSearchMode}></ptcs-button>
                `)}
            </div>`;
    }

    _renderNodeList() {
        return () =>
            map(this.getNodes(), (node) => html`
                <ptcs-button part="node" .label=${node.name} variant="tertiary"
                    .icon=${this.colorDotDisabled ? undefined : 'cds:icon_thumb_circle'}
                    style="--ptcs-tree-selector--icon-color: ${node.color || 'var(--ptcs-tree-selector--default-icon-color, #40A3FE)'}"
                    multi-line .maxNumberOfLines=${2} ?active-node=${this._withinHierarchy(node)}
                    ?disabled=${this.disabled} node-id=${node.__ID__} no-tabindex @click=${this._nodeClicked}></ptcs-button>`);
    }

    _renderNodesWhenData() {
        return () => html`
            ${when(!this._hasContent(), () => html`
                <ptcs-label part="leaf-indicator" variant="caption" label=${this.selectionWithNoChildNodesMessage}></ptcs-label>`)}
            <div id="nodes" part="nodes">
                ${when(this.hasNodes(), this._renderNodeList())}
            </div>`;
    }

    // Should we display that 'The selected item does not have any child nodes' message or not?
    _hasContent() {
        if (!this._currentNode) {
            // Root level
            return true;
        }

        if (this._displayedNode !== this._currentNode.__parent__) {
            return true;
        }

        return this._currentNode.contents && this._currentNode.contents.length > 0;
    }

    _renderNodes() {
        return () => html`
            ${when(this.selectionLabel || !this.disableSearch, this._renderSearchHeader())}
            ${when(this._hasValidData(), this._renderNodesWhenData(), this._renderError())}`;
    }

    _renderError() {
        return () => html`
            <div id="errorroot" part="error-root">
                <div id="error" part="error">
                    <ptcs-icon part="error-icon" .icon=${this._getEmptyStateIcon()}></ptcs-icon>
                    <ptcs-label part="error-label" .label=${this._getEmptyStateMessage()}></ptcs-label>
                </div>
            </div>
        `;
    }

    _getEmptyStateMessage() {
        switch (this.currEmptyState) {
            case undefined:
                return undefined;
            case 'empty-data':
                return this.emptyStateMessageEmptyData;
            case 'error':
                return this.emptyStateMessageError;
            case 'no-data-b':
                return this.emptyStateMessageNoDataB;
        }
        // Default ('no-data-a')
        return this.emptyStateMessageNoDataA;
    }

    _getEmptyStateIcon() {
        switch (this.currEmptyState) {
            case undefined:
                return undefined;
            case 'empty-data':
                return this.emptyStateIconEmptyData;
            case 'error':
                return this.emptyStateIconError;
            case 'no-data-b':
                return this.emptyStateIconNoDataB;
        }
        // Default ('no-data-a')
        return this.emptyStateIconNoDataA;
    }

    _renderHits() {
        return () =>
            map(this._getSearchHits(), (hit, index) => html`
                <div part="hit">
                    <ptcs-button part="hit-node" .label=${hit.name} variant="tertiary"
                        ?tall=${this.searchResultNodeHeight === 'tall'} content-align="left"
                        .icon=${this.colorDotDisabled ? undefined : 'cds:icon_thumb_circle'}
                        style="--ptcs-tree-selector--icon-color: ${hit.color || 'var(--ptcs-tree-selector--default-icon-color, #40A3FE)'}"
                        ?active-node=${index === 0} node-id=${hit.__ID__} ?disabled=${this.disabled} no-tabindex
                        @click=${this._searchHitClicked}></ptcs-button>
                </div>`);
    }

    _renderNoHits() {
        return () => html`
            <div part="no-search-result">
                <ptcs-icon part="no-search-result-icon" icon=${this.noSearchResultIcon}></ptcs-icon>
                <ptcs-label part="no-search-result-label" .label=${this.noSearchResultMessage}></ptcs-label>
            </div>`;
    }

    _renderSearchHits() {
        return () => html`
            <div id="hits" part="search-hits">
                ${when(this._hasHits(), this._renderHits(), this._renderNoHits())}
            </div>`;
    }

    _renderRecentSearches() {
        return () => html`
            <div id="recent" part="recent">
                <ptcs-list id="recentlist" part="recent-list" .items=${this._recentSearches} selector="name"
                    ?disabled=${this.disabled} no-tabindex @selected-items-changed=${this._recentSelected}></ptcs-list>
            </div>`;
    }

    _renderSearch() {
        return () => html`
            <div part="search">
                <div part="search-top">
                    <ptcs-textfield id="searchfield" part="search-field" .label=${this.searchLabel}
                        ?disabled=${this.disabled} no-tabindex @text-changed=${this._searchTextChanged}></ptcs-textfield>
                    <ptcs-button part="close-search" id="closesearch" icon="cds:icon_close" variant="transparent"
                        ?disabled=${this.disabled} no-tabindex @click=${this.toggleSearchMode}></ptcs-button>
                </div>
                ${when(this._currSearchString || !this._numRecents(), this._renderSearchHits(), this._renderRecentSearches())}
            </div>`;
    }

    _renderNodeRegion() {
        return html`
            <div part="node-root">
                ${when(!this._searchModeOpen, this._renderNodes(), this._renderSearch())}
            </div>`;
    }

    static get is() {
        return 'ptcs-tree-selector';
    }

    static get properties() {
        return {
            data: {
                type: Array
            },

            _data: {
                type: Array
            },

            disabled: {
                type:    Boolean,
                reflect: true
            },

            // Current node in the structure (or NULL if we are on the root level)
            _currentNode: {
                type: Object
            },

            // The currently *displayed* node in the structure. Normally the same as the _currentNode, unless you go "back" in the hierarchy...
            _displayedNode: {
                type: Object
            },

            // The current search string
            _currentSearch: {
                type: String
            },

            preselectedNode: {
                type:      String,
                attribute: 'preselected-node'
            },

            disableSearch: {
                type:      Boolean,
                attribute: 'disable-search'
            },

            noSelectionIcon: {
                type:      String,
                attribute: 'no-selection-icon'
            },

            noSelectionMessage: {
                type:      String,
                attribute: 'no-selection-message'
            },

            noSearchResultIcon: {
                type:      String,
                attribute: 'no-search-result-icon'
            },

            noSearchResultMessage: {
                type:      String,
                attribute: 'no-search-result-message'
            },

            // Empty state selectors---keep the names fairly generic since this will be used by the Widget layer (terms like
            // "binding" have no meaning here in the WC)
            emptyStateMessageNoDataA: {
                type:      String,
                attribute: 'empty-state-message-no-data-a'
            },

            emptyStateIconNoDataA: {
                type:      String,
                attribute: 'empty-state-icon-no-data-a'
            },

            emptyStateMessageNoDataB: {
                type:      String,
                attribute: 'empty-state-message-no-data-b'
            },

            emptyStateIconNoDataB: {
                type:      String,
                attribute: 'empty-state-icon-no-data-b'
            },

            emptyStateMessageEmptyData: {
                type:      String,
                attribute: 'empty-state-message-empty-data'
            },

            emptyStateIconEmptyData: {
                type:      String,
                attribute: 'empty-state-icon-empty-data'
            },

            emptyStateMessageError: {
                type:      String,
                attribute: 'empty-state-message-error'
            },

            emptyStateIconError: {
                type:      String,
                attribute: 'empty-state-icon-error'
            },

            // Defines which of the "generic" state strings ("no-data-a", "no-data-b", "empty-data", or "error") to display
            // in the error panel
            currEmptyState: {
                type:      String,
                attribute: 'curr-empty-state'
            },

            titleLabel: {
                type:      String,
                attribute: 'title-label'
            },

            selectionLabel: {
                type:      String,
                attribute: 'selection-label'
            },

            typeDelay: {
                type:      Number,
                attribute: 'type-delay'
            },

            clearSelectionLabel: {
                type:      String,
                attribute: 'clear-selection-label'
            },

            colorDotDisabled: {
                type:      Boolean,
                attribute: 'color-dot-disabled'
            },

            searchLabel: {
                type:      String,
                attribute: 'search-label'
            },

            searchResultNodeHeight: {
                type:      String,
                attribute: 'search-result-node-height'
            },

            selectionWithNoChildNodesMessage: {
                type:      String,
                attribute: 'selection-with-no-child-nodes-message'
            },

            _searchModeOpen: {
                type: Boolean
            },

            _hits: {
                type: Array
            }
        };
    }

    constructor() {
        super();

        // Internal
        this._currentNode = null;
        this._displayedNode = null;
        this._lookup = {};
        this._searchModeOpen = false;
        this._hits = [];

        // Labels
        this.titleLabel = '';
        this.selectionLabel = 'Choose Below';
        this.clearSelectionLabel = 'Clear Selection';

        // Icons
        this.noSelectionIcon = 'cds:icon_not_visible';
        this.noSearchResultIcon = 'cds:icon_not_visible';
        this.noDataIcon = 'cds:icon_error';

        // Messages
        this.noSelectionMessage = 'Select an item from the tree.';
        this.noSearchResultMessage = 'No results.';

        // State messages/icons
        this.emptyStateMessageNoDataA = 'No data A';
        this.emptyStateIconNoDataA = 'cds:icon_bind';
        this.emptyStateMessageNoDataB = 'No data B';
        this.emptyStateIconNoDataB = 'cds:icon_bind';
        this.emptyStateMessageEmptyData = 'Empty data';
        this.emptyStateIconEmptyData = 'cds:icon_not_visible';
        this.emptyStateMessageError = 'Error';
        this.emptyStateIconError = 'cds:icon_error';

        this.currEmptyState = 'empty-data';

        // Misc
        this.disableSearch = false;
        this.typeDelay = 500;
        this._currSearchString = '';

        this.colorDotDisabled = false;
        this.searchLabel = 'Search:';
        this.searchResultNodeHeight = 'short';
        this.selectionWithNoChildNodesMessage = 'The selected item does not have any child nodes';

        // Focus initially on the first node
        this._focusedSection = AREA_NODES;
        this._currNodeIdx = 0;
        this._currParentIdx = 0;
    }

    // Callback for BehaviorFocus
    _initTrackFocus() {
        this._trackFocus(this, () => {
            if (this._focusedEl) {
                return this._focusedEl;
            }
            if (!this._hasValidData()) {
                return this;
            }
            return null;
        });
    }

    _nodeHasChildren(nodeEl) {
        const nodeId = nodeEl.getAttribute('node-id');
        const node = this._lookup[nodeId];
        if (node) {
            return node.contents && node.contents.length > 0;
        }
        return false;
    }

    firstUpdated() {
        super.firstUpdated();

        // For keyboard navigation / managing focus
        this.addEventListener('keydown', ev => this._keyDown(ev));
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (changedProperties.has('data')) {
            this._initData();
        }
    }

    _initData() {
        // Reset the lookup
        this.__currId = 0;
        this._lookup = {};

        // Start over (no need to generate an event here?)
        this._data = null;
        this._currentNode = null;
        this._displayedNode = null;
        this._hits = null;
        this._searchModeOpen = false;

        // Focus initially on the first node
        this._focusedSection = AREA_NODES;
        this._currNodeIdx = 0;
        this._currParentIdx = 0;

        if (typeof this.data === 'function') {
            const getData = this.data;

            // Get the root level items
            this._data = getData(null);

            this._setParent(null, this._data);

            this._setCurrentNode(null);
        } else if (Array.isArray(this.data)) {
            // Create a copy of the input data that we can edit any way we want...
            this._data = PTCS.clone(this.data);

            // Traverse everything and add parent pointers "upwards" in the structure
            this._setParent(null, this._data);

            if (this.preselectedNode) {
                // Wait a while
                requestAnimationFrame(() => this.selectNode(this.preselectedNode));
            }
        }
    }

    _hasValidData() {
        return this._data && this._data.length > 0;
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        // Scan the nodes *once* after a render() to see the number of nodes / parent nodes / search hits
        const parentCntr = this.shadowRoot.getElementById('hierarchy');
        const parentNodeEls = parentCntr && parentCntr.querySelectorAll('[part~=parent-node]');
        this._numParentNodes = parentNodeEls ? parentNodeEls.length : 0;

        const nodeCntr = this.shadowRoot.getElementById('nodes');
        const nodeEls = nodeCntr && nodeCntr.querySelectorAll('[part~=node]');
        this._numNodes = nodeEls ? nodeEls.length : 0;

        const hitCntr = this.shadowRoot.getElementById('hits');
        const hitEls = hitCntr && hitCntr.querySelectorAll('[part~=hit-node]');
        this._numHits = hitEls ? hitEls.length : 0;

        // Make sure the currently focused element is set after the render() is complete
        this._setFocusedEl();
    }

    // Recursively remember who is your parent
    _setParent(parent, contents) {
        contents.forEach((node) => {
            // Know your heritage
            node.__parent__ = parent;

            // Create a unique ID for the node so that we can easily map the DOM elements to the original data
            node.__ID__ = '#' + this.__currId++;
            this._lookup[node.__ID__] = node;

            // Process any children recursively
            if (node.contents) {
                this._setParent(node, node.contents);
            }
        });
    }

    _scrollToNode(query) {
        const el = this.shadowRoot.querySelector(query);
        if (el) {
            el.scrollIntoViewIfNeeded();
        }
    }

    _setCurrentNode(node) {
        // If we are loading data dynamically, get the next level of children from the function
        if (node && typeof this.data === 'function') {
            const getData = this.data;
            if (!node.contents) {
                node.contents = getData(node);
                if (node.contents) {
                    this._setParent(node, node.contents);
                }
            }
        }

        // Set the new current node (this will update the UI automatically)
        this._currentNode = node;

        // For the "leaf" nodes without any children, we display the last parent level instead (with
        // the current node selected), instead of displaying an empty "no more to select" area...
        this._displayedNode = node;

        if (node && !(node.contents && node.contents.length > 0)) {
            this._displayedNode = node.__parent__;
        }

        // Inform the client what just happened
        this.dispatchEvent(new CustomEvent('action', {composed: true, detail: {item: node}}));

        if (node) {
            // Let things settle...
            requestAnimationFrame(() => {
                // ...before making sure that the item we just added is visible
                this._scrollToNode('[part~=parent]:last-of-type');
                this._scrollToNode('[part~=node][active-node]');
            });
        }
    }

    selectNode(text) {
        // Scan the lookup for the text in question
        if (this._lookup) {
            let id = 0;
            let node;
            do {
                node = this._lookup['#' + id++];
                if (node && node.name === text) {
                    this._setCurrentNode(node);
                    this.performUpdate();

                    // Update the current focus as well
                    if (node.contents) {
                        // The node in question has children, set the focus on the right one
                        this._focusedSection = AREA_NODES;
                        this._currNodeIdx = 0;
                    } else {
                        // End node, focus on the hierarchy instead
                        this._focusOnLastHierarchyNode();
                    }
                    return;
                }
            } while (node);
        }
    }

    // Get the idx of an element
    _getNodeIdx(nodeEl) {
        const nodeId = nodeEl.getAttribute('node-id');
        const nodeCntr = this.shadowRoot.getElementById('nodes');
        const nodeEls = nodeCntr && nodeCntr.querySelectorAll('[part~=node]');
        const numNodes = nodeEls ? nodeEls.length : 0;

        // Scan the nodes to find the index of the clicked one
        for (let i = 0; i < numNodes; i++) {
            const id = nodeEls[i].getAttribute('node-id');
            if (id === nodeId) {
                return i;
            }
        }

        // Default to 0
        return 0;
    }

    _nodeClicked(ev) {
        if (this.disabled) {
            return;
        }

        const nodeEl = ev.target;
        const nodeId = nodeEl.getAttribute('node-id');
        const node = this._lookup[nodeId];
        const hasContents = node.contents && node.contents.length > 0;

        // Is the clicked node selected or not?
        const isSelected = nodeEl.hasAttribute('active-node');

        // This should be all that is needed to update the UI...
        this._setCurrentNode(isSelected ? node.__parent__ : node);

        // We must update the _focusedEl as well...
        this.performUpdate();

        if (hasContents) {
            if (this._numNodes > 0) {
                this._currNodeIdx = 0;
            } else {
                this._focusOnLastHierarchyNode();
            }
        } else {
            // A "leaf" node was clicked, make sure we keep the focus on the clicked node
            this._currNodeIdx = this._getNodeIdx(nodeEl);
        }
    }

    // Recursively scan the data for a certain string
    _scanData(contents, str) {
        if (!contents) {
            return;
        }
        contents.forEach((node) => {
            if (node.name.toLowerCase().includes(str)) {
                this._hits.push(node);
            }

            // Process any children recursively
            if (node.contents) {
                this._scanData(node.contents, str);
            }
        });
    }

    _getId(node) {
        return Number(node.__ID__.substring(1));
    }

    _searchTextChanged(ev) {
        const currId = this._currentNode ? this._getId(this._currentNode) : 0;
        const searchString = ev.detail.value.toLowerCase();

        this._currSearchString = searchString;

        // Clear any previous hits
        this._hits = [];

        if (searchString) {
            // Scan all data for the search string
            this._scanData(this._data, searchString);
        }

        // Sort the hits, the ones "closest" to the current node should be displayed first
        this._hits.sort((a, b) => {
            const d1 = Math.abs(currId - this._getId(a));
            const d2 = Math.abs(currId - this._getId(b));
            // eslint-disable-next-line no-nested-ternary
            return d1 < d2 ? -1 : (d1 > d2 ? 1 : 0);
        });
    }

    _numRecents() {
        return this._recentSearches ? this._recentSearches.length : 0;
    }

    // Sets a specific node as the current one (called from the hit list and from the recent list)
    _setNode(node) {
        // ...and update the UI to show the hit node...
        this._setCurrentNode(node);

        // Update the focus as well
        this.performUpdate();

        // For some reason the focus overlay sometimes vanishes---two blur/focus calls solves this...
        this.blur();
        this.focus();

        // Wait a while with updating the focus...
        requestAnimationFrame(() => {
            const el = this.shadowRoot.querySelector('[part~=node][active-node]');
            if (el) {
                this._focusedSection = AREA_NODES;
                this._currNodeIdx = this._getNodeIdx(el);
            }
            this._setFocusedEl();
        });
    }

    _recentSelected(ev) {
        const selectedItems = ev.detail.value || [];
        if (selectedItems.length < 1) {
            return;
        }

        // Close the search area again...
        this._searchModeOpen = false;

        // Now move the selected item to the top of the recent searches
        const node = selectedItems[0];

        const recentIdx = this._recentSearches.findIndex(n => n === node);

        if (recentIdx > -1) {
            this._recentSearches.splice(recentIdx, 1);
            this._recentSearches.unshift(node);
        }

        this._setNode(node);
    }

    _addRecentSearch(node) {
        if (!node) {
            return;
        }
        if (!this._recentSearches) {
            this._recentSearches = [];
        } else {
            // First check if the exact same item already exists in the recent list
            const recentIdx = this._recentSearches.findIndex(n => n === node);

            if (recentIdx > -1) {
                // Clicked item already in the recent list, remove existing item
                this._recentSearches.splice(recentIdx, 1);
            } else if (this._recentSearches.length > 4) {
                // List already full, remove last item
                this._recentSearches.pop();
            }
        }

        // Add this as the first/top item
        this._recentSearches.unshift(node);
    }

    _searchHitClicked(ev) {
        if (this.disabled) {
            return;
        }

        const nodeId = ev.target.getAttribute('node-id');
        const node = this._lookup[nodeId];

        // Close the search part again...
        this._searchModeOpen = false;

        // Remember what we searched for
        this._addRecentSearch(node);

        this._setNode(node);
    }

    _parentNodeClicked(ev) {
        if (this.disabled) {
            return;
        }

        const nodeId = ev.target.getAttribute('node-id');
        const node = this._lookup[nodeId];

        // This should be all that is needed to update the UI...
        this._displayedNode = node;

        if (node) {
            this.performUpdate();

            if (node.contents) {
                this._focusOnFirstNode();
            } else {
                // No children, keep the focus on the left side
                this._focusOnLastHierarchyNode();
            }
            this._setFocusedEl();

            // Let things settle...
            requestAnimationFrame(() => {
                // ...before making sure that the highlighted item we just added is visible
                this._scrollToNode('[part~=node][active-node]');
            });
        }
    }

    _clearSelectionClicked() {
        if (this.disabled || !this._hasValidData()) {
            return;
        }

        this._setCurrentNode(null);
        this._focusOnFirstNode();
        this._setFocusedEl();
    }

    // Checks if one of the nodes in the right region is among the nodes in the current hierarchy
    _withinHierarchy(node) {
        if (this._currentNode) {
            for (let par = this._currentNode; par; par = par.__parent__) {
                if (node === par) {
                    return true;
                }
            }
        }
        // Not found
        return false;
    }

    getHierarchy() {
        if (this._currentNode) {
            const parentChain = [];
            const hasChildren = this._currentNode.contents && this._currentNode.contents.length > 0;
            const startNode = hasChildren ? this._currentNode : this._currentNode.__parent__;
            for (let par = startNode; par; par = par.__parent__) {
                parentChain.push(par);
            }
            return parentChain.reverse();
        }
        return [];
    }

    hasNodes() {
        const nodes = this.getNodes();
        return Array.isArray(nodes) && nodes.length > 0;
    }

    getNodes() {
        if (this._displayedNode) {
            return this._displayedNode.contents;
        }

        // Emit root node
        return this._data;
    }

    _getParentPath(node) {
        const parents = [];
        while (node) {
            parents.push(node.name);
            node = node.__parent__;
        }
        return parents.reverse().join(' / ');
    }

    _getPath(node) {
        const path = this._getParentPath(node.__parent__);
        const hitName = node.name + (!path ? '' : ` (${path})`);
        return hitName;
    }

    _hasHits() {
        return this._hits && this._hits.length > 0;
    }

    _getSearchHits() {
        return this._hits.map(hit => {
            // In the "hit" data we only need the id of the corresponding node and the color. The name is set to include the full path
            // of the node..
            return {name: this._getPath(hit), color: hit.color, __ID__: hit.__ID__};
        });
    }

    toggleSearchMode() {
        if (this.disabled || !this._hasValidData()) {
            return;
        }

        this._hits = [];
        this._currSearchString = '';
        this._searchModeOpen = !this._searchModeOpen;
        if (this._searchModeOpen) {
            // Focus on the search textfield
            requestAnimationFrame(() => this.shadowRoot.getElementById('searchfield').focus());
            this._focusedSection = AREA_SEARCH;

            // In the search "dialog", initial focus should be on the textfield
            this._searchIdx = FOCUS_SEARCH_FIELD;
        } else {
            this._focusedSection = AREA_NODES;
            this.performUpdate();
            // Without this, the focus overlay disappears...
            this.blur();
            this.focus();
        }
        this.performUpdate();
        this._setFocusedEl();
    }

    _keyDown(ev) {
        if (this.shadowRoot.getElementById('error')) {
            return;
        }

        switch (this._focusedSection) {
            case AREA_NODES:
                this._nodesKeyPress(ev);
                break;
            case AREA_HIERARCHY:
                this._hierarchyKeyPress(ev);
                break;
            case AREA_OPEN_SEARCH:
                this._openSearchKeyPress(ev);
                break;
            case AREA_SEARCH:
                this._searchKeyPress(ev);
                break;
            case AREA_CLEAR_SELECTION:
                this._clearSelectionKeyPress(ev);
                break;
        }

        // Finally, update the focus element to match the selection
        requestAnimationFrame(() => this._setFocusedEl());
    }

    _setFocusedEl() {
        // Are we in "error" mode? If so, focus on the entire element...
        if (this.shadowRoot.getElementById('error')) {
            this._focusedEl = null;
            return;
        }

        switch (this._focusedSection) {
            case AREA_NODES:
                {
                    const nodeCntr = this.shadowRoot.getElementById('nodes');
                    const nodeEls = nodeCntr && nodeCntr.querySelectorAll('[part~=node]');
                    if (nodeEls && nodeEls.length > this._currNodeIdx) {
                        this._focusedEl = nodeEls[this._currNodeIdx];
                        this._focusedEl.scrollIntoViewIfNeeded();
                    }
                }
                break;
            case AREA_HIERARCHY:
                {
                    const parentCntr = this.shadowRoot.getElementById('hierarchy');
                    const parentEls = parentCntr && parentCntr.querySelectorAll('[part~=parent-node]');
                    if (parentEls && parentEls.length > this._currParentIdx) {
                        this._focusedEl = parentEls[this._currParentIdx];
                        this._focusedEl.scrollIntoViewIfNeeded();
                    }
                }
                break;
            case AREA_CLEAR_SELECTION:
                this._focusedEl = this.shadowRoot.getElementById('clearselection');
                break;
            case AREA_OPEN_SEARCH:
                this._focusedEl = this.shadowRoot.getElementById('opensearch');
                break;
            case AREA_SEARCH:
                if (this._searchIdx === FOCUS_SEARCH_FIELD) {
                    const searchField = this.shadowRoot.getElementById('searchfield');
                    // Focus is on the edit field
                    if (searchField) {
                        // But just on the textbox, not on the entire thing
                        this._focusedEl = searchField.shadowRoot.getElementById('textbox');
                    }
                } else if (this._searchIdx === FOCUS_SEARCH_CLOSE_BUTTON) {
                    // Focus is on the close button
                    this._focusedEl = this.shadowRoot.getElementById('closesearch');
                } else if (this._searchIdx === FOCUS_SEARCH_RECENT_LIST) {
                    // Focus is on the recent list
                    this._focusedEl = this.shadowRoot.getElementById('recentlist');
                } else {
                    // Focus is on one of the search hits
                    const hits = this.shadowRoot.getElementById('hits');
                    const hitEls = hits && hits.querySelectorAll('[part~=hit-node]');
                    if (hitEls) {
                        this._focusedEl = hitEls[this._searchIdx];
                        this._focusedEl.scrollIntoViewIfNeeded();
                    }
                }
                break;
        }
    }

    _focusOnFirstNode() {
        if (this._numNodes) {
            this._focusedSection = AREA_NODES;
            this._currNodeIdx = 0;
            return true;
        }
        return false;
    }

    _focusOnLastNode() {
        if (this._numNodes) {
            this._focusedSection = AREA_NODES;
            this._currNodeIdx = this._numNodes - 1;
            return true;
        }
        return false;
    }

    _focusOnPrevRowNode() {
        const nodeEls = this.shadowRoot.getElementById('nodes').querySelectorAll('[part~=node]');

        const currNodeRect = nodeEls[this._currNodeIdx].getBoundingClientRect();
        const currTop = currNodeRect.top;
        const currLeft = currNodeRect.left;

        // Going up...
        if (this._currNodeIdx > 0) {
            for (let i = this._currNodeIdx - 1; i >= 0; i--) {
                const prevNodeRect = nodeEls[i].getBoundingClientRect();
                if (prevNodeRect.left === currLeft && prevNodeRect.top < currTop) {
                    this._currNodeIdx = i;
                    return true;
                }
            }
        }
        return false;
    }

    _focusOnNextRowNode() {
        const nodeEls = this.shadowRoot.getElementById('nodes').querySelectorAll('[part~=node]');
        const numNodes = nodeEls.length;

        const currNodeRect = nodeEls[this._currNodeIdx].getBoundingClientRect();
        const currTop = currNodeRect.top;
        const currLeft = currNodeRect.left;

        for (let i = this._currNodeIdx + 1; i < numNodes; i++) {
            const nextNodeRect = nodeEls[i].getBoundingClientRect();
            if (nextNodeRect.left === currLeft && nextNodeRect.top > currTop) {
                this._currNodeIdx = i;
                return true;
            }
        }
        return false;
    }

    _focusOnClearSelection() {
        const clearSelectionEl = this.shadowRoot.getElementById('clearselection');
        if (clearSelectionEl) {
            this._focusedSection = AREA_CLEAR_SELECTION;
            return true;
        }
        return false;
    }

    _focusOnOpenSearch() {
        const openSearchEl = this.shadowRoot.getElementById('opensearch');
        if (openSearchEl) {
            this._focusedSection = AREA_OPEN_SEARCH;
            return true;
        }
        return false;
    }

    _focusOnFirstHierarchyNode() {
        if (this._numParentNodes > 0) {
            this._focusedSection = AREA_HIERARCHY;
            this._currParentIdx = 0;
            return true;
        }
        return false;
    }

    _focusOnLastHierarchyNode() {
        if (this._numParentNodes > 0) {
            this._focusedSection = AREA_HIERARCHY;
            this._currParentIdx = this._numParentNodes - 1;
            return true;
        }
        return false;
    }

    _nodesKeyPress(ev) {
        let hasChildren;
        switch (ev.key) {
            case 'ArrowLeft':
                if (this._currNodeIdx > 0) {
                    this._currNodeIdx--;
                } else if (this._numParentNodes > 0) {
                    // Move focus back to the hierarchy section (or further back in the chain) Just keep whatever index was last active
                    this._focusedSection = AREA_HIERARCHY;
                } else if (!this._focusOnOpenSearch()) {
                    if (!this._focusOnClearSelection()) {
                        // This should always work (since we are in the nodes section)
                        this._focusOnLastNode();
                    }
                }
                ev.preventDefault();
                break;

            case 'ArrowRight':
                if (this._currNodeIdx + 1 < this._numNodes) {
                    this._currNodeIdx++;
                } else {
                    this._currNodeIdx = 0;
                }
                ev.preventDefault();
                break;

            case 'ArrowUp':
                if (!this._focusOnPrevRowNode()) {
                    if (!this._focusOnOpenSearch()) {
                        if (!this._focusOnLastHierarchyNode()) {
                            if (!this._focusOnClearSelection()) {
                                // All attempts to move backwards just failed, just move to the end of the node list
                                this._currNodeIdx = this._numNodes - 1;
                            }
                        }
                    }
                }
                ev.preventDefault();
                break;

            case 'ArrowDown':
                // Move focus to the next row
                if (!this._focusOnNextRowNode()) {
                    // We were on the last row, "wrap" focus to the Clear Selection button
                    if (!this._focusOnClearSelection()) {
                        // No 'Clear Selection' link/button, move focus to the first item in the hierarchy
                        if (!this._focusOnFirstHierarchyNode()) {
                            // If we get here, we should remain in the nodes list, just move focus to the first item...
                            this._currNodeIdx = 0;
                        }
                    }
                }
                ev.preventDefault();
                break;

            case 'Home':
                this._currNodeIdx = 0;
                ev.preventDefault();
                break;

            case 'End':
                this._currNodeIdx = this._numNodes - 1;
                ev.preventDefault();
                break;

            case 'Enter':
            case ' ':
                hasChildren = this._nodeHasChildren(this._focusedEl);
                this._focusedEl.click();
                this.performUpdate();
                if (hasChildren) {
                    if (this._numNodes > 0) {
                        this._currNodeIdx = 0;
                    } else {
                        this._focusOnLastHierarchyNode();
                    }
                }
                ev.preventDefault();
                break;

            default:
                if (ev.key.length === 1 && (ev.key >= 'A' && ev.key <= 'Z' || ev.key >= 'a' && ev.key <= 'z')) {
                    this._appendSelectFocusKey(ev.key);
                }
                break;
        }
    }

    _appendSelectFocusKey(key) {
        if (this.typeDelay > 0) {
            // Clear any existing timeout, prolonging the wait with another <n>ms
            clearTimeout(this.__selectFocusTimeoutId);

            // Are there any "pending" characters? If so, append this char to the previously typed ones...
            this.__selectFocus = this.__selectFocus ? this.__selectFocus + key : key;

            this.__selectFocusTimeoutId = setTimeout(() => {
                // Nothing typed in the last <typeDelay> ms, search for what we have...
                this._focusOnNamedNode(this.__selectFocus);

                // ...and forget anything previously typed
                this.__selectFocus = undefined;
            }, this.typeDelay);
        } else {
            // Just search for the single key that was just typed...
            this._focusOnNamedNode(key);
        }
    }

    _focusOnNamedNode(startsWith) {
        // Make the search case-insensitive
        const searchString = startsWith.toLowerCase();

        // The input data for the nodes in question
        const nodes = this._displayedNode ? this._displayedNode.contents : [];
        const numNodes = nodes.length;

        // We start with _currNodeIdx + 1 and work our way through all items, moving back to the beginning when we reach the end...
        for (let i = 1; i <= numNodes; i++) {
            const idx = (this._currNodeIdx + i) % numNodes;
            if (nodes[idx].name.toLowerCase().startsWith(searchString)) {
                this._currNodeIdx = idx;
                this._setFocusedEl();
                return;
            }
        }
    }

    _hierarchyKeyPress(ev) {
        switch (ev.key) {
            case 'ArrowUp':
                if (this._currParentIdx > 0) {
                    this._currParentIdx--;
                } else if (!this._focusOnClearSelection()) {
                    if (!this._focusOnLastNode()) {
                        if (!this._focusOnOpenSearch()) {
                            // No other sections---move to the last parent node
                            this._currParentIdx = this._numParentNodes - 1;
                        }
                    }
                }
                ev.preventDefault();
                break;

            case 'ArrowDown':
                if (this._currParentIdx + 1 < this._numParentNodes) {
                    this._currParentIdx++;
                } else if (!this._focusOnOpenSearch()) {
                    if (!this._focusOnFirstNode()) {
                        if (!this._focusOnClearSelection()) {
                            this._currParentIdx = 0;
                        }
                    }
                }
                ev.preventDefault();
                break;

            case 'ArrowRight':
                // Move focus to the node area
                this._focusOnFirstNode();
                ev.preventDefault();
                break;

            case 'Home':
                this._currParentIdx = 0;
                ev.preventDefault();
                break;

            case 'End':
                this._currParentIdx = this._numParentNodes - 1;
                ev.preventDefault();
                break;

            case 'Enter':
            case ' ':
                this._focusedEl.click();
                this.performUpdate();
                this._focusOnFirstNode();
                ev.preventDefault();
                break;
        }
    }

    _clearSelectionKeyPress(ev) {
        switch (ev.key) {
            case 'ArrowDown':
            case 'ArrowRight':
                // Focus should go to the first node in the AREA_HIERARCHY section
                if (!this._focusOnFirstHierarchyNode()) {
                    if (!this._focusOnOpenSearch()) {
                        this._focusOnFirstNode();
                    }
                }
                ev.preventDefault();
                break;
            case 'ArrowUp':
            case 'ArrowLeft':
                // Focus should go to the last node in the AREA_NODES section
                if (!this._focusOnLastNode()) {
                    if (!this._focusOnOpenSearch()) {
                        this._focusOnLastHierarchyNode();
                    }
                }
                ev.preventDefault();
                break;
            case 'Enter':
            case ' ':
                this._focusedEl.click();
                this.performUpdate();
                if (this._numNodes > 0) {
                    this._focusedSection = AREA_NODES;
                    this._currNodeIdx = 0;
                }
                ev.preventDefault();
                break;
        }
    }

    _focusOnLastSearchHit() {
        // The current number of search hits were stored in updated()
        if (this._numHits > 0) {
            this._searchIdx = this._numHits - 1;
            return true;
        }
        return false;
    }

    _focusOnTextField() {
        this.shadowRoot.getElementById('searchfield').focus();
        this._searchIdx = FOCUS_SEARCH_FIELD;
    }

    _focusIsOnLastRecentItem() {
        const recentList = this.shadowRoot.getElementById('recentlist');
        return this.__prevRecentFocus === recentList.focusedItemIdx && this._numRecents() === recentList.focusedItemIdx + 1;
    }

    _focusIsOnFirstRecentItem() {
        const recentList = this.shadowRoot.getElementById('recentlist');
        return this.__prevRecentFocus === recentList.focusedItemIdx && recentList.focusedItemIdx === 0;
    }

    _searchKeyPress(ev) {
        const searchField = this.shadowRoot.getElementById('searchfield');
        const recentList = this.shadowRoot.getElementById('recentlist');

        let updateRecentListFocus = true;

        switch (ev.key) {
            case 'ArrowUp':
                if (this._searchIdx === FOCUS_SEARCH_FIELD) {
                    if (ev.shiftKey) {
                        // Keep this within the search field
                        return;
                    }

                    // Focus is on the textfield, move it to the search button
                    this._searchIdx = FOCUS_SEARCH_CLOSE_BUTTON;

                    // Make sure the focus leaves the text field and goes back to the Tree selector itself
                    searchField.blur();
                    this.focus();
                } else if (this._searchIdx === FOCUS_SEARCH_CLOSE_BUTTON) {
                    // Focus is on the close button, move it to the last hit (or to the textfield if there are no search hits)
                    if (recentList) {
                        // We are in "recent" mode, no search hits, move focus to the recent list
                        recentList.resetFocusLast(true);
                        this.__prevRecentFocus = this._numRecents() - 1;
                        updateRecentListFocus = false;
                        this._searchIdx = FOCUS_SEARCH_RECENT_LIST;
                    } else if (!this._focusOnLastSearchHit()) {
                        this._focusOnTextField();
                    }
                } else if (this._searchIdx === FOCUS_SEARCH_RECENT_LIST) {
                    if (this._focusIsOnFirstRecentItem()) {
                        // Focus on the top item, move the focus to the search field
                        this._focusOnTextField();
                    }
                } else if (this._searchIdx === 0) {
                    this._focusOnTextField();
                } else if (this._searchIdx > 0) {
                    this._searchIdx--;
                }
                ev.preventDefault();
                break;

            case 'ArrowDown':
                if (this._searchIdx === FOCUS_SEARCH_CLOSE_BUTTON) {
                    // Focus is on the close button, move it to the textfield
                    searchField.focus();
                    this._searchIdx = FOCUS_SEARCH_FIELD;
                } else if (this._searchIdx === FOCUS_SEARCH_FIELD) {
                    if (ev.shiftKey) {
                        // Keep this within the search field
                        return;
                    }

                    if (recentList) {
                        // Focus on the recent list
                        recentList.resetFocus(true);
                        this._searchIdx = FOCUS_SEARCH_RECENT_LIST;
                    } else {
                        // Focus on the hits (or the close button)
                        searchField.blur();
                        this.focus();

                        // Focus on the first hit (or on the close button if there are no hits)
                        this._searchIdx = this._numHits > 0 ? 0 : FOCUS_SEARCH_CLOSE_BUTTON;
                    }
                } else if (this._searchIdx === FOCUS_SEARCH_RECENT_LIST) {
                    if (this._focusIsOnLastRecentItem()) {
                        // Focus is on the *last* hit, move it to the close button
                        recentList.blur();
                        this.blur();
                        this.focus();
                        this._searchIdx = FOCUS_SEARCH_CLOSE_BUTTON;
                        this._focusedEl = this.shadowRoot.getElementById('closesearch');
                    }
                } else if (this._searchIdx + 1 < this._numHits) {
                    // Focus is on one of the hits, move to the next
                    this._searchIdx++;
                } else {
                    // Focus is on the *last* hit, move focus to the close button
                    this._searchIdx = FOCUS_SEARCH_CLOSE_BUTTON;
                }
                ev.preventDefault();
                break;

            case 'Home':
                if (this._searchIdx === FOCUS_SEARCH_FIELD) {
                    return;
                } else if (this._searchIdx === FOCUS_SEARCH_RECENT_LIST) {
                    recentList.resetFocus(true);
                } else if (this._searchIdx >= 0) {
                    this._searchIdx = 0;
                }
                ev.preventDefault();
                break;

            case 'End':
                if (this._searchIdx === FOCUS_SEARCH_FIELD) {
                    return;
                } else if (this._searchIdx === FOCUS_SEARCH_RECENT_LIST) {
                    recentList.resetFocusLast(true);
                    this.__prevRecentFocus = this._numRecents() - 1;
                    updateRecentListFocus = false;
                } else if (this._searchIdx >= 0) {
                    this._searchIdx = this._numHits - 1;
                }
                ev.preventDefault();
                break;

            case ' ':
            case 'Enter':
                if (this._searchIdx === FOCUS_SEARCH_FIELD) {
                    // No preventDefault when the focus is on the text field
                    return;
                } else if (this._searchIdx === FOCUS_SEARCH_CLOSE_BUTTON) {
                    // Close the search area
                    this._searchModeOpen = false;
                    this.focus();
                    this._focusedSection = AREA_OPEN_SEARCH;
                    this._setFocusedEl();
                } else if (this._searchIdx >= 0) {
                    const hasChildren = this._nodeHasChildren(this._focusedEl);
                    this._focusedEl.click();
                    this.performUpdate();
                    if (hasChildren) {
                        this._focusOnFirstNode();
                    } else {
                        this._focusOnLastHierarchyNode();
                    }
                }
                ev.preventDefault();
                break;

            case 'Escape':
                // Close the search area
                this._searchModeOpen = false;

                // If the focus was on the edit field, put it back to the host
                searchField.blur();
                if (recentList) {
                    recentList.blur();
                }
                if (this._searchIdx >= 0) {
                    this._focusedEl.blur();
                }

                this.blur();
                this.focus();
                this._focusedSection = AREA_OPEN_SEARCH;
                this._setFocusedEl();
                ev.preventDefault();
                break;
        }

        // Update the currently selected recent list item (unless we already updated it "manually" above)
        if (recentList && updateRecentListFocus) {
            // Remember this and use it the next time (since the focused index of the list is already updated when we get this event...)
            this.__prevRecentFocus = recentList.focusedItemIdx;
        }
    }

    _openSearchKeyPress(ev) {
        switch (ev.key) {
            case 'ArrowDown':
            case 'ArrowRight':
                // Focus should go to the first node in the AREA_HIERARCHY section
                if (!this._focusOnFirstNode()) {
                    if (!this._focusOnClearSelection()) {
                        // Focus on the first hierarchiclal node (if this fails, then just do nothing)
                        this._focusOnFirstHierarchyNode();
                    }
                }
                ev.preventDefault();
                break;
            case 'ArrowUp':
            case 'ArrowLeft':
                // Focus should go to the first node in the AREA_HIERARCHY section
                if (!this._focusOnLastHierarchyNode()) {
                    if (!this._focusOnClearSelection()) {
                        // Focus on the last node (if this fails, then just do nothing)
                        this._focusOnLastNode();
                    }
                }
                ev.preventDefault();
                break;
            case 'Enter':
            case ' ':
                this._focusedEl.click();
                ev.preventDefault();
                break;
        }
    }

};

customElements.define(PTCS.TreeSelector.is, PTCS.TreeSelector);
