import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';

PTCS.Tab = class extends PTCS.BehaviorTooltip(PTCS.BehaviorFocus(L2Pw(LitElement))) {
    static get styles() {
        return css`
        :host {
            display: flex;
            align-items: center;
            outline: none;
            padding: 48px 0;
            box-sizing: border-box;
            flex-shrink: 0;
            color: var(--ptcs-text-color, #1675be);
        }

        :host([hidden]) {
            display: none !important;
        }

        :host([disabled]) {
            pointer-events: none;
            color: var(--ptcs-text-color__disabled, #cccccc);
        }

        :host([focus-ring]) {
            background-color: rgba(0, 0, 0, 0.1);
        }`;
    }

    static get properties() {
        // These were implemented somewhere in the vaadin layers, now with that part gone we need it here...
        return {
            selected: {
                type:    Boolean,
                reflect: true,
                notify:  true
            },

            disabled: {
                type:    Boolean,
                reflect: true
            },

            labelContent: {
                type:      String,
                attribute: 'label-content',
                reflect:   true
            },

            orientation: {
                type:    String,
                reflect: true
            }
        };
    }


    render() {
        return html`<slot></slot>`;
    }

    static get is() {
        return 'ptcs-tab';
    }

    constructor() {
        super();

        this.orientation = 'horizontal';
    }

    ready() {
        super.ready();
        this.tooltipFunc = this._monitorTooltip.bind(this);
        this.focusNoClipping = true; // Want a full focus rect around the tab
        this.addEventListener('focus', this._focusEv.bind(this));
        this.addEventListener('blur', this._blurEv.bind(this));
        this.addEventListener('click', this._clickEv.bind(this));
        this.addEventListener('keydown', this._keyDownEv.bind(this));
    }

    connectedCallback() {
        super.connectedCallback();
        if (this.parentNode.disabled) { // When tabs are initialized in disabled
            const tabNumber = this.getAttribute('tab-number') - 1;
            this.setAttribute('tabindex', tabNumber === this.parentElement.selected ? '0' : '-1');
        }
    }

    _monitorTooltip() { // Implements ptcs-tab's tooltip behavior on tab name truncation
        const el = this.firstElementChild; // ptcs-label in slotted content
        if (el && typeof el.tooltipFunc === 'function') { // Does the container have a function to deliver the tooltip contents?
            return el.tooltipFunc() || '';
        }
        return '';
    }

    _keyDownEv(ev) {
        if (this.disabled || ev.defaultPrevented || !this.tabindex) {
            return;
        }
        if (ev.key === 'Enter' || ev.key === ' ') {
            ev.preventDefault();
            this._dispatchFocusOrClickEv('tab-clicked');
        }
    }

    _dispatchFocusOrClickEv(eventName) {
        const tabNumberAttr = this.getAttribute('tab-number');
        let index = 0;
        if (tabNumberAttr === null) {
            // Use the same 1-based index as the 'tab-number' attribute...
            index = 1;
            // This ptcs-tab does not have a tab-number attribute set, so check its position in the DOM tree instead
            for (let el = this.previousElementSibling; el; el = el.previousElementSibling) {
                if (el.nodeName === 'PTCS-TAB') {
                    index++;
                }
            }
        }
        const tabNumber = index ? index : +tabNumberAttr;
        if (!isNaN(tabNumber) && tabNumber > 0) {
            this.dispatchEvent(new CustomEvent(eventName, {
                bubbles:  true, composed: true,
                detail:   {selected: tabNumber - 1}
            }));
        }
    }

    _focusEv(ev) {
        this._dispatchFocusOrClickEv('tab-focus');
        this.setAttribute('focused', '');
    }

    _blurEv() {
        this.removeAttribute('focused');
    }

    _clickEv(ev) {
        this.focus();
        this._dispatchFocusOrClickEv('tab-clicked');
    }
};

customElements.define(PTCS.Tab.is, PTCS.Tab);
