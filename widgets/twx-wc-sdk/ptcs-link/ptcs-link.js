import {LitElement, html, css} from 'lit';
import {ifDefined} from 'lit/directives/if-defined.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';
import 'ptcs-label/ptcs-label.js';

PTCS.Link = class extends PTCS.BehaviorTabindex(PTCS.BehaviorTooltip(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))))) {
    static get styles() {
        return css`
            :host {
                /*display: inline-block;*/
                display: inline-flex;
                justify-content: space-between;
                align-items: center;

                overflow: hidden;

                min-width: 34px;
                min-height: 19px;

                box-sizing: border-box;

                align-items: flex-start;
                overflow: auto
            }

            :host([disabled]) [part=link] {
                cursor: auto;
                pointer-events: none;
            }

            :host(:not([disabled]):not([variant=label])) [part=link] {
                cursor: pointer;
            }

            :host([vertical-alignment=flex-start]) {
                align-items: flex-start;
            }

            :host([vertical-alignment=center]) {
                align-items: center;
            }

            :host([vertical-alignment=flex-end]) {
                align-items: flex-end;
            }

            :host([_zero-padding-no-scroll]) {
                overflow: hidden;
                min-height: 0px;
            }

            :host([_zero-padding-no-scroll]) [part=label]{
                padding-top: 0px;
                padding-bottom: 0px;
            }

            a {
                display: inline-flex;

                width: 100%;
            }

            [part=label] {
                width: inherit;

                text-decoration: inherit;

                min-width: unset;
                min-height: unset;
            }
        `;
    }

    render() {
        return html`
            <a part="link" id="link" href=${ifDefined(this._compute_href(this.disabled, this.href))} target=${this._compute_target(this.target)}
                tabindex=${this._tabindex(this._delegatedFocus, this.noTabindex)} rel="nofollow noopener noreferrer">
                <ptcs-label part="label" id="label" .variant=${this.variant} .label=${this.label} .multiLine=${!this.singleLine}
                    exportparts=${this._exportparts} ?disScrollOnEllipsMultiLine=${this._disScrollOnPtcsLabelEllipsMultiLine}
                    .maxHeight=${this._disScrollOnPtcsLabelMaxHeight} .horizontalAlignment=${this.alignment}
                    .maxNumberOfLines=${this.maxNumberOfLines} disclosure-control="ellipsis" disable-tooltip no-wc-style/>
            </a>
        `;
    }

    static get is() {
        return 'ptcs-link';
    }

    static get properties() {
        return {
            href: {
                type:     String,
                observer: '_hrefChanged',
                reflect:  true
            },

            linkRouted: {
                type:      Boolean,
                attribute: 'link-routed',
            },

            target: {
                type:    String,
                reflect: true
            },

            label: {
                type:    String,
                reflect: true
            },

            // Note - variant is assigned to the internal ptcs-label too, to prevent it from the default variant=label.
            //        However, since no-wc-style is active, the variant for the ptcs-label is ignored, so the assignemnt
            //        is only for code readability.
            variant: {
                type:    String,
                value:   'primary',
                reflect: true
            },

            singleLine: {
                type:      Boolean,
                attribute: 'single-line',
                value:     false
            },

            disabled: {
                type:    Boolean,
                value:   false,
                reflect: true
            },

            ariaDisabled: {
                type:      String,
                attribute: 'aria-disabled',
                computed:  '_compute_ariaDisabled(disabled)',
                reflect:   true
            },

            role: {
                type:    String,
                value:   'link',
                reflect: true
            },

            alignment: {
                type: String
            },

            textMaximumWidth: {
                type:      String,
                attribute: 'text-maximum-width',
                observer:  '_textMaximumWidth_changed'
            },

            maxNumberOfLines: {
                type:      Number,
                attribute: 'max-number-of-lines'
            },

            _zeroPaddingNoScroll: {
                type:    Boolean,
                reflect: true
            },

            _disScrollOnPtcsLabelEllipsMultiLine: {
                type: Boolean
            },

            _disScrollOnPtcsLabelMaxHeight: {
                type: String
            },

            _delegatedFocus: {
                type:  String,
                value: null
            },

            verticalAlignment: {
                type:      String,
                attribute: 'vertical-alignment',
                reflect:   true
            },

            _exportparts: {
                type:     String,
                readOnly: true,
                value:    PTCS.exportparts('label-', PTCS.Label)
            }
        };
    }

    constructor() {
        super();
        this.label = 'Link';
    }

    ready() {
        super.ready();

        const link = this.$.link;
        this._trackFocus(link, this);
        this.addEventListener('click', this._onClick.bind(this), true);

        // Custom tooltip func
        this.tooltipFunc = this._monitorTooltip;

        link.addEventListener('keypress', ev => {
            const key = ev.which || ev.keyCode;
            if ((key === 32 || key === 13) && !this.disabled) {
                this.$.label.click();
                ev.preventDefault();
            }
        });
    }

    // Implements ptcs-link's tooltip behavior on label truncation
    _monitorTooltip() {
        const label = this.$.label;
        if (label && label.isTruncated()) {
            if (!this.tooltip) {
                return this.label;
            } else if (this.tooltip !== this.label) {
                // Truncated label with a tooltip (not identical to the label), show both
                return this.label + '\n\n' + this.tooltip;
            }
        } else if (this.tooltip === this.label) {
            return '';
        }
        return this.tooltip || '';
    }

    _tabindex(_delegatedFocus, noTabindex) {
        return (_delegatedFocus && !noTabindex) ? _delegatedFocus : '-1';
    }

    _hrefChanged(href) {
        if (!PTCS.validateURL(href)) {
            console.warn('[ptcs-link]XSS prevention: URL includes the protocol "javascript:"');
        }
    }

    _compute_href(disabled, href) {
        if (disabled || !href) {
            return undefined;
        }
        return PTCS.validateURL(href) ? (PTCS.rectifyURI(href) || '#') : '';
    }


    _compute_target(target) {
        switch (target) {
            case '_self':
            case '_blank':
            case '_parent':
            case '_top':
                return target;

            case 'same':
                return '_self';

            case 'new':
                return '_blank';

            case '_popup':
            case 'popup':
                return 'PopupWindow';
        }

        return '_self';
    }

    _activateLink(event) {
        event.preventDefault();
        event.stopImmediatePropagation();
        this.activateLink() ;
    }


    _onClick(ev) {
        if (this.disabled || !PTCS.validateURL(this.href) || this.isIDE) {
            ev.preventDefault();
            return;
        }

        const targetName = ev.composedPath();
        if (targetName && targetName[0] && targetName[0].nodeName === 'PTCS-LINK') {
            // in case one click on ptcs-link but not on the anchor element itself (in case ptcs-link has padding)
            this._activateLink(ev);
            return;
        }

        // Add event for hyperlink
        const evClickA = new CustomEvent('a-click', {
            bubbles:    true,
            cancelable: true,
            composed:   true,
            detail:     {
                a:             this.shadowRoot.querySelector('a'),
                originalEvent: ev
            }});

        this.dispatchEvent(evClickA);

        // If the user is handling the link routing, then we are done...
        if (evClickA.defaultPrevented || this.linkRouted) {
            ev.preventDefault();
            return;
        }

        const trgt = this._compute_target(this.target);
        if (trgt === 'PopupWindow') {
            ev.preventDefault();
            const wnd = PTCS.openUrl('open', this.href, 'PopupWindow', 'height=450,width=700');
            if (wnd) {
                wnd.focus();
            }
        } else {
            const bRequireReload = trgt !== '_self';
            PTCS.keepHashForSSORedirect(this.href, bRequireReload); // <a href> doen't call to openUrl but _onClick helps us to manage #-part (SSO)
        }

    }

    _compute_ariaDisabled(disabled) {
        return disabled ? 'true' : false;
    }

    connectedCallback() {
        super.connectedCallback();
        this._connected = true;
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this._connected = false;
    }

    _textMaximumWidth_changed(val) {
        if (val) {
            const unitTest = val + '';
            if (unitTest.indexOf('px') === -1) {
                this.$.label.style.maxWidth = val + 'px';
            } else {
                this.$.label.style.maxWidth = val;
            }
        } else {
            this.$.label.style.maxWidth = '';
        }
    }

    // Allow manual activation of the link
    activateLink() {
        this.$.label.click();
    }

    static get $parts() {
        if (!this._$parts) {
            this._$parts = ['link', 'label', ...PTCS.partnames('label-', PTCS.Label)];
        }
        return this._$parts;
    }
};

customElements.define(PTCS.Link.is, PTCS.Link);
