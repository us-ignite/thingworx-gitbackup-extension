/* eslint-disable no-unused-expressions */

import '../ptcs-pagination-carousel.js';
import {PTCS} from 'ptcs-library/library.js';

import {expect, fixture} from '@open-wc/testing/index.js';


describe('<ptcs-pagination-carousel>', () => {
    const ENABLED = false;
    const DISABLED = true;
    const TEN = 10;
    let carousel, expectedButtonNumbers;

    function buttonsAreUpdated() {
        const buttonNodeList = carousel.shadowRoot.querySelectorAll('ptcs-button[part="page-number-button"]');

        return Promise.all(Array.from(buttonNodeList).map(button => button.updateComplete));
    }

    function areButtonValuesCorrect(refArray) {
        const buttonNodeList = carousel.shadowRoot.querySelectorAll('ptcs-button[part="page-number-button"]');
        if (buttonNodeList.length !== refArray.length) {
            return false;
        }

        for (let i = 0; i < buttonNodeList.length; ++i) {
            if (Number(buttonNodeList[i].label) !== Number(refArray[i])) {
                return false;
            }
        }
        return true;
    }

    function areArrowStateCorrect(isRefLetArrowDisabled, isRefRightArrowDisabled) {
        const isLeftArrowDisabled = carousel.shadowRoot.querySelector('#left-arrow').hasAttribute('disabled');
        const isRightArrowDisabled = carousel.shadowRoot.querySelector('#right-arrow').hasAttribute('disabled');

        return isLeftArrowDisabled === isRefLetArrowDisabled && isRightArrowDisabled === isRefRightArrowDisabled;
    }

    function checkCommonResponse() {
        const buttonNodeList = carousel.shadowRoot.querySelectorAll('ptcs-button[part="page-number-button"]');
        const buttonsArray = [];
        buttonNodeList.forEach((button) => buttonsArray.push(button.label));
        expect(areButtonValuesCorrect(expectedButtonNumbers)).to.be.eql(true, 'actual: ' + buttonsArray + ', expected: ' +
          expectedButtonNumbers.toString());
        const selectedButton = carousel.shadowRoot.querySelector('ptcs-button[part="page-number-button"][selected]');
        expect(Number(selectedButton.label)).to.be.eql(carousel.currentPage, 'Current page has selected attribute');
    }

    beforeEach(async() => {
        carousel = await fixture('<ptcs-pagination-carousel></ptcs-pagination-carousel>');
    });

    it('carousel buttons generation', async function() {
        const buttonNumber = carousel.shadowRoot.querySelectorAll('ptcs-button[part="page-number-button"]').length;
        const disabledArrowsNumber = carousel.shadowRoot.querySelectorAll('ptcs-button[disabled]').length;

        expect(buttonNumber).to.be.eql(1);
        expect(disabledArrowsNumber).to.be.eql(2);
    });

    it('one item', async function() {
        const buttonNumber = carousel.shadowRoot.querySelectorAll('ptcs-button[part="page-number-button"]').length;
        const disabledArrowsNumber = carousel.shadowRoot.querySelectorAll('ptcs-button[disabled]').length;

        expect(buttonNumber).to.be.eql(1);
        expect(disabledArrowsNumber).to.be.eql(2);
    });

    it('two items', async function() {
        carousel.totalNumberOfPages = 2;

        await carousel.updateComplete;

        const buttonNumber = carousel.shadowRoot.querySelectorAll('ptcs-button[part="page-number-button"]').length;
        const isLeftArrowDisabled = carousel.shadowRoot.querySelector('#left-arrow').hasAttribute('disabled');
        const isRightArrowDisabled = carousel.shadowRoot.querySelector('#right-arrow').hasAttribute('disabled');

        expect(buttonNumber).to.be.eql(2);
        expect(isLeftArrowDisabled).to.be.eql(true);
        expect(isRightArrowDisabled).to.be.eql(false);
    });

    it('seven items, navigation back and forth', async function() {
        const SEVEN = 7;
        carousel.totalNumberOfPages = SEVEN;
        await PTCS.wait();

        const buttonNumber = carousel.shadowRoot.querySelectorAll('ptcs-button[part="page-number-button"]').length;
        expectedButtonNumbers = [1, 2, 3, 4, 5, 6, 7];
        const rightArrow = carousel.shadowRoot.querySelector('#right-arrow');

        expect(areArrowStateCorrect(DISABLED, ENABLED)).to.be.eql(true);
        expect(buttonNumber).to.be.eql(SEVEN);

        // navigate to the right
        for (let i = 2; i < SEVEN; ++i) {
            rightArrow.click();

            await buttonsAreUpdated();

            checkCommonResponse();
            expect(carousel.currentPage).to.be.eql(i);
        }

        rightArrow.click();
        await buttonsAreUpdated();

        expect(areArrowStateCorrect(ENABLED, DISABLED)).to.be.eql(true);

        // navigate to the left
        const leftArrow = carousel.shadowRoot.querySelector('#left-arrow');
        for (let i = SEVEN - 1; i > 1; --i) {
            leftArrow.click();
            await buttonsAreUpdated();

            checkCommonResponse();
            expect(carousel.currentPage).to.be.eql(i);
        }

        leftArrow.click();
        await buttonsAreUpdated();

        expect(areArrowStateCorrect(DISABLED, ENABLED)).to.be.eql(true);
    });

    it('many items, navigation back & forth', async function() {
        carousel.totalNumberOfPages = TEN;
        await buttonsAreUpdated();
        await PTCS.wait();

        expectedButtonNumbers = [1, 2, 3, 4, 5, 10]; // 1 2 3 4 5 ... 10

        expect(areArrowStateCorrect(DISABLED, ENABLED)).to.be.eql(true, 'arrows DISABLED ENABLED');
        expect(areButtonValuesCorrect(expectedButtonNumbers)).to.be.eql(true, '1 2 3 4 5 ... 10 at initialization');

        const rightArrow = carousel.shadowRoot.querySelector('#right-arrow');
        rightArrow.click();
        await buttonsAreUpdated();

        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(2);

        rightArrow.click();
        await buttonsAreUpdated();

        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(3);

        rightArrow.click();
        await buttonsAreUpdated();

        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(4);

        rightArrow.click();
        await buttonsAreUpdated();

        expectedButtonNumbers = [1, 5, 6, 7, 10];
        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(5);

        rightArrow.click();
        await buttonsAreUpdated();

        expectedButtonNumbers = [1, 5, 6, 7, 10];
        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(6);

        rightArrow.click();
        await buttonsAreUpdated();

        expectedButtonNumbers = [1, 6, 7, 8, 9, 10];
        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(7);

        rightArrow.click();
        await buttonsAreUpdated();

        expectedButtonNumbers = [1, 6, 7, 8, 9, 10];
        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(8);

        rightArrow.click();
        await buttonsAreUpdated();

        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(9);

        rightArrow.click();
        await buttonsAreUpdated();

        expectedButtonNumbers = [1, 6, 7, 8, 9, 10];
        expect(areButtonValuesCorrect(expectedButtonNumbers)).to.be.eql(true, '1 ... 6 7 8 9 10');
        expect(areArrowStateCorrect(ENABLED, DISABLED)).to.be.eql(true, 'arrows ENABLED DISABLED');
        expect(carousel.currentPage).to.be.eql(10);

        const leftArrow = carousel.shadowRoot.querySelector('#left-arrow');
        leftArrow.click();
        await buttonsAreUpdated();

        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(9);

        leftArrow.click();
        await buttonsAreUpdated();

        expectedButtonNumbers = [1, 6, 7, 8, 9, 10];
        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(8);

        leftArrow.click();
        await buttonsAreUpdated();

        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(7);

        leftArrow.click();

        await buttonsAreUpdated();

        expectedButtonNumbers = [1, 4, 5, 6, 10];
        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(6);

        leftArrow.click();
        await buttonsAreUpdated();

        expectedButtonNumbers = [1, 4, 5, 6, 10];
        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(5);

        leftArrow.click();
        await buttonsAreUpdated();

        expectedButtonNumbers = [1, 2, 3, 4, 5, 10];
        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(4);

        leftArrow.click();
        await buttonsAreUpdated();

        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(3);

        leftArrow.click();
        await buttonsAreUpdated();

        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(2);

        leftArrow.click();
        await buttonsAreUpdated();

        expect(areButtonValuesCorrect(expectedButtonNumbers)).to.be.eql(true, '1 2 3 4 5 ... 10');
        expect(areArrowStateCorrect(DISABLED, ENABLED)).to.be.eql(true, 'arrows DISABLED ENABLED');
        expect(carousel.currentPage).to.be.eql(1);
    });

    it('huge number of items, navigation back & forth', async function() {
        carousel.totalNumberOfPages = 10000000;
        await buttonsAreUpdated();
        await PTCS.wait();

        expectedButtonNumbers = [1, 2, 3, 4, 5, 10000000];
        expect(areButtonValuesCorrect(expectedButtonNumbers)).to.be.eql(true, '1 2 3 4 5 ... 10000000');

        carousel.currentPage = 10;
        await buttonsAreUpdated();
        await PTCS.wait();

        expectedButtonNumbers = [1, 10, 11, 12, 10000000];
        expect(areButtonValuesCorrect(expectedButtonNumbers)).to.be.eql(true, '1 10 11 12 10000000');

        carousel.currentPage = 10000000;
        await buttonsAreUpdated();
        await PTCS.wait();

        expectedButtonNumbers = [1, 9999997, 9999998, 9999999, 10000000];
        expect(areButtonValuesCorrect(expectedButtonNumbers)).to.be.eql(true, '1 9999997 9999998 9999999 10000000 current page 10000000 ');

        const leftArrow = carousel.shadowRoot.querySelector('#left-arrow');
        leftArrow.click();
        await buttonsAreUpdated();

        expect(carousel.currentPage).to.be.eql(9999999);

        leftArrow.click();
        await buttonsAreUpdated();

        expect(carousel.currentPage).to.be.eql(9999998);
        expectedButtonNumbers = [1, 9999997, 9999998, 9999999, 10000000];
        expect(areButtonValuesCorrect(expectedButtonNumbers)).to.be.eql(true, '1 9999997 9999998 9999999 10000000 current page 9999998');

        leftArrow.click();
        await buttonsAreUpdated();

        expect(carousel.currentPage).to.be.eql(9999997);
        expectedButtonNumbers = [1, 9999997, 9999998, 9999999, 10000000];
        expect(areButtonValuesCorrect(expectedButtonNumbers)).to.be.eql(true, '1 9999997 9999998 9999999 10000000 current page 9999997');

        leftArrow.click();
        await buttonsAreUpdated();

        expect(carousel.currentPage).to.be.eql(9999996);
        expectedButtonNumbers = [1, 9999995, 9999996, 10000000];
        expect(areButtonValuesCorrect(expectedButtonNumbers)).to.be.eql(true, '1 9999995 9999996 10000000 current page 9999996');
    });

    it('direct page setting', async function() {
        carousel.totalNumberOfPages = TEN;
        const SIX = 6;
        carousel.currentPage = SIX;

        await carousel.updateComplete;

        expectedButtonNumbers = [1, 6, 7, 8, 9, 10]; // 1 ... 6 7 8 9 10

        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(SIX);

        const NINE = 9;
        carousel.currentPage = NINE;

        await carousel.updateComplete;

        expectedButtonNumbers = [1, 6, 7, 8, 9, 10]; // 1 ... 6 7 8 9 10

        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(NINE);

        // unhappy path; set a page outside the allowed range;
        carousel.currentPage = TEN + 1;

        await carousel.updateComplete;

        expect(carousel.currentPage).to.be.eql(NINE);

        // clicking on a button
        const someButton = carousel.shadowRoot.querySelector('ptcs-button[part="page-number-button"]');
        const someButtonLabelAsNumber = Number(someButton.label);
        someButton.click();

        await PTCS.wait();

        expect(carousel.currentPage).to.be.eql(someButtonLabelAsNumber);
    });

    it('click on arrow button sets navMode', async function() {
        carousel.totalNumberOfPages = 10;
        await buttonsAreUpdated();
        await PTCS.wait();

        expectedButtonNumbers = [1, 2, 3, 4, 5, 10]; // 1 2 3 4 5 ... 10

        expect(areArrowStateCorrect(DISABLED, ENABLED)).to.be.eql(true, 'arrows DISABLED ENABLED');
        expect(areButtonValuesCorrect(expectedButtonNumbers)).to.be.eql(true, '1 2 3 4 5 ... 10 at initialization');
        expect(carousel.currentPage).to.be.eql(1, 'Page 1 default');
        expect(carousel.navMode).to.be.eql(undefined, 'No navigation as yet');

        const leftArrow = carousel.shadowRoot.querySelector('#left-arrow');
        leftArrow.click();
        await buttonsAreUpdated();

        expect(carousel.navMode).to.be.eql(undefined, 'Click on arrow is ignored when the button is disabled');

        const rightArrow = carousel.shadowRoot.querySelector('#right-arrow');
        rightArrow.click();
        await buttonsAreUpdated();

        expect(areArrowStateCorrect(ENABLED, ENABLED)).to.be.eql(true);
        expect(areButtonValuesCorrect(expectedButtonNumbers)).to.be.eql(true, '1 2 3 4 5 ... 10 when current page is 2');
        expect(carousel.currentPage).to.be.eql(2, 'currentPage is 2');
        expect(carousel.navMode).to.be.eql('right-arrow', 'Click on arrow updates navMode');

        leftArrow.click();
        await buttonsAreUpdated();

        expect(areArrowStateCorrect(DISABLED, ENABLED)).to.be.eql(true, 'arrows DISABLED ENABLED again');
        expect(areButtonValuesCorrect(expectedButtonNumbers)).to.be.eql(true, '1 2 3 4 5 ... 10 at initialization');
        expect(carousel.currentPage).to.be.eql(1, 'Page 1 arrived at via left-arrow');
        expect(carousel.navMode).to.be.eql('left-arrow', 'From page 2 to 1 with left-arrow');
    });

    it('min size layout without ...', async function() {
        carousel.totalNumberOfPages = 4;
        carousel.minSize = true;

        await carousel.updateComplete;

        expect(areArrowStateCorrect(DISABLED, ENABLED)).to.be.eql(true);
        expect(areButtonValuesCorrect([1, 2, 3, 4])).to.be.eql(true, '1 2 3 4');

        const labels = carousel.shadowRoot.querySelectorAll('ptcs-label[part="three-dots"]');
        expect(labels.length).to.be.eql(0, 'No ... label');


    });

    it('min size attribute', async function() {
        carousel.totalNumberOfPages = 6;
        carousel.minSize = true;

        await carousel.updateComplete;

        expect(areArrowStateCorrect(DISABLED, ENABLED)).to.be.eql(true);
        expect(areButtonValuesCorrect([1, 2, 6])).to.be.eql(true, '1 2 ... 6');
        expect(carousel.currentPage).to.be.eql(1);

        const rightArrow = carousel.shadowRoot.querySelector('#right-arrow');
        rightArrow.click();

        await PTCS.wait();

        expect(areArrowStateCorrect(ENABLED, ENABLED)).to.be.eql(true);
        expect(areButtonValuesCorrect([2, 3, 6])).to.be.eql(true, '2 3 ... 6');
        expect(carousel.currentPage).to.be.eql(2, 'currentPage 2');
        rightArrow.click();

        await PTCS.wait();

        function checkCommonResponseWhenGoRight() {
            expect(areArrowStateCorrect(ENABLED, ENABLED)).to.be.eql(true, 'areArrowStateCorrect ENABLED ENABLED');
            expect(areButtonValuesCorrect([3, 4, 5, 6])).to.be.eql(true, 'areButtonValuesCorrect 3 4 5 6');
        }

        checkCommonResponseWhenGoRight();
        expect(carousel.currentPage).to.be.eql(3, 'currentPage 3');
        rightArrow.click();

        await PTCS.wait();

        checkCommonResponseWhenGoRight();
        expect(carousel.currentPage).to.be.eql(4, 'currentPage 4');
        rightArrow.click();

        await PTCS.wait();

        checkCommonResponseWhenGoRight();
        expect(carousel.currentPage).to.be.eql(5, 'currentPage 5');
        rightArrow.click();

        await PTCS.wait();

        expect(areArrowStateCorrect(ENABLED, DISABLED)).to.be.eql(true, 'areArrowStateCorrect ENABLED DISABLED');
        expect(areButtonValuesCorrect([1, 5, 6])).to.be.eql(true, 'areButtonValuesCorrect 1 5 6');
        expect(carousel.currentPage).to.be.eql(6, 'currentPage 6');

        const leftArrow = carousel.shadowRoot.querySelector('#left-arrow');
        leftArrow.click();

        await PTCS.wait();

        expect(areArrowStateCorrect(ENABLED, ENABLED)).to.be.eql(true, 'areArrowStateCorrect ENABLED ENABLED #2');
        expect(areButtonValuesCorrect([1, 4, 5])).to.be.eql(true, 'areButtonValuesCorrect 1 4 5');
        expect(carousel.currentPage).to.be.eql(5);
        leftArrow.click();

        await PTCS.wait();

        function checkCommonResponseWhenGoLeft() {
            expect(areArrowStateCorrect(ENABLED, ENABLED)).to.be.eql(true);
            expect(areButtonValuesCorrect([1, 2, 3, 4])).to.be.eql(true);
        }
        checkCommonResponseWhenGoLeft();
        expect(carousel.currentPage).to.be.eql(4);
        leftArrow.click();

        await PTCS.wait();

        checkCommonResponseWhenGoLeft();
        expect(carousel.currentPage).to.be.eql(3);
        leftArrow.click();

        await PTCS.wait();

        checkCommonResponseWhenGoLeft();
        expect(carousel.currentPage).to.be.eql(2);
        leftArrow.click();

        await PTCS.wait();
        expect(areArrowStateCorrect(DISABLED, ENABLED)).to.be.eql(true);
        expect(areButtonValuesCorrect([1, 2, 6])).to.be.eql(true);
        expect(carousel.currentPage).to.be.eql(1);
    });

    it('big numbers and button widths', async function() {
        // Fewer buttons are shown when we have large numbers
        carousel.totalNumberOfPages = 99999;
        carousel.currentPage = 12340; // PageSize is 1 in the test

        await carousel.updateComplete;
        await PTCS.wait();

        const buttons = carousel.shadowRoot.querySelectorAll('ptcs-button[part="page-number-button"]');
        let buttonNumber = buttons.length;
        expect(buttonNumber).to.be.eql(4, 'Four page number buttons');

        expectedButtonNumbers = [1, 12340, 12341, 99999];
        await buttonsAreUpdated();
        checkCommonResponse();

        // Don't drop the target page number on large page numbers
        carousel.currentPage = 10000 ;

        await carousel.updateComplete;
        await PTCS.wait();

        buttonNumber = carousel.shadowRoot.querySelectorAll('ptcs-button[part="page-number-button"]').length;
        expect(buttonNumber).to.be.eql(4, '1 ... 10000 1001 ... 99999');

        expectedButtonNumbers = [1, 10000, 10001, 99999];
        checkCommonResponse();
    });

    it('minSize switch and min layout navigation', async function() {
        carousel.totalNumberOfPages = 50;
        carousel.currentPage = 27; // PageSize is 1 in the test

        await carousel.updateComplete;
        await PTCS.wait();
        expectedButtonNumbers = [1, 27, 28, 29, 50];

        checkCommonResponse();

        carousel.minSize = true; // Switch to minSize
        await carousel.updateComplete;
        await PTCS.wait();

        expectedButtonNumbers = [27, 28, 50];
        checkCommonResponse();

        carousel.totalNumberOfPages = 20;
        await carousel.updateComplete;
        await PTCS.wait();

        expectedButtonNumbers = [1, 2, 20];
        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(1, 'currentPage reset to 1 as previous value 27 is now out-of-bounds');

        carousel.currentPage = 6;

        await carousel.updateComplete;
        await PTCS.wait();

        expectedButtonNumbers = [6, 7, 20];
        checkCommonResponse();

        // navigate to the left
        const leftArrow = carousel.shadowRoot.querySelector('#left-arrow');
        leftArrow.click();

        await carousel.updateComplete;
        await PTCS.wait();

        expectedButtonNumbers = [5, 6, 20];
        checkCommonResponse();

        leftArrow.click();

        await carousel.updateComplete;
        await PTCS.wait();

        // Legacy idiosyncrasy: On arrow navigation the first / last 4 buttons
        // get displayed, dropping the other range endpoint until the current page
        // is start or end
        expectedButtonNumbers = [1, 2, 3, 4];
        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(4);

        leftArrow.click();

        await carousel.updateComplete;
        await PTCS.wait();

        checkCommonResponse(); // Still showing 1, 2, 3 4
        expect(carousel.currentPage).to.be.eql(3);

        leftArrow.click();

        await carousel.updateComplete;
        await PTCS.wait();

        checkCommonResponse(); // Still showing 1, 2, 3 4
        expect(carousel.currentPage).to.be.eql(2);

        leftArrow.click();

        await carousel.updateComplete;
        await PTCS.wait();

        expectedButtonNumbers = [1, 2, 20];
        checkCommonResponse();
        expect(carousel.currentPage).to.be.eql(1, 'Reached start of range, both endpoints showing');
    });

    it('invalid totalNumberOfPages fallback', async function() {
        carousel.totalNumberOfPages = 100;
        carousel.currentPage = 50;
        await carousel.updateComplete;

        expect(carousel.currentPage).to.be.eql(50);

        carousel.totalNumberOfPages = 40;
        await carousel.updateComplete;

        expect(carousel.currentPage).to.be.eql(1, 'Page number is reset to 1 when currentPage > totalNumberofPages');

        carousel.totalNumberOfPages = 'xyzzy';
        await carousel.updateComplete;

        expect(carousel.currentPage).to.be.eql(1, 'invalid assignment has no effect');
    });

});
