/* eslint-disable no-unused-expressions */

import '../ptcs-pagination-input-number.js';

import {expect, fixture} from '@open-wc/testing/index.js';

describe('<ptcs-pagination-input-number>', () => {

    const initialValue = '1';
    let inputNumber, inputField;

    beforeEach(async() => {
        inputNumber = await fixture(`<ptcs-pagination-input-number value="${initialValue}"
                                     total-number-of-pages="20"></ptcs-pagination-input-number>`);
        inputField = inputNumber.shadowRoot.querySelector('#pagination-input');
    });

    it('should not allow string input', async function() {
        const keyDownEvent = new KeyboardEvent('keydown', {key: 'a', cancelable: true});
        const isCharacter = inputField.dispatchEvent(keyDownEvent);
        expect(isCharacter).to.be.eql(false);
        expect(inputNumber._inputValue).to.be.eql(initialValue);
        if (!isCharacter) {
            const keyUpEvent = new KeyboardEvent('keyup', {key: 'a'});
            inputField.dispatchEvent(keyUpEvent);
            expect(inputNumber._inputValue).to.be.eql(initialValue);
        }
    });

    it('_isInputValueGreaterThanMax returns true when input value is grather than total number of pages', async function() {
        const event = new KeyboardEvent('keyup', {});

        inputNumber._inputValue = 22;
        inputField.dispatchEvent(event);
    });

    it('disabled attribute should be set if disabled property is equal true', async function() {
        const newDisabledValue = true;

        inputNumber.disabled = newDisabledValue;

        await inputNumber.updateComplete;

        expect(inputField.hasAttribute('disabled')).to.be.eql(newDisabledValue);
    });

    describe('test of blur event and `Enter` pressed', () => {
        let keyUpEvent, blurEvent;

        beforeEach(async() => {
            inputNumber = await fixture(`<ptcs-pagination-input-number value="${initialValue}"
                                         total-number-of-pages="20"></ptcs-pagination-input-number>`);
            inputField = inputNumber.shadowRoot.querySelector('#pagination-input');
            keyUpEvent = new KeyboardEvent('keyup', {key: 'Enter', cancelable: true});
            blurEvent = new Event('blur');
        });

        it ('check whether input value is set to the current value after blur event or pressing `Enter`', async function() {
            inputNumber._inputValue = 22;
            inputField.dispatchEvent(keyUpEvent);
            expect(inputNumber._inputValue).to.be.eql(initialValue);

            inputNumber._inputValue = 22;
            inputField.dispatchEvent(blurEvent);
            expect(inputNumber._inputValue).to.be.eql(initialValue);
        });

        it('should set input value to previous value after blur event if not type any value', async function() {
            inputNumber._inputValue = '';
            inputField.dispatchEvent(blurEvent);
            expect(inputNumber._inputValue).to.be.eql(initialValue);
        });
    });

    describe('test of pasted values', () => {

        function checkPastedValue(value) {
            const pasteData = new DataTransfer();
            pasteData.setData('text', value);
            const pasteEvent = new ClipboardEvent('paste', {
                clipboardData: pasteData,
                cancelable:    true
            });
            return inputField.dispatchEvent(pasteEvent);
        }

        beforeEach(async() => {
            inputNumber = await fixture(`<ptcs-pagination-input-number value="${initialValue}"
                                         total-number-of-pages="20"></ptcs-pagination-input-number>`);
        });

        it('invalid value should not be successfully pasted into the field, the input value should not change', async function() {
            const isPastedValueIsValid = checkPastedValue('abc');
            expect(isPastedValueIsValid).to.be.eql(false);
            expect(inputNumber._inputValue).to.be.eql(initialValue);
        });

        it('input value should show error message for values > MAX', async function() {
            const valueGreaterThanMax = '22';
            checkPastedValue(valueGreaterThanMax);
            inputNumber._inputValue = valueGreaterThanMax;
            expect(inputNumber._isInputValueGreaterThanMax(inputNumber._inputValue)).to.be.eql(true);
            expect(inputNumber._inputValue).to.be.eql(valueGreaterThanMax);
        });
    });
});
