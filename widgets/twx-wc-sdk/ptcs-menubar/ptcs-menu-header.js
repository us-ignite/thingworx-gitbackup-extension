import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import './ptcs-menu-item.js';

PTCS.MenuHeader = class extends PTCS.BehaviorTooltip(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement)))) {
    static get styles() {
        return css`
        :host {
            display: block;
            cursor: default;
            flex-wrap: nowrap;

            min-width: 34px;
            min-height: 19px;

            box-sizing: border-box;

            white-space: nowrap;
            overflow: hidden;
            outline: none;
        }

        :host([hidden]) {
            display: none;
        }

        :host([disabled]) {
            cursor: default;
        }`;
    }

    render() {
        return html`
        <ptcs-menu-item variant=${this.variant} id="item" part="item" .compactMode=${this.compactMode} .icon=${this.icon}
            .iconWidth=${this.iconWidth} .iconHeight=${this.iconHeight}
            .item=${this.item} level="0" header="true" .ignoreClick=${this.ignoreClick} .disabled=${this.disabled}
            .allowMissingIcons=${this.allowMissingIcons} .displayIcons=${this.displayIcons}>
        </ptcs-menu-item>`;
    }

    static get is() {
        return 'ptcs-menu-header';
    }

    static get properties() {
        return {
            compactMode: {
                type:      Boolean,
                attribute: 'compact-mode',
                reflect:   true
            },

            icon: {
                type: String
            },

            iconWidth: {
                type:      String,
                attribute: 'icon-width',
            },

            iconHeight: {
                type:      String,
                attribute: 'icon-height'
            },

            ignoreClick: {
                type:      Boolean,
                attribute: 'ignore-click'
            },

            allowMissingIcons: {
                type:      Boolean,
                attribute: 'allow-missing-icons'
            },

            displayIcons: {
                type:      Boolean,
                attribute: 'display-icons'
            },

            disabled: {
                type:    Boolean,
                reflect: true
            },

            variant: {
                type: String
            },
        };
    }

    constructor() {
        super();
        this.compactMode = false;
        this.ignoreClick = false;
        this.disabled = false;
    }

    ready() {
        super.ready();

        this.addEventListener('blur', ev => {
            this.dispatchEvent(new CustomEvent('lost-focus', {
                bubbles:  true,
                composed: true,
                detail:   {cmpnt: 'header'}
            }));
        });
    }

    // Callback for BehaviorFocus
    _initTrackFocus() {
        this._trackFocus(this, this.$.item);
    }
};

customElements.define(PTCS.MenuHeader.is, PTCS.MenuHeader);
