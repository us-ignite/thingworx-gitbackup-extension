import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-textfield/ptcs-textfield-base.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';

PTCS.Textarea = class extends PTCS.BehaviorTabindex(PTCS.BehaviorTooltip(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(
    PTCS.TextFieldMixin(L2Pw(LitElement), []))))) {

    static get styles() {
        // NOTE: This styling contains lots of ancient margins and paddings that should go to the theme engine
        return css`
        :host {
          display: inline-flex;
          flex-direction: column;
          box-sizing: border-box;
          overflow: hidden;
          height: var(--ptcs-textfield-auto-height);
        }

        :host(:focus) {
          outline: none;
        }

        [part=root] {
          display: flex;
          flex-direction: column;
          width: 100%;
          height: 100%;
          flex-grow: 1;
        }

        [part=text-box] {
          flex-grow: 1;
          box-sizing: border-box;
          display: inline-flex;
          flex-direction: column;
          position: relative;
        }

        :host([text-alignment=right]) [part=text-box] {
          text-align: right;
        }

        [part=label] {
          display: none;
          flex-shrink: 0;
          min-width: unset;
          min-height: unset;
        }

        :host(:not([label=""])) [part=label] {
          display: inline-flex;
          padding-bottom: 4px;
        }

        [part=text-value] {
          resize: none;
          overflow: auto;
          border: 0;
          background: transparent;
          padding: 8px;
          outline: none;
          box-shadow: none;
          width: 100%;
          height: 100%;
          box-sizing: border-box;
          flex: 1 1 auto;
        }

        [part=text-value]:focus::-webkit-input-placeholder { color: transparent; }
        [part=text-value]:focus::-webkit-input-placeholder { color: transparent; }
        [part=text-value]:focus::-moz-placeholder { color: transparent; }
        [part=text-value]:focus:-ms-input-placeholder { color: transparent; }

        [part=counter] {
          display: none;
          margin-bottom: 8px;
          direction: rtl;
        }

        :host([counter]:not([maxlength=""]):not([disabled]):not([read-only])) [part=counter] {
          display: block;
          margin-right: 8px;
          text-align: right;
          width: auto;
        }

        [part=hint-text] {
          position: absolute;
          height: 100%;
          width: 100%;
          cursor: text;
          box-sizing: border-box;
        }

        :host([has-text]) [part=hint-text] {
          display: none;
        }

        /* Hide hint-text on focus. Also a must for Safari, as <input> sometimes refuses keyboard input */
        [part=text-value]:focus + [part=hint-text] {
            display: none;
        }`;
    }

    render() {
        return html`
        <div part="root" id="root">
          <ptcs-label part="label" id="label" .label=${this.label} multi-line
            .horizontalAlignment=${this.labelAlignment} disable-tooltip></ptcs-label>
          <div part="text-box" id="textbox">
            <textarea part="text-value" id="input" .disabled=${this.disabled} ?readonly=${this.readOnly}
            maxlength=${this.maxNumberOfCharacters} .value=${this.text || ''} @input=${this._onInput}
            tabindex=${this._tabindex(this._delegatedFocus, this.noTabindex)}></textarea>
            <label part="hint-text" id="hintText">${this.hintText}</label>
            <div id="counter" part="counter">${this._counterString}</div>
          </div>
        </div>`;
    }

    static get is() {
        return 'ptcs-textarea';
    }

    static get properties() {
        return {
            // Alignment of text (left, right).
            textAlignment: {
                type:      String,
                value:     'left',
                attribute: 'text-alignment',
                reflect:   true
            },

            // Alignment of label (left, right, center).
            labelAlignment: {
                type:      String,
                value:     'left',
                attribute: 'label-alignment'
            },

            // Don't assign own height (just go with the flow)?
            noHeight: {
                type:      Boolean,
                attribute: 'no-height'
            },

            _autoHeight: {
                type:        Number,
                observer:    '_autoHeightChanged',
                observeWhen: 'immediate'
            },

            // Prevent scrolling to the input element when textarea is getting focused
            _preventFocusAutoScroll: {
                type:     Boolean,
                computed: '_computePreventFocusAutoScroll(_noSpaceForMessage)'
            },

            _delegatedFocus: {
                type:  String,
                value: null
            }
        };
    }

    static get observers() {
        return [
            '_observeValidationMessage(_validationChangeNo)',
            '_computeHeight(text, noHeight)'
        ];
    }

    ready() {
        super.ready();

        const _tooltipClose =  () => this._tooltipClose();

        this._trackFocus(this, this.$.textbox);

        if (this.text === undefined) {
            this.text = '';
        }

        // Clicking on the hint text sends focus to <input>
        this.$.hintText.addEventListener('mouseup', () => {
            if (!this.disabled && !this.readOnly) {
                // Forward focus to <textarea>
                this.$.input.focus();
            }
            requestAnimationFrame(_tooltipClose);
        });

        this.addEventListener('focus', ev => this._showTooltip(ev));

        this.addEventListener('blur', () => {
            if (this.isValueChanged()) {
                this.dispatchEvent(new CustomEvent('TextAreaChanged', {bubbles: true, composed: true, detail: {key: 'Enter'}}));
            }
            _tooltipClose();
        });

        this.$.input.addEventListener('blur', () => {
            this._stayUnvalidated = false;
        });

        // Listen to keys in order to dismiss tooltip (if any)
        this.$.input.addEventListener('keyup', ev => {
            if (PTCS.alphanumericKey(ev.key)) {
                this._stayUnvalidated = true;
            }
            requestAnimationFrame(_tooltipClose);
        });

        // Listen to click in order to dismiss tooltip (if any)
        this.$.input.addEventListener('click', () => requestAnimationFrame(_tooltipClose));

        // Use boilerplate function in ptcs-behavior-tooltip
        this.tooltipFunc = this.hideIfTooltipEqualsLabel;

        // Unless otherwise specified, a textarea should not start validation until the user "blurs away" from a changed textfield
        if (this._stayUnvalidated === undefined) {
            this._stayUnvalidated = true;
        }

        this._initInput(4);
    }

    // Hack that appears to be needed. Don't know how to figure out if HTML textfield is ready or not. Frustrating.
    _initInput(numLeft) {
        // Whitout these repeated calls, the validation behavior demo for textfield starts with a textfield that is too small
        this._computeHeight();
        if (numLeft > 0) {
            requestAnimationFrame(() => this._initInput(numLeft - 1));
        }
    }

    _onInput(ev) {
        this.text = ev.target.value;
    }

    _autoHeightChanged(_autoHeight) {
        this.style.setProperty('--ptcs-textfield-auto-height', (_autoHeight || _autoHeight === 0) ? `${_autoHeight}px` : '');
    }

    _tabindex(_delegatedFocus, noTabindex) {
        return (_delegatedFocus && !noTabindex) ? _delegatedFocus : '-1';
    }

    _showTooltip(ev) {
        const tooltip = this.tooltipFunc;
        this._tooltipEnter(this, ev.clientX, ev.clientY, tooltip, {showAnyway: true});
    }

    _computeHeight() {
        if (this.noHeight) {
            // Don't manipulate own height. Someone else has requested control
            this._autoHeight = undefined;
            this.$.root.style.height = '';
            this.$.root.style.flexShrink = '';
            return;
        }

        const scrollTop = this.scrollTop;

        this._autoHeight = undefined;
        this.$.root.style.height = '';
        this.$.root.style.flexShrink = '';

        const h = this.$.root.offsetHeight + this.$.input.scrollHeight - this.$.input.clientHeight + this.offsetHeight - this.clientHeight;

        if (h === 0) {
            // TW-108172, disallow zero height
            return;
        }

        const messageElement = this.shadowRoot.querySelector('ptcs-validation-message');

        if (messageElement) {
            if (messageElement.previousSibling) {
                // Add height for the validation message below the textarea
                this._autoHeight = h + messageElement.scrollHeight + messageElement.offsetHeight - messageElement.clientHeight;
            } else {
                // Enter weird mode. The validation message resides above the textarea. That means that someone else controls
                // the components height. Now the root element must retain its size and the host element enable a scrollbar so
                // the user can scroll to see both the validation message and the root
                messageElement.style.display = 'none';
                this._autoHeight = h;
                const h2 = this.$.root.offsetHeight;
                messageElement.style.display = '';
                this.$.root.style.height = `${h2}px`;
                this.$.root.style.flexShrink = '0';
            }
        } else {
            this._autoHeight = h; // No message. Try to set optimal height
        }

        this.scrollTop = scrollTop;
    }

    _insertValidationMessage(messageElement) {
        this.defaultInsertValidationMessageForVerticalLayout(messageElement);
        this.updateComplete.then(() => requestAnimationFrame(this._computeHeight.bind(this)));
    }

    _computePreventFocusAutoScroll(_noSpaceForMessage) {
        return _noSpaceForMessage;
    }

    // When the validation message changes
    _observeValidationMessage(_validationChangeNo) {
        this.updateComplete.then(this._computeHeight.bind(this));
    }
};

customElements.define(PTCS.Textarea.is, PTCS.Textarea);
