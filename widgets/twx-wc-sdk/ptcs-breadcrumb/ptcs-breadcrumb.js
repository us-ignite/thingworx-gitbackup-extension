import {LitElement, html, css} from 'lit';
import {map} from 'lit/directives/map.js';
import {when} from 'lit/directives/when.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-icons/cds-icons.js';
import 'ptcs-link/ptcs-link.js';
import 'ptcs-dropdown/ptcs-dropdown.js';
import 'ptcs-div/ptcs-div.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';

const DEFAULT_MAX_BREADCRUMB_WIDTH_PX = 2000;
const DEFAULT_MIN_BREADCRUMB_WIDTH_PX = 34;
const OVERFLOW_THRESHOLD_DEFAULT = 4;
PTCS.Breadcrumb = class extends PTCS.BehaviorTabindex(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement)))) {
    static get styles() {
        return css`
        :host {
          display: inline-block;
          box-sizing: border-box;
          overflow: hidden;
        }

        #root {
          position: relative;
          box-sizing: content-box;
        }

        #list {
          flex: 1 1 auto;
          overflow: hidden;
        }

       [part="separator"]:last-of-type {
          display: none;
        }

        [part="separator"] {
          flex: 0 0 auto;
          min-width: 0px;
          text-align: center;
        }

        ptcs-link::part(label) {
          text-overflow: ellipsis;
        }

        ptcs-link {
          flex: 0 0 auto;
          position: relative;
          white-space: nowrap;
        }

        [hidden] {
          display: none;
        }

        #dropdown:not([disabled]) {
          cursor: pointer;
        }

        [part=dropdown] {
          border: none;
        }

        ptcs-dropdown::part(select-box) {
          min-height: 0;
          border: none;
        }

        ptcs-dropdown::part(item-value) {
          display: none;
        }

        ptcs-dropdown::part(icon) {
          min-height: 0;
          pointer-events: auto;
        }

        [part=root],
        [part=body] {
            display: flex;
            flex-flow: row nowrap;
            place-content: center flex-start;
            align-items: center;
        `;
    }

    render() {
        return html`<ptcs-div id="root" part="root"><ptcs-div part="body" id="list">
           ${map(this.items, (item, i) => html`
             ${when(i === 0, () => html`<ptcs-link part="link" @click=${this._clickOnLink} index=${i}
                       .href=${this._getUrlFromObject(item, this.selectorUrl)} .disabled=${this._isCurrentLevel(i, this.disabled)}
                       .textMaximumWidth=${this._linkTruncation(this.linkTruncation, this.linkTruncationLength, this._maxLenCrumb, this._overflow)}
                       single-line variant="secondary" label=${this._getLabelFromObject(item, this.selector)}
                       tabindex=${this._delegatedFocus} exportparts=${this._exportlink}></ptcs-link>
                     <span part="separator">/</span>`)}
                   ${when(this._overflow && i === 0, () => html`<ptcs-dropdown part="dropdown" id="dropdown"
                       .items=${this._overflowItems} .stateSelector=${this._itemStateSelector}  @selected-changed=${this._dropdownSelectedChanged}
                       .disabled=${this.disabled} value-hide display-mode="small" tabindex=${this._delegatedFocus} @click=${this._clickDropdownButton}
                       .selector=${this._selector(this.selector)} exportparts=${this._exportdropdown}
                       .icon=${'cds:icon_more_horizontal_mini'}></ptcs-dropdown>
                       <span part="separator">/</span>`)}
                   ${when(!this._overflow && i > 0, () => html`<ptcs-link part="link" @click=${this._clickOnLink} index=${i}
                       .href=${this._getUrlFromObject(item, this.selectorUrl)} .disabled=${this._isCurrentLevel(i, this.disabled)}
                       .textMaximumWidth=${this._linkTruncation(this.linkTruncation, this.linkTruncationLength, this._maxLenCrumb, this._overflow)}
                       single-line variant="secondary" label=${this._getLabelFromObject(item, this.selector)}
                       tabindex=${this._delegatedFocus} ?hidden=${this._hideCurrentLevel(i)}
                       exportparts=${this._exportlink}></ptcs-link>
                       <span part="separator">/</span>`)}
                  ${when(this._overflow && (i === this._itemsLength - 2 || i === this._itemsLength - 1), () => html`<ptcs-link
                       part="link" @click=${this._clickOnLink} index=${i} .href=${this._getUrlFromObject(item, this.selectorUrl)}
                       .disabled=${this._isCurrentLevel(i, this.disabled)} ?hidden=${this._hideCurrentLevel(i)}
                       .textMaximumWidth=${this._linkTruncation(this.linkTruncation, this.linkTruncationLength, this._maxLenCrumb, this._overflow)}
                       single-line variant="secondary" label=${this._getLabelFromObject(item, this.selector)} tabindex=${this._delegatedFocus}
                       exportparts=${this._exportlink}></ptcs-link>
                      <span part="separator">/</span>`)}
                  `)}
        </ptcs-div>
        <span id="filler">&nbsp;</span>
      </ptcs-div>`;
    }

    static get is() {
        return 'ptcs-breadcrumb';
    }

    static get properties() {
        return {
            // An array of strings, where each string is a step in the breadcrumb path (used as input only)
            items: {
                type:     Array,
                value:    () => [],
                observer: '_itemsChanged'
            },

            // The selector is the key that specifies the entry label when _items_ is a list of objects,
            selector: {
                type:  String,
                value: ''
            },

            // If items is an array of objects, the selecturUrl should hold the key to the URL to use for each entry
            selectorUrl: {
                type:      String,
                attribute: 'selector-url',
                value:     ''
            },

            // Deprecated. Should the "current" breadcrumb level be shown or not?
            showCurrentLevel: {
                type:      Boolean,
                attribute: 'show-current-level',
                reflect:   true,
                observer:  '_updateHideCurrentLevel'
            },

            // Hides the current level indicator
            hideCurrentLevel: {
                type:      Boolean,
                attribute: 'hide-current-level',
                reflect:   true,
                value:     false,
                observer:  '_updateShowCurrentLevel'
            },

            // Assigned threshold: Max number of breadcrumbs before switching to layout of dropdown list with overflowing links
            overflowThreshold: {
                type:      Number,
                attribute: 'overflow-threshold',
                value:     OVERFLOW_THRESHOLD_DEFAULT,
                observer:  '_updateLayoutOnResize'
            },

            // Truncate breadcrumbs if rightmost item (link or separator) right edge is not visible?
            visibleBoundsTruncation: {
                type: Boolean
            },

            // Truncate the label of long links?
            linkTruncation: {
                type:      Boolean,
                attribute: 'link-truncation',
                value:     false,
                observer:  '_refresh'
            },

            // If link truncation is active, what should be the max width?
            linkTruncationLength: {
                type:      Number,
                attribute: 'link-truncation-length',
                value:     120
            },

            // Index of last clicked link
            lastClickedIndex: {
                type:      Number,
                attribute: 'last-clicked-index',
                value:     -1
            },

            disabled: {
                type:  Boolean,
                value: false
            },

            // //////////////////////////////////////////////////////////// //
            //  P r i v a t e
            // //////////////////////////////////////////////////////////// //

            // Length of link items array
            _itemsLength: {
                type: Number
            },

            // Overflow raised when the number of links exceed overflowThreshold value, i.e. dropdown appears
            _overflow: {
                type:    Boolean,
                reflect: true
            },

            // List items for the dropdown in _overflow condition
            _overflowItems: {
                type: Array
            },

            // Set when breadcrumbs don't fit the list container (to adjust their widths for truncation)
            _breadcrumbOverflow: {
                type: Boolean
            },

            // Max width of the link breadcrumb, items wider than this should be auto-truncated
            _maxLenCrumb: {
                type:  Number,
                value: DEFAULT_MAX_BREADCRUMB_WIDTH_PX
            },

            // Exported parts
            _exportlink: {
                type:     String,
                readOnly: true,
                value:    PTCS.exportparts('link-', PTCS.Link)
            },

            _exportdropdown: {
                type:     String,
                readOnly: true,
                // It only makes sense to export these parts, since the breadcrumb hides the other parts
                value:    'select-box : dropdown-select-box, icon : dropdown-icon'
            },

            _delegatedFocus: String,

            _resizeObserver: ResizeObserver
        };
    }

    constructor(...arg) {
        super(arg);

        // Resize observer to adjust the breadcrumb to available width
        this._resizeObserver = new ResizeObserver(() => {
            this._updateLayoutOnResize();
        });

        // For keyboard navigation / managing focus
        this.addEventListener('keydown', ev => this._keyDown(ev));
        window.addEventListener('resize', () => this._updateLayoutOnResize());
    }

    connectedCallback() {
        super.connectedCallback();
        this._resizeObserver.observe(this);
    }

    disconnectedCallback() {
        this._resizeObserver.unobserve(this);
        super.disconnectedCallback();
    }

    _updateLayoutOnResize() {
        // Debounce layout computations during resizing
        if (!this.__updateLayout) {
            this.__updateLayout = true;
            requestAnimationFrame(() => {
                this._refresh();
                this.__updateLayout = false;
            }, 100);
        }
    }

    _selector(selector) {
        return selector || 'name';
    }

    _getLabelFromObject(item, selector) {
        if (!item) {
            return '';
        }

        if (typeof item === 'string') {
            return item;
        }

        if (!selector) {
            return item['name'] || '';
        }

        if (typeof selector === 'string') {
            return item[selector] || '';
        }

        if (selector && selector.constructor && selector.call && selector.apply) {
            return selector(item);
        }

        console.error('Invalid selector');

        // Fallback
        return item || '';
    }

    _getUrlFromObject(item, selector) {
        if (!item) {
            return '';
        }

        if (!selector) {
            return '';
        }

        if (typeof selector === 'string') {
            return item[selector] || '';
        }

        if (selector && selector.constructor && selector.call && selector.apply) {
            return selector(item);
        }

        console.error('Invalid url selector');

        // Fallback
        return '';
    }

    _linkTruncation(linkTruncation, linkTruncationLength, _maxLenCrumb, _overflow) {
        if (linkTruncation && !isNaN(linkTruncationLength) && !isNaN(_maxLenCrumb)) {
            return '' + Math.min(linkTruncationLength, _maxLenCrumb);
        }
        if (_overflow && !isNaN(_maxLenCrumb)) {
            return '' + _maxLenCrumb;
        }
        if (!isNaN(_maxLenCrumb)) {
            // Default to the "max" possible value
            return _maxLenCrumb === DEFAULT_MAX_BREADCRUMB_WIDTH_PX ? '' : '' + _maxLenCrumb;
        }
        return '';
    }

    _hideCurrentLevel(index) {
        return index === this._itemsLength - 1 ? !this.showCurrentLevel : false;
    }

    // Current (or disabled) level is displayed as disabled
    _isCurrentLevel(index, disabled) {
        return disabled || index === (this._itemsLength - 1);
    }

    async _breadcrumbFitContainerCheck() {
        // containerEdge is rightmost boundary for the link
        const containerEdge = this.$.filler.getBoundingClientRect().left;
        const list = this.$.list;
        const links = list.querySelectorAll('ptcs-link');

        // TW-116277: Sometimes (at certain zoom levels) there might be rounding errors here that causes the breadcrumb items to be
        // truncated even though the component width is "unrestricted". Allow a minimal "slack" (0.25px) before we consider the item
        // to be overflowing...
        const isOverflowing = (limit, value) => value > (limit + 0.25);

        const lastSeparator = list.querySelector('span:nth-last-of-type(2)');
        for (let i = 0; i < links.length; i++) {
            const link = links[i];
            link.performUpdate();
            link.$.label.performUpdate();
            const lastItem = this.showCurrentLevel ? link : lastSeparator;
            const lastItemBCRect = lastItem.getBoundingClientRect();
            const lastItemRight = lastItemBCRect.right;
            if (this.visibleBoundsTruncation) {
                this._breadcrumbOverflow = isOverflowing(containerEdge, lastItemRight) ||
                    document.elementFromPoint(lastItemRight, lastItemBCRect.y) === null;
            } else {
                this._breadcrumbOverflow = isOverflowing(containerEdge, lastItemRight);
            }
            if (this._breadcrumbOverflow) {
                break;
            }
        }
        if (!this._breadcrumbOverflow && !this._overflow) {
            // Store the value at which the current breadcrumbs fit without dropdown and without breadcrumb overflow,
            // if forced to use the overflow dropdown menu while resizing.
            this._noOverflowLimit = containerEdge;
        }
        this.performUpdate();
    }

    _truncateBreadcrumbs() {
        const list = this.$.list;
        const links = list.querySelectorAll('ptcs-link');
        const len = links.length;
        if (len < 2) {
            return;
        }

        this._breadcrumbFitContainerCheck();

        if (!this._breadcrumbOverflow) {
            // Breadcrumbs fit the container. Reset an earlier dynamically set threshold constraint (if any) when the container width is
            // sufficient. This is to avoid an oscillation between _updateLayoutOnResize and _refresh when restoring the original
            // overflowThreshold.
            if (this.overflowThreshold < this._currentOverflowThreshold && (this.$.list.clientWidth > this._noOverflowLimit)) {
                this.overflowThreshold = this._currentOverflowThreshold;
            }
            return;
        }

        /* Figma spec 4.1 Overflow
         *  If the breadcrumb width is longer than the parent container, truncate the text links to an EVEN width
         *  (single line label truncation ellipsis with tooltip)
         *
         * Breadcrumb overflow: Figma 5.2: Set a dynamic max length, with a minimum limit of 34px
         */
        this._maxLenCrumb = list.clientWidth;
        // Truncate the links by reducing _maxLenCrumb until we no longer have an overflow or reach the minimum allowed link width.
        while (this._breadcrumbOverflow && this._maxLenCrumb > DEFAULT_MIN_BREADCRUMB_WIDTH_PX) {
            this._maxLenCrumbTmp = this._maxLenCrumb - 4;
            this._maxLenCrumb = Math.max(DEFAULT_MIN_BREADCRUMB_WIDTH_PX, this._maxLenCrumbTmp % 2 ? this._maxLenCrumbTmp - 1 : this._maxLenCrumbTmp);
            this._breadcrumbFitContainerCheck();
        }

        if (this._breadcrumbOverflow && this.overflowThreshold !== OVERFLOW_THRESHOLD_DEFAULT) {
            /* If the breadcrumb still overflows after applying dynamic link truncation, use the overflow dropdown menu
             * (forced use, regardless of current overflowThreshold value = MaxLinkNumber)
             */
            this._currentOverflowThreshold = this.overflowThreshold;
            this.overflowThreshold = OVERFLOW_THRESHOLD_DEFAULT;
        }
    }

    _breadcrumbThresholdLimitCheck() {
        this._itemsLength = this.items ? this.items.length : 0;
        this._overflow = this._itemsLength > this.overflowThreshold;
        this._updateDropdownList();
    }

    _updateDropdownList() {
        const len = this.items ? this.items.length : 0;
        if (this.items) {
            const _overflowItems = [...this.items];
            if (len > this.overflowThreshold) {
                for (let i = 0; i < len; i++) {
                    // Hide first item and penultimate item as they are always visible.
                    // Also hide the last item, it is shown when showCurrentLevel is set
                    const show = !((i === 0) ||
                        (i === (len - 2)) ||
                        (i === (len - 1))
                    );
                    if (typeof _overflowItems[i] === 'string') {
                        _overflowItems[i] = {name: this._getLabelFromObject(this.items[i], this.selector), visible: show};
                    } else {
                        _overflowItems[i].visible = show;
                    }
                }
                this._overflowItems = _overflowItems;
            }
        }
    }

    _clickDropdownButton() {
        // Get the dropdown button DOMRect to custom position the dropdown list flush with the button's left edge and slightly below it
        const buttonClientRect = JSON.parse(JSON.stringify(this.$.dropdown.shadowRoot.querySelector('#icon').getBoundingClientRect()));
        buttonClientRect.top += 8; // Shift the dropdown just below its hit area per Figma spec
        this.$.dropdown.customListPosRect = buttonClientRect;
    }

    _clickOnLink(ev) {
        if (this.disabled || (ev.target && ev.target.disabled)) { // ev.target is the actual ptcs-link
            return;
        }

        if (ev.ctrlKey) {
            return;
        }

        const index = +ev.target.getAttribute('index');

        if (index >= 0 && index < this.items.length && index !== null) {
            this.lastClickedIndex = index;
            setTimeout(() => {
                this.items.splice(this.lastClickedIndex + 1);
                this.dispatchEvent(new CustomEvent('ptcs-breadcrumb', {bubbles: true, composed: true, detail: {index, item: this.items[index]}}));
                this.dispatchEvent(new CustomEvent('items-changed', {bubbles: true, composed: true, detail: this.items}));
                this._refresh();
            });
        } else {
            this.lastClickedIndex = -1;
        }
    }

    async getUpdateComplete() {
        await super.getUpdateComplete();
        await new Promise(requestAnimationFrame);
        await Promise.all([...this.$.list.querySelectorAll('ptcs-link')].map(el => el.updateComplete));
        return true;
    }

    // Selected changed in the overflow menu of the dropdown
    async _dropdownSelectedChanged(ev) {
        if (this.disabled) {
            return;
        }

        const index = ev.detail.value;
        if (index >= 0 && index < this.items.length && index !== null) {
            this.lastClickedIndex = index;

            this.items.splice(this.lastClickedIndex + 1);
            this.dispatchEvent(new CustomEvent('items-changed', {bubbles: true, composed: true, detail: this.items}));
            this.dispatchEvent(new CustomEvent('ptcs-breadcrumb', {bubbles: true, composed: true, detail: {index, item: this.items[index]}}));

            this._breadcrumbThresholdLimitCheck();

            this.performUpdate();
            await this.updateComplete;

            // See if we have a URL attached and, if so, navigate to the same href...
            const link = this.$.list.querySelector('ptcs-link:last-of-type');
            // If the link has an href, open it the same way as if the link itself would have been clicked
            if (link.href && PTCS.validateURL(link.href)) {
                PTCS.openUrl('open', link.href, link.target ? link.target : '_self');
            } else {
                // TW-109300 After dropdown navigation, Shift-TAB should put focus on the selected link
                link.focus();
                link.blur();
            }
        } else {
            this.lastClickedIndex = -1;
        }
    }

    _refresh() {
        this._maxLenCrumb = DEFAULT_MAX_BREADCRUMB_WIDTH_PX;
        this._breadcrumbThresholdLimitCheck();
        this._breadcrumbFitContainerCheck();
        this._truncateBreadcrumbs();
    }

    _itemsChanged(items) {
        this._itemsLength = items ? items.length : 0;
        if (items) {
            this._refresh();
        }
    }

    _getItemEl(index) {
        return this.$.list.querySelector(`ptcs-link:nth-of-type(${index + 1})`);
    }

    _keyDown(ev) {
        if (this.disabled || ev.defaultPrevented || !this.tabindex || !this.shadowRoot.activeElement) {
            return;
        }
        if (ev.key === 'Enter' || ev.key === ' ') {
            ev.preventDefault();
            this.shadowRoot.activeElement.click();
        }
    }

    _itemStateSelector(item) {
        if (!item) {
            return undefined;
        }

        if (typeof item.visible !== 'undefined' && item.visible === false) {
            return 'hidden';
        } else if (item.disabled) {
            return 'disabled';
        }

        return undefined;
    }

    _updateHideCurrentLevel(v) {
        this.hideCurrentLevel = !v;
        this._refresh();
    }

    _updateShowCurrentLevel(v) {
        this.showCurrentLevel = !v;
        this._refresh();
    }
};

customElements.define(PTCS.Breadcrumb.is, PTCS.Breadcrumb);
