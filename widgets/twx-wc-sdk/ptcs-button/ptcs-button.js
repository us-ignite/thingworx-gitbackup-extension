import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-icon/ptcs-icon.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';

PTCS.Button = class extends PTCS.BehaviorTabindex(PTCS.BehaviorTooltip(PTCS.BehaviorFocus(
    PTCS.BehaviorStyleable(L2Pw(LitElement))))) {
    static get styles() {
        return css`
        :host {
            user-select: none;
            -ms-user-select: none;
            position: relative;
            display: inline-flex;
            flex-direction: row;
            align-items: center;
            flex-wrap: nowrap;
            box-sizing: border-box;
          }

          :host([content-align='left']) {
            justify-content: flex-start;
          }

          :host([content-align='center']) {
            align-items: center;
            justify-content: center;
          }

          :host([content-align='right']) {
            justify-content: flex-end;
          }

          :host([aria-disabled="true"]) {
            cursor: auto;
          }

          :host([aria-disabled="false"]) {
            cursor: pointer;
          }

          [part="root"] {
            box-sizing: border-box;
            display: flex;
            align-self: center;
            align-items: center;
          }

          :host([icon-placement="right"]) [part="root"] {
              flex-direction: row-reverse;
          }

          :host([icon-placement="top"]) [part="root"] {
              flex-direction: column;
          }

          :host([icon-placement="bottom"]) [part="root"] {
              flex-direction: column-reverse;
          }

          :host([icon-placement="bottom"]) [part="label"],
          :host([icon-placement="top"]) [part="label"] {
              width: 100%;
          }

          [part="root"] {
            overflow: hidden;
            max-height: 100%;
            justify-content: center;
          }

          :host([mode="label"]) ptcs-icon {
            display: none;
          }

          [part="label"] {
              min-width: unset;
              min-height: unset;
          }`;
    }

    render() {
        return html`<div part="root">
            <ptcs-icon part="icon"
                exportparts=${this._exportparts} ?hidden=${!(this.icon || this.iconSrc || this.svgIcon)}
                .icon=${this.icon || this.iconSrc || this.svgIcon}
                .size=${this._iconSize()} .iconWidth=${this._iconWidth()} .iconHeight=${this._iconHeight()}
                .iconSet=${this.icon ? this.iconSrc : undefined} .disabled=${this.disabled}>
                </ptcs-icon>
            <ptcs-label part="label" id="label"
                .tooltip=${this.tooltip} .tooltipIcon=${this.tooltipIcon} disable-tooltip
                .label=${this.label} .multiLine=${this.multiLine} .maxNumberOfLines=${this.maxNumberOfLines}
                ?hidden=${!this.label} .horizontalAlignment=${this.contentAlign}
                .maxHeight=${this.maxHeight} .maxWidth=${this.buttonMaxWidth}
                .disclosureControl=${'ellipsis'}>
                </ptcs-label>
        </div>`;
    }

    static get is() {
        return 'ptcs-button';
    }

    static get observers() {
        return [
            '_observeVariantSize(variant, size)'
        ];
    }

    static get properties() {
        return {
            // Specifies style variant of the button.
            variant: {
                type:    String,
                value:   'primary',
                reflect: true
            },

            // The path to the .png button icon
            icon: {
                type:  String,
                value: null
            },

            // Sets a fixed width for the icon (both iconWidth and iconHeight should be set, otherwise the icon default size is set)
            iconWidth: {
                type:      String,
                attribute: 'icon-width'
            },

            // Sets a fixed height for the icon (both iconWidth and iconHeight should be set, otherwise the icon default size is set)
            iconHeight: {
                type:      String,
                attribute: 'icon-height'
            },

            // iconWidth & iconHeight have more weight than iconSize
            // This property determines the size of the icon inside the button. This property has lower priority than iconWidth and iconHeight.
            iconSize: {
                type:      String,
                attribute: 'icon-size'
            },

            // Pre-defined button sizes: 'standard' / 'medium' / 'large' / 'xl', plus responsive 'fill'
            // Determines the size of the button.
            // This property applies to all button variations except for Small Button (Variation 6).
            // If fill is selected, the button will be the size of the parent container.
            // If the builder wants further control, a custom width and height can be provided using the width and height properties.
            size: {
                type:      String,
                attribute: 'size'
            },

            // The path to the .svg button icon
            iconSrc: {
                type:      String,
                value:     null,
                attribute: 'icon-src'
            },

            // Specifies the ptcs-icon-library icon to display within the button
            svgIcon: {
                type:      String,
                attribute: 'svg-icon',
                value:     null
            },

            // Displays the icon to the left, right, above, or below the text label. (`left`\
            iconPlacement: {
                type:      String,
                attribute: 'icon-placement',
                value:     'left',
                reflect:   true
            },

            // The button label
            label: {
                type:  String,
                value: null
            },

            // The alignment of the label.
            contentAlign: {
                type:      String,
                attribute: 'content-align',
                value:     'center',
                reflect:   true
            },

            // The maximum width of the button in pixels
            buttonMaxWidth: {
                type:      Number,
                attribute: 'button-max-width',
                observer:  '_buttonMaxWidthChanged'
            },

            // Multi-line
            multiLine: {
                type:      Boolean,
                value:     false,
                attribute: 'multi-line'
            },

            maxNumberOfLines: {
                type:      Number,
                attribute: 'max-number-of-lines'
            },

            // Fixed max-height for multi-line
            maxHeight: {
                type:      String,
                attribute: 'max-height'
            },

            mode: {
                type:     String,
                computed: '_computeMode(icon, iconSrc, svgIcon, label)',
                reflect:  true
            },

            // Disables the button
            disabled: {
                type:    Boolean,
                value:   false,
                reflect: true
            },

            // FocusBehavior should simulate a click event when ArrowDown key is pressed. This is (currently)
            // only used in the Grid toolbar 'Display' button
            _arrowDownActivate: {
                type: Boolean
            },

            // ARIA attributes
            ariaDisabled: {
                type:      String,
                attribute: 'aria-disabled',
                computed:  '_disabled(disabled)',
                reflect:   true
            },

            ariaLabel: {
                type:      String,
                attribute: 'aria-label',
                computed:  '_computeAriaLabel(label, tooltip)',
                reflect:   true
            },

            ariaLabelledby: {
                type:      String,
                attribute: 'aria-labelledby',
                reflect:   true
            },

            // FocusBehavior should simulate a click event when enter key is pressed
            _enterActivate: {
                type:     Boolean,
                value:    true,
                readOnly: true
            },

            // FocusBehavior should simulate a click event when space is pressed
            _spaceActivate: {
                type:     Boolean,
                value:    true,
                readOnly: true
            },

            // Handles its own focus styling - no need for FocusBehavior to track its position
            _ownFocusStyling: {
                type:     Boolean,
                value:    true,
                readOnly: true
            },

            role: {
                type:    String,
                value:   'button',
                reflect: true
            },

            _exportparts: {
                type:     String,
                readOnly: true,
                value:    PTCS.exportparts('icon-', PTCS.Icon)
            },

            item: {
                type: Object
            }
        };
    }

    constructor() {
        super();
        this.tooltipFunc = this._monitorTooltip.bind(this);
        this.addEventListener('click', this._onClick.bind(this));
    }

    _iconSize() {
        return (this.iconWidth || this.iconHeight || this.iconSize) ? 'custom' : 'small';
    }

    _iconWidth() {
        return this.iconWidth || this.iconSize;
    }

    _iconHeight() {
        return this.iconHeight || this.iconSize;
    }

    _buttonMaxWidthChanged() {
        if (this.buttonMaxWidth) {
            const unitTest = `${this.buttonMaxWidth}`;
            if (unitTest.indexOf('px') === -1) {
                this.style.maxWidth = unitTest + 'px';
            } else {
                this.style.maxWidth = unitTest;
            }
        } else {
            this.style.removeProperty('max-width');
        }
    }

    _monitorTooltip() { // Implements ptcs-button's tooltip behavior on label truncation
        const el = this.shadowRoot.querySelector('[part=label]');

        const tooltip = el.tooltipFunc();

        if (!tooltip || tooltip === this.tooltip) {
            // If we are here it means that the label text is not truncated and it is not included in the tooltip.
            // Give it another chance. Maybe the truncation was not identified by the label because of sub-pixel difference.
            const rootEl = this.shadowRoot.querySelector('[part=root]');
            const elR = el.getBoundingClientRect();

            const paddingLeft = getComputedStyle(rootEl).paddingLeft;
            const paddingRight = getComputedStyle(rootEl).paddingRight;

            if (!paddingLeft && !paddingRight) {
                // No padding to un-restrict the label width
                return tooltip;
            }

            rootEl.style.paddingLeft = 0;
            rootEl.style.paddingRight = 0;

            const elRNew = el.getBoundingClientRect();
            rootEl.style.paddingLeft = '';
            rootEl.style.paddingRight = '';

            // TW-98495: Re-calculate the tooltip if the size *grows* when given more space
            return elRNew.width > elR.width ? el.tooltipFunc(true) : tooltip;
        }

        return tooltip;
    }

    _computeMode() {
        const iconLabel = this.label ? 'icon+label' : 'icon';
        return this.icon || this.iconSrc || this.svgIcon ? iconLabel : 'label';
    }

    _disabled(disabled) {
        return disabled ? 'true' : 'false';
    }

    _onClick(ev) {
        if (PTCS.wrongMouseButton(ev)) {
            return;
        }
        if (!this.disabled) {
            this.dispatchEvent(new CustomEvent('action', {
                // Only set the bubbles/composed options if the item is defined
                bubbles:  this.item !== undefined,
                composed: this.item !== undefined,
                detail:   {item: this.item}
            }));
        }
    }

    // All button variants except 'small' can have 'size'. The recognized sizes are reflected with corresponding theming driven by that state.
    _observeVariantSize(variant, size) {
        if (variant === 'small') {
            this.removeAttribute('size');
        } else {
            switch (size) {
                case 'standard':
                case 'medium':
                case 'large':
                case 'xl':
                case 'fill':
                    this.setAttribute('size', size);
                    break;
                default:
                    this.removeAttribute('size');
                    break;
            }
        }
    }

    // ARIA attributes
    _computeAriaLabel(label, tooltip) {
        return label || tooltip;
    }

    static get $parts() {
        if (!this._$parts) {
            this._$parts = [/* 'root', */'icon', 'label', ...PTCS.partnames('icon-', PTCS.Icon)];
        }
        return this._$parts;
    }
};


customElements.define(PTCS.Button.is, PTCS.Button);
