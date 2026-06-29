import {LitElement, html, svg, css} from 'lit';
import {PTCS} from 'ptcs-library/library.js';
import {L2Pw} from 'ptcs-library/library-lit';
import './ptcs-icon-svg.js';
import 'ptcs-icons/ptcs-icons';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';

// Default placeholder icons
// eslint-disable-next-line max-len
const defaultIconPlaceholder = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTZweCIgaGVpZ2h0PSIxNnB4IiB2aWV3Qm94PSIwIDAgMTYgMTYiIHZlcnNpb249IjEuMSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBzdHJva2U9Im5vbmUiIGZpbGw9IiM2RTcxN0MiIGZpbGwtcnVsZT0ibm9uemVybyI+PHBhdGggZD0iTTE2LDUuOSBMMTAuNSw1IEw4LDAgTDUuNSw1LjEgTDAsNS45IEw0LDkuOCBMMywxNiBMOCwxMi43IEwxMywxNiBMMTIsOS44IEwxNiw1LjkgWiBNOCwxMSBMNSwxMyBMNS42LDkuNCBMMy4yLDYuOSBMNi41LDYuNCBMOCwzLjQgTDkuNSw2LjQgTDEyLjgsNi45IEwxMC40LDkuMiBMMTEsMTIuOCBMOCwxMSBaIj48L3BhdGg+PC9nPjwvc3ZnPg==';

// Customizable placeholder icon
let globalPlaceholderIcon;

// Observers for placeholder icon
const watchPlaceholderIcon = new Set();

PTCS.Icon = class extends PTCS.BehaviorTooltip(PTCS.BehaviorStyleable(L2Pw(LitElement))) {
    static get styles() {
        return css`
        :host {
            flex: 0 0 auto;
            display: inline-flex;
            justify-content: space-between;
            align-items: stretch;
            box-sizing: border-box;
            fill: currentColor;
        }

        [part=image] {
            flex: 1 1 auto;
        }

        :host([hidden]) {
            display: none !important;
        }

        :host([aria-disabled="true"]) {
            pointer-events: none;
        }

        :host([disabled]) [part=image] > img {
            opacity: 0.6;
        }`;
    }

    render() {
        let {icon} = this;
        const placeholder = this._usingPlaceholder;
        const src = () => this.preventCaching ? `${icon}?${Date.now()}` : icon;

        if (this.iconSet && this.iconSet !== '#iron-icon') {
            return svg`<svg part="image"><use href=${encodeURI(`${this.iconSet}#${src()}`)}></use></svg>`;
        }

        if (icon && icon !== '#none') {
            // Try do decode this into an icon name
            const m = /^([a-zA-Z0-9_-]+):([a-zA-Z0-9_-]+)$/.exec(icon);
            if (m) {
                return this._createIcon(icon);
            }
            return this._createImage(src);
        }

        if (!this.placeholder) {
            // Don't want an icon displayed (right now at least)
            return html`<div part="image" alt=" "></div>`;
        }

        // Display placeholder
        this._usingPlaceholder = icon = PTCS.Icon.placeholderIcon;

        if (!this._usingPlaceholder !== !placeholder) {
            if (this._usingPlaceholder) {
                watchPlaceholderIcon.add(this);
            } else {
                watchPlaceholderIcon.delete(this);
            }
        }

        // Chrome will display a 'broken icon image' if the icon reference doesn't resolve, but only if alt is non-empty.
        // This ought to be due to guideline from HTML5 spec, see
        // https://www.w3.org/TR/html5/semantics-embedded-content.html#a-purely-decorative-image-that-doesnt-add-any-information.
        // However, VD doesn't want a tooltip to appear in such case, so just using a space as alt attribute value.
        return html`<img part="image" src=${src()} alt=" " style="width:100%;height:100%;">`;
    }

    static get properties() {
        return {
            // A URL to an icon set from which `icon` selects an icon
            iconSet: {
                type:      String,
                attribute: 'icon-set'
            },

            // If `iconSet` is unspecified, `icon` specifies a separate icon file.
            // If `iconSet` is specified, `icon` specifies an icon in that icon set
            icon: {
                type: String
            },

            // A descriptive text of the icon, intended for screen readers / assistive technologies
            alt: {
                type:    String,
                reflect: true
            },

            // The size of the image: small || medium || large || xlarge
            size: {
                type:        String,
                reflect:     true,
                observer:    '_iconSizeChanged',
                observeWhen: 'immediate'
            },

            // Sets a fixed width for the icon
            iconWidth: {
                type:        String,
                attribute:   'icon-width',
                observer:    '_iconSizeChanged',
                observeWhen: 'immediate'
            },

            // Sets a fixed height for the icon
            iconHeight: {
                type:        String,
                attribute:   'icon-height',
                observer:    '_iconSizeChanged',
                observeWhen: 'immediate'
            },

            // Prevent the icon source from being cached by the browser so that the most recent version is shown when reloaded
            preventCaching: {
                type:      Boolean,
                attribute: 'prevent-caching'
            },

            // Use placeholder image if the icon property is empty
            placeholder: {
                type:    Boolean,
                reflect: true
            },

            _ariaLabel: {
                type:      String,
                attribute: 'aria-label',
                reflect:   true
            },

            disabled: {
                type:    Boolean,
                reflect: true
            }
        };
    }

    static get is() {
        return 'ptcs-icon';
    }

    constructor() {
        super();
        this.size = 'small';
        this.preventCaching = false;
        this.placeholder = false;
        this.disabled = false;
    }

    connectedCallback() {
        super.connectedCallback();
        if (this._usingPlaceholder) {
            watchPlaceholderIcon.delete(this);
        }
    }

    disconnectedCallback() {
        if (this._usingPlaceholder) {
            watchPlaceholderIcon.add(this);
        }
        super.disconnectedCallback();
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);
        this._ariaLabel = this.alt;
    }

    // Get current placeholder icon
    static get placeholderIcon() {
        return globalPlaceholderIcon || defaultIconPlaceholder;
    }

    // Set a new placeholder icon
    static set placeholderIcon(icon) {
        if (globalPlaceholderIcon !== icon) {
            globalPlaceholderIcon = icon;
            watchPlaceholderIcon.forEach(el => el.requestUpdate());
        }
    }

    _iconSizeChanged() {
        if (this.size === 'custom') {
            this.style.width = PTCS.normalizeUnit(this.iconWidth);
            this.style.height = PTCS.normalizeUnit(this.iconHeight);
        } else {
            this.style.removeProperty('width');
            this.style.removeProperty('height');
        }
    }

    _createIcon(icon) {
        const el = PTCS.createElement('ptcs-icon-svg', {style: 'stroke:inherit;fill:inherit;width:unset;height:unset;', part: 'image', alt: ' '});
        el.icon = icon;
        return el;
    }

    _createImage(src) {
        const el = PTCS.createElement('div', {part: 'image', alt: ' '});
        el.appendChild(PTCS.createElement('img', {style: 'width:100%;height:100%;', src: src()}));
        return el;
    }

    // Hack for PTCS grid.
    performGridDisable(isDisabled) {
        // disabled property not offically supported. Need to manually assign aria-disabled attribute
        this.disabled = !!isDisabled;
        if (this.disabled) {
            this.setAttribute('aria-disabled', 'true');
        } else {
            this.removeAttribute('aria-disabled');
        }
    }

    static get $parts() {
        return ['image'];
    }
};

customElements.define(PTCS.Icon.is, PTCS.Icon);

export const Icon = PTCS.Icon;
