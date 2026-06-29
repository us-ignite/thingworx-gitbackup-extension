import {LitElement, html, css} from 'lit';
import {when} from 'lit/directives/when.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-image/ptcs-image.js';
import 'ptcs-label/ptcs-label.js';

PTCS.MenuFooter = class extends PTCS.BehaviorTooltip(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement)))) {
    static get styles() {
        return css`
        :host {
            cursor: pointer;
            display: flex;
            flex-direction: row;
            min-width: 34px;
            min-height: 19px;
            align-items: center;

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
        }

        :host [part=icon][hidden] {
            display: none;
        }

        [part=icon] {
            flex-grow: 0;
            flex-shrink: 0;
        }

        [part=label] {
            flex-grow: 100;
        }`;
    }

    render() {
        return html`
        ${when(!this._hideImage, () => html`
            <ptcs-image id="icon" part="icon" .src=${this._imageSrc} size="contain" aria-hidden="true"></ptcs-image>`)}
        ${when(!this._hideLabel, () => html`
            <ptcs-label part="label" id="label" .label=${this.text} .verticalAlignment="center" disable-tooltip></ptcs-label>`)}`;
    }

    static get is() {
        return 'ptcs-menu-footer';
    }

    static get properties() {
        return {
            text: {
                type: String
            },

            icon: {
                type: String
            },

            logo: {
                type: String
            },

            item: {
                type: Object
            },

            compactMode: {
                type:      Boolean,
                attribute: 'compact-mode'
            },

            ignoreClick: {
                type:      Boolean,
                attribute: 'ignore-click'
            },

            disabled: {
                type:    Boolean,
                reflect: true
            },

            iconWidth: {
                type:     Number,
                observer: 'iconWidthChanged'
            },

            iconHeight: {
                type:     Number,
                observer: 'iconHeightChanged'
            },

            hidden: {
                type:    Boolean,
                reflect: true
            },

            logoMode: {
                type:      Boolean,
                attribute: 'logo-mode',
                observer:  '_logoModeChanged',
                reflect:   true
            },

            _imageSrc: {
                type:      String,
                attribute: '_image-src'
            },

            _hideImage: {
                type:     Boolean,
                computed: '_hideImageFunc(compactMode, logo, icon)',
            },

            _hideLabel: {
                type:     Boolean,
                computed: '_hideLabelFunc(compactMode, text, logo, icon)'
            },

            variant: {
                type: String
            },

            _resizeObserver: ResizeObserver
        };
    }

    static get observers() {
        return [
            '_imageSrcFunc(compactMode, logo, icon)'
        ];
    }

    constructor() {
        super();
        this.ignoreClick = false;
        this.disabled = false;
        this.hidden = false;
        this.logoMode = false;
    }

    ready() {
        super.ready();
        this.addEventListener('click', (ev) => {
            this._emitActionEvent();
            ev.preventDefault();
        });
        this.addEventListener('keydown', ev => this._emitActionEvent(ev));
        this.tooltipFunc = this._monitorTooltip;

        this.addEventListener('blur', () => {
            this.dispatchEvent(new CustomEvent('lost-focus', {
                bubbles:  true,
                composed: true,
                detail:   {cmpnt: 'footer'}
            }));
        });
    }

    connectedCallback() {
        super.connectedCallback();
        if (!this.resizeObserver) {
            this._resizeObserver = new ResizeObserver(this.__resize.bind(this));
        }
        this._resizeObserver.observe(this);
    }

    disconnectedCallback() {
        this._resizeObserver.unobserve(this);
        super.disconnectedCallback();
    }

    _logoModeChanged(logoMode) {
        if (logoMode) {
            // We must allow the logo to shrink when the mode is toggled and it doesn't fit in the menu
            this.__resize();
        }
    }


    __resize() {
        if (this.compactMode || !this.logo) {
            return;
        }
        const footerWidth = PTCS.getElementWidth(this) || 0;
        const widthStr = window.getComputedStyle(this.$.icon).getPropertyValue('width');
        const imageWidth = Number(widthStr.substr(0, widthStr.indexOf('px'))) || 0;

        if (this.logoMode) {
            this.__logoimageWidth = imageWidth;
            if (imageWidth > footerWidth) {
                this._imageSrc = this.icon;
                this.logoMode = false;
                this.$.icon.position = 'center';
            }
        } else if (footerWidth > this.__logoimageWidth) {
            this._imageSrc = this.logo;
            this.logoMode = true;
            this.$.icon.position = 'left';
        }
    }

    iconWidthChanged(iconWidth) {
        if (iconWidth) {
            this.$.icon.style.width =  PTCS.normalizeUnit(iconWidth);
        } else {
            this.$.icon.style.width = '';
        }
    }

    iconHeightChanged(iconHeight) {
        if (iconHeight) {
            this.$.icon.style.height =  PTCS.normalizeUnit(iconHeight);
        } else {
            this.$.icon.style.height = '';
        }
    }

    _imageSrcFunc(compactMode, logo, icon) {
        const iconEl = this.shadowRoot.getElementById('icon');
        if (!compactMode && logo) {
            this._imageSrc = logo;
            this.logoMode = true;
            if (iconEl) {
                iconEl.position = 'left';
            }
        } else {
            this._imageSrc = icon;
            this.logoMode = false;
            if (iconEl) {
                iconEl.position = 'center';
            }
        }
        this.__resize();
    }

    _hideImageFunc(compactMode, logo, icon) {
        return !!(!icon && (compactMode || (!compactMode && !logo)));
    }

    _hideLabelFunc(compactMode, text, logo, icon) {
        return !!(!text || (compactMode && icon) || (!compactMode && logo));
    }

    _monitorTooltip() {
        // Is the label actually present (don't use this.$.label)?
        const label = this.shadowRoot.getElementById('label');
        if (!label || this.compactMode) {
            return this.text;
        }
        // In the "normal" case, the label component handles everything...
        return label.tooltipFunc();
    }

    _emitActionEvent(ev) {
        if (!this.disabled && !this.ignoreClick) {
            this._tooltipClose();
            if (!ev || ev && (ev.key === ' ' || ev.key === 'Enter')) {
                this.dispatchEvent(new CustomEvent('action',
                    {
                        bubbles:  true,
                        composed: true,
                        detail:   {item: this.item}
                    })
                );
            }
        }
    }
};

customElements.define(PTCS.MenuFooter.is, PTCS.MenuFooter);
