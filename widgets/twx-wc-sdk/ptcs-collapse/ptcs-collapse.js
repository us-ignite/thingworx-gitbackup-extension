import {PTCS} from 'ptcs-library/library.js';
import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit.js';

PTCS.Collapse = class extends L2Pw(LitElement) {
    static get styles() {
        return css`
        :host {
            display: block;
            overflow: hidden;
            transition: max-height var(--ptcs-collapse-transition-duration, 300ms);
        }`;
    }

    render() {
        return html`<slot></slot>`;
    }

    static get is() {
        return 'ptcs-collapse';
    }

    static get properties() {
        return {
            opened: {
                type:     Boolean,
                notify:   true,
                observer: '_openedChanged'
            }
        };
    }

    ready() {
        super.ready();

        if (!this.opened) {
            // Don't show any content
            this.style.maxHeight = '0';
        }

        this.setAttribute('role', 'group');
        this.addEventListener('transitionend', this._transitionEnd.bind(this));
    }

    toggle() {
        this.opened = !this.opened;
    }

    show() {
        this.opened = true;
    }

    hide() {
        this.opened = false;
    }

    _openedChanged(opened) {
        if (opened) {
            this.removeAttribute('aria-hidden');

            // Grow height of component to reveal its content
            const height = [...this.children].reduce((w, el) => w + PTCS.getElementHeight(el), 0);

            this.style.maxHeight = height > 0 ? `${height}px` : ''; // If there is nothing to grow, start as fully expanded
        } else {
            this.setAttribute('aria-hidden', 'true');

            // Set start height
            this.style.maxHeight = `${this.offsetHeight}px`;

            requestAnimationFrame(() => {
                // Set end height - and start animation
                this.style.maxHeight = '0';
            });
        }
    }

    _transitionEnd() {
        // Adapt height to the height of the children - or hide them
        this.style.maxHeight = this.opened ? '' : '0';
    }
};

customElements.define(PTCS.Collapse.is, PTCS.Collapse);
