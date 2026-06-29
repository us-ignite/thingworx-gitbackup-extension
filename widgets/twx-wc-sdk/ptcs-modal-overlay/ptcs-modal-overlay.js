import {LitElement, html, css} from 'lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';

PTCS.ModalOverlay = class extends PTCS.BehaviorStyleable(LitElement) {
    static get styles() {
        return css`
            :host {
                cursor: default;
            }

            :host [part=backdrop] {
                display: inline-flex;
                position: fixed;
                top: 0;
                left: 0;
                bottom: 0;
                right: 0;
            }
        `;
    }

    render() {
        const style = [];

        if (this.backdropColor) {
            style.push(`background-color:${this.backdropColor}`);
        }

        if (this.backdropOpacity) {
            style.push(`opacity:${this.backdropOpacity}`);
        }

        if (this.backdropZIndex) {
            style.push(`z-index:${this.backdropZIndex}`);
        }

        return html`
            <div part="backdrop" id="backdrop" style=${style.join('; ')}></div>
        `;
    }

    static get is() {
        return 'ptcs-modal-overlay';
    }

    static get properties() {
        return {

            backdropColor: { // Backdrop color
                type:      String,
                attribute: 'backdrop-color'
            },

            backdropOpacity: { // Backdrop opacity
                type:      String,
                attribute: 'backdro-opacity'
            },

            backdropZIndex: { // Backdrop Z-index
                type:      String,
                attribute: 'backdrop-z-index'
            }

        };
    }
};

customElements.define(PTCS.ModalOverlay.is, PTCS.ModalOverlay);
