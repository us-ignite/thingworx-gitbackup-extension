import {LitElement, html, css} from 'lit';
import {choose} from 'lit/directives/choose.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-icons/cds-icons.js';

PTCS.ValueDisplayPopup = class extends (PTCS.BehaviorTooltip(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))))) {

    static get styles() {
        return css`
            [part=popup-container] {
              display: flex;
              align-items: center;
              justify-content: center;
              position: fixed;
              top: 0;
              left: 0;
              bottom: 0;
              right: 0;
              z-index: 99997;
            }

            [part="value-display-popup"] {
               display: flex;
               flex-direction: column;
               box-sizing: border-box;
               position: relative;
               z-index: 99998;
               pointer-events: auto;
            }

            [part="live-contents-area-popup"] {
               overflow: auto;
               display: flex;
               flex-direction: column;
            }

            [part="value-container-popup"] {
               position: relative;
               overflow: auto;
               z-index: 99998;
            }

            [part="popup-close-button-container"] {
               align-self: flex-end;
               position: fixed;
               display: flex;
               align-items: center;
               justify-content: center;
               z-index: 99998;
            }

            [part="value-display-label-popup"] {
               box-sizing: border-box;
               position: static;
               z-index: 99998;
               display: block;
            }

            [part=item-value-container] {
               display: inline-flex;
            }

            :host([label-alignment=left]) [part=item-value-container] {
               justify-content: flex-start;
            }

            :host([label-alignment=right]) [part=item-value-container] {
               justify-content: flex-end;
            }

            :host([label-alignment=center]) [part=item-value-container] {
               justify-content: center;
            }`;
    }

    render() {
        const _checkbox = () => html`<ptcs-checkbox part="item-value"
          ?checked=${this.value ? this.value !== 'false' : false} disabled></ptcs-checkbox>`;

        const _link = () => html `<ptcs-link part="item-value" variant="primary" .href=${encodeURI(this.value.href)}
                .target=${this.itemMeta.target} .disabled=${this.disabled} .label=${this.value.label}
                .singleLine=${!this.textWrap} tabindex=${this._tabSequence} disable-tooltip
             ></ptcs-link>`;

        const _textNoEllipsis = () => html`<ptcs-label part="item-value" .label=${this.value} variant="body"
               .multiLine=${this.textWrap} .disabled=${this.disabled} disable-tooltip
             ></ptcs-label>`;

        /* eslint-disable indent */
        return html`<div part="popup-container">
           <div part="value-display-popup" style=${this._computeModalSize(this.modalHeight, this.modalWidth)}>
                <div part="live-contents-area-popup">
                    <div part="popup-close-button-container">
                        <ptcs-button variant="small" id="close" part="popup-close-button"
                               icon="cds:icon_close_mini"></ptcs-button>
                    </div>
                    <ptcs-label part="value-display-label-popup" .label=${this.label}
                        variant=${this.labelVariant} multi-line .horizontalAlignment=${this.labelAlignment}></ptcs-label>
                    <div part="value-container-popup">
                        <div part="item-value-container">
                            ${choose(this.valueType, [
                                ['checkbox', () => _checkbox],
                                ['function', () => html`<span part="item-value"></span>`],
                                ['html', () => html`<span part="item-value" .innerHTML=${this.value}></span>`],
                                ['link', _link],
                                ['password', () => html`<ptcs-textfield part="item-value" text=${this.value} password read-only></ptcs-textfield>`],
                                ['text', _textNoEllipsis]
                            ])}
                        </div>
                    </div>
                </div>
            </div>
        </div >`;
    /* eslint-enable indent */
    }

    static get is() {
        return 'ptcs-value-display-popup';
    }

    static get properties() {
        return {

            // The value to display
            value: {
                type: String
            },

            itemMeta: {
                type:      Object,
                attribute: 'item-meta'
            },

            // The key label above the value
            label: {
                type: String
            },

            // Label variant (header, sub-header, label, body, ...)
            labelVariant: {
                type:      String,
                attribute: 'label-variant'
            },

            // Label Horizontal Alignment: 'left', 'center', 'right'
            labelAlignment: {
                type:      String,
                attribute: 'label-alignment',
                reflect:   true
            },

            // Data type of the value: 'text' | 'image' | ...
            valueType: {
                type:      String,
                attribute: 'value-type'
            },

            // Allow text content to wrap in the renderer?
            textWrap: {
                type:      Boolean,
                attribute: 'text-wrap'
            },

            // Max width in pixels
            maxWidth: {
                type:      Number,
                attribute: 'max-width'
            },

            // Modal pop-up dialog height in pixels
            modalHeight: {
                type:      Number,
                attribute: 'modal-height'
            },

            // Modal pop-up dialog width in pixels
            modalWidth: {
                type:      Number,
                attribute: 'modal-width'
            },

            // Modal backdrop color
            backdropColor: {
                type: String
            },

            // Modal backdrop opacity
            backdropOpacity: {
                type:      Number,
                attribute: 'backdrop-opacity'
            }
        };
    }

    _computeModalSize(modalHeight, modalWidth) {
        const w = modalWidth ? 'width:' + modalWidth + 'px;' : '';
        const h = modalHeight ? 'height:' + modalHeight + 'px;' : '';
        return h + w;
    }
};

customElements.define(PTCS.ValueDisplayPopup.is, PTCS.ValueDisplayPopup);
