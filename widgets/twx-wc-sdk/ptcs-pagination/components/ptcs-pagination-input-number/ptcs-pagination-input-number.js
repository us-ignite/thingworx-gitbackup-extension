import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {when} from 'lit/directives/when.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-icon/ptcs-icon.js';
import 'ptcs-textfield/ptcs-textfield.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import {PTCS} from 'ptcs-library';

class InputNumber extends PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))) {


    static get styles() {
        return css`
                :host {
                    position: relative;
                    display: inline-flex;
                    flex-direction: column;
                }

                [part="pagination-input"] {
                    box-sizing: border-box;
                }

                [part="error-text"] {
                    display: block;
                    text-align: right;
                }`;
    }

    render() {

        const _errorMsg = () => html`<ptcs-label id="error-text" part="error-text"
                .label=${this.errorMessage + ': ' + this.totalNumberOfPages}></ptcs-label>`;

        return html`<ptcs-textfield id="pagination-input" part="pagination-input" .text=${this._inputValue}
                @text-changed=${this._textChanged} @blur=${this._onBlurInput} @paste=${this._checkPastedValue}
                @keyup=${this._onKeyUpChange} @keydown=${this._onKeyDownChange} .disabled=${this.disabled}
                hide-clear-text tabindex=${this._delegatedFocus}>
            </ptcs-textfield>
            ${when(this._showErrorMessage, _errorMsg)}`;
    }

    constructor() {
        super();
        this.reset();
    }

    static get properties() {
        return {
            disabled: {
                type:    Boolean,
                reflect: true
            },

            totalNumberOfPages: {
                type:      Number,
                attribute: 'total-number-of-pages'
            },

            errorMessage: {
                type:      String,
                value:     'Max',
                attribute: 'error-message'
            },

            _showErrorMessage: {
                type:     Boolean,
                computed: '_isInputValueGreaterThanMax(_inputValue)'
            },

            _inputValue: {
                type: String
            },

            _delegatedFocus: {
                type:      String,
                value:     null,
                attribute: '_delegated-focus'
            },
        };
    }

    static get is() {
        return 'ptcs-pagination-input-number';
    }

    get value() {
        return this._currentValue;
    }

    reset() {
        this._inputValue = '1';
        this._currentValue = 1;
    }

    _isInputValueGreaterThanMax() {
        return Number(this._inputValue) > this.totalNumberOfPages;
    }

    _checkPastedValue(ev) {
        const clipboardData = ev.clipboardData.getData('Text');
        if (isNaN(clipboardData) || clipboardData < 0) {
            ev.preventDefault();
        }
    }

    _onKeyDownChange(ev) {
        const numericArray = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'Backspace', 'ArrowLeft', 'ArrowRight', 'Delete', 'Tab', 'Home', 'End'];
        this._inputValue = this._inputValue === '0' ? '' : this._inputValue;
        if (!numericArray.includes(ev.key) &&
            !((ev.ctrlKey || ev.metaKey) && ['a', 'A', 'c', 'C', 'v', 'V', 'x', 'X'].includes(ev.key))
        ) {
            ev.preventDefault();
        }
    }

    _onBlurInput() {
        if (this._inputValue.length === 0) {
            this._inputValue = this._currentValue.toString();
            return;
        }
        this._checkBorderUseCases(this._inputValue);
    }

    _onKeyUpChange(ev) {
        if (ev.key === 'Enter') {
            if (this._inputValue.length === 0) {
                this._inputValue = this._currentValue.toString();
            }
            this._checkBorderUseCases(this._inputValue);
            this._selectInputText();
        } else if (ev.key === 'Escape') {
            this._inputValue = this._currentValue.toString();
            this._selectInputText();
        }
    }

    _textChanged(ev) {
        this._inputValue = ev.detail.value;
    }

    _checkBorderUseCases(page) {
        let inputValueNumber = Number(page);
        if (inputValueNumber > this.totalNumberOfPages || inputValueNumber === 0) {
            this._inputValue = this._currentValue.toString();
            inputValueNumber = this._currentValue;
        }
        this._currentValue = inputValueNumber;
        this.dispatchEvent(new CustomEvent('value-approved', {
            bubbles:  true,
            composed: true,
            detail:   {
                pageNo: this._currentValue
            }
        }));
    }

    _selectInputText() {
        const input = this.$['pagination-input'];
        if (!input.hasUpdated) {
            input.performUpdate();
        }
        input.selectAll();
    }
}

customElements.define(InputNumber.is, InputNumber);
