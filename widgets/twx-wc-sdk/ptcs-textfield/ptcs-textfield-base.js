import {LitElement, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-validate/ptcs-behavior-validate.js';

// This value is sent to a validation function, but it is not doing any validation
const not_a_validation_value = () => false;

PTCS.TextFieldMixin = subclass => class PtcsTextFieldMixin extends PTCS.BehaviorValidate(L2Pw(LitElement, subclass)) {
    static get styles() {
        return css`
        :host
        {
          display: inline-block;
          font-family: 'Open Sans', sans-serif;
          font-size: 14px;
          font-weight: normal;
          font-style: normal;
          font-stretch: normal;
          letter-spacing: normal;

          min-width:fit-content;
          min-height:fit-content;
        }

        [part="root"] {
          display: inline-flex;
          flex-direction: column;

          width: 100%;
        }

        [part="text-box"] {
          min-width: 32px;
          min-height: 32px;
        }

        [part="text-value"] {
          border: 0;
          background: transparent;
          padding: 0;
          outline: none;
          box-shadow: none;

          margin-left: 8px;
          margin-right: 8px;

          width: 100%;
          box-sizing: border-box;
          flex: 1;
          min-width: 0;
        }

        [part="label"] {
          display: none;

          font-size: 12px;
        }

        :host(:not([label=""])) [part="label"] {
          display: block;

          margin-bottom: 4px;
        }

        [part="text-value"]:focus::-webkit-input-placeholder { color: transparent; }
        [part="text-value"]:focus::-webkit-input-placeholder { color: transparent; }
        [part="text-value"]:focus::-moz-placeholder { color: transparent; }
        [part="text-value"]:focus:-ms-input-placeholder { color: transparent; }

        :host([counter]:not([maxlength=""]):not([disabled]):not([read-only])) [part="counter"] {
          display: block;

          font-size: 12px;

          margin-right: 8px;
          width: auto;
        }

        [part="counter"] {
          display: none;
        }`;
    }

    static get properties() {
        return {
            // The initial value of the control. Supports two-way data binding
            text: {
                type:        String,
                notify:      true,
                value:       '', // Need to declare default value, so it doesn't generate a change event
                // eslint-disable-next-line max-len
                validate:    '_validateText(required, requiredMessage, minlength, minLengthFailureMessage, maxlength, maxLengthFailureMessage, pattern, extraValidation)',
                observer:    '_changeBaseInput',
                observeWhen: 'immediate'
            },

            // Client-provided custom validation function
            // This is invoked with the text component itself as parameter, so that it can use any ptcs-textfield property for custom validation.
            // Can return `true` (= valid), `false` (= invalid), or `undefined` (ignore validation)
            extraValidation: {
                type: Function
            },

            // If we have both TextField and Grid Widget,
            // In some cases, dhtmlxgrid.js:1665 vendor code checks for existance of the 'value' property, and if it's
            // not there, the Grid prevents event propagation. (dhtmlxgrid.js:1698)
            value: {
                type:        String,
                observer:    'valueChanged',
                observeWhen: 'immediate'
            },

            minlength: {
                type:      Number,
                attribute: 'min-length',
                isValue:   minlength => minlength > 0
            },

            // The message to display when the value is invalid because of min length
            minLengthFailureMessage: {
                type:      String,
                isValue:   not_a_validation_value,
                attribute: 'min-length-failure-message'
            },

            maxlength: {
                type:      Number,
                attribute: 'max-length',
                isValue:   maxlength => maxlength > 0
            },

            // The message to display when the current value exceeds the maximum character length.
            maxLengthFailureMessage: {
                type:      String,
                isValue:   not_a_validation_value,
                attribute: 'max-length-failure-message'
            },

            // Validation criterion: Need to enter text
            required: {
                type:    Boolean,
                isValue: required => !!required
            },

            // The message that is displayed when no text is entered.
            requiredMessage: {
                type:      String,
                isValue:   not_a_validation_value,
                attribute: 'required-message'
            },

            // $NUP _valueHasChanged: {
            //    type: Boolean
            // },

            // The label that is shown for the text field
            label: {
                type:    String,
                value:   '',
                reflect: true
            },

            // Adds a characters counter at the end of the field
            counter: {
                type:    Boolean,
                value:   false,
                reflect: true
            },

            _counterString: {
                type: String
            },

            _nearLimit: {
                type:      Boolean,
                attribute: '_near-limit',
                reflect:   true
            },

            errorThreshold: {
                type:      String,
                attribute: 'error-threshold',
                observer:  '_updateNearLimit'
            },

            // Allows only specified amount of characters.
            maxNumberOfCharacters: {
                type:        Number,
                attribute:   'max-number-of-characters',
                reflect:     true,
                observer:    '_changeBaseInput',
                observeWhen: 'immediate'
            },

            // The hint text to show for an empty text field
            hintText: {
                type:      String,
                observer:  'hintTextChanged',
                attribute: 'hint-text',
                reflect:   true
            },

            // Disables the component
            disabled: {
                type:    Boolean,
                value:   false,
                reflect: true
            },

            // This attribute indicates that the user cannot modify the value of the control
            readOnly: {
                type:      Boolean,
                value:     false,
                attribute: 'read-only',
                reflect:   true
            },

            /**
            * A read-only property indicating whether this input has a non empty value.
            * It can be used for example in styling of the component.
            **/
            hasText: {
                type:        Boolean,
                value:       false,
                readOnly:    true,
                observer:    '_hasTextChanged',
                observeWhen: 'immediate',
                attribute:   'has-text',
                reflect:     true
            }
        };
    }

    constructor() {
        super();

        this.maxNumberOfCharacters = 1000000;
        this._text$ = '';
        this.errorThreshold = '0.9';

        // Adjust dependent values now. Doing it later will force updates
        this._changeBaseInput();
    }

    _hasTextChanged(hasText) {
        if (!hasText) {
            this.hintTextChanged(this.hintText);
        } else {
            this._hintTextOverflow = false;
        }
    }

    hintTextChanged(val) {
        if (val) {
            requestAnimationFrame(() => {
                const el = this.shadowRoot.querySelector('[part=hint-text]');
                this._hintTextOverflow = el.offsetWidth < el.scrollWidth;
            });
        }
    }

    valueChanged(value, oldval) {
        if (oldval === undefined) {
            this._valueHasChanged = value !== '';
        } else {
            this._valueHasChanged = true;
        }
        this.text = value;
    }

    isValueChanged() {
        const result =  this._valueHasChanged;
        this._valueHasChanged = false;
        return result;
    }

    isTruncated() {
        const el = this.$.input;

        // Rounding errors can (at certain zoom levels) return an offsetWidth that is 1px less than the scrollWidth (TW-115170),
        // so allow a difference of (at most) one pixel...
        if (this.hasText && (el.offsetWidth + 1) < el.scrollWidth) {
            return true;
        }
        // Are we showing truncated hint text?
        if (!this.hasText && this.hintText) {
            const hintEl = this.shadowRoot.querySelector('[part=hint-text]');
            this._hintTextOverflow = (hintEl.offsetWidth + 1) < hintEl.scrollWidth;
            // When textfield is *focused* the hint text is not shown and its scrollWidth becomes zero as display === none
            // It is included in the tooltip even if not truncated, as it is no longer visible
            return this._hintTextOverflow || hintEl.scrollWidth === 0;
        }
        return false;
    }

    _changeBaseInput() {
        const {maxNumberOfCharacters} = this;

        // NOTE: this is a very flawed approach - this method changes the property that is being observed!
        if (!this.text && typeof this.text !== 'number' && this.text !== '') {
            this.text = '';
            return;
        }

        if (this.text.length > maxNumberOfCharacters) {
            this.text = this.text.substring(0, maxNumberOfCharacters);
            return;
        }

        this.value = this.text;
        this._setHasText(typeof this.text === 'number' ? true : !!this.text);
        this._counterString = (maxNumberOfCharacters ? `${this.text.length}/${maxNumberOfCharacters}` : '');
        this._updateNearLimit();
    }

    _updateNearLimit() {
        if (!this.errorThreshold) {
            this._nearLimit = false;
            return;
        }

        const {maxNumberOfCharacters, text, errorThreshold} = this;

        if (errorThreshold.includes('%')) {
            // e.g., '50%'
            this._nearLimit = maxNumberOfCharacters && maxNumberOfCharacters * (parseFloat(errorThreshold) / 100) <= text.length;
        } else if (errorThreshold <= 1) {
            // e.g., '0.4'
            this._nearLimit = maxNumberOfCharacters && maxNumberOfCharacters * parseFloat(errorThreshold) <= text.length;
        } else if (errorThreshold % 1 === 0) {
            // e.g., '37'
            this._nearLimit = parseInt(errorThreshold, 10) <= text.length;
        } else {
            console.assert(null, 'errorThreshold must contain an integer or a valid percentage value.');
        }
    }

    // eslint-disable-next-line max-len
    _validateText(required, requiredMessage, minlength, minLengthFailureMessage, maxlength, maxLengthFailureMessage, pattern, extraValidation, value) {
        const messages = [];

        // required
        if (!value && required) {
            messages.push(requiredMessage);
        }

        // minlength
        if (value !== undefined && minlength > 0 && value.length < minlength) {
            const msg = PTCS.replaceStringTokens(minLengthFailureMessage, {value: minlength});
            messages.push(msg ? msg.join('. ') : false);
        }

        // maxlength
        if (value !== undefined && maxlength > 0 && value.length > maxlength) {
            const msg = PTCS.replaceStringTokens(maxLengthFailureMessage, {value: maxlength});
            messages.push(msg ? msg.join('. ') : false);
        }

        // pattern
        if (pattern) {
            try {
                const re = new RegExp(`^${pattern}$`);
                if (!re.test(value)) {
                    messages.push(false);
                }
            } catch (err) {
                console.error(`Invalid textfield pattern: ${JSON.stringify(pattern)}`);
            }
        }

        // At least one validation failed
        if (messages.length) {
            return messages;
        }

        // All standard validation has succeeded. Leave final say to the custom validation, if any
        return typeof extraValidation === 'function' ? extraValidation(this) : true;
    }
};
