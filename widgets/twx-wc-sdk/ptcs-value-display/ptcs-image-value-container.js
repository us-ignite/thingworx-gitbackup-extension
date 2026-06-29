import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-image/ptcs-image.js';

PTCS.ImageValueContainer = class extends PTCS.BehaviorStyleable(L2Pw(LitElement)) {

    static get styles() {
        return css`
                [part=image-area] {
                    width: 100%;
                    height: 100%;
                }`;
    }

    render() {
        return html`<div id="image-area" part="image-area">
                <ptcs-image id="image" part="image"
                  .src=${this.src} .size=${this._size} .position=${this._position} .alt=${this.altText} no-placeholder
                ></ptcs-image>
            </div>
            `;
    }

    static get is() {
        return 'ptcs-image-value-container';
    }

    static get properties() {
        return {
            src: {
                type: String
            },

            scaling: {
                type:     String,
                observer: '_observeScaling',
                value:    'image'
            },

            width: {
                type: Number
            },

            height: {
                type: Number
            },

            // Image alt text
            altText: {
                type:      String,
                attribute: 'alt-text'
            },

            _size: {
                type: String,
            },

            _position: {
                type: String
            }
        };
    }

    static get observers() {
        return [
            '_observeHeightWidthScaling(height, width, scaling)'
        ];
    }

    _observeHeightWidthScaling(height, width, scaling) {
        const imageArea = this.$['image-area'];
        if (imageArea) {
            if (scaling.indexOf('%') !== -1) {
                imageArea.style.width = '';
                imageArea.style.height = '';
            } else {
                if (width > 0) {
                    imageArea.style.width = width + 'px';
                }
                if (height > 0) {
                    imageArea.style.height = height + 'px';
                }
            }
        }
    }

    _observeScaling(scaling) {
        // Mapping of TWX scaling options to corresponding ptcs-image size options
        switch (scaling) {
            case 'image':
                this._size = 'auto';
                this._position = 'center';
                break;

            case 'scaledtowidth':
                this._size = 'fit-x';
                this._position = 'top';
                break;

            case 'scaledtoheight':
                this._size = 'fit-y';
                this._position = 'top';
                break;

            case 'contain':
                this._size = 'contain';
                this._position = 'center';
                break;

            case '25%':
            case '50%':
            case '75%':
                this._size = scaling;
                this._position = 'center';
                break;

            case 'cover':
            default:
                this._size = 'cover';
                this._position = 'center';
                break;
        }
    }
};

customElements.define(PTCS.ImageValueContainer.is, PTCS.ImageValueContainer);
