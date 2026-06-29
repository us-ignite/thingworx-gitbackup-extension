import {LitElement, html} from 'lit';
import {PTCS} from 'ptcs-library/library.js';
import {iconMgr} from './ptcs-iconset-svg.js';
import 'ptcs-icons/cds-icons.js';

PTCS.IconSvg =  class extends LitElement {
    static get properties() {
        return {
            icon: {
                type: String
            },

            src: {
                type: String
            }
        };
    }

    static get is() {
        return 'ptcs-icon-svg';
    }

    render() {
        if (this.src) {
            return html`<img src=${this.src} style="width: 100%; height: 100%;" draggable="false">`;
        }
        if (this.icon) {
            const parts = this.icon.split(':');
            const icon = parts[1] || parts[0];
            const iconSet = parts[1] ? parts[0] : 'icons';
            const fullIcon = iconMgr[iconSet] && iconMgr[iconSet].querySelector(`:scope > svg > defs > g[id=${icon}]`);
            if (fullIcon) {
                return html`<svg
                    viewBox="${fullIcon.getAttribute('viewBox') || `0 0 ${this.size || 24} ${this.size || 24}`}"
                    preserveAspectRatio="xMidYMid meet" focusable="false"
                    style="pointer-events: none; display: block; width: 100%; height: 100%;"
                    >${fullIcon.cloneNode(true)}</svg>`;
            }
        }
        return html``;
    }

    // The icon doesn't use a shadow dom
    createRenderRoot() {
        return this;
    }
};

customElements.define(PTCS.IconSvg.is, PTCS.IconSvg);
