import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {when} from 'lit/directives/when.js';
import {PTCS} from 'ptcs-library/library.js';
import './components/ptcs-pagination-input-number/ptcs-pagination-input-number.js';
import './components/ptcs-pagination-carousel/ptcs-pagination-carousel.js';
import 'ptcs-icon/ptcs-icon.js';
import 'ptcs-icons/cds-icons.js';
import 'ptcs-dropdown/ptcs-dropdown.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';
import {delegateToPrev} from 'ptcs-behavior-focus/ptcs-behavior-focus.js';

const THRESHOLD_WIDTH_FOR_MIN_SIZE = 432;
const navKeys = new Set(['Home', 'End', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', ' ']);

PTCS.Pagination = class extends PTCS.BehaviorTabindex(PTCS.BehaviorStyleable(L2Pw(LitElement))) {

    static get styles() {
        return css`
                :host(:not([show-direct-link]):not([show-page-break]):not([show-total-results])) [part="page-break-and-total-results-container"] {
                    display: none !important;
                }
                :host([layout=minimum]:not([show-page-break]):not([show-total-results])) [part="page-break-and-total-results-container"] {
                    display: none !important;
                }
                :host(:not([show-direct-link]):not([show-total-results]:not([show-page-break]))) [part="carousel"] {
                    padding-bottom: 0;
                }
                [part="direct-link"] {
                    display: flex;
                    align-items: center;
                    flex: 0 0;
                    order: 3;
                }

                [part="page-break-and-total-results-container"] {
                    display: flex;
                    flex-flow: row nowrap;
                    place-content: center;
                    align-items: center;
                    flex: 0 0;
                }

                [part="carousel"] {
                    flex: 0 0;
                }

                [part="string-per-page-label"],
                [part="page-results-dropdown"],
                [part="total-results-label"] {
                    min-width: max-content;
                }

                [part="total-results"] {
                    display: flex;
                    align-items: center;
                    flex-shrink: 0;
                }

                :host([layout=minimum]) {
                    display: inline-flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                }

                :host([layout=minimum]) [part="carousel"] {
                    order: 1;
                }

                :host([layout=minimum]) [part="page-break-and-total-results-container"] {
                    order: 2;
                }

                :host([layout=medium]) {
                    display: grid;
                    width: min-content;
                    grid-template-columns: 1fr;
                    grid-template-rows: auto;
                    grid-template-areas:
                      "carousel"
                      "row2";
                    align-items: center;
                    justify-content: center;
                }

                :host([layout=medium]) [part="carousel"] {
                    grid-area: carousel;
                }

                :host([layout=medium]) [part="page-break-and-total-results-container"] {
                    grid-area: row2;
                    display: inline-flex;
                    flex-wrap: nowrap;
                }

                :host([layout=maximum]) {
                    display: inline-flex;
                    justify-content: flex-start;
                }

                :host([layout=maximum]) [part="page-break-and-total-results-container"] {
                    order: 1;
                }

                :host([layout=maximum]) [part="carousel"] {
                    order: 2;
                }`;
    }

    render() {

        const _dropdown =  () => html`<ptcs-dropdown id="main-drop-down" part="page-results-dropdown"
                .selectedValue=${this._getPageSizeStringRepresentation(this.pageSize)} tabindex=${this._tabindex(this.tabindex)}
                @selected-value-changed=${this._handleMainDropDownChange} style="order: ${this._mainDropDownOrder}"></ptcs-dropdown>
                <ptcs-label part="string-per-page-label" id="string-per-page"
                .label=${this._stringPerPage} style="order: ${this._stringPerPageOrder}" disable-tooltip></ptcs-label>`;

        const _totalresults = () => html`<ptcs-label part="total-results-label" id="total-results-label" disable-tooltip
                .label=${this._stringResults}></ptcs-label>`;

        const _directLink = () => html`<div id="direct-link" part="direct-link">
                <ptcs-label part="string-jump-to-label" id="string-jump-to"
                    .label=${this.stringJumpToPage + ':'} disable-tooltip></ptcs-label>
                <ptcs-pagination-input-number id='input-number' part="input-number"
                    @value-approved=${this._handleJumpLink} .totalNumberOfPages=${this._totalNumberOfPages}
                   .errorMessage=${this.stringMax} tabindex=${this._tabindex(this.tabindex)}>
                </ptcs-pagination-input-number></div>`;

        return html`<ptcs-pagination-carousel id="carousel" part="carousel"
                .focusable=${this._focusable(this.tabindex)} .currentPage=${this.pageNumber}
                .minSize=${this._minSize} .totalNumberOfPages=${this._totalNumberOfPages}
                @change=${this._handleCarouselOrInputNumberChange}  @focus-on-button=${this._focusOnCarouselButton}>
            </ptcs-pagination-carousel>
            <div id="page-break-control-and-total-results-container" part="page-break-and-total-results-container"
                       start center>
                ${when(this.showTotalResults, _totalresults)}
                ${when(this.showPageBreak, _dropdown)}
                ${when(this.showDirectLink && this.layout === 'medium', _directLink)}
            </div>
            ${when(this.showDirectLink && this.layout !== 'medium', _directLink)}`;
    }

    static get properties() {
        return {
            // Private read-only _pageNumber
            _pageNumber: {
                type:     Number,
                readOnly: true,
                value:    1
            },

            // Two-way bindable public pageNumber
            pageNumber: {
                type:      Number,
                notify:    true,
                value:     1,
                attribute: 'page-number',
                observer:  'pageNumberChanged'
            },

            pageSize: {
                type:      Number,
                value:     1,
                notify:    true,
                attribute: 'page-size'
            },

            _pageBreaks: {
                type:     Array,
                computed: '_parseObjectToArray(firstBreak, secondBreak, thirdBreak, fourthBreak, fifthBreak)'
            },

            // PageBreaks
            firstBreak: {
                type:      Number,
                attribute: 'first-break'
            },

            secondBreak: {
                type:      Number,
                attribute: 'second-break'
            },

            thirdBreak: {
                type:      Number,
                attribute: 'third-break'
            },

            fourthBreak: {
                type:      Number,
                attribute: 'fourth-break'
            },

            fifthBreak: {
                type:      Number,
                attribute: 'fifth-break'
            },

            totalNumberOfElements: {
                type:      Number,
                attribute: 'total-number-of-elements',
            },

            showPageBreak: {
                type:      Boolean,
                reflect:   true,
                attribute: 'show-page-break',
            },

            resultsOptions: {
                type:      Number,
                attribute: 'results-options'
            },

            showDirectLink: {
                type:      Boolean,
                reflect:   true,
                attribute: 'show-direct-link'
            },

            showTotalResults: {
                type:      Boolean,
                reflect:   true,
                attribute: 'show-total-results'
            },

            stringPerPage: {
                type:      String,
                attribute: 'string-per-page'
            },

            // Layout configuration: minimum, medium, maximum
            layout: {
                type:    String,
                reflect: true
            },

            // The processed stringPerPage, without placeholder text
            _stringPerPage: {
                type:     String,
                computed: '_computeStringPerPage(stringPerPage)'
            },

            stringResults: {
                type:      String,
                attribute: 'string-results'
            },

            // The processed stringResults, without placeholder text
            _stringResults: {
                type:     String,
                computed: '_computeStringResults(totalNumberOfElements, stringResults)'
            },

            stringJumpToPage: {
                type:      String,
                attribute: 'string-jump-to-page'
            },

            stringMax: {
                type:      String,
                attribute: 'string-max'
            },

            _focusEl: {
                type: Element
            },

            // For IDE interaction
            resizing: {
                type: Boolean
            },

            // Maximum width in pixels (as a layout constraint)
            maximumWidth: {
                type:      Number,
                attribute: 'maximum-width'
            },

            // Externally computed minimum width of the component in minSize layout (used in M-B IDE)
            minimumWidth: {
                type: Number
            },

            // Externally computed minimum height of the component in minSize layout (used in M-B IDE)
            minimumHeight: {
                type: Number
            },

            // Toggle to switch ptcs-pagination-carousel to mini view
            _minSize: {
                type: Boolean
            },

            // CSS flexbox order for ptcs-label id="string-per-page", as determined by stringPerPage
            _stringPerPageOrder: {
                type: String
            },

            // CSS flexbox order for the ptcs-dropdown id="main-drop-down", as determined by stringPerPage
            _mainDropDownOrder: {
                type: String
            }
        };
    }
    static get observers() {
        return [
            /* eslint-disable max-len*/
            '_debouncedUpdateLayout(maximumWidth, totalNumberOfElements, showPageBreak, showTotalResults, showDirectLink, _minSize, _stringResults, _stringPerPage, stringJumpToPage)',
            '_observeTotalNumberOfElementsOrPageSize(pageSize, totalNumberOfElements, _pageBreaks, showPageBreak, resultsOptions)'
            /* eslint-enable max-len*/
        ];
    }
    ready() {
        super.ready();

        // Keyboard navigation
        this.shadowRoot.addEventListener('mousedown', this._mouseDown.bind(this));
        this.addEventListener('keydown', this._keyDown.bind(this));
        this.addEventListener('focus', this._focusEv.bind(this));
    }

    static get is() {
        return 'ptcs-pagination';
    }
    constructor() {
        super();
        this._totalNumberOfPages = 1;
        this.stringJumpToPage = 'Jump to page';
        this.stringPerPage =  '__ResultsDropdown__ per page';
        this.stringResults = '__TotalResults__ results';
        this.stringMax = 'Max';
        this.firstBreak = 10;
        this.secondBreak = 25;
        this.thirdBreak = 50;
        this.fourthBreak = 75;
        this.fifthBreak = 100;
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (changedProperties.has('stringPerPage')) {
            // The order in which the dropdown and "per page" string appears are configurable via placeholder in stringPerPage
            const dropdownPlaceholder = '__ResultsDropdown__';
            const perPageString = this.stringPerPage.replace(dropdownPlaceholder, '').trim();
            if (this.stringPerPage.indexOf(dropdownPlaceholder) > this.stringPerPage.indexOf(perPageString)) {
                // Dropdown should be after the per page string
                this._stringPerPageOrder = '2';
                this._mainDropDownOrder = '3';
            } else {
                // Dropdown should be before the per page string
                this._mainDropDownOrder = '2';
                this._stringPerPageOrder = '3';
            }
        }

        if (changedProperties.has('maximumWidth')) {
            this.style.minWidth = '';
        }
    }

    // eslint-disable-next-line max-len
    _debouncedUpdateLayout(maximumWidth, totalNumberOfElements, showPageBreak, showTotalResults, showDirectLink, _minSize, _stringResults, _stringPerPage, stringJumpToPage) {
        // Debounce layout computations
        clearTimeout(this._debounceTimeoutId);
        this._debounceTimeoutId = setTimeout(() => {
            this.__updateLayout();
        }, 50);
    }

    _litDependenciesAreUpdated() {
        const litSubComponents = [this.$.carousel];
        if (this.showTotalResults) {
            litSubComponents.push(this.$['total-results-label']);
        }
        if (this.showDirectLink) {
            litSubComponents.push(this.$['string-jump-to']);
            litSubComponents.push(this.$['input-number']);
        }
        return Promise.all([
            ...this.$.carousel.shadowRoot.querySelectorAll('ptcs-button[part="page-number-button"]'),
            ...litSubComponents
        ].map(el => el.updateComplete));
    }

    async __updateLayout() {
        // Ensure the lit-based subcomponents have updated before resolving scrollWidth

        // Try maximum layout first
        this.layout = 'maximum';
        await this.updateComplete;
        await this._litDependenciesAreUpdated();

        const carousel = this.$.carousel;
        const pageBreakControl = this.$['page-break-control-and-total-results-container'];
        const directLinkWidth = this.showDirectLink ? this.$['direct-link'].scrollWidth : 0;

        // Dynamic toggle: During resizing the carousel can switch between its smaller and expanded view
        this._minSize = (this.resizing ? this.maximumWidth : Math.max(carousel.scrollWidth, this.maximumWidth)) < THRESHOLD_WIDTH_FOR_MIN_SIZE;

        await this.updateComplete;
        await this._litDependenciesAreUpdated();

        const componentMaxWidth = pageBreakControl.scrollWidth + carousel.scrollWidth + directLinkWidth;
        if (this.maximumWidth && this.maximumWidth < componentMaxWidth) {
            // Component in maximum layout is wider than maximumWidth constraint: Try medium layout
            this.layout = 'medium';
            await this.updateComplete;
            await this._litDependenciesAreUpdated();

            if (carousel.scrollWidth > this.maximumWidth || pageBreakControl.scrollWidth > this.maximumWidth) {
                // Medium layout is still too wide, fallback to minimum layout
                this.layout = 'minimum';
            }
        }

        // Legacy behavior: Ensure that ptcs-pagination at least has min-width to contain the ptcs-carousel
        setTimeout(() => {
            this.style.minWidth = carousel.scrollWidth + 'px';
        }, 300);

    }

    _computeStringPerPage(stringPerPage) {
        const dropdownPlaceholder = '__ResultsDropdown__';
        return stringPerPage.replace(dropdownPlaceholder, '').trim();
    }

    _computeStringResults(totalNumberOfElements, stringResults) {
        const totalString = String(totalNumberOfElements);
        const placeholder = '__TotalResults__';
        return stringResults.includes(placeholder)
            ? stringResults.replace(placeholder, totalString)
            : totalString + ' ' + stringResults;
    }

    pageNumberChanged(num) {
        const pageno = Number(num);
        if ((pageno < 1 || pageno > this._totalNumberOfPages || isNaN(pageno))) {
            // provided page number is out of bounds, reset pageNumber to current value
            this.pageNumber = this._pageNumber || 1;
        }
    }

    _handleJumpLink(event) {
        this._handleCarouselOrInputNumberChange(event);
    }

    _handleCarouselOrInputNumberChange(event) {
        const pageNo = event.detail.pageNo;
        if (pageNo && pageNo !== this._pageNumber) {
            // Set the read-only _pageNumber
            this._set_pageNumber(pageNo);
            // Update the public pageNumber
            this.pageNumber = pageNo;
        }
        // Reset the navMode (to stop a button click or jump link assignment from being misinterpreted as an arrow navigation)
        this.$.carousel.navMode = undefined;
        event.stopPropagation();
    }

    _observeTotalNumberOfElementsOrPageSize(pageSize, totalNumberOfElements, _pageBreaks, showPageBreak, resultsOptions) {
        if (pageSize > 0 && totalNumberOfElements > 0) {
            this._totalNumberOfPages = Math.ceil(totalNumberOfElements / pageSize);
        } else {
            this._totalNumberOfPages = 1;
        }
        if (this.showDirectLink && this._totalNumberOfPages < this.$['input-number'].value) {
            this.$['input-number'].reset();
        }
        this.$.carousel.totalNumberOfPages = this._totalNumberOfPages;

        // Set items in dropdown
        if (showPageBreak) {
            this.$['main-drop-down'].items = _pageBreaks.slice(0, resultsOptions);
            this.$['main-drop-down'].selectedValue = this._getPageSizeStringRepresentation(pageSize);
        }

        this.__updateLayout();
    }

    // main-drop-down index change might happen when:
    //   1. User has selected an item
    //   2. pageBreak param has been updated

    _handleMainDropDownChange() {
        const mainDropDownSelectedItem = this.$['main-drop-down'].selectedValue;
        this.pageSize = Number(mainDropDownSelectedItem);
    }

    _getPageSizeStringRepresentation(pageSize) {
        return pageSize.toString();
    }

    _parseObjectToArray(...pageBreaks) {
        return [...new Set(Object.values(pageBreaks).filter(elem => elem))];
    }

    // Keyboard navigation
    _tabindex(tabindex) {
        return tabindex && typeof tabindex === 'string' && '0';
    }

    _focusable(tabindex) {
        return tabindex && typeof tabindex === 'string' && '-1';
    }

    get focusableElements() {
        const resultOrder = [];

        const order = id => getComputedStyle(this.$[id]).order;

        resultOrder.push([order('carousel'), this.$.carousel.shadowRoot.querySelectorAll('ptcs-button')]);

        return resultOrder.sort((a, b) => a[0] - b[0]).reduce((acc, v) => {
            acc.push(...v[1]);
            return acc;
        }, []);
    }

    _mouseDown(ev) {
        const tagName = ev.target.tagName;
        if (tagName === 'PTCS-DROPDOWN') {
            this._focusEl = ev.target;
        } else if (tagName === 'PTCS-PAGINATION-INPUT-NUMBER') {
            this._focusEl = ev.target.shadowRoot.querySelector('ptcs-textfield');
        }
    }

    _focusOnCarouselButton(ev) {
        this._focusEl = ev.detail.button;
    }

    _focusEv() {
        // Ignore if we don't support focusing or already have focus on a sub element
        if (!this.tabindex || this.shadowRoot.activeElement) {
            return;
        }
        if (!this._focusEl || !this._focusEl.clientWidth) {
            const fe = this.focusableElements;
            this._focusEl = fe.find(el => el.hasAttribute('selected')) || fe[0];
            console.assert(this._focusEl);
        }
        this._focusEl.focus();
    }

    get focusElement() {
        if (!this.tabindex) {
            return null; // Not focusable
        }
        let hit = this.shadowRoot.activeElement;
        let el = hit || document.activeElement;
        while (el && el.shadowRoot && el.shadowRoot.activeElement) {
            hit = hit || el === this || this.contains(el);
            el = el.shadowRoot.activeElement;
        }
        // Focused element must be  in slotted content or in shadow dom
        return (hit || this.shadowRoot.activeElement) && el;
    }

    _keyDown(ev) {
        // This element must be focusable, or the key event is only for the textfield
        if (ev.defaultPrevented || !this.tabindex) {
            return;
        }
        const focusable = this.focusableElements;

        if (ev.shiftKey && ev.key === 'Tab') {
            // Move with ShiftTab key between the directLink and the PageBreak
            if (this.showDirectLink && this.showPageBreak && this.shadowRoot.activeElement === this.shadowRoot.getElementById('input-number')) {
                return;
            }
            // Fisrt focus not starting by focusable element (carousel)
            if (!focusable.includes(this._focusEl)) {
                const fe = this.focusableElements;
                this._focusEl = fe.find(el => el.hasAttribute('selected')) || fe[0];
                this._focusEl.focus();
            } else if (this.focusElement !== this._focusEl) {
                this._focusEl.focus();
            // Prevent backwards navigation from stopping on this (ptcs-pagination) element
            } else if (!delegateToPrev(this)) {
                this.blur();
            }

            ev.preventDefault();
            return;
        }

        // There must be a focused sub element - and the key must be relevant for keyboard navigation
        if (!this._focusEl || !navKeys.has(ev.key)) {
            return;
        }

        // Special rules for textfield
        if (this.focusElement.tagName === 'INPUT') {
            const focus = this.focusElement.getRootNode().host;
            if (focus.tagName === 'PTCS-TEXTFIELD') {
                switch (ev.key) {
                    case 'ArrowLeft':
                    case 'Home':
                        if (this.focus.selectionEnd > 0) {
                            return; // Ignore unless cursor is at start of text
                        }
                        break;
                    case 'End':
                    case 'ArrowRight':
                        if (focus.selectionStart < focus.text.length) {
                            return; // Ignore unless cursor is at end of text
                        }
                        break;
                }
            }
        }

        let focusEl = this._focusEl;
        const index = focusable.indexOf(focusEl);
        let idx;

        if (focusable.includes(this.focusElement)) {
            switch (ev.key) {
                case 'Home':
                    focusEl = focusable[0];
                    break;
                case 'End':
                    focusEl = focusable[focusable.length - 1];
                    break;
                case 'ArrowLeft':
                case 'ArrowUp':
                    idx = index === 0 ? focusable.length - 1 : index - 1;
                    focusEl = focusable[idx];
                    break;
                case 'ArrowRight':
                case 'ArrowDown':
                    idx = index === focusable.length - 1 ? 0 : index + 1;
                    focusEl = focusable[idx];
                    break;
                case ' ':
                    this._focusEl.click();
                    ev.preventDefault();
                    return;
            }

            if (focusEl && focusEl !== this._focusEl) {
                this._focusEl = focusEl;
                focusEl.focus();
                ev.preventDefault();
            }
        }
    }
};

customElements.define(PTCS.Pagination.is, PTCS.Pagination);
