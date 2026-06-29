import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-image/ptcs-image.js';
import 'ptcs-icons/cds-icons.js';
import 'ptcs-modal-overlay/ptcs-modal-overlay.js';
import './ptcs-modal-image-popup-container.js';

PTCS.ModalImagePopup = class extends PTCS.BehaviorStyleable(L2Pw(LitElement)) {

    static get styles() {
        return css`
        :host {
          display: flex;
          align-items: center;
          justify-content: center;
        }

        :host([disabled]) {
            pointer-events: none;
        }

        :host([disabled]) [part=popup-container]::selection {
            background-color: transparent;
        }

        [part=live-art-area-image-thumbnail] {
            box-sizing: border-box;
        }

        [part=popup-root] {
            box-sizing: border-box;
        }`;
    }

    render() {
        return html`<div part="popup-root" id="root">
            <div part="live-art-area-image-thumbnail" @load=${this._onLoad}>
                <ptcs-image id="img" part="image" src=${this.src} alt=${this.altText} label-variant="body"
                  size="contain" position="center"></ptcs-image>
            </div>
        </div>`;
    }

    static get is() {
        return 'ptcs-modal-image-popup';
    }

    static get properties() {
        return {

            // Image url
            src: {
                type: String
            },

            // Image alt text
            altText: {
                type:      String,
                attribute: 'alt-text'
            },

            // Toggle to show or hide the popup dialog
            _showpopup: {
                type: Boolean
            },

            // Modal backdrop color
            backdropColor: {
                type:      String,
                attribute: 'backdrop-color'
            },

            // Modal backdrop opacity
            backdropOpacity: {
                type:      String,
                attribute: 'backdrop-opacity'
            },

            // Prevents pop-up when true
            disabled: {
                type:    Boolean,
                reflect: true
            },

            // Fixed width constraint
            width: {
                type: Number
            },

            // Fixed height constraint
            height: {
                type: Number
            },

            // Widget max-width constraint in pixels (i.e. including any internal padding etc)
            maxWidth: {
                type:      Number,
                attribute: 'max-width'
            },

            // Widget max-height constraint in pixels (i.e. including any internal padding, space for label etc)
            maxHeight: {
                type:      Number,
                attribute: 'max-height'
            },

            // The height of the displayed image
            _imageScaledHeight: {
                type: Number
            },

            // The width of the displayed image
            _imageScaledWidth: {
                type: Number
            },

            // The intrinsic width of the image
            _naturalWidth: {
                type: Number
            },

            // The intrinsic height of the image
            _naturalHeight: {
                type: Number
            },

            // Did image load?
            _imgLoaded: {
                type: Boolean
            },

            //  If the displayed image is smaller than its intrinsic size it overflows
            overflow: {
                type:     Boolean,
                observer: '_overflowChanged',
                reflect:  true
            }
        };
    }

    static get observers() {
        return [
            '_observe(width, height, maxWidth, maxHeight, _imgLoaded)'
        ];
    }

    disconnectedCallback() {
        this.close();
        if (this._dialog) {
            document.body.removeChild(this._dialog);
        }
        const mdl = document.body.querySelector('ptcs-modal-overlay');
        if (mdl) {
            document.body.removeChild(mdl);
        }
        super.disconnectedCallback();
    }

    open() {
        if (!this._showpopup) { // If we are not showing a modal background / dialog...
            if (!this.disabled) {

                // Create the modal overlay and style it
                const mdl = document.createElement('ptcs-modal-overlay');
                mdl.backdropColor = this.backdropColor;
                mdl.backdropOpacity = this.backdropOpacity;
                document.body.appendChild(mdl); // Insert backdrop as child of body

                this._showpopup = true;

                if (!this._dialog) {
                    const popup = document.createElement('ptcs-modal-image-popup-container');
                    popup.src = this.src;
                    this._dialog = document.body.appendChild(popup);
                }

                // Store the current 'focus' element (in a PD, this is the PD itself and not the VD)
                this.__prevFocusElt = document.activeElement;

                if (this.__prevFocusElt) {
                    // "Un-focus" while the popup is open
                    this.__prevFocusElt.blur();
                }

                // Add an event listener that prevents the user from tabbing out of the modal dialog and
                // allows closing it with <ESC>, <Enter>, or <Space>
                requestAnimationFrame(() => {
                    if (!this._captureTab) {
                        this._captureTab = (ev) => {
                            switch (ev.key) {
                                case 'Enter':
                                case 'Escape':
                                case ' ':
                                    this.close();
                                    // Fall through to next case (preventDefault())
                                case 'Tab':
                                    ev.preventDefault();
                                    break;
                            }
                        };
                    }
                    document.addEventListener('keydown', this._captureTab);
                });
            }
            requestAnimationFrame(() => {
                this._dialog.shadowRoot.querySelector('[part=popup-close-button-container]').addEventListener('click', () => this.close());
            });
        }
    }

    close() {
        if (this._showpopup) {
            // Remove the popup from DOM
            document.body.removeChild(this._dialog);
            this._dialog = undefined;
            // Remove the modal overlay
            const mdl = document.body.querySelector('ptcs-modal-overlay');
            if (mdl) {
                document.body.removeChild(mdl);
            }
            this.dispatchEvent(new CustomEvent('popup-close-action'), {
                bubbles:  true,
                composed: true
            });
            this._showpopup = false;

            // Remove the "global" event listener for the "modal" popup
            document.removeEventListener('keydown', this._captureTab);

            // Restore focus to "main" part of the component
            if (this.__prevFocusElt) {
                this.__prevFocusElt.focus();
                this.__prevfocusElt = undefined;
            }
        }
    }

    // Image has been loaded
    _onLoad(ev) {
        this.setProperties({_imgLoaded: true, _naturalWidth: ev.detail.naturalWidth, _naturalHeight: ev.detail.naturalHeight});
    }

    // Report image overflow state
    _overflowChanged(overflow) {
        this.dispatchEvent(new CustomEvent(
            'image-overflow',
            {
                bubbles:  true,
                composed: true,
                detail:   {overflow: overflow}
            }));
    }

    _observe(width, height, maxWidth, maxHeight, _imgLoaded) {
        if (_imgLoaded) {
            const DEFAULT_IMG_HEIGHT = 158; // max default thumbnail height
            const DEFAULT_IMG_WIDTH = 284; // max default thumbnail width
            const IMG_WS = 16; // Padding horisontally and vertically
            const constraintV = maxHeight ? Math.min(maxHeight, DEFAULT_IMG_HEIGHT) : DEFAULT_IMG_HEIGHT;
            const constraintH = maxWidth ? Math.min(maxWidth, DEFAULT_IMG_WIDTH) : DEFAULT_IMG_WIDTH;
            if (this._naturalHeight <= constraintV && this._naturalWidth <= constraintH) {
                // No disclosure button required, image fits at its intrinsic size within size constraints
                this._imageScaledWidth = this._naturalWidth;
                this._imageScaledHeight = this._naturalHeight;
                this.$.root.style = 'height: ' + this._naturalHeight + 'px; width: ' + this._naturalWidth + 'px;';
                this.overflow = false;
            } else {
                // Overflow with disclosure button
                const MIN_IMAGE_SIZE = 18; // Min image size is 18 x 18
                const DISCLOSURE_BUTTON_HEIGHT = 35; // Including bottom border
                const DEFAULT_IMG_HEIGHT_WITH_DISCLOSURE_BUTTON = 124;
                // The provided maxHeight / maxWidth are resolved values that take into account the fixed properties height / width
                const heightConstraint = (maxHeight ? Math.min(maxHeight, DEFAULT_IMG_HEIGHT_WITH_DISCLOSURE_BUTTON)
                    : DEFAULT_IMG_HEIGHT_WITH_DISCLOSURE_BUTTON) - IMG_WS - DISCLOSURE_BUTTON_HEIGHT;
                const widthConstraint = (maxWidth ? Math.min(maxWidth, DEFAULT_IMG_WIDTH) : DEFAULT_IMG_WIDTH) - IMG_WS;

                // Thumbnail image must be at least 18 x 18
                const desiredHeight = Math.max(heightConstraint, MIN_IMAGE_SIZE);
                const desiredWidth = Math.max(widthConstraint, MIN_IMAGE_SIZE);

                // Image is scaled uniformly: If the horizontal and vertical scale differ, pick the smaller of the two
                const scaleH = desiredHeight / this._naturalHeight;
                const scaleW = desiredWidth / this._naturalWidth;
                const scale = Math.min(scaleH, scaleW);

                this._imageScaledWidth = Math.round(scale * this._naturalWidth);
                this._imageScaledHeight = Math.round(scale * this._naturalHeight);

                const h1 = this._imageScaledHeight + IMG_WS;
                const w1 = this._imageScaledWidth + IMG_WS;
                this.$.root.style = 'height: ' + h1 + 'px; width: ' + w1 + 'px;';
                this.overflow = true;
            }

            // Set fixed height / width if larger than the image's intrinsic size
            this.style.height = height > this._naturalHeight + IMG_WS ? height + 'px' : '';
            this.style.width = width > this._naturalWidth + IMG_WS ? width + 'px' : '';
        }
    }
};

customElements.define(PTCS.ModalImagePopup.is, PTCS.ModalImagePopup);
