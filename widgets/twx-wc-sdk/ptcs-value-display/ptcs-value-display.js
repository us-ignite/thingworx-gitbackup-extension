import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {choose} from 'lit/directives/choose.js';
import {when} from 'lit/directives/when.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';
import {closeTooltip} from 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-link/ptcs-link.js';
import 'ptcs-checkbox/ptcs-checkbox.js';
import 'ptcs-textfield/ptcs-textfield.js';
import 'ptcs-div/ptcs-focusable-div.js';
import 'ptcs-image/ptcs-image.js';
import 'ptcs-icons/cds-icons.js';
import 'ptcs-modal-image-popup/ptcs-modal-image-popup.js';
import 'ptcs-modal-overlay/ptcs-modal-overlay.js';
import './ptcs-image-value-container.js';
import './ptcs-value-display-popup.js';

PTCS.ValueDisplay = class extends PTCS.BehaviorTabindex(PTCS.BehaviorTooltip(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))))) {

    static get styles() {
        return css`
        :host
        {
            display: inline-block;
            box-sizing: border-box;
        }

        :host(:not([_value-type=image]))
        {
          overflow: auto;
        }

        :host([_fallback]:not([_default-text])) [part=overflow-control]::after {
            content: var(--ptcs-value-display-ide-string);
            display: block;
            text-align: center !important;
            width: 100%;
            font-weight: 600;
            font-size: 14px;
            order: 3;
        }

        :host([_fallback]:not([_default-text])) [part=value-container] {
            display: none;
        }

        [part=root] {
            width: 100%;
            height: 100%;
        }

        [part=value-display-area] {
            display: flex;
            flex-direction: column;
            box-sizing: border-box;
        }

        :host(:not([_image-area])) [part=overflow-control] {
            display: flex;
            flex-direction: column;
            position: relative;
            overflow: hidden;
        }

        :host([_image-area]) [part=overflow-control] {
            width: 100%;
            height: 100%;
        }

        [part=disclosure-button-overlay] {
            display: none;
        }

        [part=value-display-label] {
            display: inline-flex;
            box-sizing: border-box;
            height: fit-content;
            flex-shrink: 0;
            order: 1;
        }

        [part=value-display-label][variant=header] {
            min-height: 35px;
        }

        [part=value-container] {
            order: 2;
            overflow: visible;
            box-sizing: border-box;
        }

        [part=disclosure-button-overlay] {
            order: 3;
            display: flex;
            justify-content: flex-end;
        }

        :host([_overflow][overflow-option=disclosure]) [part=disclosure-button-overlay] {
            position: absolute;
            box-sizing: border-box;
            left: 0px;
            bottom: 0px;
            height: 34px;
            width: 100%;
            z-index: 1;
        }

        :host([_overflow][_value-type=image][image-disclosure=button]) [part=disclosure-button-overlay] {
            position: relative;
        }

        :host([_fallback][_overflow]) [part=disclosure-button-overlay] {
            display: none;
        }

        :host([_overflow]) [part=disclosure-button-container] {
          z-index: 1;
        }

        :host([_overflow][overflow-option=showmore]) [part=show-button] {
          position: absolute;
          order: 3;
          display: flex;
          justify-content: flex-end;
          left: 0px;
          bottom: 0px;
          width: 100%;
          z-index: 1;
        }

        :host([overflow-option=showmore][_show-all]) [part=show-button] {
          background-color: transparent;
          text-align: right;
          order: 3;
          display: flex;
          justify-content: flex-end;
          position: relative;
       }

        :host(:not([disabled]))  [part=show-button] {
            cursor: auto;
        }

        :host(:not([_show-all])) [part=text-link]::before {
            content: var(--ptcs-label-show-button--more, "Show More");
        }

        :host([_show-all]) [part=text-link]::before {
            content: var(--ptcs-label-show-button--less, "Show Less");
            background: transparent;
            box-shadow: none;
       }

       [part=item-value-container] {
           display: flex;
       }

       :host([_value-type=image]) [part=value-container],
       :host([_value-type=image]) [part=item-value-container] {
           justify-content: center !important;
       }

        :host([horizontal-alignment=left]) [part=item-value-container] {
            justify-content: flex-start;
        }

        :host([horizontal-alignment=right]) [part=item-value-container] {
            justify-content: flex-end;
        }

        :host([horizontal-alignment=center]) [part=item-value-container] {
            justify-content: center;
        }

        :host([horizontal-alignment=left]) [part=value-display-area] {
            text-align: left;
        }

        :host([horizontal-alignment=right]) [part=value-display-area] {
            text-align: right;
        }

        :host([horizontal-alignment=center]) [part=value-display-area] {
            text-align: center;
        }

        :host(:not([disabled]))  [part=text-link]::before {
            pointer-events: auto;
            cursor: pointer;
        }`;
    }

    render() {
        const _checkbox = () => html`<ptcs-checkbox part="item-value"
            ?checked=${this._resolvedData ? this._resolvedData !== 'false' : false} disabled></ptcs-checkbox>`;

        const _imagePopup = () => html`<ptcs-modal-image-popup part="item-value" .src=${this._resolvedData}
                  .altText=${this.defaultText} .width=${this.width} .height=${this.height}
                  .maxHeight=${this._resolvedMaxHeight} .maxWidth=${this._resolvedMaxWidth}
                  .backdropColor=${this.backdropColor} .backdropOpacity=${this.backdropOpacity}
                  @image-overflow=${this._imageOverflow}  @load=${this._onLoad}></ptcs-modal-image-popup>`;

        const _imageNoDisclosure = () => html`<ptcs-image-value-container part="item-value"
                  .src=${this._label(this.data, this.selector)} .scaling=${this.scaling} .altText=${this.defaultText}
                  .width=${this._resolvedWidth} .height=${this._resolvedHeight} @load=${this._onLoad}></ptcs-image-value-container>`;

        const _link = () => html`<ptcs-link part="item-value" variant="primary" .href=${encodeURI(this._resolvedData.href)}
                  .target=${this.itemMeta.target} .disabled=${this.disabled} .label=${this._resolvedData.label}
                  .singleLine=${!this.textWrap} .textMaximumWidth=${this._resolvedMaxWidth} tabindex=${this._tabindex}
                  disable-tooltip></ptcs-link>`;

        const _password = () => html`<ptcs-textfield part="item-value" text=${this._resolvedData} password read-only
                  no-tabindex></ptcs-textfield>`;

        const _textEllipsis = () => html`<ptcs-label part="item-value" .label=${this._resolvedData}
                  .multiLine=${this.textWrap} .maxHeight=${this._resolvedMaxHeight} .maxWidth=${this._resolvedMaxWidth}
                  .disabled=${this.disabled} .disclosureControl=${'ellipsis'} variant="body"
                  disable-tooltip></ptcs-label>`;

        const _textNoEllipsis = () => html`<ptcs-label part="item-value" .label=${this._resolvedData}
                  .multiLine=${this.textWrap} .maxWidth=${this._resolvedMaxWidth}
                  .disabled=${this.disabled} variant="body"
                  disable-tooltip></ptcs-label>`;

        const _componentLabel = () => html`<ptcs-label part="value-display-label" id="keylabel"
                  .label=${this.label} variant=${this.valueDisplayType} multi-line
                  .horizontalAlignment=${this.labelAlignment} .maxWidth=${this._resolvedMaxWidth}
                  disable-tooltip></ptcs-label>`;

        const _disclosureButtonContainer = () => html`<div part="disclosure-button-overlay" id="disclosurebuttonoverlay">
                  <div part="disclosure-button-container" @click=${this.open}>
                    <ptcs-button variant="small" id="open" part="disclosure-button" icon="cds:icon_disclosure_mini"
                        tabindex=${this._resolveTabindex()} .disabled=${this.disabled}></ptcs-button>
                  </div>
                </div>`;

        const _showMoreContainer = () => html`<div part="show-button" id="show">
                  <ptcs-focusable-div part="text-link" id="textlink" @click=${this._clickShow} @keydown=${this._keydownShowMore}
                    tabindex=${this._resolveTabindex()}></ptcs-focusable-div>
                </div>`;

        /* eslint-disable indent */
        return html`<div part="root" id="valueroot">
            <ptcs-div part="value-display-area" id="valuedisplayarea">
               <div part="overflow-control" id="overflowcontrol">
                <!-- The label above the value -->
                 ${when(this.label, _componentLabel)}
                 <div id="valuecontainer" part="value-container">
                     <div id="itemvaluecontainer" part="item-value-container">
                        <!-- The value emitted as part="item-value" in various forms, dependent on _valueType -->
                       ${when(!this.data || !this._valueType, _textEllipsis, () => html`${choose(this._valueType,
                           [
                               ['checkbox', _checkbox],
                               ['function', () => html`<span part="item-value"></span>`],
                               ['html', () => html`<span part="item-value" .innerHTML=${this._resolvedData}></span>`],
                               ['image', () => html`${when(this.imageDisclosure === 'button', _imagePopup, _imageNoDisclosure)}`],
                               ['link', _link],
                               ['password', _password],
                               ['text', () => html`${when(this.overflowOption === 'ellipsis' || this._fallback,
                                   _textEllipsis, _textNoEllipsis
                               )}`
                               ]
                           ])}`
                       )}</div>
                 </div>
                 <!-- Disclosure button for image overflow -->
                 ${when(this._overflow && this._valueType === 'image' && this.imageDisclosure === 'button', _disclosureButtonContainer)}
                 <!-- Non-image data overflow -->
                 ${when(this._overflow && this._valueType !== 'image', () => html`
                    <!-- Disclosure button or Show More link  -->
                    ${choose(this.overflowOption, [
                        ['disclosure', _disclosureButtonContainer],
                        ['showmore', _showMoreContainer]
                    ])}`
                  )}
               </div>
            </ptcs-div></div>`;
        /* eslint-enable indent */
    }

    static get is() {
        return 'ptcs-value-display';
    }

    static get properties() {
        return {

            // Input data
            data: {
                type:      Object,
                converter: {
                    toAttribute(value) {
                        const retVal = typeof value === 'object' ? JSON.stringify(value) : value;
                        return retVal;
                    },

                    fromAttribute(value) {
                        const retVal = typeof value === 'object' ? JSON.stringify(value) : value;
                        return retVal;
                    }
                },
                observer: '_dataChanged'
            },

            _resolvedData: {
                type:     Object,
                computed: '_computeResolvedData(data, selector, _fallback, defaultText)',
            },

            selector: {
                value: null
            },

            twNumberFormatToken: {
                type:      String,
                attribute: 'tw-number-format-token',
                observer:  '_twNumberFormatTokenChanged'
            },

            itemMeta: {
                type:      Object,
                attribute: 'item-meta',
                value:     {type: 'text'},
                observer:  '_itemMetaChanged'
            },

            // The key label above the value
            label: {
                type:     String,
                observer: '_determineOverflow'
            },

            // Actual height of the label above the value
            labelHeight: {
                type:      Number,
                attribute: 'label-height',
                value:     0
            },

            // Label Horizontal Alignment: 'left', 'center', 'right'
            labelAlignment: {
                type:      String,
                attribute: 'label-alignment',
                value:     'left'
            },

            // Label variant (header, sub-header, label, body)
            valueDisplayType: {
                type:      String,
                attribute: 'value-display-type',
                value:     'label'
            },

            // Horizontal Alignment within renderer
            horizontalAlignment: {
                type:      String,
                attribute: 'horizontal-alignment',
                value:     'left',
                reflect:   true
            },

            // Vertical Alignment within renderer
            verticalAlignment: {
                type:      String,
                attribute: 'vertical-alignment',
                value:     'flex-start',
                observer:  '_verticalAlignmentChanged',
                reflect:   true
            },

            // Allow text content to wrap in the renderer?
            textWrap: {
                type:      Boolean,
                attribute: 'text-wrap',
                value:     false
            },

            // Default Textual Contents (if there is no data to render)
            defaultText: {
                type:      String,
                attribute: 'default-text',
                observer:  '_defaultTextChanged'
            },

            // Height in pixels
            height: {
                type: Number
            },

            // Width in pixels
            width: {
                type: Number
            },

            // Max height in pixels
            maxHeight: {
                type:      Number,
                attribute: 'max-height'
            },

            // Smallest of maxHeight / _dynamicHeight when both exist, otherwise height minus _verticalSpacing + labelHeight
            _resolvedMaxHeight: {
                type:      Number,
                attribute: '_resolved-max-height',
                // eslint-disable-next-line max-len
                computed:  '_computeMaxHeight(height, maxHeight, _dynamicHeight, labelHeight, defaultText, _verticalSpacing, _valueType, _fallback, _imgLoaded, _overflow)'
            },

            // Height less border on part=item-value-container (only relevant for ptcs-image-value-container)
            _resolvedHeight: {
                type:      Number,
                attribute: '_resolved-height'
            },

            // Max width in pixels
            maxWidth: {
                type:      Number,
                attribute: 'max-width'
            },

            // Smallest of maxWidth / width MINUS _horizontalSpacing
            _resolvedMaxWidth: {
                type:      Number,
                attribute: '_resolved-max-width',
                computed:  '_computeMaxWidth(width, maxWidth, _dynamicWidth, defaultText, _horizontalSpacing, _valueType, _imgLoaded, _overflow)'
            },

            // Width less border on part=item-value-container (only relevant for ptcs-image-value-container)
            _resolvedWidth: {
                type:      Number,
                attribute: '_resolved-width'
            },

            // Image loaded successfully?
            _imgLoaded: {
                attribute: '_img-loaded',
                type:      Boolean
            },

            // Image's intrinsic width
            _imgNaturalWidth: {
                type:      Number,
                attribute: '_img-natural-width'
            },

            // Image's intrinsic height
            _imgNaturalHeight: {
                type:      Number,
                attribute: '_img-natural-height'
            },

            // Modal pop-up dialog height in pixels
            modalHeight: {
                type:      Number,
                attribute: 'modal-height',
                value:     380
            },

            // Modal pop-up dialog width in pixels
            modalWidth: {
                type:      Number,
                attribute: 'modal-width',
                value:     600
            },

            disabled: {
                type:    Boolean,
                value:   false,
                reflect: true
            },

            // Modal backdrop color
            backdropColor: {
                type:      String,
                attribute: 'backdrop-color'
            },

            // Modal backdrop opacity
            backdropOpacity: {
                type:      Number,
                attribute: 'backdrop-opacity'
            },

            // Controls whether to show disclosure button (default), horizontal ellipsis, or 'Show More' on overflow
            overflowOption: {
                type:      String,
                attribute: 'overflow-option',
                reflect:   true,
                value:     'disclosure' // 'disclosure' | 'ellipsis' | 'showmore'
            },

            // Resolved overflow option
            _overflowOption: {
                type:      String,
                computed:  '_computeOverflowOption(overflowOption, _valueType, _fallback)',
                attribute: '_overflow-option'
            },

            // Controls whether to show disclosure button (default) on image thumbnail
            imageDisclosure: {
                type:      String, // 'none' | 'button',
                attribute: 'image-disclosure',
                reflect:   true,
                value:     'button'
            },

            // Data type of the value: 'text' | 'image' | ...
            _valueType: {
                type:      String,
                attribute: '_value-type',
                reflect:   true,
                computed:  '_computeType(itemMeta)'
            },

            // Toggle to show or hide the modal pop-up dialog
            _showpopup: {
                type:    Boolean,
                reflect: true
            },

            // State of the show more / show less. When true, we are showing all and display 'Show Less' link.
            _showAll: {
                type:        Boolean,
                attribute:   '_show-all',
                observer:    '_showAllChanged',
                observeWhen: 'immediate',
                value:       false,
                reflect:     true
            },

            // To keep track of size change
            _resizeObserver: {
                type:      ResizeObserver,
                attribute: '_resize-observer'
            },

            // Set when the value height exceeds the allotted display area height
            _overflow: {
                type:    Boolean,
                reflect: true
            },

            // Set when the part item-value label is truncated (to make it Tab reachable and see the truncation tooltip)
            _labelIsTruncated: {
                type:      Boolean,
                attribute: '_label-is-truncated'
            },

            // Set if no data to display; we may have defaultText to show
            _fallback: {
                type:     Boolean,
                reflect:  true,
                observer: '_fallbackChanged',
                computed: '_showFallback(data, selector)'
            },

            // Boolean to turn off the dynamic size constraints logic (that was originally added to make a responsive component)
            noDynamicSizeConstraint: {
                type:      Boolean,
                attribute: 'no-dynamic-size-constraint'
            },

            // Enable dynamic size constraints?
            responsiveLayout: {
                type:      Boolean,
                attribute: 'responsive-layout'
            },

            // Height set dynamically, via the resizeObserver
            _dynamicHeight: {
                type:     Number,
                observer: '_updateWidgetConstraints'
            },

            // Width set dynamically, via the resizeObserver
            _dynamicWidth: {
                type: Number
            },

            // Horizontal padding around part overflow-control (reduces the available width in which to display the value or image)
            _horizontalSpacing: {
                type:      Number,
                attribute: '_horizontal-spacing'

            },

            // Vertical padding around part overflow-control (reduces the available height in which to display the value or image)
            _verticalSpacing: {
                type:      Number,
                attribute: '_vertical-spacing'
            },

            // Initial background color for the value display
            _bgdColor: {
                type: String
            },

            // Initial background color of disclosure container (this.$.show or this.$.disclosurebuttonoverlay, depending on overflowOption)
            _containerBgdColor: {
                type: String
            },

            // Initial box-shadow for the Show More / Show Less container this.$.show
            _boxShadow: {
                type: String
            },

            // Flag set when _bgdColor, _containerBgdColor, and _boxShadow have been stored
            _showStylingUpdated: {
                type: Boolean
            },

            _delegatedFocus: {
                type:      String,
                attribute: '_delegated-focus',
                value:     null
            },

            // To give externally assigned tabindex value priority over default tabindex
            _tabSequence: {
                type:      String,
                attribute: '_tab-sequence'
            },

            // Initial tabindex assignment, with fallback '0'
            _tabindex: {
                type: String
            },

            // Only used when imageDisclosure is set to none
            scaling: {
                type: String
            },

            // Flag for when imageDisclosure is set to none
            _imageArea: {
                type:      Boolean,
                attribute: '_image-area',
                computed:  '_computeImageArea(imageDisclosure, itemMeta)',
                reflect:   true
            }
        };
    }

    static get observers() {
        return [
            '_observeVariables(_showAll, width, height, maxWidth, _dynamicWidth, maxHeight, _valueType, data, defaultText, overflowOption, textWrap)',
            '_updateTabindexState(_overflow, _labelIsTruncated, _valueType)',
            '_toggleDescendantsFocus(noTabindex, data)',
            '_scrollbarFlicker(_valueType, textWrap, _resolvedMaxHeight)'
        ];
    }

    constructor() {
        super();
        this._tabindex = this._tabSequence || this.getAttribute('tabindex') || '0';
    }

    ready() {
        super.ready();

        this.tooltipFunc = this._monitorTooltip.bind(this);
        this.$.overflowcontrol.tooltipFunc = this._monitorTooltip.bind(this);
        this.$.overflowcontrol.tooltipIconFunc = () => this.tooltipIcon || '';
        this._trackFocus(this, this.$.overflowcontrol);
        this._untrackHover(this);
        this._trackHover(this.$.overflowcontrol);
        // Listen to keypress events when the 'Show More' "button" has focus...
        this.addEventListener('keypress', ev => {
            const key = ev.which || ev.keyCode;
            if (key === 32 || key === 13) {
                this._activateVD();
                ev.preventDefault();
            }
        });

        // This is dispatched from the Property Display when <space> has been pressed (since Edge
        // has issues with passing on the "real" KeyboardEvent)
        this.addEventListener('space-activate', ev => {
            this._activateVD();
            ev.preventDefault();
        });

        if (this.hasAttribute('property-display-item')) {
            this.noTabindex = true;
        }
        this._labelIsTruncated = false;

        // Render focus only on keyboard navigation
        PTCS.renderFocusOnClick(false);
    }

    connectedCallback() {
        super.connectedCallback();
        this.__resizeWindowCb = this._resizeWindow.bind(this);
        window.addEventListener('resize', this.__resizeWindowCb);
        this._resizeObserver = new ResizeObserver(entries => {

            requestAnimationFrame(() => { // to avoid ResizeObserver loop limit exceeded, it stops UTs
                // Piggy-backing on the resizeObserver to retrieve the padding on this.$.overflowcontrol as
                // the styling is applied with a delay
                const overflowcontrol = this.shadowRoot.getElementById('overflowcontrol');
                if (overflowcontrol) {
                    const overflowcontrolCS = window.getComputedStyle(overflowcontrol);
                    this._verticalSpacing = PTCS.cssDecodeSize(overflowcontrolCS.paddingTop) + PTCS.cssDecodeSize(overflowcontrolCS.paddingBottom);
                    this._horizontalSpacing = PTCS.cssDecodeSize(overflowcontrolCS.paddingLeft) + PTCS.cssDecodeSize(overflowcontrolCS.paddingRight);
                }
                if (this._valueType !== undefined && this._valueType !== 'image') {
                    if (this.responsiveLayout) {
                        // Dynamic size constraints applied on non-image data when in responsiveLayout, monitored by _observeVariables
                        this._dynamicHeight = entries[0].contentRect.height;
                        this._dynamicWidth = entries[0].contentRect.width;
                    } else {
                        // Debounce the overflow determination, as the component size *has* changed
                        clearTimeout(this.__overflowTimeoutId);
                        this.__overflowTimeoutId = setTimeout(() => {
                            this._determineOverflow();
                        }, 50);
                    }
                } else if (this._valueType === 'image' && this.imageDisclosure === 'none' && this.responsiveLayout) {
                    this._dynamicHeight = entries[0].contentRect.height;
                    this._dynamicWidth = entries[0].contentRect.width;
                }
            });
        });
        this._resizeObserver.observe(this);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        window.removeEventListener('resize', this.__resizeWindowCb);
        this._resizeObserver.unobserve(this);
        if (this._dialog) {
            document.body.removeChild(this._dialog);
        }
        const mdl = document.body.querySelector('ptcs-modal-overlay');
        if (mdl) {
            document.body.removeChild(mdl);
        }
    }

    // Invoked by Lit when the DOM has been rendered
    updated(changedProperties) {
        super.updated(changedProperties);
        if (changedProperties.has('overflowOption')) {
            // We must make sure we re-calculate these props if we switch overflow mode
            this._showStylingUpdated = false;
        }
        if (this._overflow && this.overflowOption === 'showmore' && !this._showStylingUpdated) {
            // Show More / Show Less disclosure: Store the initial values in private properties so that they can be
            // restored in _showAllChanged when link is toggled
            if (!this._bgdColor) {
                this._bgdColor = window.getComputedStyle(this).getPropertyValue('background-color');
            }
            const container = this.shadowRoot.getElementById('show');
            if (container) {
                const showCS = window.getComputedStyle(container);
                if (!this._containerBgdColor) {
                    this._containerBgdColor = showCS.getPropertyValue('background-color');
                }
                if (!this._boxShadow) {
                    this._boxShadow = showCS.getPropertyValue('box-shadow');
                }
                // Need to invoke _showAllChanged once after the styling info has been stored, to set the box-shadow color
                if (this._containerBgdColor && this._containerBgdColor && this._boxShadow) {
                    this._showStylingUpdated = true;
                    this._showAllChanged(this._showAll);
                }
            }
        }
        // Disclosure Button: Match the box-shadow color of the container to its background color
        if (this._overflow && this.overflowOption === 'disclosure' && !this._showStylingUpdated) {
            const container = this.shadowRoot.getElementById('disclosurebuttonoverlay');
            if (container) {
                const contCS = window.getComputedStyle(container);
                const boxShadow = contCS.getPropertyValue('box-shadow');
                this._containerBgdColor = contCS.getPropertyValue('background-color');
                if (boxShadow.startsWith('rgb')) {
                    container.style.boxShadow = this._containerBgdColor + boxShadow.substring(boxShadow.indexOf(')') + 1);
                    this._showStylingUpdated = true;
                }
            }
        }
        this._determineOverflow();
    }

    // Complement the resize observer to monitor size changes of the browser window: When you resize the window of a
    // mashup, the component resizeObserver is not getting invoked because the value display itself is not being resized,
    // but Mashup-Builder resizes its container, which imposes size constraints for a responsive behavior.
    _resizeWindow() {
        clearTimeout(this._debounceResizeWindowTimeoutId);
        this._debounceResizeWindowTimeoutId = setTimeout(() => {
            // Reset 'Show Less' to 'Show More' on window resize
            if (this._showAll) {
                this._showAll = false;
            }
            const el = this.shadowRoot.querySelector('[part=item-value]');
            if (el && this._valueType === 'text' && this.textWrap) {
                const label = el.shadowRoot.getElementById('label');
                const labelRoot = el.shadowRoot.getElementById('root');
                if (labelRoot.classList.contains('sl') &&
                    this._resolvedMaxHeight > 2 * PTCS.cssDecodeSize(getComputedStyle(label).fontSize, label, true)) {
                    // ptcs-label switches multi-line display to single line when the label height has shrunk too much for
                    // multi-line content but is not responsive to container size change like the value display. Restore the
                    // ptcs-label multi-line class 'ml' and remove the single line class 'sl' on window resize when there is
                    // again enough height for more than one line.
                    el.multiLine = this.textWrap;
                    labelRoot.classList.remove('sl');
                    labelRoot.classList.add('ml');
                }
            }
            this._updateWidgetConstraints();
            this._determineOverflow();
        }, 50);
    }

    _activateVD() {
        if (!this.disabled) {
            if (this._valueType === 'image') {
                const elPopup = this.shadowRoot.querySelector('ptcs-modal-image-popup');
                if (elPopup) {
                    elPopup.open();
                }
            } else if (this._valueType === 'link') {
                const elLink = this.shadowRoot.querySelector('ptcs-link');
                if (elLink) {
                    const elA = elLink.shadowRoot.querySelector('[part=label]');
                    if (elA) {
                        elA.click();
                    }
                }
            } else if (this._overflow) {
                if (this.overflowOption === 'showmore') {
                    requestAnimationFrame(closeTooltip);
                    this._showAll = !this._showAll;
                } else if (this.overflowOption === 'disclosure') {
                    // This is the equivalent of a click on the disclosure button
                    this.open();
                }
            }
        }
    }

    _showAllChanged(_showAll) {
        // The background color of the component and the Show More container can be customized and are exposed in Style Properties
        // tab, but the box-shadow is not. Synchronize background and box-shadow color as the Show More / Show Less link is clicked.
        // In expanded "Show Less" state the link container this.$.show has transparent color and no box-shadow, in the collapsed
        // "Show More" state it has a box-shadow whose color should match that of the background.
        if (this._showStylingUpdated) {
            const show = this.shadowRoot.getElementById('show'); // Lit renders show div on overflow
            if (show && _showAll) {
                // "Show Less" (expanded) state. Inherit the background color and remove box-shadow.
                show.style.backgroundColor = 'inherit';
                show.style.boxShadow = 'none';
            } else {
                // "Show More" state
                const inheritColor = 'rgba(0, 0, 0, 0)';
                if (this._boxShadow.startsWith('rgb')) {
                    if (this._bgdColor !== inheritColor && this._containerBgdColor === inheritColor) {
                        // Component has custom background color and  Show More container is inheriting its color from the component
                        show.style.backgroundColor = this._bgdColor;
                        show.style.boxShadow = this._bgdColor + this._boxShadow.substring(this._boxShadow.indexOf(')') + 1);
                    } else {
                        // Restore the Show More container's initial background assignment and set the box shadow color to match
                        show.style.backgroundColor = this._containerBgdColor;
                        show.style.boxShadow = this._containerBgdColor + this._boxShadow.substring(this._boxShadow.indexOf(')') + 1);
                    }
                }
            }
        }
    }

    _toggleDescendantsFocus(noTabindex) {
        if (this.hasAttribute('property-display-item')) {
            requestAnimationFrame(() => {
                this.shadowRoot.querySelectorAll(noTabindex ? '[tabindex]' : '[no-tabindex]').forEach(el => {
                    el.noTabindex = noTabindex;
                    if (noTabindex) {
                        el.setAttribute('no-tabindex', '');
                    } else {
                        el.removeAttribute('no-tabindex');
                    }
                });
            });
        }
    }

    _monitorTooltip() { // Implements ptcs-value-display's tooltip behavior on truncation
        const el = this.shadowRoot.querySelector('[part=item-value]');
        if (el && typeof el.tooltipFunc === 'function') { // Does the container have a function to deliver the tooltip contents?
            const containerTooltip = el.tooltipFunc();
            if (containerTooltip) {
                if (this.tooltip) {
                    if (this.tooltip !== this.label) {
                        return containerTooltip + '\n\n' + this.tooltip;
                    }
                }
                return containerTooltip;
            }
        }
        // Default to element tooltip proper
        if (this.tooltip !== this.label) {
            return this.tooltip || '';
        }
        return '';
    }

    _dataChanged(data) {
        if (!data) {
            this._showAll = false;
            this._overflow = false;
        }
    }

    _showFallback(data, selector) {
        return (this._label(data, selector) === '');
    }

    _fallbackChanged(val) {
        if (val) {
            this._showAll = false;
            requestAnimationFrame(() => {
                this._determineOverflow();
            });
        }
    }

    _defaultTextChanged(val) {
        // Set Boolean attribute _default-text when there is a fallback text defined
        PTCS.setbattr(this, '_default-text', !!val);
    }

    _determineOverflow() {
        const valuecontainer = this.$.valuecontainer;
        if (!valuecontainer) {
            return;
        }

        if (this.$.valueroot.scrollHeight > 0) { // Ready?
            const keylabel = this.shadowRoot.getElementById('keylabel');
            this.labelHeight = keylabel ? keylabel.scrollHeight : 0;

            if (this._valueType !== 'image') {
                // Image overflow is handled in ptcs-modal-image-popup, emitting event image-overflow that assigns this._overflow
                const valuecontainerHeight = valuecontainer.querySelector('[part=item-value-container]').scrollHeight;
                const valuedisplayarea = this.shadowRoot.getElementById('valuedisplayarea');
                const heightconstraint = Math.min(this.maxHeight, this.height) ||
                    Math.min(this.maxHeight, this._dynamicHeight) ||
                    this.maxHeight || this.height || this._dynamicHeight;
                // Only state-formatted content (TW-101640) or value display of property display (TW-102387) is allowed to extend
                // into the bottom padding without causing overflow whereas non-state-formatted content should trigger overflow (TW-106511)
                this._overflow = (valuedisplayarea.hasAttribute('ptcstate-valueformat') || this.hasAttribute('property-display-item'))
                    ? (valuecontainerHeight + this.labelHeight) > heightconstraint
                    : (valuecontainerHeight + this.labelHeight + this._verticalSpacing + 1) > heightconstraint;
                if (!this._overflow && this.overflowOption === 'showmore' && this._showAll) {
                    // Reset _showAll when no overflow, so that VD does not resume in a "Show Less" state on a subsequent overflow
                    this._showAll = false;
                }
            }

            if (this._resolvedData && this._valueType === 'text') {
                // part=item-value is ptcs-label
                const el = valuecontainer.querySelector('[part=item-value]');
                if (el) {
                    el.updateComplete.then(() => {
                        // The multiline ellipsis truncation needs some time to run. Resolution of _labelIsTruncated is not
                        // time critical as it is used to make the element Tab reachable and see truncation tooltip.
                        setTimeout(() => {
                            this._labelIsTruncated = el.isTruncated();
                        }, 50);
                    });
                }
            }
        }
    }

    _updateWidgetConstraints() {
        if (!this._valueType) {
            return;
        }
        const valuedisplayarea = this.$.valuedisplayarea;
        if (!valuedisplayarea) {
            return;
        }
        if (!this.isIDE) {
            if (this._valueType !== 'image') {
                valuedisplayarea.style.height = this.height > 0 ? this.height + 'px' : '100%';
                valuedisplayarea.style.width = this.width > 0 ? this.width + 'px' : '100%';
                valuedisplayarea.style.maxHeight = this.maxHeight > 0 ? this.maxHeight + 'px' : '';
                valuedisplayarea.style.maxWidth = this.maxWidth > 0 ? this.maxWidth + 'px' : '';
            } else if (this._valueType === 'image' && this.imageDisclosure === 'button') {
                valuedisplayarea.style.minHeight = this.height > 0 ? this.height + 'px' : '';
                valuedisplayarea.style.minWidth = this.width > 0 ? this.width + 'px' : '';
                valuedisplayarea.style.height = '';
                valuedisplayarea.style.width = '';
                valuedisplayarea.style.maxHeight = '';
                valuedisplayarea.style.maxWidth = '';
            } else if (this._valueType === 'image' && this.imageDisclosure === 'none') {
                valuedisplayarea.style.height = this.height ? this.height + 'px' : '100%';
                valuedisplayarea.style.width = this.width ? this.width + 'px' : '100%';
                valuedisplayarea.style.maxHeight = '';
                valuedisplayarea.style.maxWidth = '';
            }
        }
    }

    _updateTabindexState(_overflow, _labelIsTruncated, _valueType) {
        if (this.hasAttribute('property-display-item')) {
            return;
        }
        // On overflow, label truncation, or link, the content should be Tab reachable because it is "interactive"
        this.setAttribute('tabindex', (_overflow || _labelIsTruncated || _valueType === 'link') ? this._tabindex : '-1');
    }

    // Observer monitors more variables than it uses itself to be invoked whenever the value is affected somehow
    _observeVariables(_showAll, width, height, maxWidth, _dynamicWidth, maxHeight, _valueType, data, defaultText, overflowOption, textWrap) {
        if (!this.isIDE) {
            if (_valueType !== '' || defaultText) { // Do we have a data binding or defaultText?
                if (_showAll) { // We are showing all
                    // Remove height constraints on the value display area container to allow it to be shown in full
                    this.$.valuedisplayarea.style.maxHeight = '';
                    this.$.valuedisplayarea.style.height = '';
                } else { // Resetting to state 'Show More' (if applicable)
                    this._updateWidgetConstraints();
                }
                if (_valueType === 'image') {
                    if (maxWidth > 0 && width > 0) {
                        this.style.maxWidth = Math.min(width, maxWidth) + 'px';
                    } else if (maxWidth > 0) {
                        this.style.maxWidth = maxWidth + 'px';
                    } else if (width) {
                        this.style.maxWidth = width + 'px';
                    }
                }
                // Link should be Tab navigable regardless of overflow
                if (!this.noTabindex && _valueType === 'link') {
                    this.setAttribute('tabindex', this._tabindex);
                }

                // Debounce the overflow determination
                clearTimeout(this.__observeVariablesTimeoutId);
                this.__observeVariablesTimeoutId = setTimeout(() => {
                    this._determineOverflow();
                }, 30);
            }
        }
    }

    _computeMaxWidth(width, maxWidth, _dynamicWidth, defaultText, _horizontalSpacing, _valueType, _imgLoaded, _overflow) {
        if ((_valueType !== 'image' && !this._resolvedData) || (_valueType === 'image' && !_imgLoaded)) {
            // Data is not ready yet
            return undefined;
        }

        // Is overflowcontrol rendered yet?
        if (isNaN(this._verticalSpacing) || isNaN(this._horizontalSpacing)) {
            return undefined;
        }

        let widthConstraint = 0;
        if (maxWidth > 0) {
            if (width > 0) {
                widthConstraint = Math.min(maxWidth, width);
            } else {
                widthConstraint = maxWidth;
            }
        } else {
            widthConstraint = width;
        }

        if (!isNaN(widthConstraint)) {
            widthConstraint -= this._horizontalSpacing;
        }

        // Determine _resolvedWidth used by ptcs-image-value-container. Adjust width to accommodate the
        // space of a legacy styles' border
        const itemValue = this.shadowRoot.querySelector('[part=item-value]');
        if (itemValue) {
            // _dynamicWidth to fit the image within the widget dimensions
            const _dynamicW = _dynamicWidth > 1 ? _dynamicWidth - 1 : undefined;
            const imageWidth = width || widthConstraint || _dynamicW || this._imgNaturalWidth;
            const itemValueCS = window.getComputedStyle(itemValue);
            const borderWidth = PTCS.cssDecodeSize(itemValueCS.borderLeftWidth) +
                PTCS.cssDecodeSize(itemValueCS.borderRightWidth);
            this._resolvedWidth = imageWidth > borderWidth ? imageWidth - borderWidth : imageWidth;
        }
        return widthConstraint > 0 ? widthConstraint : undefined;
    }

    // ptcs-modal-image-popup loaded image successfully
    _onLoad(ev) {
        this.setProperties({_imgLoaded: true, _imgNaturalWidth: ev.detail.naturalWidth, _imgNaturalHeight: ev.detail.naturalHeight});
        ev.stopPropagation();
    }

    // This is dispatched from the ptcs-modal-image-popup to report an overflow condition in order to show the disclosure button (when applicable)
    _imageOverflow(ev) {
        this._overflow = ev.detail.overflow;
        ev.stopPropagation();
    }

    _computeMaxHeight(height, maxHeight, _dynamicHeight, labelHeight, defaultText, _verticalSpacing, _valueType, _fallback, _imgLoaded, _overflow) {
        if ((_valueType !== 'image' && !this._resolvedData) || (_valueType === 'image' && !_imgLoaded)) {
            // Data is not ready yet
            return undefined;
        }

        // Is overflowcontrol rendered yet?
        if (isNaN(this._verticalSpacing) || isNaN(this._horizontalSpacing)) {
            return undefined;
        }

        let heightConstraint = 0;
        if (maxHeight > 0) {
            if (height > 0) {
                heightConstraint = Math.min(maxHeight, height);
            } else if (this.noDynamicSizeConstraint) {
                heightConstraint = maxHeight;
            } else {
                heightConstraint = _dynamicHeight && this._valueType !== 'image' ? Math.min(maxHeight, _dynamicHeight) : maxHeight;
            }
        } else if (_dynamicHeight > 0 && !this.noDynamicSizeConstraint && this._valueType !== 'image') {
            heightConstraint = height > 0 ? height : _dynamicHeight;
        } else {
            heightConstraint = height;
        }
        if (!isNaN(heightConstraint)) {
            heightConstraint -= this._verticalSpacing;
            heightConstraint = labelHeight ? heightConstraint - labelHeight : heightConstraint;
        }

        // Determine _resolvedHeight used by ptcs-image-value-container. Adjust height to accommodate the
        // space of a legacy styles' border
        const itemValue = this.shadowRoot.querySelector('[part=item-value]');
        const scrollBarAdjustmentHeight = 4;
        if (itemValue) {
            // _dynamicHeight to fit the image within the widget dimensions
            // scrollBarAdjustmentHeight is to prevent vertical scrollbars appearing from TWX rendering of the image
            const _dynamicH = _dynamicHeight > scrollBarAdjustmentHeight ? _dynamicHeight - scrollBarAdjustmentHeight : undefined;
            const imgHeight = height || heightConstraint || _dynamicH || this._imgNaturalHeight;
            const imageHeight = imgHeight > this.labelHeight ? imgHeight - this.labelHeight : imgHeight;
            const itemValueCS = window.getComputedStyle(itemValue);
            const borderWidth = PTCS.cssDecodeSize(itemValueCS.borderTopWidth) +
                PTCS.cssDecodeSize(itemValueCS.borderBottomWidth);
            this._resolvedHeight = imageHeight > borderWidth ? imageHeight - borderWidth : imageHeight;
        }
        return heightConstraint > 0 ? heightConstraint : undefined;
    }

    _itemMetaChanged(meta) {
        this._updateFormattingByBaseType();
    }

    _twNumberFormatTokenChanged() {
        this.itemMeta._isFormatted = false;
        this.selector = null;

        this._updateFormattingByBaseType();
    }

    _computeType(meta) {
        if (!meta) {
            return '';
        }
        if (meta.type) {
            return meta.type;
        }
        if (!meta.baseType) {
            return '';
        }

        if (!meta.formatterStruct) {
            meta.formatterStruct = {renderer: meta.baseType};
            if (this.meta) {
                this.meta = meta.formatterStruct;
            }
        }
        meta.type = PTCS.Formatter.getContainerType(meta.baseType, meta.formatterStruct);
        if (this.meta) {
            this.meta.type = meta.type;
        }
        return meta.type;
    }

    open() {
        if (!this.disabled) {
            if (this._valueType === 'image') {
                const valuecontainer = this.$.valuecontainer;
                const elPopup = valuecontainer.querySelector('ptcs-modal-image-popup');
                if (elPopup) {
                    elPopup.open();
                }
            } else if (!this._showpopup) {
                // Create the modal overlay backdrop as child of body
                this.__modalOverlay = this.__modalOverlay || document.createElement('ptcs-modal-overlay');
                this.__modalOverlay.backdropColor = this.backdropColor;
                this.__modalOverlay.backdropOpacity = this.backdropOpacity;
                document.body.appendChild(this.__modalOverlay);

                // Create the ptcs-value-display-popup dynamically each time the disclosure button is clicked
                const popup = document.createElement('ptcs-value-display-popup');
                popup.modalHeight = this.modalHeight;
                popup.modalWidth = this.modalWidth;
                popup.label = this.label;
                popup.value = this._label(this.data, this.selector);
                popup.valueType = this._valueType;
                popup.itemMeta = this.itemMeta;
                popup.textWrap = this.textWrap;
                popup.labelVariant = this.valueDisplayType;
                popup.backdropColor = this.backdropColor;
                popup.backdropOpacity = this.backdropOpacity;
                popup.labelAlignment = this.labelAlignment;

                this._dialog = document.body.appendChild(popup);
                this._showpopup = true;

                // Lit render needs a bit of time before the dialog can be styled
                requestAnimationFrame(() => {
                    const vcp = this._dialog.shadowRoot.querySelector('[part=value-container-popup]');
                    console.assert(vcp !== null, 'vcp is not null');
                    const style = window.getComputedStyle(vcp);
                    const popupHorizontalPadding = PTCS.cssDecodeSize(style.paddingLeft) + PTCS.cssDecodeSize(style.paddingRight);
                    this._dialog.maxWidth = this.modalWidth - popupHorizontalPadding;

                    // Copy custom styling of value from the inline part item-value to the popup item-value
                    const iiv = this.shadowRoot.querySelector('#valuecontainer').querySelector('[part=item-value]');
                    const cs = window.getComputedStyle(iiv);
                    const iv = this._dialog.shadowRoot.querySelector('[part=value-container-popup]').querySelector('[part=item-value]');

                    console.assert(iv !== null, 'iv is not null');

                    // The below 2 lines are done differently because of a special FF behavior
                    iv.style.backgroundColor = cs.backgroundColor;
                    iv.style.backgroundImage = cs.backgroundImage;

                    iv.style.color = cs.getPropertyValue('color');
                    iv.style.fontFamily = cs.getPropertyValue('font-family');
                    iv.style.fontSize = cs.getPropertyValue('font-size');
                    iv.style.fontStyle = cs.getPropertyValue('font-style');
                    iv.style.fontWeight = cs.getPropertyValue('font-weight');
                    iv.style.letterSpacing = cs.getPropertyValue('letter-spacing');
                    iv.style.lineHeight = cs.getPropertyValue('line-height');
                    iv.style.textDecoration = cs.getPropertyValue('text-decoration');

                    // State formatting can display an image next to the value. This image should be displayed in popup as well.
                    const beforeCS = window.getComputedStyle(iiv, ':before');

                    if (beforeCS.content && beforeCS.content !== 'none') {
                        const imgDiv = document.createElement('div');
                        imgDiv.style.content = beforeCS.content;
                        imgDiv.style.paddingRight = beforeCS.paddingRight;

                        iv.parentElement.insertBefore(imgDiv, iv);
                        iv.parentElement.style.display = 'inline-flex';
                        iv.parentElement.style.backgroundColor = cs.backgroundColor;
                        iv.parentElement.style.backgroundImage = cs.backgroundImage;
                        iv.parentElement.style.alignItems = 'center';

                        iv.style.background = 'transparent';
                    }
                });

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
                    this._dialog.shadowRoot.querySelector('[part=popup-close-button-container]').addEventListener('click', () => this.close());
                });
            }
        }
    }

    close() {
        if (this._showpopup) {
            // Remove the popup from DOM
            this._dialog.remove();
            this._dialog = undefined;
            // Remove the modal background overlay from DOM
            const mdl = document.body.querySelector('ptcs-modal-overlay');
            mdl.remove();
            this._showpopup = false;

            // Emit popup-close-action event
            this.dispatchEvent(new CustomEvent('popup-close-action'), {
                bubbles:  true,
                composed: true
            });

            // Remove the "global" event listener for the "modal" popup
            document.removeEventListener('keydown', this._captureTab);

            // Restore focus to "main" part of the component
            if (this.__prevFocusElt) {
                this.__prevFocusElt.focus();
                this.__prevfocusElt = undefined;
            }
        }
    }

    _clickShow() {
        if (!this.disabled) {
            this._showAll = !this._showAll;
            requestAnimationFrame(closeTooltip);
        }
    }

    _keydownShowMore(ev) {
        // Is  the VD is a property-display-item? The PD handles the keyboard navigation between its VD items.
        if (this.hasAttribute('property-display-item')) {
            // On arrow navigation *away* from the VD when Show More / Show Less link has focus, route the key event to
            // the owner PD by explicitly blurring the ptcs-focusable-div and putting the focus on the PD. The use case
            // scenario is a mashup with several PDs with a Show More link randomly being clicked on (not reached via a
            // previous keyboard navigation), and the VD losing focus because of arrow or Escape key.
            const parentOf = el => ((el.nodeType === 11 && el.host) ? el.host : el.parentNode);
            let vd = parentOf(this);
            switch (ev.key) {
                case 'Escape':
                case 'ArrowUp':
                case 'ArrowLeft':
                case 'ArrowDown':
                case 'ArrowRight': {
                    ev.target.blur();
                    while (vd && vd.nodeName !== 'PTCS-PROPERTY-DISPLAY') {
                        vd = parentOf(vd);
                    }
                    vd.focus();
                    break;
                }
            }
        }
    }

    _verticalAlignmentChanged(verticalAlignment) {
        this.$.valuedisplayarea.style.justifyContent = verticalAlignment;
    }

    _label(item, selector) {
        if (item === null || item === '' || item === undefined) {
            return '';
        }

        let retLabel = '';
        if (!selector) {
            retLabel = item;
        } else if (typeof selector === 'string') {
            retLabel = item[selector];
        } else if (selector.constructor && selector.call && selector.apply) {
            retLabel = selector(item);
        } else {
            console.error('Invalid ptcs-value-display value selector', selector);
        }

        if (retLabel === undefined || retLabel === null) {
            retLabel = '';
        } else if ((!this.itemMeta || (this.itemMeta.type !== 'link' && this.itemMeta.type !== 'function')) && typeof retLabel !== 'string') {
            retLabel = retLabel.toString();
        }

        return retLabel;
    }

    _updateFormattingByBaseType() {
        const meta = this.itemMeta;

        if (!meta || meta._isFormatted || !meta.baseType) {
            return;
        }
        meta._isFormatted = true;

        meta.formatterStruct = meta.formatterStruct || {renderer: meta.baseType};
        meta.formatterStruct.numberFormatString = this.twNumberFormatToken ? '[[' + this.twNumberFormatToken + ']]' : this.twNumberFormatToken;

        const formattingInfo = PTCS.Formatter.getFormaterFunc(meta.baseType, this.selector, meta.formatterStruct);
        if (typeof formattingInfo === 'function') {
            this.selector = formattingInfo;
        } else if (formattingInfo) {
            _.forEach(formattingInfo, (value, key) => {
                if (typeof value === 'function') {
                    this.selector = value;
                } else {
                    meta[key] = value;
                }
            });
            this._determineOverflow();
        } else {
            // console.log('WARN: ptcs-value-display: Unknown formatter type: ' + meta.baseType);
        }
    }

    _computeOverflowOption(overflowOption, _valueType, _fallback) {
        if (_fallback) {
            // Force ellipsis truncation for fallback text regardless of overflowOption value
            return 'ellipsis';
        }
        if (_valueType === 'image' || overflowOption === 'ellipsis' && _valueType !== 'text') {
            // Ellipsis truncation is only supported for text where ptcs-label does the truncation. Image has disclosure button by default.
            return 'disclosure';
        }
        return overflowOption;
    }

    _computeResolvedData(data, selector, _fallback, defaultText) {
        return this._label(data, selector) || defaultText || '';
    }

    _computeImageArea(imageDisclosure, itemMeta) {
        if (imageDisclosure === 'button' || (imageDisclosure === 'none' && itemMeta && itemMeta.type !== 'image')) {
            return false;
        }
        return imageDisclosure === 'none';
    }

    _resolveTabindex() {
        return this.hasAttribute('property-display-item') ? '-1' : this._tabindex;
    }

    // TW-99633
    _scrollbarFlicker(_valueType, textWrap, _resolvedMaxHeight) {
        if (_resolvedMaxHeight) {
            if (_valueType === 'text') {
                const el = this.shadowRoot.querySelector('[part=item-value]');
                setTimeout(() => {
                    if (textWrap &&
                        el.$.label.clientHeight < 2 * PTCS.cssDecodeSize(getComputedStyle(el.$.label).fontSize, el.$.label, true)) {
                        // To prevent scrollbar flicker in ptcs-label _checkHeight(). When only one line will fit, multiline text
                        // gets assigned class forced-single-line but a vertical scrollbar first appears for a noticeable instant.
                        el.multiLine = false;
                    }
                }, 50);
            }
        }
    }
};

customElements.define(PTCS.ValueDisplay.is, PTCS.ValueDisplay);
