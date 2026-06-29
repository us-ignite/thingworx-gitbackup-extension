import {LitElement} from 'lit';
import {PTCS} from 'ptcs-library/library.js';

export const iconMgr = {};

PTCS.IconSetSvg =  class extends LitElement {
    static get is() {
        return 'ptcs-iconset-svg';
    }

    connectedCallback() {
        super.connectedCallback();

        if (!this._icons) {
            this.style.display = 'none';
            this._icons = {};
            this.querySelectorAll('defs > [id]').forEach(icon => {
                this._icons[icon.id] = icon;
            });
        }
        this.registerIconSet(this.getAttribute('name'));
    }

    disconnectedCallback() {
        const name = this.getAttribute('name');
        if (iconMgr[name] === this) {
            delete iconMgr[name];
        }
        document.dispatchEvent(new CustomEvent('ptcs-iconset-svg', {detail: {name: name}}));
        super.disconnectedCallback();
    }

    registerIconSet(name) {
        if (!iconMgr[name]) {
            iconMgr[name] = this;
            document.dispatchEvent(new CustomEvent('ptcs-iconset-svg', {detail: {name: name}}));
        }
    }

    unregisterIconSet(name) {
        if (iconMgr[name] === this) {
            delete iconMgr[name];
            document.dispatchEvent(new CustomEvent('ptcs-iconset-svg', {detail: {name: name}}));
        }
    }

    rename(newName) {
        this.unregisterIconSet(this.getAttribute('name'));
        this.registerIconSet(newName);
    }
};

customElements.define(PTCS.IconSetSvg.is, PTCS.IconSetSvg);
