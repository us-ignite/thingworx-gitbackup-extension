import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {map} from 'lit/directives/map.js';
import {when} from 'lit/directives/when.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-icons/cds-icons.js';

const HORIZONTAL_ELLIPSIS = '\u2026';
const HUGE_NUMBER_OF_PAGES = 10000;

PTCS.Carousel = class extends PTCS.BehaviorStyleable(L2Pw(LitElement)) {

    static get styles() {
        return css`
            :host {
                display: inline-flex;
                align-items: center;
                justify-content: center;
                outline: none;
            }`;
    }

    render() {
        /* eslint-disable indent */
        return html`<ptcs-button part="left-arrow" id="left-arrow" @click=${this.__handleClickOnArrow} variant="small"
                        icon="cds:icon_chevron_left_mini" .mode=${'icon'}
                        tabindex=${this.focusable}></ptcs-button>
                ${map(this._carouselButtons, (item) => html`
                   ${when(item === HORIZONTAL_ELLIPSIS,
                       () => html`<ptcs-label id="three-dots" part="three-dots" label=${item}></ptcs-label>`,
                       () => html`<ptcs-button id="page-number-button" part="page-number-button" label=${item}
                                      variant="transparent" tabindex=${this.focusable}
                                      @click=${this._handleClickOnNumber} ?selected=${this._isSelected(item)}>`)}
               `)}
            <ptcs-button part="right-arrow" id="right-arrow" @click=${this.__handleClickOnArrow} variant="small"
                icon="cds:icon_chevron_right_mini" .mode=${'icon'}
                tabindex=${this.focusable}></ptcs-button>`;
        /* eslint-enable indent */
    }

    static get is() {
        return 'ptcs-pagination-carousel';
    }

    static get properties() {
        return {

            currentPage: {
                type:       Number,
                attribute:  'current-page',
                noAccessor: true
            },

            _currentPage: {
                type:     Number,
                observer: '_observeCurrentPageChange'
            },

            minSize: {
                type:      Boolean,
                attribute: 'min-size'
            },

            totalNumberOfPages: {
                type:      Number,
                value:     1,
                observer:  '_observeTotalNumberOfPagesChange',
                attribute: 'total-number-of-pages'
            },

            focusable: {
                type: String
            },

            // Computed array representation of the carousel data to display
            _carouselButtons: {
                type:     Array,
                computed: '_computeCarouselButtons(totalNumberOfPages, _currentPage, minSize)'
            },

            // Track the navigation mode (assigned a value on arrow navigation, undefined on direct (jump) assignment / button click)
            navMode: {
                type: String
            },

            // Clicked on number button (vs. arrow button)
            _clickedOnNumber: {
                type: Boolean
            },

            // Counter for consecutive clicks on arrow buttons (just for a visual oscillation effect)
            _arrowClicks: {
                type: Number
            }
        };
    }

    constructor() {
        super();
        this._constructorInitialization = true;
        this._currentPage = 1;
        this._true = true;
        this._arrowClicks = 0;
    }

    ready() {
        super.ready();
        this._constructorInitialization = false;
    }

    updated(changedProperties) {
        super.updated(changedProperties);
        this.$['left-arrow'].disabled = this.totalNumberOfPages < 1 || this._currentPage === 1;
        this.$['right-arrow'].disabled = this.totalNumberOfPages < 1 || this._currentPage === this.totalNumberOfPages;
        if (this._clickedOnNumber) {
            // Re-rendered carousel may have moved a selected numeric button to a new position: Set focus on it
            this._clickedOnNumber = false;
            const selectedButton = this.shadowRoot.querySelector('ptcs-button[part="page-number-button"][selected]');
            selectedButton.focus();
            this.notifyFocusButton(selectedButton);
        }
    }

    set currentPage(newPageNo) {
        newPageNo = Number(newPageNo);
        if (this.totalNumberOfPages >= newPageNo && newPageNo > 0) {
            this._previousPage = this._currentPage;
            this._currentPage = newPageNo;
        }
    }

    get currentPage() {
        return this._currentPage;
    }

    _isSelected(item) {
        return Number(item) === this._currentPage;
    }

    notifyFocusButton(button) {
        if (this.focusable) {
            this.dispatchEvent(new CustomEvent('focus-on-button', {composed: true, detail: {button}}));
        }
    }

    // Minimum carousel size layout
    _minSizeLayout(total, pgno) {
        // In the minimum layout, the range start or end may not be shown.
        // This layout has (legacy) idiosyncrasies: When reaching the start or end of the range with
        // arrow buttons, only the first (or last) 4 buttons of the range are displayed, until the
        // current page is the first (or last) one, at which point the other range endpoint and a '...'
        // label appears, if there are more than 4 pages. The arrow navigation outcome is affected
        // by which button is currently selected when the arrow button is used.
        //
        // Example output, navigating 500 Total Results
        // -------------------------------
        // Using right arrow button, starting from left endpoint 1:    6  7  ... 500
        // Jump link, entering page number 6:                          6  7  ... 500
        // Using left arrow button, starting from right endpoint 500:  1 ... 492 493

        // When the current page is the range start or end, the other range endpoint is shown:
        // Current page is 1:                                                1  2  ... 500
        // Current page is 500:                                              1 ... 499 500
        const a = [];
        if (total <= 4) {
            // No ellipsis
            // eslint-disable-next-line curly
            for (let i = 1; a.length < total; i++) a.push(i);
        } else {
            const numericButtons = this.shadowRoot.querySelectorAll('ptcs-button[part="page-number-button"]');
            const firstButtonIsSelected = numericButtons[0] && numericButtons[0].hasAttribute('selected');
            if (pgno === total) {
                a.push(1, HORIZONTAL_ELLIPSIS, total - 1, total);
            } else if (pgno === 1) {
                a.push(1, 2, HORIZONTAL_ELLIPSIS, total);
            } else if (this.navMode === 'right-arrow' || !this.navMode || (firstButtonIsSelected && pgno > 4)) {
                // eslint-disable-next-line curly
                for (let i = Math.min(total - 3, pgno); a.length < 3; i++) a.push(i);
                if (a[2] < total - 1) {
                    a[2] = HORIZONTAL_ELLIPSIS;
                }
                a.push(total);
            } else if (pgno < 5) {
                // eslint-disable-next-line curly
                for (let i = 1; a.length < 4; i++) a.push(i);
            } else {
                a.push(1, HORIZONTAL_ELLIPSIS, pgno - 1, pgno);
            }
        }

        return a;
    }

    // Expanded carousel size layout
    _expandedSizeLayout(total, pgno) {
        // In this layout the range start / end are always visible and there can be up to two '...' labels.
        // When Total Results <= 7 no '...' label is needed:              1  2   3   4   5   6   7
        // When Total Results > 7 (say 500):
        //   Current page is between 1-5:                                 1  2   3   4   5  ... 500
        //   Current page is between 496-500                              1 ... 496 497 498 499 500
        //   Current page is not close to either range endpoint:          1 ... 311 312 313 ... 500
        //
        // This layout has (legacy) idiosyncrasies: The "pivot" aims to emulate previous algoritm results, with
        // the modulo 2 computation off the count of consecutive clicks on arrow buttons causing an oscillation
        // between selected page number buttons, without any real functional purpose.
        let pivot;
        if (pgno === total || pgno === 1) {
            this._arrowClicks = 0;
        }
        if (pgno >= HUGE_NUMBER_OF_PAGES && this.navMode === 'right-arrow') {
            pivot = this._arrowClicks % 2 === 0 ? pgno - 1 : pgno;
        } else if (this.navMode === 'right-arrow') {
            pivot = this._arrowClicks % 2 === 0 ? Math.min(total, pgno + 1) : pgno;
        } else if (this.navMode === 'left-arrow') {
            pivot = this._arrowClicks % 2 === 0 ? Math.max(1, pgno - 1) : pgno;
        } else { // Jump link
            if (pgno >= HUGE_NUMBER_OF_PAGES) {
                pivot = pgno;
            } else {
                pivot = pgno < this._previousPage ? Math.max(1, pgno - 1) : Math.min(pgno + 1, total);
            }
            this._arrowClicks = 0;
        }
        const a = [pivot];
        const addLeft = () => a[0] > 1 && a.unshift(a[0] - 1);
        const addRight = () => a[a.length - 1] < total && a.push(a[a.length - 1] + 1);
        const either = (x, y) => x || y; // Both x and y must be evaluated. This avoids boolean short-circuiting
        // eslint-disable-next-line curly
        while (a.length < 7 && either(addLeft(), addRight()));

        // Fix start
        if (a[0] !== 1) {
            a[0] = 1;
            a[1] = HORIZONTAL_ELLIPSIS;
        }

        // Fix end
        if (a[a.length - 1] !== total) {
            a[a.length - 2] = HORIZONTAL_ELLIPSIS;
            a[a.length - 1] = total;
        }

        // Drop one page if current page is 10,000 pages or above
        if (pgno >= HUGE_NUMBER_OF_PAGES) {
            a.splice(a[1] === HORIZONTAL_ELLIPSIS ? 2 : 4, 1);
        }

        return a;
    }

    // Returns an array representation of the carousel data to display, totalNumberOfPages and _currentPage have default value of 1.
    _computeCarouselButtons(totalNumberOfPages, _currentPage, minSize) {
        if (minSize) {
            return this._minSizeLayout(totalNumberOfPages, _currentPage);
        }
        return this._expandedSizeLayout(totalNumberOfPages, _currentPage);
    }

    _handleClickOnNumber(event) {
        const clickedButton = event.target;
        const clickedNumber = Number(clickedButton.label);
        this._currentPage = clickedNumber;
        this._clickedOnNumber = true;
        this._arrowClicks = 0;
        // this.notifyFocusButton invoked from updated(), after the carousel has re-rendered
    }

    __handleClickOnArrow(event) {
        if (event.target.disabled) {
            return;
        }
        const arrowId = event.target.id;
        this.navMode = arrowId;
        this._currentPage = arrowId === 'left-arrow' ? Math.max(1, this._currentPage - 1) : Math.min(this.totalNumberOfPages, this._currentPage + 1);
        this._clickedOnNumber = false;
        this.notifyFocusButton(event.target);
        this._arrowClicks++;
    }

    _observeCurrentPageChange(_currentPage) {
        if (!this._constructorInitialization) {
            this.dispatchEvent(new CustomEvent('change', {
                bubbles:  true,
                composed: true,
                detail:   {
                    pageNo: _currentPage
                }
            }));
        }
    }

    _observeTotalNumberOfPagesChange(totalNumberOfPages) {
        if (!Number.isInteger(totalNumberOfPages) || totalNumberOfPages < 1) {
            console.error('Incorrect value of total number of pages');
            this._currentPage = 1;
            return;
        }
        if (this._currentPage > this.totalNumberOfPages) {
            this._currentPage = 1;
            return;
        }
    }
};

customElements.define(PTCS.Carousel.is, PTCS.Carousel);
