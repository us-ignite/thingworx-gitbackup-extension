/* eslint-disable no-unneeded-ternary */
import {fixture, expect} from '@open-wc/testing/index.js';
import {PTCS} from 'ptcs-library';
import '../ptcs-chip-data-filter-chip-container.js';

describe('<ptcs-chip-data-filter-chip-container>', () => {
    let chipContainer;
    const chipData = [{
        content: 'animal:smallCat',
        id:      '2'
    }, {
        content: 'veryVeryVeryLongCategory:veryVeryVeryLongValue',
        id:      '5'
    }, {
        content: 'vehicle:car',
        id:      '3'
    }, {
        content: 'device:smartphone',
        id:      '1'
    }];

    beforeEach(async() => {
        chipContainer = await fixture(`
            <ptcs-chip-data-filter-chip-container dictionary='{"stringShowMore": "Show more", "stringShowLess": "Show less"}'>
            </ptcs-chip-data-filter-chip-container>');
        `);
        chipContainer.data = chipData;

        await chipContainer.updateComplete;
    });

    it('check whether chips have correct params & attributes', async function() {
        const chipArray = chipContainer.shadowRoot.querySelectorAll('ptcs-chip-data-filter-chip-child');
        expect(chipArray.length).to.be.eql(chipData.length);
        await PTCS.wait();

        chipArray.forEach((chip, chipIdx) => {
            expect(chip.content).to.be.eql(chipData[chipIdx].content);
            expect(chip.dataId).to.be.eql(chipData[chipIdx].id, 'dataId property');
            expect(chip.getAttribute('data-id')).to.be.eql(chipData[chipIdx].id, 'dataId reflected as data-id');
        });
    });

    it('test empty data', async function() {
        chipContainer.data = [];
        await PTCS.wait();

        expect(null).to.be.equal(chipContainer.focusedChip);
        expect([...chipContainer.$.container.querySelectorAll('[part=chip-child]')]).to.be.eql([]);
    });

    it('test that focusing activates tooltips', async function() {
        chipContainer.subTabindex = '0';
        const focusable = chipContainer.focusableElements;
        const _tooltipEnter = chipContainer._tooltipEnter;
        const _tooltipLeave = chipContainer._tooltipLeave;
        let enterEl, leaveEl;

        await PTCS.wait();
        chipContainer._tooltipEnter = el => {
            enterEl = el.$['cross-button'];
        };

        chipContainer._tooltipLeave = el => {
            leaveEl = el.$['cross-button'];
        };

        focusable[0].focus();
        await PTCS.wait();
        expect(enterEl === focusable[0]).to.be.true;
        focusable[1].focus();
        await PTCS.wait();
        expect(leaveEl === focusable[0]).to.be.true;
        expect(enterEl === focusable[1]).to.be.true;
        chipContainer._tooltipEnter = _tooltipEnter;
        chipContainer._tooltipLeave = _tooltipLeave;
    });
});
